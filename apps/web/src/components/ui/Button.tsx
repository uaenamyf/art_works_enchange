"use client";

import { forwardRef } from "react";
import type { ButtonHTMLAttributes, ReactNode } from "react";
import { cn } from "@/components/domain/types";

type Variant = "primary" | "secondary" | "ghost" | "danger";
type Size = "sm" | "md" | "lg";

const variantClass: Record<Variant, string> = {
  primary:
    "bg-ink-900 text-white hover:bg-ink-700 focus-visible:bg-ink-700 disabled:bg-ink-300",
  secondary:
    "bg-white text-ink-900 border border-ink-300 hover:bg-ink-100 focus-visible:bg-ink-100",
  ghost: "bg-transparent text-ink-900 hover:bg-ink-100 focus-visible:bg-ink-100",
  danger: "bg-error text-white hover:opacity-90"
};

const sizeClass: Record<Size, string> = {
  sm: "h-9 px-3 text-sm",
  md: "h-11 px-4 text-sm",
  lg: "h-12 px-5 text-base"
};

export interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
  leftIcon?: ReactNode;
  rightIcon?: ReactNode;
}

export const Button = forwardRef<HTMLButtonElement, ButtonProps>(function Button(
  {
    variant = "primary",
    size = "md",
    loading = false,
    disabled,
    leftIcon,
    rightIcon,
    className,
    children,
    type = "button",
    ...rest
  },
  ref
) {
  return (
    <button
      ref={ref}
      type={type}
      disabled={disabled || loading}
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-token font-medium transition-colors",
        "disabled:cursor-not-allowed disabled:opacity-60",
        "focus-visible:outline-2 focus-visible:outline-offset-2",
        variantClass[variant],
        sizeClass[size],
        className
      )}
      {...rest}
    >
      {loading ? <span aria-hidden className="size-3 animate-pulse rounded-full bg-current" /> : leftIcon}
      <span>{children}</span>
      {rightIcon}
    </button>
  );
});
