<script setup lang="ts">
import type { ChatSessionSummary } from '../../types/chat'

defineProps<{
  sessions: ChatSessionSummary[]
  currentChatId: string
}>()

const emit = defineEmits<{
  select: [chatId: string]
  remove: [chatId: string]
}>()

function formatTime(value: string) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return '--:--'
  }
  return date.toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<template>
  <aside class="session-sidebar panel">
    <div class="panel__header">
      <h2 class="panel__title">会话档案</h2>
      <p class="panel__note">本地自动保存</p>
    </div>

    <ul v-if="sessions.length > 0" class="session-list">
      <li v-for="session in sessions" :key="session.id">
        <button
          type="button"
          class="session-item"
          :class="{ 'session-item--active': session.id === currentChatId }"
          @click="emit('select', session.id)"
        >
          <span class="session-item__title">{{ session.title }}</span>
          <span class="session-item__preview">{{ session.preview }}</span>
          <span class="session-item__time">{{ formatTime(session.updatedAt) }}</span>
        </button>
        <button type="button" class="session-delete" @click="emit('remove', session.id)">删除</button>
      </li>
    </ul>

    <p v-else class="session-empty">还没有历史会话，发送第一条消息即可创建。</p>
  </aside>
</template>

<style scoped>
.panel {
  border: 1px solid var(--line);
  border-radius: 18px;
  background: rgba(245, 239, 228, 0.9);
  box-shadow: var(--shadow);
}

.session-sidebar {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 420px;
}

.panel__header {
  padding: 1rem 1rem 0.85rem;
  border-bottom: 1px solid var(--line);
}

.panel__title {
  margin: 0;
  font-family: 'Fraunces', Georgia, serif;
  font-size: 1.18rem;
}

.panel__note {
  margin: 0.2rem 0 0;
  font-size: 0.8rem;
  color: var(--ink-soft);
}

.session-list {
  list-style: none;
  margin: 0;
  padding: 0.8rem;
  display: grid;
  gap: 0.6rem;
  overflow: auto;
}

.session-item {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: rgba(255, 251, 243, 0.86);
  padding: 0.65rem 0.72rem;
  text-align: left;
  display: grid;
  gap: 0.18rem;
  cursor: pointer;
  transition: all 180ms ease;
}

.session-item:hover {
  transform: translateY(-2px);
}

.session-item--active {
  border-color: var(--brand);
  background: var(--brand-soft);
}

.session-item__title {
  font-weight: 600;
  font-size: 0.9rem;
}

.session-item__preview {
  font-size: 0.78rem;
  color: var(--ink-soft);
}

.session-item__time {
  font-size: 0.72rem;
  color: var(--ink-soft);
}

.session-delete {
  margin-top: 0.28rem;
  border: none;
  padding: 0;
  background: transparent;
  color: #8f5847;
  cursor: pointer;
  font-size: 0.75rem;
}

.session-empty {
  margin: 0;
  padding: 1rem;
  color: var(--ink-soft);
  font-size: 0.88rem;
}

@media (max-width: 920px) {
  .session-sidebar {
    min-height: auto;
  }

  .session-list {
    grid-auto-flow: column;
    grid-auto-columns: minmax(200px, 1fr);
    overflow-x: auto;
  }
}
</style>
