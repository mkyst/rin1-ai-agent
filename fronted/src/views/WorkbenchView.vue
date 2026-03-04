<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import AppHeader from '../components/layout/AppHeader.vue'
import SessionSidebar from '../components/sidebar/SessionSidebar.vue'
import ChatPanel from '../components/chat/ChatPanel.vue'
import InsightPanel from '../components/sidebar/InsightPanel.vue'
import { useChatStore } from '../stores/chatStore'
import { useInsights } from '../composables/useInsights'
import type { ChatMode } from '../types/chat'

const chatStore = useChatStore()
const draft = ref('')
const showBackToTop = ref(false)

const { insightCards } = useInsights(computed(() => chatStore.messages))

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
        @send="handleSend"
        @stop="chatStore.stopMessage"
        @retry="chatStore.retryLastPrompt"
      />

      <InsightPanel
        class="reveal reveal--4"
        :cards="insightCards"
        :quick-prompts="chatStore.quickPrompts"
        @use-prompt="applyPrompt"
      />
    </section>

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
