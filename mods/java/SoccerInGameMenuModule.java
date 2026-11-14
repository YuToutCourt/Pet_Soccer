public class SoccerInGameMenuModule extends SoccerMenuModule {
   MenuInGame m_cMenuInGame = null;
   MenuYesNoDialog m_cMenuYesNo = null;
   MenuTactics m_cMenuTactics = null; // [mod] écran Tactique, ouvert depuis le menu pause

   void OnDestroy() {
      this.m_cMenuInGame.Remove();
      this.m_cMenuInGame.delete();
      this.m_cMenuInGame = null;
      this.m_cMenuTactics.Remove();
      this.m_cMenuTactics.delete();
      this.m_cMenuTactics = null;
      this.m_cMenuYesNo.Remove();
      this.m_cMenuYesNo.delete();
      this.m_cMenuYesNo = null;
   }

   MenuYesNoDialog GetYesNoDlg() {
      return this.m_cMenuYesNo;
   }

   void OnDeactivate() {
      this.UnregisterKeyRequest();
      this.m_cMenuInGame.Hide();
      this.m_cMenuTactics.Hide(); // [mod]
   }

   // [mod] menu pause -> écran Tactique (retour par MenuTactics.Close)
   void ShowTactics() {
      this.m_cMenuInGame.Hide();
      this.m_cMenuTactics.Show();
   }

   void OnActivate() {
      this.RegisterKeyRequest();
      this.m_cMenuInGame.Show();
   }

   void Destroy() {
   }

   void OnInputKeyDebug(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      if (!bKeyState) {
         switch (nKeyCode) {
            case 0:
         }
      }
   }

   public void OnCreate() {
      int nResolution = SoccerGame.GetResolution();
      this.m_cMenuYesNo = (MenuYesNoDialog)this.CreateObject("MenuYesNoDialog");
      this.m_cMenuYesNo.Load(this, nResolution);
      this.m_cMenuYesNo.Hide();
      this.m_cMenuInGame = (MenuInGame)this.CreateObject("MenuInGame");
      this.m_cMenuInGame.Load(this, nResolution);
      this.m_cMenuInGame.Hide();
      this.m_cMenuTactics = (MenuTactics)this.CreateObject("MenuTactics"); // [mod]
      this.m_cMenuTactics.Load(this, nResolution);
      this.m_cMenuTactics.Hide();
   }

   void OnInputKey(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      if (SoccerGame.m_bDebug) {
         this.OnInputKeyDebug(nKeyCode, bKeyState, nKeyAscii);
      }

      if (!bKeyState) {
         switch (nKeyCode) {
            case 0:
         }
      }
   }

   void OnChangeResolution() {
   }
}
