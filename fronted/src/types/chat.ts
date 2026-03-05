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
  tags: string[]
  updatedAt: string
}

export interface InsightCard {
  title: string
  value: string
  detail: string
}

export interface RadarMetric {
  key: 'emotion' | 'conflict' | 'trust' | 'actionability'
  label: string
  value: number
}

export type CompareStatus = 'idle' | 'loading' | 'success' | 'error'

export interface CompareModeResult {
  mode: ChatMode
  status: CompareStatus
  content: string
  error?: string
}
