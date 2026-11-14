import java.io.IOException;

public class Championships extends GameObject {
   static final int[] m_nFinalOpponents = new int[]{0, 1, 3, 6, 5, 7, -1};
   static final int[] m_nAvailableTeams = new int[]{2, 4, 10, 8, 9, 11};
   static final int _CUP_1_UPC = 0;
   static final int _CUP_2_PSFC = 1;
   static final int _CUP_3_OPST = 2;
   static final int _CUP_4_APSSC = 3;
   static final int _CUP_5_PCSC = 4;
   static final int _CUP_6_OCCC = 5;
   static final int _CUP_7_ASPT = 6;
   static final String[] m_sCupNamesWav = new String[]{
      "data/sounds/commentary/" + "105.wav",
      "data/sounds/commentary/" + "106.wav",
      "data/sounds/commentary/" + "107.wav",
      "data/sounds/commentary/" + "108.wav",
      "data/sounds/commentary/" + "109.wav",
      "data/sounds/commentary/" + "110.wav",
      "data/sounds/commentary/" + "111.wav"
   };
   static final String[] m_sCupNames = new String[]{"&CupName1&", "&CupName2&", "&CupName3&", "&CupName4&", "&CupName5&", "&CupName6&", "&CupName7&"};
   static final int _POINTS_WIN = 3;
   static final int _POINTS_DRAW = 1;
   static final int _PHASE_QUALIFICATIONS = 0;
   static final int _PHASE_ELIMINATIONS = 1;
   static final int _PHASE_SEMIFINAL = 2;
   static final int _PHASE_FINAL = 3;
   static final int _RESULT_FAILURE = 0;
   static final int _RESULT_NEXT_PHASE_ELIM = 1;
   static final int _RESULT_NEXT_PHASE_SEMI = 2;
   static final int _RESULT_NEXT_PHASE_FINAL = 3;
   static final int _RESULT_NEXT_PHASE_VICTORY = 4;
   static final int _RESULT_REPLAY_MATCH = 5;
   String[] sResultStrings = new String[]{
      "_RESULT_FAILURE", "_RESULT_NEXT_PHASE_ELIM", "_RESULT_NEXT_PHASE_SEMI", "_RESULT_NEXT_PHASE_FINAL", "_RESULT_NEXT_PHASE_VICTORY", "_RESULT_REPLAY_MATCH"
   };
   int m_nPhase = 0;
   int m_nPlayerTeam = -1;
   int m_nPlayerTeamClothes = -1;
   Championships.TeamRecord m_cPlayerTeam = null;
   static final int _GROUP_A = 0;
   static final int _GROUP_B = 1;
   static final int _GROUP_C = 2;
   static final int _GROUP_D = 3;
   int m_nPlayerGroup = -1;
   java.util.Vector m_vGroupA = new java.util.Vector();
   java.util.Vector m_vGroupB = new java.util.Vector();
   java.util.Vector m_vGroupC = new java.util.Vector();
   java.util.Vector m_vGroupD = new java.util.Vector();
   java.util.Vector m_vGroupAMatches = new java.util.Vector();
   java.util.Vector m_vGroupBMatches = new java.util.Vector();
   java.util.Vector m_vGroupCMatches = new java.util.Vector();
   java.util.Vector m_vGroupDMatches = new java.util.Vector();
   MatchData m_cElimA1B2 = null;
   MatchData m_cElimA2B1 = null;
   MatchData m_cElimC1D2 = null;
   MatchData m_cElimC2D1 = null;
   MatchData m_cSemi1 = null;
   MatchData m_cSemi2 = null;
   MatchData m_cFinal = null;
   MatchData m_cNextMatch = null;
   java.util.Vector m_vAllTeams = new java.util.Vector();
   int m_nFinalOpponent = -1;
   int m_nCurGroupMatch = 0;
   final int m_nMaxGroupMatch = 5;
   int m_nCurChampionships = 0;
   boolean m_bStartNextChampionships = true;
   boolean m_bIsReplayingMatch = false;
   int m_nPrevChampWinner = -1;

   void NextPhase() {
      this.m_nPhase++;
      this.UpdateData(this.m_nPhase);
   }

   boolean IsGroupOK(int nGroup) {
      if (this.m_nCurGroupMatch >= 5 && this.AreScoresEqual(nGroup)) {
         return false;
      }

      if (this.Group(this.FindGroup(this.FindTeamRecord(this.m_nFinalOpponent))) == this.Group(nGroup)) {
      }

      return true;
   }

