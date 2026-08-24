# studio feature

> Creator Studio：Dashboard、作品管理、订单履约、设置。

## 进入条件

- 调整 `/studio/*` 路由与组件。
- 调整 Studio Dashboard 数字卡或布局。

## 必读

- 根 `AGENTS.md` §4、§5、§7。
- `docs/openapi.yaml` 标签 `Studio`。
- 14 周计划 Week 10、Week 11、Week 12。

## 约束

- Studio 默认使用 Client Component（信息密度高、交互多）。
- 复刻 `Design Token`，避免营销视觉与紫色渐变。
- 写操作携带乐观锁 `version`；失败时展示 Problem Details。
- 履约状态变更（PROCESSING、SHIPPED）严格遵循后端状态机。
- 移动端为单列布局；侧边栏在 `< 1024px` 折叠为顶部抽屉。

## 常用命令

```bash
pnpm --filter @creatorhub/web test -- studio
```

## 交叉引用

- `artworks/AGENTS.md` — 共享作品卡片与上传。
- `orders/AGENTS.md` — 订单展示。
- `components/layout/AGENTS.md` — Studio 布局。
