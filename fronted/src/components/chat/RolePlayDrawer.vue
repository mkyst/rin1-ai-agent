<script setup lang="ts">
interface RolePlayTurn {
  id: string
  user: string
  partner: string
}

const scenario = defineModel<string>('scenario', { required: true })
const userLine = defineModel<string>('userLine', { required: true })

defineProps<{
  open: boolean
  running: boolean
  history: RolePlayTurn[]
}>()

const emit = defineEmits<{
  close: []
  run: []
  clear: []
}>()
</script>

<template>
  <div v-if="open" class="roleplay-mask" @click.self="emit('close')">
    <section class="roleplay-drawer panel">
      <header class="roleplay-drawer__header">
        <div>
          <h3 class="roleplay-drawer__title">角色扮演练习</h3>
          <p class="roleplay-drawer__note">模拟“我说一句，对方回一句”，先预演再进入真实对话。</p>
        </div>
        <button type="button" class="roleplay-close" @click="emit('close')">关闭</button>
      </header>

      <div class="roleplay-setup">
        <label class="roleplay-label">
          场景设定
          <textarea
            v-model="scenario"
            class="roleplay-textarea"
            rows="3"
            placeholder="例如：对方最近经常已读不回，我想平和地表达感受并约一次沟通。"
          />
        </label>
      </div>

      <div class="roleplay-body">
        <div class="roleplay-history">
          <article v-for="item in history" :key="item.id" class="roleplay-turn">
            <p class="roleplay-turn__me"><span>我：</span>{{ item.user }}</p>
            <p class="roleplay-turn__partner"><span>对方：</span>{{ item.partner }}</p>
          </article>
          <p v-if="history.length === 0" class="roleplay-empty">
            先输入你想说的话，点击“让对方回应”开始练习。
          </p>
        </div>

        <div class="roleplay-compose">
          <label class="roleplay-label">
            我这句想说
            <textarea
              v-model="userLine"
              class="roleplay-textarea"
              rows="3"
              placeholder="例如：我不是要你秒回，只是希望你忙完能告诉我一声。"
            />
          </label>
          <div class="roleplay-actions">
            <button type="button" class="roleplay-btn roleplay-btn--ghost" :disabled="running" @click="emit('clear')">
              清空练习
            </button>
            <button
              type="button"
              class="roleplay-btn roleplay-btn--primary"
              :disabled="running || !scenario.trim() || !userLine.trim()"
              @click="emit('run')"
            >
              {{ running ? '生成中...' : '让对方回应' }}
            </button>
          </div>
        </div>
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

.roleplay-mask {
  position: fixed;
  inset: 0;
  z-index: 72;
  background: rgba(32, 28, 21, 0.3);
  backdrop-filter: blur(2px);
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 1rem;
}

.roleplay-drawer {
  width: min(980px, 96vw);
  max-height: 92vh;
  display: grid;
  grid-template-rows: auto auto minmax(0, 1fr);
  overflow: hidden;
}

.roleplay-drawer__header {
  display: flex;
  justify-content: space-between;
  gap: 0.8rem;
  padding: 1rem 1rem 0.78rem;
  border-bottom: 1px solid var(--line);
}

.roleplay-drawer__title {
  margin: 0;
  font-family: 'Fraunces', Georgia, serif;
  font-size: 1.22rem;
}

.roleplay-drawer__note {
  margin: 0.24rem 0 0;
  color: var(--ink-soft);
  font-size: 0.82rem;
}

.roleplay-close {
  border: 1px solid var(--line);
  border-radius: 999px;
  background: rgba(245, 239, 228, 0.8);
  padding: 0.25rem 0.72rem;
  cursor: pointer;
}

.roleplay-setup {
  border-bottom: 1px solid var(--line);
  padding: 0.72rem 1rem;
}

.roleplay-body {
  min-height: 0;
  overflow: auto;
  padding: 0.85rem 1rem 1rem;
  display: grid;
  gap: 0.8rem;
  align-content: start;
}

.roleplay-label {
  display: grid;
  gap: 0.35rem;
  font-size: 0.84rem;
  font-weight: 620;
  color: var(--ink);
}

.roleplay-textarea {
  width: 100%;
  border: 1px solid var(--line);
  border-radius: 10px;
  background: rgba(255, 251, 243, 0.9);
  padding: 0.58rem 0.65rem;
  resize: vertical;
  font: inherit;
}

.roleplay-history {
  border: 1px solid var(--line);
  border-radius: 12px;
  background: rgba(255, 251, 243, 0.84);
  padding: 0.7rem;
  display: grid;
  gap: 0.6rem;
}

.roleplay-turn {
  border: 1px dashed var(--line);
  border-radius: 10px;
  padding: 0.55rem 0.58rem;
  background: rgba(245, 239, 228, 0.72);
  display: grid;
  gap: 0.4rem;
}

.roleplay-turn p {
  margin: 0;
  line-height: 1.48;
}

.roleplay-turn span {
  font-weight: 700;
}

.roleplay-turn__partner {
  color: #1f4f68;
}

.roleplay-empty {
  margin: 0;
  color: var(--ink-soft);
  font-size: 0.85rem;
}

.roleplay-compose {
  display: grid;
  gap: 0.55rem;
}

.roleplay-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.45rem;
}

.roleplay-btn {
  border-radius: 10px;
  padding: 0.44rem 0.82rem;
  cursor: pointer;
  border: 1px solid transparent;
}

.roleplay-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.roleplay-btn--ghost {
  border-color: var(--line);
  background: rgba(245, 239, 228, 0.72);
}

.roleplay-btn--primary {
  border: none;
  background: var(--ink);
  color: #fbf7ee;
}

@media (max-width: 820px) {
  .roleplay-actions {
    flex-wrap: wrap;
  }
}
</style>
