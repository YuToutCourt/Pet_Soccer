public class MenuChampionships2 extends SoccerUserInterface {
   static UIWindowBlinking m_cOneBlinking = null;
   static final String BACK = "ID_BACK";
   static final String OK = "ID_OK";
   static final int ID_BACK = 1100;
   static final int ID_OK = 1101;
   int m_nResult = -1;
   MenuChampionships2.TeamInfo[] m_tQFinal = new MenuChampionships2.TeamInfo[8];
   MenuChampionships2.TeamInfo[] m_tSemiFinal = new MenuChampionships2.TeamInfo[4];
   MenuChampionships2.TeamInfo[] m_tFinal = new MenuChampionships2.TeamInfo[2];
   UIStaticBlinking m_cInfoScreenText = null;
   UIWindow m_cInfoScreen = null;
   float m_fInfoTime = 6.0F;

   void OnOK() {
      Championships ch = this.GetChampionships();
      if (this.m_nResult != 0 && (ch.m_cFinal == null || ch.m_cFinal.GetLooser() != ch.m_nPlayerTeam)) {
         if (this.m_nResult != 4 && (ch.m_cFinal == null || ch.m_cFinal.GetWinner() != ch.m_nPlayerTeam)) {
            MatchData match = ch.GetNextMatch();
            if (ch.m_bIsReplayingMatch) {
               match.ResetResults();
               match.SwitchSides();
            }

            ETeams.GetTeamData(ch.m_nPlayerTeam).m_nAnimalClothes = ch.m_nPlayerTeamClothes;
            this.PlayMatch(match);
         } else {
            ((SoccerMainMenuModule)this.GetModule()).ShowUI(13);
            this.m_nResult = -1;
         }
      } else {
         ch.RestartChampionship();
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(0);
         this.m_nResult = -1;
         SoccerGame.SaveAttrib();
         ((SoccerMainMenuModule)this.GetModule()).PlayMusic();
      }
   }

   void OnBack() {
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(0);
   }

   void HideInfoScreen() {
      if (m_cOneBlinking != null) {
         m_cOneBlinking.Blink();
      }

      this.m_cInfoScreenText.EndBlink();
      this.m_cInfoScreen.Hide();
      this.DisableCallMethod("HideInfoScreen");
   }

   void OnKeyPressed(int nKey, char cAscii) {
      switch (nKey) {
         case 17:
            this.GetChampionships().WinChampionships();
            this.Show();
      }
   }

   MenuChampionships2() {
      super.m_sFiles = new String[]{"data\\menu\\scr\\Championships2640.scr", "data\\menu\\scr\\Championships2.scr", "data\\menu\\scr\\Championships21024.scr"};
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

   void NewChampionships() {
   }

   void PlayMatch(MatchData cMatch) {
      if (cMatch == null) {
         Tools.CrashLogError("MenuChampionships2.PlayMatch cMatch == null");
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

   public void OnInit() {
      super.OnInit();
      this.AddToSlideIn((UIButton)this.GetElem("ID_BACK"));
      this.AddToSlideIn((UIButton)this.GetElem("ID_OK"));
      this.m_cInfoScreenText = (UIStaticBlinking)this.GetElem("ID_INFO_SCREEN_TEXT");
      this.m_cInfoScreen = this.GetElem("ID_INFO_SCREEN");
      this.m_cInfoScreen.Hide();
      this.m_cInfoScreen.SetAlphaForAll(0.75F);
      this.m_tQFinal[0] = new MenuChampionships2.TeamInfo(this, this.GetElem("ID_QFINAL_1"));
      this.m_tQFinal[1] = new MenuChampionships2.TeamInfo(this, this.GetElem("ID_QFINAL_2"));
      this.m_tQFinal[2] = new MenuChampionships2.TeamInfo(this, this.GetElem("ID_QFINAL_3"));
      this.m_tQFinal[3] = new MenuChampionships2.TeamInfo(this, this.GetElem("ID_QFINAL_4"));
      this.m_tQFinal[4] = new MenuChampionships2.TeamInfo(this, this.GetElem("ID_QFINAL_5"));
      this.m_tQFinal[5] = new MenuChampionships2.TeamInfo(this, this.GetElem("ID_QFINAL_6"));
      this.m_tQFinal[6] = new MenuChampionships2.TeamInfo(this, this.GetElem("ID_QFINAL_7"));
      this.m_tQFinal[7] = new MenuChampionships2.TeamInfo(this, this.GetElem("ID_QFINAL_8"));
      this.m_tSemiFinal[0] = new MenuChampionships2.TeamInfo(this, this.GetElem("ID_SEMIFINAL_1"));
      this.m_tSemiFinal[1] = new MenuChampionships2.TeamInfo(this, this.GetElem("ID_SEMIFINAL_2"));
      this.m_tSemiFinal[2] = new MenuChampionships2.TeamInfo(this, this.GetElem("ID_SEMIFINAL_3"));
      this.m_tSemiFinal[3] = new MenuChampionships2.TeamInfo(this, this.GetElem("ID_SEMIFINAL_4"));
      this.m_tFinal[0] = new MenuChampionships2.TeamInfo(this, this.GetElem("ID_FINAL_1"));
      this.m_tFinal[1] = new MenuChampionships2.TeamInfo(this, this.GetElem("ID_FINAL_2"));
      this.ProcessKb(true);
   }

   void Reset() {
      for (int i = 0; i < this.m_tQFinal.length; i++) {
         this.m_tQFinal[i].Looser(-1);
      }

      for (int i = 0; i < this.m_tSemiFinal.length; i++) {
         this.m_tSemiFinal[i].Looser(-1);
      }

      for (int i = 0; i < this.m_tFinal.length; i++) {
         this.m_tFinal[i].Looser(-1);
      }
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
      this.m_cInfoScreenText.Blink();
      this.CallMethodIn("HideInfoScreen", this.m_fInfoTime);
   }

   public void Hide() {
      super.Hide();
   }

   public void Show() {
      Championships ch = this.GetChampionships();
      super.m_sTitle = Championships.GetCupName(ch.m_nCurChampionships);
      if (ch == null) {
         Tools.CrashLogError("MenuChampionships.Show() brak obiektu mistrzostw w playerze");
      }

      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("Championships2.Show match " + Match.m_cMatchData);
      }

      if (Match.m_cMatchData != null) {
         this.m_nResult = ch.ReportMatchEnd(Match.m_cMatchData);
         SoccerGame.SaveAttrib();
         Match.m_cMatchData = null;
      }

      if (this.m_nResult == 4) {
         GameObject.CrashLog("victory");
         this.OnOK();
      } else {
         int nTeamA = -1;
         int nTeamB = -1;
         if (ch.m_cElimA1B2 != null) {
            if (ch.IsInGroup(0, ch.m_cElimA1B2.m_nTeamHost)) {
               nTeamA = ch.m_cElimA1B2.m_nTeamHost;
               nTeamB = ch.m_cElimA1B2.m_nTeamGuest;
            } else {
               nTeamA = ch.m_cElimA1B2.m_nTeamGuest;
               nTeamB = ch.m_cElimA1B2.m_nTeamHost;
            }

            this.m_tQFinal[0].SetInfo(nTeamA);
            this.m_tQFinal[1].SetInfo(nTeamB);
         } else {
            this.m_tQFinal[0].SetInfo(-1);
            this.m_tQFinal[1].SetInfo(-1);
         }

         if (ch.m_cElimA2B1 != null) {
            if (ch.IsInGroup(1, ch.m_cElimA2B1.m_nTeamHost)) {
               nTeamA = ch.m_cElimA2B1.m_nTeamHost;
               nTeamB = ch.m_cElimA2B1.m_nTeamGuest;
            } else {
               nTeamA = ch.m_cElimA2B1.m_nTeamGuest;
               nTeamB = ch.m_cElimA2B1.m_nTeamHost;
            }

            this.m_tQFinal[2].SetInfo(nTeamA);
            this.m_tQFinal[3].SetInfo(nTeamB);
         } else {
            this.m_tQFinal[2].SetInfo(-1);
            this.m_tQFinal[3].SetInfo(-1);
         }

         if (ch.m_cElimC1D2 != null) {
            if (ch.IsInGroup(2, ch.m_cElimC1D2.m_nTeamHost)) {
               nTeamA = ch.m_cElimC1D2.m_nTeamHost;
               nTeamB = ch.m_cElimC1D2.m_nTeamGuest;
            } else {
               nTeamA = ch.m_cElimC1D2.m_nTeamGuest;
               nTeamB = ch.m_cElimC1D2.m_nTeamHost;
            }

            this.m_tQFinal[4].SetInfo(nTeamA);
            this.m_tQFinal[5].SetInfo(nTeamB);
         } else {
            this.m_tQFinal[4].SetInfo(-1);
            this.m_tQFinal[5].SetInfo(-1);
         }

         if (ch.m_cElimC2D1 != null) {
            if (ch.IsInGroup(3, ch.m_cElimC2D1.m_nTeamHost)) {
               nTeamA = ch.m_cElimC2D1.m_nTeamHost;
               nTeamB = ch.m_cElimC2D1.m_nTeamGuest;
            } else {
               nTeamA = ch.m_cElimC2D1.m_nTeamGuest;
               nTeamB = ch.m_cElimC2D1.m_nTeamHost;
            }

            this.m_tQFinal[6].SetInfo(nTeamA);
            this.m_tQFinal[7].SetInfo(nTeamB);
         } else {
            this.m_tQFinal[6].SetInfo(-1);
            this.m_tQFinal[7].SetInfo(-1);
         }

         if (ch.m_cSemi1 != null) {
            nTeamA = ch.m_cElimA1B2.GetWinner();
            nTeamB = ch.m_cElimA2B1.GetWinner();
            this.m_tQFinal[0].Looser(ch.m_cElimA1B2.GetLooser());
            this.m_tQFinal[1].Looser(ch.m_cElimA1B2.GetLooser());
            this.m_tQFinal[2].Looser(ch.m_cElimA2B1.GetLooser());
            this.m_tQFinal[3].Looser(ch.m_cElimA2B1.GetLooser());
            this.m_tSemiFinal[0].SetInfo(nTeamA);
            this.m_tSemiFinal[1].SetInfo(nTeamB);
         } else {
            this.m_tSemiFinal[0].SetInfo(-1);
            this.m_tSemiFinal[1].SetInfo(-1);
         }

         if (ch.m_cSemi2 != null) {
            this.m_tQFinal[4].Looser(ch.m_cElimC1D2.GetLooser());
            this.m_tQFinal[5].Looser(ch.m_cElimC1D2.GetLooser());
            this.m_tQFinal[6].Looser(ch.m_cElimC2D1.GetLooser());
            this.m_tQFinal[7].Looser(ch.m_cElimC2D1.GetLooser());
            nTeamA = ch.m_cElimC1D2.GetWinner();
            nTeamB = ch.m_cElimC2D1.GetWinner();
            this.m_tSemiFinal[2].SetInfo(nTeamA);
            this.m_tSemiFinal[3].SetInfo(nTeamB);
         } else {
            this.m_tSemiFinal[2].SetInfo(-1);
            this.m_tSemiFinal[3].SetInfo(-1);
         }

         if (ch.m_cFinal != null) {
            this.m_tSemiFinal[0].Looser(ch.m_cSemi1.GetLooser());
            this.m_tSemiFinal[1].Looser(ch.m_cSemi1.GetLooser());
            this.m_tSemiFinal[2].Looser(ch.m_cSemi2.GetLooser());
            this.m_tSemiFinal[3].Looser(ch.m_cSemi2.GetLooser());
            nTeamA = ch.m_cSemi1.GetWinner();
            nTeamB = ch.m_cSemi2.GetWinner();
            this.m_tFinal[0].SetInfo(nTeamA);
            this.m_tFinal[1].SetInfo(nTeamB);
            this.m_tFinal[0].Looser(ch.m_cFinal.GetLooser());
            this.m_tFinal[1].Looser(ch.m_cFinal.GetLooser());
         } else {
            this.m_tFinal[0].SetInfo(-1);
            this.m_tFinal[1].SetInfo(-1);
         }

         if (this.m_nResult == 0) {
            ((UIButton)this.GetElem("ID_OK")).SetText(Text.Get("&MenuMenu&"));
            ((UIButton)this.GetElem("ID_BACK")).Hide();
            this.ShowInfoScreen(Text.Get("&MenuChampionshipsInfoLost&"));
            ((SoccerMainMenuModule)this.GetModule()).PlayMusic();
         } else {
            ((UIButton)this.GetElem("ID_OK")).SetText(Text.Get("&MenuOK&"));
            ((UIButton)this.GetElem("ID_BACK")).Show();
         }

         super.Show();
      }
   }

   public void OnCreate() {
      super.OnCreate();
   }

   Championships GetChampionships() {
      return CoreLogic.GetPlayer().m_cChampionships;
   }

   // [mod] classe imbriquee static : la version decompilee (this.this$0 = this$0) ne recompile pas
   public static class TeamInfo {
      MenuChampionships2 m_cOwner;
      UIWindowBlinking m_cLogo;
      UIStatic m_cName;
      UIWindow m_cLost;
      int m_nTeam;

      TeamInfo(MenuChampionships2 cOwner, UIWindow cWnd) {
         this.m_cOwner = cOwner;
         this.m_cLogo = null;
         this.m_cName = null;
         this.m_cLost = null;
         this.m_nTeam = -1;
         this.m_cLogo = (UIWindowBlinking)cWnd.GetElem("ID_LOGO");
         this.m_cName = (UIStatic)cWnd.GetElem("ID_NAME");
         this.m_cLost = cWnd.GetElem("ID_LOST");
         this.m_cLost.Hide();
      }

      void SetInfo(int nTeam) {
         this.m_nTeam = nTeam;
         if (nTeam != -1) {
            TeamData data = ETeams.GetTeamData(nTeam);
            this.m_cLogo.SetTexture(data.m_cLogo);
            this.m_cName.SetText(data.m_sNameAbr);
            if (data.m_nAnimalType == this.m_cOwner.GetChampionships().m_nPlayerTeam) {
               if (MenuChampionships2.m_cOneBlinking != null) {
                  MenuChampionships2.m_cOneBlinking.EndBlink();
               }

               MenuChampionships2.m_cOneBlinking = this.m_cLogo;
               this.m_cLogo.Blink();
            } else {
               this.m_cLogo.EndBlink();
            }
         } else {
            this.m_cLogo.SetTexture(null);
            this.m_cName.SetText("?");
         }
      }

      void Looser(int nTeam) {
         if (nTeam == this.m_nTeam && this.m_nTeam != -1) {
            this.m_cLost.Show();
         } else {
            this.m_cLost.Hide();
         }
      }
   }
}
