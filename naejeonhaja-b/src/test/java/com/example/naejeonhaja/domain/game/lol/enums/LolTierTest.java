package com.example.naejeonhaja.domain.game.lol.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LolTierTest {

    @Test
    void scoresIncreaseMonotonically() {
        LolTier[] tiers = LolTier.values();
        for (int i = 2; i < tiers.length; i++) {
            assertThat(tiers[i].getScore()).isGreaterThan(tiers[i - 1].getScore());
        }
    }

    @Test
    void knownScores() {
        assertThat(LolTier.IRON_IV.getScore()).isEqualTo(50);
        assertThat(LolTier.GOLD_I.getScore()).isEqualTo(800);
        assertThat(LolTier.DIAMOND_I.getScore()).isEqualTo(1400);
        assertThat(LolTier.MASTER_I.getScore()).isEqualTo(1600);
        assertThat(LolTier.GRANDMASTER_I.getScore()).isEqualTo(2100);
        assertThat(LolTier.CHALLENGER_I.getScore()).isEqualTo(2600);
        assertThat(LolTier.UNRANKED.getScore()).isEqualTo(LolTier.UNRANKED_SCORE);
    }

    @Test
    void lpNeverReachesNextDivision() {
        assertThat(LolTier.GOLD_II.scoreWithLp(99)).isLessThan(LolTier.GOLD_I.getScore());
        assertThat(LolTier.GOLD_II.scoreWithLp(100)).isLessThan(LolTier.GOLD_I.getScore());
        assertThat(LolTier.GOLD_II.scoreWithLp(50)).isEqualTo(LolTier.GOLD_II.getScore() + 25);
        assertThat(LolTier.MASTER_I.scoreWithLp(5000)).isLessThan(LolTier.GRANDMASTER_I.getScore());
    }

    @Test
    void fromRiot() {
        assertThat(LolTier.fromRiot("GOLD", "II")).isEqualTo(LolTier.GOLD_II);
        assertThat(LolTier.fromRiot("master", "I")).isEqualTo(LolTier.MASTER_I);
        assertThat(LolTier.fromRiot("CHALLENGER", null)).isEqualTo(LolTier.CHALLENGER_I);
        assertThat(LolTier.fromRiot(null, null)).isEqualTo(LolTier.UNRANKED);
        assertThat(LolTier.fromRiot("NOPE", "I")).isEqualTo(LolTier.UNRANKED);
    }
}
