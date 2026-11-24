import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

// [mod] Multijoueur en ligne (notes/online.md).
//
// L'hôte simule le match comme un multi local : la manette de l'équipe de droite reçoit les touches du
// client (TeamController.Inject). Il diffuse l'état 30 fois par seconde, plus une file d'événements
// fiables (animations, effets, sons, HUD, commentateur). Le client est une marionnette : il envoie ses
// touches et applique ce qu'il reçoit.
//
// Tout le réseau passe par un DatagramChannel non bloquant, lu à chaque frame depuis SoccerGame.OnFrame :
// aucun thread Java ne touche le moteur.
class ModNet {
   static final int _MAGIC = 0x50534E31; // "PSN1"
   // Version du mod pour le jeu en ligne : à augmenter dès que les paquets ou les règles du match changent.
   // Le serveur ne met dans un même salon que deux jeux de même version.
   static final int _MOD_VERSION = 8; // 6 : ligue en ligne (match entre amis, but en or en playoffs) ; 7 : caméra propre au client ;
   // 8 : statistiques des matchs en ligne (PKT_MATCH_STATS), matchs des salons privés enregistrés par le serveur
   static final int _ROLE_NONE = 0;
   static final int _ROLE_HOST = 1;
   static final int _ROLE_CLIENT = 2;
   static final int _PKT_INPUT = 1; // client -> hôte
   static final int _PKT_SNAP = 2; // hôte -> client
   static final int _PKT_BYE = 3; // l'un des deux quitte le match
   static final int _PKT_PUNCH = 4; // sonde de la liaison directe : d'un jeu à l'autre, jamais par le serveur
   // Liaison directe (PunchFrame)
   static final long _PUNCH_FAST_MS = 250L; // entre deux sondes tant que la liaison n'est pas faite
   static final long _PUNCH_SLOW_MS = 1000L; // ensuite : elles gardent le passage ouvert dans les box
   static final long _PUNCH_GIVE_UP_MS = 10000L; // sans réponse après ce temps, les sondes ralentissent
   static final long _DIRECT_FRESH_MS = 3000L; // une sonde plus vieille ne prouve plus rien
   static final long _DIRECT_STALL_MS = 1500L; // client : sans état de l'hôte pendant ce temps, retour au relais
   static final long _DIRECT_HOLD_MS = 10000L; // ... et pas de nouvel essai avant ce délai
   // Événements fiables, hôte -> client
   static final int _EV_ANIM = 1;
   static final int _EV_FX = 2;
   static final int _EV_SOUND = 3;
   static final int _EV_HUD = 4;
   static final int _EV_SAY = 5;
   static final int _EV_SWAP = 6;
   static final int _EV_CROWD = 7;
   static final int _EV_WEATHER = 8;
   static final int _EV_STATS = 9;
   // Index d'objet : équipe d'origine * 16 + numéro, l'arbitre, le ballon, le leurre du tir double
   static final int _OBJ_REFEREE = 32;
   static final int _OBJ_BALL = 40;
   static final int _OBJ_DECOY = 41;
   // Sons (_EV_SOUND)
   static final int _SND_BALL = 0; // Ball.PlaySound
   static final int _SND_PLAYER = 1; // FootBaller.PlaySound
   static final int _SND_MODULE = 2; // SoccerModule.PlaySound
   static final int _SND_ONESHOT = 3; // AttachOneShotSound sur un joueur
   static final int _SND_VOICE = 4; // cri d'un joueur (Individual.StartSound), avec ses distances
   // Appels du HUD (_EV_HUD)
   static final int _HUD_LETTERS = 1;
   static final int _HUD_FLASH = 2;
   static final int _HUD_FLASH_END = 3;
   static final int _HUD_SWITCH = 4;
   static final int _HUD_EVENT = 5;
   static final int _HUD_SHOW_INFO = 6;
   static final int _HUD_HIDE_INFO = 7;
   static final int _MAX_PACKET = 1300;
   static final int _MAX_EVENT = 600;
   static final int _EVENT_RING = 512;
   static final int _INPUT_RING = 16;
   static final long _SNAP_MS = 33L;
   static final long _INPUT_MS = 33L;
   static final long _TIMEOUT_MS = 5000L;
   static final float _INTERP_DELAY = 0.1F; // le client affiche avec ce retard, en secondes
   static final int _SNAP_RING = 32;
   static final int _CAM_NONE = -1; // cible de la caméra : rien, un joueur (son index), le ballon, le viseur
   static final int _CAM_BALL = 100;
   static final int _CAM_SELECTION = 101;
   // État, octet A
   static final int _FA_HUD = 1;
   static final int _FA_REPLAY = 2;
   static final int _FA_MARKER = 4;
   static final int _FA_DECOY = 8;
   static final int _FA_SUPERS_ON = 16;
   static final int _FA_GOLD_GOAL = 32;
   static final int _FA_SUPER_SHOT = 64;
   static final int _FA_DEST_MARK = 128;
   // État, octet B
   static final int _FB_MAGNET = 1;
   static final int _FB_CRAZY = 2;
   static final int _FB_FROZEN_A = 4;
   static final int _FB_FROZEN_B = 8;
   static final int _FB_WALL_A = 16;
   static final int _FB_WALL_B = 32;
   static final int _FB_HALF_HOST = 64; // pause de mi-temps : l'hôte a cliqué « continuer »
   static final int _FB_HALF_PEER = 128; // ... le client aussi
   // Sons en boucle rejoués par le client d'après l'état
   static final int _LOOP_SHOT = 0;
   static final int _LOOP_MAGNET = 1;
   static final int _LOOP_CRAZY = 2;
   static final int _LOOP_DECOY = 3;
   static final int _LOOP_WALL_A = 4;
   static final int _LOOP_WALL_B = 5;
   static final int _LOOP_SPEED = 6;
   static final int _LOOP_FEINT = 7;
   static final int _LOOPS = 8;

   // Actions envoyées par le client, et leurs touches virtuelles côté hôte (hors des vraies touches)
   static final int _ACT_PASS = 0;
   static final int _ACT_SHOT = 1;
   static final int _ACT_FEINT = 2;
   static final int _ACT_SUPER = 3;
   static final int _ACT_UP = 4;
   static final int _ACT_RIGHT = 5;
   static final int _ACT_DOWN = 6;
   static final int _ACT_LEFT = 7;
   static final int[] _ACT_KEYS = new int[]{901, 902, 903, 904, 911, 912, 913, 914};
   // Demandes du client, jouées par le module de l'hôte
   static final int _ACT_SPACE = 8; // espace : passer l'intro, un ralenti, lancer la mi-temps
   static final int _ACT_ESC = 9; // échap : ouvrir ou fermer le menu pause
   static final int _ACT_CONTINUE = 10; // bouton « continuer » du menu
   // touches directes vers les joueurs 2..7 (ModSwitch) : _ACT_SEL + 0..5, jouées par l'hôte sur l'équipe de l'invité
   static final int _ACT_SEL = 11;

   static int m_nRole = _ROLE_NONE;
   static String m_sHost = null;
   static int m_nPort = 0;
   static DatagramChannel m_cChannel = null;
   static boolean m_bFailed = false;
   static boolean m_bResetLogged = false; // la coupure du pair a déjà été écrite au journal
   static boolean m_bLeft = false; // on a quitté le match : plus de réseau jusqu'à la fin du module
   static ByteBuffer m_cIn = ByteBuffer.allocate(2048);
   static ByteBuffer m_cOut = ByteBuffer.allocate(2048);
   static ByteBuffer m_cEv = ByteBuffer.allocate(1024);
   static SocketAddress m_cPeer = null;
   static long m_lLastRecv = 0L;
   static int m_nSession = 0; // tiré par le client ; l'hôte repart de zéro quand il change
   static Team[] m_tTeam = new Team[2]; // équipes dans l'ordre du début du match (elles changent de côté à la mi-temps)
   static boolean m_bApplying = false; // vrai pendant que le client applique ce qui vient de l'hôte
   static boolean m_bStarted = false; // le match de ce module a démarré (Match.StartMatch)
   static int m_nDebugHalf = 0; // debugconf.scr : NetHalf(secondes), mi-temps courte pour les tests
   static int m_nHalfSeconds = 0; // durée d'une mi-temps choisie dans le salon (0 : celle du profil de l'hôte)

   // --- liaison directe (match d'un salon) : les deux jeux se parlent sans passer par le serveur ---
   static ByteBuffer m_cPunch = ByteBuffer.allocate(32);
   static SocketAddress m_cDirect = null; // adresse d'où viennent les sondes de l'autre jeu
   static long m_lDirectSeen = 0L; // dernière sonde reçue de lui
   static long m_lDirectAck = 0L; // dernière sonde où il disait recevoir les miennes : le chemin marche dans les deux sens
   static long m_lNextPunch = 0L;
   static long m_lPunchStart = 0L;
   static long m_lDirectHold = 0L; // pas de liaison directe avant cette heure
   static long m_lDirectSince = 0L;
   static boolean m_bOnDirect = false; // les paquets du match passent en direct

   // --- pause de mi-temps en ligne : le match reprend quand les deux joueurs ont cliqué « continuer », ou à la fin
   // du temps (HalfBreak de online.scr) ---
   static long m_lHalfEnd = 0L; // hôte : heure de la reprise, 0 hors de la pause
   static boolean m_bHalfHostOk = false;
   static boolean m_bHalfPeerOk = false;
   static int m_nHalfLeft = -1; // client : secondes restantes d'après l'hôte, -1 hors de la pause
   static int m_nHalfFlags = 0; // client : drapeaux _FB_HALF_* du dernier état
   static int m_nHalfShown = -2; // ce que le menu affiche
   static boolean m_bNoSkip = false; // debugconf.scr : NetNoSkip(1), les jeux automatiques laissent la pause aller au bout

   // --- hôte ---
   static int m_nSnapSeq = 0;
   static long m_lNextSnap = 0L;
   static int m_nInputApplied = 0; // dernier changement de touche du client déjà joué
   static byte[][] m_tEvents = new byte[_EVENT_RING][];
   static int m_nEventNext = 1; // numéro du prochain événement
   static int m_nEventAcked = 0; // dernier événement reçu par le client
   static String[] m_tLastAnim = new String[40]; // dernière animation de chaque objet, pour un client qui arrive
   static int[] m_tLastRep = new int[40];
   static int m_nCamTarget = _CAM_BALL;
   static boolean m_bHudShown = true; // SoccerModule.ShowHud / HideHud
   static float m_fSpeedUp = 1.0F; // dernier TimerSetSpeedUp (ralentis)
   static boolean m_bSaid = false; // le commentateur parle (pour n'envoyer qu'un seul « stop »)
   static boolean m_bWaitMenu = false; // menu ouvert par nous : en attente de l'adversaire
   static boolean m_bLocalMenu = false; // menu ouvert par échap chez ce joueur seulement : le match continue
   static boolean m_bWaitShown = false; // ce menu a déjà été proposé depuis la dernière connexion
   static boolean m_bEverConnected = false;
   static int m_nLoggedMenu = 0;
   static int m_nLoggedScore = 0;
   static boolean m_bLoggedReplay = false;

   // --- client ---
   static byte[] m_tInput = new byte[_INPUT_RING];
   static int m_nInputSeq = 0; // numéro du dernier changement de touche
   static long m_lNextInput = 0L;
   static int m_nEventApplied = -1; // -1 : pas encore calé sur l'hôte
   static NetSnap[] m_tSnaps = new NetSnap[_SNAP_RING];
   static int m_nSnapCount = 0; // nombre d'états reçus
   static int m_nLastSnapSeq = 0;
   static float m_fTimeOffset = 0.0F; // horloge de l'hôte - horloge locale
   static boolean m_bOffsetSet = false;
   static boolean m_bPuppets = false; // les objets du match ont été figés
   static int m_nShownScoreLeft = -1;
   static int m_nShownScoreRight = -1;
   static int m_nShownSecond = -1;
   static int m_nShownCam = -99;
   static float[] m_tCam = new float[6]; // caméra interpolée : position, direction
   static boolean m_bCamReady = false;
   static boolean m_bCamFree = false; // la caméra de l'hôte suit sa cible : le client calcule la sienne
   static boolean m_bShownCamReplay = false;
   static int m_nShownHud = -1;
   static int m_nShownReplay = -1;
   static int m_nShownMarker = -1;
   static int m_nShownDest = -1;
   static int m_nShownDecoy = -1;
   static int m_nShownFrozen = 0;
   static int m_nShownFade = -1;
   static int m_nShownSpeed = 100;
   static int m_nShownMenu = 0;
   static boolean m_bHostSupers = true;
   static boolean m_bHostLost = false; // plus de nouvelles de l'hôte
   static boolean m_bHostLeft = false; // l'hôte a quitté le match
   static ControlObject[] m_tLoopObj = new ControlObject[_LOOPS];
   static int[] m_tLoopId = new int[_LOOPS];
   static String[][] m_tStats = null; // statistiques du menu, calculées par l'hôte
   static Vector m_vTmpPos = new Vector();
   static Vector m_vTmpDir = new Vector();
   static Vector m_vZero = new Vector();
   static Vector m_vWhite = new Vector(1.0F, 1.0F, 1.0F);
   static boolean m_bBot = false;
   static boolean m_bBotTactics = false; // le client automatique a déjà changé de schéma dans ce match
   static long m_lBotNext = 0L;
   static int m_nBotStep = 0;
   static float m_fBotClock = 0.0F; // test du menu local de l'hôte : horloge à l'ouverture
   static int m_nBotDir = -1;

   // --- suivi ---
   static long m_lNextStat = 0L;
   static int m_nStatIn = 0;
   static int m_nStatOut = 0;
   static int m_nStatEvents = 0;

   static void Log(String s) {
      GameObject.CrashLog("[mod] net " + s + "\n");
   }

   static boolean IsHost() {
      return m_nRole == _ROLE_HOST;
   }

   static boolean IsClient() {
      return m_nRole == _ROLE_CLIENT;
   }

   static boolean IsOnline() {
      return m_nRole != _ROLE_NONE;
   }

   // En ligne, l'équipe de ce jeu : celle de gauche au coup d'envoi pour l'hôte, de droite pour le client.
   // Ne pas comparer le côté (GetTeam(n)) : les équipes changent de côté à la mi-temps.
   static boolean IsMyTeam(Team cTeam) {
      return cTeam != null && cTeam == m_tTeam[m_nRole == _ROLE_CLIENT ? 1 : 0];
   }