   int ReportMatchEnd(MatchData cMatchData) {
      switch (this.m_nPhase) {
         case 0:
            this.ApplyMatchResult(cMatchData);
            this.UpdateOtherGroupsScores();
            this.m_nCurGroupMatch++;
            if (this.m_nCurGroupMatch > 5) {
               if (this.Group(this.m_nPlayerGroup).indexOf(this.m_cPlayerTeam) < 2) {
                  this.NextPhase();
                  return 1;
               }

               this.UpdateData(this.m_nPhase + 1);
               return 0;
            }
         default:
            return -1;
         case 1:
            if (!this.m_bIsReplayingMatch) {
               this.RandomizeEliminationsResults();
            }

            if (cMatchData.IsDraw()) {
               this.m_bIsReplayingMatch = true;
               this.UpdateData(this.m_nPhase + 1);
               return 5;
            } else {
               this.m_bIsReplayingMatch = false;
               if (cMatchData.m_nTeamHost == this.m_nPlayerTeam && cMatchData.m_nGoalsHost > cMatchData.m_nGoalsGuest) {
                  this.NextPhase();
                  return 2;
               } else {
                  if (cMatchData.m_nTeamGuest == this.m_nPlayerTeam && cMatchData.m_nGoalsGuest > cMatchData.m_nGoalsHost) {
                     this.NextPhase();
                     return 2;
                  }

                  this.UpdateData(this.m_nPhase + 1);
                  return 0;
               }
            }
         case 2:
            if (!this.m_bIsReplayingMatch) {
               this.RandomizeElimMatch(this.m_cSemi2);
            }

            if (cMatchData.IsDraw()) {
               this.m_bIsReplayingMatch = true;
               this.UpdateData(this.m_nPhase + 1);
               return 5;
            } else {
               this.m_bIsReplayingMatch = false;
               if (cMatchData.IsWinner(this.m_nPlayerTeam)) {
                  this.NextPhase();
                  return 3;
               }

               this.UpdateData(this.m_nPhase + 1);
               return 0;
            }
         case 3:
            if (cMatchData.IsWinner(this.m_nPlayerTeam)) {
               this.m_bIsReplayingMatch = false;
               return 4;
            } else if (cMatchData.IsDraw()) {
               this.m_bIsReplayingMatch = true;
               return 5;
            } else {
               this.m_bIsReplayingMatch = false;
               return 0;
            }
      }
   }

   boolean Load(Stream s) {
      try {
         if (!s.ReadMarker(7000)) {
            return false;
         }

         this.m_nPhase = s.in.readInt();
         this.m_nPlayerTeam = s.in.readInt();
         this.m_nPlayerTeamClothes = s.in.readInt();
         this.m_nPlayerGroup = s.in.readInt();
         this.m_nFinalOpponent = s.in.readInt();
         this.m_nCurGroupMatch = s.in.readInt();
         this.m_nCurChampionships = s.in.readInt();
         this.m_nPrevChampWinner = s.in.readInt();
         this.m_bStartNextChampionships = s.in.readBoolean();
         this.m_bIsReplayingMatch = s.in.readBoolean();
         this.m_vGroupA = this.LoadGroup(s);
         this.m_vGroupB = this.LoadGroup(s);
         this.m_vGroupC = this.LoadGroup(s);
         this.m_vGroupD = this.LoadGroup(s);
         this.m_vGroupAMatches = this.LoadGroupMatches(s);
         this.m_vGroupBMatches = this.LoadGroupMatches(s);
         this.m_vGroupCMatches = this.LoadGroupMatches(s);
         this.m_vGroupDMatches = this.LoadGroupMatches(s);
         if (s.in.readBoolean()) {
            this.m_cElimA1B2 = new MatchData();
            this.m_cElimA1B2.Load(s);
         } else {
            this.m_cElimA1B2 = null;
         }

         if (s.in.readBoolean()) {
            this.m_cElimA2B1 = new MatchData();
            this.m_cElimA2B1.Load(s);
         } else {
            this.m_cElimA2B1 = null;
         }

         if (s.in.readBoolean()) {
            this.m_cElimC1D2 = new MatchData();
            this.m_cElimC1D2.Load(s);
         } else {
            this.m_cElimC1D2 = null;
         }

         if (s.in.readBoolean()) {
            this.m_cElimC2D1 = new MatchData();
            this.m_cElimC2D1.Load(s);
         } else {
            this.m_cElimC2D1 = null;
         }

         if (s.in.readBoolean()) {
            this.m_cSemi1 = new MatchData();
            this.m_cSemi1.Load(s);
         } else {
            this.m_cSemi1 = null;
         }

         if (s.in.readBoolean()) {
            this.m_cSemi2 = new MatchData();
            this.m_cSemi2.Load(s);
         } else {
            this.m_cSemi2 = null;
         }

         if (s.in.readBoolean()) {
            this.m_cFinal = new MatchData();
            this.m_cFinal.Load(s);
         } else {
            this.m_cFinal = null;
         }

         if (s.in.readBoolean()) {
            MatchData cNextMatch = new MatchData();
            cNextMatch.Load(s);
            this.m_cNextMatch = this.FindNextMatchReference(cNextMatch);
         } else {
            this.m_cNextMatch = null;
         }

         return s.ReadMarker(7100);
      } catch (IOException e) {
         GameObject.LogLine("Championships::Load() - IOException");
         return false;
      }
   }

   java.util.Vector GroupMatches(int nGroup) {
      switch (nGroup) {
         case 0:
            return this.m_vGroupAMatches;
         case 1:
            return this.m_vGroupBMatches;
         case 2:
            return this.m_vGroupCMatches;
         case 3:
            return this.m_vGroupDMatches;
         default:
            return null;
      }
   }

