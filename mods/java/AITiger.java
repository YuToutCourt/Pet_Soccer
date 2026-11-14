// [mod] joueur Tigre : copie d'AIPanther (meme modele 3D)
public class AITiger extends AIFootBaller {
   void UpdateExtents() {
      super.vMeshExtents = new Vector(60.0F, 74.0F, 60.0F);
      super.UpdateExtents();
   }

   public void OnCreate() {
      super.OnCreate();
      super.m_fBallDribbleDistance = -5.0F;
   }

   public void GoLive() {
      GameObject.LogLine("Jestem w GoLive");
      super.m_currContext.SetValue("Emocje", "Spokojny", super.m_advSpace);
      super.GoLive();
   }
}
