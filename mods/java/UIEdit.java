// [mod] copie du jeu d'origine, pour le bug « on ne peut pas écrire dans un champ » (2026-11-25, notes/ui-input.md) :
// - le moteur ne donne pas toujours les touches au champ en saisie (code du salon en ligne) : SoccerGame.OnInputKey,
//   qui reçoit toutes les touches, les lui passe aussi (OnGlobalKey) ; une touche reçue deux fois n'est comptée
//   qu'une fois (MarkKey : enfoncée, puis relâchée) ;
// - le moteur ne donne pas toujours le clic aux cases imbriquées (Options > Commandes, toutes sauf « Haut ») :
//   SoccerGame.OnInputMouseButton (bouton gauche relâché) met en saisie le champ survolé (OnGlobalClick) ;
// - Alt resté enfoncé après un Alt+Tab bloquait toutes les lettres : touches de contrôle remises à zéro au début
//   de chaque saisie ;
// - traces en mode debug (Debug(1)).
// La classe interne Idle n'est pas recopiée : celle du jeu (UIEdit$Idle.class) reste dans code.pak, la version
// décompilée ne recompile pas.
public class UIEdit extends UIButton {
   static UIEdit m_cTyping = null; // [mod] champ en saisie
   boolean[] m_tKeyDown = new boolean[256]; // [mod] touches enfoncées vues par ce champ (une seule fois par appui)
   boolean m_bGlobalKey = false; // [mod] touche passée par SoccerGame
   static UIEdit m_cHover = null; // [mod] champ sous la souris
   boolean m_bLCtrl = false;
   boolean m_bRCtrl = false;
   boolean m_bLAlt = false;
   boolean m_bRAlt = false;
   boolean m_bLShift = false;
   boolean m_bRShift = false;
   float m_fMargin = 5.0F;
   String m_sEditText = new String();
   String m_sOldText = "NO_DEFINED";
   String m_sCursorSign = "_";
   String m_sForbiddenChars = "";
   Sprite m_cCursorSprite = null;
   float m_fCursorBlinkPeriod = 0.5F;
   boolean m_bMouseBtnOutAsEnter = true;

   void OnKeyEscape() {
      if (super.m_bActive) {
         this.Command(this, 4);
         this.m_sEditText = this.m_sOldText;
         this.UpdateText();
         this.SetFocus(false);
         this.SetCheck(false);
      }
   }

   void OnKeyEnter() {
      if (super.m_bActive) {
         this.Command(this, 3);
         this.SetFocus(false);
         this.SetCheck(false);
      }
   }

