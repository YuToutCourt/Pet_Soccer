public class Commentator extends MeshObject {
   static final String _ANIM_EXCITED = "wielkie_emocje";
   static final String _ANIM_NERVOUS = "denerwuje_sie";
   static final String _ANIM_TALK = "gadanie";
   static final String _ANIM_TALK_SHORT = "gadanie_krotkie";
   static final String _ANIM_CLAPPING = "bicie_brawa";
   float m_fRotationF = 0.0F;
   float m_fRotationUp = 0.0F;
   boolean m_bVisible = true;
   float m_fHideDelayTime = 0.5F;
   java.util.Vector m_vToSay = new java.util.Vector();
   int m_nCurrentTalkId = -1;
   int m_nCurrComment = -1;
   boolean m_bCommentsInGame = true;
   static final String[] m_sIntroduction = new String[]{
      "data/sounds/commentary/" + "097.wav",
      "data/sounds/commentary/" + "098.wav",
      "data/sounds/commentary/" + "099.wav",
      "data/sounds/commentary/" + "100.wav",
      "data/sounds/commentary/" + "101.wav"
   };
   static final int _SAY_INTRODUCTION = 0;
   static final int _SAY_COMMENT_TEAM_SELECTION = 1;
   static final int _SAY_HALF_TIME_COMMENTS = 2;
   static final int _COMM_SHOT = 3;
   static final int _COMM_GOAL = 4;
   static final int _COMM_MISSED_SHOT = 5;
   static final int _COMM_DEFENCE = 6;
   static final int _COMM_SUPER_ACTION = 7;
   static final int _COMM_PASS_CHANCE = 8;
   static final int _COMM_PASS = 9;
   static final int _COMM_TAKE_BALL = 11;
   static final int _COMM_FEINT = 12;
   static final int _COMM_REACT_BALL_TAKEN = 13;
   static final int _COMM_WEATHER = 14;
   static final int _COMM_HEAD = 15;
   static final int _COMM_FUN = 17;
   static final int _COMM_TEAM = 18;
   static final int _COMM_CORNER = 19;
   static final int _COMM_OUT = 20;
   static final String _VAR1 = "v1";
   static final String _VAR2 = "v2";
   static final String _VAR1A = "v1d";
   static final String _VAR2A = "v2d";
   static final String[][] m_sAnnouncingCup = new String[][]{
      {"data/sounds/commentary/" + "104a.wav", "v2", "data/sounds/commentary/" + "104b.wav"},
      {"data/sounds/commentary/" + "102a.wav", "v1", "v2", "data/sounds/commentary/" + "102b.wav"},
      {"data/sounds/commentary/" + "103a.wav", "v1", "data/sounds/commentary/" + "103b.wav", "v2", "data/sounds/commentary/" + "103c.wav"}
   };
   static final String[] m_sTeamChosenComments = new String[]{
      "data/sounds/commentary/" + "124.wav",
      "data/sounds/commentary/" + "125.wav",
      "data/sounds/commentary/" + "126.wav",
      "data/sounds/commentary/" + "127.wav",
      "data/sounds/commentary/" + "128.wav",
      "data/sounds/commentary/" + "129.wav"
   };
   static final String[][] m_sStartTheMatch = new String[][]{
      {"data/sounds/commentary/" + "135a.wav", "v1d", "data/sounds/commentary/" + "135b.wav", "v2d", "data/sounds/commentary/" + "135c.wav"},
      {"data/sounds/commentary/" + "130a.wav", "v1", "data/sounds/commentary/" + "130b.wav", "v2", "data/sounds/commentary/" + "130c.wav"},
      {"data/sounds/commentary/" + "133a.wav", "v1d", "data/sounds/commentary/" + "133b.wav", "v2d", "data/sounds/commentary/" + "133c.wav"},
      {"data/sounds/commentary/" + "131a.wav", "v1", "data/sounds/commentary/" + "131b.wav", "v2", "data/sounds/commentary/" + "131c.wav"},
      {"data/sounds/commentary/" + "139a.wav", "v1d", "data/sounds/commentary/" + "139b.wav", "v2d", "data/sounds/commentary/" + "139c.wav"},
      {"data/sounds/commentary/" + "141a.wav", "v1", "data/sounds/commentary/" + "141b.wav", "v2", "data/sounds/commentary/" + "141c.wav"},
      {"data/sounds/commentary/" + "132a.wav", "v1", "data/sounds/commentary/" + "132b.wav", "v2", "data/sounds/commentary/" + "132c.wav"},
      {"data/sounds/commentary/" + "134a.wav", "v1", "data/sounds/commentary/" + "134b.wav", "v2", "data/sounds/commentary/" + "134c.wav"},
      {"data/sounds/commentary/" + "136a.wav", "v1", "data/sounds/commentary/" + "136b.wav", "v2", "data/sounds/commentary/" + "136c.wav"},
      {"data/sounds/commentary/" + "137a.wav", "v1d", "data/sounds/commentary/" + "137b.wav", "v2d", "data/sounds/commentary/" + "137c.wav"},
      {"data/sounds/commentary/" + "140a.wav", "v1", "data/sounds/commentary/" + "140b.wav", "v2", "data/sounds/commentary/" + "140c.wav"},
      {"data/sounds/commentary/" + "138.wav"}
   };
   static final String[][] m_sStartTheMatchExtra = new String[][]{
      {"data/sounds/commentary/" + "142a.wav", "v1d", "data/sounds/commentary/" + "142b.wav", "v2d", "data/sounds/commentary/" + "142c.wav"},
      {"data/sounds/commentary/" + "143.wav"},
      {"data/sounds/commentary/" + "144a.wav", "v1d", "data/sounds/commentary/" + "144b.wav", "v2d", "data/sounds/commentary/" + "144c.wav"},
      {"data/sounds/commentary/" + "145.wav"}
   };
   static final String[] m_sHalfTimeCommentsA = new String[]{
      "data/sounds/commentary/" + "146.wav",
      "data/sounds/commentary/" + "147.wav",
      "data/sounds/commentary/" + "149.wav",
      "data/sounds/commentary/" + "150.wav",
      "data/sounds/commentary/" + "148.wav"
   };
   static final String[] m_sHalfTimeCommentsB = new String[]{
      "data/sounds/commentary/" + "151.wav",
      "data/sounds/commentary/" + "152.wav",
      "data/sounds/commentary/" + "153.wav",
      "data/sounds/commentary/" + "154.wav",
      "data/sounds/commentary/" + "155.wav"
   };
   static final String[][] m_sGoal = new String[][]{
      {"data/sounds/commentary/" + "186.wav", "v1", "data/sounds/commentary/" + "187.wav", "v2"},
      {"data/sounds/commentary/" + "180.wav"},
      {"data/sounds/commentary/" + "181.wav"},
      {"data/sounds/commentary/" + "182.wav"},
      {"data/sounds/commentary/" + "183.wav"},
      {"data/sounds/commentary/" + "184.wav"},
      {"data/sounds/commentary/" + "185.wav"}
   };
   static final String[][] m_sShot = new String[][]{
      {"data/sounds/commentary/" + "176.wav"}, {"v1", "data/sounds/commentary/" + "177.wav"}, {"v1"}, {"data/sounds/commentary/" + "179.wav"}
   };
   static final String[] m_sMissedShot = new String[]{
      "data/sounds/commentary/" + "217.wav",
      "data/sounds/commentary/" + "218.wav",
      "data/sounds/commentary/" + "219.wav",
      "data/sounds/commentary/" + "220.wav",
      "data/sounds/commentary/" + "221.wav",
      "data/sounds/commentary/" + "222.wav",
      "data/sounds/commentary/" + "223.wav",
      "data/sounds/commentary/" + "224.wav",
      "data/sounds/commentary/" + "225.wav"
   };
   static final String[][] m_sDefence = new String[][]{
      {"data/sounds/commentary/" + "239.wav"},
      {"data/sounds/commentary/" + "241.wav"},
      {"data/sounds/commentary/" + "240.wav"},
      {"data/sounds/commentary/" + "238.wav"}
   };
   static final String[][] m_sPassChance = new String[][]{
      {"v1", "data/sounds/commentary/" + "248.wav", "v2d"}, {"v2", "data/sounds/commentary/" + "247.wav"}, {"v2", "data/sounds/commentary/" + "249.wav"}
   };
   static final String[][] m_sPass = new String[][]{
      {"v1", "data/sounds/commentary/" + "252.wav", "v2d"}, {"data/sounds/commentary/" + "259a.wav", "v2d"}, {"data/sounds/commentary/" + "254.wav"}
   };
   static final String[][] m_sPassAfter = new String[][]{
      {"v2"},
      {"v2"},
      {"v2"},
      {"v2"},
      {"v2"},
      {"v2"},
      {"v2"},
      {"v2"},
      {"v2"},
      {"data/sounds/commentary/" + "253.wav"},
      {"data/sounds/commentary/" + "256.wav"},
      {"v1", "data/sounds/commentary/" + "255.wav", "v2d"}
   };
   static final String[][] m_sPassLob = new String[][]{
      {"v1", "data/sounds/commentary/" + "257.wav", "v2d"},
      {"data/sounds/commentary/" + "259a.wav", "v1d", "data/sounds/commentary/" + "259b.wav" + "v2d"},
      {"data/sounds/commentary/" + "258a.wav", "v2d", "data/sounds/commentary/" + "258b.wav"}
   };
   static final String[] m_sFeint = new String[]{
      "data/sounds/commentary/" + "269.wav",
      "data/sounds/commentary/" + "270.wav",
      "data/sounds/commentary/" + "271.wav",
      "data/sounds/commentary/" + "272.wav",
      "data/sounds/commentary/" + "273.wav",
      "data/sounds/commentary/" + "274.wav",
      "data/sounds/commentary/" + "275.wav",
      "data/sounds/commentary/" + "276.wav",
      "data/sounds/commentary/" + "277.wav"
   };
   static final String[] m_sHead = new String[]{
      "data/sounds/commentary/" + "291.wav", "data/sounds/commentary/" + "290.wav", "data/sounds/commentary/" + "292.wav"
   };
   static final String[][] m_sTakeBall = new String[][]{
      {"data/sounds/commentary/" + "261.wav"},
      {"data/sounds/commentary/" + "260.wav"},
      {"data/sounds/commentary/" + "266.wav", "v1d"},
      {"v2", "data/sounds/commentary/" + "262.wav"},
      {"v2", "data/sounds/commentary/" + "295.wav"},
      {"data/sounds/commentary/" + "267a.wav", "v1", "data/sounds/commentary/" + "267b.wav"},
      {"data/sounds/commentary/" + "268.wav"},
      {"data/sounds/commentary/" + "294.wav"}
   };
   static final String[][] m_sTakeBallWSlide = new String[][]{
      {"data/sounds/commentary/" + "293.wav", "v1d"}, {"v1", "data/sounds/commentary/" + "296.wav", "v2d"}
   };
   static final String[] m_sReactBallTaken = new String[]{
      "data/sounds/commentary/" + "263.wav", "data/sounds/commentary/" + "264.wav", "data/sounds/commentary/" + "265.wav"
   };
   static final String[] m_sReactBallTakenWFall = new String[]{
      "data/sounds/commentary/" + "278.wav",
      "data/sounds/commentary/" + "279.wav",
      "data/sounds/commentary/" + "280.wav",
      "data/sounds/commentary/" + "281.wav",
      "data/sounds/commentary/" + "282.wav",
      "data/sounds/commentary/" + "283.wav"
   };
   static final String[] m_sSuperAction = new String[]{
      "data/sounds/commentary/" + "246.wav",
      "data/sounds/commentary/" + "242.wav",
      "data/sounds/commentary/" + "243.wav",
      "data/sounds/commentary/" + "245.wav",
      "data/sounds/commentary/" + "244.wav"
   };
   static final String[] m_sFunGeneral = new String[]{
      "data/sounds/commentary/" + "297.wav",
      "data/sounds/commentary/" + "298.wav",
      "data/sounds/commentary/" + "299.wav",
      "data/sounds/commentary/" + "300.wav"
   };
   static final String[][] m_sFunTeam = new String[][]{
      {"data/sounds/commentary/" + "301.wav", "data/sounds/commentary/" + "302.wav", "data/sounds/commentary/" + "303.wav"},
      {"data/sounds/commentary/" + "307.wav", "data/sounds/commentary/" + "308.wav", "data/sounds/commentary/" + "309.wav"},
      {"data/sounds/commentary/" + "304.wav", "data/sounds/commentary/" + "305.wav", "data/sounds/commentary/" + "306.wav"},
      {"data/sounds/commentary/" + "322.wav", "data/sounds/commentary/" + "323.wav", "data/sounds/commentary/" + "324.wav"},
      {"data/sounds/commentary/" + "316.wav", "data/sounds/commentary/" + "317.wav", "data/sounds/commentary/" + "318.wav"},
      {"data/sounds/commentary/" + "334.wav", "data/sounds/commentary/" + "335.wav", "data/sounds/commentary/" + "336.wav"},
      {"data/sounds/commentary/" + "325.wav", "data/sounds/commentary/" + "326.wav", "data/sounds/commentary/" + "327.wav"},
      {"data/sounds/commentary/" + "319.wav", "data/sounds/commentary/" + "320.wav", "data/sounds/commentary/" + "321.wav"},
      {"data/sounds/commentary/" + "313.wav", "data/sounds/commentary/" + "314.wav", "data/sounds/commentary/" + "315.wav"},
      {"data/sounds/commentary/" + "328.wav", "data/sounds/commentary/" + "329.wav", "data/sounds/commentary/" + "330.wav"},
      {"data/sounds/commentary/" + "310.wav", "data/sounds/commentary/" + "311.wav", "data/sounds/commentary/" + "312.wav"},
      {"data/sounds/commentary/" + "331.wav", "data/sounds/commentary/" + "332.wav", "data/sounds/commentary/" + "333.wav"}
   };
   static final String[][] m_sMatchEndNeutralGood = new String[][]{{"data/sounds/commentary/" + "161.wav"}, {"data/sounds/commentary/" + "163.wav"}};
   static final String[][] m_sMatchEndNeutralBad = new String[][]{
      {"data/sounds/commentary/" + "172.wav"}, {"data/sounds/commentary/" + "173.wav"}, {"data/sounds/commentary/" + "174.wav"}
   };
   static final String[][] m_sMatchEndWin = new String[][]{
      {"v1", "data/sounds/commentary/" + "165.wav"},
      {"data/sounds/commentary/" + "166.wav"},
      {"data/sounds/commentary/" + "158.wav"},
      {"data/sounds/commentary/" + "160.wav"},
      {"data/sounds/commentary/" + "162.wav", "v1"},
      {"data/sounds/commentary/" + "163.wav"}
   };
   static final String[][] m_sMatchEndLoose = new String[][]{
      {"data/sounds/commentary/" + "169.wav"},
      {"data/sounds/commentary/" + "171.wav"},
      {"data/sounds/commentary/" + "172.wav"},
      {"data/sounds/commentary/" + "173.wav"},
      {"data/sounds/commentary/" + "174.wav"},
      {"data/sounds/commentary/" + "168.wav"},
      {"data/sounds/commentary/" + "175a.wav", "v1", "data/sounds/commentary/" + "175b.wav"}
   };
   float fTimeDelay = 0.2F;

   boolean IsActive() {
      return CoreLogic.GetPlayer().m_bCommentatorOn;
   }

   void SayNextPart() {
      if (this.m_vToSay.size() < 1) {
         this.StopTalking();
      } else {
         if (this.IsTalking()) {
            this.StopCurrentWave();
         }

         this.DisableCallMethod("SayNextPart");
         String sToSay = (String)this.m_vToSay.elementAt(0);
         this.m_vToSay.removeElementAt(0);
         float fTalkTime = this.Say(sToSay);
         this.CallMethodIn("SayNextPart", fTalkTime);
      }
   }

   void CommentMissedShot(int nAnimalType) {
      if (!this.IsTalking() || this.CanBreakComment(5, false)) {
         if (this.IsActive()) {
            this.ClearCurrComment();
            this.m_nCurrComment = 5;
            int nScenarioNo = this.Toss(0, m_sMissedShot.length);
            float fTalkTime;
            if (nScenarioNo == m_sMissedShot.length) {
               fTalkTime = this.Say(EAnimals.m_sTeamMissWav[nAnimalType]);
            } else {
               fTalkTime = this.Say(m_sMissedShot[nScenarioNo]);
            }

            this.CallMethodIn("StopTalking", fTalkTime);
         }
      }
   }

   void AnnounceCup(int nCupID, int nFavourite) {
      if (this.IsActive()) {
         int nWayToAnnounce = SoccerGame.GetRandom(0, 2);
         String sCupName = Championships.GetCupNameWav(nCupID);
         String sFavouriteName = EAnimals.GetTeamNameWav(nFavourite);
         String[][] sScenarios = m_sAnnouncingCup;
         this.BuildTalkFromScenario(nWayToAnnounce, sScenarios, sCupName, sFavouriteName);
         this.SayNextPart();
      }
   }

   void CommentFeint() {
      if (!this.IsTalking() || this.CanBreakComment(12, false)) {
         if (this.IsActive()) {
            this.ClearCurrComment();
            this.m_nCurrComment = 12;
            int nScenarioNo = this.Toss(0, m_sFeint.length - 1);
            float fTalkTime = this.Say(m_sFeint[nScenarioNo]);
            this.CallMethodIn("StopTalking", fTalkTime);
         }
      }
   }

   void CommentOut() {
      if (!this.IsTalking() || this.CanBreakComment(20, false)) {
         if (this.IsActive()) {
            this.ClearCurrComment();
            float fTalkTime = this.Say("data/sounds/commentary/" + "out.wav");
            this.CallMethodIn("StopTalking", fTalkTime);
         }
      }
   }

   void BuildTalkFromScenario(int nScenarioID, String[][] sTexts, String sVar1, String sVar2) {
      this.m_vToSay.removeAllElements();
      String[] sText = sTexts[nScenarioID];
      int nCurConst = 0;
      String sNextText = null;

      for (int i = 0; i < sText.length; i++) {
         sNextText = sText[i];
         if (sNextText.equalsIgnoreCase("v1")) {
            sNextText = this.MakeWav(sVar1);
         } else if (sNextText.equalsIgnoreCase("v2")) {
            sNextText = this.MakeWav(sVar2);
         } else if (sNextText.equalsIgnoreCase("v1d")) {
            sNextText = this.MakeWavA(sVar1);
         } else if (sNextText.equalsIgnoreCase("v2d")) {
            sNextText = this.MakeWavA(sVar2);
         }

         this.m_vToSay.addElement(sNextText);
      }
   }

   void CommentPass(AIFootBaller cFBaller, AIFootBaller cFDest) {
      if (!this.IsTalking() || this.CanBreakComment(9, false)) {
         if (this.IsActive()) {
            this.ClearCurrComment();
            this.m_nCurrComment = 9;
            int nScenarioNo = this.Toss(0, m_sPass.length - 1);
            this.BuildTalkFromScenario(
               nScenarioNo,
               m_sPass,
               EAnimals.GetFBNameWav(cFBaller.m_nAnimalType, cFBaller.m_nNumberInTeam),
               EAnimals.GetFBNameWav(cFDest.m_nAnimalType, cFDest.m_nNumberInTeam)
            );
            this.SayNextPart();
         }
      }
   }

   boolean TossHalf(float fChance) {
      float fRnd = GameObject.RandomFloat(100.0F);
      return fRnd < fChance;
   }

   void CommentPassChance(AIFootBaller cFBaller, AIFootBaller cFDest) {
      if (!this.IsTalking() || this.CanBreakComment(8, false)) {
         if (this.IsActive()) {
            this.ClearCurrComment();
            this.m_nCurrComment = 8;
            float fBallerCenetrOff = Math.abs(cFBaller.GetPosition().fX - GameField.cCenter.GetPosition().fX);
            float fDestCenetrOff = Math.abs(cFDest.GetPosition().fX - GameField.cCenter.GetPosition().fX);
            int nScenarioNo = this.Toss(0, m_sPassChance.length - 1);
            if (nScenarioNo == 0) {
               this.BuildTalkFromScenario(
                  nScenarioNo,
                  m_sPassChance,
                  EAnimals.GetFBNameWav(cFBaller.m_nAnimalType, cFBaller.m_nNumberInTeam),
                  EAnimals.GetFBNameWav(cFDest.m_nAnimalType, cFDest.m_nNumberInTeam)
               );
            } else {
               this.BuildTalkFromScenario(
                  nScenarioNo,
                  m_sPassChance,
                  EAnimals.GetFBNameWav(cFBaller.m_nAnimalType, cFBaller.m_nNumberInTeam),
                  EAnimals.GetFBNameWav(cFDest.m_nAnimalType, cFDest.m_nNumberInTeam)
               );
            }

            this.SayNextPart();
         }
      }
   }

   void SayRandom(int nType) {
      this.Show();
      if (this.IsActive()) {
         this.StopTalking();
         float fTalkTime = 0.0F;
         switch (nType) {
            case 0:
               fTalkTime = this.Say(this.GetRandomString(m_sIntroduction));
               break;
            case 1:
               fTalkTime = this.Say(this.GetRandomString(m_sTeamChosenComments));
               break;
            case 2:
               int nScope = m_sHalfTimeCommentsA.length;
               if (SoccerModule.m_cMatch.m_bGoldGoal && nScope > 0) {
                  nScope--;
               }

               this.ClearCurrComment();
               this.m_vToSay.addElement(m_sHalfTimeCommentsA[SoccerGame.GetRandom(0, nScope - 1)]);
               if (!SoccerModule.m_cMatch.m_bGoldGoal) {
                  this.m_vToSay.addElement(m_sHalfTimeCommentsB[SoccerGame.GetRandom(0, m_sHalfTimeCommentsB.length - 1)]);
               }

               this.CallMethodIn("SayNextPart", 0.1F);
               return;
         }

         this.PlayTalkAnim();
         this.CallMethodIn("StopTalking", fTalkTime);
      }
   }

   void StartTheMatch(int nField, int nHosts, int nGuests) {
      this.ClearCurrComment();
      if (this.IsActive()) {
         String sHostName = EAnimals.GetTeamNameWav(nHosts);
         String sGuestName = EAnimals.GetTeamNameWav(nGuests);
         int nScenario = -1;
         if (nField < 15) {
            nScenario = nField;
            this.BuildTalkFromScenario(nScenario, m_sStartTheMatch, sHostName, sGuestName);
         } else {
            nScenario = SoccerGame.GetRandom(0, m_sStartTheMatchExtra.length - 1);
            this.BuildTalkFromScenario(nScenario, m_sStartTheMatchExtra, sHostName, sGuestName);
         }

         this.CallMethodIn("SayNextPart", 0.1F);
      }
   }

   void ClearCurrComment() {
      this.DisableCallMethod("StopTalking");
      this.StopTalking();
      switch (this.m_nCurrComment) {
         case 4:
            this.DisableCallMethod("CommentGoalAddition");
      }
   }

   void CommentWhileChoosingTeam(int nTeam) {
      if (this.IsActive()) {
         this.StopTalking();
         float fTalkTime = this.Say(EAnimals.GetTeamCommentWav(nTeam));
         this.CallMethodIn("StopTalking", fTalkTime);
      }
   }

   void OnInputKeyDebug(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      if (!bKeyState) {
         switch (nKeyCode) {
            case 0:
         }
      }
   }

   void Hide() {
      this.DisableCallMethod("Show");
      this.DisableCallMethod("Hide");
      if (this.m_bVisible) {
         this.m_bVisible = false;
         CommentatorModule cMod = (CommentatorModule)this.GetModule();
         int nPos = 1;
         if (cMod.GetViewFOriginX() > 0.0F && cMod.GetViewFOriginX() < 1.0F) {
            nPos = 1;
         }

         if (cMod.GetViewFOriginX() < 0.0F && cMod.GetViewFOriginX() > -1.0F) {
            nPos = 3;
         }

         switch (nPos) {
            case 1:
               cMod.m_nSlideIn = 5;
               cMod.SetDestViewOrigin(1.0F, -1.0F);
            case 2:
            default:
               break;
            case 3:
               cMod.m_nSlideIn = 6;
               cMod.SetDestViewOrigin(-1.0F, -1.0F);
         }
      }
   }

   void CommentTakeBall(AIFootBaller cFBaller, AIFootBaller cOpp, boolean bSlide, boolean bDefence) {
      if (!this.IsTalking() || this.CanBreakComment(11, false)) {
         if (this.IsActive()) {
            this.ClearCurrComment();
            this.m_nCurrComment = 11;
            if (bDefence && this.TossHalf(50.0F)) {
               float fTalkTime = this.Say(m_sTakeBall[0][0]);
               this.CallMethodIn("StopTalking", fTalkTime);
            } else {
               if (bSlide) {
                  int nScenarioNo = this.Toss(0, m_sTakeBallWSlide.length - 1);
                  this.BuildTalkFromScenario(
                     nScenarioNo,
                     m_sTakeBallWSlide,
                     EAnimals.GetFBNameWav(cFBaller.m_nAnimalType, cFBaller.m_nNumberInTeam),
                     EAnimals.GetFBNameWav(cOpp.m_nAnimalType, cOpp.m_nNumberInTeam)
                  );
               } else {
                  int var7 = this.Toss(0, m_sTakeBall.length - 1);
                  this.BuildTalkFromScenario(
                     var7,
                     m_sTakeBall,
                     EAnimals.GetFBNameWav(cFBaller.m_nAnimalType, cFBaller.m_nNumberInTeam),
                     EAnimals.GetFBNameWav(cOpp.m_nAnimalType, cOpp.m_nNumberInTeam)
                  );
               }

               this.SayNextPart();
            }
         }
      }
   }

   void CommentShot(AIFootBaller cFB) {
      if (!this.IsTalking() || this.CanBreakComment(3, false)) {
         if (this.IsActive()) {
            this.ClearCurrComment();
            this.m_nCurrComment = 3;
            int nScenarioNo = this.Toss(0, m_sShot.length - 1);
            if (nScenarioNo == 0) {
               float fTalkTime = this.Say(m_sShot[nScenarioNo][0]);
               this.CallMethodIn("StopTalking", fTalkTime);
            } else {
               this.BuildTalkFromScenario(nScenarioNo, m_sShot, EAnimals.GetFBNameWav(cFB.m_nAnimalType, cFB.m_nNumberInTeam), "");
               this.SayNextPart();
            }
         }
      }
   }

   void CommentPassAfter(AIFootBaller cFBaller, AIFootBaller cFDest) {
      if (!this.IsTalking() || this.CanBreakComment(9, false)) {
         if (this.IsActive()) {
            this.m_nCurrComment = 9;
            this.ClearCurrComment();
            int nScenarioNo = this.Toss(0, m_sPassAfter.length - 1);
            if (cFBaller == cFDest) {
               if (SoccerGame.m_bDebug) {
                  Tools.CrashLogWarning("Commentator.CommentPassAfter x podaje do x");
               }
            } else {
               this.BuildTalkFromScenario(
                  nScenarioNo,
                  m_sPassAfter,
                  EAnimals.GetFBNameWav(cFBaller.m_nAnimalType, cFBaller.m_nNumberInTeam),
                  EAnimals.GetFBNameWav(cFDest.m_nAnimalType, cFDest.m_nNumberInTeam)
               );
               this.SayNextPart();
            }
         }
      }
   }

   void SetCommVolumeNow(float fVolume) {
      if (this.IsTalking()) {
         this.SetWaveVolume(this.m_nCurrentTalkId, fVolume);
      }
   }

   float Say(String sWavFile) {
      // [mod] en ligne : l'hôte diffuse ce que dit le commentateur, le client ne dit que cela
      if (!ModNet.OnSay(sWavFile)) {
         return 0.0F;
      }

      this.Show();
      if (!this.IsActive()) {
         return 0.0F;
      }

      this.StopCurrentWave();
      this.PlayTalkAnim();
      this.m_nCurrentTalkId = this.GetActiveModule().PlayWave(sWavFile);
      this.SetWaveVolume(this.m_nCurrentTalkId, SoccerGame.GetCommentaryVolume());
      float fTalkTime = this.GetWaveDuration(this.m_nCurrentTalkId);
      return fTalkTime + this.fTimeDelay;
   }

   String MakeWav(String sWav) {
      return !sWav.toLowerCase().endsWith(".wav") ? sWav + ".wav" : sWav;
   }

   void PlayTalkAnim() {
      this.SetAnimMorphTime(0.5F);
      this.PlayAnim("gadanie", -1);
   }

   String GetRandomString(String[] sStrings) {
      int rnd = GameObject.RandomInt(100);
      if (sStrings.length > 0) {
         rnd %= sStrings.length;
         return sStrings[rnd];
      } else {
         return null;
      }
   }

   boolean Toss() {
      float fRnd = GameObject.RandomFloat(100.0F);
      return fRnd > 50.0F;
   }

   int Toss(int nStart, int nMax) {
      int nScope = nMax - nStart;
      int nRnd = GameObject.RandomInt(nScope * 10);
      int nResult = nRnd % (nScope + 1);
      return nStart + nResult;
   }

   void SetOrientation(int nWhere) {
      this.RotateLocal(Vector.Forward, -this.m_fRotationF);
      switch (nWhere) {
         case 0:
            this.m_fRotationF = 180.0F;
            this.RotateLocal(Vector.Forward, this.m_fRotationF);
            break;
         case 1:
            this.m_fRotationF = 90.0F;
            this.RotateLocal(Vector.Forward, this.m_fRotationF);
            break;
         case 2:
            this.m_fRotationF = 0.0F;
            this.RotateLocal(Vector.Forward, this.m_fRotationF);
            break;
         case 3:
            this.m_fRotationF = 270.0F;
            this.RotateLocal(Vector.Forward, this.m_fRotationF);
      }
   }

   void CommentPassChanceReject() {
      if (!this.IsTalking() || this.CanBreakComment(8, false)) {
         if (this.IsActive()) {
            this.ClearCurrComment();
            this.m_nCurrComment = 8;
            float fTalkTime = this.Say("data/sounds/commentary/" + "251.wav");
            this.CallMethodIn("StopTalking", fTalkTime);
         }
      }
   }

   void HideNow() {
      this.DisableCallMethod("Show");
      this.DisableCallMethod("HideNow");
      if (this.m_bVisible) {
         this.m_bVisible = false;
         CommentatorModule cMod = (CommentatorModule)this.GetModule();
         cMod.SetViewOrigin(cMod.m_nView, -1.0F, -1.0F);
         cMod.SetDestViewOrigin(-1.0F, -1.0F);
         cMod.m_nSlideIn = 4;
      }
   }

   void CommentHead(boolean bShot, boolean bPass) {
      if (!this.IsTalking() || this.CanBreakComment(15, false)) {
         if (this.IsActive()) {
            this.ClearCurrComment();
            this.m_nCurrComment = 15;
            int nScenarioNo;
            if (bShot) {
               nScenarioNo = this.Toss(0, 2);
            } else if (bPass) {
               nScenarioNo = this.Toss(0, 1);
            } else {
               nScenarioNo = 0;
            }

            float fTalkTime = this.Say(m_sHead[nScenarioNo]);
            this.CallMethodIn("StopTalking", fTalkTime);
         }
      }
   }

   Commentator() {
      super.MeshName = "data/meshes/animals/papuga.3da";
   }

   void CommentTeam(int nAnimalType) {
      if (!this.IsTalking() || this.CanBreakComment(18, false)) {
         if (this.IsActive()) {
            this.ClearCurrComment();
            int nScenarioNo = this.Toss(0, 2);
            float fTalkTime = this.Say(m_sFunTeam[nAnimalType][nScenarioNo]);
            this.CallMethodIn("StopTalking", fTalkTime);
         }
      }
   }

   void StopCurrentWave() {
      if (this.m_nCurrentTalkId != -1) {
         this.StopWave(this.m_nCurrentTalkId);
      }

      this.m_nCurrentTalkId = -1;
   }

   String GetWavFromScore(int nScore) {
      String sScore = null;
      switch (nScore) {
         case 0:
            sScore = "data/sounds/commentary/" + "188.wav";
            break;
         case 1:
            sScore = "data/sounds/commentary/" + "189.wav";
            break;
         case 2:
            sScore = "data/sounds/commentary/" + "190.wav";
            break;
         case 3:
            sScore = "data/sounds/commentary/" + "191.wav";
            break;
         case 4:
            sScore = "data/sounds/commentary/" + "192.wav";
            break;
         case 5:
            sScore = "data/sounds/commentary/" + "193.wav";
            break;
         case 6:
            sScore = "data/sounds/commentary/" + "194.wav";
            break;
         case 7:
            sScore = "data/sounds/commentary/" + "195.wav";
            break;
         case 8:
            sScore = "data/sounds/commentary/" + "196.wav";
            break;
         case 9:
            sScore = "data/sounds/commentary/" + "197.wav";
            break;
         case 10:
            sScore = "data/sounds/commentary/" + "198.wav";
            break;
         case 11:
            sScore = "data/sounds/commentary/" + "199.wav";
            break;
         case 12:
            sScore = "data/sounds/commentary/" + "200.wav";
            break;
         case 13:
            sScore = "data/sounds/commentary/" + "201.wav";
            break;
         case 14:
            sScore = "data/sounds/commentary/" + "202.wav";
            break;
         case 15:
            sScore = "data/sounds/commentary/" + "203.wav";
            break;
         case 16:
            sScore = "data/sounds/commentary/" + "204.wav";
            break;
         case 17:
            sScore = "data/sounds/commentary/" + "205.wav";
            break;
         case 18:
            sScore = "data/sounds/commentary/" + "206.wav";
            break;
         case 19:
            sScore = "data/sounds/commentary/" + "207.wav";
            break;
         case 20:
            sScore = "data/sounds/commentary/" + "208.wav";
            break;
         case 21:
            sScore = "data/sounds/commentary/" + "209.wav";
            break;
         case 22:
            sScore = "data/sounds/commentary/" + "210.wav";
            break;
         case 23:
            sScore = "data/sounds/commentary/" + "2101.wav";
            break;
         case 24:
            sScore = "data/sounds/commentary/" + "2102.wav";
            break;
         case 25:
            sScore = "data/sounds/commentary/" + "211.wav";
            break;
         case 26:
            sScore = "data/sounds/commentary/" + "212.wav";
            break;
         case 27:
            sScore = "data/sounds/commentary/" + "213.wav";
            break;
         case 28:
            sScore = "data/sounds/commentary/" + "214.wav";
            break;
         case 29:
            sScore = "data/sounds/commentary/" + "215.wav";
            break;
         case 30:
            sScore = "data/sounds/commentary/" + "216.wav";
      }

      return sScore;
   }

   String MakeWavA(String sWav) {
      return this.MakeWav(sWav);
   }

   String GetWavFromScorePl(int nScore, int nSide) {
      String sScore = null;
      switch (nScore) {
         case 0:
            sScore = "data/sounds/commentary/" + "188";
            break;
         case 1:
            sScore = "data/sounds/commentary/" + "189";
            break;
         case 2:
            sScore = "data/sounds/commentary/" + "190";
            break;
         case 3:
            sScore = "data/sounds/commentary/" + "191";
            break;
         case 4:
            sScore = "data/sounds/commentary/" + "192";
            break;
         case 5:
            sScore = "data/sounds/commentary/" + "193";
            break;
         case 6:
            sScore = "data/sounds/commentary/" + "194";
            break;
         case 7:
            sScore = "data/sounds/commentary/" + "195";
            break;
         case 8:
            sScore = "data/sounds/commentary/" + "196";
            break;
         case 9:
            sScore = "data/sounds/commentary/" + "197";
            break;
         case 10:
            sScore = "data/sounds/commentary/" + "198";
            break;
         case 11:
            sScore = "data/sounds/commentary/" + "199";
            break;
         case 12:
            sScore = "data/sounds/commentary/" + "200";
            break;
         case 13:
            sScore = "data/sounds/commentary/" + "201";
            break;
         case 14:
            sScore = "data/sounds/commentary/" + "202";
            break;
         case 15:
            sScore = "data/sounds/commentary/" + "203";
            break;
         case 16:
            sScore = "data/sounds/commentary/" + "204";
            break;
         case 17:
            sScore = "data/sounds/commentary/" + "205";
            break;
         case 18:
            sScore = "data/sounds/commentary/" + "206";
            break;
         case 19:
            sScore = "data/sounds/commentary/" + "207";
            break;
         case 20:
            sScore = "data/sounds/commentary/" + "208";
            break;
         case 21:
            sScore = "data/sounds/commentary/" + "209";
            break;
         case 22:
            sScore = "data/sounds/commentary/" + "210";
            break;
         case 23:
            sScore = "data/sounds/commentary/" + "2101";
            break;
         case 24:
            sScore = "data/sounds/commentary/" + "2102";
            break;
         case 25:
            sScore = "data/sounds/commentary/" + "211";
            break;
         case 26:
            sScore = "data/sounds/commentary/" + "212";
            break;
         case 27:
            sScore = "data/sounds/commentary/" + "213";
            break;
         case 28:
            sScore = "data/sounds/commentary/" + "214";
            break;
         case 29:
            sScore = "data/sounds/commentary/" + "215";
            break;
         case 30:
            sScore = "data/sounds/commentary/" + "216";
      }

      if (nSide == 0) {
         sScore = sScore + "a.wav";
      } else {
         sScore = sScore + "b.wav";
      }

      return sScore;
   }

   void CommentReactBallTaken(boolean bFalling) {
      if (!this.IsTalking() || this.CanBreakComment(13, false)) {
         if (this.IsActive()) {
            this.ClearCurrComment();
            this.m_nCurrComment = 13;
            float fTalkTime = 0.0F;
            if (!bFalling) {
               int nScenarioNo = this.Toss(0, m_sReactBallTaken.length - 1);
               fTalkTime = this.Say(m_sReactBallTaken[nScenarioNo]);
            } else {
               int var4 = this.Toss(0, m_sReactBallTakenWFall.length - 1);
               fTalkTime = this.Say(m_sReactBallTakenWFall[var4]);
            }

            this.CallMethodIn("StopTalking", fTalkTime);
         }
      }
   }

   void CommentMatchEnd() {
      if (this.IsActive()) {
         this.StopTalking();
         this.ClearCurrComment();
         MatchData cData = Match.m_cMatchData;
         String[][] sScenarios = null;
         String sTeam = null;
         int nScenario = -1;
         if (!CoreLogic.m_bMultiplayer && !cData.IsDraw()) {
            int nPlayerAnimal = SoccerModule.m_cMatch.GetTeam(SoccerModule.m_cMatch.m_nPlayer1Team).m_nAnimalType;
            if (cData.IsWinner(nPlayerAnimal)) {
               sScenarios = m_sMatchEndWin;
               sTeam = EAnimals.GetTeamNameWav(nPlayerAnimal);
               if (CoreLogic.m_bChampionships) {
                  nScenario = this.Toss(0, sScenarios.length - 1);
               } else {
                  nScenario = this.Toss(2, sScenarios.length - 1);
               }
            } else {
               sScenarios = m_sMatchEndLoose;
               sTeam = EAnimals.GetTeamNameWav(nPlayerAnimal);
            }
         } else if (cData.m_nGoalsGuest + cData.m_nGoalsHost > 0) {
            sScenarios = m_sMatchEndNeutralGood;
         } else {
            sScenarios = m_sMatchEndNeutralBad;
         }

         if (nScenario == -1) {
            nScenario = this.Toss(0, sScenarios.length - 1);
         }

         this.BuildTalkFromScenario(nScenario, sScenarios, sTeam, null);
         this.SayNextPart();
      }
   }

   String GetWavFromScoreDe(int nScore) {
      String sScore = null;
      switch (nScore) {
         case 0:
            sScore = "data/sounds/commentary/" + "188.wav";
            break;
         case 1:
            sScore = "data/sounds/commentary/" + "189.wav";
            break;
         case 2:
            sScore = "data/sounds/commentary/" + "190.wav";
            break;
         case 3:
            sScore = "data/sounds/commentary/" + "191.wav";
            break;
         case 4:
            sScore = "data/sounds/commentary/" + "192.wav";
            break;
         case 5:
            sScore = "data/sounds/commentary/" + "193.wav";
            break;
         case 6:
            sScore = "data/sounds/commentary/" + "194.wav";
            break;
         case 7:
            sScore = "data/sounds/commentary/" + "195.wav";
            break;
         case 8:
            sScore = "data/sounds/commentary/" + "196.wav";
            break;
         case 9:
            sScore = "data/sounds/commentary/" + "197.wav";
            break;
         case 10:
            sScore = "data/sounds/commentary/" + "198.wav";
            break;
         case 11:
            sScore = "data/sounds/commentary/" + "199.wav";
            break;
         case 12:
            sScore = "data/sounds/commentary/" + "200.wav";
            break;
         case 13:
            sScore = "data/sounds/commentary/" + "201.wav";
            break;
         case 14:
            sScore = "data/sounds/commentary/" + "202.wav";
            break;
         case 15:
            sScore = "data/sounds/commentary/" + "203.wav";
            break;
         case 16:
            sScore = "data/sounds/commentary/" + "204.wav";
            break;
         case 17:
            sScore = "data/sounds/commentary/" + "205.wav";
            break;
         case 18:
            sScore = "data/sounds/commentary/" + "337.wav";
            break;
         case 19:
            sScore = "data/sounds/commentary/" + "338.wav";
            break;
         case 20:
            sScore = "data/sounds/commentary/" + "206.wav";
            break;
         case 21:
            sScore = "data/sounds/commentary/" + "207.wav";
            break;
         case 22:
            sScore = "data/sounds/commentary/" + "208.wav";
            break;
         case 23:
            sScore = "data/sounds/commentary/" + "209.wav";
            break;
         case 24:
            sScore = "data/sounds/commentary/" + "210.wav";
            break;
         case 25:
            sScore = "data/sounds/commentary/" + "211.wav";
            break;
         case 26:
            sScore = "data/sounds/commentary/" + "212.wav";
            break;
         case 27:
            sScore = "data/sounds/commentary/" + "213.wav";
            break;
         case 28:
            sScore = "data/sounds/commentary/" + "214.wav";
            break;
         case 29:
            sScore = "data/sounds/commentary/" + "215.wav";
            break;
         case 30:
            sScore = "data/sounds/commentary/" + "216.wav";
      }

      return sScore;
   }

   void StopTalking() {
      ModNet.OnSayStop(); // [mod]
      this.m_vToSay.removeAllElements();
      this.DisableCallMethod("StopTalking");
      this.DisableCallMethod("SayNextPart");
      this.StopCurrentWave();
      this.StopAnim("");
      this.Hide();
   }

   boolean IsTalking() {
      return this.m_nCurrentTalkId == -1 ? false : this.IsPlayingWave(this.m_nCurrentTalkId);
   }

   void CommentFirstPass(AIFootBaller cFBaller) {
      if (!this.IsTalking() || this.CanBreakComment(9, false)) {
         if (this.IsActive()) {
            this.ClearCurrComment();
            float fTalkTime = this.Say(this.MakeWav(EAnimals.GetFBNameWav(cFBaller.m_nAnimalType, cFBaller.m_nNumberInTeam)));
            this.CallMethodIn("StopTalking", fTalkTime);
         }
      }
   }

   void CommentLobPass(AIFootBaller cFBaller, AIFootBaller cFDest) {
      if (!this.IsTalking() || this.CanBreakComment(9, false)) {
         if (this.IsActive()) {
            if (GameObject.RandomFloat(1.0F) > 0.2F) {
               this.CommentPass(cFBaller, cFDest);
            } else {
               this.ClearCurrComment();
               this.m_nCurrComment = 9;
               AIFootBaller cObstacle = cFBaller.m_cStrategy.GetObstacleFB(cFBaller, cFBaller.GetPosition(), cFDest.GetPosition(), cFDest);
               boolean bCanSayLobOver = true;
               if (cObstacle != null && bCanSayLobOver) {
                  int nScenarioNo = this.Toss(0, m_sPassLob.length - 1);
                  if (nScenarioNo == 0) {
                     this.BuildTalkFromScenario(
                        nScenarioNo,
                        m_sPassLob,
                        EAnimals.GetFBNameWav(cFBaller.m_nAnimalType, cFBaller.m_nNumberInTeam),
                        EAnimals.GetFBNameWav(cObstacle.m_nAnimalType, cObstacle.m_nNumberInTeam)
                     );
                  }

                  if (nScenarioNo == 1) {
                     this.BuildTalkFromScenario(
                        nScenarioNo,
                        m_sPassLob,
                        EAnimals.GetFBNameWav(cFDest.m_nAnimalType, cFDest.m_nNumberInTeam),
                        EAnimals.GetFBNameWav(cObstacle.m_nAnimalType, cObstacle.m_nNumberInTeam)
                     );
                  }
               } else {
                  this.BuildTalkFromScenario(2, m_sPassLob, null, EAnimals.GetFBNameWav(cFDest.m_nAnimalType, cFDest.m_nNumberInTeam));
               }

               this.SayNextPart();
            }
         }
      }
   }

   void CommentCorner() {
      if (!this.IsTalking() || this.CanBreakComment(19, false)) {
         if (this.IsActive()) {
            this.ClearCurrComment();
            float fTalkTime = this.Say("data/sounds/commentary/" + "corner.wav");
            this.CallMethodIn("StopTalking", fTalkTime);
         }
      }
   }

   void CommentDefence(AIFootBaller cGK) {
      if (!this.IsTalking() || this.CanBreakComment(6, false)) {
         if (this.IsActive()) {
            this.ClearCurrComment();
            this.m_nCurrComment = 6;
            int nScenarioNo = -1;
            if (cGK.m_bJumping && this.TossHalf(50.0F)) {
               nScenarioNo = 3;
            }

            int nScope;
            if (cGK.m_bIsFalling) {
               nScope = 2;
            } else {
               nScope = 1;
            }

            if (nScenarioNo == -1) {
               nScenarioNo = this.Toss(0, nScope);
            }

            float fTalkTime = this.Say(m_sDefence[nScenarioNo][0]);
            this.CallMethodIn("StopTalking", fTalkTime);
         }
      }
   }

   void CommentSuperAction(boolean bWithFBallers) {
      if (!this.IsTalking() || this.CanBreakComment(7, false)) {
         if (this.IsActive()) {
            this.ClearCurrComment();
            this.m_nCurrComment = 7;
            int nScenarioNo;
            if (!bWithFBallers) {
               nScenarioNo = this.Toss(1, m_sSuperAction.length - 1);
            } else {
               nScenarioNo = this.Toss(0, m_sSuperAction.length - 1);
            }

            float fTalkTime = this.Say(m_sSuperAction[nScenarioNo]);
            this.CallMethodIn("StopTalking", fTalkTime);
         }
      }
   }

   void CommentWeather(String sComment) {
      if (!this.IsTalking() || this.CanBreakComment(14, false)) {
         if (this.IsActive()) {
            this.ClearCurrComment();
            float fTalkTime = this.Say(sComment);
            this.CallMethodIn("StopTalking", fTalkTime);
         }
      }
   }

   void SayForMenuOptionsAV(String sWavFile, float fVolume) {
      int nTalk = this.GetActiveModule().PlayWave(sWavFile);
      this.SetWaveVolume(nTalk, fVolume);
   }

   void Show() {
      this.DisableCallMethod("Hide");
      this.DisableCallMethod("HideNow");
      if (this.IsActive()) {
         if (!this.m_bVisible) {
            this.m_bVisible = true;
            CommentatorModule cMod = (CommentatorModule)this.GetModule();
            int nChoose = SoccerGame.GetRandom(0, 1);
            int nWhere;
            if (nChoose == 0) {
               nWhere = 1;
            } else {
               nWhere = 3;
            }

            if (SoccerGame.activeLevelModule != null) {
               this.SetPosition(new Vector(0.0F, -30.0F, 500.0F));
            } else {
               this.SetPosition(new Vector(0.0F, -20.0F, 750.0F));
            }

            this.RotateLocal(Vector.Forward, 0.0F);
            cMod.m_nSlideIn = nWhere;
            this.Show();
            float fRandomPosH = 0.0F;
            float fRandomPosV = 0.0F;
            switch (nWhere) {
               case 0:
               case 3:
                  this.RotateLocal(this.VectorWorldToLocal(Vector.Forward), -this.m_fRotationF);
                  this.RotateLocal(this.VectorWorldToLocal(Vector.Up), -this.m_fRotationUp);
                  this.m_fRotationF = SoccerGame.GetRandom(-45, -20);
                  this.m_fRotationUp = SoccerGame.GetRandom(0, 45);
                  this.RotateLocal(this.VectorWorldToLocal(Vector.Up), this.m_fRotationUp);
                  this.RotateLocal(this.VectorWorldToLocal(Vector.Forward), this.m_fRotationF);
                  fRandomPosH = SoccerGame.GetRandom(-0.45F, -0.4F);
                  fRandomPosV = SoccerGame.GetRandom(-0.5F, -0.45F);
                  cMod.SetViewOrigin(cMod.m_nView, -1.0F, -1.0F);
                  cMod.SetDestViewOrigin(fRandomPosH, fRandomPosV);
                  break;
               case 1:
               case 2:
                  this.RotateLocal(this.VectorWorldToLocal(Vector.Forward), -this.m_fRotationF);
                  this.RotateLocal(this.VectorWorldToLocal(Vector.Up), -this.m_fRotationUp);
                  this.m_fRotationF = SoccerGame.GetRandom(10, 45);
                  this.m_fRotationUp = SoccerGame.GetRandom(-45, 45);
                  this.RotateLocal(this.VectorWorldToLocal(Vector.Up), this.m_fRotationUp);
                  this.RotateLocal(this.VectorWorldToLocal(Vector.Forward), this.m_fRotationF);
                  fRandomPosH = SoccerGame.GetRandom(0.45F, 0.5F);
                  fRandomPosV = SoccerGame.GetRandom(-0.5F, -0.45F);
                  cMod.SetViewOrigin(cMod.m_nView, 1.0F, -1.0F);
                  cMod.SetDestViewOrigin(fRandomPosH, fRandomPosV);
            }
         }
      }
   }

   public void OnCreate() {
      GameObject.CrashLog("OnCreate " + this);
      super.OnCreate();
      this.SetPosition(new Vector());
      this.RegisterKeyRequest();
      this.ClearFlag(4);
      this.ClearFlag(2);
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

   void CommentFun() {
      if (!this.IsTalking() || this.CanBreakComment(17, false)) {
         if (this.IsActive()) {
            this.ClearCurrComment();
            int nScenarioNo = 0;
            if (SoccerModule.m_bRenderPublic) {
               nScenarioNo = this.Toss(0, m_sFunGeneral.length - 1);
            } else {
               nScenarioNo = this.Toss(2, m_sFunGeneral.length - 1);
            }

            float fTalkTime = this.Say(m_sFunGeneral[nScenarioNo]);
            this.CallMethodIn("StopTalking", fTalkTime);
         }
      }
   }

   boolean CanBreakComment(int nNewComment, boolean bCritical) {
      if (bCritical) {
         return true;
      }

      if (nNewComment != 11 && nNewComment != 13 || this.m_nCurrComment != 12 && this.m_nCurrComment != 9) {
         if (nNewComment != 3 || this.m_nCurrComment != 9 && this.m_nCurrComment != 12) {
            return (nNewComment == 6 || nNewComment == 5 || nNewComment == 20) && this.m_nCurrComment == 3
               ? true
               : nNewComment == 20 && this.m_nCurrComment == 9;
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   void CommentGoal(AIFootBaller cGoalKeeper) {
      if (!this.IsTalking() || this.CanBreakComment(4, true)) {
         if (this.IsActive()) {
            this.ClearCurrComment();
            this.m_nCurrComment = 4;
            int nScenarioNo = this.Toss(1, m_sGoal.length - 1);
            float fTalkTime = this.Say(m_sGoal[nScenarioNo][0]);
            this.CallMethodIn("StopTalking", fTalkTime);
            this.CallMethodIn("CommentGoalAddition", fTalkTime + 1.0F);
         }
      }
   }

   Module GetActiveModule() {
      Module cMod = null;
      if (SoccerGame.game.activeMenu == 1) {
         cMod = SoccerGame.firstMenuModule;
      } else if (SoccerGame.game.activeMenu == 2) {
         cMod = SoccerGame.activeLevelModule;
      }

      if (cMod == null) {
         cMod = this.GetModule();
      }

      return cMod;
   }

   public void CommentGoalAddition() {
      int nFirstScore = SoccerModule.m_cMatch.m_nScoreLeft;
      int nSecScore = SoccerModule.m_cMatch.m_nScoreRight;
      if (nFirstScore <= 30 && nSecScore <= 30) {
         this.BuildTalkFromScenario(0, m_sGoal, this.GetWavFromScore(nFirstScore), this.GetWavFromScore(nSecScore));
         this.SayNextPart();
      }
   }
}
