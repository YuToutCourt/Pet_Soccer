import java.util.Hashtable;

// [mod] Déplacements sans ballon quand l'équipe a le ballon : démarquage, appels en profondeur, une-deux,
// et passe « dans la course » (le ballon part là où le coureur arrivera). Réglages dans data\roles.scr (voir notes/tactics.md).
class ModMovement {
   // état d'un joueur de champ
   static class State {
      float m_fNextFree = -100.0F; // prochain calcul du démarquage
      boolean m_bHasOff = false; // décalage choisi autour de la position de rôle
      float m_fOffX = 0.0F;
      float m_fOffZ = 0.0F;
      Vector m_vRun = null; // course en cours (cible), null = aucune
      int m_nRunKind = 0; // 1 appel en profondeur, 2 une-deux
      float m_fRunStart = -100.0F;
      float m_fRunEnd = -100.0F;
      float m_fNextRunCheck = -100.0F;
      float m_fRunCooldown = -100.0F;
   }

   static final int _RUN_DEEP = 1;
   static final int _RUN_GIVEGO = 2;

   private static boolean m_bLoaded = false;
   private static boolean m_bEnabled = true;
   private static boolean m_bPassToBest = true;
   private static float m_fFreeRefresh = 0.35F;
   private static float m_fFreeKeep = 0.12F;
   private static float m_fRunCheck = 0.4F;
   private static float m_fRunChance = 0.5F;
   private static float m_fRunDuration = 3.5F; // durée maximale
   private static float m_fRunFrom = 0.25F; // un appel part d'au plus cette distance derrière la ligne de hors-jeu
   private static float m_fRunPassWindow = 1.0F;
   private static float m_fRunCooldownTime = 3.0F;
   private static float m_fRunDepth = 0.14F;
   private static int m_nMaxRunners = 1;
   private static float m_fRunSpeed = 0.0F; // 0 = FBSpeed d'ai_params.scr
   private static float m_fThroughLead = 900.0F;
   private static float m_fThroughMargin = 0.1F; // un défenseur peut arriver jusqu'à ce temps avant le coureur (il doit se retourner)
   private static float m_fThroughTurn = 0.5F; // retard d'un défenseur qui doit se retourner
   private static float m_fKickDelay = 0.25F;
   private static float m_fAIThroughChance = 0.6F;
   private static float m_fGiveGoMaxDist = 1200.0F;
   private static float m_fGiveGoDuration = 1.6F;
   // schéma ORIGINE (pas de rôle) : Free, Runs, GiveGo, Drop selon le type d'origine
   private static float[] m_tOriginAttacker = new float[]{0.07F, 0.6F, 0.4F, 0.2F};
   private static float[] m_tOriginDefender = new float[]{0.04F, 0.0F, 0.0F, 0.0F};

   private static Hashtable m_tStates = new Hashtable(); // AIFootBaller -> State
   private static AIMatch m_cMatch = null;
   private static float[] m_fNextAIThrough = new float[]{-100.0F, -100.0F};
   // DebugLog : tests d'appel refusés (0 rôle/repos/porteur gardien/max, 1 ligne trop haute, 2 trop loin de la ligne,
   // 3 porteur devant, 4 trop loin du porteur, 5 tirage, 6 pas de couloir libre, 7 appels lancés)
   private static int[] m_nWhy = new int[8];

   static String DebugSummary() {
      String s = "";

      for (int i = 0; i < m_nWhy.length; i++) {
         s = s + " " + m_nWhy[i];
         m_nWhy[i] = 0;
      }

      return s;
   }

