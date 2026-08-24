# components/domain

> 业务组件：ArtworkCard、CreatorCard、MediaGallery、PriceTag、OrderSummary 等。

## 进入条件

- 新增/调整业务展示组件。
- 调整卡片、画廊或订单展示的视觉与状态。

## 必读

- 根 `AGENTS.md` §5 UI 规则。
- 根 `DEVELOPMENT_GUIDE.md` §7。
- `docs/openapi.yaml` 资源模型。

## 约束

- 严格使用 Design Token；不引入业务自定颜色/阴影。
- 4:5 作品卡、16:9 封面、1:1 头像比例固定。
- 不嵌套卡片；不放大紫色渐变。
- 图标按钮必须有可访问名称与 Tooltip。
- 状态：loading、empty、error、success 必须显式存在。

## 常用命令

```bash
pnpm --filter @creatorhub/web test -- components/domain
```

## 交叉引用

- `components/ui/AGENTS.md` — 基础组件复用。
- `features/*/AGENTS.md` — 各领域使用约定。
