import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

// [mod] Statistiques de match (passes, ballons récupérés, arrêts, buteurs) et historique des matchs
// par profil, sauvegardé dans stats.dat (voir notes/stats.md)
class ModStats {
   static final String _SAVE_FILE = "stats.dat";
   static final int _MAX_HISTORY = 2000; // par profil
   static final int _MAX_SCORER_LINES = 3; // lignes ID_SCORER<n>_* (mods/ui/make_stats_scr.py)
   static final int _STAT_ROWS = 6; // lignes ID_STAT<n>_* : GetValues
   static final int _SUMMARY_ROWS = 14; // lignes ID_S<n>_* de l'écran Profil : GetSummary
   // un but juste après un arrêt (ballon repoussé dans le but) annule l'arrêt
   static final float _SAVE_CANCEL_TIME = 2.0F;
   // modes de jeu enregistrés (textes StatsMode0..)
   static final int _MODE_FRIENDLY = 0;
   static final int _MODE_CUP = 1;
   static final int _MODE_EXTRA = 2;
   static final int _MODE_SURVIVAL = 3; // [mod] survie (ModSurvival)
   static final int _MODE_LEAGUE = 4; // [mod] ligue (ModLeagueMode)
   // champs d'un match de l'historique (Record.m_tValues)
   static final int _MODE = 0;
   static final int _MY_TEAM = 1;
   static final int _OPP_TEAM = 2;
   static final int _GOALS_FOR = 3;
   static final int _GOALS_AGAINST = 4;
   static final int _LEVEL = 5;
   static final int _RATED = 6;
   static final int _ELO_DELTA = 7;
   static final int _ABANDON = 8;
   static final int _SHOTS = 9;
   static final int _POSSESSION = 10;
   static final int _PASSES_OK = 11;
   static final int _PASSES = 12;
   static final int _RECOVERIES = 13;
   static final int _SAVES = 14;
   static final int _SUPERS = 15;
   static final int _TOP_SCORER_GOALS = 16; // buts du meilleur buteur du joueur (triplés)
   static final int _CHEAT = 17; // triche de match active (menu Mods) : match non classé
   // champs lus par les succès (ModAchievements)
   static final int _MAX_DEFICIT = 18; // plus grand retard au score pendant le match
   static final int _GOLDEN_GOAL = 19; // le match est allé au but en or
   static final int _WINNER_MINUTE = 20; // minute du but de la victoire (0 sans victoire)
   static final int _SUPER_TYPES = 21; // types de supers utilisés (bit n = super n)
   static final int _CUP_WON = 22; // finale de coupe gagnée : numéro de la coupe + 1, 0 sinon
   static final int _SURVIVAL_WAVE = 23; // survie : numéro de la manche jouée (1 = première), 0 hors survie
   static final int _FIELDS = 24;
   // [mod] en ligne : statistiques détaillées que le jeu de l'hôte envoie au serveur (PutMatchStats, PKT_MATCH_STATS)
   static final int _HEAT_D = 12; // cases de la heatmap en profondeur (0 = son but)
   static final int _HEAT_L = 8; // ... et en latéral (0 = haut de l'écran), repère de l'écran Tactique
   static final float _SAMPLE_STEP = 0.25F; // secondes de match entre deux relevés des positions
   static final float _MAX_STEP = 0.06F; // déplacement entre deux relevés au-delà duquel c'est un replacement (pas compté)
   static final int _MAX_GOALS = 30;
   static final int _MAX_SHOTS = 30; // par équipe, dans le paquet
   static final int _MAX_SUPERS = 20;
   static final int _SHOT_MISSED = 0; // issues d'un tir (protocol.SHOT_*)
   static final int _SHOT_GOAL = 1;
   static final int _SHOT_SAVED = 2;
   static final int _SHOT_BLOCKED = 3;

   // compteurs d'une équipe pendant le match. Clé : l'objet Team, car les index d'équipe s'inversent à la mi-temps.
   static class TeamStats {
      int m_nPasses = 0;
      int m_nPassesOk = 0;
      int m_nRecoveries = 0;
      int m_nSaves = 0;
      int m_nMaxDeficit = 0;
      int m_nSuperTypes = 0;
      java.util.Vector m_vGoals = new java.util.Vector(); // dans l'ordre du match
      // [mod] en ligne (PKT_MATCH_STATS) : heatmaps (profondeur x latéral), tirs et supers dans l'ordre du match
      int[] m_tHeat = new int[_HEAT_D * _HEAT_L];
      int[] m_tBallHeat = new int[_HEAT_D * _HEAT_L]; // le ballon quand l'équipe l'a
      java.util.Vector m_vShots = new java.util.Vector();
      java.util.Vector m_vSupers = new java.util.Vector(); // int[]{minute, type}
   }

   // [mod] en ligne : ce que fait chaque joueur (PKT_MATCH_STATS)
   static class PlayerStats {
      int m_nGoals = 0;
      int m_nShots = 0;
      int m_nPasses = 0;
      int m_nPassesOk = 0;
      int m_nRecoveries = 0;
      float m_fDistance = 0.0F; // en fractions de la longueur du terrain
      float m_fSumDepth = 0.0F;
      float m_fSumLateral = 0.0F;
      int m_nSamples = 0;
      boolean m_bHasLast = false;
      float m_fLastX = 0.0F;
      float m_fLastZ = 0.0F;
   }

