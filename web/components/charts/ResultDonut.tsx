"use client";

import { Cell, Pie, PieChart, Tooltip } from "recharts";

/** Anneau victoires / nuls / défaites, taux de victoire au centre. */
export default function ResultDonut({ w, d, l }: { w: number; d: number; l: number }) {
  const total = w + d + l;
  const data = [
    { name: "Victoires", value: w, color: "var(--win)" },
    { name: "Nuls", value: d, color: "var(--draw)" },
    { name: "Défaites", value: l, color: "var(--loss)" },
  ].filter((x) => x.value > 0);
  return (
    <div className="flex items-center gap-5">
      <div className="relative size-36 shrink-0">
          <PieChart responsive style={{ width: "100%", height: "100%" }}>
            <Pie data={data} dataKey="value" innerRadius="70%" outerRadius="100%" paddingAngle={data.length > 1 ? 3 : 0} stroke="none" startAngle={90} endAngle={-270} cornerRadius={4}>
              {data.map((x) => (
                <Cell key={x.name} fill={x.color} />
              ))}
            </Pie>
            <Tooltip
              formatter={(v, name) => [`${v} (${total ? Math.round((Number(v) / total) * 100) : 0} %)`, name]}
              contentStyle={{ background: "var(--card-strong)", border: "1px solid var(--line-strong)", borderRadius: 12, color: "var(--ink)" }}
              itemStyle={{ color: "var(--ink)" }}
            />
          </PieChart>
        <div className="pointer-events-none absolute inset-0 grid place-items-center text-center">
          <div>
            <div className="font-display text-3xl tabular">{total ? Math.round((w / total) * 100) : 0} %</div>
            <div className="label">victoires</div>
          </div>
        </div>
      </div>
      <ul className="space-y-1.5 text-sm">
        <li className="flex items-center gap-2"><i className="size-2.5 rounded-full bg-win" /><span className="w-16 text-ink-dim">Victoires</span><b className="tabular">{w}</b></li>
        <li className="flex items-center gap-2"><i className="size-2.5 rounded-full bg-draw" /><span className="w-16 text-ink-dim">Nuls</span><b className="tabular">{d}</b></li>
        <li className="flex items-center gap-2"><i className="size-2.5 rounded-full bg-loss" /><span className="w-16 text-ink-dim">Défaites</span><b className="tabular">{l}</b></li>
      </ul>
    </div>
  );
}
