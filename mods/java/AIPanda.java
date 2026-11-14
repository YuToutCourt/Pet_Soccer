// [mod] joueur Panda : copie d'AIBear (meme modele 3D)
public class AIPanda extends AIFootBaller {
   void UpdateExtents() {
      super.vMeshExtents = new Vector(59.5F, 100.0F, 59.5F);
      super.UpdateExtents();
   }

   public void OnCreate() {
      super.OnCreate();
      super.m_fBallDribbleDistance = 0.0F;
   }

   public void GoLive() {
      GameObject.LogLine("Jestem w GoLive");
      super.m_currContext.SetValue("Emocje", "Spokojny", super.m_advSpace);
      super.GoLive();
   }
}
