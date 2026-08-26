package com.creatorhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * CreatorHub Spring Boot DDD 模块化单体启动入口。
 *
 * @author creatorhub-team
 * @since 2026-08-25
 */
// Identity 使用自有 JWT Filter，不启用 Spring Boot 自动生成的内存用户和临时密码。
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class CreatorHubApplication {

  public static void main(String[] args) {
    SpringApplication.run(CreatorHubApplication.class, args);
  }
}
