// Requêtes du site (portage de l'ancien web.py). Les agrégats sont castés en int/float8 : postgres.js renvoie
// bigint et numeric sous forme de chaînes.
import { db } from "./db";
import { rankOf, RANKS } from "./game";

// --- types ---

export interface MatchRow {
  id: number;
  played: number;
  kind: string;
  rated: boolean;
  outcome: string;
  forfeit: boolean;
  forfeit_side: number | null;
  score_host: number;
  score_guest: number;
  has_stats: boolean;
  golden_goal: boolean | null;
  duration_s: number | null;
  stadium: number | null;
  room_code: string | null;
  host_id: number | null;
  host_name: string;
  team_host: number | null;
  delta_host: number;
  scheme_host: string | null;
  guest_id: number | null;
  guest_name: string;
  team_guest: number | null;
  delta_guest: number;
  scheme_guest: string | null;
}

export interface LeaderRow {
  id: number;
  pos: number; // place dans tout le classement, même filtré
  name: string;
  elo: number;
  games: number;
  wins: number;
  draws: number;
  losses: number;
  peak: number;
  last_seen: number;
  team: number | null;
}

export interface ServerStatus {
  online: number;
  queued: number;
  live_matches: number;
  updated: number;
}

export interface SidePlayer {
  side: number;
  player_id: number | null;
  name: string;
  animal: number | null;
  goals: number;
  result: string;
  elo_before: number | null;
  elo_after: number | null;
  delta: number;
  scheme: string | null;
  roles: string[] | null;
  layouts: Record<string, [number, number][] | null> | null;
  shots: number | null;
  possession: number | null;
  passes_ok: number | null;
  passes: number | null;
  recoveries: number | null;
  saves: number | null;
  supers: number | null;
  heatmap: number[] | null;
  ball_heatmap: number[] | null;
  heat_depth: number | null;
  heat_lateral: number | null;
}

export interface FootballerRow {
  side: number;
  number: number;
  slot: number | null;
  role: string | null;
  goals: number;
  own_goals: number;
  shots: number;
  passes: number;
  passes_ok: number;
  recoveries: number;
  distance: number;
  avg_depth: number | null;
  avg_lateral: number | null;
}

export interface EventRow {
  seq: number;
  minute: number;
  side: number;
  kind: string;
  number: number | null;
  super_type: number | null;
  pos_depth: number | null;
  pos_lateral: number | null;
  result: string | null;
}

// --- requêtes ---

function matchSelect() {
  return db()`
    SELECT m.id, extract(epoch FROM m.played_at)::float8 AS played, m.kind, m.rated, m.outcome,
           m.outcome = 'forfeit' AS forfeit, m.forfeit_side, m.score_host, m.score_guest, m.has_stats,
           m.golden_goal, m.duration_s, m.stadium, m.room_code,
           h.player_id AS host_id, h.name AS host_name, h.animal AS team_host, h.delta AS delta_host, h.scheme AS scheme_host,
           gu.player_id AS guest_id, gu.name AS guest_name, gu.animal AS team_guest, gu.delta AS delta_guest, gu.scheme AS scheme_guest
    FROM matches m
    JOIN match_players h ON h.match_id = m.id AND h.side = 0
    JOIN match_players gu ON gu.match_id = m.id AND gu.side = 1
  `;
}

export async function getServerStatus(): Promise<ServerStatus | null> {
  try {
    const [row] = await db()<ServerStatus[]>`
      SELECT online, queued, live_matches, extract(epoch FROM updated)::float8 AS updated FROM server_status`;
    if (!row || Date.now() / 1000 - row.updated > 60) return null; // serveur de jeu arrêté
    return row;
  } catch {
    return null;
  }
}

export async function getTotals() {
  const [row] = await db()<{ players: number; matches: number; goals: number }[]>`
    SELECT (SELECT count(*) FROM players WHERE games > 0)::int AS players,
           (SELECT count(*) FROM matches)::int AS matches,
           (SELECT coalesce(sum(score_host + score_guest), 0) FROM matches)::int AS goals`;
  return row;
}

