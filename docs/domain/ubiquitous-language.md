# CreatorHub 统一语言

本词表约束产品、API、代码、测试和数据库中的业务术语。新增或改变术语必须明确所属限界上下文；同一词在不同上下文含义不同时，使用上下文限定名称。

| 中文术语 | 代码术语 | 所属上下文 | 定义 | 禁止混用 |
| --- | --- | --- | --- | --- |
| 用户账号 | User | Identity | 可认证、授权和维护基础资料的账号 | Creator、Buyer |
| 创作者 | Creator | Creator | 已建立公开创作者档案并可发布作品的身份 | User、Seller |
| 买家 | Buyer | Ordering | 在某张订单中发起购买的用户快照 | User 实体 |
| 卖家 | SellerSnapshot | Ordering | 订单创建时保存的创作者交易快照 | Creator 聚合 |
| 作品 | Artwork | Catalog | 由创作者发布、展示并可交易的创作对象 | Product、SKU |
| 可售库存 | Stock | Catalog | 当前允许被订单预占的作品数量 | 实物盘点库存 |
| 库存预占 | StockReservation | Catalog/Ordering | 待支付订单在有效期内对可售库存的临时占用 | 已售出 |
| 收藏 | FavoriteCollection | Collection | 用户保存作品以便再次访问的关系 | 关注、购买 |
| 关注 | Follow | Creator | 用户订阅创作者动态的关系 | 收藏 |
| 订单 | Order | Ordering | 买家对单一创作者作品形成的不可变价格交易快照 | Payment |
| 待支付 | PENDING_PAYMENT | Ordering | 已预占库存但尚未由支付 Webhook 确认成功 | 未创建支付 |
| 支付 | Payment | Payment | 外部支付提供商一次支付意图及其状态记录 | Order |
| 履约 | Fulfillment | Ordering | 创作者接单、制作、发货和完成订单的过程 | Payment |
| 领域事件 | DomainEvent | 单一上下文内部 | 聚合完成业务行为后产生的过去式事实 | 集成事件 |
| 集成事件 | IntegrationEvent | 跨上下文 | 从领域事件转换出的稳定、版本化协作契约 | PersistenceRow/DO |
| 产品类型 | ProductType | Catalog | 作品的交易形态，`PHYSICAL` 或 `DIGITAL`，决定订单履约规则 | ArtworkCategory |
| 数字资产 | DigitalAsset | Catalog | 私有对象存储中的可交付文件及其不可变版本 | 公开媒体 |
| 购买授权 | Entitlement | Digital Delivery | 支付确认后买家获得的数字资产访问权 | 下载 URL、下载次数 |
| 下载凭证 | DownloadGrant | Digital Delivery | 为一次下载申请签发的短期、可撤销、可审计凭证 | 永久访问权 |
| 实体履约 | PhysicalFulfillment | Ordering | 地址、库存、制作、发货和完成组成的履约过程 | 数字交付 |
| 数字交付 | DigitalFulfillment | Ordering/Digital Delivery | 支付确认后创建授权并提供受控下载的履约过程 | 实体发货 |

## 命名规则

- 聚合行为使用业务动词，如 `publish`、`reserveStock`、`markPaid`，避免 `setStatus`。
- 事件使用过去式，如 `ArtworkPublished`、`PaymentSucceeded`。
- API 与代码使用 `Artwork`，不引入 `Product`；订单上下文需要引用作品时使用 `ArtworkSnapshot`。
- `PHYSICAL` 与 `DIGITAL` 是产品类型，不使用 `shippingRequired` 这类由客户端推导的布尔字段；地址是否允许由服务端根据产品类型决定。
- `DigitalAsset`、`Entitlement` 和 `DownloadGrant` 分别表示资产、长期授权和短期凭证，不得使用 `downloadUrl` 统称三者。
- 数据库表可使用技术复数命名，但字段含义必须与本词表一致。