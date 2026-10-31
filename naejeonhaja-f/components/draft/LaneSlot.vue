<template>
  <li
    class="slot"
    :class="[side, { filled: !!player, sub: player?.mmrReduced }]"
    :style="{ '--delay': `${index * 90}ms` }"
  >
    <span class="lane-col">
      <UiLaneIcon :line="laneLine" :size="22" />
      <span class="lane-label">{{ laneLine ? LINE_LABEL[laneLine] : index + 1 }}</span>
    </span>

    <Transition name="pick" appear>
      <span v-if="player" :key="`${player.name}-${revealKey}`" class="pick">
        <span class="tint" aria-hidden="true" />
        <span class="avatar" aria-hidden="true">
          <img v-if="icon" :src="icon" alt="" width="52" height="52" loading="lazy">
          <span v-else class="initial">{{ shortName.slice(0, 1) }}</span>
        </span>
        <span class="who">
          <span class="name">{{ shortName }}</span>
          <span class="sub-line">
            <UiTierTag :tier="player.tier" :show-lp="false" />
            <span v-if="player.offRole" class="badge off" title="고르지 않은 라인 — MMR 을 크게 낮춰 계산">라인 외</span>
            <span v-else-if="player.mmrReduced" class="badge" title="부 라인 배정 — MMR 을 낮춰 계산">부 라인</span>
          </span>
        </span>
        <span class="mmr num">
          {{ player.mmr }}
          <span v-if="player.mmrReduced" class="base">{{ player.baseMmr }}</span>
        </span>
      </span>
      <span v-else class="empty">
        <span class="q">?</span>
      </span>
    </Transition>
  </li>
</template>

<script setup lang="ts">
import type { AssignedPlayer, Line } from '~/types/api'
import { LINE_LABEL, LINES } from '~/utils/lol'

const props = defineProps<{
  side: 'blue' | 'red'
  index: number
  player?: AssignedPlayer
  mode: 'rift' | 'abyss'
  revealKey: number
}>()

const roster = useRoster()
const rp = computed(() => (props.player ? roster.byName(props.player.name) : undefined))
const laneLine = computed<Line | null>(() =>
  props.mode === 'rift' ? (props.player?.line ?? LINES[props.index] ?? null) : null
)
// 대진표에는 챔피언(일러스트·이름)을 숨겨 상대가 픽을 미리 알 수 없게 하고, 소환사 아이콘만 보여 준다
const icon = computed(() => {
  const p = rp.value
  if (!p) return undefined
  return p.profile?.profileIconUrl ?? roster.members.value.find((m) => m.id === p.memberId)?.profileIconUrl
})
const shortName = computed(() => props.player?.name.split('#')[0] ?? '')
</script>

<style scoped>
.slot {
  position: relative;
  display: grid;
  grid-template-columns: 56px minmax(0, 1fr);
  min-height: 84px;
  border-radius: var(--r-m);
  background: var(--panel);
  border: 1px solid var(--line);
  overflow: hidden;
}

.slot.red {
  grid-template-columns: minmax(0, 1fr) 56px;
}

.slot.red .lane-col {
  order: 2;
}

.lane-col {
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 4px;
  color: var(--muted);
  background: var(--bg-deep);
  border-right: 1px solid var(--line);
}

.slot.red .lane-col {
  border-right: 0;
  border-left: 1px solid var(--line);
}

.slot.filled.blue .lane-col {
  color: var(--blue);
}

.slot.filled.red .lane-col {
  color: var(--red);
}

.lane-label {
  font-size: var(--fs-xs);
  font-weight: 700;
}

.pick,
.empty {
  position: relative;
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 0 18px;
  min-width: 0;
}

.slot.red .pick {
  text-align: right;
}

.slot.red .pick .mmr {
  order: -1;
}

.slot.red .pick .avatar {
  order: 2;
}

/* 진영 색 배경 */
.tint {
  position: absolute;
  inset: 0;
}

