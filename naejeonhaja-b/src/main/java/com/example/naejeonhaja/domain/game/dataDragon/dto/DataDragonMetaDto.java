package com.example.naejeonhaja.domain.game.dataDragon.dto;

/**
 * @param version       현재 사용 중인 Data Dragon 버전 (예: 16.21.1)
 * @param cdnBaseUrl    이미지 URL 접두어 (예: https://ddragon.leagueoflegends.com/cdn/16.21.1)
 * @param championCount 로드된 챔피언 수
 * @param loaded        정상 로드 여부 (false 면 대체 버전으로 동작 중)
 */
public record DataDragonMetaDto(String version, String cdnBaseUrl, int championCount, boolean loaded) {
}
