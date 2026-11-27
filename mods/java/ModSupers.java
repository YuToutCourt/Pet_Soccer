// [mod] Nouveaux supers : gel, ballon fou, aimant, tir double (voir notes/supers.md)
// Types 4..7 de Team.m_nSuperType, à la suite du jeu d'origine (0 tir, 1 feinte, 2 vitesse, 3 mur).
// Réglages dans data/supers.scr. Tout est piloté depuis AITeam.Update (Update) et AIFootBaller.UseSuper (Use).
class ModSupers {
   static final String _FILE = "data/supers.scr";
   static final int _FREEZE = 4;
   static final int _CRAZY = 5;
   static final int _MAGNET = 6;
   static final int _DOUBLE = 7;
   static final int _TYPES = 8;
   static final String _DECOY_MESH = "data\\meshes\\objects\\pilka.3da";
   static final String _FREEZE_SOUND = "data/sounds/sniezyca.wav";
   static final String _MAGNET_SOUND = "data/sounds/super jakis.wav";
   static final String _CRAZY_SOUND = "data/sounds/superpilka3.wav";
   static final String _DOUBLE_SOUND = "data/sounds/super jakis 2.wav";
   // intervalle entre deux bouffées de fumée derrière le ballon fou
   static final float _SMOKE_PERIOD = 0.04F;
   // part de la distance de tir (la fin) sur laquelle le zigzag se resserre
   static final float _CRAZY_FADE = 0.25F;
   static boolean m_bLoaded = false;
   // poids du tirage au sort (Team.ResetSuper) ; jeu d'origine : 25 pour chacun des 4 premiers
   static float[] m_fChance = new float[]{15.0F, 15.0F, 15.0F, 15.0F, 10.0F, 10.0F, 10.0F, 10.0F};
   static float m_fFreezeTime = 5.0F;
   static float m_fFreezeSpeed = 0.5F;
   static boolean m_bFreezeKeeper = true;
   static Vector m_vFreezeColor = new Vector(0.55F, 0.75F, 1.0F);
   static float m_fCrazyAmplitude = 0.4F;
   static float m_fCrazyFrequency = 2.5F;
   static float m_fCrazyAim = 0.25F;
   static float m_fCrazyTime = 3.0F;
   static float m_fMagnetTime = 6.0F;
   static float m_fMagnetRadius = 700.0F;
   static float m_fMagnetPull = 6.0F;
   static float m_fMagnetMinSpeed = 600.0F;
   static float m_fDoubleSpread = 0.3F;
   static float m_fDoubleFoolChance = 50.0F;
   static float m_fDoubleTime = 2.5F;
   // secondes de possession par lettre de la jauge (Strategy.m_fSuperUpgradeTime) ; 0 = valeur du jeu (6)
   static float m_fFillTime = 10.0F;
   // autres sources de lettres (AddAction) : une lettre toutes les X actions de l'équipe ; 0 = source désactivée
   static final int _ACT_SHOT = 0;
   static final int _ACT_RECOVERY = 1;
   static final int _ACT_SAVE = 2;
   static final String[] _ACT_NAMES = new String[]{"tir cadre", "recuperation", "arret"};
   static int[] m_nPerLetter = new int[]{8, 15, 8};
   // [mod] rattrapage (méta du 2026-11-27) : l'équipe menée remplit sa jauge plus vite, x (1 + m_fCatchUpPerGoal par
   // but de retard), au plus x m_fCatchUpMax ; 0 = désactivé
   static float m_fCatchUpPerGoal = 0.0F;
   static float m_fCatchUpMax = 1.0F;
   // lettre d'un but : 0 = personne, 1 = l'équipe qui marque (jeu d'origine), 2 = l'équipe qui encaisse
   static final int _GOAL_LETTER_NONE = 0;
   static final int _GOAL_LETTER_SCORER = 1;
   static final int _GOAL_LETTER_CONCEDER = 2;
   static int m_nGoalLetter = _GOAL_LETTER_SCORER;
   // effets à durée, par côté (Team.m_nSide) : fin en temps de match, -1 = inactif
   static float[] m_fFreezeEnd = new float[]{-1.0F, -1.0F};
   static float[] m_fMagnetEnd = new float[]{-1.0F, -1.0F};
   static int m_nMagnetSound = -1;
   static float m_fPrevTime = -1.0F;
   // ballon fou en vol
   static Team m_cCrazyTeam = null;
   static Vector m_vCrazyDir = null;
   static Vector m_vCrazySide = null;
   static Vector m_vCrazyStart = null;
   static float m_fCrazyDist = 0.0F;
   static float m_fCrazySign = 1.0F;
   static float m_fCrazyZigAmp = 0.0F;
   static float m_fCrazyZigLeft = 0.0F;
   static float m_fCrazyT = 0.0F;
   static float m_fCrazySmoke = 0.0F;
   static int m_nCrazySound = -1;
   // leurre du tir double (créé une fois par match)
   static MeshObject m_cDecoy = null;
   static Ball m_cDecoyBall = null;
   static float m_fDecoyScale = 1.0F;
   static Team m_cDoubleTeam = null;
   static Vector m_vDecoyPos = null;
   static Vector m_vDecoyVel = null;
   static float m_fDecoyT = 0.0F;
   static int m_nDecoySound = -1;

