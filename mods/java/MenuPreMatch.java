public class MenuPreMatch extends SoccerUserInterface {
   static final String PRE_MATCH_BACK = "ID_BACK";
   static final String PRE_MATCH_PLAY = "ID_PLAY";
   static final int PRE_MATCH_BACK_ID = 400;
   static final int PRE_MATCH_PLAY_ID = 401;
   UIStatic m_cName1 = null;
   UIStatic m_cName2 = null;
   UIStatic m_cPlayers1 = null;
   UIStatic m_cPlayers2 = null;
   UIStatic m_cPlay = null;
   UIWindow m_cLogo1 = null;
   UIWindow m_cLogo2 = null;
   static boolean bFirstShow = false;
   boolean bCheated = false;

   void OnOK() {
      this.bCheated = false;
      SoccerGame.game.GetCommentator().HideNow();
      if (!CoreLogic.m_bMultiplayer) {
         SoccerGame.game.RunModule();
      } else {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(7);
      }
   }

   void OnBack() {
      if (CoreLogic.m_bChampionships) {
         if (!this.bCheated) {
            Match.m_cMatchData.ResetResults();
            Match.m_cMatchData = null;
         } else {
            this.bCheated = false;
         }

         ((SoccerMainMenuModule)this.GetModule()).ShowUI(11);
      } else if (CoreLogic.m_bMultiplayer) {
         if (!this.bCheated) {
            Match.m_cMatchData.ResetResults();
            Match.m_cMatchData = null;
         } else {
            this.bCheated = false;
         }

         ((SoccerMainMenuModule)this.GetModule()).ShowUI(17);
      } else if (CoreLogic.m_bSurvival) {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_SURVIVAL); // [mod]
      } else if (ModOnlineLeague.m_bMatch) {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_ONLINE_LEAGUE); // [mod] ligue en ligne
      } else if (CoreLogic.m_bLeague) {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_LEAGUE); // [mod]
      } else {
         MenuBackground.SetPlayerType(2);
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(3);
      }
   }

   void OnKeyPressed(int nKey, char cAscii) {
      switch (nKey) {
         case 2:
            if (CoreLogic.m_bChampionships || CoreLogic.m_bMultiplayer) {
               this.bCheated = true;
               Match.m_cMatchData.m_nGoalsHost = 1;
               Match.m_cMatchData.m_nGoalsGuest = 0;
               Tools.ShowInfo(Match.m_cMatchData.toString(), this.GetModule(), ESpriteText.TOP_LEFT);
            }
            break;
         case 3:
            if (CoreLogic.m_bChampionships || CoreLogic.m_bMultiplayer) {
               this.bCheated = true;
               Match.m_cMatchData.m_nGoalsHost = 0;
               Match.m_cMatchData.m_nGoalsGuest = 0;
               Tools.ShowInfo(Match.m_cMatchData.toString(), this.GetModule(), ESpriteText.TOP_LEFT);
            }
            break;
         case 4:
            if (CoreLogic.m_bChampionships || CoreLogic.m_bMultiplayer) {
               this.bCheated = true;
               Match.m_cMatchData.m_nGoalsHost = 0;
               Match.m_cMatchData.m_nGoalsGuest = 1;
               Tools.ShowInfo(Match.m_cMatchData.toString(), this.GetModule(), ESpriteText.TOP_LEFT);
            }
      }
   }

   MenuPreMatch() {
      super.m_sFiles = new String[]{"data\\menu\\scr\\prematch640.scr", "data\\menu\\scr\\prematch.scr", "data\\menu\\scr\\prematch1024.scr"};
   }

   void OnUICommand(UIWindow cSender, int nCommand) {
      super.OnUICommand(cSender, nCommand);
      if (nCommand == 1) {
         GameObject.LogLine("-i-- Double Click -----" + cSender.m_sId);
      }

      if (nCommand == 0) {
         int nID = -1;
         if (cSender.m_sId.equals("ID_BACK")) {
            nID = 400;
         }

         if (cSender.m_sId.equals("ID_PLAY")) {
            nID = 401;
         }

         if (cSender.m_sId.equals("ID_TACTICS")) {
            nID = 402;
         }

         if (this.GetModule() != null) {
            switch (nID) {
               case 400:
                  this.OnBack();
                  break;
               case 401:
                  this.OnOK();
                  break;
               case 402:
                  ((SoccerMainMenuModule)this.GetModule()).ShowUI(18); // [mod] écran Tactique
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

   void OnKeyReleased(int nKey, char cAscii) {
      switch (nKey) {
         case 2:
         case 3:
         case 4:
            if (CoreLogic.m_bChampionships || CoreLogic.m_bMultiplayer) {
               Tools.ShowInfoTop("", this.GetModule());
            }
      }
   }

   public void OnInit() {
      super.OnInit();
      this.m_cName1 = (UIStatic)this.GetElem("ID_INFO_FRAME_1");
      this.m_cName2 = (UIStatic)this.GetElem("ID_INFO_FRAME_2");
      this.m_cPlayers1 = (UIStatic)this.GetElem("ID_PLAYERS_1");
      this.m_cPlayers2 = (UIStatic)this.GetElem("ID_PLAYERS_2");
      this.m_cPlay = (UIStatic)this.GetElem("ID_PLAY");
      this.m_cLogo1 = this.GetElem("ID_LOGO_TEAM_1");
      this.m_cLogo2 = this.GetElem("ID_LOGO_TEAM_2");
      this.AddToSlideIn(this.GetElem("ID_BACK"));
      this.AddToSlideIn(this.GetElem("ID_TACTICS")); // [mod]
      this.AddToSlideIn(this.GetElem("ID_PLAY"));
      if (SoccerGame.m_bDebug) {
         this.SetProcessKb(true);
      }
   }

   void ShowInfo(TeamData t1, TeamData t2) {
      if (bFirstShow) {
         bFirstShow = false;
         this.m_cPlayers1.OnSizeAndPos();
         this.m_cPlayers2.OnSizeAndPos();
      }

      this.m_cName1.SetText(t1.m_sName.toLowerCase());
      this.m_cName2.SetText(t2.m_sName.toLowerCase());
      this.m_cPlayers1.SetText(t1.m_sPlayers);
      this.m_cPlayers2.SetText(t2.m_sPlayers);
      this.m_cLogo1.SetTexture(t1.m_cLogo);
      this.m_cLogo2.SetTexture(t2.m_cLogo);
   }

   public void Show() {
      if (!CoreLogic.m_bMultiplayer) {
         this.m_cPlay.SetText(Text.Get("&MenuPreMatchPlay&"));
      } else {
         this.m_cPlay.SetText(Text.Get("&MenuOK&"));
      }

      SoccerGame.game.GetCommentator().StopTalking();
      SoccerGame.game.GetCommentator().Hide();
      TeamData t1 = CoreLogic.m_cTeamDataLeft;
      TeamData t2 = CoreLogic.m_cTeamDataRight;
      if (t1 != null && t2 != null) {
         this.ShowInfo(t1, t2);
      }

      super.m_sTitle = "";
      super.Show();
   }
}
