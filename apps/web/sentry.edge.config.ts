import * as Sentry from "@sentry/nextjs";

function stripQuery(url: string) {
  try {
    const u = new URL(url);
    u.search = "";
    return u.toString();
  } catch {
    return url.split("?")[0] ?? url;
  }
}

Sentry.init({
  dsn: process.env.SENTRY_DSN ?? process.env.NEXT_PUBLIC_SENTRY_DSN,
  enabled: !!(process.env.SENTRY_DSN ?? process.env.NEXT_PUBLIC_SENTRY_DSN),
  tracesSampleRate: 0,
  sendDefaultPii: false,
  beforeSend(event) {
    if (event.request?.url) event.request.url = stripQuery(event.request.url);
    if (event.request) {
      delete event.request.headers;
      delete event.request.cookies;
      delete event.request.data;
    }
    delete event.user;

    if (event.breadcrumbs) {
      for (const b of event.breadcrumbs) {
        const data = b.data as Record<string, unknown> | undefined;
        const url = typeof data?.url === "string" ? (data.url as string) : null;
        if (url) (b.data as Record<string, unknown>).url = stripQuery(url);
      }
    }

    return event;
  },
});

