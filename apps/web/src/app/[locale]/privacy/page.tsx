import { getTranslations } from "next-intl/server";

import { Badge, Hero } from "@/components/ui/AppShell";

export default async function PrivacyPage() {
  const t = await getTranslations("Privacy");

  return (
    <div className="space-y-6 sm:space-y-8">
      <Hero
        kicker={<Badge tone="teal">{t("title")}</Badge>}
        title={t("title")}
        subtitle={t("body")}
        className="py-7 sm:py-8"
      />

      <div className="grid gap-3 sm:grid-cols-2">
        <section className="soft-section rounded-2xl p-4">
          <div className="font-semibold">{t("vaultTitle")}</div>
          <div className="mt-1 text-sm text-[color:var(--color-muted)]">{t("vaultBody")}</div>
        </section>

        <section className="soft-section rounded-2xl p-4">
          <div className="font-semibold">{t("logsTitle")}</div>
          <div className="mt-1 text-sm text-[color:var(--color-muted)]">{t("logsBody")}</div>
        </section>

        <section className="soft-section rounded-2xl p-4 sm:col-span-2">
          <div className="font-semibold">{t("thirdPartiesTitle")}</div>
          <div className="mt-1 text-sm text-[color:var(--color-muted)]">{t("thirdPartiesBody")}</div>
        </section>
      </div>
    </div>
  );
}
