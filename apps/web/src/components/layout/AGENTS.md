# components/layout

> 通用布局：Header、Footer、BottomNav、Studio Sidebar、EmptyState、ErrorBoundary。

## 进入条件

- 新增/调整全局或路由级布局。
- 调整导航、侧边栏与错误边界。

## 必读

- 根 `AGENTS.md` §5、§7。
- 根 `DEVELOPMENT_GUIDE.md` §6 前端设计、§7 响应式与无障碍。
- 14 周计划 Week 2、Week 5、Week 10。

## 约束

- Header 桌面 80px、Mobile Bottom Nav 72px，使用仓库 Token。
- 触控目标 ≥ 44px；焦点可见、键盘可达。
- 移动端在 < 1024px 折叠侧边栏为顶部抽屉。
- 不嵌入营销式 Hero；Studio 走信息密度布局。

## 常用命令

```bash
pnpm --filter @creatorhub/web test -- components/layout
```

## 交叉引用

- `components/ui/AGENTS.md`、`components/domain/AGENTS.md`。
- `src/app/AGENTS.md` — Layout 嵌套。
