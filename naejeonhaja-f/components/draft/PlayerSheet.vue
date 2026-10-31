<template>
  <Transition name="sheet">
    <div v-if="player" class="backdrop" @click.self="close">
      <aside
        class="sheet"
        role="dialog"
        aria-modal="true"
        :aria-label="`${player.name} 정보`"
        @keydown.esc="close"
      >
        <header class="hero" :style="{ '--tier': tierColor(player.tier) }">
          <span class="hero-art" aria-hidden="true">
            <img v-if="player.featured?.splashUrl" :src="player.featured.splashUrl" alt="">
          </span>
          <button ref="closeBtn" type="button" class="close" aria-label="닫기" @click="close">
            <svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true"><path d="M3 3l10 10M13 3L3 13" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" /></svg>
          </button>
          <div class="hero-body">
            <img v-if="player.profile?.profileIconUrl" class="icon" :src="player.profile.profileIconUrl" alt="" width="64" height="64">
            <div class="hero-text">
              <h2 class="name">{{ displayName }}<span v-if="tag" class="tag">#{{ tag }}</span></h2>
              <p class="hero-meta">
                <UiTierTag :tier="player.tier" :lp="player.lp" />
                <span v-if="player.profile" class="lvl">Lv.{{ player.profile.summonerLevel }}</span>
                <span v-if="rank" class="num rec">{{ rank.wins }}W {{ rank.losses }}L · {{ Math.round(rank.winRate) }}%</span>
                <span v-if="player.memberId" class="member">★ 단골 멤버</span>
              </p>
              <p v-if="player.featured" class="featured">대표 챔피언 {{ player.featured.championName }}</p>
            </div>
          </div>
        </header>

        <div class="body">
          <!-- MMR -->
          <section class="block">
            <h3 class="h3">팀 나누기에 쓰는 점수</h3>
            <div class="mmr-row">
              <span class="mmr-total num">{{ effective }}</span>
              <span class="mmr-expl">
                기본 <strong class="num">{{ player.mmr }}</strong>
                <template v-if="player.inhouseDelta">
                  + 내전 보정 <strong class="num" :class="player.inhouseDelta > 0 ? 'up' : 'down'">{{ player.inhouseDelta > 0 ? '+' : '' }}{{ player.inhouseDelta }}</strong>
                </template>
              </span>
            </div>
            <div class="mmr-edit">
              <label for="sheet-mmr" class="label">기본 점수 직접 정하기</label>
              <input id="sheet-mmr" class="field num-input" type="number" min="0" max="10000" step="10" :value="player.mmr" @change="onMmr">
              <button v-if="player.mmrEdited" type="button" class="btn btn--ghost" @click="roster.resetMmr(player.id)">추천값으로</button>
            </div>
            <p v-if="player.profile?.mmrNote && !player.mmrEdited" class="note">{{ player.profile.mmrNote }}</p>
            <div v-if="player.manual || player.tier === 'UNRANKED'" class="tier-edit">
              <label for="sheet-tier" class="label">티어</label>
              <select id="sheet-tier" class="field" :value="player.tier" @change="onTier">
                <option v-for="t in TIERS" :key="t" :value="t">{{ tierLabel(t) }}</option>
              </select>
            </div>
          </section>

          <!-- 라인 -->
          <section class="block">
            <h3 class="h3">가능 라인 <span class="h3-note">{{ linesNote }}</span></h3>
            <div class="lane-grid" role="group" aria-label="가능 라인">
              <button
                v-for="line in LINES"
                :key="line"
                type="button"
                class="lane-btn"
                :class="roleOf(line)?.toLowerCase()"
                :aria-pressed="!!roleOf(line)"
                @click="roster.cycleLine(player.id, line)"
              >
                <UiLaneIcon :line="line" :size="22" />
                <span>{{ LINE_LABEL[line] }}</span>
                <span class="role">{{ roleOf(line) === 'MAINLINE' ? '주' : roleOf(line) === 'SUBLINE' ? '부' : '' }}</span>
              </button>
            </div>
            <div class="lane-actions">
              <button v-if="player.lines.length" type="button" class="btn btn--ghost" @click="roster.setLines(player.id, [])">상관없음으로</button>
              <button v-if="!player.manual" type="button" class="btn btn--ghost" :disabled="player.linesState === 'analyzing'" @click="roster.reanalyze(player.id)">
                모스트 3으로 다시 분석
              </button>
            </div>
          </section>

          <!-- 표본 경기 요약 + 모스트 3 -->
          <section v-if="player.recent && player.recent.games" class="block">
            <h3 class="h3">{{ basisLabel(player.recent.basis) }} {{ player.recent.games }}판</h3>
            <div class="stats">
              <p><strong class="num">{{ player.recent.wins }}W {{ player.recent.games - player.recent.wins }}L</strong><span>승률 {{ Math.round(player.recent.winRate) }}%</span></p>
              <p><strong class="num">{{ player.recent.kda.toFixed(2) }}</strong><span>KDA</span></p>
            </div>
            <ul v-if="player.recent.positions.length" class="pos">
              <li v-for="p in player.recent.positions" :key="p.line">
                <UiLaneIcon :line="p.line" :size="16" />
                <span class="pos-bar"><span :style="{ width: `${p.ratio}%` }" /></span>
                <span class="num">{{ p.games }}</span>
              </li>
            </ul>
            <template v-if="player.recent.most3 && player.recent.most3.length">
              <p class="most-title">모스트 3 <span>→ 이 챔피언들의 포지션이 주 라인</span></p>
              <ul class="champs">
                <li v-for="c in player.recent.most3" :key="c.championId" :class="{ dim: !c.main }" :title="c.main ? '' : '1판뿐이라 주 라인에서 뺐어요'">
                  <img v-if="c.imageUrl" :src="c.imageUrl" :alt="c.championName" width="36" height="36">
                  <span>
                    <strong>{{ c.championName }}</strong>
                    <span class="num">{{ c.games }}판 {{ Math.round(c.winRate) }}%</span>
                    <span class="most-line"><UiLaneIcon :line="c.line" :size="12" /> {{ c.lineLabel }}</span>
                  </span>
                </li>
              </ul>
              <template v-if="player.recent.restLines && player.recent.restLines.length">
                <p class="most-title">부 라인 판별 <span>→ 모스트 3 밖 챔피언으로 2판 이상 간 라인</span></p>
                <ul class="rest">
                  <li v-for="r in player.recent.restLines" :key="r.line" :class="{ dim: !r.sub }">
                    <span class="rest-line">
                      <UiLaneIcon :line="r.line" :size="14" />
                      <strong>{{ r.lineLabel }}</strong>
                      <span class="num">{{ r.games }}판</span>
                      <span class="rest-tag" :class="{ on: r.sub }">{{ r.sub ? '부 라인' : '기준 미달' }}</span>
                    </span>
                    <span class="rest-champs">
                      <span v-for="c in r.champions" :key="c.championId" class="rest-champ" :title="`${c.championName} ${c.games}판 ${c.wins}승`">
                        <img v-if="c.imageUrl" :src="c.imageUrl" :alt="c.championName" width="26" height="26" loading="lazy">
                        <span>{{ c.championName }}<em v-if="c.games > 1" class="num"> ×{{ c.games }}</em></span>
                      </span>
                    </span>
                  </li>
                </ul>
              </template>
              <p v-else-if="player.recent.restLines" class="note">모스트 3 밖 챔피언으로 간 다른 라인이 없어 부 라인은 없어요.</p>
            </template>
            <ul v-else-if="player.recent.mostChampions.length" class="champs">
              <li v-for="c in player.recent.mostChampions" :key="c.championId">
                <img v-if="c.imageUrl" :src="c.imageUrl" :alt="c.championName" width="36" height="36">
                <span><strong>{{ c.championName }}</strong><span class="num">{{ c.games }}판 {{ Math.round(c.winRate) }}%</span></span>
              </li>
            </ul>
          </section>

          <!-- 경기 목록 -->
          <section v-if="!player.manual && player.status === 'ready'" class="block">
            <button v-if="!history && !historyLoading" type="button" class="btn" @click="loadHistory">최근 경기 자세히 보기</button>
            <p v-if="historyLoading" class="note">최근 경기를 불러오는 중…</p>
            <p v-if="historyError" class="note err">{{ historyError }}</p>
            <ul v-if="history" class="games">
              <li v-for="m in history.matches" :key="m.matchId" class="game" :class="m.remake ? 'remake' : m.win ? 'win' : 'loss'">
                <img v-if="m.championImageUrl" :src="m.championImageUrl" :alt="m.championName" width="40" height="40" loading="lazy">
                <div class="g-main">
                  <p class="g-top"><strong>{{ m.remake ? '다시하기' : m.win ? '승리' : '패배' }}</strong> {{ m.queueName }} · {{ timeAgo(m.gameStartTimestamp) }}</p>
                  <p class="g-kda num">{{ m.kills }}/{{ m.deaths }}/{{ m.assists }} <span>KDA {{ formatKda(m.kda, m.deaths) }}</span> <span>CS {{ m.cs }}</span></p>
                </div>
                <span v-if="m.position" class="g-pos"><UiLaneIcon :line="m.position" :size="16" /></span>
                <div class="g-items">
                  <span v-for="(it, i) in m.items.slice(0, 6)" :key="i" class="item"><img v-if="it" :src="it" alt="" width="22" height="22" loading="lazy"></span>
                </div>
              </li>
            </ul>
          </section>
        </div>

        <footer class="foot">
          <button v-if="roster.groupCode.value" type="button" class="btn btn--white" @click="roster.saveAsMember(player.id)">
            {{ player.memberId ? '단골 멤버 정보 갱신' : '단골 멤버로 저장' }}
          </button>
          <button type="button" class="btn btn--ghost danger" @click="removeAndClose">대기열에서 빼기</button>
        </footer>
      </aside>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import type { Line, MatchHistoryDto } from '~/types/api'
