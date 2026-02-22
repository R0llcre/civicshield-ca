import { getTranslations } from "next-intl/server";

import { Badge, Hero } from "@/components/ui/AppShell";
import { AskClient } from "@/features/ask/AskClient";

export default async function AskPage() {
  const t = await getTranslations("Ask");

  return (
    <div className="space-y-6 sm:space-y-8">
      <Hero
        kicker={<Badge tone="teal">{t("title")}</Badge>}
        title={t("title")}
        subtitle={t("description")}
        className="py-7 sm:py-8"
      />

      <AskClient />
    </div>
  );
}

