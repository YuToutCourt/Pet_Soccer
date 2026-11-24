"""Ligues en ligne : plusieurs joueurs dans une ligue à 14, les autres équipes jouées par l'IA. Gardées par le
serveur dans la base Postgres (table leagues de schema.sql, elles durent des jours), logique dans league.py. Voir notes/online-league.md.

- Un joueur crée une ligue (code de 4 lettres, réglages tirés de son league.scr) ; ses amis la rejoignent avec ce
  code et prennent chacun une équipe différente. Le créateur la lance : les équipes restantes sont à l'IA.
- Une journée se ferme quand tous les matchs des humains sont joués, ou au bout du délai choisi par le créateur :
  ce qui reste est alors simulé. Le créateur peut aussi la fermer tout de suite (« passer »).
- Match contre l'IA : joué en solo chez le joueur, qui envoie le score (LG_RESULT). Il compte pour l'Elo local du
  jeu, pas pour le serveur. LG_PLAY au coup d'envoi : le délai épargne ce match pendant `grace` secondes.
- Match entre deux joueurs : quand les deux demandent à le jouer (LG_PLAY), rooms.Lobby.league_play ouvre un salon
  entre eux, comme une partie classée : le résultat compte pour l'Elo en ligne et pour la ligue.
Pas de réseau ici : server.py et rooms.py s'en occupent.
"""
import json
import random
import time

import league as L
import protocol as P
from rooms import CODE_LETTERS, CODE_LENGTH

ANIMALS = 14  # EAnimals._ANIMALS_COUNT : toutes les équipes jouent la ligue
WANT_FRESH = 3.0  # secondes pendant lesquelles un LG_PLAY vaut « j'attends l'autre joueur »
DELAYS = (0, 24 * 60 * 30)  # délai d'une journée accepté, en minutes (0 : pas de délai)


class Member:
    def __init__(self, player_id, name, animal, clothes):
        self.player_id = player_id
        self.name = name
        self.animal = animal
        self.clothes = clothes
        self.left = False  # a abandonné après le départ : l'IA joue son équipe

    def to_dict(self):
        return dict(self.__dict__)

    @classmethod
    def from_dict(cls, d):
        m = cls(d["player_id"], d["name"], d["animal"], d["clothes"])
        m.left = d["left"]
        return m


class OnlineLeague:
    def __init__(self, league_id, code, owner, settings, ratings, now):
        self.id = league_id
        self.code = code
        self.owner = owner  # numéro du compte du créateur (passe à un autre membre s'il abandonne)
        self.settings = settings
        self.ratings = ratings
        self.members = []
        self.league = None  # league.League une fois lancée
        self.created = now
        self.deadline = 0.0  # fin du délai de la journée en cours (heure du serveur, time.time), 0 : aucun
        self.step = None  # (phase, journée, jour) : quand il change, le délai repart
        self.version = 1  # change à chaque changement de l'état de la ligue
        self.playing = {}  # clé d'un match contre l'IA -> heure de son coup d'envoi (LG_PLAY)
        self.over_at = 0.0

    def member(self, player_id):
        for m in self.members:
            if m.player_id == player_id:
                return m
        return None

    def active(self):
        return [m for m in self.members if not m.left]

    def team_of(self, member):
        """Index local de l'équipe du membre dans la ligue lancée, ou -1."""
        if self.league is None or member is None:
            return -1
        return self.league.find_team(member.animal)

    def member_of_team(self, team):
        if self.league is None or not 0 <= team < self.league.n_teams:
            return None
        animal = self.league.teams[team]
        for m in self.active():
            if m.animal == animal:
                return m
        return None

    def to_dict(self):
        return dict(id=self.id, code=self.code, owner=self.owner, settings=self.settings, ratings=self.ratings,
                    members=[m.to_dict() for m in self.members], created=self.created, deadline=self.deadline,
                    step=self.step, version=self.version, playing={str(k): v for k, v in self.playing.items()},
                    over_at=self.over_at, league=self.league.to_dict() if self.league else None)

    @classmethod
    def from_dict(cls, d):
        lg = cls(d["id"], d["code"], d["owner"], d["settings"], d["ratings"], d["created"])
        lg.members = [Member.from_dict(m) for m in d["members"]]
        lg.deadline = d["deadline"]
        lg.step = tuple(d["step"]) if d["step"] else None
        lg.version = d["version"]
        lg.playing = {int(k): v for k, v in d["playing"].items()}
        lg.over_at = d["over_at"]
        lg.league = L.League.from_dict(d["league"]) if d["league"] else None
        return lg


