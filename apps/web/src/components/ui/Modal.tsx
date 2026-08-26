"use client";

import type { ReactNode } from "react";
import * as DialogPrimitive from "@radix-ui/react-dialog";
import { X } from "lucide-react";
import { cn } from "@/components/domain/types";

export interface ModalProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  description?: string;
  children: ReactNode;
  footer?: ReactNode;
}

/**
 * 受控模态框组件；打开状态由调用方持有，焦点锁定、Escape 关闭和语义关联委托给 Radix。
 * 业务代码应通过 onOpenChange 更新状态，不直接操纵 DOM。
 */
export function Modal({ open, onOpenChange, title, description, children, footer }: ModalProps) {
  return (
    <DialogPrimitive.Root open={open} onOpenChange={onOpenChange}>
      <DialogPrimitive.Portal>
        <DialogPrimitive.Overlay className="fixed inset-0 z-40 bg-ink-900/40 backdrop-blur-sm" />
        <DialogPrimitive.Content
          className={cn(
            "fixed left-1/2 top-1/2 z-50 w-[min(560px,92vw)] -translate-x-1/2 -translate-y-1/2",
            "rounded-token bg-white p-6 shadow-pop focus-visible:outline-none"
          )}
        >
          <header className="mb-4 flex items-start justify-between gap-4">
            <div>
              <DialogPrimitive.Title className="text-lg text-ink-900">
                {title}
              </DialogPrimitive.Title>
              {description ? (
                <DialogPrimitive.Description className="mt-1 text-sm text-ink-500">
                  {description}
                </DialogPrimitive.Description>
              ) : null}
            </div>
            <DialogPrimitive.Close
              aria-label="关闭"
              className="touch-target inline-flex items-center justify-center rounded-token text-ink-500 hover:bg-ink-100"
            >
              <X aria-hidden className="size-4" />
            </DialogPrimitive.Close>
          </header>
          <div className="text-sm text-ink-700">{children}</div>
          {footer ? <footer className="mt-6 flex justify-end gap-2">{footer}</footer> : null}
        </DialogPrimitive.Content>
      </DialogPrimitive.Portal>
    </DialogPrimitive.Root>
  );
}
