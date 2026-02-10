import { ExternalLink } from "lucide-react";
import { getTranslations } from "next-intl/server";

export default async function AboutPage() {
  const t = await getTranslations("About");
  const tCommon = await getTranslations("Common");

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-[family-name:var(--font-display)] text-3xl font-bold tracking-tight">
          {t("title")}
        </h1>
        <p className="mt-2 max-w-3xl text-[color:var(--color-muted)]">{t("body")}</p>
      </div>

      <section className="space-y-3">
        <h2 className="font-[family-name:var(--font-display)] text-xl font-semibold tracking-tight">
          {t("sourcesTitle")}
        </h2>

        <div className="grid gap-3 sm:grid-cols-2">
          <div className="rounded-2xl bg-white/60 p-4 ring-1 ring-[color:var(--color-surface-border)]">
            <div className="font-semibold">{t("sources.doj.title")}</div>
            <div className="mt-1 text-sm text-[color:var(--color-muted)]">{t("sources.doj.body")}</div>
            <a
              className="mt-3 inline-flex items-center gap-2 text-sm font-semibold text-[color:var(--color-accent)] hover:underline"
              href="https://www.justice.gov/eoir/recognition-accreditation-roster-reports"
              target="_blank"
              rel="noreferrer"
            >
              {tCommon("open")}
              <ExternalLink className="h-4 w-4" />
            </a>
          </div>

          <div className="rounded-2xl bg-white/60 p-4 ring-1 ring-[color:var(--color-surface-border)]">
            <div className="font-semibold">{t("sources.calbar.title")}</div>
            <div className="mt-1 text-sm text-[color:var(--color-muted)]">{t("sources.calbar.body")}</div>
            <a
              className="mt-3 inline-flex items-center gap-2 text-sm font-semibold text-[color:var(--color-accent)] hover:underline"
              href="https://apps.calbar.ca.gov/attorney/LicenseeSearch/QuickSearch"
              target="_blank"
              rel="noreferrer"
            >
              {tCommon("open")}
              <ExternalLink className="h-4 w-4" />
            </a>
          </div>

          <div className="rounded-2xl bg-white/60 p-4 ring-1 ring-[color:var(--color-surface-border)] sm:col-span-2">
            <div className="font-semibold">{t("sources.sos.title")}</div>
            <div className="mt-1 text-sm text-[color:var(--color-muted)]">{t("sources.sos.body")}</div>
            <a
              className="mt-3 inline-flex items-center gap-2 text-sm font-semibold text-[color:var(--color-accent)] hover:underline"
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

      <section className="rounded-2xl bg-white/60 p-4 ring-1 ring-[color:var(--color-surface-border)]">
        <div className="font-semibold">{t("updatesTitle")}</div>
        <div className="mt-1 text-sm text-[color:var(--color-muted)]">{t("updatesBody")}</div>
      </section>
    </div>
  );
}

