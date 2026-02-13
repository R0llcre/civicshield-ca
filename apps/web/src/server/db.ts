import { Pool } from "pg";

import { getServerEnv } from "@/server/env";

let pool: Pool | null = null;

function shouldUseSsl(dbUrl: string) {
  try {
    const u = new URL(dbUrl);
    const sslmode = u.searchParams.get("sslmode");
    if (sslmode && sslmode !== "disable") return true;
  } catch {
    // ignore
  }
  return process.env.DATABASE_SSL === "true";
}

export function getPool() {
  const { DATABASE_URL } = getServerEnv();
  if (!DATABASE_URL) return null;

  if (!pool) {
    pool = new Pool({
      connectionString: DATABASE_URL,
      ssl: shouldUseSsl(DATABASE_URL) ? { rejectUnauthorized: false } : undefined,
      max: 10,
      idleTimeoutMillis: 30_000,
      connectionTimeoutMillis: 5_000,
    });
  }

  return pool;
}

export async function queryRows<T = unknown>(text: string, params: unknown[] = []) {
  const p = getPool();
  if (!p) throw new Error("DATABASE_URL not configured");
  const res = await p.query(text, params);
  return res.rows as T[];
}
