import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

// [mod] Rôles des joueurs (libéro, milieu, ailier...), schémas et marquage, lus dans data\roles.scr ;
// composition du joueur sauvegardée dans tactics.dat (voir notes/tactics.md)
class ModTactics {
   static final String _FILE = "data\\roles.scr";
   static final String _SAVE_FILE = "tactics.dat";
   static final String _ORIGIN = "ORIGINE";
   static final int _SLOTS = 6;
   // profondeur maximale (0 = son but, 1 = but adverse) d'un adversaire considéré comme une menace à marquer
   static final float _THREAT_DEPTH = 0.7F;
   // le marqueur lâche son adversaire s'il l'entraîne au-delà de MarkRange × ce facteur de sa position
   static final float _MARK_LEASH = 1.3F;
   static final float _MARK_REFRESH = 0.25F;
   static final float _FIELD_MARGIN = 0.06F;

   // situations de la carte tactique : une position par joueur pour chacune (null = automatique)
   static final int _SIT_PLAY = 0; // position de base en jeu, autour de laquelle le joueur suit le ballon selon son rôle
   static final int _SIT_KICKOFF = 1; // coup d'envoi (les deux équipes), dans sa moitié
   static final int _SIT_THROW = 2; // nos touches : profondeur relative au ballon (dessiné à mi-terrain, en haut)
   static final int _SIT_GOALKICK = 3; // nos dégagements au gardien
   static final int _SITUATIONS = 4;
   // positions sont notées (profondeur, latéral) : profondeur 0 = son but, 1 = but adverse ;
   // latéral 0 = haut de l'écran, 1 = bas (comme dans le jeu d'origine, un joueur garde son côté de l'écran à la mi-temps)
   // caméra par défaut placée côté X faible et tournée vers X croissant : le haut de l'écran est le côté X élevé
   static final boolean _TOP_IS_HIGH_X = true;
   // places 0..5 des joueurs de champ = points d'apparition triés par ligne (écart de profondeur < 0,03 = même ligne),
   // de la défense à l'attaque, puis de haut en bas ; positions de ces points dans le stade standard (crash.log, LogField)
   static final float[] _DEFAULT_LAYOUT = new float[]{0.2F, 0.23F, 0.18F, 0.5F, 0.2F, 0.78F, 0.44F, 0.39F, 0.41F, 0.5F, 0.44F, 0.61F};
   static final float _KICKOFF_MAX_DEPTH = 0.47F;
   static final float _MIN_DEPTH = 0.03F;
   static final float _MAX_DEPTH = 0.97F;
   // ballon des situations, pour l'aperçu de la carte (profondeur, latéral)
   static final float[] _THROW_BALL = new float[]{0.5F, 0.0F};
   static final float[] _GOALKICK_BALL = new float[]{0.06F, 0.5F};

   // rôle : placement sans ballon, en fractions du terrain
   static class Role {
      String m_sId;
      String m_sText;
      int m_nEngine = 1; // type moteur : 0 attaquant, 1 défenseur
      float m_fDepth = 0.3F; // profondeur de base (0 = son but, 1 = but adverse)
      float m_fFollow = 0.5F; // part du déplacement du ballon suivie en profondeur
      float m_fPush = 0.0F; // montée en plus quand l'équipe a le ballon
      float m_fMinDepth = 0.05F;
      float m_fMaxDepth = 0.9F;
      float m_fWidth = 0.0F; // écart au centre (0 = axe, 1 = ligne de touche)
      boolean m_bSide = false; // seul de son rôle : garde le côté de son point de départ au lieu de l'axe
      float m_fFollowX = 0.15F; // part du déplacement latéral du ballon suivie
      float m_fMarking = 0.0F; // 0 = défense de zone, 1 = marquage serré
      float m_fMarkRange = 0.2F; // rayon de recherche d'un adversaire à marquer (fraction de la longueur)
      boolean m_bCover = false; // reste derrière l'attaquant adverse le plus avancé
      // déplacements quand l'équipe a le ballon (ModMovement)
      float m_fFree = 0.0F; // rayon du démarquage autour de sa place (fraction de la longueur), 0 = aucun
      float m_fRuns = 0.0F; // envie de faire des appels en profondeur (0..1)
      float m_fGiveGo = 0.0F; // chance de repartir en une-deux après une passe courte (0..1)
      float m_fDrop = 0.0F; // décrochage vers le porteur quand il est loin derrière (0..1)

      public String toString() {
         return this.m_sId;
      }
   }

   static class Scheme {
      String m_sId;
      String m_sText;
      String[] m_tRoles; // null = jeu d'origine
   }

   static class Composition {
      String m_sScheme;
      String[] m_tRoles; // rôle de chaque place 0..5 (voir _DEFAULT_LAYOUT), null = jeu d'origine
      float[][] m_tLayouts = new float[_SITUATIONS][]; // par situation : (profondeur, latéral) des places 0..5, null = automatique

      Composition Copy() {
         Composition c = new Composition();
         c.m_sScheme = this.m_sScheme;
         c.m_tRoles = this.m_tRoles != null ? (String[])this.m_tRoles.clone() : null;

         for (int s = 0; s < _SITUATIONS; s++) {
            c.m_tLayouts[s] = this.m_tLayouts[s] != null ? (float[])this.m_tLayouts[s].clone() : null;
         }

         return c;
      }
   }

   static class PlayerInfo {
      Role m_cRole;
      int m_nSlot = -1;
      float m_fAnchorD = 0.3F; // position de base (profondeur, latéral)
      float m_fAnchorL = 0.5F;
      float[] m_tKickoff = null; // positions des situations, null = automatique
      float[] m_tThrow = null;
      float[] m_tGoalKick = null;
      int m_nOrigType = -1;
      float m_fPieceLog = -10.0F; // DebugLog des arrêts de jeu
   }

   private static int m_nDebugTest = -1; // TacticsTest(1) dans debugconf.scr : ouvre l'écran Tactique au menu (une fois)

   static boolean GetDebugTest() {
      if (m_nDebugTest < 0) {
         m_nDebugTest = 0;

         for (ParseKeyword key = GameObject.ParseFile("debugconf.scr"); key != null; key = key.GetNext()) {
            if (key.Equals("TacticsTest")) {
               m_nDebugTest = key.GetIParam(0);
            }
         }

         return m_nDebugTest > 0;
      }

      return false;
   }

   private static boolean m_bLoaded = false;
   private static java.util.Vector m_vRoles = new java.util.Vector();
   private static java.util.Vector m_vSchemes = new java.util.Vector();
   private static Hashtable m_tTeamSchemes = new Hashtable(); // type d'animal -> id de schéma
   private static String m_sHumanScheme = _ORIGIN;
   private static String m_sAIScheme = _ORIGIN;
   // [mod] tactiques variées de l'IA : schéma tiré à chaque match, changé à la mi-temps selon le score
   private static boolean m_bAIVary = false;
   private static java.util.Vector m_vAIPool = new java.util.Vector(); // ids de schéma
   private static java.util.Vector m_vAIWeights = new java.util.Vector(); // Float
   private static float m_fAIFavorite = 0.0F; // chance de jouer le schéma préféré de l'équipe (TeamScheme)
   private static float m_fAIAdaptChance = 0.0F;
   private static int m_nAIAdaptLosing = 1; // retard (buts) à la mi-temps pour passer à m_sAIAdaptLosing
   private static int m_nAIAdaptWinning = 2; // avance pour passer à m_sAIAdaptWinning
   private static String m_sAIAdaptLosing = "ATTAQUE";
   private static String m_sAIAdaptWinning = "BETON";
   private static Hashtable m_tMatchSchemes = new Hashtable(); // Team -> id de schéma du match en cours
   private static Hashtable m_tGoals = new Hashtable(); // Team -> Integer, buts marqués dans le match
   private static int m_nLastHalf = 0;
   private static float m_fHumanMarking = 1.0F;
   private static float m_fHumanMarkDistance = 150.0F;
   private static Hashtable m_tSaved = new Hashtable(); // "profil;animal" -> Composition
   private static Hashtable m_tPlayers = new Hashtable(); // AIFootBaller -> PlayerInfo
   private static AIMatch m_cLastMatch = null;
   private static boolean m_bFieldLogged = false;
   private static Hashtable m_tMarks = new Hashtable(); // AIFootBaller marqueur -> FootBaller marqué
   private static float[] m_fMarksTime = new float[]{-1.0F, -1.0F};
   private static boolean m_bDebugLog = false;
   private static float m_fDebugTime = -10.0F;
   // hors-jeu « doux » : sans ballon, personne ne reste derrière l'avant-dernier adversaire (pas de coup de sifflet)
   private static boolean m_bOffside = true;
   private static float m_fOffsideMargin = 0.02F; // recul sous la ligne (fraction de la longueur)
   private static float m_fOffsideTolerance = 0.01F; // au-delà de la ligne + tolérance, l'IA ne lui passe plus le ballon
   // passes longues : au-delà de LongPassStart, le point visé dérive de LongPassError × l'excès (au plus LongPassMaxError)
   private static float m_fLongPassStart = 1200.0F;
   private static float m_fLongPassError = 0.25F;
   private static float m_fLongPassMaxError = 450.0F;

