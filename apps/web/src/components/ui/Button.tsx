"use client";

import * as React from "react";

import { cn } from "@/lib/cn";

type Variant = "primary" | "secondary" | "ghost";

export function Button({
  className,
  variant = "primary",
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant }) {
  return (
    <button
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-xl px-4 py-2 text-sm font-medium transition",
        "disabled:opacity-50 disabled:pointer-events-none",
        variant === "primary" &&
          "bg-[color:var(--color-accent)] text-white shadow-sm hover:brightness-[1.03] active:brightness-95",
        variant === "secondary" &&
          "bg-[color:var(--color-surface)] text-[color:var(--color-foreground)] ring-1 ring-[color:var(--color-surface-border)] hover:bg-white",
        variant === "ghost" &&
          "bg-transparent text-[color:var(--color-foreground)] hover:bg-white/60 ring-1 ring-transparent hover:ring-[color:var(--color-surface-border)]",
        className,
      )}
      {...props}
    />
  );
}

