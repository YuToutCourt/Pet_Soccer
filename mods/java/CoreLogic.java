import java.io.IOException;

public class CoreLogic {
   static final String _EDIT_FORBIDDEN_CHARS = "\\/~!@#$%^&*_+|{}[]<>?,'\"`";
   static TeamData m_cTeamDataLeft = null;
   static TeamData m_cTeamDataRight = null;
   static boolean m_bDemo = false;
   static final int _SUPER_SHOT = 0;
   static final int _SUPER_FEINT = 1;
   static final int _SUPER_SPEED = 2;
   static final int _SUPER_DEFENCE = 3;
   static final int _PLAYER_1 = 0;
   static final int _PLAYER_2 = 1;
   static final int _PLAYER_COMPUTER = 2;
   static boolean m_bMultiplayer = false;
   static boolean m_bChampionships = false;
   static boolean m_bSurvival = false; // [mod] mode Survie (ModSurvival, MenuSurvival)
   static boolean m_bLeague = false; // [mod] mode Ligue (ModLeagueMode, MenuLeague, MenuLeagueBracket)
   static int m_nNextLevel = -1;
   static PlayerSave m_cCurPlayer = null;
   static final int _DEF_HALF_TIME_INDEX = 1;
   static final int[] _HALF_TIMES = new int[]{2, 4, 6, 10};
   static final int _LEVEL_BEARS = 0;
   static final int _LEVEL_BULLS = 1;
   static final int _LEVEL_CHICKENS = 2;
   static final int _LEVEL_DINOS = 3;
   static final int _LEVEL_FROGS = 4;
   static final int _LEVEL_GIRAFFES = 5;
   static final int _LEVEL_GORILLAS = 6;
   static final int _LEVEL_LIONS = 7;
   static final int _LEVEL_PANTHERS = 8;
   static final int _LEVEL_SHARKS = 9;
   static final int _LEVEL_TURTLES = 10;
   static final int _LEVEL_ROBOS = 11;
   static final int _LEVEL_INTRO = 12;
   static final int _LEVEL_OUTRO = 13;
   static final int _LEVEL_ONEFB = 14;
   static final int _LEVEL_EXTRA_ = 15;
   static final int _LEVEL_EXTRA_1 = 15;
   static final int _LEVEL_EXTRA_2 = 16;
   static final int _LEVEL_EXTRA_3 = 17;
   static final int _LEVEL_EXTRA_4 = 18;
   static final int _LEVEL_EXTRA_5 = 19;
   static final int _LEVEL_EXTRA_6 = 20;
   static final int _LAST_LEVEL_DUMMY = 21;
   static int _LEVELS_COUNT = 21;
   static String[] sLevelFileNames = new String[]{
      "data/levels/niedzwiedzie/niedzwiedzie.map",
      "data/levels/byki/byki.map",
      "data/levels/kurczaki/kurczaki.map",
      "data/levels/dinozaury/dinozaury.map",
      "data/levels/zaby/zaby.map",
      "data/levels/zyrafy/zyrafy.map",
      "data/levels/goryle/goryle.map",
      "data/levels/lwy/lwy.map",
      "data/levels/pantery/pantery.map",
      "data/levels/rekiny/rekiny.map",
      "data/levels/zolwie/zolwie.map",
      "data/levels/robociki/robociki.map",
      "data/levels/intro.map",
      "data/levels/outro.map",
      "data/levels/byki/byki_0.map",
      "data/levels/E1HighwayWarior/E1HighwayWarior.map",
      "data/levels/E2CutDownTrees/E2CutDownTrees.map",
      "data/levels/E3ParkMaze/E3ParkMaze.map",
      "data/levels/E4TheRedPlanet/E4TheRedPlanet.map",
      "data/levels/E5RainingCats&Dogs/E5RainingCats&Dogs.map",
      "data/levels/E6ThreeASide/E6ThreeASide.map"
   };
   static String[] sLevelNames = new String[]{
      "_LEVEL_BEARS",
      "_LEVEL_BULLS",
      "_LEVEL_CHICKENS",
      "_LEVEL_DINOS",
      "_LEVEL_FROGS",
      "_LEVEL_GIRAFFES",
      "_LEVEL_GORILLAS",
      "_LEVEL_LIONS",
      "_LEVEL_PANTHERS",
      "_LEVEL_SHARKS",
      "_LEVEL_TURTLES",
      "_LEVEL_ROBOS",
      "_LEVEL_INTRO",
      "_LEVEL_OUTRO",
      "_LEVEL_ONEFB",
      "&LevelNameExtra1&",
      "&LevelNameExtra2&",
      "&LevelNameExtra3&",
      "&LevelNameExtra4&",
      "&LevelNameExtra5&",
      "&LevelNameExtra6&"
   };
   static String[] sLevelMusic = new String[]{
      "data/Music/bears.ogg",
      "data/Music/bulls.ogg",
      "data/Music/chicken.ogg",
      "data/Music/dinos.ogg",
      "data/Music/frogs.ogg",
      "data/Music/giraffes.ogg",
      "data/Music/gorillas.ogg",
      "data/Music/lions.ogg",
      "data/Music/panthers.ogg",
      "data/Music/sharks.ogg",
      "data/Music/turtles.ogg",
      "data/Music/robos.ogg",
      "data/music/intro.ogg",
      "data/music/outro.ogg",
      "data/Music/bulls.ogg",
      "data/Music/extra.ogg",
      "data/Music/extra.ogg",
      "data/Music/extra.ogg",
      "data/Music/extra.ogg",
      "data/Music/extra.ogg",
      "data/Music/extra.ogg",
      "data/Music/extra.ogg"
   };
   static String[] sLevelLoopSounds = new String[]{
      null,
      "data/sounds/forestLoop.wav",
      "data/sounds/forestLoop.wav",
      "data/sounds/forestLoop.wav",
      "data/sounds/forestLoop.wav",
      "data/sounds/wiatr_loop.wav",
      "data/sounds/goryle_loop.wav",
      "data/sounds/wiatr_loop.wav",
      "data/sounds/u panter.wav",
      "data/sounds/wiatr_loop_rekiny.wav",
      "data/sounds/wiatr_loop.wav",
      null,
      null,
      null,
      "data/sounds/forestLoop.wav",
      "data/sounds/wiatr_loop.wav",
      "data/sounds/forestLoop.wav",
      "data/sounds/forestLoop.wav",
      null,
      "data/sounds/forestLoop.wav",
      "data/sounds/forestLoop.wav"
   };
   java.util.Vector m_Players = new java.util.Vector();
   static int _MENU_MUSIC_NORMAL = 0;
   static int _MENU_MUSIC_WIN = 1;
   static int _MENU_MUSIC_LOOSE = 2;
   static final String[] m_sMenuMusics = new String[]{"data/music/Menu.wav", "data/music/win.wav", "data/music/loose.wav"};
   boolean m_bPlayOutro = false;

