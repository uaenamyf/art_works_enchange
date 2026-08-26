package com.creatorhub.identity.application.port.in;

/**
 * 认证端点限流输入端口。
 *
 * <p>接口层只提供已经由服务器解析的客户端 IP 和规范化前的邮箱；具体计数、摘要和 Redis 键结构由基础设施适配器负责，避免 Controller 直接依赖 Redis。
 */
public interface AuthRateLimitUseCase {

  /** 注册按客户端 IP 限流，防止批量创建账号。 */
  void checkRegistration(String clientIp);

  /** 登录同时按客户端 IP 与邮箱摘要限流，兼顾撞库和针对单账号的暴力破解。 */
  void checkLogin(String clientIp, String email);

  /** Refresh Token 轮换按客户端 IP 限流，降低 Cookie 重放和资源消耗。 */
  void checkRefresh(String clientIp);
}
