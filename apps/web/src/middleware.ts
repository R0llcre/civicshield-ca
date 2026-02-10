import createMiddleware from "next-intl/middleware";

import { routing } from "./i18n/routing";

export default createMiddleware(routing);

export const config = {
  // Skip static files and API routes
  matcher: ["/((?!api|_next|.*\\..*).*)"],
};
