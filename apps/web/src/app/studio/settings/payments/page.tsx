"use client";

import { useEffect, useState } from "react";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { ApiError } from "@/lib/api/fetcher";
import { useAuth } from "@/features/auth/AuthProvider";
import { hasCreatorWorkspaceAccess } from "@/features/auth/types";
import { useLocale } from "@/features/locale/LocaleProvider";
import type { components } from "@/lib/api/schema.gen";

type PayPalAccount = components["schemas"]["PayPalAccount"];

/** Creator 收款设置；只保存 PayPal 入驻后的 merchant_id，不处理 PayPal 密钥。 */
export default function StudioPaymentSettingsPage() {
  const { session, loading, authenticatedFetch } = useAuth();
  const { t } = useLocale();
  const [account, setAccount] = useState<PayPalAccount | null>(null);
  const [merchantId, setMerchantId] = useState("");
  const [loadError, setLoadError] = useState<string | null>(null);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!hasCreatorWorkspaceAccess(session)) return;
    void authenticatedFetch<PayPalAccount>("/api/v1/me/creator/paypal-account")
      .then((result) => {
        setAccount(result);
        setMerchantId(result.merchantId ?? "");
      })
      .catch((error: unknown) => {
        setLoadError(error instanceof ApiError ? error.problem.detail ?? t("artworksLoadFailed") : t("artworksLoadFailed"));
      });
  }, [authenticatedFetch, session, t]);

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
      setSaveError(error instanceof ApiError ? error.problem.detail ?? t("saveError") : t("saveNetworkError"));
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <Message text={t("loadingAccount")} />;
  if (!hasCreatorWorkspaceAccess(session)) return <Message text={t("creatorOnlyPayments")} alert />;
  if (loadError) return <Message text={loadError} alert />;
  if (!account) return <Message text={t("paypalSettingsLoading")} />;

  return (
    <main>
      <header className="border-b border-ink-300 pb-6">
        <p className="text-xs uppercase tracking-[0.18em] text-brand-600">Studio / Payments</p>
        <h1 className="mt-2 font-serif text-4xl text-ink-900">{t("paypalSettings")}</h1>
        <p className="mt-3 text-sm leading-6 text-ink-500">{t("paypalDescription")}</p>
      </header>
      <section className="max-w-xl border-b border-ink-300 py-8">
        <p className="text-sm text-ink-700">{t("paypalStatus")}: {account.configured ? t("configured") : t("notConfigured")}</p>
        <div className="mt-5 grid gap-4">
          <Input
            label="PayPal merchant ID"
            value={merchantId}
            onChange={(event) => setMerchantId(event.target.value)}
            maxLength={64}
            pattern="[A-Za-z0-9_-]+"
            hint={t("paypalMerchantHint")}
          />
          {saveError ? <p role="alert" className="text-sm text-error">{saveError}</p> : null}
          {saved ? <p className="text-sm text-success">{t("paypalSaved")}</p> : null}
          <Button type="button" loading={saving} disabled={saving || !merchantId.trim()} onClick={() => void save()}>
            {t("savePaypal")}
          </Button>
        </div>
      </section>
    </main>
  );
}

function Message({ text, alert = false }: { text: string; alert?: boolean }) {
  return <main className="py-12"><p role={alert ? "alert" : undefined} className={alert ? "text-error" : "text-ink-500"}>{text}</p></main>;
}
