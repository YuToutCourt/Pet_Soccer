class Recorder {
   static int m_nRecordersPlaying = 0;
   static int m_nRecordersCount = 0;
   int _RECORD_TIME = 7;
   int _RECORD_FREQ = 0;
   int _RECORD_COUNT = 0;
   MeshObject m_cRecordedObject = null;
   Clock m_cModuleClockRef = null;
   static Clock m_cClock = null;
   ReplayData[] m_tReplayData = null;
   int m_nCurSnapShot = 0;
   int m_nPlaySnapShot = 0;
   float m_fPlayingStartTime = 0.0F;
   float m_fPlayingStartTimeReal = 0.0F;
   float m_fRecordingStartTime = 0.0F;
   boolean m_bPlaying = false;
   static int PROFILE_SnapShot = 0;
   static int PROFILE_SnapBall = 0;
   static int PROFILE_SnapFB = 0;

   ReplayData GetData(int nSnapShot) {
      int nRealIndex = nSnapShot % this._RECORD_COUNT;
      return this.m_tReplayData[nRealIndex];
   }

   void SnapShot() {
      if (PROFILE_SnapShot == 0) {
         PROFILE_SnapShot = SoccerGame.GetSoccerModule().ProfileAdd("Soccer", "Snapshot");
         PROFILE_SnapBall = SoccerGame.GetSoccerModule().ProfileAdd("Soccer", "Snap(Ball)");
         PROFILE_SnapFB = SoccerGame.GetSoccerModule().ProfileAdd("Soccer", "Snap(FB)");
      }

      SoccerGame.GetSoccerModule().ProfileBegin(PROFILE_SnapShot);
      float fDeltaTime = m_cClock.GetMatchTimeF() - this.m_fRecordingStartTime;
      int nPrevSnap = this.m_nCurSnapShot;
      this.m_nCurSnapShot = this.GetCurSnapshotFromTime(fDeltaTime);
      if (this.m_cRecordedObject instanceof Ball) {
         SoccerGame.GetSoccerModule().ProfileBegin(PROFILE_SnapBall);
         boolean bHasOwner = ((Ball)this.m_cRecordedObject).HasOwner();
         Vector vPos = this.m_cRecordedObject.GetPositionVectorVolatile();
         Vector vPosWall1 = null;
         Vector vPosWall2 = null;
         SuperWall cWall1 = ((AITeam)SoccerModule.m_cMatch.GetTeam(0)).m_cSuperWall;
         SuperWall cWall2 = ((AITeam)SoccerModule.m_cMatch.GetTeam(1)).m_cSuperWall;
         vPosWall1 = cWall1.GetPositionVectorVolatile();
         vPosWall2 = cWall2.GetPositionVectorVolatile();
         ReplayData rPrev = this.GetData(nPrevSnap);
         if (this.m_nCurSnapShot > 0) {
            int nNum = this.m_nCurSnapShot - nPrevSnap;
            if (nNum > 1) {
               Vector vDelta = vPos.Sub2(rPrev.vPos);
               Vector vDeltaWall1 = null;
               Vector vDeltaWall2 = null;
               if (rPrev.vDir != null && vPosWall1 != null) {
                  vDeltaWall1 = vPosWall1.Sub2(rPrev.vDir);
                  vDeltaWall1.Mul(1.0F / nNum);
               }

               if (rPrev.vSpeed != null && vPosWall2 != null) {
                  vDeltaWall2 = vPosWall2.Sub2(rPrev.vSpeed);
                  vDeltaWall2.Mul(1.0F / nNum);
               }

               vDelta.Mul(1.0F / nNum);

               for (int i = nPrevSnap + 1; i < this.m_nCurSnapShot; i++) {
                  ReplayData cPrev = this.GetData(i - 1);
                  Vector vPosInterpolated = cPrev.vPos.Add2(vDelta);
                  Vector vPosIntWall1 = null;
                  Vector vPosIntWall2 = null;
                  if (vDeltaWall1 != null) {
                     vPosIntWall1 = cPrev.vDir.Add2(vDeltaWall1);
                  }

                  if (vDeltaWall2 != null) {
                     vPosIntWall2 = cPrev.vSpeed.Add2(vDeltaWall2);
                  }

                  this.SetData(i, vPosInterpolated, vPosIntWall1, vPosIntWall2, null);
               }
            }
         }

         String sFX = "";
         if (this.m_nCurSnapShot == nPrevSnap && this.m_nCurSnapShot != 0) {
            sFX = this.GetData(this.m_nCurSnapShot).sFxName;
         }

         this.SetData(this.m_nCurSnapShot, vPos, vPosWall1, vPosWall2, sFX);
         SoccerGame.GetSoccerModule().ProfileEnd(PROFILE_SnapBall);
      }

      if (this.m_cRecordedObject instanceof FootBaller) {
         SoccerGame.GetSoccerModule().ProfileBegin(PROFILE_SnapFB);
         ReplayData rPrev = this.GetData(nPrevSnap);
         Vector vPos = this.m_cRecordedObject.GetPositionVectorVolatile();
         Vector vDir = this.m_cRecordedObject.GetForwardVectorVolatile();
         if (this.m_nCurSnapShot > 0) {
            int nNum = this.m_nCurSnapShot - nPrevSnap;
            if (nNum > 1) {
               Vector vDelta = vPos.Sub2(rPrev.vPos);
               vDelta.Mul(1.0F / nNum);

               for (int i = nPrevSnap + 1; i < this.m_nCurSnapShot; i++) {
                  Vector vPosInterpolated = this.GetData(i - 1).vPos.Add2(vDelta);
                  this.SetData(i, vPosInterpolated, vDir, null, "", 0, null);
               }
            }
         }

         if (this.m_nCurSnapShot == nPrevSnap && this.m_nCurSnapShot != 0) {
            ReplayData rNow = this.GetData(this.m_nCurSnapShot);
            String sFx = rNow.sFxName;
            if (rNow.sAnimName.length() != 0) {
               this.SetData(this.m_nCurSnapShot, vPos, vDir, null, rNow.sAnimName, rNow.nAnimRep, sFx);
            }

            if (sFx.length() > 0) {
               this.GetData(this.m_nCurSnapShot).SetFxName(sFx);
            }
         } else {
            this.SetData(this.m_nCurSnapShot, vPos, vDir, null, "", 0, null);
         }

         SoccerGame.GetSoccerModule().ProfileEnd(PROFILE_SnapFB);
      }

      SoccerGame.GetSoccerModule().ProfileEnd(PROFILE_SnapShot);
   }

   boolean PlaySnapShot() {
      float fDeltaTime = this.m_fPlayingStartTime + this.m_cRecordedObject.GetTime() - this.m_fPlayingStartTimeReal;
      int nPrevSnap = this.m_nPlaySnapShot;
      this.m_nPlaySnapShot = this.GetCurSnapshotFromTime(fDeltaTime);
      if (nPrevSnap == 0) {
         nPrevSnap = this.m_nCurSnapShot - this._RECORD_COUNT;
      }

      if (this.m_nCurSnapShot - this.m_nPlaySnapShot <= this._RECORD_FREQ * 1.5F) {
         this.TimerSetSpeedUp(0.33F);
      }

      if (this.m_nPlaySnapShot >= this.m_nCurSnapShot) {
         return false;
      }

      ReplayData r = this.GetData(this.m_nPlaySnapShot);
      if (this.m_cRecordedObject instanceof Ball) {
         if (nPrevSnap == this.m_nPlaySnapShot) {
            Vector vNextPos = this.GetData(this.m_nPlaySnapShot + 1).vPos;
            Vector vDelta = vNextPos.Sub2(r.vPos);
            float fTime = fDeltaTime - (float)this.m_nPlaySnapShot / this._RECORD_FREQ;
            float fCoeff = fTime / (1.0F / this._RECORD_FREQ);
            vDelta.Mul(fCoeff);
            Vector vPosInterpolated = r.vPos.Add2(vDelta);
            if (!vPosInterpolated.IsEqual(Vector.Zero)) {
               this.m_cRecordedObject.SetPosition(vPosInterpolated);
            }

            if (r.sFxName.length() > 0) {
               Vector vPos = this.m_cRecordedObject.GetPositionVectorVolatile();
               Vector vNormal = this.m_cRecordedObject.GetSpeedVectorVolatile();
               vPos.fZ = vPos.fZ - ((Ball)this.m_cRecordedObject).fBallRadius;
               if (vNormal.Len() == 0.0F) {
                  vNormal = Vector.Forward;
               }

               this.m_cRecordedObject.ExecuteFX(r.sFxName, vPos, vNormal);
            }
         } else {
            for (int i = nPrevSnap + 1; i <= this.m_nPlaySnapShot; i++) {
               r = this.GetData(i);
               if (!r.vPos.IsEqual(Vector.Zero)) {
                  this.m_cRecordedObject.SetPosition(r.vPos);
               }

               SuperWall cWall1 = ((AITeam)SoccerModule.m_cMatch.GetTeam(0)).m_cSuperWall;
               SuperWall cWall2 = ((AITeam)SoccerModule.m_cMatch.GetTeam(1)).m_cSuperWall;
               cWall1.SetPosition(r.vDir);
               cWall2.SetPosition(r.vSpeed);
               if (r.sFxName.length() > 0) {
                  Vector vPos = this.m_cRecordedObject.GetPositionVectorVolatile();
                  Vector vNormal = this.m_cRecordedObject.GetSpeedVectorVolatile();
                  vPos.fZ = vPos.fZ - ((Ball)this.m_cRecordedObject).fBallRadius;
                  if (vNormal.Len() == 0.0F) {
                     vNormal = Vector.Forward;
                  }

                  this.m_cRecordedObject.ExecuteFX(r.sFxName, vPos, vNormal);
               }
            }
         }
      }

      if (this.m_cRecordedObject instanceof FootBaller) {
         String sFX = "";
         String sAnim = "";
         int nAnimRep = 0;
         if (nPrevSnap == this.m_nCurSnapShot - this._RECORD_COUNT) {
            nPrevSnap--;
         }

         for (int i = nPrevSnap + 1; i < this.m_nPlaySnapShot; i++) {
            r = this.GetData(i);
            if (r.sAnimName.length() > 0) {
               sAnim = r.sAnimName;
               nAnimRep = r.nAnimRep;
            }

            if (r.sFxName.length() > 0) {
               sFX = r.sFxName;
            }
         }

         r = this.GetData(this.m_nPlaySnapShot);
         ((AIFootBaller)this.m_cRecordedObject).SetPositionWithY(r.vPos);
         this.m_cRecordedObject.SetDir(r.vDir);
         this.m_cRecordedObject.SetSpeed(r.vSpeed);
         if (r.sAnimName.length() > 0) {
            sAnim = r.sAnimName;
            nAnimRep = r.nAnimRep;
         }

         if (r.sFxName.length() > 0) {
            sFX = r.sFxName;
         }

         if (sAnim.length() > 0) {
            this.m_cRecordedObject.PlayAnim(sAnim, nAnimRep);
            sAnim = sAnim + "(played)";
         }

         if (sFX.length() > 0) {
            if (sFX.equalsIgnoreCase("FXSuperSpeed")) {
               ((AIFootBaller)this.m_cRecordedObject).PlaySuperSpeedFX();
            }

            if (sFX.equalsIgnoreCase("FXKick")) {
               ((AIFootBaller)this.m_cRecordedObject).PlayKickFX();
            }

            if (sFX.equalsIgnoreCase("FXHit")) {
               ((AIFootBaller)this.m_cRecordedObject).PlayHitBySuperBallFX();
            }
         }
      }

      return true;
   }

   public String toStringState() {
      ReplayData rPlay = this.GetData(this.m_nPlaySnapShot);
      ReplayData rCur = this.GetData(this.m_nCurSnapShot);
      return "Playing "
         + this.m_cRecordedObject
         + " "
         + this.m_bPlaying
         + "\n"
         + "   snap "
         + this.m_nCurSnapShot
         + " "
         + rCur
         + "\n"
         + "   play "
         + this.m_nPlaySnapShot
         + " "
         + rPlay
         + "\n";
   }

   void SetRecorded(MeshObject cObject) {
      if (cObject instanceof Ball) {
         this._RECORD_FREQ = 200;
      } else {
         this._RECORD_FREQ = 30;
      }

      this._RECORD_COUNT = this._RECORD_TIME * this._RECORD_FREQ;
      this.m_tReplayData = new ReplayData[this._RECORD_TIME * this._RECORD_FREQ];
      if (m_cClock == null) {
         m_cClock = new Clock();
      }

      m_nRecordersCount++;
      this.m_cRecordedObject = cObject;
      this.m_cModuleClockRef = SoccerGame.activeLevelModule.m_Clock;

      for (int i = 0; i < this._RECORD_COUNT; i++) {
         if (this.m_cRecordedObject instanceof Ball) {
            this.m_tReplayData[i] = new ReplayData(new Vector(), new Vector(), new Vector(), null, 0, null);
         }

         if (this.m_cRecordedObject instanceof FootBaller) {
            this.m_tReplayData[i] = new ReplayData(new Vector(), new Vector(), new Vector(), new String(""), 0, null);
         }
      }
   }

   void ReplayStop() {
      ((SoccerModule)this.m_cRecordedObject.GetModule()).DisableCallMethod("CallReplayBreak");
      ((SoccerModule)this.m_cRecordedObject.GetModule()).CallMethodIn("CallReplayBreak", 1.0F);
      m_nRecordersPlaying += -1;
      if (m_nRecordersPlaying < 0) {
         m_nRecordersPlaying = 0;
      }

      ReplayData r = this.GetData(this.m_nCurSnapShot);
      if (this.m_cRecordedObject instanceof Ball) {
         this.m_cRecordedObject.SetFlag(32);
         this.m_cRecordedObject.ClearCollFlags(8192);
         this.m_cRecordedObject.SetPhysicsType(6);
         this.m_cRecordedObject.SetPhysicsActive(true);
         ReplayData rCur = this.GetData(this.m_nCurSnapShot);
         SuperWall cWall1 = ((AITeam)SoccerModule.m_cMatch.GetTeam(0)).m_cSuperWall;
         cWall1.SetPosition(r.vDir);
         SuperWall cWall2 = ((AITeam)SoccerModule.m_cMatch.GetTeam(1)).m_cSuperWall;
         cWall2.SetPosition(r.vSpeed);
      }

      if (this.m_cRecordedObject instanceof FootBaller) {
         this.m_cRecordedObject.SetFlag(1);
         this.m_cRecordedObject.SetFlag(32);
         this.m_cRecordedObject.ClearCollFlags(8192);
      }

      this.m_bPlaying = false;
      if (m_nRecordersPlaying == 0) {
         ((SoccerModule)this.m_cRecordedObject.GetModule()).DisableCallMethod("CallReplayBreak");
         m_cClock.Start();
         SoccerGame.GetCommModule().HideReplaySign();
         SoccerModule.m_cMatch.OnReplayStop();
         this.TimerSetSpeedUp(1.0F);
      }
   }

   void Record() {
      this.Reset();
   }

   void RecordStop() {
   }

   int GetCurSnapshotFromTime(float fDeltaTime) {
      int nSec = (int)Math.floor(fDeltaTime);
      float fT = fDeltaTime - nSec;
      fT /= 1.0F / this._RECORD_FREQ;
      int nInSec = (int)Math.floor(fT);
      return nSec * this._RECORD_FREQ + nInSec;
   }

   public String toStringData() {
      String s = new String("");
      int n = this.m_nCurSnapShot - this._RECORD_COUNT;
      if (n < 0) {
         n = 0;
      }

      for (int i = n; i < this.m_nCurSnapShot; i++) {
         s = s + i / 50 + "." + (i - i / 50 * 50) + " " + this.GetData(i) + "\n";
      }

      return s;
   }

   void Reset() {
      m_cClock.Reset();
      m_cClock.Start();
      this.m_fRecordingStartTime = m_cClock.GetMatchTimeF();
      this.m_bPlaying = false;
      this.m_nCurSnapShot = 0;
      this.m_nPlaySnapShot = 0;
   }

   void SnapShotFX(String sFx) {
      float fDeltaTime = m_cClock.GetMatchTimeF() - this.m_fRecordingStartTime;
      int nPrevSnap = this.m_nCurSnapShot;
      this.m_nCurSnapShot = this.GetCurSnapshotFromTime(fDeltaTime);
      this.GetData(this.m_nCurSnapShot).SetFxName(sFx);
   }

   void SnapShotWithAnim(String sAnim, int nRepetition) {
      float fDeltaTime = m_cClock.GetMatchTimeF() - this.m_fRecordingStartTime;
      int nPrevSnap = this.m_nCurSnapShot;
      this.m_nCurSnapShot = this.GetCurSnapshotFromTime(fDeltaTime);
      Vector vPos = this.m_cRecordedObject.GetPositionVectorVolatile();
      Vector vDir = this.m_cRecordedObject.GetForwardVectorVolatile();
      Vector vSpeed = this.m_cRecordedObject.GetSpeedVectorVolatile();
      ReplayData rPrev = this.GetData(nPrevSnap);
      String sFX = "";
      if (nPrevSnap == this.m_nCurSnapShot) {
         sFX = rPrev.sFxName;
      }

      if (this.m_nCurSnapShot > 0) {
         int nNum = this.m_nCurSnapShot - nPrevSnap;
         if (nNum > 1) {
            Vector vDelta = vPos.Sub2(rPrev.vPos);
            vDelta.Mul(1.0F / nNum);

            for (int i = nPrevSnap + 1; i < this.m_nCurSnapShot; i++) {
               Vector vPosInterpolated = this.GetData(i - 1).vPos.Add2(vDelta);
               this.SetData(i, vPosInterpolated, vDir, vSpeed, "", 0, null);
            }
         }
      }

      this.SetData(this.m_nCurSnapShot, vPos, vDir, vSpeed, sAnim, nRepetition, sFX);
   }

   void TimerSetSpeedUp(float fSpeedUp) {
      ModNet.OnSpeedUp(fSpeedUp); // [mod] en ligne : le ralenti est diffusé au client
      this.m_cRecordedObject.GetModule().TimerSetSpeedUp(ModMeta.Speed(fSpeedUp)); // [mod] méta : temps accéléré
      m_cClock.SetSpeedUp(fSpeedUp);
   }

   static void StopClock() {
      m_cClock.Stop();
   }

   static void StartClock() {
      m_cClock.Start();
   }

   void SetData(int nSnapShot, Vector vPos, Vector vDir, Vector vSpeed, String sAnim, int nAnimRep, String sFx) {
      ReplayData rData = this.GetData(nSnapShot);
      if (this.m_cRecordedObject instanceof FootBaller && nSnapShot >= this._RECORD_COUNT) {
         ReplayData rDataNext = this.GetData(nSnapShot + 1);
         if (rData.nAnimRep == -1 && rData.sAnimName.length() > 0 && rDataNext.sAnimName.length() == 0) {
            rDataNext.sAnimName = new String(rData.sAnimName);
            rDataNext.nAnimRep = rData.nAnimRep;
         }
      }

      rData.Set(vPos, vDir, vSpeed, sAnim, nAnimRep, sFx);
   }

   void SetData(int nSnapShot, Vector vPos, Vector vDir, Vector vSpeed, String sFx) {
      this.GetData(nSnapShot).Set(vPos, vDir, vSpeed, sFx);
   }

   void Replay() {
      if (m_nRecordersPlaying == 0) {
         this.m_cModuleClockRef.Stop();
         SoccerModule.HideHud();
         SoccerGame.GetCommModule().ShowReplaySign();
      }

      this.m_nPlaySnapShot = 0;
      m_nRecordersPlaying++;
      this.m_bPlaying = true;
      m_cClock.Stop();
      this.m_fPlayingStartTime = m_cClock.GetMatchTimeF() - this._RECORD_TIME;
      if (this.m_fPlayingStartTime < this.m_fRecordingStartTime) {
         this.m_fPlayingStartTime = this.m_fRecordingStartTime;
      }

      this.m_fPlayingStartTimeReal = this.m_cRecordedObject.GetTime();
      if (this.m_cRecordedObject instanceof Ball) {
         this.m_cRecordedObject.ClearFlag(32);
         this.m_cRecordedObject.SetCollFlags(8192);
         this.m_cRecordedObject.SetPhysicsType(0);
         this.m_cRecordedObject.SetPhysicsActive(false);
      }

      if (this.m_cRecordedObject instanceof FootBaller) {
         if (((AIFootBaller)this.m_cRecordedObject).HasBall()) {
            ((AIFootBaller)this.m_cRecordedObject).DetachBall();
         }

         this.m_cRecordedObject.ClearFlag(1);
         this.m_cRecordedObject.ClearFlag(32);
         this.m_cRecordedObject.SetCollFlags(8192);
      }
   }

   void delete() {
      this.m_cRecordedObject = null;
      this.m_cModuleClockRef = null;
      m_cClock = null;

      for (int i = 0; i < this.m_tReplayData.length; i++) {
         this.m_tReplayData[i].delete();
         this.m_tReplayData[i] = null;
      }

      this.m_tReplayData = null;
   }
}
