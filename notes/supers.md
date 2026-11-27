# Supers ajoutés (`ModSupers`)

Types de `Team.m_nSuperType` : 0 tir, 1 feinte, 2 vitesse, 3 mur (jeu d'origine), puis 4 gel, 5 ballon fou,
6 aimant, 7 tir double. Tous les réglages sont dans `data/supers.scr` (tirage, durées, effets).
Icônes HUD : `Data/Menu/P_Ikonki2.png` (128x512, une case de 126x50 par super, générée par
`mods/art/make_super_icons.py`), lues par `HudSuper._MOD_ICON_RECTS`.

## The World (abandonné)

Arrêt du temps façon JoJo (demande du 2026-11-25), implémenté puis **retiré le 2026-11-27** : après un arrêt,
le joueur humain ne pouvait plus faire de passe ni tirer, jugé trop compliqué à corriger. Le code complet
(gel des joueurs et de l'arbitre, ballon figé, frappes relâchées à la reprise, brouillard gris, échos, icône,
son) est dans l'historique git si on veut le reprendre : le type 8 n'existe plus (`ModSupers._TYPES` = 8).
