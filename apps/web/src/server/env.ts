import { z } from "zod";

const envSchema = z.object({
  // When set, API routes prefer direct Postgres (Azure) instead of Supabase.
  DATABASE_URL: z.string().min(1).optional(),

  // Optional; used for diagnostics and environment-specific behavior.
  APP_ENV: z.string().min(1).optional(),

  // Optional; required only for /api/ask.
  OPENAI_API_KEY: z.string().min(1).optional(),
  OPENAI_MODEL: z.string().min(1).optional(),
});

let cached: z.infer<typeof envSchema> | null = null;

export function getServerEnv() {
  if (cached) return cached;
  const parsed = envSchema.safeParse({
    DATABASE_URL: process.env.DATABASE_URL,
    APP_ENV: process.env.APP_ENV,
    OPENAI_API_KEY: process.env.OPENAI_API_KEY,
    OPENAI_MODEL: process.env.OPENAI_MODEL,
  });
  cached = parsed.success
    ? {
        ...parsed.data,
        OPENAI_MODEL: parsed.data.OPENAI_MODEL ?? "gpt-4o-mini",
      }
    : {
        DATABASE_URL: undefined,
        APP_ENV: undefined,
        OPENAI_API_KEY: undefined,
        OPENAI_MODEL: "gpt-4o-mini",
      };
  return cached;
}
