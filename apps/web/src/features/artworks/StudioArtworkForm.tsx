"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";
import { useLocale } from "@/features/locale/LocaleProvider";

const categories = ["ART", "PHOTOGRAPHY", "CERAMICS", "WOODWORK", "LEATHER", "JEWELRY", "FASHION", "DIGITAL", "COLLECTIBLES"] as const;
const schema = z.object({
  productType: z.enum(["PHYSICAL", "DIGITAL"]),
  title: z.string().trim().min(2, "标题至少 2 个字符").max(160),
  description: z.string().trim().min(10, "描述至少 10 个字符").max(10000),
  category: z.enum(categories),
  amount: z.coerce.number().positive("价格必须大于 0"),
  currency: z.string().regex(/^[A-Z]{3}$/, "请输入 3 位大写货币代码"),
  stock: z.coerce.number().int().min(0, "库存不能为负数").max(100000)
});

type Values = z.infer<typeof schema>;
type ArtworkDetail = { id: string; version: number };
type UploadTarget = { mediaId: string; uploadUrl: string; method: "PUT"; contentType: string };
type DigitalUploadTarget = { versionId: string; version: number; uploadUrl: string; method: "PUT"; contentType: string };

/** Studio 作品创建表单：先确认媒体，再绑定作品并提交审核或直接上架。 */
export function StudioArtworkForm() {
  const router = useRouter();
  const { session, loading, authenticatedFetch } = useAuth();
  const { locale, t } = useLocale();
  const english = locale === "en-US";
  const [files, setFiles] = useState<File[]>([]);
  const [digitalFile, setDigitalFile] = useState<File | null>(null);
  const [formError, setFormError] = useState<string | null>(null);
  const [progress, setProgress] = useState<string | null>(null);
  const { register, handleSubmit, control, formState: { errors, isSubmitting } } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { productType: "PHYSICAL", category: "ART", currency: "USD", stock: 1 } });
  const productType = useWatch({ control, name: "productType" });

  async function submit(values: Values) {
    setFormError(null);
    if (files.length === 0) { setFormError(english ? "Select at least one artwork image." : "请至少选择一张作品图片"); return; }
    if (values.productType === "DIGITAL" && !digitalFile) { setFormError(english ? "Select a digital asset file." : "请选择数字资产文件"); return; }
    try {
      setProgress(english ? "Creating draft..." : "正在创建草稿…");
      const artwork = await authenticatedFetch<ArtworkDetail>("/api/v1/artworks", { method: "POST", body: { title: values.title, productType: values.productType, description: values.description, category: values.category, price: { amount: values.amount.toFixed(2), currency: values.currency }, stock: values.stock, mediaIds: [], version: 0 } });
      const mediaIds: string[] = [];
      for (const [index, file] of files.entries()) {
        setProgress(english ? `Uploading image ${index + 1}/${files.length}...` : `正在上传图片 ${index + 1}/${files.length}…`);
        const target = await authenticatedFetch<UploadTarget>("/api/v1/media/upload-requests", { method: "POST", body: { fileName: file.name, contentType: file.type, sizeBytes: file.size, purpose: "ARTWORK" } });
        const upload = await fetch(target.uploadUrl, { method: target.method, headers: { "Content-Type": target.contentType }, body: file });
        if (!upload.ok) throw new Error(english ? `Image ${index + 1} upload failed` : `图片 ${index + 1} 上传失败`);
        await authenticatedFetch<void>(`/api/v1/media/${target.mediaId}/confirm`, { method: "POST" });
        mediaIds.push(target.mediaId);
      }
      if (values.productType === "DIGITAL" && digitalFile) {
        setProgress(english ? "Checking and uploading digital asset..." : "正在校验并上传数字资产…");
        const digest = await crypto.subtle.digest("SHA-256", await digitalFile.arrayBuffer());
        const sha256 = Array.from(new Uint8Array(digest)).map((byte) => byte.toString(16).padStart(2, "0")).join("");
        const target = await authenticatedFetch<DigitalUploadTarget>("/api/v1/digital-assets/upload-requests", { method: "POST", body: { artworkId: artwork.id, fileName: digitalFile.name, contentType: digitalFile.type || "application/octet-stream", sizeBytes: digitalFile.size, sha256 } });
        const upload = await fetch(target.uploadUrl, { method: target.method, headers: { "Content-Type": target.contentType }, body: digitalFile });
        if (!upload.ok) throw new Error(english ? "Digital asset upload failed" : "数字资产上传失败");
        await authenticatedFetch<void>(`/api/v1/digital-assets/${target.versionId}/confirm`, { method: "POST" });
      }
      setProgress(english ? "Saving and submitting artwork..." : "正在保存作品并提交审核…");
      await authenticatedFetch<ArtworkDetail>(`/api/v1/artworks/${artwork.id}`, { method: "PUT", body: { title: values.title, productType: values.productType, description: values.description, category: values.category, price: { amount: values.amount.toFixed(2), currency: values.currency }, stock: values.stock, mediaIds, version: artwork.version } });
      await authenticatedFetch(`/api/v1/artworks/${artwork.id}/submit-review`, { method: "POST" });
      router.replace(`/studio/artworks/${artwork.id}`);
    } catch (error) {
      setProgress(null);
      setFormError(error instanceof ApiError ? error.problem.detail ?? (english ? t("saveNetworkError") : t("saveError")) : english ? t("saveNetworkError") : t("saveError"));
    }
  }

  if (loading) return <p className="text-sm text-ink-500">{t("restoringSession")}</p>;
  if (!session) return <p role="alert" className="text-sm text-error">{t("loginCreatorRequired")}</p>;
  return <form className="flex max-w-2xl flex-col gap-5" onSubmit={handleSubmit(submit)} noValidate>
    <fieldset className="grid gap-3 border-b border-ink-300 pb-5"><legend className="text-sm text-ink-700">{english ? "Product type" : "产品类型"}</legend><div className="grid gap-3 sm:grid-cols-2"><label className="flex cursor-pointer items-start gap-3 rounded-token border border-ink-300 bg-white p-4 has-[:checked]:border-ink-900"><input type="radio" value="PHYSICAL" {...register("productType")} /><span><span className="block text-sm text-ink-900">{english ? "Physical product" : "实体产品"}</span><span className="mt-1 block text-xs text-ink-500">{english ? "Requires stock and shipping address" : "需要库存和收货地址"}</span></span></label><label className="flex cursor-pointer items-start gap-3 rounded-token border border-ink-300 bg-white p-4 has-[:checked]:border-ink-900"><input type="radio" value="DIGITAL" {...register("productType")} /><span><span className="block text-sm text-ink-900">{english ? "Digital product" : "数字产品"}</span><span className="mt-1 block text-xs text-ink-500">{english ? "Download after payment authorization" : "支付后通过授权下载"}</span></span></label></div></fieldset>
    <Input label={t("artworkTitle")} error={errors.title?.message} {...register("title")} />
    <label className="flex flex-col gap-1.5 text-sm text-ink-700">{t("artworkDescription")}<textarea className="min-h-36 rounded-token border border-ink-300 bg-white p-3" {...register("description")} />{errors.description ? <span className="text-error">{errors.description.message}</span> : null}</label>
    <label className="flex flex-col gap-1.5 text-sm text-ink-700">{t("category")}<select className="h-11 rounded-token border border-ink-300 bg-white px-3" {...register("category")}>{categories.map((category) => <option key={category}>{category}</option>)}</select></label>
    <div className="grid gap-4 sm:grid-cols-[1fr_7rem]"><Input label={t("price")} type="number" min="0.01" step="0.01" error={errors.amount?.message} {...register("amount")} /><Input label={t("currency")} maxLength={3} error={errors.currency?.message} {...register("currency")} /></div>
    {productType === "PHYSICAL" ? <Input label={t("stock")} type="number" min="0" step="1" error={errors.stock?.message} {...register("stock")} /> : <><p className="rounded-token border border-ink-300 bg-ink-50 p-3 text-sm text-ink-500">{english ? "Digital products do not use physical stock. Uploaded files enter a scanning queue." : "数字产品不扣实体库存。上传后文件会进入扫描队列。"}</p><label className="flex flex-col gap-1.5 text-sm text-ink-700">{english ? "Digital asset file" : "数字资产文件"}<input className="rounded-token border border-dashed border-ink-300 bg-white p-3 text-sm" type="file" aria-label={english ? "Digital asset file" : "数字资产文件"} accept="application/pdf,application/zip,application/epub+zip,audio/mpeg,audio/wav,video/mp4,font/ttf,font/otf,application/octet-stream" onChange={(event) => setDigitalFile(event.target.files?.[0] ?? null)} /><span className="text-ink-500">{digitalFile ? `${digitalFile.name} · ${digitalFile.size} bytes` : english ? "No digital file selected" : "尚未选择数字文件"}</span></label></>}
    <label className="flex flex-col gap-1.5 text-sm text-ink-700">{english ? "Artwork images (up to 12)" : "作品图片（最多 12 张）"}<input className="rounded-token border border-dashed border-ink-300 bg-white p-3 text-sm" type="file" aria-label={english ? "Artwork images (up to 12)" : "作品图片（最多 12 张）"} accept="image/*" multiple onChange={(event) => setFiles(Array.from(event.target.files ?? []).slice(0, 12))} /><span className="text-ink-500">{english ? `${files.length} image(s) selected` : `已选择 ${files.length} 张图片`}</span></label>
    {formError ? <p role="alert" className="text-sm text-error">{formError}</p> : null}{progress ? <p role="status" className="text-sm text-ink-500">{progress}</p> : null}<Button type="submit" size="lg" loading={isSubmitting}>{english ? "Submit artwork" : t("saveSubmitArtwork")}</Button>
  </form>;
}
