import Link from "next/link";

export default function NotFound() {
  return (
    <div className="flex flex-col items-center py-20 text-center">
      <div className="anim-float text-7xl" aria-hidden>⚽</div>
      <h1 className="title-game mt-6 text-4xl">Hors-jeu !</h1>
      <p className="mt-2 text-ink-dim">Cette page n&apos;existe pas, ou ce match et ce joueur sont introuvables.</p>
      <Link href="/" className="btn-pill mt-8 h-12 px-7 text-lg">Retour au vestiaire</Link>
    </div>
  );
}
