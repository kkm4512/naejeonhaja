package com.example.naejeonhaja.domain.game.lol.dto.req;

import com.example.naejeonhaja.domain.game.lol.dto.common.LolPlayerDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LolPlayerRequestDto {
    @NotNull(message = "플레이어 목록이 비어 있습니다.")
    @Size(min = 10, max = 10, message = "정확히 10명의 플레이어가 필요합니다.")
    private List<@Valid @NotNull LolPlayerDto> lolPlayers;

    /** 라인을 지키는 조합이 없으면 라인 외 배정을 허용할지 (기본 true) */
    private Boolean allowOffRole;

    public LolPlayerRequestDto(List<LolPlayerDto> lolPlayers) {
        this.lolPlayers = lolPlayers;
    }

    public boolean allowOffRoleOrDefault() {
        return allowOffRole == null || allowOffRole;
    }
}
