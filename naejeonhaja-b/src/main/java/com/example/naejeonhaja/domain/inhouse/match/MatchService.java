package com.example.naejeonhaja.domain.inhouse.match;

import com.example.naejeonhaja.common.exception.BaseException;
import com.example.naejeonhaja.common.response.enums.BaseApiResponse;
import com.example.naejeonhaja.domain.game.dataDragon.service.DataDragonService;
import com.example.naejeonhaja.domain.game.lol.enums.LolBalanceEnum;
import com.example.naejeonhaja.domain.inhouse.group.InhouseGroup;
import com.example.naejeonhaja.domain.inhouse.member.Member;
import com.example.naejeonhaja.domain.inhouse.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.*;

@Service
@RequiredArgsConstructor
public class MatchService {

    private static final String ID_CHARS = "abcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final InhouseMatchRepository matchRepository;
    private final MemberRepository memberRepository;
    private final DataDragonService dataDragon;

    @Transactional
    public MatchDtos.MatchOut save(InhouseGroup group, MatchDtos.SaveRequest req) {
        validateUnique(req);
        int blueTotal = req.blue().stream().mapToInt(MatchDtos.PlayerIn::mmr).sum();
        int redTotal = req.red().stream().mapToInt(MatchDtos.PlayerIn::mmr).sum();
        LolBalanceEnum balance = LolBalanceEnum.fromDifference(blueTotal - redTotal);

        InhouseMatch match = new InhouseMatch(newPublicId(), group, req.mode(), blueTotal, redTotal, balance.name(),
                blankToNull(req.memo()));
        addPlayers(group, match, req.blue(), InhouseMatch.Team.BLUE);
        addPlayers(group, match, req.red(), InhouseMatch.Team.RED);
        return toOut(matchRepository.save(match));
    }