   static void Load() {
      if (m_bLoaded) {
         return;
      }

      m_bLoaded = true;
      ParseKeyword root = GameObject.ParseFile(_FILE);
      if (root == null) {
         GameObject.CrashLog("[mod] " + _FILE + " absent, roles desactives\n");
         return;
      }

      for (ParseKeyword key = root; key != null; key = key.GetNext()) {
         if (key.Equals("Role")) {
            Role cRole = new Role();
            cRole.m_sId = key.GetSParam(0);
            cRole.m_sText = key.GetSParam(1);

            for (ParseKeyword p = key.GetChild(); p != null; p = p.GetNext()) {
               if (p.Equals("Engine")) {
                  cRole.m_nEngine = p.GetIParam(0);
               } else if (p.Equals("Depth")) {
                  cRole.m_fDepth = p.GetFParam(0);
               } else if (p.Equals("Follow")) {
                  cRole.m_fFollow = p.GetFParam(0);
               } else if (p.Equals("Push")) {
                  cRole.m_fPush = p.GetFParam(0);
               } else if (p.Equals("MinDepth")) {
                  cRole.m_fMinDepth = p.GetFParam(0);
               } else if (p.Equals("MaxDepth")) {
                  cRole.m_fMaxDepth = p.GetFParam(0);
               } else if (p.Equals("Width")) {
                  cRole.m_fWidth = p.GetFParam(0);
               } else if (p.Equals("Side")) {
                  cRole.m_bSide = p.GetIParam(0) != 0;
               } else if (p.Equals("FollowX")) {
                  cRole.m_fFollowX = p.GetFParam(0);
               } else if (p.Equals("Marking")) {
                  cRole.m_fMarking = p.GetFParam(0);
               } else if (p.Equals("MarkRange")) {
                  cRole.m_fMarkRange = p.GetFParam(0);
               } else if (p.Equals("Cover")) {
                  cRole.m_bCover = p.GetIParam(0) != 0;
               } else if (p.Equals("Free")) {
                  cRole.m_fFree = p.GetFParam(0);
               } else if (p.Equals("Runs")) {
                  cRole.m_fRuns = p.GetFParam(0);
               } else if (p.Equals("GiveGo")) {
                  cRole.m_fGiveGo = p.GetFParam(0);
               } else if (p.Equals("Drop")) {
                  cRole.m_fDrop = p.GetFParam(0);
               }
            }

            m_vRoles.addElement(cRole);
         } else if (key.Equals("Scheme")) {
            Scheme cScheme = new Scheme();
            cScheme.m_sId = key.GetSParam(0);
            cScheme.m_sText = key.GetSParam(1);
            if (!cScheme.m_sId.equals(_ORIGIN)) {
               cScheme.m_tRoles = new String[_SLOTS];

               for (int i = 0; i < _SLOTS; i++) {
                  cScheme.m_tRoles[i] = key.GetSParam(2 + i);
               }
            }

            m_vSchemes.addElement(cScheme);
         } else if (key.Equals("TeamScheme")) {
            m_tTeamSchemes.put(new Integer(key.GetIParam(0)), key.GetSParam(1));
         } else if (key.Equals("HumanScheme")) {
            m_sHumanScheme = key.GetSParam(0);
         } else if (key.Equals("AIScheme")) {
            m_sAIScheme = key.GetSParam(0);
         } else if (key.Equals("AIVary")) {
            m_bAIVary = key.GetIParam(0) != 0;
         } else if (key.Equals("AISchemeWeight")) {
            m_vAIPool.addElement(key.GetSParam(0));
            m_vAIWeights.addElement(new Float(key.GetFParam(1)));
         } else if (key.Equals("AIFavorite")) {
            m_fAIFavorite = key.GetFParam(0);
         } else if (key.Equals("AIAdapt")) {
            m_fAIAdaptChance = key.GetFParam(0);
            m_nAIAdaptLosing = key.GetIParam(1);
            m_nAIAdaptWinning = key.GetIParam(2);
            m_sAIAdaptLosing = key.GetSParam(3);
            m_sAIAdaptWinning = key.GetSParam(4);
         } else if (key.Equals("HumanMarking")) {
            m_fHumanMarking = key.GetFParam(0);
         } else if (key.Equals("HumanMarkDistance")) {
            m_fHumanMarkDistance = key.GetFParam(0);
         } else if (key.Equals("DebugLog")) {
            m_bDebugLog = key.GetIParam(0) != 0;
         } else if (key.Equals("KickoffTest")) {
            ModKickoff.m_nTest = key.GetIParam(0);
         } else if (key.Equals("Offside")) {
            m_bOffside = key.GetIParam(0) != 0;
         } else if (key.Equals("OffsideMargin")) {
            m_fOffsideMargin = key.GetFParam(0);
         } else if (key.Equals("OffsideTolerance")) {
            m_fOffsideTolerance = key.GetFParam(0);
         } else if (key.Equals("LongPassStart")) {
            m_fLongPassStart = key.GetFParam(0);
         } else if (key.Equals("LongPassError")) {
            m_fLongPassError = key.GetFParam(0);
         } else if (key.Equals("LongPassMaxError")) {
            m_fLongPassMaxError = key.GetFParam(0);
         }
      }

      // un schéma qui cite un rôle inconnu est ignoré
      for (int s = m_vSchemes.size() - 1; s >= 0; s--) {
         Scheme cScheme = (Scheme)m_vSchemes.elementAt(s);
         if (cScheme.m_tRoles != null) {
            for (int i = 0; i < _SLOTS; i++) {
               if (GetRole(cScheme.m_tRoles[i]) == null) {
                  GameObject.CrashLog("[mod] roles : schema " + cScheme.m_sId + " ignore, role inconnu " + cScheme.m_tRoles[i] + "\n");
                  m_vSchemes.removeElementAt(s);
                  break;
               }
            }
         }
      }

      LoadSaved();
      GameObject.CrashLog("[mod] roles charges : " + m_vRoles.size() + " roles, " + m_vSchemes.size() + " schemas\n");
   }

   static int GetRoleCount() {
      Load();
      return m_vRoles.size();
   }

   static Role GetRole(int nIndex) {
      Load();
      return (Role)m_vRoles.elementAt(nIndex);
   }

   static Role GetRole(String sId) {
      for (int i = 0; i < m_vRoles.size(); i++) {
         Role cRole = (Role)m_vRoles.elementAt(i);
         if (cRole.m_sId.equals(sId)) {
            return cRole;
         }
      }

      return null;
   }

   static int GetSchemeCount() {
      Load();
      return m_vSchemes.size();
   }

   static Scheme GetScheme(int nIndex) {
      Load();
      return (Scheme)m_vSchemes.elementAt(nIndex);
   }

   static Scheme GetScheme(String sId) {
      for (int i = 0; i < m_vSchemes.size(); i++) {
         Scheme cScheme = (Scheme)m_vSchemes.elementAt(i);
         if (cScheme.m_sId.equals(sId)) {
            return cScheme;
         }
      }

      return null;
   }

   // ---------------------------------------------------------------- compositions sauvegardées

   private static String GetPlayerName() {
      PlayerSave player = CoreLogic.GetPlayer();
      if (player == null || player.m_Name == null) {
         return "default";
      }

      // ';' sert de séparateur dans tactics.dat
      return player.m_Name.replace(';', ',');
   }