/** elo : [min, max[ d'un rang, null = tous. */
export async function getLeaderboard(query: string, elo: [number, number] | null = null, limit = 200): Promise<LeaderRow[]> {
  const sql = db();
  const filter = query ? sql`AND p.name ILIKE ${"%" + likeEscape(query) + "%"}` : sql``;
  const range = elo ? sql`AND p.elo >= ${elo[0]} AND p.elo < ${elo[1]}` : sql``;
  return sql<LeaderRow[]>`
    SELECT p.id, p.pos, p.name, p.elo, p.games, p.wins, p.draws, p.losses, p.peak,
           extract(epoch FROM p.last_seen)::float8 AS last_seen,
           (SELECT mp.animal FROM match_players mp WHERE mp.player_id = p.id AND mp.animal IS NOT NULL
            GROUP BY mp.animal ORDER BY count(*) DESC LIMIT 1) AS team
    FROM (SELECT *, (row_number() OVER (ORDER BY elo DESC, games DESC, id))::int AS pos FROM players WHERE games > 0) p
    WHERE true ${filter} ${range}
    ORDER BY p.pos LIMIT ${limit}`;
}

/** Nombre de joueurs classés dans chaque rang (clé : nom du rang). */
export async function getRankCounts(): Promise<Record<string, number>> {
  const rows = await db()<{ elo: number; n: number }[]>`SELECT elo, count(*)::int AS n FROM players WHERE games > 0 GROUP BY elo`;
  const out: Record<string, number> = Object.fromEntries(RANKS.map((r) => [r.name, 0]));
  for (const r of rows) out[rankOf(r.elo).name] += r.n;
  return out;
}

/** % et _ sont des jokers de ILIKE : un pseudo « 100% » se cherche tel quel. */
function likeEscape(s: string): string {
  return s.replace(/[\\%_]/g, (c) => "\\" + c);
}

export interface PlayerHit {
  id: number;
  name: string;
  elo: number;
  games: number;
  team: number | null;
}

/** Suggestions de la recherche de joueur : pseudo qui commence par la saisie d'abord, puis qui la contient. */
export async function searchPlayers(query: string, limit = 8): Promise<PlayerHit[]> {
  const q = likeEscape(query.trim());
  if (!q) return [];
  const sql = db();
  return sql<PlayerHit[]>`
    SELECT p.id, p.name, p.elo, p.games,
           (SELECT mp.animal FROM match_players mp WHERE mp.player_id = p.id AND mp.animal IS NOT NULL
            GROUP BY mp.animal ORDER BY count(*) DESC LIMIT 1) AS team
    FROM players p
    WHERE p.name ILIKE ${"%" + q + "%"}
    ORDER BY (p.name ILIKE ${q + "%"}) DESC, (p.games > 0) DESC, p.elo DESC, p.id
    LIMIT ${limit}`;
}

export async function getMatches(kind: string | null, page: number, perPage = 30) {
  const sql = db();
  const where = kind ? sql`WHERE m.kind = ${kind}` : sql``;
  const [{ n: total }] = await sql<{ n: number }[]>`SELECT count(*)::int AS n FROM matches m ${where}`;
  const pages = Math.max(1, Math.ceil(total / perPage));
  const cur = Math.min(Math.max(1, page), pages);
  const rows = await sql<MatchRow[]>`
    ${matchSelect()} ${where} ORDER BY m.played_at DESC LIMIT ${perPage} OFFSET ${(cur - 1) * perPage}`;
  return { rows, total, pages, page: cur };
}

