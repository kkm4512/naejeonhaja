package com.example.naejeonhaja.domain.game.lol.enums;

import lombok.Getter;

@Getter
public enum LolLineRole {
    MAINLINE,
    SUBLINE;

    public boolean isMainRole() {
        return this == MAINLINE;
    }
}
