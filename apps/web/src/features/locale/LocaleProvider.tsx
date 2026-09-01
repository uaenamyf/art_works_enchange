"use client";

import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { translate, type MessageKey } from "./translate";

export type Locale = "zh-CN" | "en-US";

interface LocaleContextValue {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  toggleLocale: () => void;
  t: (key: MessageKey, variables?: Record<string, string | number>) => string;
}

const LocaleContext = createContext<LocaleContextValue | null>(null);

function readLocale(): Locale {
  if (typeof document === "undefined") return "zh-CN";
  return document.cookie.split(";").map((item) => item.trim()).find((item) => item.startsWith("ch_locale="))?.split("=")[1] === "en-US" ? "en-US" : "zh-CN";
}

/** 全局语言状态；只保存白名单 locale 到非敏感 Cookie，不影响认证 Cookie。 */
export function LocaleProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>("zh-CN");

  useEffect(() => {
    setLocaleState(readLocale());
  }, []);

  useEffect(() => {
    document.documentElement.lang = locale;
    document.cookie = `ch_locale=${locale}; Max-Age=31536000; Path=/; SameSite=Lax`;
  }, [locale]);

  const setLocale = (nextLocale: Locale) => {
    setLocaleState(nextLocale);
    document.cookie = `ch_locale=${nextLocale}; Max-Age=31536000; Path=/; SameSite=Lax`;
    window.location.reload();
  };

  const value = useMemo<LocaleContextValue>(() => ({
    locale,
    setLocale,
    toggleLocale: () => setLocale(locale === "zh-CN" ? "en-US" : "zh-CN"),
    t: (key, variables) => {
      return translate(locale, key, variables);
    }
  }), [locale]);

  return <LocaleContext.Provider value={value}>{children}</LocaleContext.Provider>;
}

export function useLocale(): LocaleContextValue {
  const value = useContext(LocaleContext);
  if (!value) throw new Error("useLocale must be used within LocaleProvider");
  return value;
}
