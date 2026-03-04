<script setup lang="ts">
import type { ChatMessage } from '../../types/chat'

defineProps<{
  message: ChatMessage
}>()
</script>

<template>
  <article class="bubble-wrap" :class="`bubble-wrap--${message.role}`">
    <div class="bubble" :class="{ 'bubble--error': message.isError }">
      <p class="bubble__meta">
        {{ message.role === 'assistant' ? '顾问助手' : '你' }}
      </p>
      <p class="bubble__content">{{ message.content || (message.isStreaming ? '...' : '') }}</p>
    </div>
  </article>
</template>

<style scoped>
.bubble-wrap {
  display: flex;
  animation: bubble-enter 320ms cubic-bezier(0.16, 0.82, 0.24, 1) both;
}

.bubble-wrap--user {
  justify-content: flex-end;
}

.bubble-wrap--assistant {
  justify-content: flex-start;
}

.bubble {
  max-width: min(86%, 680px);
  border: 1px solid var(--line);
  border-radius: 18px;
  padding: 0.8rem 0.95rem;
  background: rgba(255, 251, 243, 0.9);
  box-shadow: 0 10px 26px rgba(73, 58, 42, 0.09);
}

.bubble-wrap--user .bubble {
  background: var(--brand-soft);
  border-color: #dba693;
}

.bubble--error {
  border-color: #ca443f;
  background: rgba(232, 152, 152, 0.24);
}

.bubble__meta {
  margin: 0;
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--ink-soft);
}

.bubble__content {
  margin: 0.26rem 0 0;
  white-space: pre-wrap;
  line-height: 1.54;
}

@keyframes bubble-enter {
  from {
    opacity: 0;
    transform: translateY(8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>