   // debugconf.scr : NetHost(port)
   static void SetHost(int nPort) {
      m_nRole = _ROLE_HOST;
      m_nPort = nPort;
      Log("role hote, port " + nPort);
   }

   // debugconf.scr : NetJoin("ip", port)
   static void SetJoin(String sHost, int nPort) {
      m_nRole = _ROLE_CLIENT;
      m_sHost = sHost;
      m_nPort = nPort;
      m_nSession = (int)(System.currentTimeMillis() & 0x7FFFFFFFL);
      if (m_nSession == 0) {
         m_nSession = 1;
      }

      Log("role client, hote " + sHost + ":" + nPort + ", session " + m_nSession);
   }

   static boolean Open() {
      if (m_cChannel != null) {
         return true;
      }

      if (m_bFailed) {
         return false;
      }

      try {
         if (m_nRole == _ROLE_HOST) {
            if (!OpenChannel(m_nPort)) {
               m_bFailed = true;
               return false;
            }

            Log("ecoute sur le port UDP " + m_nPort);
         } else {
            // Port local libre : sous Windows, lire une socket jamais liée échoue (« Invalid argument »)
            if (!OpenChannel(0)) {
               m_bFailed = true;
               return false;
            }

            m_cPeer = new InetSocketAddress(m_sHost, m_nPort);
            Log("canal ouvert vers " + m_cPeer);
         }

         return true;
      } catch (Throwable e) {
         Log("ECHEC ouverture : " + e);
         m_bFailed = true;
         CloseChannel();
         return false;
      }
   }

   // Ouvre le canal UDP sur un port local (0 : n'importe lequel). Sert aussi à ModLobby, pour parler au serveur.
   static boolean OpenChannel(int nPort) {
      if (m_cChannel != null) {
         return true;
      }

      try {
         m_cChannel = DatagramChannel.open();
         m_cChannel.configureBlocking(false);
         m_cChannel.socket().bind(new InetSocketAddress(nPort));
         m_bResetLogged = false;
         return true;
      } catch (Throwable e) {
         Log("ECHEC ouverture du canal : " + e);
         CloseChannel();
         return false;
      }
   }

   static void CloseChannel() {
      try {
         if (m_cChannel != null) {
            m_cChannel.close();
         }
      } catch (Throwable e) {
      }

      m_cChannel = null;
   }

   // ModLobby.BeginMatch : un match commence dans un salon. Les paquets partent par le serveur, qui relaie, puis en
   // direct si la liaison se fait (PunchFrame).
   static void StartMatch(boolean bHost, SocketAddress cServer, int nHalfSeconds) {
      m_nRole = bHost ? _ROLE_HOST : _ROLE_CLIENT;
      m_bLeft = false;
      m_bFailed = false;
      m_lLastRecv = 0L;
      m_nHalfSeconds = nHalfSeconds;
      DirectReset();
      m_lPunchStart = 0L;
      m_lDirectHold = 0L;
      m_lHalfEnd = 0L;
      m_nHalfLeft = -1;
      m_nHalfFlags = 0;
      // hôte
      m_nSnapSeq = 0;
      m_lNextSnap = 0L;
      m_nInputApplied = 0;
      m_nEventNext = 1;
      m_nEventAcked = 0;
      m_bEverConnected = false;
      m_nLoggedScore = 0;
      m_bLoggedReplay = false;
      // client
      m_nInputSeq = 0;
      m_lNextInput = 0L;
      m_nEventApplied = -1;
      m_nLastSnapSeq = 0;
      m_bHostLeft = false;
      m_nBotStep = 0;
      m_nBotDir = -1;
      m_lBotNext = 0L;
      m_bBotTactics = false;
      if (bHost) {
         m_cPeer = null; // connu au premier paquet du client
         m_nSession = 0;
      } else {
         m_cPeer = cServer;
         m_nSession = (int)(System.currentTimeMillis() & 0x7FFFFFFFL);
         if (m_nSession == 0) {
            m_nSession = 1;
         }
      }

      Log("match en salon, " + (bHost ? "hote" : "client, session " + m_nSession));
   }

   // ModTactics.GetTeamComposition : chez l'hôte d'un salon, composition envoyée par l'invité pour son équipe
   static ModTactics.Composition RemoteComposition(Team cTeam) {
      if (m_nRole != _ROLE_HOST || cTeam == null || cTeam != m_tTeam[1] || !ModLobby.IsOpen() || ModLobby.m_cRemoteComp == null) {
         return null;
      }

      return ModLobby.m_cRemoteComp.Copy();
   }

   // Appelé à chaque frame (menus et match) par SoccerGame.OnFrame
   static void OnFrame() {
      boolean bLobby = ModLobby.IsOpen(); // canal ouvert vers le serveur : salon, puis match relayé
      if (m_nRole == _ROLE_NONE && !bLobby) {
         if (m_sTestHost != null && !m_bTestDone) {
            TestFrame();
         }

         return;
      }

      if (!bLobby && m_bLeft || !Open()) {
         return;
      }

      long lNow = System.currentTimeMillis();

      try {
         Receive(lNow);
         if (bLobby) {
            ModLobby.OnFrame(lNow);
         }

         if (!m_bLeft) {
            if (bLobby && m_nRole != _ROLE_NONE) {
               PunchFrame(lNow);
            }

            if (m_nRole == _ROLE_HOST) {
               HostFrame(lNow);
            } else if (m_nRole == _ROLE_CLIENT) {
               ClientSend(lNow);
            }

            if (m_nRole != _ROLE_NONE) {
               HalfBreakShow();
            }
         }
      } catch (Throwable e) {
         Log("ERREUR frame : " + e);
      }

      if (lNow >= m_lNextStat) {
         if (m_lNextStat != 0L && SoccerGame.m_bDebug) {
            // chez l'hôte en match : où en est le match (état, horloge, dernier arrêt de jeu), pour suivre un blocage
            String sMatch = "";
            if (m_nRole == _ROLE_HOST && !m_bLeft && MatchReady()) {
               SoccerModule cModule = SoccerGame.activeLevelModule;
               sMatch = ", etat " + AIMatch.m_nMatchState + ", horloge " + (int)cModule.m_Clock.m_fTime + (cModule.m_Clock.m_bStop ? " (arretee)" : "")
                  + (Match.m_bPlaying ? ", ralenti" : "") + (cModule.m_bMenuVisible ? ", menu" : "") + ", arret de jeu :" + SoccerModule.m_cMatch.m_sDB_SpecialBallPoint;
            }

            Log("stat : recus " + m_nStatIn + ", envoyes " + m_nStatOut + ", evenements " + m_nStatEvents + " en 5 s, pair " + m_cPeer
               + (m_nRole != _ROLE_NONE && bLobby ? (m_bOnDirect ? " (direct)" : " (relais)") : "") + sMatch);
         }

         m_nStatIn = 0;
         m_nStatOut = 0;
         m_nStatEvents = 0;
         m_lNextStat = lNow + 5000L;
      }
   }

   static void Receive(long lNow) throws java.io.IOException {
      int nErrors = 0;

      for (int i = 0; i < 64; i++) {
         m_cIn.clear();
         SocketAddress cFrom = null;

         try {
            cFrom = m_cChannel.receive(m_cIn);
         } catch (java.io.IOException e) {
            // Windows : un port à qui on a écrit ne répond pas (« Connection reset by peer ») : l'autre jeu est
            // fermé, ou une sonde de la liaison directe est partie vers une adresse sans personne.
            // Ce n'est pas une panne : le délai sans paquets fera le reste, et les paquets suivants se lisent.
            if (!m_bResetLogged) {
               m_bResetLogged = true;
               Log("un port ne repond pas : " + e.getMessage());
            }

            if (++nErrors > 4 || m_cChannel == null || !m_cChannel.isOpen()) {
               break;
            }

            continue;
         }

         // Le JRE 1.4.0 du jeu renvoie une adresse même quand rien n'est arrivé : seul le nombre
         // d'octets lus dit s'il y a un paquet
         if (cFrom == null || m_cIn.position() == 0) {
            break;
         }

         m_cIn.flip();
         if (m_cIn.remaining() < 5 || m_cIn.getInt() != _MAGIC) {
            continue;
         }

         int nType = m_cIn.get();
         m_nStatIn++;
         // Dans un salon, seuls le serveur et l'autre joueur (liaison directe) sont écoutés
         if (ModLobby.IsOpen() && !cFrom.equals(ModLobby.m_cServer)) {
            // debugconf.scr, NetDirect(2) : le chemin direct casse 20 s après le début du match (test du retour au relais)
            if (ModLobby.m_nDebugDirect == 2 && m_lPunchStart != 0L && lNow - m_lPunchStart > 20000L) {
               continue;
            }

            if (nType == _PKT_PUNCH) {
               ReadPunch(cFrom, lNow);
               continue;
            }

            // Un paquet de match venu d'ailleurs que du serveur : de l'adresse des sondes, ou de la session en cours
            if (nType >= ModLobby._PKT_FIRST || m_nRole == _ROLE_NONE || !ModLobby.CanLink()) {
               continue;
            }

            if (!cFrom.equals(m_cDirect) && (m_nSession == 0 || m_cIn.remaining() < 4 || m_cIn.getInt(m_cIn.position()) != m_nSession)) {
               continue;
            }
         } else if (nType == _PKT_PUNCH) {
            continue;
         }

         try {
            if (nType >= ModLobby._PKT_FIRST) {
               if (ModLobby.IsOpen()) {
                  ModLobby.OnPacket(nType, m_cIn);
               }
            } else if (m_bLeft || m_nRole == _ROLE_NONE) {
               // paquet d'un match déjà quitté
            } else if (nType == _PKT_BYE) {
               ReadBye();
            } else if (m_nRole == _ROLE_HOST && nType == _PKT_INPUT) {
               HostReadInput(cFrom, lNow);
            } else if (m_nRole == _ROLE_CLIENT && nType == _PKT_SNAP) {
               ClientReadSnap(lNow);
            }
         } catch (RuntimeException e) {
            Log("paquet illisible (type " + nType + ") : " + e);
         }
      }
   }

   static void Send() throws java.io.IOException {
      m_cOut.flip();
      m_cChannel.send(m_cOut, m_cPeer);
      m_nStatOut++;
   }

   // ------------------------------------------------------------------ liaison directe
   //
   // Le serveur donne à chaque joueur d'un salon l'adresse de l'autre (ModLobby.m_cPeerPublic, m_cPeerLocal) et une
   // clé. Pendant le match, chaque jeu envoie des sondes à ces adresses, par le canal qui parle déjà au serveur : sa
   // box laisse alors passer les réponses, et les sondes de l'autre finissent par arriver. Une sonde dit aussi « je
   // reçois les tiennes » : quand c'est vrai des deux côtés, le chemin marche dans les deux sens. Le client envoie
   // alors ses touches en direct, et l'hôte répond toujours par où les touches arrivent. Si le chemin direct se
   // tait, le client repasse par le relais du serveur, qui reste ouvert tout le match.

   static void DirectReset() {
      m_cDirect = null;
      m_lDirectSeen = 0L;
      m_lDirectAck = 0L;
      m_lNextPunch = 0L;
      m_bOnDirect = false;
   }

   static boolean DirectUp(long lNow) {
      return m_cDirect != null && lNow - m_lDirectSeen < _DIRECT_FRESH_MS && lNow - m_lDirectAck < _DIRECT_FRESH_MS && lNow >= m_lDirectHold;
   }

   static void SendPunch(SocketAddress cTo, boolean bSeen) {
      ByteBuffer b = m_cPunch;
      b.clear();
      b.putInt(_MAGIC);
      b.put((byte)_PKT_PUNCH);
      b.putInt(ModLobby.m_nLinkKey);
      b.putInt(ModLobby.m_nLastMatchId);
      b.put((byte)(bSeen ? 1 : 0));
      b.flip();

      try {
         m_cChannel.send(b, cTo);
      } catch (Throwable e) {
         // adresse injoignable d'ici : le relais reste
      }
   }

   static void ReadPunch(SocketAddress cFrom, long lNow) {
      if (m_cIn.remaining() < 9 || m_nRole == _ROLE_NONE || m_bLeft || !ModLobby.CanLink()) {
         return;
      }

      int nKey = m_cIn.getInt();
      int nMatch = m_cIn.getInt();
      int nFlags = m_cIn.get();
      if (nKey != ModLobby.m_nLinkKey || nMatch != ModLobby.m_nLastMatchId) {
         return;
      }

      if (!cFrom.equals(m_cDirect)) {
         // Ses sondes peuvent arriver par deux chemins (réseau local et box) : tant que l'un marche, on le garde
         if (m_cDirect != null && lNow - m_lDirectSeen < _DIRECT_FRESH_MS) {
            return;
         }

         m_cDirect = cFrom;
         m_lDirectAck = 0L;
         Log("liaison directe : sonde recue de " + cFrom);
      }

      m_lDirectSeen = lNow;
      if ((nFlags & 1) != 0) {
         m_lDirectAck = lNow;
      }
   }

   // Chaque frame d'un match de salon : envoie les sondes, et choisit chez le client le chemin des paquets du match
   static void PunchFrame(long lNow) {
      if (!ModLobby.CanLink()) {
         // refusée dans online.scr, ou le serveur n'a pas d'adresse à donner (autre joueur parti, chemins différents)
         if (m_cDirect != null || m_bOnDirect) {
            if (m_bOnDirect) {
               Log("liaison directe fermee : relais par le serveur");
            }

            DirectReset();
         }

         if (m_nRole == _ROLE_CLIENT) {
            m_cPeer = ModLobby.m_cServer;
         }

         return;
      }

      boolean bUp = DirectUp(lNow);
      if (m_lPunchStart == 0L) {
         m_lPunchStart = lNow;
      }

      if (lNow >= m_lNextPunch) {
         m_lNextPunch = lNow + (bUp ? _PUNCH_SLOW_MS : (lNow - m_lPunchStart < _PUNCH_GIVE_UP_MS ? _PUNCH_FAST_MS : 2L * _PUNCH_SLOW_MS));
         boolean bSeen = m_cDirect != null && lNow - m_lDirectSeen < _DIRECT_FRESH_MS;
         if (bSeen) {
            SendPunch(m_cDirect, true);
         }

         // tant que la liaison n'est pas faite, on essaie toutes les adresses connues de l'autre joueur
         if (!bUp) {
            SocketAddress cPublic = ModLobby.m_cPeerPublic;
            SocketAddress cLocal = ModLobby.m_cPeerLocal;
            if (cPublic != null && !(bSeen && cPublic.equals(m_cDirect))) {
               SendPunch(cPublic, bSeen);
            }

            if (cLocal != null && !cLocal.equals(cPublic) && !(bSeen && cLocal.equals(m_cDirect))) {
               SendPunch(cLocal, bSeen);
            }
         }
      }

      if (m_nRole != _ROLE_CLIENT) {
         return;
      }

      // Client : en direct tant que les sondes passent et que les états de l'hôte arrivent, sinon par le serveur
      if (m_bOnDirect && bUp && m_lLastRecv != 0L && lNow - m_lLastRecv > _DIRECT_STALL_MS && lNow - m_lDirectSince > _DIRECT_STALL_MS) {
         m_lDirectHold = lNow + _DIRECT_HOLD_MS;
         bUp = false;
         Log("liaison directe muette depuis " + (lNow - m_lLastRecv) + " ms");
      }

      if (bUp != m_bOnDirect) {
         m_bOnDirect = bUp;
         m_lDirectSince = lNow;
         Log(bUp ? "liaison directe avec " + m_cDirect : "relais par le serveur");
      }

      m_cPeer = m_bOnDirect ? m_cDirect : ModLobby.m_cServer;
   }

