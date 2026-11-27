import java.util.Random;

public class SoccerGame extends Game {
   Cheat m_cCheat = null;
   int nFirstLevel = 14;
   static boolean m_bMenuON = true;
   static boolean m_bMenuEditor = false;
   static boolean m_bShowFPS = false;
   static boolean m_bDebug = false;
   static boolean m_bRecordingEnabled = false;
   static int m_nDifficulty = 2;
   static boolean m_bForceMultiplayer = false;
   static boolean m_bForceDemo = false;
   static final boolean _NOCD = true;
   static SoccerModule activeLevelModule = null;
   static SoccerGame game = null;
   static ObjectManager m_cObjectManager = null;
   static MovieModule movieModule = null;
   static LoadingScreen m_cLoadingScreen = null;
   static boolean m_bEscPressed = false;
   static GameSerializeSettings m_cSettings = new GameSerializeSettings();
   static SoccerMainMenuModule firstMenuModule = null;
   static CommentatorModule m_cCommentatorModule = null;
   static final int _ACTIVE_LOGO = 0;
   static final int _ACTIVE_MENU = 1;
   static final int _ACTIVE_GAME_MODULE = 2;
   static final int _ACTIVE_INTRO = 3;
   static final int _ACTIVE_OUTRO = 4;
   int activeMenu = 0;
   static Random m_Random = new Random();
   public boolean m_bExitDirectToMainMenu = false;
   String m_CheatString = new String("");

   public void LoadFirstGameModule() {
      movieModule.Deactivate();
   }

   public void LoadDemoModule() {
      CoreLogic log = GetCoreLogic();
      CoreLogic.m_bDemo = true;
      log.SetNextLevel(this.nFirstLevel);
      this.RunModule();
   }

   public void DestroyMovieModule() {
      if (movieModule != null) {
         movieModule.delete();
         movieModule = null;
      }
   }

   static CommentatorModule GetCommModule() {
      return m_cCommentatorModule;
   }

   public void EndModule() {
      CoreLogic log = GetCoreLogic();
      boolean bEscape = m_bEscPressed;
      m_bEscPressed = false;
      this.activeMenu = 1;
      if (activeLevelModule != null) {
         activeLevelModule.Deactivate();
         firstMenuModule.CallMethodIn("DeleteLevelModule", 0.0F);
      }

      if (this.m_bExitDirectToMainMenu) {
      }

      this.m_bExitDirectToMainMenu = false;
      firstMenuModule.Activate();
   }

   static void DecModule() {
      m_cObjectManager.ctModuleCounter += -1;
   }

   static void DecInGame() {
      m_cObjectManager.ctInGameCounter += -1;
   }

   static CoreLogic GetCoreLogic() {
      return GameSerializeSettings.Logic;
   }

   static void SetDetails(int d) {
      m_cSettings.SetDetailLevel(d);
   }

   static void DecBall() {
      m_cObjectManager.ctBallCounter += -1;
   }

   static void IncAction() {
      m_cObjectManager.ctActionCounter++;
   }

   static void DecEvent() {
      m_cObjectManager.ctEventCounter += -1;
   }

   static int GetDetails() {
      return m_cSettings.m_Details;
   }

   static void IncTeam() {
      m_cObjectManager.ctTeamCounter++;
   }

   void PlayCS() {
      this.PlaySound("Data/Sounds/bonus.wav");
   }

   public void SetDemo(boolean bDemo) {
      m_bForceDemo = bDemo;
   }

   public void OnPreInitialize(String[] aParams) {
      String sTexts = "data\\texts.scr";
      if (!Text.FromFile(sTexts)) {
         GameObject.CrashLog("Missing or corrupt file: " + sTexts + "\n");
      }

      this.SetGameName(Text.Get("&GameName&"));
      game = this;
      this.LoadDebugConfig();
      ModOptions.Load(); // [mod] réglages du menu Mods (mods.dat)
      if (aParams != null) {
         this.ReadExecuteParams(aParams);
      }

      if (Game._bSafeMode) {
         this.SetBPP(16);
         this.SetTextureBPP(16);
         this.SetTextureFiltering(128);
         this.SetFullscreen(true);
         this.ApplyVideoSettings();
         this.SaveVideoSettings(null);
      }

      Game.bCanShowObjectNames = true;
      this.TimersDefaultFrozen(false);
   }

