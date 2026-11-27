public class Ball extends FieldObject {
   Recorder m_cRecorder = null;
   static String[] m_sSoundsPassBall = new String[]{"Kick01_1.wav", "Kick01_2.wav", "Kick01_3.wav", "Kick01_4.wav", "Kick01_5.wav"};
   static String[] m_sSoundsBounceOnGrass = new String[]{"Bounce02_1.wav", "Bounce02_2.wav", "Bounce02_3.wav"};
   static String[] m_sSoundsShotBall = new String[]{"Kick02.wav"};
   static final int _OUT_TOP = 0;
   static final int _OUT_BOTTOM = 1;
   static final int _CORNER_LEFT = 2;
   static final int _CORNER_RIGHT = 3;
   static final String[] m_sSuperShotSounds = new String[]{"data/sounds/superpilka.wav", "data/sounds/superpilka2.wav", "data/sounds/superpilka3.wav"};
   int m_nSuperShotSound = -1;
   boolean m_bSuperShot = false;
   Vector m_vSuperShotDest = new Vector();
   float m_fSuperShotSpeedup = 0.25F;
   float fSuperShotPowerMul = 1.2F;
   float m_fSuperShotSlowDownDistance = -1.0F;
   AIFootBaller m_cSuperShotFB = null;
   boolean m_bShooted = false;
   boolean m_bLastLobbed = false;
   AIFootBaller m_cPrevOwner = null;
   AIFootBaller m_cCurOwner = null;
   float m_fAttachTime = 0.0F;
   float m_fLossTime = 0.0F;
   boolean m_bLastWasHead = false;
   float fFrictionDeceleration = 500.0F;
   float fBallRadius = -1.0F;
   float fMinKickSpeed = 600.0F;
   float fMaxKickSpeed = 2000.0F;
   int m_nMaxBounces = 0;
   int m_nBounces = 0;
   boolean m_bReachedDestination = false;
   float m_fReachPathLen = -1.0F;
   Vector m_vStartPos = null;
   boolean m_bHasOwner = false;
   boolean m_bWasCorner = false;
   boolean m_bWasOut = false;
   boolean m_bWasGoal = false;
   int m_nTeamGoal = -1;
   static final int _BOUNCE_FOOTBALLER = 0;
   static final int _BOUNCE_GRASS = 1;
   static final int _BOUNCE_OBJECT = 2;
   static final int _BOUNCE_NET = 3;
   static final int _BOUNCE_WOOD = 4;
   static final int _BOUNCE_BUSH = 5;
   static final int _BOUNCE_WATCHER = 6;
   float m_fMinSpeedToBounce = 400.0F;
   Vector m_vPosAttached = null;
   boolean m_bSloMo = false;
   boolean m_SpecialBallState = false;
   float fXTimeNormal = 0.01F;
   float fXTimeSlow = 0.07F;
   int nFXTimeNormal = 1;
   int nFXTimeSlow = 7;
   float fS = 700.0F;
   float fFs = 1100.0F;
   boolean bMoveByKeys = false;
   String m_sPrevSound = "";
   float m_fPrevSoundStart = 0.0F;
   Vector m_vNormal = null;
   boolean m_bAttached = false;
   static int PROFILE_BallOnPhysics = 0;
   Vector vPrevPos = null;
   boolean bMoving = false;
   float fRotationCoeff = 180.0F;
   Vector vOwnerPrevPos = null;
   float fPrevTime = 0.0F;
   float fLocalSpeedForward = 130.0F;
   float fLocalSpeedBackward = 75.0F;
   float fLocalPosForward = -10.0F;
   float fLocalPosBackward = 20.0F;
   boolean bLocalForward = true;
   boolean bReceiveBall = false;
   float fAmplituda = 30.0F;
   float m_fSpeedup = 1.0F;

   float GetPossesTime() {
      return this.GetTime() - this.m_fAttachTime;
   }

   void UpdateFeetDance() {
      if (this.HasOwner() && !this.IsReplaying()) {
         if (!this.GetOwner().bBallKicked) {
            float fTime = this.GetTime();
            Vector vOwnerPos = this.m_cCurOwner.GetPositionVectorVolatile();
            if (this.vOwnerPrevPos == null) {
               this.fPrevTime = this.GetTime();
               this.vOwnerPrevPos = new Vector(vOwnerPos);
               this.bLocalForward = false;
               this.bReceiveBall = true;
            }

            float fPosDelta = vOwnerPos.Distance(this.vOwnerPrevPos);
            if (fPosDelta < 0.1) {
               if (AIMatch.IsMatchState(0)) {
                  this.SetPhysicsActive(true);
               }

               super.SetSpeed(Vector.Zero);
            } else {
               this.vOwnerPrevPos.Set(vOwnerPos);
               Vector vCurPos = this.m_cCurOwner.PointWorldToLocal(this.GetPositionVectorVolatile());
               float fMinDribbleDistance = this.m_vPosAttached.fZ + this.m_cCurOwner.m_fBallDribbleDistance;
               if (vCurPos.fZ <= fMinDribbleDistance) {
                  this.bLocalForward = true;
                  this.bReceiveBall = false;
                  if (Math.abs(vCurPos.fX) > 1.0F) {
                     vCurPos.fX = 0.0F;
                     this.SetPosition(vCurPos);
                  }
               }

               if (vCurPos.fZ >= this.m_vPosAttached.fZ + this.fAmplituda) {
                  this.bLocalForward = false;
               }

               Vector vOwnerForward = this.m_cCurOwner.GetForwardVectorVolatile();
               if (vOwnerForward.fY < -0.01 || vOwnerForward.fY > 0.01) {
                  Tools.CrashLogWarning("!!Ownerforward.fY == " + vOwnerForward.fY);
               }

               if (this.bLocalForward) {
                  vOwnerForward.Mul(this.fLocalSpeedForward);
                  this.SetSpeed(vOwnerForward);
               } else {
                  vOwnerForward.Negate();
                  if (!this.bReceiveBall) {
                     vOwnerForward.Mul(this.fLocalSpeedBackward);
                  } else {
                     vOwnerForward.Mul(this.m_cCurOwner.GetSpeedValue());
                  }

                  this.SetSpeed(vOwnerForward);
               }
            }
         }
      }
   }

   public void OnFrame() {
      if (this.m_cRecorder != null) {
         if (!this.IsReplaying()) {
            this.m_cRecorder.SnapShot();
         } else if (this.IsReplaying()) {
            if (!this.m_cRecorder.PlaySnapShot()) {
               this.ReplayStop();
            } else {
               this.UpdateRotation(0.0F);
            }
         }
      }
   }

   public void OnDestroy() {
      if (this.m_cRecorder != null) {
         this.m_cRecorder.delete();
      }

      this.m_cRecorder = null;
      this.m_cCurOwner = null;
      this.m_cPrevOwner = null;
      this.m_cSuperShotFB = null;
      ModSupers.OnBallDestroy(); // [mod] le leurre du tir double part avec le module
      GameField.m_cBall = null;
      this.m_vSuperShotDest = null;
      this.m_vStartPos = null;
   }

   void SetSuperShot() {
      this.m_bSuperShot = true;
      this.DisableCollisions();
      this.UpdateFXs();
      int nRnd = GameObject.RandomInt(100);
      nRnd %= 3;
      this.m_nSuperShotSound = this.AttachSound(m_sSuperShotSounds[nRnd]);
   }

   boolean IsSuperShot() {
      return this.m_bSuperShot;
   }

   public void BallAttachAsChildTo(ControlObject cParent) {
      if (!this.IsReplaying()) {
         super.AttachAsChildTo(cParent);
         this.m_bAttached = true;
      }
   }

   boolean IsOutside() {
      Vector vPos = this.GetPositionVectorVolatile();
      return GameField.IsPointOutsideGates(vPos) || GameField.GetZoneForPointVertical(vPos) != 1;
   }

   void SetDestUnreached(Vector vDest) {
      this.m_vStartPos = this.GetPosition();
      this.m_fReachPathLen = vDest.Distance(this.m_vStartPos);
      this.m_bReachedDestination = false;
   }

   float GetLossTime() {
      return this.GetTime() - this.m_fLossTime;
   }

   void OnHitObject(ControlObject obj, Vector vPoint, Vector vNormal) {
      if (obj instanceof FieldGate) {
         if (this.m_bWasGoal) {
            this.PlayBounceSound(3);
         } else if (!GameField.IsPointOutsideGates(this.GetPositionVectorVolatile())) {
            this.PlayBounceSound(2);
         }
      } else if (!(obj instanceof FieldObstacle) && obj instanceof FieldObject) {
         this.PlayBounceSound(2);
      }

      if (this.m_nBounces == 1) {
         this.m_nMaxBounces = 2;
      }

      float fSpeed = this.GetSpeedValue();
      if (this.m_bWasGoal) {
         fSpeed *= 0.16666667F;
      } else if (obj instanceof FieldObstacle) {
         if (SoccerGame.GetSoccerModule().m_nLevelID == 16) {
            fSpeed *= 0.75F;
            if (!this.HasOwner()) {
               this.PlayBounceSound(4);
            }
         }

         if (SoccerGame.GetSoccerModule().m_nLevelID == 17) {
            if (!this.IsSuperShot()) {
               fSpeed *= 0.15F;
            } else {
               fSpeed *= 0.75F;
            }

            if (!this.HasOwner()) {
               this.PlayBounceSound(5);
            }
         }
      } else {
         fSpeed *= 0.33333334F;
         if (obj instanceof Watcher) {
            this.PlayBounceSound(6);
         }
      }

      if (!this.HasOwner()) {
         this.SetSpeedValue(fSpeed);
      }
   }

   void UpdateRotation(float fTime) {
      Vector vPos = this.GetPositionVectorVolatile();
      if (this.vPrevPos == null) {
         this.vPrevPos = new Vector(vPos);
      }

      Vector vSpeed = vPos.Sub2(this.vPrevPos);
      float fRoute = this.GetDistanceTo(this.vPrevPos);
      if (fRoute <= 0.3F) {
         this.bMoving = false;
         if (fRoute <= 0.1F) {
            float fTerrainHeight = this.TerrainHeight(vPos.fX, vPos.fZ);
            if (fTerrainHeight > vPos.fY - this.fBallRadius * 2.0F) {
               this.SetPhysicsActive(false);
            }
         }
      } else {
         this.bMoving = true;
         this.vPrevPos.Set(vPos);
         vSpeed.fY = 0.0F;
         vSpeed.Normalize();
         if (vSpeed.Len() != 0.0F) {
            Vector vAxis = Vector.Cross(vSpeed, Vector.Up);
            vAxis = this.VectorWorldToLocal(vAxis);
            float fRotation = -fRoute / (2.0F * this.fBallRadius * (float) Math.PI);
            float fRotationAngle = fRotation * this.fRotationCoeff;
            this.RotateLocal(vAxis, fRotationAngle);
         }
      }
   }

   public int ExecuteFX(String sFXName, Vector pos, Vector normal) {
      // [mod] en ligne : l'hôte diffuse l'effet, le client ne joue que ceux de l'hôte
      if (!ModNet.OnFX(ModNet._OBJ_BALL, sFXName, pos, normal)) {
         return 0;
      }

      if (this.m_cRecorder != null && !this.IsReplaying()) {
         this.m_cRecorder.SnapShotFX(sFXName);
      }

      return super.ExecuteFX(sFXName, pos, normal);
   }

   void PlayKickSound(boolean bShot) {
      if (bShot) {
         Tools.PlayRandomSound(this, m_sSoundsShotBall);
      } else {
         Tools.PlayRandomSound(this, m_sSoundsPassBall);
      }
   }

   public void PlaySound(String sSound) {
      if (this.m_sPrevSound.equalsIgnoreCase(sSound)) {
         float fSoundLen = this.GetWaveDuration(sSound);
         if (this.GetTime() - this.m_fPrevSoundStart < fSoundLen) {
            return;
         }
      }

      // [mod] en ligne : l'hôte diffuse le son, le client ne joue que ceux de l'hôte
      if (!ModNet.OnSound(ModNet._SND_BALL, null, sSound)) {
         return;
      }

      super.PlaySound(sSound);
      this.m_sPrevSound = sSound;
      this.m_fPrevSoundStart = this.GetTime();
   }

   void CheckTerrain() {
      Vector vPos = this.GetPositionVectorVolatile();
      float fTerrainH = this.TerrainHeight(vPos.fX, vPos.fZ);
      if (fTerrainH > vPos.fY - this.fBallRadius) {
         vPos.fY = fTerrainH + this.fBallRadius + 1.0F;
         if (this.m_bAttached && this.m_bHasOwner) {
            vPos = this.m_cCurOwner.PointWorldToLocal(vPos);
         }

         this.SetPosition(vPos);
      }
   }

   public void SetFallSpeed(Vector v) {
      this.SetPhysicsActive(true);
      super.SetFallSpeed(v);
      if (this.m_cRecorder != null && !this.IsReplaying() && !this.HasOwner()) {
      }
   }

   void OnHitFootballer(FootBaller cFB, Vector vNormal) {
      this.m_cPrevOwner = (AIFootBaller)cFB;
      ModStats.OnTouch((AIFootBaller)cFB); // [mod] arrêts du gardien, tirs contrés
      this.ResetShooted();
      if (this.IsMoving()) {
         AIFootBaller cFBaller = (AIFootBaller)cFB;
         if (this.m_cCurOwner == null) {
            if (!cFBaller.m_bBumped && !this.IsSuperShot()) {
               float fSpeed = this.GetSpeedValue();
               Vector vFallSpeed = this.GetFallSpeed();
               float fFallSpeed = vFallSpeed.Len();
               fSpeed *= 0.16666667F;
               fFallSpeed *= 0.16666667F;
               vNormal.Mul(fSpeed);
               vNormal.fY = 0.0F;
               vFallSpeed.Normalize();
               vFallSpeed.Mul(fFallSpeed);
               if (!this.HasOwner()) {
                  this.SetSpeedValue(vNormal.Len());
                  this.SetFallSpeed(vFallSpeed);
               }
            }

            if ((!cFBaller.IsGoalKeeper() || !cFBaller.IsDefence()) && !cFBaller.m_bBumped) {
               if (AIMatch.IsMatchState(0)) {
                  this.PlayBounceSound(0);
               }

               cFBaller.m_bBumped = true;
            }
         }

         if (this.IsSuperShot()) {
            cFB.ExecuteFX("FXHit", this.GetPositionVectorVolatile());
         }
      }
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

   boolean KickedExpired() {
      return this.m_cPrevOwner != null && this.m_cPrevOwner.bBallKicked ? this.m_cPrevOwner.m_fKickTime + 1.0F < this.GetTime() : false;
   }

   void CheckOutSide() {
      Vector vPos = this.GetPositionVectorVolatile();
      if (this.IsOutside() && vPos.fY < this.TerrainHeight(vPos.fX, vPos.fZ) + 50.0F) {
         float fDist = GameField.cCenter.GetDistanceTo(vPos);
         if (fDist > GameField.GetDiagonalLength() / 2.0F + 200.0F) {
            this.SetSpeed(Vector.Zero);
         }
      }
   }

   void ResetPhysics() {
      this.SetSpeed(Vector.Zero);
      this.SetFallSpeed(Vector.Zero);
      this.SetPhysicsActive(false);
   }

   boolean IsPrevOwner(Team cTeam) {
      return this.m_cPrevOwner != null ? this.m_cPrevOwner.m_cTeam == cTeam : false;
   }

   public void ResetSlomo() {
      this.TimerSetSpeedUp(1.0F);
   }

   public void ResetSuperShot() {
      this.DisableCallMethod("ResetSuperShot");
      this.DisableCallMethod("ResetSlomo");
      if (this.m_bSuperShot) {
         SoccerModule.GetHud().FlashSuperIconEnd(this.m_cPrevOwner.m_nTeam);
      }

      this.EnableCollisions();
      this.m_bSuperShot = false;
      if (!this.m_bWasGoal) {
         this.TimerSetSpeedUp(1.0F);
      } else {
         this.CallMethodIn("ResetSlomo", 1.5F * this.m_fSuperShotSpeedup);
      }

      if (this.m_cSuperShotFB != null) {
         SoccerModule.GetHud().FlashSuperIconEnd(this.m_cSuperShotFB.m_nTeam);
         if (this.m_cSuperShotFB.m_cTeam != null) {
            this.m_cSuperShotFB.m_cTeam.ResetSuperDoing();
         }
      }

      this.m_cSuperShotFB = null;
      if (this.m_nSuperShotSound != -1) {
         this.DetachSound(this.m_nSuperShotSound);
         this.m_nSuperShotSound = -1;
      }
   }

   void CheckDestReached() {
      if ((!GameField.m_cBall.IsMoving() || this.HasOwner()) && !this.IsKicked(null)) {
         GameField.m_cBall.ClearDestReached();
      }

      if (this.m_vStartPos != null && this.GetDistanceTo(this.m_vStartPos) > this.m_fReachPathLen + 100.0F) {
         this.ClearDestReached();
      }
   }

   boolean IsDestReached() {
      return this.m_bReachedDestination;
   }

   public void TimerSetSpeedUp(float f) {
      ModNet.OnSpeedUp(f); // [mod] en ligne
      this.m_fSpeedup = f;
      this.GetModule().TimerSetSpeedUp(ModMeta.Speed(f)); // [mod] méta : temps accéléré
      if (f < 1.0F) {
         this.m_bSloMo = true;
      } else {
         this.m_bSloMo = false;
      }
   }

   void TakeBallEasy() {
      GameObject.LogLine(" TAKE BALL EASY");
      if (this.m_cCurOwner != null) {
         this.m_cCurOwner.DetachBall();
         this.SetSpeed(new Vector(0.0F, 0.0F, 0.0F));
      }

      if (this.m_cPrevOwner != null) {
         this.m_cPrevOwner.ResetBallKicked();
      }

      this.ResetEventFlags();
   }

   void SetNewOwner(AIFootBaller cPrevOwner, AIFootBaller cNewOwner) {
      if (!this.IsReplaying() || cNewOwner == null) {
         if (cPrevOwner != null) {
            this.m_cPrevOwner = cPrevOwner;
         }

         this.m_cCurOwner = cNewOwner;
         if (cNewOwner == null) {
            this.m_bHasOwner = false;
            SoccerModule.m_cMatch.ChangePossesor(this.m_cPrevOwner, null);
            this.m_fLossTime = this.GetTime();
         } else {
            this.m_fAttachTime = this.GetTime();
            this.m_bHasOwner = true;
            this.SetDir(cNewOwner.GetForwardVectorVolatile());
            SoccerModule.m_cMatch.ChangePossesor(this.m_cPrevOwner, this.m_cCurOwner);
         }

         this.vOwnerPrevPos = null;
      }
   }

   FootBaller GetOwner() {
      return this.m_cCurOwner;
   }

   Team GetPrevOwner() {
      return this.m_cPrevOwner == null ? null : this.m_cPrevOwner.m_cTeam;
   }

   void ResetStartPosition() {
      this.DetachFromParentAndKeepWorldPos();
      if (this.m_cCurOwner != null) {
         this.m_cCurOwner.DetachBall();
      }

      this.m_cPrevOwner = null;
      this.m_cCurOwner = null;
      Vector vStartPos = GameField.cCenter.GetPositionVectorVolatile();
      vStartPos.fY = vStartPos.fY + this.fBallRadius;
      this.SetPosition(vStartPos);
      this.SetSpeed(new Vector());
      this.SetFallSpeed(new Vector());
      this.ResetEventFlags();
      this.ResetSuperShot();
      if (SoccerModule.m_cMatch != null) {
         GameField.SetDestPos(GameField.m_cBall.GetPosition(), false);
      }
   }

   void SetSpecialBall() {
      this.m_SpecialBallState = true;
   }

   boolean IsSpecialBall() {
      return this.m_SpecialBallState;
   }

   void WrtiteStateAsCrashLog() {
      GameObject.CrashLog(
         " BALL -------------------------------\n - m_nSuperShotSound "
            + this.m_nSuperShotSound
            + "\n"
            + " - m_bSuperShot "
            + this.m_bSuperShot
            + "\n"
            + " - m_fSuperShotSpeedup "
            + this.m_fSuperShotSpeedup
            + "\n"
            + " - m_cSuperShotFB "
            + this.m_cSuperShotFB
            + "\n"
            + " - m_bShooted "
            + this.m_bShooted
            + "\n"
            + " - m_bLastLobbed "
            + this.m_bLastLobbed
            + "\n"
            + " - m_cPrevOwner "
            + this.m_cPrevOwner
            + "\n"
            + " - m_cCurOwner "
            + this.m_cCurOwner
            + "\n"
            + " - m_fAttachTime "
            + this.m_fAttachTime
            + "\n"
            + " - m_fLossTime "
            + this.m_fLossTime
            + "\n"
            + " - m_bReachedDestination "
            + this.m_bReachedDestination
            + "\n"
            + " - m_vStartPos "
            + this.m_vStartPos
            + "\n"
            + " - m_bHasOwner "
            + this.m_bHasOwner
            + "\n"
            + " - m_bWasCorner "
            + this.m_bWasCorner
            + "\n"
            + " - m_bWasOut "
            + this.m_bWasOut
            + "\n"
            + " - m_bWasGoal "
            + this.m_bWasGoal
            + "\n"
            + " - m_nTeamGoal "
            + this.m_nTeamGoal
            + "\n"
            + " - m_vPosAttached "
            + this.m_vPosAttached
            + "\n"
            + " - m_bSloMo "
            + this.m_bSloMo
            + "\n"
            + " - m_SpecialBallState "
            + this.m_SpecialBallState
            + "\n"
            + " - GameField.m_vDestPosition "
            + GameField.m_vDestPosition
            + "\n"
      );
   }

   public void OnChangeMovement(int nMod) {
      switch (nMod) {
         case 1:
            this.OnHitGround();
         case 0:
      }
   }

   void SetMaxFallSpeed(float fSpeed) {
      this.SetMaxFallSpeed(new Vector(0.0F, fSpeed, 0.0F));
   }

   void OnHitGround() {
      if (!this.IsReplaying()) {
         if (AIMatch.IsMatchState(0)) {
            if (this.m_cCurOwner == null) {
               float fSpeed = this.GetSpeedValue();
               this.PlayBounceSound(1);
               if (this.m_nBounces < this.m_nMaxBounces) {
                  this.SetSpeedValue(this.GetSpeedValue() / 2.0F);
                  this.m_nBounces++;
               } else {
                  this.m_nBounces = 0;
                  this.m_nMaxBounces = 0;
               }
            }
         }
      }
   }

   void UpdateSpeed(float fSimInterval) {
      if (this.m_fSuperShotSlowDownDistance == -1.0F) {
         this.m_fSuperShotSlowDownDistance = 1000.0F;
      }

      if (!this.m_bSloMo && this.IsSuperShot() && this.GetDistanceTo(this.m_vSuperShotDest) < this.m_fSuperShotSlowDownDistance) {
         this.TimerSetSpeedUp(this.m_fSuperShotSpeedup);
      }

      if (!this.HasOwner()) {
         if (!this.IsSuperShot()) {
            Vector vPos = this.GetPositionVectorVolatile();
            float fSpeed = this.GetSpeedValue();
            float fGroundHeight = -1.0F;
            float fEpsilon = 10.0F;
            if (fSpeed > 0.0F) {
               fGroundHeight = this.TerrainHeight(vPos.fX, vPos.fZ);
               if (Math.abs(fGroundHeight + this.fBallRadius - vPos.fY) < fEpsilon && this.m_bReachedDestination) {
                  float fNewSpeed = fSpeed - fSimInterval * this.fFrictionDeceleration;
                  if (fNewSpeed < 0.0F) {
                     fNewSpeed = 0.0F;
                  }

                  this.SetSpeedValue(fNewSpeed);
               }
            }
         }
      }
   }

   public boolean IsKicked(FootBaller cFB) {
      return this.m_cPrevOwner == null || !this.m_cPrevOwner.bBallKicked ? false : this.m_cPrevOwner == cFB || cFB == null;
   }

   void CheckGoal() {
      if (AIMatch.IsMatchState(0) && !SoccerModule.m_cMatch.IsWaiting()) {
         Vector vPos = this.GetPositionLogVolatile();
         GameField cField = this.GetGameField();
         if (!this.m_bWasOut && !this.m_bWasCorner && !this.m_bWasGoal) {
            if (!this.IsSpecialBall()) {
               if (vPos.fX > GameField.GetLineOutTopPos()) {
                  if (this.m_cCurOwner != null) {
                     this.m_cCurOwner.DetachBall();
                  }

                  SoccerModule.m_cMatch.OnOut(this.m_cPrevOwner, 0, this.GetPositionVectorVolatile().fZ);
                  this.m_bWasOut = true;
               } else if (vPos.fX < GameField.GetLineOutBottomPos()) {
                  if (this.m_cCurOwner != null) {
                     this.m_cCurOwner.DetachBall();
                  }

                  SoccerModule.m_cMatch.OnOut(this.m_cPrevOwner, 1, this.GetPositionVectorVolatile().fZ);
                  this.m_bWasOut = true;
               } else if (vPos.fZ < 0.0F) {
                  Collision col = this.TraceObjectsWithFlagsExclude(this.GetPositionVectorVolatile(), GameField.cGateLeft.GetPositionVectorVolatile(), this, 4);
                  if (col == null) {
                     this.m_bWasGoal = true;
                     boolean bIsSuperShot = this.IsSuperShot();
                     this.ResetSuperShot();
                     this.m_nTeamGoal = 1;
                     SoccerModule.m_cMatch.OnGoal(1, bIsSuperShot);
                  } else {
                     this.m_bWasCorner = true;
                     if (this.m_cCurOwner != null) {
                        this.m_cCurOwner.DetachBall();
                     }

                     SoccerModule.m_cMatch.OnCorner(this.m_cPrevOwner, 2);
                  }
               } else {
                  if (vPos.fZ > GameField.GetEndLinePos()) {
                     Collision col = this.TraceObjectsWithFlagsExclude(
                        this.GetPositionVectorVolatile(), GameField.cGateRight.GetPositionVectorVolatile(), this, 4
                     );
                     if (col == null) {
                        this.m_bWasGoal = true;
                        boolean bIsSuperShot = this.IsSuperShot();
                        this.ResetSuperShot();
                        this.DisableCallMethod("ResetSuperShot");
                        this.m_nTeamGoal = 0;
                        SoccerModule.m_cMatch.OnGoal(0, bIsSuperShot);
                        return;
                     }

                     this.m_bWasCorner = true;
                     if (this.m_cCurOwner != null) {
                        this.m_cCurOwner.DetachBall();
                     }

                     SoccerModule.m_cMatch.OnCorner(this.m_cPrevOwner, 3);
                  }
               }
            } else {
               if (!(vPos.fX > GameField.GetLineOutTopPos())
                     && !(vPos.fX < GameField.GetLineOutBottomPos())
                     && !(vPos.fZ < 0.0F)
                     && !(vPos.fZ > GameField.GetEndLinePos())
                  || this.IsDestReached()) {
                  this.ResetSpecialBall();
               }
            }
         }
      }
   }

   public void SetPosition(Vector vPos) {
      float fTerrainH = 0.0F;
      float fBallOnTerrainH = 0.0F;
      if (this.m_bAttached && this.HasOwner()) {
         Vector vPosG = this.m_cCurOwner.PointLocalToWorld(vPos);
         fTerrainH = this.TerrainHeight(vPosG.fX, vPosG.fZ);
         fBallOnTerrainH = fTerrainH + this.fBallRadius;
         if (vPosG.fY < fBallOnTerrainH) {
            vPosG.fY = fBallOnTerrainH + 1.0F;
            vPos = this.m_cCurOwner.PointWorldToLocal(vPosG);
         }
      } else {
         fTerrainH = this.TerrainHeight(vPos.fX, vPos.fZ);
         fBallOnTerrainH = fTerrainH + this.fBallRadius + 1.0F;
         if (vPos.fY < fBallOnTerrainH) {
            vPos.fY = fBallOnTerrainH;
         }
      }

      super.SetPosition(vPos);
      if (this.m_cRecorder != null && !this.IsReplaying() && !this.HasOwner()) {
      }
   }

   AIFootBaller.Shot KickToLob(Vector vNewPos, boolean bShoot) {
      if (vNewPos == null) {
         GameObject.CrashLog("WARNING: KickTo(null) ");
         return null;
      }

      ModStats.OnKick(this.m_cCurOwner != null ? this.m_cCurOwner : this.m_cPrevOwner); // [mod] dernier tireur (buteur)
      this.PlayKickSound(bShoot);
      boolean bHeadLob = false;
      boolean bHeadShot = false;
      if (this.m_cCurOwner != null && this.m_cCurOwner.m_bHeadShot) {
         bHeadShot = true;
      }

      if (this.m_cCurOwner != null && this.m_cCurOwner.IsUserControled() && this.m_cCurOwner.HasToHeadLob()) {
         this.m_cCurOwner.m_bHeadShot = false;
         bHeadLob = true;
      }

      if (this.m_cCurOwner != null && this.m_cCurOwner.m_bHeadShot) {
         this.m_bLastWasHead = true;
      } else {
         this.m_bLastWasHead = false;
      }

      this.SetDestUnreached(vNewPos);
      if (this.m_cCurOwner != null) {
         this.m_cCurOwner.m_bLastBallTouched = true;
      }

      boolean bLight;
      if (!bShoot && (this.m_cCurOwner == null || this.m_cCurOwner.m_cTeam.m_bUserControled)) {
         bLight = true;
      } else {
         bLight = false;
      }

      if (!bShoot) {
         this.m_bLastLobbed = true;
         GameField.SetDestPos(vNewPos, bLight);
         SoccerModule.m_cMatch.UpdateOrder();
      }

      this.SetDestinationPos(vNewPos);
      Vector vPos = this.GetPosition();
      Vector vDir = new Vector(vNewPos);
      vDir.Sub(vPos);
      float fDistance = vDir.Len();
      vDir.Normalize();
      if (this.m_cCurOwner == null) {
         return null;
      }

      AIFootBaller.Shot cShot = null;
      if (this.m_cCurOwner != null && this.m_cCurOwner.m_cShot != null) {
         cShot = this.m_cCurOwner.m_cShot;
      }

      Vector vUp = this.m_cCurOwner.GetUpVector();
      // [mod] on retient la puissance du tireur (Force + bonus IA) avant que DetachBall ne vide m_cCurOwner
      float fShooterPowerMul = ModDifficulty.GetShotPowerMul(this.m_cCurOwner);
      this.m_cCurOwner.SetBallKicked();
      this.m_cCurOwner.SetDir(new Vector(vDir.fX, 0.0F, vDir.fZ));
      this.m_cCurOwner.DetachBall();
      float fPowerCoeff = 2.0F;
      float fPower = 0.0F;
      float fBallSpeed = 0.0F;
      float fPowerUp = 0.0F;
      if (bShoot) {
         if (cShot != null) {
            this.SetShooted();
            float fSh = cShot.m_vDest.fY - this.TerrainHeight(cShot.m_vDest.fX, cShot.m_vDest.fZ);
            float fMulCoef;
            if (fDistance < 1000.0F) {
               if (fSh > 70.0F) {
                  fMulCoef = 1.15F;
               } else {
                  fMulCoef = 1.5F;
               }
            } else if (fDistance < 2000.0F) {
               if (fSh > 100.0F) {
                  fMulCoef = 1.5F;
               } else {
                  fMulCoef = 2.0F;
               }
            } else {
               fMulCoef = 2.0F;
            }

            if (SoccerGame.GetSoccerModule().m_nLevelID == 18 || bHeadShot) {
               fMulCoef++;
            }

            fSh *= fMulCoef;
            fPowerUp = (float)Math.sqrt(fSh * 20.0F * this.GetMass());
            float fMaxTime = fPowerUp / (10.0F * this.GetMass());
            float fMaxToGateDist = fSh - fSh / fMulCoef;
            float fMaxToGateTime = (float)Math.sqrt(2.0F * fMaxToGateDist / (10.0F * this.GetMass()));
            float fShotTime = fMaxTime + fMaxToGateTime;
            fPower = fDistance / fShotTime;
            // [mod] Force : vol plus court (tir plus rapide et plus tendu) arrivant au même point.
            // y(T) = vUp*T - g*T²/2 = hauteur visée  =>  vUp = h/T + g*T/2 (avec mult = 1 on retrouve l'original)
            float fShotMul = fShooterPowerMul;
            if (fShotMul != 1.0F) {
               float fGravity = 10.0F * this.GetMass();
               float fTargetHeight = fSh / fMulCoef;
               fShotTime /= fShotMul;
               fPower = fDistance / fShotTime;
               fPowerUp = fTargetHeight / fShotTime + fGravity * fShotTime / 2.0F;
            }
         } else {
            GameObject.CrashLog("ERROR:  KickToLob: cShot == null");
         }
      } else {
         if (SoccerGame.GetSoccerModule().m_nLevelID == 18) {
            fBallSpeed = Strategy.CalcSpeed(GameField.m_fDistToDest) / 1.5F;
         } else {
            fBallSpeed = Strategy.CalcSpeed(GameField.m_fDistToDest);
         }

         float fHeadChance = 0.0F;
         if (bHeadLob || AIMatch.IsMatchState(4)) {
            if (this.m_cCurOwner != null) {
               fHeadChance = this.m_cCurOwner.fHeight + 70.0F;
            } else {
               fHeadChance = 130.0F;
            }

            if (fBallSpeed == Strategy.m_fLongPassSpeed) {
               fHeadChance -= 50.0F;
            }

            if (fBallSpeed == Strategy.m_fShortPassSpeed) {
               fHeadChance += 50.0F;
            }

            if (SoccerGame.GetSoccerModule().m_nLevelID == 18) {
               fHeadChance /= 2.0F;
            }
         }

         fPower = fBallSpeed;
         float fTime = fDistance / fBallSpeed;
         fPowerUp = 10.0F * this.GetMass() * fTime / 2.0F + fHeadChance;
      }

      vDir.Mul(fPower);
      vUp.Mul(fPowerUp);
      this.m_nBounces = 0;
      if (fPower > this.fMaxKickSpeed / fPowerCoeff / 2.0F) {
         this.m_nMaxBounces = 1;
      } else {
         this.m_nMaxBounces = 0;
      }

      AIFootBaller.Shot cDefence = null;
      if (bShoot && cShot != null) {
         cDefence = new AIFootBaller.Shot();
         cShot.CopyTo(cDefence);
         cDefence.m_vDest = new Vector(cShot.m_vDest);
         cDefence.fSpeed = fPower;
      }

      this.SetFallSpeed(vUp);
      this.SetDir(vDir);
      this.SetSpeed(vDir);
      return cDefence;
   }

   FootBaller GetFootBaller(int nTeam, int nFootBaller) {
      SoccerModule var10000 = (SoccerModule)this.GetModule();
      Team team = SoccerModule.m_cMatch.GetTeam(nTeam);
      return team.GetFootBaller(nFootBaller);
   }

   void WrtiteStateAsLog() {
      GameObject.LogD(" BALL -------------------------------\n");
      GameObject.LogD(" - m_nSuperShotSound " + this.m_nSuperShotSound + "\n");
      GameObject.LogD(" - m_bSuperShot " + this.m_bSuperShot + "\n");
      GameObject.LogD(" - GetPhysicsActive() " + this.GetPhysicsActive() + "\n");
      GameObject.LogD(" - m_fSuperShotSpeedup " + this.m_fSuperShotSpeedup + "\n");
      GameObject.LogD(" - m_cSuperShotFB " + this.m_cSuperShotFB + "\n");
      GameObject.LogD(" - m_bShooted " + this.m_bShooted + "\n");
      GameObject.LogD(" - m_bLastLobbed " + this.m_bLastLobbed + "\n");
      GameObject.LogD(" - m_cPrevOwner " + this.m_cPrevOwner + "\n");
      GameObject.LogD(" - m_cCurOwner " + this.m_cCurOwner + "\n");
      GameObject.LogD(" - m_fAttachTime " + this.m_fAttachTime + "\n");
      GameObject.LogD(" - m_fLossTime " + this.m_fLossTime + "\n");
      GameObject.LogD(" - m_bReachedDestination " + this.m_bReachedDestination + "\n");
      GameObject.LogD(" - m_vStartPos " + this.m_vStartPos + "\n");
      GameObject.LogD(" - m_bHasOwner " + this.m_bHasOwner + "\n");
      GameObject.LogD(" - m_bWasCorner " + this.m_bWasCorner + "\n");
      GameObject.LogD(" - m_bWasOut " + this.m_bWasOut + "\n");
      GameObject.LogD(" - m_bWasGoal " + this.m_bWasGoal + "\n");
      GameObject.LogD(" - m_nTeamGoal " + this.m_nTeamGoal + "\n");
      GameObject.LogD(" - m_vPosAttached " + this.m_vPosAttached + "\n");
      GameObject.LogD(" - m_bSloMo " + this.m_bSloMo + "\n");
      GameObject.LogD(" - m_SpecialBallState " + this.m_SpecialBallState + "\n");
      GameObject.LogD(" - GameField.m_vDestPosition " + GameField.m_vDestPosition + "\n");
   }

   public void SetSpeed(Vector v) {
      v.fY = 0.0F;
      this.SetPhysicsActive(true);
      if (v.Len() == 0.0F) {
         this.SetFallSpeed(Vector.Zero);
      }

      super.SetSpeed(v);
      if (this.m_cRecorder != null && !this.IsReplaying() && !this.HasOwner()) {
      }
   }

   void SetShooted() {
      this.m_bShooted = true;
   }

   AIFootBaller.Shot KickTo(Vector vNewPos, boolean bShoot, boolean bDefence) {
      if (vNewPos == null) {
         GameObject.CrashLog("WARNING: KickTo(null) ");
         return null;
      }

      ModStats.OnKick(this.m_cCurOwner != null ? this.m_cCurOwner : this.m_cPrevOwner); // [mod] dernier tireur (buteur)
      this.PlayKickSound(bShoot);
      this.SetDestUnreached(vNewPos);
      if (this.m_cCurOwner != null) {
         this.m_cCurOwner.m_bLastBallTouched = true;
      }

      this.m_bLastWasHead = false;
      boolean bLight;
      if (!bShoot && (this.m_cCurOwner == null || this.m_cCurOwner.m_cTeam.m_bUserControled) && !bDefence) {
         bLight = true;
      } else {
         bLight = false;
      }

      if (!bShoot) {
         this.m_bLastLobbed = false;
         GameField.SetDestPos(vNewPos, bLight);
         SoccerModule.m_cMatch.UpdateOrder();
      }

      this.SetDestinationPos(vNewPos);
      Vector vPos = this.GetPosition();
      Vector vDir = new Vector(vNewPos);
      vDir.Sub(vPos);
      float fDistance = vDir.Len();
      vDir.Normalize();
      AIFootBaller.Shot cShot = null;
      if (this.m_cCurOwner != null && this.m_cCurOwner.m_cShot != null) {
         cShot = this.m_cCurOwner.m_cShot;
      }

      // [mod] on retient la puissance du tireur (Force + bonus IA) avant que DetachBall ne vide m_cCurOwner
      float fShooterPowerMul = ModDifficulty.GetShotPowerMul(this.m_cCurOwner);
      if (this.m_cCurOwner != null) {
         this.m_cCurOwner.SetBallKicked();
         this.m_cCurOwner.SetDir(new Vector(vDir.fX, 0.0F, vDir.fZ));
         this.m_cCurOwner.DetachBall();
      }

      float fPower = Strategy.CalcSpeed(GameField.m_fDistToDest);
      if (this.IsSuperShot()) {
         fPower = this.fSuperShotPowerMul * this.fMaxKickSpeed;
      }

      AIFootBaller.Shot cDefence = null;
      if (bShoot) {
         this.SetShooted();
         fPower = this.fMaxKickSpeed * fShooterPowerMul;
         if (cShot != null) {
            cDefence = new AIFootBaller.Shot();
            cShot.CopyTo(cDefence);
            cDefence.m_vDest = new Vector(cShot.m_vDest.fX, this.TerrainHeight(cShot.m_vDest.fX, cShot.m_vDest.fZ) + 10.0F, cShot.m_vDest.fZ);
            cDefence.fSpeed = fPower;
         }
      }

      vDir.Mul(fPower);
      this.SetDir(vDir);
      this.SetSpeed(vDir);
      this.m_nBounces = 0;
      return cDefence;
   }

   boolean IsShooted() {
      return this.m_bShooted;
   }

   boolean IsMoving() {
      return this.bMoving;
   }

   void ReplayStop() {
      if (this.m_cRecorder != null) {
         this.m_cRecorder.ReplayStop();
      }
   }

   public void SetSpeedValue(float fSpeed) {
      super.SetSpeedValue(fSpeed);
      if (this.m_cRecorder != null && !this.IsReplaying() && !this.HasOwner()) {
      }
   }

   void OnPhysicsUpdate(float fAbsoluteTime, float fSimInterval) {
      if (PROFILE_BallOnPhysics == 0) {
         PROFILE_BallOnPhysics = this.GetModule().ProfileAdd("Soccer/Ball", "OnPhysicsUpdate");
      }

      this.GetModule().ProfileBegin(PROFILE_BallOnPhysics);
      if (this.KickedExpired()) {
         this.m_cPrevOwner.ResetBallKicked();
      }

      if (!this.IsReplaying()) {
         this.CheckGoal();
         this.CheckTerrain();
         this.CheckOutSide();
         this.UpdateSpeed(fSimInterval);
      }

      if (this.m_cCurOwner == null || !this.m_cCurOwner.IsReferee()) {
         this.UpdateRotation(fSimInterval);
         if (this.m_cCurOwner != null) {
            this.UpdateFeetDance();
         }
      }

      SoccerModule var10000 = (SoccerModule)this.GetModule();
      SoccerModule.GetHud().UpdateBallPosition(this);
      this.GetModule().ProfileEnd(PROFILE_BallOnPhysics);
   }

   FootBaller GetCurOwner() {
      return this.m_cCurOwner;
   }

   boolean HasOwner() {
      return this.m_bHasOwner;
   }

   void ClearDestReached() {
      GameField.m_cDestMark.Hide();
      this.m_bReachedDestination = true;
   }

   void ResetSpecialBall() {
      this.m_SpecialBallState = false;
   }

   void ResetEventFlags() {
      if (this.m_bWasGoal) {
         this.TimerSetSpeedUp(1.0F);
      }

      this.m_bWasGoal = false;
      this.m_bWasOut = false;
      this.m_bWasCorner = false;
   }

   void SuperShotSmokeFX() {
      Vector vPos = this.GetPositionVectorVolatile();
      vPos.fZ = vPos.fZ - this.fBallRadius;
      if (this.m_bSloMo) {
         Vector vNormal = this.GetSpeedVectorVolatile();
         if (vNormal.Len() == 0.0F) {
            vNormal = Vector.Forward;
         }

         this.ExecuteFX("FXSmoke", vPos, vNormal);
         if (this.IsSuperShot()) {
            this.CallMethodIn("SuperShotSmokeFX", this.nFXTimeNormal / 100.0F * (this.m_fSuperShotSpeedup * 10.0F));
         }
      } else {
         Vector vNormal = this.GetSpeedVectorVolatile();
         if (vNormal.Len() == 0.0F) {
            vNormal = Vector.Forward;
         }

         this.ExecuteFX("FXSmoke", vPos, vNormal);
         if (this.IsSuperShot()) {
            this.CallMethodIn("SuperShotSmokeFX", this.nFXTimeNormal / 100.0F);
         }
      }
   }

   void PlayBounceSound(int nType) {
      switch (nType) {
         case 0:
            this.PlaySound("Bounce04.wav");
            break;
         case 1:
            Tools.PlayRandomSound(this, m_sSoundsBounceOnGrass);
            break;
         case 2:
            this.PlaySound("bounce01.wav");
            break;
         case 3:
            this.PlaySound("net.wav");
            break;
         case 4:
            this.PlaySound("pien.wav");
            break;
         case 5:
            this.PlaySound("krzak.wav");
            break;
         case 6:
            this.PlaySound("catch03.wav");
      }
   }

   void ResetShooted() {
      this.m_bShooted = false;
   }

   public void OnCollisionFullInfo(ControlObject obj, Vector vPoint, Vector vNormal) {
      this.ClearDestReached();
      if (obj != null) {
         ModSupers.OnBallHit(); // [mod] le ballon fou cesse de zigzaguer au premier contact
         if ((!(obj instanceof FootBaller) || obj != this.m_cPrevOwner) && this.m_cPrevOwner != null) {
            this.m_cPrevOwner.m_bLastBallTouched = false;
         }

         if (!(obj instanceof FootBaller)) {
            if (this.IsSuperShot() && obj instanceof SuperWall) {
               this.ResetSuperShot();
            }

            this.OnHitObject(obj, vPoint, vNormal);
         } else {
            this.OnHitFootballer((FootBaller)obj, vNormal);
         }
      } else {
         GameObject.LogD("null\n");
      }
   }

   void Record() {
      if (this.m_cRecorder != null) {
         this.m_cRecorder.Record();
      }
   }

   public void BallDetachFromParentAndKeepWorldPos() {
      super.DetachFromParentAndKeepWorldPos();
      this.m_bAttached = false;
   }

   public void OnDestinationPos() {
      this.ClearDestReached();
   }

   boolean IsReplaying() {
      return this.m_cRecorder != null ? this.m_cRecorder.m_bPlaying : false;
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

   void UpdateFXs() {
      if (this.IsSuperShot()) {
         this.SuperShotSmokeFX();
      }
   }

   void OnCreate() {
      if (CoreLogic.GetPlayer().m_bReplaysOn) {
         this.m_cRecorder = new Recorder();
         this.m_cRecorder.SetRecorded(this);
      }

      super.OnCreate();
      this.ClearFlag(4096);
      this.SetFlag(1);
      this.SetFlag(256);
      this.SetFlag(32);
      this.RegisterInInteractionMap();
      this.SetCollAction(32);
      this.SetCollType(0);
      this.SetTraceCollType(6);
      this.SetCollHandler(768);
      this.SetPhysicsType(6);
      this.SetPhysicsActive(true);
      this.RegisterKeyRequest();
      Vector vMin = new Vector();
      Vector vMax = new Vector();
      this.GetMeshExtentsInWorld(vMin, vMax);
      vMax.Sub(vMin);
      this.SetBlobMapSize(vMax.fX * 0.6F);
      this.fBallRadius = vMax.fY / 2.0F;
      this.m_vPosAttached = new Vector(0.0F, this.fBallRadius, 60.0F);
      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("fBallRadius " + this.fBallRadius);
      }

      this.RegisterKeyRequest();
      this.SetMaxPhysicsUpdateInterval(0.0F);
      this.SetUnImportanceThreshold(50.0F);
      this.SetMass(200.0F);
      SoccerGame.IncBall();
   }

   void OnInputKey(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      if (SoccerGame.m_bDebug) {
         if (bKeyState) {
            switch (nKeyCode) {
               case 28:
                  if (!this.bMoveByKeys) {
                     return;
                  }
                  break;
               case 200:
                  if (!this.bMoveByKeys) {
                     return;
                  }

                  this.SetSpeed(new Vector(this.fS, 0.0F, 0.0F));
                  break;
               case 203:
                  if (!this.bMoveByKeys) {
                     return;
                  }

                  this.SetSpeed(new Vector(0.0F, 0.0F, -this.fS));
                  break;
               case 205:
                  if (!this.bMoveByKeys) {
                     return;
                  }

                  this.SetSpeed(new Vector(0.0F, 0.0F, this.fS));
                  break;
               case 208:
                  if (!this.bMoveByKeys) {
                     return;
                  }

                  this.SetSpeed(new Vector(-this.fS, 0.0F, 0.0F));
                  break;
               case 210:
                  this.bMoveByKeys = !this.bMoveByKeys;
               case 57:
                  if (!this.bMoveByKeys) {
                     return;
                  }

                  Vector vPos = GameField.cGateRight.GetPosition();
                  vPos.fZ -= 500.0F;
                  this.SetPosition(vPos);
                  this.SetSpeed(Vector.Zero);
                  this.SetFallSpeed(Vector.Zero);
                  break;
               case 211:
                  if (!this.bMoveByKeys) {
                     return;
                  }

                  SoccerGame.game.m_cCheat.sinkfbs();
                  SoccerModule.m_cMatch.ActivateAIs(false);
                  break;
               default:
                  if (!this.bMoveByKeys) {
                     return;
                  }

                  if (!this.HasOwner()) {
                  }
            }
         }
      }
   }

   void Replay() {
      if (this.m_cRecorder != null) {
         this.m_cRecorder.Replay();
      }
   }

   void DoSuperShot(AIFootBaller cFBaller) {
      this.DisableCallMethod("ResetSuperShot");
      this.m_cSuperShotFB = cFBaller;
      this.SetSuperShot();
      this.CallMethodIn("ResetSuperShot", Strategy.m_fSuperShotTime);
      SoccerModule.GetHud().FlashSuperIcon(cFBaller.m_nTeam);
      Vector vPos = null;
      if (cFBaller.m_nTeam == 0) {
         vPos = GameField.cGateRight.GetPosition();
      } else {
         vPos = GameField.cGateLeft.GetPosition();
      }

      this.m_vSuperShotDest.Set(vPos);
      this.KickTo(vPos, true, false);
   }
}
