package com.example.naejeonhaja.domain.game.lol.controller;

import com.example.naejeonhaja.common.response.ApiResponse;
import com.example.naejeonhaja.domain.game.lol.dto.req.LolPlayerRequestDto;
import com.example.naejeonhaja.domain.game.lol.dto.res.LolTeamResponseDto;
import com.example.naejeonhaja.domain.game.lol.service.LolTeamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.example.naejeonhaja.common.response.enums.BaseApiResponse.SUCCESS;

@Tag(name = "팀 구성")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/lol")
public class RiftController {

    private final LolTeamService lolTeamService;

    @Operation(summary = "소환사의 협곡 5:5 팀 구성",
            description = "각 팀에 5개 라인이 한 명씩 배정되는 조합 중 MMR 차이가 가장 작은 조합과 차선 조합을 반환합니다.")
    @PostMapping("/rift/teams")
    public ApiResponse<LolTeamResponseDto> createTeam(@RequestBody @Valid LolPlayerRequestDto dto) {
        return ApiResponse.of(SUCCESS, lolTeamService.createRiftTeams(dto));
    }
}
