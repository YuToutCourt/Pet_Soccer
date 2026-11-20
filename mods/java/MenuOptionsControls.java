// [mod] + ligne « Joueurs 2-7 » : 6 touches directes par joueur (ModSwitch, enregistrées dans mods.dat),
// cases ID_EDIT_KEY_SEL2..7 ajoutées par mods/ui/make_controls_scr.py
public class MenuOptionsControls extends SoccerUserInterface {
   static final int ID_OK = 600;
   static final int ID_CANCEL = 601;
   static final int ID_BUTTON = 602;
   UIComboBox m_cController1 = null;
   UIComboBox m_cController2 = null;
   UIWindow m_cKeys1 = null;
   UIWindow m_cKeys2 = null;
   UIStatic m_cFrame1 = null;
   UIStatic m_cFrame2 = null;
   UIStatic m_cOk = null;
   UIStatic m_cCancel = null;
   UIEditSelKey[] m_tKeys1 = new UIEditSelKey[8];
   UIEditSelKey[] m_tKeys2 = new UIEditSelKey[8];
   UIEditSelKey[] m_tSel1 = new UIEditSelKey[ModSwitch._NUMBERS]; // [mod]
   UIEditSelKey[] m_tSel2 = new UIEditSelKey[ModSwitch._NUMBERS]; // [mod]

   void OnButton(UIWindow cSender) {
      UIComboBox cParentCombo = (UIComboBox)cSender.m_wndParent.m_wndParent.m_wndParent;
      UIComboBox cSecondCombo = null;
      int nNewSelection = cParentCombo.GetSelection();
      UIEditSelKey[] tKeys = null;
      UIEditSelKey[] tKeysSecond = null;
      if (cParentCombo == this.m_cController1) {
         cSecondCombo = this.m_cController2;
         tKeys = this.m_tKeys1;
         tKeysSecond = this.m_tKeys2;
      }

      if (cParentCombo == this.m_cController2) {
         cSecondCombo = this.m_cController1;
         tKeys = this.m_tKeys2;
         tKeysSecond = this.m_tKeys1;
      }

      if (nNewSelection > 0) {
         if (cSecondCombo.GetSelection() == nNewSelection) {
            cSecondCombo.SetSelection(0);
            this.SetKeyboard(cSecondCombo, tKeysSecond);
         }

         this.SetJoyStick(cParentCombo, tKeys, nNewSelection - 1);
      } else {
         this.SetKeyboard(cParentCombo, tKeys);
      }
   }

   void OnOK() {
      for (int i = 0; i < this.m_tKeys1.length; i++) {
         if (this.m_tKeys1[i] != null) {
            this.GetKb0().m_Keys[i] = this.m_tKeys1[i].GetKey();
         }

         if (this.m_tKeys2[i] != null) {
            this.GetKb1().m_Keys[i] = this.m_tKeys2[i].GetKey();
         }
      }

      int nSelection1 = this.m_cController1.GetSelection();
      if (nSelection1 == 0) {
         this.GetKb0().m_Device = 0;
      } else {
         this.GetKb0().m_Device = 1;
         this.GetKb0().m_JoyNumber = nSelection1 - 1;
      }

      int nSelection2 = this.m_cController2.GetSelection();
      if (nSelection2 == 0) {
         this.GetKb1().m_Device = 0;
      } else {
         this.GetKb1().m_Device = 1;
         this.GetKb1().m_JoyNumber = nSelection2 - 1;
      }

      // [mod] touches directes vers les joueurs 2..7
      for (int i = 0; i < ModSwitch._NUMBERS; i++) {
         if (this.m_tSel1[i] != null) {
            ModSwitch.m_tKeys[0][i] = this.m_tSel1[i].GetKey();
         }

         if (this.m_tSel2[i] != null) {
            ModSwitch.m_tKeys[1][i] = this.m_tSel2[i].GetKey();
         }
      }

      ModOptions.Save();
      SoccerGame.SaveAttrib();
      if (CoreLogic.m_bMultiplayer) {
         SoccerGame.game.RunModule();
      } else {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(5);
      }
   }

