import { createClient } from "@supabase/supabase-js";
import { NextResponse } from "next/server";

import { getServerEnv } from "@/server/env";
import { queryRows } from "@/server/db";

export async function GET() {
  const { DATABASE_URL } = getServerEnv();

  if (DATABASE_URL) {
    try {
      const [orgRun, repRun] = await Promise.all([
        queryRows<{ ended_at: string | null; source_sha256: string | null }>(
          "select ended_at, source_sha256 from public.ra_import_run where status = 'success' and dataset = 'orgs_by_state_city' order by ended_at desc nulls last limit 1",
        ).then((r) => r[0] ?? null),
        queryRows<{ ended_at: string | null; source_sha256: string | null }>(
          "select ended_at, source_sha256 from public.ra_import_run where status = 'success' and dataset = 'reps_roster' order by ended_at desc nulls last limit 1",
        ).then((r) => r[0] ?? null),
      ]);

      return NextResponse.json({
        ok: true,
        mode: "postgres",
        db_configured: true,
        supabase_configured: false,
        ra_import: {
          orgs_by_state_city: orgRun,
          reps_roster: repRun,
        },
      });
    } catch (e) {
      return NextResponse.json({
        ok: false,
        mode: "postgres",
        db_configured: true,
        db_error: e instanceof Error ? e.message : String(e),
      });
    }
  }

  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const key = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;

  if (!url || !key) {
    return NextResponse.json({
      ok: true,
      mode: "sample",
      supabase_configured: false,
      db_configured: false,
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
    mode: "supabase",
    supabase_configured: true,
    db_configured: false,
    ra_import: {
      orgs_by_state_city: orgRun.error ? null : orgRun.data,
      reps_roster: repRun.error ? null : repRun.data,
    },
  });
}
