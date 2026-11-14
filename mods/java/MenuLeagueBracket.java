// [mod] Playoffs de la ligue (mode Ligue, ModLeagueMode / ModLeague) : tableau des gagnants en haut, des perdants en bas,
// grande finale (et revanche éventuelle) à droite, prochain match et dernier résultat du joueur en bas.
// Ouvert par MenuLeague (où ramène le retour de match). Écran généré par mods/ui/make_league_scr.py
// (prévu pour 8 qualifiés au plus), voir notes/league.md.
public class MenuLeagueBracket extends SoccerUserInterface {
   static final int _WB_ROUNDS = 3;
   static final int _LB_ROUNDS = 4;
   static final int[] _WB_SLOTS = new int[]{4, 2, 1};
   static final int[] _LB_SLOTS = new int[]{2, 2, 1, 1};
   UIStatic m_cTitle = null;
   UIStatic m_cMsg0 = null;
   UIStatic m_cMsg1 = null;
   UIMenuButton m_cPlay = null;
   UIMenuButton m_cBack = null;
   UIStaticBlinking m_cInfoScreenText = null;
   UIWindow m_cInfoScreen = null;
   float m_fInfoTime = 5.0F;

   MenuLeagueBracket() {
      super.m_sFiles = new String[]{"data\\menu\\scr\\LeagueBracket640.scr", "data\\menu\\scr\\LeagueBracket.scr", "data\\menu\\scr\\LeagueBracket1024.scr"};
   }

   public void OnInit() {
      super.OnInit();
      this.m_cTitle = (UIStatic)this.GetElem("ID_TITLE");
      this.m_cMsg0 = (UIStatic)this.GetElem("ID_MSG0");
      this.m_cMsg1 = (UIStatic)this.GetElem("ID_MSG1");
      this.m_cPlay = (UIMenuButton)this.GetElem("ID_OK");
      this.m_cBack = (UIMenuButton)this.GetElem("ID_CANCEL");
      this.m_cInfoScreenText = (UIStaticBlinking)this.GetElem("ID_INFO_SCREEN_TEXT");
      this.m_cInfoScreen = this.GetElem("ID_INFO_SCREEN");
      this.m_cInfoScreen.Hide();
      this.m_cInfoScreen.SetAlphaForAll(0.75F);
      this.AddToSlideIn(this.m_cPlay);
      this.AddToSlideIn(this.m_cBack);
      this.ProcessKb(true);
   }

   public void Show() {
      ModLeague l = ModLeagueMode.GetLeague();
      if (l == null || l.m_nPhase == ModLeague._PHASE_LEAGUE) {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(l == null ? 0 : SoccerMainMenuModule._UI_LEAGUE);
         return;
      }

      CoreLogic.m_bLeague = true;
      // résultat du dernier match de playoffs (une seule annonce)
      int nReport = -2;
      if (ModLeagueMode.m_bHasLast && !ModLeagueMode.m_bLastLeague) {
         nReport = ModLeagueMode.m_nLastResult;
      }

      ModLeagueMode.m_bHasLast = false;
      this.Refresh(l, nReport);
      super.m_sTitle = "";
      this.CenterCursor();
      super.Show();
      if (nReport == ModLeague._RES_CHAMPION) {
         this.ShowInfoScreen(Text.Get("&LeagueInfoWon&"));
      } else if (nReport == ModLeague._RES_REPLAY) {
         this.ShowInfoScreen(Text.Get("&LeagueInfoReplay&"));
      } else if (nReport == ModLeague._RES_OUT) {
         this.ShowInfoScreen(Text.Get("&LeagueInfoOut&"));
      } else if (nReport == ModLeague._RES_CONTINUE && this.DroppedToLosers(l)) {
         this.ShowInfoScreen(Text.Get("&LeagueInfoLosers&"));
      }
   }

   // le dernier match du joueur est une défaite dans le tableau des gagnants : il passe dans celui des perdants
   boolean DroppedToLosers(ModLeague l) {
      int m = this.GetLastPlayerMatch(l);
      return m >= 0 && l.m_tBType[m] == ModLeague._WB && l.GetLoser(m) == l.m_nPlayer && l.m_nPhase == ModLeague._PHASE_PLAYOFFS;
   }

   // dernier match joué par le joueur dans le tableau (-1 s'il n'en a pas)
   int GetLastPlayerMatch(ModLeague l) {
      int nLast = -1;

      for (int m = 0; m < l.m_nMatches; m++) {
         if (l.m_tBPlayed[m] && (l.m_tBA[m] == l.m_nPlayer || l.m_tBB[m] == l.m_nPlayer) && (nLast < 0 || l.m_tBDay[m] > l.m_tBDay[nLast])) {
            nLast = m;
         }
      }

      return nLast;
   }