   void ResetKeys(int nSet) {
      UIEditSelKey[] tKeys = null;
      KbSettings cKbSet = null;
      if (nSet == 0) {
         tKeys = this.m_tKeys1;
         cKbSet = this.GetKb0();
      } else {
         tKeys = this.m_tKeys2;
         cKbSet = this.GetKb1();
      }

      for (int i = 0; i < tKeys.length; i++) {
         if (tKeys[i] != null) {
            tKeys[i].SetKey(cKbSet.m_Keys[i]);
            tKeys[i].m_nJoy = nSet;
         }
      }

      // [mod]
      UIEditSelKey[] tSel = nSet == 0 ? this.m_tSel1 : this.m_tSel2;

      for (int i = 0; i < tSel.length; i++) {
         if (tSel[i] != null) {
            tSel[i].SetKey(ModSwitch.m_tKeys[nSet][i]);
            tSel[i].m_nJoy = nSet;
         }
      }
   }

   MenuOptionsControls() {
      super.m_sFiles = new String[]{
         "data\\menu\\scr\\OptionsControls640.scr", "data\\menu\\scr\\OptionsControls.scr", "data\\menu\\scr\\OptionsControls1024.scr"
      };
   }

   void OnUICommand(UIWindow cSender, int nCommand) {
      super.OnUICommand(cSender, nCommand);
      if (nCommand == 1) {
         GameObject.LogLine("-i-- Double Click -----" + cSender.m_sId);
      }

      if (nCommand == 3) {
         this.OnKeyChanged((UIEditSelKey)cSender);
      }

      if (nCommand == 0) {
         int nID = -1;
         if (cSender.m_sId.equals("ID_OK")) {
            if (!CoreLogic.m_bMultiplayer) {
               nID = 600;
            } else {
               nID = 601;
            }
         }

         if (cSender.m_sId.equals("ID_CANCEL")) {
            if (!CoreLogic.m_bMultiplayer) {
               nID = 601;
            } else {
               nID = 600;
            }
         }

         if (cSender.m_sId.equals("ID_BUTTON")) {
            nID = 602;
         }

         switch (nID) {
            case 600:
               this.OnOK();
               break;
            case 601:
               this.OnCancel();
               break;
            case 602:
               this.OnButton(cSender);
               break;
            default:
               ((SoccerMenuModule)this.GetModule()).HandleUIClick(nID);
         }

         GameObject.LogLine("-i--  Click -----" + cSender.m_sId);
      }

      if (nCommand == 5) {
         GameObject.LogLine("-i--  Sel -----" + cSender.m_sId + " z menu " + ((UIMenuButton)cSender).GetMenu().m_sId);
      }

      if (nCommand == 6) {
         GameObject.LogLine("-i--  Sel_dbClick -----" + cSender.m_sId + " z menu " + ((UIMenuButton)cSender).GetMenu().m_sId);
      }
   }

   int FindKeyDuplicate(UIEditSelKey[] tKeys, UIEditSelKey cKey) {
      if (cKey.GetKey() < 0) { // [mod] une case vidée n'en vide pas d'autres
         return -1;
      }

      for (int i = 0; i < tKeys.length; i++) {
         if (tKeys[i] != null && cKey != tKeys[i] && tKeys[i].GetKey() == cKey.GetKey()) {
            return i;
         }
      }

      return -1;
   }

   void ClearAndSetActive(UIEditSelKey cEdit, boolean bActive) {
      if (!bActive) {
         cEdit.SetKey(-1);
         cEdit.m_bCanWork = false;
      } else {
         cEdit.m_bCanWork = true;
      }
   }

   KbSettings GetKb0() {
      return SoccerGame.GetKb0();
   }

   void SetJoyStick(UIComboBox cCombo, UIEditSelKey[] tKeys, int nJoy) {
      this.ClearAndSetActive(tKeys[0], false);
      this.ClearAndSetActive(tKeys[1], false);
      this.ClearAndSetActive(tKeys[2], false);
      this.ClearAndSetActive(tKeys[3], false);

      for (int i = 0; i < tKeys.length; i++) {
         tKeys[i].m_nJoy = nJoy;
      }

      // [mod]
      UIEditSelKey[] tSel = this.GetSel(tKeys);

      for (int i = 0; i < tSel.length; i++) {
         if (tSel[i] != null) {
            tSel[i].m_nJoy = nJoy;
         }
      }
   }

