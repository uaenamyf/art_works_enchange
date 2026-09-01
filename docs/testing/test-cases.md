# CreatorHub 全量测试用例

状态约定：`自动化` 表示仓库已有或本轮可执行自动化；`条件化` 表示需要外部凭据或业务前置数据；`手工` 表示需要浏览器/运营环境配合。

## 1. Identity 与安全

| ID | 操作 | 用例与预期 | 状态 |
| --- | --- | --- | --- |
| ID-01 | `POST /auth/register` | 合法邮箱、强密码注册成功；返回用户和会话，Refresh Token 仅写 HttpOnly Cookie | 自动化 |
| ID-02 | `POST /auth/register` | 重复邮箱、弱密码、空字段、超长字段返回校验/业务错误 | 自动化 |
| ID-03 | `POST /auth/login` | 正确凭据登录；错误邮箱/密码统一返回认证失败 | 自动化 |
| ID-04 | `POST /auth/login` | 禁用账号不能建立会话；登录限流返回 `RATE_LIMITED` 和 `Retry-After` | 自动化 |
| ID-05 | `POST /auth/refresh` | 有效 HttpOnly Refresh Cookie 轮换成功；旧 token 不能重复使用 | 自动化 |
| ID-06 | `POST /auth/refresh` | 缺失、过期、重复使用或伪造 Cookie 返回 401，不泄露原因 | 自动化 |
| ID-07 | `POST /auth/logout` | 注销撤销当前 Refresh Token，重复注销保持幂等 | 自动化 |
| ID-08 | `GET /me` | 有效 Access Token 返回当前用户；无 token 返回 401 | 自动化 |
| ID-09 | 全部认证接口 | Origin/Referer 校验、Cookie 属性、不会把 Access Token 写入 localStorage | 自动化/手工 |

## 2. Creator、Follow 与 Notification

| ID | 操作 | 用例与预期 | 状态 |
| --- | --- | --- | --- |
| CR-01 | `GET /creators` | 匿名列表、分类筛选、游标分页和 limit 边界正确 | 自动化 |
| CR-02 | `GET /creators/{creatorId}` | 匿名详情成功；不存在资源返回明确错误；关注状态匿名为 false | 自动化 |
| CR-03 | `POST /me/creator` | USER 合法申请成功并将身份升级为 CREATOR | 自动化 |
| CR-04 | `POST /me/creator` | 匿名、CREATOR、ADMIN、重复申请、非法 slug/category 被拒绝 | 自动化 |
| CR-05 | `PUT /me/creator` | Creator 更新自己的资料；不能指定其他 owner | 自动化 |
| CR-06 | `PUT/DELETE /creators/{creatorId}/follow` | 关注/取消关注成功、重复操作幂等、不能关注自己 | 自动化 |
| CR-07 | `GET /creators/{creatorId}/followers` | 分页、权限、不存在 Creator 和数据脱敏正确 | 自动化 |
| NT-01 | `GET /notifications` | 登录用户只能看到自己的通知，支持 unreadOnly 和游标 | 自动化 |
| NT-02 | `POST /notifications/{id}/read` | 只能标记自己的通知；重复标记幂等 | 自动化 |

## 3. Catalog、Media 与 Digital Asset

| ID | 操作 | 用例与预期 | 状态 |
| --- | --- | --- | --- |
| CA-01 | `GET /artworks` | 匿名公开查询、分类/productType/query/sort/cursor 组合正确 | 自动化 |
| CA-02 | `POST /artworks` | CREATOR/ADMIN 创建草稿；USER、匿名被拒绝；客户端 owner/价格权限不可信 | 自动化 |
| CA-03 | `PUT /artworks/{id}` | 只能更新自己的可编辑状态；已发布作品和他人作品被拒绝 | 自动化 |
| CA-04 | `POST /artworks/{id}/submit-review` | 至少有合法媒体且价格有效才可提交；重复提交按状态机拒绝 | 自动化 |
| CA-05 | `POST /artworks/{id}/review/approve|reject` | 仅 ADMIN；理由写入审计/通知；Creator 不能自审 | 自动化 |
| CA-06 | `POST /artworks/{id}/publish|archive` | 审核状态流转正确；库存、媒体和发布前置条件有效 | 自动化 |
| CA-07 | `POST /media/upload-requests` / `confirm` | Creator/ADMIN 可申请；大小、Content-Type、签名和归属校验正确 | 自动化/条件化 |
| CA-08 | `GET /media/{mediaId}` | 仅公开已发布媒体可匿名读取；草稿媒体不泄露 | 自动化 |
| CA-09 | `POST /digital-assets/upload-requests` / `confirm` | 数字作品归属、扫描状态和 SHA-256 校验正确 | 自动化 |
| CA-10 | `POST /digital-assets/{versionId}/recall` | 仅 owner/ADMIN 允许；状态和通知幂等 | 自动化 |

## 4. Collection 与 Ordering

