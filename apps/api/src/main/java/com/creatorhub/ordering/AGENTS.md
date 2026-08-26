# ordering 限界上下文

> 核心域。订单、价格快照、库存预占、状态机。

## 进入条件

- 修改 `/orders/*` 或订单状态机。
- 调整 `Order` 聚合、`OrderLine`、`Address`。
- 库存预占、超期返还与支付结果回写。

## 必读

- 根 `AGENTS.md` §3、§7。
- 根 `DEVELOPMENT_GUIDE.md` §4.4 库存与支付一致性、§4.5 Outbox 最低契约。
- `docs/openapi.yaml` 标签 `Orders`、`Payments`。
- 14 周计划 Week 7-9、Week 11。

## 聚合与事件

- 聚合：`Order`。
- 值对象：`OrderId`、`OrderLine`、`Address`、`Money`。
- 集成事件：`OrderCreated` v1（→ Payment、Notification）、`OrderStatusChanged` v1（→ Notification）。
- 消费事件：`PaymentSucceeded` v1、`PaymentFailed` v1（来自 Payment）。

## 约束

- 金额使用价格快照；订单项创建后不可变。
- 单事务内条件更新 `stock >= quantity` 预占库存，并写 Outbox。
- `Idempotency-Key` 作用域：当前用户 + `createOrder` + Key，保留 24 小时。
- 状态机：`PENDING_PAYMENT → PAID → PROCESSING → SHIPPED → COMPLETED`，退款分支需审批。
- 订单超期定时任务必须幂等返还库存。
- 不得直接读 `artworks` 表；只能通过 Catalog `StockAvailabilityPort`。
- 不得由 Controller 直接把订单置为 `PAID`；只能由 Payment Webhook 驱动。

## 常用命令

```bash
cd apps/api
mvn -Dtest='com.creatorhub.ordering.**.*Test' test
```

## 交叉引用

- Catalog：库存预占端口。
- Payment：PaymentIntent 与 Webhook 触发状态迁移。
- Identity：获取 `BuyerId`。
- Notification：发送 `OrderStatusChanged`。
