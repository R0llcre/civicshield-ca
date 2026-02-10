import { createClient } from "@supabase/supabase-js";
import { NextResponse } from "next/server";
import { z } from "zod";

import { SAMPLE_RA, type RaEntry } from "@/data/ra_sample";

const requestSchema = z.object({
  name: z.string().min(1),
  state: z.string().min(2).max(2).optional(),
});

function scoreMatch(q: string, value: string) {
  const needle = q.toLowerCase();
  const hay = value.toLowerCase();
  if (hay === needle) return 100;
  if (hay.includes(needle)) return 70;
  // crude token overlap fallback
  const n = new Set(needle.split(/\s+/).filter(Boolean));
  const h = new Set(hay.split(/\s+/).filter(Boolean));
  let inter = 0;
  for (const tok of n) if (h.has(tok)) inter++;
  return inter * 10;
}

function annotateScore(q: string, r: RaEntry): RaEntry {
  const sName = scoreMatch(q, r.name);
  const sOrg = r.organization_name ? scoreMatch(q, r.organization_name) : 0;
  const score = Math.max(sName, sOrg);
  const matched_on: RaEntry["matched_on"] = sName >= sOrg ? "name" : "organization_name";
  return { ...r, score, matched_on };
}

async function handleVerify(name: string, state: string) {
  const trimmedName = name.trim();
  const trimmedState = state.trim().toUpperCase();

  if (!trimmedName) return NextResponse.json({ error: "Missing name" }, { status: 400 });

  const url = process.env.NEXT_PUBLIC_SUPABASE_URL;
  const key = process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;

  // Fallback: sample data only.
  if (!url || !key) {
    const results = SAMPLE_RA.slice()
      .filter((r) => {
        const hay = `${r.name} ${r.organization_name ?? ""}`.toLowerCase();
        const ok = hay.includes(trimmedName.toLowerCase());
        if (!ok) return false;
        if (r.entry_type === "organization") return r.state === trimmedState;
        return true;
      })
      .map((r) => annotateScore(trimmedName, r))
      .sort((a, b) => (b.score ?? 0) - (a.score ?? 0))
      .slice(0, 20);

    return NextResponse.json({
      results,
      data_freshness: null,
    });
  }

  const supabase = createClient(url, key, { auth: { persistSession: false } });

  // Best-effort freshness (table may not exist yet in early setups).
  type RunRow = { ended_at: string | null; source_sha256: string | null };
  let freshness: { ended_at: string | null; source_sha256: string | null } | null = null;
  let orgSha: string | null = null;
  let repSha: string | null = null;
  try {
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

    const orgData: RunRow | null = !orgRun.error && orgRun.data ? (orgRun.data as unknown as RunRow) : null;
    const repData: RunRow | null = !repRun.error && repRun.data ? (repRun.data as unknown as RunRow) : null;

    orgSha = orgData?.source_sha256 ?? null;
    repSha = repData?.source_sha256 ?? null;

    // Expose a single freshness marker (max ended_at).
    const orgEnded = orgData?.ended_at ?? null;
    const repEnded = repData?.ended_at ?? null;

    const pick =
      orgEnded && repEnded ? (new Date(orgEnded) > new Date(repEnded) ? ["org", orgEnded, orgSha] : ["rep", repEnded, repSha]) : orgEnded ? ["org", orgEnded, orgSha] : repEnded ? ["rep", repEnded, repSha] : null;

    if (pick) {
      freshness = { ended_at: pick[1] as string, source_sha256: (pick[2] as string | null) ?? null };
    }
  } catch {
    // ignore
  }

  // Search recognized orgs by state + reps nationwide.
  const q = trimmedName;

  // Prefer similarity-ranked search via RPC (pg_trgm). Fall back to ILIKE if unavailable.
  try {
    const rpc = await supabase.rpc("search_ra_entries", {
      q,
      state: trimmedState,
      lim: 20,
    });
    if (!rpc.error && rpc.data) {
      return NextResponse.json({
        results: (rpc.data as unknown as RaEntry[]) ?? [],
        data_freshness: freshness,
      });
    }
  } catch {
    // ignore and fall back
  }

  let orgQuery = supabase
    .from("ra_entry")
    .select(
      "entry_type,name,organization_name,city,state,address,phone,website,accreditation_level,status,expires_at,pending_renewal,source_url,source_sha256",
    )
    .eq("dataset", "orgs_by_state_city")
    .eq("entry_type", "organization")
    .eq("state", trimmedState)
    .ilike("name", `%${q}%`);
  if (orgSha) orgQuery = orgQuery.eq("source_sha256", orgSha);
  const orgRes = await orgQuery.limit(15);

  let repQuery = supabase
    .from("ra_entry")
    .select(
      "entry_type,name,organization_name,city,state,address,phone,website,accreditation_level,status,expires_at,pending_renewal,source_url,source_sha256",
    )
    .eq("dataset", "reps_roster")
    .eq("entry_type", "representative")
    .or(`name.ilike.%${q}%,organization_name.ilike.%${q}%`);
  if (repSha) repQuery = repQuery.eq("source_sha256", repSha);
  const repRes = await repQuery.limit(15);

  const resultsRaw: RaEntry[] = [
    ...(((repRes.data ?? []) as unknown as RaEntry[])),
    ...(((orgRes.data ?? []) as unknown as RaEntry[])),
  ];

  const results = resultsRaw
    .slice()
    .map((r) => annotateScore(q, r))
    .sort((a, b) => (b.score ?? 0) - (a.score ?? 0))
    .slice(0, 20);

  return NextResponse.json({
    results,
    data_freshness: freshness,
  });
}

export async function GET(req: Request) {
  const { searchParams } = new URL(req.url);
  const parsed = requestSchema.safeParse({
    name: (searchParams.get("name") ?? "").trim(),
    state: (searchParams.get("state") ?? "CA").trim().toUpperCase(),
  });
  if (!parsed.success) return NextResponse.json({ error: "Invalid request" }, { status: 400 });

  return handleVerify(parsed.data.name, parsed.data.state ?? "CA");
}

export async function POST(req: Request) {
  const body = await req.json().catch(() => null);
  const parsed = requestSchema.safeParse(body);
  if (!parsed.success) return NextResponse.json({ error: "Invalid request" }, { status: 400 });

  return handleVerify(parsed.data.name, parsed.data.state ?? "CA");
}
