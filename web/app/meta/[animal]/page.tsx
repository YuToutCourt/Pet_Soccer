import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { connection } from "next/server";
import { Suspense, type ReactNode } from "react";
import BarList from "@/components/BarList";
import ResultDonut from "@/components/charts/ResultDonut";
import { GoalMap, HeatPitch, ZoneTable } from "@/components/Pitch";
import { Empty, KindTabs, MatchLine, PageSkeleton, SectionTitle, StatTile, TeamLogo, TeamTag } from "@/components/ui";
import { footballerName, nowSec, parseKind, pct, ROLES, SCHEMES, score, SUPERS, team, TEAMS } from "@/lib/game";
import { getTeamMeta, type PossessionDiag, SHOT_D, SHOT_L } from "@/lib/queries";

export async function generateMetadata({ params }: PageProps<"/meta/[animal]">): Promise<Metadata> {
  const { animal } = await params;
  return { title: TEAMS[Number(animal)]?.name ?? "Équipe" };
}

export default function Page({ params, searchParams }: PageProps<"/meta/[animal]">) {
  return (
    <Suspense fallback={<PageSkeleton />}>
      <TeamPage params={params} searchParams={searchParams} />
    </Suspense>
  );
}

const wrTone = (wr: number) => (wr >= 55 ? "text-win" : wr < 45 ? "text-loss" : "text-draw");
const PERIODS = ["1-15", "16-30", "31-45", "46-60", "61-75", "76-90"];

