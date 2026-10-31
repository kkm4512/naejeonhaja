package com.example.naejeonhaja.domain.inhouse.group;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Entity
@Table(name = "inhouse_group")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InhouseGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 12)
    private String code;

    @Column(nullable = false, length = 40)
    private String name;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    public InhouseGroup(String code, String name, String passwordHash) {
        this.code = code;
        this.name = name;
        this.passwordHash = passwordHash;
        this.createdAt = OffsetDateTime.now();
    }

    public void rename(String name) {
        this.name = name;
    }
}
