下面给出 **CreatorHub 完整 Figma Design System 设计规范**，按照真实商业产品设计团队的方式组织。

目标：

* 支撑 Web + iOS + Android
* 支撑消费者端 + Creator Studio
* 保持“艺术品交易平台”的高级感
* 可直接转成 Figma Variables / Components / Styles

设计理念：

> **Apple Store 的高级感 + Airbnb 的信任感 + Pinterest 的探索感 + Etsy 的创作者温度**

---

# CreatorHub Design System v1.0

---

# 1. Design Principles（设计原则）

## Principle 01 — Creator First

创作者比商品更重要。

设计优先级：

```
Creator
   ↓
Story
   ↓
Artwork
   ↓
Transaction
```

---

## Principle 02 — Premium but Human

避免：

❌ 电商廉价感

❌ 大量促销标签

强调：

✅ 留白

✅ 大图

✅ 故事

✅ 收藏感

---

## Principle 03 — Visual Storytelling

作品图片是第一视觉。

规则：

图片比例：

```
Artwork Card

4:5


Creator Cover

16:9


Avatar

1:1

```

---

# 2. Color System（颜色系统）

采用：

## Neutral + Warm Accent

---

# Primary Colors

## Black

用途：

按钮

标题

导航

```
Black/900

#111111

```

---

## White

```
White/0

#FFFFFF

```

---

# Gray Scale

| Token    | HEX     | 用途   |
| -------- | ------- | ---- |
| Gray-900 | #1A1A1A | 标题   |
| Gray-700 | #4A4A4A | 正文   |
| Gray-500 | #777777 | 辅助文字 |
| Gray-300 | #D8D8D8 | 边框   |
| Gray-100 | #F5F5F3 | 背景   |
| Gray-50  | #FAFAF8 | 页面背景 |

---

# Brand Accent

Creator Orange

用于：

收藏

Drop

Highlight

```
Brand/500

#C97B4B

```

---

# Secondary Colors

## Success

订单状态：

```
#2E7D5B

```

---

## Error

错误：

```
#D64545

```

---

## Warning

提醒：

```
#D89B32

```

---

# Color Token 结构

Figma Variables:

```
Color

├── Brand

│    ├── Primary

│    └── Accent


├── Neutral

│    ├── Background

│    ├── Text

│    └── Border


├── Semantic

     ├── Success

     ├── Error

     └── Warning

```

---

# 3. Typography System（字体系统）

设计目标：

艺术感 + 可读性。

---

# Font Family

## English

推荐：

```
Inter

```

原因：

* UI友好
* 数字清晰

---

## 中文

推荐：

```
PingFang SC

Noto Sans SC

```

---

# Display Typography

用于：

首页 Hero

作品标题

## Display XL

```
64px

Line Height 72

Weight 600

```

Example:

```
Discover

Independent

Creators

```

---

## Display L

```
48px

56px

600

```

---

# Heading

## H1

```
36px

44px

600

```

---

## H2

```
28px

36px

600

```

---

## H3

```
20px

28px

600

```

---

# Body

## Body Large

```
18px

28px

400

```

---

## Body

```
16px

24px

400

```

---

## Caption

```
13px

18px

400

```

---

# Price Typography

商品价格：

```
24px

Bold

```

---

# 4. Spacing System

采用 8pt Grid。

Figma:

```
Spacing

4

8

12

16

24

32

40

48

64

80

96

```

---

常用：

| 场景             | Spacing |
| -------------- | ------- |
| Button Padding | 16      |
| Card Padding   | 20      |
| Section Gap    | 64      |
| Page Margin    | 80      |

---

# 5. Radius System

整体：

圆润但高级。

Token:

```
Radius


Small

8px


Medium

16px


Large

24px


XL

32px


Pill

999px

```

---

应用：

Button:

16px

Card:

24px

Avatar:

100%

---

## 5.1 ⚠️ CreatorHub 实际 token 偏离

> 本节与 `AGENTS.md §5 UI 规则` 冲突，按仓库规范以 AGENTS.md 为准。
> 设计稿的 16/24/32px 圆角方案在 CreatorHub 中**不适用**。

实际值：

- 卡片圆角：上限 **8px**（AGENTS.md §5 规定，不嵌套卡片）
- 按钮圆角：同 8px
- Pill（圆形徽标）：按 999px 用 `rounded-full`（不冲突）

> 调整视觉时请保留卡片"有角"的高级感，避免使用大圆角导致像营销卡片。
> 改设计后请同步更新 `apps/web/tailwind.config.ts` 的 `borderRadius.token` 与 `apps/web/src/styles/globals.css` 的 `--radius-token`。

---

# 6. Shadow System

避免强阴影。

---

## Shadow Small

```
0

2px

8px

rgba(0,0,0,0.08)

```

---

## Shadow Medium

```
0

8px

24px

rgba(0,0,0,0.12)

```

---

# 7. Button Component

Figma Component:

```
Button

Variants:

Primary

Secondary

Ghost

Danger

```

---

# Primary Button

用途：

购买

关注

尺寸：

### Large

```
Height:

56px


Padding:

24px


Radius:

16px

```

样式：

```
Background:

#111111


Text:

White

```

Example:

```
Buy Artwork

```

---

# Secondary Button

```
Background:

Transparent


Border:

1px solid #DDD


Text:

Black

```

---

# Ghost Button

用于：

收藏

分享

---

# Button States

必须设计：

```
Default

Hover

Pressed

Disabled

Loading

```

---

# 8. Input Component

用途：

搜索

登录

创作编辑

---

## Search Input

