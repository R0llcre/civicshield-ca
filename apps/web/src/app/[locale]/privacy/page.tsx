import { getTranslations } from "next-intl/server";

export default async function PrivacyPage() {
  const t = await getTranslations("Privacy");

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-[family-name:var(--font-display)] text-3xl font-bold tracking-tight">
          {t("title")}
        </h1>
        <p className="mt-2 max-w-3xl text-[color:var(--color-muted)]">{t("body")}</p>
      </div>

      <div className="grid gap-3 sm:grid-cols-2">
        <section className="rounded-2xl bg-white/60 p-4 ring-1 ring-[color:var(--color-surface-border)]">
          <div className="font-semibold">{t("vaultTitle")}</div>
          <div className="mt-1 text-sm text-[color:var(--color-muted)]">{t("vaultBody")}</div>
        </section>

        <section className="rounded-2xl bg-white/60 p-4 ring-1 ring-[color:var(--color-surface-border)]">
          <div className="font-semibold">{t("logsTitle")}</div>
          <div className="mt-1 text-sm text-[color:var(--color-muted)]">{t("logsBody")}</div>
        </section>

        <section className="rounded-2xl bg-white/60 p-4 ring-1 ring-[color:var(--color-surface-border)] sm:col-span-2">
          <div className="font-semibold">{t("thirdPartiesTitle")}</div>
          <div className="mt-1 text-sm text-[color:var(--color-muted)]">{t("thirdPartiesBody")}</div>
        </section>
      </div>
    </div>
  );
}

