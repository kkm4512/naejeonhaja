package com.example.naejeonhaja.domain.inhouse.common;

import com.example.naejeonhaja.domain.game.lol.dto.common.LolLinesDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/** 라인 목록 ↔ JSON 문자열 (member.lines_json) */
public final class LinesJson {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private LinesJson() {
    }

    public static String write(List<LolLinesDto> lines) {
        try {
            return MAPPER.writeValueAsString(lines == null ? List.of() : lines);
        } catch (Exception e) {
            return "[]";
        }
    }

    public static List<LolLinesDto> read(String json) {
        try {
            return json == null || json.isBlank() ? List.of() : MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