   java.util.Vector DrawMatches(java.util.Vector vTeams) {
      MatchData match = null;
      MatchData rematch = null;
      java.util.Vector vMatches = new java.util.Vector();

      for (int i = 0; i < vTeams.size(); i++) {
         match = new MatchData(
            ((Championships.TeamRecord)vTeams.elementAt(i)).m_nTeam, ((Championships.TeamRecord)vTeams.elementAt((i + 1) % vTeams.size())).m_nTeam
         );
         int nIndex = SoccerGame.GetRandom(0, vMatches.size());
         if (nIndex == vMatches.size()) {
            vMatches.addElement(match);
         } else {
            vMatches.insertElementAt(match, nIndex);
         }
      }

      for (int i = 0; i < vTeams.size(); i++) {
         match = (MatchData)vMatches.elementAt(i);
         rematch = new MatchData(match.m_nTeamGuest, match.m_nTeamHost);
         vMatches.addElement(rematch);
      }

      return vMatches;
   }

   java.util.Vector LoadGroup(Stream s) {
      java.util.Vector vGroup = new java.util.Vector();
      Championships.TeamRecord cRecord = null;

      try {
         int nGroupCount = s.in.readInt();

         for (int i = 0; i < nGroupCount; i++) {
            cRecord = new Championships.TeamRecord(this);
            cRecord.Load(s);
            vGroup.addElement(cRecord);
            this.m_vAllTeams.addElement(cRecord);
            if (cRecord.m_nTeam == this.m_nPlayerTeam) {
               this.m_cPlayerTeam = cRecord;
            }
         }

         return vGroup;
      } catch (IOException e) {
         GameObject.LogLine("Championships::LoadGroup() - IOException");
         return null;
      }
   }

   MatchData GetGroupPrevMatch(int nGroup) {
      return this.m_nCurGroupMatch > 0 ? (MatchData)this.GroupMatches(nGroup).elementAt(this.m_nCurGroupMatch - 1) : null;
   }

   void UpdateData(int nPhase) {
      int nTeam1 = -1;
      int nTeam2 = -1;
      switch (nPhase) {
         case 1:
            nTeam1 = ((Championships.TeamRecord)this.Group(0).elementAt(0)).m_nTeam;
            nTeam2 = ((Championships.TeamRecord)this.Group(1).elementAt(1)).m_nTeam;
            this.m_cElimA1B2 = new MatchData(nTeam1, nTeam2);
            this.m_cElimA1B2.RandomizeSides();
            if (nTeam1 == this.m_nPlayerTeam || nTeam2 == this.m_nPlayerTeam) {
               this.m_cNextMatch = this.m_cElimA1B2;
            }

            nTeam1 = ((Championships.TeamRecord)this.Group(0).elementAt(1)).m_nTeam;
            nTeam2 = ((Championships.TeamRecord)this.Group(1).elementAt(0)).m_nTeam;
            this.m_cElimA2B1 = new MatchData(nTeam1, nTeam2);
            this.m_cElimA2B1.RandomizeSides();
            if (nTeam1 == this.m_nPlayerTeam || nTeam2 == this.m_nPlayerTeam) {
               this.m_cNextMatch = this.m_cElimA2B1;
            }

            nTeam1 = ((Championships.TeamRecord)this.Group(2).elementAt(0)).m_nTeam;
            nTeam2 = ((Championships.TeamRecord)this.Group(3).elementAt(1)).m_nTeam;
            this.m_cElimC1D2 = new MatchData(nTeam1, nTeam2);
            this.m_cElimC1D2.RandomizeSides();
            if (nTeam1 == this.m_nPlayerTeam || nTeam2 == this.m_nPlayerTeam) {
               this.m_cNextMatch = this.m_cElimC1D2;
            }

            nTeam1 = ((Championships.TeamRecord)this.Group(2).elementAt(1)).m_nTeam;
            nTeam2 = ((Championships.TeamRecord)this.Group(3).elementAt(0)).m_nTeam;
            this.m_cElimC2D1 = new MatchData(nTeam1, nTeam2);
            this.m_cElimC2D1.RandomizeSides();
            if (nTeam1 == this.m_nPlayerTeam || nTeam2 == this.m_nPlayerTeam) {
               this.m_cNextMatch = this.m_cElimC2D1;
            }
            break;
         case 2:
            this.m_cSemi1 = new MatchData(this.m_cElimA1B2.GetWinner(), this.m_cElimA2B1.GetWinner());
            if (!this.m_cElimA1B2.IsDraw() && !this.m_cElimA2B1.IsDraw()) {
               this.m_cSemi1.RandomizeSides();
               this.m_cNextMatch = this.m_cSemi1;
            }

            this.m_cSemi2 = new MatchData(this.m_cElimC1D2.GetWinner(), this.m_cElimC2D1.GetWinner());
            this.m_cSemi2.RandomizeSides();
            break;
         case 3:
            this.m_cFinal = new MatchData(this.m_cSemi1.GetWinner(), this.m_cSemi2.GetWinner());
            if (!this.m_cSemi1.IsDraw()) {
               if (this.m_cFinal.m_nTeamGuest == this.m_nFinalOpponent) {
                  this.m_cFinal.SwitchSides();
               }

               this.m_cNextMatch = this.m_cFinal;
            }
      }
   }

