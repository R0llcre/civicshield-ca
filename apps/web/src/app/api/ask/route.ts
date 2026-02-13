import { NextResponse } from "next/server";
import { z } from "zod";

import { checkRateLimit, getClientIp } from "@/lib/rateLimit";
import { askOpenAI, type AskChatMessage, type AskModelOutput } from "@/server/openai";
import { getServerEnv } from "@/server/env";

export const runtime = "nodejs";
export const dynamic = "force-dynamic";

const ALLOWED_CITATION_DOMAINS = [
  "uscis.gov",
  "justice.gov",
  "eoir.justice.gov",
  "ca.gov",
  "calbar.ca.gov",
  "sos.ca.gov",
  "lawhelpca.org",
  "immigrationadvocates.org",
] as const;

function isAllowedUrl(url: string): boolean {
  try {
    const u = new URL(url);
    if (u.protocol !== "https:" && u.protocol !== "http:") return false;
    const host = u.hostname.toLowerCase();
    return ALLOWED_CITATION_DOMAINS.some((d) => host === d || host.endsWith(`.${d}`));
  } catch {
    return false;
  }
}

function payloadUrl(payload: unknown): string | null {
  if (!payload || typeof payload !== "object") return null;
  const u = (payload as Record<string, unknown>).url;
  return typeof u === "string" ? u : null;
}

const requestSchema = z
  .object({
    locale: z.enum(["en", "es", "zh-Hans"]),
    messages: z
      .array(
        z.object({
          role: z.enum(["user", "assistant"]),
          content: z.string().min(1),
        }),
      )
      .min(1)
      .max(20),
  })
  .superRefine((v, ctx) => {
    let total = 0;
    for (const [i, m] of v.messages.entries()) {
      if (m.content.length > 2000) {
        ctx.addIssue({
          code: "custom",
          path: ["messages", i, "content"],
          message: "Message too long",
        });
      }
      total += m.content.length;
    }
    if (total > 8000) {
      ctx.addIssue({
        code: "custom",
        path: ["messages"],
        message: "Conversation too long",
      });
    }
  });

const OUTPUT_SCHEMA: Record<string, unknown> = {
  type: "object",
  additionalProperties: false,
  required: ["answer_markdown", "citations", "suggested_actions", "disclaimer"],
  properties: {
    answer_markdown: { type: "string" },
    citations: {
      type: "array",
      maxItems: 6,
      items: {
        type: "object",
        additionalProperties: false,
        required: ["title", "url"],
        properties: {
          title: { type: "string" },
          url: { type: "string" },
        },
      },
    },
    suggested_actions: {
      type: "array",
      maxItems: 6,
      items: {
        type: "object",
        additionalProperties: false,
        // OpenAI Structured Outputs (strict=true) requires `required` to list every key in `properties`.
        required: ["type", "label", "payload"],
        properties: {
          type: {
            type: "string",
            enum: ["open_verify", "open_help", "open_official_url"],
          },
          label: { type: "string" },
          payload: {
            type: "object",
            additionalProperties: false,
            required: ["url"],
            properties: {
              url: { type: "string" },
            },
          },
        },
      },
    },
    disclaimer: { type: "string" },
  },
};

const DISCLAIMER = "Information only. Not legal advice. Do not share sensitive personal info.";

function sanitizeOutput(out: AskModelOutput): AskModelOutput {
  const citations = (out.citations ?? [])
    .filter((c) => c && typeof c.title === "string" && typeof c.url === "string" && isAllowedUrl(c.url))
    .slice(0, 6);

  const suggested_actions = (out.suggested_actions ?? [])
    .filter((a) => a && typeof a.type === "string" && typeof a.label === "string")
    .map((a) => {
      if (a.type !== "open_official_url") return { type: a.type, label: a.label } as const;
      const rawUrl = payloadUrl(a.payload);
      const url = typeof rawUrl === "string" && isAllowedUrl(rawUrl) ? rawUrl : null;
      return url
        ? ({ type: a.type, label: a.label, payload: { url } } as const)
        : null;
    })
    .filter(Boolean)
    .slice(0, 6) as AskModelOutput["suggested_actions"];

  return {
    answer_markdown: String(out.answer_markdown ?? "").trim(),
    citations,
    suggested_actions,
    disclaimer: DISCLAIMER,
  };
}

