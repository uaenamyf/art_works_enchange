# lib/stripe

> Stripe Payment Element 适配层。负责 `client_secret` 注入、提交与 3DS 错误处理。

## 进入条件

- 调整 Stripe Elements 加载、提交或错误提示。
- 引入新的 Stripe 受支持支付方式。

## 必读

- 根 `AGENTS.md` §4、§7。
- `docs/openapi.yaml` 标签 `Payments`。
- 14 周计划 Week 8、Week 11。

## 约束

- `clientSecret` 仅从后端 `/orders/{id}/payment-intent` 获取；不从前端构造。
- Payment Intent 提交失败统一走 Problem Details 与 toast；不修改订单状态。
- 不在 Stripe Element 中保存任何 PII；所有地址信息走订单接口。

## 常用命令

```bash
pnpm --filter @creatorhub/web test -- lib/stripe
```

## 交叉引用

- `features/payments/AGENTS.md` — 支付意图与结果页。
- `features/orders/AGENTS.md` — 订单状态展示。
