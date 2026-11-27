// [mod] Changement de joueur des équipes humaines (voir notes/tactics.md, « Changement de joueur ») :
// - touche passe sans ballon : coéquipier le plus proche du ballon (option : dans la direction des flèches tenues),
//   puis le suivant si on appuie plusieurs fois ;
// - verrou : après un changement manuel, le jeu ne change plus de joueur tout seul pendant SwitchLock secondes ;
// - 6 touches directes vers les joueurs 2 à 7 (numéros de l'écran Tactique), réglées dans Options > Contrôles
//   et enregistrées dans mods.dat ;
// - numéros des joueurs humains sur la mini-carte (ID_MAPNUM_<côté>_<numéro> dans Hud*.scr), et au-dessus
//   des têtes en option (menu Mods, ID_NUM_<côté>_<numéro>).
// Réglages dans data/switch.scr.
class ModSwitch {
   static final String _FILE = "data/switch.scr";
   static final int _NUMBERS = 6; // joueurs de champ, numéros 2..7 (le gardien est le 1)
   static final int _FIRST_NUMBER = 2;
   static boolean m_bLoaded = false;
   // demi-angle du cône de recherche autour de la direction tenue (degrés) ; 0 = pas de choix par direction
   static float m_fCone = 0.0F;
   // secondes sans changement automatique après un changement manuel
   static float m_fLock = 1.2F;
   // secondes pendant lesquelles on ne revient pas au joueur qu'on vient de quitter
   static float m_fNoBack = 0.6F;
   // hauteur des numéros au-dessus des pieds (unités du terrain)
   static float m_fNumberHeight = 120.0F;
   // en plus pour le joueur contrôlé, au-dessus du losange d'origine
   static float m_fFocusExtra = 60.0F;
   // numéros au-dessus des têtes (menu Mods, mods.dat : PlayerNumbers), désactivés par défaut
   static boolean m_bShowNumbers = false;
   // numéros sur la mini-carte (switch.scr : MapNumbers), et écart en pixels entre le point et le numéro
   static boolean m_bMapNumbers = true;
   static float m_fMapNumberGap = 1.0F;
   // touches directes par jeu de touches (0 = joueur 1, 1 = joueur 2) : code DIK, bouton de manette (>= 1000), -1 = aucune.
   // Par défaut, le joueur 1 a les touches 1 à 6 de la rangée du haut (DIK 2..7).
   static int[][] m_tKeys = new int[][]{{2, 3, 4, 5, 6, 7}, {-1, -1, -1, -1, -1, -1}};
   static final int[][] _DEFAULT_KEYS = new int[][]{{2, 3, 4, 5, 6, 7}, {-1, -1, -1, -1, -1, -1}};

   static void Load() {
      if (m_bLoaded) {
         return;
      }

      m_bLoaded = true;
      ParseKeyword root = GameObject.ParseFile(_FILE);
      if (root == null) {
         GameObject.CrashLog("[mod] " + _FILE + " absent, valeurs par defaut\n");
         return;
      }

      for (ParseKeyword key = root; key != null; key = key.GetNext()) {
         if (key.Equals("SwitchCone")) {
            m_fCone = Math.max(0.0F, Math.min(180.0F, key.GetFParam(0)));
         } else if (key.Equals("SwitchLock")) {
            m_fLock = Math.max(0.0F, key.GetFParam(0));
         } else if (key.Equals("SwitchNoBack")) {
            m_fNoBack = Math.max(0.0F, key.GetFParam(0));
         } else if (key.Equals("NumberHeight")) {
            m_fNumberHeight = key.GetFParam(0);
         } else if (key.Equals("NumberFocusExtra")) {
            m_fFocusExtra = key.GetFParam(0);
         } else if (key.Equals("MapNumbers")) {
            m_bMapNumbers = key.GetIParam(0) != 0;
         } else if (key.Equals("MapNumberGap")) {
            m_fMapNumberGap = key.GetFParam(0);
         }
      }

      GameObject.CrashLog("[mod] changement de joueur : cone " + m_fCone + ", verrou " + m_fLock + " s, anti-retour " + m_fNoBack + " s\n");
   }

   private static int m_nDebugTest = -1; // ControlsTest(1) dans debugconf.scr : ouvre l'écran Contrôles au menu (une fois)

