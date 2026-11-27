import type { Metadata } from "next";
import Image from "next/image";
import Link from "next/link";
import { connection } from "next/server";
import { Suspense } from "react";
import AutoRefresh from "@/components/AutoRefresh";
import Comparison from "@/components/Comparison";
import { actionColor, actionText, liveActions, LiveBadge } from "@/components/Live";
import { Empty, PageSkeleton, RankBadge, SectionTitle, TeamLogo } from "@/components/ui";
import { fmtAgo, KINDS, nowSec, team } from "@/lib/game";
import { getLiveMatch } from "@/lib/queries";

export const metadata: Metadata = { title: "Match en direct" };

export default function Page({ params }: PageProps<"/direct/[key]">) {
  return (
    <Suspense fallback={<PageSkeleton />}>
      <LiveDetail params={params} />
    </Suspense>
  );
}

async function LiveDetail({ params }: { params: PageProps<"/direct/[key]">["params"] }) {
  await connection();
  const { key } = await params;
  const m = await getLiveMatch(decodeURIComponent(key));
  if (!m) {
    return (
      <div className="card px-6 py-12 text-center">
        <h1 className="title-game text-3xl">Ce direct est fini</h1>
        <p className="mt-2 text-ink-dim">Le match n&apos;est plus en cours. Retrouve-le dans la liste des matchs joués.</p>
        <div className="mt-5 flex justify-center gap-3">
          <Link href="/matches" className="btn-pill h-10 px-5 text-base">Matchs</Link>
          <Link href="/direct" className="inline-flex h-10 items-center rounded-full border border-line px-5 font-bold text-ink-dim hover:text-ink">Autres directs</Link>
        </div>
      </div>
    );
  }
  const now = nowSec();
  const actions = liveActions(m);
  const names = [m.host_name, m.guest_name];
  const host = m.stats?.teams[0];
  const guest = m.stats?.teams[1];
  // fini et enregistré (ou annulé) : plus rien ne changera
  const done = m.ended && (m.match_id != null || m.outcome === "void");

  return (
    <div className="space-y-6">
      <div className="card relative overflow-hidden px-4 py-7 sm:px-8">
        <div className="pointer-events-none absolute inset-0 opacity-40" style={{ background: `radial-gradient(circle at 15% 50%, ${team(m.host_animal).color}33, transparent 45%), radial-gradient(circle at 85% 50%, ${team(m.guest_animal).color}33, transparent 45%)` }} />
        <div className="relative grid grid-cols-[1fr_auto_1fr] items-center gap-3 text-center sm:gap-6">
          <Side id={m.host_id} name={m.host_name} animal={m.host_animal} elo={m.host_elo} />
          <div className="flex flex-col items-center gap-3">
            <div className="font-display text-5xl tabular sm:text-7xl" aria-live="polite" aria-label={`Score : ${m.score_host} à ${m.score_guest}`}>
              <span className="text-ball">{m.score_host}</span>
              <span className="mx-2 text-ink-muted">–</span>
              <span className="text-ball">{m.score_guest}</span>
            </div>
            <LiveBadge m={m} big />
          </div>
          <Side id={m.guest_id} name={m.guest_name} animal={m.guest_animal} elo={m.guest_elo} />
        </div>
        <div className="relative mt-5 flex flex-wrap justify-center gap-x-3 gap-y-1 text-sm text-ink-dim">
          <span className="chip border border-line-strong text-gold-100">{KINDS[m.kind] ?? m.kind}</span>
          {m.started && <span>commencé {fmtAgo(m.started, now)}</span>}
          {m.half_seconds != null && <span>· mi-temps de {Math.round(m.half_seconds / 60)} min</span>}
          {m.stadium != null && <span>· stade {m.stadium}</span>}
          {m.spectators > 0 && <span>· {m.spectators} spectateur{m.spectators > 1 ? "s" : ""} dans le jeu</span>}
        </div>
      </div>

      {m.ended && (
        <div className="card flex flex-wrap items-center justify-between gap-3 px-5 py-4">
          <p className="font-bold">
            {m.outcome === "void" ? "Match annulé : il n'est pas compté." : m.outcome === "forfeit" ? "Match fini par forfait." : "Match terminé."}
          </p>
          {m.match_id != null ? (
            <Link href={`/match/${m.match_id}`} className="btn-pill h-10 px-5 text-base">Voir la fiche du match</Link>
          ) : (
            m.outcome !== "void" && <span className="text-sm text-ink-muted">Enregistrement du match…</span>
          )}
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-[1fr_1.15fr]">
        <div className="card p-5">
          <SectionTitle sub={m.ended ? undefined : "Le plus récent en haut."}>Le fil du match</SectionTitle>
          {m.stats == null ? (
            <Empty>Le jeu de l&apos;hôte n&apos;envoie que le score : il lui faut la dernière version du mod pour le fil du match.</Empty>
          ) : actions.length === 0 ? (
            <Empty>Pas encore d&apos;action.</Empty>
          ) : (
            <ol className="space-y-1.5">
              {actions.map((a) => {
                const goal = a.kind === "goal" || a.kind === "own_goal";
                return (
                  <li key={a.id} className={`flex items-center gap-3 rounded-xl px-2.5 py-1.5 ${goal ? "bg-white/[0.06]" : ""}`}>
                    <span className={`grid h-7 min-w-11 place-items-center rounded-full px-1.5 font-display tabular ${goal ? "btn-pill text-sm" : "border border-line bg-grass-900 text-xs text-ink-dim"}`}>
                      {a.minute}&apos;
                    </span>
                    <TeamLogo animal={a.side === 0 ? m.host_animal : m.guest_animal} size={22} className="shrink-0" />
                    <span className="inline-block size-2.5 shrink-0 rounded-full" style={{ background: actionColor(a) }} aria-hidden />
                    <span className={`min-w-0 flex-1 text-sm ${goal ? "font-extrabold text-ink" : "text-ink-dim"}`}>
                      {actionText(a, names[a.side])}
                      <span className="ml-1.5 text-xs font-normal text-ink-muted">{names[a.side]}</span>
                    </span>
                    {a.kind === "super" && a.superType != null && (
                      <Image src={`/img/supers/super_${a.superType}.png`} alt="" width={48} height={19} className="shrink-0" unoptimized />
                    )}
                  </li>
                );
              })}
            </ol>
          )}
        </div>
        {host && guest ? (
          <Comparison title="Statistiques en direct" host={{ ...host, name: m.host_name }} guest={{ ...guest, name: m.guest_name }} />
        ) : (
          <div className="card p-5">
            <SectionTitle>Statistiques en direct</SectionTitle>
            <Empty>Pas encore de statistiques pour ce match.</Empty>
          </div>
        )}
      </div>
      {!done && <AutoRefresh every={4000} />}
    </div>
  );
}

function Side({ id, name, animal, elo }: { id: number | null; name: string; animal: number | null; elo: number | null }) {
  return (
    <div className="flex min-w-0 flex-col items-center gap-1.5">
      <TeamLogo animal={animal} size={84} className="drop-shadow-[0_6px_12px_rgba(0,0,0,0.5)]" />
      {id ? (
        <Link href={`/player/${id}`} className="max-w-full truncate font-display text-xl hover:text-gold-300 sm:text-2xl">{name}</Link>
      ) : (
        <span className="max-w-full truncate font-display text-xl sm:text-2xl">{name}</span>
      )}
      <span className="hidden text-sm text-ink-dim sm:block">{team(animal).name}</span>
      {elo != null && (
        <span className="flex items-center gap-1.5 text-sm text-ink-dim">
          <RankBadge elo={elo} size={20} /> {elo} Elo
        </span>
      )}
    </div>
  );
}
