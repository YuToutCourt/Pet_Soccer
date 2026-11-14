# Feuille de route — suite des mods

Déjà fait : difficulté 8-10, Elo + rangs, équipes Panda et Tigre, rôles / schémas / écran « Tactique », stats + Profil, menu « Mods », power-ups, succès, survie, mode Ligue.

Coût : ★ = quelques heures, ★★ = une session, ★★★ = plusieurs sessions, ★★★★ = gros chantier.

## Ordre proposé

| # | Chantier | Coût | Dépend de |
|---|---|---|---|
| 1 | ~~Stats de fin de match + historique du profil~~ — fait (`notes/stats.md`) | ★★ | — |
| 2 | ~~Menu « Mods » (remplace les codes clavier)~~ — fait (`notes/mods-menu.md`) | ★★ | — |
| 3 | ~~Nouveaux power-ups : gel, ballon fou, aimant, tir double~~ — fait (`ModSupers`, `supers.scr`) | ★★★ | — |
| 4 | ~~Succès~~ — fait et validé (`notes/achievements.md`) | ★★ | 1 |
| 5 | ~~Survie~~ — fait et validé (`notes/survival.md`) | ★★ | 1 |
| 6 | ~~Mode Ligue : ligue à 14 + playoffs avec loser bracket~~ — fait (`notes/league.md`) | ★★★ | 1 |
| 7 | Multijoueur en ligne — phase 5 sur 5 faite (liaison directe, installeur pour les amis, file d'attente par Elo, pause de mi-temps), phases 2 à 5 à valider en jeu (`notes/online.md`) | ★★★★ | tout le reste figé |

Carrière et défis : abandonnés (2026-11-10).

## 1. Stats de fin de match + historique

- Compteurs pendant le match : buts par joueur, tirs, tirs cadrés, possession (base déjà dans `Team`), passes, supers utilisés.
- Écran de fin de match : un panneau de stats en plus du score et du `+Elo`.
- Historique : `matches.dat`, indexé par profil comme `elo.dat` (on ne touche pas `settings.dat`). Date, mode, adversaire, score, difficulté, variation d'Elo.
- Accroche : `AIMatch.AfterMatch()`.

## 2. Menu « Mods »

- Bouton au menu principal, écran à cases et curseurs : équipes débloquées, FPS, mode debug, triche (vitesse, supers infinis…).
- Réglages sauvegardés dans `mods.dat`. Les codes clavier restent comme raccourci.

## 3. Power-ups

| Super | Effet |
|---|---|
| Gel | L'équipe adverse est ralentie pendant N s |
| Ballon fou | Trajectoire du tir qui zigzague |
| Aimant | Le ballon revient vers le porteur / reste collé au pied |
| Tir double | Deux ballons tirés, un seul réel (ou deux tirs enchaînés) |

- Point d'entrée : `FootBaller.UseSuper()`. Durées et probabilités dans un `.scr`.
- `p_ikonki.png` n'a que 4 cases, déjà prises : il faut une nouvelle planche d'icônes HUD.

## 4. Succès

- Exemples : premier triplé, victoire sans encaisser, remontée de 3 buts, 10 victoires d'affilée, battre un Légende, gagner chaque coupe.
- Liste et conditions dans `achievements.scr`, progression dans `achievements.dat`.
- Notification en fin de match + écran de liste.

## 5. Survie

- Matchs enchaînés tant qu'on ne perd pas. Chaque victoire fait monter la difficulté : niveaux 1 → 10, puis au-delà les stats de l'IA augmentent.
- Les multiplicateurs `AISpeedMul` / `AIShotPowerMul` existent déjà (désactivés en jeu normal) : on les réactive **seulement** en survie.
- Record personnel enregistré dans l'historique.

## 6. Mode Ligue : ligue + playoffs

Fait en **mode séparé** (bouton « ligue ») : le championnat d'origine (7 coupes, poules) est gardé tel quel. Détails dans `notes/league.md`.

- 14 équipes, round robin en aller simple (13 journées), puis les 8 premiers en playoffs à double élimination (loser bracket).
- Nombre de qualifiés réglable dans un `.scr` (8 par défaut), pour suivre si on ajoute des équipes.
- Matchs IA contre IA simulés à partir de `team_stats.scr` au lieu du hasard pur : le classement est visible, il doit être crédible.
- Égalité en élimination : but en or (existe déjà). Pas de tirs au but.
- Nouveaux écrans : classement à 14 lignes, tableau winner / loser bracket.
- ~~Le nouveau format remplace `Championships`~~ : abandonné, les deux coexistent.

## 7. Multijoueur en ligne

But : un serveur auquel on se connecte pour jouer contre de vrais joueurs, entre amis puis en public. Détails et avancement dans `notes/online.md`.

- Hôte autoritaire : le jeu d'un des deux joueurs simule le match (UDP, `java.nio`). L'autre est une « marionnette » qui envoie ses touches et applique l'état reçu.
- Serveur Python (`server\server.py`) : salons à code, relais des paquets, partie rapide par Elo, comptes et Elo en ligne (SQLite). Sur le PC ; le VPS est écarté (2026-11-14).
- Phases : 0 faisabilité (fait), 1 match en direct sans serveur (fait, validé), 2 match complet (fait, à valider), 3 serveur + salons (fait, à valider), 4 public : partie rapide, Elo en ligne, classement (fait, à valider), 5 finition : liaison directe entre les jeux, installeur pour les amis (`mods\package.ps1`), file d'attente par Elo, pause de 30 s à la mi-temps (fait, à valider).
- Parsec ou Steam Remote Play Together sur le multi local : essai optionnel, sans serveur ni matchmaking.
