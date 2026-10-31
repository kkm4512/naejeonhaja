<template>
  <div class="console" :class="{ revealed: !!draft.result.value }">
    <!-- 결과 전 -->
    <template v-if="!draft.result.value">
      <div class="modes" role="radiogroup" aria-label="게임 모드">
        <button
          v-for="m in modes"
          :key="m.value"
          type="button"
          role="radio"
          class="mode"
          :aria-checked="draft.mode.value === m.value"
          @click="draft.mode.value = m.value"
        >
          {{ m.label }}
        </button>
      </div>

      <div v-if="draft.mode.value === 'rift'" class="coverage">
        <p class="cov-title">라인별 가능 인원</p>
        <ul class="cov-list">
          <li v-for="c in roster.coverage.value" :key="c.line" class="cov" :class="{ short: ready > 0 && c.total < 2 }" :title="`${LINE_LABEL[c.line]} ${c.total}명 (부 ${c.sub})`">
            <UiLaneIcon :line="c.line" :size="16" />
            <span class="cov-bar" aria-hidden="true">
              <span class="seg main" :style="{ height: pct(c.main) }" />
              <span class="seg sub" :style="{ height: pct(c.sub) }" />
            </span>
            <span class="cov-num num">{{ c.total }}</span>
          </li>
        </ul>
      </div>
      <p v-else class="abyss-note">칼바람은 라인 없이<br>MMR 합만 맞춰요</p>

      <button type="button" class="split" :disabled="!canSplit || draft.loading.value" @click="draft.split()">
        <span class="split-blue" aria-hidden="true" />
        <span class="split-red" aria-hidden="true" />
        <span class="split-text">{{ draft.loading.value ? '나누는 중…' : '팀 나누기' }}</span>
      </button>
      <p v-if="blocker" class="blocker">{{ blocker }}</p>
      <button v-if="showFill" type="button" class="btn fill" @click="fill">부족한 라인 채우기</button>
      <p v-if="draft.error.value" class="error" role="alert">{{ draft.error.value }}</p>
    </template>

    <!-- 결과 후 -->
    <template v-else>
      <p :key="draft.revealKey.value" class="vs num" aria-hidden="true">VS</p>
      <div class="diff">
        <span class="diff-label">MMR 차이</span>
        <span class="diff-num num">{{ shownDiff }}</span>
        <span class="grade" :class="draft.current.value?.balance.toLowerCase()">{{ draft.current.value?.balanceDescription }}</span>
      </div>
      <div class="tug" :aria-label="`블루 ${draft.blueTotal.value}, 레드 ${draft.redTotal.value}`" role="img">
        <span class="tug-blue" :style="{ width: `${bluePct}%` }"><span class="num">{{ draft.blueTotal.value }}</span></span>
        <span class="tug-red"><span class="num">{{ draft.redTotal.value }}</span></span>
      </div>

      <p v-if="draft.current.value?.offRoleCount" class="off-note">
        모든 라인을 지키는 조합이 없어 {{ draft.current.value.offRoleCount }}명은 고르지 않은 라인에 배정했어요. (점수를 낮춰 계산)
      </p>
      <div v-if="draft.options.value.length > 1 && !draft.saved.value" class="options" role="tablist" aria-label="다른 조합">
        <button
          v-for="(o, i) in draft.options.value"
          :key="i"
          type="button"
          role="tab"
          class="opt"
          :aria-selected="draft.selected.value === i"
          @click="draft.choose(i)"
        >
          <span>{{ i === 0 ? '최적' : `${i + 1}안` }}</span>
          <span class="opt-diff num">±{{ o.mmrDiff }}</span>
        </button>
      </div>

      <!-- 저장/승패 -->
      <div v-if="draft.saved.value" class="match-state">
        <template v-if="!draft.saved.value.winner">
          <p class="ms-title">경기가 끝나면 이긴 팀을 눌러 주세요</p>
          <div class="win-btns">
            <button type="button" class="btn btn--blue" @click="draft.decide('BLUE')">블루 승</button>
            <button type="button" class="btn btn--red" @click="draft.decide('RED')">레드 승</button>
          </div>
        </template>
        <template v-else>
          <p class="winner" :class="draft.saved.value.winner.toLowerCase()">
            {{ draft.saved.value.winner === 'BLUE' ? '블루' : '레드' }}팀 승리
          </p>
          <button type="button" class="btn btn--ghost small" @click="draft.decide(null)">승패 기록 취소</button>
        </template>
      </div>

      <div class="actions">
        <button v-if="roster.groupCode.value && !draft.saved.value" type="button" class="btn btn--white" @click="draft.saveMatch()">
          이 팀으로 기록 시작
        </button>
        <button v-if="!draft.saved.value" type="button" class="btn" @click="draft.swap()">진영 바꾸기</button>
        <button type="button" class="btn" @click="copy">{{ copied ? '복사했어요' : draft.saved.value ? '공유 링크 복사' : '결과 복사' }}</button>
        <button type="button" class="btn btn--ghost" @click="draft.reset()">{{ draft.saved.value ? '새 내전' : '다시 짜기' }}</button>
      </div>
      <p v-if="!roster.groupCode.value" class="guest-note">
        방을 만들면 이 팀을 저장하고 승패에 따라 내전 MMR 을 쌓을 수 있어요.
      </p>
    </template>
  </div>
