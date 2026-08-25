package com.creatorhub.sharedkernel.error;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * 业务错误码。
 *
 * <p>错误码为稳定字符串，前端与日志按此归类。命名采用 {@code CONTEXT_REASON} 形式。
 */
public final class BusinessError extends RuntimeException {

    private final String code;
    private final transient Map<String, Object> details;

    public BusinessError(String code, String message) {
        this(code, message, Collections.emptyMap(), null);
    }

    public BusinessError(String code, String message, Map<String, Object> details, Throwable cause) {
        super(Objects.requireNonNull(message, "message"), cause);
        this.code = Objects.requireNonNull(code, "code");
        this.details = details == null ? Collections.emptyMap() : Map.copyOf(details);
    }

    public String code() {
        return code;
    }

    public Map<String, Object> details() {
        return details;
    }
}
