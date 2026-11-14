import java.util.Hashtable;

// [mod] Écran Succès : liste des succès du profil, page par page (ModAchievements), ouvert depuis l'écran Profil,
// voir notes/achievements.md
public class MenuAchievements extends SoccerUserInterface {
   static final int _ROWS = 12; // lignes ID_A<n>_* (mods/ui/make_achievements_scr.py)
   UIStatic m_cTitle = null;
   UIStatic m_cFrame = null;
   UIStatic[] m_tName = new UIStatic[_ROWS];
   UIStatic[] m_tDesc = new UIStatic[_ROWS];
   UIStatic[] m_tState = new UIStatic[_ROWS];
   int m_nPage = 0;

   MenuAchievements() {
      super.m_sFiles = new String[]{"data\\menu\\scr\\achievements640.scr", "data\\menu\\scr\\achievements.scr", "data\\menu\\scr\\achievements1024.scr"};
   }

   public void OnInit() {
      super.OnInit();
      this.m_cTitle = (UIStatic)this.GetElem("ID_TITLE");
      this.m_cFrame = (UIStatic)this.GetElem("ID_FRAME_LIST");

      for (int i = 0; i < _ROWS; i++) {
         this.m_tName[i] = (UIStatic)this.GetElem("ID_A" + i + "_NAME");
         this.m_tDesc[i] = (UIStatic)this.GetElem("ID_A" + i + "_DESC");
         this.m_tState[i] = (UIStatic)this.GetElem("ID_A" + i + "_STATE");
      }

      this.AddToSlideIn(this.GetElem("ID_NEXT_PAGE"));
      this.AddToSlideIn(this.GetElem("ID_CANCEL"));
   }

   int GetPageCount() {
      return Math.max(1, (ModAchievements.GetList().size() + _ROWS - 1) / _ROWS);
   }

   public void Show() {
      ModAchievements.Evaluate(false);
      this.m_nPage = 0;
      ((UIButton)this.GetElem("ID_NEXT_PAGE")).SetDisabled(this.GetPageCount() <= 1);
      this.Refresh();
      super.m_sTitle = "";
      this.CenterCursor();
      super.Show();
   }

   void Refresh() {
      java.util.Vector vList = ModAchievements.GetList();
      // le nom du profil est déjà affiché en haut à droite ; un titre plus long passerait sous le logo
      this.m_cTitle.SetText(Text.Get("&AchTitle&") + " : " + ModAchievements.CountUnlocked() + " / " + vList.size());
      this.m_cFrame.SetText(Text.Get("&AchFrameList&") + " " + (this.m_nPage + 1) + "/" + this.GetPageCount());
      Hashtable tProgress = ModAchievements.GetProgress();

      for (int i = 0; i < _ROWS; i++) {
         int nIndex = this.m_nPage * _ROWS + i;
         if (nIndex >= vList.size()) {
            this.m_tName[i].SetText("");
            this.m_tDesc[i].SetText("");
            this.m_tState[i].SetText("");
         } else {
            ModAchievements.Achievement a = (ModAchievements.Achievement)vList.elementAt(nIndex);
            String sDate = ModAchievements.GetUnlockDate(a);
            this.m_tName[i].SetText(ModAchievements.GetName(a));
            this.m_tDesc[i].SetText(ModAchievements.GetDescription(a));
            if (sDate != null) {
               // débloqué : nom doré, date « 10 nov. » (« 10/11 » se lirait comme une progression)
               this.m_tName[i].SetTextColor(1.0F, 0.82F, 0.25F);
               this.m_tDesc[i].SetTextColor(1.0F, 1.0F, 1.0F);
               this.m_tState[i].SetText(ModAchievements.FormatDate(sDate));
               this.m_tState[i].SetTextColor(0.4F, 1.0F, 0.4F);
            } else {
               this.m_tName[i].SetTextColor(0.7F, 0.7F, 0.7F);
               this.m_tDesc[i].SetTextColor(0.7F, 0.7F, 0.7F);
               this.m_tState[i].SetText(ModAchievements.GetProgressText(tProgress, a));
               this.m_tState[i].SetTextColor(0.7F, 0.7F, 0.7F);
            }
         }
      }
   }

   void OnUICommand(UIWindow cSender, int nCommand) {
      super.OnUICommand(cSender, nCommand);
      if (nCommand == 0 && cSender.m_sId != null) {
         if (cSender.m_sId.equals("ID_NEXT_PAGE")) {
            this.m_nPage = (this.m_nPage + 1) % this.GetPageCount();
            this.Refresh();
         } else if (cSender.m_sId.equals("ID_CANCEL")) {
            ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_PROFILE);
         }
      }
   }
}
