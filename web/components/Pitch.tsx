// Terrains SVG du détail de match. Terrain vu par l'équipe : son but à gauche, elle attaque vers la droite.
// Repère du jeu : profondeur 0..1 (0 = son but), latéral 0..1 (0 = haut de l'écran).
import type { ReactNode } from "react";
import { footballerName, SHOT_RESULTS } from "@/lib/game";
import type { EventRow } from "@/lib/queries";

const W = 105;
const H = 68;
const clamp = (v: number, a: number, b: number) => Math.min(Math.max(v, a), b);

function PitchFrame({ children, label, id }: { children?: ReactNode; label: string; id: string }) {
  return (
    <svg viewBox={`0 0 ${W} ${H}`} role="img" aria-label={label} className="block h-auto w-full rounded-xl shadow-[inset_0_0_0_1px_rgba(255,255,255,0.06)]">
      <defs>
        <pattern id={`${id}-mow`} width="15" height={H} patternUnits="userSpaceOnUse">
          <rect width="7.5" height={H} fill="#17602c" />
          <rect x="7.5" width="7.5" height={H} fill="#145726" />
        </pattern>
        <clipPath id={`${id}-clip`}>
          <rect width={W} height={H} rx="2" />
        </clipPath>
        <filter id={`${id}-blur`} x="-10%" y="-10%" width="120%" height="120%">
          <feGaussianBlur stdDeviation="2.2" />
        </filter>
      </defs>
      <g clipPath={`url(#${id}-clip)`}>
        <rect width={W} height={H} fill={`url(#${id}-mow)`} />
        {children}
      </g>
      <g fill="none" stroke="rgba(255,255,255,.55)" strokeWidth="0.4">
        <rect x="0.5" y="0.5" width={W - 1} height={H - 1} rx="1" />
        <line x1={W / 2} y1="0.5" x2={W / 2} y2={H - 0.5} />
        <circle cx={W / 2} cy={H / 2} r="9.15" />
        <circle cx={W / 2} cy={H / 2} r="0.5" fill="rgba(255,255,255,.55)" />
        <rect x="0.5" y={H / 2 - 20} width="16.5" height="40" />
        <rect x="0.5" y={H / 2 - 9} width="5.5" height="18" />
        <rect x={W - 17} y={H / 2 - 20} width="16.5" height="40" />
        <rect x={W - 6} y={H / 2 - 9} width="5.5" height="18" />
      </g>
    </svg>
  );
}

export function HeatPitch({ grid, color, label, id }: { grid: number[][]; color: string; label: string; id: string }) {
  const nd = grid.length;
  const nl = grid[0]?.length ?? 0;
  return (
    <PitchFrame label={label} id={id}>
      <rect width={W} height={H} fill="rgba(0,0,0,0.35)" />
      <g filter={`url(#${id}-blur)`}>
        {grid.flatMap((row, d) =>
          row.map((v, l) =>
            v > 0 ? (
              <rect key={`${d}-${l}`} x={(d * W) / nd} y={(l * H) / nl} width={W / nd + 0.3} height={H / nl + 0.3} fill={color} fillOpacity={Math.min(0.92, (v / 255) * 0.95)} />
            ) : null,
          ),
        )}
      </g>
    </PitchFrame>
  );
}

export interface TacticPlayer {
  number: number;
  slot: number | null;
  name: string;
  avg: [number, number] | null;
  planned: [number, number] | null;
}

export function TacticPitch({ players, color, id }: { players: TacticPlayer[]; color: string; id: string }) {
  return (
    <PitchFrame label="Placement prévu et position moyenne réelle" id={id}>
      {players.map((f) => {
        if (!f.avg) return null;
        const ax = clamp(f.avg[0] * W, 3, W - 3);
        const ay = clamp(f.avg[1] * H, 3, H - 6);
        const px = f.planned ? f.planned[0] * W : null;
        const py = f.planned ? f.planned[1] * H : null;
        const label = f.slot != null && f.slot >= 0 ? f.slot + 2 : f.number + 1;
        return (
          <g key={f.number}>
            <title>{`${f.name}${f.planned ? " : écart au placement prévu" : ""}`}</title>
            {px != null && py != null && (
              <>
                <line x1={px} y1={py} x2={ax} y2={ay} stroke="rgba(255,255,255,.45)" strokeWidth="0.4" strokeDasharray="1 1" />
                <circle cx={px} cy={py} r="4" fill="rgba(255,255,255,.08)" stroke="#fff" strokeWidth="0.6" strokeDasharray="1.4 0.9" />
              </>
            )}
            <circle cx={ax} cy={ay} r="2.9" fill={color} stroke="#0b1a0f" strokeWidth="0.5" />
            <text x={ax} y={ay + 1.1} fontSize="3" textAnchor="middle" fill="#0b1a0f" fontWeight="800">{label}</text>
            <text x={ax} y={ay + 6} fontSize="2.5" textAnchor="middle" fill="#fff" fontWeight="700" stroke="rgba(0,0,0,.6)" strokeWidth="0.5" paintOrder="stroke">
              {f.name}
            </text>
          </g>
        );
      })}
    </PitchFrame>
  );
}

export function ShotPitch({ shots, animal, id }: { shots: EventRow[]; animal: number | null; id: string }) {
  return (
    <div>
      <PitchFrame label="Carte des tirs" id={id}>
        <rect width={W} height={H} fill="rgba(0,0,0,0.2)" />
        {shots.map((s) =>
          s.pos_depth == null || s.pos_lateral == null ? null : (
            <circle key={s.seq} cx={s.pos_depth * W} cy={s.pos_lateral * H} r={s.result === "goal" ? 2.3 : 1.8} fill={SHOT_RESULTS[s.result ?? ""]?.color ?? "#fff"} stroke="#0b1a0f" strokeWidth="0.4">
              <title>{`${s.minute}' ${footballerName(animal, s.number)} : ${SHOT_RESULTS[s.result ?? ""]?.label ?? s.result}`}</title>
            </circle>
          ),
        )}
      </PitchFrame>
      <div className="mt-2 flex flex-wrap gap-3 text-xs text-ink-dim">
        {Object.entries(SHOT_RESULTS).map(([k, v]) => (
          <span key={k} className="inline-flex items-center gap-1.5">
            <i className="inline-block size-2.5 rounded-full" style={{ background: v.color }} />
            {v.label} ({shots.filter((s) => s.result === k).length})
          </span>
        ))}
      </div>
    </div>
  );
}
