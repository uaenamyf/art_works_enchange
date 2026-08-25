/**
 * 应用元信息常量。后续 OpenAPI 客户端生成后，路径与 base URL 也可集中在此。
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
  authRegister: "/auth/register"
} as const;
