import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

// [mod] Classement Elo par profil, réglages dans data\elo.scr, sauvegarde dans elo.dat (voir notes/elo.md)
class ModElo {
   static final String _CONFIG_FILE = "data\\elo.scr";
   static final String _SAVE_FILE = "elo.dat";
   static final int _MAX_RANKS = 16;
   // champs d'un profil dans m_tRecords
   static final int _ELO = 0;
   static final int _GAMES = 1;
   static final int _WINS = 2;
   static final int _DRAWS = 3;
   static final int _LOSSES = 4;
   static final int _PEAK = 5;
   static final int _FIELDS = 6;
   private static boolean m_bLoaded = false;
   private static int m_nStart = 1000;
   private static float m_fK = 32.0F;
   private static int m_nPlacementGames = 10;
   private static float m_fPlacementK = 48.0F;
   private static boolean m_bAbandonIsLoss = true;
   private static int[] m_nAIElo = new int[]{0, 600, 750, 900, 1050, 1200, 1350, 1500, 1650, 1800, 2000};
   private static int m_nRankedOffset = 0;
   private static int m_nRanks = 0;
   private static int[] m_nRankMin = new int[_MAX_RANKS];
   private static float[][] m_fRankColor = new float[_MAX_RANKS][3];
   private static String[] m_sRankBadge = new String[_MAX_RANKS];
   private static Hashtable m_tRecords = new Hashtable();
   // résultat du dernier match, affiché sur l'écran de fin de match
   static boolean m_bLastRated = false;
   static int m_nLastDelta = 0;
   static int m_nLastOldElo = 0;
   static int m_nLastOldRank = 0;
   static boolean m_bLastAbandonRated = false; // l'abandon a compté (lu par ModStats)
   static boolean m_bLastCheat = false; // match non classé à cause d'une triche (menu Mods)

   static void Load() {
      if (m_bLoaded) {
         return;
      }

      m_bLoaded = true;
      ParseKeyword root = GameObject.ParseFile(_CONFIG_FILE);
      if (root == null) {
         GameObject.CrashLog("[mod] " + _CONFIG_FILE + " absent, reglages Elo par defaut\n");
      }

      for (ParseKeyword key = root; key != null; key = key.GetNext()) {
         if (key.Equals("Start")) {
            m_nStart = key.GetIParam(0);
         }

         if (key.Equals("K")) {
            m_fK = key.GetFParam(0);
         }

         if (key.Equals("Placement")) {
            m_nPlacementGames = key.GetIParam(0);
            m_fPlacementK = key.GetFParam(1);
         }

         if (key.Equals("AbandonIsLoss")) {
            m_bAbandonIsLoss = key.GetIParam(0) == 1;
         }

         if (key.Equals("RankedOffset")) {
            m_nRankedOffset = key.GetIParam(0);
         }

         if (key.Equals("AIElo")) {
            int nLevel = key.GetIParam(0);
            if (nLevel >= 1 && nLevel < m_nAIElo.length) {
               m_nAIElo[nLevel] = key.GetIParam(1);
            }
         }

         if (key.Equals("Rank") && m_nRanks < _MAX_RANKS) {
            m_nRankMin[m_nRanks] = key.GetIParam(0);
            m_fRankColor[m_nRanks][0] = key.GetFParam(1);
            m_fRankColor[m_nRanks][1] = key.GetFParam(2);
            m_fRankColor[m_nRanks][2] = key.GetFParam(3);
            m_nRanks++;
         }

         if (key.Equals("RankBadge")) {
            int nRank = key.GetIParam(0);
            if (nRank >= 0 && nRank < _MAX_RANKS) {
               m_sRankBadge[nRank] = key.GetSParam(1);
            }
         }
      }

      if (m_nRanks == 0) {
         m_nRankMin[0] = 0;
         m_fRankColor[0][0] = m_fRankColor[0][1] = m_fRankColor[0][2] = 1.0F;
         m_nRanks = 1;
      }

      LoadRecords();
      GameObject.CrashLog("[mod] Elo charge : " + m_tRecords.size() + " profil(s), " + m_nRanks + " rangs\n");
   }

   // elo.dat : une ligne par profil, "nom;elo;matchs;victoires;nuls;defaites;record"
   private static void LoadRecords() {
      File f = new File(_SAVE_FILE);
      if (!f.exists()) {
         return;
      }

      try {
         BufferedReader in = new BufferedReader(new FileReader(f));

         for (String sLine = in.readLine(); sLine != null; sLine = in.readLine()) {
            StringTokenizer tok = new StringTokenizer(sLine, ";");
            if (tok.countTokens() == _FIELDS + 1) {
               String sName = tok.nextToken();
               int[] tRecord = new int[_FIELDS];

               for (int i = 0; i < _FIELDS; i++) {
                  tRecord[i] = Integer.parseInt(tok.nextToken().trim());
               }

               m_tRecords.put(sName, tRecord);
            }
         }

         in.close();
      } catch (Exception e) {
         GameObject.CrashLog("[mod] Elo : lecture de " + _SAVE_FILE + " impossible : " + e + "\n");
      }
   }

