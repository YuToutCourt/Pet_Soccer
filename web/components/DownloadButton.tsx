import { Download } from "lucide-react";
import { connection } from "next/server";
import { fmtDay } from "@/lib/game";
import { fmtSize, getRelease, RELEASES_URL } from "@/lib/release";

export function DownloadButtonFallback() {
  return (
    <div className="flex flex-col items-start gap-2">
      <a href={RELEASES_URL} className="btn-pill h-16 px-9 text-2xl">
        <Download className="size-6" strokeWidth={3} aria-hidden /> Télécharger le mod
      </a>
      <span className="h-4" />
    </div>
  );
}

/** Gros bouton de téléchargement : dernière Release GitHub (version, date, taille, téléchargements). */
export default async function DownloadButton() {
  await connection();
  const r = await getRelease();
  const details = [
    r.tag && `Version ${r.tag.replace(/^v/i, "")}`,
    r.publishedAt && `du ${fmtDay(r.publishedAt)}`,
    fmtSize(r.size),
    r.downloads != null && `${r.downloads} téléchargement${r.downloads > 1 ? "s" : ""}`,
  ].filter(Boolean);
  return (
    <div className="flex flex-col items-start gap-2">
      <a href={r.downloadUrl} className="btn-pill h-16 px-9 text-2xl">
        <Download className="size-6" strokeWidth={3} aria-hidden /> Télécharger le mod
      </a>
      <span className="text-xs font-semibold text-ink-dim">
        {details.length ? details.join(" · ") : "Dernière version sur GitHub"}
        {" · "}
        <a href={r.pageUrl} className="underline decoration-line-strong underline-offset-2 hover:text-gold-300">
          notes de version
        </a>
      </span>
    </div>
  );
}