   static void Load() {
      if (m_bLoaded) {
         return;
      }

      m_bLoaded = true;

      for (ParseKeyword key = GameObject.ParseFile(ModTactics._FILE); key != null; key = key.GetNext()) {
         if (key.Equals("Movement")) {
            m_bEnabled = key.GetIParam(0) != 0;
         } else if (key.Equals("PassToBest")) {
            m_bPassToBest = key.GetIParam(0) != 0;
         } else if (key.Equals("FreeRefresh")) {
            m_fFreeRefresh = key.GetFParam(0);
         } else if (key.Equals("FreeKeep")) {
            m_fFreeKeep = key.GetFParam(0);
         } else if (key.Equals("RunCheck")) {
            m_fRunCheck = key.GetFParam(0);
         } else if (key.Equals("RunChance")) {
            m_fRunChance = key.GetFParam(0);
         } else if (key.Equals("RunFrom")) {
            m_fRunFrom = key.GetFParam(0);
         } else if (key.Equals("RunDuration")) {
            m_fRunDuration = key.GetFParam(0);
         } else if (key.Equals("RunPassWindow")) {
            m_fRunPassWindow = key.GetFParam(0);
         } else if (key.Equals("RunCooldown")) {
            m_fRunCooldownTime = key.GetFParam(0);
         } else if (key.Equals("RunDepth")) {
            m_fRunDepth = key.GetFParam(0);
         } else if (key.Equals("MaxRunners")) {
            m_nMaxRunners = key.GetIParam(0);
         } else if (key.Equals("RunSpeed")) {
            m_fRunSpeed = key.GetFParam(0);
         } else if (key.Equals("ThroughLead")) {
            m_fThroughLead = key.GetFParam(0);
         } else if (key.Equals("ThroughMargin")) {
            m_fThroughMargin = key.GetFParam(0);
         } else if (key.Equals("ThroughTurn")) {
            m_fThroughTurn = key.GetFParam(0);
         } else if (key.Equals("KickDelay")) {
            m_fKickDelay = key.GetFParam(0);
         } else if (key.Equals("AIThroughChance")) {
            m_fAIThroughChance = key.GetFParam(0);
         } else if (key.Equals("GiveGoMaxDist")) {
            m_fGiveGoMaxDist = key.GetFParam(0);
         } else if (key.Equals("GiveGoDuration")) {
            m_fGiveGoDuration = key.GetFParam(0);
         } else if (key.Equals("OriginAttacker")) {
            m_tOriginAttacker = new float[]{key.GetFParam(0), key.GetFParam(1), key.GetFParam(2), key.GetFParam(3)};
         } else if (key.Equals("OriginDefender")) {
            m_tOriginDefender = new float[]{key.GetFParam(0), key.GetFParam(1), key.GetFParam(2), key.GetFParam(3)};
         }
      }

      GameObject.CrashLog("[mod] deplacements " + (m_bEnabled ? "actifs" : "desactives") + (m_bPassToBest ? ", passe au mieux place" : "") + "\n");
   }

   static boolean IsEnabled() {
      Load();
      return m_bEnabled;
   }

   // correction du jeu d'origine (Strategy.FindBestFootBaller gardait le coéquipier le MOINS bien placé)
   static boolean PassToBest() {
      Load();
      return m_bPassToBest;
   }

   private static float Now() {
      return SoccerGame.GetSoccerModule().GetTime();
   }

   private static State Get(AIFootBaller cFB) {
      if (SoccerModule.m_cMatch != m_cMatch) {
         m_cMatch = SoccerModule.m_cMatch;
         m_tStates.clear();
         m_fNextAIThrough[0] = m_fNextAIThrough[1] = -100.0F;
      }

      State s = (State)m_tStates.get(cFB);
      if (s == null) {
         s = new State();
         m_tStates.put(cFB, s);
      }

      return s;
   }

   private static State Peek(AIFootBaller cFB) {
      return SoccerModule.m_cMatch != m_cMatch ? null : (State)m_tStates.get(cFB);
   }

   // ---------------------------------------------------------------- paramètres du joueur (rôle ou type d'origine)

