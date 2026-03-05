<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import AppHeader from '../components/layout/AppHeader.vue'
import SessionSidebar from '../components/sidebar/SessionSidebar.vue'
import ChatPanel from '../components/chat/ChatPanel.vue'
import InsightPanel from '../components/sidebar/InsightPanel.vue'
import ModeCompareDrawer from '../components/chat/ModeCompareDrawer.vue'
import RolePlayDrawer from '../components/chat/RolePlayDrawer.vue'
import { useChatStore } from '../stores/chatStore'
import { useInsights } from '../composables/useInsights'
import { collectStreamChat, syncChat } from '../composables/useSseChat'
import type { ChatMode, CompareModeResult } from '../types/chat'

const chatStore = useChatStore()
const draft = ref('')
const showBackToTop = ref(false)

const compareOpen = ref(false)
const comparePrompt = ref('')
const compareRunning = ref(false)
const compareResults = ref<CompareModeResult[]>([
  { mode: 'basic', status: 'idle', content: '' },
  { mode: 'rag', status: 'idle', content: '' },
  { mode: 'mcp', status: 'idle', content: '' },
])

const rolePlayOpen = ref(false)
const rolePlayScenario = ref('场景：对方最近经常已读不回，我想平和表达感受并约一次沟通。')
const rolePlayUserLine = ref('')
const rolePlayRunning = ref(false)
const rolePlayTurns = ref<Array<{ id: string; user: string; partner: string }>>([])
const rolePlayChatId = ref(`${chatStore.chatId}-roleplay`)

const { insightCards, radarMetrics } = useInsights(computed(() => chatStore.messages))

function handleModeChange(mode: ChatMode) {
  chatStore.setMode(mode)
}

async function handleSend() {
  const content = draft.value.trim()
  if (!content) {
    return
  }
  draft.value = ''
  await chatStore.sendMessage(content)
}

function applyPrompt(prompt: string) {
  draft.value = prompt
}

function updateBackToTopVisibility() {
  showBackToTop.value = window.scrollY > 280
}

function backToTop() {
  window.scrollTo({
    top: 0,
    behavior: 'smooth',
  })
}

function resetCompareResults() {
  compareResults.value = [
    { mode: 'basic', status: 'idle', content: '' },
    { mode: 'rag', status: 'idle', content: '' },
    { mode: 'mcp', status: 'idle', content: '' },
  ]
}

function updateCompareResult(mode: ChatMode, patch: Partial<CompareModeResult>) {
  const target = compareResults.value.find((item) => item.mode === mode)
  if (!target) {
    return
  }
  Object.assign(target, patch)
}

function buildCompareChatId(mode: ChatMode) {
  return `${chatStore.chatId}-cmp-${mode}-${Date.now()}-${Math.floor(Math.random() * 1000)}`
}

function openCompareDrawer() {
  compareOpen.value = true
  if (!comparePrompt.value.trim()) {
    const latestUserMessage = [...chatStore.messages].reverse().find((message) => message.role === 'user')
    comparePrompt.value = draft.value.trim() || latestUserMessage?.content || ''
  }
  if (!compareRunning.value) {
    resetCompareResults()
  }
}

function openRolePlayDrawer() {
  rolePlayOpen.value = true
}

function closeRolePlayDrawer() {
  rolePlayOpen.value = false
}

function clearRolePlayTurns() {
  rolePlayTurns.value = []
  rolePlayChatId.value = `${chatStore.chatId}-roleplay-${Date.now()}`
}

async function runRolePlayTurn() {
  const scenario = rolePlayScenario.value.trim()
  const userLine = rolePlayUserLine.value.trim()
  if (!scenario || !userLine || rolePlayRunning.value) {
    return
  }

  rolePlayRunning.value = true
  const transcript = rolePlayTurns.value
    .map((turn, index) => `第${index + 1}轮\\n我：${turn.user}\\n对方：${turn.partner}`)
    .join('\\n\\n')

  const prompt = [
    '你正在做“对话预演”的角色扮演。',
    '你扮演“对方”，我扮演“我”。',
    `场景：${scenario}`,
    transcript ? `历史对话：\\n${transcript}` : '这是第一轮。',
    `我这次说：${userLine}`,
    '请只回复“对方”这一句，语气自然、简短、可继续对话，不要解释。'
  ].join('\\n\\n')

  try {
    const reply = await syncChat(prompt, rolePlayChatId.value)
    const cleanedReply = reply.replace(/^对方[:：]\\s*/u, '').trim()
    rolePlayTurns.value.push({
      id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
      user: userLine,
      partner: cleanedReply || '...',
    })
    rolePlayUserLine.value = ''
  } catch {
    rolePlayTurns.value.push({
      id: `${Date.now()}-${Math.random().toString(16).slice(2)}`,
      user: userLine,
      partner: '生成失败，请稍后重试。',
    })
  } finally {
    rolePlayRunning.value = false
  }
}

function closeCompareDrawer() {
  compareOpen.value = false
}

