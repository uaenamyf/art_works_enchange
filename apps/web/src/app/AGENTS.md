# src/app — Next.js App Router

> 路由层。使用 Route Group（(auth)/(market)/(checkout)/(account)/studio）组织页面与布局。

## 进入条件

- 新增/调整页面或布局文件。
- 调整路由策略、Layout 嵌套或 Loading/Error 边界。

## 必读

- 根 `AGENTS.md` §4、§5、§6。
- 根 `DEVELOPMENT_GUIDE.md` §6 前端设计。
- 14 周计划 Week 2、Week 5、Week 10、Week 11。

## 约束

- 默认 Server Component；只在需要浏览器 API、交互或 Hook 时加 `use client`。
- 每条路由必须考虑 loading、empty、error、success 四态；禁止传统全页 spinner。
- 路由级 `error.tsx` 必须回退到 Problem Details 友好提示。
- URL 保留可分享状态（搜索词、筛选、cursor）。
- 移动端三档断点（375/768/1440）需视觉检查。

## 常用命令

```bash
pnpm --filter @creatorhub/web dev
pnpm --filter @creatorhub/web test -- app
```

## 交叉引用

- `features/*/AGENTS.md` — 各领域业务规则。
- `components/layout/AGENTS.md` — 通用布局。
