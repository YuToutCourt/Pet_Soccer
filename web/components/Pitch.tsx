// Terrains SVG du détail de match. Terrain vu par l'équipe : son but à gauche, elle attaque vers la droite.
// Repère du jeu : profondeur 0..1 (0 = son but), latéral 0..1 (0 = haut de l'écran).
// Proportions et lignes du terrain du jeu (mini-carte du HUD, Data/Menu/P_Menu.png : 146 x 82 px, terrain de
// 5802 x 3196 unités) : surface sur 16 % de la longueur et la moitié de la largeur, pas de petite surface.
import type { ReactNode } from "react";
import type { ShotMap } from "@/lib/queries";

const W = 105;
const H = 58;
const BOX_D = 0.159 * W;
const BOX_W = 0.49 * H;
const CIRCLE = 0.138 * W;
const clamp = (v: number, a: number, b: number) => Math.min(Math.max(v, a), b);

/** half : seulement une moitié du terrain (la droite, ou la gauche avec half = "left"), cages comprises. */
export function PitchFrame({ children, label, id, half }: { children?: ReactNode; label: string; id: string; half?: "left" | "right" }) {
  const box = half === "right" ? `${W / 2} -1 ${W / 2 + 2.5} ${H + 2}` : half === "left" ? `-2.5 -1 ${W / 2 + 2.5} ${H + 2}` : `0 0 ${W} ${H}`;
  return (
    <svg viewBox={box} role="img" aria-label={label} className="block h-auto w-full rounded-xl shadow-[inset_0_0_0_1px_rgba(255,255,255,0.06)]">
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
        <circle cx={W / 2} cy={H / 2} r={CIRCLE} />
        <circle cx={W / 2} cy={H / 2} r="0.5" fill="rgba(255,255,255,.55)" />
        <rect x="0.5" y={H / 2 - BOX_W / 2} width={BOX_D - 0.5} height={BOX_W} />
        <rect x={W - BOX_D} y={H / 2 - BOX_W / 2} width={BOX_D - 0.5} height={BOX_W} />
        {half && (
          <>
            <rect x="-2" y={H / 2 - 3.66} width="2" height="7.32" stroke="rgba(255,255,255,.8)" />
            <rect x={W} y={H / 2 - 3.66} width="2" height="7.32" stroke="rgba(255,255,255,.8)" />
          </>
        )}
      </g>
    </svg>
  );
}

/** Le coin du gardien (devant son but) : jusqu'au mod v13 il y passait tout le match, et la heatmap de l'équipe en
 * faisait sa case la plus chaude. */
function inKeeperZone(d: number, l: number, nd: number, nl: number) {
  return d < Math.max(1, Math.round(nd / 6)) && l >= Math.floor(nl / 4) && l < Math.ceil((3 * nl) / 4);
}

/**
 * fieldScale : l'intensité est rapportée à la case la plus chaude hors du coin du gardien (les cases de ce coin
 * plus chaudes sont plafonnées). Sert aux heatmaps d'équipe, dont celles d'avant le mod v13 comptent le gardien.
 */