   // ModLobby.SendReport : mon terrain est chargé et j'envoie des paquets de match à l'autre joueur. En liaison
   // directe le serveur ne les voit plus passer : c'est ce que je lui dis qui lui apprend que le match se joue.
   static boolean IsSending() {
      if (m_bLeft || !MatchReady()) {
         return false;
      }

      return m_nRole == _ROLE_CLIENT || m_nRole == _ROLE_HOST && m_cPeer != null;
   }

   // ------------------------------------------------------------------ pause de mi-temps

   // Secondes avant la reprise, -1 hors de la pause
   static int HalfBreakLeft() {
      if (m_nRole == _ROLE_HOST) {
         return m_lHalfEnd == 0L || m_bLeft ? -1 : (int)Math.max(0L, (m_lHalfEnd - System.currentTimeMillis() + 999L) / 1000L);
      }

      return m_nRole == _ROLE_CLIENT && m_nShownMenu == 2 && !m_bHostLost ? m_nHalfLeft : -1;
   }

   // J'ai cliqué « continuer »
   static boolean HalfBreakMine() {
      return m_nRole == _ROLE_HOST ? m_bHalfHostOk : (m_nHalfFlags & _FB_HALF_PEER) != 0;
   }

   // L'autre joueur a cliqué « continuer »
   static boolean HalfBreakTheirs() {
      return m_nRole == _ROLE_HOST ? m_bHalfPeerOk : (m_nHalfFlags & _FB_HALF_HOST) != 0;
   }

   // Le menu de mi-temps montre le temps restant et qui est prêt : remis à jour quand cela change. Le temps du
   // module est arrêté pendant ce menu, d'où ce suivi par frame plutôt que par un CallMethodEvery.
   static void HalfBreakShow() {
      SoccerModule cModule = SoccerGame.activeLevelModule;
      if (cModule == null || cModule.m_cMenuInGameModule == null || cModule.m_cMenuInGameModule.m_cMenuInGame == null) {
         return;
      }

      int nLeft = HalfBreakLeft();
      int nKey = nLeft < 0 ? -1 : nLeft * 4 + (HalfBreakMine() ? 1 : 0) + (HalfBreakTheirs() ? 2 : 0);
      if (nKey != m_nHalfShown) {
         m_nHalfShown = nKey;
         cModule.m_cMenuInGameModule.m_cMenuInGame.ShowHalfBreak();
      }
   }

   // Hôte, chaque frame : ouvre la pause avec le menu de mi-temps, et la ferme quand les deux joueurs sont prêts
   // ou que le temps est écoulé
   static void HostHalfBreak(SoccerModule cModule, long lNow) {
      if (HostMenuState(cModule) != 2) {
         m_lHalfEnd = 0L;
         return;
      }

      if (m_lHalfEnd == 0L) {
         int nSeconds = ModLobby.GetHalfBreak();
         if (nSeconds <= 0) {
            return; // pas de limite : le premier « continuer » ferme le menu, comme hors ligne
         }

         m_lHalfEnd = lNow + 1000L * nSeconds;
         m_bHalfHostOk = false;
         m_bHalfPeerOk = false;
         Log("mi-temps : pause de " + nSeconds + " s");
      }

      boolean bBoth = m_bHalfHostOk && (m_bHalfPeerOk || m_cPeer == null);
      if (!bBoth && lNow < m_lHalfEnd) {
         return;
      }

      Log(bBoth ? "mi-temps : les deux joueurs continuent" : "mi-temps : temps ecoule, reprise");
      m_lHalfEnd = 0L;
      // un écran Tactique encore ouvert garde ce qui y est réglé
      cModule.m_cMenuInGameModule.m_cMenuTactics.CommitIfOpen();
      cModule.m_cMenuInGameModule.m_cMenuYesNo.Hide();
      cModule.HideMenu();
   }

   // « Continuer » ou échap de l'hôte pendant la pause de mi-temps : il est prêt, le menu se fermera quand le
   // client le sera aussi. Renvoie vrai si la pause est en cours.
   static boolean HostHalfSkip() {
      if (m_nRole != _ROLE_HOST || m_bLeft || m_lHalfEnd == 0L) {
         return false;
      }

      if (!m_bHalfHostOk) {
         m_bHalfHostOk = true;
         Log("mi-temps : l'hote est pret");
      }

      return true;
   }

   // ModLobby.ReadPeer : l'invité a changé sa composition (mi-temps)
   static void HostRemoteTactics() {
      if (m_nRole == _ROLE_HOST && !m_bLeft && MatchReady()) {
         ModTactics.ApplyTeams(SoccerModule.m_cMatch);
         Log("composition de l'invite appliquee");
      }
   }

   static boolean MatchReady() {
      if (SoccerGame.activeLevelModule == null || SoccerModule.m_cMatch == null || SoccerModule.m_cHud == null) {
         return false;
      }

      AIMatch cMatch = SoccerModule.m_cMatch;
      if (m_tTeam[0] == null || m_tTeam[1] == null || cMatch.m_cReferee == null || GameField.m_cBall == null) {
         return false;
      }

      // m_bInited retombe à faux pendant un ralenti (Match.Replay) : seul le premier passage compte
      if (cMatch.m_bInited) {
         m_bStarted = true;
      }

      return m_bStarted;
   }

   // Test1.InitTeams : les deux équipes existent. On retient leur ordre de départ, qui sert d'index réseau.
   static void OnTeamsReady(AIMatch cMatch) {
      m_tTeam[0] = cMatch.GetTeam(0);
      m_tTeam[1] = cMatch.GetTeam(1);
      if (m_nRole == _ROLE_HOST && m_nDebugHalf > 0) {
         cMatch.m_nHalfTime = m_nDebugHalf;
         Log("mi-temps de test : " + m_nDebugHalf + " s");
      } else if (m_nRole == _ROLE_HOST && m_nHalfSeconds > 0) {
         cMatch.m_nHalfTime = m_nHalfSeconds; // durée choisie par l'hôte dans le salon
      }
   }

   // Le match de l'hôte a-t-il les équipes inversées par rapport au début (seconde mi-temps) ?
   static boolean IsSwapped() {
      return SoccerModule.m_cMatch != null && m_tTeam[0] != null && SoccerModule.m_cMatch.GetTeam(0) != m_tTeam[0];
   }

   // Index réseau d'un joueur : équipe d'origine * 16 + numéro, _OBJ_REFEREE pour l'arbitre, -1 si inconnu
   static int ObjIndex(FootBaller cFB) {
      if (m_nRole == _ROLE_NONE || cFB == null) {
         return -1;
      }

      if (cFB instanceof AIReferee) {
         return _OBJ_REFEREE;
      }

      if (cFB.m_cTeam == null || cFB.m_nNumberInTeam < 0 || cFB.m_nNumberInTeam > 15) {
         return -1;
      }

      if (cFB.m_cTeam == m_tTeam[0]) {
         return cFB.m_nNumberInTeam;
      }

      if (cFB.m_cTeam == m_tTeam[1]) {
         return 16 + cFB.m_nNumberInTeam;
      }

      return -1;
   }

   static FootBaller ObjFromIndex(int nIndex) {
      AIMatch cMatch = SoccerModule.m_cMatch;
      if (cMatch == null) {
         return null;
      }

      if (nIndex == _OBJ_REFEREE) {
         return cMatch.m_cReferee;
      }

      if (nIndex < 0 || nIndex >= 32) {
         return null;
      }

      Team cTeam = m_tTeam[nIndex / 16];
      if (cTeam == null || nIndex % 16 >= cTeam.GetFootBallerCount()) {
         return null;
      }

      return cTeam.GetFootBaller(nIndex % 16);
   }

   // Réglage « supers » qui vaut pour ce match : celui de l'hôte
   static boolean SupersOn() {
      return m_nRole == _ROLE_CLIENT ? m_bHostSupers : CoreLogic.GetPlayer().m_bSupersOn;
   }

   // SoccerModule.CallSyncMethod, Ball.TimerSetSpeedUp, Recorder.TimerSetSpeedUp, MenuInGame.Quit
   static void OnSpeedUp(float fSpeedUp) {
      if (m_nRole != _ROLE_CLIENT) {
         m_fSpeedUp = fSpeedUp;
      }
   }

   // Texte à afficher dans le menu du match à la place de la mi-temps en cours, ou null
   static String MenuMessage() {
      if (m_nRole == _ROLE_HOST && m_bWaitMenu) {
         return m_bEverConnected ? "&OnlineLost&" : "&OnlineWaiting&";
      }

      if (m_nRole == _ROLE_CLIENT && m_bHostLost) {
         return "&OnlineHostLost&";
      }

      return null;
   }

   // L'autre joueur n'est plus dans le match : jamais arrivé, parti, ou muet depuis 5 s (ModLobby.OnMatchQuit)
   static boolean PeerGone() {
      return m_nRole == _ROLE_HOST ? m_cPeer == null : m_bHostLost || m_bHostLeft;
   }

   // MenuInGame.Quit : on prévient l'autre, puis plus de réseau
   static void OnQuit() {
      if (m_nRole == _ROLE_NONE || m_bLeft) {
         return;
      }

      m_bLeft = true;
      Log("match quitte");
      if (m_nRole == _ROLE_HOST) {
         HostReleaseKeys();
      }

      try {
         if (m_cChannel != null && m_cPeer != null) {
            for (int i = 0; i < 3; i++) {
               m_cOut.clear();
               m_cOut.putInt(_MAGIC);
               m_cOut.put((byte)_PKT_BYE);
               m_cOut.putInt(m_nSession);
               Send();
            }
         }
      } catch (Throwable e) {
         Log("ERREUR depart : " + e);
      }

      if (m_nRole == _ROLE_HOST) {
         m_cPeer = null;
      }
   }

   static void ReadBye() {
      if (m_cIn.getInt() != m_nSession) {
         return;
      }

      if (m_nRole == _ROLE_HOST) {
         if (m_cPeer != null) {
            Log("le client a quitte le match");
            HostDropPeer();
         }
      } else if (!m_bHostLeft) {
         Log("l'hote a quitte le match");
         m_bHostLeft = true;
      }
   }

   // ------------------------------------------------------------------ hôte

   static void HostDropPeer() {
      m_cPeer = null;
      m_nSession = 0;
      HostReleaseKeys();
   }

   static void HostReadInput(SocketAddress cFrom, long lNow) {
      int nSession = m_cIn.getInt();
      int nAckEvent = m_cIn.getInt();
      int nLastSeq = m_cIn.getInt();
      int nCount = m_cIn.get() & 0xFF;
      if (nSession != m_nSession) {
         // nouveau client (ou client revenu) : on repart de zéro et on lui redonne l'état des événements
         m_nSession = nSession;
         m_nInputApplied = nLastSeq; // ses anciennes touches ne sont pas rejouées
         m_nEventAcked = m_nEventNext - 1;
         m_cPeer = cFrom;
         Log("client " + cFrom + ", session " + nSession);
         HostReleaseKeys();
         HostResync();
         m_bEverConnected = true;
         m_bWaitShown = false;
         if (m_bWaitMenu) {
            m_bWaitMenu = false;
            SoccerModule cModule = SoccerGame.activeLevelModule;
            if (cModule != null && cModule.m_bMenuVisible && !cModule.m_bMatchEnd) {
               cModule.HideMenu();
            }
         }
      }

      m_cPeer = cFrom; // l'hôte répond par où les touches arrivent : en direct ou par le serveur, au choix du client
      m_lLastRecv = lNow;
      if (ModLobby.IsOpen()) {
         boolean bDirect = !cFrom.equals(ModLobby.m_cServer);
         if (bDirect != m_bOnDirect) {
            m_bOnDirect = bDirect;
            Log(bDirect ? "liaison directe avec " + cFrom : "relais par le serveur");
         }
      }

      if (nAckEvent > m_nEventAcked && nAckEvent < m_nEventNext) {
         m_nEventAcked = nAckEvent;
      }

      // Le paquet porte les nCount derniers changements, numérotés jusqu'à nLastSeq
      for (int i = 0; i < nCount; i++) {
         int nCode = m_cIn.get() & 0xFF;
         int nSeq = nLastSeq - nCount + 1 + i;
         if (nSeq > m_nInputApplied) {
            m_nInputApplied = nSeq;
            HostApplyKey(nCode & 0x7F, (nCode & 0x80) != 0);
         }
      }
   }

   // Événements qui décrivent un état durable, pour un client qui arrive en cours de match
   static void HostResync() {
      if (!MatchReady()) {
         return;
      }

      AIMatch cMatch = SoccerModule.m_cMatch;
      HostQueueSwap(IsSwapped(), cMatch.m_nHalfNo);

      for (int i = 0; i < m_tLastAnim.length; i++) {
         if (m_tLastAnim[i] != null) {
            HostQueueAnim(i, m_tLastAnim[i], m_tLastRep[i]);
         }
      }

      for (int s = 0; s < 2; s++) {
         Team cTeam = cMatch.GetTeam(s);
         HostQueueHud(_HUD_LETTERS, cTeam.m_nSuperFillCount, cTeam.m_nSuperType, s);
      }

      HostQueueHud(_HUD_SWITCH, cMatch.m_nPlayer1Team, 0, 0);
   }

   static TeamController HostRemoteController() {
      if (!MatchReady()) {
         return null;
      }

      TeamController cController = m_tTeam[1].GetTeamController();
      return cController != null && cController.m_bRemote ? cController : null;
   }