</template>

<script setup lang="ts">
import { LINE_LABEL } from '~/utils/lol'
import { copyText } from '~/utils/clipboard'

const roster = useRoster()
const draft = useDraft()
const toast = useToast()

const modes = [
  { value: 'rift', label: '소환사의 협곡' },
  { value: 'abyss', label: '칼바람 나락' }
] as const

const ready = computed(() => roster.readyPlayers.value.length)
const loadingCount = computed(() => roster.players.value.filter((p) => p.status === 'loading').length)
const errorCount = computed(() => roster.players.value.filter((p) => p.status === 'error').length)
const analyzing = computed(() => roster.players.value.filter((p) => p.linesState === 'analyzing' || p.linesState === 'waiting').length)

const blocker = computed(() => {
  if (loadingCount.value) return `${loadingCount.value}명 불러오는 중이에요.`
  if (errorCount.value) return `불러오지 못한 ${errorCount.value}명을 다시 불러오거나 빼 주세요.`
  if (ready.value < 10) return `${10 - ready.value}명 더 필요해요.`
  if (draft.mode.value === 'rift') {
    const short = roster.coverage.value.filter((c) => c.total < 2).map((c) => LINE_LABEL[c.line])
    if (short.length) return `${short.join(', ')} 가능 인원이 부족해요. 그대로 나누면 누군가는 고르지 않은 라인에 가요.`
    if (analyzing.value) return `${analyzing.value}명은 아직 라인 분석 중이에요. 지금 나누면 그 사람은 ‘상관없음’으로 계산돼요.`
  }
  return ''
})
// 라인이 모자라도 나눌 수는 있다 (서버가 '라인 외' 배정을 최소로 해서라도 팀을 만든다)
const canSplit = computed(() => ready.value === 10 && !loadingCount.value && !errorCount.value)

const showFill = computed(() => draft.mode.value === 'rift' && ready.value === 10 && !loadingCount.value
  && roster.coverage.value.some((c) => c.total < 2))

function fill() {
  const n = roster.fillShortLines()
  toast.show(n ? `${n}명에게 부 라인을 추가했어요. 카드에서 점선으로 표시돼요.` : '채울 수 있는 사람이 없어요. 직접 라인을 바꿔 주세요.', n ? 'ok' : 'error')
}

function pct(n: number) {
  return `${Math.min(100, (n / 10) * 100)}%`
}

const bluePct = computed(() => {
  const b = draft.blueTotal.value
  const r = draft.redTotal.value
  return b + r === 0 ? 50 : Math.round((b / (b + r)) * 1000) / 10
})

// 차이 숫자 카운트업 (결과가 바뀔 때마다)
const shownDiff = ref(0)
let raf = 0
watch(() => [draft.current.value?.mmrDiff, draft.revealKey.value] as const, ([target]) => {
  if (import.meta.client) cancelAnimationFrame(raf)
  if (target == null) return
  // 서버 렌더링 중에는 window 가 없으므로 바로 값만
  if (!import.meta.client || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
    shownDiff.value = target
    return
  }
  const from = Math.max(target * 4, 400)
  const start = performance.now()
  const dur = 900
  const step = (t: number) => {
    const k = Math.min(1, (t - start) / dur)
    const eased = 1 - Math.pow(1 - k, 3)
    shownDiff.value = Math.round(from + (target - from) * eased)
    if (k < 1) raf = requestAnimationFrame(step)
  }
  raf = requestAnimationFrame(step)
}, { immediate: true })

const copied = ref(false)
async function copy() {
  const text = draft.saved.value ? `${location.origin}/m/${draft.saved.value.publicId}` : draft.shareText()
  const ok = await copyText(text)
  if (ok) {
    copied.value = true
    setTimeout(() => (copied.value = false), 1800)
  } else {
    toast.show('복사가 막혀 있어요. 결과 화면을 캡처해서 공유해 주세요.', 'error')
  }
}
</script>

<style scoped>
.console {
  display: grid;
  align-content: center;
  justify-items: center;
  gap: 18px;
  padding: 8px 0;
  text-align: center;
}

.modes {
  display: grid;
  grid-template-columns: 1fr 1fr;
  width: 100%;
  padding: 3px;
  border-radius: var(--r-s);
  background: var(--bg-deep);
  border: 1px solid var(--line);
}

.mode {
  min-height: 36px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--muted);
  font-size: var(--fs-sm);
  font-weight: 700;
}

.mode[aria-checked='true'] {
  background: var(--panel-2);
  color: var(--text);
}

.coverage {
  width: 100%;
}

.cov-title {
  font-size: var(--fs-xs);
  color: var(--muted);
  margin-bottom: 8px;
}

.cov-list {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 8px;
}

.cov {
  display: grid;
  justify-items: center;
  gap: 6px;
  color: var(--muted);
}

