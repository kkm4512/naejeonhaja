package com.example.naejeonhaja.domain.game.riot.client;

import com.example.naejeonhaja.common.exception.RiotException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RiotRateLimiterTest {

    @Test
    void waitsBrieflyWhenShortWindowIsFull() {
        RiotRateLimiter limiter = new RiotRateLimiter("3:1", 2000);
        long start = System.currentTimeMillis();
        for (int i = 0; i < 4; i++) limiter.acquire();
        // 4번째는 1초 창이 비기를 기다림
        assertThat(System.currentTimeMillis() - start).isGreaterThanOrEqualTo(900);
    }

    @Test
    void throwsWithRetryAfterWhenLongWindowIsFull() {
        RiotRateLimiter limiter = new RiotRateLimiter("100:1,5:120", 2000);
        for (int i = 0; i < 5; i++) limiter.acquire();
        assertThat(limiter.available()).isZero();
        assertThatThrownBy(limiter::acquire)
                .isInstanceOf(RiotException.class)
                .satisfies(e -> assertThat(((RiotException) e).getRetryAfterSeconds()).isBetween(110, 120));
    }

    @Test
    void blockForStopsCalls() {
        RiotRateLimiter limiter = new RiotRateLimiter("100:1", 500);
        limiter.blockFor(30);
        assertThatThrownBy(limiter::acquire).isInstanceOf(RiotException.class);
    }
}
