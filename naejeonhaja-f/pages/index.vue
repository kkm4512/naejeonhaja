<template>
  <div class="landing">
    <div class="art" aria-hidden="true">
      <div class="half left"><img :src="leftArt" alt=""></div>
      <div class="half right"><img :src="rightArt" alt=""></div>
      <div class="veil" />
    </div>

    <main class="hero">
      <h1 class="wordmark">
        <span class="w-blue">내전</span><span class="w-red">하자</span>
      </h1>
      <p class="lede">
        소환사 이름만 붙여 넣으세요. 티어와 주 라인을 읽어 와<br class="br">
        가장 팽팽한 5대5를 짜 드려요.
      </p>

      <section class="panel" aria-label="방 입장">
        <div class="tabs" role="tablist">
          <button type="button" role="tab" class="tab" :aria-selected="tab === 'join'" @click="tab = 'join'">방 들어가기</button>
          <button type="button" role="tab" class="tab" :aria-selected="tab === 'create'" @click="tab = 'create'">방 만들기</button>
        </div>

        <form v-if="tab === 'join'" class="form" @submit.prevent="join">
          <label class="label" for="join-code">입장 코드</label>
          <input id="join-code" v-model="joinCode" class="field code-input num" maxlength="6" placeholder="ABC123" autocomplete="off" autocapitalize="characters" @input="joinCode = joinCode.toUpperCase()">
          <label class="label" for="join-pw">비밀번호</label>
          <input id="join-pw" v-model="joinPw" class="field" type="password" autocomplete="current-password">
          <button type="submit" class="btn btn--white submit" :disabled="busy || joinCode.length < 6 || !joinPw">{{ busy ? '들어가는 중…' : '들어가기' }}</button>
        </form>

        <form v-else class="form" @submit.prevent="create">
          <label class="label" for="create-name">방 이름</label>
          <input id="create-name" v-model="createName" class="field" maxlength="40" placeholder="예: 금요일 내전팟">
          <label class="label" for="create-pw">비밀번호 <span class="label-note">4자 이상, 친구들과 같이 씁니다</span></label>
          <input id="create-pw" v-model="createPw" class="field" type="password" autocomplete="new-password">
          <button type="submit" class="btn btn--white submit" :disabled="busy || !createName.trim() || createPw.length < 4">{{ busy ? '만드는 중…' : '방 만들기' }}</button>
        </form>

        <p v-if="error" class="error" role="alert">{{ error }}</p>

        <div v-if="session.sessions.value.length" class="recent">
          <p class="recent-title">최근 들어간 방</p>
          <ul>
            <li v-for="s in session.sessions.value" :key="s.code">
              <NuxtLink :to="`/g/${s.code}`" class="recent-item">
                <span class="recent-name">{{ s.name }}</span>
                <span class="num recent-code">{{ s.code }}</span>
              </NuxtLink>
            </li>
          </ul>
        </div>

        <NuxtLink to="/play" class="try">방 없이 바로 써보기 (저장 안 됨)</NuxtLink>
      </section>

      <ul class="facts">
        <li><strong>검색 한 번</strong>티어·LP·모스트 3 라인까지 자동</li>
        <li><strong>전수 탐색</strong>라인을 지키는 조합 중 MMR 차이 최소</li>
        <li><strong>내전 MMR</strong>승패를 기록할수록 정확해져요</li>
      </ul>
    </main>
  </div>
</template>

<script setup lang="ts">
const route = useRoute()
const api = useApi()
const session = useSession()

useHead({ title: '내전하자 | 롤 내전 팀 나누기' })

const leftArt = 'https://ddragon.leagueoflegends.com/cdn/img/champion/splash/Garen_0.jpg'
const rightArt = 'https://ddragon.leagueoflegends.com/cdn/img/champion/splash/Darius_0.jpg'

const tab = ref<'join' | 'create'>('join')
const joinCode = ref('')
const joinPw = ref('')
const createName = ref('')
const createPw = ref('')
const busy = ref(false)
const error = ref('')

onMounted(() => {
  session.load()
  const j = route.query.join
  if (typeof j === 'string' && j) {
    tab.value = 'join'
    joinCode.value = j.toUpperCase().slice(0, 6)
    nextTick(() => document.getElementById('join-pw')?.focus())
  }
})

watch(tab, () => (error.value = ''))

async function join() {
  busy.value = true
  error.value = ''
  try {
    const s = await api.joinGroup(joinCode.value, joinPw.value)
    session.save(s)
    await navigateTo(`/g/${s.code}`)
  } catch (e: any) {
    error.value = e?.message ?? '들어가지 못했어요.'
  } finally {
    busy.value = false
  }
}

async function create() {
  busy.value = true
  error.value = ''
  try {
    const s = await api.createGroup(createName.value.trim(), createPw.value)
    session.save(s)
    await navigateTo(`/g/${s.code}`)
  } catch (e: any) {
    error.value = e?.message ?? '방을 만들지 못했어요.'
  } finally {
    busy.value = false
  }
}
</script>

