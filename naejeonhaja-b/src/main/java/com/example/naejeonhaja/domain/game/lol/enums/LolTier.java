package com.example.naejeonhaja.domain.game.lol.enums;

import java.util.EnumMap;
import java.util.Locale;

/**
 * 롤 티어와 티어별 기본 점수.
 *
 * <p>아이언 IV = 50 부터 세부 티어 하나당 +50 (다이아 I = 1400).
 * 마스터 이상은 실력 격차가 크므로 가산폭을 키운다 (마스터 1600, 그마 2100, 챌린저 2600).
 * 배치를 안 본 UNRANKED 는 0점이면 밸런스가 크게 무너지므로 골드 IV 수준(650)으로 둔다.
 * 실제 실력과 다르면 화면에서 MMR을 직접 수정하면 된다.</p>
 */
public enum LolTier {
    UNRANKED,
    IRON_IV, IRON_III, IRON_II, IRON_I,
    BRONZE_IV, BRONZE_III, BRONZE_II, BRONZE_I,
    SILVER_IV, SILVER_III, SILVER_II, SILVER_I,
    GOLD_IV, GOLD_III, GOLD_II, GOLD_I,
    PLATINUM_IV, PLATINUM_III, PLATINUM_II, PLATINUM_I,
    EMERALD_IV, EMERALD_III, EMERALD_II, EMERALD_I,
    DIAMOND_IV, DIAMOND_III, DIAMOND_II, DIAMOND_I,
    MASTER_I,
    GRANDMASTER_I,
    CHALLENGER_I;

    public static final int UNRANKED_SCORE = 650;
    private static final int DIVISION_STEP = 50;

    private static final EnumMap<LolTier, Integer> SCORES = new EnumMap<>(LolTier.class);

    static {
        int score = 0;
        for (LolTier tier : values()) {
            switch (tier) {
                case UNRANKED -> SCORES.put(tier, UNRANKED_SCORE);
                case MASTER_I -> SCORES.put(tier, score += 200);
                case GRANDMASTER_I -> SCORES.put(tier, score += 500);
                case CHALLENGER_I -> SCORES.put(tier, score += 500);
                default -> SCORES.put(tier, score += DIVISION_STEP);
            }
        }
    }

    public int getScore() {
        return SCORES.get(this);
    }

    public boolean isApexTier() {
        return this == MASTER_I || this == GRANDMASTER_I || this == CHALLENGER_I;
    }

    /**
     * 티어 점수 + LP 보정.
     * 다이아 이하: LP 0~99 → +0~49 (다음 세부 티어 점수 직전까지).
     * 마스터 이상: LP 의 절반, 다음 티어 점수를 넘지 않도록 상한.
     */
    public int scoreWithLp(Integer lp) {
        int base = getScore();
        if (lp == null || lp <= 0 || this == UNRANKED) {
            return base;
        }
        return switch (this) {
            case MASTER_I -> base + Math.min(lp / 2, 450);
            case GRANDMASTER_I -> base + Math.min(lp / 2, 450);
            case CHALLENGER_I -> base + Math.min(lp / 2, 1000);
            default -> base + Math.min(lp, 99) * DIVISION_STEP / 100;
        };
    }

    /**
     * Riot League API 의 tier/rank 문자열("GOLD", "II")을 enum 으로 변환.
     */
    public static LolTier fromRiot(String tier, String rank) {
        if (tier == null || tier.isBlank()) {
            return UNRANKED;
        }
        String t = tier.trim().toUpperCase(Locale.ROOT);
        switch (t) {
            case "MASTER": return MASTER_I;
            case "GRANDMASTER": return GRANDMASTER_I;
            case "CHALLENGER": return CHALLENGER_I;
            default:
        }
        String r = rank == null || rank.isBlank() ? "IV" : rank.trim().toUpperCase(Locale.ROOT);
        try {
            return LolTier.valueOf(t + "_" + r);
        } catch (IllegalArgumentException e) {
            return UNRANKED;
        }
    }
}
