import java.util.Random;
import java.util.Vector;

// [mod] Tournoi : ligue en aller simple (toutes les équipes), puis playoffs à double élimination
// (tableau des gagnants, tableau des perdants, grande finale) pour les N premiers.
// Logique pure, sans le moteur (testable hors du jeu) : Championships la pilote, l'enregistre dans league.dat
// et l'affiche (MenuChampionships = classement, MenuChampionships2 = tableau). Voir notes/league.md.
// Les équipes sont repérées par un index local 0..n-1 (m_tTeam donne l'animal).
class ModLeague {
   static final int _VERSION = 1;
   static final int _PHASE_LEAGUE = 0;
   static final int _PHASE_PLAYOFFS = 1;
   static final int _PHASE_OVER = 2;
   // tableaux
   static final int _WB = 0; // gagnants
   static final int _LB = 1; // perdants
   static final int _GF = 2; // grande finale
   static final int _GF2 = 3; // revanche de la grande finale (si le vainqueur du tableau des perdants gagne la première)
   // origine d'une équipe dans un match du tableau : tête de série s, vainqueur ou perdant du match m
   static final int _SRC_WIN = 1000;
   static final int _SRC_LOSE = 2000;
   // retour de Report (mêmes valeurs que les _RESULT_ de Championships)
   static final int _RES_CONTINUE = -1;
   static final int _RES_OUT = 0;
   static final int _RES_QUALIFIED = 1;
   static final int _RES_CHAMPION = 4;
   static final int _RES_REPLAY = 5;

   // réglages (league.scr, via Championships)
   int m_nPointsWin = 3;
   int m_nPointsDraw = 1;
   int m_nQualifiers = 8;
   boolean m_bReset = false;
   float m_fGoalsPerMatch = 1.6F; // buts attendus par équipe et par match entre deux équipes égales
   float m_fHomeBonus = 0.1F;
   float m_fStrengthEffect = 0.08F; // par point de note d'écart
   Random m_cRandom = new Random();

   // équipes
   int m_nTeams = 0;
   int[] m_tTeam = new int[0];
   int[] m_tRating = new int[0]; // note x100, forme du tournoi comprise
   int[] m_tLots = new int[0]; // tirage au sort, dernier critère du classement
   int m_nPlayer = -1;
   int m_nPhase = _PHASE_LEAGUE;
   boolean[] m_tHuman = null; // ligue en ligne : équipes jouées par un joueur (ModOnlineLeague), pas sauvegardé

   // ligue
   int m_nRounds = 0;
   int m_nRound = 0; // journée en cours
   int m_nFixtures = 0;
   int[] m_tFRound = new int[0];
   int[] m_tFHost = new int[0];
   int[] m_tFGuest = new int[0];
   int[] m_tFGH = new int[0];
   int[] m_tFGG = new int[0];
   boolean[] m_tFPlayed = new boolean[0];

   // playoffs
   int[] m_tSeed = new int[0]; // équipe par tête de série (0 = premier de la ligue)
   int m_nMatches = 0;
   int[] m_tBType = new int[0];
   int[] m_tBRound = new int[0];
   int[] m_tBIndex = new int[0];
   int[] m_tBSrcA = new int[0];
   int[] m_tBSrcB = new int[0];
   int[] m_tBDay = new int[0];
   int[] m_tBA = new int[0]; // équipes (-1 tant que l'origine n'est pas jouée)
   int[] m_tBB = new int[0];
   int[] m_tBGA = new int[0];
   int[] m_tBGB = new int[0];
   boolean[] m_tBHostA = new boolean[0]; // A reçoit
   boolean[] m_tBPlayed = new boolean[0];
   boolean[] m_tBActive = new boolean[0];
   int m_nDay = 0;
   int m_nChampion = -1;

   // ================================================================ départ

