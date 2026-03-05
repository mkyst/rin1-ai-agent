<script setup lang="ts">
import { computed } from 'vue'
import { renderMarkdown } from '../../utils/markdown'

const props = defineProps<{
  content: string
}>()

const safeHtml = computed(() => renderMarkdown(props.content))
</script>

<template>
  <!-- 内容先做了 HTML 转义，再按 Markdown 规则生成有限标签 -->
  <div class="markdown-body" v-html="safeHtml" />
</template>

<style scoped>
.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3) {
  margin: 0.45rem 0 0.35rem;
  font-family: 'Fraunces', Georgia, serif;
  line-height: 1.25;
}

.markdown-body :deep(h1) {
  font-size: 1.22rem;
}

.markdown-body :deep(h2) {
  font-size: 1.12rem;
}

.markdown-body :deep(h3) {
  font-size: 1.03rem;
}

.markdown-body :deep(p) {
  margin: 0.25rem 0;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  margin: 0.3rem 0;
  padding-left: 1.1rem;
}

.markdown-body :deep(li) {
  margin: 0.18rem 0;
}

.markdown-body :deep(code) {
  background: rgba(32, 28, 21, 0.08);
  border-radius: 6px;
  padding: 0.1rem 0.35rem;
  font-family: 'Consolas', 'Cascadia Mono', monospace;
  font-size: 0.9em;
}

.markdown-body :deep(pre) {
  margin: 0.38rem 0;
  background: rgba(32, 28, 21, 0.9);
  color: #fdf8ef;
  border-radius: 10px;
  padding: 0.6rem 0.7rem;
  overflow: auto;
}

.markdown-body :deep(pre code) {
  background: transparent;
  padding: 0;
  color: inherit;
}

.markdown-body :deep(blockquote) {
  margin: 0.35rem 0;
  padding: 0.15rem 0.65rem;
  border-left: 3px solid var(--teal);
  color: var(--ink-soft);
}

.markdown-body :deep(a) {
  color: var(--teal);
  text-decoration: underline;
}

.markdown-body :deep(br) {
  display: block;
  content: '';
  margin: 0.18rem 0;
}
</style>
