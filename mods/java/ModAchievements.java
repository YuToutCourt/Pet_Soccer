import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

// [mod] Succès : conditions dans data\achievements.scr, progression calculée à partir de l'historique (ModStats)
// et de l'Elo, déblocages par profil dans achievements.dat (voir notes/achievements.md)
class ModAchievements {
   static final String _CONFIG_FILE = "data\\achievements.scr";
   static final String _SAVE_FILE = "achievements.dat";
   // conditions sans progression affichée (seuil qui n'est pas un compte)
   static final String[] _NO_PROGRESS = new String[]{"WinLevel", "CleanSheetLevel", "GoldenGoal", "LateWinner", "Rank"};

   static class Achievement {
      String m_sKey; // textes Ach_<clé> et Ach_<clé>_Desc de texts.scr, clé de achievements.dat
      String m_sCondition;
      int m_nTarget;
   }

   private static boolean m_bLoaded = false;
   private static java.util.Vector m_vList = new java.util.Vector();
   private static String m_sSound = null;
   private static Hashtable m_tUnlocked = new Hashtable(); // profil -> Hashtable (clé -> date « jj/mm/aa »)
   // succès débloqués par le dernier match, annoncés sur l'écran de fin de match
   static java.util.Vector m_vLastUnlocked = new java.util.Vector();

   static void Load() {
      if (m_bLoaded) {
         return;
      }

      m_bLoaded = true;
      ParseKeyword root = GameObject.ParseFile(_CONFIG_FILE);
      if (root == null) {
         GameObject.CrashLog("[mod] " + _CONFIG_FILE + " absent, aucun succes\n");
      }

      for (ParseKeyword key = root; key != null; key = key.GetNext()) {
         if (key.Equals("Achievement")) {
            Achievement a = new Achievement();
            a.m_sKey = key.GetSParam(0);
            a.m_sCondition = key.GetSParam(1);
            a.m_nTarget = key.GetIParam(2);
            m_vList.addElement(a);
         }

         if (key.Equals("Sound")) {
            m_sSound = key.GetSParam(0);
         }
      }

      LoadUnlocked();
      GameObject.CrashLog("[mod] succes charges : " + m_vList.size() + "\n");
   }

   static java.util.Vector GetList() {
      Load();
      return m_vList;
   }

   static String GetName(Achievement a) {
      return Text.Get("&Ach_" + a.m_sKey + "&");
   }

   static String GetDescription(Achievement a) {
      return Text.Get("&Ach_" + a.m_sKey + "_Desc&");
   }

   static String GetSound() {
      Load();
      return m_sSound;
   }

   // ---------------------------------------------------------------- progression

