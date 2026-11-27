import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { connection } from "next/server";
import { Suspense, type ReactNode } from "react";
import Comparison from "@/components/Comparison";
import { Delta, PageSkeleton, SectionTitle, TeamLogo } from "@/components/ui";
import { HeatPitch, TacticPitch, type TacticPlayer } from "@/components/Pitch";
import { ShotPitch } from "@/components/ShotPitch";
import { fmtDate, footballerName, KINDS, pct, ROLES, SCHEMES, team } from "@/lib/game";
import { getMatch } from "@/lib/queries";

export const metadata: Metadata = { title: "Match" };

export default function Page({ params }: PageProps<"/match/[id]">) {
  return (
    <Suspense fallback={<PageSkeleton />}>
      <MatchDetail params={params} />
    </Suspense>
  );
}

function toGrid(data: number[] | null, depth: number | null, lateral: number | null): number[][] | null {
  if (!data || !depth || !lateral) return null;
  return Array.from({ length: depth }, (_, d) => data.slice(d * lateral, (d + 1) * lateral));
}

async function MatchDetail({ params }: { params: PageProps<"/match/[id]">["params"] }) {
  await connection();
  const { id } = await params;
  const matchId = Number(id);
  if (!Number.isInteger(matchId) || matchId <= 0) notFound();
  const data = await getMatch(matchId);
  if (!data) notFound();
  const { m, sides, footballers, events } = data;
  const host = sides.find((s) => s.side === 0);
  const guest = sides.find((s) => s.side === 1);

  const timeline = events
    .filter((e) => e.kind !== "shot")
    .map((e) => {
      const side = sides.find((s) => s.side === e.side);
      let who: string | null = null;
      if (e.kind === "own_goal") {
        // le joueur qui marque contre son camp est de l'autre équipe
        who = footballerName(sides.find((s) => s.side !== e.side)?.animal, e.number);
      } else if (e.number != null) {
        who = footballerName(side?.animal, e.number);
      }
      return { ...e, who };
    });

  return (
    <div className="space-y-6">
      {/* --- En-tête --- */}
      <div className="card relative overflow-hidden px-4 py-7 sm:px-8">
        <div className="pointer-events-none absolute inset-0 opacity-40" style={{ background: `radial-gradient(circle at 15% 50%, ${team(m.team_host).color}33, transparent 45%), radial-gradient(circle at 85% 50%, ${team(m.team_guest).color}33, transparent 45%)` }} />
        <div className="relative grid grid-cols-[1fr_auto_1fr] items-center gap-3 text-center sm:gap-6">
          <SideHead id={m.host_id} name={m.host_name} animal={m.team_host} delta={m.rated ? m.delta_host : null} />
          <div>
            <div className="font-display text-5xl tabular sm:text-7xl">
              <span className={m.score_host > m.score_guest ? "title-gold" : "text-ball"}>{m.score_host}</span>
              <span className="mx-2 text-ink-muted">–</span>
              <span className={m.score_guest > m.score_host ? "title-gold" : "text-ball"}>{m.score_guest}</span>
            </div>
            {m.forfeit && <div className="chip mt-2 border border-loss/40 bg-loss/15 text-loss">Forfait {m.forfeit_side === 0 ? m.host_name : m.guest_name}</div>}
            {m.golden_goal && <div className="mt-2 text-sm font-bold text-gold-300">But en or</div>}
          </div>
          <SideHead id={m.guest_id} name={m.guest_name} animal={m.team_guest} delta={m.rated ? m.delta_guest : null} />
        </div>
        <div className="relative mt-5 flex flex-wrap justify-center gap-x-3 gap-y-1 text-sm text-ink-dim">
          <span className="chip border border-line-strong text-gold-100">{KINDS[m.kind] ?? m.kind}</span>
          <span>{fmtDate(m.played)}</span>
          {m.duration_s != null && m.duration_s < 3 * 3600 && <span>· {Math.floor(m.duration_s / 60)} min {m.duration_s % 60} s</span>}
          {m.stadium != null && <span>· stade {m.stadium}</span>}
          {m.room_code && m.kind === "private" && <span>· salon {m.room_code}</span>}
        </div>
      </div>

      {!m.has_stats || !host || !guest ? (
        <div className="card px-6 py-10 text-center text-ink-muted">
          Pas de statistiques pour ce match (le jeu de l&apos;hôte ne les a pas envoyées).
        </div>
      ) : (
        <>
          <div className="grid gap-6 lg:grid-cols-[1.4fr_1fr]">
            <Comparison host={host} guest={guest} />
            <div className="card p-5">
              <SectionTitle>Fil du match</SectionTitle>
              {timeline.length === 0 ? (
                <p className="text-ink-muted">Aucun but ni super.</p>
              ) : (
                <ol className="relative space-y-1.5 before:absolute before:inset-y-0 before:left-1/2 before:w-px before:bg-line">
                  {timeline.map((e) => {
                    const superImg = e.kind === "super" && e.super_type != null
                      ? <Image src={`/img/supers/super_${e.super_type}.png`} alt={`Super ${e.super_type}`} width={48} height={19} className="inline-block" unoptimized />
                      : null;
                    const label = e.kind === "goal" ? `⚽ ${e.who ?? "But"}` : e.kind === "own_goal" ? `⚽ ${e.who} (csc)` : superImg ?? `⚡ Super ${e.super_type ?? ""}`;
                    const goal = e.kind !== "super";
                    return (
                      <li key={e.seq} className="relative grid grid-cols-[1fr_3rem_1fr] items-center gap-2 text-sm">
                        <span className={`text-right ${goal ? "font-bold text-ink" : "text-ink-dim"}`}>{e.side === 0 ? label : ""}</span>
                        <span className={`z-10 mx-auto grid h-7 min-w-10 place-items-center rounded-full px-1.5 font-display tabular ${goal ? "btn-pill text-sm" : "border border-line bg-grass-900 text-xs text-ink-dim"}`}>
                          {e.minute}&apos;
                        </span>
                        <span className={goal ? "font-bold text-ink" : "text-ink-dim"}>{e.side === 1 ? label : ""}</span>
                      </li>
                    );
                  })}
                </ol>
              )}
            </div>
          </div>

          {sides.map((s) => {
            const t = team(s.animal);
            const mine = footballers.filter((f) => f.side === s.side);
            const layout = s.layouts?.play ?? null;
            const players: TacticPlayer[] = mine.map((f) => ({
              number: f.number,
              slot: f.slot,
              name: footballerName(s.animal, f.number),
              avg: f.avg_depth != null && f.avg_lateral != null ? [f.avg_depth, f.avg_lateral] : null,
              planned: layout && f.slot != null && f.slot >= 0 && f.slot < layout.length ? layout[f.slot] : null,
            }));
            const heat = toGrid(s.heatmap, s.heat_depth, s.heat_lateral);
            const ball = toGrid(s.ball_heatmap, s.heat_depth, s.heat_lateral);
            const shots = events.filter((e) => e.side === s.side && e.kind === "shot");
            return (
              <section key={s.side} className="card p-5">
                <div className="mb-5 flex flex-wrap items-center gap-3">
                  <TeamLogo animal={s.animal} size={44} />
                  <div>
                    <h2 className="title-game text-2xl">{s.name} <span className="text-ink-dim">· {t.name}</span></h2>
                    <p className="text-sm text-ink-dim">Schéma : {SCHEMES[s.scheme ?? ""] ?? s.scheme ?? "tactique inconnue"}</p>
                  </div>
                </div>
                <p className="mb-4 rounded-xl border border-line bg-black/20 px-4 py-2.5 text-sm text-ink-dim">
                  Les terrains ci-dessous sont vus du côté de <b className="text-ink">{s.name}</b> : son but est à gauche, elle attaque vers la droite.
                </p>
                <div className="grid gap-6 md:grid-cols-2">
                  <PitchFigure
                    title="Placement des joueurs"
                    help={
                      players.some((p) => p.planned)
                        ? "Chaque pastille est la position moyenne d'un joueur pendant le match. Le cercle pointillé relié à elle est la place que le joueur lui avait donnée dans l'écran Tactique : plus le trait est long, plus il s'est éloigné de son poste."
                        : `Chaque pastille est la position moyenne d'un joueur pendant le match. ${s.name} a laissé le placement automatique dans l'écran Tactique, il n'y a donc pas de place prévue à comparer.`
                    }
                    legend={players.some((p) => p.planned) ? <TacticLegend color={t.color} /> : undefined}
                  >
                    <TacticPitch players={players} color={t.color} id={`tac-${s.side}`} />
                  </PitchFigure>
                  <PitchFigure title={`Tirs (${shots.length})`} help="Chaque point est un tir, à l'endroit d'où il est parti. Survole un point pour voir le tireur et la minute ; clique sur une issue sous la carte pour la cacher ou l'afficher.">
                    <ShotPitch shots={shots} animal={s.animal} id={`shot-${s.side}`} />
                  </PitchFigure>
                  {heat && (
                    <PitchFigure
                      title="Zones occupées par l'équipe"
                      help={`Carte de chaleur : où se trouvaient les joueurs de champ de ${s.name} pendant le match (sans le gardien, qui reste dans sa surface). Plus une zone est jaune vif, plus ils y ont passé de temps.`}
                      legend={<HeatLegend color="#ffd84a" />}
                    >
                      <HeatPitch grid={heat} color="#ffd84a" label="Zones occupées par l'équipe" id={`heat-${s.side}`} fieldScale />
                    </PitchFigure>
                  )}
                  {ball && (
                    <PitchFigure
                      title="Où était le ballon pendant sa possession"
                      help={`Seulement les moments où ${s.name} avait le ballon : plus une zone est bleu vif, plus le jeu s'y est déroulé. Beaucoup de bleu à droite = une équipe qui attaque, à gauche = une équipe qui ressort de sa défense.`}
                      legend={<HeatLegend color="#70a6ff" />}
                    >
                      <HeatPitch grid={ball} color="#70a6ff" label="Position du ballon pendant la possession" id={`ball-${s.side}`} />
                    </PitchFigure>
                  )}
                </div>
                <div className="mt-5 overflow-x-auto">
                  <table className="data-table">
                    <thead>
                      <tr><th>Joueur</th><th>Rôle</th><th className="text-right">Buts</th><th className="text-right">Tirs</th><th className="text-right">Passes</th><th className="text-right">Récup.</th><th className="text-right">Distance</th></tr>
                    </thead>
                    <tbody>
                      {mine.map((f) => (
                        <tr key={f.number}>
                          <td className="font-bold">
                            {footballerName(s.animal, f.number)}
                            {f.number === 0 && <span className="ml-1.5 font-normal text-ink-muted">(gardien)</span>}
                          </td>
                          <td className="text-ink-dim">{ROLES[f.role ?? ""] ?? f.role ?? "—"}</td>
                          <td className="text-right tabular">
                            {f.goals > 0 ? <span className="font-extrabold text-gold-300">{f.goals}</span> : f.goals}
                            {f.own_goals > 0 && <span className="ml-1 text-loss">({f.own_goals} csc)</span>}
                          </td>
                          <td className="text-right tabular">{f.shots}</td>
                          <td className="text-right tabular">{f.passes_ok}/{f.passes} <span className="text-xs text-ink-muted">{pct(f.passes_ok, f.passes)} %</span></td>
                          <td className="text-right tabular">{f.recoveries}</td>
                          <td className="text-right tabular">{f.distance} m</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </section>
            );
          })}
        </>
      )}
    </div>
  );
}

function PitchFigure({ title, help, legend, children }: { title: string; help: string; legend?: ReactNode; children: ReactNode }) {
  return (
    <figure className="flex flex-col">
      <figcaption className="mb-2.5">
        <h3 className="font-display text-lg text-gold-100">{title}</h3>
        <p className="text-sm leading-snug text-ink-dim">{help}</p>
      </figcaption>
      <div className="mt-auto">
        {children}
        <div className="mt-1.5 flex items-center justify-between text-[0.7rem] font-bold uppercase tracking-wide text-ink-muted">
          <span>◀ son but</span>
          <span>attaque ▶</span>
        </div>
        {legend}
      </div>
    </figure>
  );
}

function TacticLegend({ color }: { color: string }) {
  return (
    <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-xs text-ink-dim">
      <span className="inline-flex items-center gap-1.5">
        <i className="inline-block size-3 rounded-full border border-dashed border-white" />
        Place prévue
      </span>
      <span className="inline-flex items-center gap-1.5">
        <i className="inline-block size-3 rounded-full border border-black/60" style={{ background: color }} />
        Position moyenne réelle
      </span>
    </div>
  );
}

function HeatLegend({ color }: { color: string }) {
  return (
    <div className="mt-2 flex items-center gap-2 text-xs text-ink-dim">
      <span>Rarement</span>
      <span className="h-2 w-28 rounded-full" style={{ background: `linear-gradient(to right, rgba(0,0,0,0.35), ${color})` }} />
      <span>Souvent</span>
    </div>
  );
}

function SideHead({ id, name, animal, delta }: { id: number | null; name: string; animal: number | null; delta: number | null }) {
  return (
    <div className="flex min-w-0 flex-col items-center gap-1.5">
      <TeamLogo animal={animal} size={84} className="drop-shadow-[0_6px_12px_rgba(0,0,0,0.5)]" />
      {id ? (
        <Link href={`/player/${id}`} className="max-w-full truncate font-display text-xl hover:text-gold-300 sm:text-2xl">{name}</Link>
      ) : (
        <span className="max-w-full truncate font-display text-xl sm:text-2xl">{name}</span>
      )}
      <span className="hidden text-sm text-ink-dim sm:block">{team(animal).name}</span>
      {delta != null && <span className="text-sm"><Delta value={delta} suffix=" Elo" /></span>}
    </div>
  );
}