   private static float Param(AIFootBaller cFB, int n) {
      ModTactics.Role r = ModTactics.GetRoleOf(cFB);
      if (r != null) {
         switch (n) {
            case 0:
               return r.m_fFree;
            case 1:
               return r.m_fRuns;
            case 2:
               return r.m_fGiveGo;
            default:
               return r.m_fDrop;
         }
      }

      return (ModTactics.GetOrigType(cFB) == 0 ? m_tOriginAttacker : m_tOriginDefender)[n];
   }

   private static float RunSpeed() {
      return m_fRunSpeed > 0.0F ? m_fRunSpeed : (Strategy.m_fFBSpeed > 0.0F ? Strategy.m_fFBSpeed : 420.0F);
   }

   // ---------------------------------------------------------------- géométrie

   private static float Depth(int nSide, Vector v) {
      float f = (v.fZ - GameField.vFieldLogicalStart.fZ) / GameField.fLineEnd;
      return nSide == 0 ? f : 1.0F - f;
   }

   private static float ZOf(int nSide, float fDepth) {
      return GameField.vFieldLogicalStart.fZ + (nSide == 0 ? fDepth : 1.0F - fDepth) * GameField.fLineEnd;
   }

   private static float XFrac(Vector v) {
      return (v.fX - GameField.vFieldLogicalStart.fX) / GameField.fLineOutTop;
   }

   private static float XOf(float fFrac) {
      return GameField.vFieldLogicalStart.fX + fFrac * GameField.fLineOutTop;
   }

   // garde le point dans le terrain (marges latérales du mod, pas collé aux lignes de but)
   private static void ClampField(int nSide, Vector v) {
      v.fX = XOf(ModTactics.Clamp(XFrac(v), ModTactics._FIELD_MARGIN, 1.0F - ModTactics._FIELD_MARGIN));
      v.fZ = ZOf(nSide, ModTactics.Clamp(Depth(nSide, v), 0.04F, 0.92F));
   }

   private static float Dist2D(Vector a, Vector b) {
      float dx = a.fX - b.fX;
      float dz = a.fZ - b.fZ;
      return (float)Math.sqrt(dx * dx + dz * dz);
   }

   // distance de p au segment [a, b] (plan du terrain)
   private static float SegDist(Vector a, Vector b, Vector p) {
      float dx = b.fX - a.fX;
      float dz = b.fZ - a.fZ;
      float len2 = dx * dx + dz * dz;
      float t = len2 > 1.0F ? ((p.fX - a.fX) * dx + (p.fZ - a.fZ) * dz) / len2 : 0.0F;
      t = ModTactics.Clamp(t, 0.0F, 1.0F);
      float qx = a.fX + t * dx - p.fX;
      float qz = a.fZ + t * dz - p.fZ;
      return (float)Math.sqrt(qx * qx + qz * qz);
   }

   private static Team Opponents(Team cTeam) {
      Team cTeam0 = SoccerModule.m_cMatch.GetTeam(0);
      return cTeam0 == cTeam ? SoccerModule.m_cMatch.GetTeam(1) : cTeam0;
   }

   // profondeur maximale autorisée sans ballon (sous la ligne de hors-jeu), 1 si le hors-jeu doux est coupé
   private static float DepthLimit(AIFootBaller cFB) {
      return ModTactics.IsOffsideOn() ? ModTactics.OffsideLine(cFB) - ModTactics.GetOffsideMargin() : 1.0F;
   }

   // ---------------------------------------------------------------- note d'une place (démarquage)

