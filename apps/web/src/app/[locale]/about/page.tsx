import { ExternalLink } from "lucide-react";
import { getTranslations } from "next-intl/server";

import { Badge, Hero } from "@/components/ui/AppShell";

export default async function AboutPage() {
  const t = await getTranslations("About");
  const tCommon = await getTranslations("Common");

  return (
    <div className="space-y-6 sm:space-y-8">
      <Hero
        kicker={<Badge tone="blue">{t("title")}</Badge>}
        title={t("title")}
        subtitle={t("body")}
        className="py-7 sm:py-8"
      />

      <section className="space-y-3">
        <h2 className="font-[family-name:var(--font-display)] text-xl font-semibold tracking-tight">
          {t("sourcesTitle")}
        </h2>

        <div className="grid gap-3 sm:grid-cols-2">
          <div className="soft-section rounded-2xl p-4">
            <div className="font-semibold">{t("sources.doj.title")}</div>
            <div className="mt-1 text-sm text-[color:var(--color-muted)]">{t("sources.doj.body")}</div>
            <a
              className="mt-3 inline-flex items-center gap-2 rounded-full border border-[color:var(--color-surface-border)] bg-white px-3 py-1 text-sm font-semibold text-[color:var(--color-accent)] hover:bg-[color:var(--color-accent-soft)]"
              href="https://www.justice.gov/eoir/recognition-accreditation-roster-reports"
              target="_blank"
              rel="noreferrer"
            >
              {tCommon("open")}
              <ExternalLink className="h-4 w-4" />
            </a>
          </div>

          <div className="soft-section rounded-2xl p-4">
            <div className="font-semibold">{t("sources.calbar.title")}</div>
            <div className="mt-1 text-sm text-[color:var(--color-muted)]">{t("sources.calbar.body")}</div>
            <a
              className="mt-3 inline-flex items-center gap-2 rounded-full border border-[color:var(--color-surface-border)] bg-white px-3 py-1 text-sm font-semibold text-[color:var(--color-accent)] hover:bg-[color:var(--color-accent-soft)]"
              href="https://apps.calbar.ca.gov/attorney/LicenseeSearch/QuickSearch"
              target="_blank"
              rel="noreferrer"
            >
              {tCommon("open")}
              <ExternalLink className="h-4 w-4" />
            </a>
          </div>

          <div className="soft-section rounded-2xl p-4 sm:col-span-2">
            <div className="font-semibold">{t("sources.sos.title")}</div>
            <div className="mt-1 text-sm text-[color:var(--color-muted)]">{t("sources.sos.body")}</div>
            <a
              className="mt-3 inline-flex items-center gap-2 rounded-full border border-[color:var(--color-surface-border)] bg-white px-3 py-1 text-sm font-semibold text-[color:var(--color-accent)] hover:bg-[color:var(--color-accent-soft)]"
              href="https://specialfilings.sos.ca.gov/icbs"
              target="_blank"
              rel="noreferrer"
            >
              {tCommon("open")}
              <ExternalLink className="h-4 w-4" />
            </a>
          </div>
        </div>
      </section>

      <section className="soft-section rounded-2xl p-4">
        <div className="font-semibold">{t("updatesTitle")}</div>
        <div className="mt-1 text-sm text-[color:var(--color-muted)]">{t("updatesBody")}</div>
      </section>
    </div>
  );
}