import { effectiveMmr } from '~/composables/useRoster'
import { formatKda, LINE_LABEL, LINES, TIERS, tierColor, tierLabel, timeAgo } from '~/utils/lol'

const openId = defineModel<string | null>({ default: null })

const roster = useRoster()
const api = useApi()
const closeBtn = ref<HTMLButtonElement | null>(null)
const history = ref<MatchHistoryDto | null>(null)
const historyLoading = ref(false)
const historyError = ref('')

const player = computed(() => roster.players.value.find((p) => p.id === openId.value))
const displayName = computed(() => player.value?.profile?.gameName ?? player.value?.name.split('#')[0] ?? '')
const tag = computed(() => player.value?.profile?.tagLine ?? player.value?.name.split('#')[1])
const rank = computed(() => player.value?.profile?.soloRank ?? player.value?.profile?.flexRank)
const effective = computed(() => (player.value ? effectiveMmr(player.value) : 0))
const linesNote = computed(() => {
  switch (player.value?.linesState) {
    case 'auto': return '이번 시즌 랭크 모스트 3 기준 자동 설정'
    case 'saved': return '단골 멤버에 저장된 라인'
    case 'edited': return '직접 설정'
    case 'analyzing': return '분석 중…'
    case 'waiting': return '호출 한도 대기 중'
    default: return '아무것도 안 고르면 상관없음'
  }
})

