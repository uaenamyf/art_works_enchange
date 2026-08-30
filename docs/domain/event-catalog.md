# CreatorHub 事件目录

本目录记录 MVP 的领域事件和跨上下文集成事件。事件表示已经发生的事实，不是命令；业务数据与 Outbox 在同一事务提交，跨上下文按至少一次语义投递。

## 公共信封

| 字段 | 说明 |
| --- | --- |
| `eventId` | UUID；消费者幂等键的一部分 |
| `eventType` | 稳定事件名称 |
| `eventVersion` | 从 1 开始的正整数 |
| `aggregateType` / `aggregateId` | 事件来源聚合 |
| `aggregateVersion` | 产生事件后的聚合版本 |
| `occurredAt` | UTC ISO 8601 时间 |
| `traceId` | 与请求链路关联的 UUID |
| `payload` | 最小必要业务字段，不含密码、Token、完整地址或支付机密 |

## MVP 事件

| 事件 | 类型 | 发布上下文 | 消费上下文 | 关键载荷 | 幂等效果 |
| --- | --- | --- | --- | --- | --- |
| `CreatorFollowed` v1 | 集成事件 | Creator | Notification | `userId`、`creatorId` | 同一关注只生成一次通知 |
| `ArtworkPublished` v1 | 集成事件 | Catalog | Notification | `artworkId`、`creatorId`、`title` | 同一作品版本只通知一次 |
| `StockReserved` v1 | 领域事件 | Catalog | Catalog 内部 | `artworkId`、`orderId`、`quantity` | 重复订单预占不再次扣减 |
| `StockReleased` v1 | 集成事件 | Ordering | Catalog | `orderId`、`artworkId`、`quantity` | 已返还订单不重复增加库存 |
| `OrderCreated` v1 | 集成事件 | Ordering | Payment、Notification | `orderId`、`buyerId`、`total`、`currency` | 同一订单只初始化一次下游状态 |
| `PaymentSucceeded` v1 | 集成事件 | Payment | Ordering、Notification | `paymentId`、`orderId`、`amount`、`currency` | 已支付订单保持 `PAID`，不重复迁移 |
| `PaymentFailed` v1 | 集成事件 | Payment | Ordering | `paymentId`、`orderId`、`reasonCode` | 按 `eventId + orderId` 持久化消费记录；不回退已支付订单 |
| `OrderStatusChanged` v1 | 集成事件 | Ordering | Notification | `orderId`、`fromStatus`、`toStatus` | 每个订单版本只通知一次 |
| `DigitalEntitlementRequested` v1 | 集成事件 | Ordering | Digital Delivery | `orderId`、`artworkId`、`assetVersionId`、`buyerId` | 同一订单只创建一次购买授权 |
| `DigitalEntitlementGranted` v1 | 集成事件 | Digital Delivery | Ordering、Notification | `orderId`、`assetVersionId`、`entitlementId` | 已授权订单不重复创建下载权限 |
| `DigitalAssetRecalled` v1 | 集成事件 | Catalog/Digital Delivery | Ordering、Notification | `artworkId`、`assetVersionId`、`reasonCode` | 撤回消费者按资产版本撤销授权并失效下载凭证；重复/未知版本无副作用 |

## 演进规则

- 新增可选字段可保持当前版本；删除字段、改变语义或类型必须发布新版本。
- 消费者至少兼容当前版本和前一版本，未知字段必须忽略。
- 不直接将领域对象、PersistenceRow/DO 或第三方 Webhook 载荷作为事件载荷。
- 新增消费者时必须记录幂等键、失败重试和补偿行为。
- 数字交付事件不得包含对象存储 key、签名 URL、下载 Token、买家地址或其他个人信息；下载地址必须在授权校验后临时生成。