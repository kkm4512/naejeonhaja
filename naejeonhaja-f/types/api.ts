// 백엔드 API 타입 (naejeonhaja-b 의 DTO 와 1:1)

export type Line = 'TOP' | 'JUNGLE' | 'MID' | 'AD' | 'SUPPORT'
export type LineRole = 'MAINLINE' | 'SUBLINE'
export type Tier = string // LolTier enum 값 (예: GOLD_II, MASTER_I, UNRANKED)
export type Balance = 'PERFECT' | 'EXCELLENT' | 'GOOD' | 'FAIR' | 'POOR'

export interface ApiEnvelope<T> {
  code: number
  message: string
  data?: T
  retryAfter?: number
}

export interface HealthDto {
  status: string
  riotApiKeyConfigured: boolean
  ddragonVersion: string
}

export interface RankDto {
  queueType: string
  tier: string
  rank: string
  tierKey: Tier
  leaguePoints: number
  wins: number
  losses: number
  winRate: number
  hotStreak: boolean
}

export interface MasteryDto {
  championId: number
  championKey?: string
  championName: string
  imageUrl?: string
  splashUrl?: string
  loadingUrl?: string
  level: number
  points: number
}

export interface PlayerProfileDto {
  puuid: string
  gameName: string
  tagLine: string
  riotId: string
  summonerLevel: number
  profileIconId: number
  profileIconUrl: string
  soloRank?: RankDto
  flexRank?: RankDto
  topChampions?: MasteryDto[]
  suggestedTier: Tier
  suggestedLp?: number
  suggestedMmr: number
  mmrSource: 'SOLO' | 'FLEX' | 'UNRANKED'
  mmrNote: string
}

export interface MatchDto {
  matchId: string
  queueId: number
  queueName: string
  gameStartTimestamp: number
  gameDurationSec: number
  win: boolean
  remake: boolean
  championId: number
  championName: string
  championImageUrl: string | null
  champLevel: number
  position: Line | null
  kills: number
  deaths: number
  assists: number
  kda: number
  killParticipation: number
  cs: number
  csPerMin: number
  goldEarned: number
  damageToChampions: number
  visionScore: number
  multiKill: string | null
  items: (string | null)[]
  spells: (string | null)[]
  runes: (string | null)[]
}

export interface MatchSummaryDto {
  games: number
  wins: number
  losses: number
  winRate: number
  avgKills: number
  avgDeaths: number
  avgAssists: number
  kda: number
  avgCsPerMin: number
  avgKillParticipation: number
  positions: { line: Line; label: string; games: number; ratio: number }[]
  recommendedLines: { line: Line; lineRole: LineRole }[]
  mostChampions: { championId: number; championName: string; imageUrl: string | null; games: number; wins: number; winRate: number; kda: number }[]
}

export interface MatchHistoryDto {
  puuid: string
  riotId: string
  summary: MatchSummaryDto
  matches: MatchDto[]
}

export interface PlayerLine {
  line: Line
  lineRole: LineRole
}

export interface TeamRequestPlayer {
  name: string
  tier: Tier
  lines: PlayerLine[]
  mmr: number
}

export interface AssignedPlayer {
  name: string
  tier: Tier
  line?: Line
  lineRole?: LineRole
  baseMmr: number
  mmr: number
  mmrReduced: boolean
  /** 고르지 않은 라인에 배정됨 */
  offRole?: boolean
}

export interface TeamResult {
  balance: Balance
  balanceDescription: string
  teamATotalMmr: number
  teamBTotalMmr: number
  mmrDiff: number
  subLineCount: number
  offRoleCount: number
  teamA: AssignedPlayer[]
  teamB: AssignedPlayer[]
  alternatives?: TeamResult[]
}

// ---------------------------------------------------------------- 소환사 검색 (insight)

export interface ChampionStat {
  championId: number
  championName: string
  imageUrl: string | null
  games: number
  wins: number
  winRate: number
  kda: number
}

export interface FeaturedChampion {
  championId: number
  championKey: string
  championName: string
  imageUrl: string
  splashUrl: string
  loadingUrl: string
}

export interface MostChampion {
  championId: number
  championName: string
  imageUrl: string | null
  games: number
  wins: number
  winRate: number
  kda: number
  line: Line
  lineLabel: string
  main: boolean
}

export interface RestLine {
  line: Line
  lineLabel: string
  games: number
  sub: boolean
  champions: { championId: number; championName: string; imageUrl: string | null; games: number; wins: number }[]
}

export interface InsightDto {
  profile: PlayerProfileDto
  lines?: PlayerLine[] | null
  linesPending: boolean
  retryAfter?: number
  featured?: FeaturedChampion
  recent?: {
    games: number
    wins: number
    winRate: number
    kda: number
    positions: { line: Line; label: string; games: number; ratio: number }[]
    mostChampions: ChampionStat[]
    /** 표본: SOLO 이번 시즌 솔로랭크 / RANKED 자유랭크 포함 / NORMAL 랭크가 없어 일반 게임 */
    basis?: 'SOLO' | 'RANKED' | 'NORMAL'
    /** 모스트 3 과 각 챔피언으로 간 포지션 (main=false 면 1판뿐이라 주 라인에서 뺌) */
    most3?: MostChampion[]
    /** 모스트 3 밖 챔피언으로 간 포지션별 챔피언 (sub=true 면 부 라인) */
    restLines?: RestLine[]
  }
  sampleSize: number
  cached: boolean
  analyzedAt?: string
}

// ---------------------------------------------------------------- 방 / 멤버 / 기록

export interface GroupSession {
  code: string
  name: string
  token: string
}

export interface GroupSummary {
  code: string
  name: string
  memberCount: number
  matchCount: number
}

export interface MemberDto {
  id: number
  name: string
  riotId?: string
  puuid?: string
  profileIconId?: number
  profileIconUrl?: string
  tier: Tier
  lp?: number
  lines: PlayerLine[]
  mmrOverride?: number
  baseMmr: number
  inhouseDelta: number
  effectiveMmr: number
  wins: number
  losses: number
  winRate: number
  updatedAt: string
}

export interface MemberUpsert {
  name: string
  riotId?: string
  puuid?: string
  profileIconId?: number
  tier: Tier
  lp?: number
  lines: PlayerLine[]
  mmrOverride?: number | null
}

export type TeamSide = 'BLUE' | 'RED'

export interface MatchPlayerIn {
  memberId?: number
  name: string
  tier: Tier
  line?: Line
  lineRole?: LineRole
  baseMmr: number
  mmr: number
}

export interface MatchPlayerOut {
  memberId?: number
  name: string
  tier: Tier
  line?: Line
  lineRole?: LineRole
  baseMmr: number
  mmr: number
  ratingDelta: number
  profileIconUrl?: string
}

export interface MatchOut {
  id: number
  publicId: string
  groupName: string
  mode: 'RIFT' | 'ABYSS'
  blueTotal: number
  redTotal: number
  mmrDiff: number
  balance: Balance
  balanceDescription: string
  winner?: TeamSide | null
  memo?: string | null
  createdAt: string
  decidedAt?: string | null
  blue: MatchPlayerOut[]
  red: MatchPlayerOut[]
}

export interface MatchPage {
  items: MatchOut[]
  page: number
  size: number
  total: number
  hasNext: boolean
}

export interface StandingRow {
  memberId: number
  name: string
  profileIconUrl?: string
  tier: Tier
  wins: number
  losses: number
  winRate: number
  inhouseDelta: number
  effectiveMmr: number
  form: ('W' | 'L')[]
  streak: number
}
