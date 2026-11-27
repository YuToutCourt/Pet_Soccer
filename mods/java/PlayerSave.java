import java.io.IOException;

public class PlayerSave {
   static final int _DIFF_LOW = 0;
   static final int _DIFF_MEDIUM = 1;
   static final int _DIFF_HARD = 2;
   String m_Name = null;
   KbSettings m_Kb0 = new KbSettings(0);
   KbSettings m_Kb1 = new KbSettings(1);
   int m_nDiffLevel = 1;
   int m_nDiffSetting = 6; // [mod] Classé par défaut (CoreLogic.GetDiffLevel)
   static final int _MAX_DIFF_LEVEL = 7;
   boolean[] m_tExtrasAvailable = new boolean[CoreLogic.GetExtraLevelsCount()];
   boolean[] m_tTeamsAvailable = new boolean[12];
   int m_nPlayerTeam = 1;
   Championships m_cChampionships = null;
   boolean m_bCommentatorOn = true;
   boolean m_bFieldMapOn = true;
   boolean m_bReplaysOn = true;
   boolean m_bAudienceOn = true;
   boolean m_bSupersOn = true;
   int m_nMatchHalfTimeIndex = 1;
   int m_nDefCameraMode = 0;
   float m_fCamZoomHoriz = 0.0F;
   float m_fCamZoomHorizSat = 0.0F;
   float m_fCamZoomVert = 0.0F;
   float m_fCamZoomVertSat = 0.0F;

   boolean IsExtraAvailable(int nExtra) {
      return this.m_tExtrasAvailable[nExtra - 15] || Cheat.m_bAllExtras;
   }

   void SetExtraAvailable(int nExtra) {
      this.m_tExtrasAvailable[nExtra - 15] = true;
   }

   void Save(Stream s) {
      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("Saving player: " + this.m_Name);
      }

