# CreatorHub API（apps/api）智能体规范

> 范围：只描述 `apps/api` 的进入路径与后端专属约束。  
> 团队级规则见根 `AGENTS.md`；架构与端口归属见根 `DEVELOPMENT_GUIDE.md` §4；Claude 执行节奏见根 `CLAUDE.md`。

## 1. 何时打开本文件

- 你正在处理或计划处理 `apps/api/**` 下的代码、依赖、Flyway、测试或构建。
- 你需要判断某个变更应落入哪个限界上下文或哪一层。

## 2. 必读

1. 根 `AGENTS.md` §3 后端规则。
2. `DEVELOPMENT_GUIDE.md` §4 后端 DDD 设计（含端口归属表）。
3. `docs/openapi.yaml` 当前 API 与错误模型。
4. 涉及支付/订单：`docs/plan/development-plan.md` Week 7-9 任务。
5. 统一语言：`docs/domain/ubiquitous-language.md`；事件：`docs/domain/event-catalog.md`。

## 3. 模块化单体结构

```text
com.creatorhub
├── sharedkernel/   # Money、DomainEvent、强类型 ID、Problem Details 基础
├── identity/       # 注册、登录、Refresh Cookie、Spring Security
├── creator/        # Creator 档案、Follow
├── catalog/        # Artwork 聚合、媒体适配器、PostgreSQL FTS
├── collection/     # FavoriteCollection
├── ordering/       # Order 聚合、价格快照、库存预占
├── payment/        # PaymentIntent、Stripe Webhook、Payment 状态机
├── notification/   # 站内通知、邮件适配器
├── studio/         # Studio 接口 + Dashboard 只读投影（不独立）
├── webhook/        # 通用 Webhook 验签与基础设施
├── outbox/         # 跨上下文事件投递
├── config/         # Spring 配置
└── observability/  # 日志、指标、健康检查
```

每个业务上下文固定四层：`domain / application / interfaces / infrastructure`，再按需 `domain/{model,event,repository,service}` 与 `application/{port/in,port/out,service,command,query}`。

子模块有各自的 `AGENTS.md`，按需打开：

- `sharedkernel/AGENTS.md` — 共享内核与不可变值对象。
- `identity/AGENTS.md` — 认证、Refresh Cookie、密码哈希。
- `creator/AGENTS.md` — Creator 档案与 Follow 关系。
- `catalog/AGENTS.md` — Artwork 聚合、媒体、搜索、库存。
- `collection/AGENTS.md` — 收藏幂等。
- `ordering/AGENTS.md` — 订单状态机、价格快照、库存预占。
- `payment/AGENTS.md` — Stripe PaymentIntent、Webhook 验签。
- `notification/AGENTS.md` — 通知通道适配器。
- `studio/AGENTS.md` — Studio 接口与只读投影。
- `webhook/AGENTS.md` — 通用 Webhook 验签。
- `outbox/AGENTS.md` — Outbox 投递与死信。
- `config/AGENTS.md`、`observability/AGENTS.md` — 基础设施配置。

## 4. 常用命令

```bash
cd apps/api
mvn test
mvn spotless:check
mvn spring-boot:run -Dspring-boot.run.profiles=local
```

## 5. 关键约束

- 任何跨上下文协作都通过 `application/port/in` 或 Outbox 集成事件；不直接 import 其他上下文的 Repository、Entity 或表。
- Domain 不得依赖 Spring/MyBatis-Plus/Stripe/S3/Redis SDK。
- Mapper、PersistenceRow/DO 和 Mapper XML 只属于 `infrastructure/persistence`；Controller、application 与 domain 禁止直接依赖 Mapper。
- Repository Adapter 负责领域聚合与 PersistenceRow/DO 显式转换；关键写 SQL 优先 XML 并使用 PostgreSQL Testcontainers 验证。
- 业务规则只写在聚合方法或 `domain/service`；ApplicationService 只编排。
- 写操作必须做对象级权限校验；金额、支付状态、库存不信任客户端。
- 新增或修改表结构必须增加 Flyway migration；不要修改已共享的 migration。

## 6. 交叉引用

- 上下文映射：根 `DEVELOPMENT_GUIDE.md` §4.1。
- 端口归属：根 `DEVELOPMENT_GUIDE.md` §4.2。
- 订单/支付/Outbox：根 `DEVELOPMENT_GUIDE.md` §4.4。
- 安全与限流：根 `DEVELOPMENT_GUIDE.md` §8。
