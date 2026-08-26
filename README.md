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

## 当前里程碑：Week 3 Identity + Creator

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

未完成（Week 3 收尾 / 后续周）：

- 登录/注册/Creator 申请 Playwright 浏览器 E2E（当前后端/组件/集成测试已覆盖）。
- OpenAPI 客户端生成与 TanStack Query 接入。
- Catalog 等后续上下文的 Flyway migration 与数据库集成测试。
- GitHub Actions CI 工作流（已按要求跳过）。
