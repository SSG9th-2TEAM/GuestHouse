package com.ssg9th2team.geharbang.global.error;

import com.ssg9th2team.geharbang.global.dto.ErrorResponse;
import com.ssg9th2team.geharbang.global.lock.LockAcquisitionException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.view.RedirectView;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${oauth2.failure-redirect-uri:/login}")
    private String failureRedirectUri;

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("IllegalArgumentException: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException ex) {
        log.warn("IllegalStateException: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT); // 409 에러
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException ex) {
        log.warn("EntityNotFoundException: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(LockAcquisitionException.class)
    public ResponseEntity<ErrorResponse> handleLockAcquisitionException(LockAcquisitionException ex) {
        log.warn("LockAcquisitionException: {}", ex.getMessage());
        ErrorResponse response = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT); // 409 Conflict
    }

    @ExceptionHandler(RuntimeException.class)
    public Object handleRuntimeException(RuntimeException ex, HttpServletRequest request) {
        log.error("RuntimeException: {}", ex.getMessage(), ex);

        // 브라우저 요청(HTML)인 경우 로그인 페이지로 리다이렉트
        if (isHtmlRequest(request)) {
            log.info("Redirecting browser request to login page due to RuntimeException");
            return new RedirectView(failureRedirectUri);
        }

        // API 요청인 경우 JSON 응답
        ErrorResponse response = new ErrorResponse(ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(Exception.class)
    public Object handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled Exception: ", ex);

        // 브라우저 요청(HTML)인 경우 로그인 페이지로 리다이렉트
        if (isHtmlRequest(request)) {
            log.info("Redirecting browser request to login page due to exception");
            return new RedirectView(failureRedirectUri);
        }

        // API 요청인 경우 JSON 응답
        ErrorResponse response = new ErrorResponse("서버 내부 오류가 발생했습니다.");
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 브라우저의 HTML 요청인지 확인
     */
    private boolean isHtmlRequest(HttpServletRequest request) {
        String acceptHeader = request.getHeader("Accept");
        String requestUri = request.getRequestURI();

        // API 경로는 항상 JSON 응답
        if (requestUri != null && requestUri.startsWith("/api/")) {
            return false;
        }

        // OAuth2 콜백 경로는 브라우저 요청으로 처리
        if (requestUri != null && requestUri.startsWith("/login/oauth2/code/")) {
            return true;
        }

        // Accept 헤더로 판단
        return acceptHeader != null && acceptHeader.contains("text/html");
    }
}
