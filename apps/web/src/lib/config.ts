/**
 * 应用元信息常量。后续 OpenAPI 客户端生成后，路径与 base URL 也可集中在此。
 *
 * <p><b>NEXT_PUBLIC_* 安全说明</b>：以 {@code NEXT_PUBLIC_} 开头的环境变量会被
 * Next.js 在 build 时内联到浏览器 bundle，**任何用户都能读到**。绝对不能把
 * API key、JWT secret、数据库 URL 含密码等机密写入 {@code NEXT_PUBLIC_*}。
 * 真正的后端 secret 必须走服务端运行时环境变量（无 {@code NEXT_PUBLIC_} 前缀），
 * 由 Next.js Route Handler / Server Action 转发到后端。
 */
export const APP_NAME = "CreatorHub";
export const APP_DESCRIPTION =
  "独立创作者作品交易社区 — 发现独特的作品，认识背后的创作者。";

export const ROUTES = {
  home: "/",
  discover: "/",
  artworkDetail: (id: string) => `/artworks/${id}`,
  creatorProfile: (id: string) => `/creators/${id}`,
  authLogin: "/auth/login",
  authRegister: "/auth/register",
  creatorApply: "/studio/creator/apply",
  favorites: "/me/favorites",
  follows: "/me/follows",
  orders: "/orders",
  orderDetail: (id: string) => `/orders/${id}`,
  notifications: "/notifications"
} as const;