   static void HostApplyKey(int nAction, boolean bState) {
      if (nAction >= _ACT_SEL && nAction < _ACT_SEL + ModSwitch._NUMBERS) { // [mod] touche directe de l'invité
         TeamController cRemote = HostRemoteController();
         if (bState && cRemote != null && cRemote.m_cTeam != null && MatchReady()) {
            ModSwitch.Select(cRemote.m_cTeam, nAction - _ACT_SEL + ModSwitch._FIRST_NUMBER);
         }

         return;
      }

      if (nAction >= _ACT_SPACE) {
         if (bState) {
            HostRequest(nAction);
         }

         return;
      }

      TeamController cController = HostRemoteController();
      if (cController != null && nAction >= 0 && nAction < _ACT_KEYS.length) {
         cController.Inject(_ACT_KEYS[nAction], bState);
      }
   }

   // Espace, échap et « continuer » du client : joués par le module de l'hôte, comme ses propres touches
   static void HostRequest(int nAction) {
      SoccerModule cModule = SoccerGame.activeLevelModule;
      if (cModule == null || !MatchReady()) {
         return;
      }

      if (SoccerGame.m_bDebug) {
         Log("demande du client : " + nAction);
      }

      // Pause de mi-temps : « continuer » (ou échap) du client dit qu'il est prêt, le menu reste ouvert
      if (m_lHalfEnd != 0L && (nAction == _ACT_CONTINUE || nAction == _ACT_ESC)) {
         if (!m_bHalfPeerOk) {
            m_bHalfPeerOk = true;
            Log("mi-temps : le client est pret");
         }

         return;
      }

      // Hors de la pause de mi-temps, l'échap du client n'ouvre que son menu à lui (OnEscape) : rien à faire ici.
      // Le menu local de l'hôte ne compte pas : le match continue pour le client.
      if (nAction == _ACT_SPACE) {
         if (!cModule.m_bMenuVisible || m_bLocalMenu) {
            cModule.OnInputKey(57, true, ' ');
         }
      } else if (nAction == _ACT_CONTINUE) {
         if (cModule.m_bMenuVisible && !m_bLocalMenu && !cModule.m_bMatchEnd) {
            cModule.OnMenuContinue();
         }
      }
   }

   // Relâche tout : un client perdu ne doit pas laisser une direction enfoncée
   static void HostReleaseKeys() {
      TeamController cController = HostRemoteController();
      if (cController != null) {
         for (int i = 0; i < _ACT_KEYS.length; i++) {
            cController.Inject(_ACT_KEYS[i], false);
         }
      }
   }

   // Sans adversaire, l'hôte attend dans le menu du match (il peut choisir de continuer seul)
   static void HostWaitMenu(SoccerModule cModule) {
      // un menu local ouvert est remplacé par celui d'attente (ShowMenu → OnShowMenu)
      if (m_cPeer != null || m_bWaitShown || cModule.m_bMenuVisible && !m_bLocalMenu || cModule.m_bMatchEnd || cModule.m_nFadeMode != 0) {
         return;
      }

      if (GameField.m_cBall.IsSuperShot() || Match.m_bPlaying) {
         return;
      }

      m_bWaitShown = true;
      m_bWaitMenu = true;
      Log(m_bEverConnected ? "adversaire parti : menu d'attente" : "en attente de l'adversaire");
      cModule.ShowMenu();
   }

   // SoccerModule.HideMenu
   static void OnMenuHidden() {
      m_bWaitMenu = false;
      m_bLocalMenu = false;
   }

   // SoccerModule.OnEscape (et OnModuleKey chez le client). En ligne, échap ouvre ou ferme un menu chez ce
   // joueur seulement, sans mettre le match en pause : l'autre continue de jouer. Renvoie vrai si la touche
   // est prise ici.
   static boolean OnEscape(SoccerModule cModule) {
      if (m_nRole == _ROLE_NONE || cModule == null || cModule.m_cMenuInGameModule == null) {
         return false;
      }

      if (m_bLocalMenu) {
         cModule.m_cMenuInGameModule.m_cMenuYesNo.Hide();
         cModule.HideMenu();
         return true;
      }

      if (cModule.m_bMenuVisible) {
         if (m_nRole == _ROLE_CLIENT) {
            // pause de mi-temps : échap vaut « continuer », comme chez l'hôte
            if (m_nShownMenu == 2 && !m_bHostLost) {
               ClientQueueKey(_ACT_ESC, true);
            }

            return true;
         }

         return false; // hôte : mi-temps ou attente de l'adversaire, comme avant
      }

      if (!cModule.m_bMatchEnd && !GameField.m_cBall.IsSuperShot()) {
         OpenLocalMenu(cModule);
      }

      return true;
   }

   // Le menu pause, sans ce que SoccerModule.ShowMenu y ajoute : ni arrêt de l'horloge, ni caméra figée
   static void OpenLocalMenu(SoccerModule cModule) {
      MenuInGame cMenu = cModule.m_cMenuInGameModule.m_cMenuInGame;
      m_bLocalMenu = true;
      cModule.m_bMenuVisible = true;
      cMenu.m_bMatchEnd = false;
      cMenu.m_bMatchHalfTime = false;
      cModule.m_cMenuInGameModule.Activate();
      Log("menu local ouvert, le match continue");
   }

   // SoccerModule.ShowMenu : un vrai menu (mi-temps, fin, attente) remplace le menu local
   static void OnShowMenu(SoccerModule cModule) {
      if (m_bLocalMenu && cModule.m_cMenuInGameModule != null) {
         m_bLocalMenu = false;
         cModule.m_cMenuInGameModule.m_cMenuYesNo.Hide();
         cModule.m_cMenuInGameModule.Deactivate();
      }
   }

   // TeamController.OnInputKey : menu local ouvert, les touches appuyées ne jouent pas (les relâchées passent,
   // pour qu'une direction tenue au moment d'échap ne reste pas enfoncée)
   static boolean IsKeyBlocked(boolean bKeyState) {
      return bKeyState && m_bLocalMenu;
   }

   static int HostMenuState(SoccerModule cModule) {
      if (!cModule.m_bMenuVisible || m_bLocalMenu || cModule.m_cMenuInGameModule == null) {
         return 0;
      }

      MenuInGame cMenu = cModule.m_cMenuInGameModule.m_cMenuInGame;
      return cMenu.m_bMatchEnd ? 3 : (cMenu.m_bMatchHalfTime ? 2 : 1);
   }

   static void HostFrame(long lNow) throws java.io.IOException {
      if (m_cPeer != null && lNow - m_lLastRecv > _TIMEOUT_MS) {
         Log("client perdu (plus de paquets depuis " + _TIMEOUT_MS + " ms)");
         HostDropPeer();
      }

      if (!MatchReady()) {
         return;
      }

      SoccerModule cModule = SoccerGame.activeLevelModule;
      HostHalfBreak(cModule, lNow);
      HostWaitMenu(cModule);
      if (m_bBot) {
         HostBot(lNow);
      }

      if (m_cPeer == null || lNow < m_lNextSnap) {
         return;
      }

      m_lNextSnap = lNow + _SNAP_MS;
      AIMatch cMatch = SoccerModule.m_cMatch;
      Ball cBall = GameField.m_cBall;
      ByteBuffer b = m_cOut;
      b.clear();
      b.putInt(_MAGIC);
      b.put((byte)_PKT_SNAP);
      b.putInt(m_nSession);
      b.putInt(++m_nSnapSeq);
      b.putInt(m_nInputApplied);
      b.putFloat(cModule.GetTime());
      b.put((byte)AIMatch.m_nMatchState);
      b.put((byte)cMatch.m_nHalfNo);
      b.put((byte)cMatch.m_nScoreLeft);
      b.put((byte)cMatch.m_nScoreRight);
      b.putFloat(cModule.m_Clock.m_fTime);
      PutVector(b, cBall.GetPositionVectorVolatile());
      PutVector(b, cBall.GetSpeedVectorVolatile());
      b.put((byte)(cBall.m_cCurOwner != null && cBall.HasOwner() ? ObjIndex(cBall.m_cCurOwner) : -1));
      b.put((byte)m_nCamTarget);
      PutFocus(b, m_tTeam[0]);
      PutFocus(b, m_tTeam[1]);
      // Caméra de l'hôte : le client montre exactement la même image (intro, ralentis, fin de mi-temps)
      SoccerCamera cCam = cModule.GetSoccerCamera();
      PutVector(b, cCam.GetPosition());
      Vector vCamDir = cCam.GetForwardVector();
      vCamDir.Negate();
      PutVector(b, vCamDir);
      // 1 : ralenti ; 2 : caméra libre (elle suit sa cible, sans ralenti, fin de mi-temps ni super tir) : le client
      // calcule alors la sienne, avec son mode, son zoom et sa souris
      boolean bCamFree = !cCam.m_bReplay && !cCam.m_bEndHalf && !cCam.m_bFreezed && cCam.bAutoUpdate && !cCam.m_bFirstUpdate
         && cCam.m_Observed != null && !cBall.IsSuperShot();
      b.put((byte)((cCam.m_bReplay ? 1 : 0) | (bCamFree ? 2 : 0)));
      int nCount0 = Math.min(m_tTeam[0].GetFootBallerCount(), 7);
      int nCount1 = Math.min(m_tTeam[1].GetFootBallerCount(), 7);
      b.put((byte)nCount0);
      b.put((byte)nCount1);

      for (int i = 0; i < nCount0; i++) {
         PutObject(b, m_tTeam[0].GetFootBaller(i));
      }

      for (int i = 0; i < nCount1; i++) {
         PutObject(b, m_tTeam[1].GetFootBaller(i));
      }

      PutObject(b, cMatch.m_cReferee);
      HostPutExtras(b, cModule, cMatch, cBall);

      // Événements fiables : tous ceux que le client n'a pas encore confirmés, tant qu'il reste de la place
      if (m_nEventNext - 1 - m_nEventAcked > _EVENT_RING) {
         Log("file d'evenements pleine, " + (m_nEventNext - 1 - m_nEventAcked - _EVENT_RING) + " perdus");
         m_nEventAcked = m_nEventNext - 1 - _EVENT_RING;
      }

      int nFirst = m_nEventAcked + 1;
      b.putInt(nFirst);
      int nCountPos = b.position();
      b.put((byte)0);
      int nEvents = 0;

      for (int nSeq = nFirst; nSeq < m_nEventNext && nEvents < 255; nSeq++) {
         byte[] tEvent = m_tEvents[nSeq % _EVENT_RING];
         if (b.position() + 2 + tEvent.length > _MAX_PACKET) {
            break;
         }

         b.putShort((short)tEvent.length);
         b.put(tEvent);
         nEvents++;
      }

      b.put(nCountPos, (byte)nEvents);
      Send();
      int nMenu = HostMenuState(cModule);
      int nScore = cMatch.m_nScoreLeft * 100 + cMatch.m_nScoreRight;
      if (nScore != m_nLoggedScore && SoccerGame.m_bDebug) {
         m_nLoggedScore = nScore;
         Log("score " + cMatch.m_nScoreLeft + "-" + cMatch.m_nScoreRight);
      }

      if (Match.m_bPlaying != m_bLoggedReplay && SoccerGame.m_bDebug) {
         m_bLoggedReplay = Match.m_bPlaying;
         Log(m_bLoggedReplay ? "ralenti" : "fin du ralenti");
      }

      if (nMenu != m_nLoggedMenu) {
         m_nLoggedMenu = nMenu;
         Log("menu de l'hote : " + nMenu + ", score " + cMatch.m_nScoreLeft + "-" + cMatch.m_nScoreRight + ", mi-temps " + cMatch.m_nHalfNo);
      }
   }

   // Suite de l'état : ce qui n'est ni un joueur ni le ballon (drapeaux, menu, fondu, ralenti, supers, viseur)
   static void HostPutExtras(ByteBuffer b, SoccerModule cModule, AIMatch cMatch, Ball cBall) {
      AITeam cA = (AITeam)m_tTeam[0];
      AITeam cB = (AITeam)m_tTeam[1];
      SelectionDest cSel = cMatch.cSpecialBallSel;
      boolean bDecoy = ModSupers.m_cDoubleTeam != null && ModSupers.m_cDecoy != null && ModSupers.m_vDecoyPos != null;
      int nA = 0;
      if (m_bHudShown) {
         nA |= _FA_HUD;
      }

      if (Match.m_bPlaying) {
         nA |= _FA_REPLAY;
      }

      if (cSel != null && cSel.IsVisible()) {
         nA |= _FA_MARKER;
      }

      if (bDecoy) {
         nA |= _FA_DECOY;
      }

      if (CoreLogic.GetPlayer().m_bSupersOn) {
         nA |= _FA_SUPERS_ON;
      }

      if (cMatch.m_bGoldGoal) {
         nA |= _FA_GOLD_GOAL;
      }

      if (cBall.IsSuperShot()) {
         nA |= _FA_SUPER_SHOT;
      }

      SelectionDest cDest = GameField.m_cDestMark;
      if (cDest != null && cDest.IsVisible()) {
         nA |= _FA_DEST_MARK;
      }

      int nB = 0;
      if (ModSupers.m_fMagnetEnd[0] >= 0.0F || ModSupers.m_fMagnetEnd[1] >= 0.0F) {
         nB |= _FB_MAGNET;
      }

      if (ModSupers.m_cCrazyTeam != null) {
         nB |= _FB_CRAZY;
      }

      if (ModSupers.m_fFreezeEnd[cA.m_nSide & 1] >= 0.0F) {
         nB |= _FB_FROZEN_A;
      }

      if (ModSupers.m_fFreezeEnd[cB.m_nSide & 1] >= 0.0F) {
         nB |= _FB_FROZEN_B;
      }

      if (cA.IsSuperWall()) {
         nB |= _FB_WALL_A;
      }

      if (cB.IsSuperWall()) {
         nB |= _FB_WALL_B;
      }

      // Pause de mi-temps : qui a cliqué « continuer », et le temps qui reste (255 : pas de pause)
      int nHalfLeft = HalfBreakLeft();
      if (nHalfLeft >= 0 && m_bHalfHostOk) {
         nB |= _FB_HALF_HOST;
      }

      if (nHalfLeft >= 0 && m_bHalfPeerOk) {
         nB |= _FB_HALF_PEER;
      }

      // Joueur qui porte le son en boucle du super « vitesse » ou « feinte »
      int nSpeedObj = -1;
      int nFeintObj = -1;

      for (int t = 0; t < 2; t++) {
         for (int i = 0; i < m_tTeam[t].GetFootBallerCount(); i++) {
            AIFootBaller cFB = (AIFootBaller)m_tTeam[t].GetFootBaller(i);
            if (cFB.m_nSuperSpeedSound != -1) {
               nSpeedObj = t * 16 + i;
            }

            if (cFB.m_nSuperFeintSound != -1) {
               nFeintObj = t * 16 + i;
            }
         }
      }

      b.put((byte)nA);
      b.put((byte)nB);
      b.put((byte)HostMenuState(cModule));
      b.put((byte)Math.max(0, Math.min(255, (int)(cModule.m_FadeCurrentAlpha * 255.0F))));
      b.put((byte)Math.max(0, Math.min(200, (int)(m_fSpeedUp * 100.0F + 0.5F))));
      b.put((byte)cMatch.m_nPlayer1Team);
      b.putShort((short)cMatch.m_nHalfTime);
      b.put((byte)nSpeedObj);
      b.put((byte)nFeintObj);
      b.put((byte)(nHalfLeft < 0 ? 255 : Math.min(nHalfLeft, 250)));
      PutVector(b, cSel != null ? cSel.GetPositionVectorVolatile() : m_vZero);
      PutVector(b, cA.m_cSuperWall != null ? cA.m_cSuperWall.GetPositionVectorVolatile() : m_vZero);
      PutVector(b, cB.m_cSuperWall != null ? cB.m_cSuperWall.GetPositionVectorVolatile() : m_vZero);
      PutVector(b, bDecoy ? ModSupers.m_vDecoyPos : m_vZero);
      PutVector(b, cDest != null ? cDest.GetPositionVectorVolatile() : m_vZero);
   }

