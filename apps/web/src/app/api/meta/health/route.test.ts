// @vitest-environment node
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const queryRowsMock = vi.fn();
const getServerEnvMock = vi.fn();
const createClientMock = vi.fn();

vi.mock("@/server/db", () => ({ queryRows: queryRowsMock }));
vi.mock("@/server/env", () => ({ getServerEnv: getServerEnvMock }));
vi.mock("@supabase/supabase-js", () => ({ createClient: createClientMock }));

async function importRoute() {
  const mod = await import("./route");
  return mod as typeof import("./route");
}

function makeSupabaseQuery(maybeSingle: ReturnType<typeof vi.fn>) {
  const chain = {
    select: vi.fn(),
    eq: vi.fn(),
    order: vi.fn(),
    limit: vi.fn(),
    maybeSingle,
  };
  chain.select.mockReturnValue(chain);
  chain.eq.mockReturnValue(chain);
  chain.order.mockReturnValue(chain);
  chain.limit.mockReturnValue(chain);
  return chain;
}

describe("/api/meta/health", () => {
  const envSnapshot = { ...process.env };

  beforeEach(() => {
    vi.resetModules();
    vi.clearAllMocks();
    process.env = { ...envSnapshot };
  });

  afterEach(() => {
    process.env = { ...envSnapshot };
  });

  it("returns postgres mode with compatibility fields", async () => {
    getServerEnvMock.mockReturnValue({
      DATABASE_URL: "postgres://example",
      OPENAI_API_KEY: "key",
      OPENAI_VECTOR_STORE_ID: "vs_123",
      OPENAI_ENABLE_WEB_SEARCH: true,
      OPENAI_WEB_SEARCH_EXTERNAL: false,
      OPENAI_RAG_STRICT: true,
    });
    queryRowsMock.mockImplementation(async (sql: string) => {
      if (sql.includes("orgs_by_state_city")) {
        return [{ ended_at: "2026-02-20T00:00:00.000Z", source_sha256: "org_sha" }];
      }
      return [{ ended_at: "2026-02-21T00:00:00.000Z", source_sha256: "rep_sha" }];
    });

    const { GET } = await importRoute();
    const res = await GET();
    const json = await res.json();

    expect(json).toMatchObject({
      ok: true,
      mode: "postgres",
      db_configured: true,
      supabase_configured: false,
      ask_configured: true,
      rag_configured: true,
      web_search_enabled: true,
      web_search_external: false,
      rag_strict: true,
    });
    expect(json.ra_import.orgs_by_state_city.source_sha256).toBe("org_sha");
    expect(json.ra_import.reps_roster.source_sha256).toBe("rep_sha");
  });

  it("returns postgres failure with db_error and compatibility fields", async () => {
    getServerEnvMock.mockReturnValue({
      DATABASE_URL: "postgres://example",
      OPENAI_API_KEY: "key",
      OPENAI_VECTOR_STORE_ID: undefined,
      OPENAI_ENABLE_WEB_SEARCH: false,
      OPENAI_WEB_SEARCH_EXTERNAL: false,
      OPENAI_RAG_STRICT: false,
    });
    queryRowsMock.mockRejectedValueOnce(new Error("db boom"));

    const { GET } = await importRoute();
    const res = await GET();
    const json = await res.json();

    expect(json).toMatchObject({
      ok: false,
      mode: "postgres",
      db_configured: true,
      supabase_configured: false,
      ask_configured: true,
      rag_configured: false,
      web_search_enabled: false,
      web_search_external: false,
      rag_strict: false,
      error: "Database query failed",
    });
    expect(String(json.db_error)).toContain("db boom");
  });

  it("returns sample mode with compatibility fields when Supabase env is missing", async () => {
    delete process.env.NEXT_PUBLIC_SUPABASE_URL;
    delete process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY;
    getServerEnvMock.mockReturnValue({
      DATABASE_URL: undefined,
      OPENAI_API_KEY: undefined,
      OPENAI_VECTOR_STORE_ID: undefined,
      OPENAI_ENABLE_WEB_SEARCH: true,
      OPENAI_WEB_SEARCH_EXTERNAL: false,
      OPENAI_RAG_STRICT: true,
    });

    const { GET } = await importRoute();
    const res = await GET();
    const json = await res.json();

    expect(json).toMatchObject({
      ok: true,
      mode: "sample",
      db_configured: false,
      supabase_configured: false,
      ask_configured: false,
      rag_configured: false,
      web_search_enabled: true,
      web_search_external: false,
      rag_strict: true,
      ra_import: null,
    });
  });

  it("returns supabase mode with compatibility fields", async () => {
    process.env.NEXT_PUBLIC_SUPABASE_URL = "https://supabase.example";
    process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY = "anon";
    getServerEnvMock.mockReturnValue({
      DATABASE_URL: undefined,
      OPENAI_API_KEY: "key",
      OPENAI_VECTOR_STORE_ID: "vs_123",
      OPENAI_ENABLE_WEB_SEARCH: true,
      OPENAI_WEB_SEARCH_EXTERNAL: false,
      OPENAI_RAG_STRICT: true,
    });

    const maybeSingle = vi
      .fn()
      .mockResolvedValueOnce({ error: null, data: { ended_at: "2026-02-20T00:00:00.000Z", source_sha256: "org_sha" } })
      .mockResolvedValueOnce({ error: null, data: { ended_at: "2026-02-21T00:00:00.000Z", source_sha256: "rep_sha" } });
    createClientMock.mockReturnValue({
      from: vi.fn(() => makeSupabaseQuery(maybeSingle)),
    });

    const { GET } = await importRoute();
    const res = await GET();
    const json = await res.json();

    expect(json).toMatchObject({
      ok: true,
      mode: "supabase",
      db_configured: false,
      supabase_configured: true,
      ask_configured: true,
      rag_configured: true,
      web_search_enabled: true,
      web_search_external: false,
      rag_strict: true,
    });
    expect(json.ra_import.orgs_by_state_city.source_sha256).toBe("org_sha");
    expect(json.ra_import.reps_roster.source_sha256).toBe("rep_sha");
  });
});
