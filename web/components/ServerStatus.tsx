import { connection } from "next/server";
import { getServerStatus } from "@/lib/queries";

/** Pastille « serveur en ligne » (table server_status, réécrite toutes les 10 s par le serveur de jeu). */
export default async function ServerStatus({ big = false }: { big?: boolean }) {
  await connection();
  const s = await getServerStatus();
  if (!s) {
    return (
      <span className={`inline-flex items-center gap-2 whitespace-nowrap ${big ? "text-sm" : "text-xs"} font-bold text-ink-muted`}>
        <span className="size-2 rounded-full bg-ink-muted" /> Serveur hors ligne
      </span>
    );
  }
  return (
    <span className={`inline-flex items-center gap-2 whitespace-nowrap ${big ? "text-sm" : "text-xs"} font-bold text-win`} title={`${s.queued} en file d'attente`}>
      <span className="anim-pulse size-2 rounded-full bg-win shadow-[0_0_8px_var(--win)]" />
      {s.online} en ligne · {s.live_matches} en match
    </span>
  );
}
