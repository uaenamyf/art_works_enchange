import { clsx } from "clsx";

/**
 * 业务组件的视图层类型。
 * 当前 Week 1-2 用 mock 数据；后续 OpenAPI 客户端生成时，
 * 保持这些类型作为领域 UI 契约，避免组件直接依赖 fetch 响应结构。
 */
export interface ArtworkCardData {
  id: string;
  title: string;
  category?: string;
  creatorId: string;
  creatorName: string;
  priceCents: number;
  currency: string;
  imageUrl: string;
  /** CSS aspect-ratio 值，例如 "4/5"。 */
  aspect: "4/5" | "16/9" | "1/1";
  publishedAt: string;
  isFavorite: boolean;
  isForSale: boolean;
}

export interface CreatorCardData {
  id: string;
  name: string;
  handle: string;
  avatarUrl: string | null;
  coverUrl: string | null;
  bio: string;
  location: string;
  followers: number;
  publishedArtworks: number;
}

export type AspectRatio = ArtworkCardData["aspect"];

export const cn = clsx;
