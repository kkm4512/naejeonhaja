<template>
  <div class="page">
    <template v-if="ready">
    <UiTopBar :code="code" />
    <main class="main">
      <header class="head">
        <h1 class="title">기록·순위</h1>
        <div class="tabs" role="tablist">
          <button type="button" role="tab" class="tab" :aria-selected="tab === 'standings'" @click="tab = 'standings'">순위표</button>
          <button type="button" role="tab" class="tab" :aria-selected="tab === 'matches'" @click="tab = 'matches'">
            내전 기록 <span class="num">{{ total }}</span>
          </button>
        </div>
      </header>

      <p v-if="error" class="error">{{ error }}</p>

      <RecordsStandings v-if="tab === 'standings'" :rows="standings" />

      <section v-else class="matches">
        <p v-if="!matches.length && !loading" class="empty">저장된 내전이 아직 없어요.</p>
        <RecordsMatchCard
          v-for="m in matches"
          :key="m.id"
          :match="m"
          editable
          @decide="decide(m, $event)"
          @share="share(m)"
          @delete="remove(m)"
        />
        <button v-if="hasNext" type="button" class="btn more" :disabled="loading" @click="loadMore">더 보기</button>
      </section>
    </main>
    </template>
  </div>
</template>

<script setup lang="ts">
import type { MatchOut, StandingRow, TeamSide } from '~/types/api'
import { copyText } from '~/utils/clipboard'

const route = useRoute()
const session = useSession()
const api = useApi()
const toast = useToast()
const code = computed(() => String(route.params.code).toUpperCase())

const ready = ref(false)
const tab = ref<'standings' | 'matches'>('standings')
const standings = ref<StandingRow[]>([])
const matches = ref<MatchOut[]>([])
const total = ref(0)
const page = ref(0)
const hasNext = ref(false)
const loading = ref(false)
const error = ref('')

useHead(() => ({ title: `기록·순위 | ${session.get(code.value)?.name ?? code.value}` }))

onMounted(async () => {
  if (!session.get(code.value)) {
    navigateTo(`/?join=${code.value}`, { replace: true })
    return
  }
  ready.value = true
  await Promise.all([loadStandings(), loadMatches(0)])
  if (!standings.value.length && matches.value.length) tab.value = 'matches'
})

async function loadStandings() {
  try {
    standings.value = await api.standings(code.value)
  } catch (e: any) {
    error.value = e?.message ?? '순위를 불러오지 못했어요.'
  }
}

async function loadMatches(p: number) {
  loading.value = true
  try {
    const res = await api.listMatches(code.value, p, 20)
    matches.value = p === 0 ? res.items : [...matches.value, ...res.items]
    total.value = res.total
    hasNext.value = res.hasNext
    page.value = p
  } catch (e: any) {
    error.value = e?.message ?? '기록을 불러오지 못했어요.'
  } finally {
    loading.value = false
  }
}

function loadMore() {
  loadMatches(page.value + 1)
}

async function decide(m: MatchOut, winner: TeamSide | null) {
  try {
    const updated = await api.decideMatch(code.value, m.id, winner)
    matches.value = matches.value.map((x) => (x.id === m.id ? updated : x))
    await loadStandings()
    toast.show(winner ? `${winner === 'BLUE' ? '블루' : '레드'}팀 승리로 기록했어요.` : '승패 기록을 취소했어요.', 'ok')
  } catch (e: any) {
    toast.show(e?.message ?? '기록하지 못했어요.', 'error')
  }
}

async function share(m: MatchOut) {
  const ok = await copyText(`${location.origin}/m/${m.publicId}`)
  toast.show(ok ? '공유 링크를 복사했어요. 방 밖의 사람도 볼 수 있어요.' : `${location.origin}/m/${m.publicId}`, ok ? 'ok' : 'info', 4000)
}

async function remove(m: MatchOut) {
  try {
    await api.deleteMatch(code.value, m.id)
    matches.value = matches.value.filter((x) => x.id !== m.id)
    total.value = Math.max(0, total.value - 1)
    await loadStandings()
    toast.show('기록을 지웠어요. 승패 보정도 되돌렸어요.', 'ok')
  } catch (e: any) {
    toast.show(e?.message ?? '삭제하지 못했어요.', 'error')
  }
}
</script>

<style scoped>
.page {
  min-height: 100vh;
}

.main {
  max-width: 1040px;
  margin: 0 auto;
  padding: 28px var(--gutter) 60px;
  display: grid;
  gap: 20px;
}

.head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.title {
  font-family: var(--font-display);
  font-weight: 400;
  font-size: var(--fs-xl);
}

.tabs {
  display: flex;
  padding: 3px;
  border-radius: var(--r-s);
  background: var(--bg-deep);
  border: 1px solid var(--line);
}

.tab {
  min-height: 36px;
  padding: 0 16px;
  border: 0;
  border-radius: 4px;
  background: transparent;
  color: var(--muted);
  font-weight: 700;
}

.tab[aria-selected='true'] {
  background: var(--panel-2);
  color: var(--text);
}

.matches {
  display: grid;
  gap: 12px;
}

.empty {
  color: var(--muted);
  padding: 24px 0;
}

.more {
  justify-self: center;
}

.error {
  padding: 10px 12px;
  border-left: 3px solid var(--red);
  background: rgba(255, 78, 91, 0.1);
  font-size: var(--fs-sm);
}
</style>
