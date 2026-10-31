<template>
  <section ref="el" class="stage" :class="{ revealed: !!draft.result.value }" aria-label="팀 구성">
    <div class="side blue">
      <header class="side-head">
        <h2 class="side-name">BLUE</h2>
        <span v-if="draft.result.value" class="side-total num">{{ draft.blueTotal.value }}</span>
        <span v-if="draft.saved.value?.winner === 'BLUE'" class="crown">승리</span>
      </header>
      <ol class="slots">
        <DraftLaneSlot
          v-for="i in 5"
          :key="`b${i}`"
          side="blue"
          :index="i - 1"
          :player="draft.blue.value[i - 1]"
          :mode="slotMode"
          :reveal-key="draft.revealKey.value"
        />
      </ol>
    </div>

    <div class="center">
      <DraftCenterConsole />
    </div>

    <div class="side red">
      <header class="side-head">
        <span v-if="draft.saved.value?.winner === 'RED'" class="crown">승리</span>
        <span v-if="draft.result.value" class="side-total num">{{ draft.redTotal.value }}</span>
        <h2 class="side-name">RED</h2>
      </header>
      <ol class="slots">
        <DraftLaneSlot
          v-for="i in 5"
          :key="`r${i}`"
          side="red"
          :index="i - 1"
          :player="draft.red.value[i - 1]"
          :mode="slotMode"
          :reveal-key="draft.revealKey.value"
        />
      </ol>
    </div>
  </section>
</template>

<script setup lang="ts">
const draft = useDraft()
const el = ref<HTMLElement | null>(null)

const slotMode = computed(() => (draft.result.value ? draft.resultMode.value : draft.mode.value))

// 팀이 공개되면 화면을 무대 쪽으로
watch(() => draft.revealKey.value, async () => {
  await nextTick()
  const rect = el.value?.getBoundingClientRect()
  if (!rect || rect.top >= 0 && rect.top < window.innerHeight * 0.3) return
  const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
  el.value?.scrollIntoView({ behavior: reduce ? 'auto' : 'smooth', block: 'start' })
})
</script>

<style scoped>
.stage {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 300px minmax(0, 1fr);
  gap: 24px;
  padding: 26px var(--gutter) 30px;
  scroll-margin-top: 64px;
  background:
    radial-gradient(ellipse at 0% 50%, rgba(62, 139, 255, 0.12), transparent 55%),
    radial-gradient(ellipse at 100% 50%, rgba(255, 78, 91, 0.12), transparent 55%),
    var(--bg-deep);
  border-top: 1px solid var(--line);
  border-bottom: 1px solid var(--line);
}

/* 가운데 세로 분리선 */
.stage::before {
  content: '';
  position: absolute;
  top: 0;
  bottom: 0;
  left: 50%;
  width: 1px;
  background: linear-gradient(180deg, transparent, var(--line-strong) 20%, var(--line-strong) 80%, transparent);
  opacity: 0.6;
  pointer-events: none;
}

.side {
  display: grid;
  align-content: start;
  gap: 12px;
  min-width: 0;
}

.side-head {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.red .side-head {
  justify-content: flex-end;
}

.side-name {
  font-family: var(--font-num);
  font-style: italic;
  font-weight: 800;
  font-size: 2.6rem;
  line-height: 1;
  letter-spacing: 0.02em;
}

.blue .side-name {
  color: var(--blue);
  text-shadow: 0 0 24px var(--blue-glow);
}

.red .side-name {
  color: var(--red);
  text-shadow: 0 0 24px var(--red-glow);
}

.side-total {
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--muted);
}

.crown {
  padding: 2px 10px;
  border-radius: 999px;
  background: var(--win);
  color: var(--bg-deep);
  font-weight: 800;
  font-size: var(--fs-sm);
}

.slots {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 8px;
}

.center {
  position: relative;
  z-index: 1;
  display: grid;
  align-items: center;
  padding: 0 4px;
  background: var(--bg-deep);
}

@media (max-width: 1100px) {
  .stage {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  }
  .stage::before {
    display: none;
  }
  .center {
    grid-column: 1 / -1;
    grid-row: 1;
    max-width: 420px;
    width: 100%;
    margin: 0 auto;
    background: transparent;
  }
}

@media (max-width: 640px) {
  .stage {
    grid-template-columns: 1fr;
    gap: 18px;
  }
  /* 휴대폰에서는 팀이 나오기 전 빈 슬롯을 숨겨 스크롤을 줄인다 */
  .stage:not(.revealed) .side {
    display: none;
  }
  .red .side-head {
    justify-content: flex-start;
    flex-direction: row-reverse;
  }
  .side-name {
    font-size: 2rem;
  }
}
</style>
