#!/usr/bin/env node
/* eslint-disable no-console */

import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const ROOT_DIR = path.resolve(__dirname, "..");

const KB_DIR = path.join(ROOT_DIR, "rag", "kb");
const TMP_DIR = path.join(ROOT_DIR, ".tmp");
const KEY_FILE = path.join(TMP_DIR, "openai_api_key.txt");
const VECTOR_STORE_ID_FILE = path.join(TMP_DIR, "openai_vector_store_id.txt");
const LAST_FILE_ID_FILE = path.join(TMP_DIR, "openai_kb_file_id.txt");
const BUNDLE_FILE = path.join(TMP_DIR, "regshield_kb_bundle.md");

const VECTOR_STORE_NAME = "regshieldca_kb_staging_v1";

// Keep consistent with apps/web/src/app/api/ask/route.ts
const ALLOWED_CITATION_DOMAINS = [
  "uscis.gov",
  "justice.gov",
  "eoir.justice.gov",
  "ca.gov",
  "calbar.ca.gov",
  "sos.ca.gov",
  "lawhelpca.org",
  "immigrationadvocates.org",
];

function usage() {
  console.log(
    [
      "Usage: node scripts/rag_sync_openai_vector_store.mjs",
      "",
      "This script bundles rag/kb/*.md and syncs it into an OpenAI Vector Store for Ask RAG.",
      "",
      "Inputs (local, gitignored):",
      `- ${KEY_FILE} (OpenAI API key)`,
      "",
      "Outputs (local, gitignored):",
      `- ${VECTOR_STORE_ID_FILE} (vector store id)`,
      `- ${LAST_FILE_ID_FILE} (last uploaded bundle file id)`,
      `- ${BUNDLE_FILE} (bundle markdown)`,
      "",
      "Notes:",
      "- Never paste the key into chat/PRs/issues. Rotate if it was ever leaked.",
    ].join("\n"),
  );
}

function isAllowedUrl(raw) {
  try {
    const u = new URL(raw);
    if (u.protocol !== "https:" && u.protocol !== "http:") return false;
    const host = u.hostname.toLowerCase();
    return ALLOWED_CITATION_DOMAINS.some((d) => host === d || host.endsWith(`.${d}`));
  } catch {
    return false;
  }
}

async function fileExists(p) {
  try {
    await fs.access(p);
    return true;
  } catch {
    return false;
  }
}

async function readTrimmed(p) {
  const text = await fs.readFile(p, "utf8");
  return text.trim();
}

function sleep(ms) {
  return new Promise((r) => setTimeout(r, ms));
}

function openAiErrorMessage(data) {
  if (!data || typeof data !== "object") return null;
  const err = data.error;
  if (!err || typeof err !== "object") return null;
  const msg = err.message;
  return typeof msg === "string" ? msg : null;
}

async function openaiJson(url, { method = "GET", headers = {}, body = undefined, apiKey }) {
  const res = await fetch(url, {
    method,
    headers: {
      authorization: `Bearer ${apiKey}`,
      ...headers,
    },
    body,
    signal: AbortSignal.timeout(60_000),
  });
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const msg = openAiErrorMessage(data) ?? `HTTP ${res.status}`;
    throw new Error(`${method} ${url} failed: ${msg}`);
  }
  return data;
}

async function ensureTmpDir() {
  await fs.mkdir(TMP_DIR, { recursive: true });
}

async function loadApiKey() {
  const envKey = (process.env.OPENAI_API_KEY ?? "").trim();
  if (envKey) return envKey;
  if (!(await fileExists(KEY_FILE))) {
    console.error(`Missing key file: ${KEY_FILE}`);
    console.error("Create a NEW key and store it in that file (one line).");
    process.exit(2);
  }
  const key = await readTrimmed(KEY_FILE);
  if (!key) {
    console.error(`Key file is empty: ${KEY_FILE}`);
    process.exit(2);
  }
  if (!key.startsWith("sk-")) {
    console.error("OpenAI key does not look valid (expected prefix sk-...).");
    process.exit(2);
  }
  return key;
}

async function bundleKb() {
  if (!(await fileExists(KB_DIR))) {
    console.error(`KB directory not found: ${KB_DIR}`);
    process.exit(2);
  }
  const files = (await fs.readdir(KB_DIR)).filter((f) => f.endsWith(".md")).sort();
  if (!files.length) {
    console.error(`No KB files found under: ${KB_DIR}`);
    process.exit(2);
  }

  const parts = [];
  parts.push(`# RegShield CA Knowledge Base Bundle\\n`);
  parts.push(`Generated at: ${new Date().toISOString()}\\n`);

  const allUrls = new Set();

  for (const f of files) {
    const full = path.join(KB_DIR, f);
    const content = await fs.readFile(full, "utf8");
    parts.push(`\\n---\\n\\n`);
    parts.push(`## Source file: ${f}\\n\\n`);
    parts.push(content.trim());
    parts.push("\\n");

    for (const m of content.matchAll(/https?:\/\/[^\s)\]]+/g)) {
      allUrls.add(m[0]);
    }
  }

  const invalid = [...allUrls].filter((u) => !isAllowedUrl(u));
  if (invalid.length) {
    console.error("KB contains non-allowlisted URLs. Refusing to upload:");
    for (const u of invalid.slice(0, 50)) console.error(`- ${u}`);
    process.exit(2);
  }

  const out = parts.join("");
  await fs.writeFile(BUNDLE_FILE, out, "utf8");
  console.log(`KB bundle written: ${BUNDLE_FILE}`);
  console.log(`KB files: ${files.length}`);
  console.log(`Unique URLs (allowlisted): ${allUrls.size}`);
}

