import type { Metadata, Viewport } from "next";
import { Lilita_One, Nunito } from "next/font/google";
import Link from "next/link";
import { Suspense } from "react";
import LogoOnline from "@/components/LogoOnline";
import NavLinks, { NavLinksView } from "@/components/NavLinks";
import PlayerSearch from "@/components/PlayerSearch";
import ServerStatus from "@/components/ServerStatus";
import { REPO } from "@/lib/release";
import "./globals.css";

const lilita = Lilita_One({ variable: "--font-lilita", weight: "400", subsets: ["latin"] });
const nunito = Nunito({ variable: "--font-nunito", subsets: ["latin"] });

export const metadata: Metadata = {
  metadataBase: new URL(process.env.SITE_URL ?? "http://localhost:8080"),
  title: { default: "Pet Soccer Online", template: "%s · Pet Soccer Online" },
  description:
    "Le mod communautaire de Pet Soccer : multijoueur en ligne, classement Elo, tactiques, Ligue, Survie et succès. Téléchargement et statistiques des matchs.",
  openGraph: {
    title: "Pet Soccer Online",
    description: "Une seconde vie pour Pet Soccer : en ligne, classement Elo, tactiques, Ligue et Survie.",
    images: ["/img/screens/match.jpg"],
    locale: "fr_FR",
    type: "website",
  },
};

export const viewport: Viewport = { themeColor: "#062611" };

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="fr" className={`${lilita.variable} ${nunito.variable} h-full antialiased`}>
      <body className="flex min-h-full flex-col font-sans">
        <header className="sticky top-0 z-40 border-b border-line bg-grass-950/80 backdrop-blur-md">
          <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-5 gap-y-2 px-4 py-2">
            <Link href="/" aria-label="Pet Soccer Online, accueil" className="shrink-0 py-1">
              <LogoOnline width={118} />
            </Link>
            <div className="order-3 -mx-4 w-[calc(100%+2rem)] px-4 sm:order-none sm:mx-0 sm:w-auto sm:px-0">
              <Suspense fallback={<NavLinksView path="" />}>
                <NavLinks />
              </Suspense>
            </div>
            <div className="ml-auto flex items-center gap-4">
              <div className="hidden md:block">
                <Suspense fallback={null}>
                  <ServerStatus />
                </Suspense>
              </div>
              <PlayerSearch />
            </div>
          </div>
        </header>

        <main className="mx-auto w-full max-w-6xl flex-1 px-4 py-8">{children}</main>

        <footer className="border-t border-line bg-grass-950/70">
          <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-4 px-4 py-6 text-sm text-ink-muted">
            <p>
              <span className="font-display text-ink-dim">Pet Soccer Online</span> · mod communautaire de Pet Soccer (Techland, 2002)
            </p>
            <a href={`https://github.com/${REPO}`} className="font-bold text-ink-dim hover:text-gold-300" target="_blank" rel="noreferrer">
              Code source sur GitHub
            </a>
          </div>
        </footer>
      </body>
    </html>
  );
}
