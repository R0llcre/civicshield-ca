"use client";

import * as React from "react";

import { cn } from "@/lib/cn";

type Variant = "primary" | "secondary" | "ghost";
type Size = "sm" | "md" | "lg";
type Tone = "accent" | "teal" | "amber";

export function Button({
  className,
  variant = "primary",
  size = "md",
  tone = "accent",
  ...props
}: React.ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
  size?: Size;
  tone?: Tone;
}) {
  return (
    <button
      className={cn(
        "inline-flex items-center justify-center gap-2 rounded-2xl border text-sm font-semibold tracking-[0.01em] transition",
        "disabled:opacity-50 disabled:pointer-events-none",
        size === "sm" && "h-9 px-3 text-xs",
        size === "md" && "h-10 px-4 text-sm",
        size === "lg" && "h-12 px-5 text-[15px]",
        variant === "primary" &&
          cn(
            "text-white shadow-[var(--shadow-sm)]",
            tone === "accent" &&
              "border-transparent bg-[color:var(--color-accent)] hover:brightness-105 active:brightness-95",
            tone === "teal" &&
              "border-transparent bg-[color:var(--color-accent-2)] hover:brightness-105 active:brightness-95",
            tone === "amber" &&
              "border-transparent bg-[color:var(--color-accent-3)] hover:brightness-105 active:brightness-95",
          ),
        variant === "secondary" &&
          "border-[color:var(--color-surface-border)] bg-[color:var(--color-surface-strong)] text-[color:var(--color-foreground)] hover:border-[color:var(--color-surface-border-strong)] hover:bg-white",
        variant === "ghost" &&
          "border-transparent bg-transparent text-[color:var(--color-foreground)] hover:border-[color:var(--color-surface-border)] hover:bg-white/70",
        className,
      )}
      {...props}
    />
  );
}
