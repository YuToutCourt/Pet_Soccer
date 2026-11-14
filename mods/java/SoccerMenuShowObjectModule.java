public class SoccerMenuShowObjectModule extends SoccerMenuModule {
   MenuAnimal m_cObject = null;
   MenuAnimal[] tAnimals = new MenuAnimal[EAnimals._ANIMALS_COUNT]; // [mod] 12 -> toutes les equipes
   boolean m_bUnloaded = false;
   int m_nCurFB = -1;
   MeshObject cLightMesh = null;
   boolean m_bActive = false;
   float fScale = 10.0F;
   int nMovedObject = 0;
   static final int _DEST_POS = 0;
   static final int _CAM_POS = 1;
   static final int _OBJECT_POS = 2;
   static final int _CAM_OBS_POS = 3;

   void MoveObject(int nKeyCode) {
      Vector vTemp = null;
      switch (this.nMovedObject) {
         case 0:
            Tools.MovePointByKeys(this.m_cObject.m_vDestinationPos, nKeyCode, 0.0F);
            this.m_cObject.SetPosition(this.m_cObject.m_vDestinationPos);
            break;
         case 1:
            Tools.MovePointByKeys(super.m_Camera.GetPosition(), nKeyCode, 0.0F);
            super.m_Camera.SetObservationPos(vTemp);
            break;
         case 2:
            Tools.MoveObjectByKeys(this.m_cObject, nKeyCode, 0.0F);
            Tools.ShowInfoTop("fbpos " + this.m_cObject.GetPosition() + " d " + Tools.g_fDelta, this);
            break;
         case 3:
            Tools.MoveObjectByKeys(this.cLightMesh, nKeyCode, 0.0F);
      }
   }

   void ReloadAnimalMesh(int nAnimal) {
      if (this.tAnimals[nAnimal] != null) {
         TeamData cData = ETeams.GetTeamData(nAnimal);
         EAnimals.SetAppearanceForMenu(this.tAnimals[nAnimal], nAnimal, cData.IsAvailable());
      }
   }

   public void OnDeactivate() {
      super.OnDeactivate();
      if (this.m_nCurFB != -1 && !this.m_bUnloaded && this.tAnimals[this.m_nCurFB] != null) {
         this.tAnimals[this.m_nCurFB].DeactivateOnModule();
      }

      this.m_bActive = false;
   }

   public void Reload() {
      if (this.m_bUnloaded) {
         this.m_bUnloaded = false;
      }
   }

   void ResetScene() {
      if (this.m_cObject != null) {
         this.m_cObject.Reset();
         this.m_cObject = null;
         this.m_nCurFB = -1;
         super.m_Camera.m_vCameraObserverPoint = new Vector();
         super.m_Camera.SetObservationPos(new Vector(590.0F, 500.0F, 690.0F));
         super.m_Camera.SetObserved(new Vector(340.0F, 470.0F, 500.0F));
      }
   }

   void InitAnimals() {
      for (int i = 0; i < this.tAnimals.length; i++) { // [mod]
         this.m_cObject = (MenuAnimal)this.CreateObject("MenuAnimal");
         TeamData cData = ETeams.GetTeamData(i);
         if (cData != null) {
            EAnimals.SetAppearanceForMenu(this.m_cObject, i, cData.IsAvailable());
         }

         this.tAnimals[i] = this.m_cObject;
         this.AddControlObject(this.m_cObject);
         this.m_cObject.DeactivateOnModule();
      }
   }

   public void Deactivate() {
      if (this.IsActive()) {
         this.m_bActive = false;
         super.Deactivate();
      }
   }

   public void OnActivate() {
      super.OnActivate();
      this.SetFootballer(this.m_nCurFB);
      this.m_bActive = true;
   }

   void SetFootballer(int nAnimalType) {
      if (nAnimalType == -1) {
         this.ResetScene();
      } else {
         boolean bLoaded = false;
         if (this.tAnimals[nAnimalType] == null) {
            this.InitAnimal(nAnimalType);
            if (this.m_nCurFB == -1) {
               this.ResetScene();
            }

            bLoaded = true;
         }

         if (nAnimalType == this.m_nCurFB) {
            this.tAnimals[this.m_nCurFB].ActivateOnModule();
         } else {
            if (this.m_nCurFB != -1) {
               this.tAnimals[this.m_nCurFB].DeactivateOnModule();
               this.ResetScene();
            }

            this.m_nCurFB = nAnimalType;
            this.m_cObject = this.tAnimals[this.m_nCurFB];
            if (bLoaded) {
               this.CallMethodIn("ContinueSetFootballer", 0.1F);
            } else {
               this.ContinueSetFootballer();
            }
         }
      }
   }

   void ContinueSetFootballer() {
      this.m_cObject.ActivateOnModule();
      this.m_cObject.Reset();
      this.m_cObject.SetHappiness(false);
      this.m_cObject.GotoPoint(this.m_cObject.m_vDestinationPos);
   }

   void SetFootballer(int nAnimalType, int nAnimalClothes) {
      if (this.tAnimals[nAnimalType] == null) {
         this.InitAnimal(nAnimalType);
         if (this.m_nCurFB == -1) {
            this.ResetScene();
         }
      }

      this.SetFootballer(nAnimalType);
      int nMaterial = EAnimals.GetSkinMaterial(this.m_cObject);
      String sTexName = EAnimals.m_sTextureNormal[nAnimalType][nAnimalClothes];
      if (ETeams.GetTeamData(nAnimalType).IsAvailable()) {
         this.m_cObject.SetSkinElement(sTexName, nMaterial);
      }
   }

   void SetFootballerCup(int nAnimalType, int nAnimalClothes) {
      if (this.tAnimals[nAnimalType] == null) {
         this.InitAnimal(nAnimalType);
         if (this.m_nCurFB == -1) {
            this.ResetScene();
         }
      }

      this.SetFootballer(nAnimalType);
      this.m_cObject.Reset();
      this.m_cObject.SetHappiness(true);
      this.m_cObject.GotoPoint(this.m_cObject.m_vDestinationPosCup);
      int nMaterial = EAnimals.GetSkinMaterial(this.m_cObject);
      String sTexName = EAnimals.m_sTextureNormal[nAnimalType][nAnimalClothes];
      this.m_cObject.SetSkinElement(sTexName, nMaterial);
   }

   public void Activate() {
      if (!this.IsActive()) {
         this.m_bActive = true;
         super.Activate();
      }
   }

   void SetFootballerInstant(int nAnimalType, int nAnimalClothes) {
      this.SetFootballer(nAnimalType, nAnimalClothes);
      this.m_cObject.GotoPointInstant(this.m_cObject.m_vDestinationPos);
   }

   void ActivateMenuCamera() {
      GameObject.LogLine("Activate menu camera");
      this.DeactivateMenuCamera();
      super.m_View = this.AddView(super.m_Camera, 0.3F, 0.0F, 0.7F, 1.0F);
      this.ActivateView(super.m_View);
   }

   void InitFonts() {
   }

   public void OnCreate() {
      super.OnCreate();
      this.RegisterKeyRequest();
   }

   public void Unload() {
      if (!this.m_bUnloaded) {
         for (int i = 0; i < this.tAnimals.length; i++) {
            if (this.tAnimals[i] != null) {
               this.tAnimals[i].DeactivateOnModule();
               this.tAnimals[i].delete();
               this.tAnimals[i] = null;
            }
         }

         this.DeleteUnusedMeshes();
         this.m_bUnloaded = true;
      }
   }

   void InitAnimal(int nAnimal) {
      this.m_cObject = (MenuAnimal)this.CreateObject("MenuAnimal");
      TeamData cData = ETeams.GetTeamData(nAnimal);
      if (cData != null) {
         EAnimals.SetAppearanceForMenu(this.m_cObject, nAnimal, cData.IsAvailable());
      }

      this.tAnimals[nAnimal] = this.m_cObject;
      this.AddControlObject(this.m_cObject);
      this.m_cObject.DeactivateOnModule();
   }

   void OnInputKey(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      new Vector();
      if (SoccerGame.m_bDebug) {
         if (!bKeyState) {
            switch (nKeyCode) {
               case 0:
            }
         }
      }
   }

   public void InitModule() {
      super.InitModule();
      this.ResetScene();
   }

   boolean IsActive() {
      return this.m_bActive;
   }
}
