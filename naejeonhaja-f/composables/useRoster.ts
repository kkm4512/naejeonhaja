import type {
  FeaturedChampion,
  InsightDto,
  Line,
  LineRole,
  MemberDto,
  PlayerLine,
  PlayerProfileDto,
  TeamRequestPlayer,
  Tier
} from '~/types/api'
import { LINES, riotIdKey, tierScore, tierScoreWithLp } from '~/utils/lol'

/**
 * linesState
 *  - none:      아직 정해지지 않음 (상관없음)
 *  - analyzing: 모스트 3으로 라인 분석 중
 *  - waiting:   Riot 호출 한도 때문에 잠시 뒤 다시 분석 (retryAt)
 *  - auto:      이번 시즌 랭크 모스트 3으로 자동 설정됨
 *  - saved:     단골 멤버에 저장된 라인을 사용
 *  - edited:    직접 고침
 */
export type LinesState = 'none' | 'analyzing' | 'waiting' | 'auto' | 'saved' | 'edited'

export interface RosterPlayer {
  id: string
  input: string
  name: string
  manual: boolean
  status: 'loading' | 'ready' | 'error'
  error?: string
  profile?: PlayerProfileDto
  featured?: FeaturedChampion
  recent?: InsightDto['recent']
  linesState: LinesState
  retryAt?: number
  tier: Tier
  lp?: number
  /** 기본 MMR (티어/LP 또는 직접 입력) */
  mmr: number
  mmrEdited: boolean
  /** 단골 멤버와 연결되면 그 id 와 내전 보정치 */
  memberId?: number
  inhouseDelta: number
  lines: PlayerLine[]
}

export const MAX_PLAYERS = 10
const CONCURRENCY = 2

let uid = 0
const nextId = () => `p${Date.now().toString(36)}${(uid++).toString(36)}`
const timers = new Map<string, ReturnType<typeof setTimeout>>()

export function effectiveMmr(p: RosterPlayer): number {
  return Math.max(1, p.mmr + (p.inhouseDelta || 0))
}

