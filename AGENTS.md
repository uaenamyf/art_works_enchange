# CreatorHub 智能体协作规范

本文件适用于仓库中的所有编码智能体。目标是让智能体交付可审查、可测试、符合产品边界的代码，而不是只生成看似完整的样例。

## 1. 开始任务前

1. 阅读 `DEVELOPMENT_GUIDE.md`，确认当前架构、模块和交付门槛。
2. 涉及 HTTP 接口时阅读 `docs/openapi.yaml`；它是接口单一事实来源。
3. 涉及视觉时阅读 `figma_design.md`，同时以 `DEVELOPMENT_GUIDE.md` 的 UI 约束为实施基线。
4. 先定位直接控制行为的代码、邻近测试和现有模式，再提出最小修改。
5. 工作区可能包含用户未提交的改动。不要覆盖、回退或格式化无关文件。

## 2. 固定技术边界

- 后端：Java 21、Spring Boot 3、Maven、PostgreSQL、Redis、Flyway。
- 前端：Next.js App Router、React、TypeScript、Tailwind CSS、Radix UI。
- 部署：Docker 镜像与 Docker Compose。
- 架构：DDD 模块化单体，以限界上下文为一级代码边界。未经明确 ADR 和用户批准，禁止引入微服务、Kafka、Kubernetes、服务发现或分布式事务。
- 外部依赖优先包在适配器后面，业务规则不得依赖 Stripe、S3 或框架类型。
- 不凭空更换框架、包管理器、测试库或视觉体系。

### 本地技能使用规则

- `.agent/.skill` 与 `.CLAUDE/skills/` 都是通用参考，不是仓库规范；优先级低于用户要求、`AGENTS.md`、`docs/openapi.yaml`、`DEVELOPMENT_GUIDE.md` 和 ADR。
- `.CLAUDE/skills/` 是独立 git 仓镜像自 `github.com/openai/skills`（上游已 deprecated），**只读引用**，不修改、不提交 CreatorHub 业务代码。详细使用规则见 `.CLAUDE/CREATORHUB.md`。
- DDD 任务可参考 `ddd-domain-designer`、`ddd-architecture-hexagonal`、`ddd-event-storming`、`ddd-api-designer`、`ddd-cqrs-architecture`、`ddd-testing-strategist`，但只采用与本仓库技术边界一致的部分。
- API 成功响应直接使用资源 DTO，错误使用 RFC 9457 Problem Details；禁止套用技能中的 `Result<T>`、数字错误码和每前端一个 BFF。
- CQRS 限定为共享数据库的 L1 代码级读写分离。禁止自行引入读写数据库分离、Event Sourcing、Command Bus 框架或物化视图基础设施。
- 禁止采用绑定 DDD4J、COLA、MyBatis、Sa-Token、微服务、Kafka 或 Kubernetes 的技能模板。
- 技能中的目录、依赖矩阵或代码模板与仓库规则冲突时，以 `DEVELOPMENT_GUIDE.md` 的端口归属和依赖方向为准，不得直接复制。

## 3. 后端规则

- 先确定限界上下文、统一语言、聚合与不变量，再编写 Controller 或数据库表；禁止把 DDD 简化为目录命名。
- 包按限界上下文组织，每个上下文使用 `domain/application/interfaces/infrastructure` 分层。Controller / ApplicationService / Repository Adapter 角色都存在，但 DDD 强制它们归属正确层并遵守依赖方向。
- Domain 必须是纯 Java，不依赖 Spring、JPA、Redis、Stripe、S3 或 Web DTO。
- Controller 只做协议转换、校验和授权入口，调用 `application/port/in` 执行业务用例；不得直接调用 Repository、不得包含业务规则。
- ApplicationService 只编排用例、事务和权限，不承载应属于聚合的业务规则；业务规则只写在 `domain/model` 聚合方法或 `domain/service` 中。
- JPA Entity、API DTO 与领域模型三者分离并显式转换；Repository Adapter（DAO 实现）只能返回聚合根或值对象，禁止把 JPA Entity 透传到 application 或 interfaces。
- Repository 是 domain 中按聚合根定义的端口，由 infrastructure 实现；不要为每张表机械创建 Repository。
- 跨上下文只调用公开 application port 或消费领域事件，不直接使用其他上下文的 Repository、Entity 或数据表。
- 跨上下文引用只使用强类型 ID 或不可变快照，禁止跨上下文 JPA 关联。
- 聚合根负责不变量并暴露业务行为，不使用公共 setter 构造贫血模型。
- 一个事务原则上只修改一个聚合；跨上下文副作用使用领域事件和 Outbox，消费者必须幂等。
- Command 与 Query DTO 分离；查询模型只读，不复用聚合或写 DTO，不修改业务状态。
- 领域事件与跨上下文集成事件分离，事件必须过去式命名、版本化且不含敏感信息，并登记到 `docs/domain/event-catalog.md`。
- 新增或修改领域术语时同步维护 `docs/domain/ubiquitous-language.md`。
- Shared Kernel 保持最小，只放稳定领域概念；通用工具类不能借机堆入其中。
- 事务边界放在 application service；外部网络调用不长时间占用数据库事务。
- 金额使用 `BigDecimal`，时间使用 UTC，外部 ID 使用 UUID。
- 写操作校验对象级权限；不要依赖客户端角色或请求中的价格、用户 ID。
- 新增或修改表结构必须增加 Flyway migration；不要修改已共享的 migration。
- 使用构造器注入。禁止字段注入、静态 service locator 和空泛的 `catch (Exception)`。
- 错误返回 RFC 9457 Problem Details，并使用稳定的业务错误码。
- 订单、支付、库存、Webhook 必须考虑幂等、并发、回滚与重复投递。

