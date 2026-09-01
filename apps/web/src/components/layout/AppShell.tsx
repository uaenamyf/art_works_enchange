import type { ReactNode } from "react";
import { Navbar } from "@/components/layout/Navbar";
import { Footer } from "@/components/layout/Footer";
import { AuthProvider } from "@/features/auth/AuthProvider";
import { QueryProvider } from "@/providers/QueryProvider";
import { LocaleProvider, type Locale } from "@/features/locale/LocaleProvider";

export interface AppShellProps {
  children: ReactNode;
  initialLocale?: Locale;
}

/** 全站结构壳层，统一导航、主内容可伸缩区域和页脚。 */
export function AppShell({ children, initialLocale = "zh-CN" }: AppShellProps) {
  return (
    <QueryProvider>
      <LocaleProvider initialLocale={initialLocale}>
        <AuthProvider>
          <div className="flex min-h-screen flex-col">
            <Navbar />
            <main className="flex-1">{children}</main>
            <Footer />
          </div>
        </AuthProvider>
      </LocaleProvider>
    </QueryProvider>
  );
}
