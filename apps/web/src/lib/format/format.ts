/**
 * 货币与日期格式化工具。统一在 format 工具下集中，避免业务组件内散落 Intl/正则。
 */

const DEFAULT_LOCALE = "zh-CN";

export function formatPrice(amountCents: number, currency: string): string {
  const formatter = new Intl.NumberFormat(DEFAULT_LOCALE, {
    style: "currency",
    currency
  });
  return formatter.format(amountCents / 100);
}

export function formatDate(iso: string): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) {
    return iso;
  }
  return new Intl.DateTimeFormat(DEFAULT_LOCALE, {
    year: "numeric",
    month: "long",
    day: "numeric"
  }).format(d);
}

export function formatRelative(iso: string, now: Date = new Date()): string {
  const d = new Date(iso);
  if (Number.isNaN(d.getTime())) {
    return iso;
  }
  const diff = (now.getTime() - d.getTime()) / 1000;
  const rtf = new Intl.RelativeTimeFormat(DEFAULT_LOCALE, { numeric: "auto" });
  if (diff < 60) {
    return rtf.format(-Math.round(diff), "second");
  }
  if (diff < 3600) {
    return rtf.format(-Math.round(diff / 60), "minute");
  }
  if (diff < 86400) {
    return rtf.format(-Math.round(diff / 3600), "hour");
  }
  if (diff < 86400 * 30) {
    return rtf.format(-Math.round(diff / 86400), "day");
  }
  return formatDate(iso);
}