   // nom d'un tour du tableau
   static String RoundName(ModLeague l, int nType, int nRound) {
      if (nType == ModLeague._GF) {
         return Text.Get("&LeagueRoundGF&");
      } else if (nType == ModLeague._GF2) {
         return Text.Get("&LeagueRoundGF2&");
      } else if (nType == ModLeague._WB) {
         int nFromEnd = l.GetWBRounds() - 1 - nRound;
         if (nFromEnd == 0) {
            return Text.Get(l.m_tSeed.length == 2 ? "&LeagueRoundFinal&" : "&LeagueRoundWBFinal&");
         } else {
            return Text.Get(nFromEnd == 1 ? "&LeagueRoundSemi&" : "&LeagueRoundQuarter&");
         }
      } else {
         return nRound == l.GetLBRounds() - 1 ? Text.Get("&LeagueRoundLBFinal&") : Text.Get("&LeagueRoundLB&") + " " + (nRound + 1);
      }
   }

   void SetHeader(String sId, String sText) {
      UIStatic s = (UIStatic)this.GetElem(sId);
      if (s != null) {
         s.SetText(sText);
         MenuLeague.SetColor(s, MenuLeague._GOLD);
      }
   }

   void SetLine(String sId, String sName, String sScore, float[] c) {
      UIStatic n = (UIStatic)this.GetElem(sId + "N");
      UIStatic s = (UIStatic)this.GetElem(sId + "S");
      if (n != null && s != null) {
         n.SetText(sName);
         s.SetText(sScore);
         MenuLeague.SetColor(n, c);
         MenuLeague.SetColor(s, c);
      }
   }

   // un match du tableau dans la case sPrefix (ID_<prefix>_A / _B), m = -1 : case vide
   void SetSlot(ModLeague l, String sPrefix, int m) {
      String sId = "ID_" + sPrefix + "_";
      if (m < 0 || !l.m_tBActive[m]) {
         this.SetLine(sId + "A", "", "", MenuLeague._WHITE);
         this.SetLine(sId + "B", "", "", MenuLeague._WHITE);
      } else {
         this.SetTeamLine(l, sId + "A", m, l.m_tBA[m], l.m_tBGA[m]);
         this.SetTeamLine(l, sId + "B", m, l.m_tBB[m], l.m_tBGB[m]);
      }
   }

   void SetTeamLine(ModLeague l, String sId, int m, int nTeam, int nGoals) {
      if (nTeam < 0) {
         this.SetLine(sId, "-", "", MenuLeague._GREY);
      } else {
         String sName = (l.GetSeed(nTeam) + 1) + ". " + MenuLeague.TeamName(l.m_tTeam[nTeam]);
         boolean bPlayed = l.m_tBPlayed[m];
         float[] c = MenuLeague._WHITE;
         if (nTeam == l.m_nPlayer) {
            c = MenuLeague._GOLD;
         } else if (bPlayed && l.GetLoser(m) == nTeam) {
            c = MenuLeague._GREY;
         }

         this.SetLine(sId, sName, bPlayed ? "" + nGoals : "", c);
      }
   }

   void SetMsg(UIStatic s, String sText, float[] c) {
      s.SetText(sText);
      MenuLeague.SetColor(s, c);
   }

