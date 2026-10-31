package com.example.naejeonhaja.common.response;

import com.example.naejeonhaja.common.response.enums.ApiResponseEnum;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

/**
 * 공통 응답 형식: { "code": 200, "message": "...", "data": ... }
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final int code;
    private final String message;
    private final T data;
    /** 429 응답일 때 몇 초 뒤 다시 시도하면 되는지 */
    private final Integer retryAfter;

    private ApiResponse(int code, String message, T data) {
        this(code, message, data, null);
    }

    private ApiResponse(int code, String message, T data, Integer retryAfter) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.retryAfter = retryAfter;
    }

    public static <T> ApiResponse<T> of(ApiResponseEnum responseEnum, T data) {
        return new ApiResponse<>(responseEnum.getCode().value(), responseEnum.getMessage(), data);
    }

    public static ApiResponse<Void> of(ApiResponseEnum responseEnum) {
        return new ApiResponse<>(responseEnum.getCode().value(), responseEnum.getMessage(), null);
    }

    public static ApiResponse<Void> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }

    public static ApiResponse<Void> error(int code, String message, Integer retryAfter) {
        return new ApiResponse<>(code, message, null, retryAfter);
    }
}
