import { z } from "zod";

const envSchema = z.object({
  // When set, API routes prefer direct Postgres (Azure) instead of Supabase.
  DATABASE_URL: z.string().min(1).optional(),

  // Optional; used for diagnostics and environment-specific behavior.
  APP_ENV: z.string().min(1).optional(),
});

let cached: z.infer<typeof envSchema> | null = null;

export function getServerEnv() {
  if (cached) return cached;
  const parsed = envSchema.safeParse({
    DATABASE_URL: process.env.DATABASE_URL,
    APP_ENV: process.env.APP_ENV,
  });
  cached = parsed.success ? parsed.data : { DATABASE_URL: undefined, APP_ENV: undefined };
  return cached;
}

