package com.example.naejeonhaja.domain.game.riot.dto;

import com.example.naejeonhaja.domain.game.lol.enums.LolTier;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 소환사 프로필 (팀 구성 화면의 플레이어 카드용).
 *
 * @param suggestedTier 밸런싱에 쓸 티어 (솔랭 → 자랭 → UNRANKED 순)
 * @param suggestedLp   그 티어의 LP
 * @param suggestedMmr  티어 + LP + 시즌 승률 보정 MMR
 * @param mmrSource     SOLO / FLEX / UNRANKED
 * @param mmrNote       MMR 계산 근거 설명
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PlayerProfileDto(
        String puuid,
        String gameName,
        String tagLine,
        String riotId,
        long summonerLevel,
        int profileIconId,
        String profileIconUrl,
        RankDto soloRank,
        RankDto flexRank,
        List<MasteryDto> topChampions,
        LolTier suggestedTier,
        Integer suggestedLp,
        int suggestedMmr,
        String mmrSource,
        String mmrNote
) {

    /**
     * @param tierKey LolTier enum 값 (예: GOLD_II)
     */
    public record RankDto(String queueType, String tier, String rank, LolTier tierKey,
                          int leaguePoints, int wins, int losses, double winRate, boolean hotStreak) {
    }

    public record MasteryDto(int championId, String championKey, String championName, String imageUrl,
                             String splashUrl, String loadingUrl, int level, long points) {
    }
}
