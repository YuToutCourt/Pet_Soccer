public class MenuOptionsMatch extends SoccerUserInterface {
   static final int ID_OK = 500;
   static final int ID_CANCEL = 501;
   UIComboBox m_cHalfTime = null;
   UIComboBox m_cDefCamera = null;
   UIComboBox m_cDiffSetting = null;

   void OnOK() {
      CoreLogic.GetPlayer().m_bFieldMapOn = ((UIButton)this.GetElem("ID_FIELDMAP_CHECK")).GetCheck();
      CoreLogic.GetPlayer().m_bCommentatorOn = ((UIButton)this.GetElem("ID_COMMENTARY_CHECK")).GetCheck();
      CoreLogic.GetPlayer().m_bReplaysOn = ((UIButton)this.GetElem("ID_REPLAYS_CHECK")).GetCheck();
      CoreLogic.GetPlayer().m_bSupersOn = ((UIButton)this.GetElem("ID_SUPERS_CHECK")).GetCheck();
      boolean bRenderPublic = ((UIButton)this.GetElem("ID_AUDIENCE_CHECK")).GetCheck();
      CoreLogic.GetPlayer().m_bAudienceOn = bRenderPublic;
      SoccerModule.m_bRenderPublic = bRenderPublic;
      CoreLogic.GetPlayer().m_nMatchHalfTimeIndex = this.m_cHalfTime.GetSelection();
      CoreLogic.GetPlayer().m_nDefCameraMode = this.m_cDefCamera.GetSelection();
      CoreLogic.GetPlayer().m_nDiffSetting = this.m_cDiffSetting.GetSelection();
      SoccerGame.SaveAttrib();
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(5);
   }

   MenuOptionsMatch() {
      super.m_sFiles = new String[]{"data\\menu\\scr\\OptionsMatch640.scr", "data\\menu\\scr\\OptionsMatch.scr", "data\\menu\\scr\\OptionsMatch1024.scr"};
   }

   void OnUICommand(UIWindow cSender, int nCommand) {
      super.OnUICommand(cSender, nCommand);
      if (nCommand == 1) {
         GameObject.LogLine("-i-- Double Click -----" + cSender.m_sId);
      }

      if (nCommand == 0) {
         int nID = -1;
         if (cSender.m_sId.equals("ID_OK")) {
            nID = 500;
         }

         if (cSender.m_sId.equals("ID_CANCEL")) {
            nID = 501;
         }

         switch (nID) {
            case 500:
               this.OnOK();
               break;
            case 501:
               this.OnCancel();
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

   public void FillTimeCombo() {
      for (int i = 0; i < CoreLogic._HALF_TIMES.length; i++) {
         this.m_cHalfTime.AddItem(CoreLogic._HALF_TIMES[i] + Text.Get("&MenuOptionsMatchTimeSuffix&"), null);
      }
   }

   public void OnInit() {
      super.OnInit();
      this.m_cHalfTime = (UIComboBox)this.GetElem("ID_COMBOBOX_TIME");
      this.FillTimeCombo();
      this.m_cDefCamera = (UIComboBox)this.GetElem("ID_COMBOBOX_CAM");
      this.FillCameraCombo();
      this.m_cDiffSetting = (UIComboBox)this.GetElem("ID_COMBOBOX_DIFF");
      this.FillDiffCombo();
      this.AddToSlideIn(this.GetElem("ID_OK"));
      this.AddToSlideIn(this.GetElem("ID_CANCEL"));
   }

   void OnCancel() {
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(5);
   }

   public void FillCameraCombo() {
      this.m_cDefCamera.AddItem(Text.Get("&MenuOptionsMatchCam0&"), null);
      this.m_cDefCamera.AddItem(Text.Get("&MenuOptionsMatchCam1&"), null);
      this.m_cDefCamera.AddItem(Text.Get("&MenuOptionsMatchCam2&"), null);
      this.m_cDefCamera.AddItem(Text.Get("&MenuOptionsMatchCam3&"), null);
   }

   public void Show() {
      ((UIButton)this.GetElem("ID_FIELDMAP_CHECK")).SetCheck(CoreLogic.GetPlayer().m_bFieldMapOn);
      ((UIButton)this.GetElem("ID_COMMENTARY_CHECK")).SetCheck(CoreLogic.GetPlayer().m_bCommentatorOn);
      ((UIButton)this.GetElem("ID_REPLAYS_CHECK")).SetCheck(CoreLogic.GetPlayer().m_bReplaysOn);
      ((UIButton)this.GetElem("ID_SUPERS_CHECK")).SetCheck(CoreLogic.GetPlayer().m_bSupersOn);
      ((UIButton)this.GetElem("ID_AUDIENCE_CHECK")).SetCheck(CoreLogic.GetPlayer().m_bAudienceOn);
      this.m_cHalfTime.SetSelection(CoreLogic.GetPlayer().m_nMatchHalfTimeIndex);
      this.m_cDefCamera.SetSelection(CoreLogic.GetPlayer().m_nDefCameraMode);
      this.m_cDiffSetting.SetSelection(CoreLogic.GetPlayer().m_nDiffSetting);
      super.Show();
   }

   public void OnCreate() {
      super.OnCreate();
   }

   public void FillDiffCombo() {
      this.m_cDiffSetting.AddItem(Text.Get("&MenuOptionsMatchDiff0&"), null);
      this.m_cDiffSetting.AddItem(Text.Get("&MenuOptionsMatchDiff1&"), null);
      this.m_cDiffSetting.AddItem(Text.Get("&MenuOptionsMatchDiff2&"), null);
      // [mod] niveaux 8..10 (CoreLogic.GetDiffLevel)
      this.m_cDiffSetting.AddItem(Text.Get("&MenuOptionsMatchDiff3&"), null);
      this.m_cDiffSetting.AddItem(Text.Get("&MenuOptionsMatchDiff4&"), null);
      this.m_cDiffSetting.AddItem(Text.Get("&MenuOptionsMatchDiff5&"), null);
      // [mod] Classé : la difficulté suit l'Elo
      this.m_cDiffSetting.AddItem(Text.Get("&MenuOptionsMatchDiff6&"), null);
   }
}
