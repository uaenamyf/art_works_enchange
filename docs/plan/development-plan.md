# CreatorHub 14 周 MVP 开发计划（精简版）

> 角色视角：CTO + Tech Lead + Product Engineer
> 适配对象：2–5 人小团队从 0 到 1
> 现状：仓库仅含 `apps/web`、`apps/api` 空目录骨架与文档基线。
> 目标：以最小范围在 14 周内跑通“发现 → 关注/收藏 → 下单 → 支付 → 实体或数字履约 → Studio”核心闭环，并具备生产部署与基础监控能力。

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

## 3.1 产品形态与复用策略

CreatorHub 支持实体和数字两种履约类型，共用发现、Creator、价格快照、订单、幂等和 Outbox，不拆成两套商城。

| 维度 | 实体产品 `PHYSICAL` | 数字产品 `DIGITAL` |
| --- | --- | --- |
| 下单 | 数量 + 收货地址 | 数量通常为 1，不收集地址 |
| 库存 | PostgreSQL 条件扣减和释放 | 默认不扣实体库存；有限许可证另建额度 |
| 付款后履约 | 制作、发货、物流 | 创建购买授权，生成短期下载地址 |
| 个人信息 | 仅收集履约所需地址和电话 | `shipping_address` 必须为 NULL |

数字产品复用 `Artwork`、价格快照、`Order`、幂等键、支付占位和 Outbox；新增 `ProductType`、`DigitalAsset`、`Entitlement`、`DownloadGrant` 四个稳定概念。原始数字文件使用私有对象存储，不能与公开作品媒体混用。

关键不变量：服务端依据产品类型决定是否需要地址和库存；客户端不能通过发送空地址绕过校验；公开 API、日志和事件中不得出现原始 object key、签名 URL 或下载 Token。

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
- 不变量：发布前至少 1 张公开媒体；价格有效；`PHYSICAL` 库存非负，`DIGITAL` 至少绑定一个已扫描的数字资产版本。
- 端口：`ArtworkRepository`、`MediaStoragePort`（S3/MinIO 输出端口）、`StockAvailabilityPort`（供 Ordering 使用）。
- 数字资产端口：`DigitalAssetStore`、`MalwareScanner`、`DownloadGrantIssuer`；第三方扫描和对象存储 SDK 只能存在 infrastructure。
- 应用服务：`CreateArtworkDraft`、`UpdateArtwork`、`PublishArtwork`、`ArchiveArtwork`。
- 搜索：PostgreSQL FTS + `pg_trgm`。
- 领域事件：`ArtworkPublished` → Outbox。

**媒体流程**

- `/media/upload-requests` 颁发预签名 PUT。
- 浏览器直传 MinIO/S3。
- 服务端在媒体归属上做对象级授权。
- `GET /media/{mediaId}` 只为已绑定到 `PUBLISHED` 作品的媒体签发短期 GET URL。
- 数字文件使用独立私有 bucket/prefix；上传完成后经过扫描、哈希、MIME 和大小校验后才能绑定到数字作品版本。

**前端**

- `studio/artworks/new`：上传组件 + 草稿。
- `studio/artworks/[id]`：编辑。
- 公共详情页接真实 API，并通过短期媒体 URL 渲染画廊。
- Studio 根据产品类型显示实体库存或数字资产上传/版本管理；消费者只看到交付类型和文件规格，不看到原始存储地址。
- 页面视觉采用“独立首页 + 共享分类页”：`/` 展示品牌叙事和类别预览，`/categories/[category]` 负责分类搜索与作品网格。

**Week 4 验收**

- Creator：上传图片 → 创建设置 → 发布。
- 消费者：作品详情页能看到图片。
- Vitest 覆盖上传表单的无图提交阻止、媒体可见性和公开详情读取；Playwright 待补齐本地 E2E 基础设施后执行。
- 预签名上传、媒体归属、公开媒体状态门禁和作品发布链路已通过后端测试；FTS 查询待 Week 5/搜索数据模型完善后执行。

---

### Week 5：Discover + Search + Creator Profile（消费者侧完整）

**目标**：消费者侧所有状态机完整，搜索可用。

**前端**

