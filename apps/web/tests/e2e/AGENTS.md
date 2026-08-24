# tests/e2e — Playwright

> 端到端关键路径。Week 12 至少覆盖四条：Discover、Purchase、Creator Publish、Creator Fulfillment。

## 进入条件

- 新增/调整 E2E 流程或共用 fixture。

## 必读

- 根 `AGENTS.md` §8、§9。
- 根 `DEVELOPMENT_GUIDE.md` §9 测试策略。
- 14 周计划 Week 11、Week 12。

## 约束

- 优先使用语义定位器（`getByRole`、`getByLabel`），避免脆弱 CSS Selector。
- CI 失败必须保留 trace、截图与视频。
- E2E 默认运行在本地 Compose 环境；不依赖外部网络。
- 不在 E2E 中 mock 业务规则；只覆盖真实路径。
- 每条 E2E 配套一个固定种子数据；不要在测试中再创建 Creator/作品。

## 常用命令

```bash
pnpm exec playwright test
pnpm exec playwright test --grep 'purchase'
pnpm exec playwright show-report
```

## 交叉引用

- `features/*/AGENTS.md` — 各领域四态约定。
- `lib/api/AGENTS.md` — Problem Details 与 `X-Request-Id` 校验。