| ID | 操作 | 用例与预期 | 状态 |
| --- | --- | --- | --- |
| CO-01 | `PUT/DELETE /favorites/{artworkId}` | 登录用户收藏/取消收藏成功、重复操作幂等、只能作用于已发布作品 | 自动化 |
| CO-02 | `GET /me/favorites` | 只能返回当前用户收藏，支持游标和 limit | 自动化 |
| OR-01 | `POST /orders` | 实物订单校验地址、库存预占、价格快照和幂等键 | 自动化 |
| OR-02 | `POST /orders` | 数字订单不要求收货地址；库存/价格异常时事务回滚 | 自动化 |
| OR-03 | `GET /orders` / `{id}` | 买家只能读取自己的订单；他人订单返回拒绝或不存在 | 自动化 |
| OR-04 | `GET /orders/{id}/tracking` | 物流状态和 tracking 数据按订单归属返回 | 自动化 |
| OR-05 | `POST /orders/{id}/cancel` | 仅允许状态机允许的取消；库存正确释放；重复取消安全 | 自动化 |
| OR-06 | `POST /orders/{id}/payment-*` | 仅订单买家和合法状态可创建支付会话；客户端不能改变金额/状态 | 自动化/条件化 |
| OR-07 | 订单并发 | 同一库存并发下单最多成功可用库存数，不出现负库存 | 自动化 |

## 5. Payment、Webhook 与 Digital Delivery

| ID | 操作 | 用例与预期 | 状态 |
| --- | --- | --- | --- |
| PA-01 | `POST /payments/webhooks/stripe` | 合法 HMAC/签名、时间窗口和事件解析成功 | 自动化/条件化 |
| PA-02 | `POST /payments/webhooks/paypal` | 合法签名成功；错误签名、重放和篡改 payload 被拒绝 | 自动化/条件化 |
| PA-03 | Webhook 重复事件 | 相同 provider event 重复投递只产生一次业务副作用 | 自动化 |
| PA-04 | Payment failure/success | 支付失败、成功、取消和退款按事件驱动订单状态流转 | 自动化/条件化 |
| DD-01 | `GET /orders/{id}/entitlement` | 仅买家、已支付数字订单可读取授权；其他情况拒绝 | 自动化 |
| DD-02 | `POST /orders/{id}/download-grants` | 仅有效授权生成短期下载链接；授权缺失返回 404/稳定错误 | 自动化 |
| DD-03 | `GET /digital-downloads/{grantId}` | grant 归属、过期、撤回和重复使用规则正确 | 自动化 |

## 6. Studio 与权限矩阵

| ID | 操作 | 用例与预期 | 状态 |
| --- | --- | --- | --- |
| ST-01 | `/studio/dashboard` | CREATOR 和 ADMIN 可读；USER/匿名不可读 | 自动化 |
| ST-02 | `/studio/artworks` | CREATOR 和 ADMIN 可查看自己的作品；分页和状态筛选正确 | 自动化 |
| ST-03 | `/studio/artworks/{id}` / reviews | CREATOR 和 ADMIN 可访问合法资源；对象归属仍由应用服务校验 | 自动化 |
| ST-04 | `/studio/orders` | CREATOR 和 ADMIN 可查看履约订单，不能越权操作他人资源 | 自动化 |
| ST-05 | `/studio/orders/{id}/status` | 合法状态推进、tracking 校验、非法跳转和重复请求正确 | 自动化 |
| ST-06 | `/me/creator/paypal-account` | CREATOR 和 ADMIN 可读写 merchant_id；密码/token 不入库、不返回 | 自动化 |
| ST-07 | 前端 `/studio/**` | ADMIN 不再被错误显示为“无 Creator 权限”；普通 USER 仍显示申请入口 | 自动化/手工 |

## 7. 前端与 E2E

| ID | 流程 | 预期 | 状态 |
| --- | --- | --- | --- |
| FE-01 | Discover 浏览 | 首页、分类、详情、Creator 公开页加载成功，语言切换一致 | 自动化 |
| FE-02 | Navbar 菜单 | Discover/Creators/Account/Language 互斥，鼠标移出关闭，键盘焦点可用 | 自动化/手工 |
| FE-03 | 响应式 | 375/768/1440px 无横向溢出和菜单覆盖 | 自动化 |
| FE-04 | 登录/注册 | 加载、校验、错误、成功和 Refresh 恢复态正确 | 自动化 |
| FE-05 | 实体购买 | 作品到订单、地址、支付入口和订单详情状态正确 | 条件化 |
| FE-06 | 数字购买 | 无地址、授权创建和短期下载流程正确 | 条件化 |
| FE-07 | Creator 履约 | 已支付订单从 PAID 到 PROCESSING/SHIPPED/COMPLETED | 条件化 |
| FE-08 | 跨角色发布 | Creator 提交、Admin 审核、Creator 发布完整闭环 | 条件化 |