<style scoped>
.landing {
  position: relative;
  min-height: 100vh;
  overflow: hidden;
  background: var(--bg-deep);
}

/* 배경: 두 진영 일러스트를 사선으로 가른다 */
.art {
  position: absolute;
  inset: 0;
}

.half {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 58%;
  overflow: hidden;
}

.half img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.half.left {
  left: 0;
  clip-path: polygon(0 0, 100% 0, 78% 100%, 0 100%);
}

.half.left img {
  object-position: 30% 20%;
  filter: saturate(0.85);
}

.half.left::after,
.half.right::after {
  content: '';
  position: absolute;
  inset: 0;
  mix-blend-mode: multiply;
}

.half.left::after {
  background: linear-gradient(90deg, #0d2c66 0%, rgba(62, 139, 255, 0.55) 100%);
}

.half.right {
  right: 0;
  clip-path: polygon(22% 0, 100% 0, 100% 100%, 0 100%);
}

.half.right img {
  object-position: 70% 20%;
  filter: saturate(0.85);
}

.half.right::after {
  background: linear-gradient(270deg, #5c0f18 0%, rgba(255, 78, 91, 0.55) 100%);
}

.veil {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 50% 45%, rgba(7, 9, 18, 0.55), rgba(7, 9, 18, 0.92) 70%),
    linear-gradient(180deg, transparent 60%, var(--bg-deep));
}

.hero {
  position: relative;
  z-index: 1;
  display: grid;
  justify-items: center;
  gap: 22px;
  min-height: 100vh;
  padding: clamp(48px, 10vh, 110px) var(--gutter) 40px;
  text-align: center;
}

.wordmark {
  font-family: var(--font-display);
  font-weight: 400;
  font-size: clamp(4.5rem, 14vw, 10rem);
  line-height: 0.9;
  letter-spacing: -0.02em;
  animation: rise 0.8s var(--ease-out) both;
}

.w-blue {
  color: var(--blue);
  text-shadow: 0 0 60px var(--blue-glow);
}

.w-red {
  color: var(--red);
  text-shadow: 0 0 60px var(--red-glow);
}

@keyframes rise {
  from { opacity: 0; transform: translateY(18px) scale(0.98); letter-spacing: 0.06em; }
  to { opacity: 1; transform: none; }
}

.lede {
  max-width: 34em;
  font-size: var(--fs-md);
  color: rgba(238, 240, 247, 0.82);
  line-height: 1.6;
}

.panel {
  display: grid;
  gap: 14px;
  width: min(420px, 100%);
  padding: 22px;
  border-radius: 16px;
  background: rgba(11, 14, 26, 0.82);
  border: 1px solid var(--line-strong);
  backdrop-filter: blur(14px);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.5);
  text-align: left;
}

.tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  padding: 3px;
  border-radius: var(--r-s);
  background: var(--bg-deep);
}

.tab {
  min-height: 38px;
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

.form {
  display: grid;
  gap: 6px;
}

.label {
  margin-top: 6px;
  font-size: var(--fs-xs);
  color: var(--muted);
  font-weight: 600;
}

.label-note {
  color: var(--faint);
  font-weight: 500;
  margin-left: 4px;
}

.code-input {
  font-size: 1.6rem;
  font-weight: 800;
  letter-spacing: 0.3em;
  text-transform: uppercase;
}

.submit {
  margin-top: 12px;
  min-height: 48px;
  font-size: var(--fs-base);
}

.error {
  padding: 10px 12px;
  border-left: 3px solid var(--red);
  background: rgba(255, 78, 91, 0.1);
  font-size: var(--fs-sm);
}

.recent {
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

.recent-title {
  font-size: var(--fs-xs);
  color: var(--muted);
  margin-bottom: 6px;
}

.recent ul {
  margin: 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 4px;
}

.recent-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border-radius: var(--r-s);
  background: var(--panel);
  text-decoration: none;
  font-weight: 700;
}

.recent-item:hover {
  background: var(--panel-2);
}

.recent-code {
  color: var(--muted);
  letter-spacing: 0.1em;
}

.try {
  justify-self: center;
  color: var(--muted);
  font-size: var(--fs-sm);
  text-underline-offset: 3px;
}

.try:hover {
  color: var(--text);
}

.facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 220px));
  gap: 20px;
  margin: 12px 0 0;
  padding: 0;
  list-style: none;
  color: var(--muted);
  font-size: var(--fs-sm);
  text-align: left;
}

.facts strong {
  display: block;
  color: var(--text);
  font-family: var(--font-display);
  font-weight: 400;
  font-size: var(--fs-md);
}

@media (max-width: 720px) {
  .br {
    display: none;
  }
  .facts {
    grid-template-columns: 1fr;
    gap: 10px;
  }
  .half {
    width: 70%;
  }
}
</style>