   private static float Score(Team cTeam, AIFootBaller cFB, Vector vHolder, Vector c, Vector vBase, float fRadius) {
      int nSide = cFB.m_nTeam;
      Team cOpp = Opponents(cTeam);
      float fMinOpp = 100000.0F;
      float fLane = 100000.0F;

      for (int i = 0; i < cOpp.m_vFootBallers.size(); i++) {
         FootBaller o = (FootBaller)cOpp.m_vFootBallers.elementAt(i);
         if (!o.IsReferee()) {
            Vector vo = o.GetPosition();
            fMinOpp = Math.min(fMinOpp, Dist2D(vo, c));
            fLane = Math.min(fLane, SegDist(vHolder, c, vo));
         }
      }

      float fScore = ModTactics.Clamp(fMinOpp / 600.0F, 0.0F, 1.0F); // espace libre autour
      fScore += 1.2F * ModTactics.Clamp((fLane - 60.0F) / 250.0F, 0.0F, 1.0F); // ligne de passe dégagée
      fScore += 4.0F * 0.4F * (Depth(nSide, c) - Depth(nSide, vBase)); // vers l'avant
      float fD = Dist2D(vHolder, c);
      if (fD < 500.0F) {
         fScore -= (500.0F - fD) / 500.0F; // pas collé au porteur
      } else if (fD > 2400.0F) {
         fScore -= (fD - 2400.0F) / 1500.0F; // pas trop loin non plus
      }

      for (int i = 0; i < cTeam.m_vFootBallers.size(); i++) {
         AIFootBaller m = (AIFootBaller)cTeam.m_vFootBallers.elementAt(i);
         if (m != cFB && !m.IsGoalKeeper()) {
            float dm = Dist2D(m.GetPosition(), c);
            if (dm < 450.0F) {
               fScore -= (450.0F - dm) / 450.0F; // espacement entre coéquipiers
            }
         }
      }

      return fScore - Dist2D(c, vBase) / (fRadius * 4.0F); // reste près de sa zone
   }

   private static void ChooseFree(AITeam cTeam, AIFootBaller cFB, AIFootBaller cHolder, Vector vBase, float fRadius, State s) {
      int nSide = cFB.m_nTeam;
      float fLimit = DepthLimit(cFB);
      Vector vHolder = cHolder.GetPosition();
      Vector vBest = null;
      float fBest = -1000.0F;

      for (int ring = 0; ring < 3; ring++) {
         float r = fRadius * ring * 0.5F;
         int nDirs = ring == 0 ? 1 : 8;

         for (int k = 0; k < nDirs; k++) {
            double a = k * Math.PI / 4.0 + (ring == 2 ? Math.PI / 8.0 : 0.0);
            Vector c = new Vector(vBase);
            c.fX = c.fX + (float)Math.cos(a) * r;
            c.fZ = c.fZ + (float)Math.sin(a) * r;
            ClampField(nSide, c);
            if (ring > 0 && Depth(nSide, c) > fLimit) {
               continue;
            }

            float f = Score(cTeam, cFB, vHolder, c, vBase, fRadius);
            if (f > fBest) {
               fBest = f;
               vBest = c;
            }
         }
      }

      if (vBest == null) {
         return;
      }

      // on garde l'ancienne place sauf si la nouvelle est nettement meilleure (pas de va-et-vient)
      if (s.m_bHasOff) {
         Vector vOld = new Vector(vBase);
         vOld.fX = vOld.fX + s.m_fOffX;
         vOld.fZ = vOld.fZ + s.m_fOffZ;
         ClampField(nSide, vOld);
         if (Depth(nSide, vOld) <= fLimit && Score(cTeam, cFB, vHolder, vOld, vBase, fRadius) + m_fFreeKeep >= fBest) {
            return;
         }
      }

      s.m_bHasOff = true;
      s.m_fOffX = vBest.fX - vBase.fX;
      s.m_fOffZ = vBest.fZ - vBase.fZ;
   }

   // ---------------------------------------------------------------- appels en profondeur

   private static int CountRunners(Team cTeam, float fNow) {
      int n = 0;

      for (int i = 0; i < cTeam.m_vFootBallers.size(); i++) {
         State s = Peek((AIFootBaller)cTeam.m_vFootBallers.elementAt(i));
         if (s != null && s.m_vRun != null && s.m_nRunKind == _RUN_DEEP && fNow < s.m_fRunEnd) {
            n++;
         }
      }

      return n;
   }

