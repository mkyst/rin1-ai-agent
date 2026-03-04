export type ChatMode = 'basic' | 'rag' | 'mcp'
export type ChatRole = 'user' | 'assistant'
export type StreamStatus = 'idle' | 'streaming' | 'error'

export interface ChatMessage {
  id: string
  role: ChatRole
  content: string
  createdAt: string
  isStreaming?: boolean
  isError?: boolean
}

export interface ChatSessionSummary {
  id: string
  title: string
  preview: string
  updatedAt: string
}

export interface InsightCard {
  title: string
  value: string
  detail: string
}