   public void OnInitialize() {
      if (m_bDebug || m_bRecordingEnabled) {
         super.OnInitialize();
      }

      Game.SetLoadingOnCreatePercentage(0.1F);
      this.RegisterKeyRequest();
      this.RegisterMouseButtonRequest(); // [mod] OnInputMouseButton, pour les champs de saisie (UIEdit)
      this.SetDefaultSoundRanges(1000.0F, 4000.0F);
      this.LoadSoundTemplates("Data/sounds.ini");
      GameObject.CrashLog(" ************************Loading settings**************************** ");
      m_cSettings.Load();
      if (m_cSettings.m_bFirstTime) {
         if (m_bDebug) {
            GameObject.CrashLog("Game has been run for the first time\n");
         }

         PerformanceSettings.SetQuality(this.GetCPUTime(), this.GetVideoTime(), this.GetMemTime());
         m_cSettings.SetDetailLevel(PerformanceSettings.m_iCurrQuality);
         if (m_cSettings.m_Details == 0) {
            m_cSettings.m_Resolution = 0;
         } else {
            m_cSettings.m_Resolution = 1;
         }
      }

      m_cSettings.SetCurrentSettings();
      this.m_cCheat = (Cheat)this.CreateObject("Cheat");
      movieModule = (MovieModule)this.CreateObject("MovieModule");
      this.activeMenu = 0;
      movieModule.Activate();
      m_cObjectManager = (ObjectManager)this.CreateObject("ObjectManager");
   }

   static void IncController() {
      m_cObjectManager.ctControllerCounter++;
   }

   static void IncHud() {
      m_cObjectManager.ctHudCounter++;
   }

   // [mod] Codes restaurés : L + code, validé par Shift gauche (voir notes/cheats.md)
   void CheckCheatString() {
      String sCode = this.m_CheatString;
      this.m_CheatString = new String("");
      if (sCode == null || this.m_cCheat == null) {
         return;
      }

      // [mod] en ligne : aucun code pendant un match (supers, horloge, score), sauf l'affichage des FPS
      if (ModNet.IsOnline() && !sCode.equals("LF")) {
         return;
      }

      String sRet = null;
      if (sCode.equals("L111")) {
         sRet = this.m_cCheat.allteams();
      } else if (sCode.equals("L222")) {
         sRet = this.m_cCheat.alltextras();
      } else if (sCode.length() == 2 && sCode.charAt(1) >= '0' && sCode.charAt(1) <= '7') {
         // [mod] L0..L7 : super à l'équipe gauche (4..7 = ModSupers)
         sRet = this.m_cCheat.givesuperleft(sCode.charAt(1) - '0');
      } else if (sCode.length() == 3 && sCode.startsWith("LR") && sCode.charAt(2) >= '0' && sCode.charAt(2) <= '7') {
         sRet = this.m_cCheat.givesuperright(sCode.charAt(2) - '0');
      } else if (sCode.equals("LF")) {
         this.m_cCheat.fps();
         sRet = "<fps>";
      } else if (sCode.equals("LT")) {
         sRet = this.m_cCheat.stopTime();
      } else if (sCode.equals("LTT")) {
         sRet = this.m_cCheat.startTime();
      } else if (sCode.length() == 3 && sCode.startsWith("LB")) {
         int nLevel = sCode.charAt(2) - '0';
         if (nLevel == 0) {
            // [mod] LB0 = niveau 10
            nLevel = 10;
         }

         sRet = activeLevelModule != null ? this.m_cCheat.setDiffLevelRaw(nLevel) : this.m_cCheat.setDiffLevel(nLevel);
      } else if (sCode.length() == 4 && sCode.startsWith("LN")) {
         // [mod] LN<hote><invite> : termine le match sur ce score (ex. LN20 = 2-0 pour l'equipe hote)
         sRet = this.m_cCheat.endmatch(sCode.charAt(2) - '0', sCode.charAt(3) - '0');
      }

      if (sRet != null) {
         this.PlayCS();
         GameObject.CrashLog("[mod] cheat " + sCode + " -> " + sRet + "\n");
      }
   }

