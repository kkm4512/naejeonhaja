package com.example.naejeonhaja.domain.game.riot.insight;

import com.example.naejeonhaja.domain.game.lol.enums.LolLine;
import com.example.naejeonhaja.domain.game.lol.enums.LolLineRole;
import com.example.naejeonhaja.domain.game.riot.dto.MatchHistoryDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MostLineAnalyzerTest {

    private long clock = 1_000_000;

    /** 호출 순서대로 점점 과거 경기 (첫 번째가 가장 최근) */
    private MatchHistoryDto.Match game(int champ, LolLine line) {
        return game(champ, line, false);
    }

    private MatchHistoryDto.Match game(int champ, LolLine line, boolean remake) {
        clock -= 1000;
        return new MatchHistoryDto.Match("M" + clock, 420, "솔로랭크", clock, 1800, true, remake,
                champ, "C" + champ, null, 16, line, 5, 2, 5, 5.0, 50, 200, 7, 12000, 20000, 20, null,
                List.of(), List.of(), List.of());
    }

    private static List<String> lines(MostLineAnalyzer.Result r) {
        return r.lines().stream().map(l -> l.line() + ":" + (l.lineRole() == LolLineRole.MAINLINE ? "주" : "부")).toList();
    }

    @Test
    @DisplayName("모스트 3 이 모두 2판 이상이면 세 포지션 모두 주 라인, 나머지 챔피언 포지션은 부 라인")
    void threeMains() {
        List<MatchHistoryDto.Match> m = new ArrayList<>();
        for (int i = 0; i < 4; i++) m.add(game(1, LolLine.MID));
        for (int i = 0; i < 3; i++) m.add(game(2, LolLine.TOP));
        for (int i = 0; i < 2; i++) m.add(game(3, LolLine.JUNGLE));
        m.add(game(4, LolLine.SUPPORT));
        m.add(game(5, LolLine.SUPPORT));
        m.add(game(6, LolLine.AD));

        MostLineAnalyzer.Result r = MostLineAnalyzer.analyze(m);
        assertThat(lines(r)).containsExactly("MID:주", "TOP:주", "JUNGLE:주", "SUPPORT:부");
        assertThat(r.most()).extracting(InsightDto.MostChampion::championId).containsExactly(1, 2, 3);
        // 부 라인 판별 근거: 서폿(챔피언 4·5, 2판 → 부), 원딜(챔피언 6, 1판 → 부 아님)
        assertThat(r.rest()).extracting(InsightDto.RestLine::line).containsExactly(LolLine.SUPPORT, LolLine.AD);
        assertThat(r.rest().get(0).sub()).isTrue();
        assertThat(r.rest().get(0).champions()).extracting(InsightDto.RestChampion::championId).containsExactly(4, 5);
        assertThat(r.rest().get(1).sub()).isFalse();
    }

    @Test
    @DisplayName("모스트 3 끼리 같은 포지션이면 주 라인은 하나, 나머지에서 1판뿐인 포지션은 부 라인이 아님")
    void sameLineMost() {
        List<MatchHistoryDto.Match> m = new ArrayList<>();
        for (int i = 0; i < 5; i++) m.add(game(1, LolLine.MID));
        for (int i = 0; i < 3; i++) m.add(game(2, LolLine.MID));
        for (int i = 0; i < 2; i++) m.add(game(3, LolLine.MID));
        m.add(game(4, LolLine.TOP));
        m.add(game(5, LolLine.AD));

        assertThat(lines(MostLineAnalyzer.analyze(m))).containsExactly("MID:주");
    }

    @Test
    @DisplayName("챔피언 하나를 여러 포지션으로 갔으면 가장 많이 간 포지션, 같으면 최근 포지션")
    void championMultiLine() {
        List<MatchHistoryDto.Match> m = new ArrayList<>();
        m.add(game(1, LolLine.SUPPORT));
        m.add(game(1, LolLine.MID));
        m.add(game(1, LolLine.MID));
        m.add(game(2, LolLine.TOP));
        m.add(game(2, LolLine.JUNGLE));

        MostLineAnalyzer.Result r = MostLineAnalyzer.analyze(m);
        assertThat(r.most().get(0).line()).isEqualTo(LolLine.MID);
        assertThat(r.most().get(1).line()).isEqualTo(LolLine.TOP); // 1:1 → 더 최근인 탑
    }

    @Test
    @DisplayName("다시하기·포지션 없는 경기는 빼고, 경기가 없으면 빈 결과")
    void emptyAndRemake() {
        assertThat(MostLineAnalyzer.analyze(List.of()).lines()).isEmpty();
        List<MatchHistoryDto.Match> m = List.of(game(1, LolLine.TOP, true), game(2, null), game(3, LolLine.AD));
        assertThat(lines(MostLineAnalyzer.analyze(m))).containsExactly("AD:주");
    }
}