   static class Shot {
      Team m_cTeam;
      int m_nMinute;
      int m_nNumber; // GetListIndex du tireur
      float m_fDepth; // position du tireur, repère de son équipe
      float m_fLateral;
      int m_nResult = _SHOT_MISSED;
   }

   static class Goal {
      int m_nMinute;
      int m_nAnimal = -1; // équipe du buteur (pour son nom)
      int m_nNumber = -1; // place dans la liste des joueurs (GetListIndex)
      boolean m_bOwnGoal = false;
   }

   static class Record {
      String m_sDate;
      int[] m_tValues = new int[_FIELDS];
   }

   private static Hashtable m_tTeams = new Hashtable(); // Team -> TeamStats
   private static AIFootBaller m_cLastKicker = null;
   private static AIFootBaller m_cPasser = null; // passe en l'air
   private static Team m_cShotTeam = null; // tir en cours, ni arrêté ni contré
   private static Team m_cLastPossesor = null;
   private static TeamStats m_cLastSave = null;
   private static float m_fLastSaveTime = -100.0F;
   private static boolean m_bLoaded = false;
   private static Hashtable m_tHistory = new Hashtable(); // profil -> java.util.Vector de Record (du plus ancien au plus récent)
   private static Hashtable m_tPlayerStats = new Hashtable(); // AIFootBaller -> PlayerStats
   private static Shot m_cOpenShot = null; // dernier tir, issue pas encore connue
   private static Shot m_cSavedShot = null; // tir du dernier arrêt : redevient un but si l'arrêt est annulé
   private static float m_fLastSample = -1.0F;
   private static boolean m_bSampleGap = true; // jeu arrêté depuis le dernier relevé : la distance repart de là

   // ---------------------------------------------------------------- pendant le match

   // appelé par AIMatch.ResetStatistics (début ou reprise d'un match)
   static void Reset() {
      m_tTeams.clear();
      m_tPlayerStats.clear();
      m_cOpenShot = null;
      m_cSavedShot = null;
      m_fLastSample = -1.0F;
      m_bSampleGap = true;
      m_cLastKicker = null;
      m_cPasser = null;
      m_cShotTeam = null;
      m_cLastPossesor = null;
      m_cLastSave = null;
      m_fLastSaveTime = -100.0F;
      ModAchievements.Reset();
   }

   static TeamStats Get(Team cTeam) {
      TeamStats s = (TeamStats)m_tTeams.get(cTeam);
      if (s == null) {
         s = new TeamStats();
         m_tTeams.put(cTeam, s);
      }

      return s;
   }

   private static boolean IsPlaying() {
      return AIMatch.IsMatchState(0) && !GameField.m_cBall.IsReplaying();
   }

   private static float GetTime() {
      return SoccerModule.m_cMatch != null ? SoccerModule.m_cMatch.m_cClockRef.GetMatchTimeF() : 0.0F;
   }

   static PlayerStats GetPlayer(AIFootBaller cFB) {
      PlayerStats p = (PlayerStats)m_tPlayerStats.get(cFB);
      if (p == null) {
         p = new PlayerStats();
         m_tPlayerStats.put(cFB, p);
      }

      return p;
   }

   // AIFootBaller.Pass
   static void OnPass(AIFootBaller cFB) {
      if (IsPlaying()) {
         m_cPasser = cFB;
         Get(cFB.m_cTeam).m_nPasses++;
         GetPlayer(cFB).m_nPasses++;
      }
   }

   // AIFootBaller.DefenceRequest / DoSuperShot (là où le jeu compte les tirs)
   static void OnShot(AIFootBaller cFB) {
      m_cShotTeam = cFB.m_cTeam;
      m_cPasser = null;
      if (IsPlaying()) {
         // [mod] en ligne : tireur et position ; l'issue (but, arrêt, contré) vient ensuite, sinon le tir est à côté
         GetPlayer(cFB).m_nShots++;
         Shot s = new Shot();
         s.m_cTeam = cFB.m_cTeam;
         s.m_nMinute = SoccerModule.m_cMatch != null ? GetMinute(SoccerModule.m_cMatch, GetTime()) : 0;
         s.m_nNumber = GetListIndex(cFB);
         Vector v = cFB.GetPosition();
         s.m_fDepth = ModTactics.Clamp(ModTactics.Depth(cFB.m_nTeam, v), 0.0F, 1.0F);
         s.m_fLateral = ModTactics.LateralOf(v);
         TeamStats t = Get(cFB.m_cTeam);
         if (t.m_vShots.size() < _MAX_SHOTS) {
            t.m_vShots.addElement(s);
         }

         m_cOpenShot = s;
      }
   }

   // Ball.KickTo / KickToLob : dernier joueur à avoir frappé le ballon (le buteur en cas de but)
   static void OnKick(AIFootBaller cFB) {
      if (cFB != null && !GameField.m_cBall.IsReplaying()) {
         m_cLastKicker = cFB;
      }
   }

   private static void AddSave(Team cTeam) {
      m_cLastSave = Get(cTeam);
      m_cLastSave.m_nSaves++;
      m_fLastSaveTime = GetTime();
      m_cShotTeam = null;
      m_cSavedShot = null;
      if (m_cOpenShot != null && m_cOpenShot.m_cTeam != cTeam) {
         m_cOpenShot.m_nResult = _SHOT_SAVED;
         m_cSavedShot = m_cOpenShot;
      }

      m_cOpenShot = null;
   }