function basisLabel(basis?: string) {
  if (basis === 'SOLO') return '이번 시즌 솔로랭크'
  if (basis === 'RANKED') return '이번 시즌 랭크'
  if (basis === 'NORMAL') return '랭크 기록이 없어 최근 일반'
  return '최근'
}

let lastFocus: HTMLElement | null = null
watch(openId, async (id, prev) => {
  if (id !== prev) {
    history.value = null
    historyError.value = ''
  }
  if (id) {
    lastFocus = document.activeElement as HTMLElement | null
    document.body.style.overflow = 'hidden'
    await nextTick()
    closeBtn.value?.focus()
  } else {
    document.body.style.overflow = ''
    lastFocus?.focus?.()
  }
})

function close() {
  openId.value = null
}

function roleOf(line: Line) {
  return player.value?.lines.find((l) => l.line === line)?.lineRole
}

function onMmr(e: Event) {
  if (player.value) roster.setMmr(player.value.id, Number((e.target as HTMLInputElement).value))
}

function onTier(e: Event) {
  if (player.value) roster.setTier(player.value.id, (e.target as HTMLSelectElement).value)
}

async function loadHistory() {
  if (!player.value) return
  historyLoading.value = true
  historyError.value = ''
  try {
    history.value = await api.matches(player.value.name, 10)
  } catch (e: any) {
    historyError.value = e?.message ?? '불러오지 못했어요.'
  } finally {
    historyLoading.value = false
  }
}

