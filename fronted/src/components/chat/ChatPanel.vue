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
}>()

const emit = defineEmits<{
  send: []
  stop: []
  retry: []
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
      <h2 class="chat-panel__title">对话主区</h2>
      <p class="chat-panel__note">支持 SSE 流式输出，可中断、重试、切换模式</p>
    </header>

    <div ref="streamRef" class="chat-stream">
      <MessageBubble v-for="message in messages" :key="message.id" :message="message" />
      <p v-if="messages.length === 0" class="chat-empty">
        先说一个你最近最困扰的关系场景，系统会按「分析 -> 建议 -> 下一步」回复。
      </p>
    </div>

    <ComposerBar
      v-model="draft"
      :disabled="isStreaming"
      :can-retry="canRetry"
      @send="emit('send')"
      @stop="emit('stop')"
      @retry="emit('retry')"
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
</style>
