<script setup lang="ts">
const model = defineModel<string>({ required: true })

defineProps<{
  disabled: boolean
  canRetry: boolean
}>()

const emit = defineEmits<{
  send: []
  stop: []
  retry: []
}>()

function handleEnter(event: KeyboardEvent) {
  if (event.shiftKey) {
    return
  }
  event.preventDefault()
  emit('send')
}
</script>

<template>
  <div class="composer">
    <textarea
      v-model="model"
      class="composer__input"
      rows="3"
      placeholder="把你的关系问题具体描述出来，助手会先拆解问题再给行动方案。"
      @keydown.enter="handleEnter"
    />
    <div class="composer__actions">
      <button
        type="button"
        class="composer__btn composer__btn--secondary"
        :disabled="disabled || !canRetry"
        @click="emit('retry')"
      >
        重试上轮
      </button>
      <button
        type="button"
        class="composer__btn composer__btn--warn"
        :disabled="!disabled"
        @click="emit('stop')"
      >
        停止
      </button>
      <button
        type="button"
        class="composer__btn composer__btn--primary"
        :disabled="disabled || !model.trim()"
        @click="emit('send')"
      >
        发送
      </button>
    </div>
    <p v-if="!canRetry" class="composer__hint">发送过至少一条消息后可使用重试。</p>
  </div>
</template>

<style scoped>
.composer {
  border-top: 1px solid var(--line);
  padding: 0.85rem;
  background: rgba(245, 239, 228, 0.86);
}

.composer__input {
  width: 100%;
  border-radius: 14px;
  border: 1px solid var(--line);
  padding: 0.72rem 0.78rem;
  resize: vertical;
  min-height: 84px;
  background: rgba(255, 251, 243, 0.84);
}

.composer__input:focus {
  outline: 2px solid rgba(31, 109, 102, 0.34);
  border-color: var(--teal);
}

.composer__actions {
  margin-top: 0.62rem;
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
}

.composer__btn {
  border: none;
  border-radius: 10px;
  padding: 0.45rem 0.82rem;
  cursor: pointer;
  transition: transform 180ms ease;
}

.composer__btn:disabled {
  cursor: not-allowed;
  opacity: 0.45;
}

.composer__btn:not(:disabled):hover {
  transform: translateY(-1px);
}

.composer__btn--secondary {
  background: #e0d2bf;
}

.composer__btn--warn {
  background: rgba(202, 68, 63, 0.18);
  color: #7d2520;
}

.composer__btn--primary {
  background: var(--ink);
  color: #fbf7ee;
}

.composer__hint {
  margin: 0.45rem 0 0;
  font-size: 0.73rem;
  color: var(--ink-soft);
}
</style>
