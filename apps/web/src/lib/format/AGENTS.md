# lib/format

> 货币、日期、数字与本地化的展示工具。

## 进入条件

- 调整货币或日期显示规则。
- 引入新的本地化或格式化函数。

## 必读

- 根 `AGENTS.md` §4。
- 根 `DEVELOPMENT_GUIDE.md` §6。

## 约束

- 使用 `Intl.NumberFormat` / `Intl.DateTimeFormat`，不引入额外库除非必要。
- 货币与币种来自后端 `Money` DTO；前端不硬编码。
- 组件中不散落 `toFixed`、`toLocaleString` 等临时格式化。

## 常用命令

```bash
pnpm --filter @creatorhub/web test -- lib/format
```

## 交叉引用

- `components/domain/AGENTS.md` — 业务展示组件。
