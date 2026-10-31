import type { GroupSession } from '~/types/api'

const STORAGE_KEY = 'naejeonhaja:sessions:v1'

/**
 * 입장한 방들의 토큰 보관 (브라우저 localStorage).
 * 여러 방에 들어가 있을 수 있고, 최근에 들어간 방이 앞에 온다.
 */
export function useSession() {
  const sessions = useState<GroupSession[]>('sessions', () => [])
  const loaded = useState('sessions-loaded', () => false)

  function load() {
    if (!import.meta.client || loaded.value) return
    loaded.value = true
    try {
      const saved = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]')
      if (Array.isArray(saved)) sessions.value = saved.filter((s) => s && s.code && s.token)
    } catch {
      /* 무시 */
    }
  }

  function persist() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(sessions.value))
    } catch {
      /* 무시 */
    }
  }

  function save(session: GroupSession) {
    load()
    sessions.value = [session, ...sessions.value.filter((s) => s.code !== session.code)].slice(0, 8)
    persist()
  }

  function get(code: string | undefined | null): GroupSession | undefined {
    load()
    if (!code) return undefined
    return sessions.value.find((s) => s.code === code.toUpperCase())
  }

  function forget(code: string) {
    sessions.value = sessions.value.filter((s) => s.code !== code.toUpperCase())
    persist()
  }

  return { sessions, load, save, get, forget }
}