   // Ball.OnHitFootballer : le ballon touche un joueur sans qu'il le contrôle
   static void OnTouch(AIFootBaller cFB) {
      if (m_cShotTeam != null && cFB.m_cTeam != m_cShotTeam && IsPlaying()) {
         if (cFB.IsGoalKeeper()) {
            AddSave(cFB.m_cTeam);
         } else {
            m_cShotTeam = null; // tir contré
            if (m_cOpenShot != null && m_cOpenShot.m_cTeam != cFB.m_cTeam) {
               m_cOpenShot.m_nResult = _SHOT_BLOCKED;
            }

            m_cOpenShot = null;
         }
      }
   }

   // AIMatch.ChangePossesor : un joueur prend le ballon
   static void OnPossession(AIFootBaller cFB) {
      Team cTeam = cFB.m_cTeam;
      boolean bPlaying = IsPlaying();
      if (m_cPasser != null) {
         if (bPlaying && cTeam == m_cPasser.m_cTeam && cFB != m_cPasser) {
            Get(cTeam).m_nPassesOk++;
            GetPlayer(m_cPasser).m_nPassesOk++;
         }

         m_cPasser = null;
      }

      boolean bSave = false;
      if (m_cShotTeam != null && cTeam != m_cShotTeam) {
         if (bPlaying && cFB.IsGoalKeeper()) {
            AddSave(cTeam);
            bSave = true;
         }

         m_cShotTeam = null;
      }

      if (!bSave && bPlaying && m_cLastPossesor != null && cTeam != m_cLastPossesor) {
         Get(cTeam).m_nRecoveries++;
         GetPlayer(cFB).m_nRecoveries++;
      }

      m_cLastPossesor = cTeam;
   }

   // AIMatch.OnGoal (nTeam = index de l'équipe qui marque)
   static void OnGoal(AIMatch cMatch, int nTeam) {
      Team cScoring = cMatch.GetTeam(nTeam);
      float fTime = GetTime();
      if (m_cLastSave != null && m_cLastSave != Get(cScoring) && fTime - m_fLastSaveTime < _SAVE_CANCEL_TIME) {
         m_cLastSave.m_nSaves--;
         if (m_cSavedShot != null && m_cSavedShot.m_cTeam == cScoring) {
            m_cSavedShot.m_nResult = _SHOT_GOAL; // ballon repoussé dans le but : l'arrêt ne compte pas
         }
      }

      if (m_cOpenShot != null && m_cOpenShot.m_cTeam == cScoring) {
         m_cOpenShot.m_nResult = _SHOT_GOAL;
      }

      m_cOpenShot = null;
      m_cSavedShot = null;
      Goal g = new Goal();
      g.m_nMinute = GetMinute(cMatch, fTime);
      if (m_cLastKicker != null) {
         g.m_nAnimal = m_cLastKicker.m_nAnimalType;
         g.m_nNumber = GetListIndex(m_cLastKicker);
         g.m_bOwnGoal = m_cLastKicker.m_cTeam != cScoring;
         if (!g.m_bOwnGoal) {
            GetPlayer(m_cLastKicker).m_nGoals++;
         }
      } else {
         g.m_nAnimal = cScoring.m_nAnimalType;
      }

      Get(cScoring).m_vGoals.addElement(g);
      TeamStats o = Get(cMatch.GetTeam(1 - nTeam));
      o.m_nMaxDeficit = Math.max(o.m_nMaxDeficit, Get(cScoring).m_vGoals.size() - o.m_vGoals.size());
      GameObject.CrashLog("[mod] stats : but de " + cScoring.m_sName + " (" + GetPlayerName(g.m_nAnimal, g.m_nNumber)
         + (m_cLastKicker != null ? ", n" + m_cLastKicker.m_nNumberInTeam : "") + (g.m_bOwnGoal ? ", csc" : "") + ") a la " + g.m_nMinute + "e\n");
      m_cLastSave = null;
      m_cShotTeam = null;
      m_cPasser = null;
      m_cLastPossesor = null;
   }

   // AIFootBaller.UseSuper : un super a été déclenché
   static void OnSuper(Team cTeam, int nType) {
      if (nType >= 0 && nType < 31) {
         Get(cTeam).m_nSuperTypes |= 1 << nType;
      }

      TeamStats t = Get(cTeam);
      if (nType >= 0 && t.m_vSupers.size() < _MAX_SUPERS && SoccerModule.m_cMatch != null) {
         t.m_vSupers.addElement(new int[]{GetMinute(SoccerModule.m_cMatch, GetTime()), nType});
      }
   }

