import { z } from "zod";

const envSchema = z.object({
  // When set, API routes prefer direct Postgres (Azure) instead of Supabase.
  DATABASE_URL: z.string().min(1).optional(),

  // Optional; used for diagnostics and environment-specific behavior.
  APP_ENV: z.string().min(1).optional(),

  // Optional; required only for /api/ask.
  OPENAI_API_KEY: z.string().min(1).optional(),
  OPENAI_MODEL: z.string().min(1).optional(),
  OPENAI_VECTOR_STORE_ID: z.string().min(1).optional(),
  OPENAI_ENABLE_WEB_SEARCH: z.string().min(1).optional(),
  OPENAI_WEB_SEARCH_EXTERNAL: z.string().min(1).optional(),
  OPENAI_RAG_STRICT: z.string().min(1).optional(),
});

export type ServerEnv = {
  DATABASE_URL?: string;
  APP_ENV?: string;
  OPENAI_API_KEY?: string;
  OPENAI_MODEL: string;
  OPENAI_VECTOR_STORE_ID?: string;
  OPENAI_ENABLE_WEB_SEARCH: boolean;
  OPENAI_WEB_SEARCH_EXTERNAL: boolean;
  OPENAI_RAG_STRICT: boolean;
};

let cached: ServerEnv | null = null;

function parseBool(v: string | undefined, defaultValue: boolean): boolean {
  if (!v) return defaultValue;
  const s = v.trim().toLowerCase();
  if (["1", "true", "yes", "y", "on"].includes(s)) return true;
  if (["0", "false", "no", "n", "off"].includes(s)) return false;
  return defaultValue;
}

export function getServerEnv() {
  if (cached) return cached;
  const parsed = envSchema.safeParse({
    DATABASE_URL: process.env.DATABASE_URL,
    APP_ENV: process.env.APP_ENV,
    OPENAI_API_KEY: process.env.OPENAI_API_KEY,
    OPENAI_MODEL: process.env.OPENAI_MODEL,
    OPENAI_VECTOR_STORE_ID: process.env.OPENAI_VECTOR_STORE_ID,
    OPENAI_ENABLE_WEB_SEARCH: process.env.OPENAI_ENABLE_WEB_SEARCH,
    OPENAI_WEB_SEARCH_EXTERNAL: process.env.OPENAI_WEB_SEARCH_EXTERNAL,
    OPENAI_RAG_STRICT: process.env.OPENAI_RAG_STRICT,
  });
  cached = parsed.success
    ? {
        ...parsed.data,
        OPENAI_MODEL: parsed.data.OPENAI_MODEL ?? "gpt-4.1",
        OPENAI_ENABLE_WEB_SEARCH: parseBool(parsed.data.OPENAI_ENABLE_WEB_SEARCH, true),
        OPENAI_WEB_SEARCH_EXTERNAL: parseBool(parsed.data.OPENAI_WEB_SEARCH_EXTERNAL, false),
        OPENAI_RAG_STRICT: parseBool(parsed.data.OPENAI_RAG_STRICT, true),
      }
    : {
        DATABASE_URL: undefined,
        APP_ENV: undefined,
        OPENAI_API_KEY: undefined,
        OPENAI_MODEL: "gpt-4.1",
        OPENAI_VECTOR_STORE_ID: undefined,
        OPENAI_ENABLE_WEB_SEARCH: true,
        OPENAI_WEB_SEARCH_EXTERNAL: false,
        OPENAI_RAG_STRICT: true,
      };
  return cached;
}
