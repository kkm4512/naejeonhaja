package com.example.naejeonhaja.domain.game.lol.service;

import com.example.naejeonhaja.common.exception.BaseException;
import com.example.naejeonhaja.common.response.enums.BaseApiResponse;
import com.example.naejeonhaja.domain.game.lol.dto.common.LolLinesDto;
import com.example.naejeonhaja.domain.game.lol.dto.common.LolPlayerDto;
import com.example.naejeonhaja.domain.game.lol.dto.res.LolAssignedPlayerDto;
import com.example.naejeonhaja.domain.game.lol.dto.res.LolTeamResponseDto;
import com.example.naejeonhaja.domain.game.lol.enums.LolBalanceEnum;
import com.example.naejeonhaja.domain.game.lol.enums.LolLine;
import com.example.naejeonhaja.domain.game.lol.enums.LolLineRole;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 10명을 5:5 두 팀으로 나누는 순수 알고리즘 (스프링 의존 없음).
 *
 * <p>가능한 5:5 분할 126가지(플레이어 0을 항상 A팀에 고정해 대칭 중복 제거)를 모두 살펴본다.
 * 협곡 모드에서는 분할마다 각 팀이 5개 라인을 한 명씩 맡을 수 있는지 120가지 순열로 확인하고,
 * 그중 서브 라인 배정이 가장 적은 배정을 고른다. 서브 라인을 맡은 플레이어는 MMR 에 페널티를 준 뒤
 * 두 팀 MMR 합의 차이를 계산한다.</p>
 *
 * <p>정렬 기준: ① 팀 MMR 차이 ② 서브 라인 배정 수. 상위 N개 조합을 돌려준다.
 * 전체 연산량은 126 × 2 × 120 ≈ 3만 번으로 즉시 끝난다.</p>
 */
public class LolTeamBalancer {

    public enum Mode { RIFT, ABYSS }

    private static final int PLAYER_COUNT = 10;
    private static final int TEAM_SIZE = 5;
    private static final LolLine[] LINES = LolLine.values();
    private static final int[][] PERMUTATIONS = buildPermutations();

    private final int subLinePenalty;
    private final int offRolePenalty;

    public LolTeamBalancer(int subLinePenalty) {
        this(subLinePenalty, subLinePenalty * 2);
    }

    public LolTeamBalancer(int subLinePenalty, int offRolePenalty) {
        this.subLinePenalty = Math.max(0, subLinePenalty);
        this.offRolePenalty = Math.max(0, offRolePenalty);
    }

    public List<LolTeamResponseDto> balance(List<LolPlayerDto> players, Mode mode, int maxResults) {
        return balance(players, mode, maxResults, false);
    }

    /**
     * @param maxResults  반환할 최대 조합 수 (첫 번째가 최적)
     * @param allowOffRole 라인을 지키는 조합이 하나도 없을 때, 고르지 않은 라인에도 배정해서라도 팀을 만든다
     *                     (그런 사람은 최소로, MMR 은 offRolePenalty 만큼 낮춰 계산)
     */
    public List<LolTeamResponseDto> balance(List<LolPlayerDto> players, Mode mode, int maxResults, boolean allowOffRole) {
        if (players == null || players.size() != PLAYER_COUNT) {
            throw new BaseException(BaseApiResponse.INVALID_PLAYER_COUNT);
        }
        int[] baseMmr = players.stream().mapToInt(LolPlayerDto::resolveMmr).toArray();
        LolLineRole[][] roles = buildRoleTable(players);

        List<Candidate> candidates = search(mode, baseMmr, roles, false);
        if (candidates.isEmpty() && mode == Mode.RIFT && allowOffRole) {
            candidates = search(mode, baseMmr, roles, true);
        }
        if (candidates.isEmpty()) {
            throw new BaseException(BaseApiResponse.LINE_UNSATISFIABLE, explainUnsatisfiable(players, roles));
        }

        candidates.sort(Comparator
                .comparingInt((Candidate c) -> c.offCount)
                .thenComparingInt(c -> c.diff)
                .thenComparingInt(c -> c.subCount));

        return candidates.stream()
                .limit(Math.max(1, maxResults))
                .map(c -> c.toResponse(players))
                .toList();
    }

    // ---------------------------------------------------------------- internals

