import type { Metadata } from "next";
import Link from "next/link";
import { notFound, redirect } from "next/navigation";
import { connection } from "next/server";
import { Suspense } from "react";
import BarList from "@/components/BarList";
import EloChart from "@/components/charts/EloChart";
import ResultDonut from "@/components/charts/ResultDonut";
import { Delta, Empty, PageSkeleton, RankBadge, ResultTag, SectionTitle, StatTile, TeamLogo } from "@/components/ui";
import { fmtAgo, fmtDate, KINDS, nowSec, pct, rankOf, RANKS, SCHEMES, team } from "@/lib/game";
import { findPlayerByName, getPlayer } from "@/lib/queries";

export const metadata: Metadata = { title: "Profil" };

export default function Page({ params }: PageProps<"/player/[key]">) {
  return (
    <Suspense fallback={<PageSkeleton />}>
      <Profile params={params} />
    </Suspense>
  );
}

async function Profile({ params }: { params: PageProps<"/player/[key]">["params"] }) {
  await connection();
  const { key } = await params;
  const raw = decodeURIComponent(key);
  if (!/^\d+$/.test(raw)) {
    // ancien lien /player/<pseudo>
    const id = await findPlayerByName(raw);
    if (id == null) notFound();
    redirect(`/player/${id}`);
  }
  const data = await getPlayer(Number(raw));
  if (!data) notFound();
  const { p, matches, rankPos, totalPlayers, history, teams, schemes, averages, form } = data;
  const r = rankOf(p.elo);
  const next = [...RANKS].reverse().find((x) => x.min > p.elo);
  const fav = teams[0]?.animal ?? null;
  const now = nowSec();

  return (
    <div className="space-y-6">
      {/* --- En-tête --- */}
      <section className="card relative overflow-hidden p-6 sm:p-8">
        <div className="pointer-events-none absolute -right-20 -top-20 size-80 rounded-full blur-3xl" style={{ background: `${r.color}22` }} />
        <div className="relative flex flex-wrap items-center gap-6">
          <div className="relative">
            <RankBadge elo={p.elo} size={112} />
            {fav != null && (
              <span className="absolute -bottom-2 -right-3 rounded-full border-2 border-grass-900 bg-grass-850 p-1">
                <TeamLogo animal={fav} size={38} />
              </span>
            )}
          </div>
          <div className="min-w-0 flex-1">
            <h1 className="title-game truncate text-4xl sm:text-5xl">{p.name}</h1>
            <div className="mt-2 flex flex-wrap items-center gap-x-3 gap-y-1">
              <span className="chip" style={{ color: r.color, background: `${r.color}1f`, border: `1px solid ${r.color}55` }}>{r.name}</span>
              <span className="font-display text-2xl tabular">{p.elo} <span className="text-base text-ink-dim">Elo</span></span>
              <span className="text-sm text-ink-dim">#{rankPos} sur {totalPlayers}</span>
            </div>
            <p className="mt-2 text-sm text-ink-muted">
              Inscrit le {fmtDate(p.created)} · vu {fmtAgo(p.last_seen, now)}
              {p.peak > p.elo && <> · pic à <b className="text-gold-300">{p.peak}</b></>}
            </p>
            {next && (
              <div className="mt-3 max-w-sm">
                <div className="mb-1 flex justify-between text-xs font-bold text-ink-muted">
                  <span>{r.name}</span>
                  <span>{next.name} dans {next.min - p.elo} Elo</span>
                </div>
                <div className="h-2 overflow-hidden rounded-full bg-black/30">
                  <div className="h-full rounded-full" style={{ width: `${Math.max(4, ((p.elo - r.min) / (next.min - r.min)) * 100)}%`, background: `linear-gradient(90deg, ${r.color}, ${next.color})` }} />
                </div>
              </div>
            )}
          </div>
          {form.length > 0 && (
            <div>
              <div className="label mb-2">Forme récente</div>
              <div className="flex flex-wrap gap-1.5">
                {[...form].reverse().map((f) => (
                  <Link key={f.id} href={`/match/${f.id}`}>
                    <ResultTag result={f.result} size={30} />
                  </Link>
                ))}
              </div>
            </div>
          )}
        </div>
      </section>

      <section className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatTile label="Matchs classés" value={p.games} />
        <StatTile label="Victoires" value={p.wins} tone="win" hint={`${pct(p.wins, p.games)} %`} />
        <StatTile label="Nuls" value={p.draws} tone="draw" />
        <StatTile label="Défaites" value={p.losses} tone="loss" />
      </section>

      <div className="grid gap-6 lg:grid-cols-[1.6fr_1fr]">
        <section className="card p-5">
          <SectionTitle sub={history.length > 1 ? `${history.length - 1} derniers matchs classés` : undefined}>Courbe Elo</SectionTitle>
          {history.length > 1 ? <EloChart data={history} /> : <Empty>Pas encore assez de matchs classés.</Empty>}
        </section>
        <section className="card p-5">
          <SectionTitle>Bilan</SectionTitle>
          {p.games > 0 ? <ResultDonut w={p.wins} d={p.draws} l={p.losses} /> : <Empty>Aucun match classé.</Empty>}
          {averages && averages.n > 0 && (
            <dl className="mt-5 grid grid-cols-2 gap-x-4 gap-y-2 border-t border-line pt-4 text-sm">
              <Avg label="Buts / match" v={averages.goals} digits={2} />
              <Avg label="Tirs / match" v={averages.shots} digits={1} />
              <Avg label="Possession" v={averages.possession} digits={0} unit=" %" />
              <Avg label="Précision passes" v={pct(averages.passes_ok, averages.passes)} digits={0} unit=" %" />
              <Avg label="Récupérations" v={averages.recoveries} digits={1} />
              <Avg label="Record de buts" v={averages.max_goals} digits={0} />
            </dl>
          )}
        </section>
      </div>

      <div className="grid gap-6 md:grid-cols-2">
        <section className="card p-5">
          <SectionTitle>Équipes jouées</SectionTitle>
          {teams.length ? (
            <BarList items={teams.map((t) => ({ key: String(t.animal), n: t.n, w: t.w, label: <span className="inline-flex items-center gap-2"><TeamLogo animal={t.animal} size={22} />{team(t.animal).name}</span> }))} />
          ) : (
            <Empty>Aucune équipe enregistrée.</Empty>
          )}
        </section>
        <section className="card p-5">
          <SectionTitle>Schémas tactiques</SectionTitle>
          {schemes.length ? (
            <BarList items={schemes.map((s) => ({ key: s.scheme, n: s.n, w: s.w, label: SCHEMES[s.scheme] ?? s.scheme }))} />
          ) : (
            <Empty>Pas encore de tactique enregistrée.</Empty>
          )}
        </section>
      </div>

      <section className="card p-5">
        <SectionTitle>Derniers matchs</SectionTitle>
        {matches.length === 0 ? (
          <Empty>Aucun match.</Empty>
        ) : (
          <ul className="divide-y divide-line">
            {matches.map((m) => {
              const isHost = m.host_id === p.id;
              const me = isHost ? { goals: m.score_host, animal: m.team_host, delta: m.delta_host } : { goals: m.score_guest, animal: m.team_guest, delta: m.delta_guest };
              const opp = isHost ? { id: m.guest_id, name: m.guest_name, goals: m.score_guest, animal: m.team_guest } : { id: m.host_id, name: m.host_name, goals: m.score_host, animal: m.team_host };
              const result = me.goals > opp.goals ? "W" : me.goals < opp.goals ? "L" : "D";
              const forfeitLoss = m.forfeit && m.forfeit_side === (isHost ? 0 : 1);
              const res = m.forfeit ? (forfeitLoss ? "L" : "W") : result;
              return (
                <li key={m.id} className="relative flex items-center gap-3 py-3 transition hover:bg-white/[0.02]">
                  <ResultTag result={res} />
                  <TeamLogo animal={me.animal} size={30} />
                  <Link href={`/match/${m.id}`} className="font-display text-xl tabular after:absolute after:inset-0 after:content-['']">
                    {me.goals}–{opp.goals}
                  </Link>
                  <TeamLogo animal={opp.animal} size={30} />
                  <div className="min-w-0 flex-1">
                    <div className="truncate text-sm">
                      contre{" "}
                      {opp.id ? (
                        <Link href={`/player/${opp.id}`} className="relative z-10 font-bold hover:text-gold-300">{opp.name}</Link>
                      ) : (
                        <b>{opp.name}</b>
                      )}
                    </div>
                    <div className="text-xs text-ink-muted">
                      {KINDS[m.kind] ?? m.kind}
                      {m.forfeit && " · forfait"} · {fmtAgo(m.played, now)}
                    </div>
                  </div>
                  {m.rated && <span className="text-sm"><Delta value={me.delta} /></span>}
                </li>
              );
            })}
          </ul>
        )}
      </section>
    </div>
  );
}

function Avg({ label, v, digits, unit = "" }: { label: string; v: number | null; digits: number; unit?: string }) {
  return (
    <>
      <dt className="text-ink-muted">{label}</dt>
      <dd className="text-right font-extrabold tabular">{v == null ? "—" : `${v.toFixed(digits)}${unit}`}</dd>
    </>
  );
}
