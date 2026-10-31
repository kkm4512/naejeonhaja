package com.example.naejeonhaja.domain.game.lol.dto.res;

import com.example.naejeonhaja.domain.game.lol.enums.LolBalanceEnum;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * 팀 구성 결과.
 *
 * @param offRoleCount 고르지 않은 라인에 배정된 인원 (라인을 지키는 조합이 없을 때만 0보다 큼)
 * @param alternatives 차선 조합 (최상위 결과에만 포함, 차선 조합 안에서는 null)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LolTeamResponseDto(
        LolBalanceEnum balance,
        String balanceDescription,
        int teamATotalMmr,
        int teamBTotalMmr,
        int mmrDiff,
        int subLineCount,
        int offRoleCount,
        List<LolAssignedPlayerDto> teamA,
        List<LolAssignedPlayerDto> teamB,
        List<LolTeamResponseDto> alternatives
) {
    public LolTeamResponseDto withAlternatives(List<LolTeamResponseDto> alternatives) {
        return new LolTeamResponseDto(balance, balanceDescription, teamATotalMmr, teamBTotalMmr,
                mmrDiff, subLineCount, offRoleCount, teamA, teamB, alternatives);
    }
}