   static boolean IsExtraLevel(int nLevel) {
      return nLevel >= 15;
   }

   CoreLogic() {
   }

   boolean Load(Stream s) {
      GameObject.CrashLog("CoreLogic load");

      try {
         if (!s.ReadMarker(1100)) {
            return false;
         }

         PlayerSave player = null;
         this.m_Players.removeAllElements();
         int players = s.in.readInt();

         for (int i = 0; i < players; i++) {
            player = new PlayerSave();
            if (!player.Load(s)) {
               return false;
            }

            this.m_Players.addElement(player);
         }

         return s.ReadMarker(1104);
      } catch (IOException e) {
         GameObject.LogLine("CoreLogic::Save() - IOException");
         return false;
      }
   }

   static String GetLevelLoopSoundName(int nLevelID) {
      return sLevelLoopSounds[nLevelID];
   }

   PlayerSave FindPlayerSaveByName(String name) {
      PlayerSave player = null;
      int cnt = this.m_Players.size();

      for (int i = 0; i < cnt; i++) {
         player = (PlayerSave)this.m_Players.elementAt(i);
         if (player.m_Name.equalsIgnoreCase(name)) {
            return player;
         }
      }

      return null;
   }

   static void SetPlayer(PlayerSave cPlayer) {
      m_cCurPlayer = cPlayer;
      SoccerModule.m_bRenderPublic = cPlayer.m_bAudienceOn;
   }

   static String GetLevelName(int nLevelID) {
      return sLevelNames[nLevelID];
   }

   static String GetMenuMusicName(int nType) {
      return m_sMenuMusics[nType];
   }

   static int GetExtraLevelsCount() {
      return 6;
   }

