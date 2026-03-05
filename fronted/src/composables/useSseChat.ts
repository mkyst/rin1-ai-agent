import type { ChatMode } from '../types/chat'

interface StreamChatOptions {
  mode: ChatMode
  message: string
  chatId: string
  onChunk: (chunk: string) => void
  onError?: (message: string) => void
  onComplete?: () => void
}

const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8123/api'

const STREAM_ENDPOINT_MAP: Record<ChatMode, string> = {
  basic: '/ai/love_app/chat/stream',
  rag: '/ai/love_app/chat/stream/rag',
  mcp: '/ai/love_app/chat/stream/mcp',
}

const SYNC_ENDPOINT = '/ai/love_app/chat/sync'

function buildUrl(endpoint: string, message: string, chatId: string) {
  const params = new URLSearchParams({
    message,
    chatId,
  })
  return `${API_BASE}${endpoint}?${params.toString()}`
}

export function streamChat(options: StreamChatOptions) {
  const url = buildUrl(STREAM_ENDPOINT_MAP[options.mode], options.message, options.chatId)
  const eventSource = new EventSource(url)
  let disposed = false
  let receivedAnyData = false

  const finalize = () => {
    if (disposed) {
      return
    }
    disposed = true
    eventSource.close()
    options.onComplete?.()
  }

  eventSource.onmessage = (event) => {
    receivedAnyData = true
    options.onChunk(event.data)
  }

  eventSource.onerror = () => {
    if (!receivedAnyData) {
      options.onError?.('流式连接失败，请稍后重试。')
    }
    finalize()
  }

  return finalize
}

export async function syncChat(message: string, chatId: string) {
  const url = buildUrl(SYNC_ENDPOINT, message, chatId)
  const response = await fetch(url)

  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`)
  }
  return response.text()
}

export function collectStreamChat(mode: ChatMode, message: string, chatId: string, timeoutMs = 60000) {
  return new Promise<string>((resolve, reject) => {
    let result = ''
    let finished = false

    const timer = window.setTimeout(() => {
      if (finished) {
        return
      }
      finished = true
      stop()
      reject(new Error('请求超时，请稍后重试。'))
    }, timeoutMs)

    const stop = streamChat({
      mode,
      message,
      chatId,
      onChunk: (chunk) => {
        result += chunk
      },
      onError: (errorMessage) => {
        if (finished) {
          return
        }
        finished = true
        window.clearTimeout(timer)
        reject(new Error(errorMessage))
      },
      onComplete: () => {
        if (finished) {
          return
        }
        finished = true
        window.clearTimeout(timer)
        if (!result.trim()) {
          reject(new Error('没有收到模型回复。'))
          return
        }
        resolve(result)
      },
    })
  })
}
