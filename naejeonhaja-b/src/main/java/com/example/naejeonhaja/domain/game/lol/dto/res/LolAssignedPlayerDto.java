package com.example.naejeonhaja.domain.game.lol.dto.res;

import com.example.naejeonhaja.domain.game.lol.enums.LolLine;
import com.example.naejeonhaja.domain.game.lol.enums.LolLineRole;
import com.example.naejeonhaja.domain.game.lol.enums.LolTier;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 팀에 배정된 플레이어.
 *
 * @param line        배정된 라인 (칼바람은 null)
 * @param lineRole    배정 라인이 주/부 라인 중 무엇인지 (칼바람은 null)
 * @param baseMmr     티어/LP 또는 직접 입력한 원래 MMR
 * @param mmr         서브 라인 페널티까지 반영한 실제 밸런싱 MMR
 * @param mmrReduced  서브 라인(또는 라인 외) 배정으로 MMR 이 깎였는지
 * @param offRole     고르지 않은 라인에 배정됨 (라인을 지키는 조합이 없을 때만)
 * @param lines       하위 호환용: [배정 라인] 한 개만 담긴 목록
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LolAssignedPlayerDto(
        String name,
        LolTier tier,
        LolLine line,
        LolLineRole lineRole,
        int baseMmr,
        int mmr,
        boolean mmrReduced,
        boolean offRole,
        @Schema(description = "하위 호환용 [{line, lineRole}]") List<Line> lines
) {
    public record Line(LolLine line, LolLineRole lineRole) {
    }
}
