"use client";

// Recherche de joueur avec menu déroulant : les pseudos proposés arrivent pendant la frappe (/api/players).
// Entrée sans suggestion choisie : le formulaire part vers /classement?q=… (qui ouvre le profil s'il n'y a qu'un joueur).
import { useRouter } from "next/navigation";
import { useEffect, useId, useRef, useState } from "react";
import { Search } from "lucide-react";
import { RankBadge, TeamLogo } from "@/components/ui";
import { rankOf } from "@/lib/game";
import type { PlayerHit } from "@/lib/queries";

export default function PlayerSearch({ variant = "header", defaultValue = "", rank = null }: { variant?: "header" | "filter"; defaultValue?: string; rank?: string | null }) {
  const router = useRouter();
  const listId = useId();
  const [q, setQ] = useState(defaultValue);
  const [hits, setHits] = useState<PlayerHit[]>([]);
  const [open, setOpen] = useState(false);
  const [active, setActive] = useState(-1);
  const box = useRef<HTMLFormElement>(null);

  // suggestions, 150 ms après la dernière touche ; une réponse en retard sur la saisie est ignorée
  useEffect(() => {
    const text = q.trim();
    if (!text) return;
    const ctrl = new AbortController();
    const timer = setTimeout(() => {
      fetch(`/api/players?q=${encodeURIComponent(text)}`, { signal: ctrl.signal })
        .then((r) => (r.ok ? r.json() : []))
        .then((rows: PlayerHit[]) => {
          setHits(rows);
          setActive(-1);
        })
        .catch(() => {});
    }, 150);
    return () => {
      clearTimeout(timer);
      ctrl.abort();
    };
  }, [q]);

  // clic ailleurs : le menu se ferme
  useEffect(() => {
    const close = (e: MouseEvent) => {
      if (box.current && !box.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener("mousedown", close);
    return () => document.removeEventListener("mousedown", close);
  }, []);

  const shown = open && q.trim() !== "" && hits.length > 0;
  const go = (h: PlayerHit) => {
    setOpen(false);
    if (variant === "header") {
      setQ("");
      setHits([]);
    }
    router.push(`/player/${h.id}`);
  };

  const onKey = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Escape") {
      setOpen(false);
      return;
    }
    if (!shown) return;
    if (e.key === "ArrowDown") {
      e.preventDefault();
      setActive((a) => (a + 1) % hits.length);
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setActive((a) => (a <= 0 ? hits.length - 1 : a - 1));
    } else if (e.key === "Enter" && active >= 0) {
      e.preventDefault();
      go(hits[active]);
    }
  };

  const header = variant === "header";
  return (
    <form ref={box} action="/classement" method="get" role="search" className={header ? "relative" : "relative flex flex-1 gap-2 sm:max-w-md"}>
      {header && <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-ink-muted" aria-hidden />}
      {rank && <input type="hidden" name="rang" value={rank} />}
      <input
        type="search"
        name="q"
        value={q}
        onChange={(e) => {
          setQ(e.target.value);
          setOpen(true);
          if (!e.target.value.trim()) setHits([]);
        }}
        onFocus={() => setOpen(true)}
        onKeyDown={onKey}
        autoComplete="off"
        role="combobox"
        aria-expanded={shown}
        aria-controls={listId}
        aria-autocomplete="list"
        aria-activedescendant={shown && active >= 0 ? `${listId}-${active}` : undefined}
        placeholder={header ? "Un joueur…" : "Filtrer par pseudo…"}
        aria-label={header ? "Rechercher un joueur" : "Nom du joueur"}
        className={
          header
            ? "h-9 w-36 rounded-full border border-line bg-black/30 pl-9 pr-3 text-sm text-ink placeholder:text-ink-muted transition-[width] focus:w-48 focus:border-gold-300 focus:outline-none sm:w-44 sm:focus:w-56"
            : "h-10 min-w-0 flex-1 rounded-full border border-line bg-black/30 px-4 text-sm placeholder:text-ink-muted focus:border-gold-300 focus:outline-none"
        }
      />
      {!header && <button type="submit" className="btn-pill h-10 px-5 text-base">Filtrer</button>}
      {shown && (
        <ul
          id={listId}
          role="listbox"
          aria-label="Joueurs trouvés"
          className={`absolute top-full z-50 mt-1.5 max-h-80 w-72 max-w-[calc(100vw-2rem)] overflow-y-auto rounded-2xl border border-line-strong bg-grass-950/95 p-1.5 shadow-2xl backdrop-blur-md ${header ? "right-0" : "left-0"}`}
        >
          {hits.map((h, i) => {
            const r = rankOf(h.elo);
            return (
              <li
                key={h.id}
                id={`${listId}-${i}`}
                role="option"
                aria-selected={i === active}
                onMouseEnter={() => setActive(i)}
                onMouseDown={(e) => e.preventDefault()}
                onClick={() => go(h)}
                className={`flex cursor-pointer items-center gap-2.5 rounded-xl px-2.5 py-1.5 ${i === active ? "bg-white/10" : ""}`}
              >
                <RankBadge elo={h.elo} size={28} />
                <span className="min-w-0 flex-1">
                  <span className="block truncate font-bold text-ink">{h.name}</span>
                  <span className="block text-xs text-ink-muted">
                    {h.games > 0 ? (
                      <>
                        <span style={{ color: r.color }}>{r.name}</span> · {h.elo} Elo · {h.games} match{h.games > 1 ? "s" : ""}
                      </>
                    ) : (
                      "pas encore classé"
                    )}
                  </span>
                </span>
                <TeamLogo animal={h.team} size={24} />
              </li>
            );
          })}
        </ul>
      )}
    </form>
  );
}
