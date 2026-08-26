import { CreatorApplicationForm } from "@/features/auth/CreatorApplicationForm";

/** Creator 入驻页面；具体权限与 owner 由后端 JWT 和对象级规则决定。 */
export default function CreatorApplyPage() {
  return (
    <div className="mx-auto w-full max-w-2xl px-6 py-16">
      <header className="mb-8">
        <h1 className="text-3xl text-ink-900">创建 Creator 档案</h1>
        <p className="mt-2 text-ink-500">公开名称与 slug 创建后会成为你的创作者主页标识。</p>
      </header>
      <CreatorApplicationForm />
    </div>
  );
}