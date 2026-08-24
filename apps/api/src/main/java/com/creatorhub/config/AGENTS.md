# config 顶层包

> Spring 配置。包含多 profile、Bean 装配、安全、OpenAPI 等基础设施装配。

## 进入条件

- 调整 `application-{profile}.yaml` 加载策略。
- 引入新的 `@Configuration`、CORS、限流、过滤器。
- 调整 Spring Security 配置（除 `identity/infrastructure/security` 外）。

## 必读

- 根 `AGENTS.md` §3、§7。
- 根 `DEVELOPMENT_GUIDE.md` §2 技术选型、§8 安全与隐私。
- 14 周计划 Week 1、Week 12。

## 约束

- 不放业务规则；只放 Bean 装配、Profile 绑定、配置类。
- 不直接依赖 `domain` 业务包；只装配 `application` 与 `interfaces` 入口。
- 任何密钥、连接串从环境变量或 Secret 注入；仓库仅保留 `.env.example`。
- CORS、CSRF、限流等横切关注点统一在此处装配；不在 Controller 上散落。

## 常用命令

```bash
cd apps/api
./gradlew test --tests 'com.creatorhub.config.*'
```

## 交叉引用

- `identity/infrastructure/security`：鉴权细节。
- `observability/AGENTS.md`：健康检查与指标装配。
- `webhook/AGENTS.md`：Webhook 过滤器。