- 独立首页 `/`：品牌叙事、按类别横向预览作品，不显示搜索栏。
- 分类页 `/categories/[category]`：共享模板、分类内搜索、分类切换和作品网格。
- Creator 目录 `/creators` 与 Creator Profile `/creators/[id]`：真实 API、分类筛选和公开作品展示。
- Artwork Detail：真实字段、短期媒体画廊和收藏按钮。
- 作品列表与 Creator 列表使用游标分页，并保留可分享 URL 状态。
- Empty / Loading / Error / Success 四态全覆盖。
- 无障碍：键盘可达、焦点可见、44px 触控目标。

**后端补强**

- `GET /artworks` 已支持已发布作品、关键词、类别、Creator 筛选和稳定游标分页。
- `GET /creators` 已支持类别筛选和稳定游标分页。
- 推荐：**待实现**；当前 `POPULAR`、`TRENDING` 暂不伪造统计排序，待收藏/趋势统计数据模型完成后按规则实现。

**Week 5 验收**

- 页面路由链路已具备：分类搜索 → 作品列表 → 详情 → Creator Profile → 返回；Playwright 端到端测试待补。
- Lighthouse 移动端基线（无障碍 ≥ 90，性能 ≥ 80）。
- 不引入 ML 推荐。

---

### Week 6：Follow + Favorite

**目标**：用户能关注/取消关注、收藏/取消收藏，并在刷新后保持。

> 进度备注：收藏基础链路已提前完成，包括 `PUT/DELETE /favorites/{artworkId}`、V6 收藏关系表、幂等约束，以及作品详情页乐观更新和失败回滚。

**Collection 上下文**

- 聚合：`FavoriteCollection`。
- API：`PUT/DELETE /favorites/{artworkId}`、`GET /me/favorites`。
- 已完成：`GET /me/favorites`、收藏列表游标分页，以及收藏页的认证、空态、错误态和成功态。
- 待补强：作品列表/详情中的真实 `favoriteCount` 与 `favoritedByMe` 投影。

**Creator Follow**

- 已完成：同一上下文内的 `Follow` 关系、V7 迁移及幂等关注/取消关注。
- 已完成 API：`PUT/DELETE /creators/{id}/follow`、`GET /creators/{id}/followers`、`GET /me/follows`。
- 已完成投影：Creator 列表/详情返回真实 `followerCount` 和认证用户的 `followedByMe`。
- 待补强：关注列表和粉丝列表的前端页面，以及完整 E2E 测试。

**前端**

- 详情页和创作者页：Follow / Favorite 切换按钮，乐观更新 + 失败回滚。
- 已完成我的收藏页和我的关注页；Creator 详情页使用服务端初始关注状态。

**Week 6 验收**

- 刷新后状态保持：收藏页和 Creator 详情页已接入真实 API 与服务端状态。
- 幂等测试通过：重复 PUT/DELETE 由唯一键、删除幂等和服务层测试覆盖。
- 读取 API 测试通过：我的关注、粉丝列表、认证与公开访问均有 Controller 覆盖。
- ArchUnit：collection 不直接 import catalog 的 Mapper、PersistenceRow/DO 或 infrastructure。

---

### Week 7：Ordering

**目标**：下单链路 + 库存不能超卖。

**Ordering 上下文**

