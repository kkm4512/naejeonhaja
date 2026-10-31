package com.example.naejeonhaja;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InhouseFlowTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper om;

    private record Session(String code, String token) {
    }

    private Session createGroup(String name, String password) throws Exception {
        String body = mvc.perform(post("/api/v1/groups").contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(Map.of("name", name, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode data = om.readTree(body).path("data");
        return new Session(data.path("code").asText(), data.path("token").asText());
    }

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b, Session s) {
        return b.header("X-Group-Token", s.token()).contentType(MediaType.APPLICATION_JSON);
    }

    @Test
    @DisplayName("방 만들기 → 입장 → 토큰 없이/다른 방 토큰으로는 거부")
    void groupAuth() throws Exception {
        Session a = createGroup("우리 내전방", "pass1234");
        assertThat(a.code()).hasSize(6);

        mvc.perform(post("/api/v1/groups/{code}/join", a.code().toLowerCase()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"pass1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("우리 내전방"))
                .andExpect(jsonPath("$.data.token").isNotEmpty());

        mvc.perform(post("/api/v1/groups/{code}/join", a.code()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("비밀번호가 맞지 않습니다."));

        mvc.perform(get("/api/v1/groups/{code}/members", a.code()))
                .andExpect(status().isUnauthorized());

        Session b = createGroup("다른 방", "pass5678");
        mvc.perform(get("/api/v1/groups/{code}/members", a.code()).header("X-Group-Token", b.token()))
                .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/groups/{code}", a.code()).header("X-Group-Token", a.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.memberCount").value(0));

        mvc.perform(get("/api/v1/groups/{code}/members", "NOPE99").header("X-Group-Token", a.token()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("비밀번호를 10번 틀리면 잠시 잠김")
    void lockout() throws Exception {
        Session a = createGroup("잠금 테스트", "pass1234");
        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/api/v1/groups/{code}/join", a.code()).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"password\":\"x\"}")).andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/v1/groups/{code}/join", a.code()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"pass1234\"}"))
                .andExpect(status().isTooManyRequests());
    }

    @Test
    @DisplayName("멤버 저장(upsert)·수정·중복·삭제")
    void members() throws Exception {
        Session s = createGroup("멤버 테스트", "pass1234");
        String body = mvc.perform(auth(post("/api/v1/groups/{code}/members", s.code()), s)
                        .content("""
                                {"name":"Hide on bush#KR1","riotId":"Hide on bush#KR1","puuid":"p-1","profileIconId":29,
                                 "tier":"CHALLENGER_I","lp":500,"lines":[{"line":"MID","lineRole":"MAINLINE"}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseMmr").value(2600 + 250))
                .andExpect(jsonPath("$.data.effectiveMmr").value(2850))
                .andExpect(jsonPath("$.data.lines[0].line").value("MID"))
                .andReturn().getResponse().getContentAsString();
        long id = om.readTree(body).path("data").path("id").asLong();

        // 같은 puuid 로 다시 저장하면 새로 만들지 않고 갱신
        mvc.perform(auth(post("/api/v1/groups/{code}/members", s.code()), s)
                        .content("{\"name\":\"Hide on bush#KR1\",\"puuid\":\"p-1\",\"tier\":\"GRANDMASTER_I\",\"lines\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id))
                .andExpect(jsonPath("$.data.tier").value("GRANDMASTER_I"));

        mvc.perform(auth(post("/api/v1/groups/{code}/members", s.code()), s)
                        .content("{\"name\":\"친구\",\"tier\":\"GOLD_II\",\"mmrOverride\":1234}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.baseMmr").value(1234));

        // 다른 멤버 이름으로 바꾸려 하면 409
        mvc.perform(auth(put("/api/v1/groups/{code}/members/{id}", s.code(), id), s)
                        .content("{\"name\":\" 친 구 \",\"tier\":\"GOLD_II\"}"))
                .andExpect(status().isConflict());

        mvc.perform(auth(get("/api/v1/groups/{code}/members", s.code()), s))
                .andExpect(jsonPath("$.data", hasSize(2)));

        mvc.perform(auth(delete("/api/v1/groups/{code}/members/{id}", s.code(), id), s))
                .andExpect(status().isOk());
        mvc.perform(auth(get("/api/v1/groups/{code}/members", s.code()), s))
                .andExpect(jsonPath("$.data", hasSize(1)));
    }

    private String player(String name, String tier, String line, int mmr) {
        return """
                {"name":"%s","tier":"%s","line":"%s","lineRole":"MAINLINE","baseMmr":%d,"mmr":%d}"""
                .formatted(name, tier, line, mmr, mmr);
    }

    private String matchBody(List<String> blue, List<String> red) {
        return "{\"mode\":\"RIFT\",\"blue\":[" + String.join(",", blue) + "],\"red\":[" + String.join(",", red) + "]}";
    }

    @Test
    @DisplayName("내전 저장 → 승패 기록 → 내전 MMR 보정 → 결과 바꾸기/취소/삭제 시 되돌림 → 순위표")
    void matchesAndRating() throws Exception {
        Session s = createGroup("기록 테스트", "pass1234");
        String[] lines = {"TOP", "JUNGLE", "MID", "AD", "SUPPORT"};
        List<String> blue = new ArrayList<>();
        List<String> red = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            // 멤버로 저장해 둔 사람만 보정 대상
            mvc.perform(auth(post("/api/v1/groups/{code}/members", s.code()), s)
                    .content("{\"name\":\"B" + i + "\",\"tier\":\"GOLD_I\"}")).andExpect(status().isOk());
            mvc.perform(auth(post("/api/v1/groups/{code}/members", s.code()), s)
                    .content("{\"name\":\"R" + i + "\",\"tier\":\"GOLD_I\"}")).andExpect(status().isOk());
            blue.add(player("B" + i, "GOLD_I", lines[i], 800));
            red.add(player("R" + i, "GOLD_I", lines[i], 800));
        }
        // 손님(멤버 아님) 한 명 섞기
        red.set(4, player("손님", "SILVER_I", "SUPPORT", 800));

        String body = mvc.perform(auth(post("/api/v1/groups/{code}/matches", s.code()), s).content(matchBody(blue, red)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.blueTotal").value(4000))
                .andExpect(jsonPath("$.data.balance").value("PERFECT"))
                .andExpect(jsonPath("$.data.blue[0].memberId").isNumber())
                .andExpect(jsonPath("$.data.red[4].memberId").doesNotExist())
                .andExpect(jsonPath("$.data.winner").doesNotExist())
                .andReturn().getResponse().getContentAsString();
        JsonNode match = om.readTree(body).path("data");
        long matchId = match.path("id").asLong();
        String publicId = match.path("publicId").asText();

        // 블루 승: 팽팽한 경기 → 블루 +20, 레드 -20
        mvc.perform(auth(put("/api/v1/groups/{code}/matches/{id}/result", s.code(), matchId), s).content("{\"winner\":\"BLUE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.winner").value("BLUE"))
                .andExpect(jsonPath("$.data.blue[0].ratingDelta").value(20))
                .andExpect(jsonPath("$.data.red[0].ratingDelta").value(-20))
                .andExpect(jsonPath("$.data.red[4].ratingDelta").value(0));
        assertMember(s, "B0", 1, 0, 20);
        assertMember(s, "R0", 0, 1, -20);

        // 레드 승으로 정정 → 이전 보정 되돌리고 반대로 적용
        mvc.perform(auth(put("/api/v1/groups/{code}/matches/{id}/result", s.code(), matchId), s).content("{\"winner\":\"RED\"}"))
                .andExpect(status().isOk());
        assertMember(s, "B0", 0, 1, -20);
        assertMember(s, "R0", 1, 0, 20);

        // 순위표: 레드(이긴 쪽 멤버 4명) 위, 블루 5명 아래
        mvc.perform(auth(get("/api/v1/groups/{code}/standings", s.code()), s))
                .andExpect(jsonPath("$.data", hasSize(9)))
                .andExpect(jsonPath("$.data[0].wins").value(1))
                .andExpect(jsonPath("$.data[0].form[0]").value("W"))
                .andExpect(jsonPath("$.data[8].streak").value(-1));

        // 공유 링크 (토큰 없이)
        mvc.perform(get("/api/v1/public/matches/{p}", publicId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupName").value("기록 테스트"))
                .andExpect(jsonPath("$.data.winner").value("RED"));

        // 취소 → 모두 원래대로
        mvc.perform(auth(put("/api/v1/groups/{code}/matches/{id}/result", s.code(), matchId), s).content("{\"winner\":null}"))
                .andExpect(status().isOk());
        assertMember(s, "B0", 0, 0, 0);
        assertMember(s, "R0", 0, 0, 0);

        // 다시 블루 승 후 기록 삭제 → 보정 되돌림
        mvc.perform(auth(put("/api/v1/groups/{code}/matches/{id}/result", s.code(), matchId), s).content("{\"winner\":\"BLUE\"}"));
        mvc.perform(auth(delete("/api/v1/groups/{code}/matches/{id}", s.code(), matchId), s)).andExpect(status().isOk());
        assertMember(s, "B0", 0, 0, 0);
        mvc.perform(auth(get("/api/v1/groups/{code}/matches", s.code()), s))
                .andExpect(jsonPath("$.data.total").value(0));
    }

    @Test
    @DisplayName("기대 승률이 낮은 쪽이 이기면 더 많이 오른다")
    void upsetGivesMore() {
        int[] even = com.example.naejeonhaja.domain.inhouse.match.InhouseRating.deltas(4000, 4000,
                com.example.naejeonhaja.domain.inhouse.match.InhouseMatch.Team.BLUE);
        int[] upset = com.example.naejeonhaja.domain.inhouse.match.InhouseRating.deltas(3500, 4500,
                com.example.naejeonhaja.domain.inhouse.match.InhouseMatch.Team.BLUE);
        int[] expected = com.example.naejeonhaja.domain.inhouse.match.InhouseRating.deltas(4500, 3500,
                com.example.naejeonhaja.domain.inhouse.match.InhouseMatch.Team.BLUE);
        assertThat(even[0]).isEqualTo(20);
        assertThat(upset[0]).isGreaterThan(even[0]);
        assertThat(expected[0]).isLessThan(even[0]).isPositive();
        assertThat(upset[1]).isEqualTo(-upset[0]);
    }

    private void assertMember(Session s, String name, int wins, int losses, int delta) throws Exception {
        mvc.perform(auth(get("/api/v1/groups/{code}/members", s.code()), s))
                .andExpect(jsonPath("$.data[?(@.name == '" + name + "')].wins", contains(wins)))
                .andExpect(jsonPath("$.data[?(@.name == '" + name + "')].losses", contains(losses)))
                .andExpect(jsonPath("$.data[?(@.name == '" + name + "')].inhouseDelta", contains(delta)))
                .andExpect(jsonPath("$.data[?(@.name == '" + name + "')].effectiveMmr", contains(800 + delta)));
    }

    @Test
    @DisplayName("저장 요청 검증: 5명 아님 / 중복 이름")
    void saveValidation() throws Exception {
        Session s = createGroup("검증 테스트", "pass1234");
        mvc.perform(auth(post("/api/v1/groups/{code}/matches", s.code()), s)
                        .content(matchBody(List.of(player("a", "GOLD_I", "TOP", 800)), List.of())))
                .andExpect(status().isBadRequest());
        List<String> blue = new ArrayList<>();
        List<String> red = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            blue.add(player("P" + i, "GOLD_I", "TOP", 800));
            red.add(player("Q" + i, "GOLD_I", "TOP", 800));
        }
        red.set(0, player("p0", "GOLD_I", "TOP", 800));
        mvc.perform(auth(post("/api/v1/groups/{code}/matches", s.code()), s).content(matchBody(blue, red)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("p0")));
    }
}
