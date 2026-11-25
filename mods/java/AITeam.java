public class AITeam extends Team {
   // [mod] actions comptées pour la jauge SUPER (ModSupers.AddAction) : tirs, récupérations, arrêts
   int[] m_nModSuperActions = new int[3];
   AISystem m_cAISystem = null;
   Order m_cOrder = null;
   boolean m_bInited = false;
   float m_fLastSituationCalcTime = 0.0F;
   boolean m_bIsToStabilization = false;
   boolean m_bThink = true;
   int m_nFBallerToCalculation = 0;
   float m_fLastControlTime = -1.0F;
   boolean m_bControlBlock = false;
   int m_bOldKeyState = 0;
   boolean m_bChangeControlToUserRequest = false;
   SuperWall m_cSuperWall = null;
   boolean m_bSuperWallToCall = false;
   boolean m_bSuperWallActive = false;
   AIFootBaller m_cHelpAttacker = null;
   AIFootBaller m_cHelpBallAttacker = null;
   AIFootBaller m_cSelDestTracker = null;
   // [mod] ModSwitch : fin du verrou après un changement manuel, joueur qu'on vient de quitter
   float m_fSwitchLockEnd = -1.0F;
   AIFootBaller m_cSwitchLeft = null;
   float m_fSwitchLeftTime = -1.0F;
   static int PROFILE_UpdateGameInTeam0 = 0;
   static int PROFILE_UpdateGameInTeam1 = 0;

   void DoSuperDefence() {
      this.m_bSuperWallToCall = false;
      this.SetSuperDoing();
      super.m_cGoalKeeper.ChangeDirectionLogic(super.m_cGoalKeeper.m_cStrategy.GetActionDirToDest(super.m_cGoalKeeper, GameField.cCenter.GetPosition(), false));
      super.m_cGoalKeeper.ChangeActivity("Bieganie");
      super.m_cGoalKeeper.CallMethodIn("SetSuperWallForTeam", 0.3F);
   }

   void SetActive(int nFBaller) {
      if (nFBaller < super.m_vFootBallers.size()) {
         AIFootBaller cAIFootBaller = (AIFootBaller)super.m_vFootBallers.elementAt(nFBaller);
         this.SetActive(cAIFootBaller);
      }
   }

   public void Update() {
      super.Update();
      ModOptions.UpdateTeam(this); // [mod] super imposé / supers infinis (menu Mods)
      ModSupers.Update(this); // [mod] gel, ballon fou, aimant, tir double
      if (this.m_bInited) {
         if (this.IsStabilization()) {
            this.UpdateStabilization();
         } else if (!SoccerModule.m_cMatch.m_bWaiting) {
            if (PROFILE_UpdateGameInTeam0 == 0) {
               PROFILE_UpdateGameInTeam0 = super.m_cModule.ProfileAdd("Soccer", "UpdateGame(Team0)");
               PROFILE_UpdateGameInTeam1 = super.m_cModule.ProfileAdd("Soccer", "UpdateGame(Team1)");
            }

            if (!GameField.m_cBall.m_bWasGoal) {
               if (super.m_nSide == 0) {
                  super.m_cModule.ProfileBegin(PROFILE_UpdateGameInTeam0);
               } else {
                  super.m_cModule.ProfileBegin(PROFILE_UpdateGameInTeam1);
               }

               this.m_cSelDestTracker = null;
               this.UpdateGame();
               if (super.m_nSide == 0) {
                  super.m_cModule.ProfileEnd(PROFILE_UpdateGameInTeam0);
               } else {
                  super.m_cModule.ProfileEnd(PROFILE_UpdateGameInTeam1);
               }
            }
         } else {
            if (AIMatch.IsMatchState(7) || AIMatch.IsMatchState(5) || AIMatch.IsMatchState(6) || AIMatch.IsMatchState(4)) {
               if (SoccerModule.m_cMatch.cSpecialBallSel.IsActive()
                  && !AIMatch.IsMatchState(7)
                  && SoccerModule.m_cMatch.m_cSpecialFB != null
                  && SoccerModule.m_cMatch.m_cSpecialFB.m_cTeam == this) {
                  AIFootBaller cSelDestTracker = this.FindNearestExclude(SoccerModule.m_cMatch.m_cSpecialFB);
                  if (cSelDestTracker != this.m_cSelDestTracker) {
                     this.SetSelDestTracker(cSelDestTracker);
                  }

                  if (this.m_cSelDestTracker != null) {
                     Goal cGoal = this.m_cSelDestTracker.GetGoal();
                     if ((
                           cGoal == null
                              || cGoal.m_nId != 22
                              || cGoal.m_nId == 22 && SoccerModule.m_cMatch.cSpecialBallSel.GetDistanceTo(((GoalGoToPos)cGoal).m_vDestPos) > 300.0F
                        )
                        && SoccerModule.m_cMatch.cSpecialBallSel.GetDistanceTo(this.m_cSelDestTracker.GetPositionVectorVolatile()) > 300.0F) {
                        this.m_cAISystem.RequestEvent(this.m_cSelDestTracker, 18, SoccerModule.m_cMatch.cSpecialBallSel.GetPosition());
                     }
                  }
               }

               int nSize = super.m_vFootBallers.size();

               for (int i = 0; i < nSize; i++) {
                  AIFootBaller cFBaller = (AIFootBaller)super.m_vFootBallers.elementAt(i);
                  if (!cFBaller.IsGoalKeeper()
                     && !cFBaller.HasSpecialGoal()
                     && SoccerModule.m_cMatch.m_cSpecialFB != cFBaller
                     && (SoccerModule.m_cMatch.m_cSpecialDestFB != cFBaller || !AIMatch.IsMatchState(7))) {
                     this.UpdateValidPos(cFBaller);
                     if (!cFBaller.IsReferee() || cFBaller.GetDistanceTo(cFBaller.m_vValidPos) > Strategy.GetActiveDistance(cFBaller) + 10.0F) {
                        cFBaller.Update();
                     }
                  }
               }
            }
         }
      }
   }

   void UpdateOrderOnStart(int nTeamSide) {
      if (nTeamSide == 0) {
         this.m_cOrder.SetCenterZ(GameField.cOrderCenterLeft.GetPosition().fZ);
      }

      if (nTeamSide == 1) {
         this.m_cOrder.SetCenterZ(GameField.cOrderCenterRight.GetPosition().fZ);
      }
   }

   void Restart() {
      int nSize = super.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         AIFootBaller cFBaller = (AIFootBaller)super.m_vFootBallers.elementAt(i);
         if (cFBaller.m_cDecContext.m_cGoal != null && !cFBaller.m_cDecContext.m_cGoal.m_bReached) {
            cFBaller.m_cDecContext.m_cGoal.m_cAction.Exit();
            cFBaller.m_cDecContext.m_cGoal.m_bReached = true;
         }
      }
   }

   void SetParams(AISystem cAI, SoccerModule cModule) {
      this.m_cAISystem = cAI;
      super.m_cStrategy = cAI.m_cStrategy;
      super.SetParams(cModule);
      this.InitSuperWall();
   }

   void UpdateOrder() {
      this.m_cOrder.SetCenterZ(GameField.GetDestPos().fZ);
   }

   void InitSuperWall() {
      this.m_cSuperWall = (SuperWall)super.m_cModule.CreateObject("SuperWall");
      super.m_cModule.AddControlObject(this.m_cSuperWall);
   }

   boolean CanChangeSituation(AIFootBaller cFBaller) {
      return cFBaller.HasBall() && !cFBaller.IsUserControled() ? true : !cFBaller.IsActive() && !cFBaller.IsGoalKeeper() && cFBaller.GetZone() == 2;
   }

   void BlockControl(boolean bBlocked) {
      this.m_bControlBlock = bBlocked;
      if (!bBlocked) {
         this.m_bChangeControlToUserRequest = false;
      }
   }

   void ShowContexts() {
      GameObject.LogLine("TEAM " + super.m_sName + "   ======================");
      int nSize = super.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         AIFootBaller cAIFootBaller = (AIFootBaller)super.m_vFootBallers.elementAt(i);
         cAIFootBaller.ShowContext();
      }
   }

   void UpdateValidPos(AIFootBaller cFBaller) {
      Vector vValidPos = null;
      if (cFBaller.IsGoalKeeper()) {
         if (!cFBaller.IsDefence()) {
            vValidPos = null;
         }
      } else {
         vValidPos = new Vector(this.m_cOrder.m_vCenter);
         vValidPos.Add(cFBaller.m_vValidPosOff);
         if (!SoccerModule.m_cMatch.IsWaiting()) {
            vValidPos = this.Deviation(cFBaller, vValidPos);
         }

         if ((this.IsPossesor() || this.IsPrevOwner())
            && cFBaller.m_nType == 0
            && GameField.GetZoneForPoint(GameField.m_cBall.GetPositionVectorVolatile(), super.m_nSide) != 0
            && AIMatch.IsMatchState(0)) {
            if (super.m_nSide == 0) {
               vValidPos.fZ += 500.0F;
            } else {
               vValidPos.fZ -= 500.0F;
            }
         }

         vValidPos = super.m_cStrategy.TuneValidScope(cFBaller, vValidPos);
      }

      if (!cFBaller.IsGoalKeeper() || !cFBaller.IsDefence()) {
         cFBaller.SetValidPos(super.m_cStrategy.CalculateValidPosition(cFBaller, vValidPos));
      }

      ModTactics.UpdateValidPos(this, cFBaller); // [mod] placement selon le rôle + marquage
   }

   void SetThink() {
      this.m_bThink = true;
   }

   void ResetHelpAttacker() {
      this.m_cHelpAttacker = null;
   }

   public String toString() {
      return super.m_sName;
   }

   boolean IsPrevOwner() {
      return GameField.m_cBall.m_cPrevOwner == null || GameField.m_cBall.m_cPrevOwner.m_cTeam == this;
   }

   void SetHelpBallAttacker(AIFootBaller cHelpBallAttacker) {
      if (this.m_cHelpBallAttacker != null
         && this.m_cHelpBallAttacker != cHelpBallAttacker
         && this.m_cHelpBallAttacker.HasGoal(3)
         && this.m_cHelpBallAttacker.GoalUnreached()) {
         this.m_cHelpBallAttacker.GetGoal().m_bReached = true;
         this.m_cHelpBallAttacker.GetGoal().ActionAfterReached(this.m_cHelpBallAttacker);
      }

      this.m_cHelpBallAttacker = cHelpBallAttacker;
   }

   AIFootBaller FindNearest() {
      return this.FindNearestExclude(null);
   }

   void FixOrder() {
      this.UpdateOrderOnStart(super.m_nSide);
      int nSize = super.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         FootBaller cFBaller = (FootBaller)super.m_vFootBallers.elementAt(i);
         ((AIFootBaller)cFBaller).FixValidPos();
      }

      this.m_bInited = true;
   }

   void SetSuperWall() {
      this.m_bSuperWallActive = true;
      Vector vWallPos = null;
      Vector vWallDir = null;
      if (super.m_nSide == 0) {
         vWallPos = GameField.cGateLeft.GetPosition();
         vWallDir = GameField.cGateLeft.GetForwardVector();
      } else {
         vWallPos = GameField.cGateRight.GetPosition();
         vWallDir = GameField.cGateRight.GetForwardVector();
      }

      this.m_cSuperWall.SetDestPosition(vWallPos);
      vWallPos.fY -= 400.0F;
      this.m_cSuperWall.SetPosition(vWallPos);
      this.m_cSuperWall.SetDir(vWallDir);
      this.m_cSuperWall.Show();
      AIFootBaller cGK = (AIFootBaller)super.m_cGoalKeeper;
      cGK.SetSpecialGoal();
      vWallPos.fY += 400.0F;
      this.m_cAISystem.RequestEvent(cGK, 18, super.m_cStrategy.GetGKPosWithSuperWall(vWallPos, cGK));
   }

   boolean IsSuperWall() {
      return this.m_bSuperWallActive;
   }

   void WrtiteStateAsCrashLog() {
      GameObject.CrashLog(
         this
            + " ----------------------"
            + "\n"
            + " - Side "
            + super.m_nSide
            + " \n"
            + " - m_bIsToStabilization "
            + this.m_bIsToStabilization
            + " \n"
            + " - m_cHelpAttacker "
            + this.m_cHelpAttacker
            + " \n"
            + " - m_cSelDestTracker "
            + this.m_cSelDestTracker
            + " \n"
            + " - m_cActiveFootBaller "
            + super.m_cActiveFootBaller
            + " \n"
            + " - m_cFocusFBaller "
            + super.m_cFocusFBaller
            + " \n"
            + " - m_bUserControled "
            + super.m_bUserControled
            + " \n"
            + " - m_bSuperAvailable "
            + super.m_bSuperAvailable
            + " \n"
            + " - m_bSuperDoing "
            + super.m_bSuperDoing
            + " \n"
            + " - m_bSuperFeint "
            + super.m_bSuperFeint
            + " \n"
            + " - m_bSuperSpeed "
            + super.m_bSuperSpeed
            + " \n"
            + " - m_cSuperFB "
            + super.m_cSuperFB
            + " \n"
            + " - m_cPrevBallPossesor "
            + super.m_cPrevBallPossesor
            + " \n"
      );
   }

   void UpdateStabilization() {
      if (SoccerModule.m_cMatch.IsStabilizationNew()) {
         this.CheckStabilizationProcess();
      }
   }

   Vector Deviation(AIFootBaller cFBaller, Vector vPos) {
      Vector vNewPos = new Vector(vPos);
      vNewPos.fX = vNewPos.fX + cFBaller.m_fDevX;
      float fRealDevZ = cFBaller.m_fDevZ;
      if ((this.IsPossesor() || this.IsPrevOwner())
         && cFBaller.m_nType == 0
         && GameField.GetZoneForPoint(GameField.m_cBall.GetPositionVectorVolatile(), super.m_nSide) != 0
         && (super.m_nSide == 0 && fRealDevZ < 0.0F || super.m_nSide == 1 && fRealDevZ > 0.0F)) {
         fRealDevZ = -fRealDevZ;
      }

      vNewPos.fZ += fRealDevZ;
      int nZone = GameField.GetZoneForPoint(vNewPos, -1);
      return nZone != 3 && nZone != 4 && GameField.GetZoneForPointVertical(vNewPos) != 6 && GameField.GetZoneForPointVertical(vNewPos) != 5 ? vNewPos : vPos;
   }

   void ResetTeamFunction() {
      this.m_cHelpAttacker = null;
      this.m_cHelpBallAttacker = null;
      this.m_cSelDestTracker = null;
   }

   void TestLog() {
      int nSize = super.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         FootBaller cFootBaller = (FootBaller)super.m_vFootBallers.elementAt(i);
         cFootBaller.ChangeDirectionLogic(super.m_cStrategy.GetActionDirToDest((AIFootBaller)cFootBaller, this.m_cOrder.m_vCenter, false));
      }
   }

   void WrtiteStateAsLog() {
      GameObject.LogD(this + " ----------------------" + "\n");
      GameObject.LogD(" - Side " + super.m_nSide + " \n");
      GameObject.LogD(" - m_bIsToStabilization " + this.m_bIsToStabilization + " \n");
      GameObject.LogD(" - m_fLastControlTime " + this.m_fLastControlTime + " now: " + SoccerGame.GetSoccerModule().GetTime() + " \n");
      GameObject.LogD(" - m_bControlBlock " + this.m_bControlBlock + " \n");
      GameObject.LogD(" - m_bOldKeyState " + this.m_bOldKeyState + " \n");
      GameObject.LogD(" - m_bChangeControlToUserRequest " + this.m_bChangeControlToUserRequest + " \n");
      GameObject.LogD(" - m_bSuperWallActive " + this.m_bSuperWallActive + " \n");
      GameObject.LogD(" - m_cHelpAttacker " + this.m_cHelpAttacker + " \n");
      GameObject.LogD(" - m_cSelDestTracker " + this.m_cSelDestTracker + " \n");
      GameObject.LogD(" - m_cActiveFootBaller " + super.m_cActiveFootBaller + " \n");
      GameObject.LogD(" - m_cFocusFBaller " + super.m_cFocusFBaller + " \n");
      GameObject.LogD(" - m_bUserControled " + super.m_bUserControled + " \n");
      GameObject.LogD(" - m_bSuperAvailable " + super.m_bSuperAvailable + " \n");
      GameObject.LogD(" - m_bSuperDoing " + super.m_bSuperDoing + " \n");
      GameObject.LogD(" - m_bSuperFeint " + super.m_bSuperFeint + " \n");
      GameObject.LogD(" - m_bSuperSpeed " + super.m_bSuperSpeed + " \n");
      GameObject.LogD(" - m_cSuperFB " + super.m_cSuperFB + " \n");
      GameObject.LogD(" - m_cPrevBallPossesor " + super.m_cPrevBallPossesor + " \n");
   }

   boolean IsBlockControl() {
      return this.m_bControlBlock;
   }

   boolean CheckGoalKeeperAttack() {
      AIFootBaller cBallPossesor = GameField.m_cBall.m_cCurOwner;
      if (cBallPossesor != null && super.m_cGoalKeeper != null) {
         ControlObject vGate = null;
         if (super.m_cGoalKeeper.m_nTeam == 0) {
            vGate = GameField.cGateLeft;
         } else {
            vGate = GameField.cGateRight;
         }

         float fBallDistance = GameField.m_cBall.GetDistanceTo(super.m_cGoalKeeper);
         float fGateDistance = super.m_cGoalKeeper.GetDistanceTo(vGate);
         if (fBallDistance < super.m_cStrategy.m_fGKAttackScope * 0.7F
            && fGateDistance < super.m_cStrategy.m_fGKAttackScope
            && GameField.m_cBall.GetDistanceTo(vGate) < super.m_cStrategy.m_fGKAttackScope
            && super.m_cStrategy.GetFBDistance(cBallPossesor) == null) {
            AIFootBaller cGK = (AIFootBaller)super.m_cGoalKeeper;
            this.m_cAISystem.RequestEvent(cGK, 5, GameField.m_cBall.GetPosition());
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   void CheckHelpAttack() {
      if (GameField.m_cBall.m_cCurOwner == null || super.m_cActiveFootBaller != null || !GameField.m_cBall.m_cCurOwner.IsGoalKeeper()) {
         AIFootBaller cHelpAttacker = this.FindNearestExclude((AIFootBaller)super.m_cActiveFootBaller);
         if (cHelpAttacker == null || !(cHelpAttacker.GetDistanceTo(GameField.m_cBall) < super.m_cStrategy.m_fMinDistFBallers)) {
            if (cHelpAttacker != null && cHelpAttacker != this.m_cHelpAttacker) {
               this.SetHelpAttacker(cHelpAttacker);
            }

            if (cHelpAttacker != null && (cHelpAttacker.HasGoal(9) && !cHelpAttacker.GoalUnreached() || !cHelpAttacker.HasGoal(9)) && !cHelpAttacker.Busy()) {
               this.m_cAISystem.RequestEvent(cHelpAttacker, 5, GameField.m_cBall.GetPosition());
            }
         }
      }
   }

   boolean EveryOneSpecialGoalDone() {
      boolean bDone = true;
      int nSize = super.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         AIFootBaller cFBaller = (AIFootBaller)super.m_vFootBallers.elementAt(i);
         if (cFBaller.HasSpecialGoal() && !cFBaller.IsSpecialGoalDone()) {
            bDone = false;
         }
      }

      return bDone;
   }

   void ResetActiveSupers() {
      if (this.IsSuperWall()) {
         this.ResetSuperWall();
      }

      if (this.IsSuperSpeed()) {
         this.ResetSuperSpeed();
      }

      if (this.IsSuperFeint()) {
         this.ResetSuperFeint();
      }
   }

   void ClearThink() {
      this.m_bThink = false;
   }

   void PassAndShotChance(AIFootBaller cActive) {
      AIFootBaller cBestFB = (AIFootBaller)super.m_cStrategy.FindBestFootBaller(cActive, -1.0F);
      if (cBestFB != null) {
         float m_fSituationDiff = cBestFB.m_cDecContext.m_cSituation.GetValue() - cActive.m_cDecContext.m_cSituation.GetValue();
         if (m_fSituationDiff > super.m_cStrategy.m_fMaxSituationAnotherPlayerDiff) {
            float fForwardDist = super.m_cStrategy.GetForwardDistDiff(cActive, cBestFB);
            if (fForwardDist > super.m_cStrategy.m_fMinPassForwardDist) {
               SoccerGame.game.GetCommentator().CommentPassChance(cActive, cBestFB);
               this.m_cAISystem.RequestEvent(cActive, 8, new AIFootBaller.Pass(cBestFB, cBestFB.m_vPassPosition));
            }
         }
      }
   }

   void ResetRequestSuperDefence() {
      if (this.m_bSuperWallToCall) {
         this.m_bSuperWallToCall = false;
         SoccerModule.GetHud().FlashSuperIconEnd(super.m_nSide);
         this.ResetSuperDoing();
      }
   }

   AIFootBaller FindNearestExclude(AIFootBaller cFBallerExclude) {
      float fMinDist = GameField.GetDiagonalLength();
      int nMinFBaller = -1;
      Vector vDestPos = GameField.m_vDestPosition;
      int nSize = super.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         FootBaller cFBaller = (FootBaller)super.m_vFootBallers.elementAt(i);
         if (!((AIFootBaller)cFBaller).IsGoalKeeper() && cFBaller != cFBallerExclude && cFBaller != SoccerModule.m_cMatch.m_cActiveLossFB) {
            float fDistance = cFBaller.GetDistanceTo(vDestPos);
            if (fDistance < fMinDist) {
               fMinDist = fDistance;
               nMinFBaller = i;
            }
         }
      }

      return nMinFBaller != -1 ? (AIFootBaller)super.m_vFootBallers.elementAt(nMinFBaller) : null;
   }

   void ChangeActive() {
      // [mod] équipes humaines : choix selon la direction tenue, puis tour des plus proches (ModSwitch)
      if (super.m_bUserControled && AIMatch.IsMatchState(0)) {
         AIFootBaller cPick = ModSwitch.Choose(this);
         if (cPick != null) {
            this.SwitchTo(cPick);
            return;
         }
      }

      AIFootBaller cActive = (AIFootBaller)super.m_cActiveFootBaller;
      AIFootBaller cNearest;
      if (GameField.m_cBall.HasOwner()) {
         cNearest = this.FindAttacker();
      } else {
         cNearest = this.FindNearest();
      }

      AIFootBaller cOldFocusFB = null;
      if (cNearest != cActive) {
         if (!cNearest.Busy()) {
            cActive = (AIFootBaller)this.SetActive(cNearest);
            if (cActive != null) {
               cOldFocusFB = (AIFootBaller)super.m_cFocusFBaller;
               this.SetFocus(cActive, false);
            }
         }
      } else if (cActive != null && !cActive.HasFocus() && !cActive.Busy()) {
         cOldFocusFB = (AIFootBaller)super.m_cFocusFBaller;
         this.SetFocus(cActive, false);
      } else {
         cNearest = this.FindNearestExclude(cActive);
         if (!cNearest.Busy()) {
            cActive = (AIFootBaller)this.SetActive(cNearest);
            if (cActive != null) {
               cOldFocusFB = (AIFootBaller)super.m_cFocusFBaller;
               this.SetFocus(cActive, false);
            }
         }
      }

      if (cOldFocusFB != null && cOldFocusFB != super.m_cFocusFBaller && cOldFocusFB.HasEvent(1)) {
         cOldFocusFB.ResetGoal();
         cOldFocusFB.m_cDecContext.m_cEvent = null;
         GameObject.LogLine(" czyszcze cel dla " + cOldFocusFB);
      }

      this.BlockControl(true);
      this.SetLastControlTime();
      this.m_bChangeControlToUserRequest = true;
   }

   // [mod] ModSwitch : le joueur humain prend cFB (touche passe sans ballon, ou touche directe), comme ChangeActive,
   // puis verrou du changement automatique
   void SwitchTo(AIFootBaller cFB) {
      AIFootBaller cOldFocusFB = (AIFootBaller)super.m_cFocusFBaller;
      AIFootBaller cActive = (AIFootBaller)this.SetActive(cFB);
      if (cActive != null) {
         this.SetFocus(cActive, false);
      }

      if (cOldFocusFB != null && cOldFocusFB != super.m_cFocusFBaller) {
         this.m_cSwitchLeft = cOldFocusFB;
         this.m_fSwitchLeftTime = ModSwitch.Now();
         if (cOldFocusFB.HasEvent(1)) {
            cOldFocusFB.ResetGoal();
            cOldFocusFB.m_cDecContext.m_cEvent = null;
         }
      }

      this.BlockControl(true);
      this.SetLastControlTime();
      this.m_bChangeControlToUserRequest = true;
      ModSwitch.SetLock(this);
   }

   void ResetHelpBallAttacker() {
      this.m_cHelpBallAttacker = null;
   }

   void UpdateGame() {
      if (this.m_bSuperWallToCall && !((AIFootBaller)super.m_cGoalKeeper).IsDefence() && !SoccerModule.m_cMatch.IsWaiting()) {
         this.DoSuperDefence();
      }

      AIFootBaller cActiveFB = (AIFootBaller)super.m_cActiveFootBaller;
      Ball cBall = GameField.m_cBall;
      if (cActiveFB != null) {
         if (this.IsSuperSpeed() && !cActiveFB.IsSuperSpeed()) {
            this.ExchangeSuperSpeed(cActiveFB);
         }

         if (this.IsSuperFeint() && !cActiveFB.IsSuperFeint()) {
            this.ExchangeSuperFeint(cActiveFB);
         }
      }

      this.CheckSupers();
      FootBaller cBallPossesor = GameField.m_cBall.GetOwner();
      int nBallPossesTeam;
      if (cBallPossesor != null) {
         if (cBallPossesor.m_nTeam == super.m_nSide) {
            nBallPossesTeam = 1;
         } else {
            nBallPossesTeam = 2;
         }
      } else {
         nBallPossesTeam = 0;
      }

      switch (nBallPossesTeam) {
         case 0:
            AIFootBaller cNearest = this.FindNearest();
            boolean bNearestChanged = false;
            if (cNearest != null && cActiveFB != cNearest && (cActiveFB == null || !cActiveFB.IsPassDest()) && super.m_cStrategy.CanBeChangeNearest(cNearest)) {
               cActiveFB = (AIFootBaller)this.SetActive(cNearest);
            }

            if (GameField.m_cBall.IsDestReached()) {
               if (cActiveFB != null && !cActiveFB.HasFocus()) {
                  if (cActiveFB.IsUserControled() && !cActiveFB.Busy() && !cActiveFB.IsPassDest()) {
                     this.SetFocus(cActiveFB, false);
                  }

                  if (cActiveFB.HasGoal(3) && !cActiveFB.GoalUnreached() || !cActiveFB.HasGoal(3)) {
                     this.m_cAISystem.RequestEvent(cActiveFB, 2, GameField.GetDestPos());
                  }
               }
            } else if (cActiveFB != null) {
               if (this.IsPrevOwner() || !super.m_bUserControled && cActiveFB.m_fFreeBallInteria < GameField.m_cBall.GetLossTime()) {
                  if (!cActiveFB.HasFocus() && (bNearestChanged || !cActiveFB.HasGoal(3))) {
                     this.m_cAISystem.RequestEvent(cActiveFB, 2, GameField.GetDestPos());
                  }
               } else if (super.m_bUserControled && cActiveFB != null && !cActiveFB.HasFocus() && !cActiveFB.Busy()) {
                  this.SetFocus(cActiveFB, false);
               }
            }

            if (!this.CheckGoalKeeperGotoBall() && GameField.m_cBall.IsDestReached()) {
               this.CheckHelpBallAttack();
            }
            break;
         case 1:
            if ((cActiveFB == null || !cActiveFB.HasBall()) && !cBall.GetCurOwner().IsGoalKeeper() && !((AIFootBaller)cBall.GetCurOwner()).Busy()) {
               cActiveFB = (AIFootBaller)this.SetActive(cBall.GetCurOwner());
               if (super.m_bUserControled) {
                  this.SetFocus(cActiveFB, false);
               } else {
                  this.m_cAISystem.RequestEvent(cActiveFB, 4, null);
               }
            }

            if (super.m_cGoalKeeper.HasBall()) {
               AIFootBaller cGK = (AIFootBaller)super.m_cGoalKeeper;
               if (super.m_cStrategy.IsFBDistanceOpp(super.m_cGoalKeeper)) {
                  if (!cGK.HasGoal(19) || !cGK.GoalUnreached()) {
                     this.m_cAISystem.RequestEvent(cGK, 15, null);
                  }
               } else {
                  this.m_cAISystem.RequestEvent(cGK, 4, null);
               }
            }

            if (cActiveFB != null
               && cActiveFB.HasBall()
               && !cActiveFB.Busy()
               && !cActiveFB.IsDoingFeint()
               && !cActiveFB.IsUserControled()
               && (cActiveFB.GetGoal() == null || cActiveFB.GetGoal() != null && !cActiveFB.GoalUnreached())) {
               this.m_cAISystem.RequestEvent(cActiveFB, 4, null);
            }

            if (super.m_bUserControled && cActiveFB != null && !cActiveFB.HasFocus() && !cActiveFB.Busy()) {
               this.SetFocus(cActiveFB, false);
            }
            break;
         case 2:
            AIFootBaller cAttacker = this.FindAttacker();
            if (cActiveFB != null && (cAttacker == cActiveFB || !super.m_cStrategy.CanBeChangeAttacker(cAttacker))) {
               if (!cActiveFB.IsUserControled() & !cActiveFB.IsGoalKeeper()
                  && super.m_cStrategy.CanAttackerGoToOpp()
                  && (cActiveFB.HasGoal(9) && !cActiveFB.GoalUnreached() || !cActiveFB.HasGoal(9))
                  && !cActiveFB.Busy()
                  && GameField.m_cBall.GetPossesTime() > Strategy.m_fAttackInteria) {
                  this.m_cAISystem.RequestEvent(cActiveFB, 5, GameField.m_cBall.GetPosition());
               }
            } else {
               cActiveFB = (AIFootBaller)this.SetActive(cAttacker);
               if (cActiveFB != null) {
                  if (!cActiveFB.IsUserControled()) {
                     if (!cActiveFB.Busy() && GameField.m_cBall.GetPossesTime() > cActiveFB.m_fAttackInteria && super.m_cStrategy.CanAttackerGoToOpp()) {
                        this.m_cAISystem.RequestEvent(cActiveFB, 5, GameField.m_cBall.GetPosition());
                     }
                  } else if (!cActiveFB.Busy()) {
                     this.SetFocus(cActiveFB, false);
                  }
               }
            }

            if (super.m_bUserControled && cActiveFB != null && !cActiveFB.HasFocus() && !cActiveFB.Busy() && !SoccerModule.m_cMatch.m_bGameStarted) {
               this.SetFocus(cActiveFB, false);
               SoccerModule.m_cMatch.m_bGameStarted = true;
            }

            if (!this.CheckGoalKeeperAttack()) {
               this.CheckHelpAttack();
            }
      }

      this.m_fLastSituationCalcTime = SoccerGame.GetSoccerModule().GetTime();
      int nSize = super.m_vFootBallers.size();
      if (nSize != 0) {
         AIFootBaller cActive = (AIFootBaller)super.m_cActiveFootBaller;

         for (int i = 0; i < nSize; i++) {
            AIFootBaller cAIFootBaller = (AIFootBaller)super.m_vFootBallers.elementAt(i);
            if (!cAIFootBaller.m_bHasSingleUpdate && i == this.m_nFBallerToCalculation || !cAIFootBaller.m_bSituationFixed || cAIFootBaller.IsGoalKeeper()) {
               if (!cAIFootBaller.IsGoalKeeper()) {
                  cAIFootBaller.CalculateParams();
               }

               if (!cAIFootBaller.Busy() && !cAIFootBaller.IsGoalKeeper() && this.IsPossesor()) {
                  if (cAIFootBaller.HasBall() && !cAIFootBaller.IsUserControled()) {
                     if (cAIFootBaller.m_cDecContext.m_cSituation.m_fOpportunity > super.m_cStrategy.m_fShotOpportunity) {
                        AIFootBaller.Shot cShot = super.m_cStrategy.GetShotDescription(cAIFootBaller);
                        if (cShot != null) {
                           cAIFootBaller.SetShot(cShot);
                           this.m_cAISystem.RequestEvent(cAIFootBaller, 9, null);
                        }
                     }

                     if (cAIFootBaller.m_bHasSingleUpdate) {
                        continue;
                     }

                     // [mod] passe dans la course d'un coéquipier qui fait un appel, sinon décision d'origine
                     if (!ModMovement.AIThroughPass(this, cAIFootBaller)) {
                        this.PassAndShotChance(cAIFootBaller);
                     }

                     if (cAIFootBaller.m_bHasSingleUpdate) {
                        continue;
                     }
                  }

                  // [mod] sans ballon, le démarquage de ModMovement remplace le « meilleur point » d'origine
                  if (this.CanChangeSituation(cAIFootBaller) && (cAIFootBaller.HasBall() || !ModMovement.IsEnabled())) {
                     Vector vBetterPos = super.m_cStrategy.ChoseBestNewPosition(cAIFootBaller, super.m_cStrategy.m_fNewPosDist);
                     if (vBetterPos != null) {
                        vBetterPos = super.m_cStrategy.TuneValidScope(cAIFootBaller, vBetterPos);
                        if (cAIFootBaller.IsUserControled() && !cAIFootBaller.HasBall()) {
                        }
                     }

                     if ((!cAIFootBaller.GoalUnreached() || cAIFootBaller.HasGoal(7) || cAIFootBaller.HasGoal(2) || cAIFootBaller.HasGoal(5))
                        && vBetterPos != null
                        && (cAIFootBaller.HasBall() || GameField.GetZoneForPoint(vBetterPos, cAIFootBaller.m_nTeam) == 2)) {
                        this.m_cAISystem.RequestEvent(cAIFootBaller, 12, vBetterPos);
                     }
                  }
               }

               this.UpdateValidPos(cAIFootBaller);
               cAIFootBaller.Update();
            }
         }

         if (this.m_nFBallerToCalculation >= nSize) {
            this.m_nFBallerToCalculation = 0;
         } else {
            this.m_nFBallerToCalculation++;
         }
      }
   }

   void SetLastControlTime() {
      this.m_fLastControlTime = SoccerGame.GetSoccerModule().GetTime();
   }

   void CheckHotSpecialGoals(int nEventId, int nGoalId, Object cObject) {
      int nSize = super.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         AIFootBaller cFBaller = (AIFootBaller)super.m_vFootBallers.elementAt(i);
         if (cFBaller.HasSpecialGoal() && !cFBaller.IsSpecialGoalDone()) {
            cFBaller.CheckHotGoal(nEventId, nGoalId, cObject, cFBaller.HasSpecialGoal());
         }
      }
   }

   void ResetStabilizationRaw() {
      int nSize = super.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         AIFootBaller cFBaller = (AIFootBaller)super.m_vFootBallers.elementAt(i);
         cFBaller.ResetStabilization();
      }

      this.m_bIsToStabilization = false;
   }

   public void UpdateParams() {
      if (this.IsPossesor()) {
         this.ResetHelpAttacker();
         this.ResetHelpBallAttacker();
      }

      if (super.m_bUserControled) {
         if (AIMatch.IsMatchState(0) && super.m_cFocusFBaller != null && !super.m_cFocusFBaller.HasFocusMark() && !Match.m_bPlaying) {
            super.m_cFocusFBaller.SetFocusMark();
         }

         int nSize = super.m_vFootBallers.size();

         for (int i = 0; i < nSize; i++) {
            AIFootBaller cFBaller = (AIFootBaller)super.m_vFootBallers.elementAt(i);
            if (cFBaller.HasFocusMark() && (!AIMatch.IsMatchState(0) || cFBaller != super.m_cFocusFBaller && super.m_cFocusFBaller != null)) {
               cFBaller.ResetFocusMark();
            }
         }
      }

      if (super.m_bUserControled) {
         if (GameField.m_cBall.IsDestReached() || !this.IsBlockControl() || this.m_bChangeControlToUserRequest) {
            if (super.m_cTeamController._KEY_STATES != 0 && !this.IsBlockControl()) {
               this.BlockControl(true);
            }

            if (this.m_fLastControlTime != -1.0F && super.m_cTeamController._KEY_STATES != 0) {
               this.m_fLastControlTime = -1.0F;
            }

            if (this.m_bOldKeyState != 0 && super.m_cTeamController._KEY_STATES == 0) {
               this.SetLastControlTime();
            }

            this.m_bOldKeyState = super.m_cTeamController._KEY_STATES;
         }

         if (this.m_fLastControlTime != -1.0F
            && SoccerGame.GetSoccerModule().GetTime() - this.m_fLastControlTime > super.m_cStrategy.m_fBlockControlTime
            && ModSwitch.CanUnblock(this)) { // [mod] verrou après un changement manuel
            this.BlockControl(false);
            this.m_fLastControlTime = -1.0F;
         }
      }
   }

   boolean CheckGoalKeeperGotoBall() {
      AIFootBaller cGK = (AIFootBaller)super.m_cGoalKeeper;
      if (GameField.m_cBall.IsDestReached() && cGK != null) {
         if (!(GameField.m_cBall.GetDistanceTo(cGK) < super.m_cStrategy.m_fMinDistFBallers) || cGK.HasGoal(3) && (!cGK.HasGoal(3) || cGK.GoalUnreached())) {
            return false;
         }

         this.m_cAISystem.RequestEvent(cGK, 2, GameField.GetDestPos());
         return true;
      } else {
         return false;
      }
   }

   void CalculateParamsForAll() {
      int nSize = super.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         AIFootBaller cFBaller = (AIFootBaller)super.m_vFootBallers.elementAt(i);
         cFBaller.CalculateParams();
      }
   }

   void ResetSuperWall() {
      ((AIFootBaller)super.m_cGoalKeeper).DisableCallMethod("SetSuperWallForTeam");
      SoccerModule.GetHud().FlashSuperIconEnd(super.m_nSide);
      this.m_bSuperWallActive = false;
      this.m_cSuperWall.Hide();
      super.m_cGoalKeeper.CallMethodIn("MoveSuperWallToInfinity", this.m_cSuperWall.m_fSlideInTime);
   }

   void SetStabilization(boolean bWithDelay) {
      this.m_bIsToStabilization = true;
      this.SetActive(null);
      if (super.m_bUserControled) {
         this.SetFocus(null, false);
      }

      this.UpdateStabilizationPos();
      int nSize = super.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         AIFootBaller cFBaller = (AIFootBaller)super.m_vFootBallers.elementAt(i);
         cFBaller.DisableCallMethod("DelayFocus");
         if (bWithDelay) {
            cFBaller.SetStabilizationWithDelay();
         } else {
            cFBaller.SetStabilization();
         }
      }
   }

   boolean IsStabilization() {
      return this.m_bIsToStabilization;
   }

   void ClearActiveAndFocus() {
      this.SetHelpAttacker(null);
      this.SetHelpBallAttacker(null);
      super.ClearActiveAndFocus();
   }

   AITeam() {
      SoccerGame.IncTeam();
      this.m_cOrder = new Order();
   }

   public void MoveSuperWallToInfinity() {
   }

   void WrtiteStateAsCrashLogFBallers() {
      int nSize = super.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         AIFootBaller cFBaller = (AIFootBaller)super.m_vFootBallers.elementAt(i);
         cFBaller.WrtiteStateAsCrashLog();
      }
   }

   void UpdateStabilizationPos() {
      if (AIMatch.IsMatchState(7)) {
         this.UpdateOrderOnStart(super.m_nSide);
      }

      int nSize = super.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         AIFootBaller cFBaller = (AIFootBaller)super.m_vFootBallers.elementAt(i);
         switch (AIMatch.GetMatchState()) {
            case 1:
               cFBaller.m_vStabilizationPos = cFBaller.cSpawnPointPresentation.GetPosition();
            case 2:
            default:
               break;
            case 3:
               cFBaller.m_vStabilizationPos = cFBaller.cSpawnPointStart.GetPosition();
               break;
            case 4:
            case 5:
            case 6:
               this.UpdateValidPos(cFBaller);
               cFBaller.m_vStabilizationPos = new Vector(cFBaller.m_vValidPos);
               break;
            case 7:
               this.UpdateValidPos(cFBaller);
               cFBaller.m_vStabilizationPos = cFBaller.cSpawnPoint.GetPosition();
               // [mod] placement de coup d'envoi de la carte tactique
               Vector vKickoff = ModTactics.KickoffPos(cFBaller);
               if (vKickoff != null) {
                  cFBaller.m_vStabilizationPos = vKickoff;
               }
         }
      }
   }

   void CheckStabilizationProcess() {
      int nSize = super.m_vFootBallers.size();
      boolean bAllStabilzationFinished = true;

      for (int i = 0; i < nSize; i++) {
         AIFootBaller cFBaller = (AIFootBaller)super.m_vFootBallers.elementAt(i);
         if (!cFBaller.m_bWaitingForDelayStab) {
            cFBaller.CheckHotGoal(16, 20, null, cFBaller.IsStabilization());
         }

         if (cFBaller.IsStabilization()) {
            bAllStabilzationFinished = false;
         }
      }

      this.m_bIsToStabilization = !bAllStabilzationFinished;
   }

   void RefreshContext() {
      int nSize = super.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         AIFootBaller cAIFootBaller = (AIFootBaller)super.m_vFootBallers.elementAt(i);
         if (super.m_cActiveFootBaller == cAIFootBaller) {
            cAIFootBaller.m_cDecContext.m_bActive = true;
         } else {
            cAIFootBaller.m_cDecContext.m_bActive = false;
         }
      }
   }

   void CheckHelpBallAttack() {
      if (super.m_cActiveFootBaller != null) {
         AIFootBaller cHelpBallAttacker = this.FindNearestExclude((AIFootBaller)super.m_cActiveFootBaller);
         if (cHelpBallAttacker != null && cHelpBallAttacker != this.m_cHelpBallAttacker) {
            this.SetHelpBallAttacker(cHelpBallAttacker);
         }

         if (cHelpBallAttacker != null
            && (cHelpBallAttacker.HasGoal(3) && !cHelpBallAttacker.GoalUnreached() || !cHelpBallAttacker.HasGoal(3))
            && !cHelpBallAttacker.Busy()) {
            this.m_cAISystem.RequestEvent(cHelpBallAttacker, 2, GameField.GetDestPos());
         }
      }
   }

   void RequestSuperDefence() {
      this.m_bSuperWallToCall = true;
   }

   AIFootBaller FindAttacker() {
      float fMinDist = GameField.GetDiagonalLength();
      int nMinFBaller = -1;
      Vector vBallPos = GameField.m_cBall.GetPosition();
      int nSize = super.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         FootBaller cFBaller = (FootBaller)super.m_vFootBallers.elementAt(i);
         if (!((AIFootBaller)cFBaller).IsGoalKeeper() && cFBaller != SoccerModule.m_cMatch.m_cActiveLossFB) {
            float fDistance = cFBaller.GetDistanceTo(vBallPos);
            if (fDistance < fMinDist && super.m_cStrategy.IsForwardFromPos(cFBaller, vBallPos)) {
               fMinDist = fDistance;
               nMinFBaller = i;
            }
         }
      }

      AIFootBaller cResultFB = null;
      if (nMinFBaller != -1) {
         cResultFB = (AIFootBaller)super.m_vFootBallers.elementAt(nMinFBaller);
      }

      AIFootBaller cNearest = this.FindNearest();
      if (cResultFB == null) {
         return cNearest;
      } else if (cNearest == null) {
         return null;
      } else {
         return Math.abs(cNearest.GetDistanceTo(vBallPos) / cResultFB.GetDistanceTo(vBallPos)) > super.m_cStrategy.m_fMinForwardToNear ? cResultFB : cNearest;
      }
   }

   void SetHelpAttacker(AIFootBaller cHelpAttacker) {
      if (this.m_cHelpAttacker != null && this.m_cHelpAttacker != cHelpAttacker && this.m_cHelpAttacker.HasGoal(3) && this.m_cHelpAttacker.GoalUnreached()) {
         this.m_cHelpAttacker.GetGoal().m_bReached = true;
         this.m_cHelpAttacker.GetGoal().ActionAfterReached(this.m_cHelpAttacker);
      }

      this.m_cHelpAttacker = cHelpAttacker;
   }

   void SetSelDestTracker(AIFootBaller cSelDestTracker) {
      this.m_cSelDestTracker = cSelDestTracker;
   }

   void WrtiteStateAsLogFBallers() {
      int nSize = super.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         AIFootBaller cFBaller = (AIFootBaller)super.m_vFootBallers.elementAt(i);
         cFBaller.WrtiteStateAsLog();
      }
   }

   // [mod] tirage pondéré des 8 supers (data/supers.scr)
   int GetRandomSuper() {
      return ModSupers.GetRandomSuper(this);
   }

   void CheckSupers() {
      if (!super.m_bUserControled && super.m_bSuperAvailable && !super.m_bSuperDoing) {
         AIFootBaller cActive = (AIFootBaller)super.m_cActiveFootBaller;
         if (cActive == null) {
            return;
         }

         switch (super.m_nSuperType) {
            case 0:
            case 1:
            case 2:
            case ModSupers._FREEZE: // [mod] le gel sert à filer au but, comme la vitesse
               if (cActive.HasBall()) {
                  Vector vOppGatePos = super.m_cStrategy.GetOpponentGatePos(cActive);
                  float fGateDist = cActive.GetDistanceTo(vOppGatePos);
                  float fScope = GameField.cCenter.GetDistanceTo(vOppGatePos);
                  if (!(fGateDist > fScope) && (super.m_nSuperType == 0 || super.m_cStrategy.GetFBDistance(cActive) == null)) {
                     cActive.UseSuper();
                  }
               }
               break;
            case ModSupers._CRAZY:
            case ModSupers._DOUBLE:
               // [mod] tirs que le gardien peut arrêter : seulement à distance de tir
               if (cActive.HasBall()) {
                  Vector vOppGatePos = super.m_cStrategy.GetOpponentGatePos(cActive);
                  if (cActive.GetDistanceTo(vOppGatePos) <= super.m_cStrategy.m_fGateDistRadius) {
                     cActive.UseSuper();
                  }
               }
               break;
            case 3:
            case ModSupers._MAGNET: // [mod] l'aimant défend, comme le mur
               int nZone = GameField.GetZoneForPoint(GameField.m_cBall.GetPositionVectorVolatile(), super.m_nSide);
               if (!this.IsPossesor() && nZone != 2 && nZone != 1) {
                  cActive.UseSuper();
               }
         }
      }
   }

   public void delete() {
      if (this.m_cAISystem != null) {
         this.m_cAISystem.delete();
      }

      this.m_cOrder = null;
      this.m_cSuperWall = null;
      this.m_cHelpAttacker = null;
      this.m_cHelpBallAttacker = null;
      this.m_cSelDestTracker = null;
      super.delete();
   }

   void RefreshMatchContext(DecisionContext cDecContext) {
      if (cDecContext != null) {
         int nSize = super.m_vFootBallers.size();

         for (int i = 0; i < nSize; i++) {
            AIFootBaller cAIFootBaller = (AIFootBaller)super.m_vFootBallers.elementAt(i);
            cAIFootBaller.m_cDecContext.m_nBallPossesor = cDecContext.m_nBallPossesor;
         }
      }
   }

   FootBaller SetActive(FootBaller cFBaller) {
      AIFootBaller cActive = (AIFootBaller)super.m_cActiveFootBaller;
      if (cActive != null && cActive != cFBaller && cActive.HasGoal(3) && cActive.GoalUnreached()) {
         cActive.GetGoal().m_bReached = true;
         cActive.GetGoal().ActionAfterReached(cActive);
      }

      if (cActive != null) {
         cActive.m_fAttackInteria = GameObject.RandomFloat(0.4F, Strategy.m_fAttackInteria);
      }

      if (cActive != null) {
         cActive.m_fFreeBallInteria = GameObject.RandomFloat(0.2F, Strategy.m_fFreeBallInteria);
      }

      if (cFBaller == this.m_cHelpBallAttacker) {
         this.m_cHelpBallAttacker = null;
      }

      if (cFBaller == this.m_cHelpAttacker) {
         this.m_cHelpAttacker = null;
      }

      FootBaller cResult = super.SetActive(cFBaller);
      this.RefreshContext();
      return cResult;
   }
}
