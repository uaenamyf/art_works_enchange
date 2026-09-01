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
import { useLocale } from "@/features/locale/LocaleProvider";

const schema = z.object({
  email: z.string().email("请输入有效邮箱"),
  password: z.string().min(1, "请输入密码")
});

type LoginValues = z.infer<typeof schema>;

/**
 * 登录交互表单：使用 RHF + Zod 做客户端反馈，并把服务端字段错误映射回输入项。
 *
 * Access Token 只存在于当前调用结果中，不写入 localStorage/sessionStorage；Refresh Token
 * 由后端 HttpOnly Cookie 管理。全局认证状态将在 TanStack Query Provider 接入后统一维护。
 */
export function LoginForm() {
  const router = useRouter();
  const { setSession } = useAuth();
  const { locale } = useLocale();
  const english = locale === "en-US";
  const [formError, setFormError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors, isSubmitting }
  } = useForm<LoginValues>({ resolver: zodResolver(schema) });

  async function submit(values: LoginValues) {
    setFormError(null);
    try {
      const session = await apiFetch<AuthSession>("/api/v1/auth/login", {
        method: "POST",
        body: values
      });
      setSession(session);
      startTransition(() => router.replace(ROUTES.home));
    } catch (error) {
      if (error instanceof ApiError) {
        for (const [field, message] of Object.entries(error.problem.fieldErrors ?? {})) {
          if (field === "email" || field === "password") {
            setError(field, { message });
          }
        }
        setFormError(
          error.problem.code === "INVALID_CREDENTIALS"
            ? english ? "Invalid email or password" : "邮箱或密码错误"
            : error.problem.code === "RATE_LIMITED"
              ? english ? "Too many attempts. Please try again later." : "尝试次数过多，请稍后再试"
              : english ? "Unable to log in. Please try again." : error.problem.detail ?? "登录失败，请稍后再试"
        );
        return;
      }
      setFormError(english ? "Unable to log in. Please try again." : "登录失败，请稍后再试");
    }
  }

  return (
    <form
      className="flex flex-col gap-4 rounded-token border border-ink-300 bg-white p-6"
      onSubmit={handleSubmit(submit)}
      noValidate
    >
      <header>
        <h1 className="text-2xl text-ink-900">{english ? "Log in" : "登录"}</h1>
        <p className="mt-1 text-sm text-ink-500">{english ? "Continue with your email and password." : "使用邮箱与密码继续。"}</p>
      </header>
      <Input
        label={english ? "Email" : "邮箱"}
        type="email"
        autoComplete="email"
        error={errors.email?.message}
        {...register("email")}
      />
      <Input
        label={english ? "Password" : "密码"}
        type="password"
        autoComplete="current-password"
        error={errors.password?.message}
        {...register("password")}
      />
      {formError ? (
        <p role="alert" className="text-sm text-error">
          {formError}
        </p>
      ) : null}
      <Button size="lg" type="submit" loading={isSubmitting}>
        {english ? "Log in" : "登录"}
      </Button>
      <p className="text-center text-sm text-ink-500">
        {english ? "Don't have an account?" : "还没有账号？"}
        <Link href={ROUTES.authRegister} className="ml-1 text-ink-900 underline-offset-2 hover:underline">
          {english ? "Create an account" : "创建账号"}
        </Link>
      </p>
    </form>
  );
}
