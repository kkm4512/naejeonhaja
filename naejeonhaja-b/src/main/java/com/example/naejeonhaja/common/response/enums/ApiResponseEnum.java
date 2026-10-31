package com.example.naejeonhaja.common.response.enums;

import org.springframework.http.HttpStatus;

public interface ApiResponseEnum {
    HttpStatus getCode();
    String getMessage();
}
