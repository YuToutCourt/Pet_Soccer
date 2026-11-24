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

-- Ligues en ligne (leagues.py) : l'état complet en JSON
CREATE TABLE IF NOT EXISTS leagues (
    id integer PRIMARY KEY,
    code text NOT NULL,
    closed boolean NOT NULL DEFAULT false,
    created double precision NOT NULL,
    updated double precision NOT NULL,
    data jsonb NOT NULL
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
