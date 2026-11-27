class SoccerModule extends Module {
   static int m_nMapCount = 0;
   static final int CHK_RACE_CHECKPOINT = 10030;
   static final int CHK_RACE_CHECKPOINT2 = 10031;
   static final int CHK_RACE_CHECKPOINT3 = 10032;
   static boolean m_bRenderTreesAndGrass = true;
   static boolean m_bRenderPublic = true;
   static boolean m_bFBLighting = true;
   int m_nTeamLastScored = -1;
   boolean m_bStartMatch = false;
   static final int _FADE_NONE = 0;
   static final int _FADE_OUT = 1;
   static final int _FADE_IN = 2;
   boolean m_b1 = true;
   int m_nFadeMode = 0;
   float m_FadeCurrentAlpha = 0.9F;
   float m_fFadeTime = 0.5F;
   Sprite m_Fade = null;
   boolean m_bFading = false;
   static GameField cField = null;
   public Camera[] m_Cams = new Camera[2];
   public static int[] m_CamsIDs = new int[2];
   float m_fNowTime = 0.0F;
   float m_fFrameTime = 0.0F;
   float m_fLastUpdateTime = 0.0F;
   static SoccerModule m_Mod = null;
   int m_nLevelID = -1;
   Clock m_Clock = null;
   static PetSoccerHud m_cHud = null;
   static String[] MAPTEXNAMES = new String[0];
   static AIMatch m_cMatch = null;
   static AISystem m_cAISystem = null;
   SoccerInGameMenuModule m_cMenuInGameModule = null;
   boolean m_bMenuVisible = false;
   Vector vFogColor;
   float fFogNear;
   float fFogFar;
   boolean m_bMatchEnd = false;
   boolean m_bMatchHalfTime = false;
   int m_nTimerSmoothing = 10;
   int nFadeTex = 0;
   int nFocusTeam = 0;
   boolean bPaused = false;
   LineLoader cLineLoader = null;

   public void OnFrame() {
      this.m_fNowTime = this.GetTime();
      this.m_fFrameTime = this.m_fNowTime - this.m_fLastUpdateTime;
      switch (this.m_nFadeMode) {
         case 0:
         default:
            break;
         case 1:
            this.Fade(0.9F, 1.0F / this.m_fFadeTime, this.m_fFrameTime);
            break;
         case 2:
            this.Fade(0.0F, -1.0F / this.m_fFadeTime, this.m_fFrameTime);
      }

      this.m_fLastUpdateTime = this.m_fNowTime;
      // [mod] en ligne : le client ne simule rien, il applique l'état reçu de l'hôte
      if (ModNet.IsClient()) {
         ModNet.ClientMatchFrame(this);
         return;
      }

      float dtime = this.m_Clock.Update();
      if (this.m_fFrameTime > 0.0F) {
         if (m_cMatch.m_Events != null) {
            while (m_cMatch.m_Events.CheckAndActivate(this.m_Clock.GetMatchTimeF())) {
            }
         }

         float fTime = this.m_Clock.GetMatchTimeF();
         m_cMatch.UpdateWeather(fTime);
      }

      if (m_cMatch != null) {
         m_cMatch.Update();
      }
   }

   void CallSyncMethod(String sMethodName) {
      this.DisableCallMethod("CallRestartMatch");
      this.DisableCallMethod("CallGameRaw");
      this.DisableCallMethod("CallEndGameRaw");
      this.DisableCallMethod("CallStartHalfRaw");
      this.DisableCallMethod("CallBreakPauseAfterGoal");
      this.DisableCallMethod("CallBreakStab");
      this.DisableCallMethod("CallMenuInGame");
      this.DisableCallMethod("CallMenuEnd");
      this.DisableCallMethod("CallMenuInGameContinue");
      this.DisableCallMethod("CallReplay");
      this.DisableCallMethod("CallReplayBreak");
      m_cMatch.SetSpaceActive(false);
      this.StartFadeOut();
      ModNet.OnSpeedUp(1.0F); // [mod] en ligne
      this.TimerSetSpeedUp(ModMeta.Speed(1.0F)); // [mod] méta : temps accéléré
      this.CallMethodIn(sMethodName, this.m_fFadeTime);
   }

   void OnDeactivate() {
      this.StopMusicLoop();
      this.m_Clock.Stop();
      this.m_Cams[0].UnregisterKeyRequest();
      this.m_Cams[0].UnregisterMouseAxisRequest();
      this.m_Cams[0].UnregisterJoyAxisRequest();
      this.m_Cams[0].UnregisterJoyButtonRequest();
   }

   AIReferee AddReferee(int nLeftAnimalType, int nRightAnimalType, String sName) {
      if (cField == null) {
         GameObject.CrashLog("nie ma pola gry w soccermodule");
      }

      AIReferee cAI = null;
      int nAnimalType = EAnimals.SelectReferee(nLeftAnimalType, nRightAnimalType);
      GameObject.LogLine(" wybralem " + nAnimalType);
      if (nAnimalType == -1) {
         GameObject.CrashLog(" I can not find the class of referee ");
      }

      String sClassName = EAnimals.GetRefereeClassName(nAnimalType);
      GameObject.LogLine(" wybralem klase " + sClassName);
      cAI = (AIReferee)this.CreateObject(sClassName);
      cAI.SetAnimalType(nAnimalType, 0);
      this.AddControlObject(cAI);
      cAI.Init(GameField.m_cRefereeSpawnPoint, GameField.m_cRefereeSpawnPointStart, GameField.m_cRefereeSpawnPointPresentation, -1);
      cAI.SetIndividualName(sName);
      cAI.SetParams(m_cAISystem);
      cAI.InitActivityGraph();
      return cAI;
   }

   void StopMusicLoop() {
      SoccerGame.GetCommModule().StopMusicLoop();
   }

   void InitTeams() {
      if (CoreLogic.m_cTeamDataLeft != null && CoreLogic.m_cTeamDataRight != null) {
         m_cHud.SetTeamsInfo(
            CoreLogic.m_cTeamDataLeft.m_sName, CoreLogic.m_cTeamDataLeft.m_cLogo, CoreLogic.m_cTeamDataRight.m_sName, CoreLogic.m_cTeamDataRight.m_cLogo
         );
      }
   }

   void OnMenuContinue() {
      // [mod] en ligne : chez le client, c'est l'hôte qui ferme le menu des deux joueurs
      if (ModNet.OnMenuContinue()) {
         return;
      }

      this.HideMenu();
   }

   public void BreakSpecialBallStab() {
      m_cMatch.m_sDB_SpecialBallPoint = " BreakSpecialBallStab ";
      this.StartFadeOut();
      if (CoreLogic.GetPlayer().m_bReplaysOn && m_cMatch.m_bLastOutIsShoot && GameObject.RandomFloat(100.0F) > 30.0F) {
         m_cMatch.m_bIsGoalReplay = false;
         this.CallMethodIn("CallReplay", this.m_fFadeTime);
      } else {
         this.CallMethodIn("CallBreakStab", this.m_fFadeTime);
      }
   }

   void OnInputKeyDebug(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      if (bKeyState) {
         switch (nKeyCode) {
            case 207:
               if (AIMatch.IsMatchState(0)) {
                  if (m_cMatch.m_nHalfNo == 0) {
                     this.m_Clock.m_fTime = m_cMatch.m_nHalfTime;
                  } else {
                     this.m_Clock.m_fTime = m_cMatch.m_nHalfTime * 2;
                  }
               }
         }
      }
   }

   public void CallReplayBreak() {
      m_cMatch.ReplayStop();
   }

   static PetSoccerHud GetHud() {
      return m_cHud;
   }

   public void CallBreakPauseAfterGoal() {
      ShowHud();
      this.GetSoccerCamera().ClearReplayCamera();
      if (m_cMatch.m_bGoldGoal) {
         this.m_Clock.Stop();
         m_cMatch.AfterMatch();
         this.DisableCallMethod("OnGoal");
         this.CallMenuEnd();
      } else {
         this.StartFadeIn();
         if (this.m_nTeamLastScored == 1) {
            m_cMatch.RestartGameRaw(0);
         } else {
            m_cMatch.RestartGameRaw(1);
         }
      }
   }

   public void CallCommentGoal() {
      AIFootBaller cGK;
      if (this.m_nTeamLastScored == 0) {
         cGK = (AIFootBaller)m_cMatch.GetTeam(1).m_cGoalKeeper;
      } else {
         cGK = (AIFootBaller)m_cMatch.GetTeam(0).m_cGoalKeeper;
      }

      SoccerGame.game.GetCommentator().CommentGoal(cGK);
   }

   void ShowMenu() {
      ModNet.OnShowMenu(this); // [mod] en ligne : remplace un menu ouvert par échap chez ce joueur seulement
      this.m_bMenuVisible = true;
      this.m_cMenuInGameModule.m_cMenuInGame.m_bMatchEnd = this.m_bMatchEnd;
      this.m_cMenuInGameModule.m_cMenuInGame.m_bMatchHalfTime = this.m_bMatchHalfTime;
      this.m_bMatchHalfTime = false;
      this.GetSoccerCamera().Freeze();
      m_cMatch.ClearSuperSounds();
      if (!this.m_bMatchEnd && !this.m_bMatchHalfTime) {
         this.TimerStop();
      }

      this.m_cMenuInGameModule.Activate();
   }

   void CallSpace() {
      this.OnInputKey(57, true, ' ');
   }

   void StartMatch() {
      this.TimerSetSmoothing(this.m_nTimerSmoothing);
      m_cMatch.StartMatch();
   }

   void OnMenuRestartMatch() {
      this.m_bMatchEnd = false;
      this.HideMenu();
      if (m_cMatch.m_bRestartAfterEnd) {
         this.StartFadeIn();
         this.CallRestartMatch();
      } else {
         this.CallSyncMethod("CallRestartMatch");
      }
   }

   public void CallRestartMatch() {
      GameObject.LogLine(" calling  Restart ");
      this.StartFadeIn();
      m_cMatch.Restart();
   }

   static void HideHud() {
      ModNet.m_bHudShown = false; // [mod] en ligne
      m_cHud.Hide();
   }

   void OnActivate() {
      this.RegisterKeyRequest();
      this.RegisterJoyAxisRequest();
      this.RegisterJoyButtonRequest();
      this.m_Cams[0].RegisterMouseAxisRequest();
      this.m_Cams[0].RegisterKeyRequest();
      this.m_Cams[0].RegisterJoyAxisRequest();
      this.m_Cams[0].RegisterJoyButtonRequest();
      this.PlayMusic(this.m_nLevelID, SoccerGame.GetMusicVolume());
   }

   void ShowMenuHalfTime() {
      this.m_bMatchHalfTime = true;
      this.ShowMenu();
   }

   public void StartFadeIn() {
      if (!this.m_bFading) {
         if ((AIMatch.IsMatchState(4) || AIMatch.IsMatchState(5) || AIMatch.IsMatchState(6)) && m_cMatch.cSpecialBallSel.IsActive()) {
            if (m_cMatch.cSpecialBallSel != null) {
               this.GetSoccerCamera().SetObserved(m_cMatch.cSpecialBallSel);
            } else {
               this.GetSoccerCamera().SetObserved(GameField.m_cBall);
            }
         } else {
            this.GetSoccerCamera().SetObserved(GameField.m_cBall);
         }

         this.m_bFading = true;
         this.m_FadeCurrentAlpha = 0.9F;
         this.m_Fade.SetAlpha(this.m_FadeCurrentAlpha);
         this.m_Fade.Show();
         this.ForceRender();
         this.m_Fade.MoveTop();
         this.m_nFadeMode = 2;
      }
   }

   void InitFadeWnd() {
      if (this.m_Fade == null) {
         this.m_Fade = (Sprite)this.CreateObject("Sprite");
         this.AddSprite(this.m_Fade);
         this.nFadeTex = this.m_Fade.AddTexture("Data/Textures/black.png", true, true);
         this.m_Fade.SetTexture(this.nFadeTex, new Rect(0.0F, 0.0F, 1.0F, 1.0F));
         this.m_Fade.SetSize(new Vector(GameObject.GetScreenWidth(), GameObject.GetScreenHeight(), 0.0F));
         this.m_Fade.SetPos(new Vector(0.0F, 0.0F, 0.0F));
         this.m_Fade.Show();
      }
   }

   public void CallBreakStab() {
      m_cMatch.m_sDB_SpecialBallPoint = " CallBreakStab ";
      this.StartFadeIn();
      ShowHud();
      this.GetSoccerCamera().ClearReplayCamera();
      m_cMatch.ResetStabilizationNew();
      ((AITeam)m_cMatch.GetTeam(0)).m_bIsToStabilization = false;
      ((AITeam)m_cMatch.GetTeam(1)).m_bIsToStabilization = false;
      m_cMatch.RestoreSpecialBallPositions();
   }

   public void CallGameRaw() {
      this.StartFadeIn();
      m_cMatch.SideLottery();
      m_cMatch.RestartGameRaw(m_cMatch.m_nTeamStartedFirstHalf);
   }

   AIFootBaller AddFootBaller(int nTeam, int nAnimalType, int nClothesId, String sName) {
      if (cField == null) {
         GameObject.CrashLog("nie ma pola gry w soccermodule");
      }

      AIFootBaller cAI = null;
      String sClassName = null;
      switch (nAnimalType) {
         case 0:
            sClassName = "AIBear";
            break;
         case 1:
            sClassName = "AIBull";
            break;
         case 2:
            sClassName = "AIChicken";
            break;
         case 3:
            sClassName = "AIDino";
            break;
         case 4:
            sClassName = "AIFrog";
            break;
         case 5:
            sClassName = "AIGiraffe";
            break;
         case 6:
            sClassName = "AIGorilla";
            break;
         case 7:
            sClassName = "AILion";
            break;
         case 8:
            sClassName = "AIPanther";
            break;
         case 9:
            sClassName = "AIShark";
            break;
         case 10:
            sClassName = "AITurtle";
            break;
         case 11:
            sClassName = "AIRobo";
            break;
         // [mod] nouvelles equipes
         case 12:
            sClassName = "AIPanda";
            break;
         case 13:
            sClassName = "AITiger";
      }

      cAI = (AIFootBaller)this.CreateObject(sClassName);
      cAI.SetAnimalType(nAnimalType, nClothesId);
      SpawnPoint[] tPoints = new SpawnPoint[3];
      cField.GetNextSpawnPoint(nTeam, tPoints);
      if (tPoints[0] == null) {
         return null;
      }

      this.AddControlObject(cAI);
      cAI.m_nAnimalType = nAnimalType;
      cAI.Init(tPoints[0], tPoints[1], tPoints[2], nTeam);
      cAI.SetIndividualName(sName);
      int nNo = m_cMatch.GetTeam(nTeam).m_vFootBallers.size();
      cAI.m_nNumberInTeam = nNo;
      m_cMatch.GetTeam(nTeam).AddFootBaller(cAI);
      if (tPoints[0].GetFootballerType() == 2) {
         cAI.m_cTeam.SetGoalKeeper(cAI);
      }

      cAI.SetParams(m_cAISystem);
      cAI.InitActivityGraph();
      return cAI;
   }

   void WrtiteStateAsLog() {
      GameObject.CrashLog(" ");
      GameObject.CrashLog("");
      GameObject.CrashLog("AIMatch - state: " + AIMatch.m_nMatchState + " is waiting: " + m_cMatch.IsWaiting());
   }

   void OnGoal() {
      GameObject.LogLine("Module::OnGoal");
      m_cMatch.ClearAI(true);
      m_cMatch.SetThink();
      if (CoreLogic.GetPlayer().m_bReplaysOn) {
         m_cMatch.m_bIsGoalReplay = true;
         this.CallSyncMethod("CallReplay");
      } else {
         this.CallSyncMethod("CallBreakPauseAfterGoal");
      }
   }

   void ShowMenuEnd() {
      this.m_bMatchEnd = true;
      this.ShowMenu();
      SoccerGame.SaveAttrib();
   }

   void Temp(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      switch (nKeyCode) {
         case 57:
            if (!m_cMatch.IsSpaceActive()) {
               return;
            } else if (CoreLogic.GetPlayer().m_bReplaysOn && Match.m_bPlaying) {
               this.CallSyncMethod("CallReplayBreak");
            } else if (m_cMatch.m_bGoalReact) {
               this.CallSyncMethod("CallBreakPauseAfterGoal");
            } else if (AIMatch.IsMatchState(1)) {
               SoccerGame.game.GetCommentator().ClearCurrComment();
               this.CallSyncMethod("CallGameRaw");
            } else if (m_cMatch.IsHalfChanging() && m_cMatch.m_bWasHalfMenu) {
               this.CallSyncMethod("CallStartHalfRaw");
            } else if (m_cMatch.IsHalfChanging() && !m_cMatch.m_bWasHalfMenu) {
               m_cMatch.m_bEndBreak = true;
               this.CallSyncMethod("CallEndGameRaw");
            } else if (AIMatch.IsMatchState(3)) {
               m_cMatch.m_bEndBreak = true;
               this.CallSyncMethod("CallEndGameRaw");
            }
      }
   }

   void OnEscape() {
      // [mod] en ligne : échap n'ouvre le menu que chez ce joueur, le match continue pour l'autre
      if (ModNet.OnEscape(this)) {
         return;
      }

      if (!GameField.m_cBall.IsSuperShot()) {
         if (this.m_bMenuVisible) {
            // [mod] en ligne, pause de mi-temps : échap vaut « continuer », le menu attend l'autre joueur
            if (ModNet.HostHalfSkip()) {
               return;
            }

            this.HideMenu();
         } else {
            this.ShowMenu();
         }
      }
   }

   public void CallStopTimForReplay() {
      Recorder.StopClock();
   }

   public void StartGameDelay() {
      m_cMatch.StartSpecialBall(m_cMatch.m_nTeamToStart);
   }

   void PlayMusic(int nLevel, float fVolume) {
      SoccerGame.GetCommModule().PlayMusic(nLevel, fVolume);
   }

   void PlayMusic() {
      this.PlayMusic(this.m_nLevelID, SoccerGame.GetMusicVolume());
   }

   static int GetMapsCnt() {
      return CoreLogic._LEVELS_COUNT;
   }

   void PlayReplayMusic() {
      SoccerGame.GetCommModule().PlayReplayMusic();
   }

   void CallEsc() {
      this.OnInputKey(1, true, ' ');
   }

   static String GetLevelFileName(int nLevel) {
      return nLevel >= 0 && nLevel < CoreLogic._LEVELS_COUNT ? CoreLogic.GetLevelFileName(nLevel) : null;
   }

   public void CallMenuInGame() {
      this.StartFadeIn();
      m_cMatch.m_nHalfNo = 1;
      m_cMatch.ChangeTeamSides();
      m_cMatch.RestoreStartPositions(false);
      m_cMatch.ClearAI(true);
      if (m_cMatch.m_nTeamStartedFirstHalf == 0) {
         m_cMatch.RestartGame(0);
      } else {
         m_cMatch.RestartGame(1);
      }

      this.ShowMenuHalfTime();
      this.CallMethodIn("CallMenuInGameContinue", 0.2F);
      m_cMatch.m_bWasHalfMenu = true;
      m_cMatch.m_bEndBreak = false;
   }

   void InitSounds() {
      GameObject[] multisounds = GameObject.GetObjectsFromClass("MultiSoundDummy");
      if (multisounds != null) {
         for (int i = 0; i < multisounds.length; i++) {
            ((MultiSoundDummy)multisounds[i]).InitSound();
         }
      } else {
         MultiSoundBirds birds = (MultiSoundBirds)this.CreateObject("MultisoundBirds");
         this.AddControlObject(birds);
         birds.SetPosition(GameField.cCenter.GetPosition());
         birds.InitSound();
      }
   }

   Clock GetClock() {
      if (this.m_Clock == null) {
         GameObject.CrashLog("m_Clock == null");
      }

      return this.m_Clock;
   }

   public void CallEndGameRaw() {
      m_cMatch.RestoreStartPositions(true);
   }

   public void CallStartHalfRaw() {
      this.StartFadeIn();
      m_cMatch.RestartHalfRaw();
   }

   SoccerCamera GetSoccerCamera() {
      if (this.m_Cams[0] == null) {
         GameObject.CrashLog("m_Camera == null");
      }

      return (SoccerCamera)this.m_Cams[0];
   }

   void HideMenu() {
      ModNet.OnMenuHidden(); // [mod] en ligne
      m_cMatch.PlaySuperSounds();
      if (this.m_bMatchEnd) {
         this.m_cMenuInGameModule.m_cMenuInGame.OnQuit();
      } else {
         this.m_bMenuVisible = false;
         this.m_cMenuInGameModule.Deactivate();
         this.GetSoccerCamera().UnFreeze();
         this.TimerStart();
      }
   }

   void InitField() {
      GameObject.CrashLog("Initializing GameField");
      cField = new GameField();
      cField.Init(this);
      m_cMatch = new AIMatch();
      m_cMatch.SetParams(m_cAISystem);
      m_cMatch.SetModule(this);
      m_cMatch.InitSpecialBallSel();
      GameObject.LogLine(" MAtch created as Game Object");
      this.GetSoccerCamera().SetObserved(GameField.m_cBall);
   }

   void SetRot(float x, float y, float z, float t) {
   }

   void InitPublic() {
      GameObject[] watchers = GameObject.GetObjectsFromClass("Watcher");
      if (watchers != null) {
         for (int i = 0; i < watchers.length; i++) {
            ((Watcher)watchers[i]).Init();
         }
      }
   }

   public void StartFadeOut() {
      this.GetSoccerCamera().SetObserved(null);
      this.m_bFading = false;
      this.m_Fade.MoveTop();
      this.m_FadeCurrentAlpha = 0.0F;
      this.m_Fade.SetAlpha(this.m_FadeCurrentAlpha);
      this.m_Fade.Show();
      this.ForceRender();
      this.m_nFadeMode = 1;
   }

   public void CallMenuInGameContinue() {
      m_cMatch.SetSpaceActive(true);
   }

   public void CallMenuEnd() {
      this.ShowMenuEnd();
   }

   static void ShowHud() {
      ModNet.m_bHudShown = true; // [mod] en ligne
      m_cHud.Show();
   }

   void InitAISystem() {
      m_cAISystem = new AISystem();
      m_cAISystem.SetFileName("data/ai_params.scr");
      m_cAISystem.Load();
      m_cAISystem.ShowParams();
   }

   public void OnLoadChunk(int nChkID, int nDataSize, MapInputStream cMapStream) {
      if (nChkID == 10030) {
         if (SoccerGame.m_bDebug) {
            GameObject.CrashLog("loading race checkpoints\n");
         }

         this.cLineLoader = new LineLoader();
         this.cLineLoader.Load(cMapStream);
      }

      if (nChkID == 10031) {
         this.cLineLoader.LoadBools(cMapStream);
      }

      if (nChkID == 10032) {
         this.cLineLoader.LoadAddLayers(cMapStream);
      }
   }

   void HandleUIClick(int nID) {
      switch (nID) {
         case 1:
            this.OnMenuContinue();
            break;
         case 2:
            this.OnMenuRestartMatch();
      }
   }

   public void CallReplay() {
      m_cMatch.m_sDB_SpecialBallPoint = " CallReplay ";
      this.StartFadeIn();
      m_cMatch.Replay();
      m_cMatch.SetSpaceActive(true);
   }

   void EndMatch() {
      m_cMatch.EndMatch();
   }

   void OnCreate() {
      m_nMapCount++;
      ModNet.OnModuleCreate(); // [mod] en ligne
      SoccerGame.activeLevelModule = this;
      this.m_Clock = new Clock();
      super.OnCreate();
      this.vFogColor = this.FogGetColor();
      this.fFogNear = this.FogGetNear();
      this.fFogFar = this.FogGetFar();
      Vector vLevelExtents = this.GetTerrainSize();
      vLevelExtents.fY += 100000.0F;
      this.SetExtents(vLevelExtents);
      BaseCamera.SetModule(this);
      SoccerCamera camera = null;
      camera = (SoccerCamera)this.CreateObjectNoInit("SoccerCamera");
      this.m_Cams[0] = camera;
      this.InitFXSystem();
      this.m_fLastUpdateTime = this.GetTime();
      m_Mod = this;
      this.InitAISystem();
      SoccerGame.IncModule();
   }

   void OnInputKey(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      // [mod] en ligne : chez le client, espace et échap sont des demandes envoyées à l'hôte
      if (ModNet.OnModuleKey(nKeyCode, bKeyState)) {
         return;
      }

      if (SoccerGame.m_bDebug && !ModLobby.IsOpen()) { // [mod] pas de touches de debug dans un match de salon
         this.OnInputKeyDebug(nKeyCode, bKeyState, nKeyAscii);
      }

      if (bKeyState) {
         switch (nKeyCode) {
            case 1:
               this.OnEscape();
               break;
            case 57:
               if (!m_cMatch.IsSpaceActive()) {
                  return;
               }

               if ((AIMatch.IsMatchState(4) || AIMatch.IsMatchState(5) || AIMatch.IsMatchState(6)) && !Match.m_bPlaying) {
                  return;
               }

               m_cMatch.m_bWasSpacePressed = true;
               if (CoreLogic.GetPlayer().m_bReplaysOn && Match.m_bPlaying) {
                  this.CallSyncMethod("CallReplayBreak");
               } else if (m_cMatch.m_bGoalReact) {
                  this.CallSyncMethod("CallBreakPauseAfterGoal");
               } else if (AIMatch.IsMatchState(1)) {
                  m_cMatch.ClearSendHalfCamera();
                  SoccerGame.game.GetCommentator().ClearCurrComment();
                  this.CallSyncMethod("CallGameRaw");
               } else if (m_cMatch.IsHalfChanging() && m_cMatch.m_bWasHalfMenu) {
                  this.CallSyncMethod("CallStartHalfRaw");
               } else if (m_cMatch.IsHalfChanging() && !m_cMatch.m_bWasHalfMenu) {
                  m_cMatch.m_bEndBreak = true;
                  this.CallSyncMethod("CallEndGameRaw");
               } else if (m_cMatch.m_bMatchEnding || AIMatch.IsMatchState(3)) {
                  if (!AIMatch.IsMatchState(3)) {
                     this.DisableCallMethod("OnMatchEnd");
                     this.OnMatchEnd();
                  }

                  m_cMatch.m_bEndBreak = true;
                  this.CallSyncMethod("CallEndGameRaw");
               }
               break;
            case 68:
               if (CoreLogic.GetPlayer().IsCheater() && !ModNet.IsOnline()) { // [mod] pas de triche en ligne
                  m_cMatch.ResetSupers();
                  m_cMatch.GetTeam(0).GiveSuper();
                  m_cMatch.GetTeam(1).GiveSuper();
               }
         }
      }
   }

   public void OnMatchEnd() {
      GameObject.LogLine(" ON_MATCH_END ");
      if (m_cMatch.IsStabilizationNew()) {
         m_cMatch.ResetStabilizationRaw();
      }

      if (m_cMatch.m_bGoldGoal) {
         GameField.m_cBall.ResetStartPosition();
      }

      this.GetSoccerCamera().SetEndHalfCamera();
      AIMatch.SetMatchState(3);
      m_cMatch.SetStabilizationNew(true);
   }

   boolean Fade(float dest, float speed, float dtime) {
      boolean ret = false;
      this.m_FadeCurrentAlpha += speed * dtime;
      if (speed >= 0.0F) {
         if (this.m_FadeCurrentAlpha >= dest) {
            this.m_FadeCurrentAlpha = dest;
            ret = true;
            this.m_nFadeMode = 0;
         }
      } else if (this.m_FadeCurrentAlpha <= dest) {
         this.m_FadeCurrentAlpha = dest;
         ret = true;
         this.m_nFadeMode = 0;
      }

      this.m_Fade.SetAlpha(this.m_FadeCurrentAlpha);
      return ret;
   }

   public void InitModule() {
      CoreLogic log = SoccerGame.GetCoreLogic();
      this.InitField();
      this.SetTreesLODSelectionFactor(PerformanceSettings._TREES_LOD[PerformanceSettings.m_iCurrQuality]);
      this.SetTreesVisibilityRange(PerformanceSettings._TREES_CLIP[PerformanceSettings.m_iCurrQuality]);
      this.SetObjectsVisibilityRange(
         0.8F * PerformanceSettings._OBJECTS_CLIP[PerformanceSettings.m_iCurrQuality], PerformanceSettings._OBJECTS_CLIP[PerformanceSettings.m_iCurrQuality]
      );
      this.m_Cams[0].InitObject();
      m_CamsIDs[0] = this.AddView(this.m_Cams[0], 0.0F, 0.0F, 1.0F, 1.0F);
      this.ListenerOnViewActive(m_CamsIDs[0], true);
      String sLoop = CoreLogic.GetLevelLoopSoundName(this.m_nLevelID);
      if (sLoop != null) {
         ((SoccerCamera)this.m_Cams[0]).AttachSound(sLoop);
      }

      if (SoccerGame.m_bRecordingEnabled) {
         SoccerGame.game.SetScreenshotView(this, m_CamsIDs[0]);
      }

      this.ViewRenderTrees(m_CamsIDs[0], m_bRenderTreesAndGrass);
      this.TreesBlockMove(m_bRenderTreesAndGrass);
      this.ViewRenderGrass(m_CamsIDs[0], m_bRenderTreesAndGrass);
      this.InitFadeWnd();
      int nResolution = SoccerGame.GetResolution();
      this.m_cMenuInGameModule = (SoccerInGameMenuModule)this.CreateObject("SoccerInGameMenuModule");
      this.m_cMenuInGameModule.InitModule();
      m_cHud = (PetSoccerHud)this.CreateObject("PetSoccerHud");
      m_cHud.Load(this, nResolution);
      m_cHud.Show();
      this.InitSounds();
      if (m_bRenderPublic) {
         this.InitPublic();
      }

      this.ActivateView(m_CamsIDs[0]);
      this.GetSoccerCamera().ChangeMode(CoreLogic.GetPlayer().m_nDefCameraMode);
      this.CallMethodIn("StartMatch", 0.0F);
   }

   public void OnDestroy() {
      ModNet.OnModuleDestroy(); // [mod] en ligne
      m_cHud.Remove();
      this.RemoveSprite(m_cHud);
      m_cHud.delete();
      m_cHud = null;
      this.m_cMenuInGameModule = null;
      m_cMatch.delete();
      m_cMatch = null;
      m_cAISystem.delete();
      m_cAISystem = null;
      this.m_Clock = null;
      this.m_Fade.delete();
      this.m_Fade = null;
      cField.Reset();
      cField = null;
      SoccerGame.activeLevelModule = null;
      m_Mod = null;
      this.m_Cams[0].delete();
      this.m_Cams[0] = null;
      this.m_Cams = null;
      if (this.cLineLoader != null) {
         this.cLineLoader.delete();
      }
   }
}
