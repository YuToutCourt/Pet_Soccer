import type { Metadata } from "next";
import Link from "next/link";
import { connection } from "next/server";
import { Suspense } from "react";
import AutoRefresh from "@/components/AutoRefresh";
import { LiveCard } from "@/components/Live";
import { Empty, PageSkeleton } from "@/components/ui";
import { getLiveMatches } from "@/lib/queries";

export const metadata: Metadata = { title: "En direct" };

export default function Page() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="title-game text-4xl">En direct</h1>
        <p className="mt-1 text-ink-dim">Les matchs en ligne en train de se jouer : score, minute et actions, mis à jour toutes les quelques secondes.</p>
      </div>
      <Suspense fallback={<PageSkeleton />}>
        <LiveList />
      </Suspense>
      <AutoRefresh every={5000} />
    </div>
  );
}

async function LiveList() {
  await connection();
  const rows = await getLiveMatches();
  const live = rows.filter((m) => !m.ended);
  const ended = rows.filter((m) => m.ended);
  return (
    <>
      <section className="card p-4 sm:p-5" aria-label="Matchs en cours">
        {live.length === 0 ? (
          <Empty>
            Aucun match en cours pour le moment. Les derniers matchs joués sont dans{" "}
            <Link href="/matches" className="font-bold text-gold-300 hover:underline">Matchs</Link>.
          </Empty>
        ) : (
          <div className="space-y-2.5">
            {live.map((m) => <LiveCard key={m.key} m={m} />)}
          </div>
        )}
      </section>
      {ended.length > 0 && (
        <section aria-label="Matchs qui viennent de finir">
          <h2 className="mb-3 font-display text-lg text-gold-100">Viennent de finir</h2>
          <div className="space-y-2.5">
            {ended.map((m) => <LiveCard key={m.key} m={m} />)}
          </div>
        </section>
      )}
    </>
  );
}
