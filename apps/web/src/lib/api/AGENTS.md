# lib/api

> OpenAPI 客户端、Problem Details 解析、TanStack Query 通用 hooks。

## 进入条件

- 调整生成的 API 客户端或统一错误处理。
- 新增/调整共享数据获取 Hook。

## 必读

- 根 `AGENTS.md` §4、§7。
- `docs/openapi.yaml`。
- 14 周计划 Week 1、Week 12。

## 约束

- 不手写与 OpenAPI 重复的 DTO；类型与 client 必须从 `docs/openapi.yaml` 生成。
- 所有请求自动注入 `X-Request-Id`。
- 错误统一解析为 `Problem` 形状，业务码透传到上层。
- 受保护接口未认证时跳转登录，并保留 `returnTo`。
- 业务错误（库存、支付、限流）保留服务端 `code`，用于本地化文案。

## 常用命令

```bash
pnpm --filter @creatorhub/web lint
pnpm --filter @creatorhub/web test -- lib/api
```

## 交叉引用

- `lib/auth/AGENTS.md` — 会话与 Refresh 刷新。
- `lib/format/AGENTS.md` — 货币、日期格式化。
- `lib/stripe/AGENTS.md` — 支付客户端。
