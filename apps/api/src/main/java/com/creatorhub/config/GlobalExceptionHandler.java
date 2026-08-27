package com.creatorhub.config;

import com.creatorhub.sharedkernel.error.BusinessError;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
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
    HttpStatus status =
        switch (ex.code()) {
          case "EMAIL_TAKEN", "CREATOR_ALREADY_EXISTS", "CREATOR_SLUG_TAKEN" -> HttpStatus.CONFLICT;
          case "INVALID_CREDENTIALS", "REFRESH_TOKEN_INVALID" -> HttpStatus.UNAUTHORIZED;
          case "RATE_LIMITED" -> HttpStatus.TOO_MANY_REQUESTS;
          case "CREATOR_NOT_FOUND" -> HttpStatus.NOT_FOUND;
          case "ARTWORK_NOT_FOUND" -> HttpStatus.NOT_FOUND;
          case "MEDIA_NOT_FOUND" -> HttpStatus.NOT_FOUND;
          case "INVALID_CURSOR" -> HttpStatus.BAD_REQUEST;
          case "ARTWORK_VERSION_CONFLICT" -> HttpStatus.CONFLICT;
          default -> HttpStatus.BAD_REQUEST;
        };
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
    detail.setType(URI.create(PROBLEM_TYPE_BASE + ex.code()));
    detail.setTitle(ex.code());
    detail.setProperty("code", ex.code());
    detail.setProperty("details", ex.details());
    ResponseEntity.BodyBuilder response = ResponseEntity.status(status);
    if (status == HttpStatus.TOO_MANY_REQUESTS) {
      Object retryAfter = ex.details().get("retryAfterSeconds");
      if (retryAfter != null) {
        response.header(HttpHeaders.RETRY_AFTER, retryAfter.toString());
      }
    }
    return response.body(detail);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> fieldErrors =
        ex.getBindingResult().getFieldErrors().stream()
            .collect(
                Collectors.toMap(
                    f -> f.getField(),
                    f -> f.getDefaultMessage() == null ? "invalid" : f.getDefaultMessage(),
                    (a, b) -> a));
    ProblemDetail detail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
    detail.setType(URI.create(PROBLEM_TYPE_BASE + "VALIDATION_FAILED"));
    detail.setTitle("VALIDATION_FAILED");
    detail.setProperty("code", "VALIDATION_FAILED");
    detail.setProperty("fieldErrors", fieldErrors);
    return ResponseEntity.badRequest().body(detail);
  }

  /** 方法级权限拒绝统一返回 403，避免被通用异常处理器误判为 500。 */
  @ExceptionHandler(AuthorizationDeniedException.class)
  public ResponseEntity<ProblemDetail> handleAuthorizationDenied(AuthorizationDeniedException ex) {
    ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Forbidden");
    detail.setType(URI.create(PROBLEM_TYPE_BASE + "FORBIDDEN"));
    detail.setTitle("FORBIDDEN");
    detail.setProperty("code", "FORBIDDEN");
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(detail);
  }

  /** 领域值对象拒绝客户端输入时返回统一校验错误，而不是暴露为服务器故障。 */
  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ProblemDetail> handleIllegalArgument(IllegalArgumentException ex) {
    ProblemDetail detail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
    detail.setType(URI.create(PROBLEM_TYPE_BASE + "VALIDATION_FAILED"));
    detail.setTitle("VALIDATION_FAILED");
    detail.setProperty("code", "VALIDATION_FAILED");
    return ResponseEntity.badRequest().body(detail);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ProblemDetail> handleConstraint(ConstraintViolationException ex) {
    // 不回显原始约束消息，避免暴露方法参数名和内部实现提示。
    logUnexpected("constraint violation", ex);
    ProblemDetail detail =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Request validation failed");
    detail.setType(URI.create(PROBLEM_TYPE_BASE + "VALIDATION_FAILED"));
    detail.setTitle("VALIDATION_FAILED");
    detail.setProperty("code", "VALIDATION_FAILED");
    return ResponseEntity.badRequest().body(detail);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ProblemDetail> handleUnexpected(Exception ex) {
    // 未预期异常只返回固定消息；完整异常仅在服务端记录，避免信息泄漏。
    logUnexpected("unexpected", ex);
    ProblemDetail detail =
        ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error");
    detail.setType(URI.create(PROBLEM_TYPE_BASE + "INTERNAL_ERROR"));
    detail.setTitle("INTERNAL_ERROR");
    detail.setProperty("code", "INTERNAL_ERROR");
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(detail);
  }

  private static void logUnexpected(String kind, Throwable ex) {
    // 日志只写固定标签与异常类型，不主动拼接密码、Token、Cookie 或个人信息。
    org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class)
        .error("{}: {}", kind, ex.getClass().getName(), ex);
  }
}
