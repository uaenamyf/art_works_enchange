"use client";

import { useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";

const categories = [
  "ART",
  "PHOTOGRAPHY",
  "CERAMICS",
  "WOODWORK",
  "LEATHER",
  "JEWELRY",
  "FASHION",
  "DIGITAL",
  "COLLECTIBLES"
] as const;

const schema = z.object({
  title: z.string().trim().min(2, "标题至少 2 个字符").max(160),
  description: z.string().trim().min(10, "描述至少 10 个字符").max(10000),
  category: z.enum(categories),
  amount: z.coerce.number().positive("价格必须大于 0"),
  currency: z.string().regex(/^[A-Z]{3}$/, "请输入 3 位大写货币代码"),
  stock: z.coerce.number().int().min(0, "库存不能为负数").max(100000)
});

type Values = z.infer<typeof schema>;
type ArtworkDetail = {
  id: string;
  title: string;
  description: string;
  category: (typeof categories)[number];
  priceAmount: string;
  priceCurrency: string;
  stock: number;
  mediaIds: string[];
  status: string;
  version: number;
};

/** Studio 编辑器：按最新版本更新作品，并只允许草稿或被拒绝作品再次提交审核。 */
export function StudioArtworkEditor({ artworkId }: { artworkId: string }) {
  const { session, loading, authenticatedFetch } = useAuth();
  const authenticatedFetchRef = useRef(authenticatedFetch);
  authenticatedFetchRef.current = authenticatedFetch;
  const [artwork, setArtwork] = useState<ArtworkDetail | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const {
    register,
    reset,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<Values>({ resolver: zodResolver(schema) });

  useEffect(() => {
    if (!session) return;
    let active = true;
    authenticatedFetchRef.current<ArtworkDetail>(`/api/v1/studio/artworks/${artworkId}`)
      .then((loaded) => {
        if (!active) return;
        setArtwork(loaded);
        reset({
          title: loaded.title,
          description: loaded.description,
          category: loaded.category,
          amount: Number(loaded.priceAmount),
          currency: loaded.priceCurrency,
          stock: loaded.stock
        });
      })
      .catch((error) => {
        if (active) setLoadError(error instanceof ApiError ? error.problem.detail ?? "作品加载失败" : "作品加载失败");
      });
    return () => {
      active = false;
    };
  }, [artworkId, reset, session]);

  async function save(values: Values, submitForReview = false) {
    if (!artwork) return;
    setFormError(null);
    setSaved(false);
    try {
      const updated = await authenticatedFetch<ArtworkDetail>(`/api/v1/artworks/${artwork.id}`, {
        method: "PUT",
        body: {
          title: values.title,
          description: values.description,
          category: values.category,
          price: { amount: values.amount.toFixed(2), currency: values.currency },
          stock: values.stock,
          mediaIds: artwork.mediaIds,
          version: artwork.version
        }
      });
      setArtwork(updated);
      setSaved(true);
      if (submitForReview) {
        const reviewed = await authenticatedFetch<ArtworkDetail>(
          `/api/v1/artworks/${artwork.id}/submit-review`,
          { method: "POST" }
        );
        setArtwork(reviewed);
      }
    } catch (error) {
      setFormError(error instanceof ApiError ? error.problem.detail ?? "保存失败，请刷新后重试" : "保存失败，请检查网络后重试");
    }
  }

  if (loading) return <p className="text-sm text-ink-500">正在恢复登录状态…</p>;
  if (!session) return <p role="alert" className="text-sm text-error">请先登录并完成 Creator 入驻。</p>;
  if (loadError) return <p role="alert" className="text-sm text-error">{loadError}</p>;
  if (!artwork) return <p role="status" className="text-sm text-ink-500">正在加载作品…</p>;

  const editable = artwork.status === "DRAFT" || artwork.status === "REJECTED";
  return (
    <form className="flex max-w-2xl flex-col gap-5" onSubmit={handleSubmit((values) => save(values))} noValidate>
      <div className="flex items-center justify-between border-b border-ink-300 pb-4">
        <div>
          <p className="text-sm text-ink-500">作品状态</p>
          <p className="mt-1 text-lg text-ink-900">{artwork.status}</p>
        </div>
        <p className="text-sm text-ink-500">已绑定 {artwork.mediaIds.length} 张图片</p>
      </div>
      <Input label="作品标题" error={errors.title?.message} disabled={!editable} {...register("title")} />
      <label className="flex flex-col gap-1.5 text-sm text-ink-700">
        作品描述
        <textarea className="min-h-36 rounded-token border border-ink-300 bg-white p-3 disabled:bg-ink-100" disabled={!editable} {...register("description")} />
        {errors.description ? <span className="text-error">{errors.description.message}</span> : null}
      </label>
      <label className="flex flex-col gap-1.5 text-sm text-ink-700">
        类别
        <select className="h-11 rounded-token border border-ink-300 bg-white px-3 disabled:bg-ink-100" disabled={!editable} {...register("category")}>
          {categories.map((category) => <option key={category}>{category}</option>)}
        </select>
      </label>
      <div className="grid gap-4 sm:grid-cols-[1fr_7rem]">
        <Input label="价格" type="number" min="0.01" step="0.01" disabled={!editable} error={errors.amount?.message} {...register("amount")} />
        <Input label="货币" maxLength={3} disabled={!editable} error={errors.currency?.message} {...register("currency")} />
      </div>
      <Input label="库存" type="number" min="0" step="1" disabled={!editable} error={errors.stock?.message} {...register("stock")} />
      {formError ? <p role="alert" className="text-sm text-error">{formError}</p> : null}
      {saved ? <p role="status" className="text-sm text-success">作品已保存</p> : null}
      {editable ? (
        <div className="flex flex-wrap gap-3">
          <Button type="submit" loading={isSubmitting}>保存草稿</Button>
          <Button type="button" variant="secondary" loading={isSubmitting} onClick={handleSubmit((values) => save(values, true))}>保存并提交审核</Button>
        </div>
      ) : null}
    </form>
  );
}