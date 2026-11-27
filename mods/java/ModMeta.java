// [mod] Matchs de méta (notes/meta.md) : un match démo IA contre IA lancé depuis debugconf.scr, au niveau choisi et
// avec la durée d'une partie classée. À la fin, le jeu écrit meta_result.txt (statistiques au format de
// PKT_MATCH_STATS, voir ModStats.PutMatchStats) puis se ferme : mods\meta\meta_run.py l'enregistre dans la base.
//
//   !MetaMatch(i,i,i,i,i)   MetaMatch(équipe A, tenue A, équipe B, tenue B, niveau 1..10) : A à gauche au coup d'envoi
//   !MetaHalf(i)            durée d'une mi-temps en secondes (240 = RankedHalf(4) de online.scr)
//   !MetaSpeed(f)           accélération du temps du jeu (1 = temps réel)
//   !MetaSchemes(s,s)       schéma prédéfini de A et de B (roles.scr, ex. BETON) ; "" = celui de l'IA
class ModMeta {
   static final String _RESULT = "meta_result.txt";
   static boolean m_bActive = false;
   static int m_nAnimalA = 0;
   static int m_nClothesA = 0;
   static int m_nAnimalB = 1;
   static int m_nClothesB = 0;
   static int m_nLevel = 8;
   static int m_nHalf = 240;
   static float m_fSpeed = 1.0F;
   static String m_sSchemeA = null;
   static String m_sSchemeB = null;
   private static boolean m_bStarted = false;
   private static boolean m_bDone = false;
   private static float m_fStartAbs = 0.0F;

   static boolean IsActive() {
      return m_bActive;
   }

   // ModTactics.GetTeamComposition : schéma imposé à cette équipe, null = choix habituel de l'IA. Les équipes sont
   // reconnues à leur animal : pas de match miroir (même animal des deux côtés) avec deux schémas.
   static String Scheme(Team t) {
      if (!m_bActive || t == null || m_nAnimalA == m_nAnimalB) {
         return null;
      }

      String s = t.m_nAnimalType == m_nAnimalA ? m_sSchemeA : t.m_nAnimalType == m_nAnimalB ? m_sSchemeB : null;
      return s != null && s.length() > 0 ? s : null;
   }

   // accélération appliquée à tout TimerSetSpeedUp du module de match (ralentis compris)
   static float Speed(float f) {
      return m_bActive ? f * m_fSpeed : f;
   }

   // MenuMain.Show : le premier passage au menu lance le match
   static boolean ShouldStart() {
      return m_bActive && !m_bStarted;
   }

   static void Start() {
      m_bStarted = true;
      CoreLogic.m_bDemo = true;
      CoreLogic.m_nNextLevel = ETeams.GetHomeField(m_nAnimalA);
      CoreLogic.m_cTeamDataLeft = ETeams.GetTeamData(m_nAnimalA);
      CoreLogic.m_cTeamDataLeft.SetClothesId(m_nClothesA);
      CoreLogic.m_cTeamDataRight = ETeams.GetTeamData(m_nAnimalB);
      CoreLogic.m_cTeamDataRight.SetClothesId(m_nClothesB);
      m_fStartAbs = GameObject.GetAbsoluteTime();
      GameObject.CrashLog("[mod] meta : " + m_nAnimalA + " contre " + m_nAnimalB + ", niveau " + m_nLevel + ", mi-temps " + m_nHalf
         + " s, vitesse " + m_fSpeed + (m_sSchemeA != null ? ", schemas " + m_sSchemeA + " / " + m_sSchemeB : "") + "\n");
      SoccerGame.game.GetCommentator().HideNow();
      SoccerGame.game.RunModule();
   }

   // Test1.InitTeams (via ModNet.OnTeamsReady) : durée de la mi-temps, tactiques retenues pour le résultat
   static void OnTeamsReady(AIMatch cMatch) {
      if (!m_bActive) {
         return;
      }

      cMatch.m_nHalfTime = m_nHalf;
      SoccerGame.GetSoccerModule().TimerSetSpeedUp(Speed(1.0F));
   }

   // Diagnostic de la possession (2026-11-27, Bullrusia à 41 % dans toutes les séries) : ce que vaut le temps de
   // balle de chaque équipe, jeu en cours (état 0) seulement. N'agit pas sur le match : il ne fait que compter.
   //   0 tient le ballon (m_cBallPossesor)   1 ballon libre après sa touche   2 dont le gardien tient le ballon
   //   3 temps pendant un de ses supers (Team.Update ne compte rien alors)    4 phases de jeu (série de touches de
   //   l'équipe)   5 dont finies par un arrêt de jeu (sortie, but, faute)   6 temps de jeu total (état 0)
   //   7 temps de la phase la plus longue
   static final int _DIAG_N = 8;
   private static Team m_cDiagTouch = null; // dernière équipe à avoir eu le ballon dans la phase en cours
   private static float m_fDiagSpellStart = 0.0F;
   private static float m_fDiagFrame = -1.0F; // temps du match déjà traité pour les phases (une fois par image)

