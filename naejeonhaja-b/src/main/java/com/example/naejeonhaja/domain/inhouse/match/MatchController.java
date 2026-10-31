package com.example.naejeonhaja.domain.inhouse.match;

import com.example.naejeonhaja.common.response.ApiResponse;
import com.example.naejeonhaja.domain.inhouse.auth.GroupAuthInterceptor;
import com.example.naejeonhaja.domain.inhouse.group.InhouseGroup;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.example.naejeonhaja.common.response.enums.BaseApiResponse.SUCCESS;

@Tag(name = "내전 기록")
@RestController
@RequiredArgsConstructor
public class MatchController {

    private final MatchService matchService;

    @Operation(summary = "팀 구성 저장")
    @PostMapping("/api/v1/groups/{code}/matches")
    public ApiResponse<MatchDtos.MatchOut> save(@PathVariable String code,
                                               @RequestAttribute(GroupAuthInterceptor.ATTR) InhouseGroup group,
                                               @RequestBody @Valid MatchDtos.SaveRequest req) {
        return ApiResponse.of(SUCCESS, matchService.save(group, req));
    }

    @Operation(summary = "내전 기록 목록 (최신순)")
    @GetMapping("/api/v1/groups/{code}/matches")
    public ApiResponse<MatchDtos.PageOut> list(@PathVariable String code,
                                              @RequestAttribute(GroupAuthInterceptor.ATTR) InhouseGroup group,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.of(SUCCESS, matchService.list(group, page, size));
    }

    @Operation(summary = "내전 기록 하나")
    @GetMapping("/api/v1/groups/{code}/matches/{id}")
    public ApiResponse<MatchDtos.MatchOut> get(@PathVariable String code, @PathVariable Long id,
                                              @RequestAttribute(GroupAuthInterceptor.ATTR) InhouseGroup group) {
        return ApiResponse.of(SUCCESS, matchService.get(group, id));
    }

    @Operation(summary = "승패 기록 (winner: BLUE / RED / null=취소)", description = "기록하면 멤버의 내전 MMR 이 보정됩니다.")
    @PutMapping("/api/v1/groups/{code}/matches/{id}/result")
    public ApiResponse<MatchDtos.MatchOut> decide(@PathVariable String code, @PathVariable Long id,
                                                 @RequestAttribute(GroupAuthInterceptor.ATTR) InhouseGroup group,
                                                 @RequestBody MatchDtos.ResultRequest req) {
        return ApiResponse.of(SUCCESS, matchService.decide(group, id, req.winner()));
    }

    @Operation(summary = "메모 수정")
    @PutMapping("/api/v1/groups/{code}/matches/{id}/memo")
    public ApiResponse<MatchDtos.MatchOut> memo(@PathVariable String code, @PathVariable Long id,
                                               @RequestAttribute(GroupAuthInterceptor.ATTR) InhouseGroup group,
                                               @RequestBody @Valid MatchDtos.MemoRequest req) {
        return ApiResponse.of(SUCCESS, matchService.updateMemo(group, id, req.memo()));
    }

    @Operation(summary = "내전 기록 삭제 (승패 보정도 되돌림)")
    @DeleteMapping("/api/v1/groups/{code}/matches/{id}")
    public ApiResponse<Void> delete(@PathVariable String code, @PathVariable Long id,
                                    @RequestAttribute(GroupAuthInterceptor.ATTR) InhouseGroup group) {
        matchService.delete(group, id);
        return ApiResponse.of(SUCCESS);
    }

    @Operation(summary = "내전 순위표")
    @GetMapping("/api/v1/groups/{code}/standings")
    public ApiResponse<List<MatchDtos.StandingRow>> standings(@PathVariable String code,
                                                             @RequestAttribute(GroupAuthInterceptor.ATTR) InhouseGroup group) {
        return ApiResponse.of(SUCCESS, matchService.standings(group));
    }

    @Operation(summary = "공유 링크로 보기 (로그인 불필요, 읽기 전용)")
    @GetMapping("/api/v1/public/matches/{publicId}")
    public ApiResponse<MatchDtos.MatchOut> getPublic(@PathVariable String publicId) {
        return ApiResponse.of(SUCCESS, matchService.getPublic(publicId));
    }
}