   // tAnimals : animaux participants, tRatings : leurs notes, fForm : forme du tournoi (+/- au hasard), nPlayerAnimal : le joueur
   void Start(int[] tAnimals, float[] tRatings, float fForm, int nPlayerAnimal) {
      int n = tAnimals.length;
      this.m_nTeams = n;
      this.m_tTeam = new int[n];
      this.m_tRating = new int[n];
      this.m_tLots = new int[n];
      this.m_nPlayer = -1;
      int[] tShuffle = this.Shuffle(n);

      for (int i = 0; i < n; i++) {
         this.m_tTeam[i] = tAnimals[i];
         float fNoise = fForm > 0.0F ? (this.m_cRandom.nextFloat() * 2.0F - 1.0F) * fForm : 0.0F;
         this.m_tRating[i] = Math.round((tRatings[i] + fNoise) * 100.0F);
         this.m_tLots[i] = tShuffle[i];
         if (tAnimals[i] == nPlayerAnimal) {
            this.m_nPlayer = i;
         }
      }

      this.m_nQualifiers = ClampQualifiers(this.m_nQualifiers, n);
      this.m_nPhase = _PHASE_LEAGUE;
      this.m_nRound = 0;
      this.m_nDay = 0;
      this.m_nChampion = -1;
      this.m_tSeed = new int[0];
      this.m_nMatches = 0;
      this.AllocBracket(0);
      this.BuildSchedule();
      this.Advance();
   }

   // plus grande puissance de 2 <= nWanted et <= nTeams, au moins 2
   static int ClampQualifiers(int nWanted, int nTeams) {
      int q = 2;

      while (q * 2 <= nWanted && q * 2 <= nTeams) {
         q *= 2;
      }

      return q;
   }

   int[] Shuffle(int n) {
      int[] t = new int[n];

      for (int i = 0; i < n; i++) {
         t[i] = i;
      }

      for (int i = n - 1; i > 0; i--) {
         int j = this.m_cRandom.nextInt(i + 1);
         int k = t[i];
         t[i] = t[j];
         t[j] = k;
      }

      return t;
   }

   // méthode du cercle : chaque équipe rencontre toutes les autres une fois ; nombre impair -> une équipe au repos par journée
   void BuildSchedule() {
      int n = this.m_nTeams;
      int nSlots = n % 2 == 0 ? n : n + 1;
      int[] arr = new int[nSlots];
      int[] tOrder = this.Shuffle(n);

      for (int i = 0; i < nSlots; i++) {
         arr[i] = i < n ? tOrder[i] : -1;
      }

      this.m_nRounds = nSlots - 1;
      int nMax = this.m_nRounds * (nSlots / 2);
      this.m_tFRound = new int[nMax];
      this.m_tFHost = new int[nMax];
      this.m_tFGuest = new int[nMax];
      this.m_tFGH = new int[nMax];
      this.m_tFGG = new int[nMax];
      this.m_tFPlayed = new boolean[nMax];
      this.m_nFixtures = 0;

      for (int r = 0; r < this.m_nRounds; r++) {
         for (int i = 0; i < nSlots / 2; i++) {
            int a = arr[i];
            int b = arr[nSlots - 1 - i];
            if (a >= 0 && b >= 0) {
               boolean bAHome = i == 0 ? r % 2 == 0 : i % 2 == 0;
               int f = this.m_nFixtures++;
               this.m_tFRound[f] = r;
               this.m_tFHost[f] = bAHome ? a : b;
               this.m_tFGuest[f] = bAHome ? b : a;
            }
         }

         // rotation : le premier reste en place, les autres tournent d'un cran
         int nLast = arr[nSlots - 1];

         for (int i = nSlots - 1; i > 1; i--) {
            arr[i] = arr[i - 1];
         }

         arr[1] = nLast;
      }
   }

   // ================================================================ déroulement

   // joue (simule) tout ce qui ne concerne pas le joueur, jusqu'à son prochain match ou la fin du tournoi
   void Advance() {
      while (true) {
         if (this.m_nPhase == _PHASE_LEAGUE) {
            if (this.m_nRound >= this.m_nRounds) {
               this.StartPlayoffs();
            } else {
               if (this.GetPlayerFixture() >= 0) {
                  return;
               }

               for (int f = 0; f < this.m_nFixtures; f++) {
                  if (this.m_tFRound[f] == this.m_nRound && !this.m_tFPlayed[f]) {
                     this.SimFixture(f);
                  }
               }

               this.m_nRound++;
            }
         } else {
            if (this.m_nPhase != _PHASE_PLAYOFFS) {
               return;
            }

            this.ResolveAll();
            if (this.GetPlayerMatch() >= 0) {
               return;
            }

            for (int m = 0; m < this.m_nMatches; m++) {
               if (this.m_tBActive[m] && !this.m_tBPlayed[m] && this.m_tBDay[m] == this.m_nDay) {
                  this.SimMatch(m);
                  this.OnMatchPlayed(m);
               }
            }

            if (!this.HasPendingMatch()) {
               this.Finish();
               return;
            }

            this.m_nDay++;
         }
      }
   }

