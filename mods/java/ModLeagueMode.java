import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

// [mod] Mode Ligue (bouton « ligue » du menu principal), à côté du championnat d'origine qui ne change pas :
// ligue à 14 en aller simple, puis playoffs à double élimination pour les premiers (logique dans ModLeague).
// Indépendant des coupes : pas de déblocage, difficulté = réglage Options > Match.
// Comme la survie, le joueur joue toujours à gauche ; le match a lieu au stade de l'équipe qui reçoit.
// Résultat enregistré à la fin du match (AIMatch.AfterMatch -> OnMatchEnd). Réglages dans data\league.scr,
// tournoi en cours dans league.dat (une ligne par profil). Écrans : MenuLeague (classement), MenuLeagueBracket (tableau).
// Voir notes/league.md.
class ModLeagueMode {
   static final String _CONFIG_FILE = "data\\league.scr";
   static final String _SAVE_FILE = "league.dat";
   // réglages (league.scr)
   static boolean m_bConfigLoaded = false;
   static int m_nCfgQualifiers = 8;
   static boolean m_bCfgReset = false;
   static int m_nCfgPointsWin = 3;
   static int m_nCfgPointsDraw = 1;
   static float m_fCfgGoalsPerMinute = 0.2F;
   static float m_fCfgHomeBonus = 0.1F;
   static float m_fCfgStrengthEffect = 0.08F;
   static float m_fCfgForm = 2.0F;
   static float[] m_tCfgWeights = new float[]{1.0F, 1.0F, 1.0F};
   static Hashtable m_tCfgRatings = new Hashtable();
   // test sans souris : LeagueTest(n) dans debugconf.scr (MenuMain.LeagueTest), -1 = pas encore lu
   static int m_nDebugAutoStart = -1;
   // tournoi du profil courant
   static ModLeague m_cLeague = null;
   static String m_sOwner = null; // profil pour lequel m_cLeague a été lu
   static int m_nClothes = 0;
   // dernier match du joueur (écrans de la ligue)
   static boolean m_bHasLast = false; // un résultat à annoncer (consommé par les écrans)
   static int m_nLastResult = -1; // ModLeague._RES_*
   static boolean m_bLastLeague = false; // c'était un match de ligue (sinon de playoffs)
   static boolean m_bLastGolden = false;

   // ================================================================ réglages

   static void LoadConfig() {
      if (m_bConfigLoaded) {
         return;
      }

      m_bConfigLoaded = true;
      ParseKeyword root = GameObject.ParseFile(_CONFIG_FILE);
      if (root == null) {
         GameObject.CrashLog("[mod] " + _CONFIG_FILE + " absent, reglages de la ligue par defaut\n");
         return;
      }

      for (ParseKeyword key = root; key != null; key = key.GetNext()) {
         if (key.Equals("Qualifiers")) {
            m_nCfgQualifiers = key.GetIParam(0);
         } else if (key.Equals("GrandFinalReset")) {
            m_bCfgReset = key.GetIParam(0) != 0;
         } else if (key.Equals("Points")) {
            m_nCfgPointsWin = key.GetIParam(0);
            m_nCfgPointsDraw = key.GetIParam(1);
         } else if (key.Equals("GoalsPerMinute")) {
            m_fCfgGoalsPerMinute = key.GetFParam(0);
         } else if (key.Equals("HomeBonus")) {
            m_fCfgHomeBonus = key.GetFParam(0);
         } else if (key.Equals("StrengthEffect")) {
            m_fCfgStrengthEffect = key.GetFParam(0);
         } else if (key.Equals("Form")) {
            m_fCfgForm = key.GetFParam(0);
         } else if (key.Equals("RatingWeights")) {
            m_tCfgWeights = new float[]{key.GetFParam(0), key.GetFParam(1), key.GetFParam(2)};
         } else if (key.Equals("Rating")) {
            m_tCfgRatings.put(key.GetSParam(0).toLowerCase(), new Float(key.GetFParam(1)));
         }
      }

      GameObject.CrashLog("[mod] ligue : " + m_nCfgQualifiers + " qualifies, revanche en finale " + m_bCfgReset + "\n");
   }

   static int GetDebugAutoStart() {
      if (m_nDebugAutoStart < 0) {
         m_nDebugAutoStart = 0;
         ParseKeyword root = GameObject.ParseFile("debugconf.scr");

         for (ParseKeyword key = root; key != null; key = key.GetNext()) {
            if (key.Equals("LeagueTest")) {
               m_nDebugAutoStart = key.GetIParam(0);
            }
         }
      }

      return m_nDebugAutoStart;
   }

