# CreatorHub Week 1-2 工程核查报告

> 生成时间：2026-08-25
> 范围：commit `a102e2f` 之上的全量工程（后端骨架 / 前端 skeleton / infra）
> 依据：`.CLAUDE/skills/` 中与本阶段相关的 4 个 skill（`security-best-practices`、`security-threat-model`、`figma-implement-design`、`playwright`）+ 本仓库规范（`AGENTS.md` / `DEVELOPMENT_GUIDE.md` / `figma_design.md`）
> 立场：**被动**审查，未做任何修改；执行需在每条确认后单独立项

---

## 0. 执行摘要

| 类别 | 严重问题 | 中等问题 | 低等问题 | 合计 |
| --- | --- | --- | --- | --- |
| 安全（前端） | 0 | 3 | 2 | 5 |
| 安全（后端） | 0 | 4 | 1 | 5 |
| 视觉 / UI 一致性 | 0 | 3 | 2 | 5 |
| infra / 部署 | 0 | 2 | 1 | 3 |
| 工程卫生 | 0 | 1 | 2 | 3 |
| **合计** | **0** | **13** | **8** | **21** |

**好消息**：没有 Critical 级问题（XSS、SQL 注入、密钥泄漏、auth bypass、远程代码执行）。当前 Week 1-2 阶段工程基座在"安全 by default"上已合格。

**坏消息**：
1. **figma_design.md 与 AGENTS.md 在圆角 / 间距 / 圆角尺寸上有冲突**，目前代码选了 AGENTS.md 侧（8px 圆角），但 figma 文档没同步——会误导后续开发同学
2. **`next.config.ts` 没有 production 必需的安全 header 配置**（CSP / `X-Content-Type-Options` / `Referrer-Policy` / `Permissions-Policy`）—— 后端 Spring Security 还没接，暂时是 Next.js 自管
3. **`infra/web.Dockerfile` 没设置 `NEXT_PUBLIC_API_BASE_URL`，web 容器启动后无法正确连 api**（但用户问"是否要先 build"才暴露）

---

## 1. 安全 — 前端

按 `security-best-practices/references/javascript-typescript-nextjs-web-server-security.md` 与 `javascript-typescript-react-web-frontend-security.md` 逐条核查。

### SEC-WEB-001 (Medium) — 缺少安全 Header 中间件

**位置**：`apps/web/next.config.ts`

**证据**：当前 `next.config.ts` 只配置了 `reactStrictMode` + `images.remotePatterns`。无 CSP / `X-Content-Type-Options` / `X-Frame-Options` / `Referrer-Policy` / `Permissions-Policy`。

**影响**：浏览器侧缺乏对 XSS、clickjacking、MIME sniffing、referrer 泄漏的纵深防御。

**修复**：在 `next.config.ts` 的 `async headers()` 中加全局 header。`Strict-Transport-Security` 在 self-host 时按需（skill 警告不强制）。

```ts
// next.config.ts 应增加
async headers() {
  return [{
    source: "/:path*",
    headers: [
      { key: "X-Content-Type-Options", value: "nosniff" },
      { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
      { key: "X-Frame-Options", value: "DENY" },
      { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" }
      // CSP 建议先以 report-only 上线一周，再切 enforce
    ]
  }];
}
```

**验证**：`pnpm web:build && pnpm web:start` 后 `curl -I http://localhost:3000/` 检查 header 存在。

---

### SEC-WEB-002 (Medium) — 登录/注册表单无 CSRF 防护

**位置**：`apps/web/src/app/(auth)/auth/login/page.tsx`、`register/page.tsx`

**证据**：`<form action="/auth/login" method="post">` 是裸 HTML form 提交。Week 3 Identity 接入前是占位，但**已上线的 form action 必须配 CSRF**——等到 Identity 实现时不可再裸 form。

**影响**：Cookie 鉴权后，任何第三方网站都能让用户向 /auth/login POST，CSRF 攻击面。

**修复**：Week 3 Identity 实现时：
- 用 Server Action（Next.js 16 内置 origin 校验）
- 或 `fetch('/api/v1/auth/login', {credentials: 'include'})` 走 `apiFetch` 并要求 Cookie 上加 `SameSite=Lax`（`application.yml` 已配）