   // résultat du match du joueur (score de l'équipe qui reçoit, puis de l'invitée)
   int Report(int nGoalsHost, int nGoalsGuest) {
      if (this.m_nPhase == _PHASE_LEAGUE) {
         int f = this.GetPlayerFixture();
         if (f < 0) {
            return _RES_CONTINUE;
         }

         this.m_tFGH[f] = nGoalsHost;
         this.m_tFGG[f] = nGoalsGuest;
         this.m_tFPlayed[f] = true;
         this.Advance();
         if (this.m_nPhase == _PHASE_LEAGUE) {
            return _RES_CONTINUE;
         } else {
            return this.IsPlayerQualified() ? _RES_QUALIFIED : _RES_OUT;
         }
      } else if (this.m_nPhase == _PHASE_PLAYOFFS) {
         int m = this.GetPlayerMatch();
         if (m < 0) {
            return _RES_CONTINUE;
         } else if (nGoalsHost == nGoalsGuest) {
            // pas de nul en élimination (le but en or tranche) : un nul vient d'un match interrompu, on le rejoue
            return _RES_REPLAY;
         } else {
            this.m_tBGA[m] = this.m_tBHostA[m] ? nGoalsHost : nGoalsGuest;
            this.m_tBGB[m] = this.m_tBHostA[m] ? nGoalsGuest : nGoalsHost;
            this.m_tBPlayed[m] = true;
            this.OnMatchPlayed(m);
            this.Advance();
            if (this.m_nPhase == _PHASE_OVER) {
               return this.m_nChampion == this.m_nPlayer ? _RES_CHAMPION : _RES_OUT;
            } else {
               return _RES_CONTINUE;
            }
         }
      } else {
         return _RES_CONTINUE;
      }
   }

   // fin de la ligue : les N premiers, têtes de série dans l'ordre du classement
   void StartPlayoffs() {
      int[] tRank = this.GetRanking();
      int q = this.m_nQualifiers;
      this.m_tSeed = new int[q];

      for (int i = 0; i < q; i++) {
         this.m_tSeed[i] = tRank[i];
      }

      this.BuildBracket();
      this.m_nPhase = _PHASE_PLAYOFFS;
      this.m_nDay = 0;
      this.ResolveAll();
   }

   void Finish() {
      int nLast = -1;

      for (int m = 0; m < this.m_nMatches; m++) {
         if (this.m_tBActive[m] && this.m_tBPlayed[m] && (nLast < 0 || this.m_tBDay[m] >= this.m_tBDay[nLast])) {
            nLast = m;
         }
      }

      this.m_nChampion = nLast >= 0 ? this.GetWinner(nLast) : -1;
      this.m_nPhase = _PHASE_OVER;
   }

   // ================================================================ tableau à double élimination

   void AllocBracket(int n) {
      this.m_tBType = new int[n];
      this.m_tBRound = new int[n];
      this.m_tBIndex = new int[n];
      this.m_tBSrcA = new int[n];
      this.m_tBSrcB = new int[n];
      this.m_tBDay = new int[n];
      this.m_tBA = new int[n];
      this.m_tBB = new int[n];
      this.m_tBGA = new int[n];
      this.m_tBGB = new int[n];
      this.m_tBHostA = new boolean[n];
      this.m_tBPlayed = new boolean[n];
      this.m_tBActive = new boolean[n];
   }

