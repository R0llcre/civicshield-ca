import createMiddleware from "next-intl/middleware";
import type { NextRequest } from "next/server";

import { routing } from "./i18n/routing";

const intlProxy = createMiddleware(routing);

// Next.js 16: `middleware.ts` is deprecated in favor of `proxy.ts`.
// Keep this file intentionally tiny (locale routing only).
export default function proxy(request: NextRequest) {
  return intlProxy(request);
}

export const config = {
  // Skip static files and API routes
  matcher: ["/((?!api|_next|.*\\..*).*)"],
};

