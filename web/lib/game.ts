// Données du jeu (reprises de l'ancien web.py) et petits formats d'affichage.

export interface Team {
  name: string;
  emoji: string;
  color: string;
  logo: string;
}

export const TEAMS: Record<number, Team> = {
  0: { name: "Bearchester United", emoji: "🐻", color: "#8B5E3C", logo: "T_Logo_Bearchester.png" },
  1: { name: "Bullrusia Dortmund", emoji: "🐂", color: "#FFD700", logo: "T_Logo_Bullrusia.png" },
  2: { name: "Deportivo la Chickeno", emoji: "🐓", color: "#E8A020", logo: "T_Logo_Chickena.png" },
  3: { name: "Dynamo Tiranosaurus", emoji: "🦕", color: "#4CAF50", logo: "T_Logo_Dynamo.png" },
  4: { name: "Frogen Leverkusen", emoji: "🐸", color: "#2ECC71", logo: "T_Logo_Frogen.png" },
  5: { name: "Giraffinaikos", emoji: "🦒", color: "#F4A261", logo: "T_Logo_Giraffinaikos.png" },
  6: { name: "Wisla Gorillakov", emoji: "🦍", color: "#6C757D", logo: "T_Logo_Gorilakov.png" },
  7: { name: "Lyon Lions", emoji: "🦁", color: "#E74C3C", logo: "T_Logo_Lions.png" },
  8: { name: "Ajax Panterdam", emoji: "🐆", color: "#E67E22", logo: "T_Logo_Panterdam.png" },
  9: { name: "Shalke Sharks", emoji: "🦈", color: "#3498DB", logo: "T_Logo_Sharks.png" },
  10: { name: "Fc Turtelona", emoji: "🐢", color: "#1ABC9C", logo: "T_Logo_Turtelona.png" },
  11: { name: "As Robots", emoji: "🤖", color: "#9B59B6", logo: "T_Logo_Robots.png" },
  12: { name: "Real Pandrid", emoji: "🐼", color: "#ECF0F1", logo: "T_Logo_Pandrid.png" },
  13: { name: "Borussia Tigermund", emoji: "🐯", color: "#F39C12", logo: "T_Logo_Tigermund.png" },
};

const UNKNOWN_TEAM: Team = { name: "?", emoji: "❓", color: "#555", logo: "" };

export function team(idx: number | null | undefined): Team {
  if (idx == null) return UNKNOWN_TEAM;
  return TEAMS[idx] ?? { ...UNKNOWN_TEAM, name: `Équipe ${idx}` };
}

// Joueurs de chaque équipe (texts.scr, Team<Animal>Players) : index 0 = gardien, 1..6 = joueurs de champ
export const FOOTBALLERS: Record<number, string[]> = {
  0: ["Beartez", "Silvet", "Cabell", "Owel", "Banc", "Honeys", "Furson"],
  1: ["Laman", "Worm", "Hornler", "Picken", "Biboc", "Dudu", "Buller"],
  2: ["Blufon", "Djalima", "Tistan", "Palbo", "Maycray", "Chickaldo", "Beako"],
  3: ["Shokolovsky", "Hoko", "Dmitrozaur", "Kardazaur", "Shevchoktyl", "Rexbrov", "Jurassov"],
  4: ["Bit", "Ziko", "Frogler", "Kissen", "Dissel", "Backlack", "Greenvald"],
  5: ["Nikolipokis", "Vitanodis", "Binasos", "Georgos", "Zebronis", "Giraffis", "Longneck"],
  6: ["Donkdek", "Gorilec", "Franco", "Kos", "Gulavik", "Rabbit", "Monkeysky"],
  7: ["Pupet", "Papaca", "Roaret", "Brucet", "Ardenson", "Clawinho", "Mane"],
  8: ["Glim", "Mixel", "VarDenVan", "Hopsam", "Pantasek", "VanPan", "Pinker"],
  9: ["Shreck", "Halok", "Valbok", "Nec", "Bierteeff", "Snack", "Sharptooth"],
  10: ["Banano", "Pulol", "Rolado", "Sorgi", "Turtelsson", "DeBro", "Shellbo"],
  11: ["Cajax", "Z-B", "Z-G", "R-M", "Batterystuta", "N-S", "R4"],
  12: ["Bambillas", "Hierpanda", "Bamberto Carlos", "Makebambou", "Zipandane", "Pandigo", "Raulpanda"],
  13: ["Tigermann", "Grrrns", "Rawrhl", "Stripicky", "Felinoso", "Kroller", "Rayuraldo"],
};

