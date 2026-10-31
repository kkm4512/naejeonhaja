package com.example.naejeonhaja.domain.inhouse.match;

import com.example.naejeonhaja.domain.game.lol.enums.LolLine;
import com.example.naejeonhaja.domain.game.lol.enums.LolLineRole;
import com.example.naejeonhaja.domain.game.lol.enums.LolTier;
import com.example.naejeonhaja.domain.inhouse.member.Member;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "inhouse_match_player")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InhouseMatchPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "match_id")
    private InhouseMatch match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(nullable = false)
    private int slot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 4)
    private InhouseMatch.Team team;

    @Column(nullable = false, length = 40)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LolTier tier;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private LolLine line;

    @Enumerated(EnumType.STRING)
    @Column(name = "line_role", length = 10)
    private LolLineRole lineRole;

    @Column(name = "base_mmr", nullable = false)
    private int baseMmr;

    @Column(nullable = false)
    private int mmr;

    @Column(name = "rating_delta", nullable = false)
    private int ratingDelta;

    public InhouseMatchPlayer(Member member, int slot, InhouseMatch.Team team, String name, LolTier tier,
                              LolLine line, LolLineRole lineRole, int baseMmr, int mmr) {
        this.member = member;
        this.slot = slot;
        this.team = team;
        this.name = name;
        this.tier = tier;
        this.line = line;
        this.lineRole = lineRole;
        this.baseMmr = baseMmr;
        this.mmr = mmr;
    }

    void attach(InhouseMatch match) {
        this.match = match;
    }

    public void setRatingDelta(int ratingDelta) {
        this.ratingDelta = ratingDelta;
    }
}