   private static Composition MakeComposition(String sScheme) {
      Composition c = new Composition();
      Scheme cScheme = GetScheme(sScheme);
      if (cScheme == null) {
         cScheme = GetScheme(_ORIGIN);
      }

      c.m_sScheme = cScheme != null ? cScheme.m_sId : _ORIGIN;
      if (cScheme != null && cScheme.m_tRoles != null) {
         c.m_tRoles = SortByDepth(cScheme.m_tRoles);
      }

      return c;
   }

   // rôles rangés du plus défensif au plus offensif : la place 0 (la plus reculée) reçoit le plus défensif
   static String[] SortByDepth(String[] tRoles) {
      String[] t = (String[])tRoles.clone();

      for (int i = 1; i < t.length; i++) {
         for (int j = i; j > 0 && RoleDepth(t[j]) < RoleDepth(t[j - 1]); j--) {
            String tmp = t[j];
            t[j] = t[j - 1];
            t[j - 1] = tmp;
         }
      }

      return t;
   }

   private static float RoleDepth(String sId) {
      Role r = GetRole(sId);
      return r != null ? r.m_fDepth : 0.5F;
   }

   // rôle d'une place (rôle inconnu : le premier de roles.scr)
   static Role SlotRole(Composition c, int nSlot) {
      Role r = GetRole(c.m_tRoles[nSlot]);
      return r != null ? r : GetRole(0);
   }

   // composition de l'équipe nAnimal pour le profil courant (copie modifiable)
   static Composition GetComposition(int nAnimal) {
      Load();
      Composition cSaved = (Composition)m_tSaved.get(GetPlayerName() + ";" + nAnimal);
      if (cSaved == null) {
         return MakeComposition(m_sHumanScheme);
      }

      // rôles sauvegardés (schéma modifié à la main ou schéma retiré de roles.scr) : gardés tels quels
      if (cSaved.m_tRoles != null) {
         return cSaved.Copy();
      }

      return MakeComposition(cSaved.m_sScheme);
   }

   static void SetComposition(int nAnimal, Composition c) {
      Load();
      m_tSaved.put(GetPlayerName() + ";" + nAnimal, c.Copy());
      SaveSaved();
   }

   // tactics.dat : une ligne par profil et par équipe,
   // "nom;animal;schéma;rôle1,...,rôle6;jeu;engagement;touche;dégagement" ("-" = jeu d'origine / placement automatique),
   // chaque placement = 6 couples "profondeur/latéral" en millièmes.
   // Ancien format (sans placements) : rôles dans n'importe quel ordre, rangés par profondeur à la lecture.
   private static void LoadSaved() {
      File f = new File(_SAVE_FILE);
      if (!f.exists()) {
         return;
      }

      try {
         BufferedReader in = new BufferedReader(new FileReader(f));

         for (String sLine = in.readLine(); sLine != null; sLine = in.readLine()) {
            StringTokenizer tok = new StringTokenizer(sLine, ";");
            if (tok.countTokens() >= 3) {
               boolean bNewFormat = tok.countTokens() >= 4 + _SITUATIONS;
               String sName = tok.nextToken();
               String sAnimal = tok.nextToken().trim();
               Composition c = new Composition();
               c.m_sScheme = tok.nextToken().trim();
               if (tok.hasMoreTokens()) {
                  StringTokenizer roles = new StringTokenizer(tok.nextToken(), ",");
                  if (roles.countTokens() == _SLOTS) {
                     c.m_tRoles = new String[_SLOTS];

                     for (int i = 0; i < _SLOTS; i++) {
                        c.m_tRoles[i] = roles.nextToken().trim();
                        if (GetRole(c.m_tRoles[i]) == null) {
                           c.m_tRoles = null;
                           break;
                        }
                     }
                  }
               }

               if (c.m_tRoles != null && !bNewFormat) {
                  c.m_tRoles = SortByDepth(c.m_tRoles);
               }

               for (int s = 0; s < _SITUATIONS && tok.hasMoreTokens(); s++) {
                  c.m_tLayouts[s] = c.m_tRoles != null ? ParseLayout(tok.nextToken()) : null;
               }

               m_tSaved.put(sName + ";" + sAnimal, c);
            }
         }

         in.close();
      } catch (Exception e) {
         GameObject.CrashLog("[mod] tactique : lecture de " + _SAVE_FILE + " impossible : " + e + "\n");
      }
   }

   private static float[] ParseLayout(String s) {
      StringTokenizer pairs = new StringTokenizer(s.trim(), ",");
      if (pairs.countTokens() != _SLOTS) {
         return null;
      }

      float[] t = new float[_SLOTS * 2];

      try {
         for (int i = 0; i < _SLOTS; i++) {
            StringTokenizer xy = new StringTokenizer(pairs.nextToken(), "/");
            t[2 * i] = Clamp(Integer.parseInt(xy.nextToken().trim()) / 1000.0F, _MIN_DEPTH, _MAX_DEPTH);
            t[2 * i + 1] = Clamp(Integer.parseInt(xy.nextToken().trim()) / 1000.0F, 0.0F, 1.0F);
         }
      } catch (Exception e) {
         return null;
      }

      return t;
   }

   private static String LayoutString(float[] t) {
      if (t == null) {
         return "-";
      }

      String s = "";

      for (int i = 0; i < _SLOTS; i++) {
         s = s + (i > 0 ? "," : "") + Math.round(t[2 * i] * 1000.0F) + "/" + Math.round(t[2 * i + 1] * 1000.0F);
      }

      return s;
   }

   // [mod] en ligne : composition sur une ligne, pour l'envoyer à l'autre joueur (format de tactics.dat, sans profil ni équipe)
   static String ToLine(Composition c) {
      Load();
      StringBuffer s = new StringBuffer(c.m_sScheme);
      if (c.m_tRoles != null) {
         s.append(';');

         for (int i = 0; i < _SLOTS; i++) {
            s.append(i > 0 ? "," : "").append(c.m_tRoles[i]);
         }

         for (int n = 0; n < _SITUATIONS; n++) {
            s.append(';').append(LayoutString(c.m_tLayouts[n]));
         }
      }

      return s.toString();
   }

   // [mod] en ligne : lit une ligne de ToLine. Ligne vide, rôle ou schéma inconnu ici : composition par défaut.
   static Composition FromLine(String sLine) {
      Load();

      try {
         StringTokenizer tok = new StringTokenizer(sLine == null ? "" : sLine, ";");
         if (!tok.hasMoreTokens()) {
            return MakeComposition(m_sHumanScheme);
         }

         Composition c = new Composition();
         c.m_sScheme = tok.nextToken().trim();
         if (!tok.hasMoreTokens()) {
            return MakeComposition(c.m_sScheme);
         }

         StringTokenizer roles = new StringTokenizer(tok.nextToken(), ",");
         if (roles.countTokens() != _SLOTS) {
            return MakeComposition(c.m_sScheme);
         }

         c.m_tRoles = new String[_SLOTS];

         for (int i = 0; i < _SLOTS; i++) {
            c.m_tRoles[i] = roles.nextToken().trim();
            if (GetRole(c.m_tRoles[i]) == null) {
               return MakeComposition(c.m_sScheme);
            }
         }

         for (int n = 0; n < _SITUATIONS && tok.hasMoreTokens(); n++) {
            c.m_tLayouts[n] = ParseLayout(tok.nextToken());
         }

         return c;
      } catch (Exception e) {
         return MakeComposition(m_sHumanScheme);
      }
   }

   private static void SaveSaved() {
      try {
         FileWriter out = new FileWriter(_SAVE_FILE);
         Enumeration keys = m_tSaved.keys();

         while (keys.hasMoreElements()) {
            String sKey = (String)keys.nextElement();
            Composition c = (Composition)m_tSaved.get(sKey);
            out.write(sKey + ";" + c.m_sScheme);
            if (c.m_tRoles != null) {
               out.write(";");

               for (int i = 0; i < _SLOTS; i++) {
                  out.write((i > 0 ? "," : "") + c.m_tRoles[i]);
               }

               for (int s = 0; s < _SITUATIONS; s++) {
                  out.write(";" + LayoutString(c.m_tLayouts[s]));
               }
            }

            out.write("\r\n");
         }

         out.close();
      } catch (Exception e) {
         GameObject.CrashLog("[mod] tactique : ecriture de " + _SAVE_FILE + " impossible : " + e + "\n");
      }
   }

   // ---------------------------------------------------------------- placements (communs à la carte et au match)

   static float LToX(float fL) {
      return _TOP_IS_HIGH_X ? 1.0F - fL : fL;
   }

