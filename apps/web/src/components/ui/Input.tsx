import { forwardRef } from "react";
import type { InputHTMLAttributes, ReactNode } from "react";
import { cn } from "@/components/domain/types";

export interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label?: string;
  hint?: ReactNode;
  error?: string;
}

/**
 * 表单输入基础组件，统一关联 label、错误文本和无障碍属性，并透传 ref 给表单库。
 * 当 label 缺失或可能重复时，调用方应显式传 id，避免生成重复的关联标识。
 */
export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { label, hint, error, className, id, ...rest },
  ref
) {
  const inputId = id ?? `input-${label?.replace(/\s+/g, "-").toLowerCase() ?? "field"}`;
  return (
    <label htmlFor={inputId} className="flex flex-col gap-1.5 text-sm">
      {label ? <span className="text-ink-700">{label}</span> : null}
      <input
        ref={ref}
        id={inputId}
        className={cn(
          "h-11 rounded-token border border-ink-300 bg-white px-3 text-ink-900",
          "placeholder:text-ink-500",
          "focus-visible:border-ink-900 focus-visible:outline-none",
          error && "border-error",
          className
        )}
        aria-invalid={Boolean(error)}
        aria-describedby={error ? `${inputId}-error` : undefined}
        {...rest}
      />
      {error ? (
        <span id={`${inputId}-error`} className="text-error">
          {error}
        </span>
      ) : hint ? (
        <span className="text-ink-500">{hint}</span>
      ) : null}
    </label>
  );
});
