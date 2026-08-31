# CreatorHub

独立创作者作品交易社区。架构与开发基线见 [`DEVELOPMENT_GUIDE.md`](DEVELOPMENT_GUIDE.md)，智能体协作规范见 [`AGENTS.md`](AGENTS.md)，前后端接口手册见 [`docs/api/README.md`](docs/api/README.md)。智能体工作补充说明与外部 skills 镜像见 [`.CLAUDE/`](.CLAUDE/)。

## 仓库结构

```text
creatorhub/
├── apps/
│   ├── api/                 # Spring Boot 3.5 + Java 21 + Maven + MyBatis-Plus
│   └── web/                 # Next.js 15 + React 19 + TypeScript + Tailwind 4
├── docs/
│   ├── openapi.yaml         # 前后端接口的单一事实来源
│   ├── api/                 # 工程级 API 手册（按限界上下文组织）
│   ├── domain/              # 统一语言与事件目录
│   └── plan/                # 14 周 MVP 开发计划
├── infra/                   # Docker Compose / Dockerfile / .env.example
├── .CLAUDE/                 # Claude 工作补充 + 外部 skills 仓（独立 git 仓镜像）
├── AGENTS.md
├── DEVELOPMENT_GUIDE.md
└── prd_tech_design.md
```

## 本地开发

Docker Compose 只运行 PostgreSQL、Redis 和 MinIO；API 使用本机 Maven，Web 使用本机 pnpm。
这种组合保留后端/前端快速反馈，同时让中间件版本保持一致。

### 1. 首次启动

```powershell
Copy-Item infra/env.example .env
docker compose -f infra/compose.yaml up -d
docker compose -f infra/compose.yaml ps
```

容器包含：PostgreSQL 17（主机端口 15432）、Redis 8、MinIO。

访问地址：

- MinIO Console：`http://localhost:9001`

### 2. 启动后端

```powershell
mvn -B -ntp -f apps/api/pom.xml spring-boot:run -Dspring-boot.run.profiles=local
```

API 健康检查：`http://localhost:8080/actuator/health`。

### 3. 启动前端

```powershell
pnpm install
pnpm web:dev
```

Web：`http://localhost:3000`。中间件细节见 [`infra/README.md`](infra/README.md)。

## 验证

```powershell
mvn -B -ntp -f apps/api/pom.xml test
mvn -B -ntp -f apps/api/pom.xml spotless:check
pnpm web:typecheck
pnpm web:lint
pnpm web:test
docker compose -f infra/compose.yaml config
```

## 当前里程碑：非支付业务闭环收尾与 Admin MVP

已完成：

