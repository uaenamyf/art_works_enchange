"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "./AuthProvider";

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
  displayName: z.string().trim().min(2, "名称至少 2 个字符").max(50),
  slug: z
    .string()
    .regex(/^[a-z0-9](?:[a-z0-9-]{1,38}[a-z0-9])?$/, "slug 仅支持小写字母、数字和连字符"),
  category: z.enum(categories),
  bio: z.string().max(2000, "简介最多 2000 个字符"),
  location: z.string().max(100, "地点最多 100 个字符").optional()
});

type Values = z.infer<typeof schema>;
interface CreatorDetail { id: string }

/** Creator 申请表单；owner 和角色只来自 Access Token，表单不能指定用户 ID。 */
export function CreatorApplicationForm() {
  const router = useRouter();
  const { session, loading, authenticatedFetch, refresh } = useAuth();
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting }
  } = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { category: "ART" } });

  async function submit(values: Values) {
    setFormError(null);
    try {
      const creator = await authenticatedFetch<CreatorDetail>("/api/v1/me/creator", {
        method: "POST",
        body: values
      });
      await refresh();
      router.replace(`/creators/${creator.id}`);
    } catch (error) {
      setFormError(
        error instanceof ApiError
          ? error.problem.code === "CREATOR_SLUG_TAKEN"
            ? "该公开地址已被使用"
            : error.problem.detail ?? "申请失败，请稍后再试"
          : "申请失败，请稍后再试"
      );
    }
  }

  if (loading) {
    return <p className="text-sm text-ink-500">正在恢复登录状态…</p>;
  }
  if (!session) {
    return <p role="alert" className="text-sm text-error">请先登录，再申请 Creator。</p>;
  }

  return (
    <form className="flex flex-col gap-4" onSubmit={handleSubmit(submit)} noValidate>
      <Input label="创作者名称" error={errors.displayName?.message} {...register("displayName")} />
      <Input label="公开 slug" hint="例如 mia-studio" error={errors.slug?.message} {...register("slug")} />
      <label className="flex flex-col gap-1.5 text-sm text-ink-700">
        类别
        <select className="h-11 rounded-token border border-ink-300 bg-white px-3" {...register("category")}>
          {categories.map((category) => <option key={category}>{category}</option>)}
        </select>
      </label>
      <label className="flex flex-col gap-1.5 text-sm text-ink-700">
        简介
        <textarea className="min-h-32 rounded-token border border-ink-300 bg-white p-3" {...register("bio")} />
        {errors.bio ? <span className="text-error">{errors.bio.message}</span> : null}
      </label>
      <Input label="地点（可选）" error={errors.location?.message} {...register("location")} />
      {formError ? <p role="alert" className="text-sm text-error">{formError}</p> : null}
      <Button type="submit" size="lg" loading={isSubmitting}>提交申请</Button>
    </form>
  );
}
