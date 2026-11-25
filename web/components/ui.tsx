// Petites briques d'interface partagées (composants serveur).
import Link from "next/link";
import type { ReactNode } from "react";
import { fmtAgo, fmtDelta, KINDS, rankOf, team } from "@/lib/game";
import type { MatchRow } from "@/lib/queries";

export function TeamLogo({ animal, size = 24, className = "" }: { animal: number | null; size?: number; className?: string }) {
  const t = team(animal);
  if (!t.logo) {
    return (
      <span className={`inline-grid place-items-center ${className}`} style={{ width: size, height: size, fontSize: size * 0.7 }}>
        {t.emoji}
      </span>
    );
  }
  // eslint-disable-next-line @next/next/no-img-element
  return <img src={`/img/logos/${t.logo}`} alt={t.name} title={t.name} width={size} height={size} loading="lazy" className={`object-contain drop-shadow ${className}`} style={{ width: size, height: size }} />;
}

export function TeamTag({ animal }: { animal: number | null }) {
  if (animal == null) return <span className="text-ink-muted">—</span>;
  return (
    <span className="inline-flex items-center gap-2 whitespace-nowrap text-sm text-ink-dim">
      <TeamLogo animal={animal} size={22} />
      <span>{team(animal).name}</span>
    </span>
  );
}

export function RankBadge({ elo, size = 32, withName = false }: { elo: number; size?: number; withName?: boolean }) {
  const r = rankOf(elo);
  return (
    <span className="inline-flex items-center gap-2" title={`${r.name} (${elo} Elo)`}>
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={`/img/ranks/${r.badge}`} alt={r.name} width={size} height={size} className="rounded-full drop-shadow-[0_2px_4px_rgba(0,0,0,0.6)]" style={{ width: size, height: size }} />
      {withName && (
        <span className="chip" style={{ color: r.color, background: `${r.color}1f`, border: `1px solid ${r.color}55` }}>
          {r.name}
        </span>
      )}
    </span>
  );
}

export function WLBar({ w, d, l }: { w: number; d: number; l: number }) {
  const total = w + d + l;
  return (
    <div className="min-w-28" role="img" aria-label={`${w} victoires, ${d} nuls, ${l} défaites`}>
      <div className="flex h-1.5 gap-0.5 overflow-hidden rounded-full bg-black/30">
        {total > 0 && (
          <>
            <span className="rounded-full bg-win" style={{ flex: w }} />
            <span className="rounded-full bg-draw" style={{ flex: d }} />
            <span className="rounded-full bg-loss" style={{ flex: l }} />
          </>
        )}
      </div>
      <div className="mt-1 flex gap-2.5 text-[0.72rem] font-bold tabular">
        <span className="text-win">{w} V</span>
        <span className="text-draw">{d} N</span>
        <span className="text-loss">{l} D</span>
      </div>
    </div>
  );
}

export function Delta({ value, suffix = "" }: { value: number | null | undefined; suffix?: string }) {
  const v = value ?? 0;
  const cls = v > 0 ? "text-win" : v < 0 ? "text-loss" : "text-ink-muted";
  return <span className={`font-extrabold tabular ${cls}`}>{fmtDelta(v)}{suffix}</span>;
}

const RESULT = {
  W: { label: "V", cls: "bg-win/15 text-win border-win/40", full: "Victoire" },
  D: { label: "N", cls: "bg-draw/15 text-draw border-draw/40", full: "Nul" },
  L: { label: "D", cls: "bg-loss/15 text-loss border-loss/40", full: "Défaite" },
} as const;

export function ResultTag({ result, size = 34 }: { result: string; size?: number }) {
  const r = RESULT[result as keyof typeof RESULT] ?? { label: "?", cls: "bg-black/20 text-ink-muted border-line", full: "?" };
  return (
    <span title={r.full} className={`grid shrink-0 place-items-center rounded-xl border font-display text-sm ${r.cls}`} style={{ width: size, height: size }}>
      {r.label}
    </span>
  );
}

export function StatTile({ label, value, tone, hint }: { label: string; value: ReactNode; tone?: "win" | "draw" | "loss" | "gold"; hint?: ReactNode }) {
  const color = tone === "win" ? "text-win" : tone === "draw" ? "text-draw" : tone === "loss" ? "text-loss" : tone === "gold" ? "text-gold-300" : "text-ink";
  return (
    <div className="rounded-2xl border border-line bg-black/20 px-4 py-3.5">
      <div className="label">{label}</div>
      <div className={`mt-1 font-display text-3xl leading-tight tabular ${color}`}>{value}</div>
      {hint && <div className="mt-0.5 text-xs text-ink-muted">{hint}</div>}
    </div>
  );
}

export function SectionTitle({ children, sub, right }: { children: ReactNode; sub?: ReactNode; right?: ReactNode }) {
  return (
    <div className="mb-4 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h2 className="title-game text-2xl sm:text-[1.7rem]">{children}</h2>
        {sub && <p className="mt-0.5 text-sm text-ink-dim">{sub}</p>}
      </div>
      {right}
    </div>
  );
}

export function Empty({ children }: { children: ReactNode }) {
  return <div className="px-4 py-12 text-center text-ink-muted">{children}</div>;
}

