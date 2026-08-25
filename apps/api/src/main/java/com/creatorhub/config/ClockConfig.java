package com.creatorhub.config;

import com.creatorhub.sharedkernel.domain.AppClock;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

/**
 * 启动时确保 {@link AppClock} 使用系统 UTC；测试可通过 @BeforeEach 注入固定时钟。
 */
@Configuration
public class ClockConfig {

    @PostConstruct
    void resetClock() {
        AppClock.reset();
    }
}
