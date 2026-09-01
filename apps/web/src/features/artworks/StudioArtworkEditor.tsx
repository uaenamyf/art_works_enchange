"use client";

import { useEffect, useRef, useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";
import { useLocale } from "@/features/locale/LocaleProvider";
import type { components } from "@/lib/api/schema.gen";

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
type ReviewRecord = components["schemas"]["ReviewRecord"];

/** Studio 编辑器：按最新版本更新作品，并只允许草稿或被拒绝作品再次提交审核。 */
export function StudioArtworkEditor({ artworkId }: { artworkId: string }) {
  const { session, loading, authenticatedFetch } = useAuth();
  const { t } = useLocale();
  const authenticatedFetchRef = useRef(authenticatedFetch);
  authenticatedFetchRef.current = authenticatedFetch;
  const [artwork, setArtwork] = useState<ArtworkDetail | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [published, setPublished] = useState(false);
  const reviews = useQuery<ReviewRecord[], Error>({
    queryKey: ["studio-artwork-reviews", artworkId],
    enabled: Boolean(session) && !loading,
    queryFn: () => authenticatedFetchRef.current<ReviewRecord[]>(`/api/v1/studio/artworks/${artworkId}/reviews`)
  });
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
        if (active) setLoadError(error instanceof ApiError ? error.problem.detail ?? t("artworkLoadError") : t("artworkLoadError"));
      });
    return () => {
      active = false;
    };
  }, [artworkId, reset, session, t]);

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
      setFormError(error instanceof ApiError ? error.problem.detail ?? t("saveError") : t("saveNetworkError"));
    }
  }

  async function publish() {
    if (!artwork || artwork.status !== "APPROVED") return;
    setFormError(null);
    setPublished(false);
    try {
      const updated = await authenticatedFetch<ArtworkDetail>(`/api/v1/artworks/${artwork.id}/publish`, {
        method: "POST"
      });
      setArtwork(updated);
      setPublished(true);
    } catch (error) {
      setFormError(error instanceof ApiError ? error.problem.detail ?? t("publishError") : t("publishError"));
    }
  }

  if (loading) return <p className="text-sm text-ink-500">{t("restoringSession")}</p>;
  if (!session) return <p role="alert" className="text-sm text-error">{t("loginCreatorRequired")}</p>;
  if (loadError) return <p role="alert" className="text-sm text-error">{loadError}</p>;
  if (!artwork) return <p role="status" className="text-sm text-ink-500">{t("loadingArtworks")}</p>;

  const editable = artwork.status === "DRAFT" || artwork.status === "REJECTED";
  const latestRejection = reviews.data?.find((review) => review.decision === "REJECTED");
  return (
    <form className="flex max-w-2xl flex-col gap-5" onSubmit={handleSubmit((values) => save(values))} noValidate>
      <div className="flex items-center justify-between border-b border-ink-300 pb-4">
        <div>
          <p className="text-sm text-ink-500">{t("artworkStatus")}</p>
          <p className="mt-1 text-lg text-ink-900">{artwork.status}</p>
        </div>
        <p className="text-sm text-ink-500">{t("boundImages").replace("{count}", String(artwork.mediaIds.length))}</p>
      </div>
      {artwork.status === "APPROVED" ? (
        <section className="flex flex-wrap items-center justify-between gap-4 border-b border-ink-300 pb-5">
          <p className="text-sm text-ink-700">{t("approvedArtworkHint")}</p>
          <Button type="button" onClick={() => void publish()}>{t("publishArtwork")}</Button>
        </section>
      ) : null}
      {latestRejection ? <p className="border-b border-ink-300 pb-5 text-sm text-error">{t("latestReviewReason").replace("{reason}", latestRejection.reason)}</p> : null}
      <Input label={t("artworkTitle")} error={errors.title?.message} disabled={!editable} {...register("title")} />
      <label className="flex flex-col gap-1.5 text-sm text-ink-700">
        {t("artworkDescription")}
        <textarea className="min-h-36 rounded-token border border-ink-300 bg-white p-3 disabled:bg-ink-100" disabled={!editable} {...register("description")} />
        {errors.description ? <span className="text-error">{errors.description.message}</span> : null}
      </label>
      <label className="flex flex-col gap-1.5 text-sm text-ink-700">
        {t("category")}
        <select className="h-11 rounded-token border border-ink-300 bg-white px-3 disabled:bg-ink-100" disabled={!editable} {...register("category")}>
          {categories.map((category) => <option key={category}>{category}</option>)}
        </select>
      </label>
      <div className="grid gap-4 sm:grid-cols-[1fr_7rem]">
        <Input label={t("price")} type="number" min="0.01" step="0.01" disabled={!editable} error={errors.amount?.message} {...register("amount")} />
        <Input label={t("currency")} maxLength={3} disabled={!editable} error={errors.currency?.message} {...register("currency")} />
      </div>
      <Input label={t("stock")} type="number" min="0" step="1" disabled={!editable} error={errors.stock?.message} {...register("stock")} />
      {formError ? <p role="alert" className="text-sm text-error">{formError}</p> : null}
      {saved ? <p role="status" className="text-sm text-success">{t("saved")}</p> : null}
      {published ? <p role="status" className="text-sm text-success">{t("publishedNotice")}</p> : null}
      {editable ? (
        <div className="flex flex-wrap gap-3">
          <Button type="submit" loading={isSubmitting}>{t("saveDraft")}</Button>
          <Button type="button" variant="secondary" loading={isSubmitting} onClick={handleSubmit((values) => save(values, true))}>{t("saveAndSubmit")}</Button>
        </div>
      ) : null}
    </form>
  );
}