   int AddMatch(int nType, int nRound, int nIndex, int nSrcA, int nSrcB) {
      int m = this.m_nMatches++;
      this.m_tBType[m] = nType;
      this.m_tBRound[m] = nRound;
      this.m_tBIndex[m] = nIndex;
      this.m_tBSrcA[m] = nSrcA;
      this.m_tBSrcB[m] = nSrcB;
      this.m_tBA[m] = -1;
      this.m_tBB[m] = -1;
      this.m_tBActive[m] = nType != _GF2;
      this.m_tBDay[m] = Math.max(this.SrcDay(nSrcA), this.SrcDay(nSrcB)) + 1;
      return m;
   }

   int SrcDay(int nSrc) {
      return nSrc >= _SRC_WIN ? this.m_tBDay[nSrc % 1000] : -1;
   }

   // ordre des têtes de série au premier tour : 1-8, 4-5, 2-7, 3-6 (1 et 2 ne se croisent qu'en finale)
   static int[] SeedOrder(int q) {
      int[] t = new int[]{0};

      while (t.length < q) {
         int n = t.length * 2;
         int[] u = new int[n];

         for (int i = 0; i < t.length; i++) {
            u[2 * i] = t[i];
            u[2 * i + 1] = n - 1 - t[i];
         }

         t = u;
      }

      return t;
   }

   static int Log2(int q) {
      int k = 0;

      while (1 << k < q) {
         k++;
      }

      return k;
   }

   int GetWBRounds() {
      return Log2(this.m_tSeed.length);
   }

   int GetLBRounds() {
      int k = this.GetWBRounds();
      return k >= 2 ? 2 * (k - 1) : 0;
   }

   void BuildBracket() {
      int q = this.m_tSeed.length;
      int k = Log2(q);
      this.m_nMatches = 0;
      this.AllocBracket(2 * q + 2);
      int[][] tWB = new int[k][];
      int[] tOrder = SeedOrder(q);
      // tableau des gagnants
      tWB[0] = new int[q / 2];

      for (int i = 0; i < q / 2; i++) {
         tWB[0][i] = this.AddMatch(_WB, 0, i, tOrder[2 * i], tOrder[2 * i + 1]);
      }

      for (int r = 1; r < k; r++) {
         tWB[r] = new int[tWB[r - 1].length / 2];

         for (int i = 0; i < tWB[r].length; i++) {
            tWB[r][i] = this.AddMatch(_WB, r, i, _SRC_WIN + tWB[r - 1][2 * i], _SRC_WIN + tWB[r - 1][2 * i + 1]);
         }
      }

      if (k < 2) {
         return; // 2 qualifiés : une simple finale
      }

      // tableau des perdants : un tour entre perdants, puis alternance « arrivée des perdants du tour suivant » / « tour entre rescapés »
      int nLB = 0;
      int[] tPrev = new int[q / 4];

      for (int i = 0; i < tPrev.length; i++) {
         tPrev[i] = this.AddMatch(_LB, nLB, i, _SRC_LOSE + tWB[0][2 * i], _SRC_LOSE + tWB[0][2 * i + 1]);
      }

      for (int j = 1; j < k; j++) {
         nLB++;
         int[] tDrop = new int[tPrev.length];

         for (int i = 0; i < tDrop.length; i++) {
            // perdants croisés (ordre inverse) pour éviter de rejouer tout de suite le même adversaire
            int nFrom = tWB[j][tWB[j].length - 1 - i];
            tDrop[i] = this.AddMatch(_LB, nLB, i, _SRC_WIN + tPrev[i], _SRC_LOSE + nFrom);
         }

         tPrev = tDrop;
         if (j < k - 1) {
            nLB++;
            int[] tCons = new int[tPrev.length / 2];

            for (int i = 0; i < tCons.length; i++) {
               tCons[i] = this.AddMatch(_LB, nLB, i, _SRC_WIN + tPrev[2 * i], _SRC_WIN + tPrev[2 * i + 1]);
            }

            tPrev = tCons;
         }
      }

      int nGF = this.AddMatch(_GF, 0, 0, _SRC_WIN + tWB[k - 1][0], _SRC_WIN + tPrev[0]);
      this.AddMatch(_GF2, 0, 0, _SRC_WIN + tWB[k - 1][0], _SRC_WIN + tPrev[0]);
      this.m_tBDay[nGF + 1] = this.m_tBDay[nGF] + 1;
   }