export async function getMatch(id: number) {
  const sql = db();
  const [m] = await sql<MatchRow[]>`${matchSelect()} WHERE m.id = ${id}`;
  if (!m) return null;
  const [sides, footballers, events] = await Promise.all([
    sql<SidePlayer[]>`SELECT * FROM match_players WHERE match_id = ${id} ORDER BY side`,
    sql<FootballerRow[]>`SELECT * FROM match_footballers WHERE match_id = ${id} ORDER BY side, number`,
    sql<EventRow[]>`SELECT * FROM match_events WHERE match_id = ${id} ORDER BY minute, seq`,
  ]);
  return { m, sides, footballers, events };
}

export async function findPlayerByName(name: string): Promise<number | null> {
  const sql = db();
  const [row] = await sql<{ id: number }[]>`
    SELECT id FROM players WHERE name = ${name} ORDER BY games DESC LIMIT 1`;
  if (row) return row.id;
  const [ci] = await sql<{ id: number }[]>`
    SELECT id FROM players WHERE lower(name) = lower(${name}) ORDER BY games DESC LIMIT 1`;
  return ci?.id ?? null;
}

export interface PlayerRow {
  id: number;
  name: string;
  elo: number;
  games: number;
  wins: number;
  draws: number;
  losses: number;
  peak: number;
  created: number;
  last_seen: number;
}

export async function getPlayer(id: number) {
  const sql = db();
  const [p] = await sql<PlayerRow[]>`
    SELECT id, name, elo, games, wins, draws, losses, peak,
           extract(epoch FROM created)::float8 AS created, extract(epoch FROM last_seen)::float8 AS last_seen
    FROM players WHERE id = ${id}`;
  if (!p) return null;
  const [matches, rankRow, rated, teams, schemes, averages, form] = await Promise.all([
    sql<MatchRow[]>`${matchSelect()} WHERE h.player_id = ${id} OR gu.player_id = ${id} ORDER BY m.played_at DESC LIMIT 20`,
    sql<{ pos: number; total: number }[]>`
      SELECT (1 + count(*) FILTER (WHERE elo > ${p.elo}))::int AS pos, count(*)::int AS total
      FROM players WHERE games > 0`,
    sql<{ id: number; played: number; elo_before: number; elo_after: number; result: string; opp: string | null }[]>`
      SELECT m.id, extract(epoch FROM m.played_at)::float8 AS played, mp.elo_before, mp.elo_after, mp.result,
             (SELECT o.name FROM match_players o WHERE o.match_id = m.id AND o.side <> mp.side) AS opp
      FROM match_players mp JOIN matches m ON m.id = mp.match_id
      WHERE mp.player_id = ${id} AND m.rated AND mp.elo_before IS NOT NULL
      ORDER BY m.played_at DESC LIMIT 60`,
    sql<{ animal: number; n: number; w: number }[]>`
      SELECT animal, count(*)::int AS n, sum((result = 'W')::int)::int AS w FROM match_players
      WHERE player_id = ${id} AND animal IS NOT NULL GROUP BY animal ORDER BY n DESC LIMIT 6`,
    sql<{ scheme: string; n: number; w: number }[]>`
      SELECT scheme, count(*)::int AS n, sum((result = 'W')::int)::int AS w FROM match_players
      WHERE player_id = ${id} AND scheme IS NOT NULL GROUP BY scheme ORDER BY n DESC LIMIT 6`,
    sql<{
      n: number; shots: number | null; possession: number | null; passes_ok: number | null; passes: number | null;
      recoveries: number | null; saves: number | null; goals: number | null; max_goals: number | null;
    }[]>`
      SELECT count(*)::int AS n, avg(shots)::float8 AS shots, avg(possession)::float8 AS possession,
             sum(passes_ok)::int AS passes_ok, sum(passes)::int AS passes, avg(recoveries)::float8 AS recoveries,
             avg(saves)::float8 AS saves, avg(goals)::float8 AS goals, max(goals)::int AS max_goals
      FROM match_players WHERE player_id = ${id} AND shots IS NOT NULL`,
    sql<{ result: string; id: number }[]>`
      SELECT mp.result, m.id FROM match_players mp JOIN matches m ON m.id = mp.match_id
      WHERE mp.player_id = ${id} ORDER BY m.played_at DESC LIMIT 10`,
  ]);
  rated.reverse();
  const history = rated.length
    ? [
        { i: 0, elo: rated[0].elo_before, played: rated[0].played, id: null as number | null, result: null as string | null, opp: null as string | null },
        ...rated.map((r, i) => ({ i: i + 1, elo: r.elo_after, played: r.played, id: r.id, result: r.result, opp: r.opp })),
      ]
    : [];
  return {
    p, matches, rankPos: rankRow[0].pos, totalPlayers: rankRow[0].total, history, teams, schemes,
    averages: averages[0], form,
  };
}

