import { beforeEach, describe, expect, it } from "vitest";

import { checkRateLimit, getClientIp } from "@/lib/rateLimit";

beforeEach(() => {
  globalThis.__regshieldRateLimitStore = new Map();
});

describe("getClientIp", () => {
  it("uses the first x-forwarded-for value", () => {
    const req = new Request("https://example.com/api", {
      headers: { "x-forwarded-for": "1.1.1.1, 2.2.2.2" },
    });
    expect(getClientIp(req)).toBe("1.1.1.1");
  });

  it("falls back to x-real-ip", () => {
    const req = new Request("https://example.com/api", {
      headers: { "x-real-ip": "203.0.113.10" },
    });
    expect(getClientIp(req)).toBe("203.0.113.10");
  });
});

describe("checkRateLimit", () => {
  it("limits within a window and resets after the window", () => {
    const limit = 2;
    const windowMs = 100;

    const r1 = checkRateLimit({ key: "k", limit, windowMs, nowMs: 1000 });
    expect(r1.ok).toBe(true);
    expect(r1.remaining).toBe(1);
    expect(r1.resetAtMs).toBe(1100);

    const r2 = checkRateLimit({ key: "k", limit, windowMs, nowMs: 1001 });
    expect(r2.ok).toBe(true);
    expect(r2.remaining).toBe(0);
    expect(r2.resetAtMs).toBe(1100);

    const r3 = checkRateLimit({ key: "k", limit, windowMs, nowMs: 1002 });
    expect(r3.ok).toBe(false);
    expect(r3.remaining).toBe(0);
    expect(r3.resetAtMs).toBe(1100);

    const r4 = checkRateLimit({ key: "k", limit, windowMs, nowMs: 1100 });
    expect(r4.ok).toBe(true);
    expect(r4.remaining).toBe(1);
    expect(r4.resetAtMs).toBe(1200);
  });
});
