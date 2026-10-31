package com.example.naejeonhaja.domain.game.riot.service;

import com.example.naejeonhaja.common.exception.BaseException;
import com.example.naejeonhaja.common.exception.RiotException;
import com.example.naejeonhaja.common.response.enums.BaseApiResponse;
import com.example.naejeonhaja.config.AppProperties;
import com.example.naejeonhaja.domain.game.dataDragon.dto.ChampionInfo;
import com.example.naejeonhaja.domain.game.dataDragon.service.DataDragonService;
import com.example.naejeonhaja.domain.game.lol.enums.LolLine;
import com.example.naejeonhaja.domain.game.lol.enums.LolLineRole;
import com.example.naejeonhaja.domain.game.lol.enums.LolTier;
import com.example.naejeonhaja.domain.game.riot.client.RiotGateway;
import com.example.naejeonhaja.domain.game.riot.dto.MatchHistoryDto;
import com.example.naejeonhaja.domain.game.riot.dto.PlayerProfileDto;
import com.example.naejeonhaja.domain.game.riot.dto.RiotApiDtos.Account;
import com.example.naejeonhaja.domain.game.riot.dto.RiotApiDtos.ChampionMastery;
import com.example.naejeonhaja.domain.game.riot.dto.RiotApiDtos.LeagueEntry;
import com.example.naejeonhaja.domain.game.riot.dto.RiotApiDtos.Summoner;
import com.example.naejeonhaja.domain.game.riot.enums.LolQueue;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Riot API 를 조합해 프로필과 최근 전적을 만든다.
 */
@Slf4j(topic = "RiotPlayerService")
@Service
public class RiotPlayerService {

    public static final int MAX_MATCH_COUNT = 20;
    private static final int MATCH_FETCH_PARALLELISM = 4;
    /** 시즌 승률 보정: 50% 기준 ±1%p 당 3점, 최대 ±50점, 20판 이상일 때만 */
    private static final int WIN_RATE_MIN_GAMES = 20;
    private static final int WIN_RATE_MAX_ADJUST = 50;
    /** 자유랭크는 솔로랭크보다 티어가 후하게 나오는 편이라 감점 */
    private static final int FLEX_PENALTY = 50;

    private final RiotGateway gateway;
    private final DataDragonService dataDragon;
    private final AppProperties.Riot props;
    private final ExecutorService matchExecutor = Executors.newFixedThreadPool(MATCH_FETCH_PARALLELISM);

    public RiotPlayerService(RiotGateway gateway, DataDragonService dataDragon, AppProperties.Riot props) {
        this.gateway = gateway;
        this.dataDragon = dataDragon;
        this.props = props;
    }

    @PreDestroy
    void shutdown() {
        matchExecutor.shutdownNow();
    }

    // ================================================================ 프로필

    public PlayerProfileDto getProfile(String rawRiotId, boolean includeMastery) {
        Account account = findAccount(rawRiotId);
        Summoner summoner = gateway.summoner(account.puuid());
        List<LeagueEntry> leagues = gateway.leagues(account.puuid());

        PlayerProfileDto.RankDto solo = toRank(leagues, "RANKED_SOLO_5x5");
        PlayerProfileDto.RankDto flex = toRank(leagues, "RANKED_FLEX_SR");

        List<PlayerProfileDto.MasteryDto> masteries = null;
        if (includeMastery) {
            masteries = gateway.topMasteries(account.puuid(), 3).stream()
                    .map(this::toMastery)
                    .toList();
        }

        SuggestedMmr suggested = suggestMmr(solo, flex);
        return new PlayerProfileDto(
                account.puuid(),
                account.gameName(),
                account.tagLine(),
                account.gameName() + "#" + account.tagLine(),
                summoner.summonerLevel(),
                summoner.profileIconId(),
                dataDragon.profileIconUrl(summoner.profileIconId()),
                solo,
                flex,
                masteries,
                suggested.tier(),
                suggested.lp(),
                suggested.mmr(),
                suggested.source(),
                suggested.note());
    }

    record SuggestedMmr(LolTier tier, Integer lp, int mmr, String source, String note) {
    }

