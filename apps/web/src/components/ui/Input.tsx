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
        "w-full rounded-xl bg-white/70 px-3 py-2 text-sm",
        "ring-1 ring-[color:var(--color-surface-border)]",
        "placeholder:text-[color:var(--color-muted-2)]",
        "focus:outline-none focus:ring-2 focus:ring-[color:var(--color-focus)]",
        className,
      )}
      {...props}
    />
  );
}

