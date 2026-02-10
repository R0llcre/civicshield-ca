"use client";

import { useTranslations } from "next-intl";
import * as React from "react";

import { Card, CardContent, CardHeader } from "@/components/ui/Card";
import { EvidenceVault } from "@/features/shield/VaultClient";

const RED_FLAG_KEYS = [
  "cashOnlyNoReceipt",
  "guaranteesResults",
  "signBlankForms",
  "tellsYouToLie",
  "keepsOriginalDocuments",
  "refusesWrittenAgreement",
] as const;

export function ShieldClient() {
  const t = useTranslations("Shield");
  const tVault = useTranslations("Vault");
  const tCommon = useTranslations("Common");

  const [checked, setChecked] = React.useState<Record<string, boolean>>({});

  return (
    <div className="grid gap-4 lg:grid-cols-[0.9fr_1.1fr]">
      <Card>
        <CardHeader>
          <div className="font-[family-name:var(--font-display)] text-lg font-semibold tracking-tight">
            {t("redFlagsTitle")}
          </div>
        </CardHeader>
        <CardContent className="space-y-4">
          <ul className="space-y-2">
            {RED_FLAG_KEYS.map((key) => (
              <li key={key} className="flex items-start gap-3">
                <label className="flex items-start gap-3">
                  <input
                    type="checkbox"
                    className="mt-1 h-4 w-4"
                    checked={!!checked[key]}
                    onChange={(e) => setChecked((prev) => ({ ...prev, [key]: e.target.checked }))}
                  />
                  <span className="text-sm text-[color:var(--color-muted)]">{t(`redFlags.${key}`)}</span>
                </label>
              </li>
            ))}
          </ul>

          <div className="rounded-2xl bg-white/60 p-4 ring-1 ring-[color:var(--color-surface-border)]">
            <div className="text-sm font-semibold">{t("actionsTitle")}</div>
            <ul className="mt-2 list-disc space-y-1 pl-5 text-sm text-[color:var(--color-muted)]">
              <li>{t("actions.verify")}</li>
              <li>{t("actions.receipts")}</li>
              <li>{t("actions.noBlank")}</li>
            </ul>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <div className="font-[family-name:var(--font-display)] text-lg font-semibold tracking-tight">
            {t("vaultTitle")}
          </div>
          <div className="mt-1 text-sm text-[color:var(--color-muted)]">{t("vaultBody")}</div>
        </CardHeader>
        <CardContent>
          <EvidenceVault
            labels={{
              newTitle: tVault("newTitle"),
              unlockTitle: tVault("unlockTitle"),
              passphraseLabel: tVault("passphraseLabel"),
              passphraseHint: tVault("passphraseHint"),
              createButton: tVault("createButton"),
              unlockButton: tVault("unlockButton"),
              lockButton: tVault("lockButton"),
              addFiles: tVault("addFiles"),
              export: tVault("export"),
              import: tVault("import"),
              itemsTitle: tVault("itemsTitle"),
              empty: tVault("empty"),
              download: tVault("download"),
              delete: tVault("delete"),
              importDialogTitle: tVault("importDialogTitle"),
              importDialogBody: tVault("importDialogBody"),
              importConfirm: tVault("importConfirm"),
              cancel: tCommon("cancel"),
              addFilesFailed: tVault("errors.addFilesFailed"),
              exportFailed: tVault("errors.exportFailed"),
              wrongPassphrase: tVault("errors.wrongPassphrase"),
              invalidFile: tVault("errors.invalidFile"),
            }}
          />
        </CardContent>
      </Card>
    </div>
  );
}
