"use client";

import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";

export type Locale = "zh-CN" | "en-US";

type MessageKey = keyof typeof messages["zh-CN"];

const messages = {
  "zh-CN": {
    discover: "发现",
    exploreArtworks: "探索作品",
    allArtworks: "全部作品",
    browseByType: "按产品类型浏览",
    creators: "创作者",
    creatorStudio: "Creator Studio",
    becomeCreator: "成为 Creator",
    enterStudio: "进入 Studio",
    account: "账户",
    personalSpace: "我的空间",
    orders: "订单",
    favorites: "收藏",
    follows: "关注",
    notifications: "通知",
    management: "管理",
    admin: "管理后台",
    logout: "注销",
    overview: "概览",
    artworkManagement: "作品管理",
    listArtwork: "上架作品",
    fulfillment: "订单履约",
    paymentMethods: "收款方式",
    login: "登录",
    register: "注册",
    loginToStudio: "登录后即可进入 Creator Studio 工作台。",
    language: "语言",
    chinese: "中文",
    english: "English",
    myOrders: "我的订单",
    order: "订单",
    createdAt: "创建时间",
    logistics: "物流信息",
    carrier: "承运商",
    trackingNumber: "物流单号",
    deliveryStatus: "配送状态",
    awaitingShipment: "待发货",
    shippedAwaitingDelivery: "已发货，等待配送",
    creatorWillAddTracking: "创作者发货后会在这里显示承运商和物流单号。",
    backToOrders: "返回订单",
    orderSummary: "订单摘要",
    quantity: "数量",
    total: "总计"
    ,checkout: "结算"
    ,confirmOrder: "确认订单"
    ,backToArtwork: "返回作品"
    ,digitalDelivery: "数字交付"
    ,accessAfterPayment: "支付确认后即可访问"
    ,noShippingAddress: "无需填写收货地址。订单支付确认后，授权文件会出现在订单详情中。"
    ,shippingAddress: "收货信息"
    ,buyerQuantity: "购买数量"
    ,recipient: "收货人"
    ,address: "地址"
    ,city: "城市"
    ,postalCode: "邮编"
    ,countryCode: "国家代码"
    ,phone: "手机号"
    ,orderCreated: "订单摘要"
    ,digitalOrderHint: "支付确认后创建购买授权。"
    ,physicalOrderHint: "库存将在订单创建事务中确认，订单状态为待支付。"
    ,createDigitalOrder: "创建数字订单"
    ,createPendingOrder: "创建待支付订单"
    ,backToStudio: "返回 Studio"
    ,studioOverview: "工作台概览"
    ,studioOverviewHint: "查看作品、订单和创作者社区的最新状态。"
    ,revenue: "累计收入"
    ,orderCount: "订单数"
    ,publishedArtworkCount: "已发布作品"
    ,followerCount: "关注者"
    ,digitalDownloadCount: "数字下载"
    ,manageArtworks: "管理作品"
    ,viewOrders: "查看订单"
    ,loadingAccount: "正在加载账户..."
    ,loadingOverview: "正在加载概览..."
    ,overviewLoadFailed: "概览加载失败"
    ,artworkManagementHint: "管理草稿、审核和已发布作品。"
    ,all: "全部"
    ,draft: "草稿"
    ,pendingReview: "待审核"
    ,published: "已发布"
    ,archived: "已归档"
    ,addArtwork: "上架新作品"
    ,artworkFlow: "作品上架流程"
    ,createDraft: "创建草稿"
    ,uploadAssets: "上传素材"
    ,submitArtwork: "提交作品"
    ,publishAfterReview: "审核通过后发布或直接上架"
    ,noArtworksInFilter: "当前筛选下暂无作品。"
    ,loadingArtworks: "正在加载作品..."
    ,artworksLoadFailed: "作品加载失败"
    ,fulfillmentTitle: "订单履约"
    ,fulfillmentHint: "处理已支付订单并推进履约状态。"
    ,noOrdersInFilter: "当前筛选下暂无订单。"
    ,loadingOrders: "正在加载订单..."
    ,ordersLoadFailed: "订单加载失败"
    ,acceptOrder: "接单"
    ,markShipped: "标记发货"
    ,carrierPlaceholder: "承运商"
    ,trackingPlaceholder: "物流单号"
    ,loadMore: "加载更多"
    ,saveSubmitArtwork: "提交作品"
    ,uploadFailed: "上传或保存失败，请检查网络后重试"
    ,artworkNotAvailable: "这件作品目前无法购买。"
    ,artworkLoadFailed: "作品加载失败，请返回详情页重试"
    ,orderCreateFailed: "订单创建失败，请稍后重试"
    ,accountLoading: "正在加载账户..."
    ,artworksLoading: "正在加载作品..."
    ,loginRequiredCheckout: "请先登录后再结算。"
    ,loginRequiredOrders: "请先登录后查看订单。"
    ,loginAction: "去登录"
    ,creatorName: "创作者名称"
    ,publicSlug: "公开 slug"
    ,slugHint: "例如 mia-studio"
    ,category: "类别"
    ,bio: "简介"
    ,locationOptional: "地点（可选）"
    ,submitApplication: "提交申请"
    ,loginBeforeCreator: "请先登录，再申请 Creator。"
    ,applicationFailed: "申请失败，请稍后再试"
    ,slugTaken: "该公开地址已被使用"
    ,myNotifications: "我的通知"
    ,unreadNotifications: "条未读通知"
    ,unreadOnly: "只看未读"
    ,noNotifications: "暂无通知。"
    ,view: "查看"
    ,markRead: "标记已读"
    ,loadingNotifications: "正在加载通知..."
    ,moreNotificationsFailed: "更多通知加载失败"
    ,markReadFailed: "标记已读失败"
    ,loadMoreNotifications: "加载更多"
    ,loadingMore: "正在加载..."
    ,restoringSession: "正在恢复登录状态..."
    ,loginCreatorRequired: "请先登录并完成 Creator 入驻。"
    ,artworkStatus: "作品状态"
    ,boundImages: "已绑定 {count} 张图片"
    ,approvedArtworkHint: "作品已通过审核，可以正式上架。"
    ,publishArtwork: "发布作品"
    ,latestReviewReason: "最近一次审核意见：{reason}"
    ,artworkTitle: "作品标题"
    ,artworkDescription: "作品描述"
    ,price: "价格"
    ,currency: "货币"
    ,stock: "库存"
    ,saved: "作品已保存"
    ,publishedNotice: "作品已发布"
    ,saveDraft: "保存草稿"
    ,saveAndSubmit: "保存并提交审核"
    ,artworkLoadError: "作品加载失败"
    ,saveError: "保存失败，请刷新后重试"
    ,saveNetworkError: "保存失败，请检查网络后重试"
    ,publishError: "发布失败，请刷新后重试"
    ,paypalSettings: "收款方式"
    ,paypalSettingsLoading: "正在加载收款设置..."
    ,creatorOnlyPayments: "只有 Creator 可以管理收款方式。"
    ,paypalDescription: "连接 PayPal Multiparty 入驻后分配的商户 ID。平台不会保存 PayPal 密码、Client Secret 或 Access Token。"
    ,paypalStatus: "PayPal 状态"
    ,configured: "已配置"
    ,notConfigured: "未配置"
    ,paypalMerchantHint: "请从 PayPal 商户入驻流程或开发者后台获取 merchant_id，不要填写 PayPal 邮箱。"
    ,paypalSaved: "PayPal 收款方式已保存。"
    ,savePaypal: "保存 PayPal 收款方式"
    ,adminOnly: "只有管理员可以访问此页面。"
    ,operationsOverview: "运营概览"
    ,operationsOverviewHint: "查看平台内容和订单状态，不包含支付结算操作。"
    ,userCount: "用户总数"
    ,creatorCount: "Creator 总数"
    ,pendingArtworkCount: "待审核作品"
    ,artworkStatusTitle: "作品状态"
    ,orderStatusTitle: "订单状态"
    ,reviewQueue: "查看审核队列"
    ,reviewTitle: "作品审核"
    ,reviewHint: "审核 Creator 提交的作品，批准后由 Creator 在 Studio 完成发布。"
    ,emptyReviewQueue: "当前没有待审核作品。"
    ,viewAndReview: "查看并审核"
    ,creatorWorkspace: "创作者工作台"
  },
  "en-US": {
    discover: "Discover",
    exploreArtworks: "Explore artworks",
    allArtworks: "All artworks",
    browseByType: "Browse by type",
    creators: "Creators",
    creatorStudio: "Creator Studio",
    becomeCreator: "Become a Creator",
    enterStudio: "Open Studio",
    account: "Account",
    personalSpace: "My space",
    orders: "Orders",
    favorites: "Favorites",
    follows: "Following",
    notifications: "Notifications",
    management: "Management",
    admin: "Admin",
    logout: "Log out",
    overview: "Overview",
    artworkManagement: "Artwork management",
    listArtwork: "List artwork",
    fulfillment: "Order fulfillment",
    paymentMethods: "Payout methods",
    login: "Log in",
    register: "Sign up",
    loginToStudio: "Log in to enter the Creator Studio workspace.",
    language: "Language",
    chinese: "中文",
    english: "English",
    myOrders: "My orders",
    order: "Order",
    createdAt: "Created",
    logistics: "Shipping",
    carrier: "Carrier",
    trackingNumber: "Tracking number",
    deliveryStatus: "Delivery status",
    awaitingShipment: "Awaiting shipment",
    shippedAwaitingDelivery: "Shipped, awaiting delivery",
    creatorWillAddTracking: "The carrier and tracking number will appear here after the creator ships your order.",
    backToOrders: "Back to orders",
    orderSummary: "Order summary",
    quantity: "Quantity",
    total: "Total"
    ,checkout: "Checkout"
    ,confirmOrder: "Confirm order"
    ,backToArtwork: "Back to artwork"
    ,digitalDelivery: "Digital delivery"
    ,accessAfterPayment: "Access after payment confirmation"
    ,noShippingAddress: "No shipping address is needed. Your download entitlement will appear in the order after payment confirmation."
    ,shippingAddress: "Shipping address"
    ,buyerQuantity: "Quantity"
    ,recipient: "Recipient"
    ,address: "Address"
    ,city: "City"
    ,postalCode: "Postal code"
    ,countryCode: "Country code"
    ,phone: "Phone"
    ,orderCreated: "Order summary"
    ,digitalOrderHint: "A purchase entitlement will be created after payment confirmation."
    ,physicalOrderHint: "Stock is confirmed when the order is created; the order starts awaiting payment."
    ,createDigitalOrder: "Create digital order"
    ,createPendingOrder: "Create order"
    ,backToStudio: "Back to Studio"
    ,studioOverview: "Studio overview"
    ,studioOverviewHint: "View the latest status of your artwork, orders, and creator community."
    ,revenue: "Revenue"
    ,orderCount: "Orders"
    ,publishedArtworkCount: "Published artworks"
    ,followerCount: "Followers"
    ,digitalDownloadCount: "Digital downloads"
    ,manageArtworks: "Manage artworks"
    ,viewOrders: "View orders"
    ,loadingAccount: "Loading account..."
    ,loadingOverview: "Loading overview..."
    ,overviewLoadFailed: "Unable to load overview"
    ,artworkManagementHint: "Manage drafts, reviews, and published artworks."
    ,all: "All"
    ,draft: "Draft"
    ,pendingReview: "In review"
    ,published: "Published"
    ,archived: "Archived"
    ,addArtwork: "List new artwork"
    ,artworkFlow: "Artwork listing flow"
    ,createDraft: "Create draft"
    ,uploadAssets: "Upload assets"
    ,submitArtwork: "Submit artwork"
    ,publishAfterReview: "Publish after review or list directly"
    ,noArtworksInFilter: "No artworks in this filter."
    ,loadingArtworks: "Loading artworks..."
    ,artworksLoadFailed: "Unable to load artworks"
    ,fulfillmentTitle: "Order fulfillment"
    ,fulfillmentHint: "Process paid orders and advance fulfillment status."
    ,noOrdersInFilter: "No orders in this filter."
    ,loadingOrders: "Loading orders..."
    ,ordersLoadFailed: "Unable to load orders"
    ,acceptOrder: "Accept order"
    ,markShipped: "Mark as shipped"
    ,carrierPlaceholder: "Carrier"
    ,trackingPlaceholder: "Tracking number"
    ,loadMore: "Load more"
    ,saveSubmitArtwork: "Submit artwork"
    ,uploadFailed: "Upload or save failed. Check your connection and try again."
    ,artworkNotAvailable: "This artwork is currently unavailable."
    ,artworkLoadFailed: "Unable to load artwork. Return to the artwork page and try again."
    ,orderCreateFailed: "Unable to create order. Please try again."
    ,accountLoading: "Loading account..."
    ,artworksLoading: "Loading artworks..."
    ,loginRequiredCheckout: "Log in before checkout."
    ,loginRequiredOrders: "Log in to view your orders."
    ,loginAction: "Log in"
    ,creatorName: "Creator name"
    ,publicSlug: "Public slug"
    ,slugHint: "For example, mia-studio"
    ,category: "Category"
    ,bio: "Bio"
    ,locationOptional: "Location (optional)"
    ,submitApplication: "Submit application"
    ,loginBeforeCreator: "Log in before applying to become a Creator."
    ,applicationFailed: "Unable to submit application. Please try again."
    ,slugTaken: "That public address is already in use"
    ,myNotifications: "My notifications"
    ,unreadNotifications: "unread notifications"
    ,unreadOnly: "Unread only"
    ,noNotifications: "No notifications."
    ,view: "View"
    ,markRead: "Mark as read"
    ,loadingNotifications: "Loading notifications..."
    ,moreNotificationsFailed: "Unable to load more notifications"
    ,markReadFailed: "Unable to mark as read"
    ,loadMoreNotifications: "Load more"
    ,loadingMore: "Loading..."
    ,restoringSession: "Restoring your session..."
    ,loginCreatorRequired: "Log in and complete Creator onboarding first."
    ,artworkStatus: "Artwork status"
    ,boundImages: "{count} bound images"
    ,approvedArtworkHint: "This artwork passed review and can now be published."
    ,publishArtwork: "Publish artwork"
    ,latestReviewReason: "Latest review note: {reason}"
    ,artworkTitle: "Artwork title"
    ,artworkDescription: "Artwork description"
    ,price: "Price"
    ,currency: "Currency"
    ,stock: "Stock"
    ,saved: "Artwork saved"
    ,publishedNotice: "Artwork published"
    ,saveDraft: "Save draft"
    ,saveAndSubmit: "Save and submit for review"
    ,artworkLoadError: "Unable to load artwork"
    ,saveError: "Unable to save. Refresh and try again."
    ,saveNetworkError: "Unable to save. Check your connection and try again."
    ,publishError: "Unable to publish. Refresh and try again."
    ,paypalSettings: "Payout methods"
    ,paypalSettingsLoading: "Loading payout settings..."
    ,creatorOnlyPayments: "Only Creators can manage payout methods."
    ,paypalDescription: "Connect the merchant ID assigned during PayPal Multiparty onboarding. The platform never stores your PayPal password, Client Secret, or Access Token."
    ,paypalStatus: "PayPal status"
    ,configured: "Configured"
    ,notConfigured: "Not configured"
    ,paypalMerchantHint: "Get the merchant_id from PayPal onboarding or the developer dashboard. Do not enter a PayPal email."
    ,paypalSaved: "PayPal payout method saved."
    ,savePaypal: "Save PayPal payout method"
    ,adminOnly: "Only administrators can access this page."
    ,operationsOverview: "Operations overview"
    ,operationsOverviewHint: "Review platform content and order status. Payment settlement is handled separately."
    ,userCount: "Users"
    ,creatorCount: "Creators"
    ,pendingArtworkCount: "Artworks in review"
    ,artworkStatusTitle: "Artwork status"
    ,orderStatusTitle: "Order status"
    ,reviewQueue: "View review queue"
    ,reviewTitle: "Artwork review"
    ,reviewHint: "Review Creator submissions. After approval, the Creator publishes the artwork from Studio."
    ,emptyReviewQueue: "There are no artworks awaiting review."
    ,viewAndReview: "View and review"
    ,creatorWorkspace: "Creator workspace"
  }
} as const;

