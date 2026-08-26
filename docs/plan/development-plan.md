# CreatorHub 14 周 MVP 开发计划（精简版）

> 角色视角：CTO + Tech Lead + Product Engineer
> 适配对象：2–5 人小团队从 0 到 1
> 现状：仓库仅含 `apps/web`、`apps/api` 空目录骨架与文档基线。
> 目标：以最小范围在 14 周内跑通“发现 → 关注/收藏 → 下单 → Stripe 支付 → 履约 → Studio”核心闭环，并具备生产部署与基础监控能力。

## 0. 战略不动摇

- **架构**：DDD Modular Monolith + Contract First + Domain First。
- **技术栈**：PostgreSQL、Redis、Docker Compose、Next.js + TypeScript、Spring Boot 3 + Java 21、Stripe、Flyway、Outbox、Playwright、Vitest、ArchUnit、GitHub Actions。
- **生产部署**：AWS ECS/Fargate + RDS + S3 + CloudFront + ElastiCache，**不上 Kubernetes**。

## 1. 明确不做什么（Phase 2+）

- Kubernetes / EKS
- Kafka / 消息总线
- Elasticsearch / OpenSearch
- Vector DB / 双塔 / ML 推荐
- 多 Region
- Service Mesh
- 复杂 CQRS、Event Sourcing
- 微服务拆分
- 复杂 Observability Stack（用轻量 Sentry + CloudWatch + 自托管 Grafana）
- 实时聊天 / 复杂通知路由
- 大量 ADR 仪式化（只在关键决策点写 ADR-0001 ~ 0003 即可）
- 150+ Figma Components

## 2. 通用节奏（每周都执行）

1. **Contract first**：任何跨进程 API 先改 `docs/openapi.yaml`，CI 跑 `redocly lint`。
2. **Domain first**：在 `apps/api` 先写聚合、值对象、领域事件、单元测试。
3. **Adapter second**：application service + MyBatis-Plus/外部适配器 + Testcontainers 集成测试。
4. **Interface last**：Controller、API DTO、Problem Details 映射、ArchUnit 边界测试。
5. **Frontend follow**：用 OpenAPI 生成客户端，覆盖 loading/empty/error/success 四态。
6. **E2E 校验**：关键路径 Playwright 全绿后，本周才视为完成。
7. **范围控制**：发现会拖慢 MVP 的需求一律进 Icebox，本周不接。

## 3. 团队假设

- 后端 2：负责 Spring Boot、DDD 边界、Payment、Outbox、CI、运维脚本。
- 前端 1–2：负责 Next.js、Design System、Studio、Stripe Element、E2E。
- 全栈 + 运维共用 1：Compose、部署脚本、监控告警、Backups。
- 不要求每人独立负责一端，鼓励 PR 互相 review；CI 是真实守门人。

---

## 4. 周计划

### Week 1：工程基座

**目标**：项目能跑、能构建、能测。

**后端**

- Spring Boot 3 + Java 21 + Maven。
- 依赖：`spring-boot-starter-web`、`-security`、`-validation`、`-actuator`、MyBatis-Plus Boot 3 Starter、springdoc-openapi、flyway-core、postgresql、lettuce、jjwt、archunit-junit5、spring-modulith-test。
- `CreatorHubApplication`、`sharedkernel` 骨架：`Money`、`AggregateRoot`、`DomainEvent`、`StrongId`、`Clock`。
- 多 profile：`local` / `test` / `staging` / `production`。
- ArchUnit 基线规则：domain 不依赖 infrastructure；interfaces 不直接读 domain。
- Spring Boot Actuator + Micrometer Prometheus endpoint。

**前端**

- Next.js 15 + TypeScript + Tailwind 4 + ESLint Flat Config。
- pnpm workspace + 根脚本（`web:dev|build|test|lint|typecheck`）。
- 根 `layout.tsx`、Design Token 占位、字体、404 / 500。
- Problem Details 解析工具与 `useRequestId`。

**基础设施**

