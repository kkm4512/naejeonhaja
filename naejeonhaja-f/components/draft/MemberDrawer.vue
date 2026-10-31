<template>
  <Transition name="sheet">
    <div v-if="open" class="backdrop" @click.self="open = false">
      <aside class="drawer" role="dialog" aria-modal="true" aria-label="단골 멤버" @keydown.esc="open = false">
        <header class="head">
          <h2 class="title">단골 멤버</h2>
          <span class="num count">{{ roster.members.value.length }}</span>
          <button ref="closeBtn" type="button" class="close" aria-label="닫기" @click="open = false">
            <svg viewBox="0 0 16 16" width="16" height="16" aria-hidden="true"><path d="M3 3l10 10M13 3L3 13" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" /></svg>
          </button>
        </header>
        <div class="tools">
          <label class="visually-hidden" for="member-filter">멤버 검색</label>
          <input id="member-filter" v-model="q" class="field" placeholder="이름으로 찾기">
          <button type="button" class="btn" :disabled="!selectable.length || roster.isFull.value" @click="addChecked">
            선택한 {{ checked.size }}명 대기열에 넣기
          </button>
        </div>

        <p v-if="!roster.members.value.length" class="empty">
          아직 저장된 멤버가 없어요. 대기열 카드를 눌러 ‘단골 멤버로 저장’하면 다음부터 여기서 바로 넣을 수 있어요.
        </p>

        <ul class="list">
          <li v-for="m in filtered" :key="m.id" class="row" :class="{ inRoster: inRoster.has(m.id) }">
            <label class="check">
              <input type="checkbox" :disabled="inRoster.has(m.id)" :checked="checked.has(m.id)" @change="toggle(m.id)">
              <span class="visually-hidden">{{ m.name }} 선택</span>
            </label>
            <img v-if="m.profileIconUrl" :src="m.profileIconUrl" alt="" width="36" height="36" class="icon">
            <span v-else class="icon dot" aria-hidden="true">{{ m.name.slice(0, 1) }}</span>
            <div class="who">
              <p class="name">{{ m.name }}</p>
              <p class="meta">
                <UiTierTag :tier="m.tier" :show-lp="false" />
                <span class="lanes">
                  <UiLaneIcon v-for="l in m.lines" :key="l.line" :line="l.line" :size="14" :class="l.lineRole === 'SUBLINE' ? 'sub' : ''" />
                </span>
                <span v-if="m.wins + m.losses" class="num rec">{{ m.wins }}W {{ m.losses }}L</span>
              </p>
            </div>
            <span class="score num" :title="`기본 ${m.baseMmr}, 내전 보정 ${m.inhouseDelta}`">
              {{ m.effectiveMmr }}
              <span v-if="m.inhouseDelta" :class="m.inhouseDelta > 0 ? 'up' : 'down'">{{ m.inhouseDelta > 0 ? '+' : '' }}{{ m.inhouseDelta }}</span>
            </span>
            <span v-if="inRoster.has(m.id)" class="in">대기열</span>
            <button v-else type="button" class="btn btn--ghost add" :disabled="roster.isFull.value" @click="addOne(m)">넣기</button>
            <button type="button" class="del" :aria-label="`${m.name} 삭제`" :class="{ armed: armed === m.id }" @click="remove(m)">
              {{ armed === m.id ? '삭제?' : '' }}
              <svg v-if="armed !== m.id" viewBox="0 0 16 16" width="13" height="13" aria-hidden="true"><path d="M3 4h10M6.5 4V2.5h3V4M5 4l.6 9h4.8L11 4" stroke="currentColor" stroke-width="1.4" fill="none" stroke-linejoin="round" /></svg>
            </button>
          </li>
        </ul>
      </aside>
    </div>
  </Transition>
</template>

<script setup lang="ts">
import type { MemberDto } from '~/types/api'
import { riotIdKey } from '~/utils/lol'

const open = defineModel<boolean>({ default: false })
const roster = useRoster()
const api = useApi()
const toast = useToast()
const q = ref('')
const checked = ref(new Set<number>())
const armed = ref<number | null>(null)
const closeBtn = ref<HTMLButtonElement | null>(null)

const inRoster = computed(() => new Set(roster.players.value.map((p) => p.memberId).filter(Boolean) as number[]))
const filtered = computed(() => {
  const key = riotIdKey(q.value)
  return roster.members.value.filter((m) => !key || riotIdKey(m.name).includes(key))
})
const selectable = computed(() => [...checked.value].filter((id) => !inRoster.value.has(id)))

