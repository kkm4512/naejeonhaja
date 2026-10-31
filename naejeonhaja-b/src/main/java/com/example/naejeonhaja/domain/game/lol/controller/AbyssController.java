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
public class AbyssController {

    private final LolTeamService lolTeamService;

    @Operation(summary = "칼바람 나락 5:5 팀 구성", description = "라인 없이 MMR 합계 차이만 최소화합니다.")
    @PostMapping({"/abyss/teams", "/abyss/team"})
    public ApiResponse<LolTeamResponseDto> createTeam(@RequestBody @Valid LolPlayerRequestDto dto) {
        return ApiResponse.of(SUCCESS, lolTeamService.createAbyssTeams(dto));
    }
}