- 聚合：`Order`；值对象：`OrderLine`、`Address`、`Money`。
- 不变量：金额使用价格快照；订单项创建后不可变；状态机严格。
- 应用服务：`CreateOrder`、`CancelPendingOrder`、`GetOrder`、`ListMyOrders`。
- 已完成基础链路：`POST /orders`、`Order` 聚合、价格快照、`PENDING_PAYMENT` 初始状态、地址校验和 `201 + Location` 响应。
- 已完成订单生命周期基础能力：`POST /orders/{orderId}/cancel`、待支付订单条件取消、过期扫描和库存幂等释放。
- 已完成库存审计基础能力：V10 `inventory_movements` 流水表，预占/释放使用 `reservation_id` 幂等记录。
- 关键事务：单一 PostgreSQL 事务内使用条件更新 `stock >= quantity` 预占库存，并写订单、库存流水和 `outbox_events`；PostgreSQL 是库存最终事实源。
- Redis 只作为高并发前置闸门和短期缓存：使用 Lua 脚本原子校验/扣减可用库存并生成带 TTL 的 reservation token，不能绕过数据库条件更新，也不能单独决定订单成功。
- Redis 预占成功但数据库事务失败时必须释放 token 并补回缓存；Redis 重启、淘汰、主从切换或缓存不一致时，从 PostgreSQL 重建库存缓存，宁可降级为数据库路径也不能放行超卖。
- 幂等记录使用 PostgreSQL 唯一约束保存“用户 + createOrder + Idempotency-Key + 请求摘要 + 响应快照”；Redis 锁只用于缩短竞争窗口，不能作为唯一幂等事实源。
- 已完成幂等入口：PostgreSQL 先 claim 幂等键，再执行库存和订单写入，成功后 complete；相同 Key 不重复扣库存，不同请求摘要返回冲突。
- 订单超期定时任务：返还库存必须幂等。
- 复用 Catalog `StockAvailabilityPort`，Ordering 不直接读 `artworks` 表。
- 创建订单根据作品类型分流：实体订单必须有地址并预占库存；数字订单数量通常为 1，地址必须为空，不扣实体库存。
- 订单表增加 `fulfillment_type`，并由数据库约束保证实体订单地址非空、数字订单地址为空。
- 数字订单支付确认后通过 Outbox 发布 `DigitalEntitlementRequested`，由数字交付模块幂等创建授权；订单详情只返回授权状态，不返回下载 URL。

**前端**

- 结算页骨架：地址、订单摘要、跳支付占位。
- 我的订单列表 + 详情。
- 当前已完成创建订单 HTTP 入口、结算页、订单列表和订单详情；支付跳转仍等待具体 Provider。
- 结算页按产品类型渲染不同表单：数字产品不展示地址字段，也不发送空地址对象。
- 本周不接具体支付 Provider，支付会话留到 Week 8 的统一 Payment Provider 端口。

**Week 7 验收**

- 高并发库存测试：100 并发抢 10 库存，最多成交 10 笔。
- Redis 正常、Redis 不可用、缓存重建和数据库条件更新失败四种路径均不得超卖；库存最终值与订单/库存流水一致。
- 相同幂等键同请求重放同一订单，不同请求摘要返回 `409 IDEMPOTENCY_KEY_REUSED`，并发请求只允许一个执行。
- 订单超期返还库存幂等。
- PostgreSQL Testcontainers 覆盖条件更新失败、并发预占、缓存降级、超时返还和事务回滚；Redis Testcontainers 覆盖 Lua 原子脚本。
- 当前剩余：Redis 缓存重建、真实 Redis Testcontainers 验收、Outbox 其余业务事件接入，以及实体/数字双履约的完整业务验收。
- 数字产品验收新增：地址不落库、数字订单不扣实体库存、授权幂等、下载越权和撤销授权测试。

---

### Week 8：Payment Providers

**目标**：保留统一支付端口和订单支付确认边界；微信支付、支付宝接入暂停，优先完成不依赖具体 Provider 的业务闭环。

> **范围调整（2026-08-28）**：微信支付 API v3 和支付宝 RSA2 暂停到后续迭代。本阶段不接入商户证书、私钥、沙箱联调或 Provider-specific 回调；数字产品使用受信任的支付成功事件测试替身推进授权、下载和 E2E。Stripe 也只保留接口边界，不阻塞实体/数字履约、Outbox、Studio 和安全工作。

**Payment 上下文**

