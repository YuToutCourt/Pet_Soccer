import type { Metadata } from "next";
import { connection } from "next/server";
import { Suspense } from "react";
import BarList from "@/components/BarList";
import TeamScatter from "@/components/charts/TeamScatter";
import { Empty, KindTabs, PageSkeleton, SectionTitle, TeamLogo, TeamTag } from "@/components/ui";
import { footballerName, parseKind, pct, ROLES, SCHEMES, team } from "@/lib/game";
import { getMeta } from "@/lib/queries";

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
  const { total, teams, schemes, roles, matchups, scorers } = await getMeta(kind);
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
            <SectionTitle sub="Plus à droite : plus choisie. Plus haut : gagne plus souvent. La ligne pointillée marque 50 %.">Équipes</SectionTitle>
            <TeamScatter
              data={teams.map((t) => ({
                animal: t.animal, name: team(t.animal).name, logo: team(t.animal).logo,
                pick: pct(t.n, total * 2), win: pct(t.w, t.n), n: t.n,
              }))}
            />
            <div className="mt-4 overflow-x-auto">
              <table className="data-table">
                <thead>
                  <tr>
                    <th>Équipe</th><th className="text-right">Choix</th><th className="text-right">Taux de choix</th>
                    <th className="text-right">Victoires</th><th className="text-right">Nuls</th><th className="text-right">Buts / match</th><th className="text-right">Possession</th>
                  </tr>
                </thead>
                <tbody>
                  {teams.map((t) => {
                    const wr = pct(t.w, t.n);
                    return (
                      <tr key={t.animal}>
                        <td><TeamTag animal={t.animal} /></td>
                        <td className="text-right tabular">{t.n}</td>
                        <td className="text-right tabular">{pct(t.n, total * 2)} %</td>
                        <td className={`text-right font-extrabold tabular ${wr >= 55 ? "text-win" : wr < 45 ? "text-loss" : "text-draw"}`}>{wr} %</td>
                        <td className="text-right tabular">{t.d}</td>
                        <td className="text-right tabular">{(t.goals ?? 0).toFixed(2)}</td>
                        <td className="text-right tabular">{t.possession == null ? "—" : `${t.possession.toFixed(0)} %`}</td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>

          <div className="grid gap-6 lg:grid-cols-3">
            <section className="card p-5">
              <SectionTitle>Schémas</SectionTitle>
              {schemes.length ? (
                <BarList items={schemes.map((s) => ({ key: s.scheme, label: SCHEMES[s.scheme] ?? s.scheme, n: s.n, w: s.w }))} />
              ) : (
                <Empty>Pas encore de tactique enregistrée.</Empty>
              )}
            </section>
            <section className="card p-5">
              <SectionTitle>Rôles</SectionTitle>
              {roles.length ? (
                <BarList unit="place" items={roles.map((r) => ({ key: r.role, label: ROLES[r.role] ?? r.role, n: r.n, w: r.w }))} />
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
              <SectionTitle sub="Pourcentage de victoires de l'équipe en ligne contre l'équipe en colonne. Survole une case pour le détail.">
                Équipe contre équipe
              </SectionTitle>
              <div className="overflow-x-auto">
                <table className="border-separate border-spacing-0.5 text-xs">
                  <thead>
                    <tr>
                      <th />
                      {animals.map((b) => (
                        <th key={b} className="p-0.5"><TeamLogo animal={b} size={26} className="mx-auto" /></th>
                      ))}
                    </tr>
                  </thead>
                  <tbody>
                    {animals.map((a) => (
                      <tr key={a}>
                        <th className="p-0.5 pr-1.5"><TeamLogo animal={a} size={26} /></th>
                        {animals.map((b) => {
                          const c = grid.get(`${a}-${b}`);
                          if (!c) return <td key={b} className="size-9 rounded-md bg-black/15 text-center text-ink-muted">·</td>;
                          const wr = pct(c.w, c.n);
                          return (
                            <td
                              key={b}
                              title={`${team(a).name} contre ${team(b).name} : ${c.w} victoire${c.w > 1 ? "s" : ""} sur ${c.n}`}
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
