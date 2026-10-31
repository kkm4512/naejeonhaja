package com.example.naejeonhaja.domain.game.riot.client;

import com.example.naejeonhaja.common.exception.RiotException;
import com.example.naejeonhaja.common.response.enums.BaseApiResponse;
import com.example.naejeonhaja.config.AppProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

/**
 * Riot API 저수준 호출기. 상태 코드를 의미 있는 예외로 바꾼다.
 * <ul>
 *   <li>404 → RIOT_PLAYER_NOT_FOUND</li>
 *   <li>401/403 → 키 만료/무효</li>
 *   <li>429 → Retry-After 가 3초 이하면 한 번 기다렸다 재시도, 아니면 RIOT_RATE_LIMITED</li>
 *   <li>5xx/타임아웃 → RIOT_API_ERROR</li>
 * </ul>
 */
@Slf4j(topic = "RiotApiClient")
@Component
@RequiredArgsConstructor
public class RiotApiClient {

    private static final int MAX_INLINE_WAIT_SECONDS = 3;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final AppProperties.Riot props;
    private final RiotRateLimiter rateLimiter;

    public <T> T get(URI uri, TypeReference<T> type) {
        return get(uri, type, true);
    }

    private <T> T get(URI uri, TypeReference<T> type, boolean allowRetry) {
        if (!props.hasKey()) {
            throw new RiotException(BaseApiResponse.RIOT_API_KEY_MISSING);
        }
        rateLimiter.acquire();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Riot-Token", props.key());
        try {
            String body = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
            return objectMapper.readValue(body, type);
        } catch (HttpStatusCodeException e) {
            HttpStatusCode status = e.getStatusCode();
            if (status.value() == 404) {
                throw new RiotException(BaseApiResponse.RIOT_PLAYER_NOT_FOUND);
            }
            if (status.value() == 401 || status.value() == 403) {
                log.error("Riot API 인증 실패 ({}): {}", status.value(), uri.getPath());
                throw new RiotException(BaseApiResponse.RIOT_API_KEY_INVALID);
            }
            if (status.value() == 429) {
                int retryAfter = Math.max(1, parseRetryAfter(e.getResponseHeaders()));
                rateLimiter.blockFor(retryAfter);
                if (allowRetry && retryAfter <= MAX_INLINE_WAIT_SECONDS) {
                    sleepSeconds(retryAfter);
                    return get(uri, type, false);
                }
                throw new RiotException(BaseApiResponse.RIOT_RATE_LIMITED,
                        "Riot API 호출 한도를 초과했습니다. 약 " + retryAfter + "초 후 다시 시도해주세요.", retryAfter);
            }
            log.error("Riot API 오류 {} : {}", status.value(), uri.getPath());
            throw new RiotException(BaseApiResponse.RIOT_API_ERROR);
        } catch (ResourceAccessException e) {
            log.error("Riot API 연결 실패: {}", e.getMessage());
            throw new RiotException(BaseApiResponse.RIOT_API_ERROR, "Riot API 응답이 없습니다. 잠시 후 다시 시도해주세요.");
        } catch (RiotException e) {
            throw e;
        } catch (Exception e) {
            log.error("Riot API 응답 처리 실패: {}", e.getMessage());
            throw new RiotException(BaseApiResponse.RIOT_API_ERROR);
        }
    }

    private static int parseRetryAfter(HttpHeaders headers) {
        if (headers == null) return 10;
        String value = headers.getFirst("Retry-After");
        try {
            return value == null ? 10 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 10;
        }
    }

    private static void sleepSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
