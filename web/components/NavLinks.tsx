"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const LINKS = [
  { href: "/", label: "Accueil" },
  { href: "/classement", label: "Classement" },
  { href: "/matches", label: "Matchs" },
  { href: "/meta", label: "Méta" },
  { href: "/patch", label: "Patch" },
];

export default function NavLinks() {
  return <NavLinksView path={usePathname()} />;
}

/** Version sans lien actif, servie dans la coquille statique avant que le chemin soit connu. */
export function NavLinksView({ path }: { path: string }) {
  return (
    <nav className="flex items-center gap-1 overflow-x-auto" aria-label="Navigation principale">
      {LINKS.map((l) => {
        const active = l.href === "/" ? path === "/" : path.startsWith(l.href) || (l.href === "/matches" && path.startsWith("/match/"));
        return (
          <Link
            key={l.href}
            href={l.href}
            aria-current={active ? "page" : undefined}
            className={`whitespace-nowrap rounded-full px-3.5 py-1.5 font-display text-[1.02rem] tracking-wide transition ${
              active ? "btn-pill px-4" : "text-ink-dim hover:bg-white/5 hover:text-ink"
            }`}
          >
            {l.label}
          </Link>
        );
      })}
    </nav>
  );
}
