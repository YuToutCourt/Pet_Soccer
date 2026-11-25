import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { connection } from "next/server";
import { Suspense } from "react";
import { Crown } from "lucide-react";
import { Empty, PageSkeleton, RankBadge, TeamLogo, TeamTag, WLBar } from "@/components/ui";
import { fmtAgo, nowSec, pct, rankOf, RANKS } from "@/lib/game";
import { getLeaderboard, type LeaderRow } from "@/lib/queries";

export const metadata: Metadata = { title: "Classement Elo" };

export default function Page({ searchParams }: PageProps<"/classement">) {
  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="title-game text-4xl">Classement Elo</h1>
          <p className="mt-1 text-ink-dim">Les matchs classés en partie rapide font monter ou descendre ton Elo.</p>
        </div>
        <RankLegend />
      </div>
      <Suspense fallback={<PageSkeleton />}>
        <Leaderboard searchParams={searchParams} />
      </Suspense>
    </div>
  );
}

function RankLegend() {
  return (
    <ul className="flex flex-wrap gap-1.5" aria-label="Rangs">
      {[...RANKS].reverse().map((r) => (
        <li key={r.name} className="flex items-center gap-1.5 rounded-full border border-line bg-black/20 py-0.5 pl-0.5 pr-2.5 text-xs font-bold text-ink-dim" title={`${r.name} : ${r.min}+ Elo`}>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={`/img/ranks/${r.badge}`} alt="" width={22} height={22} className="rounded-full" />
          <span>{r.min}+</span>
        </li>
      ))}
    </ul>
  );
}

async function Leaderboard({ searchParams }: { searchParams: PageProps<"/classement">["searchParams"] }) {
  await connection();
  const sp = await searchParams;
  const query = (typeof sp.q === "string" ? sp.q : "").trim();
  const players = await getLeaderboard(query);
  if (query && players.length === 1) redirect(`/player/${players[0].id}`);
  const now = nowSec();
  const podium = !query && players.length >= 3 ? players.slice(0, 3) : [];

  return (
    <>
      {podium.length > 0 && <Podium players={podium} />}

      <div className="card overflow-hidden">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-line px-5 py-4">
          <form action="/classement" method="get" role="search" className="flex flex-1 gap-2 sm:max-w-md">
            <input
              type="search"
              name="q"
              defaultValue={query}
              placeholder="Filtrer par pseudo…"
              aria-label="Nom du joueur"
              className="h-10 flex-1 rounded-full border border-line bg-black/30 px-4 text-sm placeholder:text-ink-muted focus:border-gold-300 focus:outline-none"
            />
            <button type="submit" className="btn-pill h-10 px-5 text-base">Filtrer</button>
          </form>
          {query && (
            <Link href="/classement" className="text-sm font-bold text-ink-dim hover:text-gold-300">
              Effacer « {query} »
            </Link>
          )}
        </div>
        {players.length === 0 ? (
          <Empty>{query ? `Aucun joueur trouvé pour « ${query} ».` : "Aucun joueur classé pour le moment."}</Empty>
        ) : (
          <div className="overflow-x-auto">
            <table className="data-table">
              <thead>
                <tr>
                  <th className="w-12 text-center">#</th>
                  <th>Joueur</th>
                  <th className="hidden md:table-cell">Équipe favorite</th>
                  <th>Elo</th>
                  <th className="hidden sm:table-cell">V / N / D</th>
                  <th className="text-right">Victoires</th>
                </tr>
              </thead>
              <tbody>
                {players.map((p, i) => (
                  <Row key={p.id} p={p} pos={i + 1} now={now} />
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </>
  );
}

function Row({ p, pos, now }: { p: LeaderRow; pos: number; now: number }) {
  const r = rankOf(p.elo);
  const medal = pos === 1 ? "text-gold-300" : pos === 2 ? "text-[#d6d6d6]" : pos === 3 ? "text-[#cd7f32]" : "text-ink-muted";
  return (
    <tr>
      <td className={`text-center font-display text-lg tabular ${medal}`}>{pos}</td>
      <td>
        <div className="flex items-center gap-3">
          <RankBadge elo={p.elo} size={34} />
          <div className="min-w-0">
            <Link href={`/player/${p.id}`} className="font-bold text-ink hover:text-gold-300">{p.name}</Link>
            <div className="text-xs text-ink-muted">
              <span style={{ color: r.color }}>{r.name}</span> · vu {fmtAgo(p.last_seen, now)}
            </div>
          </div>
        </div>
      </td>
      <td className="hidden md:table-cell"><TeamTag animal={p.team} /></td>
      <td>
        <span className="font-display text-xl tabular">{p.elo}</span>
        {p.peak > p.elo && <div className="text-[0.7rem] text-ink-muted tabular">pic {p.peak}</div>}
      </td>
      <td className="hidden sm:table-cell"><WLBar w={p.wins} d={p.draws} l={p.losses} /></td>
      <td className="text-right">
        <span className="font-extrabold tabular text-ink">{pct(p.wins, p.games)} %</span>
        <div className="text-xs text-ink-muted tabular">{p.games} match{p.games > 1 ? "s" : ""}</div>
      </td>
    </tr>
  );
}

function Podium({ players }: { players: LeaderRow[] }) {
  // ordre d'affichage : 2e, 1er, 3e
  const order = [players[1], players[0], players[2]];
  const heights = ["sm:mt-8", "", "sm:mt-12"];
  const rings = ["#d6d6d6", "var(--gold-300)", "#cd7f32"];
  const positions = [2, 1, 3];
  return (
    <div className="grid gap-4 sm:grid-cols-3">
      {order.map((p, i) => (
        <Link
          key={p.id}
          href={`/player/${p.id}`}
          className={`card group relative flex flex-col items-center px-4 pb-5 pt-7 text-center transition hover:-translate-y-1 ${heights[i]} ${positions[i] === 1 ? "order-first sm:order-none" : ""}`}
          style={{ borderColor: `color-mix(in srgb, ${rings[i]} 45%, transparent)` }}
        >
          {positions[i] === 1 && <Crown className="absolute -top-4 size-8 fill-gold-300 text-gold-700 drop-shadow" aria-hidden />}
          <span className="absolute left-4 top-3 font-display text-3xl" style={{ color: rings[i] }}>{positions[i]}</span>
          <div className="relative">
            <RankBadge elo={p.elo} size={positions[i] === 1 ? 84 : 68} />
            <span className="absolute -bottom-1 -right-2"><TeamLogo animal={p.team} size={30} /></span>
          </div>
          <div className="mt-3 max-w-full truncate font-display text-2xl group-hover:text-gold-100">{p.name}</div>
          <div className="font-display text-3xl tabular" style={{ color: rings[i] }}>{p.elo}</div>
          <div className="mt-1 text-xs font-bold text-ink-muted tabular">
            {p.wins} V · {p.draws} N · {p.losses} D
          </div>
        </Link>
      ))}
    </div>
  );
}