   // [mod] en ligne : AIMatch.UpdateGame, à chaque frame de jeu. Toutes les _SAMPLE_STEP secondes de match, la case
   // de chaque joueur dans la heatmap de son équipe, sa position moyenne, sa distance parcourue, et la case du ballon
   // pour l'équipe qui l'a. Repère de l'écran Tactique : profondeur 0 = son but, latéral 0 = haut de l'écran.
   static void OnFrame(AIMatch cMatch) {
      if (!IsPlaying() || GameField.m_cBall == null) {
         m_bSampleGap = true;
         return;
      }

      float fTime = GetTime();
      if (m_fLastSample >= 0.0F && fTime >= m_fLastSample && fTime - m_fLastSample < _SAMPLE_STEP) {
         return;
      }

      m_fLastSample = fTime;
      boolean bGap = m_bSampleGap;
      m_bSampleGap = false;
      float fLength = GameField.fLineEnd > 0.0F ? GameField.fLineEnd : 1.0F;

      for (int i = 0; i < 2; i++) {
         Team cTeam = cMatch.GetTeam(i);
         if (cTeam == null) {
            continue;
         }

         TeamStats t = Get(cTeam);
         int nSide = i; // côté de l'équipe pour la profondeur : celui de ses joueurs

         for (int j = 0; j < cTeam.m_vFootBallers.size(); j++) {
            AIFootBaller cFB = (AIFootBaller)cTeam.m_vFootBallers.elementAt(j);
            if (cFB.IsReferee()) {
               continue;
            }

            nSide = cFB.m_nTeam;
            Vector v = cFB.GetPosition();
            float fDepth = ModTactics.Clamp(ModTactics.Depth(cFB.m_nTeam, v), 0.0F, 1.0F);
            float fLateral = ModTactics.LateralOf(v);
            t.m_tHeat[HeatCell(fDepth, fLateral)]++;
            PlayerStats p = GetPlayer(cFB);
            p.m_fSumDepth += fDepth;
            p.m_fSumLateral += fLateral;
            p.m_nSamples++;
            if (p.m_bHasLast && !bGap) {
               float dx = (v.fX - p.m_fLastX) / fLength;
               float dz = (v.fZ - p.m_fLastZ) / fLength;
               float fStep = (float)Math.sqrt(dx * dx + dz * dz);
               if (fStep < _MAX_STEP) {
                  p.m_fDistance += fStep;
               }
            }

            p.m_bHasLast = true;
            p.m_fLastX = v.fX;
            p.m_fLastZ = v.fZ;
         }

         if (cTeam == m_cLastPossesor) {
            Vector b = GameField.m_cBall.GetPosition();
            t.m_tBallHeat[HeatCell(ModTactics.Clamp(ModTactics.Depth(nSide, b), 0.0F, 1.0F), ModTactics.LateralOf(b))]++;
         }
      }
   }

   private static int HeatCell(float fDepth, float fLateral) {
      int d = Math.min(_HEAT_D - 1, Math.max(0, (int)(fDepth * _HEAT_D)));
      int l = Math.min(_HEAT_L - 1, Math.max(0, (int)(fLateral * _HEAT_L)));
      return d * _HEAT_L + l;
   }

   private static int Clip(int n, int nMax) {
      return Math.max(0, Math.min(nMax, n));
   }

   // [mod] en ligne : les statistiques du match pour le serveur (PKT_MATCH_STATS, format dans server/protocol.py).
   // Équipe de l'hôte (à gauche au coup d'envoi) puis celle de l'invité ; même règle d'index que AddRecord.
   static void PutMatchStats(java.nio.ByteBuffer b, AIMatch cMatch, int nMatchId) {
      int nHost = cMatch.m_nHalfNo != 0 && !cMatch.m_bGoldGoal ? 1 : 0;
      b.putInt(nMatchId);
      b.put((byte)(cMatch.m_bGoldGoal ? 1 : 0));
      b.put((byte)_HEAT_D);
      b.put((byte)_HEAT_L);
      float fLength = GameField.fLineEnd > 0.0F ? GameField.fLineEnd : 1.0F;

      for (int k = 0; k < 2; k++) {
         int nIndex = k == 0 ? nHost : 1 - nHost;
         Team cTeam = cMatch.GetTeam(nIndex);
         TeamStats t = Get(cTeam);
         b.putShort((short)Clip(cMatch.GetShootCount(nIndex), 32767));
         b.put((byte)Clip((int)cMatch.GetEntirePossesion(nIndex), 100));
         b.putShort((short)Clip(t.m_nPassesOk, 32767));
         b.putShort((short)Clip(t.m_nPasses, 32767));
         b.putShort((short)Clip(t.m_nRecoveries, 32767));
         b.putShort((short)Clip(t.m_nSaves, 32767));
         b.putShort((short)Clip(cMatch.GetSuperCount(nIndex), 32767));
         b.putInt(t.m_nSuperTypes);
         b.put((byte)Clip(t.m_nMaxDeficit, 255));
         int n = Math.min(t.m_vGoals.size(), _MAX_GOALS);
         b.put((byte)n);

         for (int i = 0; i < n; i++) {
            Goal g = (Goal)t.m_vGoals.elementAt(i);
            b.put((byte)Clip(g.m_nMinute, 255));
            b.put((byte)Math.max(-1, Math.min(127, g.m_nNumber)));
            b.put((byte)(g.m_bOwnGoal ? 1 : 0));
         }

         n = t.m_vSupers.size();
         b.put((byte)n);

         for (int i = 0; i < n; i++) {
            int[] s = (int[])t.m_vSupers.elementAt(i);
            b.put((byte)Clip(s[0], 255));
            b.put((byte)Clip(s[1], 255));
         }

         n = t.m_vShots.size();
         b.put((byte)n);

         for (int i = 0; i < n; i++) {
            Shot s = (Shot)t.m_vShots.elementAt(i);
            b.put((byte)Clip(s.m_nMinute, 255));
            b.put((byte)Math.max(-1, Math.min(127, s.m_nNumber)));
            b.put((byte)Clip(Math.round(s.m_fDepth * 255.0F), 255));
            b.put((byte)Clip(Math.round(s.m_fLateral * 255.0F), 255));
            b.put((byte)s.m_nResult);
         }

         java.util.Vector vPlayers = new java.util.Vector();

         for (int i = 0; i < cTeam.m_vFootBallers.size(); i++) {
            AIFootBaller cFB = (AIFootBaller)cTeam.m_vFootBallers.elementAt(i);
            if (!cFB.IsReferee() && GetListIndex(cFB) >= 0) {
               vPlayers.addElement(cFB);
            }
         }

         b.put((byte)vPlayers.size());

         for (int i = 0; i < vPlayers.size(); i++) {
            AIFootBaller cFB = (AIFootBaller)vPlayers.elementAt(i);
            PlayerStats p = GetPlayer(cFB);
            b.put((byte)GetListIndex(cFB));
            b.put((byte)ModTactics.GetSlot(cFB));
            b.put((byte)Clip(p.m_nGoals, 255));
            b.put((byte)Clip(p.m_nShots, 255));
            b.put((byte)Clip(p.m_nPasses, 255));
            b.put((byte)Clip(p.m_nPassesOk, 255));
            b.put((byte)Clip(p.m_nRecoveries, 255));
            b.putShort((short)Clip(Math.round(p.m_fDistance * 100.0F), 65535)); // longueur du terrain = 100 m
            float fDepth = p.m_nSamples > 0 ? p.m_fSumDepth / p.m_nSamples : 0.0F;
            float fLateral = p.m_nSamples > 0 ? p.m_fSumLateral / p.m_nSamples : 0.5F;
            b.put((byte)Clip(Math.round(fDepth * 255.0F), 255));
            b.put((byte)Clip(Math.round(fLateral * 255.0F), 255));
         }

         PutHeat(b, t.m_tHeat);
         PutHeat(b, t.m_tBallHeat);
      }
   }

