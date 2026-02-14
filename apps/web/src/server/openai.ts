import { getServerEnv } from "@/server/env";

export type AskChatMessage = {
  role: "user" | "assistant";
  content: string;
};

export type AskModelOutput = {
  answer_markdown: string;
  citations: Array<{ title: string; url: string }>;
  suggested_actions: Array<{
    type: "open_verify" | "open_help" | "open_official_url";
    label: string;
    payload?: Record<string, unknown>;
  }>;
  disclaimer: string;
};

function openAiErrorMessage(data: unknown): string | null {
  if (!data || typeof data !== "object") return null;
  const err = (data as Record<string, unknown>).error;
  if (!err || typeof err !== "object") return null;
  const msg = (err as Record<string, unknown>).message;
  return typeof msg === "string" ? msg : null;
}

function extractOutput(data: unknown): { text: string | null; output: unknown[] | null } {
  if (!data || typeof data !== "object") return { text: null, output: null };
  const anyData = data as Record<string, unknown>;
  const output = Array.isArray(anyData.output) ? (anyData.output as unknown[]) : null;
  if (typeof anyData.output_text === "string") {
    return { text: anyData.output_text, output };
  }

  if (!output) return { text: null, output: null };
  for (const item of output) {
    if (!item || typeof item !== "object") continue;
    const it = item as Record<string, unknown>;
    if (it.type !== "message") continue;
    const content = it.content;
    if (!Array.isArray(content)) continue;
    const texts = content
      .filter((c) => c && typeof c === "object" && (c as Record<string, unknown>).type === "output_text")
      .map((c) => (c as Record<string, unknown>).text)
      .filter((t): t is string => typeof t === "string");
    if (texts.length) return { text: texts.join(""), output };
  }
  return { text: null, output };
}

function outputHasType(output: unknown[] | null, type: string): boolean {
  if (!output) return false;
  for (const item of output) {
    if (!item || typeof item !== "object") continue;
    const it = item as Record<string, unknown>;
    if (it.type === type) return true;
  }
  return false;
}

export async function askOpenAI(params: {
  locale: "en" | "es" | "zh-Hans";
  messages: AskChatMessage[];
  schema: Record<string, unknown>;
  mode: "file_search" | "web_search";
  vectorStoreId?: string;
  allowedDomains?: string[];
  webSearchExternal?: boolean;
}): Promise<AskModelOutput> {
  const { OPENAI_API_KEY, OPENAI_MODEL } = getServerEnv();
  if (!OPENAI_API_KEY) throw new Error("OPENAI_API_KEY not configured");
  const isGpt5Family = OPENAI_MODEL.startsWith("gpt-5");

  const locale = params.locale;
  const allowedDomains = params.allowedDomains ?? [];
  const instructions = [
    "You are RegShield CA, an informational assistant for California (CA) and U.S. immigration topics.",
    "Do NOT provide legal advice. Do NOT make promises or guarantees. Encourage consulting a qualified attorney or accredited representative.",
    "Do NOT ask for or request sensitive personal data (e.g., A-number, passport/ID numbers, full address). If the user includes sensitive data, tell them to remove it.",
    "Be practical and action-oriented: provide plain-language explanations and steps someone can take today.",
    "Prefer official sources and legal aid directories. If you are unsure, say so and suggest official verification steps.",
    "You MUST ground answers in retrieved sources. If you cannot find authoritative sources, say so and provide safe next steps (verify credentials, find legal aid, consult qualified professionals).",
    "Only include citations that are from official or legal-aid domains.",
    `Respond in locale "${locale}".`,
    "",
    "Return ONLY valid JSON matching the provided JSON schema. Do not wrap in markdown code fences.",
  ].join("\n");

  const tools: Array<Record<string, unknown>> = [];
  const include: string[] = [];
  if (params.mode === "file_search") {
    if (!params.vectorStoreId) throw new Error("OPENAI_VECTOR_STORE_ID not configured");
    tools.push({
      type: "file_search",
      vector_store_ids: [params.vectorStoreId],
      max_num_results: 6,
    });
    include.push("file_search_call.results");
  } else {
    tools.push({
      type: "web_search",
      // Keep the search constrained to official/legal-aid domains.
      filters: { allowed_domains: allowedDomains },
      // Cache-only by default; enable external access only when explicitly configured.
      external_web_access: Boolean(params.webSearchExternal),
      // Keep context bounded; we only need a handful of authoritative sources.
      search_context_size: "low",
      user_location: {
        type: "approximate",
        country: "US",
        region: "California",
        timezone: "America/Los_Angeles",
      },
    });
    include.push("web_search_call.action.sources");
  }

  const text: Record<string, unknown> = {
    ...(isGpt5Family ? { verbosity: "low" } : {}),
    format: {
      type: "json_schema",
      name: "regshield_ask_v1",
      schema: params.schema,
      strict: true,
    },
  };

  const payload: Record<string, unknown> = {
    model: OPENAI_MODEL,
    instructions,
    input: params.messages.map((m) => ({ role: m.role, content: m.content })),
    ...(isGpt5Family ? { reasoning: { effort: "low" } } : {}),
    store: false,
    tool_choice: "required",
    tools,
    include,
    max_output_tokens: 900,
    text,
  };

  const res = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      authorization: `Bearer ${OPENAI_API_KEY}`,
      "content-type": "application/json",
    },
    body: JSON.stringify(payload),
    // Keep requests bounded; client already retries at the UX layer.
    // RAG calls (file_search + generation) can be slow; keep below mobile HTTP timeouts.
    signal: AbortSignal.timeout(45_000),
  });

  const data = (await res.json().catch(() => null)) as unknown;
  if (!res.ok) {
    const msg = openAiErrorMessage(data) ?? `HTTP ${res.status}`;
    throw new Error(`OpenAI request failed: ${msg}`);
  }

  const extracted = extractOutput(data);
  if (!extracted || !extracted.text) throw new Error("OpenAI response missing output text");

  // Sanity check: ensure the required tool call was actually used.
  const requiredType = params.mode === "file_search" ? "file_search_call" : "web_search_call";
  if (!outputHasType(extracted.output, requiredType)) {
    throw new Error(`OpenAI did not produce expected tool call: ${requiredType}`);
  }

  const parsed = JSON.parse(extracted.text) as AskModelOutput;
  return parsed;
}
