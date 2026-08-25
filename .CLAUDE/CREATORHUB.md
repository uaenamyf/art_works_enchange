# `.CLAUDE/skills/` 在 CreatorHub 工程中的使用

> 本目录是 CreatorHub 工程**引用**的外部技能仓，**不是 CreatorHub 自身代码的一部分**。
> 本身是独立 git 仓库，镜像自 [openai/skills](https://github.com/openai/skills)。

## 重要：上游已声明弃用

`.CLAUDE/skills/README.md`（上游自带）有这段说明：

> **This repository is deprecated.** For current Codex skill and plugin examples, use the [OpenAI Plugins repository](https://github.com/openai/plugins).

也就是说：

- `.CLAUDE/skills/` 内容是 **2026-06 之前** OpenAI 公开的 Codex skills 镜像
- 上游已**废弃**该仓，后续 Codex 技能以 `openai/plugins` 为准
- CreatorHub 当前仍可使用其内容作为通用方法论参考；新增技能需求应优先评估 `openai/plugins`

## 仓属性

| 字段 | 值 |
| --- | --- |
| 来源 | `https://github.com/openai/skills` |
| 镜像位置 | `D:\art_works_enchange\.CLAUDE\skills\`（本机路径） |
| 远端 | `origin` → `https://github.com/openai/skills.git` |
| 当前 HEAD | `49f948f`（2026-06-23，更新 skill installer post-install 指引） |
| 仓本身内容 | `.curated/` 37 个通用 skill + `.system/` 5 个系统 skill |
| 总规模 | 42 个 skill，每个含 `SKILL.md` + `agents/openai.yaml` + `assets/` + `references/`，部分含 `scripts/` |

## 与 CreatorHub 的关系

```text
CreatorHub 仓库（d:\art_works_enchange\）
├── AGENTS.md           ← CreatorHub 仓库协作规范
├── DEVELOPMENT_GUIDE.md
├── docs/
├── apps/
└── .CLAUDE/
    ├── CLAUDE.md       ← Claude 工作补充说明（已通过 attachments 注入到 agent 工作说明）
    └── skills/         ← 独立 git 仓，OpenAI skills 镜像（不参与业务构建）
        ├── .git/
        ├── README.md   ← 上游自带，本文件不覆盖
        ├── CREATORHUB.md ← 本文件（CreatorHub 视角的本地使用说明）
        └── skills/
            ├── .curated/   37 个通用 skill
            └── .system/    5 个系统 skill
```

- **不**在 CreatorHub 工程的 `apps/`、`docs/`、根 `package.json` 引入
- **不**参与 Gradle / pnpm / Docker 构建
- **不**进入根仓 `git add` 范围——`.CLAUDE/skills/` 是子 git 仓，根仓是否 track 视团队约定
- 与 `AGENTS.md` §2 提到的 `.agent/.skill` 是**两套不同**的引用：`.agent/.skill/` 存 CreatorHub 业务专属 DDD 技能（按需补充），`.CLAUDE/skills/` 是通用 OpenAI 技能库

## 怎么用

### 列出所有可用 skill

```powershell
cd D:\art_works_enchange\.CLAUDE\skills
git log --oneline -1                  # 确认 HEAD 与本 README 一致
cmd /c "dir /A /B skills\.curated"   # 列出 37 个 curated skill
cmd /c "dir /A /B skills\.system"    # 列出 5 个 system skill
```

### 同步到上游最新

```powershell
cd D:\art_works_enchange\.CLAUDE\skills
git fetch origin
git log @{u}.. --oneline              # 看上游比你多哪些 commit
git pull --ff-only                    # 仅快进合并
```

> ⚠️ **不建议**继续长期跟随上游——仓已 deprecated，长期跟随成本 > 价值。

### 未来方向

CreatorHub 的工作流需要新增技能时：

1. 优先看 [`openai/plugins`](https://github.com/openai/plugins)（上游推荐替代）
2. 实在没有再自己写，放到 CreatorHub 仓内的 `.agent/.skill/<topic>/SKILL.md`
3. 不要往 `.CLAUDE/skills/` 里新增 CreatorHub 专属 skill（它已是 deprecated 上游镜像）

## 在开发时引用一个 skill

技能通过 Claude / Codex agent 的 `SKILL.md` 触发，无需手动调用。当任务匹配某个 skill 的 `description` 字段，agent 会自动加载对应 `SKILL.md` 及其子 `references/`。

对 CreatorHub 真正可能用到的（按当前 Week 1-8 计划）：

| 任务 | 可能用到的 skill（路径，相对 `.CLAUDE/skills/skills/`） | 备注 |
| --- | --- | --- |
| 把 Figma 设计稿落到代码 | `.curated/figma-implement-design/`、`.curated/figma-use/` | Week 5+ Studio 与 Discover 页面落地时 |
| 写 E2E 测试 | `.curated/playwright/`、`.curated/playwright-interactive/` | Week 8+ 关键工作流（注册→下单→支付） |
| Sentry 接入 | `.curated/sentry/` | Week 1-2 已留口子（`error.tsx`），接 Sentry 时启用 |
| 安全评审 | `.curated/security-best-practices/`、`.curated/security-threat-model/` | Week 3 Identity + Week 7 Payment 上线前 |
| 部署 | `.curated/vercel-deploy/`、`.curated/render-deploy/` | Web 端；API 端走 Docker Compose（见 `infra/compose.yaml`） |
| 调试 / 文档查阅 | `.system/openai-docs/` | 按需 |

**未列出**的不代表禁用，但**默认不主动加载**——按需在 prompt 中点名（如「用 `cli-creator` 的方法帮我想 X」）。

## 与 CreatorHub 边界规则

`AGENTS.md` §2 已规定：

> `.agent/.skill` 是通用参考，不是仓库规范；优先级低于用户要求、`AGENTS.md`、`docs/openapi.yaml`、`DEVELOPMENT_GUIDE.md` 和 ADR。

`.CLAUDE/skills/` 与 `.agent/.skill/` 同属"通用参考"层，**约束一致**：

1. **不**用 `.CLAUDE/skills/` 中的模板覆盖 CreatorHub 已确定的技术选型（见 `DEVELOPMENT_GUIDE.md` §2）
2. **不**采用其中与本仓库 `AGENTS.md` 冲突的 DDD 模板（特别是禁 `Result<T>`、数字错误码、每端一个 BFF、微服务、MyBatis、Sa-Token、DDD4J、COLA）
3. **不**把 `references/` 里的目录结构或文件路径直接复制到 CreatorHub 工程
4. **不**把它当 git submodule 在 CreatorHub 仓内 commit；保留它作为独立子仓
5. 引用具体 skill 时只引用**方法论与检查清单**，不直接拷贝代码片段到 CreatorHub 业务代码

## 维护

- **谁来更新**：CreatorHub 工程的开发同学**不建议**主动 `git pull` 同步上游；上游已 deprecated，长期维护无价值
- **冲突解决**：本仓与上游的差异应**反向**回写给上游（fork + PR），不要在本地长期维护分支
- **清理**：如果 CreatorHub 不再需要这个镜像，整个 `.CLAUDE/skills/` 可以删除；只需要先确认 `apps/`、`docs/`、`infra/` 没有依赖它的文件路径
- **本文件**：不覆盖上游 `README.md`；CreatorHub 视角的说明放在 `CREATORHUB.md`，避免与上游同步时被冲突

---

## 变更记录

| 日期 | HEAD | 说明 |
| --- | --- | --- |
| 2026-08-25 | `49f948f` | 首次登记到 CreatorHub 仓库；新建本 `CREATORHUB.md` |