async function ensureVectorStoreId(apiKey) {
  if (await fileExists(VECTOR_STORE_ID_FILE)) {
    const id = await readTrimmed(VECTOR_STORE_ID_FILE);
    if (id) return id;
  }

  console.log("Creating vector store...");
  const vs = await openaiJson("https://api.openai.com/v1/vector_stores", {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({
      name: VECTOR_STORE_NAME,
      // Optional TTL to avoid long-running costs for staging.
      // Delete the file to force recreation if it expires.
      expires_after: { anchor: "last_active_at", days: 30 },
    }),
    apiKey,
  });

  const id = String(vs.id ?? "").trim();
  if (!id) throw new Error("Vector store create response missing id");

  await fs.writeFile(VECTOR_STORE_ID_FILE, `${id}\\n`, "utf8");
  console.log(`Vector store created: ${id}`);
  return id;
}

async function uploadBundleFile(apiKey) {
  const content = await fs.readFile(BUNDLE_FILE, "utf8");
  const blob = new Blob([content], { type: "text/markdown" });
  const form = new FormData();
  form.set("purpose", "assistants");
  form.set("file", blob, "regshield_kb_bundle.md");

  const res = await fetch("https://api.openai.com/v1/files", {
    method: "POST",
    headers: { authorization: `Bearer ${apiKey}` },
    body: form,
    signal: AbortSignal.timeout(60_000),
  });
  const data = await res.json().catch(() => null);
  if (!res.ok) {
    const msg = openAiErrorMessage(data) ?? `HTTP ${res.status}`;
    throw new Error(`File upload failed: ${msg}`);
  }
  const fileId = String(data.id ?? "").trim();
  if (!fileId) throw new Error("File upload response missing id");
  console.log(`Uploaded file: ${fileId}`);
  return fileId;
}

async function detachAndDeletePrevious(apiKey, vectorStoreId) {
  if (!(await fileExists(LAST_FILE_ID_FILE))) return;
  const oldId = await readTrimmed(LAST_FILE_ID_FILE);
  if (!oldId) return;

  // Best-effort cleanup; ignore errors.
  const detachUrl = `https://api.openai.com/v1/vector_stores/${vectorStoreId}/files/${oldId}`;
  await fetch(detachUrl, { method: "DELETE", headers: { authorization: `Bearer ${apiKey}` } }).catch(() => {});
  const delUrl = `https://api.openai.com/v1/files/${oldId}`;
  await fetch(delUrl, { method: "DELETE", headers: { authorization: `Bearer ${apiKey}` } }).catch(() => {});
}

async function attachToVectorStore(apiKey, vectorStoreId, fileId) {
  console.log("Attaching file to vector store (file batch)...");
  const batch = await openaiJson(`https://api.openai.com/v1/vector_stores/${vectorStoreId}/file_batches`, {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ file_ids: [fileId] }),
    apiKey,
  });
  const batchId = String(batch.id ?? "").trim();
  if (!batchId) throw new Error("file_batch create response missing id");
  console.log(`File batch: ${batchId}`);

  const statusUrl = `https://api.openai.com/v1/vector_stores/${vectorStoreId}/file_batches/${batchId}`;
  const deadline = Date.now() + 5 * 60_000;
  while (true) {
    const st = await openaiJson(statusUrl, { method: "GET", apiKey });
    const status = String(st.status ?? "");
    if (status === "completed") break;
    if (status === "failed" || status === "cancelled") {
      throw new Error(`file_batch ${batchId} status=${status}`);
    }
    if (Date.now() > deadline) throw new Error("Timed out waiting for vector store ingestion");
    await sleep(2000);
  }

  await fs.writeFile(LAST_FILE_ID_FILE, `${fileId}\\n`, "utf8");
  console.log("Vector store ingestion completed.");
}

async function main() {
  if (process.argv.includes("-h") || process.argv.includes("--help")) {
    usage();
    process.exit(0);
  }

  await ensureTmpDir();
  const apiKey = await loadApiKey();

  await bundleKb();
  const vectorStoreId = await ensureVectorStoreId(apiKey);
  await detachAndDeletePrevious(apiKey, vectorStoreId);
  const fileId = await uploadBundleFile(apiKey);
  await attachToVectorStore(apiKey, vectorStoreId, fileId);

  console.log("");
  console.log("RAG sync complete.");
  console.log(`OPENAI_VECTOR_STORE_ID=${vectorStoreId}`);
}

await main();
