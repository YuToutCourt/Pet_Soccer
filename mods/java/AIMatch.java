public class AIMatch extends Match {
   static int m_nMatchState = -1;
   static boolean m_bStateStarted = false;
   static final int STATE_GAME = 0;
   static final int STATE_INTRO = 1;
   static final int STATE_END = 3;
   static final int STATE_CORNER = 4;
   static final int STATE_OUT = 5;
   static final int STATE_OUT_GK = 6;
   static final int STATE_START_GAME = 7;
   static final int START_GAME_POS_OFF = 50;
   AISystem m_cAISystem = null;
   boolean m_bFirstPass = false;
   boolean m_bIsStabilization = false;
   boolean m_bWaiting = false;
   boolean m_bSpecialBallStab = false;
   float m_fZofOut = 0.0F;
   boolean m_bIsGoalReplay = false;
   boolean m_bLastOutIsShoot = false;
   boolean m_bRestartAfterEnd = false;
   boolean m_bEndBreak = false;
   boolean m_bBallPossesToCount = false;
   AIFootBaller m_cSpecialFB = null;
   AIFootBaller m_cLastSpecialFB = null;
   AIFootBaller m_cSpecialDestFB = null;
   AIFootBaller m_cActiveLossFB = null;
   AIFootBaller m_cSpecialFBRight = null;
   AIFootBaller m_cSpecialFBLeft = null;
   AIFootBaller m_test_cLatestSpecialFB = null;
   boolean m_bGoneToSpecialBall = false;
   boolean m_bDestGoneToSpecialBall = false;
   boolean m_bSpecialBallDone = false;
   boolean m_bSpecialBallStarted = false;
   boolean m_bStartGameWhistleDone = false;
   boolean m_bJustRestart = false;
   boolean m_bTeamsGoneToLoterry = false;
   boolean m_bRefereeTossing = false;
   boolean m_bRefereeDecision = false;
   boolean m_bIntroFinished = false;
   boolean m_bHalfChanging = false;
   boolean m_bMatchEnding = false;
   boolean m_bWasHalfMenu = false;
   boolean m_bSpaceCanBePressed = false;
   boolean m_bWasSpacePressed = false;
   boolean m_bGoalReact = false;
   boolean m_bTeamChangedSides = false;
   boolean m_bGKOut = false;
   boolean m_bGameStarted = false;
   int m_nSpecialGoalSide = -1;
   SelectionDest cSpecialBallSel = null;
   int m_nTeamToStart = -1;
   int m_nTeamStartedFirstHalf = -1;
   boolean m_bTeamFirstCommented = false;
   float m_fTeamFirstCommentTime = 0.0F;
   boolean m_bTeamSecCommented = false;
   float m_fTeamSecCommentTime = 0.0F;
   boolean m_bAnythigCommented = false;
   float m_fAnythigCommentTime = 0.0F;
   boolean m_bWeatherCommented = false;
   float m_fWeatherCommentTime = 0.0F;
   boolean m_bWasSuperCommeneted = true;
   boolean m_bSuperCommentWithBaller = false;
   String m_sDB_SpecialBallPoint = " none ";
   int m_nTeamToUpdate = 0;
   float m_fDistUpdateLastTime = 0.0F;
   int m_nUpdateInc = 0;
   AIFootBaller m_cSuperSpeedSoundFB = null;
   AIFootBaller m_cSuperFeintSoundFB = null;
   static int PROFILE_UpdateMatch = 0;
   static int PROFILE_UpdateTeam0 = 0;
   static int PROFILE_UpdateTeam1 = 0;
   static int PROFILE_UpdateParams = 0;
   static int PROFILE_UpdateGame = 0;

   void UpdateEndGame() {
   }

   float TossCommentTime() {
      return GameObject.RandomFloat(super.m_nHalfTime * 2);
   }

   void ClearSuperSounds() {
      if (GameField.m_cBall.IsSuperShot() && GameField.m_cBall.m_nSuperShotSound != -1) {
         GameField.m_cBall.DetachSound(GameField.m_cBall.m_nSuperShotSound);
      }

      AITeam cTeamLeft = (AITeam)this.GetTeam(0);
      AITeam cTeamRight = (AITeam)this.GetTeam(1);
      if (cTeamLeft.IsSuperWall() && cTeamLeft.m_cSuperWall.m_nSuperDefenceSound != -1) {
         cTeamLeft.m_cSuperWall.StopSound(cTeamLeft.m_cSuperWall.m_nSuperDefenceSound);
      }

      if (cTeamRight.IsSuperWall() && cTeamRight.m_cSuperWall.m_nSuperDefenceSound != -1) {
         cTeamRight.m_cSuperWall.StopSound(cTeamRight.m_cSuperWall.m_nSuperDefenceSound);
      }

      if (cTeamLeft.m_bSuperSpeed && cTeamLeft.m_cSuperFB.m_nSuperSpeedSound != -1) {
         cTeamLeft.m_cSuperFB.DetachSound(cTeamLeft.m_cSuperFB.m_nSuperSpeedSound);
         cTeamLeft.m_cSuperFB.m_nSuperSpeedSound = -1;
      }

      if (cTeamLeft.m_bSuperFeint && cTeamLeft.m_cSuperFB.m_nSuperFeintSound != -1) {
         cTeamLeft.m_cSuperFB.DetachSound(cTeamLeft.m_cSuperFB.m_nSuperFeintSound);
         cTeamLeft.m_cSuperFB.m_nSuperFeintSound = -1;
      }

      if (cTeamRight.m_bSuperSpeed && cTeamRight.m_cSuperFB.m_nSuperSpeedSound != -1) {
         cTeamRight.m_cSuperFB.DetachSound(cTeamRight.m_cSuperFB.m_nSuperSpeedSound);
         cTeamRight.m_cSuperFB.m_nSuperSpeedSound = -1;
      }

      if (cTeamRight.m_bSuperFeint && cTeamRight.m_cSuperFB.m_nSuperFeintSound != -1) {
         cTeamRight.m_cSuperFB.DetachSound(cTeamRight.m_cSuperFB.m_nSuperFeintSound);
         cTeamRight.m_cSuperFB.m_nSuperFeintSound = -1;
      }
   }

   public void Update() {
      if (PROFILE_UpdateMatch == 0) {
         PROFILE_UpdateMatch = Match.m_cModule.ProfileAdd("Soccer", "UpdateMatch");
         PROFILE_UpdateParams = Match.m_cModule.ProfileAdd("Soccer", "UpdateParams");
         PROFILE_UpdateTeam0 = Match.m_cModule.ProfileAdd("Soccer", "UpdateTeam0");
         PROFILE_UpdateTeam1 = Match.m_cModule.ProfileAdd("Soccer", "UpdateTeam1");
      }

      Match.m_cModule.ProfileBegin(PROFILE_UpdateMatch);
      super.Update();
      String ctr = " state " + m_nMatchState + " waiting " + this.m_bWaiting + " started " + m_bStateStarted;
      if (super.m_bInited) {
         ModKickoff.Update(this); // [mod] traces et test du coup d'envoi
         Match.m_cModule.ProfileBegin(PROFILE_UpdateParams);
         ((AITeam)super.m_teams[0]).UpdateParams();
         ((AITeam)super.m_teams[1]).UpdateParams();
         super.m_cReferee.UpdateParams();

         for (int i = 0; i < 2; i++) {
            for (int j = 0; j < super.m_teams[i].m_vFootBallers.size(); j++) {
               AIFootBaller cFBaller = (AIFootBaller)super.m_teams[i].m_vFootBallers.elementAt(j);
               cFBaller.UpdateParams();
            }
         }

         Match.m_cModule.ProfileEnd(PROFILE_UpdateParams);
         if (this.IsStabilizationNew()) {
            if (!((AITeam)super.m_teams[0]).IsStabilization() && !((AITeam)super.m_teams[1]).IsStabilization() && !super.m_cReferee.IsStabilization()) {
               this.ResetStabilizationNew();
            } else if (IsMatchState(3) && this.EveryoneOutsideOrNotFrustum() && !this.m_bEndBreak) {
               this.m_bEndBreak = true;
               Match.m_cModule.CallSyncMethod("CallEndGameRaw");
            }
         } else if (this.IsWaiting() && !this.IsStateStarted()) {
            this.ActionAfterStabilization();
         }

         if (!this.IsStabilizationNew() && this.IsStateStarted()) {
            switch (GetMatchState()) {
               case 0:
                  if (!this.IsWaiting()) {
                     this.UpdateGame();
                  }
                  break;
               case 1:
                  this.UpdateIntro();
               case 2:
               default:
                  break;
               case 3:
                  this.UpdateEndGame();
                  break;
               case 4:
               case 5:
               case 6:
               case 7:
                  this.UpdateSpecialBall();
            }
         }

         if (this.m_nTeamToUpdate == 0) {
            if (super.m_teams[0] != null) {
               Match.m_cModule.ProfileBegin(PROFILE_UpdateTeam0);
               ((AITeam)super.m_teams[0]).Update();
               Match.m_cModule.ProfileEnd(PROFILE_UpdateTeam0);
            }

            if (super.m_teams[1] != null) {
               Match.m_cModule.ProfileBegin(PROFILE_UpdateTeam1);
               ((AITeam)super.m_teams[1]).Update();
               Match.m_cModule.ProfileEnd(PROFILE_UpdateTeam1);
            }
         }

         this.m_nTeamToUpdate = (this.m_nTeamToUpdate + 1) % 2;
         super.m_cReferee.Update();
         Match.m_cModule.ProfileEnd(PROFILE_UpdateMatch);
      }
   }

   boolean EveryoneOutsideOrNotFrustum() {
      boolean bAllOutside = true;

      for (int t = 0; t < 2; t++) {
         for (int f = 0; f < super.m_teams[t].m_vFootBallers.size(); f++) {
            AIFootBaller cFBaller = (AIFootBaller)super.m_teams[t].m_vFootBallers.elementAt(f);
            if (!cFBaller.IsOutside() && cFBaller.IsInFrustum()) {
               bAllOutside = false;
               break;
            }
         }
      }

      if (!super.m_cReferee.IsOutside() && super.m_cReferee.IsInFrustum()) {
         bAllOutside = false;
      }

      return bAllOutside;
   }

   void StartIntro() {
      this.m_bTeamsGoneToLoterry = false;
      this.m_bRefereeTossing = false;
      this.m_bRefereeDecision = false;
      this.m_bIntroFinished = false;
      AITeam cTeamLeft = (AITeam)super.m_teams[0];
      AITeam cTeamRight = (AITeam)super.m_teams[1];
      this.m_cSpecialFBLeft = cTeamLeft.FindNearest();
      this.m_cSpecialFBRight = cTeamRight.FindNearest();
      super.m_cReferee.SetIgnoreCollision();
      if (this.m_cSpecialFBLeft != null) {
         this.m_cSpecialFBLeft.SetSpecialGoal();
         Vector vLeft = super.m_cStrategy.GetLotteryPosition(this.m_cSpecialFBLeft);
         this.m_cAISystem.RequestEvent(this.m_cSpecialFBLeft, 18, vLeft);
      }

      if (this.m_cSpecialFBRight != null) {
         this.m_cSpecialFBRight.SetSpecialGoal();
         Vector vRight = super.m_cStrategy.GetLotteryPosition(this.m_cSpecialFBRight);
         this.m_cAISystem.RequestEvent(this.m_cSpecialFBRight, 18, vRight);
      }
   }

   public void Restart() {
      this.m_bJustRestart = true;
      this.ReplayStop();
      SoccerModule.ShowHud();
      Match.m_cModule.GetSoccerCamera().ClearReplayCamera();
      super.Restart();
      if (SoccerGame.GetSoccerModule().m_nLevelID == 18) {
         GameField.m_cBall.SetMass(100.0F);
      } else {
         GameField.m_cBall.SetMass(200.0F);
      }

      Match.m_cModule.DisableCallMethod("CallBreakStab");
      Match.m_cModule.DisableCallMethod("CallBreakPauseAfterGoal");
      Match.m_cModule.DisableCallMethod("BreakSpecialBallStab");
      Match.m_cModule.DisableCallMethod("OnGoal");
      this.m_nUpdateInc = 0;
      if (CoreLogic.m_bDemo) {
         super.m_cStrategy.FixDifficultyLevel(4);
      } else {
         super.m_cStrategy.FixDifficultyLevel(CoreLogic.GetDiffLevel());
      }

      ModTactics.OnRestart(this); // [mod] rôles des joueurs

      if (super.m_nHalfNo == 1 && !super.m_bGoldGoal) {
         this.ChangeTeamSides();
      }

      ((AITeam)this.GetTeam(0)).ResetTeamFunction();
      ((AITeam)this.GetTeam(1)).ResetTeamFunction();
      if (this.m_cSpecialFB != null) {
         this.m_cSpecialFB.SetLocalDest(Vector.Zero);
      }

      this.m_bRestartAfterEnd = false;
      this.m_bEndBreak = false;
      this.m_bGameStarted = false;
      this.m_bHalfChanging = false;
      this.m_bMatchEnding = false;
      this.m_bGoalReact = false;
      this.m_bBallPossesToCount = false;
      super.m_bGoldGoal = false;
      this.SetSpaceActive(true);
      this.m_cSpecialDestFB = null;
      super.m_cClockRef.Stop();
      super.m_cClockRef.Reset();
      super.m_nHalfNo = 0;
      this.m_bTeamFirstCommented = false;
      this.m_fTeamFirstCommentTime = this.TossCommentTime();
      this.m_bTeamSecCommented = false;
      this.m_fTeamSecCommentTime = this.TossCommentTime();
      this.m_bAnythigCommented = false;
      this.m_fAnythigCommentTime = this.TossCommentTime();
      this.m_bWeatherCommented = false;
      this.m_fWeatherCommentTime = this.TossCommentTime();
      this.ResetMatchParams();
      this.RestartControllers();
      if (this.IsStabilizationNew()) {
         this.ResetStabilizationRaw();
      }

      this.FixControledTeamSide();
      this.cSpecialBallSel.SetActive(false);
      this.MakeWeatherEvents(SoccerGame.GetSoccerModule().m_nLevelID);
      this.ResetSupers();
      super.m_nScoreLeft = super.m_nScoreRight = 0;
      this.ResetStatistics();
      this.RestoreStartPositions(true);
      this.ClearAI(true);
      SetMatchState(1);
      Match.m_cModule.GetSoccerCamera().ClearEndHalfCamera();
      Match.m_cModule.GetSoccerCamera().SetEndHalfCamera();
      this.SetStabilizationNew(false);
      this.m_bJustRestart = false;
   }

   void SetParams(AISystem cAI) {
      this.m_cAISystem = cAI;
      super.m_cStrategy = cAI.m_cStrategy;
   }

   void RestartGame(int nSide) {
      ((AITeam)this.GetTeam(0)).ResetTeamFunction();
      ((AITeam)this.GetTeam(1)).ResetTeamFunction();
      this.m_cSpecialDestFB = null;
      this.m_bEndBreak = false;
      this.SetSpaceActive(false);
      this.RestoreGamePositions(false);
      this.m_nTeamToStart = nSide;
      if (this.IsStabilizationNew()) {
         this.ResetStabilizationRaw();
      }

      this.RestartControllers();
      SetMatchState(7);
      if (CoreLogic.GetPlayer().m_bReplaysOn) {
         Recorder.StartClock();
      }

      this.RestartSpecialBall();
      SoccerModule.GetHud().ShowInfo();
   }

   void UpdateOrder() {
      ((AITeam)super.m_teams[0]).UpdateOrder();
      ((AITeam)super.m_teams[1]).UpdateOrder();
   }

   void ChangeTeamSides() {
      ModNet.HostSwap(super.m_nHalfNo); // [mod] en ligne : le client change de côté au même moment
      AITeam cTeamLeft = (AITeam)super.m_teams[0];
      AITeam cTeamRight = (AITeam)super.m_teams[1];
      SpawnPoint cSpawnPoint_left = null;
      SpawnPoint cSpawnPointStart_left = null;
      SpawnPoint cSpawnPointPresentation_left = null;
      SpawnPoint cSpawnPoint_right = null;
      SpawnPoint cSpawnPointStart_right = null;
      SpawnPoint cSpawnPointPresentation_right = null;
      PatrolNode cPatrolPath_left_0 = null;
      PatrolNode cPatrolPath_left_1 = null;
      PatrolNode cPatrolPath_left_2 = null;
      PatrolNode cPatrolPath_left_3 = null;
      PatrolNode cPatrolPath_right_0 = null;
      PatrolNode cPatrolPath_right_1 = null;
      PatrolNode cPatrolPath_right_2 = null;
      PatrolNode cPatrolPath_right_3 = null;
      int nFBCount = cTeamLeft.m_vFootBallers.size();

      for (int i = 0; i < nFBCount; i++) {
         AIFootBaller cFBallerLeft = (AIFootBaller)cTeamLeft.m_vFootBallers.elementAt(i);
         AIFootBaller cFBallerRight = (AIFootBaller)cTeamRight.m_vFootBallers.elementAt(i);
         cFBallerLeft.m_nTeam = 1;
         cFBallerRight.m_nTeam = 0;
         cSpawnPoint_left = cFBallerLeft.cSpawnPoint;
         cSpawnPointStart_left = cFBallerLeft.cSpawnPointStart;
         cSpawnPointPresentation_left = cFBallerLeft.cSpawnPointPresentation;
         cSpawnPoint_right = cFBallerRight.cSpawnPoint;
         cSpawnPointStart_right = cFBallerRight.cSpawnPointStart;
         cSpawnPointPresentation_right = cFBallerRight.cSpawnPointPresentation;
         cFBallerLeft.cSpawnPoint = cSpawnPoint_right;
         cFBallerLeft.cSpawnPointStart = cSpawnPointStart_right;
         cFBallerLeft.cSpawnPointPresentation = cSpawnPointPresentation_right;
         cFBallerRight.cSpawnPoint = cSpawnPoint_left;
         cFBallerRight.cSpawnPointStart = cSpawnPointStart_left;
         cFBallerRight.cSpawnPointPresentation = cSpawnPointPresentation_left;
         int nPatrolCount = cFBallerLeft.aPatrolPath.length;

         for (int p = 0; p < nPatrolCount; p++) {
            PatrolNode cLeftPatrol = cFBallerLeft.aPatrolPath[p];
            cFBallerLeft.aPatrolPath[p] = cFBallerRight.aPatrolPath[p];
            cFBallerRight.aPatrolPath[p] = cLeftPatrol;
         }
      }

      cTeamLeft = (AITeam)super.m_teams[0];
      super.m_teams[0] = super.m_teams[1];
      super.m_teams[1] = cTeamLeft;
      ((AITeam)super.m_teams[0]).m_nSide = 0;
      ((AITeam)super.m_teams[1]).m_nSide = 1;
      ((AITeam)super.m_teams[0]).FixOrder();
      ((AITeam)super.m_teams[1]).FixOrder();
      this.FixControledTeamSide();
      this.ResetSupers();
      this.m_bTeamChangedSides = true;
   }

   void ActionAfterStabilization() {
      this.m_sDB_SpecialBallPoint = " ActionAfterStabilization " + GetMatchState();
      boolean bStateStarted = true;
      int nState = GetMatchState();
      switch (GetMatchState()) {
         case 1:
            this.ClearSendHalfCamera();
            this.StartIntro();
         case 2:
         default:
            break;
         case 3:
            Match.m_cModule.DisableCallMethod("CallEndGameRaw");
            Match.m_cModule.GetSoccerCamera().ClearEndHalfCamera();
            GameField.m_cBall.ResetPhysics();
            if (this.m_bHalfChanging) {
               bStateStarted = false;
               Match.m_cModule.CallMenuInGame();
            } else {
               this.AfterMatch();
               this.m_bRestartAfterEnd = true;
               if (this.m_bEndBreak) {
                  Match.m_cModule.CallMenuEnd();
               } else {
                  Match.m_cModule.CallSyncMethod("CallMenuEnd");
               }
            }

            if (CoreLogic.m_bDemo) {
               return;
            }
            break;
         case 4:
         case 5:
         case 6:
         case 7:
            this.ResetSpecialBallStab();
      }

      if (bStateStarted) {
         this.SetStateStarted();
      }
   }

   void SendDistanceRequest(AIFootBaller cFBaller, AIFootBaller cSecFB, int nEvent) {
      if (cFBaller.m_cStrategy.IsForwardToPos(cFBaller, cSecFB.GetPosition())
         && (cFBaller.IsActive() || cFBaller.IsGoalKeeper() || cFBaller.IsHelpAttacker() || cFBaller.IsDoingFeint())
         && cFBaller.CanGoalBreak()) {
         this.m_cAISystem.RequestEvent(cFBaller, nEvent, cSecFB);
      }
   }

   void UnblockControlForFocus(Vector vDestPos) {
      for (int t = 0; t < 2; t++) {
         AITeam cTeam = (AITeam)super.m_teams[t];
         if (cTeam.m_bUserControled) {
            FootBaller cFocusFB = cTeam.m_cFocusFBaller;
            if (cFocusFB != null) {
               float fDist = cFocusFB.GetPosition().Distance(vDestPos);
               if (fDist > super.m_cStrategy.m_fMinDistToChangeFocus) {
                  cTeam.BlockControl(false);
               }
            } else {
               cTeam.BlockControl(false);
            }
         }
      }
   }

   SelectionDest GetSelectionDest() {
      return this.cSpecialBallSel;
   }

   int GetShootCount(int nTeam) {
      return super.m_teams[nTeam].GetShootCount();
   }

   void ResetStateStarted() {
      m_bStateStarted = false;
   }

   void ResetStabilizationNew() {
      this.ClearAI(false);
      this.m_bIsStabilization = false;
   }

   void SetAllDirToBall() {
      for (int i = 0; i < 2; i++) {
         for (int j = 0; j < super.m_teams[i].m_vFootBallers.size(); j++) {
            AIFootBaller cFBaller = (AIFootBaller)super.m_teams[i].m_vFootBallers.elementAt(j);
            if (!cFBaller.HasFocus()) {
               cFBaller.m_cStrategy.SetDirToBall(cFBaller);
            }
         }
      }
   }

   void UpdateSpecialBall() {
      if (this.m_bSpecialBallStarted) {
         if (!this.m_bGoneToSpecialBall && this.m_cSpecialFB == null && this.m_test_cLatestSpecialFB != null) {
            this.m_cSpecialFB = this.m_test_cLatestSpecialFB;
            this.m_cSpecialFB.SetSpecialGoal();
            GameObject.CrashLog(" ERROR  m_cSpecialFB == null in UpdateSpecialBall !!!! \n");
         }

         if (!this.m_bGoneToSpecialBall && this.m_cSpecialFB.HasSpecialGoal() && !this.m_cSpecialFB.IsSpecialGoalDone()) {
            Vector vBallDestPos = GameField.m_cBall.GetPosition();
            if (IsMatchState(7)) {
               if (this.m_cSpecialFB.m_nTeam == 0) {
                  vBallDestPos.fZ += 50.0F;
               } else {
                  vBallDestPos.fZ -= 50.0F;
               }
            }

            this.m_cSpecialFB.CheckHotGoal(18, 22, vBallDestPos, this.m_cSpecialFB.HasSpecialGoal());
         } else {
            this.m_bGoneToSpecialBall = true;
         }

         if (IsMatchState(7)) {
            if (!this.m_bDestGoneToSpecialBall && this.m_cSpecialDestFB.HasSpecialGoal() && !this.m_cSpecialDestFB.IsSpecialGoalDone()) {
               this.m_cSpecialDestFB.CheckHotGoal(18, 22, GameField.m_cBall.GetPosition(), this.m_cSpecialDestFB.HasSpecialGoal());
            } else {
               this.m_bDestGoneToSpecialBall = true;
            }
         }

         if (this.m_cSpecialFB != null && this.m_cSpecialFB.HasBall() && this.m_bGoneToSpecialBall && !this.m_bSpecialBallDone) {
            Vector vDest = null;
            switch (GetMatchState()) {
               case 4:
                  vDest = super.m_cStrategy.GetCornerPoint(this.m_nSpecialGoalSide);
                  break;
               case 5:
                  vDest = super.m_cStrategy.GetOutPoint(this.m_nSpecialGoalSide);
                  break;
               case 6:
                  vDest = super.m_cStrategy.GetGoalKeeperOutPos(this.m_nSpecialGoalSide);
                  break;
               case 7:
                  vDest = GameField.cCenter.GetPosition();
                  break;
               default:
                  vDest = this.m_cSpecialFB.GetPosition();
            }

            if (this.m_cSpecialFB.HasGoal(23) && (!this.m_cSpecialFB.HasSpecialGoal() || this.m_cSpecialFB.IsSpecialGoalDone())) {
               this.m_bSpecialBallDone = true;
            } else {
               this.m_cSpecialFB.CheckHotGoal(19, 23, vDest, this.m_cSpecialFB.HasSpecialGoal());
            }
         }

         if (IsMatchState(7) && this.m_bSpecialBallDone && this.m_bDestGoneToSpecialBall && !this.m_bStartGameWhistleDone) {
            this.SignReferee();
            this.m_cSpecialFB.CallMethodIn("PassSpecialBall", 1.5F);
            this.m_bStartGameWhistleDone = true;
         }
      }
   }

   void ShowContexts() {
      if (super.m_teams[0] != null) {
         ((AITeam)super.m_teams[0]).ShowContexts();
      }

      if (super.m_teams[1] != null) {
         ((AITeam)super.m_teams[1]).ShowContexts();
      }
   }

   void ResetSupers() {
      this.GetTeam(0).ResetSuper();
      SoccerModule.GetHud().FlashSuperIconEnd(0);
      this.GetTeam(1).ResetSuper();
      SoccerModule.GetHud().FlashSuperIconEnd(1);
      SoccerModule.GetHud().SwitchSuperOn(super.m_nPlayer1Team);
   }

   boolean IsHalfChanging() {
      return this.m_bHalfChanging;
   }

   void ChangePossesor(FootBaller cFBallerOff, FootBaller cFBallerOn) {
      ((AITeam)this.GetTeam(0)).m_cHelpAttacker = null;
      ((AITeam)this.GetTeam(1)).m_cHelpAttacker = null;
      if (cFBallerOn != null) {
         AIFootBaller cAIFBallerOn = (AIFootBaller)cFBallerOn;
         if (IsMatchState(0) && cAIFBallerOn.HasSpecialGoal()) {
            if (SoccerModule.m_cMatch.m_cSpecialDestFB == cAIFBallerOn) {
               SoccerModule.m_cMatch.m_cSpecialDestFB = null;
            }

            cAIFBallerOn.ResetSpecialGoal();
         }

         if (!this.m_bBallPossesToCount && IsMatchState(0)) {
            this.m_bBallPossesToCount = true;
         }

         ModStats.OnPossession(cAIFBallerOn); // [mod] passes, récupérations, arrêts

         this.m_cActiveLossFB = null;
         super.m_cBallPossesor = cFBallerOn.m_cTeam;
         if (cFBallerOn.m_cTeam.m_bUserControled && cFBallerOn.m_cTeam.m_cFocusFBaller != cFBallerOn && !cAIFBallerOn.HasSpecialGoal()) {
            cFBallerOn.m_cTeam.SetFocus(cFBallerOn, false);
         }
      } else {
         super.m_cBallPossesor = null;
      }

      this.RefreshContext();
      if (cFBallerOff != null && cFBallerOn == null) {
      }
   }

   boolean IsBallPossesor() {
      return SoccerModule.m_cMatch.m_cBallPossesor != null;
   }

   void SetThink() {
      for (int t = 0; t < 2; t++) {
         ((AITeam)super.m_teams[t]).SetThink();
      }
   }

   Vector GetSelectionDestPos() {
      return this.cSpecialBallSel.GetPosition();
   }

   void RestoreSpecialBallPositions() {
      for (int i = 0; i < 2; i++) {
         Team team = super.m_teams[i];

         for (int j = 0; j < team.GetFootBallerCount(); j++) {
            AIFootBaller fb = (AIFootBaller)team.GetFootBaller(j);
            fb.SetPosition(fb.m_vValidPos);
            super.m_cStrategy.SetDirToBall(fb);
         }
      }

      super.m_cReferee.SetPosition(super.m_cReferee.m_vValidPos);
      super.m_cStrategy.SetDirToBall(super.m_cReferee);
      if (IsMatchState(5) || IsMatchState(4)) {
         AITeam cTeam;
         if (this.m_nTeamToStart == 0) {
            cTeam = (AITeam)super.m_teams[0];
         } else {
            cTeam = (AITeam)super.m_teams[1];
         }

         this.m_cSpecialFB = cTeam.FindNearest();
         Vector vPos = super.m_cStrategy.GetSpecialBallFBPos(this.m_cSpecialFB, this.m_nSpecialGoalSide);
         this.m_cSpecialFB.SetPosition(vPos);
         GameObject.LogLine(" special ball restore , pos: " + vPos);
         Vector vDir = GameField.cCenter.GetPosition();
         vDir.Sub(this.m_cSpecialFB.GetPosition());
         vDir.Normalize();
         this.m_cSpecialFB.SetDir(vDir);
      }
   }

   void FixSelectionDestSystem(AITeam cTeam) {
      this.cSpecialBallSel.m_cTeam = cTeam;
      this.cSpecialBallSel.SetActive(true);
      GameField.m_cDestMark.Hide();
      Match.m_cModule.GetSoccerCamera().SetObserved(this.cSpecialBallSel);
   }

   void SetWaiting() {
      this.m_bWaiting = true;
   }

   boolean IsWaiting() {
      return this.m_bWaiting;
   }

   void FixControledTeamSide() {
      AITeam cTeamLeft = (AITeam)super.m_teams[0];
      AITeam cTeamRight = (AITeam)super.m_teams[1];
      if (!CoreLogic.m_bMultiplayer) {
         if (cTeamLeft.m_bUserControled) {
            super.m_nPlayer1Team = 0;
            super.m_nPlayer2Team = 1;
         } else {
            super.m_nPlayer1Team = 1;
            super.m_nPlayer2Team = 0;
         }
      } else if (super.m_nHalfNo == 0) {
         super.m_nPlayer1Team = 0;
         super.m_nPlayer2Team = 1;
      } else {
         super.m_nPlayer1Team = 1;
         super.m_nPlayer2Team = 0;
      }

      SoccerCamera cam = Match.m_cModule.GetSoccerCamera();
      cam.ChangeMode(cam.m_nMode);
   }

   void SetSpaceActive(boolean bActive) {
      this.m_bSpaceCanBePressed = bActive;
      if (bActive) {
         this.m_bWasSpacePressed = false;
      }
   }

   boolean IsSpaceActive() {
      return this.m_bSpaceCanBePressed;
   }

   void PlaySuperSounds() {
      if (GameField.m_cBall.IsSuperShot() && GameField.m_cBall.m_nSuperShotSound != -1) {
         GameField.m_cBall.PlaySound(GameField.m_cBall.m_nSuperShotSound);
      }

      AITeam cTeamLeft = (AITeam)this.GetTeam(0);
      AITeam cTeamRight = (AITeam)this.GetTeam(1);
      if (cTeamLeft.IsSuperWall() && cTeamLeft.m_cSuperWall.m_nSuperDefenceSound != -1) {
         cTeamLeft.m_cSuperWall.PlaySound(cTeamLeft.m_cSuperWall.m_nSuperDefenceSound);
      }

      if (cTeamRight.IsSuperWall() && cTeamRight.m_cSuperWall.m_nSuperDefenceSound != -1) {
         cTeamRight.m_cSuperWall.PlaySound(cTeamRight.m_cSuperWall.m_nSuperDefenceSound);
      }
   }

   void RestartHalfRaw() {
      if (!this.m_bTeamChangedSides) {
         super.m_nHalfNo = 1;
         this.ChangeTeamSides();
      }

      this.m_bHalfChanging = false;
      this.m_bMatchEnding = false;
      if (this.m_nTeamStartedFirstHalf == 0) {
         this.RestartGameRaw(0);
      } else {
         this.RestartGameRaw(1);
      }
   }

   void PrepareTeams() {
      GameField.SetDestPos(GameField.m_cBall.GetPosition(), false);
      AITeam cTeam = (AITeam)super.m_teams[0];
      AIFootBaller cActive = cTeam.FindNearest();
      cTeam.SetActive(cActive);
      cTeam = (AITeam)super.m_teams[1];
      cActive = cTeam.FindNearest();
      cTeam.SetActive(cActive);
   }

   void StartSpecialBall(int nTeam) {
      this.m_bSpecialBallStarted = true;
      GameField.m_cBall.SetSpecialBall();
      GameField.m_cBall.TakeBallEasy();
      if (IsMatchState(7)) {
         GameField.m_cBall.SetPosition(GameField.cCenter.GetPosition());
      } else if (IsMatchState(6)) {
         GameField.m_cBall.SetPosition(super.m_cStrategy.GetGoalKeeperOutPos(this.m_nSpecialGoalSide));
      } else {
         GameField.m_cBall.SetPosition(super.m_cStrategy.GetSpecialBallPos(this.m_nSpecialGoalSide));
      }

      GameField.m_cBall.ResetPhysics();
      GameField.SetDestPos(GameField.m_cBall.GetPosition(), false);
      super.m_cStrategy.SetDirToCenter(super.m_cReferee);
      if (nTeam == -1) {
         nTeam = 0;
      }

      AITeam cTeam;
      if (nTeam == 0) {
         cTeam = (AITeam)super.m_teams[0];
      } else {
         cTeam = (AITeam)super.m_teams[1];
      }

      if (IsMatchState(6)) {
         this.m_cSpecialFB = (AIFootBaller)cTeam.m_cGoalKeeper;
      } else {
         this.m_cSpecialFB = cTeam.FindNearest();
      }

      GameObject.LogLine(" wybrany do special ball " + this.m_cSpecialFB);
      this.m_test_cLatestSpecialFB = this.m_cSpecialFB;
      if (IsMatchState(7)) {
         this.m_cSpecialDestFB = cTeam.FindNearestExclude(this.m_cSpecialFB);
         GameObject.LogLine(" wybrany jako dest special ball " + this.m_cSpecialDestFB);
      }

      this.m_cSpecialFB.ClearAI(false);
      this.m_cSpecialFB.SetSpecialGoal();
      if (IsMatchState(7)) {
         this.m_cSpecialDestFB.ClearAI(false);
         this.m_cSpecialDestFB.SetSpecialGoal();
      }

      this.m_bGoneToSpecialBall = false;
      this.m_bDestGoneToSpecialBall = false;
      this.m_bSpecialBallDone = false;
      this.m_bStartGameWhistleDone = false;
      Vector vBallDestPos = GameField.m_cBall.GetPosition();
      if (IsMatchState(7)) {
         if (this.m_cSpecialFB.m_nTeam == 0) {
            vBallDestPos.fZ += 50.0F;
         } else {
            vBallDestPos.fZ -= 50.0F;
         }
      }

      this.m_cAISystem.RequestEvent(this.m_cSpecialFB, 18, vBallDestPos);
      if (IsMatchState(7)) {
         this.m_cAISystem.RequestEvent(this.m_cSpecialDestFB, 18, super.m_cStrategy.GetStartGameDestPos(this.m_cSpecialDestFB));
      }
   }

   float GetEntirePossesion(int nTeam) {
      float fLeftTime = super.m_teams[0].GetEntirePossesion();
      float fRightTime = super.m_teams[1].GetEntirePossesion();
      float fEntireTime = fLeftTime + fRightTime;
      if (fEntireTime <= 0.0F) {
         return 0.0F;
      } else {
         return nTeam == 0 ? fLeftTime / fEntireTime * 100.0F : fRightTime / fEntireTime * 100.0F;
      }
   }

   boolean AreFight(AIFootBaller cFirstFB, AIFootBaller cSecFB) {
      if (cFirstFB.IsFight()) {
         return true;
      } else if (cSecFB.IsFight()) {
         return true;
      } else {
         return (!cFirstFB.IsDoingFeint() || cFirstFB.HasGoal(12)) && (!cSecFB.IsDoingFeint() || cSecFB.HasGoal(12))
            ? cFirstFB.IsKickMark() || cSecFB.IsKickMark()
            : true;
      }
   }

   void WrtiteStateAsCrashLog() {
      GameObject.CrashLog(
         " Match:  \n - state "
            + GetMatchState()
            + " \n"
            + " - m_bIsStabilization "
            + this.m_bIsStabilization
            + " \n"
            + " - m_bWaiting "
            + this.m_bWaiting
            + " \n"
            + " - m_bSpecialBallStab "
            + this.m_bSpecialBallStab
            + " \n"
            + " - m_bIsGoalReplay "
            + this.m_bIsGoalReplay
            + " \n"
            + " - m_bLastOutIsShoot "
            + this.m_bLastOutIsShoot
            + " \n"
            + " - m_bRestartAfterEnd "
            + this.m_bRestartAfterEnd
            + " \n"
            + " - m_bEndBreak "
            + this.m_bEndBreak
            + " \n"
            + " - m_cSpecialFB "
            + this.m_cSpecialFB
            + " \n"
            + " - m_test_cLatestSpecialFB "
            + this.m_test_cLatestSpecialFB
            + " \n"
            + " - m_cSpecialDestFB "
            + this.m_cSpecialDestFB
            + " \n"
            + " - m_cSpecialFBRight "
            + this.m_cSpecialFBRight
            + " - m_cSpecialFBLeft "
            + this.m_cSpecialFBLeft
            + " - m_cActiveLossFB "
            + this.m_cActiveLossFB
            + " \n"
            + " - m_bGoneToSpecialBall "
            + this.m_bGoneToSpecialBall
            + " \n"
            + " - m_bDestGoneToSpecialBall "
            + this.m_bDestGoneToSpecialBall
            + " \n"
            + " - m_bSpecialBallDone "
            + this.m_bSpecialBallDone
            + " \n"
            + " - m_bSpecialBallStarted "
            + this.m_bSpecialBallStarted
            + " \n"
            + " - m_bHalfChanging "
            + this.m_bHalfChanging
            + " \n"
            + " - m_bWasHalfMenu "
            + this.m_bWasHalfMenu
            + " \n"
            + " - m_bSpaceCanBePressed "
            + this.m_bSpaceCanBePressed
            + " \n"
            + " - m_bWasSpacePressed "
            + this.m_bWasSpacePressed
            + " \n"
            + " - m_bGoalReact "
            + this.m_bGoalReact
            + " \n"
            + " - m_bTeamChangedSides "
            + this.m_bTeamChangedSides
            + " \n"
            + " - m_bGKOut "
            + this.m_bGKOut
            + " \n"
            + " - m_bGameStarted "
            + this.m_bGameStarted
            + " \n"
            + " - m_nSpecialGoalSide "
            + this.m_nSpecialGoalSide
            + " \n"
            + " - m_nTeamToStart "
            + this.m_nTeamToStart
            + " \n"
            + " - m_nTeamStartedFirstHalf "
            + this.m_nTeamStartedFirstHalf
            + " \n"
            + " - m_sDB_SpecialBallPoint "
            + this.m_sDB_SpecialBallPoint
            + " \n"
            + " - BALL_pos "
            + GameField.m_cBall.GetPosition()
            + " \n"
            + " - m_nHalfNo "
            + super.m_nHalfNo
            + " - m_bGoldGoal "
            + super.m_bGoldGoal
            + " camera -> "
            + Match.m_cModule.GetSoccerCamera().m_Observed
            + " \n"
      );
      GameField.m_cBall.WrtiteStateAsCrashLog();
      if (this.cSpecialBallSel != null) {
         this.cSpecialBallSel.WrtiteStateAsCrashLog();
      } else {
         GameObject.CrashLog("cSpecialBallSel == null \n");
      }

      GameObject.CrashLog(" TEAM LEFT \n");
      ((AITeam)this.GetTeam(0)).WrtiteStateAsCrashLog();
      GameObject.CrashLog(" TEAM RIGHT \n");
      ((AITeam)this.GetTeam(1)).WrtiteStateAsCrashLog();
      GameObject.CrashLog(" FBallers of  LEFT \n");
      ((AITeam)this.GetTeam(0)).WrtiteStateAsCrashLogFBallers();
      GameObject.CrashLog(" FBallers of  RIGHT \n");
      ((AITeam)this.GetTeam(1)).WrtiteStateAsCrashLogFBallers();
      super.m_cReferee.WrtiteStateAsCrashLog();
   }

   void SetBallForReferee() {
      Vector vPos = super.m_cReferee.cSpawnPointPresentation.GetPosition();
      Vector vOff = new Vector(GameField._DIR_S);
      vOff.Normalize();
      vOff.Mul(100.0F);
      Vector vNewPos = new Vector();
      vOff.Add(vPos, vNewPos);
      GameField.m_cBall.SetPosition(vNewPos);
   }

   void SideLottery() {
      float fRnd = GameObject.RandomFloat(100.0F);
      if (fRnd < 50.0F) {
         this.m_nTeamStartedFirstHalf = 0;
      } else {
         this.m_nTeamStartedFirstHalf = 1;
      }
   }

   int GetSuperCount(int nTeam) {
      return super.m_teams[nTeam].GetSuperCounter();
   }

   void RestartGameRaw(int nSide) {
      this.ResetMatchParams();
      this.RestartControllers();
      this.RestoreGamePositions(true);
      ModTactics.PlaceKickoff(this); // [mod] placement de coup d'envoi choisi dans la carte tactique
      this.ClearAI(true);
      this.m_bGoalReact = false;
      this.RestartGame(nSide);
   }

   void ResetMatchParams() {
      this.ClearSelectionDestSystem();
      Match.m_cModule.DisableCallMethod("OnGoal");
      Match.m_cModule.DisableCallMethod("StartGameDelay");
      if (this.m_cSpecialFB != null) {
         this.m_cSpecialFB.DisableCallMethod("PassSpecialBall");
      }

      AITeam cTeamLeft = (AITeam)super.m_teams[0];
      AITeam cTeamRight = (AITeam)super.m_teams[1];
      if (GameField.m_cDestMark.IsVisible()) {
         GameField.m_cDestMark.Hide();
      }

      if (cTeamLeft.IsSuperWall()) {
         cTeamLeft.ResetSuperWall();
      }

      cTeamLeft.ResetRequestSuperDefence();
      if (cTeamRight.IsSuperWall()) {
         cTeamRight.ResetSuperWall();
      }

      cTeamRight.ResetRequestSuperDefence();
      this.m_bSpecialBallStab = false;
   }

   String ChooseWeatherComment() {
      int nLevel = Match.m_cModule.m_nLevelID;
      String sCommentWeather = "";
      Commentator comm = SoccerGame.game.GetCommentator();
      if (super.m_Weather.m_Disaster != null && super.m_Weather.m_Disaster.m_DisasterKind != 0) {
         switch (super.m_Weather.m_Disaster.m_DisasterKind) {
            case 0:
               break;
            case 1:
            case 2:
            case 3:
               int var6 = comm.Toss(0, 1);
               if (var6 == 0) {
                  sCommentWeather = "data/sounds/commentary/" + "287.wav";
               } else {
                  sCommentWeather = "data/sounds/commentary/" + "288.wav";
               }
               break;
            case 4:
               sCommentWeather = "data/sounds/commentary/" + "284.wav";
               break;
            default:
               sCommentWeather = "data/sounds/commentary/" + "286.wav";
         }
      } else {
         switch (nLevel) {
            case 0:
               int nTypex = comm.Toss(0, 1);
               if (nTypex == 0) {
                  sCommentWeather = "data/sounds/commentary/" + "286.wav";
               } else {
                  sCommentWeather = "data/sounds/commentary/" + "284.wav";
               }
               break;
            case 1:
            case 2:
            case 3:
            case 4:
            case 9:
            case 10:
            case 11:
               sCommentWeather = "data/sounds/commentary/" + "286.wav";
               break;
            case 5:
            case 6:
            case 7:
            case 8:
               int nType = comm.Toss(0, 1);
               if (nType == 0) {
                  sCommentWeather = "data/sounds/commentary/" + "285.wav";
               } else {
                  sCommentWeather = "data/sounds/commentary/" + "284.wav";
               }
               break;
            default:
               sCommentWeather = "data/sounds/commentary/" + "286.wav";
         }
      }

      return sCommentWeather;
   }

   void SignReferee() {
      GameField.m_cBall.PlaySound("Whistle.wav");
   }

   void ClearSendHalfCamera() {
      Match.m_cModule.GetSoccerCamera().ClearEndHalfCamera();
      Match.m_cModule.GetSoccerCamera().SetObserved(GameField.m_cBall);
   }

   void ClearAI(boolean bClearSupers) {
      for (int t = 0; t < 2; t++) {
         super.m_teams[t].SetActive(null);
         if (super.m_teams[t].m_bUserControled) {
            super.m_teams[t].SetFocus(null, false);
         }

         for (int i = 0; i < super.m_teams[t].m_vFootBallers.size(); i++) {
            AIFootBaller cFBaller = (AIFootBaller)super.m_teams[t].m_vFootBallers.elementAt(i);
            cFBaller.ClearAI(bClearSupers);
         }
      }

      super.m_cReferee.ClearAI(bClearSupers);
      if (bClearSupers) {
         ModSupers.Reset(); // [mod] fin des gel / aimant / ballon fou / tir double en cours
      }
   }

   void EndHalf() {
      this.ResetActiveSupers();
      this.RestartHalf();
   }

   void OnReplayStop() {
      super.OnReplayStop();
      if (!this.m_bJustRestart) {
         if (this.m_bIsGoalReplay) {
            if (this.m_bWasSpacePressed) {
               Match.m_cModule.CallBreakPauseAfterGoal();
            } else {
               Match.m_cModule.CallSyncMethod("CallBreakPauseAfterGoal");
            }
         } else if (this.m_bWasSpacePressed) {
            Match.m_cModule.CallBreakStab();
         } else {
            Match.m_cModule.CallSyncMethod("CallBreakStab");
         }
      }
   }

   void WrtiteStateAsLog() {
      GameObject.LogD(" Match:  \n");
      GameObject.LogD(" - state " + GetMatchState() + " \n");
      GameObject.LogD(" - m_bIsStabilization " + this.m_bIsStabilization + " \n");
      GameObject.LogD(" - m_bWaiting " + this.m_bWaiting + " \n");
      GameObject.LogD(" - m_bSpecialBallStab " + this.m_bSpecialBallStab + " \n");
      GameObject.LogD(" - m_bIsGoalReplay " + this.m_bIsGoalReplay + " \n");
      GameObject.LogD(" - m_bLastOutIsShoot " + this.m_bLastOutIsShoot + " \n");
      GameObject.LogD(" - m_bRestartAfterEnd " + this.m_bRestartAfterEnd + " \n");
      GameObject.LogD(" - m_bEndBreak " + this.m_bEndBreak + " \n");
      GameObject.LogD(" - m_cSpecialFB " + this.m_cSpecialFB + " \n");
      GameObject.LogD(" - m_cSpecialDestFB " + this.m_cSpecialDestFB + " \n");
      GameObject.LogD(" - m_cActiveLossFB " + this.m_cActiveLossFB + " \n");
      GameObject.LogD(" - m_bGoneToSpecialBall " + this.m_bGoneToSpecialBall + " \n");
      GameObject.LogD(" - m_bDestGoneToSpecialBall " + this.m_bDestGoneToSpecialBall + " \n");
      GameObject.LogD(" - m_bSpecialBallDone " + this.m_bSpecialBallDone + " \n");
      GameObject.LogD(" - m_bSpecialBallStarted " + this.m_bSpecialBallStarted + " \n");
      GameObject.LogD(" - m_bHalfChanging " + this.m_bHalfChanging + " \n");
      GameObject.LogD(" - m_bWasHalfMenu " + this.m_bWasHalfMenu + " \n");
      GameObject.LogD(" - m_bSpaceCanBePressed " + this.m_bSpaceCanBePressed + " \n");
      GameObject.LogD(" - m_bWasSpacePressed " + this.m_bWasSpacePressed + " \n");
      GameObject.LogD(" - m_bGoalReact " + this.m_bGoalReact + " \n");
      GameObject.LogD(" - m_bTeamChangedSides " + this.m_bTeamChangedSides + " \n");
      GameObject.LogD(" - m_bGKOut " + this.m_bGKOut + " \n");
      GameObject.LogD(" - m_bGameStarted " + this.m_bGameStarted + " \n");
      GameObject.LogD(" - m_nSpecialGoalSide " + this.m_nSpecialGoalSide + " \n");
      GameObject.LogD(" - m_nTeamToStart " + this.m_nTeamToStart + " \n");
      GameObject.LogD(" - m_nTeamStartedFirstHalf " + this.m_nTeamStartedFirstHalf + " \n");
      GameField.m_cBall.WrtiteStateAsLog();
      if (this.cSpecialBallSel != null) {
         this.cSpecialBallSel.WrtiteStateAsLog();
      } else {
         GameObject.LogD("cSpecialBallSel == null \n");
      }

      GameObject.LogD(" TEAM LEFT \n");
      ((AITeam)this.GetTeam(0)).WrtiteStateAsLog();
      GameObject.LogD(" TEAM RIGHT \n");
      ((AITeam)this.GetTeam(1)).WrtiteStateAsLog();
      GameObject.LogD(" FBallers of  LEFT \n");
      ((AITeam)this.GetTeam(0)).WrtiteStateAsLogFBallers();
      GameObject.LogD(" FBallers of  RIGHT \n");
      ((AITeam)this.GetTeam(1)).WrtiteStateAsLogFBallers();
      super.m_cReferee.WrtiteStateAsLog();
   }

   void SetSpecialBallStab() {
      this.m_sDB_SpecialBallPoint = " SetSpecialBallStab ";
      this.m_bSpecialBallStab = true;
      Match.m_cModule.CallMethodIn("BreakSpecialBallStab", super.m_cStrategy.m_fBreakSpecialBallTime);
   }

   void OnGoal(int nTeam, boolean bSuperShot) {
      GameObject.LogLine(" On Goal ----------------");
      ModStats.OnGoal(this, nTeam); // [mod] buteur et minute
      if ((nTeam != 0 || super.m_nHalfNo != 0) && (nTeam != 1 || super.m_nHalfNo != 1 || super.m_bGoldGoal) && (nTeam != 0 || !super.m_bGoldGoal)) {
         super.m_nScoreRight++;
      } else {
         super.m_nScoreLeft++;
      }

      if (!bSuperShot) {
         this.GetTeam(nTeam).IncreaseSuperFillCount();
      }

      Match.m_cModule.m_nTeamLastScored = nTeam;
      Team cTeamLeft = this.GetTeam(0);
      Team cTeamRight = this.GetTeam(1);
      this.ResetActiveSupers();
      cTeamLeft.ClearActiveAndFocus();
      cTeamRight.ClearActiveAndFocus();
      SoccerModule.m_cHud.ShowEventInfo(0);
      Match.m_cModule.CallMethodIn("CallCommentGoal", 1.0F);
      this.SignReferee();
      Match.m_cModule.m_Clock.Stop();
      this.m_bGoalReact = true;
      float fAnimTime = this.PlayOnGoalReaction(nTeam);
      if (bSuperShot) {
         fAnimTime *= GameField.m_cBall.m_fSuperShotSpeedup;
      }

      this.SetWaiting();
      if (CoreLogic.GetPlayer().m_bReplaysOn) {
         Match.m_cModule.CallMethodIn("CallStopTimForReplay", 1.0F);
      }

      Match.m_cModule.CallMethodIn("OnGoal", fAnimTime + 1.0F);
      this.SetSpaceActive(true);
      SoccerModule.GetHud().ShowInfo();
   }

   void ResetActiveSupers() {
      if (GameField.m_cBall.IsSuperShot()) {
         GameField.m_cBall.ResetSuperShot();
      }

      ((AITeam)this.GetTeam(0)).ResetActiveSupers();
      ((AITeam)this.GetTeam(1)).ResetActiveSupers();
   }

   void ClearThink() {
      for (int t = 0; t < 2; t++) {
         super.m_teams[t].SetActive(null);
         if (super.m_teams[t].m_bUserControled) {
            super.m_teams[t].SetFocus(null, false);
         }

         ((AITeam)super.m_teams[t]).ClearThink();
      }
   }

   void StartGameDirectly() {
      SetMatchState(0);
      GameField.m_cBall.ResetEventFlags();
      SoccerModule.m_cMatch.ResetWaiting();
      SoccerModule.m_cMatch.SetStateStarted();
      Match.m_cModule.m_Clock.Start();
      if (CoreLogic.GetPlayer().m_bReplaysOn) {
         Recorder.StartClock();
      }
   }

   void ClearSelectionDestSystem() {
      this.cSpecialBallSel.SetActive(false);
      Match.m_cModule.GetSoccerCamera().SetObserved(GameField.m_cBall);
      this.cSpecialBallSel.m_cTeam = null;
   }

   void ResetWaiting() {
      this.m_bWaiting = false;
   }

   void CallRefreshContext() {
      this.RefreshContext();
   }

   void CheckDistance(AIFootBaller cFirstFB, AIFootBaller cSecFB, float fDistance) {
      if (!this.AreFight(cFirstFB, cSecFB) && this.CanBreak(cFirstFB, cSecFB)) {
         if ((!cFirstFB.IsActive() && !cFirstFB.IsHelpAttacker() || !cSecFB.IsActive() && !cSecFB.IsHelpAttacker())
            && !cFirstFB.IsDoingFeint()
            && !cSecFB.IsDoingFeint()) {
            if (fDistance < super.m_cStrategy.m_fMinDistFBallers && cFirstFB.m_cTeam == cSecFB.m_cTeam) {
            }
         } else {
            float fScope = Strategy.GetActiveDistance(cFirstFB, cSecFB);
            if (fDistance < fScope && cFirstFB.m_cTeam != cSecFB.m_cTeam && (cFirstFB.HasBall() || cSecFB.HasBall())) {
               if (!cFirstFB.IsUserControled()
                  || cFirstFB.HasGoal(12) && cFirstFB.GoalUnreached()
                  || Strategy.m_bAutoTakeBall && this.BothInFront(cFirstFB, cSecFB) && cSecFB.HasBall()) {
                  cFirstFB.m_bTakingBallFromKey = false;
                  this.SendDistanceRequest(cFirstFB, cSecFB, 6);
               }

               if (!cSecFB.IsUserControled()
                  || cSecFB.HasGoal(12) && cSecFB.GoalUnreached()
                  || Strategy.m_bAutoTakeBall && this.BothInFront(cSecFB, cFirstFB) && cFirstFB.HasBall()) {
                  cSecFB.m_bTakingBallFromKey = false;
                  this.SendDistanceRequest(cSecFB, cFirstFB, 6);
               }

               return;
            }

            if (fDistance < super.m_cStrategy.m_fMinDistFBallers && cFirstFB.m_cTeam != cSecFB.m_cTeam) {
               if (cFirstFB.HasBall() && (!cFirstFB.IsUserControled() || cFirstFB.IsSuperFeint())) {
                  this.SendDistanceRequest(cFirstFB, cSecFB, 0);
               }

               if (cSecFB.HasBall() && (!cSecFB.IsUserControled() || cSecFB.IsSuperFeint())) {
                  this.SendDistanceRequest(cSecFB, cFirstFB, 0);
               }
            }
         }
      }
   }

   static void SetMatchState(int nState) {
      m_nMatchState = nState;
      m_bStateStarted = false;
      GameObject.LogLine(" set macth state " + nState);
   }

   void CheckDistance_GK(AIFootBaller cGK, AIFootBaller cFBaller, float fDistance) {
      if (!this.AreFight(cGK, cFBaller) && this.CanBreak(cGK, cFBaller)) {
         float fScope = Strategy.GetActiveDistance(cGK, cFBaller);
         if (fDistance < fScope && cGK.m_cTeam != cFBaller.m_cTeam && cFBaller.HasBall()) {
            cGK.m_bTakingBallFromKey = false;
            this.SendDistanceRequest(cGK, cFBaller, 6);
         }
      }
   }

   static boolean IsMatchState(int nState) {
      return m_nMatchState == nState;
   }

   void InitSpecialBallSel() {
      this.cSpecialBallSel = (SelectionDest)Match.m_cModule.CreateObject("SelectionDest");
      Match.m_cModule.AddControlObject(this.cSpecialBallSel);
      this.cSpecialBallSel.Init(Match.m_cModule);
   }

   void ResetStatistics() {
      ModStats.Reset(); // [mod] statistiques de match
      if (super.m_teams[0] != null) {
         super.m_teams[0].ResetEntirePossesion();
         super.m_teams[0].ResetSuperCounter();
         super.m_teams[0].ResetShootCounter();
      }

      if (super.m_teams[1] != null) {
         super.m_teams[1].ResetEntirePossesion();
         super.m_teams[1].ResetSuperCounter();
         super.m_teams[1].ResetShootCounter();
      }
   }

   void EndGame() {
      this.ResetMatchParams();
      this.SignReferee();
      Match.m_cModule.m_Clock.Stop();
      Team cTeamLeft = this.GetTeam(0);
      Team cTeamRight = this.GetTeam(1);
      cTeamLeft.ClearActiveAndFocus();
      cTeamRight.ClearActiveAndFocus();
      this.m_bHalfChanging = false;
      this.m_bMatchEnding = true;
      GameObject.LogLine(" !!!!!!!!END_GAME!!!11111! ");
      this.ResetActiveSupers();
      this.SetWaiting();
      if (super.m_nScoreLeft != super.m_nScoreRight) {
         Match.m_cModule.CallMethodIn("OnMatchEnd", 3.0F);
      } else {
         Match.m_cModule.OnMatchEnd();
      }

      GameField.m_cBall.TakeBallEasy();
      this.SetSpaceActive(true);
   }

   void UpdateGame() {
      if (PROFILE_UpdateGame == 0) {
         PROFILE_UpdateGame = Match.m_cModule.ProfileAdd("Soccer", "UpdateGame");
      }

      Match.m_cModule.ProfileBegin(PROFILE_UpdateGame);
      Commentator cComm = SoccerGame.game.GetCommentator();
      if (!this.m_bTeamFirstCommented && Match.m_cModule.m_Clock.GetMatchTimeF() > this.m_fTeamFirstCommentTime && !cComm.IsTalking()) {
         cComm.CommentTeam(EAnimals.GetCommentAnimal(this.GetTeam(super.m_nPlayer1Team).m_nAnimalType)); // [mod] nouvelles equipes
         this.m_bTeamFirstCommented = true;
      }

      if (!this.m_bTeamSecCommented && Match.m_cModule.m_Clock.GetMatchTimeF() > this.m_fTeamSecCommentTime && !cComm.IsTalking()) {
         cComm.CommentTeam(EAnimals.GetCommentAnimal(this.GetTeam(super.m_nPlayer2Team).m_nAnimalType)); // [mod]
         this.m_bTeamSecCommented = true;
      }

      if (!this.m_bAnythigCommented && Match.m_cModule.m_Clock.GetMatchTimeF() > this.m_fAnythigCommentTime && !cComm.IsTalking()) {
         cComm.CommentFun();
         this.m_bAnythigCommented = true;
      }

      if (!this.m_bWeatherCommented && Match.m_cModule.m_Clock.GetMatchTimeF() > this.m_fWeatherCommentTime && !cComm.IsTalking()) {
         cComm.CommentWeather(this.ChooseWeatherComment());
         this.m_bWeatherCommented = true;
      }

      if ((this.GetTeam(0).m_bSuperDoing || this.GetTeam(1).m_bSuperDoing) && !this.m_bWasSuperCommeneted && !cComm.IsTalking()) {
         cComm.CommentSuperAction(this.m_bSuperCommentWithBaller);
         this.m_bWasSuperCommeneted = true;
      }

      if (!GameField.m_cBall.IsDestReached()) {
         GameField.m_cBall.CheckDestReached();
      }

      if (GameField.m_cDestMark.IsVisible() && GameField.m_cBall.IsDestReached()) {
         GameField.m_cDestMark.Hide();
      }

      Vector vBallPos = GameField.m_cBall.GetPosition();
      GameField.m_cBall.SetFlag(8);
      if (GameField.m_cBall.GetOwner() == null || !GameField.m_cBall.GetOwner().IsGoalKeeper()) {
         if (!GameField.m_cBall.IsMoving()
            && !vBallPos.IsEqual(GameField.GetDestPos())
            && GameField.GetZoneForPointVertical(vBallPos) != 6
            && GameField.GetZoneForPointVertical(vBallPos) != 5
            && GameField.GetZoneForPoint(vBallPos, 0) != 3
            && GameField.GetZoneForPoint(vBallPos, 0) != 4
            && GameField.m_cBall.IsDestReached()) {
            GameField.SetDestPos(vBallPos, false);
         }

         if (GameField.m_cBall.IsMoving() && (GameField.m_cBall.m_bHasOwner || GameField.m_cBall.IsDestReached())) {
            GameField.SetDestPos(vBallPos, false);
            this.UpdateOrder();
         }
      }

      if (GameField.m_cDestMark.IsVisible() && GameField.m_cBall.IsDestReached()) {
         GameField.m_cDestMark.Hide();
      }

      this.m_fDistUpdateLastTime = SoccerGame.GetSoccerModule().GetTime();

      for (int i = 0; i < 2; i++) {
         for (int j = 0; j < super.m_teams[i].m_vFootBallers.size(); j++) {
            AIFootBaller cFBaller = (AIFootBaller)super.m_teams[i].m_vFootBallers.elementAt(j);
            int nZone = cFBaller.GetZone();
            int nZoneVertical = GameField.GetZoneForPointVertical(cFBaller.GetPositionVectorVolatile());
            if (nZone != 3 && nZone != 4 && nZoneVertical != 6 && nZoneVertical != 5) {
               cFBaller.m_bOutside = false;
               if (cFBaller == this.m_cLastSpecialFB) {
                  this.m_cLastSpecialFB = null;
               }
            } else {
               cFBaller.m_bOutside = true;
               if (!cFBaller.IsStabilization() && cFBaller != this.m_cLastSpecialFB) {
                  this.m_cAISystem.RequestEvent(cFBaller, 3, cFBaller.GetPosition());
               }
            }
         }
      }

      int nCountLeft = super.m_teams[0].m_vFootBallers.size();
      int nCountRight = super.m_teams[0].m_vFootBallers.size();
      int nEntireCount = nCountLeft + nCountRight;

      for (int i = 1; i < nEntireCount; i++) {
         for (int j = 0; j < nEntireCount; j++) {
            int nFB1 = j;
            int nFB2 = j + i;
            if (nFB2 < nEntireCount) {
               AIFootBaller cFB1;
               if (nFB1 >= nCountLeft) {
                  cFB1 = (AIFootBaller)super.m_teams[1].m_vFootBallers.elementAt(nFB1 - nCountLeft);
               } else {
                  cFB1 = (AIFootBaller)super.m_teams[0].m_vFootBallers.elementAt(nFB1);
               }

               AIFootBaller cFB2;
               if (nFB2 >= nCountLeft) {
                  cFB2 = (AIFootBaller)super.m_teams[1].m_vFootBallers.elementAt(nFB2 - nCountLeft);
               } else {
                  cFB2 = (AIFootBaller)super.m_teams[0].m_vFootBallers.elementAt(nFB2);
               }

               if (Math.abs(cFB1.GetPositionVectorVolatile().fZ - cFB2.GetPositionVectorVolatile().fZ) < super.m_cStrategy.m_fMinDistFBallers + 50.0F) {
                  float fDistance = cFB1.GetDistanceTo(cFB2);
                  if (cFB1.IsGoalKeeper()) {
                     this.CheckDistance_GK(cFB1, cFB2, fDistance);
                  } else if (cFB2.IsGoalKeeper()) {
                     this.CheckDistance_GK(cFB2, cFB1, fDistance);
                  } else {
                     this.CheckDistance(cFB1, cFB2, fDistance);
                  }
               }
            }
         }
      }

      Match.m_cModule.ProfileEnd(PROFILE_UpdateGame);
   }

   void OnCorner(FootBaller cFB, int nSide) {
      this.SignReferee();
      Match.m_cModule.m_Clock.Stop();
      this.m_bGKOut = false;
      if (nSide == 2 && cFB.m_nTeam == 1) {
         this.m_bGKOut = true;
      }

      if (nSide == 3 && cFB.m_nTeam == 0) {
         this.m_bGKOut = true;
      }

      this.m_bLastOutIsShoot = false;
      if (this.m_bGKOut) {
         SoccerModule.m_cHud.ShowEventInfo(1);
         if (GameField.m_cBall.IsShooted()) {
            this.m_bLastOutIsShoot = true;
            if (CoreLogic.GetPlayer().m_bReplaysOn) {
               Match.m_cModule.CallMethodIn("CallStopTimForReplay", 1.0F);
            }

            if (SoccerGame.game.GetCommentator().TossHalf(70.0F)) {
               SoccerGame.game.GetCommentator().CommentMissedShot(EAnimals.GetCommentAnimal(cFB.m_nAnimalType)); // [mod]
            } else {
               SoccerGame.game.GetCommentator().CommentOut();
            }
         } else {
            SoccerGame.game.GetCommentator().CommentOut();
         }
      } else {
         SoccerModule.m_cHud.ShowEventInfo(2);
         SoccerGame.game.GetCommentator().CommentCorner();
      }

      if (this.m_bGKOut) {
         SetMatchState(6);
      } else {
         SetMatchState(4);
      }

      this.m_nSpecialGoalSide = nSide;
      if (cFB.m_nTeam == 0) {
         this.m_nTeamToStart = 1;
      } else {
         this.m_nTeamToStart = 0;
      }

      this.RestartSpecialBall();
   }

   boolean BothInFront(AIFootBaller cFirstFB, AIFootBaller cSecFB) {
      return cFirstFB.m_cStrategy.IsForwardToPos(cSecFB, cFirstFB.GetPosition()) && cFirstFB.m_cStrategy.IsForwardToPos(cFirstFB, cSecFB.GetPosition());
   }

   void ResetStabilizationRaw() {
      ((AITeam)super.m_teams[0]).ResetStabilizationRaw();
      ((AITeam)super.m_teams[1]).ResetStabilizationRaw();
      super.m_cReferee.ResetStabilization();
      this.m_bIsStabilization = false;
      this.ResetWaiting();
   }

   void RestartSpecialBall() {
      this.m_sDB_SpecialBallPoint = " RestartSpecialBall ";
      this.m_bSpecialBallStarted = false;
      if (this.IsStabilizationNew()) {
         this.ResetStabilizationRaw();
      }

      if (!IsMatchState(7)) {
         this.SetSpecialBallStab();
      }

      this.SetStabilizationNew(false);
   }

   void SetStabilizationNew(boolean bWithDelay) {
      this.SetWaiting();
      this.m_bIsStabilization = true;
      Ball cBall = GameField.m_cBall;
      if (cBall.GetCurOwner() != null) {
         ((AIFootBaller)cBall.GetCurOwner()).DetachBall();
      }

      ((AITeam)super.m_teams[0]).SetStabilization(bWithDelay);
      ((AITeam)super.m_teams[1]).SetStabilization(bWithDelay);
      super.m_cReferee.SetStabilization();
   }

   AIMatch() {
      SoccerGame.IncMatch();
   }

   boolean CanBreak(AIFootBaller cFirstFB, AIFootBaller cSecFB) {
      if (!cFirstFB.CanGoalBreak() && !cSecFB.IsDoingFeint() || !cSecFB.CanGoalBreak() && !cFirstFB.IsDoingFeint()) {
         if (cFirstFB.Condition() || cSecFB.Condition()) {
            if (!cFirstFB.CanGoalBreak()) {
               GameObject.LogLine(cFirstFB + " no can break (match)" + cFirstFB.GetGoal());
            }

            if (!cSecFB.CanGoalBreak()) {
               GameObject.LogLine(cSecFB + " no can break (match)" + cSecFB.GetGoal());
            }
         }

         return false;
      } else {
         return true;
      }
   }

   boolean IsStabilizationNew() {
      return this.m_bIsStabilization;
   }

   void SetStateStarted() {
      m_bStateStarted = true;
   }

   boolean IsStateStarted() {
      return m_bStateStarted;
   }

   void ResetSpecialBallStab() {
      Match.m_cModule.DisableCallMethod("BreakSpecialBallStab");
      Match.m_cModule.DisableCallMethod("CallBreakStab");
      Match.m_cModule.DisableCallMethod("CallReplay");
      if (!IsMatchState(7)) {
         SoccerModule.ShowHud();
         Match.m_cModule.GetSoccerCamera().ClearReplayCamera();
      }

      Match.m_cModule.StartFadeIn();
      this.m_bSpecialBallStab = false;
      if (IsMatchState(7)) {
         Match.m_cModule.CallMethodIn("StartGameDelay", 1.5F);
      } else {
         this.StartSpecialBall(this.m_nTeamToStart);
      }

      this.SetStateStarted();
   }

   void RestartHalf() {
      this.m_bGameStarted = false;
      this.SignReferee();
      Match.m_cModule.m_Clock.Stop();
      GameField.m_cBall.TakeBallEasy();
      this.ResetMatchParams();
      this.RestartControllers();
      if (this.IsStabilizationNew()) {
         this.ResetStabilizationRaw();
      }

      this.cSpecialBallSel.SetActive(false);
      this.m_bHalfChanging = true;
      this.m_bMatchEnding = false;
      this.m_bWasHalfMenu = false;
      this.m_bTeamChangedSides = false;
      SetMatchState(3);
      this.SetStabilizationNew(true);
      Match.m_cModule.GetSoccerCamera().SetEndHalfCamera();
      this.SetSpaceActive(true);
   }

   void OnOut(FootBaller cFB, int nSide, float fZofOut) {
      this.SignReferee();
      SoccerGame.game.GetCommentator().CommentOut();
      Match.m_cModule.m_Clock.Stop();
      SoccerModule.m_cHud.ShowEventInfo(1);
      SetMatchState(5);
      this.m_nSpecialGoalSide = nSide;
      this.m_bLastOutIsShoot = false;
      if (cFB.m_nTeam == 0) {
         this.m_nTeamToStart = 1;
      } else {
         this.m_nTeamToStart = 0;
      }

      this.m_fZofOut = fZofOut;
      this.RestartSpecialBall();
   }

   void SetSelectionDestPos(Vector vPos) {
      this.cSpecialBallSel.SetPosition(vPos);
   }

   void RefreshContext() {
      DecisionContext cDecContext = new DecisionContext();
      SoccerGame.IncCtx();

      for (int i = 0; i < 2; i++) {
         if (super.m_cBallPossesor == null) {
            cDecContext.m_nBallPossesor = 0;
         } else if (super.m_teams[i] == super.m_cBallPossesor) {
            cDecContext.m_nBallPossesor = 1;
         } else {
            cDecContext.m_nBallPossesor = 2;
         }

         ((AITeam)super.m_teams[i]).RefreshMatchContext(cDecContext);
      }
   }

   void EndMatch() {
      this.EndGame();
   }

   void AfterMatch() {
      Match.m_cMatchData.m_nGoalsHost = super.m_nScoreLeft;
      Match.m_cMatchData.m_nGoalsGuest = super.m_nScoreRight;
      // [mod] classement Elo
      ModElo.OnMatchEnd(this);
      // [mod] historique du profil
      ModStats.OnMatchEnd(this);
      // [mod] survie : manche réussie ou fin de série (après ModStats, qui enregistre la manche jouée)
      ModSurvival.OnMatchEnd(this);
      // [mod] ligue : résultat du match du joueur (après ModStats, qui lit le match du titre)
      ModLeagueMode.OnMatchEnd(this);
      // [mod] ligue en ligne : score du match contre l'IA, envoyé au serveur
      ModOnlineLeague.OnMatchEnd(this);
      this.ClearWeatherEvents();
      Commentator cComm = SoccerGame.game.GetCommentator();
      if (!CoreLogic.m_bDemo) {
         cComm.CallMethodIn("CommentMatchEnd", 0.5F);
      }
   }

   static int GetMatchState() {
      return m_nMatchState;
   }

   void RestartControllers() {
      AITeam cTeamLeft = (AITeam)super.m_teams[0];
      AITeam cTeamRight = (AITeam)super.m_teams[1];
      if (cTeamLeft.m_bUserControled) {
         cTeamLeft.GetTeamController().Clear();
      }

      if (cTeamRight.m_bUserControled) {
         cTeamRight.GetTeamController().Clear();
      }
   }

   public void delete() {
      this.m_cSpecialFB = null;
      this.m_cLastSpecialFB = null;
      this.m_cSpecialDestFB = null;
      this.m_cActiveLossFB = null;
      this.m_cSpecialFBRight = null;
      this.m_cSpecialFBLeft = null;
      this.m_test_cLatestSpecialFB = null;
      this.cSpecialBallSel = null;
      this.m_cSuperSpeedSoundFB = null;
      this.m_cSuperFeintSoundFB = null;
      super.delete();
   }

   void UpdateIntro() {
      if (!this.m_bIntroFinished) {
         AITeam cTeamLeft = (AITeam)super.m_teams[0];
         AITeam cTeamRight = (AITeam)super.m_teams[1];
         if (!this.m_bTeamsGoneToLoterry) {
            if (cTeamLeft.EveryOneSpecialGoalDone() && cTeamRight.EveryOneSpecialGoalDone()) {
               if (this.m_cSpecialFBLeft != null) {
                  this.m_cSpecialFBLeft
                     .ChangeDirectionLogic(super.m_cStrategy.GetActionDirToDest(this.m_cSpecialFBLeft, super.m_cReferee.GetPosition(), false));
               }

               if (this.m_cSpecialFBRight != null) {
                  this.m_cSpecialFBRight
                     .ChangeDirectionLogic(super.m_cStrategy.GetActionDirToDest(this.m_cSpecialFBRight, super.m_cReferee.GetPosition(), false));
               }

               this.m_bTeamsGoneToLoterry = true;
               super.m_cReferee.SetReactType("sedzia_losuje");
               super.m_cReferee.SetNoDone();
               super.m_cReferee.ChangeActivity("Reakcja");
               this.m_bRefereeTossing = true;
            } else {
               if (this.m_cSpecialFBLeft != null) {
                  Vector vLeft = super.m_cStrategy.GetLotteryPosition(this.m_cSpecialFBLeft);
                  cTeamLeft.CheckHotSpecialGoals(18, 22, vLeft);
               }

               if (this.m_cSpecialFBRight != null) {
                  Vector vRight = super.m_cStrategy.GetLotteryPosition(this.m_cSpecialFBRight);
                  cTeamRight.CheckHotSpecialGoals(18, 22, vRight);
               }
            }
         }

         if (this.m_bRefereeTossing && super.m_cReferee.Done()) {
            super.m_cReferee.ChangeActivity("Stanie");
            super.m_cReferee.SetReactType("sedzia_daje_znak");
            super.m_cReferee.SetNoDone();
            super.m_cReferee.ChangeActivity("Reakcja");
            this.m_bRefereeDecision = true;
            this.m_bRefereeTossing = false;
         }

         if (this.m_bRefereeDecision && super.m_cReferee.Done()) {
            this.m_bIntroFinished = true;
            super.m_cReferee.ResetIgnoreCollision();
            this.SideLottery();
            this.RestartGame(this.m_nTeamStartedFirstHalf);
         }
      }
   }

   void SetActive(int nTeam, FootBaller cFBaller) {
      if (super.m_teams[nTeam] != null) {
         ((AITeam)super.m_teams[nTeam]).SetActive(cFBaller);
      }
   }
}
