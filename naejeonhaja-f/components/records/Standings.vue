<template>
  <div class="standings">
    <p v-if="!rows.length" class="empty">
      아직 승패가 기록된 내전이 없어요. 드래프트에서 팀을 나눈 뒤 ‘이 팀으로 기록 시작’을 누르고, 끝나면 이긴 팀을 눌러 주세요.
    </p>
    <table v-else class="table">
      <thead>
        <tr>
          <th scope="col" class="c-rank">#</th>
          <th scope="col">멤버</th>
          <th scope="col" class="c-rec">전적</th>
          <th scope="col" class="c-wr">승률</th>
          <th scope="col" class="c-form">최근</th>
          <th scope="col" class="c-delta" title="내전 승패로 쌓인 보정치">내전 보정</th>
          <th scope="col" class="c-mmr">점수</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="(r, i) in rows" :key="r.memberId" :class="{ podium: i < 3 }">
          <td class="c-rank num">{{ i + 1 }}</td>
          <td>
            <span class="who">
              <img v-if="r.profileIconUrl" :src="r.profileIconUrl" alt="" width="32" height="32">
              <span v-else class="dot" aria-hidden="true">{{ r.name.slice(0, 1) }}</span>
              <span class="name">{{ r.name.split('#')[0] }}</span>
              <UiTierTag :tier="r.tier" :show-lp="false" class="tier" />
            </span>
          </td>
          <td class="c-rec num">{{ r.wins }}W {{ r.losses }}L</td>
          <td class="c-wr">
            <span class="wr">
              <span class="wr-bar"><span :style="{ width: `${r.winRate}%` }" /></span>
              <span class="num">{{ Math.round(r.winRate) }}%</span>
            </span>
          </td>
          <td class="c-form">
            <span class="form">
              <span v-for="(f, k) in r.form" :key="k" class="pill" :class="f === 'W' ? 'w' : 'l'">{{ f === 'W' ? '승' : '패' }}</span>
            </span>
            <span v-if="Math.abs(r.streak) >= 3" class="streak" :class="r.streak > 0 ? 'hot' : 'cold'">{{ Math.abs(r.streak) }}{{ r.streak > 0 ? '연승' : '연패' }}</span>
          </td>
          <td class="c-delta num" :class="r.inhouseDelta > 0 ? 'up' : r.inhouseDelta < 0 ? 'down' : ''">
            {{ r.inhouseDelta > 0 ? '+' : '' }}{{ r.inhouseDelta }}
          </td>
          <td class="c-mmr num">{{ r.effectiveMmr }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<script setup lang="ts">
import type { StandingRow } from '~/types/api'

defineProps<{ rows: StandingRow[] }>()
</script>

<style scoped>
.empty {
  padding: 32px 0;
  color: var(--muted);
  line-height: 1.6;
}

.table {
  width: 100%;
  border-collapse: collapse;
}

th {
  padding: 10px 12px;
  font-size: var(--fs-xs);
  font-weight: 600;
  color: var(--muted);
  text-align: left;
  border-bottom: 1px solid var(--line-strong);
}

td {
  padding: 10px 12px;
  border-bottom: 1px solid var(--line);
  vertical-align: middle;
}

.c-rank {
  width: 48px;
  font-size: 1.4rem;
  font-weight: 800;
  color: var(--muted);
}

.podium .c-rank {
  color: var(--text);
}

.who {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.who img,
.dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  flex: none;
}

.dot {
  display: grid;
  place-items: center;
  background: var(--panel-2);
  font-family: var(--font-display);
  color: var(--muted);
}

.name {
  font-weight: 700;
}

.c-rec,
.c-mmr,
.c-delta {
  font-size: 1.15rem;
  font-weight: 700;
  white-space: nowrap;
}

.wr {
  display: flex;
  align-items: center;
  gap: 8px;
}

.wr-bar {
  width: 80px;
  height: 6px;
  border-radius: 999px;
  background: var(--bg-deep);
  overflow: hidden;
}

.wr-bar span {
  display: block;
  height: 100%;
  background: var(--text);
}

.wr .num {
  font-size: 1.1rem;
  font-weight: 700;
}

.form {
  display: inline-flex;
  gap: 3px;
}

.pill {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border-radius: 4px;
  font-size: 0.6875rem;
  font-weight: 800;
}

.pill.w {
  background: rgba(62, 139, 255, 0.22);
  color: var(--blue);
}

.pill.l {
  background: rgba(255, 78, 91, 0.18);
  color: var(--red);
}

.streak {
  margin-left: 8px;
  font-size: var(--fs-xs);
  font-weight: 700;
}

.streak.hot {
  color: var(--warn);
}

.streak.cold {
  color: var(--muted);
}

.up {
  color: var(--win);
}

.down {
  color: var(--red);
}

@media (max-width: 760px) {
  .c-form,
  .c-delta,
  .tier {
    display: none;
  }
  .wr-bar {
    display: none;
  }
  th,
  td {
    padding: 8px 6px;
  }
}
</style>