watch(open, async (v) => {
  if (v) {
    checked.value = new Set()
    roster.loadMembers()
    await nextTick()
    closeBtn.value?.focus()
  }
})

function toggle(id: number) {
  const next = new Set(checked.value)
  next.has(id) ? next.delete(id) : next.add(id)
  checked.value = next
}

function addOne(m: MemberDto) {
  const problem = roster.addMember(m)
  if (problem) toast.show(problem, 'error')
}

function addChecked() {
  const problems: string[] = []
  for (const id of selectable.value) {
    const m = roster.members.value.find((x) => x.id === id)
    if (!m) continue
    const p = roster.addMember(m)
    if (p) problems.push(p)
  }
  checked.value = new Set()
  if (problems.length) toast.show(problems[0]!, 'error')
  else open.value = false
}

async function remove(m: MemberDto) {
  if (armed.value !== m.id) {
    armed.value = m.id
    setTimeout(() => { if (armed.value === m.id) armed.value = null }, 3000)
    return
  }
  armed.value = null
  const code = roster.groupCode.value
  if (!code) return
  try {
    await api.deleteMember(code, m.id)
    roster.members.value = roster.members.value.filter((x) => x.id !== m.id)
    for (const p of roster.players.value) {
      if (p.memberId === m.id) {
        p.memberId = undefined
        p.inhouseDelta = 0
      }
    }
    toast.show(`${m.name} 을(를) 단골 멤버에서 지웠어요.`, 'ok')
  } catch (e: any) {
    toast.show(e?.message ?? '삭제하지 못했어요.', 'error')
  }
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

.drawer {
  display: flex;
  flex-direction: column;
  width: min(520px, 100%);
  height: 100%;
  background: var(--bg);
  border-left: 1px solid var(--line);
}

.head {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 18px 20px 10px;
}

.title {
  font-family: var(--font-display);
  font-weight: 400;
  font-size: var(--fs-lg);
}

.count {
  font-size: 1.3rem;
  color: var(--muted);
  font-weight: 700;
}

.close {
  margin-left: auto;
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 50%;
  background: var(--panel);
}

.tools {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  padding: 0 20px 12px;
  border-bottom: 1px solid var(--line);
}

.empty {
  padding: 24px 20px;
  color: var(--muted);
  font-size: var(--fs-sm);
  line-height: 1.6;
}

.list {
  margin: 0;
  padding: 6px 12px 20px;
  list-style: none;
  overflow-y: auto;
  flex: 1;
}

.row {
  display: grid;
  grid-template-columns: 22px 36px minmax(0, 1fr) auto auto auto;
  align-items: center;
  gap: 10px;
  padding: 10px 8px;
  border-bottom: 1px solid var(--line);
}

.row.inRoster {
  opacity: 0.55;
}

.check input {
  width: 16px;
  height: 16px;
  accent-color: var(--text);
}

.icon {
  width: 36px;
  height: 36px;
  border-radius: 50%;
}

.dot {
  display: grid;
  place-items: center;
  background: var(--panel-2);
  font-family: var(--font-display);
  color: var(--muted);
}

.who {
  min-width: 0;
}

.name {
  font-weight: 700;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: var(--fs-xs);
}

.lanes {
  display: inline-flex;
  gap: 2px;
  color: var(--text);
}

.lanes .sub {
  opacity: 0.45;
}

.rec {
  color: var(--muted);
  font-size: 0.85rem;
}

.score {
  font-size: 1.2rem;
  font-weight: 800;
  text-align: right;
}

.score span {
  display: block;
  font-size: 0.8rem;
}

.up {
  color: var(--win);
}

.down {
  color: var(--red);
}

.in {
  font-size: var(--fs-xs);
  color: var(--muted);
}

.add {
  min-height: 32px;
  padding: 0 10px;
}

.del {
  display: grid;
  place-items: center;
  min-width: 30px;
  height: 30px;
  padding: 0 6px;
  border: 0;
  border-radius: var(--r-s);
  background: transparent;
  color: var(--faint);
  font-size: var(--fs-xs);
  font-weight: 700;
}

.del:hover,
.del.armed {
  color: var(--red);
  background: rgba(255, 78, 91, 0.1);
}

.sheet-enter-active,
.sheet-leave-active {
  transition: opacity 0.2s ease;
}

.sheet-enter-active .drawer,
.sheet-leave-active .drawer {
  transition: transform 0.28s var(--ease-out);
}

.sheet-enter-from,
.sheet-leave-to {
  opacity: 0;
}

.sheet-enter-from .drawer,
.sheet-leave-to .drawer {
  transform: translateX(40px);
}
</style>