   static float XToL(float fX) {
      return _TOP_IS_HIGH_X ? 1.0F - fX : fX;
   }

   // placement d'une situation : positions choisies, sinon calculées (jeu d'origine : points d'apparition du stade standard)
   static float[] GetLayout(Composition c, int nSit) {
      if (c.m_tRoles == null) {
         return (float[])_DEFAULT_LAYOUT.clone();
      }

      if (c.m_tLayouts[nSit] != null) {
         return (float[])c.m_tLayouts[nSit].clone();
      }

      return AutoLayout(c, nSit);
   }

   static float[] AutoLayout(Composition c, int nSit) {
      float[] t = new float[_SLOTS * 2];
      if (nSit == _SIT_PLAY) {
         // profondeur du rôle ; joueurs d'un même rôle répartis de -Width à +Width (de haut en bas), Side(1) = garde son côté
         boolean[] tDone = new boolean[_SLOTS];

         for (int k = 0; k < _SLOTS; k++) {
            Role r = SlotRole(c, k);
            t[2 * k] = r.m_fDepth;
            if (!tDone[k]) {
               int[] tGroup = new int[_SLOTS];
               int n = 0;

               for (int j = k; j < _SLOTS; j++) {
                  if (!tDone[j] && SlotRole(c, j) == r) {
                     int a = n++;

                     while (a > 0 && _DEFAULT_LAYOUT[2 * tGroup[a - 1] + 1] > _DEFAULT_LAYOUT[2 * j + 1]) {
                        tGroup[a] = tGroup[a - 1];
                        a--;
                     }

                     tGroup[a] = j;
                     tDone[j] = true;
                  }
               }

               for (int i = 0; i < n; i++) {
                  float fLane;
                  if (n > 1) {
                     fLane = -r.m_fWidth + 2.0F * r.m_fWidth * i / (n - 1);
                  } else if (r.m_bSide) {
                     fLane = _DEFAULT_LAYOUT[2 * tGroup[i] + 1] < 0.5F ? -r.m_fWidth : r.m_fWidth;
                  } else {
                     fLane = 0.0F;
                  }

                  t[2 * tGroup[i] + 1] = 0.5F + 0.5F * fLane;
               }
            }
         }

         return t;
      }

      float[] tPlay = GetLayout(c, _SIT_PLAY);

      for (int k = 0; k < _SLOTS; k++) {
         Role r = SlotRole(c, k);
         float[] p;
         if (nSit == _SIT_KICKOFF) {
            // même couloir ; un défenseur garde sa profondeur, les joueurs plus hauts se rapprochent du milieu
            float d = tPlay[2 * k];
            p = new float[]{Math.min(Math.min(d, 0.3F + 0.2F * d), _KICKOFF_MAX_DEPTH), tPlay[2 * k + 1]};
         } else if (nSit == _SIT_THROW) {
            p = RolePlace(r, tPlay[2 * k], tPlay[2 * k + 1], _THROW_BALL[0], _THROW_BALL[1], true);
         } else {
            p = RolePlace(r, tPlay[2 * k], tPlay[2 * k + 1], _GOALKICK_BALL[0], _GOALKICK_BALL[1], true);
         }

         t[2 * k] = p[0];
         t[2 * k + 1] = p[1];
      }

      return t;
   }

   // position sans ballon d'après le rôle : la base suit le ballon (Follow / FollowX), monte de Push si l'équipe l'a,
   // et reste dans l'amplitude du rôle (MinDepth..MaxDepth autour de Depth) décalée sur la base
   static float[] RolePlace(Role r, float fAnchorD, float fAnchorL, float fBallD, float fBallL, boolean bPossession) {
      float fDepth = fAnchorD + r.m_fFollow * (fBallD - 0.5F);
      if (bPossession) {
         fDepth += r.m_fPush;
      }

      fDepth = Clamp(fDepth, fAnchorD - Math.max(r.m_fDepth - r.m_fMinDepth, 0.0F), fAnchorD + Math.max(r.m_fMaxDepth - r.m_fDepth, 0.0F));
      fDepth = Clamp(fDepth, _MIN_DEPTH, _MAX_DEPTH);
      float fL = Clamp(fAnchorL + r.m_fFollowX * (fBallL - 0.5F), _FIELD_MARGIN, 1.0F - _FIELD_MARGIN);
      return new float[]{fDepth, fL};
   }

   // ---------------------------------------------------------------- attribution des rôles

   // appelé par AIMatch.Restart (début de match, mi-temps, après un but)
   static void OnRestart(AIMatch cMatch) {
      Load();
      if (cMatch != m_cLastMatch) {
         m_cLastMatch = cMatch;
         m_tPlayers.clear();
         m_tMarks.clear();
         m_tMatchSchemes.clear();
         m_tGoals.clear();
         m_nLastHalf = cMatch.m_nHalfNo;
         if (!m_bFieldLogged) {
            m_bFieldLogged = true;
            LogField(cMatch);
         }
      }

      if (m_nLastHalf == 0 && cMatch.m_nHalfNo == 1) {
         m_nLastHalf = 1;
         AdaptAtHalfTime(cMatch);
      }

      ApplyTeams(cMatch);
   }

   // appelé par AIMatch.OnGoal (nTeam = équipe qui marque, indice du moment)
   static void OnGoal(AIMatch cMatch, int nTeam) {
      Team cTeam = cMatch.GetTeam(nTeam);
      if (cTeam != null) {
         m_tGoals.put(cTeam, new Integer(Goals(cTeam) + 1));
      }
   }

   private static int Goals(Team cTeam) {
      Integer n = (Integer)m_tGoals.get(cTeam);
      return n != null ? n.intValue() : 0;
   }

   // l'IA ne choisit son schéma que si rien ne l'impose (joueur, en ligne, méta)
   private static boolean VariesScheme(Team cTeam) {
      return m_bAIVary && !ModMeta.IsActive() && ModNet.RemoteComposition(cTeam) == null
         && (!cTeam.m_bUserControled || CoreLogic.m_bDemo);
   }

   // mi-temps : une IA menée passe à l'attaque, une IA qui mène nettement ferme le jeu (chance m_fAIAdaptChance)
   private static void AdaptAtHalfTime(AIMatch cMatch) {
      for (int t = 0; t < 2; t++) {
         Team cTeam = cMatch.GetTeam(t);
         Team cOther = cMatch.GetTeam(1 - t);
         if (cTeam == null || cOther == null || !VariesScheme(cTeam)) {
            continue;
         }

         int nDiff = Goals(cTeam) - Goals(cOther);
         String sNew = null;
         if (nDiff <= -m_nAIAdaptLosing) {
            sNew = m_sAIAdaptLosing;
         } else if (nDiff >= m_nAIAdaptWinning) {
            sNew = m_sAIAdaptWinning;
         }

         String sOld = (String)m_tMatchSchemes.get(cTeam);
         if (sNew == null || sNew.equals(sOld) || GetScheme(sNew) == null || GameObject.RandomFloat(1.0F) >= m_fAIAdaptChance) {
            continue;
         }

         m_tMatchSchemes.put(cTeam, sNew);
         GameObject.CrashLog("[mod] tactique IA : animal " + cTeam.m_nAnimalType + " passe de " + sOld + " a " + sNew
            + " a la mi-temps (" + Goals(cTeam) + "-" + Goals(cOther) + ")\n");
      }
   }

   // schéma tiré au début du match : le préféré de l'équipe (TeamScheme) avec la chance AIFavorite, sinon AISchemeWeight
   private static String DrawScheme(Team cTeam) {
      String sFavorite = (String)m_tTeamSchemes.get(new Integer(cTeam.m_nAnimalType));
      if (sFavorite != null && GetScheme(sFavorite) != null && GameObject.RandomFloat(1.0F) < m_fAIFavorite) {
         return sFavorite;
      }

      float fTotal = 0.0F;
      for (int i = 0; i < m_vAIPool.size(); i++) {
         if (GetScheme((String)m_vAIPool.elementAt(i)) != null) {
            fTotal += ((Float)m_vAIWeights.elementAt(i)).floatValue();
         }
      }

      if (fTotal > 0.0F) {
         float f = GameObject.RandomFloat(fTotal);
         for (int i = 0; i < m_vAIPool.size(); i++) {
            String sId = (String)m_vAIPool.elementAt(i);
            if (GetScheme(sId) != null) {
               f -= ((Float)m_vAIWeights.elementAt(i)).floatValue();
               if (f < 0.0F) {
                  return sId;
               }
            }
         }
      }

      return sFavorite != null ? sFavorite : m_sAIScheme;
   }

