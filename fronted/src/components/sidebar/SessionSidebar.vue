<script setup lang="ts">
import { computed, ref } from 'vue'
import type { ChatSessionSummary } from '../../types/chat'

const props = defineProps<{
  sessions: ChatSessionSummary[]
  currentChatId: string
}>()

const emit = defineEmits<{
  select: [chatId: string]
  remove: [chatId: string]
}>()

const keyword = ref('')
const activeTag = ref<string>('全部')

const allTags = computed(() => {
  const unique = new Set<string>()
  for (const session of props.sessions) {
    for (const tag of session.tags) {
      unique.add(tag)
    }
  }
  return ['全部', ...[...unique]]
})

const filteredSessions = computed(() => {
  const key = keyword.value.trim().toLowerCase()

  return props.sessions.filter((session) => {
    const inTag = activeTag.value === '全部' || session.tags.includes(activeTag.value)
    if (!inTag) {
      return false
    }
    if (!key) {
      return true
    }

    return (
      session.title.toLowerCase().includes(key) ||
      session.preview.toLowerCase().includes(key) ||
      session.tags.some((tag) => tag.toLowerCase().includes(key))
    )
  })
})

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
      <p class="panel__note">关键词检索 + 标签筛选</p>
    </div>

    <div class="session-filter">
      <label class="search-pill">
        <span class="search-pill__icon">⌕</span>
        <input v-model="keyword" type="search" class="search-pill__input" placeholder="搜索会话内容..." />
        <button v-if="keyword" type="button" class="search-pill__clear" @click="keyword = ''">清空</button>
      </label>

      <div class="tag-row">
        <button
          v-for="tag in allTags"
          :key="tag"
          type="button"
          class="tag-chip"
          :class="{ 'tag-chip--active': tag === activeTag }"
          @click="activeTag = tag"
        >
          {{ tag }}
        </button>
      </div>
    </div>

    <ul v-if="filteredSessions.length > 0" class="session-list">
      <li v-for="session in filteredSessions" :key="session.id" class="session-row">
        <button
          type="button"
          class="session-item"
          :class="{ 'session-item--active': session.id === currentChatId }"
          @click="emit('select', session.id)"
        >
          <span class="session-item__title">{{ session.title }}</span>
          <span class="session-item__preview">{{ session.preview }}</span>
          <span v-if="session.tags.length > 0" class="session-item__tags">
            <span v-for="tag in session.tags" :key="`${session.id}-${tag}`" class="session-tag">{{ tag }}</span>
          </span>
          <span class="session-item__time">{{ formatTime(session.updatedAt) }}</span>
        </button>

        <div class="session-row__actions">
          <button type="button" class="session-delete" @click="emit('remove', session.id)">删除会话</button>
        </div>
      </li>
    </ul>

    <p v-else class="session-empty">
      {{ sessions.length > 0 ? '没有匹配到会话，换个关键词试试。' : '还没有历史会话，发送第一条消息即可创建。' }}
    </p>
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
  min-height: 0;
  display: flex;
  flex-direction: column;
}

.panel__header {
  padding: 1rem 1rem 0.7rem;
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

.session-filter {
  padding: 0.75rem 0.8rem 0.68rem;
  border-bottom: 1px solid var(--line);
  display: grid;
  gap: 0.6rem;
}

.search-pill {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 0.45rem;
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(255, 251, 243, 0.9);
  padding: 0.16rem 0.22rem 0.16rem 0.62rem;
}

.search-pill__icon {
  color: var(--ink-soft);
  font-size: 0.8rem;
}

.search-pill__input {
  width: 100%;
  border: none;
  background: transparent;
  font-size: 0.86rem;
  color: var(--ink);
  min-width: 0;
}

.search-pill__input:focus {
  outline: none;
}

.search-pill:focus-within {
  border-color: var(--teal);
  box-shadow: 0 0 0 2px rgba(31, 109, 102, 0.18);
}

.search-pill__clear {
  border: 1px solid transparent;
  border-radius: 999px;
  background: rgba(32, 28, 21, 0.08);
  color: var(--ink-soft);
  font-size: 0.72rem;
  padding: 0.12rem 0.42rem;
  cursor: pointer;
}

.search-pill__clear:hover {
  border-color: var(--line);
}

.tag-row {
  display: flex;
  gap: 0.38rem;
  overflow-x: auto;
  padding-bottom: 0.12rem;
}

.tag-chip {
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(245, 239, 228, 0.8);
  color: var(--ink-soft);
  padding: 0.2rem 0.62rem;
  font-size: 0.72rem;
  white-space: nowrap;
  cursor: pointer;
  transition: all 160ms ease;
}

.tag-chip:hover {
  border-color: #b9a688;
}

.tag-chip--active {
  border-color: var(--brand);
  background: rgba(191, 76, 42, 0.16);
  color: #8a3218;
}

.session-list {
  list-style: none;
  margin: 0;
  padding: 0.8rem;
  display: grid;
  gap: 0.68rem;
  overflow: auto;
}

.session-row {
  display: grid;
  gap: 0.3rem;
}

.session-item {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: rgba(255, 251, 243, 0.86);
  padding: 0.66rem 0.72rem;
  text-align: left;
  display: grid;
  gap: 0.22rem;
  cursor: pointer;
  transition: all 180ms ease;
}

.session-item:hover {
  transform: translateY(-2px);
  border-color: #bea98b;
}

.session-item--active {
  border-color: var(--brand);
  background: rgba(191, 76, 42, 0.14);
}

.session-item__title {
  font-weight: 650;
  font-size: 0.94rem;
}

.session-item__preview {
  font-size: 0.8rem;
  color: var(--ink-soft);
}

.session-item__tags {
  display: flex;
  gap: 0.3rem;
  flex-wrap: wrap;
}

.session-tag {
  border-radius: 999px;
  background: rgba(31, 109, 102, 0.12);
  color: var(--teal);
  font-size: 0.67rem;
  padding: 0.08rem 0.4rem;
}

.session-item__time {
  font-size: 0.72rem;
  color: var(--ink-soft);
}

.session-row__actions {
  display: flex;
  justify-content: flex-end;
}

.session-delete {
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: #8f5847;
  cursor: pointer;
  font-size: 0.73rem;
  padding: 0.15rem 0.4rem;
}

.session-delete:hover {
  border-color: rgba(143, 88, 71, 0.3);
  background: rgba(143, 88, 71, 0.08);
}

.session-empty {
  margin: 0;
  padding: 1rem;
  color: var(--ink-soft);
  font-size: 0.88rem;
}

@media (max-width: 920px) {
  .session-list {
    grid-auto-flow: column;
    grid-auto-columns: minmax(220px, 1fr);
    overflow-x: auto;
  }
}
</style>
