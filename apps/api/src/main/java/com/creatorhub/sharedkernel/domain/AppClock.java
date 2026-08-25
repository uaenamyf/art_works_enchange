package com.creatorhub.sharedkernel.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * 时间抽象。
 *
 * <p>领域内全部使用 UTC；服务在需要当前时间时通过 {@link #now()} 获取，便于测试桩注入。
 */
public final class AppClock {

    private static volatile Clock clock = Clock.systemUTC();

    private AppClock() {
    }

    public static Instant now() {
        return clock.instant();
    }

    public static Instant nowUtc() {
        return clock.instant().atZone(ZoneOffset.UTC).toInstant();
    }

    /** 测试或独立工具可注入固定时钟。 */
    public static void override(Clock newClock) {
        clock = newClock;
    }

    /** 默认重置回系统 UTC；用于测试清理。 */
    public static void reset() {
        clock = Clock.systemUTC();
    }
}