- `infra/compose.yaml`：`postgres`、`redis`、`minio`、`api`、`web`、健康检查、共享网络、卷。
- `infra/env.example`，不含真实密钥。
- Dockerfile（多阶段）。
- GitHub Actions：`backend-ci`（Maven 测试 + ArchUnit）、`frontend-ci`（pnpm lint/typecheck/test）、`contract-ci`（redocly lint）、`docker-ci`（镜像构建）。

**Week 1 验收**

- `docker compose -f infra/compose.yaml up` 成功，API `/actuator/health` 200。
- Web 首页能打开。
- Flyway 迁移成功一次。
- CI 全绿。

---

### Week 2：设计骨架 + Discover 静态

**目标**：用户能打开三个核心页面（Discover、Artwork Detail、Creator Profile）的可点击骨架。

**设计系统**（只做 MVP 必需，不做 150 个组件）

- Token、Navbar、Button、Input、Modal、Tabs、Skeleton。
- `ArtworkCard`、`CreatorCard`、`MediaGallery` 骨架，使用 4:5 / 16:9 占位。

**前端页面**

- `app/(market)/page.tsx`：Discover 骨架 + 1 段 hero。
- `app/(market)/artworks/[id]/page.tsx`：Artwork Detail 骨架。
- `app/(market)/creators/[id]/page.tsx`：Creator Profile 骨架。
- 全部页面通过 mock 数据渲染，对接下一周才接的 API 客户端。

**Week 2 验收**

- 用户能 Discover → Artwork Detail → Creator Profile 跳转，UI 状态完整。
- Vitest 覆盖 3 个核心组件快照与交互。
- Lint、typecheck 通过。

---

### Week 3：Identity + Creator

**目标**：注册登录、Creator 入驻。

**Identity 上下文**

- 聚合：`User`；值对象：`Email`、`PasswordHash`、`UserId`。
- 应用服务：`RegisterUser`、`IssueSession`、`RotateSession`、`RevokeSession`。
- API：`/auth/register`、`/auth/login`、`/auth/refresh`、`/auth/logout`，遵守 Refresh Token Cookie 与轮换契约。
- 错误：`EMAIL_TAKEN`、`INVALID_CREDENTIALS`、`RATE_LIMITED`。
- 限流：登录/注册/刷新。

**Creator 上下文**

- 聚合：`Creator`；不变量：一个 UserId 一个 Creator。
- 应用服务：`ApplyForCreator`、`UpdateCreatorProfile`。
- API：`/me/creator`、`/creators/{id}`。

**权限**

- 角色：`USER`、`CREATOR`、`ADMIN`。
- Spring Security 默认拒绝，方法级权限注解。

**前端**

- `/auth/login`、`/auth/register` 页面，Server Action + 客户端 fallback。
- Creator 申请表单。

**Week 3 验收**

- 用户能注册、登录、刷新、注销。
- 登录后能申请 Creator 并看到自己的 Creator 主页。
- ArchUnit：identity 与 creator 之间只通过 `application/port/in` 调用。
- 集成测试：Testcontainers PostgreSQL + 真实 Argon2id。

---

### Week 4：Catalog + Media

**目标**：Creator 能上传图片、发布作品；消费者能看到作品。

**Catalog 上下文**

- 聚合：`Artwork`；值对象：`Money`、`Stock`、`MediaSet`、`ArtworkId`。
- 不变量：发布前至少 1 张媒体；库存非负；价格有效。
- 端口：`ArtworkRepository`、`MediaStoragePort`（S3/MinIO 输出端口）、`StockAvailabilityPort`（供 Ordering 使用）。
- 应用服务：`CreateArtworkDraft`、`UpdateArtwork`、`PublishArtwork`、`ArchiveArtwork`。
- 搜索：PostgreSQL FTS + `pg_trgm`。
- 领域事件：`ArtworkPublished` → Outbox。

**媒体流程**

- `/media/upload-requests` 颁发预签名 PUT。
- 浏览器直传 MinIO/S3。
- 服务端在媒体归属上做对象级授权。

**前端**

- `studio/artworks/new`：上传组件 + 草稿。
- `studio/artworks/[id]`：编辑。
- 公共详情页接真实 API。

**Week 4 验收**