.slot.blue .tint {
  background: linear-gradient(90deg, rgba(30, 84, 170, 0.85) 0%, rgba(18, 58, 122, 0.55) 55%, rgba(11, 14, 26, 0.2) 100%);
}

.slot.red .tint {
  background: linear-gradient(270deg, rgba(170, 32, 48, 0.85) 0%, rgba(122, 22, 33, 0.55) 55%, rgba(11, 14, 26, 0.2) 100%);
}

.avatar {
  position: relative;
  z-index: 1;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  overflow: hidden;
  display: grid;
  place-items: center;
  background: var(--bg-deep);
  box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.18);
}

.slot.blue .avatar {
  box-shadow: 0 0 0 2px var(--blue);
}

.slot.red .avatar {
  box-shadow: 0 0 0 2px var(--red);
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.initial {
  font-family: var(--font-display);
  font-size: 1.4rem;
  color: var(--muted);
}

.who {
  position: relative;
  z-index: 1;
  min-width: 0;
  display: grid;
  gap: 2px;
}

.name {
  font-family: var(--font-display);
  font-size: 1.45rem;
  line-height: 1.1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sub-line {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.slot.red .sub-line {
  justify-content: flex-end;
}

.badge {
  padding: 0 6px;
  border: 1px dashed currentColor;
  border-radius: 4px;
  font-size: 0.6875rem;
  color: var(--text);
  opacity: 0.85;
}

.badge.off {
  border-style: solid;
  border-color: var(--warn);
  color: var(--warn);
  opacity: 1;
}

.mmr {
  position: relative;
  z-index: 1; /* 그리드 order 로 순서를 바꾸면 일러스트 아래에 그려지므로 */
  font-size: 1.7rem;
  font-weight: 800;
  line-height: 1;
}

.base {
  display: block;
  font-size: 0.85rem;
  font-weight: 600;
  color: rgba(238, 240, 247, 0.55);
  text-decoration: line-through;
  text-align: right;
}

.empty {
  grid-template-columns: 1fr;
  color: var(--line-strong);
}

.slot.red .empty {
  text-align: right;
}

.q {
  font-family: var(--font-num);
  font-size: 2rem;
  font-weight: 800;
}

/* 팀 공개 연출: 바깥쪽에서 안쪽으로 밀려 들어오며 한 라인씩 */
.pick-enter-active {
  transition: transform 0.55s var(--ease-out) var(--delay), opacity 0.4s ease var(--delay), clip-path 0.55s var(--ease-out) var(--delay);
}

.slot.blue .pick-enter-from {
  transform: translateX(-40px);
  opacity: 0;
  clip-path: inset(0 100% 0 0);
}

.slot.red .pick-enter-from {
  transform: translateX(40px);
  opacity: 0;
  clip-path: inset(0 0 0 100%);
}

.pick-enter-to {
  clip-path: inset(0 0 0 0);
}

.pick-leave-active {
  display: none;
}

@media (max-width: 640px) {
  .slot,
  .slot.red {
    grid-template-columns: 44px minmax(0, 1fr);
    min-height: 68px;
  }
  .slot.red .lane-col {
    order: 0;
    border-left: 0;
    border-right: 1px solid var(--line);
  }
  .slot.red .pick {
    text-align: left;
  }
  .slot.red .pick .mmr {
    order: 2;
  }
  .slot.red .pick .avatar {
    order: 0;
  }
  .slot.red .sub-line {
    justify-content: flex-start;
  }
  .slot.red .tint {
    background: linear-gradient(90deg, rgba(170, 32, 48, 0.85) 0%, rgba(122, 22, 33, 0.55) 55%, rgba(11, 14, 26, 0.2) 100%);
  }
  .avatar {
    width: 40px;
    height: 40px;
  }
  .name {
    font-size: 1.2rem;
  }
  .mmr {
    font-size: 1.4rem;
  }
  .pick,
  .empty {
    padding: 0 12px;
  }
}
</style>
