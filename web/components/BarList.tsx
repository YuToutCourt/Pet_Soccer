import type { ReactNode } from "react";

export interface BarItem {
  key: string;
  label: ReactNode;
  n: number;
  w: number;
}

/** Liste de barres horizontales : longueur = nombre de matchs, pastille = taux de victoire. */
export default function BarList({ items, unit = "match" }: { items: BarItem[]; unit?: string }) {
  const max = Math.max(1, ...items.map((i) => i.n));
  return (
    <ul className="space-y-2.5">
      {items.map((it) => {
        const wr = it.n ? Math.round((100 * it.w) / it.n) : 0;
        const tone = wr >= 55 ? "text-win" : wr < 45 ? "text-loss" : "text-draw";
        return (
          <li key={it.key} title={`${it.n} ${unit}${it.n > 1 ? "s" : ""}, ${wr} % de victoires`}>
            <div className="mb-1 flex items-center justify-between gap-3 text-sm">
              <span className="min-w-0 truncate font-bold">{it.label}</span>
              <span className="shrink-0 tabular text-ink-dim">
                {it.n} · <span className={`font-extrabold ${tone}`}>{wr} %</span>
              </span>
            </div>
            <div className="h-2 overflow-hidden rounded-full bg-black/30">
              <div className="h-full rounded-full bg-gradient-to-r from-gold-700 via-gold-500 to-gold-300" style={{ width: `${(it.n / max) * 100}%` }} />
            </div>
          </li>
        );
      })}
    </ul>
  );
}
