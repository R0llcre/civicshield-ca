import { getTranslations } from "next-intl/server";

import { Badge, Hero } from "@/components/ui/AppShell";
import { VerifyClient } from "@/features/verify/VerifyClient";

export default async function VerifyPage() {
  const t = await getTranslations("Verify");

  return (
    <div className="space-y-6 sm:space-y-8">
      <Hero
        kicker={<Badge tone="blue">{t("title")}</Badge>}
        title={t("title")}
        subtitle={t("description")}
        className="py-7 sm:py-8"
      />

      <VerifyClient />
    </div>
  );
}