async function runModeComparison() {
  const prompt = comparePrompt.value.trim()
  if (!prompt || compareRunning.value) {
    return
  }

  compareRunning.value = true
  resetCompareResults()
  for (const mode of ['basic', 'rag', 'mcp'] as ChatMode[]) {
    updateCompareResult(mode, { status: 'loading', content: '', error: undefined })
  }

  await Promise.all(
    (['basic', 'rag', 'mcp'] as ChatMode[]).map(async (mode) => {
      try {
        const compareChatId = buildCompareChatId(mode)
        const content =
          mode === 'basic'
            ? await syncChat(prompt, compareChatId)
            : await collectStreamChat(mode, prompt, compareChatId)

        updateCompareResult(mode, {
          status: 'success',
          content,
          error: undefined,
        })
      } catch (error) {
        updateCompareResult(mode, {
          status: 'error',
          content: '',
          error: error instanceof Error ? error.message : '请求失败，请稍后重试。',
        })
      }
    }),
  )

  compareRunning.value = false
}

onMounted(() => {
  chatStore.bootstrap()
  updateBackToTopVisibility()
  window.addEventListener('scroll', updateBackToTopVisibility, { passive: true })
})

onUnmounted(() => {
  window.removeEventListener('scroll', updateBackToTopVisibility)
})
</script>

<template>
  <main class="workbench">
    <AppHeader
      class="workbench__header reveal reveal--1"
      :mode="chatStore.mode"
      :status="chatStore.status"
      :chat-id="chatStore.chatId"
      @mode-change="handleModeChange"
      @new-session="chatStore.startNewSession"
    />

    <section class="workbench__body">
      <SessionSidebar
        class="reveal reveal--2"
        :sessions="chatStore.sessions"
        :current-chat-id="chatStore.chatId"
        @select="chatStore.switchSession"
        @remove="chatStore.removeSession"
      />

      <ChatPanel
        class="reveal reveal--3"
        v-model:draft="draft"
        :messages="chatStore.messages"
        :is-streaming="chatStore.isStreaming"
        :can-retry="chatStore.canRetry"
        :web-search-enabled="chatStore.webSearchEnabled"
        @send="handleSend"
        @stop="chatStore.stopMessage"
        @retry="chatStore.retryLastPrompt"
        @toggle-web-search="chatStore.toggleWebSearch"
        @open-role-play="openRolePlayDrawer"
        @revoke-message="chatStore.revokeUserMessage"
        @open-compare="openCompareDrawer"
      />

      <InsightPanel
        class="reveal reveal--4"
        :cards="insightCards"
        :radar-metrics="radarMetrics"
        :quick-prompts="chatStore.quickPrompts"
        @use-prompt="applyPrompt"
      />
    </section>

    <ModeCompareDrawer
      v-model:prompt="comparePrompt"
      :open="compareOpen"
      :running="compareRunning"
      :results="compareResults"
      @close="closeCompareDrawer"
      @run="runModeComparison"
    />

    <RolePlayDrawer
      v-model:scenario="rolePlayScenario"
      v-model:user-line="rolePlayUserLine"
      :open="rolePlayOpen"
      :running="rolePlayRunning"
      :history="rolePlayTurns"
      @close="closeRolePlayDrawer"
      @clear="clearRolePlayTurns"
      @run="runRolePlayTurn"
    />

    <button
      v-show="showBackToTop"
      type="button"
      class="back-to-top"
      aria-label="回到页面顶部"
      @click="backToTop"
    >
      ↑
    </button>
  </main>
</template>

<style scoped>
.workbench {
  height: 100dvh;
  min-height: 100dvh;
  padding: clamp(0.8rem, 1.8vw, 1.4rem);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  gap: 0.92rem;
  overflow: hidden;
}

.workbench__body {
  display: grid;
  grid-template-columns: minmax(220px, 260px) minmax(0, 1fr) minmax(260px, 320px);
  gap: 0.92rem;
  min-height: 0;
}

.workbench__body > * {
  min-height: 0;
}

.reveal {
  opacity: 0;
  transform: translateY(12px);
  animation: reveal-up 520ms cubic-bezier(0.2, 0.82, 0.25, 1) forwards;
}

.reveal--1 {
  animation-delay: 80ms;
}

.reveal--2 {
  animation-delay: 130ms;
}

.reveal--3 {
  animation-delay: 190ms;
}

.reveal--4 {
  animation-delay: 250ms;
}

@keyframes reveal-up {
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@media (max-width: 1120px) {
  .workbench__body {
    grid-template-columns: minmax(220px, 250px) minmax(0, 1fr);
  }
}

@media (max-width: 920px) {
  .workbench {
    height: auto;
    min-height: auto;
    overflow: visible;
  }

  .workbench__body {
    grid-template-columns: 1fr;
  }
}

.back-to-top {
  position: fixed;
  right: clamp(0.75rem, 2vw, 1.25rem);
  bottom: clamp(1rem, 3vw, 1.6rem);
  width: 2.75rem;
  height: 2.75rem;
  border: 1px solid rgba(32, 28, 21, 0.18);
  border-radius: 999px;
  background: linear-gradient(140deg, rgba(32, 28, 21, 0.95), rgba(31, 109, 102, 0.94));
  color: #fdf8ef;
  font-size: 1.1rem;
  font-weight: 700;
  line-height: 1;
  box-shadow: 0 12px 28px rgba(32, 28, 21, 0.24);
  cursor: pointer;
  z-index: 40;
  transition: transform 200ms ease, box-shadow 200ms ease, opacity 200ms ease;
}

.back-to-top:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 32px rgba(32, 28, 21, 0.3);
}

.back-to-top:focus-visible {
  outline: 2px solid rgba(31, 109, 102, 0.65);
  outline-offset: 3px;
}
</style>