   static void DiagPossession(AITeam t) {
      if (!m_bActive || SoccerModule.m_cMatch == null || SoccerModule.m_Mod == null) {
         return;
      }

      float fNow = SoccerModule.m_Mod.m_Clock.GetMatchTimeF();
      float dt = t.m_fModDiagLast < 0.0F ? 0.0F : fNow - t.m_fModDiagLast;
      t.m_fModDiagLast = fNow;
      if (dt < 0.0F || dt > 0.5F) {
         dt = 0.0F;
      }

      AIMatch cMatch = SoccerModule.m_cMatch;
      boolean bPlay = AIMatch.IsMatchState(0);
      if (fNow != m_fDiagFrame) { // phases : une seule fois par image, pour les deux équipes
         m_fDiagFrame = fNow;
         Team cPoss = cMatch.m_cBallPossesor;
         if (!bPlay) {
            if (m_cDiagTouch != null) {
               DiagEndSpell(fNow);
               ((AITeam)m_cDiagTouch).m_fModDiag[5]++;
               m_cDiagTouch = null;
            }
         } else if (cPoss != null && cPoss != m_cDiagTouch) {
            if (m_cDiagTouch != null) {
               DiagEndSpell(fNow);
            }

            m_cDiagTouch = cPoss;
            m_fDiagSpellStart = fNow;
            ((AITeam)cPoss).m_fModDiag[4]++;
         }
      }

      if (!bPlay || dt <= 0.0F) {
         return;
      }

      float[] d = t.m_fModDiag;
      d[6] += dt;
      Team cPoss = cMatch.m_cBallPossesor;
      if (cPoss == t) {
         d[0] += dt;
         if (GameField.m_cBall != null && GameField.m_cBall.GetCurOwner() == t.m_cGoalKeeper) {
            d[2] += dt;
         }
      } else if (cPoss == null && m_cDiagTouch == t) {
         d[1] += dt;
      }

      if (t.m_bSuperDoing) {
         d[3] += dt;
      }
   }

   private static void DiagEndSpell(float fNow) {
      float[] d = ((AITeam)m_cDiagTouch).m_fModDiag;
      d[7] = Math.max(d[7], fNow - m_fDiagSpellStart);
   }

   // une équipe : animal, puis les valeurs de DiagPossession, puis le compteur du jeu (Team.m_fEntirePossesionTime)
   private static String DiagLine(Team t) {
      StringBuffer s = new StringBuffer();
      s.append(t.m_nAnimalType);
      float[] d = ((AITeam)t).m_fModDiag;
      for (int i = 0; i < d.length; i++) {
         s.append(' ').append((int)(d[i] * 10.0F + 0.5F) / 10.0F);
      }

      s.append(' ').append((int)(t.GetEntirePossesion() * 10.0F + 0.5F) / 10.0F);
      return s.toString();
   }

   // MenuInGame.Show à la fin du match démo : résultat écrit, puis le jeu se ferme
   static void OnMatchEnd() {
      if (!m_bActive || m_bDone) {
         return;
      }

      m_bDone = true;
      AIMatch cMatch = SoccerModule.m_cMatch;

      try {
         java.nio.ByteBuffer b = java.nio.ByteBuffer.allocate(4000);
         ModStats.PutMatchStats(b, cMatch, 0);
         b.flip();
         StringBuffer hex = new StringBuffer();

         while (b.hasRemaining()) {
            int v = b.get() & 255;
            hex.append(Character.forDigit(v >> 4, 16)).append(Character.forDigit(v & 15, 16));
         }

         int nHost = cMatch.m_nHalfNo != 0 && !cMatch.m_bGoldGoal ? 1 : 0;
         String sLineA = ModTactics.TeamLine(cMatch.GetTeam(nHost));
         String sLineB = ModTactics.TeamLine(cMatch.GetTeam(1 - nHost));
         // le score reste celui des côtés du coup d'envoi (gauche = A), même après l'échange de la mi-temps
         int nScoreA = cMatch.m_nScoreLeft;
         int nScoreB = cMatch.m_nScoreRight;
         float fReal = GameObject.GetAbsoluteTime() - m_fStartAbs;
         String nl = "\r\n";
         String s = "ok" + nl
            + m_nAnimalA + " " + m_nClothesA + " " + m_nAnimalB + " " + m_nClothesB + " " + m_nLevel + " " + m_nHalf + " " + CoreLogic.m_nNextLevel + nl
            + nScoreA + " " + nScoreB + nl
            + sLineA + nl
            + sLineB + nl
            + hex.toString() + nl
            + (int)fReal + nl
            + DiagLine(cMatch.GetTeam(nHost)) + " | " + DiagLine(cMatch.GetTeam(1 - nHost)) + nl;
         java.io.FileOutputStream out = new java.io.FileOutputStream(_RESULT + ".tmp");
         out.write(s.getBytes("ISO-8859-1"));
         out.close();
         java.io.File f = new java.io.File(_RESULT);
         f.delete();
         new java.io.File(_RESULT + ".tmp").renameTo(f);
         GameObject.CrashLog("[mod] meta : fin " + nScoreA + "-" + nScoreB + " en " + (int)fReal + " s reelles\n");
      } catch (Throwable e) {
         GameObject.CrashLog("[mod] meta : ERREUR resultat " + e + "\n");
      }

      GameObject.ExitGame();
   }
}
