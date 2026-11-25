// [mod] Écran Tactique (avant-match et mi-temps), voir notes/tactics.md :
// carte du terrain avec les 6 joueurs de champ (numéros 2 à 7, le gardien est le 1) ; clic sur un joueur pour le
// sélectionner, clic sur le terrain pour le déplacer. Une carte par situation : jeu, coup d'envoi, touche, dégagement.
// À droite : schéma (préréglage de tous les rôles), rôle du joueur sélectionné.
public class MenuTactics extends SoccerUserInterface {
   static final String _TEX = "data/menu/tactics_field.png";
   // rectangles de la planche (512x512), alignés sur mods/art/make_tactics_field.py
   static final float[] _RECT_NORMAL = new float[]{0.0F, 320.0F, 64.0F, 384.0F};
   static final float[] _RECT_SELECTED = new float[]{64.0F, 320.0F, 128.0F, 384.0F};
   static final float[] _RECT_GREY = new float[]{256.0F, 320.0F, 320.0F, 384.0F};
   static final float[] _GK_POS = new float[]{0.035F, 0.5F};
   static final float[] _KICKOFF_BALL = new float[]{0.5F, 0.5F};
   static final String[] _SIT_KEYS = new String[]{"TacticsSitPlay", "TacticsSitKickoff", "TacticsSitThrow", "TacticsSitGoalKick"};
   static final String[] _ORIGIN_ROLES = new String[]{"DEFENSEUR", "DEFENSEUR", "DEFENSEUR", "ATTAQUANT", "ATTAQUANT", "ATTAQUANT"};

   UIStatic m_cTitle = null;
   UIStatic m_cScheme = null;
   UIStatic m_cSituation = null;
   UIStatic m_cPlayer = null;
   UIStatic m_cRole = null;
   UIStatic m_cInfo = null;
   UIStatic m_cHint = null;
   UIWindow m_cField = null;
   UIStatic m_cGK = null;
   UIStatic m_cBall = null;
   UIStatic[] m_tTokens = new UIStatic[ModTactics._SLOTS];
   UITexture[][] m_tTokenTex = new UITexture[ModTactics._SLOTS][];
   int m_nAnimal = -1;
   ModTactics.Composition m_cComp = null;
   int m_nSchemeIndex = -1; // -1 = personnalisé
   int m_nSituation = ModTactics._SIT_PLAY;
   int m_nSelected = 0;
   boolean m_bOpen = false; // [mod] l'écran est affiché

   MenuTactics() {
      super.m_sFiles = new String[]{"data\\menu\\scr\\tactics640.scr", "data\\menu\\scr\\tactics.scr", "data\\menu\\scr\\tactics1024.scr"};
   }

   public void OnInit() {
      super.OnInit();
      this.m_cTitle = (UIStatic)this.GetElem("ID_TITLE");
      this.m_cScheme = (UIStatic)this.GetElem("ID_SCHEME_VALUE");
      this.m_cSituation = (UIStatic)this.GetElem("ID_SIT_VALUE");
      this.m_cPlayer = (UIStatic)this.GetElem("ID_PLAYER");
      this.m_cRole = (UIStatic)this.GetElem("ID_ROLE_VALUE");
      this.m_cInfo = (UIStatic)this.GetElem("ID_INFO");
      this.m_cHint = (UIStatic)this.GetElem("ID_MAP_HINT");
      this.m_cField = this.GetElem("ID_FIELD");
      this.m_cGK = (UIStatic)this.GetElem("ID_TOK_GK");
      this.m_cBall = (UIStatic)this.GetElem("ID_TOK_BALL");

      for (int i = 0; i < ModTactics._SLOTS; i++) {
         this.m_tTokens[i] = (UIStatic)this.GetElem("ID_TOK" + i);
         this.m_tTokenTex[i] = new UITexture[]{
            MakeTexture(this.m_tTokens[i], _RECT_NORMAL), MakeTexture(this.m_tTokens[i], _RECT_SELECTED), MakeTexture(this.m_tTokens[i], _RECT_GREY)
         };
      }

      this.AddToSlideIn(this.GetElem("ID_OK"));
      this.AddToSlideIn(this.GetElem("ID_CANCEL"));
      this.AddToSlideIn(this.GetElem("ID_AUTO"));
   }

