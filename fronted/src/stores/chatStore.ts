import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { streamChat, syncChat } from '../composables/useSseChat'
import type { ChatMessage, ChatMode, ChatSessionSummary, StreamStatus } from '../types/chat'

const SESSION_KEY = 'rin1.frontend.sessions'
const MESSAGE_KEY_PREFIX = 'rin1.frontend.messages.'

const QUICK_PROMPTS = [
  '对方最近总是已读不回，我应该怎么沟通？',
  '帮我准备一段既坚定又温柔的边界表达。',
  '我们因为小事频繁争吵，先从哪一步修复最有效？',
  '给我一个 7 天关系升温行动清单。',
]

function createId(prefix: string) {
  if (typeof crypto !== 'undefined' && typeof crypto.randomUUID === 'function') {
    return `${prefix}-${crypto.randomUUID()}`
  }
  return `${prefix}-${Date.now()}-${Math.floor(Math.random() * 1000)}`
}

function loadSessions() {
  const raw = localStorage.getItem(SESSION_KEY)
  if (!raw) {
    return [] as ChatSessionSummary[]
  }
  try {
    return JSON.parse(raw) as ChatSessionSummary[]
  } catch {
    return [] as ChatSessionSummary[]
  }
}

function loadMessages(chatId: string) {
  const raw = localStorage.getItem(`${MESSAGE_KEY_PREFIX}${chatId}`)
  if (!raw) {
    return [] as ChatMessage[]
  }
  try {
    return JSON.parse(raw) as ChatMessage[]
  } catch {
    return [] as ChatMessage[]
  }
}

function saveMessages(chatId: string, messages: ChatMessage[]) {
  localStorage.setItem(`${MESSAGE_KEY_PREFIX}${chatId}`, JSON.stringify(messages))
}

function summarizeSession(chatId: string, messages: ChatMessage[]): ChatSessionSummary {
  const firstUser = messages.find((message) => message.role === 'user')
  const lastMessage = messages[messages.length - 1]
  const title = firstUser?.content.slice(0, 14) || '新会话'
  const preview = lastMessage?.content.slice(0, 28) || '等待提问'

  return {
    id: chatId,
    title,
    preview,
    updatedAt: new Date().toISOString(),
  }
}

