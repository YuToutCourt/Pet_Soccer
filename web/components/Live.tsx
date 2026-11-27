// Briques du direct (pages /direct) : pastille de minute, carte d'un match en cours, fil des actions.
import Link from "next/link";
import { TeamLogo } from "@/components/ui";
import { footballerName, KINDS, SHOT_RESULTS, SUPERS, team } from "@/lib/game";
import type { LiveRow } from "@/lib/queries";

/** « 62' », « But en or 93' », « Terminé »... */
export function liveClock(m: LiveRow): string {
  if (m.ended) return m.outcome === "void" ? "Annulé" : m.outcome === "forfeit" ? "Forfait" : "Terminé";
  if (m.minute == null) return "En cours";
  if (m.half === 2) return `But en or · ${m.minute}'`;
  return `${m.minute}'`;
}

export function LiveBadge({ m, big = false }: { m: LiveRow; big?: boolean }) {
  const size = big ? "px-3 py-1 text-sm" : "px-2 py-0.5 text-[0.7rem]";
  if (m.ended) {
    return <span className={`inline-flex items-center gap-1.5 rounded-full border border-line bg-black/30 font-bold uppercase tracking-wider text-ink-dim ${size}`}>{liveClock(m)}</span>;
  }
  return (
    <span className={`inline-flex items-center gap-1.5 rounded-full border border-loss/50 bg-loss/15 font-bold uppercase tracking-wider text-loss ${size}`}>
      <span className="anim-pulse size-2 rounded-full bg-loss shadow-[0_0_8px_var(--loss)]" aria-hidden />
      <span>Direct</span>
      <span className="font-display normal-case tracking-normal text-ink tabular">{liveClock(m)}</span>
    </span>
  );
}

export interface LiveAction {
  id: string;
  minute: number;
  side: number; // 0 = hôte
  kind: "goal" | "own_goal" | "super" | "shot";
  who: string | null;
  result?: string; // tir : saved | blocked | missed
  superType?: number;
}

/** Fil du match en direct, le plus récent d'abord. Les tirs gagnants sont déjà les buts. */
export function liveActions(m: LiveRow): LiveAction[] {
  const animals = [m.host_animal, m.guest_animal];
  const out: LiveAction[] = [];
  m.stats?.teams.forEach((t, side) => {
    t.goals.forEach((g, i) => {
      // csc : compté pour l'équipe qui en profite, le joueur est de l'autre
      const who = g.number >= 0 ? footballerName(animals[g.own_goal ? 1 - side : side], g.number) : null;
      out.push({ id: `g${side}-${i}`, minute: g.minute, side, kind: g.own_goal ? "own_goal" : "goal", who });
    });
    t.super_events.forEach((s, i) => out.push({ id: `s${side}-${i}`, minute: s.minute, side, kind: "super", who: null, superType: s.type }));
    t.shot_events.forEach((s, i) => {
      if (s.result === "goal") return;
      out.push({ id: `t${side}-${i}`, minute: s.minute, side, kind: "shot", who: s.number >= 0 ? footballerName(animals[side], s.number) : null, result: s.result });
    });
  });
  // à minute égale : but, puis super, puis tir (l'ordre exact n'est pas connu)
  const weight = { goal: 0, own_goal: 0, super: 1, shot: 2 };
  return out.sort((a, b) => b.minute - a.minute || weight[a.kind] - weight[b.kind]);
}

export function actionText(a: LiveAction, teamName: string): string {
  switch (a.kind) {
    case "goal":
      return a.who ? `But de ${a.who}` : `But de ${teamName}`;
    case "own_goal":
      return a.who ? `But contre son camp de ${a.who}` : "But contre son camp";
    case "super":
      return `Super ${SUPERS[a.superType ?? -1] ?? ""}`.trim();
    default: {
      const res = a.result === "saved" ? "arrêté" : a.result === "blocked" ? "contré" : "à côté";
      return a.who ? `Tir de ${a.who}, ${res}` : `Tir ${res}`;
    }
  }
}

export function actionColor(a: LiveAction): string {
  if (a.kind === "goal" || a.kind === "own_goal") return SHOT_RESULTS.goal.color;
  if (a.kind === "super") return "#c9a6ff";
  return SHOT_RESULTS[a.result ?? ""]?.color ?? "#fff";
}

/** Une ligne de la liste des matchs en direct. */
export function LiveCard({ m }: { m: LiveRow }) {
  const last = liveActions(m).find((a) => a.kind === "goal" || a.kind === "own_goal");
  const hostWon = m.ended && m.score_host > m.score_guest;
  const guestWon = m.ended && m.score_guest > m.score_host;
  return (
    <div className={`group relative grid grid-cols-[1fr_auto_1fr] items-center gap-3 rounded-2xl border px-3 py-3.5 transition sm:px-5 ${m.ended ? "border-line bg-black/10 opacity-80 hover:opacity-100" : "border-loss/30 bg-black/20 hover:border-loss/60"}`}>
      <div className="flex min-w-0 items-center justify-end gap-2.5 text-right">
        <div className="min-w-0">
          <div className="truncate font-bold">{m.host_name}</div>
          <div className="hidden truncate text-xs text-ink-muted sm:block">{team(m.host_animal).name}</div>
        </div>
        <TeamLogo animal={m.host_animal} size={40} className="shrink-0" />
      </div>
      <Link href={`/direct/${encodeURIComponent(m.key)}`} className="flex flex-col items-center gap-1 px-1 after:absolute after:inset-0 after:content-['']" aria-label={`Direct ${m.host_name} contre ${m.guest_name}`}>
        <span className="font-display text-3xl tabular text-ball">
          <span className={hostWon ? "text-gold-300" : ""}>{m.score_host}</span>
          <span className="mx-1.5 text-ink-muted">–</span>
          <span className={guestWon ? "text-gold-300" : ""}>{m.score_guest}</span>
        </span>
        <LiveBadge m={m} />
      </Link>
      <div className="flex min-w-0 items-center gap-2.5">
        <TeamLogo animal={m.guest_animal} size={40} className="shrink-0" />
        <div className="min-w-0">
          <div className="truncate font-bold">{m.guest_name}</div>
          <div className="hidden truncate text-xs text-ink-muted sm:block">{team(m.guest_animal).name}</div>
        </div>
      </div>
      <div className="col-span-3 flex flex-wrap justify-center gap-x-2 text-xs text-ink-muted">
        <span>{KINDS[m.kind] ?? m.kind}</span>
        {last && <span>· dernier but : {last.who ?? "?"} ({last.minute}&apos;)</span>}
        {m.spectators > 0 && <span>· {m.spectators} spectateur{m.spectators > 1 ? "s" : ""} en jeu</span>}
      </div>
    </div>
  );
}