   static UITexture MakeTexture(UIWindow cWnd, float[] r) {
      UITexture cTex = new UITexture(_TEX, 512.0F, 512.0F, r[0], r[1], r[2], r[3]);
      cTex.m_cWindow = cWnd;
      cTex.m_nId = cWnd.AddTexture(_TEX);
      return cTex;
   }

   boolean IsInGame() {
      return !(this.GetModule() instanceof SoccerMainMenuModule);
   }

   // équipe du joueur : en match celle qu'il dirige, avant le match l'équipe hôte (ou l'invitée en championnat)
   TeamData GetTeamData() {
      // [mod] en ligne : les deux équipes sont dirigées par des joueurs ; celle de l'hôte est toujours « gauche »,
      // celle de l'invité « droite »
      if (this.IsInGame() && ModNet.IsOnline()) {
         return ModNet.IsClient() ? CoreLogic.m_cTeamDataRight : CoreLogic.m_cTeamDataLeft;
      }

      if (this.IsInGame() && SoccerModule.m_cMatch != null) {
         for (int t = 0; t < 2; t++) {
            Team cTeam = SoccerModule.m_cMatch.GetTeam(t);
            if (cTeam != null && cTeam.m_bUserControled) {
               if (CoreLogic.m_cTeamDataRight != null && CoreLogic.m_cTeamDataRight.m_nAnimalType == cTeam.m_nAnimalType) {
                  return CoreLogic.m_cTeamDataRight;
               }

               return CoreLogic.m_cTeamDataLeft;
            }
         }
      }

      if (CoreLogic.m_bChampionships && Match.m_cMatchData != null && !Match.m_cMatchData.IsPlayerHost()) {
         return CoreLogic.m_cTeamDataRight;
      }

      return CoreLogic.m_cTeamDataLeft;
   }

   public void Show() {
      TeamData td = this.GetTeamData();
      this.m_nAnimal = td != null ? td.m_nAnimalType : -1;
      this.m_cComp = ModTactics.GetComposition(this.m_nAnimal);
      this.m_cTitle.SetText(Text.Get("&TacticsTitle&") + (td != null ? " : " + td.m_sName : ""));
      this.m_nSchemeIndex = this.FindScheme(this.m_cComp.m_sScheme);
      this.m_nSituation = ModTactics._SIT_PLAY;
      this.m_nSelected = 0;
      this.Refresh();
      this.ShowSchemeInfo();
      super.m_sTitle = "";
      this.CenterCursor();
      super.Show();
      this.m_bOpen = true;
      // les pions se placent d'après la carte : une fois l'écran affiché, sa position réelle est connue
      this.RefreshMap();
   }

   public void Hide() {
      this.m_bOpen = false;
      super.Hide();
   }

   // [mod] en ligne : la pause de mi-temps finit pendant que l'écran est ouvert (ModNet). Ce qui est réglé est
   // gardé, comme si le joueur avait cliqué « ok ».
   void CommitIfOpen() {
      if (this.m_bOpen && this.m_cComp != null && this.m_nAnimal >= 0) {
         this.m_bOpen = false;
         this.Save();
      }
   }

   // enregistre la composition et l'applique au match en cours
   void Save() {
      ModTactics.SetComposition(this.m_nAnimal, this.m_cComp);
      if (this.IsInGame() && ModNet.IsClient()) {
         ModLobby.SetMyTactics(this.m_cComp); // [mod] en ligne : c'est l'hôte qui l'applique à l'équipe de l'invité
      } else if (this.IsInGame() && SoccerModule.m_cMatch != null) {
         if (ModNet.IsHost()) {
            // [mod] en ligne : le serveur enregistre la tactique du dernier PKT_STATE ; sans ça il garderait celle du
            // coup d'envoi alors que les places (ModStats) sont celles de la nouvelle, et les rôles du site seraient faux
            ModLobby.SetMyTactics(this.m_cComp);
         }

         ModTactics.ApplyTeams(SoccerModule.m_cMatch);
      }
   }

   int FindScheme(String sId) {
      for (int s = 0; s < ModTactics.GetSchemeCount(); s++) {
         if (ModTactics.GetScheme(s).m_sId.equals(sId)) {
            return s;
         }
      }

      return -1;
   }