- 领域端口不暴露第三方 SDK：`PaymentProviderPort` 统一抽象 `createPayment`、`queryPayment`、`closePayment`、`refundPayment` 和 `verifyNotification`，返回领域 `PaymentSession`/`PaymentNotification`。
- Stripe 适配器：暂不实现，仅保留 `PaymentProviderPort` 和支付成功事件输入边界。
- 微信支付适配器：**暂停**。API v3 请求签名、平台证书验签、AES-GCM 解密、查单、关单和退款进入暂停队列。
- 支付宝适配器：**暂停**。RSA2 签名、交易查询/关闭/退款和异步通知验签进入暂停队列。
- Provider 配置、证书和密钥只从环境变量或 Secret 注入；第三方 SDK 类型只存在 infrastructure 适配器。
- 应用服务：保留 `MarkPaymentSucceeded`/`MarkPaymentFailed` 的事件边界，具体 Provider 会话创建延后。
- 集成事件：`PaymentSucceeded`、`PaymentFailed` → Outbox；当前使用测试事件适配器验证下游，不伪造真实支付成功。
- API：保留 `POST /orders/{id}/payment-session` 契约，但在 Provider 未接入前不作为本阶段验收入口。
- 统一返回 `PaymentSession`，允许 Provider-specific `clientPayload`：Stripe `clientSecret`、微信 `prepayId`/调起参数、支付宝支付表单或交易号，不强行把三者建模成同一字段。
- **强约束**：前端不得直接修改订单状态为 `PAID`；只能由验签后的 Webhook 推进。
- 回调先验签/解密，再校验订单号、金额、币种、商户/应用标识和支付状态；按 `provider + providerEventId` 唯一去重，重复通知返回 2xx 且不重复推进订单。
- 支付通知不能依赖到达顺序；查单结果是补偿依据，最终仍由订单状态机和幂等消费者决定。

**编排**

- Ordering 消费 `PaymentSucceeded` 推进订单状态，幂等键 `eventId + orderId`。

**前端**

- 前端暂不实现 Stripe、微信或支付宝支付组件；只完成支付结果状态模型和重复提交保护，支付渠道接入不阻塞其他工作。

**Week 8 验收**

- 统一 Payment Port 的契约和事件边界通过测试；具体 Provider 的超时、5xx、查单补偿和退款测试延后。
- 微信支付和支付宝的全部接入、验签、沙箱和商户联调进入暂停队列，不作为当前版本完成条件。
- 当前支付替身只允许驱动受信任的 `PaymentSucceeded` 测试事件，不能由前端或普通 Controller 直接把订单置为 `PAID`。

**本阶段转移的工作量**

- 优先完成数字产品 `Entitlement`、`DownloadGrant`、私有对象存储、下载审计和授权撤销。
- 完成 Outbox Poller、失败重试、退避、死信和 Notification 基础通道。
- 完成实体/数字双履约的 Studio 页面、订单状态和关键 E2E。

---

### Week 9：Outbox + Notification

**目标**：可靠事件投递 + 基础通知。

**Outbox**

- `outbox_events` 表：事件 ID、类型、版本、聚合信息、payload、attempts、next_attempt_at、locked_until、status、last_error、occurred_at、trace_id。
- `OutboxPoller`（当前实现为 Spring `@Scheduled` Worker 每秒执行），`FOR UPDATE SKIP LOCKED` 并发领取已完成。
- 退避：指数退避，超过最大尝试次数进入 `DEAD`；发布端口可替换，当前本地适配器只记录脱敏事件元数据。
- 消费者幂等：状态机守卫 + 事件去重表。

**Notification 上下文**

- MVP 事件：`ArtworkPublished`、`PaymentSucceeded`、`PaymentFailed`、`OrderStatusChanged`。
- 通道：站内通知 + 邮件 stub（控制台 + 适配器接口），不接真实 SMTP。
- 数字产品事件：`DigitalEntitlementGranted`、`DigitalAssetRecalled`；通知只携带订单或作品入口，不携带签名下载 URL。

**Week 9 验收**

- 注入失败：验证重试、退避、死信告警。
- 重复消费：不会产生重复副作用。
- 邮件/通知适配器可在测试中被替换。

**当前进度（2026-08-28）**

- 已完成 Notification 表、事件去重表、游标列表、未读筛选和标记已读接口。
- 已完成本地 Outbox Publisher 到 Notification handler 的可替换消费骨架；重复事件通过事件 ID 幂等。
- 已完成可替换的邮件投递端口和脱敏 Console stub；当前不接入真实 SMTP。
- 已接入订单创建 `OrderCreated` 事件；订单状态变更、Catalog 发布和数字交付事件仍待接入业务事务。
- 业务上下文尚未统一补齐各事件的通知收件人和脱敏文案载荷，真实业务事件接入与邮件触发保持待办，不将日志记录视为通知发送成功。

---

### Week 10：Creator Studio

**目标**：Creator 能管理作品和订单。

**Studio 接口**

