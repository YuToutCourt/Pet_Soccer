public class PetSoccerHud extends SoccerUserInterface {
   static final String _SUPER_INC_SOUND = "data/sounds/super_inc.wav";
   HudClock cClock = null;
   HudInfo cInfo = null;
   HudEvent cEvent = null;
   HudSuper cSuper1 = null;
   HudSuper cSuper2 = null;
   UIWindow cArrowGreen = null;
   UIWindow cArrowRed = null;
   HudField cFieldMap = null;
   int nLettersLight = 0;

   public void OnFrame() {
      super.OnFrame();
      int nPlayer1Team = SoccerModule.m_cMatch.m_nPlayer1Team;
      int nPlayer2Team = nPlayer1Team == 0 ? 1 : 0;
      this.UpdateArrow(SoccerModule.m_cMatch.GetTeam(nPlayer1Team));
      if (CoreLogic.m_bMultiplayer) {
         this.UpdateArrow(SoccerModule.m_cMatch.GetTeam(nPlayer2Team));
      }

      ModSwitch.UpdateNumbers(this); // [mod] numéros au-dessus des joueurs humains
   }

   void UpdateFBPosition(FootBaller cFB) {
      this.cFieldMap.UpdateFBPosition(cFB);
   }

   void UpdateBallPosition(Ball cBall) {
      if (this.cFieldMap != null) {
         this.cFieldMap.UpdateBallPosition(cBall);
      }
   }

   void InitHud() {
      this.cClock = (HudClock)this.GetElem("ID_CLOCK");
      if (this.cClock == null) {
         GameObject.CrashLog("[Error]No clock in UI\n");
      } else {
         this.cClock.Init();
      }

      this.cInfo = (HudInfo)this.GetElem("ID_INFO");
      if (this.cInfo == null) {
         GameObject.CrashLog("[Error]No goalcounter in UI\n");
      } else {
         this.cInfo.Init();
      }

      this.cEvent = (HudEvent)this.GetElem("ID_EVENT");
      if (this.cEvent == null) {
         GameObject.CrashLog("[Error]No ID_EVENT in UI\n");
      } else {
         this.cEvent.Init();
      }

      this.cSuper1 = (HudSuper)this.GetElem("ID_SUPER_1");
      if (this.cSuper1 == null) {
         GameObject.CrashLog("[Error]No ID_SUPER_1 in UI\n");
      } else {
         this.cSuper1.Init();
      }

      this.cSuper2 = (HudSuper)this.GetElem("ID_SUPER_2");
      if (this.cSuper2 == null) {
         GameObject.CrashLog("[Error]No ID_SUPER_2 in UI\n");
      } else {
         this.cSuper2.Init();
      }

      this.cFieldMap = (HudField)this.GetElem("ID_FIELD");
      if (this.cFieldMap == null) {
         GameObject.CrashLog("[Error]No ID_FIELD in UI\n");
      } else {
         this.cFieldMap.Init();
      }

      if (SoccerGame.m_bDebug) {
         this.RegisterKeyRequest();
      }

      this.cArrowGreen = this.GetElem("ID_ARROW_1");
      this.cArrowGreen.Hide();
      if (this.cArrowGreen == null) {
         GameObject.CrashLog("[Error]No ID_ARROW_1 in UI\n");
      }

      this.cArrowRed = this.GetElem("ID_ARROW_2");
      this.cArrowRed.Hide();
      if (this.cArrowRed == null) {
         GameObject.CrashLog("[Error]No ID_ARROW_2 in UI\n");
      }

      GameObject.CrashLog("Hud inited\n");
   }

   void OnKeyPressed(int nKey, char cAscii) {
      if (SoccerGame.m_bDebug) {
         this.OnKeyPressedDebug(nKey, cAscii);
      }

      switch (nKey) {
         case 28:
            this.cInfo.Show();
      }
   }

   void LightSuperLetters(int nCount, int nSuperType, int nTeam) {
      // [mod] en ligne : l'hôte diffuse les appels du HUD, le client ne joue que ceux de l'hôte
      if (!ModNet.OnHud(ModNet._HUD_LETTERS, nCount, nSuperType, nTeam)) {
         return;
      }

      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("[mod] HUD super : cote " + nTeam + ", " + nCount + " lettres, type " + nSuperType + "\n");
      }

      if (ModNet.SupersOn()) {
         if (nTeam == 0) {
            if (this.cSuper1 != null) {
               this.cSuper1.LightLetters(nCount, nSuperType, nTeam == SoccerModule.m_cMatch.m_nPlayer1Team);
            }
         } else if (this.cSuper2 != null) {
            this.cSuper2.LightLetters(nCount, nSuperType, nTeam == SoccerModule.m_cMatch.m_nPlayer1Team);
         }

         if (nCount >= 5) {
            if (CoreLogic.m_bMultiplayer) {
               this.PlaySound("data/sounds/super_inc.wav");
            }

            if (!CoreLogic.m_bDemo && !CoreLogic.m_bMultiplayer && SoccerModule.m_cMatch.m_nPlayer1Team == nTeam) {
               this.PlaySound("data/sounds/super_inc.wav");
            }
         }
      }
   }

   PetSoccerHud() {
      super.m_sFiles = new String[]{"data\\menu\\scr\\Hud640.scr", "data\\menu\\scr\\Hud.scr", "data\\menu\\scr\\Hud1024.scr"};
   }

   void UpdateScore(int nLeft, int nRight) {
      if (this.cInfo != null) {
         this.cInfo.Update(nLeft, nRight);
      }
   }

   void OnKeyPressedDebug(int nKey, char cAscii) {
      switch (nKey) {
         case 2:
            this.nLettersLight++;
            this.nLettersLight %= 6;
            if (this.cSuper1 != null) {
               this.cSuper1.LightLetters(this.nLettersLight, 0, true);
            }

            if (this.cSuper2 != null) {
               this.cSuper2.LightLetters(this.nLettersLight, 0, true);
            }
            break;
         case 3:
            this.cInfo.StartFadeIn();
            break;
         case 4:
            this.cInfo.StartFadeOut();
            break;
         case 5:
            this.cInfo.StartSlideIn();
            break;
         case 6:
            this.cInfo.StartSlideOut();
            break;
         case 7:
            this.cEvent.ShowEvent(2);
            break;
         case 8:
            this.cEvent.ShowEvent(0);
            break;
         case 9:
            this.cEvent.ShowEvent(1);
      }
   }

   void SetTeamsInfo(String sNameLeft, UITexture cTextureLeft, String sNameRight, UITexture cTextureRight) {
      if (this.cInfo != null) {
         this.cInfo.SetTeams(sNameLeft, cTextureLeft, sNameRight, cTextureRight);
      }
   }

   void HideInfo() {
      if (!ModNet.OnHud(ModNet._HUD_HIDE_INFO, 0, 0, 0)) { // [mod]
         return;
      }

      this.cInfo.Hide();
   }

   void SwitchSuperOn(int nTeam) {
      if (!ModNet.OnHud(ModNet._HUD_SWITCH, nTeam, 0, 0)) { // [mod]
         return;
      }

      if (!CoreLogic.m_bDemo && (!CoreLogic.m_bChampionships || !SoccerModule.m_cMatch.m_bGoldGoal) && ModNet.SupersOn()) {
         this.cSuper1.Show();
         this.cSuper1.SetActive(true);
         this.cSuper2.Show();
         this.cSuper2.SetActive(true);
      } else {
         this.cSuper1.Hide();
         this.cSuper1.SetActive(false);
         this.cSuper2.Hide();
         this.cSuper2.SetActive(false);
      }
   }

   void UpdateArrow(Team cTeam) {
      if (cTeam != null) {
         if (Match.m_cMatchData != null) {
            int nTeam = cTeam.m_nSide;
            UIWindow cArrow = cTeam.m_cHudArrow;
            if (cArrow == null) {
               if (CoreLogic.m_bChampionships && !Match.m_cMatchData.IsPlayerHost()) {
                  nTeam = nTeam == 0 ? 1 : 0;
               }

               if (nTeam == 0) {
                  cTeam.m_cHudArrow = this.cArrowGreen;
               } else {
                  cTeam.m_cHudArrow = this.cArrowRed;
               }

               cArrow = cTeam.m_cHudArrow;
            }

            FootBaller cFocus = cTeam.m_cFocusFBaller;
            if (cFocus == null) {
               cArrow.Hide();
            } else {
               if (cFocus.IsInFrustum()) {
                  cArrow.Hide();
               } else {
                  SoccerCamera camera = SoccerGame.GetSoccerModule().GetSoccerCamera();
                  Vector vPosOnScreen = camera.PointToScreenClampToFrustum(cFocus.GetPositionVectorVolatile());
                  int _LEFT = 1;
                  int _TOP = 2;
                  int _RIGHT = 4;
                  int _BOTTOM = 8;
                  int nPos = 0;
                  if (vPosOnScreen.fX <= 0.0F) {
                     vPosOnScreen.fX = 0.0F;
                     nPos |= 1;
                  }

                  if (vPosOnScreen.fX >= GameObject.GetScreenWidth()) {
                     vPosOnScreen.fX = GameObject.GetScreenWidth() - cArrow.GetSize().fX;
                     nPos |= 4;
                  }

                  if (vPosOnScreen.fY <= 0.0F) {
                     nPos |= 2;
                     vPosOnScreen.fY = 0.0F;
                  }

                  if (vPosOnScreen.fY >= GameObject.GetScreenHeight()) {
                     nPos |= 8;
                     vPosOnScreen.fY = GameObject.GetScreenHeight() - cArrow.GetSize().fY;
                  }

                  float fAngle = 0.0F;
                  switch (nPos) {
                     case 1:
                        fAngle = 0.0F;
                        break;
                     case 2:
                        fAngle = 90.0F;
                        break;
                     case 3:
                        fAngle = 45.0F;
                        break;
                     case 4:
                        fAngle = 180.0F;
                     case 5:
                     case 7:
                     case 10:
                     case 11:
                     default:
                        break;
                     case 6:
                        fAngle = 135.0F;
                        break;
                     case 8:
                        fAngle = 270.0F;
                        break;
                     case 9:
                        fAngle = 315.0F;
                        break;
                     case 12:
                        fAngle = 225.0F;
                  }

                  cArrow.SetPos(vPosOnScreen);
                  cArrow.SetRotation(fAngle);
                  cArrow.Show();
               }
            }
         }
      }
   }

   void UpdateClock(Vector vTime, boolean bEnding) {
      if (this.cClock != null) {
         this.cClock.Update(vTime, bEnding);
      }
   }

   public void OnInit() {
      super.OnInit();
      this.InitHud();
   }

   void FlashSuperIconEnd(int nTeam) {
      if (!ModNet.OnHud(ModNet._HUD_FLASH_END, nTeam, 0, 0)) { // [mod]
         return;
      }

      if (ModNet.SupersOn()) {
         if (nTeam == 0) {
            this.cSuper1.EndFlashSuper();
         } else {
            this.cSuper2.EndFlashSuper();
         }
      }
   }

   void ShowEventInfo(int nEvent) {
      if (!ModNet.OnHud(ModNet._HUD_EVENT, nEvent, 0, 0)) { // [mod]
         return;
      }

      if (this.cEvent != null) {
         this.cEvent.ShowEvent(nEvent);
      }
   }

   float GetVectorsAngle(Vector v1, Vector v2) {
      float fDot = v1.Dot(v2);
      return Tools.acos(fDot) * 180.0F / (float) Math.PI;
   }

   void ShowInfo() {
      if (!ModNet.OnHud(ModNet._HUD_SHOW_INFO, 0, 0, 0)) { // [mod]
         return;
      }

      this.cInfo.Show();
   }

   public void Show() {
      if (CoreLogic.m_bDemo) {
         this.SetText(Text.Get("&HudDemo&"));
      } else {
         this.SetText("");
      }

      super.Show();
   }

   void OnCreate() {
      super.OnCreate();
      SoccerGame.IncHud();
   }

   public void delete() {
      this.cClock = null;
      this.cInfo = null;
      this.cEvent = null;
      this.cSuper1 = null;
      this.cSuper2 = null;
      this.cArrowGreen = null;
      this.cArrowRed = null;
      this.cFieldMap = null;
      super.delete();
   }

   void OnChangeCameraMode(int nMode) {
      this.cFieldMap.OnChangeCameraMode(nMode);
   }

   void FlashSuperIcon(int nTeam) {
      if (!ModNet.OnHud(ModNet._HUD_FLASH, nTeam, 0, 0)) { // [mod]
         return;
      }

      if (ModNet.SupersOn()) {
         if (nTeam == 0) {
            this.cSuper1.FlashSuper();
         } else {
            this.cSuper2.FlashSuper();
         }
      }
   }
}