function removeAndClose() {
  if (!player.value) return
  roster.remove(player.value.id)
  close()
}
</script>

<style scoped>
.backdrop {
  position: fixed;
  inset: 0;
  z-index: 60;
  display: flex;
  justify-content: flex-end;
  background: rgba(4, 6, 12, 0.66);
}

.sheet {
  display: flex;
  flex-direction: column;
  width: min(520px, 100%);
  height: 100%;
  background: var(--bg);
  border-left: 1px solid var(--line);
  overflow-y: auto;
}

.hero {
  position: relative;
  min-height: 200px;
  display: flex;
  align-items: flex-end;
  padding: 20px;
  overflow: hidden;
}

.hero-art {
  position: absolute;
  inset: 0;
}

.hero-art img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  object-position: center 20%;
}

.hero-art::after {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(11, 14, 26, 0.2), rgba(11, 14, 26, 0.96) 85%);
}

.close {
  position: absolute;
  top: 12px;
  right: 12px;
  z-index: 2;
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 50%;
  background: rgba(7, 9, 18, 0.7);
  color: var(--text);
}

.hero-body {
  position: relative;
  display: flex;
  align-items: flex-end;
  gap: 14px;
}

.icon {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  border: 2px solid var(--tier);
}

.name {
  font-family: var(--font-display);
  font-weight: 400;
  font-size: var(--fs-xl);
  line-height: 1;
}

.tag {
  font-family: var(--font-body);
  font-size: var(--fs-base);
  color: var(--muted);
  margin-left: 4px;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 12px;
  margin-top: 6px;
  font-size: var(--fs-sm);
}

.lvl,
.rec {
  color: var(--muted);
}

.member {
  color: #ffd66b;
  font-weight: 700;
}

.featured {
  margin-top: 4px;
  font-size: var(--fs-xs);
  color: var(--muted);
}

.body {
  display: grid;
  gap: 4px;
  padding: 4px 20px 20px;
  flex: 1;
}

.block {
  display: grid;
  gap: 10px;
  padding: 16px 0;
  border-bottom: 1px solid var(--line);
}

.h3 {
  font-size: var(--fs-sm);
  font-weight: 700;
}

.h3-note {
  margin-left: 6px;
  color: var(--muted);
  font-weight: 500;
  font-size: var(--fs-xs);
}

.mmr-row {
  display: flex;
  align-items: baseline;
  gap: 14px;
}

.mmr-total {
  font-size: 2.6rem;
  font-weight: 800;
  line-height: 1;
}

.mmr-expl {
  color: var(--muted);
  font-size: var(--fs-sm);
}

.mmr-expl strong {
  color: var(--text);
  font-size: 1.05rem;
}

.up {
  color: var(--win) !important;
}

.down {
  color: var(--red) !important;
}

.mmr-edit,
.tier-edit {
  display: grid;
  grid-template-columns: auto 120px auto;
  align-items: center;
  gap: 8px;
}

.tier-edit {
  grid-template-columns: auto 180px;
}

.label {
  font-size: var(--fs-xs);
  color: var(--muted);
}

.note {
  font-size: var(--fs-xs);
  color: var(--faint);
}

.note.err {
  color: #ff8a92;
}

.lane-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 6px;
}

.lane-btn {
  display: grid;
  justify-items: center;
  gap: 4px;
  padding: 10px 4px 8px;
  border: 1px solid var(--line);
  border-radius: var(--r-s);
  background: transparent;
  color: var(--muted);
  font-size: var(--fs-xs);
  font-weight: 700;
}

.lane-btn.mainline {
  background: var(--text);
  border-color: var(--text);
  color: var(--bg-deep);
}

.lane-btn.subline {
  border-style: dashed;
  border-color: var(--muted);
  color: var(--text);
}

.role {
  min-height: 1em;
  font-size: 0.6875rem;
}

.lane-actions {
  display: flex;
  gap: 6px;
}

