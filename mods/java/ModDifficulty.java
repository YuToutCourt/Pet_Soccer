// [mod] Niveaux de difficulté 8..10 et bonus de l'IA, lus dans data\difficulty.scr (voir notes/difficulty.md)
class ModDifficulty {
   static final String _FILE = "data\\difficulty.scr";
   static final int _MAX_LEVEL = 10;
   // mot-clé = nom + "_level", ex. AttackInteria_level(8, 0.35)
   static final String[] _PARAMS = new String[]{
      "AttackInteria", "FreeBallInteria", "BallTakenInteria", "FeintChance", "FeintChance_AI", "SuperUpgradeTime",
      "PassChoiceCoef", "ValidPosDeviation", "ShotOpportunity", "MaxShotDeviation", "GKNoticeTime",
      "AIShotDeviation", "AISpeedMul", "AIShotPowerMul", "Marking", "MarkDistance"
   };
   static final int _AI_SHOT_DEVIATION = 11;
   static final int _AI_SPEED_MUL = 12;
   static final int _AI_SHOT_POWER_MUL = 13;
   static final int _MARKING = 14;
   static final int _MARK_DISTANCE = 15;
   private static float[][] m_fValues = new float[_PARAMS.length][_MAX_LEVEL + 1];
   private static boolean[][] m_bDefined = new boolean[_PARAMS.length][_MAX_LEVEL + 1];
   private static boolean m_bLoaded = false;
   private static int m_nLevel = 0;

   static void Load() {
      if (m_bLoaded) {
         return;
      }

      m_bLoaded = true;
      ParseKeyword root = GameObject.ParseFile(_FILE);
      if (root == null) {
         GameObject.CrashLog("[mod] " + _FILE + " absent, niveaux 8-10 = niveau 7\n");
         return;
      }

      for (ParseKeyword key = root; key != null; key = key.GetNext()) {
         for (int p = 0; p < _PARAMS.length; p++) {
            if (key.Equals(_PARAMS[p] + "_level")) {
               int nLevel = key.GetIParam(0);
               if (nLevel >= 1 && nLevel <= _MAX_LEVEL) {
                  m_fValues[p][nLevel] = key.GetFParam(1);
                  m_bDefined[p][nLevel] = true;
               } else {
                  GameObject.CrashLog("[mod] difficulty : niveau hors limites " + _PARAMS[p] + "(" + nLevel + ")\n");
               }
            }
         }
      }

      GameObject.CrashLog("[mod] difficulte chargee depuis " + _FILE + "\n");
   }

   // appelé par Strategy.FixDifficultyLevel après les valeurs d'ai_params.scr (niveau 7 pour 8..10)
   static void Apply(Strategy s, int nLevel) {
      Load();
      m_nLevel = nLevel;
      if (nLevel < 1 || nLevel > _MAX_LEVEL) {
         return;
      }

      for (int p = 0; p < _AI_SHOT_DEVIATION; p++) {
         if (m_bDefined[p][nLevel]) {
            float v = m_fValues[p][nLevel];
            switch (p) {
               case 0:
                  Strategy.m_fAttackInteria = v;
                  break;
               case 1:
                  Strategy.m_fFreeBallInteria = v;
                  break;
               case 2:
                  s.m_fBallTakenInteria = v;
                  break;
               case 3:
                  s.m_fFeintChance = v;
                  break;
               case 4:
                  s.m_fFeintChance_AI = v;
                  break;
               case 5:
                  s.m_fSuperUpgradeTime = v;
                  break;
               case 6:
                  s.m_fPassChoiceCoef = v;
                  break;
               case 7:
                  s.m_fValidPosDeviation = v;
                  break;
               case 8:
                  s.m_fShotOpportunity = v;
                  break;
               case 9:
                  s.m_fMaxShotDeviation = v;
                  break;
               case 10:
                  s.m_fGKNoticeTime = v;
            }
         }
      }

      GameObject.CrashLog("[mod] difficulte niveau " + nLevel + "\n");
   }

   private static float GetAI(int p) {
      return m_nLevel >= 1 && m_nLevel <= _MAX_LEVEL && m_bDefined[p][m_nLevel] ? m_fValues[p][m_nLevel] : 1.0F;
   }

   // dispersion gauche-droite des tirs de l'IA (1.0 = jeu d'origine)
   static float GetAIShotDeviation() {
      return GetAI(_AI_SHOT_DEVIATION);
   }

   // vitesse de déplacement des équipes IA
   static float GetAISpeedMul() {
      return GetAI(_AI_SPEED_MUL) * ModSurvival.GetAISpeedMul(); // [mod] survie : bonus au-delà du niveau max
   }

   // stat Force du tireur × bonus de puissance si c'est l'IA qui tire, × triche « puissance de tir » (menu Mods) si c'est un humain
   static float GetShotPowerMul(AIFootBaller cShooter) {
      if (cShooter == null) {
         return 1.0F;
      }

      float fMul = ETeams.GetShotPowerMul(cShooter.m_nAnimalType);
      if (!cShooter.IsUserControled()) {
         fMul *= GetAI(_AI_SHOT_POWER_MUL) * ModSurvival.GetAIShotPowerMul(); // [mod] survie
      } else {
         fMul *= ModOptions.GetShotPowerMul();
      }

      return fMul;
   }

   private static float GetLevel(int p, float fDefault) {
      return m_nLevel >= 1 && m_nLevel <= _MAX_LEVEL && m_bDefined[p][m_nLevel] ? m_fValues[p][m_nLevel] : fDefault;
   }

   // force du marquage des équipes IA (0 = jeu d'origine, 1 = marquage serré), voir ModTactics
   static float GetMarking() {
      return GetLevel(_MARKING, 0.0F);
   }

   // distance entre le marqueur IA et son adversaire, côté but
   static float GetMarkDistance() {
      return GetLevel(_MARK_DISTANCE, 150.0F);
   }
}
