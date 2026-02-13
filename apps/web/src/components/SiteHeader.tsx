"use client";

import { useTranslations } from "next-intl";

import { Link, usePathname } from "@/i18n/navigation";
import { cn } from "@/lib/cn";
import { LocaleSwitcher } from "@/components/LocaleSwitcher";
import { Badge } from "@/components/ui/AppShell";

export function SiteHeader() {
  const tNav = useTranslations("Nav");
  const tApp = useTranslations("App");
  const pathname = usePathname();

  const items = [
    { href: "/verify", label: tNav("verify") },
    { href: "/shield", label: tNav("shield") },
    { href: "/help", label: tNav("help") },
    { href: "/partner", label: tNav("partner") },
  ] as const;

  return (
    <header className="sticky top-0 z-30 border-b border-[color:var(--color-surface-border)]/80 bg-white/70 backdrop-blur-xl">
      <div className="mx-auto flex w-full max-w-6xl items-center justify-between gap-4 px-4 py-3 sm:px-6 lg:px-8">
        <Link href="/" className="group min-w-0">
          <div className="flex items-center gap-2">
            <span className="font-[family-name:var(--font-display)] text-lg font-extrabold tracking-tight text-[color:var(--color-accent)] transition group-hover:brightness-110">
              RegShield
            </span>
            <Badge tone="amber" className="px-2.5 py-0.5 text-[10px]">
              CA
            </Badge>
          </div>
          <div className="mt-1 hidden max-w-[360px] truncate text-xs text-[color:var(--color-muted)] sm:block">
            {tApp("tagline")}
          </div>
        </Link>

        <nav className="hidden items-center gap-1.5 md:flex">
          {items.map((item) => {
            const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "rounded-2xl border px-3 py-2 text-sm font-semibold transition",
                  active
                    ? "border-[color:var(--color-accent)]/35 bg-[color:var(--color-accent-soft)] text-[color:var(--color-accent)] shadow-[var(--shadow-sm)]"
                    : "border-transparent text-[color:var(--color-muted)] hover:border-[color:var(--color-surface-border)] hover:bg-white/75 hover:text-[color:var(--color-foreground)]",
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

      <div className="mx-auto w-full max-w-6xl px-4 pb-3 sm:px-6 md:hidden lg:px-8">
        <nav className="flex gap-2 overflow-x-auto pb-1">
          {items.map((item) => {
            const active = pathname === item.href || pathname.startsWith(`${item.href}/`);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  "shrink-0 rounded-2xl border px-3 py-2 text-xs font-semibold transition",
                  active
                    ? "border-[color:var(--color-accent)]/35 bg-[color:var(--color-accent-soft)] text-[color:var(--color-accent)]"
                    : "border-transparent bg-white/40 text-[color:var(--color-muted)]",
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
