// [mod] Écran Mods : déblocages, affichage, debug et triches de match (ModOptions), ouvert depuis le menu principal, voir notes/mods-menu.md
public class MenuMods extends SoccerUserInterface {
   UIStatic m_cSpeed = null;
   UIStatic m_cShotPower = null;
   UIStatic m_cSuper = null;
   UIStatic m_cCheatInfo = null;
   // valeurs en cours d'édition, appliquées par OK
   int m_nSpeed = 0;
   int m_nShotPower = 0;
   int m_nSuperType = ModOptions._SUPER_RANDOM;

   MenuMods() {
      super.m_sFiles = new String[]{"data\\menu\\scr\\mods640.scr", "data\\menu\\scr\\mods.scr", "data\\menu\\scr\\mods1024.scr"};
   }

   public void OnInit() {
      super.OnInit();
      this.m_cSpeed = (UIStatic)this.GetElem("ID_SPEED_VALUE");
      this.m_cShotPower = (UIStatic)this.GetElem("ID_POWER_VALUE");
      this.m_cSuper = (UIStatic)this.GetElem("ID_SUPER_VALUE");
      this.m_cCheatInfo = (UIStatic)this.GetElem("ID_CHEAT_INFO");
      this.AddToSlideIn(this.GetElem("ID_OK"));
      this.AddToSlideIn(this.GetElem("ID_CANCEL"));
   }

   UIButton GetCheck(String sId) {
      return (UIButton)this.GetElem(sId);
   }

   public void Show() {
      this.GetCheck("ID_ALLTEAMS_CHECK").SetCheck(Cheat.m_bAllTeams);
      this.GetCheck("ID_ALLEXTRAS_CHECK").SetCheck(Cheat.m_bAllExtras);
      this.GetCheck("ID_FPS_CHECK").SetCheck(SoccerGame.m_bShowFPS);
      this.GetCheck("ID_DEBUG_CHECK").SetCheck(ModOptions.m_bDebugNextRun);
      this.GetCheck("ID_SUPERS_CHECK").SetCheck(ModOptions.m_bInfiniteSupers);
      this.m_nSpeed = Math.max(0, ModOptions.Find(ModOptions._SPEEDS, ModOptions.m_nSpeed));
      this.m_nShotPower = Math.max(0, ModOptions.Find(ModOptions._SHOT_POWERS, ModOptions.m_nShotPower));
      this.m_nSuperType = ModOptions.m_nSuperType;
      this.Refresh();
      super.m_sTitle = "";
      this.CenterCursor();
      super.Show();
   }

   static String Percent(int n) {
      return n + " %";
   }

   boolean IsCheat() {
      return this.m_nSpeed != 0 || this.m_nShotPower != 0 || this.m_nSuperType != ModOptions._SUPER_RANDOM || this.GetCheck("ID_SUPERS_CHECK").GetCheck();
   }

   void Refresh() {
      this.m_cSpeed.SetText(Percent(ModOptions._SPEEDS[this.m_nSpeed]));
      this.m_cShotPower.SetText(Percent(ModOptions._SHOT_POWERS[this.m_nShotPower]));
      this.m_cSuper.SetText(Text.Get("&ModsSuper" + (this.m_nSuperType + 1) + "&"));
      this.SetValueColor(this.m_cSpeed, this.m_nSpeed != 0);
      this.SetValueColor(this.m_cShotPower, this.m_nShotPower != 0);
      this.SetValueColor(this.m_cSuper, this.m_nSuperType != ModOptions._SUPER_RANDOM);
      if (this.IsCheat()) {
         this.m_cCheatInfo.SetText(Text.Get("&ModsCheatOn&"));
         this.m_cCheatInfo.SetTextColor(1.0F, 0.75F, 0.3F);
      } else {
         this.m_cCheatInfo.SetText(Text.Get("&ModsCheatOff&"));
         this.m_cCheatInfo.SetTextColor(0.75F, 0.75F, 0.75F);
      }
   }

   // valeur du jeu d'origine en blanc, triche en orange
   void SetValueColor(UIStatic cStatic, boolean bCheat) {
      if (bCheat) {
         cStatic.SetTextColor(1.0F, 0.75F, 0.3F);
      } else {
         cStatic.SetTextColor(1.0F, 1.0F, 1.0F);
      }
   }

   void OnOK() {
      Cheat.m_bAllTeams = this.GetCheck("ID_ALLTEAMS_CHECK").GetCheck();
      Cheat.m_bAllExtras = this.GetCheck("ID_ALLEXTRAS_CHECK").GetCheck();
      SoccerGame.m_bShowFPS = this.GetCheck("ID_FPS_CHECK").GetCheck();
      ModOptions.m_bDebugNextRun = this.GetCheck("ID_DEBUG_CHECK").GetCheck();
      ModOptions.m_bInfiniteSupers = this.GetCheck("ID_SUPERS_CHECK").GetCheck();
      ModOptions.m_nSpeed = ModOptions._SPEEDS[this.m_nSpeed];
      ModOptions.m_nShotPower = ModOptions._SHOT_POWERS[this.m_nShotPower];
      ModOptions.m_nSuperType = this.m_nSuperType;
      ModOptions.Save();
      this.Close();
   }

   void Close() {
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(0);
   }

   void OnUICommand(UIWindow cSender, int nCommand) {
      super.OnUICommand(cSender, nCommand);
      if (nCommand == 0 && cSender.m_sId != null) {
         String sId = cSender.m_sId;
         int nDelta = sId.endsWith("_NEXT") ? 1 : (sId.endsWith("_PREV") ? -1 : 0);
         if (sId.equals("ID_OK")) {
            this.OnOK();
         } else if (sId.equals("ID_CANCEL")) {
            this.Close();
         } else if (sId.startsWith("ID_SPEED_") && nDelta != 0) {
            this.m_nSpeed = (this.m_nSpeed + nDelta + ModOptions._SPEEDS.length) % ModOptions._SPEEDS.length;
            this.Refresh();
         } else if (sId.startsWith("ID_POWER_") && nDelta != 0) {
            this.m_nShotPower = (this.m_nShotPower + nDelta + ModOptions._SHOT_POWERS.length) % ModOptions._SHOT_POWERS.length;
            this.Refresh();
         } else if (sId.startsWith("ID_SUPER_") && nDelta != 0) {
            // -1 (aléatoire) .. _SUPER_TYPES - 1
            int nCount = ModOptions._SUPER_TYPES + 1;
            this.m_nSuperType = (this.m_nSuperType + 1 + nDelta + nCount) % nCount - 1;
            this.Refresh();
         } else if (sId.equals("ID_SUPERS_CHECK")) {
            this.Refresh();
         }
      }
   }
}
