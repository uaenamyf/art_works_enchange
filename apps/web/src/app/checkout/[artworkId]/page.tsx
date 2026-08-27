"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { startTransition, useEffect, useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";
import { z } from "zod";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { ApiError, apiFetch } from "@/lib/api/fetcher";
import { ROUTES } from "@/lib/config";
import { useAuth } from "@/features/auth/AuthProvider";

const schema = z.object({
  quantity: z.coerce.number().int().min(1, "至少购买 1 件").max(99, "最多购买 99 件"),
  recipientName: z.string().min(1, "请输入收货人").max(100),
  line1: z.string().min(1, "请输入地址").max(200),
  city: z.string().min(1, "请输入城市").max(100),
  postalCode: z.string().min(1, "请输入邮编").max(20),
  countryCode: z.string().regex(/^[A-Z]{2}$/, "请输入两位国家代码"),
  phone: z.string().min(6, "请输入有效手机号").max(30)
});

type CheckoutValues = z.infer<typeof schema>;

interface ArtworkSnapshot {
  id: string;
  title: string;
  priceAmount: string;
  priceCurrency: string;
  stock: number;
  status: string;
  productType?: "PHYSICAL" | "DIGITAL";
  digitalAsset?: { fileName: string; contentType: string; sizeBytes: number; version: number } | null;
}

interface OrderResponse { id: string; status: string }

type ProductType = "PHYSICAL" | "DIGITAL";

/** 结算页只提交数量和收货地址，价格由后端作品快照决定。 */
export default function CheckoutPage({ params }: { params: Promise<{ artworkId: string }> }) {
  const router = useRouter();
  const { session, loading: authLoading, authenticatedFetch } = useAuth();
  const [artworkId, setArtworkId] = useState<string | null>(null);
  const [artwork, setArtwork] = useState<ArtworkSnapshot | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<CheckoutValues>({
    resolver: zodResolver(schema),
    defaultValues: { quantity: 1, countryCode: "CN" }
  });

  useEffect(() => {
    void params.then(({ artworkId: id }) => {
      setArtworkId(id);
      void apiFetch<ArtworkSnapshot>(`/api/v1/artworks/${id}`)
        .then(setArtwork)
        .catch(() => setLoadError("作品加载失败，请返回详情页重试"));
    });
  }, [params]);

  async function submit(values: CheckoutValues) {
    if (!session || !artworkId) return;
    setSubmitError(null);
    try {
      const order = await authenticatedFetch<OrderResponse>("/api/v1/orders", {
        method: "POST",
        headers: { "Idempotency-Key": crypto.randomUUID() },
        body: {
          artworkId,
          quantity: values.quantity,
          ...(productType === "DIGITAL"
            ? {}
            : {
                shippingAddress: {
                  recipientName: values.recipientName,
                  line1: values.line1,
                  line2: null,
                  city: values.city,
                  region: null,
                  postalCode: values.postalCode,
                  countryCode: values.countryCode,
                  phone: values.phone
                }
              })
        }
      });
      startTransition(() => router.push(`/orders/${order.id}`));
    } catch (error) {
      setSubmitError(error instanceof ApiError ? error.problem.detail ?? "订单创建失败，请稍后重试" : "订单创建失败，请稍后重试");
    }
  }

  if (authLoading) return <PageMessage message="正在加载账户..." />;
  if (!session) return <PageMessage message="请先登录后再结算。" action={<Link href={ROUTES.authLogin} className="mt-5 inline-flex h-11 items-center rounded-token bg-ink-900 px-5 text-sm text-white">去登录</Link>} />;
  if (loadError) return <PageMessage message={loadError} alert />;
  if (!artwork) return <PageMessage message="正在加载作品..." />;
  const productType: ProductType = artwork.productType ?? "PHYSICAL";
  if (artwork.status !== "PUBLISHED" || (productType === "PHYSICAL" && artwork.stock < 1)) return <PageMessage message="这件作品目前无法购买。" alert />;

  return (
    <main className="mx-auto w-full max-w-gallery px-6 py-12">
      <header className="mb-8 border-b border-ink-300 pb-6">
        <Link href={`/artworks/${artwork.id}`} className="text-sm text-ink-500 hover:text-ink-900">返回作品</Link>
        <p className="mt-5 text-xs uppercase tracking-[0.2em] text-brand-500">CreatorHub / 结算</p>
        <h1 className="mt-3 text-4xl text-ink-900">确认订单</h1>
        <p className="mt-3 text-ink-700">{artwork.title} · {artwork.priceAmount} {artwork.priceCurrency}</p>
      </header>
      <form onSubmit={handleSubmit(submit)} noValidate className="grid gap-8 lg:grid-cols-[1fr_320px]">
        {productType === "DIGITAL" ? (
          <section className="rounded-token border border-ink-300 bg-white p-6">
            <p className="text-xs uppercase tracking-[0.18em] text-brand-500">数字交付</p>
            <h2 className="mt-2 text-xl text-ink-900">支付确认后即可访问</h2>
            <p className="mt-3 text-sm leading-6 text-ink-500">无需填写收货地址。订单支付确认后，授权文件会出现在订单详情中。</p>
            {artwork.digitalAsset ? <p className="mt-5 text-sm text-ink-700">{artwork.digitalAsset.fileName} · {artwork.digitalAsset.contentType} · 版本 {artwork.digitalAsset.version}</p> : null}
            <input type="hidden" value="1" {...register("quantity")} />
          </section>
        ) : (
          <section className="grid gap-4 rounded-token border border-ink-300 bg-white p-6 sm:grid-cols-2">
            <h2 className="sm:col-span-2 text-xl text-ink-900">收货信息</h2>
            <Input label="购买数量" type="number" min={1} max={Math.min(99, artwork.stock)} error={errors.quantity?.message} {...register("quantity")} />
            <Input label="收货人" autoComplete="name" error={errors.recipientName?.message} {...register("recipientName")} />
            <Input label="地址" autoComplete="street-address" error={errors.line1?.message} {...register("line1")} />
            <Input label="城市" autoComplete="address-level2" error={errors.city?.message} {...register("city")} />
            <Input label="邮编" autoComplete="postal-code" error={errors.postalCode?.message} {...register("postalCode")} />
            <Input label="国家代码" maxLength={2} error={errors.countryCode?.message} {...register("countryCode")} />
            <Input label="手机号" type="tel" autoComplete="tel" error={errors.phone?.message} {...register("phone")} />
          </section>
        )}
        <aside className="h-fit rounded-token border border-ink-300 bg-white p-6">
          <h2 className="text-xl text-ink-900">订单摘要</h2>
          <p className="mt-4 text-sm text-ink-500">{productType === "DIGITAL" ? "支付确认后创建购买授权。" : "库存将在订单创建事务中确认，订单状态为待支付。"}</p>
          {submitError ? <p role="alert" className="mt-4 text-sm text-error">{submitError}</p> : null}
          <Button type="submit" size="lg" loading={isSubmitting} className="mt-6 w-full">{productType === "DIGITAL" ? "创建数字订单" : "创建待支付订单"}</Button>
        </aside>
      </form>
    </main>
  );
}

function PageMessage({ message, alert = false, action }: { message: string; alert?: boolean; action?: React.ReactNode }) {
  return <main className="mx-auto w-full max-w-gallery px-6 py-16"><p role={alert ? "alert" : undefined} className={alert ? "text-error" : "text-ink-500"}>{message}</p>{action}</main>;
}