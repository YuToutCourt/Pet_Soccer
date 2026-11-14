import java.util.Random;

// [mod] Survie : des matchs enchaînés tant qu'on ne perd pas. Chaque manche réussie fait monter l'IA d'un niveau
// (1 -> 10), puis, au-delà du niveau max, l'IA gagne en vitesse et en puissance de tir à chaque manche.
// Réglages dans data\survival.scr, record calculé sur l'historique (ModStats), voir notes/survival.md.
// Une série ne vit qu'en mémoire : quitter le jeu entre deux manches l'arrête (le record, lui, est dans stats.dat).
class ModSurvival {
   static final String _FILE = "data\\survival.scr";
   // réglages (survival.scr)
   private static boolean m_bLoaded = false;
   private static int m_nStartLevel = 1;
   private static int m_nMaxLevel = 10;
   private static int m_nWavesPerLevel = 1;
   private static float m_fSpeedPerWave = 0.03F;
   private static float m_fShotPowerPerWave = 0.04F;
   private static float m_fMaxSpeedMul = 1.5F;
   private static float m_fMaxShotPowerMul = 1.6F;
   private static boolean m_bGoldenGoal = true;
   private static boolean m_bRated = false;
   // série
   private static boolean m_bRunning = false; // série en cours, y compris entre deux matchs
   private static boolean m_bOver = false; // la série vient de s'arrêter (écran de fin)
   private static int m_nTeam = 0;
   private static int m_nClothes = 0;
   private static int m_nWave = 1; // manche à jouer (ou en train d'être jouée)
   private static int m_nCleared = 0; // manches réussies dans la série
   private static int m_nOpponent = -1;
   private static int[] m_tOrder = null; // adversaires mélangés, chacun une fois avant de revoir le premier
   private static int m_nOrderPos = 0;
   private static int m_nRecordAtStart = 0;
   private static Random m_cRandom = new Random();
   // dernier match de la série (écran Survie et fin de match)
   static boolean m_bHasLast = false;
   static boolean m_bLastCleared = false;
   static boolean m_bLastAbandon = false;
   static boolean m_bLastGolden = false;
   static int m_nLastFor = 0;
   static int m_nLastAgainst = 0;
   static int m_nLastOpponent = -1;
   static int m_nLastWave = 0;

   static void Load() {
      if (m_bLoaded) {
         return;
      }

      m_bLoaded = true;
      ParseKeyword root = GameObject.ParseFile(_FILE);
      if (root == null) {
         GameObject.CrashLog("[mod] " + _FILE + " absent, reglages de survie par defaut\n");
         return;
      }

      for (ParseKeyword key = root; key != null; key = key.GetNext()) {
         if (key.Equals("StartLevel")) {
            m_nStartLevel = key.GetIParam(0);
         } else if (key.Equals("MaxLevel")) {
            m_nMaxLevel = key.GetIParam(0);
         } else if (key.Equals("WavesPerLevel")) {
            m_nWavesPerLevel = key.GetIParam(0);
         } else if (key.Equals("SpeedPerWave")) {
            m_fSpeedPerWave = key.GetFParam(0);
         } else if (key.Equals("ShotPowerPerWave")) {
            m_fShotPowerPerWave = key.GetFParam(0);
         } else if (key.Equals("MaxSpeedMul")) {
            m_fMaxSpeedMul = key.GetFParam(0);
         } else if (key.Equals("MaxShotPowerMul")) {
            m_fMaxShotPowerMul = key.GetFParam(0);
         } else if (key.Equals("GoldenGoal")) {
            m_bGoldenGoal = key.GetIParam(0) != 0;
         } else if (key.Equals("Rated")) {
            m_bRated = key.GetIParam(0) != 0;
         }
      }

      m_nMaxLevel = Math.max(1, Math.min(m_nMaxLevel, ModDifficulty._MAX_LEVEL));
      m_nStartLevel = Math.max(1, Math.min(m_nStartLevel, m_nMaxLevel));
      m_nWavesPerLevel = Math.max(1, m_nWavesPerLevel);
      GameObject.CrashLog("[mod] survie : niveaux " + m_nStartLevel + " -> " + m_nMaxLevel + ", " + m_nWavesPerLevel + " manche(s) par niveau\n");
   }

   // ---------------------------------------------------------------- état

   // un match de survie (ou l'écran Survie) est en cours
   static boolean IsActive() {
      return CoreLogic.m_bSurvival && m_bRunning && !CoreLogic.m_bDemo && !CoreLogic.m_bMultiplayer;
   }

   static boolean IsRunning() {
      return m_bRunning;
   }

   static boolean IsOver() {
      return m_bOver;
   }

   static int GetWave() {
      return m_nWave;
   }

   static int GetCleared() {
      return m_nCleared;
   }

   static int GetTeam() {
      return m_nTeam;
   }

