# components/ui

> 无业务的基础组件：Button、Input、Modal、Tabs、Skeleton、Toast 等。

## 进入条件

- 新增/调整通用基础组件或扩展变体。

## 必读

- 根 `AGENTS.md` §5 UI 规则。
- 根 `DEVELOPMENT_GUIDE.md` §7.1 Design Tokens。
- 根 `figma_design.md`。

## 约束

- 仅依赖 Radix UI/Lucide，不耦合业务 DTO。
- 圆角 ≤ 8px；不嵌套卡片。
- 所有变体必须可访问：键盘可达、焦点可见、44px 触控目标。
- 动效 160–240ms，尊重 `prefers-reduced-motion`。
- Story 或 Vitest 至少覆盖 default/hover/disabled/loading 四态。

## 常用命令

```bash
pnpm --filter @creatorhub/web test -- components/ui
```

## 交叉引用

- `components/domain/AGENTS.md` — 业务组件基于基础组件组合。
- `src/styles/AGENTS.md` — Design Token 注入。
