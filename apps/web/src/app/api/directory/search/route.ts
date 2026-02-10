import { createClient } from "@supabase/supabase-js";
import { NextResponse } from "next/server";

import { SAMPLE_AID_ORGS, type AidOrg } from "@/data/aid_org_sample";

export async function GET(req: Request) {
  const { searchParams } = new URL(req.url);
  const county = (searchParams.get("county") ?? "").trim();
  const lang = (searchParams.get("lang") ?? "").trim();
  const service = (searchParams.get("service") ?? "").trim();

  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const key = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;

  if (!url || !key) {
    const results = SAMPLE_AID_ORGS.filter((o) => {
      if (county && !o.counties.includes(county)) return false;
      if (lang && !o.languages.includes(lang)) return false;
      if (service && !o.services.includes(service)) return false;
      return true;
    });
    return NextResponse.json({ results });
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

  return NextResponse.json({ results: (res.data as unknown as AidOrg[]) ?? [] });
}
