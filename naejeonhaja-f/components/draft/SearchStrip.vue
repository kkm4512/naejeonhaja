<template>
  <section class="strip" aria-label="소환사 추가">
    <form class="search" :class="{ disabled: roster.isFull.value }" @submit.prevent="submit">
      <svg class="glass" viewBox="0 0 20 20" width="18" height="18" aria-hidden="true">
        <circle cx="8.5" cy="8.5" r="5.5" stroke="currentColor" stroke-width="2" fill="none" />
        <path d="M13 13l4.5 4.5" stroke="currentColor" stroke-width="2" stroke-linecap="round" />
      </svg>
      <label class="visually-hidden" for="summoner-search">소환사 검색</label>
      <textarea
        id="summoner-search"
        ref="box"
        v-model="text"
        rows="1"
        class="input"
        autocomplete="off"
        spellcheck="false"
        :disabled="roster.isFull.value"
        :placeholder="placeholder"
        @keydown.enter="onEnter"
        @input="grow"
        @focus="focused = true"
        @blur="onBlur"
      />
      <span v-if="parsed.length > 1" class="count num">{{ parsed.length }}명</span>
      <button type="submit" class="btn btn--white go" :disabled="!parsed.length || roster.isFull.value">
        {{ parsed.length > 1 ? `${parsed.length}명 불러오기` : '불러오기' }}
      </button>

      <ul v-if="suggestions.length" class="suggest" role="listbox" aria-label="단골 멤버">
        <li v-for="m in suggestions" :key="m.id">
          <button type="button" class="suggest-item" @mousedown.prevent="pick(m)">
            <img v-if="m.profileIconUrl" :src="m.profileIconUrl" alt="" width="28" height="28">
            <span v-else class="suggest-dot" aria-hidden="true">{{ m.name.slice(0, 1) }}</span>
            <span class="suggest-name">{{ m.name }}</span>
            <UiTierTag :tier="m.tier" :show-lp="false" />
            <span class="suggest-note">단골</span>
          </button>
        </li>
      </ul>
    </form>

    <div class="side-actions">
      <button v-if="roster.groupCode.value" type="button" class="btn" @click="$emit('open-members')">
        단골 멤버 <span class="num badge">{{ roster.members.value.length }}</span>
      </button>
      <button type="button" class="btn btn--ghost" :aria-expanded="manualOpen" @click="manualOpen = !manualOpen">직접 추가</button>
    </div>

    <form v-if="manualOpen" class="manual" @submit.prevent="submitManual">
      <label class="visually-hidden" for="manual-name">이름</label>
      <input id="manual-name" v-model="manualName" class="field" maxlength="24" placeholder="닉네임 (Riot 조회 없이)" :disabled="roster.isFull.value">
      <label class="visually-hidden" for="manual-tier">티어</label>
      <select id="manual-tier" v-model="manualTier" class="field tier-select">
        <option v-for="t in TIERS" :key="t" :value="t">{{ tierLabel(t) }}</option>
      </select>
      <button type="submit" class="btn" :disabled="!manualName.trim() || roster.isFull.value">추가</button>
    </form>

    <ul v-if="messages.length" class="messages" aria-live="polite">
      <li v-for="m in messages" :key="m">{{ m }}</li>
    </ul>
  </section>
</template>

<script setup lang="ts">
import type { MemberDto } from '~/types/api'
import { parseRiotIds, riotIdKey, TIERS, tierLabel } from '~/utils/lol'

defineEmits<{ 'open-members': [] }>()

const roster = useRoster()
const text = ref('')
const box = ref<HTMLTextAreaElement | null>(null)
const focused = ref(false)
const manualOpen = ref(false)
const manualName = ref('')
const manualTier = ref('GOLD_IV')
const messages = ref<string[]>([])

const parsed = computed(() => parseRiotIds(text.value))
const placeholder = computed(() =>
  roster.isFull.value
    ? '10명이 다 찼어요. 바꾸려면 한 명을 빼 주세요.'
    : '소환사 이름#태그 (여러 명은 줄바꿈, 로비 채팅 붙여넣기 가능)'
)

// 입력 중인 글자로 단골 멤버 추천 (명단에 없는 사람만)
const suggestions = computed<MemberDto[]>(() => {
  const q = riotIdKey(text.value)
  if (!focused.value || !q || text.value.includes('\n') || !roster.members.value.length) return []
  const inRoster = new Set(roster.players.value.map((p) => p.memberId))
  return roster.members.value
    .filter((m) => !inRoster.has(m.id) && (riotIdKey(m.name).includes(q) || riotIdKey(m.riotId ?? '').includes(q)))
    .slice(0, 6)
})

