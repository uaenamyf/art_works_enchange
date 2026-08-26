"use client";

import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { apiFetch, type ApiFetchOptions } from "@/lib/api/fetcher";
import type { AuthSession } from "./types";

interface AuthContextValue {
  session: AuthSession | null;
  loading: boolean;
  setSession: (session: AuthSession) => void;
  refresh: () => Promise<AuthSession | null>;
  logout: () => Promise<void>;
  authenticatedFetch: <T>(path: string, options?: ApiFetchOptions) => Promise<T>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

/**
 * 仅在浏览器内存维护 Access Token，并通过 HttpOnly Refresh Cookie 恢复会话。
 *
 * <p>页面刷新会丢失内存 Token，这是刻意的安全取舍；Provider 随后调用 refresh 轮换 Cookie
 * 并恢复 Access Token。任何 Token 都不会写入 Web Storage。
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSessionState] = useState<AuthSession | null>(null);
  const [loading, setLoading] = useState(true);

  async function refresh(): Promise<AuthSession | null> {
    try {
      const restored = await apiFetch<AuthSession>("/api/v1/auth/refresh", { method: "POST" });
      setSessionState(restored);
      return restored;
    } catch {
      setSessionState(null);
      return null;
    }
  }

  useEffect(() => {
    void refresh().finally(() => setLoading(false));
  }, []);

  async function logout() {
    await apiFetch<void>("/api/v1/auth/logout", { method: "POST" });
    setSessionState(null);
  }

  async function authenticatedFetch<T>(path: string, options: ApiFetchOptions = {}) {
    if (!session?.accessToken) {
      throw new Error("Authentication is required");
    }
    return apiFetch<T>(path, {
      ...options,
      headers: { ...options.headers, Authorization: `Bearer ${session.accessToken}` }
    });
  }

  return (
    <AuthContext.Provider
      value={{
        session,
        loading,
        setSession: setSessionState,
        refresh,
        logout,
        authenticatedFetch
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

/** 读取认证上下文；只允许在 AuthProvider 子树内使用。 */
export function useAuth(): AuthContextValue {
  const value = useContext(AuthContext);
  if (!value) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return value;
}
