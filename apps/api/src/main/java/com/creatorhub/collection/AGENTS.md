# collection 限界上下文

> 通用域。收藏（Favorite）幂等关系。

## 进入条件

- 修改 `/favorites/*`、`/me/favorites`。
- 调整 `FavoriteCollection` 聚合或幂等行为。

## 必读

- 根 `AGENTS.md` §3。
- 根 `DEVELOPMENT_GUIDE.md` §4.2。
- `docs/openapi.yaml` 标签 `Favorites`。
- 14 周计划 Week 6。

## 聚合与事件

- 聚合：`FavoriteCollection`。
- 值对象：`UserId`、`ArtworkId`。
- 内部状态变化不发布集成事件（MVP）。

## 约束

- 同一作品只允许收藏一次；PUT/DELETE 幂等。
- 收藏数 `favoriteCount` 写入 `Artwork` 摘要（Catalog）需要 Catalog 投影；当前 MVP 通过查询侧组合。
- 跨上下文只持有 `ArtworkId`，不导入 Catalog 实体。

## 常用命令

```bash
cd apps/api
mvn -Dtest='com.creatorhub.collection.**.*Test' test
```

## 交叉引用

- Catalog：通过 `ArtworkId` 校验作品存在与状态。
- 通知：未来可发布 `ArtworkFavorited` 事件。
