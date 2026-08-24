# catalog 限界上下文

> 核心域。Artwork 聚合、媒体上传、PostgreSQL FTS、库存接口。

## 进入条件

- 修改 `/artworks/*`、`/media/upload-requests`。
- 调整 `Artwork` 聚合、媒体适配器或搜索。
- 库存预占对外端口（被 Ordering 调用）。

## 必读

- 根 `AGENTS.md` §3。
- 根 `DEVELOPMENT_GUIDE.md` §4.2 端口归属、§4.4 库存与支付一致性。
- `docs/openapi.yaml` 标签 `Artworks`、`Media`、`Studio`（作品写操作）。
- 14 周计划 Week 4、Week 5、Week 10。

## 聚合与事件

- 聚合：`Artwork`。
- 值对象：`ArtworkId`、`Money`、`Stock`、`MediaSet`。
- 领域事件：`ArtworkPublished`、`ArtworkArchived`（领域层）。
- 集成事件：`ArtworkPublished` v1 → Notification。

## 约束

- 发布前至少 1 张媒体；价格与库存合法。
- `domain/repository` 端口只能由聚合根粒度的方法签名。
- 媒体上传使用预签名 PUT，强制 `Content-Type` 与大小限制，对象归属校验。
- 搜索先 PostgreSQL FTS + `pg_trgm`；不引入 Elasticsearch。
- 库存预占由 Ordering 调用 `application/port/in`，Catalog 不感知 Order 存在。
- 乐观锁字段 `version`；写请求必须携带最新版本。

## 常用命令

```bash
cd apps/api
./gradlew test --tests 'com.creatorhub.catalog.*'
```

## 交叉引用

- Ordering：`StockAvailabilityPort` 在 `application/port/in` 暴露。
- Creator：作品必须归属 `CreatorId`，禁止跨上下文 JPA 关联。
- Notification：消费 `ArtworkPublished`。
