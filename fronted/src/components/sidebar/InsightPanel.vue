<script setup lang="ts">
import type { InsightCard } from '../../types/chat'

defineProps<{
  cards: InsightCard[]
  quickPrompts: string[]
}>()

const emit = defineEmits<{
  usePrompt: [prompt: string]
}>()
</script>

<template>
  <aside class="insight-panel panel">
    <div class="panel__header">
      <h2 class="panel__title">实时洞察</h2>
      <p class="panel__note">随模型回复动态更新</p>
    </div>

    <div class="insight-cards">
      <article v-for="card in cards" :key="card.title" class="insight-card">
        <p class="insight-card__title">{{ card.title }}</p>
        <p class="insight-card__value">{{ card.value }}</p>
        <p class="insight-card__detail">{{ card.detail }}</p>
      </article>
    </div>

    <div class="prompt-zone">
      <h3 class="prompt-zone__title">快捷提问</h3>
      <button
        v-for="prompt in quickPrompts"
        :key="prompt"
        type="button"
        class="prompt-btn"
        @click="emit('usePrompt', prompt)"
      >
        {{ prompt }}
      </button>
    </div>
  </aside>
</template>

<style scoped>
.panel {
  border: 1px solid var(--line);
  border-radius: 18px;
  background: rgba(245, 239, 228, 0.9);
  box-shadow: var(--shadow);
}

.insight-panel {
  height: 100%;
  min-height: 420px;
  display: grid;
  grid-template-rows: auto auto 1fr;
}

.panel__header {
  padding: 1rem 1rem 0.85rem;
  border-bottom: 1px solid var(--line);
}

.panel__title {
  margin: 0;
  font-family: 'Fraunces', Georgia, serif;
  font-size: 1.16rem;
}

.panel__note {
  margin: 0.2rem 0 0;
  font-size: 0.8rem;
  color: var(--ink-soft);
}

.insight-cards {
  padding: 0.9rem;
  display: grid;
  gap: 0.7rem;
}

.insight-card {
  border: 1px solid var(--line);
  border-radius: 12px;
  background: rgba(255, 251, 243, 0.86);
  padding: 0.72rem;
}

.insight-card__title {
  margin: 0;
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: var(--ink-soft);
}

.insight-card__value {
  margin: 0.22rem 0 0;
  font-size: 1.01rem;
  font-weight: 700;
}

.insight-card__detail {
  margin: 0.25rem 0 0;
  font-size: 0.8rem;
  color: var(--ink-soft);
}

.prompt-zone {
  border-top: 1px solid var(--line);
  padding: 0.8rem 0.9rem 0.9rem;
  display: grid;
  align-content: start;
  gap: 0.55rem;
}

.prompt-zone__title {
  margin: 0;
  font-size: 0.86rem;
}

.prompt-btn {
  border: 1px solid var(--line);
  border-radius: 11px;
  padding: 0.58rem 0.65rem;
  text-align: left;
  background: rgba(245, 239, 228, 0.7);
  cursor: pointer;
  transition: all 180ms ease;
}

.prompt-btn:hover {
  border-color: var(--teal);
  transform: translateX(3px);
}

@media (max-width: 920px) {
  .insight-panel {
    min-height: auto;
  }
}
</style>
