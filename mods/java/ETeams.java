public class ETeams {
   private static TeamData _TEAM_BEAR = null;
   private static TeamData _TEAM_BULL = null;
   private static TeamData _TEAM_CHICKEN = null;
   private static TeamData _TEAM_DINO = null;
   private static TeamData _TEAM_FROG = null;
   private static TeamData _TEAM_GIRAFFE = null;
   private static TeamData _TEAM_GORILLA = null;
   private static TeamData _TEAM_LION = null;
   private static TeamData _TEAM_PANTHER = null;
   private static TeamData _TEAM_SHARK = null;
   private static TeamData _TEAM_TURTLE = null;
   private static TeamData _TEAM_ROBO = null;
   // [mod] nouvelles equipes (voir notes/new-teams.md)
   private static TeamData _TEAM_PANDA = null;
   private static TeamData _TEAM_TIGER = null;
   // [mod] + Panda, Tigre (valeurs par defaut si team_stats.scr est absent)
   private static int[] m_nSkillsStrength = new int[]{90, 100, 80, 95, 85, 95, 90, 90, 90, 95, 90, 80, 100, 90};
   private static int[] m_nSkillsAccuracy = new int[]{90, 90, 90, 85, 85, 95, 90, 100, 100, 90, 100, 95, 85, 90};
   private static int[] m_nSkillsTurning = new int[]{90, 80, 100, 90, 100, 80, 90, 80, 80, 85, 80, 95, 85, 100};
   private static TeamData[] tTeams = null;
   // [mod] Stats lues dans data\team_stats.scr et appliquées en match (voir notes/team-stats.md)
   static final String _STATS_FILE = "data\\team_stats.scr";
   private static boolean m_bStatsLoaded = false;
   private static float m_fShotPowerPerPoint = 0.01F;
   private static float m_fShotDeviationPerPoint = 0.03F;
   private static float m_fSpeedPerPoint = 0.005F;

   static void LoadStats() {
      if (m_bStatsLoaded) {
         return;
      }

      m_bStatsLoaded = true;
      ParseKeyword root = GameObject.ParseFile(_STATS_FILE);
      if (root == null) {
         GameObject.CrashLog("[mod] " + _STATS_FILE + " absent, stats d'origine\n");
         return;
      }

      for (ParseKeyword key = root; key != null; key = key.GetNext()) {
         if (key.Equals("Team")) {
            String sName = key.GetSParam(0);
            int nAnimal = -1;

            for (int i = 0; i < m_nSkillsStrength.length; i++) {
               if (EAnimals.m_sNames[i].equalsIgnoreCase(sName)) {
                  nAnimal = i;
               }
            }

            if (nAnimal == -1) {
               GameObject.CrashLog("[mod] team_stats : equipe inconnue " + sName + "\n");
            } else {
               m_nSkillsStrength[nAnimal] = key.GetIParam(1);
               m_nSkillsAccuracy[nAnimal] = key.GetIParam(2);
               m_nSkillsTurning[nAnimal] = key.GetIParam(3);
            }
         }

         if (key.Equals("Effects")) {
            m_fShotPowerPerPoint = key.GetFParam(0) / 100.0F;
            m_fShotDeviationPerPoint = key.GetFParam(1) / 100.0F;
            m_fSpeedPerPoint = key.GetFParam(2) / 100.0F;
         }
      }

      GameObject.CrashLog("[mod] stats chargees depuis " + _STATS_FILE + "\n");
   }

   private static boolean IsTeamAnimal(int nAnimal) {
      return nAnimal >= 0 && nAnimal < m_nSkillsStrength.length;
   }

   // Force : multiplicateur de puissance des tirs (90 = jeu d'origine)
   static float GetShotPowerMul(int nAnimal) {
      LoadStats();
      return !IsTeamAnimal(nAnimal) ? 1.0F : Math.max(0.1F, 1.0F + (m_nSkillsStrength[nAnimal] - 90) * m_fShotPowerPerPoint);
   }

   // Précision : multiplicateur de dispersion des tirs (plus petit = plus précis)
   static float GetShotDeviationMul(int nAnimal) {
      LoadStats();
      return !IsTeamAnimal(nAnimal) ? 1.0F : Math.max(0.1F, 1.0F + (90 - m_nSkillsAccuracy[nAnimal]) * m_fShotDeviationPerPoint);
   }

   // Réflexe : multiplicateur de vitesse de déplacement
   static float GetSpeedMul(int nAnimal) {
      LoadStats();
      return !IsTeamAnimal(nAnimal) ? 1.0F : Math.max(0.1F, 1.0F + (m_nSkillsTurning[nAnimal] - 90) * m_fSpeedPerPoint);
   }

   static int GetHomeField(int nAnimal) {
      switch (nAnimal) {
         case 0:
            return 0;
         case 1:
            return 1;
         case 2:
            return 2;
         case 3:
            return 3;
         case 4:
            return 4;
         case 5:
            return 5;
         case 6:
            return 6;
         case 7:
            return 7;
         case 8:
            return 8;
         case 9:
            return 9;
         case 10:
            return 10;
         case 11:
            return 11;
         // [mod] pas de stade propre : Panda chez les gorilles (jungle), Tigre chez les pantheres
         case 12:
            return 6;
         case 13:
            return 8;
         default:
            return -1;
      }
   }

   static void InitTeamData() {
      _TEAM_BEAR = new TeamData(
         Text.Get("&TeamBears&"),
         Text.Get("&TeamBearsAbr&"),
         Text.Get("&TeamBearsMin&"),
         Text.Get("&TeamBearsInfo&"),
         Text.Get("&TeamBearsPlayers&"),
         ETeamLogos._LOGO_BEAR,
         0
      );
      _TEAM_BULL = new TeamData(
         Text.Get("&TeamBulls&"),
         Text.Get("&TeamBullsAbr&"),
         Text.Get("&TeamBullsMin&"),
         Text.Get("&TeamBullsInfo&"),
         Text.Get("&TeamBullsPlayers&"),
         ETeamLogos._LOGO_BULL,
         1
      );
      _TEAM_CHICKEN = new TeamData(
         Text.Get("&TeamChickens&"),
         Text.Get("&TeamChickensAbr&"),
         Text.Get("&TeamChickensMin&"),
         Text.Get("&TeamChickensInfo&"),
         Text.Get("&TeamChickensPlayers&"),
         ETeamLogos._LOGO_CHICKEN,
         2
      );
      _TEAM_DINO = new TeamData(
         Text.Get("&TeamDinos&"),
         Text.Get("&TeamDinosAbr&"),
         Text.Get("&TeamDinosMin&"),
         Text.Get("&TeamDinosInfo&"),
         Text.Get("&TeamDinosPlayers&"),
         ETeamLogos._LOGO_DINO,
         3
      );
      _TEAM_FROG = new TeamData(
         Text.Get("&TeamFrogs&"),
         Text.Get("&TeamFrogsAbr&"),
         Text.Get("&TeamFrogsMin&"),
         Text.Get("&TeamFrogsInfo&"),
         Text.Get("&TeamFrogsPlayers&"),
         ETeamLogos._LOGO_FROG,
         4
      );
      _TEAM_GIRAFFE = new TeamData(
         Text.Get("&TeamGiraffes&"),
         Text.Get("&TeamGiraffesAbr&"),
         Text.Get("&TeamGiraffesMin&"),
         Text.Get("&TeamGiraffesInfo&"),
         Text.Get("&TeamGiraffesPlayers&"),
         ETeamLogos._LOGO_GIRAFFE,
         5
      );
      _TEAM_GORILLA = new TeamData(
         Text.Get("&TeamGorillas&"),
         Text.Get("&TeamGorillasAbr&"),
         Text.Get("&TeamGorillasMin&"),
         Text.Get("&TeamGorillasInfo&"),
         Text.Get("&TeamGorillasPlayers&"),
         ETeamLogos._LOGO_GORILLA,
         6
      );
      _TEAM_LION = new TeamData(
         Text.Get("&TeamLions&"),
         Text.Get("&TeamLionsAbr&"),
         Text.Get("&TeamLionsMin&"),
         Text.Get("&TeamLionsInfo&"),
         Text.Get("&TeamLionsPlayers&"),
         ETeamLogos._LOGO_LION,
         7
      );
      _TEAM_PANTHER = new TeamData(
         Text.Get("&TeamPanthers&"),
         Text.Get("&TeamPanthersAbr&"),
         Text.Get("&TeamPanthersMin&"),
         Text.Get("&TeamPanthersInfo&"),
         Text.Get("&TeamPanthersPlayers&"),
         ETeamLogos._LOGO_PANTHER,
         8
      );
      _TEAM_SHARK = new TeamData(
         Text.Get("&TeamSharks&"),
         Text.Get("&TeamSharksAbr&"),
         Text.Get("&TeamSharksMin&"),
         Text.Get("&TeamSharksInfo&"),
         Text.Get("&TeamSharksPlayers&"),
         ETeamLogos._LOGO_SHARK,
         9
      );
      _TEAM_TURTLE = new TeamData(
         Text.Get("&TeamTurtles&"),
         Text.Get("&TeamTurtlesAbr&"),
         Text.Get("&TeamTurtlesMin&"),
         Text.Get("&TeamTurtlesInfo&"),
         Text.Get("&TeamTurtlesPlayers&"),
         ETeamLogos._LOGO_TURTLE,
         10
      );
      _TEAM_ROBO = new TeamData(
         Text.Get("&TeamRobos&"),
         Text.Get("&TeamRobosAbr&"),
         Text.Get("&TeamRobosMin&"),
         Text.Get("&TeamRobosInfo&"),
         Text.Get("&TeamRobosPlayers&"),
         ETeamLogos._LOGO_ROBO,
         11
      );
      _TEAM_PANDA = new TeamData(
         Text.Get("&TeamPandas&"),
         Text.Get("&TeamPandasAbr&"),
         Text.Get("&TeamPandasMin&"),
         Text.Get("&TeamPandasInfo&"),
         Text.Get("&TeamPandasPlayers&"),
         ETeamLogos._LOGO_PANDA,
         12
      );
      _TEAM_TIGER = new TeamData(
         Text.Get("&TeamTigers&"),
         Text.Get("&TeamTigersAbr&"),
         Text.Get("&TeamTigersMin&"),
         Text.Get("&TeamTigersInfo&"),
         Text.Get("&TeamTigersPlayers&"),
         ETeamLogos._LOGO_TIGER,
         13
      );
      tTeams = new TeamData[]{
         _TEAM_BEAR,
         _TEAM_BULL,
         _TEAM_CHICKEN,
         _TEAM_DINO,
         _TEAM_FROG,
         _TEAM_GIRAFFE,
         _TEAM_GORILLA,
         _TEAM_LION,
         _TEAM_PANTHER,
         _TEAM_SHARK,
         _TEAM_TURTLE,
         _TEAM_ROBO,
         _TEAM_PANDA,
         _TEAM_TIGER
      };
   }

   static TeamData GetTeamData(int nAnimal) {
      if (tTeams == null) {
         InitTeamData();
      }

      if (nAnimal >= tTeams.length || nAnimal < 0) {
         Tools.CrashLogWarning("ETeams proba pobrania zespolu spoza zakresu " + nAnimal);
         nAnimal %= tTeams.length;
      }

      return tTeams[nAnimal];
   }

   static TeamData[] GetTeamDataTab() {
      if (tTeams == null) {
         InitTeamData();
      }

      return tTeams;
   }

   static int GetSkillStrength(int nAnimal) {
      LoadStats();
      return m_nSkillsStrength[nAnimal];
   }

   static int GetSkillTurning(int nAnimal) {
      LoadStats();
      return m_nSkillsTurning[nAnimal];
   }

   static int GetSkillAccuracy(int nAnimal) {
      LoadStats();
      return m_nSkillsAccuracy[nAnimal];
   }
}