   static boolean GetDebugTest() {
      if (m_nDebugTest < 0) {
         m_nDebugTest = 0;

         for (ParseKeyword key = GameObject.ParseFile("debugconf.scr"); key != null; key = key.GetNext()) {
            if (key.Equals("ControlsTest")) {
               m_nDebugTest = key.GetIParam(0);
            }
         }

         return m_nDebugTest > 0;
      }

      return false;
   }

   // ------------------------------------------------------------------ mods.dat

   // ModOptions.Set : clés SelKey<jeu>_<numéro> et PlayerNumbers. Renvoie vrai si la clé est à nous.
   static boolean SetOption(String sKey, int n) {
      if (sKey.equals("PlayerNumbers")) {
         m_bShowNumbers = n != 0;
         return true;
      }

      if (sKey.startsWith("SelKey") && sKey.length() == 9 && sKey.charAt(7) == '_') {
         int nSet = sKey.charAt(6) - '0';
         int nIndex = sKey.charAt(8) - '0' - _FIRST_NUMBER;
         if (nSet >= 0 && nSet < 2 && nIndex >= 0 && nIndex < _NUMBERS) {
            m_tKeys[nSet][nIndex] = n;
            return true;
         }
      }

      return false;
   }

   static String SaveLines() {
      String s = "PlayerNumbers=" + (m_bShowNumbers ? 1 : 0) + "\r\n";

      for (int nSet = 0; nSet < 2; nSet++) {
         for (int i = 0; i < _NUMBERS; i++) {
            s = s + "SelKey" + nSet + "_" + (i + _FIRST_NUMBER) + "=" + m_tKeys[nSet][i] + "\r\n";
         }
      }

      return s;
   }

   static String GetSummary() {
      String s = "numeros " + m_bShowNumbers + ", touches";

      for (int nSet = 0; nSet < 2; nSet++) {
         s = s + " j" + (nSet + 1) + "=";

         for (int i = 0; i < _NUMBERS; i++) {
            s = s + (i > 0 ? "," : "") + m_tKeys[nSet][i];
         }
      }

      return s;
   }

   // ------------------------------------------------------------------ touches directes

   // TeamController.OnInputKey / OnInputJoyButton : renvoie vrai si la touche est une touche directe de cette manette
   static boolean OnKey(TeamController cController, int nKeyCode, boolean bKeyState) {
      int nSet = cController.m_nKeySet;
      if (nSet < 0 || nSet > 1 || nKeyCode < 0) {
         return false;
      }

      int nIndex = -1;

      for (int i = 0; i < _NUMBERS; i++) {
         if (m_tKeys[nSet][i] == nKeyCode) {
            nIndex = i;
         }
      }

      if (nIndex < 0) {
         return false;
      }

      if (bKeyState) {
         // en ligne, le client demande à l'hôte, qui simule
         if (!ModNet.ClientSelect(nIndex) && cController.m_cTeam != null) {
            Select(cController.m_cTeam, nIndex + _FIRST_NUMBER);
         }
      }

      return true;
   }

   // vrai si nKeyCode est une touche directe (d'un des deux jeux de touches) : les raccourcis de debug du jeu
   // d'origine sur les touches 1 à 7 (AIFootBaller, AIReferee) l'ignorent alors
   static boolean IsSelectKey(int nKeyCode) {
      if (nKeyCode < 0) {
         return false;
      }

      for (int nSet = 0; nSet < 2; nSet++) {
         for (int i = 0; i < _NUMBERS; i++) {
            if (m_tKeys[nSet][i] == nKeyCode) {
               return true;
            }
         }
      }

      return false;
   }

   // prend le joueur numéro nNumber (2..7) ; ignoré hors du jeu, si l'équipe a le ballon, ou si le joueur est occupé
   static boolean Select(AITeam cTeam, int nNumber) {
      if (!cTeam.m_bUserControled || !AIMatch.IsMatchState(0) || SoccerModule.m_cMatch == null || SoccerModule.m_cMatch.IsWaiting()) {
         return false;
      }

      AIFootBaller[] tFB = ModTactics.GetNumbered(cTeam);
      int nIndex = nNumber - _FIRST_NUMBER;
      if (tFB == null || nIndex < 0 || nIndex >= _NUMBERS) {
         return false;
      }

      AIFootBaller cFB = tFB[nIndex];
      FootBaller cOwner = GameField.m_cBall.GetOwner();
      if (cFB == cTeam.m_cFocusFBaller || cFB.Busy() || cOwner != null && cOwner.m_cTeam == cTeam) {
         return false;
      }

      cTeam.SwitchTo(cFB);
      Log(cTeam, "touche " + nNumber, cFB);
      return true;
   }

