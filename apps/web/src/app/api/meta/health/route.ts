import { createClient } from "@supabase/supabase-js";
import { NextResponse } from "next/server";

export async function GET() {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const key = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;

  if (!url || !key) {
    return NextResponse.json({
      ok: true,
      supabase_configured: false,
      ra_import: null,
    });
  }

  const supabase = createClient(url, key, { auth: { persistSession: false } });

  const [orgRun, repRun] = await Promise.all([
    supabase
      .from("ra_import_run")
      .select("ended_at,source_sha256")
      .eq("status", "success")
      .eq("dataset", "orgs_by_state_city")
      .order("ended_at", { ascending: false })
      .limit(1)
      .maybeSingle(),
    supabase
      .from("ra_import_run")
      .select("ended_at,source_sha256")
      .eq("status", "success")
      .eq("dataset", "reps_roster")
      .order("ended_at", { ascending: false })
      .limit(1)
      .maybeSingle(),
  ]);

  return NextResponse.json({
    ok: true,
    supabase_configured: true,
    ra_import: {
      orgs_by_state_city: orgRun.error ? null : orgRun.data,
      reps_roster: repRun.error ? null : repRun.data,
    },
  });
}

