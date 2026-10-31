# 내전하자 (naejeonhaja)

롤 내전용 팀 나누기 서비스입니다. 소환사 10명의 Riot ID를 붙여 넣으면 티어·LP와 **최근 경기 포지션으로 주/부 라인까지 자동으로** 채우고,
**각 팀에 5개 라인이 한 명씩 배정되면서 두 팀 점수 차이가 가장 작은 조합**을 찾아 픽창처럼 보여 줍니다.
방(그룹)을 만들면 단골 멤버, 내전 기록, 승패에 따른 **내전 MMR**과 순위표가 쌓입니다.

```
naejeonhaja/
├── docker-compose.yml   ← DB + 백엔드 + 프론트엔드 한 번에 실행
├── .env.example         ← Riot API 키 등 설정 견본
├── naejeonhaja-b/       ← Spring Boot 3.3 API 서버 (Java 17, PostgreSQL, Flyway)
└── naejeonhaja-f/       ← Nuxt 4 프론트엔드
```

## 빠른 시작 (Docker)

필요한 것: Docker Desktop, Riot API 키 ([developer.riotgames.com](https://developer.riotgames.com))

```bash
copy .env.example .env        # macOS/Linux: cp .env.example .env
# .env 를 열어 RIOT_API_KEY=RGAPI-... 입력
docker compose up -d --build
```

- 화면: http://localhost:3000
- API 문서(Swagger): http://localhost:8080/docs (이 컴퓨터에서만 열림)
- 중지: `docker compose down` (데이터 유지) / 데이터까지 삭제: `docker compose down -v`
- 로그: `docker compose logs -f backend`

개발용 Riot 키는 **24시간마다 만료**됩니다. 화면에 “Riot API 키가 유효하지 않거나 만료되었습니다”가 뜨면 새 키로 `.env` 를 바꾸고 `docker compose up -d` 를 다시 실행하세요.

```
브라우저 ──▶ frontend:3000 (Nuxt) ──/api/**──▶ backend:8080 (Spring Boot) ──▶ db:5432 (PostgreSQL)
                                                     ├─▶ Riot API (asia / kr)
                                                     └─▶ Data Dragon (챔피언 일러스트·아이콘)
```

## 화면과 기능

| 화면 | 내용 |
|---|---|
| 첫 화면 `/` | 방 만들기(이름+비밀번호 → 6자리 입장 코드) / 방 들어가기 / 방 없이 체험(`/play`, 저장 안 됨) |
| 드래프트 `/g/{코드}` | 위: 소환사 검색 · 가운데: BLUE / RED 라인 슬롯과 팀 나누기 · 아래: 대기열 카드 10장 |
| 기록·순위 `/g/{코드}/records` | 내전 순위표(승률·최근 5경기·연승·내전 보정) / 내전 기록(승패 기록·취소·삭제·공유 링크) |
| 공유 `/m/{id}` | 방 밖 사람도 볼 수 있는 읽기 전용 대진표 |

- **소환사 검색**: 한 줄에 한 명, 로비 채팅 “○○님이 로비에 참가하셨습니다.” 붙여넣기, 단골 멤버 이름 자동완성
- **자동 라인 (모스트 3)**: 이번 시즌 솔로랭크 최근 20판에서 가장 많이 한 챔피언 3개 → 그 챔피언들로 간 포지션 = **주 라인** (모스트 2·3 이 1판뿐이면 제외), 모스트 3 이 아닌 챔피언으로 2판 이상 간 포지션 = **부 라인**. 솔로랭크가 5판 미만이면 자유랭크 포함, 랭크가 없으면 최근 일반 게임. 결과는 DB 에 24시간 저장되어 같은 사람은 바로 나옴
- **대기열 카드**: 대표 챔피언 일러스트, 티어, 점수(내전 보정 포함), 라인 버튼(주 → 부 → 해제), 카드를 누르면 상세(점수 직접 정하기, 최근 경기, 단골 저장)
- **부족한 라인 채우기**: 어떤 라인이 1명뿐이면 최근에 그 라인을 가 본 사람에게 부 라인을 자동 추가
- **팀 공개 연출**: 라인별로 양 진영에 순서대로 등장, VS, 점수 차이 카운트, 차선 조합 4개, 진영 바꾸기, 결과 복사
- **기록 → 승패 → 내전 MMR**: ‘이 팀으로 기록 시작’ → 경기 후 ‘블루 승/레드 승’. 대기열 사람은 자동으로 단골 멤버가 되고 내전 보정치가 쌓임

## 밸런싱 규칙

1. **기본 점수** = 티어 점수 + LP 보정 + 시즌 승률 보정 (화면에서 직접 정하면 그 값)
   - 티어: 아이언 4 = 50부터 세부 티어마다 +50 (골드 1 = 800, 다이아 1 = 1400), 마스터 1600, 그마 2100, 챌린저 2600
   - LP: 다이아 이하 LP 0~99 → +0~49, 마스터 이상 LP ÷ 2 (상한)
   - 승률: 솔랭 20판 이상일 때 50% 기준 1%p 당 ±3점 (최대 ±50). 솔랭이 없으면 자랭 −50, 둘 다 없으면 650
2. **최종 점수** = 기본 점수 + 내전 보정치 (단골 멤버일 때)
3. **탐색**: 5:5 로 나누는 126가지 × 각 팀 라인 배치 120가지를 전부 확인. 부 라인 배정은 −100
4. **라인을 지키는 조합이 하나도 없으면**: 고르지 않은 라인에 배정되는 사람을 최소로 해서라도 팀을 만들고 그 사람은 −250 (화면에 ‘라인 외’ 표시)
5. **정렬**: 라인 외 인원 → 점수 차이 → 부 라인 인원 순. 최적 + 차선 4개

**내전 MMR (Elo 방식)**: 두 팀 평균 점수 차로 기대 승률을 구하고(평균 200점 차 ≈ 91%), 이긴 팀은 `40 × (1 − 기대 승률)` 만큼 오르고 진 팀은 그만큼 내려갑니다. 팽팽한 경기면 ±20, 불리한 팀이 이기면 더 많이 오릅니다. 승패를 바꾸거나 취소·삭제하면 정확히 되돌립니다.

| 두 팀 점수 차이 | 등급 |
|---|---|
| 0 ~ 100 | 완벽한 밸런스 |
| 101 ~ 300 | 우수한 밸런스 |
| 301 ~ 600 | 좋은 밸런스 |
| 601 ~ 1000 | 보통 밸런스 |
| 1001 이상 | 불균형한 밸런스 |

## Riot API 호출 한도

개발용 키는 **1초 20회, 2분 100회**입니다. 서버가 이 한도를 미리 지키면서(기본 18/1초, 95/2분) 호출하고,
자리가 없으면 화면 카드에 “호출 한도 대기 · N초 뒤 분석”이 뜬 뒤 자동으로 이어서 불러옵니다.

- 소환사 1명 검색 ≈ 24회 (계정·소환사·랭크·경기 목록 + 경기 20판) → 처음 보는 10명은 약 240회라 **4~5분** 걸림 (화면에 대기 시간이 표시되고 자동으로 이어짐)
- 더 빨리 하려면 `.env` 의 `LOL_INSIGHT_SAMPLE` 을 10 으로 (10명 ≈ 2~3분)
- 한 번 본 소환사는 DB(24시간)·메모리 캐시 덕분에 다시 호출하지 않음
- 운영(Production) 키를 받으면 `.env` 의 `RIOT_RATE_LIMITS` 를 올리세요

## API 요약

모든 응답은 `{ "code": 200, "message": "...", "data": ... }` 이고 오류일 때는 HTTP 상태도 같은 값입니다 (429 는 `retryAfter` 와 `Retry-After` 헤더 포함).

| 메서드 | 경로 | 설명 |
|---|---|---|
| GET | `/api/v1/game/lol/riot/player/{riotId}/insight` | 소환사 검색: 프로필 + 모스트 3 기반 추천 라인 + 대표 챔피언 (`?refresh=true` 로 재분석) |
| GET | `/api/v1/game/lol/riot/player/{riotId}` | 프로필 + 숙련도 상위 3 |
| GET | `/api/v1/game/lol/riot/player/{riotId}/matches?count=10` | 최근 전적 |
| POST | `/api/v1/lol/rift/teams` | 협곡 팀 구성 (`allowOffRole` 기본 true) |
| POST | `/api/v1/lol/abyss/teams` | 칼바람 팀 구성 |
| POST | `/api/v1/groups` | 방 만들기 → `{code, name, token}` |
| POST | `/api/v1/groups/{code}/join` | 방 입장 (비밀번호, 10분에 10번 틀리면 잠김) |
| GET/POST/PUT/DELETE | `/api/v1/groups/{code}/members[/{id}]` | 단골 멤버 (POST 는 같은 사람이면 갱신) |
| POST/GET | `/api/v1/groups/{code}/matches` | 내전 저장 / 목록 |
| PUT | `/api/v1/groups/{code}/matches/{id}/result` | 승패 기록 `{"winner":"BLUE"\|"RED"\|null}` |
| DELETE | `/api/v1/groups/{code}/matches/{id}` | 기록 삭제 (보정치 되돌림) |
| GET | `/api/v1/groups/{code}/standings` | 순위표 |
| GET | `/api/v1/public/matches/{publicId}` | 공유 링크 (로그인 불필요) |
| GET | `/health` | 상태, Riot 키 설정 여부, Data Dragon 버전 |

`/api/v1/groups/{code}/**` 는 입장할 때 받은 토큰을 `X-Group-Token` 헤더로 보내야 합니다.
Riot ID 의 `#` 은 URL 에서 `%23` 으로 인코딩합니다 (예: `Hide%20on%20bush%23KR1`). 태그를 빼면 `KR1`.

## 환경 변수 (`.env`)

| 이름 | 기본값 | 설명 |
|---|---|---|
| `RIOT_API_KEY` | (필수) | Riot API 키 |
| `DB_PASSWORD` | naejeonhaja | DB 비밀번호 (처음 실행 전에 정하기) |
| `FRONTEND_PORT` / `BACKEND_PORT` | 3000 / 8080 | 포트 (백엔드는 127.0.0.1 에만 열림) |
| `RIOT_RATE_LIMITS` | 18:1,95:120 | Riot 호출 한도 "횟수:초" |
| `LOL_SUB_LINE_PENALTY` / `LOL_OFF_ROLE_PENALTY` | 100 / 250 | 부 라인 / 라인 외 감점 |
| `LOL_INSIGHT_SAMPLE` | 20 | 모스트 3 을 셀 이번 시즌 랭크 경기 수 |
| `LOL_SEASON_START` | (올해 1월 1일) | 이번 시즌 시작일 yyyy-MM-dd |
| `APP_SECRET` | (자동 생성) | 방 입장 토큰 서명 키 |
| `APP_ADMIN_TOKEN` | (비움) | Data Dragon 재로드용 |

## Docker 없이 개발하기

```bash
# DB 만 Docker 로
docker compose up -d db
# (또는 로컬 PostgreSQL 에 naejeonhaja DB/계정 생성)

# 백엔드 (JDK 17+)
cd naejeonhaja-b
echo RIOT_API_KEY=RGAPI-... > .env
./gradlew bootRun            # Windows: gradlew.bat bootRun
./gradlew test               # 테스트 69개 (H2 사용, DB 불필요)

# 프론트엔드 (Node 20+)
cd naejeonhaja-f
npm install
npm run dev                  # http://localhost:3000
```

`docker compose up -d db` 로 띄운 DB 는 compose 네트워크 안에만 열려 있으니, 로컬 백엔드에서 쓰려면 `db` 서비스에 `ports: ["5432:5432"]` 를 잠시 추가하세요.

---

내전하자는 Riot Games 의 공식 서비스가 아니며 Riot Games 의 견해를 대변하지 않습니다.
