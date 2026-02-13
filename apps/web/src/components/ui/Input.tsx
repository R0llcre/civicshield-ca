"use client";

import * as React from "react";

import { cn } from "@/lib/cn";

export function Input({
  className,
  ...props
}: React.InputHTMLAttributes<HTMLInputElement>) {
  return (
    <input
      className={cn(
        "h-11 w-full rounded-2xl border bg-[color:var(--color-surface-strong)] px-3.5 text-sm text-[color:var(--color-foreground)]",
        "border-[color:var(--color-surface-border)] shadow-[var(--shadow-sm)]",
        "placeholder:text-[color:var(--color-muted-2)]",
        "focus:border-[color:var(--color-focus)] focus:outline-none focus:ring-2 focus:ring-[color:var(--color-focus)]/35",
        className,
      )}
      {...props}
    />
  );
}