   // heatmap ramenée à 0..255 : la case la plus fréquentée vaut 255
   private static void PutHeat(java.nio.ByteBuffer b, int[] tHeat) {
      int nMax = 0;

      for (int i = 0; i < tHeat.length; i++) {
         nMax = Math.max(nMax, tHeat[i]);
      }

      for (int i = 0; i < tHeat.length; i++) {
         b.put((byte)(nMax > 0 ? (int)((long)tHeat[i] * 255L / nMax) : 0));
      }
   }

   // minute « football » (90 min) à partir du temps réel du match
   static int GetMinute(AIMatch cMatch, float fTime) {
      if (cMatch.m_nHalfTime <= 0) {
         return 0;
      }

      int nMinute = (int)(fTime * 45.0F / cMatch.m_nHalfTime) + 1;
      if (!cMatch.m_bGoldGoal) {
         nMinute = Math.min(nMinute, cMatch.m_nHalfNo == 0 ? 45 : 90);
      }

      return nMinute;
   }

   // place d'un joueur dans la liste de son équipe : le gardien en premier (« 1. Blufon » = Buffon),
   // puis les joueurs de champ dans l'ordre de l'équipe (le gardien, lui, apparaît en dernier : n6)
   static int GetListIndex(AIFootBaller cFB) {
      if (cFB.IsGoalKeeper()) {
         return 0;
      }

      int nIndex = 1;

      for (int i = 0; i < cFB.m_cTeam.m_vFootBallers.size(); i++) {
         AIFootBaller cOther = (AIFootBaller)cFB.m_cTeam.m_vFootBallers.elementAt(i);
         if (cOther == cFB) {
            return nIndex;
         }

         if (!cOther.IsGoalKeeper()) {
            nIndex++;
         }
      }

      return -1;
   }

   // nom d'un joueur d'après sa place dans la liste de l'équipe (texts.scr, Team<Animal>Players : « 1. Nom \n2. Nom ... »)
   static String GetPlayerName(int nAnimal, int nNumber) {
      if (nAnimal >= 0 && nNumber >= 0) {
         StringTokenizer tok = new StringTokenizer(ETeams.GetTeamData(nAnimal).m_sPlayers, "\n");

         for (int i = 0; tok.hasMoreTokens(); i++) {
            String sLine = tok.nextToken().trim();
            if (i == nNumber) {
               int nDot = sLine.indexOf(". ");
               return nDot >= 0 && nDot < 3 ? sLine.substring(nDot + 2).trim() : sLine;
            }
         }
      }

      return "?";
   }

   // buteurs d'une équipe, une ligne par joueur : « Bambillas 12' 30' »
   static String[] GetScorers(Team cTeam) {
      java.util.Vector vNames = new java.util.Vector();
      Hashtable tMinutes = new Hashtable();
      java.util.Vector vGoals = Get(cTeam).m_vGoals;

      for (int i = 0; i < vGoals.size(); i++) {
         Goal g = (Goal)vGoals.elementAt(i);
         String sName = GetPlayerName(g.m_nAnimal, g.m_nNumber);
         if (g.m_bOwnGoal) {
            sName = sName + " " + Text.Get("&StatsOwnGoal&");
         }

         StringBuffer sb = (StringBuffer)tMinutes.get(sName);
         if (sb == null) {
            sb = new StringBuffer(sName);
            tMinutes.put(sName, sb);
            vNames.addElement(sName);
         }

         sb.append(" " + g.m_nMinute + "'");
      }

      String[] tLines = new String[_MAX_SCORER_LINES];

      for (int i = 0; i < _MAX_SCORER_LINES; i++) {
         if (i >= vNames.size()) {
            tLines[i] = "";
         } else if (i == _MAX_SCORER_LINES - 1 && vNames.size() > _MAX_SCORER_LINES) {
            tLines[i] = "...";
         } else {
            tLines[i] = tMinutes.get(vNames.elementAt(i)).toString();
         }
      }

      return tLines;
   }

