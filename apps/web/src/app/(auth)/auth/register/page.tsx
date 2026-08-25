import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";

export default function RegisterPage() {
  return (
    <form
      className="flex flex-col gap-4 rounded-token border border-ink-300 bg-white p-6"
      action="/auth/register"
      method="post"
    >
      <header>
        <h1 className="text-2xl text-ink-900">创作者入驻</h1>
        <p className="mt-1 text-sm text-ink-500">我们会先帮你创建账号，再补充创作者资料。</p>
      </header>
      <Input label="邮箱" type="email" name="email" autoComplete="email" required />
      <Input
        label="密码"
        type="password"
        name="password"
        autoComplete="new-password"
        minLength={8}
        required
      />
      <Input label="创作者名字" type="text" name="displayName" required />
      <Button size="lg" type="submit">
        创建账号
      </Button>
      <p className="text-center text-xs text-ink-500">
        Week 1-2 阶段注册页为占位；Identity + Creator 上下文实现后接入。
      </p>
    </form>
  );
}