   static void Load() {
      if (m_bLoaded) {
         return;
      }

      m_bLoaded = true;
      ParseKeyword root = GameObject.ParseFile(_FILE);
      if (root == null) {
         GameObject.CrashLog("[mod] " + _FILE + " absent, valeurs par defaut\n");
         return;
      }

      for (ParseKeyword key = root; key != null; key = key.GetNext()) {
         if (key.Equals("SuperChance")) {
            int nType = key.GetIParam(0);
            if (nType >= 0 && nType < _TYPES) {
               m_fChance[nType] = Math.max(0.0F, key.GetFParam(1));
            }
         } else if (key.Equals("FreezeTime")) {
            m_fFreezeTime = key.GetFParam(0);
         } else if (key.Equals("FreezeSpeed")) {
            m_fFreezeSpeed = key.GetFParam(0);
         } else if (key.Equals("FreezeKeeper")) {
            m_bFreezeKeeper = key.GetIParam(0) != 0;
         } else if (key.Equals("FreezeColor")) {
            m_vFreezeColor = new Vector(key.GetFParam(0), key.GetFParam(1), key.GetFParam(2));
         } else if (key.Equals("CrazyAmplitude")) {
            m_fCrazyAmplitude = key.GetFParam(0);
         } else if (key.Equals("CrazyFrequency")) {
            m_fCrazyFrequency = key.GetFParam(0);
         } else if (key.Equals("CrazyAim")) {
            m_fCrazyAim = key.GetFParam(0);
         } else if (key.Equals("CrazyTime")) {
            m_fCrazyTime = key.GetFParam(0);
         } else if (key.Equals("MagnetTime")) {
            m_fMagnetTime = key.GetFParam(0);
         } else if (key.Equals("MagnetRadius")) {
            m_fMagnetRadius = key.GetFParam(0);
         } else if (key.Equals("MagnetPull")) {
            m_fMagnetPull = key.GetFParam(0);
         } else if (key.Equals("MagnetMinSpeed")) {
            m_fMagnetMinSpeed = key.GetFParam(0);
         } else if (key.Equals("DoubleSpread")) {
            m_fDoubleSpread = key.GetFParam(0);
         } else if (key.Equals("DoubleFoolChance")) {
            m_fDoubleFoolChance = key.GetFParam(0);
         } else if (key.Equals("DoubleTime")) {
            m_fDoubleTime = key.GetFParam(0);
         } else if (key.Equals("FillTime")) {
            m_fFillTime = Math.max(0.0F, key.GetFParam(0));
         } else if (key.Equals("ShotsPerLetter")) {
            m_nPerLetter[_ACT_SHOT] = Math.max(0, key.GetIParam(0));
         } else if (key.Equals("RecoveriesPerLetter")) {
            m_nPerLetter[_ACT_RECOVERY] = Math.max(0, key.GetIParam(0));
         } else if (key.Equals("SavesPerLetter")) {
            m_nPerLetter[_ACT_SAVE] = Math.max(0, key.GetIParam(0));
         } else if (key.Equals("CatchUpPerGoal")) {
            m_fCatchUpPerGoal = Math.max(0.0F, key.GetFParam(0));
         } else if (key.Equals("CatchUpMax")) {
            m_fCatchUpMax = Math.max(1.0F, key.GetFParam(0));
         } else if (key.Equals("GoalLetter")) {
            m_nGoalLetter = Math.max(0, Math.min(2, key.GetIParam(0)));
         }
      }

      GameObject.CrashLog("[mod] supers charges depuis " + _FILE + " (FillTime " + m_fFillTime + ", lettre tous les " + m_nPerLetter[_ACT_SHOT] + " tirs / " + m_nPerLetter[_ACT_RECOVERY] + " recuperations / " + m_nPerLetter[_ACT_SAVE] + " arrets, rattrapage " + m_fCatchUpPerGoal + " par but max " + m_fCatchUpMax
         + ", lettre du but " + m_nGoalLetter + ")\n");
   }

   // ---------------------------------------------------------------- jauge

   // une action de l'équipe (tir cadré, récupération, arrêt : ModStats) ;
   // toutes les m_nPerLetter[nAct] actions, une lettre de plus (Team.IncreaseSuperFillCount gère HUD, son, disponibilité)
   static void AddAction(Team t, int nAct) {
      AddAction(t, nAct, true);
   }

