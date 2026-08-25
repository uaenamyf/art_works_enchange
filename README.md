# CreatorHub

独立创作者作品交易社区。架构与开发基线见 [`DEVELOPMENT_GUIDE.md`](DEVELOPMENT_GUIDE.md)，智能体协作规范见 [`AGENTS.md`](AGENTS.md)，前后端接口手册见 [`docs/api/README.md`](docs/api/README.md)。智能体工作补充说明与外部 skills 镜像见 [`.CLAUDE/`](.CLAUDE/)。

## 仓库结构

```text
creatorhub/
├── apps/
│   ├── api/                 # Spring Boot 3.5 + Java 21 + Gradle KTS
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

### 1. 启动基础设施

```bash
cp infra/env.example .env
docker compose -f infra/compose.yaml up -d
```

容器包含：PostgreSQL 17、Redis 8、MinIO、API、WEB。

### 2. 后端（apps/api）

```bash
cd apps/api
./gradlew bootRun --args='--spring.profiles.active=local'
# 健康检查
curl http://localhost:8080/actuator/health
```

### 3. 前端（apps/web）

```bash
pnpm install
pnpm web:dev
# 打开 http://localhost:3000
```

## 验证

```bash
# 后端
cd apps/api
./gradlew test
./gradlew spotlessCheck   # 暂未启用，后续接入

# 前端
pnpm web:lint
pnpm web:typecheck
pnpm web:test
```

## 当前里程碑：Week 1-2 工程基座

已完成：

- 后端 Spring Boot 工程基线：Java 21 工具链、Gradle Kotlin DSL、共享配置与多 profile。
- `sharedkernel`：`Money`、`AggregateRoot`、`DomainEvent`、`StrongId` 与 UTC 时间抽象。
- ArchUnit 边界基线测试（domain 不依赖 infrastructure / Spring / JPA / Stripe）。
- 全局 RFC 9457 Problem Details 错误处理。
- Docker Compose（postgres/redis/minio/api/web）+ 多阶段 Dockerfile。
- 前端 Next.js 15 + Tailwind 4 + Design Token；Discover / Artwork Detail / Creator Profile 静态页面与登录/注册占位。
- UI 基础组件：Button、Input、Tabs、Modal、Skeleton、Navbar、Footer、AppShell、ArtworkCard、CreatorCard、MediaGallery。
- 基础 fetcher 封装与 Problem Details 解析工具。
- `useRequestId` 钩子。

未完成（按计划进入 Week 3）：

- Identity / Creator 业务上下文与真实 API。
- OpenAPI 客户端生成与 TanStack Query 接入。
- Flyway migration 与真实数据库集成测试。
- GitHub Actions CI 工作流（已按要求跳过）。