async function TeamPage({ params, searchParams }: { params: PageProps<"/meta/[animal]">["params"]; searchParams: PageProps<"/meta/[animal]">["searchParams"] }) {
  await connection();
  const [{ animal: raw }, sp] = await Promise.all([params, searchParams]);
  const animal = Number(raw);
  if (!Number.isInteger(animal) || !(animal in TEAMS)) notFound();
  const kind = parseKind(sp.kind);
  const data = await getTeamMeta(animal, kind);
  const t = team(animal);
  const back = kind ? `/meta?kind=${kind}` : "/meta";

  if (!data) {
    return (
      <div className="space-y-6">
        <Header animal={animal} back={back} />
        <KindTabs base={`/meta/${animal}`} kind={kind} />
        <div className="card"><Empty>Aucun match de {t.name} pour le moment.</Empty></div>
      </div>
    );
  }

  const { total, avg, rankPos, teamsRanked, opponents, footballers, supers, schemes, heatGrid, minutes, recent, shotsFor, shotsAgainst, possession } = data;
  const wr = score(avg.w, avg.d, avg.n);
  const goalsFor = footballers.reduce((a, f) => a + f.goals, 0);
  const now = nowSec();
  const maxMin = Math.max(1, ...minutes.flatMap((m) => [m.scored, m.conceded]));

  return (
    <div className="space-y-6">
      <Header animal={animal} back={back}>
        <div className="mt-2 flex flex-wrap items-center gap-x-4 gap-y-1 text-sm">
          <span className="chip" style={{ color: t.color, background: `${t.color}1f`, border: `1px solid ${t.color}55` }}>
            #{rankPos} sur {teamsRanked} au score · <span className={`font-extrabold ${wrTone(wr)}`}>{wr} %</span>
          </span>
          <span className="text-ink-dim">Choisie dans <b className="text-ink">{pct(avg.n, total * 2)} %</b> des équipes alignées</span>
        </div>
      </Header>

      <KindTabs base={`/meta/${animal}`} kind={kind} />

      <section className="grid grid-cols-2 gap-3 sm:grid-cols-4">
        <StatTile label="Matchs" value={avg.n} hint={`${pct(avg.n, total * 2)} % de choix`} />
        <StatTile label="Victoires" value={avg.w} tone="win" hint={`${pct(avg.w, avg.n)} %`} />
        <StatTile label="Nuls" value={avg.d} tone="draw" hint={`${pct(avg.d, avg.n)} %`} />
        <StatTile label="Défaites" value={avg.l} tone="loss" hint={`${pct(avg.l, avg.n)} %`} />
      </section>

      <div className="grid gap-6 lg:grid-cols-[1fr_1.4fr]">
        <section className="card p-5">
          <SectionTitle>Bilan</SectionTitle>
          <ResultDonut w={avg.w} d={avg.d} l={avg.l} />
        </section>
        <section className="card p-5">
          <SectionTitle sub="Moyennes par match">Statistiques</SectionTitle>
          <dl className="grid grid-cols-2 gap-x-6 gap-y-2 text-sm sm:grid-cols-[1fr_auto_1fr_auto]">
            <Avg label="Buts marqués" v={avg.goals} digits={2} />
            <Avg label="Buts subis" v={avg.conceded} digits={2} />
            <Avg label="Tirs" v={avg.shots} digits={1} />
            <Avg label="Tirs subis" v={avg.shots_against} digits={1} />
            <Avg label="Réussite des tirs" v={avg.shots ? (100 * (avg.goals ?? 0)) / avg.shots : null} digits={0} unit=" %" />
            <Avg label="Possession" v={avg.possession} digits={0} unit=" %" />
            <Avg label="Précision passes" v={avg.passes ? pct(avg.passes_ok, avg.passes) : null} digits={0} unit=" %" />
            <Avg label="Récupérations" v={avg.recoveries} digits={1} />
            <Avg label="Arrêts du gardien" v={avg.saves} digits={1} />
            <Avg label="Supers lancés" v={avg.supers} digits={2} />
            <Avg label="Sans encaisser" v={pct(avg.clean, avg.n)} digits={0} unit=" %" />
            <Avg label="Marque au moins 1 but" v={pct(avg.scored, avg.n)} digits={0} unit=" %" />
            <Avg label="Record de buts" v={avg.max_goals} digits={0} />
            <Avg label="Plus large victoire" v={avg.best_diff != null && avg.best_diff > 0 ? avg.best_diff : null} digits={0} unit=" buts d'écart" />
          </dl>
        </section>
      </div>

      {possession.mine && possession.opp ? <PossessionSection mine={possession.mine} opp={possession.opp} /> : null}

      <section className="card p-5">
        <SectionTitle sub="Endroit d'où partent les tirs gagnants. Survole une case pour le détail ; la case pointillée est la plus prolifique. « Devant la surface » : jusqu'à 30 % de la longueur du terrain ; « De loin » : au-delà.">Buts</SectionTitle>
        <div className="grid gap-6 lg:grid-cols-2">
          <div className="space-y-3">
            <h3 className="font-display text-lg">Où elle marque <span className="text-sm text-ink-muted">(elle attaque vers la droite)</span></h3>
            <GoalMap map={shotsFor} d={SHOT_D} l={SHOT_L} id="goals-for" color="#5fd38d" />
            <div className="overflow-x-auto"><ZoneTable map={shotsFor} /></div>
          </div>
          <div className="space-y-3">
            <h3 className="font-display text-lg">Où elle encaisse <span className="text-sm text-ink-muted">(son but à gauche)</span></h3>
            <GoalMap map={shotsAgainst} d={SHOT_D} l={SHOT_L} id="goals-against" against />
            <div className="overflow-x-auto"><ZoneTable map={shotsAgainst} /></div>
          </div>
        </div>
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="card p-5">
          <SectionTitle sub="Buts marqués et encaissés par quart d'heure (buts contre son camp compris).">Moments des buts</SectionTitle>
          {minutes.length ? (
            <ul className="space-y-2.5">
              {PERIODS.map((label, i) => {
                const m = minutes.find((x) => x.period === i) ?? { scored: 0, conceded: 0 };
                return (
                  <li key={label} className="grid grid-cols-[4rem_1fr] items-center gap-3 text-sm">
                    <span className="tabular text-ink-dim">{label}&apos;</span>
                    <div className="space-y-1">
                      <Bar v={m.scored} max={maxMin} className="bg-win" label={`${m.scored} marqués`} />
                      <Bar v={m.conceded} max={maxMin} className="bg-loss" label={`${m.conceded} encaissés`} />
                    </div>
                  </li>
                );
              })}
            </ul>
          ) : (
            <Empty>Aucun but.</Empty>
          )}
        </section>
        <section className="card p-5">
          <SectionTitle sub="Position moyenne des joueurs de champ sur tous ses matchs (son but à gauche).">Zones occupées</SectionTitle>
          {heatGrid ? <HeatPitch grid={heatGrid} color="#ffd84a" label="Zones occupées par l'équipe" id="team-heat" fieldScale /> : <Empty>Pas de carte enregistrée.</Empty>}
        </section>
      </div>

      <section className="card p-5">
        <SectionTitle>Joueurs</SectionTitle>
        <div className="overflow-x-auto">
          <table className="data-table">
            <thead>
              <tr>
                <th>Joueur</th><th>Poste</th><th className="text-right">Buts</th><th className="text-right">Buts / match</th>
                <th className="text-right">Part des buts</th><th className="text-right">Tirs</th><th className="text-right">Réussite</th>
                <th className="text-right">Passes réussies</th><th className="text-right">Récup. / match</th>
              </tr>
            </thead>
            <tbody>
              {footballers.map((f) => (
                <tr key={f.number}>
                  <td className="font-bold">{footballerName(animal, f.number)}</td>
                  <td className="text-ink-dim">{f.number === 0 ? "Gardien" : f.role ? ROLES[f.role] ?? f.role : "—"}</td>
                  <td className="text-right font-extrabold tabular">{f.goals}</td>
                  <td className="text-right tabular">{(f.goals / f.n).toFixed(2)}</td>
                  <td className="text-right tabular">{pct(f.goals, goalsFor)} %</td>
                  <td className="text-right tabular">{f.shots}</td>
                  <td className="text-right tabular">{f.shots ? `${pct(f.goals, f.shots)} %` : "—"}</td>
                  <td className="text-right tabular">{f.passes ? `${pct(f.passes_ok, f.passes)} %` : "—"}</td>
                  <td className="text-right tabular">{(f.recoveries / f.n).toFixed(1)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </section>

      <section className="card p-5">
        <SectionTitle sub="Classé du meilleur au pire score (victoire = 1, nul = ½).">Contre chaque équipe</SectionTitle>
        <div className="overflow-x-auto">
          <table className="data-table">
            <thead>
              <tr>
                <th>Adversaire</th><th className="text-right">Matchs</th><th className="text-right" title="Victoire = 1, nul = ½">Score</th>
                <th className="text-right">Nuls</th><th className="text-right">Score moyen</th>
              </tr>
            </thead>
            <tbody>
              {opponents.map((o) => {
                const owr = score(o.w, o.d, o.n);
                return (
                  <tr key={o.animal}>
                    <td><Link href={kind ? `/meta/${o.animal}?kind=${kind}` : `/meta/${o.animal}`} className="hover:text-gold-300"><TeamTag animal={o.animal} /></Link></td>
                    <td className="text-right tabular">{o.n}</td>
                    <td className={`text-right font-extrabold tabular ${wrTone(owr)}`}>{owr} %</td>
                    <td className="text-right tabular">{o.d}</td>
                    <td className="text-right tabular">{o.goals.toFixed(1)} – {o.conceded.toFixed(1)}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </section>

      <div className="grid gap-6 lg:grid-cols-2">
        <section className="card p-5">
          <SectionTitle sub="Score (victoire = 1, nul = ½) : matchs où l'équipe a lancé ce super.">Supers</SectionTitle>
          {supers.length ? (
            <BarList
              items={supers.map((s) => ({
                key: String(s.super_type),
                n: s.n,
                w: s.w,
                d: s.d,
                label: (
                  <span className="inline-flex items-center gap-2">
                    <Image src={`/img/supers/super_${s.super_type}.png`} alt="" width={40} height={16} className="inline-block" unoptimized />
                    {SUPERS[s.super_type] ?? `Super ${s.super_type}`}
                    <span className="text-xs font-normal text-ink-muted">{s.uses} lancés</span>
                  </span>
                ),
              }))}
            />
          ) : (
            <Empty>Aucun super lancé.</Empty>
          )}
        </section>
        <section className="card p-5">
          <SectionTitle>Schémas tactiques</SectionTitle>
          {schemes.length ? (
            <BarList items={schemes.map((s) => ({ key: s.scheme, n: s.n, w: s.w, d: s.d, label: SCHEMES[s.scheme] ?? s.scheme }))} />
          ) : (
            <Empty>Pas encore de tactique enregistrée.</Empty>
          )}
        </section>
      </div>

      <section className="card p-5">
        <SectionTitle>Derniers matchs</SectionTitle>
        <div className="space-y-2">
          {recent.map((m) => <MatchLine key={m.id} m={m} now={now} />)}
        </div>
      </section>
    </div>
  );
}

function Header({ animal, back, children }: { animal: number; back: string; children?: ReactNode }) {
  const t = team(animal);
  return (
    <section className="card relative overflow-hidden p-6 sm:p-8">
      <div className="pointer-events-none absolute -right-20 -top-20 size-80 rounded-full blur-3xl" style={{ background: `${t.color}22` }} />
      <div className="relative flex flex-wrap items-center gap-6">
        <TeamLogo animal={animal} size={96} />
        <div className="min-w-0 flex-1">
          <Link href={back} className="text-sm font-bold text-ink-muted hover:text-gold-300">← Méta</Link>
          <h1 className="title-game truncate text-4xl sm:text-5xl">{t.name}</h1>
          {children}
        </div>
      </div>
    </section>
  );
}

function Avg({ label, v, digits, unit = "" }: { label: string; v: number | null | undefined; digits: number; unit?: string }) {
  return (
    <>
      <dt className="text-ink-muted">{label}</dt>
      <dd className="text-right font-extrabold tabular">{v == null ? "—" : `${v.toFixed(digits)}${unit}`}</dd>
    </>
  );
}

function Bar({ v, max, className, label }: { v: number; max: number; className: string; label: string }) {
  return (
    <div className="flex items-center gap-2" title={label}>
      <div className="h-2 flex-1 overflow-hidden rounded-full bg-black/30">
        <div className={`h-full rounded-full ${className}`} style={{ width: `${(v / max) * 100}%` }} />
      </div>
      <span className="w-6 text-right text-xs tabular text-ink-dim">{v}</span>
    </div>
  );
}

// Temps de balle détaillé (match_possession) : d'où vient la possession affichée par le jeu
function PossessionSection({ mine, opp }: { mine: PossessionDiag; opp: PossessionDiag }) {
  const share = (v: number, p: PossessionDiag) => (p.play > 0 ? (100 * v) / p.play : null);
  const spell = (p: PossessionDiag) => (p.spells > 0 ? (p.hold + p.loose) / p.spells : null);
  const counter = mine.game_counter + opp.game_counter;
  const nobody = mine.play > 0 ? (100 * (mine.play - mine.hold - mine.loose - opp.hold - opp.loose)) / mine.play : null;
  const rows: { label: string; hint?: string; v: (p: PossessionDiag) => number | null; digits: number; unit: string }[] = [
    { label: "Possession (compteur du jeu)", v: (p) => (counter > 0 ? (100 * p.game_counter) / counter : null), digits: 1, unit: " %" },
    { label: "Tient le ballon", hint: "Part du temps de jeu", v: (p) => share(p.hold, p), digits: 1, unit: " %" },
    { label: "Ballon libre après sa touche", hint: "Passe, tir ou dégagement en l'air ou au sol", v: (p) => share(p.loose, p), digits: 1, unit: " %" },
    { label: "Dont le gardien tient le ballon", v: (p) => p.gk_hold, digits: 1, unit: " s" },
    { label: "Phases de jeu par match", hint: "Suite de touches de l'équipe", v: (p) => p.spells, digits: 1, unit: "" },
    { label: "Durée moyenne d'une phase", v: spell, digits: 2, unit: " s" },
    { label: "Phases finies par un arrêt de jeu", hint: "Sortie, but ou faute ; les autres finissent sur une touche adverse", v: (p) => (p.spells > 0 ? (100 * p.spells_dead) / p.spells : null), digits: 0, unit: " %" },
    { label: "Phase la plus longue", hint: "Moyenne par match", v: (p) => p.longest, digits: 1, unit: " s" },
    { label: "Pendant ses supers", hint: "Le compteur du jeu ne compte rien pendant un super", v: (p) => p.super_time, digits: 1, unit: " s" },
    { label: "Compteur du jeu moins la mesure", hint: "Proche de 0 : le compteur du jeu mesure bien le temps de balle", v: (p) => p.game_counter - p.hold - p.loose, digits: 1, unit: " s" },
  ];
  return (
    <section className="card p-5">
      <SectionTitle sub={`Mesuré pendant le jeu sur ${mine.n} match${mine.n > 1 ? "s" : ""} de méta (temps de jeu ${Math.round(mine.play)} s par match, arrêts de jeu exclus). Ballon à personne après un arrêt de jeu : ${nobody == null ? "—" : `${nobody.toFixed(1)} %`} du temps.`}>
        Possession détaillée
      </SectionTitle>
      <div className="overflow-x-auto">
        <table className="data-table">
          <thead>
            <tr><th>Moyenne par match</th><th className="text-right">L&apos;équipe</th><th className="text-right">Ses adversaires</th></tr>
          </thead>
          <tbody>
            {rows.map((r) => {
              const a = r.v(mine);
              const b = r.v(opp);
              return (
                <tr key={r.label}>
                  <td>
                    {r.label}
                    {r.hint ? <span className="block text-xs text-ink-muted">{r.hint}</span> : null}
                  </td>
                  <td className="text-right font-extrabold tabular">{a == null ? "—" : `${a.toFixed(r.digits)}${r.unit}`}</td>
                  <td className="text-right tabular text-ink-dim">{b == null ? "—" : `${b.toFixed(r.digits)}${r.unit}`}</td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}
