# CreatorHub 全量测试计划

## 1. 目标

验证 CreatorHub 当前 MVP 的前后端接口、权限边界、核心交易状态机、异步 Outbox、前端页面和关键用户流程。测试以 `docs/openapi.yaml` 为接口范围基线，以数据库、Redis、MinIO 和本地 API/Web 服务为执行环境。

## 2. 测试范围

| 范围 | 内容 | 验证方式 |
| --- | --- | --- |
| Identity | 注册、登录、刷新、注销、当前用户、限流和 Cookie 安全属性 | Maven 测试、HTTP 契约、E2E |
| Creator | 申请、更新、公开列表/详情、关注和粉丝 | Maven 集成测试、前端测试、E2E |
| Catalog | 作品查询、草稿、更新、审核、发布、归档、媒体和数字资产 | Maven 集成测试、前端测试、E2E |
| Collection | 收藏添加、移除和列表 | Maven 测试、E2E |
| Ordering | 创建、查询、取消、库存、价格快照、物流和幂等 | Maven 集成测试、E2E |
| Payment | Payment Session/Intent、支付回调、重复事件和授权边界 | Maven 测试；真实 Provider 需沙箱凭据 |
| Studio | Dashboard、作品管理、订单履约和收款方式 | Maven、前端测试、管理员/Creator E2E |
| Notification | 列表、未读筛选、已读和更多分页 | Maven、Vitest、E2E |
| 前端体验 | 21 条路由、加载/空/错误态、语言切换、响应式导航 | TypeScript、Vitest、Playwright |

## 3. 环境与前置条件

- Node.js >= 22、pnpm 11、Java 21+、Maven。
- PostgreSQL 17、Redis 8、MinIO 按 `infra/compose.yaml` 启动。
- API 使用 `local` profile，监听 `8080`；Web 使用开发服务，监听 `3000`。
- 测试不得与 `next build`/`next start` 同时写入同一个 `.next` 目录。
- 支付沙箱、Webhook secret、已支付订单和数字作品 ID 通过环境变量提供，不写入仓库。

## 4. 执行层级

1. 静态与契约：TypeScript、ESLint、Maven 编译、Spotless、OpenAPI lint、生成类型漂移。
2. 后端：完整 `mvn test`，覆盖领域、应用服务、Controller、安全和 PostgreSQL Testcontainers 集成测试。
3. 前端：完整 Vitest、类型检查、Lint、生产构建。
4. E2E：Playwright 三种视口，覆盖公开浏览、认证页面、通知、实体购买和条件化 Studio/数字支付流程。
5. 手工/环境依赖：真实 PayPal/Stripe 沙箱 Webhook、跨角色完整支付履约和恢复演练。

## 5. 通过标准

- 必跑自动化测试无失败、无错误、无意外跳过。
- OpenAPI lint 和生成类型检查通过。
- 未认证请求不能访问受保护写接口；普通用户、Creator、Admin 权限边界符合用例矩阵。
- 金额、库存、订单状态和 Webhook 重复投递场景通过。
- Playwright 在 375px、768px、1440px 下无横向溢出和关键控件遮挡。
- 条件化 E2E 若因缺少外部前置条件未执行，必须在报告中列明，不得计为通过。

## 6. 风险与限制

- 真实支付 Provider 尚未接入或未必具备当前沙箱凭据，支付成功、退款和真实 Webhook 只能做代码/模拟边界验证。
- 需要已支付订单的 Creator 履约 E2E 不能用伪造订单替代。
- Testcontainers 需要 Docker；Docker 不可用时 PostgreSQL 集成测试会阻塞或失败。
- 本地开发服务和生产构建共用 `.next` 会造成 ChunkLoadError，执行构建前应停止开发服务。

## 7. 交付物

- `docs/testing/test-plan.md`：测试策略与门槛。
- `docs/testing/test-cases.md`：接口级和流程级用例。
- `docs/testing/test-report.md`：本轮执行结果、失败项、覆盖缺口和风险结论。