   // meilleur couloir pour une course vers la profondeur fTargetD (droit, vers l'axe ou vers l'extérieur)
   private static Vector RunTarget(Team cTeam, AIFootBaller cFB, Vector vFrom, Vector vPasser, float fTargetD) {
      int nSide = cFB.m_nTeam;
      float fx = XFrac(cFB.GetPosition());
      float[] tX = new float[]{fx, fx + (0.5F - fx) * 0.5F, fx - (0.5F - fx) * 0.3F};
      Team cOpp = Opponents(cTeam);
      Vector vBest = null;
      float fBest = -1000.0F;

      for (int k = 0; k < tX.length; k++) {
         Vector c = new Vector(cFB.GetPosition());
         c.fX = XOf(tX[k]);
         c.fZ = ZOf(nSide, fTargetD);
         ClampField(nSide, c);
         float fMinOpp = 100000.0F;
         float fLane = 100000.0F;

         for (int i = 0; i < cOpp.m_vFootBallers.size(); i++) {
            FootBaller o = (FootBaller)cOpp.m_vFootBallers.elementAt(i);
            if (!o.IsReferee()) {
               fMinOpp = Math.min(fMinOpp, Dist2D(o.GetPosition(), c));
               fLane = Math.min(fLane, SegDist(vPasser, c, o.GetPosition()));
            }
         }

         float f = ModTactics.Clamp(fMinOpp / 700.0F, 0.0F, 1.0F) + 0.6F * ModTactics.Clamp((fLane - 60.0F) / 300.0F, 0.0F, 1.0F);
         if (k == 0) {
            f += 0.05F; // à égalité, tout droit
         }

         if (fMinOpp > 250.0F && f > fBest) {
            fBest = f;
            vBest = c;
         }
      }

      return vBest;
   }

   private static boolean TryStartRun(AITeam cTeam, AIFootBaller cFB, AIFootBaller cHolder, State s, float fNow) {
      float fRuns = Param(cFB, 1);
      if (fRuns <= 0.0F) {
         return false;
      }

      if (fNow < s.m_fRunCooldown || cHolder.IsGoalKeeper() || CountRunners(cTeam, fNow) >= m_nMaxRunners) {
         m_nWhy[0]++;
         return false;
      }

      int nSide = cFB.m_nTeam;
      float fLine = ModTactics.OffsideLine(cFB);
      float fMy = Depth(nSide, cFB.GetPosition());
      float fHolder = Depth(nSide, cHolder.GetPosition());
      // il faut de l'espace derrière la défense, être proche de la ligne, et que le porteur soit derrière lui
      int nWhy = fLine > 0.86F ? 1 : (fMy < fLine - m_fRunFrom ? 2 : (fHolder > fMy + 0.03F ? 3 : (cFB.GetDistanceTo(cHolder) > 3200.0F ? 4 : 0)));
      if (nWhy == 0 && GameObject.RandomFloat(1.0F) > fRuns * m_fRunChance) {
         nWhy = 5;
      }

      Vector vTarget = nWhy == 0 ? RunTarget(cTeam, cFB, cFB.GetPosition(), cHolder.GetPosition(), Math.min(fLine + m_fRunDepth, 0.9F)) : null;
      if (nWhy == 0 && vTarget == null) {
         nWhy = 6;
      }

      m_nWhy[nWhy == 0 ? 7 : nWhy]++;
      if (nWhy != 0) {
         return false;
      }

      // durée selon la distance à courir (bornée)
      float fDuration = ModTactics.Clamp(Dist2D(cFB.GetPosition(), vTarget) / RunSpeed() + 0.4F, 1.2F, m_fRunDuration);
      StartRun(cFB, s, vTarget, _RUN_DEEP, fDuration, fNow);
      if (!cTeam.m_bUserControled) {
         // le porteur IA voit l'appel partir et décide tout de suite
         m_fNextAIThrough[cTeam == SoccerModule.m_cMatch.GetTeam(0) ? 0 : 1] = -100.0F;
         AIThroughPass(cTeam, cHolder);
      }

      return true;
   }

