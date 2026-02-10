"use client";

import { Search } from "lucide-react";
import { useTranslations } from "next-intl";
import * as React from "react";

import { Card, CardContent, CardHeader } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { cn } from "@/lib/cn";
import type { AidOrg } from "@/data/aid_org_sample";

const CA_COUNTIES = [
  "Alameda",
  "Alpine",
  "Amador",
  "Butte",
  "Calaveras",
  "Colusa",
  "Contra Costa",
  "Del Norte",
  "El Dorado",
  "Fresno",
  "Glenn",
  "Humboldt",
  "Imperial",
  "Inyo",
  "Kern",
  "Kings",
  "Lake",
  "Lassen",
  "Los Angeles",
  "Madera",
  "Marin",
  "Mariposa",
  "Mendocino",
  "Merced",
  "Modoc",
  "Mono",
  "Monterey",
  "Napa",
  "Nevada",
  "Orange",
  "Placer",
  "Plumas",
  "Riverside",
  "Sacramento",
  "San Benito",
  "San Bernardino",
  "San Diego",
  "San Francisco",
  "San Joaquin",
  "San Luis Obispo",
  "San Mateo",
  "Santa Barbara",
  "Santa Clara",
  "Santa Cruz",
  "Shasta",
  "Sierra",
  "Siskiyou",
  "Solano",
  "Sonoma",
  "Stanislaus",
  "Sutter",
  "Tehama",
  "Trinity",
  "Tulare",
  "Tuolumne",
  "Ventura",
  "Yolo",
  "Yuba"
];

const LANGUAGES = [
  { value: "English", key: "english" },
  { value: "Spanish", key: "spanish" },
  { value: "Chinese", key: "chinese" },
  { value: "Vietnamese", key: "vietnamese" },
  { value: "Korean", key: "korean" },
  { value: "Tagalog", key: "tagalog" },
  { value: "Arabic", key: "arabic" },
] as const;

const SERVICES = [
  { value: "Asylum", key: "asylum" },
  { value: "Family", key: "family" },
  { value: "VAWA", key: "vawa" },
  { value: "Work authorization", key: "workAuth" },
  { value: "Removal defense", key: "removalDefense" },
  { value: "Naturalization", key: "naturalization" },
] as const;

type ApiResponse = { results: AidOrg[] };

function statusPill(status: AidOrg["intake_status"]) {
  if (status === "open") return "bg-emerald-50 text-emerald-800 ring-emerald-200";
  if (status === "waitlist") return "bg-amber-50 text-amber-800 ring-amber-200";
  return "bg-zinc-100 text-zinc-700 ring-zinc-200";
}