   // (ré)applique la composition des deux équipes, aussi après un changement en cours de match
   static void ApplyTeams(AIMatch cMatch) {
      for (int t = 0; t < 2; t++) {
         Team cTeam = cMatch.GetTeam(t);
         if (cTeam != null) {
            ApplyTeam(cTeam, GetTeamComposition(cTeam));
         }
      }
   }

   private static Composition GetTeamComposition(Team cTeam) {
      // [mod] en ligne : l'équipe du joueur distant joue avec la composition qu'il a envoyée
      Composition cRemote = ModNet.RemoteComposition(cTeam);
      if (cRemote != null) {
         return cRemote;
      }

      if (cTeam.m_bUserControled && !CoreLogic.m_bDemo) {
         return GetComposition(cTeam.m_nAnimalType);
      }

      // [mod] méta : schéma imposé par MetaSchemes (matchs de tactique)
      String sMeta = ModMeta.Scheme(cTeam);
      if (sMeta != null) {
         return MakeComposition(sMeta);
      }

      if (VariesScheme(cTeam)) {
         String sDrawn = (String)m_tMatchSchemes.get(cTeam);
         if (sDrawn == null) {
            sDrawn = DrawScheme(cTeam);
            m_tMatchSchemes.put(cTeam, sDrawn);
            GameObject.CrashLog("[mod] tactique IA : animal " + cTeam.m_nAnimalType + " joue " + sDrawn + "\n");
         }

         return MakeComposition(sDrawn);
      }

      // avec AIVary, TeamScheme n'est qu'un schéma préféré : la méta garde AIScheme pour toutes les équipes
      String sScheme = m_bAIVary ? null : (String)m_tTeamSchemes.get(new Integer(cTeam.m_nAnimalType));
      return MakeComposition(sScheme != null ? sScheme : m_sAIScheme);
   }

   // [mod] méta : la composition jouée par une équipe, en ligne (ToLine)
   static String TeamLine(Team cTeam) {
      return cTeam != null ? ToLine(GetTeamComposition(cTeam)) : "";
   }

   private static float SpawnDepth(AIFootBaller cFB) {
      float fZ = (cFB.cSpawnPoint.GetPosition().fZ - GameField.vFieldLogicalStart.fZ) / GameField.fLineEnd;
      return cFB.m_nTeam == 0 ? fZ : 1.0F - fZ;
   }

   private static float SpawnX(AIFootBaller cFB) {
      return (cFB.cSpawnPoint.GetPosition().fX - GameField.vFieldLogicalStart.fX) / GameField.fLineOutTop;
   }

   // ordre des places : du point d'apparition le plus reculé au plus avancé, puis de haut en bas
   private static boolean SlotBefore(AIFootBaller a, AIFootBaller b) {
      float da = SpawnDepth(a);
      float db = SpawnDepth(b);
      if (Math.abs(da - db) > 0.03F) {
         return da < db;
      }

      return XToL(SpawnX(a)) < XToL(SpawnX(b));
   }

   // [mod] ModSwitch : joueurs de champ rangés par numéro de la carte tactique (case k = joueur k + 2).
   // Place connue (ApplyTeam) d'abord ; sinon (composition ORIGINE, client en ligne) même ordre que ApplyTeam,
   // par points d'apparition. null si l'équipe n'a pas exactement _SLOTS joueurs de champ.
   static AIFootBaller[] GetNumbered(Team cTeam) {
      if (cTeam == null || cTeam.m_vFootBallers == null) {
         return null;
      }

      AIFootBaller[] tFB = new AIFootBaller[_SLOTS];
      int nField = 0;
      boolean bSlots = true;

      for (int i = 0; i < cTeam.m_vFootBallers.size(); i++) {
         AIFootBaller cFB = (AIFootBaller)cTeam.m_vFootBallers.elementAt(i);
         if (cFB == null || cFB.IsGoalKeeper() || cFB.IsReferee()) {
            continue;
         }

         if (nField >= _SLOTS || cFB.cSpawnPoint == null) {
            return null;
         }

         tFB[nField++] = cFB;
         PlayerInfo info = (PlayerInfo)m_tPlayers.get(cFB);
         if (info == null || info.m_nSlot < 0 || info.m_nSlot >= _SLOTS) {
            bSlots = false;
         }
      }

      if (nField != _SLOTS) {
         return null;
      }

      if (bSlots) {
         AIFootBaller[] tBySlot = new AIFootBaller[_SLOTS];

         for (int i = 0; i < _SLOTS; i++) {
            tBySlot[((PlayerInfo)m_tPlayers.get(tFB[i])).m_nSlot] = tFB[i];
         }

         boolean bFull = true;

         for (int i = 0; i < _SLOTS; i++) {
            bFull &= tBySlot[i] != null;
         }

         if (bFull) {
            return tBySlot;
         }
      }

      for (int i = 1; i < _SLOTS; i++) {
         for (int j = i; j > 0 && SlotBefore(tFB[j], tFB[j - 1]); j--) {
            AIFootBaller tmp = tFB[j];
            tFB[j] = tFB[j - 1];
            tFB[j - 1] = tmp;
         }
      }

      return tFB;
   }

   private static void ApplyTeam(Team cTeam, Composition c) {
      java.util.Vector vField = new java.util.Vector();

      for (int i = 0; i < cTeam.m_vFootBallers.size(); i++) {
         AIFootBaller cFB = (AIFootBaller)cTeam.m_vFootBallers.elementAt(i);
         if (!cFB.IsGoalKeeper() && !cFB.IsReferee()) {
            PlayerInfo info = (PlayerInfo)m_tPlayers.get(cFB);
            if (info == null) {
               info = new PlayerInfo();
               info.m_nOrigType = cFB.m_nType;
               m_tPlayers.put(cFB, info);
            }

            vField.addElement(cFB);
         }
      }

      boolean bOrigin = c.m_tRoles == null || vField.size() != _SLOTS;
      String sLog = "[mod] tactique " + cTeam.m_sName + " (" + (cTeam.m_bUserControled ? "humain, profil " + GetPlayerName() : "IA") + ", animal " + cTeam.m_nAnimalType + ") : " + (bOrigin ? _ORIGIN : c.m_sScheme);
      if (bOrigin) {
         for (int i = 0; i < vField.size(); i++) {
            AIFootBaller cFB = (AIFootBaller)vField.elementAt(i);
            PlayerInfo info = (PlayerInfo)m_tPlayers.get(cFB);
            info.m_cRole = null;
            info.m_nSlot = -1;
            info.m_tKickoff = info.m_tThrow = info.m_tGoalKick = null;
            cFB.m_nType = info.m_nOrigType;
         }

         GameObject.CrashLog(sLog + "\n");
         return;
      }

      // place k = k-ième point d'apparition (le plus reculé d'abord) : c'est le joueur numéro k + 2 de la carte tactique
      AIFootBaller[] tFB = new AIFootBaller[_SLOTS];

      for (int i = 0; i < _SLOTS; i++) {
         tFB[i] = (AIFootBaller)vField.elementAt(i);
      }

      for (int i = 1; i < _SLOTS; i++) {
         for (int j = i; j > 0 && SlotBefore(tFB[j], tFB[j - 1]); j--) {
            AIFootBaller tmp = tFB[j];
            tFB[j] = tFB[j - 1];
            tFB[j - 1] = tmp;
         }
      }

      float[] tPlay = GetLayout(c, _SIT_PLAY);
      float[] tKick = GetLayout(c, _SIT_KICKOFF);
      float[] tThrow = c.m_tLayouts[_SIT_THROW];
      float[] tGoalKick = c.m_tLayouts[_SIT_GOALKICK];

      for (int k = 0; k < _SLOTS; k++) {
         PlayerInfo info = (PlayerInfo)m_tPlayers.get(tFB[k]);
         info.m_nSlot = k;
         info.m_cRole = SlotRole(c, k);
         info.m_fAnchorD = tPlay[2 * k];
         info.m_fAnchorL = tPlay[2 * k + 1];
         info.m_tKickoff = new float[]{tKick[2 * k], tKick[2 * k + 1]};
         // touche et dégagement automatiques : placement du rôle autour du ballon, calculé en match
         info.m_tThrow = tThrow != null ? new float[]{tThrow[2 * k], tThrow[2 * k + 1]} : null;
         info.m_tGoalKick = tGoalKick != null ? new float[]{tGoalKick[2 * k], tGoalKick[2 * k + 1]} : null;
         tFB[k].m_nType = info.m_cRole.m_nEngine;
         sLog = sLog + " j" + (k + 2) + "=n" + tFB[k].m_nNumberInTeam + ":" + info.m_cRole + "@" + (int)(info.m_fAnchorD * 100.0F) + "/" + (int)(info.m_fAnchorL * 100.0F);
      }

      String sCustom = "";

      for (int s = 0; s < _SITUATIONS; s++) {
         sCustom = sCustom + (c.m_tLayouts[s] != null ? "P" : "a");
      }

      GameObject.CrashLog(sLog + " placements " + sCustom + "\n");
   }

