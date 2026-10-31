package com.example.naejeonhaja.domain.game.lol.dto.common;

import com.example.naejeonhaja.domain.game.lol.enums.LolLine;
import com.example.naejeonhaja.domain.game.lol.enums.LolLineRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class LolLinesDto {
    @NotNull(message = "라인은 공란일 수 없습니다")
    private LolLine line;
    @NotNull(message = "라인 역할(MAINLINE/SUBLINE)은 공란일 수 없습니다")
    private LolLineRole lineRole;
}
