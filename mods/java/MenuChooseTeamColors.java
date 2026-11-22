public class MenuChooseTeamColors extends SoccerUserInterface {
   static final String CHOOSE_COLOR_BACK = "ID_BACK_COLORS";
   static final String CHOOSE_COLOR_OK = "ID_OK_COLORS";
   static final int CHOOSE_COLOR_BACK_ID = 700;
   static final int CHOOSE_COLOR_OK_ID = 701;
   static final int ID_COLOR = 702;
   SoccerMenuShowObjectModule m_cModuleShowFB = null;
   String m_sTeamName = "unknown";
   int m_nAnimal = 1;
   int m_nChosen = 0;
   int m_nPlayer = 0;
   int m_nClothes = 0;
   TeamData m_cTeamData = null;
   UIStatic cTeamName = null;
   UIListBox cColors = null;
   boolean m_bFirstShow = true;

   void OnOK() {
      this.m_cTeamData.SetClothesId(this.m_nClothes);
      if (ModLobby.IsMenu() && ModOnlineLeague.m_bPickTeam) {
         // [mod] ligue en ligne : l'équipe choisie part au serveur (avant le départ de la ligue)
         ModOnlineLeague.OnTeamChosen(this.m_nAnimal, this.m_nClothes);
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_ONLINE_LEAGUE);
      } else if (ModLobby.IsMenu()) {
         // [mod] en ligne : chacun ne choisit que son équipe, puis retourne au salon
         ModLobby.SetMyTeam(this.m_nAnimal, this.m_nClothes);
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_ONLINE);
      } else if (CoreLogic.m_bChampionships) {
         CoreLogic.GetPlayer().ResetChampionship(this.m_nAnimal, this.m_nClothes);
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(11);
      } else if (CoreLogic.m_bLeague) {
         // [mod] mode Ligue : nouvelle ligue, puis classement
         ModLeagueMode.Start(this.m_nAnimal, this.m_nClothes);
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_LEAGUE);
      } else if (CoreLogic.m_bSurvival) {
         // [mod] survie : l'adversaire est tiré au sort, on passe directement à l'écran Survie
         ModSurvival.StartRun(this.m_nAnimal, this.m_nClothes);
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_SURVIVAL);
      } else if (this.m_nPlayer != 1 && this.m_nPlayer != 2) {
         MenuChooseTeam cMenu = (MenuChooseTeam)((SoccerMainMenuModule)this.GetModule()).GetUI(2);
         cMenu.NextPlayer();
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(2);
      } else {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(9);
      }
   }

   void OnBack() {
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(2);
   }

   MenuChooseTeamColors() {
      super.m_sFiles = new String[]{
         "data\\menu\\scr\\ChooseTeamColors640.scr", "data\\menu\\scr\\ChooseTeamColors.scr", "data\\menu\\scr\\ChooseTeamColors1024.scr"
      };
   }

   void OnUICommand(UIWindow cSender, int nCommand) {
      super.OnUICommand(cSender, nCommand);
      if (nCommand == 1) {
         GameObject.LogLine("-i-- Double Click -----" + cSender.m_sId);
      }

      if (nCommand == 0) {
         int nID = -1;
         if (cSender.m_sId.equals("ID_BUTTON")) {
            nID = 702;
         }

         if (cSender.m_sId.equals("ID_BACK_COLORS")) {
            nID = 700;
         }

         if (cSender.m_sId.equals("ID_OK_COLORS")) {
            nID = 701;
         }

         if (this.GetModule() != null) {
            switch (nID) {
               case 700:
                  this.OnBack();
                  break;
               case 701:
                  this.OnOK();
                  break;
               case 702:
                  this.OnColor((UIButton)cSender);
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

   void Comment() {
      if (this.m_nPlayer == 0 || CoreLogic.m_bMultiplayer) {
         this.GetCommentator().SayRandom(1);
      }
   }

   public void OnInit() {
      super.OnInit();
      this.cTeamName = (UIStatic)this.GetElem("ID_TEAM_NAME");
      this.cColors = (UIListBox)this.GetElem("ID_LIST_COLORS");
      this.m_cModuleShowFB = MenuChooseTeam.m_cModuleShowFB;

      for (int i = 0; i < EAnimals.GetClothesCount(this.m_nAnimal); i++) {
         this.cColors.AddItem(" ", null);
      }
   }

   void SetColor(int nClothes) {
      this.m_nClothes = nClothes;
      this.m_cTeamData.SetClothesId(this.m_nClothes);
      MeshObject cAnimal = this.m_cModuleShowFB.m_cObject;
      cAnimal.SetSkinElement(EAnimals.GetClothNormal(this.m_nAnimal, nClothes), EAnimals.GetSkinMaterial(cAnimal));
   }

   public void Hide() {
      super.Hide();
      if (this.m_cModuleShowFB != null) {
         this.m_cModuleShowFB.Deactivate();
      }
   }

   void OnColor(UIButton btnSender) {
      this.m_nClothes = this.cColors.GetSelection();
      this.SetColor(this.m_nClothes);
   }

   void SetInfo(TeamData cData, int nPlayer) {
      this.m_cTeamData = cData;
      this.m_sTeamName = cData.m_sName;
      this.m_nAnimal = cData.m_nAnimalType;
      this.m_nClothes = cData.m_nAnimalClothes;
      this.m_nPlayer = nPlayer;
      this.m_cModuleShowFB.SetFootballerInstant(this.m_nAnimal, this.m_nClothes);
   }

   void ShowInfo() {
      this.cTeamName.SetText(this.m_sTeamName.toLowerCase());
      if (this.cColors.GetItemCount() == 0) {
         for (int i = 0; i < EAnimals.GetClothesCount(this.m_nAnimal); i++) {
            int n = this.cColors.AddItem(" ", EAnimals.GetClothNormal(this.m_nAnimal, i));
            UIMenuButton btn = (UIMenuButton)this.cColors.GetItem(n);
            ((UIButtonChooseColor)btn).SetLogoTex(EAnimals.GetClothNormalLogo(this.m_nAnimal, i));
         }
      } else {
         for (int i = 0; i < EAnimals.GetClothesCount(this.m_nAnimal); i++) {
            UIMenuButton btn = (UIMenuButton)this.cColors.GetItem(i);
            btn.SetData(EAnimals.GetClothNormal(this.m_nAnimal, i));
            ((UIButtonChooseColor)btn).SetLogoTex(EAnimals.GetClothNormalLogo(this.m_nAnimal, i));
         }
      }

      if (this.m_cModuleShowFB != null) {
         this.m_cModuleShowFB.SetFootballer(this.m_nAnimal);
      }

      this.cColors.SetSelection(this.m_nClothes);
      this.SetColor(this.m_nClothes);
   }

   public void Show() {
      if (this.m_cModuleShowFB != null) {
         this.m_cModuleShowFB.Activate();
      }

      this.ShowInfo();
      super.m_sTitle = Text.Get("&MenuTitleChooseColors&");
      if (this.m_bFirstShow) {
         this.CallMethodIn("Show", 0.0F);
         this.m_bFirstShow = false;
      } else {
         super.Show();
         this.CallMethodIn("Comment", 0.1F);
      }
   }

   public void OnCreate() {
      super.OnCreate();
   }
}
