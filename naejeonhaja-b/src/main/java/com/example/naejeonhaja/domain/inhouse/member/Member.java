package com.example.naejeonhaja.domain.inhouse.member;

import com.example.naejeonhaja.domain.game.lol.enums.LolTier;
import com.example.naejeonhaja.domain.inhouse.group.InhouseGroup;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Locale;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private InhouseGroup group;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(name = "name_key", nullable = false, length = 40)
    private String nameKey;

    @Column(name = "riot_id", length = 40)
    private String riotId;

    @Column(length = 100)
    private String puuid;

    @Column(name = "profile_icon_id")
    private Integer profileIconId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LolTier tier;

    private Integer lp;

    @Column(name = "lines_json", nullable = false, columnDefinition = "TEXT")
    private String linesJson = "[]";

    @Column(name = "mmr_override")
    private Integer mmrOverride;

    @Column(name = "inhouse_delta", nullable = false)
    private int inhouseDelta;

    @Column(nullable = false)
    private int wins;

    @Column(nullable = false)
    private int losses;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    public Member(InhouseGroup group, String name) {
        this.group = group;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = this.createdAt;
        rename(name);
    }

    public static String keyOf(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    public void rename(String name) {
        this.name = name.trim();
        this.nameKey = keyOf(name);
        touch();
    }

    public void updateProfile(String riotId, String puuid, Integer profileIconId) {
        this.riotId = riotId;
        this.puuid = puuid;
        this.profileIconId = profileIconId;
        touch();
    }

    public void updateRank(LolTier tier, Integer lp) {
        this.tier = tier == null ? LolTier.UNRANKED : tier;
        this.lp = lp;
        touch();
    }

    public void updateLines(String linesJson) {
        this.linesJson = linesJson == null ? "[]" : linesJson;
        touch();
    }

    public void updateMmrOverride(Integer mmrOverride) {
        this.mmrOverride = mmrOverride != null && mmrOverride > 0 ? mmrOverride : null;
        touch();
    }

    /** 내전 결과 반영 (되돌릴 때는 음수 delta, win=null 로 승패 수만 조정) */
    public void applyResult(int ratingDelta, Boolean won, int direction) {
        this.inhouseDelta += ratingDelta;
        if (won != null) {
            if (won) this.wins = Math.max(0, this.wins + direction);
            else this.losses = Math.max(0, this.losses + direction);
        }
        touch();
    }

    /** 티어/LP 또는 직접 정한 값 */
    public int baseMmr() {
        if (mmrOverride != null && mmrOverride > 0) return mmrOverride;
        return tier.scoreWithLp(lp);
    }

    /** 밸런싱에 쓰는 최종 MMR = 기본 + 내전 보정 */
    public int effectiveMmr() {
        return Math.max(1, baseMmr() + inhouseDelta);
    }

    private void touch() {
        this.updatedAt = OffsetDateTime.now();
    }
}
