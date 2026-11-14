# Menu « Mods » (mod)

Bouton « mods » au menu principal (`ID_MODS` dans `Main*.scr`, sous « profil ») → `MenuMods` (`_UI_MODS = 20`).
Réglages globaux (pas par profil), enregistrés par OK dans `game_dev\mods.dat`. Annuler ne garde rien.

## Réglages

| Cadre | Réglage | Effet | Variable |
|---|---|---|---|
| général | Toutes les équipes | équipes débloquées (comme `L111`) | `Cheat.m_bAllTeams` |
| général | Tous les terrains bonus | extras débloqués (comme `L222`) | `Cheat.m_bAllExtras` |
| général | Compteur FPS | affiché en match (comme `LF`) | `SoccerGame.m_bShowFPS` |
| général | Debug (au redémarrage) | mode debug du jeu, pris en compte au lancement suivant | `ModOptions.m_bDebugNextRun` → `SoccerGame.m_bDebug` |
| triche | Vitesse 100 / 110 / 125 / 150 / 200 % | vitesse des équipes humaines | `AIFootBaller.SetLocalSpeedStat` |
| triche | Frappe 100 / 125 / 150 / 200 % | puissance de tir des équipes humaines | `ModDifficulty.GetShotPowerMul` |
| triche | Super : aléatoire / tir / feinte / vitesse / mur | type de super imposé aux équipes humaines | `ModOptions.UpdateTeam` |
| triche | Supers infinis | le super revient 1 s (temps de match) après usage | `ModOptions.UpdateTeam` |

- Les triches ne touchent que les équipes humaines (`Team.m_bUserControled`), donc les deux en multijoueur local.
- Super imposé et supers infinis respectent l'option d'origine « Spéciaux » et la règle « pas de super pendant le but en or ».
- `ModOptions.UpdateTeam` est appelé à chaque `AITeam.Update`.

## Classement
Une triche de match active (valeur ≠ jeu d'origine) rend le match **non classé** : pas d'Elo (`ModElo.IsRatedMatch`),
y compris pour un abandon. Fin de match : « Triche : match non classé » à la place du rang.
Historique : champ `triche` (18e valeur de `stats.dat`), « triche » dans la colonne Elo du Profil.
Les déblocages, le FPS et le debug ne comptent pas comme triche.

## mods.dat
Texte, `Clé=valeur` par ligne : `AllTeams`, `AllExtras`, `ShowFPS`, `Debug`, `Speed`, `ShotPower`, `InfiniteSupers`, `SuperType` (-1 aléatoire, 0..3).
- Lu par `ModOptions.Load` juste après `debugconf.scr` (`SoccerGame.OnPreInitialize`) : une clé présente dans `mods.dat` l'emporte.
  Sans `mods.dat`, les valeurs de `debugconf.scr` s'appliquent comme avant.
- Les codes `L111`, `L222` et `LF` enregistrent aussi `mods.dat` (ils restent des raccourcis du menu).
- Supprimer le fichier = retour aux valeurs de `debugconf.scr`.

## Fichiers
`ModOptions`, `MenuMods`, `MenuMain`, `SoccerMainMenuModule`, `SoccerGame`, `Cheat`, `AITeam`, `AIFootBaller`, `ModDifficulty`, `ModElo`, `ModStats`, `MenuInGame`.
`Mods*.scr` générés par `mods/ui/make_mods_scr.py` (gabarit Options > Match : cases à cocher d'origine + lignes « < > » de l'écran Tactique).
Textes `Mods*`, `MenuMainMods`, `StatsCheat` dans `texts.scr`.
