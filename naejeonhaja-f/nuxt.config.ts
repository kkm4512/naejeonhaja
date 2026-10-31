// Nuxt 설정
// 브라우저는 항상 같은 출처의 /api 로 요청하고, Nuxt 서버(server/api/[...path].ts)가 백엔드로 넘긴다.
// 백엔드 주소는 런타임 환경변수 NUXT_API_BASE 로 바꿀 수 있다. (docker compose: http://backend:8080)
export default defineNuxtConfig({
  compatibilityDate: '2025-07-15',
  devtools: { enabled: false },
  ssr: true,
  runtimeConfig: {
    apiBase: 'http://localhost:8080'
  },
  css: ['~/assets/css/main.css'],
  app: {
    head: {
      htmlAttrs: { lang: 'ko' },
      title: '내전하자 | 롤 내전 팀 나누기',
      meta: [
        { name: 'description', content: '소환사 10명을 검색하면 티어와 라인을 읽어 와 가장 공정한 5대5 내전 팀을 짜 드립니다.' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { name: 'theme-color', content: '#0B0E1A' }
      ],
      link: [
        { rel: 'preconnect', href: 'https://fonts.googleapis.com' },
        { rel: 'preconnect', href: 'https://fonts.gstatic.com', crossorigin: '' },
        { rel: 'preconnect', href: 'https://cdn.jsdelivr.net', crossorigin: '' },
        { rel: 'preconnect', href: 'https://ddragon.leagueoflegends.com' },
        {
          rel: 'stylesheet',
          href: 'https://fonts.googleapis.com/css2?family=Do+Hyeon&family=Barlow+Condensed:ital,wght@0,600;0,800;1,800&display=swap'
        },
        {
          rel: 'stylesheet',
          href: 'https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable-dynamic-subset.min.css'
        }
      ]
    },
    pageTransition: { name: 'page', mode: 'out-in' }
  },
  typescript: {
    strict: true
  }
})