   void NextChampionship() {
      CoreLogic.GetPlayer().NextDiffLevel();
      if (this.m_nFinalOpponent != -1) {
         CoreLogic.GetPlayer().SetTeamAvailable(this.m_nFinalOpponent);
      }

      if (this.m_nCurChampionships < 6) {
         CoreLogic.GetPlayer().SetExtraAvailable(15 + this.m_nCurChampionships);
      }

      this.m_bStartNextChampionships = true;
      this.m_nCurChampionships++;
      if (this.m_nCurChampionships > 6) {
         this.m_nCurChampionships = 0;
      } else {
         if (this.m_nCurChampionships == 6) {
            this.m_nPrevChampWinner = this.m_nPlayerTeam;
         }

         if (this.m_nPrevChampWinner != -1) {
            this.m_nPrevChampWinner = this.m_nPlayerTeam;
         }
      }
   }

   void RestartChampionship() {
      this.m_bStartNextChampionships = true;
   }

   void ApplyMatchResult(MatchData cMatchData) {
      if (cMatchData.m_nGoalsHost == cMatchData.m_nGoalsGuest) {
         this.FindTeamRecord(cMatchData.m_nTeamHost).m_nPoints++;
         this.FindTeamRecord(cMatchData.m_nTeamGuest).m_nPoints++;
      }

      if (cMatchData.m_nGoalsHost > cMatchData.m_nGoalsGuest) {
         this.FindTeamRecord(cMatchData.m_nTeamHost).m_nPoints += 3;
      }

      if (cMatchData.m_nGoalsHost < cMatchData.m_nGoalsGuest) {
         this.FindTeamRecord(cMatchData.m_nTeamGuest).m_nPoints += 3;
      }
   }

   void RevokeMatchResult(MatchData cMatchData) {
      if (cMatchData.m_nGoalsHost == cMatchData.m_nGoalsGuest) {
         this.FindTeamRecord(cMatchData.m_nTeamHost).m_nPoints--;
         this.FindTeamRecord(cMatchData.m_nTeamGuest).m_nPoints--;
      }

      if (cMatchData.m_nGoalsHost > cMatchData.m_nGoalsGuest) {
         this.FindTeamRecord(cMatchData.m_nTeamHost).m_nPoints -= 3;
      }

      if (cMatchData.m_nGoalsHost < cMatchData.m_nGoalsGuest) {
         this.FindTeamRecord(cMatchData.m_nTeamGuest).m_nPoints -= 3;
      }
   }

   void UpdateOtherGroupsScores() {
      for (int i = 0; i < 4; i++) {
         if (this.m_nPlayerGroup == i) {
            this.SortGroup(i);
         } else {
            int nMaxScore = CoreLogic._HALF_TIMES[CoreLogic.GetPlayer().m_nMatchHalfTimeIndex];
            MatchData match = (MatchData)this.GroupMatches(i).elementAt(this.m_nCurGroupMatch);

            while (true) {
               match.RandomizeResult(nMaxScore);
               if (match.m_nTeamHost == this.m_nFinalOpponent) {
                  if (match.m_nGoalsHost < match.m_nGoalsGuest) {
                     match.m_nGoalsHost = match.m_nGoalsGuest;
                  } else if (match.m_nGoalsHost == match.m_nGoalsGuest) {
                     match.m_nGoalsHost++;
                  }
               }

               if (match.m_nTeamGuest == this.m_nFinalOpponent) {
                  if (match.m_nGoalsGuest < match.m_nGoalsHost) {
                     match.m_nGoalsGuest = match.m_nGoalsHost;
                  } else if (match.m_nGoalsGuest == match.m_nGoalsHost) {
                     match.m_nGoalsGuest++;
                  }
               }

               this.ApplyMatchResult(match);
               this.SortGroup(i);
               if (this.IsGroupOK(i)) {
                  break;
               }

               this.RevokeMatchResult(match);
               if (SoccerGame.m_bDebug) {
                  GameObject.CrashLog("Grupa nie OK ---------------------------------------");
               }
            }
         }
      }
   }

   static String GetCupName(int nCup) {
      if (nCup < 0) {
         nCup = 0;
      }

      return Text.Get(m_sCupNames[nCup]);
   }

   void RandomizeEliminationsResults() {
      this.RandomizeElimMatch(this.m_cElimA1B2);
      this.RandomizeElimMatch(this.m_cElimA2B1);
      this.RandomizeElimMatch(this.m_cElimC1D2);
      this.RandomizeElimMatch(this.m_cElimC2D1);
   }

   void SortGroup(int nGroup) {
      Championships.TeamRecord team = null;
      Championships.TeamRecord teamMax = null;
      int nSize = this.Group(nGroup).size();
      int nMax = -1;

      for (int i = 0; i < nSize; i++) {
         nMax = -1;

         for (int j = i; j < nSize; j++) {
            team = (Championships.TeamRecord)this.Group(nGroup).elementAt(j);
            int nScore = team.m_nPoints;
            if (nScore > nMax || nScore == nMax && team.m_nTeam == this.m_nPlayerTeam) {
               teamMax = team;
               nMax = nScore;
            }
         }

         this.Group(nGroup).removeElement(teamMax);
         this.Group(nGroup).insertElementAt(teamMax, i);
      }
   }

