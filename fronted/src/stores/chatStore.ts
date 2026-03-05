import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { streamChat, syncChat } from '../composables/useSseChat'
import type { ChatMessage, ChatMode, ChatSessionSummary, StreamStatus } from '../types/chat'

const SESSION_KEY = 'rin1.frontend.sessions'
const MESSAGE_KEY_PREFIX = 'rin1.frontend.messages.'
const WEB_SEARCH_KEY = 'rin1.frontend.web-search-enabled'

const QUICK_PROMPTS = [
  '对方最近总是已读不回，我应该怎么沟通？',
  '帮我准备一段既坚定又温柔的边界表达。',
  '我们因为小事频繁争吵，先从哪一步修复最有效？',
  '给我一个 7 天关系升温行动清单。',
]

const SESSION_TAG_RULES: Array<{ tag: string; keywords: string[] }> = [
  { tag: '沟通', keywords: ['沟通', '表达', '聊天', '对话', '已读不回'] },
  { tag: '冲突', keywords: ['争吵', '冲突', '冷战', '拉黑', '吵架'] },
  { tag: '边界', keywords: ['边界', '尊重', '控制', '拒绝', '压力'] },
  { tag: '修复', keywords: ['修复', '和好', '挽回', '复合', '缓和'] },
  { tag: '信任', keywords: ['信任', '怀疑', '安全感', '背叛'] },
  { tag: '成长', keywords: ['成长', '自我', '反思', '提升'] },
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
    const parsed = JSON.parse(raw) as Array<Partial<ChatSessionSummary>>
    return parsed.map((item) => ({
      id: item.id ?? createId('chat'),
      title: item.title ?? '新会话',
      preview: item.preview ?? '等待提问',
      tags: Array.isArray(item.tags) ? item.tags : [],
      updatedAt: item.updatedAt ?? new Date().toISOString(),
    }))
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

function extractSessionTags(messages: ChatMessage[]) {
  const text = messages.map((message) => message.content).join(' ').toLowerCase()
  const matched = SESSION_TAG_RULES.filter((rule) =>
    rule.keywords.some((keyword) => text.includes(keyword.toLowerCase())),
  ).map((rule) => rule.tag)
  return matched.slice(0, 3)
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
    tags: extractSessionTags(messages),
    updatedAt: new Date().toISOString(),
  }
}

export const useChatStore = defineStore('chat', () => {
  const mode = ref<ChatMode>('mcp')
  const status = ref<StreamStatus>('idle')
  const chatId = ref(createId('chat'))
  const messages = ref<ChatMessage[]>([])
  const sessions = ref<ChatSessionSummary[]>([])
  const stopStreaming = ref<null | (() => void)>(null)
  const webSearchEnabled = ref(localStorage.getItem(WEB_SEARCH_KEY) === '1')

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
    const requestMode: ChatMode = webSearchEnabled.value ? 'mcp' : mode.value

    await new Promise<void>((resolve) => {
      const stop = streamChat({
        mode: requestMode,
        message: content,
        chatId: chatId.value,
        onChunk: (chunk) => {
          receivedChunk = true
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

    if (!receivedChunk && !stoppedByUser && requestMode === 'basic') {
      try {
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

  function revokeUserMessage(messageId: string) {
    if (isStreaming.value) {
      stopMessage()
    }

    const index = messages.value.findIndex((item) => item.id === messageId)
    if (index < 0) {
      return
    }

    const target = messages.value[index]
    if (!target || target.role !== 'user') {
      return
    }

    let end = index + 1
    while (end < messages.value.length) {
      const nextMessage = messages.value[end]
      if (!nextMessage || nextMessage.role !== 'assistant') {
        break
      }
      end += 1
    }

    messages.value.splice(index, end - index)
    status.value = 'idle'
    persistSession()
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

  function toggleWebSearch() {
    webSearchEnabled.value = !webSearchEnabled.value
    localStorage.setItem(WEB_SEARCH_KEY, webSearchEnabled.value ? '1' : '0')
  }

  return {
    mode,
    webSearchEnabled,
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
    revokeUserMessage,
    setMode,
    toggleWebSearch,
    startNewSession,
    switchSession,
    removeSession,
  }
})
