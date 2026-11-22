// [mod] Écran de la ligue (mode Ligue, ModLeagueMode / ModLeague) : classement à gauche, prochain match du joueur et
// résultats de la dernière journée à droite. Le retour de match ramène toujours ici (SoccerMainMenuModule.ShowLoadingScreen) ;
// pendant les playoffs, il passe la main au tableau (MenuLeagueBracket).
// Écran généré par mods/ui/make_league_scr.py, voir notes/league.md.
// Ligue en ligne (ModOnlineLeague.m_bView) : la ligue vient du serveur, l'écran se relit quand elle change, les équipes
// des autres joueurs sont en bleu, et « retour » ramène à l'écran de la ligue en ligne.
public class MenuLeague extends SoccerUserInterface {
   static final int _ROWS = 14;
   static final int _RESULTS = 7;
   static final String[] _COLS = new String[]{"POS", "TEAM", "P", "W", "D", "L", "GOALS", "DIFF", "PTS"};
   static final float[] _GOLD = new float[]{1.0F, 0.82F, 0.25F};
   static final float[] _GREEN = new float[]{0.4F, 1.0F, 0.4F};
   static final float[] _RED = new float[]{1.0F, 0.45F, 0.45F};
   static final float[] _GREY = new float[]{0.6F, 0.6F, 0.6F};
   static final float[] _WHITE = new float[]{1.0F, 1.0F, 1.0F};
   UIStatic m_cTitle = null;
   UIStatic m_cFrameTable = null;
   UIStatic m_cFrameMatch = null;
   UIStatic[][] m_tCells = new UIStatic[_ROWS][_COLS.length];
   UIStatic[] m_tResults = new UIStatic[_RESULTS];
   UIStatic m_cRoundTitle = null;
   UIStatic m_cMsg = null;
   UIStatic m_cName1 = null;
   UIStatic m_cName2 = null;
   UIStatic m_cVs = null;
   UIWindow m_cLogo1 = null;
   UIWindow m_cLogo2 = null;
   UIMenuButton m_cPlay = null;
   UIMenuButton m_cBack = null;
   UIStaticBlinking m_cInfoScreenText = null;
   UIWindow m_cInfoScreen = null;
   float m_fInfoTime = 5.0F;
   int m_nShownVersion = -1; // ligue en ligne : version de l'état affiché

   MenuLeague() {
      super.m_sFiles = new String[]{"data\\menu\\scr\\League640.scr", "data\\menu\\scr\\League.scr", "data\\menu\\scr\\League1024.scr"};
   }

   public void OnInit() {
      super.OnInit();
      this.m_cTitle = (UIStatic)this.GetElem("ID_TITLE");
      this.m_cFrameTable = (UIStatic)this.GetElem("ID_FRAME_TABLE");
      this.m_cFrameMatch = (UIStatic)this.GetElem("ID_FRAME_MATCH");

      for (int r = 0; r < _ROWS; r++) {
         for (int c = 0; c < _COLS.length; c++) {
            this.m_tCells[r][c] = (UIStatic)this.GetElem("ID_T" + r + "_" + _COLS[c]);
         }
      }

      for (int i = 0; i < _RESULTS; i++) {
         this.m_tResults[i] = (UIStatic)this.GetElem("ID_RES" + i);
      }

      this.m_cRoundTitle = (UIStatic)this.GetElem("ID_ROUND_TITLE");
      this.m_cMsg = (UIStatic)this.GetElem("ID_MSG");
      this.m_cName1 = (UIStatic)this.GetElem("ID_NAME_1");
      this.m_cName2 = (UIStatic)this.GetElem("ID_NAME_2");
      this.m_cVs = (UIStatic)this.GetElem("ID_VS");
      this.m_cLogo1 = this.GetElem("ID_LOGO_1");
      this.m_cLogo2 = this.GetElem("ID_LOGO_2");
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
      boolean bOnline = ModOnlineLeague.m_bView;
      ModLeague l = ModOnlineLeague.GetViewLeague();
      if (l == null) {
         this.GoToMainMenu();
         return;
      }

      if (bOnline) {
         ModOnlineLeague.m_bScreen = true; // on suit la ligue pendant qu'on la regarde
      } else {
         CoreLogic.m_bLeague = true;
      }

      // fin de la ligue annoncée ici (classement final), sinon les playoffs se suivent sur le tableau
      boolean bLeagueEnd = !bOnline && ModLeagueMode.m_bHasLast && ModLeagueMode.m_bLastLeague && l.m_nPhase != ModLeague._PHASE_LEAGUE;
      if (l.m_nPhase != ModLeague._PHASE_LEAGUE && !bLeagueEnd) {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_LEAGUE_BRACKET);
         return;
      }

      if (!bOnline) {
         ModLeagueMode.m_bHasLast = false;
      }

