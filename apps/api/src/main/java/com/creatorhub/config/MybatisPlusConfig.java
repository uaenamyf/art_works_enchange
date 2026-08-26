package com.creatorhub.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 写模型配置。
 *
 * <p>乐观锁拦截器识别持久化行上的 {@code @Version}，防止并发更新静默覆盖。 Refresh Token 轮换仍使用显式 {@code FOR
 * UPDATE}，因为它需要消费式悲观锁。
 */
@Configuration
public class MybatisPlusConfig {

  /** 注册只影响写模型的乐观锁插件；分页插件待查询端游标分页落地时再加入。 */
  @Bean
  MybatisPlusInterceptor mybatisPlusInterceptor() {
    MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
    interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
    return interceptor;
  }
}
