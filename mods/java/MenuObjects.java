public class MenuObjects extends MeshObject {
   float m_fRotatedAngle = 0.0F;
   float m_fDestAngleDelta = 0.0F;
   boolean m_bDestAngleSet = false;
   float m_fObjectAngleShift = 5.0F;
   final float _ANGSPEED = -30.0F;
   final float _ANGSPEED_FOCUS = -180.0F;
   float fAngSpeed = -30.0F;
   float fLastTime = 0.0F;
   String[] m_sElems = new String[]{"Mistrz03", "Multi1", "Mecz02", "Extra02", "Multi02", "Mecz1", "Mistrz1", "Extra1"};
   int[] m_nElems = null;
   float fDestAngle = 45.0F;
   int m_nFocus = -1;
   boolean bUp = false;
   boolean bRight = false;
   boolean bRotate = true;
   boolean bPlay = true;

   void SetFocus(int nItem) {
      GameObject.LogLine("SetFocus " + nItem);
      if (this.m_nFocus != nItem) {
         this.m_nFocus = nItem;
         switch (nItem) {
            case -1:
               this.ClearDestAngle();
            case 0:
            case 3:
            case 5:
            default:
               break;
            case 1:
               if (this.GetAngleDelta(180.0F + this.m_fObjectAngleShift) < this.GetAngleDelta(0.0F)) {
                  this.SetDestAngle(180.0F + this.m_fObjectAngleShift);
               } else {
                  this.SetDestAngle(this.m_fObjectAngleShift);
               }
               break;
            case 2:
               if (this.GetAngleDelta(135.0F + this.m_fObjectAngleShift) < this.GetAngleDelta(315.0F + this.m_fObjectAngleShift)) {
                  this.SetDestAngle(135.0F + this.m_fObjectAngleShift);
               } else {
                  this.SetDestAngle(315.0F + this.m_fObjectAngleShift);
               }
               break;
            case 4:
               if (this.GetAngleDelta(45.0F + this.m_fObjectAngleShift) < this.GetAngleDelta(225.0F + this.m_fObjectAngleShift)) {
                  this.SetDestAngle(45.0F + this.m_fObjectAngleShift);
               } else {
                  this.SetDestAngle(225.0F + this.m_fObjectAngleShift);
               }
               break;
            case 6:
               if (this.GetAngleDelta(90.0F + this.m_fObjectAngleShift) < this.GetAngleDelta(270.0F + this.m_fObjectAngleShift)) {
                  this.SetDestAngle(90.0F + this.m_fObjectAngleShift);
               } else {
                  this.SetDestAngle(270.0F + this.m_fObjectAngleShift);
               }
         }
      }
   }

   float GetAngleDelta(float fAngle) {
      return fAngle < this.m_fRotatedAngle ? 360.0F - this.m_fRotatedAngle + fAngle : fAngle - this.m_fRotatedAngle;
   }

   void PlayAnims() {
      for (int i = 0; i < this.m_sElems.length; i++) {
         this.PlayAnim(this.m_sElems[i], "all", -1);
      }
   }

   public void EnableRendering(boolean bEnable) {
      this.bRotate = bEnable;
      if (bEnable) {
         this.PlayAnims();
      } else {
         this.StopAnims();
      }

      super.EnableRendering(bEnable);
   }

   void ClearDestAngle() {
      this.m_fDestAngleDelta = 0.0F;
      this.fAngSpeed = -30.0F;
      this.m_bDestAngleSet = false;
   }

   public void OnPostUpdate() {
      float fTime = this.GetTime();
      if (this.fLastTime == 0.0F) {
         this.fLastTime = fTime;
      }

      float fTimeDelta = fTime - this.fLastTime;
      this.fLastTime = fTime;
      if (fTimeDelta > 0.2F) {
         fTimeDelta = 0.0F;
      }

      float fAngle = this.fAngSpeed * fTimeDelta;
      if (this.bRotate) {
         if (this.m_bDestAngleSet) {
            if (this.m_fDestAngleDelta > 0.0F) {
               if (this.m_fDestAngleDelta + fAngle < 0.0F) {
                  fAngle = -this.m_fDestAngleDelta;
               }

               this.RotateLocal(Vector.Forward, fAngle);
               this.m_fRotatedAngle -= fAngle;
               this.m_fDestAngleDelta += fAngle;
               if (this.m_fDestAngleDelta < 0.0F) {
                  this.m_fDestAngleDelta = 0.0F;
               }
            }
         } else {
            this.RotateLocal(Vector.Forward, fAngle);
            this.m_fRotatedAngle -= fAngle;
         }

         if (this.m_fRotatedAngle > 360.0F) {
            this.m_fRotatedAngle -= 360.0F;
         }

         this.PositionElems();
      }
   }

   void PositionElems() {
      if (this.m_nElems == null) {
         this.m_nElems = new int[this.m_sElems.length];

         for (int i = 0; i < this.m_sElems.length; i++) {
            this.m_nElems[i] = this.GetElementID(this.m_sElems[i]);
         }
      }

      Vector vPos = null;
      Vector vUpVect = Vector.Up;
      Vector vFdVect = null;

      for (int i = 0; i < this.m_nElems.length; i++) {
         int id = this.m_nElems[i];
         vPos = this.GetElementPos(id);
         // [mod] l'avant de l'element est deja en repere monde : le repasser en local (code d'origine) faisait
         // trembler les objets, d'autant plus fort que le FPS est eleve. On le garde a l'horizontale.
         vFdVect = this.GetElementForwardVector(id);
         vFdVect.fY = 0.0F;
         if (vFdVect.Len2() < 0.01F) {
            continue;
         }

         vFdVect.Normalize();
         this.FromUpForwardPosElementWorld(id, vUpVect, vFdVect, vPos);
      }
   }

   void OnCreate() {
      super.OnCreate();
      super.MeshName = "data/menu/Menu1.3DA";
      this.LoadMesh();
      this.RegisterKeyRequest();
      this.RotateLocal(Vector.Right, 40.0F);
      this.ClearFlag(4);
      this.ClearFlag(2);
      this.SetPosition(new Vector(398.0F, 245.0F, 347.0F));
      this.SetLighting(true);
      this.InitAnimSeq("all", 0, 99);
      this.EnableRendering(false);
   }

   void OnInputKey(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      if (SoccerGame.m_bDebug) {
         if (bKeyState) {
            switch (nKeyCode) {
               case 0:
            }
         }
      }
   }

   void StopAnims() {
      for (int i = 0; i < this.m_sElems.length; i++) {
         this.StopAnim(this.m_sElems[i]);
      }
   }

   void SetDestAngle(float fDestAngle) {
      this.m_fDestAngleDelta = this.GetAngleDelta(fDestAngle);
      this.fAngSpeed = -180.0F;
      this.fDestAngle = fDestAngle;
      this.m_bDestAngleSet = true;
   }
}
