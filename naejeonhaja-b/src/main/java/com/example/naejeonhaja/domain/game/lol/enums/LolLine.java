package com.example.naejeonhaja.domain.game.lol.enums;

import java.util.Locale;

public enum LolLine {
    TOP("탑"),
    JUNGLE("정글"),
    MID("미드"),
    AD("원딜"),
    SUPPORT("서폿");

    private final String label;

    LolLine(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Riot match-v5 의 teamPosition / individualPosition 값을 변환. 알 수 없으면 null.
     */
    public static LolLine fromRiotPosition(String position) {
        if (position == null) {
            return null;
        }
        return switch (position.toUpperCase(Locale.ROOT)) {
            case "TOP" -> TOP;
            case "JUNGLE" -> JUNGLE;
            case "MIDDLE", "MID" -> MID;
            case "BOTTOM", "BOT", "ADC" -> AD;
            case "UTILITY", "SUPPORT" -> SUPPORT;
            default -> null;
        };
    }
}
