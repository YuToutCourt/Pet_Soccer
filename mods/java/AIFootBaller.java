public class AIFootBaller extends FootBaller {
   static final int RECEIVE_DOWN = 0;
   static final int RECEIVE_BRUST = 1;
   static final int RECEIVE_HEAD = 2;
   static final int _ATTACH_FREE = 0;
   static final int _ATTACH_ATTACK = 1;
   int m_nAnimalType = -1;
   AIFootBaller.Feint m_cFeint = null;
   AIFootBaller m_cFightOpp = null;
   AIFootBaller.Shot m_cShot = null;
   AIFootBaller.Kick m_cKick = null;
   AIFootBaller.Shot m_cDefence = null;
   boolean m_bBumped = false;
   boolean m_bSuperFeint = false;
   boolean m_bSuperSpeed = false;
   boolean m_bSuperShotMovement = false;
   boolean m_bSuperShotReaction = false;
   int m_nSuperSpeedSound = -1;
   int m_nSuperFeintSound = -1;
   boolean m_bDefenceNone = false;
   Situation m_cTempSituation = new Situation();
   float m_fSuperWallTimeLeft = 0.0F;
   float m_fFreeTimeLong = 1.0F;
   boolean m_bWasCommentAfterBallTaken = false;
   boolean m_bEmptyReact = false;
   boolean m_bIsFalling = false;
   boolean m_bJumping = false;
   boolean m_bHeadShot = false;
   boolean m_bBallTakenSucces = false;
   boolean m_bAttackKick = false;
   boolean m_bPassDest = false;
   boolean m_bTakingBallFromKey = false;
   boolean m_bHasToInteria = false;
   boolean m_bActiveAI = true;
   Vector m_vValidPosOff = null;
   float m_fDevX = 0.0F;
   float m_fDevZ = 0.0F;
   Vector m_vValidPos = null;
   Vector m_vStabilizationPos = null;
   AISystem m_cAISystem = null;
   boolean m_bSpecialGoal = false;
   boolean m_bSpecialGoalDone = false;
   AIFootBaller m_bSpecialFBDest = null;
   float m_fAttackInteria = 0.8F;
   float m_fFreeBallInteria = 0.8F;
   boolean m_bInited = false;
   boolean m_bDecisionProcess = false;
   boolean m_bSituationFixed = false;
   DecisionContext m_cDecContext = null;
   String m_sParams = null;
   Vector m_test_vLastKickBallPos = null;
   String m_test_sDestFB = null;
   Vector m_test_vDestPos = null;
   String m_test_sDestFindWay = null;
   boolean m_bIdle = false;
   boolean m_bHasSingleUpdate = false;
   ControlObject m_cCollObject = null;
   boolean m_bIgnoreCollision = false;
   boolean m_bIsToStabilization = false;
   boolean m_bWaitingForDelayStab = false;
   Vector m_vRight = null;
   Vector m_vRightSlant = null;
   Vector m_vForward = null;
   Vector m_vLeft = null;
   Vector m_vLeftSlant = null;
   Vector m_vBack = null;
   Vector m_vPassPosition = null;
   float m_fPassPositionSituation = -1.0F;
   boolean m_nBallAttachedAfterCol = false;
   boolean m_bKick = false;
   float m_fKickMarkTime = 0.0F;
   boolean m_bLastBallTouched = false;
   static int PROFILE_BallReceive = 0;
   float m_fMinPassLobDistance = 500.0F;
   String m_sKickType = null;
   boolean m_sIdleBeforeKick = false;
   boolean m_bOutside = false;
   float fPrevTime = 0.0F;

   void ResetShot(AIFootBaller.Shot cShot) {
      this.m_cShot = null;
   }

   void ResetDefence() {
      this.m_cDefence = null;
   }

   void GoalKeeperNotice() {
      if (this.m_cDefence != null) {
         AITeam cTeam = this.GetOppTeam();
         AIFootBaller cGK = (AIFootBaller)cTeam.GetGoalKeeper();
         float fGKSpeed = super.m_cStrategy.m_fSpeedThrowGK;
         float fGKDist = Math.abs(this.m_cDefence.m_vDest.fX - cGK.GetPosition().fX);
         if (fGKDist < super.m_cStrategy.m_fThrowDefence && this.m_cDefence.m_vDest.fX < super.m_cStrategy.m_fUpScope) {
            this.m_vValidPos = this.m_cDefence.m_vDest;
            this.m_vValidPos.fY = cGK.GetPosition().fY;
            this.SetDefence(this.m_cDefence);
         } else {
            if (fGKDist < super.m_cStrategy.m_fThrowDefence && this.m_cDefence.m_vDest.fY > super.m_cStrategy.m_fUpDefence) {
               fGKDist = this.m_cDefence.m_vDest.fY;
            }

            float fGKTimeReach = fGKDist / fGKSpeed;
            Vector vBallDist = new Vector(this.m_cDefence.m_vDest);
            vBallDist.Sub(this.m_cDefence.m_vStartPos);
            float fGKBallTimeReach = vBallDist.Len() / this.m_cDefence.fSpeed;
            float fDefenceTime = fGKBallTimeReach - fGKTimeReach;
            if (fDefenceTime < 0.0F) {
               fDefenceTime = 0.0F;
            }

            float fDelay = 0.0F;
            if (this.IsUserControled() && !CoreLogic.m_bMultiplayer) {
               fDelay = super.m_cStrategy.m_fGKNoticeTime;
            }

            if (vBallDist.Len() > 400.0F) {
               this.CallMethodIn("DefenceRequest", fDefenceTime + fDelay / 2.0F);
            } else if (fDelay > 0.0F) {
               this.CallMethodIn("DefenceRequest", fDelay);
            } else {
               this.DefenceRequest();
            }
         }
      }
   }

   void Shot() {
      if (this.HasBall()) {
         if (this.m_cShot == null) {
            this.SetShot(super.m_cStrategy.GetShotDescription(this));
         }

         if (this.m_cShot == null) {
            if (this.IsUserControled()) {
               this.KickStrongForward();
            }
         } else {
            AIFootBaller.Shot cShot = this.m_cShot;
            this.m_cKick = new AIFootBaller.Kick(this);
            this.m_cKick.m_vDestPos = cShot.m_vDest;
            this.m_cKick.m_bShoot = true;
            this.m_cKick.m_bDefence = false;
            if (!(cShot.m_vDest.fY - this.TerrainHeight(cShot.m_vDest.fX, cShot.m_vDest.fZ) > 70.0F) && !this.m_bHeadShot) {
               this.m_cKick.m_bLob = false;
               SoccerModule.m_cMatch.UnblockControlForFocus(cShot.m_vDest);
            } else {
               if (this.m_bHeadShot) {
                  SoccerGame.game.GetCommentator().CommentHead(true, false);
                  this.m_cDefence = GameField.m_cBall.KickToLob(cShot.m_vDest, true);
                  this.PlayKickFX();
                  this.GoalKeeperNotice();
               }

               this.m_cKick.m_bLob = true;
               SoccerModule.m_cMatch.UnblockControlForFocus(cShot.m_vDest);
            }

            if (!this.m_bHeadShot) {
               SoccerGame.game.GetCommentator().CommentShot(this);
               this.RequestKickShowing("Shot");
            }
         }

         this.SetKickMark();
      }
   }

   void SettleToShot() {
      Vector vGatePos;
      if (super.m_nTeam == 0) {
         vGatePos = GameField.cGateRight.GetPosition();
      } else {
         vGatePos = GameField.cGateLeft.GetPosition();
      }

      Vector vDist = new Vector(GameField.cCenter.GetPosition());
      vDist.Sub(vGatePos);
      vDist.Normalize();
      vDist.Mul(800.0F);
      Vector vNewPos = new Vector();
      vGatePos.Add(vDist, vNewPos);
      this.SetPosition(vNewPos);
   }

   void ShowOutside() {
      GameObject.LogLine(" active " + this.IsActive() + " fight " + this.m_cFightOpp + " idle " + this.m_bIdle + " singleUpdate " + this.m_bHasSingleUpdate);
   }

   boolean IsOutside() {
      int nZone = this.GetZone();
      return nZone == 3
         || nZone == 4
         || GameField.GetZoneForPointVertical(this.GetPosition()) == 6
         || GameField.GetZoneForPointVertical(this.GetPosition()) == 5;
   }

   public void StopIdle() {
      Team cTeam = null;
      FootBaller cFB = null;
      if (SoccerModule.m_cMatch != null) {
         for (int i = 0; i < 2; i++) {
            cTeam = SoccerModule.m_cMatch.GetTeam(i);

            for (int j = 0; j < cTeam.GetFootBallerCount(); j++) {
               cFB = cTeam.GetFootBaller(j);
               if (cFB != this) {
                  cFB.SetInteraction(this, true);
                  this.SetInteraction(cFB, true);
               }
            }
         }

         this.m_bIdle = false;
      }
   }

   void StartIdle() {
      if (!this.IsGoalKeeper()) {
         Team cTeam = null;
         FootBaller cFB = null;
         if (SoccerModule.m_cMatch == null) {
            return;
         }

         for (int i = 0; i < 2; i++) {
            cTeam = SoccerModule.m_cMatch.GetTeam(i);

            for (int j = 0; j < cTeam.GetFootBallerCount(); j++) {
               cFB = cTeam.GetFootBaller(j);
               if (cFB != this) {
                  cFB.SetInteraction(this, false);
                  this.SetInteraction(cFB, false);
               }
            }
         }
      }

      this.m_bIdle = true;
   }

   public boolean IsIdle() {
      return this.m_bIdle;
   }

   boolean HasGoalFight() {
      return this.m_cDecContext.m_cGoal != null && this.m_cDecContext.m_cGoal.IsFight();
   }

   void Feint() {
      AIFootBaller cOpp = super.m_cStrategy.GetFBDistance(this);
      if (cOpp != null) {
         this.m_cAISystem.RequestEvent(this, 0, cOpp);
      }
   }

   boolean HasEvent(int nEventInd) {
      return this.m_cDecContext.m_cEvent != null && this.m_cDecContext.m_cEvent.m_nId == nEventInd;
   }

   boolean IsDoingFeint() {
      return this.m_cFeint != null;
   }

   boolean IsDoingFeint(AIFootBaller cOpponent) {
      return this.m_cFeint != null && this.m_cFeint.m_cOpponent == cOpponent;
   }

   AITeam GetOppTeam() {
      return super.m_nTeam == 0 ? (AITeam)SoccerModule.m_cMatch.m_teams[1] : (AITeam)SoccerModule.m_cMatch.m_teams[0];
   }

   void SetSuperFeint() {
      this.m_bSuperFeint = true;
   }

   boolean IsSuperFeint() {
      return this.m_bSuperFeint;
   }

   void SetSuperSpeed() {
      this.m_bSuperSpeed = true;
   }

   boolean IsSuperSpeed() {
      return this.m_bSuperSpeed;
   }

   boolean AttachBall(int nMode, int nReceiveType, AIFootBaller cPrevOwner) {
      Ball cBall = GameField.m_cBall;
      if (nMode == 0) {
         if (cBall.IsShooted()) {
            cBall.ResetShooted();
         }

         GameField.m_cBall.SetNewOwner(null, this);
      } else {
         this.SetBallInteraction();
         cBall.BallDetachFromParentAndKeepWorldPos();
         cBall.SetNewOwner(cPrevOwner, this);
      }

      cBall.SetSpeed(new Vector());
      cBall.BallAttachAsChildTo(this);
      this.ClearBallInteraction();
      cBall.m_vPosAttached = new Vector(0.0F, cBall.fBallRadius, cBall.fBallRadius + super.fEllRadius);
      if (this.IsGoalKeeper() && AIMatch.IsMatchState(0)) {
         GameField.SetDestPos(GameField.cCenter.GetPositionVectorVolatile(), false);
         SoccerModule.m_cMatch.UpdateOrder();
         ((AITeam)SoccerModule.m_cMatch.GetTeam(0)).ResetHelpAttacker();
         ((AITeam)SoccerModule.m_cMatch.GetTeam(0)).ResetHelpBallAttacker();
         ((AITeam)SoccerModule.m_cMatch.GetTeam(1)).ResetHelpAttacker();
         ((AITeam)SoccerModule.m_cMatch.GetTeam(1)).ResetHelpBallAttacker();
      }

      return true;
   }

   AIFootBaller PassDirBall(boolean bLob, float fScope) {
      AIFootBaller cFBaller = (AIFootBaller)super.m_cStrategy.FindPassPlayer(this, fScope);
      if (cFBaller != null) {
         Vector vPassDest = super.m_cStrategy.ChosePassPosition(this, cFBaller);
         float fObstacle = super.m_cStrategy.CalculateTraceObstacles(this, this.GetPosition(), vPassDest, cFBaller);
         if (fObstacle < 1.0F) {
            this.Pass(cFBaller, vPassDest, true);
         } else {
            this.Pass(cFBaller, vPassDest, false);
         }
      }

      return cFBaller;
   }

   void DoSpecialBall() {
      SoccerModule.m_cMatch.m_bFirstPass = true;
      AIFootBaller cDestFB = null;
      this.m_bSpecialFBDest = null;
      this.m_test_sDestFB = null;
      this.m_test_vDestPos = null;
      this.m_test_sDestFindWay = null;
      this.m_test_sDestFindWay = " ";
      if (!this.IsUserControled()) {
         this.m_test_sDestFindWay = this.m_test_sDestFindWay + "! isusercontroled ";
         if (!AIMatch.IsMatchState(7)) {
            ((AITeam)super.m_cTeam).CalculateParamsForAll();
            float fScope = Strategy.m_fSpecialBallScope;
            if (this.IsGoalKeeper()) {
               fScope = Strategy.m_fSpecialBallScopeGK;
            }

            cDestFB = (AIFootBaller)super.m_cStrategy.FindBestFootBaller(this, fScope);
            this.m_test_sDestFB = " after findbest " + cDestFB;
            if (cDestFB == null) {
               cDestFB = (AIFootBaller)super.m_cStrategy.FindPassPlayer(this, fScope);
               this.m_test_sDestFindWay = this.m_test_sDestFindWay + " findpassplayer " + cDestFB;
            }
         } else {
            this.m_test_sDestFindWay = this.m_test_sDestFindWay
               + " start game: SoccerModule.m_cMatch.m_cSpecialDestFB "
               + SoccerModule.m_cMatch.m_cSpecialDestFB;
            cDestFB = SoccerModule.m_cMatch.m_cSpecialDestFB;
         }

         if (cDestFB != null) {
            this.m_bSpecialFBDest = cDestFB;
            GameField.SetDestPos(cDestFB.GetPosition(), false);
            if (!AIMatch.IsMatchState(7)) {
               SoccerModule.m_cMatch.UpdateOrder();
            }
         }

         if (!AIMatch.IsMatchState(7)) {
            this.CallMethodIn("PassSpecialBall", 1.5F);
         }
      } else {
         SoccerModule.m_cMatch.FixSelectionDestSystem((AITeam)super.m_cTeam);
      }
   }

   public void PassSpecialBall() {
      this.DisableCallMethod("PassSpecialBall");
      GameObject.LogLine(this + " PASS special BALL");
      SoccerModule.m_cMatch.m_cActiveLossFB = this;
      this.m_cKick = new AIFootBaller.Kick(this);
      this.m_cKick.m_bShoot = false;
      this.m_cKick.m_bDefence = false;
      if (AIMatch.IsMatchState(7)) {
         this.m_cKick.m_bLob = false;
      } else {
         this.m_cKick.m_bLob = true;
      }

      AIFootBaller cKickoffDest = SoccerModule.m_cMatch.m_cSpecialDestFB;
      if (AIMatch.IsMatchState(7) && cKickoffDest != null && !this.IsUserControled()) {
         // [mod] coup d'envoi de l'IA : dans les pieds du receveur, qui devient le destinataire de la passe (actif, va au ballon).
         // Le jeu d'origine visait ChosePassPosition, un point à côté du receveur en direction de la touche : RealKick devait
         // corriger la cible en état 7, mais GoalKick passe en état 0 juste avant RealKick, donc le ballon partait souvent en touche.
         this.m_bSpecialFBDest = cKickoffDest;
         this.m_cKick.m_vDestPos = cKickoffDest.GetPosition();
         this.m_cKick.m_cPassDestFB = cKickoffDest;
         GameField.SetDestPos(this.m_cKick.m_vDestPos, false);
      } else if (this.m_bSpecialFBDest != null) {
         this.m_test_sDestFindWay = this.m_test_sDestFindWay + " m_bSpecialFBDest = " + this.m_bSpecialFBDest;
         Vector vPassDest = super.m_cStrategy.ChosePassPosition(this, this.m_bSpecialFBDest);
         this.m_test_vDestPos = vPassDest;
         this.m_test_sDestFindWay = this.m_test_sDestFindWay + " vPassDest " + vPassDest;
         if (vPassDest != null) {
            Vector vLogPos = GameField.WorldToField(vPassDest);
            if (vLogPos.fZ > GameField.fLineEnd - 200.0F) {
               vPassDest.fZ -= 200.0F;
            }

            if (vLogPos.fZ < 200.0F) {
               vPassDest.fZ += 200.0F;
            }

            if (vPassDest.fX > GameField.fLineOutTop - 200.0F) {
               vPassDest.fX -= 200.0F;
            }

            if (vPassDest.fX < -200.0F) {
               vPassDest.fX += 200.0F;
            }

            this.m_cKick.m_vDestPos = vPassDest;
         } else {
            this.m_cKick.m_vDestPos = GameField.cCenter.GetPosition();
            this.m_test_sDestFindWay = this.m_test_sDestFindWay + " center pos ";
         }
      } else if (this.IsUserControled()) {
         Vector vDest = null;
         if (AIMatch.IsMatchState(7)) {
            vDest = SoccerModule.m_cMatch.m_cSpecialDestFB.GetPosition();
            GameField.SetDestPos(SoccerModule.m_cMatch.m_cSpecialDestFB.GetPosition(), false);
         } else {
            vDest = SoccerModule.m_cMatch.cSpecialBallSel.GetPosition();
         }

         this.m_cKick.m_vDestPos = vDest;
         SoccerModule.m_cMatch.ClearSelectionDestSystem();
      } else {
         this.m_cKick.m_vDestPos = GameField.cCenter.GetPosition();
         this.m_test_sDestFindWay = this.m_test_sDestFindWay + " center pos ";
      }

      ModKickoff.OnPass(this, this.m_bSpecialFBDest, this.m_cKick.m_vDestPos); // [mod] traces du coup d'envoi
      SoccerModule.m_cMatch.UnblockControlForFocus(this.m_cKick.m_vDestPos);
      this.RequestKickShowing("PassSpecialBall");
      this.SetKickMark();
      this.ResetSpecialGoal();
      if (SoccerModule.m_cMatch.m_cSpecialFB == this) {
         SoccerModule.m_cMatch.m_cLastSpecialFB = this;
         SoccerModule.m_cMatch.m_cSpecialFB = null;
      }
   }

   void GoBack(Vector vDest) {
      this.ChangeActivity("cofanie");
      this.SetPhysicsActive(true);
      this.SetPhysicsType(5);
      this.SetSpeed(super.m_cStrategy.GetBackDir(this, vDest));
   }

   void ShowContext() {
      GameObject.LogLine("AIFootBaller: " + super.m_sIndividualName);
      GameObject.LogLine(" - T: " + this.m_cDecContext.m_nAIFootBallerType);
      GameObject.LogLine(" - A: " + this.m_cDecContext.m_bActive);
      GameObject.LogLine(" - P: " + this.m_cDecContext.m_nBallPossesor);
      GameObject.LogLine(" - G: " + this.m_cDecContext.m_cGoal);
      GameObject.LogLine(" - E: " + this.m_cDecContext.m_cEvent);
   }

   void SetPassActive(AIFootBaller cFBaller) {
      AITeam cTeam = (AITeam)super.m_cTeam;
      cTeam.SetActive(cFBaller);
      cFBaller.SetPassDest();
      this.m_cAISystem.RequestEvent(cFBaller, 2, GameField.GetDestPos());
   }

   void SetReactType(String sType) {
      super.m_currContext.SetValue("Typ_reakcji", sType, super.m_advSpace);
      this.ChangeContext();
   }

   void SetFreeTimeType(String sType) {
      super.m_currContext.SetValue("Wolny_czas", sType, super.m_advSpace);
      this.ChangeContext();
   }

   boolean IsReactType(String sType) {
      return super.m_currContext.IsValue("Typ_reakcji", sType, super.m_advSpace);
   }

   boolean IsSuperWall() {
      return ((AITeam)super.m_cTeam).IsSuperWall();
   }

   ControlObject GetCollObject() {
      return this.m_cCollObject;
   }

   void ResetFight() {
      this.m_cFightOpp = null;
   }

   void ResetCollObject() {
      this.m_cCollObject = null;
   }

   void SetIgnoreCollision() {
      this.m_bIgnoreCollision = true;
   }

   void WrtiteStateAsCrashLog() {
      GameObject.CrashLog(this + " ---------");
      GameObject.CrashLog(" - m_cGoal " + this.m_cDecContext.m_cGoal + "\n");
      if (this.m_cDecContext.m_cGoal != null) {
         GameObject.CrashLog(" - m_cGoal.m_bReached " + this.m_cDecContext.m_cGoal.m_bReached + "\n");
      }

      if (this.m_cDecContext.m_cGoal instanceof GoalReact) {
         GameObject.CrashLog(" - m_cGoal.m_bFirstPartDone " + ((GoalReact)this.m_cDecContext.m_cGoal).m_bFirstPartDone + "\n");
      }

      GameObject.CrashLog(
         " - m_cEvent " + this.m_cDecContext.m_cEvent + "\n" + " - Position " + this.GetPosition() + "\n" + " - m_cFeint " + this.m_cFeint + "\n"
      );
      if (this.m_cFeint != null) {
         GameObject.CrashLog(" - Feint opp: " + this.m_cFeint.m_cOpponent);
      }

      GameObject.CrashLog(
         " - m_cFightOpp "
            + this.m_cFightOpp
            + "\n"
            + " - m_cShot "
            + this.m_cShot
            + "\n"
            + " - m_cKick "
            + this.m_cKick
            + "\n"
            + " - m_sKickType "
            + this.m_sKickType
            + "\n"
            + " - m_sIdleBeforeKick "
            + this.m_sIdleBeforeKick
            + "\n"
            + " - m_cDefence "
            + this.m_cDefence
            + "\n"
            + " - m_bBumped "
            + this.m_bBumped
            + "\n"
            + " - m_bSuperFeint "
            + this.m_bSuperFeint
            + "\n"
            + " - m_bSuperSpeed "
            + this.m_bSuperSpeed
            + "\n"
            + " - m_bSuperShotMovement "
            + this.m_bSuperShotMovement
            + "\n"
            + " - m_bSuperShotReaction "
            + this.m_bSuperShotReaction
            + "\n"
            + " - m_nSuperSpeedSound "
            + this.m_nSuperSpeedSound
            + "\n"
            + " - m_nSuperFeintSound "
            + this.m_nSuperFeintSound
            + "\n"
            + " - m_bDefenceNone "
            + this.m_bDefenceNone
            + "\n"
            + " - m_bEmptyReact "
            + this.m_bEmptyReact
            + "\n"
            + " - m_bIsFalling "
            + this.m_bIsFalling
            + "\n"
            + " - m_bJumping "
            + this.m_bJumping
            + "\n"
            + " - m_bHeadShot "
            + this.m_bHeadShot
            + "\n"
            + " - m_bBallTakenSucces "
            + this.m_bBallTakenSucces
            + "\n"
            + " - m_bAttackKick "
            + this.m_bAttackKick
            + "\n"
            + " - m_bPassDest "
            + this.m_bPassDest
            + "\n"
            + " - m_bHasToInteria "
            + this.m_bHasToInteria
            + "\n"
            + " - m_bActiveAI "
            + this.m_bActiveAI
            + "\n"
            + " - m_vValidPos "
            + this.m_vValidPos
            + "\n"
            + " - m_vStabilizationPos "
            + this.m_vStabilizationPos
            + "\n"
            + " - m_bSpecialGoal "
            + this.m_bSpecialGoal
            + "\n"
            + " -  IsSelDestTracker() "
            + this.IsSelDestTracker()
            + "\n"
            + " -  IsHelpAttacker()  "
            + this.IsHelpAttacker()
            + "\n"
            + " -  IsHelpBallAttacker()  "
            + this.IsHelpBallAttacker()
            + "\n"
            + " -  IsActive()  "
            + this.IsActive()
            + "\n"
            + " -  HasFocus()  "
            + this.HasFocus()
            + "\n"
            + " - m_bSpecialGoalDone "
            + this.m_bSpecialGoalDone
            + "\n"
            + " - m_bSpecialFBDest "
            + this.m_bSpecialFBDest
            + "\n"
            + " - m_bDecisionProcess "
            + this.m_bDecisionProcess
            + "\n"
            + " - m_bSituationFixed "
            + this.m_bSituationFixed
            + "\n"
            + " - m_bIdle "
            + this.m_bIdle
            + "\n"
            + " - m_bHasSingleUpdate "
            + this.m_bHasSingleUpdate
            + "\n"
            + " - m_cCollObject "
            + this.m_cCollObject
            + "\n"
            + " - m_bIgnoreCollision "
            + this.m_bIgnoreCollision
            + "\n"
            + " - m_bIsToStabilization "
            + this.m_bIsToStabilization
            + "\n"
            + " - Activity  "
            + this.GetActivity()
            + "\n"
            + " m_currAnim.m_sName "
            + super.m_currAnim.m_sName
            + " \n"
            + " - m_bKick "
            + this.m_bKick
            + "\n"
            + " - m_test_sDestFB "
            + this.m_test_sDestFB
            + "\n"
            + " - m_test_vDestPos "
            + this.m_test_vDestPos
            + "\n"
            + " - m_test_sDestFindWay "
            + this.m_test_sDestFindWay
            + "\n"
            + " -  m_test_vLastKickBallPos "
            + this.m_test_vLastKickBallPos
            + "\n"
            + " - bBallKicked "
            + super.bBallKicked
            + "\n"
      );
   }

   void Init(SpawnPoint cPoint, SpawnPoint cPointStart, SpawnPoint cPointPresentation, int nTeam) {
      super.Init(cPoint, cPointStart, cPointPresentation, nTeam);
      this.RefreshContext();
   }

   void Init(SpawnPoint cPoint, int nTeam) {
      this.Init(cPoint, null, null, nTeam);
   }

   void ResetSuperFeint() {
      this.m_bSuperFeint = false;
   }

   void PassDestPosition(AIFootBaller cFBaller, Vector vDestPos, boolean bLob) {
      this.Pass(cFBaller, vDestPos, bLob);
   }

   void SetBallInteraction() {
      if (!GameField.m_cBall.IsSuperShot()) {
         this.SetInteraction(GameField.m_cBall, true);
         GameField.m_cBall.SetInteraction(this, true);
      }
   }

   void PassAction() {
      if (this.HasBall()) {
         this.PassDirBall(false, super.m_cStrategy.GetHalfFieldDistance());
      }
   }

   void CalcValidPosDeviation() {
      this.m_fDevX = GameObject.RandomFloat(super.m_cStrategy.m_fValidPosDeviation);
      this.m_fDevZ = GameObject.RandomFloat(super.m_cStrategy.m_fValidPosDeviation);
      float fSign = 1.0F;
      if (GameObject.RandomFloat(100.0F) > 50.0F) {
         fSign *= -1.0F;
      }

      this.m_fDevX *= fSign;
      if (GameObject.RandomFloat(100.0F) > 50.0F) {
         fSign *= -1.0F;
      }

      int nZone = this.GetZone();
      if (nZone != 0 && super.m_nType == 0 && super.m_cTeam.IsPossesor()) {
         if (super.m_nTeam == 0) {
            fSign = 1.0F;
         } else {
            fSign = -1.0F;
         }
      }

      this.m_fDevZ *= fSign;
   }

   void WrtiteStateAsLog() {
      GameObject.LogD(this + " ---------" + "\n");
      GameObject.LogD(" - m_cGoal " + this.m_cDecContext.m_cGoal + "\n");
      GameObject.LogD(" - m_cGoal.m_bReached " + this.m_cDecContext.m_cGoal.m_bReached + "\n");
      GameObject.LogD(" - m_cEvent " + this.m_cDecContext.m_cEvent + "\n");
      GameObject.LogD(" - m_cFeint " + this.m_cFeint + "\n");
      GameObject.LogD(" - m_cFightOpp " + this.m_cFightOpp + "\n");
      GameObject.LogD(" - m_cShot " + this.m_cShot + "\n");
      GameObject.LogD(" - m_cKick " + this.m_cKick + "\n");
      GameObject.LogD(" - m_cDefence " + this.m_cDefence + "\n");
      GameObject.LogD(" - m_bBumped " + this.m_bBumped + "\n");
      GameObject.LogD(" - m_bSuperFeint " + this.m_bSuperFeint + "\n");
      GameObject.LogD(" - m_bSuperSpeed " + this.m_bSuperSpeed + "\n");
      GameObject.LogD(" - m_bSuperShotMovement " + this.m_bSuperShotMovement + "\n");
      GameObject.LogD(" - m_bSuperShotReaction " + this.m_bSuperShotReaction + "\n");
      GameObject.LogD(" - m_nSuperSpeedSound " + this.m_nSuperSpeedSound + "\n");
      GameObject.LogD(" - m_nSuperFeintSound " + this.m_nSuperFeintSound + "\n");
      GameObject.LogD(" - m_bDefenceNone " + this.m_bDefenceNone + "\n");
      GameObject.LogD(" - m_bEmptyReact " + this.m_bEmptyReact + "\n");
      GameObject.LogD(" - m_bIsFalling " + this.m_bIsFalling + "\n");
      GameObject.LogD(" - m_bJumping " + this.m_bJumping + "\n");
      GameObject.LogD(" - m_bHeadShot " + this.m_bHeadShot + "\n");
      GameObject.LogD(" - m_bBallTakenSucces " + this.m_bBallTakenSucces + "\n");
      GameObject.LogD(" - m_bAttackKick " + this.m_bAttackKick + "\n");
      GameObject.LogD(" - m_bPassDest " + this.m_bPassDest + "\n");
      GameObject.LogD(" - m_bHasToInteria " + this.m_bHasToInteria + "\n");
      GameObject.LogD(" - m_bActiveAI " + this.m_bActiveAI + "\n");
      GameObject.LogD(" - m_vValidPos " + this.m_vValidPos + "\n");
      GameObject.LogD(" - m_vStabilizationPos " + this.m_vStabilizationPos + "\n");
      GameObject.LogD(" - m_bSpecialGoal " + this.m_bSpecialGoal + "\n");
      GameObject.LogD(" - m_bSpecialGoalDone " + this.m_bSpecialGoalDone + "\n");
      GameObject.LogD(" - m_bSpecialFBDest " + this.m_bSpecialFBDest + "\n");
      GameObject.LogD(" - m_bDecisionProcess " + this.m_bDecisionProcess + "\n");
      GameObject.LogD(" - m_bIdle " + this.m_bIdle + "\n");
      GameObject.LogD(" - m_bHasSingleUpdate " + this.m_bHasSingleUpdate + "\n");
      GameObject.LogD(" - m_cCollObject " + this.m_cCollObject + "\n");
      GameObject.LogD(" - m_bIgnoreCollision " + this.m_bIgnoreCollision + "\n");
      GameObject.LogD(" - m_bIsToStabilization " + this.m_bIsToStabilization + "\n");
      GameObject.LogD(" - m_bKick " + this.m_bKick + "\n");
      GameObject.LogD(" - bBallKicked " + super.bBallKicked + "\n");
   }

   public void SetPosition(Vector vPos) {
      super.SetPosition(new Vector(vPos.fX, this.TerrainHeight(vPos.fX, vPos.fZ), vPos.fZ));
   }

   // [mod] vitesse multipliée par la stat « Réflexe » de l'équipe (sauf arbitre), par le bonus IA du niveau
   // et, pour les humains, par la triche « vitesse » du menu Mods
   void SetLocalSpeedStat(Vector vSpeed) {
      if (!this.IsReferee()) {
         float fMul = ETeams.GetSpeedMul(super.m_nAnimalType);
         if (!this.IsUserControled()) {
            fMul *= ModDifficulty.GetAISpeedMul();
         } else {
            fMul *= ModOptions.GetSpeedMul();
         }

         fMul *= ModSupers.GetSpeedMul(this); // [mod] super « gel » de l'adversaire
         vSpeed.Mul(fMul);
      }

      super.SetLocalSpeed(vSpeed);
   }

   public void SetLocalSpeed(Vector vSpeed) {
      if (this.IsGoalKeeper()) {
         if (this.IsActivity("Reakcja_z_upadkiem")) {
            if (this.IsContext("Typ_reakcji", "rzut_lewo")) {
               Vector vDefenceSpeed = this.GetRightVector();
               if (super.m_nTeam == 1) {
                  vDefenceSpeed.Mul(-1.0F);
               }

               vDefenceSpeed.Normalize();
               vDefenceSpeed.Mul(super.m_currAnim.m_fSpeed);
               this.SetLocalSpeedStat(vDefenceSpeed);
               if (this.m_bJumping) {
                  this.Jump(new Vector(0.0F, super.m_cStrategy.m_fJumpSpeed, 0.0F));
               }

               return;
            }

            if (this.IsContext("Typ_reakcji", "rzut_prawo")) {
               Vector vDefenceSpeed = this.GetRightVector();
               if (super.m_nTeam == 0) {
                  vDefenceSpeed.Mul(-1.0F);
               }

               vDefenceSpeed.Normalize();
               vDefenceSpeed.Mul(super.m_currAnim.m_fSpeed);
               this.SetLocalSpeedStat(vDefenceSpeed);
               if (this.m_bJumping) {
                  this.Jump(new Vector(0.0F, super.m_cStrategy.m_fJumpSpeed, 0.0F));
               }

               return;
            }
         }

         if (this.IsActivity("Reakcja") && this.IsContext("Typ_reakcji", "chwyt_gora")) {
            if (this.m_bJumping) {
               this.Jump(new Vector(0.0F, super.m_cStrategy.m_fJumpSpeed, 0.0F));
            }

            return;
         }
      }

      boolean bSpeedFixed = false;
      if (super.m_currAnim != null && super.m_currAnim.m_sName != null) {
         if (super.m_currAnim.m_sName.equals("bieganie")) {
            if (this.IsSuperSpeed()) {
               if (super.m_currAnim.m_fFrequency > 0.0F) {
                  this.SetAnimFreq(super.m_currAnim.GetName(), super.m_currAnim.m_fFrequency);
               }

               this.SetLocalSpeedStat(new Vector(0.0F, 0.0F, super.m_cStrategy.m_fSuperSpeed));
               bSpeedFixed = true;
            } else if (this.IsKeySpeed()) {
               if (super.m_currAnim.m_fFrequency > 0.0F) {
                  this.SetAnimFreq(super.m_currAnim.GetName(), super.m_currAnim.m_fFrequency * 1.5F);
               }

               this.SetLocalSpeedStat(new Vector(0.0F, 0.0F, super.m_currAnim.m_fSpeed * 1.5F));
               bSpeedFixed = true;
            } else if (this.IsKeyFallSpeed()) {
               if (super.m_currAnim.m_fFrequency > 0.0F) {
                  this.SetAnimFreq(super.m_currAnim.GetName(), super.m_currAnim.m_fFrequency * 1.15F);
               }

               this.SetLocalSpeedStat(new Vector(0.0F, 0.0F, super.m_currAnim.m_fSpeed * 1.15F));
               bSpeedFixed = true;
            } else if (super.m_bKeyRest) {
               if (super.m_currAnim.m_fFrequency > 0.0F) {
                  this.SetAnimFreq(super.m_currAnim.GetName(), super.m_currAnim.m_fFrequency * 0.7F);
               }

               this.SetLocalSpeedStat(new Vector(0.0F, 0.0F, super.m_currAnim.m_fSpeed * 0.7F));
               bSpeedFixed = true;
            } else if (this.HasBall() && !this.IsSuperFeint()) {
               if (super.m_currAnim.m_fFrequency > 0.0F) {
                  this.SetAnimFreq(super.m_currAnim.GetName(), super.m_currAnim.m_fFrequency);
               }

               this.SetLocalSpeedStat(new Vector(0.0F, 0.0F, super.m_cStrategy.m_fSpeedWithBall));
               bSpeedFixed = true;
            }
         }

         if (!bSpeedFixed) {
            if (super.m_currAnim.m_fFrequency > 0.0F) {
               this.SetAnimFreq(super.m_currAnim.GetName(), super.m_currAnim.m_fFrequency);
            }

            this.SetLocalSpeedStat(new Vector(0.0F, 0.0F, super.m_currAnim.m_fSpeed));
         }
      }
   }

   Goal GetGoal() {
      return this.m_cDecContext.m_cGoal;
   }

   void ResetGoal() {
      this.m_cDecContext.m_cGoal = null;
   }

   void ResetSpecialGoal() {
      this.m_bSpecialGoal = false;
   }

   void FixValidPos() {
      Vector vCenter = null;
      if (super.m_nTeam == 0) {
         vCenter = new Vector(0.0F, 0.0F, GameField.cOrderCenterLeft.GetPosition().fZ);
      } else {
         vCenter = new Vector(0.0F, 0.0F, GameField.cOrderCenterRight.GetPosition().fZ);
      }

      Vector vPos = new Vector(super.cSpawnPoint.GetPosition());
      this.m_vValidPosOff = new Vector(vPos);
      this.m_vValidPosOff.Sub(vCenter);
      this.m_vValidPos = new Vector(super.cSpawnPoint.GetPosition());
      this.m_bInited = true;
   }

   void SetValidPos(Vector vValidPos) {
      this.m_vValidPos = vValidPos;
      this.m_vValidPos.fY = this.TerrainHeight(this.m_vValidPos.fX, this.m_vValidPos.fZ);
   }

   public void OnCollisionFullInfoMove(ControlObject obj, Vector vPoint, Vector vNormal, Vector vMove) {
      if (obj instanceof Ball) {
         GameObject.LogLine(this + " OnColl with ball ");
      }

      if (this.m_bInited
         && super.m_bLiving
         && !this.HasFocus()
         && !this.IsGoalKeeper()
         && !this.m_bIgnoreCollision
         && (AIMatch.IsMatchState(0) || !this.HasBall())) {
         if (super.m_cStrategy.IsForwardToPos(this, obj.GetPosition())) {
            if (!this.Busy() && !this.IsDoingFeint() && this.GetCollObject() != obj) {
               if (!this.IsReferee() || !AIMatch.IsMatchState(7) && !AIMatch.IsMatchState(5)) {
                  if (!(obj instanceof SelectionDest)) {
                     if (obj instanceof AIFootBaller) {
                        AIFootBaller cObstacle = (AIFootBaller)obj;
                        if (cObstacle.GetCollObject() == this) {
                           return;
                        }

                        if (this.HasBall() && cObstacle.IsGoalKeeper()) {
                           return;
                        }
                     } else if (obj instanceof Ball) {
                        if (!SoccerModule.m_cMatch.IsWaiting() && !this.IsReferee() && !this.IsActiveLoss()) {
                           return;
                        }

                        if (SoccerModule.m_cMatch.IsWaiting() && this.HasSpecialGoal() && !AIMatch.IsMatchState(1)) {
                           return;
                        }
                     }

                     this.SetCollObject(obj);
                     if (AISystem.Condition(this)) {
                        GameObject.LogLine(this + " colission with " + obj);
                     }

                     this.m_cAISystem.RequestEvent(this, 17, null);
                  }
               }
            }
         }
      }
   }

   int ChooseReceiveType() {
      return 0;
   }

   String GetReactType() {
      return super.m_currContext.GetValue("Typ_reakcji", super.m_advSpace);
   }

   boolean CanBeActive() {
      return true;
   }

   void KickForward() {
      this.m_cKick = new AIFootBaller.Kick(this);
      this.m_cKick.m_bShoot = false;
      this.m_cKick.m_bDefence = false;
      this.m_cKick.m_bLob = false;
      Vector vShot = new Vector();
      Vector m_vStrong = new Vector(this.GetForwardVector());
      m_vStrong.Normalize();
      m_vStrong.Mul(150.0F);
      this.GetPosition().Add(m_vStrong, vShot);
      this.m_cKick.m_vDestPos = vShot;
      this.RequestKickShowing("KickForward");
      this.SetKickMark();
   }

   void SetShot(AIFootBaller.Shot cShot) {
      this.m_cShot = cShot;
   }

   void SetActiveAI(boolean bActive) {
      this.m_bActiveAI = bActive;
   }

   void KickFarBall() {
      this.m_cKick = new AIFootBaller.Kick(this);
      this.m_cKick.m_bShoot = false;
      this.m_cKick.m_bDefence = false;
      this.m_cKick.m_bLob = true;
      this.m_cKick.m_vDestPos = GameField.cCenter.GetPosition();
      this.RequestKickShowing("KickFarBall");
      this.SetKickMark();
   }

   void BumpDefenceBall() {
      if (this.m_cDefence != null) {
         GameField.m_cBall.m_cCurOwner = this;
         Vector vForward;
         if (super.m_nTeam == 0) {
            vForward = new Vector(GameField._DIR_E);
         } else {
            vForward = new Vector(GameField._DIR_W);
         }

         vForward.Normalize();
         Vector vShotDir = new Vector(this.m_cDefence.m_vDir);
         vShotDir.Normalize();
         vShotDir.Mul(-1.0F);
         float fCos = vShotDir.Dot(vForward);
         Vector vMap = new Vector(vForward);
         vMap.Mul(fCos);
         Vector vDist = new Vector(vMap);
         vDist.Sub(vShotDir);
         float fDist = vDist.Len();
         vDist.Normalize();
         vDist.Mul(fDist * 2.0F);
         Vector vDefenceDir = new Vector();
         vShotDir.Add(vDist, vDefenceDir);
         vDefenceDir.Mul(this.m_cDefence.m_vDir.Len() / 3.0F);
         Vector vDest = new Vector();
         this.GetPosition().Add(vDefenceDir, vDest);
         GameField.m_fDistToDest = vDest.Distance(this.GetPositionVectorVolatile());
         GameField.m_cBall.KickTo(vDest, false, true);
         GameField.m_cBall.m_cPrevOwner = this;
         SoccerModule.m_cMatch.UnblockControlForFocus(vDest);
         GameField.m_cBall.PlayBounceSound(0);
         this.m_bBumped = true;
      }
   }

   public void ClearReact() {
      this.StopIdle();
      this.ResetFight();
   }

   public void NextAnim2() {
      this.PlayAnim("rozgrzewka_przysiady", 1);
   }

   void TakeBall() {
      AIFootBaller cOpp = super.m_cStrategy.GetFBActiveDistance(this);
      this.m_bTakingBallFromKey = true;
      this.m_cAISystem.RequestEvent(this, 6, cOpp);
   }

   void ResetKickMark() {
      this.m_bKick = false;
      this.m_bHeadShot = false;
   }

   void ForwardKickByHead() {
      Vector vShot = new Vector();
      Vector m_vStrong = new Vector(this.GetForwardVector());
      m_vStrong.Normalize();
      m_vStrong.Mul(super.m_cStrategy.m_fKickForwardByHead);
      this.GetPosition().Add(m_vStrong, vShot);
      GameField.m_fDistToDest = vShot.Distance(this.GetPositionVectorVolatile());
      GameField.m_cBall.KickToLob(vShot, false);
      SoccerModule.m_cMatch.UnblockControlForFocus(vShot);
      SoccerGame.game.GetCommentator().CommentHead(false, false);
   }

   AIFootBaller() {
      this.m_cDecContext = new DecisionContext();
      this.m_sParams = "";
      this.m_vRight = new Vector();
      this.m_vRightSlant = new Vector();
      this.m_vForward = new Vector();
      this.m_vLeft = new Vector();
      this.m_vLeftSlant = new Vector();
      this.m_vBack = new Vector();
   }

   void DoSuperFeint() {
      super.m_cTeam.SetSuperFeint(this);
      this.UpdateSpeed();
   }

   void ResetFeint() {
      this.m_cFeint = null;
   }

   boolean IsSpecialGoalDone() {
      return this.m_bSpecialGoalDone;
   }

   public void ResetSuperWallForTeam() {
      super.ResetSuperWallForTeam();
      SoccerModule.GetHud().FlashSuperIconEnd(super.m_nTeam);
      ((AITeam)super.m_cTeam).ResetSuperWall();
      this.m_fSuperWallTimeLeft = 0.0F;
   }

   void RealKick() {
      if (this.m_cKick != null) {
         if (AIMatch.IsMatchState(7)) {
            this.m_cKick.m_vDestPos = SoccerModule.m_cMatch.m_cSpecialDestFB.GetPosition();
         }

         GameField.m_fDistToDest = this.m_cKick.m_vDestPos.Distance(this.GetPositionVectorVolatile());
         if (this.m_cKick.m_bLob) {
            this.m_cDefence = GameField.m_cBall.KickToLob(this.m_cKick.m_vDestPos, this.m_cKick.m_bShoot);
         } else {
            this.m_cDefence = GameField.m_cBall.KickTo(this.m_cKick.m_vDestPos, this.m_cKick.m_bShoot, this.m_cKick.m_bDefence);
         }

         this.m_test_vLastKickBallPos = new Vector(this.m_cKick.m_vDestPos);
         if (this.m_cKick.m_cPassDestFB != null) {
            this.SetPassActive(this.m_cKick.m_cPassDestFB);
         }

         if (this.m_cKick.m_bShoot) {
            this.PlayKickFX();
            this.GoalKeeperNotice();
         }
      }
   }

   void KickToOut() {
      Vector vCornerPos = GameField.cCornersTab[1].GetPosition();
      vCornerPos.fZ += 1000.0F;
      vCornerPos.fX += 300.0F;
      GameField.m_cBall.KickTo(vCornerPos, false, false);
   }

   boolean HasGoal(int nGoalInd) {
      return this.m_cDecContext.m_cGoal != null && this.m_cDecContext.m_cGoal.m_nId == nGoalInd;
   }

   public void NextAnim() {
      this.PlayAnim("stoi_i_sapie", 1);
   }

   boolean HasSpecialGoal() {
      return this.m_bSpecialGoal;
   }

   void CheckHotGoal(int nEventId, int nGoalId, Object cObject, boolean bCondition) {
      if (bCondition && (!this.HasGoal(nGoalId) || this.HasGoal(nGoalId) && !this.GetGoal().IsChanceToReach(this))) {
         if (AISystem.Condition(this)) {
            GameObject.LogLine(this + " Ponawiam ponownie dla " + this.GetGoal());
         }

         this.m_cAISystem.RequestEvent(this, nEventId, cObject);
      }
   }

   void EnableCollisions() {
      Team cTeam = null;
      FootBaller cFB = null;
      if (SoccerModule.m_cMatch != null) {
         for (int i = 0; i < 2; i++) {
            cTeam = SoccerModule.m_cMatch.GetTeam(i);

            for (int j = 0; j < cTeam.GetFootBallerCount(); j++) {
               cFB = cTeam.GetFootBaller(j);
               cFB.SetInteraction(this, true);
               this.SetInteraction(cFB, true);
            }
         }
      }
   }

   float GetReceiveDistance() {
      float fSupport = 0.0F;
      if (SoccerModule.m_cMatch.m_bFirstPass) {
         fSupport = 50.0F;
      }

      return super.m_cStrategy.m_fReceiveRadius + GameField.m_cBall.fBallRadius + super.fEllRadius + fSupport;
   }

   void SetDefence(AIFootBaller.Shot cDefence) {
      this.m_cDefence = cDefence;
   }

   boolean IsDefence() {
      return this.m_cDefence != null && this.IsGoalKeeper();
   }

   boolean IsHelpAttacker() {
      return super.m_cTeam != null ? this == ((AITeam)super.m_cTeam).m_cHelpAttacker : false;
   }

   boolean IsSelDestTracker() {
      return super.m_cTeam != null ? this == ((AITeam)super.m_cTeam).m_cSelDestTracker : false;
   }

   public void OnCreate() {
      SoccerGame.IncFBallers();
      SoccerGame.IncCtx();
      super.OnCreate();
   }

   void OnInputKey(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      super.OnInputKey(nKeyCode, bKeyState, nKeyAscii);
      if (SoccerGame.m_bDebug) {
         if (!bKeyState && !ModSwitch.IsSelectKey(nKeyCode)) { // [mod] touches directes : pas d'animation de debug
            switch (nKeyCode) {
               case 2:
                  if (this.IsUserControled()) {
                     this.PlayAnim("stoi_rozglada_sie", -1);
                  }
                  break;
               case 3:
                  if (this.IsUserControled()) {
                     this.SetFreeTimeType("pech");
                     this.ChangeActivity("Czekaj_aktywnie");
                  }
                  break;
               case 4:
                  if (this.IsUserControled()) {
                     this.SetFreeTimeType("rozgladanie");
                     this.ChangeActivity("Czekaj_aktywnie");
                  }
                  break;
               case 5:
                  if (this.IsUserControled()) {
                     this.SetFreeTimeType("rozgrzewka_pajacyk");
                     this.ChangeActivity("Czekaj_aktywnie");
                  }
                  break;
               case 6:
                  if (this.IsUserControled()) {
                     this.SetFreeTimeType("rozgrzewka_przysiady");
                     this.ChangeActivity("Czekaj_aktywnie");
                  }
                  break;
               case 7:
                  if (this.IsUserControled()) {
                     this.SetFreeTimeType("rozgrzewka_sklony_boki");
                     this.ChangeActivity("Czekaj_aktywnie");
                  }
                  break;
               case 11:
                  if (this.IsUserControled()) {
                     this.PlayAnim("stoi_i_sapie", -1);
                  }
                  break;
               case 16:
                  if (AISystem.Condition(this)) {
                     GameObject.LogLine(
                        this
                           + " fight : "
                           + this.m_cFightOpp
                           + " defence "
                           + this.m_cDefence
                           + " goal "
                           + this.GetGoal()
                           + " reached "
                           + !this.GoalUnreached()
                           + " activity "
                           + this.GetActivity()
                     );
                     GameObject.CrashLog(
                        this
                           + " fight : "
                           + this.m_cFightOpp
                           + " defence "
                           + this.m_cDefence
                           + " goal "
                           + this.GetGoal()
                           + " reached "
                           + !this.GoalUnreached()
                           + " activity "
                           + this.GetActivity()
                           + " \n"
                     );
                     GameObject.LogLine(this + " bBallKicked " + super.bBallKicked + " single up: " + this.m_bHasSingleUpdate);
                     GameObject.CrashLog(this + " bBallKicked " + super.bBallKicked + " single up: " + this.m_bHasSingleUpdate + " \n");
                     GameObject.LogLine(this + " is active " + this.IsActive());
                  }
                  break;
               case 17:
                  if (this.Condition()) {
                     GameObject.LogLine("START ------------->");
                  }
                  break;
               case 20:
                  if (this.IsUserControled()) {
                     this.PlayAnim("stoi_i_sapie", 1);
                     float fAnimTime = this.GetAnimLength("stoi_i_sapie");
                     this.SetAnimMorphTime(0.2F);
                     this.CallMethodIn("NextAnim", fAnimTime - 0.2F);
                  }
                  break;
               case 38:
                  if (this.HasBall()) {
                     this.SettleToShot();
                  }
                  break;
               case 45:
                  if (this.IsActive()) {
                     AITeam cTeam = (AITeam)super.m_cTeam;
                     GameObject.LogLine(" wall pos " + cTeam.m_cSuperWall.GetPosition());
                     cTeam.m_cSuperWall.Show();
                  }
                  break;
               case 47:
                  if (!super.m_bTestKey) {
                     super.m_bTestKey = true;
                  } else {
                     super.m_bTestKey = false;
                  }
            }
         }
      }
   }

   void StrafeLeft_start(Vector vDest) {
      this.ChangeActivity("chodzenie_bokiem_lewo");
      this.SetPhysicsActive(true);
      this.SetPhysicsType(5);
      this.SetSpeed(super.m_cStrategy.GetStrafeDir(this, vDest));
   }

   void SetType(int nType) {
      super.m_nType = nType;
      this.RefreshContext();
   }

   boolean IsActive() {
      return this.IsReferee() ? false : super.m_cTeam.m_cActiveFootBaller == this;
   }

   void StrafeRight_start(Vector vDest) {
      this.ChangeActivity("chodzenie_bokiem_prawo");
      this.SetPhysicsActive(true);
      this.SetPhysicsType(5);
      this.SetSpeed(super.m_cStrategy.GetStrafeDir(this, vDest));
   }

   public void OnFrame() {
      if (this.fPrevTime == 0.0F) {
         this.fPrevTime = SoccerGame.GetSoccerModule().m_Clock.GetMatchTimeF();
      }

      float fTimeDelta = SoccerGame.GetSoccerModule().m_Clock.GetMatchTimeF() - this.fPrevTime;
      this.fPrevTime = SoccerGame.GetSoccerModule().m_Clock.GetMatchTimeF();
      super.OnFrame();
      String sSuper1 = null;
      String sSuper2 = null;
      String sSuper3 = null;
      if (!this.IsReplaying()) {
         if (this.IsSuperFeint()) {
            super.m_cTeam.m_fSuperFeintTimeLeft -= fTimeDelta;
            if (super.m_cTeam.m_fSuperFeintTimeLeft <= 0.0F) {
               super.m_cTeam.m_fSuperFeintTimeLeft = 0.0F;
               super.m_cTeam.ResetSuperFeint();
            }

            sSuper1 = "Feint " + super.m_cTeam.m_fSuperFeintTimeLeft;
         }

         if (this.IsSuperSpeed()) {
            super.m_cTeam.m_fSuperSpeedTimeLeft -= fTimeDelta;
            if (super.m_cTeam.m_fSuperSpeedTimeLeft <= 0.0F) {
               super.m_cTeam.m_fSuperSpeedTimeLeft = 0.0F;
               super.m_cTeam.ResetSuperSpeed();
            }

            sSuper2 = " Speed " + super.m_cTeam.m_fSuperSpeedTimeLeft;
         }

         if (super.m_cTeam != null && ((AITeam)super.m_cTeam).IsSuperWall() && this.IsGoalKeeper()) {
            this.m_fSuperWallTimeLeft -= fTimeDelta;
            if (this.m_fSuperWallTimeLeft <= 0.0F) {
               this.m_fSuperWallTimeLeft = 0.0F;
               this.ResetSuperWallForTeam();
            }

            sSuper3 = " Wall " + this.m_fSuperWallTimeLeft;
         }
      }
   }

   public void OnDestroy() {
      this.m_cFeint = null;
      this.m_cFightOpp = null;
      if (this.m_cShot != null) {
         this.m_cShot.delete();
      }

      this.m_cShot = null;
      if (this.m_cKick != null) {
         this.m_cKick.delete();
      }

      this.m_cKick = null;
      if (this.m_cDefence != null) {
         this.m_cDefence.delete();
      }

      this.m_cDefence = null;
      this.m_cTempSituation = null;
      this.m_cAISystem = null;
      this.m_bSpecialFBDest = null;
      if (this.m_cDecContext != null) {
         this.m_cDecContext.delete();
      }

      this.m_cDecContext = null;
      this.m_cCollObject = null;
      this.m_vValidPosOff = null;
      this.m_vValidPos = null;
      this.m_vStabilizationPos = null;
      this.m_test_vDestPos = null;
      this.m_vPassPosition = null;
      this.m_test_vLastKickBallPos = null;
      super.OnDestroy();
   }

   void DefenceRequest() {
      GameObject.LogLine("defence request ");
      AITeam cTeam = this.GetOppTeam();
      AIFootBaller cGK = (AIFootBaller)cTeam.GetGoalKeeper();
      if (cGK != null && !cGK.IsSuperWall() && !((AITeam)cGK.m_cTeam).m_bSuperWallToCall) {
         this.m_cAISystem.RequestEvent((AIFootBaller)cTeam.GetGoalKeeper(), 13, this.m_cDefence);
      }

      this.m_cShot = null;
      super.m_cTeam.IncreaseShootCounter();
      ModStats.OnShot(this); // [mod] arrêts du gardien
   }

   void SetPassDest() {
      this.m_bPassDest = true;
   }

   boolean IsPassDest() {
      return this.m_bPassDest;
   }

   public void Update() {
      if (this.m_bInited && super.m_bLiving) {
         if (!this.m_bHasSingleUpdate) {
            float fValidPosDist = this.GetDistanceTo(this.m_vValidPos);
            if ((!GameField.m_cBall.IsOutside() || AIMatch.IsMatchState(5) || AIMatch.IsMatchState(4))
               && !this.IsHelpAttacker()
               && !this.IsHelpBallAttacker()
               && !this.IsSelDestTracker()) {
               if (this.IsGoalKeeper()) {
                  if (fValidPosDist > super.m_cStrategy.m_fMaxValidPosDistGK) {
                     this.m_cAISystem.RequestEvent(this, 1, this.m_vValidPos);
                  }
               } else if (fValidPosDist > super.m_cStrategy.m_fMaxValidPosDist) {
                  this.m_cAISystem.RequestEvent(this, 1, this.m_vValidPos);
               }
            }
         }
      }
   }

   void SetParams(AISystem cAI) {
      this.m_cAISystem = cAI;
      super.m_cStrategy = cAI.m_cStrategy;
   }

   boolean HasFocus() {
      return this.IsReferee() ? false : super.m_cTeam.m_cFocusFBaller == this;
   }

   Object GetObject() {
      return this.m_cDecContext.m_cObject;
   }

   void ResetPassDest() {
      this.m_bPassDest = false;
   }

   void ResetStabilization() {
      this.DisableCallMethod("SetStabilization");
      this.m_bWaitingForDelayStab = false;
      this.m_bIsToStabilization = false;
      if (this.HasGoal(20)) {
         this.m_cDecContext.m_cGoal = null;
      }
   }

   void SetCollObject(ControlObject cObj) {
      this.m_cCollObject = cObj;
   }

   boolean Condition() {
      return AISystem.Condition(this);
   }

   boolean CanGoalBreak() {
      return this.m_cDecContext.m_cGoal == null || this.m_cDecContext.m_cGoal.CanBeBreak() || this.m_cDecContext.m_cGoal.m_bReached;
   }

   void SetFight(AIFootBaller cFB) {
      this.m_cFightOpp = cFB;
   }

   boolean IsFight() {
      return this.m_cFightOpp != null;
   }

   void DisableCollisions() {
      Team cTeam = null;
      FootBaller cFB = null;
      if (SoccerModule.m_cMatch != null) {
         for (int i = 0; i < 2; i++) {
            cTeam = SoccerModule.m_cMatch.GetTeam(i);

            for (int j = 0; j < cTeam.GetFootBallerCount(); j++) {
               cFB = cTeam.GetFootBaller(j);
               cFB.SetInteraction(this, false);
               this.SetInteraction(cFB, false);
            }
         }
      }
   }

   void AttackKick() {
      Vector vShotDir = this.GetForwardVector();
      vShotDir.Normalize();
      vShotDir.Mul(super.m_cStrategy.m_fAttackKickStength);
      Vector vDest = new Vector(0.0F, 0.0F, 0.0F);
      this.GetPosition().Add(vShotDir, vDest);
      GameField.m_fDistToDest = 300.0F;
      GameField.m_cBall.KickTo(vDest, false, false);
      GameField.m_cBall.m_cPrevOwner = this;
      SoccerModule.m_cMatch.UnblockControlForFocus(vDest);
   }

   void Defence(String sType) {
      this.SetContext("Typ_reakcji", sType);
      this.ChangeActivity("Reakcja_z_upadkiem");
   }

   boolean KickedExpired() {
      return this.IsKickMark() ? this.m_fKickMarkTime + 1.5 < this.GetTime() : false;
   }

   public String toString() {
      String sActive = "";
      if (this.m_cDecContext.m_bActive) {
         sActive = sActive + "(A)";
      }

      return ">" + super.m_sIndividualName + ":" + super.m_cTeam + " " + sActive + ">";
   }

   boolean IsUserControled() {
      return super.m_cTeam != null ? super.m_cTeam.m_bUserControled : false;
   }

   void KickStrongForward() {
      this.m_cKick = new AIFootBaller.Kick(this);
      this.m_cKick.m_bShoot = false;
      this.m_cKick.m_bDefence = false;
      this.m_cKick.m_bLob = true;
      Vector vShot = new Vector();
      Vector m_vStrong = this.GetForwardVector();
      m_vStrong.Normalize();
      float fStrong = 1700.0F;
      m_vStrong.Mul(fStrong);
      this.GetPosition().Add(m_vStrong, vShot);
      this.m_cKick.m_vDestPos = vShot;
      this.RequestKickShowing("KickStrongForward");
      this.SetKickMark();
   }

   void KickGoalKeeperForward() {
      Vector vDest = GameField.cCenter.GetPosition();
      vDest.Sub(this.GetPosition());
      float fDist = vDest.Len();
      vDest.Normalize();
      vDest.Mul(fDist / 2.0F);
      Vector vResult = new Vector();
      vDest.Add(this.GetPosition(), vResult);
      this.m_cKick = new AIFootBaller.Kick(this);
      this.m_cKick.m_bShoot = false;
      this.m_cKick.m_bDefence = false;
      this.m_cKick.m_bLob = true;
      this.m_cKick.m_vDestPos = vResult;
      this.RequestKickShowing("KickGoalKeeperForward");
      this.SetKickMark();
   }

   public void SetDir(Vector vDir) {
      if (vDir.fY > 0.0F) {
         vDir.fY = 0.0F;
      }

      super.SetDir(vDir);
   }

   boolean IsHelpBallAttacker() {
      return super.m_cTeam != null ? this == ((AITeam)super.m_cTeam).m_cHelpBallAttacker : false;
   }

   void PlayKickFX() {
      Vector vFXPos = this.GetPosition();
      Vector vBallDir = GameField.m_cBall.GetPosition();
      vBallDir.fY = vBallDir.fY - GameField.m_cBall.fBallRadius;
      float fBallDistance = vBallDir.Distance(vFXPos);
      vBallDir.Sub(this.GetPosition());
      vBallDir.Normalize();
      vBallDir.Mul(fBallDistance - GameField.m_cBall.fBallRadius);
      vFXPos.Add(vBallDir);
      this.ExecuteFX("FXKick", vFXPos);
   }

   void DetachBall() {
      Ball cBall = GameField.m_cBall;
      if (!super.bBallKicked) {
         this.SetBallInteraction();
      }

      cBall.BallDetachFromParentAndKeepWorldPos();
      cBall.SetNewOwner(this, null);
   }

   void ReceiveAction() {
   }

   float GetSituation(Vector vPos) {
      int nZoneVertical = GameField.GetZoneForPointVertical(vPos);
      int nZoneForPoint = GameField.GetZoneForPoint(vPos, super.m_nTeam);
      if (nZoneVertical != 6 && nZoneVertical != 5 && nZoneForPoint != 3 && nZoneForPoint != 4) {
         this.m_cTempSituation.m_fFreedom = super.m_cStrategy.CalculateFreedom(this, vPos);
         super.m_cStrategy.CalculateOpportunityParams(this, vPos, this.m_cTempSituation);
         this.m_cTempSituation.Calculate(this);
         return this.m_cTempSituation.GetValue();
      } else {
         this.m_cTempSituation.m_fFreedom = 0.0F;
         this.m_cTempSituation.m_fOpportunity = 0.0F;
         this.m_cTempSituation.m_fAccess = 0.0F;
         this.m_cTempSituation.m_fSituation = -1.0F;
         return this.m_cTempSituation.GetValue();
      }
   }

   void ShotAction() {
      if (this.HasBall()) {
         this.SetShot(null);
         this.Shot();
      } else if (GameField.m_cBall.IsDestReached()) {
         this.TakeBall();
      }
   }

   void UpdateSpeed() {
      if (super.m_currAnim != null) {
         this.SetLocalSpeed(new Vector(0.0F, 0.0F, super.m_currAnim.m_fSpeed));
      }
   }

   public void ResetSuperSpeed() {
      this.m_bSuperSpeed = false;
      this.SetLocalSpeed(this.GetLocalSpeed());
   }

   void SetSpecialGoal() {
      this.m_bSpecialGoal = true;
   }

   boolean IsActiveLoss() {
      return SoccerModule.m_cMatch.m_cActiveLossFB == this;
   }

   void SetKickMark() {
      this.m_bKick = true;
      this.m_fKickMarkTime = this.GetTime();
   }

   boolean IsKickMark() {
      return this.m_bKick;
   }

   void ResetIgnoreCollision() {
      this.m_bIgnoreCollision = false;
   }

   void ClearAI(boolean bResetSupers) {
      this.m_cDecContext.m_nBallPossesor = 0;
      this.m_cDecContext.m_cObject = null;
      this.m_cDecContext.m_cGoal = null;
      this.m_cDecContext.m_cEvent = null;
      this.m_bSuperShotMovement = false;
      this.m_cShot = null;
      this.ResetFight();
      this.ResetFeint();
      this.StopIdle();
      this.SetFlag(32);
      this.ResetKickMark();
      this.ResetDefence();
      this.ResetPassDest();
      this.ResetSpecialGoal();
      this.ResetIgnoreCollision();
      super.m_bKeySpeed = false;
      super.m_bKeyFallSpeed = false;
      super.m_bKeyRest = false;
      this.ClearSensKeys();
      if (bResetSupers) {
         this.ResetSuperFeint();
         this.ResetSuperSpeed();
      }

      this.m_bSituationFixed = false;
      this.ResetBallKicked();
      if (this.IsStabilization()) {
         this.ResetStabilization();
      }

      this.ChangeActivity("Stanie");
   }

   void PlaySuperSpeedFX() {
      Vector vBack = this.GetForwardVector();
      vBack.Normalize();
      vBack.Mul(-100.0F);
      Vector vFXPos = new Vector();
      Vector vOff = new Vector(vBack);
      vOff.Mul(0.5F);
      this.GetPosition().Add(vOff, vFXPos);
      vFXPos.fY += 35.0F;
      this.ExecuteFX("FXSuperSpeed", vFXPos);
   }

   public void ChangeActivity(String sName) {
      super.ChangeActivity(sName);
   }

   void SetFeint(AIFootBaller.Feint cFeint) {
      this.m_cFeint = cFeint;
   }

   public void SetSuperWallForTeam() {
      super.SetSuperWallForTeam();
      SoccerModule.GetHud().FlashSuperIcon(super.m_nTeam);
      ((AITeam)super.m_cTeam).SetSuperWall();
      this.m_fSuperWallTimeLeft = super.m_cStrategy.m_fSuperDefenceTime;
   }

   public void InitActivityGraph() {
      if (this.m_nAnimalType != -1) {
         GameObject.LogLine(EAnimals.GetAnimalName(this.m_nAnimalType, false) + " : InitAcitvityGraph \n");
      }

      String sSpecName = null;
      String sSpecFileName = null;
      if (super.m_nType == 2) {
         sSpecFileName = EAnimals.GetAnimalScrFile(true);
         sSpecName = EAnimals.m_sSpecNameGK;
      } else {
         sSpecFileName = EAnimals.GetAnimalScrFile(false);
         sSpecName = EAnimals.m_sSpecName;
      }

      Species cSpecies = FootBaller.GetSpecies(sSpecName);
      if (cSpecies == null) {
         cSpecies = this.AddSpecies(sSpecFileName);
      }

      if (cSpecies != null) {
         this.SetSpecies(cSpecies);
      }
   }

   void SetStabilizationWithDelay() {
      this.DisableCallMethod("SetStabilization");
      this.m_bWaitingForDelayStab = true;
      this.m_bIsToStabilization = true;
      float fRnd = GameObject.RandomFloat(1.0F, super.m_cStrategy.m_fStabilizationWithDelay);
      this.CallMethodIn("SetStabilization", fRnd);
   }

   void ChangeDirectionLogic(FootBaller.DirAction cDirAction) {
      super.ChangeDirectionLogic(cDirAction);
   }

   void OnPhysicsUpdate(float fAbsoulteTime, float fSimInterval) {
      if (!this.HasBall()) {
         this.LookTowards(GameField.m_cBall);
      } else {
         this.LookTowards(null);
      }

      if (AIMatch.IsMatchState(0)) {
         if (this.IsSuperSpeed() && this.IsActivity("Bieganie")) {
            this.PlaySuperSpeedFX();
            if (this.m_nSuperSpeedSound == -1 && !SoccerGame.GetSoccerModule().m_bMenuVisible) {
               this.m_nSuperSpeedSound = this.AttachSound("data/sounds/superspeed.wav");
            }
         } else if (this.m_nSuperSpeedSound != -1) {
            this.DetachSound(this.m_nSuperSpeedSound);
            this.m_nSuperSpeedSound = -1;
         }

         if (this.IsSuperFeint() && this.HasBall()) {
            if (this.m_nSuperFeintSound == -1 && !SoccerGame.GetSoccerModule().m_bMenuVisible) {
               this.m_nSuperFeintSound = this.AttachSound("data/sounds/superzwod.wav");
            }
         } else if (this.m_nSuperFeintSound != -1) {
            this.DetachSound(this.m_nSuperFeintSound);
            this.m_nSuperFeintSound = -1;
         }
      } else {
         if (this.m_nSuperSpeedSound != -1) {
            this.DetachSound(this.m_nSuperSpeedSound);
            this.m_nSuperSpeedSound = -1;
         }

         if (this.m_nSuperFeintSound != -1) {
            this.DetachSound(this.m_nSuperFeintSound);
            this.m_nSuperFeintSound = -1;
         }
      }

      if (!this.IsReferee()) {
         SoccerModule.GetHud().UpdateFBPosition(this);
      }

      Ball ball = GameField.m_cBall;
      Vector vBallPos = ball.GetPosition();
      Vector vPos = this.GetPosition();
      super.fFlatBallDistance = ball.GetPositionLog().Distance(this.GetPositionLog());
      if (!this.IsStabilization() && AIMatch.IsMatchState(0) || this.HasSpecialGoal()) {
         if (!AIMatch.IsMatchState(1) && !SoccerModule.m_cMatch.m_bMatchEnding) {
            float fScopeUp = 20.0F;
            if (this.IsGoalKeeper()
               && super.fFlatBallDistance <= super.fCanAttachBallDistance
               && !ball.HasOwner()
               && vBallPos.fY > vPos.fY - ball.fBallRadius * 2.0F
               && vBallPos.fY <= vPos.fY + super.fHeight + ball.fBallRadius * 2.0F + fScopeUp
               && this.IsDefence()
               && !this.m_bBumped) {
               this.BumpDefenceBall();
               SoccerGame.game.GetCommentator().CommentDefence(this);
            } else {
               fScopeUp = 0.0F;
               if (this.m_bPassDest && !GameField.m_cBall.m_bLastWasHead) {
                  fScopeUp = super.m_cStrategy.m_fRecieveScopeUp;
               }

               boolean bTest1 = super.fFlatBallDistance <= this.GetReceiveDistance();
               boolean bTest2 = vBallPos.fY <= vPos.fY + super.fHeight + ball.fBallRadius * 2.0F + fScopeUp;
               if (super.fFlatBallDistance <= this.GetReceiveDistance() && vBallPos.fY <= vPos.fY + super.fHeight + ball.fBallRadius * 2.0F + fScopeUp) {
                  if (!super.bBallKicked && !ball.IsKicked(this) && !ball.HasOwner() && !this.IsActiveLoss() && !this.Busy()) {
                     if (PROFILE_BallReceive == 0) {
                        PROFILE_BallReceive = this.ProfileAdd("Soccer", "Ball receive");
                     }

                     this.ProfileBegin(PROFILE_BallReceive);
                     this.m_nBallAttachedAfterCol = true;
                     this.m_cAISystem.RequestEvent(this, 14, null);
                     this.ProfileEnd(PROFILE_BallReceive);
                  }
               } else if ((super.bBallKicked || this.m_bBumped)
                  && (super.fFlatBallDistance > this.GetReceiveDistance() * 1.5F || !this.m_bLastBallTouched || ball.GetLossTime() > 2.0F)) {
                  this.ResetBallKicked();
                  this.m_bBumped = false;
               }

               if (super.bBallKicked && ball.GetLossTime() > 2.0F) {
                  this.ResetBallKicked();
               }
            }
         }
      }
   }

   void PlayHitBySuperBallFX() {
      this.ExecuteFX("FXHit", this.GetPosition());
   }

   boolean GoalUnreached() {
      return this.m_cDecContext.m_cGoal != null && !this.m_cDecContext.m_cGoal.m_bReached;
   }

   public void UpdateParams() {
      if (AIMatch.IsMatchState(0)) {
         if (!this.IsUserControled()
            && this.IsActive()
            && GameField.m_cBall.HasOwner()
            && this.IsActivity("Bieganie")
            && this.CanKeySpeed()
            && this.GetZone() != 0) {
            this.SetKeySpeed();
         }

         this.CheckKeySpeed();
      }

      if (SoccerGame.m_bDebug && !this.GoalUnreached()) {
         this.SetLocalDest(Vector.Zero);
      }

      this.m_bHasSingleUpdate = false;
      this.SetFlag(8);
      if (this.IsPassDest() && GameField.m_cBall.HasOwner()) {
         this.ResetPassDest();
      }

      if (this.m_cCollObject != null && !super.m_cStrategy.InActiveDistance(this, this.m_cCollObject.GetPosition())) {
         this.ResetCollObject();
      }

      if (this.IsKickMark()
         && (
            GameField.m_cBall.m_cPrevOwner != this
               || !super.m_cStrategy.InActiveDistance(this, GameField.m_cBall.GetPosition())
               || GameField.m_cBall.m_cCurOwner != null && !this.Busy()
         )) {
         this.ResetKickMark();
      }

      if (this.KickedExpired()) {
         this.ResetKickMark();
      }

      if (this.m_bLastBallTouched && GameField.m_cBall.HasOwner() && GameField.m_cBall.GetOwner() != this) {
         this.m_bLastBallTouched = false;
      }

      if (this.m_cDecContext.m_cGoal != null && !this.m_cDecContext.m_cGoal.m_bReached && this.m_cDecContext.m_cGoal.GoalTest(this)) {
         this.m_cDecContext.m_cGoal.m_bReached = true;
         if (!this.HasFocus()) {
            this.m_cDecContext.m_cGoal.ActionAfterReached(this);
         }
      }

      if (this.IsDefence() && GameField.m_cBall.IsDestReached() && !this.HasBall() && (!this.HasGoal(17) || !this.GoalUnreached())) {
         this.ResetDefence();
      }

      if (this.IsFight() && (!this.HasGoalFight() || !this.GoalUnreached()) && (!super.m_cStrategy.IsFightActiveDistance(this) || this.HasGoal(13))) {
         this.ResetFight();
      }

      if (this.m_cFeint != null && !this.HasGoal(12) && !this.HasGoal(13)) {
         this.ResetFeint();
      }

      if (!this.IsReferee()
         && !this.IsGoalKeeper()
         && !this.IsHelpBallAttacker()
         && !this.IsHelpAttacker()
         && (!this.IsActive() || this.HasBall())
         && !this.IsOutside()
         && !this.HasSpecialGoal()
         && !this.IsStabilization()
         && this.m_bInited
         && super.m_bLiving
         && !this.HasFocus()
         && !SoccerModule.m_cMatch.IsWaiting()
         && this != SoccerModule.m_cMatch.m_cLastSpecialFB) {
         Vector vForward = this.GetForwardVector();
         vForward.Normalize();
         if (this.IsSuperSpeed()) {
            vForward.Mul(super.m_cStrategy.m_fNewPosDist * 1.5F);
         } else {
            vForward.Mul(super.m_cStrategy.m_fNewPosDist);
         }

         Vector vForwardPos = new Vector();
         vForward.Add(this.GetPositionVectorVolatile(), vForwardPos);
         if (!super.m_cStrategy.IsForwardInside(this, vForwardPos)
            || this.IsSuperSpeed() && GameField.GetZoneForPoint(vForwardPos, super.m_nTeam) != 2 && this.GetZone() == 2) {
            if (this.HasBall()) {
               this.m_cAISystem.RequestEvent(this, 4, null);
            } else if (!this.Busy()) {
               if ((this.HasGoal(21) || this.HasGoal(16)) && this.GoalUnreached()) {
                  this.GetGoal().m_bReached = true;
                  this.GetGoal().ActionAfterReached(this);
               } else {
                  this.ChangeActivity("Stanie");
               }
            }
         }
      }

      if ((!AIMatch.IsMatchState(0) || SoccerModule.m_cMatch.IsWaiting() || AIMatch.IsMatchState(0) && this.IsReferee())
         && !this.IsStabilization()
         && !this.GoalUnreached()
         && !this.HasSpecialGoal()
         && SoccerModule.m_cMatch.m_cSpecialFB != this
         && !this.HasFocus()
         && super.m_bAnimationFinished
         && (!this.IsReferee() || !AIMatch.IsMatchState(1))) {
         this.m_cAISystem.RequestEvent(this, 21, null);
      }

      if (!this.IsUserControled() && AIMatch.IsMatchState(0) && this.HasBall() && this.GetZone() == 2) {
         AIFootBaller cGK;
         if (super.m_nTeam == 0) {
            cGK = (AIFootBaller)SoccerModule.m_cMatch.GetTeam(1).m_cGoalKeeper;
         } else {
            cGK = (AIFootBaller)SoccerModule.m_cMatch.GetTeam(0).m_cGoalKeeper;
         }

         if (cGK.Busy()) {
            this.m_cAISystem.RequestEvent(this, 9, null);
         }
      }
   }

   void Idle(float fTime) {
      if (!this.m_bIdle) {
         this.StartIdle();
         this.DisableCallMethod("StopIdle");
         this.CallMethodIn("StopIdle", fTime);
      }
   }

   void CalculateParams() {
      this.m_cDecContext.m_cSituation = this.CalculateSituation(this.GetPosition());
      if (this.IsSuperSpeed() || this.IsSuperFeint()) {
         this.m_cDecContext.m_cSituation.m_fSituation = 2.0F;
      }

      if (!this.HasBall() && !this.IsActive()) {
         this.m_vPassPosition = super.m_cStrategy.CalculatePassPosition(this);
         if (this.m_vPassPosition != null) {
            this.m_fPassPositionSituation = this.CalculateSituation(this.m_vPassPosition).GetValue();
         } else {
            this.m_fPassPositionSituation = -1.0F;
         }
      } else {
         this.m_vPassPosition = null;
         this.m_fPassPositionSituation = -1.0F;
      }

      this.m_bSituationFixed = true;
   }

   boolean Busy() {
      return this.IsFight() || this.IsDoingFeint() || this.IsIdle();
   }

   void ClearBallInteraction() {
      this.SetInteraction(GameField.m_cBall, false);
      GameField.m_cBall.SetInteraction(this, false);
   }

   Situation CalculateSituation(Vector vPos) {
      Situation cSituation = new Situation();
      if (GameField.GetZoneForPointVertical(vPos) != 6
         && GameField.GetZoneForPointVertical(vPos) != 5
         && GameField.GetZoneForPoint(vPos, super.m_nTeam) != 3
         && GameField.GetZoneForPoint(vPos, super.m_nTeam) != 4) {
         cSituation.m_fFreedom = super.m_cStrategy.CalculateFreedom(this, vPos);
         super.m_cStrategy.CalculateOpportunityParams(this, vPos, cSituation);
         cSituation.Calculate(this);
         return cSituation;
      } else {
         cSituation.m_fFreedom = 0.0F;
         cSituation.m_fOpportunity = 0.0F;
         cSituation.m_fAccess = 0.0F;
         cSituation.m_fSituation = -1.0F;
         return cSituation;
      }
   }

   void SetStabilization() {
      this.m_bWaitingForDelayStab = false;
      this.m_bIsToStabilization = true;
   }

   boolean IsStabilization() {
      return this.m_bIsToStabilization;
   }

   void DoSuperSpeed() {
      super.m_cTeam.SetSuperSpeed(this);
      this.UpdateSpeed();
   }

   // [mod] note le type de super déclenché, pour les succès (le compteur du jeu augmente quand le super part)
   void UseSuper() {
      Team cTeam = super.m_cTeam;
      int nType = cTeam.m_nSuperType;
      int nCount = cTeam.m_nSuperCounter;
      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("[mod] UseSuper " + cTeam.m_sName + " (cote " + cTeam.m_nSide + ") : dispo " + cTeam.m_bSuperAvailable + ", type " + cTeam.m_nSuperType + ", lettres " + cTeam.m_nSuperFillCount + "\n");
      }

      this.UseSuperType();
      if (cTeam.m_nSuperCounter > nCount) {
         ModStats.OnSuper(cTeam, nType);
      }
   }

   // [mod] supers 4..7 (gel, ballon fou, aimant, tir double) gérés par ModSupers ; 0..3 = FootBaller.UseSuper d'origine
   void UseSuperType() {
      Team cTeam = super.m_cTeam;
      if (!cTeam.m_bSuperAvailable || !ModSupers.IsModSuper(cTeam.m_nSuperType)) {
         super.UseSuper();
      } else if (ModSupers.Use(this, cTeam.m_nSuperType)) {
         boolean bWithBall = ModSupers.NeedsBall(cTeam.m_nSuperType);
         cTeam.ResetSuper();
         cTeam.IncreaseSuperCounter();
         SoccerModule.m_cMatch.m_bWasSuperCommeneted = false;
         SoccerModule.m_cMatch.m_bSuperCommentWithBaller = !bWithBall;
      }
   }

   public void SetPositionWithY(Vector vPos) {
      super.SetPosition(vPos);
   }

   public void MoveSuperWallToInfinity() {
      if (SoccerModule.m_cMatch.m_cSpecialFB != this) {
         this.ResetSpecialGoal();
      }

      if (super.m_cTeam != null) {
         super.m_cTeam.ResetSuperDoing();
      }
   }

   void RequestKickShowing(String sKickType) {
      this.m_sKickType = sKickType;
      this.m_sIdleBeforeKick = this.IsIdle();
      this.m_cAISystem.RequestEvent(this, 22, null);
   }

   void RefreshContext() {
      this.m_cDecContext.m_nAIFootBallerType = super.m_nType;
   }

   String GetTextToShow() {
      String sResult = this.toString() + " i: " + this.m_bIdle;
      if (this.GetGoal() != null) {
         sResult = sResult + " g: " + this.GetGoal();
      } else {
         sResult = sResult + " g: " + null;
      }

      return sResult;
   }

   void Pass(AIFootBaller cFBaller, Vector vPassDest, boolean bLob) {
      if (cFBaller != null) {
         ModStats.OnPass(this); // [mod] passes tentées
         vPassDest = ModTactics.LongPassError(this, vPassDest); // [mod] passes longues moins précises
         float fDistance = this.GetPosition().Distance(cFBaller.GetPosition());
         GameField.m_fDistToDest = vPassDest.Distance(this.GetPositionVectorVolatile());
         if (super.m_cStrategy.PassAccessibility(this, cFBaller, cFBaller.GetPosition()) > super.m_cStrategy.m_fMinPassAcces
            && !this.HasToHeadLob()
            && !bLob
            && (AIMatch.IsMatchState(0) || AIMatch.IsMatchState(4))) {
            GameObject.LogLine(this + " pass to " + cFBaller + " dest " + vPassDest);
            GameField.m_cBall.KickTo(vPassDest, false, false);
            if (SoccerGame.game.GetCommentator().TossHalf(75.0F) && this.GetPosition().Distance(cFBaller.GetPosition()) > 500.0F && !this.HasEvent(8)) {
               SoccerGame.game.GetCommentator().CommentPass(this, cFBaller);
            }
         } else {
            GameField.m_cBall.KickToLob(vPassDest, false);
            if (SoccerGame.game.GetCommentator().TossHalf(75.0F) && this.GetPosition().Distance(cFBaller.GetPosition()) > 600.0F && !this.HasEvent(8)) {
               SoccerGame.game.GetCommentator().CommentLobPass(this, cFBaller);
            }
         }

         this.SetPassActive(cFBaller);
         ModMovement.OnPass(this, cFBaller); // [mod] fin de l'appel du receveur, une-deux du passeur
         SoccerModule.m_cMatch.UnblockControlForFocus(vPassDest);
         if (!this.m_bHeadShot) {
            this.SetNoDone();
            this.ChangeActivity("Strzal");
            this.SetKickMark();
         }
      }
   }

   void DoSuperShot() {
      GameField.m_cBall.DoSuperShot(this);
      super.m_cTeam.IncreaseShootCounter();
      ModStats.OnShot(this); // [mod] arrêts du gardien
   }

   static class Feint {
      AIFootBaller m_cOpponent = null;
      Vector m_vDestPos = null;
      Vector m_vFeintPos = null;
      boolean m_bSucces = false;

      boolean IsSucces() {
         return this.m_bSucces;
      }

      void delete() {
         this.m_cOpponent = null;
         this.m_vDestPos = null;
         this.m_vFeintPos = null;
      }
   }

   // [mod] artefact de décompilation (this$0) : Kick n'utilise pas son instance englobante
   static class Kick {
      Vector m_vDestPos;
      boolean m_bLob;
      boolean m_bShoot;
      boolean m_bDefence;
      AIFootBaller m_cPassDestFB;

      Kick(AIFootBaller cOwner) {
         this.m_vDestPos = null;
         this.m_bLob = false;
         this.m_bShoot = false;
         this.m_bDefence = false;
         this.m_cPassDestFB = null;
      }

      void delete() {
         this.m_vDestPos = null;
         this.m_cPassDestFB = null;
      }
   }

   static class Pass {
      AIFootBaller m_cDestFB = null;
      Vector m_vDestPos = null;

      Pass(AIFootBaller cDestFB, Vector vDestPos) {
         this.m_cDestFB = cDestFB;
         this.m_vDestPos = vDestPos;
      }

      void delete() {
         this.m_cDestFB = null;
         this.m_vDestPos = null;
      }
   }

   static class Shot {
      Vector m_vStartPos = null;
      Vector m_vDest = null;
      Vector m_vDir = null;
      FootBaller.DirAction m_cDirAction = null;
      float m_fAngle = 0.0F;
      float fSpeed;
      boolean m_bSucces = false;
      int m_nReactType = -1;

      void CopyTo(AIFootBaller.Shot cShot) {
         cShot.m_vStartPos = new Vector(this.m_vStartPos);
         cShot.m_vDest = new Vector(this.m_vDest);
         cShot.m_vDir = new Vector(this.m_vDir);
         cShot.fSpeed = this.fSpeed;
         cShot.m_fAngle = this.m_fAngle;
      }

      void delete() {
         this.m_vStartPos = null;
         this.m_vDest = null;
         this.m_vDir = null;
         if (this.m_cDirAction != null) {
            this.m_cDirAction.delete();
         }

         this.m_cDirAction = null;
      }
   }
}
