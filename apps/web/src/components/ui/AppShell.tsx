import * as React from "react";

import { cn } from "@/lib/cn";

type Tone = "blue" | "teal" | "amber" | "neutral";

const toneClass: Record<Tone, string> = {
  blue: "bg-[color:var(--color-accent-soft)] text-[color:var(--color-accent)]",
  teal: "bg-emerald-100 text-emerald-800",
  amber: "bg-amber-100 text-amber-800",
  neutral: "bg-slate-100 text-slate-700",
};

export function Badge({
  className,
  tone = "blue",
  ...props
}: React.HTMLAttributes<HTMLSpanElement> & { tone?: Tone }) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold tracking-[0.02em]",
        toneClass[tone],
        className,
      )}
      {...props}
    />
  );
}

export function Metric({
  label,
  value,
  className,
}: {
  label: string;
  value: string;
  className?: string;
}) {
  return (
    <div className={cn("soft-section rounded-2xl px-4 py-3", className)}>
      <div className="text-[11px] font-semibold uppercase tracking-[0.08em] text-[color:var(--color-muted)]">
        {label}
      </div>
      <div className="mt-1 font-[family-name:var(--font-display)] text-xl font-bold tracking-tight">
        {value}
      </div>
    </div>
  );
}

export function Hero({
  kicker,
  title,
  subtitle,
  action,
  children,
  className,
}: {
  kicker?: React.ReactNode;
  title: React.ReactNode;
  subtitle?: React.ReactNode;
  action?: React.ReactNode;
  children?: React.ReactNode;
  className?: string;
}) {
  return (
    <section
      className={cn(
        "glass-panel animate-rise-in overflow-hidden rounded-[28px] border px-6 py-8 sm:px-8 sm:py-10",
        className,
      )}
    >
      <div className="relative">
        <div className="absolute -right-24 -top-24 h-56 w-56 rounded-full bg-[color:var(--color-accent)]/15 blur-3xl" />
        <div className="absolute -bottom-24 -left-24 h-56 w-56 rounded-full bg-[color:var(--color-accent-2)]/15 blur-3xl" />
        <div className="relative">
          {kicker ? <div className="text-sm font-semibold text-[color:var(--color-accent)]">{kicker}</div> : null}
          <h1 className="mt-2 max-w-4xl font-[family-name:var(--font-display)] text-4xl font-extrabold tracking-tight sm:text-5xl">
            {title}
          </h1>
          {subtitle ? (
            <p className="mt-4 max-w-3xl text-base leading-7 text-[color:var(--color-muted)] sm:text-lg">{subtitle}</p>
          ) : null}
          {action ? <div className="mt-6">{action}</div> : null}
          {children ? <div className="mt-6">{children}</div> : null}
        </div>
      </div>
    </section>
  );
}

export function SectionHeader({
  title,
  subtitle,
  action,
  className,
}: {
  title: React.ReactNode;
  subtitle?: React.ReactNode;
  action?: React.ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("flex flex-wrap items-end justify-between gap-3", className)}>
      <div>
        <h2 className="font-[family-name:var(--font-display)] text-2xl font-bold tracking-tight">{title}</h2>
        {subtitle ? <p className="mt-1 text-sm text-[color:var(--color-muted)]">{subtitle}</p> : null}
      </div>
      {action ? <div>{action}</div> : null}
    </div>
  );
}