## 4. 前端规则

- 默认使用 Server Component；仅在需要浏览器 API、交互状态或 Hook 时添加 `use client`。
- 服务端数据使用生成的 OpenAPI 类型和客户端，不手写重复接口类型。
- TanStack Query 管理远程状态；局部交互状态留在组件中，筛选状态放入 URL。
- 表单使用 React Hook Form + Zod，并展示服务端字段错误。
- 不在组件中散落原始 `fetch`、Token 处理、状态码判断或货币格式化逻辑。
- 图片使用 `next/image`，提供准确 `alt`；装饰图片使用空 `alt`。
- 基础组件进入 `components/ui`，业务组件进入 `components/domain` 或对应 feature。
- 不复制已有组件来改变少量样式，优先扩展明确的 variant。
- 保证键盘操作、可见焦点、44px 触控目标和 WCAG 2.2 AA 对比度。

## 5. UI 规则

- 视觉方向是克制的编辑画廊，而非促销型电商模板。
- 作品与创作者必须是页面首要视觉；保持作品 `4:5`、封面 `16:9` 的稳定比例。
- 使用仓库 Token，禁止在业务组件中任意增加相近颜色、圆角和阴影。
- 卡片圆角不超过 8px，不嵌套卡片，不使用装饰性光球或大面积紫色渐变。
- 图标使用 Lucide；图标按钮提供可访问名称，陌生图标提供 Tooltip。
- Studio 页面强调信息密度和操作效率，不套用营销 Hero。
- 修改页面后检查 375px、768px、1440px，确保无横向溢出、遮挡和布局跳动。

## 6. API 契约流程

接口变更采用 contract-first：

1. 先修改 `docs/openapi.yaml`，补全成功响应、错误响应、鉴权和示例/约束。
2. 运行 OpenAPI 校验和破坏性变更检查。
3. 生成或更新 Java 接口/DTO 与 TypeScript 类型。
4. 实现后端行为与权限。
5. 实现前端调用和完整状态：loading、empty、error、success。
6. 添加契约、集成或 E2E 测试。

未经用户明确批准，不删除字段、不改变字段语义、不改变状态码。新增字段优先保持向后兼容。

## 7. 安全红线

- 不提交密钥、Token、Cookie、真实支付信息或个人数据。
- 不把 Access Token 存入 localStorage；Refresh Token 只使用安全 HttpOnly Cookie。
- 不记录密码、鉴权头、完整地址、Stripe client secret 或 Webhook secret。
- 不绕过 Stripe Webhook 验签，不根据前端“支付成功”更新订单。
- 不使用字符串拼接 SQL，不关闭 CSRF/CORS/认证来快速通过测试。
- 不允许客户端指定资源 owner、订单金额、支付状态或创作者收入。
- 不执行破坏性数据库或 Git 命令，除非用户明确要求并确认影响。

## 8. 测试与验证

每次修改后先运行最窄、最能证伪当前实现的检查，再扩大范围。

后端常用验证（项目脚手架建立后）：

```bash
cd apps/api
mvn test
mvn spotless:check
```

或从仓库根运行（多模块视感，便于 CI）：

```bash
mvn -pl apps/api -am test
mvn -pl apps/api -am spotless:check
```

前端常用验证：

```bash
cd apps/web
pnpm lint
pnpm typecheck
pnpm test
```
mvn -pl apps/api -am verify

契约与容器验证：

```bash
pnpm exec @redocly/cli lint docs/openapi.yaml
docker compose -f infra/compose.yaml config
docker compose -f infra/compose.yaml build
```

测试要求随风险增加：

- 纯展示修改：组件测试或视觉检查。
- 业务规则修改：单元测试。
- Repository、Security、事务修改：Testcontainers 集成测试。
- API 修改：OpenAPI 校验与契约测试。
- 支付、库存、登录、履约修改：集成测试和关键 E2E。

不得用删除测试、弱化断言、永久 skip、`any`、关闭 lint 或扩大异常捕获来制造通过。

## 9. 完成标准

任务完成前确认：

- 实现满足请求且没有顺手改动无关代码。
- 接口、实现、生成类型和文档一致。
- 新增行为有与风险相称的测试。
- 已运行相关检查，并在结果中明确说明命令与状态。
- 新增配置同步到 `.env.example`，但不含真实值。
- UI 包含加载、空、错误、禁用和成功状态，并通过响应式与键盘检查。
- 没有引入无必要依赖、抽象、服务或基础设施。

## 10. 与用户沟通

- 先说明当前假设、准备修改的文件和验证方式，再实施。
- 遇到产品歧义时，优先选择可逆、兼容、范围小的方案；不可逆决策再提问。
- 发现无关缺陷时不要扩大任务范围，在最终结果中简要记录即可。
- 最终说明改了什么、为什么、如何验证，以及仍存在的明确风险或未完成项。