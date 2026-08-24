# favorites feature

> 收藏/取消收藏、我的收藏列表。

## 进入条件

- 调整 `/favorites/{artworkId}` 或 `/me/favorites`。
- 调整收藏按钮与乐观更新。

## 必读

- 根 `AGENTS.md` §4、§5。
- `docs/openapi.yaml` 标签 `Favorites`。
- 14 周计划 Week 6。

## 约束

- 收藏/取消收藏使用乐观更新 + 失败回滚。
- 我的收藏页使用相同的 `ArtworkPage` 模型，复用 `artworks` 列表组件。
- 同一作品重复操作幂等；不展示中间态。

## 常用命令

```bash
pnpm --filter @creatorhub/web test -- favorites
```

## 交叉引用

- `artworks/AGENTS.md` — 详情页收藏按钮。
- `lib/api/AGENTS.md` — 统一错误处理。
