<template>
  <div class="page">
    <header class="bar">
      <NuxtLink to="/" class="mark"><span class="b">내전</span><span class="r">하자</span></NuxtLink>
    </header>
    <main class="main">
      <p v-if="pending" class="note">불러오는 중…</p>
      <p v-else-if="error" class="note err">{{ error }}</p>
      <template v-else-if="match">
        <p class="room">{{ match.groupName }}</p>
        <h1 class="title" :class="match.winner?.toLowerCase()">
          {{ match.winner ? `${match.winner === 'BLUE' ? '블루' : '레드'}팀 승리` : '내전 대진표' }}
        </h1>
        <RecordsMatchCard :match="match" big />
        <NuxtLink to="/" class="cta btn btn--white">우리도 내전 팀 짜기</NuxtLink>
      </template>
    </main>
  </div>
</template>

<script setup lang="ts">
import type { MatchOut } from '~/types/api'

const route = useRoute()
const api = useApi()
const id = String(route.params.id)

const { data: match, pending, error: fetchError } = await useAsyncData<MatchOut>(`public-match-${id}`, () => api.publicMatch(id))
const error = computed(() => (fetchError.value ? (fetchError.value as any).message ?? '내전 기록을 찾을 수 없어요.' : ''))

useHead(() => ({
  title: match.value ? `${match.value.groupName} 내전 | 내전하자` : '내전 기록 | 내전하자',
  meta: match.value
    ? [{ name: 'description', content: `블루 ${match.value.blueTotal} vs 레드 ${match.value.redTotal}, 차이 ${match.value.mmrDiff}` }]
    : []
}))
</script>

<style scoped>
.page {
  min-height: 100vh;
  background:
    radial-gradient(ellipse at 0% 0%, rgba(62, 139, 255, 0.14), transparent 50%),
    radial-gradient(ellipse at 100% 0%, rgba(255, 78, 91, 0.14), transparent 50%),
    var(--bg);
}

.bar {
  padding: 18px var(--gutter);
}

.mark {
  font-family: var(--font-display);
  font-size: 1.6rem;
  text-decoration: none;
}

.b {
  color: var(--blue);
}

.r {
  color: var(--red);
}

.main {
  max-width: 760px;
  margin: 0 auto;
  padding: 20px var(--gutter) 60px;
  display: grid;
  gap: 16px;
}

.room {
  color: var(--muted);
  font-weight: 700;
}

.title {
  font-family: var(--font-display);
  font-weight: 400;
  font-size: var(--fs-2xl);
  line-height: 1;
}

.title.blue {
  color: var(--blue);
}

.title.red {
  color: var(--red);
}

.cta {
  justify-self: start;
  min-height: 46px;
}

.note {
  color: var(--muted);
}

.note.err {
  color: #ff8a92;
}
</style>
