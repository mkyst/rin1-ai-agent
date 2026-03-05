import { computed, type Ref } from 'vue'
import type { ChatMessage, InsightCard, RadarMetric } from '../types/chat'

function countMatches(text: string, keywords: string[]) {
  return keywords.reduce((total, keyword) => (text.includes(keyword) ? total + 1 : total), 0)
}

function clamp(value: number, min = 10, max = 95) {
  return Math.min(max, Math.max(min, Math.round(value)))
}

function scoreEmotionIntensity(text: string) {
  const negative = countMatches(text, ['难过', '焦虑', '痛苦', '崩溃', '失望', '委屈', '生气'])
  const intensifier = countMatches(text, ['总是', '一直', '每次', '特别', '非常', '真的'])
  const soothing = countMatches(text, ['平静', '稳定', '温和', '理解', '缓和', '冷静'])
  return clamp(35 + negative * 12 + intensifier * 8 - soothing * 6)
}

function scoreConflictRisk(text: string) {
  const conflict = countMatches(text, ['争吵', '冷战', '拉黑', '分手', '冲突', '指责', '埋怨'])
  const escalation = countMatches(text, ['受不了', '忍不住', '爆发', '彻底', '没法', '算了'])
  const repair = countMatches(text, ['沟通', '修复', '和好', '倾听', '边界', '协商'])
  return clamp(30 + conflict * 14 + escalation * 7 - repair * 8)
}

function scoreTrustLevel(text: string) {
  const trustPositive = countMatches(text, ['信任', '坦诚', '尊重', '安全感', '一致', '稳定'])
  const trustNegative = countMatches(text, ['怀疑', '背叛', '欺骗', '隐瞒', '不安', '猜疑'])
  return clamp(45 + trustPositive * 10 - trustNegative * 12)
}

function scoreActionability(text: string) {
  const action = countMatches(text, ['建议', '步骤', '先', '然后', '可以', '今天', '本周', '清单', '练习'])
  const structure = countMatches(text, ['1.', '2.', '3.', '第一', '第二', '第三'])
  const vague = countMatches(text, ['可能', '大概', '也许', '不确定'])
  return clamp(30 + action * 11 + structure * 8 - vague * 6)
}

function moodLabel(score: number) {
  if (score >= 75) {
    return '情绪偏高'
  }
  if (score >= 50) {
    return '中等波动'
  }
  return '相对平稳'
}

function riskLabel(score: number) {
  if (score >= 70) {
    return '高风险'
  }
  if (score >= 45) {
    return '中风险'
  }
  return '低风险'
}

function actionLabel(score: number) {
  if (score >= 70) {
    return '可立即执行'
  }
  if (score >= 45) {
    return '可落地'
  }
  return '需细化'
}

function buildEmotionDetail(emotion: number) {
  if (emotion >= 75) {
    return '情绪起伏较大，建议先镜像对方感受，再表达自己的需求。'
  }
  if (emotion >= 50) {
    return '有波动但可沟通，先对齐感受，再进入问题与方案。'
  }
  return '情绪整体可控，适合用平和语气讨论边界和具体安排。'
}

function buildRiskDetail(conflict: number) {
  if (conflict >= 70) {
    return '冲突升级概率高，先暂停争议点，约定冷静后再谈。'
  }
  if (conflict >= 45) {
    return '存在误解升级风险，建议用“具体事件+感受+请求”表达。'
  }
  return '关系风险较低，可以围绕事实和行动直接推进沟通。'
}

function buildActionDetail(actionability: number, trust: number) {
  if (actionability >= 70) {
    return trust < 45
      ? '有执行条件，但需先补一句“理解你”的缓冲，再给出行动方案。'
      : '可直接执行：一句感受 + 一句需求 + 一句可选方案。'
  }

  if (actionability >= 45) {
    return '先把表达句式具体化，再约一个 10-15 分钟的短沟通窗口。'
  }

  return '先收敛目标，本轮沟通只解决一个小问题，再逐步推进。'
}

export function useInsights(messages: Ref<ChatMessage[]>) {
  const latestAssistantText = computed(() => {
    const latest = [...messages.value].reverse().find((message) => message.role === 'assistant')
    return latest?.content ?? ''
  })

  const latestUserText = computed(() => {
    const latest = [...messages.value].reverse().find((message) => message.role === 'user')
    return latest?.content ?? ''
  })

  const insightSource = computed(() => `${latestUserText.value}\n${latestAssistantText.value}`.trim())

  const radarMetrics = computed<RadarMetric[]>(() => {
    const text = insightSource.value
    const emotion = scoreEmotionIntensity(text)
    const conflict = scoreConflictRisk(text)
    const trust = scoreTrustLevel(text)
    const actionability = scoreActionability(text)

    return [
      { key: 'emotion', label: '情绪强度', value: emotion },
      { key: 'conflict', label: '冲突风险', value: conflict },
      { key: 'trust', label: '信任水平', value: trust },
      { key: 'actionability', label: '可执行度', value: actionability },
    ]
  })

  const insightCards = computed<InsightCard[]>(() => {
    const metrics = radarMetrics.value
    const emotion = metrics.find((item) => item.key === 'emotion')?.value ?? 35
    const conflict = metrics.find((item) => item.key === 'conflict')?.value ?? 30
    const trust = metrics.find((item) => item.key === 'trust')?.value ?? 45
    const actionability = metrics.find((item) => item.key === 'actionability')?.value ?? 30

    return [
      {
        title: '情绪温度',
        value: `${moodLabel(emotion)} (${emotion})`,
        detail: buildEmotionDetail(emotion),
      },
      {
        title: '关系风险',
        value: `${riskLabel(conflict)} (${conflict})`,
        detail: buildRiskDetail(conflict),
      },
      {
        title: '行动建议',
        value: `${actionLabel(actionability)} (${actionability})`,
        detail: buildActionDetail(actionability, trust),
      },
    ]
  })

  return {
    insightCards,
    radarMetrics,
  }
}