   void Refresh(ModLeague l, int nReport) {
      this.m_cTitle.SetText(Text.Get("&LeagueTitle&"));
      int nWB = l.GetWBRounds();
      int nLB = l.GetLBRounds();

      for (int r = 0; r < _WB_ROUNDS; r++) {
         this.SetHeader("ID_WH" + r, r < nWB ? RoundName(l, ModLeague._WB, r) : "");

         for (int i = 0; i < _WB_SLOTS[r]; i++) {
            this.SetSlot(l, "W" + r + "_" + i, r < nWB ? l.FindMatch(ModLeague._WB, r, i) : -1);
         }
      }

      for (int r = 0; r < _LB_ROUNDS; r++) {
         this.SetHeader("ID_LH" + r, r < nLB ? RoundName(l, ModLeague._LB, r) : "");

         for (int i = 0; i < _LB_SLOTS[r]; i++) {
            this.SetSlot(l, "L" + r + "_" + i, r < nLB ? l.FindMatch(ModLeague._LB, r, i) : -1);
         }
      }

      int nGF = l.FindMatch(ModLeague._GF, 0, 0);
      int nGF2 = l.FindMatch(ModLeague._GF2, 0, 0);
      this.SetHeader("ID_GH", nGF >= 0 ? RoundName(l, ModLeague._GF, 0) : "");
      this.SetSlot(l, "G", nGF);
      boolean bGF2 = nGF2 >= 0 && l.m_tBActive[nGF2];
      this.SetHeader("ID_GH2", bGF2 ? RoundName(l, ModLeague._GF2, 0) : "");
      this.SetSlot(l, "G2", bGF2 ? nGF2 : -1);
      // 1re ligne : prochain match ou issue des playoffs
      int nNext = l.GetPlayerMatch();
      if (l.m_nPhase == ModLeague._PHASE_OVER) {
         if (l.m_nChampion == l.m_nPlayer) {
            this.SetMsg(this.m_cMsg0, Text.Get("&LeagueChampionPlayer&"), MenuLeague._GOLD);
         } else {
            this.SetMsg(this.m_cMsg0, Text.Get("&LeagueChampion&") + " : " + MenuLeague.TeamName(l.m_tTeam[l.m_nChampion]), MenuLeague._WHITE);
         }

         this.m_cPlay.SetText(Text.Get("&MenuMenu&"));
         this.m_cBack.Hide();
      } else if (nNext >= 0) {
         int h = l.m_tTeam[l.GetHost(nNext)];
         int g = l.m_tTeam[l.GetGuest(nNext)];
         this.SetMsg(
            this.m_cMsg0,
            Text.Get("&LeagueNextMatch&") + " : " + MenuLeague.TeamName(h) + Text.Get("&NowPlayingAnd&") + MenuLeague.TeamName(g)
               + " (" + RoundName(l, l.m_tBType[nNext], l.m_tBRound[nNext]) + ")",
            MenuLeague._WHITE
         );
         this.m_cPlay.SetText(Text.Get("&LeaguePlay&"));
         this.m_cBack.Show();
      } else {
         this.SetMsg(this.m_cMsg0, "", MenuLeague._WHITE);
         this.m_cPlay.SetText(Text.Get("&MenuMenu&"));
         this.m_cBack.Show();
      }

      this.m_cBack.SetText(Text.Get("&MenuBack&"));
      // 2e ligne : dernier résultat du joueur
      int nLast = this.GetLastPlayerMatch(l);
      if (nReport == ModLeague._RES_REPLAY) {
         this.SetMsg(this.m_cMsg1, Text.Get("&LeagueInfoReplay&"), MenuLeague._GOLD);
      } else if (nLast >= 0) {
         boolean bMineA = l.m_tBA[nLast] == l.m_nPlayer;
         boolean bWon = l.GetWinner(nLast) == l.m_nPlayer;
         int nOpp = bMineA ? l.m_tBB[nLast] : l.m_tBA[nLast];
         int nFor = bMineA ? l.m_tBGA[nLast] : l.m_tBGB[nLast];
         int nAgainst = bMineA ? l.m_tBGB[nLast] : l.m_tBGA[nLast];
         String sText = Text.Get(bWon ? "&LeagueLastWin&" : "&LeagueLastLoss&")
            + " "
            + nFor
            + "-"
            + nAgainst
            + " "
            + Text.Get("&LeagueAgainst&")
            + " "
            + MenuLeague.TeamName(l.m_tTeam[nOpp])
            + " ("
            + RoundName(l, l.m_tBType[nLast], l.m_tBRound[nLast])
            + ")";
         if (nReport != -2 && ModLeagueMode.m_bLastGolden) {
            sText = sText + " - " + Text.Get("&SurvivalGolden&");
         } else if (!bWon && l.m_tBType[nLast] == ModLeague._WB && l.m_nPhase == ModLeague._PHASE_PLAYOFFS) {
            sText = sText + " - " + Text.Get("&LeagueToLosers&");
         }

         this.SetMsg(this.m_cMsg1, sText, bWon ? MenuLeague._GREEN : MenuLeague._RED);
      } else if (!l.IsPlayerQualified()) {
         this.SetMsg(this.m_cMsg1, Text.Get("&LeagueNotQualified&") + " (" + (l.GetRank(l.m_nPlayer) + 1) + ")", MenuLeague._RED);
      } else {
         this.SetMsg(this.m_cMsg1, Text.Get("&LeagueQualified&") + " (" + (l.GetRank(l.m_nPlayer) + 1) + ")", MenuLeague._GREEN);
      }
   }

   void OnOK() {
      this.HideInfoScreen();
      ModLeague l = ModLeagueMode.GetLeague();
      if (l == null || l.m_nPhase == ModLeague._PHASE_OVER) {
         // ligue terminée : elle est retirée de league.dat (les matchs restent dans l'historique du profil)
         ModLeagueMode.Stop();
         this.GoToMainMenu();
      } else if (ModLeagueMode.PrepareMatch()) {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(9);
      }
   }

   void OnBack() {
      this.HideInfoScreen();
      this.GoToMainMenu();
   }

   void GoToMainMenu() {
      CoreLogic.m_bLeague = false;
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(0);
   }

   void ShowInfoScreen(String sText) {
      this.DisableCallMethod("HideInfoScreen");
      this.m_cInfoScreenText.SetText(sText);
      this.m_cInfoScreenText.m_fBlinkTime = this.m_fInfoTime;
      this.m_cInfoScreenText.m_fBlinkHide = 0.15F;
      this.m_cInfoScreen.Show();
      this.m_cInfoScreenText.Show();
      this.m_cInfoScreenText.Fade();
      this.CallMethodIn("HideInfoScreen", this.m_fInfoTime);
   }

   void HideInfoScreen() {
      if (this.m_cInfoScreenText != null) {
         this.m_cInfoScreenText.EndFade();
         this.m_cInfoScreen.Hide();
      }

      this.DisableCallMethod("HideInfoScreen");
   }

   void OnUICommand(UIWindow cSender, int nCommand) {
      super.OnUICommand(cSender, nCommand);
      if (nCommand == 0 && cSender.m_sId != null) {
         if (cSender.m_sId.equals("ID_OK")) {
            this.OnOK();
         } else if (cSender.m_sId.equals("ID_CANCEL")) {
            this.OnBack();
         }
      }
   }
}
