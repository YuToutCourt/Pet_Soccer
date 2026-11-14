public class ActionTakeBall extends ActionReact {
   AIFootBaller m_cOpponent = null;

   public void Inner() {
      this.m_cOpponent = (AIFootBaller)super.m_cAIFootBaller.m_cDecContext.m_cObject;
      super.m_cAIFootBaller.StartIdle();
      super.m_cAIFootBaller.m_bBallTakenSucces = false;
      boolean bForward = false;
      // [mod] porteur gelé (super « gel ») : le tacle réussit quel que soit l'angle, sans feinte possible
      boolean bFrozen = ModSupers.IsFrozen(this.m_cOpponent);
      if (this.m_cOpponent != null && !bFrozen) {
         if (!this.m_cOpponent.m_cStrategy.IsForwardToPos(super.m_cAIFootBaller, this.m_cOpponent.GetPosition())) {
            this.m_cOpponent = null;
         } else if (super.m_cAIFootBaller.IsUserControled() && !super.m_cAIFootBaller.m_cStrategy.CanTakeBall(super.m_cAIFootBaller, this.m_cOpponent)) {
            this.m_cOpponent = null;
         }
      }

      if (this.m_cOpponent != null) {
         bForward = bFrozen || this.m_cOpponent.m_cStrategy.IsForwardToPos(this.m_cOpponent, super.m_cAIFootBaller.GetPosition());
         if (bFrozen && this.m_cOpponent.HasBall()
            || (this.m_cOpponent.m_cFeint == null || !this.m_cOpponent.m_cFeint.m_bSucces)
               && this.m_cOpponent.HasBall()
               && !this.m_cOpponent.IsKickMark()
               && (!this.m_cOpponent.IsSuperFeint() || super.m_cAIFootBaller.IsGoalKeeper())) {
            if (bForward) {
               super.m_cAIFootBaller.AttachBall(1, 0, this.m_cOpponent);
            }

            super.m_cAIFootBaller.m_bHasToInteria = false;
            super.m_cAIFootBaller.m_bBallTakenSucces = true;
         } else {
            super.m_cAIFootBaller.m_bHasToInteria = true;
         }
      }

      boolean bAttack = false;
      if (!super.m_cAIFootBaller.IsActivity("Bieganie") || super.m_cAIFootBaller.IsUserControled() && !super.m_cAIFootBaller.m_bTakingBallFromKey) {
         bAttack = false;
      } else {
         bAttack = true;
      }

      if (this.m_cOpponent != null) {
         this.m_cOpponent.m_bWasCommentAfterBallTaken = false;
      }

      if (bAttack) {
         if (SoccerGame.game.GetCommentator().TossHalf(50.0F) && this.m_cOpponent != null && super.m_cAIFootBaller.m_bBallTakenSucces) {
            SoccerGame.game.GetCommentator().CommentTakeBall(super.m_cAIFootBaller, this.m_cOpponent, true, this.m_cOpponent.GetZone() == 2);
            this.m_cOpponent.m_bWasCommentAfterBallTaken = true;
         }

         super.m_cAIFootBaller.SetReactType("wslizg");
         super.m_cAIFootBaller.m_bIsFalling = true;
         super.m_cAIFootBaller.PlaySound("wslizg.wav");
      } else {
         if (SoccerGame.game.GetCommentator().TossHalf(50.0F) && this.m_cOpponent != null && super.m_cAIFootBaller.m_bBallTakenSucces) {
            SoccerGame.game.GetCommentator().CommentTakeBall(super.m_cAIFootBaller, this.m_cOpponent, true, this.m_cOpponent.GetZone() == 2);
            this.m_cOpponent.m_bWasCommentAfterBallTaken = true;
         }

         super.m_cAIFootBaller.SetReactType("zastawienie");
         super.m_cAIFootBaller.m_bIsFalling = false;
      }

      super.Inner();
      if (this.m_cOpponent != null && super.m_cAIFootBaller.m_bBallTakenSucces) {
         if (!bForward) {
            super.m_cAIFootBaller.AttackKick();
         }

         if (!this.m_cOpponent.IsIdle()) {
            super.m_cAIFootBaller.m_cAISystem.RequestEvent(this.m_cOpponent, 7, super.m_cAIFootBaller);
         }
      }

      super.m_bDone = true;
   }

   void delete() {
      this.m_cOpponent = null;
      super.delete();
   }
}
