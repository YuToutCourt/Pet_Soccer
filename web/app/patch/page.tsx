import type { Metadata } from "next";
import { ArrowDown, ArrowUp, Crosshair, Equal, Hand, Maximize2, Shield, Swords, Target, Timer, TrendingUp, Trophy, Zap } from "lucide-react";
import { SectionTitle, TeamLogo } from "@/components/ui";
import { team } from "@/lib/game";

export const metadata: Metadata = {
  title: "Notes de patch",
  description: "Patch du 27 novembre après trois séries de matchs IA contre IA : la Force sert enfin, les petits gardiens sont aidés, la jauge SUPER aide l'équipe menée, 10 équipes retouchées et la tier list.",
};

// Sources de lettres de la jauge SUPER (mods/data/Data/supers.scr : FillTime, ShotsPerLetter, RecoveriesPerLetter, SavesPerLetter)
const SUPER_SOURCES = [
  { Icon: Timer, title: "10 s de possession", note: "Comme avant : les passes ne coupent pas la possession.", isNew: false },
  { Icon: Trophy, title: "1 but", note: "Comme avant, sauf un but sur super tir.", isNew: false },
  { Icon: Target, title: "8 tirs cadrés", note: "Arrêtés par le gardien ou marqués. Un tir à côté, contré ou sur le poteau ne compte pas, le super tir non plus.", isNew: true },
  { Icon: Swords, title: "15 ballons récupérés", note: "Chaque ballon repris à l’adversaire : tacle ou interception.", isNew: true },
  { Icon: Hand, title: "8 arrêts", note: "Chaque arrêt de ton gardien.", isNew: true },
];

// Stats avant / après (mods/data/Data/team_stats.scr). Ordre : Force, Précision, Réflexe.
type Stats = [number, number, number];
interface TeamPatch {
  animal: number;
  before: Stats;
  after: Stats;
  /** largeur du modèle (hitbox), codée en dur dans le jeu */
  width: number;
  /** bonus de la règle d'équilibrage pour la taille */
  size: number;
  role: string;
  note: string;
}

const PATCH: TeamPatch[] = [
  { animal: 6, before: [90, 90, 90], after: [100, 80, 85], width: 110, size: 20, role: "Mur défensif", note: "La plus grosse hitbox du jeu avec des stats neutres : le Gorille ralentit et vise moins bien, mais frappe plus fort." },
  { animal: 13, before: [90, 90, 100], after: [100, 80, 95], width: 60, size: 0, role: "Puissant et rapide", note: "Vitesse maximale sans aucun défaut : il perd un peu de vitesse et de précision et gagne en frappe." },
  { animal: 3, before: [95, 85, 90], after: [100, 84, 85], width: 100, size: 16, role: "Costaud", note: "Très large, il récupérait trop de ballons : il ralentit." },
  { animal: 1, before: [100, 90, 80], after: [110, 80, 82], width: 101, size: 16, role: "Gros tireur", note: "Encore plus de puissance, des tirs moins cadrés." },
  { animal: 5, before: [95, 95, 80], after: [95, 91, 80], width: 90, size: 24, role: "Jeu aérien", note: "Elle capte les ballons hauts que personne d’autre n’atteint et, en gardienne, arrête les tirs hauts sans sauter. Elle le paie en précision." },
  { animal: 2, before: [80, 90, 100], after: [80, 90, 100], width: 60, size: 0, role: "Rapide", note: "Déjà dans la cible : aucun changement." },
  { animal: 4, before: [85, 85, 100], after: [86, 86, 100], width: 55, size: -2, role: "Rapide", note: "Petit coup de pouce aux tirs." },
  { animal: 0, before: [90, 90, 90], after: [95, 95, 90], width: 59.5, size: 0, role: "Polyvalent", note: "Rien de spécial avant, un peu meilleur partout maintenant." },
  { animal: 12, before: [100, 85, 85], after: [110, 80, 90], width: 59.5, size: 0, role: "Puissant", note: "Retrouve sa vitesse et frappe encore plus fort." },
  { animal: 7, before: [90, 100, 80], after: [100, 105, 82], width: 62, size: 1, role: "Finisseur", note: "Était identique à la Panthère et à la Tortue : devient le meilleur buteur." },
  { animal: 8, before: [90, 100, 80], after: [84, 100, 93], width: 60, size: 0, role: "Vive et précise", note: "Beaucoup plus rapide, frappe moins forte." },
  { animal: 9, before: [90, 85, 90], after: [110, 85, 90], width: 48, size: -5, role: "Frappeur", note: "Petite hitbox et stats moyennes : il devient une vraie menace de loin." },
  { animal: 11, before: [80, 95, 95], after: [80, 106, 95], width: 45, size: -6, role: "Petit et précis", note: "La plus petite hitbox : il compense par une précision au-delà de 100." },
  { animal: 10, before: [90, 100, 80], after: [100, 114, 80], width: 51, size: -4, role: "Tireuse d'élite", note: "Lente et petite, l'équipe la plus faible : ses tirs sont maintenant les plus précis du jeu." },
];

