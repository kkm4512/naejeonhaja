package com.example.naejeonhaja.domain.game.riot.insight;

import com.example.naejeonhaja.domain.game.lol.enums.LolLine;
import com.example.naejeonhaja.domain.game.lol.enums.LolLineRole;
import com.example.naejeonhaja.domain.game.riot.dto.MatchHistoryDto;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 모스트 3 챔피언으로 주/부 라인을 정한다 (스프링 의존 없는 순수 로직).
 *
 * <ol>
 *   <li>표본 경기(이번 시즌 랭크)에서 가장 많이 한 챔피언 3개 = 모스트 3.
 *       판 수가 같으면 더 최근에 한 챔피언이 앞.</li>
 *   <li>모스트 3 각 챔피언을 가장 많이 간 포지션 → <b>주 라인</b> (겹치면 하나로).
 *       단, 모스트 2·3 이 {@value #MOST_MIN_GAMES}판 미만이면 우연히 한 판일 수 있어 주 라인에서 뺀다.</li>
 *   <li>모스트 3 이 아닌 나머지 챔피언으로 간 포지션 중 주 라인이 아니고
 *       {@value #SUB_MIN_GAMES}판 이상인 것 → <b>부 라인</b> (많이 간 순서로 최대 {@value #MAX_SUB}개).</li>
 * </ol>
 */
public final class MostLineAnalyzer {

    public static final int MOST = 3;
    static final int MOST_MIN_GAMES = 2;
    static final int SUB_MIN_GAMES = 2;
    static final int MAX_SUB = 2;

    private MostLineAnalyzer() {
    }

    /**
     * @param rest 모스트 3 이 아닌 챔피언으로 간 포지션별 내역 (주 라인 제외, 많이 간 순). sub=true 면 부 라인이 됨
     */
    public record Result(List<MatchHistoryDto.RecommendedLine> lines, List<InsightDto.MostChampion> most,
                         List<InsightDto.RestLine> rest) {
    }

    /** @param matches 최신순 경기 목록 (다시하기 제외 전) */
    public static Result analyze(List<MatchHistoryDto.Match> matches) {
        List<MatchHistoryDto.Match> games = matches.stream()
                .filter(m -> !m.remake() && m.position() != null)
                .sorted(Comparator.comparingLong(MatchHistoryDto.Match::gameStartTimestamp).reversed())
                .toList();
        if (games.isEmpty()) return new Result(List.of(), List.of(), List.of());

        // 챔피언별 경기 (처음 나온 순서 = 최근 순서를 유지)
        Map<Integer, List<MatchHistoryDto.Match>> byChamp = new LinkedHashMap<>();
        games.forEach(m -> byChamp.computeIfAbsent(m.championId(), k -> new ArrayList<>()).add(m));
        List<List<MatchHistoryDto.Match>> ranked = byChamp.values().stream()
                .sorted(Comparator.comparingInt((List<MatchHistoryDto.Match> l) -> l.size()).reversed()) // 안정 정렬 → 동률은 최근 순
                .toList();

        Set<LolLine> mains = new LinkedHashSet<>();
        Set<Integer> mostIds = new HashSet<>();
        List<InsightDto.MostChampion> most = new ArrayList<>();
        for (int i = 0; i < Math.min(MOST, ranked.size()); i++) {
            List<MatchHistoryDto.Match> list = ranked.get(i);
            MatchHistoryDto.Match first = list.get(0);
            LolLine line = topLine(list);
            mostIds.add(first.championId());
            int wins = (int) list.stream().filter(MatchHistoryDto.Match::win).count();
            int k = list.stream().mapToInt(MatchHistoryDto.Match::kills).sum();
            int d = list.stream().mapToInt(MatchHistoryDto.Match::deaths).sum();
            int a = list.stream().mapToInt(MatchHistoryDto.Match::assists).sum();
            boolean counts = i == 0 || list.size() >= MOST_MIN_GAMES;
            if (counts) mains.add(line);
            most.add(new InsightDto.MostChampion(first.championId(), first.championName(), first.championImageUrl(),
                    list.size(), wins, round1(wins * 100.0 / list.size()),
                    d == 0 ? round1(k + a) : round1((k + a) / (double) d),
                    line, line.getLabel(), counts));
        }

        // 나머지 챔피언으로 간 포지션 → 부 라인 (포지션별로 어떤 챔피언이었는지도 함께)
        Map<LolLine, List<MatchHistoryDto.Match>> restByLine = new EnumMap<>(LolLine.class);
        games.stream()
                .filter(m -> !mostIds.contains(m.championId()))
                .filter(m -> !mains.contains(m.position()))
                .forEach(m -> restByLine.computeIfAbsent(m.position(), k -> new ArrayList<>()).add(m));
        List<Map.Entry<LolLine, List<MatchHistoryDto.Match>>> restSorted = restByLine.entrySet().stream()
                .sorted(Comparator.comparingInt((Map.Entry<LolLine, List<MatchHistoryDto.Match>> e) -> e.getValue().size()).reversed())
                .toList();
        List<LolLine> subs = restSorted.stream()
                .filter(e -> e.getValue().size() >= SUB_MIN_GAMES)
                .limit(MAX_SUB)
                .map(Map.Entry::getKey)
                .toList();
        List<InsightDto.RestLine> rest = restSorted.stream()
                .map(e -> new InsightDto.RestLine(e.getKey(), e.getKey().getLabel(), e.getValue().size(),
                        subs.contains(e.getKey()), champions(e.getValue())))
                .toList();

        List<MatchHistoryDto.RecommendedLine> lines = new ArrayList<>();
        mains.forEach(l -> lines.add(new MatchHistoryDto.RecommendedLine(l, LolLineRole.MAINLINE)));
        subs.forEach(l -> lines.add(new MatchHistoryDto.RecommendedLine(l, LolLineRole.SUBLINE)));
        return new Result(lines, most, rest);
    }

    /** 한 포지션에서 한 챔피언들 (판 수 많은 순, 같으면 최근 순) */
    private static List<InsightDto.RestChampion> champions(List<MatchHistoryDto.Match> list) {
        Map<Integer, List<MatchHistoryDto.Match>> by = new LinkedHashMap<>();
        list.forEach(m -> by.computeIfAbsent(m.championId(), k -> new ArrayList<>()).add(m));
        return by.values().stream()
                .sorted(Comparator.comparingInt((List<MatchHistoryDto.Match> l) -> l.size()).reversed())
                .map(l -> new InsightDto.RestChampion(l.get(0).championId(), l.get(0).championName(),
                        l.get(0).championImageUrl(), l.size(), (int) l.stream().filter(MatchHistoryDto.Match::win).count()))
                .toList();
    }

    /** 이 챔피언으로 가장 많이 간 포지션 (같으면 최근에 간 포지션) */
    private static LolLine topLine(List<MatchHistoryDto.Match> list) {
        Map<LolLine, Integer> count = new LinkedHashMap<>();
        list.forEach(m -> count.merge(m.position(), 1, Integer::sum));
        // Stream.max 는 동률이면 앞의 것(= 먼저 넣은 = 최근)을 남긴다
        return count.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry<LolLine, Integer>::getValue))
                .map(Map.Entry::getKey)
                .orElseThrow();
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
