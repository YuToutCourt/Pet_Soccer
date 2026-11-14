# Succès (mod)

39 succès par profil, calculés à partir de l'historique des matchs (`stats.dat`, voir `stats.md`) et de l'Elo.
Ils comptent les mêmes matchs que l'historique (humain contre l'IA, hors démo et multijoueur local),
**sauf** les matchs joués avec une triche du menu Mods. Un abandon compte comme une défaite (il casse les séries).

## Où les voir
- **Annonce** : en fin de match, un bandeau « Succès débloqué ! » apparaît en haut de l'écran des résultats (0,8 s après),
  avec un son (`Sound` dans `achievements.scr`). S'il y en a plusieurs, il passe au suivant toutes les 3 s (« 1/3 »).
- **Liste** : Profil > bouton « succès » → `MenuAchievements` (`_UI_ACHIEVEMENTS = 21`), 12 succès par page,
  « suivant » change de page, « retour » revient au Profil.
  Débloqué : nom doré + date (« 10 nov. »). Bloqué : grisé + progression (« 16/100 ») quand le seuil est un compte.

## Calcul
`ModAchievements.GetProgress` parcourt tout l'historique du profil et donne une valeur par condition
(meilleure valeur sur un match, ou total / série sur l'historique). Un succès est atteint quand valeur ≥ seuil.
- Fin de match : `ModStats.OnMatchEnd` → `ModAchievements.OnMatchEnd` → `Evaluate(true)` : les nouveaux vont dans le bandeau.
- Menu principal (`MenuMain.Show`) et écran Succès : `Evaluate(false)`, déblocage **sans annonce**.
  C'est le rattrapage : l'historique joué avant le mod (ou après suppression de `achievements.dat`) débloque ce qu'il doit.
  Au premier lancement, « yu » a eu 13 succès ainsi.

Champs ajoutés à `stats.dat` pour les succès (19e à 23e valeurs, 0 dans les anciennes lignes) :

| Champ | Contenu | Accroche |
|---|---|---|
| `_MAX_DEFICIT` | plus grand retard au score | `ModStats.OnGoal` |
| `_GOLDEN_GOAL` | le match est allé au but en or | `AIMatch.m_bGoldGoal` |
| `_WINNER_MINUTE` | minute du but de la victoire = le (buts encaissés + 1)e but marqué | `AddRecord` |
| `_SUPER_TYPES` | types de supers utilisés (bit n = super n, 0..7) | `AIFootBaller.UseSuper` → `ModStats.OnSuper` |
| `_CUP_WON` | finale de coupe gagnée : numéro de la coupe + 1 | `Championships.m_nPhase == 3` à la fin du match |
| `_SURVIVAL_WAVE` | manche de survie jouée (condition `SurvivalWaves` = plus haute manche réussie) | `ModSurvival.GetWave` |

La coupe est détectée à la fin du match, pas dans `Championships.ReportMatchEnd`, qui ne tourne qu'au retour au menu
(après l'écran de fin de match, donc trop tard pour le bandeau). `WinChampionships` (triche `L` + code) ne compte pas.

## achievements.scr
`Achievement(clé, condition, seuil)`. Nom et description dans `texts.scr` : `Ach_<clé>`, `Ach_<clé>_Desc`.
La clé est aussi celle de `achievements.dat` : la renommer fait perdre le déblocage. L'ordre des lignes = l'ordre de l'écran.
Liste des conditions en tête du fichier. `WinLevel`, `CleanSheetLevel`, `GoldenGoal`, `LateWinner` et `Rank`
n'affichent pas de progression (le seuil n'est pas un compte).

## achievements.dat
Texte, `profil;clé;date` (date `jj/mm/aa`), une ligne par succès débloqué. Supprimer le fichier = tout se re-débloque
sans annonce au prochain affichage du menu, à la date du jour.

## Fichiers
`ModAchievements`, `MenuAchievements`, `ModStats`, `MenuInGame`, `MenuProfile`, `MenuMain`, `SoccerMainMenuModule`,
`AIFootBaller`, `ModElo` (`GetPeakRank`).
`Achievements*.scr` : `mods/ui/make_achievements_scr.py`. Bouton « succès » de `Profile*.scr` : `mods/ui/make_profile_scr.py`.
Bandeau `ID_ACH_*` de `InGame*.scr` : `mods/ui/make_achievement_banner_scr.py` (relançable, remplace son bloc).

## Pièges
- `GetTopScorerGoals` comptait les buts au buteur inconnu (numéro -1) comme ceux d'un seul joueur : un « triplé à 8 buts »
  dans l'historique de « yu » vient sans doute de là. Corrigé pour les matchs suivants ; les anciennes lignes restent telles quelles.
- Pas de place pour un 7e bouton au menu principal (il chevaucherait les icônes options / quitter) : la liste passe par le Profil.