   private static void SaveRecords() {
      try {
         FileWriter out = new FileWriter(_SAVE_FILE);
         Enumeration names = m_tRecords.keys();

         while (names.hasMoreElements()) {
            String sName = (String)names.nextElement();
            int[] tRecord = (int[])m_tRecords.get(sName);
            out.write(sName);

            for (int i = 0; i < _FIELDS; i++) {
               out.write(";" + tRecord[i]);
            }

            out.write("\r\n");
         }

         out.close();
      } catch (Exception e) {
         GameObject.CrashLog("[mod] Elo : ecriture de " + _SAVE_FILE + " impossible : " + e + "\n");
      }
   }

   private static String GetPlayerName() {
      PlayerSave player = CoreLogic.GetPlayer();
      if (player == null || player.m_Name == null) {
         return "default";
      }

      // ';' sert de séparateur dans elo.dat
      return player.m_Name.replace(';', ',');
   }

   private static int[] GetRecord() {
      Load();
      String sName = GetPlayerName();
      int[] tRecord = (int[])m_tRecords.get(sName);
      if (tRecord == null) {
         tRecord = new int[_FIELDS];
         tRecord[_ELO] = m_nStart;
         tRecord[_PEAK] = m_nStart;
         m_tRecords.put(sName, tRecord);
      }

      return tRecord;
   }

   // seuls les matchs d'un humain contre l'IA comptent
   private static boolean IsHumanVsAI() {
      return !CoreLogic.m_bDemo && !CoreLogic.m_bMultiplayer && SoccerModule.m_cMatch != null;
   }

   // ... et sans triche de match (menu Mods)
   private static boolean IsRatedMatch() {
      // [mod] survie : non classée par défaut (Rated dans survival.scr)
      return IsHumanVsAI() && !ModOptions.IsMatchCheat() && (!ModSurvival.IsActive() || ModSurvival.IsRated());
   }

   // appelé par AIMatch.AfterMatch (fin normale ou but en or)
   static void OnMatchEnd(AIMatch cMatch) {
      m_bLastRated = false;
      m_bLastCheat = IsHumanVsAI() && ModOptions.IsMatchCheat();
      if (IsRatedMatch()) {
         // m_nScoreLeft = hôte. Les index d'équipe suivent les côtés du terrain : après la mi-temps
         // (hors but en or) l'hôte est l'équipe 1 (même règle que MenuInGame.ShowInfo).
         // En championnat le joueur peut être l'invité.
         int nHostTeam = cMatch.m_nHalfNo != 0 && !cMatch.m_bGoldGoal ? 1 : 0;
         boolean bPlayerHost = cMatch.GetTeam(nHostTeam).m_bUserControled;
         int nFor = bPlayerHost ? cMatch.m_nScoreLeft : cMatch.m_nScoreRight;
         int nAgainst = bPlayerHost ? cMatch.m_nScoreRight : cMatch.m_nScoreLeft;
         Rate(nFor, nAgainst, "match " + nFor + "-" + nAgainst);
         m_bLastRated = true;
      }
   }

   // appelé par MenuInGame quand on quitte ou recommence un match en cours
   static void OnAbandon() {
      Load();
      m_bLastAbandonRated = false;
      if (m_bAbandonIsLoss && IsRatedMatch()) {
         Rate(0, 1, "abandon");
         m_bLastAbandonRated = true;
      }
   }