   int SrcTeam(int nSrc) {
      if (nSrc < _SRC_WIN) {
         return nSrc < this.m_tSeed.length ? this.m_tSeed[nSrc] : -1;
      } else {
         int m = nSrc % 1000;
         if (!this.m_tBPlayed[m]) {
            return -1;
         } else {
            return nSrc >= _SRC_LOSE ? this.GetLoser(m) : this.GetWinner(m);
         }
      }
   }

   void ResolveAll() {
      for (int m = 0; m < this.m_nMatches; m++) {
         if (this.m_tBA[m] < 0 || this.m_tBB[m] < 0) {
            this.m_tBA[m] = this.SrcTeam(this.m_tBSrcA[m]);
            this.m_tBB[m] = this.SrcTeam(this.m_tBSrcB[m]);
            if (this.m_tBA[m] >= 0 && this.m_tBB[m] >= 0) {
               // le mieux classé en ligue reçoit ; en grande finale, le vainqueur du tableau des gagnants
               int t = this.m_tBType[m];
               this.m_tBHostA[m] = t == _GF || t == _GF2 || this.GetSeed(this.m_tBA[m]) <= this.GetSeed(this.m_tBB[m]);
            }
         }
      }
   }

   void OnMatchPlayed(int m) {
      // grande finale gagnée par le rescapé du tableau des perdants : revanche si le règlement le prévoit
      if (this.m_tBType[m] == _GF && this.m_bReset && m + 1 < this.m_nMatches && this.GetWinner(m) == this.m_tBB[m]) {
         this.m_tBActive[m + 1] = true;
      }
   }

   boolean HasPendingMatch() {
      for (int m = 0; m < this.m_nMatches; m++) {
         if (this.m_tBActive[m] && !this.m_tBPlayed[m]) {
            return true;
         }
      }

      return false;
   }

   int GetWinner(int m) {
      return this.m_tBGA[m] > this.m_tBGB[m] ? this.m_tBA[m] : this.m_tBB[m];
   }

   int GetLoser(int m) {
      return this.m_tBGA[m] > this.m_tBGB[m] ? this.m_tBB[m] : this.m_tBA[m];
   }

   int GetHost(int m) {
      return this.m_tBHostA[m] ? this.m_tBA[m] : this.m_tBB[m];
   }

   int GetGuest(int m) {
      return this.m_tBHostA[m] ? this.m_tBB[m] : this.m_tBA[m];
   }

   int GetSeed(int nTeam) {
      for (int i = 0; i < this.m_tSeed.length; i++) {
         if (this.m_tSeed[i] == nTeam) {
            return i;
         }
      }

      return 99;
   }

   int FindMatch(int nType, int nRound, int nIndex) {
      for (int m = 0; m < this.m_nMatches; m++) {
         if (this.m_tBType[m] == nType && this.m_tBRound[m] == nRound && this.m_tBIndex[m] == nIndex) {
            return m;
         }
      }

      return -1;
   }

   // ================================================================ joueur

   int GetPlayerFixture() {
      if (this.m_nPhase == _PHASE_LEAGUE) {
         for (int f = 0; f < this.m_nFixtures; f++) {
            if (this.m_tFRound[f] == this.m_nRound && !this.m_tFPlayed[f] && (this.m_tFHost[f] == this.m_nPlayer || this.m_tFGuest[f] == this.m_nPlayer)) {
               return f;
            }
         }
      }

      return -1;
   }

   int GetPlayerMatch() {
      if (this.m_nPhase == _PHASE_PLAYOFFS) {
         for (int m = 0; m < this.m_nMatches; m++) {
            if (this.m_tBActive[m]
               && !this.m_tBPlayed[m]
               && this.m_tBDay[m] == this.m_nDay
               && (this.m_tBA[m] == this.m_nPlayer || this.m_tBB[m] == this.m_nPlayer)) {
               return m;
            }
         }
      }

      return -1;
   }

   // prochain match du joueur : {équipe qui reçoit, invitée} (index locaux), null s'il n'en a plus
   int[] GetPlayerNext() {
      int f = this.GetPlayerFixture();
      if (f >= 0) {
         return new int[]{this.m_tFHost[f], this.m_tFGuest[f]};
      } else {
         int m = this.GetPlayerMatch();
         return m >= 0 ? new int[]{this.GetHost(m), this.GetGuest(m)} : null;
      }
   }

