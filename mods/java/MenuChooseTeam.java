public class MenuChooseTeam extends SoccerUserInterface {
   static final String CHOOSE_TEAM_BACK = "ID_BACK";
   static final String CHOOSE_TEAM_OK = "ID_OK";
   static final String CHOOSE_TEAM_PREV = "ID_PREV";
   static final String CHOOSE_TEAM_NEXT = "ID_NEXT";
   static final int CHOOSE_TEAM_BACK_ID = 100;
   static final int CHOOSE_TEAM_OK_ID = 101;
   static final int CHOOSE_TEAM_NEXT_ID = 102;
   static final int CHOOSE_TEAM_PREV_ID = 103;
   static final int MENU_EXTRA_ID = 4;
   static final int MENU_LOADLEVEL_ID = 2;
   static final int MENU_LOADLEVEL_CHOOSE_TEAM_ID = 3;
   TeamData[] tTeams = new TeamData[EAnimals._ANIMALS_COUNT]; // [mod] 12 -> toutes les equipes
   static SoccerMenuShowObjectModule m_cModuleShowFB = null;
   int m_nChosen = 0;
   int m_nChosenPlayer1 = 0;
   int m_nPlayer = 0;
   float m_fCommentDelay = 2.0F;
   UIStatic cTeamName = null;
   UIStatic cTeamInfo = null;
   UIWindow cTeamLogo = null;
   UIProgressSkills cPrgStrength = null;
   UIProgressSkills cPrgAccuracy = null;
   UIProgressSkills cPrgTurning = null;
   UIStatic m_cToWinText = null;

   void OnOK() {
      this.DisableCallMethod("CommentOnTeam");
      if (this.m_nPlayer == 0) {
         this.m_nChosenPlayer1 = this.m_nChosen;
      }

      int nLevel = CoreLogic.m_nNextLevel;
      // [mod] le switch d'origine (un cas par equipe, stade = index) remplace par GetHomeField
      int nAnimal = this.tTeams[this.m_nChosen].m_nAnimalType;
      if (this.m_nPlayer == 0) {
         CoreLogic.m_cTeamDataLeft = ETeams.GetTeamData(nAnimal);
         CoreLogic.m_nNextLevel = ETeams.GetHomeField(nAnimal);
      } else {
         CoreLogic.m_cTeamDataRight = ETeams.GetTeamData(nAnimal);
      }

      if (CoreLogic.IsExtraLevel(nLevel)) {
         CoreLogic.m_nNextLevel = nLevel;
      }

      MenuChooseTeamColors cMenu = (MenuChooseTeamColors)((SoccerMainMenuModule)this.GetModule()).GetUI(3);
      if (this.m_nPlayer == 0) {
         cMenu.SetInfo(CoreLogic.m_cTeamDataLeft, this.m_nPlayer);
      } else {
         cMenu.SetInfo(CoreLogic.m_cTeamDataRight, this.m_nPlayer);
      }

      ((SoccerMainMenuModule)this.GetModule()).ShowUI(3);
   }

   void OnBack() {
      Commentator comm = this.GetCommentator();
      comm.StopTalking();
      this.DisableCallMethod("CommentOnTeam");
      if (ModLobby.IsMenu()) {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_ONLINE); // [mod] en ligne : retour au salon
      } else if (this.m_nPlayer != 0) {
         this.m_nPlayer = 0;
         MenuBackground.SetPlayerType(0);
         this.m_nChosen = this.m_nChosenPlayer1;
         MenuChooseTeamColors cMenu = (MenuChooseTeamColors)((SoccerMainMenuModule)this.GetModule()).GetUI(3);
         cMenu.SetInfo(CoreLogic.m_cTeamDataLeft, 0);
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(3);
      } else if (CoreLogic.IsExtraLevel(CoreLogic.m_nNextLevel)) {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(15);
      } else {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(0);
      }
   }

   void CommentOnTeam() {
      int nChosenAnimal = this.tTeams[this.m_nChosen].m_nAnimalType;
      Commentator comm = this.GetCommentator();
      comm.StopTalking();
      comm.CommentWhileChoosingTeam(nChosenAnimal);
   }

   MenuChooseTeam() {
      super.m_sFiles = new String[]{"data\\menu\\scr\\ChooseTeam640.scr", "data\\menu\\scr\\ChooseTeam.scr", "data\\menu\\scr\\ChooseTeam1024.scr"};
   }

   void OnUICommand(UIWindow cSender, int nCommand) {
      super.OnUICommand(cSender, nCommand);
      if (nCommand == 1) {
         GameObject.LogLine("-i-- Double Click -----" + cSender.m_sId);
      }

      if (nCommand == 0) {
         int nID = -1;
         if (cSender.m_sId.equals("ID_BACK")) {
            nID = 100;
         }

         if (cSender.m_sId.equals("ID_OK")) {
            nID = 101;
         }

         if (cSender.m_sId.equals("ID_PREV")) {
            nID = 103;
         }

         if (cSender.m_sId.equals("ID_NEXT")) {
            nID = 102;
         }

         if (this.GetModule() != null) {
            switch (nID) {
               case 100:
                  this.OnBack();
                  break;
               case 101:
                  this.OnOK();
                  break;
               case 102:
                  this.OnNext();
                  break;
               case 103:
                  this.OnPrev();
                  break;
               default:
                  ((SoccerMenuModule)this.GetModule()).HandleUIClick(nID);
            }
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

   void Info(String sText) {
      this.DisplayText(sText);
   }

   void RefreshAvailableTeams() {
      for (int i = 0; i < this.tTeams.length; i++) {
         m_cModuleShowFB.ReloadAnimalMesh(this.tTeams[i].m_nAnimalType);
      }
   }

   void Reset() {
      this.m_nPlayer = 0;
      this.m_nChosen = 0;
      this.m_nChosenPlayer1 = 0;
      m_cModuleShowFB.ResetScene();
   }

   public void OnInit() {
      super.OnInit();
      if (m_cModuleShowFB != null) {
         m_cModuleShowFB.InitModule();
      }

      UIStatic info = (UIStatic)this.GetElem("ID_INFO_FRAME");
      if (info != null) {
         info.SetTextPosFlags(ESpriteText.HORIZ_MIDDLE | ESpriteText.VERT_TOP);
      }

      this.cTeamName = (UIStatic)this.GetElem("ID_TEAM_NAME");
      this.cTeamInfo = (UIStatic)this.GetElem("ID_TEAM_INFO");
      this.cTeamLogo = this.GetElem("ID_LOGO_TEAM");

      for (int i = 0; i < this.tTeams.length; i++) {
         if (this.tTeams[i] != null) {
            this.tTeams[i].m_cLogo.m_cWindow = this.cTeamLogo;
            this.tTeams[i].m_cLogo.m_nId = this.cTeamLogo.AddTexture(this.tTeams[i].m_cLogo.m_sFileName);
         }
      }

      this.cPrgStrength = (UIProgressSkills)this.GetElem("ID_PROGRESS_STRENGTH");
      this.cPrgAccuracy = (UIProgressSkills)this.GetElem("ID_PROGRESS_ACCURACY");
      this.cPrgTurning = (UIProgressSkills)this.GetElem("ID_PROGRESS_TURNING");
      this.m_cToWinText = (UIStatic)this.GetElem("ID_MENU_CHOOSE");
   }

   public void Hide() {
      super.Hide();
      if (m_cModuleShowFB != null) {
         m_cModuleShowFB.Deactivate();
      }
   }

   void NextPlayer() {
      this.m_nChosen = 0;
      if (this.m_nChosen == this.m_nChosenPlayer1) {
         this.OnNext();
      }

      if (this.m_nPlayer != 0) {
         Tools.CrashLogWarning("MenuChooseTeam: niedozwolona proba zmiany gracza na nastepnego");
      } else {
         if (CoreLogic.m_bMultiplayer) {
            this.m_nPlayer = 1;
         } else {
            this.m_nPlayer = 2;
         }

         MenuBackground.SetPlayerType(this.m_nPlayer);
      }
   }

   void ShowInfo(int nTeam) {
      Commentator comm = this.GetCommentator();
      comm.StopTalking();
      this.DisableCallMethod("CommentOnTeam");
      if (this.tTeams[nTeam].IsAvailable()) {
         this.CallMethodIn("CommentOnTeam", this.m_fCommentDelay);
      }

      this.cTeamName.SetText(this.tTeams[nTeam].m_sName.toLowerCase());
      if (this.cTeamInfo.GetSize().Len() == 0.0F) {
         this.cTeamInfo.SetFont(this.cTeamInfo.m_sFontId);
         this.cTeamInfo.OnSizeAndPos();
      }

      String sText = null;
      sText = this.tTeams[nTeam].m_sInfo;
      this.cTeamInfo.SetText(sText);
      this.cPrgStrength.SetValue(5 - (100 - this.tTeams[nTeam].m_nSkillStrength) / 5);
      this.cPrgAccuracy.SetValue(5 - (100 - this.tTeams[nTeam].m_nSkillAccuracy) / 5);
      this.cPrgTurning.SetValue(5 - (100 - this.tTeams[nTeam].m_nSkillTurning) / 5);
      if (m_cModuleShowFB != null) {
         m_cModuleShowFB.SetFootballer(this.tTeams[nTeam].m_nAnimalType, this.tTeams[nTeam].m_nAnimalClothes);
      }

      this.cTeamLogo.SetTexture(this.tTeams[nTeam].m_cLogo);
      if (this.tTeams[nTeam].IsAvailable() && (this.m_nPlayer == 0 || nTeam != this.m_nChosenPlayer1)) {
         ((UIButton)this.GetElem("ID_OK")).SetDisabled(false);
      } else {
         ((UIButton)this.GetElem("ID_OK")).SetDisabled(true);
      }

      if (!this.tTeams[nTeam].IsAvailable()) {
         this.m_cToWinText.SetText("&&MenuChooseTeamToWin&&");
      } else {
         this.m_cToWinText.SetText("");
      }
   }

   public void Show() {
      this.DisableCallMethod("CommentOnTeam");
      this.RefreshAvailableTeams();
      this.GetCommentator().StopTalking();
      if (this.m_nPlayer == 0) {
         this.AddToSlideIn((UIButton)this.GetElem("ID_BACK"));
         this.AddToSlideIn((UIButton)this.GetElem("ID_OK"));
      } else {
         this.RemoveFromSlideIn((UIButton)this.GetElem("ID_BACK"));
         this.RemoveFromSlideIn((UIButton)this.GetElem("ID_OK"));
      }

      this.ShowInfo(this.m_nChosen);
      if (m_cModuleShowFB != null) {
         m_cModuleShowFB.Activate();
      }

      super.m_sTitle = Text.Get("&MenuTitleChooseTeam&");
      super.Show();
   }

   public void OnCreate() {
      super.OnCreate();
      this.tTeams[0] = ETeams.GetTeamData(Championships.m_nAvailableTeams[0]);
      this.tTeams[1] = ETeams.GetTeamData(Championships.m_nAvailableTeams[1]);
      this.tTeams[2] = ETeams.GetTeamData(Championships.m_nAvailableTeams[2]);
      this.tTeams[3] = ETeams.GetTeamData(Championships.m_nAvailableTeams[3]);
      this.tTeams[4] = ETeams.GetTeamData(Championships.m_nAvailableTeams[4]);
      this.tTeams[5] = ETeams.GetTeamData(Championships.m_nAvailableTeams[5]);
      this.tTeams[6] = ETeams.GetTeamData(Championships.m_nFinalOpponents[0]);
      this.tTeams[7] = ETeams.GetTeamData(Championships.m_nFinalOpponents[1]);
      this.tTeams[8] = ETeams.GetTeamData(Championships.m_nFinalOpponents[2]);
      this.tTeams[9] = ETeams.GetTeamData(Championships.m_nFinalOpponents[3]);
      this.tTeams[10] = ETeams.GetTeamData(Championships.m_nFinalOpponents[4]);
      this.tTeams[11] = ETeams.GetTeamData(Championships.m_nFinalOpponents[5]);
      // [mod] nouvelles equipes, en fin de carrousel
      this.tTeams[12] = ETeams.GetTeamData(EAnimals._ANIMAL_PANDA);
      this.tTeams[13] = ETeams.GetTeamData(EAnimals._ANIMAL_TIGER);
      if (!SoccerGame.m_bMenuEditor) {
         m_cModuleShowFB = (SoccerMenuShowObjectModule)this.CreateObject("SoccerMenuShowObjectModule");
      }
   }

   void OnPrev() {
      this.m_nChosen += -1;
      if (this.m_nChosen < 0) {
         this.m_nChosen = this.tTeams.length - 1;
      }

      if (this.tTeams[this.m_nChosen] == null) {
         this.OnPrev();
      } else {
         this.ShowInfo(this.m_nChosen);
      }
   }

   TeamData[] GetTeams() {
      return this.tTeams;
   }

   void OnNext() {
      this.m_nChosen++;
      this.m_nChosen = this.m_nChosen % this.tTeams.length;
      if (this.tTeams[this.m_nChosen] == null) {
         this.OnNext();
      } else {
         this.ShowInfo(this.m_nChosen);
      }
   }
}
