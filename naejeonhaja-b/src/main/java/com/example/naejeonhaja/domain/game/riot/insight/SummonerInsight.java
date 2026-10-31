package com.example.naejeonhaja.domain.game.riot.insight;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

/** 소환사 최근 경기 분석 결과 캐시 (방과 무관하게 공유) */
@Entity
@Table(name = "summoner_insight")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SummonerInsight {

    @Id
    @Column(length = 100)
    private String puuid;

    @Column(name = "riot_id", nullable = false, length = 40)
    private String riotId;

    @Column(name = "lines_json", nullable = false, columnDefinition = "TEXT")
    private String linesJson;

    @Column(name = "stats_json", nullable = false, columnDefinition = "TEXT")
    private String statsJson;

    @Column(name = "sample_size", nullable = false)
    private int sampleSize;

    @Column(name = "computed_at", nullable = false)
    private OffsetDateTime computedAt;

    public SummonerInsight(String puuid) {
        this.puuid = puuid;
    }

    public void update(String riotId, String linesJson, String statsJson, int sampleSize) {
        this.riotId = riotId;
        this.linesJson = linesJson;
        this.statsJson = statsJson;
        this.sampleSize = sampleSize;
        this.computedAt = OffsetDateTime.now();
    }
}