   static void DecFBallers() {
      m_cObjectManager.ctFBallersCounter += -1;
   }

   void OnLoadingProgress(float fProgress) {
      if (m_cLoadingScreen != null) {
         m_cLoadingScreen.OnLoadingProgress(fProgress);
      }
   }

   static float GetRandom() {
      return m_Random.nextFloat();
   }

   static int GetRandom(int iExcludedMaxInt) {
      return Math.round(GetRandom() * (iExcludedMaxInt - 1));
   }

   static int GetRandom(int iMin, int iMax) {
      return iMin + GetRandom(iMax - iMin + 1);
   }

   static float GetRandom(float left, float right) {
      float t = m_Random.nextFloat();
      return left + t * (right - left);
   }

   static float GetRandom(float fMax) {
      return fMax * m_Random.nextFloat();
   }

   static void DecHud() {
      m_cObjectManager.ctHudCounter += -1;
   }

   static void SaveAttrib() {
      m_cSettings.Save();
   }

   public void RunModule() {
      CoreLogic log = GetCoreLogic();
      this.activeMenu = 2;
      firstMenuModule.ShowLoadingScreen();
      this.LoadLevel(log.GetNextLevel());
      firstMenuModule.Deactivate();
      activeLevelModule.m_nLevelID = log.GetNextLevel();
      activeLevelModule.Activate();
   }

   static float GetMusicVolume() {
      return m_cSettings.m_MusicVolume;
   }

   static SoccerModule GetSoccerModule() {
      if (activeLevelModule == null) {
         GameObject.CrashLog("activeLevelModule == NULL!!!");
      }

      return activeLevelModule;
   }

   static KbSettings GetKb0() {
      return CoreLogic.GetPlayer().m_Kb0;
   }

   static void SetCommentaryVolume(float fVolume) {
      m_cSettings.m_fCommentaryVolume = fVolume;
      GetCommModule().GetCommentator().SetCommVolumeNow(fVolume);
   }

   static void SetFXVolume(float v) {
      m_cSettings.SetFXVolume(v);
   }

   void EndIntroMovies() {
      if (movieModule != null) {
         movieModule.SplashScreen();
         movieModule.Deactivate();
      }

      this.LoadMenuLevel();
      if (!m_bMenuEditor) {
         this.CreateCommentatorModule();
      }

      this.activeMenu = 1;
      firstMenuModule.Activate();
      if (!SoccerMainMenuModule.bMenuEditor && !m_bMenuON) {
         this.LoadFirstModule();
      }
   }

   static void IncMatch() {
      m_cObjectManager.ctMatchCounter++;
   }

   Commentator GetCommentator() {
      return GetCommModule().GetCommentator();
   }