      try {
         s.WriteMarker(4001);
         if (this.m_Name == null) {
            this.m_Name = "default";
         }

         s.out.writeUTF(this.m_Name);
         this.m_Kb0.Save(s);
         this.m_Kb1.Save(s);
         s.out.writeInt(this.m_nDiffLevel);
         s.out.writeInt(this.m_nDiffSetting);
         s.WriteMarker(4002);

         for (int i = 0; i < this.m_tExtrasAvailable.length; i++) {
            s.out.writeBoolean(this.m_tExtrasAvailable[i]);
         }

         for (int var5 = 0; var5 < this.m_tTeamsAvailable.length; var5++) {
            s.out.writeBoolean(this.m_tTeamsAvailable[var5]);
         }

         s.out.writeBoolean(this.m_bCommentatorOn);
         s.out.writeBoolean(this.m_bFieldMapOn);
         s.out.writeBoolean(this.m_bReplaysOn);
         s.out.writeBoolean(this.m_bAudienceOn);
         s.out.writeInt(this.m_nMatchHalfTimeIndex);
         s.out.writeInt(this.m_nDefCameraMode);
         s.out.writeFloat(this.m_fCamZoomHoriz);
         s.out.writeFloat(this.m_fCamZoomHorizSat);
         s.out.writeFloat(this.m_fCamZoomVert);
         s.out.writeFloat(this.m_fCamZoomVertSat);
         if (this.m_cChampionships != null) {
            this.m_cChampionships.Save(s);
         }

         s.out.writeBoolean(this.m_bSupersOn);
         s.WriteMarker(4003);
      } catch (IOException e) {
         GameObject.LogLine("PlayerSave::Save() - IOException");
      }
   }

   void SetCameraZoom(int nCameraMode, float fZoom) {
      this.m_nDefCameraMode = nCameraMode;
      switch (nCameraMode) {
         case 0:
            this.m_fCamZoomHoriz = fZoom;
            break;
         case 1:
            this.m_fCamZoomHorizSat = fZoom;
            break;
         case 2:
            this.m_fCamZoomVert = fZoom;
            break;
         case 3:
            this.m_fCamZoomVertSat = fZoom;
      }
   }

   boolean Load(Stream s) {
      try {
         if (!s.ReadMarker(4001)) {
            return false;
         }

         this.m_Name = s.in.readUTF();
         if (!this.m_Kb0.Load(s)) {
            return false;
         }

         if (!this.m_Kb1.Load(s)) {
            return false;
         }

         this.m_nDiffLevel = s.in.readInt();
         this.m_nDiffSetting = s.in.readInt();
         if (!s.ReadMarker(4002)) {
            return false;
         }

         for (int i = 0; i < this.m_tExtrasAvailable.length; i++) {
            this.m_tExtrasAvailable[i] = s.in.readBoolean();
         }

         for (int var5 = 0; var5 < this.m_tTeamsAvailable.length; var5++) {
            this.m_tTeamsAvailable[var5] = s.in.readBoolean();
         }

         this.m_bCommentatorOn = s.in.readBoolean();
         this.m_bFieldMapOn = s.in.readBoolean();
         this.m_bReplaysOn = s.in.readBoolean();
         this.m_bAudienceOn = s.in.readBoolean();
         this.m_nMatchHalfTimeIndex = s.in.readInt();
         this.m_nDefCameraMode = s.in.readInt();
         this.m_fCamZoomHoriz = s.in.readFloat();
         this.m_fCamZoomHorizSat = s.in.readFloat();
         this.m_fCamZoomVert = s.in.readFloat();
         this.m_fCamZoomVertSat = s.in.readFloat();
         this.m_cChampionships = new Championships();
         this.m_cChampionships.Load(s);
         if (GameSerializeSettings.m_nSaveVersion >= 10) {
            this.m_bSupersOn = s.in.readBoolean();
         }

         return s.ReadMarker(4003);
      } catch (IOException e) {
         GameObject.LogLine("PlayerSave::Load() - IOException");
         return false;
      }
   }

   float GetCameraZoom(int nCameraMode) {
      switch (nCameraMode) {
         case 0:
            return this.m_fCamZoomHoriz;
         case 1:
            return this.m_fCamZoomHorizSat;
         case 2:
            return this.m_fCamZoomVert;
         case 3:
            return this.m_fCamZoomVertSat;
         default:
            return 0.0F;
      }
   }

   void NextDiffLevel() {
      if (this.m_nDiffLevel < 7) {
         this.m_nDiffLevel++;
      }
   }

   void ResetChampionship(int nTeam, int nTeamClothes) {
      int nCup = this.m_cChampionships.m_nCurChampionships;
      if (nCup == -1) {
         nCup = 0;
      }

      int nOpponent = Championships.m_nFinalOpponents[nCup];
      this.m_cChampionships.Reset(this, nTeam, nTeamClothes, nOpponent);
   }

   // [mod] les equipes ajoutees (index >= 12) sont toujours disponibles ; settings.dat garde ses 12 cases
   boolean IsTeamAvailable(int nTeam) {
      return nTeam >= this.m_tTeamsAvailable.length || this.m_tTeamsAvailable[nTeam] || Cheat.m_bAllTeams;
   }

   void SetTeamAvailable(int nTeam) {
      if (nTeam < this.m_tTeamsAvailable.length) {
         this.m_tTeamsAvailable[nTeam] = true;
      }
   }

   boolean IsCheater() {
      return this.m_Name.equalsIgnoreCase("techlandrulez");
   }

   int GetHalfTime() {
      return 60 * CoreLogic._HALF_TIMES[this.m_nMatchHalfTimeIndex];
   }

   void SetDefault() {
      this.m_Name = "DEFAULT";
      this.m_Kb0 = new KbSettings(0);
      this.m_Kb1 = new KbSettings(1);
      this.m_nDiffLevel = 1;
      this.m_nDiffSetting = 6; // [mod] Classé par défaut

      for (int i = 0; i < this.m_tExtrasAvailable.length; i++) {
         this.m_tExtrasAvailable[i] = false;
      }

      for (int var2 = 0; var2 < this.m_tTeamsAvailable.length; var2++) {
         this.m_tTeamsAvailable[var2] = false;
      }

      for (int var3 = 0; var3 < Championships.m_nAvailableTeams.length; var3++) {
         this.SetTeamAvailable(Championships.m_nAvailableTeams[var3]);
      }

      this.m_cChampionships = new Championships();
      this.m_bCommentatorOn = true;
      this.m_bFieldMapOn = true;
      this.m_bReplaysOn = true;
      this.m_bAudienceOn = SoccerModule.m_bRenderPublic;
      this.m_bSupersOn = true;
      this.m_nMatchHalfTimeIndex = 1;
      this.m_nDefCameraMode = 0;
      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("Created Championships: ");
         GameObject.CrashLog(this.m_cChampionships.toString());
      }

      this.m_fCamZoomHoriz = 1000.0F;
      this.m_fCamZoomHorizSat = 1500.0F;
      this.m_fCamZoomVert = 1500.0F;
      this.m_fCamZoomVertSat = 1500.0F;
   }

   void SetName(String sName) {
      this.m_Name = sName;
      if (sName.equalsIgnoreCase("techlandrulez")) {
         for (int i = 0; i < this.m_tExtrasAvailable.length; i++) {
            this.m_tExtrasAvailable[i] = true;
         }

         for (int i = 0; i < this.m_tTeamsAvailable.length; i++) {
            this.m_tTeamsAvailable[i] = true;
         }
      }
   }
}