   // bInPlay = false pour un tir cadré qui finit en but : le jeu n'est déjà plus « en cours » à ce moment-là
   static void AddAction(Team t, int nAct, boolean bInPlay) {
      Load();
      if (!(t instanceof AITeam) || nAct < 0 || nAct >= m_nPerLetter.length || m_nPerLetter[nAct] <= 0) {
         return;
      }

      // en ligne, seul l'hôte simule ; pas de lettre hors du jeu, en ralenti, au but en or, pendant un super,
      // ni quand la jauge est pleine (pas de réserve)
      if (ModNet.IsClient() || SoccerModule.m_cMatch == null || GameField.m_cBall == null || bInPlay && !AIMatch.IsMatchState(0)
         || GameField.m_cBall.IsReplaying() || SoccerModule.m_cMatch.m_bGoldGoal || t.m_bSuperDoing || t.m_nSuperFillCount >= 5) {
         return;
      }

      int[] n = ((AITeam)t).m_nModSuperActions;
      n[nAct]++;
      // [mod] rattrapage : l'équipe menée a besoin de moins d'actions par lettre
      int nNeed = Math.max(1, (int)Math.ceil(m_nPerLetter[nAct] / CatchUpMul(t)));
      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("[mod] lettre super : " + t.m_sName + " " + _ACT_NAMES[nAct] + " " + n[nAct] + "/" + nNeed + "\n");
      }

