public class Strategy {
   static final float _ACTIVE_DIST = 110.0F;
   float m_fSlantDirTime = 0.1F;
   float m_fSharpDirTime = 0.2F;
   float m_fBackSlantDirTime = 0.3F;
   float m_fBackDirTime = 0.4F;
   float m_fChangeDirTimeCoef = 0.01F;
   static final int PASS_GROUND = 0;
   float m_fMaxGoalChangeDiff = 0.0F;
   float m_fMaxSituationAnotherPlayerDiff = 0.0F;
   float m_fMaxSituationAnotherPlaceDiff = 0.0F;
   float m_fMaxSituationDiff = 0.0F;
   float m_fMaxValidPosDist = 0.0F;
   float m_fMaxValidPosDistGK = 0.0F;
   float m_fMinChangeValidPosDiff = 0.0F;
   float m_fMinChangeValidPosDiffGK = 0.0F;
   float m_fValidPosTolerance = 0.0F;
   float m_fValidPosToleranceGK = 0.0F;
   float m_fValidPosToleranceRough = 0.0F;
   float m_fMinDefenderValidPosScope = 0.0F;
   float m_fMaxDefenderValidPosScope = 0.0F;
   float m_fMinValidPosScope = 0.0F;
   float m_fMaxValidPosScope = 0.0F;
   float m_fMaxValidReferee = 1000.0F;
   boolean m_bAIThink = true;
   boolean m_bLeftUserControl = false;
   boolean m_bRightUserControl = false;
   float m_fMinGoalsDiff = 0.0F;
   float m_fMinDistFBallers = 0.0F;
   float m_fActiveDistForGorilla = 0.0F;
   float m_fRestoreDistance = 0.0F;
   float m_fDestPosTolerance = 0.0F;
   static boolean m_bAutoTakeBall = false;
   float m_fLookingTime = 0.0F;
   float m_fFreeTime = 1.5F;
   float m_fBreakSpecialBallTime = 0.0F;
   float m_fMinDistToChangeActive = 0.0F;
   float m_fAttackDestChange = 0.0F;
   float m_fMinForwardToNear = 0.0F;
   float m_fMinDistToChangeFocus = 500.0F;
   float m_fGoalKeeperDistGate = 0.0F;
   float m_fGKAttackScope = 500.0F;
   float m_fGoalKeeperStrafeSpeed = 0.0F;
   float m_fFocusInteria = 0.0F;
   float m_fFreeBallCapture = 0.0F;
   float m_fGKChangeRunToValidPos = 100.0F;
   float m_fFreedomRadius = 200.0F;
   float m_fAccessRadius = 100.0F;
   float m_fAccesDistInfluence = 1.0F;
   float m_fMinPassAcces = 0.5F;
   float m_fMinPassForwardDist = -500.0F;
   float m_fPassChanceTime = 0.1F;
   float m_fGateDistRadius = 1000.0F;
   float m_fFreedomBackwardInc = 3.0F;
   float m_fForwardToOppAngle = 60.0F;
   float m_fFeintDist = 100.0F;
   float m_fNewPosDist = 200.0F;
   float m_fFeintChance_GK = 40.0F;
   float m_fSpeedWithBall = 280.0F;
   float m_fSpeedThrowGK = 200.0F;
   static float m_fPassPrecision = 0.3F;
   float m_fMaxPrecisionDist = 200.0F;
   float m_fPerfectPrecisionDist = 200.0F;
   float m_fSituationCalcGrain = 0.25F;
   float m_fJumpSpeed = 1500.0F;
   float m_fAttackKickStength = 250.0F;
   float m_fSuperDefenceTime = 10.0F;
   float m_fSuperFeintTime = 5.0F;
   float m_fSuperSpeedTime = 10.0F;
   float m_fSuperSpeed = 500.0F;
   static float m_fSuperShotTime = 5.0F;
   float m_fDownDefence = 50.0F;
   float m_fUpDefence = 200.0F;
   float m_fThrowDefence = 150.0F;
   float m_fUpScope = 250.0F;
   float m_fThrowScope = 250.0F;
   float m_fDefenceDelayDiff = 4000.0F;
   float m_fDefenceWithoutDelay = 800.0F;
   float m_fReceiveRadius = 100.0F;
   float m_fRecieveScopeUp = 150.0F;
   float m_fReceiveDown = 50.0F;
   float m_fReceiveBrust = 70.0F;
   float m_fReceiveHead = 100.0F;
   float m_fKickForwardByHead = 400.0F;
   float m_fMaxShotByHeadDist = 500.0F;
   float m_fHeadKeyDistance = 100.0F;
   float m_fBlockControlTime = 2.0F;
   float m_fAIforFocusTime = 5.0F;
   float m_fLastTimeKey_A = 1.0F;
   float m_fMaxPassDirAngle = 45.0F;
   float m_fGKOutDist = 200.0F;
   float m_fFadingTime = 1.0F;
   static float m_fSpecialBallScope = 1000.0F;
   static float m_fSpecialBallScopeGK = 2200.0F;
   float m_fStabilizationWithDelay = 2.0F;
   float m_fMaxKeySpeedTime = 0.6F;
   float m_fMaxKeyFallSpeedTime = 0.4F;
   float m_fMaxKeyRestTime = 1.0F;
   float m_fKeySpeedSens = 0.3F;
   static float m_fAttackInteria = 1.5F;
   static float m_fAttackInteria_level_1 = 1.5F;
   static float m_fAttackInteria_level_2 = 1.5F;
   static float m_fAttackInteria_level_3 = 1.5F;
   static float m_fAttackInteria_level_4 = 1.5F;
   static float m_fAttackInteria_level_5 = 1.5F;
   static float m_fAttackInteria_level_6 = 1.5F;
   static float m_fAttackInteria_level_7 = 1.5F;
   static float m_fFreeBallInteria = 1.5F;
   static float m_fFreeBallInteria_level_1 = 1.5F;
   static float m_fFreeBallInteria_level_2 = 1.5F;
   static float m_fFreeBallInteria_level_3 = 1.5F;
   static float m_fFreeBallInteria_level_4 = 1.5F;
   static float m_fFreeBallInteria_level_5 = 1.5F;
   static float m_fFreeBallInteria_level_6 = 1.5F;
   static float m_fFreeBallInteria_level_7 = 1.5F;
   float m_fBallTakenInteria = 1.5F;
   float m_fBallTakenInteria_level_1 = 1.5F;
   float m_fBallTakenInteria_level_2 = 1.5F;
   float m_fBallTakenInteria_level_3 = 1.5F;
   float m_fBallTakenInteria_level_4 = 1.5F;
   float m_fBallTakenInteria_level_5 = 1.5F;
   float m_fBallTakenInteria_level_6 = 1.5F;
   float m_fBallTakenInteria_level_7 = 1.5F;
   float m_fFeintChance = 40.0F;
   float m_fFeintChance_level_1 = 40.0F;
   float m_fFeintChance_level_2 = 40.0F;
   float m_fFeintChance_level_3 = 40.0F;
   float m_fFeintChance_level_4 = 40.0F;
   float m_fFeintChance_level_5 = 40.0F;
   float m_fFeintChance_level_6 = 40.0F;
   float m_fFeintChance_level_7 = 40.0F;
   float m_fFeintChance_AI = 40.0F;
   float m_fFeintChance_level_1_AI = 40.0F;
   float m_fFeintChance_level_2_AI = 40.0F;
   float m_fFeintChance_level_3_AI = 40.0F;
   float m_fFeintChance_level_4_AI = 40.0F;
   float m_fFeintChance_level_5_AI = 40.0F;
   float m_fFeintChance_level_6_AI = 40.0F;
   float m_fFeintChance_level_7_AI = 40.0F;
   float m_fSuperUpgradeTime = 5.0F;
   float m_fSuperUpgradeTime_level_1 = 5.0F;
   float m_fSuperUpgradeTime_level_2 = 5.0F;
   float m_fSuperUpgradeTime_level_3 = 5.0F;
   float m_fSuperUpgradeTime_level_4 = 5.0F;
   float m_fSuperUpgradeTime_level_5 = 5.0F;
   float m_fSuperUpgradeTime_level_6 = 5.0F;
   float m_fSuperUpgradeTime_level_7 = 5.0F;
   float m_fPassChoiceCoef = 0.2F;
   float m_fPassChoiceCoef_level_1 = 0.2F;
   float m_fPassChoiceCoef_level_2 = 0.2F;
   float m_fPassChoiceCoef_level_3 = 0.2F;
   float m_fPassChoiceCoef_level_4 = 0.2F;
   float m_fPassChoiceCoef_level_5 = 0.2F;
   float m_fPassChoiceCoef_level_6 = 0.2F;
   float m_fPassChoiceCoef_level_7 = 0.2F;
   float m_fValidPosDeviation = 250.0F;
   float m_fValidPosDeviation_level_1 = 250.0F;
   float m_fValidPosDeviation_level_2 = 250.0F;
   float m_fValidPosDeviation_level_3 = 250.0F;
   float m_fValidPosDeviation_level_4 = 250.0F;
   float m_fValidPosDeviation_level_5 = 250.0F;
   float m_fValidPosDeviation_level_6 = 250.0F;
   float m_fValidPosDeviation_level_7 = 250.0F;
   float m_fShotOpportunity = 0.5F;
   float m_fShotOpportunity_level_1 = 0.5F;
   float m_fShotOpportunity_level_2 = 0.5F;
   float m_fShotOpportunity_level_3 = 0.5F;
   float m_fShotOpportunity_level_4 = 0.5F;
   float m_fShotOpportunity_level_5 = 0.5F;
   float m_fShotOpportunity_level_6 = 0.5F;
   float m_fShotOpportunity_level_7 = 0.5F;
   float m_fMaxShotDeviation = 1.0F;
   float m_fMaxShotDeviation_level_1 = 1.0F;
   float m_fMaxShotDeviation_level_2 = 1.0F;
   float m_fMaxShotDeviation_level_3 = 1.0F;
   float m_fMaxShotDeviation_level_4 = 1.0F;
   float m_fMaxShotDeviation_level_5 = 1.0F;
   float m_fMaxShotDeviation_level_6 = 1.0F;
   float m_fMaxShotDeviation_level_7 = 1.0F;
   float m_fGKNoticeTime = 0.2F;
   float m_fGKNoticeTime_level_1 = 0.2F;
   float m_fGKNoticeTime_level_2 = 0.2F;
   float m_fGKNoticeTime_level_3 = 0.2F;
   float m_fGKNoticeTime_level_4 = 0.2F;
   float m_fGKNoticeTime_level_5 = 0.2F;
   float m_fGKNoticeTime_level_6 = 0.2F;
   float m_fGKNoticeTime_level_7 = 0.2F;
   static float m_fShortPassSpeed = 0.0F;
   static float m_fMiddlePassSpeed = 0.0F;
   static float m_fLongPassSpeed = 0.0F;
   static float m_fFBSpeed = 400.0F;
   static float m_fShortPassDelay = 80.0F;
   static float m_fMiddlePassDelay = 100.0F;
   static float m_fLongPassDelay = 130.0F;
   float m_f_pGateDistance = 0.0F;
   float m_f_pAccess = 0.0F;
   float m_f_pAngle = 0.0F;
   float m_f_pS = 0.0F;
   float m_f_pSw = 0.5F;
   float m_f_pSp = 0.5F;
   float m_f_pS_gk = 0.0F;
   float m_f_pSw_gk = 0.0F;
   float m_f_pSp_gk = 0.0F;
   int m_nRight_l = 205;
   int m_nLeft_l = 203;
   int m_nUp_l = 200;
   int m_nDown_l = 208;
   int m_nPass_l = 31;
   int m_nShot_l = 32;
   int m_nFeint_l = 30;
   int m_nSuper_l = 33;
   int m_nRight_r = 205;
   int m_nLeft_r = 203;
   int m_nUp_r = 200;
   int m_nDown_r = 208;
   int m_nPass_r = 31;
   int m_nShot_r = 32;
   int m_nFeint_r = 30;
   int m_nSuper_r = 33;

   static float GetActiveDistance(AIFootBaller cFBaller) {
      return cFBaller.fEllRadius + 110.0F;
   }