**当前**：`secure: false` + `same-site: lax` 已在 `application.yml`，但**默认 API 路由还没启用**——Week 3 才生效。

**验证**：等 Identity 落地后用 `playwright` skill 跑 CSRF 探测：跨域 POST 应被 403 / 401。

---

### SEC-WEB-003 (Medium) — `next/image` `remotePatterns` 通配

**位置**：`apps/web/next.config.ts`

**证据**：

```ts
remotePatterns: [
  { protocol: "https", hostname: "**" },
  { protocol: "http", hostname: "localhost" }
]
```

`hostname: "**"` 允许任意 https 主机（包括恶意上传到 attacker.com 的图）。`next/image` 优化器会下载到自己的服务器，存在 SSRF 风险（构造恶意 URL 让 Next 服务器去访问内部网段）。

**影响**：
1. **滥用优化器带宽**（任意 URL 触发下载）
2. **SSRF**：恶意 URL 让 Next 服务器请求内部端口
3. **内容来源不受控**（图片被替换为不当内容）

**修复**：收紧白名单。当前 mock 数据只用 `images.unsplash.com`，正式上线需要：
- 用户上传图片走 `/api/v1/media/upload-requests` 拿预签名 URL 上传到自有 S3
- `remotePatterns` 限定为 `images.unsplash.com`（mock 阶段）+ 自有 CDN domain

**验证**：`pnpm web:build` 后用 `node -e "console.log(new URL('http://localhost:3000/_next/image?url=https://attacker.com/x.jpg'))"` 测被 Next 拒绝。

---

### SEC-WEB-004 (Low) — 缺少 `NEXT_PUBLIC_*` 注释

**位置**：`apps/web/src/lib/api/fetcher.ts:36` `process.env.NEXT_PUBLIC_API_BASE_URL`

**证据**：未在代码注释中说明 `NEXT_PUBLIC_*` 会暴露到客户端 bundle。

**影响**：低——这本身是 Next.js 标准行为，新人可能误把 JWT secret 写成 `NEXT_PUBLIC_*` 暴露给浏览器。

**修复**：在 `lib/config.ts` 加注释 "All `NEXT_PUBLIC_*` env vars are inlined into the browser bundle. Never put secrets here."

---

### SEC-WEB-005 (Low) — `error.tsx` 直接 console.error 原始 Error

**位置**：`apps/web/src/app/error.tsx:14-18`

**证据**：

```ts
if (process.env.NODE_ENV !== "production") {
  console.error("App error:", error);
}
```

**影响**：本地开发 OK，但**生产环境如果 NODE_ENV 没被正确设置为 "production"（Next.js 16 有此问题历史），会向浏览器 console 泄漏 Error 详情**。注意 Next.js 自带处理应该会盖掉 NODE_ENV，但仍值得防御。

**修复**：改用 `process.env.NEXT_PUBLIC_APP_ENV === 'development'` 这种应用层 env 区分，不依赖 NODE_ENV。

---

## 2. 安全 — 后端

按 `security-best-practices` 的"先识别语言/框架再看 references"流程。后端是 Java + Spring Boot 3.5，**该 skill 不直接支持 Java**，但 `references/security-controls-and-assets.md` 与 `prompt-template.md`（来自 `security-threat-model`）通用。下面是按 Spring Boot 通用 OWASP Top 10 做的核查。

### SEC-API-001 (已解决) — Flyway migration 已落地并移除 Hibernate ddl-auto

> 2026-08-26 更新：V1/V2 Flyway migration 已通过 PostgreSQL 17 Testcontainers；持久化层已迁移到 MyBatis-Plus，`spring.jpa.hibernate.ddl-auto` 已删除。以下保留原始审计证据，便于追溯。

**原位置**：`apps/api/src/main/resources/application.yml:18-22`

**证据**：

```yaml
jpa:
  hibernate:
    ddl-auto: validate
flyway:
  enabled: true
```

`flyway.enabled: true` 但 `db/migration/` 目录还没创建任何 migration，**首次启动会因找不到 migration 报 `FlywayValidateException`**。

**影响**：docker compose `up` 直接失败，业务上下文无法启动。