   void LoadDebugConfig() {
      ParseKeyword root = GameObject.ParseFile("debugconf.scr");
      if (root != null) {
         GameObject.LogLine("Parser zaladowany");

         for (ParseKeyword key = root; key != null; key = key.GetNext()) {
            if (key.Equals("MenuOn")) {
               m_bMenuON = key.GetIParam(0) == 1;
            }

            if (key.Equals("FirstLevel")) {
               this.nFirstLevel = key.GetIParam(0);
            }

            if (key.Equals("RenderPublic")) {
               SoccerModule.m_bRenderPublic = key.GetIParam(0) == 1;
            }

            if (key.Equals("RenderTreesAndGrass")) {
               SoccerModule.m_bRenderTreesAndGrass = key.GetIParam(0) == 1;
            }

            if (key.Equals("FBLighting")) {
               SoccerModule.m_bFBLighting = key.GetIParam(0) == 1;
            }

            if (key.Equals("MenuEditor")) {
               m_bMenuEditor = key.GetIParam(0) == 1;
            }

            if (key.Equals("ShowFPS")) {
               m_bShowFPS = key.GetIParam(0) == 1;
            }

            if (key.Equals("Debug")) {
               m_bDebug = key.GetIParam(0) == 1;
               Game._bDbg = m_bDebug;
            }

            if (key.Equals("Difficulty")) {
               m_nDifficulty = key.GetIParam(0);
            }

            if (key.Equals("Multiplayer")) {
               m_bForceMultiplayer = true;
            }

            if (key.Equals("Demo")) {
               m_bForceDemo = true;
            }

            if (key.Equals("RecordingEnabled")) {
               m_bRecordingEnabled = key.GetIParam(0) == 1;
            }

            // [mod] déblocage sans code clavier (non sauvegardé, comme les cheats L111 / L222)
            if (key.Equals("AllTeams")) {
               Cheat.m_bAllTeams = key.GetIParam(0) == 1;
            }

            if (key.Equals("AllExtras")) {
               Cheat.m_bAllExtras = key.GetIParam(0) == 1;
            }

            // [mod] en ligne : test d'écho UDP (déclaration !NetTest(si))
            if (key.Equals("NetTest")) {
               ModNet.SetTest(key.GetSParam(0), key.GetIParam(1));
            }

            // [mod] en ligne : match direct, hôte (!NetHost(i)) ou client (!NetJoin(s,i))
            if (key.Equals("NetHost")) {
               ModNet.SetHost(key.GetIParam(0));
            }

            if (key.Equals("NetJoin")) {
               ModNet.SetJoin(key.GetSParam(0), key.GetIParam(1));
            }

            // [mod] en ligne : le client joue tout seul, pour les tests (!NetBot(i))
            if (key.Equals("NetBot")) {
               ModNet.m_bBot = key.GetIParam(0) == 1;
            }

            // [mod] en ligne : durée d'une mi-temps en secondes chez l'hôte, pour les tests (!NetHalf(i))
            if (key.Equals("NetHalf")) {
               ModNet.m_nDebugHalf = key.GetIParam(0);
            }

            // [mod] ligue en ligne : but en or à égalité dans tout match entre amis, pour les tests (!NetGolden(i))
            if (key.Equals("NetGolden")) {
               ModOnlineLeague.m_bDebugGolden = key.GetIParam(0) == 1;
            }

            // [mod] en ligne, tests du salon (notes/online.md) : adresse du serveur (!NetServer(s,i)),
            // salon créé ou rejoint sans souris (!NetAuto(i), !NetCode(s)), équipe (!NetTeam(i,i)), nombre de matchs (!NetMatches(i))
            if (key.Equals("NetServer")) {
               ModLobby.m_sDebugHost = key.GetSParam(0);
               ModLobby.m_nDebugPort = key.GetIParam(1);
            }

            if (key.Equals("NetAuto")) {
               ModLobby.m_nAuto = key.GetIParam(0);
            }

            if (key.Equals("NetCode")) {
               ModLobby.m_sAutoCode = key.GetSParam(0);
            }

            if (key.Equals("NetTeam")) {
               ModLobby.m_nAutoAnimal = key.GetIParam(0);
               ModLobby.m_nAutoClothes = key.GetIParam(1);
            }

            if (key.Equals("NetMatches")) {
               ModLobby.m_nAutoMatches = key.GetIParam(0);
            }

            // [mod] en ligne, tests de la partie classée : nom montré aux autres joueurs (!NetName(s)),
            // match quitté tout seul après n secondes (!NetQuit(i))
            if (key.Equals("NetName")) {
               ModLobby.m_sDebugName = key.GetSParam(0);
            }

            if (key.Equals("NetQuit")) {
               ModLobby.m_nDebugQuit = key.GetIParam(0);
            }

            // [mod] en ligne, tests : liaison directe permise ou non, à la place de online.scr (!NetDirect(i)) ;
            // les jeux automatiques ne cliquent pas « continuer » à la mi-temps (!NetNoSkip(i))
            if (key.Equals("NetDirect")) {
               ModLobby.m_nDebugDirect = key.GetIParam(0);
            }

            // [mod] matchs de méta IA contre IA (ModMeta, notes/meta.md)
            if (key.Equals("MetaMatch")) {
               ModMeta.m_bActive = true;
               ModMeta.m_nAnimalA = key.GetIParam(0);
               ModMeta.m_nClothesA = key.GetIParam(1);
               ModMeta.m_nAnimalB = key.GetIParam(2);
               ModMeta.m_nClothesB = key.GetIParam(3);
               ModMeta.m_nLevel = key.GetIParam(4);
            }

            if (key.Equals("MetaHalf")) {
               ModMeta.m_nHalf = key.GetIParam(0);
            }

            if (key.Equals("MetaSpeed")) {
               ModMeta.m_fSpeed = key.GetFParam(0);
            }

            if (key.Equals("MetaSchemes")) {
               ModMeta.m_sSchemeA = key.GetSParam(0);
               ModMeta.m_sSchemeB = key.GetSParam(1);
            }

            if (key.Equals("NetNoSkip")) {
               ModNet.m_bNoSkip = key.GetIParam(0) == 1;
            }
         }
      }
   }

