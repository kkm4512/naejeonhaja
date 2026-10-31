<template>
  <article class="match" :class="[match.winner ? `won-${match.winner.toLowerCase()}` : 'open', { big }]">
    <header class="head">
      <p class="when">
        <time :datetime="match.createdAt">{{ formatDate(match.createdAt) }}</time>
        <span class="mode">{{ match.mode === 'RIFT' ? '소환사의 협곡' : '칼바람 나락' }}</span>
      </p>
      <p class="diff">
        <span class="num">±{{ match.mmrDiff }}</span>
        <span class="grade">{{ match.balanceDescription }}</span>
      </p>
    </header>

    <div class="teams">
      <div class="team blue" :class="{ winner: match.winner === 'BLUE', loser: match.winner === 'RED' }">
        <p class="team-head">
          <span class="team-name num">BLUE</span>
          <span class="team-total num">{{ match.blueTotal }}</span>
          <span v-if="match.winner === 'BLUE'" class="win-badge">승리</span>
        </p>
        <ul>
          <li v-for="p in match.blue" :key="p.name">
            <UiLaneIcon v-if="p.line" :line="p.line" :size="14" />
            <span class="pname">{{ p.name.split('#')[0] }}</span>
            <span v-if="p.ratingDelta" class="delta num" :class="p.ratingDelta > 0 ? 'up' : 'down'">{{ p.ratingDelta > 0 ? '+' : '' }}{{ p.ratingDelta }}</span>
          </li>
        </ul>
      </div>
      <span class="vs num" aria-hidden="true">VS</span>
      <div class="team red" :class="{ winner: match.winner === 'RED', loser: match.winner === 'BLUE' }">
        <p class="team-head">
          <span v-if="match.winner === 'RED'" class="win-badge">승리</span>
          <span class="team-total num">{{ match.redTotal }}</span>
          <span class="team-name num">RED</span>
        </p>
        <ul>
          <li v-for="p in match.red" :key="p.name">
            <span v-if="p.ratingDelta" class="delta num" :class="p.ratingDelta > 0 ? 'up' : 'down'">{{ p.ratingDelta > 0 ? '+' : '' }}{{ p.ratingDelta }}</span>
            <span class="pname">{{ p.name.split('#')[0] }}</span>
            <UiLaneIcon v-if="p.line" :line="p.line" :size="14" />
          </li>
        </ul>
      </div>
    </div>

    <footer v-if="editable" class="foot">
      <template v-if="!match.winner">
        <span class="pending">결과 대기 중</span>
        <button type="button" class="btn btn--blue small" @click="$emit('decide', 'BLUE')">블루 승</button>
        <button type="button" class="btn btn--red small" @click="$emit('decide', 'RED')">레드 승</button>
      </template>
      <button v-else type="button" class="btn btn--ghost small" @click="$emit('decide', null)">승패 취소</button>
      <span class="spacer" />
      <button type="button" class="btn btn--ghost small" @click="$emit('share')">공유 링크</button>
      <button type="button" class="btn btn--ghost small danger" :class="{ armed }" @click="del">{{ armed ? '정말 삭제' : '삭제' }}</button>
    </footer>
  </article>
</template>

<script setup lang="ts">
import type { MatchOut, TeamSide } from '~/types/api'

defineProps<{ match: MatchOut; editable?: boolean; big?: boolean }>()
const emit = defineEmits<{ decide: [winner: TeamSide | null]; share: []; delete: [] }>()

const armed = ref(false)

function del() {
  if (!armed.value) {
    armed.value = true
    setTimeout(() => (armed.value = false), 3000)
    return
  }
  armed.value = false
  emit('delete')
}

function formatDate(iso: string) {
  const d = new Date(iso)
  return d.toLocaleString('ko-KR', { timeZone: 'Asia/Seoul', month: 'long', day: 'numeric', weekday: 'short', hour: '2-digit', minute: '2-digit' })
}
</script>

<style scoped>
.match {
  display: grid;
  gap: 14px;
  padding: 16px 18px;
  border-radius: var(--r-m);
  background: var(--panel);
  border: 1px solid var(--line);
}

.match.won-blue {
  background: linear-gradient(90deg, rgba(62, 139, 255, 0.14), var(--panel) 45%);
}

.match.won-red {
  background: linear-gradient(270deg, rgba(255, 78, 91, 0.14), var(--panel) 45%);
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
}

.when {
  display: flex;
  gap: 10px;
  font-size: var(--fs-sm);
  font-weight: 600;
}

.mode {
  color: var(--muted);
  font-weight: 500;
}

.diff {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.diff .num {
  font-size: 1.3rem;
  font-weight: 800;
}

.grade {
  font-size: var(--fs-xs);
  color: var(--muted);
}

.teams {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto minmax(0, 1fr);
  align-items: start;
  gap: 14px;
}

.team ul {
  margin: 6px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 3px;
}

.team li {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: var(--fs-sm);
  min-width: 0;
}

.team.red li,
.team.red .team-head {
  justify-content: flex-end;
}

.team.blue li {
  color: var(--text);
}

.team.loser {
  opacity: 0.6;
}

.team-head {
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.team-name {
  font-style: italic;
  font-weight: 800;
  font-size: 1.3rem;
}

.blue .team-name {
  color: var(--blue);
}

.red .team-name {
  color: var(--red);
}

.team-total {
  color: var(--muted);
  font-weight: 600;
}

.win-badge {
  padding: 0 8px;
  border-radius: 999px;
  background: var(--win);
  color: var(--bg-deep);
  font-size: var(--fs-xs);
  font-weight: 800;
}

.pname {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.delta {
  font-size: 0.85rem;
  font-weight: 700;
}

.up {
  color: var(--win);
}

.down {
  color: var(--red);
}

.vs {
  align-self: center;
  font-style: italic;
  font-weight: 800;
  font-size: 1.4rem;
  color: var(--faint);
}

.foot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

.pending {
  font-size: var(--fs-xs);
  color: var(--warn);
  margin-right: 4px;
}

.spacer {
  flex: 1;
}

.small {
  min-height: 32px;
  padding: 0 12px;
}

.danger.armed,
.danger:hover {
  color: var(--red) !important;
}

.big .team li {
  font-size: var(--fs-base);
}

@media (max-width: 560px) {
  .teams {
    grid-template-columns: 1fr;
  }
  .vs {
    display: none;
  }
  .team.red li,
  .team.red .team-head {
    justify-content: flex-start;
    flex-direction: row-reverse;
  }
}
</style>