   static void PutVector(ByteBuffer b, Vector v) {
      b.putFloat(v.fX);
      b.putFloat(v.fY);
      b.putFloat(v.fZ);
   }

   // Joueur qui a la main dans l'équipe, et joueur qui porte le marqueur au sol (-1 : aucun)
   static void PutFocus(ByteBuffer b, Team cTeam) {
      int nMark = -1;

      for (int i = 0; i < cTeam.GetFootBallerCount(); i++) {
         if (cTeam.GetFootBaller(i).HasFocusMark()) {
            nMark = i;
         }
      }

      b.put((byte)(cTeam.m_cFocusFBaller != null ? cTeam.m_cFocusFBaller.m_nNumberInTeam : -1));
      b.put((byte)nMark);
   }

   // Crochet de SoccerCamera.SetObserved. Renvoie faux si la cible ne doit pas changer ici.
   static boolean OnCamObserved(ControlObject cObj) {
      if (m_nRole == _ROLE_HOST) {
         if (cObj == null) {
            m_nCamTarget = _CAM_NONE;
         } else if (cObj instanceof FootBaller) {
            int nIndex = ObjIndex((FootBaller)cObj);
            m_nCamTarget = nIndex >= 0 ? nIndex : _CAM_BALL;
         } else if (SoccerModule.m_cMatch != null && cObj == SoccerModule.m_cMatch.cSpecialBallSel) {
            m_nCamTarget = _CAM_SELECTION;
         } else {
            m_nCamTarget = _CAM_BALL;
         }
      }

      // Le client, une fois ses objets figés, ne suit que la cible donnée par l'hôte
      return m_nRole != _ROLE_CLIENT || !m_bPuppets || m_bApplying;
   }

   static void PutObject(ByteBuffer b, FootBaller cFB) {
      PutVector(b, cFB.GetPositionVectorVolatile());
      PutVector(b, cFB.GetForwardVectorVolatile());
   }

   // ------------------------------------------------------------------ hôte : événements

   static boolean HostHasPeer() {
      return m_nRole == _ROLE_HOST && m_cPeer != null && !m_bLeft;
   }

   static void HostQueueEvent(byte[] tEvent) {
      if (m_cPeer == null) {
         return;
      }

      if (tEvent.length > _MAX_EVENT) {
         Log("evenement trop gros (" + tEvent.length + " octets), ignore");
         return;
      }

      m_tEvents[m_nEventNext % _EVENT_RING] = tEvent;
      m_nEventNext++;
      m_nStatEvents++;
   }

   static ByteBuffer EvBegin(int nType) {
      m_cEv.clear();
      m_cEv.put((byte)nType);
      return m_cEv;
   }

   static void EvEnd() {
      byte[] tEvent = new byte[m_cEv.position()];
      m_cEv.flip();
      m_cEv.get(tEvent);
      HostQueueEvent(tEvent);
   }

   // Chaîne courte : un octet de longueur, puis un octet par caractère (cp1252 / Latin-1)
   static void PutString(ByteBuffer b, String s) {
      if (s == null) {
         s = "";
      }

      int nLen = Math.min(s.length(), 120);
      b.put((byte)nLen);

      for (int i = 0; i < nLen; i++) {
         b.put((byte)s.charAt(i));
      }
   }

   static String GetString(ByteBuffer b) {
      int nLen = b.get() & 0xFF;
      StringBuffer s = new StringBuffer(nLen);

      for (int i = 0; i < nLen; i++) {
         s.append((char)(b.get() & 0xFF));
      }

      return s.toString();
   }

   static void HostQueueAnim(int nIndex, String sAnim, int nRep) {
      ByteBuffer b = EvBegin(_EV_ANIM);
      b.put((byte)nIndex);
      b.put((byte)nRep);
      PutString(b, sAnim);
      EvEnd();
   }

   static void HostQueueHud(int nCode, int nA, int nB, int nC) {
      ByteBuffer b = EvBegin(_EV_HUD);
      b.put((byte)nCode);
      b.put((byte)nA);
      b.put((byte)nB);
      b.put((byte)nC);
      EvEnd();
   }

   static void HostQueueSwap(boolean bSwapped, int nHalfNo) {
      ByteBuffer b = EvBegin(_EV_SWAP);
      b.put((byte)(bSwapped ? 1 : 0));
      b.put((byte)nHalfNo);
      EvEnd();
   }

   // Crochet de FootBaller.PlayAnim. Renvoie faux si l'animation ne doit pas être jouée ici.
   static boolean OnAnim(FootBaller cFB, String sAnim, int nRep) {
      if (m_nRole == _ROLE_NONE) {
         return true;
      }

      if (m_nRole == _ROLE_CLIENT) {
         return m_bApplying; // le client ne joue que les animations venues de l'hôte
      }

      int nIndex = ObjIndex(cFB);
      if (nIndex >= 0 && sAnim != null) {
         m_tLastAnim[nIndex] = sAnim;
         m_tLastRep[nIndex] = nRep;
         if (HostHasPeer()) {
            HostQueueAnim(nIndex, sAnim, nRep);
         }
      }

      return true;
   }

   // Crochet des ExecuteFX des joueurs, du ballon et du leurre. vNormal peut être null.
   static boolean OnFX(int nIndex, String sName, Vector vPos, Vector vNormal) {
      if (m_nRole == _ROLE_NONE) {
         return true;
      }

      if (m_nRole == _ROLE_CLIENT) {
         return m_bApplying;
      }

      if (nIndex >= 0 && sName != null && vPos != null && HostHasPeer()) {
         ByteBuffer b = EvBegin(_EV_FX);
         b.put((byte)nIndex);
         PutString(b, sName);
         PutVector(b, vPos);
         b.put((byte)(vNormal != null ? 1 : 0));
         if (vNormal != null) {
            PutVector(b, vNormal);
         }

         EvEnd();
      }

      return true;
   }

   // Crochet de Ball.PlaySound et FootBaller.PlaySound
   static boolean OnSound(int nKind, FootBaller cFB, String sName) {
      if (m_nRole == _ROLE_NONE) {
         return true;
      }

      if (m_nRole == _ROLE_CLIENT) {
         return m_bApplying;
      }

      HostSound(nKind, cFB, sName);
      return true;
   }

   // Son joué par l'hôte à un endroit sans crochet commun (bip de fin de mi-temps, gel)
   static void HostSound(int nKind, FootBaller cFB, String sName) {
      if (!HostHasPeer() || sName == null) {
         return;
      }

      int nIndex = cFB != null ? ObjIndex(cFB) : -1;
      if (cFB != null && nIndex < 0) {
         return;
      }

      ByteBuffer b = EvBegin(_EV_SOUND);
      b.put((byte)nKind);
      b.put((byte)nIndex);
      PutString(b, sName);
      EvEnd();
   }

   // Crochet de Individual.StartSound : cri d'un joueur choisi par son graphe d'activité.
   // Renvoie faux si le son ne doit pas partir ici (le client ne joue que ceux de l'hôte).
   static boolean OnVoice(Individual cObj, String sName, float fMin, float fMax) {
      if (m_nRole == _ROLE_NONE || !(cObj instanceof FootBaller)) {
         return true;
      }

      if (m_nRole == _ROLE_CLIENT) {
         return false;
      }

      int nIndex = ObjIndex((FootBaller)cObj);
      if (nIndex >= 0 && sName != null && HostHasPeer()) {
         ByteBuffer b = EvBegin(_EV_SOUND);
         b.put((byte)_SND_VOICE);
         b.put((byte)nIndex);
         PutString(b, sName);
         b.putFloat(fMin);
         b.putFloat(fMax);
         EvEnd();
      }

      return true;
   }

   // Crochet des méthodes de PetSoccerHud
   static boolean OnHud(int nCode, int nA, int nB, int nC) {
      if (m_nRole == _ROLE_NONE) {
         return true;
      }

      if (m_nRole == _ROLE_CLIENT) {
         return m_bApplying;
      }

      if (HostHasPeer()) {
         HostQueueHud(nCode, nA, nB, nC);
      }

      return true;
   }

   // Crochet de Commentator.Say
   static boolean OnSay(String sWav) {
      if (m_nRole == _ROLE_NONE) {
         return true;
      }

      if (m_nRole == _ROLE_CLIENT) {
         return m_bApplying;
      }

      if (HostHasPeer() && sWav != null && sWav.length() > 0) {
         m_bSaid = true;
         ByteBuffer b = EvBegin(_EV_SAY);
         PutString(b, sWav);
         EvEnd();
      }

      return true;
   }

   // Crochet de Commentator.StopTalking
   static void OnSayStop() {
      if (m_bSaid && HostHasPeer()) {
         ByteBuffer b = EvBegin(_EV_SAY);
         PutString(b, "");
         EvEnd();
      }

      m_bSaid = false;
   }

   // AIMatch.ChangeTeamSides, appelé juste avant l'échange
   static void HostSwap(int nHalfNo) {
      if (HostHasPeer() && m_tTeam[0] != null) {
         HostQueueSwap(!IsSwapped(), nHalfNo);
      }
   }

   // Match.PlayOnGoalReaction : le public fête le but
   static void HostCrowd(int nAnimal) {
      if (HostHasPeer()) {
         ByteBuffer b = EvBegin(_EV_CROWD);
         b.put((byte)nAnimal);
         EvEnd();
      }
   }

   // ScriptedEvent.Activate (une intempérie commence) et Match.ClearWeatherEvents (nKind 0 : fin)
   static void HostWeather(int nKind, float fMaxTime) {
      if (HostHasPeer() && nKind >= 0 && nKind <= 5) {
         ByteBuffer b = EvBegin(_EV_WEATHER);
         b.put((byte)nKind);
         b.putFloat(fMaxTime);
         EvEnd();
      }
   }

   // MenuInGame.ShowInfo chez l'hôte : le client affiche les mêmes statistiques
   static void HostStats(String[] tHost, String[] tGuest, String[] tHostScorers, String[] tGuestScorers) {
      if (!HostHasPeer()) {
         return;
      }

      String[][] t = new String[][]{tHost, tGuest, tHostScorers, tGuestScorers};
      ByteBuffer b = EvBegin(_EV_STATS);

      for (int i = 0; i < 4; i++) {
         int nCount = t[i] == null ? 0 : Math.min(t[i].length, 12);
         b.put((byte)nCount);

         for (int j = 0; j < nCount; j++) {
            String s = t[i][j] == null ? "" : t[i][j];
            PutString(b, s.length() > 40 ? s.substring(0, 40) : s);
         }
      }

      EvEnd();
   }

   // ------------------------------------------------------------------ client : touches

   // Crochet de TeamController.OnInputKey. Renvoie vrai si la touche est prise par le réseau.
   static boolean OnLocalKey(TeamController cController, int nKeyCode, boolean bKeyState) {
      if (m_nRole != _ROLE_CLIENT) {
         return false;
      }

      int nAction = -1;
      if (cController.m_Actions[0] instanceof InputDigital && ((InputDigital)cController.m_Actions[0]).m_iKeyValue == nKeyCode) {
         nAction = _ACT_PASS;
      } else if (cController.m_Actions[1] instanceof InputDigital && ((InputDigital)cController.m_Actions[1]).m_iKeyValue == nKeyCode) {
         nAction = _ACT_SHOT;
      } else if (cController.m_Actions[2] instanceof InputDigital && ((InputDigital)cController.m_Actions[2]).m_iKeyValue == nKeyCode) {
         nAction = _ACT_FEINT;
      } else if (cController.m_Actions[4] instanceof InputDigital && ((InputDigital)cController.m_Actions[4]).m_iKeyValue == nKeyCode) {
         nAction = _ACT_SUPER;
      } else if (cController.m_Actions[3] instanceof InputDigital) {
         InputDigital cTurn = (InputDigital)cController.m_Actions[3];
         if (cTurn.m_nUpKey == nKeyCode) {
            nAction = _ACT_UP;
         } else if (cTurn.m_nRightKey == nKeyCode) {
            nAction = _ACT_RIGHT;
         } else if (cTurn.m_nDownKey == nKeyCode) {
            nAction = _ACT_DOWN;
         } else if (cTurn.m_nLeftKey == nKeyCode) {
            nAction = _ACT_LEFT;
         }
      }

      if (nAction >= 0) {
         ClientQueueKey(nAction, bKeyState);
      }

      return true;
   }

   // Crochet de SoccerModule.OnInputKey. Chez le client, espace est une demande à l'hôte, échap ouvre le menu
   // local (OnEscape). Renvoie vrai si la touche est prise par le réseau.
   static boolean OnModuleKey(int nKeyCode, boolean bKeyState) {
      if (m_nRole != _ROLE_CLIENT) {
         return false;
      }

      if (bKeyState) {
         if (nKeyCode == 1) {
            OnEscape(SoccerGame.activeLevelModule);
         } else if (nKeyCode == 57 && !m_bHostLost && !m_bLocalMenu) {
            ClientQueueKey(_ACT_SPACE, true);
         }
      }

      return true;
   }

