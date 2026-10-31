package com.example.naejeonhaja.domain.inhouse.member;

import com.example.naejeonhaja.common.exception.BaseException;
import com.example.naejeonhaja.common.response.enums.BaseApiResponse;
import com.example.naejeonhaja.domain.game.dataDragon.service.DataDragonService;
import com.example.naejeonhaja.domain.game.lol.enums.LolTier;
import com.example.naejeonhaja.domain.inhouse.common.LinesJson;
import com.example.naejeonhaja.domain.inhouse.group.InhouseGroup;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final DataDragonService dataDragon;

    @Transactional(readOnly = true)
    public List<MemberDtos.MemberDto> list(InhouseGroup group) {
        return memberRepository.findByGroupIdOrderByNameAsc(group.getId()).stream().map(this::toDto).toList();
    }

    /** puuid → 이름 순으로 같은 사람을 찾아 갱신, 없으면 새로 만든다 */
    @Transactional
    public MemberDtos.MemberDto upsert(InhouseGroup group, MemberDtos.UpsertRequest req) {
        Optional<Member> existing = Optional.empty();
        if (req.puuid() != null && !req.puuid().isBlank()) {
            existing = memberRepository.findByGroupIdAndPuuid(group.getId(), req.puuid());
        }
        if (existing.isEmpty()) {
            existing = memberRepository.findByGroupIdAndNameKey(group.getId(), Member.keyOf(req.name()));
        }
        Member member = existing.orElseGet(() -> new Member(group, req.name()));
        apply(group, member, req);
        return toDto(memberRepository.save(member));
    }

    @Transactional
    public MemberDtos.MemberDto update(InhouseGroup group, Long id, MemberDtos.UpsertRequest req) {
        Member member = find(group, id);
        apply(group, member, req);
        return toDto(member);
    }

    @Transactional
    public void delete(InhouseGroup group, Long id) {
        memberRepository.delete(find(group, id));
    }

    public Member find(InhouseGroup group, Long id) {
        return memberRepository.findByIdAndGroupId(id, group.getId())
                .orElseThrow(() -> new BaseException(BaseApiResponse.MEMBER_NOT_FOUND));
    }

    private void apply(InhouseGroup group, Member member, MemberDtos.UpsertRequest req) {
        String key = Member.keyOf(req.name());
        memberRepository.findByGroupIdAndNameKey(group.getId(), key)
                .filter(other -> !other.getId().equals(member.getId()))
                .ifPresent(other -> {
                    throw new BaseException(BaseApiResponse.DUPLICATE_MEMBER, "같은 이름의 멤버가 이미 있습니다: " + other.getName());
                });
        if (!key.equals(member.getNameKey()) || !req.name().trim().equals(member.getName())) {
            member.rename(req.name());
        }
        if (req.riotId() != null || req.puuid() != null || req.profileIconId() != null) {
            member.updateProfile(blankToNull(req.riotId()), blankToNull(req.puuid()), req.profileIconId());
        }
        member.updateRank(req.tier() == null ? (member.getTier() == null ? LolTier.UNRANKED : member.getTier()) : req.tier(), req.lp());
        if (req.lines() != null) member.updateLines(LinesJson.write(req.lines()));
        member.updateMmrOverride(req.mmrOverride());
    }

    public MemberDtos.MemberDto toDto(Member m) {
        int games = m.getWins() + m.getLosses();
        return new MemberDtos.MemberDto(
                m.getId(), m.getName(), m.getRiotId(), m.getPuuid(), m.getProfileIconId(),
                m.getProfileIconId() == null ? null : dataDragon.profileIconUrl(m.getProfileIconId()),
                m.getTier(), m.getLp(), LinesJson.read(m.getLinesJson()), m.getMmrOverride(),
                m.baseMmr(), m.getInhouseDelta(), m.effectiveMmr(),
                m.getWins(), m.getLosses(), games == 0 ? 0 : Math.round(m.getWins() * 1000.0 / games) / 10.0,
                m.getUpdatedAt());
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
