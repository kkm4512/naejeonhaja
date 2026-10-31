import type {
  ApiEnvelope,
  GroupSession,
  GroupSummary,
  HealthDto,
  InsightDto,
  MatchHistoryDto,
  MatchOut,
  MatchPage,
  MatchPlayerIn,
  MemberDto,
  MemberUpsert,
  StandingRow,
  TeamRequestPlayer,
  TeamResult,
  TeamSide
} from '~/types/api'

export class ApiError extends Error {
  status: number
  retryAfter?: number
  constructor(status: number, message: string, retryAfter?: number) {
    super(message)
    this.status = status
    this.retryAfter = retryAfter
  }
}

type FetchOptions = NonNullable<Parameters<typeof $fetch>[1]>

async function request<T>(url: string, options: FetchOptions = {}): Promise<T> {
  try {
    const res = await $fetch<ApiEnvelope<T>>(url, options as any)
    if (res.data === undefined || res.data === null) {
      // 성공인데 data 가 없는 응답 (삭제 등)
      if (res.code && res.code < 300) return undefined as T
      throw new ApiError(res.code ?? 500, res.message || '응답에 데이터가 없습니다.')
    }
    return res.data
  } catch (error: any) {
    if (error instanceof ApiError) throw error
    const status = error?.response?.status ?? error?.statusCode ?? 0
    const body = error?.data as ApiEnvelope<unknown> | undefined
    const message = body?.message
      ?? (status === 0 ? '서버에 연결할 수 없습니다. 네트워크를 확인해주세요.' : `요청에 실패했습니다. (${status})`)
    throw new ApiError(status, message, body?.retryAfter)
  }
}

const enc = (v: string) => encodeURIComponent(v.trim())

/** 브라우저 → 같은 출처 /api → Nuxt 서버 프록시 → 백엔드 */
export function useApi() {
  const session = useSession()

  /** 방 API: 입장 토큰을 붙이고, 토큰이 무효면 입장 화면으로 */
  async function group<T>(code: string, path: string, options: FetchOptions = {}): Promise<T> {
    const s = session.get(code)
    if (!s) {
      await navigateTo(`/?join=${code}`)
      throw new ApiError(401, '방에 다시 입장해주세요.')
    }
    try {
      return await request<T>(`/api/v1/groups/${code}${path}`, {
        ...options,
        headers: { ...(options.headers as Record<string, string> | undefined), 'X-Group-Token': s.token }
      })
    } catch (error) {
      if (error instanceof ApiError && error.status === 401) {
        session.forget(code)
        await navigateTo(`/?join=${code}`)
      }
      throw error
    }
  }

  return {
    health: () => request<HealthDto>('/api/health'),

    // Riot
    insight: (riotId: string, refresh = false) =>
      request<InsightDto>(`/api/v1/game/lol/riot/player/${enc(riotId)}/insight`, { query: refresh ? { refresh: true } : {} }),
    matches: (riotId: string, count = 10) =>
      request<MatchHistoryDto>(`/api/v1/game/lol/riot/player/${enc(riotId)}/matches`, { query: { count } }),

    // 팀 나누기
    teams: (mode: 'rift' | 'abyss', players: TeamRequestPlayer[]) =>
      request<TeamResult>(mode === 'rift' ? '/api/v1/lol/rift/teams' : '/api/v1/lol/abyss/teams', {
        method: 'POST',
        body: { lolPlayers: players }
      }),

    // 방
    createGroup: (name: string, password: string) =>
      request<GroupSession>('/api/v1/groups', { method: 'POST', body: { name, password } }),
    joinGroup: (code: string, password: string) =>
      request<GroupSession>(`/api/v1/groups/${enc(code.toUpperCase())}/join`, { method: 'POST', body: { password } }),
    groupSummary: (code: string) => group<GroupSummary>(code, ''),

    // 멤버
    members: (code: string) => group<MemberDto[]>(code, '/members'),
    saveMember: (code: string, body: MemberUpsert) => group<MemberDto>(code, '/members', { method: 'POST', body }),
    updateMember: (code: string, id: number, body: MemberUpsert) =>
      group<MemberDto>(code, `/members/${id}`, { method: 'PUT', body }),
    deleteMember: (code: string, id: number) => group<void>(code, `/members/${id}`, { method: 'DELETE' }),

    // 기록
    saveMatch: (code: string, body: { mode: 'RIFT' | 'ABYSS'; blue: MatchPlayerIn[]; red: MatchPlayerIn[]; memo?: string }) =>
      group<MatchOut>(code, '/matches', { method: 'POST', body }),
    listMatches: (code: string, page = 0, size = 20) => group<MatchPage>(code, '/matches', { query: { page, size } }),
    decideMatch: (code: string, id: number, winner: TeamSide | null) =>
      group<MatchOut>(code, `/matches/${id}/result`, { method: 'PUT', body: { winner } }),
    deleteMatch: (code: string, id: number) => group<void>(code, `/matches/${id}`, { method: 'DELETE' }),
    standings: (code: string) => group<StandingRow[]>(code, '/standings'),
    publicMatch: (publicId: string) => request<MatchOut>(`/api/v1/public/matches/${enc(publicId)}`)
  }
}
