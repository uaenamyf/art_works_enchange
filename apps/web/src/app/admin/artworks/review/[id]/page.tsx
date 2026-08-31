"use client";

import { useEffect, useState } from "react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useRouter } from "next/navigation";
import { Button } from "@/components/ui/Button";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";
import type { components } from "@/lib/api/schema.gen";

interface PageProps { params: Promise<{ id: string }> }
type ReviewItem = components["schemas"]["AdminReviewQueueItem"];

/** Admin 审核详情；审核理由由管理员明确输入并提交给已有审核状态机。 */
export default function AdminReviewDetailPage({ params }: PageProps) {
  const router = useRouter();
  const { session, loading, authenticatedFetch } = useAuth();
  const [artworkId, setArtworkId] = useState<string | null>(null);
  const [reason, setReason] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  useEffect(() => {
    void params.then(({ id }) => setArtworkId(id));
  }, [params]);
  const review = useQuery<ReviewItem, Error>({
    queryKey: ["admin-review-artwork", artworkId],
    enabled: !loading && session?.user.role === "ADMIN" && Boolean(artworkId),
    queryFn: () => authenticatedFetch<ReviewItem>(`/api/v1/admin/artworks/review-queue/${artworkId}`)
  });
  const mutation = useMutation<unknown, Error, "approve" | "reject">({
    mutationFn: (decision) => authenticatedFetch(`/api/v1/artworks/${artworkId}/review/${decision}`, { method: "POST", body: { reason: reason.trim() } }),
    onSuccess: () => router.replace("/admin/artworks/review")
  });

  if (loading) return <Message text="正在加载账户..." />;
  if (!session || session.user.role !== "ADMIN") return <Message text="只有管理员可以访问此页面。" alert />;
  if (review.isError) return <Message text={review.error instanceof ApiError ? review.error.problem.detail ?? "作品加载失败" : "作品加载失败"} alert />;
  if (review.isPending || !review.data) return <Message text="正在加载审核详情..." />;

  const item = review.data;
  async function submit(decision: "approve" | "reject") {
    setFormError(null);
    if (reason.trim().length === 0) {
      setFormError("请填写审核理由。");
      return;
    }
    mutation.mutate(decision);
  }

  return (
    <main>
      <header className="border-b border-ink-300 pb-6"><p className="text-xs uppercase tracking-[0.18em] text-brand-600">Admin / Review</p><h1 className="mt-2 font-serif text-4xl text-ink-900">审核作品</h1><p className="mt-3 text-sm text-ink-500">{item.title} · {item.creator.displayName} · {item.productType}</p></header>
      <dl className="grid gap-4 border-b border-ink-300 py-6 sm:grid-cols-3"><div><dt className="text-sm text-ink-500">分类</dt><dd className="mt-1 text-ink-900">{item.category}</dd></div><div><dt className="text-sm text-ink-500">价格</dt><dd className="mt-1 text-ink-900">{item.priceAmount} {item.priceCurrency}</dd></div><div><dt className="text-sm text-ink-500">库存</dt><dd className="mt-1 text-ink-900">{item.stock}</dd></div></dl>
      <p className="max-w-3xl whitespace-pre-wrap py-6 text-ink-700">{item.description}</p>
      <form className="max-w-2xl border-t border-ink-300 pt-6" onSubmit={(event) => { event.preventDefault(); void submit("approve"); }}>
        <label className="flex flex-col gap-1.5 text-sm text-ink-700">审核理由<textarea className="min-h-32 rounded-token border border-ink-300 bg-white p-3" maxLength={2000} value={reason} onChange={(event) => setReason(event.target.value)} required /></label>
        {formError ? <p role="alert" className="mt-3 text-sm text-error">{formError}</p> : null}
        {mutation.isError ? <p role="alert" className="mt-3 text-sm text-error">{mutation.error instanceof ApiError ? mutation.error.problem.detail ?? "审核操作失败" : "审核操作失败"}</p> : null}
        <div className="mt-5 flex flex-wrap gap-3"><Button type="submit" loading={mutation.isPending}>批准并进入发布</Button><Button type="button" variant="danger" loading={mutation.isPending} onClick={() => void submit("reject")}>拒绝并退回</Button></div>
      </form>
    </main>
  );
}

function Message({ text, alert = false }: { text: string; alert?: boolean }) {
  return <main className="py-12"><p role={alert ? "alert" : undefined} className={alert ? "text-error" : "text-ink-500"}>{text}</p></main>;
}
