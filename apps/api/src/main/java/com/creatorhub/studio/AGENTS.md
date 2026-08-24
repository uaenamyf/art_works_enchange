# studio

> Studio 不是独立限界上下文。它是面向创作者的接口与查询视图，复用 Creator / Catalog / Ordering 公开端口。

## 进入条件

- 修改 `/studio/*` 接口或 Studio Dashboard 投影。
- 为 Studio 调整权限、聚合或新增只读视图。

## 必读

- 根 `AGENTS.md` §3。
- 根 `DEVELOPMENT_GUIDE.md` §4 Studio 定位。
- `docs/openapi.yaml` 标签 `Studio`。
- 14 周计划 Week 10、Week 12（权限测试）。

## 结构

- `application/query` 只读投影，复用 Creator/Catalog/Ordering 端口，禁止新增交易规则。
- `interfaces/rest` 与 `interfaces/dto` 负责 HTTP 适配与对象级权限。
- 不需要 `domain` 聚合；如确需聚合，必须升级为限界上下文并补充 ADR。

## 约束

- 接口 `/studio/*` 只允许 `CREATOR` 角色访问，非创作者返回 403。
- 创作者只能操作自己的资源（作品、订单），逐 Controller 校验 owner。
- 状态变更遵循底层上下文的状态机；Studio 不持有写规则。
- 仪表盘数字来自只读投影，不引入物化视图或读写分离。

## 常用命令

```bash
cd apps/api
./gradlew test --tests 'com.creatorhub.studio.*'
```

## 交叉引用

- Creator / Catalog / Ordering：输入端口。
- Notification：使用同一通知事件（Studio 不另起通道）。
