// @vitest-environment node
import { describe, expect, it, vi } from "vitest";

async function importRoute() {
  const mod = await import("./route");
  return mod as typeof import("./route");
}

describe("/api/ask", () => {
  it("returns 501 when OPENAI_API_KEY is missing", async () => {
    vi.resetModules();
    delete process.env.OPENAI_API_KEY;

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

  it("filters citations to allowlisted domains and returns structured output", async () => {
    vi.resetModules();
    process.env.OPENAI_API_KEY = "test-key";
    process.env.OPENAI_MODEL = "gpt-test";

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
      vi.fn(async () => {
        return new Response(
          JSON.stringify({
            output: [
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
});