   int FindGroup(Championships.TeamRecord cTeam) {
      for (int i = 0; i < 4; i++) {
         if (this.Group(i).indexOf(cTeam) != -1) {
            return i;
         }
      }

      return -1;
   }

   static String GetCupNameWav(int nCup) {
      return m_sCupNamesWav[nCup];
   }

   String GetResultString(int i) {
      return i == -1 ? "OK" : this.sResultStrings[i];
   }

   void WinChampionships() {
      MatchData match;
      do {
         match = null;
         match = this.GetNextMatch();
         if (match.m_nTeamGuest == this.m_nPlayerTeam) {
            match.m_nGoalsGuest = 1;
         } else if (match.m_nTeamHost == this.m_nPlayerTeam) {
            match.m_nGoalsHost = 1;
         } else {
            match.m_nGoalsHost = match.m_nGoalsGuest = 0;
         }
      } while (this.ReportMatchEnd(match) != 4);
   }

   void Save(Stream s) {
      try {
         s.WriteMarker(7000);
         s.out.writeInt(this.m_nPhase);
         s.out.writeInt(this.m_nPlayerTeam);
         s.out.writeInt(this.m_nPlayerTeamClothes);
         s.out.writeInt(this.m_nPlayerGroup);
         s.out.writeInt(this.m_nFinalOpponent);
         s.out.writeInt(this.m_nCurGroupMatch);
         s.out.writeInt(this.m_nCurChampionships);
         s.out.writeInt(this.m_nPrevChampWinner);
         s.out.writeBoolean(this.m_bStartNextChampionships);
         s.out.writeBoolean(this.m_bIsReplayingMatch);
         this.SaveGroup(s, this.m_vGroupA);
         this.SaveGroup(s, this.m_vGroupB);
         this.SaveGroup(s, this.m_vGroupC);
         this.SaveGroup(s, this.m_vGroupD);
         this.SaveGroupMatches(s, this.m_vGroupAMatches);
         this.SaveGroupMatches(s, this.m_vGroupBMatches);
         this.SaveGroupMatches(s, this.m_vGroupCMatches);
         this.SaveGroupMatches(s, this.m_vGroupDMatches);
         if (this.m_cElimA1B2 != null) {
            s.out.writeBoolean(true);
            this.m_cElimA1B2.Save(s);
         } else {
            s.out.writeBoolean(false);
         }

         if (this.m_cElimA2B1 != null) {
            s.out.writeBoolean(true);
            this.m_cElimA2B1.Save(s);
         } else {
            s.out.writeBoolean(false);
         }

         if (this.m_cElimC1D2 != null) {
            s.out.writeBoolean(true);
            this.m_cElimC1D2.Save(s);
         } else {
            s.out.writeBoolean(false);
         }

         if (this.m_cElimC2D1 != null) {
            s.out.writeBoolean(true);
            this.m_cElimC2D1.Save(s);
         } else {
            s.out.writeBoolean(false);
         }

         if (this.m_cSemi1 != null) {
            s.out.writeBoolean(true);
            this.m_cSemi1.Save(s);
         } else {
            s.out.writeBoolean(false);
         }

         if (this.m_cSemi2 != null) {
            s.out.writeBoolean(true);
            this.m_cSemi2.Save(s);
         } else {
            s.out.writeBoolean(false);
         }

         if (this.m_cFinal != null) {
            s.out.writeBoolean(true);
            this.m_cFinal.Save(s);
         } else {
            s.out.writeBoolean(false);
         }

         if (this.m_cNextMatch != null) {
            s.out.writeBoolean(true);
            this.m_cNextMatch.Save(s);
         } else {
            s.out.writeBoolean(false);
         }

         s.WriteMarker(7100);
         s.out.flush();
      } catch (IOException e) {
         GameObject.LogLine("Championships::Save() - IOException");
      }
   }

   Championships.TeamRecord GetTeamRecord(int nGroup, int nTeamPos) {
      switch (nGroup) {
         case 0:
            return (Championships.TeamRecord)this.m_vGroupA.elementAt(nTeamPos);
         case 1:
            return (Championships.TeamRecord)this.m_vGroupB.elementAt(nTeamPos);
         case 2:
            return (Championships.TeamRecord)this.m_vGroupC.elementAt(nTeamPos);
         case 3:
            return (Championships.TeamRecord)this.m_vGroupD.elementAt(nTeamPos);
         default:
            return null;
      }
   }

   java.util.Vector LoadGroupMatches(Stream s) {
      java.util.Vector vMatches = new java.util.Vector();
      MatchData cData = null;

      try {
         int nMatchCount = s.in.readInt();

         for (int i = 0; i < nMatchCount; i++) {
            cData = new MatchData();
            cData.Load(s);
            vMatches.addElement(cData);
         }

         return vMatches;
      } catch (IOException e) {
         GameObject.LogLine("Championships::LoadGroupMatches() - IOException");
         return null;
      }
   }

