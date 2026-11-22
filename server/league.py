"""Logique de la ligue en ligne : copie de mods/java/ModLeague.java, avec plusieurs équipes humaines.

Ligue en aller simple (toutes les équipes), puis playoffs à double élimination pour les N premiers. Les matchs
entre IA sont simulés ; une journée (ou un jour de playoffs) se ferme quand tous les matchs des humains sont joués,
ou quand on la force (délai dépassé, voir leagues.py) : ce qui reste est alors simulé.

Les tableaux et la sauvegarde ont la forme de ModLeague (save() rend la suite d'entiers de ModLeague.Save) : le
jeu lit l'état envoyé par le serveur avec ModLeague.Load et l'affiche avec les écrans de la ligue solo.

Les calculs en float du jeu sont refaits en float 32 bits et le hasard est celui de java.util.Random : avec la
même graine, une ligue à un humain donne exactement la ligue du jeu (vérifié par test_league.py).
Pas de réseau ni de fichier ici. Voir notes/online-league.md.
"""
import math
import struct

VERSION = 1  # ModLeague._VERSION
PHASE_LEAGUE = 0
PHASE_PLAYOFFS = 1
PHASE_OVER = 2
WB, LB, GF, GF2 = 0, 1, 2, 3
SRC_WIN = 1000
SRC_LOSE = 2000

# Clé d'un match pour le réseau : numéro du match de ligue, ou KEY_PLAYOFFS + numéro du match du tableau
KEY_PLAYOFFS = 10000


def f32(x):
    """Arrondi au float 32 bits, comme un calcul en float du jeu."""
    return struct.unpack("f", struct.pack("f", x))[0]


def java_round(x):
    """Math.round(float) du JRE du jeu : (int) floor(x + 0.5f)."""
    return int(math.floor(f32(x + 0.5)))


class JavaRandom:
    """java.util.Random, pour retrouver le hasard du jeu à graine égale."""
    MULT = 0x5DEECE66D
    MASK = (1 << 48) - 1

    def __init__(self, seed):
        self.seed = (seed ^ self.MULT) & self.MASK

    def next(self, bits):
        self.seed = (self.seed * self.MULT + 0xB) & self.MASK
        r = self.seed >> (48 - bits)
        return r - (1 << 32) if r & (1 << 31) else r  # int Java signé

    def next_int(self, n):
        if n & -n == n:
            return (n * self.next(31)) >> 31
        while True:
            bits = self.next(31)
            val = bits % n
            if bits - val + (n - 1) < (1 << 31):  # pas de dépassement de l'int Java
                return val

    def next_float(self):
        return self.next(24) / float(1 << 24)

    def next_double(self):
        return ((self.next(26) << 27) + self.next(27)) / float(1 << 53)