   // ------------------------------------------------------------------ touche passe sans ballon

   // AITeam.ChangeActive, équipes humaines : joueur à prendre, ou null pour garder le choix du jeu d'origine.
   // Le coéquipier le plus proche du ballon (position réelle du ballon) ; si SwitchCone > 0 et qu'une flèche est
   // tenue, le plus proche dans le cône de cette direction d'abord. Le joueur qu'on vient de quitter est sauté
   // pendant SwitchNoBack : appuyer plusieurs fois passe au suivant.
   static AIFootBaller Choose(AITeam cTeam) {
      Load();
      AIFootBaller cCur = (AIFootBaller)cTeam.m_cFocusFBaller;
      AIFootBaller cBack = cTeam.m_cSwitchLeft != null && Now() - cTeam.m_fSwitchLeftTime < m_fNoBack ? cTeam.m_cSwitchLeft : null;
      java.util.Vector vCand = new java.util.Vector();

      for (int i = 0; i < cTeam.m_vFootBallers.size(); i++) {
         AIFootBaller cFB = (AIFootBaller)cTeam.m_vFootBallers.elementAt(i);
         if (!cFB.IsGoalKeeper() && !cFB.IsReferee() && cFB != cCur && cFB != SoccerModule.m_cMatch.m_cActiveLossFB && !cFB.Busy()) {
            vCand.addElement(cFB);
         }
      }

      if (vCand.size() > 1 && cBack != null) {
         vCand.removeElement(cBack);
      }

      if (vCand.isEmpty()) {
         return null;
      }

      Vector vBall = GameField.m_cBall.GetPosition();
      TeamController cController = cTeam.GetTeamController();
      Vector vDir = m_fCone > 0.0F && cController != null && cController._KEY_STATES != 0 ? cController.GetFieldDirFromKeys() : null;

      // 1. (option) flèches tenues : le plus proche du ballon dans le cône de la direction (pénalité selon l'angle)
      if (vDir != null) {
         float fDirLen = (float)Math.sqrt(vDir.fX * vDir.fX + vDir.fZ * vDir.fZ);
         if (fDirLen > 0.001F) {
            AIFootBaller cBest = null;
            float fBest = 0.0F;
            float fCosCone = (float)Math.cos(m_fCone * Math.PI / 180.0);

            for (int i = 0; i < vCand.size(); i++) {
               AIFootBaller cFB = (AIFootBaller)vCand.elementAt(i);
               Vector vPos = cFB.GetPosition();
               float fX = vPos.fX - vBall.fX;
               float fZ = vPos.fZ - vBall.fZ;
               float fDist = (float)Math.sqrt(fX * fX + fZ * fZ);
               float fCos = fDist < 1.0F ? 1.0F : (fX * vDir.fX + fZ * vDir.fZ) / (fDist * fDirLen);
               if (fCos >= fCosCone) {
                  // 1 dans l'axe, 2 au bord du cône
                  float fScore = fDist * (1.0F + (1.0F - fCos) / Math.max(0.0001F, 1.0F - fCosCone));
                  if (cBest == null || fScore < fBest) {
                     cBest = cFB;
                     fBest = fScore;
                  }
               }
            }

            if (cBest != null) {
               Log(cTeam, "direction", cBest);
               return cBest;
            }
         }
      }

      // 2. le plus proche du ballon
      AIFootBaller cBest = null;
      float fBest = 0.0F;

      for (int i = 0; i < vCand.size(); i++) {
         AIFootBaller cFB = (AIFootBaller)vCand.elementAt(i);
         Vector vPos = cFB.GetPosition();
         float fX = vPos.fX - vBall.fX;
         float fZ = vPos.fZ - vBall.fZ;
         float fDist = fX * fX + fZ * fZ;
         if (cBest == null || fDist < fBest) {
            cBest = cFB;
            fBest = fDist;
         }
      }

      if (cBest != null) {
         Log(cTeam, cBack != null ? "suivant" : "proche du ballon", cBest);
      }

      return cBest;
   }

   // ------------------------------------------------------------------ verrou

   // AITeam.UpdateParams : le jeu peut-il relâcher le blocage du changement automatique ?
   static boolean CanUnblock(AITeam cTeam) {
      return Now() >= cTeam.m_fSwitchLockEnd;
   }

