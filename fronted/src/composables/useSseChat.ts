import type { ChatMode } from '../types/chat'

interface StreamChatOptions {
  mode: ChatMode
  message: string
  chatId: string
  onChunk: (chunk: string) => void
  onError?: (message: string) => void
  onComplete?: () => void
}

// 后端 API 根地址：优先读取 .env 中的 VITE_API_BASE_URL
const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8123/api'
const STREAM_ENDPOINT_MAP: Record<ChatMode, string> = {
  // 三种模式分别映射到后端的三个 SSE 接口
  basic: '/ai/love_app/chat/stream',
  rag: '/ai/love_app/chat/stream/rag',
  mcp: '/ai/love_app/chat/stream/mcp',
}

// 基础模式的同步兜底接口（非 SSE）
const SYNC_ENDPOINT = '/ai/love_app/chat/sync'

function buildUrl(endpoint: string, message: string, chatId: string) {
  const params = new URLSearchParams({
    // 与后端 AiController 入参保持一致
    message,
    chatId,
  })
  return `${API_BASE}${endpoint}?${params.toString()}`
}

export function streamChat(options: StreamChatOptions) {
  const url = buildUrl(STREAM_ENDPOINT_MAP[options.mode], options.message, options.chatId)
  // EventSource 用于消费 text/event-stream 流式输出
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
    // 后端每次推送一段 data，前端把分片持续拼接到消息气泡
    receivedAnyData = true
    options.onChunk(event.data)
  }

  eventSource.onerror = () => {
    // 未收到任何分片就断流，视为连接失败
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
