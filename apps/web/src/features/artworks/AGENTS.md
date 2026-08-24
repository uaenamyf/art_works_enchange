# artworks feature

> 作品浏览、搜索、详情、Studio 作品管理。

## 进入条件

- 调整 `/artworks`、`/artworks/[id]`、Studio 作品相关页面。
- 调整搜索筛选、排序、分页与四态。

## 必读

- 根 `AGENTS.md` §4、§5。
- `docs/openapi.yaml` 标签 `Artworks`、`Media`、`Studio`。
- 14 周计划 Week 4、Week 5、Week 10。

## 约束

- 列表/搜索使用游标分页（`cursor` + `limit`）；筛选条件入 URL。
- 详情页使用 `next/image`，保留 4:5 比例；不允许 CSS 强行改比例。
- 收藏/关注按钮使用乐观更新 + 失败回滚。
- Studio 写操作必须携带最新 `version`；版本冲突展示 Problem Details。
- 媒体上传：先 `/media/upload-requests` 获取预签名 URL，再浏览器 PUT，最后在写接口中绑定 `mediaIds`。

## 常用命令

```bash
pnpm --filter @creatorhub/web test -- artworks
```

## 交叉引用

- `components/domain/artwork/AGENTS.md` — `ArtworkCard` 复用。
- `studio/AGENTS.md` — Studio 写操作。
- `favorites/AGENTS.md` — 收藏状态。
