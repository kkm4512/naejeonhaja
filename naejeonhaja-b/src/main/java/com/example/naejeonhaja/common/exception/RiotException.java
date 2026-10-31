package com.example.naejeonhaja.common.exception;

import com.example.naejeonhaja.common.response.enums.ApiResponseEnum;
import lombok.Getter;

@Getter
public class RiotException extends BaseException {

    /** 429 일 때 몇 초 뒤에 다시 시도하면 되는지 (그 외에는 null) */
    private final Integer retryAfterSeconds;

    public RiotException(ApiResponseEnum responseEnum) {
        super(responseEnum);
        this.retryAfterSeconds = null;
    }

    public RiotException(ApiResponseEnum responseEnum, String detailMessage) {
        super(responseEnum, detailMessage);
        this.retryAfterSeconds = null;
    }

    public RiotException(ApiResponseEnum responseEnum, String detailMessage, int retryAfterSeconds) {
        super(responseEnum, detailMessage);
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
