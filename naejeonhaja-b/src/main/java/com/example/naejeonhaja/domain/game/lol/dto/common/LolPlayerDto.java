package com.example.naejeonhaja.domain.game.lol.dto.common;

import com.example.naejeonhaja.domain.game.lol.enums.LolTier;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LolPlayerDto {

    @NotBlank(message = "이름은 공란일 수 없습니다")
    private String name;

    @NotNull(message = "티어는 공란일 수 없습니다")
    private LolTier tier;

    @Schema(description = "가능한 라인 목록. 비워 두면 모든 라인 가능(상관없음)으로 처리")
    private List<@Valid LolLinesDto> lines = new ArrayList<>();

    @Schema(description = "직접 지정한 MMR. 0 또는 null 이면 티어 + LP 로 계산")
    @Min(value = 0, message = "MMR 은 0 이상이어야 합니다")
    @Max(value = 10000, message = "MMR 은 10000 이하여야 합니다")
    private Integer mmr;

    @Schema(description = "현재 티어의 LP (선택)")
    @Min(value = 0, message = "LP 는 0 이상이어야 합니다")
    private Integer lp;

    public LolPlayerDto(String name, LolTier tier, List<LolLinesDto> lines) {
        this(name, tier, lines, null, null);
    }

    public List<LolLinesDto> getLines() {
        return lines == null ? List.of() : lines;
    }

    /** 밸런싱에 사용할 기본 MMR */
    public int resolveMmr() {
        if (mmr != null && mmr > 0) {
            return mmr;
        }
        return tier.scoreWithLp(lp);
    }
}
