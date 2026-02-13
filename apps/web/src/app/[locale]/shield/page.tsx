import { getTranslations } from "next-intl/server";

import { Badge, Hero } from "@/components/ui/AppShell";
import { ShieldClient } from "@/features/shield/ShieldClient";

export default async function ShieldPage() {
  const t = await getTranslations("Shield");

  return (
    <div className="space-y-6 sm:space-y-8">
      <Hero
        kicker={<Badge tone="amber">{t("title")}</Badge>}
        title={t("title")}
        subtitle={t("description")}
        className="py-7 sm:py-8"
      />

      <ShieldClient />
    </div>
  );
}
