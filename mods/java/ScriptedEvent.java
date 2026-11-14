public class ScriptedEvent {
   static final int _EVENT_NONE = 0;
   static final int _EVENT_STORM = 1;
   static final int _EVENT_STORM_AND_FOG = 2;
   static final int _EVENT_STORM_AND_FLASH = 3;
   static final int _EVENT_SNOW = 4;
   static final int _EVENT_EARTHQUAKE = 5;
   ScriptedEvent m_NextEvent = null;
   float m_TriggerTime = 0.0F;
   float m_fMaxTime = 10.0F;
   int m_EventKind = 0;
   String m_sText = null;
   float m_fParam1 = 0.0F;
   float m_fParam2 = 0.0F;
   int m_nParam1 = 0;
   int m_nParam2 = 0;

   ScriptedEvent() {
      this.m_EventKind = 0;
      this.m_TriggerTime = 0.0F;
   }

   ScriptedEvent(int kind, float time) {
      this.m_EventKind = kind;
      this.m_TriggerTime = time;
   }

   ScriptedEvent(int kind, float time, float fMaxTime) {
      this.m_EventKind = kind;
      this.m_TriggerTime = time;
      this.m_fMaxTime = fMaxTime;
   }

   ScriptedEvent(int kind, float time, String text, float fp1, float fp2, int np1, int np2) {
      this.m_EventKind = kind;
      this.m_TriggerTime = time;
      this.m_sText = text;
      this.m_fParam1 = fp1;
      this.m_fParam2 = fp2;
      this.m_nParam1 = np1;
      this.m_nParam2 = np2;
   }

   void Activate() {
      ModNet.HostWeather(this.m_EventKind, this.m_fMaxTime); // [mod] en ligne : même météo chez le client
      switch (this.m_EventKind) {
         case 1: {
            Storm ds = new Storm(this.m_fMaxTime);
            SoccerModule.m_cMatch.GetWeather().AddDisaster(ds);
            break;
         }
         case 2: {
            StormAndFog ds = new StormAndFog(this.m_fMaxTime);
            SoccerModule.m_cMatch.GetWeather().AddDisaster(ds);
            break;
         }
         case 3: {
            StormAndFlash ds = new StormAndFlash(this.m_fMaxTime);
            SoccerModule.m_cMatch.GetWeather().AddDisaster(ds);
            break;
         }
         case 4: {
            Snow ds = new Snow(this.m_fMaxTime);
            SoccerModule.m_cMatch.GetWeather().AddDisaster(ds);
            break;
         }
         case 5: {
            Earthquake ds = new Earthquake(this.m_fMaxTime);
            SoccerModule.m_cMatch.GetWeather().AddDisaster(ds);
         }
      }
   }

   public void delete() {
   }
}
