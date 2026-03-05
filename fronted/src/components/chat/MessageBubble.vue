<script setup lang="ts">
import { ref } from 'vue'
import type { ChatMessage } from '../../types/chat'
import MarkdownContent from './MarkdownContent.vue'

const props = defineProps<{
  message: ChatMessage
  allowRevoke?: boolean
}>()

const emit = defineEmits<{
  revoke: [messageId: string]
}>()

const copied = ref(false)
let copiedTimer: number | undefined

async function copyContent() {
  const text = props.message.content?.trim()
  if (!text) {
    return
  }

  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(text)
    } else {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
    }
    copied.value = true
    if (copiedTimer) {
      window.clearTimeout(copiedTimer)
    }
    copiedTimer = window.setTimeout(() => {
      copied.value = false
    }, 1300)
  } catch {
    copied.value = false
  }
}

function onRevoke() {
  emit('revoke', props.message.id)
}
</script>

<template>
  <article class="bubble-wrap" :class="`bubble-wrap--${message.role}`">
    <div class="bubble" :class="{ 'bubble--error': message.isError }">
      <p class="bubble__meta">
        {{ message.role === 'assistant' ? '顾问助手' : '你' }}
      </p>

      <div class="bubble__content">
        <MarkdownContent :content="message.content || (message.isStreaming ? '...' : '')" />
      </div>

      <div class="bubble__actions">
        <button type="button" class="bubble-action" @click="copyContent">
          {{ copied ? '已复制' : '复制' }}
        </button>
        <button
          v-if="message.role === 'user'"
          type="button"
          class="bubble-action bubble-action--warn"
          :disabled="!allowRevoke"
          @click="onRevoke"
        >
          撤回
        </button>
      </div>
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
  margin-top: 0.26rem;
  line-height: 1.54;
}

.bubble__actions {
  margin-top: 0.45rem;
  display: flex;
  justify-content: flex-end;
  gap: 0.34rem;
}

.bubble-action {
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(245, 239, 228, 0.82);
  color: var(--ink-soft);
  font-size: 0.7rem;
  padding: 0.12rem 0.5rem;
  cursor: pointer;
}

.bubble-action:hover:not(:disabled) {
  border-color: #b9a688;
}

.bubble-action--warn {
  color: #8f5847;
}

.bubble-action:disabled {
  opacity: 0.5;
  cursor: not-allowed;
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