   boolean IsPlayerQualified() {
      return this.m_nPhase != _PHASE_LEAGUE && this.GetSeed(this.m_nPlayer) < this.m_tSeed.length;
   }

   // le prochain match du joueur, s'il le gagne, lui donne le titre
   boolean IsTitleMatch() {
      int m = this.GetPlayerMatch();
      if (m < 0) {
         return false;
      }

      int t = this.m_tBType[m];
      if (t == _GF2) {
         return true;
      } else if (t == _GF) {
         return !this.m_bReset || this.m_tBA[m] == this.m_nPlayer;
      } else {
         return t == _WB && this.m_tSeed.length == 2;
      }
   }

   // ================================================================ simulation

   float Lambda(int a, int b, boolean bHome) {
      float fDiff = (this.m_tRating[a] - this.m_tRating[b]) / 100.0F;
      float l = this.m_fGoalsPerMatch * (float)Math.exp(fDiff * this.m_fStrengthEffect);
      return bHome ? l * (1.0F + this.m_fHomeBonus) : l;
   }

   int Poisson(float l) {
      double fLimit = Math.exp(-l);
      double p = 1.0;
      int k = 0;

      do {
         k++;
         p *= this.m_cRandom.nextDouble();
      } while (p > fLimit && k < 30);

      return k - 1;
   }

   void SimFixture(int f) {
      int h = this.m_tFHost[f];
      int g = this.m_tFGuest[f];
      this.m_tFGH[f] = this.Poisson(this.Lambda(h, g, true));
      this.m_tFGG[f] = this.Poisson(this.Lambda(g, h, false));
      this.m_tFPlayed[f] = true;
   }

   void SimMatch(int m) {
      int h = this.GetHost(m);
      int g = this.GetGuest(m);
      boolean bNeutral = this.m_tBType[m] == _GF || this.m_tBType[m] == _GF2;
      float lh = this.Lambda(h, g, !bNeutral);
      float lg = this.Lambda(g, h, false);
      int gh = this.Poisson(lh);
      int gg = this.Poisson(lg);
      if (gh == gg) {
         // but en or
         if (this.m_cRandom.nextFloat() < lh / (lh + lg)) {
            gh++;
         } else {
            gg++;
         }
      }

      this.m_tBGA[m] = this.m_tBHostA[m] ? gh : gg;
      this.m_tBGB[m] = this.m_tBHostA[m] ? gg : gh;
      this.m_tBPlayed[m] = true;
   }

   // ================================================================ classement

   // par équipe : joués, gagnés, nuls, perdus, buts pour, buts contre, points
   int[][] GetStats() {
      int[][] t = new int[this.m_nTeams][7];

      for (int f = 0; f < this.m_nFixtures; f++) {
         if (this.m_tFPlayed[f]) {
            this.AddResult(t[this.m_tFHost[f]], this.m_tFGH[f], this.m_tFGG[f]);
            this.AddResult(t[this.m_tFGuest[f]], this.m_tFGG[f], this.m_tFGH[f]);
         }
      }

      return t;
   }

   void AddResult(int[] s, int nFor, int nAgainst) {
      s[0]++;
      if (nFor > nAgainst) {
         s[1]++;
         s[6] += this.m_nPointsWin;
      } else if (nFor == nAgainst) {
         s[2]++;
         s[6] += this.m_nPointsDraw;
      } else {
         s[3]++;
      }

      s[4] += nFor;
      s[5] += nAgainst;
   }

   // a passe devant b : points, différence de buts, buts marqués, victoires, tirage au sort
   boolean Before(int[][] st, int a, int b) {
      int[] x = st[a];
      int[] y = st[b];
      if (x[6] != y[6]) {
         return x[6] > y[6];
      } else if (x[4] - x[5] != y[4] - y[5]) {
         return x[4] - x[5] > y[4] - y[5];
      } else if (x[4] != y[4]) {
         return x[4] > y[4];
      } else if (x[1] != y[1]) {
         return x[1] > y[1];
      } else {
         return this.m_tLots[a] < this.m_tLots[b];
      }
   }

