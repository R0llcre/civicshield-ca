import type { NextConfig } from "next";
import createNextIntlPlugin from "next-intl/plugin";

const withNextIntl = createNextIntlPlugin();

function buildCspReportOnlyValue(isDev: boolean): string {
  const scriptSrc = ["'self'", "'unsafe-inline'", "https:"];
  if (isDev) scriptSrc.push("'unsafe-eval'");

  const directives = [
    "default-src 'self'",
    "base-uri 'self'",
    "form-action 'self'",
    "object-src 'none'",
    "frame-ancestors 'none'",
    "img-src 'self' data: https:",
    "font-src 'self' data: https:",
    "style-src 'self' 'unsafe-inline' https:",
    `script-src ${scriptSrc.join(" ")}`,
    // Allow calls to Supabase/Sentry/etc without needing per-host config at MVP stage.
    "connect-src 'self' https:",
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
  { key: "Strict-Transport-Security", value: "max-age=15552000; includeSubDomains" },
  // Disable powerful features we don't need.
  { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=()" },
];

const nextConfig: NextConfig = {
  async headers() {
    const isDev = process.env.NODE_ENV !== "production";
    const headers = [
      ...securityHeaders,
      // Start with report-only to reduce rollout risk; enforce later once verified in staging.
      { key: "Content-Security-Policy-Report-Only", value: buildCspReportOnlyValue(isDev) },
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
