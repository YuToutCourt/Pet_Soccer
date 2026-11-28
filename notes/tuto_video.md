# Vidéo tuto : installer le mod Pet Soccer

Storyboard pour OpenFilm (https://github.com/openfilm/openfilm). Format 1920x1080, environ 2 min,
sous-titres en français, musique douce en fond (volume bas), zoom et flèche sur chaque clic important.

Rushs à enregistrer (Win+Alt+R ou OBS), un fichier par plan, dans `notes/tuto_rushs/` :

| # | Rush | Ce qu'on voit | Texte à l'écran / voix off |
|---|------|---------------|----------------------------|
| 0 | (aucun, carte titre) | Logo du mod + titre | « Installer le mod Pet Soccer » |
| 1 | `01_site.mp4` | Arrivée sur le site, clic sur **Télécharger** | « Va sur le site du mod et clique sur Télécharger. » |
| 2 | `02_dossier.mp4` | Création d'un dossier **Pet Soccer**, le zip glissé dedans, clic droit > **Extraire tout** | « Crée un dossier Pet Soccer, mets le zip dedans et extrais-le. » |
| 3 | `03_contenu.mp4` | Le dossier extrait : l'**ISO** et le dossier **mod** (zoom) | « Dedans : l'image du jeu (ISO) et le dossier du mod. » |
| 4 | `04_monter.mp4` | Clic droit sur l'ISO > **Monter** | « Clic droit sur l'ISO, puis Monter. » |
| 5 | `05_install_jeu.mp4` | Lancement de l'installateur du jeu, choix du dossier, fin de l'installation | « Lance l'installateur et installe le jeu où tu veux. Retiens ce dossier ! » |
| 6 | `06_installer_mod.mp4` | Dossier **mod** > double-clic **Installer.bat** (si SmartScreen : « Informations complémentaires » > « Exécuter quand même ») | « Ouvre le dossier mod et lance Installer. » |
| 7 | `07_choix_dossier.mp4` | Fenêtre de choix : dossier d'installation du jeu, sélection du sous-dossier **game** (encadré rouge) | « Choisis le dossier où tu as installé le jeu, et sélectionne bien **game**. » |
| 8 | `08_jouer.mp4` | **Jouer.bat** / raccourci **Pet Soccer (mod)** sur le bureau, fenêtre de mise à jour, menu du jeu avec la version | « Lance le raccourci Pet Soccer (mod) : il met le mod à jour puis lance le jeu. Passe toujours par lui. » |
| 9 | (aucun, carte fin) | Adresse du site + Discord | « Bon match ! » |

Prompt à donner à l'agent OpenFilm :

> Fais une vidéo tuto de 2 minutes à partir des rushs de notes/tuto_rushs, dans l'ordre du storyboard
> notes/tuto_video.md. Coupe les temps morts, accélère les barres de progression (installation du jeu),
> zoome sur chaque clic, ajoute une flèche ou un encadré sur l'élément à cliquer, les sous-titres du
> tableau, une carte titre et une carte de fin. Musique douce et discrète.