- Creator：上传图片 → 创建设置 → 发布。
- 消费者：作品详情页能看到图片。
- Vitest + Playwright 覆盖：上传成功/失败、发布前缺图阻止。
- 集成测试：FTS 查询、预签名适配器。

---

### Week 5：Discover + Search + Creator Profile（消费者侧完整）

**目标**：消费者侧所有状态机完整，搜索可用。

**前端**

- Discover：Latest、Popular、Trending、Category。
- Marketplace：搜索、筛选、游标分页。
- Artwork Detail：完整字段、媒体画廊、收藏按钮占位、关注按钮占位。
- Creator Profile：完整字段、Grid 布局、作品瀑布流。
- Empty / Loading / Error / Success 四态全覆盖。
- 无障碍：键盘可达、焦点可见、44px 触控目标。

**后端补强**

- 列表/搜索响应、详情 DTO 完整。
- 推荐：**仅规则**：latest、popular、trending、category 同类去重。

**Week 5 验收**

- 端到端：搜索 → 列表 → 详情 → 创作者 → 返回。
- Lighthouse 移动端基线（无障碍 ≥ 90，性能 ≥ 80）。
- 不引入 ML 推荐。

---

### Week 6：Follow + Favorite

**目标**：用户能关注/取消关注、收藏/取消收藏，并在刷新后保持。

**Collection 上下文**

- 聚合：`FavoriteCollection`。
- API：`PUT/DELETE /favorites/{artworkId}`、`GET /me/favorites`。

**Creator Follow**

- 同一上下文内新增 `Follow` 聚合与方法。
- API：`PUT/DELETE /creators/{id}/follow`、`GET /creators/{id}/followers`、`GET /me/follows`。
- 幂等：重复操作返回当前状态，不重复产生事件。

**前端**

- 详情页和创作者页：Follow / Favorite 切换按钮，乐观更新 + 失败回滚。
- 我的收藏页、个人中心 follow 列表。

**Week 6 验收**

- 刷新后状态保持。
- 幂等测试通过（重复 PUT/DELETE 返回相同结果）。
- ArchUnit：collection 不直接 import catalog 的 Mapper、PersistenceRow/DO 或 infrastructure。

---

### Week 7：Ordering

**目标**：下单链路 + 库存不能超卖。

**Ordering 上下文**

- 聚合：`Order`；值对象：`OrderLine`、`Address`、`Money`。
- 不变量：金额使用价格快照；订单项创建后不可变；状态机严格。
- 应用服务：`CreateOrder`、`CancelPendingOrder`、`GetOrder`、`ListMyOrders`。
- 关键事务：单一事务内使用条件更新 `stock >= quantity` 预占库存，并写 `outbox_events`。
- 订单超期定时任务：返还库存必须幂等。
- 复用 Catalog `StockAvailabilityPort`，Ordering 不直接读 `artworks` 表。

**前端**

- 结算页骨架：地址、订单摘要、跳支付占位。
- 我的订单列表 + 详情。
- 不在本周接 Stripe。

**Week 7 验收**

- 高并发库存测试：100 并发抢 10 库存，最多成交 10 笔。
- 订单超期返还库存幂等。
- 集成测试覆盖条件更新失败、并发预占、超时返还。

---

### Week 8：Stripe Payment

**目标**：真实支付闭环 + Webhook 幂等。

**Payment 上下文**

- 适配器：Stripe PaymentIntent、Webhook 验签、退款（stub）。
- 应用服务：`CreatePaymentIntent`、`MarkPaymentSucceeded`、`MarkPaymentFailed`。
- 集成事件：`PaymentSucceeded`、`PaymentFailed` → Outbox。
- API：`POST /orders/{id}/payment-intent`（`Idempotency-Key`）、`POST /payments/webhooks/stripe`（`Stripe-Signature` 验签）。
- **强约束**：前端不得直接修改订单状态为 `PAID`；只能由验签后的 Webhook 推进。

**编排**

- Ordering 消费 `PaymentSucceeded` 推进订单状态，幂等键 `eventId + orderId`。

**前端**

- Stripe Payment Element 集成。
- 支付结果页、成功/失败 toast。

**Week 8 验收**

