# notification 限界上下文

> 通用域。站内通知与邮件投递。

## 进入条件

- 修改 `/notifications/*` 或通知类型枚举。
- 调整邮件/站内通知适配器。
- 接入新的领域事件作为通知源。

## 必读

- 根 `AGENTS.md` §3、§7。
- 根 `DEVELOPMENT_GUIDE.md` §4.4。
- `docs/openapi.yaml` 标签 `Notifications`。
- `docs/domain/event-catalog.md` 事件目录。
- 14 周计划 Week 9。

## 聚合与事件

- 聚合：`Notification`。
- 集成事件：消费 `ArtworkPublished`、`OrderStatusChanged`、`PaymentSucceeded`、`PaymentFailed`。
- 不发布集成事件。

## 约束

- MVP 邮件通道使用控制台 stub，由 `infrastructure/email` 的适配器接口注入；不直接 SMTP。
- 通知正文不写敏感字段；链接使用相对路径或可校验 token。
- 消费者幂等：状态机守卫 + 事件去重表。
- 通知失败允许重试；超出重试次数进入 `DEAD` 并告警。

## 常用命令

```bash
cd apps/api
mvn -Dtest='com.creatorhub.notification.**.*Test' test
```

## 交叉引用

- 顶层 `outbox/AGENTS.md` 投递契约。
- 各事件源上下文：Catalog、Ordering、Payment、Creator。
