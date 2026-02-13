import { getTranslations } from "next-intl/server";

import { Link } from "@/i18n/navigation";
import { Badge } from "@/components/ui/AppShell";

export async function SiteFooter() {
  const t = await getTranslations("Common");

  return (
    <footer className="border-t border-[color:var(--color-surface-border)]/80 bg-white/45">
      <div className="mx-auto w-full max-w-6xl px-4 py-8 text-sm text-[color:var(--color-muted)] sm:px-6 lg:px-8">
        <div className="flex flex-wrap items-center gap-2">
          <Badge tone="blue">RegShield CA</Badge>
          <span className="font-semibold text-[color:var(--color-foreground)]">
            {t("informationOnlyTitle")}
          </span>
        </div>
        <div className="mt-2 max-w-3xl leading-6">{t("informationOnlyBody")}</div>

        <div className="mt-4 flex flex-wrap gap-2 text-xs font-semibold">
          <Link
            href="/about"
            className="rounded-full border border-[color:var(--color-surface-border)] bg-white/70 px-3 py-1.5 text-[color:var(--color-accent)] hover:bg-white"
          >
            {t("links.about")}
          </Link>
          <Link
            href="/privacy"
            className="rounded-full border border-[color:var(--color-surface-border)] bg-white/70 px-3 py-1.5 text-[color:var(--color-accent)] hover:bg-white"
          >
            {t("links.privacy")}
          </Link>
          <Link
            href="/disclaimer"
            className="rounded-full border border-[color:var(--color-surface-border)] bg-white/70 px-3 py-1.5 text-[color:var(--color-accent)] hover:bg-white"
          >
            {t("links.disclaimer")}
          </Link>
        </div>

        <div className="mt-6 text-xs text-[color:var(--color-muted)]">RegShield CA</div>
      </div>
    </footer>
  );
}
