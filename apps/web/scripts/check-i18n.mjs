import fs from "node:fs";
import path from "node:path";
import process from "node:process";

function flattenKeys(obj, prefix = "") {
  const out = new Set();
  if (obj && typeof obj === "object" && !Array.isArray(obj)) {
    for (const [k, v] of Object.entries(obj)) {
      const next = prefix ? `${prefix}.${k}` : k;
      if (v && typeof v === "object" && !Array.isArray(v)) {
        for (const x of flattenKeys(v, next)) out.add(x);
      } else {
        out.add(next);
      }
    }
  }
  return out;
}

function loadJson(p) {
  return JSON.parse(fs.readFileSync(p, "utf8"));
}

const root = process.cwd();
const messagesDir = path.join(root, "messages");

const locales = ["en", "es", "zh-Hans"];
const baseLocale = "en";

const basePath = path.join(messagesDir, `${baseLocale}.json`);
const base = loadJson(basePath);
const baseKeys = flattenKeys(base);

let ok = true;
for (const locale of locales) {
  const p = path.join(messagesDir, `${locale}.json`);
  const data = loadJson(p);
  const keys = flattenKeys(data);

  const missing = [...baseKeys].filter((k) => !keys.has(k));
  if (missing.length) {
    ok = false;
    console.error(`\\n[i18n] Missing keys in ${locale}.json (${missing.length}):`);
    for (const k of missing.slice(0, 80)) console.error(`- ${k}`);
    if (missing.length > 80) console.error(`... (${missing.length - 80} more)`);
  }
}

if (!ok) process.exit(1);
console.log("[i18n] OK");