export async function getMeta(kind: string | null) {
  const sql = db();
  const where = kind ? sql`WHERE m.kind = ${kind}` : sql`WHERE true`;
  const [[{ n: total }], teams, schemes, roles, matchups, scorers, supers] = await Promise.all([
    sql<{ n: number }[]>`SELECT count(*)::int AS n FROM matches m ${where}`,
    sql<{ animal: number; n: number; w: number; d: number; goals: number | null; conceded: number | null; supers: number | null; possession: number | null }[]>`
      SELECT mp.animal, count(*)::int AS n, sum((mp.result = 'W')::int)::int AS w, sum((mp.result = 'D')::int)::int AS d,
             avg(mp.goals)::float8 AS goals, avg(o.goals)::float8 AS conceded, avg(mp.supers)::float8 AS supers,
             avg(mp.possession)::float8 AS possession
      FROM match_players mp JOIN matches m ON m.id = mp.match_id
      JOIN match_players o ON o.match_id = mp.match_id AND o.side <> mp.side
      ${where} AND mp.animal IS NOT NULL
      GROUP BY mp.animal ORDER BY n DESC`,
    sql<{ scheme: string; n: number; w: number; d: number }[]>`
      SELECT mp.scheme, count(*)::int AS n, sum((mp.result = 'W')::int)::int AS w, sum((mp.result = 'D')::int)::int AS d
      FROM match_players mp JOIN matches m ON m.id = mp.match_id ${where} AND mp.scheme IS NOT NULL
      GROUP BY mp.scheme ORDER BY n DESC`,
    sql<{ role: string; n: number; w: number; d: number }[]>`
      SELECT r.role, count(*)::int AS n, sum((mp.result = 'W')::int)::int AS w, sum((mp.result = 'D')::int)::int AS d
      FROM match_players mp JOIN matches m ON m.id = mp.match_id, unnest(mp.roles) AS r(role) ${where}
      GROUP BY r.role ORDER BY n DESC`,
    sql<{ a: number; b: number; n: number; w: number; d: number }[]>`
      SELECT a.animal AS a, b.animal AS b, count(*)::int AS n, sum((a.result = 'W')::int)::int AS w, sum((a.result = 'D')::int)::int AS d
      FROM match_players a JOIN match_players b ON b.match_id = a.match_id AND b.side <> a.side
      JOIN matches m ON m.id = a.match_id ${where} AND a.animal IS NOT NULL AND b.animal IS NOT NULL
      GROUP BY a.animal, b.animal`,
    sql<{ animal: number; number: number; goals: number; shots: number; n: number }[]>`
      SELECT mp.animal, f.number, sum(f.goals)::int AS goals, sum(f.shots)::int AS shots, count(*)::int AS n
      FROM match_footballers f JOIN match_players mp ON mp.match_id = f.match_id AND mp.side = f.side
      JOIN matches m ON m.id = f.match_id ${where}
      GROUP BY mp.animal, f.number HAVING sum(f.goals) > 0 ORDER BY goals DESC LIMIT 10`,
    // Supers : utilisations (fil du match) et résultat de l'équipe qui l'a lancé (un match compte une fois par équipe)
    sql<{ super_type: number; uses: number; n: number; w: number; d: number }[]>`
      WITH u AS (
        SELECT e.match_id, e.side, e.super_type, count(*)::int AS uses
        FROM match_events e JOIN matches m ON m.id = e.match_id ${where} AND e.kind = 'super' AND e.super_type IS NOT NULL
        GROUP BY e.match_id, e.side, e.super_type
      )
      SELECT u.super_type, sum(u.uses)::int AS uses, count(*)::int AS n,
             sum((mp.result = 'W')::int)::int AS w, sum((mp.result = 'D')::int)::int AS d
      FROM u JOIN match_players mp ON mp.match_id = u.match_id AND mp.side = u.side
      GROUP BY u.super_type ORDER BY uses DESC`,
  ]);
  return { total, teams, schemes, roles, matchups, scorers, supers };
}