export function HelpClient() {
  const t = useTranslations("Help");

  const [county, setCounty] = React.useState<string>("");
  const [lang, setLang] = React.useState<string>("");
  const [service, setService] = React.useState<string>("");
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [results, setResults] = React.useState<AidOrg[] | null>(null);

  async function runSearch() {
    setLoading(true);
    setError(null);
    setResults(null);
    try {
      const url = new URL("/api/directory/search", window.location.origin);
      if (county) url.searchParams.set("county", county);
      if (lang) url.searchParams.set("lang", lang);
      if (service) url.searchParams.set("service", service);

      const res = await fetch(url.toString());
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = (await res.json()) as ApiResponse;
      setResults(data.results);
    } catch (e) {
      setError(e instanceof Error ? e.message : t("errors.unknown"));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="grid gap-4 lg:grid-cols-[0.9fr_1.1fr]">
      <Card>
        <CardHeader>
          <div className="text-sm font-semibold text-[color:var(--color-muted)]">{t("description")}</div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-3">
            <label className="space-y-2">
              <div className="text-xs font-semibold text-[color:var(--color-muted)]">{t("countyLabel")}</div>
              <select
                value={county}
                onChange={(e) => setCounty(e.target.value)}
                className={cn(
                  "w-full rounded-xl bg-white/70 px-3 py-2 text-sm",
                  "ring-1 ring-[color:var(--color-surface-border)]",
                  "focus:outline-none focus:ring-2 focus:ring-[color:var(--color-focus)]",
                )}
              >
                <option value="">{t("any")}</option>
                {CA_COUNTIES.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </label>

            <label className="space-y-2">
              <div className="text-xs font-semibold text-[color:var(--color-muted)]">{t("languageLabel")}</div>
              <select
                value={lang}
                onChange={(e) => setLang(e.target.value)}
                className={cn(
                  "w-full rounded-xl bg-white/70 px-3 py-2 text-sm",
                  "ring-1 ring-[color:var(--color-surface-border)]",
                  "focus:outline-none focus:ring-2 focus:ring-[color:var(--color-focus)]",
                )}
              >
                <option value="">{t("any")}</option>
                {LANGUAGES.map((l) => (
                  <option key={l.value} value={l.value}>
                    {t(`languages.${l.key}`)}
                  </option>
                ))}
              </select>
            </label>

            <label className="space-y-2">
              <div className="text-xs font-semibold text-[color:var(--color-muted)]">{t("serviceLabel")}</div>
              <select
                value={service}
                onChange={(e) => setService(e.target.value)}
                className={cn(
                  "w-full rounded-xl bg-white/70 px-3 py-2 text-sm",
                  "ring-1 ring-[color:var(--color-surface-border)]",
                  "focus:outline-none focus:ring-2 focus:ring-[color:var(--color-focus)]",
                )}
              >
                <option value="">{t("any")}</option>
                {SERVICES.map((s) => (
                  <option key={s.value} value={s.value}>
                    {t(`services.${s.key}`)}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <Button onClick={runSearch} disabled={loading}>
            <Search className="h-4 w-4" />
            {t("searchButton")}
          </Button>

          {error ? (
            <div className="rounded-xl bg-red-50 px-3 py-2 text-sm text-red-700 ring-1 ring-red-200">
              {error}
            </div>
          ) : null}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <div className="font-[family-name:var(--font-display)] text-lg font-semibold tracking-tight">
            {t("resultsTitle")}
          </div>
        </CardHeader>
        <CardContent className="space-y-3">
          {results ? (
            results.length === 0 ? (
              <div className="text-sm text-[color:var(--color-muted)]">{t("noResults")}</div>
            ) : (
              <ul className="space-y-2">
                {results
                  .slice()
                  .sort((a, b) => {
                    const rank = (s: AidOrg["intake_status"]) =>
                      s === "open" ? 0 : s === "waitlist" ? 1 : 2;
                    return rank(a.intake_status) - rank(b.intake_status);
                  })
                  .map((org) => {
                    const verifiedAt = org.verified_at ?? org.updated_at ?? null;
                    const verifiedLabel = verifiedAt
                      ? t("verifiedAt", { time: new Date(verifiedAt).toLocaleString() })
                      : null;

                    return (
                      <li
                        key={org.id}
                        className="rounded-2xl bg-white/60 p-4 ring-1 ring-[color:var(--color-surface-border)]"
                      >
                        <div className="flex flex-wrap items-start justify-between gap-2">
                          <div>
                            <div className="font-[family-name:var(--font-display)] text-base font-semibold">
                              {org.name}
                            </div>
                            <div className="mt-1 text-xs text-[color:var(--color-muted)]">
                              {t("result.counties")}: {org.counties.join(", ")}
                            </div>
                            {verifiedLabel ? (
                              <div className="mt-1 text-xs text-[color:var(--color-muted)]">{verifiedLabel}</div>
                            ) : null}
                          </div>
                          <span
                            className={cn(
                              "inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ring-1",
                              statusPill(org.intake_status),
                            )}
                          >
                            {t(`status.${org.intake_status}`)}
                          </span>
                        </div>

                        <div className="mt-3 grid gap-1 text-xs text-[color:var(--color-muted)]">
                          <div>
                            {t("result.languages")}: {org.languages.join(", ")}
                          </div>
                          <div>
                            {t("result.services")}: {org.services.join(", ")}
                          </div>
                          {org.eta_callback_days ? (
                            <div>{t("result.etaCallbackDays", { days: org.eta_callback_days })}</div>
                          ) : null}
                          {org.next_clinic_at ? (
                            <div>
                              {t("result.nextClinicAt", {
                                time: new Date(org.next_clinic_at).toLocaleString(),
                              })}
                            </div>
                          ) : null}
                          {org.website ? (
                            <a
                              href={org.website}
                              target="_blank"
                              rel="noreferrer"
                              className="text-[color:var(--color-accent)] hover:underline"
                            >
                              {t("result.website")}
                            </a>
                          ) : null}
                          {org.source_url ? (
                            <a
                              href={org.source_url}
                              target="_blank"
                              rel="noreferrer"
                              className="text-[color:var(--color-accent)] hover:underline"
                            >
                              {t("result.source")}
                            </a>
                          ) : null}
                        </div>
                      </li>
                    );
                  })}
              </ul>
            )
          ) : (
            <div className="text-sm text-[color:var(--color-muted)]">
              {t("emptyState")}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
