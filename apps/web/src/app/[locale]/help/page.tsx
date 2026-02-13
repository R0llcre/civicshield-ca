import { getTranslations } from "next-intl/server";

import { Badge, Hero } from "@/components/ui/AppShell";
import { HelpClient } from "@/features/help/HelpClient";

export default async function HelpPage() {
  const t = await getTranslations("Help");

  return (
    <div className="space-y-6 sm:space-y-8">
      <Hero
        kicker={<Badge tone="teal">{t("title")}</Badge>}
        title={t("title")}
        subtitle={t("description")}
        className="py-7 sm:py-8"
      />

      <HelpClient />
    </div>
  );
}
