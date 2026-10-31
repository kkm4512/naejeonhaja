package com.example.naejeonhaja.domain.game.lol.enums;

/**
 * 두 팀 MMR 합계 차이에 따른 밸런스 등급.
 */
public enum LolBalanceEnum {
    PERFECT(0, 100, "완벽한 밸런스"),
    EXCELLENT(101, 300, "우수한 밸런스"),
    GOOD(301, 600, "좋은 밸런스"),
    FAIR(601, 1000, "보통 밸런스"),
    POOR(1001, Integer.MAX_VALUE, "불균형한 밸런스");

    private final int minDiff;
    private final int maxDiff;
    private final String description;

    LolBalanceEnum(int minDiff, int maxDiff, String description) {
        this.minDiff = minDiff;
        this.maxDiff = maxDiff;
        this.description = description;
    }

    public int getMinDiff() {
        return minDiff;
    }

    public int getMaxDiff() {
        return maxDiff;
    }

    public String getDescription() {
        return description;
    }

    public static LolBalanceEnum fromDifference(int diff) {
        int abs = Math.abs(diff);
        for (LolBalanceEnum balance : values()) {
            if (abs >= balance.minDiff && abs <= balance.maxDiff) {
                return balance;
            }
        }
        return POOR;
    }
}