const STATS = [
  { label: "Force", hint: "puissance des tirs" },
  { label: "Précision", hint: "écart des tirs" },
  { label: "Réflexe", hint: "vitesse" },
];

/** Note globale de la règle d'équilibrage : 10 pour tout le monde après le patch. */
function value(s: Stats, size: number): number {
  return s[0] - 90 + (s[1] - 90) + 2 * (s[2] - 90) + size;
}

const BAR_MIN = 70;
const BAR_MAX = 120;
const barPct = (v: number) => `${((Math.min(BAR_MAX, Math.max(BAR_MIN, v)) - BAR_MIN) / (BAR_MAX - BAR_MIN)) * 100}%`;

// ---------------------------------------------------------------------------------------------------------------
// Patch du 27 novembre 2026 : tout ce qui a changé depuis le 25, après trois séries de matchs IA contre IA
// (notes/meta.md). Réglages : mods/data/Data/team_stats.scr (Effects, ForceEffects, KeeperMinReach, KeeperMinWidth,
// Team) et supers.scr (CatchUp*, GoalLetter). Chiffres de la série 3 : build/meta/serie3.sql (910 matchs).
// ---------------------------------------------------------------------------------------------------------------

/** Points par match de chaque équipe dans la série 3 (910 matchs, 130 par équipe). */
const S3_PPM: Record<number, number> = {
  5: 1.84, 3: 1.8, 2: 1.51, 4: 1.51, 1: 1.42, 6: 1.34, 0: 1.32,
  13: 1.28, 7: 1.25, 12: 1.22, 8: 1.17, 9: 1.16, 11: 1.12, 10: 0.88,
};
/** Bilan de la série 3 : victoires, nuls, défaites. */
const S3_RECORD: Record<number, [number, number, number]> = {
  0: [42, 46, 42], 1: [48, 40, 42], 2: [51, 42, 36], 3: [64, 44, 23], 4: [53, 37, 40], 5: [64, 47, 19], 6: [46, 36, 48],
  7: [41, 40, 49], 8: [39, 35, 56], 9: [37, 40, 53], 10: [26, 37, 68], 11: [38, 32, 60], 12: [36, 50, 43], 13: [43, 38, 49],
};
/** Part des matchs en % : 0 victoires, 1 nuls, 2 défaites. */
const s3pct = (a: number, i: 0 | 1 | 2) => {
  const r = S3_RECORD[a];
  return Math.round((100 * r[i]) / (r[0] + r[1] + r[2]));
};
const ordinal = (n: number) => `${n}${n === 1 ? "re" : "e"}`;

/** Buts encaissés pour 100 tirs subis dans la série 3 : surtout la taille du gardien. */
const S3_CONCEDED: Record<number, number> = {
  5: 3.2, 3: 3.9, 1: 5.3, 12: 5.6, 0: 5.9, 6: 6.5, 7: 6.6,
  4: 7.1, 9: 7.3, 2: 7.5, 8: 7.5, 13: 8.2, 10: 9.2, 11: 10.1,
};
const S3_RANK = Object.entries(S3_PPM)
  .sort((a, b) => b[1] - a[1])
  .reduce<Record<number, number>>((acc, [a, v], i, arr) => ({ ...acc, [Number(a)]: arr.findIndex((x) => x[1] === v) + 1 }), {});

/** Stats en vigueur (team_stats.scr) : Force, Précision, Réflexe. */
const CURRENT: Record<number, Stats> = {
  0: [95, 95, 90], 1: [105, 85, 82], 2: [80, 90, 100], 3: [100, 84, 83], 4: [86, 86, 100], 5: [90, 88, 80], 6: [95, 85, 85],
  7: [100, 105, 82], 8: [88, 100, 93], 9: [105, 92, 92], 10: [100, 108, 88], 11: [84, 106, 97], 12: [105, 89, 90], 13: [95, 85, 95],
};

/** Retouches faites après la série 3 : la tier list (mesurée avant) devrait bouger dans ce sens. */
const AFTER_S3: Record<number, 1 | -1> = { 3: -1, 5: -1, 8: 1, 9: 1, 10: 1, 11: 1, 12: 1 };

/** Hauteur du modèle de chaque animal (gardien), codée en dur dans le jeu. */
const HEIGHT: Record<number, number> = {
  0: 100, 1: 85, 2: 76, 3: 105, 4: 83.6, 5: 147, 6: 72.5, 7: 88, 8: 74, 9: 84, 10: 77.4, 11: 57, 12: 100, 13: 74,
};

