import { cookies } from "next/headers";
import type { Locale } from "./LocaleProvider";

/** Server Component 使用的语言读取入口，与客户端 ch_locale Cookie 保持同一白名单。 */
export async function getServerLocale(): Promise<Locale> {
  return (await cookies()).get("ch_locale")?.value === "en-US" ? "en-US" : "zh-CN";
}
