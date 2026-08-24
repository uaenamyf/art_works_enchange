# creator 限界上下文

> 核心域。Creator 档案、申请、Follow 关系。

## 进入条件

- 修改 `/creators/*`、`/creators/{id}/follow`、`/me/creator`。
- 调整 `Creator` 聚合或 `Follow` 关系。
- 创作者资料与统计接口。

## 必读

- 根 `AGENTS.md` §3。
- 根 `DEVELOPMENT_GUIDE.md` §4.1 上下文映射、§4.2 端口归属。
- `docs/openapi.yaml` 标签 `Creators`、`Follows`。
- 14 周计划 Week 3、Week 6 验收项。

## 聚合与事件

- 聚合：`Creator`、`Follow`。
- 集成事件：`CreatorFollowed` v1 → Notification（发布时只对外发送过去式事件）。

## 约束

- 一个 `UserId` 仅能绑定一个 `Creator`；不能关注自己。
- `Follow` 操作幂等：重复 PUT 返回当前状态，不重复产生事件。
- `CreatorSummary` 是 Catalog/Ordering 使用的不可变快照，跨上下文通过 port/in 获取或重新组装。
- 禁止跨上下文 JPA 关联。

## 常用命令

```bash
cd apps/api
./gradlew test --tests 'com.creatorhub.creator.*'
```

## 交叉引用

- Identity：依赖 `IdentityQueries` 输入端口拿 `UserId`、Email 等。
- Catalog：`Artwork.creatorId` 引用 `CreatorId`，跨上下文只用 ID。
- Notification：消费 `CreatorFollowed`。