- `GET/POST /studio/artworks`、`PUT /studio/artworks/{id}`、`POST /studio/artworks/{id}/publish`。
- `GET /studio/orders`、`PATCH /studio/orders/{id}/status`。
- `GET /studio/dashboard`：收入、订单数、已发布作品、关注者数。
- Studio 不新增领域，只调用 Creator、Catalog、Ordering 公开端口 + 只读投影。

**前端**

- `app/studio/layout.tsx`：侧边栏 + 信息密度布局（当前尚未实现，现有页面暂复用全站 AppShell）。
- Dashboard：4 个核心数字卡。
- 作品列表/编辑/发布：乐观锁版本号，字段错误展示；当前已完成新建、编辑和审核提交，Studio 作品列表待补。
- 订单列表/详情/履约：状态推进按钮。
- 数字产品工作流：创建数字产品 → 上传私有资产 → 病毒扫描/哈希校验 → 发布不可变资产版本 → 查看数字订单与下载统计；Studio 不读取买家下载 Token。
- 新增数据模型：`digital_assets`、`digital_asset_versions`、`entitlements`、`download_grants`。替换文件必须创建新版本，保留旧版本审计记录。
- 当前优先级：完善数字资产版本管理、扫描结果处理、资产撤回、授权撤销和下载审计；不等待微信/支付宝接入。

**Week 10 验收**

- 创作者 E2E：发布作品 → 用户下单 → 创作者接单 → 填物流 → 状态推进。
- 权限测试：非创作者访问 Studio 全部 403；创作者只能操作自己的资源。

---

### Week 11：Checkout + UX 完整化

**目标**：完成消费者端从作品到订单的端到端体验。

**前端**

- Checkout：地址表单（React Hook Form + Zod）、订单摘要、Stripe Element、错误展示。
- Checkout 根据产品类型选择地址表单；数字产品不收集地址，并显示支付完成后的交付说明。
- Order Result：成功/失败/待处理三态。
- Order History：分页、状态筛选、空态。
- Digital Delivery：订单详情展示授权状态、文件名、大小和版本；只有授权有效时才能申请短期下载链接，过期后重新申请。
- Mobile responsive：375 / 768 / 1440。
- 动效：160–240ms，遵守 `prefers-reduced-motion`。

**后端补强**

- Order 详情返回最新状态和支付信息。
- 幂等与错误响应在所有写接口完整。
- 数字交付接口：`GET /orders/{id}/entitlement`、`POST /orders/{id}/download-grants`；仅已支付且授权有效的买家可调用，长期权限与短期下载凭证分离。
- 数字订单验收可使用受信任支付事件测试替身；不得把测试替身暴露为生产支付入口。

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
- 数字资产安全：私有 bucket、短期签名 URL、对象级授权、下载审计、撤销授权和防枚举；禁止通过公开媒体 URL 访问原始文件。
- Provider Webhook Signature：具体 Provider 接入前不实现；通用 Webhook 原始载荷、签名失败和幂等边界先以契约测试固定。
- 依赖扫描：GitHub Dependabot + OWASP dependency-check。
- 日志：禁记密码、Token、Cookie、支付信息、完整地址与邮箱。

**E2E（4 条核心）**

1. **Discover**：搜索 → 列表 → 详情 → 返回。
2. **Physical Purchase**：实体作品 → 地址结算 → 测试支付 → 订单 PAID → 创作者履约。
3. **Digital Purchase**：数字作品 → 无地址结算 → 测试支付 → 授权生成 → 获取下载 → 越权访问被拒。
4. **Creator Publish**：申请 Creator → 上传作品或数字资产 → 发布 → 消费者可见。
5. **Creator Fulfillment**：用户下单 → 创作者接单 → 发货或更新数字资产版本 → 状态推进。

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
- 数字资产生命周期：原始文件私有存储并按版本保留；扫描失败自动隔离，未绑定资产定期清理；下载日志只保留必要审计字段，不记录签名 URL。

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

**暂停队列（不阻塞当前版本）**

- 微信支付 API v3：商户签名、平台证书验签、AES-GCM 回调解密、查单、关单、退款和商户联调。
- 支付宝 RSA2：请求签名、异步通知验签、交易查询、关闭、退款和沙箱联调。
- Stripe 具体 SDK、3DS 和真实 Webhook 联调；当前只保留统一端口和受信任事件测试边界。

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
