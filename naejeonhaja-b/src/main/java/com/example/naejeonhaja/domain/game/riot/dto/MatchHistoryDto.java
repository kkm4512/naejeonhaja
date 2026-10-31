package com.example.naejeonhaja.domain.game.riot.dto;

import com.example.naejeonhaja.domain.game.lol.enums.LolLine;
import com.example.naejeonhaja.domain.game.lol.enums.LolLineRole;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 최근 전적과 요약 통계.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MatchHistoryDto(String puuid, String riotId, Summary summary, List<Match> matches) {

    /**
     * @param recommendedLines 최근 협곡 경기 포지션 기준 추천 라인 (가장 많이 간 라인 = 주, 다음 = 부)
     */
    public record Summary(int games, int wins, int losses, double winRate,
                          double avgKills, double avgDeaths, double avgAssists, double kda,
                          double avgCsPerMin, double avgKillParticipation,
                          List<PositionStat> positions,
                          List<RecommendedLine> recommendedLines,
                          List<ChampionStat> mostChampions) {
    }

    public record PositionStat(LolLine line, String label, int games, double ratio) {
    }

    public record RecommendedLine(LolLine line, LolLineRole lineRole) {
    }

    public record ChampionStat(int championId, String championName, String imageUrl,
                               int games, int wins, double winRate, double kda) {
    }

    /**
     * @param remake     다시하기(조기 항복) 경기 — 승패 집계에서 제외
     * @param items      아이템 0~5 + 장신구 이미지 URL (빈 칸은 null)
     * @param spells     소환사 주문 2개 이미지 URL
     * @param runes      핵심 룬, 보조 룬 계열 아이콘 URL
     */
    @JsonInclude(JsonInclude.Include.ALWAYS)
    public record Match(String matchId, int queueId, String queueName,
                        long gameStartTimestamp, long gameDurationSec,
                        boolean win, boolean remake,
                        int championId, String championName, String championImageUrl, int champLevel,
                        LolLine position,
                        int kills, int deaths, int assists, double kda, double killParticipation,
                        int cs, double csPerMin, int goldEarned, int damageToChampions, int visionScore,
                        String multiKill,
                        List<String> items, List<String> spells, List<String> runes) {
    }
}
