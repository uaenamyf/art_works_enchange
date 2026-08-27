"use client";

import { UserRoundPlus } from "lucide-react";
import { useState } from "react";
import { useAuth } from "@/features/auth/AuthProvider";
import { ApiError } from "@/lib/api/fetcher";
import { Button } from "@/components/ui/Button";

/** Creator 关注按钮：先更新本地状态，失败时回滚，关注数以服务端返回为准。 */
export function FollowButton({ creatorId, initialFollowing = false }: { creatorId: string; initialFollowing?: boolean }) {
  const { session, authenticatedFetch } = useAuth();
  const [following, setFollowing] = useState(initialFollowing);
  const [followerCount, setFollowerCount] = useState<number | null>(null);
  const [pending, setPending] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function toggle() {
    if (!session) {
      setError("请先登录后关注创作者");
      return;
    }
    if (pending) return;
    const previous = following;
    setFollowing(!previous);
    setPending(true);
    setError(null);
    try {
      if (previous) {
        await authenticatedFetch<void>(`/api/v1/creators/${creatorId}/follow`, { method: "DELETE" });
        setFollowing(false);
      } else {
        const state = await authenticatedFetch<{ following: boolean; followerCount: number }>(
          `/api/v1/creators/${creatorId}/follow`,
          { method: "PUT" }
        );
        setFollowing(state.following);
        setFollowerCount(state.followerCount);
      }
    } catch (requestError) {
      setFollowing(previous);
      setError(requestError instanceof ApiError ? requestError.problem.detail ?? "关注失败，请稍后重试" : "关注失败，请稍后重试");
    } finally {
      setPending(false);
    }
  }

  return (
    <div className="flex min-w-0 flex-col items-end gap-1">
      <Button
        type="button"
        variant={following ? "secondary" : "primary"}
        loading={pending}
        aria-pressed={following}
        aria-label={following ? "取消关注创作者" : "关注创作者"}
        leftIcon={<UserRoundPlus className="size-4" />}
        onClick={() => void toggle()}
      >
        {following ? "已关注" : "关注"}
      </Button>
      {followerCount !== null ? <span className="text-xs text-ink-500">{followerCount.toLocaleString()} 位关注者</span> : null}
      {error ? <span role="alert" className="text-xs text-error">{error}</span> : null}
    </div>
  );
}