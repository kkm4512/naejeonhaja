package com.example.naejeonhaja.domain.game.dataDragon.dto;

/**
 * @param key        숫자 키 (match/mastery API 의 championId)
 * @param id         영문 id (예: "MonkeyKing")
 * @param name       한국어 이름 (예: "오공")
 * @param imageUrl   정사각형 챔피언 아이콘 URL
 * @param splashUrl  가로형 일러스트 (1215x717)
 * @param loadingUrl 세로형 로딩 화면 일러스트 (308x560)
 */
public record ChampionInfo(int key, String id, String name, String title, String imageUrl,
                           String splashUrl, String loadingUrl) {
}
