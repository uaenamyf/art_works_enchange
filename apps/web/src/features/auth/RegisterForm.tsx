"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { startTransition, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { ApiError, apiFetch } from "@/lib/api/fetcher";
import { ROUTES } from "@/lib/config";
import type { AuthSession } from "./types";
import { useAuth } from "./AuthProvider";

const schema = z.object({
  email: z.string().email("请输入有效邮箱").max(254, "邮箱过长"),
  password: z.string().min(10, "密码至少 10 个字符").max(128, "密码最多 128 个字符"),
  displayName: z.string().trim().min(2, "名字至少 2 个字符").max(50, "名字最多 50 个字符")
});

type RegisterValues = z.infer<typeof schema>;

/**
 * 注册交互表单，字段约束与 OpenAPI RegisterRequest 保持一致。
 *
 * 用户角色和 owner 完全由服务端决定；前端不提交角色、用户 ID 或 Creator ID。
 */
export function RegisterForm() {
  const router = useRouter();
  const { setSession } = useAuth();
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting }
  } = useForm<RegisterValues>({ resolver: zodResolver(schema) });

  async function submit(values: RegisterValues) {
    setFormError(null);
    try {
      const session = await apiFetch<AuthSession>("/api/v1/auth/register", {
        method: "POST",
        body: values
      });
      setSession(session);
      startTransition(() => router.replace(ROUTES.home));
    } catch (error) {
      if (error instanceof ApiError) {
        for (const [field, message] of Object.entries(error.problem.fieldErrors ?? {})) {
          if (field === "email" || field === "password" || field === "displayName") {
            setError(field, { message });
          }
        }
        setFormError(
          error.problem.code === "EMAIL_TAKEN"
            ? "该邮箱已注册"
            : error.problem.code === "RATE_LIMITED"
              ? "注册次数过多，请稍后再试"
              : error.problem.detail ?? "注册失败，请稍后再试"
        );
        return;
      }
      setFormError("注册失败，请稍后再试");
    }
  }

  return (
    <form
      className="flex flex-col gap-4 rounded-token border border-ink-300 bg-white p-6"
      onSubmit={handleSubmit(submit)}
      noValidate
    >
      <header>
        <h1 className="text-2xl text-ink-900">创建账号</h1>
        <p className="mt-1 text-sm text-ink-500">先创建普通账号，登录后再申请 Creator 档案。</p>
      </header>
      <Input
        label="邮箱"
        type="email"
        autoComplete="email"
        error={errors.email?.message}
        {...register("email")}
      />
      <Input
        label="密码"
        type="password"
        autoComplete="new-password"
        error={errors.password?.message}
        {...register("password")}
      />
      <Input
        label="显示名称"
        type="text"
        autoComplete="name"
        error={errors.displayName?.message}
        {...register("displayName")}
      />
      {formError ? (
        <p role="alert" className="text-sm text-error">
          {formError}
        </p>
      ) : null}
      <Button size="lg" type="submit" loading={isSubmitting}>
        创建账号
      </Button>
      <p className="text-center text-sm text-ink-500">
        已有账号？
        <Link href={ROUTES.authLogin} className="ml-1 text-ink-900 underline-offset-2 hover:underline">
          返回登录
        </Link>
      </p>
    </form>
  );
}