const RANGE_PER_POINT = 0.5; // ForceEffects a, % par point au-dessus de 90
const LOB_BASE = 70; // un tir visé plus haut part en lob
const LOB_PER_POINT = 1.5; // ForceEffects c, % par point au-dessus de 90
const KEEPER_MIN = 95; // KeeperMinReach
const KEEPER_MIN_WIDTH = 90; // KeeperMinWidth

const rangePct = (f: number) => Math.max(0, f - 90) * RANGE_PER_POINT;
const lobLimit = (f: number) => LOB_BASE * (1 + (Math.max(0, f - 90) * LOB_PER_POINT) / 100);
const fr = (n: number, d = 0) => n.toLocaleString("fr-FR", { minimumFractionDigits: d, maximumFractionDigits: d });

interface TeamChange {
  animal: number;
  note: string;
}

/** Équipes dont les stats ont changé depuis le 25 novembre (avant : PATCH[].after). */
const CHANGES: TeamChange[] = [
  { animal: 5, note: "Première des séries 1 et 3, avec une gardienne presque imbattable (3,2 buts pour 100 tirs subis). Elle perd sa portée de tir bonus (Force 90) et 3 points de Précision : il faut construire le jeu pour marquer." },
  { animal: 3, note: "Toujours dans le trio de tête, porté par son gardien (3,9 buts pour 100 tirs). Il perd 2 points de Réflexe." },
  { animal: 10, note: "Dernière des trois séries (20 % de victoires et 52 % de défaites en série 3). Trop lente pour profiter de sa précision : elle échange 6 points de Précision contre 8 points de Réflexe." },
  { animal: 11, note: "Le plus petit gardien du jeu (10,1 buts pour 100 tirs, le pire taux). Il frappe plus fort et va plus vite, et son gardien est aidé en hauteur comme en largeur." },
  { animal: 9, note: "Dernier de la série 1 malgré sa Force 110, qui ne lui rapportait rien. 5 points passent dans la Précision, puis +2 en Précision et en Réflexe après la série 3." },
  { animal: 8, note: "En bas du classement de la série 3 (30 % de victoires, 43 % de défaites). Elle gagne 4 points de Force : ses tirs partent plus fort." },
  { animal: 12, note: "5 points de Force passent dans la Précision, puis 4 de plus après la série 3, où il n’était que 10e. Il reste un puissant : il tire de plus loin que la base." },
  { animal: 1, note: "Toujours le gros tireur : il tire de 7,5 % plus loin et frappe plus souvent droit en hauteur, mais 5 points de Force vont à la Précision." },
  { animal: 6, note: "Avant-dernier de la série 1, avec un petit gardien. 5 points de Force passent dans la Précision et son gardien profite de l’aide aux petits gardiens." },
  { animal: 13, note: "Tirs trop peu cadrés et gardien parmi les plus faibles : 5 points de Force passent dans la Précision." },
];

const META_FINDINGS = [
  {
    Icon: Zap,
    title: "La Force ne servait à rien",
    text: "Elle ne faisait qu’accélérer le ballon. Les équipes à Force 110 avaient même le plus bas taux de buts sur tirs cadrés (30 %, contre 39 % pour la Tortue et la Panthère).",
  },
  {
    Icon: Shield,
    title: "Le gardien, c’est la taille",
    text: "Sur 100 tirs subis, la Girafe encaisse 3,2 buts et le Dino 3,9, la Tortue 9,2 et le Robot 10,1, presque dans l’ordre de la taille du modèle. Même après l’aide aux petits gardiens, c’est ce qui sépare le haut du bas du classement.",
  },
  {
    Icon: TrendingUp,
    title: "Les supers font boule de neige",
    text: "L’équipe qui lance le plus de supers gagnait 70 % de ses matchs et n’en perdait que 4,5 %. Chaque but donnait en plus une lettre à l’équipe qui marque. Menée de 2 buts, une équipe n’évitait la défaite que 11 % du temps.",
  },
  {
    Icon: Crosshair,
    title: "Les tirs dans l’axe partaient toujours du même côté",
    text: "Un bug d’origine : l’écart d’un tir droit au centre était toujours dans le même sens. Les tirs d’un côté du but marquaient 8,5 %, ceux de l’autre côté 5,1 %.",
  },
];

// Sources de lettres en vigueur (supers.scr)
const SUPER_SOURCES_NOW = [
  { Icon: Timer, title: "10 s de possession", note: "Comme avant. Plus rapide pour une équipe menée (voir le tableau).", badge: "plus rapide si menée" },
  { Icon: Target, title: "8 tirs cadrés", note: "Arrêtés par le gardien ou marqués. Le super tir ne compte pas.", badge: "" },
  { Icon: Swords, title: "15 ballons récupérés", note: "Tacle ou interception.", badge: "" },
  { Icon: Hand, title: "8 arrêts", note: "Chaque arrêt de ton gardien.", badge: "" },
  { Icon: Trophy, title: "Un but", note: "Ne donne plus de lettre : marquer suffit comme récompense.", badge: "supprimé" },
];

