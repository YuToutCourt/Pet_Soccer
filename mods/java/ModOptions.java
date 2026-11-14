import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

// [mod] Menu « Mods » : déblocages, affichage, debug et triches de match, sauvegardés dans mods.dat (voir notes/mods-menu.md)
class ModOptions {
   static final String _SAVE_FILE = "mods.dat";
   // valeurs proposées par l'écran Mods (en %)
   static final int[] _SPEEDS = new int[]{100, 110, 125, 150, 200};
   static final int[] _SHOT_POWERS = new int[]{100, 125, 150, 200};
   // super imposé : -1 = aléatoire (jeu d'origine), sinon type de Team.m_nSuperType
   // (0 tir, 1 feinte, 2 vitesse, 3 mur, puis ModSupers : 4 gel, 5 ballon fou, 6 aimant, 7 tir double)
   static final int _SUPER_RANDOM = -1;
   static final int _SUPER_TYPES = ModSupers._TYPES;
   // supers infinis : délai avant la recharge, en secondes de match
   static final float _SUPER_REFILL_DELAY = 1.0F;
   // mode debug voulu au prochain lancement (SoccerGame.m_bDebug ne change qu'au démarrage)
   static boolean m_bDebugNextRun = false;
   static int m_nSpeed = 100;
   static int m_nShotPower = 100;
   static boolean m_bInfiniteSupers = false;
   static int m_nSuperType = _SUPER_RANDOM;
   private static float[] m_tEmptySince = new float[]{-1.0F, -1.0F};

   // appelé par SoccerGame.OnPreInitialize après debugconf.scr : une clé présente dans mods.dat l'emporte
   static void Load() {
      m_bDebugNextRun = SoccerGame.m_bDebug;
      File f = new File(_SAVE_FILE);
      if (!f.exists()) {
         return;
      }

      try {
         BufferedReader in = new BufferedReader(new FileReader(f));

         for (String sLine = in.readLine(); sLine != null; sLine = in.readLine()) {
            int nEq = sLine.indexOf('=');
            if (nEq > 0) {
               Set(sLine.substring(0, nEq).trim(), Integer.parseInt(sLine.substring(nEq + 1).trim()));
            }
         }

         in.close();
         GameObject.CrashLog("[mod] mods.dat charge : " + GetSummary() + "\n");
      } catch (Exception e) {
         GameObject.CrashLog("[mod] mods : lecture de " + _SAVE_FILE + " impossible : " + e + "\n");
      }
   }

   private static void Set(String sKey, int n) {
      if (sKey.equals("AllTeams")) {
         Cheat.m_bAllTeams = n != 0;
      } else if (sKey.equals("AllExtras")) {
         Cheat.m_bAllExtras = n != 0;
      } else if (sKey.equals("ShowFPS")) {
         SoccerGame.m_bShowFPS = n != 0;
      } else if (sKey.equals("Debug")) {
         SoccerGame.m_bDebug = n != 0;
         Game._bDbg = SoccerGame.m_bDebug;
         m_bDebugNextRun = SoccerGame.m_bDebug;
      } else if (sKey.equals("Speed")) {
         m_nSpeed = Find(_SPEEDS, n) >= 0 ? n : 100;
      } else if (sKey.equals("ShotPower")) {
         m_nShotPower = Find(_SHOT_POWERS, n) >= 0 ? n : 100;
      } else if (sKey.equals("InfiniteSupers")) {
         m_bInfiniteSupers = n != 0;
      } else if (sKey.equals("SuperType")) {
         m_nSuperType = n >= 0 && n < _SUPER_TYPES ? n : _SUPER_RANDOM;
      }
   }

