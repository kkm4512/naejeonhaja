package com.example.naejeonhaja.common.response.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BaseApiResponse implements ApiResponseEnum {
    SUCCESS(HttpStatus.OK, "요청이 성공했습니다."),

    // 요청 오류
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다."),

    // 팀 구성
    INVALID_PLAYER_COUNT(HttpStatus.BAD_REQUEST, "정확히 10명의 플레이어가 필요합니다."),
    DUPLICATE_PLAYER(HttpStatus.BAD_REQUEST, "같은 이름의 플레이어가 있습니다."),
    LINE_UNSATISFIABLE(HttpStatus.UNPROCESSABLE_ENTITY, "모든 라인을 채우는 팀 조합이 없습니다."),
    TEAM_MISMATCH(HttpStatus.BAD_REQUEST, "팀 구성에 실패했습니다. 다시 시도해주세요."),

    // 방 / 멤버 / 내전 기록
    GROUP_NOT_FOUND(HttpStatus.NOT_FOUND, "방을 찾을 수 없습니다. 입장 코드를 확인해주세요."),
    GROUP_WRONG_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 맞지 않습니다."),
    GROUP_LOCKED(HttpStatus.TOO_MANY_REQUESTS, "비밀번호를 너무 여러 번 틀렸습니다. 10분 뒤에 다시 시도해주세요."),
    GROUP_UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "방에 다시 입장해주세요. (입장 정보가 없거나 만료됨)"),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "멤버를 찾을 수 없습니다."),
    DUPLICATE_MEMBER(HttpStatus.CONFLICT, "같은 이름의 멤버가 이미 있습니다."),
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "내전 기록을 찾을 수 없습니다."),

    // Riot API
    INVALID_RIOT_ID(HttpStatus.BAD_REQUEST, "Riot ID 형식이 올바르지 않습니다. (예: 이름#KR1)"),
    RIOT_PLAYER_NOT_FOUND(HttpStatus.NOT_FOUND, "플레이어를 찾을 수 없습니다."),
    RIOT_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Riot API 호출 한도를 초과했습니다. 잠시 후 다시 시도해주세요."),
    RIOT_API_KEY_MISSING(HttpStatus.SERVICE_UNAVAILABLE, "서버에 Riot API 키가 설정되지 않았습니다. (.env 의 RIOT_API_KEY)"),
    RIOT_API_KEY_INVALID(HttpStatus.SERVICE_UNAVAILABLE, "Riot API 키가 유효하지 않거나 만료되었습니다. (개발용 키는 24시간마다 갱신 필요)"),
    RIOT_API_ERROR(HttpStatus.BAD_GATEWAY, "Riot API 호출에 실패했습니다.");

    private final HttpStatus code;
    private final String message;
}
