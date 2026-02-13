import { createClient } from "@supabase/supabase-js";
import { NextResponse } from "next/server";

import { SAMPLE_AID_ORGS, type AidOrg } from "@/data/aid_org_sample";
import { checkRateLimit, getClientIp } from "@/lib/rateLimit";
import { queryRows } from "@/server/db";
import { getServerEnv } from "@/server/env";

export async function GET(req: Request) {
  const { searchParams } = new URL(req.url);
  const county = (searchParams.get("county") ?? "").trim();
  const lang = (searchParams.get("lang") ?? "").trim();
  const service = (searchParams.get("service") ?? "").trim();

  const ip = getClientIp(req);
  const limit = 60;
  const windowMs = 60_000;
  const rl = ip === "unknown" ? null : checkRateLimit({ key: `directory:${ip}`, limit, windowMs });
  if (rl && !rl.ok) {
    const retryAfterSec = Math.max(1, Math.ceil((rl.resetAtMs - Date.now()) / 1000));
    return NextResponse.json(
      { error: "Too many requests" },
      {
        status: 429,
        headers: {
          "Retry-After": String(retryAfterSec),
          "X-RateLimit-Limit": String(limit),
          "X-RateLimit-Remaining": "0",
          "X-RateLimit-Reset": String(Math.floor(rl.resetAtMs / 1000)),
        },
      },
    );
  }

  const { DATABASE_URL } = getServerEnv();
  if (DATABASE_URL) {
    const where: string[] = [];
    const params: unknown[] = [];
    if (county) {
      params.push([county]);
      where.push(`counties @> $${params.length}::text[]`);
    }
    if (lang) {
      params.push([lang]);
      where.push(`languages @> $${params.length}::text[]`);
    }
    if (service) {
      params.push([service]);
      where.push(`services @> $${params.length}::text[]`);
    }

    const sql = [
      "select id,name,website,phone,email,counties,languages,services,intake_status,next_clinic_at,eta_callback_days,source_url,verified_at,verified_by,updated_at",
      "from public.aid_org",
      where.length ? `where ${where.join(" and ")}` : "",
      "limit 50",
    ]
      .filter(Boolean)
      .join(" ");

    const rows = await queryRows<AidOrg>(sql, params);
    const out = NextResponse.json({ results: rows ?? [] });
    if (rl) {
      out.headers.set("X-RateLimit-Limit", String(limit));
      out.headers.set("X-RateLimit-Remaining", String(rl.remaining));
      out.headers.set("X-RateLimit-Reset", String(Math.floor(rl.resetAtMs / 1000)));
    }
    return out;
  }

  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const key = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;

  if (!url || !key) {
    const results = SAMPLE_AID_ORGS.filter((o) => {
      if (county && !o.counties.includes(county)) return false;
      if (lang && !o.languages.includes(lang)) return false;
      if (service && !o.services.includes(service)) return false;
      return true;
    });
    const res = NextResponse.json({ results });
    if (rl) {
      res.headers.set("X-RateLimit-Limit", String(limit));
      res.headers.set("X-RateLimit-Remaining", String(rl.remaining));
      res.headers.set("X-RateLimit-Reset", String(Math.floor(rl.resetAtMs / 1000)));
    }
    return res;
  }

  const supabase = createClient(url, key, { auth: { persistSession: false } });

  let q = supabase
    .from("aid_org")
    .select(
      "id,name,website,phone,email,counties,languages,services,intake_status,next_clinic_at,eta_callback_days,source_url,verified_at,verified_by,updated_at",
    )
    .limit(50);

  if (county) q = q.contains("counties", [county]);
  if (lang) q = q.contains("languages", [lang]);
  if (service) q = q.contains("services", [service]);

  const res = await q;
  if (res.error) return NextResponse.json({ error: res.error.message }, { status: 500 });

  const out = NextResponse.json({ results: (res.data as unknown as AidOrg[]) ?? [] });
  if (rl) {
    out.headers.set("X-RateLimit-Limit", String(limit));
    out.headers.set("X-RateLimit-Remaining", String(rl.remaining));
    out.headers.set("X-RateLimit-Reset", String(Math.floor(rl.resetAtMs / 1000)));
  }
  return out;
}
