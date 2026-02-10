type Bucket = { resetAtMs: number; count: number };

declare global {
  var __regshieldRateLimitStore: Map<string, Bucket> | undefined;
}

function store(): Map<string, Bucket> {
  if (!globalThis.__regshieldRateLimitStore) globalThis.__regshieldRateLimitStore = new Map();
  return globalThis.__regshieldRateLimitStore;
}

export function getClientIp(req: Request): string {
  const xff = req.headers.get("x-forwarded-for");
  if (xff) return xff.split(",")[0]?.trim() || "unknown";
  const real = req.headers.get("x-real-ip");
  if (real) return real.trim() || "unknown";
  return "unknown";
}

export function checkRateLimit(params: {
  key: string;
  limit: number;
  windowMs: number;
  nowMs?: number;
}): { ok: boolean; remaining: number; resetAtMs: number } {
  const now = params.nowMs ?? Date.now();
  const s = store();

  // Opportunistic cleanup to keep memory bounded.
  if (s.size > 10_000) {
    for (const [k, v] of s) {
      if (v.resetAtMs <= now) s.delete(k);
    }
  }

  const existing = s.get(params.key);
  if (!existing || existing.resetAtMs <= now) {
    const resetAtMs = now + params.windowMs;
    s.set(params.key, { resetAtMs, count: 1 });
    return { ok: true, remaining: Math.max(0, params.limit - 1), resetAtMs };
  }

  if (existing.count >= params.limit) {
    return { ok: false, remaining: 0, resetAtMs: existing.resetAtMs };
  }

  existing.count += 1;
  s.set(params.key, existing);
  return { ok: true, remaining: Math.max(0, params.limit - existing.count), resetAtMs: existing.resetAtMs };
}