   // découpe un texte multiligne (les textes de texts.scr contiennent « \n »)
   static String[] SplitLines(String s, int nCount) {
      String[] tLines = new String[nCount];
      int nStart = 0;
      // « \n » peut rester sous forme de deux caractères selon la façon dont le texte est lu
      for (int k = s == null ? -1 : s.indexOf("\\n"); k >= 0; k = s.indexOf("\\n")) {
         s = s.substring(0, k) + "\n" + s.substring(k + 2);
      }


      for (int i = 0; i < nCount; i++) {
         int nEnd = s == null ? -1 : s.indexOf('\n', nStart);
         if (s == null || nStart > s.length()) {
            tLines[i] = "";
         } else if (nEnd < 0) {
            tLines[i] = s.substring(nStart).trim();
            nStart = s.length() + 1;
         } else {
            tLines[i] = s.substring(nStart, nEnd).trim();
            nStart = nEnd + 1;
         }
      }

      return tLines;
   }

   // plus grand nombre de buts d'un même joueur de l'équipe (hors csc et buteur inconnu)
   static int GetTopScorerGoals(Team cTeam) {
      Hashtable tCount = new Hashtable();
      int nBest = 0;
      java.util.Vector vGoals = Get(cTeam).m_vGoals;

      for (int i = 0; i < vGoals.size(); i++) {
         Goal g = (Goal)vGoals.elementAt(i);
         if (!g.m_bOwnGoal && g.m_nNumber >= 0) {
            Integer nKey = new Integer(g.m_nNumber);
            Integer nOld = (Integer)tCount.get(nKey);
            int n = nOld == null ? 1 : nOld.intValue() + 1;
            tCount.put(nKey, new Integer(n));
            nBest = Math.max(nBest, n);
         }
      }

      return nBest;
   }

   // valeurs d'une équipe pour l'écran de fin de match : tirs, possession, passes, récupérations, arrêts, supers
   static String[] GetValues(AIMatch cMatch, int nTeam, int nPossession) {
      Team cTeam = cMatch.GetTeam(nTeam);
      TeamStats s = Get(cTeam);
      return new String[]{
         "" + cMatch.GetShootCount(nTeam), nPossession + "%", s.m_nPassesOk + "/" + s.m_nPasses, "" + s.m_nRecoveries, "" + s.m_nSaves,
         "" + cMatch.GetSuperCount(nTeam)
      };
   }

   // ---------------------------------------------------------------- historique

   // seuls les matchs d'un humain contre l'IA sont enregistrés (comme pour l'Elo)
   private static boolean IsRecordedMatch() {
      return !CoreLogic.m_bDemo && !CoreLogic.m_bMultiplayer && SoccerModule.m_cMatch != null && CoreLogic.GetPlayer() != null;
   }

   // appelé par AIMatch.AfterMatch, après ModElo.OnMatchEnd
   static void OnMatchEnd(AIMatch cMatch) {
      if (IsRecordedMatch()) {
         AddRecord(cMatch, false);
         ModAchievements.OnMatchEnd();
      }
   }

   // appelé par MenuInGame quand on quitte ou recommence un match en cours, après ModElo.OnAbandon
   static void OnAbandon() {
      if (IsRecordedMatch()) {
         AddRecord(SoccerModule.m_cMatch, true);
      }
   }

