package com.example.naejeonhaja.domain.game.dataDragon.service;

import com.example.naejeonhaja.config.AppProperties;
import com.example.naejeonhaja.domain.game.dataDragon.dto.ChampionInfo;
import com.example.naejeonhaja.domain.game.dataDragon.dto.DataDragonMetaDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Riot Data Dragon(정적 데이터 CDN)에서 최신 버전, 챔피언, 소환사 주문, 룬 정보를 받아 메모리에 보관하고
 * 각종 이미지 URL 을 만들어 준다. 6시간마다 새 패치를 확인한다.
 */
@Slf4j(topic = "DataDragonService")
@Service
public class DataDragonService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties.DataDragon props;

    private volatile Snapshot snapshot;

    public DataDragonService(RestTemplate restTemplate, ObjectMapper objectMapper, AppProperties.DataDragon props) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.props = props;
        this.snapshot = Snapshot.empty(props.fallbackVersion());
    }

    private record Snapshot(String version,
                            Map<Integer, ChampionInfo> championsByKey,
                            Map<String, ChampionInfo> championsById,
                            Map<Integer, String> spellImages,
                            Map<Integer, String> runeIcons,
                            boolean loaded) {
        static Snapshot empty(String version) {
            return new Snapshot(version, Map.of(), Map.of(), Map.of(), Map.of(), false);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        try {
            reload();
        } catch (Exception e) {
            log.warn("Data Dragon 초기 로드 실패 (요청 시 다시 시도): {}", e.getMessage());
        }
    }

    @Scheduled(initialDelayString = "PT6H", fixedDelayString = "PT6H")
    public void scheduledRefresh() {
        try {
            String latest = fetchLatestVersion();
            if (!latest.equals(snapshot.version()) || !snapshot.loaded()) {
                reload();
            }
        } catch (Exception e) {
            log.warn("Data Dragon 갱신 실패: {}", e.getMessage());
        }
    }

    /** 최신 버전으로 챔피언/주문/룬 데이터를 다시 읽는다. */
    public synchronized DataDragonMetaDto reload() {
        String version = fetchLatestVersion();
        String base = props.baseUrl() + "/cdn/" + version + "/data/" + props.locale();

        Map<Integer, ChampionInfo> byKey = new HashMap<>();
        Map<String, ChampionInfo> byId = new HashMap<>();
        JsonNode champions = fetch(base + "/champion.json").path("data");
        champions.forEach(c -> {
            String champId = c.path("id").asText();
            ChampionInfo info = new ChampionInfo(
                    c.path("key").asInt(),
                    champId,
                    c.path("name").asText(),
                    c.path("title").asText(),
                    championImageUrl(version, c.path("image").path("full").asText()),
                    splashUrl(champId),
                    loadingUrl(champId));
            byKey.put(info.key(), info);
            byId.put(info.id(), info);
        });

        Map<Integer, String> spells = new HashMap<>();
        fetch(base + "/summoner.json").path("data").forEach(s ->
                spells.put(s.path("key").asInt(),
                        props.baseUrl() + "/cdn/" + version + "/img/spell/" + s.path("image").path("full").asText()));

        Map<Integer, String> runes = new HashMap<>();
        fetch(base + "/runesReforged.json").forEach(style -> {
            runes.put(style.path("id").asInt(), props.baseUrl() + "/cdn/img/" + style.path("icon").asText());
            style.path("slots").forEach(slot -> slot.path("runes").forEach(rune ->
                    runes.put(rune.path("id").asInt(), props.baseUrl() + "/cdn/img/" + rune.path("icon").asText())));
        });

        snapshot = new Snapshot(version, Map.copyOf(byKey), Map.copyOf(byId), Map.copyOf(spells), Map.copyOf(runes), true);
        log.info("Data Dragon 로드 완료 (버전 {}, 챔피언 {}개)", version, byKey.size());
        return meta();
    }

    // ----------------------------------------------------------------- 조회

    public DataDragonMetaDto meta() {
        Snapshot s = current();
        return new DataDragonMetaDto(s.version(), props.baseUrl() + "/cdn/" + s.version(), s.championsByKey().size(), s.loaded());
    }

    public String getVersion() {
        return current().version();
    }

    public ChampionInfo getChampion(int championKey) {
        return current().championsByKey().get(championKey);
    }

    public ChampionInfo getChampionById(String championId) {
        return current().championsById().get(championId);
    }

    /** 챔피언 key(숫자)로 찾고, 없으면 match 데이터의 championName(영문 id)으로 URL 을 만든다. */
    public ChampionInfo resolveChampion(int championKey, String championIdFallback) {
        ChampionInfo info = getChampion(championKey);
        if (info != null) return info;
        if (championIdFallback != null && !championIdFallback.isBlank()) {
            ChampionInfo byId = getChampionById(championIdFallback);
            if (byId != null) return byId;
            return new ChampionInfo(championKey, championIdFallback, championIdFallback, "",
                    championImageUrl(getVersion(), championIdFallback + ".png"),
                    splashUrl(championIdFallback), loadingUrl(championIdFallback));
        }
        return null;
    }

    public String profileIconUrl(int profileIconId) {
        return props.baseUrl() + "/cdn/" + getVersion() + "/img/profileicon/" + profileIconId + ".png";
    }

    public String itemImageUrl(int itemId) {
        if (itemId <= 0) return null;
        return props.baseUrl() + "/cdn/" + getVersion() + "/img/item/" + itemId + ".png";
    }

    public String spellImageUrl(int spellKey) {
        return current().spellImages().get(spellKey);
    }

    public String runeIconUrl(int perkOrStyleId) {
        return current().runeIcons().get(perkOrStyleId);
    }

    // ----------------------------------------------------------------- internals

    private volatile long lastLazyAttempt = 0;

    private Snapshot current() {
        // 로드 실패 상태라면 최대 1분에 한 번만 다시 시도 (매 요청마다 타임아웃 대기하지 않도록)
        if (!snapshot.loaded() && System.currentTimeMillis() - lastLazyAttempt > 60_000) {
            synchronized (this) {
                if (!snapshot.loaded() && System.currentTimeMillis() - lastLazyAttempt > 60_000) {
                    lastLazyAttempt = System.currentTimeMillis();
                    try {
                        reload();
                    } catch (Exception e) {
                        log.warn("Data Dragon 지연 로드 실패: {}", e.getMessage());
                    }
                }
            }
        }
        return snapshot;
    }

    public String splashUrl(String championId) {
        return props.baseUrl() + "/cdn/img/champion/splash/" + championId + "_0.jpg";
    }

    public String loadingUrl(String championId) {
        return props.baseUrl() + "/cdn/img/champion/loading/" + championId + "_0.jpg";
    }

    private String championImageUrl(String version, String file) {
        return props.baseUrl() + "/cdn/" + version + "/img/champion/" + file;
    }

    private String fetchLatestVersion() {
        try {
            JsonNode versions = fetch(props.baseUrl() + "/api/versions.json");
            if (versions.isArray() && !versions.isEmpty()) {
                return versions.get(0).asText();
            }
        } catch (Exception e) {
            log.warn("Data Dragon 버전 조회 실패, {} 사용: {}", props.fallbackVersion(), e.getMessage());
        }
        return props.fallbackVersion();
    }

    private JsonNode fetch(String url) {
        try {
            String body = restTemplate.getForObject(url, String.class);
            return objectMapper.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("Data Dragon 호출 실패: " + url + " (" + e.getMessage() + ")", e);
        }
    }
}
