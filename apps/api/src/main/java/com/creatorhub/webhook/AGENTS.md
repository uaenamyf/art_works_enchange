# webhook 顶层包

> 跨上下文 Webhook 验签与基础设施。`payment/interfaces/webhook` 之外的通用部分。

## 进入条件

- 调整 Webhook 验签基类、原始请求体读取工具。
- 引入新的第三方 Webhook（不在当前 MVP 范围）。

## 必读

- 根 `AGENTS.md` §7。
- 根 `DEVELOPMENT_GUIDE.md` §8 Webhook 验签。
- 14 周计划 Week 8 支付 Webhook 验收。

## 约束

- 验签必须使用原始请求体；不预先 JSON 解析。
- 不记录 secret；密钥从环境变量或 Secret 注入。
- 任何 Webhook 路径默认安全配置：Origin/Referer 校验、限流、幂等。
- 不在 Webhook 内部直接修改其他上下文的数据库；只能发布集成事件。

## 常用命令

```bash
cd apps/api
mvn -Dtest='com.creatorhub.webhook.**.*Test' test
```

## 交叉引用

- `payment/interfaces/webhook` 复用 `webhook/security` 通用基类。
- `outbox/AGENTS.md` 投递契约。