    @Transactional(readOnly = true)
    public MatchDtos.PageOut list(InhouseGroup group, int page, int size) {
        Page<InhouseMatch> result = matchRepository.findByGroupIdOrderByCreatedAtDesc(group.getId(),
                PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50)));
        return new MatchDtos.PageOut(result.getContent().stream().map(this::toOut).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(), result.hasNext());
    }

    @Transactional(readOnly = true)
    public MatchDtos.MatchOut get(InhouseGroup group, Long id) {
        return toOut(find(group, id));
    }

    @Transactional(readOnly = true)
    public MatchDtos.MatchOut getPublic(String publicId) {
        return toOut(matchRepository.findByPublicId(publicId)
                .orElseThrow(() -> new BaseException(BaseApiResponse.MATCH_NOT_FOUND)));
    }

    /**
     * 승패 기록/변경/취소. 이전에 기록된 결과가 있으면 그 보정치를 먼저 되돌린 뒤 새 결과를 반영한다.
     */
    @Transactional
    public MatchDtos.MatchOut decide(InhouseGroup group, Long id, InhouseMatch.Team winner) {
        InhouseMatch match = find(group, id);
        if (match.getWinner() == winner) {
            return toOut(match);
        }
        revert(match);
        match.decide(winner);
        if (winner != null) {
            int[] d = InhouseRating.deltas(match.getBlueTotal(), match.getRedTotal(), winner);
            for (InhouseMatchPlayer p : match.getPlayers()) {
                int delta = p.getTeam() == InhouseMatch.Team.BLUE ? d[0] : d[1];
                p.setRatingDelta(p.getMember() == null ? 0 : delta);
                if (p.getMember() != null) {
                    p.getMember().applyResult(delta, p.getTeam() == winner, +1);
                }
            }
        }
        return toOut(match);
    }

    @Transactional
    public MatchDtos.MatchOut updateMemo(InhouseGroup group, Long id, String memo) {
        InhouseMatch match = find(group, id);
        match.updateMemo(blankToNull(memo));
        return toOut(match);
    }

    @Transactional
    public void delete(InhouseGroup group, Long id) {
        InhouseMatch match = find(group, id);
        revert(match);
        matchRepository.delete(match);
    }

    /** 순위표: 내전을 한 판이라도 한 멤버, 승률 → 승수 → 내전 보정치 순 */
    @Transactional(readOnly = true)
    public List<MatchDtos.StandingRow> standings(InhouseGroup group) {
        List<Member> members = memberRepository.findByGroupIdOrderByNameAsc(group.getId());
        Map<Long, List<String>> forms = new HashMap<>();
        List<InhouseMatch> recent = matchRepository.findByGroupIdOrderByCreatedAtDesc(group.getId(), PageRequest.of(0, 200)).getContent();
        for (InhouseMatch m : recent) {
            if (m.getWinner() == null) continue;
            for (InhouseMatchPlayer p : m.getPlayers()) {
                if (p.getMember() == null) continue;
                forms.computeIfAbsent(p.getMember().getId(), k -> new ArrayList<>()).add(p.getTeam() == m.getWinner() ? "W" : "L");
            }
        }
        return members.stream()
                .filter(m -> m.getWins() + m.getLosses() > 0)
                .map(m -> {
                    List<String> form = forms.getOrDefault(m.getId(), List.of());
                    int streak = 0;
                    if (!form.isEmpty()) {
                        String first = form.get(0);
                        for (String f : form) {
                            if (!f.equals(first)) break;
                            streak += first.equals("W") ? 1 : -1;
                        }
                    }
                    int games = m.getWins() + m.getLosses();
                    return new MatchDtos.StandingRow(m.getId(), m.getName(),
                            m.getProfileIconId() == null ? null : dataDragon.profileIconUrl(m.getProfileIconId()),
                            m.getTier(), m.getWins(), m.getLosses(), Math.round(m.getWins() * 1000.0 / games) / 10.0,
                            m.getInhouseDelta(), m.effectiveMmr(), form.subList(0, Math.min(5, form.size())), streak);
                })
                .sorted(Comparator.comparingDouble(MatchDtos.StandingRow::winRate).reversed()
                        .thenComparing(Comparator.comparingInt(MatchDtos.StandingRow::wins).reversed())
                        .thenComparing(Comparator.comparingInt(MatchDtos.StandingRow::inhouseDelta).reversed()))
                .toList();
    }

    // ------------------------------------------------------------------ helpers

    private void revert(InhouseMatch match) {
        if (match.getWinner() == null) return;
        for (InhouseMatchPlayer p : match.getPlayers()) {
            if (p.getMember() != null) {
                p.getMember().applyResult(-p.getRatingDelta(), p.getTeam() == match.getWinner(), -1);
            }
            p.setRatingDelta(0);
        }
        match.decide(null);
    }

    private void addPlayers(InhouseGroup group, InhouseMatch match, List<MatchDtos.PlayerIn> players, InhouseMatch.Team team) {
        for (int i = 0; i < players.size(); i++) {
            MatchDtos.PlayerIn p = players.get(i);
            Member member = resolveMember(group, p);
            match.addPlayer(new InhouseMatchPlayer(member, i, team, p.name().trim(), p.tier(), p.line(), p.lineRole(),
                    p.baseMmr(), p.mmr()));
        }
    }

    /** memberId 가 오면 그 멤버, 아니면 같은 이름의 단골 멤버와 자동으로 연결 */
    private Member resolveMember(InhouseGroup group, MatchDtos.PlayerIn p) {
        if (p.memberId() != null) {
            return memberRepository.findByIdAndGroupId(p.memberId(), group.getId()).orElse(null);
        }
        return memberRepository.findByGroupIdAndNameKey(group.getId(), Member.keyOf(p.name())).orElse(null);
    }

    private void validateUnique(MatchDtos.SaveRequest req) {
        Set<String> names = new HashSet<>();
        for (MatchDtos.PlayerIn p : concat(req.blue(), req.red())) {
            if (!names.add(Member.keyOf(p.name()))) {
                throw new BaseException(BaseApiResponse.DUPLICATE_PLAYER, "같은 이름의 플레이어가 두 번 들어있어요: " + p.name());
            }
        }
    }

    private InhouseMatch find(InhouseGroup group, Long id) {
        return matchRepository.findByIdAndGroupId(id, group.getId())
                .orElseThrow(() -> new BaseException(BaseApiResponse.MATCH_NOT_FOUND));
    }

    private MatchDtos.MatchOut toOut(InhouseMatch m) {
        List<MatchDtos.PlayerOut> blue = new ArrayList<>();
        List<MatchDtos.PlayerOut> red = new ArrayList<>();
        m.getPlayers().stream()
                .sorted(Comparator.comparing(InhouseMatchPlayer::getTeam).thenComparingInt(InhouseMatchPlayer::getSlot))
                .forEach(p -> {
                    Integer icon = p.getMember() == null ? null : p.getMember().getProfileIconId();
                    MatchDtos.PlayerOut out = new MatchDtos.PlayerOut(
                            p.getMember() == null ? null : p.getMember().getId(), p.getName(), p.getTier(),
                            p.getLine(), p.getLineRole(), p.getBaseMmr(), p.getMmr(), p.getRatingDelta(),
                            icon == null ? null : dataDragon.profileIconUrl(icon));
                    (p.getTeam() == InhouseMatch.Team.BLUE ? blue : red).add(out);
                });
        LolBalanceEnum balance = LolBalanceEnum.valueOf(m.getBalance());
        return new MatchDtos.MatchOut(m.getId(), m.getPublicId(), m.getGroup().getName(), m.getMode(),
                m.getBlueTotal(), m.getRedTotal(), m.getMmrDiff(), balance, balance.getDescription(),
                m.getWinner(), m.getMemo(), m.getCreatedAt(), m.getDecidedAt(), blue, red);
    }

    private String newPublicId() {
        String id;
        do {
            StringBuilder sb = new StringBuilder(10);
            for (int i = 0; i < 10; i++) sb.append(ID_CHARS.charAt(RANDOM.nextInt(ID_CHARS.length())));
            id = sb.toString();
        } while (matchRepository.findByPublicId(id).isPresent());
        return id;
    }

    private static <T> List<T> concat(List<T> a, List<T> b) {
        List<T> all = new ArrayList<>(a);
        all.addAll(b);
        return all;
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
