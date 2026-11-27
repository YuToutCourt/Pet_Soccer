"use client";

import { ChevronDown, ChevronUp, ChevronsUpDown } from "lucide-react";
import { type ReactNode, useMemo, useState } from "react";

export type SortCol = {
  label: string;
  title?: string;
  /** Colonne de nombres : alignée à droite, triée du plus grand au plus petit au premier clic. */
  num?: boolean;
};

export type SortRow = {
  key: string | number;
  /** Une case par colonne, déjà rendue. */
  cells: ReactNode[];
  /** Valeur de tri par colonne ; null passe toujours en dernier. */
  values: (number | string | null)[];
  /** Classe de chaque case (sinon `text-right tabular` pour les nombres). */
  classes?: (string | undefined)[];
};

/** Tableau `data-table` dont on trie les lignes en cliquant sur les en-têtes. */
export default function SortTable({ cols, rows, initial }: { cols: SortCol[]; rows: SortRow[]; initial?: { col: number; desc: boolean } }) {
  const [sort, setSort] = useState<{ col: number; desc: boolean } | null>(initial ?? null);

  const sorted = useMemo(() => {
    if (!sort) return rows;
    const { col, desc } = sort;
    return [...rows].sort((a, b) => {
      const x = a.values[col], y = b.values[col];
      if (x == null || y == null) return x == null ? (y == null ? 0 : 1) : -1;
      const c = typeof x === "number" && typeof y === "number" ? x - y : String(x).localeCompare(String(y), "fr");
      return desc ? -c : c;
    });
  }, [rows, sort]);

  function click(i: number) {
    const first = !!cols[i].num; // nombres : décroissant d'abord, texte : A → Z
    setSort((s) => (s?.col === i ? { col: i, desc: !s.desc } : { col: i, desc: first }));
  }

  return (
    <table className="data-table data-table-compact">
      <thead>
        <tr>
          {cols.map((c, i) => {
            const on = sort?.col === i;
            const Icon = on ? (sort.desc ? ChevronDown : ChevronUp) : ChevronsUpDown;
            return (
              <th key={i} className={c.num ? "text-right" : undefined} title={c.title} aria-sort={on ? (sort.desc ? "descending" : "ascending") : "none"}>
                <button
                  type="button"
                  onClick={() => click(i)}
                  className={`max-w-[6.5rem] cursor-pointer whitespace-normal uppercase tracking-[inherit] hover:text-gold-300 ${on ? "text-gold-300" : ""} ${c.num ? "text-right" : "text-left"}`}
                >
                  {c.num && <Icon className={`mr-1 inline size-3 align-[-1px] ${on ? "" : "opacity-40"}`} />}
                  {c.label}
                  {!c.num && <Icon className={`ml-1 inline size-3 align-[-1px] ${on ? "" : "opacity-40"}`} />}
                </button>
              </th>
            );
          })}
        </tr>
      </thead>
      <tbody>
        {sorted.map((r) => (
          <tr key={r.key}>
            {r.cells.map((cell, i) => (
              <td key={i} className={r.classes?.[i] ?? (cols[i].num ? "text-right tabular" : undefined)}>{cell}</td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}