// --- Cartes des tirs (méta) ---
// Repère du tireur : profondeur 0..1 (0 = son but), latéral 0..1. Les tirs partent de la moitié adverse :
// grille de SHOT_D cases sur la profondeur 0,5..1 et SHOT_L cases sur la largeur.
export const SHOT_D = 15;
export const SHOT_L = 12;
export const BOX_DEPTH = 0.159;
export const BOX_WIDTH = 0.49;

export interface ShotCell { d: number; l: number; shots: number; goals: number }
export interface ShotZone { zone: "box" | "front" | "far"; shots: number; goals: number }
export interface ShotMap { cells: ShotCell[]; zones: ShotZone[] }

/** Tirs de toutes les équipes, ceux d'une équipe, ou ceux de ses adversaires (against). */
export async function getShotMap(kind: string | null, animal: number | null = null, against = false): Promise<ShotMap> {
  const sql = db();
  const kindF = kind ? sql`AND m.kind = ${kind}` : sql``;
  const teamF =
    animal == null
      ? sql``
      : against
        ? sql`AND EXISTS (SELECT 1 FROM match_players o WHERE o.match_id = e.match_id AND o.side <> e.side AND o.animal = ${animal})`
        : sql`AND EXISTS (SELECT 1 FROM match_players o WHERE o.match_id = e.match_id AND o.side = e.side AND o.animal = ${animal})`;
  // surface de réparation du jeu (mini-carte du HUD, Data/Menu/P_Menu.png) : 16 % de la longueur, moitié de la largeur.
  // Pas de petite surface. « Devant la surface » : jusqu'à 30 % de la longueur depuis le but.
  const rows = await sql<{ d: number; l: number; zone: ShotZone["zone"]; shots: number; goals: number }[]>`
    SELECT least(${SHOT_D - 1}, greatest(0, floor((e.pos_depth - 0.5) * ${SHOT_D * 2})))::int AS d,
           least(${SHOT_L - 1}, greatest(0, floor(e.pos_lateral * ${SHOT_L})))::int AS l,
           CASE WHEN e.pos_depth >= ${1 - BOX_DEPTH} AND abs(e.pos_lateral - 0.5) <= ${BOX_WIDTH / 2} THEN 'box'
                WHEN e.pos_depth >= 0.7 THEN 'front'
                ELSE 'far' END AS zone,
           count(*)::int AS shots, sum((e.result = 'goal')::int)::int AS goals
    FROM match_events e JOIN matches m ON m.id = e.match_id
    WHERE e.kind = 'shot' AND e.pos_depth IS NOT NULL AND e.pos_lateral IS NOT NULL ${kindF} ${teamF}
    GROUP BY 1, 2, 3`;
  const cells = new Map<string, ShotCell>();
  const zones = new Map<string, ShotZone>((["box", "front", "far"] as const).map((z) => [z, { zone: z, shots: 0, goals: 0 }]));
  for (const r of rows) {
    const k = `${r.d}-${r.l}`;
    const c = cells.get(k) ?? { d: r.d, l: r.l, shots: 0, goals: 0 };
    c.shots += r.shots;
    c.goals += r.goals;
    cells.set(k, c);
    const z = zones.get(r.zone)!;
    z.shots += r.shots;
    z.goals += r.goals;
  }
  return { cells: [...cells.values()], zones: [...zones.values()] };
}

