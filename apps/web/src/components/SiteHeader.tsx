"use client";

import { useTranslations } from "next-intl";

import { Link, usePathname } from "@/i18n/navigation";
import { cn } from "@/lib/cn";
import { LocaleSwitcher } from "@/components/LocaleSwitcher";

export function SiteHeader() {
  const tNav = useTranslations("Nav");
  const pathname = usePathname();

  const items = [
    { href: "/verify", label: tNav("verify") },
    { href: "/shield", label: tNav("shield") },
    { href: "/help", label: tNav("help") },
    { href: "/partner", label: tNav("partner") },
  ] as const;

  return (
    <header className="sticky top-0 z-20 border-b border-black/5 bg-[color:var(--color-surface)]/80 backdrop-blur">
      <div className="mx-auto flex max-w-5xl items-center justify-between px-4 py-3 sm:px-6">
        <Link href="/" className="group flex items-baseline gap-2">
          <span className="font-[family-name:var(--font-display)] text-base font-semibold tracking-tight">
            RegShield
          </span>
          <span className="text-xs font-semibold text-[color:var(--color-accent)]">CA</span>
        </Link>

        <nav className="hidden items-center gap-1 sm:flex">
          {items.map((item) => {
            const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "rounded-xl px-3 py-2 text-sm font-medium transition",
                  active
                    ? "bg-white/70 ring-1 ring-[color:var(--color-surface-border)]"
                    : "text-[color:var(--color-muted)] hover:bg-white/60",
                )}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>

        <div className="flex items-center gap-2">
          <LocaleSwitcher />
        </div>
      </div>

      <div className="mx-auto max-w-5xl px-4 pb-3 sm:hidden sm:px-6">
        <nav className="flex flex-wrap gap-2">
          {items.map((item) => {
            const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "rounded-xl px-3 py-2 text-xs font-semibold transition",
                  active
                    ? "bg-white/70 ring-1 ring-[color:var(--color-surface-border)]"
                    : "text-[color:var(--color-muted)] hover:bg-white/60",
                )}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>
      </div>
    </header>
  );
}

