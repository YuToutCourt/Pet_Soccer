class Cheat extends GameObject {
   static boolean m_bAllTeams = false;
   static boolean m_bAllExtras = false;

   String alltextras() {
      if (SoccerMainMenuModule.m_nCurUI != 0) {
         return "<dziala tylko w menu glownym>";
      }

      m_bAllExtras = !m_bAllExtras;
      ModOptions.Save(); // [mod] le code reste un raccourci du menu Mods
      return m_bAllExtras ? "<allextras ON>" : "<allextras OFF>";
   }

   String givesuperright(int nSuper) {
      if (SoccerGame.activeLevelModule == null) {
         return "<dziala tylko w grze>";
      }

      String sRet = "";
      switch (nSuper) {
         case 0:
            sRet = "<super strzal>";
            SoccerModule.m_cMatch.GetTeam(1).GiveSuper(nSuper);
            break;
         case 1:
            sRet = "<super wzwod>";
            SoccerModule.m_cMatch.GetTeam(1).GiveSuper(nSuper);
            break;
         case 2:
            sRet = "<super predkosc>";
            SoccerModule.m_cMatch.GetTeam(1).GiveSuper(nSuper);
            break;
         case 3:
            sRet = "<super sciana jest niedostepna>";
            break;
         default:
            if (ModSupers.IsModSuper(nSuper)) {
               // [mod] gel, ballon fou, aimant, tir double
               sRet = "<super mod " + nSuper + ">";
               SoccerModule.m_cMatch.GetTeam(1).GiveSuper(nSuper);
            } else {
               sRet = "<mozliwe wartosci parametru: 0,1,2,3>";
            }
      }

      return sRet;
   }

   String endmatch(int nHosts, int nGuests) {
      if (SoccerGame.activeLevelModule == null) {
         return "<brak aktywnego modulu gry>";
      } else {
         SoccerModule mod = Match.m_cModule;
         if (AIMatch.IsMatchState(0)) {
            SoccerModule.m_cMatch.m_nScoreLeft = nHosts;
            SoccerModule.m_cMatch.m_nScoreRight = nGuests;
            SoccerModule.m_cMatch.EndMatch();
            return "<ok>";
         } else {
            return "<dziala tylko w trakcie normalnej gry>";
         }
      }
   }

   String champfinal(int nPlace) {
      nPlace--;
      if (SoccerGame.activeLevelModule != null) {
         return "<dziala tylko w menu>";
      } else if (SoccerMainMenuModule.m_nCurUI != 11) {
         return "<dziala tylko na ekranie grup>";
      } else if (nPlace >= 0 && nPlace <= 2) {
         String sRet = "";
         SoccerUserInterface ui = SoccerGame.firstMenuModule.GetUI(SoccerMainMenuModule.m_nCurUI);
         sRet = CoreLogic.GetPlayer().m_cChampionships.PassQualifications(nPlace);
         ui.Show();
         return sRet;
      } else {
         return "<mozliwe wartosci: 1,2,3>";
      }
   }

   String stopTime() {
      if (SoccerGame.activeLevelModule == null) {
         return "<dziala tylko w grze>";
      }

      SoccerGame.GetSoccerModule().m_Clock.Stop();
      return "<ok>";
   }

   String startTime() {
      if (SoccerGame.activeLevelModule == null) {
         return "<dziala tylko w grze>";
      }

      SoccerGame.GetSoccerModule().m_Clock.Start();
      return "<ok>";
   }

   String champwin() {
      if (SoccerGame.activeLevelModule != null) {
         return "<dziala tylko w menu>";
      }

      if (SoccerMainMenuModule.m_nCurUI != 11 && SoccerMainMenuModule.m_nCurUI != 12) {
         return "<dziala tylko na ekranie grup i drzewka mistrzostw>";
      }

      SoccerUserInterface ui = SoccerGame.firstMenuModule.GetUI(SoccerMainMenuModule.m_nCurUI);
      CoreLogic.GetPlayer().m_cChampionships.WinChampionships();
      ui.Show();
      return "<ok>";
   }

   String sinkfbs(int nTeam) {
      if (nTeam != 0 && nTeam != 1) {
         return "<0 zespol lewy, 1 zespol prawy>";
      }

      GameObject[] tab = GameObject.GetObjectsFromClass("AIFootBaller");

      for (int i = 0; i < tab.length; i++) {
         Vector vPos = ((AIFootBaller)tab[i]).GetPosition();
         vPos.fY -= 1000.0F;
         if (((AIFootBaller)tab[i]).m_nTeam == nTeam) {
            ((AIFootBaller)tab[i]).SetPositionWithY(vPos);
            ((AIFootBaller)tab[i]).SetPhysicsType(0);
         }
      }

      return "<OK>";
   }

   String setDiffLevelRaw(int nLevel) {
      // [mod] niveaux 8..10 (ModDifficulty)
      if (nLevel < 1 || nLevel > 10) {
         return "<poziomy maja zakres 1..10>";
      } else if (SoccerModule.m_cMatch != null && SoccerModule.m_cMatch.m_cStrategy != null) {
         SoccerModule.m_cMatch.m_cStrategy.FixDifficultyLevel(nLevel);
         return "<OK>";
      } else {
         return "<nie moge odwolac sie do SoccerModule.m_cMatch.m_cStrategy -> null>";
      }
   }

   void sinkfbs() {
      this.sinkfbs(0);
      this.sinkfbs(1);
   }

   String endhalf(int nHosts, int nGuests) {
      if (SoccerGame.activeLevelModule == null) {
         return "<brak aktywnego modulu gry>";
      }

      SoccerModule mod = Match.m_cModule;
      if (AIMatch.IsMatchState(0)) {
         SoccerModule.m_cMatch.m_nScoreLeft = nHosts;
         SoccerModule.m_cMatch.m_nScoreRight = nGuests;
         Match match = SoccerModule.m_cMatch;
         if (match.IsFirstHalf()) {
            mod.m_Clock.Reset(match.m_nHalfTime);
         } else {
            if (match.m_bGoldGoal) {
               return "<nie dziala przy zlotym strzale>";
            }

            mod.m_Clock.Reset(2 * match.m_nHalfTime);
         }

         return "<ok>";
      } else {
         return "<dziala tylko w trakcie normalnej gry (gdy czas plynie)>";
      }
   }

   void writeToCrash() {
      if (SoccerModule.m_cMatch != null) {
         SoccerGame.GetSoccerModule().TimerStop();
         SoccerModule.m_cMatch.WrtiteStateAsCrashLog();
         SoccerGame.GetSoccerModule().TimerStart();
      }
   }

   void fps() {
      if (SoccerGame.activeLevelModule != null) {
         SoccerGame.m_bShowFPS = !SoccerGame.m_bShowFPS;
         ModOptions.Save(); // [mod]
         PetSoccerHud hud = SoccerModule.m_cHud;
         UIWindow fps = hud.GetElem("ID_FPS");
         if (fps != null) {
            if (SoccerGame.m_bShowFPS) {
               fps.Show();
            } else {
               fps.Hide();
            }
         }
      }
   }

   String setDiffLevel(int nLevel) {
      if (SoccerGame.activeLevelModule != null) {
         return "<dziala tylko w menu>";
      } else if (nLevel >= 1 && nLevel <= 7) {
         CoreLogic.GetPlayer().m_nDiffLevel = nLevel;
         return "<OK>";
      } else if (nLevel >= 8 && nLevel <= 10) {
         // [mod] 8..10 = reglages Expert / Maitre / Legende du menu (CoreLogic.GetDiffLevel)
         CoreLogic.GetPlayer().m_nDiffSetting = nLevel - 5;
         return "<OK reglage " + (nLevel - 5) + ">";
      } else {
         return "<poziomy maja zakres 1..10>";
      }
   }

   String getDiffLevel() {
      return "poziom " + CoreLogic.GetDiffLevel();
   }

   String allteams() {
      if (SoccerMainMenuModule.m_nCurUI != 0) {
         return "<dziala tylko w menu glownym>";
      }

      m_bAllTeams = !m_bAllTeams;
      ModOptions.Save(); // [mod] le code reste un raccourci du menu Mods
      return m_bAllTeams ? "<allteams ON>" : "<allteams OFF>";
   }

   String givesuperleft(int nSuper) {
      if (SoccerGame.activeLevelModule == null) {
         return "<dziala tylko w grze>";
      }

      String sRet = "";
      switch (nSuper) {
         case 0:
            sRet = "<super strzal>";
            SoccerModule.m_cMatch.GetTeam(0).GiveSuper(nSuper);
            break;
         case 1:
            sRet = "<super wzwod>";
            SoccerModule.m_cMatch.GetTeam(0).GiveSuper(nSuper);
            break;
         case 2:
            sRet = "<super predkosc>";
            SoccerModule.m_cMatch.GetTeam(0).GiveSuper(nSuper);
            break;
         case 3:
            sRet = "<super sciana>";
            SoccerModule.m_cMatch.GetTeam(0).GiveSuper(nSuper);
            sRet = "<super sciana jest niedostepna>";
            break;
         default:
            if (ModSupers.IsModSuper(nSuper)) {
               // [mod] gel, ballon fou, aimant, tir double
               sRet = "<super mod " + nSuper + ">";
               SoccerModule.m_cMatch.GetTeam(0).GiveSuper(nSuper);
            } else {
               sRet = "<mozliwe wartosci parametru: 0,1,2,3>";
            }
      }

      return sRet;
   }
}
