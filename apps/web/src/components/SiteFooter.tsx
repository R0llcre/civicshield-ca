import { getTranslations } from "next-intl/server";

export async function SiteFooter() {
  const t = await getTranslations("Common");

  return (
    <footer className="border-t border-black/5">
      <div className="mx-auto max-w-5xl px-4 py-8 text-sm text-[color:var(--color-muted)] sm:px-6">
        <div className="font-semibold text-[color:var(--color-foreground)]">
          {t("informationOnlyTitle")}
        </div>
        <div className="mt-1 max-w-3xl">{t("informationOnlyBody")}</div>
      </div>
    </footer>
  );
}

