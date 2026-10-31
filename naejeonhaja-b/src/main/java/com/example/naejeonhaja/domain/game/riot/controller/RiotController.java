package com.example.naejeonhaja.domain.game.riot.controller;

import com.example.naejeonhaja.common.response.ApiResponse;
import com.example.naejeonhaja.domain.game.riot.dto.MatchHistoryDto;
import com.example.naejeonhaja.domain.game.riot.dto.PlayerProfileDto;
import com.example.naejeonhaja.domain.game.riot.insight.InsightDto;
import com.example.naejeonhaja.domain.game.riot.insight.InsightService;
import com.example.naejeonhaja.domain.game.riot.service.RiotPlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.example.naejeonhaja.common.response.enums.BaseApiResponse.SUCCESS;

/**
 * Riot ID 의 '#' 은 URL 에서 %23 으로 인코딩해야 한다. (예: Hide%20on%20bush%23KR1)
 */
@Tag(name = "Riot 소환사")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/game/lol/riot")
public class RiotController {

    private final RiotPlayerService riotPlayerService;
    private final InsightService insightService;

    @Operation(summary = "소환사 검색 (프로필 + 추천 라인 + 대표 챔피언)",
            description = "최근 경기 포지션으로 라인을 추천합니다. 결과는 24시간 DB 에 저장됩니다. "
                    + "호출 한도에 걸리면 linesPending=true 와 retryAfter(초)를 돌려주니 그 뒤에 다시 요청하세요.")
    @GetMapping("/player/{riotId}/insight")
    public ApiResponse<InsightDto> getInsight(@PathVariable String riotId,
                                              @RequestParam(defaultValue = "false") boolean refresh) {
        return ApiResponse.of(SUCCESS, insightService.get(riotId, refresh));
    }

    @Operation(summary = "소환사 프로필", description = "아이콘, 레벨, 솔랭/자랭, 모스트 챔피언 3개, 추천 MMR")
    @GetMapping("/player/{riotId}")
    public ApiResponse<PlayerProfileDto> getProfile(
            @Parameter(description = "Riot ID (이름#태그)") @PathVariable String riotId) {
        return ApiResponse.of(SUCCESS, riotPlayerService.getProfile(riotId, true));
    }

    @Operation(summary = "소환사 기본 프로필", description = "모스트 챔피언 없이 빠르게 조회")
    @GetMapping("/player/{riotId}/basic")
    public ApiResponse<PlayerProfileDto> getBasicProfile(@PathVariable String riotId) {
        return ApiResponse.of(SUCCESS, riotPlayerService.getProfile(riotId, false));
    }

    @Operation(summary = "최근 전적", description = "최근 경기 목록과 승률·KDA·포지션 통계, 추천 라인. count 최대 20")
    @GetMapping("/player/{riotId}/matches")
    public ApiResponse<MatchHistoryDto> getMatches(
            @PathVariable String riotId,
            @RequestParam(defaultValue = "10") int count,
            @Parameter(description = "큐 필터 (420 솔랭, 440 자랭, 450 칼바람 등). 비우면 전체")
            @RequestParam(required = false) Integer queue) {
        return ApiResponse.of(SUCCESS, riotPlayerService.getMatchHistory(riotId, count, queue));
    }
}
