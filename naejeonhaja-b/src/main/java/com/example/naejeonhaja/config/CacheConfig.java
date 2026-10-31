package com.example.naejeonhaja.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Riot API 응답 캐시. 개발용 키는 2분에 100회 제한이라 같은 소환사를 다시 불러올 때 호출을 아낀다.
 */
@Configuration
public class CacheConfig {

    public static final String ACCOUNT = "riotAccount";
    public static final String SUMMONER = "riotSummoner";
    public static final String LEAGUE = "riotLeague";
    public static final String MASTERY = "riotMastery";
    public static final String MATCH_IDS = "riotMatchIds";
    public static final String MATCH = "riotMatch";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setAllowNullValues(false);
        manager.registerCustomCache(ACCOUNT, build(Duration.ofHours(6), 2_000));
        manager.registerCustomCache(SUMMONER, build(Duration.ofMinutes(10), 2_000));
        manager.registerCustomCache(LEAGUE, build(Duration.ofMinutes(5), 2_000));
        manager.registerCustomCache(MASTERY, build(Duration.ofMinutes(30), 2_000));
        manager.registerCustomCache(MATCH_IDS, build(Duration.ofMinutes(2), 2_000));
        // 끝난 경기 정보는 바뀌지 않으므로 오래 보관
        manager.registerCustomCache(MATCH, build(Duration.ofHours(24), 5_000));
        return manager;
    }

    private static com.github.benmanes.caffeine.cache.Cache<Object, Object> build(Duration ttl, long maxSize) {
        return Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(maxSize).build();
    }
}