const CATCH_UP = [
  { deficit: "À égalité ou devant", mul: "×1", possession: "10 s", shots: "8", recov: "15" },
  { deficit: "Mené de 1 but", mul: "×1,2", possession: "8,3 s", shots: "7", recov: "13" },
  { deficit: "Mené de 2 buts ou plus", mul: "×1,4", possession: "7,1 s", shots: "6", recov: "11" },
];

// Tier list : points par match de la série 3 (une victoire 3 points, un nul 1).
const TIERS = [
  { tier: "S", min: 1.7, color: "#ff5a64", label: "Au-dessus du lot" },
  { tier: "A", min: 1.4, color: "#f5a623", label: "Très solides" },
  { tier: "B", min: 1.2, color: "#ffd84a", label: "Dans la moyenne" },
  { tier: "C", min: 1.0, color: "#5fd38d", label: "En retrait" },
  { tier: "D", min: 0, color: "#70a6ff", label: "À la traîne" },
];

function TierList() {
  const teams = Object.keys(S3_PPM)
    .map(Number)
    .sort((a, b) => S3_PPM[b] - S3_PPM[a] || a - b);
  return (
    <div className="card overflow-hidden">
      {TIERS.map((t, i) => {
        const next = TIERS[i - 1]?.min ?? Infinity;
        const rows = teams.filter((a) => S3_PPM[a] >= t.min && S3_PPM[a] < next);
        return (
          <div key={t.tier} className="flex border-b border-line last:border-b-0">
            <div className="flex w-16 shrink-0 flex-col items-center justify-center py-3 sm:w-24" style={{ background: `${t.color}26`, boxShadow: `inset 4px 0 0 ${t.color}` }}>
              <span className="font-display text-4xl leading-none" style={{ color: t.color }}>{t.tier}</span>
              <span className="mt-1 hidden text-center text-[0.65rem] font-bold uppercase tracking-wide text-ink-muted sm:block">{t.label}</span>
            </div>
            <ul className="flex flex-1 flex-wrap items-center gap-2 p-3">
              {rows.length === 0 && <li className="text-sm text-ink-muted">Aucune équipe</li>}
              {rows.map((a) => {
                const move = AFTER_S3[a];
                return (
                  <li key={a} className="flex items-center gap-2 rounded-xl border border-line bg-black/25 py-1.5 pl-1.5 pr-3" title={`${team(a).name} : ${S3_RECORD[a][0]} victoires, ${S3_RECORD[a][1]} nuls, ${S3_RECORD[a][2]} défaites en série 3${move ? (move > 0 ? ", renforcée depuis" : ", affaiblie depuis") : ""}`}>
                    <TeamLogo animal={a} size={34} />
                    <span className="min-w-0">
                      <span className="block font-bold leading-tight text-ink">{team(a).name}</span>
                      <span className="block text-xs text-ink-muted tabular">
                        {s3pct(a, 0)} % V · {s3pct(a, 2)} % D
                        {move === 1 && <span className="ml-1.5 font-bold text-win">▲ renforcée</span>}
                        {move === -1 && <span className="ml-1.5 font-bold text-loss">▼ affaiblie</span>}
                      </span>
                    </span>
                  </li>
                );
              })}
            </ul>
          </div>
        );
      })}
    </div>
  );
}

