import type { Metadata } from "next";
import { connection } from "next/server";
import { Suspense } from "react";
import { Empty, KindTabs, MatchLine, PageSkeleton, Pagination } from "@/components/ui";
import { fmtDay, nowSec, parseKind } from "@/lib/game";
import { getMatches, type MatchRow } from "@/lib/queries";

export const metadata: Metadata = { title: "Matchs" };

export default function Page({ searchParams }: PageProps<"/matches">) {
  return (
    <div className="space-y-6">
      <h1 className="title-game text-4xl">Matchs</h1>
      <Suspense fallback={<PageSkeleton />}>
        <MatchList searchParams={searchParams} />
      </Suspense>
    </div>
  );
}

async function MatchList({ searchParams }: { searchParams: PageProps<"/matches">["searchParams"] }) {
  await connection();
  const sp = await searchParams;
  const kind = parseKind(sp.kind);
  const page = Number.parseInt(typeof sp.p === "string" ? sp.p : "1", 10) || 1;
  const res = await getMatches(kind, page);
  const now = nowSec();

  // regroupés par jour
  const days: { day: string; rows: MatchRow[] }[] = [];
  for (const m of res.rows) {
    const day = fmtDay(m.played);
    if (days.at(-1)?.day !== day) days.push({ day, rows: [] });
    days.at(-1)!.rows.push(m);
  }

  const href = (p: number) => {
    const q = new URLSearchParams();
    if (kind) q.set("kind", kind);
    if (p > 1) q.set("p", String(p));
    const s = q.toString();
    return s ? `/matches?${s}` : "/matches";
  };

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <KindTabs base="/matches" kind={kind} />
        <span className="text-sm font-bold text-ink-muted tabular">{res.total.toLocaleString("fr-FR")} matchs</span>
      </div>
      {res.rows.length === 0 ? (
        <div className="card"><Empty>Aucun match pour le moment.</Empty></div>
      ) : (
        <div className="space-y-6">
          {days.map((d) => (
            <section key={d.day}>
              <h2 className="label mb-2 pl-1">{d.day}</h2>
              <div className="space-y-2">
                {d.rows.map((m) => (
                  <MatchLine key={m.id} m={m} now={now} />
                ))}
              </div>
            </section>
          ))}
        </div>
      )}
      <Pagination page={res.page} pages={res.pages} href={href} />
    </>
  );
}
