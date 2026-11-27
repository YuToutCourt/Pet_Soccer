// [mod] copie du jeu d'origine : saisie fiabilisée et traces en mode debug (voir UIEdit.java)
public class UIEditSelKey extends UIEdit {
   int m_nKey = -1;
   int m_nJoy = -1;
   int m_nJoyButton = -1;
   boolean m_bCanWork = true;

   int GetKey() {
      return this.m_nKey;
   }

   void SetCheck(boolean bCheck) {
      if (super.m_bChecked != bCheck) {
         this.Trace("SetCheck " + bCheck + ", peut " + this.m_bCanWork);
      }

      if (super.m_bActive && this.m_bCanWork) {
         if (!super.m_bChecked && bCheck) {
            super.m_bChecked = true;
            super.m_sOldText = new String(super.m_sEditText);
            super.m_sEditText = "";
            this.OnTypingStart(); // [mod]
            this.CaptureMouse();
         }

         if (super.m_bChecked && !bCheck) {
            super.m_bChecked = false;
            this.ClearKeys();
            this.OnTypingEnd(); // [mod]
            this.ClearCaptureMouse();
         }

         this.UpdateText();
         this.OnTextureChange();
      }
   }

   void OnKeyPressed(int nKey, char nAsci) {
      boolean bNew = this.MarkKey(nKey, true); // [mod] une touche reçue deux fois (directe et par SoccerGame)
      if (super.m_bFocus || super.m_bChecked) {
         this.Trace("touche " + nKey + ", peut " + this.m_bCanWork + (super.m_bGlobalKey ? " (jeu)" : " (direct)") + (bNew ? "" : " deja vue"));
      }

      if (!bNew) {
         return;
      }

      if (super.m_bActive && super.m_bChecked && this.m_bCanWork) {
         char ch = ' ';
         String sChar = "";
         switch (nKey) {
            case 1:
               if (super.m_bChecked) {
                  super.m_sEditText = KbSettings.GetKeyString(this.m_nKey);
                  this.OnKeyEscape();
               }
               break;
            default:
               if (KbSettings.GetKeyString(nKey).length() > 0) {
                  super.m_sEditText = KbSettings.GetKeyString(nKey);
               }

               this.m_nKey = nKey;
         }

         this.UpdateText();
         this.OnKeyEnter();
      }
   }

   public void OnInputJoyButton(int nJoy, int nButton, boolean bState) {
      if (super.m_bActive && super.m_bChecked && this.m_bCanWork) {
         if (nJoy == this.m_nJoy) {
            this.m_nJoyButton = nButton;
            this.m_nKey = 1000 * (nJoy + 1) + nButton;
            super.m_sEditText = KbSettings.GetKeyString(this.m_nKey);
         }

         this.UpdateText();
         this.OnKeyEnter();
      }
   }

   public void OnInit() {
      super.OnInit();
      super.m_bMouseBtnOutAsEnter = false;
   }

   void SetKey(int nKey) {
      this.m_nKey = nKey;
      super.m_sEditText = KbSettings.GetKeyString(nKey);
      this.UpdateText();
   }
}
