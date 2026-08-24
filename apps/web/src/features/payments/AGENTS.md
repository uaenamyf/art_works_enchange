# payments feature

> 支付意图、Stripe Payment Element、成功/失败 toast、结果页。

## 进入条件

- 调整支付意图获取或结果页。
- 调整支付错误展示与重试。

## 必读

- 根 `AGENTS.md` §4、§7。
- `docs/openapi.yaml` 标签 `Payments`。
- 14 周计划 Week 8、Week 11。

## 约束

- 不在前端携带或计算金额；金额来自后端 `OrderDetail.total`。
- `Idempotency-Key` 必须在每次创建 PaymentIntent 时生成新的 UUID。
- Stripe `client_secret` 仅传递给 Payment Element；不写入 localStorage 或日志。
- 支付成功页不直接修改订单状态；仅提示并跳转订单详情。
- 3DS、SCA 失败统一走 Problem Details 解析与 toast。

## 常用命令

```bash
pnpm --filter @creatorhub/web test -- payments
```

## 交叉引用

- `lib/stripe/AGENTS.md` — Stripe 适配。
- `orders/AGENTS.md` — 订单展示。
- `checkout/AGENTS.md` — 结算页流程。
