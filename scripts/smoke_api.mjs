import process from "node:process";

function usage() {
  // Keep it simple and copy/paste friendly.
  console.log('Usage: node scripts/smoke_api.mjs --base-url "https://<host>"');
}

function parseArgs(argv) {
  const out = { baseUrl: null };
  for (let i = 2; i < argv.length; i++) {
    const a = argv[i];
    if (a === "--base-url") {
      out.baseUrl = argv[i + 1] ?? null;
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

async function main() {
  const { baseUrl } = parseArgs(process.argv);
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
    return "health ok";
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

