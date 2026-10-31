package com.example.naejeonhaja.common.exception;

import com.example.naejeonhaja.common.response.enums.ApiResponseEnum;
import lombok.Getter;

@Getter
public class BaseException extends RuntimeException {
    private final ApiResponseEnum responseEnum;

    public BaseException(ApiResponseEnum responseEnum) {
        super(responseEnum.getMessage());
        this.responseEnum = responseEnum;
    }

    /** enum 의 기본 메시지 대신 상황에 맞는 상세 메시지를 내려줄 때 */
    public BaseException(ApiResponseEnum responseEnum, String detailMessage) {
        super(detailMessage);
        this.responseEnum = responseEnum;
    }
}
