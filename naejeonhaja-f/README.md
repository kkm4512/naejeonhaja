# 내전하자 프론트엔드 (naejeonhaja-f)

Nuxt 4 로 만든 드래프트(픽창) 스타일 화면입니다. 전체 실행 방법은 **상위 폴더의 [README.md](../README.md)** 를 보세요.

```bash
npm install
npm run dev        # http://localhost:3000
npm run build      # .output/ 생성
npm run start      # 빌드 결과 실행
npm run typecheck
```

브라우저는 같은 출처의 `/api/**` 로만 요청하고 `server/api/[...path].ts` 가 백엔드(`NUXT_API_BASE`, 기본 http://localhost:8080)로 넘깁니다.

## 구조

| 경로 | 역할 |
|---|---|
| `pages/index.vue` | 첫 화면: 방 만들기/들어가기, 최근 방 |
| `pages/play.vue` | 방 없이 체험 (저장 안 됨) |
| `pages/g/[code]/index.vue` | 드래프트 (방) |
| `pages/g/[code]/records.vue` | 순위표·내전 기록 |
| `pages/m/[id].vue` | 공유용 읽기 전용 대진표 |
| `components/draft/` | `DraftPage`(배치), `SearchStrip`(검색), `Stage`·`LaneSlot`(BLUE/RED 슬롯), `CenterConsole`(모드·라인 현황·팀 나누기·결과·기록), `Bench`·`BenchCard`(대기열), `PlayerSheet`(상세), `MemberDrawer`(단골) |
| `components/records/` | `MatchCard`, `Standings` |
| `components/ui/` | `TopBar`, `LaneIcon`(직접 그린 라인 아이콘), `TierTag`, `ToastHost` |
| `composables/useRoster.ts` | 대기열 상태, 소환사 검색 대기열(동시 2명, 호출 한도 대기 후 자동 재시도), 단골 연결, 브라우저 저장 |
| `composables/useDraft.ts` | 팀 나누기 결과·차선·진영·기록·승패 |
| `composables/useSession.ts` | 입장한 방 토큰 (localStorage) |
| `composables/useApi.ts` | API 호출, 방 토큰 헤더, 401 이면 입장 화면으로 |
| `utils/lol.ts` | 라인/티어 이름·점수, Riot ID 붙여넣기 해석 |
