import process from "node:process";

function usage() {
  // Keep it simple and copy/paste friendly.
  console.log(
    [
      'Usage: node scripts/smoke_api.mjs --base-url "https://<host>" [--require-supabase] [--max-ra-age-hours 48]',
      "",
      "Options:",
      "  --base-url <url>          Required API base URL",
      "  --require-supabase        Fail if /api/meta/health reports supabase_configured=false",
      "  --max-ra-age-hours <int>  Fail if latest successful RA import is older than threshold",
    ].join("\n"),
  );
}

function parseArgs(argv) {
  const out = {
    baseUrl: process.env.STAGING_BASE_URL ?? null,
    requireSupabase: false,
    maxRaAgeHours: null,
  };

  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--base-url") {
      out.baseUrl = argv[i + 1] ?? null;
      i++;
      continue;
    }
    if (a === "--require-supabase") {
      out.requireSupabase = true;
      continue;
    }
    if (a === "--max-ra-age-hours") {
      const raw = argv[i + 1] ?? "";
      const parsed = Number.parseInt(raw, 10);
      if (!Number.isFinite(parsed) || parsed <= 0) {
        throw new Error(`Invalid --max-ra-age-hours value: ${raw}`);
      }
      out.maxRaAgeHours = parsed;
      i++;
      continue;
    }
    if (a === "-h" || a === "--help") {
      usage();
      process.exit(0);
    }
  }
  return out;
}

function joinUrl(baseUrl, path) {
  const base = baseUrl.endsWith("/") ? baseUrl.slice(0, -1) : baseUrl;
  return `${base}${path.startsWith("/") ? path : `/${path}`}`;
}

async function mustJson(res) {
  const text = await res.text();
  try {
    return JSON.parse(text);
  } catch {
    throw new Error(`Expected JSON, got: ${text.slice(0, 200)}`);
  }
}

function hoursSince(isoTs) {
  const thenMs = new Date(isoTs).getTime();
  if (!Number.isFinite(thenMs)) return Number.POSITIVE_INFINITY;
  return (Date.now() - thenMs) / (1000 * 60 * 60);
}

function getMostRecentEndedAt(healthBody) {
  const ra = healthBody?.ra_import;
  const orgTs = ra?.orgs_by_state_city?.ended_at ?? null;
  const repTs = ra?.reps_roster?.ended_at ?? null;
  if (!orgTs && !repTs) return null;
  if (!orgTs) return repTs;
  if (!repTs) return orgTs;
  return new Date(orgTs) > new Date(repTs) ? orgTs : repTs;
}

async function main() {
  const { baseUrl, requireSupabase, maxRaAgeHours } = parseArgs(process.argv);
  if (!baseUrl) {
    usage();
    process.exit(2);
  }

  const checks = [];

  // 1) health
  checks.push((async () => {
    const url = joinUrl(baseUrl, "/api/meta/health");
    const res = await fetch(url, { method: "GET" });
    if (!res.ok) throw new Error(`health ${res.status}`);
    const body = await mustJson(res);
    if (!body.ok) throw new Error("health body.ok != true");
    if (requireSupabase && !body.supabase_configured) {
      throw new Error("health supabase_configured=false");
    }

    if (maxRaAgeHours != null) {
      const newest = getMostRecentEndedAt(body);
      if (!newest) throw new Error("health missing ra_import freshness");
      const ageHours = hoursSince(newest);
      if (ageHours > maxRaAgeHours) {
        throw new Error(
          `ra freshness too old: ${ageHours.toFixed(1)}h > ${maxRaAgeHours}h (latest ended_at=${newest})`,
        );
      }
      return `health ok (ra_age_hours=${ageHours.toFixed(1)})`;
    }

    return `health ok${body.supabase_configured ? " (supabase=true)" : " (supabase=false)"}`;
  })());

  // 2) verify (POST to avoid query in URL)
  checks.push((async () => {
    const url = joinUrl(baseUrl, "/api/verify/ra");
    const res = await fetch(url, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify({ name: "Jane Doe", state: "CA" }),
    });
    if (!res.ok) throw new Error(`verify ${res.status}`);
    const body = await mustJson(res);
    if (!Array.isArray(body.results)) throw new Error("verify results not array");
    return `verify ok (results=${body.results.length})`;
  })());

  // 3) directory search
  checks.push((async () => {
    const url = joinUrl(baseUrl, "/api/directory/search?county=Los%20Angeles");
    const res = await fetch(url, { method: "GET" });
    if (!res.ok) throw new Error(`directory ${res.status}`);
    const body = await mustJson(res);
    if (!Array.isArray(body.results)) throw new Error("directory results not array");
    return `directory ok (results=${body.results.length})`;
  })());

  const settled = await Promise.allSettled(checks);
  let ok = true;
  for (const s of settled) {
    if (s.status === "fulfilled") {
      console.log(`[OK] ${s.value}`);
    } else {
      ok = false;
      console.error(`[FAIL] ${s.reason?.message ?? String(s.reason)}`);
    }
  }

  process.exit(ok ? 0 : 1);
}

await main();