   // [mod] trace en mode debug
   void Trace(String sWhat) {
      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("[mod] edit " + super.m_sId + " : " + sWhat + " (actif " + super.m_bActive + ", focus " + super.m_bFocus
            + ", saisie " + super.m_bChecked + ", visible " + super.m_bVisible + ")\n");
      }
   }

   void OnMouseLeave(float fMouseX, float fMouseY) {
      if (super.m_bFocus || super.m_bChecked) {
         this.Trace("souris sortie");
      }

      if (m_cHover == this) {
         m_cHover = null;
      }

      super.OnMouseLeave(fMouseX, fMouseY);
   }

   // [mod] champ sous la souris (le survol arrive au champ même quand le clic n'y arrive pas)
   void OnMouseMove(float fMouseX, float fMouseY, int iButtonsFlags) {
      if (m_cHover != this) {
         m_cHover = this;
      }

      super.OnMouseMove(fMouseX, fMouseY, iButtonsFlags);
   }

   // [mod] vrai si le champ et tous ses parents sont affichés
   boolean IsShownOnScreen() {
      for (UIWindow cWnd = this; cWnd != null; cWnd = cWnd.m_wndParent) {
         if (!cWnd.m_bVisible) {
            return false;
         }
      }

      return true;
   }

   // [mod] SoccerGame.OnInputMouseButton, bouton gauche relâché : clic sur le champ survolé que le moteur ne lui a
   // pas transmis, même effet que UIEdit$Idle.OnLBUp. Si le moteur le transmet aussi, SetCheck(true) sur un champ
   // déjà en saisie ne fait rien.
   static void OnGlobalClick() {
      UIEdit cEdit = m_cHover;
      if (cEdit == null) {
         return;
      }

      if (!cEdit.m_bActive || cEdit.m_bDisabled || !cEdit.IsShownOnScreen() || !cEdit.IsCursorInUIWindow()) {
         cEdit.Trace("clic du jeu ignore (actif " + cEdit.m_bActive + ", desactive " + cEdit.m_bDisabled + ", affiche "
            + cEdit.IsShownOnScreen() + ", souris dessus " + cEdit.IsCursorInUIWindow() + ")");
         return;
      }

      if (!cEdit.m_bChecked) {
         cEdit.Trace("clic vu par le jeu");
         if (!cEdit.m_bFocus) {
            cEdit.SetFocus(true);
         }

         cEdit.SetCheck(true);
      }
   }

   // [mod] SoccerGame.OnInputKey : toute touche du jeu, pour le champ en saisie
   static void OnGlobalKey(int nKey, boolean bState, char nAscii) {
      UIEdit cEdit = m_cTyping;
      if (cEdit == null || !cEdit.m_bChecked || !cEdit.IsShownOnScreen()) {
         return;
      }

      cEdit.m_bGlobalKey = true;

      try {
         if (bState) {
            cEdit.OnKeyPressed(nKey, nAscii);
         } else {
            cEdit.OnKeyReleased(nKey, nAscii);
         }
      } finally {
         cEdit.m_bGlobalKey = false;
      }
   }

   // [mod] faux si cet appui (ou ce relâchement) a déjà été vu par l'autre chemin
   boolean MarkKey(int nKey, boolean bDown) {
      if (nKey < 0 || nKey >= this.m_tKeyDown.length) {
         return true;
      }

      if (this.m_tKeyDown[nKey] == bDown) {
         return false;
      }

      this.m_tKeyDown[nKey] = bDown;
      return true;
   }

   // [mod] début et fin de saisie (SetCheck, ici et dans UIEditSelKey)
   void OnTypingStart() {
      this.ClearKeys();

      for (int i = 0; i < this.m_tKeyDown.length; i++) {
         this.m_tKeyDown[i] = false;
      }

      m_cTyping = this;
   }

   void OnTypingEnd() {
      if (m_cTyping == this) {
         m_cTyping = null;
      }
   }

   void SetFocus(boolean bActive) {
      if (super.m_bFocus != bActive) {
         this.Trace("SetFocus " + bActive);
      }

      if (super.m_bActive) {
         if (!super.m_bFocus && bActive) {
            this.ProcessKb(true);
         }

         if (super.m_bFocus && !bActive) {
            this.ProcessKb(false);
            this.SetCheck(false);
         }

         super.SetFocus(bActive);
      }
   }

   void SetCheck(boolean bCheck) {
      if (super.m_bChecked != bCheck) {
         this.Trace("SetCheck " + bCheck);
      }

      if (super.m_bActive) {
         if (!super.m_bChecked && bCheck) {
            super.m_bChecked = true;
            this.m_sOldText = new String(this.m_sEditText);
            this.OnTypingStart();
            this.CaptureMouse();
         }

         if (super.m_bChecked && !bCheck) {
            super.m_bChecked = false;
            this.ClearKeys();
            this.OnTypingEnd();
            this.ClearCaptureMouse();
         }

         this.OnTextureChange();
         this.UpdateText();
      }
   }

   void ClearKeys() {
      this.m_bLCtrl = false;
      this.m_bRCtrl = false;
      this.m_bLAlt = false;
      this.m_bRAlt = false;
      this.m_bLShift = false;
      this.m_bRShift = false;
   }

   void OnKeyPressed(int nKey, char nAsci) {
      boolean bNew = this.MarkKey(nKey, true);
      if (super.m_bFocus || super.m_bChecked) {
         this.Trace("touche " + nKey + " ascii " + (int)nAsci + (this.m_bGlobalKey ? " (jeu)" : " (direct)") + (bNew ? "" : " deja vue"));
      }

      if (!bNew) {
         return;
      }

      if (super.m_bActive && super.m_bChecked) {
         char ch = ' ';
         String sChar = "";
         switch (nKey) {
            case 1:
               if (super.m_bChecked) {
                  this.OnKeyEscape();
               }
               break;
            case 14:
               this.OnKeyBackspace();
               break;
            case 28:
               if (super.m_bChecked) {
                  this.OnKeyEnter();
               }
               break;
            case 29:
               this.m_bLCtrl = true;
               break;
            case 42:
               this.m_bLShift = true;
               break;
            case 54:
               this.m_bRShift = true;
               break;
            case 56:
               this.m_bLAlt = true;
               break;
            case 157:
               this.m_bRCtrl = true;
               break;
            case 184:
               this.m_bRAlt = true;
               break;
            default:
               if (super.m_bChecked) {
                  if (nAsci == 0) {
                     return;
                  }

                  ch = nAsci;
                  sChar = sChar + ch;
                  if (!this.m_bLAlt && !this.m_bRCtrl && !this.m_bLCtrl && !this.m_bRAlt) {
                     if (!this.m_bLShift && !this.m_bRShift) {
                        sChar = sChar.toLowerCase();
                     } else {
                        sChar = sChar.toUpperCase();
                     }

                     if (this.m_sForbiddenChars.indexOf(sChar) != -1) {
                        return;
                     }

                     if (this.CheckExtent(this.m_sEditText + ch + this.m_sCursorSign)) {
                        this.m_sEditText = this.m_sEditText + sChar;
                     }
                  }
               }
         }

         this.UpdateText();
      }
   }

   void InitCursorSprite() {
      if (this.m_cCursorSprite == null) {
         this.m_cCursorSprite = (Sprite)this.CreateObject("Sprite");
         if (super.m_cFramedWindowTextSprite != null) {
            super.m_cFramedWindowTextSprite.AddChild(this.m_cCursorSprite);
         } else {
            this.AddChild(this.m_cCursorSprite);
         }

         this.m_cCursorSprite.SetSize(new Vector(20.0F, 20.0F, 0.0F));
         this.m_cCursorSprite.Hide();
      }
   }

   boolean Load(ParseKeyword key) {
      for (ParseKeyword paramKey = key.GetChild(); paramKey != null; paramKey = paramKey.GetNext()) {
         if (paramKey.Equals("TextureEdit")) {
            super.m_cCheckedTex = new UITexture(this);
            super.m_cCheckedTex.m_sFileName = paramKey.GetSParam(0);
            super.m_cCheckedTex.m_rRect = null;
            this.LoadTexture(super.m_cCheckedTex);
         }

         if (paramKey.Equals("TextEditColor")) {
            float r = paramKey.GetFParam(0);
            float g = paramKey.GetFParam(1);
            float b = paramKey.GetFParam(2);
            super.m_vCheckedTxtColor = new Vector(r, g, b);
         }
      }

      super.Load(key);
      return true;
   }

   void OnKeyReleased(int nKey, char nAsci) {
      this.MarkKey(nKey, false); // [mod] relâchement : rien à craindre d'un doublon (il ne fait que relâcher Ctrl, Alt, Maj)
      if (super.m_bActive && super.m_bChecked) {
         switch (nKey) {
            case 29:
               this.m_bLCtrl = false;
               break;
            case 42:
               this.m_bLShift = false;
               break;
            case 54:
               this.m_bRShift = false;
               break;
            case 56:
               this.m_bLAlt = false;
               break;
            case 157:
               this.m_bRCtrl = false;
               break;
            case 184:
               this.m_bRAlt = false;
         }
      }
   }

   public void SetTextPosFlags(int nFlags) {
      super.SetTextPosFlags(nFlags);
      if (this.m_cCursorSprite == null) {
         this.InitCursorSprite();
      }

      this.m_cCursorSprite.SetTextPosFlags(nFlags);
   }

   public void OnInit() {
      super.OnInit();
      this.InitCursorSprite();
   }

   void ShowTextCursor(boolean bShow) {
      this.DisableCallMethod("BlinkCursor");
      if (bShow) {
         this.m_cCursorSprite.Show();
         this.CallMethodEvery("BlinkCursor", this.m_fCursorBlinkPeriod);
      } else {
         this.m_cCursorSprite.Hide();
      }
   }

   void BlinkCursor() {
      if (this.m_cCursorSprite.IsVisible()) {
         this.m_cCursorSprite.Hide();
      } else {
         this.m_cCursorSprite.Show();
      }
   }

   void UpdateTextCursorPos() {
      if (super.m_bInited) {
         Vector vCursorPos = new Vector();
         if (this.m_sEditText.length() != 0) {
            vCursorPos = this.GetTextExtent(this.m_sEditText);
            vCursorPos.fY = 0.0F;
            vCursorPos.Add(this.GetTextPos());
         } else {
            Vector vLetterSize = this.GetTextExtent("N");
            if ((super.m_nTextPosFlags & ESpriteText.HORIZ_LEFT) == ESpriteText.HORIZ_LEFT) {
               vCursorPos.fX = 0.0F;
            }

            if ((super.m_nTextPosFlags & ESpriteText.HORIZ_MIDDLE) == ESpriteText.HORIZ_MIDDLE) {
               vCursorPos.fX = this.GetSize().fX / 2.0F;
            }

            if ((super.m_nTextPosFlags & ESpriteText.HORIZ_RIGHT) == ESpriteText.HORIZ_RIGHT) {
               vCursorPos.fX = this.GetSize().fX;
            }

            if ((super.m_nTextPosFlags & ESpriteText.VERT_TOP) == ESpriteText.VERT_TOP) {
               vCursorPos.fY = 0.0F;
            }

            if ((super.m_nTextPosFlags & ESpriteText.VERT_MIDDLE) == ESpriteText.VERT_MIDDLE) {
               vCursorPos.fY = this.GetSize().fY / 2.0F - vLetterSize.fY / 2.0F;
            }

            if ((super.m_nTextPosFlags & ESpriteText.VERT_BOTTOM) == ESpriteText.VERT_BOTTOM) {
               vCursorPos.fY = this.GetSize().fY - vLetterSize.fY;
            }
         }

         this.m_cCursorSprite.SetRelativePos(vCursorPos);
      }
   }

   void OnKeyBackspace() {
      if (super.m_bActive) {
         int nLen = this.m_sEditText.length();
         if (nLen > 0) {
            String sNewText = this.m_sEditText.substring(0, nLen - 1);
            this.m_sEditText = sNewText;
            this.UpdateText();
         }
      }
   }

   void OnCreate() {
      if (super.m_sText == null) {
         super.m_sText = "";
      }

      this.m_sOldText = super.m_sText;
      this.m_sEditText = super.m_sText;
      super.m_bMouseLeaveDestroyFocus = false;
      super.OnCreate();
   }

   public boolean SetFont(String sFont) {
      boolean bRet = super.SetFont(sFont);
      if (this.m_cCursorSprite == null) {
         this.InitCursorSprite();
      }

      this.m_cCursorSprite.SetFont(sFont);
      this.m_cCursorSprite.DisplayTextFitSize(this.m_sCursorSign);
      return bRet;
   }

   boolean CheckExtent(String sText) {
      String sTmpTest = new String(sText);
      Vector vText = this.GetTextExtent(sTmpTest);
      return !(vText.fX + 2.0F * this.m_fMargin >= this.GetSize().fX);
   }

   void OnLBUpOut(float fMouseX, float fMouseY) {
      if (super.m_bActive) {
         if (super.m_bChecked) {
            if (this.m_bMouseBtnOutAsEnter) {
               this.OnKeyEnter();
            } else {
               this.OnKeyEscape();
            }
         }
      }
   }

   void UpdateText() {
      if (super.m_bActive) {
         super.SetText(this.m_sEditText);
         if (super.m_bChecked) {
            this.ShowTextCursor(true);
            this.UpdateTextCursorPos();
         } else {
            this.ShowTextCursor(false);
         }
      }
   }

   public void SetText(String sNewText) {
      this.m_sOldText = new String(this.m_sEditText);
      this.m_sEditText = new String(sNewText);
      super.SetText(this.m_sEditText);
   }
}