   // Crochet de SoccerModule.OnMenuContinue. Renvoie vrai si c'est l'hôte qui fermera le menu.
   static boolean OnMenuContinue() {
      if (m_bLocalMenu) {
         return false; // menu local : il se ferme ici, l'autre joueur n'en sait rien
      }

      if (m_nRole == _ROLE_HOST) {
         return HostHalfSkip(); // pause de mi-temps : le menu se fermera quand le client sera prêt aussi
      }

      if (m_nRole != _ROLE_CLIENT) {
         return false;
      }

      if (!m_bHostLost) {
         ClientQueueKey(_ACT_CONTINUE, true);
      }

      return true;
   }

   // ModSwitch.OnKey : chez le client, une touche directe part à l'hôte. Renvoie vrai si elle est envoyée.
   static boolean ClientSelect(int nIndex) {
      if (m_nRole != _ROLE_CLIENT) {
         return false;
      }

      ClientQueueKey(_ACT_SEL + nIndex, true);
      return true;
   }

   static void ClientQueueKey(int nAction, boolean bState) {
      m_nInputSeq++;
      m_tInput[m_nInputSeq % _INPUT_RING] = (byte)(nAction | (bState ? 0x80 : 0));
      m_lNextInput = 0L; // part à la prochaine frame
   }

   // debugconf.scr : NetBot(1). Le client joue tout seul (directions qui tournent, passes, tirs, espace,
   // « continuer » à la mi-temps) : sert aux tests à deux jeux sur un PC, où une seule fenêtre a le clavier.
   static void ClientBot(long lNow) {
      if (lNow < m_lBotNext) {
         return;
      }

      m_lBotNext = lNow + 1000L;
      m_nBotStep++;
      if (m_nBotStep % 2 == 0) {
         if (m_nBotDir >= 0) {
            ClientQueueKey(m_nBotDir, false);
         }

         // vers le but adverse la moitié du temps, un écart vers le haut ou le bas sinon
         int nPhase = m_nBotStep / 2 % 4;
         m_nBotDir = nPhase == 1 ? _ACT_UP : (nPhase == 3 ? _ACT_DOWN : (IsSwapped() ? _ACT_RIGHT : _ACT_LEFT));
         ClientQueueKey(m_nBotDir, true);
      }

      if (m_nBotStep % 5 == 1) {
         ClientQueueKey(_ACT_PASS, true);
         ClientQueueKey(_ACT_PASS, false);
      }

      if (m_nBotStep % 3 == 2) {
         ClientQueueKey(_ACT_SHOT, true);
         ClientQueueKey(_ACT_SHOT, false);
      }

      if (m_nBotStep % 11 == 3) {
         ClientQueueKey(_ACT_SUPER, true);
         ClientQueueKey(_ACT_SUPER, false);
      }

      // touche directe vers un joueur (ModSwitch), en faisant le tour des numéros 2..7
      if (m_nBotStep % 7 == 4) {
         ClientQueueKey(_ACT_SEL + m_nBotStep / 7 % ModSwitch._NUMBERS, true);
      }

      // espace seulement hors du jeu et des ralentis : le test doit voir les ralentis en entier
      // échap : le menu local du client, refermé par « continuer » ; le match de l'hôte ne doit pas s'arrêter
      SoccerModule cModule = SoccerGame.activeLevelModule;
      if (m_nBotStep == 25 && m_nShownMenu == 0) {
         OnEscape(cModule);
      } else if (m_bLocalMenu && m_nBotStep == 29 && cModule != null) {
         cModule.OnMenuContinue();
         Log("test : menu local du client referme");
      }

      // à la mi-temps, une fois : un autre schéma part chez l'hôte, comme un invité qui revoit sa tactique
      if (m_nShownMenu == 2 && !m_bBotTactics && ModLobby.IsOpen() && CoreLogic.m_cTeamDataRight != null) {
         m_bBotTactics = true;
         String sNow = ModTactics.GetComposition(CoreLogic.m_cTeamDataRight.m_nAnimalType).m_sScheme;

         for (int i = 0; i < ModTactics.GetSchemeCount(); i++) {
            String sId = ModTactics.GetScheme(i).m_sId;
            if (!sId.equals(sNow)) {
               Log("test : schema " + sId + " envoye a la mi-temps");
               ModLobby.SetMyTactics(ModTactics.FromLine(sId));
               break;
            }
         }
      }

      if (m_nShownMenu == 1 || m_nShownMenu == 2 && !m_bNoSkip) {
         if (m_nBotStep % 4 == 2) {
            ClientQueueKey(_ACT_CONTINUE, true);
         }
      } else if (m_nBotStep % 4 == 2 && m_nShownReplay <= 0 && AIMatch.m_nMatchState != 0) {
         ClientQueueKey(_ACT_SPACE, true);
      }
   }

   // debugconf.scr : NetBot(1) chez l'hôte, pour les tests sans personne au clavier. Une touche ou un coup franc
   // de son équipe attend sinon indéfiniment : une passe toutes les 2 s remet le ballon en jeu.
   static void HostBot(long lNow) {
      if (lNow < m_lBotNext || m_tTeam[0] == null) {
         return;
      }

      m_lBotNext = lNow + 2000L;
      m_nBotStep++;
      if (!m_bNoSkip) {
         HostHalfSkip(); // à la mi-temps : « continuer »
      }

      // échap vers 16 s : le menu local de l'hôte, refermé 6 s plus tard ; l'horloge du match doit avancer
      SoccerModule cModule = SoccerGame.activeLevelModule;
      if (m_nBotStep == 8 && cModule != null && !cModule.m_bMenuVisible) {
         m_fBotClock = cModule.m_Clock.GetMatchTimeF();
         OnEscape(cModule);
      } else if (m_nBotStep == 11 && m_bLocalMenu) {
         Log("test : menu local de l'hote, horloge " + m_fBotClock + " -> " + cModule.m_Clock.GetMatchTimeF());
         OnEscape(cModule);
      }

      TeamController cController = m_tTeam[0].GetTeamController();
      if (cController != null && !cController.m_bRemote && cController.m_Actions[0] instanceof InputDigital) {
         int nKey = ((InputDigital)cController.m_Actions[0]).m_iKeyValue;
         cController.Inject(nKey, true);
         cController.Inject(nKey, false);
      }
   }

   static void ClientSend(long lNow) throws java.io.IOException {
      // L'hôte ne voit le client qu'une fois son terrain chargé : il l'attend jusque-là
      if (!MatchReady()) {
         return;
      }

      if (m_bBot) {
         ClientBot(lNow);
      }

      if (lNow < m_lNextInput) {
         return;
      }

      m_lNextInput = lNow + _INPUT_MS;
      ByteBuffer b = m_cOut;
      b.clear();
      b.putInt(_MAGIC);
      b.put((byte)_PKT_INPUT);
      b.putInt(m_nSession);
      b.putInt(m_nEventApplied < 0 ? 0 : m_nEventApplied);
      b.putInt(m_nInputSeq);
      int nCount = Math.min(m_nInputSeq, _INPUT_RING - 1);
      b.put((byte)nCount);

      for (int nSeq = m_nInputSeq - nCount + 1; nSeq <= m_nInputSeq; nSeq++) {
         b.put(m_tInput[nSeq % _INPUT_RING]);
      }

      Send();
   }

   // ------------------------------------------------------------------ client : réception

   static void ClientReadSnap(long lNow) {
      ByteBuffer b = m_cIn;
      if (b.getInt() != m_nSession || !MatchReady()) {
         return; // état destiné à une ancienne session, ou terrain pas encore prêt
      }

      int nSeq = b.getInt();
      b.getInt(); // dernier changement de touche joué par l'hôte (mesure du retard, plus tard)
      m_lLastRecv = lNow;
      boolean bNewer = nSeq > m_nLastSnapSeq;
      NetSnap s = null;
      if (bNewer) {
         m_nLastSnapSeq = nSeq;
         s = m_tSnaps[m_nSnapCount % _SNAP_RING];
         if (s == null) {
            s = new NetSnap();
            m_tSnaps[m_nSnapCount % _SNAP_RING] = s;
         }
      } else {
         s = new NetSnap(); // paquet en retard : on ne garde que ses événements
      }

      s.nSeq = nSeq;
      s.fTime = b.getFloat();
      s.nMatchState = b.get();
      s.nHalfNo = b.get();
      s.nScoreLeft = b.get() & 0xFF;
      s.nScoreRight = b.get() & 0xFF;
      s.fClock = b.getFloat();

      for (int i = 0; i < 6; i++) {
         s.tBall[i] = b.getFloat();
      }

      s.nBallOwner = b.get();
      s.nCamTarget = b.get();
      s.nFocus0 = b.get();
      s.nMark0 = b.get();
      s.nFocus1 = b.get();
      s.nMark1 = b.get();

      for (int i = 0; i < 6; i++) {
         s.tCam[i] = b.getFloat();
      }

      int nCamFlags = b.get();
      s.bCamReplay = (nCamFlags & 1) != 0;
      s.bCamFree = (nCamFlags & 2) != 0;
      s.nCount0 = Math.min(b.get() & 0xFF, 7);
      s.nCount1 = Math.min(b.get() & 0xFF, 7);
      int nObjects = s.nCount0 + s.nCount1 + 1;

      for (int i = 0; i < nObjects * 6; i++) {
         s.tObj[i] = b.getFloat();
      }

      s.nFlagsA = b.get() & 0xFF;
      s.nFlagsB = b.get() & 0xFF;
      s.nMenu = b.get() & 0xFF;
      s.nFade = b.get() & 0xFF;
      s.nSpeed = b.get() & 0xFF;
      s.nPlayer1 = b.get() & 1;
      s.nHalfTime = b.getShort();
      s.nSpeedObj = b.get();
      s.nFeintObj = b.get();
      s.nHalfLeft = b.get() & 0xFF;

      for (int i = 0; i < s.tExtra.length; i++) {
         s.tExtra[i] = b.getFloat();
      }

      if (bNewer) {
         m_nSnapCount++;
         // pause de mi-temps : le dernier état reçu fait foi, sans attendre le retard d'affichage
         m_nHalfLeft = s.nHalfLeft == 255 ? -1 : s.nHalfLeft;
         m_nHalfFlags = s.nFlagsB;
      }

      int nFirst = b.getInt();
      int nEvents = b.get() & 0xFF;
      // Premier état, ou événements que l'hôte a renoncé à renvoyer (retour après une coupure) :
      // on se cale sur sa file
      if (m_nEventApplied < 0 || nFirst > m_nEventApplied + 1) {
         m_nEventApplied = nFirst - 1;
      }

      for (int i = 0; i < nEvents; i++) {
         int nLen = b.getShort() & 0xFFFF;
         int nNext = b.position() + nLen;
         int nEventSeq = nFirst + i;
         if (nEventSeq == m_nEventApplied + 1) {
            m_nEventApplied = nEventSeq;
            m_nStatEvents++;
            m_bApplying = true;

            try {
               ClientApplyEvent(b);
            } catch (RuntimeException e) {
               Log("evenement " + nEventSeq + " illisible : " + e);
            } finally {
               m_bApplying = false;
            }
         }

         b.position(nNext);
      }
   }

   static void ClientApplyEvent(ByteBuffer b) {
      SoccerModule cModule = SoccerGame.activeLevelModule;
      AIMatch cMatch = SoccerModule.m_cMatch;
      int nType = b.get();
      if (nType == _EV_ANIM) {
         int nIndex = b.get();
         int nRep = b.get(); // signé : -1 = en boucle
         String sName = GetString(b);
         FootBaller cFB = ObjFromIndex(nIndex);
         if (cFB != null) {
            cFB.PlayAnim(sName, nRep);
         }
      } else if (nType == _EV_FX) {
         int nIndex = b.get();
         String sName = GetString(b);
         Vector vPos = new Vector(b.getFloat(), b.getFloat(), b.getFloat());
         Vector vNormal = null;
         if (b.get() != 0) {
            vNormal = new Vector(b.getFloat(), b.getFloat(), b.getFloat());
         }

         if (nIndex == _OBJ_BALL || nIndex == _OBJ_DECOY) {
            if (vNormal != null) {
               GameField.m_cBall.ExecuteFX(sName, vPos, vNormal);
            } else {
               GameField.m_cBall.ExecuteFX(sName, vPos);
            }
         } else {
            FootBaller cFB = ObjFromIndex(nIndex);
            if (cFB != null) {
               if (vNormal != null) {
                  cFB.ExecuteFX(sName, vPos, vNormal);
               } else {
                  cFB.ExecuteFX(sName, vPos);
               }
            }
         }
      } else if (nType == _EV_SOUND) {
         int nKind = b.get();
         FootBaller cFB = ObjFromIndex(b.get());
         String sName = GetString(b);
         if (nKind == _SND_BALL) {
            GameField.m_cBall.PlaySound(sName);
         } else if (nKind == _SND_MODULE) {
            cModule.PlaySound(sName);
         } else if (cFB != null) {
            if (nKind == _SND_PLAYER) {
               cFB.PlaySound(sName);
            } else if (nKind == _SND_ONESHOT) {
               cFB.AttachOneShotSound(sName);
            } else if (nKind == _SND_VOICE) {
               float fMin = b.getFloat();
               float fMax = b.getFloat();
               cFB.AttachOneShotSound(sName, fMin, fMax);
            }
         }
      } else if (nType == _EV_HUD) {
         int nCode = b.get();
         int nA = b.get();
         int nB = b.get();
         int nC = b.get();
         PetSoccerHud cHud = SoccerModule.m_cHud;
         switch (nCode) {
            case _HUD_LETTERS:
               cHud.LightSuperLetters(nA, nB, nC);
               break;
            case _HUD_FLASH:
               cHud.FlashSuperIcon(nA);
               break;
            case _HUD_FLASH_END:
               cHud.FlashSuperIconEnd(nA);
               break;
            case _HUD_SWITCH:
               cHud.SwitchSuperOn(nA);
               break;
            case _HUD_EVENT:
               cHud.ShowEventInfo(nA);
               break;
            case _HUD_SHOW_INFO:
               cHud.ShowInfo();
               break;
            case _HUD_HIDE_INFO:
               cHud.HideInfo();
         }
      } else if (nType == _EV_SAY) {
         String sWav = GetString(b);
         Commentator cComm = SoccerGame.game.GetCommentator();
         if (sWav.length() > 0) {
            cComm.Say(sWav);
         } else {
            cComm.StopTalking();
         }
      } else if (nType == _EV_SWAP) {
         boolean bSwapped = b.get() != 0;
         int nHalfNo = b.get();
         if (bSwapped != IsSwapped()) {
            cMatch.m_nHalfNo = nHalfNo;
            cMatch.ChangeTeamSides();
            Log("client : changement de cote");
         }
      } else if (nType == _EV_CROWD) {
         int nAnimal = b.get();
         java.util.Vector vWatchers = GameField.GetWatchers();

         for (int i = 0; i < vWatchers.size(); i++) {
            ((Watcher)vWatchers.elementAt(i)).CallOnGoalReaction(nAnimal);
         }

         cModule.GetSoccerCamera().PlaySound("Happy.wav");
      } else if (nType == _EV_WEATHER) {
         int nKind = b.get();
         float fMaxTime = b.getFloat();
         if (nKind == 0) {
            cMatch.GetWeather().KillDisaster();
         } else {
            new ScriptedEvent(nKind, 0.0F, fMaxTime).Activate();
         }
      } else if (nType == _EV_STATS) {
         String[][] t = new String[4][];

         for (int i = 0; i < 4; i++) {
            t[i] = new String[b.get() & 0xFF];

            for (int j = 0; j < t[i].length; j++) {
               t[i][j] = GetString(b);
            }
         }

         m_tStats = t;
         if (cModule.m_bMenuVisible && cModule.m_cMenuInGameModule != null) {
            cModule.m_cMenuInGameModule.m_cMenuInGame.ShowInfo();
         }
      }
   }

