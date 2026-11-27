// Suggestions de la recherche de joueur (components/PlayerSearch.tsx) : GET /api/players?q=pseudo
import { searchPlayers } from "@/lib/queries";

export async function GET(request: Request) {
  const q = (new URL(request.url).searchParams.get("q") ?? "").slice(0, 32);
  try {
    const players = await searchPlayers(q);
    return Response.json(players, { headers: { "Cache-Control": "no-store" } });
  } catch {
    return Response.json([], { status: 503 });
  }
}
