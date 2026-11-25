// Dernière version du mod, lue sur les Releases GitHub (gardée 1 h en mémoire).

export const REPO = "YuToutCourt/Pet_Soccer";
export const RELEASES_URL = `https://github.com/${REPO}/releases/latest`;

export interface Release {
  tag: string;
  name: string;
  downloadUrl: string;
  pageUrl: string;
  publishedAt: number | null;
  size: number | null;
  downloads: number | null;
}

const FALLBACK: Release = {
  tag: "", name: "", downloadUrl: RELEASES_URL, pageUrl: RELEASES_URL, publishedAt: null, size: null, downloads: null,
};

interface GhAsset {
  name: string;
  size: number;
  download_count: number;
  browser_download_url: string;
}

let cached: { at: number; value: Release } | null = null;
const TTL = 3600_000;

export async function getRelease(): Promise<Release> {
  if (cached && Date.now() - cached.at < TTL) return cached.value;
  let value = FALLBACK;
  try {
    const res = await fetch(`https://api.github.com/repos/${REPO}/releases/latest`, {
      headers: { Accept: "application/vnd.github+json", "User-Agent": "pet-soccer-web" },
      signal: AbortSignal.timeout(4000),
    });
    if (res.ok) {
      const data = await res.json();
      const assets: GhAsset[] = data.assets ?? [];
      const zip = assets.find((a) => a.name === "PetSoccerMod.zip") ?? assets.find((a) => a.name.endsWith(".zip"));
      value = {
        tag: data.tag_name ?? "",
        name: data.name ?? data.tag_name ?? "",
        downloadUrl: zip?.browser_download_url ?? data.html_url ?? RELEASES_URL,
        pageUrl: data.html_url ?? RELEASES_URL,
        publishedAt: data.published_at ? Date.parse(data.published_at) / 1000 : null,
        size: zip?.size ?? null,
        downloads: assets.reduce((n, a) => n + (a.download_count ?? 0), 0),
      };
    }
  } catch {
    // GitHub injoignable : lien vers la page des releases
  }
  // en cas d'échec, on réessaie dans 5 min plutôt que dans 1 h
  cached = { at: value === FALLBACK ? Date.now() - TTL + 300_000 : Date.now(), value };
  return value;
}

export function fmtSize(bytes: number | null): string {
  if (!bytes) return "";
  return bytes >= 1e6 ? `${(bytes / 1e6).toFixed(0)} Mo` : `${Math.round(bytes / 1e3)} Ko`;
}