   // Statistiques du menu reçues de l'hôte : 0 équipe de gauche du tableau, 1 de droite, 2 et 3 leurs buteurs
   static String[] ClientStats(int nWhich) {
      return m_nRole == _ROLE_CLIENT && m_tStats != null ? m_tStats[nWhich] : null;
   }

   // ------------------------------------------------------------------ client : affichage

   // Fige un objet du match comme le fait Recorder.Replay : plus de physique ni de collisions,
   // c'est l'état reçu de l'hôte qui le place
   static void ClientFreeze(FootBaller cFB) {
      if (cFB != null) {
         if (((AIFootBaller)cFB).HasBall()) {
            ((AIFootBaller)cFB).DetachBall();
         }

         cFB.ClearFlag(1);
         cFB.ClearFlag(32);
         cFB.SetCollFlags(8192);
         cFB.SetSpeed(m_vZero);
      }
   }

   static void ClientFreezeAll() {
      AIMatch cMatch = SoccerModule.m_cMatch;

      for (int t = 0; t < 2; t++) {
         Team cTeam = m_tTeam[t];

         for (int i = 0; i < cTeam.GetFootBallerCount(); i++) {
            ClientFreeze(cTeam.GetFootBaller(i));
         }
      }

      ClientFreeze(cMatch.m_cReferee);
      Ball cBall = GameField.m_cBall;
      cBall.ClearFlag(32);
      cBall.SetCollFlags(8192);
      cBall.SetPhysicsType(0);
      cBall.SetPhysicsActive(false);
      Log("client : objets du match figes");
   }

   // Appelé par SoccerModule.OnFrame à la place de la logique du match. Applique l'état reçu.
   static void ClientMatchFrame(SoccerModule cModule) {
      if (m_bLeft || !MatchReady() || m_nSnapCount == 0) {
         return;
      }

      if (!m_bPuppets) {
         m_bPuppets = true;
         ClientFreezeAll();
      }

      boolean bLost = m_bHostLeft || System.currentTimeMillis() - m_lLastRecv > _TIMEOUT_MS;
      if (bLost != m_bHostLost) {
         m_bHostLost = bLost;
         Log(bLost ? "hote perdu" : "hote revenu");
         if (cModule.m_bMenuVisible && cModule.m_cMenuInGameModule != null) {
            cModule.m_cMenuInGameModule.m_cMenuInGame.Show(); // boutons et texte du menu à jour
         }
      }

      NetSnap cNew = m_tSnaps[(m_nSnapCount - 1) % _SNAP_RING];
      float fLocal = cModule.GetTime();
      float fOffset = cNew.fTime - fLocal;
      // Le décalage d'horloge retenu est celui du paquet le plus rapide : il ne recule que doucement
      if (!m_bOffsetSet || fOffset > m_fTimeOffset || fOffset < m_fTimeOffset - 1.0F) {
         m_fTimeOffset = fOffset;
         m_bOffsetSet = true;
      } else {
         m_fTimeOffset -= 0.0005F;
      }

      float fRender = fLocal + m_fTimeOffset - _INTERP_DELAY;
      // Cherche les deux états qui encadrent l'instant affiché
      NetSnap cA = null;
      NetSnap cB = null;
      int nStored = Math.min(m_nSnapCount, _SNAP_RING);

      for (int i = 0; i < nStored; i++) {
         NetSnap s = m_tSnaps[(m_nSnapCount - 1 - i) % _SNAP_RING];
         if (s.fTime <= fRender) {
            cA = s;
            break;
         }

         cB = s;
      }

      if (cA == null) {
         cA = cB; // tout est plus récent : on prend le plus ancien
         cB = null;
      }

      float fK = 0.0F;
      if (cB != null && cB.fTime > cA.fTime) {
         fK = (fRender - cA.fTime) / (cB.fTime - cA.fTime);
         if (fK < 0.0F) {
            fK = 0.0F;
         }

         if (fK > 1.0F) {
            fK = 1.0F;
         }
      } else {
         cB = cA;
      }

      AIMatch cMatch = SoccerModule.m_cMatch;
      m_bApplying = true;

      try {
         int nObj = 0;

         for (int t = 0; t < 2; t++) {
            Team cTeam = m_tTeam[t];
            int nCount = Math.min(t == 0 ? cA.nCount0 : cA.nCount1, cTeam.GetFootBallerCount());

            for (int i = 0; i < nCount; i++) {
               ClientPlace(cTeam.GetFootBaller(i), cA, cB, fK, nObj + i);
            }

            nObj += t == 0 ? cA.nCount0 : cA.nCount1;
         }

         ClientPlace(cMatch.m_cReferee, cA, cB, fK, nObj);
         m_vTmpPos.fX = cA.tBall[0] + (cB.tBall[0] - cA.tBall[0]) * fK;
         m_vTmpPos.fY = cA.tBall[1] + (cB.tBall[1] - cA.tBall[1]) * fK;
         m_vTmpPos.fZ = cA.tBall[2] + (cB.tBall[2] - cA.tBall[2]) * fK;
         GameField.m_cBall.SetPosition(m_vTmpPos);
         SoccerModule.GetHud().UpdateBallPosition(GameField.m_cBall);
         ClientFocus(m_tTeam[0], cNew.nFocus0, cNew.nMark0);
         ClientFocus(m_tTeam[1], cNew.nFocus1, cNew.nMark1);
         if (cNew.nCamTarget != m_nShownCam) {
            m_nShownCam = cNew.nCamTarget;
            ControlObject cTarget = null;
            if (m_nShownCam >= 0 && m_nShownCam < 40) {
               cTarget = ObjFromIndex(m_nShownCam);
            } else if (m_nShownCam == _CAM_SELECTION && cMatch.cSpecialBallSel != null) {
               cTarget = cMatch.cSpecialBallSel;
            } else if (m_nShownCam != _CAM_NONE) {
               cTarget = GameField.m_cBall;
            }

            cModule.GetSoccerCamera().SetObserved(cTarget);
         }

         ClientExtras(cModule, cMatch, cNew, cA, cB, fK);
      } finally {
         m_bApplying = false;
      }

      for (int i = 0; i < 6; i++) {
         m_tCam[i] = cA.tCam[i] + (cB.tCam[i] - cA.tCam[i]) * fK;
      }

      if (cNew.bCamReplay != m_bShownCamReplay) {
         m_bShownCamReplay = cNew.bCamReplay;
         SoccerCamera cCam = cModule.GetSoccerCamera();
         cCam.SetFOV(m_bShownCamReplay ? cCam.m_fReplayFOV : cCam.m_fFOV);
      }

      boolean bCamFree = (cB != null ? cB : cA).bCamFree;
      if (bCamFree != m_bCamFree) {
         m_bCamFree = bCamFree;
         Log("client : camera " + (bCamFree ? "locale" : "de l'hote"));
      }
      m_bCamReady = true;
      // Score, horloge et état du match : ceux de l'état le plus récent
      AIMatch.m_nMatchState = cNew.nMatchState;
      cMatch.m_nHalfNo = cNew.nHalfNo;
      cMatch.m_nScoreLeft = cNew.nScoreLeft;
      cMatch.m_nScoreRight = cNew.nScoreRight;
      cMatch.m_nPlayer1Team = cNew.nPlayer1;
      cMatch.m_nPlayer2Team = 1 - cNew.nPlayer1;
      cMatch.m_bGoldGoal = (cNew.nFlagsA & _FA_GOLD_GOAL) != 0;
      if (cNew.nHalfTime > 0) {
         cMatch.m_nHalfTime = cNew.nHalfTime;
      }

      cModule.m_Clock.m_bStop = true;
      cModule.m_Clock.m_fTime = cNew.fClock;
      cMatch.UpdateWeather(cModule.m_Clock.GetMatchTimeF());
      if (cNew.nScoreLeft != m_nShownScoreLeft || cNew.nScoreRight != m_nShownScoreRight) {
         m_nShownScoreLeft = cNew.nScoreLeft;
         m_nShownScoreRight = cNew.nScoreRight;
         SoccerModule.m_cHud.UpdateScore(cNew.nScoreLeft, cNew.nScoreRight);
      }

      int nSecond = (int)cNew.fClock;
      if (nSecond != m_nShownSecond) {
         m_nShownSecond = nSecond;
         // derniers instants d'une mi-temps : l'horloge clignote comme chez l'hôte (Match.Update)
         int nScope = cNew.nHalfNo % 2 == 0 ? cNew.nHalfTime : cNew.nHalfTime * 2;
         boolean bEnding = !cMatch.m_bGoldGoal && nScope > 0 && nSecond >= nScope - cMatch.m_nBeepLastSeconds && nSecond < nScope;
         SoccerModule.m_cHud.UpdateClock(cModule.m_Clock.GetMatchTime(), bEnding);
      }

      // Le menu en dernier : il arrête le temps du module
      int nMenu = cNew.nMenu;
      if (m_bHostLost && nMenu == 0) {
         nMenu = 1;
      }

      ClientMenu(cModule, nMenu);
   }

   // Ce que l'état dit en plus des joueurs et du ballon : HUD, ralenti, fondu, viseur, supers
   static void ClientExtras(SoccerModule cModule, AIMatch cMatch, NetSnap cNew, NetSnap cA, NetSnap cB, float fK) {
      int nA = cNew.nFlagsA;
      int nB = cNew.nFlagsB;
      m_bHostSupers = (nA & _FA_SUPERS_ON) != 0;
      int nHud = nA & _FA_HUD;
      if (nHud != m_nShownHud) {
         m_nShownHud = nHud;
         if (nHud != 0) {
            SoccerModule.ShowHud();
         } else {
            SoccerModule.HideHud();
         }
      }

      int nReplay = nA & _FA_REPLAY;
      if (nReplay != m_nShownReplay) {
         boolean bFirst = m_nShownReplay < 0;
         m_nShownReplay = nReplay;
         Match.m_bPlaying = nReplay != 0;
         if (SoccerGame.m_bDebug && !bFirst) {
            Log(nReplay != 0 ? "client : ralenti" : "client : fin du ralenti");
         }

         if (nReplay != 0) {
            SoccerGame.GetCommModule().ShowReplaySign();
            cModule.PlayReplayMusic();
         } else if (!bFirst) {
            SoccerGame.GetCommModule().HideReplaySign();
            cModule.PlayMusic();
         }
      }

      if (cNew.nSpeed != m_nShownSpeed) {
         m_nShownSpeed = cNew.nSpeed;
         cModule.TimerSetSpeedUp(cNew.nSpeed / 100.0F);
      }

      // Fondu au noir : celui de l'hôte remplace le fondu local
      cModule.m_nFadeMode = 0;
      if (cNew.nFade != m_nShownFade) {
         if (cNew.nFade > 0 && m_nShownFade <= 0) {
            cModule.m_Fade.Show();
            cModule.m_Fade.MoveTop();
         }

         m_nShownFade = cNew.nFade;
         cModule.m_FadeCurrentAlpha = cNew.nFade / 255.0F;
         cModule.m_Fade.SetAlpha(cModule.m_FadeCurrentAlpha);
      }

      // Viseur des coups de pied arrêtés
      SelectionDest cSel = cMatch.cSpecialBallSel;
      if (cSel != null) {
         int nMarker = nA & _FA_MARKER;
         if (nMarker != 0) {
            ClientExtraPos(cNew, cA, cB, fK, 0, _FA_MARKER);
            cSel.SetPosition(m_vTmpPos);
         }

         if (nMarker != m_nShownMarker) {
            m_nShownMarker = nMarker;
            if (nMarker != 0) {
               cSel.Show();
            } else {
               cSel.Hide();
            }
         }
      }

      // Repère jaune : là où le ballon va retomber
      SelectionDest cDest = GameField.m_cDestMark;
      if (cDest != null) {
         int nDest = nA & _FA_DEST_MARK;
         if (nDest != 0) {
            ClientExtraPos(cNew, cA, cB, fK, 12, _FA_DEST_MARK);
            cDest.SetPosition(m_vTmpPos);
         }

         if (nDest != m_nShownDest) {
            m_nShownDest = nDest;
            if (nDest != 0) {
               cDest.Show();
            } else {
               cDest.Hide();
            }
         }
      }

      // Murs du super « défense »
      AITeam cTeamA = (AITeam)m_tTeam[0];
      AITeam cTeamB = (AITeam)m_tTeam[1];
      ClientWall(cTeamA.m_cSuperWall, cNew, 3);
      ClientWall(cTeamB.m_cSuperWall, cNew, 6);

      // Leurre du tir double
      int nDecoy = nA & _FA_DECOY;
      if (nDecoy != 0) {
         ClientExtraPos(cNew, cA, cB, fK, 9, _FA_DECOY);
         ModSupers.ShowDecoy(GameField.m_cBall, m_vTmpPos);
      }

      if (nDecoy != m_nShownDecoy) {
         m_nShownDecoy = nDecoy;
         if (nDecoy == 0 && ModSupers.m_cDecoy != null) {
            ModSupers.m_cDecoy.EnableRendering(false);
         }
      }

      // Équipes gelées : teinte bleue
      int nFrozen = nB & (_FB_FROZEN_A | _FB_FROZEN_B);
      if (nFrozen != m_nShownFrozen) {
         ModSupers.Load();
         ClientFrozenLook(m_tTeam[0], (nFrozen & _FB_FROZEN_A) != 0, (m_nShownFrozen & _FB_FROZEN_A) != 0);
         ClientFrozenLook(m_tTeam[1], (nFrozen & _FB_FROZEN_B) != 0, (m_nShownFrozen & _FB_FROZEN_B) != 0);
         m_nShownFrozen = nFrozen;
      }

      // Sons en boucle des supers : coupés pendant qu'un menu est ouvert, comme chez l'hôte
      boolean bSound = cNew.nMenu == 0 && !m_bHostLost;
      Ball cBall = GameField.m_cBall;
      ClientLoop(_LOOP_SHOT, bSound && (nA & _FA_SUPER_SHOT) != 0 ? cBall : null, Ball.m_sSuperShotSounds[0]);
      ClientLoop(_LOOP_MAGNET, bSound && (nB & _FB_MAGNET) != 0 ? cBall : null, ModSupers._MAGNET_SOUND);
      ClientLoop(_LOOP_CRAZY, bSound && (nB & _FB_CRAZY) != 0 ? cBall : null, ModSupers._CRAZY_SOUND);
      ClientLoop(_LOOP_DECOY, bSound && nDecoy != 0 ? ModSupers.m_cDecoy : null, ModSupers._DOUBLE_SOUND);
      ClientLoop(_LOOP_WALL_A, bSound && (nB & _FB_WALL_A) != 0 ? cTeamA.m_cSuperWall : null, SuperWall.m_sSuperDefenceSounds[0]);
      ClientLoop(_LOOP_WALL_B, bSound && (nB & _FB_WALL_B) != 0 ? cTeamB.m_cSuperWall : null, SuperWall.m_sSuperDefenceSounds[1]);
      ClientLoop(_LOOP_SPEED, bSound ? ObjFromIndex(cNew.nSpeedObj) : null, "data/sounds/superspeed.wav");
      ClientLoop(_LOOP_FEINT, bSound ? ObjFromIndex(cNew.nFeintObj) : null, "data/sounds/superzwod.wav");
   }