   private static void AddRecord(AIMatch cMatch, boolean bAbandon) {
      // index de l'hôte : même règle que ModElo.OnMatchEnd
      int nHost = cMatch.m_nHalfNo != 0 && !cMatch.m_bGoldGoal ? 1 : 0;
      int nMine = cMatch.GetTeam(nHost).m_bUserControled ? nHost : 1 - nHost;
      boolean bHost = nMine == nHost;
      Team cMine = cMatch.GetTeam(nMine);
      Team cOpp = cMatch.GetTeam(1 - nMine);
      TeamStats s = Get(cMine);
      Record r = new Record();
      r.m_sDate = new SimpleDateFormat("dd/MM/yy HH:mm").format(new Date());
      int[] v = r.m_tValues;
      v[_MODE] = CoreLogic.m_bChampionships ? _MODE_CUP : (SoccerGame.GetSoccerModule().m_nLevelID >= 15 ? _MODE_EXTRA : _MODE_FRIENDLY);
      if (ModSurvival.IsActive()) {
         v[_MODE] = _MODE_SURVIVAL;
         v[_SURVIVAL_WAVE] = ModSurvival.GetWave();
      }

      if (ModLeagueMode.IsActive() || ModOnlineLeague.m_bMatch) {
         v[_MODE] = _MODE_LEAGUE; // [mod] ligue, solo ou en ligne
      }

      v[_MY_TEAM] = cMine.m_nAnimalType;
      v[_OPP_TEAM] = cOpp.m_nAnimalType;
      v[_GOALS_FOR] = bHost ? cMatch.m_nScoreLeft : cMatch.m_nScoreRight;
      v[_GOALS_AGAINST] = bHost ? cMatch.m_nScoreRight : cMatch.m_nScoreLeft;
      v[_LEVEL] = CoreLogic.GetDiffLevel();
      v[_RATED] = !bAbandon && ModElo.m_bLastRated || bAbandon && ModElo.m_bLastAbandonRated ? 1 : 0;
      v[_ELO_DELTA] = v[_RATED] == 1 ? ModElo.m_nLastDelta : 0;
      v[_ABANDON] = bAbandon ? 1 : 0;
      v[_SHOTS] = cMatch.GetShootCount(nMine);
      v[_POSSESSION] = (int)cMatch.GetEntirePossesion(nMine); // arrondi comme sur l'écran de fin de match
      v[_PASSES_OK] = s.m_nPassesOk;
      v[_PASSES] = s.m_nPasses;
      v[_RECOVERIES] = s.m_nRecoveries;
      v[_SAVES] = s.m_nSaves;
      v[_SUPERS] = cMatch.GetSuperCount(nMine);
      v[_TOP_SCORER_GOALS] = GetTopScorerGoals(cMine);
      v[_CHEAT] = ModOptions.IsMatchCheat() ? 1 : 0;
      v[_MAX_DEFICIT] = s.m_nMaxDeficit;
      v[_GOLDEN_GOAL] = cMatch.m_bGoldGoal ? 1 : 0;
      // but de la victoire : le but qui donne l'avance définitive, donc le (buts encaissés + 1)e
      if (!bAbandon && v[_GOALS_FOR] > v[_GOALS_AGAINST] && v[_GOALS_AGAINST] < s.m_vGoals.size()) {
         v[_WINNER_MINUTE] = ((Goal)s.m_vGoals.elementAt(v[_GOALS_AGAINST])).m_nMinute;
      }

      v[_SUPER_TYPES] = s.m_nSuperTypes;
      // finale de coupe (Championships, phase 3) gagnée ; le menu du championnat ne le saura qu'au retour
      Championships ch = CoreLogic.GetPlayer().m_cChampionships;
      if (!bAbandon && CoreLogic.m_bChampionships && ch != null && ch.m_nPhase == 3 && v[_GOALS_FOR] > v[_GOALS_AGAINST]) {
         v[_CUP_WON] = ch.m_nCurChampionships + 1;
      }
      java.util.Vector vHistory = GetHistory();
      vHistory.addElement(r);
      while (vHistory.size() > _MAX_HISTORY) {
         vHistory.removeElementAt(0);
      }

      Save();
      GameObject.CrashLog("[mod] stats : match enregistre (" + GetProfileName() + ") " + v[_GOALS_FOR] + "-" + v[_GOALS_AGAINST] + (bAbandon ? " abandon" : "")
         + ", tirs " + v[_SHOTS] + ", passes " + v[_PASSES_OK] + "/" + v[_PASSES] + ", recup " + v[_RECOVERIES] + ", arrets " + v[_SAVES] + "\n");
   }

   static String GetProfileName() {
      PlayerSave player = CoreLogic.GetPlayer();
      if (player == null || player.m_Name == null) {
         return "default";
      }

      // ';' sert de séparateur dans stats.dat
      return player.m_Name.replace(';', ',');
   }

   // historique du profil courant, du plus ancien au plus récent
   static java.util.Vector GetHistory() {
      Load();
      String sName = GetProfileName();
      java.util.Vector v = (java.util.Vector)m_tHistory.get(sName);
      if (v == null) {
         v = new java.util.Vector();
         m_tHistory.put(sName, v);
      }

      return v;
   }

   // stats.dat : une ligne par match, « profil;date;champ0;champ1;... »
   private static void Load() {
      if (m_bLoaded) {
         return;
      }

      m_bLoaded = true;
      File f = new File(_SAVE_FILE);
      if (!f.exists()) {
         return;
      }

      try {
         BufferedReader in = new BufferedReader(new FileReader(f));

         for (String sLine = in.readLine(); sLine != null; sLine = in.readLine()) {
            StringTokenizer tok = new StringTokenizer(sLine, ";");
            // les champs ajoutés plus tard manquent dans les anciennes lignes : ils restent à 0
            if (tok.countTokens() >= 3) {
               String sName = tok.nextToken();
               Record r = new Record();
               r.m_sDate = tok.nextToken();

               for (int i = 0; i < _FIELDS && tok.hasMoreTokens(); i++) {
                  r.m_tValues[i] = Integer.parseInt(tok.nextToken().trim());
               }

               java.util.Vector v = (java.util.Vector)m_tHistory.get(sName);
               if (v == null) {
                  v = new java.util.Vector();
                  m_tHistory.put(sName, v);
               }

               v.addElement(r);
            }
         }

         in.close();
      } catch (Exception e) {
         GameObject.CrashLog("[mod] stats : lecture de " + _SAVE_FILE + " impossible : " + e + "\n");
      }
   }

   private static void Save() {
      try {
         FileWriter out = new FileWriter(_SAVE_FILE);
         Enumeration names = m_tHistory.keys();

         while (names.hasMoreElements()) {
            String sName = (String)names.nextElement();
            java.util.Vector v = (java.util.Vector)m_tHistory.get(sName);

            for (int i = 0; i < v.size(); i++) {
               Record r = (Record)v.elementAt(i);
               out.write(sName + ";" + r.m_sDate);

               for (int k = 0; k < _FIELDS; k++) {
                  out.write(";" + r.m_tValues[k]);
               }

               out.write("\r\n");
            }
         }

         out.close();
      } catch (Exception e) {
         GameObject.CrashLog("[mod] stats : ecriture de " + _SAVE_FILE + " impossible : " + e + "\n");
      }
   }

   // ---------------------------------------------------------------- écran Profil

   static String GetModeName(int nMode) {
      return Text.Get("&StatsMode" + nMode + "&");
   }

