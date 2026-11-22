# Génère l'écran « ligue en ligne » (data/menu/scr/OnlineLeague{640,,1024}.scr), lu par MenuOnlineLeague :
# - cadre « ligue » à gauche : état, code de la ligue (champ de saisie), mon équipe, trois boutons dont
#   MenuOnlineLeague change le texte (créer / rejoindre / équipe, puis équipe / quitter, puis classement / passer /
#   quitter), puis délai d'une journée et durée des mi-temps (« < valeur > », réglables avant la création) ;
# - cadre « joueurs » à droite : 14 places sur deux colonnes (nom à gauche, équipe à droite), une ligne pour la
#   journée et son délai, puis 3 lignes de message ;
# - boutons ID_OK (« lancer », « jouer ») et ID_CANCEL (« retour ») en bas.
# Mêmes briques que l'écran « en ligne » (make_online_scr.py). Voir notes/online-league.md.
# Usage : python mods/ui/make_online_league_scr.py  ->  mods/data/Data/Menu/scr/OnlineLeague*.scr
import os

from make_online_scr import (ACTION_HEIGHT, ACTION_WIDTH, CENTER, LEFT, OUT_DIR, RIGHT, ROW_H, SMALL, block, clone,
                             first, frame, indent_block, read, row, spread_menu, static)

MEMBERS = 14  # ModOnlineLeague._MAX_MEMBERS
MEMBER_ROWS = 7
MSG_ROWS = 3  # MenuOnlineLeague._MSG_ROWS


def build(res):
    src = read("OptionsMatch%s.scr" % res)
    player = read("ChoosePlayer%s.scr" % res)

    header = src[:next(k for k, l in enumerate(src) if l.startswith("Interface("))]
    iface = block(src, 'Interface("UI_OPTIONS_MATCH")', 'Menu("UIMenu","ID_MENU"')[:-1]
    iface = [l.replace("UI_OPTIONS_MATCH", "UI_ONLINE_LEAGUE") for l in iface]
    menu = block(src, 'Menu("UIMenu","ID_MENU"', "}// [UIMenu] ID_MENU")
    menu = [l.replace("&&MenuOK&&", "&&LeagueOnStart&&").replace("&&MenuCancel&&", "&&MenuBack&&") for l in menu]
    menu = spread_menu(menu, res)
    footer = src[next(k for k, l in enumerate(src) if l.strip().startswith("EditTexture(")):]
    frame_src = block(src, 'Static("UIStatic","ID_FRAME"', 'ControlObject("UIComboBox"')
    deco = [first(frame_src, "FrameCornerSize("), first(frame_src, "TextureRect(")]
    jazz = first(frame_src, "Font(")[6:-2]
    trigger = block(src, 'Button("UIButtonFlying","ID_COMBO_TRIGGER"', "}// [UIButtonFlying] ID_COMBO_TRIGGER")
    combo_text = block(src, 'Static("UIStatic","ID_COMBO_TEXT"', "}// [UIStatic] ID_COMBO_TEXT")
    roman = first(combo_text, "Font(")[6:-2]
    value_deco = [first(combo_text, "FrameCornerSize("), first(combo_text, "TextureRect(")]
    button = block(player, 'Button("UIButtonFlying","ID_OK"', "}// [UIButtonFlying] ID_OK")
    edit = block(player, 'Button("UIEdit","ID_EDIT"', "}// [UIEdit] ID_EDIT")

    # cadre gauche (0,38 de large) : positions en fraction du cadre, tailles en fraction de l'écran
    left = [
        static("ID_STATUS", "", SMALL, CENTER, 0.03, 0.12, 0.357, 0.045),
        static("ID_CODE_LABEL", "&&OnlineCode&&", roman, LEFT, 0.06, 0.205, 0.1, ROW_H),
        clone(edit, "UIEdit", "ID_EDIT", "ID_CODE", "", (0.43, 0.205), (0.135, 0.055), center_x=0),
        static("ID_MYTEAM", "", SMALL, CENTER, 0.03, 0.29, 0.357, 0.045),
        clone(button, "UIButtonFlying", "ID_OK", "ID_ACTION1", "&&LeagueOnCreate&&", (0.0, 0.365), (ACTION_WIDTH, ACTION_HEIGHT)),
        clone(button, "UIButtonFlying", "ID_OK", "ID_ACTION2", "&&LeagueOnJoin&&", (0.0, 0.48), (ACTION_WIDTH, ACTION_HEIGHT)),
        clone(button, "UIButtonFlying", "ID_OK", "ID_ACTION3", "&&LeagueOnTeam&&", (0.0, 0.595), (ACTION_WIDTH, ACTION_HEIGHT)),
    ]
    left += row(trigger, value_deco, roman, "DELAY", "&&LeagueOnDelay&&", 0.73)
    left += row(trigger, value_deco, roman, "HALF", "&&OnlineTime&&", 0.85)

    # cadre droit (0,5325 de large) : deux colonnes de 7 joueurs, nom (0,16 de l'écran = 0,3 du cadre) puis équipe
    # (0,09 de l'écran, calée à droite, finit à 0,47 du cadre) ; 0,05 de marge à gauche, sinon le bord du cadre mord
    right = []
    for i in range(MEMBERS):
        col, r = divmod(i, MEMBER_ROWS)
        x = round(0.05 + col * 0.47, 4)
        y = round(0.1 + r * 0.061, 4)
        right.append(static("ID_P%d_NAME" % i, "", SMALL, LEFT, x, y, 0.16, 0.04))
        right.append(static("ID_P%d_TEAM" % i, "", SMALL, RIGHT, round(x + 0.25, 4), y, 0.09, 0.04))
    right.append(static("ID_ROUND", "", SMALL, CENTER, 0.012, 0.545, 0.52, 0.045))
    for r in range(MSG_ROWS):
        right.append(static("ID_MSG%d" % r, "", SMALL, CENTER, 0.012, round(0.64 + r * 0.085, 4), 0.52, 0.045))

    out = header + iface
    out += indent_block(menu, 1) + [""]
    # titre centré à droite du logo du jeu : « Ligue en ligne », centré sur l'écran, mordait dessus
    out += indent_block(static("ID_TITLE", "&&LeagueOnTitle&&", jazz, CENTER, 0.3, 0.1, 0.66, 0.08), 1) + [""]
    out += indent_block(frame("ID_FRAME_LEAGUE", "&&LeagueOnFrame&&", jazz, deco, 0.0375, 0.20833331, 0.38, 0.68833333, left), 1) + [""]
    out += indent_block(frame("ID_FRAME_PLAYERS", "&&LeagueOnPlayers&&", jazz, deco, 0.43, 0.20833331, 0.5325, 0.68833333, right), 1)
    out += footer

    path = os.path.join(OUT_DIR, "OnlineLeague%s.scr" % res)
    open(path, "wb").write("\r\n".join(out).encode("cp1252"))
    print(path)


if __name__ == "__main__":
    for res in ("640", "", "1024"):
        build(res)