   private static void StartRun(AIFootBaller cFB, State s, Vector vTarget, int nKind, float fDuration, float fNow) {
      s.m_vRun = vTarget;
      s.m_nRunKind = nKind;
      s.m_fRunStart = fNow;
      s.m_fRunEnd = fNow + fDuration;
      s.m_bHasOff = false;
      if (ModTactics.IsDebug()) {
         GameObject.CrashLog("[mod] " + cFB.m_cTeam.m_sName + " n" + cFB.m_nNumberInTeam + (nKind == _RUN_DEEP ? " appel en profondeur" : " une-deux")
            + " vers " + (int)(Depth(cFB.m_nTeam, vTarget) * 100.0F) + "/" + (int)(XFrac(vTarget) * 100.0F) + "\n");
      }
   }

   private static void EndRun(State s, float fNow) {
      if (s.m_vRun != null) {
         s.m_vRun = null;
         s.m_fRunCooldown = fNow + m_fRunCooldownTime;
      }
   }

   // ---------------------------------------------------------------- appelés depuis ModTactics / Strategy / AITeam / AIFootBaller

   // ModTactics.UpdateValidPos, jeu courant, joueur de champ sans ballon dirigé par l'IA : renvoie la nouvelle place, ou null
   static Vector Adjust(AITeam cTeam, AIFootBaller cFB, Vector vBase) {
      if (!IsEnabled() || cFB.HasBall()) {
         return null;
      }

      State s = Get(cFB);
      float fNow = Now();
      if (!cTeam.IsPossesor()) {
         s.m_bHasOff = false;
         EndRun(s, fNow);
         return null;
      }

      if (s.m_vRun != null) {
         if (fNow < s.m_fRunEnd) {
            return new Vector(s.m_vRun);
         }

         EndRun(s, fNow);
      }

      int nSide = cFB.m_nTeam;
      FootBaller cOwner = GameField.m_cBall.GetOwner();
      if (cOwner != null && cOwner.m_cTeam == cTeam && cOwner != cFB) {
         AIFootBaller cHolder = (AIFootBaller)cOwner;
         if (fNow >= s.m_fNextRunCheck || fNow < s.m_fNextRunCheck - 10.0F) {
            s.m_fNextRunCheck = fNow + m_fRunCheck * (0.75F + GameObject.RandomFloat(0.5F));
            // le porteur IA peut le servir tout de suite (OnPass termine alors la course : m_vRun redevient null)
            if (TryStartRun(cTeam, cFB, cHolder, s, fNow) && s.m_vRun != null) {
               return new Vector(s.m_vRun);
            }
         }

         float fRadius = Param(cFB, 0) * GameField.fLineEnd;
         if (fRadius <= 0.0F) {
            return null;
         }

         // décrochage : le joueur loin devant le porteur revient un peu vers lui
         float fDrop = Param(cFB, 3);
         float fGap = Depth(nSide, vBase) - Depth(nSide, cHolder.GetPosition()) - 0.12F;
         if (fDrop > 0.0F && fGap > 0.0F) {
            vBase = new Vector(vBase);
            vBase.fZ = ZOf(nSide, Depth(nSide, vBase) - fDrop * Math.min(fGap, 0.2F));
         }

         if (fNow >= s.m_fNextFree || fNow < s.m_fNextFree - 10.0F || !s.m_bHasOff) {
            s.m_fNextFree = fNow + m_fFreeRefresh * (0.8F + GameObject.RandomFloat(0.4F));
            ChooseFree(cTeam, cFB, cHolder, vBase, fRadius, s);
         }
      }

      if (!s.m_bHasOff) {
         return null;
      }

      // ballon en l'air ou porteur : garde le décalage choisi autour de sa place du moment
      Vector v = new Vector(vBase);
      v.fX = v.fX + s.m_fOffX;
      v.fZ = v.fZ + s.m_fOffZ;
      ClampField(nSide, v);
      return v;
   }

