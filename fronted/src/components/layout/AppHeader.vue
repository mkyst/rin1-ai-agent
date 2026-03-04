<script setup lang="ts">
import type { ChatMode, StreamStatus } from '../../types/chat'

defineProps<{
  mode: ChatMode
  chatId: string
  status: StreamStatus
}>()

const emit = defineEmits<{
  modeChange: [mode: ChatMode]
  newSession: []
}>()

const MODE_OPTIONS: Array<{ value: ChatMode; label: string; description: string }> = [
  { value: 'basic', label: 'Base', description: '标准对话' },
  { value: 'rag', label: 'RAG', description: '知识增强' },
  { value: 'mcp', label: 'MCP', description: '工具调用' },
]
</script>

<template>
  <header class="app-header">
    <div class="app-header__brand">
      <p class="app-header__eyebrow">Rin1 Relationship Console</p>
      <h1 class="app-header__title">AI 恋爱顾问工作台</h1>
      <p class="app-header__subtitle">流式分析 · 关系修复 · 行动建议</p>
    </div>

    <div class="app-header__controls">
      <div class="app-header__modes">
        <button
          v-for="item in MODE_OPTIONS"
          :key="item.value"
          type="button"
          class="mode-btn"
          :class="{ 'mode-btn--active': item.value === mode }"
          @click="emit('modeChange', item.value)"
        >
          <span class="mode-btn__label">{{ item.label }}</span>
          <span class="mode-btn__desc">{{ item.description }}</span>
        </button>
      </div>

      <div class="app-header__meta">
        <div class="status-chip" :class="`status-chip--${status}`">
          {{ status === 'streaming' ? '生成中' : status === 'error' ? '连接异常' : '待命' }}
        </div>
        <p class="chat-id" :title="chatId"># {{ chatId.slice(0, 18) }}</p>
        <button type="button" class="new-session-btn" @click="emit('newSession')">新会话</button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.app-header {
  position: relative;
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 1.25rem;
  padding: 1.2rem 1.4rem;
  border: 1px solid var(--line);
  border-radius: 20px;
  background: linear-gradient(125deg, rgba(245, 239, 228, 0.96), rgba(237, 227, 210, 0.78));
  box-shadow: var(--shadow);
  overflow: hidden;
}

.app-header::before {
  content: '';
  position: absolute;
  inset: 0;
  background:
    linear-gradient(102deg, transparent 0 56%, rgba(191, 76, 42, 0.08) 76%, rgba(31, 109, 102, 0.12)),
    repeating-linear-gradient(
      13deg,
      rgba(183, 166, 143, 0.2) 0 1px,
      transparent 1px 26px
    );
  pointer-events: none;
}

.app-header__brand,
.app-header__controls {
  position: relative;
  z-index: 1;
}

.app-header__eyebrow {
  margin: 0;
  font-size: 0.75rem;
  letter-spacing: 0.2em;
  text-transform: uppercase;
  color: var(--ink-soft);
}

.app-header__title {
  margin: 0.3rem 0 0;
  font-family: 'Fraunces', Georgia, serif;
  font-size: clamp(1.45rem, 2.6vw, 2.1rem);
  line-height: 1.08;
}

.app-header__subtitle {
  margin: 0.4rem 0 0;
  color: var(--ink-soft);
  font-size: 0.92rem;
}

.app-header__controls {
  display: flex;
  gap: 1rem;
  align-items: center;
}

.app-header__modes {
  display: inline-flex;
  gap: 0.45rem;
}

.mode-btn {
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 0.45rem 0.72rem;
  display: grid;
  gap: 0.08rem;
  background: rgba(245, 239, 228, 0.75);
  transition: all 220ms ease;
  cursor: pointer;
}

.mode-btn:hover {
  border-color: var(--brand);
  transform: translateY(-2px);
}

.mode-btn__label {
  font-size: 0.77rem;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.mode-btn__desc {
  color: var(--ink-soft);
  font-size: 0.75rem;
}

.mode-btn--active {
  border-color: var(--brand);
  background: var(--brand-soft);
}

.app-header__meta {
  display: inline-flex;
  align-items: center;
  gap: 0.55rem;
}

.status-chip {
  font-size: 0.75rem;
  font-weight: 700;
  letter-spacing: 0.06em;
  text-transform: uppercase;
  border-radius: 999px;
  border: 1px solid var(--line);
  padding: 0.24rem 0.72rem;
  background: #fff7ee;
}

.status-chip--streaming {
  border-color: var(--teal);
  background: rgba(31, 109, 102, 0.12);
}

.status-chip--error {
  border-color: #cc3030;
  background: rgba(204, 48, 48, 0.14);
}

.chat-id {
  margin: 0;
  max-width: 150px;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
  font-size: 0.78rem;
  color: var(--ink-soft);
}

.new-session-btn {
  border: none;
  border-radius: 12px;
  padding: 0.55rem 0.9rem;
  background: var(--ink);
  color: #fdf8f0;
  cursor: pointer;
  transition: transform 220ms ease;
}

.new-session-btn:hover {
  transform: translateY(-2px);
}

@media (max-width: 1080px) {
  .app-header {
    grid-template-columns: 1fr;
  }

  .app-header__controls {
    flex-direction: column;
    align-items: flex-start;
  }

  .app-header__modes {
    width: 100%;
    overflow-x: auto;
    padding-bottom: 0.2rem;
  }
}
</style>