   MatchData FindNextMatchReference(MatchData cNextMatch) {
      if (cNextMatch.Equals(this.m_cElimA1B2)) {
         return this.m_cElimA1B2;
      }

      if (cNextMatch.Equals(this.m_cElimA2B1)) {
         return this.m_cElimA2B1;
      }

      if (cNextMatch.Equals(this.m_cElimC1D2)) {
         return this.m_cElimC1D2;
      }

      if (cNextMatch.Equals(this.m_cElimC2D1)) {
         return this.m_cElimC2D1;
      }

      if (cNextMatch.Equals(this.m_cSemi1)) {
         return this.m_cSemi1;
      }

      if (cNextMatch.Equals(this.m_cSemi2)) {
         return this.m_cSemi2;
      }

      if (cNextMatch.Equals(this.m_cFinal)) {
         return this.m_cFinal;
      }

      Tools.CrashLogError("Championships - Couldn't find m_cNextMatch reference");
      return null;
   }

   String PassQualifications(int nPlace) {
      if (this.m_nPhase != 0) {
         return "<dziala tylko w rozgrywkach grupowych";
      }

      for (int i = this.m_nCurGroupMatch; i < 5; i++) {
         MatchData match = this.GetNextMatch();
         match.RandomizeResult(5);
         this.ReportMatchEnd(match);
         Match.m_cMatchData = match;
      }

      this.Group(this.m_nPlayerGroup).removeElement(this.m_cPlayerTeam);
      this.Group(this.m_nPlayerGroup).insertElementAt(this.m_cPlayerTeam, nPlace);

      for (int i = 0; i < this.Group(this.m_nPlayerGroup).size(); i++) {
         Championships.TeamRecord cTeamOnPlace = (Championships.TeamRecord)this.Group(this.m_nPlayerGroup).elementAt(i);
         cTeamOnPlace.m_nPoints = 6 - 2 * i;
      }

      MatchData match = this.GetNextMatch();
      Match.m_cMatchData = match;
      GameObject.CrashLog(this.toString());
      return "<ok>";
   }

   MatchData GetNextMatch() {
      MatchData match = null;
      switch (this.m_nPhase) {
         case 0:
            match = this.GetGroupCurMatch(this.m_nPlayerGroup);
            break;
         case 1:
         case 2:
         case 3:
            match = this.m_cNextMatch;
      }

      return match;
   }

   void SaveGroup(Stream s, java.util.Vector vGroup) {
      Championships.TeamRecord cRecord = null;
      int nGroupCount = vGroup.size();

      try {
         s.out.writeInt(nGroupCount);

         for (int i = 0; i < nGroupCount; i++) {
            cRecord = (Championships.TeamRecord)vGroup.elementAt(i);
            cRecord.Save(s);
         }
      } catch (IOException e) {
         GameObject.LogLine("Championships::SaveGroup() - IOException");
      }
   }

   public String toString() {
      String sText = new String();
      sText = sText + "GroupA: \n";

      for (int i = 0; i < this.m_vGroupA.size(); i++) {
         sText = sText + "   " + (Championships.TeamRecord)this.m_vGroupA.elementAt(i) + "\n";
      }

      sText = sText + "       Matches:\n";

      for (int i = 0; i < this.m_vGroupAMatches.size(); i++) {
         sText = sText + "          " + (MatchData)this.m_vGroupAMatches.elementAt(i) + "\n";
      }

      sText = sText + "\nGroupB: \n";

      for (int i = 0; i < this.m_vGroupB.size(); i++) {
         sText = sText + "   " + (Championships.TeamRecord)this.m_vGroupB.elementAt(i) + "\n";
      }

      sText = sText + "       Matches:\n";

      for (int i = 0; i < this.m_vGroupBMatches.size(); i++) {
         sText = sText + "          " + (MatchData)this.m_vGroupBMatches.elementAt(i) + "\n";
      }

      sText = sText + "\nGroupC: \n";

      for (int i = 0; i < this.m_vGroupC.size(); i++) {
         sText = sText + "   " + (Championships.TeamRecord)this.m_vGroupC.elementAt(i) + "\n";
      }

      sText = sText + "       Matches:\n";

      for (int i = 0; i < this.m_vGroupCMatches.size(); i++) {
         sText = sText + "          " + (MatchData)this.m_vGroupCMatches.elementAt(i) + "\n";
      }

      sText = sText + "\nGroupD: \n";

      for (int i = 0; i < this.m_vGroupB.size(); i++) {
         sText = sText + "   " + (Championships.TeamRecord)this.m_vGroupD.elementAt(i) + "\n";
      }

      sText = sText + "       Matches:\n";

      for (int i = 0; i < this.m_vGroupDMatches.size(); i++) {
         sText = sText + "          " + (MatchData)this.m_vGroupDMatches.elementAt(i) + "\n";
      }

      sText = sText + "\nEliminations:\n";
      sText = sText + "  A1B2 " + this.m_cElimA1B2 + "\n";
      sText = sText + "  A2B1 " + this.m_cElimA2B1 + "\n";
      sText = sText + "  C1D2 " + this.m_cElimC1D2 + "\n";
      sText = sText + "  C2D1 " + this.m_cElimC2D1 + "\n";
      sText = sText + "\nSemi final:\n";
      sText = sText + "  semi1 " + this.m_cSemi1 + "\n";
      sText = sText + "  semi2 " + this.m_cSemi2 + "\n";
      sText = sText + "\nFINAL:\n";
      return sText + this.m_cFinal + "\n";
   }

