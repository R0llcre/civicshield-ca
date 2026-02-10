import { getTranslations } from "next-intl/server";

import { ShieldClient } from "@/features/shield/ShieldClient";

export default async function ShieldPage() {
  const t = await getTranslations("Shield");

  return (
    <div className="space-y-6">
      <div>
        <h1 className="font-[family-name:var(--font-display)] text-3xl font-bold tracking-tight">
          {t("title")}
        </h1>
        <p className="mt-2 max-w-3xl text-[color:var(--color-muted)]">{t("description")}</p>
      </div>

      <ShieldClient />
    </div>
  );
}