   int[] GetRanking() {
      int[][] st = this.GetStats();
      int[] t = new int[this.m_nTeams];

      for (int i = 0; i < t.length; i++) {
         t[i] = i;
      }

      for (int i = 1; i < t.length; i++) {
         int v = t[i];
         int j = i - 1;

         while (j >= 0 && this.Before(st, v, t[j])) {
            t[j + 1] = t[j];
            j--;
         }

         t[j + 1] = v;
      }

      return t;
   }

   int GetRank(int nTeam) {
      int[] t = this.GetRanking();

      for (int i = 0; i < t.length; i++) {
         if (t[i] == nTeam) {
            return i;
         }
      }

      return -1;
   }

   // ligue en ligne : cette équipe est jouée par un autre joueur que moi
   boolean IsOtherHuman(int nTeam) {
      return this.m_tHuman != null && nTeam >= 0 && nTeam < this.m_tHuman.length && this.m_tHuman[nTeam] && nTeam != this.m_nPlayer;
   }

   int FindTeam(int nAnimal) {
      for (int i = 0; i < this.m_nTeams; i++) {
         if (this.m_tTeam[i] == nAnimal) {
            return i;
         }
      }

      return -1;
   }

   // ================================================================ triches (Cheat)

   // finit la ligue (matchs du joueur simulés aussi), le joueur à la place nPlace (0 = premier)
   void CheatFinishLeague(int nPlace) {
      if (this.m_nPhase != _PHASE_LEAGUE) {
         return;
      }

      for (int f = 0; f < this.m_nFixtures; f++) {
         if (!this.m_tFPlayed[f]) {
            this.SimFixture(f);
         }
      }

      this.m_nRound = this.m_nRounds;
      int[] tRank = this.GetRanking();
      nPlace = Math.max(0, Math.min(nPlace, this.m_nTeams - 1));
      int nOther = tRank[nPlace];
      if (nOther != this.m_nPlayer) {
         // échange les résultats des deux équipes (calendrier identique à une permutation près)
         for (int f = 0; f < this.m_nFixtures; f++) {
            this.m_tFHost[f] = this.Swap(this.m_tFHost[f], nOther);
            this.m_tFGuest[f] = this.Swap(this.m_tFGuest[f], nOther);
         }

         int k = this.m_tLots[nOther];
         this.m_tLots[nOther] = this.m_tLots[this.m_nPlayer];
         this.m_tLots[this.m_nPlayer] = k;
      }

      this.Advance();
   }

   int Swap(int t, int nOther) {
      if (t == this.m_nPlayer) {
         return nOther;
      } else {
         return t == nOther ? this.m_nPlayer : t;
      }
   }

   // ================================================================ sauvegarde (une suite d'entiers)

   int[] Save() {
      Vector v = new Vector();
      Put(v, _VERSION);
      Put(v, this.m_nPointsWin);
      Put(v, this.m_nPointsDraw);
      Put(v, this.m_bReset ? 1 : 0);
      Put(v, this.m_nQualifiers);
      Put(v, this.m_nTeams);
      Put(v, this.m_nPlayer);
      Put(v, this.m_nPhase);
      Put(v, this.m_nRound);
      Put(v, this.m_nRounds);
      Put(v, this.m_nDay);
      Put(v, this.m_nChampion);

      for (int i = 0; i < this.m_nTeams; i++) {
         Put(v, this.m_tTeam[i]);
         Put(v, this.m_tRating[i]);
         Put(v, this.m_tLots[i]);
      }

      Put(v, this.m_nFixtures);

      for (int f = 0; f < this.m_nFixtures; f++) {
         Put(v, this.m_tFRound[f]);
         Put(v, this.m_tFHost[f]);
         Put(v, this.m_tFGuest[f]);
         Put(v, this.m_tFGH[f]);
         Put(v, this.m_tFGG[f]);
         Put(v, this.m_tFPlayed[f] ? 1 : 0);
      }

      Put(v, this.m_tSeed.length);

      for (int i = 0; i < this.m_tSeed.length; i++) {
         Put(v, this.m_tSeed[i]);
      }

      Put(v, this.m_nMatches);

      for (int m = 0; m < this.m_nMatches; m++) {
         Put(v, this.m_tBType[m]);
         Put(v, this.m_tBRound[m]);
         Put(v, this.m_tBIndex[m]);
         Put(v, this.m_tBSrcA[m]);
         Put(v, this.m_tBSrcB[m]);
         Put(v, this.m_tBDay[m]);
         Put(v, this.m_tBA[m]);
         Put(v, this.m_tBB[m]);
         Put(v, this.m_tBGA[m]);
         Put(v, this.m_tBGB[m]);
         Put(v, (this.m_tBHostA[m] ? 1 : 0) | (this.m_tBPlayed[m] ? 2 : 0) | (this.m_tBActive[m] ? 4 : 0));
      }

      int[] t = new int[v.size()];

      for (int i = 0; i < t.length; i++) {
         t[i] = ((Integer)v.elementAt(i)).intValue();
      }

      return t;
   }

