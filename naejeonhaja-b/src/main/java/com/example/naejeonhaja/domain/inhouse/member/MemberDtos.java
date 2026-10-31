package com.example.naejeonhaja.domain.inhouse.member;

import com.example.naejeonhaja.domain.game.lol.dto.common.LolLinesDto;
import com.example.naejeonhaja.domain.game.lol.enums.LolTier;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;

public final class MemberDtos {

    private MemberDtos() {
    }

    /**
     * 저장/수정 요청. puuid 나 이름이 같은 멤버가 있으면 그 멤버를 갱신한다 (upsert).
     */
    public record UpsertRequest(
            @NotBlank(message = "이름을 입력해주세요.") @Size(max = 40, message = "이름은 40자 이하로 입력해주세요.") String name,
            @Size(max = 40) String riotId,
            @Size(max = 100) String puuid,
            Integer profileIconId,
            LolTier tier,
            @Min(0) Integer lp,
            List<@Valid LolLinesDto> lines,
            @Min(value = 0, message = "MMR 은 0 이상이어야 합니다") @Max(value = 10000, message = "MMR 은 10000 이하여야 합니다") Integer mmrOverride) {
    }

    /**
     * @param baseMmr       티어/LP 또는 직접 정한 MMR
     * @param inhouseDelta  내전 승패로 누적된 보정치
     * @param effectiveMmr  팀 나누기에 쓰는 값 (base + delta)
     */
    public record MemberDto(
            Long id,
            String name,
            String riotId,
            String puuid,
            Integer profileIconId,
            String profileIconUrl,
            LolTier tier,
            Integer lp,
            List<LolLinesDto> lines,
            Integer mmrOverride,
            int baseMmr,
            int inhouseDelta,
            int effectiveMmr,
            int wins,
            int losses,
            double winRate,
            OffsetDateTime updatedAt) {
    }
}