   static void SetLock(AITeam cTeam) {
      Load();
      cTeam.m_fSwitchLockEnd = Now() + m_fLock;
   }

   static float Now() {
      return SoccerGame.GetSoccerModule().GetTime();
   }

   static void Log(AITeam cTeam, String sWhy, AIFootBaller cFB) {
      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("[mod] switch " + cTeam.m_sName + " : " + sWhy + " -> n" + cFB.m_nNumberInTeam + "\n");
      }
   }

   // ------------------------------------------------------------------ numéros des joueurs

   // étiquettes [côté][joueur 2..7] : au-dessus des têtes (ID_NUM_*) et sur la mini-carte (ID_MAPNUM_*), dans Hud*.scr
   static UIStatic[][] m_tLabels = null;
   static UIStatic[][] m_tMapLabels = null;
   static PetSoccerHud m_cLabelsHud = null;

   static UIStatic[][] FindLabels(PetSoccerHud cHud, String sPrefix) {
      UIStatic[][] t = new UIStatic[2][_NUMBERS];

      for (int nSide = 0; nSide < 2; nSide++) {
         for (int i = 0; i < _NUMBERS; i++) {
            UIWindow cWnd = cHud.GetElem(sPrefix + nSide + "_" + (i + _FIRST_NUMBER));
            t[nSide][i] = cWnd instanceof UIStatic ? (UIStatic)cWnd : null;
            // déclarées visibles dans Hud*.scr (comme les flèches), sinon Show() ne les dessine pas : cachées ici
            if (cWnd != null) {
               cWnd.Hide();
            }
         }
      }

      return t;
   }

   // vert ou rouge comme la flèche de l'équipe, blanc pour le joueur contrôlé
   static void SetLabelColor(UIStatic cLabel, PetSoccerHud cHud, Team cTeam, boolean bFocus) {
      if (bFocus) {
         cLabel.SetTextColor(1.0F, 1.0F, 1.0F);
      } else if (cTeam.m_cHudArrow == cHud.cArrowRed) {
         cLabel.SetTextColor(1.0F, 0.55F, 0.45F);
      } else {
         cLabel.SetTextColor(0.6F, 1.0F, 0.5F);
      }
   }

   // PetSoccerHud.OnFrame
   static void UpdateNumbers(PetSoccerHud cHud) {
      if (m_cLabelsHud != cHud) {
         m_cLabelsHud = cHud;
         m_tLabels = FindLabels(cHud, "ID_NUM_");
         m_tMapLabels = FindLabels(cHud, "ID_MAPNUM_");
      }

      Load();
      AIMatch cMatch = SoccerModule.m_cMatch;
      HudField cMap = cHud.cFieldMap;
      boolean bMapOn = m_bMapNumbers && cMap != null && cMap.m_bActive;

      for (int nSide = 0; nSide < 2; nSide++) {
         Team cTeam = cMatch != null ? cMatch.GetTeam(nSide) : null;
         boolean bTeam = cTeam != null && cTeam.m_bUserControled && !CoreLogic.m_bDemo && cHud.m_bVisible
            && (AIMatch.IsMatchState(0) || AIMatch.IsMatchState(7))
            // en ligne, chacun ne voit que les numéros de son équipe (gauche au coup d'envoi pour l'hôte, droite pour
            // le client), y compris après le changement de côté de la mi-temps
            && (!ModNet.IsOnline() || ModNet.IsMyTeam(cTeam));
         AIFootBaller[] tFB = bTeam && (m_bShowNumbers || bMapOn) ? ModTactics.GetNumbered(cTeam) : null;
         int nSet = tFB != null ? GetKeySet(cTeam) : 0;

         for (int i = 0; i < _NUMBERS; i++) {
            AIFootBaller cFB = tFB != null ? tFB[i] : null;
            boolean bFocus = cFB != null && cFB == cTeam.m_cFocusFBaller;
            if (cFB != null) {
               // le texte suit la touche directe choisie dans Options > Commandes
               String sText = GetLabelText(nSet, i);
               SetLabelText(m_tLabels[nSide][i], sText);
               SetLabelText(m_tMapLabels[nSide][i], sText);
            }

            UpdateHeadLabel(m_tLabels[nSide][i], m_bShowNumbers ? cFB : null, bFocus, cHud, cTeam);
            UpdateMapLabel(m_tMapLabels[nSide][i], bMapOn ? cFB : null, bFocus, cHud, cTeam);
         }
      }
   }

   // jeu de touches de l'équipe (0 = joueur 1, 1 = joueur 2) ; en ligne ou sans manette connue, celui du joueur 1
   static int GetKeySet(Team cTeam) {
      TeamController cController = cTeam.GetTeamController();
      int nSet = cController != null ? cController.m_nKeySet : -1;
      return nSet == 1 && !ModNet.IsOnline() ? 1 : 0;
   }

   // nom court de la touche directe du joueur i (0..5) : « 1 », « A », « N7 », « Feu3 »… ; sans touche, le numéro
   // Tactique - 1 comme avant (le joueur 2 n'a pas de touches par défaut)
   static String GetLabelText(int nSet, int i) {
      int nKey = m_tKeys[nSet][i];
      String s = nKey >= 0 ? KbSettings.GetKeyString(nKey) : "";
      if (s == null || s.length() == 0) {
         return String.valueOf(i + _FIRST_NUMBER - 1);
      }

      if (s.startsWith("Numpad")) {
         return "N" + s.substring(6);
      }

      if (s.startsWith("Num ")) {
         return "N" + s.substring(4);
      }

      if (s.equals("LControl")) {
         return "LCtrl";
      }

      return s;
   }

   // SetText seulement quand le texte change (appelé à chaque image)
   static void SetLabelText(UIStatic cLabel, String sText) {
      if (cLabel != null && !sText.equals(cLabel.GetText())) {
         cLabel.SetText(sText);
      }
   }

   // option du menu Mods : numéro au-dessus de la tête
   static void UpdateHeadLabel(UIStatic cLabel, AIFootBaller cFB, boolean bFocus, PetSoccerHud cHud, Team cTeam) {
      if (cLabel == null) {
         return;
      }

      if (cFB == null || !cFB.IsInFrustum()) {
         cLabel.Hide();
         return;
      }

      // joueur contrôlé : un peu plus haut, au-dessus du losange d'origine
      Vector vFeet = cFB.GetPositionVectorVolatile();
      Vector vHead = new Vector(vFeet.fX, vFeet.fY + m_fNumberHeight + (bFocus ? m_fFocusExtra : 0.0F), vFeet.fZ);
      // projection de la caméra active, comme MeshObject.ShowObjectName (PointToScreenClampToFrustum de la
      // caméra du match donnait des positions décalées)
      Camera cCam = cFB.GetModule() != null ? cFB.GetModule().GetActiveCamera() : null;
      Vector vScreen = cCam != null ? cFB.PointToScreen(cCam, vHead) : null;
      // tête hors de l'écran : pas d'étiquette
      if (vScreen == null || vScreen.fX <= 0.0F || vScreen.fY <= 0.0F || vScreen.fX >= GameObject.GetScreenWidth() || vScreen.fY >= GameObject.GetScreenHeight()) {
         cLabel.Hide();
         return;
      }

      Vector vSize = cLabel.GetSize();
      cLabel.SetPos(new Vector(vScreen.fX - vSize.fX / 2.0F, vScreen.fY - vSize.fY, 0.0F));
      SetLabelColor(cLabel, cHud, cTeam, bFocus);
      cLabel.Show();
   }

   // numéro à droite du point du joueur sur la mini-carte (HudField, point placé par UpdateFBPosition)
   static void UpdateMapLabel(UIStatic cLabel, AIFootBaller cFB, boolean bFocus, PetSoccerHud cHud, Team cTeam) {
      if (cLabel == null) {
         return;
      }

      UIWindow cPoint = cFB != null ? cFB.m_cMapPoint : null;
      if (cPoint == null || !cPoint.m_bVisible || cPoint.m_wndParent == null || !cPoint.m_wndParent.m_bVisible) {
         cLabel.Hide();
         return;
      }

      Vector vPoint = cPoint.GetPos();
      Vector vPointSize = cPoint.GetSize();
      Vector vSize = cLabel.GetSize();
      cLabel.SetPos(new Vector(vPoint.fX + vPointSize.fX + m_fMapNumberGap, vPoint.fY + (vPointSize.fY - vSize.fY) / 2.0F, 0.0F));
      SetLabelColor(cLabel, cHud, cTeam, bFocus);
      cLabel.Show();
   }
}