export function Skeleton({ className = "" }: { className?: string }) {
  return <div className={`animate-pulse rounded-2xl bg-white/5 ${className}`} />;
}

export function PageSkeleton() {
  return (
    <div className="space-y-5">
      <Skeleton className="h-28" />
      <Skeleton className="h-72" />
      <Skeleton className="h-48" />
    </div>
  );
}

export function KindTabs({ base, kind, extra = {} }: { base: string; kind: string | null; extra?: Record<string, string> }) {
  const items: [string | null, string][] = [[null, "Tous"], ...Object.entries(KINDS)];
  return (
    <nav className="flex flex-wrap gap-2" aria-label="Type de match">
      {items.map(([k, label]) => {
        const params = new URLSearchParams(extra);
        if (k) params.set("kind", k);
        const qs = params.toString();
        const on = k === kind;
        return (
          <Link
            key={label}
            href={qs ? `${base}?${qs}` : base}
            aria-current={on ? "page" : undefined}
            className={`rounded-full border px-3.5 py-1.5 text-sm font-bold transition ${on ? "border-gold-300 bg-gold-300/15 text-gold-100" : "border-line text-ink-dim hover:border-line-strong hover:text-ink"}`}
          >
            {label}
          </Link>
        );
      })}
    </nav>
  );
}

export function Pagination({ page, pages, href }: { page: number; pages: number; href: (p: number) => string }) {
  if (pages <= 1) return null;
  const nums = new Set<number>([1, pages]);
  for (let p = page - 2; p <= page + 2; p++) if (p >= 1 && p <= pages) nums.add(p);
  const list = [...nums].sort((a, b) => a - b);
  const cls = "grid h-10 min-w-10 place-items-center rounded-full border px-3 text-sm font-bold tabular";
  return (
    <nav className="mt-6 flex flex-wrap items-center justify-center gap-2" aria-label="Pagination">
      {page > 1 && <Link className={`${cls} border-line text-ink-dim hover:text-ink`} href={href(page - 1)} rel="prev">‹</Link>}
      {list.map((p, i) => (
        <span key={p} className="contents">
          {i > 0 && list[i - 1] < p - 1 && <span className="text-ink-muted">…</span>}
          {p === page ? (
            <span className={`${cls} btn-pill border-transparent`} aria-current="page">{p}</span>
          ) : (
            <Link className={`${cls} border-line text-ink-dim hover:border-line-strong hover:text-ink`} href={href(p)}>{p}</Link>
          )}
        </span>
      ))}
      {page < pages && <Link className={`${cls} border-line text-ink-dim hover:text-ink`} href={href(page + 1)} rel="next">›</Link>}
    </nav>
  );
}

function PlayerName({ id, name }: { id: number | null; name: string }) {
  return id ? (
    <Link href={`/player/${id}`} className="font-bold text-ink hover:text-gold-300">{name}</Link>
  ) : (
    <span className="font-bold text-ink">{name}</span>
  );
}

/** Une ligne de match : hôte à gauche, score au centre, invité à droite. */
export function MatchLine({ m, now }: { m: MatchRow; now: number }) {
  const hostWon = m.score_host > m.score_guest;
  const guestWon = m.score_guest > m.score_host;
  return (
    <div className="group relative grid grid-cols-[1fr_auto_1fr] items-center gap-3 rounded-2xl border border-line bg-black/15 px-3 py-3 transition hover:border-line-strong hover:bg-black/25 sm:px-4">
      <div className="flex min-w-0 items-center justify-end gap-2.5 text-right">
        <div className="min-w-0">
          <div className={`relative z-10 truncate ${hostWon ? "" : "opacity-80"}`}><PlayerName id={m.host_id} name={m.host_name} /></div>
          {m.rated && <div className="text-xs"><Delta value={m.delta_host} suffix=" Elo" /></div>}
        </div>
        <TeamLogo animal={m.team_host} size={36} className="shrink-0" />
      </div>
      <Link href={`/match/${m.id}`} className="flex flex-col items-center px-1 after:absolute after:inset-0 after:content-['']" aria-label={`Match ${m.host_name} contre ${m.guest_name}`}>
        <span className="font-display text-2xl tabular text-ball">
          <span className={hostWon ? "text-gold-300" : ""}>{m.score_host}</span>
          <span className="mx-1 text-ink-muted">–</span>
          <span className={guestWon ? "text-gold-300" : ""}>{m.score_guest}</span>
        </span>
        <span className="flex items-center gap-1.5 text-[0.68rem] font-bold uppercase tracking-wider text-ink-muted">
          {m.forfeit ? <span className="text-loss">Forfait</span> : KINDS[m.kind] ?? m.kind}
          <span aria-hidden>·</span>
          <span className="normal-case tracking-normal">{fmtAgo(m.played, now)}</span>
        </span>
      </Link>
      <div className="flex min-w-0 items-center gap-2.5">
        <TeamLogo animal={m.team_guest} size={36} className="shrink-0" />
        <div className="min-w-0">
          <div className={`relative z-10 truncate ${guestWon ? "" : "opacity-80"}`}><PlayerName id={m.guest_id} name={m.guest_name} /></div>
          {m.rated && <div className="text-xs"><Delta value={m.delta_guest} suffix=" Elo" /></div>}
        </div>
      </div>
    </div>
  );
}