- Stripe 测试模式：成功支付、失败支付、3DS、重复 Webhook 全部通过。
- PaymentIntent 重放相同 `Idempotency-Key` 返回同一结果。
- Webhook 验签失败的请求全部拒绝。
- 集成测试：覆盖 Stripe 异常码、超时。

---

### Week 9：Outbox + Notification

**目标**：可靠事件投递 + 基础通知。

**Outbox**

- `outbox_events` 表：事件 ID、类型、版本、聚合信息、payload、attempts、next_attempt_at、locked_until、status、last_error、occurred_at、trace_id。
- `OutboxPoller`（Spring `@Scheduled` 每秒），`FOR UPDATE SKIP LOCKED` 并发领取。
- 退避：指数 + 抖动；超过 N 次进 `DEAD` 并告警。
- 消费者幂等：状态机守卫 + 事件去重表。

**Notification 上下文**

- MVP 事件：`ArtworkPublished`、`PaymentSucceeded`、`PaymentFailed`、`OrderStatusChanged`。
- 通道：站内通知 + 邮件 stub（控制台 + 适配器接口），不接真实 SMTP。

**Week 9 验收**

- 注入失败：验证重试、退避、死信告警。
- 重复消费：不会产生重复副作用。
- 邮件/通知适配器可在测试中被替换。

---

### Week 10：Creator Studio

**目标**：Creator 能管理作品和订单。

**Studio 接口**

- `GET/POST /studio/artworks`、`PUT /studio/artworks/{id}`、`POST /studio/artworks/{id}/publish`。
- `GET /studio/orders`、`PATCH /studio/orders/{id}/status`。
- `GET /studio/dashboard`：收入、订单数、已发布作品、关注者数。
- Studio 不新增领域，只调用 Creator、Catalog、Ordering 公开端口 + 只读投影。

**前端**

- `app/studio/layout.tsx`：侧边栏 + 信息密度布局。
- Dashboard：4 个核心数字卡。
- 作品列表/编辑/发布：乐观锁版本号，字段错误展示。
- 订单列表/详情/履约：状态推进按钮。

**Week 10 验收**

- 创作者 E2E：发布作品 → 用户下单 → 创作者接单 → 填物流 → 状态推进。
- 权限测试：非创作者访问 Studio 全部 403；创作者只能操作自己的资源。

---

### Week 11：Checkout + UX 完整化

**目标**：完成消费者端从作品到订单的端到端体验。

**前端**

- Checkout：地址表单（React Hook Form + Zod）、订单摘要、Stripe Element、错误展示。
- Order Result：成功/失败/待处理三态。
- Order History：分页、状态筛选、空态。
- Mobile responsive：375 / 768 / 1440。
- 动效：160–240ms，遵守 `prefers-reduced-motion`。

**后端补强**

- Order 详情返回最新状态和支付信息。
- 幂等与错误响应在所有写接口完整。

**Week 11 验收**

- 完整链路：Artwork → Checkout → Pay → Order Success。
- 移动端三档断点视觉检查无溢出/遮挡。
- Lighthouse 性能 ≥ 85，无障碍 ≥ 95。

---

### Week 12：安全 + E2E + Bug Fix

**目标**：补齐安全基础与端到端测试。

**安全**

- Refresh Token Cookie：HttpOnly + Secure + SameSite=Lax，Path `/api/v1/auth`。
- CSRF：写入操作 Origin/Referer 校验 + SameSite Cookie，必要时引入双重提交。
- CORS：仅允许生产域名。
- Rate Limit：登录/注册/刷新/支付意图。
- Input Validation：Bean Validation + 自定义业务校验。
- Object-level Authorization：每个 Controller 校验资源 owner。
- Stripe Webhook Signature：所有路径强制验签。
- 依赖扫描：GitHub Dependabot + OWASP dependency-check。
- 日志：禁记密码、Token、Cookie、支付信息、完整地址与邮箱。

**E2E（4 条核心）**

1. **Discover**：搜索 → 列表 → 详情 → 返回。
2. **Purchase**：作品 → 下单 → Stripe 测试支付 → 订单 PAID。
3. **Creator Publish**：申请 Creator → 上传作品 → 发布 → 消费者可见。
4. **Creator Fulfillment**：用户下单 → 创作者接单 → 发货 → 状态推进。

