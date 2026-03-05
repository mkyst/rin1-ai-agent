<script setup lang="ts">
import { nextTick, useTemplateRef, watch } from 'vue'
import ComposerBar from './ComposerBar.vue'
import MessageBubble from './MessageBubble.vue'
import type { ChatMessage } from '../../types/chat'

const draft = defineModel<string>('draft', { required: true })

const props = defineProps<{
  messages: ChatMessage[]
  isStreaming: boolean
  canRetry: boolean
  webSearchEnabled: boolean
}>()

const emit = defineEmits<{
  send: []
  stop: []
  retry: []
  toggleWebSearch: []
  openRolePlay: []
  openCompare: []
  revokeMessage: [messageId: string]
}>()

const streamRef = useTemplateRef<HTMLDivElement>('streamRef')

function scrollToBottom() {
  const target = streamRef.value
  if (!target) {
    return
  }
  target.scrollTo({
    top: target.scrollHeight,
    behavior: 'smooth',
  })
}

watch(
  () => props.messages.length,
  async () => {
    await nextTick()
    scrollToBottom()
  },
  { immediate: true },
)

watch(
  () => props.messages[props.messages.length - 1]?.content,
  async () => {
    await nextTick()
    scrollToBottom()
  },
)
</script>

<template>
  <section class="chat-panel panel">
    <header class="chat-panel__header">
      <div class="chat-panel__head-main">
        <h2 class="chat-panel__title">对话主区</h2>
        <p class="chat-panel__note">支持 SSE 流式输出，可中断、重试、切换模式</p>
      </div>

      <button type="button" class="compare-btn" @click="emit('openCompare')">
        <span class="compare-btn__dot" />
        三模式对比
      </button>
    </header>

    <div ref="streamRef" class="chat-stream">
      <MessageBubble
        v-for="message in messages"
        :key="message.id"
        :message="message"
        :allow-revoke="message.role === 'user'"
        @revoke="emit('revokeMessage', $event)"
      />
      <p v-if="messages.length === 0" class="chat-empty">
        先说一个你最近最困扰的关系场景，系统会按「分析 -> 建议 -> 下一步」回复。
      </p>
    </div>

    <ComposerBar
      v-model="draft"
      :disabled="isStreaming"
      :can-retry="canRetry"
      :web-search-enabled="webSearchEnabled"
      @send="emit('send')"
      @stop="emit('stop')"
      @retry="emit('retry')"
      @toggle-web-search="emit('toggleWebSearch')"
      @open-role-play="emit('openRolePlay')"
    />
  </section>
</template>

<style scoped>
.panel {
  border: 1px solid var(--line);
  border-radius: 18px;
  background: rgba(245, 239, 228, 0.9);
  box-shadow: var(--shadow);
}

.chat-panel {
  height: 100%;
  min-height: 0;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  overflow: hidden;
}

.chat-panel__header {
  padding: 1rem 1rem 0.85rem;
  border-bottom: 1px solid var(--line);
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: start;
  gap: 0.72rem;
}

.chat-panel__head-main {
  min-width: 0;
}

.chat-panel__title {
  margin: 0;
  font-family: 'Fraunces', Georgia, serif;
  font-size: 1.3rem;
}

.chat-panel__note {
  margin: 0.2rem 0 0;
  font-size: 0.82rem;
  color: var(--ink-soft);
}

.compare-btn {
  justify-self: end;
  align-self: center;
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 0.34rem 0.76rem;
  background: linear-gradient(145deg, rgba(255, 251, 243, 0.9), rgba(245, 239, 228, 0.9));
  color: var(--ink);
  font-size: 0.79rem;
  font-weight: 620;
  letter-spacing: 0.02em;
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  cursor: pointer;
  white-space: nowrap;
  box-shadow: 0 6px 14px rgba(73, 58, 42, 0.1);
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.compare-btn__dot {
  width: 0.4rem;
  height: 0.4rem;
  border-radius: 999px;
  background: var(--brand);
  box-shadow: 0 0 0 3px rgba(191, 76, 42, 0.15);
}

.compare-btn:hover {
  transform: translateY(-1px);
  border-color: #b9a688;
  box-shadow: 0 9px 18px rgba(73, 58, 42, 0.14);
}

.compare-btn:focus-visible {
  outline: 2px solid rgba(31, 109, 102, 0.42);
  outline-offset: 2px;
}

.chat-stream {
  min-height: 0;
  overflow: auto;
  padding: 0.95rem;
  display: grid;
  align-content: start;
  gap: 0.8rem;
}

.chat-empty {
  margin: 0;
  padding: 0.8rem;
  border: 1px dashed var(--line);
  border-radius: 12px;
  color: var(--ink-soft);
  background: rgba(255, 251, 243, 0.65);
}

@media (max-width: 680px) {
  .chat-panel__header {
    grid-template-columns: 1fr;
  }

  .compare-btn {
    justify-self: start;
  }
}
</style>
