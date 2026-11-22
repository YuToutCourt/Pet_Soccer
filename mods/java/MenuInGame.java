public class MenuInGame extends SoccerUserInterface {
   MenuYesNoDialog m_cYesNo = null;
   boolean m_bMatchEnd = false;
   boolean m_bMatchHalfTime = false;
   UIStatic m_cResult = null;
   UIStatic m_cTeam1 = null;
   UIStatic m_cTeam2 = null;
   // [mod] tableau des statistiques et buteurs, une static par ligne (mods/ui/make_stats_scr.py) :
   // [ligne][0 = hôte, 1 = invité, 2 = libellé]
   UIStatic[][] m_tStats = new UIStatic[ModStats._STAT_ROWS][3];
   UIStatic[][] m_tScorers = new UIStatic[ModStats._MAX_SCORER_LINES][2];
   UIStatic m_cHalf = null;
   UIButton m_cExit = null;
   // [mod] panneau Elo de fin de match (ID_ELO_* dans InGame*.scr)
   UIStatic m_cEloRank = null;
   UIStatic m_cEloValue = null;
   UIStatic m_cEloDelta = null;
   UIWindow m_cEloBadge = null;
   UITexture[] m_tEloBadges = null;
   float m_fEloShown = 0.0F;
   float m_fEloStep = 0.0F;
   // [mod] bandeau « succès débloqué » de fin de match (ID_ACH_* dans InGame*.scr)
   UIStatic m_cAchFrame = null;
   UIStatic m_cAchName = null;
   UIStatic m_cAchDesc = null;
   int m_nAchIndex = 0;
   SoccerModule m_cGameModule = null;
   static final int ID_QUIT = 0;
   static final int ID_CONTINUE = 1;
   static final int ID_RE_MATCH = 2;
   static final int YESNO_QUIT = 0;
   static final int YESNO_RESTART = 1;

   MenuInGame() {
      super.m_sFiles = new String[]{"data/menu/scr/InGame640.scr", "data/menu/scr/InGame.scr", "data/menu/scr/InGame1024.scr"};
   }

   void OnUICommand(UIWindow cSender, int nCommand) {
      super.OnUICommand(cSender, nCommand);
      if (nCommand == 0) {
         int nID = -1;
         if (cSender.m_sId.equals("ID_QUIT")) {
            nID = 0;
         }

         if (cSender.m_sId.equals("ID_CONTINUE")) {
            nID = 1;
         }

         if (cSender.m_sId.equals("ID_RE_MATCH")) {
            nID = 2;
         }

         // [mod] écran Tactique (sinon l'id inconnu partirait dans HandleUIClick comme « Continuer »)
         if (cSender.m_sId.equals("ID_TACTICS")) {
            ((SoccerInGameMenuModule)this.GetModule()).ShowTactics();
            return;
         }

         if (this.m_cGameModule != null) {
            switch (nID) {
               case 0:
                  this.OnQuit();
                  break;
               case 1:
               default:
                  this.m_cGameModule.HandleUIClick(nID);
                  break;
               case 2:
                  this.OnRestarMatch();
            }
         }
      }
   }

   void OnQuit() {
      if (!this.m_bMatchEnd) {
         this.YesNoDialog(0, Text.Get("&InGameYNQuitTitle&"), Text.Get("&InGameYNQuitQuestion&"), null, null);
      } else {
         this.Quit();
      }
   }

   void OnRestarMatch() {
      this.YesNoDialog(1, Text.Get("&InGameYNRestartTitle&"), Text.Get("&InGameYNRestartQuestion&"), null, null);
   }

   void OnYesFromDlg(int nID) {
      super.OnYesFromDlg(nID);
      switch (nID) {
         case 0:
            this.Quit();
            break;
         case 1:
            if (!this.m_bMatchEnd) {
               // [mod] recommencer un match en cours = abandon pour l'Elo et l'historique
               ModElo.OnAbandon();
               ModStats.OnAbandon();
               ModSurvival.OnAbandon(); // [mod] la série de survie s'arrête
            }

            this.m_cGameModule.OnMenuRestartMatch();
      }
   }

   public void OnInit() {
      this.m_cGameModule = SoccerGame.activeLevelModule;
      super.OnInit();
      this.m_cResult = (UIStatic)this.GetElem("ID_RESULT");
      this.m_cTeam1 = (UIStatic)this.GetElem("ID_TEAM_1");
      this.m_cTeam2 = (UIStatic)this.GetElem("ID_TEAM_2");
      String[] tLabels = ModStats.SplitLines(Text.Get("&InGameInfo&"), ModStats._STAT_ROWS);

      for (int i = 0; i < ModStats._STAT_ROWS; i++) {
         this.m_tStats[i][0] = (UIStatic)this.GetElem("ID_STAT" + i + "_1");
         this.m_tStats[i][1] = (UIStatic)this.GetElem("ID_STAT" + i + "_2");
         this.m_tStats[i][2] = (UIStatic)this.GetElem("ID_STAT" + i + "_LABEL");
         this.m_tStats[i][2].SetText(tLabels[i]);
      }

      for (int i = 0; i < ModStats._MAX_SCORER_LINES; i++) {
         this.m_tScorers[i][0] = (UIStatic)this.GetElem("ID_SCORER" + i + "_1");
         this.m_tScorers[i][1] = (UIStatic)this.GetElem("ID_SCORER" + i + "_2");
      }

      this.m_cHalf = (UIStatic)this.GetElem("ID_HALF");
      this.m_cExit = (UIButton)this.GetElem("ID_QUIT");
      this.m_cEloRank = (UIStatic)this.GetElem("ID_ELO_RANK");
      this.m_cEloValue = (UIStatic)this.GetElem("ID_ELO_VALUE");
      this.m_cEloDelta = (UIStatic)this.GetElem("ID_ELO_DELTA");
      this.m_cEloBadge = this.GetElem("ID_ELO_BADGE");
      if (this.m_cEloBadge != null) {
         this.m_tEloBadges = ModElo.LoadBadges(this.m_cEloBadge);
      }

      this.m_cAchFrame = (UIStatic)this.GetElem("ID_ACH_FRAME");
      this.m_cAchName = (UIStatic)this.GetElem("ID_ACH_NAME");
      this.m_cAchDesc = (UIStatic)this.GetElem("ID_ACH_DESC");
      this.AddToSlideIn(this.GetElem("ID_CONTINUE"), 0.0F);
      this.AddToSlideIn(this.GetElem("ID_TACTICS"), 0.05F); // [mod]
      this.AddToSlideIn(this.GetElem("ID_RE_MATCH"), 0.1F);
      this.AddToSlideIn(this.GetElem("ID_QUIT"), 0.15F);
   }

   void Quit() {
      SoccerGame.GetCommModule().HideReplaySign();
      this.GetModule().Deactivate();
      ModLobby.OnMatchQuit(this.m_bMatchEnd); // [mod] partie classée : fin du match ou forfait, dit au serveur
      ModNet.OnQuit(); // [mod] en ligne : l'autre joueur est prévenu
      ModNet.OnSpeedUp(1.0F);
      this.m_cGameModule.TimerSetSpeedUp(1.0F);
      this.m_cGameModule.TimerStart();
      if (!this.m_bMatchEnd) {
         // [mod] quitter un match en cours = abandon pour l'Elo et l'historique
         ModElo.OnAbandon();
         ModStats.OnAbandon();
         ModSurvival.OnAbandon(); // [mod] la série de survie s'arrête
      }

      if (!CoreLogic.m_bChampionships && !CoreLogic.m_bMultiplayer) {
         Match.m_cMatchData = null;
      } else if (!this.m_bMatchEnd) {
         Match.m_cMatchData = null;
      }

      SoccerGame.game.ExitToMenu();
   }

   void ShowInfo() {
      AIMatch cMatch = SoccerModule.m_cMatch;
      SoccerModule mod = this.m_cGameModule;
      String sResult = new String(SoccerModule.m_cMatch.m_nScoreLeft + " : " + SoccerModule.m_cMatch.m_nScoreRight);
      this.m_cResult.SetText(sResult);
      if (CoreLogic.m_cTeamDataLeft != null) {
         this.m_cTeam1.SetText(CoreLogic.m_cTeamDataLeft.m_sNameAbr); // [mod] nom court (colonne du tableau)
      } else {
         this.m_cTeam1.SetText("zespół downa");
      }

      if (CoreLogic.m_cTeamDataRight != null) {
         this.m_cTeam2.SetText(CoreLogic.m_cTeamDataRight.m_sNameAbr); // [mod]
      } else {
         this.m_cTeam2.SetText("team deathmatch");
      }

      int nPlayer1Team = 0;
      int nPlayer2Team = 1;
      byte var9;
      byte var10;
      if (cMatch.m_nHalfNo != 0 && !SoccerModule.m_cMatch.m_bGoldGoal) {
         var9 = 1;
         var10 = 0;
      } else {
         var9 = 0;
         var10 = 1;
      }

      int nBallPossesion = (int)SoccerModule.m_cMatch.GetEntirePossesion(var9);
      // [mod] tirs, possession, passes réussies, ballons récupérés, arrêts, supers ; buteurs
      String[] tHost = ModStats.GetValues(cMatch, var9, nBallPossesion);
      if (!SoccerModule.m_cMatch.m_bBallPossesToCount) {
         nBallPossesion = 0;
      } else {
         nBallPossesion = 100 - nBallPossesion;
      }

      String[] tGuest = ModStats.GetValues(cMatch, var10, nBallPossesion);

      for (int i = 0; i < ModStats._STAT_ROWS; i++) {
         this.m_tStats[i][0].SetText(tHost[i]);
         this.m_tStats[i][1].SetText(tGuest[i]);
      }

      String[] tHostScorers = ModStats.GetScorers(cMatch.GetTeam(var9));
      String[] tGuestScorers = ModStats.GetScorers(cMatch.GetTeam(var10));
      // [mod] en ligne : les statistiques sont celles de l'hôte, qui les envoie au client
      ModNet.HostStats(tHost, tGuest, tHostScorers, tGuestScorers);
      String[][] tNet = new String[][]{ModNet.ClientStats(0), ModNet.ClientStats(1), ModNet.ClientStats(2), ModNet.ClientStats(3)};
      if (ModNet.IsClient()) {
         for (int i = 0; i < ModStats._STAT_ROWS; i++) {
            this.m_tStats[i][0].SetText(tNet[0] != null && i < tNet[0].length ? tNet[0][i] : "");
            this.m_tStats[i][1].SetText(tNet[1] != null && i < tNet[1].length ? tNet[1][i] : "");
         }

         tHostScorers = new String[ModStats._MAX_SCORER_LINES];
         tGuestScorers = new String[ModStats._MAX_SCORER_LINES];

         for (int i = 0; i < ModStats._MAX_SCORER_LINES; i++) {
            tHostScorers[i] = tNet[2] != null && i < tNet[2].length ? tNet[2][i] : "";
            tGuestScorers[i] = tNet[3] != null && i < tNet[3].length ? tNet[3][i] : "";
         }
      }

      for (int i = 0; i < ModStats._MAX_SCORER_LINES; i++) {
         this.m_tScorers[i][0].SetText(tHostScorers[i]);
         this.m_tScorers[i][1].SetText(tGuestScorers[i]);
      }

      if (SoccerModule.m_cMatch.m_bGoldGoal) {
         this.m_cHalf.SetText(Text.Get("&InGameGoldenShot&"));
      } else if (this.m_bMatchHalfTime) {
         this.m_cHalf.SetText(Text.Get("&InGameAfterHalfOne&"));
      } else if (this.m_bMatchEnd) {
         this.m_cHalf.SetText(Text.Get("&InGameAfterMatchEnd&"));
      } else if (cMatch.m_nHalfNo == 0) {
         this.m_cHalf.SetText(Text.Get("&InGameHalfOne&"));
      } else {
         this.m_cHalf.SetText(Text.Get("&InGameHalfTwo&"));
      }

      // [mod] en ligne : attente de l'adversaire, adversaire ou hôte parti
      String sNetMessage = ModNet.MenuMessage();
      if (sNetMessage != null && !this.m_bMatchEnd) {
         this.m_cHalf.SetText(Text.Get(sNetMessage));
      }
   }

   public void Show() {
      Commentator comm = SoccerGame.game.GetCommentator();
      if (!ModNet.m_bLocalMenu) { // [mod] en ligne : le match continue derrière un menu local, le commentaire aussi
         comm.ClearCurrComment();
         comm.StopTalking();
      }
      if (this.m_bMatchEnd) {
         this.m_cExit.SetText(Text.Get("&InGameOk&"));
      } else {
         this.m_cExit.SetText(Text.Get("&InGameQuit&"));
      }

      if (this.m_bMatchHalfTime && !ModNet.IsClient()) { // [mod] en ligne : le client entend le commentaire de l'hôte
         comm.SayRandom(2);
      }

      // [mod] en ligne, chez le client : « continuer » demande à l'hôte (inutile s'il est parti),
      // « recommencer » est réservé à l'hôte ; « tactique » (à la mi-temps) demande un salon, par où la
      // composition part chez l'hôte
      boolean bClient = ModNet.IsClient();
      boolean bClientTactics = bClient && ModLobby.IsOpen() && !ModNet.m_bHostLost;
      ((UIButton)this.GetElem("ID_CONTINUE")).SetDisabled(this.m_bMatchEnd || bClient && ModNet.m_bHostLost || this.IsHalfBreakDone());
      // [mod] survie : pas de « recommencer », qui permettrait de rejouer une manche perdue
      // [mod] ligue : idem, le résultat est déjà enregistré à la fin du match
      // [mod] partie classée : idem, le match compte pour l'Elo en ligne
      ((UIButton)this.GetElem("ID_RE_MATCH")).SetDisabled(CoreLogic.m_bSurvival || CoreLogic.m_bLeague || bClient || ModLobby.IsRankedMatch());
      // [mod] tactique modifiable seulement à la mi-temps (et à l'avant-match), pas pendant le jeu
      ((UIButton)this.GetElem("ID_TACTICS")).SetDisabled(!this.m_bMatchHalfTime || this.m_bMatchEnd || CoreLogic.m_bDemo || bClient && !bClientTactics);
      this.ShowInfo();
      this.StartElo();
      this.ShowHalfBreak(); // [mod] en ligne : pause de mi-temps
      this.CenterCursor();
      if (!CoreLogic.m_bDemo) {
         super.Show();
      } else {
         if (!this.m_bMatchEnd && this.m_bMatchHalfTime) {
            this.m_cGameModule.HandleUIClick(1);
         }

         if (this.m_bMatchEnd) {
            this.Quit();
            ((MenuMain)SoccerGame.firstMenuModule.GetUI(0)).CallMethodIn("StartDemo", 20.0F);
         }

         if (!this.m_bMatchEnd && !this.m_bMatchHalfTime) {
            this.Quit();
         }
      }

      this.StartAchievements(); // [mod] après super.Show, qui réafficherait le bandeau
   }

   // [mod] en ligne, pause de mi-temps : j'ai déjà cliqué « continuer », le match reprendra quand l'autre joueur
   // l'aura fait aussi, ou à la fin du temps
   boolean IsHalfBreakDone() {
      return this.m_bMatchHalfTime && !this.m_bMatchEnd && ModNet.HalfBreakLeft() >= 0 && ModNet.HalfBreakMine();
   }

   // [mod] en ligne, pause de mi-temps : temps avant la reprise et qui est prêt, dans le panneau de l'Elo (vide à
   // la mi-temps). Appelé par ModNet.HalfBreakShow chaque fois que cela change.
   void ShowHalfBreak() {
      if (!this.m_bMatchHalfTime || this.m_bMatchEnd || this.m_cEloRank == null || this.m_cEloValue == null || this.m_cEloDelta == null) {
         return;
      }

      int nLeft = ModNet.HalfBreakLeft();
      if (nLeft < 0) {
         return;
      }

      boolean bMine = ModNet.HalfBreakMine();
      boolean bTheirs = ModNet.HalfBreakTheirs();
      ModElo.SetBadge(this.m_cEloBadge, this.m_tEloBadges, -1);
      this.m_cEloRank.SetText(Text.Get("&OnlineHalfResume&"));
      this.m_cEloRank.SetTextColor(1.0F, 0.82F, 0.25F);
      this.m_cEloValue.SetText(nLeft + " s");
      this.m_cEloValue.SetTextColor(1.0F, 1.0F, 1.0F);
      if (bMine) {
         this.m_cEloDelta.SetText(Text.Get("&OnlineWaiting&"));
         this.m_cEloDelta.SetTextColor(0.85F, 0.85F, 0.85F);
      } else if (bTheirs) {
         this.m_cEloDelta.SetText(Text.Get("&OnlineHalfPeerReady&"));
         this.m_cEloDelta.SetTextColor(0.4F, 1.0F, 0.4F);
      } else {
         this.m_cEloDelta.SetText(Text.Get("&OnlineHalfHint&"));
         this.m_cEloDelta.SetTextColor(0.85F, 0.85F, 0.85F);
      }

      ((UIButton)this.GetElem("ID_CONTINUE")).SetDisabled(bMine);
   }

   // [mod] succès débloqués par le match : le bandeau apparaît avec un son, puis passe au suivant toutes les 3 s
   void StartAchievements() {
      if (this.m_cAchFrame == null || this.m_cAchName == null || this.m_cAchDesc == null) {
         return;
      }

      this.DisableCallMethod("AchievementShow");
      this.DisableCallMethod("AchievementNext");
      this.m_cAchFrame.Hide();
      if (this.m_bMatchEnd && !CoreLogic.m_bDemo && !ModNet.IsOnline() && ModAchievements.m_vLastUnlocked.size() > 0) { // [mod] pas de succès en ligne
         this.m_nAchIndex = -1;
         this.CallMethodIn("AchievementShow", 0.8F);
      }
   }

   void AchievementShow() {
      this.AchievementNext();
      this.m_cAchFrame.Show();
      String sSound = ModAchievements.GetSound();
      if (sSound != null && sSound.length() > 0) {
         this.PlaySound(sSound);
      }

      if (ModAchievements.m_vLastUnlocked.size() > 1) {
         this.CallMethodEvery("AchievementNext", 3.0F);
      }
   }

   void AchievementNext() {
      java.util.Vector v = ModAchievements.m_vLastUnlocked;
      if (v.size() == 0) {
         return;
      }

      this.m_nAchIndex = (this.m_nAchIndex + 1) % v.size();
      ModAchievements.Achievement a = (ModAchievements.Achievement)v.elementAt(this.m_nAchIndex);
      this.m_cAchFrame.SetText(Text.Get("&AchUnlocked&") + (v.size() > 1 ? "  " + (this.m_nAchIndex + 1) + "/" + v.size() : ""));
      this.m_cAchName.SetText(ModAchievements.GetName(a));
      this.m_cAchName.SetTextColor(1.0F, 0.82F, 0.25F);
      this.m_cAchDesc.SetText(ModAchievements.GetDescription(a));
   }

   // [mod] survie : manche réussie (et niveau de la suivante) ou fin de série (bilan et record)
   void ShowSurvival() {
      ModElo.SetBadge(this.m_cEloBadge, this.m_tEloBadges, -1);
      this.m_cEloDelta.SetText("");
      if (ModSurvival.m_bLastCleared) {
         this.m_cEloRank.SetText(Text.Get("&SurvivalWaveWord&") + " " + ModSurvival.m_nLastWave + " " + Text.Get("&SurvivalClearedWord&"));
         this.m_cEloRank.SetTextColor(0.4F, 1.0F, 0.4F);
         // grande ligne courte (FLRoman56) : la manche suivante ; petite ligne : le niveau de l'IA
         this.m_cEloValue.SetText(Text.Get("&SurvivalWaveWord&") + " " + ModSurvival.GetWave());
         this.m_cEloValue.SetTextColor(1.0F, 1.0F, 1.0F);
         this.m_cEloDelta.SetText(Text.Get("&SurvivalNextAI&") + " " + ModSurvival.GetLevelText(ModSurvival.GetWave()));
         this.m_cEloDelta.SetTextColor(0.85F, 0.85F, 0.85F);
      } else {
         this.m_cEloRank.SetText(Text.Get("&SurvivalOver&"));
         this.m_cEloRank.SetTextColor(1.0F, 0.45F, 0.45F);
         int nCleared = ModSurvival.GetCleared();
         this.m_cEloValue.SetText(nCleared + " " + Text.Get(nCleared > 1 ? "&SurvivalWaves&" : "&SurvivalWave&"));
         this.m_cEloValue.SetTextColor(1.0F, 1.0F, 1.0F);
         if (ModSurvival.IsNewRecord()) {
            this.m_cEloDelta.SetText(Text.Get("&SurvivalNewRecord&"));
            this.m_cEloDelta.SetTextColor(1.0F, 0.82F, 0.25F);
         } else {
            this.m_cEloDelta.SetText(Text.Get("&SurvivalRecord&") + " : " + ModSurvival.GetRecord());
            this.m_cEloDelta.SetTextColor(0.8F, 0.8F, 0.8F);
         }
      }

      if (ModElo.m_bLastCheat && ModSurvival.m_bLastCleared) {
         this.m_cEloDelta.SetText(Text.Get("&ModsUnrated&"));
         this.m_cEloDelta.SetTextColor(0.8F, 0.8F, 0.8F);
      }
   }

   // [mod] Elo : ancien rang, puis le nombre défile jusqu'à la nouvelle valeur (EloTick)
   void StartElo() {
      if (this.m_cEloRank == null || this.m_cEloValue == null || this.m_cEloDelta == null) {
         return;
      }

      this.DisableCallMethod("EloTick");
      this.DisableCallMethod("OnlineEloTick");
      if (ModLobby.IsRankedMatch()) {
         // [mod] partie classée : l'Elo en ligne, dès que le serveur a compté le match
         this.m_nOnlineEloShown = -1;
         this.OnlineEloTick();
         this.CallMethodEvery("OnlineEloTick", 0.25F);
         return;
      }

      if (this.m_bMatchEnd && CoreLogic.m_bSurvival && ModSurvival.m_bHasLast && !ModElo.m_bLastRated) {
         this.ShowSurvival(); // [mod] survie (non classée) : la manche à la place de l'Elo
         return;
      }

      if (!this.m_bMatchEnd || !ModElo.m_bLastRated || ModNet.IsOnline()) { // [mod] pas d'Elo contre l'IA en ligne
         this.m_cEloRank.SetText("");
         if (this.m_bMatchEnd && ModElo.m_bLastCheat) {
            // [mod] triche du menu Mods : le match ne compte pas
            this.m_cEloRank.SetText(Text.Get("&ModsUnrated&"));
            this.m_cEloRank.SetTextColor(0.8F, 0.8F, 0.8F);
         }

         this.m_cEloValue.SetText("");
         this.m_cEloDelta.SetText("");
         ModElo.SetBadge(this.m_cEloBadge, this.m_tEloBadges, -1);
         return;
      }

      this.m_fEloShown = ModElo.m_nLastOldElo;
      // 1,5 s quel que soit l'écart (75 pas de 0,02 s, l'affichage est arrondi)
      this.m_fEloStep = Math.abs(ModElo.m_nLastDelta) / 75.0F;
      this.m_cEloRank.SetText(ModElo.GetRankName(ModElo.m_nLastOldRank));
      ModElo.SetRankColor(this.m_cEloRank, ModElo.m_nLastOldRank);
      ModElo.SetBadge(this.m_cEloBadge, this.m_tEloBadges, ModElo.m_nLastOldRank);
      this.m_cEloValue.SetText(ModElo.m_nLastOldElo + " Elo");
      ModElo.SetRankColor(this.m_cEloValue, ModElo.m_nLastOldRank);
      this.m_cEloDelta.SetText(ModElo.FormatLastDelta());
      if (ModElo.m_nLastDelta > 0) {
         this.m_cEloDelta.SetTextColor(0.3F, 1.0F, 0.3F);
      } else if (ModElo.m_nLastDelta < 0) {
         this.m_cEloDelta.SetTextColor(1.0F, 0.3F, 0.3F);
      } else {
         this.m_cEloDelta.SetTextColor(0.8F, 0.8F, 0.8F);
      }

      // on laisse d'abord l'écran apparaître
      this.CallMethodIn("EloStartCount", 0.8F);
   }

   // [mod] partie classée : le panneau Elo montre l'Elo en ligne. Le serveur compte le match quand les deux jeux
   // lui ont annoncé la fin (ou un forfait) : sa réponse arrive un instant après l'ouverture du menu.
   int m_nOnlineEloShown = -1;

   void OnlineEloTick() {
      if (this.m_cEloRank == null || this.m_cEloValue == null || this.m_cEloDelta == null) {
         return;
      }

      int nResult = ModLobby.MatchResult();
      if (nResult == this.m_nOnlineEloShown || this.m_bMatchHalfTime && ModNet.HalfBreakLeft() >= 0) { // pause de mi-temps : le panneau est pris
         return;
      }

      this.m_nOnlineEloShown = nResult;
      this.m_cEloValue.SetText("");
      this.m_cEloDelta.SetText("");
      ModElo.SetBadge(this.m_cEloBadge, this.m_tEloBadges, -1);
      if (nResult == ModLobby._RESULT_RATED) {
         int nRank = ModElo.GetRankIndex(ModLobby.m_nMyElo);
         this.m_cEloRank.SetText(ModElo.GetRankName(nRank));
         ModElo.SetRankColor(this.m_cEloRank, nRank);
         ModElo.SetBadge(this.m_cEloBadge, this.m_tEloBadges, nRank);
         this.m_cEloValue.SetText(ModLobby.m_nMyElo + " Elo");
         ModElo.SetRankColor(this.m_cEloValue, nRank);
         // la grande ligne ne tient que le nom du rang : le forfait se dit sur la petite
         this.m_cEloDelta.SetText(ModLobby.FormatDelta(ModLobby.m_nResDelta) + (ModLobby.m_nResHow == ModLobby._HOW_PLAYED ? "" : "  " + Text.Get("&OnlineForfeit&")));
         ModLobby.SetResultColor(this.m_cEloDelta);
      } else if (nResult == ModLobby._RESULT_VOID) {
         this.m_cEloRank.SetText(Text.Get("&OnlineResVoid&"));
         this.m_cEloRank.SetTextColor(0.8F, 0.8F, 0.8F);
      } else {
         this.m_cEloRank.SetText(this.m_bMatchEnd ? Text.Get("&OnlineRankedRoom&") : "");
         this.m_cEloRank.SetTextColor(0.8F, 0.8F, 0.8F);
      }

      if (nResult != ModLobby._RESULT_NONE) {
         this.DisableCallMethod("OnlineEloTick");
      }
   }

   void EloStartCount() {
      this.CallMethodEvery("EloTick", 0.02F);
   }

   void EloTick() {
      int nTarget = ModElo.GetLastNewElo();
      if (this.m_fEloShown < nTarget) {
         this.m_fEloShown = Math.min(nTarget, this.m_fEloShown + this.m_fEloStep);
      } else {
         this.m_fEloShown = Math.max(nTarget, this.m_fEloShown - this.m_fEloStep);
      }

      int nShown = Math.round(this.m_fEloShown);
      int nRank = ModElo.GetRankIndex(nShown);
      this.m_cEloValue.SetText(nShown + " Elo");
      ModElo.SetRankColor(this.m_cEloValue, nRank);
      this.m_cEloRank.SetText(ModElo.GetRankName(nRank));
      ModElo.SetRankColor(this.m_cEloRank, nRank);
      ModElo.SetBadge(this.m_cEloBadge, this.m_tEloBadges, nRank);
      if (nShown == nTarget) {
         this.DisableCallMethod("EloTick");
         if (nRank > ModElo.m_nLastOldRank) {
            this.m_cEloRank.SetText(Text.Get("&EloPromoted&") + " " + ModElo.GetRankName(nRank));
         } else if (nRank < ModElo.m_nLastOldRank) {
            this.m_cEloRank.SetText(Text.Get("&EloDemoted&") + " " + ModElo.GetRankName(nRank));
         }
      }
   }

   public void OnCreate() {
      SoccerGame.IncInGame();
      super.OnCreate();
   }

   public void OnDestroy() {
      this.m_cGameModule = null;
      this.m_cResult = null;
      this.m_cTeam1 = null;
      this.m_cTeam2 = null;
      this.m_tStats = null; // [mod]
      this.m_tScorers = null;
      this.m_cHalf = null;
      this.m_cExit = null;
      this.m_cAchFrame = null; // [mod]
      this.m_cAchName = null;
      this.m_cAchDesc = null;
      super.m_cYesNoDlg = null;
   }
}