   // valeur atteinte pour chaque condition (Integer), calculée sur tout l'historique du profil.
   // Les matchs avec triche (menu Mods) ne comptent pas ; un abandon est une défaite.
   static Hashtable GetProgress() {
      java.util.Vector vHistory = ModStats.GetHistory();
      int nMatches = 0;
      int nWins = 0;
      int nGoals = 0;
      int nCleanSheets = 0;
      int nExtraWins = 0;
      int nStreak = 0;
      int nBestStreak = 0;
      int nUnbeaten = 0;
      int nBestUnbeaten = 0;
      int nSuperTypes = 0;
      int nCups = 0;
      Hashtable tTeamsWon = new Hashtable();
      Hashtable tTeamsBeaten = new Hashtable();
      // meilleure valeur sur un seul match
      int nWinLevel = 0;
      int nMatchGoals = 0;
      int nCleanSheetLevel = 0;
      int nMargin = 0;
      int nHatTrick = 0;
      int nComeback = 0;
      int nGoldenGoal = 0;
      int nLateWinner = 0;
      int nPossession = 0;
      int nPasses = 0;
      int nSaves = 0;
      int nSupers = 0;
      int nSurvival = 0; // [mod] manches de survie réussies dans une même série (record)

      for (int i = 0; i < vHistory.size(); i++) {
         ModStats.Record r = (ModStats.Record)vHistory.elementAt(i);
         int[] v = r.m_tValues;
         if (v[ModStats._CHEAT] == 1) {
            continue;
         }

         int nOutcome = ModStats.GetOutcome(r);
         if (nOutcome > 0) {
            nStreak++;
            nBestStreak = Math.max(nBestStreak, nStreak);
         } else {
            nStreak = 0;
         }

         if (nOutcome >= 0) {
            nUnbeaten++;
            nBestUnbeaten = Math.max(nBestUnbeaten, nUnbeaten);
         } else {
            nUnbeaten = 0;
         }

         if (v[ModStats._ABANDON] == 1) {
            continue;
         }

         nMatches++;
         nGoals += v[ModStats._GOALS_FOR];
         nSuperTypes |= v[ModStats._SUPER_TYPES];
         if (v[ModStats._CUP_WON] > 0) {
            nCups |= 1 << v[ModStats._CUP_WON] - 1;
         }

         nMatchGoals = Math.max(nMatchGoals, v[ModStats._GOALS_FOR]);
         nHatTrick = Math.max(nHatTrick, v[ModStats._TOP_SCORER_GOALS]);
         nPasses = Math.max(nPasses, v[ModStats._PASSES_OK]);
         nSaves = Math.max(nSaves, v[ModStats._SAVES]);
         nSupers = Math.max(nSupers, v[ModStats._SUPERS]);
         if (v[ModStats._MODE] == ModStats._MODE_SURVIVAL && nOutcome >= 0) {
            nSurvival = Math.max(nSurvival, v[ModStats._SURVIVAL_WAVE]);
         }

         if (nOutcome > 0) {
            nWins++;
            tTeamsWon.put(new Integer(v[ModStats._MY_TEAM]), Boolean.TRUE);
            tTeamsBeaten.put(new Integer(v[ModStats._OPP_TEAM]), Boolean.TRUE);
            if (v[ModStats._MODE] == ModStats._MODE_EXTRA) {
               nExtraWins++;
            }

            nWinLevel = Math.max(nWinLevel, v[ModStats._LEVEL]);
            nMargin = Math.max(nMargin, v[ModStats._GOALS_FOR] - v[ModStats._GOALS_AGAINST]);
            nComeback = Math.max(nComeback, v[ModStats._MAX_DEFICIT]);
            nLateWinner = Math.max(nLateWinner, v[ModStats._WINNER_MINUTE]);
            nPossession = Math.max(nPossession, v[ModStats._POSSESSION]);
            if (v[ModStats._GOLDEN_GOAL] == 1) {
               nGoldenGoal = 1;
            }

            if (v[ModStats._GOALS_AGAINST] == 0) {
               nCleanSheets++;
               nCleanSheetLevel = Math.max(nCleanSheetLevel, v[ModStats._LEVEL]);
            }
         }
      }

      Hashtable t = new Hashtable();
      Put(t, "Matches", nMatches);
      Put(t, "Wins", nWins);
      Put(t, "Goals", nGoals);
      Put(t, "CleanSheets", nCleanSheets);
      Put(t, "ExtraWins", nExtraWins);
      Put(t, "WinStreak", nBestStreak);
      Put(t, "Unbeaten", nBestUnbeaten);
      Put(t, "SuperTypes", BitCount(nSuperTypes));
      Put(t, "Cups", BitCount(nCups));
      Put(t, "TeamsWon", tTeamsWon.size());
      Put(t, "TeamsBeaten", tTeamsBeaten.size());
      Put(t, "WinLevel", nWinLevel);
      Put(t, "MatchGoals", nMatchGoals);
      Put(t, "CleanSheetLevel", nCleanSheetLevel);
      Put(t, "Margin", nMargin);
      Put(t, "HatTrick", nHatTrick);
      Put(t, "Comeback", nComeback);
      Put(t, "GoldenGoal", nGoldenGoal);
      Put(t, "LateWinner", nLateWinner);
      Put(t, "Possession", nPossession);
      Put(t, "Passes", nPasses);
      Put(t, "Saves", nSaves);
      Put(t, "Supers", nSupers);
      Put(t, "Rank", ModElo.GetPeakRank());
      Put(t, "SurvivalWaves", nSurvival);
      return t;
   }

   private static void Put(Hashtable t, String sKey, int n) {
      t.put(sKey, new Integer(n));
   }

   private static int BitCount(int n) {
      int nCount = 0;

      for (; n != 0; n &= n - 1) {
         nCount++;
      }

      return nCount;
   }

   static int GetValue(Hashtable tProgress, Achievement a) {
      Integer n = (Integer)tProgress.get(a.m_sCondition);
      if (n == null) {
         GameObject.CrashLog("[mod] succes " + a.m_sKey + " : condition inconnue " + a.m_sCondition + "\n");
         return 0;
      }

      return n.intValue();
   }

   // « 7/10 » pour un succès bloqué qui se compte, sinon ""
   static String GetProgressText(Hashtable tProgress, Achievement a) {
      if (a.m_nTarget <= 1) {
         return "";
      }

      for (int i = 0; i < _NO_PROGRESS.length; i++) {
         if (_NO_PROGRESS[i].equals(a.m_sCondition)) {
            return "";
         }
      }

      return Math.min(GetValue(tProgress, a), a.m_nTarget) + "/" + a.m_nTarget;
   }

   // ---------------------------------------------------------------- déblocages

