"use client";

import { CartesianGrid, ReferenceLine, Scatter, ScatterChart, Tooltip, XAxis, YAxis, ZAxis } from "recharts";

export interface TeamPoint {
  animal: number;
  name: string;
  logo: string;
  pick: number; // % des choix
  win: number; // % de victoires
  n: number;
}

function TeamDot(props: { cx?: number; cy?: number; payload?: TeamPoint }) {
  const { cx = 0, cy = 0, payload } = props;
  if (!payload) return null;
  const s = 30;
  return (
    <g>
      <circle cx={cx} cy={cy} r={s / 2 + 2} fill="rgba(3,22,9,0.85)" stroke="rgba(255,244,160,0.35)" strokeWidth={1.5} />
      {payload.logo ? (
        <image href={`/img/logos/${payload.logo}`} x={cx - s / 2 + 2} y={cy - s / 2 + 2} width={s - 4} height={s - 4} />
      ) : (
        <circle cx={cx} cy={cy} r={5} fill="#ffd84a" />
      )}
    </g>
  );
}

function TeamTip({ active, payload }: { active?: boolean; payload?: { payload: TeamPoint }[] }) {
  if (!active || !payload?.length) return null;
  const t = payload[0].payload;
  return (
    <div className="rounded-xl border border-line-strong bg-card-strong px-3 py-2 text-sm shadow-xl">
      <div className="font-display text-base text-gold-100">{t.name}</div>
      <div className="text-ink-dim">Choisie dans <b className="text-ink">{t.pick} %</b> des cas ({t.n})</div>
      <div className="text-ink-dim">Gagne <b className="text-ink">{t.win} %</b> de ses matchs</div>
    </div>
  );
}

/** Méta des équipes : popularité (x) contre efficacité (y), une pastille par logo. */
export default function TeamScatter({ data }: { data: TeamPoint[] }) {
  const maxPick = Math.max(10, ...data.map((d) => d.pick));
  return (
    <div className="h-80 w-full sm:h-96">
        <ScatterChart responsive style={{ width: "100%", height: "100%" }} margin={{ top: 20, right: 24, bottom: 28, left: 0 }}>
          <CartesianGrid stroke="rgba(255,255,255,0.06)" />
          <XAxis
            type="number"
            dataKey="pick"
            domain={[0, Math.ceil((maxPick + 2) / 5) * 5]}
            unit=" %"
            tick={{ fill: "var(--ink-muted)", fontSize: 11 }}
            axisLine={false}
            tickLine={false}
            label={{ value: "Taux de choix", position: "insideBottom", offset: -16, fill: "var(--ink-dim)", fontSize: 12, fontWeight: 700 }}
          />
          <YAxis
            type="number"
            dataKey="win"
            domain={[0, 100]}
            unit=" %"
            tick={{ fill: "var(--ink-muted)", fontSize: 11 }}
            axisLine={false}
            tickLine={false}
            width={52}
            label={{ value: "Victoires", angle: -90, position: "insideLeft", offset: 14, fill: "var(--ink-dim)", fontSize: 12, fontWeight: 700 }}
          />
          <ZAxis range={[400, 400]} />
          <ReferenceLine y={50} stroke="rgba(255,244,160,0.35)" strokeDasharray="4 4" />
          <Tooltip content={<TeamTip />} cursor={false} />
          <Scatter data={data} shape={<TeamDot />} isAnimationActive={false} />
        </ScatterChart>
    </div>
  );
}
