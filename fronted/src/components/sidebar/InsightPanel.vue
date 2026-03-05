<script setup lang="ts">
import RelationshipRadar from './RelationshipRadar.vue'
import type { InsightCard, RadarMetric } from '../../types/chat'

defineProps<{
  cards: InsightCard[]
  quickPrompts: string[]
  radarMetrics: RadarMetric[]
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

    <div class="insight-scroll">
      <RelationshipRadar class="insight-item" :metrics="radarMetrics" />

      <section class="insight-item insight-cards-block">
        <article v-for="card in cards" :key="card.title" class="insight-row">
          <p class="insight-row__title">{{ card.title }}</p>
          <p class="insight-row__value">{{ card.value }}</p>
          <p class="insight-row__detail">{{ card.detail }}</p>
        </article>
      </section>

      <section class="insight-item prompt-zone">
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
      </section>
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
  grid-template-rows: auto minmax(0, 1fr);
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

.insight-scroll {
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-gutter: stable;
  padding: 0.9rem;
  display: grid;
  align-content: start;
  gap: 0.75rem;
}

.insight-item {
  border: 1px solid var(--line);
  border-radius: 14px;
  background: rgba(255, 251, 243, 0.88);
}

.insight-cards-block {
  padding: 0.72rem;
  display: grid;
  gap: 0.58rem;
}

.insight-row {
  border: 1px solid rgba(95, 83, 67, 0.24);
  border-radius: 12px;
  background: rgba(245, 239, 228, 0.7);
  padding: 0.62rem 0.66rem;
}

.insight-row__title {
  margin: 0;
  font-size: 0.78rem;
  text-transform: uppercase;
  letter-spacing: 0.07em;
  color: var(--ink-soft);
}

.insight-row__value {
  margin: 0.2rem 0 0;
  font-size: 1.02rem;
  font-weight: 700;
}

.insight-row__detail {
  margin: 0.24rem 0 0;
  font-size: 0.8rem;
  color: var(--ink-soft);
}

.prompt-zone {
  padding: 0.72rem;
  display: grid;
  gap: 0.55rem;
}

.prompt-zone__title {
  margin: 0;
  font-size: 0.9rem;
}

.prompt-btn {
  border: 1px solid rgba(95, 83, 67, 0.24);
  border-radius: 12px;
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

  .insight-scroll {
    max-height: 62dvh;
  }
}

.insight-scroll::-webkit-scrollbar {
  width: 10px;
}

.insight-scroll::-webkit-scrollbar-track {
  background: rgba(95, 83, 67, 0.12);
  border-radius: 999px;
}

.insight-scroll::-webkit-scrollbar-thumb {
  background: rgba(95, 83, 67, 0.36);
  border-radius: 999px;
  border: 2px solid rgba(245, 239, 228, 0.9);
}
</style>
