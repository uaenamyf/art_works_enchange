# sharedkernel

> 共享内核：只放多个限界上下文稳定共享的领域概念。**禁止**借机堆入通用工具类。

## 进入条件

- 新增 `Money`、强类型 ID、`DomainEvent` 基类等真正被多个上下文共享的概念。
- 调整 Problem Details 公共错误模型。

## 必读

- 根 `AGENTS.md` §3。
- 根 `DEVELOPMENT_GUIDE.md` §4.1 sharedkernel 规则。
- `docs/domain/ubiquitous-language.md`。

## 约束

- 不放分页、Web 错误、Spring 配置或工具类。
- 所有类型不可变；`Money` 必须携带 ISO 4217 币种。
- 不得依赖 Spring/JPA/Stripe/S3/Redis。

## 交叉引用

- 任何上下文需要 ID 或 `Money` 时从 `sharedkernel.domain` 引入。
- 测试用值对象在 `apps/api/src/test/.../sharedkernel` 下，不进入主源码。
