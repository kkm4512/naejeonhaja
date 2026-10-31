// /api/** 요청을 백엔드(Spring Boot)로 그대로 전달하는 프록시.
// - 브라우저 입장에서는 같은 출처라 CORS 설정이 필요 없다.
// - /api/health 는 백엔드의 /health 로 보낸다.
// - Riot ID 의 '#'(%23) 이 풀리지 않도록 디코딩 전의 원본 URL 을 그대로 넘긴다.
export default defineEventHandler(async (event) => {
  const { apiBase } = useRuntimeConfig(event)
  const base = String(apiBase).replace(/\/$/, '')
  const rawUrl = (event.node.req as { originalUrl?: string }).originalUrl ?? event.node.req.url ?? event.path
  const path = rawUrl.startsWith('/api/health') ? rawUrl.replace('/api/health', '/health') : rawUrl

  // 서버 간 호출이므로 브라우저의 Origin/Referer 를 넘기지 않는다.
  // (넘기면 백엔드 CORS 검사에 걸려 localhost:3000 이외의 주소로 접속했을 때 POST 가 403 이 된다)
  delete event.node.req.headers.origin
  delete event.node.req.headers.referer

  try {
    return await proxyRequest(event, base + path)
  } catch {
    setResponseStatus(event, 502)
    return {
      code: 502,
      message: '백엔드 서버에 연결할 수 없습니다. 서버가 켜져 있는지 확인해주세요.'
    }
  }
})
