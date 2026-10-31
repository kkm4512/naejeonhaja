package com.example.naejeonhaja;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "riot.api.key=RGAPI-test",
        "riot.api.regional-base-url=http://riot-asia.test",
        "riot.api.platform-base-url=http://riot-kr.test",
        "ddragon.base-url=http://ddragon.test"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApiIntegrationTest {

    private static final String DD = "http://ddragon.test";
    private static final String V = "16.21.1";
    private static final String PUUID = "puuid-123";

    @Autowired
    MockMvc mvc;
    @Autowired
    RestTemplate restTemplate;
    @Autowired
    CacheManager cacheManager;

    MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        cacheManager.getCacheNames().forEach(n -> cacheManager.getCache(n).clear());
        server = MockRestServiceServer.bindTo(restTemplate).ignoreExpectOrder(true).build();
        // Data Dragon (지연 로드 시 호출될 수 있음)
        json(ExpectedCount.manyTimes(), DD + "/api/versions.json", "[\"" + V + "\"]");
        json(ExpectedCount.manyTimes(), DD + "/cdn/" + V + "/data/ko_KR/champion.json", """
                {"data":{"Ahri":{"id":"Ahri","key":"103","name":"아리","title":"구미호","image":{"full":"Ahri.png"}},
                         "Garen":{"id":"Garen","key":"86","name":"가렌","title":"데마시아의 힘","image":{"full":"Garen.png"}}}}""");
        json(ExpectedCount.manyTimes(), DD + "/cdn/" + V + "/data/ko_KR/summoner.json",
                "{\"data\":{\"SummonerFlash\":{\"key\":\"4\",\"image\":{\"full\":\"SummonerFlash.png\"}}}}");
        json(ExpectedCount.manyTimes(), DD + "/cdn/" + V + "/data/ko_KR/runesReforged.json",
                "[{\"id\":8100,\"icon\":\"perk-images/Styles/7200_Domination.png\",\"slots\":[{\"runes\":[{\"id\":8112,\"icon\":\"perk-images/Electrocute.png\"}]}]}]");
    }

    private void json(ExpectedCount count, String url, String body) {
        server.expect(count, requestTo(url)).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
    }

    private void account() {
        server.expect(ExpectedCount.manyTimes(), requestTo(startsWith("http://riot-asia.test/riot/account/v1/accounts/by-riot-id/")))
                .andExpect(header("X-Riot-Token", "RGAPI-test"))
                .andRespond(withSuccess("{\"puuid\":\"" + PUUID + "\",\"gameName\":\"테스트 유저\",\"tagLine\":\"KR1\"}",
                        MediaType.APPLICATION_JSON));
    }

    // ======================================================== Riot 프로필

    @Test
    @DisplayName("프로필: 아이콘 URL, 솔랭, 모스트 챔피언 이미지, 추천 MMR")
    void profile() throws Exception {
        account();
        json(ExpectedCount.once(), "http://riot-kr.test/lol/summoner/v4/summoners/by-puuid/" + PUUID,
                "{\"puuid\":\"" + PUUID + "\",\"profileIconId\":29,\"summonerLevel\":321}");
        json(ExpectedCount.once(), "http://riot-kr.test/lol/league/v4/entries/by-puuid/" + PUUID, """
                [{"queueType":"RANKED_SOLO_5x5","tier":"GOLD","rank":"II","leaguePoints":50,"wins":60,"losses":40},
                 {"queueType":"RANKED_FLEX_SR","tier":"PLATINUM","rank":"IV","leaguePoints":0,"wins":5,"losses":5}]""");
        json(ExpectedCount.once(), "http://riot-kr.test/lol/champion-mastery/v4/champion-masteries/by-puuid/" + PUUID + "/top?count=3",
                "[{\"championId\":103,\"championLevel\":30,\"championPoints\":350000}]");

        mvc.perform(get("/api/v1/game/lol/riot/player/{id}", "테스트 유저#KR1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.riotId").value("테스트 유저#KR1"))
                .andExpect(jsonPath("$.data.summonerLevel").value(321))
                .andExpect(jsonPath("$.data.profileIconUrl").value(DD + "/cdn/" + V + "/img/profileicon/29.png"))
                .andExpect(jsonPath("$.data.soloRank.tierKey").value("GOLD_II"))
                .andExpect(jsonPath("$.data.soloRank.winRate").value(60.0))
                .andExpect(jsonPath("$.data.topChampions[0].championName").value("아리"))
                .andExpect(jsonPath("$.data.topChampions[0].imageUrl").value(DD + "/cdn/" + V + "/img/champion/Ahri.png"))
                .andExpect(jsonPath("$.data.suggestedTier").value("GOLD_II"))
                // 골드2 750 + LP50 → 775, 승률 60% → +30
                .andExpect(jsonPath("$.data.suggestedMmr").value(805))
                .andExpect(jsonPath("$.data.mmrSource").value("SOLO"));
    }

    @Test
    @DisplayName("없는 소환사는 HTTP 404 와 Riot ID 가 담긴 메시지")
    void notFound() throws Exception {
        server.expect(ExpectedCount.once(), requestTo(startsWith("http://riot-asia.test/riot/account")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        mvc.perform(get("/api/v1/game/lol/riot/player/{id}/basic", "없는사람#KR1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value(containsString("없는사람#KR1")));
    }

    @Test
    @DisplayName("만료된 키는 503 + 안내 메시지")
    void expiredKey() throws Exception {
        server.expect(ExpectedCount.once(), requestTo(startsWith("http://riot-asia.test/riot/account")))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));
        mvc.perform(get("/api/v1/game/lol/riot/player/{id}/basic", "누구#KR1"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(containsString("API 키")));
    }

    @Test
    @DisplayName("호출 한도 초과(긴 Retry-After)는 429")
    void rateLimited() throws Exception {
        server.expect(ExpectedCount.once(), requestTo(startsWith("http://riot-asia.test/riot/account")))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "60"));
        mvc.perform(get("/api/v1/game/lol/riot/player/{id}/basic", "누구#KR1"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value(containsString("60초")));
    }

    // ======================================================== 전적

    @Test
    @DisplayName("전적: 경기 요약, 챔피언/아이템/주문 이미지, 추천 라인")
    void matches() throws Exception {
        account();
        json(ExpectedCount.once(), "http://riot-asia.test/lol/match/v5/matches/by-puuid/" + PUUID + "/ids?start=0&count=3",
                "[\"KR_1\",\"KR_2\",\"KR_3\"]");
        json(ExpectedCount.once(), "http://riot-asia.test/lol/match/v5/matches/KR_1", match(103, "MIDDLE", true, 10, 2, 5, 420, 3000));
        json(ExpectedCount.once(), "http://riot-asia.test/lol/match/v5/matches/KR_2", match(103, "MIDDLE", false, 3, 6, 4, 420, 2000));
        json(ExpectedCount.once(), "http://riot-asia.test/lol/match/v5/matches/KR_3", match(86, "TOP", true, 7, 0, 3, 440, 1000));

        mvc.perform(get("/api/v1/game/lol/riot/player/{id}/matches", "테스트 유저#KR1").param("count", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.matches", hasSize(3)))
                // 최신 경기가 먼저
                .andExpect(jsonPath("$.data.matches[0].matchId").value("KR_1"))
                .andExpect(jsonPath("$.data.matches[0].championName").value("아리"))
                .andExpect(jsonPath("$.data.matches[0].championImageUrl").value(DD + "/cdn/" + V + "/img/champion/Ahri.png"))
                .andExpect(jsonPath("$.data.matches[0].items[0]").value(DD + "/cdn/" + V + "/img/item/3089.png"))
                .andExpect(jsonPath("$.data.matches[0].spells[0]").value(DD + "/cdn/" + V + "/img/spell/SummonerFlash.png"))
                .andExpect(jsonPath("$.data.matches[0].runes[0]").value(DD + "/cdn/img/perk-images/Electrocute.png"))
                .andExpect(jsonPath("$.data.matches[0].queueName").value("솔로랭크"))
                .andExpect(jsonPath("$.data.matches[0].position").value("MID"))
                .andExpect(jsonPath("$.data.matches[0].kda").value(7.5))
                .andExpect(jsonPath("$.data.matches[2].kda").value(10.0))
                .andExpect(jsonPath("$.data.summary.games").value(3))
                .andExpect(jsonPath("$.data.summary.wins").value(2))
                .andExpect(jsonPath("$.data.summary.recommendedLines[0].line").value("MID"))
                .andExpect(jsonPath("$.data.summary.recommendedLines[0].lineRole").value("MAINLINE"))
                .andExpect(jsonPath("$.data.summary.mostChampions[0].championName").value("아리"))
                .andExpect(jsonPath("$.data.summary.mostChampions[0].games").value(2));
    }

    /** application-test.yml 의 season-start 2026-01-01 (KST) */
    private static final long SEASON_2026 = 1767193200L;

    @Test
    @DisplayName("소환사 검색(insight): 티어 + 추천 라인 + 대표 챔피언, 두 번째는 DB 캐시")
    void insight() throws Exception {
        account();
        json(ExpectedCount.manyTimes(), "http://riot-kr.test/lol/summoner/v4/summoners/by-puuid/" + PUUID,
                "{\"puuid\":\"" + PUUID + "\",\"profileIconId\":29,\"summonerLevel\":321}");
        json(ExpectedCount.manyTimes(), "http://riot-kr.test/lol/league/v4/entries/by-puuid/" + PUUID,
                "[{\"queueType\":\"RANKED_SOLO_5x5\",\"tier\":\"PLATINUM\",\"rank\":\"I\",\"leaguePoints\":10,\"wins\":10,\"losses\":10}]");
        json(ExpectedCount.manyTimes(), "http://riot-kr.test/lol/champion-mastery/v4/champion-masteries/by-puuid/" + PUUID + "/top?count=3",
                "[{\"championId\":86,\"championLevel\":10,\"championPoints\":50000}]");
        // 이번 시즌 솔로랭크가 5판 이상 → 자유랭크·일반 경기는 조회하지 않음
        json(ExpectedCount.once(), "http://riot-asia.test/lol/match/v5/matches/by-puuid/" + PUUID
                        + "/ids?start=0&count=20&queue=420&startTime=" + SEASON_2026,
                "[\"KR_1\",\"KR_2\",\"KR_3\",\"KR_4\",\"KR_5\",\"KR_6\",\"KR_7\",\"KR_8\"]");
        // 아리(미드) 3판 = 모스트1, 가렌(탑) 2판 = 모스트2, 1판짜리 중 가장 최근인 갈리오(서폿) = 모스트3
        json(ExpectedCount.once(), "http://riot-asia.test/lol/match/v5/matches/KR_1", match(103, "MIDDLE", true, 5, 2, 5, 420, 9000));
        json(ExpectedCount.once(), "http://riot-asia.test/lol/match/v5/matches/KR_2", match(103, "MIDDLE", true, 5, 2, 5, 420, 8000));
        json(ExpectedCount.once(), "http://riot-asia.test/lol/match/v5/matches/KR_3", match(86, "TOP", true, 5, 2, 5, 420, 7000));
        json(ExpectedCount.once(), "http://riot-asia.test/lol/match/v5/matches/KR_4", match(3, "UTILITY", false, 1, 2, 9, 420, 6000));
        json(ExpectedCount.once(), "http://riot-asia.test/lol/match/v5/matches/KR_5", match(103, "MIDDLE", false, 5, 2, 5, 420, 5000));
        json(ExpectedCount.once(), "http://riot-asia.test/lol/match/v5/matches/KR_6", match(86, "TOP", true, 5, 2, 5, 420, 4000));
        // 모스트 3 밖의 챔피언으로 정글 2판 → 부 라인
        json(ExpectedCount.once(), "http://riot-asia.test/lol/match/v5/matches/KR_7", match(64, "JUNGLE", true, 5, 2, 5, 420, 3000));
        json(ExpectedCount.once(), "http://riot-asia.test/lol/match/v5/matches/KR_8", match(120, "JUNGLE", false, 5, 2, 5, 420, 2000));

        mvc.perform(get("/api/v1/game/lol/riot/player/{id}/insight", "테스트 유저#KR1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profile.suggestedTier").value("PLATINUM_I"))
                .andExpect(jsonPath("$.data.lines", hasSize(3)))
                .andExpect(jsonPath("$.data.lines[0].line").value("MID"))
                .andExpect(jsonPath("$.data.lines[0].lineRole").value("MAINLINE"))
                .andExpect(jsonPath("$.data.lines[1].line").value("TOP"))
                .andExpect(jsonPath("$.data.lines[1].lineRole").value("MAINLINE"))
                // 갈리오는 1판뿐이라 서폿은 주 라인에서 빠짐
                .andExpect(jsonPath("$.data.lines[2].line").value("JUNGLE"))
                .andExpect(jsonPath("$.data.lines[2].lineRole").value("SUBLINE"))
                .andExpect(jsonPath("$.data.recent.basis").value("SOLO"))
                .andExpect(jsonPath("$.data.recent.most3", hasSize(3)))
                .andExpect(jsonPath("$.data.recent.most3[0].championName").value("아리"))
                .andExpect(jsonPath("$.data.recent.most3[0].games").value(3))
                .andExpect(jsonPath("$.data.recent.most3[2].line").value("SUPPORT"))
                .andExpect(jsonPath("$.data.recent.most3[2].main").value(false))
                .andExpect(jsonPath("$.data.recent.restLines", hasSize(1)))
                .andExpect(jsonPath("$.data.recent.restLines[0].line").value("JUNGLE"))
                .andExpect(jsonPath("$.data.recent.restLines[0].sub").value(true))
                .andExpect(jsonPath("$.data.recent.restLines[0].champions", hasSize(2)))
                .andExpect(jsonPath("$.data.featured.championName").value("아리"))
                .andExpect(jsonPath("$.data.featured.splashUrl").value(DD + "/cdn/img/champion/splash/Ahri_0.jpg"))
                .andExpect(jsonPath("$.data.sampleSize").value(8))
                .andExpect(jsonPath("$.data.cached").value(false));

        // 두 번째: 경기 조회 없이 DB 캐시 (위 once() 기대가 깨지지 않음)
        mvc.perform(get("/api/v1/game/lol/riot/player/{id}/insight", "테스트 유저#KR1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cached").value(true))
                .andExpect(jsonPath("$.data.lines[0].line").value("MID"));
        // 경기 조회가 다시 일어났다면 once() 기대 때문에 위 요청이 실패했을 것
    }

    private static String match(int championId, String position, boolean win, int k, int d, int a, int queue, long start) {
        return """
                {"info":{"queueId":%d,"gameStartTimestamp":%d,"gameEndTimestamp":%d,"gameDuration":1800,
                  "participants":[
                    {"puuid":"%s","teamId":100,"championId":%d,"championName":"X","champLevel":16,"teamPosition":"%s",
                     "win":%s,"kills":%d,"deaths":%d,"assists":%d,"totalMinionsKilled":200,"neutralMinionsKilled":10,
                     "goldEarned":12000,"totalDamageDealtToChampions":25000,"visionScore":20,
                     "item0":3089,"item1":0,"item2":0,"item3":0,"item4":0,"item5":0,"item6":3340,
                     "summoner1Id":4,"summoner2Id":14,"doubleKills":1,
                     "perks":{"styles":[{"style":8100,"selections":[{"perk":8112}]},{"style":8100}]}},
                    {"puuid":"other","teamId":100,"kills":10},
                    {"puuid":"enemy","teamId":200,"kills":30}
                  ]}}""".formatted(queue, start, start + 1_800_000, PUUID, championId, position, win, k, d, a);
    }

    // ======================================================== 팀 구성

    private static final String SAMPLE = """
            {"lolPlayers":[
              {"name":"Player1","tier":"GOLD_I","lines":[{"line":"TOP","lineRole":"MAINLINE"}]},
              {"name":"Player2","tier":"SILVER_II","lines":[{"line":"MID","lineRole":"MAINLINE"}]},
              {"name":"Player3","tier":"PLATINUM_III","lines":[{"line":"AD","lineRole":"MAINLINE"}]},
              {"name":"Player4","tier":"BRONZE_I","lines":[{"line":"JUNGLE","lineRole":"MAINLINE"}]},
              {"name":"Player5","tier":"DIAMOND_II","lines":[{"line":"SUPPORT","lineRole":"MAINLINE"}]},
              {"name":"Player6","tier":"IRON_IV","lines":[{"line":"TOP","lineRole":"SUBLINE"}]},
              {"name":"Player7","tier":"EMERALD_I","lines":[{"line":"MID","lineRole":"SUBLINE"}]},
              {"name":"Player8","tier":"MASTER_I","lines":[{"line":"JUNGLE","lineRole":"SUBLINE"}]},
              {"name":"Player9","tier":"GRANDMASTER_I","lines":[{"line":"SUPPORT","lineRole":"SUBLINE"}]},
              {"name":"Player10","tier":"CHALLENGER_I","lines":[{"line":"AD","lineRole":"MAINLINE"}],"mmr":0,"mmrReduced":false}
            ]}""";

    @Test
    @DisplayName("협곡 팀 구성: 예전 샘플 데이터로 200 + 차선 조합")
    void riftTeams() throws Exception {
        mvc.perform(post("/api/v1/lol/rift/teams").contentType(MediaType.APPLICATION_JSON).content(SAMPLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teamA", hasSize(5)))
                .andExpect(jsonPath("$.data.teamA[*].line", contains("TOP", "JUNGLE", "MID", "AD", "SUPPORT")))
                .andExpect(jsonPath("$.data.teamB[*].line", contains("TOP", "JUNGLE", "MID", "AD", "SUPPORT")))
                .andExpect(jsonPath("$.data.alternatives", hasSize(4)))
                .andExpect(jsonPath("$.data.balance").exists());
    }

    @Test
    @DisplayName("칼바람 팀 구성 (예전 경로 /abyss/team 도 지원)")
    void abyssTeams() throws Exception {
        mvc.perform(post("/api/v1/lol/abyss/team").contentType(MediaType.APPLICATION_JSON).content(SAMPLE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.teamA", hasSize(5)))
                .andExpect(jsonPath("$.data.teamA[0].line").doesNotExist());
    }

    @Test
    @DisplayName("검증 오류: 인원수, 중복 이름, 잘못된 티어")
    void validationErrors() throws Exception {
        mvc.perform(post("/api/v1/lol/rift/teams").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lolPlayers\":[{\"name\":\"a\",\"tier\":\"GOLD_I\"}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("정확히 10명의 플레이어가 필요합니다."));

        mvc.perform(post("/api/v1/lol/rift/teams").contentType(MediaType.APPLICATION_JSON)
                        .content(SAMPLE.replace("\"Player2\"", "\"player1\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("player1")));

        mvc.perform(post("/api/v1/lol/rift/teams").contentType(MediaType.APPLICATION_JSON)
                        .content(SAMPLE.replace("GOLD_I", "GOLD_V")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("라인을 지킬 수 없으면: 기본은 라인 외 배정으로라도 팀 구성, allowOffRole=false 면 422 + 안내")
    void unsatisfiable() throws Exception {
        String body = SAMPLE.replace("{\"line\":\"SUPPORT\",\"lineRole\":\"SUBLINE\"}", "{\"line\":\"TOP\",\"lineRole\":\"SUBLINE\"}");
        mvc.perform(post("/api/v1/lol/rift/teams").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.offRoleCount").value(1))
                .andExpect(jsonPath("$.data.teamA[*].line", contains("TOP", "JUNGLE", "MID", "AD", "SUPPORT")));

        String strict = body.replaceFirst("\\{\"lolPlayers\"", "{\"allowOffRole\":false,\"lolPlayers\"");
        mvc.perform(post("/api/v1/lol/rift/teams").contentType(MediaType.APPLICATION_JSON).content(strict))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(containsString("서폿")));
    }

    @Test
    void health() throws Exception {
        mvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.data.riotApiKeyConfigured").value(true));
    }
}
