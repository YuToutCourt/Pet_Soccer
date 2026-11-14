public class MenuChampionships extends SoccerUserInterface {
   static UIWindowBlinking m_cOneBlinking = null;
   static final String BACK = "ID_BACK";
   static final String OK = "ID_OK";
   static final int ID_BACK = 1100;
   static final int ID_OK = 1101;
   MenuChampionships.TeamInfo[] m_tGroupA = new MenuChampionships.TeamInfo[3];
   MenuChampionships.TeamInfo[] m_tGroupB = new MenuChampionships.TeamInfo[3];
   MenuChampionships.TeamInfo[] m_tGroupC = new MenuChampionships.TeamInfo[3];
   MenuChampionships.TeamInfo[] m_tGroupD = new MenuChampionships.TeamInfo[3];
   MatchData m_cCurMatch = null;
   int m_nResult = -1;
   UIStatic m_cInfo = null;
   UIStatic m_cInfoTitle = null;
   UIStaticBlinking m_cInfoScreenText = null;
   UIWindow m_cInfoScreen = null;
   float m_fInfoTime = 6.0F;

   void OnOK() {
      SoccerGame.GetCommModule().GetCommentator().ClearCurrComment();
      SoccerGame.GetCommModule().GetCommentator().StopTalking();
      this.HideInfoScreen();
      Championships ch = this.GetChampionships();
      if (this.m_cCurMatch != null) {
         this.m_cCurMatch = null;
         Match.m_cMatchData = null;
         if (this.m_nResult == 0) {
            ch.RestartChampionship();
            ((SoccerMainMenuModule)this.GetModule()).ShowUI(0);
            this.m_nResult = -1;
            SoccerGame.SaveAttrib();
            ((SoccerMainMenuModule)this.GetModule()).PlayMusic();
            return;
         }

         if (this.m_nResult == 1) {
            ((SoccerMainMenuModule)this.GetModule()).GetUI(12).Reset();
            ((SoccerMainMenuModule)this.GetModule()).ShowUI(12);
            this.m_nResult = -1;
            return;
         }

         this.Show();
      } else {
         this.m_nResult = -1;
         MatchData match = ch.GetNextMatch();
         if (!match.IsPlaying(ch.m_nPlayerTeam)) {
            int nMaxScore = CoreLogic._HALF_TIMES[CoreLogic.GetPlayer().m_nMatchHalfTimeIndex];
            match.RandomizeResult(nMaxScore);
            Match.m_cMatchData = match;
            this.Show();
         } else {
            ETeams.GetTeamData(ch.m_nPlayerTeam).m_nAnimalClothes = ch.m_nPlayerTeamClothes;
            this.PlayMatch(match);
         }
      }
   }

   void OnBack() {
      SoccerGame.GetCommModule().GetCommentator().ClearCurrComment();
      SoccerGame.GetCommModule().GetCommentator().StopTalking();
      this.m_nResult = -1;
      this.HideInfoScreen();
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(0);
   }

   void HideInfoScreen() {
      if (m_cOneBlinking != null) {
         m_cOneBlinking.Blink();
      }

      this.m_cInfoScreenText.EndFade();
      this.m_cInfoScreen.Hide();
      this.DisableCallMethod("HideInfoScreen");
   }

   void OnKeyPressed(int nKey, char cAscii) {
      if (SoccerGame.m_bDebug) {
         switch (nKey) {
            case 17:
               this.GetChampionships().WinChampionships();
               this.Show();
               break;
            case 23:
               this.ShowInfoScreen("you lost the championships");
         }
      }
   }

   MenuChampionships() {
      super.m_sFiles = new String[]{"data\\menu\\scr\\Championships640.scr", "data\\menu\\scr\\Championships.scr", "data\\menu\\scr\\Championships1024.scr"};
   }

   void OnUICommand(UIWindow cSender, int nCommand) {
      super.OnUICommand(cSender, nCommand);
      if (nCommand == 1) {
         GameObject.LogLine("-i-- Double Click -----" + cSender.m_sId);
      }

      if (nCommand == 0) {
         int nID = -1;
         if (cSender.m_sId.equals("ID_BACK")) {
            nID = 1100;
         }

         if (cSender.m_sId.equals("ID_OK")) {
            nID = 1101;
         }

         if (this.GetModule() != null) {
            switch (nID) {
               case 1100:
                  this.OnBack();
                  break;
               case 1101:
                  this.OnOK();
                  break;
               default:
                  ((SoccerMenuModule)this.GetModule()).HandleUIClick(nID);
            }
         }
      }
   }

   void FillGroupInfoTab(MenuChampionships.TeamInfo[] cGroup, UIWindow cGroupWin) {
      cGroup[0] = new MenuChampionships.TeamInfo(
         this, (UIStatic)cGroupWin.GetElem("ID_NAME_1"), (UIWindowBlinking)cGroupWin.GetElem("ID_LOGO_1"), (UIStatic)cGroupWin.GetElem("ID_SCORE_1")
      );
      cGroup[1] = new MenuChampionships.TeamInfo(
         this, (UIStatic)cGroupWin.GetElem("ID_NAME_2"), (UIWindowBlinking)cGroupWin.GetElem("ID_LOGO_2"), (UIStatic)cGroupWin.GetElem("ID_SCORE_2")
      );
      cGroup[2] = new MenuChampionships.TeamInfo(
         this, (UIStatic)cGroupWin.GetElem("ID_NAME_3"), (UIWindowBlinking)cGroupWin.GetElem("ID_LOGO_3"), (UIStatic)cGroupWin.GetElem("ID_SCORE_3")
      );
   }

   MenuChampionships.TeamInfo[] GetGroup(int nGroup) {
      switch (nGroup) {
         case 0:
            return this.m_tGroupA;
         case 1:
            return this.m_tGroupB;
         case 2:
            return this.m_tGroupC;
         case 3:
            return this.m_tGroupD;
         default:
            return null;
      }
   }

   void PlayMatch(MatchData cMatch) {
      if (cMatch == null) {
         Tools.CrashLogError("MenuChampionships.PlayMatch cMatch == null");
      }

      Match.m_cMatchData = cMatch;
      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("Setting Match.m_cMatchData to " + cMatch + "\n");
      }

      CoreLogic.m_cTeamDataLeft = ETeams.GetTeamData(cMatch.m_nTeamHost);
      CoreLogic.m_cTeamDataRight = ETeams.GetTeamData(cMatch.m_nTeamGuest);
      // [mod] stade de l'equipe qui recoit : le switch d'origine (0..11) laissait un niveau invalide pour Panda / Tigre
      CoreLogic.m_nNextLevel = ETeams.GetHomeField(cMatch.m_nTeamHost);

      ((SoccerMainMenuModule)this.GetModule()).ShowUI(9);
   }

   String GetGroupPrevMatch(int nGroup, String sPrefix) {
      float fInfoWidth = this.m_cInfo.GetSize().fX;
      Championships ch = this.GetChampionships();
      String sMatch = ch.GetGroupPrevMatch(nGroup).toString();
      if (this.m_cInfo.GetTextExtent(sPrefix + sMatch).fX >= fInfoWidth - 5.0F) {
         sMatch = ch.GetGroupPrevMatch(nGroup).toStringAbbr();
         if (this.m_cInfo.GetTextExtent(sPrefix + sMatch).fX >= fInfoWidth - 5.0F) {
            sMatch = ch.GetGroupPrevMatch(nGroup).toStringMin();
         }
      }

      return sMatch;
   }

   void Reset() {
      CoreLogic.m_bMultiplayer = false;
      CoreLogic.m_bChampionships = true;
      CoreLogic.m_bDemo = false;
      Match.m_cMatchData = null;
   }

   public void OnInit() {
      super.OnInit();
      this.m_cInfo = (UIStatic)this.GetElem("ID_INFO");
      this.m_cInfoTitle = (UIStatic)this.GetElem("ID_INFO_FRAME");
      this.m_cInfoScreenText = (UIStaticBlinking)this.GetElem("ID_INFO_SCREEN_TEXT");
      this.m_cInfoScreen = this.GetElem("ID_INFO_SCREEN");
      this.m_cInfoScreen.Hide();
      this.m_cInfoScreen.SetAlphaForAll(0.75F);
      this.AddToSlideIn((UIButton)this.GetElem("ID_BACK"));
      this.AddToSlideIn((UIButton)this.GetElem("ID_OK"));
      UIWindow cGroup = this.GetElem("ID_GROUP_A");
      this.FillGroupInfoTab(this.m_tGroupA, cGroup);
      cGroup = this.GetElem("ID_GROUP_B");
      this.FillGroupInfoTab(this.m_tGroupB, cGroup);
      cGroup = this.GetElem("ID_GROUP_C");
      this.FillGroupInfoTab(this.m_tGroupC, cGroup);
      cGroup = this.GetElem("ID_GROUP_D");
      this.FillGroupInfoTab(this.m_tGroupD, cGroup);
      this.ProcessKb(true);
   }

   void ShowInfoScreen(String sText) {
      this.DisableCallMethod("HideInfoScreen");
      if (m_cOneBlinking != null) {
         m_cOneBlinking.EndBlink();
      }

      this.m_cInfoScreenText.SetText(sText);
      this.m_cInfoScreenText.m_fBlinkTime = this.m_fInfoTime;
      this.m_cInfoScreenText.m_fBlinkHide = 0.15F;
      this.m_cInfoScreen.Show();
      this.m_cInfoScreenText.Show();
      this.m_cInfoScreenText.Fade();
      this.CallMethodIn("HideInfoScreen", this.m_fInfoTime);
   }

   public void Hide() {
      super.Hide();
   }

   void SetTeamInfo(int nGroup, int nTeamPos, int nTeam, int nPoints) {
      this.GetGroup(nGroup)[nTeamPos].SetInfo(nTeam, nPoints);
   }

   public void Show() {
      Championships ch = this.GetChampionships();
      if (ch.m_nPhase > 0) {
         ((SoccerMainMenuModule)this.GetModule()).GetUI(12).Reset();
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(12);
      } else {
         this.m_cCurMatch = Match.m_cMatchData;
         String sPlayerGroupA = "";
         String sPlayerGroupB = "";
         if (ch.m_nPlayerGroup == 0) {
            sPlayerGroupA = Text.Get("&MenuChampionshipsPlayerGroup&");
         }

         if (ch.m_nPlayerGroup == 1) {
            sPlayerGroupB = Text.Get("&MenuChampionshipsPlayerGroup&");
         }

         if (this.m_cCurMatch != null) {
            this.m_cInfoTitle
               .SetText(
                  Text.Get("&MenuChampionshipsInfoTitle1&")
                     + String.valueOf(ch.m_nCurGroupMatch + 1)
                     + Text.Get("&MenuChampionshipsInfoTitle2&")
                     + (ch.m_nMaxGroupMatch + 1)
               );
            this.m_nResult = ch.ReportMatchEnd(this.m_cCurMatch);
            SoccerGame.SaveAttrib();
            String sText = Text.Get("&MenuChampionshipsInfoExplanation&") + " \n" + " \n" + Text.Get("&MenuChampionshipsInfoResults&") + " \n" + " \n";
            if (sPlayerGroupA.length() > 0) {
               sText = sText
                  + "["
                  + Text.Get("&MenuChampionshipsGroupA&")
                  + "]"
                  + sPlayerGroupA
                  + " \n"
                  + "  "
                  + this.GetGroupPrevMatch(0, "  ")
                  + " \n"
                  + " \n"
                  + "["
                  + Text.Get("&MenuChampionshipsGroupB&")
                  + "]"
                  + " \n"
                  + "  "
                  + this.GetGroupPrevMatch(1, "  ")
                  + " \n";
            }

            if (sPlayerGroupB.length() > 0) {
               sText = sText
                  + "["
                  + Text.Get("&MenuChampionshipsGroupB&")
                  + "]"
                  + sPlayerGroupB
                  + " \n"
                  + "  "
                  + this.GetGroupPrevMatch(1, "  ")
                  + " \n"
                  + " \n"
                  + "["
                  + Text.Get("&MenuChampionshipsGroupA&")
                  + "]"
                  + " \n"
                  + "  "
                  + this.GetGroupPrevMatch(0, "  ")
                  + " \n";
            }

            sText = sText
               + "["
               + Text.Get("&MenuChampionshipsGroupC&")
               + "]"
               + " \n"
               + "  "
               + this.GetGroupPrevMatch(2, "  ")
               + " \n"
               + "["
               + Text.Get("&MenuChampionshipsGroupD&")
               + "]"
               + " \n"
               + "  "
               + this.GetGroupPrevMatch(3, "  ");
            this.m_cInfo.SetText(sText);
         } else if (ch.GetNextMatch() != null) {
            TeamData cTeam1 = ETeams.GetTeamData(ch.GetNextMatch().m_nTeamHost);
            TeamData cTeam2 = ETeams.GetTeamData(ch.GetNextMatch().m_nTeamGuest);
            this.m_cInfoTitle
               .SetText(
                  Text.Get("&MenuChampionshipsInfoTitle1&")
                     + String.valueOf(ch.m_nCurGroupMatch + 1)
                     + Text.Get("&MenuChampionshipsInfoTitle2&")
                     + (ch.m_nMaxGroupMatch + 1)
               );
            String sText = Text.Get("&MenuChampionshipsInfoExplanation&") + " \n" + " \n" + Text.Get("&MenuChampionshipsInfoNowPlaying&") + " \n" + " \n";
            if (sPlayerGroupA.length() > 0) {
               sText = sText
                  + "["
                  + Text.Get("&MenuChampionshipsGroupA&")
                  + "]"
                  + sPlayerGroupA
                  + " \n"
                  + "  "
                  + this.GetGroupCurMatch(0, "  ")
                  + " \n"
                  + " \n"
                  + "["
                  + Text.Get("&MenuChampionshipsGroupB&")
                  + "]"
                  + " \n"
                  + "  "
                  + this.GetGroupCurMatch(1, "  ")
                  + " \n";
            }

            if (sPlayerGroupB.length() > 0) {
               sText = sText
                  + "["
                  + Text.Get("&MenuChampionshipsGroupB&")
                  + "]"
                  + sPlayerGroupB
                  + " \n"
                  + "  "
                  + this.GetGroupCurMatch(1, "  ")
                  + " \n"
                  + " \n"
                  + "["
                  + Text.Get("&MenuChampionshipsGroupA&")
                  + "]"
                  + " \n"
                  + "  "
                  + this.GetGroupCurMatch(0, "  ")
                  + " \n";
            }

            sText = sText
               + "["
               + Text.Get("&MenuChampionshipsGroupC&")
               + "]"
               + " \n"
               + "  "
               + this.GetGroupCurMatch(2, "  ")
               + " \n"
               + "["
               + Text.Get("&MenuChampionshipsGroupD&")
               + "]"
               + " \n"
               + "  "
               + this.GetGroupCurMatch(3, "  ");
            this.m_cInfo.SetText(sText);
         }

         if (ch == null) {
            Tools.CrashLogError("MenuChampionships.Show() brak obiektu mistrzostw w playerze");
         }

         for (int nGroup = 0; nGroup < 4; nGroup++) {
            for (int i = 0; i < 3; i++) {
               Championships.TeamRecord record = ch.GetTeamRecord(nGroup, i);
               this.SetTeamInfo(nGroup, i, record.m_nTeam, record.m_nPoints);
            }
         }

         super.m_sTitle = Championships.GetCupName(ch.m_nCurChampionships);
         if (this.m_nResult == 0) {
            ((SoccerMainMenuModule)this.GetModule()).PlayMusic();
            ((UIButton)this.GetElem("ID_OK")).SetText(Text.Get("&MenuMenu&"));
            ((UIButton)this.GetElem("ID_BACK")).Hide();
            this.ShowInfoScreen(Text.Get("&MenuChampionshipsInfoLost&"));
         } else {
            ((UIButton)this.GetElem("ID_OK")).SetText(Text.Get("&MenuOK&"));
            ((UIButton)this.GetElem("ID_BACK")).Show();
            if (this.m_nResult == 1) {
               this.ShowInfoScreen(Text.Get("&MenuChampionshipsInfoFinals&"));
            }
         }

         super.Show();
      }
   }

   String GetGroupCurMatch(int nGroup, String sPrefix) {
      float fInfoWidth = this.m_cInfo.GetSize().fX;
      Championships ch = this.GetChampionships();
      String sMatch = ch.GetGroupCurMatch(nGroup).toStringBeforeMatch();
      if (this.m_cInfo.GetTextExtent(sPrefix + sMatch).fX >= fInfoWidth - 5.0F) {
         sMatch = ch.GetGroupCurMatch(nGroup).toStringBeforeMatchAbr();
      }

      return sMatch;
   }

   public void OnCreate() {
      super.OnCreate();
   }

   Championships GetChampionships() {
      return CoreLogic.GetPlayer().m_cChampionships;
   }

   // [mod] classe imbriquee static : la version decompilee (this.this$0 = this$0) ne recompile pas
   public static class TeamInfo {
      MenuChampionships m_cOwner;
      UIWindowBlinking m_cLogo;
      UIStatic m_cName;
      UIStatic m_cScore;

      TeamInfo(MenuChampionships cOwner, UIStatic cName, UIWindowBlinking cLogo, UIStatic cScore) {
         this.m_cOwner = cOwner;
         this.m_cLogo = null;
         this.m_cName = null;
         this.m_cScore = null;
         this.m_cLogo = cLogo;
         this.m_cName = cName;
         this.m_cScore = cScore;
      }

      void SetInfo(int nTeam, int nScore) {
         TeamData data = ETeams.GetTeamData(nTeam);
         this.m_cLogo.SetTexture(data.m_cLogo);
         this.m_cName.SetText(data.m_sNameAbr);
         this.m_cScore.SetText(String.valueOf(nScore));
         if (data.m_nAnimalType == this.m_cOwner.GetChampionships().m_nPlayerTeam) {
            MenuChampionships.m_cOneBlinking = this.m_cLogo;
            this.m_cLogo.Blink();
         } else {
            this.m_cLogo.EndBlink();
         }
      }
   }
}
