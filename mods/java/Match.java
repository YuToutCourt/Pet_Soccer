public class Match {
   static MatchData m_cMatchData = null;
   boolean m_bCommentaryDone = false;
   static final int WEATHER_NORMAL = -1;
   static final float MIN_WEATHER_TIME = 40.0F;
   Team[] m_teams = new Team[2];
   Team m_cBallPossesor = null;
   int m_nHalfTime = 0;
   int m_nHalfNo = 0;
   boolean m_bGoldGoal = false;
   boolean m_bAIActive = true;
   int m_nPlayer1Team = 0;
   int m_nPlayer2Team = 1;
   Strategy m_cStrategy = null;
   static SoccerModule m_cModule = null;
   Weather m_Weather = new Weather();
   ScriptedEventList m_Events = null;
   boolean m_bInited = false;
   Clock m_cClockRef = null;
   int m_nScoreLeft = 0;
   int m_nScoreRight = 0;
   boolean m_bStabilizationState = true;
   int m_nBeepLastSeconds = 3;
   boolean m_bJustMatchStarted = false;
   AIReferee m_cReferee = null;
   int m_nPrevTimeInSeconds = 0;
   static boolean m_bPlaying = false;

   void StartGame(int nTeam) {
   }

   void EndHalf() {
   }

   void OnReplayStop() {
      m_bPlaying = false;
      this.m_bInited = true;
      Team cTeam = null;
      FootBaller fb = null;

      for (int i = 0; i < 2; i++) {
         cTeam = this.GetTeam(i);

         for (int j = 0; j < cTeam.GetFootBallerCount(); j++) {
            fb = cTeam.GetFootBaller(j);
            ((AIFootBaller)fb).SetActiveAI(true);
         }
      }

      m_cModule.PlayMusic();
   }

   void ClearWeatherEvents() {
      ModNet.HostWeather(0, 0.0F); // [mod] en ligne
      this.m_Weather.KillDisaster();
      if (this.m_Events != null) {
         this.m_Events = null;
      }
   }

   float TossWeatherTime(int nLevelId, int nType, float fStartTime) {
      float fTimeLeft = this.m_nHalfTime * 2 - fStartTime;
      float fRnd = 0.0F;
      switch (nLevelId) {
         case 0:
            if (nType == -1) {
               fRnd = GameObject.RandomFloat(fTimeLeft / 2.0F);
            } else if (nType == 4) {
               fRnd = GameObject.RandomFloat(fTimeLeft);
            }
            break;
         case 1:
            if (nType == -1) {
               fRnd = GameObject.RandomFloat(fTimeLeft);
            } else if (nType == 1) {
               fRnd = GameObject.RandomFloat(fTimeLeft / 2.0F);
            }
            break;
         case 2:
            if (nType == -1) {
               fRnd = GameObject.RandomFloat(fTimeLeft);
            } else if (nType == 1) {
               fRnd = GameObject.RandomFloat(fTimeLeft / 2.0F);
            }
            break;
         case 3:
            if (nType == -1) {
               fRnd = GameObject.RandomFloat(fTimeLeft / 2.0F);
            } else if (nType == 1) {
               fRnd = GameObject.RandomFloat(fTimeLeft);
            } else if (nType == 3) {
               fRnd = GameObject.RandomFloat(fTimeLeft);
            }
            break;
         case 4:
            if (nType == -1) {
               fRnd = GameObject.RandomFloat(fTimeLeft / 2.0F);
            } else if (nType == 1) {
               fRnd = GameObject.RandomFloat(fTimeLeft / 2.0F);
            } else if (nType == 2) {
               fRnd = GameObject.RandomFloat(fTimeLeft);
            }
         case 5:
         case 6:
         case 7:
         case 8:
         case 9:
         case 10:
         case 11:
         case 12:
         case 13:
         case 14:
         case 15:
         case 16:
         case 17:
         case 18:
         default:
            break;
         case 19:
            fRnd = 20.0F;
      }

      if (fRnd > 0.0F) {
         if (fRnd > fTimeLeft) {
            fRnd = fTimeLeft;
         } else if (fRnd < 40.0F && nLevelId != 19) {
            fRnd = 40.0F;
         }
      }

      return fRnd;
   }

   void Update() {
      Vector vTime = this.m_cClockRef.GetMatchTime();
      int nSeconds = (int)(vTime.fX * 60.0F + vTime.fY);
      boolean bEnding = false;
      int nSecondsScope;
      if (this.IsFirstHalf()) {
         nSecondsScope = this.m_nHalfTime;
      } else {
         nSecondsScope = this.m_nHalfTime * 2;
      }

      if (this.m_nPrevTimeInSeconds < nSeconds || this.m_nPrevTimeInSeconds == nSeconds && nSeconds == nSecondsScope) {
         if (nSeconds >= nSecondsScope && !this.m_bGoldGoal) {
            if (nSeconds > nSecondsScope) {
               nSeconds = nSecondsScope;
               this.m_cClockRef.Reset(nSeconds);
               vTime = this.m_cClockRef.GetMatchTime();
            }

            if (nSeconds == nSecondsScope) {
               SoccerModule.m_cHud.UpdateClock(vTime, bEnding);
            }

            if (!this.m_bGoldGoal) {
               if (this.IsFirstHalf()) {
                  if (AIMatch.IsMatchState(0)
                     && !SoccerModule.m_cMatch.IsWaiting()
                     && (!GameField.m_cBall.IsMoving() || GameField.m_cBall.HasOwner())
                     && !GameField.m_cBall.m_bWasOut
                     && !GameField.m_cBall.m_bWasCorner
                     && !GameField.m_cBall.m_bWasGoal) {
                     this.EndHalf();
                  }
               } else if ((CoreLogic.m_bChampionships && CoreLogic.GetPlayer().m_cChampionships.m_nPhase > 0 || ModSurvival.IsGoldenGoal() || ModLeagueMode.IsGoldenGoal() || ModOnlineLeague.IsGoldenGoal()) // [mod] survie, playoffs de la ligue
                  && this.m_nScoreLeft == this.m_nScoreRight) {
                  if (AIMatch.IsMatchState(0)
                     && !SoccerModule.m_cMatch.IsWaiting()
                     && (!GameField.m_cBall.IsMoving() || GameField.m_cBall.HasOwner())
                     && !GameField.m_cBall.m_bWasOut
                     && !GameField.m_cBall.m_bWasCorner
                     && !GameField.m_cBall.m_bWasGoal) {
                     this.m_bGoldGoal = true;
                     this.EndHalf();
                  }
               } else if (AIMatch.IsMatchState(0)
                  && !SoccerModule.m_cMatch.IsWaiting()
                  && (!GameField.m_cBall.IsMoving() || GameField.m_cBall.HasOwner())
                  && !GameField.m_cBall.m_bWasOut
                  && !GameField.m_cBall.m_bWasCorner
                  && !GameField.m_cBall.m_bWasGoal) {
                  this.EndMatch();
               }
            }

            this.m_nPrevTimeInSeconds = nSeconds;
            SoccerModule.m_cHud.UpdateScore(this.m_nScoreLeft, this.m_nScoreRight);
            return;
         }

         if ((
               nSeconds >= this.m_nHalfTime - this.m_nBeepLastSeconds && this.IsFirstHalf()
                  || nSeconds >= this.m_nHalfTime * 2 - this.m_nBeepLastSeconds && !this.IsFirstHalf()
            )
            && !this.m_bGoldGoal) {
            m_cModule.PlaySound("beep.wav");
            ModNet.HostSound(ModNet._SND_MODULE, null, "beep.wav"); // [mod] en ligne
            bEnding = true;
         }

         if (nSeconds % HudInfo.m_nShowPeriod == 0) {
            SoccerModule.GetHud().ShowInfo();
         }
      }

      this.m_nPrevTimeInSeconds = nSeconds;
      SoccerModule.m_cHud.UpdateClock(vTime, bEnding);
      SoccerModule.m_cHud.UpdateScore(this.m_nScoreLeft, this.m_nScoreRight);
   }

   AIReferee GetReferee() {
      return this.m_cReferee;
   }

   void Restart() {
      this.Record();
      this.m_bCommentaryDone = false;
      SoccerGame.game.GetCommentator().ClearCurrComment();
      int nTeamHost = -1;
      int nTeamGuest = -1;
      if (m_cMatchData != null) {
         m_cMatchData.ResetResults();
         nTeamHost = CoreLogic.m_cTeamDataLeft.m_nAnimalType;
         nTeamGuest = CoreLogic.m_cTeamDataRight.m_nAnimalType;
         if (SoccerGame.m_bDebug && CoreLogic.m_bChampionships) {
            GameObject.CrashLog(
               "Match.Restart " + m_cMatchData.toString() + " player team " + ETeams.GetTeamData(CoreLogic.GetPlayer().m_cChampionships.m_nPlayerTeam).m_sName
            );
         }
      } else if (!CoreLogic.m_bChampionships) {
         nTeamHost = CoreLogic.m_cTeamDataLeft.m_nAnimalType;
         nTeamGuest = CoreLogic.m_cTeamDataRight.m_nAnimalType;
         m_cMatchData = new MatchData(nTeamHost, nTeamGuest);
      } else {
         Tools.CrashLogError("Brak danych meczu w Match.Restart");
      }

      if (!this.m_bCommentaryDone) {
         Commentator comm = SoccerGame.game.GetCommentator();
         comm.Hide();
         comm.StartTheMatch(CoreLogic.m_nNextLevel, nTeamHost, nTeamGuest);
         this.m_bCommentaryDone = true;
      }
   }

   void ReplayStop() {
      if (m_bPlaying) {
         m_bPlaying = false;
         this.m_bInited = true;
         GameField.m_cBall.ReplayStop();
         Team cTeam = null;
         FootBaller fb = null;

         for (int i = 0; i < 2; i++) {
            cTeam = this.GetTeam(i);

            for (int j = 0; j < cTeam.GetFootBallerCount(); j++) {
               fb = cTeam.GetFootBaller(j);
               fb.ReplayStop();
            }
         }
      }
   }

   int TossWeatherType(int nTypeCount, float fTimePoint) {
      float nScope = nTypeCount * 10 + 10.0F;
      float nRnd = GameObject.RandomFloat(nScope);
      if (!(nRnd < 10.0F) && fTimePoint != 0.0F) {
         nRnd -= 10.0F;
         return (int)(nRnd / 10.0F);
      } else {
         return -1;
      }
   }

   Team GetTeam(int nTeam) {
      return this.m_teams[nTeam];
   }

   void RestoreGamePositions(boolean bFBallersPos) {
      if (bFBallersPos) {
         for (int i = 0; i < 2; i++) {
            Team team = this.m_teams[i];

            for (int j = 0; j < team.GetFootBallerCount(); j++) {
               AIFootBaller fb = (AIFootBaller)team.GetFootBaller(j);
               fb.ResetSpawnPosition();
            }
         }

         this.m_cReferee.ResetSpawnPosition();
      }

      Ball cBall = GameField.m_cBall;
      if (cBall.GetCurOwner() != null) {
         ((AIFootBaller)cBall.GetCurOwner()).DetachBall();
      }

      GameField.m_cBall.ResetStartPosition();
   }

   void RestorePresentationPositions(boolean bBallPos) {
      for (int i = 0; i < 2; i++) {
         Team team = this.m_teams[i];

         for (int j = 0; j < team.GetFootBallerCount(); j++) {
            AIFootBaller fb = (AIFootBaller)team.GetFootBaller(j);
            fb.ResetPresentationPosition();
         }
      }

      this.m_cReferee.ResetPresentationPosition();
   }

   void MakeWeatherEvents(int nLevelId) {
      this.ClearWeatherEvents();
      this.m_Events = new ScriptedEventList();
      float fTimePoint = 0.0F;
      int nLastWeatherType = -1;

      while (fTimePoint < this.m_nHalfTime * 2) {
         float fTime = 0.0F;
         switch (nLevelId) {
            case 0:
               int nTypeLoss = this.TossWeatherType(1, fTimePoint);
               int nType = -1;
               if (nTypeLoss == 0) {
                  nType = 4;
               }

               fTime = this.TossWeatherTime(nLevelId, nType, fTimePoint);
               this.AddWeather(nType, fTimePoint, fTime);
               break;
            case 1:
               int var10 = this.TossWeatherType(2, fTimePoint);
               int var15 = -1;
               if (var10 == 1) {
                  var15 = 1;
               }

               fTime = this.TossWeatherTime(nLevelId, var15, fTimePoint);
               this.AddWeather(var15, fTimePoint, fTime);
               break;
            case 2:
               int var9 = this.TossWeatherType(1, fTimePoint);
               int var14 = -1;
               if (var9 == 0) {
                  var14 = 1;
               }

               fTime = this.TossWeatherTime(nLevelId, var14, fTimePoint);
               this.AddWeather(var14, fTimePoint, fTime);
               break;
            case 3:
               int var8 = this.TossWeatherType(1, fTimePoint);
               int var13 = -1;
               if (var8 == 0) {
                  var13 = 3;
               }

               fTime = this.TossWeatherTime(nLevelId, var13, fTimePoint);
               this.AddWeather(var13, fTimePoint, fTime);
               break;
            case 4:
               int var7 = this.TossWeatherType(2, fTimePoint);
               int var12 = -1;
               if (var7 == 0) {
                  var12 = 1;
               }

               if (var7 == 1) {
                  var12 = 2;
               }

               fTime = this.TossWeatherTime(nLevelId, var12, fTimePoint);
               this.AddWeather(var12, fTimePoint, fTime);
            case 5:
            case 6:
            case 7:
            case 8:
            case 9:
            case 10:
            case 11:
            case 12:
            case 13:
            case 14:
            case 15:
            case 16:
            case 17:
            case 18:
            default:
               break;
            case 19:
               // [mod] variables renommées : le code décompilé redéclarait nType / nTypeLoss dans le même bloc
               int nTypeLoss19 = this.TossWeatherType(6, fTimePoint);
               int nType19 = nTypeLoss19;
               if (nType19 == nLastWeatherType || nType19 == -1) {
                  nType19 = (nType19 + GameObject.RandomInt(5)) % 7;
                  if (nType19 == -1) {
                     nType19 = 0;
                  }
               }

               if (nType19 == 6) {
                  nType19 = 5;
               }

               if (nType19 == 0) {
                  nType19 += 1 + GameObject.RandomInt(4);
               }

               fTime = this.TossWeatherTime(nLevelId, nType19, fTimePoint);
               this.AddWeather(nType19, fTimePoint, fTime);
               nLastWeatherType = nType19;
               GameObject.LogLine("Make: WYNIK typ " + nType19 + " time " + fTime + " od " + fTimePoint);
         }

         if (fTime > 0.0F) {
            fTimePoint += fTime;
            if (fTimePoint >= this.m_nHalfTime * 2 - 5) {
               fTimePoint = this.m_nHalfTime * 2;
            }
         } else {
            fTimePoint = this.m_nHalfTime * 2;
         }
      }
   }

   void ActivateAIs(boolean bActivate) {
      this.m_bAIActive = bActivate;

      for (int i = 0; i < 2; i++) {
         if (this.m_teams[i] != null) {
            for (int f = 0; f < this.m_teams[i].m_vFootBallers.size(); f++) {
               AIFootBaller cFBaller = (AIFootBaller)this.m_teams[i].m_vFootBallers.elementAt(f);
               cFBaller.SetActiveAI(bActivate);
            }
         }
      }
   }

   void ClearTeams() {
      this.m_teams[0] = null;
      this.m_teams[1] = null;
   }

   void AddWeather(int kind, float starttime, float maxTime) {
      this.m_Events.NewEvent(kind, starttime, maxTime);
   }

   Weather GetWeather() {
      return this.m_Weather;
   }

   void RestoreStartPositions(boolean bBallPos) {
      for (int i = 0; i < 2; i++) {
         Team team = this.m_teams[i];

         for (int j = 0; j < team.GetFootBallerCount(); j++) {
            AIFootBaller fb = (AIFootBaller)team.GetFootBaller(j);
            fb.ResetSpawnStartPosition();
         }
      }

      this.m_cReferee.ResetSpawnStartPosition();
      Ball cBall = GameField.m_cBall;
      if (cBall.GetCurOwner() != null) {
         ((AIFootBaller)cBall.GetCurOwner()).DetachBall();
      }

      if (bBallPos) {
         GameField.m_cBall.ResetStartPosition();
      }
   }

   float PlayOnGoalReaction(int nTeamScored) {
      ModNet.HostCrowd(this.GetTeam(nTeamScored).m_nAnimalType); // [mod] en ligne : le public du client réagit aussi
      java.util.Vector vWatchers = GameField.GetWatchers();

      for (int i = 0; i < vWatchers.size(); i++) {
         int nAnimalScored = this.GetTeam(nTeamScored).m_nAnimalType;
         ((Watcher)vWatchers.elementAt(i)).CallOnGoalReaction(nAnimalScored);
      }

      m_cModule.GetSoccerCamera().PlaySound("Happy.wav");
      return m_cModule.GetSoccerCamera().GetWaveDuration("Happy.wav");
   }

   void StartMatch() {
      GameObject.CrashLog("StartMatch");
      Team cTeamLeft = this.GetTeam(0);
      Team cTeamRight = this.GetTeam(1);
      GameObject.LogLine(" TL: " + cTeamLeft + " TR: " + cTeamRight);
      cTeamLeft.ResetSuper();
      cTeamRight.ResetSuper();
      m_cModule.GetSoccerCamera().ForceUpdate();
      m_cModule.StartFadeIn();
      this.m_cClockRef.Start();
      this.GoLive();
      this.m_bJustMatchStarted = true;
      this.m_bInited = true;
      ((AIMatch)this).Restart();
   }

   void ResumeMatch() {
      this.m_cClockRef.Start();
   }

   void SetReferee(AIReferee cReferee) {
      this.m_cReferee = cReferee;
   }

   Match() {
      this.m_teams[0] = null;
      this.m_teams[1] = null;
      // [mod] ligue en ligne : la durée choisie à la création de la ligue, pas celle du profil
      this.m_nHalfTime = ModOnlineLeague.m_bMatch ? ModOnlineLeague.GetHalfSeconds() : CoreLogic.GetPlayer().GetHalfTime();
   }

   void SetTeam(int nInd, Team cTeam) {
      this.m_teams[nInd] = cTeam;
      cTeam.m_nSide = nInd;
   }

   void SetModule(SoccerModule module) {
      m_cModule = module;
      this.m_cClockRef = m_cModule.m_Clock;
   }

   void Record() {
      GameField.m_cBall.Record();
      Team cTeam = null;
      FootBaller fb = null;

      for (int i = 0; i < 2; i++) {
         cTeam = this.GetTeam(i);

         for (int j = 0; j < cTeam.GetFootBallerCount(); j++) {
            fb = cTeam.GetFootBaller(j);
            fb.Record();
         }
      }
   }

   boolean IsFirstHalf() {
      return this.m_nHalfNo % 2 == 0;
   }

   void UpdateWeather(float fTime) {
      this.m_Weather.UpdateWeather(fTime);
   }

   void EndMatch() {
   }

   void PauseMatch() {
      this.m_cClockRef.Stop();
   }

   void Replay() {
      if (!m_bPlaying) {
         m_cModule.PlayReplayMusic();
         m_bPlaying = true;
         this.m_bInited = false;
         int nZone = GameField.GetZoneForPoint(GameField.m_cBall.GetPositionVectorVolatile(), 0);
         CameraDummy cam = null;
         switch (nZone) {
            case 0:
            case 3:
               cam = SoccerModule.cField.GetRandomStaticReplayCamera(0);
               break;
            case 1:
            default:
               cam = (CameraDummyRight)m_cModule.CreateObject("CameraDummyRight");
               Vector vPos = GameField.cCenter.GetPosition();
               vPos.fY += 500.0F;
               cam.SetPosition(vPos);
               if (SoccerGame.m_bDebug) {
                  cam = null;
               }
               break;
            case 2:
            case 4:
               cam = SoccerModule.cField.GetRandomStaticReplayCamera(1);
         }

         if (cam != null) {
            m_cModule.GetSoccerCamera().SetReplayCamera(cam);
         }

         GameField.m_cBall.Replay();
         m_cModule.GetSoccerCamera().SetObserved(GameField.m_cBall);
         Team cTeam = null;
         FootBaller fb = null;

         for (int i = 0; i < 2; i++) {
            cTeam = this.GetTeam(i);

            for (int j = 0; j < cTeam.GetFootBallerCount(); j++) {
               fb = cTeam.GetFootBaller(j);
               fb.Replay();
               ((AIFootBaller)fb).SetActiveAI(false);
               ((AIFootBaller)fb).ResetFocusMark();
            }
         }
      }
   }

   void delete() {
      this.m_cBallPossesor = null;
      if (this.m_teams[0] != null) {
         this.m_teams[0].delete();
      }

      this.m_teams[0] = null;
      if (this.m_teams[1] != null) {
         this.m_teams[1].delete();
      }

      this.m_teams[1] = null;
      this.m_teams = null;
      if (this.m_cStrategy != null) {
         this.m_cStrategy.delete();
      }

      this.m_cStrategy = null;
      m_cModule = null;
      this.m_Weather = null;
      if (this.m_Events != null) {
         this.m_Events.delete();
      }

      this.m_Events = null;
      this.m_cClockRef = null;
      this.m_cReferee = null;
   }

   void GoLive() {
      for (int i = 0; i < 2; i++) {
         if (this.m_teams[i] != null) {
            for (int f = 0; f < this.m_teams[i].m_vFootBallers.size(); f++) {
               FootBaller cFBaller = (FootBaller)this.m_teams[i].m_vFootBallers.elementAt(f);
               cFBaller.GoLive();
            }
         }
      }

      this.m_cReferee.GoLive();
   }
}