   // en pleine course (le hors-jeu doux ne le retient pas)
   static boolean IsRunning(AIFootBaller cFB) {
      State s = Peek(cFB);
      return s != null && s.m_vRun != null && Now() < s.m_fRunEnd;
   }

   // course partie il y a peu : il était en jeu au départ, on peut encore le servir
   static boolean IsFreshRun(AIFootBaller cFB) {
      State s = Peek(cFB);
      if (s == null || s.m_vRun == null) {
         return false;
      }

      float fAge = Now() - s.m_fRunStart;
      return fAge >= 0.0F && fAge < m_fRunPassWindow && Now() < s.m_fRunEnd;
   }

   static String Describe(AIFootBaller cFB) {
      State s = Peek(cFB);
      if (s == null) {
         return "";
      }

      if (s.m_vRun != null && Now() < s.m_fRunEnd) {
         return s.m_nRunKind == _RUN_DEEP ? " appel" : " une-deux";
      }

      return s.m_bHasOff ? " demarque" : "";
   }

   // Strategy.ChosePassPosition : passe vers un coureur = ballon dans sa course, si personne n'y arrive avant lui
   static Vector ThroughPoint(AIFootBaller cPasser, AIFootBaller cDest) {
      if (!IsEnabled() || cPasser == null || cDest == null || cPasser.m_cTeam != cDest.m_cTeam || !AIMatch.IsMatchState(0)) {
         return null;
      }

      State s = Peek(cDest);
      float fNow = Now();
      if (s == null || s.m_vRun == null || fNow >= s.m_fRunEnd) {
         return null;
      }

      int nSide = cDest.m_nTeam;
      Vector vRunner = cDest.GetPosition();
      Vector vDir = new Vector(s.m_vRun);
      vDir.Sub(vRunner);
      vDir.fY = 0.0F;
      if (vDir.Len() < 100.0F) {
         // presque arrivé : continue vers le but adverse
         vDir = new Vector(0.0F, 0.0F, nSide == 0 ? 1.0F : -1.0F);
      }

      vDir.Normalize();
      float fSpeed = RunSpeed();
      Vector vPasser = cPasser.GetPosition();
      Vector v = null;
      float t = 0.5F;

      for (int i = 0; i < 3; i++) {
         float fLead = Math.min(fSpeed * t, m_fThroughLead);
         v = new Vector(vRunner);
         v.fX = v.fX + vDir.fX * fLead;
         v.fZ = v.fZ + vDir.fZ * fLead;
         ClampField(nSide, v);
         if (Depth(nSide, v) > 0.9F) {
            v.fZ = ZOf(nSide, 0.9F);
         }

         float d = Dist2D(vPasser, v);
         t = d / Strategy.CalcSpeed(d) + m_fKickDelay;
      }

      // le coureur doit arriver avant tout adversaire (gardien compris)
      float tRunner = Dist2D(vRunner, v) / fSpeed;
      Team cOpp = Opponents(cDest.m_cTeam);

      for (int i = 0; i < cOpp.m_vFootBallers.size(); i++) {
         FootBaller o = (FootBaller)cOpp.m_vFootBallers.elementAt(i);
         // un défenseur qui doit courir vers son propre but part en retard (il se retourne)
         float fReact = Depth(nSide, o.GetPosition()) < Depth(nSide, v) ? m_fThroughTurn : 0.0F;
         if (!o.IsReferee() && (Dist2D(o.GetPosition(), v) - 100.0F) / fSpeed + fReact < tRunner - m_fThroughMargin) {
            return null;
         }
      }

      v.fY = vRunner.fY;
      return v;
   }

