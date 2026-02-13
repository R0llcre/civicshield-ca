"use client";

import * as DropdownMenu from "@radix-ui/react-dropdown-menu";
import { useLocale } from "next-intl";
import { useTranslations } from "next-intl";
import { Check, Languages } from "lucide-react";

import { routing, type AppLocale } from "@/i18n/routing";
import { usePathname, useRouter } from "@/i18n/navigation";
import { cn } from "@/lib/cn";

const LOCALE_LABEL: Record<AppLocale, string> = {
  en: "EN",
  es: "ES",
  "zh-Hans": "中文",
};

export function LocaleSwitcher() {
  const t = useTranslations("Common");
  const router = useRouter();
  const pathname = usePathname();
  const locale = useLocale() as AppLocale;

  return (
    <DropdownMenu.Root>
      <DropdownMenu.Trigger
        className={cn(
          "inline-flex h-10 items-center gap-2 rounded-2xl border border-[color:var(--color-surface-border)]",
          "bg-white/75 px-3 text-sm font-semibold shadow-[var(--shadow-sm)] hover:bg-white",
        )}
        aria-label={t("switchLanguage")}
      >
        <Languages className="h-4 w-4" />
        <span>{LOCALE_LABEL[locale] ?? locale}</span>
      </DropdownMenu.Trigger>
      <DropdownMenu.Portal>
        <DropdownMenu.Content
          align="end"
          sideOffset={8}
          className={cn(
            "min-w-40 rounded-2xl border border-[color:var(--color-surface-border)] bg-white/95 p-1.5 shadow-[var(--shadow-md)]",
            "backdrop-blur",
          )}
        >
          {routing.locales.map((l) => {
            const isActive = l === locale;
            return (
              <DropdownMenu.Item
                key={l}
                className={cn(
                  "flex cursor-pointer items-center justify-between gap-3 rounded-xl px-3 py-2 text-sm font-semibold",
                  "outline-none hover:bg-[color:var(--color-accent-soft)]",
                )}
                onSelect={() => router.replace(pathname, { locale: l })}
              >
                <span className="font-semibold">{LOCALE_LABEL[l] ?? l}</span>
                {isActive ? <Check className="h-4 w-4 text-[color:var(--color-accent)]" /> : null}
              </DropdownMenu.Item>
            );
          })}
        </DropdownMenu.Content>
      </DropdownMenu.Portal>
    </DropdownMenu.Root>
  );
}
