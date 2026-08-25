package com.creatorhub.config;

import com.creatorhub.sharedkernel.error.BusinessError;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局错误处理：业务错误与校验错误统一映射为 RFC 9457 Problem Details。
 *
 * <p>错误体附加 {@code code} 字段，前端按业务错误码路由错误消息与重试行为。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final URI PROBLEM_TYPE_BASE = URI.create("https://creatorhub.com/problems/");

    @ExceptionHandler(BusinessError.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessError ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setType(URI.create(PROBLEM_TYPE_BASE + ex.code()));
        detail.setTitle(ex.code());
        detail.setProperty("code", ex.code());
        detail.setProperty("details", ex.details());
        return ResponseEntity.badRequest().body(detail);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        f -> f.getField(),
                        f -> f.getDefaultMessage() == null ? "invalid" : f.getDefaultMessage(),
                        (a, b) -> a));
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
        detail.setType(URI.create(PROBLEM_TYPE_BASE + "VALIDATION_FAILED"));
        detail.setTitle("VALIDATION_FAILED");
        detail.setProperty("code", "VALIDATION_FAILED");
        detail.setProperty("fieldErrors", fieldErrors);
        return ResponseEntity.badRequest().body(detail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraint(ConstraintViolationException ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        detail.setType(URI.create(PROBLEM_TYPE_BASE + "VALIDATION_FAILED"));
        detail.setTitle("VALIDATION_FAILED");
        detail.setProperty("code", "VALIDATION_FAILED");
        return ResponseEntity.badRequest().body(detail);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error");
        detail.setType(URI.create(PROBLEM_TYPE_BASE + "INTERNAL_ERROR"));
        detail.setTitle("INTERNAL_ERROR");
        detail.setProperty("code", "INTERNAL_ERROR");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(detail);
    }

    private static Map<String, Object> baseBody(String code, String message, HttpStatus status, WebRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