export const useChatStore = defineStore('chat', () => {
  // mode 会决定请求哪个后端接口（basic/rag/mcp）
  const mode = ref<ChatMode>('mcp')
  const status = ref<StreamStatus>('idle')
  // chatId 是后端会话记忆的关键字段，同一 chatId 才能延续上下文
  const chatId = ref(createId('chat'))
  const messages = ref<ChatMessage[]>([])
  const sessions = ref<ChatSessionSummary[]>([])
  const stopStreaming = ref<null | (() => void)>(null)

  const isStreaming = computed(() => status.value === 'streaming')
  const canRetry = computed(() => messages.value.some((item) => item.role === 'user') && !isStreaming.value)
  const quickPrompts = QUICK_PROMPTS

  function persistSession() {
    saveMessages(chatId.value, messages.value)

    const currentSummary = summarizeSession(chatId.value, messages.value)
    const targetIndex = sessions.value.findIndex((item) => item.id === chatId.value)
    if (targetIndex === -1) {
      sessions.value.unshift(currentSummary)
    } else {
      sessions.value.splice(targetIndex, 1)
      sessions.value.unshift(currentSummary)
    }
    localStorage.setItem(SESSION_KEY, JSON.stringify(sessions.value))
  }

  function createAssistantPlaceholder() {
    const id = createId('assistant')
    messages.value.push({
      id,
      role: 'assistant',
      content: '',
      createdAt: new Date().toISOString(),
      isStreaming: true,
    })
    return id
  }

  function patchMessage(id: string, patch: Partial<ChatMessage>) {
    const target = messages.value.find((item) => item.id === id)
    if (!target) {
      return
    }
    Object.assign(target, patch)
  }

  async function sendMessage(input: string) {
    const content = input.trim()
    if (!content || isStreaming.value) {
      return
    }

    status.value = 'streaming'
    messages.value.push({
      id: createId('user'),
      role: 'user',
      content,
      createdAt: new Date().toISOString(),
    })
    const assistantId = createAssistantPlaceholder()
    persistSession()

    let receivedChunk = false
    let stoppedByUser = false
    let streamError = ''

    await new Promise<void>((resolve) => {
      const stop = streamChat({
        mode: mode.value,
        message: content,
        chatId: chatId.value,
        onChunk: (chunk) => {
          receivedChunk = true
          // 把后端 SSE 分片逐段拼接成完整回复
          const previous = messages.value.find((item) => item.id === assistantId)?.content ?? ''
          patchMessage(assistantId, { content: `${previous}${chunk}` })
        },
        onError: (errorMessage) => {
          if (!stoppedByUser) {
            streamError = errorMessage
          }
        },
        onComplete: resolve,
      })

      stopStreaming.value = () => {
        stoppedByUser = true
        stop()
      }
    })

    stopStreaming.value = null
    patchMessage(assistantId, { isStreaming: false })

    if (!receivedChunk && !stoppedByUser && mode.value === 'basic') {
      try {
        // 仅 basic 模式下使用同步接口兜底，减少首包失败带来的空回复
        const fallback = await syncChat(content, chatId.value)
        patchMessage(assistantId, {
          content: fallback,
          isError: false,
        })
        streamError = ''
      } catch {
        streamError = '服务暂时不可用，请稍后重试。'
      }
    }

    if (stoppedByUser && !messages.value.find((item) => item.id === assistantId)?.content) {
      patchMessage(assistantId, { content: '已停止生成。' })
    }

    if (streamError && !receivedChunk) {
      patchMessage(assistantId, {
        content: streamError,
        isError: true,
      })
      status.value = 'error'
    } else {
      status.value = 'idle'
    }

    persistSession()
  }

  function stopMessage() {
    stopStreaming.value?.()
    stopStreaming.value = null
    status.value = 'idle'
  }

  function retryLastPrompt() {
    if (!canRetry.value) {
      return
    }
    const lastUserMessage = [...messages.value].reverse().find((item) => item.role === 'user')
    if (!lastUserMessage) {
      return
    }
    void sendMessage(lastUserMessage.content)
  }

  function bootstrap() {
    sessions.value = loadSessions()
    const latest = sessions.value[0]
    if (latest) {
      chatId.value = latest.id
      messages.value = loadMessages(latest.id)
      return
    }
    persistSession()
  }

  function startNewSession() {
    stopMessage()
    chatId.value = createId('chat')
    messages.value = []
    status.value = 'idle'
    persistSession()
  }

  function switchSession(targetChatId: string) {
    if (isStreaming.value) {
      return
    }
    // 切换本地会话时，后续发送会自动携带对应 chatId 给后端
    chatId.value = targetChatId
    messages.value = loadMessages(targetChatId)
    status.value = 'idle'
    persistSession()
  }

  function removeSession(targetChatId: string) {
    if (isStreaming.value && chatId.value === targetChatId) {
      stopMessage()
    }

    const targetIndex = sessions.value.findIndex((item) => item.id === targetChatId)
    if (targetIndex < 0) {
      return
    }
    sessions.value.splice(targetIndex, 1)
    localStorage.setItem(SESSION_KEY, JSON.stringify(sessions.value))
    localStorage.removeItem(`${MESSAGE_KEY_PREFIX}${targetChatId}`)

    if (chatId.value !== targetChatId) {
      return
    }
    const nextSession = sessions.value[0]
    if (nextSession) {
      switchSession(nextSession.id)
      return
    }
    startNewSession()
  }

  function setMode(nextMode: ChatMode) {
    mode.value = nextMode
  }

  return {
    mode,
    status,
    chatId,
    messages,
    sessions,
    quickPrompts,
    isStreaming,
    canRetry,
    bootstrap,
    sendMessage,
    stopMessage,
    retryLastPrompt,
    setMode,
    startNewSession,
    switchSession,
    removeSession,
  }
})