    private List<Candidate> search(Mode mode, int[] baseMmr, LolLineRole[][] roles, boolean relaxed) {
        List<Candidate> candidates = new ArrayList<>();
        for (int mask = 0; mask < (1 << PLAYER_COUNT); mask++) {
            if ((mask & 1) == 0 || Integer.bitCount(mask) != TEAM_SIZE) {
                continue;
            }
            int[] teamA = members(mask, true);
            int[] teamB = members(mask, false);

            int[] linesA = null;
            int[] linesB = null;
            if (mode == Mode.RIFT) {
                linesA = bestLineAssignment(teamA, roles, relaxed);
                if (linesA == null) continue;
                linesB = bestLineAssignment(teamB, roles, relaxed);
                if (linesB == null) continue;
            }
            candidates.add(new Candidate(teamA, teamB, linesA, linesB, baseMmr, roles));
        }
        return candidates;
    }

    /** roles[player][line] = MAINLINE / SUBLINE / null(불가). 라인을 하나도 안 고르면 전 라인 MAINLINE */
    private static LolLineRole[][] buildRoleTable(List<LolPlayerDto> players) {
        LolLineRole[][] roles = new LolLineRole[PLAYER_COUNT][LINES.length];
        for (int i = 0; i < PLAYER_COUNT; i++) {
            List<LolLinesDto> lines = players.get(i).getLines();
            if (lines.isEmpty()) {
                for (int l = 0; l < LINES.length; l++) roles[i][l] = LolLineRole.MAINLINE;
                continue;
            }
            for (LolLinesDto line : lines) {
                if (line == null || line.getLine() == null) continue;
                int l = line.getLine().ordinal();
                LolLineRole role = line.getLineRole() == null ? LolLineRole.MAINLINE : line.getLineRole();
                // 같은 라인이 주/부 둘 다 들어오면 주 라인 우선
                if (roles[i][l] != LolLineRole.MAINLINE) roles[i][l] = role;
            }
        }
        return roles;
    }

    private static int[] members(int mask, boolean inMask) {
        int[] result = new int[TEAM_SIZE];
        int k = 0;
        for (int i = 0; i < PLAYER_COUNT; i++) {
            if (((mask >> i) & 1) == (inMask ? 1 : 0)) result[k++] = i;
        }
        return result;
    }

    /**
     * 팀원(인덱스 5개)에게 라인을 하나씩 배정. result[k] = team[k] 가 맡을 라인 ordinal.
     * 불가능하면 null. 가능한 배정 중 (라인 외 배정 수, 서브 라인 수) 가 가장 적은 것을 고른다.
     * relaxed=false 면 고르지 않은 라인(null)에는 배정하지 않는다.
     */
    private static int[] bestLineAssignment(int[] team, LolLineRole[][] roles, boolean relaxed) {
        int[] best = null;
        int bestCost = Integer.MAX_VALUE;
        for (int[] perm : PERMUTATIONS) {
            int cost = 0;
            boolean ok = true;
            for (int k = 0; k < TEAM_SIZE; k++) {
                LolLineRole role = roles[team[k]][perm[k]];
                if (role == null) {
                    if (!relaxed) { ok = false; break; }
                    cost += 100;
                } else if (role == LolLineRole.SUBLINE) {
                    cost += 1;
                }
            }
            if (ok && cost < bestCost) {
                bestCost = cost;
                best = perm;
                if (cost == 0) break;
            }
        }
        return best;
    }

    private static String explainUnsatisfiable(List<LolPlayerDto> players, LolLineRole[][] roles) {
        List<String> shortages = new ArrayList<>();
        for (LolLine line : LINES) {
            List<String> able = new ArrayList<>();
            for (int i = 0; i < PLAYER_COUNT; i++) {
                if (roles[i][line.ordinal()] != null) able.add(players.get(i).getName());
            }
            if (able.size() < 2) {
                shortages.add(line.getLabel() + " 가능 인원 " + able.size() + "명");
            }
        }
        if (!shortages.isEmpty()) {
            return "라인마다 최소 2명이 필요합니다: " + String.join(", ", shortages)
                    + ". 해당 라인을 서브 라인으로 추가해 주세요.";
        }
        return "모든 라인을 채우는 팀 조합이 없습니다. 몇 명의 라인 선택을 넓혀 주세요(예: '상관없음').";
    }