function strictNoSources(locale: "en" | "es" | "zh-Hans"): AskModelOutput {
  const body =
    locale === "es"
      ? [
          "No pude encontrar fuentes autorizadas para responder con confianza.",
          "",
          "**Pasos seguros:**",
          "- Verifica credenciales con fuentes oficiales (DOJ/EOIR, State Bar, CA SOS).",
          "- Busca ayuda legal a traves de organizaciones verificadas y directorios oficiales.",
          "- Si tienes una fecha limite, consulta con un abogado calificado o un representante acreditado.",
        ].join("\n")
      : locale === "zh-Hans"
        ? [
            "我没有找到足够权威的来源来给出可靠回答。",
            "",
            "**更安全的下一步：**",
            "- 通过官方渠道核验资质（DOJ/EOIR 名录、加州律师协会、加州 SOS）。",
            "- 通过可信法律援助机构/官方目录寻求帮助。",
            "- 如果你有截止日期或开庭日期，请尽快咨询合格律师或认可机构/认证代表。",
          ].join("\n")
        : [
            "I couldn't find authoritative sources to answer confidently.",
            "",
            "**Safer next steps:**",
            "- Verify credentials using official sources (DOJ/EOIR, CA State Bar, CA SOS).",
            "- Find legal aid through verified organizations and official directories.",
            "- If you have a deadline, consult a qualified attorney or accredited representative.",
          ].join("\n");

  const labelVerify = locale === "es" ? "Open Verify" : locale === "zh-Hans" ? "打开核验" : "Open Verify";
  const labelHelp = locale === "es" ? "Open Help" : locale === "zh-Hans" ? "打开求助" : "Open Help";

  return {
    answer_markdown: body,
    citations: [],
    suggested_actions: [
      { type: "open_verify", label: labelVerify },
      { type: "open_help", label: labelHelp },
    ],
    disclaimer: DISCLAIMER,
  };
}

export async function POST(req: Request) {
  const { OPENAI_API_KEY, OPENAI_VECTOR_STORE_ID, OPENAI_ENABLE_WEB_SEARCH, OPENAI_WEB_SEARCH_EXTERNAL, OPENAI_RAG_STRICT } =
    getServerEnv();
  if (!OPENAI_API_KEY || !OPENAI_VECTOR_STORE_ID) {
    return NextResponse.json(
      { error: "AI Q&A is not configured on this server." },
      { status: 501 },
    );
  }

  const ip = getClientIp(req);
  const rl = checkRateLimit({
    key: `ask:${ip}`,
    limit: 10,
    windowMs: 60_000,
  });
  if (!rl.ok) {
    const retryAfterSec = Math.max(1, Math.ceil((rl.resetAtMs - Date.now()) / 1000));
    return NextResponse.json(
      { error: "Rate limit exceeded. Please try again shortly." },
      {
        status: 429,
        headers: {
          "retry-after": String(retryAfterSec),
        },
      },
    );
  }

  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
  }

  const parsed = requestSchema.safeParse(body);
  if (!parsed.success) {
    const issues = parsed.error.issues ?? [];
    const tooLong = issues.some((i) => String(i.message).toLowerCase().includes("too long"));
    return NextResponse.json(
      {
        error: tooLong
          ? "Your question or chat history is too long. Please shorten it or clear chat and try again."
          : "Invalid request",
      },
      { status: 400 },
    );
  }

  const messages: AskChatMessage[] = parsed.data.messages.map((m) => ({
    role: m.role,
    content: m.content.trim(),
  }));

  try {
    const raw = await askOpenAI({
      locale: parsed.data.locale,
      messages,
      schema: OUTPUT_SCHEMA,
      mode: "file_search",
      vectorStoreId: OPENAI_VECTOR_STORE_ID,
    });
    const out = sanitizeOutput(raw);
    if (OPENAI_RAG_STRICT && out.citations.length === 0) {
      if (OPENAI_ENABLE_WEB_SEARCH) {
        const rawWeb = await askOpenAI({
          locale: parsed.data.locale,
          messages,
          schema: OUTPUT_SCHEMA,
          mode: "web_search",
          allowedDomains: [...ALLOWED_CITATION_DOMAINS],
          webSearchExternal: OPENAI_WEB_SEARCH_EXTERNAL,
        });
        const outWeb = sanitizeOutput(rawWeb);
        if (outWeb.citations.length > 0 && outWeb.answer_markdown) {
          return NextResponse.json(outWeb);
        }
      }
      return NextResponse.json(strictNoSources(parsed.data.locale));
    }
    if (!out.answer_markdown) {
      return NextResponse.json(
        { error: "No answer generated. Please rephrase and try again." },
        { status: 502 },
      );
    }
    return NextResponse.json(out);
  } catch (e) {
    const msg = e instanceof Error ? e.message : String(e);
    const configuredError = msg.includes("OPENAI_API_KEY not configured") || msg.includes("OPENAI_VECTOR_STORE_ID not configured");
    const verifyOrgError =
      msg.includes("must be verified to use the model") ||
      msg.includes("Verify Organization") ||
      msg.includes("platform.openai.com/settings/organization");
    if (verifyOrgError) {
      return NextResponse.json(
        {
          error:
            "AI model access requires OpenAI organization verification. Verify your organization in OpenAI settings, then retry in ~15 minutes.",
        },
        { status: 503 },
      );
    }
    return NextResponse.json(
      { error: configuredError ? "AI Q&A is not configured on this server." : "AI request failed. Please try again." },
      { status: configuredError ? 501 : 502 },
    );
  }
}