   // [mod] en ligne : le réseau est lu à chaque frame, dans les menus comme en match
   public void OnFrame() {
      super.OnFrame();
      ModNet.OnFrame();
   }

   void ShowCt() {
      System.gc();
      GameObject.CrashLog(m_cObjectManager.toString());
   }

   static void SetResolution(int r) {
      m_cSettings.m_Resolution = r;
   }

   static void DecAction() {
      m_cObjectManager.ctActionCounter += -1;
   }

   static void IncSituation() {
      m_cObjectManager.ctSituationCounter++;
   }

   static void IncFO() {
      m_cObjectManager.ctFOCounter++;
   }

   static void DecTeam() {
      m_cObjectManager.ctTeamCounter += -1;
   }

   static int GetResolution() {
      return m_cSettings.m_Resolution;
   }

   static void DecSituation() {
      m_cObjectManager.ctSituationCounter += -1;
   }

   static void DecController() {
      m_cObjectManager.ctControllerCounter += -1;
   }

   void LoadLevel(int nLevel) {
      String sLevelName = SoccerModule.GetLevelFileName(nLevel);

      try {
         GameObject.CrashLog("Loading level " + sLevelName);
         Runtime rt = Runtime.getRuntime();
         rt.runFinalization();
         GameObject.CrashLog(" free: " + rt.freeMemory() + " total: " + rt.totalMemory() + " \n");
         activeLevelModule = (SoccerModule)this.LoadModule(sLevelName);
         activeLevelModule.m_nLevelID = nLevel;
         activeLevelModule.InitModule();
      } catch (ClassCastException e) {
         GameObject.CrashLog("Unable to load " + sLevelName);
      }
   }

   void NextLevel() {
      CoreLogic.m_nNextLevel++;
      if (CoreLogic.m_nNextLevel > 11 && CoreLogic.m_nNextLevel < 15) {
         CoreLogic.m_nNextLevel = 15;
      }

      if (CoreLogic.m_nNextLevel == 21) {
         CoreLogic.m_nNextLevel = 0;
      }

      if (CoreLogic.m_nNextLevel < 0) {
         CoreLogic.m_nNextLevel = 20;
      }

      CoreLogic.m_cTeamDataRight = CoreLogic.m_cTeamDataLeft;
      CoreLogic.m_cTeamDataLeft = ETeams.GetTeamData((CoreLogic.m_cTeamDataLeft.m_nAnimalType + 1) % 12);
      activeLevelModule.Deactivate();
      activeLevelModule.delete();
      this.RunModule();
   }

