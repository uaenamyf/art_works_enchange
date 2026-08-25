/**
 * 静态 mock 数据。
 *
 * 当前 Week 1-2 阶段：所有页面以 mock 数据渲染；
 * OpenAPI 客户端 + TanStack Query 会在 Week 2 末接入。
 */

import type { ArtworkCardData, CreatorCardData } from "@/components/domain/types";

export const mockCreators: CreatorCardData[] = [
  {
    id: "c-mia",
    name: "Mia Chen",
    handle: "@miastudio",
    avatarUrl: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=320&q=80",
    coverUrl: "https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=1600&q=80",
    bio: "陶艺与釉色实验。把山野的颜色烧进杯子里。",
    location: "杭州",
    followers: 1284,
    publishedArtworks: 26
  },
  {
    id: "c-john",
    name: "John Park",
    handle: "@parkframes",
    avatarUrl: "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=320&q=80",
    coverUrl: "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=1600&q=80",
    bio: "城市胶片与建筑几何。",
    location: "上海",
    followers: 5320,
    publishedArtworks: 84
  },
  {
    id: "c-luna",
    name: "Luna Ito",
    handle: "@lunaito",
    avatarUrl: "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?w=320&q=80",
    coverUrl: "https://images.unsplash.com/photo-1490604001847-b712b0c2f967?w=1600&q=80",
    bio: "织物与植物染。",
    location: "京都",
    followers: 921,
    publishedArtworks: 18
  }
];

export const mockArtworks: ArtworkCardData[] = [
  {
    id: "a-001",
    title: "山雾 · 釉变杯",
    creatorId: "c-mia",
    creatorName: "Mia Chen",
    priceCents: 18800,
    currency: "CNY",
    imageUrl: "https://images.unsplash.com/photo-1493106819501-66d381c466f1?w=800&q=80",
    aspect: "4/5",
    publishedAt: "2026-08-10T10:00:00Z",
    isFavorite: false,
    isForSale: true
  },
  {
    id: "a-002",
    title: "夜行 No.7",
    creatorId: "c-john",
    creatorName: "John Park",
    priceCents: 120000,
    currency: "CNY",
    imageUrl: "https://images.unsplash.com/photo-1505691938895-1758d7feb511?w=800&q=80",
    aspect: "4/5",
    publishedAt: "2026-08-04T08:30:00Z",
    isFavorite: true,
    isForSale: true
  },
  {
    id: "a-003",
    title: "草染色织壁挂",
    creatorId: "c-luna",
    creatorName: "Luna Ito",
    priceCents: 45000,
    currency: "CNY",
    imageUrl: "https://images.unsplash.com/photo-1490604001847-b712b0c2f967?w=800&q=80",
    aspect: "4/5",
    publishedAt: "2026-07-28T14:00:00Z",
    isFavorite: false,
    isForSale: true
  },
  {
    id: "a-004",
    title: "青灰落釉壶",
    creatorId: "c-mia",
    creatorName: "Mia Chen",
    priceCents: 36000,
    currency: "CNY",
    imageUrl: "https://images.unsplash.com/photo-1530018607912-eff2daa1bac4?w=800&q=80",
    aspect: "4/5",
    publishedAt: "2026-08-18T09:00:00Z",
    isFavorite: false,
    isForSale: true
  },
  {
    id: "a-005",
    title: "胶片 · 京都早春",
    creatorId: "c-luna",
    creatorName: "Luna Ito",
    priceCents: 68000,
    currency: "CNY",
    imageUrl: "https://images.unsplash.com/photo-1469474968028-56623f02e42e?w=800&q=80",
    aspect: "4/5",
    publishedAt: "2026-08-20T07:30:00Z",
    isFavorite: false,
    isForSale: true
  },
  {
    id: "a-006",
    title: "城市几何 01",
    creatorId: "c-john",
    creatorName: "John Park",
    priceCents: 88000,
    currency: "CNY",
    imageUrl: "https://images.unsplash.com/photo-1486325212027-8081e485255e?w=800&q=80",
    aspect: "4/5",
    publishedAt: "2026-08-22T11:00:00Z",
    isFavorite: false,
    isForSale: true
  }
];

export function findCreator(id: string): CreatorCardData | undefined {
  return mockCreators.find((c) => c.id === id);
}

export function findArtwork(id: string): ArtworkCardData | undefined {
  return mockArtworks.find((a) => a.id === id);
}

export function findArtworksByCreator(creatorId: string): ArtworkCardData[] {
  return mockArtworks.filter((a) => a.creatorId === creatorId);
}
