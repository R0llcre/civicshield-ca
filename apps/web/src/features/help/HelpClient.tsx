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

const LANGUAGES = ["English", "Spanish", "Chinese", "Vietnamese", "Korean", "Tagalog", "Arabic"] as const;

const SERVICES = [
  "Asylum",
  "Family",
  "VAWA",
  "Work authorization",
  "Removal defense",
  "Naturalization"
] as const;

type ApiResponse = { results: AidOrg[] };

function statusLabel(status: AidOrg["intake_status"]) {
  if (status === "open") return "Open";
  if (status === "waitlist") return "Waitlist";
  return "Closed";
}

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
      setError(e instanceof Error ? e.message : "Unknown error");
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
                <option value="">Any</option>
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
                <option value="">Any</option>
                {LANGUAGES.map((l) => (
                  <option key={l} value={l}>
                    {l}
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
                <option value="">Any</option>
                {SERVICES.map((s) => (
                  <option key={s} value={s}>
                    {s}
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
                  .map((org) => (
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
                            Counties: {org.counties.join(", ")}
                          </div>
                        </div>
                        <span
                          className={cn(
                            "inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold ring-1",
                            statusPill(org.intake_status),
                          )}
                        >
                          {statusLabel(org.intake_status)}
                        </span>
                      </div>

                      <div className="mt-3 grid gap-1 text-xs text-[color:var(--color-muted)]">
                        <div>Languages: {org.languages.join(", ")}</div>
                        <div>Services: {org.services.join(", ")}</div>
                        {org.eta_callback_days ? (
                          <div>Estimated callback: {org.eta_callback_days} day(s)</div>
                        ) : null}
                        {org.next_clinic_at ? (
                          <div>Next clinic: {new Date(org.next_clinic_at).toLocaleString()}</div>
                        ) : null}
                        {org.website ? (
                          <a
                            href={org.website}
                            target="_blank"
                            rel="noreferrer"
                            className="text-[color:var(--color-accent)] hover:underline"
                          >
                            Website
                          </a>
                        ) : null}
                      </div>
                    </li>
                  ))}
              </ul>
            )
          ) : (
            <div className="text-sm text-[color:var(--color-muted)]">
              Set filters and search to see results.
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}