export function HeatPitch({ grid: raw, color, label, id, fieldScale = false }: { grid: number[][]; color: string; label: string; id: string; fieldScale?: boolean }) {
  const nd = raw.length;
  const nl = raw[0]?.length ?? 0;
  let grid = raw;
  if (fieldScale) {
    const max = Math.max(0, ...raw.flatMap((row, d) => row.filter((_, l) => !inKeeperZone(d, l, nd, nl))));
    if (max > 0) grid = raw.map((row) => row.map((v) => Math.min(255, (v * 255) / max)));
  }
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

/**
 * Où les buts sont marqués : tirs gagnants dans le repère du tireur (il attaque vers la droite), sur sa moitié
 * offensive. against : vu par l'équipe qui encaisse, son but à gauche. La case la plus prolifique est entourée.
 */
export function GoalMap({ map, d: nd, l: nl, id, against = false, color = "#ff5a64" }: { map: ShotMap; d: number; l: number; id: string; against?: boolean; color?: string }) {
  const max = Math.max(1, ...map.cells.map((c) => c.goals));
  const total = map.cells.reduce((a, c) => a + c.goals, 0);
  const best = map.cells.reduce<ShotMap["cells"][number] | null>((a, c) => (c.goals > (a?.goals ?? 0) ? c : a), null);
  const cw = W / 2 / nd;
  const ch = H / nl;
  // x de la case dans le repère du tireur, retourné si on regarde depuis l'équipe qui encaisse
  const cx = (d: number) => (against ? W / 2 - (d + 1) * cw : W / 2 + d * cw);
  return (
    <PitchFrame label={against ? "Où l'équipe encaisse" : "Où les buts sont marqués"} id={id} half={against ? "left" : "right"}>
      <rect width={W} height={H} fill="rgba(0,0,0,0.4)" />
      <g filter={`url(#${id}-blur)`}>
        {map.cells.map((c) =>
          c.goals > 0 ? (
            <rect key={`${c.d}-${c.l}`} x={cx(c.d)} y={c.l * ch} width={cw + 0.3} height={ch + 0.3} fill={color} fillOpacity={Math.min(0.95, 0.15 + (c.goals / max) * 0.8)} />
          ) : null,
        )}
      </g>
      {best && (
        <g>
          <rect x={cx(best.d)} y={best.l * ch} width={cw} height={ch} fill="none" stroke="#fff" strokeWidth="0.5" strokeDasharray="1 0.7" />
          <text x={cx(best.d) + cw / 2} y={best.l === 0 ? ch + 3 : best.l * ch - 1} fontSize="2.6" textAnchor="middle" fill="#fff" fontWeight="800" stroke="rgba(0,0,0,.7)" strokeWidth="0.5" paintOrder="stroke">
            {best.goals} buts
          </text>
        </g>
      )}
      {map.cells.map((c) => (
        <rect key={`t-${c.d}-${c.l}`} x={cx(c.d)} y={c.l * ch} width={cw} height={ch} fill="transparent">
          <title>{`${c.goals} but${c.goals > 1 ? "s" : ""} sur ${c.shots} tir${c.shots > 1 ? "s" : ""} (${Math.round((100 * c.goals) / c.shots)} % de réussite, ${total ? Math.round((100 * c.goals) / total) : 0} % des buts)`}</title>
        </rect>
      ))}
    </PitchFrame>
  );
}

export const ZONES: Record<string, string> = { box: "Surface de réparation", front: "Devant la surface", far: "De loin" };

/** Tableau des buts par zone : part des buts et réussite des tirs. */
export function ZoneTable({ map }: { map: ShotMap }) {
  const goals = map.zones.reduce((a, z) => a + z.goals, 0);
  const shots = map.zones.reduce((a, z) => a + z.shots, 0);
  return (
    <table className="data-table whitespace-nowrap">
      <thead>
        <tr><th>Zone de tir</th><th className="text-right">Buts</th><th className="text-right">Part</th><th className="text-right">Tirs</th><th className="text-right">Réussite</th></tr>
      </thead>
      <tbody>
        {map.zones.map((z) => (
          <tr key={z.zone}>
            <td className="font-bold">{ZONES[z.zone]}</td>
            <td className="text-right tabular">{z.goals}</td>
            <td className="text-right tabular">{goals ? Math.round((100 * z.goals) / goals) : 0} %</td>
            <td className="text-right tabular">{z.shots}</td>
            <td className="text-right font-extrabold tabular">{z.shots ? Math.round((100 * z.goals) / z.shots) : 0} %</td>
          </tr>
        ))}
        <tr>
          <td className="font-bold text-ink-dim">Total</td>
          <td className="text-right tabular">{goals}</td><td />
          <td className="text-right tabular">{shots}</td>
          <td className="text-right font-extrabold tabular">{shots ? Math.round((100 * goals) / shots) : 0} %</td>
        </tr>
      </tbody>
    </table>
  );
}
