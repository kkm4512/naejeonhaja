<template>
  <li class="card" :class="[player.status, { manual: player.manual, linked: !!player.memberId }]" :style="cardStyle">
    <button type="button" class="open" :aria-label="`${player.name} 자세히 보기`" @click="$emit('open', player.id)">
      <span class="art" aria-hidden="true">
        <img v-if="art" :src="art" alt="" loading="lazy" @error="artFailed = true">
        <span v-else class="initial">{{ initial }}</span>
      </span>
      <span class="shade" aria-hidden="true" />

      <span class="top">
        <span class="tier-chip num" :style="{ '--tier': tierColor(player.tier) }">{{ tierShort(player.tier) }}</span>
        <span v-if="player.memberId" class="member-chip" title="단골 멤버">★</span>
      </span>

      <span class="bottom">
        <span class="name">{{ displayName }}</span>
        <span v-if="tag" class="tag">#{{ tag }}</span>
        <span class="meta">
          <span v-if="player.status === 'loading'" class="state">불러오는 중</span>
          <span v-else-if="player.status === 'error'" class="state err">{{ player.error }}</span>
          <template v-else>
            <span class="mmr num">{{ effective }}</span>
            <span v-if="player.inhouseDelta" class="delta num" :class="player.inhouseDelta > 0 ? 'up' : 'down'">
              {{ player.inhouseDelta > 0 ? '+' : '' }}{{ player.inhouseDelta }}
            </span>
          </template>
        </span>
      </span>
      <span v-if="player.status === 'loading'" class="shimmer" aria-hidden="true" />
    </button>

    <div class="lines" role="group" :aria-label="`${player.name} 가능 라인`">
      <button
        v-for="line in LINES"
        :key="line"
        type="button"
        class="lane"
        :class="roleOf(line)?.toLowerCase()"
        :aria-pressed="!!roleOf(line)"
        :title="laneTitle(line)"
        :disabled="player.status !== 'ready'"
        @click="roster.cycleLine(player.id, line)"
      >
        <UiLaneIcon :line="line" :size="16" />
        <span class="visually-hidden">{{ laneTitle(line) }}</span>
      </button>
    </div>
    <p class="lines-state" :class="player.linesState">
      <template v-if="player.linesState === 'analyzing'">모스트 3으로 라인 분석 중…</template>
      <template v-else-if="player.linesState === 'waiting'">호출 한도 대기 · {{ waitLeft }}초 뒤 분석</template>
      <template v-else-if="player.linesState === 'auto'">모스트 3 기준 자동</template>
      <template v-else-if="player.linesState === 'saved'">저장된 라인</template>
      <template v-else-if="player.linesState === 'edited'">직접 설정</template>
      <template v-else>라인 상관없음</template>
    </p>

    <button type="button" class="remove" :aria-label="`${player.name} 빼기`" @click="roster.remove(player.id)">
      <svg viewBox="0 0 16 16" width="12" height="12" aria-hidden="true"><path d="M3 3l10 10M13 3L3 13" stroke="currentColor" stroke-width="2" stroke-linecap="round" /></svg>
    </button>
    <button v-if="player.status === 'error' && !player.manual" type="button" class="retry btn" @click="roster.retry(player.id)">다시 불러오기</button>
  </li>
</template>

<script setup lang="ts">
import type { Line } from '~/types/api'
import { effectiveMmr, type RosterPlayer } from '~/composables/useRoster'
import { LINE_LABEL, LINES, tierColor, tierShort } from '~/utils/lol'

const props = defineProps<{ player: RosterPlayer }>()
defineEmits<{ open: [id: string] }>()

const roster = useRoster()
const artFailed = ref(false)
const now = ref(Date.now())
let timer: ReturnType<typeof setInterval> | undefined

onMounted(() => {
  timer = setInterval(() => (now.value = Date.now()), 1000)
})
onBeforeUnmount(() => clearInterval(timer))

watch(() => props.player.featured?.loadingUrl, () => (artFailed.value = false))

const art = computed(() => (artFailed.value ? undefined : props.player.featured?.loadingUrl))
const displayName = computed(() => props.player.profile?.gameName ?? props.player.name.split('#')[0])
const tag = computed(() => props.player.profile?.tagLine ?? props.player.name.split('#')[1])
const initial = computed(() => (displayName.value || '?').slice(0, 1))
const effective = computed(() => effectiveMmr(props.player))
const waitLeft = computed(() => Math.max(0, Math.ceil(((props.player.retryAt ?? 0) - now.value) / 1000)))
const cardStyle = computed(() => ({ '--tier': tierColor(props.player.tier) }))

function roleOf(line: Line) {
  return props.player.lines.find((l) => l.line === line)?.lineRole
}

