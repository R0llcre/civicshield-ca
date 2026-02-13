// @vitest-environment node
import { describe, expect, it, vi } from "vitest";

async function importRoute() {
  const mod = await import("./route");
  return mod as typeof import("./route");
}

type RequestBody = {
  model?: unknown;
  temperature?: unknown;
  reasoning?: unknown;
  tool_choice?: unknown;
  tools?: Array<Record<string, unknown>>;
  include?: unknown;
  text?: Record<string, unknown>;
};

function parseJsonBody(init: unknown): RequestBody {
  if (!init || typeof init !== "object") return {};
  const body = (init as Record<string, unknown>).body;
  if (typeof body !== "string") return {};
  return JSON.parse(body) as RequestBody;
}

describe("/api/ask", () => {
  it("returns 501 when OPENAI_API_KEY is missing", async () => {
    vi.resetModules();
    delete process.env.OPENAI_API_KEY;
    delete process.env.OPENAI_VECTOR_STORE_ID;

    const { POST } = await importRoute();
    const res = await POST(
      new Request("http://localhost/api/ask", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ locale: "en", messages: [{ role: "user", content: "Hi" }] }),
      }),
    );

    expect(res.status).toBe(501);
    const json = await res.json();
    expect(json.error).toMatch(/not configured/i);
  });

  it("returns 501 when OPENAI_VECTOR_STORE_ID is missing", async () => {
    vi.resetModules();
    process.env.OPENAI_API_KEY = "test-key";
    delete process.env.OPENAI_VECTOR_STORE_ID;

    const { POST } = await importRoute();
    const res = await POST(
      new Request("http://localhost/api/ask", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ locale: "en", messages: [{ role: "user", content: "Hi" }] }),
      }),
    );

    expect(res.status).toBe(501);
  });

  it("filters citations to allowlisted domains and returns structured output", async () => {
    vi.resetModules();
    process.env.OPENAI_API_KEY = "test-key";
    process.env.OPENAI_MODEL = "gpt-test";
    process.env.OPENAI_VECTOR_STORE_ID = "vs_test";

    const modelOut = {
      answer_markdown: "General information only.",
      citations: [
        { title: "USCIS", url: "https://www.uscis.gov/" },
        { title: "Bad", url: "https://example.com/" },
      ],
      suggested_actions: [
        { type: "open_verify", label: "Open Verify" },
        { type: "open_official_url", label: "Open USCIS", payload: { url: "https://www.uscis.gov/" } },
        { type: "open_official_url", label: "Open Bad", payload: { url: "https://evil.example.com/" } },
      ],
      disclaimer: "ignored",
    };

    vi.stubGlobal(
      "fetch",
      vi.fn(async (url: unknown, init: unknown) => {
        const body = parseJsonBody(init);
        expect(body.model).toBe("gpt-test");
        expect(body.temperature).toBeUndefined();
        expect(body.reasoning).toBeUndefined();
        expect(body.tool_choice).toBe("required");
        const tools = body.tools ?? [];
        expect(Array.isArray(tools)).toBe(true);
        expect(tools[0]?.type).toBe("file_search");
        expect(tools[0]?.vector_store_ids).toEqual(["vs_test"]);
        expect(Array.isArray(body.include)).toBe(true);
        expect(body.include).toContain("file_search_call.results");
        const text = body.text ?? {};
        const format =
          typeof text.format === "object" && text.format
            ? (text.format as Record<string, unknown>)
            : {};
        expect(format.type).toBe("json_schema");

        return new Response(
          JSON.stringify({
            output: [
              { type: "file_search_call", id: "fs_1", status: "completed", results: [] },
              {
                type: "message",
                role: "assistant",
                content: [{ type: "output_text", text: JSON.stringify(modelOut) }],
              },
            ],
          }),
          { status: 200, headers: { "content-type": "application/json" } },
        );
      }),
    );

    const { POST } = await importRoute();
    const res = await POST(
      new Request("http://localhost/api/ask", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ locale: "en", messages: [{ role: "user", content: "What is asylum?" }] }),
      }),
    );

    expect(res.status).toBe(200);
    const json = await res.json();
    expect(json.answer_markdown).toBe(modelOut.answer_markdown);
    expect(json.citations).toEqual([{ title: "USCIS", url: "https://www.uscis.gov/" }]);
    expect(json.suggested_actions).toEqual([
      { type: "open_verify", label: "Open Verify" },
      { type: "open_official_url", label: "Open USCIS", payload: { url: "https://www.uscis.gov/" } },
    ]);
    expect(json.disclaimer).toMatch(/not legal advice/i);

    vi.unstubAllGlobals();
  });

  it("falls back to authoritative web_search when RAG yields no allowlisted citations (strict)", async () => {
    vi.resetModules();
    process.env.OPENAI_API_KEY = "test-key";
    process.env.OPENAI_MODEL = "gpt-test";
    process.env.OPENAI_VECTOR_STORE_ID = "vs_test";
    process.env.OPENAI_RAG_STRICT = "true";
    process.env.OPENAI_ENABLE_WEB_SEARCH = "true";
    process.env.OPENAI_WEB_SEARCH_EXTERNAL = "false";

    const ragOut = {
      answer_markdown: "Here is an answer with bad citations.",
      citations: [{ title: "Bad", url: "https://example.com/" }],
      suggested_actions: [],
      disclaimer: "ignored",
    };
    const webOut = {
      answer_markdown: "Here is an answer with good citations.",
      citations: [{ title: "USCIS", url: "https://www.uscis.gov/" }],
      suggested_actions: [{ type: "open_help", label: "Open Help" }],
      disclaimer: "ignored",
    };

    const fetchMock = vi.fn(async (_url: unknown, init: unknown) => {
      const body = parseJsonBody(init);
      const toolType = (body.tools ?? [])[0]?.type;
      if (toolType === "file_search") {
        return new Response(
          JSON.stringify({
            output: [
              { type: "file_search_call", id: "fs_1", status: "completed", results: [] },
              { type: "message", role: "assistant", content: [{ type: "output_text", text: JSON.stringify(ragOut) }] },
            ],
          }),
          { status: 200, headers: { "content-type": "application/json" } },
        );
      }
      expect(toolType).toBe("web_search");
      const tool = (body.tools ?? [])[0] ?? {};
      const filters =
        typeof tool.filters === "object" && tool.filters
          ? (tool.filters as Record<string, unknown>)
          : {};
      expect(filters.allowed_domains).toBeTruthy();
      expect(tool.external_web_access).toBe(false);
      return new Response(
        JSON.stringify({
          output: [
            { type: "web_search_call", id: "ws_1", status: "completed", action: { sources: [] } },
            { type: "message", role: "assistant", content: [{ type: "output_text", text: JSON.stringify(webOut) }] },
          ],
        }),
        { status: 200, headers: { "content-type": "application/json" } },
      );
    });
    vi.stubGlobal("fetch", fetchMock);

    const { POST } = await importRoute();
    const res = await POST(
      new Request("http://localhost/api/ask", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ locale: "en", messages: [{ role: "user", content: "What is asylum?" }] }),
      }),
    );

    expect(res.status).toBe(200);
    const json = await res.json();
    expect(json.answer_markdown).toBe(webOut.answer_markdown);
    expect(json.citations).toEqual([{ title: "USCIS", url: "https://www.uscis.gov/" }]);
    expect(fetchMock).toHaveBeenCalledTimes(2);

    vi.unstubAllGlobals();
  });
});
