# 내전하자 백엔드 (naejeonhaja-b)

롤 내전 팀 구성 API 서버입니다. 실행 방법, API 목록, 밸런싱 규칙은 **상위 폴더의 [README.md](../README.md)** 를 보세요.

- Spring Boot 3.3 · Java 17 · Gradle 8.14
- Swagger UI: http://localhost:8080/docs
- DB: PostgreSQL (Flyway 마이그레이션 `src/main/resources/db/migration`)
- 테스트: `./gradlew test` (H2 사용, 밸런싱 무작위 검증·방/기록 흐름·Riot 모의 응답 포함)

## 패키지 구조

```
com.example.naejeonhaja
├── common
│   ├── config/WebConfig              CORS (APP_ALLOWED_ORIGINS)
│   ├── exception/                    BaseException, RiotException, GlobalExceptionHandler
│   └── response/                     ApiResponse{code, message, data}, BaseApiResponse(오류 코드)
├── config/                           AppProperties(설정), CacheConfig(Caffeine), RestTemplate(타임아웃), Swagger
└── domain/game
    ├── health/                       GET /health
    ├── lol/
    │   ├── controller/               Rift(협곡) / Abyss(칼바람)
    │   ├── service/LolTeamBalancer   ★ 5:5 전수 탐색 + 라인 배정 (스프링 의존 없는 순수 로직)
    │   ├── service/LolTeamService    이름 중복 검사, 차선 조합
    │   ├── dto/                      요청(LolPlayerDto), 응답(LolTeamResponseDto, LolAssignedPlayerDto)
    │   └── enums/                    LolTier(점수/LP), LolLine, LolLineRole, LolBalanceEnum
    ├── riot/insight/                 소환사 검색(프로필+모스트 3 라인+대표 챔피언), MostLineAnalyzer, summoner_insight 캐시
    ├── riot/
    │   ├── client/RiotApiClient      상태 코드 → 의미 있는 예외 (404/401·403/429/5xx)
    │   ├── client/RiotGateway        엔드포인트별 호출 + 캐시
    │   ├── service/RiotPlayerService 프로필, 추천 MMR, 전적 요약, 추천 라인
    │   └── dto/                      Riot 원본(RiotApiDtos), 응답(PlayerProfileDto, MatchHistoryDto)
    └── dataDragon/                   버전·챔피언·주문·룬 메모리 캐시, 이미지 URL, 6시간마다 새 패치 확인
com.example.naejeonhaja.domain.inhouse
    ├── auth/                         방 입장 토큰(HMAC) 발급·검증, X-Group-Token 인터셉터
    ├── group/                        방 만들기/입장 (BCrypt, 비밀번호 대입 잠금)
    ├── member/                       단골 멤버 (upsert, 내전 보정치)
    └── match/                        내전 기록, 승패 → Elo 보정(InhouseRating), 순위표, 공유
```

## 설정 (application.yml / 환경 변수)

| 환경 변수 | 기본값 |
|---|---|
| `RIOT_API_KEY` | (필수) |
| `RIOT_REGIONAL_BASE_URL` | https://asia.api.riotgames.com |
| `RIOT_PLATFORM_BASE_URL` | https://kr.api.riotgames.com |
| `RIOT_DEFAULT_TAG_LINE` | KR1 |
| `RIOT_RATE_LIMITS` | 18:1,95:120 |
| `DB_URL` / `DB_USERNAME` / `DB_PASSWORD` | jdbc:postgresql://localhost:5432/naejeonhaja / naejeonhaja / naejeonhaja |
| `APP_SECRET` | (비우면 DB 에 자동 생성) |
| `LOL_OFF_ROLE_PENALTY` | 250 |
| `DDRAGON_BASE_URL` | https://ddragon.leagueoflegends.com |
| `LOL_SUB_LINE_PENALTY` | 100 |
| `LOL_INSIGHT_SAMPLE` | 20 |
| `LOL_SEASON_START` | (올해 1월 1일) |
| `APP_ALLOWED_ORIGINS` | http://localhost:3000 |
| `APP_ADMIN_TOKEN` | (비움 = reload 비활성화) |

로컬 실행 시 이 폴더의 `.env` 파일도 읽습니다 (`RIOT_API_KEY=...`).

## 캐시 (Caffeine)

| 대상 | 유지 시간 |
|---|---|
| 계정 (Riot ID → PUUID) | 6시간 |
| 소환사 | 10분 |
| 랭크 | 5분 |
| 숙련도 | 30분 |
| 경기 ID 목록 | 2분 |
| 경기 상세 | 24시간 |
