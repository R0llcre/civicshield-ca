"use client";

import { AlertTriangle, FolderLock } from "lucide-react";
import { useTranslations } from "next-intl";
import * as React from "react";

import { Badge, SectionHeader } from "@/components/ui/AppShell";
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
  const checkedCount = RED_FLAG_KEYS.filter((k) => checked[k]).length;

  return (
    <div className="grid gap-4 xl:grid-cols-[0.95fr_1.05fr]">
      <Card variant="highlight" className="animate-rise-in">
        <CardHeader>
          <SectionHeader
            title={t("redFlagsTitle")}
            subtitle={t("description")}
            action={<Badge tone="amber">{checkedCount}</Badge>}
          />
        </CardHeader>
        <CardContent className="space-y-4">
          <ul className="space-y-2">
            {RED_FLAG_KEYS.map((key) => (
              <li key={key}>
                <label className="group flex cursor-pointer items-start gap-3 rounded-2xl border border-[color:var(--color-surface-border)] bg-white/80 px-3.5 py-3 shadow-[var(--shadow-sm)] hover:border-[color:var(--color-accent-3)]/40">
                  <input
                    type="checkbox"
                    className="mt-0.5 h-4 w-4 rounded border-[color:var(--color-surface-border)] text-[color:var(--color-accent-3)] focus:ring-[color:var(--color-focus)]"
                    checked={!!checked[key]}
                    onChange={(e) => setChecked((prev) => ({ ...prev, [key]: e.target.checked }))}
                  />
                  <span className="text-sm text-[color:var(--color-muted)]">{t(`redFlags.${key}`)}</span>
                </label>
              </li>
            ))}
          </ul>

          <div className="soft-section rounded-2xl p-4">
            <div className="flex items-center gap-2 text-sm font-bold">
              <AlertTriangle className="h-4 w-4 text-[color:var(--color-accent-3)]" />
              {t("actionsTitle")}
            </div>
            <ul className="mt-2 grid gap-2 text-sm text-[color:var(--color-muted)]">
              <li className="rounded-xl border border-[color:var(--color-surface-border)] bg-white/75 px-3 py-2">
                {t("actions.verify")}
              </li>
              <li className="rounded-xl border border-[color:var(--color-surface-border)] bg-white/75 px-3 py-2">
                {t("actions.receipts")}
              </li>
              <li className="rounded-xl border border-[color:var(--color-surface-border)] bg-white/75 px-3 py-2">
                {t("actions.noBlank")}
              </li>
            </ul>
          </div>
        </CardContent>
      </Card>

      <Card variant="flat" className="animate-rise-in">
        <CardHeader>
          <SectionHeader
            title={t("vaultTitle")}
            subtitle={t("vaultBody")}
            action={
              <span className="inline-flex items-center gap-2 rounded-full bg-emerald-100 px-3 py-1 text-xs font-semibold text-emerald-800">
                <FolderLock className="h-3.5 w-3.5" />
                {tCommon("informationOnlyTitle")}
              </span>
            }
          />
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
