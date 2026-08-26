# observability 顶层包

> 日志、指标、健康检查。MVP 使用 Spring Boot Actuator + Micrometer + 结构化日志。

## 进入条件

- 调整日志格式、追踪字段、指标名。
- 增加告警或健康检查项。
- 接入 Sentry 或 OpenTelemetry（Week 13 之后）。

## 必读

- 根 `AGENTS.md` §7、§8。
- 根 `DEVELOPMENT_GUIDE.md` §11 可观测性。
- 14 周计划 Week 13 生产就绪。

## 约束

- 日志字段：`timestamp`、`level`、`service`、`requestId`、`userId`（可空）、`route`。
- 不记录密码、Token、Cookie、支付信息、完整地址和邮箱。
- 自定义指标命名 `creatorhub.<domain>.<metric>`，如 `creatorhub.outbox.dead_letters`。
- 健康检查：`/actuator/health` 公开 + liveness/readiness 拆分；敏感端点不对公网。
- 告警阈值至少覆盖：5xx 激增、Stripe Webhook 失败、Outbox 死信、数据库连接耗尽、卷容量。

## 常用命令

```bash
cd apps/api
mvn spring-boot:run -Dspring-boot.run.profiles=local
curl http://localhost:8080/actuator/health
```

## 交叉引用

- `outbox/AGENTS.md`：Outbox 积压与死信指标。
- `payment/AGENTS.md`：Webhook 失败指标。
- 根 `DEVELOPMENT_GUIDE.md` §13 Docker 部署：健康检查在 Compose 中配置。
