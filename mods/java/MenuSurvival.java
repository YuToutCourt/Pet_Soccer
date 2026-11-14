// [mod] Écran Survie (ModSurvival), entre deux manches : état de la série, prochain adversaire, résultat du dernier match.
// Ouvert après le choix de l'équipe (MenuChooseTeamColors) et au retour de chaque match (SoccerMainMenuModule.ShowLoadingScreen).
// Voir notes/survival.md. Écran généré par mods/ui/make_survival_scr.py.
public class MenuSurvival extends SoccerUserInterface {
   static final int _RUN_ROWS = 6;
   static final int _MSG_ROWS = 4;
   static final int DLG_STOP = 0;
   UIStatic m_cFrameMatch = null;
   UIStatic[] m_tLabels = new UIStatic[_RUN_ROWS];
   UIStatic[] m_tValues = new UIStatic[_RUN_ROWS];
   UIStatic[] m_tMsg = new UIStatic[_MSG_ROWS];
   UIStatic m_cName1 = null;
   UIStatic m_cName2 = null;
   UIStatic m_cVs = null;
   UIWindow m_cLogo1 = null;
   UIWindow m_cLogo2 = null;
   UIMenuButton m_cPlay = null;
   UIMenuButton m_cBack = null;

   MenuSurvival() {
      super.m_sFiles = new String[]{"data\\menu\\scr\\survival640.scr", "data\\menu\\scr\\survival.scr", "data\\menu\\scr\\survival1024.scr"};
   }

   public void OnInit() {
      super.OnInit();
      this.m_cFrameMatch = (UIStatic)this.GetElem("ID_FRAME_MATCH");
      String[] tLabels = ModStats.SplitLines(Text.Get("&SurvivalLabels&"), _RUN_ROWS);

      for (int i = 0; i < _RUN_ROWS; i++) {
         this.m_tLabels[i] = (UIStatic)this.GetElem("ID_R" + i + "_LABEL");
         this.m_tValues[i] = (UIStatic)this.GetElem("ID_R" + i + "_VALUE");
         this.m_tLabels[i].SetText(tLabels[i]);
      }

      for (int i = 0; i < _MSG_ROWS; i++) {
         this.m_tMsg[i] = (UIStatic)this.GetElem("ID_MSG" + i);
      }

      this.m_cName1 = (UIStatic)this.GetElem("ID_NAME_1");
      this.m_cName2 = (UIStatic)this.GetElem("ID_NAME_2");
      this.m_cVs = (UIStatic)this.GetElem("ID_VS");
      this.m_cLogo1 = this.GetElem("ID_LOGO_1");
      this.m_cLogo2 = this.GetElem("ID_LOGO_2");
      this.m_cPlay = (UIMenuButton)this.GetElem("ID_OK");
      this.m_cBack = (UIMenuButton)this.GetElem("ID_CANCEL");
      this.AddToSlideIn(this.m_cPlay);
      this.AddToSlideIn(this.m_cBack);
   }

   public void Show() {
      this.Refresh();
      super.m_sTitle = "";
      this.CenterCursor();
      super.Show();
   }

   void SetLogo(UIWindow cWnd, int nTeam) {
      if (cWnd == null) {
         return;
      }

      if (nTeam < 0) {
         cWnd.SetTextureAlpha(0.0F);
      } else {
         cWnd.SetTexture(ETeams.GetTeamData(nTeam).m_cLogo);
         cWnd.SetTextureAlpha(1.0F);
      }
   }

   void SetMsg(int i, String s, float r, float g, float b) {
      this.m_tMsg[i].SetText(s);
      this.m_tMsg[i].SetTextColor(r, g, b);
   }

