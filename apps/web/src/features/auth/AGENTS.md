# auth feature

> 登录、注册、刷新、注销、`/me`。配合 `lib/auth` 与 `lib/api`。

## 进入条件

- 调整 `/auth/login`、`/auth/register`、`/auth/logout` 页面与表单。
- 调整 Refresh Cookie 行为或会话刷新逻辑。

## 必读

- 根 `AGENTS.md` §4 前端规则、§7 安全红线。
- `docs/openapi.yaml` 标签 `Auth`。
- 14 周计划 Week 3。

## 约束

- Access Token 仅放内存；Refresh Cookie 由浏览器自动管理。
- 表单使用 React Hook Form + Zod；展示服务端字段错误。
- 错误解析统一走 `lib/api/problem`，不在页面散落状态码判断。
- 登录、注册、刷新等写操作携带 `X-Request-Id` 与对应幂等键。

## 常用命令

```bash
pnpm --filter @creatorhub/web test -- auth
```

## 交叉引用

- `lib/api/AGENTS.md` — API client 与 Problem Details。
- `lib/auth` — 会话状态。
