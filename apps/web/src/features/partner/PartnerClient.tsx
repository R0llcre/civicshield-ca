"use client";

import { useTranslations } from "next-intl";
import * as React from "react";

import { Button } from "@/components/ui/Button";
import { Card, CardContent, CardHeader } from "@/components/ui/Card";
import { Input } from "@/components/ui/Input";
import { createBrowserSupabaseClient } from "@/lib/supabase";
import { cn } from "@/lib/cn";

type AidOrg = {
  id: string;
  name: string;
  intake_status: "open" | "waitlist" | "closed";
  next_clinic_at: string | null;
  eta_callback_days: number | null;
  languages: string[];
  counties: string[];
};

type MembershipRow = {
  org_id: string;
  role: "admin" | "editor";
};

function toDatetimeLocal(value: string | null) {
  if (!value) return "";
  const d = new Date(value);
  const pad = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

function fromDatetimeLocal(value: string) {
  if (!value) return null;
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return null;
  return d.toISOString();
}

export function PartnerClient() {
  const t = useTranslations("Partner");

  const supabase = React.useMemo(() => createBrowserSupabaseClient(), []);

  const [email, setEmail] = React.useState("");
  const [status, setStatus] = React.useState<string | null>(null);
  const [sessionToken, setSessionToken] = React.useState<string | null>(null);
  const [orgs, setOrgs] = React.useState<AidOrg[] | null>(null);
  const [busy, setBusy] = React.useState(false);

  React.useEffect(() => {
    if (!supabase) return;
    (async () => {
      const { data } = await supabase.auth.getSession();
      setSessionToken(data.session?.access_token ?? null);
    })();

    const { data: sub } = supabase.auth.onAuthStateChange((_evt, sess) => {
      setSessionToken(sess?.access_token ?? null);
    });
    return () => sub.subscription.unsubscribe();
  }, [supabase]);

  React.useEffect(() => {
    if (!supabase || !sessionToken) return;
    (async () => {
      setStatus(null);
      setOrgs(null);
      // Membership list
      const membership = await supabase.from("aid_org_membership").select("org_id,role");
      if (membership.error) {
        setStatus(membership.error.message);
        return;
      }
      const membershipRows = (membership.data ?? []) as unknown as MembershipRow[];
      const orgIds = membershipRows.map((m) => m.org_id).filter(Boolean);
      if (orgIds.length === 0) {
        setOrgs([]);
        return;
      }
      const orgRes = await supabase
        .from("aid_org")
        .select("id,name,intake_status,next_clinic_at,eta_callback_days,languages,counties")
        .in("id", orgIds);
      if (orgRes.error) {
        setStatus(orgRes.error.message);
        return;
      }
      setOrgs((orgRes.data ?? []) as unknown as AidOrg[]);
    })();
  }, [supabase, sessionToken]);

  async function sendMagicLink() {
    if (!supabase) return;
    setBusy(true);
    setStatus(null);
    try {
      const res = await supabase.auth.signInWithOtp({
        email: email.trim(),
        options: {
          emailRedirectTo: `${window.location.origin}/partner`,
        },
      });
      if (res.error) throw res.error;
      setStatus("Check your email for the magic link.");
    } catch (e) {
      setStatus(e instanceof Error ? e.message : "Failed to send link");
    } finally {
      setBusy(false);
    }
  }

  async function signOut() {
    if (!supabase) return;
    await supabase.auth.signOut();
    setOrgs(null);
    setStatus(null);
  }

  async function saveOrg(org: AidOrg) {
    if (!sessionToken) return;
    setBusy(true);
    setStatus(null);
    try {
      const res = await fetch(`/api/partner/org/${org.id}`, {
        method: "POST",
        headers: {
          "content-type": "application/json",
          authorization: `Bearer ${sessionToken}`,
        },
        body: JSON.stringify({
          intake_status: org.intake_status,
          next_clinic_at: org.next_clinic_at,
          eta_callback_days: org.eta_callback_days,
          languages: org.languages,
          counties: org.counties,
        }),
      });
      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      setStatus(t("saved"));
    } catch (e) {
      setStatus(e instanceof Error ? e.message : "Save failed");
    } finally {
      setBusy(false);
    }
  }

  if (!supabase) {
    return (
      <Card>
        <CardContent className="p-5 text-sm text-[color:var(--color-muted)]">{t("missingEnv")}</CardContent>
      </Card>
    );
  }

  return (
    <div className="space-y-4">
      {sessionToken ? (
        <div className="flex justify-end">
          <Button variant="ghost" onClick={signOut} disabled={busy}>
            {t("signOut")}
          </Button>
        </div>
      ) : null}

      {!sessionToken ? (
        <Card>
          <CardHeader>
            <div className="text-sm font-semibold text-[color:var(--color-muted)]">{t("description")}</div>
          </CardHeader>
          <CardContent className="space-y-3">
            <label className="space-y-2">
              <div className="text-xs font-semibold text-[color:var(--color-muted)]">{t("emailLabel")}</div>
              <Input value={email} onChange={(e) => setEmail(e.target.value)} type="email" />
            </label>
            <Button onClick={sendMagicLink} disabled={busy || !email.trim()}>
              {t("sendLink")}
            </Button>
            {status ? (
              <div className="rounded-xl bg-white/60 px-3 py-2 text-sm ring-1 ring-[color:var(--color-surface-border)]">
                {status}
              </div>
            ) : null}
          </CardContent>
        </Card>
      ) : (
        <Card>
          <CardHeader>
            <div className="font-[family-name:var(--font-display)] text-lg font-semibold tracking-tight">
              {t("statusTitle")}
            </div>
          </CardHeader>
          <CardContent className="space-y-4">
            {status ? (
              <div className="rounded-xl bg-white/60 px-3 py-2 text-sm ring-1 ring-[color:var(--color-surface-border)]">
                {status}
              </div>
            ) : null}

            {orgs ? (
              orgs.length === 0 ? (
                <div className="text-sm text-[color:var(--color-muted)]">No organizations assigned.</div>
              ) : (
                <ul className="space-y-3">
                  {orgs.map((org, idx) => (
                    <li
                      key={org.id}
                      className="rounded-2xl bg-white/60 p-4 ring-1 ring-[color:var(--color-surface-border)]"
                    >
                      <div className="font-[family-name:var(--font-display)] text-base font-semibold">
                        {org.name}
                      </div>

                      <div className="mt-3 grid gap-3 sm:grid-cols-2">
                        <label className="space-y-2">
                          <div className="text-xs font-semibold text-[color:var(--color-muted)]">Intake</div>
                          <select
                            value={org.intake_status}
                            onChange={(e) =>
                              setOrgs((prev) => {
                                if (!prev) return prev;
                                const next = [...prev];
                                next[idx] = {
                                  ...next[idx],
                                  intake_status: e.target.value as AidOrg["intake_status"],
                                };
                                return next;
                              })
                            }
                            className={cn(
                              "w-full rounded-xl bg-white/70 px-3 py-2 text-sm",
                              "ring-1 ring-[color:var(--color-surface-border)]",
                            )}
                          >
                            <option value="open">open</option>
                            <option value="waitlist">waitlist</option>
                            <option value="closed">closed</option>
                          </select>
                        </label>

                        <label className="space-y-2">
                          <div className="text-xs font-semibold text-[color:var(--color-muted)]">ETA callback (days)</div>
                          <Input
                            type="number"
                            value={org.eta_callback_days ?? ""}
                            onChange={(e) =>
                              setOrgs((prev) => {
                                if (!prev) return prev;
                                const next = [...prev];
                                next[idx] = {
                                  ...next[idx],
                                  eta_callback_days: e.target.value ? Number(e.target.value) : null,
                                };
                                return next;
                              })
                            }
                          />
                        </label>

                        <label className="space-y-2 sm:col-span-2">
                          <div className="text-xs font-semibold text-[color:var(--color-muted)]">Next clinic</div>
                          <Input
                            type="datetime-local"
                            value={toDatetimeLocal(org.next_clinic_at)}
                            onChange={(e) =>
                              setOrgs((prev) => {
                                if (!prev) return prev;
                                const next = [...prev];
                                next[idx] = { ...next[idx], next_clinic_at: fromDatetimeLocal(e.target.value) };
                                return next;
                              })
                            }
                          />
                        </label>

                        <label className="space-y-2 sm:col-span-2">
                          <div className="text-xs font-semibold text-[color:var(--color-muted)]">Languages (comma-separated)</div>
                          <Input
                            value={org.languages.join(", ")}
                            onChange={(e) =>
                              setOrgs((prev) => {
                                if (!prev) return prev;
                                const next = [...prev];
                                next[idx] = {
                                  ...next[idx],
                                  languages: e.target.value
                                    .split(",")
                                    .map((s) => s.trim())
                                    .filter(Boolean),
                                };
                                return next;
                              })
                            }
                          />
                        </label>

                        <label className="space-y-2 sm:col-span-2">
                          <div className="text-xs font-semibold text-[color:var(--color-muted)]">Counties (comma-separated)</div>
                          <Input
                            value={org.counties.join(", ")}
                            onChange={(e) =>
                              setOrgs((prev) => {
                                if (!prev) return prev;
                                const next = [...prev];
                                next[idx] = {
                                  ...next[idx],
                                  counties: e.target.value
                                    .split(",")
                                    .map((s) => s.trim())
                                    .filter(Boolean),
                                };
                                return next;
                              })
                            }
                          />
                        </label>
                      </div>

                      <div className="mt-4 flex justify-end">
                        <Button onClick={() => saveOrg(org)} disabled={busy}>
                          {t("save")}
                        </Button>
                      </div>
                    </li>
                  ))}
                </ul>
              )
            ) : (
              <div className="text-sm text-[color:var(--color-muted)]">Loading…</div>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
}