class Leagues:
    def __init__(self, db, rng=None, grace=20 * 60, keep_over=14 * 24 * 3600, max_leagues=300, fixed_code=None,
                 delay_scale=60.0, clock=time.time):
        self.clock = clock  # heure réelle : les ligues durent des jours et survivent à un redémarrage du serveur
        self.db = db  # store.Database (celle de store.Store), None : rien n'est gardé (tests)
        self.rng = rng or random.Random()
        self.grace = grace  # secondes pendant lesquelles un match contre l'IA en cours échappe au délai
        self.keep_over = keep_over  # une ligue finie reste visible ce temps-là
        self.max_leagues = max_leagues
        self.fixed_code = fixed_code
        self.delay_scale = delay_scale  # secondes par minute de délai (tests : moins de 60)
        self.by_code = {}
        self.by_player = {}  # numéro du compte -> sa ligue (en cours, ou finie et pas encore quittée)
        self.want = {}  # numéro du compte -> (clé du match, heure) : il attend de jouer ce match
        self.next_id = 1
        self.log = lambda text: None
        if db is not None:
            for row_id, data in db.all("SELECT id, data FROM leagues WHERE NOT closed"):
                try:
                    self.add(OnlineLeague.from_dict(data if isinstance(data, dict) else json.loads(data)))
                except (ValueError, KeyError, TypeError) as e:
                    print("ligue %d illisible dans la base : %s" % (row_id, e))
            self.next_id = 1 + (db.one("SELECT MAX(id) FROM leagues")[0] or 0)

    def add(self, lg):
        self.by_code[lg.code] = lg
        for m in lg.members:
            if not m.left:
                self.by_player[m.player_id] = lg
        self.next_id = max(self.next_id, lg.id + 1)

    def save(self, lg, now, closed=False):
        if self.db is None:
            return
        data = json.dumps(lg.to_dict(), separators=(",", ":"))
        self.db.execute("INSERT INTO leagues (id, code, closed, created, updated, data) VALUES (%s, %s, %s, %s, %s, %s::jsonb)"
                        " ON CONFLICT (id) DO UPDATE SET code = EXCLUDED.code, closed = EXCLUDED.closed,"
                        " updated = EXCLUDED.updated, data = EXCLUDED.data",
                        (lg.id, lg.code, bool(closed), lg.created, now, data))

    def new_code(self):
        if self.fixed_code:
            return None if self.fixed_code in self.by_code else self.fixed_code
        for _ in range(200):
            code = "".join(self.rng.choice(CODE_LETTERS) for _ in range(CODE_LENGTH))
            if code not in self.by_code:
                return code
        return None

    def close(self, lg, now):
        """La ligue disparaît (créateur parti avant le départ, ou finie depuis longtemps)."""
        self.by_code.pop(lg.code, None)
        for pid, other in list(self.by_player.items()):
            if other is lg:
                del self.by_player[pid]
        self.save(lg, now, closed=True)

    # ------------------------------------------------------------ commandes

    def command(self, player, name, cmd, args, now):
        """Une commande PKT_LEAGUE d'un joueur (store.Player). Renvoie un code P.LGE_*."""
        if player is None:
            return P.LGE_NO_ACCOUNT
        lg = self.by_player.get(player.id)
        me = lg.member(player.id) if lg else None
        if me is not None and me.name != name:
            me.name = name
            self.save(lg, now)
        if cmd == P.LG_CREATE:
            return self.create(player, name, args, now)
        if cmd == P.LG_JOIN:
            return self.join(player, name, args, now)
        if lg is None:
            return P.LGE_NONE
        if cmd == P.LG_TEAM:
            return self.set_team(lg, me, args["animal"], args["clothes"], now)
        if cmd == P.LG_START:
            return self.start(lg, player, now)
        if cmd == P.LG_LEAVE:
            return self.leave(lg, me, now)
        if cmd == P.LG_RESULT:
            return self.result(lg, me, args["key"], args["goals_host"], args["goals_guest"], now)
        if cmd == P.LG_PLAY:
            return self.play(lg, me, args["key"], now)
        if cmd == P.LG_SKIP:
            return self.skip(lg, player, now)
        return P.LGE_NONE

    def create(self, player, name, args, now):
        lg = self.by_player.get(player.id)
        if lg is not None and lg.owner == player.id and lg.league is None:
            return P.LGE_NONE  # demande répétée
        s = args["settings"]
        ratings = args["ratings"]
        if (len(ratings) != ANIMALS or not all(30.0 <= r <= 200.0 for r in ratings) or not DELAYS[0] <= s["delay"] <= DELAYS[1]
                or not 1 <= s["half"] <= 10 or not 0 <= s["form"] <= 20 or not 0 <= s["goals_per_minute"] <= 2
                or not 0 <= s["home_bonus"] <= 1 or not 0 <= s["strength_effect"] <= 1
                or not 0 <= args["animal"] < ANIMALS or s["points_win"] > 20 or s["points_draw"] > 20):
            return P.LGE_BAD
        if len(self.by_code) >= self.max_leagues:
            return P.LGE_LIMIT
        code = self.new_code()
        if code is None:
            return P.LGE_LIMIT
        if lg is not None:
            self.leave(lg, lg.member(player.id), now)
        lg = OnlineLeague(self.next_id, code, player.id, s, ratings, now)
        self.next_id += 1
        lg.members.append(Member(player.id, name, args["animal"], min(args["clothes"], 7)))
        self.add(lg)
        self.save(lg, now)
        self.log("ligue %s créée par %r : %d qualifiés, délai %d min, mi-temps %d min" % (
            code, player, s["qualifiers"], s["delay"], s["half"]))
        return P.LGE_NONE

    def open_league(self, player_id):
        """Sans code : la ligue pas encore lancée, avec de la place, créée le plus récemment par un autre joueur."""
        best = None
        for lg in self.by_code.values():
            if (lg.league is None and lg.owner != player_id and lg.active() and len({m.animal for m in lg.members}) < ANIMALS
                    and (best is None or (lg.created, lg.id) > (best.created, best.id))):
                best = lg
        return best

    def join(self, player, name, args, now):
        code = args["code"].strip().upper()
        current = self.by_player.get(player.id)
        if not code:
            # rejoindre sans code (le champ du code est vide) : la dernière ligue ouverte
            if current is not None and current.league is None:
                return P.LGE_NONE  # déjà dans une ligue qui attend (ou demande répétée)
            lg = self.open_league(player.id)
            if lg is None:
                return P.LGE_UNKNOWN
            code = lg.code
        lg = self.by_code.get(code)
        if lg is not None and current is lg:
            return P.LGE_NONE  # demande répétée
        if lg is None:
            return P.LGE_UNKNOWN
        if lg.league is not None:
            return P.LGE_STARTED
        taken = {m.animal for m in lg.members}
        if len(taken) >= ANIMALS:
            return P.LGE_FULL
        if current is not None:
            self.leave(current, current.member(player.id), now)
        animal, error = args["animal"], P.LGE_NONE
        if animal in taken or not 0 <= animal < ANIMALS:
            animal = min(set(range(ANIMALS)) - taken)
            error = P.LGE_TEAM_TAKEN
        lg.members.append(Member(player.id, name, animal, min(args["clothes"], 7) if error == P.LGE_NONE else 0))
        self.by_player[player.id] = lg
        self.save(lg, now)
        self.log("ligue %s : %r entre (%d joueurs)" % (code, player, len(lg.members)))
        return error

    def set_team(self, lg, me, animal, clothes, now):
        if lg.league is not None or me is None:
            return P.LGE_STARTED if me is not None else P.LGE_NONE
        if not 0 <= animal < ANIMALS:
            return P.LGE_BAD
        if any(m.animal == animal for m in lg.members if m is not me):
            return P.LGE_TEAM_TAKEN
        if (me.animal, me.clothes) != (animal, min(clothes, 7)):
            me.animal, me.clothes = animal, min(clothes, 7)
            self.save(lg, now)
        return P.LGE_NONE

    def start(self, lg, player, now):
        if lg.league is not None:
            return P.LGE_NONE  # demande répétée
        if lg.owner != player.id:
            return P.LGE_NOT_OWNER
        s = lg.settings
        league = L.League(L.JavaRandom(self.rng.randrange(1 << 48)))
        league.qualifiers = s["qualifiers"]
        league.reset = s["reset"]
        league.points_win = s["points_win"]
        league.points_draw = s["points_draw"]
        league.goals_per_match = L.f32(L.f32(L.f32(s["goals_per_minute"]) * 2.0) * s["half"])
        league.home_bonus = L.f32(s["home_bonus"])
        league.strength_effect = L.f32(s["strength_effect"])
        humans = [m.animal for m in lg.members]  # les équipes 0..13 sont les animaux 0..13
        league.start(list(range(ANIMALS)), lg.ratings, s["form"], humans)
        lg.league = league
        self.changed(lg, now)
        self.log("ligue %s lancée : %d joueurs, %d journées" % (lg.code, len(lg.members), league.rounds))
        return P.LGE_NONE

    def leave(self, lg, me, now):
        if me is None:
            return P.LGE_NONE
        self.by_player.pop(me.player_id, None)
        self.want.pop(me.player_id, None)
        if lg.league is None:
            if me.player_id == lg.owner:
                self.log("ligue %s fermée : le créateur est parti avant le départ" % lg.code)
                self.close(lg, now)
                return P.LGE_NONE
            lg.members.remove(me)
            self.save(lg, now)
            self.log("ligue %s : %s sort" % (lg.code, me.name))
            return P.LGE_NONE
        if not me.left:
            me.left = True
            if lg.league.phase != L.PHASE_OVER:
                lg.league.drop_human(lg.league.find_team(me.animal))
                self.log("ligue %s : %s abandonne, l'IA reprend son équipe" % (lg.code, me.name))
            if me.player_id == lg.owner and lg.active():
                lg.owner = lg.active()[0].player_id
            self.changed(lg, now)
        if not lg.active():
            self.close(lg, now)
        return P.LGE_NONE

    def result(self, lg, me, key, goals_host, goals_guest, now):
        """Score d'un match contre l'IA, joué en solo chez le joueur."""
        team = lg.team_of(me) if me is not None and not me.left else -1
        if team < 0 or lg.league.phase == L.PHASE_OVER or key not in lg.league.pending_keys():
            return P.LGE_REFUSED if not self.already(lg, team, key) else P.LGE_NONE
        sides = lg.league.sides(key)
        other = sides[1] if sides[0] == team else sides[0]
        if team not in sides or other in lg.league.humans:
            return P.LGE_REFUSED  # un match entre deux joueurs passe par un salon
        res = lg.league.report(key, min(goals_host, 30), min(goals_guest, 30))
        if res is None:
            return P.LGE_REPLAY
        lg.playing.pop(key, None)
        self.log("ligue %s : %s %d-%d (%s contre l'IA)" % (lg.code, self.key_name(lg, key), goals_host, goals_guest, me.name))
        self.changed(lg, now)
        return P.LGE_NONE

    def already(self, lg, team, key):
        """Ce match de l'équipe a déjà son résultat : le jeu redit LG_RESULT tant qu'il n'a pas vu la réponse."""
        lgx = lg.league
        if lgx is None or team < 0:
            return False
        if key < L.KEY_PLAYOFFS:
            return 0 <= key < lgx.n_fixtures and lgx.f_played[key] and team in (lgx.f_host[key], lgx.f_guest[key])
        m = key - L.KEY_PLAYOFFS
        return 0 <= m < lgx.n_matches and lgx.b_played[m] and team in (lgx.b_a[m], lgx.b_b[m])

    def play(self, lg, me, key, now):
        team = lg.team_of(me) if me is not None and not me.left else -1
        if team < 0 or key not in lg.league.pending_keys() or team not in lg.league.sides(key):
            return P.LGE_REFUSED
        self.want[me.player_id] = (key, now)
        sides = lg.league.sides(key)
        other = sides[1] if sides[0] == team else sides[0]
        if other not in lg.league.humans and key not in lg.playing:
            lg.playing[key] = now
            self.log("ligue %s : %s joue %s contre l'IA" % (lg.code, me.name, self.key_name(lg, key)))
            self.save(lg, now)
        return P.LGE_NONE

    def skip(self, lg, player, now):
        if lg.owner != player.id:
            return P.LGE_NOT_OWNER
        if lg.league is None or lg.league.phase == L.PHASE_OVER:
            return P.LGE_NONE
        done = lg.league.close_step(self.in_play(lg, now))
        self.log("ligue %s : journée passée par le créateur, %d matchs simulés" % (lg.code, len(done)))
        self.changed(lg, now)
        return P.LGE_NONE

    # ------------------------------------------------------------ matchs entre deux joueurs (rooms.Lobby)

    def pairing(self, player_id, now):
        """Ce joueur attend de jouer un match contre un autre joueur, qui l'attend aussi : renvoie
        (ligue, clé, numéro du compte de l'équipe qui reçoit, de l'invitée), sinon None."""
        lg = self.by_player.get(player_id)
        want = self.want.get(player_id)
        if lg is None or lg.league is None or want is None or now - want[1] > WANT_FRESH:
            return None
        key = want[0]
        if key not in lg.league.pending_keys():
            return None
        home, away = (lg.member_of_team(t) for t in lg.league.sides(key))
        if home is None or away is None:
            return None
        other = away if home.player_id == player_id else home
        theirs = self.want.get(other.player_id)
        if theirs is None or theirs[0] != key or now - theirs[1] > WANT_FRESH:
            return None
        return lg, key, home.player_id, away.player_id

    def match_result(self, league_id, key, goals_host, goals_guest):
        """Match entre deux joueurs compté par le salon (rooms.Lobby.settle). Renvoie True s'il entre dans la ligue."""
        now = self.clock()
        lg = next((x for x in self.by_code.values() if x.id == league_id), None)
        if lg is None or lg.league is None:
            return False
        res = lg.league.report(key, min(goals_host, 30), min(goals_guest, 30))
        if res is not True:
            self.log("ligue %s : %s %d-%d pas retenu (%s)" % (lg.code, self.key_name(lg, key), goals_host, goals_guest,
                                                             "nul en playoffs, à rejouer" if res is None else "plus à jouer"))
            return False
        self.log("ligue %s : %s %d-%d (entre joueurs)" % (lg.code, self.key_name(lg, key), goals_host, goals_guest))
        self.changed(lg, now)
        return True

    # ------------------------------------------------------------ temps

    def in_play(self, lg, now, busy=()):
        """Matchs épargnés par le délai : contre l'IA lancés depuis moins de `grace`, entre joueurs en cours (busy)."""
        keep = {k for k, t in lg.playing.items() if now - t < self.grace}
        return keep | {k for (league_id, k) in busy if league_id == lg.id}

    def changed(self, lg, now):
        """L'état de la ligue a changé : nouvelle version ; nouvelle journée -> le délai repart."""
        lg.version += 1
        league = lg.league
        step = (league.phase, league.round, league.day) if league else None
        if step != lg.step:
            lg.step = step
            lg.playing = {}
            delay = lg.settings["delay"]
            lg.deadline = now + delay * self.delay_scale if delay and league and league.phase != L.PHASE_OVER else 0.0
            if league and league.phase == L.PHASE_OVER and not lg.over_at:
                lg.over_at = now
                champion = lg.member_of_team(league.champion)
                self.log("ligue %s finie : champion %s" % (lg.code, champion.name if champion else "l'IA (équipe %d)" % league.teams[league.champion]))
        self.save(lg, now)

    def tick(self, now, busy=()):
        """Une fois par seconde : délais dépassés, ligues finies depuis longtemps. busy : (ligue, clé) des matchs
        entre joueurs en cours dans un salon."""
        for lg in list(self.by_code.values()):
            league = lg.league
            if league is None:
                continue
            if league.phase == L.PHASE_OVER:
                if now - lg.over_at > self.keep_over:
                    self.close(lg, now)
                continue
            if lg.deadline and now >= lg.deadline:
                keep = self.in_play(lg, now, busy)
                done = league.close_step(keep)
                if done or not keep:
                    self.log("ligue %s : délai dépassé, %d matchs simulés" % (lg.code, len(done)))
                    if not done:
                        lg.deadline = now + lg.settings["delay"] * self.delay_scale  # rien à fermer : on repart
                    self.changed(lg, now)

    # ------------------------------------------------------------ ce que voit un joueur

    def view(self, player, known_version, now, presence):
        """Pour make_league_info. presence(numéro du compte) -> drapeaux LGM_ONLINE / LGM_BUSY."""
        if player is None:
            return None
        lg = self.by_player.get(player.id)
        if lg is None:
            return None
        league = lg.league
        me = lg.member(player.id)
        my_team = lg.team_of(me)
        my_key = league.next_key(my_team) if league is not None and my_team >= 0 else None
        mine_want = self.want.get(player.id)
        members = []
        for m in lg.members:
            flags = presence(m.player_id)
            if m.player_id == player.id:
                flags |= P.LGM_ME
            if m.player_id == lg.owner:
                flags |= P.LGM_OWNER
            if m.left:
                flags |= P.LGM_LEFT
            want = self.want.get(m.player_id)
            if (my_key is not None and want is not None and want[0] == my_key and now - want[1] <= WANT_FRESH
                    and m.player_id != player.id):
                flags |= P.LGM_WAITING
            members.append(dict(name=m.name, animal=m.animal, clothes=m.clothes, flags=flags))
        flags = (P.LGF_OWNER if lg.owner == player.id else 0) | (P.LGF_STARTED if league else 0)
        if league is not None and league.phase == L.PHASE_OVER:
            flags |= P.LGF_OVER
        state = None
        if league is not None and known_version != lg.version:
            # le jeu lit cet état avec ModLeague.Load : son « joueur » doit être une équipe, la mienne si j'en ai une
            state = league.save(my_team if my_team >= 0 else 0)
        deadline = int(max(0, lg.deadline - now)) if lg.deadline and league and league.phase != L.PHASE_OVER else -1
        return dict(code=lg.code, flags=flags, version=lg.version, settings=lg.settings, deadline=deadline,
                    my_team=my_team, my_key=my_key if my_key is not None else -1, members=members, state=state)

    @staticmethod
    def key_name(lg, key):
        league = lg.league
        h, g = league.sides(key)
        # la journée du match lui-même : après son score, la journée en cours peut déjà être la suivante
        return "%s %d contre %d" % ("journée %d :" % (league.f_round[key] + 1) if key < L.KEY_PLAYOFFS else "playoffs :",
                                    league.teams[h], league.teams[g])