   // AITeam : le porteur IA sert un coureur dans sa course (à la place de sa décision de passe habituelle)
   static boolean AIThroughPass(AITeam cTeam, AIFootBaller cHolder) {
      if (!IsEnabled() || cTeam.m_bUserControled || cHolder.IsGoalKeeper() || !cHolder.HasBall() || !AIMatch.IsMatchState(0)) {
         return false;
      }

      int t = cTeam == SoccerModule.m_cMatch.GetTeam(0) ? 0 : 1;
      float fNow = Now();
      Get(cHolder);
      if (fNow < m_fNextAIThrough[t] && fNow > m_fNextAIThrough[t] - 10.0F) {
         return false;
      }

      m_fNextAIThrough[t] = fNow + 0.3F;

      for (int i = 0; i < cTeam.m_vFootBallers.size(); i++) {
         AIFootBaller m = (AIFootBaller)cTeam.m_vFootBallers.elementAt(i);
         if (m != cHolder && !m.IsGoalKeeper() && IsFreshRun(m)) {
            Vector v = ThroughPoint(cHolder, m);
            boolean bLuck = GameObject.RandomFloat(1.0F) < m_fAIThroughChance;
            if (ModTactics.IsDebug() && (v == null || !bLuck)) {
               GameObject.CrashLog("[mod] " + cTeam.m_sName + " n" + m.m_nNumberInTeam + " pas servi : " + (v == null ? "adversaire plus pres" : "tirage") + "\n");
            }

            if (v != null && bLuck) {
               if (ModTactics.IsDebug()) {
                  GameObject.CrashLog("[mod] " + cTeam.m_sName + " passe dans la course n" + cHolder.m_nNumberInTeam + " -> n" + m.m_nNumberInTeam + "\n");
               }

               cTeam.m_cAISystem.RequestEvent(cHolder, 8, new AIFootBaller.Pass(m, v));
               return cHolder.m_bHasSingleUpdate; // événement refusé : décision d'origine
            }
         }
      }

      return false;
   }

   // AIFootBaller.Pass : la course du receveur s'arrête (il va au ballon) ; le passeur peut partir en une-deux
   static void OnPass(AIFootBaller cPasser, AIFootBaller cReceiver) {
      if (!IsEnabled() || !AIMatch.IsMatchState(0)) {
         return;
      }

      float fNow = Now();
      State sr = Peek(cReceiver);
      if (sr != null) {
         if (sr.m_vRun != null && ModTactics.IsDebug()) {
            GameObject.CrashLog("[mod] " + cReceiver.m_cTeam.m_sName + " n" + cReceiver.m_nNumberInTeam + " servi pendant son " + (sr.m_nRunKind == _RUN_DEEP ? "appel" : "une-deux") + "\n");
         }

         EndRun(sr, fNow);
      }

      if (cPasser.IsGoalKeeper() || cReceiver.IsGoalKeeper() || cPasser.GetDistanceTo(cReceiver) > m_fGiveGoMaxDist) {
         return;
      }

      float fChance = Param(cPasser, 2);
      if (fChance <= 0.0F || GameObject.RandomFloat(1.0F) > fChance) {
         return;
      }

      int nSide = cPasser.m_nTeam;
      float fFrom = Depth(nSide, cPasser.GetPosition());
      float fTarget = Math.max(Math.min(fFrom + 0.12F, ModTactics.OffsideLine(cPasser) + 0.06F), fFrom + 0.04F);
      Vector vTarget = RunTarget(cPasser.m_cTeam, cPasser, cPasser.GetPosition(), cReceiver.GetPosition(), Math.min(fTarget, 0.9F));
      if (vTarget != null) {
         StartRun(cPasser, Get(cPasser), vTarget, _RUN_GIVEGO, m_fGiveGoDuration, fNow);
      }
   }
}
