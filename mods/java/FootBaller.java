public class FootBaller extends Individual {
   boolean m_bTestKey = false;
   Recorder m_cRecorder = null;
   MeshObject cPointMesh = null;
   MeshObject cLight = null;
   int m_nNumberInTeam = -1;
   UIWindow m_cMapPoint = null;
   boolean m_bLiving = false;
   float m_fKeySpeedStart = 0.0F;
   float m_fKeyFallSpeedStart = 0.0F;
   float m_fKeyRestStart = 0.0F;
   boolean m_bKeySpeed = false;
   boolean m_bKeyFallSpeed = false;
   boolean m_bKeyRest = false;
   float m_fLast_N = 0.0F;
   float m_fLast_NE = 0.0F;
   float m_fLast_E = 0.0F;
   float m_fLast_SE = 0.0F;
   float m_fLast_S = 0.0F;
   float m_fLast_SW = 0.0F;
   float m_fLast_W = 0.0F;
   float m_fLast_NW = 0.0F;
   float m_fBallDribbleDistance = -5.0F;
   static final String _ATTACKER_ID = "ATTACKER";
   static final String _DEFENDER_ID = "DEFENDER";
   static final String _GOALKEEPER_ID = "GOALKEEPER";
   static final String _REFEREE_ID = "REFEREE";
   static final int KICK_FORWARD_SHOT = 150;
   public PatrolNode[] aPatrolPath = null;
   static final int _TYPE_ATTACKER = 0;
   static final int _TYPE_DEFENDER = 1;
   static final int _TYPE_GOALKEEPER = 2;
   static final int _TYPE_REFEREE = 3;
   static final String _RUN = "Bieganie";
   static final String _STRAFE_LEFT = "chodzenie_bokiem_lewo";
   static final String _STRAFE_RIGHT = "chodzenie_bokiem_prawo";
   static final String _GO_BACK = "cofanie";
   static final String _IDLE = "Stanie";
   static final String _SHOT = "Strzal";
   static final String _LOOKING = "Rozgladanie";
   static final String _REACT = "Reakcja";
   static final String _REACT_FALL = "Reakcja_z_upadkiem";
   static final String _UP = "Powstanie";
   static final String _WAIT_ACTIVE = "Czekaj_aktywnie";
   static final String ADVERB_REACT = "Typ_reakcji";
   static final String ADVERB_FREE_TIME = "Wolny_czas";
   static final String CTX_BLOCK = "zastawienie";
   static final String CTX_ATTACK = "wslizg";
   static final String CTX_FALL_FORWARD = "upadek_w_przod";
   static final String CTX_FALL_BACK = "upadek_w_tyl";
   static final String CTX_DEFENCE_LEFT_THROW = "rzut_lewo";
   static final String CTX_DEFENCE_RIGHT_THROW = "rzut_prawo";
   static final String CTX_DEFENCE_CATCH_UP = "chwyt_gora";
   static final String CTX_DEFENCE_CATCH_DOWN = "chwyt_dol";
   static final String CTX_DEFENCE_NONE = "bez_obrony";
   static final String CTX_RECEIVE_DOWN = "przyjecie_dol";
   static final String CTX_RECEIVE_BRUST = "przyjecie_klatka";
   static final String CTX_RECEIVE_HEAD = "glowka";
   static final String CTX_RECEIVE_REFEREE_TAKE = "sedzia_podnosi_pilke";
   static final String CTX_RECEIVE_REFEREE_PUT = "sedzia_zabiera_pilke";
   static final String CTX_RECEIVE_REFEREE_TOSS = "sedzia_losuje";
   static final String CTX_KICK = "strzal";
   static final String CTX_SIGN_REFEREE = "sedzia_daje_znak";
   static final String CTX_DECISION_REFEREE = "sedzia_przydziela";
   static final String CTX_WAIT_SUCCES = "sukces";
   static final String CTX_WAIT_UNLUCK = "pech";
   static final String CTX_WAIT_LOOKING = "rozgladanie";
   static final String CTX_WAIT_CLOWN = "rozgrzewka_pajacyk";
   static final String CTX_WAIT_CROTCH = "rozgrzewka_przysiady";
   static final String CTX_WAIT_BEND_SIDE = "rozgrzewka_sklony_boki";
   static final String CTX_WAIT_BEND_FORWARD = "rozgrzewka_sklony_przod";
   static final String CTX_WAIT_IDLE = "stoi_i_sapie";
   static final String CTX_BEND_LEFT = "obrot_lewo";
   static final String CTX_BEND_RIGHT = "obrot_prawo";
   static final String CTX_BEND_LEFT_RUN = "obrot_lewo_bieg";
   static final String CTX_BEND_RIGHT_RUN = "obrot_prawo_bieg";
   static final int DEFENCE_NONE = 0;
   static final int DEFENCE_THROW_LEFT = 1;
   static final int DEFENCE_THROW_RIGHT = 2;
   static final int DEFENCE_CATCH_UP = 3;
   static final int DEFENCE_CATCH_DOWN = 4;
   static final int _LEFT = 0;
   static final int _RIGHT = 1;
   static final int _FORWARD = 2;
   static final int _SLANT = 3;
   static final int _SHARP = 4;
   static final int _BACK_SLANT = 5;
   static final int _BACK = 6;
   int m_nType = 1;
   int m_nAnimalType = -1;
   float m_fBendTime = 1.0F;
   float m_fRunBendTime = 1.0F;
   int m_nClothes = 0;
   Strategy m_cStrategy = null;
   float m_fLastTimeKey_A = 0.0F;
   float m_fMaxLookAngle = 45.0F;
   float m_fMaxLookAngleV = 60.0F;
   float m_fMaxLookAngleVDown = 10.0F;
   Team m_cTeam = null;
   int m_nTeam = 0;
   SpawnPoint cSpawnPoint = null;
   SpawnPoint cSpawnPointStart = null;
   SpawnPoint cSpawnPointPresentation = null;
   SelectionObject cSelectionObj = null;
   boolean bInputFocus = false;
   boolean m_bFocusMark = false;
   String m_sSpeciesFile = null;
   static java.util.Vector m_vSpecies = null;
   float fFlatBallDistance = -1.0F;
   float fHeight = -1.0F;
   float fEllRadius = -1.0F;
   float fCanAttachBallDistance = -1.0F;
   boolean bBallKicked = false;
   float m_fKickTime = 0.0F;
   Vector vMeshExtents = null;
   float fLodCoeff = 0.99F;
   boolean m_bEllipsoidAdjustment = false;
   float fFS = 1500.0F;
   boolean bPoprzeczka = true;
   static final Vector vAlongXAxis = new Vector(1.0F, 0.0F, 0.0F);
   static final Vector vAlongYAxis = new Vector(0.0F, 1.0F, 0.0F);
   ControlObject cPrevLookTowards = null;
   boolean bShowFrame = false;
   int nFrameCount = 0;
   static int nNum = 0;
   int nMyNum = -1;

   public void OnFrame() {
      if (this.m_cRecorder != null) {
         if (!this.IsReplaying()) {
            this.m_cRecorder.SnapShot();
         } else {
            if (this.bShowFrame) {
               this.nFrameCount++;
            }

            if (!this.m_cRecorder.PlaySnapShot()) {
               this.ReplayStop();
            }
         }
      }
   }

   public void OnDestroy() {
      this.cPrevLookTowards = null;
      this.m_cTeam = null;
      if (m_vSpecies != null) {
         for (int i = 0; i < m_vSpecies.size(); i++) {
            ((Species)m_vSpecies.elementAt(i)).delete();
         }

         m_vSpecies.removeAllElements();
      }

      m_vSpecies = null;
      this.cSpawnPoint = null;
      this.cSpawnPointStart = null;
      this.cSpawnPointPresentation = null;
      this.cSelectionObj = null;
      this.m_cStrategy = null;
      this.cPointMesh = null;
      this.cLight = null;
      if (this.m_cMapPoint != null) {
         this.m_cMapPoint.Remove();
         this.m_cMapPoint.delete();
      }

      this.m_cMapPoint = null;
      if (this.m_cRecorder != null) {
         this.m_cRecorder.delete();
      }

      this.m_cRecorder = null;
   }

   void RemoveSpecies(String sName) {
      if (m_vSpecies != null) {
         Species cSpecies = null;
         int nSize = m_vSpecies.size();

         for (int i = 0; i < nSize; i++) {
            cSpecies = (Species)m_vSpecies.elementAt(i);
            if (cSpecies.m_sName.equals(sName)) {
               m_vSpecies.removeElement(cSpecies);
               break;
            }
         }
      }
   }

   void SetKeyChange() {
      GameObject.LogLine(" Set key change ");
      Vector vNewDir = this.m_cTeam.GetTeamController().GetFieldDirFromKeys();
      if (vNewDir != null) {
         if (vNewDir.IsEqual(GameField._DIR_N)) {
            GameObject.LogLine(" Set key change N");
            if (this.CanKeySpeed()) {
               if (SoccerGame.GetSoccerModule().GetTime() - this.m_fLast_N < this.m_cStrategy.m_fKeySpeedSens) {
                  this.SetKeySpeed();
               } else {
                  GameObject.LogLine(" Set key change N - fix last");
                  this.m_fLast_N = this.GetTime();
               }
            }
         }

         if (vNewDir.IsEqual(GameField._DIR_NE) && this.CanKeySpeed()) {
            GameObject.LogLine(" Set key change NE");
            if (SoccerGame.GetSoccerModule().GetTime() - this.m_fLast_NE < this.m_cStrategy.m_fKeySpeedSens) {
               this.SetKeySpeed();
            } else {
               this.m_fLast_NE = this.GetTime();
            }
         }

         if (vNewDir.IsEqual(GameField._DIR_E) && this.CanKeySpeed()) {
            GameObject.LogLine(" Set key change E");
            if (SoccerGame.GetSoccerModule().GetTime() - this.m_fLast_E < this.m_cStrategy.m_fKeySpeedSens) {
               this.SetKeySpeed();
            } else {
               this.m_fLast_E = this.GetTime();
            }
         }

         if (vNewDir.IsEqual(GameField._DIR_SE) && this.CanKeySpeed()) {
            GameObject.LogLine(" Set key change SE");
            if (SoccerGame.GetSoccerModule().GetTime() - this.m_fLast_SE < this.m_cStrategy.m_fKeySpeedSens) {
               this.SetKeySpeed();
            } else {
               this.m_fLast_SE = this.GetTime();
            }
         }

         if (vNewDir.IsEqual(GameField._DIR_S) && this.CanKeySpeed()) {
            if (SoccerGame.GetSoccerModule().GetTime() - this.m_fLast_S < this.m_cStrategy.m_fKeySpeedSens) {
               this.SetKeySpeed();
            } else {
               this.m_fLast_S = this.GetTime();
            }
         }

         if (vNewDir.IsEqual(GameField._DIR_SW) && this.CanKeySpeed()) {
            GameObject.LogLine(" Set key change SW");
            if (SoccerGame.GetSoccerModule().GetTime() - this.m_fLast_SW < this.m_cStrategy.m_fKeySpeedSens) {
               this.SetKeySpeed();
            } else {
               this.m_fLast_SW = this.GetTime();
            }
         }

         if (vNewDir.IsEqual(GameField._DIR_W) && this.CanKeySpeed()) {
            GameObject.LogLine(" Set key change W");
            if (SoccerGame.GetSoccerModule().GetTime() - this.m_fLast_W < this.m_cStrategy.m_fKeySpeedSens) {
               this.SetKeySpeed();
            } else {
               this.m_fLast_W = this.GetTime();
            }
         }

         if (vNewDir.IsEqual(GameField._DIR_NW)) {
            GameObject.LogLine(" Set key change NW");
            if (this.CanKeySpeed()) {
               if (SoccerGame.GetSoccerModule().GetTime() - this.m_fLast_NW < this.m_cStrategy.m_fKeySpeedSens) {
                  this.SetKeySpeed();
               } else {
                  this.m_fLast_NW = this.GetTime();
               }
            }
         }
      }
   }

   public void DelayFocus() {
      GameObject.LogLine(this + "DELAY FOCUS");
      if (this.m_cTeam.m_cFocusFBaller != null) {
         this.m_cTeam.m_cFocusFBaller.SetInputFocus(false, true);
      }

      this.m_cTeam.m_cFocusFBaller = this;
      this.SetInputFocus(true, true);
   }

   void SetKeyRest() {
      this.m_fKeyRestStart = SoccerGame.GetSoccerModule().GetTime();
      this.m_bKeyRest = true;
   }

   void AfterOnCreate() {
      if (CoreLogic.GetPlayer().m_bReplaysOn) {
         this.m_cRecorder = new Recorder();
         this.m_cRecorder.SetRecorded(this);
      }

      this.SetFlag(256);
      if (SoccerGame.m_bDebug) {
         this.SetLighting(SoccerModule.m_bFBLighting);
      } else {
         this.SetLighting(PerformanceSettings._LIGHTING[PerformanceSettings.m_iCurrQuality]);
      }

      this.SetFlag(32);
      this.ClearFlag(4);
      this.ClearFlag(2);
      this.SetAnimType((byte)4, (byte)3, false);
      this.SetAnimMorphTime(0.2F);
      this.SetCollAction(16);
      this.SetCollType(0);
      this.SetTraceCollType(6);
      this.SetCollHandler(1024);
      this.SetPhysicsType(5);
      this.SetPhysicsActive(true);
      this.RegisterKeyRequest();
      this.UpdateExtents();
      this.InitSelectionObject();
      this.SetMaxPhysicsUpdateInterval(0.0F);
      this.SetUnImportanceThreshold(50.0F);
      this.SetLODCoeff(PerformanceSettings._ANIMAL_LOD[PerformanceSettings.m_iCurrQuality]);
      this.SetMeshMaterialAmbient(0.4F, 0.4F, 0.4F);
      this.RegisterInInteractionMap();
   }

   void SetBallKicked() {
      this.bBallKicked = true;
      this.m_fKickTime = this.GetTime();
   }

   boolean HasBall() {
      return GameField.m_cBall.m_cCurOwner == this;
   }

   boolean HasFocusMark() {
      return this.m_bFocusMark;
   }

   void ResetSpawnPosition() {
      if (this.cSpawnPoint == null) {
         GameObject.CrashLog("[FootBaller.SetSpawnPosition] brak spawnpointa");
      } else {
         this.SetPositionLog(this.cSpawnPoint.GetPositionLog());
      }

      if (this.m_nTeam == 0) {
         this.SetDir(GameField._DIR_E);
      } else {
         this.SetDir(GameField._DIR_W);
      }
   }

   void ResetPresentationPosition() {
      if (this.cSpawnPoint == null) {
         GameObject.CrashLog("[FootBaller.SetSpawnPosition] brak spawnpointa");
      } else {
         this.SetPositionLog(this.cSpawnPointPresentation.GetPositionLog());
      }

      this.SetDir(GameField._DIR_S);
   }

   void ClearKeyRest() {
      this.m_bKeyRest = false;
      this.SetLocalSpeed(this.GetLocalSpeed());
   }

   void StopMovement() {
      this.SetSpeed(Vector.Zero);
      this.SetAngSpeed(0.0F);
      this.SetState("IdleState");
   }

   boolean CanKeySpeed() {
      return !this.m_bKeySpeed && !this.m_bKeyFallSpeed && !this.m_bKeyRest;
   }

   void SetKeySpeed() {
      this.m_fKeySpeedStart = SoccerGame.GetSoccerModule().GetTime();
      this.m_bKeySpeed = true;
      GameObject.LogLine(this + " SetKey spedd ");
   }

   public void ChangeActiveInTeam() {
      ((AITeam)this.m_cTeam).ChangeActive();
   }

   void OnInputKeyDebug(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      if (bKeyState) {
         switch (nKeyCode) {
            case 50:
               this.SetFallSpeed(new Vector(0.0F, this.fFS, 0.0F));
               break;
            case 57:
               if (this.bInputFocus && bKeyState && this.HasBall() && !((AIFootBaller)this).Busy()) {
                  ((AIFootBaller)this).KickForward();
               }
         }
      }
   }

   boolean IsKeySpeed() {
      return this.m_bKeySpeed;
   }

   void CheckKeySpeed() {
      if (this.m_bKeySpeed && SoccerGame.GetSoccerModule().GetTime() > this.m_fKeySpeedStart + this.m_cStrategy.m_fMaxKeySpeedTime) {
         this.ClearKeySpeed();
      }

      if (this.m_bKeyFallSpeed && SoccerGame.GetSoccerModule().GetTime() > this.m_fKeyFallSpeedStart + this.m_cStrategy.m_fMaxKeyFallSpeedTime) {
         this.ClearKeyFallSpeed();
      }

      if (this.m_bKeyRest && SoccerGame.GetSoccerModule().GetTime() > this.m_fKeyRestStart + this.m_cStrategy.m_fMaxKeyRestTime) {
         this.ClearKeyRest();
      }
   }

   public int ExecuteFX(String sFXName, Vector pos, Vector normal) {
      // [mod] en ligne : l'hôte diffuse l'effet, le client ne joue que ceux de l'hôte
      if (!ModNet.OnFX(ModNet.ObjIndex(this), sFXName, pos, normal)) {
         return 0;
      }

      if (this.m_cRecorder != null && !this.IsReplaying()) {
         this.m_cRecorder.SnapShotFX(sFXName);
      }

      return super.ExecuteFX(sFXName, pos, normal);
   }

   // [mod] en ligne : sons d'un joueur (tacle, choc du super tir), diffusés par l'hôte
   public void PlaySound(String sSound) {
      if (!ModNet.OnSound(ModNet._SND_PLAYER, this, sSound)) {
         return;
      }

      super.PlaySound(sSound);
   }

   public int ExecuteFX(String sFXName, Vector pos) {
      if (!ModNet.OnFX(ModNet.ObjIndex(this), sFXName, pos, null)) { // [mod]
         return 0;
      }

      if (this.m_cRecorder != null && !this.IsReplaying()) {
         this.m_cRecorder.SnapShotFX(sFXName);
      }

      return super.ExecuteFX(sFXName, pos);
   }

   public int PlayAnim(String sAnim, int nRep) {
      if (this.m_cRecorder != null && !this.IsReplaying()) {
         this.m_cRecorder.SnapShotWithAnim(sAnim, nRep);
      }

      // [mod] en ligne : l'hôte diffuse l'animation, le client ne joue que celles de l'hôte
      if (!ModNet.OnAnim(this, sAnim, nRep)) {
         return 0;
      }

      return super.PlayAnim(sAnim, nRep);
   }

   public void SetDir(Vector vDir) {
      boolean bError = false;
      if (vDir.fY == -1.0F && SoccerGame.m_bDebug) {
         Tools.CrashLogWarning(this + " SetDir " + vDir + "\n");
         bError = true;
      }

      if (vDir.fY < 0.0F) {
         vDir.fY = 0.0F;
      }

      if (vDir.Len() != 0.0F) {
         super.SetDir(vDir);
      }
   }

   boolean IsKeyrest() {
      return this.m_bKeyRest;
   }

   void PlayKickFX() {
   }

   void RotateHead(float fAngle) {
      this.BoneRotate((byte)4, vAlongXAxis, fAngle, false);
   }

   void ResetBallKicked() {
      this.bBallKicked = false;
      if (!this.HasBall()) {
         ((AIFootBaller)this).SetBallInteraction();
      }
   }

   public void SetSpecies(Species species) {
      super.SetSpecies(species);
      EAnimals.SetAppearance(this, this.m_nAnimalType, this.m_nClothes, this.m_nType);
      Verb cReactVerb = species.GetVerb("Reakcja");
      Animation cBendAnim = cReactVerb.GetStartAnimation("obrot_lewo");
      Animation cRunBendAnim = cReactVerb.GetStartAnimation("obrot_z_biegu_lewo");
      if (cBendAnim.m_fFrequency > 0.0F) {
         this.SetAnimFreq(cBendAnim.GetName(), cBendAnim.m_fFrequency);
      }

      this.m_fBendTime = this.GetAnimLength(cBendAnim.GetName());
      if (cRunBendAnim.m_fFrequency > 0.0F) {
         this.SetAnimFreq(cRunBendAnim.GetName(), cRunBendAnim.m_fFrequency);
      }

      this.m_fRunBendTime = this.GetAnimLength(cRunBendAnim.GetName());
      this.AfterOnCreate();
   }

   void SetInputFocus(boolean bSet, boolean bUserControl) {
      if (this.cSelectionObj == null) {
         GameObject.CrashLog("[FootBaller.SetInputFocus] nie ma obiektu podswietlenia\n");
      } else {
         if (bSet) {
            this.bInputFocus = true;
            this.UpdateMovement();
         } else {
            if (this.IsKeySpeed()) {
               this.ClearKeySpeed();
            }

            this.bInputFocus = false;
         }
      }
   }

   public void ChangeDirection(float fAngle, float fTime) {
      this.DisableCallMethod("EndChangeDirection");
      float fAttrib = fAngle / fTime;
      this.SetAngSpeed(fAttrib);
      this.CallMethodIn("EndChangeDirection", fTime);
   }

   void LogRecorder(int i) {
      if (this.nMyNum == i && this.m_cRecorder != null) {
         GameObject.CrashLog(this.m_cRecorder.toStringData());
      }
   }

   public void SetPositionLog(Vector vFieldPos) {
      Vector vWorldPos = GameField.FieldToWorld(vFieldPos);
      super.SetPosition(vWorldPos);
   }

   void SetDirTo(FieldObject obj) {
      Vector p = obj.GetPosition();
      p.Sub(this.GetPosition());
      if (p.Len() > 0.0F) {
         p.fY = 0.0F;
         p.Normalize();
         this.SetDir(p);
      }
   }

   void Init(SpawnPoint cPoint, SpawnPoint cPointStart, SpawnPoint cPointPresentation, int nTeam) {
      if (nTeam != -1) {
         this.SetTeam(nTeam);
      }

      this.SetSpawnPoint(cPoint, cPointStart, cPointPresentation);
      this.m_nType = cPoint.GetFootballerType();
      GameObject.LogLine(this + " fb: type " + this.m_nType);
      switch (this.m_nType) {
         case 0:
         case 1:
      }
   }

   void EllipsoidAdjustment(int nKeyCode) {
      Vector vMinExtent = new Vector();
      Vector vMaxExtent = new Vector();
      this.GetMeshExtents(vMinExtent, vMaxExtent);
      vMaxExtent.Sub(vMinExtent);
      this.vMeshExtents = new Vector(vMaxExtent);
      Vector vEllipsoid = new Vector(this.vMeshExtents.fX / 2.0F, this.vMeshExtents.fY, this.vMeshExtents.fY / 2.0F);
      Tools.MovePointByKeys(vEllipsoid, nKeyCode, 0.0F);
      this.SetEllipsoid(vEllipsoid.fX, vEllipsoid.fY, vEllipsoid.fZ);
      this.GetMeshExtents(vMinExtent, vMaxExtent);
      vMaxExtent.Sub(vMinExtent);
      this.vMeshExtents = new Vector(vMaxExtent);
      Tools.ShowInfo("Setting Ellipsoid. Extents = " + this.vMeshExtents, this.GetModule(), Tools._INFO_POS_TOP);
   }

   void Bend(FootBaller.DirAction cDirAction) {
      float fAngle = 0.0F;
      float fTime = 0.0F;
      if (cDirAction.m_bPartition) {
         switch (cDirAction.m_nType) {
            case 2:
               return;
            case 3:
               fTime = this.m_cStrategy.m_fSlantDirTime;
               break;
            case 4:
               if (cDirAction.m_bRunning) {
                  fTime = this.m_fRunBendTime;
               } else {
                  fTime = this.m_fBendTime;
               }
               break;
            case 5:
               if (cDirAction.m_bRunning) {
                  fTime = this.m_fRunBendTime;
               } else {
                  fTime = this.m_fBendTime;
               }
               break;
            case 6:
               if (cDirAction.m_bRunning) {
                  fTime = this.m_fRunBendTime;
               } else {
                  fTime = this.m_fBendTime;
               }
         }
      }

      fAngle = cDirAction.m_fAngle;
      this.ChangeDirection(fAngle, fTime);
   }

   void LookTowards(ControlObject obj) {
      if (obj == null && this.cPrevLookTowards != null) {
         this.RotateHead(0.0F);
         this.BoneRotate((byte)4, vAlongYAxis, 0.0F, false);
      } else {
         this.cPrevLookTowards = obj;
         Vector vLookTo = obj.GetPositionVectorVolatile();
         Vector vPos = this.GetPositionVectorVolatile();
         vLookTo.Sub(vPos);
         vLookTo.Normalize();
         Vector vHForward = this.GetForwardVectorVolatile();
         vHForward.fY = 0.0F;
         vHForward.Normalize();
         Vector vHLook = vLookTo;
         vHLook.fY = 0.0F;
         vHLook.Normalize();
         float fHDot = vHLook.Dot(vHForward);
         if (fHDot > 1.0F) {
            fHDot = 1.0F;
         }

         if (fHDot < -1.0F) {
            fHDot = -1.0F;
         }

         float fHAngle = (float)Math.acos(fHDot) * 180.0F / (float) Math.PI;
         boolean bHAngleMax = false;
         if (Math.abs(fHAngle) > this.m_fMaxLookAngle) {
            bHAngleMax = true;
            fHAngle = 0.0F;
         }

         if (vHLook.Dot(this.GetRightVectorVolatile()) < 0.0F) {
            fHAngle *= -1.0F;
         }

         vLookTo = obj.GetPositionVectorVolatile();
         vPos.fY = vPos.fY + this.fHeight;
         float fDistance = vPos.Distance(vLookTo);
         float fObjHeight = vLookTo.fY - this.TerrainHeight(vLookTo.fX, vLookTo.fZ);
         float fSin = 0.0F;
         if (fObjHeight > this.fHeight) {
            fSin = (fObjHeight - this.fHeight) / fDistance;
         } else {
            fSin = (this.fHeight - fObjHeight) / fDistance;
         }

         if (fSin > 1.0F) {
            fSin = 1.0F;
         }

         if (fSin < -1.0F) {
            fSin = -1.0F;
         }

         float fVAngle = (float)Math.asin(fSin) * 180.0F / (float) Math.PI;
         if (fObjHeight > this.fHeight) {
            fVAngle *= -1.0F;
         }

         if (fVAngle > this.m_fMaxLookAngleVDown) {
            fVAngle = this.m_fMaxLookAngleVDown;
         }

         if (fVAngle < -this.m_fMaxLookAngleV) {
            fVAngle = -this.m_fMaxLookAngleV;
         }

         this.RotateHead(fHAngle);
         if (bHAngleMax && !((AIFootBaller)this).HasFocus() && !this.IsReplaying()) {
            AIFootBaller cAIThis = (AIFootBaller)this;
            if (!cAIThis.GoalUnreached() && !this.IsActivity("Bieganie") && AIMatch.IsMatchState(0)) {
               this.m_cStrategy.SetDirToBall((AIFootBaller)this);
            }
         }

         this.RotateHeadV(0.0F);
         this.RotateHead(fHAngle);
         this.RotateHeadV(fVAngle);
      }
   }

   void RotateHeadV(float fAngle) {
      if (fAngle != 0.0F) {
         this.BoneRotate((byte)4, vAlongYAxis, fAngle, true);
      } else {
         this.BoneRotate((byte)4, vAlongYAxis, fAngle, false);
      }
   }

   void ClearKeySpeed() {
      this.m_bKeySpeed = false;
      this.SetKeyFallSpeed();
      this.SetLocalSpeed(this.GetLocalSpeed());
      GameObject.LogLine(this + " SetKey spedd false");
   }

   int GetZoneVertical() {
      return GameField.GetZoneForPointVertical(this.GetPositionVectorVolatile());
   }

   void SetFocusMark() {
      this.m_bFocusMark = true;
      this.cSelectionObj.Show();
   }

   void ResetSpawnStartPosition() {
      if (this.cSpawnPoint == null) {
         GameObject.CrashLog("[FootBaller.SetSpawnPosition] brak spawnpointa-startowego");
      } else {
         this.SetPositionLog(this.cSpawnPointStart.GetPositionLog());
      }

      this.SetDir(GameField._DIR_N);
   }

   void EndChangeDirection() {
      this.SetAngSpeed(0.0F);
      this.UpdateSpeed();
   }

   boolean IsGoalKeeper() {
      return this.m_nType == 2;
   }

   void InitSelectionObject() {
      this.cSelectionObj = (SelectionObject)this.CreateObject("SelectionObject");
      this.GetModule().AddControlObject(this.cSelectionObj);
      this.cSelectionObj.AttachTo(this);
   }

   public Vector GetPositionLog() {
      Vector vFieldPos = super.GetPosition();
      return GameField.WorldToField(vFieldPos);
   }

   boolean HasToHeadLob() {
      return this.m_fLastTimeKey_A + this.m_cStrategy.m_fLastTimeKey_A > this.GetTime();
   }

   void SetSpawnPoint(SpawnPoint cPoint, SpawnPoint cPointStart, SpawnPoint cPointPresentation) {
      if (cPoint == null) {
         GameObject.CrashLog(this + " brak spawnpointa");
      } else {
         this.cSpawnPoint = cPoint;
         this.aPatrolPath = this.cSpawnPoint.aPatrolPath;
         this.cSpawnPointStart = cPointStart;
         this.cSpawnPointPresentation = cPointPresentation;
      }
   }

   void SetKeyFallSpeed() {
      this.m_fKeyFallSpeedStart = SoccerGame.GetSoccerModule().GetTime();
      this.m_bKeyFallSpeed = true;
   }

   boolean IsKeyFallSpeed() {
      return this.m_bKeyFallSpeed;
   }

   public void SetSuperWallForTeam() {
      if (this.IsGoalKeeper()) {
         this.SetInteraction(((AITeam)this.m_cTeam).m_cSuperWall, false);
      }
   }

   void ReplayStop() {
      if (this.m_cRecorder != null) {
         this.m_cRecorder.ReplayStop();
      }
   }

   public void OnCollisionFullInfoMove(ControlObject obj, Vector vPoint, Vector vNormal, Vector vMove) {
   }

   void UseSuper() {
      if (this.m_cTeam.m_bSuperAvailable) {
         this.m_cTeam.m_bSuperFeint = false;
         this.m_cTeam.m_bSuperSpeed = false;
         switch (this.m_cTeam.m_nSuperType) {
            case 0:
               if (this.HasBall()) {
                  ((AIFootBaller)this).DoSuperShot();
                  this.m_cTeam.ResetSuper();
                  this.m_cTeam.IncreaseSuperCounter();
                  this.m_cTeam.SetSuperDoing();
               }
               break;
            case 1:
               if (this.HasBall()) {
                  ((AIFootBaller)this).DoSuperFeint();
                  this.m_cTeam.ResetSuper();
                  this.m_cTeam.IncreaseSuperCounter();
               }
               break;
            case 2:
               ((AIFootBaller)this).DoSuperSpeed();
               this.m_cTeam.ResetSuper();
               this.m_cTeam.IncreaseSuperCounter();
               break;
            case 3:
               ((AITeam)this.m_cTeam).RequestSuperDefence();
               this.m_cTeam.ResetSuper();
               this.m_cTeam.IncreaseSuperCounter();
         }

         SoccerModule.m_cMatch.m_bWasSuperCommeneted = false;
         if (this.m_cTeam.m_nSuperType != 3) {
            SoccerModule.m_cMatch.m_bSuperCommentWithBaller = false;
         } else {
            SoccerModule.m_cMatch.m_bSuperCommentWithBaller = true;
         }
      }
   }

   void SetAnimalType(int nAnimalType, int nClothes) {
      this.m_nAnimalType = nAnimalType;
      this.m_nClothes = nClothes;
   }

   void ChangeDirectionLogic(FootBaller.DirAction cDirAction) {
      if (cDirAction != null) {
         float fAngle = 0.0F;
         float fTime = 0.0F;
         if (cDirAction.m_vDir != null) {
            this.SetDir(cDirAction.m_vDir);
            this.UpdateSpeed();
         }
      }
   }

   void ClearSensKeys() {
      this.m_fLast_N = 0.0F;
      this.m_fLast_NE = 0.0F;
      this.m_fLast_E = 0.0F;
      this.m_fLast_SE = 0.0F;
      this.m_fLast_S = 0.0F;
      this.m_fLast_SW = 0.0F;
      this.m_fLast_W = 0.0F;
      this.m_fLast_NW = 0.0F;
   }

   void SetLocalDest(Vector vPos) {
      if (SoccerGame.m_bDebug && this.cPointMesh != null) {
         this.cPointMesh.SetPosition(vPos);
      }
   }

   Species AddSpecies(String sFileName) {
      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("Addspecies " + sFileName);
      }

      if (m_vSpecies == null) {
         m_vSpecies = new java.util.Vector();
      }

      Species cSpecies = new Species();
      cSpecies.LoadData(sFileName);
      if (this.IsGoalKeeper()) {
         cSpecies.m_sName = EAnimals.m_sSpecNameGK;
      } else {
         cSpecies.m_sName = EAnimals.m_sSpecName;
      }

      m_vSpecies.addElement(cSpecies);
      return cSpecies;
   }

   static Species GetSpecies(String sName) {
      if (m_vSpecies == null) {
         return null;
      }

      Species cSpecies = null;

      for (int i = 0; i < m_vSpecies.size(); i++) {
         cSpecies = (Species)m_vSpecies.elementAt(i);
         GameObject.LogLine(" Mam  " + cSpecies.m_sName + " a szukam " + sName);
         if (cSpecies.m_sName.equals(sName)) {
            break;
         }

         cSpecies = null;
      }

      return cSpecies;
   }

   static void RemoveAllSpecies() {
      if (m_vSpecies != null) {
         m_vSpecies.removeAllElements();
      }
   }

   void UpdateExtents() {
      if (this.vMeshExtents == null) {
         Vector vMinExtent = new Vector();
         Vector vMaxExtent = new Vector();
         this.GetMeshExtents(vMinExtent, vMaxExtent);
         vMaxExtent.Sub(vMinExtent);
         this.vMeshExtents = new Vector(vMaxExtent);
      }

      this.SetBlobMapSize(this.vMeshExtents.fX * 0.6F);
      this.SetEllipsoid(this.vMeshExtents.fX / 2.0F, this.vMeshExtents.fY, this.vMeshExtents.fY / 2.0F);
      this.fHeight = this.vMeshExtents.fY;
      this.fEllRadius = this.vMeshExtents.fX / 2.0F;
      float fAttachScope;
      if (this.IsGoalKeeper()) {
         if (((AIFootBaller)this).IsDefence()) {
            fAttachScope = 15.0F;
         } else {
            fAttachScope = 15.0F;
         }
      } else {
         fAttachScope = 10.0F;
      }

      this.fCanAttachBallDistance = this.vMeshExtents.fX / 2.0F + GameField.m_cBall.fBallRadius + fAttachScope;
   }

   public Vector GetPositionLogVolatile() {
      Vector vFieldPos = super.GetPositionVectorVolatile();
      return GameField.WorldToFieldSet(vFieldPos);
   }

   void ResetFocusMark() {
      this.m_bFocusMark = false;
      this.cSelectionObj.Hide();
   }

   boolean IsReferee() {
      return this.m_nType == 3;
   }

   void UpdateMovement() {
      TeamController cController = this.m_cTeam.GetTeamController();
      if (cController._KEY_STATES == 0) {
         this.ChangeActivity("Stanie");
         if (((AIFootBaller)this).HasFocus() && this.IsKeySpeed()) {
            this.ClearKeySpeed();
         }
      } else {
         Vector vNewDir = this.m_cTeam.GetTeamController().GetFieldDirFromKeys();
         if (vNewDir != null) {
            this.ChangeActivity("Bieganie");
            this.ChangeDirectionLogic(this.m_cStrategy.GetActionDir(this, vNewDir, true));
         }
      }
   }

   void ClearKeyFallSpeed() {
      this.m_bKeyFallSpeed = false;
      this.SetKeyRest();
      this.SetLocalSpeed(this.GetLocalSpeed());
   }

   void SetTeam(Team cTeam) {
      this.m_cTeam = cTeam;
   }

   void SetTeam(int nT) {
      this.m_nTeam = nT;
   }

   public void ResetSuperWallForTeam() {
   }

   void Record() {
      if (this.m_cRecorder != null) {
         this.m_cRecorder.Record();
      }
   }

   int GetZone() {
      return GameField.GetZoneForPoint(this.GetPositionVectorVolatile(), this.m_nTeam);
   }

   boolean IsReplaying() {
      return this.m_cRecorder != null ? this.m_cRecorder.m_bPlaying : false;
   }

   void SetAppearance(int nType) {
      switch (nType) {
         case 0:
         case 1:
         case 2:
         case 3:
      }
   }

   void CreateHelperMeshes() {
      this.cPointMesh = (MeshObject)this.CreateObject("MeshObject");
      this.GetModule().AddControlObject(this.cPointMesh);
      this.cPointMesh.MeshName = "data\\interface\\wykrzyknik.3da";
      this.cPointMesh.SetSkinElement("data/textures/red.png", 0);
      this.cPointMesh.LoadMesh();
      this.cPointMesh.SetScale(new Vector(0.25F, 0.25F, 0.25F));
      this.cPointMesh.ClearFlag(1);
      this.cPointMesh.SetPosition(this.GetPosition());
      this.cLight = (MeshObject)this.CreateObject("MeshObject");
      this.GetModule().AddControlObject(this.cLight);
      this.cLight.MeshName = "data\\interface\\zarowka.3da";
      this.cLight.LoadMesh();
      this.cLight.SetScale(new Vector(0.5F, 0.5F, 0.5F));
      this.cLight.ClearFlag(1);
   }

   public void OnCreate() {
      super.OnCreate();
   }

   void OnInputKey(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      if (SoccerGame.m_bDebug) {
         if (nKeyCode == 18 && bKeyState && this.bInputFocus) {
            this.m_bEllipsoidAdjustment = !this.m_bEllipsoidAdjustment;
            this.ShowCoarseBox(this.m_bEllipsoidAdjustment);
            Tools.ShowInfo("", this.GetModule(), Tools._INFO_POS_TOP);
            return;
         }

         if (this.m_bEllipsoidAdjustment && this.bInputFocus) {
            if (bKeyState) {
               this.EllipsoidAdjustment(nKeyCode);
            }

            return;
         }

         this.OnInputKeyDebug(nKeyCode, bKeyState, nKeyAscii);
      }

      switch (nKeyCode) {
         case 0:
      }
   }

   void Replay() {
      if (this.m_cRecorder != null) {
         this.m_cRecorder.Replay();
      }
   }

   void SetObserved() {
      this.nMyNum = nNum++;
   }

   void SetObserved(int i) {
      if (this.nMyNum == -1) {
         this.SetObserved();
      }

      SoccerCamera cam = ((SoccerModule)this.GetModule()).GetSoccerCamera();
      if (this.nMyNum == i) {
         cam.SetObserved(this);
      } else if (i == -1) {
         cam.SetObserved(GameField.m_cBall);
      }
   }

   public void GoLive() {
      GameObject.LogLine("JA LATAM : Start() ");
      this.m_bLiving = true;
      this.Start();
   }

   static class DirAction {
      int m_nOrient = -1;
      int m_nType = -1;
      float m_fAngle = 0.0F;
      Vector m_vDir = null;
      boolean m_bPartition = false;
      boolean m_bRunning = false;
      int m_nEventAfterBend = -1;
      boolean m_bRunAfterBend = false;

      DirAction() {
      }

      DirAction(int nOrient, int nType, float fAngle, Vector vDir, boolean bPartition, int nEventAfterBend) {
         this.m_nOrient = nOrient;
         this.m_nType = nType;
         this.m_fAngle = fAngle;
         this.m_vDir = vDir;
         this.m_bPartition = bPartition;
         this.m_bRunning = false;
         this.m_nEventAfterBend = nEventAfterBend;
         this.m_bRunAfterBend = false;
      }

      static String GetFlagName(int nFlag) {
         switch (nFlag) {
            case 0:
               return "_LEFT";
            case 1:
               return "_RIGHT";
            case 2:
               return "_FORWARD";
            case 3:
               return "_SLANT";
            case 4:
               return "_SHARP";
            case 5:
               return "_BACK_SLANT";
            case 6:
               return "_BACK";
            default:
               return "UNKNOWN";
         }
      }

      void delete() {
         this.m_vDir = null;
      }
   }
}
