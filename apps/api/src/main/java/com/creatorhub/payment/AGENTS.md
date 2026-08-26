# payment 限界上下文

> 支撑域。Stripe PaymentIntent、Webhook 验签、Payment 状态机。

## 进入条件

- 修改 `/orders/{id}/payment-intent` 或 `/payments/webhooks/stripe`。
- 调整 `Payment` 聚合或 Stripe 适配器。
- 处理 Webhook 验签、幂等键、退款状态机。

## 必读

- 根 `AGENTS.md` §3、§7。
- 根 `DEVELOPMENT_GUIDE.md` §4.4 支付时序与 PaymentIntent 恢复、§8 Stripe Webhook 验签。
- `docs/openapi.yaml` 标签 `Payments`、`Webhooks`。
- 14 周计划 Week 8、Week 9。
- 顶层 `webhook/AGENTS.md`。

## 聚合与事件

- 聚合：`Payment`。
- 集成事件：`PaymentSucceeded` v1、`PaymentFailed` v1（→ Ordering、Notification）。

## 约束

- Stripe SDK 仅出现在 `infrastructure/stripe`；domain 不依赖。
- Webhook 路由使用原始请求体验签，不使用 Bearer Token。
- Webhook 事件 ID 唯一索引；重复投递直接返回 2xx。
- PaymentIntent 创建使用 `Idempotency-Key`，相同 Key 同摘要重放同一结果。
- 不记录 `client_secret` 或 Webhook secret；不依赖前端支付成功推进订单。
- 退款在 MVP 仅提供 stub。

## 常用命令

```bash
cd apps/api
mvn -Dtest='com.creatorhub.payment.**.*Test' test
```

## 交叉引用

- Ordering：发布 `PaymentSucceeded`/`PaymentFailed` 集成事件。
- 顶层 `webhook/security` 提供通用验签基础。
