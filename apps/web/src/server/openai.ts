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

function extractOutputText(data: unknown): string | null {
  if (!data || typeof data !== "object") return null;
  const anyData = data as Record<string, unknown>;
  if (typeof anyData.output_text === "string") return anyData.output_text;

  const output = anyData.output;
  if (!Array.isArray(output)) return null;
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
    if (texts.length) return texts.join("");
  }
  return null;
}

export async function askOpenAI(params: {
  locale: "en" | "es" | "zh-Hans";
  messages: AskChatMessage[];
  schema: Record<string, unknown>;
}): Promise<AskModelOutput> {
  const { OPENAI_API_KEY, OPENAI_MODEL } = getServerEnv();
  if (!OPENAI_API_KEY) throw new Error("OPENAI_API_KEY not configured");

  const locale = params.locale;
  const instructions = [
    "You are RegShield CA, an informational assistant for California (CA) and U.S. immigration topics.",
    "Do NOT provide legal advice. Do NOT make promises or guarantees. Encourage consulting a qualified attorney or accredited representative.",
    "Do NOT ask for or request sensitive personal data (e.g., A-number, passport/ID numbers, full address). If the user includes sensitive data, tell them to remove it.",
    "Be practical and action-oriented: provide plain-language explanations and steps someone can take today.",
    "Prefer official sources and legal aid directories. If you are unsure, say so and suggest official verification steps.",
    `Respond in locale "${locale}".`,
    "",
    "Return ONLY valid JSON matching the provided JSON schema. Do not wrap in markdown code fences.",
  ].join("\\n");

  const payload = {
    model: OPENAI_MODEL,
    instructions,
    input: params.messages.map((m) => ({ role: m.role, content: m.content })),
    temperature: 0.2,
    store: false,
    text: {
      format: {
        type: "json_schema",
        name: "regshield_ask_v1",
        schema: params.schema,
        strict: true,
      },
    },
  };

  const res = await fetch("https://api.openai.com/v1/responses", {
    method: "POST",
    headers: {
      authorization: `Bearer ${OPENAI_API_KEY}`,
      "content-type": "application/json",
    },
    body: JSON.stringify(payload),
    // Keep requests bounded; client already retries at the UX layer.
    signal: AbortSignal.timeout(20_000),
  });

  const data = (await res.json().catch(() => null)) as unknown;
  if (!res.ok) {
    const msg = openAiErrorMessage(data) ?? `HTTP ${res.status}`;
    throw new Error(`OpenAI request failed: ${msg}`);
  }

  const text = extractOutputText(data);
  if (!text) throw new Error("OpenAI response missing output text");

  const parsed = JSON.parse(text) as AskModelOutput;
  return parsed;
}
