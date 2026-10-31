package com.example.naejeonhaja.domain.game.riot.service;

import com.example.naejeonhaja.common.exception.BaseException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiotIdParserTest {

    @Test
    void parsesNameAndTag() {
        RiotIdParser.RiotId id = RiotIdParser.parse(" Hide on bush #KR1 ", "KR1");
        assertThat(id.gameName()).isEqualTo("Hide on bush");
        assertThat(id.tagLine()).isEqualTo("KR1");
    }

    @Test
    void defaultTagWhenMissing() {
        assertThat(RiotIdParser.parse("자신있게싸우자", "KR1").tagLine()).isEqualTo("KR1");
        assertThat(RiotIdParser.parse("자신있게싸우자#", "KR1").tagLine()).isEqualTo("KR1");
    }

    @Test
    void rejectsInvalid() {
        assertThatThrownBy(() -> RiotIdParser.parse("  ", "KR1")).isInstanceOf(BaseException.class);
        assertThatThrownBy(() -> RiotIdParser.parse("#KR1", "KR1")).isInstanceOf(BaseException.class);
        assertThatThrownBy(() -> RiotIdParser.parse("abc#TOOLONGTAG", "KR1")).isInstanceOf(BaseException.class);
    }
}
