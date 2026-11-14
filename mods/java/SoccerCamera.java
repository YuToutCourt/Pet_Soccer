public class SoccerCamera extends BaseCamera {
   static final int _CAM_MODE_HORIZONTAL = 0;
   static final int _CAM_MODE_HORIZONTAL_SAT = 1;
   static final int _CAM_MODE_VERTICAL = 2;
   static final int _CAM_MODE_VERTICAL_SAT = 3;
   int m_nMode = 0;
   Vector m_vCamModeVerticalLeft = new Vector(0.0F, 358.0F, -826.52F);
   Vector m_vCamModeVerticalRight = new Vector(0.0F, 358.0F, 826.52F);
   Vector m_vCamModeHorizontal = new Vector(-744.8F, 366.0F, 0.0F);
   Vector m_vCamModeVerticalSatRight = new Vector(0.0F, 1250.0F, 75.0F);
   Vector m_vCamModeVerticalSatLeft = new Vector(0.0F, 1250.0F, -75.0F);
   Vector m_vCamModeHorizontalSat = new Vector(-75.0F, 1250.0F, 0.0F);
   Vector m_vCameraDistance = this.m_vCamModeHorizontal;
   float m_fCamZoomDelta = 250.0F;
   float m_fCamMaxDistance = 3000.0F;
   float m_fCamMinDistance = 250.0F;
   float _CAM_DIR_DELAY = 0.25F;
   float _CAM_DIR_NO_UPDATE_TIME = 0.25F;
   boolean m_bDontUpdateDir = false;
   float m_fCamDirDelay = this._CAM_DIR_DELAY;
   boolean m_bEndHalf = false;
   float _CAM_MOVE_DELAY = 0.25F;
   float _CAM_MOVE_DELAY_ENDHALF = 4.0F;
   float _CAM_DIR_DELAY_REPLAY = 0.15F;
   ControlObject m_Observed = null;
   boolean m_bFirstUpdate = true;
   boolean m_bFreezed = false;
   CameraDummy m_cReplayCamera = null;
   boolean m_bReplay = false;
   float m_fReplayFOV = 110.0F;
   ControlObject coSpeaker = null;
   int nSound = -1;
   int nDisasterSound = -1;
   float m_fToggleCameraTime = 30.0F;
   boolean bAutoUpdate = true;
   float m_fTimePrev = 0.0F;
   float fShakeCamDist = 200.0F;
   Vector vDestCamPos = new Vector();
   Vector vDestCamDir = new Vector();
   Vector vBase = new Vector();
   boolean bMoveCamera = false;
   boolean bChangeDelays = false;
   float m_fMaxAngle = 50.0F;
   boolean m_bShaker = false;
   Vector m_vShakerOsc = null;
   float m_MBTime = 0.1F;
   float m_ShakeTimeTo = this.m_MBTime;
   float fEndX = -300.0F;
   float fEndY = 250.0F;
   float fEndZ = -300.0F;
   float fEndHalfCamSwitchTime = 2.5F;

   public void OnDestroy() {
      this.vDestCamPos = null;
      this.vDestCamDir = null;
      this.vBase = null;
      this.m_cReplayCamera = null;
      this.m_Observed = null;
      super.OnDestroy();
   }

   void SetCameraZoom(int nMode, float fZoom) {
      PlayerSave player = CoreLogic.GetPlayer();
      player.SetCameraZoom(nMode, fZoom);
   }

   void EnableDirUpdate() {
      this.m_bDontUpdateDir = false;
      this.m_fCamDirDelay = this._CAM_DIR_DELAY * 10.0F;
   }

   void ContinueSetEndHalCameraInFront() {
      float fRealZ = this.fEndZ;
      float fRealX = this.fEndX;
      float fRealY = this.fEndY;
      fRealX -= 600.0F;
      fRealY += 150.0F;
      this.m_vCameraDistance = new Vector(fRealX, fRealY, fRealZ);
      this.SetObserved(SoccerModule.m_cMatch.GetTeam(0).GetGoalKeeper());
      this.ForceUpdate();
      this.CallMethodIn("ContinueSetEndHalfCamera", 1.0F);
   }

   void ChangeMode(int nMode) {
      int nTeam = SoccerModule.m_cMatch.m_nPlayer1Team;
      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("Changing camera mode to " + nMode + " for " + nTeam + "\n");
      }

      if (CoreLogic.m_bMultiplayer) {
         if (nMode == 2) {
            nMode = 0;
         }

         if (nMode == 3) {
            nMode = 1;
         }
      }

      this.m_nMode = nMode;
      switch (nMode) {
         case 0:
            this.m_vCameraDistance = this.m_vCamModeHorizontal;
            break;
         case 1:
            this.m_vCameraDistance = this.m_vCamModeHorizontalSat;
            break;
         case 2:
            if (nTeam == 0) {
               this.m_vCameraDistance = this.m_vCamModeVerticalLeft;
            } else {
               this.m_vCameraDistance = this.m_vCamModeVerticalRight;
            }
            break;
         case 3:
            if (nTeam == 0) {
               this.m_vCameraDistance = this.m_vCamModeVerticalSatLeft;
            } else {
               this.m_vCameraDistance = this.m_vCamModeVerticalSatRight;
            }
      }

      this.m_vCameraDistance.Normalize();
      this.m_vCameraDistance.Mul(this.GetCameraZoom(nMode));
      SoccerModule.GetHud().OnChangeCameraMode(this.m_nMode);
      GameField.SetControlDirsOrientation(this.m_nMode, nTeam);
      this.SetCameraZoom(this.m_nMode, this.GetCameraZoom(this.m_nMode));
   }

   float GetCameraZoom(int nMode) {
      PlayerSave player = CoreLogic.GetPlayer();
      return player.GetCameraZoom(nMode);
   }

   void OnPostUpdate() {
      // [mod] en ligne : chez le client, la caméra copie celle de l'hôte
      if (ModNet.ClientCamera(this)) {
         this.UpdateCoSpeaker();
         return;
      }

      if (this.m_Observed != null && this.bAutoUpdate && !this.m_bFreezed) {
         Vector vBallPosition = null;
         if (this.m_Observed instanceof Ball) {
            if (((Ball)this.m_Observed).HasOwner()) {
               vBallPosition = ((Ball)this.m_Observed).GetCurOwner().GetPositionVectorVolatile();
            } else {
               vBallPosition = this.m_Observed.GetPositionVectorVolatile();
            }
         } else {
            vBallPosition = this.m_Observed.GetPositionVectorVolatile();
         }

         this.vDestCamPos.Set(vBallPosition);
         if (this.m_bReplay && this.m_cReplayCamera.m_bDynamic) {
            this.vDestCamPos.Add(this.m_cReplayCamera.m_vDynamicPos);
         } else {
            this.vDestCamPos.Add(this.m_vCameraDistance);
         }

         if (!this.m_bReplay && !this.m_bEndHalf) {
            this.AdjustDestCamPos(this.vDestCamPos);
         }

         boolean bShakeOnce = false;
         if (this.m_bReplay && !this.m_cReplayCamera.m_bDynamic) {
            this.vDestCamPos = this.m_cReplayCamera.GetPosition();
            if (this.m_cReplayCamera instanceof CameraDummyLeft) {
               this.vDestCamPos.fZ = this.vDestCamPos.fZ + GameField.WorldToField(vBallPosition).fZ * 0.6666667F;
            }

            if (this.m_cReplayCamera instanceof CameraDummyRight) {
               this.vDestCamPos.fZ = this.vDestCamPos.fZ - (GameField.GetEndLinePos() - GameField.WorldToField(vBallPosition).fZ) * 0.6666667F;
            }

            if (this.vDestCamPos.Distance(vBallPosition) <= this.fShakeCamDist) {
               bShakeOnce = true;
               this.m_vShakerOsc = new Vector(100.0F, 100.0F, 100.0F);
            }
         }

         this.vDestCamDir.Set(vBallPosition);
         this.vDestCamDir.Sub(this.vDestCamPos);
         this.vDestCamDir.Normalize();
         Vector vCurrCamDir = this.GetForwardVector();
         vCurrCamDir.Negate();
         Vector vCurrCamPos = this.GetPosition();
         boolean bShakePermission = false;
         float fTimeDelta = this.GetTime() - this.m_fTimePrev;
         this.m_fTimePrev = this.GetTime();
         this.m_ShakeTimeTo -= fTimeDelta;
         if (this.m_ShakeTimeTo <= 0.0F) {
            this.m_ShakeTimeTo = this.m_MBTime;
            bShakePermission = true;
         }

         if (bShakeOnce || this.m_bShaker && bShakePermission && this.m_vShakerOsc != null) {
            bShakeOnce = false;
            float disX = SoccerGame.GetRandom(-1.0F, 1.0F);
            float disY = SoccerGame.GetRandom(-1.0F, 1.0F);
            float disZ = SoccerGame.GetRandom(-1.0F, 1.0F);
            vCurrCamPos.fX = vCurrCamPos.fX + disX * this.m_vShakerOsc.fX;
            vCurrCamPos.fY = vCurrCamPos.fY + disY * this.m_vShakerOsc.fY;
            vCurrCamPos.fZ = vCurrCamPos.fZ + disZ * this.m_vShakerOsc.fZ;
         }

         if (this.m_bFirstUpdate && this.m_Observed != null) {
            vCurrCamDir.Set(this.vDestCamDir);
            vCurrCamPos.Set(this.vDestCamPos);
            this.SetCamPosDir(vCurrCamPos, vCurrCamDir);
            this.m_bFirstUpdate = false;
         } else {
            float fCoef = 1.0F;
            if (!this.m_bDontUpdateDir) {
               if (GameField.m_cBall.IsSuperShot()) {
                  this.vDestCamDir.Set(vBallPosition);
                  this.vDestCamDir.Sub(vCurrCamPos);
                  this.vDestCamDir.Normalize();
                  fCoef = this._CAM_DIR_DELAY / SoccerModule.m_Mod.m_fFrameTime;
               } else if (!this.m_bReplay && !this.m_bEndHalf) {
                  fCoef = this.m_fCamDirDelay / SoccerModule.m_Mod.m_fFrameTime;
               } else {
                  if (this.m_bEndHalf) {
                     this.vDestCamDir.Set(vBallPosition);
                     this.vDestCamDir.Sub(vCurrCamPos);
                     this.vDestCamDir.Normalize();
                  }

                  fCoef = this._CAM_DIR_DELAY_REPLAY / SoccerModule.m_Mod.m_fFrameTime;
               }

               vCurrCamDir.fX = vCurrCamDir.fX + (this.vDestCamDir.fX - vCurrCamDir.fX) / fCoef;
               vCurrCamDir.fY = vCurrCamDir.fY + (this.vDestCamDir.fY - vCurrCamDir.fY) / fCoef;
               vCurrCamDir.fZ = vCurrCamDir.fZ + (this.vDestCamDir.fZ - vCurrCamDir.fZ) / fCoef;
               if (this.m_fCamDirDelay > this._CAM_DIR_DELAY) {
                  this.m_fCamDirDelay = this.m_fCamDirDelay - this.m_fCamDirDelay / this._CAM_DIR_NO_UPDATE_TIME * SoccerModule.m_Mod.m_fFrameTime;
                  if (this.m_fCamDirDelay < this._CAM_DIR_DELAY) {
                     this.m_fCamDirDelay = this._CAM_DIR_DELAY;
                  }
               }
            }

            if (GameField.m_cBall.IsSuperShot()) {
               fCoef = this._CAM_MOVE_DELAY * 3.0F / SoccerModule.m_Mod.m_fFrameTime;
               this.vDestCamPos.fX = this.vDestCamPos.fX - this.m_vCameraDistance.fX;
            } else if (this.m_bEndHalf) {
               fCoef = this._CAM_MOVE_DELAY_ENDHALF / SoccerModule.m_Mod.m_fFrameTime;
            } else {
               fCoef = this._CAM_MOVE_DELAY / SoccerModule.m_Mod.m_fFrameTime;
            }

            vCurrCamPos.fX = vCurrCamPos.fX + (this.vDestCamPos.fX - vCurrCamPos.fX) / fCoef;
            vCurrCamPos.fY = vCurrCamPos.fY + (this.vDestCamPos.fY - vCurrCamPos.fY) / fCoef;
            vCurrCamPos.fZ = vCurrCamPos.fZ + (this.vDestCamPos.fZ - vCurrCamPos.fZ) / fCoef;
            this.SetCamPosDir(vCurrCamPos, vCurrCamDir);
            this.UpdateCoSpeaker();
         }
      } else {
         this.UpdateCoSpeaker();
      }
   }

   void AttachDisasterSound(String wavefile) {
      this.DetachDisasterSound();
      this.nDisasterSound = this.coSpeaker.AttachSound(wavefile);
   }

   void OnInputKeyDebug(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      if (nKeyCode != 49 && nKeyCode != 50) {
         if (bKeyState) {
            if (this.bMoveCamera) {
               Tools.MovePointByKeys(this.m_vCameraDistance, nKeyCode, 0.0F);
            }

            if (this.bChangeDelays) {
               Vector vDelays = new Vector(this._CAM_MOVE_DELAY, this._CAM_DIR_DELAY, 0.0F);
               Tools.MovePointByKeys(vDelays, nKeyCode, 0.01F);
               this._CAM_MOVE_DELAY = vDelays.fX;
               this._CAM_DIR_DELAY = vDelays.fY;
               Tools.ShowInfo("_CAM_MOVE_DELAY " + this._CAM_MOVE_DELAY + " _CAM_DIR_DELAY " + this._CAM_DIR_DELAY, this.GetModule(), Tools._INFO_POS_TOP);
            }

            switch (nKeyCode) {
               case 19:
                  this.bAutoUpdate = false;
                  break;
               case 63:
                  if (this.m_bEndHalf) {
                     this.ClearEndHalfCamera();
                  } else {
                     this.SetEndHalfCamera();
                  }
                  break;
               case 67:
                  this.DumpCameraPos();
                  break;
               case 68:
                  this.bChangeDelays = false;
                  this.bMoveCamera = !this.bMoveCamera;
                  if (!this.bMoveCamera) {
                     Tools.ShowInfo("", this.GetModule(), Tools._INFO_POS_TOP);
                  } else {
                     Tools.ShowInfo(
                        "Camera relative pos " + this.m_vCameraDistance.toString() + " fDelta " + Tools.g_fDelta, this.GetModule(), Tools._INFO_POS_TOP
                     );
                  }
                  break;
               case 87:
                  this.bMoveCamera = false;
                  this.bChangeDelays = !this.bChangeDelays;
                  if (!this.bChangeDelays) {
                     Tools.ShowInfo("", this.GetModule(), Tools._INFO_POS_TOP);
                  } else {
                     Tools.ShowInfo("_CAM_MOVE_DELAY " + this._CAM_MOVE_DELAY + " _CAM_DIR_DELAY " + this._CAM_DIR_DELAY, this.GetModule(), Tools._INFO_POS_TOP);
                  }
            }
         }
      } else {
         super.OnInputKey(nKeyCode, bKeyState, nKeyAscii);
      }
   }

   void ClearEndHalfCamera() {
      this.DisableCallMethod("ContinueSetEndHalfCamera");
      this.DisableCallMethod("ContinueSetEndHalCameraInFront");
      this.m_bEndHalf = false;
      this.ChangeMode(this.m_nMode);
   }

   void SetCamPosDir(Vector vPos, Vector vDir) {
      vDir.Negate();
      super.m_vCamBaseZ.Set(vDir);
      super.m_vCamBaseZ.Normalize();
      float fDot = Vector.Up.Dot(super.m_vCamBaseZ);
      if (fDot == 1.0F) {
         this.vBase.Set(super.m_vCamBaseZ.Cross(Vector.Forward));
      } else if (fDot == -1.0F) {
         this.vBase.Set(Vector.Forward.Cross(super.m_vCamBaseZ));
      } else {
         this.vBase.Set(Vector.Up.Cross(super.m_vCamBaseZ));
      }

      this.vBase.Normalize();
      super.m_vCamBaseY.Set(super.m_vCamBaseZ.Cross(this.vBase));
      this.FromForwardUpPos(super.m_vCamBaseZ, super.m_vCamBaseY, vPos);
   }

   void SetShaker(boolean shaker, Vector osc, float mbtime) {
      this.m_bShaker = shaker;
      this.m_vShakerOsc = osc;
      if (osc == null) {
         this.m_bShaker = false;
      }

      this.m_MBTime = mbtime;
      this.m_ShakeTimeTo = this.m_MBTime;
   }

   void SetSoundVolume(float volume) {
      if (this.nSound != -1) {
         if (volume < 0.0F) {
            volume = 0.0F;
         }

         if (volume > 1.0F) {
            volume = 1.0F;
         }

         this.coSpeaker.SetSoundVolume(this.nSound, volume);
      }
   }

   void ZoomOut() {
      float fCurCamDistance = this.GetCameraZoom(this.m_nMode);
      fCurCamDistance += this.m_fCamZoomDelta;
      if (fCurCamDistance <= this.m_fCamMaxDistance) {
         this.SetCameraZoom(this.m_nMode, fCurCamDistance);
         this.m_vCameraDistance.Normalize();
         this.m_vCameraDistance.Mul(fCurCamDistance);
      }
   }

   void AdjustDestCamPos(Vector vPos) {
      int nPlayerTeam = SoccerModule.m_cMatch.m_nPlayer1Team;
      Vector vLogPos = GameField.WorldToField(vPos);
      switch (this.m_nMode) {
         case 0:
            if (nPlayerTeam == 0) {
               if (vLogPos.fX > GameField.GetLineOutTopPos() / 2.0F) {
                  vPos.fX = GameField.vFieldLogicalStart.fX + GameField.GetLineOutTopPos() / 2.0F;
               }
            } else if (vLogPos.fX > GameField.GetLineOutTopPos() / 2.0F) {
               vPos.fX = GameField.vFieldLogicalStart.fX + GameField.GetLineOutTopPos() / 2.0F;
            }
         case 1:
         case 3:
         default:
            break;
         case 2:
            if (nPlayerTeam == 0) {
               if (vLogPos.fZ >= GameField.GetAttackLinePos(0)) {
                  vPos.fZ = GameField.vFieldLogicalStart.fZ + GameField.GetAttackLinePos(0);
               }
            } else if (vLogPos.fZ <= GameField.GetAttackLinePos(1)) {
               vPos.fZ = GameField.vFieldLogicalStart.fZ + GameField.GetAttackLinePos(1);
            }
      }
   }

   void DetachSound() {
      if (this.nSound != -1) {
         if (this.coSpeaker != null) {
            this.coSpeaker.DetachSound(this.nSound);
         }

         this.nSound = -1;
      }
   }

   void SetReplayCamera(CameraDummy cCamera) {
      this.m_bReplay = true;
      this.m_cReplayCamera = cCamera;
      this.SetPosition(cCamera.GetPosition());
      this.SetFOV(this.m_fReplayFOV);
      this.ForceUpdate();
   }

   void ToggleMode() {
      int nNewMode = (this.m_nMode + 1) % 4;
      this.ChangeMode(nNewMode);
      this.CallMethodIn("ToggleMode", this.m_fToggleCameraTime);
   }

   float AngleToObserved() {
      if (this.m_Observed == null) {
         return 0.0F;
      }

      Vector vObservedPos = null;
      if (this.m_Observed instanceof Ball) {
         if (((Ball)this.m_Observed).HasOwner()) {
            vObservedPos = ((Ball)this.m_Observed).GetCurOwner().GetPosition();
         } else {
            vObservedPos = this.m_Observed.GetPosition();
         }
      } else {
         vObservedPos = this.m_Observed.GetPosition();
      }

      Vector vPos = this.GetPosition();
      Vector vDir = vPos.Sub2(vObservedPos);
      vDir.Normalize();
      float fDot = vDir.Dot(this.GetForwardVector());
      if (fDot > 1.0F) {
         fDot = 1.0F;
      }

      if (fDot < -1.0F) {
         fDot = -1.0F;
      }

      return (float)Math.acos(fDot) * 180.0F / (float) Math.PI;
   }

   void PlayDisasterSound(String wavefile) {
      if (this.nDisasterSound == -1) {
         this.nDisasterSound = this.coSpeaker.AttachSound(wavefile);
      }
   }

   void DisableDirUpdate() {
      this.m_bDontUpdateDir = true;
      this.DisableCallMethod("EnableDirUpdate");
      this.CallMethodIn("EnableDirUpdate", this._CAM_DIR_NO_UPDATE_TIME);
   }

   void ForceUpdate() {
      this.m_bFirstUpdate = true;
      this.OnPostUpdate();
   }

   void OnInputMouseAxis(int nAxis, int nDelta) {
      if (!this.m_bFirstUpdate && !this.m_bFreezed && !this.m_bReplay) {
         if (!this.bAutoUpdate) {
            super.OnInputMouseAxis(nAxis, nDelta);
         } else {
            Vector vForward = this.GetForwardVector();
            Vector vUp = this.GetUpVector();
            Vector vPos = this.GetPosition();
            if (nAxis == 0 && this.m_nMode != 1 && this.m_nMode != 3) {
               this.DisableDirUpdate();
               super.OnInputMouseAxis(nAxis, nDelta);
            }

            if (nAxis == 1 && this.m_nMode != 1 && this.m_nMode != 3) {
               this.DisableDirUpdate();
               super.OnInputMouseAxis(nAxis, nDelta);
            }

            float fAngle = this.AngleToObserved();
            if (fAngle > this.m_fMaxAngle) {
               this.FromForwardUpPos(vForward, vUp, vPos);
            }

            if (nAxis == 2) {
               if (nDelta < 0) {
                  this.ZoomIn();
               } else {
                  this.ZoomOut();
               }
            }
         }
      }
   }

   void Freeze() {
      this.m_bFreezed = true;
   }

   void SetDisasterSoundVolume(float volume) {
      if (this.nDisasterSound != -1) {
         if (volume < 0.0F) {
            volume = 0.0F;
         }

         if (volume > 1.0F) {
            volume = 1.0F;
         }

         this.coSpeaker.SetSoundVolume(this.nDisasterSound, volume);
      }
   }

   void Flash(int frames, float fMBTime) {
   }

   void ClearReplayCamera() {
      this.m_bReplay = false;
      this.m_cReplayCamera = null;
      this.ChangeMode(this.m_nMode);
      this.SetFOV(super.m_fFOV);
      this.ForceUpdate();
   }

   void SetEndHalfCamera() {
      this.fEndX = -300.0F;
      this.fEndY = 250.0F;
      this.fEndZ = -300.0F;
      float fRealZ = this.fEndZ;
      float fRealX = this.fEndX;
      float fRealY = this.fEndY;
      if (!AIMatch.IsMatchState(1)) {
         fRealZ -= 80.0F;
         fRealX -= 150.0F;
         fRealY += 150.0F;
         this.SetObserved(SoccerModule.m_cMatch.GetTeam(0).GetGoalKeeper());
      } else {
         fRealX += 2100.0F;
         fRealY += 50.0F;
         fRealZ += 300.0F;
         this.SetObserved(SoccerModule.m_cMatch.m_cReferee);
      }

      this.m_vCameraDistance = new Vector(fRealX, fRealY, fRealZ);
      if (AIMatch.IsMatchState(1)) {
         this.fEndHalfCamSwitchTime = 2.5F;
      } else {
         this.fEndHalfCamSwitchTime = 0.0F;
      }

      if (!AIMatch.IsMatchState(1)) {
         this.CallMethodIn("ContinueSetEndHalfCamera", this.fEndHalfCamSwitchTime);
      } else {
         this.CallMethodIn("ContinueSetEndHalCameraInFront", this.fEndHalfCamSwitchTime);
      }

      this.ForceUpdate();
   }

   void ContinueSetEndHalfCamera() {
      this.m_bEndHalf = true;
      this.SetObserved(SoccerModule.m_cMatch.GetTeam(1).GetGoalKeeper());
      if (AIMatch.IsMatchState(1)) {
         float fRealZ = this.fEndZ;
         float fRealX = this.fEndX;
         float fRealY = this.fEndY;
         fRealX -= 750.0F;
         fRealY += 200.0F;
         this.m_vCameraDistance = new Vector(fRealX, fRealY, fRealZ);
      }
   }

   void DumpCameraPos() {
      GameObject.CrashLog("_CAM_MODE_HORIZONTAL " + this.m_vCamModeHorizontal.toString() + "\n");
      GameObject.CrashLog("_CAM_MODE_HORIZONTAL_SAT " + this.m_vCamModeHorizontalSat.toString() + "\n");
      GameObject.CrashLog("_CAM_MODE_VERTICAL " + this.m_vCamModeVerticalLeft.toString() + "\n");
      GameObject.CrashLog("_CAM_MODE_VERTICAL_SAT " + this.m_vCamModeVerticalSatLeft.toString() + "\n");
      GameObject.CrashLog("\n");
   }

   void UnFreeze() {
      this.m_bFreezed = false;
   }

   void UpdateCoSpeaker() {
      if (this.coSpeaker != null) {
         this.coSpeaker.SetPosition(this.GetPosition());
      }
   }

   void OnCreate() {
      this.m_vCamModeVerticalLeft.Normalize();
      this.m_vCamModeVerticalRight.Normalize();
      this.m_vCamModeHorizontal.Normalize();
      this.m_vCamModeVerticalSatRight.Normalize();
      this.m_vCamModeVerticalSatLeft.Normalize();
      this.m_vCamModeHorizontalSat.Normalize();
      this.SetClipFar(PerformanceSettings._CAM_CLIP[PerformanceSettings.m_iCurrQuality]);
      if (this.coSpeaker == null) {
         this.coSpeaker = (ControlObject)SoccerGame.GetSoccerModule().CreateObject("ControlObject");
         this.coSpeaker.ActivateOnModule();
      }
   }

   public void SetObserved(ControlObject Obj) {
      // [mod] en ligne : l'hôte diffuse la cible de la caméra, le client ne suit que celle de l'hôte
      if (!ModNet.OnCamObserved(Obj)) {
         return;
      }

      this.m_Observed = Obj;
   }

   void OnInputKey(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      if (!this.m_bFirstUpdate && !this.m_bFreezed && !this.m_bReplay) {
         if (!this.bAutoUpdate) {
            if (nKeyCode == 19 && bKeyState) {
               this.bAutoUpdate = true;
            } else {
               super.OnInputKey(nKeyCode, bKeyState, nKeyAscii);
            }
         } else {
            if (SoccerGame.m_bDebug) {
               this.OnInputKeyDebug(nKeyCode, bKeyState, nKeyAscii);
            }

            if (bKeyState) {
               switch (nKeyCode) {
                  case 12:
                  case 74:
                     this.ZoomOut();
                     break;
                  case 13:
                  case 78:
                     this.ZoomIn();
                     break;
                  case 59:
                     this.ChangeMode(0);
                     this.bAutoUpdate = true;
                     break;
                  case 60:
                     this.ChangeMode(1);
                     this.bAutoUpdate = true;
                     break;
                  case 61:
                     this.ChangeMode(2);
                     this.bAutoUpdate = true;
                     break;
                  case 62:
                     this.ChangeMode(3);
                     this.bAutoUpdate = true;
               }
            }
         }
      }
   }

   void AttachSound(String wavefile) {
      this.DetachSound();
      this.nSound = this.coSpeaker.AttachSound(wavefile);
   }

   void DetachDisasterSound() {
      if (this.nDisasterSound != -1) {
         if (this.coSpeaker != null) {
            this.coSpeaker.DetachSound(this.nDisasterSound);
         }

         this.nDisasterSound = -1;
      }
   }

   void ZoomIn() {
      float fCurCamDistance = this.GetCameraZoom(this.m_nMode);
      fCurCamDistance -= this.m_fCamZoomDelta;
      if (fCurCamDistance >= this.m_fCamMinDistance) {
         this.SetCameraZoom(this.m_nMode, fCurCamDistance);
         this.m_vCameraDistance.Normalize();
         this.m_vCameraDistance.Mul(fCurCamDistance);
      }
   }
}