   static int GetOpponent() {
      return m_nOpponent;
   }

   // ---------------------------------------------------------------- difficulté

   // niveau de l'IA pour une manche (1 = première)
   static int GetLevel(int nWave) {
      Load();
      return Math.min(m_nMaxLevel, m_nStartLevel + (nWave - 1) / m_nWavesPerLevel);
   }

   static int GetLevel() {
      return GetLevel(m_nWave);
   }

   // manches jouées au-delà du niveau max (0 avant) : chacune renforce l'IA
   static int GetExtraWaves(int nWave) {
      Load();
      int nFirstExtra = (m_nMaxLevel - m_nStartLevel + 1) * m_nWavesPerLevel + 1;
      return Math.max(0, nWave - nFirstExtra + 1);
   }

   static float GetSpeedMul(int nWave) {
      return 1.0F + Math.min(GetExtraWaves(nWave) * m_fSpeedPerWave, m_fMaxSpeedMul - 1.0F);
   }

   static float GetShotPowerMul(int nWave) {
      return 1.0F + Math.min(GetExtraWaves(nWave) * m_fShotPowerPerWave, m_fMaxShotPowerMul - 1.0F);
   }

   // multiplicateurs des équipes IA pendant un match de survie (ModDifficulty), 1.0 sinon
   static float GetAISpeedMul() {
      return IsActive() ? GetSpeedMul(m_nWave) : 1.0F;
   }

   static float GetAIShotPowerMul() {
      return IsActive() ? GetShotPowerMul(m_nWave) : 1.0F;
   }

   // égalité à la fin du temps réglementaire : but en or (Match.Update)
   static boolean IsGoldenGoal() {
      Load();
      return IsActive() && m_bGoldenGoal;
   }

   // Elo : seulement si Rated(1), et seulement tant que l'IA n'est pas renforcée (son Elo ne veut plus rien dire)
   static boolean IsRated() {
      Load();
      return m_bRated && GetExtraWaves(m_nWave) == 0;
   }

   // ---------------------------------------------------------------- série

   static void StartRun(int nTeam, int nClothes) {
      Load();
      m_bRunning = true;
      m_bOver = false;
      m_bHasLast = false;
      m_nTeam = nTeam;
      m_nClothes = nClothes;
      m_nWave = 1;
      m_nCleared = 0;
      m_nRecordAtStart = GetRecord();
      ShuffleOpponents();
      m_nOpponent = NextOpponent();
      GameObject.CrashLog("[mod] survie : nouvelle serie avec " + ETeams.GetTeamData(nTeam).m_sName + ", record " + m_nRecordAtStart + "\n");
   }

   // relance une série avec la même équipe
   static void Restart() {
      StartRun(m_nTeam, m_nClothes);
   }

   // abandon entre deux manches (bouton retour de l'écran Survie)
   static void Stop() {
      if (m_bRunning) {
         GameObject.CrashLog("[mod] survie : serie arretee a " + m_nCleared + " manche(s)\n");
      }

      m_bRunning = false;
      m_bOver = false;
      m_bHasLast = false;
   }

   private static void ShuffleOpponents() {
      int nCount = EAnimals._ANIMALS_COUNT - 1;
      m_tOrder = new int[nCount];
      int n = 0;

      for (int i = 0; i < EAnimals._ANIMALS_COUNT; i++) {
         if (i != m_nTeam) {
            m_tOrder[n++] = i;
         }
      }

      for (int i = nCount - 1; i > 0; i--) {
         int j = m_cRandom.nextInt(i + 1);
         int t = m_tOrder[i];
         m_tOrder[i] = m_tOrder[j];
         m_tOrder[j] = t;
      }

      m_nOrderPos = 0;
   }

   private static int NextOpponent() {
      if (m_nOrderPos >= m_tOrder.length) {
         int nLast = m_nOpponent;
         ShuffleOpponents();
         // pas deux fois de suite le même adversaire entre deux tours
         if (m_tOrder.length > 1 && m_tOrder[0] == nLast) {
            m_tOrder[0] = m_tOrder[1];
            m_tOrder[1] = nLast;
         }
      }

      return m_tOrder[m_nOrderPos++];
   }

   // prépare la manche : équipes, stade de l'adversaire, nouvelles données de match (match amical)
   static void PrepareMatch() {
      TeamData cMine = ETeams.GetTeamData(m_nTeam);
      cMine.SetClothesId(m_nClothes);
      CoreLogic.m_cTeamDataLeft = cMine;
      CoreLogic.m_cTeamDataRight = ETeams.GetTeamData(m_nOpponent);
      CoreLogic.m_nNextLevel = ETeams.GetHomeField(m_nOpponent);
      Match.m_cMatchData = null;
   }