.stats {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.stats p {
  display: grid;
  padding: 10px 12px;
  border-radius: var(--r-s);
  background: var(--panel);
}

.stats strong {
  font-size: 1.4rem;
  font-weight: 800;
}

.stats span {
  font-size: var(--fs-xs);
  color: var(--muted);
}

.pos,
.champs,
.games {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 6px;
}

.pos li {
  display: grid;
  grid-template-columns: 18px 1fr 24px;
  align-items: center;
  gap: 10px;
  color: var(--muted);
}

.pos-bar {
  height: 6px;
  border-radius: 999px;
  background: var(--panel);
  overflow: hidden;
}

.pos-bar span {
  display: block;
  height: 100%;
  background: var(--text);
}

.champs {
  grid-template-columns: repeat(3, 1fr);
}

.champs li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--fs-xs);
}

.champs img {
  border-radius: 6px;
}

.champs span {
  display: grid;
}

.champs .num {
  color: var(--muted);
}

.champs li.dim {
  opacity: 0.5;
}

.most-title {
  margin: 4px 0 0;
  font-size: var(--fs-xs);
  font-weight: 700;
}

.most-title span {
  color: var(--muted);
  font-weight: 400;
}

.rest {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 8px;
}

.rest li {
  display: grid;
  gap: 6px;
  padding: 8px 10px;
  border-radius: var(--r-s);
  background: var(--panel);
  font-size: var(--fs-xs);
}

.rest li.dim {
  opacity: 0.55;
}

.rest-line {
  display: flex;
  align-items: center;
  gap: 6px;
}

.rest-line .num {
  color: var(--muted);
}

.rest-tag {
  margin-left: auto;
  padding: 1px 7px;
  border-radius: 999px;
  border: 1px solid var(--line-strong);
  color: var(--muted);
  font-size: 0.6875rem;
}

.rest-tag.on {
  border-color: var(--warn);
  color: var(--warn);
}

.rest-champs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 10px;
}

.rest-champ {
  display: inline-flex;
  align-items: center;
  gap: 5px;
}

.rest-champ img {
  border-radius: 5px;
}

.rest-champ em {
  font-style: normal;
  color: var(--muted);
}

.most-line {
  display: inline-flex !important;
  align-items: center;
  gap: 3px;
  color: var(--muted);
}

.game {
  display: grid;
  grid-template-columns: 40px minmax(0, 1fr) auto;
  grid-template-areas: 'img main pos' 'img items items';
  align-items: center;
  gap: 2px 10px;
  padding: 8px 10px;
  border-radius: var(--r-s);
  background: var(--panel);
  border-left: 3px solid var(--faint);
}

.game.win {
  border-left-color: var(--blue);
}

.game.loss {
  border-left-color: var(--red);
}

.game img {
  grid-area: img;
  border-radius: 50%;
}

.g-main {
  grid-area: main;
  min-width: 0;
}

.g-top {
  font-size: var(--fs-xs);
  color: var(--muted);
}

.g-top strong {
  color: var(--text);
}

.win .g-top strong {
  color: var(--blue);
}

.loss .g-top strong {
  color: var(--red);
}

.g-kda {
  font-size: 1.05rem;
  font-weight: 700;
}

.g-kda span {
  font-size: 0.9rem;
  color: var(--muted);
  margin-left: 6px;
  font-weight: 600;
}

.g-pos {
  grid-area: pos;
  color: var(--muted);
}

.g-items {
  grid-area: items;
  display: flex;
  gap: 3px;
}

.item {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  background: var(--bg-deep);
  overflow: hidden;
}

.item img {
  border-radius: 4px;
}

.foot {
  position: sticky;
  bottom: 0;
  display: flex;
  gap: 8px;
  padding: 14px 20px;
  background: var(--bg);
  border-top: 1px solid var(--line);
}

.danger:hover {
  color: var(--red) !important;
}

.sheet-enter-active,
.sheet-leave-active {
  transition: opacity 0.2s ease;
}

.sheet-enter-active .sheet,
.sheet-leave-active .sheet {
  transition: transform 0.28s var(--ease-out);
}

.sheet-enter-from,
.sheet-leave-to {
  opacity: 0;
}

.sheet-enter-from .sheet,
.sheet-leave-to .sheet {
  transform: translateX(40px);
}
</style>
