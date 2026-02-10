"use client";

import { ExternalLink, Search } from "lucide-react";
import { useTranslations } from "next-intl";
import * as React from "react";

import { Card, CardContent, CardHeader } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Input } from "@/components/ui/Input";
import { cn } from "@/lib/cn";
import type { RaEntry } from "@/data/ra_sample";

const US_STATES: Array<{ name: string; code: string }> = [
  { name: "California", code: "CA" },
  { name: "Alabama", code: "AL" },
  { name: "Alaska", code: "AK" },
  { name: "Arizona", code: "AZ" },
  { name: "Arkansas", code: "AR" },
  { name: "Colorado", code: "CO" },
  { name: "Connecticut", code: "CT" },
  { name: "Delaware", code: "DE" },
  { name: "District of Columbia", code: "DC" },
  { name: "Florida", code: "FL" },
  { name: "Georgia", code: "GA" },
  { name: "Hawaii", code: "HI" },
  { name: "Idaho", code: "ID" },
  { name: "Illinois", code: "IL" },
  { name: "Indiana", code: "IN" },
  { name: "Iowa", code: "IA" },
  { name: "Kansas", code: "KS" },
  { name: "Kentucky", code: "KY" },
  { name: "Louisiana", code: "LA" },
  { name: "Maine", code: "ME" },
  { name: "Maryland", code: "MD" },
  { name: "Massachusetts", code: "MA" },
  { name: "Michigan", code: "MI" },
  { name: "Minnesota", code: "MN" },
  { name: "Mississippi", code: "MS" },
  { name: "Missouri", code: "MO" },
  { name: "Montana", code: "MT" },
  { name: "Nebraska", code: "NE" },
  { name: "Nevada", code: "NV" },
  { name: "New Hampshire", code: "NH" },
  { name: "New Jersey", code: "NJ" },
  { name: "New Mexico", code: "NM" },
  { name: "New York", code: "NY" },
  { name: "North Carolina", code: "NC" },
  { name: "North Dakota", code: "ND" },
  { name: "Ohio", code: "OH" },
  { name: "Oklahoma", code: "OK" },
  { name: "Oregon", code: "OR" },
  { name: "Pennsylvania", code: "PA" },
  { name: "Rhode Island", code: "RI" },
  { name: "South Carolina", code: "SC" },
  { name: "South Dakota", code: "SD" },
  { name: "Tennessee", code: "TN" },
  { name: "Texas", code: "TX" },
  { name: "Utah", code: "UT" },
  { name: "Vermont", code: "VT" },
  { name: "Virginia", code: "VA" },
  { name: "Washington", code: "WA" },
  { name: "West Virginia", code: "WV" },
  { name: "Wisconsin", code: "WI" },
  { name: "Wyoming", code: "WY" }
];

type ApiResponse = {
  results: RaEntry[];
  data_freshness: { ended_at: string | null; source_sha256: string | null } | null;
};