function PatchNow() {
  const before = (a: number) => PATCH.find((p) => p.animal === a)!;
  const patchCards: TeamPatch[] = CHANGES.map((c) => ({ ...before(c.animal), before: before(c.animal).after, after: CURRENT[c.animal], note: c.note }));
  const unchanged = PATCH.filter((p) => !CHANGES.some((c) => c.animal === p.animal)).map((p) => team(p.animal).name);
  const allTeams = Object.keys(CURRENT)
    .map(Number)
    .sort((a, b) => CURRENT[b][0] - CURRENT[a][0] || a - b);

  return (
    <div className="space-y-8">
      <div>
        <span className="chip bg-gold-300/15 text-gold-300">27 novembre 2026</span>
        <h1 className="title-game mt-2 text-4xl">Notes de patch</h1>
        <p className="mt-2 max-w-3xl text-ink-dim">
          Depuis le patch du 25 novembre, trois séries de matchs IA contre IA ont été jouées, chaque équipe contre toutes les
          autres (910 matchs pour la dernière). Elles ont montré que la Force ne servait à rien, que la taille du gardien
          décidait de beaucoup trop de matchs et qu’une équipe menée ne revenait presque jamais. Ce patch corrige ces trois
          points, un vieux bug de tir, et retouche 10 équipes sur 14.
        </p>
        <p className="mt-2 max-w-3xl text-sm text-ink-muted">
          En ligne, les deux joueurs doivent avoir la nouvelle version du mod pour jouer ensemble. Les valeurs des équipes
          seront encore ajustées si la prochaine série de matchs le demande.
        </p>
      </div>

      <section>
        <SectionTitle sub="Sur les séries de matchs IA contre IA jouées depuis le 25 novembre.">Ce que la méta a montré</SectionTitle>
        <div className="grid gap-4 sm:grid-cols-2">
          {META_FINDINGS.map(({ Icon, title, text }) => (
            <div key={title} className="card flex gap-3 p-5">
              <Icon className="mt-1 size-5 shrink-0 text-gold-300" aria-hidden />
              <div>
                <h3 className="font-display text-lg text-gold-100">{title}</h3>
                <p className="mt-1 text-sm text-ink-dim">{text}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      <section>
        <SectionTitle sub="Ce qui change pour toutes les équipes.">Changements de jeu</SectionTitle>
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
          <div className="card p-5">
            <h3 className="font-display text-xl text-gold-100">La Force, enfin utile</h3>
            <ul className="mt-2 list-disc space-y-1.5 pl-5 text-sm text-ink-dim">
              <li>
                <b className="text-ink">Portée de tir</b> : au-dessus de Force 90, chaque point permet de tirer 0,5 % plus
                loin (+7,5 % à Force 105). En dessous de 90, la portée reste celle d’origine : pas de pénalité.
              </li>
              <li>
                <b className="text-ink">Tirs droits en hauteur</b> : au-dessus de Force 90, une partie des tirs qui partaient
                en lob part maintenant en tir droit et haut, plus dur à lire pour le gardien. L’effet reste faible :
                environ un tir de loin sur dix pour un gros tireur.
              </li>
              <li>La précision ne change pas avec la Force : un tir de loin reste aussi dispersé.</li>
            </ul>
          </div>
          <div className="card p-5">
            <h3 className="font-display text-xl text-gold-100">Précision et vitesse</h3>
            <p className="mt-2 text-sm text-ink-dim">
              Un point de Précision réduit maintenant l’écart des tirs de <b className="text-ink">2,5 %</b> au lieu de 2 %.
              Un point de Réflexe donne <b className="text-ink">0,7 %</b> de vitesse au lieu de 0,8 %.
            </p>
            <p className="mt-2 text-sm text-ink-dim">
              Sur la méta, un point de Réflexe valait bien deux points de Précision : le Réflexe reste compté double dans
              l’équilibrage.
            </p>
          </div>
          <div className="card p-5">
            <h3 className="font-display text-xl text-gold-100">Aide aux petits gardiens</h3>
            <p className="mt-2 text-sm text-ink-dim">
              Pour les arrêts, un gardien compte maintenant comme s’il mesurait au moins <b className="text-ink">{KEEPER_MIN}</b>{" "}
              de haut (ballons hauts, décision de sauter) et <b className="text-ink">{KEEPER_MIN_WIDTH}</b> de large (ballons
              sur le côté). Le Robot, le Requin, la Tortue, la Panthère et les autres petits gardiens arrêtent plus de tirs.
            </p>
            <p className="mt-2 text-sm text-ink-dim">
              Seul le gardien en profite : la hitbox, le tacle et la réception des joueurs de champ ne changent pas.
            </p>
          </div>
          <div className="card p-5">
            <h3 className="font-display text-xl text-gold-100">Correction : tirs dans l’axe</h3>
            <p className="mt-2 text-sm text-ink-dim">
              Un tir droit visé au centre du but s’écartait toujours du même côté. Il part maintenant d’un côté ou de
              l’autre au hasard : les deux côtés du but ont le même taux de buts.
            </p>
          </div>
        </div>
      </section>

      <section>
        <SectionTitle sub="Barre pâle : 25 novembre. Barre pleine : maintenant.">
          Équipes modifiées depuis le 25 novembre
        </SectionTitle>
        <p className="mb-4 rounded-xl border border-line bg-black/20 px-4 py-2.5 text-sm text-ink-dim">
          Le badge donne la <b className="text-ink">place de l’équipe sur 14</b> dans la série 3 (vert : dans les 4 premières,
          rouge : dans les 5 dernières), et la barre du bas son bilan sur ses 130 matchs. Mesuré avant les dernières retouches.
        </p>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {patchCards.map((p) => (
            <TeamCard
              key={p.animal}
              p={p}
              chip={{
                label: `${ordinal(S3_RANK[p.animal])} sur 14`,
                cls: `normal-case ${S3_RANK[p.animal] <= 4 ? "bg-win/15 text-win" : S3_RANK[p.animal] >= 10 ? "bg-loss/15 text-loss" : "bg-draw/15 text-draw"}`,
                title: "Place de l’équipe dans la série 3 (910 matchs)",
              }}
              record={S3_RECORD[p.animal]}
            />
          ))}
        </div>
        <p className="mt-3 text-sm text-ink-muted">
          {unchanged.join(", ").replace(/, ([^,]*)$/, " et $1")} gardent leurs stats du 25 novembre.
        </p>
      </section>

      <section>
        <SectionTitle sub="Mesurée sur la série 3 (910 matchs, IA de niveau 7 et 8, 130 matchs par équipe). Les flèches montrent les équipes retouchées depuis : elles devraient monter ou descendre d’un cran à la prochaine série.">
          Tier list
        </SectionTitle>
        <TierList />
        <p className="mt-3 text-sm text-ink-muted">
          V : part de matchs gagnés, D : part de matchs perdus (le reste, ce sont des nuls). Les équipes sont rangées comme au
          classement d’un championnat : une victoire rapporte 3 points, un nul 1. Avec 130 matchs par équipe, le hasard
          suffit à faire bouger une équipe de quelques pourcents : deux équipes voisines se valent.
        </p>
      </section>

      <section>
        <SectionTitle sub="Pour qu’une équipe menée puisse revenir, sans que le super devienne facile à avoir.">Jauge SUPER</SectionTitle>
        <div className="grid gap-4 lg:grid-cols-2">
          <div className="card p-5">
            <h3 className="font-display text-xl text-gold-100">Rattrapage de l’équipe menée</h3>
            <p className="mt-2 text-sm text-ink-dim">
              Une équipe menée remplit sa jauge 20 % plus vite par but de retard, jamais plus de 40 %. Ça vaut pour la
              possession comme pour les actions. Il faut toujours 5 lettres : sur une mi-temps entière à un but de retard, ça
              fait environ une demi-lettre de plus.
            </p>
            <div className="mt-4 overflow-x-auto">
              <table className="data-table data-table-compact">
                <thead>
                  <tr>
                    <th>Score</th>
                    <th className="text-right">Jauge</th>
                    <th className="text-right">Possession</th>
                    <th className="text-right">Tirs cadrés / arrêts</th>
                    <th className="text-right">Récupérations</th>
                  </tr>
                </thead>
                <tbody>
                  {CATCH_UP.map((c) => (
                    <tr key={c.deficit}>
                      <td className="font-bold text-ink">{c.deficit}</td>
                      <td className="tabular text-right">{c.mul}</td>
                      <td className="tabular text-right">{c.possession}</td>
                      <td className="tabular text-right">{c.shots}</td>
                      <td className="tabular text-right">{c.recov}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <p className="mt-3 text-xs text-ink-muted">Pas de rattrapage au but en or.</p>
          </div>

          <div className="card p-5">
            <h3 className="font-display text-xl text-gold-100">Une lettre pour…</h3>
            <ul className="mt-3 space-y-2.5">
              {SUPER_SOURCES_NOW.map(({ Icon, title, note, badge }) => (
                <li key={title} className="flex gap-3 rounded-lg bg-black/20 p-3">
                  <Icon className="mt-0.5 size-5 shrink-0 text-gold-300" aria-hidden />
                  <div className="min-w-0">
                    <p className="font-bold text-ink">
                      <span className={badge === "supprimé" ? "text-ink-muted line-through" : undefined}>{title}</span>
                      {badge && (
                        <span className={`chip ml-2 ${badge === "supprimé" ? "bg-loss/15 text-loss" : "bg-win/15 text-win"}`}>
                          {badge}
                        </span>
                      )}
                    </p>
                    <p className="mt-0.5 text-sm text-ink-dim">{note}</p>
                  </div>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>
    </div>
  );
}

/** Taille du gardien : celle du modèle, ou le minimum en vert quand l’aide aux petits gardiens joue. */
function KeeperDim({ real, min }: { real: number; min: number }) {
  if (real >= min) return <>{fr(real, real % 1 ? 1 : 0)}</>;
  return (
    <span title={`${fr(real, real % 1 ? 1 : 0)} → ${min}`} className="font-bold text-win">
      {min}
    </span>
  );
}

export default function Page() {
  return (
    <div className="space-y-14">
      <PatchNow />
      <div className="border-t border-white/10 pt-10">
        <OldPatch />
      </div>
    </div>
  );
}

function OldPatch() {
  return (
    <div className="space-y-8">
      <div>
        <span className="chip bg-ink/10 text-ink-muted">25 novembre 2026 · patch précédent</span>
        <h2 className="title-game mt-2 text-3xl">Rééquilibrage des équipes</h2>
        <p className="mt-2 max-w-3xl text-ink-dim">
          Les 14 équipes sont rééquilibrées. Certaines stats valaient beaucoup plus que d’autres, et la taille des animaux,
          qui compte énormément pour récupérer le ballon, n’était pas prise en compte. La jauge SUPER est aussi gardée à la
          mi-temps, et de nouvelles actions la remplissent (en bas de la page).
        </p>
      </div>

      <section className="grid gap-4 md:grid-cols-3">
        <div className="card p-5">
          <h2 className="font-display text-xl text-gold-100">La taille compte</h2>
          <p className="mt-2 text-sm text-ink-dim">
            Chaque animal a sa hitbox, de 45 (Robot) à 110 de large (Gorille). Plus elle est large, plus le joueur attrape le ballon
            et tacle de loin : le Gorille contrôle le ballon 50 à 60 % plus loin que le Robot, et tacle 25 % plus loin. Les gros animaux paient maintenant cet avantage en
            vitesse ou en précision.
          </p>
        </div>
        <div className="card p-5">
          <h2 className="font-display text-xl text-gold-100">La vitesse compte double</h2>
          <p className="mt-2 text-sm text-ink-dim">
            Le Réflexe sert aux 7 joueurs pendant tout le match, la Force et la Précision seulement au moment de tirer. Un point de
            Réflexe vaut donc deux points des autres stats.
          </p>
        </div>
        <div className="card p-5">
          <h2 className="font-display text-xl text-gold-100">Au-delà de 100</h2>
          <p className="mt-2 text-sm text-ink-dim">
            Les petites équipes peuvent dépasser 100 pour compenser. La Précision passe de 3 % à 2 % d’effet par point, et vitesse de 0.5 à 0.8 pour qu’une
            Précision de 114 reste humaine : les tirs de la Tortue s’écartent deux fois moins que la moyenne.
          </p>
        </div>
      </section>

      <section>
        <SectionTitle sub="Barre pâle : avant. Barre pleine : après. Équilibre : la note globale de l’équipe (stats + taille) avant le patch, comparée à la cible.">
          Équipe par équipe
        </SectionTitle>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {PATCH.map((p) => (
            <TeamCard key={p.animal} p={p} />
          ))}
        </div>
      </section>

      <section className="card p-5 text-sm text-ink-dim">
        <h2 className="font-display text-xl text-gold-100">La règle</h2>
        <p className="mt-2">
          Pour chaque équipe : (Force − 90) + (Précision − 90) + 2 × (Réflexe − 90) + bonus de taille = 10. Le bonus de taille
          vaut 0,8 par point de rayon au-dessus de 30, avec 12 points de plus pour la hauteur de la Girafe. Ces poids viennent
          d’estimations : ils seront ajustés selon les résultats des matchs en ligne (voir la page Méta).
        </p>
      </section>

      <section>
        <SectionTitle sub="La jauge ne se vide plus à la mi-temps, et la possession n’est plus le seul moyen de la remplir.">
          Jauge SUPER
        </SectionTitle>
        <div className="grid gap-4 md:grid-cols-2">
          <div className="card p-5">
            <h3 className="font-display text-xl text-gold-100">Gardée à la mi-temps</h3>
            <p className="mt-2 text-sm text-ink-dim">
              Avant, toutes les lettres disparaissaient au changement de côté. Maintenant chaque équipe garde ses lettres et le
              super qu’elle avait tiré. Un super complet en fin de 1re mi-temps reste utilisable en 2e mi-temps. La jauge ne
              repart à zéro qu’au début d’un match.
            </p>
          </div>
          <div className="card p-5">
            <h3 className="font-display text-xl text-gold-100">Pas de réserve</h3>
            <p className="mt-2 text-sm text-ink-dim">
              Les seuils sont volontairement hauts pour que le super reste rare. Rien ne se compte quand la jauge est pleine,
              pendant un super ou au but en or : impossible de stocker des lettres d’avance.
            </p>
          </div>
        </div>

        <div className="card mt-4 p-5">
          <h3 className="font-display text-xl text-gold-100">Une lettre pour…</h3>
          <p className="mt-1 text-sm text-ink-dim">5 lettres (S‑U‑P‑E‑R) font un super. Les compteurs continuent d’une mi-temps à l’autre.</p>
          <ul className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {SUPER_SOURCES.map(({ Icon, title, note, isNew }) => (
              <li key={title} className="flex gap-3 rounded-lg bg-black/20 p-3">
                <Icon className="mt-0.5 size-5 shrink-0 text-gold-300" aria-hidden />
                <div className="min-w-0">
                  <p className="font-bold text-ink">
                    {title}
                    {isNew && <span className="chip ml-2 bg-win/15 text-win">nouveau</span>}
                  </p>
                  <p className="mt-0.5 text-sm text-ink-dim">{note}</p>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </section>
    </div>
  );
}

function TeamCard({ p, chip, record }: { p: TeamPatch; chip?: { label: string; cls: string; title?: string }; record?: [number, number, number] }) {
  const t = team(p.animal);
  const gap = 10 - value(p.before, p.size);
  const balance =
    gap <= -3
      ? { label: "Affaiblie", cls: "bg-loss/15 text-loss", Icon: ArrowDown }
      : gap >= 3
        ? { label: "Renforcée", cls: "bg-win/15 text-win", Icon: ArrowUp }
        : { label: "Ajustée", cls: "bg-draw/15 text-draw", Icon: Equal };

  return (
    <article className="card flex flex-col p-4" style={{ borderTopColor: t.color, borderTopWidth: 3 }}>
      <div className="flex items-center gap-3">
        <TeamLogo animal={p.animal} size={44} />
        <div className="min-w-0 flex-1">
          <h3 className="truncate font-display text-lg leading-tight text-ink">{t.name}</h3>
          <p className="text-xs font-bold text-ink-muted">{p.role}</p>
        </div>
        {chip ? (
          <span className={`chip ${chip.cls}`} title={chip.title}>
            {chip.label}
          </span>
        ) : (
          <span className={`chip ${balance.cls}`} title={`Écart à la cible avant le patch : ${gap > 0 ? "+" : ""}${gap}`}>
            <balance.Icon className="size-3" aria-hidden />
            {balance.label}
          </span>
        )}
      </div>

      <div className="mt-4 space-y-2.5">
        {STATS.map((s, i) => {
          const b = p.before[i];
          const a = p.after[i];
          const d = a - b;
          return (
            <div key={s.label}>
              <div className="flex items-baseline justify-between text-sm">
                <span className="font-bold text-ink-dim">
                  {s.label} <span className="text-xs font-normal text-ink-muted">· {s.hint}</span>
                </span>
                <span className="tabular">
                  <span className="text-ink-muted">{b} → </span>
                  <span className="font-extrabold text-ink">{a}</span>
                  <span className={`ml-1.5 inline-block w-8 text-right text-xs font-extrabold ${d > 0 ? "text-win" : d < 0 ? "text-loss" : "text-ink-muted"}`}>
                    {d > 0 ? `+${d}` : d < 0 ? `${d}` : "="}
                  </span>
                </span>
              </div>
              <div className="relative mt-1 h-2 overflow-hidden rounded-full bg-black/35">
                <div className="absolute inset-y-0 left-0 rounded-full bg-ink/15" style={{ width: barPct(b) }} />
                <div
                  className={`absolute inset-y-0 left-0 rounded-full ${d > 0 ? "bg-win" : d < 0 ? "bg-loss" : "bg-gold-300"}`}
                  style={{ width: barPct(a), opacity: 0.85 }}
                />
                {/* repère 100 */}
                <div className="absolute inset-y-0 w-px bg-white/40" style={{ left: barPct(100) }} />
              </div>
            </div>
          );
        })}
      </div>

      <p className="mt-4 flex-1 text-sm text-ink-dim">{p.note}</p>

      {record && <RecordBar record={record} />}

      <div className="mt-3 flex items-center gap-1.5 text-xs text-ink-muted">
        <Maximize2 className="size-3.5" aria-hidden />
        <span>
          Hitbox <span className="font-bold text-ink-dim tabular">{String(p.width).replace(".", ",")}</span>
          {p.size !== 0 && (
            <span className="tabular"> · bonus taille {p.size > 0 ? `+${p.size}` : p.size}</span>
          )}
        </span>
      </div>
    </article>
  );
}

/** Bilan d'une série : victoires, nuls, défaites, en barre et en %. */
function RecordBar({ record }: { record: [number, number, number] }) {
  const n = record[0] + record[1] + record[2];
  const parts = [
    { label: "victoires", v: record[0], cls: "bg-win", text: "text-win" },
    { label: "nuls", v: record[1], cls: "bg-draw", text: "text-draw" },
    { label: "défaites", v: record[2], cls: "bg-loss", text: "text-loss" },
  ];
  return (
    <div className="mt-3" title={`Série 3 : ${record[0]} victoires, ${record[1]} nuls, ${record[2]} défaites sur ${n} matchs`}>
      <div className="flex h-1.5 gap-0.5 overflow-hidden rounded-full">
        {parts.map((x) => <span key={x.label} className={x.cls} style={{ width: `${(100 * x.v) / n}%` }} />)}
      </div>
      <div className="mt-1 flex justify-between text-xs font-bold tabular">
        {parts.map((x) => (
          <span key={x.label} className={x.text}>{Math.round((100 * x.v) / n)} % {x.label}</span>
        ))}
      </div>
    </div>
  );
}
