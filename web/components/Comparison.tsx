// Statistiques des deux équipes face à face (page d'un match, direct).
import { SectionTitle } from "@/components/ui";
import { pct } from "@/lib/game";

export interface TeamNumbers {
  name: string;
  possession: number | null;
  shots: number | null;
  passes_ok: number | null;
  passes: number | null;
  recoveries: number | null;
  saves: number | null;
  supers: number | null;
}

export default function Comparison({ host, guest, title = "Statistiques" }: { host: TeamNumbers; guest: TeamNumbers; title?: string }) {
  const rows: [string, number | null, number | null, string?][] = [
    ["Possession", host.possession, guest.possession, " %"],
    ["Tirs", host.shots, guest.shots],
    ["Passes réussies", host.passes_ok, guest.passes_ok],
    ["Précision des passes", pct(host.passes_ok, host.passes), pct(guest.passes_ok, guest.passes), " %"],
    ["Récupérations", host.recoveries, guest.recoveries],
    ["Arrêts", host.saves, guest.saves],
    ["Supers", host.supers, guest.supers],
  ];
  return (
    <div className="card p-5">
      <SectionTitle>{title}</SectionTitle>
      <div className="mb-3 flex items-center justify-between text-sm font-bold">
        <span className="flex items-center gap-2"><i className="size-2.5 rounded-full bg-gold-300" />{host.name}</span>
        <span className="flex items-center gap-2">{guest.name}<i className="size-2.5 rounded-full bg-diamant" /></span>
      </div>
      <div className="space-y-3.5">
        {rows.map(([label, a, b, unit = ""]) => {
          const tot = (a ?? 0) + (b ?? 0);
          const pa = tot ? ((a ?? 0) / tot) * 100 : 50;
          return (
            <div key={label}>
              <div className="mb-1 flex items-baseline justify-between text-sm">
                <span className={`font-extrabold tabular ${(a ?? 0) > (b ?? 0) ? "text-gold-100" : "text-ink-dim"}`}>{a ?? "—"}{a != null && unit}</span>
                <span className="label">{label}</span>
                <span className={`font-extrabold tabular ${(b ?? 0) > (a ?? 0) ? "text-[#cfe0ff]" : "text-ink-dim"}`}>{b ?? "—"}{b != null && unit}</span>
              </div>
              <div className="flex h-2 gap-0.5 overflow-hidden rounded-full bg-black/30" title={`${label} : ${a ?? 0} contre ${b ?? 0}`}>
                <span className="rounded-full bg-gradient-to-r from-gold-700 to-gold-300" style={{ width: `${pa}%` }} />
                <span className="flex-1 rounded-full bg-gradient-to-r from-diamant to-[#3b6fd8]" />
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