    private static int[][] buildPermutations() {
        List<int[]> result = new ArrayList<>();
        permute(new int[]{0, 1, 2, 3, 4}, 0, result);
        return result.toArray(new int[0][]);
    }

    private static void permute(int[] arr, int k, List<int[]> out) {
        if (k == arr.length) {
            out.add(arr.clone());
            return;
        }
        for (int i = k; i < arr.length; i++) {
            swap(arr, k, i);
            permute(arr, k + 1, out);
            swap(arr, k, i);
        }
    }

    private static void swap(int[] a, int i, int j) {
        int t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    private final class Candidate {
        final int[] teamA;
        final int[] teamB;
        final int[] linesA;
        final int[] linesB;
        final int[] baseMmr;
        final LolLineRole[][] roles;
        final int sumA;
        final int sumB;
        final int diff;
        final int subCount;
        final int offCount;

        Candidate(int[] teamA, int[] teamB, int[] linesA, int[] linesB, int[] baseMmr, LolLineRole[][] roles) {
            this.teamA = teamA;
            this.teamB = teamB;
            this.linesA = linesA;
            this.linesB = linesB;
            this.baseMmr = baseMmr;
            this.roles = roles;
            int subsA = countRole(teamA, linesA, LolLineRole.SUBLINE);
            int subsB = countRole(teamB, linesB, LolLineRole.SUBLINE);
            int offA = countRole(teamA, linesA, null);
            int offB = countRole(teamB, linesB, null);
            this.sumA = sum(teamA) - subsA * subLinePenalty - offA * offRolePenalty;
            this.sumB = sum(teamB) - subsB * subLinePenalty - offB * offRolePenalty;
            this.diff = Math.abs(sumA - sumB);
            this.subCount = subsA + subsB;
            this.offCount = offA + offB;
        }

        private int sum(int[] team) {
            int s = 0;
            for (int i : team) s += baseMmr[i];
            return s;
        }

        /** role=null 이면 라인 외 배정 수 */
        private int countRole(int[] team, int[] lines, LolLineRole role) {
            if (lines == null) return 0;
            int n = 0;
            for (int k = 0; k < TEAM_SIZE; k++) {
                if (roles[team[k]][lines[k]] == role) n++;
            }
            return n;
        }

        LolTeamResponseDto toResponse(List<LolPlayerDto> players) {
            List<LolAssignedPlayerDto> a = toPlayers(players, teamA, linesA);
            List<LolAssignedPlayerDto> b = toPlayers(players, teamB, linesB);
            LolBalanceEnum balance = LolBalanceEnum.fromDifference(diff);
            return new LolTeamResponseDto(balance, balance.getDescription(), sumA, sumB, diff, subCount, offCount, a, b, null);
        }

        private List<LolAssignedPlayerDto> toPlayers(List<LolPlayerDto> players, int[] team, int[] lines) {
            List<LolAssignedPlayerDto> result = new ArrayList<>();
            for (int k = 0; k < TEAM_SIZE; k++) {
                int idx = team[k];
                LolPlayerDto p = players.get(idx);
                if (lines == null) {
                    result.add(new LolAssignedPlayerDto(p.getName(), p.getTier(), null, null,
                            baseMmr[idx], baseMmr[idx], false, false, List.of()));
                } else {
                    LolLine line = LINES[lines[k]];
                    LolLineRole role = roles[idx][lines[k]];
                    boolean sub = role == LolLineRole.SUBLINE;
                    boolean off = role == null;
                    int mmr = baseMmr[idx] - (sub ? subLinePenalty : 0) - (off ? offRolePenalty : 0);
                    LolLineRole shown = off ? LolLineRole.SUBLINE : role;
                    result.add(new LolAssignedPlayerDto(p.getName(), p.getTier(), line, shown,
                            baseMmr[idx], mmr, sub || off, off, List.of(new LolAssignedPlayerDto.Line(line, shown))));
                }
            }
            if (lines == null) {
                result.sort(Comparator.comparingInt(LolAssignedPlayerDto::mmr).reversed());
            } else {
                result.sort(Comparator.comparingInt(p -> p.line().ordinal()));
            }
            return result;
        }
    }
}