    static SuggestedMmr suggestMmr(PlayerProfileDto.RankDto solo, PlayerProfileDto.RankDto flex) {
        PlayerProfileDto.RankDto rank = solo != null ? solo : flex;
        if (rank == null || rank.tierKey() == LolTier.UNRANKED) {
            return new SuggestedMmr(LolTier.UNRANKED, null, LolTier.UNRANKED_SCORE, "UNRANKED",
                    "랭크 기록이 없어 기본값(골드 IV 수준)을 사용합니다. 실제 실력에 맞게 수정해주세요.");
        }
        int base = rank.tierKey().scoreWithLp(rank.leaguePoints());
        StringBuilder note = new StringBuilder(solo != null ? "솔로랭크 " : "자유랭크 ")
                .append(rank.tier()).append(rank.tierKey().isApexTier() ? "" : " " + rank.rank())
                .append(" ").append(rank.leaguePoints()).append("LP = ").append(base);

        int adjust = 0;
        int games = rank.wins() + rank.losses();
        if (games >= WIN_RATE_MIN_GAMES) {
            adjust = (int) Math.round((rank.winRate() - 50.0) * 3);
            adjust = Math.max(-WIN_RATE_MAX_ADJUST, Math.min(WIN_RATE_MAX_ADJUST, adjust));
            if (adjust != 0) {
                note.append(String.format(", 승률 %.0f%% %s%d", rank.winRate(), adjust > 0 ? "+" : "", adjust));
            }
        }
        int mmr = base + adjust;
        if (solo == null) {
            mmr -= FLEX_PENALTY;
            note.append(", 자랭 보정 -").append(FLEX_PENALTY);
        }
        return new SuggestedMmr(rank.tierKey(), rank.leaguePoints(), Math.max(1, mmr),
                solo != null ? "SOLO" : "FLEX", note.toString());
    }

    // ================================================================ 전적

    public MatchHistoryDto getMatchHistory(String rawRiotId, int count, Integer queue) {
        int size = Math.max(1, Math.min(count, MAX_MATCH_COUNT));
        Account account = findAccount(rawRiotId);
        List<String> ids = gateway.matchIds(account.puuid(), size, queue, null, null);

        List<MatchHistoryDto.Match> matches = fetchMatches(account.puuid(), ids);
        return new MatchHistoryDto(account.puuid(), account.gameName() + "#" + account.tagLine(),
                summarize(matches), matches);
    }

