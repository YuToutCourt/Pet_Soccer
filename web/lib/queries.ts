// Requêtes du site (portage de l'ancien web.py). Les agrégats sont castés en int/float8 : postgres.js renvoie
// bigint et numeric sous forme de chaînes.
import { db } from "./db";

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

export async function getLeaderboard(query: string, limit = 200): Promise<LeaderRow[]> {
  const sql = db();
  const filter = query ? sql`AND p.name ILIKE ${"%" + query + "%"}` : sql``;
  return sql<LeaderRow[]>`
    SELECT p.id, p.name, p.elo, p.games, p.wins, p.draws, p.losses, p.peak,
           extract(epoch FROM p.last_seen)::float8 AS last_seen,
           (SELECT mp.animal FROM match_players mp WHERE mp.player_id = p.id AND mp.animal IS NOT NULL
            GROUP BY mp.animal ORDER BY count(*) DESC LIMIT 1) AS team
    FROM players p WHERE p.games > 0 ${filter}
    ORDER BY p.elo DESC, p.games DESC, p.id LIMIT ${limit}`;
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
  const [[{ n: total }], teams, schemes, roles, matchups, scorers] = await Promise.all([
    sql<{ n: number }[]>`SELECT count(*)::int AS n FROM matches m ${where}`,
    sql<{ animal: number; n: number; w: number; d: number; goals: number | null; possession: number | null }[]>`
      SELECT mp.animal, count(*)::int AS n, sum((mp.result = 'W')::int)::int AS w, sum((mp.result = 'D')::int)::int AS d,
             avg(mp.goals)::float8 AS goals, avg(mp.possession)::float8 AS possession
      FROM match_players mp JOIN matches m ON m.id = mp.match_id ${where} AND mp.animal IS NOT NULL
      GROUP BY mp.animal ORDER BY n DESC`,
    sql<{ scheme: string; n: number; w: number }[]>`
      SELECT mp.scheme, count(*)::int AS n, sum((mp.result = 'W')::int)::int AS w
      FROM match_players mp JOIN matches m ON m.id = mp.match_id ${where} AND mp.scheme IS NOT NULL
      GROUP BY mp.scheme ORDER BY n DESC`,
    sql<{ role: string; n: number; w: number }[]>`
      SELECT r.role, count(*)::int AS n, sum((mp.result = 'W')::int)::int AS w
      FROM match_players mp JOIN matches m ON m.id = mp.match_id, unnest(mp.roles) AS r(role) ${where}
      GROUP BY r.role ORDER BY n DESC`,
    sql<{ a: number; b: number; n: number; w: number }[]>`
      SELECT a.animal AS a, b.animal AS b, count(*)::int AS n, sum((a.result = 'W')::int)::int AS w
      FROM match_players a JOIN match_players b ON b.match_id = a.match_id AND b.side <> a.side
      JOIN matches m ON m.id = a.match_id ${where} AND a.animal IS NOT NULL AND b.animal IS NOT NULL
      GROUP BY a.animal, b.animal`,
    sql<{ animal: number; number: number; goals: number; shots: number; n: number }[]>`
      SELECT mp.animal, f.number, sum(f.goals)::int AS goals, sum(f.shots)::int AS shots, count(*)::int AS n
      FROM match_footballers f JOIN match_players mp ON mp.match_id = f.match_id AND mp.side = f.side
      JOIN matches m ON m.id = f.match_id ${where}
      GROUP BY mp.animal, f.number HAVING sum(f.goals) > 0 ORDER BY goals DESC LIMIT 10`,
  ]);
  return { total, teams, schemes, roles, matchups, scorers };
}