class League:
    def __init__(self, rng):
        self.rng = rng
        # réglages
        self.points_win = 3
        self.points_draw = 1
        self.qualifiers = 8
        self.reset = False
        self.goals_per_match = f32(1.6)
        self.home_bonus = f32(0.1)
        self.strength_effect = f32(0.08)
        # équipes
        self.teams = []  # animal de chaque équipe
        self.rating = []  # note x100, forme du tournoi comprise
        self.lots = []  # tirage au sort, dernier critère du classement
        self.humans = set()  # équipes jouées par un humain (index locaux)
        self.phase = PHASE_LEAGUE
        # ligue
        self.rounds = 0
        self.round = 0
        self.f_round, self.f_host, self.f_guest, self.f_gh, self.f_gg, self.f_played = [], [], [], [], [], []
        # playoffs
        self.seed = []
        self.alloc_bracket(0)
        self.day = 0
        self.champion = -1

    @property
    def n_teams(self):
        return len(self.teams)

    @property
    def n_fixtures(self):
        return len(self.f_round)

    @property
    def n_matches(self):
        return len(self.b_type)

    # ------------------------------------------------------------ départ

    def start(self, animals, ratings, form, humans):
        """animals : animaux participants ; ratings : leurs notes (float) ; humans : index des équipes humaines."""
        n = len(animals)
        self.teams = list(animals)
        self.rating = [0] * n
        self.lots = [0] * n
        self.humans = set(humans)
        shuffle = self.shuffle(n)
        form = f32(form)
        for i in range(n):
            noise = f32(f32(self.rng.next_float() * 2.0 - 1.0) * form) if form > 0 else 0.0
            self.rating[i] = java_round(f32(f32(f32(ratings[i]) + noise) * 100.0))
            self.lots[i] = shuffle[i]
        self.qualifiers = clamp_qualifiers(self.qualifiers, n)
        self.phase = PHASE_LEAGUE
        self.round = 0
        self.day = 0
        self.champion = -1
        self.seed = []
        self.alloc_bracket(0)
        self.build_schedule()
        self.advance()

    def shuffle(self, n):
        t = list(range(n))
        for i in range(n - 1, 0, -1):
            j = self.rng.next_int(i + 1)
            t[i], t[j] = t[j], t[i]
        return t

    def build_schedule(self):
        """Méthode du cercle : chaque équipe rencontre toutes les autres une fois."""
        n = self.n_teams
        slots = n if n % 2 == 0 else n + 1
        order = self.shuffle(n)
        arr = [order[i] if i < n else -1 for i in range(slots)]
        self.rounds = slots - 1
        self.f_round, self.f_host, self.f_guest, self.f_gh, self.f_gg, self.f_played = [], [], [], [], [], []
        for r in range(self.rounds):
            for i in range(slots // 2):
                a, b = arr[i], arr[slots - 1 - i]
                if a >= 0 and b >= 0:
                    a_home = r % 2 == 0 if i == 0 else i % 2 == 0
                    self.f_round.append(r)
                    self.f_host.append(a if a_home else b)
                    self.f_guest.append(b if a_home else a)
                    self.f_gh.append(0)
                    self.f_gg.append(0)
                    self.f_played.append(False)
            last = arr[slots - 1]
            for i in range(slots - 1, 1, -1):
                arr[i] = arr[i - 1]
            arr[1] = last

    # ------------------------------------------------------------ déroulement

    def advance(self):
        """Simule tout ce qui peut l'être, jusqu'à une journée où un humain a encore un match, ou la fin."""
        while True:
            if self.phase == PHASE_LEAGUE:
                if self.round >= self.rounds:
                    self.start_playoffs()
                    continue
                if self.pending_keys():
                    return
                for f in range(self.n_fixtures):
                    if self.f_round[f] == self.round and not self.f_played[f]:
                        self.sim_fixture(f)
                self.round += 1
            elif self.phase == PHASE_PLAYOFFS:
                self.resolve_all()
                if self.pending_keys():
                    return
                for m in range(self.n_matches):
                    if self.b_active[m] and not self.b_played[m] and self.b_day[m] == self.day:
                        self.sim_match(m)
                        self.on_match_played(m)
                if not self.has_pending_match():
                    self.finish()
                    return
                self.day += 1
            else:
                return

    def close_step(self, keep=()):
        """Délai dépassé : les matchs des humains encore à jouer cette journée sont simulés, sauf ceux de keep
        (en cours). Renvoie les clés simulées."""
        done = []
        for key in self.pending_keys():
            if key in keep:
                continue
            if key < KEY_PLAYOFFS:
                self.sim_fixture(key)
            else:
                m = key - KEY_PLAYOFFS
                self.sim_match(m)
                self.on_match_played(m)
            done.append(key)
        self.advance()
        return done

    def report(self, key, goals_host, goals_guest):
        """Résultat d'un match d'humain (buts de l'équipe qui reçoit, puis de l'invitée).
        Renvoie True s'il est enregistré, False si ce match n'est pas à jouer ; None : nul en playoffs, à rejouer."""
        if key not in self.pending_keys():
            return False
        if key < KEY_PLAYOFFS:
            self.f_gh[key] = goals_host
            self.f_gg[key] = goals_guest
            self.f_played[key] = True
        else:
            if goals_host == goals_guest:
                return None  # pas de nul en élimination : un nul vient d'un match interrompu
            m = key - KEY_PLAYOFFS
            host_a = self.b_host_a[m]
            self.b_ga[m] = goals_host if host_a else goals_guest
            self.b_gb[m] = goals_guest if host_a else goals_host
            self.b_played[m] = True
            self.on_match_played(m)
        self.advance()
        return True

    def drop_human(self, team):
        """Le joueur de cette équipe abandonne : l'IA la reprend, ses matchs seront simulés."""
        self.humans.discard(team)
        self.advance()

    def start_playoffs(self):
        rank = self.ranking()
        self.seed = rank[:self.qualifiers]
        self.build_bracket()
        self.phase = PHASE_PLAYOFFS
        self.day = 0
        self.resolve_all()

    def finish(self):
        last = -1
        for m in range(self.n_matches):
            if self.b_active[m] and self.b_played[m] and (last < 0 or self.b_day[m] >= self.b_day[last]):
                last = m
        self.champion = self.winner(last) if last >= 0 else -1
        self.phase = PHASE_OVER

    # ------------------------------------------------------------ matchs en attente

    def pending_keys(self):
        """Matchs de la journée (ou du jour de playoffs) en cours où joue un humain, pas encore joués."""
        keys = []
        if self.phase == PHASE_LEAGUE and self.round < self.rounds:
            for f in range(self.n_fixtures):
                if (self.f_round[f] == self.round and not self.f_played[f]
                        and (self.f_host[f] in self.humans or self.f_guest[f] in self.humans)):
                    keys.append(f)
        elif self.phase == PHASE_PLAYOFFS:
            for m in range(self.n_matches):
                if (self.b_active[m] and not self.b_played[m] and self.b_day[m] == self.day
                        and (self.b_a[m] in self.humans or self.b_b[m] in self.humans)):
                    keys.append(KEY_PLAYOFFS + m)
        return keys

    def sides(self, key):
        """(équipe qui reçoit, invitée) d'un match."""
        if key < KEY_PLAYOFFS:
            return self.f_host[key], self.f_guest[key]
        m = key - KEY_PLAYOFFS
        return self.host(m), self.guest(m)

    def next_key(self, team):
        """Prochain match de cette équipe dans la journée en cours, ou None."""
        for key in self.pending_keys():
            if team in self.sides(key):
                return key
        return None

    def is_golden(self, key):
        """But en or à égalité : en playoffs."""
        return key >= KEY_PLAYOFFS

    def is_qualified(self, team):
        return self.phase != PHASE_LEAGUE and team in self.seed

    # ------------------------------------------------------------ tableau à double élimination

    def alloc_bracket(self, n):
        self.b_type, self.b_round, self.b_index, self.b_src_a, self.b_src_b, self.b_day = [], [], [], [], [], []
        self.b_a, self.b_b, self.b_ga, self.b_gb = [], [], [], []
        self.b_host_a, self.b_played, self.b_active = [], [], []

    def add_match(self, mtype, rnd, index, src_a, src_b):
        m = self.n_matches
        self.b_type.append(mtype)
        self.b_round.append(rnd)
        self.b_index.append(index)
        self.b_src_a.append(src_a)
        self.b_src_b.append(src_b)
        self.b_a.append(-1)
        self.b_b.append(-1)
        self.b_ga.append(0)
        self.b_gb.append(0)
        self.b_host_a.append(False)
        self.b_played.append(False)
        self.b_active.append(mtype != GF2)
        self.b_day.append(max(self.src_day(src_a), self.src_day(src_b)) + 1)
        return m

    def src_day(self, src):
        return self.b_day[src % 1000] if src >= SRC_WIN else -1

    def build_bracket(self):
        q = len(self.seed)
        k = log2(q)
        self.alloc_bracket(2 * q + 2)
        order = seed_order(q)
        wb = [[self.add_match(WB, 0, i, order[2 * i], order[2 * i + 1]) for i in range(q // 2)]]
        for r in range(1, k):
            prev = wb[r - 1]
            wb.append([self.add_match(WB, r, i, SRC_WIN + prev[2 * i], SRC_WIN + prev[2 * i + 1])
                       for i in range(len(prev) // 2)])
        if k < 2:
            return  # 2 qualifiés : une simple finale
        lb = 0
        prev = [self.add_match(LB, lb, i, SRC_LOSE + wb[0][2 * i], SRC_LOSE + wb[0][2 * i + 1]) for i in range(q // 4)]
        for j in range(1, k):
            lb += 1
            drop = []
            for i in range(len(prev)):
                came_from = wb[j][len(wb[j]) - 1 - i]  # perdants croisés
                drop.append(self.add_match(LB, lb, i, SRC_WIN + prev[i], SRC_LOSE + came_from))
            prev = drop
            if j < k - 1:
                lb += 1
                prev = [self.add_match(LB, lb, i, SRC_WIN + prev[2 * i], SRC_WIN + prev[2 * i + 1])
                        for i in range(len(prev) // 2)]
        gf = self.add_match(GF, 0, 0, SRC_WIN + wb[k - 1][0], SRC_WIN + prev[0])
        self.add_match(GF2, 0, 0, SRC_WIN + wb[k - 1][0], SRC_WIN + prev[0])
        self.b_day[gf + 1] = self.b_day[gf] + 1

    def src_team(self, src):
        if src < SRC_WIN:
            return self.seed[src] if src < len(self.seed) else -1
        m = src % 1000
        if not self.b_played[m]:
            return -1
        return self.loser(m) if src >= SRC_LOSE else self.winner(m)

    def resolve_all(self):
        for m in range(self.n_matches):
            if self.b_a[m] < 0 or self.b_b[m] < 0:
                self.b_a[m] = self.src_team(self.b_src_a[m])
                self.b_b[m] = self.src_team(self.b_src_b[m])
                if self.b_a[m] >= 0 and self.b_b[m] >= 0:
                    t = self.b_type[m]
                    self.b_host_a[m] = t in (GF, GF2) or self.seed_of(self.b_a[m]) <= self.seed_of(self.b_b[m])

    def on_match_played(self, m):
        if self.b_type[m] == GF and self.reset and m + 1 < self.n_matches and self.winner(m) == self.b_b[m]:
            self.b_active[m + 1] = True

    def has_pending_match(self):
        return any(self.b_active[m] and not self.b_played[m] for m in range(self.n_matches))

    def winner(self, m):
        return self.b_a[m] if self.b_ga[m] > self.b_gb[m] else self.b_b[m]

    def loser(self, m):
        return self.b_b[m] if self.b_ga[m] > self.b_gb[m] else self.b_a[m]

    def host(self, m):
        return self.b_a[m] if self.b_host_a[m] else self.b_b[m]

    def guest(self, m):
        return self.b_b[m] if self.b_host_a[m] else self.b_a[m]

    def seed_of(self, team):
        return self.seed.index(team) if team in self.seed else 99

    # ------------------------------------------------------------ simulation

    def lam(self, a, b, home):
        diff = f32((self.rating[a] - self.rating[b]) / 100.0)
        l = f32(self.goals_per_match * f32(math.exp(f32(diff * self.strength_effect))))
        return f32(l * f32(1.0 + self.home_bonus)) if home else l

    def poisson(self, l):
        limit = math.exp(-l)
        p = 1.0
        k = 0
        while True:
            k += 1
            p *= self.rng.next_double()
            if not (p > limit and k < 30):
                return k - 1

    def sim_fixture(self, f):
        h, g = self.f_host[f], self.f_guest[f]
        self.f_gh[f] = self.poisson(self.lam(h, g, True))
        self.f_gg[f] = self.poisson(self.lam(g, h, False))
        self.f_played[f] = True

    def sim_match(self, m):
        h, g = self.host(m), self.guest(m)
        neutral = self.b_type[m] in (GF, GF2)
        lh = self.lam(h, g, not neutral)
        lg = self.lam(g, h, False)
        gh = self.poisson(lh)
        gg = self.poisson(lg)
        if gh == gg:  # but en or
            if self.rng.next_float() < f32(lh / f32(lh + lg)):
                gh += 1
            else:
                gg += 1
        self.b_ga[m] = gh if self.b_host_a[m] else gg
        self.b_gb[m] = gg if self.b_host_a[m] else gh
        self.b_played[m] = True

    # ------------------------------------------------------------ classement

    def stats(self):
        """Par équipe : joués, gagnés, nuls, perdus, buts pour, buts contre, points."""
        t = [[0] * 7 for _ in range(self.n_teams)]
        for f in range(self.n_fixtures):
            if self.f_played[f]:
                self.add_result(t[self.f_host[f]], self.f_gh[f], self.f_gg[f])
                self.add_result(t[self.f_guest[f]], self.f_gg[f], self.f_gh[f])
        return t

    def add_result(self, s, gf, ga):
        s[0] += 1
        if gf > ga:
            s[1] += 1
            s[6] += self.points_win
        elif gf == ga:
            s[2] += 1
            s[6] += self.points_draw
        else:
            s[3] += 1
        s[4] += gf
        s[5] += ga

    def before(self, st, a, b):
        x, y = st[a], st[b]
        if x[6] != y[6]:
            return x[6] > y[6]
        if x[4] - x[5] != y[4] - y[5]:
            return x[4] - x[5] > y[4] - y[5]
        if x[4] != y[4]:
            return x[4] > y[4]
        if x[1] != y[1]:
            return x[1] > y[1]
        return self.lots[a] < self.lots[b]

    def ranking(self):
        st = self.stats()
        t = list(range(self.n_teams))
        for i in range(1, len(t)):  # tri par insertion, comme le jeu
            v = t[i]
            j = i - 1
            while j >= 0 and self.before(st, v, t[j]):
                t[j + 1] = t[j]
                j -= 1
            t[j + 1] = v
        return t

    def find_team(self, animal):
        return self.teams.index(animal) if animal in self.teams else -1

    # ------------------------------------------------------------ sauvegarde

    def save(self, player):
        """La suite d'entiers de ModLeague.Save, vue par l'équipe player (le « joueur » du jeu)."""
        v = [VERSION, self.points_win, self.points_draw, 1 if self.reset else 0, self.qualifiers, self.n_teams, player,
             self.phase, self.round, self.rounds, self.day, self.champion]
        for i in range(self.n_teams):
            v += [self.teams[i], self.rating[i], self.lots[i]]
        v.append(self.n_fixtures)
        for f in range(self.n_fixtures):
            v += [self.f_round[f], self.f_host[f], self.f_guest[f], self.f_gh[f], self.f_gg[f], 1 if self.f_played[f] else 0]
        v.append(len(self.seed))
        v += self.seed
        v.append(self.n_matches)
        for m in range(self.n_matches):
            v += [self.b_type[m], self.b_round[m], self.b_index[m], self.b_src_a[m], self.b_src_b[m], self.b_day[m],
                  self.b_a[m], self.b_b[m], self.b_ga[m], self.b_gb[m],
                  (1 if self.b_host_a[m] else 0) | (2 if self.b_played[m] else 0) | (4 if self.b_active[m] else 0)]
        return v

    def to_dict(self):
        """Tout l'état, hasard compris, pour la base du serveur."""
        d = {k: v for k, v in self.__dict__.items() if k != "rng"}
        d["humans"] = sorted(self.humans)
        d["rng_seed"] = self.rng.seed
        return d

    @classmethod
    def from_dict(cls, d):
        rng = JavaRandom(0)
        rng.seed = d["rng_seed"]
        league = cls(rng)
        for k, v in d.items():
            if k != "rng_seed":
                setattr(league, k, v)
        league.humans = set(d["humans"])
        return league


def clamp_qualifiers(wanted, n_teams):
    """Plus grande puissance de 2 <= wanted et <= n_teams, au moins 2."""
    q = 2
    while q * 2 <= wanted and q * 2 <= n_teams:
        q *= 2
    return q


def seed_order(q):
    """Têtes de série au premier tour : 1-8, 4-5, 2-7, 3-6."""
    t = [0]
    while len(t) < q:
        n = len(t) * 2
        u = []
        for x in t:
            u += [x, n - 1 - x]
        t = u
    return t


def log2(q):
    k = 0
    while 1 << k < q:
        k += 1
    return k