   // [mod] en ligne (ModStats.PutMatchStats) : place 0..5 du joueur dans la composition, -1 (gardien, jeu d'origine)
   static int GetSlot(AIFootBaller cFB) {
      PlayerInfo info = (PlayerInfo)m_tPlayers.get(cFB);
      return info != null && info.m_cRole != null ? info.m_nSlot : -1;
   }

   // [mod] en ligne (ModStats.PutMatchStats) : GetSlot, et pour un joueur de champ du jeu d'origine sa place par
   // points d'apparition (GetNumbered) + _STATS_ORIGIN, + _STATS_ORIGIN_ATT s'il est attaquant pour le moteur
   static final int _STATS_ORIGIN = 0x10;
   static final int _STATS_ORIGIN_ATT = 0x08;

   static int GetStatsSlot(AIFootBaller cFB) {
      int nSlot = GetSlot(cFB);
      if (nSlot >= 0 || cFB.IsGoalKeeper()) {
         return nSlot;
      }

      AIFootBaller[] tFB = GetNumbered(cFB.m_cTeam);

      for (int k = 0; tFB != null && k < tFB.length; k++) {
         if (tFB[k] == cFB) {
            return _STATS_ORIGIN | (GetOrigType(cFB) == 0 ? _STATS_ORIGIN_ATT : 0) | k;
         }
      }

      return -1;
   }

   // ---------------------------------------------------------------- placement en match

   private static float BallDepth(int nSide) {
      float f = (GameField.GetDestPos().fZ - GameField.vFieldLogicalStart.fZ) / GameField.fLineEnd;
      f = Clamp(f, 0.0F, 1.0F);
      return nSide == 0 ? f : 1.0F - f;
   }

   // rôle d'un joueur en match (null = jeu d'origine ou inconnu)
   static Role GetRoleOf(AIFootBaller cFB) {
      PlayerInfo info = (PlayerInfo)m_tPlayers.get(cFB);
      return info != null ? info.m_cRole : null;
   }

   // type moteur d'origine (0 attaquant, 1 défenseur)
   static int GetOrigType(AIFootBaller cFB) {
      PlayerInfo info = (PlayerInfo)m_tPlayers.get(cFB);
      return info != null ? info.m_nOrigType : cFB.m_nType;
   }

   static boolean IsDebug() {
      return m_bDebugLog;
   }

   static boolean IsOffsideOn() {
      Load();
      return m_bOffside;
   }

   static float GetOffsideMargin() {
      return m_fOffsideMargin;
   }

   static float Depth(int nSide, Vector vPos) {
      float f = (vPos.fZ - GameField.vFieldLogicalStart.fZ) / GameField.fLineEnd;
      return nSide == 0 ? f : 1.0F - f;
   }

   static float Clamp(float f, float fMin, float fMax) {
      return f < fMin ? fMin : (f > fMax ? fMax : f);
   }

   private static Vector FieldPos(int nSide, float fDepth, float fX, float fY) {
      float fZ = nSide == 0 ? fDepth : 1.0F - fDepth;
      return new Vector(
         GameField.vFieldLogicalStart.fX + Clamp(fX, _FIELD_MARGIN, 1.0F - _FIELD_MARGIN) * GameField.fLineOutTop,
         fY,
         GameField.vFieldLogicalStart.fZ + Clamp(fZ, 0.02F, 0.98F) * GameField.fLineEnd
      );
   }

   // latéral du ballon (0 = haut de l'écran)
   static float LateralOf(Vector vPos) {
      return XToL(Clamp((vPos.fX - GameField.vFieldLogicalStart.fX) / GameField.fLineOutTop, 0.0F, 1.0F));
   }

   private static Vector RolePos(AITeam cTeam, AIFootBaller cFB, PlayerInfo info) {
      Role r = info.m_cRole;
      int nSide = cFB.m_nTeam;
      boolean bPossession = cTeam.IsPossesor() || cTeam.IsPrevOwner();
      float[] p = RolePlace(r, info.m_fAnchorD, info.m_fAnchorL, BallDepth(nSide), LateralOf(GameField.GetDestPos()), bPossession);
      float fDepth = p[0];
      if (r.m_bCover) {
         // dernier rempart : reste entre son but et l'adversaire le plus avancé
         float fDeepest = DeepestOpponent(cTeam, nSide);
         if (fDepth > fDeepest - 0.04F) {
            fDepth = Math.max(fDeepest - 0.04F, 0.03F);
         }
      }

      Vector vPos = FieldPos(nSide, fDepth, LToX(p[1]), cFB.GetPosition().fY);
      if (!SoccerModule.m_cMatch.IsWaiting()) {
         vPos.fX = vPos.fX + cFB.m_fDevX * 0.5F;
         vPos.fZ = vPos.fZ + cFB.m_fDevZ * 0.3F;
      }

      return vPos;
   }

   private static Team GetOpponents(Team cTeam) {
      Team cTeam0 = SoccerModule.m_cMatch.GetTeam(0);
      return cTeam0 == cTeam ? SoccerModule.m_cMatch.GetTeam(1) : cTeam0;
   }

   private static float DeepestOpponent(Team cTeam, int nSide) {
      Team cOpp = GetOpponents(cTeam);
      float fMin = 1.0F;

      for (int i = 0; i < cOpp.m_vFootBallers.size(); i++) {
         FootBaller cFB = (FootBaller)cOpp.m_vFootBallers.elementAt(i);
         if (!cFB.IsGoalKeeper()) {
            fMin = Math.min(fMin, Depth(nSide, cFB.GetPosition()));
         }
      }

      return fMin;
   }

   // force du marquage (0..1) : rôle × difficulté pour l'IA, rôle × HumanMarking pour l'équipe du joueur
   private static float MarkingStrength(Team cTeam, AIFootBaller cFB, PlayerInfo info) {
      if (info == null) {
         return 0.0F;
      }

      if (info.m_cRole == null) {
         // jeu d'origine : seuls les défenseurs de l'IA marquent, selon la difficulté
         return !cTeam.m_bUserControled && info.m_nOrigType == 1 ? ModDifficulty.GetMarking() : 0.0F;
      }

      return info.m_cRole.m_fMarking * (cTeam.m_bUserControled ? m_fHumanMarking : ModDifficulty.GetMarking());
   }

   private static float MarkRange(PlayerInfo info) {
      return (info.m_cRole != null ? info.m_cRole.m_fMarkRange : 0.2F) * GameField.fLineEnd;
   }

   // joueur dirigé au clavier / à la manette (IsUserControled() vaut pour toute l'équipe)
   private static boolean IsHumanPlayer(Team cTeam, AIFootBaller cFB) {
      return cTeam.m_bUserControled && cFB == cTeam.m_cActiveFootBaller;
   }

   private static boolean CanMark(AITeam cTeam, AIFootBaller cFB) {
      return !cFB.HasBall()
         && cFB != cTeam.m_cActiveFootBaller
         && cFB != cTeam.m_cHelpAttacker
         && cFB != cTeam.m_cHelpBallAttacker;
   }

