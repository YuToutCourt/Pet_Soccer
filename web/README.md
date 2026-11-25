# Site Pet Soccer Online

Présentation du mod, lien de téléchargement (dernière Release GitHub) et statistiques des matchs en ligne :
classement Elo, matchs, détail d'un match, profil joueur, méta. Next.js 16 (App Router, Cache Components),
Tailwind CSS 4, Recharts. Lit la base Postgres du serveur de jeu en **lecture seule**.

## En local

```sh
docker compose up -d postgres                 # à la racine du dépôt
python server/seed_demo.py                    # base pet_soccer_demo remplie de faux matchs
cd web
npm install
DATABASE_URL=postgresql://pet-soccer:toor@127.0.0.1:54320/pet_soccer_demo npm run dev
```

Variables : `DATABASE_URL` (sans mot de passe possible), `PGPASSWORD`, `SITE_URL` (adresse publique, pour les
images de partage).

## En production

Service `web` de `docker-compose.yml` : image construite depuis ce dossier (`output: "standalone"`), port 8080.

## Repères

- `lib/queries.ts` : toutes les requêtes SQL ; `lib/game.ts` : équipes, joueurs, rangs, formats.
- `lib/release.ts` : version, taille et téléchargements lus sur l'API GitHub (gardés 1 h en mémoire).
- Chaque page affiche une coquille statique et ses données en streaming dans un `<Suspense>`, après `connection()`
  (obligatoire avec Cache Components).
- Logo : `public/img/logo-petsoccer.png` est produit par `mods/art/make_site_logo.py` ; « Online » est du texte SVG
  (`components/LogoOnline.tsx`).
