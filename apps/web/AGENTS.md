# CreatorHub Web（apps/web）智能体规范

> 范围：只描述 `apps/web` 的进入路径与本应用专属约束。  
> 团队级规则、UI Token、安全红线、契约流程等见仓库根 `AGENTS.md`；项目背景、计划与里程碑见根 `DEVELOPMENT_GUIDE.md` 与 `docs/plan/development-plan.md`；Claude 的执行节奏见根 `CLAUDE.md`。

## 1. 何时打开本文件

- 你正在处理或计划处理 `apps/web/**` 下的代码、依赖、配置或 E2E。
- 你需要确认前端模块归属、UI 规则或测试运行方式。

## 2. 必读

1. 根 `AGENTS.md` §4 前端规则与 §5 UI 规则。
2. `DEVELOPMENT_GUIDE.md` §2 技术选型、§6 前端设计、§7 视觉与交互。
3. `docs/openapi.yaml` 端点与错误模型。
4. 涉及 Studio 时同步阅读 `docs/plan/development-plan.md` Week 10/11。

## 3. 应用结构（渐进式披露）

```text
apps/web
├── src/app/                    # App Router 路由；按 (auth)/(market)/(checkout)/(account)/studio 分组
├── src/components/             # ui/ 基础组件；domain/ 业务组件；layout/ 布局
├── src/features/               # 业务领域：auth/creators/follows/artworks/favorites/orders/payments/notifications/studio
├── src/lib/                    # api/{generated,hooks,problem}、auth、stripe、format
├── src/providers/              # Query Provider、Auth Provider 等
├── src/hooks/                  # 跨页通用 hooks
├── src/styles/                 # Design Token 与全局样式
├── tests/                      # unit/、integration/、e2e/
└── public/                     # 静态资源
```

子模块有各自的 `AGENTS.md`，按需打开：

- `src/app/AGENTS.md` — App Router 路由与四态约定。
- `src/components/ui/AGENTS.md` — 基础组件与变体规则。
- `src/components/domain/AGENTS.md` — 业务组件与设计 Token 落地。
- `src/features/<领域>/AGENTS.md` — 业务领域入口。
- `src/lib/api/AGENTS.md` — OpenAPI 客户端、Problem Details 解析。
- `src/lib/stripe/AGENTS.md` — Stripe Payment Element 与幂等键。
- `tests/e2e/AGENTS.md` — Playwright 关键路径。

## 4. 常用命令

```bash
pnpm --filter @creatorhub/web dev
pnpm --filter @creatorhub/web typecheck
pnpm --filter @creatorhub/web lint
pnpm --filter @creatorhub/web test
pnpm --filter @creatorhub/web build
pnpm exec playwright test
```

## 5. 渐进式披露原则

- 通用规则永远先看根 `AGENTS.md`，不要在本文件复制。
- 子模块 `AGENTS.md` 只描述：进入条件、必读链接、本地约定、禁止项、验证命令。
- 任何与根规范冲突时，以根 `AGENTS.md` → `DEVELOPMENT_GUIDE.md` → `docs/openapi.yaml` 的顺序为准。

## 6. 交叉引用

- 视觉规范：根 `figma_design.md`、根 `DEVELOPMENT_GUIDE.md` §7。
- API 契约：`docs/openapi.yaml`。
- 统一语言：`docs/domain/ubiquitous-language.md`。
- 领域事件：`docs/domain/event-catalog.md`。
