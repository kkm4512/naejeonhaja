<template>
  <section class="bench" aria-labelledby="bench-title">
    <header class="head">
      <h2 id="bench-title" class="title">대기열</h2>
      <p class="count num"><strong>{{ roster.readyPlayers.value.length }}</strong>/10</p>
      <p class="hint">카드 아래 아이콘은 라인이에요. 누를 때마다 주 → 부 → 해제. 카드를 누르면 자세히 볼 수 있어요.</p>
      <div class="head-actions">
        <button
          v-if="roster.groupCode.value && unsavedCount"
          type="button"
          class="btn btn--ghost"
          @click="saveAll"
        >
          명단 {{ unsavedCount }}명 단골로 저장
        </button>
        <button v-if="roster.count.value" type="button" class="btn btn--ghost" :class="{ armed: confirmClear }" @click="clearAll">
          {{ confirmClear ? `한 번 더 누르면 ${roster.count.value}명 모두 빠져요` : '모두 빼기' }}
        </button>
      </div>
    </header>

    <ol class="grid">
      <DraftBenchCard v-for="p in roster.players.value" :key="p.id" :player="p" @open="$emit('open', $event)" />
      <li v-for="n in empty" :key="`e${n}`" class="empty">
        <span class="empty-num num">{{ roster.count.value + n }}</span>
      </li>
    </ol>
  </section>
</template>

<script setup lang="ts">
import { MAX_PLAYERS } from '~/composables/useRoster'

defineEmits<{ open: [id: string] }>()

const roster = useRoster()
const confirmClear = ref(false)
let t: ReturnType<typeof setTimeout> | undefined

const empty = computed(() => Math.max(0, MAX_PLAYERS - roster.count.value))
const unsavedCount = computed(() => roster.readyPlayers.value.filter((p) => !p.memberId).length)

function clearAll() {
  if (!confirmClear.value) {
    confirmClear.value = true
    clearTimeout(t)
    t = setTimeout(() => (confirmClear.value = false), 4000)
    return
  }
  confirmClear.value = false
  roster.clear()
}

async function saveAll() {
  for (const p of roster.readyPlayers.value.filter((x) => !x.memberId)) {
    await roster.saveAsMember(p.id)
  }
}
</script>

<style scoped>
.bench {
  display: grid;
  gap: 14px;
}

.head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 6px 14px;
}

.title {
  font-family: var(--font-display);
  font-size: var(--fs-lg);
  font-weight: 400;
}

.count {
  font-size: var(--fs-md);
  color: var(--muted);
  font-weight: 600;
}

.count strong {
  color: var(--text);
  font-size: 1.6rem;
  font-weight: 800;
}

.hint {
  color: var(--faint);
  font-size: var(--fs-xs);
}

.head-actions {
  margin-left: auto;
  display: flex;
  gap: 4px;
}

.armed {
  color: var(--warn) !important;
}

.grid {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  grid-template-columns: repeat(10, minmax(0, 1fr));
  gap: 10px;
}

.empty {
  display: grid;
  place-items: center;
  aspect-ratio: 308 / 560;
  border: 1px dashed var(--line);
  border-radius: var(--r-m);
}

.empty-num {
  font-size: 2.4rem;
  font-weight: 800;
  color: var(--line-strong);
}

@media (max-width: 1280px) {
  .grid {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }
  .empty {
    aspect-ratio: auto;
    min-height: 120px;
  }
}

@media (max-width: 640px) {
  .grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .hint {
    display: none;
  }
}
</style>
