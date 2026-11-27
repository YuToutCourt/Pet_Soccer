import type { Metadata } from "next";
import { connection } from "next/server";
import { Suspense } from "react";
import BarList from "@/components/BarList";
import SortTable from "@/components/SortTable";
import TeamScatter from "@/components/charts/TeamScatter";
import { Empty, KindTabs, PageSkeleton, SectionTitle, TeamLogo, TeamTag } from "@/components/ui";
import Image from "next/image";
import Link from "next/link";
import { GoalMap, ZoneTable } from "@/components/Pitch";
import { footballerName, parseKind, pct, ROLES, SCHEMES, score, SUPERS, team } from "@/lib/game";
import { getMeta, getShotMap, SHOT_D, SHOT_L } from "@/lib/queries";

export const metadata: Metadata = { title: "Méta" };

export default function Page({ searchParams }: PageProps<"/meta">) {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="title-game text-4xl">Méta</h1>
        <p className="mt-1 text-ink-dim">Les équipes, schémas et rôles qui gagnent.</p>
      </div>
      <Suspense fallback={<PageSkeleton />}>
        <Meta searchParams={searchParams} />
      </Suspense>
    </div>
  );
}

/** Couleur divergente : rouge (perd) → gris (50 %) → vert (gagne). */
function wrColor(wr: number): string {
  const t = Math.min(1, Math.abs(wr - 50) / 35);
  const hue = wr >= 50 ? "95, 211, 141" : "255, 90, 100";
  return `rgba(${hue}, ${(0.08 + t * 0.55).toFixed(2)})`;
}