   // Position d'un objet de la suite de l'état, dans m_vTmpPos. Interpolée seulement s'il était déjà là
   // dans les deux états (sinon il traverserait le terrain en apparaissant).
   static void ClientExtraPos(NetSnap cNew, NetSnap cA, NetSnap cB, float fK, int o, int nFlag) {
      if ((cA.nFlagsA & nFlag) != 0 && (cB.nFlagsA & nFlag) != 0) {
         m_vTmpPos.fX = cA.tExtra[o] + (cB.tExtra[o] - cA.tExtra[o]) * fK;
         m_vTmpPos.fY = cA.tExtra[o + 1] + (cB.tExtra[o + 1] - cA.tExtra[o + 1]) * fK;
         m_vTmpPos.fZ = cA.tExtra[o + 2] + (cB.tExtra[o + 2] - cA.tExtra[o + 2]) * fK;
      } else {
         m_vTmpPos.fX = cNew.tExtra[o];
         m_vTmpPos.fY = cNew.tExtra[o + 1];
         m_vTmpPos.fZ = cNew.tExtra[o + 2];
      }
   }

   static void ClientWall(SuperWall cWall, NetSnap cNew, int o) {
      if (cWall == null) {
         return;
      }

      m_vTmpPos.fX = cNew.tExtra[o];
      m_vTmpPos.fY = cNew.tExtra[o + 1];
      m_vTmpPos.fZ = cNew.tExtra[o + 2];
      if (m_vTmpPos.fX != 0.0F || m_vTmpPos.fY != 0.0F || m_vTmpPos.fZ != 0.0F) {
         cWall.SetPosition(m_vTmpPos);
      }
   }

   static void ClientFrozenLook(Team cTeam, boolean bFrozen, boolean bWasFrozen) {
      if (bFrozen == bWasFrozen) {
         return;
      }

      for (int i = 0; i < cTeam.GetFootBallerCount(); i++) {
         AIFootBaller cFB = (AIFootBaller)cTeam.GetFootBaller(i);
         if (cFB != null && (ModSupers.m_bFreezeKeeper || !cFB.IsGoalKeeper())) {
            cFB.SetMeshColor(bFrozen ? ModSupers.m_vFreezeColor : m_vWhite);
         }
      }
   }

   // Son en boucle attaché à un objet tant que l'état de l'hôte le dit. cObj null : arrêt.
   static void ClientLoop(int nSlot, ControlObject cObj, String sName) {
      if (m_tLoopObj[nSlot] == cObj) {
         return;
      }

      if (m_tLoopObj[nSlot] != null) {
         m_tLoopObj[nSlot].DetachSound(m_tLoopId[nSlot]);
      }

      m_tLoopObj[nSlot] = cObj;
      if (cObj != null) {
         m_tLoopId[nSlot] = cObj.AttachSound(sName);
      }
   }

   // Le menu du match suit celui de l'hôte : 0 fermé, 1 pause, 2 mi-temps, 3 fin de match
   static void ClientMenu(SoccerModule cModule, int nState) {
      if (nState == m_nShownMenu || cModule.m_cMenuInGameModule == null) {
         return;
      }

      Log("client : menu " + m_nShownMenu + " -> " + nState);
      m_nShownMenu = nState;
      if (cModule.m_bMenuVisible) {
         m_bLocalMenu = false; // un menu local ouvert est remplacé par celui de l'hôte
         // fin de la pause de mi-temps avec l'écran Tactique ouvert : ce qui y est réglé part chez l'hôte
         cModule.m_cMenuInGameModule.m_cMenuTactics.CommitIfOpen();
         cModule.m_bMenuVisible = false;
         cModule.m_cMenuInGameModule.m_cMenuYesNo.Hide();
         cModule.m_cMenuInGameModule.Deactivate();
         cModule.GetSoccerCamera().UnFreeze();
         cModule.TimerStart();
      }

      cModule.m_bMatchEnd = nState == 3;
      if (nState == 2) {
         cModule.ShowMenuHalfTime();
      } else if (nState != 0) {
         cModule.ShowMenu();
      }
   }

   // Crochet de SoccerCamera.OnPostUpdate. Chez le client, la caméra copie celle de l'hôte pendant les ralentis,
   // la fin de mi-temps et les pauses ; en jeu, elle suit la même cible mais avec les réglages du client.
   // Renvoie vrai si la caméra a été placée ici.
   static boolean ClientCamera(SoccerCamera cCam) {
      if (m_nRole != _ROLE_CLIENT || !m_bPuppets || !m_bCamReady || m_bCamFree) {
         return false;
      }

      m_vTmpPos.fX = m_tCam[0];
      m_vTmpPos.fY = m_tCam[1];
      m_vTmpPos.fZ = m_tCam[2];
      m_vTmpDir.fX = m_tCam[3];
      m_vTmpDir.fY = m_tCam[4];
      m_vTmpDir.fZ = m_tCam[5];
      if (m_vTmpDir.fX != 0.0F || m_vTmpDir.fY != 0.0F || m_vTmpDir.fZ != 0.0F) {
         cCam.SetCamPosDir(m_vTmpPos, m_vTmpDir);
      }

      return true;
   }

   // Marqueur au sol et flèche du HUD : le joueur qui a la main chez l'hôte
   static void ClientFocus(Team cTeam, int nFocus, int nMark) {
      cTeam.m_cFocusFBaller = nFocus >= 0 && nFocus < cTeam.GetFootBallerCount() ? cTeam.GetFootBaller(nFocus) : null;

      for (int i = 0; i < cTeam.GetFootBallerCount(); i++) {
         FootBaller cFB = cTeam.GetFootBaller(i);
         if (i == nMark) {
            if (!cFB.HasFocusMark()) {
               cFB.SetFocusMark();
            }
         } else if (cFB.HasFocusMark()) {
            cFB.ResetFocusMark();
         }
      }
   }

   static void ClientPlace(FootBaller cFB, NetSnap cA, NetSnap cB, float fK, int nObj) {
      if (cFB == null) {
         return;
      }

      int o = nObj * 6;
      m_vTmpPos.fX = cA.tObj[o] + (cB.tObj[o] - cA.tObj[o]) * fK;
      m_vTmpPos.fY = cA.tObj[o + 1] + (cB.tObj[o + 1] - cA.tObj[o + 1]) * fK;
      m_vTmpPos.fZ = cA.tObj[o + 2] + (cB.tObj[o + 2] - cA.tObj[o + 2]) * fK;
      NetSnap cDir = fK < 0.5F ? cA : cB;
      m_vTmpDir.fX = cDir.tObj[o + 3];
      m_vTmpDir.fY = cDir.tObj[o + 4];
      m_vTmpDir.fZ = cDir.tObj[o + 5];
      ((AIFootBaller)cFB).SetPositionWithY(m_vTmpPos);
      if (m_vTmpDir.fX != 0.0F || m_vTmpDir.fZ != 0.0F) {
         cFB.SetDir(m_vTmpDir);
      }

      if (!(cFB instanceof AIReferee)) {
         SoccerModule.GetHud().UpdateFBPosition(cFB);
      }
   }

   // Un nouveau match commence (SoccerModule.OnCreate)
   static void OnModuleCreate() {
      m_bPuppets = false;
      m_nShownScoreLeft = -1;
      m_nShownScoreRight = -1;
      m_nShownSecond = -1;
      m_nShownCam = -99;
      m_nCamTarget = _CAM_BALL;
      m_bCamReady = false;
      m_bCamFree = false;
      m_bShownCamReplay = false;
      m_nShownHud = -1;
      m_nShownReplay = -1;
      m_nShownMarker = -1;
      m_nShownDest = -1;
      m_nShownDecoy = -1;
      m_nShownFrozen = 0;
      m_nShownFade = -1;
      m_nShownSpeed = 100;
      m_nShownMenu = 0;
      m_bHostLost = false;
      m_tStats = null;
      m_bHudShown = true;
      m_fSpeedUp = 1.0F;
      m_bSaid = false;
      m_bWaitMenu = false;
      m_bLocalMenu = false;
      m_bWaitShown = false;
      m_nLoggedMenu = 0;
      m_lHalfEnd = 0L;
      m_nHalfLeft = -1;
      m_nHalfFlags = 0;
      m_nHalfShown = -2;
      m_tTeam[0] = null;
      m_tTeam[1] = null;
      m_nSnapCount = 0;
      m_bOffsetSet = false;
      m_bStarted = false;

      for (int i = 0; i < _LOOPS; i++) {
         m_tLoopObj[i] = null;
      }

      for (int i = 0; i < m_tLastAnim.length; i++) {
         m_tLastAnim[i] = null;
      }
   }

   // Le match est détruit (SoccerModule.OnDestroy). Après un départ, le jeu redevient hors ligne.
   static void OnModuleDestroy() {
      m_tTeam[0] = null;
      m_tTeam[1] = null;

      for (int i = 0; i < _LOOPS; i++) {
         m_tLoopObj[i] = null;
      }

      if (ModLobby.IsOpen()) {
         // match d'un salon : le canal reste ouvert, le joueur retourne dans le salon
         if (m_nRole != _ROLE_NONE) {
            Log("fin du match, retour au salon");
         }

         m_nRole = _ROLE_NONE;
         m_bLeft = false;
         m_cPeer = null;
         m_nHalfSeconds = 0;
      } else if (m_bLeft) {
         Log("retour hors ligne");
         m_nRole = _ROLE_NONE;
         m_bLeft = false;
         m_cPeer = null;
         CloseChannel();
      }
   }

   // ------------------------------------------------------------------ test d'écho (phase 0)
   // Lancé par NetTest("ip", port) dans debugconf.scr : 10 allers-retours avec server\echo_test.py

   static final int _TEST_MAGIC = 0x50534E30; // "PSN0"
   static final int _TEST_PINGS = 10;
   static String m_sTestHost = null;
   static int m_nTestPort = 0;
   static DatagramChannel m_cTestChannel = null;
   static ByteBuffer m_cTestBuf = null;
   static SocketAddress m_cTestAddr = null;
   static long m_lTestNext = 0L;
   static int m_nTestSent = 0;
   static int m_nTestRecv = 0;
   static int m_nTestFrames = 0;
   static boolean m_bTestDone = false;

   static void SetTest(String sHost, int nPort) {
      m_sTestHost = sHost;
      m_nTestPort = nPort;
      Log("test demande vers " + sHost + ":" + nPort);
   }

   static void TestFrame() {
      m_nTestFrames++;

      try {
         long lNow = System.currentTimeMillis();
         if (m_cTestChannel == null) {
            m_cTestChannel = DatagramChannel.open();
            m_cTestChannel.configureBlocking(false);
            m_cTestAddr = new InetSocketAddress(m_sTestHost, m_nTestPort);
            m_cTestBuf = ByteBuffer.allocate(1500);
            m_lTestNext = lNow;
            Log("test : canal ouvert");
         }

         if (m_nTestSent < _TEST_PINGS && lNow >= m_lTestNext) {
            m_cTestBuf.clear();
            m_cTestBuf.putInt(_TEST_MAGIC);
            m_cTestBuf.putInt(m_nTestSent);
            m_cTestBuf.putLong(lNow);
            m_cTestBuf.flip();
            m_cTestChannel.send(m_cTestBuf, m_cTestAddr);
            m_nTestSent++;
            m_lTestNext = lNow + 1000L;
         }

         for (int i = 0; i < 16; i++) {
            m_cTestBuf.clear();
            SocketAddress cFrom = m_cTestChannel.receive(m_cTestBuf);
            if (cFrom == null || m_cTestBuf.position() == 0) {
               break;
            }

            m_cTestBuf.flip();
            if (m_cTestBuf.remaining() >= 16 && m_cTestBuf.getInt() == _TEST_MAGIC) {
               int nSeq = m_cTestBuf.getInt();
               long lSent = m_cTestBuf.getLong();
               m_nTestRecv++;
               Log("test : echo " + nSeq + " en " + (lNow - lSent) + " ms (frame " + m_nTestFrames + ")");
            }
         }

         if (m_nTestSent >= _TEST_PINGS && lNow >= m_lTestNext + 1000L) {
            Log("test fini : " + m_nTestRecv + "/" + m_nTestSent + " echos, " + m_nTestFrames + " frames");
            m_cTestChannel.close();
            m_cTestChannel = null;
            m_bTestDone = true;
         }
      } catch (Throwable e) {
         Log("test ECHEC : " + e);
         m_bTestDone = true;
      }
   }
}