   // AIMatch.AfterMatch, après ModElo et ModStats (qui lisent encore la manche en cours)
   static void OnMatchEnd(AIMatch cMatch) {
      if (!IsActive()) {
         return;
      }

      int nHostTeam = cMatch.m_nHalfNo != 0 && !cMatch.m_bGoldGoal ? 1 : 0;
      boolean bPlayerHost = cMatch.GetTeam(nHostTeam).m_bUserControled;
      int nFor = bPlayerHost ? cMatch.m_nScoreLeft : cMatch.m_nScoreRight;
      int nAgainst = bPlayerHost ? cMatch.m_nScoreRight : cMatch.m_nScoreLeft;
      SetLast(nFor, nAgainst, false, cMatch.m_bGoldGoal);
      if (nFor >= nAgainst) {
         // une égalité (seulement sans but en or) ne fait pas perdre : la série continue
         m_nCleared++;
         m_nWave++;
         m_nOpponent = NextOpponent();
         GameObject.CrashLog("[mod] survie : manche " + m_nLastWave + " reussie " + nFor + "-" + nAgainst + ", manche " + m_nWave + " niveau " + GetLevel() + "\n");
      } else {
         End();
      }
   }

   // MenuInGame : quitter ou recommencer un match en cours = défaite, la série s'arrête
   static void OnAbandon() {
      if (!IsActive()) {
         return;
      }

      AIMatch cMatch = SoccerModule.m_cMatch;
      int nFor = 0;
      int nAgainst = 0;
      if (cMatch != null) {
         int nHostTeam = cMatch.m_nHalfNo != 0 && !cMatch.m_bGoldGoal ? 1 : 0;
         boolean bPlayerHost = cMatch.GetTeam(nHostTeam).m_bUserControled;
         nFor = bPlayerHost ? cMatch.m_nScoreLeft : cMatch.m_nScoreRight;
         nAgainst = bPlayerHost ? cMatch.m_nScoreRight : cMatch.m_nScoreLeft;
      }

      SetLast(nFor, nAgainst, true, false);
      End();
   }

   private static void SetLast(int nFor, int nAgainst, boolean bAbandon, boolean bGolden) {
      m_bHasLast = true;
      m_nLastFor = nFor;
      m_nLastAgainst = nAgainst;
      m_bLastAbandon = bAbandon;
      m_bLastGolden = bGolden;
      m_nLastOpponent = m_nOpponent;
      m_nLastWave = m_nWave;
      m_bLastCleared = !bAbandon && nFor >= nAgainst;
   }

   private static void End() {
      m_bRunning = false;
      m_bOver = true;
      GameObject.CrashLog("[mod] survie : serie terminee a la manche " + m_nWave + ", " + m_nCleared + " manche(s) reussie(s), record " + GetRecord() + "\n");
   }

   static boolean IsNewRecord() {
      return m_nCleared > 0 && m_nCleared > m_nRecordAtStart;
   }

   // meilleur nombre de manches réussies dans une série : plus haute manche réussie de l'historique (hors triche)
   static int GetRecord() {
      java.util.Vector v = ModStats.GetHistory();
      int nBest = 0;

      for (int i = 0; i < v.size(); i++) {
         ModStats.Record r = (ModStats.Record)v.elementAt(i);
         int[] t = r.m_tValues;
         if (t[ModStats._MODE] == ModStats._MODE_SURVIVAL && t[ModStats._CHEAT] == 0 && ModStats.GetOutcome(r) >= 0) {
            nBest = Math.max(nBest, t[ModStats._SURVIVAL_WAVE]);
         }
      }

      return nBest;
   }

   // ---------------------------------------------------------------- textes

   // « 4 », « 9 (Maître) », « Légende +3 »
   static String GetLevelText(int nWave) {
      int nLevel = GetLevel(nWave);
      int nExtra = GetExtraWaves(nWave);
      String sName = nLevel >= 8 ? Text.Get("&MenuOptionsMatchDiff" + (nLevel - 5) + "&") : null;
      if (nExtra > 0) {
         return (sName != null ? sName : "" + nLevel) + " +" + nExtra;
      }

      return sName != null ? nLevel + " (" + sName + ")" : "" + nLevel;
   }

   // « +0 % », « +12 % »
   static String FormatBonus(float fMul) {
      return "+" + Math.round((fMul - 1.0F) * 100.0F) + " %";
   }

   // « 3-1 contre Gorilles »
   static String GetLastScoreText() {
      return m_nLastFor + "-" + m_nLastAgainst + " " + Text.Get("&SurvivalAgainst&") + " " + ETeams.GetTeamData(m_nLastOpponent).m_sName;
   }

   // « 3 manches réussies »
   static String GetClearedText(int n) {
      return n + " " + Text.Get(n > 1 ? "&SurvivalWavesCleared&" : "&SurvivalWaveCleared&");
   }
}
