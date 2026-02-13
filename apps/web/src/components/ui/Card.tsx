import * as React from "react";

import { cn } from "@/lib/cn";

type CardVariant = "elevated" | "flat" | "highlight";

export function Card({
  className,
  variant = "elevated",
  ...props
}: React.HTMLAttributes<HTMLDivElement> & { variant?: CardVariant }) {
  return (
    <div
      className={cn(
        "rounded-[var(--radius-lg)] border",
        variant === "elevated" &&
          "glass-panel border-[color:var(--color-surface-border)] shadow-[var(--shadow-md)]",
        variant === "flat" &&
          "bg-[color:var(--color-surface-strong)] border-[color:var(--color-surface-border)] shadow-[var(--shadow-sm)]",
        variant === "highlight" &&
          "bg-[color:var(--color-surface-strong)] border-[color:var(--color-accent)]/35 shadow-[var(--shadow-lg)]",
        className,
      )}
      {...props}
    />
  );
}

export function CardHeader({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("p-5 pb-0 sm:p-6 sm:pb-0", className)} {...props} />;
}

export function CardContent({
  className,
  ...props
}: React.HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("p-5 sm:p-6", className)} {...props} />;
}
