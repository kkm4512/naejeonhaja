package com.example.naejeonhaja.domain.inhouse.match;

import com.example.naejeonhaja.domain.game.lol.enums.LolBalanceEnum;
import com.example.naejeonhaja.domain.game.lol.enums.LolLine;
import com.example.naejeonhaja.domain.game.lol.enums.LolLineRole;
import com.example.naejeonhaja.domain.game.lol.enums.LolTier;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public final class MatchDtos {

    private MatchDtos() {
    }

    public record PlayerIn(
            Long memberId,
            @NotBlank String name,
            @NotNull LolTier tier,
            LolLine line,
            LolLineRole lineRole,
            @Min(0) int baseMmr,
            @Min(0) int mmr) {
    }

    /** 화면에 보이는 그대로(진영 바꾸기 반영) 블루/레드 5명씩 */
    public record SaveRequest(
            @NotNull @Pattern(regexp = "RIFT|ABYSS", message = "mode 는 RIFT 또는 ABYSS 입니다.") String mode,
            @NotNull @Size(min = 5, max = 5, message = "블루팀은 5명이어야 합니다.") List<@Valid PlayerIn> blue,
            @NotNull @Size(min = 5, max = 5, message = "레드팀은 5명이어야 합니다.") List<@Valid PlayerIn> red,
            @Size(max = 100) String memo) {
    }

    public record ResultRequest(InhouseMatch.Team winner) {
    }

    public record MemoRequest(@Size(max = 100) String memo) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PlayerOut(Long memberId, String name, LolTier tier, LolLine line, LolLineRole lineRole,
                            int baseMmr, int mmr, int ratingDelta, String profileIconUrl) {
    }

    public record MatchOut(
            Long id,
            String publicId,
            String groupName,
            String mode,
            int blueTotal,
            int redTotal,
            int mmrDiff,
            LolBalanceEnum balance,
            String balanceDescription,
            InhouseMatch.Team winner,
            String memo,
            OffsetDateTime createdAt,
            OffsetDateTime decidedAt,
            List<PlayerOut> blue,
            List<PlayerOut> red) {
    }

    public record PageOut(List<MatchOut> items, int page, int size, long total, boolean hasNext) {
    }

    /**
     * @param form 최근 5경기 결과 (최신이 앞, W/L)
     */
    public record StandingRow(Long memberId, String name, String profileIconUrl, LolTier tier,
                              int wins, int losses, double winRate, int inhouseDelta, int effectiveMmr,
                              List<String> form, int streak) {
    }
}
