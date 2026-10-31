package com.example.naejeonhaja.common.exception;

import com.example.naejeonhaja.common.response.ApiResponse;
import com.example.naejeonhaja.common.response.enums.BaseApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ApiResponse<Void>> handleBaseException(BaseException e) {
        int status = e.getResponseEnum().getCode().value();
        if (status >= 500) {
            log.error("BaseException [{}]: {}", status, e.getMessage());
        } else {
            log.info("BaseException [{}]: {}", status, e.getMessage());
        }
        Integer retryAfter = e instanceof RiotException re ? re.getRetryAfterSeconds() : null;
        var builder = ResponseEntity.status(status);
        if (retryAfter != null) {
            builder.header("Retry-After", String.valueOf(retryAfter));
        }
        return builder.body(ApiResponse.error(status, e.getMessage(), retryAfter));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream()
                .findFirst()
                .map(err -> {
                    String msg = err.getDefaultMessage();
                    if (err instanceof FieldError fe && msg != null && msg.startsWith("must")) {
                        return fe.getField() + ": " + msg;
                    }
                    return msg;
                })
                .orElse(BaseApiResponse.BAD_REQUEST.getMessage());
        return badRequest(message);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodValidation(HandlerMethodValidationException e) {
        String message = e.getAllErrors().stream()
                .findFirst()
                .map(err -> err.getDefaultMessage())
                .orElse(BaseApiResponse.BAD_REQUEST.getMessage());
        return badRequest(message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadable(HttpMessageNotReadableException e) {
        log.info("요청 본문 파싱 실패: {}", e.getMostSpecificCause().getMessage());
        return badRequest("요청 본문을 읽을 수 없습니다. 티어/라인 값과 JSON 형식을 확인해주세요.");
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleParam(Exception e) {
        return badRequest(BaseApiResponse.BAD_REQUEST.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException e) {
        return ResponseEntity.status(404).body(ApiResponse.of(BaseApiResponse.NOT_FOUND));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethod(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(405).body(ApiResponse.error(405, "지원하지 않는 HTTP 메서드입니다."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("UnhandledException: {}", e.getMessage(), e);
        return ResponseEntity.internalServerError().body(ApiResponse.of(BaseApiResponse.INTERNAL_ERROR));
    }

    private ResponseEntity<ApiResponse<Void>> badRequest(String message) {
        return ResponseEntity.badRequest().body(ApiResponse.error(400, message));
    }
}