   // 1 victoire, 0 nul, -1 défaite (un abandon est une défaite)
   static int GetOutcome(Record r) {
      int[] v = r.m_tValues;
      if (v[_ABANDON] == 1 || v[_GOALS_FOR] < v[_GOALS_AGAINST]) {
         return -1;
      } else {
         return v[_GOALS_FOR] > v[_GOALS_AGAINST] ? 1 : 0;
      }
   }

   // bilan du profil : libellés et valeurs, une ligne par statistique
   static String[] GetSummary() {
      java.util.Vector vHistory = GetHistory();
      int nGames = vHistory.size();
      int nWins = 0;
      int nDraws = 0;
      int nLosses = 0;
      int nFor = 0;
      int nAgainst = 0;
      int nClean = 0;
      int nHatTricks = 0;
      int nStreak = 0;
      int nBestStreak = 0;
      int nShots = 0;
      int nPossession = 0;
      int nPassesOk = 0;
      int nPasses = 0;
      int nRecoveries = 0;
      int nSaves = 0;
      Record cBest = null;

      for (int i = 0; i < nGames; i++) {
         Record r = (Record)vHistory.elementAt(i);
         int[] v = r.m_tValues;
         int nOutcome = GetOutcome(r);
         if (nOutcome > 0) {
            nWins++;
            nStreak++;
            nBestStreak = Math.max(nBestStreak, nStreak);
            int nMargin = v[_GOALS_FOR] - v[_GOALS_AGAINST];
            int[] b = cBest != null ? cBest.m_tValues : null;
            if (b == null || nMargin > b[_GOALS_FOR] - b[_GOALS_AGAINST] || nMargin == b[_GOALS_FOR] - b[_GOALS_AGAINST] && v[_GOALS_FOR] > b[_GOALS_FOR]) {
               cBest = r;
            }
         } else {
            nStreak = 0;
            if (nOutcome == 0) {
               nDraws++;
            } else {
               nLosses++;
            }
         }

         nFor += v[_GOALS_FOR];
         nAgainst += v[_GOALS_AGAINST];
         if (v[_GOALS_AGAINST] == 0 && v[_ABANDON] == 0) {
            nClean++;
         }

         if (v[_TOP_SCORER_GOALS] >= 3) {
            nHatTricks++;
         }

         nShots += v[_SHOTS];
         nPossession += v[_POSSESSION];
         nPassesOk += v[_PASSES_OK];
         nPasses += v[_PASSES];
         nRecoveries += v[_RECOVERIES];
         nSaves += v[_SAVES];
      }

      String sBest = "-";
      if (cBest != null) {
         sBest = cBest.m_tValues[_GOALS_FOR] + "-" + cBest.m_tValues[_GOALS_AGAINST] + " " + ETeams.GetTeamData(cBest.m_tValues[_OPP_TEAM]).m_sNameMin;
      }

      String sValues = nGames + "\n"
         + nWins + " / " + nDraws + " / " + nLosses + "\n"
         + nFor + " / " + nAgainst + "\n"
         + nClean + "\n"
         + nHatTricks + "\n"
         + sBest + "\n"
         + nBestStreak + "\n"
         + nStreak + "\n"
         + Average(nShots, nGames) + "\n"
         + (nGames > 0 ? Math.round((float)nPossession / nGames) + "%" : "-") + "\n"
         + (nPasses > 0 ? Math.round(100.0F * nPassesOk / nPasses) + "%" : "-") + "\n"
         + Average(nRecoveries, nGames) + "\n"
         + Average(nSaves, nGames) + "\n"
         + GetSurvivalRecordText() + "\n";
      return new String[]{Text.Get("&ProfileSummaryLabels&"), sValues};
   }

   // record de survie (ModSurvival.GetRecord) : « 7 manches », « - » sans série
   static String GetSurvivalRecordText() {
      int n = ModSurvival.GetRecord();
      return n > 0 ? n + " " + Text.Get(n > 1 ? "&SurvivalWaves&" : "&SurvivalWave&") : "-";
   }

   // moyenne par match, un chiffre après la virgule
   private static String Average(int nTotal, int nGames) {
      if (nGames == 0) {
         return "-";
      }

      int n10 = Math.round(10.0F * nTotal / nGames);
      return n10 / 10 + "," + n10 % 10;
   }

   // ligne de l'historique : « Pandr. 3 - 1 Lions »
   static String GetMatchText(Record r) {
      int[] v = r.m_tValues;
      String s = ETeams.GetTeamData(v[_MY_TEAM]).m_sNameMin + "  " + v[_GOALS_FOR] + " - " + v[_GOALS_AGAINST] + "  " + ETeams.GetTeamData(v[_OPP_TEAM]).m_sNameMin;
      if (v[_ABANDON] == 1) {
         s = s + " " + Text.Get("&StatsAbandon&");
      }

      return s;
   }

   static String GetEloText(Record r) {
      if (r.m_tValues[_RATED] == 0) {
         if (r.m_tValues[_CHEAT] == 1) {
            return Text.Get("&StatsCheat&");
         }

         // [mod] survie non classée : numéro de la manche
         return r.m_tValues[_SURVIVAL_WAVE] > 0 ? Text.Get("&StatsWave&") + r.m_tValues[_SURVIVAL_WAVE] : "";
      }

      int n = r.m_tValues[_ELO_DELTA];
      return n >= 0 ? "+" + n : "" + n;
   }

   // jour et mois : « 10/11 »
   static String GetShortDate(Record r) {
      return r.m_sDate.length() >= 5 ? r.m_sDate.substring(0, 5) : r.m_sDate;
   }
}
