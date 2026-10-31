package com.example.naejeonhaja.domain.inhouse.auth;

import com.example.naejeonhaja.domain.inhouse.group.InhouseGroup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

/**
 * 방 입장 토큰 (서명된 문자열, 서버에 세션을 두지 않음).
 *
 * <pre>token = base64url(code.issuedAt.pwFingerprint) + "." + base64url(HMAC-SHA256)</pre>
 *
 * <p>비밀번호를 바꾸면 pwFingerprint 가 달라져 기존 토큰이 모두 무효가 된다.
 * 서명 키는 APP_SECRET 환경변수, 없으면 처음 실행할 때 만들어 DB(app_setting)에 저장한다.</p>
 */
@Slf4j
@Service
public class GroupTokenService {

    private static final Duration TTL = Duration.ofDays(90);
    private static final Base64.Encoder ENC = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DEC = Base64.getUrlDecoder();

    private final byte[] secret;

    public GroupTokenService(@Value("${app.secret:}") String configured, JdbcTemplate jdbc) {
        this.secret = resolveSecret(configured, jdbc).getBytes(StandardCharsets.UTF_8);
    }

    public record Claims(String code, String pwFingerprint, Instant issuedAt) {
    }

    public String issue(InhouseGroup group) {
        String payload = group.getCode() + "." + Instant.now().getEpochSecond() + "." + fingerprint(group.getPasswordHash());
        return ENC.encodeToString(payload.getBytes(StandardCharsets.UTF_8)) + "." + ENC.encodeToString(sign(payload));
    }

    public Optional<Claims> verify(String token) {
        if (token == null || token.isBlank()) return Optional.empty();
        try {
            String[] parts = token.trim().split("\\.");
            if (parts.length != 2) return Optional.empty();
            String payload = new String(DEC.decode(parts[0]), StandardCharsets.UTF_8);
            if (!MessageDigest.isEqual(sign(payload), DEC.decode(parts[1]))) return Optional.empty();
            String[] p = payload.split("\\.");
            if (p.length != 3) return Optional.empty();
            Instant issued = Instant.ofEpochSecond(Long.parseLong(p[1]));
            if (issued.plus(TTL).isBefore(Instant.now())) return Optional.empty();
            return Optional.of(new Claims(p[0], p[2], issued));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    public boolean matches(Claims claims, InhouseGroup group) {
        return claims.code().equals(group.getCode()) && claims.pwFingerprint().equals(fingerprint(group.getPasswordHash()));
    }

    private byte[] sign(String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret, "HmacSHA256"));
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String fingerprint(String passwordHash) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(passwordHash.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(d, 0, 6);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String resolveSecret(String configured, JdbcTemplate jdbc) {
        if (configured != null && configured.length() >= 16) {
            return configured;
        }
        var existing = jdbc.queryForList("SELECT v FROM app_setting WHERE k = 'token_secret'", String.class);
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        byte[] random = new byte[32];
        new SecureRandom().nextBytes(random);
        String generated = HexFormat.of().formatHex(random);
        jdbc.update("INSERT INTO app_setting (k, v) VALUES ('token_secret', ?)", generated);
        log.info("방 입장 토큰 서명 키를 새로 만들어 DB에 저장했습니다.");
        return generated;
    }
}
