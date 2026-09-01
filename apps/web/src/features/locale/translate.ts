import enCommon from "../../../locales/en/common.json";
import zhCommon from "../../../locales/zh-CN/common.json";
import type { Locale } from "./LocaleProvider";

export const messages = {
  "zh-CN": zhCommon,
  "en-US": enCommon
} as const;

export type MessageKey = keyof typeof zhCommon | "navbar.home" | "navbar.about" | "greeting" | `categories.${keyof typeof zhCommon.categories}` | `productTypes.${keyof typeof zhCommon.productTypes}`;

/** 从语言词典读取单层或点号路径文本，并安全处理插值参数。 */
export function translate(locale: Locale, key: MessageKey, variables?: Record<string, string | number>): string {
  const value = key.split(".").reduce<unknown>((current, segment) => {
    if (!current || typeof current !== "object") return undefined;
    return (current as Record<string, unknown>)[segment];
  }, messages[locale]);
  const text = typeof value === "string" ? value : key;
  return variables ? Object.entries(variables).reduce((result, [name, replacement]) => result.replaceAll(`{${name}}`, String(replacement)), text) : text;
}
