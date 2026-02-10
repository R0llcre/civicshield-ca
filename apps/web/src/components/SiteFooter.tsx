import { getTranslations } from "next-intl/server";

import { Link } from "@/i18n/navigation";

export async function SiteFooter() {
  const t = await getTranslations("Common");

  return (
    <footer className="border-t border-black/5">
      <div className="mx-auto max-w-5xl px-4 py-8 text-sm text-[color:var(--color-muted)] sm:px-6">
        <div className="font-semibold text-[color:var(--color-foreground)]">
          {t("informationOnlyTitle")}
        </div>
        <div className="mt-1 max-w-3xl">{t("informationOnlyBody")}</div>

        <div className="mt-4 flex flex-wrap gap-3 text-xs font-semibold">
          <Link href="/about" className="text-[color:var(--color-accent)] hover:underline">
            {t("links.about")}
          </Link>
          <Link href="/privacy" className="text-[color:var(--color-accent)] hover:underline">
            {t("links.privacy")}
          </Link>
          <Link href="/disclaimer" className="text-[color:var(--color-accent)] hover:underline">
            {t("links.disclaimer")}
          </Link>
        </div>
      </div>
    </footer>
  );
}
