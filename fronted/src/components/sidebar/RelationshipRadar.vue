<script setup lang="ts">
import { computed } from 'vue'
import type { RadarMetric } from '../../types/chat'

const props = defineProps<{
  metrics: RadarMetric[]
}>()

const size = 220
const center = size / 2
const radius = 78
const levels = 4

function pointBy(index: number, r: number, total: number) {
  const angle = -Math.PI / 2 + (index * 2 * Math.PI) / total
  const x = center + Math.cos(angle) * r
  const y = center + Math.sin(angle) * r
  return { x, y }
}

const axisPoints = computed(() => props.metrics.map((_, index) => pointBy(index, radius, props.metrics.length)))

const gridPolygons = computed(() =>
  Array.from({ length: levels }, (_, idx) => {
    const ratio = (idx + 1) / levels
    return props.metrics
      .map((_, index) => pointBy(index, radius * ratio, props.metrics.length))
      .map((point) => `${point.x},${point.y}`)
      .join(' ')
  }),
)

const valuePolygon = computed(() =>
  props.metrics
    .map((metric, index) => pointBy(index, (metric.value / 100) * radius, props.metrics.length))
    .map((point) => `${point.x},${point.y}`)
    .join(' '),
)

const labelPoints = computed(() =>
  props.metrics.map((metric, index) => {
    const p = pointBy(index, radius + 24, props.metrics.length)
    return { ...metric, x: p.x, y: p.y }
  }),
)
</script>

<template>
  <section class="radar">
    <header class="radar__header">
      <h3 class="radar__title">关系雷达盘</h3>
      <p class="radar__note">把本轮对话可视化为情绪/冲突/信任/可执行度。</p>
    </header>

    <div class="radar__body">
      <svg class="radar-svg" :viewBox="`0 0 ${size} ${size}`" role="img" aria-label="关系雷达盘">
        <polygon
          v-for="(points, index) in gridPolygons"
          :key="`grid-${index}`"
          :points="points"
          fill="none"
          stroke="rgba(95, 83, 67, 0.18)"
          stroke-width="1"
        />
        <line
          v-for="(point, index) in axisPoints"
          :key="`axis-${index}`"
          :x1="center"
          :y1="center"
          :x2="point.x"
          :y2="point.y"
          stroke="rgba(95, 83, 67, 0.2)"
          stroke-width="1"
        />
        <polygon :points="valuePolygon" fill="rgba(31, 109, 102, 0.25)" stroke="var(--teal)" stroke-width="2" />
        <circle
          v-for="(metric, index) in metrics"
          :key="metric.key"
          :cx="pointBy(index, (metric.value / 100) * radius, metrics.length).x"
          :cy="pointBy(index, (metric.value / 100) * radius, metrics.length).y"
          r="3.5"
          fill="var(--teal)"
        />
        <text
          v-for="metric in labelPoints"
          :key="`label-${metric.key}`"
          :x="metric.x"
          :y="metric.y"
          text-anchor="middle"
          dominant-baseline="middle"
          class="radar-label"
        >
          {{ metric.label }}
        </text>
      </svg>

      <div class="radar-metrics">
        <div v-for="metric in metrics" :key="metric.key" class="radar-metric">
          <div class="radar-metric__head">
            <span>{{ metric.label }}</span>
            <strong>{{ metric.value }}</strong>
          </div>
          <div class="radar-metric__track">
            <span class="radar-metric__bar" :style="{ width: `${metric.value}%` }" />
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.radar {
  overflow: hidden;
  min-height: 600px;
}

.radar__header {
  padding: 0.9rem 0.9rem 0.72rem;
  border-bottom: 1px solid var(--line);
}

.radar__title {
  margin: 0;
  font-size: 0.95rem;
  font-weight: 700;
}

.radar__note {
  margin: 0.2rem 0 0;
  color: var(--ink-soft);
  font-size: 0.76rem;
}

.radar__body {
  padding: 1.1rem 0.95rem 2.25rem;
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  gap: 1.15rem;
  align-content: start;
}

.radar-svg {
  width: 100%;
  max-width: 260px;
  margin: 0 auto;
  display: block;
}

.radar-label {
  font-size: 9px;
  fill: var(--ink-soft);
}

.radar-metrics {
  display: grid;
  gap: 0.58rem;
  padding-top: 0.24rem;
  padding-bottom: 0.55rem;
}

.radar-metric__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.77rem;
}

.radar-metric__track {
  margin-top: 0.18rem;
  height: 6px;
  background: rgba(95, 83, 67, 0.16);
  border-radius: 999px;
  overflow: hidden;
}

.radar-metric__bar {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, rgba(31, 109, 102, 0.68), rgba(191, 76, 42, 0.72));
}
</style>