   // répartit les adversaires dangereux entre les marqueurs (le plus proche de notre but d'abord)
   private static void UpdateMarks(AITeam cTeam) {
      int t = cTeam == SoccerModule.m_cMatch.GetTeam(0) ? 0 : 1;
      float fNow = SoccerGame.GetSoccerModule().GetTime();
      if (fNow >= m_fMarksTime[t] && fNow - m_fMarksTime[t] < _MARK_REFRESH) {
         return;
      }

      m_fMarksTime[t] = fNow;
      java.util.Vector vMarkers = new java.util.Vector();

      for (int i = 0; i < cTeam.m_vFootBallers.size(); i++) {
         AIFootBaller cFB = (AIFootBaller)cTeam.m_vFootBallers.elementAt(i);
         m_tMarks.remove(cFB);
         if (!cFB.IsGoalKeeper() && CanMark(cTeam, cFB) && MarkingStrength(cTeam, cFB, (PlayerInfo)m_tPlayers.get(cFB)) > 0.0F) {
            vMarkers.addElement(cFB);
         }
      }

      if (vMarkers.size() == 0 || cTeam.IsPossesor() || cTeam.IsPrevOwner()) {
         return;
      }

      int nSide = ((AIFootBaller)vMarkers.elementAt(0)).m_nTeam;
      Team cOpp = GetOpponents(cTeam);
      java.util.Vector vThreats = new java.util.Vector();

      for (int i = 0; i < cOpp.m_vFootBallers.size(); i++) {
         FootBaller cFB = (FootBaller)cOpp.m_vFootBallers.elementAt(i);
         if (!cFB.IsGoalKeeper() && !cFB.HasBall() && Depth(nSide, cFB.GetPosition()) < _THREAT_DEPTH) {
            int k = 0;

            while (k < vThreats.size() && Depth(nSide, ((FootBaller)vThreats.elementAt(k)).GetPosition()) <= Depth(nSide, cFB.GetPosition())) {
               k++;
            }

            vThreats.insertElementAt(cFB, k);
         }
      }

      for (int i = 0; i < vThreats.size() && vMarkers.size() > 0; i++) {
         FootBaller cThreat = (FootBaller)vThreats.elementAt(i);
         AIFootBaller cBest = null;
         float fBest = 0.0F;

         for (int m = 0; m < vMarkers.size(); m++) {
            AIFootBaller cMarker = (AIFootBaller)vMarkers.elementAt(m);
            float fDist = cMarker.GetDistanceTo(cThreat.GetPosition());
            if (fDist < MarkRange((PlayerInfo)m_tPlayers.get(cMarker)) && (cBest == null || fDist < fBest)) {
               cBest = cMarker;
               fBest = fDist;
            }
         }

         if (cBest != null) {
            m_tMarks.put(cBest, cThreat);
            vMarkers.removeElement(cBest);
         }
      }
   }

   // appelé à la fin d'AITeam.UpdateValidPos : remplace la position d'origine par celle du rôle, puis applique le marquage
   static void UpdateValidPos(AITeam cTeam, AIFootBaller cFB) {
      if (cFB.IsGoalKeeper() || cFB.IsReferee() || cFB.m_vValidPos == null) {
         return;
      }

      PlayerInfo info = (PlayerInfo)m_tPlayers.get(cFB);
      if (info == null) {
         return;
      }

      if (!AIMatch.IsMatchState(0)) {
         SetPiecePos(cFB, info);
         return;
      }

      if (SoccerModule.m_cMatch.IsWaiting() || IsHumanPlayer(cTeam, cFB)) {
         return;
      }

      Vector vPos = info.m_cRole != null ? RolePos(cTeam, cFB, info) : new Vector(cFB.m_vValidPos);
      boolean bChanged = info.m_cRole != null;
      float fStrength = MarkingStrength(cTeam, cFB, info);
      if (fStrength > 0.0F) {
         UpdateMarks(cTeam);
         FootBaller cThreat = (FootBaller)m_tMarks.get(cFB);
         if (cThreat != null && vPos.Distance(cThreat.GetPosition()) < MarkRange(info) * _MARK_LEASH) {
            // côté but : entre l'adversaire et le centre de notre but
            Vector vThreat = cThreat.GetPosition();
            Vector vGoal = cFB.m_nTeam == 0 ? GameField.cGateLeft.GetPosition() : GameField.cGateRight.GetPosition();
            Vector vDir = new Vector(vGoal);
            vDir.Sub(vThreat);
            vDir.fY = 0.0F;
            float fLen = vDir.Len();
            float fDist = cTeam.m_bUserControled ? m_fHumanMarkDistance : ModDifficulty.GetMarkDistance();
            if (fLen > 1.0F) {
               vDir.Mul(Math.min(fDist, fLen * 0.5F) / fLen);
            }

            vPos.fX = vPos.fX + (vThreat.fX + vDir.fX - vPos.fX) * fStrength;
            vPos.fZ = vPos.fZ + (vThreat.fZ + vDir.fZ - vPos.fZ) * fStrength;
            bChanged = true;
         }
      }

      // [mod] démarquage, appels en profondeur, une-deux (quand l'équipe a le ballon)
      Vector vMove = ModMovement.Adjust(cTeam, cFB, vPos);
      if (vMove != null) {
         vPos = vMove;
         bChanged = true;
      }

      if (m_bOffside && !cFB.HasBall() && !ModMovement.IsRunning(cFB)) {
         float fLine = OffsideLine(cFB) - m_fOffsideMargin;
         if (Depth(cFB.m_nTeam, vPos) > fLine) {
            vPos.fZ = FieldZ(cFB.m_nTeam, fLine);
            bChanged = true;
         }
      }

      if (bChanged) {
         cFB.SetValidPos(vPos);
      }

      if (m_bDebugLog) {
         DebugLog(cTeam, cFB, info);
      }
   }

   // ---------------------------------------------------------------- coup d'envoi, touches, corners, dégagements

   // position d'un joueur de champ pendant un arrêt de jeu (composition personnalisée seulement ; jeu d'origine inchangé).
   // Appelé chaque image pour les joueurs qui ne vont pas au ballon, et à la mise en place (UpdateStabilizationPos).
   private static void SetPiecePos(AIFootBaller cFB, PlayerInfo info) {
      if (info.m_cRole == null) {
         return;
      }

      AIMatch cMatch = SoccerModule.m_cMatch;
      int nState = AIMatch.GetMatchState();
      int nSide = cFB.m_nTeam;
      float fDepth;
      float fL;
      if (nState == 7) {
         if (info.m_tKickoff == null) {
            return;
         }

         fDepth = info.m_tKickoff[0];
         fL = info.m_tKickoff[1];
      } else {
         if (nState != 4 && nState != 5 && nState != 6) {
            return;
         }

         Vector vBall = nState == 6
            ? cMatch.m_cStrategy.GetGoalKeeperOutPos(cMatch.m_nSpecialGoalSide)
            : cMatch.m_cStrategy.GetSpecialBallPos(cMatch.m_nSpecialGoalSide);
         if (vBall == null) {
            return;
         }

         float fBallD = Clamp(Depth(nSide, vBall), 0.0F, 1.0F);
         float fBallL = LateralOf(vBall);
         boolean bOurs = cMatch.m_nTeamToStart >= 0 && cMatch.GetTeam(cMatch.m_nTeamToStart) == cFB.m_cTeam;
         if (nState == 5 && bOurs && info.m_tThrow != null) {
            // touche dessinée ballon en haut à mi-terrain : suit la profondeur du ballon, inversée si la touche est en bas
            fDepth = fBallD + info.m_tThrow[0] - _THROW_BALL[0];
            fL = fBallL > 0.5F ? 1.0F - info.m_tThrow[1] : info.m_tThrow[1];
         } else if (nState == 6 && bOurs && info.m_tGoalKick != null) {
            fDepth = info.m_tGoalKick[0];
            fL = info.m_tGoalKick[1];
         } else {
            float[] p = RolePlace(info.m_cRole, info.m_fAnchorD, info.m_fAnchorL, fBallD, fBallL, bOurs);
            fDepth = p[0];
            fL = p[1];
         }
      }

      cFB.SetValidPos(FieldPos(nSide, Clamp(fDepth, _MIN_DEPTH, _MAX_DEPTH), LToX(fL), cFB.GetPosition().fY));
      if (m_bDebugLog) {
         float fNow = SoccerGame.GetSoccerModule().GetTime();
         if (fNow - info.m_fPieceLog > 3.0F || fNow < info.m_fPieceLog) {
            info.m_fPieceLog = fNow;
            GameObject.CrashLog("[mod] arret " + nState + " " + cFB.m_cTeam.m_sName + " j" + (info.m_nSlot + 2) + " cible "
               + (int)(Clamp(fDepth, _MIN_DEPTH, _MAX_DEPTH) * 100.0F) + "/" + (int)(fL * 100.0F) + " (profondeur/lateral, 0 = haut)\n");
         }
      }
   }

