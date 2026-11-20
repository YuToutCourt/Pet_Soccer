public class AIReferee extends AIFootBaller {
   int m_nCoinAnim = -1;

   void SetStabilization() {
      this.UpdateValidPos();
      switch (AIMatch.GetMatchState()) {
         case 1:
            super.m_vStabilizationPos = new Vector(super.cSpawnPointPresentation.GetPosition());
         case 2:
         case 6:
         default:
            break;
         case 3:
            super.m_vStabilizationPos = new Vector(super.cSpawnPointStart.GetPosition());
            break;
         case 4:
         case 5:
         case 7:
            super.m_vStabilizationPos = new Vector(super.m_vValidPos);
      }

      super.SetStabilization();
   }

   public void Update() {
      if (super.m_bInited && super.m_bLiving) {
         if (SoccerModule.m_cMatch.IsStabilizationNew()) {
            this.CheckStabilizationProcess();
         } else if (!SoccerModule.m_cMatch.m_bWaiting) {
            if (AIMatch.IsMatchState(0)) {
               this.UpdateValidPos();
               super.Update();
            }
         }
      }
   }

   void ShowCoin(boolean bShow) {
      int nCoinId = this.GetElementID("Moneta_0");
      if (nCoinId == -1) {
         Tools.CrashLogError(this + " nCoinId = -1");
      }

      if (bShow) {
         this.UnhideElement(nCoinId);
      } else {
         this.HideElement(nCoinId);
      }
   }

   public void OnAnimEnd(int iAnimID) {
      super.OnAnimEnd(iAnimID);
      if (iAnimID == this.m_nCoinAnim) {
         this.ShowCoin(false);
      }

      this.m_nCoinAnim = -1;
   }

   void FixValidPos() {
      Vector vCenter = new Vector(0.0F, 0.0F, super.cSpawnPoint.GetPosition().fZ + 300.0F);
      Vector vPos = new Vector(super.cSpawnPoint.GetPosition());
      super.m_vValidPosOff = new Vector(vPos);
      super.m_vValidPosOff.Sub(vCenter);
      super.m_vValidPos = new Vector(super.cSpawnPoint.GetPosition());
      super.m_bInited = true;
   }

   void CheckStabilizationProcess() {
      if (this.IsStabilization() && (!this.HasGoal(20) || this.HasGoal(20) && !this.GetGoal().IsChanceToReach(this))) {
         super.m_cAISystem.RequestEvent(this, 16, null);
      }
   }

   void OnInputKey(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      if (SoccerGame.m_bDebug) {
         super.OnInputKey(nKeyCode, bKeyState, nKeyAscii);
         if (!bKeyState && !ModSwitch.IsSelectKey(nKeyCode)) { // [mod] touches directes : pas de raccourci debug
            switch (nKeyCode) {
               case 2:
                  this.SetPosition(super.cSpawnPointPresentation.GetPosition());
                  break;
               case 3:
                  this.SetPosition(super.cSpawnPointStart.GetPosition());
                  break;
               case 4:
                  this.SetPosition(super.cSpawnPoint.GetPosition());
            }
         }
      }
   }

   public int PlayAnim(String sAnim, int nRep) {
      if (sAnim.equalsIgnoreCase("sedzia_rzut_moneta")) {
         this.m_nCoinAnim = this.PlayAnim("moneta_0", sAnim, nRep);
         this.ShowCoin(true);
      }

      return super.PlayAnim(sAnim, nRep);
   }

   public void UpdateParams() {
      super.UpdateParams();
   }

   void UpdateValidPos() {
      Vector vValidPos = null;
      vValidPos = new Vector(GameField.GetDestPos());
      vValidPos.Add(super.m_vValidPosOff);
      vValidPos = super.m_cStrategy.TuneValidScope(this, vValidPos);
      float fBallDist = SoccerModule.m_cMatch.m_fZofOut - vValidPos.fZ;
      if (AIMatch.IsMatchState(5) && Math.abs(fBallDist) < 400.0F) {
         if (fBallDist < 0.0F) {
            vValidPos.fZ += 400.0F;
         } else {
            vValidPos.fZ -= 400.0F;
         }
      }

      this.SetValidPos(vValidPos);
   }
}