function laneTitle(line: Line) {
  const role = roleOf(line)
  return `${LINE_LABEL[line]}: ${role === 'MAINLINE' ? '주 라인' : role === 'SUBLINE' ? '부 라인' : '선택 안 함'} (누를 때마다 주 → 부 → 해제)`
}
</script>

<style scoped>
.card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  border-radius: var(--r-m);
  background: var(--panel);
  border: 1px solid var(--line);
  overflow: hidden;
  transition: border-color 0.15s ease, transform 0.2s var(--ease-out);
}

.card:hover {
  border-color: var(--line-strong);
}

.card.error {
  border-color: rgba(255, 78, 91, 0.6);
}

.open {
  position: relative;
  display: block;
  width: 100%;
  aspect-ratio: 308 / 460;
  padding: 0;
  border: 0;
  background: var(--panel-2);
  text-align: left;
  overflow: hidden;
}

.art {
  position: absolute;
  inset: 0;
}

.art img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 18%;
  transition: transform 0.4s var(--ease-out);
}

.open:hover .art img {
  transform: scale(1.04);
}

.initial {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  font-family: var(--font-display);
  font-size: 3.4rem;
  color: var(--tier);
  background:
    radial-gradient(circle at 50% 35%, color-mix(in srgb, var(--tier) 22%, transparent), transparent 60%),
    var(--panel-2);
}

.shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(7, 9, 18, 0) 38%, rgba(7, 9, 18, 0.92) 82%);
}

.top {
  position: absolute;
  top: 8px;
  left: 8px;
  right: 8px;
  display: flex;
  justify-content: space-between;
}

.tier-chip {
  padding: 1px 7px;
  border-radius: 4px;
  background: rgba(7, 9, 18, 0.78);
  border: 1px solid var(--tier);
  color: var(--tier);
  font-size: 0.95rem;
  font-weight: 800;
}

.member-chip {
  color: #ffd66b;
  font-size: 0.9rem;
  text-shadow: 0 0 6px rgba(0, 0, 0, 0.7);
  margin-right: 20px;
}

.bottom {
  position: absolute;
  left: 10px;
  right: 10px;
  bottom: 8px;
  display: grid;
  gap: 0;
}

.name {
  font-family: var(--font-display);
  font-size: 1.2rem;
  line-height: 1.15;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tag {
  font-size: var(--fs-xs);
  color: var(--muted);
  margin-top: -1px;
}

.meta {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-top: 2px;
}

.mmr {
  font-size: 1.35rem;
  font-weight: 800;
  line-height: 1;
}

.delta {
  font-size: 0.95rem;
  font-weight: 700;
}

.delta.up {
  color: var(--win);
}

.delta.down {
  color: var(--red);
}

.state {
  font-size: var(--fs-xs);
  color: var(--muted);
}

.state.err {
  color: #ff8a92;
  white-space: normal;
  line-height: 1.3;
}

.shimmer {
  position: absolute;
  inset: 0;
  background: linear-gradient(100deg, transparent 30%, rgba(255, 255, 255, 0.08) 50%, transparent 70%);
  background-size: 220% 100%;
  animation: shimmer 1.3s linear infinite;
}

@keyframes shimmer {
  from { background-position: 120% 0; }
  to { background-position: -120% 0; }
}

.lines {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 3px;
  padding: 8px 8px 0;
}

.lane {
  display: grid;
  place-items: center;
  height: 26px;
  padding: 0;
  border: 1px solid var(--line);
  border-radius: 4px;
  background: transparent;
  color: var(--faint);
}

.lane:hover:not(:disabled) {
  color: var(--muted);
  border-color: var(--line-strong);
}

.lane.mainline {
  background: var(--text);
  border-color: var(--text);
  color: var(--bg-deep);
}

.lane.subline {
  border-style: dashed;
  border-color: var(--muted);
  color: var(--text);
}

.lines-state {
  padding: 4px 8px 8px;
  font-size: 0.6875rem;
  color: var(--faint);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.lines-state.analyzing,
.lines-state.waiting {
  color: var(--warn);
}

.lines-state.auto {
  color: var(--muted);
}

.remove {
  position: absolute;
  top: 6px;
  right: 6px;
  display: grid;
  place-items: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: 0;
  border-radius: 50%;
  background: rgba(7, 9, 18, 0.7);
  color: var(--muted);
  opacity: 0;
  transition: opacity 0.15s ease;
}

.card:hover .remove,
.remove:focus-visible {
  opacity: 1;
}

.remove:hover {
  color: var(--text);
  background: var(--red);
}

.retry {
  position: absolute;
  left: 8px;
  right: 8px;
  top: 40%;
  min-height: 32px;
}

@media (max-width: 640px) {
  .open {
    aspect-ratio: 1 / 1.1;
  }
}

@media (hover: none) {
  .remove {
    opacity: 1;
  }
}
</style>
