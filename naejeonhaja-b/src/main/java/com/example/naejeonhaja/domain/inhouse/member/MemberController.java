package com.example.naejeonhaja.domain.inhouse.member;

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

@Tag(name = "단골 멤버")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups/{code}/members")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "멤버 목록")
    @GetMapping
    public ApiResponse<List<MemberDtos.MemberDto>> list(@PathVariable String code,
                                                       @RequestAttribute(GroupAuthInterceptor.ATTR) InhouseGroup group) {
        return ApiResponse.of(SUCCESS, memberService.list(group));
    }

    @Operation(summary = "멤버 저장 (같은 사람이 있으면 갱신)")
    @PostMapping
    public ApiResponse<MemberDtos.MemberDto> upsert(@PathVariable String code,
                                                   @RequestAttribute(GroupAuthInterceptor.ATTR) InhouseGroup group,
                                                   @RequestBody @Valid MemberDtos.UpsertRequest req) {
        return ApiResponse.of(SUCCESS, memberService.upsert(group, req));
    }

    @Operation(summary = "멤버 수정")
    @PutMapping("/{id}")
    public ApiResponse<MemberDtos.MemberDto> update(@PathVariable String code, @PathVariable Long id,
                                                   @RequestAttribute(GroupAuthInterceptor.ATTR) InhouseGroup group,
                                                   @RequestBody @Valid MemberDtos.UpsertRequest req) {
        return ApiResponse.of(SUCCESS, memberService.update(group, id, req));
    }

    @Operation(summary = "멤버 삭제 (지난 내전 기록의 이름은 남음)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String code, @PathVariable Long id,
                                    @RequestAttribute(GroupAuthInterceptor.ATTR) InhouseGroup group) {
        memberService.delete(group, id);
        return ApiResponse.of(SUCCESS);
    }
}