.cov-bar {
  display: flex;
  flex-direction: column-reverse;
  width: 12px;
  height: 64px;
  border-radius: 999px;
  background: var(--panel-2);
  box-shadow: inset 0 0 0 1px var(--line);
  overflow: hidden;
}

.seg.main {
  background: var(--text);
}

.seg.sub {
  background: repeating-linear-gradient(135deg, var(--muted) 0 3px, transparent 3px 6px);
}

.cov-num {
  font-size: 1.05rem;
  font-weight: 800;
  color: var(--text);
}

.cov.short,
.cov.short .cov-num {
  color: var(--warn);
}

.abyss-note {
  color: var(--muted);
  font-size: var(--fs-sm);
  line-height: 1.6;
}

/* 팀 나누기 버튼: 블루/레드 반쪽 */
.split {
  position: relative;
  width: 100%;
  min-height: 64px;
  border: 0;
  border-radius: var(--r-m);
  overflow: hidden;
  background: var(--panel-2);
  color: #fff;
  isolation: isolate;
}

.split-blue,
.split-red {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 56%;
  z-index: -1;
  transition: transform 0.35s var(--ease-out);
}

.split-blue {
  left: -6%;
  background: linear-gradient(90deg, var(--blue-deep), var(--blue));
  clip-path: polygon(0 0, 100% 0, 88% 100%, 0 100%);
}

.split-red {
  right: -6%;
  background: linear-gradient(270deg, var(--red-deep), var(--red));
  clip-path: polygon(12% 0, 100% 0, 100% 100%, 0 100%);
}

.split:hover:not(:disabled) .split-blue {
  transform: translateX(-4px);
}

.split:hover:not(:disabled) .split-red {
  transform: translateX(4px);
}

.split:disabled {
  opacity: 0.35;
}

.split-text {
  font-family: var(--font-display);
  font-size: 1.6rem;
  letter-spacing: 0.02em;
  text-shadow: 0 2px 10px rgba(0, 0, 0, 0.4);
}

.fill {
  width: 100%;
}

.off-note {
  font-size: var(--fs-xs);
  color: var(--warn);
  line-height: 1.5;
}

.blocker {
  font-size: var(--fs-sm);
  color: var(--muted);
  line-height: 1.5;
}

.error {
  padding: 10px 12px;
  border-left: 3px solid var(--red);
  background: rgba(255, 78, 91, 0.1);
  font-size: var(--fs-sm);
  text-align: left;
}

.vs {
  font-size: 4.6rem;
  font-weight: 800;
  font-style: italic;
  line-height: 0.85;
  background: linear-gradient(90deg, var(--blue) 0 50%, var(--red) 50% 100%);
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
  animation: slam 0.5s var(--ease-out) 0.45s both;
}

@keyframes slam {
  0% { transform: scale(2.2); opacity: 0; filter: blur(6px); }
  70% { transform: scale(0.94); opacity: 1; filter: blur(0); }
  100% { transform: scale(1); }
}

.diff {
  display: grid;
  justify-items: center;
  gap: 2px;
}

.diff-label {
  font-size: var(--fs-xs);
  color: var(--muted);
}

.diff-num {
  font-size: var(--fs-2xl);
  font-weight: 800;
  line-height: 0.95;
}

.grade {
  font-weight: 700;
  font-size: var(--fs-sm);
  color: var(--win);
}

.grade.fair,
.grade.poor {
  color: var(--warn);
}

.tug {
  display: flex;
  width: 100%;
  height: 28px;
  border-radius: 999px;
  overflow: hidden;
  font-size: 1rem;
  font-weight: 800;
}

.tug-blue,
.tug-red {
  display: flex;
  align-items: center;
  padding: 0 10px;
}

.tug-blue {
  background: var(--blue);
  transition: width 0.8s var(--ease-out);
}

.tug-red {
  flex: 1;
  justify-content: flex-end;
  background: var(--red);
}

.options {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
}

.opt {
  display: grid;
  min-width: 56px;
  padding: 6px 10px;
  border: 1px solid var(--line);
  border-radius: var(--r-s);
  background: transparent;
  font-size: var(--fs-sm);
  font-weight: 700;
}

.opt[aria-selected='true'] {
  border-color: var(--text);
  background: var(--panel-2);
}

.opt-diff {
  font-size: 0.85rem;
  color: var(--muted);
  font-weight: 600;
}

.match-state {
  display: grid;
  gap: 10px;
  width: 100%;
  padding: 14px;
  border-radius: var(--r-m);
  background: var(--panel);
  border: 1px solid var(--line);
}

.ms-title {
  font-size: var(--fs-sm);
  color: var(--muted);
}

.win-btns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.win-btns .btn {
  min-height: 48px;
  font-size: var(--fs-base);
}

.winner {
  font-family: var(--font-display);
  font-size: var(--fs-xl);
  line-height: 1;
}

.winner.blue {
  color: var(--blue);
}

.winner.red {
  color: var(--red);
}

.small {
  min-height: 30px;
  justify-self: center;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 6px;
}

.guest-note {
  font-size: var(--fs-xs);
  color: var(--faint);
  line-height: 1.5;
}
</style>