// --- Fiche d'une équipe (méta) ---
export async function getTeamMeta(animal: number, kind: string | null) {
  const sql = db();
  const kindF = kind ? sql`AND m.kind = ${kind}` : sql``;
  const [[{ n: total }], ranking, [avg], opponents, footballers, supers, schemes, heat, minutes, recent, shotsFor, shotsAgainst, possession] =
    await Promise.all([
      sql<{ n: number }[]>`SELECT count(*)::int AS n FROM matches m WHERE true ${kindF}`,
      sql<{ animal: number; n: number; w: number; d: number }[]>`
        SELECT mp.animal, count(*)::int AS n, sum((mp.result = 'W')::int)::int AS w, sum((mp.result = 'D')::int)::int AS d
        FROM match_players mp JOIN matches m ON m.id = mp.match_id
        WHERE mp.animal IS NOT NULL ${kindF} GROUP BY mp.animal`,
      sql<{
        n: number; w: number; d: number; l: number; goals: number | null; conceded: number | null;
        shots: number | null; shots_against: number | null; possession: number | null; passes_ok: number | null;
        passes: number | null; recoveries: number | null; saves: number | null; supers: number | null;
        clean: number; scored: number; max_goals: number | null; best_diff: number | null;
      }[]>`
        SELECT count(*)::int AS n, sum((mp.result = 'W')::int)::int AS w, sum((mp.result = 'D')::int)::int AS d,
               sum((mp.result = 'L')::int)::int AS l, avg(mp.goals)::float8 AS goals, avg(o.goals)::float8 AS conceded,
               avg(mp.shots)::float8 AS shots, avg(o.shots)::float8 AS shots_against, avg(mp.possession)::float8 AS possession,
               sum(mp.passes_ok)::int AS passes_ok, sum(mp.passes)::int AS passes, avg(mp.recoveries)::float8 AS recoveries,
               avg(mp.saves)::float8 AS saves, avg(mp.supers)::float8 AS supers,
               sum((o.goals = 0)::int)::int AS clean, sum((mp.goals > 0)::int)::int AS scored,
               max(mp.goals)::int AS max_goals, max(mp.goals - o.goals)::int AS best_diff
        FROM match_players mp JOIN matches m ON m.id = mp.match_id
        JOIN match_players o ON o.match_id = mp.match_id AND o.side <> mp.side
        WHERE mp.animal = ${animal} ${kindF}`,
      sql<{ animal: number; n: number; w: number; d: number; goals: number; conceded: number }[]>`
        SELECT o.animal, count(*)::int AS n, sum((mp.result = 'W')::int)::int AS w, sum((mp.result = 'D')::int)::int AS d,
               avg(mp.goals)::float8 AS goals, avg(o.goals)::float8 AS conceded
        FROM match_players mp JOIN matches m ON m.id = mp.match_id
        JOIN match_players o ON o.match_id = mp.match_id AND o.side <> mp.side
        WHERE mp.animal = ${animal} AND o.animal IS NOT NULL ${kindF}
        GROUP BY o.animal ORDER BY (sum((mp.result = 'W')::int) + 0.5 * sum((mp.result = 'D')::int))::float8 / count(*) DESC, count(*) DESC`,
      sql<{ number: number; role: string | null; n: number; goals: number; shots: number; passes: number; passes_ok: number; recoveries: number }[]>`
        SELECT f.number, mode() WITHIN GROUP (ORDER BY f.role) AS role, count(*)::int AS n,
               sum(f.goals)::int AS goals, sum(f.shots)::int AS shots, sum(f.passes)::int AS passes,
               sum(f.passes_ok)::int AS passes_ok, sum(f.recoveries)::int AS recoveries
        FROM match_footballers f JOIN match_players mp ON mp.match_id = f.match_id AND mp.side = f.side
        JOIN matches m ON m.id = f.match_id
        WHERE mp.animal = ${animal} ${kindF}
        GROUP BY f.number ORDER BY f.number`,
      sql<{ super_type: number; uses: number; n: number; w: number; d: number }[]>`
        WITH u AS (
          SELECT e.match_id, e.side, e.super_type, count(*)::int AS uses
          FROM match_events e JOIN matches m ON m.id = e.match_id
          JOIN match_players mp ON mp.match_id = e.match_id AND mp.side = e.side
          WHERE e.kind = 'super' AND e.super_type IS NOT NULL AND mp.animal = ${animal} ${kindF}
          GROUP BY 1, 2, 3
        )
        SELECT u.super_type, sum(u.uses)::int AS uses, count(*)::int AS n, sum((mp.result = 'W')::int)::int AS w, sum((mp.result = 'D')::int)::int AS d
        FROM u JOIN match_players mp ON mp.match_id = u.match_id AND mp.side = u.side
        GROUP BY u.super_type ORDER BY uses DESC`,
      sql<{ scheme: string; n: number; w: number; d: number }[]>`
        SELECT mp.scheme, count(*)::int AS n, sum((mp.result = 'W')::int)::int AS w, sum((mp.result = 'D')::int)::int AS d
        FROM match_players mp JOIN matches m ON m.id = mp.match_id
        WHERE mp.animal = ${animal} AND mp.scheme IS NOT NULL ${kindF} GROUP BY mp.scheme ORDER BY n DESC`,
      sql<{ i: number; v: number }[]>`
        SELECT h.i::int AS i, avg(h.v)::float8 AS v
        FROM match_players mp JOIN matches m ON m.id = mp.match_id, unnest(mp.heatmap) WITH ORDINALITY AS h(v, i)
        WHERE mp.animal = ${animal} AND mp.heat_depth = 12 AND mp.heat_lateral = 8 ${kindF}
        GROUP BY h.i ORDER BY h.i`,
      // buts contre son camp : side = l'équipe à qui le but profite
      sql<{ period: number; scored: number; conceded: number }[]>`
        SELECT least(5, (e.minute - 1) / 15)::int AS period,
               sum((e.side = mp.side)::int)::int AS scored, sum((e.side <> mp.side)::int)::int AS conceded
        FROM match_events e JOIN matches m ON m.id = e.match_id
        JOIN match_players mp ON mp.match_id = e.match_id AND mp.animal = ${animal}
        WHERE e.kind IN ('goal', 'own_goal') ${kindF}
        GROUP BY 1 ORDER BY 1`,
      sql<MatchRow[]>`
        ${matchSelect()} WHERE (h.animal = ${animal} OR gu.animal = ${animal}) ${kindF}
        ORDER BY m.played_at DESC LIMIT 10`,
      getShotMap(kind, animal),
      getShotMap(kind, animal, true),
      // temps de balle détaillé (matchs méta mesurés par ModMeta.DiagPossession) : l'équipe, puis ses adversaires
      sql<PossessionDiag[]>`
        SELECT (p.side = mp.side) AS mine, count(*)::int AS n,
               avg(p.hold)::float8 AS hold, avg(p.loose)::float8 AS loose, avg(p.gk_hold)::float8 AS gk_hold,
               avg(p.super_time)::float8 AS super_time, avg(p.spells)::float8 AS spells,
               avg(p.spells_dead)::float8 AS spells_dead, avg(p.play)::float8 AS play,
               avg(p.longest)::float8 AS longest, avg(p.game_counter)::float8 AS game_counter
        FROM match_possession p JOIN matches m ON m.id = p.match_id
        JOIN match_players mp ON mp.match_id = p.match_id AND mp.animal = ${animal}
        WHERE true ${kindF}
        GROUP BY 1`,
    ]);
  if (!avg || !avg.n) return null;
  const ranked = ranking.filter((r) => r.n > 0).sort((a, b) => (b.w + b.d / 2) / b.n - (a.w + a.d / 2) / a.n || b.n - a.n);
  const rankPos = ranked.findIndex((r) => r.animal === animal) + 1;
  const max = Math.max(0, ...heat.map((h) => h.v));
  const heatGrid =
    heat.length === 96 && max > 0
      ? Array.from({ length: 12 }, (_, d) => heat.slice(d * 8, (d + 1) * 8).map((h) => Math.round((h.v / max) * 255)))
      : null;
  return {
    total, avg, rankPos, teamsRanked: ranked.length, opponents, footballers, supers, schemes, heatGrid, minutes, recent,
    shotsFor, shotsAgainst,
    possession: {
      mine: possession.find((p) => p.mine) ?? null,
      opp: possession.find((p) => !p.mine) ?? null,
    },
  };
}

