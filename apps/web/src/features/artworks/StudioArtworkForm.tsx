"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
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
type ArtworkDetail = { id: string; version: number };
type UploadTarget = { mediaId: string; uploadUrl: string; method: "PUT"; contentType: string };

/** Studio 作品创建表单：先上传并确认媒体，再绑定媒体并提交审核。 */
export function StudioArtworkForm() {
  const router = useRouter();
  const { session, loading, authenticatedFetch } = useAuth();
  const [files, setFiles] = useState<File[]>([]);
  const [formError, setFormError] = useState<string | null>(null);
  const [progress, setProgress] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<Values>({
    resolver: zodResolver(schema),
    defaultValues: { category: "ART", currency: "USD", stock: 1 }
  });

  async function submit(values: Values) {
    setFormError(null);
    if (files.length === 0) {
      setFormError("请至少选择一张作品图片");
      return;
    }

    try {
      setProgress("正在创建草稿…");
      const artwork = await authenticatedFetch<ArtworkDetail>("/api/v1/artworks", {
        method: "POST",
        body: {
          title: values.title,
          description: values.description,
          category: values.category,
          price: { amount: values.amount.toFixed(2), currency: values.currency },
          stock: values.stock,
          mediaIds: [],
          version: 0
        }
      });

      const mediaIds: string[] = [];
      for (const [index, file] of files.entries()) {
        setProgress(`正在上传图片 ${index + 1}/${files.length}…`);
        const target = await authenticatedFetch<UploadTarget>("/api/v1/media/upload-requests", {
          method: "POST",
          body: {
            fileName: file.name,
            contentType: file.type,
            sizeBytes: file.size,
            purpose: "ARTWORK"
          }
        });
        const upload = await fetch(target.uploadUrl, {
          method: target.method,
          headers: { "Content-Type": target.contentType },
          body: file
        });
        if (!upload.ok) {
          throw new Error(`图片 ${index + 1} 上传失败`);
        }
        await authenticatedFetch<void>(`/api/v1/media/${target.mediaId}/confirm`, { method: "POST" });
        mediaIds.push(target.mediaId);
      }

      setProgress("正在保存作品并提交审核…");
      await authenticatedFetch<ArtworkDetail>(`/api/v1/artworks/${artwork.id}`, {
        method: "PUT",
        body: {
          title: values.title,
          description: values.description,
          category: values.category,
          price: { amount: values.amount.toFixed(2), currency: values.currency },
          stock: values.stock,
          mediaIds,
          version: artwork.version
        }
      });
      await authenticatedFetch(`/api/v1/artworks/${artwork.id}/submit-review`, { method: "POST" });
      router.replace(`/studio/artworks/${artwork.id}`);
    } catch (error) {
      setProgress(null);
      setFormError(error instanceof ApiError ? error.problem.detail ?? "保存失败，请稍后再试" : "上传或保存失败，请检查网络后重试");
    }
  }

  if (loading) return <p className="text-sm text-ink-500">正在恢复登录状态…</p>;
  if (!session) return <p role="alert" className="text-sm text-error">请先登录并完成 Creator 入驻。</p>;

  return (
    <form className="flex max-w-2xl flex-col gap-5" onSubmit={handleSubmit(submit)} noValidate>
      <Input label="作品标题" error={errors.title?.message} {...register("title")} />
      <label className="flex flex-col gap-1.5 text-sm text-ink-700">
        作品描述
        <textarea className="min-h-36 rounded-token border border-ink-300 bg-white p-3" {...register("description")} />
        {errors.description ? <span className="text-error">{errors.description.message}</span> : null}
      </label>
      <label className="flex flex-col gap-1.5 text-sm text-ink-700">
        类别
        <select className="h-11 rounded-token border border-ink-300 bg-white px-3" {...register("category")}>
          {categories.map((category) => <option key={category}>{category}</option>)}
        </select>
      </label>
      <div className="grid gap-4 sm:grid-cols-[1fr_7rem]">
        <Input label="价格" type="number" min="0.01" step="0.01" error={errors.amount?.message} {...register("amount")} />
        <Input label="货币" maxLength={3} error={errors.currency?.message} {...register("currency")} />
      </div>
      <Input label="库存" type="number" min="0" step="1" error={errors.stock?.message} {...register("stock")} />
      <label className="flex flex-col gap-1.5 text-sm text-ink-700">
        作品图片（最多 12 张）
        <input
          className="rounded-token border border-dashed border-ink-300 bg-white p-3 text-sm"
          type="file"
          accept="image/*"
          multiple
          onChange={(event) => setFiles(Array.from(event.target.files ?? []).slice(0, 12))}
        />
        <span className="text-ink-500">已选择 {files.length} 张图片</span>
      </label>
      {formError ? <p role="alert" className="text-sm text-error">{formError}</p> : null}
      {progress ? <p role="status" className="text-sm text-ink-500">{progress}</p> : null}
      <Button type="submit" size="lg" loading={isSubmitting}>保存并提交审核</Button>
    </form>
  );
}