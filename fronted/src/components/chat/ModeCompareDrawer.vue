<script setup lang="ts">
import type { CompareModeResult } from '../../types/chat'
import MarkdownContent from './MarkdownContent.vue'

const prompt = defineModel<string>('prompt', { required: true })

defineProps<{
  open: boolean
  running: boolean
  results: CompareModeResult[]
}>()

const emit = defineEmits<{
  close: []
  run: []
}>()

const MODE_LABEL: Record<string, string> = {
  basic: 'Base',
  rag: 'RAG',
  mcp: 'MCP',
}

function statusText(status: CompareModeResult['status']) {
  if (status === 'loading') {
    return '生成中'
  }
  if (status === 'success') {
    return '完成'
  }
  if (status === 'error') {
    return '失败'
  }
  return '待运行'
}
</script>

<template>
  <div v-if="open" class="compare-mask" @click.self="emit('close')">
    <section class="compare-drawer panel">
      <header class="compare-drawer__header">
        <div>
          <h3 class="compare-drawer__title">三模式对比</h3>
          <p class="compare-drawer__note">同一提问并行比较 Base / RAG / MCP 的回复差异</p>
        </div>
        <button type="button" class="compare-close" @click="emit('close')">关闭</button>
      </header>

      <div class="compare-input">
        <textarea
          v-model="prompt"
          class="compare-input__textarea"
          rows="3"
          placeholder="输入要对比的提问内容..."
        />
        <button type="button" class="compare-run" :disabled="running || !prompt.trim()" @click="emit('run')">
          {{ running ? '对比中...' : '开始对比' }}
        </button>
      </div>

      <div class="compare-grid">
        <article v-for="result in results" :key="result.mode" class="compare-card">
          <div class="compare-card__head">
            <p class="compare-card__mode">{{ MODE_LABEL[result.mode] ?? result.mode }}</p>
            <span class="compare-status" :class="`compare-status--${result.status}`">
              {{ statusText(result.status) }}
            </span>
          </div>

          <div class="compare-card__body">
            <p v-if="result.status === 'idle'" class="compare-placeholder">等待开始对比。</p>
            <p v-else-if="result.status === 'loading'" class="compare-placeholder">正在请求模型输出...</p>
            <p v-else-if="result.status === 'error'" class="compare-error">{{ result.error ?? '请求失败。' }}</p>
            <MarkdownContent v-else :content="result.content" />
          </div>
        </article>
      </div>
    </section>
  </div>
</template>

<style scoped>
.panel {
  border: 1px solid var(--line);
  border-radius: 18px;
  background: rgba(245, 239, 228, 0.96);
  box-shadow: var(--shadow);
}

.compare-mask {
  position: fixed;
  inset: 0;
  z-index: 70;
  background: rgba(32, 28, 21, 0.3);
  backdrop-filter: blur(2px);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 1rem;
}

.compare-drawer {
  width: min(1200px, 96vw);
  max-height: 92vh;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  overflow: hidden;
}

.compare-drawer__header {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  padding: 1rem 1rem 0.78rem;
  border-bottom: 1px solid var(--line);
}

.compare-drawer__title {
  margin: 0;
  font-family: 'Fraunces', Georgia, serif;
  font-size: 1.22rem;
}

.compare-drawer__note {
  margin: 0.24rem 0 0;
  color: var(--ink-soft);
  font-size: 0.82rem;
}

.compare-close {
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(245, 239, 228, 0.8);
  padding: 0.25rem 0.72rem;
  cursor: pointer;
}

.compare-input {
  border-bottom: 1px solid var(--line);
  padding: 0.78rem 1rem;
  display: grid;
  gap: 0.55rem;
}

.compare-input__textarea {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: rgba(255, 251, 243, 0.9);
  padding: 0.58rem 0.65rem;
  resize: vertical;
}

.compare-run {
  justify-self: end;
  border: none;
  border-radius: 10px;
  padding: 0.45rem 0.85rem;
  background: var(--ink);
  color: #fbf7ee;
  cursor: pointer;
}

.compare-run:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.compare-grid {
  min-height: 0;
  overflow: auto;
  padding: 0.8rem;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0.75rem;
}

.compare-card {
  border: 1px solid var(--line);
  border-radius: 14px;
  background: rgba(255, 251, 243, 0.88);
  display: grid;
  grid-template-rows: auto minmax(0, 1fr);
  min-height: 0;
}

.compare-card__head {
  border-bottom: 1px solid var(--line);
  padding: 0.62rem 0.7rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.compare-card__mode {
  margin: 0;
  font-weight: 700;
  letter-spacing: 0.04em;
}

.compare-status {
  border-radius: 999px;
  font-size: 0.68rem;
  padding: 0.06rem 0.42rem;
  border: 1px solid var(--line);
}

.compare-status--loading {
  background: rgba(31, 109, 102, 0.12);
  border-color: var(--teal);
  color: var(--teal);
}

.compare-status--success {
  background: rgba(45, 119, 56, 0.14);
  border-color: #2d7738;
  color: #2d7738;
}

.compare-status--error {
  background: rgba(202, 68, 63, 0.12);
  border-color: #ca443f;
  color: #8b2824;
}

.compare-card__body {
  min-height: 0;
  overflow: auto;
  padding: 0.65rem 0.7rem;
}

.compare-placeholder {
  margin: 0;
  color: var(--ink-soft);
  font-size: 0.85rem;
}

.compare-error {
  margin: 0;
  color: #8b2824;
  font-size: 0.85rem;
}

@media (max-width: 980px) {
  .compare-grid {
    grid-template-columns: 1fr;
  }
}
</style>
