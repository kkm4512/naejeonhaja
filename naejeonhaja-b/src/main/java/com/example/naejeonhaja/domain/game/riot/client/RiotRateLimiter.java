package com.example.naejeonhaja.domain.game.riot.client;

import com.example.naejeonhaja.common.exception.RiotException;
import com.example.naejeonhaja.common.response.enums.BaseApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Riot API 호출 한도를 서버 쪽에서 미리 지킨다 (슬라이딩 윈도우).
 *
 * <p>기본값은 개발용 키 기준 "1초 20회, 2분 100회" 보다 살짝 여유 있게 잡은 "1초 18회, 2분 95회".
 * 운영 키라면 RIOT_RATE_LIMITS=500:10,30000:600 처럼 바꾸면 된다.</p>
 *
 * <p>자리가 날 때까지 {@code maxWaitMs} 이하면 기다렸다가 호출하고, 더 오래 기다려야 하면
 * 몇 초 뒤에 다시 시도하라는 RiotException(429, retryAfter) 를 던진다.
 * Riot 이 실제로 429 를 돌려주면 {@link #blockFor(int)} 로 그 시간 동안 모든 호출을 멈춘다.</p>
 */
@Slf4j
@Component
public class RiotRateLimiter {

    private record Window(int limit, long millis, Deque<Long> calls) {
    }

    private final List<Window> windows = new ArrayList<>();
    private final long maxWaitMs;
    private long blockedUntil = 0;

    public RiotRateLimiter(@Value("${riot.api.rate-limits:18:1,95:120}") String spec,
                           @Value("${riot.api.max-wait-ms:4000}") long maxWaitMs) {
        for (String part : spec.split(",")) {
            String[] lp = part.trim().split(":");
            windows.add(new Window(Integer.parseInt(lp[0].trim()), Long.parseLong(lp[1].trim()) * 1000L, new ArrayDeque<>()));
        }
        this.maxWaitMs = maxWaitMs;
    }

    /** 호출 한 번의 자리를 확보한다 (필요하면 잠깐 대기) */
    public void acquire() {
        while (true) {
            long wait;
            synchronized (this) {
                long now = System.currentTimeMillis();
                wait = Math.max(0, blockedUntil - now);
                for (Window w : windows) {
                    while (!w.calls.isEmpty() && now - w.calls.peekFirst() >= w.millis) w.calls.pollFirst();
                    if (w.calls.size() >= w.limit) {
                        wait = Math.max(wait, w.calls.peekFirst() + w.millis - now);
                    }
                }
                if (wait == 0) {
                    for (Window w : windows) w.calls.addLast(now);
                    return;
                }
            }
            if (wait > maxWaitMs) {
                int seconds = (int) Math.ceil(wait / 1000.0);
                throw new RiotException(BaseApiResponse.RIOT_RATE_LIMITED,
                        "Riot API 호출 한도에 걸렸어요. 약 " + seconds + "초 뒤에 이어서 불러올게요.", seconds);
            }
            sleep(wait + 5);
        }
    }

    /** Riot 이 429 를 돌려줬을 때: 그 시간 동안 모든 호출 중지 */
    public synchronized void blockFor(int seconds) {
        blockedUntil = Math.max(blockedUntil, System.currentTimeMillis() + seconds * 1000L);
        log.warn("Riot API 429 — {}초 동안 호출을 멈춥니다.", seconds);
    }

    /** 지금 바로 몇 번 더 호출할 수 있는지 (가장 빡빡한 창 기준) */
    public synchronized int available() {
        long now = System.currentTimeMillis();
        if (blockedUntil > now) return 0;
        int min = Integer.MAX_VALUE;
        for (Window w : windows) {
            while (!w.calls.isEmpty() && now - w.calls.peekFirst() >= w.millis) w.calls.pollFirst();
            min = Math.min(min, w.limit - w.calls.size());
        }
        return min;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RiotException(BaseApiResponse.RIOT_API_ERROR);
        }
    }
}
