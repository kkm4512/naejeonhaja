package com.example.naejeonhaja.domain.game.health;

import com.example.naejeonhaja.common.response.ApiResponse;
import com.example.naejeonhaja.config.AppProperties;
import com.example.naejeonhaja.domain.game.dataDragon.service.DataDragonService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import static com.example.naejeonhaja.common.response.enums.BaseApiResponse.SUCCESS;

@RestController
@RequiredArgsConstructor
@RequestMapping("/health")
public class HealthController {

    private final AppProperties.Riot riotProperties;
    private final DataDragonService dataDragonService;

    /**
     * @return status, Riot 키 설정 여부, Data Dragon 버전 (프론트에서 경고 배너용)
     */
    @GetMapping
    public ApiResponse<HealthDto> healthCheck() {
        return ApiResponse.of(SUCCESS, new HealthDto("UP", riotProperties.hasKey(), dataDragonService.getVersion()));
    }

    public record HealthDto(String status, boolean riotApiKeyConfigured, String ddragonVersion) {
    }
}
