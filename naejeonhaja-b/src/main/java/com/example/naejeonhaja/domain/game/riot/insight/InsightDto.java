package com.example.naejeonhaja.domain.game.riot.insight;

import com.example.naejeonhaja.domain.game.lol.enums.LolLine;
import com.example.naejeonhaja.domain.game.riot.dto.MatchHistoryDto;
import com.example.naejeonhaja.domain.game.riot.dto.PlayerProfileDto;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * 소환사 검색 한 번으로 얻는 정보: 프로필 + 모스트 3 기반 추천 라인 + 대표 챔피언.
 *
 * @param lines          추천 라인 (모스트 3 챔피언의 포지션 = 주, 나머지 챔피언으로 2판 이상 간 포지션 = 부). 분석 전이면 null
 * @param linesPending   호출 한도 때문에 라인 분석을 아직 못 함 → retryAfter 초 뒤 같은 요청을 다시 보내면 됨
 * @param featured       카드 배경에 쓸 대표 챔피언 (모스트 1, 없으면 숙련도 1위)
 * @param recent         표본 경기 요약 (승률, KDA, 포지션 분포, 모스트 3)
 * @param cached         DB 캐시에서 바로 응답했는지
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record InsightDto(
        PlayerProfileDto profile,
        List<MatchHistoryDto.RecommendedLine> lines,
        boolean linesPending,
        Integer retryAfter,
        Featured featured,
        Recent recent,
        int sampleSize,
        boolean cached,
        OffsetDateTime analyzedAt) {

    public record Featured(int championId, String championKey, String championName,
                           String imageUrl, String splashUrl, String loadingUrl) {
    }

    /**
     * @param basis 표본 경기 종류: SOLO(이번 시즌 솔로랭크), RANKED(솔로랭크가 적어 자유랭크 포함), NORMAL(랭크가 없어 최근 일반 게임)
     * @param most3 모스트 3 챔피언과 각 챔피언으로 간 포지션
     * @param restLines 모스트 3 밖의 챔피언으로 간 포지션별 챔피언 (부 라인 판별 근거)
     */
    public record Recent(int games, int wins, double winRate, double kda,
                         List<MatchHistoryDto.PositionStat> positions,
                         List<MatchHistoryDto.ChampionStat> mostChampions,
                         String basis,
                         List<MostChampion> most3,
                         List<RestLine> restLines) {
    }

    /** @param sub 이 포지션이 부 라인이 됐는지 (2판 이상) */
    public record RestLine(LolLine line, String lineLabel, int games, boolean sub, List<RestChampion> champions) {
    }

    public record RestChampion(int championId, String championName, String imageUrl, int games, int wins) {
    }

    /** @param main 이 챔피언의 포지션이 주 라인에 들어갔는지 (모스트 2·3 이 1판뿐이면 false) */
    public record MostChampion(int championId, String championName, String imageUrl,
                               int games, int wins, double winRate, double kda,
                               LolLine line, String lineLabel, boolean main) {
    }
}