// moyennes par match, en secondes de jeu (état 0) ; voir match_possession dans server/schema.sql
export type PossessionDiag = {
  mine: boolean; n: number; hold: number; loose: number; gk_hold: number; super_time: number; spells: number;
  spells_dead: number; play: number; longest: number; game_counter: number;
};

// --- Direct (table live_matches, réécrite toutes les 3 s par le serveur de jeu) ---

export interface LiveTeam {
  shots: number;
  possession: number;
  passes_ok: number;
  passes: number;
  recoveries: number;
  saves: number;
  supers: number;
  goals: { minute: number; number: number; own_goal: boolean }[];
  super_events: { minute: number; type: number }[];
  shot_events: { minute: number; number: number; depth: number; lateral: number; result: string }[];
}

export interface LiveRow {
  key: string;
  kind: string;
  started: number | null;
  updated: number;
  host_id: number | null;
  host_name: string;
  host_animal: number | null;
  host_elo: number | null;
  guest_id: number | null;
  guest_name: string;
  guest_animal: number | null;
  guest_elo: number | null;
  score_host: number;
  score_guest: number;
  minute: number | null; // null : l'hôte a un mod d'avant le direct (v13)
  half: number | null; // 0, 1, 2 = but en or
  half_seconds: number | null;
  stadium: number | null;
  spectators: number;
  stats: { golden_goal: boolean; teams: LiveTeam[] } | null;
  ended: boolean;
  outcome: string | null; // played | forfeit | void
  match_id: number | null; // fiche du match une fois enregistré
}

function liveSelect() {
  // une ligne qui n'a pas été réécrite depuis 30 s vient d'un serveur de jeu arrêté
  return db()`
    SELECT key, kind, extract(epoch FROM started_at)::float8 AS started, extract(epoch FROM updated)::float8 AS updated,
           host_id, host_name, host_animal, host_elo, guest_id, guest_name, guest_animal, guest_elo,
           score_host, score_guest, minute, half, half_seconds, stadium, spectators, stats, ended, outcome, match_id
    FROM live_matches WHERE updated > now() - interval '30 seconds'`;
}

/** Matchs en cours (les mieux classés d'abord), puis ceux qui viennent de finir. */
export async function getLiveMatches(): Promise<LiveRow[]> {
  try {
    return await db()<LiveRow[]>`
      ${liveSelect()} ORDER BY ended, coalesce(host_elo, 0) + coalesce(guest_elo, 0) DESC, started_at DESC`;
  } catch {
    return []; // base d'avant le direct (pas de table live_matches)
  }
}

export async function getLiveMatch(key: string): Promise<LiveRow | null> {
  try {
    const [row] = await db()<LiveRow[]>`${liveSelect()} AND key = ${key}`;
    return row ?? null;
  } catch {
    return null;
  }
}