   void PreviousLevel() {
      CoreLogic.m_nNextLevel += -1;
      if (CoreLogic.m_nNextLevel > 11 && CoreLogic.m_nNextLevel < 15) {
         CoreLogic.m_nNextLevel = 15;
      }

      if (CoreLogic.m_nNextLevel == 21) {
         CoreLogic.m_nNextLevel = 0;
      }

      if (CoreLogic.m_nNextLevel < 0) {
         CoreLogic.m_nNextLevel = 20;
      }

      CoreLogic.m_cTeamDataRight = CoreLogic.m_cTeamDataLeft;
      CoreLogic.m_cTeamDataLeft = ETeams.GetTeamData((CoreLogic.m_cTeamDataLeft.m_nAnimalType + 1) % 12);
      activeLevelModule.Deactivate();
      activeLevelModule.delete();
      this.RunModule();
   }

   void LoadMenuLevel() {
      firstMenuModule = (SoccerMainMenuModule)this.CreateObject("SoccerMainMenuModule");
      firstMenuModule.InitModule();
   }

   public void SetDebug(boolean bDebug) {
      m_bDebug = bDebug;
   }

   static void IncGoal() {
      m_cObjectManager.ctGoalCouner++;
   }

   static void DecMatch() {
      m_cObjectManager.ctMatchCounter += -1;
   }

   static KbSettings GetKb1() {
      return CoreLogic.GetPlayer().m_Kb1;
   }

   static void IncCtx() {
      m_cObjectManager.ctDecCtxCounter++;
   }

   void CreateCommentatorModule() {
      m_cCommentatorModule = (CommentatorModule)this.CreateObject("CommentatorModule");
      m_cCommentatorModule.InitModule();
      m_cCommentatorModule.Activate();
   }

   static void IncModule() {
      m_cObjectManager.ctModuleCounter++;
   }

   static void DecCtx() {
      m_cObjectManager.ctDecCtxCounter += -1;
   }

   static float GetCommentaryVolume() {
      return m_cSettings.m_fCommentaryVolume;
   }

   static float GetFXVolume() {
      return m_cSettings.m_FXVolume;
   }

   public void SetMultiplayer(boolean bMulti) {
      m_bForceMultiplayer = bMulti;
   }

   static void IncInGame() {
      m_cObjectManager.ctInGameCounter++;
   }

   static void DecFO() {
      m_cObjectManager.ctFOCounter += -1;
   }

   static void IncBall() {
      m_cObjectManager.ctBallCounter++;
   }

   static void IncDecProc() {
      m_cObjectManager.ctDecProcCounter++;
   }

   static void IncEvent() {
      m_cObjectManager.ctEventCounter++;
   }

   void ExitToMenu() {
      this.activeMenu = 1;
      if (activeLevelModule != null) {
         activeLevelModule.Deactivate();
         firstMenuModule.Activate();
         firstMenuModule.CallMethodIn("DeleteLevelModule", 0.0F);
      } else {
         firstMenuModule.Activate();
      }
   }

   static void DecDecProc() {
      m_cObjectManager.ctDecProcCounter += -1;
   }

   static void DecGoal() {
      m_cObjectManager.ctGoalCouner += -1;
   }

   static void IncWatcher() {
      m_cObjectManager.ctWatcherCounter++;
   }

   static void DecWatcher() {
      m_cObjectManager.ctWatcherCounter += -1;
   }

   static void IncFBallers() {
      m_cObjectManager.ctFBallersCounter++;
   }

   // [mod] boutons de la souris (RegisterMouseButtonRequest). La signature exacte attendue par le moteur n'est pas
   // connue : les deux formes probables sont déclarées, le moteur appelle celle qui existe.
   void OnInputMouseButton(int nButton, boolean bState) {
      this.OnModMouseButton(nButton, bState ? 1 : 0);
   }

   void OnInputMouseButton(int nButton, int nState) {
      this.OnModMouseButton(nButton, nState);
   }

   void OnModMouseButton(int nButton, int nState) {
      if (m_bDebug) {
         GameObject.CrashLog("[mod] souris : bouton " + nButton + " etat " + nState + "\n");
      }

      // bouton gauche relâché
      if (nButton == 0 && nState == 0) {
         UIEdit.OnGlobalClick();
      }
   }

