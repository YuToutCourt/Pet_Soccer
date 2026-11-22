@echo off
rem Met le mod de Pet Soccer a jour si une nouvelle version existe, puis lance le jeu. Voir LISEZMOI.txt.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0mod_maj\update.ps1" %*