      if (n[nAct] >= nNeed) {
         n[nAct] = 0;
         t.IncreaseSuperFillCount();
      }
   }

   // AIMatch.Restart : nouveau match, compteurs à zéro
   static void ResetCounters(Match cMatch) {
      for (int s = 0; s < 2; s++) {
         Team t = cMatch.GetTeam(s);
         if (t instanceof AITeam) {
            int[] n = ((AITeam)t).m_nModSuperActions;
            for (int i = 0; i < n.length; i++) {
               n[i] = 0;
            }

            ((AITeam)t).m_fModCatchUpTime = 0.0F;
         }
      }
   }

   // [mod] buts de retard de l'équipe t (0 si elle mène ou s'il y a égalité). Le score reste celui des côtés du coup
   // d'envoi (m_nScoreLeft = équipe de gauche au coup d'envoi, même après l'échange de la mi-temps) : même règle
   // d'index que ModStats.AddRecord
   static int Deficit(Team t) {
      AIMatch cMatch = SoccerModule.m_cMatch;
      if (cMatch == null) {
         return 0;
      }

      int nHost = cMatch.m_nHalfNo != 0 && !cMatch.m_bGoldGoal ? 1 : 0;
      boolean bHost = t.m_nSide == nHost;
      int nFor = bHost ? cMatch.m_nScoreLeft : cMatch.m_nScoreRight;
      int nAgainst = bHost ? cMatch.m_nScoreRight : cMatch.m_nScoreLeft;
      return Math.max(0, nAgainst - nFor);
   }

   // [mod] vitesse de remplissage de la jauge de l'équipe t (1 = normale)
   static float CatchUpMul(Team t) {
      Load();
      if (m_fCatchUpPerGoal <= 0.0F) {
         return 1.0F;
      }

      return Math.min(m_fCatchUpMax, 1.0F + m_fCatchUpPerGoal * Deficit(t));
   }

   // [mod] rattrapage sur la possession : Team.Update donne une lettre toutes les m_fSuperUpgradeTime secondes de
   // possession ; l'équipe menée qui a le ballon cumule en plus fDelta x (mul - 1) et gagne une lettre de plus à
   // chaque m_fSuperUpgradeTime. Mêmes exclusions qu'AddAction.
   static void UpdateCatchUp(float fDelta) {
      AIMatch cMatch = SoccerModule.m_cMatch;
      if (m_fCatchUpPerGoal <= 0.0F || ModNet.IsClient() || cMatch == null || !AIMatch.IsMatchState(0) || cMatch.m_bGoldGoal) {
         return;
      }

      for (int s = 0; s < 2; s++) {
         Team t = cMatch.GetTeam(s);
         if (!(t instanceof AITeam)) {
            continue;
         }

         AITeam at = (AITeam)t;
         float fMul = CatchUpMul(t);
         if (fMul <= 1.0F || t.m_cPrevBallPossesor != t || t.m_bSuperDoing || t.m_nSuperFillCount >= 5 || t.m_cStrategy == null) {
            continue;
         }

         float fFill = t.m_cStrategy.m_fSuperUpgradeTime;
         if (fFill <= 0.0F) {
            continue;
         }

         at.m_fModCatchUpTime += fDelta * (fMul - 1.0F);
         if (at.m_fModCatchUpTime >= fFill) {
            at.m_fModCatchUpTime -= fFill;
            Log("lettre super de rattrapage : " + t.m_sName + " (x" + fMul + ", " + Deficit(t) + " but(s) de retard)");
            t.IncreaseSuperFillCount();
         }
      }
   }

   // [mod] AIMatch.OnGoal : l'équipe qui reçoit la lettre d'un but (GoalLetter), null = personne
   static Team GoalLetterTeam(Match cMatch, int nScorer) {
      Load();
      switch (m_nGoalLetter) {
         case _GOAL_LETTER_SCORER:
            return cMatch.GetTeam(nScorer);
         case _GOAL_LETTER_CONCEDER:
            return cMatch.GetTeam(1 - nScorer);
         default:
            return null;
      }
   }

   // AIMatch.ChangeTeamSides, après l'échange : les équipes gardent leur jauge, le HUD (un par côté) suit
   static void OnSidesSwapped(Match cMatch) {
      PetSoccerHud cHud = SoccerModule.GetHud();
      // en ligne, le client reçoit les jauges de l'hôte juste après l'échange : ses valeurs locales (vides) ne
      // doivent pas s'afficher entre-temps
      if (cHud == null || ModNet.IsClient()) {
         return;
      }

      for (int s = 0; s < 2; s++) {
         Team t = cMatch.GetTeam(s);
         if (t != null) {
            // l'icône du super complet reste affichée sous 5 lettres (HudSuper.LightLetters ne la cache pas)
            cHud.FlashSuperIconEnd(s);
            cHud.LightSuperLetters(t.m_nSuperFillCount, t.m_nSuperType, s);
         }
      }
   }

   // tirage des supers : générateur à part, graine selon l'heure. Celui du jeu (GameObject.RandomFloat) part toujours
   // de la graine 0, donc les mêmes supers revenaient d'un lancement à l'autre.
   static java.util.Random m_cRandom = new java.util.Random(System.currentTimeMillis());

   // remplace Team.GetRandomSuper (AITeam) : tirage pondéré par SuperChance, sans le super que l'autre équipe a en ce
   // moment (les deux équipes avaient souvent le même), sauf s'il est le seul possible
   static int GetRandomSuper(Team cTeam) {
      Load();
      int nOther = -1;
      if (cTeam != null && SoccerModule.m_cMatch != null) {
         Team cOther = SoccerModule.m_cMatch.GetTeam(1 - (cTeam.m_nSide & 1));
         if (cOther != null && cOther != cTeam) {
            nOther = cOther.m_nSuperType;
         }
      }

      float fTotal = 0.0F;

      for (int i = 0; i < _TYPES; i++) {
         if (i != nOther) {
            fTotal += m_fChance[i];
         }
      }

      if (fTotal <= 0.0F) {
         nOther = -1;

         for (int i = 0; i < _TYPES; i++) {
            fTotal += m_fChance[i];
         }
      }

      if (fTotal <= 0.0F) {
         return 0;
      }

      float fRnd = m_cRandom.nextFloat() * fTotal;
      int nType = -1;
      int nLast = 0; // dernier possible, si l'arrondi fait dépasser la somme

      for (int i = 0; i < _TYPES; i++) {
         if (i != nOther && m_fChance[i] > 0.0F) {
            nLast = i;
            fRnd -= m_fChance[i];
            if (fRnd < 0.0F) {
               nType = i;
               break;
            }
         }
      }

      if (nType < 0) {
         nType = nLast;
      }

      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("[mod] super tire pour " + (cTeam != null ? cTeam.m_sName : "?") + " : type " + nType + " (autre equipe : " + nOther + ")\n");
      }

      return nType;
   }

   static boolean IsModSuper(int nType) {
      return nType >= _FREEZE && nType < _TYPES;
   }

   // ballon fou et tir double se déclenchent ballon au pied, comme le super tir
   static boolean NeedsBall(int nType) {
      return nType == _CRAZY || nType == _DOUBLE;
   }

   // AIFootBaller.UseSuper : false si le super ne peut pas partir maintenant (il reste disponible)
   static boolean Use(AIFootBaller fb, int nType) {
      Load();
      if (fb.m_cTeam == null || SoccerModule.m_cMatch == null || GameField.m_cBall == null) {
         return false;
      }

      if (NeedsBall(nType) && !fb.HasBall()) {
         return false;
      }

      switch (nType) {
         case _FREEZE:
            StartFreeze(fb);
            break;
         case _CRAZY:
            StartCrazy(fb);
            break;
         case _MAGNET:
            StartMagnet(fb);
            break;
         case _DOUBLE:
            StartDouble(fb);
            break;
         default:
            return false;
      }

      Log("super " + nType + " lance par " + fb.m_cTeam.m_sName + " (cote " + fb.m_cTeam.m_nSide + ")");
      // le commentateur ne parle d'un super que pendant m_bSuperDoing (AIMatch) ; rendu à la fin de l'effet
      fb.m_cTeam.SetSuperDoing();
      SoccerModule.GetHud().FlashSuperIcon(fb.m_cTeam.m_nSide);
      return true;
   }

   static float Now() {
      return SoccerModule.m_Mod.m_Clock.GetMatchTimeF();
   }

   // traces dans crash.log, en mode debug seulement
   static void Log(String s) {
      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("[mod] supers : " + s + "\n");
      }
   }

   static Team GetTeam(int nSide) {
      return SoccerModule.m_cMatch.m_teams[nSide & 1];
   }

   // fin d'un effet : la jauge peut se remplir de nouveau, l'icône arrête de clignoter
   static void EndEffect(Team t) {
      if (t != null) {
         Log("fin d'effet pour " + t.m_sName);
         t.ResetSuperDoing();
         SoccerModule.GetHud().FlashSuperIconEnd(t.m_nSide);
      }
   }

   // appelé à chaque AITeam.Update ; le travail n'est fait qu'une fois par image (côté 0)
   static void Update(Team t) {
      if (t.m_nSide != 0 || SoccerModule.m_cMatch == null || GameField.m_cBall == null) {
         return;
      }

      float fNow = Now();
      float fDelta = m_fPrevTime < 0.0F || fNow < m_fPrevTime ? 0.0F : fNow - m_fPrevTime;
      m_fPrevTime = fNow;

      // le chrono s'arrête quand le jeu est interrompu (sortie, corner, but) : gel et aimant finissent là,
      // sinon l'équipe gelée resterait bloquée pour aller chercher le ballon
      boolean bStopped = !AIMatch.IsMatchState(0);

      for (int s = 0; s < 2; s++) {
         if (m_fFreezeEnd[s] >= 0.0F && (bStopped || fNow >= m_fFreezeEnd[s])) {
            EndFreeze(s);
         }

         if (m_fMagnetEnd[s] >= 0.0F && (bStopped || fNow >= m_fMagnetEnd[s])) {
            EndMagnet(s);
         }
      }

      Ball b = GameField.m_cBall;
      if (fDelta <= 0.0F || b.IsReplaying()) {
         return;
      }

      UpdateCatchUp(fDelta);
      UpdateCrazy(b, fDelta);
      UpdateMagnet(b, fDelta);
      UpdateDecoy(b, fDelta);
   }

   // AIMatch.ClearAI(true) : coup d'envoi, but, mi-temps
   static void Reset() {
      for (int s = 0; s < 2; s++) {
         if (m_fFreezeEnd[s] >= 0.0F) {
            EndFreeze(s);
         }

         if (m_fMagnetEnd[s] >= 0.0F) {
            EndMagnet(s);
         }
      }

      EndCrazy();
      EndDouble(false);
      m_fPrevTime = -1.0F;
   }

   // Ball.OnDestroy : fin du match, les objets du module disparaissent avec lui
   static void OnBallDestroy() {
      m_cDecoy = null;
      m_cDecoyBall = null;
      m_cCrazyTeam = null;
      m_cDoubleTeam = null;
      m_nCrazySound = -1;
      m_nMagnetSound = -1;
      m_nDecoySound = -1;
      m_fFreezeEnd[0] = m_fFreezeEnd[1] = -1.0F;
      m_fMagnetEnd[0] = m_fMagnetEnd[1] = -1.0F;
      m_fPrevTime = -1.0F;
   }

   // Ball.OnCollisionFullInfo : le ballon fou s'arrête de zigzaguer au premier contact
   static void OnBallHit() {
      if (m_cCrazyTeam != null) {
         EndCrazy();
      }
   }

   // ---------------------------------------------------------------- gel

   static void StartFreeze(AIFootBaller fb) {
      int nOpp = 1 - (fb.m_cTeam.m_nSide & 1);
      boolean bWasFrozen = m_fFreezeEnd[nOpp] >= 0.0F;
      m_fFreezeEnd[nOpp] = Now() + m_fFreezeTime;
      fb.AttachOneShotSound(_FREEZE_SOUND);
      ModNet.HostSound(ModNet._SND_ONESHOT, fb, _FREEZE_SOUND); // [mod] en ligne
      if (!bWasFrozen) {
         SetFrozenLook(GetTeam(nOpp), true);
      }
   }

   static void EndFreeze(int nSide) {
      m_fFreezeEnd[nSide] = -1.0F;
      SetFrozenLook(GetTeam(nSide), false);
      EndEffect(GetTeam(1 - nSide));
   }

   // teinte bleue et vitesse recalculée (AIFootBaller.SetLocalSpeedStat lit GetSpeedMul)
   static void SetFrozenLook(Team t, boolean bFrozen) {
      for (int i = 0; i < t.GetFootBallerCount(); i++) {
         AIFootBaller fb = (AIFootBaller)t.GetFootBaller(i);
         if (fb == null || fb.IsGoalKeeper() && !m_bFreezeKeeper) {
            continue;
         }

         fb.SetMeshColor(bFrozen ? m_vFreezeColor : new Vector(1.0F, 1.0F, 1.0F));
         // le gardien garde sa vitesse en cours (plongeon) : il ralentit à son prochain mouvement
         if (!fb.IsGoalKeeper()) {
            fb.UpdateSpeed();
            Log("  " + fb.m_sIndividualName + " vitesse " + fb.GetLocalSpeedVectorVolatile().Len());
         }
      }

      Log((bFrozen ? "gel de " : "degel de ") + t.m_sName);
   }

   // multiplicateur de vitesse d'un joueur gelé (1 sinon)
   static float GetSpeedMul(AIFootBaller fb) {
      if (fb.m_cTeam == null || m_fFreezeEnd[fb.m_cTeam.m_nSide & 1] < 0.0F) {
         return 1.0F;
      }

      return fb.IsGoalKeeper() && !m_bFreezeKeeper ? 1.0F : m_fFreezeSpeed;
   }

   // joueur de champ gelé : il ne feinte plus (Strategy.CalcFeint) et perd le ballon au tacle (ActionTakeBall)
   static boolean IsFrozen(AIFootBaller fb) {
      return fb != null && !fb.IsGoalKeeper() && fb.m_cTeam != null && m_fFreezeEnd[fb.m_cTeam.m_nSide & 1] >= 0.0F;
   }

   // ---------------------------------------------------------------- aimant

   static void StartMagnet(AIFootBaller fb) {
      int nSide = fb.m_cTeam.m_nSide & 1;
      m_fMagnetEnd[nSide] = Now() + m_fMagnetTime;
      if (m_nMagnetSound == -1) {
         m_nMagnetSound = GameField.m_cBall.AttachSound(_MAGNET_SOUND);
      }
   }

   static void EndMagnet(int nSide) {
      m_fMagnetEnd[nSide] = -1.0F;
      if (m_nMagnetSound != -1 && m_fMagnetEnd[1 - nSide] < 0.0F) {
         GameField.m_cBall.DetachSound(m_nMagnetSound);
         m_nMagnetSound = -1;
      }

      EndEffect(GetTeam(nSide));
   }

   // un ballon libre que l'équipe n'a pas touché en dernier est attiré vers son joueur le plus proche
   static void UpdateMagnet(Ball b, float fDelta) {
      if (b.HasOwner() || b.IsSuperShot() || !AIMatch.IsMatchState(0)) {
         return;
      }

      for (int s = 0; s < 2; s++) {
         Team t = GetTeam(s);
         if (m_fMagnetEnd[s] < 0.0F || b.IsPrevOwner(t)) {
            continue;
         }

         Vector vBall = b.GetPosition();
         FootBaller cTarget = null;
         float fBest = m_fMagnetRadius;

         for (int i = 0; i < t.GetFootBallerCount(); i++) {
            FootBaller fb = t.GetFootBaller(i);
            if (fb != null) {
               Vector vPos = fb.GetPosition();
               float fDX = vPos.fX - vBall.fX;
               float fDZ = vPos.fZ - vBall.fZ;
               float fDist = (float)Math.sqrt(fDX * fDX + fDZ * fDZ);
               if (fDist < fBest) {
                  fBest = fDist;
                  cTarget = fb;
               }
            }
         }

         if (cTarget == null || fBest < 1.0F) {
            continue;
         }

         Vector vTo = cTarget.GetPosition();
         vTo.Sub(vBall);
         vTo.fY = 0.0F;
         vTo.Mul(1.0F / fBest);
         Vector vSpeed = new Vector(b.GetSpeedVectorVolatile());
         vSpeed.fY = 0.0F;
         float fSpeed = Math.max(vSpeed.Len(), m_fMagnetMinSpeed);
         float k = Math.min(1.0F, m_fMagnetPull * fDelta);
         vSpeed.fX += (vTo.fX * fSpeed - vSpeed.fX) * k;
         vSpeed.fZ += (vTo.fZ * fSpeed - vSpeed.fZ) * k;
         b.SetSpeed(vSpeed);
         return;
      }
   }

   // ---------------------------------------------------------------- tirs (ballon fou, tir double)

   static Vector GetOppGatePos(AIFootBaller fb) {
      return fb.m_nTeam == 0 ? GameField.cGateRight.GetPosition() : GameField.cGateLeft.GetPosition();
   }

   // nombre au hasard dans [-fMax, fMax]
   static float RandomSigned(float fMax) {
      return GameObject.RandomFloat(2.0F * fMax) - fMax;
   }

   // tir réel vers vDest ; le gardien adverse plonge vers vGKDest (null = vers le vrai tir)
   static void Shoot(AIFootBaller fb, Vector vDest, Vector vGKDest) {
      Ball b = GameField.m_cBall;
      AIFootBaller.Shot cShot = new AIFootBaller.Shot();
      cShot.m_vStartPos = fb.GetPosition();
      cShot.m_vDest = new Vector(vDest);
      cShot.m_vDir = vDest.Sub2(cShot.m_vStartPos);
      // Ball.KickTo tire du m_cShot du tireur la description du tir donnée au gardien
      fb.m_cShot = cShot;
      GameField.m_fDistToDest = vDest.Distance(fb.GetPositionVectorVolatile());
      AIFootBaller.Shot cDefence = b.KickTo(vDest, true, false);
      fb.m_cShot = null;
      if (cDefence != null && vGKDest != null) {
         cDefence.m_vDest.fX = vGKDest.fX;
         cDefence.m_vDest.fZ = vGKDest.fZ;
      }

      fb.m_cDefence = cDefence;
      fb.PlayKickFX();
      fb.GoalKeeperNotice();
      SoccerModule.m_cMatch.UnblockControlForFocus(vDest);
   }

   // direction horizontale normalisée de vFrom vers vTo
   static Vector FlatDir(Vector vFrom, Vector vTo) {
      Vector vDir = new Vector(vTo.fX - vFrom.fX, 0.0F, vTo.fZ - vFrom.fZ);
      vDir.Normalize();
      return vDir;
   }

   static void StartCrazy(AIFootBaller fb) {
      Ball b = GameField.m_cBall;
      float fWidth = GameField.m_fGateWidth;
      Vector vDest = new Vector(GetOppGatePos(fb));
      vDest.fX += RandomSigned(fWidth * m_fCrazyAim);
      Vector vStart = b.GetPosition();
      Shoot(fb, vDest, null);
      m_vCrazyStart = new Vector(vStart);
      m_vCrazyDir = FlatDir(vStart, vDest);
      m_vCrazySide = new Vector(-m_vCrazyDir.fZ, 0.0F, m_vCrazyDir.fX);
      m_fCrazyDist = Math.max(1.0F, (vDest.fX - vStart.fX) * m_vCrazyDir.fX + (vDest.fZ - vStart.fZ) * m_vCrazyDir.fZ);
      m_fCrazySign = GameObject.RandomInt(2) == 0 ? -1.0F : 1.0F;
      m_fCrazyZigLeft = 0.0F;
      NextCrazyZig(0.5F);
      m_fCrazyT = 0.0F;
      m_fCrazySmoke = 0.0F;
      m_cCrazyTeam = fb.m_cTeam;
      if (m_nCrazySound == -1) {
         m_nCrazySound = b.AttachSound(_CRAZY_SOUND);
      }

      Log("ballon fou : largeur des cages " + fWidth + ", distance " + m_fCrazyDist);
   }

   // segment suivant du zigzag : virage sec vers l'autre côté, amplitude et durée un peu au hasard
   static void NextCrazyZig(float fScale) {
      m_fCrazySign = -m_fCrazySign;
      m_fCrazyZigAmp = GameField.m_fGateWidth * m_fCrazyAmplitude * (0.6F + GameObject.RandomFloat(0.4F));
      m_fCrazyZigLeft += fScale * (0.5F / m_fCrazyFrequency) * (0.7F + GameObject.RandomFloat(0.6F));
   }

   static void EndCrazy() {
      if (m_cCrazyTeam == null) {
         return;
      }

      Team t = m_cCrazyTeam;
      m_cCrazyTeam = null;
      if (m_nCrazySound != -1 && GameField.m_cBall != null) {
         GameField.m_cBall.DetachSound(m_nCrazySound);
      }

      m_nCrazySound = -1;
      EndEffect(t);
   }

   // zigzag : lignes droites d'un bord à l'autre de la ligne de tir, virages secs ;
   // l'écart se resserre sur la fin pour que le tir reste cadré
   static void UpdateCrazy(Ball b, float fDelta) {
      if (m_cCrazyTeam == null) {
         return;
      }

      m_fCrazyT += fDelta;
      if (b.HasOwner() || !AIMatch.IsMatchState(0) || m_fCrazyT > m_fCrazyTime) {
         EndCrazy();
         return;
      }

      Vector vSpeed = b.GetSpeedVectorVolatile();
      float fForward = vSpeed.fX * m_vCrazyDir.fX + vSpeed.fZ * m_vCrazyDir.fZ;
      if (fForward < 300.0F) {
         EndCrazy();
         return;
      }

      m_fCrazyZigLeft -= fDelta;
      if (m_fCrazyZigLeft <= 0.0F) {
         NextCrazyZig(1.0F);
      }

      Vector vPos = b.GetPosition();
      float fDX = vPos.fX - m_vCrazyStart.fX;
      float fDZ = vPos.fZ - m_vCrazyStart.fZ;
      float fOff = fDX * m_vCrazySide.fX + fDZ * m_vCrazySide.fZ;
      float fLeft = m_fCrazyDist - (fDX * m_vCrazyDir.fX + fDZ * m_vCrazyDir.fZ);
      float fEnv = Math.max(0.0F, Math.min(1.0F, fLeft / (m_fCrazyDist * _CRAZY_FADE)));
      float fTarget = m_fCrazySign * m_fCrazyZigAmp * fEnv;
      float fSide = (fTarget - fOff) / Math.max(m_fCrazyZigLeft, 0.05F);
      fSide = Math.max(-fForward, Math.min(fForward, fSide));
      b.SetSpeed(new Vector(m_vCrazyDir.fX * fForward + m_vCrazySide.fX * fSide, 0.0F, m_vCrazyDir.fZ * fForward + m_vCrazySide.fZ * fSide));
      m_fCrazySmoke -= fDelta;
      if (m_fCrazySmoke <= 0.0F) {
         m_fCrazySmoke = _SMOKE_PERIOD;
         Vector vNormal = new Vector(b.GetSpeedVectorVolatile());
         if (vNormal.Len() == 0.0F) {
            vNormal = Vector.Forward;
         }

         b.ExecuteFX("FXSmoke", b.GetPosition(), vNormal);
      }
   }

   static void StartDouble(AIFootBaller fb) {
      Ball b = GameField.m_cBall;
      float fWidth = GameField.m_fGateWidth;
      Vector vGate = GetOppGatePos(fb);
      float fSign = GameObject.RandomInt(2) == 0 ? -1.0F : 1.0F;
      Vector vReal = new Vector(vGate);
      vReal.fX += fSign * fWidth * m_fDoubleSpread;
      Vector vFake = new Vector(vGate);
      vFake.fX -= fSign * fWidth * m_fDoubleSpread;
      boolean bFooled = GameObject.RandomFloat(100.0F) < m_fDoubleFoolChance;
      Vector vStart = b.GetPosition();
      Shoot(fb, vReal, bFooled ? vFake : null);
      Vector vSpeed = b.GetSpeedVectorVolatile();
      float fSpeed = (float)Math.sqrt(vSpeed.fX * vSpeed.fX + vSpeed.fZ * vSpeed.fZ);
      if (!ShowDecoy(b, vStart)) {
         m_cDoubleTeam = fb.m_cTeam;
         EndDouble(false);
         return;
      }

      m_vDecoyPos = vStart;
      m_vDecoyVel = FlatDir(vStart, vFake);
      m_vDecoyVel.Mul(fSpeed);
      m_fDecoyT = 0.0F;
      m_cDoubleTeam = fb.m_cTeam;
      if (m_nDecoySound == -1) {
         m_nDecoySound = m_cDecoy.AttachSound(_DOUBLE_SOUND);
      }

      Log("tir double : gardien trompe " + bFooled + ", vitesse " + fSpeed + ", echelle du leurre " + m_fDecoyScale);
   }

   // crée le leurre au premier tir double du match, puis le place sur le ballon
   static boolean ShowDecoy(Ball b, Vector vPos) {
      if (m_cDecoy == null || m_cDecoyBall != b) {
         SoccerModule cModule = SoccerGame.GetSoccerModule();
         m_cDecoy = (MeshObject)cModule.CreateObject("MeshObject");
         if (m_cDecoy == null) {
            GameObject.CrashLog("[mod] tir double : creation du leurre impossible\n");
            return false;
         }

         cModule.AddControlObject(m_cDecoy);
         m_cDecoy.MeshName = _DECOY_MESH;
         m_cDecoy.LoadMesh();
         m_cDecoy.ClearFlag(1);
         m_cDecoyBall = b;
         // même taille que le ballon du terrain (fBallRadius vient de son mesh)
         Vector vMin = new Vector();
         Vector vMax = new Vector();
         m_cDecoy.GetMeshExtentsInWorld(vMin, vMax);
         vMax.Sub(vMin);
         m_fDecoyScale = vMax.fY > 0.0F ? 2.0F * b.fBallRadius / vMax.fY : 1.0F;
         m_cDecoy.SetScale(new Vector(m_fDecoyScale, m_fDecoyScale, m_fDecoyScale));
         m_cDecoy.SetBlobMapSize(vMax.fX * m_fDecoyScale * 0.6F);
      }

      m_cDecoy.SetPosition(vPos);
      m_cDecoy.EnableRendering(true);
      return true;
   }

   static void EndDouble(boolean bPuff) {
      if (m_cDoubleTeam == null) {
         return;
      }

      Team t = m_cDoubleTeam;
      m_cDoubleTeam = null;
      if (m_cDecoy != null) {
         if (bPuff && m_vDecoyPos != null) {
            m_cDecoy.ExecuteFX("FXSmoke", m_vDecoyPos, Vector.Up);
            ModNet.OnFX(ModNet._OBJ_DECOY, "FXSmoke", m_vDecoyPos, Vector.Up); // [mod] en ligne
         }

         if (m_nDecoySound != -1) {
            m_cDecoy.DetachSound(m_nDecoySound);
         }

         m_cDecoy.EnableRendering(false);
      }

      m_nDecoySound = -1;
      EndEffect(t);
   }

   // le leurre file en ligne droite au ras du sol et disparaît en franchissant la ligne de but
   static void UpdateDecoy(Ball b, float fDelta) {
      if (m_cDoubleTeam == null || m_cDecoy == null) {
         return;
      }

      m_fDecoyT += fDelta;
      float fStep = m_vDecoyVel.Len() * fDelta;
      m_vDecoyPos.fX += m_vDecoyVel.fX * fDelta;
      m_vDecoyPos.fZ += m_vDecoyVel.fZ * fDelta;
      m_vDecoyPos.fY = b.TerrainHeight(m_vDecoyPos.fX, m_vDecoyPos.fZ) + b.fBallRadius;
      m_cDecoy.SetPosition(m_vDecoyPos);
      if (fStep > 0.0F && b.fBallRadius > 0.0F) {
         Vector vDir = new Vector(m_vDecoyVel);
         vDir.Normalize();
         Vector vAxis = m_cDecoy.VectorWorldToLocal(Vector.Cross(vDir, Vector.Up));
         m_cDecoy.RotateLocal(vAxis, -fStep / (2.0F * b.fBallRadius * (float)Math.PI) * b.fRotationCoeff);
      }

      int nZone = GameField.GetZoneForPoint(m_vDecoyPos, 0);
      if (nZone == 3 || nZone == 4 || m_fDecoyT > m_fDoubleTime || !AIMatch.IsMatchState(0)) {
         EndDouble(true);
      }
   }
}