   static int GetDiffLevel() {
      // [mod] match de méta IA contre IA : le niveau demandé
      if (ModMeta.IsActive()) {
         return ModMeta.m_nLevel;
      }

      // [mod] en ligne : niveau fixe pour l'IA des coéquipiers, quel que soit le profil de l'hôte (data\online.scr)
      if (ModNet.IsOnline()) {
         return ModLobby.GetTeammateLevel();
      }

      // [mod] survie : le niveau suit la manche, quel que soit le réglage
      if (ModSurvival.IsActive()) {
         return ModSurvival.GetLevel();
      }

      int nDiffSetting = m_cCurPlayer.m_nDiffSetting;
      int nDiffLevel = m_cCurPlayer.m_nDiffLevel;
      switch (nDiffSetting) {
         case 0:
            switch (nDiffLevel) {
               case 1:
                  return 1;
               case 2:
                  return 1;
               case 3:
                  return 2;
               case 4:
                  return 2;
               case 5:
                  return 3;
               case 6:
                  return 3;
               case 7:
                  return 4;
               default:
                  return 3;
            }
         case 1:
            switch (nDiffLevel) {
               case 1:
                  return 2;
               case 2:
                  return 2;
               case 3:
                  return 3;
               case 4:
                  return 3;
               case 5:
                  return 4;
               case 6:
                  return 4;
               case 7:
                  return 5;
               default:
                  return 3;
            }
         case 2:
            switch (nDiffLevel) {
               case 1:
                  return 4;
               case 2:
                  return 4;
               case 3:
                  return 5;
               case 4:
                  return 5;
               case 5:
                  return 6;
               case 6:
                  return 6;
               case 7:
                  return 7;
            }
         // [mod] Expert / Maître / Légende : niveaux fixes 8..10 (difficulty.scr), indépendants des coupes gagnées
         case 3:
            return 8;
         case 4:
            return 9;
         case 5:
            return 10;
         // [mod] Classé : niveau de l'IA choisi selon l'Elo du profil (ModElo.GetRankedLevel)
         case 6:
            return ModElo.GetRankedLevel();
      }

      return 3;
   }

   boolean CanPlayOutro() {
      if (this.m_bPlayOutro) {
         this.m_bPlayOutro = false;
         return true;
      } else {
         return false;
      }
   }

   static PlayerSave GetPlayer() {
      if (m_cCurPlayer == null && SoccerGame.m_bDebug) {
         Tools.CrashLogWarning("CoreLogic m_cCurPlayer == null");
      }

      return m_cCurPlayer;
   }

   PlayerSave GetPlayer(int i) {
      return (PlayerSave)this.m_Players.elementAt(0);
   }

   int AddPlayer(PlayerSave cNewPlayer) {
      this.m_Players.addElement(cNewPlayer);
      return this.m_Players.size();
   }

   static String GetLevelFileName(int nLevelID) {
      return sLevelFileNames[nLevelID];
   }

   static String GetLevelMusicName(int nLevelID) {
      return sLevelMusic[nLevelID];
   }

   void RemovePlayer(PlayerSave cPlayer) {
      if (SoccerGame.m_bDebug) {
         GameObject.CrashLog("RemovePlayer " + cPlayer);
      }

      int nIndex = this.m_Players.indexOf(cPlayer);
      if (nIndex != -1) {
         this.m_Players.removeElementAt(nIndex);
      }
   }

   void SetNextLevel(int nLevel) {
      if (nLevel < 0) {
         nLevel = 0;
      }

      if (nLevel >= _LEVELS_COUNT) {
         nLevel = _LEVELS_COUNT - 1;
      }

      m_nNextLevel = nLevel;
   }

   int GetNextLevel() {
      return m_nNextLevel;
   }

   public void SetDefault() {
      if (SoccerGame.m_bDebug) {
         PlayerSave player = new PlayerSave();
         player.SetDefault();
         player.m_Name = Text.Get("&CLDefaultPlayerName&");
         this.m_Players.removeAllElements();
         this.m_Players.addElement(player);
      }

      GameObject.CrashLog("CoreLogic.LoadDefaults()\n");
   }

   void Save(Stream s) {
      try {
         s.WriteMarker(1100);
         s.out.writeInt(this.m_Players.size());
         PlayerSave player = null;
         int cnt = this.m_Players.size();

         for (int i = 0; i < cnt; i++) {
            player = (PlayerSave)this.m_Players.elementAt(i);
            player.Save(s);
         }

         s.WriteMarker(1104);
         s.out.flush();
      } catch (IOException e) {
         GameObject.LogLine("CoreLogic::Save() - IOException");
      }
   }
}
