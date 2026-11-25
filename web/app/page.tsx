import Image from "next/image";
import Link from "next/link";
import { connection } from "next/server";
import { Suspense } from "react";
import { nowSec } from "@/lib/game";
import { ArrowRight, Brain, Globe2, Medal, Swords, Trophy, Users } from "lucide-react";
import DownloadButton, { DownloadButtonFallback } from "@/components/DownloadButton";
import LogoOnline from "@/components/LogoOnline";
import ServerStatus from "@/components/ServerStatus";
import { Empty, MatchLine, RankBadge, SectionTitle, Skeleton, TeamLogo } from "@/components/ui";
import { getLeaderboard, getMatches, getTotals } from "@/lib/queries";

const FEATURES = [
  {
    icon: Globe2,
    title: "En ligne",
    items: [
      "Salons privés à code de 4 lettres pour jouer entre amis",
      "Partie rapide classée : file d'attente par Elo",
      "Liaison directe entre les jeux, sinon relais par le serveur",
      "Pause de 30 s à la mi-temps pour revoir sa tactique",
    ],
  },
  {
    icon: Trophy,
    title: "Modes de jeu",
    items: [
      "Ligue : 14 équipes puis playoffs à double élimination",
      "Survie : enchaîne les manches, l'IA monte au niveau 10 et au-delà",
      "Les modes d'origine restent là",
    ],
  },
  {
    icon: Brain,
    title: "Gameplay",
    items: [
      "Écran Tactique : rôles, schémas, placements par situation",
      "Appels en profondeur, une-deux, passe dans la course",
      "Difficultés Expert, Maître et Légende",
      "Hors-jeu « doux », stats d'équipes qui comptent vraiment",
    ],
  },
  {
    icon: Medal,
    title: "Progression",
    items: [
      "Classement Elo et 7 rangs, de Bois à Challenger",
      "Stats de match et écran Profil",
      "39 succès à débloquer",
      "Deux nouvelles équipes : Real Pandrid et Borussia Tigermund",
    ],
  },
];

const SCREENS = [
  { src: "match.jpg", label: "Un match en cours" },
  { src: "online.jpg", label: "En ligne : salons et partie rapide" },
  { src: "tactics.jpg", label: "Tactique : rôles et placements" },
  { src: "league.jpg", label: "Ligue : 14 équipes puis playoffs" },
  { src: "profile.jpg", label: "Profil : bilan et derniers matchs" },
  { src: "menu.jpg", label: "Menu principal avec ton rang" },
];

const STEPS = [
  { title: "Télécharge le zip", text: "Le bouton en haut de la page récupère la dernière version." },
  { title: "Décompresse tout", text: "Ferme le jeu, puis extrais tout le zip dans un dossier." },
  { title: "Lance Installer.bat", text: "Il trouve le dossier du jeu (ou te le demande) et garde une copie des fichiers d'origine." },
  { title: "Joue !", text: "Lance le jeu comme d'habitude, puis bouton « en ligne » au menu principal." },
];

