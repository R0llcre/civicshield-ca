"use client";

import { Building2, Search } from "lucide-react";
import { useTranslations } from "next-intl";
import * as React from "react";

import { Badge, SectionHeader } from "@/components/ui/AppShell";
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
  "Yuba",
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

const selectClass = cn(
  "h-11 w-full rounded-2xl border border-[color:var(--color-surface-border)] bg-[color:var(--color-surface-strong)] px-3.5 text-sm",
  "shadow-[var(--shadow-sm)] focus:border-[color:var(--color-focus)] focus:outline-none focus:ring-2 focus:ring-[color:var(--color-focus)]/35",
);

function statusPill(status: AidOrg["intake_status"]) {
  if (status === "open")
    return "bg-emerald-100 text-emerald-800 border-emerald-200";
  if (status === "waitlist")
    return "bg-amber-100 text-amber-800 border-amber-200";
  return "bg-zinc-100 text-zinc-700 border-zinc-200";
}

export function HelpClient() {
  const t = useTranslations("Help");

  const [county, setCounty] = React.useState<string>("");
  const [lang, setLang] = React.useState<string>("");
  const [service, setService] = React.useState<string>("");
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [results, setResults] = React.useState<AidOrg[] | null>(null);

  const runSearch = React.useCallback(async () => {
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
  }, [county, lang, service, t]);

  const didAutoLoad = React.useRef(false);
  React.useEffect(() => {
    if (didAutoLoad.current) return;
    didAutoLoad.current = true;
    void runSearch();
  }, [runSearch]);

  return (
    <div className="grid gap-4 xl:grid-cols-[0.92fr_1.08fr]">
      <Card variant="highlight" className="animate-rise-in">
        <CardHeader>
          <SectionHeader
            title={t("searchButton")}
            subtitle={t("description")}
            action={<Badge tone="teal">{t("title")}</Badge>}
          />
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-3">
            <label className="space-y-2">
              <div className="text-xs font-semibold uppercase tracking-[0.07em] text-[color:var(--color-muted)]">
                {t("countyLabel")}
              </div>
              <select value={county} onChange={(e) => setCounty(e.target.value)} className={selectClass}>
                <option value="">{t("any")}</option>
                {CA_COUNTIES.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </label>

            <label className="space-y-2">
              <div className="text-xs font-semibold uppercase tracking-[0.07em] text-[color:var(--color-muted)]">
                {t("languageLabel")}
              </div>
              <select value={lang} onChange={(e) => setLang(e.target.value)} className={selectClass}>
                <option value="">{t("any")}</option>
                {LANGUAGES.map((l) => (
                  <option key={l.value} value={l.value}>
                    {t(`languages.${l.key}`)}
                  </option>
                ))}
              </select>
            </label>

            <label className="space-y-2">
              <div className="text-xs font-semibold uppercase tracking-[0.07em] text-[color:var(--color-muted)]">
                {t("serviceLabel")}
              </div>
              <select value={service} onChange={(e) => setService(e.target.value)} className={selectClass}>
                <option value="">{t("any")}</option>
                {SERVICES.map((s) => (
                  <option key={s.value} value={s.value}>
                    {t(`services.${s.key}`)}
                  </option>
                ))}
              </select>
            </label>
          </div>

          <Button onClick={runSearch} disabled={loading} size="lg" tone="teal">
            <Search className="h-4 w-4" />
            {t("searchButton")}
          </Button>

          {error ? (
            <div className="rounded-2xl border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</div>
          ) : null}
        </CardContent>
      </Card>

      <Card variant="flat" className="animate-rise-in">
        <CardHeader>
          <SectionHeader
            title={t("resultsTitle")}
            subtitle={t("emptyState")}
            action={
              <span className="inline-flex items-center gap-2 rounded-full bg-blue-100 px-3 py-1 text-xs font-semibold text-blue-800">
                <Building2 className="h-3.5 w-3.5" />
                {t("title")}
              </span>
            }
          />
        </CardHeader>
        <CardContent className="space-y-3">
          {results ? (
            results.length === 0 ? (
              <div className="rounded-2xl border border-[color:var(--color-surface-border)] bg-white/60 px-4 py-3 text-sm text-[color:var(--color-muted)]">
                {t("noResults")}
              </div>
            ) : (
              <ul className="space-y-2">
                {results
                  .slice()
                  .sort((a, b) => {
                    const rank = (s: AidOrg["intake_status"]) => (s === "open" ? 0 : s === "waitlist" ? 1 : 2);
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
                        className="rounded-2xl border border-[color:var(--color-surface-border)] bg-white/80 p-4 shadow-[var(--shadow-sm)]"
                      >
                        <div className="flex flex-wrap items-start justify-between gap-2">
                          <div>
                            <div className="font-[family-name:var(--font-display)] text-base font-bold tracking-tight">
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
                              "inline-flex items-center rounded-full border px-3 py-1 text-xs font-semibold",
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
                              {t("result.nextClinicAt", { time: new Date(org.next_clinic_at).toLocaleString() })}
                            </div>
                          ) : null}
                          <div className="mt-1 flex flex-wrap gap-2">
                            {org.website ? (
                              <a
                                href={org.website}
                                target="_blank"
                                rel="noreferrer"
                                className="rounded-full border border-[color:var(--color-surface-border)] bg-white px-3 py-1 text-[11px] font-semibold text-[color:var(--color-accent)] hover:bg-[color:var(--color-accent-soft)]"
                              >
                                {t("result.website")}
                              </a>
                            ) : null}
                            {org.source_url ? (
                              <a
                                href={org.source_url}
                                target="_blank"
                                rel="noreferrer"
                                className="rounded-full border border-[color:var(--color-surface-border)] bg-white px-3 py-1 text-[11px] font-semibold text-[color:var(--color-accent)] hover:bg-[color:var(--color-accent-soft)]"
                              >
                                {t("result.source")}
                              </a>
                            ) : null}
                          </div>
                        </div>
                      </li>
                    );
                  })}
              </ul>
            )
          ) : (
            <div className="rounded-2xl border border-[color:var(--color-surface-border)] bg-white/60 px-4 py-3 text-sm text-[color:var(--color-muted)]">
              {t("emptyState")}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