export function footballerName(animal: number | null | undefined, num: number | null | undefined): string {
  if (animal == null || num == null) return "?";
  return FOOTBALLERS[animal]?.[num] ?? "?";
}

export const ROLES: Record<string, string> = {
  LIBERO: "Libéro", STOPPEUR: "Stoppeur", DEFENSEUR: "Défenseur", LATERAL: "Latéral",
  MILIEU: "Milieu", AILIER: "Ailier", ATTAQUANT: "Attaquant", PIVOT: "Pivot",
  // jeu d'origine (mod v13) : le moteur ne connaît que défenseurs et attaquants
  ORIGINE_DEF: "Défenseur (origine)", ORIGINE_ATT: "Attaquant (origine)",
};
export const SCHEMES: Record<string, string> = {
  ORIGINE: "Jeu d'origine", EQUILIBRE: "Équilibre", BETON: "Béton", CONTRE: "Contre",
  INDIVIDUEL: "Individuel", ATTAQUE: "Attaque",
};
/** Types de super (Team.m_nSuperType, notes/supers.md). */
export const SUPERS: Record<number, string> = {
  0: "Tir", 1: "Feinte", 2: "Vitesse", 3: "Mur", 4: "Gel", 5: "Ballon fou", 6: "Aimant", 7: "Tir double",
};
export const KINDS: Record<string, string> = { ranked: "Classé", private: "Salon privé", league: "Ligue" };
export type Kind = keyof typeof KINDS;
export function parseKind(v: unknown): string | null {
  return typeof v === "string" && v in KINDS ? v : null;
}

export const SHOT_RESULTS: Record<string, { label: string; color: string }> = {
  goal: { label: "But", color: "#5fd38d" },
  saved: { label: "Arrêté", color: "#f5a623" },
  blocked: { label: "Contré", color: "#a8abb8" },
  missed: { label: "À côté", color: "#ff5a64" },
};

export interface Rank {
  min: number;
  name: string;
  color: string;
  badge: string;
}

export const RANKS: Rank[] = [
  { min: 1900, name: "Challenger", color: "#e654d6", badge: "T_Rank_Challenger.png" },
  { min: 1700, name: "Diamant", color: "#70a6ff", badge: "T_Rank_Diamant.png" },
  { min: 1500, name: "Platine", color: "#78e6cc", badge: "T_Rank_Platine.png" },
  { min: 1300, name: "Or", color: "#ffd700", badge: "T_Rank_Or.png" },
  { min: 1100, name: "Argent", color: "#c0c0c0", badge: "T_Rank_Argent.png" },
  { min: 900, name: "Bronze", color: "#cd7f32", badge: "T_Rank_Bronze.png" },
  { min: 0, name: "Bois", color: "#a67c52", badge: "T_Rank_Bois.png" },
];

export function rankOf(elo: number): Rank {
  return RANKS.find((r) => elo >= r.min) ?? RANKS[RANKS.length - 1];
}

// --- formats ---

export function pct(a: number | null | undefined, b: number | null | undefined): number {
  return b ? Math.round((100 * (a ?? 0)) / b) : 0;
}

/** Score en % : une victoire vaut 1, un nul ½. 50 % = équilibré. */
export function score(w: number, d: number, n: number): number {
  return pct(w + d / 2, n);
}

export function fmtDate(ts: number | null | undefined): string {
  if (!ts) return "?";
  return new Date(ts * 1000).toLocaleString("fr-FR", {
    day: "2-digit", month: "2-digit", year: "numeric", hour: "2-digit", minute: "2-digit", timeZone: "Europe/Paris",
  });
}

export function fmtDay(ts: number | null | undefined): string {
  if (!ts) return "?";
  return new Date(ts * 1000).toLocaleDateString("fr-FR", { day: "numeric", month: "short", timeZone: "Europe/Paris" });
}

export function fmtAgo(ts: number | null | undefined, now = Date.now() / 1000): string {
  if (!ts) return "?";
  const d = now - ts;
  if (d < 60) return `il y a ${Math.max(0, Math.floor(d))} s`;
  if (d < 3600) return `il y a ${Math.floor(d / 60)} min`;
  if (d < 86400) return `il y a ${Math.floor(d / 3600)} h`;
  return `il y a ${Math.floor(d / 86400)} j`;
}

export function fmtDelta(d: number | null | undefined): string {
  if (!d) return "±0";
  return d > 0 ? `+${d}` : `${d}`;
}

/** Heure courante en secondes. Pour les composants serveur rendus à la requête (après `connection()`). */
export function nowSec(): number {
  return Date.now() / 1000;
}