   void OnInputKey(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      // [mod] le champ en saisie ne reçoit pas toujours les touches du moteur : on les lui passe d'ici (UIEdit)
      UIEdit.OnGlobalKey(nKeyCode, bKeyState, nKeyAscii);
      CoreLogic log = GetCoreLogic();
      if (m_bDebug || m_bRecordingEnabled) {
         super.OnInputKey(nKeyCode, bKeyState, nKeyAscii);
      }

      if (firstMenuModule != null && !bKeyState) {
         if (this.m_CheatString == null) {
            return;
         }

         if (this.m_CheatString.length() > 10) {
            this.m_CheatString = new String("");
         }

         switch (nKeyCode) {
            case 2:
               this.m_CheatString = this.m_CheatString.concat("1");
               break;
            case 3:
               this.m_CheatString = this.m_CheatString.concat("2");
               break;
            case 4:
               this.m_CheatString = this.m_CheatString.concat("3");
               break;
            case 5:
               this.m_CheatString = this.m_CheatString.concat("4");
               break;
            case 6:
               this.m_CheatString = this.m_CheatString.concat("5");
               break;
            case 7:
               this.m_CheatString = this.m_CheatString.concat("6");
               break;
            case 8:
               this.m_CheatString = this.m_CheatString.concat("7");
               break;
            case 9:
               this.m_CheatString = this.m_CheatString.concat("8");
               break;
            case 10:
               this.m_CheatString = this.m_CheatString.concat("9");
               break;
            case 11:
               this.m_CheatString = this.m_CheatString.concat("0");
               break;
            case 19:
               this.m_CheatString = this.m_CheatString.concat("R");
               break;
            case 20:
               this.m_CheatString = this.m_CheatString.concat("T");
               break;
            case 26:
               if (m_bDebug) {
                  this.PreviousLevel();
               }
               break;
            case 27:
               if (m_bDebug) {
                  this.NextLevel();
               }
               break;
            case 33:
               this.m_CheatString = this.m_CheatString.concat("F");
               break;
            case 38:
               this.m_CheatString = new String("L");
               break;
            case 42:
            case 54:
               // [mod] Shift droit accepté aussi
               this.CheckCheatString();
               break;
            // [mod] chiffres du pavé numérique (scancodes DirectInput)
            case 71:
               this.m_CheatString = this.m_CheatString.concat("7");
               break;
            case 72:
               this.m_CheatString = this.m_CheatString.concat("8");
               break;
            case 73:
               this.m_CheatString = this.m_CheatString.concat("9");
               break;
            case 75:
               this.m_CheatString = this.m_CheatString.concat("4");
               break;
            case 76:
               this.m_CheatString = this.m_CheatString.concat("5");
               break;
            case 77:
               this.m_CheatString = this.m_CheatString.concat("6");
               break;
            case 79:
               this.m_CheatString = this.m_CheatString.concat("1");
               break;
            case 80:
               this.m_CheatString = this.m_CheatString.concat("2");
               break;
            case 81:
               this.m_CheatString = this.m_CheatString.concat("3");
               break;
            case 82:
               this.m_CheatString = this.m_CheatString.concat("0");
               break;
            case 47:
               System.gc();
               break;
            case 48:
               this.m_CheatString = this.m_CheatString.concat("B");
               break;
            case 49:
               this.m_CheatString = this.m_CheatString.concat("N");
         }
      }

      if (nKeyCode == 1 && !bKeyState) {
         switch (this.activeMenu) {
            case 1:
            case 2:
         }
      }
   }

   static void IncSel() {
      m_cObjectManager.ctSelCounter++;
   }

   static void DecSel() {
      m_cObjectManager.ctSelCounter += -1;
   }

   void LoadFirstModule() {
      CoreLogic log = GetCoreLogic();
      CoreLogic.m_bDemo = false;
      log.SetNextLevel(this.nFirstLevel);
      this.RunModule();
   }

   static void SetMusicVolume(float v) {
      m_cSettings.m_MusicVolume = v;
      firstMenuModule.SetLoopVolume(v);
   }
}
