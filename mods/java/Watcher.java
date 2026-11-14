public class Watcher extends FieldObject {
   String[] m_sRandomSounds = null;
   String[] m_sHappySounds = null;
   String[] m_sUnhappySounds = null;
   String[] m_sIdleAnims = null;
   int m_nCurAnim = -1;
   int m_nMyTeam = 0;
   int m_nTeamScored = -1;
   float fMinTime = 10.0F;
   float fMaxTime = 25.0F;
   float m_fMaxLookAngle = 70.0F;
   int m_nAnimalType = -1;
   boolean m_bMeshOk = false;
   float m_fHeight = 100.0F;
   static final Vector vAlongXAxis = new Vector(1.0F, 0.0F, 0.0F);
   static final Vector vAlongYAxis = new Vector(0.0F, 1.0F, 0.0F);
   float m_fAnimMorphTime = 0.5F;

   void OnFrame() {
      if (SoccerModule.m_bRenderPublic) {
         if (this.m_bMeshOk) {
            this.LookTowards(GameField.m_cBall);
         }
      }
   }

   void OnGoalReaction() {
      if (this.m_nTeamScored == this.m_nAnimalType) {
         Tools.PlayRandomWave(this, this.m_sHappySounds);
         if (this.m_bMeshOk) {
            this.m_nCurAnim = this.PlayAnim("publicznosc_radosc", SoccerGame.GetRandom(2, 5));
         }
      } else {
         Tools.PlayRandomSound(this, this.m_sUnhappySounds);
         if (this.m_bMeshOk) {
            this.m_nCurAnim = this.PlayAnim("publicznosc_zlosc", 1);
         }
      }
   }

   void SetSkin() {
      EAnimals.SetAppearance(this, this.m_nAnimalType, -1);
      Vector vMeshExtents = null;
      switch (this.m_nAnimalType) {
         case 0:
         case 12: // [mod] Panda : modele de l'ours
            vMeshExtents = new Vector(59.5F, 100.0F, 59.5F);
            break;
         case 1:
            vMeshExtents = new Vector(101.0F, 85.0F, 101.0F);
            break;
         case 2:
            vMeshExtents = new Vector(60.0F, 76.0F, 60.0F);
            break;
         case 3:
            vMeshExtents = new Vector(100.0F, 105.0F, 100.0F);
            break;
         case 4:
            vMeshExtents = new Vector(55.0F, 83.6F, 55.0F);
            break;
         case 5:
            vMeshExtents = new Vector(90.0F, 147.0F, 90.0F);
            break;
         case 6:
            vMeshExtents = new Vector(110.0F, 72.5F, 110.0F);
            break;
         case 7:
            vMeshExtents = new Vector(62.0F, 88.0F, 62.0F);
            break;
         case 8:
         case 13: // [mod] Tigre : modele de la panthere
            vMeshExtents = new Vector(60.0F, 74.0F, 60.0F);
            break;
         case 9:
            vMeshExtents = new Vector(48.0F, 84.0F, 48.0F);
            break;
         case 10:
            vMeshExtents = new Vector(51.0F, 77.4F, 51.0F);
            break;
         case 11:
            vMeshExtents = new Vector(51.0F, 77.4F, 51.0F);
      }

      this.SetBlobMapSize(vMeshExtents.fX * 0.6F);
      this.m_fHeight = vMeshExtents.fY;
      this.SetEllipsoid(vMeshExtents.fX / 2.0F, vMeshExtents.fY, vMeshExtents.fY / 2.0F);
      this.m_bMeshOk = true;
   }

   void OnAnimEnd(int nId) {
      if (nId == this.m_nCurAnim) {
         this.CallMethodIn("PlayRandomIdleAnim", SoccerGame.GetRandom(this.fMinTime / 2.0F, this.fMaxTime / 2.0F));
         this.SetAnimMorphTime(this.m_fAnimMorphTime);
         this.PlayAnim("stoi_i_sapie", -1);
      }
   }

   void PlayRandomIdleAnim() {
      this.m_nCurAnim = Tools.PlayRandomAnim(this, this.m_sIdleAnims);
   }

   void InitAnims() {
      this.m_sIdleAnims = new String[]{
         "publicznosc_przeciaganie_sie1", "publicznosc_przeciaganie_sie2", "publicznosc_zlosc", "publicznosc_stoi_nuda", "publicznosc_dawajdawaj"
      };
   }

   void StartSounds() {
      this.DisableCallMethod("PlayRandomSound");
      this.CallMethodIn("PlayRandomSound", SoccerGame.GetRandom(this.fMinTime, this.fMaxTime));
   }

   public void PlayRandomSound() {
      Tools.PlayRandomSound(this, this.m_sRandomSounds);
      this.CallMethodIn("PlayRandomSound", SoccerGame.GetRandom(this.fMinTime, this.fMaxTime));
   }

   void RotateHead(float fAngle) {
      this.BoneRotate((byte)4, vAlongXAxis, fAngle, false);
   }

   void DisableRendering() {
      this.EnableRendering(false);
   }

   int GetRandomSpecies() {
      return GameObject.RandomFloat(1.0F) <= 0.5F ? CoreLogic.m_cTeamDataLeft.m_nAnimalType : CoreLogic.m_cTeamDataRight.m_nAnimalType;
   }

   void LookTowards(ControlObject obj) {
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
      vPos.fY = vPos.fY + this.m_fHeight;
      float fDistance = vPos.Distance(vLookTo);
      float fObjHeight = vLookTo.fY - this.TerrainHeight(vLookTo.fX, vLookTo.fZ);
      float fSin = 0.0F;
      if (fObjHeight > this.m_fHeight) {
         fSin = (fObjHeight - this.m_fHeight) / fDistance;
      } else {
         fSin = (this.m_fHeight - fObjHeight) / fDistance;
      }

      if (fSin > 1.0F) {
         fSin = 1.0F;
      }

      if (fSin < -1.0F) {
         fSin = -1.0F;
      }

      float fVAngle = (float)Math.asin(fSin) * 180.0F / (float) Math.PI;
      if (fObjHeight > this.m_fHeight) {
         fVAngle *= -1.0F;
      }

      if (fVAngle > 10.0F) {
         fVAngle = 10.0F;
      }

      if (fVAngle < -60.0F) {
         fVAngle = -60.0F;
      }

      this.RotateHead(fHAngle);
      if (!bHAngleMax) {
         this.RotateHeadV(0.0F);
         this.RotateHead(fHAngle);
         this.RotateHeadV(fVAngle);
      }
   }

   void OnCreate() {
      SoccerGame.IncWatcher();
      if (!SoccerModule.m_bRenderPublic) {
         this.DeactivateOnModule();
      } else {
         super.OnCreate();
         this.m_nAnimalType = this.GetRandomSpecies();
         super.MeshName = EAnimals.GetMeshName(this.m_nAnimalType);
         this.LoadMesh();
         this.SetSkin();
         this.SetFlag(256);
         this.SetFlag(32);
         this.ClearFlag(4);
         this.ClearFlag(2);
         this.SetAnimType((byte)4, (byte)3, false);
         this.SetAnimMorphTime(0.2F);
         this.SetCollAction(0);
         this.SetCollType(0);
         this.SetTraceCollType(6);
         this.SetCollHandler(1024);
         this.SetPhysicsType(0);
         this.RegisterKeyRequest();
         this.SetMaxPhysicsUpdateInterval(1.0F);
         this.SetUnImportanceThreshold(50.0F);
         this.SetLODCoeff(PerformanceSettings._PUBLIC_LOD[PerformanceSettings.m_iCurrQuality]);
         this.SetMeshMaterialAmbient(0.4F, 0.4F, 0.4F);
         Vector vPos = this.GetPosition();
         vPos.fY = this.TerrainHeight(vPos.fX, vPos.fZ);
         this.SetPosition(vPos);
      }
   }

   void Init() {
      if (!SoccerModule.m_bRenderPublic) {
         this.CallMethodIn("DisableRendering", 0.0F);
      } else {
         this.fMinTime = 10.0F;
         this.fMaxTime = 25.0F;
         this.InitAnims();
         this.m_sRandomSounds = new String[]{"Watcher01.wav", "Watcher02.wav", "Watcher03.wav", "Watcher04.wav"};
         this.m_sHappySounds = new String[]{
            "data/sounds/happy.wav", "data/sounds/happy2.wav", "data/sounds/happy3.wav", "data/sounds/happy4.wav", "data/sounds/happy5.wav"
         };
         this.m_sUnhappySounds = this.m_sHappySounds;
         if (SoccerModule.m_bRenderPublic) {
            this.StartSounds();
            this.StartAnims();
         }

         this.SetDir(new Vector(-1.0F, 0.0F, 0.0F));
      }
   }

   void RotateHeadV(float fAngle) {
      if (fAngle != 0.0F) {
         this.BoneRotate((byte)4, vAlongYAxis, fAngle, true);
      } else {
         this.BoneRotate((byte)4, vAlongYAxis, fAngle, false);
      }
   }

   void StartAnims() {
      this.DisableCallMethod("PlayRandomIdleAnim");
      this.CallMethodIn("PlayRandomIdleAnim", SoccerGame.GetRandom(this.fMinTime / 2.0F, this.fMaxTime / 2.0F));
   }

   void CallOnGoalReaction(int nAnimalScored) {
      this.m_nTeamScored = nAnimalScored;
      this.CallMethodIn("OnGoalReaction", GameObject.RandomFloat(0.5F));
   }
}
