"use client";

// Carte des tirs d'une équipe. Chaque issue (but, arrêté, contré, à côté) s'affiche ou se cache d'un clic sur la
// légende : avec beaucoup de tirs, les points se recouvrent.
import { useState } from "react";
import { PitchFrame } from "@/components/Pitch";
import { footballerName, SHOT_RESULTS } from "@/lib/game";
import type { EventRow } from "@/lib/queries";

const W = 105;
const H = 58;

export function ShotPitch({ shots, animal, id }: { shots: EventRow[]; animal: number | null; id: string }) {
  const [hidden, setHidden] = useState<ReadonlySet<string>>(new Set());
  const toggle = (k: string) =>
    setHidden((h) => {
      const next = new Set(h);
      if (next.has(k)) next.delete(k);
      else next.add(k);
      return next;
    });
  // les buts par-dessus les autres points
  const order = ["missed", "blocked", "saved", "goal"];
  const visible = shots
    .filter((s) => !hidden.has(s.result ?? "") && s.pos_depth != null && s.pos_lateral != null)
    .sort((a, b) => order.indexOf(a.result ?? "") - order.indexOf(b.result ?? ""));
  return (
    <div>
      <PitchFrame label="Carte des tirs" id={id}>
        <rect width={W} height={H} fill="rgba(0,0,0,0.2)" />
        {visible.map((s) => (
          <circle key={s.seq} cx={s.pos_depth! * W} cy={s.pos_lateral! * H} r={s.result === "goal" ? 2.3 : 1.8} fill={SHOT_RESULTS[s.result ?? ""]?.color ?? "#fff"} stroke="#0b1a0f" strokeWidth="0.4">
            <title>{`${s.minute}' ${footballerName(animal, s.number)} : ${SHOT_RESULTS[s.result ?? ""]?.label ?? s.result}`}</title>
          </circle>
        ))}
      </PitchFrame>
      <div className="mt-2 flex flex-wrap gap-1.5 text-xs" role="group" aria-label="Issues affichées">
        {Object.entries(SHOT_RESULTS).map(([k, v]) => {
          const on = !hidden.has(k);
          return (
            <button
              key={k}
              type="button"
              aria-pressed={on}
              onClick={() => toggle(k)}
              title={on ? `Cacher : ${v.label}` : `Afficher : ${v.label}`}
              className={`inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 font-bold transition ${
                on ? "border-line-strong bg-black/25 text-ink" : "border-line bg-transparent text-ink-muted line-through opacity-60"
              } hover:border-gold-300`}
            >
              <i className="inline-block size-2.5 rounded-full" style={{ background: on ? v.color : "transparent", boxShadow: `inset 0 0 0 1.5px ${v.color}` }} />
              {v.label} ({shots.filter((s) => s.result === k).length})
            </button>
          );
        })}
      </div>
    </div>
  );
}