- 后端 Spring Boot 工程基线：Java 21、Maven、MyBatis-Plus、共享配置与多 profile。
- `sharedkernel`：`Money`、`AggregateRoot`、`DomainEvent`、`StrongId` 与 UTC 时间抽象。
- ArchUnit 边界基线测试（domain 不依赖 infrastructure / Spring / MyBatis-Plus / Stripe）。
- 全局 RFC 9457 Problem Details 错误处理。
- Docker Compose（postgres/redis/minio）+ API/Web 部署用多阶段 Dockerfile。
- 前端 Next.js 15 + Tailwind 3.4 + Design Token；Discover / Artwork Detail / Creator Profile 静态页面与登录/注册占位。
- UI 基础组件：Button、Input、Tabs、Modal、Skeleton、Navbar、Footer、AppShell、ArtworkCard、CreatorCard、MediaGallery。
- 基础 fetcher 封装与 Problem Details 解析工具。
- `useRequestId` 钩子。
- Identity：注册、登录、JWT Bearer、`/me`、Refresh Token HttpOnly Cookie 轮换、注销与 Origin/Referer 校验。
- Creator：申请、更新、公开详情；owner 只来自 JWT，Identity 角色通过公开 application port 提升。
- Redis 认证限流：注册 IP、登录 IP/邮箱摘要、刷新 IP；超限返回 RFC 9457 `RATE_LIMITED` + `Retry-After`。
- PostgreSQL Flyway V1/V2、MyBatis-Plus Repository Adapter、显式 UUID TypeHandler 与 Refresh Token `FOR UPDATE` Mapper XML。
- 前端真实登录/注册、内存 Access Token + HttpOnly Cookie 恢复会话、Creator 申请表单。
- OpenAPI TypeScript 契约类型生成：`pnpm web:api:generate`，生成文件位于 `apps/web/src/lib/api/schema.gen.ts`。
- Catalog：作品发布、公开搜索、分类筛选、媒体短期 URL 和 Studio 作品管理。
- Collection/Follow：收藏、关注、游标分页、认证用户状态与乐观更新。
- Ordering：订单创建、价格快照、库存预占/释放、幂等、取消、物流履约和订单详情。
- Payment/Outbox：统一 Provider 端口、可信 `PaymentSucceeded`/`PaymentFailed` 事件、Ordering 消费幂等和 V21 消费记录表。
- Studio：Dashboard、作品列表、订单履约和物流状态推进。
- Web：TanStack Query、Checkout 实体/数字产品分流、订单兼容响应层和认证用户订单入口。
- E2E：Playwright Discover 三档响应式流程，以及环境变量驱动的实体购买流程。
- E2E：已补充认证通知页访问与未读筛选流程。
- E2E：已加入环境变量驱动的数字 Checkout 前置测试；需要 `CREATORHUB_E2E_DIGITAL_ARTWORK_ID` 才执行，完整数字购买闭环尚未验收。
- E2E：已加入 Creator Studio 履约状态推进测试；需要 `CREATORHUB_E2E_PAID_ORDER_ID` 才执行，完整 Creator 履约闭环尚未验收。
- E2E：已加入 Creator → Admin → Creator 发布跨角色测试；需要 `CREATORHUB_E2E_ADMIN_EMAIL` 和 `CREATORHUB_E2E_ADMIN_PASSWORD`，Creator 账号和待审核作品由测试通过真实 API 动态准备。
- E2E Fixture：跨角色测试不把账号、密码或作品写入 Flyway/生产启动；测试通过真实注册、Creator 申请和作品提交流程准备数据，不创建订单或伪造 `PAID` 状态。
- 订单详情：已补齐待支付、履约、完成、取消和退款结果态展示，支付成功仍只由服务端状态决定。
- 订单历史：已补齐退款中和已退款状态筛选，筛选值通过 URL 传递给服务端。
- 数字交付：订单详情已查询并展示授权状态，只有已支付且授权可用时才允许申请短期下载链接。
- 数字授权查询缺失时返回明确的 404 Problem Details，不将资源不存在误报为 400。
- Discover：全部作品已按实体产品与数字产品分区展示，分区内分类入口按产品类型组织；发现下拉菜单不再重复显示创作者目录。
- 认证联调：Access Token 过期时自动使用 HttpOnly Refresh Cookie 续期并重试一次，并发请求共享同一次 refresh。
- Webhook 安全边界：已补充基于原始请求体的通用 HMAC-SHA256 验签和时间戳窗口防重放组件，具体支付 Provider 仍未接入。
- 工程质量：已加入 GitHub Actions CI，覆盖 Maven、pnpm、OpenAPI 生成漂移、Docker 构建和 Compose 校验；Dependabot 已配置 npm/Maven 周期更新。

当前闭环审计（2026-08-30）：

- Creator 申请、作品草稿创建、图片/数字资产上传确认、作品提交审核的基础链路已存在；普通用户入口为“成为 Creator”，Creator 入口为 `Creator Studio`。
- Creator 发布前端入口已补齐：作品处于 `APPROVED` 时，Creator 可在 Studio 点击发布并进入 `PUBLISHED`。
- Admin MVP 已补齐：管理员可从导航进入 `/admin`，查看只读运营指标、待审核队列和审核详情，并填写理由批准或拒绝作品。
- Admin 查询 API 仅返回待审核作品和脱敏 Creator 快照；批准/拒绝继续复用 Catalog 状态机，不新增真实支付、退款或结算假实现。
- 审核反馈基础链已接入：批准/拒绝写入 Outbox 并生成 Creator 站内通知，Creator 可读取自己作品的审核历史和拒绝理由。
- Creator 关注动作已写入 Outbox，并生成发给 Creator 所属用户的站内通知，链接指向 Creator 公开主页。
- Creator Studio Dashboard 已展示当前 Creator 数字作品的已核销下载次数，不返回 IP、User-Agent 哈希或下载凭证信息。

未完成（非支付闭环与后续生产工作）：

- 数字购买、Creator Fulfillment 的完整跨角色 Playwright E2E；当前数字无地址结算、授权越权拒绝、履约服务和 HTTP 权限边界已有回归测试，完整支付授权与履约 E2E 仍需要可信的已支付订单数据。
- 通知文案细化。
- Admin 审核历史数据库查询集成测试。
- 可重复的本地 Creator、Admin、作品和订单 Fixture；不得用伪造 `PAID` 订单替代真实支付边界。
- 后端订单 REST 响应正式收敛到 OpenAPI 的嵌套 `OrderDetail`，当前 Web 通过兼容层读取扁平响应。
- 数字资产撤回业务事件和部分通知业务载荷接入。
- Stripe/微信/支付宝真实 Provider、Webhook 和沙箱联调按范围调整进入后续迭代。
- AWS 资源、Sentry/CloudWatch、备份恢复演练和 Beta 运营验收。