function onEnter(e: KeyboardEvent) {
  // 한글 조합 중 Enter 는 글자 확정용이므로 무시, Shift+Enter 는 줄바꿈
  if (e.isComposing || e.shiftKey) return
  e.preventDefault()
  if (suggestions.value.length === 1 && parsed.value.length === 1 && !parsed.value[0]!.includes('#')) {
    pick(suggestions.value[0]!)
    return
  }
  submit()
}

function submit() {
  if (!parsed.value.length) return
  messages.value = roster.addRiotIds(parsed.value)
  text.value = ''
  nextTick(grow)
}

function pick(m: MemberDto) {
  const problem = roster.addMember(m)
  messages.value = problem ? [problem] : []
  text.value = ''
  nextTick(grow)
}

function submitManual() {
  const problem = roster.addManual(manualName.value, manualTier.value)
  messages.value = problem ? [problem] : []
  if (!problem) manualName.value = ''
}

function grow() {
  const el = box.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = `${Math.min(el.scrollHeight, 160)}px`
}

function onBlur() {
  setTimeout(() => (focused.value = false), 120)
}
</script>

<style scoped>
.strip {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 10px 12px;
  align-items: start;
}

.search {
  position: relative;
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 6px 6px 16px;
  border: 1px solid var(--line-strong);
  border-radius: var(--r-m);
  background: var(--bg-deep);
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.search:focus-within {
  border-color: var(--text);
  box-shadow: 0 0 0 4px rgba(238, 240, 247, 0.06);
}

.search.disabled {
  opacity: 0.6;
}

.glass {
  color: var(--muted);
  flex: none;
}

.input {
  flex: 1;
  min-width: 0;
  min-height: 40px;
  max-height: 160px;
  padding: 9px 0;
  border: 0;
  background: transparent;
  resize: none;
  font-size: var(--fs-md);
  line-height: 1.4;
}

.input:focus {
  outline: none;
}

.input::placeholder {
  color: var(--faint);
  font-size: var(--fs-base);
}

.count {
  color: var(--muted);
  font-size: var(--fs-md);
  font-weight: 600;
}

.go {
  min-height: 44px;
  padding: 0 20px;
}

.suggest {
  position: absolute;
  top: calc(100% + 6px);
  left: 0;
  right: 0;
  z-index: 30;
  margin: 0;
  padding: 6px;
  list-style: none;
  background: var(--panel);
  border: 1px solid var(--line-strong);
  border-radius: var(--r-m);
  box-shadow: 0 18px 40px rgba(0, 0, 0, 0.5);
}

.suggest-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 8px 10px;
  border: 0;
  border-radius: var(--r-s);
  background: transparent;
  text-align: left;
}

.suggest-item:hover {
  background: var(--panel-2);
}

.suggest-item img,
.suggest-dot {
  width: 28px;
  height: 28px;
  border-radius: 50%;
}

.suggest-dot {
  display: grid;
  place-items: center;
  background: var(--panel-2);
  font-family: var(--font-display);
  color: var(--muted);
}

.suggest-name {
  flex: 1;
  font-weight: 700;
}

.suggest-note {
  font-size: var(--fs-xs);
  color: var(--muted);
}

.side-actions {
  display: flex;
  gap: 8px;
  padding-top: 8px;
}

.badge {
  min-width: 20px;
  padding: 0 6px;
  border-radius: 999px;
  background: var(--panel-2);
  font-size: 0.9rem;
}

.manual {
  grid-column: 1 / -1;
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(0, 1fr) auto;
  gap: 8px;
  max-width: 640px;
}

.messages {
  grid-column: 1 / -1;
  margin: 0;
  padding: 8px 14px;
  list-style: none;
  border-left: 3px solid var(--warn);
  background: rgba(255, 179, 92, 0.08);
  font-size: var(--fs-sm);
}

@media (max-width: 760px) {
  .strip {
    grid-template-columns: minmax(0, 1fr);
  }
  .search {
    padding-left: 12px;
  }
  .glass,
  .count {
    display: none;
  }
  .input {
    font-size: var(--fs-base);
  }
  .input::placeholder {
    font-size: var(--fs-sm);
  }
  .side-actions {
    padding-top: 0;
  }
  .go {
    padding: 0 14px;
  }
  .manual {
    grid-template-columns: 1fr 1fr;
  }
  .manual .btn {
    grid-column: 1 / -1;
  }
}
</style>
