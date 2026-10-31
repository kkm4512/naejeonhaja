import type { AssignedPlayer, MatchOut, MatchPlayerIn, TeamResult, TeamSide } from '~/types/api'
import { LINE_LABEL } from '~/utils/lol'

/**
 * 팀 나누기 결과와 그 뒤의 흐름 (차선 조합 고르기 → 진영 바꾸기 → 저장 → 승패 기록).
 */
export function useDraft() {
  const mode = useState<'rift' | 'abyss'>('draft-mode', () => 'rift')
  const result = useState<TeamResult | null>('draft-result', () => null)
  const resultMode = useState<'rift' | 'abyss'>('draft-result-mode', () => 'rift')
  const selected = useState('draft-selected', () => 0)
  const swapped = useState('draft-swapped', () => false)
  const saved = useState<MatchOut | null>('draft-saved', () => null)
  const revealKey = useState('draft-reveal', () => 0)
  const loading = useState('draft-loading', () => false)
  const error = useState('draft-error', () => '')

  const roster = useRoster()
  const api = useApi()
  const toast = useToast()

  const options = computed(() => (result.value ? [result.value, ...(result.value.alternatives ?? [])] : []))
  const current = computed(() => options.value[selected.value] ?? result.value)
  const blue = computed<AssignedPlayer[]>(() => (current.value ? (swapped.value ? current.value.teamB : current.value.teamA) : []))
  const red = computed<AssignedPlayer[]>(() => (current.value ? (swapped.value ? current.value.teamA : current.value.teamB) : []))
  const blueTotal = computed(() => (current.value ? (swapped.value ? current.value.teamBTotalMmr : current.value.teamATotalMmr) : 0))
  const redTotal = computed(() => (current.value ? (swapped.value ? current.value.teamATotalMmr : current.value.teamBTotalMmr) : 0))

  function reset() {
    result.value = null
    saved.value = null
    selected.value = 0
    swapped.value = false
    error.value = ''
  }

  async function split() {
    loading.value = true
    error.value = ''
    saved.value = null
    try {
      const r = await api.teams(mode.value, roster.toRequest())
      result.value = r
      resultMode.value = mode.value
      selected.value = 0
      swapped.value = false
      revealKey.value++
    } catch (e: any) {
      result.value = null
      error.value = e?.message ?? '팀을 나누지 못했어요.'
    } finally {
      loading.value = false
    }
  }

  function choose(i: number) {
    if (saved.value) return
    selected.value = i
    revealKey.value++
  }

  function swap() {
    if (saved.value) return
    swapped.value = !swapped.value
  }

  function toIn(p: AssignedPlayer): MatchPlayerIn {
    const r = roster.byName(p.name)
    return {
      memberId: r?.memberId,
      name: p.name,
      tier: p.tier,
      line: p.line,
      lineRole: p.lineRole,
      baseMmr: p.baseMmr,
      mmr: p.mmr
    }
  }

  async function saveMatch() {
    const code = roster.groupCode.value
    if (!code || !current.value) return
    // 아직 단골 멤버가 아닌 사람은 먼저 저장해 둔다 → 승패를 기록하면 순위표에 바로 반영
    for (const p of roster.readyPlayers.value.filter((x) => !x.memberId)) {
      await roster.saveAsMember(p.id, true)
    }
    try {
      saved.value = await api.saveMatch(code, {
        mode: resultMode.value === 'rift' ? 'RIFT' : 'ABYSS',
        blue: blue.value.map(toIn),
        red: red.value.map(toIn)
      })
      toast.show('내전을 기록에 저장했어요. 끝나면 이긴 팀을 눌러 주세요.', 'ok', 3500)
    } catch (e: any) {
      toast.show(e?.message ?? '저장하지 못했어요.', 'error')
    }
  }

  async function decide(winner: TeamSide | null) {
    const code = roster.groupCode.value
    if (!code || !saved.value) return
    try {
      saved.value = await api.decideMatch(code, saved.value.id, winner)
      await roster.loadMembers()
      toast.show(winner ? `${winner === 'BLUE' ? '블루' : '레드'}팀 승리로 기록했어요. 내전 MMR 이 반영됐어요.` : '승패 기록을 취소했어요.', 'ok', 3500)
    } catch (e: any) {
      toast.show(e?.message ?? '기록하지 못했어요.', 'error')
    }
  }

  function shareText(): string {
    const line = (p: AssignedPlayer) =>
      `${resultMode.value === 'rift' && p.line ? `${LINE_LABEL[p.line]}${p.offRole ? '(라인외)' : p.lineRole === 'SUBLINE' ? '(부)' : ''} ` : ''}${p.name}`
    const url = saved.value && import.meta.client ? `\n${location.origin}/m/${saved.value.publicId}` : ''
    return [
      `🔵 블루팀 (${blueTotal.value})`,
      ...blue.value.map(line),
      '',
      `🔴 레드팀 (${redTotal.value})`,
      ...red.value.map(line),
      '',
      `차이 ${current.value?.mmrDiff ?? 0}, ${current.value?.balanceDescription ?? ''}${url}`
    ].join('\n')
  }

  return {
    mode,
    result,
    resultMode,
    selected,
    swapped,
    saved,
    revealKey,
    loading,
    error,
    options,
    current,
    blue,
    red,
    blueTotal,
    redTotal,
    reset,
    split,
    choose,
    swap,
    saveMatch,
    decide,
    shareText
  }
}
