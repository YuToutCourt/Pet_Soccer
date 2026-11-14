# Moteur — ChromeEngine (Techland, 2002)

Analyse native faite avec **rea 6.8.0 + Ghidra 12.1.4** sur `ChromeEngine.dll`
(sha256 `96c3db37…e1e4f`, PE x86, PDB d'origine `C:\Project\firezone\ER_SOURCE\CE_Release\ChromeEngine.pdb`, non fourni).
Snapshot rea : `build/rea/ChromeEngine.snapshot`.

## Chaîne de lancement
`PetSoccer.exe` (53 Ko) → lance `PetSoccer.bum` (le vrai exe) → charge `ChromeEngine.dll`,
lit `game.ini` (`JavaGameClassName`), appelle `InitializeGameScript`.
`start.exe` = autorun du CD (lien gspsoftware), inutile pour jouer. Aucune protection détectée.

## Démarrage de la JVM — `FUN_100a62a0`
- Tente d'ouvrir `code.pak`.
  - présent → `-Djava.class.path=code.pak`
  - **absent → `-Djava.class.path=Java/`** (classes en vrac dans `game\Java\`)
- Autres options : `-Djava.compiler=NONE -Xdebug -Xnoagent` (pas de `-Xrunjdwp` : pas de débogage distant).
- `JNI_CreateJavaVM` puis enregistrement des méthodes natives de `GameObject` (`ThisID`, `SetThisID`,
  `RegisterFields`, `StateChanged`, `IgnoreHandler`, `LogLine`…).

→ **Deux façons d'injecter du code** : réécrire `code.pak` (ce que fait `mods/build.ps1`)
ou supprimer `code.pak` et tout extraire dans `Java\`.

## `JavaClassSourcePath` (game.ini) — `FUN_10086070`
Lu dans `game.ini` et stocké dans un tableau global (`DAT_1021ed30`, compteur `DAT_1021ed80`).
Valeurs par défaut sans game.ini : `c:\Project\JaDe\Java\`, `c:\Project\Chrome\CHROMEJAVA\`,
`c:\Project\Chrome\ER_Source\Java\`, `c:\jdk1.3\src\`.
Ce sont des chemins de **sources** (outils de debug / éditeur du moteur), **pas** de chargement de classes.
Inutile pour le modding.

## Côté Java
- `ClassReloader` : charge depuis `Java\<Nom>.class` uniquement les classes dont le nom finit par `Special`.
- `GameObject.ParseFile(path)` (natif) : parseur des `.scr`, retourne une liste chaînée de `ParseKeyword`.
- Méthodes natives appelées par nom/délai : `CallMethodIn(String, float)`, `CallMethodEveryMinMax`…

## Logs utiles (dans le dossier du jeu)
`crash.log` (sortie de `GameObject.CrashLog`), `FXLog.txt`, `TexError.log`.

## Carrousels 3D des menus (`MenuObjects`, `MenuOptionsObjects`)

Les objets du carrousel (coupe, ballon, manette, « ! ») tremblaient sur PC récent. Cause : `PositionElems` prenait l'avant
de chaque élément (`GetElementForwardVector`, déjà en repère monde), le repassait en local (`VectorWorldToLocal`), puis le
donnait à `FromUpForwardPosElementWorld` comme vecteur monde. L'orientation dérivait à chaque frame, d'autant plus vite
que le FPS est élevé. Corrigé dans `mods\java` : l'avant reste en repère monde et est projeté à l'horizontale.