   int FindRole(String sId) {
      for (int r = 0; r < ModTactics.GetRoleCount(); r++) {
         if (ModTactics.GetRole(r).m_sId.equals(sId)) {
            return r;
         }
      }

      return 0;
   }

   boolean IsOrigin() {
      return this.m_cComp.m_tRoles == null;
   }

   // rôle affiché pour une place (jeu d'origine : 3 défenseurs puis 3 attaquants)
   String SlotRoleId(int nSlot) {
      return this.IsOrigin() ? _ORIGIN_ROLES[nSlot] : this.m_cComp.m_tRoles[nSlot];
   }

   // ---------------------------------------------------------------- affichage

   void Refresh() {
      if (this.m_nSchemeIndex >= 0) {
         this.m_cScheme.SetText(Text.Get("&" + ModTactics.GetScheme(this.m_nSchemeIndex).m_sText + "&"));
      } else {
         this.m_cScheme.SetText(Text.Get("&SchemeCustom&"));
      }

      boolean bAuto = !this.IsOrigin() && this.m_cComp.m_tLayouts[this.m_nSituation] == null;
      this.m_cSituation.SetText(Text.Get("&" + _SIT_KEYS[this.m_nSituation] + "&") + (bAuto ? Text.Get("&TacticsAutoSuffix&") : ""));
      this.m_cHint.SetText(Text.Get("&" + (this.IsOrigin() ? "TacticsOriginHint" : _SIT_KEYS[this.m_nSituation] + "Info") + "&"));
      this.m_cPlayer.SetText(Text.Get("&TacticsPlayer&") + (this.m_nSelected + 2));
      ModTactics.Role cRole = ModTactics.GetRole(this.FindRole(this.SlotRoleId(this.m_nSelected)));
      this.m_cRole.SetText(Text.Get("&" + cRole.m_sText + "&"));
      float fGrey = this.IsOrigin() ? 0.6F : 1.0F;
      this.m_cRole.SetTextColor(fGrey, fGrey, fGrey);
      this.RefreshMap();
   }

   // place les pions sur la carte : centre du pion = point du terrain (profondeur vers la droite, latéral vers le bas)
   void RefreshMap() {
      if (this.m_cField == null || this.m_cComp == null) {
         return;
      }

      float[] t = ModTactics.GetLayout(this.m_cComp, this.m_nSituation);
      boolean bOrigin = this.IsOrigin();

      for (int i = 0; i < ModTactics._SLOTS; i++) {
         UIStatic cTok = this.m_tTokens[i];
         this.PlaceOnField(cTok, t[2 * i], t[2 * i + 1]);
         int nTex = bOrigin ? 2 : (i == this.m_nSelected ? 1 : 0);
         cTok.SetTexture(this.m_tTokenTex[i][nTex]);
         cTok.SetTextureAlpha(1.0F);
         if (nTex == 1) {
            cTok.SetTextColor(0.1F, 0.1F, 0.1F);
         } else {
            cTok.SetTextColor(1.0F, 1.0F, 1.0F);
         }
      }

      this.PlaceOnField(this.m_cGK, _GK_POS[0], _GK_POS[1]);
      float[] tBall = null;
      if (this.m_nSituation == ModTactics._SIT_KICKOFF) {
         tBall = _KICKOFF_BALL;
      } else if (this.m_nSituation == ModTactics._SIT_THROW) {
         tBall = ModTactics._THROW_BALL;
      } else if (this.m_nSituation == ModTactics._SIT_GOALKICK) {
         tBall = ModTactics._GOALKICK_BALL;
      }

      if (tBall != null) {
         this.PlaceOnField(this.m_cBall, tBall[0], tBall[1]);
         this.m_cBall.SetTextureAlpha(1.0F);
      } else {
         this.m_cBall.SetTextureAlpha(0.0F);
      }
   }

   void PlaceOnField(UIWindow cWnd, float fDepth, float fL) {
      Vector vSize = cWnd.GetLogicSize();
      float fX = this.m_cField.GetLogicPosX() + fDepth * this.m_cField.GetLogicSizeX() - vSize.fX / 2.0F;
      float fY = this.m_cField.GetLogicPosY() + fL * this.m_cField.GetLogicSizeY() - vSize.fY / 2.0F;
      cWnd.SetLogicPos(new Vector(fX, fY, 0.0F));
   }