   // position de coup d'envoi (AITeam.UpdateStabilizationPos), null = point d'apparition du stade
   static Vector KickoffPos(AIFootBaller cFB) {
      PlayerInfo info = (PlayerInfo)m_tPlayers.get(cFB);
      if (info == null || info.m_cRole == null || info.m_tKickoff == null || cFB.IsGoalKeeper() || cFB.IsReferee()) {
         return null;
      }

      return FieldPos(cFB.m_nTeam, info.m_tKickoff[0], LToX(info.m_tKickoff[1]), cFB.cSpawnPoint.GetPosition().fY);
   }

   // appelé par AIMatch.RestartGameRaw juste après le retour aux points d'apparition (écran noir) :
   // les joueurs apparaissent directement à leur place de coup d'envoi
   static void PlaceKickoff(AIMatch cMatch) {
      for (int t = 0; t < 2; t++) {
         Team cTeam = cMatch.GetTeam(t);

         for (int i = 0; cTeam != null && i < cTeam.m_vFootBallers.size(); i++) {
            AIFootBaller cFB = (AIFootBaller)cTeam.m_vFootBallers.elementAt(i);
            Vector vPos = KickoffPos(cFB);
            if (vPos != null) {
               cFB.SetPosition(vPos);
               cFB.SetValidPos(new Vector(vPos));
               cMatch.m_cStrategy.SetDirToBall(cFB);
            }
         }
      }
   }

   // ---------------------------------------------------------------- hors-jeu doux et passes longues

   private static float FieldZ(int nSide, float fDepth) {
      float fZ = nSide == 0 ? fDepth : 1.0F - fDepth;
      return GameField.vFieldLogicalStart.fZ + fZ * GameField.fLineEnd;
   }

   // ligne de hors-jeu de l'équipe de cFB, dans son repère (1 = but adverse) : avant-dernier adversaire (gardien compris),
   // jamais en deçà du ballon ni de la ligne médiane
   static float OffsideLine(AIFootBaller cFB) {
      int nSide = cFB.m_nTeam;
      Team cOpp = GetOpponents(cFB.m_cTeam);
      float fFirst = 0.0F;
      float fSecond = 0.0F;

      for (int i = 0; i < cOpp.m_vFootBallers.size(); i++) {
         FootBaller cOppFB = (FootBaller)cOpp.m_vFootBallers.elementAt(i);
         if (!cOppFB.IsReferee()) {
            float f = Depth(nSide, cOppFB.GetPosition());
            if (f > fFirst) {
               fSecond = fFirst;
               fFirst = f;
            } else if (f > fSecond) {
               fSecond = f;
            }
         }
      }

      float fBall = Depth(nSide, GameField.m_cBall.GetPositionVectorVolatile());
      return Math.max(Math.max(fSecond, fBall), 0.5F);
   }

   // vrai si cFB (sans le ballon) est nettement derrière la ligne de hors-jeu, en jeu courant seulement
   static boolean IsOffside(AIFootBaller cFB) {
      if (ModMovement.IsFreshRun(cFB)) {
         return false; // appel lancé depuis la ligne : il était en jeu au départ de sa course
      }

      return IsOffsideAt(cFB, cFB.GetPosition());
   }

   // même test pour cFB placé en vPos (ex. appel de balle envisagé)
   static boolean IsOffsideAt(AIFootBaller cFB, Vector vPos) {
      Load();
      if (!m_bOffside || vPos == null || !AIMatch.IsMatchState(0) || cFB.IsGoalKeeper() || cFB.HasBall()) {
         return false;
      }

      return Depth(cFB.m_nTeam, vPos) > OffsideLine(cFB) + m_fOffsideTolerance;
   }

   // appelé au début d'AIFootBaller.Pass : une passe longue en jeu courant arrive un peu à côté du point visé
   static Vector LongPassError(AIFootBaller cFB, Vector vDest) {
      Load();
      if (m_fLongPassError <= 0.0F || vDest == null || !AIMatch.IsMatchState(0) || cFB.IsGoalKeeper()) {
         return vDest;
      }

      float fExcess = cFB.GetPosition().Distance(vDest) - m_fLongPassStart;
      if (fExcess <= 0.0F) {
         return vDest;
      }

      float fError = Math.min(fExcess * m_fLongPassError, m_fLongPassMaxError) * (0.5F + GameObject.RandomFloat(0.5F));
      double dAngle = GameObject.RandomFloat(6.2831855F);
      Vector vResult = new Vector(vDest);
      vResult.fX = Clamp(vResult.fX + (float)Math.cos(dAngle) * fError,
         GameField.vFieldLogicalStart.fX, GameField.vFieldLogicalStart.fX + GameField.fLineOutTop);
      vResult.fZ = Clamp(vResult.fZ + (float)Math.sin(dAngle) * fError,
         GameField.vFieldLogicalStart.fZ, GameField.vFieldLogicalStart.fZ + GameField.fLineEnd);
      if (m_bDebugLog) {
         GameObject.CrashLog("[mod] passe longue " + (int)(fExcess + m_fLongPassStart) + " ecart " + (int)fError + "\n");
      }

      return vResult;
   }

   private static void DebugLog(AITeam cTeam, AIFootBaller cFB, PlayerInfo info) {
      float fNow = SoccerGame.GetSoccerModule().GetTime();
      if (fNow - m_fDebugTime < 2.0F && fNow >= m_fDebugTime) {
         return;
      }

      m_fDebugTime = fNow;
      Team[] tTeams = new Team[]{SoccerModule.m_cMatch.GetTeam(0), SoccerModule.m_cMatch.GetTeam(1)};
      GameObject.CrashLog("[mod] t=" + (int)fNow + " ballon " + (int)(BallDepth(0) * 100.0F) + " appels refuses/lances" + ModMovement.DebugSummary() + "\n");

      for (int t = 0; t < 2; t++) {
         for (int i = 0; i < tTeams[t].m_vFootBallers.size(); i++) {
            AIFootBaller fb = (AIFootBaller)tTeams[t].m_vFootBallers.elementAt(i);
            PlayerInfo pi = (PlayerInfo)m_tPlayers.get(fb);
            if (i == 0 && m_bOffside) {
               GameObject.CrashLog("[mod]   " + tTeams[t].m_sName + " ligne de hors-jeu " + (int)(OffsideLine(fb) * 100.0F) + "\n");
            }

            if (pi != null && fb.m_vValidPos != null) {
               FootBaller cMark = (FootBaller)m_tMarks.get(fb);
               GameObject.CrashLog("[mod]   " + tTeams[t].m_sName + " n" + fb.m_nNumberInTeam + " " + (pi.m_cRole != null ? pi.m_cRole.m_sId : "origine" + fb.m_nType)
                  + " pos " + (int)(Depth(fb.m_nTeam, fb.GetPosition()) * 100.0F) + "/" + (int)((fb.GetPosition().fX - GameField.vFieldLogicalStart.fX) / GameField.fLineOutTop * 100.0F)
                  + " cible " + (int)(Depth(fb.m_nTeam, fb.m_vValidPos) * 100.0F) + "/" + (int)((fb.m_vValidPos.fX - GameField.vFieldLogicalStart.fX) / GameField.fLineOutTop * 100.0F)
                  + (cMark != null ? " marque n" + cMark.m_nNumberInTeam : "") + ModMovement.Describe(fb) + (IsHumanPlayer(tTeams[t], fb) ? " (humain)" : "") + "\n");
            }
         }
      }
   }

   private static void LogField(AIMatch cMatch) {
      GameObject.CrashLog("[mod] terrain : debut " + GameField.vFieldLogicalStart + " longueur " + GameField.fLineEnd + " largeur " + GameField.fLineOutTop + "\n");

      for (int t = 0; t < 2; t++) {
         Team cTeam = cMatch.GetTeam(t);

         for (int i = 0; i < cTeam.m_vFootBallers.size(); i++) {
            AIFootBaller cFB = (AIFootBaller)cTeam.m_vFootBallers.elementAt(i);
            GameObject.CrashLog("[mod]   " + cTeam.m_sName + " n" + cFB.m_nNumberInTeam + " type " + cFB.m_nType
               + " depart " + (int)(SpawnDepth(cFB) * 100.0F) + " x " + (int)(SpawnX(cFB) * 100.0F) + "\n");
         }
      }
   }
}