   // [mod] touches directes du même joueur que tKeys
   UIEditSelKey[] GetSel(UIEditSelKey[] tKeys) {
      return tKeys == this.m_tKeys2 ? this.m_tSel2 : this.m_tSel1;
   }

   public void OnInit() {
      super.OnInit();
      this.m_cController1 = (UIComboBox)this.GetElem("ID_COMBOBOX_CONTROLLER1");
      this.FillCombo(this.m_cController1);
      this.m_cController2 = (UIComboBox)this.GetElem("ID_COMBOBOX_CONTROLLER2");
      this.FillCombo(this.m_cController2);
      this.m_cKeys1 = this.GetElem("ID_KEYS1");
      this.m_cKeys2 = this.GetElem("ID_KEYS2");
      this.InitKeyControls(0);
      this.InitKeyControls(1);
      this.m_cFrame1 = (UIStatic)this.GetElem("ID_FRAME_PLAYER1");
      this.m_cFrame2 = (UIStatic)this.GetElem("ID_FRAME_PLAYER2");
      this.m_cCancel = (UIStatic)this.GetElem("ID_CANCEL");
      this.m_cOk = (UIStatic)this.GetElem("ID_OK");
      this.AddToSlideIn(this.GetElem("ID_OK"));
      this.AddToSlideIn(this.GetElem("ID_CANCEL"));
   }

   boolean AllAssigned() {
      for (int i = 0; i < this.m_tKeys1.length; i++) {
         if (this.m_tKeys1[i] != null && this.m_tKeys1[i].GetKey() == -1) {
            return false;
         }

         if (this.m_tKeys2[i] != null && this.m_tKeys2[i].GetKey() == -1) {
            return false;
         }
      }

      return true;
   }

   void OnCancel() {
      if (CoreLogic.m_bMultiplayer) {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(9);
      } else {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(5);
      }
   }

   void InitKeyControls(int nSet) {
      UIWindow cKeys = null;
      UIEditSelKey[] tKeys = null;
      if (nSet == 0) {
         cKeys = this.m_cKeys1;
         tKeys = this.m_tKeys1;
      } else {
         cKeys = this.m_cKeys2;
         tKeys = this.m_tKeys2;
      }

      tKeys[0] = (UIEditSelKey)cKeys.GetElem("ID_EDIT_KEY_UP");
      tKeys[1] = (UIEditSelKey)cKeys.GetElem("ID_EDIT_KEY_DOWN");
      tKeys[2] = (UIEditSelKey)cKeys.GetElem("ID_EDIT_KEY_LEFT");
      tKeys[3] = (UIEditSelKey)cKeys.GetElem("ID_EDIT_KEY_RIGHT");
      tKeys[5] = (UIEditSelKey)cKeys.GetElem("ID_EDIT_KEY_SHOT");
      tKeys[4] = (UIEditSelKey)cKeys.GetElem("ID_EDIT_KEY_PASS");
      tKeys[7] = (UIEditSelKey)cKeys.GetElem("ID_EDIT_KEY_SUPER");
      tKeys[6] = (UIEditSelKey)cKeys.GetElem("ID_EDIT_KEY_HEAD");
      // [mod]
      UIEditSelKey[] tSel = nSet == 0 ? this.m_tSel1 : this.m_tSel2;

      for (int i = 0; i < tSel.length; i++) {
         UIWindow cWnd = cKeys.GetElem("ID_EDIT_KEY_SEL" + (i + ModSwitch._FIRST_NUMBER));
         tSel[i] = cWnd instanceof UIEditSelKey ? (UIEditSelKey)cWnd : null;
      }
   }

   KbSettings GetKb1() {
      return SoccerGame.GetKb1();
   }

   public void FillCombo(UIComboBox cComboBox) {
      cComboBox.AddItem(KbSettings.GetDeviceName(0), null);
   }

