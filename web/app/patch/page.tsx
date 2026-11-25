import type { Metadata } from "next";
import { ArrowDown, ArrowUp, Equal, Hand, Maximize2, Swords, Target, Timer, Trophy } from "lucide-react";
import { SectionTitle, TeamLogo } from "@/components/ui";
import { team } from "@/lib/game";

export const metadata: Metadata = {
  title: "Notes de patch",
  description: "Rééquilibrage des stats d'équipes (Force, Précision, Réflexe, taille des animaux) et jauge SUPER gardée à la mi-temps, avec de nouvelles façons de la remplir.",
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

export default function Page() {
  return (
    <div className="space-y-8">
      <div>
        <span className="chip bg-gold-300/15 text-gold-300">25 novembre 2026</span>
        <h1 className="title-game mt-2 text-4xl">Notes de patch</h1>
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

function TeamCard({ p }: { p: TeamPatch }) {
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
        <span className={`chip ${balance.cls}`} title={`Écart à la cible avant le patch : ${gap > 0 ? "+" : ""}${gap}`}>
          <balance.Icon className="size-3" aria-hidden />
          {balance.label}
        </span>
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
