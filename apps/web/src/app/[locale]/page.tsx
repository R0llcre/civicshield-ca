import { ArrowRight } from "lucide-react";
import { getTranslations } from "next-intl/server";

import { Badge, Hero, Metric, SectionHeader } from "@/components/ui/AppShell";
import { Card, CardContent, CardHeader } from "@/components/ui/Card";
import { Link } from "@/i18n/navigation";
import { cn } from "@/lib/cn";

export default async function Home() {
  const tHome = await getTranslations("Home");
  const tApp = await getTranslations("App");
  const tNav = await getTranslations("Nav");
  const tCommon = await getTranslations("Common");

  const cards = [
    {
      href: "/verify",
      title: tHome("cards.verify.title"),
      body: tHome("cards.verify.body"),
      accent: "from-[color:var(--color-accent)]/24 to-transparent",
    },
    {
      href: "/shield",
      title: tHome("cards.shield.title"),
      body: tHome("cards.shield.body"),
      accent: "from-[color:var(--color-accent-3)]/24 to-transparent",
    },
    {
      href: "/help",
      title: tHome("cards.help.title"),
      body: tHome("cards.help.body"),
      accent: "from-[color:var(--color-accent-2)]/24 to-transparent",
    },
  ] as const;

  return (
    <div className="space-y-8 sm:space-y-10">
      <Hero
        kicker={<Badge tone="blue">{tApp("tagline")}</Badge>}
        title={tHome("title")}
        subtitle={tHome("subtitle")}
        action={
          <div className="flex flex-wrap gap-2">
            <Link
              href="/verify"
              className="inline-flex h-11 items-center rounded-2xl bg-[color:var(--color-accent)] px-4 text-sm font-semibold text-white shadow-[var(--shadow-sm)] hover:brightness-105"
            >
              {tNav("verify")}
            </Link>
            <Link
              href="/help"
              className="inline-flex h-11 items-center rounded-2xl border border-[color:var(--color-surface-border)] bg-white/85 px-4 text-sm font-semibold text-[color:var(--color-foreground)] hover:bg-white"
            >
              {tNav("help")}
            </Link>
          </div>
        }
      >
        <div className="grid gap-3 sm:grid-cols-3">
          <Metric label={tNav("verify")} value={tHome("cards.verify.title")} />
          <Metric label={tNav("shield")} value={tHome("cards.shield.title")} />
          <Metric label={tNav("help")} value={tHome("cards.help.title")} />
        </div>
      </Hero>

      <section className="space-y-4">
        <SectionHeader title={tCommon("informationOnlyTitle")} subtitle={tCommon("informationOnlyBody")} />
        <div className="grid gap-4 sm:grid-cols-3">
        {cards.map((c) => (
          <Link key={c.href} href={c.href} className="group animate-rise-in">
            <Card className="h-full overflow-hidden transition hover:-translate-y-0.5 hover:shadow-[var(--shadow-lg)]">
              <div className={cn("h-1 w-full bg-gradient-to-r", c.accent)} />
              <CardHeader>
                <div className="flex items-center justify-between gap-3">
                  <div className="font-[family-name:var(--font-display)] text-lg font-bold tracking-tight">
                    {c.title}
                  </div>
                  <ArrowRight className="h-4 w-4 text-[color:var(--color-muted)] transition group-hover:translate-x-1 group-hover:text-[color:var(--color-foreground)]" />
                </div>
              </CardHeader>
              <CardContent>
                <div className="text-sm leading-6 text-[color:var(--color-muted)]">{c.body}</div>
              </CardContent>
            </Card>
          </Link>
        ))}
        </div>
      </section>
    </div>
  );
}
