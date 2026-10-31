package com.example.naejeonhaja.domain.game.riot.service;

import com.example.naejeonhaja.common.exception.BaseException;
import com.example.naejeonhaja.common.response.enums.BaseApiResponse;

/**
 * "이름#태그" 형태의 Riot ID 를 나눈다. 태그가 없으면 기본 태그(KR1)를 붙인다.
 */
public final class RiotIdParser {

    private RiotIdParser() {
    }

    public record RiotId(String gameName, String tagLine) {
        public String display() {
            return gameName + "#" + tagLine;
        }
    }

    public static RiotId parse(String raw, String defaultTagLine) {
        if (raw == null || raw.isBlank()) {
            throw new BaseException(BaseApiResponse.INVALID_RIOT_ID);
        }
        String value = raw.trim();
        String gameName;
        String tagLine;
        int idx = value.indexOf('#');
        if (idx >= 0) {
            gameName = value.substring(0, idx).trim();
            tagLine = value.substring(idx + 1).trim();
        } else {
            gameName = value;
            tagLine = defaultTagLine;
        }
        if (tagLine.isEmpty()) {
            tagLine = defaultTagLine;
        }
        // Riot ID 규칙: 이름 3~16자, 태그 3~5자 (여유 있게 검사)
        if (gameName.isEmpty() || gameName.length() > 16 || tagLine.length() > 5 || tagLine.contains("#")) {
            throw new BaseException(BaseApiResponse.INVALID_RIOT_ID,
                    "Riot ID 형식이 올바르지 않습니다: " + raw.trim() + " (예: 이름#KR1)");
        }
        return new RiotId(gameName, tagLine);
    }
}
