-- Base Pet Soccer en ligne (Postgres). Lu par store.Store au démarrage du serveur de jeu : chaque
-- instruction doit pouvoir être rejouée sans rien casser (IF NOT EXISTS).
--
-- Côtés d'un match : 0 = hôte du salon (son équipe est à gauche au coup d'envoi, c'est son jeu qui simule),
-- 1 = invité. Positions sur le terrain : profondeur 0 = son propre but, 1 = but adverse ; latéral 0 = haut de
-- l'écran, 1 = bas (même repère que la carte de l'écran Tactique, voir notes/tactics.md).

CREATE TABLE IF NOT EXISTS players (
    id serial PRIMARY KEY,
    token text NOT NULL UNIQUE,               -- empreinte SHA-256 du jeton du jeu (online.dat), jamais le jeton
    name text NOT NULL,
    elo integer NOT NULL DEFAULT 1000,
    games integer NOT NULL DEFAULT 0,         -- matchs classés seulement
    wins integer NOT NULL DEFAULT 0,
    draws integer NOT NULL DEFAULT 0,
    losses integer NOT NULL DEFAULT 0,
    peak integer NOT NULL DEFAULT 1000,
    mod_version integer NOT NULL DEFAULT 0,
    created timestamptz NOT NULL DEFAULT now(),
    last_seen timestamptz NOT NULL DEFAULT now()
);

-- Pseudos portés par chaque compte
CREATE TABLE IF NOT EXISTS player_names (
    player_id integer NOT NULL REFERENCES players (id) ON DELETE CASCADE,
    name text NOT NULL,
    first_seen timestamptz NOT NULL DEFAULT now(),
    last_seen timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (player_id, name)
);