   void Refresh() {
      boolean bOver = ModSurvival.IsOver();
      int nWave = ModSurvival.GetWave();
      int nRecord = ModSurvival.GetRecord();
      // série : manche, manches réussies, record, niveau, vitesse et frappe de l'IA
      this.m_tValues[0].SetText("" + nWave);
      this.m_tValues[1].SetText("" + ModSurvival.GetCleared());
      this.m_tValues[2].SetText("" + nRecord);
      this.m_tValues[3].SetText(ModSurvival.GetLevelText(nWave));
      this.m_tValues[4].SetText(ModSurvival.FormatBonus(ModSurvival.GetSpeedMul(nWave)));
      this.m_tValues[5].SetText(ModSurvival.FormatBonus(ModSurvival.GetShotPowerMul(nWave)));
      this.m_tValues[2].SetTextColor(1.0F, ModSurvival.IsNewRecord() ? 0.82F : 1.0F, ModSurvival.IsNewRecord() ? 0.25F : 1.0F);
      float fBonus = ModSurvival.GetExtraWaves(nWave) > 0 ? 0.45F : 1.0F;
      this.m_tValues[4].SetTextColor(1.0F, fBonus, fBonus);
      this.m_tValues[5].SetTextColor(1.0F, fBonus, fBonus);
      // prochain match (ou dernier adversaire si la série est finie)
      int nOpponent = bOver ? ModSurvival.m_nLastOpponent : ModSurvival.GetOpponent();
      this.m_cFrameMatch.SetText(Text.Get(bOver ? "&SurvivalFrameOver&" : "&SurvivalFrameMatch&"));
      this.m_cName1.SetText(ETeams.GetTeamData(ModSurvival.GetTeam()).m_sName.toLowerCase());
      this.m_cName2.SetText(nOpponent >= 0 ? ETeams.GetTeamData(nOpponent).m_sName.toLowerCase() : "");
      this.SetLogo(this.m_cLogo1, ModSurvival.GetTeam());
      this.SetLogo(this.m_cLogo2, nOpponent);

      for (int i = 0; i < _MSG_ROWS; i++) {
         this.SetMsg(i, "", 1.0F, 1.0F, 1.0F);
      }

      if (bOver) {
         // fin de série : dernier match, bilan, record
         if (ModSurvival.m_bLastAbandon) {
            this.SetMsg(0, Text.Get("&SurvivalAbandon&") + " (" + ModSurvival.GetLastScoreText() + ")", 1.0F, 0.45F, 0.45F);
         } else {
            this.SetMsg(0, Text.Get("&SurvivalLoss&") + " " + ModSurvival.GetLastScoreText(), 1.0F, 0.45F, 0.45F);
         }

         this.SetMsg(1, Text.Get("&SurvivalOver&") + " : " + ModSurvival.GetClearedText(ModSurvival.GetCleared()), 1.0F, 1.0F, 1.0F);
         if (ModSurvival.IsNewRecord()) {
            this.SetMsg(2, Text.Get("&SurvivalNewRecord&"), 1.0F, 0.82F, 0.25F);
         } else {
            this.SetMsg(2, Text.Get("&SurvivalRecord&") + " : " + ModSurvival.GetClearedText(nRecord), 0.85F, 0.85F, 0.85F);
         }

         this.m_cPlay.SetText(Text.Get("&SurvivalRestart&"));
         this.m_cBack.SetText(Text.Get("&MenuBack&"));
      } else {
         if (ModSurvival.m_bHasLast) {
            this.SetMsg(0, Text.Get("&SurvivalWin&") + " " + ModSurvival.GetLastScoreText(), 0.4F, 1.0F, 0.4F);
            if (ModSurvival.m_bLastGolden) {
               this.SetMsg(1, Text.Get("&SurvivalGolden&"), 1.0F, 0.82F, 0.25F);
            }
         } else {
            this.SetMsg(0, Text.Get("&SurvivalIntro&"), 0.85F, 0.85F, 0.85F);
         }

         if (ModSurvival.GetExtraWaves(nWave) == 1) {
            this.SetMsg(2, Text.Get("&SurvivalBoostStart&"), 1.0F, 0.45F, 0.45F);
         } else if (ModSurvival.GetLevel(nWave) > ModSurvival.GetLevel(nWave - 1) && nWave > 1) {
            this.SetMsg(2, Text.Get("&SurvivalLevelUp&") + " " + ModSurvival.GetLevelText(nWave), 1.0F, 0.82F, 0.25F);
         }

         this.m_cPlay.SetText(Text.Get("&SurvivalPlay&"));
         this.m_cBack.SetText(Text.Get(ModSurvival.GetCleared() > 0 ? "&SurvivalStop&" : "&MenuBack&"));
      }
   }

   void OnPlay() {
      if (ModSurvival.IsOver()) {
         ModSurvival.Restart();
         this.Refresh();
         return;
      }

      ModSurvival.PrepareMatch();
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(9);
   }

   void OnBack() {
      if (ModSurvival.IsRunning() && ModSurvival.GetCleared() > 0) {
         this.YesNoDialog(DLG_STOP, Text.Get("&SurvivalStopTitle&"), Text.Get("&SurvivalStopQuestion&"), null, null);
      } else {
         this.GoToMainMenu();
      }
   }

   void GoToMainMenu() {
      ModSurvival.Stop();
      CoreLogic.m_bSurvival = false;
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(0);
   }

   void OnYesFromDlg(int nID) {
      super.OnYesFromDlg(nID);
      if (nID == DLG_STOP) {
         this.GoToMainMenu();
      }
   }

   void OnUICommand(UIWindow cSender, int nCommand) {
      super.OnUICommand(cSender, nCommand);
      if (nCommand == 0 && cSender.m_sId != null) {
         if (cSender.m_sId.equals("ID_OK")) {
            this.OnPlay();
         } else if (cSender.m_sId.equals("ID_CANCEL")) {
            this.OnBack();
         }
      }
   }
}
