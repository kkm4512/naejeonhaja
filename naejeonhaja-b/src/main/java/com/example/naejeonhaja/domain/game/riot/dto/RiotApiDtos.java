package com.example.naejeonhaja.domain.game.riot.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Riot API 원본 응답 매핑용 record 모음.
 */
public final class RiotApiDtos {

    private RiotApiDtos() {
    }

    /** account-v1 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Account(String puuid, String gameName, String tagLine) {
    }

    /** summoner-v4 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Summoner(String puuid, int profileIconId, long revisionDate, long summonerLevel) {
    }

    /** league-v4 entries */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LeagueEntry(String queueType, String tier, String rank, int leaguePoints,
                              int wins, int losses, boolean hotStreak) {
    }

    /** champion-mastery-v4 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChampionMastery(int championId, int championLevel, long championPoints, long lastPlayTime) {
    }
}
