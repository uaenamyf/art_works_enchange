# follows feature

> 关注/取消关注、粉丝列表、我关注的创作者。

## 进入条件

- 调整关注/取消关注按钮与乐观更新。
- 修改 `/creators/{id}/follow`、`/creators/{id}/followers`、`/me/follows`。

## 必读

- 根 `AGENTS.md` §4、§7。
- `docs/openapi.yaml` 标签 `Follows`。
- 14 周计划 Week 6。

## 约束

- 关注/取消关注使用乐观更新 + 失败回滚；服务端返回的状态是最终事实。
- 同一对 (user, creator) 操作幂等；不展示“已关注/未关注”之外的中间态。
- 不在前端模拟关注数；等待服务端响应后更新。

## 常用命令

```bash
pnpm --filter @creatorhub/web test -- follows
```

## 交叉引用

- `creators/AGENTS.md` — 创作者主页。
- `artworks/AGENTS.md` — 详情页的关注按钮。
