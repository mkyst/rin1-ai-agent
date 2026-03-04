import { computed, type Ref } from 'vue'
import type { ChatMessage, InsightCard } from '../types/chat'

function containsAny(text: string, keywords: string[]) {
  return keywords.some((keyword) => text.includes(keyword))
}

function inferMood(text: string) {
  if (containsAny(text, ['难过', '焦虑', '痛苦', '崩溃', '害怕'])) {
    return '敏感波动'
  }
  if (containsAny(text, ['开心', '稳定', '信任', '温暖', '轻松'])) {
    return '稳定升温'
  }
  return '理性协商'
}

function inferRisk(text: string) {
  if (containsAny(text, ['分手', '冷战', '拉黑', '争吵', '失望'])) {
    return '高优先级'
  }
  if (containsAny(text, ['误解', '沟通', '压力', '边界'])) {
    return '中优先级'
  }
  return '低优先级'
}

function inferAction(text: string) {
  if (containsAny(text, ['建议', '你可以', '下一步', '先'])) {
    return '可立即执行'
  }
  return '等待细化'
}

function shortText(value: string, fallback: string) {
  if (!value) {
    return fallback
  }
  return value.length > 64 ? `${value.slice(0, 64)}...` : value
}

export function useInsights(messages: Ref<ChatMessage[]>) {
  const latestAssistantText = computed(() => {
    const latest = [...messages.value].reverse().find((message) => message.role === 'assistant')
    return latest?.content ?? ''
  })

  const insightCards = computed<InsightCard[]>(() => {
    const text = latestAssistantText.value

    return [
      {
        title: '情绪温度',
        value: inferMood(text),
        detail: shortText(text, '等待模型回复后生成判断。'),
      },
      {
        title: '关系风险',
        value: inferRisk(text),
        detail: '根据对话关键词自动估计冲突升级概率。',
      },
      {
        title: '行动建议',
        value: inferAction(text),
        detail: text ? '建议优先执行一条低阻力行动，再观察反馈。' : '先发起一次提问以生成建议。',
      },
    ]
  })

  return {
    insightCards,
  }
}