    /** 경기 상세를 병렬로 받아 최신순으로 정리 (호출 한도는 RiotApiClient 가 관리) */
    public List<MatchHistoryDto.Match> fetchMatches(String puuid, List<String> ids) {
        List<CompletableFuture<MatchHistoryDto.Match>> futures = ids.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> fetchMatch(id, puuid), matchExecutor))
                .toList();

        List<MatchHistoryDto.Match> matches = new ArrayList<>();
        for (CompletableFuture<MatchHistoryDto.Match> f : futures) {
            try {
                MatchHistoryDto.Match m = f.join();
                if (m != null) matches.add(m);
            } catch (CompletionException e) {
                if (e.getCause() instanceof BaseException be) throw be;
                throw new RiotException(BaseApiResponse.RIOT_API_ERROR);
            }
        }
        matches.sort(Comparator.comparingLong(MatchHistoryDto.Match::gameStartTimestamp).reversed());
        return matches;
    }

    private MatchHistoryDto.Match fetchMatch(String matchId, String puuid) {
        try {
            return toMatch(matchId, gateway.match(matchId), puuid);
        } catch (RiotException e) {
            if (e.getResponseEnum() == BaseApiResponse.RIOT_PLAYER_NOT_FOUND) {
                log.warn("경기 정보를 찾을 수 없어 건너뜀: {}", matchId);
                return null;
            }
            throw e;
        }
    }

    MatchHistoryDto.Match toMatch(String matchId, JsonNode root, String puuid) {
        JsonNode info = root.path("info");
        JsonNode me = null;
        for (JsonNode p : info.path("participants")) {
            if (puuid.equals(p.path("puuid").asText())) {
                me = p;
                break;
            }
        }
        if (me == null) return null;

        int teamId = me.path("teamId").asInt();
        int teamKills = 0;
        for (JsonNode p : info.path("participants")) {
            if (p.path("teamId").asInt() == teamId) teamKills += p.path("kills").asInt();
        }

        long duration = info.path("gameDuration").asLong();
        if (!info.has("gameEndTimestamp") && duration > 100_000) {
            duration = duration / 1000; // 11.20 패치 이전 경기는 ms 단위
        }
        int kills = me.path("kills").asInt();
        int deaths = me.path("deaths").asInt();
        int assists = me.path("assists").asInt();
        int cs = me.path("totalMinionsKilled").asInt() + me.path("neutralMinionsKilled").asInt();
        boolean remake = me.path("gameEndedInEarlySurrender").asBoolean(false);
        int queueId = info.path("queueId").asInt();

        ChampionInfo champ = dataDragon.resolveChampion(me.path("championId").asInt(), me.path("championName").asText(null));

        List<String> items = new ArrayList<>();
        for (int i = 0; i <= 6; i++) {
            items.add(dataDragon.itemImageUrl(me.path("item" + i).asInt()));
        }
        List<String> spells = new ArrayList<>();
        spells.add(dataDragon.spellImageUrl(me.path("summoner1Id").asInt()));
        spells.add(dataDragon.spellImageUrl(me.path("summoner2Id").asInt()));

        List<String> runes = new ArrayList<>();
        JsonNode styles = me.path("perks").path("styles");
        if (styles.isArray() && !styles.isEmpty()) {
            runes.add(dataDragon.runeIconUrl(styles.get(0).path("selections").path(0).path("perk").asInt()));
            if (styles.size() > 1) runes.add(dataDragon.runeIconUrl(styles.get(1).path("style").asInt()));
        }

        LolLine position = LolQueue.RIFT_QUEUES.contains(queueId)
                ? LolLine.fromRiotPosition(me.path("teamPosition").asText(null))
                : null;

        return new MatchHistoryDto.Match(
                matchId, queueId, LolQueue.labelOf(queueId),
                info.path("gameStartTimestamp").asLong(info.path("gameCreation").asLong()),
                duration,
                me.path("win").asBoolean(), remake,
                me.path("championId").asInt(),
                champ != null ? champ.name() : me.path("championName").asText(),
                champ != null ? champ.imageUrl() : null,
                me.path("champLevel").asInt(),
                position,
                kills, deaths, assists,
                kda(kills, deaths, assists),
                teamKills == 0 ? 0 : Math.min(100, round1((kills + assists) * 100.0 / teamKills)),
                cs,
                duration == 0 ? 0 : round1(cs / (duration / 60.0)),
                me.path("goldEarned").asInt(),
                me.path("totalDamageDealtToChampions").asInt(),
                me.path("visionScore").asInt(),
                multiKillLabel(me),
                items, spells, runes);
    }

    public static MatchHistoryDto.Summary summarize(List<MatchHistoryDto.Match> all) {
        List<MatchHistoryDto.Match> games = all.stream().filter(m -> !m.remake()).toList();
        int n = games.size();
        if (n == 0) {
            return new MatchHistoryDto.Summary(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, List.of(), List.of(), List.of());
        }
        int wins = (int) games.stream().filter(MatchHistoryDto.Match::win).count();
        int k = games.stream().mapToInt(MatchHistoryDto.Match::kills).sum();
        int d = games.stream().mapToInt(MatchHistoryDto.Match::deaths).sum();
        int a = games.stream().mapToInt(MatchHistoryDto.Match::assists).sum();
        double csm = games.stream().mapToDouble(MatchHistoryDto.Match::csPerMin).average().orElse(0);
        double kp = games.stream().mapToDouble(MatchHistoryDto.Match::killParticipation).average().orElse(0);

        // 포지션 통계 (협곡 경기만)
        Map<LolLine, Integer> posCount = new EnumMap<>(LolLine.class);
        games.stream().map(MatchHistoryDto.Match::position).filter(Objects::nonNull)
                .forEach(l -> posCount.merge(l, 1, Integer::sum));
        int posTotal = posCount.values().stream().mapToInt(Integer::intValue).sum();
        List<MatchHistoryDto.PositionStat> positions = posCount.entrySet().stream()
                .sorted(Map.Entry.<LolLine, Integer>comparingByValue().reversed())
                .map(e -> new MatchHistoryDto.PositionStat(e.getKey(), e.getKey().getLabel(), e.getValue(),
                        round1(e.getValue() * 100.0 / posTotal)))
                .toList();

        List<MatchHistoryDto.RecommendedLine> recommended = new ArrayList<>();
        for (int i = 0; i < positions.size() && recommended.size() < 3; i++) {
            MatchHistoryDto.PositionStat p = positions.get(i);
            if (i == 0) {
                recommended.add(new MatchHistoryDto.RecommendedLine(p.line(), LolLineRole.MAINLINE));
            } else if (p.games() >= 2 && p.ratio() >= 20) {
                recommended.add(new MatchHistoryDto.RecommendedLine(p.line(), LolLineRole.SUBLINE));
            }
        }

        // 모스트 챔피언
        Map<Integer, List<MatchHistoryDto.Match>> byChamp = new LinkedHashMap<>();
        games.forEach(m -> byChamp.computeIfAbsent(m.championId(), x -> new ArrayList<>()).add(m));
        List<MatchHistoryDto.ChampionStat> most = byChamp.values().stream()
                .map(list -> {
                    MatchHistoryDto.Match first = list.get(0);
                    int w = (int) list.stream().filter(MatchHistoryDto.Match::win).count();
                    int ck = list.stream().mapToInt(MatchHistoryDto.Match::kills).sum();
                    int cd = list.stream().mapToInt(MatchHistoryDto.Match::deaths).sum();
                    int ca = list.stream().mapToInt(MatchHistoryDto.Match::assists).sum();
                    return new MatchHistoryDto.ChampionStat(first.championId(), first.championName(),
                            first.championImageUrl(), list.size(), w, round1(w * 100.0 / list.size()), kda(ck, cd, ca));
                })
                .sorted(Comparator.comparingInt(MatchHistoryDto.ChampionStat::games).reversed()
                        .thenComparing(Comparator.comparingDouble(MatchHistoryDto.ChampionStat::winRate).reversed()))
                .limit(3)
                .toList();

        return new MatchHistoryDto.Summary(n, wins, n - wins, round1(wins * 100.0 / n),
                round1((double) k / n), round1((double) d / n), round1((double) a / n), kda(k, d, a),
                round1(csm), round1(kp), positions, recommended, most);
    }

    // ================================================================ helpers

    private Account findAccount(String rawRiotId) {
        RiotIdParser.RiotId id = RiotIdParser.parse(rawRiotId, props.defaultTagLine());
        try {
            return gateway.account(id.gameName(), id.tagLine());
        } catch (RiotException e) {
            if (e.getResponseEnum() == BaseApiResponse.RIOT_PLAYER_NOT_FOUND) {
                throw new RiotException(BaseApiResponse.RIOT_PLAYER_NOT_FOUND,
                        "소환사를 찾을 수 없습니다: " + id.display());
            }
            throw e;
        }
    }

    private static PlayerProfileDto.RankDto toRank(List<LeagueEntry> leagues, String queueType) {
        if (leagues == null) return null;
        return leagues.stream()
                .filter(l -> queueType.equals(l.queueType()))
                .findFirst()
                .map(l -> {
                    int games = l.wins() + l.losses();
                    double wr = games == 0 ? 0 : round1(l.wins() * 100.0 / games);
                    return new PlayerProfileDto.RankDto(l.queueType(), l.tier(), l.rank(),
                            LolTier.fromRiot(l.tier(), l.rank()), l.leaguePoints(), l.wins(), l.losses(), wr, l.hotStreak());
                })
                .orElse(null);
    }

    private PlayerProfileDto.MasteryDto toMastery(ChampionMastery m) {
        ChampionInfo c = dataDragon.getChampion(m.championId());
        return new PlayerProfileDto.MasteryDto(m.championId(),
                c != null ? c.id() : null,
                c != null ? c.name() : "챔피언 " + m.championId(),
                c != null ? c.imageUrl() : null,
                c != null ? c.splashUrl() : null,
                c != null ? c.loadingUrl() : null,
                m.championLevel(), m.championPoints());
    }

    private static String multiKillLabel(JsonNode me) {
        if (me.path("pentaKills").asInt() > 0) return "펜타킬";
        if (me.path("quadraKills").asInt() > 0) return "쿼드라킬";
        if (me.path("tripleKills").asInt() > 0) return "트리플킬";
        if (me.path("doubleKills").asInt() > 0) return "더블킬";
        return null;
    }

    static double kda(int k, int d, int a) {
        return d == 0 ? round1(k + a) : round1((k + a) / (double) d);
    }

    private static double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }
}
