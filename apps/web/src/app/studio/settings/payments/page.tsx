"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";
import type { components } from "@/lib/api/schema.gen";

type PayPalAccount = components["schemas"]["PayPalAccount"];

/** Creator 收款设置；只保存 PayPal 入驻后的 merchant_id，不处理 PayPal 密钥。 */
export default function StudioPaymentSettingsPage() {
  const { session, loading, authenticatedFetch } = useAuth();
  const [account, setAccount] = useState<PayPalAccount | null>(null);
  const [merchantId, setMerchantId] = useState("");
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!session || session.user.role !== "CREATOR") return;
    void authenticatedFetch<PayPalAccount>("/api/v1/me/creator/paypal-account")
      .then((result) => {
        setAccount(result);
        setMerchantId(result.merchantId ?? "");
      })
      .catch((error: unknown) => {
        setLoadError(error instanceof ApiError ? error.problem.detail ?? "收款设置加载失败" : "收款设置加载失败");
      });
  }, [authenticatedFetch, session]);

  async function save() {
    setSaving(true);
    setSaved(false);
    setSaveError(null);
    try {
      const result = await authenticatedFetch<PayPalAccount>("/api/v1/me/creator/paypal-account", {
        method: "PUT",
        body: { merchantId: merchantId.trim() }
      });
      setAccount(result);
      setMerchantId(result.merchantId ?? "");
      setSaved(true);
    } catch (error: unknown) {
      setSaveError(error instanceof ApiError ? error.problem.detail ?? "收款账户保存失败" : "收款账户保存失败");
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <Message text="正在加载账户..." />;
  if (!session || session.user.role !== "CREATOR") return <Message text="只有 Creator 可以管理收款方式。" alert />;
  if (loadError) return <Message text={loadError} alert />;
  if (!account) return <Message text="正在加载收款设置..." />;

  return (
    <main>
      <header className="border-b border-ink-300 pb-6">
        <p className="text-xs uppercase tracking-[0.18em] text-brand-600">Studio / Payments</p>
        <h1 className="mt-2 font-serif text-4xl text-ink-900">收款方式</h1>
        <p className="mt-3 text-sm leading-6 text-ink-500">连接 PayPal Multiparty 入驻后分配的商户 ID。平台不会保存 PayPal 密码、Client Secret 或 Access Token。</p>
      </header>
      <section className="max-w-xl border-b border-ink-300 py-8">
        <p className="text-sm text-ink-700">PayPal 状态：{account.configured ? "已配置" : "未配置"}</p>
        <div className="mt-5 grid gap-4">
          <Input
            label="PayPal merchant ID"
            value={merchantId}
            onChange={(event) => setMerchantId(event.target.value)}
            maxLength={64}
            pattern="[A-Za-z0-9_-]+"
            hint="请从 PayPal 商户入驻流程或开发者后台获取 merchant_id，不要填写 PayPal 邮箱。"
          />
          {saveError ? <p role="alert" className="text-sm text-error">{saveError}</p> : null}
          {saved ? <p className="text-sm text-success">PayPal 收款方式已保存。</p> : null}
          <Button type="button" loading={saving} disabled={saving || !merchantId.trim()} onClick={() => void save()}>
            保存 PayPal 收款方式
          </Button>
        </div>
      </section>
    </main>
  );
}

function Message({ text, alert = false }: { text: string; alert?: boolean }) {
  return <main className="py-12"><p role={alert ? "alert" : undefined} className={alert ? "text-error" : "text-ink-500"}>{text}</p></main>;
}