高度：

52px

结构：

```
[ 🔍 ]

Search creators, artworks...


```

---

Variants:

```
Input


├── Default

├── Focus

├── Error

└── Disabled

```

---

# 9. Card Component

核心组件。

---

# Artwork Card

Figma:

```
Card/Artwork

```

结构：

```
----------------

Image


Title


Creator


Price


Like


----------------

```

---

尺寸：

Desktop:

320px

Mobile:

160px

---

Variants:

```
Artwork Card


├── Default

├── Hover

├── Sold

├── Drop

└── Featured

```

---

# Featured Card

首页推荐：

大图：

16:9

Overlay：

Creator info

---

# 10. Creator Card

类似：

Instagram Profile。

结构：

```
Avatar


Name


Category


Followers


Follow Button

```

---

组件：

```
CreatorCard

Small

Medium

Large

```

---

# 11. Avatar Component

尺寸：

```
24

32

48

72

120

```

---

支持：

状态：

```
Online

Verified

Creator

```

---

# 12. Tag Component

用于：

分类。

例如：

```
Ceramic

Photography

Limited

New

Drop

```

样式：

背景：

浅色

Radius:

999

---

Variants:

```
Tag

├── Category

├── Status

└── Limited

```

---

# 13. Navigation Component

---

## Desktop Header

高度：

80px

结构：

```
Logo


Search


Discover

Creators

Drops


Cart


Avatar

```

---

## Mobile Bottom Nav

高度：

72px

五个入口：

```
Home

Explore

Create

Inbox

Profile

```

---

# 14. Creator Profile Page Design

Layout:

```
Cover Image


        Avatar


Creator Name

Bio

Follow


Tabs


Works

Drops

Stories

About


Gallery


```

---

Tabs Component:

```
Tab

Active

Inactive

Hover

```

---

# 15. Gallery Component

作品展示核心。

布局：

Masonry

类似：

Pinterest。

Desktop:

4 columns

Tablet:

3

Mobile:

2

---

# 16. Drop Component

特殊组件。

用于限量销售。

结构：

```
Summer Collection


20 Pieces


Starts

Friday 8PM


Notify Me


```

---

状态：

```
Upcoming

Live

Sold Out

Ended

```

---

# 17. Checkout Components

---

## Order Summary Card

结构：

```
Artwork


Creator


Price


Shipping


Total


```

---

## Payment Selector

支持：

```
○ Card


○ PayPal


○ Apple Pay

```

---

# 18. Toast / Notification

类型：

```
Success

Error

Info

Warning

```

例如：

```
✓ Added to collection

```

---

# 19. Modal Component

用于：

* 登录
* 收藏
* 分享
* 删除

尺寸：

```
Small

400px


Medium

600px


Large

900px

```

---

# 20. Loading System

不要使用传统 spinner。

采用：

Skeleton。

例如：

Artwork Loading:

```
[ gray box ]

[ text line ]

[ text line ]

```

---

# 21. Icon System

推荐：

Lucide Icons

风格：

2px stroke。

主要：

```
Heart

Search

Cart

Share

Upload

Camera

User

Bell

Star

```

---

# 22. Figma 文件结构设计

真实项目建议：

```
CreatorHub Design System


├── 00 Foundations

│

├── Colors

├── Typography

├── Spacing

├── Shadows


├── 01 Components


├── Buttons

├── Inputs

├── Cards

├── Navigation

├── Modal

├── Toast


├── 02 Patterns


├── Marketplace

├── Creator Profile

├── Checkout


├── 03 Pages


├── Home

├── Artwork Detail

├── Creator Studio


```

---

# 23. Auto Layout 规范

所有组件必须：

Auto Layout

规则：

Horizontal：

```
gap 16

padding 16

```

Vertical：

```
gap 24

padding 24

```

---

# 24. Responsive Breakpoints

Desktop:

```
1440+

```

Laptop:

```
1024

```

Tablet:

```
768

```

Mobile:

```
375

```

---

# 25. 首页完整 Layout

Desktop:

```
------------------------------------------------


Header


------------------------------------------------


Hero


Discover Independent Creators


[Large Image]


------------------------------------------------


Categories


Art Photography Handmade Design


------------------------------------------------


Featured Creators


Card Card Card Card


------------------------------------------------


New Drops


Card Card Card


------------------------------------------------


Trending Works


Masonry


------------------------------------------------


Footer

```

---

# 26. Creator Studio Design

风格：

偏 Shopify。

Sidebar:

```
Dashboard

Works

Drops

Orders

Analytics

Settings

```

Main:

```
Revenue Chart


Recent Orders


Top Works


Follower Growth

```

---

# 27. Design System 最终组件数量

预计：

| 类别                |  数量 |
| ----------------- | --: |
| 基础 Token          | 50+ |
| Button            |  15 |
| Card              |  20 |
| Navigation        |   8 |
| Form              |  20 |
| Modal             |   8 |
| Page Pattern      |  10 |
| Mobile Components |  30 |

总计：

约：

150 个 Figma Components。

---

# 28. 下一步建议

如果继续推进，我建议下一步做：

## ① Figma 页面级 Prototype

包含：

* Home 页面
* Artwork Detail
* Creator Profile
* Checkout
* Creator Studio Dashboard

（带交互流程：点击 → 购买 → 支付 → 关注 → Drop）

## ② 前端工程映射

把 Figma Component 转成：

```
React Component Tree

Button.tsx

ArtworkCard.tsx

CreatorCard.tsx

DropCard.tsx

Checkout.tsx

```

这样设计和开发可以直接衔接。