**修复**：Week 3 Identity 落地时**第一个 PR 必须包含** `apps/api/src/main/resources/db/migration/V1__init_identity.sql`：
- `users` / `sessions` 表
- 索引：email（unique）、refresh_token_hash

**验证**：`mvn -B -ntp -f apps/api/pom.xml -Dtest='UserRepositoryAdapterTest,CreatorRepositoryAdapterTest' test` 已通过；Flyway V1/V2 成功应用。

---

### SEC-API-002 (Medium) — `creatorhub.security.jwt.secret` 默认值不可生产

**位置**：`apps/api/src/main/resources/application.yml:48-52`

**证据**：

```yaml
creatorhub:
  security:
    jwt:
      secret: ${JWT_SECRET:change-me-please-change-me-please-32bytes}
```

默认值是固定字符串。**生产环境如果忘了设 `JWT_SECRET` env，会用这个弱默认**，攻击者能伪造任意 Access Token。

**影响**：Critical（如果忘设 env）；但**当前没有生产部署**，故 Medium。

**修复**：
1. 删除默认值，让 `${JWT_SECRET}` 无默认 → 启动时 `IllegalArgumentException` 提示必填
2. 或者保留默认值但加 `@PostConstruct` 检查 `env == 'production' && secret == default` 时 fail-fast
3. 文档：在 `infra/env.example` 加醒目的 **"生产环境必须改"** 注释

**验证**：测试 `JWT_SECRET=change-me...` + `SPRING_PROFILES_ACTIVE=production` → 启动失败。

---

### SEC-API-003 (Medium) — 缺少安全 Header Filter

**位置**：`apps/api/src/main/java/com/creatorhub/config/`（没有 SecurityFilter 或 WebMvcConfig）

**证据**：当前没有 `SecurityFilterChain` Bean（Spring Security 依赖在但没启用）、没有 WebMvcConfigurer、没有 HeaderFilter。

**影响**：响应没有 `X-Content-Type-Options`、`Cache-Control: no-store`（敏感页面需要）等。

