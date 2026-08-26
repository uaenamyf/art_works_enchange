# identity 限界上下文

> 支撑域。负责注册、登录、Refresh Cookie 轮换、Spring Security 角色控制。

## 进入条件

- 修改或新增 `/auth/*` 或 `/me` 相关 API。
- 调整 `User` 聚合、`PasswordHash`、`Email` 值对象。
- 涉及限流（登录/注册/刷新）。

## 必读

- 根 `AGENTS.md` §3、§7（安全红线）。
- 根 `DEVELOPMENT_GUIDE.md` §4.2 端口归属、§8 Refresh Cookie 与限流。
- `docs/openapi.yaml` 标签 `Auth`、`Health`。
- 14 周计划 Week 3 验收项。

## 聚合与事件

- 聚合：`User`。
- 领域事件：`UserRegistered`（内部门控）、`UserDisabled`。
- 不对外发布集成事件。

## 约束

- `domain` 纯 Java：密码仅存 Argon2id，禁用用户不能登录。
- 限流只针对登录/注册/刷新/注销，Order/Payment 限流在各自上下文。
- `interfaces/dto`、`infrastructure/persistence` 的 Row/DO 与 `domain/model` 显式转换；不返回 PersistenceRow/DO。
- Refresh Token 轮换查询必须在 Mapper XML 中使用 PostgreSQL `FOR UPDATE`，禁止用 Wrapper 拼接锁语义。
- 禁止在 Cookie 中放置 Access Token；Refresh Cookie 属性遵守 §8 描述。

## 常用命令

```bash
cd apps/api
mvn -Dtest='com.creatorhub.identity.**.*Test' test
```

## 交叉引用

- Creator、Ordering 等通过 `application/port/in` 调用 `IdentityQueries` 读取 `UserId` 等标识。
- 通知：未来 `UserDisabled` 可能驱动通知。