   FootBaller.DirAction GetActionDir(FootBaller cFBaller, Vector vDir, boolean bPartition) {
      Vector vForwardVector = new Vector(cFBaller.GetForwardVector());
      vForwardVector.Normalize();
      Vector vDestDir = new Vector(vDir);
      float fAngle = vDestDir.Dot(vForwardVector);
      fAngle = Tools.acos(fAngle);
      fAngle = (float)(fAngle * 180.0F / Math.PI);
      Vector vNormal = vForwardVector.Cross(vDir);
      if (vNormal.fY > 0.0F) {
         fAngle = -fAngle;
      }

      return this.SelectDirAction(vDir, fAngle, bPartition);
   }

   static float GetActiveDistance(AIFootBaller cFirst, AIFootBaller cSec) {
      return cFirst.fEllRadius > cSec.fEllRadius ? cFirst.fEllRadius + 110.0F : cSec.fEllRadius + 110.0F;
   }

   float RadianToDegrees(float fAngle) {
      return (float)(fAngle * 180.0F / Math.PI);
   }

   FootBaller FindPassPlayer(AIFootBaller cFBaller, float fScope) {
      FootBaller cDestFBaller = this.FindPassPlayer(cFBaller, true, fScope);
      if (cDestFBaller == null) {
         cDestFBaller = this.FindPassPlayer(cFBaller, false, fScope);
      }

      return cDestFBaller;
   }

   FootBaller FindPassPlayer(AIFootBaller cFBaller, boolean bRestrict, float fScope) {
      TeamController cController = cFBaller.m_cTeam.GetTeamController();
      Vector vForward;
      if (cController._KEY_STATES == 0) {
         vForward = cFBaller.GetForwardVector();
      } else {
         vForward = cController.GetFieldDirFromKeys();
         if (vForward == null) {
            vForward = cFBaller.GetForwardVector();
         }
      }

      vForward.Normalize();
      Vector vPosition = cFBaller.GetPosition();
      Team cTeam = cFBaller.m_cTeam;
      int nMinAngleFB = -1;
      float fMinAngle = 361.0F;
      java.util.Vector vSelectedBallers = new java.util.Vector();
      int nSize = cTeam.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         FootBaller cFDestBaller = (FootBaller)cTeam.m_vFootBallers.elementAt(i);
         Vector vDestBallerPos = cFDestBaller.GetPosition();
         if (cFDestBaller != cFBaller
            && !((AIFootBaller)cFDestBaller).IsGoalKeeper()
            && (cFBaller.IsUserControled() || !ModTactics.IsOffside((AIFootBaller)cFDestBaller))) { // [mod] l'IA ne sert pas un joueur hors-jeu
            float fForwardDist = this.GetForwardDistDiff(cFBaller, cFDestBaller);
            if (!bRestrict || !(fForwardDist < this.m_fMinPassForwardDist)) {
               if (fScope != -1.0F) {
                  float fDist = cFBaller.GetDistanceTo(cFDestBaller);
                  if (fDist > fScope) {
                     continue;
                  }
               }

               Vector vDiff = new Vector(vDestBallerPos);
               vDiff.Sub(vPosition);
               vDiff.Normalize();
               float fCos = Vector.Dot(vDiff, vForward);
               float fAngle = Tools.acos(fCos);
               fAngle = (float)(fAngle * 180.0F / Math.PI);
               if (!bRestrict || !(fAngle > this.m_fMaxPassDirAngle)) {
                  if (bRestrict) {
                     vSelectedBallers.addElement(cFDestBaller);
                  } else if (fAngle < fMinAngle) {
                     nMinAngleFB = i;
                     fMinAngle = fAngle;
                  }
               }
            }
         }
      }