export default function Home() {
  return (
    <div className="space-y-20">
      {/* --- Hero --- */}
      <section className="relative grid items-center gap-10 pt-4 lg:grid-cols-[1.05fr_1fr]">
        <div className="anim-rise">
          <div className="hidden sm:block">
            <LogoOnline width={460} priority />
          </div>
          <div className="sm:hidden">
            <LogoOnline width={300} priority />
          </div>
          <h1 className="title-game mt-6 text-3xl leading-tight sm:text-4xl">
            Une seconde vie pour <span className="title-gold">Pet Soccer</span>
          </h1>
          <p className="mt-3 max-w-xl text-lg text-ink-dim">
            Le mod communautaire du jeu de foot animalier de 2002 : matchs en ligne, classement Elo, vraies tactiques,
            mode Ligue, mode Survie, succès et une IA bien plus coriace.
          </p>
          <div className="mt-7 flex flex-wrap items-start gap-4">
            <Suspense fallback={<DownloadButtonFallback />}>
              <DownloadButton />
            </Suspense>
            <Link href="/classement" className="btn-ghost h-16 px-6 font-display text-xl">
              Classement <ArrowRight className="size-5" aria-hidden />
            </Link>
          </div>
        </div>

        <div className="relative anim-rise [animation-delay:120ms]">
          <div className="absolute -inset-6 rounded-[2rem] bg-gold-300/10 blur-3xl" aria-hidden />
          <div className="relative rotate-[1.5deg] overflow-hidden rounded-3xl border-4 border-gold-500/70 shadow-[0_20px_60px_rgba(0,0,0,0.55)]">
            <Image src="/img/screens/match.jpg" alt="Un match en cours dans Pet Soccer Online" width={1024} height={768} priority className="h-auto w-full" />
          </div>
          <div className="absolute -bottom-5 -left-4 -rotate-3 rounded-2xl border border-line-strong bg-card-strong px-4 py-2.5 shadow-xl">
            <Suspense fallback={<span className="text-sm text-ink-muted">Serveur…</span>}>
              <ServerStatus big />
            </Suspense>
          </div>
        </div>
      </section>

      {/* --- Chiffres --- */}
      <Suspense fallback={<Skeleton className="h-24" />}>
        <Totals />
      </Suspense>

      {/* --- Fonctionnalités --- */}
      <section>
        <SectionTitle sub="Tout ce que le mod ajoute au jeu d'origine.">Ce que le mod ajoute</SectionTitle>
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {FEATURES.map((f) => (
            <div key={f.title} className="card p-5">
              <div className="mb-3 grid size-12 place-items-center rounded-2xl bg-gradient-to-b from-gold-100 via-gold-500 to-gold-700 text-[#5a2a00] shadow-[0_3px_0_#8c4a00]">
                <f.icon className="size-6" strokeWidth={2.5} aria-hidden />
              </div>
              <h3 className="title-game text-xl">{f.title}</h3>
              <ul className="mt-2 space-y-1.5 text-sm text-ink-dim">
                {f.items.map((it) => (
                  <li key={it} className="flex gap-2">
                    <span className="mt-1.5 size-1.5 shrink-0 rounded-full bg-gold-300" aria-hidden />
                    {it}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </section>

      {/* --- Captures --- */}
      <section>
        <SectionTitle>En images</SectionTitle>
        <div className="grid grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-3">
          {SCREENS.map((s) => (
            <figure key={s.src} className="group overflow-hidden rounded-2xl border border-line bg-black/30">
              <Image src={`/img/screens/${s.src}`} alt={s.label} width={1024} height={768} sizes="(min-width: 1024px) 33vw, 50vw" className="h-auto w-full transition duration-500 group-hover:scale-[1.03]" />
              <figcaption className="px-3 py-2 text-xs font-semibold text-ink-dim sm:text-sm">{s.label}</figcaption>
            </figure>
          ))}
        </div>
      </section>

      {/* --- Installation --- */}
      <section id="installation">
        <SectionTitle sub="Deux minutes, et tu es sur le terrain.">Installation</SectionTitle>
        <ol className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
          {STEPS.map((s, i) => (
            <li key={s.title} className="card relative p-5 pt-7">
              <span className="btn-pill absolute -top-4 left-5 size-10 text-xl">{i + 1}</span>
              <h3 className="title-game text-lg">{s.title}</h3>
              <p className="mt-1 text-sm text-ink-dim">{s.text}</p>
            </li>
          ))}
        </ol>
        <div className="mt-4 grid gap-3 text-sm text-ink-dim sm:grid-cols-3">
          <p className="rounded-2xl border border-line bg-black/15 p-4">
            <strong className="text-ink">Windows bloque le script ?</strong> « Informations complémentaires » puis « Exécuter quand même ».
            Le script est lisible : ouvre <code className="text-gold-100">install.ps1</code> dans le Bloc-notes.
          </p>
          <p className="rounded-2xl border border-line bg-black/15 p-4">
            <strong className="text-ink">Mise à jour :</strong> relance <code className="text-gold-100">Installer.bat</code> du nouveau zip, tes sauvegardes sont gardées.
          </p>
          <p className="rounded-2xl border border-line bg-black/15 p-4">
            <strong className="text-ink">Désinstallation :</strong> <code className="text-gold-100">Desinstaller.bat</code> remet le jeu exactement comme avant.
          </p>
        </div>
      </section>

      {/* --- Aperçu des stats --- */}
      <section className="grid gap-6 lg:grid-cols-[1fr_1.35fr]">
        <Suspense fallback={<Skeleton className="h-96" />}>
          <TopPlayers />
        </Suspense>
        <Suspense fallback={<Skeleton className="h-96" />}>
          <LatestMatches />
        </Suspense>
      </section>
    </div>
  );
}

async function Totals() {
  await connection();
  const t = await getTotals().catch(() => null);
  if (!t) return null;
  const items = [
    { icon: Users, label: "Joueurs classés", value: t.players },
    { icon: Swords, label: "Matchs joués", value: t.matches },
    { icon: Trophy, label: "Buts marqués", value: t.goals },
  ];
  return (
    <section className="grid gap-3 sm:grid-cols-3">
      {items.map((it) => (
        <div key={it.label} className="card flex items-center gap-4 px-5 py-4">
          <it.icon className="size-8 shrink-0 text-gold-300" aria-hidden />
          <div>
            <div className="font-display text-3xl tabular">{it.value.toLocaleString("fr-FR")}</div>
            <div className="label">{it.label}</div>
          </div>
        </div>
      ))}
    </section>
  );
}

async function TopPlayers() {
  await connection();
  const players = await getLeaderboard("", 5).catch(() => []);
  return (
    <div className="card p-5">
      <SectionTitle right={<Link href="/classement" className="text-sm font-bold text-gold-300 hover:underline">Tout le classement →</Link>}>
        Top 5
      </SectionTitle>
      {players.length === 0 ? (
        <Empty>Pas encore de joueur classé.</Empty>
      ) : (
        <ol className="space-y-2">
          {players.map((p, i) => (
            <li key={p.id}>
              <Link href={`/player/${p.id}`} className="flex items-center gap-3 rounded-2xl border border-line bg-black/15 px-3 py-2.5 transition hover:border-line-strong hover:bg-black/25">
                <span className={`w-6 text-center font-display text-xl ${i === 0 ? "text-gold-300" : i === 1 ? "text-[#d6d6d6]" : i === 2 ? "text-[#cd7f32]" : "text-ink-muted"}`}>{i + 1}</span>
                <RankBadge elo={p.elo} size={34} />
                <span className="min-w-0 flex-1 truncate font-bold">{p.name}</span>
                <TeamLogo animal={p.team} size={26} />
                <span className="w-14 text-right font-display text-lg tabular">{p.elo}</span>
              </Link>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

async function LatestMatches() {
  await connection();
  const res = await getMatches(null, 1, 5).catch(() => null);
  const now = nowSec();
  return (
    <div className="card p-5">
      <SectionTitle right={<Link href="/matches" className="text-sm font-bold text-gold-300 hover:underline">Tous les matchs →</Link>}>
        Derniers matchs
      </SectionTitle>
      {!res || res.rows.length === 0 ? (
        <Empty>Aucun match pour le moment.</Empty>
      ) : (
        <div className="space-y-2">
          {res.rows.map((m) => (
            <MatchLine key={m.id} m={m} now={now} />
          ))}
        </div>
      )}
    </div>
  );
}