   // note d'une équipe pour les matchs simulés : moyenne pondérée Force / Précision / Réflexe (team_stats.scr), ou Rating(...)
   static float GetRating(int nAnimal) {
      LoadConfig();
      Float f = (Float)m_tCfgRatings.get(EAnimals.m_sNames[nAnimal].toLowerCase());
      if (f != null) {
         return f.floatValue();
      } else {
         float w = m_tCfgWeights[0] + m_tCfgWeights[1] + m_tCfgWeights[2];
         if (w <= 0.0F) {
            return 90.0F;
         } else {
            return (
                  ETeams.GetSkillStrength(nAnimal) * m_tCfgWeights[0]
                     + ETeams.GetSkillAccuracy(nAnimal) * m_tCfgWeights[1]
                     + ETeams.GetSkillTurning(nAnimal) * m_tCfgWeights[2]
               )
               / w;
         }
      }
   }

   // buts attendus par équipe dans un match simulé, selon la durée des mi-temps (Options > Match)
   static float GetGoalsPerMatch() {
      LoadConfig();
      PlayerSave player = CoreLogic.GetPlayer();
      int nHalf = player != null ? CoreLogic._HALF_TIMES[player.m_nMatchHalfTimeIndex] : 4;
      return m_fCfgGoalsPerMinute * 2.0F * nHalf;
   }

   static void ApplyRuntimeConfig(ModLeague l) {
      LoadConfig();
      l.m_fGoalsPerMatch = GetGoalsPerMatch();
      l.m_fHomeBonus = m_fCfgHomeBonus;
      l.m_fStrengthEffect = m_fCfgStrengthEffect;
   }

   // ================================================================ état

   static String GetProfileName() {
      PlayerSave player = CoreLogic.GetPlayer();
      return player != null && player.m_Name != null ? player.m_Name : "default";
   }

   // tournoi du profil courant (lu dans league.dat au premier accès), null s'il n'y en a pas
   static ModLeague GetLeague() {
      String sName = GetProfileName();
      if (!sName.equals(m_sOwner)) {
         m_sOwner = sName;
         m_cLeague = Load(sName);
         m_bHasLast = false;
         if (m_cLeague != null) {
            ApplyRuntimeConfig(m_cLeague);
         }
      }

      return m_cLeague;
   }

   static boolean IsInProgress() {
      return GetLeague() != null;
   }

   // un match de ligue (ou un écran de la ligue) est en cours
   static boolean IsActive() {
      return CoreLogic.m_bLeague && !CoreLogic.m_bDemo && !CoreLogic.m_bMultiplayer && GetLeague() != null;
   }

   // playoffs : égalité à la fin du temps -> but en or (Match.Update)
   static boolean IsGoldenGoal() {
      return IsActive() && m_cLeague.m_nPhase == ModLeague._PHASE_PLAYOFFS;
   }

   static int GetPlayerTeam() {
      ModLeague l = GetLeague();
      return l != null ? l.m_tTeam[l.m_nPlayer] : -1;
   }

   // ================================================================ déroulement

   // nouvelle ligue : les 14 équipes, chacune une fois
   static void Start(int nTeam, int nClothes) {
      LoadConfig();
      int n = EAnimals._ANIMALS_COUNT;
      int[] tAnimals = new int[n];
      float[] tRatings = new float[n];

      for (int i = 0; i < n; i++) {
         tAnimals[i] = i;
         tRatings[i] = GetRating(i);
      }

      ModLeague l = new ModLeague();
      l.m_nQualifiers = m_nCfgQualifiers;
      l.m_bReset = m_bCfgReset;
      l.m_nPointsWin = m_nCfgPointsWin;
      l.m_nPointsDraw = m_nCfgPointsDraw;
      ApplyRuntimeConfig(l);
      l.Start(tAnimals, tRatings, m_fCfgForm, nTeam);
      m_cLeague = l;
      m_sOwner = GetProfileName();
      m_nClothes = nClothes;
      m_bHasLast = false;
      Save();
      GameObject.CrashLog("[mod] ligue : " + n + " equipes, " + l.m_nRounds + " journees, " + l.m_nQualifiers + " qualifies\n");
   }

   // fin (ou abandon) de la ligue : elle disparaît de league.dat
   static void Stop() {
      ModLeague l = GetLeague();
      if (l != null) {
         m_cLeague = null;
         m_bHasLast = false;
         Hashtable t = ReadFile();
         t.remove(m_sOwner);
         WriteFile(t);
      }
   }

