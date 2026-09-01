import type { Metadata } from "next";
import type { ReactNode } from "react";
import { Inter, Source_Serif_4 } from "next/font/google";
import { AppShell } from "@/components/layout/AppShell";
import { APP_DESCRIPTION, APP_NAME } from "@/lib/config";
import { getServerLocale } from "@/features/locale/serverLocale";
import "@/styles/globals.css";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-sans",
  display: "swap"
});

const serif = Source_Serif_4({
  subsets: ["latin"],
  variable: "--font-serif",
  display: "swap"
});

export const metadata: Metadata = {
  title: {
    default: APP_NAME,
    template: `%s · ${APP_NAME}`
  },
  description: APP_DESCRIPTION
};

/** 设置中文文档语言、全局字体变量，并挂载站点公共壳层。 */
export default async function RootLayout({ children }: { children: ReactNode }) {
  const locale = await getServerLocale();
  return (
    <html lang={locale} className={`${inter.variable} ${serif.variable}`}>
      <body>
        <AppShell initialLocale={locale}>{children}</AppShell>
      </body>
    </html>
  );
}
