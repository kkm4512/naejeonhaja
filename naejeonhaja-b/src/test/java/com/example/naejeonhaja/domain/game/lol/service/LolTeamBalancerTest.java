package com.example.naejeonhaja.domain.game.lol.service;

import com.example.naejeonhaja.common.exception.BaseException;
import com.example.naejeonhaja.common.response.enums.BaseApiResponse;
import com.example.naejeonhaja.domain.game.lol.dto.common.LolLinesDto;
import com.example.naejeonhaja.domain.game.lol.dto.common.LolPlayerDto;
import com.example.naejeonhaja.domain.game.lol.dto.res.LolAssignedPlayerDto;
import com.example.naejeonhaja.domain.game.lol.dto.res.LolTeamResponseDto;
import com.example.naejeonhaja.domain.game.lol.enums.LolLine;
import com.example.naejeonhaja.domain.game.lol.enums.LolLineRole;
import com.example.naejeonhaja.domain.game.lol.enums.LolTier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.naejeonhaja.domain.game.lol.enums.LolLine.*;
import static com.example.naejeonhaja.domain.game.lol.enums.LolLineRole.MAINLINE;
import static com.example.naejeonhaja.domain.game.lol.enums.LolLineRole.SUBLINE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LolTeamBalancerTest {

    private static final int PENALTY = 100;
    private final LolTeamBalancer balancer = new LolTeamBalancer(PENALTY);

    private static LolPlayerDto p(String name, LolTier tier, Object... lineRolePairs) {
        List<LolLinesDto> lines = new ArrayList<>();
        for (int i = 0; i < lineRolePairs.length; i += 2) {
            lines.add(new LolLinesDto((LolLine) lineRolePairs[i], (LolLineRole) lineRolePairs[i + 1]));
        }
        return new LolPlayerDto(name, tier, lines);
    }

    /** 기존 프론트 "샘플 10명" 데이터 — 이전 알고리즘은 이 데이터에서 항상 실패했다. */
    private static List<LolPlayerDto> legacySample() {
        return new ArrayList<>(List.of(
                p("Player1", LolTier.GOLD_I, TOP, MAINLINE),
                p("Player2", LolTier.SILVER_II, MID, MAINLINE),
                p("Player3", LolTier.PLATINUM_III, AD, MAINLINE),
                p("Player4", LolTier.BRONZE_I, JUNGLE, MAINLINE),
                p("Player5", LolTier.DIAMOND_II, SUPPORT, MAINLINE),
                p("Player6", LolTier.IRON_IV, TOP, SUBLINE),
                p("Player7", LolTier.EMERALD_I, MID, SUBLINE),
                p("Player8", LolTier.MASTER_I, JUNGLE, SUBLINE),
                p("Player9", LolTier.GRANDMASTER_I, SUPPORT, SUBLINE),
                p("Player10", LolTier.CHALLENGER_I, AD, MAINLINE)
        ));
    }

    @Test
    @DisplayName("이전 버전에서 실패하던 샘플 데이터로도 팀이 구성된다")
    void legacySampleNowSucceeds() {
        List<LolTeamResponseDto> results = balancer.balance(legacySample(), LolTeamBalancer.Mode.RIFT, 5);

        LolTeamResponseDto best = results.get(0);
        assertAllLinesCovered(best.teamA());
        assertAllLinesCovered(best.teamB());
        assertThat(best.mmrDiff()).isEqualTo(Math.abs(best.teamATotalMmr() - best.teamBTotalMmr()));
        // 라인마다 2명씩이라 각 라인 담당자가 양 팀에 1명씩 갈라져야 한다
        assertThat(results).hasSizeGreaterThan(1);
    }

    @Test
    @DisplayName("결과는 MMR 차이 오름차순이고, 첫 번째가 라인 조건을 만족하는 조합 중 최적이다")
    void bestIsOptimalAmongValidSplits() {
        Random random = new Random(7);
        List<LolPlayerDto> players = randomPlayers(random);
        while (bruteForceBestDiff(players) < 0) {
            players = randomPlayers(random);
        }
        List<LolTeamResponseDto> results = balancer.balance(players, LolTeamBalancer.Mode.RIFT, 5);

        for (int i = 1; i < results.size(); i++) {
            assertThat(results.get(i).mmrDiff()).isGreaterThanOrEqualTo(results.get(i - 1).mmrDiff());
        }
        assertThat(results.get(0).mmrDiff()).isEqualTo(bruteForceBestDiff(players));
    }

    @RepeatedTest(30)
    @DisplayName("무작위 입력에서도 항상 각 팀 5라인 1명씩 + 최적 차이")
    void randomInputsAreValidAndOptimal() {
        Random random = new Random();
        List<LolPlayerDto> players = randomPlayers(random);
        int expected = bruteForceBestDiff(players);
        if (expected < 0) {
            assertThatThrownBy(() -> balancer.balance(players, LolTeamBalancer.Mode.RIFT, 1))
                    .isInstanceOf(BaseException.class);
            return;
        }
        LolTeamResponseDto best = balancer.balance(players, LolTeamBalancer.Mode.RIFT, 1).get(0);
        assertAllLinesCovered(best.teamA());
        assertAllLinesCovered(best.teamB());
        assertThat(best.mmrDiff()).isEqualTo(expected);
        assertEveryoneOnPermittedLine(players, best);
    }

    @Test
    @DisplayName("서브 라인에 배정되면 MMR 이 페널티만큼 깎이고 표시된다")
    void subLinePenaltyApplied() {
        LolTeamResponseDto best = balancer.balance(legacySample(), LolTeamBalancer.Mode.RIFT, 1).get(0);
        List<LolAssignedPlayerDto> all = new ArrayList<>(best.teamA());
        all.addAll(best.teamB());
        for (LolAssignedPlayerDto a : all) {
            if (a.lineRole() == SUBLINE) {
                assertThat(a.mmrReduced()).isTrue();
                assertThat(a.mmr()).isEqualTo(a.baseMmr() - PENALTY);
            } else {
                assertThat(a.mmr()).isEqualTo(a.baseMmr());
            }
        }
        assertThat(best.teamATotalMmr()).isEqualTo(best.teamA().stream().mapToInt(LolAssignedPlayerDto::mmr).sum());
    }

    @Test
    @DisplayName("라인을 비워 두면 '상관없음'으로 처리된다")
    void emptyLinesMeansFill() {
        List<LolPlayerDto> players = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            players.add(new LolPlayerDto("P" + i, LolTier.values()[1 + i * 2], List.of()));
        }
        LolTeamResponseDto best = balancer.balance(players, LolTeamBalancer.Mode.RIFT, 1).get(0);
        assertAllLinesCovered(best.teamA());
        assertAllLinesCovered(best.teamB());
        assertThat(best.subLineCount()).isZero();
    }

    @Test
    @DisplayName("한 라인을 할 수 있는 사람이 1명뿐이면 이유를 알려준다")
    void unsatisfiableExplainsShortage() {
        List<LolPlayerDto> players = new ArrayList<>();
        players.add(p("Only", LolTier.GOLD_I, SUPPORT, MAINLINE));
        for (int i = 0; i < 9; i++) {
            players.add(p("P" + i, LolTier.GOLD_I, TOP, MAINLINE, JUNGLE, MAINLINE, MID, MAINLINE, AD, MAINLINE));
        }
        assertThatThrownBy(() -> balancer.balance(players, LolTeamBalancer.Mode.RIFT, 1))
                .isInstanceOf(BaseException.class)
                .satisfies(e -> assertThat(((BaseException) e).getResponseEnum()).isEqualTo(BaseApiResponse.LINE_UNSATISFIABLE))
                .hasMessageContaining("서폿 가능 인원 1명");
    }

    @Test
    @DisplayName("라인을 지킬 수 없을 때 allowOffRole 이면 라인 외 배정을 최소로 하고 감점한다")
    void offRoleFallback() {
        List<LolPlayerDto> players = new ArrayList<>();
        players.add(p("Only", LolTier.GOLD_I, SUPPORT, MAINLINE));
        for (int i = 0; i < 9; i++) {
            players.add(p("P" + i, LolTier.GOLD_I, TOP, MAINLINE, JUNGLE, MAINLINE, MID, MAINLINE, AD, MAINLINE));
        }
        LolTeamResponseDto best = new LolTeamBalancer(PENALTY, 250).balance(players, LolTeamBalancer.Mode.RIFT, 1, true).get(0);
        assertAllLinesCovered(best.teamA());
        assertAllLinesCovered(best.teamB());
        assertThat(best.offRoleCount()).isEqualTo(1);
        LolAssignedPlayerDto off = new ArrayList<>(best.teamA()) {{ addAll(best.teamB()); }}.stream()
                .filter(LolAssignedPlayerDto::offRole).findFirst().orElseThrow();
        assertThat(off.line()).isEqualTo(SUPPORT);
        assertThat(off.mmr()).isEqualTo(off.baseMmr() - 250);
    }

    @Test
    @DisplayName("칼바람 모드는 라인 없이 MMR 차이만 최소화한다")
    void abyssIgnoresLines() {
        List<LolPlayerDto> players = new ArrayList<>();
        int[] mmrs = {3000, 2500, 2000, 1500, 1200, 1000, 800, 600, 400, 200};
        for (int i = 0; i < 10; i++) {
            players.add(new LolPlayerDto("P" + i, LolTier.GOLD_I, List.of(), mmrs[i], null));
        }
        LolTeamResponseDto best = balancer.balance(players, LolTeamBalancer.Mode.ABYSS, 1).get(0);
        assertThat(best.teamA()).allMatch(a -> a.line() == null);
        // 합계 13200 → 6600:6600 가능
        assertThat(best.mmrDiff()).isZero();
    }

    @Test
    @DisplayName("직접 입력한 MMR 이 티어 점수보다 우선한다")
    void explicitMmrWins() {
        LolPlayerDto p = new LolPlayerDto("A", LolTier.IRON_IV, List.of(), 1234, 50);
        assertThat(p.resolveMmr()).isEqualTo(1234);
        LolPlayerDto q = new LolPlayerDto("B", LolTier.GOLD_II, List.of(), 0, 60);
        assertThat(q.resolveMmr()).isEqualTo(LolTier.GOLD_II.getScore() + 30);
    }

    // ---------------------------------------------------------------- helpers

    private static void assertAllLinesCovered(List<LolAssignedPlayerDto> team) {
        assertThat(team).hasSize(5);
        Set<LolLine> lines = team.stream().map(LolAssignedPlayerDto::line).collect(Collectors.toSet());
        assertThat(lines).containsExactlyInAnyOrder(LolLine.values());
        // 라인 순서대로 정렬되어 있어야 한다
        assertThat(team.stream().map(LolAssignedPlayerDto::line).toList()).containsExactly(LolLine.values());
    }

    private static void assertEveryoneOnPermittedLine(List<LolPlayerDto> players, LolTeamResponseDto result) {
        List<LolAssignedPlayerDto> all = new ArrayList<>(result.teamA());
        all.addAll(result.teamB());
        for (LolAssignedPlayerDto a : all) {
            LolPlayerDto src = players.stream().filter(p -> p.getName().equals(a.name())).findFirst().orElseThrow();
            if (!src.getLines().isEmpty()) {
                assertThat(src.getLines()).anyMatch(l -> l.getLine() == a.line() && l.getLineRole() == a.lineRole());
            }
        }
    }

    private static List<LolPlayerDto> randomPlayers(Random random) {
        List<LolPlayerDto> players = new ArrayList<>();
        LolTier[] tiers = LolTier.values();
        for (int i = 0; i < 10; i++) {
            List<LolLine> shuffled = new ArrayList<>(Arrays.asList(LolLine.values()));
            java.util.Collections.shuffle(shuffled, random);
            int count = 1 + random.nextInt(3);
            List<LolLinesDto> lines = new ArrayList<>();
            for (int j = 0; j < count; j++) {
                lines.add(new LolLinesDto(shuffled.get(j), j == 0 ? MAINLINE : SUBLINE));
            }
            players.add(new LolPlayerDto("P" + i, tiers[1 + random.nextInt(tiers.length - 1)], lines));
        }
        return players;
    }

    /**
     * 테스트 기준값: 10명 → 252가지 5명 조합 × 각 팀 라인 순열 120 × 120 을 모두 시도하는 단순 구현.
     * 각 팀은 서브 라인 수가 최소인 배정을 쓰므로 그 기준과 동일하게 계산한다. 가능한 조합이 없으면 -1.
     */
    private static int bruteForceBestDiff(List<LolPlayerDto> players) {
        int best = Integer.MAX_VALUE;
        for (int mask = 0; mask < 1024; mask++) {
            if (Integer.bitCount(mask) != 5) continue;
            List<LolPlayerDto> a = new ArrayList<>();
            List<LolPlayerDto> b = new ArrayList<>();
            for (int i = 0; i < 10; i++) ((mask >> i & 1) == 1 ? a : b).add(players.get(i));
            Integer sa = bestTeamScore(a);
            Integer sb = bestTeamScore(b);
            if (sa == null || sb == null) continue;
            best = Math.min(best, Math.abs(sa - sb));
        }
        return best == Integer.MAX_VALUE ? -1 : best;
    }

    private static Integer bestTeamScore(List<LolPlayerDto> team) {
        int minSubs = Integer.MAX_VALUE;
        for (List<LolLine> perm : permutations(new ArrayList<>(EnumSet.allOf(LolLine.class)))) {
            int subs = 0;
            boolean ok = true;
            for (int k = 0; k < 5; k++) {
                LolPlayerDto p = team.get(k);
                LolLine line = perm.get(k);
                if (p.getLines().isEmpty()) continue;
                Set<LolLineRole> roles = new HashSet<>();
                p.getLines().stream().filter(l -> l.getLine() == line).forEach(l -> roles.add(l.getLineRole()));
                if (roles.isEmpty()) { ok = false; break; }
                if (!roles.contains(MAINLINE)) subs++;
            }
            if (ok) minSubs = Math.min(minSubs, subs);
        }
        if (minSubs == Integer.MAX_VALUE) return null;
        return team.stream().mapToInt(LolPlayerDto::resolveMmr).sum() - minSubs * PENALTY;
    }

    private static List<List<LolLine>> permutations(List<LolLine> items) {
        if (items.isEmpty()) return List.of(List.of());
        List<List<LolLine>> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            List<LolLine> rest = new ArrayList<>(items);
            LolLine head = rest.remove(i);
            for (List<LolLine> tail : permutations(rest)) {
                List<LolLine> perm = new ArrayList<>();
                perm.add(head);
                perm.addAll(tail);
                result.add(perm);
            }
        }
        return result;
    }
}
