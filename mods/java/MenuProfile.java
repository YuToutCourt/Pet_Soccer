// [mod] Écran Profil : bilan du profil et derniers matchs (ModStats), ouvert depuis le menu principal, voir notes/stats.md.
// Le bouton « succès » ouvre MenuAchievements (notes/achievements.md).
public class MenuProfile extends SoccerUserInterface {
   static final int _ROWS = 12;
   UIStatic m_cTitle = null;
   UIStatic[] m_tSummaryLabels = new UIStatic[ModStats._SUMMARY_ROWS];
   UIStatic[] m_tSummaryValues = new UIStatic[ModStats._SUMMARY_ROWS];
   UIStatic m_cEmpty = null;
   UIStatic[] m_tDate = new UIStatic[_ROWS];
   UIStatic[] m_tMode = new UIStatic[_ROWS];
   UIStatic[] m_tMatch = new UIStatic[_ROWS];
   UIStatic[] m_tElo = new UIStatic[_ROWS];

   MenuProfile() {
      super.m_sFiles = new String[]{"data\\menu\\scr\\profile640.scr", "data\\menu\\scr\\profile.scr", "data\\menu\\scr\\profile1024.scr"};
   }

   public void OnInit() {
      super.OnInit();
      this.m_cTitle = (UIStatic)this.GetElem("ID_TITLE");

      for (int i = 0; i < ModStats._SUMMARY_ROWS; i++) {
         this.m_tSummaryLabels[i] = (UIStatic)this.GetElem("ID_S" + i + "_LABEL");
         this.m_tSummaryValues[i] = (UIStatic)this.GetElem("ID_S" + i + "_VALUE");
      }

      this.m_cEmpty = (UIStatic)this.GetElem("ID_EMPTY");

      for (int i = 0; i < _ROWS; i++) {
         this.m_tDate[i] = (UIStatic)this.GetElem("ID_H" + i + "_DATE");
         this.m_tMode[i] = (UIStatic)this.GetElem("ID_H" + i + "_MODE");
         this.m_tMatch[i] = (UIStatic)this.GetElem("ID_H" + i + "_MATCH");
         this.m_tElo[i] = (UIStatic)this.GetElem("ID_H" + i + "_ELO");
      }

      this.AddToSlideIn(this.GetElem("ID_ACHIEVEMENTS"));
      this.AddToSlideIn(this.GetElem("ID_CANCEL"));
   }

   public void Show() {
      PlayerSave player = CoreLogic.GetPlayer();
      this.m_cTitle.SetText(Text.Get("&ProfileTitle&") + (player != null && player.m_Name != null ? " : " + player.m_Name : ""));
      String[] tSummary = ModStats.GetSummary();
      String[] tLabels = ModStats.SplitLines(tSummary[0], ModStats._SUMMARY_ROWS);
      String[] tValues = ModStats.SplitLines(tSummary[1], ModStats._SUMMARY_ROWS);

      for (int i = 0; i < ModStats._SUMMARY_ROWS; i++) {
         this.m_tSummaryLabels[i].SetText(tLabels[i]);
         this.m_tSummaryValues[i].SetText(tValues[i]);
      }

      java.util.Vector vHistory = ModStats.GetHistory();
      this.m_cEmpty.SetText(vHistory.size() == 0 ? Text.Get("&ProfileEmpty&") : "");

      // du plus récent au plus ancien
      for (int i = 0; i < _ROWS; i++) {
         int nIndex = vHistory.size() - 1 - i;
         if (nIndex < 0) {
            this.m_tDate[i].SetText("");
            this.m_tMode[i].SetText("");
            this.m_tMatch[i].SetText("");
            this.m_tElo[i].SetText("");
         } else {
            ModStats.Record r = (ModStats.Record)vHistory.elementAt(nIndex);
            this.m_tDate[i].SetText(ModStats.GetShortDate(r));
            this.m_tMode[i].SetText(ModStats.GetModeName(r.m_tValues[ModStats._MODE]));
            this.m_tMatch[i].SetText(ModStats.GetMatchText(r));
            int nOutcome = ModStats.GetOutcome(r);
            if (nOutcome > 0) {
               this.m_tMatch[i].SetTextColor(0.4F, 1.0F, 0.4F);
            } else if (nOutcome < 0) {
               this.m_tMatch[i].SetTextColor(1.0F, 0.45F, 0.45F);
            } else {
               this.m_tMatch[i].SetTextColor(0.85F, 0.85F, 0.85F);
            }

            this.m_tElo[i].SetText(ModStats.GetEloText(r));
            int nDelta = r.m_tValues[ModStats._ELO_DELTA];
            if (nDelta > 0) {
               this.m_tElo[i].SetTextColor(0.4F, 1.0F, 0.4F);
            } else if (nDelta < 0) {
               this.m_tElo[i].SetTextColor(1.0F, 0.45F, 0.45F);
            } else {
               this.m_tElo[i].SetTextColor(0.85F, 0.85F, 0.85F);
            }
         }
      }

      super.m_sTitle = "";
      this.CenterCursor();
      super.Show();
   }

   void OnUICommand(UIWindow cSender, int nCommand) {
      super.OnUICommand(cSender, nCommand);
      if (nCommand == 0 && cSender.m_sId != null && cSender.m_sId.equals("ID_CANCEL")) {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(0);
      } else if (nCommand == 0 && cSender.m_sId != null && cSender.m_sId.equals("ID_ACHIEVEMENTS")) {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_ACHIEVEMENTS); // [mod] écran Succès
      }
   }
}
