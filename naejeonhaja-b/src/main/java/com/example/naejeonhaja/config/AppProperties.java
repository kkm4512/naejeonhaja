package com.example.naejeonhaja.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * application.yml 의 설정 묶음.
 */
public final class AppProperties {

    private AppProperties() {
    }

    /**
     * @param key             Riot API 키 (RIOT_API_KEY)
     * @param regionalBaseUrl account-v1, match-v5 (아시아 리전)
     * @param platformBaseUrl summoner-v4, league-v4, champion-mastery-v4 (한국 서버)
     * @param defaultTagLine  Riot ID 에 태그가 없을 때 붙일 기본 태그
     */
    @ConfigurationProperties(prefix = "riot.api")
    public record Riot(String key, String regionalBaseUrl, String platformBaseUrl, String defaultTagLine) {
        public Riot {
            if (regionalBaseUrl == null || regionalBaseUrl.isBlank()) regionalBaseUrl = "https://asia.api.riotgames.com";
            if (platformBaseUrl == null || platformBaseUrl.isBlank()) platformBaseUrl = "https://kr.api.riotgames.com";
            if (defaultTagLine == null || defaultTagLine.isBlank()) defaultTagLine = "KR1";
        }

        public boolean hasKey() {
            return key != null && !key.isBlank() && !key.contains("your-riot-api-key");
        }
    }

    /**
     * @param baseUrl         Data Dragon CDN 주소
     * @param locale          챔피언 이름 언어
     * @param fallbackVersion 버전 조회 실패 시 사용할 버전
     */
    @ConfigurationProperties(prefix = "ddragon")
    public record DataDragon(String baseUrl, String locale, String fallbackVersion) {
        public DataDragon {
            if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://ddragon.leagueoflegends.com";
            if (locale == null || locale.isBlank()) locale = "ko_KR";
            if (fallbackVersion == null || fallbackVersion.isBlank()) fallbackVersion = "16.21.1";
        }
    }

    /**
     * @param allowedOrigins CORS 허용 출처 (docker compose 에서는 프론트 서버가 프록시하므로 필요 없음)
     * @param adminToken     /dataDragon/reload 호출용 토큰. 비어 있으면 reload 비활성화
     */
    @ConfigurationProperties(prefix = "app")
    public record App(List<String> allowedOrigins, String adminToken) {
        public App {
            if (allowedOrigins == null || allowedOrigins.isEmpty()) {
                allowedOrigins = List.of("http://localhost:3000");
            }
        }
    }
}