CREATE TABLE IF NOT EXISTS matches (
    id serial PRIMARY KEY,
    played_at timestamptz NOT NULL DEFAULT now(),  -- fin du match (heure du serveur)
    started_at timestamptz,                        -- les deux jeux ont annoncé le match
    duration_s integer,
    kind text NOT NULL,                            -- ranked (file d'attente) | private (salon à code) | league
    rated boolean NOT NULL,                        -- compté dans l'Elo
    outcome text NOT NULL,                         -- played | forfeit
    forfeit_side smallint,                         -- côté forfait, NULL si le match est allé au bout
    room_code text,
    league_id integer,
    league_key integer,
    stadium smallint,                              -- niveau (stade) du match
    half_seconds smallint,                         -- durée d'une mi-temps en secondes réelles
    golden_goal boolean,
    score_host smallint NOT NULL,
    score_guest smallint NOT NULL,
    mod_version integer,
    has_stats boolean NOT NULL DEFAULT false       -- le jeu de l'hôte a envoyé ses statistiques (PKT_MATCH_STATS)
);
CREATE INDEX IF NOT EXISTS matches_played ON matches (played_at DESC);

CREATE TABLE IF NOT EXISTS match_players (
    match_id integer NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    side smallint NOT NULL,
    player_id integer REFERENCES players (id),     -- NULL : jeu sans compte (salon privé)
    name text NOT NULL,
    animal smallint,                               -- équipe (EAnimals, 0..13)
    clothes smallint,
    goals smallint NOT NULL,                       -- buts marqués (le score)
    result char(1) NOT NULL,                       -- W / D / L, forfait compris
    elo_before integer,                            -- NULL si le match n'est pas classé
    elo_after integer,
    delta integer NOT NULL DEFAULT 0,
    -- tactique (ModTactics.ToLine) : schéma, rôle des places 0..5, placements par situation
    scheme text,
    roles text[],
    layouts jsonb,                                 -- {"play": [[profondeur, latéral] x6] | null, "kickoff", "throw", "goalkick"}
    tactic_raw text,
    -- statistiques d'équipe (NULL sans PKT_MATCH_STATS)
    shots smallint,
    possession smallint,                           -- en %
    passes_ok smallint,
    passes smallint,
    recoveries smallint,
    saves smallint,
    supers smallint,
    super_types integer,                           -- bit n : super n utilisé
    max_deficit smallint,
    heatmap smallint[],                            -- grille profondeur x latéral (heat_depth x heat_lateral), 0..255
    ball_heatmap smallint[],                       -- le ballon quand cette équipe l'a
    heat_depth smallint,
    heat_lateral smallint,
    PRIMARY KEY (match_id, side)
);
CREATE INDEX IF NOT EXISTS match_players_player ON match_players (player_id, match_id DESC);
CREATE INDEX IF NOT EXISTS match_players_animal ON match_players (animal);
CREATE INDEX IF NOT EXISTS match_players_scheme ON match_players (scheme);

-- Les 7 joueurs de chaque équipe
CREATE TABLE IF NOT EXISTS match_footballers (
    match_id integer NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    side smallint NOT NULL,
    number smallint NOT NULL,                      -- place dans Team<Animal>Players (texts.scr) : 0 = gardien, 1..6
    slot smallint,                                 -- place 0..5 de la tactique, -1 = gardien ou inconnue
    role text,                                     -- rôle de cette place (NULL : jeu d'origine)
    goals smallint NOT NULL DEFAULT 0,
    own_goals smallint NOT NULL DEFAULT 0,
    shots smallint NOT NULL DEFAULT 0,
    passes smallint NOT NULL DEFAULT 0,
    passes_ok smallint NOT NULL DEFAULT 0,
    recoveries smallint NOT NULL DEFAULT 0,
    distance integer NOT NULL DEFAULT 0,           -- mètres « terrain » (longueur du terrain = 100 m)
    avg_depth real,                                -- position moyenne réelle, à comparer au placement prévu
    avg_lateral real,
    PRIMARY KEY (match_id, side, number)
);

-- Fil du match : buts, supers, tirs
CREATE TABLE IF NOT EXISTS match_events (
    match_id integer NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    seq smallint NOT NULL,
    minute smallint NOT NULL,
    side smallint NOT NULL,                        -- équipe qui marque, tire ou lance le super
    kind text NOT NULL,                            -- goal | own_goal | super | shot
    number smallint,                               -- joueur (voir match_footballers.number), NULL si inconnu
    super_type smallint,
    pos_depth real,                                -- tir : position du tireur
    pos_lateral real,
    result text,                                   -- tir : goal | saved | blocked | missed
    PRIMARY KEY (match_id, seq)
);
CREATE INDEX IF NOT EXISTS match_events_kind ON match_events (kind);

-- Temps de balle détaillé d'une équipe (matchs méta seulement : ModMeta.DiagPossession, mods/meta/meta_run.py).
-- Secondes de match, jeu en cours (état 0) seulement.
CREATE TABLE IF NOT EXISTS match_possession (
    match_id integer NOT NULL REFERENCES matches (id) ON DELETE CASCADE,
    side smallint NOT NULL,
    hold real NOT NULL,                            -- l'équipe tient le ballon
    loose real NOT NULL,                           -- ballon libre après sa touche (passe, tir, dégagement)
    gk_hold real NOT NULL,                         -- dont son gardien tient le ballon
    super_time real NOT NULL,                      -- pendant un de ses supers (le compteur du jeu ne compte rien)
    spells smallint NOT NULL,                      -- phases de jeu : suite de touches de l'équipe
    spells_dead smallint NOT NULL,                 -- dont finies par un arrêt de jeu (sortie, but, faute)
    play real NOT NULL,                            -- temps de jeu du match
    longest real NOT NULL,                         -- sa phase la plus longue
    game_counter real NOT NULL,                    -- compteur du jeu (Team.m_fEntirePossesionTime), d'où vient possession
    PRIMARY KEY (match_id, side)
);

-- Ligues en ligne (leagues.py) : l'état complet en JSON
CREATE TABLE IF NOT EXISTS leagues (
    id integer PRIMARY KEY,
    code text NOT NULL,
    closed boolean NOT NULL DEFAULT false,
    created double precision NOT NULL,
    updated double precision NOT NULL,
    data jsonb NOT NULL
);

-- Matchs en cours (page « En direct » du site), réécrite toutes les 3 s par le serveur de jeu (Lobby.live_rows).
-- Un match fini y reste 5 min (ended, avec son numéro dans matches) ; la table entière est remplacée à chaque écriture.
CREATE TABLE IF NOT EXISTS live_matches (
    key text PRIMARY KEY,                          -- code du salon + numéro du match chez les jeux
    kind text NOT NULL,                            -- ranked | private | league
    started_at timestamptz,
    updated timestamptz NOT NULL,
    host_id integer, host_name text NOT NULL, host_animal smallint, host_elo integer,
    guest_id integer, guest_name text NOT NULL, guest_animal smallint, guest_elo integer,
    score_host smallint NOT NULL,
    score_guest smallint NOT NULL,
    minute smallint,                               -- NULL : le jeu de l'hôte n'envoie pas le direct (mod < v13)
    half smallint,                                 -- 0, 1, 2 = but en or
    half_seconds smallint,
    stadium smallint,
    spectators smallint NOT NULL DEFAULT 0,
    stats jsonb,                                   -- dernier PKT_MATCH_LIVE (protocol.read_match_live), sans heatmaps
    ended boolean NOT NULL DEFAULT false,
    outcome text,                                  -- played | forfeit | void, une fois fini
    match_id integer                               -- numéro dans matches une fois enregistré
);

-- Ce qui se passe sur le serveur de jeu en ce moment (une seule ligne, réécrite toutes les 10 s)
CREATE TABLE IF NOT EXISTS server_status (
    id smallint PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    updated timestamptz NOT NULL,
    online integer NOT NULL,
    queued integer NOT NULL,
    rooms integer NOT NULL,
    live_matches integer NOT NULL
);
