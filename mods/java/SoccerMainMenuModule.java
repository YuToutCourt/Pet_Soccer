public class SoccerMainMenuModule extends SoccerMenuModule {
   boolean bShowObjects = true;
   static final int _UI_MAIN = 0;
   static final int _UI_CHOOSE_PLAYER = 1;
   static final int _UI_CHOOSE_TEAM = 2;
   static final int _UI_CHOOSE_COLORS = 3;
   static final int _UI_LOADING_SCREEN = 4;
   static final int _UI_OPTIONS = 5;
   static final int _UI_OPTIONS_AV = 6;
   static final int _UI_OPTIONS_CONTROLS = 7;
   static final int _UI_OPTIONS_MATCH = 8;
   static final int _UI_PRE_MATCH = 9;
   static final int _UI_TEST = 10;
   static final int _UI_CHAMPIONSHIPS = 11;
   static final int _UI_CHAMPIONSHIPS_2 = 12;
   static final int _UI_CHAMPIONSHIPS_CUP = 13;
   static final int _UI_CHAMPIONSHIPS_PRE_CUP = 14;
   static final int _UI_EXTRA = 15;
   static final int _UI_MULTIPLAYER = 16;
   static final int _UI_MULTIPLAYER_LEAGUE = 17;
   static final int _UI_LAST = 18;
   MenuBackground m_cBackground = null;
   private MenuYesNoDialog m_cYesNoDlg = null;
   MenuEditorApp m_cEditorApp;
   static int m_nCurUI = -1;
   static int m_nPrevUI = -1;
   static final int _UI_TACTICS = 18; // [mod] écran Tactique (MenuTactics)
   static final int _UI_PROFILE = 19; // [mod] écran Profil (MenuProfile)
   static final int _UI_MODS = 20; // [mod] écran Mods (MenuMods)
   static final int _UI_ACHIEVEMENTS = 21; // [mod] écran Succès (MenuAchievements)
   static final int _UI_SURVIVAL = 22; // [mod] écran Survie (MenuSurvival)
   static final int _UI_LEAGUE = 23; // [mod] mode Ligue : classement (MenuLeague)
   static final int _UI_LEAGUE_BRACKET = 24; // [mod] mode Ligue : playoffs (MenuLeagueBracket)
   static final int _UI_ONLINE = 25; // [mod] en ligne : serveur et salon (MenuOnline)
   static final int _UI_ONLINE_LEAGUE = 26; // [mod] ligue en ligne (MenuOnlineLeague)
   SoccerUserInterface[] m_tUIs = new SoccerUserInterface[27];
   MenuBall m_cMenuBall = null;
   MeshObject m_cMenuScreen = null;
   MenuObjects m_cMenuObjects = null;
   MenuOptionsObjects m_cMenuOptionsObjects = null;
   static boolean bMenuEditor = false;
   Vector m_FogColor = null;
   static boolean bBallVisible = true;
   static boolean bBkgVisible = true;
   static boolean bFirstActivation = true;
   int m_iLoopMusicID = 0;
   String m_sLoopMusicName = null;
   float m_fMusicVolume = 0.0F;

   void SetTitle(String sTitle) {
      this.m_cBackground.SetTitle(sTitle);
   }

   void ReleaseYesNoDlg(SoccerUserInterface cInterface) {
      if (cInterface != this.m_cYesNoDlg.GetOwner()) {
         Tools.CrashLogWarning(cInterface + " probuje zwolnic m_cYesNoDlg, a jest on zajety przez " + this.m_cYesNoDlg.GetOwner());
      } else {
         this.m_cYesNoDlg.SetOwner(null);
      }
   }

   MenuYesNoDialog GetYesNoDlg(SoccerUserInterface cInterface) {
      if (this.m_cYesNoDlg.GetOwner() != null) {
         Tools.CrashLogWarning("obiekt MenuYesNoDialog jest juz zajety przez " + this.m_cYesNoDlg.GetOwner());
         return null;
      } else {
         this.m_cYesNoDlg.MoveTop();
         this.m_cYesNoDlg.SetOwner(cInterface);
         return this.m_cYesNoDlg;
      }
   }

   void ShowMenuObjects() {
      if (this.m_cMenuObjects != null) {
         this.m_cMenuObjects.EnableRendering(true);
      }
   }

   void SetOptionsObjectsFocus(int nID) {
      if (this.m_cMenuOptionsObjects != null) {
         this.m_cMenuOptionsObjects.SetFocus(nID);
      }
   }

   void ShowUI(int nUI) {
      if (this.m_tUIs[nUI] == null) {
         this.LoadUI(nUI);
      }

      if (nUI == 0) {
         CoreLogic.m_bMultiplayer = false;
         CoreLogic.m_nNextLevel = -1;
         CoreLogic.m_bDemo = false;
         MenuBackground.SetPlayerType(0);
         this.ShowMenuObjects();
      } else {
         this.HideMenuObjects();
      }

      if (nUI == 5) {
         this.ShowMenuOptionsObjects();
      } else {
         this.HideMenuOptionsObjects();
      }

      if (nUI == 9) {
         MenuBackground.SetPlayerType(-1);
      }

      // [mod] en ligne : au retour d'un match, le coin de l'écran redit le nom du profil et non « joueur 1 »
      if (nUI == _UI_ONLINE || nUI == _UI_ONLINE_LEAGUE) {
         CoreLogic.m_bMultiplayer = false;
         MenuBackground.SetPlayerType(0);
      }

      this.m_cBackground.Update();
      this.HideAllUIs();
      m_nPrevUI = m_nCurUI;
      m_nCurUI = nUI;
      SoccerUserInterface cUI = this.GetUI(m_nCurUI);
      if (m_nCurUI != 4) {
         cUI.CallMethodIn("Show", 0.0F);
      } else {
         cUI.Show();
      }
   }

   public void StopMusic() {
      this.StopMusicLoop();
   }

   void OnDeactivate() {
      if (this.m_cMenuBall != null) {
         this.m_cMenuBall.UnregisterKeyRequest();
      }

      this.m_cBackground.Hide();
      this.UnregisterKeyRequest();
      this.StopMusicLoop();
      if (MenuChooseTeam.m_cModuleShowFB != null) {
         MenuChooseTeam.m_cModuleShowFB.Unload();
      }
   }

   void ShowLoadingScreen() {
      this.StopMusicLoop();
      this.m_cBackground.HideName();
      if (!SoccerGame.m_bMenuEditor) {
         this.HideAllUIs();
      }

      this.ShowUI(4);
      if (ModLobby.IsMenu()) {
         // [mod] en ligne : retour au salon après le match, ou à l'écran de la ligue en ligne
         m_nPrevUI = ModOnlineLeague.m_bMatch || ModOnlineLeague.m_bFriendMatch ? _UI_ONLINE_LEAGUE : _UI_ONLINE;
      } else if (CoreLogic.m_bChampionships) {
         m_nPrevUI = 11;
      } else if (CoreLogic.m_bMultiplayer) {
         m_nPrevUI = 17;
      } else if (CoreLogic.m_bSurvival) {
         m_nPrevUI = _UI_SURVIVAL; // [mod] retour à l'écran Survie après le match
      } else if (CoreLogic.m_bLeague) {
         m_nPrevUI = _UI_LEAGUE; // [mod] retour au classement de la ligue après le match
      } else {
         m_nPrevUI = 0;
      }

      this.ForceRender();
   }

   void OnActivate() {
      this.PlayMusic();
      super.OnActivate();
      super.m_Camera.ForceUpdate();
      this.RegisterKeyRequest();
      if (this.m_cBackground != null) {
         this.m_cBackground.Show();
      } else {
         Tools.CrashLogWarning("m_cBackground = null");
      }

      if (this.m_cMenuBall != null) {
         this.m_cMenuBall.Reset();
      }

      this.ApplyAudioSettings();
      if (bFirstActivation) {
         bFirstActivation = false;
      } else {
         this.HideLoadingScreen();
      }

      if (MenuChooseTeam.m_cModuleShowFB != null) {
         MenuChooseTeam.m_cModuleShowFB.Reload();
      }
   }

   void StopMusicLoop() {
      if (this.m_iLoopMusicID != 0) {
         this.StopWave(this.m_iLoopMusicID);
         this.m_iLoopMusicID = 0;
         this.m_sLoopMusicName = null;
         this.DisableCallMethod("PlayMusicLoop");
      }
   }

   void HideMenuOptionsObjects() {
      if (this.m_cMenuOptionsObjects != null) {
         this.m_cMenuOptionsObjects.EnableRendering(false);
      }
   }

   void SetLoopVolume(float fVolume) {
      if (this.m_iLoopMusicID != 0) {
         this.m_fMusicVolume = fVolume;
         this.SetWaveVolume(this.m_iLoopMusicID, fVolume);
      }
   }

   void SetObjectFocus(int nID) {
      if (this.m_cMenuObjects != null) {
         this.m_cMenuObjects.SetFocus(nID);
      }
   }

   void LoadUI(int nUI) {
      SoccerUserInterface cUI = null;
      int nResolution = SoccerGame.GetResolution();
      switch (nUI) {
         case 0:
            cUI = (MenuMain)this.CreateObject("MenuMain");
            break;
         case 1:
            cUI = (SoccerUserInterface)this.CreateObject("MenuChoosePlayer");
            break;
         case 2:
            cUI = (MenuChooseTeam)this.CreateObject("MenuChooseTeam");
            break;
         case 3:
            cUI = (SoccerUserInterface)this.CreateObject("MenuChooseTeamColors");
            break;
         case 4:
            cUI = (SoccerUserInterface)this.CreateObject("LoadingScreen");
            break;
         case 5:
            cUI = (SoccerUserInterface)this.CreateObject("MenuOptions");
            break;
         case 6:
            cUI = (SoccerUserInterface)this.CreateObject("MenuOptionsAV");
            break;
         case 7:
            cUI = (SoccerUserInterface)this.CreateObject("MenuOptionsControls");
            break;
         case 8:
            cUI = (SoccerUserInterface)this.CreateObject("MenuOptionsMatch");
            break;
         case 9:
            cUI = (SoccerUserInterface)this.CreateObject("MenuPreMatch");
         case 10:
         default:
            break;
         case 11:
            cUI = (SoccerUserInterface)this.CreateObject("MenuChampionships");
            break;
         case 12:
            cUI = (SoccerUserInterface)this.CreateObject("MenuChampionships2");
            break;
         case 13:
            cUI = (SoccerUserInterface)this.CreateObject("MenuChampionshipsCup");
            break;
         case 14:
            cUI = (SoccerUserInterface)this.CreateObject("MenuChampionshipsPreCup");
            break;
         case 15:
            cUI = (SoccerUserInterface)this.CreateObject("MenuExtra");
            break;
         case 16:
            cUI = (SoccerUserInterface)this.CreateObject("MenuMultiplayer");
            break;
         case 17:
            cUI = (SoccerUserInterface)this.CreateObject("MenuMultiplayerLeague");
            break;
         case 18:
            cUI = (SoccerUserInterface)this.CreateObject("MenuTactics"); // [mod]
            break;
         case 19:
            cUI = (SoccerUserInterface)this.CreateObject("MenuProfile"); // [mod]
            break;
         case 20:
            cUI = (SoccerUserInterface)this.CreateObject("MenuMods"); // [mod]
            break;
         case 21:
            cUI = (SoccerUserInterface)this.CreateObject("MenuAchievements"); // [mod]
            break;
         case 22:
            cUI = (SoccerUserInterface)this.CreateObject("MenuSurvival"); // [mod]
            break;
         case 23:
            cUI = (SoccerUserInterface)this.CreateObject("MenuLeague"); // [mod]
            break;
         case 24:
            cUI = (SoccerUserInterface)this.CreateObject("MenuLeagueBracket"); // [mod]
            break;
         case 25:
            cUI = (SoccerUserInterface)this.CreateObject("MenuOnline"); // [mod]
            break;
         case 26:
            cUI = (SoccerUserInterface)this.CreateObject("MenuOnlineLeague"); // [mod]
      }

      if (cUI == null) {
         Tools.CrashLogError("Unable to create ui of id " + nUI);
      }

      Vector vCursorPos = null;
      if (m_nCurUI != -1) {
         vCursorPos = this.m_tUIs[m_nCurUI].GetCursorPos();
         cUI.SetInitialCursorPos(vCursorPos);
      } else {
         cUI.SetInitialCursorPos(new Vector(GameObject.GetScreenWidth() / 2.0F, GameObject.GetScreenHeight() / 2.0F, 0.0F));
      }

      cUI.Load(this, nResolution);
      this.m_tUIs[nUI] = cUI;
      cUI.MoveTop();
   }

   SoccerUserInterface GetUI(int nUI) {
      if (nUI >= 0 && nUI <= this.m_tUIs.length) {
         if (this.m_tUIs[nUI] == null) {
            this.LoadUI(nUI);
         }

         return this.m_tUIs[nUI];
      } else {
         return null;
      }
   }

   void PlayMusic() {
      int nState = CoreLogic._MENU_MUSIC_NORMAL;
      if (m_nCurUI == 13) {
         nState = CoreLogic._MENU_MUSIC_WIN;
      }

      PlayerSave player = CoreLogic.GetPlayer();
      if (player != null) {
         Championships ch = player.m_cChampionships;
         if (ch != null) {
            if (m_nCurUI == 11 && ((MenuChampionships)this.GetUI(m_nCurUI)).m_nResult == 0) {
               nState = CoreLogic._MENU_MUSIC_LOOSE;
            }

            if (m_nCurUI == 12 && ((MenuChampionships2)this.GetUI(m_nCurUI)).m_nResult == 0) {
               nState = CoreLogic._MENU_MUSIC_LOOSE;
            }
         }
      }

      this.PlayMusic(nState, SoccerGame.GetMusicVolume());
   }

   void ActivateMenuCamera() {
      super.ActivateMenuCamera();
      this.ListenerOnViewActive(super.m_View, true);
   }

   void HideLoadingScreen() {
      if (this.m_cBackground != null) {
         this.m_cBackground.ShowName();
      }

      if (!SoccerGame.m_bMenuEditor) {
         this.ShowUI(m_nPrevUI);
      }

      this.ForceRender();
   }

   void PlayMusic(int nState, float fVolume) {
      this.StopMusicLoop();
      this.m_fMusicVolume = fVolume;
      this.m_sLoopMusicName = CoreLogic.GetMenuMusicName(nState);
      GameObject.LogLine("MenuModule::PlayMusic: " + this.m_fMusicVolume);
      this.PlayMusicLoop();
   }

   void OnInputKeyDebug(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      if (!bKeyState) {
         switch (nKeyCode) {
            case 21:
               GameObject.CrashLog(" YYYY ");
               break;
            case 63:
               this.HideAllUIs();
               break;
            case 64:
               if (this.m_cBackground != null) {
                  if (this.m_cBackground.IsActuallyVisible()) {
                     this.m_cBackground.Hide();
                  } else {
                     this.m_cBackground.Show();
                  }
               }
               break;
            case 65:
               if (this.m_cMenuBall != null) {
                  bBallVisible = !bBallVisible;
                  this.m_cMenuBall.EnableRendering(bBallVisible);
               }
               break;
            case 66:
               if (this.m_cMenuScreen != null) {
                  bBkgVisible = !bBkgVisible;
                  this.m_cMenuScreen.EnableRendering(bBkgVisible);
               }
               break;
            case 67:
               if (!this.GetUI(14).IsVisible()) {
                  this.ShowUI(14);
               } else {
                  this.HideAllUIs();
               }
         }
      }
   }

   void HideAllUIs() {
      for (int i = 0; i < this.m_tUIs.length; i++) { // [mod] 18 -> m_tUIs.length (écran Tactique)
         if (this.m_tUIs[i] != null) {
            this.m_tUIs[i].Hide();
         }
      }
   }

   public void PlayMusicLoop() {
      if (this.m_sLoopMusicName == null) {
         GameObject.LogLine("Nazwa muzyki == null");
      } else {
         if (this.m_iLoopMusicID != 0) {
            this.StopWave(this.m_iLoopMusicID);
            this.m_iLoopMusicID = 0;
         }

         this.m_iLoopMusicID = this.PlayWave(this.m_sLoopMusicName);
         if (this.m_iLoopMusicID == 0) {
            GameObject.LogLine("Nie udalo sie odpalic dzwieku:" + this.m_sLoopMusicName);
         } else {
            this.SetLoopVolume(this.m_fMusicVolume);
            GameObject.LogLine("MenuModule::m_fMusicVolume: " + this.m_fMusicVolume);
            float fMusicDuration = this.GetWaveDuration(this.m_iLoopMusicID);
            this.CallMethodIn("PlayMusicLoop", fMusicDuration);
         }
      }
   }

   void HideMenuObjects() {
      if (this.m_cMenuObjects != null) {
         this.m_cMenuObjects.EnableRendering(false);
      }
   }

   void HandleUIClick(int nBtnID) {
      switch (nBtnID) {
         case 0:
      }
   }

   void ShowMenuOptionsObjects() {
      if (this.m_cMenuOptionsObjects != null) {
         this.m_cMenuOptionsObjects.EnableRendering(true);
      }
   }

   public void OnCreate() {
      super.OnCreate();
      this.m_FogColor = this.FogGetColor();
      int nResolution = SoccerGame.GetResolution();
      if (SoccerGame.m_bMenuEditor) {
         CoreLogic.SetPlayer(GameSerializeSettings.Logic.GetPlayer(0));
         SoccerUserInterface cUI = (SoccerUserInterface)this.CreateObject("MenuChampionshipsPreCup");
         cUI.Load(this, nResolution);
         this.m_tUIs[14] = cUI;
         this.m_cBackground = (MenuBackground)this.CreateObject("MenuBackground");
         this.m_cBackground.Load(this, nResolution);
         this.m_cEditorApp = new MenuEditorApp();
         this.m_cEditorApp.Run();
         this.m_cEditorApp.SetModule(this);
      } else {
         SoccerUserInterface cUI = null;
         this.m_cYesNoDlg = (MenuYesNoDialog)this.CreateObject("MenuYesNoDialog");
         this.m_cYesNoDlg.Load(this, nResolution);
         this.LoadUI(0);
         this.LoadUI(4);
         m_nCurUI = 0;
         if (SoccerGame.m_bMenuON) {
            this.LoadUI(1);
            this.LoadUI(2);
            m_nCurUI = 1;
         }

         this.m_cBackground = (MenuBackground)this.CreateObject("MenuBackground");
         this.m_cBackground.Load(this, nResolution);
         if (!SoccerGame.m_bDebug && SoccerGame.m_bMenuON) {
            m_nCurUI = 1;
         }

         // [mod] test du salon sans souris (NetAuto dans debugconf.scr) : dernier profil utilisé, droit au menu principal
         java.util.Vector vPlayers = GameSerializeSettings.Logic.m_Players;
         if (ModLobby.m_nAuto != 0 && SoccerGame.m_bMenuON && vPlayers.size() > 0) {
            int nLast = SoccerGame.m_cSettings.m_nLastPlayerIndex;
            CoreLogic.SetPlayer((PlayerSave)vPlayers.elementAt(nLast >= 0 && nLast < vPlayers.size() ? nLast : 0));
            m_nCurUI = 0;
         }

         this.ShowUI(m_nCurUI);
      }

      super.m_Camera.InitObject();
      if (this.bShowObjects) {
         this.m_cMenuScreen = (MeshObject)this.CreateObject("MeshObject");
         this.m_cMenuScreen.LoadMesh("data/menu/ekran.3da");
         this.AddControlObject(this.m_cMenuScreen);
         this.m_cMenuScreen.SetFacingTowardsObserver(true);
         this.m_cMenuBall = (MenuBall)this.CreateObject("MenuBall");
         this.AddControlObject(this.m_cMenuBall);
         super.m_Camera.SetObserved(this.m_cMenuScreen);
      }

      if (!SoccerGame.m_bMenuEditor && this.bShowObjects) {
         this.m_cMenuObjects = (MenuObjects)this.CreateObject("MenuObjects");
         this.m_cMenuOptionsObjects = (MenuOptionsObjects)this.CreateObject("MenuOptionsObjects");
         this.AddControlObject(this.m_cMenuObjects);
         this.AddControlObject(this.m_cMenuOptionsObjects);
      }

      GameObject.CrashLog("SoccerMainMenuModule.OnCreate()\n");
   }

   void OnInputKey(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      if (SoccerGame.m_bDebug) {
         this.OnInputKeyDebug(nKeyCode, bKeyState, nKeyAscii);
      }

      if (!bKeyState) {
         switch (nKeyCode) {
            case 0:
         }
      }
   }

   public void InitModule() {
      super.InitModule();
   }

   void OnChangeResolution() {
      SoccerGame.GetCommModule().OnChangeResolution();
      MenuChooseTeam.m_cModuleShowFB.OnChangeResolution();
      int nResolution = SoccerGame.GetResolution();
      switch (nResolution) {
         case 0:
         case 1:
         case 2:
         default:
            super.OnChangeResolution();
            this.m_cYesNoDlg.Remove();
            this.RemoveSprite(this.m_cYesNoDlg);
            this.m_cYesNoDlg.Load(this, nResolution);
            SoccerUserInterface cUI = null;

            for (int i = 0; i < this.m_tUIs.length; i++) { // [mod] 18 -> m_tUIs.length
               cUI = this.m_tUIs[i];
               if (cUI != null) {
                  cUI.Remove();
                  this.RemoveSprite(cUI);
                  cUI.Load(this, nResolution);
               }
            }

            this.m_cBackground.Remove();
            this.RemoveSprite(this.m_cBackground);
            this.m_cBackground.Load(this, nResolution);
            this.m_cBackground.Show();
            this.m_cBackground.ShowName();
            this.ShowUI(0);
      }
   }
}