   private static Hashtable GetUnlocked() {
      Load();
      String sName = ModStats.GetProfileName();
      Hashtable t = (Hashtable)m_tUnlocked.get(sName);
      if (t == null) {
         t = new Hashtable();
         m_tUnlocked.put(sName, t);
      }

      return t;
   }

   // date de déblocage (« jj/mm/aa ») ou null
   static String GetUnlockDate(Achievement a) {
      return (String)GetUnlocked().get(a.m_sKey);
   }

   // « 10/11/26 » -> « 10 nov. » (mois dans AchMonths de texts.scr)
   static String FormatDate(String sDate) {
      StringTokenizer tok = new StringTokenizer(sDate, "/");
      StringTokenizer months = new StringTokenizer(Text.Get("&AchMonths&"), " ");
      if (tok.countTokens() < 2 || months.countTokens() != 12) {
         return sDate;
      }

      try {
         String sDay = tok.nextToken();
         int nMonth = Integer.parseInt(tok.nextToken());
         String sMonth = "";

         for (int i = 1; i <= nMonth && months.hasMoreTokens(); i++) {
            sMonth = months.nextToken();
         }

         return Integer.parseInt(sDay) + " " + sMonth;
      } catch (Exception e) {
         return sDate;
      }
   }

   static int CountUnlocked() {
      Hashtable t = GetUnlocked();
      int n = 0;

      for (int i = 0; i < GetList().size(); i++) {
         if (t.get(((Achievement)m_vList.elementAt(i)).m_sKey) != null) {
            n++;
         }
      }

      return n;
   }

   // débloque ce qui est atteint. bAnnounce : fin de match (bandeau), sinon déblocage silencieux
   // (menu principal, écran Succès : rattrape l'historique joué avant le mod ou un fichier supprimé)
   static void Evaluate(boolean bAnnounce) {
      if (CoreLogic.GetPlayer() == null) {
         return;
      }

      Hashtable tProgress = GetProgress();
      Hashtable tUnlocked = GetUnlocked();
      String sDate = new SimpleDateFormat("dd/MM/yy").format(new Date());
      boolean bChanged = false;

      for (int i = 0; i < GetList().size(); i++) {
         Achievement a = (Achievement)m_vList.elementAt(i);
         if (tUnlocked.get(a.m_sKey) == null && GetValue(tProgress, a) >= a.m_nTarget) {
            tUnlocked.put(a.m_sKey, sDate);
            bChanged = true;
            if (bAnnounce) {
               m_vLastUnlocked.addElement(a);
            }

            GameObject.CrashLog("[mod] succes debloque (" + ModStats.GetProfileName() + ") : " + a.m_sKey + (bAnnounce ? "" : " (rattrapage)") + "\n");
         }
      }

      if (bChanged) {
         Save();
      }
   }

   // appelé par ModStats.Reset (début ou reprise d'un match)
   static void Reset() {
      m_vLastUnlocked.removeAllElements();
   }

   // appelé par ModStats.OnMatchEnd, une fois le match ajouté à l'historique
   static void OnMatchEnd() {
      m_vLastUnlocked.removeAllElements();
      Evaluate(true);
   }

   // achievements.dat : une ligne par succès débloqué, « profil;clé;date »
   private static void LoadUnlocked() {
      File f = new File(_SAVE_FILE);
      if (!f.exists()) {
         return;
      }

      try {
         BufferedReader in = new BufferedReader(new FileReader(f));

         for (String sLine = in.readLine(); sLine != null; sLine = in.readLine()) {
            StringTokenizer tok = new StringTokenizer(sLine, ";");
            if (tok.countTokens() == 3) {
               String sName = tok.nextToken();
               Hashtable t = (Hashtable)m_tUnlocked.get(sName);
               if (t == null) {
                  t = new Hashtable();
                  m_tUnlocked.put(sName, t);
               }

               String sKey = tok.nextToken();
               t.put(sKey, tok.nextToken());
            }
         }

         in.close();
      } catch (Exception e) {
         GameObject.CrashLog("[mod] succes : lecture de " + _SAVE_FILE + " impossible : " + e + "\n");
      }
   }

   private static void Save() {
      try {
         FileWriter out = new FileWriter(_SAVE_FILE);
         Enumeration names = m_tUnlocked.keys();

         while (names.hasMoreElements()) {
            String sName = (String)names.nextElement();
            Hashtable t = (Hashtable)m_tUnlocked.get(sName);
            Enumeration keys = t.keys();

            while (keys.hasMoreElements()) {
               String sKey = (String)keys.nextElement();
               out.write(sName + ";" + sKey + ";" + t.get(sKey) + "\r\n");
            }
         }

         out.close();
      } catch (Exception e) {
         GameObject.CrashLog("[mod] succes : ecriture de " + _SAVE_FILE + " impossible : " + e + "\n");
      }
   }
}
