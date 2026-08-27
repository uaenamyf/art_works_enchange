"use client";

import { Heart } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";

/** 作品收藏按钮：先更新本地状态，再由失败回滚，避免网络延迟阻塞浏览体验。 */
export function FavoriteButton({ artworkId, initialFavorited = false }: { artworkId: string; initialFavorited?: boolean }) {
  const { session, authenticatedFetch } = useAuth();
  const [favorited, setFavorited] = useState(initialFavorited);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function toggle() {
    if (!session || pending) {
      setError("请先登录后收藏作品");
      return;
    }
    const previous = favorited;
    setFavorited(!previous);
    setPending(true);
    setError(null);
    try {
      if (previous) {
        await authenticatedFetch<void>(`/api/v1/favorites/${artworkId}`, { method: "DELETE" });
      } else {
        await authenticatedFetch(`/api/v1/favorites/${artworkId}`, { method: "PUT" });
      }
    } catch (requestError) {
      setFavorited(previous);
      setError(requestError instanceof ApiError ? requestError.problem.detail ?? "收藏失败，请稍后重试" : "收藏失败，请稍后重试");
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="flex min-w-0 flex-col gap-1">
      <Button
        type="button"
        variant="secondary"
        disabled={pending}
        aria-pressed={favorited}
        aria-label={favorited ? "取消收藏" : "收藏作品"}
        leftIcon={<Heart className="size-4" fill={favorited ? "currentColor" : "none"} />}
        onClick={() => void toggle()}
      >
        {favorited ? "已收藏" : "收藏"}
      </Button>
      {error ? <span role="alert" className="text-xs text-error">{error}</span> : null}
    </div>
  );
}