   static void Save() {
      try {
         FileWriter out = new FileWriter(_SAVE_FILE);
         out.write("AllTeams=" + (Cheat.m_bAllTeams ? 1 : 0) + "\r\n");
         out.write("AllExtras=" + (Cheat.m_bAllExtras ? 1 : 0) + "\r\n");
         out.write("ShowFPS=" + (SoccerGame.m_bShowFPS ? 1 : 0) + "\r\n");
         out.write("Debug=" + (m_bDebugNextRun ? 1 : 0) + "\r\n");
         out.write("Speed=" + m_nSpeed + "\r\n");
         out.write("ShotPower=" + m_nShotPower + "\r\n");
         out.write("InfiniteSupers=" + (m_bInfiniteSupers ? 1 : 0) + "\r\n");
         out.write("SuperType=" + m_nSuperType + "\r\n");
         out.close();
         GameObject.CrashLog("[mod] mods.dat enregistre : " + GetSummary() + "\n");
      } catch (Exception e) {
         GameObject.CrashLog("[mod] mods : ecriture de " + _SAVE_FILE + " impossible : " + e + "\n");
      }
   }

   static String GetSummary() {
      return "equipes " + Cheat.m_bAllTeams + ", extras " + Cheat.m_bAllExtras + ", fps " + SoccerGame.m_bShowFPS + ", debug " + m_bDebugNextRun
         + ", vitesse " + m_nSpeed + "%, tir " + m_nShotPower + "%, supers infinis " + m_bInfiniteSupers + ", super " + m_nSuperType;
   }

   static int Find(int[] tValues, int n) {
      for (int i = 0; i < tValues.length; i++) {
         if (tValues[i] == n) {
            return i;
         }
      }

      return -1;
   }

   // une triche de match est active : le match ne compte pas pour l'Elo (ModElo.IsRatedMatch)
   static boolean IsMatchCheat() {
      return m_nSpeed != 100 || m_nShotPower != 100 || m_bInfiniteSupers || m_nSuperType != _SUPER_RANDOM;
   }

   // vitesse des équipes humaines (AIFootBaller.SetLocalSpeedStat) ; les triches sont coupées en ligne
   static float GetSpeedMul() {
      return ModNet.IsOnline() ? 1.0F : m_nSpeed / 100.0F;
   }

   // puissance de tir des équipes humaines (ModDifficulty.GetShotPowerMul)
   static float GetShotPowerMul() {
      return ModNet.IsOnline() ? 1.0F : m_nShotPower / 100.0F;
   }

   // appelé à chaque AITeam.Update : super imposé et supers infinis pour les équipes humaines
   static void UpdateTeam(Team t) {
      if (ModNet.IsOnline()) {
         return;
      }

      if (!t.m_bUserControled || CoreLogic.m_bDemo || SoccerModule.m_cMatch == null || !CoreLogic.GetPlayer().m_bSupersOn || t.m_bSuperDoing) {
         return;
      }

      if (m_nSuperType != _SUPER_RANDOM && t.m_nSuperType != m_nSuperType) {
         t.m_nSuperType = m_nSuperType;
         SoccerModule.GetHud().LightSuperLetters(t.m_nSuperFillCount, m_nSuperType, t.m_nSide);
      }

      // pas de super pendant le but en or, comme dans le jeu d'origine (Team.IncreaseSuperFillCount)
      if (!m_bInfiniteSupers || SoccerModule.m_cMatch.m_bGoldGoal || !AIMatch.IsMatchState(0)) {
         return;
      }

      int nSide = t.m_nSide & 1;
      float fNow = SoccerModule.m_Mod.m_Clock.GetMatchTimeF();
      if (t.m_bSuperAvailable) {
         m_tEmptySince[nSide] = -1.0F;
      } else if (m_tEmptySince[nSide] < 0.0F || fNow < m_tEmptySince[nSide]) {
         m_tEmptySince[nSide] = fNow;
      } else if (fNow - m_tEmptySince[nSide] >= _SUPER_REFILL_DELAY) {
         t.GiveSuper();
         m_tEmptySince[nSide] = -1.0F;
      }
   }
}
