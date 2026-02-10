import { getTranslations } from "next-intl/server";

import { VerifyClient } from "@/features/verify/VerifyClient";

export default async function VerifyPage() {
  const t = await getTranslations("Verify");

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-[family-name:var(--font-display)] text-3xl font-bold tracking-tight">
          {t("title")}
        </h1>
        <p className="mt-2 max-w-3xl text-[color:var(--color-muted)]">{t("description")}</p>
      </div>

      <VerifyClient />
    </div>
  );
}

