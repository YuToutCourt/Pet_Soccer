# Codes de triche (mod `mods/java/SoccerGame.java`)

Dans la version commerciale, `SoccerGame.OnInputKey` collecte encore les touches d'un code
mais `CheckCheatString()` avait été vidée. Le mod la réimplémente en appelant la classe `Cheat` d'origine.

**Saisie** : `L` (démarre le code) → touches du code → **Shift** gauche ou droit (valide).
Un son « bonus » confirme, et le résultat est écrit dans `crash.log` (`[mod] cheat ...`).
Touches utilisables dans un code : `0-9` (rangée du haut ou pavé numérique), `R`, `T`, `F`, `B`, `N`.
Le jeu lit les scancodes physiques (disposition QWERTY) : sur AZERTY, ce sont les mêmes touches physiques.

**Sans clavier** : menu « Mods » du menu principal (`notes/mods-menu.md`), ou dans `debugconf.scr`, `AllTeams(1)` / `AllExtras(1)`
(déclarer `!AllTeams(i)` / `!AllExtras(i)`). `L111`, `L222` et `LF` sont enregistrés dans `mods.dat`, qui l'emporte sur `debugconf.scr`.

| Code | Effet | Où |
|---|---|---|
| `L111` | toutes les équipes débloquées (bascule) | menu principal |
| `L222` | tous les extras / terrains bonus (bascule) | menu principal |
| `L0` `L1` `L2` | super tir / super saut / super vitesse à l'équipe gauche | en match |
| `LR0` `LR1` `LR2` | idem équipe droite | en match |
| `LF` | compteur FPS | en match |
| `LT` / `LTT` | arrêter / relancer le chrono | en match |
| `LB1`…`LB7` | niveau de difficulté (1..7) | menu ou match |
| `LN<hôte><invité>` | termine le match sur ce score (ex. `LN20`), compte pour l'Elo (mod) | en match |
| `LB8` `LB9` `LB0` | niveaux 8, 9, 10 (mod) — au menu : réglage Expert / Maître / Légende | menu ou match |

Réponses de `Cheat` en polonais : `dziala tylko w menu glownym` = seulement au menu principal,
`dziala tylko w grze` = seulement en match.
