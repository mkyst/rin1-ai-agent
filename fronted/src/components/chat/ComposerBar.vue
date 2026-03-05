<script setup lang="ts">
const model = defineModel<string>({ required: true })

defineProps<{
  disabled: boolean
  canRetry: boolean
  webSearchEnabled: boolean
}>()

const emit = defineEmits<{
  send: []
  stop: []
  retry: []
  toggleWebSearch: []
  openRolePlay: []
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

    <div class="composer__footer">
      <div class="composer__left-tools">
        <button
          type="button"
          class="composer__search-btn"
          :class="{ 'composer__search-btn--active': webSearchEnabled }"
          :disabled="disabled"
          :aria-pressed="webSearchEnabled"
          @click="emit('toggleWebSearch')"
        >
          <span class="composer__search-dot" />
          联网搜索
        </button>
        <button
          type="button"
          class="composer__roleplay-btn"
          :disabled="disabled"
          @click="emit('openRolePlay')"
        >
          角色扮演
        </button>
      </div>

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

.composer__footer {
  margin-top: 0.62rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.6rem;
}

.composer__left-tools {
  display: flex;
  align-items: center;
  gap: 0.45rem;
}

.composer__search-btn {
  border: 1px solid var(--line);
  border-radius: 999px;
  padding: 0.42rem 0.72rem;
  background: rgba(255, 251, 243, 0.88);
  color: var(--ink);
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  cursor: pointer;
  transition: transform 180ms ease, border-color 180ms ease, box-shadow 180ms ease;
}

.composer__search-btn:not(:disabled):hover {
  transform: translateY(-1px);
  border-color: #b9a688;
  box-shadow: 0 7px 16px rgba(73, 58, 42, 0.14);
}

.composer__search-btn--active {
  border-color: rgba(31, 109, 102, 0.54);
  background: rgba(31, 109, 102, 0.13);
}

.composer__search-dot {
  width: 0.44rem;
  height: 0.44rem;
  border-radius: 999px;
  background: #a7a7a7;
}

.composer__search-btn--active .composer__search-dot {
  background: var(--teal);
}

.composer__roleplay-btn {
  border: 1px solid rgba(73, 58, 42, 0.28);
  border-radius: 999px;
  padding: 0.42rem 0.72rem;
  background: rgba(255, 251, 243, 0.9);
  color: var(--ink);
  cursor: pointer;
  transition: transform 180ms ease, box-shadow 180ms ease, border-color 180ms ease;
}

.composer__roleplay-btn:not(:disabled):hover {
  transform: translateY(-1px);
  border-color: #b9a688;
  box-shadow: 0 7px 16px rgba(73, 58, 42, 0.14);
}

.composer__actions {
  margin-top: 0;
  display: flex;
  justify-content: flex-start;
  gap: 0.5rem;
}

.composer__btn {
  border: none;
  border-radius: 10px;
  padding: 0.45rem 0.82rem;
  cursor: pointer;
  transition: transform 180ms ease;
}

.composer__btn:disabled,
.composer__search-btn:disabled,
.composer__roleplay-btn:disabled {
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

@media (max-width: 680px) {
  .composer__footer {
    flex-direction: column;
    align-items: stretch;
  }

  .composer__left-tools {
    width: 100%;
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .composer__actions {
    justify-content: flex-end;
    flex-wrap: wrap;
  }
}
</style>
