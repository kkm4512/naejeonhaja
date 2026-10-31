<template>
  <header class="bar">
    <NuxtLink to="/" class="mark" aria-label="내전하자 처음 화면">
      <span class="mark-blue">내전</span><span class="mark-red">하자</span>
    </NuxtLink>

    <nav v-if="code" class="nav" aria-label="방 메뉴">
      <span class="room" :title="`입장 코드 ${code}`">
        <span class="room-name">{{ roomName }}</span>
        <button type="button" class="code num" :aria-label="`입장 코드 ${code} 복사`" @click="copyCode">{{ code }}</button>
      </span>
      <NuxtLink :to="`/g/${code}`" class="tab" exact-active-class="active">드래프트</NuxtLink>
      <NuxtLink :to="`/g/${code}/records`" class="tab" active-class="active">기록·순위</NuxtLink>
    </nav>
    <span v-else-if="guest" class="guest">저장 안 되는 체험 모드</span>

    <div class="right">
      <span class="status" role="status">
        <span class="dot" :class="state" aria-hidden="true" />
        <span class="status-text">{{ statusText }}</span>
      </span>
      <button v-if="code" type="button" class="btn btn--ghost leave" @click="leave">나가기</button>
    </div>
  </header>
  <p v-if="health && !health.riotApiKeyConfigured" class="notice">
    서버에 Riot API 키가 없어 소환사 검색이 꺼져 있어요. 프로젝트 폴더의 <code>.env</code> 에 <code>RIOT_API_KEY</code> 를 넣고
    <code>docker compose up -d</code> 로 다시 시작해 주세요. 그동안은 ‘직접 추가’로 쓸 수 있어요.
  </p>
</template>

<script setup lang="ts">
import type { HealthDto } from '~/types/api'
import { copyText } from '~/utils/clipboard'

const props = defineProps<{ code?: string | null; guest?: boolean }>()

const api = useApi()
const session = useSession()
const toast = useToast()
const health = useState<HealthDto | null>('health', () => null)
const state = ref<'checking' | 'up' | 'down'>(health.value ? 'up' : 'checking')

const roomName = computed(() => (props.code ? session.get(props.code)?.name ?? '' : ''))
const statusText = computed(() =>
  state.value === 'checking' ? '연결 확인 중' : state.value === 'down' ? '서버 연결 안 됨' : `패치 ${health.value?.ddragonVersion ?? ''}`
)

onMounted(async () => {
  try {
    health.value = await api.health()
    state.value = 'up'
  } catch {
    state.value = 'down'
  }
})

async function copyCode() {
  if (!props.code) return
  const ok = await copyText(`${location.origin}/?join=${props.code}`)
  toast.show(ok ? '입장 링크를 복사했어요. 친구에게 비밀번호와 함께 보내 주세요.' : `입장 코드: ${props.code}`, ok ? 'ok' : 'info', 3500)
}

function leave() {
  if (!props.code) return
  session.forget(props.code)
  navigateTo('/')
}
</script>

<style scoped>
.bar {
  position: sticky;
  top: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  gap: 20px;
  height: 60px;
  padding: 0 var(--gutter);
  background: rgba(11, 14, 26, 0.86);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid var(--line);
}

.mark {
  font-family: var(--font-display);
  font-size: 1.6rem;
  line-height: 1;
  text-decoration: none;
  letter-spacing: -0.01em;
}

.mark-blue {
  color: var(--blue);
}

.mark-red {
  color: var(--red);
}

.nav {
  display: flex;
  align-items: center;
  gap: 4px;
  min-width: 0;
}

.room {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-right: 12px;
  padding-right: 16px;
  border-right: 1px solid var(--line);
  min-width: 0;
}

.room-name {
  font-weight: 700;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.code {
  padding: 2px 8px;
  border: 1px solid var(--line-strong);
  border-radius: 4px;
  background: transparent;
  color: var(--muted);
  font-size: 0.95rem;
  font-weight: 600;
  letter-spacing: 0.08em;
}

.code:hover {
  color: var(--text);
  border-color: var(--muted);
}

.tab {
  padding: 8px 12px;
  border-radius: var(--r-s);
  color: var(--muted);
  font-weight: 600;
  font-size: var(--fs-sm);
  text-decoration: none;
}

.tab:hover {
  color: var(--text);
}

.tab.active {
  color: var(--text);
  background: var(--panel);
}

.guest {
  color: var(--muted);
  font-size: var(--fs-sm);
}

.right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--muted);
  font-size: var(--fs-xs);
}

.dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--faint);
}

.dot.up {
  background: var(--win);
  box-shadow: 0 0 8px var(--win);
}

.dot.down {
  background: var(--red);
}

.notice {
  margin: 12px var(--gutter) 0;
  padding: 12px 16px;
  border-left: 3px solid var(--warn);
  background: rgba(255, 179, 92, 0.08);
  font-size: var(--fs-sm);
}

code {
  padding: 1px 5px;
  border-radius: 4px;
  background: var(--panel-2);
  font-size: 0.95em;
}

@media (max-width: 760px) {
  .bar {
    gap: 12px;
    height: auto;
    min-height: 56px;
    flex-wrap: wrap;
    padding-top: 8px;
    padding-bottom: 8px;
  }
  .room {
    margin-right: 4px;
    padding-right: 8px;
  }
  .room-name {
    max-width: 90px;
  }
  .status-text {
    display: none;
  }
  .nav {
    order: 3;
    width: 100%;
  }
}
</style>
