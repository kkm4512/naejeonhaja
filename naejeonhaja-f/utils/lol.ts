import type { Line, LineRole, Tier } from '~/types/api'

export const LINES: Line[] = ['TOP', 'JUNGLE', 'MID', 'AD', 'SUPPORT']

export const LINE_LABEL: Record<Line, string> = {
  TOP: '탑',
  JUNGLE: '정글',
  MID: '미드',
  AD: '원딜',
  SUPPORT: '서폿'
}

export const ROLE_LABEL: Record<LineRole, string> = {
  MAINLINE: '주',
  SUBLINE: '부'
}

const TIER_FAMILY_LABEL: Record<string, string> = {
  UNRANKED: '언랭',
  IRON: '아이언',
  BRONZE: '브론즈',
  SILVER: '실버',
  GOLD: '골드',
  PLATINUM: '플래티넘',
  EMERALD: '에메랄드',
  DIAMOND: '다이아',
  MASTER: '마스터',
  GRANDMASTER: '그랜드마스터',
  CHALLENGER: '챌린저'
}

const DIVISION_NUMBER: Record<string, string> = { IV: '4', III: '3', II: '2', I: '1' }

// 백엔드 LolTier 와 같은 순서·점수 (UNRANKED 650, 아이언4 50 부터 세부 티어당 +50)
const FAMILIES = ['IRON', 'BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'EMERALD', 'DIAMOND']
const DIVISIONS = ['IV', 'III', 'II', 'I']

export const TIERS: Tier[] = [
  'UNRANKED',
  ...FAMILIES.flatMap((f) => DIVISIONS.map((d) => `${f}_${d}`)),
  'MASTER_I',
  'GRANDMASTER_I',
  'CHALLENGER_I'
]

export function tierScore(tier: Tier): number {
  if (tier === 'UNRANKED') return 650
  if (tier === 'MASTER_I') return 1600
  if (tier === 'GRANDMASTER_I') return 2100
  if (tier === 'CHALLENGER_I') return 2600
  const idx = TIERS.indexOf(tier)
  return idx > 0 ? idx * 50 : 650
}

/** 백엔드 LolTier.scoreWithLp 와 같은 계산 */
export function tierScoreWithLp(tier: Tier, lp?: number | null): number {
  const base = tierScore(tier)
  if (!lp || lp <= 0 || tier === 'UNRANKED') return base
  if (tier === 'MASTER_I' || tier === 'GRANDMASTER_I') return base + Math.min(Math.floor(lp / 2), 450)
  if (tier === 'CHALLENGER_I') return base + Math.min(Math.floor(lp / 2), 1000)
  return base + Math.floor((Math.min(lp, 99) * 50) / 100)
}

export function tierFamily(tier: Tier): string {
  return (tier || 'UNRANKED').split('_')[0] ?? 'UNRANKED'
}

/** GOLD_II → 골드 2, MASTER_I → 마스터 */
export function tierLabel(tier: Tier): string {
  const [family, division] = (tier || 'UNRANKED').split('_')
  const name = TIER_FAMILY_LABEL[family ?? ''] ?? family ?? '언랭'
  if (!division || ['MASTER', 'GRANDMASTER', 'CHALLENGER'].includes(family ?? '')) return name
  return `${name} ${DIVISION_NUMBER[division] ?? division}`
}

/** 티어 계열별 색 (배지 테두리/글자) */
export function tierColor(tier: Tier): string {
  const map: Record<string, string> = {
    UNRANKED: '#8DA2B4',
    IRON: '#9A8F8A',
    BRONZE: '#C08458',
    SILVER: '#B5C3CF',
    GOLD: '#E2B655',
    PLATINUM: '#4FC1B4',
    EMERALD: '#3FBF7F',
    DIAMOND: '#7FA7FF',
    MASTER: '#C77DFF',
    GRANDMASTER: '#F0606A',
    CHALLENGER: '#F5D17A'
  }
  return map[tierFamily(tier)] ?? '#8DA2B4'
}

/**
 * 붙여넣은 텍스트에서 Riot ID 목록을 뽑는다.
 * - 한 줄에 하나, 또는 쉼표로 구분
 * - 롤 로비 채팅 "이름 #KR1님이 로비에 참가하셨습니다." 형태도 인식
 */
export function parseRiotIds(text: string): string[] {
  const seen = new Set<string>()
  const result: string[] = []
  for (const raw of text.split(/[\n,]+/)) {
    let value = raw.trim()
    value = value.replace(/\s*님이\s*로비에\s*참가(하셨|했)습니다\.?\s*$/, '')
    value = value.replace(/\s*(joined the lobby|님이 방에 참가했습니다)\.?\s*$/i, '')
    value = value.replace(/\s*#\s*/, '#').trim()
    if (!value) continue
    const key = value.toLowerCase().replace(/\s+/g, '')
    if (seen.has(key)) continue
    seen.add(key)
    result.push(value)
  }
  return result
}

/** 영어 짧은 티어 표기 (카드 배지용): GOLD_II → G2, MASTER_I → M */
export function tierShort(tier: Tier): string {
  const [family, division] = (tier || 'UNRANKED').split('_')
  const map: Record<string, string> = {
    UNRANKED: '—', IRON: 'I', BRONZE: 'B', SILVER: 'S', GOLD: 'G', PLATINUM: 'P', EMERALD: 'E', DIAMOND: 'D',
    MASTER: 'M', GRANDMASTER: 'GM', CHALLENGER: 'C'
  }
  const f = map[family ?? ''] ?? '?'
  if (!division || ['MASTER', 'GRANDMASTER', 'CHALLENGER', 'UNRANKED'].includes(family ?? '')) return f
  return f + (DIVISION_NUMBER[division] ?? '')
}

export function riotIdKey(riotId: string): string {
  return riotId.toLowerCase().replace(/\s+/g, '')
}

export function formatNumber(n: number): string {
  return n.toLocaleString('ko-KR')
}

export function formatKda(kda: number, deaths: number): string {
  return deaths === 0 ? 'Perfect' : kda.toFixed(2)
}

export function timeAgo(timestamp: number): string {
  const diff = Date.now() - timestamp
  const min = Math.floor(diff / 60000)
  if (min < 60) return `${Math.max(min, 1)}분 전`
  const hour = Math.floor(min / 60)
  if (hour < 24) return `${hour}시간 전`
  const day = Math.floor(hour / 24)
  if (day < 30) return `${day}일 전`
  return `${Math.floor(day / 30)}달 전`
}

export function formatDuration(sec: number): string {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${m}:${String(s).padStart(2, '0')}`
}