   void SetKeyboard(UIComboBox cCombo, UIEditSelKey[] tKeys) {
      this.ClearAndSetActive(tKeys[0], true);
      this.ClearAndSetActive(tKeys[1], true);
      this.ClearAndSetActive(tKeys[2], true);
      this.ClearAndSetActive(tKeys[3], true);
      if (tKeys[6].GetKey() >= 1000) {
         tKeys[6].SetKey(-1);
      }

      if (tKeys[7].GetKey() >= 1000) {
         tKeys[7].SetKey(-1);
      }

      if (tKeys[4].GetKey() >= 1000) {
         tKeys[4].SetKey(-1);
      }

      if (tKeys[5].GetKey() >= 1000) {
         tKeys[5].SetKey(-1);
      }

      for (int i = 0; i < tKeys.length; i++) {
         tKeys[i].m_nJoy = -1;
      }

      // [mod] boutons de manette retirés, comme pour passe, tir, tête et spécial
      UIEditSelKey[] tSel = this.GetSel(tKeys);

      for (int i = 0; i < tSel.length; i++) {
         if (tSel[i] != null) {
            if (tSel[i].GetKey() >= 1000) {
               tSel[i].SetKey(-1);
            }

            tSel[i].m_nJoy = -1;
         }
      }
   }

   public void Show() {
      if (CoreLogic.m_bMultiplayer) {
         this.m_cFrame1.SetText(CoreLogic.m_cTeamDataLeft.m_sName.toLowerCase());
         this.m_cFrame2.SetText(CoreLogic.m_cTeamDataRight.m_sName.toLowerCase());
         this.m_cCancel.SetText(Text.Get("&MenuPreMatchPlay&"));
         this.m_cOk.SetText(Text.Get("&MenuBack&"));
      } else {
         this.m_cFrame1.SetText(Text.Get("&MenuOptionsControlsFrame1&"));
         this.m_cFrame2.SetText(Text.Get("&MenuOptionsControlsFrame2&"));
         this.m_cCancel.SetText(Text.Get("&MenuCancel&"));
         this.m_cOk.SetText(Text.Get("&MenuOK&"));
      }

      this.ResetKeys(0);
      this.ResetKeys(1);
      if (this.m_cController1.GetItemCount() == 1) {
         if (SoccerGame.m_bDebug) {
            GameObject.CrashLog("Liczba joyow " + this.GetJoysticksCount());
         }

         String sJoy = Text.Get("&MenuControlsJoystick&");

         for (int i = 0; i < this.GetJoysticksCount(); i++) {
            this.m_cController1.AddItem(sJoy + i, null);
            this.m_cController2.AddItem(sJoy + i, null);
         }
      }

      if (this.GetKb0().m_Device == 0) {
         this.m_cController1.SetSelection(0);
      } else {
         this.m_cController1.SetSelection(this.GetKb0().m_JoyNumber + 1);
         this.SetJoyStick(this.m_cController1, this.m_tKeys1, this.GetKb0().m_JoyNumber);
      }

      if (this.GetKb1().m_Device == 0) {
         this.m_cController2.SetSelection(0);
      } else {
         this.m_cController2.SetSelection(this.GetKb1().m_JoyNumber + 1);
         this.SetJoyStick(this.m_cController2, this.m_tKeys2, this.GetKb1().m_JoyNumber);
      }

      super.Show();
   }

   public void OnCreate() {
      super.OnCreate();
   }

   void OnKeyChanged(UIEditSelKey cKey) {
      int nFound = this.FindKeyDuplicate(this.m_tKeys1, cKey);
      if (nFound != -1 && this.m_tKeys1[nFound] != cKey) {
         this.m_tKeys1[nFound].SetKey(-1);
      }

      nFound = this.FindKeyDuplicate(this.m_tKeys2, cKey);
      if (nFound != -1 && this.m_tKeys2[nFound] != cKey) {
         this.m_tKeys2[nFound].SetKey(-1);
      }

      // [mod] une même touche ne sert qu'une fois, touches directes comprises
      nFound = this.FindKeyDuplicate(this.m_tSel1, cKey);
      if (nFound != -1 && this.m_tSel1[nFound] != cKey) {
         this.m_tSel1[nFound].SetKey(-1);
      }

      nFound = this.FindKeyDuplicate(this.m_tSel2, cKey);
      if (nFound != -1 && this.m_tSel2[nFound] != cKey) {
         this.m_tSel2[nFound].SetKey(-1);
      }
   }
}
