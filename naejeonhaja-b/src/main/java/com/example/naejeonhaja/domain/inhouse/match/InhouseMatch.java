package com.example.naejeonhaja.domain.inhouse.match;

import com.example.naejeonhaja.domain.inhouse.group.InhouseGroup;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inhouse_match")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InhouseMatch {

    public enum Team { BLUE, RED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 16)
    private String publicId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id")
    private InhouseGroup group;

    @Column(nullable = false, length = 10)
    private String mode;

    @Column(name = "blue_total", nullable = false)
    private int blueTotal;

    @Column(name = "red_total", nullable = false)
    private int redTotal;

    @Column(name = "mmr_diff", nullable = false)
    private int mmrDiff;

    @Column(nullable = false, length = 12)
    private String balance;

    @Enumerated(EnumType.STRING)
    @Column(length = 4)
    private Team winner;

    @Column(length = 100)
    private String memo;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "decided_at")
    private OffsetDateTime decidedAt;

    @OneToMany(mappedBy = "match", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("team ASC, slot ASC")
    private List<InhouseMatchPlayer> players = new ArrayList<>();

    public InhouseMatch(String publicId, InhouseGroup group, String mode, int blueTotal, int redTotal,
                        String balance, String memo) {
        this.publicId = publicId;
        this.group = group;
        this.mode = mode;
        this.blueTotal = blueTotal;
        this.redTotal = redTotal;
        this.mmrDiff = Math.abs(blueTotal - redTotal);
        this.balance = balance;
        this.memo = memo;
        this.createdAt = OffsetDateTime.now();
    }

    public void addPlayer(InhouseMatchPlayer player) {
        players.add(player);
        player.attach(this);
    }

    public void decide(Team winner) {
        this.winner = winner;
        this.decidedAt = winner == null ? null : OffsetDateTime.now();
    }

    public void updateMemo(String memo) {
        this.memo = memo;
    }
}
