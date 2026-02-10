import { getTranslations } from "next-intl/server";

import { PartnerClient } from "@/features/partner/PartnerClient";

export default async function PartnerPage() {
  const t = await getTranslations("Partner");

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-[family-name:var(--font-display)] text-3xl font-bold tracking-tight">
          {t("title")}
        </h1>
        <p className="mt-2 max-w-3xl text-[color:var(--color-muted)]">{t("description")}</p>
      </div>

      <PartnerClient />
    </div>
  );
}