interface LocaleContextValue {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  toggleLocale: () => void;
  t: (key: MessageKey) => string;
}

const LocaleContext = createContext<LocaleContextValue | null>(null);

function readLocale(): Locale {
  if (typeof document === "undefined") return "zh-CN";
  return document.cookie.split(";").map((item) => item.trim()).find((item) => item.startsWith("ch_locale="))?.split("=")[1] === "en-US" ? "en-US" : "zh-CN";
}

/** 全局语言状态；只保存白名单 locale 到非敏感 Cookie，不影响认证 Cookie。 */
export function LocaleProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>("zh-CN");

  useEffect(() => {
    setLocaleState(readLocale());
  }, []);

  useEffect(() => {
    document.documentElement.lang = locale;
    document.cookie = `ch_locale=${locale}; Max-Age=31536000; Path=/; SameSite=Lax`;
  }, [locale]);

  const setLocale = (nextLocale: Locale) => {
    setLocaleState(nextLocale);
    document.cookie = `ch_locale=${nextLocale}; Max-Age=31536000; Path=/; SameSite=Lax`;
    window.location.reload();
  };

  const value = useMemo<LocaleContextValue>(() => ({
    locale,
    setLocale,
    toggleLocale: () => setLocale(locale === "zh-CN" ? "en-US" : "zh-CN"),
    t: (key) => messages[locale][key]
  }), [locale]);

  return <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>;
}

export function useLocale(): LocaleContextValue {
  const value = useContext(LocaleContext);
  if (!value) throw new Error("useLocale must be used within LocaleProvider");
  return value;
}