async function Meta({ searchParams }: { searchParams: PageProps<"/meta">["searchParams"] }) {
  await connection();
  const sp = await searchParams;
  const kind = parseKind(sp.kind);
  const [{ total, teams, schemes, roles, matchups, scorers, supers }, goalMap] = await Promise.all([getMeta(kind), getShotMap(kind)]);
  const grid = new Map(matchups.map((r) => [`${r.a}-${r.b}`, r]));
  const animals = [...new Set(teams.map((t) => t.animal))].sort((a, b) => a - b);

  return (
    <>
      <div className="flex flex-wrap items-center justify-between gap-3">
        <KindTabs base="/meta" kind={kind} />
        <span className="text-sm font-bold text-ink-muted tabular">{total.toLocaleString("fr-FR")} matchs</span>
      </div>

      {!total ? (
        <div className="card"><Empty>Aucun match enregistré pour le moment.</Empty></div>
      ) : (
        <>
          <section className="card p-5">
            <SectionTitle sub="Plus à droite : plus choisie. Plus haut : meilleur score (victoire = 1, nul = ½). La ligne pointillée marque 50 %. Clique sur une équipe pour sa fiche.">Équipes</SectionTitle>
            <TeamScatter
              data={teams.map((t) => ({
                animal: t.animal, name: team(t.animal).name, logo: team(t.animal).logo,
                pick: pct(t.n, total * 2), win: score(t.w, t.d, t.n), n: t.n,
              }))}
            />
            <div className="mt-4 overflow-x-auto">
              <SortTable
                initial={{ col: 1, desc: true }}
                cols={[
                  { label: "Équipe" }, { label: "Taux de choix", num: true, title: "Part des équipes choisies (nombre de matchs entre parenthèses)" },
                  { label: "Score", num: true, title: "Victoire = 1, nul = ½" }, { label: "Nuls", num: true },
                  { label: "Buts / match", num: true }, { label: "Buts subis / match", num: true },
                  { label: "Supers / match", num: true }, { label: "Possession", num: true },
                ]}
                rows={teams.map((t) => {
                  const wr = score(t.w, t.d, t.n);
                  return {
                    key: t.animal,
                    values: [team(t.animal).name, t.n, wr, t.d, t.goals ?? 0, t.conceded ?? 0, t.supers, t.possession],
                    classes: [undefined, undefined, `text-right font-extrabold tabular ${wr >= 55 ? "text-win" : wr < 45 ? "text-loss" : "text-draw"}`],
                    cells: [
                      <Link key="t" href={kind ? `/meta/${t.animal}?kind=${kind}` : `/meta/${t.animal}`} className="hover:text-gold-300"><TeamTag animal={t.animal} /></Link>,
                      <span key="p">{pct(t.n, total * 2)} % <span className="text-ink-dim">({t.n})</span></span>,
                      `${wr} %`,
                      t.d,
                      (t.goals ?? 0).toFixed(2),
                      (t.conceded ?? 0).toFixed(2),
                      t.supers == null ? "—" : t.supers.toFixed(2),
                      t.possession == null ? "—" : `${t.possession.toFixed(0)} %`,
                    ],
                  };
                })}
              />
            </div>
          </section>

          <section className="card p-5">
            <SectionTitle sub="Score (victoire = 1, nul = ½) : résultat de l'équipe qui a lancé le super, un match comptant une fois par équipe même si elle le lance plusieurs fois.">
              Supers
            </SectionTitle>
            {supers.length ? (
              <div className="overflow-x-auto">
                <SortTable
                  initial={{ col: 1, desc: true }}
                  cols={[
                    { label: "Super" }, { label: "Utilisations", num: true }, { label: "Par match", num: true },
                    { label: "Matchs", num: true }, { label: "Score", num: true, title: "Victoire = 1, nul = ½" }, { label: "Nuls", num: true },
                  ]}
                  rows={supers.map((s) => {
                    const wr = score(s.w, s.d, s.n);
                    const name = SUPERS[s.super_type] ?? `Super ${s.super_type}`;
                    return {
                      key: s.super_type,
                      values: [name, s.uses, s.uses, s.n, wr, s.d],
                      classes: [undefined, undefined, undefined, undefined, `text-right font-extrabold tabular ${wr >= 55 ? "text-win" : wr < 45 ? "text-loss" : "text-draw"}`],
                      cells: [
                        <span key="s" className="flex items-center gap-2">
                          <Image src={`/img/supers/super_${s.super_type}.png`} alt="" width={48} height={19} className="inline-block" unoptimized />
                          <span className="font-bold">{name}</span>
                        </span>,
                        s.uses,
                        (s.uses / total).toFixed(2),
                        s.n,
                        `${wr} %`,
                        s.d,
                      ],
                    };
                  })}
                />
              </div>
            ) : (
              <Empty>Aucun super lancé.</Empty>
            )}
          </section>

          <section className="card p-5">
            <SectionTitle sub="Endroit d'où partent les tirs gagnants, toutes équipes confondues (l'équipe attaque vers la droite). Plus c'est rouge, plus il y a de buts ; survole une case pour le détail. « Devant la surface » : jusqu'à 30 % de la longueur du terrain ; « De loin » : au-delà. Les buts contre son camp n'ont pas de position.">
              Buts
            </SectionTitle>
            <div className="grid items-start gap-6 lg:grid-cols-[1.1fr_1fr]">
              <GoalMap map={goalMap} d={SHOT_D} l={SHOT_L} id="goals-all" />
              <div className="overflow-x-auto"><ZoneTable map={goalMap} /></div>
            </div>
          </section>

          <div className="grid gap-6 lg:grid-cols-3">
            <section className="card p-5">
              <SectionTitle>Schémas</SectionTitle>
              {schemes.length ? (
                <BarList items={schemes.map((s) => ({ key: s.scheme, label: SCHEMES[s.scheme] ?? s.scheme, n: s.n, w: s.w, d: s.d }))} />
              ) : (
                <Empty>Pas encore de tactique enregistrée.</Empty>
              )}
            </section>
            <section className="card p-5">
              <SectionTitle>Rôles</SectionTitle>
              {roles.length ? (
                <BarList unit="place" items={roles.map((r) => ({ key: r.role, label: ROLES[r.role] ?? r.role, n: r.n, w: r.w, d: r.d }))} />
              ) : (
                <Empty>Pas encore de rôles.</Empty>
              )}
            </section>
            <section className="card p-5">
              <SectionTitle>Meilleurs buteurs</SectionTitle>
              {scorers.length ? (
                <ol className="space-y-1.5">
                  {scorers.map((s, i) => (
                    <li key={`${s.animal}-${s.number}`} className="flex items-center gap-3 rounded-xl px-2 py-1.5 odd:bg-black/15">
                      <span className={`w-5 text-center font-display ${i < 3 ? "text-gold-300" : "text-ink-muted"}`}>{i + 1}</span>
                      <TeamLogo animal={s.animal} size={24} />
                      <span className="min-w-0 flex-1 truncate font-bold">{footballerName(s.animal, s.number)}</span>
                      <span className="text-xs text-ink-muted tabular">{s.shots} tirs</span>
                      <span className="w-10 text-right font-display text-lg tabular text-gold-100">{s.goals}</span>
                    </li>
                  ))}
                </ol>
              ) : (
                <Empty>Aucun but enregistré.</Empty>
              )}
            </section>
          </div>

          {animals.length > 1 && (
            <section className="card p-5">
              <SectionTitle sub="Score de l'équipe en ligne contre l'équipe en colonne (victoire = 1, nul = ½). Survole une case pour le détail.">
                Équipe contre équipe
              </SectionTitle>
              <div className="overflow-x-auto">
                <table className="border-separate border-spacing-0.5 text-xs">
                  <thead>
                    <tr>
                      <th />
                      {animals.map((b) => (
                        <th key={b} className="p-0.5"><Link href={`/meta/${b}`}><TeamLogo animal={b} size={26} className="mx-auto" /></Link></th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {animals.map((a) => (
                      <tr key={a}>
                        <th className="p-0.5 pr-1.5"><Link href={`/meta/${a}`}><TeamLogo animal={a} size={26} /></Link></th>
                        {animals.map((b) => {
                          const c = grid.get(`${a}-${b}`);
                          if (!c) return <td key={b} className="size-9 rounded-md bg-black/15 text-center text-ink-muted">·</td>;
                          const wr = score(c.w, c.d, c.n);
                          return (
                            <td
                              key={b}
                              title={`${team(a).name} contre ${team(b).name} : ${c.w} victoire${c.w > 1 ? "s" : ""}, ${c.d} nul${c.d > 1 ? "s" : ""} sur ${c.n}`}
                              className="size-9 rounded-md text-center font-extrabold tabular text-ink"
                              style={{ background: wrColor(wr) }}
                            >
                              {wr}
                            </td>
                          );
                        })}
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="mt-3 flex items-center gap-2 text-xs text-ink-muted">
                <span>Perd</span>
                <span className="h-2 w-40 rounded-full" style={{ background: `linear-gradient(90deg, ${wrColor(15)}, ${wrColor(50)}, ${wrColor(85)})` }} />
                <span>Gagne</span>
              </div>
            </section>
          )}
        </>
      )}
    </>
  );
}
