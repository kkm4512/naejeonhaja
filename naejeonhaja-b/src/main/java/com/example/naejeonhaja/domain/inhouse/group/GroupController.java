package com.example.naejeonhaja.domain.inhouse.group;

import com.example.naejeonhaja.common.response.ApiResponse;
import com.example.naejeonhaja.domain.inhouse.auth.GroupAuthInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.example.naejeonhaja.common.response.enums.BaseApiResponse.SUCCESS;

@Tag(name = "방(그룹)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/groups")
public class GroupController {

    private final GroupService groupService;

    @Operation(summary = "방 만들기", description = "응답의 token 을 이후 요청의 X-Group-Token 헤더에 넣어 주세요.")
    @PostMapping
    public ApiResponse<GroupDtos.Session> create(@RequestBody @Valid GroupDtos.CreateRequest req) {
        return ApiResponse.of(SUCCESS, groupService.create(req));
    }

    @Operation(summary = "방 입장 (입장 코드 + 비밀번호)")
    @PostMapping("/{code}/join")
    public ApiResponse<GroupDtos.Session> join(@PathVariable String code, @RequestBody @Valid GroupDtos.JoinRequest req) {
        return ApiResponse.of(SUCCESS, groupService.join(code, req));
    }

    @Operation(summary = "방 정보 (X-Group-Token 필요)")
    @GetMapping("/{code}")
    public ApiResponse<GroupDtos.Summary> summary(@PathVariable String code,
                                                  @RequestAttribute(GroupAuthInterceptor.ATTR) InhouseGroup group) {
        return ApiResponse.of(SUCCESS, groupService.summary(group));
    }
}