**修复**：Week 3 Identity 引入 Spring Security 时一并加 `SecurityFilterChain` + `HeaderWriterFilter`：
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Cache-Control: no-store` for `/api/v1/auth/**`
- HSTS 按 env（dev 关闭，prod 开启）

---

### SEC-API-004 (Medium) — GlobalExceptionHandler 兜底返回 500 但 detail 暴露

**位置**：`apps/api/src/main/java/com/creatorhub/config/GlobalExceptionHandler.java:60+`

**证据**：`@ExceptionHandler(Exception.class)` 把 `ex.getMessage()` 直接写入 ProblemDetail `detail` 字段。

**影响**：**信息泄漏**——异常消息常含 SQL、内部类名、文件路径、堆栈片段。例如 NPE 暴露 `Cannot read property "x" of undefined at line 23 of UserService`。

**修复**：
- 兜底分支只返回 `"INTERNAL_ERROR"` 固定 message，**不**写入 `ex.getMessage()`
- 真实 message 通过 `Logger.error(..., ex)` 写日志（已带 RequestId）
- 对 `BusinessError` / `MethodArgumentNotValid` / `ConstraintViolation` 三个**已知**异常保留 detail

**验证**：故意制造 NPE，调用方收到 `"detail": "服务暂时不可用"` 而非 SQL 片段。

---

### SEC-API-005 (Low) — `allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3000}`

**位置**：`apps/api/src/main/resources/application.yml:60-62`

**证据**：默认值是 localhost，仅 dev OK。**没有 CORS Filter Bean 实际生效**（Spring Web 默认不暴露 CORS）。

**影响**：低（因为 CORS 还没启用）；但配置项存在会让"在生产"前临时改 env 时容易忽略——`CORS_ALLOWED_ORIGINS` 应被 **Spring Security CORS 强制使用**（Week 3 落地时）。

**修复**：Week 3 Security 引入时加 `@Bean CorsConfigurationSource` 把这个值映射到 `setAllowedOrigins`。

---

## 3. 视觉 / UI 一致性

按 `figma-implement-design` 的 "1:1 design token 还原" 原则，对照 `figma_design.md` 查 CreatorHub 实际值。

### VIS-001 (Medium) — figma_design.md 圆角与 AGENTS.md §5 冲突

**位置**：`figma_design.md` §5 Radius System vs `AGENTS.md` §5 "卡片圆角不超过 8px"

**证据**：
- figma_design.md：S=8px, M=16px, L=24px, XL=32px, Pill=999px
- AGENTS.md §5："卡片圆角不超过 8px，不嵌套卡片"
- 实际 Tailwind config：`borderRadius.token: 8px`，Button / Card / Modal 都用 `rounded-token`（=8px）

**优先级判定**：`AGENTS.md` §1 第 4 条 "DEVELOPMENT_GUIDE.md > figma_design.md"——AGENTS 自身也是顶层规范。当前选了 AGENTS 侧（8px）正确。

**但 figma 文档没同步**——会让 design partner / 新成员按 figma 做 UI，提交 PR 后才发现冲突。

**修复**：
- figma_design.md §5 加一行："CreatorHub 实际 token 受 `AGENTS.md §5 UI 规则` 约束：圆角上限 8px；figma 的 16/24/32px 方案在本项目中**不适用**"
- 改后让 design partner 重新对照 figma 调整视觉

**验证**：`grep "圆角" figma_design.md AGENTS.md` 应清晰显示优先级。

---

### VIS-002 (Medium) — Section Gap token 未被使用

**位置**：`apps/web/src/styles/globals.css:36` (`--space-section-y: 4rem`) vs `apps/web/src/app/page.tsx:18` (`py-10` = 40px)

**证据**：globals.css 定义的 section y 间距是 64px（4rem = 64px，匹配 figma "Section Gap 64px"），但 `Discover` / `Artwork` / `Creator` 页面都用了 `py-10`（=40px）硬编码。

**影响**：与 figma 不一致；token 定义了但没起作用。

**修复**：把 `py-10` 全部替换为 `py-16`（64px）匹配 token。或者在 globals.css 暴露 utility：

```css
.section-y { padding-top: var(--space-section-y); padding-bottom: var(--space-section-y); }
```

**验证**：`grep -r "py-10" apps/web/src/app` 应清空（除 `(auth)` 内的紧凑表单）。

---

### VIS-003 (Medium) — H1 字号与 figma 不一致

**位置**：`apps/web/src/app/page.tsx:20` (`text-3xl md:text-4xl`) vs figma H1 = 36px

**证据**：
- figma H1 = 36px / 44 line-height / 600 weight
- Discover 页：`text-3xl` = 30px（base），`md:text-4xl` = 36px（md+）
- 实际：base (375px) 是 30px，desktop (1440px) 才是 36px
- Artwork Detail / Creator Profile / 404 / 500 / 登录 / 注册 都用 `text-2xl` / `text-3xl`

**影响**：base 字号比设计小；用户在小屏上"标题感"不足。

**修复**：把页面级 H1 统一为 `text-3xl`（30px）或 `text-4xl`（36px），并用 CSS 暴露 `--font-size-h1` token；`text-3xl md:text-4xl` 这种"阶梯"用法对齐 figma Display / Heading 分级。

**验证**：375px / 768px / 1440px 三个断点截图，比较 H1 像素高度。

---

### VIS-004 (Low) — 缺少 Price Typography 24px/Bold 专用样式

**位置**：`apps/web/src/components/domain/ArtworkCard.tsx:42-44` 与 `artworks/[id]/page.tsx:39-41`

**证据**：figma "Price Typography: 24px Bold"。当前 ArtworkCard 用 `text-sm` (14px)，详情页用 `text-2xl` (24px) 但**没有 `font-bold` 标记**。

**影响**：Card 卡片小图，price 应小；详情页 price 应大且醒目。当前详情页 price 字号对了但粗体缺失。

**修复**：
- 详情页价格：`text-2xl font-semibold`（24px + 600 weight）
- 卡片价格：保持 `text-sm`（小图用大价反客为主）

**验证**：devtools 看详情页 price 元素 `font-weight`。

---

### VIS-005 (Low) — `(auth)` 页面 padding 与 figma Card Padding 20px 不一致

**位置**：`apps/web/src/app/(auth)/auth/login/page.tsx:6`、`register/page.tsx:6`

**证据**：`p-6` (24px) vs figma "Card Padding 20px"

**影响**：差 4px，肉眼难辨；属于"基本可接受"。

**修复**：把 `p-6` 改 `p-5`（20px），或在 Tailwind config 加 `spacing.card: '20px'` token。

---

## 4. Infra / 部署

### INFRA-001 (Medium) — `web.Dockerfile` `depends_on` 没等 `api` healthy

**位置**：`infra/compose.yaml:103`

**证据**：

```yaml
web:
  depends_on:
    - api   # 缺 condition: service_healthy
```

`api` 已有 healthcheck，web 应该等 `api` healthy 再启动，避免 web 启动时去连未就绪的 api。

**影响**：低概率（web 启动很慢于 api），但生产 k8s 化时需要。

**修复**：

```yaml
web:
  depends_on:
    api:
      condition: service_healthy
```

---

### INFRA-002 (Medium) — `web.Dockerfile` 缺 `NEXT_PUBLIC_API_BASE_URL`

**位置**：`infra/web.Dockerfile`（runtime stage）

**证据**：runtime 阶段没有 `ENV NEXT_PUBLIC_API_BASE_URL=...`。前端 `getApiBaseUrl()` 默认 `http://localhost:8080`，但容器间通过 `creatorhub_net` 网络访问，应该是 `http://api:8080`。

**影响**：web 容器内调用 api 会失败（"localhost:8080" 在 web 容器内 = web 自己的 8080，没人监听）。

**修复**：

```dockerfile
# 在 web.Dockerfile runtime stage
ENV NEXT_PUBLIC_API_BASE_URL=http://api:8080
```

或在 `compose.yaml` 的 `web.environment` 覆盖（已有 `${NEXT_PUBLIC_API_BASE_URL:-http://localhost:8080}` 但默认值错——应该是 `http://api:8080`）。

**验证**：`docker compose up`，`docker exec creatorhub-web wget -qO- http://api:8080/actuator/health` 应返回 UP。

---

### INFRA-003 (Low) — 缺少 `infra/.dockerignore` 限制构建上下文

**位置**：`infra/`（不存在 `.dockerignore`）

**证据**：Dockerfile `COPY . ..` 会把仓库所有文件（含 `.git`、`.CLAUDE/`、`node_modules`、`apps/api/build/`）打包进构建上下文，**对单 stage Dockerfile 影响构建速度**（不会影响最终镜像大小，但会拖慢 `docker build`）。

**修复**：加 `infra/.dockerignore`：

```
.git
.CLAUDE
docs
node_modules
apps/api/.gradle
apps/api/build
apps/web/.next
apps/web/node_modules
pnpm-lock.yaml.bak
*.md
```

---

## 5. 工程卫生

### ENG-001 (Medium) — `apps/api/build.gradle.kts` 没有 spotless / checkstyle

**位置**：`apps/api/build.gradle.kts`（缺）

**历史证据**：旧版 AGENTS.md §8 曾提到 `./gradlew spotlessCheck`，但旧版 `build.gradle.kts` 未引入。当前已迁移到 Maven，并由 `spotless-maven-plugin` 提供 `mvn spotless:check`。

**影响**：无强制格式化 → PR 容易风格漂移。

**修复**：Week 3 第一个 PR 加：

```kotlin
plugins {
    id("com.diffplug.spotless") version "6.25.0"
}
spotless {
    java {
        googleJavaFormat()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlin {
        ktlint()
    }
}
```

---

### ENG-002 (Low) — `tsconfig.tsbuildinfo` 偶发进版本控制

**位置**：`apps/web/tsconfig.tsbuildinfo`（可能再被生成）

**证据**：`apps/web/.gitignore` 已加 `tsconfig.tsbuildinfo`，但 `tsc --noEmit` 跑后会生成；目前 git status 不再报，说明 ignore 生效。但仍要确保 CI 不上传。

**修复**：保持 ignore。**已 OK**，不需要额外动作。

---

### ENG-003 (Low) — `compose.yaml` 默认 `JWT_SECRET` 与 `application.yml` 不同

**位置**：`infra/compose.yaml:75` vs `apps/api/.../application.yml:50`

**证据**：compose 用 `${JWT_SECRET:-change-me-please-change-me-please-32bytes}`，application.yml 默认值也是这个。但**两处独立维护**，将来改 secret 时容易漏改。

**修复**：在 `infra/env.example` 顶部写明确：

```bash
# 同一 secret 在 application.yml 与 compose.yaml 各引用一次，确保同步
JWT_SECRET=change-me-please-change-me-please-32bytes
```

**当前**：`infra/env.example` 已写，**OK**，但建议加注释说"两处都要更新"。

---

## 6. 不在范围 / 已正确

按 `figma-implement-design` "1:1 design token" 原则，下述已正确实现：

- ✅ **零 XSS 面**：未使用 `dangerouslySetInnerHTML` / `innerHTML` / `localStorage` / `eval` / `new Function`
- ✅ **Token 化颜色系统**：ink / brand / success / error / warning 与 figma HEX 一致
- ✅ **Inter + Source Serif 4** 与 figma 字体一致
- ✅ **`<Image alt>`** 在所有用图处都设置（`ArtworkCard` / `CreatorCard` / `MediaGallery` / 详情页封面）
- ✅ **`<Link>`** 与 `aria-label` 在交互元素上都有
- ✅ **焦点样式**：`:focus-visible` 2px brand outline + 2px offset（符合 WCAG 2.2）
- ✅ **`prefers-reduced-motion`** 全局禁用动画
- ✅ **44px touch target**：`.touch-target` utility 在 Modal Close 上用了
- ✅ **Header 注入的 Request ID**：所有 `apiFetch` 都注入 `X-Request-Id` ✓
- ✅ **GraphQL 不存在**（违反 project 规范 ✓）
- ✅ **`.result<T>` 包装** 不存在（符合项目规范 ✓）
- ✅ **没有 Next.js dev 模式进生产**（Dockerfile 用 `next start` ✓）

---

## 7. 行动建议（按优先级）

| 优先级 | 编号 | 改动 | 估时 |
| --- | --- | --- | --- |
| P0 | INFRA-002 | web.Dockerfile 加 `NEXT_PUBLIC_API_BASE_URL=http://api:8080` | 5 分钟 |
| P0 | INFRA-001 | web depends_on 加 `condition: service_healthy` | 5 分钟 |
| P1 | SEC-API-001 | Week 3 第一个 PR 加 Flyway V1__init_identity | 跟随 Identity 落地 |
| P1 | SEC-WEB-001 | next.config.ts 加 headers() | 30 分钟 |
| P1 | VIS-002 | py-10 → py-16（5 个页面） | 15 分钟 |
| P1 | VIS-001 | figma_design.md §5 加 AGENTS.md 冲突说明 | 10 分钟 |
| P2 | SEC-WEB-003 | remotePatterns 收紧到白名单 | 15 分钟 |
| P2 | SEC-API-002 | 删除 jwt.secret 默认值，或加 fail-fast | 15 分钟 |
| P2 | SEC-API-004 | GlobalExceptionHandler 兜底分支不写 ex.getMessage() | 15 分钟 |
| P2 | VIS-003 | H1 字号统一到 36px（或暴露 token） | 30 分钟 |
| P3 | 其他 Low | 视情况本周内或与 Week 3 一起 | < 1h |

---

## 8. 后续 milestone 检查清单

- **Week 3 Identity**：
  - 必做：SEC-API-001 / SEC-API-002 / SEC-API-003 / SEC-API-005 / SEC-WEB-002
  - 同步：ENG-001 spotless
- **Week 4 Media**：
  - 必做：SEC-WEB-003（remotePatterns 收窄）
- **Week 5 Catalog**：
  - 必做：VIS-003 字号 token
- **Week 7 Payment**：
  - 必做：SEC-API-004（避免 Stripe SDK 异常 message 泄漏到 detail）
  - 必做：INFRA-001/002/003 全部
  - 同步：playwright skill 跑"注册→下单→支付"关键 E2E

---

## 9. 报告生成信息

- 报告路径：`docs/security/week-1-2-audit.md`
- 生成依据：`.CLAUDE/skills/skills/.curated/security-best-practices` + `security-threat-model` + `figma-implement-design` + `playwright`
- 覆盖范围：commit `a102e2f`（HEAD）
- 未跑任何自动化扫描（archunit、snyk、gitleaks）—— 等后端有源码与 CI 接入后补