   Championships.TeamRecord FindTeamRecord(int nTeam) {
      Championships.TeamRecord record = null;

      for (int i = 0; i < this.m_vAllTeams.size(); i++) {
         record = (Championships.TeamRecord)this.m_vAllTeams.elementAt(i);
         if (record.m_nTeam == nTeam) {
            return record;
         }
      }

      return null;
   }

   void Reset(PlayerSave cPlayer, int nPlayerTeam, int nPlayerTeamClothes, int nNextAvailableTeam) {
      if (this.m_bStartNextChampionships) {
         this.m_bStartNextChampionships = false;
      }

      this.m_bIsReplayingMatch = false;
      Match.m_cMatchData = null;
      this.m_nFinalOpponent = nNextAvailableTeam;
      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("FinalOpponent " + this.m_nFinalOpponent);
      }

      this.m_nPlayerTeam = nPlayerTeam;
      this.m_nPlayerTeamClothes = nPlayerTeamClothes;
      this.m_nPhase = 0;
      this.m_nCurGroupMatch = 0;
      java.util.Vector vTeams = new java.util.Vector();
      java.util.Vector vTeamsAvailable = new java.util.Vector();
      new java.util.Vector();
      this.m_vAllTeams.removeAllElements();

      // [mod] 12 places pour 14 equipes : l'equipe du joueur et l'adversaire de la finale sont toujours la,
      //       les 10 autres places sont tirees au sort parmi les equipes restantes (Panda et Tigre compris)
      int[] nRoster = this.DrawRoster();

      for (int r = 0; r < nRoster.length; r++) {
         int i = nRoster[r];
         Championships.TeamRecord team = new Championships.TeamRecord(this, i);
         if (i == this.m_nPlayerTeam) {
            this.m_cPlayerTeam = team;
         }

         vTeams.addElement(team);
         this.m_vAllTeams.addElement(team);
         if (cPlayer.IsTeamAvailable(i)) {
            vTeamsAvailable.addElement(team);
         }
      }

      while (vTeamsAvailable.size() > 6) {
         int nIndex = SoccerGame.GetRandom(0, vTeamsAvailable.size() - 1);
         if ((Championships.TeamRecord)vTeamsAvailable.elementAt(nIndex) != this.m_cPlayerTeam) {
            vTeamsAvailable.removeElementAt(nIndex);
         }
      }

      this.m_vGroupA.removeAllElements();

      for (int i = 0; i < 3; i++) {
         int nIndex = SoccerGame.GetRandom(0, vTeamsAvailable.size() - 1);
         Championships.TeamRecord team = (Championships.TeamRecord)vTeamsAvailable.elementAt(nIndex);
         this.m_vGroupA.addElement(team);
         if (team == this.m_cPlayerTeam) {
            this.m_nPlayerGroup = 0;
         }

         vTeamsAvailable.removeElement(team);
         vTeams.removeElement(team);
      }

      this.m_vGroupAMatches.removeAllElements();
      this.m_vGroupAMatches = this.DrawMatches(this.m_vGroupA);
      this.m_vGroupB.removeAllElements();

      for (int i = 0; i < 3; i++) {
         int nIndex = SoccerGame.GetRandom(0, vTeamsAvailable.size() - 1);
         Championships.TeamRecord team = (Championships.TeamRecord)vTeamsAvailable.elementAt(nIndex);
         this.m_vGroupB.addElement(team);
         if (team == this.m_cPlayerTeam) {
            this.m_nPlayerGroup = 1;
         }

         vTeamsAvailable.removeElement(team);
         vTeams.removeElement(team);
      }

      this.m_vGroupBMatches.removeAllElements();
      this.m_vGroupBMatches = this.DrawMatches(this.m_vGroupB);
      this.m_vGroupC.removeAllElements();

      for (int i = 0; i < 3; i++) {
         int nIndex = SoccerGame.GetRandom(0, vTeams.size() - 1);
         Championships.TeamRecord team = (Championships.TeamRecord)vTeams.elementAt(nIndex);
         this.m_vGroupC.addElement(team);
         vTeams.removeElement(team);
      }

      this.m_vGroupCMatches.removeAllElements();
      this.m_vGroupCMatches = this.DrawMatches(this.m_vGroupC);
      this.m_vGroupD.removeAllElements();

      for (int i = 0; i < 3; i++) {
         int nIndex = SoccerGame.GetRandom(0, vTeams.size() - 1);
         Championships.TeamRecord team = (Championships.TeamRecord)vTeams.elementAt(nIndex);
         this.m_vGroupD.addElement(team);
         vTeams.removeElement(team);
      }