      this.Refresh(l);
      this.m_nShownVersion = ModOnlineLeague.m_nVersion;
      super.m_sTitle = "";
      this.CenterCursor();
      super.Show();
      this.DisableCallMethod("Tick");
      if (bOnline) {
         this.CallMethodEvery("Tick", 0.5F);
      }
      if (bLeagueEnd) {
         this.ShowInfoScreen(Text.Get(l.IsPlayerQualified() ? "&LeagueInfoQualified&" : "&LeagueInfoNotQualified&"));
      }
   }

   public void Hide() {
      this.DisableCallMethod("Tick");
      if (ModOnlineLeague.m_bView) {
         ModOnlineLeague.m_bScreen = false;
      }

      super.Hide();
   }

   // ligue en ligne : les autres joueurs jouent pendant qu'on regarde
   void Tick() {
      ModLeague l = ModOnlineLeague.GetViewLeague();
      if (l == null) {
         this.GoToMainMenu();
      } else if (l.m_nPhase != ModLeague._PHASE_LEAGUE) {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_LEAGUE_BRACKET);
      } else if (this.m_nShownVersion != ModOnlineLeague.m_nVersion) {
         this.m_nShownVersion = ModOnlineLeague.m_nVersion;
         this.Refresh(l);
      }
   }

   // couleur d'une équipe : moi, un autre joueur (ligue en ligne), ou sinon c
   static float[] TeamColor(ModLeague l, int nTeam, float[] c) {
      if (nTeam == l.m_nPlayer) {
         return _GOLD;
      }

      return l.IsOtherHuman(nTeam) ? MenuOnlineLeague._HUMAN : c;
   }

   static void SetColor(UIStatic s, float[] c) {
      s.SetTextColor(c[0], c[1], c[2]);
   }

   static String TeamName(int nAnimal) {
      return ETeams.GetTeamData(nAnimal).m_sNameAbr;
   }

   static void SetLogo(UIWindow cWnd, int nAnimal) {
      if (cWnd != null) {
         if (nAnimal < 0) {
            cWnd.SetTextureAlpha(0.0F);
         } else {
            cWnd.SetTexture(ETeams.GetTeamData(nAnimal).m_cLogo);
            cWnd.SetTextureAlpha(1.0F);
         }
      }
   }

   // « Bearchester 2 : 1 Bullrusia », noms raccourcis si la ligne est trop large
   static String MatchText(UIStatic s, int nHost, int nGuest, String sScore) {
      String sText = TeamName(nHost) + " " + sScore + " " + TeamName(nGuest);
      if (s.GetTextExtent(sText).fX >= s.GetSize().fX - 4.0F) {
         sText = ETeams.GetTeamData(nHost).m_sNameMin + " " + sScore + " " + ETeams.GetTeamData(nGuest).m_sNameMin;
      }

      return sText;
   }

   void Refresh(ModLeague l) {
      boolean bOver = l.m_nPhase != ModLeague._PHASE_LEAGUE;
      this.m_cTitle.SetText(Text.Get("&LeagueTitle&"));
      // classement
      int[][] st = l.GetStats();
      int[] tRank = l.GetRanking();
      int nQualifiers = bOver ? l.m_tSeed.length : ModLeague.ClampQualifiers(l.m_nQualifiers, l.m_nTeams);
      this.m_cFrameTable.SetText(
         bOver ? Text.Get("&LeagueFrameFinal&") : Text.Get("&LeagueFrameTable&") + " - " + Text.Get("&LeagueRound&") + " " + Math.min(l.m_nRound + 1, l.m_nRounds) + " / " + l.m_nRounds
      );

      for (int r = 0; r < _ROWS; r++) {
         UIStatic[] row = this.m_tCells[r];
         if (r >= tRank.length) {
            for (int c = 0; c < row.length; c++) {
               row[c].SetText("");
            }
         } else {
            int t = tRank[r];
            int[] s = st[t];
            row[0].SetText("" + (r + 1));
            row[1].SetText(TeamName(l.m_tTeam[t]));
            row[2].SetText("" + s[0]);
            row[3].SetText("" + s[1]);
            row[4].SetText("" + s[2]);
            row[5].SetText("" + s[3]);
            row[6].SetText(s[4] + ":" + s[5]);
            row[7].SetText((s[4] > s[5] ? "+" : "") + (s[4] - s[5]));
            row[8].SetText("" + s[6]);
            float[] c = TeamColor(l, t, r < nQualifiers ? _WHITE : _GREY);

            for (int k = 0; k < row.length; k++) {
               SetColor(row[k], c);
            }

            // la place qualificative se lit sur le numéro
            SetColor(row[0], TeamColor(l, t, r < nQualifiers ? _GREEN : _GREY));
         }
      }

      // prochain match du joueur
      int nMine = l.m_tTeam[l.m_nPlayer];
      int[] tNext = bOver ? null : l.GetPlayerNext();
      if (tNext != null) {
         int nHost = l.m_tTeam[tNext[0]];
         int nGuest = l.m_tTeam[tNext[1]];
         this.m_cFrameMatch.SetText(Text.Get("&LeagueFrameNext&"));
         this.m_cName1.SetText(TeamName(nHost));
         this.m_cName2.SetText(TeamName(nGuest));
         this.m_cVs.SetText(Text.Get("&SurvivalVs&"));
         SetLogo(this.m_cLogo1, nHost);
         SetLogo(this.m_cLogo2, nGuest);
      } else {
         this.m_cFrameMatch.SetText(Text.Get("&LeagueFrameOver&"));
         this.m_cName1.SetText(TeamName(nMine));
         this.m_cName2.SetText("");
         this.m_cVs.SetText("");
         SetLogo(this.m_cLogo1, nMine);
         SetLogo(this.m_cLogo2, -1);
      }

      // résultats de la dernière journée jouée
      int nRound = bOver ? l.m_nRounds - 1 : l.m_nRound - 1;
      int k = 0;
      if (nRound >= 0) {
         this.m_cRoundTitle.SetText(Text.Get("&LeagueRound&") + " " + (nRound + 1) + " : " + Text.Get("&LeagueResults&"));

         for (int f = 0; f < l.m_nFixtures && k < _RESULTS; f++) {
            if (l.m_tFRound[f] == nRound && l.m_tFPlayed[f]) {
               UIStatic s = this.m_tResults[k++];
               s.SetText(MatchText(s, l.m_tTeam[l.m_tFHost[f]], l.m_tTeam[l.m_tFGuest[f]], l.m_tFGH[f] + " : " + l.m_tFGG[f]));
               SetColor(s, l.m_tFHost[f] == l.m_nPlayer || l.m_tFGuest[f] == l.m_nPlayer ? _GOLD
                  : (l.IsOtherHuman(l.m_tFHost[f]) || l.IsOtherHuman(l.m_tFGuest[f]) ? MenuOnlineLeague._HUMAN : _WHITE));
            }
         }
      } else {
         // avant la 1re journée : le règlement en quelques lignes
         this.m_cRoundTitle.SetText(Text.Get("&LeagueIntroTitle&"));
         String[] tIntro = new String[]{
            Text.Get("&LeagueIntro1&"), Text.Get("&LeagueIntro2&") + " " + nQualifiers + " " + Text.Get("&LeagueIntro3&"), Text.Get("&LeagueIntro4&"), Text.Get("&LeagueIntro5&")
         };

         for (int i = 0; i < tIntro.length && k < _RESULTS; i++) {
            this.m_tResults[k].SetText(tIntro[i]);
            SetColor(this.m_tResults[k++], _WHITE);
         }
      }

      while (k < _RESULTS) {
         this.m_tResults[k++].SetText("");
      }

      // message : place du joueur
      int nPlace = l.GetRank(l.m_nPlayer) + 1;
      if (!bOver) {
         this.m_cMsg.SetText(Text.Get("&LeaguePlace&") + " : " + nPlace + " / " + l.m_nTeams);
         SetColor(this.m_cMsg, nPlace <= nQualifiers ? _GREEN : _WHITE);
         this.m_cPlay.SetText(Text.Get("&LeaguePlay&"));
      } else if (l.IsPlayerQualified()) {
         this.m_cMsg.SetText(Text.Get("&LeagueQualified&") + " (" + nPlace + ")");
         SetColor(this.m_cMsg, _GREEN);
         this.m_cPlay.SetText(Text.Get("&LeaguePlayoffs&"));
      } else {
         this.m_cMsg.SetText(Text.Get("&LeagueNotQualified&") + " (" + nPlace + ")");
         SetColor(this.m_cMsg, _RED);
         this.m_cPlay.SetText(Text.Get("&LeaguePlayoffs&"));
      }

      this.m_cBack.SetText(Text.Get("&MenuBack&"));
      // ligue en ligne : les matchs se lancent depuis l'écran de la ligue en ligne (phases 3 et 4)
      if (ModOnlineLeague.m_bView) {
         this.m_cPlay.Hide();
      } else {
         this.m_cPlay.Show();
      }
   }

   void OnOK() {
      this.HideInfoScreen();
      if (ModOnlineLeague.m_bView) {
         return;
      }

      ModLeague l = ModLeagueMode.GetLeague();
      if (l == null) {
         this.GoToMainMenu();
      } else if (l.m_nPhase != ModLeague._PHASE_LEAGUE) {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_LEAGUE_BRACKET);
      } else if (ModLeagueMode.PrepareMatch()) {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(9);
      }
   }

   // la ligue reste en cours : « ligue » au menu principal propose de la reprendre
   void OnBack() {
      this.HideInfoScreen();
      this.GoToMainMenu();
   }

   void GoToMainMenu() {
      if (ModOnlineLeague.m_bView) {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_ONLINE_LEAGUE); // ligue en ligne
         return;
      }

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
