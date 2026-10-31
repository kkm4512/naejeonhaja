package com.example.naejeonhaja.domain.game.dataDragon.controller;

import com.example.naejeonhaja.common.exception.BaseException;
import com.example.naejeonhaja.common.response.ApiResponse;
import com.example.naejeonhaja.common.response.enums.BaseApiResponse;
import com.example.naejeonhaja.config.AppProperties;
import com.example.naejeonhaja.domain.game.dataDragon.dto.ChampionInfo;
import com.example.naejeonhaja.domain.game.dataDragon.dto.DataDragonMetaDto;
import com.example.naejeonhaja.domain.game.dataDragon.service.DataDragonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static com.example.naejeonhaja.common.response.enums.BaseApiResponse.SUCCESS;

@Tag(name = "Data Dragon")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/game/lol/dataDragon")
public class DataDragonController {

    private final DataDragonService dataDragonService;
    private final AppProperties.App appProperties;

    @Operation(summary = "현재 Data Dragon 버전과 CDN 주소")
    @GetMapping("/meta")
    public ApiResponse<DataDragonMetaDto> meta() {
        return ApiResponse.of(SUCCESS, dataDragonService.meta());
    }

    @Operation(summary = "챔피언 정보 (숫자 key 또는 영문 id)")
    @GetMapping("/champion/{championKey}")
    public ApiResponse<ChampionInfo> getChampion(@PathVariable String championKey) {
        ChampionInfo info = championKey.chars().allMatch(Character::isDigit)
                ? dataDragonService.getChampion(Integer.parseInt(championKey))
                : dataDragonService.getChampionById(championKey);
        if (info == null) {
            throw new BaseException(BaseApiResponse.NOT_FOUND, "챔피언을 찾을 수 없습니다: " + championKey);
        }
        return ApiResponse.of(SUCCESS, info);
    }

    @Operation(summary = "Data Dragon 재로드 (X-Admin-Token 필요)")
    @PostMapping("/reload")
    public ApiResponse<DataDragonMetaDto> reload(@RequestHeader(value = "X-Admin-Token", required = false) String token) {
        String expected = appProperties.adminToken();
        if (expected == null || expected.isBlank() || token == null
                || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
            throw new BaseException(BaseApiResponse.FORBIDDEN);
        }
        return ApiResponse.of(SUCCESS, dataDragonService.reload());
    }
}
