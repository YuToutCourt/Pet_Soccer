"use client";

import { Area, AreaChart, CartesianGrid, ReferenceArea, Tooltip, XAxis, YAxis } from "recharts";
import { fmtDay, RANKS } from "@/lib/game";

export interface EloPoint {
  i: number;
  elo: number;
  played: number;
  id: number | null;
  result: string | null;
  opp: string | null;
}

const RESULT_LABEL: Record<string, string> = { W: "Victoire", D: "Nul", L: "Défaite" };
const RESULT_COLOR: Record<string, string> = { W: "var(--win)", D: "var(--draw)", L: "var(--loss)" };

function EloTooltip({ active, payload }: { active?: boolean; payload?: { payload: EloPoint }[] }) {
  if (!active || !payload?.length) return null;
  const p = payload[0].payload;
  return (
    <div className="rounded-xl border border-line-strong bg-card-strong px-3 py-2 text-sm shadow-xl">
      <div className="font-display text-lg tabular text-gold-100">{p.elo} Elo</div>
      {p.result ? (
        <div className="text-ink-dim">
          <span style={{ color: RESULT_COLOR[p.result] }} className="font-bold">{RESULT_LABEL[p.result] ?? p.result}</span>
          {p.opp && <> contre <span className="font-bold text-ink">{p.opp}</span></>}
        </div>
      ) : (
        <div className="text-ink-dim">Départ</div>
      )}
      <div className="text-xs text-ink-muted">{fmtDay(p.played)}</div>
    </div>
  );
}

/** Courbe de l'Elo match après match, sur fond des paliers de rang. */
export default function EloChart({ data }: { data: EloPoint[] }) {
  const values = data.map((d) => d.elo);
  const lo = Math.floor((Math.min(...values) - 40) / 50) * 50;
  const hi = Math.ceil((Math.max(...values) + 40) / 50) * 50;
  // paliers de rang visibles dans la fenêtre
  const bands = RANKS.map((r, idx) => ({ ...r, top: idx === 0 ? Infinity : RANKS[idx - 1].min }))
    .filter((r) => r.top > lo && r.min < hi)
    .map((r) => ({ ...r, y1: Math.max(r.min, lo), y2: Math.min(r.top, hi) }));

  return (
    <div className="h-64 w-full sm:h-72">
        <AreaChart responsive style={{ width: "100%", height: "100%" }} data={data} margin={{ top: 10, right: 8, bottom: 0, left: -8 }}>
          <defs>
            <linearGradient id="elo-fill" x1="0" y1="0" x2="0" y2="1">
              <stop offset="0%" stopColor="#ffd84a" stopOpacity={0.45} />
              <stop offset="100%" stopColor="#ffd84a" stopOpacity={0} />
            </linearGradient>
          </defs>
          {bands.map((b) => (
            <ReferenceArea
              key={b.name}
              y1={b.y1}
              y2={b.y2}
              fill={b.color}
              fillOpacity={0.07}
              strokeOpacity={0}
              label={{ value: b.name, position: "insideTopRight", fill: b.color, fontSize: 11, fontWeight: 800, opacity: 0.8 }}
            />
          ))}
          <CartesianGrid vertical={false} stroke="rgba(255,255,255,0.06)" />
          <XAxis dataKey="i" tick={false} axisLine={false} tickLine={false} height={6} />
          <YAxis domain={[lo, hi]} tick={{ fill: "var(--ink-muted)", fontSize: 11 }} axisLine={false} tickLine={false} width={44} tickCount={6} allowDecimals={false} />
          <Tooltip content={<EloTooltip />} cursor={{ stroke: "rgba(255,244,160,0.4)", strokeWidth: 1 }} />
          <Area
            type="monotone"
            dataKey="elo"
            stroke="#ffd84a"
            strokeWidth={2}
            fill="url(#elo-fill)"
            dot={false}
            activeDot={{ r: 5, fill: "#ffd84a", stroke: "#062611", strokeWidth: 2 }}
            isAnimationActive
          />
        </AreaChart>
    </div>
  );
}
