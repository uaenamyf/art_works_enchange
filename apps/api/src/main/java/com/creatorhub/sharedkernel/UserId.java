package com.creatorhub.sharedkernel;

import com.creatorhub.sharedkernel.domain.StrongId;
import java.util.UUID;

/**
 * 用户 ID 强类型（Identity 上下文）。
 *
 * <p>放在 Shared Kernel 是因为它会被多个上下文以 ID 形式引用；
 * User 聚合本身属于 Identity 上下文。
 */
public final class UserId extends StrongId {

    private UserId(UUID value) {
        super(value);
    }

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    public static UserId random() {
        return new UserId(StrongId.randomUuid());
    }
}
