package com.example.naejeonhaja.domain.game.riot.insight;

import com.example.naejeonhaja.common.exception.RiotException;
import com.example.naejeonhaja.common.response.enums.BaseApiResponse;
import com.example.naejeonhaja.domain.game.dataDragon.dto.ChampionInfo;
import com.example.naejeonhaja.domain.game.dataDragon.service.DataDragonService;
import com.example.naejeonhaja.domain.game.riot.client.RiotGateway;
import com.example.naejeonhaja.domain.game.riot.dto.MatchHistoryDto;
import com.example.naejeonhaja.domain.game.riot.dto.PlayerProfileDto;
import com.example.naejeonhaja.domain.game.riot.service.RiotPlayerService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 소환사 검색 → 티어 + 추천 라인 + 대표 챔피언을 한 번에.
 *
 * <p>라인은 <b>이번 시즌 솔로랭크 최근 N판(기본 20)의 모스트 3 챔피언</b>으로 정한다 ({@link MostLineAnalyzer}).
 * 솔로랭크가 {@value #MIN_SOLO}판 미만이면 자유랭크를 더하고, 랭크가 하나도 없으면 최근 일반 게임으로 본다.
 * 결과는 summoner_insight 에 저장해 {@value #TTL_HOURS}시간 동안은 Riot 호출 없이 바로 돌려준다.</p>
 *
 * <p>호출 한도에 걸리면 프로필만 먼저 돌려주고 linesPending=true, retryAfter=N 을 알려준다.
 * 프론트는 N초 뒤 같은 요청을 다시 보내면 된다 (프로필은 캐시라 호출이 늘지 않음).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InsightService {

    // 1명당 호출: 계정 1 + 소환사 1 + 랭크 1 + 경기목록 1~3 + 경기 N
    // 개발용 키(2분에 100회)로 N=20 이면 처음 보는 10명에 4~5분. 한 번 본 사람은 24시간 저장
    static final int MIN_SOLO = 5;
    static final int TTL_HOURS = 24;
    static final int SOLO_QUEUE = 420;

    /** 라인 분석에 쓸 경기 수 (LOL_INSIGHT_SAMPLE) */
    @Value("${lol.insight.sample:20}")
    private int sample = 20;

    /** 이번 시즌 시작일 (LOL_SEASON_START, yyyy-MM-dd). 비우면 올해 1월 1일 */
    @Value("${lol.insight.season-start:}")
    private String seasonStart = "";

    private final RiotPlayerService playerService;
    private final RiotGateway gateway;
    private final DataDragonService dataDragon;
    private final SummonerInsightRepository repository;
    private final TransactionTemplate tx;
    private final ObjectMapper objectMapper;

    public InsightDto get(String riotId, boolean refresh) {
        PlayerProfileDto profile = playerService.getProfile(riotId, false);

        SummonerInsight saved = repository.findById(profile.puuid()).orElse(null);
        boolean fresh = saved != null && saved.getComputedAt().isAfter(OffsetDateTime.now().minus(Duration.ofHours(TTL_HOURS)));
        if (fresh && !refresh) {
            return fromSaved(profile, saved, true);
        }

        try {
            Sample s = sampleMatches(profile.puuid());
            MatchHistoryDto.Summary summary = RiotPlayerService.summarize(s.matches());
            MostLineAnalyzer.Result most = MostLineAnalyzer.analyze(s.matches());
            InsightDto.Recent recent = new InsightDto.Recent(summary.games(), summary.wins(), summary.winRate(),
                    summary.kda(), summary.positions(), summary.mostChampions(), s.basis(), most.most(), most.rest());
            SummonerInsight entity = tx.execute(status -> {
                SummonerInsight e = repository.findById(profile.puuid()).orElseGet(() -> new SummonerInsight(profile.puuid()));
                e.update(profile.riotId(), write(most.lines()), write(recent), summary.games());
                return repository.save(e);
            });
            return build(withMastery(profile, recent), most.lines(), recent, summary.games(), false, entity.getComputedAt(), false, null);
        } catch (RiotException e) {
            if (e.getResponseEnum() != BaseApiResponse.RIOT_RATE_LIMITED) throw e;
            // 한도에 걸림: 예전 분석이라도 있으면 그걸, 없으면 프로필만 먼저
            if (saved != null) return fromSaved(profile, saved, true);
            return build(profile, null, null, 0, false, null, true, e.getRetryAfterSeconds());
        }
    }

    private record Sample(List<MatchHistoryDto.Match> matches, String basis) {
    }

    /** 이번 시즌 솔로랭크 → (부족하면) 자유랭크 포함 → (랭크가 없으면) 최근 일반 게임 */
    private Sample sampleMatches(String puuid) {
        Long since = seasonStartEpoch();
        String basis = "SOLO";
        Set<String> ids = new LinkedHashSet<>(gateway.matchIds(puuid, sample, SOLO_QUEUE, null, since));
        if (ids.size() < MIN_SOLO) {
            ids.addAll(gateway.matchIds(puuid, sample, null, "ranked", since));
            basis = "RANKED";
        }
        if (ids.isEmpty()) {
            ids.addAll(gateway.matchIds(puuid, sample, null, "normal", null));
            basis = "NORMAL";
        }
        List<String> list = new ArrayList<>(ids);
        if (list.size() > sample) list = list.subList(0, sample);
        return new Sample(playerService.fetchMatches(puuid, list), basis);
    }

    Long seasonStartEpoch() {
        ZoneId kst = ZoneId.of("Asia/Seoul");
        LocalDate date;
        try {
            date = seasonStart == null || seasonStart.isBlank()
                    ? LocalDate.now(kst).withDayOfYear(1)
                    : LocalDate.parse(seasonStart.trim());
        } catch (Exception e) {
            log.warn("LOL_SEASON_START 형식이 잘못되어 올해 1월 1일로 봅니다: {}", seasonStart);
            date = LocalDate.now(kst).withDayOfYear(1);
        }
        return date.atStartOfDay(kst).toEpochSecond();
    }

    private InsightDto fromSaved(PlayerProfileDto profile, SummonerInsight saved, boolean cached) {
        List<MatchHistoryDto.RecommendedLine> lines = read(saved.getLinesJson(), new TypeReference<>() {});
        InsightDto.Recent recent = read(saved.getStatsJson(), new TypeReference<>() {});
        return build(withMastery(profile, recent), lines, recent, saved.getSampleSize(), cached, saved.getComputedAt(), false, null);
    }

    /** 표본 경기에서 대표 챔피언을 못 찾을 때만 숙련도 1회 호출 (실패해도 무시) */
    private PlayerProfileDto withMastery(PlayerProfileDto profile, InsightDto.Recent recent) {
        boolean hasRecentChamp = recent != null && recent.mostChampions() != null && !recent.mostChampions().isEmpty();
        if (hasRecentChamp) return profile;
        try {
            return playerService.getProfile(profile.riotId(), true);
        } catch (RiotException e) {
            return profile;
        }
    }

    private InsightDto build(PlayerProfileDto profile, List<MatchHistoryDto.RecommendedLine> lines,
                             InsightDto.Recent recent, int sample, boolean cached, OffsetDateTime at,
                             boolean pending, Integer retryAfter) {
        return new InsightDto(profile, lines, pending, retryAfter, featured(profile, recent), recent,
                sample, cached, at);
    }

    /** 모스트 1 → 없으면 숙련도 1위 */
    private InsightDto.Featured featured(PlayerProfileDto profile, InsightDto.Recent recent) {
        if (recent != null && recent.most3() != null && !recent.most3().isEmpty()) {
            ChampionInfo c = dataDragon.getChampion(recent.most3().get(0).championId());
            if (c != null) return of(c);
        }
        if (recent != null && recent.mostChampions() != null && !recent.mostChampions().isEmpty()) {
            ChampionInfo c = dataDragon.getChampion(recent.mostChampions().get(0).championId());
            if (c != null) return of(c);
        }
        if (profile.topChampions() != null && !profile.topChampions().isEmpty()) {
            ChampionInfo c = dataDragon.getChampion(profile.topChampions().get(0).championId());
            if (c != null) return of(c);
        }
        return null;
    }

    private static InsightDto.Featured of(ChampionInfo c) {
        return new InsightDto.Featured(c.key(), c.id(), c.name(), c.imageUrl(), c.splashUrl(), c.loadingUrl());
    }

    private String write(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "null";
        }
    }

    private <T> T read(String json, TypeReference<T> type) {
        try {
            return json == null ? null : objectMapper.readValue(json, type);
        } catch (Exception e) {
            log.warn("소환사 분석 캐시 읽기 실패: {}", e.getMessage());
            return null;
        }
    }
}
