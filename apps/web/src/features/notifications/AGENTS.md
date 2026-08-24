# notifications feature

> 站内通知列表、标记已读。

## 进入条件

- 调整 `/notifications`、`/notifications/[id]/read`。
- 调整通知中心 UI 与未读数提示。

## 必读

- 根 `AGENTS.md` §4、§5。
- `docs/openapi.yaml` 标签 `Notifications`。
- `docs/domain/event-catalog.md`。
- 14 周计划 Week 9。

## 约束

- 通知类型映射使用 OpenAPI 枚举；新增类型必须先改 `docs/openapi.yaml`。
- 标记已读后从本地未读计数中扣减；以服务端响应为最终事实。
- 通知正文不展示敏感信息（邮箱、完整地址、支付信息）。

## 常用命令

```bash
pnpm --filter @creatorhub/web test -- notifications
```

## 交叉引用

- `lib/api/AGENTS.md` — 通知列表与已读接口。
