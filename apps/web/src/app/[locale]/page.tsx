import { ArrowRight } from "lucide-react";
import { getTranslations } from "next-intl/server";

import { Card, CardContent, CardHeader } from "@/components/ui/Card";
import { Link } from "@/i18n/navigation";
import { cn } from "@/lib/cn";

export default async function Home() {
  const tHome = await getTranslations("Home");
  const tApp = await getTranslations("App");

  const cards = [
    {
      href: "/verify",
      title: tHome("cards.verify.title"),
      body: tHome("cards.verify.body"),
      accent: "from-emerald-500/15 to-transparent",
    },
    {
      href: "/shield",
      title: tHome("cards.shield.title"),
      body: tHome("cards.shield.body"),
      accent: "from-orange-500/15 to-transparent",
    },
    {
      href: "/help",
      title: tHome("cards.help.title"),
      body: tHome("cards.help.body"),
      accent: "from-sky-500/15 to-transparent",
    },
  ] as const;

  return (
    <div className="space-y-10">
      <section className="rounded-3xl bg-white/45 p-8 ring-1 ring-[color:var(--color-surface-border)] backdrop-blur sm:p-10">
        <div className="max-w-3xl">
          <div className="text-sm font-semibold text-[color:var(--color-accent)]">
            {tApp("tagline")}
          </div>
          <h1 className="mt-3 font-[family-name:var(--font-display)] text-4xl font-bold tracking-tight sm:text-5xl">
            {tHome("title")}
          </h1>
          <p className="mt-4 text-base leading-7 text-[color:var(--color-muted)] sm:text-lg">
            {tHome("subtitle")}
          </p>
        </div>
      </section>

      <section className="grid gap-4 sm:grid-cols-3">
        {cards.map((c) => (
          <Link key={c.href} href={c.href} className="group">
            <Card className="h-full overflow-hidden">
              <div className={cn("h-1 w-full bg-gradient-to-r", c.accent)} />
              <CardHeader>
                <div className="flex items-center justify-between gap-3">
                  <div className="font-[family-name:var(--font-display)] text-lg font-semibold tracking-tight">
                    {c.title}
                  </div>
                  <ArrowRight className="h-4 w-4 text-[color:var(--color-muted)] transition group-hover:translate-x-0.5 group-hover:text-[color:var(--color-foreground)]" />
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-sm leading-6 text-[color:var(--color-muted)]">{c.body}</div>
              </CardContent>
            </Card>
          </Link>
        ))}
      </section>
    </div>
  );
}