export function VerifyClient() {
  const t = useTranslations("Verify");

  const [name, setName] = React.useState("");
  const [state, setState] = React.useState("CA");
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [results, setResults] = React.useState<RaEntry[] | null>(null);
  const [freshness, setFreshness] = React.useState<ApiResponse["data_freshness"]>(null);

  async function runSearch() {
    const q = name.trim();
    if (!q) return;

    setLoading(true);
    setError(null);
    setResults(null);
    try {
      const url = new URL("/api/verify/ra", window.location.origin);
      url.searchParams.set("name", q);
      url.searchParams.set("state", state);

      const res = await fetch(url.toString());
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const data = (await res.json()) as ApiResponse;
      setResults(data.results);
      setFreshness(data.data_freshness ?? null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Unknown error");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="grid gap-4 lg:grid-cols-[1.2fr_0.8fr]">
      <Card className="overflow-hidden">
        <CardHeader>
          <div className="text-sm font-semibold text-[color:var(--color-muted)]">{t("description")}</div>
        </CardHeader>
        <CardContent className="space-y-4">
          <div className="grid gap-3 sm:grid-cols-2">
            <label className="space-y-2">
              <div className="text-xs font-semibold text-[color:var(--color-muted)]">{t("nameLabel")}</div>
              <Input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder={t("namePlaceholder")}
                onKeyDown={(e) => {
                  if (e.key === "Enter") runSearch();
                }}
              />
            </label>
            <label className="space-y-2">
              <div className="text-xs font-semibold text-[color:var(--color-muted)]">{t("stateLabel")}</div>
              <select
                value={state}
                onChange={(e) => setState(e.target.value)}
                className={cn(
                  "w-full rounded-xl bg-white/70 px-3 py-2 text-sm",
                  "ring-1 ring-[color:var(--color-surface-border)]",
                  "focus:outline-none focus:ring-2 focus:ring-[color:var(--color-focus)]",
                )}
              >
                {US_STATES.map((s) => (
                  <option key={s.code} value={s.code}>
                    {s.name} ({s.code})
                  </option>
                ))}
              </select>
              <div className="text-xs text-[color:var(--color-muted-2)]">{t("stateHint")}</div>
            </label>
          </div>

          <div className="flex items-center gap-3">
            <Button onClick={runSearch} disabled={loading || !name.trim()}>
              <Search className="h-4 w-4" />
              {t("searchButton")}
            </Button>
            {freshness?.ended_at ? (
              <div className="text-xs text-[color:var(--color-muted)]">
                Data updated: {new Date(freshness.ended_at).toLocaleString()}
              </div>
            ) : null}
          </div>

          {error ? (
            <div className="rounded-xl bg-red-50 px-3 py-2 text-sm text-red-700 ring-1 ring-red-200">
              {error}
            </div>
          ) : null}

          {results ? (
            <div className="space-y-3">
              <div className="text-sm font-semibold">{t("resultsTitle")}</div>
              {results.length === 0 ? <div className="text-sm">{t("noResults")}</div> : null}

              <ul className="space-y-2">
                {results.map((r, idx) => (
                  <li
                    key={`${r.entry_type}-${r.name}-${r.organization_name ?? ""}-${idx}`}
                    className="rounded-2xl bg-white/60 p-4 ring-1 ring-[color:var(--color-surface-border)]"
                  >
                    <div className="flex flex-wrap items-start justify-between gap-2">
                      <div>
                        <div className="font-[family-name:var(--font-display)] text-base font-semibold">
                          {r.name}
                        </div>
                        <div className="mt-1 text-xs font-semibold text-[color:var(--color-muted)]">
                          {r.entry_type === "representative" ? "Accredited representative" : "Recognized organization"}
                          {r.organization_name ? ` · ${r.organization_name}` : ""}
                          {r.city || r.state ? ` · ${(r.city ? `${r.city}, ` : "") + r.state}` : ""}
                        </div>
                      </div>
                      {r.source_url ? (
                        <a
                          className="inline-flex items-center gap-2 rounded-xl bg-white/70 px-3 py-2 text-xs font-semibold ring-1 ring-[color:var(--color-surface-border)] hover:bg-white"
                          href={r.source_url}
                          target="_blank"
                          rel="noreferrer"
                        >
                          {t("evidence")}
                          <ExternalLink className="h-3.5 w-3.5" />
                        </a>
                      ) : null}
                    </div>

                    {r.phone || r.address || r.accreditation_level || r.status ? (
                      <div className="mt-3 grid gap-1 text-sm text-[color:var(--color-muted)]">
                        {r.accreditation_level ? (
                          <div className="text-xs">
                            <span className="font-semibold text-[color:var(--color-foreground)]">Level:</span>{" "}
                            {r.accreditation_level}
                          </div>
                        ) : null}
                        {r.status ? (
                          <div className="text-xs">
                            <span className="font-semibold text-[color:var(--color-foreground)]">Status:</span>{" "}
                            {r.status}
                            {r.pending_renewal ? " (Pending renewal)" : ""}
                          </div>
                        ) : null}
                        {r.phone ? (
                          <div className="text-xs">
                            <span className="font-semibold text-[color:var(--color-foreground)]">Phone:</span>{" "}
                            {r.phone}
                          </div>
                        ) : null}
                        {r.address ? (
                          <div className="text-xs whitespace-pre-line">
                            <span className="font-semibold text-[color:var(--color-foreground)]">Address:</span>{" "}
                            {r.address}
                          </div>
                        ) : null}
                      </div>
                    ) : null}
                  </li>
                ))}
              </ul>

              <div className="text-xs text-[color:var(--color-muted-2)]">{t("disclaimer")}</div>
            </div>
          ) : null}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <div className="font-[family-name:var(--font-display)] text-lg font-semibold tracking-tight">
            {t("officialLinks.title")}
          </div>
        </CardHeader>
        <CardContent className="space-y-4 text-sm text-[color:var(--color-muted)]">
          <div className="space-y-1">
            <div className="font-semibold text-[color:var(--color-foreground)]">
              {t("officialLinks.calbarTitle")}
            </div>
            <div>{t("officialLinks.calbarBody")}</div>
            <a
              href="https://apps.calbar.ca.gov/attorney/LicenseeSearch/QuickSearch"
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-2 text-[color:var(--color-accent)] hover:underline"
            >
              Open
              <ExternalLink className="h-4 w-4" />
            </a>
          </div>

          <div className="space-y-1">
            <div className="font-semibold text-[color:var(--color-foreground)]">
              {t("officialLinks.sosTitle")}
            </div>
            <div>{t("officialLinks.sosBody")}</div>
            <a
              href="https://specialfilings.sos.ca.gov/icbs"
              target="_blank"
              rel="noreferrer"
              className="inline-flex items-center gap-2 text-[color:var(--color-accent)] hover:underline"
            >
              Open
              <ExternalLink className="h-4 w-4" />
            </a>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}

