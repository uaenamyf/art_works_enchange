# orders feature

> 消费者购买订单：列表、详情、取消、支付意图、结果页。

## 进入条件

- 调整 `/orders/*`、`/orders/[id]`、订单结果页。
- 调整订单状态展示与取消按钮。

## 必读

- 根 `AGENTS.md` §4、§5、§7。
- `docs/openapi.yaml` 标签 `Orders`、`Payments`。
- 14 周计划 Week 7、Week 8、Week 11。

## 约束

- 创建订单、获取 PaymentIntent 必须携带 `Idempotency-Key`（UUID）。
- 不得根据前端“支付成功”把订单置为 `PAID`；只能由后端 Webhook 推进。
- 订单状态来自服务端响应；本地仅做展示映射。
- 取消只允许 `PENDING_PAYMENT`；其他状态需走退款。

## 常用命令

```bash
pnpm --filter @creatorhub/web test -- orders
```

## 交叉引用

- `payments/AGENTS.md` — 支付客户端。
- `lib/stripe/AGENTS.md` — Stripe Payment Element。
- `checkout/AGENTS.md` — 结算页。