export function useRoster() {
  const scope = useState<string>('roster-scope', () => 'guest')
  const players = useState<RosterPlayer[]>('roster', () => [])
  const members = useState<MemberDto[]>('members', () => [])
  const api = useApi()
  const toast = useToast()

  const storageKey = () => `naejeonhaja:roster:${scope.value}`
  const groupCode = computed(() => (scope.value === 'guest' ? null : scope.value))

  // ------------------------------------------------------------ 저장/복원 (첫 렌더 이후에 호출)

  function restore(newScope: string) {
    if (!import.meta.client) return
    scope.value = newScope
    timers.forEach((t) => clearTimeout(t))
    timers.clear()
    try {
      const saved = JSON.parse(localStorage.getItem(storageKey()) || '[]') as RosterPlayer[]
      players.value = Array.isArray(saved)
        ? saved.slice(0, MAX_PLAYERS).map((p) => ({
            ...p,
            inhouseDelta: p.inhouseDelta ?? 0,
            linesState: p.linesState === 'analyzing' || p.linesState === 'waiting' ? 'none' : (p.linesState ?? 'none'),
            status: p.status === 'loading' ? 'error' : p.status,
            error: p.status === 'loading' ? '불러오다 멈췄어요. 다시 불러와 주세요.' : p.error
          }))
        : []
    } catch {
      players.value = []
    }
  }

  if (import.meta.client) {
    const persistWatcher = useState('roster-persist', () => false)
    if (!persistWatcher.value) {
      persistWatcher.value = true
      watch(players, (value) => {
        try {
          localStorage.setItem(storageKey(), JSON.stringify(value))
        } catch {
          /* 무시 */
        }
      }, { deep: true })
    }
  }

  const count = computed(() => players.value.length)
  const readyPlayers = computed(() => players.value.filter((p) => p.status === 'ready'))
  const isFull = computed(() => players.value.length >= MAX_PLAYERS)
  const busy = computed(() => players.value.some((p) => p.status === 'loading' || p.linesState === 'analyzing'))

  function find(id: string) {
    return players.value.find((p) => p.id === id)
  }

  function exists(name: string, exceptId?: string) {
    const key = riotIdKey(name)
    return players.value.some((p) => p.id !== exceptId && (riotIdKey(p.input) === key || riotIdKey(p.name) === key))
  }

  function sortLines(lines: PlayerLine[]) {
    return [...lines].sort((a, b) => LINES.indexOf(a.line) - LINES.indexOf(b.line))
  }

  // ------------------------------------------------------------ 멤버

  async function loadMembers() {
    if (!groupCode.value) {
      members.value = []
      return
    }
    try {
      members.value = await api.members(groupCode.value)
      // 이미 명단에 있는 사람의 내전 보정치를 최신으로
      for (const p of players.value) {
        const m = matchMember(p)
        if (m) linkMember(p, m, false)
      }
    } catch {
      /* 화면은 계속 사용 가능 */
    }
  }

  function matchMember(p: Pick<RosterPlayer, 'name' | 'input' | 'profile' | 'memberId'>): MemberDto | undefined {
    if (p.memberId) {
      const byId = members.value.find((m) => m.id === p.memberId)
      if (byId) return byId
    }
    const puuid = p.profile?.puuid
    if (puuid) {
      const byPuuid = members.value.find((m) => m.puuid === puuid)
      if (byPuuid) return byPuuid
    }
    const keys = [riotIdKey(p.name), riotIdKey(p.input)]
    return members.value.find((m) => keys.includes(riotIdKey(m.name)) || (m.riotId && keys.includes(riotIdKey(m.riotId))))
  }

  /** 단골 멤버 정보를 명단의 선수에게 반영. applySaved=true 면 저장된 라인/MMR 을 우선 사용 */
  function linkMember(p: RosterPlayer, m: MemberDto, applySaved: boolean) {
    p.memberId = m.id
    p.inhouseDelta = m.inhouseDelta
    if (!applySaved) return
    if (m.mmrOverride) {
      p.mmr = m.mmrOverride
      p.mmrEdited = true
    }
    if (m.lines.length && p.linesState !== 'edited') {
      p.lines = sortLines(m.lines)
      p.linesState = 'saved'
    }
  }

  // ------------------------------------------------------------ 소환사 검색

  /** 여러 Riot ID 를 한꺼번에 추가. 반환: 건너뛴 이유 */
  function addRiotIds(ids: string[]): string[] {
    const skipped: string[] = []
    const added: RosterPlayer[] = []
    for (const id of ids) {
      if (players.value.length + added.length >= MAX_PLAYERS) {
        skipped.push(`${id}: 10명이 다 찼어요`)
        continue
      }
      if (exists(id) || added.some((p) => riotIdKey(p.input) === riotIdKey(id))) {
        skipped.push(`${id}: 이미 명단에 있어요`)
        continue
      }
      added.push({
        id: nextId(),
        input: id,
        name: id,
        manual: false,
        status: 'loading',
        linesState: 'analyzing',
        tier: 'UNRANKED',
        mmr: tierScore('UNRANKED'),
        mmrEdited: false,
        inhouseDelta: 0,
        lines: []
      })
    }
    players.value.push(...added)
    runQueue(added.map((p) => p.id))
    return skipped
  }

  function addMember(m: MemberDto): string | null {
    if (isFull.value) return '10명이 다 찼어요.'
    if (players.value.some((p) => p.memberId === m.id) || exists(m.name) || (m.riotId && exists(m.riotId))) {
      return `${m.name}: 이미 명단에 있어요.`
    }
    const p: RosterPlayer = {
      id: nextId(),
      input: m.riotId ?? m.name,
      name: m.riotId ?? m.name,
      manual: !m.riotId,
      status: 'ready',
      linesState: m.lines.length ? 'saved' : 'none',
      tier: m.tier,
      lp: m.lp,
      mmr: m.baseMmr,
      mmrEdited: !!m.mmrOverride,
      memberId: m.id,
      inhouseDelta: m.inhouseDelta,
      lines: sortLines(m.lines)
    }
    players.value.push(p)
    // Riot 계정이 있는 멤버는 최신 티어·대표 챔피언을 뒤에서 갱신
    if (m.riotId) runQueue([p.id])
    return null
  }

  function addManual(name: string, tier: Tier): string | null {
    const trimmed = name.trim()
    if (!trimmed) return '이름을 입력해 주세요.'
    if (isFull.value) return '10명이 다 찼어요.'
    if (exists(trimmed)) return `${trimmed}: 이미 명단에 있어요.`
    const p: RosterPlayer = {
      id: nextId(),
      input: trimmed,
      name: trimmed,
      manual: true,
      status: 'ready',
      linesState: 'none',
      tier,
      mmr: tierScore(tier),
      mmrEdited: false,
      inhouseDelta: 0,
      lines: []
    }
    const m = matchMember(p)
    if (m) linkMember(p, m, true)
    players.value.push(p)
    return null
  }

  async function runQueue(ids: string[]) {
    const queue = [...ids]
    const worker = async () => {
      while (queue.length) {
        await loadInsight(queue.shift()!)
      }
    }
    await Promise.all(Array.from({ length: Math.min(CONCURRENCY, queue.length) }, worker))
  }

  async function loadInsight(id: string, refresh = false) {
    const p = find(id)
    if (!p || p.manual) return
    const wasReady = p.status === 'ready'
    if (!wasReady) p.status = 'loading'
    if (p.linesState === 'none' || p.linesState === 'waiting' || refresh) p.linesState = 'analyzing'
    p.error = undefined
    try {
      const data = await api.insight(p.input, refresh)
      const t = find(id)
      if (!t) return
      const dup = players.value.find((o) => o.id !== id && riotIdKey(o.name) === riotIdKey(data.profile.riotId))
      if (dup) {
        t.status = 'error'
        t.linesState = 'none'
        t.error = `${data.profile.riotId} 은(는) 이미 명단에 있어요`
        return
      }
      t.profile = data.profile
      t.featured = data.featured
      t.recent = data.recent ?? t.recent
      t.name = data.profile.riotId
      t.tier = data.profile.suggestedTier
      t.lp = data.profile.suggestedLp
      if (!t.mmrEdited) t.mmr = data.profile.suggestedMmr
      t.status = 'ready'

      const m = matchMember(t)
      if (m) linkMember(t, m, !wasReady)

      if (t.linesState === 'analyzing') {
        if (data.lines) {
          t.lines = sortLines(data.lines)
          t.linesState = data.lines.length ? 'auto' : 'none'
          t.retryAt = undefined
        } else if (data.linesPending) {
          scheduleRetry(t, data.retryAfter ?? 30)
        } else {
          t.linesState = 'none'
        }
      }
    } catch (error: any) {
      const t = find(id)
      if (!t) return
      if (error?.status === 429 && error?.retryAfter) {
        // 프로필조차 한도에 걸림 → 잠시 뒤 통째로 다시
        t.status = wasReady ? 'ready' : 'loading'
        scheduleRetry(t, error.retryAfter)
        return
      }
      if (!wasReady) {
        t.status = 'error'
        t.error = error?.message ?? '불러오지 못했어요'
      }
      if (t.linesState === 'analyzing') t.linesState = t.lines.length ? 'auto' : 'none'
    }
  }

  function scheduleRetry(p: RosterPlayer, seconds: number) {
    p.linesState = 'waiting'
    p.retryAt = Date.now() + seconds * 1000
    clearTimeout(timers.get(p.id))
    timers.set(p.id, setTimeout(() => {
      timers.delete(p.id)
      loadInsight(p.id)
    }, seconds * 1000 + 300))
  }

  function retry(id: string) {
    runQueue([id])
  }

  function reanalyze(id: string) {
    const p = find(id)
    if (!p) return
    p.linesState = 'analyzing'
    loadInsight(id, true)
  }

  function remove(id: string) {
    clearTimeout(timers.get(id))
    timers.delete(id)
    players.value = players.value.filter((p) => p.id !== id)
  }

  function clear() {
    timers.forEach((t) => clearTimeout(t))
    timers.clear()
    players.value = []
  }

  // ------------------------------------------------------------ 편집

  /** 라인 버튼: 없음 → 주 → 부 → 없음 */
  function cycleLine(id: string, line: Line) {
    const p = find(id)
    if (!p) return
    const current = p.lines.find((l) => l.line === line)
    if (!current) {
      p.lines = sortLines([...p.lines, { line, lineRole: 'MAINLINE' }])
    } else if (current.lineRole === 'MAINLINE') {
      current.lineRole = 'SUBLINE'
    } else {
      p.lines = p.lines.filter((l) => l.line !== line)
    }
    p.linesState = p.lines.length ? 'edited' : 'none'
  }

  function setLines(id: string, lines: { line: Line; lineRole: LineRole }[]) {
    const p = find(id)
    if (!p) return
    p.lines = sortLines(lines.map((l) => ({ ...l })))
    p.linesState = p.lines.length ? 'edited' : 'none'
  }

  function setMmr(id: string, value: number) {
    const p = find(id)
    if (!p) return
    if (!Number.isFinite(value) || value <= 0) {
      resetMmr(id)
      return
    }
    p.mmr = Math.min(10000, Math.round(value))
    p.mmrEdited = true
  }

  function resetMmr(id: string) {
    const p = find(id)
    if (!p) return
    p.mmr = p.profile ? p.profile.suggestedMmr : tierScoreWithLp(p.tier, p.lp)
    p.mmrEdited = false
  }

  function setTier(id: string, tier: Tier) {
    const p = find(id)
    if (!p) return
    p.tier = tier
    p.lp = undefined
    if (!p.mmrEdited) p.mmr = tierScore(tier)
  }

  // ------------------------------------------------------------ 단골 멤버로 저장

  async function saveAsMember(id: string, quiet = false): Promise<boolean> {
    const p = find(id)
    if (!p || !groupCode.value) return false
    try {
      const saved = await api.saveMember(groupCode.value, {
        name: p.profile?.riotId ?? p.name,
        riotId: p.profile?.riotId,
        puuid: p.profile?.puuid,
        profileIconId: p.profile?.profileIconId,
        tier: p.tier,
        lp: p.lp,
        lines: p.lines,
        mmrOverride: p.mmrEdited ? p.mmr : null
      })
      members.value = [...members.value.filter((m) => m.id !== saved.id), saved].sort((a, b) => a.name.localeCompare(b.name))
      const t = find(id)
      if (t) {
        t.memberId = saved.id
        t.inhouseDelta = saved.inhouseDelta
      }
      if (!quiet) toast.show(`${saved.name} 을(를) 단골 멤버로 저장했어요.`, 'ok')
      return true
    } catch (error: any) {
      if (!quiet) toast.show(error?.message ?? '저장하지 못했어요.', 'error')
      return false
    }
  }

  async function saveAllAsMembers(): Promise<number> {
    let n = 0
    for (const p of readyPlayers.value) {
      if (await saveAsMember(p.id)) n++
    }
    return n
  }

  // ------------------------------------------------------------ 팀 나누기 요청

  const coverage = computed(() =>
    LINES.map((line) => {
      let main = 0
      let sub = 0
      for (const p of readyPlayers.value) {
        if (p.lines.length === 0) { main++; continue }
        const l = p.lines.find((x) => x.line === line)
        if (l?.lineRole === 'MAINLINE') main++
        else if (l?.lineRole === 'SUBLINE') sub++
      }
      return { line, main, sub, total: main + sub }
    })
  )

  /**
   * 라인마다 2명이 안 되면 부 라인을 자동으로 채운다.
   * 후보: 최근 경기에서 그 라인을 가 본 사람(많이 간 순) → 그 외에는 '라인이 남는' 사람
   * (자기 주 라인을 할 수 있는 사람이 많을수록 우선). 바꾼 사람 수를 돌려준다.
   */
  function fillShortLines(): number {
    let changed = 0
    for (let guard = 0; guard < 10; guard++) {
      const short = coverage.value.find((c) => c.total < 2)
      if (!short) break
      const line = short.line
      const total = (l: Line) => coverage.value.find((c) => c.line === l)?.total ?? 0
      const candidates = readyPlayers.value
        .filter((p) => p.lines.length > 0 && !p.lines.some((l) => l.line === line))
        .map((p) => {
          const played = p.recent?.positions.find((x) => x.line === line)?.ratio ?? 0
          // 이 사람이 빠져도 다른 라인이 2명 아래로 떨어지지 않는지 (여유)
          const slack = Math.min(...p.lines.map((l) => total(l.line)))
          return { p, played, slack }
        })
        .sort((a, b) => b.played - a.played || b.slack - a.slack)
      const pick = candidates[0]
      if (!pick) break
      pick.p.lines = sortLines([...pick.p.lines, { line, lineRole: 'SUBLINE' }])
      pick.p.linesState = 'edited'
      changed++
    }
    return changed
  }

  function toRequest(): TeamRequestPlayer[] {
    return readyPlayers.value.map((p) => ({
      name: p.name,
      tier: p.tier,
      lines: p.lines,
      mmr: effectiveMmr(p)
    }))
  }

  function byName(name: string) {
    return players.value.find((p) => p.name === name)
  }

  return {
    scope,
    groupCode,
    players,
    members,
    count,
    readyPlayers,
    isFull,
    busy,
    coverage,
    restore,
    loadMembers,
    addRiotIds,
    addMember,
    addManual,
    retry,
    reanalyze,
    remove,
    clear,
    cycleLine,
    setLines,
    setMmr,
    resetMmr,
    setTier,
    saveAsMember,
    saveAllAsMembers,
    fillShortLines,
    toRequest,
    byName
  }
}
