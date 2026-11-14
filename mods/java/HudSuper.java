public class HudSuper extends UIWindow {
   static final int _LETTERS_COUNT = 5;
   // [mod] icônes des supers 4..7 (ModSupers) : P_Ikonki2.png, même grille que p_ikonki.png
   static final String _MOD_ICONS = "data/menu/P_Ikonki2.png";
   static final float[][] _MOD_ICON_RECTS = new float[][]{{1.0F, 1.0F, 126.0F, 50.0F}, {1.0F, 53.0F, 126.0F, 102.0F}, {1.0F, 105.0F, 126.0F, 154.0F}, {1.0F, 157.0F, 127.0F, 207.0F}};
   int m_nLettersLit = 0;
   UIButton[] tLetters = new UIButton[5];
   UIButton cIcon = null;
   UITexture[] m_tModIcons = null;
   float m_fFlashShow = 0.15F;
   float m_fFlashHide = 0.1F;

   void ShowSuperIcon(int nType) {
      UITexture cTex = null;
      switch (nType) {
         case 0:
            cTex = this.cIcon.m_cNormalTexture;
            break;
         case 1:
            cTex = this.cIcon.m_cDisabledTex;
            break;
         case 2:
            cTex = this.cIcon.m_cCheckedTex;
            break;
         case 3:
            cTex = this.cIcon.m_cFocusTexture;
            break;
         default:
            cTex = this.GetModIcon(nType);
      }

      if (cTex == null) {
         return;
      }

      this.cIcon.SetTexture(cTex);
      this.cIcon.Show();
   }

   void ShowSuperIcon() {
      this.cIcon.Show();
   }

   // [mod] texture de l'icône d'un super ModSupers, chargée au premier affichage
   UITexture GetModIcon(int nType) {
      int i = nType - ModSupers._FREEZE;
      if (i < 0 || i >= _MOD_ICON_RECTS.length) {
         return null;
      }

      if (this.m_tModIcons == null) {
         this.m_tModIcons = new UITexture[_MOD_ICON_RECTS.length];
      }

      if (this.m_tModIcons[i] == null) {
         float[] r = _MOD_ICON_RECTS[i];
         UITexture cTex = new UITexture(_MOD_ICONS, 128.0F, 256.0F, r[0], r[1], r[2], r[3]);
         cTex.m_cWindow = this.cIcon;
         cTex.m_nId = this.cIcon.AddTexture(_MOD_ICONS);
         this.m_tModIcons[i] = cTex;
      }

      return this.m_tModIcons[i];
   }

   void FlashSuper() {
      this.ShowSuperIcon();
      this.DisableCallMethod("HideSuperIcon");
      this.CallMethodIn("HideSuperIcon", this.m_fFlashShow);
      this.DisableCallMethod("FlashSuper");
      this.CallMethodIn("FlashSuper", this.m_fFlashShow + this.m_fFlashHide);
   }

   void HideSuperIcon() {
      this.cIcon.Hide();
   }

   void EndFlashSuper() {
      this.DisableCallMethod("HideSuperIcon");
      this.DisableCallMethod("FlashSuper");
      this.HideSuperIcon();
   }

   void LightLetters(int nNumber, int nSuperType, boolean bGreen) {
      this.m_nLettersLit = nNumber;
      if (nNumber <= 5) {
         for (int i = 0; i < 5; i++) {
            if (i < nNumber) {
               if (bGreen) {
                  this.tLetters[i].SetTexture(this.tLetters[i].m_cCheckedTex);
               } else {
                  this.tLetters[i].SetTexture(this.tLetters[i].m_cFocusTexture);
               }
            } else {
               this.tLetters[i].SetTexture(this.tLetters[i].m_cNormalTexture);
            }
         }

         if (nNumber == 5) {
            this.ShowSuperIcon(nSuperType);
         }
      }
   }

   void Init() {
      this.tLetters[0] = (UIButton)this.GetElem("ID_SUPER_S");
      this.tLetters[1] = (UIButton)this.GetElem("ID_SUPER_U");
      this.tLetters[2] = (UIButton)this.GetElem("ID_SUPER_P");
      this.tLetters[3] = (UIButton)this.GetElem("ID_SUPER_E");
      this.tLetters[4] = (UIButton)this.GetElem("ID_SUPER_R");

      for (int i = 0; i < 5; i++) {
         if (this.tLetters[i] == null) {
            GameObject.CrashLog("[Error] brak litery w SUPER\n");
         }
      }

      this.cIcon = (UIButton)this.GetElem("ID_SUPER_ICON");
      if (this.cIcon == null) {
         GameObject.CrashLog("[Error] brak ID_SUPER_ICON\n");
      }

      this.cIcon.Hide();
   }

   public void delete() {
      for (int i = 0; i < this.tLetters.length; i++) {
         this.tLetters[i] = null;
      }

      this.cIcon = null;
      this.m_tModIcons = null;
      super.delete();
   }
}