   void ShowSchemeInfo() {
      String sKey = this.m_nSchemeIndex >= 0 ? ModTactics.GetScheme(this.m_nSchemeIndex).m_sText : "SchemeCustom";
      this.m_cInfo.SetText(Text.Get("&" + sKey + "Info&") + Text.Get("&TacticsHint&"));
   }

   void ShowRoleInfo() {
      ModTactics.Role cRole = ModTactics.GetRole(this.FindRole(this.SlotRoleId(this.m_nSelected)));
      this.m_cInfo.SetText(Text.Get("&" + cRole.m_sText + "Info&"));
   }

   // ---------------------------------------------------------------- modifications

   // toute modification quitte le jeu d'origine : 3 défenseurs + 3 attaquants, jeu aux places affichées
   // (points d'apparition du stade), coup d'envoi, touche et dégagement automatiques
   void MakeCustom() {
      if (this.IsOrigin()) {
         this.m_cComp.m_tRoles = (String[])_ORIGIN_ROLES.clone();

         for (int s = 0; s < ModTactics._SITUATIONS; s++) {
            this.m_cComp.m_tLayouts[s] = null;
         }

         this.m_cComp.m_tLayouts[ModTactics._SIT_PLAY] = (float[])ModTactics._DEFAULT_LAYOUT.clone();
      }

      this.UpdateSchemeName();
   }

   // schéma affiché : celui qui a exactement ces rôles, sinon « Personnalisé »
   void UpdateSchemeName() {
      this.m_nSchemeIndex = -1;
      this.m_cComp.m_sScheme = "PERSO";

      for (int s = 0; s < ModTactics.GetSchemeCount(); s++) {
         if (SameRoles(ModTactics.GetScheme(s).m_tRoles, this.m_cComp.m_tRoles)) {
            this.m_nSchemeIndex = s;
            this.m_cComp.m_sScheme = ModTactics.GetScheme(s).m_sId;
            break;
         }
      }
   }

   // un schéma remet tous les rôles et tous les placements par défaut
   void OnScheme(int nDelta) {
      int nCount = ModTactics.GetSchemeCount();
      if (nCount == 0) {
         return;
      }

      int nIndex = this.m_nSchemeIndex < 0 ? (nDelta > 0 ? 0 : nCount - 1) : (this.m_nSchemeIndex + nDelta + nCount) % nCount;
      this.m_nSchemeIndex = nIndex;
      ModTactics.Scheme cScheme = ModTactics.GetScheme(nIndex);
      this.m_cComp.m_sScheme = cScheme.m_sId;
      this.m_cComp.m_tRoles = cScheme.m_tRoles != null ? ModTactics.SortByDepth(cScheme.m_tRoles) : null;

      for (int s = 0; s < ModTactics._SITUATIONS; s++) {
         this.m_cComp.m_tLayouts[s] = null;
      }

      this.Refresh();
      this.ShowSchemeInfo();
   }

   // rôle du joueur sélectionné ; s'il a une position de jeu choisie, il prend la profondeur du nouveau rôle (même couloir)
   void OnRole(int nDelta) {
      this.MakeCustom();
      int nCount = ModTactics.GetRoleCount();
      int nRole = (this.FindRole(this.m_cComp.m_tRoles[this.m_nSelected]) + nDelta + nCount) % nCount;
      ModTactics.Role cRole = ModTactics.GetRole(nRole);
      this.m_cComp.m_tRoles[this.m_nSelected] = cRole.m_sId;
      float[] tPlay = this.m_cComp.m_tLayouts[ModTactics._SIT_PLAY];
      if (tPlay != null) {
         tPlay[2 * this.m_nSelected] = cRole.m_fDepth;
      }

      this.UpdateSchemeName();
      this.Refresh();
      this.ShowRoleInfo();
   }

   void OnSituation(int nDelta) {
      this.m_nSituation = (this.m_nSituation + nDelta + ModTactics._SITUATIONS) % ModTactics._SITUATIONS;
      this.Refresh();
   }

   void OnAuto() {
      if (!this.IsOrigin()) {
         this.m_cComp.m_tLayouts[this.m_nSituation] = null;
         this.Refresh();
      }
   }