   private static void Rate(int nFor, int nAgainst, String sWhat) {
      int[] tRecord = GetRecord();
      int nLevel = Math.max(1, Math.min(CoreLogic.GetDiffLevel(), m_nAIElo.length - 1));
      int nAIElo = m_nAIElo[nLevel];
      float fExpected = (float)(1.0 / (1.0 + Math.pow(10.0, (nAIElo - tRecord[_ELO]) / 400.0)));
      float fScore = nFor > nAgainst ? 1.0F : (nFor == nAgainst ? 0.5F : 0.0F);
      // écart de buts : x1 jusqu'à 1 but, x1.5 à 2 buts, puis (11 + écart) / 8
      int nMargin = Math.abs(nFor - nAgainst);
      float fMarginMul = nMargin <= 1 ? 1.0F : (nMargin == 2 ? 1.5F : (11.0F + nMargin) / 8.0F);
      float fK = tRecord[_GAMES] < m_nPlacementGames ? m_fPlacementK : m_fK;
      int nDelta = Math.round(fK * fMarginMul * (fScore - fExpected));
      m_nLastOldElo = tRecord[_ELO];
      m_nLastOldRank = GetRankIndex(tRecord[_ELO]);
      m_nLastDelta = nDelta;
      tRecord[_ELO] += nDelta;
      tRecord[_GAMES]++;
      if (fScore == 1.0F) {
         tRecord[_WINS]++;
      } else if (fScore == 0.5F) {
         tRecord[_DRAWS]++;
      } else {
         tRecord[_LOSSES]++;
      }

      tRecord[_PEAK] = Math.max(tRecord[_PEAK], tRecord[_ELO]);
      SaveRecords();
      GameObject.CrashLog("[mod] Elo " + GetPlayerName() + " : " + sWhat + " contre IA niveau " + nLevel + " (" + nAIElo + ") -> "
         + tRecord[_ELO] + " (" + FormatDelta(nDelta) + ")\n");
   }

   // réglage Classé : niveau dont l'Elo est le plus proche de celui du profil (+ RankedOffset)
   static int GetRankedLevel() {
      int nTarget = GetRecord()[_ELO] + m_nRankedOffset;
      int nBest = 1;

      for (int i = 2; i < m_nAIElo.length; i++) {
         if (Math.abs(m_nAIElo[i] - nTarget) < Math.abs(m_nAIElo[nBest] - nTarget)) {
            nBest = i;
         }
      }

      return nBest;
   }

   static int GetRankIndex(int nElo) {
      Load();
      int nRank = 0;

      for (int i = 0; i < m_nRanks; i++) {
         if (nElo >= m_nRankMin[i]) {
            nRank = i;
         }
      }

      return nRank;
   }

   static String GetRankName(int nRank) {
      return Text.Get("&EloRank" + nRank + "&");
   }

   private static String FormatDelta(int nDelta) {
      return nDelta >= 0 ? "+" + nDelta : "" + nDelta;
   }

   // menu principal : « Argent - 1032 Elo (12V 3N 5D) »
   static String GetSummary() {
      int[] tRecord = GetRecord();
      return GetRankName(GetRankIndex(tRecord[_ELO])) + " - " + tRecord[_ELO] + " Elo  (" + tRecord[_WINS] + Text.Get("&EloWinsAbr&") + " "
         + tRecord[_DRAWS] + Text.Get("&EloDrawsAbr&") + " " + tRecord[_LOSSES] + Text.Get("&EloLossesAbr&") + ")";
   }

   static String FormatLastDelta() {
      return FormatDelta(m_nLastDelta);
   }

   static int GetLastNewElo() {
      return m_nLastOldElo + m_nLastDelta;
   }

   // colore le texte avec la couleur d'un rang
   static void SetRankColor(UIStatic cStatic, int nRank) {
      cStatic.SetTextColor(m_fRankColor[nRank][0], m_fRankColor[nRank][1], m_fRankColor[nRank][2]);
   }

   // colore le texte avec la couleur du rang actuel
   static void SetRankColor(UIStatic cStatic) {
      SetRankColor(cStatic, GetRank());
   }

   // rang actuel du profil
   static int GetRank() {
      return GetRankIndex(GetRecord()[_ELO]);
   }

   // meilleur rang atteint par le profil (succès)
   static int GetPeakRank() {
      return GetRankIndex(GetRecord()[_PEAK]);
   }

   // charge les badges de rang (RankBadge dans elo.scr) dans la fenêtre qui les affiche
   static UITexture[] LoadBadges(UIWindow cWnd) {
      Load();
      UITexture[] tBadges = new UITexture[m_nRanks];

      for (int i = 0; i < m_nRanks; i++) {
         if (m_sRankBadge[i] != null) {
            tBadges[i] = new UITexture(m_sRankBadge[i], 128.0F, 128.0F, 0.0F, 0.0F, 128.0F, 128.0F);
            tBadges[i].m_cWindow = cWnd;
            tBadges[i].m_nId = cWnd.AddTexture(m_sRankBadge[i]);
         }
      }

      return tBadges;
   }

   // affiche le badge d'un rang, ou masque la fenêtre (nRank < 0 ou pas de badge)
   static void SetBadge(UIWindow cWnd, UITexture[] tBadges, int nRank) {
      if (cWnd == null || tBadges == null) {
         return;
      }

      if (nRank >= 0 && nRank < tBadges.length && tBadges[nRank] != null) {
         cWnd.SetTexture(tBadges[nRank]);
         cWnd.SetTextureAlpha(1.0F);
      } else {
         cWnd.SetTextureAlpha(0.0F);
      }
   }
}