**Week 12 验收**

- 4 条 E2E 全绿。
- OWASP ASVS L1 检查清单通过。
- 没有高危 CVE。

---

### Week 13：生产就绪

**目标**：能上生产并具备最小运维能力。

**镜像与部署**

- `web`、`api` 多阶段生产镜像，非 root、只读根文件、健康检查。
- 部署目标：AWS ECS/Fargate + RDS + ElastiCache + S3 + CloudFront。
- 部署脚本：`infra/deploy/` 下的 `deploy.sh`、`rollback.sh`。
- 密钥：AWS Secrets Manager 或 Parameter Store。

**备份与恢复**

- RDS 自动备份 + 至少一次恢复演练。
- 对象存储生命周期策略。

**监控告警（轻量）**

- Sentry（前端 + 后端），过滤敏感字段。
- CloudWatch 关键指标：5xx、API P95、Stripe Webhook 失败、Outbox 死信、PostgreSQL 连接数、卷容量。
- 告警通知到值班人。

**Week 13 验收**

- `docker compose build` 全绿。
- 部署脚本在 staging 演练成功并能回滚。
- 监控仪表盘上线，至少 6 个告警规则。

---

### Week 14：Beta 启动

**目标**：邀请真实 Creator 与消费者，收集数据，不做新功能。

**Creator Beta**

- 邀请 10–20 位真实 Creator 上传作品。

**Consumer Beta**

- 邀请 50–100 位真实用户浏览、关注、收藏、下单。

**观察指标**

- 浏览率
- Creator Follow 数
- Favorite 数
- Checkout 启动率
- 支付成功率
- 7 日回访率
- 订单履约完成率
- 客诉 / 退款率

**本周动作**

- 修 P0/P1 Bug。
- 性能检查：首页 P95 < 1.5s，下单链路 P95 < 500ms。
- 视觉与无障碍检查。
- 准备 Production 切换清单。

**Week 14 验收**

- Creator Beta：≥ 10 位完成首单。
- Consumer Beta：至少 5 笔真实 Stripe 测试或小额真实支付完成。
- 关键指标可观测。
- 任何 P0 必须在切换生产前修复。

---

## 5. DDD 限界上下文（最终范围）

```text
identity
creator
catalog
collection
ordering
payment
notification
sharedkernel
```

模块化单体内部保留这些边界，跨上下文只允许：

- 同步：通过对方 `application/port/in` 调用，传输强类型 ID 或不可变快照。
- 异步：通过 Outbox 发布集成事件，下游消费幂等。

不引入读写分离、Event Sourcing、Command Bus、Materialized View 基础设施。

## 6. Icebox（Phase 2+）

- 复杂推荐 / 个性化排序
- 评论与评分
- 二手交易
- 订阅 / 会员
- 多卖家购物车
- 复杂 CMS / Story 编辑器
- 原生 iOS / Android
- 多语言 / 多币种
- AI 创作助手

## 7. 关键风险与缓解

| 风险 | 缓解 |
+| --- | --- |
+| 库存超卖 | Week 7 高并发测试 + 条件更新 + Outbox |
+| Stripe Webhook 重复 | Week 8 幂等测试 + 事件 ID 唯一索引 + 状态机守卫 |
+| 上下文边界退化 | ArchUnit 在 CI 卡口，违反即失败 |
+| 前端接口漂移 | OpenAPI 强制生成客户端 |
| 工具链不稳定 | 锁定 Node 22、pnpm 11、Java 21、Maven 3.9 |
+| 进度失控 | 每周五 scope review；超额需求强制进 Icebox |
+| Creator 冷启动 | Week 14 前 1 周开始 BD 邀约 |

## 8. 文档与 ADR

只写 3 个核心 ADR，足够支撑 MVP：

- ADR-0001：DDD Modular Monolith（不拆微服务）。
- ADR-0002：Refresh Token Cookie 旋转与清除。
- ADR-0003：PostgreSQL Outbox + Scheduled Worker（不引 Kafka）。

其他设计决策直接写进本计划或 `DEVELOPMENT_GUIDE.md`，不单独开 ADR 编号。
