# CreatorHub 全量测试报告

## 1. 执行信息

| 项目 | 结果 |
| --- | --- |
| 执行日期 | 2026-09-01 |
| 分支 | `main` |
| Web | Next.js 15.0.3，Node.js 本地服务 |
| API | Spring Boot 3.5.5，Java 23.0.2 运行，编译目标 Java 21 |
| 数据库 | PostgreSQL 17.11 |
| 测试基线 | `docs/openapi.yaml`、`docs/testing/test-plan.md`、`docs/testing/test-cases.md` |

## 2. 总结结论

本轮静态检查、前端类型检查、前端 Lint、前端生产构建和后端编译/格式检查通过。后端全量测试执行 253 个，252 个通过、1 个失败；前端 Playwright 执行 18 个，2 个通过、1 个失败、15 个跳过。由于存在后端失败和移动端 E2E 失败，本轮不能判定为全量验收通过。

## 3. 自动化结果

| 检查 | 命令 | 结果 |
| --- | --- | --- |
| 前端类型 | `pnpm web:typecheck` | 通过 |
| 前端 Lint | `pnpm web:lint` | 通过，无 warning/error |
| 前端 Vitest | `pnpm web:test` | 15 个测试文件：14 通过，1 失败；33 个测试：31 通过，2 失败 |
| 前端生产构建 | `pnpm web:build` | 通过，21 条路由构建成功 |
| 前端 Playwright | `pnpm --filter @creatorhub/web e2e` | 2 通过，1 失败，15 跳过 |
| 后端编译 | `mvn -B -ntp -f apps/api/pom.xml -DskipTests compile` | 通过 |
| 后端 Maven 测试 | `mvn -B -ntp -f apps/api/pom.xml test` | 252 通过，1 失败 |
| 后端格式 | `mvn -B -ntp -f apps/api/pom.xml spotless:check` | 通过 |
| OpenAPI lint | `pnpm exec @redocly/cli lint docs/openapi.yaml` | 未执行，当前未安装 `@redocly/cli` |

## 4. 失败项

### API-FAIL-01：Creator 仓储回读 PayPal merchant_id 失败

- 测试：`CreatorRepositoryAdapterTest.flywayCreatesCreatorTableAndAdapterRoundTripsAggregate`
- 位置：`apps/api/src/test/java/com/creatorhub/creator/infrastructure/persistence/CreatorRepositoryAdapterTest.java:79`
- 预期：`merchant-123`
- 实际：`null`
- 影响：Creator PayPal 收款方式的持久化回读链路不可靠，收款设置不能视为通过。
- 建议：检查 Creator PersistenceRow、Mapper 查询列和领域转换是否完整映射 `paypal_merchant_id`，修复后重新运行该集成测试及收款相关 Controller 测试。

### E2E-FAIL-01：移动端 Discover 作品详情跳转失败

- 测试：`tests/e2e/discover.spec.ts:3`
- 项目：Playwright `mobile`
- 预期：点击作品后 URL 匹配 `/artworks/{uuid}`
- 实际：仍停留在 `/`
- 影响：移动端公开发现流程未通过，作品详情入口可能存在点击被遮挡、链接定位或导航竞态问题。
- 建议：检查移动端作品卡片链接的实际可点击区域、导航等待条件和响应式布局；修复后单独重跑该用例。

### WEB-FAIL-01：StudioArtworkForm 两个测试失败

- 测试文件：`apps/web/src/features/artworks/StudioArtworkForm.test.tsx`
- 失败用例：`blocks submission when no artwork image is selected`
- 失败用例：`uploads and confirms a digital asset for digital products`
- 统计：31 个通过，2 个失败。
- 影响：作品创建表单的无图片校验和数字资产上传确认流程未能通过当前前端测试，作品管理不能视为完全验收。
- 本轮处理：未修改该表单实现或测试断言，避免把与权限统一无关的行为变更混入本次测试报告任务。
- 建议：读取两个失败用例的完整断言堆栈，分别验证媒体选择校验和数字资产上传请求 mock 是否与当前表单实现一致，然后单独修复并复验。

## 5. 条件跳过项

以下 15 条 E2E 未执行不是通过。它们依赖环境变量或真实业务前置数据：

- 数字购买：`CREATORHUB_E2E_DIGITAL_ARTWORK_ID`
- 已支付 Creator 履约：`CREATORHUB_E2E_PAID_ORDER_ID`
- 跨角色发布：`CREATORHUB_E2E_ADMIN_EMAIL`、`CREATORHUB_E2E_ADMIN_PASSWORD`，以及可用 Creator 流程
- 实体购买/支付：需要可用的商品、库存、账号和支付沙箱状态

不能使用伪造 `PAID` 订单或真实密码替代这些前置条件。

## 6. 接口覆盖结论

OpenAPI 当前约 50 个操作，测试用例矩阵已覆盖 Identity、Creator、Catalog、Collection、Ordering、Payment、Studio、Notification 和 Digital Delivery 的正常、认证、授权、校验、幂等及状态机意图。仓库现有自动化已覆盖大部分领域/应用/Controller/数据库行为，但下列接口仍需真实环境或补充测试才能完成“每一个接口功能”的验收：

- Stripe/PayPal 真实 Provider Webhook、签名、重放和沙箱支付结果。
- Payment Session/Intent 的真实第三方成功、失败、取消和退款闭环。
- 需要已支付订单的数字下载和 Creator 履约完整跨角色 E2E。
- 部分 OpenAPI 生成契约与实际 HTTP 响应的一致性检查，因 Redocly CLI 当前不可用未执行。
- `GET /digital-downloads/{grantId}` 等接口需要专门的授权、过期和撤回测试证据。

## 7. 环境观察

- API、PostgreSQL、Redis 和前端本地服务均可启动。
- 未登录浏览器访问认证刷新接口返回 401，属于预期行为。
- Next.js 开发服务和生产构建不能并行写同一个 `.next` 目录，否则会产生 `ChunkLoadError` 和缺失 chunk。
- 后端测试过程中出现 Hikari 连接校验警告，但未导致本轮大多数集成测试失败。

## 8. 验收建议

1. 修复 PayPal merchant_id 持久化回读失败。
2. 修复移动端 Discover 详情跳转失败。
3. 安装并执行 OpenAPI lint/破坏性变更检查。
4. 提供不入库的测试环境变量和真实沙箱前置数据，重跑 15 条条件化 E2E。
5. 以上项目完成后重新执行完整测试集，并将本报告更新为最终验收版本。