      if (!bRestrict) {
         return nMinAngleFB != -1 ? (FootBaller)cTeam.m_vFootBallers.elementAt(nMinAngleFB) : null;
      } else {
         return cFBaller.IsUserControled() ? this.FindBestDistanceFB(cFBaller, vSelectedBallers) : this.FindBestSituationFB(vSelectedBallers);
      }
   }

   boolean CanBeChangeNearest(AIFootBaller cNearest) {
      if (cNearest == null) {
         return false;
      } else {
         AIFootBaller cActiveFB = cNearest.m_cTeam.GetActiveFB();
         if (cActiveFB == null) {
            return true;
         } else {
            return cNearest.IsHelpBallAttacker() ? false : !cNearest.IsUserControled() || !((AITeam)cNearest.m_cTeam).IsBlockControl();
         }
      }
   }

   Vector ChoseBestNewPosition(AIFootBaller cFBaller, float fNewPosDist) {
      Vector vBestNewPos = this.ChoseBestPosition(cFBaller, fNewPosDist, null);
      if (vBestNewPos == null) {
         return null;
      }

      // [mod] hors-jeu doux : pas d'appel de balle derrière la ligne de hors-jeu
      if (ModTactics.IsOffsideAt(cFBaller, vBestNewPos)) {
         return null;
      }

      float fCurrSituation = cFBaller.CalculateSituation(cFBaller.GetPosition()).GetValue();
      float fBestNewSituation = cFBaller.CalculateSituation(vBestNewPos).GetValue();
      return fBestNewSituation - fCurrSituation > this.m_fMaxSituationAnotherPlaceDiff ? new Vector(vBestNewPos) : null;
   }

   Vector ChosePassPosition(AIFootBaller cFBaller, AIFootBaller cDestFB) {
      // [mod] coéquipier en pleine course : le ballon part dans l'espace devant lui
      Vector vThrough = ModMovement.ThroughPoint(cFBaller, cDestFB);
      if (vThrough != null) {
         return vThrough;
      }

      float fDist = cFBaller.GetDistanceTo(cDestFB);
      Vector vBestDest = null;
      float fBallSpeed = CalcSpeed(fDist);
      float fTime = fDist / fBallSpeed;
      float fDelay = 0.0F;
      if (fBallSpeed == m_fShortPassSpeed) {
         fDelay = m_fShortPassDelay;
      }

      if (fBallSpeed == m_fMiddlePassSpeed) {
         fDelay = m_fMiddlePassDelay;
      }

      if (fBallSpeed == m_fLongPassSpeed) {
         fDelay = m_fLongPassDelay;
      }

      float fDistToDest = m_fFBSpeed * fTime - fDelay;
      vBestDest = this.ChoseBestPosition(cDestFB, fDistToDest, null);
      return vBestDest != null ? new Vector(vBestDest) : cFBaller.GetPosition();
   }

   AIFootBaller.Feint CalcFeint(AIFootBaller cFBaller, AIFootBaller cOpponent) {
      AIFootBaller.Feint cFeint = new AIFootBaller.Feint();
      cFeint.m_cOpponent = cOpponent;
      float fRnd = GameObject.RandomFloat(100.0F);
      float fChance = 0.0F;
      if (ModSupers.IsFrozen(cFBaller)) {
         // [mod] un joueur gelé ne dribble plus (sinon il garde le ballon en feintant sans fin)
         fChance = 0.0F;
      } else if (cOpponent.IsGoalKeeper()) {
         fChance = this.m_fFeintChance_GK;
      } else if (cFBaller.IsSuperFeint() && !cOpponent.IsGoalKeeper()) {
         fChance = 0.0F;
      } else if (!cFBaller.IsUserControled()) {
         if (!cOpponent.IsUserControled()) {
            fChance = 50.0F;
         } else {
            fChance = this.m_fFeintChance_AI;
         }
      } else {
         fChance = this.m_fFeintChance;
      }

      if (fRnd <= fChance) {
         cFeint.m_bSucces = true;
      } else {
         cFeint.m_bSucces = false;
      }

      Vector vDiff = cFBaller.GetPosition();
      vDiff.Sub(cOpponent.GetPosition());
      vDiff.Normalize();
      vDiff.Mul(GetActiveDistance(cFBaller, cOpponent) / 2.0F);
      Vector vFeintPos = this.GetNextPointToOpp(cFBaller, cOpponent, this.m_fFeintDist);
      cFeint.m_vFeintPos = new Vector();
      vFeintPos.Add(vDiff, cFeint.m_vFeintPos);
      Vector vDestPos = new Vector();
      vDiff.Mul(-2.0F);
      cFeint.m_vFeintPos.Add(vDiff, vDestPos);
      cFeint.m_vDestPos = vDestPos;
      return cFeint;
   }

   static float CalcSpeed(float fDist) {
      if (fDist > 1700.0F) {
         return m_fLongPassSpeed;
      } else {
         return fDist > 600.0F ? m_fMiddlePassSpeed : m_fShortPassSpeed;
      }
   }

   AIFootBaller FindBestSituationFB(java.util.Vector vList) {
      int nMaxFB = -1;
      float fMaxSituation = -3.0F;
      int nMaxDistFB = -1;
      float fMaxDistance = -1.0F;
      int nMinDistFB = -1;
      float fMinDistance = 10000.0F;
      FootBaller cFBwithBall = GameField.m_cBall.GetOwner();
      int nSize = vList.size();

      for (int i = 0; i < nSize; i++) {
         AIFootBaller cBestBaller = (AIFootBaller)vList.elementAt(i);
         if (cBestBaller.m_cDecContext.m_cSituation.GetValue() > fMaxSituation) {
            nMaxFB = i;
            fMaxSituation = cBestBaller.m_cDecContext.m_cSituation.GetValue();
         }
      }

      AIFootBaller cSelectedFB = null;
      if (nMaxFB > -1) {
         cSelectedFB = (AIFootBaller)vList.elementAt(nMaxFB);
      }

      return cSelectedFB;
   }

   AIFootBaller FindBestDistanceFB(AIFootBaller cFBaller, java.util.Vector vList) {
      int nMaxFB = -1;
      float fMaxDistance = -1.0F;
      int nMinFB = -1;
      float fMinDistance = GameField.m_fDiagonalLength;
      int nSize = vList.size();

      for (int i = 0; i < nSize; i++) {
         AIFootBaller cBestBaller = (AIFootBaller)vList.elementAt(i);
         float fDistance = cFBaller.GetDistanceTo(cBestBaller);
         if (fDistance > fMaxDistance) {
            nMaxFB = i;
            fMaxDistance = fDistance;
         }

         if (fDistance < fMinDistance) {
            nMinFB = i;
            fMinDistance = fDistance;
         }
      }

      AIFootBaller cSelectedFB = null;
      if (cFBaller.HasToHeadLob()) {
         if (nMaxFB > -1) {
            cSelectedFB = (AIFootBaller)vList.elementAt(nMaxFB);
         }
      } else if (nMinFB > -1) {
         cSelectedFB = (AIFootBaller)vList.elementAt(nMinFB);
      }

      return cSelectedFB;
   }

   FootBaller FindBestFootBaller(FootBaller cFBaller, float fScope) {
      Team cTeam = cFBaller.m_cTeam;
      int nMinFB = -1;
      // [mod] le jeu d'origine gardait le coéquipier le MOINS bien placé ; PassToBest(1) prend le mieux placé
      boolean bBest = ModMovement.PassToBest();
      float fMinSituation = bBest ? -100.0F : 100.0F;
      int nSize = cTeam.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         AIFootBaller cFDestBaller = (AIFootBaller)cTeam.m_vFootBallers.elementAt(i);
         if (cFDestBaller != cFBaller
            && !cFDestBaller.IsGoalKeeper()
            && !ModTactics.IsOffside(cFDestBaller) // [mod] pas de passe vers un joueur hors-jeu
            && !(this.GetForwardDistDiff(cFBaller, cFDestBaller) < this.m_fMinPassForwardDist)
            && (cFDestBaller.m_vPassPosition != null || !AIMatch.IsMatchState(0))) {
            if (fScope != -1.0F) {
               float fDist = cFBaller.GetDistanceTo(cFDestBaller);
               if (cFBaller.IsGoalKeeper() ? fDist < 800.0F || fDist > fScope : fDist > fScope) {
                  continue;
               }
            }

            float fSituation = cFDestBaller.m_fPassPositionSituation;
            if (!AIMatch.IsMatchState(0)) {
               fSituation = cFDestBaller.GetSituation(cFDestBaller.GetPositionVectorVolatile());
            }

            if (bBest ? fSituation > fMinSituation : fSituation < fMinSituation) {
               nMinFB = i;
               fMinSituation = bBest ? fSituation : cFDestBaller.m_fPassPositionSituation;
            }
         }
      }

      return nMinFB != -1 ? (FootBaller)cTeam.m_vFootBallers.elementAt(nMinFB) : null;
   }

   Vector GetGKPosWithSuperWall(Vector vWallPos, AIFootBaller cGK) {
      Vector vGKPos = new Vector(vWallPos);
      vGKPos.fX = vGKPos.fX - (GameField.m_fGateWidth / 2.0F + 50.0F);
      if (cGK.m_nTeam == 0) {
         vGKPos.fZ += 100.0F;
      } else {
         vGKPos.fZ -= 100.0F;
      }

      return vGKPos;
   }

   float CalculateTraceObstacles(AIFootBaller cAIFootBaller, Vector vBegin, Vector vEnd, AIFootBaller cExcludeFB) {
      Vector vTrace = new Vector(vBegin);
      vTrace.Sub(vEnd);
      Vector vLeft = Vector.Cross(vTrace, Vector.Up);
      Vector vRight = Vector.Cross(Vector.Up, vTrace);
      vLeft.Normalize();
      vRight.Normalize();
      vLeft.Mul(this.m_fAccessRadius);
      vRight.Mul(this.m_fAccessRadius);
      Team cOppTeam;
      if (cAIFootBaller.m_nTeam == 0) {
         cOppTeam = SoccerModule.m_cMatch.m_teams[1];
      } else {
         cOppTeam = SoccerModule.m_cMatch.m_teams[0];
      }

      Collision cCol = null;
      cAIFootBaller.ClearFlag(8);
      GameField.m_cBall.ClearFlag(8);
      cOppTeam.m_cGoalKeeper.ClearFlag(8);
      cCol = cAIFootBaller.TraceObjectsExclude(new Vector(vBegin.fX, vBegin.fY + 40.0F, vBegin.fZ), new Vector(vEnd), cExcludeFB);
      float fResult;
      if (cCol != null) {
         fResult = 0.0F;
      } else {
         Vector vBeginLeft = vBegin.Add2(vLeft);
         Vector vEndLeft = vEnd.Add2(vLeft);
         cCol = cAIFootBaller.TraceObjectsExclude(vBeginLeft, vEndLeft, cExcludeFB);
         if (cCol != null) {
            fResult = 0.0F;
         } else {
            Vector vBeginRight = vBegin.Add2(vRight);
            Vector vEndRight = vEnd.Add2(vRight);
            cCol = cAIFootBaller.TraceObjectsExclude(vBeginRight, vEndRight, cExcludeFB);
            if (cCol != null) {
               fResult = 0.0F;
            } else {
               fResult = 1.0F;
            }
         }
      }

      cAIFootBaller.SetFlag(8);
      GameField.m_cBall.SetFlag(8);
      cOppTeam.m_cGoalKeeper.SetFlag(8);
      return fResult;
   }

   boolean CanTakeBall(AIFootBaller cFBaller, AIFootBaller cOpponent) {
      if (this.IsForwardToPos(cOpponent, cFBaller.GetPosition())) {
         return true;
      }

      float fScope = GetActiveDistance(cFBaller, cOpponent);
      return cOpponent.GetDistanceTo(cFBaller) < fScope;
   }

   boolean IsBetweenPos(Vector vBetweenPos, Vector vBeginPos, Vector vEndPos) {
      Vector vBeginToEndDist = new Vector(vEndPos);
      vBeginToEndDist.Sub(vBeginPos);
      float fBeginToEndDist = vBeginToEndDist.Len();
      Vector vBeginDist = new Vector(vBeginPos);
      vBeginDist.Sub(vBetweenPos);
      float fBeginDist = vBeginDist.Len();
      Vector vEndDist = new Vector(vEndPos);
      vEndDist.Sub(vBetweenPos);
      float fEndDist = vEndDist.Len();
      return !(fBeginToEndDist < fBeginDist) && !(fBeginToEndDist < fEndDist);
   }

   boolean IsOpponentForwardToPos(Vector vPos, FootBaller cOpponent) {
      Vector vGateOpp;
      if (cOpponent.m_nTeam == 0) {
         vGateOpp = GameField.cGateLeft.GetPosition();
      } else {
         vGateOpp = GameField.cGateRight.GetPosition();
      }

      return this.IsBetweenPos(cOpponent.GetPosition(), vPos, vGateOpp);
   }

   Vector GetSpecialBallPos(int nSide) {
      Vector vPos = null;
      if (AIMatch.IsMatchState(5)) {
         vPos = this.GetOutPoint(nSide);
      }

      if (AIMatch.IsMatchState(4)) {
         vPos = this.GetCornerPoint(nSide);
      }

      return vPos;
   }

   Vector GetGoalKeeperOutPos(int nSide) {
      Vector vGatePos;
      boolean bLeftGate;
      if (nSide == 2) {
         vGatePos = GameField.cGateLeft.GetPosition();
         bLeftGate = true;
      } else {
         vGatePos = GameField.cGateRight.GetPosition();
         bLeftGate = false;
      }

      Vector vBallPos = GameField.m_cBall.GetPosition();
      Vector vCornerPos = GameField.cCornersTab[0].GetPosition();
      float fGateDist = vGatePos.fX - vCornerPos.fX;
      float fBallDist = vBallPos.fX - vCornerPos.fX;
      boolean bLeftSide;
      if (fGateDist > fBallDist) {
         bLeftSide = true;
      } else {
         bLeftSide = false;
      }

      Vector vOutPos = new Vector(vGatePos);
      if (bLeftSide) {
         vOutPos.fX = vOutPos.fX - GameField.m_fGateWidth / 2.0F;
      } else {
         vOutPos.fX = vOutPos.fX + GameField.m_fGateWidth / 2.0F;
      }

      if (bLeftGate) {
         vOutPos.fZ = vOutPos.fZ + this.m_fGKOutDist;
      } else {
         vOutPos.fZ = vOutPos.fZ - this.m_fGKOutDist;
      }

      return vOutPos;
   }

   Vector GetSuperWallPos(AITeam cTeam) {
      Vector vGatePos;
      boolean bLeft;
      if (cTeam.m_nSide == 0) {
         vGatePos = GameField.cGateLeft.GetPosition();
         bLeft = true;
      } else {
         vGatePos = GameField.cGateRight.GetPosition();
         bLeft = false;
      }

      Vector vWallPos = vGatePos;
      if (bLeft) {
         vWallPos.fZ += 10.0F;
      } else {
         vWallPos.fZ -= 10.0F;
      }

      return vWallPos;
   }

   float CalculateFreedom(AIFootBaller cAIFootBaller, Vector vPos) {
      float fResult = 0.0F;
      Team cOppTeam;
      if (cAIFootBaller.m_nTeam == 0) {
         cOppTeam = SoccerModule.m_cMatch.m_teams[1];
      } else {
         cOppTeam = SoccerModule.m_cMatch.m_teams[0];
      }

      float nMinOppDist = GameField.GetDiagonalLength();
      FootBaller cMinOpp = null;
      int nSize = cOppTeam.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         FootBaller cOpponent = (FootBaller)cOppTeam.m_vFootBallers.elementAt(i);
         float fDistance = cOpponent.GetDistanceTo(vPos);
         if (fDistance < nMinOppDist) {
            nMinOppDist = fDistance;
            cMinOpp = cOpponent;
         }
      }

      if (cMinOpp != null) {
         fResult = nMinOppDist;
         if (!this.IsOpponentForwardToPos(vPos, cMinOpp)) {
            fResult *= this.m_fFreedomBackwardInc;
         }

         if (fResult < this.m_fMinDistFBallers) {
            fResult = 0.0F;
         } else if (fResult < this.m_fFreedomRadius) {
            fResult /= this.m_fFreedomRadius;
         } else {
            fResult = 1.0F;
         }
      }

      return fResult;
   }

   float GetOppGateDistance(FootBaller cFBaller) {
      ControlObject cGateOpp = null;
      if (cFBaller.m_nTeam == 0) {
         cGateOpp = GameField.cGateRight;
      } else {
         cGateOpp = GameField.cGateLeft;
      }

      return cFBaller.GetDistanceTo(cGateOpp);
   }

   AIFootBaller GetFBDistance(FootBaller cFBaller) {
      Team cOppTeam;
      if (cFBaller.m_nTeam == 0) {
         cOppTeam = SoccerModule.m_cMatch.m_teams[1];
      } else {
         cOppTeam = SoccerModule.m_cMatch.m_teams[0];
      }

      AIFootBaller cActiveOpp = (AIFootBaller)cOppTeam.m_cActiveFootBaller;
      if (cActiveOpp != null && this.IsForwardToPos(cFBaller, cActiveOpp.GetPosition())) {
         float fDistance = cFBaller.GetDistanceTo(cActiveOpp);
         return fDistance < this.m_fMinDistFBallers ? cActiveOpp : null;
      } else {
         return null;
      }
   }

   boolean IsForwardInside(AIFootBaller cFBaller, Vector vForwardPos) {
      Situation cSituation = cFBaller.CalculateSituation(cFBaller.m_vForward);
      return GameField.GetZoneForPoint(vForwardPos, cFBaller.m_cTeam.m_nSide) != 3
         && GameField.GetZoneForPoint(vForwardPos, cFBaller.m_cTeam.m_nSide) != 4
         && GameField.GetZoneForPointVertical(vForwardPos) != 6
         && GameField.GetZoneForPointVertical(vForwardPos) != 5;
   }

   int GetDefenceType(AIFootBaller cGK, AIFootBaller.Shot cShot) {
      float fHorizontDist = cShot.m_vDest.fX - cGK.GetPosition().fX;
      float fVecticalDist = cShot.m_vDest.fY - cGK.GetPosition().fY;
      boolean bLeft = false;
      // [mod] écart latéral pris en valeur absolue : le jeu d'origine ne bornait que d'un côté (un tir très écarté
      // d'un côté faisait réagir le gardien, pas de l'autre)
      if (!(Math.abs(fHorizontDist) > this.m_fThrowScope) && !(fVecticalDist > this.m_fUpScope)) {
         if (fHorizontDist < 0.0F) {
            if (cGK.m_nTeam == 1) {
               bLeft = true;
            } else {
               bLeft = false;
            }

            fHorizontDist = -fHorizontDist;
         } else if (cGK.m_nTeam == 1) {
            bLeft = false;
         } else {
            bLeft = true;
         }

         // [mod] hauteur du gardien au moins KeeperMinReach (team_stats.scr) : les petits gardiens sautent moins tôt,
         // et leur arrêt va aussi haut (AIFootBaller, BumpDefenceBall)
         if (fVecticalDist > ETeams.GetKeeperReach(cGK) + this.m_fUpDefence) {
            cGK.m_bJumping = true;
         }

         float fMinThrowScope = this.m_fThrowDefence;
         if (cGK.GetDistanceTo(cShot.m_vStartPos) > 600.0F) {
            fMinThrowScope *= 2.0F;
         }

         if (fHorizontDist > fMinThrowScope && cShot.m_fAngle < 60.0F) {
            GameObject.LogLine(" Z RZUTEM :" + cShot.m_fAngle);
            return bLeft ? 1 : 2;
         } else {
            GameObject.LogLine(" BEZ RZUTU :" + cShot.m_fAngle);
            return fVecticalDist > this.m_fDownDefence ? 3 : 4;
         }
      } else {
         return 0;
      }
   }

   boolean IsOpponentForward(FootBaller cFBaller, FootBaller cOpponent) {
      Vector vOppGatePos = this.GetOpponentGatePos(cFBaller);
      return this.IsBetweenPos(cOpponent.GetPosition(), cFBaller.GetPosition(), vOppGatePos);
   }

   boolean CanSetHeadKey(FootBaller cFBaller) {
      return cFBaller.GetDistanceTo(GameField.m_cBall) < this.m_fHeadKeyDistance;
   }

   FootBaller.DirAction GetActionDirToDest(FootBaller cFBaller, Vector vDest, boolean bPartition) {
      Vector vDiff = new Vector(vDest);
      vDiff.Sub(cFBaller.GetPosition());
      if (vDiff.Len() < 5.0E-4F) {
         return null;
      }

      vDiff.Normalize();
      Vector vForwardVector = new Vector(cFBaller.GetForwardVector());
      vForwardVector.Normalize();
      float fAngle = vDiff.Dot(vForwardVector);
      fAngle = Tools.acos(fAngle);
      fAngle = (float)(fAngle * 180.0F / Math.PI);
      Vector vNormal = vForwardVector.Cross(vDiff);
      if (vNormal.fY > 0.0F) {
         fAngle = -fAngle;
      }

      return this.SelectDirAction(vDiff, fAngle, bPartition);
   }

   boolean CanAttackDestChange(Vector vDest, Vector vOldDest) {
      Vector vResult = new Vector(vDest);
      vResult.Sub(vOldDest);
      return vResult.Len() > this.m_fAttackDestChange;
   }

   static boolean IsSelectionDestInCorrectArea(Vector vPos) {
      if (GameField.GetZoneForPoint(vPos, 0) == 3
         || GameField.GetZoneForPoint(vPos, 0) == 4
         || GameField.GetZoneForPointVertical(vPos) == 6
         || GameField.GetZoneForPointVertical(vPos) == 5) {
         return false;
      }

      if (!(vPos.fZ - 100.0F < GameField.cCornersTab[0].GetPositionVectorVolatile().fZ)
         && !(vPos.fZ + 100.0F > GameField.cCornersTab[1].GetPositionVectorVolatile().fZ)
         && !(vPos.fX + 100.0F > GameField.cCornersTab[1].GetPositionVectorVolatile().fX)
         && !(vPos.fX - 100.0F < GameField.cCornersTab[3].GetPositionVectorVolatile().fX)) {
         float fDist = GameField.m_cBall.GetDistanceTo(vPos);
         float fScope = m_fSpecialBallScope;
         if (AIMatch.IsMatchState(6)) {
            fScope = m_fSpecialBallScopeGK * 1.45F;
         }

         return !(fDist > fScope) && (!(fDist < 350.0F) || AIMatch.IsMatchState(7));
      } else {
         return false;
      }
   }

   boolean IsFBDistanceOpp(FootBaller cFBaller) {
      Team cOppTeam;
      if (cFBaller.m_nTeam == 0) {
         cOppTeam = SoccerModule.m_cMatch.m_teams[1];
      } else {
         cOppTeam = SoccerModule.m_cMatch.m_teams[0];
      }

      int nSize = cOppTeam.m_vFootBallers.size();

      for (int i = 0; i < nSize; i++) {
         AIFootBaller cOpponent = (AIFootBaller)cOppTeam.m_vFootBallers.elementAt(i);
         float fDistance = cFBaller.GetDistanceTo(cOpponent);
         if (!(fDistance > this.m_fMinDistFBallers)
            && (!(fDistance > GetActiveDistance((AIFootBaller)cFBaller, cOpponent)) || !this.IsBackwardToPos(cFBaller, cOpponent.GetPosition()))) {
            return true;
         }
      }

      return false;
   }

   boolean CanAttackerGoToOpp() {
      AIFootBaller cPossesorFB = (AIFootBaller)GameField.m_cBall.GetOwner();
      return !cPossesorFB.IsGoalKeeper() && !cPossesorFB.Busy();
   }

   void SetDirToCenter(AIFootBaller cFBaller) {
      Vector vDir = new Vector(GameField.cCenter.GetPosition());
      vDir.Sub(cFBaller.GetPositionVectorVolatile());
      cFBaller.ChangeDirectionLogic(this.GetActionDir(cFBaller, vDir, false));
   }

   void SetDirToBall(AIFootBaller cFBaller) {
      Vector vDir = new Vector(GameField.m_cBall.GetPosition());
      vDir.Sub(cFBaller.GetPositionVectorVolatile());
      cFBaller.ChangeDirectionLogic(this.GetActionDir(cFBaller, vDir, false));
   }

   Vector GetNextPointToOpp(AIFootBaller cFBaller, AIFootBaller cOpponent, float fDist) {
      Vector vDist = cOpponent.GetPosition();
      vDist.Sub(cFBaller.GetPosition());
      vDist.Normalize();
      Vector vUp = cOpponent.GetUpVector();
      vUp.Normalize();
      float fRnd = GameObject.RandomFloat(100.0F);
      Vector vDir;
      if (fRnd > 50.0F) {
         vDir = vDist.Cross(vUp);
      } else {
         vDir = vUp.Cross(vDist);
      }

      vDir.Normalize();
      vDir.Mul(fDist);
      Vector vResult = new Vector();
      vDir.Add(cOpponent.GetPosition(), vResult);
      return vResult;
   }

   Vector CalculateValidPosition(AIFootBaller cFBaller, Vector vValidPos) {
      Vector vBegin = null;
      Vector vEnd = null;
      if (cFBaller.IsGoalKeeper()) {
         Vector vBallPos = new Vector(GameField.m_cBall.GetPosition());
         Vector vGateCenter = null;
         if (cFBaller.m_nTeam == 0) {
            vGateCenter = GameField.cGateLeft.GetPosition();
         }

         if (cFBaller.m_nTeam == 1) {
            vGateCenter = GameField.cGateRight.GetPosition();
         }

         vBallPos.Sub(vGateCenter);
         vBallPos.Normalize();
         vBallPos.Mul(this.m_fGoalKeeperDistGate);
         Vector vResult = new Vector();
         vBallPos.Add(vGateCenter, vResult);
         if (cFBaller.m_nTeam == 0) {
            if (vResult.fZ < vGateCenter.fZ) {
               vResult.fZ = vGateCenter.fZ + this.m_fGoalKeeperDistGate;
            }
         } else if (vResult.fZ > vGateCenter.fZ) {
            vResult.fZ = vGateCenter.fZ - this.m_fGoalKeeperDistGate;
         }

         return vResult;
      } else {
         int nZone = cFBaller.GetZone();
         if (nZone != 0 && (nZone != 3 || cFBaller.m_nTeam != 0) && (nZone != 4 || cFBaller.m_nTeam != 1)) {
            if (nZone == 1) {
               vBegin = cFBaller.aPatrolPath[1].GetPosition();
               vEnd = cFBaller.aPatrolPath[2].GetPosition();
            } else if (nZone != 2 && (nZone != 4 || cFBaller.m_nTeam != 0) && (nZone != 3 || cFBaller.m_nTeam != 1)) {
               GameObject.LogLine(cFBaller + " zone none , zone: " + cFBaller.GetZone() + " team " + cFBaller.m_nTeam);
            } else {
               vBegin = cFBaller.aPatrolPath[2].GetPosition();
               vEnd = cFBaller.aPatrolPath[3].GetPosition();
            }
         } else {
            vBegin = cFBaller.aPatrolPath[0].GetPosition();
            vEnd = cFBaller.aPatrolPath[1].GetPosition();
         }

         Vector vTrace = new Vector(vEnd);
         vTrace.Sub(vBegin);
         vTrace.Normalize();
         Vector vValidLocalPos = new Vector(vValidPos);
         vValidLocalPos.Sub(vBegin);
         Vector vValidNormalPos = new Vector(vValidLocalPos);
         vValidNormalPos.Normalize();
         float fCos = vTrace.Dot(vValidNormalPos);
         float fNewLen = fCos * vValidLocalPos.Len();
         Vector vResult = new Vector();
         vTrace.fX *= fNewLen;
         vTrace.fY *= fNewLen;
         vTrace.fZ *= fNewLen;
         vBegin.Add(vTrace, vResult);
         return vResult;
      }
   }

   // [mod] portée de tir du joueur : GateDistRadius (ai_params.scr) x Force (ETeams.GetShotRangeMul)
   float ShotRadius(AIFootBaller cFBaller) {
      return this.m_fGateDistRadius * ETeams.GetShotRangeMul(cFBaller.m_nAnimalType);
   }

   AIFootBaller.Shot GetShotDescription(AIFootBaller cFBaller) {
      Vector vGatePos = null;
      boolean bLeftGate = false;
      ControlObject cOppGate;
      if (cFBaller.m_nTeam == 0) {
         cOppGate = GameField.cGateRight;
         vGatePos = GameField.cGateRight.GetPosition();
      } else {
         cOppGate = GameField.cGateLeft;
         vGatePos = GameField.cGateLeft.GetPosition();
         bLeftGate = true;
      }

      float fGateDist = cFBaller.GetDistanceTo(vGatePos);
      // [mod] la Force agrandit la portée de tir (ForceEffects) ; la dispersion reste calculée sur le rayon de base,
      // donc un tir de loin n'est pas plus précis, il est seulement autorisé
      if (fGateDist > this.ShotRadius(cFBaller)) {
         return null;
      }

      float fAccuracyCoeff = fGateDist / this.m_fGateDistRadius;
      AIFootBaller.Shot cShot = new AIFootBaller.Shot();
      cShot.m_cDirAction = this.GetActionDirForShot(cFBaller, cOppGate);
      cShot.m_vStartPos = cFBaller.GetPosition();
      cShot.m_vDest = new Vector(vGatePos);
      Vector vShotDir = new Vector(cShot.m_vDest);
      vShotDir.Sub(cShot.m_vStartPos);
      cShot.m_vDir = vShotDir;
      float fMaxShotDeviation = this.m_fMaxShotDeviation;
      if (!cFBaller.IsUserControled()) {
         fMaxShotDeviation = 1.0F;
      }

      float fYDeviation = 1.4F;
      if (fAccuracyCoeff < 0.5F) {
         fYDeviation = 2.2F;
      }

      float fDevY = GameObject.RandomFloat(GameField.m_fGateHeight * fAccuracyCoeff * fYDeviation * fMaxShotDeviation);
      cShot.m_vDest.fY += fDevY;
      float fDevX = 0.0F;
      // [mod] la Précision ne réduit que l'écart gauche-droite : la hauteur (donc tir en l'air ou au sol) reste d'origine
      float fDevMulX = ETeams.GetShotDeviationMul(cFBaller.m_nAnimalType);
      if (!cFBaller.IsUserControled()) {
         // [mod] précision de l'IA selon le niveau (difficulty.scr)
         fDevMulX *= ModDifficulty.GetAIShotDeviation();
      }

      fDevX = GameObject.RandomFloat(GameField.m_fGateWidth * fAccuracyCoeff * fMaxShotDeviation * fDevMulX);
      float fXMove = 0.0F;
      Vector vGateDist = new Vector(vGatePos);
      vGateDist.Sub(cShot.m_vStartPos);
      Vector vGateDistNormal = new Vector(vGateDist);
      vGateDistNormal.Normalize();
      float fAngle = 0.0F;
      Vector vCenterToGate = new Vector(vGatePos);
      vCenterToGate.Sub(GameField.cCenter.GetPosition());
      vCenterToGate.Normalize();
      fAngle = vCenterToGate.Dot(vGateDistNormal);
      fAngle = Tools.acos(fAngle);
      cShot.m_fAngle = this.RadianToDegrees(fAngle);
      if (cShot.m_cDirAction == null) {
         return null;
      }

      switch (cShot.m_cDirAction.m_nType) {
         case 2:
            fXMove = 0.0F;
            // [mod] RandomFloat(max) va de 0 à max : sans ça, un tir droit partait toujours du même côté du terrain
            // (méta du 2026-11-27 : 8,5 % de buts d'un côté, 5,1 % de l'autre)
            if (GameObject.RandomFloat(1.0F) < 0.5F) {
               fDevX *= -1.0F;
            }
            break;
         case 3:
            if ((cShot.m_cDirAction.m_nOrient != 1 || !bLeftGate) && (cShot.m_cDirAction.m_nOrient != 0 || bLeftGate)) {
               fXMove = GameField.m_fGateWidth / 4.0F;
            } else {
               fXMove = -GameField.m_fGateWidth / 4.0F;
               fDevX *= -1.0F;
            }
            break;
         case 4:
            if ((cShot.m_cDirAction.m_nOrient != 1 || !bLeftGate) && (cShot.m_cDirAction.m_nOrient != 0 || bLeftGate)) {
               fXMove = GameField.m_fGateWidth / 3.0F;
            } else {
               fXMove = -GameField.m_fGateWidth / 3.0F;
               fDevX *= -1.0F;
            }
            break;
         default:
            return null;
      }

      fXMove += fDevX;
      cShot.m_vDest.fX += fXMove;
      return cShot;
   }

   AIFootBaller GetObstacleFB(AIFootBaller cAIFootBaller, Vector vBegin, Vector vEnd, AIFootBaller cExcludeFB) {
      float fObstacles = 1.0F;
      Vector vDest = new Vector(vEnd);
      vDest.Sub(vBegin);
      Vector vEndNormal = vDest;
      vEndNormal.Normalize();
      FootBaller cMinFB = null;
      float fMinDist = GameField.GetDiagonalLength();

      for (int t = 0; t < 2; t++) {
         Team cTeam = SoccerModule.m_cMatch.m_teams[t];

         for (int i = 0; i < cTeam.m_vFootBallers.size(); i++) {
            FootBaller cFB = (FootBaller)cTeam.m_vFootBallers.elementAt(i);
            if (!cFB.IsGoalKeeper() && cFB != cAIFootBaller && cFB != cExcludeFB && this.IsBetweenPos(cFB.GetPosition(), vBegin, vEnd)) {
               Vector vFBDist = cFB.GetPosition();
               vFBDist.Sub(vBegin);
               float fFBDist = vFBDist.Len();
               Vector vFBDistNormal = new Vector(vFBDist);
               vFBDistNormal.Normalize();
               float fCos = vFBDistNormal.Dot(vEndNormal);
               float fFBAngle = Tools.acos(fCos);
               float fTraceDist = fFBDist * (float)Math.sin(fFBAngle);
               if (fTraceDist < this.m_fAccessRadius && fTraceDist < fMinDist) {
                  fMinDist = fTraceDist;
                  cMinFB = cFB;
               }
            }
         }
      }

      return (AIFootBaller)cMinFB;
   }

   Vector GetNextPoint(AIFootBaller cFB) {
      Vector vNext = cFB.GetRightVector();
      vNext.Normalize();
      float fRnd = GameObject.RandomFloat(100.0F);
      if (fRnd > 50.0F) {
         vNext.Mul(-1.0F);
      }

      vNext.Mul(GetActiveDistance(cFB));
      Vector vNewPos = new Vector();
      cFB.GetPosition().Add(vNext, vNewPos);
      return vNewPos;
   }

   Vector GetOutPoint(int nSide) {
      Vector vCornerPosLeft = null;
      Vector vCornerPosRight = null;
      float fOffsetX;
      if (nSide == 0) {
         vCornerPosLeft = GameField.cCornersTab[0].GetPosition();
         vCornerPosRight = GameField.cCornersTab[1].GetPosition();
         fOffsetX = 100.0F;
      } else {
         vCornerPosLeft = GameField.cCornersTab[2].GetPosition();
         vCornerPosRight = GameField.cCornersTab[3].GetPosition();
         fOffsetX = -100.0F;
      }

      Vector vOutPos = new Vector();
      vOutPos.fX = vCornerPosLeft.fX + fOffsetX;
      vOutPos.fY = vCornerPosLeft.fY;
      vOutPos.fZ = SoccerModule.m_cMatch.m_fZofOut;
      return vOutPos;
   }

   void CalculateOpportunityParams(AIFootBaller cAIFootBaller, Vector vPos, Situation cSituation) {
      Vector vGatePos = null;
      Team cOppTeam = null;
      if (cAIFootBaller.m_nTeam == 0) {
         cOppTeam = SoccerModule.m_cMatch.m_teams[1];
         vGatePos = GameField.cGateRight.GetPosition();
      } else {
         cOppTeam = SoccerModule.m_cMatch.m_teams[0];
         vGatePos = GameField.cGateLeft.GetPosition();
      }

      Vector vGateDist = new Vector(vGatePos);
      vGateDist.Sub(vPos);
      float fGateDist = vGateDist.Len();
      float fAccess = this.CalculateTraceObstacles(cAIFootBaller, vPos, vGatePos, null);
      // [mod] portée de tir selon la Force (ShotRadius)
      float fRadius = this.ShotRadius(cAIFootBaller);
      if (fGateDist > fRadius || fAccess < this.m_fMinPassAcces) {
         fGateDist = 0.0F;
         fAccess = 0.0F;
      }

      fGateDist = 1.0F - fGateDist / fRadius;
      cSituation.m_fGateDistance = fGateDist;
      cSituation.m_fAccess = fAccess;
   }

   Vector GetLotteryPosition(AIFootBaller cFBaller) {
      AIReferee cReferee = SoccerModule.m_cMatch.GetReferee();
      Vector vNext = cReferee.GetRightVector();
      vNext.Normalize();
      if (cFBaller.m_nTeam == 0) {
         vNext.Mul(-1.0F);
      }

      vNext.Mul(GetActiveDistance(cFBaller) * 1.2F);
      Vector vNewPos = new Vector();
      cReferee.GetPosition().Add(vNext, vNewPos);
      return vNewPos;
   }

   Vector CalculatePassPosition(AIFootBaller cFBaller) {
      AIFootBaller cActive = (AIFootBaller)cFBaller.m_cTeam.m_cActiveFootBaller;
      if (cActive != null && cActive != cFBaller) {
         float fDist = cActive.GetDistanceTo(cFBaller);
         Vector vBestDest = null;
         vBestDest = this.ChosePassPosition(cActive, cFBaller);
         Vector fResult = null;
         if (vBestDest != null) {
            fResult = new Vector(vBestDest);
         }

         return fResult;
      } else {
         return null;
      }
   }

   float PassAccessibility(AIFootBaller cFBaller, AIFootBaller cDestFBaller, Vector vDestPos) {
      return this.CalculateTraceObstacles(cFBaller, cFBaller.GetPosition(), vDestPos, cDestFBaller);
   }

   void FixDifficultyLevel(int nLevel) {
      // [mod] niveaux 8..10 : base niveau 7, puis surcharges de difficulty.scr (ModDifficulty.Apply en fin de méthode)
      switch (Math.min(nLevel, 7)) {
         case 1:
            m_fAttackInteria = m_fAttackInteria_level_1;
            m_fFreeBallInteria = m_fFreeBallInteria_level_1;
            this.m_fBallTakenInteria = this.m_fBallTakenInteria_level_1;
            this.m_fFeintChance = this.m_fFeintChance_level_1;
            this.m_fFeintChance_AI = this.m_fFeintChance_level_1_AI;
            this.m_fSuperUpgradeTime = this.m_fSuperUpgradeTime_level_1;
            this.m_fPassChoiceCoef = this.m_fPassChoiceCoef_level_1;
            this.m_fValidPosDeviation = this.m_fValidPosDeviation_level_1;
            this.m_fShotOpportunity = this.m_fShotOpportunity_level_1;
            this.m_fMaxShotDeviation = this.m_fMaxShotDeviation_level_1;
            this.m_fGKNoticeTime = this.m_fGKNoticeTime_level_1;
            break;
         case 2:
            m_fAttackInteria = m_fAttackInteria_level_2;
            m_fFreeBallInteria = m_fFreeBallInteria_level_2;
            this.m_fBallTakenInteria = this.m_fBallTakenInteria_level_2;
            this.m_fFeintChance = this.m_fFeintChance_level_2;
            this.m_fFeintChance_AI = this.m_fFeintChance_level_2_AI;
            this.m_fSuperUpgradeTime = this.m_fSuperUpgradeTime_level_2;
            this.m_fPassChoiceCoef = this.m_fPassChoiceCoef_level_2;
            this.m_fValidPosDeviation = this.m_fValidPosDeviation_level_2;
            this.m_fShotOpportunity = this.m_fShotOpportunity_level_2;
            this.m_fMaxShotDeviation = this.m_fMaxShotDeviation_level_2;
            this.m_fGKNoticeTime = this.m_fGKNoticeTime_level_2;
            break;
         case 3:
            m_fAttackInteria = m_fAttackInteria_level_3;
            m_fFreeBallInteria = m_fFreeBallInteria_level_3;
            this.m_fBallTakenInteria = this.m_fBallTakenInteria_level_3;
            this.m_fFeintChance = this.m_fFeintChance_level_3;
            this.m_fFeintChance_AI = this.m_fFeintChance_level_3_AI;
            this.m_fSuperUpgradeTime = this.m_fSuperUpgradeTime_level_3;
            this.m_fPassChoiceCoef = this.m_fPassChoiceCoef_level_3;
            this.m_fValidPosDeviation = this.m_fValidPosDeviation_level_3;
            this.m_fShotOpportunity = this.m_fShotOpportunity_level_3;
            this.m_fMaxShotDeviation = this.m_fMaxShotDeviation_level_3;
            this.m_fGKNoticeTime = this.m_fGKNoticeTime_level_3;
            break;
         case 4:
            m_fAttackInteria = m_fAttackInteria_level_4;
            m_fFreeBallInteria = m_fFreeBallInteria_level_4;
            this.m_fBallTakenInteria = this.m_fBallTakenInteria_level_4;
            this.m_fFeintChance = this.m_fFeintChance_level_4;
            this.m_fFeintChance_AI = this.m_fFeintChance_level_4_AI;
            this.m_fSuperUpgradeTime = this.m_fSuperUpgradeTime_level_4;
            this.m_fPassChoiceCoef = this.m_fPassChoiceCoef_level_4;
            this.m_fValidPosDeviation = this.m_fValidPosDeviation_level_4;
            this.m_fShotOpportunity = this.m_fShotOpportunity_level_4;
            this.m_fMaxShotDeviation = this.m_fMaxShotDeviation_level_4;
            this.m_fGKNoticeTime = this.m_fGKNoticeTime_level_4;
            break;
         case 5:
            m_fAttackInteria = m_fAttackInteria_level_5;
            m_fFreeBallInteria = m_fFreeBallInteria_level_5;
            this.m_fBallTakenInteria = this.m_fBallTakenInteria_level_5;
            this.m_fFeintChance = this.m_fFeintChance_level_5;
            this.m_fFeintChance_AI = this.m_fFeintChance_level_5_AI;
            this.m_fSuperUpgradeTime = this.m_fSuperUpgradeTime_level_5;
            this.m_fPassChoiceCoef = this.m_fPassChoiceCoef_level_5;
            this.m_fValidPosDeviation = this.m_fValidPosDeviation_level_5;
            this.m_fShotOpportunity = this.m_fShotOpportunity_level_5;
            this.m_fMaxShotDeviation = this.m_fMaxShotDeviation_level_5;
            this.m_fGKNoticeTime = this.m_fGKNoticeTime_level_5;
            break;
         case 6:
            m_fAttackInteria = m_fAttackInteria_level_6;
            m_fFreeBallInteria = m_fFreeBallInteria_level_6;
            this.m_fBallTakenInteria = this.m_fBallTakenInteria_level_6;
            this.m_fFeintChance = this.m_fFeintChance_level_6;
            this.m_fFeintChance_AI = this.m_fFeintChance_level_6_AI;
            this.m_fSuperUpgradeTime = this.m_fSuperUpgradeTime_level_6;
            this.m_fPassChoiceCoef = this.m_fPassChoiceCoef_level_6;
            this.m_fValidPosDeviation = this.m_fValidPosDeviation_level_6;
            this.m_fShotOpportunity = this.m_fShotOpportunity_level_6;
            this.m_fMaxShotDeviation = this.m_fMaxShotDeviation_level_6;
            this.m_fGKNoticeTime = this.m_fGKNoticeTime_level_6;
            break;
         case 7:
            m_fAttackInteria = m_fAttackInteria_level_7;
            m_fFreeBallInteria = m_fFreeBallInteria_level_7;
            this.m_fBallTakenInteria = this.m_fBallTakenInteria_level_7;
            this.m_fFeintChance = this.m_fFeintChance_level_7;
            this.m_fFeintChance_AI = this.m_fFeintChance_level_7_AI;
            this.m_fSuperUpgradeTime = this.m_fSuperUpgradeTime_level_7;
            this.m_fPassChoiceCoef = this.m_fPassChoiceCoef_level_7;
            this.m_fValidPosDeviation = this.m_fValidPosDeviation_level_7;
            this.m_fShotOpportunity = this.m_fShotOpportunity_level_7;
            this.m_fMaxShotDeviation = this.m_fMaxShotDeviation_level_7;
            this.m_fGKNoticeTime = this.m_fGKNoticeTime_level_7;
      }

      ModDifficulty.Apply(this, nLevel);
      // [mod] jauge SUPER plus lente (data/supers.scr, FillTime), pour les deux équipes
      ModSupers.Load();
      if (ModSupers.m_fFillTime > 0.0F) {
         this.m_fSuperUpgradeTime = ModSupers.m_fFillTime;
      }

      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("[mod] jauge SUPER : " + this.m_fSuperUpgradeTime + " s de possession par lettre (niveau " + nLevel + ")\n");
      }
   }

   void Load(ParseKeyword key) {
      GameObject.LogLine(" LADUJE STRATEGY");

      for (ParseKeyword paramKey = key.GetChild(); paramKey != null; paramKey = paramKey.GetNext()) {
         if (paramKey.Equals("pS")) {
            this.m_f_pS = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("pSw")) {
            this.m_f_pSw = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("pSp")) {
            this.m_f_pSp = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("pS_gk")) {
            this.m_f_pS_gk = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("pSw_gk")) {
            this.m_f_pSw_gk = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("pSp_gk")) {
            this.m_f_pSp_gk = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("pGateDistance")) {
            this.m_f_pGateDistance = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("pAccess")) {
            this.m_f_pAccess = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("pAngle")) {
            this.m_f_pAngle = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("AIThink")) {
            if (paramKey.GetIParam(0) == 1) {
               this.m_bAIThink = true;
            } else {
               this.m_bAIThink = false;
            }
         }

         if (paramKey.Equals("MaxGoalChangeDiff")) {
            this.m_fMaxGoalChangeDiff = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MaxSituationAnotherPlayerDiff")) {
            this.m_fMaxSituationAnotherPlayerDiff = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MaxSituationAnotherPlaceDiff")) {
            this.m_fMaxSituationAnotherPlaceDiff = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MaxSituationDiff")) {
            this.m_fMaxSituationDiff = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MaxValidPosDist")) {
            this.m_fMaxValidPosDist = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MaxValidPosDistGK")) {
            this.m_fMaxValidPosDistGK = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MinChangeValidPosDiff")) {
            this.m_fMinChangeValidPosDiff = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MinChangeValidPosDiffGK")) {
            this.m_fMinChangeValidPosDiffGK = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ValidPosTolerance")) {
            this.m_fValidPosTolerance = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ValidPosToleranceGK")) {
            this.m_fValidPosToleranceGK = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MinDefenderValidPosScope")) {
            this.m_fMinDefenderValidPosScope = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MaxDefenderValidPosScope")) {
            this.m_fMaxDefenderValidPosScope = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MinValidPosScope")) {
            this.m_fMinValidPosScope = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MaxValidPosScope")) {
            this.m_fMaxValidPosScope = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MinGoalsDiff")) {
            this.m_fMinGoalsDiff = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MinDistFBallers")) {
            GameObject.LogLine(" Zaladowalem DISTANECE ");
            this.m_fMinDistFBallers = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ActiveDistForGorilla")) {
            this.m_fActiveDistForGorilla = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("AutoTakeBall")) {
            if (paramKey.GetIParam(0) == 1) {
               m_bAutoTakeBall = true;
            } else {
               m_bAutoTakeBall = false;
            }
         }

         if (paramKey.Equals("RestoreDistance")) {
            this.m_fRestoreDistance = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("DestPosTolerance")) {
            this.m_fDestPosTolerance = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("LookingTime")) {
            this.m_fLookingTime = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FreeTime")) {
            this.m_fFreeTime = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MinDistToChangeActive")) {
            this.m_fMinDistToChangeActive = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("AttackDestChange")) {
            this.m_fAttackDestChange = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("AttackDestChange")) {
            this.m_fAttackDestChange = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MinForwardToNear")) {
            this.m_fMinForwardToNear = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("GoalKeeperDistGate")) {
            this.m_fGoalKeeperDistGate = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("GoalKeeperStrafeSpeed")) {
            this.m_fGoalKeeperStrafeSpeed = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MinDistToChangeFocus")) {
            this.m_fMinDistToChangeFocus = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FocusInteria")) {
            this.m_fFocusInteria = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FreeBallCapture")) {
            this.m_fFreeBallCapture = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("GKChangeRunToValidPos")) {
            this.m_fGKChangeRunToValidPos = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FreedomRadius")) {
            this.m_fFreedomRadius = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("AccessRadius")) {
            this.m_fAccessRadius = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("AccesDistInfluence")) {
            this.m_fAccesDistInfluence = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MinPassAcces")) {
            this.m_fMinPassAcces = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MinPassForwardDist")) {
            this.m_fMinPassForwardDist = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("PassChanceTime")) {
            this.m_fPassChanceTime = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("GateDistRadius")) {
            this.m_fGateDistRadius = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FreedomBackwardDec")) {
            this.m_fFreedomBackwardInc = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ForwardToOppAngle")) {
            this.m_fForwardToOppAngle = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FeintDist")) {
            this.m_fFeintDist = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("LeftUserControl")) {
            if (paramKey.GetIParam(0) == 1) {
               this.m_bLeftUserControl = true;
            } else {
               this.m_bLeftUserControl = false;
            }
         }

         if (paramKey.Equals("RightUserControl")) {
            if (paramKey.GetIParam(0) == 1) {
               this.m_bRightUserControl = true;
            } else {
               this.m_bRightUserControl = false;
            }
         }

         if (paramKey.Equals("NewPosDist")) {
            this.m_fNewPosDist = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FeintChance_GK")) {
            this.m_fFeintChance_GK = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("SpeedWithBall")) {
            this.m_fSpeedWithBall = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("PassPrecision")) {
            m_fPassPrecision = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("PerfectPrecisionDist")) {
            this.m_fPerfectPrecisionDist = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("DownDefence")) {
            this.m_fDownDefence = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("UpDefence")) {
            this.m_fUpDefence = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ThrowDefence")) {
            this.m_fThrowDefence = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ThrowScope")) {
            this.m_fThrowScope = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("UpScope")) {
            this.m_fUpScope = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("DefenceDelayDiff")) {
            this.m_fDefenceDelayDiff = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("DefenceWithoutDelay")) {
            this.m_fDefenceWithoutDelay = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("SituationCalcGrain")) {
            this.m_fSituationCalcGrain = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ReceiveRadius")) {
            this.m_fReceiveRadius = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("RecieveScopeUp")) {
            this.m_fRecieveScopeUp = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ReceiveDown")) {
            this.m_fReceiveDown = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ReceiveBrust")) {
            this.m_fReceiveBrust = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ReceiveHead")) {
            this.m_fReceiveHead = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("KickForwardByHead")) {
            this.m_fKickForwardByHead = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MaxShotByHeadDist")) {
            this.m_fMaxShotByHeadDist = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("HeadKeyDistance")) {
            this.m_fHeadKeyDistance = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("AttackKickStength")) {
            this.m_fAttackKickStength = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MaxPrecisionDist")) {
            this.m_fMaxPrecisionDist = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("BlockControlTime")) {
            this.m_fBlockControlTime = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("AIforFocusTime")) {
            this.m_fAIforFocusTime = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ShortPassSpeed")) {
            m_fShortPassSpeed = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MiddlePassSpeed")) {
            m_fMiddlePassSpeed = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("LongPassSpeed")) {
            m_fLongPassSpeed = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("LastTimeKey_A")) {
            this.m_fLastTimeKey_A = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MaxPassDirAngle")) {
            this.m_fMaxPassDirAngle = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MaxValidReferee")) {
            this.m_fMaxValidReferee = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("SpecialBallScope")) {
            m_fSpecialBallScope = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FBSpeed")) {
            m_fFBSpeed = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ShortPassDelay")) {
            m_fShortPassDelay = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MiddlePassDelay")) {
            m_fMiddlePassDelay = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("LongPassDelay")) {
            m_fLongPassDelay = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("GKOutDist")) {
            this.m_fGKOutDist = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("SuperDefenceTime")) {
            this.m_fSuperDefenceTime = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("SuperFeintTime")) {
            this.m_fSuperFeintTime = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("SuperSpeed")) {
            this.m_fSuperSpeed = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("SuperSpeedTime")) {
            this.m_fSuperSpeedTime = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("SuperShotTime")) {
            m_fSuperShotTime = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("Turn_left")) {
            this.m_nRight_l = paramKey.GetIParam(0);
            this.m_nLeft_l = paramKey.GetIParam(1);
            this.m_nUp_l = paramKey.GetIParam(2);
            this.m_nDown_l = paramKey.GetIParam(3);
         }

         if (paramKey.Equals("Turn_right")) {
            this.m_nRight_r = paramKey.GetIParam(0);
            this.m_nLeft_r = paramKey.GetIParam(1);
            this.m_nUp_r = paramKey.GetIParam(2);
            this.m_nDown_r = paramKey.GetIParam(3);
         }

         if (paramKey.Equals("Pass_l")) {
            this.m_nPass_l = paramKey.GetIParam(0);
         }

         if (paramKey.Equals("Shot_l")) {
            this.m_nShot_l = paramKey.GetIParam(0);
         }

         if (paramKey.Equals("Feint_l")) {
            this.m_nFeint_l = paramKey.GetIParam(0);
         }

         if (paramKey.Equals("Super_l")) {
            this.m_nSuper_l = paramKey.GetIParam(0);
         }

         if (paramKey.Equals("Pass_r")) {
            this.m_nPass_r = paramKey.GetIParam(0);
         }

         if (paramKey.Equals("Shot_r")) {
            this.m_nShot_r = paramKey.GetIParam(0);
         }

         if (paramKey.Equals("Feint_r")) {
            this.m_nFeint_r = paramKey.GetIParam(0);
         }

         if (paramKey.Equals("Super_r")) {
            this.m_nSuper_r = paramKey.GetIParam(0);
         }

         if (paramKey.Equals("FadingTime")) {
            this.m_fFadingTime = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("AttackInteria_level_1")) {
            m_fAttackInteria_level_1 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("AttackInteria_level_2")) {
            m_fAttackInteria_level_2 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("AttackInteria_level_3")) {
            m_fAttackInteria_level_3 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("AttackInteria_level_4")) {
            m_fAttackInteria_level_4 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("AttackInteria_level_5")) {
            m_fAttackInteria_level_5 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("AttackInteria_level_6")) {
            m_fAttackInteria_level_6 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("AttackInteria_level_7")) {
            m_fAttackInteria_level_7 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FreeBallInteria_level_1")) {
            m_fFreeBallInteria_level_1 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FreeBallInteria_level_2")) {
            m_fFreeBallInteria_level_2 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FreeBallInteria_level_3")) {
            m_fFreeBallInteria_level_3 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FreeBallInteria_level_4")) {
            m_fFreeBallInteria_level_4 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FreeBallInteria_level_5")) {
            m_fFreeBallInteria_level_5 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FreeBallInteria_level_6")) {
            m_fFreeBallInteria_level_6 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FreeBallInteria_level_7")) {
            m_fFreeBallInteria_level_7 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("BallTakenInteria_level_1")) {
            this.m_fBallTakenInteria_level_1 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("BallTakenInteria_level_2")) {
            this.m_fBallTakenInteria_level_2 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("BallTakenInteria_level_3")) {
            this.m_fBallTakenInteria_level_3 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("BallTakenInteria_level_4")) {
            this.m_fBallTakenInteria_level_4 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("BallTakenInteria_level_5")) {
            this.m_fBallTakenInteria_level_5 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("BallTakenInteria_level_6")) {
            this.m_fBallTakenInteria_level_6 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("BallTakenInteria_level_7")) {
            this.m_fBallTakenInteria_level_7 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FeintChance_level_1")) {
            this.m_fFeintChance_level_1 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FeintChance_level_2")) {
            this.m_fFeintChance_level_2 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FeintChance_level_3")) {
            this.m_fFeintChance_level_3 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FeintChance_level_4")) {
            this.m_fFeintChance_level_4 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FeintChance_level_5")) {
            this.m_fFeintChance_level_5 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FeintChance_level_6")) {
            this.m_fFeintChance_level_6 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FeintChance_level_7")) {
            this.m_fFeintChance_level_7 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FeintChance_level_1_AI")) {
            this.m_fFeintChance_level_1_AI = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FeintChance_level_2_AI")) {
            this.m_fFeintChance_level_2_AI = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FeintChance_level_3_AI")) {
            this.m_fFeintChance_level_3_AI = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FeintChance_level_4_AI")) {
            this.m_fFeintChance_level_4_AI = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FeintChance_level_5_AI")) {
            this.m_fFeintChance_level_5_AI = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FeintChance_level_6_AI")) {
            this.m_fFeintChance_level_6_AI = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("FeintChance_level_7_AI")) {
            this.m_fFeintChance_level_7_AI = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("SuperUpgradeTime_level_1")) {
            this.m_fSuperUpgradeTime_level_1 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("SuperUpgradeTime_level_2")) {
            this.m_fSuperUpgradeTime_level_2 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("SuperUpgradeTime_level_3")) {
            this.m_fSuperUpgradeTime_level_3 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("SuperUpgradeTime_level_4")) {
            this.m_fSuperUpgradeTime_level_4 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("SuperUpgradeTime_level_5")) {
            this.m_fSuperUpgradeTime_level_5 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("SuperUpgradeTime_level_6")) {
            this.m_fSuperUpgradeTime_level_6 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("SuperUpgradeTime_level_7")) {
            this.m_fSuperUpgradeTime_level_7 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("PassChoiceCoef_level_1")) {
            this.m_fPassChoiceCoef_level_1 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("PassChoiceCoef_level_2")) {
            this.m_fPassChoiceCoef_level_2 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("PassChoiceCoef_level_3")) {
            this.m_fPassChoiceCoef_level_3 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("PassChoiceCoef_level_4")) {
            this.m_fPassChoiceCoef_level_4 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("PassChoiceCoef_level_5")) {
            this.m_fPassChoiceCoef_level_5 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("PassChoiceCoef_level_6")) {
            this.m_fPassChoiceCoef_level_6 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("PassChoiceCoef_level_7")) {
            this.m_fPassChoiceCoef_level_7 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ValidPosDeviation_level_1")) {
            this.m_fValidPosDeviation_level_1 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ValidPosDeviation_level_2")) {
            this.m_fValidPosDeviation_level_2 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ValidPosDeviation_level_3")) {
            this.m_fValidPosDeviation_level_3 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ValidPosDeviation_level_4")) {
            this.m_fValidPosDeviation_level_4 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ValidPosDeviation_level_5")) {
            this.m_fValidPosDeviation_level_5 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ValidPosDeviation_level_6")) {
            this.m_fValidPosDeviation_level_6 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ValidPosDeviation_level_7")) {
            this.m_fValidPosDeviation_level_7 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ShotOpportunity_level_1")) {
            this.m_fShotOpportunity_level_1 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ShotOpportunity_level_2")) {
            this.m_fShotOpportunity_level_2 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ShotOpportunity_level_3")) {
            this.m_fShotOpportunity_level_3 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ShotOpportunity_level_4")) {
            this.m_fShotOpportunity_level_4 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ShotOpportunity_level_5")) {
            this.m_fShotOpportunity_level_5 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ShotOpportunity_level_6")) {
            this.m_fShotOpportunity_level_6 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("ShotOpportunity_level_7")) {
            this.m_fShotOpportunity_level_7 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MaxShotDeviation_level_1")) {
            this.m_fMaxShotDeviation_level_1 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MaxShotDeviation_level_2")) {
            this.m_fMaxShotDeviation_level_2 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MaxShotDeviation_level_3")) {
            this.m_fMaxShotDeviation_level_3 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MaxShotDeviation_level_4")) {
            this.m_fMaxShotDeviation_level_4 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MaxShotDeviation_level_5")) {
            this.m_fMaxShotDeviation_level_5 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MaxShotDeviation_level_6")) {
            this.m_fMaxShotDeviation_level_6 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("MaxShotDeviation_level_7")) {
            this.m_fMaxShotDeviation_level_7 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("GKNoticeTime_level_1")) {
            this.m_fGKNoticeTime_level_1 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("GKNoticeTime_level_2")) {
            this.m_fGKNoticeTime_level_2 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("GKNoticeTime_level_3")) {
            this.m_fGKNoticeTime_level_3 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("GKNoticeTime_level_4")) {
            this.m_fGKNoticeTime_level_4 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("GKNoticeTime_level_5")) {
            this.m_fGKNoticeTime_level_5 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("GKNoticeTime_level_6")) {
            this.m_fGKNoticeTime_level_6 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("GKNoticeTime_level_7")) {
            this.m_fGKNoticeTime_level_7 = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("BreakSpecialBallTime")) {
            this.m_fBreakSpecialBallTime = paramKey.GetFParam(0);
         }

         if (paramKey.Equals("StabilizationWithDelay")) {
            this.m_fStabilizationWithDelay = paramKey.GetFParam(0);
         }
      }
   }

   Vector GetOpponentGatePos(FootBaller cFBaller) {
      return cFBaller.m_nTeam == 0 ? GameField.cGateRight.GetPosition() : GameField.cGateLeft.GetPosition();
   }

   boolean IsRightToPos(FootBaller cFBaller, Vector vPos) {
      Vector vDist = new Vector(vPos);
      vDist.Sub(cFBaller.GetPosition());
      vDist.fY = 0.0F;
      vDist.Normalize();
      Vector vRight = cFBaller.GetRightVector();
      vRight.fY = 0.0F;
      vRight.Normalize();
      vRight.Mul(-1.0F);
      float fAngle = vDist.Dot(vRight);
      fAngle = Tools.acos(fAngle);
      fAngle = this.RadianToDegrees(fAngle);
      return fAngle < 90.0F;
   }

   float OpponentGrabChance(float fD, float fDG) {
      return 0.0F;
   }

   AIFootBaller GetFBActiveDistance(FootBaller cFBaller) {
      Team cOppTeam;
      if (cFBaller.m_nTeam == 0) {
         cOppTeam = SoccerModule.m_cMatch.m_teams[1];
      } else {
         cOppTeam = SoccerModule.m_cMatch.m_teams[0];
      }

      AIFootBaller cActiveOpp = (AIFootBaller)cOppTeam.m_cActiveFootBaller;
      if (cActiveOpp == null) {
         return null;
      }

      float fDistance = cFBaller.GetDistanceTo(cActiveOpp);
      float fScope = GetActiveDistance((AIFootBaller)cFBaller, cActiveOpp);
      return fDistance < fScope ? cActiveOpp : null;
   }

   boolean InDistance(Vector vFirst, Vector vSec) {
      return vFirst.Distance(vSec) < this.m_fMinDistFBallers;
   }

   boolean CanBeChangeActive(FootBaller cFActive, FootBaller cFNearest, boolean bAttacker) {
      if (bAttacker) {
         Vector vBallPos = GameField.m_cBall.GetPosition();
         if (!this.IsForwardFromPos(cFActive, vBallPos)
            && this.IsForwardFromPos(cFNearest, vBallPos)
            && Math.abs(cFActive.GetDistanceTo(vBallPos) / cFNearest.GetDistanceTo(vBallPos)) < this.m_fMinForwardToNear) {
            return true;
         }
      }

      float fDistActive = cFActive.GetDistanceTo(GameField.m_cBall);
      float fDistNearest = cFNearest.GetDistanceTo(GameField.m_cBall);
      return Math.abs(fDistActive - fDistNearest) > this.m_fMinDistToChangeActive;
   }

   boolean CanBeChangeAttacker(AIFootBaller cAttacker) {
      if (cAttacker == null) {
         return false;
      } else {
         AIFootBaller cActiveFB = cAttacker.m_cTeam.GetActiveFB();
         if (cActiveFB == null) {
            return true;
         } else {
            return cAttacker.IsHelpAttacker() ? false : !cAttacker.IsUserControled() || !((AITeam)cAttacker.m_cTeam).IsBlockControl();
         }
      }
   }

   FootBaller.DirAction GetActionDirForShot(AIFootBaller cFBaller, ControlObject cOppGate) {
      Vector vShotDir;
      if (cFBaller.IsUserControled()) {
         vShotDir = cFBaller.m_cTeam.GetTeamController().GetFieldDirFromKeys();
         if (vShotDir == null) {
            vShotDir = cFBaller.GetForwardVector();
         }
      } else {
         vShotDir = cFBaller.GetForwardVector();
      }

      Vector vDir = cOppGate.GetPosition();
      vDir.Sub(GameField.cCenter.GetPositionVectorVolatile());
      vDir.Normalize();
      Vector vDestDir = new Vector(vDir);
      float fAngle = vDestDir.Dot(vShotDir);
      fAngle = Tools.acos(fAngle);
      fAngle = (float)(fAngle * 180.0F / Math.PI);
      Vector vNormal = vShotDir.Cross(vDir);
      if (vNormal.fY > 0.0F) {
         fAngle = -fAngle;
      }

      return this.SelectDirAction(vDir, fAngle, true);
   }

   Vector GetStrafeDir(FootBaller cFBaller, Vector vDir) {
      Vector vResult = new Vector(vDir);
      vResult.Sub(cFBaller.GetPosition());
      vResult.Normalize();
      vResult.Mul(this.m_fGoalKeeperStrafeSpeed);
      return vResult;
   }

   Vector GetBackDir(FootBaller cFBaller, Vector vDir) {
      Vector vResult = new Vector(vDir);
      vResult.Sub(cFBaller.GetPosition());
      vResult.Normalize();
      vResult.Mul(this.m_fSpeedWithBall);
      return vResult;
   }

   FootBaller.DirAction SelectDirAction(Vector vDir, float fAngle, boolean bPartition) {
      int nOrient = -1;
      int nType = -1;
      float fRealAngle = fAngle;
      if (bPartition) {
         if (fAngle < 0.0F) {
            nOrient = 0;
            fAngle = -fAngle;
         } else {
            nOrient = 1;
         }

         if (fAngle >= 0.0F && fAngle < 23.0F) {
            nType = 2;
         }

         if (fAngle >= 23.0F && fAngle < 68.0F) {
            nType = 3;
         }

         if (fAngle >= 68.0F && fAngle < 113.0F) {
            nType = 4;
         }

         if (fAngle >= 113.0F && fAngle < 158.0F) {
            nType = 5;
         }

         if (fAngle >= 158.0F && fAngle <= 180.0F) {
            nType = 6;
         }
      }

      return new FootBaller.DirAction(nOrient, nType, fRealAngle, vDir, bPartition, 1);
   }

   Vector ChoseBestPosition(AIFootBaller cFBaller, float fNewPosDist, Vector vStartPos) {
      Vector vRight = null;
      Vector vForward = null;
      Vector vLeft = null;
      Vector vBack = null;
      Vector vForwardNormal = cFBaller.GetForwardVector();
      vForwardNormal.Normalize();
      Vector vRightNormal = cFBaller.GetRightVector();
      vRightNormal.Normalize();
      Vector vPosition = cFBaller.GetPosition();
      vRight = new Vector(vRightNormal);
      vRight.Mul(fNewPosDist);
      vRight.Add(vPosition, cFBaller.m_vRight);
      vLeft = new Vector(vRightNormal);
      vLeft.Mul(-fNewPosDist);
      vLeft.Add(vPosition, cFBaller.m_vLeft);
      vForward = new Vector(vForwardNormal);
      vForward.Mul(fNewPosDist);
      vForward.Add(vPosition, cFBaller.m_vForward);
      vBack = new Vector(vForward);
      vBack.Mul(-1.0F);
      vBack.Add(vPosition, cFBaller.m_vBack);
      float fMaxSituation = -1.0F;
      Vector vBestPos = null;
      float vPosZ = vPosition.fZ;
      int nZone = cFBaller.GetZone();
      boolean bLeftSide = false;
      if (cFBaller.m_nTeam == 0) {
         bLeftSide = true;
      }

      if (bLeftSide && cFBaller.m_vForward.fZ > vPosZ || !bLeftSide && cFBaller.m_vForward.fZ < vPosZ || nZone == 2) {
         fMaxSituation = cFBaller.GetSituation(cFBaller.m_vForward);
         vBestPos = cFBaller.m_vForward;
      }

      if (bLeftSide && cFBaller.m_vLeft.fZ > vPosZ || !bLeftSide && cFBaller.m_vLeft.fZ < vPosZ || nZone == 2) {
         float fSituation = cFBaller.GetSituation(cFBaller.m_vLeft);
         if (fSituation > fMaxSituation) {
            fMaxSituation = fSituation;
            vBestPos = cFBaller.m_vLeft;
         }
      }

      if (bLeftSide && cFBaller.m_vRight.fZ > vPosZ || !bLeftSide && cFBaller.m_vRight.fZ < vPosZ || nZone == 2) {
         float var21 = cFBaller.GetSituation(cFBaller.m_vRight);
         if (var21 > fMaxSituation) {
            fMaxSituation = var21;
            vBestPos = cFBaller.m_vRight;
         }
      }

      if (bLeftSide && cFBaller.m_vBack.fZ > vPosZ || !bLeftSide && cFBaller.m_vBack.fZ < vPosZ || nZone == 2) {
         float var22 = cFBaller.GetSituation(cFBaller.m_vBack);
         if (var22 > fMaxSituation) {
            vBestPos = cFBaller.m_vBack;
         }
      }

      return vBestPos;
   }

   Vector GetCornerPoint(int nSide) {
      Vector vCornerPosLeft = null;
      Vector vCornerPosRight = null;
      Vector vOff = null;
      if (nSide == 2) {
         vCornerPosLeft = GameField.cCornersTab[0].GetPosition();
         vCornerPosRight = GameField.cCornersTab[2].GetPosition();
      } else {
         vCornerPosLeft = GameField.cCornersTab[1].GetPosition();
         vCornerPosRight = GameField.cCornersTab[3].GetPosition();
      }

      Vector vBallPos = GameField.m_cBall.GetPosition();
      float fLeftDist = vCornerPosLeft.Distance(vBallPos);
      float fRightDist = vCornerPosRight.Distance(vBallPos);
      Vector vCorner;
      if (fLeftDist < fRightDist) {
         vCorner = vCornerPosLeft;
         vCorner.fX -= 50.0F;
      } else {
         vCorner = vCornerPosRight;
         vCorner.fX += 50.0F;
      }

      float fZOff = 100.0F;
      if (SoccerModule.m_cMatch.m_cSpecialFB != null) {
         AIFootBaller cFB = SoccerModule.m_cMatch.m_cSpecialFB;
         if (cFB.m_nAnimalType != 1 && cFB.m_nAnimalType != 3) {
            fZOff = 50.0F;
         }
      }

      if (nSide == 2) {
         vCorner.fZ -= fZOff;
      } else {
         vCorner.fZ += fZOff;
      }

      return vCorner;
   }

   Vector TuneValidScope(AIFootBaller cFBaller, Vector vValidPos) {
      Vector vStartPos = cFBaller.cSpawnPoint.GetPosition();
      if (cFBaller.IsReferee()) {
         Vector vTunePos = new Vector(vValidPos);
         vTunePos.fX = vStartPos.fX;
         boolean bLeft;
         if (vValidPos.fZ - vStartPos.fZ > 0.0F) {
            bLeft = false;
         } else {
            bLeft = true;
         }

         if (Math.abs(vTunePos.fZ - vStartPos.fZ) > this.m_fMaxValidReferee) {
            if (bLeft) {
               vTunePos.fZ = vStartPos.fZ - this.m_fMaxValidReferee;
            } else {
               vTunePos.fZ = vStartPos.fZ + this.m_fMaxValidReferee;
            }
         }

         return vTunePos;
      } else {
         int nZone = GameField.GetZoneForPoint(GameField.m_cBall.GetPositionVectorVolatile(), cFBaller.m_nTeam);
         int nFBType = cFBaller.m_nType;
         float fMin;
         float fMax;
         if (nFBType == 1) {
            fMin = this.m_fMinDefenderValidPosScope;
            fMax = this.m_fMaxDefenderValidPosScope;
            if (AIMatch.IsMatchState(0) && !cFBaller.m_cTeam.IsPossesor() && nZone != 2) {
               fMax = 200.0F;
            }
         } else {
            fMin = this.m_fMinValidPosScope;
            fMax = this.m_fMaxValidPosScope;
         }

         if (AIMatch.IsMatchState(0) && !cFBaller.m_cTeam.IsPossesor() && nZone != 2) {
            fMax = 200.0F;
         }

         if ((AIMatch.IsMatchState(5) || AIMatch.IsMatchState(4))
            && SoccerModule.m_cMatch.m_nTeamToStart != cFBaller.m_nTeam
            && (nFBType == 1 || nFBType == 0 && nZone == 2)) {
            fMax = 500.0F;
         }

         boolean bLeft;
         if (cFBaller.m_nTeam == 0) {
            bLeft = true;
         } else {
            bLeft = false;
         }

         boolean bForward;
         if (bLeft) {
            if (vStartPos.fZ < vValidPos.fZ) {
               bForward = true;
            } else {
               bForward = false;
            }
         } else if (vStartPos.fZ > vValidPos.fZ) {
            bForward = true;
         } else {
            bForward = false;
         }

         Vector vResult = new Vector(vValidPos);
         if (bForward) {
            if (Math.abs(vValidPos.fZ - vStartPos.fZ) > fMax) {
               if (bLeft) {
                  vResult.fZ = vStartPos.fZ + fMax;
               } else {
                  vResult.fZ = vStartPos.fZ - fMax;
               }
            }
         } else if (Math.abs(vValidPos.fZ - vStartPos.fZ) > fMin) {
            if (bLeft) {
               vResult.fZ = vStartPos.fZ - fMin;
            } else {
               vResult.fZ = vStartPos.fZ + fMin;
            }
         }

         return vResult;
      }
   }

   void SetDirForwardField(AIFootBaller cFBaller) {
      Vector vDir;
      if (cFBaller.m_nTeam == 0) {
         vDir = GameField._DIR_E;
      } else {
         vDir = GameField._DIR_W;
      }

      cFBaller.ChangeDirectionLogic(this.GetActionDir(cFBaller, vDir, false));
   }

   float GetForwardDistDiff(FootBaller cFirst, FootBaller cSec) {
      return this.GetOppGateDistance(cFirst) - this.GetOppGateDistance(cSec);
   }

   Vector CalculateForwardPos(FootBaller cFBaller) {
      Vector vForwardPos = cFBaller.GetPosition();
      switch (cFBaller.GetZone()) {
         case 0:
            vForwardPos.fZ = cFBaller.aPatrolPath[1].GetPosition().fZ;
            break;
         case 1:
            vForwardPos.fZ = cFBaller.aPatrolPath[2].GetPosition().fZ;
            break;
         case 2:
            vForwardPos.fZ = cFBaller.aPatrolPath[3].GetPosition().fZ;
      }

      return vForwardPos;
   }

   boolean IsForwardFromPos(FootBaller cFBaller, Vector vBallPos) {
      Vector vGatePos = null;
      if (cFBaller.m_nTeam == 0) {
         vGatePos = GameField.cGateLeft.GetPosition();
      } else {
         vGatePos = GameField.cGateRight.GetPosition();
      }

      return this.IsBetweenPos(cFBaller.GetPosition(), vBallPos, vGatePos);
   }

   Vector GetDestFeintPos(AIFootBaller cFBaller, AIFootBaller cOpponent) {
      Vector vDist = cOpponent.GetPosition();
      vDist.Sub(cFBaller.GetPosition());
      float fDist = vDist.Len();
      vDist.Normalize();
      vDist.Mul(GetActiveDistance(cFBaller, cOpponent));
      Vector vResult = new Vector();
      vDist.Add(cOpponent.GetPosition(), vResult);
      return vResult;
   }

   Vector GetSpecialBallFBPos(AIFootBaller cFBaller, int nSide) {
      Vector vPos = null;
      if (AIMatch.IsMatchState(5)) {
         vPos = this.GetOutPoint(nSide);
         if (nSide == 0) {
            vPos.fX += 200.0F;
         } else {
            vPos.fX -= 200.0F;
         }
      }

      if (AIMatch.IsMatchState(4)) {
         vPos = this.GetCornerPoint(nSide);
         if (cFBaller.m_nTeam == 0) {
            vPos.fZ += 250.0F;
         } else {
            vPos.fZ -= 250.0F;
         }
      }

      return vPos;
   }

   Vector GetStartGameDestPos(AIFootBaller cFBaller) {
      Vector vTopCornerPos = null;
      Vector vBottomCornerPos = null;
      if (cFBaller.m_nTeam == 0) {
         vTopCornerPos = GameField.cCornersTab[0].GetPosition();
         vBottomCornerPos = GameField.cCornersTab[2].GetPosition();
      } else {
         vTopCornerPos = GameField.cCornersTab[1].GetPosition();
         vBottomCornerPos = GameField.cCornersTab[3].GetPosition();
      }

      Vector vLineTop = new Vector(vTopCornerPos);
      Vector vLineBottom = new Vector(vBottomCornerPos);
      vLineTop.Sub(GameField.cCenter.GetPosition());
      vLineBottom.Sub(GameField.cCenter.GetPosition());
      vLineTop.Normalize();
      vLineTop.Mul(400.0F);
      vLineBottom.Normalize();
      vLineBottom.Mul(400.0F);
      Vector vResultTop = new Vector();
      Vector vResultBottom = new Vector();
      GameField.cCenter.GetPosition().Add(vLineTop, vResultTop);
      GameField.cCenter.GetPosition().Add(vLineBottom, vResultBottom);
      return vResultTop.Distance(cFBaller.GetPosition()) < vResultBottom.Distance(cFBaller.GetPosition()) ? vResultTop : vResultBottom;
   }

   boolean IsForwardToPos(FootBaller cFBaller, Vector vPos) {
      Vector vDist = new Vector(vPos);
      vDist.Sub(cFBaller.GetPosition());
      vDist.fY = 0.0F;
      vDist.Normalize();
      Vector vForward = cFBaller.GetForwardVector();
      vForward.fY = 0.0F;
      vForward.Normalize();
      float fAngle = vDist.Dot(vForward);
      fAngle = Tools.acos(fAngle);
      fAngle = this.RadianToDegrees(fAngle);
      return fAngle < this.m_fForwardToOppAngle;
   }

   boolean IsBackwardToPos(FootBaller cFBaller, Vector vPos) {
      Vector vDist = new Vector(vPos);
      vDist.Sub(cFBaller.GetPosition());
      vDist.fY = 0.0F;
      vDist.Normalize();
      Vector vForward = cFBaller.GetForwardVector();
      vForward.fY = 0.0F;
      vForward.Normalize();
      float fAngle = vDist.Dot(vForward);
      fAngle = Tools.acos(fAngle);
      fAngle = this.RadianToDegrees(fAngle);
      return 180.0F - fAngle < this.m_fForwardToOppAngle;
   }

   boolean IsFightActiveDistance(AIFootBaller cFBaller) {
      if (cFBaller.m_cFightOpp != null) {
         float fDistance = cFBaller.GetDistanceTo(cFBaller.m_cFightOpp);
         float fScope = GetActiveDistance(cFBaller, cFBaller.m_cFightOpp);
         if (fDistance < fScope && cFBaller.m_cStrategy.IsForwardToPos(cFBaller, cFBaller.m_cFightOpp.GetPosition())) {
            return true;
         }
      }

      return false;
   }

   boolean InActiveDistance(AIFootBaller cFBaller, Vector vPos) {
      return cFBaller.GetDistanceTo(vPos) < GetActiveDistance(cFBaller);
   }

   public void delete() {
   }

   float GetHalfFieldDistance() {
      return GameField.cCenter.GetDistanceTo(GameField.cGateLeft.GetPositionVectorVolatile());
   }
}
