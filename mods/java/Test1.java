public class Test1 extends SoccerModule {
   void InitTeams() {
      super.InitTeams();
      GameObject.LogLine(" INIT TEEAMS --- ");
      int nClothesLeft = 0;
      int nClothesRight = 0;
      int nAnimalLeft = -1;
      int nAnimalRight = -1;
      nAnimalLeft = CoreLogic.m_cTeamDataLeft.m_nAnimalType;
      nAnimalRight = CoreLogic.m_cTeamDataRight.m_nAnimalType;
      nClothesLeft = CoreLogic.m_cTeamDataLeft.m_nAnimalClothes;
      nClothesRight = CoreLogic.m_cTeamDataRight.m_nAnimalClothes;
      AIReferee cReferee = this.AddReferee(nAnimalLeft, nAnimalRight, "Sedzia");
      cReferee.FixValidPos();
      SoccerModule.m_cMatch.m_cReferee = cReferee;
      GameObject.LogLine(" INIT TEEAMS 1 --- ");
      AITeam cAITeam = new AITeam();
      cAITeam.SetParams(SoccerModule.m_cAISystem, this);
      cAITeam.SetName("Polska");
      cAITeam.m_nAnimalType = nAnimalLeft;
      SoccerModule.m_cMatch.SetTeam(0, cAITeam);
      GameObject.LogLine(" Team left --- " + cAITeam);
      this.AddFootBaller(0, nAnimalLeft, nClothesLeft, "Kaczmarek");
      this.AddFootBaller(0, nAnimalLeft, nClothesLeft, "Mlynarczyk");
      this.AddFootBaller(0, nAnimalLeft, nClothesLeft, "Boniek");
      this.AddFootBaller(0, nAnimalLeft, nClothesLeft, "Warzycha");
      this.AddFootBaller(0, nAnimalLeft, nClothesLeft, "Platini");
      this.AddFootBaller(0, nAnimalLeft, nClothesLeft, "Waldoch");
      this.AddFootBaller(0, nAnimalLeft, nClothesLeft, "PeH");
      cAITeam.FixOrder();
      if (CoreLogic.m_bDemo) {
         cAITeam.SetUserControl(false, null);
      } else if (CoreLogic.m_bChampionships) {
         cAITeam.SetUserControl(Match.m_cMatchData.IsPlayerHost(), SoccerGame.GetKb0());
         if (SoccerGame.m_bDebug) {
            GameObject.CrashLog("team left user control " + Match.m_cMatchData.IsPlayerHost());
         }
      } else {
         cAITeam.SetUserControl(true, SoccerGame.GetKb0());
      }

      cAITeam = new AITeam();
      cAITeam.SetParams(SoccerModule.m_cAISystem, this);
      cAITeam.SetName("Holandia");
      cAITeam.m_nAnimalType = nAnimalRight;
      SoccerModule.m_cMatch.SetTeam(1, cAITeam);
      GameObject.LogLine(" Team right --- " + cAITeam);
      this.AddFootBaller(1, nAnimalRight, nClothesRight, "Van basten");
      this.AddFootBaller(1, nAnimalRight, nClothesRight, "Hulit");
      this.AddFootBaller(1, nAnimalRight, nClothesRight, "Rejkard");
      this.AddFootBaller(1, nAnimalRight, nClothesRight, "Kuman R");
      this.AddFootBaller(1, nAnimalRight, nClothesRight, "Kuman S");
      this.AddFootBaller(1, nAnimalRight, nClothesRight, "Tratttta");
      this.AddFootBaller(1, nAnimalRight, nClothesRight, "Abak");
      cAITeam.FixOrder();
      if (CoreLogic.m_bMultiplayer) {
         cAITeam.SetUserControl(true, SoccerGame.GetKb1());
         // [mod] en ligne : l'équipe de droite est celle du joueur distant. Chez l'hôte, sa manette reçoit
         // les touches du réseau ; chez le client, elle est sourde (la manette de gauche capte ses touches).
         if (ModNet.IsOnline()) {
            cAITeam.GetTeamController().SetRemote();
         }
      } else if (CoreLogic.m_bDemo) {
         cAITeam.SetUserControl(false, null);
      } else if (CoreLogic.m_bChampionships) {
         cAITeam.SetUserControl(!Match.m_cMatchData.IsPlayerHost(), SoccerGame.GetKb0());
         if (SoccerGame.m_bDebug) {
            GameObject.CrashLog("team right user control " + !Match.m_cMatchData.IsPlayerHost());
         }
      } else {
         cAITeam.SetUserControl(false, null);
      }

      GameField.SetDestPos(GameField.m_cBall.GetPosition(), false);
      ModNet.OnTeamsReady(SoccerModule.m_cMatch); // [mod] en ligne
   }

   void ReplayStop() {
      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("REPLAYSTOP");
      }

      SoccerModule.m_cMatch.ReplayStop();
   }

   public void OnCreate() {
      if (CoreLogic.m_cTeamDataLeft == null || CoreLogic.m_cTeamDataRight == null) {
         CoreLogic.m_cTeamDataLeft = ETeams.GetTeamData(CoreLogic.m_nNextLevel);
         if (CoreLogic.m_nNextLevel != 3) {
            CoreLogic.m_cTeamDataRight = ETeams.GetTeamData(3);
         } else {
            CoreLogic.m_cTeamDataRight = ETeams.GetTeamData(1);
         }

         CoreLogic.SetPlayer(SoccerGame.GetCoreLogic().GetPlayer(0));
      }

      if (SoccerGame.m_bDebug && SoccerGame.m_bForceMultiplayer) {
         CoreLogic.m_bMultiplayer = true;
      }

      if (ModNet.IsOnline()) { // [mod] en ligne : un match en ligne est un multi local dont la manette 2 vient du réseau
         CoreLogic.m_bMultiplayer = true;
         CoreLogic.m_bDemo = false;
      }

      if (SoccerGame.m_bDebug && SoccerGame.m_bForceDemo) {
         CoreLogic.m_bDemo = true;
      }

      super.OnCreate();
      this.SetShadowMapTarget(GameField.m_cBall);
      this.SetShadowMapBrightness(0.75F);
      this.SetShadowMapFrustum(55.0F, 10.0F, 10000.0F);
   }

   void OnInputKey(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      super.OnInputKey(nKeyCode, bKeyState, nKeyAscii);
      if (bKeyState) {
         switch (nKeyCode) {
            case 36:
         }
      }
   }

   void Replay() {
      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("REPLAY");
      }

      SoccerModule.m_cMatch.Replay();
   }

   public void InitModule() {
      super.InitModule();
      this.InitTeams();
   }
}
