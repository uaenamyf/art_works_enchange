import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";

export default function LoginPage() {
  return (
    <form
      className="flex flex-col gap-4 rounded-token border border-ink-300 bg-white p-6"
      action="/auth/login"
      method="post"
    >
      <header>
        <h1 className="text-2xl text-ink-900">登录</h1>
        <p className="mt-1 text-sm text-ink-500">使用邮箱与密码继续。</p>
      </header>
      <Input label="邮箱" type="email" name="email" autoComplete="email" required />
      <Input label="密码" type="password" name="password" autoComplete="current-password" required />
      <Button size="lg" type="submit">
        登录
      </Button>
      <p className="text-center text-sm text-ink-500">
        还没有账号？
        <a href="/auth/register" className="ml-1 text-ink-900 underline-offset-2 hover:underline">
          创作者入驻
        </a>
      </p>
      <p className="text-center text-xs text-ink-500">
        Week 1-2 阶段登录页为占位；Identity 上下文实现后接入。
      </p>
    </form>
  );
}
