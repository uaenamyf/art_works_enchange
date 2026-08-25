"use client";

import { useId } from "react";
import type { ReactNode } from "react";
import * as TabsPrimitive from "@radix-ui/react-tabs";
import { cn } from "@/components/domain/types";

export interface TabItem {
  value: string;
  label: string;
  content: ReactNode;
}

export interface TabsProps {
  items: TabItem[];
  defaultValue?: string;
  className?: string;
}

export function Tabs({ items, defaultValue, className }: TabsProps) {
  const fallback = useId();
  const initial = defaultValue ?? items[0]?.value ?? fallback;
  return (
    <TabsPrimitive.Root defaultValue={initial} className={cn("w-full", className)}>
      <TabsPrimitive.List
        className="flex gap-1 border-b border-ink-300"
        aria-label="切换视图"
      >
        {items.map((item) => (
          <TabsPrimitive.Trigger
            key={item.value}
            value={item.value}
            className={cn(
              "px-4 py-2 text-sm text-ink-700",
              "border-b-2 border-transparent",
              "data-[state=active]:border-ink-900 data-[state=active]:text-ink-900"
            )}
          >
            {item.label}
          </TabsPrimitive.Trigger>
        ))}
      </TabsPrimitive.List>
      {items.map((item) => (
        <TabsPrimitive.Content
          key={item.value}
          value={item.value}
          className="py-4 focus-visible:outline-none"
        >
          {item.content}
        </TabsPrimitive.Content>
      ))}
    </TabsPrimitive.Root>
  );
}