   static void Put(Vector v, int n) {
      v.addElement(new Integer(n));
   }

   // false si les données sont illisibles (l'objet est alors à jeter)
   boolean Load(int[] t) {
      try {
         int[] p = new int[]{0};
         if (Get(t, p) != _VERSION) {
            return false;
         }

         this.m_nPointsWin = Get(t, p);
         this.m_nPointsDraw = Get(t, p);
         this.m_bReset = Get(t, p) != 0;
         this.m_nQualifiers = Get(t, p);
         this.m_nTeams = Get(t, p);
         this.m_nPlayer = Get(t, p);
         this.m_nPhase = Get(t, p);
         this.m_nRound = Get(t, p);
         this.m_nRounds = Get(t, p);
         this.m_nDay = Get(t, p);
         this.m_nChampion = Get(t, p);
         int n = this.m_nTeams;
         this.m_tTeam = new int[n];
         this.m_tRating = new int[n];
         this.m_tLots = new int[n];

         for (int i = 0; i < n; i++) {
            this.m_tTeam[i] = Get(t, p);
            this.m_tRating[i] = Get(t, p);
            this.m_tLots[i] = Get(t, p);
         }

         int nf = this.m_nFixtures = Get(t, p);
         this.m_tFRound = new int[nf];
         this.m_tFHost = new int[nf];
         this.m_tFGuest = new int[nf];
         this.m_tFGH = new int[nf];
         this.m_tFGG = new int[nf];
         this.m_tFPlayed = new boolean[nf];

         for (int f = 0; f < nf; f++) {
            this.m_tFRound[f] = Get(t, p);
            this.m_tFHost[f] = Get(t, p);
            this.m_tFGuest[f] = Get(t, p);
            this.m_tFGH[f] = Get(t, p);
            this.m_tFGG[f] = Get(t, p);
            this.m_tFPlayed[f] = Get(t, p) != 0;
         }

         this.m_tSeed = new int[Get(t, p)];

         for (int i = 0; i < this.m_tSeed.length; i++) {
            this.m_tSeed[i] = Get(t, p);
         }

         int nm = Get(t, p);
         this.AllocBracket(nm);
         this.m_nMatches = nm;

         for (int m = 0; m < nm; m++) {
            this.m_tBType[m] = Get(t, p);
            this.m_tBRound[m] = Get(t, p);
            this.m_tBIndex[m] = Get(t, p);
            this.m_tBSrcA[m] = Get(t, p);
            this.m_tBSrcB[m] = Get(t, p);
            this.m_tBDay[m] = Get(t, p);
            this.m_tBA[m] = Get(t, p);
            this.m_tBB[m] = Get(t, p);
            this.m_tBGA[m] = Get(t, p);
            this.m_tBGB[m] = Get(t, p);
            int nFlags = Get(t, p);
            this.m_tBHostA[m] = (nFlags & 1) != 0;
            this.m_tBPlayed[m] = (nFlags & 2) != 0;
            this.m_tBActive[m] = (nFlags & 4) != 0;
         }

         return p[0] == t.length && this.m_nPlayer >= 0 && this.m_nPlayer < n;
      } catch (RuntimeException e) {
         return false;
      }
   }

   static int Get(int[] t, int[] p) {
      return t[p[0]++];
   }
}
