import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const withNextIntl = createNextIntlPlugin();

function buildCspValue(isDev: boolean): string {
  const supabaseHost = process.env.NEXT_PUBLIC_SUPABASE_URL
    ? new URL(process.env.NEXT_PUBLIC_SUPABASE_URL).origin
    : "";
  const sentryDsn = process.env.NEXT_PUBLIC_SENTRY_DSN ?? "";
  const sentryHost = sentryDsn
    ? (() => { try { return new URL(sentryDsn).origin; } catch { return ""; } })()
    : "";

  const scriptSrc = ["'self'", "'unsafe-inline'"];
  if (isDev) scriptSrc.push("'unsafe-eval'");

  const connectSrc = ["'self'", "https://api.openai.com"];
  if (supabaseHost) connectSrc.push(supabaseHost);
  if (sentryHost) connectSrc.push(sentryHost);

  const directives = [
    "default-src 'self'",
    "base-uri 'self'",
    "form-action 'self'",
    "object-src 'none'",
    "frame-ancestors 'none'",
    "img-src 'self' data: https:",
    "font-src 'self' data: https://fonts.gstatic.com",
    "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com",
    `script-src ${scriptSrc.join(" ")}`,
    `connect-src ${connectSrc.join(" ")}`,
    "manifest-src 'self'",
  ];

  return directives.join("; ");
}

const securityHeaders = [
  { key: "X-Frame-Options", value: "DENY" },
  { key: "X-Content-Type-Options", value: "nosniff" },
  { key: "Referrer-Policy", value: "strict-origin-when-cross-origin" },
  { key: "X-DNS-Prefetch-Control", value: "off" },
  { key: "X-Permitted-Cross-Domain-Policies", value: "none" },
  // HSTS is ignored by browsers on HTTP, but helps when served over HTTPS (Vercel).
  { key: "Strict-Transport-Security", value: "max-age=31536000; includeSubDomains; preload" },
  // Disable powerful features we don't need.
  { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
];

const nextConfig: NextConfig = {
  output: "standalone",
  async headers() {
    const isDev = process.env.NODE_ENV !== "production";
    const headers = [
      ...securityHeaders,
      { key: "Content-Security-Policy-Report-Only", value: buildCspValue(isDev) },
    ];

    return [
      {
        source: "/(.*)",
        headers,
      },
    ];
  },
};

export default withNextIntl(nextConfig);
