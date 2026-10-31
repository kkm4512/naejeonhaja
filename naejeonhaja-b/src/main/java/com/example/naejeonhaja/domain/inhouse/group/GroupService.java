package com.example.naejeonhaja.domain.inhouse.group;

import com.example.naejeonhaja.common.exception.BaseException;
import com.example.naejeonhaja.common.response.enums.BaseApiResponse;
import com.example.naejeonhaja.domain.inhouse.auth.GroupTokenService;
import com.example.naejeonhaja.domain.inhouse.match.InhouseMatchRepository;
import com.example.naejeonhaja.domain.inhouse.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
public class GroupService {

    // 헷갈리는 글자(0/O, 1/I/L) 제외
    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final BCryptPasswordEncoder ENCODER = new BCryptPasswordEncoder();

    /** 비밀번호 대입 방지: 방마다 10분에 10번까지만 틀릴 수 있음 */
    private static final int MAX_FAILURES = 10;
    private final Cache<String, AtomicInteger> failures = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10)).maximumSize(10_000).build();

    private final InhouseGroupRepository groupRepository;
    private final MemberRepository memberRepository;
    private final InhouseMatchRepository matchRepository;
    private final GroupTokenService tokenService;

    @Transactional
    public GroupDtos.Session create(GroupDtos.CreateRequest req) {
        String code;
        do {
            code = randomCode();
        } while (groupRepository.existsByCode(code));
        InhouseGroup group = groupRepository.save(new InhouseGroup(code, req.name().trim(), ENCODER.encode(req.password())));
        return new GroupDtos.Session(group.getCode(), group.getName(), tokenService.issue(group));
    }

    @Transactional(readOnly = true)
    public GroupDtos.Session join(String code, GroupDtos.JoinRequest req) {
        InhouseGroup group = groupRepository.findByCode(code.trim().toUpperCase())
                .orElseThrow(() -> new BaseException(BaseApiResponse.GROUP_NOT_FOUND));
        AtomicInteger failed = failures.get(group.getCode(), k -> new AtomicInteger());
        if (failed.get() >= MAX_FAILURES) {
            throw new BaseException(BaseApiResponse.GROUP_LOCKED);
        }
        if (!ENCODER.matches(req.password(), group.getPasswordHash())) {
            failed.incrementAndGet();
            throw new BaseException(BaseApiResponse.GROUP_WRONG_PASSWORD);
        }
        failures.invalidate(group.getCode());
        return new GroupDtos.Session(group.getCode(), group.getName(), tokenService.issue(group));
    }

    @Transactional(readOnly = true)
    public GroupDtos.Summary summary(InhouseGroup group) {
        return new GroupDtos.Summary(group.getCode(), group.getName(),
                memberRepository.countByGroupId(group.getId()), matchRepository.countByGroupId(group.getId()));
    }

    private static String randomCode() {
        StringBuilder sb = new StringBuilder(CODE_LENGTH);
        for (int i = 0; i < CODE_LENGTH; i++) {
            sb.append(CODE_CHARS.charAt(RANDOM.nextInt(CODE_CHARS.length())));
        }
        return sb.toString();
    }
}
