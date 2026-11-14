public class SoccerFonts extends Fonts {
   static final String FONT_JAZZ_26 = "FLJazz26";
   static final String FONT_JAZZ_32 = "FLJazz32";
   static final String FONT_JAZZ_42 = "FLJazz42";
   static final String FONT_ROMAN_16 = "FLRoman16";
   static final String FONT_ROMAN_21 = "FLRoman21";
   static final String FONT_ROMAN_28 = "FLRoman28";
   static final String FONT_ROMAN_36 = "FLRoman36";
   static final String FONT_ROMAN_44 = "FLRoman44";
   static final String FONT_ROMAN_56 = "FLRoman56";
   // [mod] petite police des listes (écran Profil, buteurs), même nom dans toutes les résolutions
   static final String FONT_ROMAN_SMALL = "FLRomanSmall";
   // 16 px aussi en 640x480 : en 14 ou 15 px le moteur perd une partie des lettres (vu sur l'écran « en ligne »)
   static final int[] _ROMAN_SMALL_HEIGHT = new int[]{16, 16, 21};
   int m_nResolution = -1;

   SoccerFonts() {
   }

   boolean AddAndLoadFont(String sFontID, String sTTFName, int iHeight) {
      Fonts.AddFont(sFontID, sTTFName, iHeight);
      if (super.m_cModule == null) {
         Tools.CrashLogError("SoccerFonts.AddAndLoadFont m_cModule == null");
      }

      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("SoccerFonts.AddAndLoadFont " + sFontID);
      }

      return super.m_cModule.LoadFont(sFontID, sTTFName, iHeight);
   }

   void RemoveFont(String sFontId) {
      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("SoccerFonts.RemoveFont " + sFontId);
      }

      int nInd = Fonts.GetFontVectorInd(sFontId);
      if (nInd != -1) {
         Fonts.m_vFonts.removeElementAt(nInd);
      }

      if (super.m_cModule == null) {
         Tools.CrashLogError("SoccerFonts.AddAndLoadFont m_cModule == null");
      }

      super.m_cModule.ReleaseFont(sFontId);
   }

   void LoadFonts(int nResolution) {
      this.RemoveFonts();
      this.m_nResolution = nResolution;
      if (nResolution >= 0 && nResolution < _ROMAN_SMALL_HEIGHT.length) {
         this.AddAndLoadFont(FONT_ROMAN_SMALL, "FL Romanski 13", _ROMAN_SMALL_HEIGHT[nResolution]); // [mod]
      }

      switch (nResolution) {
         case 0:
            this.AddAndLoadFont("FLRoman16", "FL Romanski 13", 16);
            this.AddAndLoadFont("FLRoman36", "FL Romanski 13", 36);
            this.AddAndLoadFont("FLJazz26", "FL Jazzowy 1", 26);
            break;
         case 1:
            this.AddAndLoadFont("FLRoman21", "FL Romanski 13", 21);
            this.AddAndLoadFont("FLRoman44", "FL Romanski 13", 44);
            this.AddAndLoadFont("FLJazz32", "FL Jazzowy 1", 32);
            break;
         case 2:
            this.AddAndLoadFont("FLRoman28", "FL Romanski 13", 28);
            this.AddAndLoadFont("FLRoman56", "FL Romanski 13", 56);
            this.AddAndLoadFont("FLJazz42", "FL Jazzowy 1", 42);
      }
   }

   void RemoveFonts() {
      if (this.m_nResolution != -1) {
         this.RemoveFont(FONT_ROMAN_SMALL); // [mod]
      }

      switch (this.m_nResolution) {
         case 0:
            this.RemoveFont("FLRoman16");
            this.RemoveFont("FLRoman36");
            this.RemoveFont("FLJazz26");
            break;
         case 1:
            this.RemoveFont("FLRoman21");
            this.RemoveFont("FLRoman44");
            this.RemoveFont("FLJazz32");
            break;
         case 2:
            this.RemoveFont("FLRoman28");
            this.RemoveFont("FLRoman56");
            this.RemoveFont("FLJazz42");
      }
   }
}