   // prochain match du joueur : lui à gauche, au stade de l'équipe qui reçoit ; false s'il n'en a plus
   static boolean PrepareMatch() {
      ModLeague l = GetLeague();
      int[] tNext = l != null ? l.GetPlayerNext() : null;
      if (tNext == null) {
         return false;
      } else {
         int nHost = l.m_tTeam[tNext[0]];
         int nGuest = l.m_tTeam[tNext[1]];
         int nMine = l.m_tTeam[l.m_nPlayer];
         TeamData cMine = ETeams.GetTeamData(nMine);
         cMine.SetClothesId(m_nClothes);
         CoreLogic.m_cTeamDataLeft = cMine;
         CoreLogic.m_cTeamDataRight = ETeams.GetTeamData(nHost == nMine ? nGuest : nHost);
         CoreLogic.m_nNextLevel = ETeams.GetHomeField(nHost);
         Match.m_cMatchData = null;
         return true;
      }
   }

   // fin d'un match de ligue (AIMatch.AfterMatch, après ModStats)
   static void OnMatchEnd(AIMatch cMatch) {
      if (!IsActive()) {
         return;
      }

      ModLeague l = m_cLeague;
      int[] tNext = l.GetPlayerNext();
      if (tNext == null) {
         return;
      }

      int nHostTeam = cMatch.m_nHalfNo != 0 && !cMatch.m_bGoldGoal ? 1 : 0;
      boolean bLeft = cMatch.GetTeam(nHostTeam).m_bUserControled;
      int nFor = bLeft ? cMatch.m_nScoreLeft : cMatch.m_nScoreRight;
      int nAgainst = bLeft ? cMatch.m_nScoreRight : cMatch.m_nScoreLeft;
      boolean bPlayerHost = tNext[0] == l.m_nPlayer;
      m_bLastLeague = l.m_nPhase == ModLeague._PHASE_LEAGUE;
      m_bLastGolden = cMatch.m_bGoldGoal;
      m_nLastResult = l.Report(bPlayerHost ? nFor : nAgainst, bPlayerHost ? nAgainst : nFor);
      m_bHasLast = true;
      Save();
      GameObject.CrashLog("[mod] ligue : " + nFor + "-" + nAgainst + " -> " + m_nLastResult + ", phase " + l.m_nPhase + ", journee " + l.m_nRound + ", jour " + l.m_nDay + "\n");
   }

   // le prochain match du joueur, s'il le gagne, lui donne le titre
   static boolean IsTitleMatch() {
      ModLeague l = GetLeague();
      return l != null && l.IsTitleMatch();
   }

   // ================================================================ league.dat : « profil;tenue;v0;v1;... »

   static Hashtable ReadFile() {
      Hashtable t = new Hashtable();
      File f = new File(_SAVE_FILE);
      if (!f.exists()) {
         return t;
      }

      try {
         BufferedReader in = new BufferedReader(new FileReader(f));

         for (String sLine = in.readLine(); sLine != null; sLine = in.readLine()) {
            int i = sLine.indexOf(59);
            if (i > 0) {
               t.put(sLine.substring(0, i), sLine.substring(i + 1));
            }
         }

         in.close();
      } catch (Exception e) {
         GameObject.CrashLog("[mod] ligue : lecture de " + _SAVE_FILE + " impossible : " + e + "\n");
      }

      return t;
   }

   static void WriteFile(Hashtable t) {
      try {
         FileWriter out = new FileWriter(_SAVE_FILE);
         Enumeration names = t.keys();

         while (names.hasMoreElements()) {
            String sName = (String)names.nextElement();
            out.write(sName + ";" + (String)t.get(sName) + "\r\n");
         }

         out.close();
      } catch (Exception e) {
         GameObject.CrashLog("[mod] ligue : ecriture de " + _SAVE_FILE + " impossible : " + e + "\n");
      }
   }

   static ModLeague Load(String sProfile) {
      String sData = (String)ReadFile().get(sProfile);
      if (sData == null) {
         return null;
      }

      try {
         StringTokenizer tok = new StringTokenizer(sData, ";");
         m_nClothes = Integer.parseInt(tok.nextToken().trim());
         int[] t = new int[tok.countTokens()];

         for (int i = 0; i < t.length; i++) {
            t[i] = Integer.parseInt(tok.nextToken().trim());
         }

         ModLeague l = new ModLeague();
         if (l.Load(t)) {
            return l;
         }
      } catch (Exception e) {
      }

      GameObject.CrashLog("[mod] ligue : donnees illisibles pour " + sProfile + " dans " + _SAVE_FILE + "\n");
      return null;
   }

   static void Save() {
      if (m_cLeague == null) {
         return;
      }

      Hashtable t = ReadFile();
      StringBuffer sb = new StringBuffer();
      sb.append(m_nClothes);
      int[] v = m_cLeague.Save();

      for (int i = 0; i < v.length; i++) {
         sb.append(';');
         sb.append(v[i]);
      }

      t.put(m_sOwner != null ? m_sOwner : GetProfileName(), sb.toString());
      WriteFile(t);
   }
}
