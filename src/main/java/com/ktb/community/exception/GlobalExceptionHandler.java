package com.ktb.community.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * [ BusinessException 처리 ]
     * 서비스 로직에서 발생하는 모든 비즈니스 예외를 처리합니다.
     * @param e BusinessException
     * @return ResponseEntity<ErrorResponse>
     */
    @ExceptionHandler(BusinessException.class)
    protected Mono<ResponseEntity<ErrorResponseDto>> handleBusinessException(BusinessException e) {
        log.warn("handleBusinessException: {}", e.getErrorCode().getMessage(), e);
        ErrorCode errorCode = e.getErrorCode();
        return ErrorResponseDto.toResponseEntity(errorCode);
    }

    /**
     * [ @Valid 유효성 검사 실패 처리 ]
     * @RequestBody, @ModelAttribute 에서 @Valid 어노테이션으로 유효성 검사 실패 시 발생합니다.
     * @param e MethodArgumentNotValidException
     * @return ResponseEntity<ErrorResponse>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected Mono<ResponseEntity<ErrorResponseDto>> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        log.warn("handleMethodArgumentNotValidException: {}", e.getMessage());

        // 1. 발생한 모든 에러 중 첫 번째 에러의 메시지를 가져옵니다.
        String errorMessage = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();

        // 2. 해당 메시지를 ErrorResponseDto를 통해 반환합니다.
        // ErrorCode의 기본 메시지 대신, 동적으로 생성된 errorMessage를 사용합니다.
        return ErrorResponseDto.toResponseEntity(ErrorCode.INVALID_INPUT_VALUE, errorMessage);
    }

    /**
     * [ 나머지 예외 처리 ]
     * 위에서 처리되지 않은 모든 예외를 처리합니다. (최후의 보루)
     * @param e Exception
     * @return ResponseEntity<ErrorResponse>
     */
    @ExceptionHandler(Exception.class)
    protected Mono<ResponseEntity<ErrorResponseDto>> handleException(Exception e) {
        log.error("unhandledException: {}", e.getMessage(), e);
        return ErrorResponseDto.toResponseEntity(ErrorCode.INTERNAL_SERVER_ERROR);
    }
}
