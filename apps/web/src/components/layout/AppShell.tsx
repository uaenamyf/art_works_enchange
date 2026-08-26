import type { ReactNode } from "react";
import { Navbar } from "@/components/layout/Navbar";
import { Footer } from "@/components/layout/Footer";
import { AuthProvider } from "@/features/auth/AuthProvider";

export interface AppShellProps {
  children: ReactNode;
}

/** 全站结构壳层，统一导航、主内容可伸缩区域和页脚。 */
export function AppShell({ children }: AppShellProps) {
  return (
    <AuthProvider>
      <div className="flex min-h-screen flex-col">
        <Navbar />
        <main className="flex-1">{children}</main>
        <Footer />
      </div>
    </AuthProvider>
  );
}
