import type { Metadata } from "next";
import { Public_Sans, Sora } from "next/font/google";
import { getLocale } from "next-intl/server";
import "./globals.css";

const bodyFont = Public_Sans({
  variable: "--font-body",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700"],
});

const displayFont = Sora({
  variable: "--font-display",
  subsets: ["latin"],
  weight: ["500", "600", "700", "800"],
});

export const metadata: Metadata = {
  title: "CivicShield CA",
  description: "Verify credentials. Avoid scams. Find help faster.",
};

export default async function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  // Best-effort: `next-intl` locale is derived from middleware routing.
  // If unavailable (e.g. middleware misconfigured), default to English.
  let lang = "en";
  try {
    lang = await getLocale();
  } catch {
    // ignore
  }

  return (
    <html lang={lang} suppressHydrationWarning>
      <body className={`${bodyFont.variable} ${displayFont.variable} antialiased`}>
        {children}
      </body>
    </html>
  );
}