      this.m_vGroupDMatches.removeAllElements();
      this.m_vGroupDMatches = this.DrawMatches(this.m_vGroupD);
      this.m_cElimA1B2 = this.m_cElimA2B1 = this.m_cElimC1D2 = this.m_cElimC2D1 = null;
      this.m_cSemi1 = this.m_cSemi2 = null;
      this.m_cFinal = null;
      this.m_cNextMatch = null;
      SoccerGame.SaveAttrib();
   }

   // [mod] les 12 equipes du tournoi, par ordre d'index
   int[] DrawRoster() {
      boolean[] bIn = new boolean[EAnimals._ANIMALS_COUNT];
      int nCount = 0;
      if (this.m_nPlayerTeam >= 0 && this.m_nPlayerTeam < bIn.length) {
         bIn[this.m_nPlayerTeam] = true;
         nCount++;
      }

      if (this.m_nFinalOpponent >= 0 && this.m_nFinalOpponent < bIn.length && !bIn[this.m_nFinalOpponent]) {
         bIn[this.m_nFinalOpponent] = true;
         nCount++;
      }

      while (nCount < 12) {
         int n = SoccerGame.GetRandom(0, bIn.length - 1);
         if (!bIn[n]) {
            bIn[n] = true;
            nCount++;
         }
      }

      int[] nRoster = new int[12];
      int k = 0;

      for (int i = 0; i < bIn.length; i++) {
         if (bIn[i]) {
            nRoster[k++] = i;
         }
      }

      return nRoster;
   }

   java.util.Vector Group(int nGroup) {
      switch (nGroup) {
         case 0:
            return this.m_vGroupA;
         case 1:
            return this.m_vGroupB;
         case 2:
            return this.m_vGroupC;
         case 3:
            return this.m_vGroupD;
         default:
            return null;
      }
   }

   void RandomizeElimMatch(MatchData cMatch) {
      if (cMatch != this.m_cNextMatch) {
         cMatch.RandomizeResultNoDraw();
         if ((cMatch.m_nTeamHost == this.m_nFinalOpponent || cMatch.m_nTeamGuest == this.m_nFinalOpponent) && !cMatch.IsWinner(this.m_nFinalOpponent)) {
            cMatch.SwitchResults();
         }
      }
   }

   MatchData GetGroupCurMatch(int nGroup) {
      return (MatchData)this.GroupMatches(nGroup).elementAt(this.m_nCurGroupMatch);
   }

   void SaveGroupMatches(Stream s, java.util.Vector vMatches) {
      MatchData cData = null;
      int nMatchCount = vMatches.size();

      try {
         s.out.writeInt(nMatchCount);

         for (int i = 0; i < nMatchCount; i++) {
            cData = (MatchData)vMatches.elementAt(i);
            cData.Save(s);
         }
      } catch (IOException e) {
         GameObject.LogLine("Championships::SaveGroupMatches() - IOException");
      }
   }

   boolean IsInGroup(int nGroup, int nAnimalType) {
      java.util.Vector vGroup = this.Group(nGroup);

      for (int i = 0; i < vGroup.size(); i++) {
         if (((Championships.TeamRecord)vGroup.elementAt(i)).m_nTeam == nAnimalType) {
            return true;
         }
      }

      return false;
   }

   boolean AreScoresEqual(int nGroup) {
      int nSize = this.Group(nGroup).size();
      int nPoints1 = ((Championships.TeamRecord)this.Group(nGroup).elementAt(0)).m_nPoints;
      int nPoints2 = ((Championships.TeamRecord)this.Group(nGroup).elementAt(1)).m_nPoints;
      int nPoints3 = ((Championships.TeamRecord)this.Group(nGroup).elementAt(2)).m_nPoints;
      return nPoints1 == nPoints2 && nPoints2 == nPoints3 ? true : nPoints2 == nPoints3;
   }

   boolean IsChampionshipInProgress() {
      return !this.m_bStartNextChampionships;
   }

   // [mod] classe imbriquee static : la version decompilee (this.this$0 = this$0) ne recompile pas.
   //       Meme constructeur (Championships, int), donc meme signature pour les classes non recompilees.
   public static class TeamRecord {
      int m_nPoints;
      int m_nTeam;
      int m_nCurrMatch;

      public String toString() {
         return EAnimals.m_sNames[this.m_nTeam] + " " + this.m_nPoints + " points";
      }

      TeamRecord(Championships cOwner, int nTeam) {
         this.m_nPoints = 0;
         this.m_nTeam = -1;
         this.m_nCurrMatch = 0;
         this.m_nTeam = nTeam;
      }

      TeamRecord(Championships cOwner) {
         this.m_nPoints = 0;
         this.m_nTeam = -1;
         this.m_nCurrMatch = 0;
      }

      boolean Load(Stream s) {
         try {
            if (!s.ReadMarker(9000)) {
               return false;
            }

            this.m_nPoints = s.in.readInt();
            this.m_nTeam = s.in.readInt();
            this.m_nCurrMatch = s.in.readInt();
            return s.ReadMarker(9100);
         } catch (IOException e) {
            GameObject.LogLine("Championships::Load() - IOException");
            return false;
         }
      }

      void Save(Stream s) {
         try {
            s.WriteMarker(9000);
            s.out.writeInt(this.m_nPoints);
            s.out.writeInt(this.m_nTeam);
            s.out.writeInt(this.m_nCurrMatch);
            s.WriteMarker(9100);
            s.out.flush();
         } catch (IOException e) {
            GameObject.LogLine("Championships::Save() - IOException");
         }
      }
   }
}
