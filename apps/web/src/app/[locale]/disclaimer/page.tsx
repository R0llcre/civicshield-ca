import { getTranslations } from "next-intl/server";

import { Badge, Hero } from "@/components/ui/AppShell";

export default async function DisclaimerPage() {
  const t = await getTranslations("Disclaimer");

  return (
    <div className="space-y-6 sm:space-y-8">
      <Hero
        kicker={<Badge tone="amber">{t("title")}</Badge>}
        title={t("title")}
        subtitle={t("body")}
        className="py-7 sm:py-8"
      />

      <div className="grid gap-3 sm:grid-cols-2">
        <section className="soft-section rounded-2xl p-4">
          <div className="font-semibold">{t("rosterTitle")}</div>
          <div className="mt-1 text-sm text-[color:var(--color-muted)]">{t("rosterBody")}</div>
        </section>

        <section className="soft-section rounded-2xl p-4">
          <div className="font-semibold">{t("noAdviceTitle")}</div>
          <div className="mt-1 text-sm text-[color:var(--color-muted)]">{t("noAdviceBody")}</div>
        </section>

        <section className="soft-section rounded-2xl p-4 sm:col-span-2">
          <div className="font-semibold">{t("emergencyTitle")}</div>
          <div className="mt-1 text-sm text-[color:var(--color-muted)]">{t("emergencyBody")}</div>
        </section>
      </div>
    </div>
  );
}
