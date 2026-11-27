public class MenuMain extends SoccerUserInterface {
   static final int DLG_LEAGUE = 2; // [mod] « continuer la ligue ? »
   static final int DLG_QUIT = 0;
   static final int DLG_CONTINUE_CH = 1;
   static final String MENU_CLOSE = "ID_QUIT";
   static final String MENU_MATCH = "ID_MATCH";
   static final String MENU_LOADLEVEL_DEMO = "ID_LOADLEVEL_DEMO";
   static final String MENU_CHAMPIONSHIPS = "ID_CHAMPIONSHIPS";
   static final String MENU_EXTRA = "ID_EXTRA";
   static final String MENU_OPTIONS = "ID_OPTIONS";
   static final String MENU_MULTIPLAYER = "ID_MULTIPLAYER";
   static final int MENU_CLOSE_ID = 0;
   static final int MENU_CHAMPIONSHIPS_ID = 1;
   static final int MENU_MATCH_ID = 2;
   static final int MENU_LOADLEVEL_DEMO_ID = 3;
   static final int MENU_EXTRA_ID = 4;
   static final int MENU_OPTIONS_ID = 5;
   static final int MENU_MULTIPLAYER_ID = 6;
   float m_fIdleTimeToDemo = 120.0F;
   UIMenu m_cMenu = null;
   UIStatic m_cElo = null;
   UIWindow m_cEloBadge = null;
   UITexture[] m_tEloBadges = null;
   float m_fShowTime = -1.0F; // [mod] heure du dernier affichage, -1 = menu caché
   static boolean m_bOnlineTestDone = false; // [mod]

   // [mod] en ligne : liaison au serveur, puis écran du salon (MenuOnline)
   void OnOnline() {
      CoreLogic.m_bMultiplayer = false;
      CoreLogic.m_bChampionships = false;
      CoreLogic.m_bSurvival = false;
      CoreLogic.m_bLeague = false;
      MenuBackground.SetPlayerType(0);
      ModLobby.Open();
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_ONLINE);
   }

   // [mod] match de méta (ModMeta)
   void MetaStart() {
      this.DisableCallMethod("StartDemo");
      ModMeta.Start();
   }

   // [mod] test du salon sans souris : NetAuto dans debugconf.scr (voir notes/online.md)
   void OnlineTest() {
      this.DisableCallMethod("StartDemo");
      this.OnOnline();
   }

   void OnMultiplayer() {
      CoreLogic.m_bMultiplayer = true;
      MenuBackground.SetPlayerType(-1);
      ((SoccerMainMenuModule)this.GetModule()).GetUI(16).Reset();
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(16);
   }

   void OnNoFromDlg(int nID) {
      super.OnNoFromDlg(nID);
      switch (nID) {
         case 1:
            this.OnStartNewChampionship();
            break;
         case DLG_LEAGUE:
            ModLeagueMode.Stop(); // [mod] nouvelle ligue : l'ancienne est abandonnée
            this.OnStartNewLeague();
            break;
         case 0:
      }
   }

   String SB() {
      return this.Start(1, 1);
   }

   void OnKeyPressed(int nKey, char cAscii) {
      this.DisableCallMethod("StartDemo");
      this.CallMethodIn("StartDemo", this.m_fIdleTimeToDemo);
   }

   MenuMain() {
      super.m_sFiles = new String[]{"data\\menu\\scr\\main640.scr", "data\\menu\\scr\\main.scr", "data\\menu\\scr\\main1024.scr"};
   }

   void OnUICommand(UIWindow cSender, int nCommand) {
      super.OnUICommand(cSender, nCommand);
      if (nCommand == 1) {
         GameObject.LogLine("-i-- Double Click -----" + cSender.m_sId);
      }

      // [mod] écrans Profil et Mods (boutons absents du jeu d'origine, donc hors TranslateIdToInt).
      // Au retour d'un match, le bouton reste parfois « enfoncé » (UIButton$LMBDown) et le premier relâchement
      // de la souris l'actionne : on ignore un appui qui arrive avant l'affichage du menu ou juste après.
      if (nCommand == 0
         && ("ID_PROFILE".equals(cSender.m_sId) || "ID_MODS".equals(cSender.m_sId) || "ID_SURVIVAL".equals(cSender.m_sId) || "ID_LEAGUE".equals(cSender.m_sId)
            || "ID_ONLINE".equals(cSender.m_sId))) {
         if (this.m_fShowTime >= 0.0F && this.GetTime() - this.m_fShowTime > 0.3F) {
            if ("ID_ONLINE".equals(cSender.m_sId)) {
               this.OnOnline();
            } else if ("ID_SURVIVAL".equals(cSender.m_sId)) {
               this.OnSurvival();
            } else if ("ID_LEAGUE".equals(cSender.m_sId)) {
               this.OnLeague();
            } else {
               ((SoccerMainMenuModule)this.GetModule()).ShowUI("ID_MODS".equals(cSender.m_sId) ? SoccerMainMenuModule._UI_MODS : SoccerMainMenuModule._UI_PROFILE);
            }
         }

         return;
      }

      if (nCommand == 0) {
         int nID = this.TranslateIdToInt(cSender.m_sId);
         if (this.GetModule() != null) {
            switch (nID) {
               case 0:
                  this.OnClose();
                  break;
               case 1:
                  this.OnChampionships();
                  break;
               case 2:
                  this.OnMatch();
                  break;
               case 3:
               default:
                  ((SoccerMenuModule)this.GetModule()).HandleUIClick(nID);
                  break;
               case 4:
                  this.OnExtra();
                  break;
               case 5:
                  this.OnOptions();
                  break;
               case 6:
                  this.OnMultiplayer();
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

   int TranslateIdToInt(String sId) {
      int nID = -1;
      if (sId.equals("ID_QUIT")) {
         nID = 0;
      }

      if (sId.equals("ID_MATCH")) {
         nID = 2;
      }

      if (sId.equals("ID_LOADLEVEL_DEMO")) {
         nID = 3;
      }

      if (sId.equals("ID_OPTIONS")) {
         nID = 5;
      }

      if (sId.equals("ID_CHAMPIONSHIPS")) {
         nID = 1;
      }

      if (sId.equals("ID_MULTIPLAYER")) {
         nID = 6;
      }

      if (sId.equals("ID_EXTRA")) {
         nID = 4;
      }

      return nID;
   }

   void OnUINotify(UIWindow cSender, int nEvent) {
      if (cSender instanceof UIMenuButtonFlying) {
         GameObject.LogLine("OnUINotify " + cSender + " event " + nEvent);
      }

      int nFocus = this.m_cMenu.FindFocus();
      if (nFocus != -1) {
         UIWindow cFocused = this.GetMember(nFocus);
         if (cFocused instanceof UIMenuButtonFlying) {
            GameObject.LogLine("Focused button " + cFocused);
         }
      }

      if (cSender instanceof UIMenuButtonFlying) {
         int nID = this.TranslateIdToInt(cSender.m_sId);
         if (nID != -1) {
            if (nEvent == 0) {
               ((SoccerMainMenuModule)this.GetModule()).SetObjectFocus(nID);
            } else {
               ((SoccerMainMenuModule)this.GetModule()).SetObjectFocus(-1);
            }
         }
      }
   }

   // [mod] survie : choix de l'équipe (et de sa tenue), puis écran Survie (MenuChooseTeamColors.OnOK)
   void OnSurvival() {
      CoreLogic.m_bMultiplayer = false;
      CoreLogic.m_bChampionships = false;
      CoreLogic.m_bSurvival = true;
      ModSurvival.Stop();
      MenuBackground.SetPlayerType(0);
      ((SoccerMainMenuModule)this.GetModule()).GetUI(2).Reset();
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(2);
   }

   // [mod] mode Ligue : reprendre la ligue en cours, ou choix de l'équipe (et de sa tenue) puis classement (MenuChooseTeamColors.OnOK)
   void OnLeague() {
      if (ModLeagueMode.IsInProgress()) {
         this.YesNoDialog(DLG_LEAGUE, Text.Get("&LeagueContinueTitle&"), Text.Get("&LeagueContinueQuestion&"), null, null);
      } else {
         this.OnStartNewLeague();
      }
   }

   void OnStartNewLeague() {
      CoreLogic.m_bMultiplayer = false;
      CoreLogic.m_bChampionships = false;
      CoreLogic.m_bSurvival = false;
      CoreLogic.m_bLeague = true;
      MenuBackground.SetPlayerType(0);
      ((SoccerMainMenuModule)this.GetModule()).GetUI(2).Reset();
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(2);
   }

   void OnContinueLeague() {
      CoreLogic.m_bMultiplayer = false;
      CoreLogic.m_bChampionships = false;
      CoreLogic.m_bSurvival = false;
      CoreLogic.m_bLeague = true;
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_LEAGUE);
   }

   void OnMatch() {
      CoreLogic.m_bMultiplayer = false;
      MenuBackground.SetPlayerType(0);
      ((SoccerMainMenuModule)this.GetModule()).GetUI(2).Reset();
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(2);
   }

   void OnYesFromDlg(int nID) {
      super.OnYesFromDlg(nID);
      switch (nID) {
         case 0:
            SoccerGame.SaveAttrib();
            GameObject.ExitGame();
            break;
         case 1:
            this.OnContinueChampionship();
            break;
         case DLG_LEAGUE:
            this.OnContinueLeague(); // [mod]
      }
   }

   void OnStartNewChampionship() {
      CoreLogic.m_bMultiplayer = false;
      CoreLogic.m_bChampionships = true;
      MenuBackground.SetPlayerType(0);
      ((SoccerMainMenuModule)this.GetModule()).GetUI(11).Reset();
      ((SoccerMainMenuModule)this.GetModule()).GetUI(2).Reset();
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(14);
   }

   void OnClose() {
      this.YesNoDialog(0, Text.Get("&MenuMainDlgQuitTitle&"), Text.Get("&MenuMainDlgQuitQuestion&"), null, null);
   }

   public void OnInit() {
      super.OnInit();
      this.AddToSlideIn((UIButton)this.GetElem("ID_MATCH"), 0.0F);
      this.AddToSlideIn((UIButton)this.GetElem("ID_CHAMPIONSHIPS"), 0.075F);
      this.AddToSlideIn((UIButton)this.GetElem("ID_LEAGUE"), 0.1125F); // [mod]
      this.AddToSlideIn((UIButton)this.GetElem("ID_SURVIVAL"), 0.15F); // [mod]
      this.AddToSlideIn((UIButton)this.GetElem("ID_EXTRA"), 0.225F);
      this.AddToSlideIn((UIButton)this.GetElem("ID_MULTIPLAYER"), 0.3F);
      this.AddToSlideIn((UIButton)this.GetElem("ID_ONLINE"), 0.3375F); // [mod]
      this.AddToSlideIn((UIButton)this.GetElem("ID_PROFILE"), 0.375F); // [mod]
      this.AddToSlideIn((UIButton)this.GetElem("ID_MODS"), 0.45F); // [mod]
      this.m_cMenu = (UIMenu)this.GetElem("ID_MENU");
      this.m_cElo = (UIStatic)this.GetElem("ID_ELO");
      UIStatic cVersion = (UIStatic)this.GetElem("ID_VERSION");
      if (cVersion != null) {
         cVersion.SetText(ModVersion.GetText());
      }
      this.m_cEloBadge = this.GetElem("ID_ELO_BADGE");
      if (this.m_cEloBadge != null) {
         this.m_tEloBadges = ModElo.LoadBadges(this.m_cEloBadge);
      }
   }

   // [mod] rang et Elo du profil courant (ID_ELO dans Main*.scr)
   void ShowElo() {
      if (this.m_cElo != null && CoreLogic.GetPlayer() != null) {
         this.m_cElo.SetText(ModElo.GetSummary());
         ModElo.SetRankColor(this.m_cElo);
         ModElo.SetBadge(this.m_cEloBadge, this.m_tEloBadges, ModElo.GetRank());
      } else {
         ModElo.SetBadge(this.m_cEloBadge, this.m_tEloBadges, -1);
      }
   }

   void OnContinueChampionship() {
      ((SoccerMainMenuModule)this.GetModule()).GetUI(11).Reset();
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(11);
   }

   void OnExtra() {
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(15);
   }

   public void Hide() {
      this.DisableCallMethod("StartDemo");
      this.m_fShowTime = -1.0F; // [mod] voir le bouton Profil dans OnUICommand
      super.Hide();
   }

   void StartDemo() {
      PlayerSave player = CoreLogic.GetPlayer();
      int nLastAvailable = -1;
      int[] nAvailableTeams = new int[12];

      for (int i = 0; i < 12; i++) {
         if (player.IsTeamAvailable(i)) {
            nAvailableTeams[++nLastAvailable] = i;
         }
      }

      int nTeamId = SoccerGame.GetRandom(0, nLastAvailable);
      int nTeam = nAvailableTeams[nTeamId];
      int nOpponentId = SoccerGame.GetRandom(0, nLastAvailable);
      if (nOpponentId == nTeamId) {
         nOpponentId = (nTeamId + 1) % nLastAvailable;
      }

      int nOpponent = nAvailableTeams[nOpponentId];
      CoreLogic.m_bDemo = true;
      CoreLogic.m_nNextLevel = ETeams.GetHomeField(nTeam);
      CoreLogic.m_cTeamDataLeft = ETeams.GetTeamData(nTeam);
      CoreLogic.m_cTeamDataRight = ETeams.GetTeamData(nOpponent);
      SoccerGame.game.GetCommentator().HideNow();
      SoccerGame.game.RunModule();
   }

   void OnOptions() {
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(5);
   }

   public void Show() {
      this.DisableCallMethod("StartDemo");
      this.CallMethodIn("StartDemo", this.m_fIdleTimeToDemo);
      CoreLogic.m_bChampionships = false;
      CoreLogic.m_bDemo = false;
      CoreLogic.m_bMultiplayer = false;
      CoreLogic.m_bSurvival = false; // [mod]
      CoreLogic.m_bLeague = false; // [mod]
      ModLobby.Close(); // [mod] en ligne : revenir au menu principal quitte le salon et le serveur
      super.Show();
      this.ShowElo();
      this.m_fShowTime = this.GetTime(); // [mod]
      // [mod] succès déjà atteints dans l'historique (joué avant le mod, fichier supprimé) : débloqués sans annonce
      ModAchievements.Evaluate(false);
      // [mod] test du tournoi sans souris : LeagueTest(n) dans debugconf.scr (voir notes/league.md)
      if (ModLeagueMode.GetDebugAutoStart() > 0) {
         this.CallMethodIn("LeagueTest", 1.0F);
      }

      // [mod] test de l'écran Tactique sans souris : TacticsTest(1) dans debugconf.scr (voir notes/tactics.md)
      if (ModTactics.GetDebugTest()) {
         this.CallMethodIn("TacticsTest", 1.0F);
      }

      // [mod] test de l'écran Contrôles sans souris : ControlsTest(1) dans debugconf.scr (voir notes/tactics.md)
      if (ModSwitch.GetDebugTest()) {
         this.CallMethodIn("ControlsTest", 1.0F);
      }

      // [mod] match de méta IA contre IA (ModMeta)
      if (ModMeta.ShouldStart()) {
         this.DisableCallMethod("StartDemo");
         this.CallMethodIn("MetaStart", 1.0F);
      }

      // [mod] test du salon sans souris : NetAuto dans debugconf.scr (une fois)
      if (ModLobby.m_nAuto >= 1 && ModLobby.m_nAuto <= 7 && ModLobby.m_nAuto != 3 && !m_bOnlineTestDone) {
         m_bOnlineTestDone = true;
         this.CallMethodIn("OnlineTest", 1.0F);
      }
   }

   // [mod] ouvre l'écran Contrôles (ControlsTest)
   void ControlsTest() {
      this.DisableCallMethod("StartDemo");
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(7);
   }

   // [mod] ouvre l'écran Tactique (équipe 1 par défaut si aucune n'est choisie)
   void TacticsTest() {
      this.DisableCallMethod("StartDemo");
      if (CoreLogic.m_cTeamDataLeft == null) {
         GameObject.CrashLog("[mod] TacticsTest : pas d'equipe choisie, composition par defaut\n");
      }

      ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_TACTICS);
   }

   // [mod] démarre une ligue (Real Pandrid), simule n matchs du joueur au hasard et ouvre le classement (ou le tableau)
   void LeagueTest() {
      int n = ModLeagueMode.GetDebugAutoStart();
      if (n <= 0 || CoreLogic.GetPlayer() == null) {
         return;
      }

      ModLeagueMode.m_nDebugAutoStart = 0;
      this.DisableCallMethod("StartDemo");
      ModLeagueMode.Start(12, 0);
      ModLeague l = ModLeagueMode.GetLeague();
      java.util.Random r = new java.util.Random();

      for (int i = 0; i < n && l.GetPlayerNext() != null; i++) {
         boolean bLeague = l.m_nPhase == ModLeague._PHASE_LEAGUE;
         int a = r.nextInt(4);
         int b = r.nextInt(3);
         if (!bLeague && a == b) {
            a++;
         }

         ModLeagueMode.m_nLastResult = l.Report(a, b);
         ModLeagueMode.m_bLastLeague = bLeague;
         ModLeagueMode.m_bLastGolden = false;
         ModLeagueMode.m_bHasLast = true;
      }

      ModLeagueMode.Save();
      GameObject.CrashLog("[mod] LeagueTest " + n + " : phase " + l.m_nPhase + ", journee " + l.m_nRound + ", jour " + l.m_nDay + "\n");
      this.OnContinueLeague();
   }

   public void OnCreate() {
      super.OnCreate();
   }

   void OnChampionships() {
      Championships ch = this.GetChampionships();
      if (ch.IsChampionshipInProgress()) {
         this.YesNoDialog(1, Text.Get("&MenuMainContinueChTitle&"), Text.Get("&MenuMainContinueChQuestion&"), null, null);
      } else {
         this.OnStartNewChampionship();
      }
   }

   Championships GetChampionships() {
      return CoreLogic.GetPlayer().m_cChampionships;
   }

   void OnMouseMove(float fMouseX, float fMouseY, int iButtonsFlags) {
      super.OnMouseMove(fMouseX, fMouseY, iButtonsFlags);
      this.DisableCallMethod("StartDemo");
      this.CallMethodIn("StartDemo", this.m_fIdleTimeToDemo);
   }

   String Start(int nAnimal1, int nAnimal2) {
      CoreLogic.m_bDemo = true;
      CoreLogic.m_nNextLevel = ETeams.GetHomeField(nAnimal1);
      CoreLogic.m_cTeamDataLeft = ETeams.GetTeamData(nAnimal1);
      CoreLogic.m_cTeamDataRight = ETeams.GetTeamData(nAnimal2);
      SoccerGame.game.GetCommentator().HideNow();
      SoccerGame.game.RunModule();
      return "<Graja " + CoreLogic.m_cTeamDataLeft.m_sNameAbr + " z " + CoreLogic.m_cTeamDataRight.m_sNameAbr + ">";
   }
}