   // clic sur la carte : sélectionne le joueur le plus proche s'il est sous le curseur, sinon y déplace le joueur sélectionné
   void OnFieldClick() {
      Vector vCursor = this.GetCursorPos();
      if (vCursor == null) {
         return;
      }

      Vector vFieldSize = this.m_cField.GetRealSize();
      if (vFieldSize.fX <= 0.0F || vFieldSize.fY <= 0.0F) {
         return;
      }

      float fDepth = (vCursor.fX - this.m_cField.GetRealX()) / vFieldSize.fX;
      float fL = (vCursor.fY - this.m_cField.GetRealY()) / vFieldSize.fY;
      float[] t = ModTactics.GetLayout(this.m_cComp, this.m_nSituation);
      Vector vTok = this.m_tTokens[0].GetRealSize();
      float fRadius = vTok.fX * 0.6F;
      int nHit = -1;
      float fBest = 0.0F;

      for (int i = 0; i < ModTactics._SLOTS; i++) {
         float dx = (t[2 * i] - fDepth) * vFieldSize.fX;
         float dy = (t[2 * i + 1] - fL) * vFieldSize.fY;
         float fDist = (float)Math.sqrt(dx * dx + dy * dy);
         if (fDist <= fRadius && (nHit < 0 || fDist < fBest)) {
            nHit = i;
            fBest = fDist;
         }
      }

      if (nHit >= 0) {
         this.m_nSelected = nHit;
         this.Refresh();
         this.ShowRoleInfo();
         return;
      }

      this.MakeCustom();
      t = ModTactics.GetLayout(this.m_cComp, this.m_nSituation);
      float fMaxDepth = this.m_nSituation == ModTactics._SIT_KICKOFF ? ModTactics._KICKOFF_MAX_DEPTH : ModTactics._MAX_DEPTH;
      t[2 * this.m_nSelected] = ModTactics.Clamp(fDepth, ModTactics._MIN_DEPTH, fMaxDepth);
      t[2 * this.m_nSelected + 1] = ModTactics.Clamp(fL, ModTactics._FIELD_MARGIN, 1.0F - ModTactics._FIELD_MARGIN);
      this.m_cComp.m_tLayouts[this.m_nSituation] = t;
      this.Refresh();
   }

   // mêmes rôles, dans n'importe quel ordre
   static boolean SameRoles(String[] a, String[] b) {
      if (a == null || b == null || a.length != b.length) {
         return false;
      }

      boolean[] tUsed = new boolean[b.length];

      for (int i = 0; i < a.length; i++) {
         boolean bFound = false;

         for (int j = 0; j < b.length && !bFound; j++) {
            if (!tUsed[j] && a[i].equals(b[j])) {
               tUsed[j] = true;
               bFound = true;
            }
         }

         if (!bFound) {
            return false;
         }
      }

      return true;
   }

   void OnOK() {
      this.Save();
      this.Close();
   }

   void Close() {
      if (this.IsInGame()) {
         this.Hide();
         ((SoccerInGameMenuModule)this.GetModule()).m_cMenuInGame.Show();
      } else if (ModLobby.IsMenu()) {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_ONLINE); // [mod] en ligne : retour au salon
      } else {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(9);
      }
   }

   void OnUICommand(UIWindow cSender, int nCommand) {
      super.OnUICommand(cSender, nCommand);
      if (nCommand == 0 && cSender.m_sId != null) {
         String sId = cSender.m_sId;
         if (sId.equals("ID_OK")) {
            this.OnOK();
         } else if (sId.equals("ID_CANCEL")) {
            this.Close();
         } else if (sId.equals("ID_AUTO")) {
            this.OnAuto();
         } else if (sId.equals("ID_FIELD")) {
            this.OnFieldClick();
         } else if (sId.equals("ID_SCHEME_PREV")) {
            this.OnScheme(-1);
         } else if (sId.equals("ID_SCHEME_NEXT")) {
            this.OnScheme(1);
         } else if (sId.equals("ID_SIT_PREV")) {
            this.OnSituation(-1);
         } else if (sId.equals("ID_SIT_NEXT")) {
            this.OnSituation(1);
         } else if (sId.equals("ID_ROLE_PREV")) {
            this.OnRole(-1);
         } else if (sId.equals("ID_ROLE_NEXT")) {
            this.OnRole(1);
         }
      }
   }
}
