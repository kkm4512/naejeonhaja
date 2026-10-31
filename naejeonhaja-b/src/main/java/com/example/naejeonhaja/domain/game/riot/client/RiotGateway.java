package com.example.naejeonhaja.domain.game.riot.client;

import com.example.naejeonhaja.config.AppProperties;
import com.example.naejeonhaja.config.CacheConfig;
import com.example.naejeonhaja.domain.game.riot.dto.RiotApiDtos.Account;
import com.example.naejeonhaja.domain.game.riot.dto.RiotApiDtos.ChampionMastery;
import com.example.naejeonhaja.domain.game.riot.dto.RiotApiDtos.LeagueEntry;
import com.example.naejeonhaja.domain.game.riot.dto.RiotApiDtos.Summoner;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Riot API 엔드포인트별 호출 + 캐시. (캐시 프록시가 동작하도록 조합 로직과 분리)
 */
@Component
@RequiredArgsConstructor
public class RiotGateway {

    private final RiotApiClient client;
    private final AppProperties.Riot props;

    @Cacheable(cacheNames = CacheConfig.ACCOUNT, key = "#gameName.toLowerCase() + '#' + #tagLine.toLowerCase()")
    public Account account(String gameName, String tagLine) {
        URI uri = UriComponentsBuilder.fromUriString(props.regionalBaseUrl())
                .path("/riot/account/v1/accounts/by-riot-id/{gameName}/{tagLine}")
                .buildAndExpand(gameName, tagLine).encode().toUri();
        return client.get(uri, new TypeReference<>() {});
    }

    @Cacheable(cacheNames = CacheConfig.SUMMONER)
    public Summoner summoner(String puuid) {
        URI uri = UriComponentsBuilder.fromUriString(props.platformBaseUrl())
                .path("/lol/summoner/v4/summoners/by-puuid/{puuid}")
                .buildAndExpand(puuid).encode().toUri();
        return client.get(uri, new TypeReference<>() {});
    }

    @Cacheable(cacheNames = CacheConfig.LEAGUE)
    public List<LeagueEntry> leagues(String puuid) {
        URI uri = UriComponentsBuilder.fromUriString(props.platformBaseUrl())
                .path("/lol/league/v4/entries/by-puuid/{puuid}")
                .buildAndExpand(puuid).encode().toUri();
        return client.get(uri, new TypeReference<>() {});
    }

    @Cacheable(cacheNames = CacheConfig.MASTERY, key = "#puuid + ':' + #count")
    public List<ChampionMastery> topMasteries(String puuid, int count) {
        URI uri = UriComponentsBuilder.fromUriString(props.platformBaseUrl())
                .path("/lol/champion-mastery/v4/champion-masteries/by-puuid/{puuid}/top")
                .queryParam("count", count)
                .buildAndExpand(puuid).encode().toUri();
        return client.get(uri, new TypeReference<>() {});
    }

    /**
     * @param queue 큐 번호 필터 (420 솔랭 등), null 이면 전체
     * @param type  ranked / normal 등 경기 종류 필터, null 이면 전체
     * @param startTime 이 시각(epoch 초) 이후 경기만, null 이면 기간 제한 없음
     */
    @Cacheable(cacheNames = CacheConfig.MATCH_IDS, key = "#puuid + ':' + #count + ':' + #queue + ':' + #type + ':' + #startTime")
    public List<String> matchIds(String puuid, int count, Integer queue, String type, Long startTime) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(props.regionalBaseUrl())
                .path("/lol/match/v5/matches/by-puuid/{puuid}/ids")
                .queryParam("start", 0)
                .queryParam("count", count);
        if (queue != null) {
            builder.queryParam("queue", queue);
        }
        if (type != null) {
            builder.queryParam("type", type);
        }
        if (startTime != null) {
            builder.queryParam("startTime", startTime);
        }
        return client.get(builder.buildAndExpand(puuid).encode().toUri(), new TypeReference<>() {});
    }

    @Cacheable(cacheNames = CacheConfig.MATCH)
    public JsonNode match(String matchId) {
        URI uri = UriComponentsBuilder.fromUriString(props.regionalBaseUrl())
                .path("/lol/match/v5/matches/{matchId}")
                .buildAndExpand(matchId).encode().toUri();
        return client.get(uri, new TypeReference<>() {});
    }
}
