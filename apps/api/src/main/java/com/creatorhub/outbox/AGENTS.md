# outbox 顶层包

> 跨上下文事件投递。PostgreSQL Outbox + Scheduled Worker，不引入 Kafka。

## 进入条件

- 调整 Outbox 表结构、Poller、重试与死信。
- 新增/修改 Outbox 事件写入逻辑（来自各聚合）。

## 必读

- 根 `AGENTS.md` §3。
- 根 `DEVELOPMENT_GUIDE.md` §4.4 Outbox 最低契约。
- `docs/domain/event-catalog.md`。
- 14 周计划 Week 9。

## 公共信封

`eventId` / `eventType` / `eventVersion` / `aggregateType` / `aggregateId` / `aggregateVersion` / `occurredAt` / `traceId` / `payload`，业务数据与 Outbox 同事务提交。

## 约束

- 业务数据与 Outbox 在同一事务；不得双写。
- Worker 使用 `FOR UPDATE SKIP LOCKED` 并发领取。
- 退避策略：指数 + 抖动；超过最大次数进入 `DEAD` 并告警，禁止静默丢弃。
- 消费者幂等：状态机守卫 + 事件去重表。
- 事件结构只向后兼容演进；破坏性变更发布新 `eventVersion`。
- 已成功事件保留 ≥30 天；死信保留 ≥90 天。

## 常用命令

```bash
cd apps/api
mvn -Dtest='com.creatorhub.outbox.**.*Test' test
```

## 交叉引用

- 各业务上下文：事件写入入口。
- 顶层 `observability/AGENTS.md`：Outbox 积压、死信告警。
