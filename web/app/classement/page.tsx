import type { Metadata } from "next";
import Link from "next/link";
import { redirect } from "next/navigation";
import { connection } from "next/server";
import { Suspense } from "react";
import { Crown } from "lucide-react";
import { Empty, PageSkeleton, RankBadge, TeamLogo, TeamTag, WLBar } from "@/components/ui";
import PlayerSearch from "@/components/PlayerSearch";
import { fmtAgo, nowSec, pct, rankOf, RANKS, type Rank } from "@/lib/game";
import { getLeaderboard, getRankCounts, type LeaderRow } from "@/lib/queries";

export const metadata: Metadata = { title: "Classement Elo" };

export default function Page({ searchParams }: PageProps<"/classement">) {
  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <h1 className="title-game text-4xl">Classement Elo</h1>
          <p className="mt-1 text-ink-dim">Les matchs classés en partie rapide font monter ou descendre ton Elo.</p>
        </div>
      </div>
      <Suspense fallback={<PageSkeleton />}>
        <Leaderboard searchParams={searchParams} />
      </Suspense>
    </div>
  );
}

/** [min, max[ d'Elo d'un rang (RANKS va du plus haut au plus bas). */
function rankRange(r: Rank): [number, number] {
  const i = RANKS.indexOf(r);
  return [r.min, i > 0 ? RANKS[i - 1].min : 2147483647];
}

function href(query: string, rank: string | null) {
  const p = new URLSearchParams();
  if (query) p.set("q", query);
  if (rank) p.set("rang", rank);
  const s = p.toString();
  return s ? `/classement?${s}` : "/classement";
}

/** Filtre par rang : une pastille par rang, du plus bas au plus haut, avec son nombre de joueurs. */
function RankFilter({ query, current, counts }: { query: string; current: Rank | null; counts: Record<string, number> }) {
  return (
    <nav aria-label="Filtrer par rang" className="flex flex-wrap gap-1.5">
      <Link
        href={href(query, null)}
        aria-current={current == null ? "true" : undefined}
        className={`flex h-[30px] items-center rounded-full border px-3 text-xs font-bold transition ${current == null ? "border-gold-300 bg-gold-300/15 text-gold-100" : "border-line bg-black/20 text-ink-dim hover:border-line-strong hover:text-ink"}`}
      >
        Tous
      </Link>
      {[...RANKS].reverse().map((r) => {
        const on = current?.name === r.name;
        const n = counts[r.name] ?? 0;
        return (
          <Link
            key={r.name}
            href={href(query, on ? null : r.name)}
            aria-current={on ? "true" : undefined}
            title={`${r.name} : ${r.min}+ Elo, ${n} joueur${n > 1 ? "s" : ""}`}
            className={`flex items-center gap-1.5 rounded-full border py-0.5 pl-0.5 pr-2.5 text-xs font-bold transition ${on ? "bg-black/40 text-ink" : "border-line bg-black/20 text-ink-dim hover:border-line-strong hover:text-ink"} ${n === 0 && !on ? "opacity-50" : ""}`}
            style={on ? { borderColor: r.color } : undefined}
          >
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={`/img/ranks/${r.badge}`} alt="" width={22} height={22} className="rounded-full" />
            <span style={on ? { color: r.color } : undefined}>{r.name}</span>
            <span className="tabular text-ink-muted">{n}</span>
          </Link>
        );
      })}
    </nav>
  );
}

async function Leaderboard({ searchParams }: { searchParams: PageProps<"/classement">["searchParams"] }) {
  await connection();
  const sp = await searchParams;
  const query = (typeof sp.q === "string" ? sp.q : "").trim();
  const rank = RANKS.find((r) => r.name === sp.rang) ?? null;
  const [players, counts] = await Promise.all([getLeaderboard(query, rank ? rankRange(rank) : null), getRankCounts()]);
  if (query && !rank && players.length === 1) redirect(`/player/${players[0].id}`);
  const now = nowSec();
  const podium = !query && !rank && players.length >= 3 ? players.slice(0, 3) : [];
  const empty = query && rank ? `Aucun joueur ${rank.name} trouvé pour « ${query} ».`
    : query ? `Aucun joueur trouvé pour « ${query} ».`
    : rank ? `Aucun joueur ${rank.name} pour le moment.` : "Aucun joueur classé pour le moment.";

  return (
    <>
      {podium.length > 0 && <Podium players={podium} />}

      <div className="card overflow-hidden">
        <div className="flex flex-wrap items-center justify-between gap-3 border-b border-line px-5 py-4">
          <PlayerSearch key={query} variant="filter" defaultValue={query} rank={rank?.name ?? null} />
          {query && (
            <Link href={href("", rank?.name ?? null)} className="text-sm font-bold text-ink-dim hover:text-gold-300">
              Effacer « {query} »
            </Link>
          )}
        </div>
        <div className="border-b border-line px-5 py-3">
          <RankFilter query={query} current={rank} counts={counts} />
        </div>
        {players.length === 0 ? (
          <Empty>{empty}</Empty>
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
                {players.map((p) => (
                  <Row key={p.id} p={p} pos={p.pos} now={now} />
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
