# creators feature

> 创作者公开主页、申请 Creator、编辑我的创作者资料。

## 进入条件

- 调整 `/creators/*`、`/creators/[id]`、`/me/creator`。
- 调整创作者卡片或申请表单。

## 必读

- 根 `AGENTS.md` §4、§5。
- `docs/openapi.yaml` 标签 `Creators`。
- 14 周计划 Week 3、Week 5。

## 约束

- 创作者主页使用 Server Component 拉取首屏。
- `CreatorCard` 必须落在 `components/domain/creator`。
- 不在前端隐藏权限判断：非创作者调用 `/me/creator` PUT 必须返回 403。
- 申请表单只接受 `ApplyCreatorRequest` 字段，不发送额外属性。

## 常用命令

```bash
pnpm --filter @creatorhub/web test -- creators
```

## 交叉引用

- `follows/AGENTS.md` — 关注按钮。
- `components/domain/creator/AGENTS.md` — 共享组件。
