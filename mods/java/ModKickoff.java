// [mod] coup d'envoi : traces (DebugLog(1) de roles.scr) et test KickoffTest(n)
public class ModKickoff {
   static int m_nTest = 0; // roles.scr KickoffTest(n) : relance un coup d'envoi n s après le précédent (tests seulement)
   private static AIMatch m_cMatch = null;
   private static int m_nLastState = -1;
   private static float m_fTraceEnd = -1.0F;
   private static float m_fTraceNext = 0.0F;
   private static float m_fKickoffTime = -1.0F;
   private static int m_nTestSide = 0;
   private static int m_nCount = 0;
   private static AIFootBaller m_cReceiver = null;
   private static AITeam m_cKickTeam = null;

   private static float Now() {
      return SoccerGame.GetSoccerModule().GetTime();
   }

   private static String P(Vector v) {
      if (v == null) {
         return "-";
      }

      Vector f = GameField.WorldToField(v);
      return (int)f.fZ + "/" + (int)f.fX;
   }

   private static String Who(FootBaller cFB) {
      if (cFB == null) {
         return "personne";
      }

      String s = cFB.m_cTeam.m_sName + " n" + cFB.m_nNumberInTeam;
      if (cFB instanceof AIFootBaller) {
         AIFootBaller a = (AIFootBaller)cFB;
         Goal g = a.GetGoal();
         s = s + " but " + (g != null ? g.m_nId + (g.m_bReached ? "ok" : "") : "-1") + (a.HasSpecialGoal() ? " special" : "") + (a.IsPassDest() ? " destpasse" : "")
            + (a.IsUserControled() ? " humain" : "") + " pos " + P(a.GetPosition());
      }

      return s;
   }

   // AIFootBaller.PassSpecialBall : la passe d'engagement part (avant RealKick)
   static void OnPass(AIFootBaller cPasser, AIFootBaller cDest, Vector vDest) {
      if (!ModTactics.IsDebug() || !AIMatch.IsMatchState(7)) {
         return;
      }

      m_nCount++;
      m_cReceiver = cDest != null ? cDest : SoccerModule.m_cMatch.m_cSpecialDestFB;
      m_cKickTeam = (AITeam)cPasser.m_cTeam;
      GameObject.CrashLog("[mod] engagement #" + m_nCount + " " + Who(cPasser) + " -> " + Who(m_cReceiver) + " vise " + P(vDest)
         + " (ecart receveur " + (m_cReceiver != null && vDest != null ? (int)m_cReceiver.GetPosition().Distance(vDest) : -1) + ")\n");
   }

   // AIMatch.Update, à chaque image
   static void Update(AIMatch cMatch) {
      if (cMatch != m_cMatch) {
         m_cMatch = cMatch;
         m_nLastState = -1;
         m_fTraceEnd = -1.0F;
         m_fKickoffTime = -1.0F;
      }

      int nState = AIMatch.GetMatchState();
      float fNow = Now();
      if (nState != m_nLastState) {
         if (m_nLastState == 7 && nState == 0) {
            m_fKickoffTime = fNow;
            m_fTraceEnd = fNow + 5.0F;
            m_fTraceNext = fNow;
         } else if (ModTactics.IsDebug() && fNow < m_fTraceEnd) {
            GameObject.CrashLog("[mod] engagement #" + m_nCount + " etat " + m_nLastState + " -> " + nState
               + (nState == 5 ? " TOUCHE" : "") + " apres " + (int)((fNow - m_fKickoffTime) * 10.0F) / 10.0F + " s\n");
            m_fTraceEnd = -1.0F;
         }

         m_nLastState = nState;
      }

      if (ModTactics.IsDebug() && nState == 0 && fNow < m_fTraceEnd && fNow >= m_fTraceNext) {
         m_fTraceNext = fNow + 0.25F;
         Ball cBall = GameField.m_cBall;
         String s = "[mod]   t+" + (int)((fNow - m_fKickoffTime) * 100.0F) / 100.0F + " ballon " + P(cBall.GetPosition()) + " cible " + P(GameField.m_vDestPosition)
            + " porteur " + Who(cBall.GetOwner());
         if (cBall.GetOwner() == null) {
            for (int t = 0; t < 2; t++) {
               AITeam cTeam = (AITeam)cMatch.GetTeam(t);
               s = s + " | actif " + Who(cTeam.m_cActiveFootBaller);
            }

            if (m_cReceiver != null) {
               s = s + " | receveur " + Who(m_cReceiver);
            }
         } else {
            m_fTraceEnd = -1.0F;
         }

         GameObject.CrashLog(s + "\n");
      }

      // test : un nouveau coup d'envoi toutes les m_nTest s, côtés alternés
      if (m_nTest > 0 && nState == 0 && m_fKickoffTime >= 0.0F && fNow > m_fKickoffTime + m_nTest && !cMatch.m_bWaiting) {
         m_fKickoffTime = -1.0F;
         m_nTestSide = 1 - m_nTestSide;
         GameObject.CrashLog("[mod] test : nouveau coup d'envoi, equipe " + m_nTestSide + "\n");
         Match.m_cModule.StartFadeIn();
         cMatch.RestartGameRaw(m_nTestSide);
      }
   }
}
