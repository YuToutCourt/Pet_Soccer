import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

// [mod] Multijoueur en ligne, phases 3 et 4 (notes/online.md) : liaison au serveur (server/server.py), salon à code,
// choix d'avant-match échangés entre les deux joueurs, lancement du match ; compte du joueur (online.dat), partie
// rapide classée, Elo en ligne et classement.
//
// Le salon ne parle qu'au serveur, par le canal UDP de ModNet (les paquets du match, eux, peuvent passer en direct
// d'un jeu à l'autre : ModNet.PunchFrame). Rien n'est fiable ni ordonné : chaque paquet
// redit tout ce que le joueur veut (« je crée un salon », « je suis dans ce salon avec cette équipe »), et le
// serveur répond à chacun par l'état du salon. Une perte se rattrape donc au paquet suivant.
class ModLobby {
   static final int _LOBBY_PROTO = 5; // version du dialogue avec le serveur (server/protocol.py) ; 4 : ligue en ligne ; 5 : statistiques
   // jeu -> serveur
   static final int _PKT_FIRST = 16;
   static final int _PKT_HELLO = 16;
   static final int _PKT_CREATE = 17;
   static final int _PKT_JOIN = 18;
   static final int _PKT_LEAVE = 19;
   static final int _PKT_STATE = 20;
   static final int _PKT_QUEUE = 21; // partie rapide : file d'attente
   static final int _PKT_MATCH = 22; // où en est mon match classé
   static final int _PKT_TOP = 23; // demande le classement
   static final int _PKT_MATCH_STATS = 25; // [mod] statistiques du match, envoyées par le jeu de l'hôte (ModStats.PutMatchStats)
   // serveur -> jeu
   static final int _PKT_INFO = 32;
   static final int _PKT_PEER = 33;
   static final int _PKT_RANKS = 34;
   // phase d'un match classé annoncée au serveur (MATCH_* de protocol.py)
   static final int _MATCH_PLAYING = 1;
   static final int _MATCH_FINISHED = 2;
   static final int _MATCH_ABANDON = 3; // j'ai quitté le match en cours : forfait
   static final int _MATCH_PEER_LOST = 4; // j'ai quitté parce que l'autre ne répondait plus
   // issue d'un match classé donnée par le serveur (RESULT_* et HOW_* de protocol.py)
   static final int _RESULT_NONE = 0;
   static final int _RESULT_RATED = 1;
   static final int _RESULT_VOID = 2;
   static final int _HOW_PLAYED = 1;
   static final int _HOW_PEER_FORFEIT = 2;
   static final int _HOW_MY_FORFEIT = 3;
   static final int _TOP_ROWS = 8; // lignes du classement (TOP_ROWS de protocol.py)
   static final String _ACCOUNT_FILE = "online.dat";
   // liaison au serveur
   static final int _NET_OFF = 0;
   static final int _NET_CONNECTING = 1;
   static final int _NET_ONLINE = 2;
   static final int _NET_UNREACHABLE = 3; // plus de réponse
   static final int _NET_BAD_PROTO = 4; // le serveur parle une autre version
   static final int _NET_FAILED = 5; // adresse introuvable, canal impossible à ouvrir
   // ce que le joueur demande au serveur
   static final int _WANT_NONE = 0;
   static final int _WANT_CREATE = 1;
   static final int _WANT_JOIN = 2;
   static final int _WANT_QUEUE = 3; // partie rapide
   static final int _WANT_LEAGUE = 4; // ligue en ligne : j'attends le salon que le serveur ouvre pour mon match contre un ami
   static final int _ROLE_HOST = 1;
   static final int _ROLE_GUEST = 2;
   // pourquoi le joueur n'est pas (ou plus) dans un salon ; 1..3 = refus du serveur (JOIN_* de protocol.py)
   static final int _OUT_NONE = 0;
   static final int _OUT_UNKNOWN_CODE = 1;
   static final int _OUT_FULL = 2;
   static final int _OUT_VERSION = 3;
   static final int _OUT_CLOSED = 4; // l'hôte est parti, ou le serveur nous a oubliés
   static final int _OUT_NO_ROOM = 5; // le serveur ne peut plus créer de salon
   static final int _CODE_LENGTH = 4;
   static final int _LEVELS = 12; // stades proposés : ceux des 12 équipes d'origine
   static final long _SILENCE_MS = 6000L;

   // --- réglages (data\online.scr, puis debugconf.scr pour les tests) ---
   static String m_sServerHost = "localhost"; // remplacé par la ligne Server de data\online.scr
   static int m_nServerPort = 7777;
   static int m_nTeammateLevel = 5; // niveau de l'IA des coéquipiers, le même pour tous les matchs en ligne
   static int m_nRankedHalf = 4; // minutes d'une mi-temps en partie classée
   static int m_nHalfBreak = 30; // secondes de pause à la mi-temps d'un match en ligne (0 : pas de limite)
   static boolean m_bDirect = true; // liaison directe entre les deux jeux quand c'est possible
   static int m_nDebugDirect = -1; // NetDirect(0 ou 1) : remplace la ligne Direct de online.scr
   static boolean m_bConfigLoaded = false;
   static String m_sDebugHost = null; // NetServer("ip", port)
   static int m_nDebugPort = 0;
   static String m_sDebugName = null; // NetName("nom") : nom montré aux autres à la place de celui du profil
   static int m_nDebugQuit = 0; // NetQuit(secondes) : quitte le match au bout de ce temps (test du forfait)
   // NetAuto(1) : crée un salon et lance le match tout seul ; NetAuto(2) : rejoint le salon NetCode ;
   // NetAuto(3) : ouvre seulement le menu principal, sans passer par le choix du profil ;
   // NetAuto(4) : partie rapide, se met prêt tout seul (NetMatches matchs)
   static int m_nAuto = 0;
   static String m_sAutoCode = "TEST"; // NetCode("....")
   static int m_nAutoAnimal = -1; // NetTeam(animal, tenue)
   static int m_nAutoClothes = 0;
   static int m_nAutoMatches = 1; // NetMatches(n) : nombre de matchs lancés par NetAuto(1)
   static long m_lAutoQuit = 0L;
   static long m_lDebugQuit = 0L;

   // --- compte (online.dat : une ligne « profil;jeton » par profil) ---
   static String m_sToken = "";
   static String m_sTokenOwner = null; // profil auquel appartient m_sToken
   static int m_nMyElo = 0; // Elo en ligne, donné par le serveur (0 : pas encore connu)
   static int m_nMyGames = 0;
   static int m_nPeerElo = 0;
   static int m_nOnline = 0; // joueurs connectés au serveur

   // --- classement (dernier PKT_RANKS) ---
   static boolean m_bTopKnown = false;
   static int m_nTopCount = 0;
   static int m_nTopTotal = 0; // joueurs classés
   static int m_nMyRank = 0; // 0 : pas encore de match classé
   static int[] m_tTopRank = new int[_TOP_ROWS];
   static String[] m_tTopName = new String[_TOP_ROWS];
   static int[][] m_tTopStats = new int[_TOP_ROWS][4]; // Elo, victoires, nuls, défaites
   static long m_lNextTop = 0L;

   // --- match classé ---
   static boolean m_bRanked = false; // mon salon vient de la file d'attente : ses matchs comptent pour l'Elo
   static boolean m_bQueued = false; // le serveur m'a mis dans la file d'attente
   static boolean m_bRankedMatch = false; // le match en cours (ou qu'on vient de quitter) est classé
   static int m_nLastRankedMatch = 0;
   // ce que j'annonce au serveur (PKT_MATCH), une fois par seconde jusqu'à ce qu'il donne l'issue du match
   static int m_nRepMatch = 0; // 0 : rien à annoncer
   static int m_nRepPhase = 0;
   static int m_nRepLeft = 0; // buts de l'hôte du salon
   static int m_nRepRight = 0;
   static boolean m_bRepFinal = false; // la phase ne changera plus
   static boolean m_bRepHost = false; // mon jeu simule ce match : c'est lui qui envoie les statistiques
   static byte[] m_tRepStats = null; // statistiques du match fini (PKT_MATCH_STATS), prises tant que le match existe
   static int m_nRepTries = 0;
   static long m_lNextReport = 0L;
   // issue de mon dernier match classé, donnée par le serveur
   static int m_nResMatch = 0;
   static int m_nResOutcome = _RESULT_NONE;
   static int m_nResHow = 0;
   static int m_nResDelta = 0;
   static int m_nResLeft = 0; // score de ce match, pour l'affichage
   static int m_nResRight = 0;
   static boolean m_bResHost = false; // j'étais l'hôte (donc l'équipe de gauche du score)

   // --- liaison ---
   static boolean m_bMenu = false; // le joueur est dans les écrans « en ligne »
   static int m_nNet = _NET_OFF;
   static SocketAddress m_cServer = null;
   static int m_nClientId = 0;
   static int m_nSeq = 0; // numéro du dernier paquet envoyé
   static int m_nSeqFloor = 0; // les réponses à des paquets plus anciens ne comptent plus
   static ByteBuffer m_cOut = ByteBuffer.allocate(1500); // PKT_MATCH_STATS : jusqu'à 1 300 octets environ
   static long m_lNextSend = 0L;
   static long m_lFirstUnanswered = 0L;
   static int m_nUnanswered = 0;
   static boolean m_bHelloNeeded = true;
   static boolean m_bLeaveNeeded = false;
   // Le jeu vient d'ouvrir la liaison : si le serveur le dit déjà dans un salon (jeu relancé après un plantage,
   // reconnu à son compte), il y retourne
   static boolean m_bMayResume = false;

   // --- salon ---
   static int m_nWant = _WANT_NONE;
   static String m_sWantCode = "";
   static int m_nWantReplies = 0;
   static String m_sRoom = null; // code du salon où le serveur nous a mis
   static int m_nRole = 0;
   static int m_nOut = _OUT_NONE;
   static boolean m_bPeerHere = false;
   static String m_sPeerName = "";
   static int m_nQueueRange = 0; // file d'attente : écart d'Elo accepté par le serveur en ce moment
   static boolean m_bLeagueRoom = false; // salon ouvert par le serveur pour un match de la ligue en ligne (classé)

   // --- liaison directe (ModNet.PunchFrame) : ce que le serveur dit de l'autre joueur du salon ---
   static byte[] m_tLocalIp = new byte[4]; // mon adresse sur mon réseau local, annoncée au serveur
   static int m_nLinkKey = 0; // secret du salon, porté par les sondes ; 0 : pas de liaison directe
   static SocketAddress m_cPeerPublic = null; // l'autre joueur vu par le serveur
   static SocketAddress m_cPeerLocal = null; // son adresse locale, si nous sortons par la même box
   static long m_lPeerPublicRaw = 0L; // les mêmes sur 6 octets, pour ne refaire une adresse que si elle change
   static long m_lPeerLocalRaw = 0L;

   // --- mes choix ---
   static int m_nMyAnimal = -1;
   static int m_nMyClothes = 0;
   static boolean m_bReady = false;
   static String m_sMyTactics = "";
   // réglages de l'hôte
   static int m_nLevel = -1; // -1 : le stade de son équipe
   static int m_nHalfIndex = -1; // index dans CoreLogic._HALF_TIMES, -1 : celui de son profil
   static int m_nMatchId = 0; // 0 : l'hôte est dans le salon ; sinon numéro du match qu'il joue
   static int[] m_tMatchTeams = new int[4]; // équipe et tenue de gauche (hôte), puis de droite (invité)
   static int m_nMatchLevel = 0;
   static int m_nMatchHalf = 0;

   // --- les choix de l'autre joueur (dernier PKT_PEER) ---
   static boolean m_bPeerState = false;
   static int m_nPeerAnimal = -1;
   static int m_nPeerClothes = 0;
   static boolean m_bPeerReady = false;
   static String m_sPeerTactics = "";
   static int m_nPeerLevel = 0;
   static int m_nPeerHalf = 0;
   static int m_nPeerMatchId = 0;
   static int[] m_tPeerTeams = new int[4];

   static int m_nLastMatchId = 0; // dernier match où ce jeu est entré
   static ModTactics.Composition m_cRemoteComp = null; // chez l'hôte : composition envoyée par l'invité

   static void Log(String s) {
      GameObject.CrashLog("[mod] salon " + s + "\n");
   }

   static void LoadConfig() {
      if (m_bConfigLoaded) {
         return;
      }

      m_bConfigLoaded = true;

      for (ParseKeyword key = GameObject.ParseFile("data\\online.scr"); key != null; key = key.GetNext()) {
         if (key.Equals("Server")) {
            m_sServerHost = key.GetSParam(0);
            m_nServerPort = key.GetIParam(1);
         }

         if (key.Equals("TeammateLevel")) {
            m_nTeammateLevel = Math.max(1, Math.min(10, key.GetIParam(0)));
         }

         if (key.Equals("RankedHalf")) {
            m_nRankedHalf = Math.max(1, Math.min(10, key.GetIParam(0)));
         }

         if (key.Equals("HalfBreak")) {
            m_nHalfBreak = Math.max(0, Math.min(250, key.GetIParam(0)));
         }

         if (key.Equals("Direct")) {
            m_bDirect = key.GetIParam(0) != 0;
         }
      }

      if (m_sDebugHost != null) {
         m_sServerHost = m_sDebugHost;
         m_nServerPort = m_nDebugPort;
      }

      if (m_nDebugDirect >= 0) {
         m_bDirect = m_nDebugDirect != 0;
      }
   }

   // Secondes de pause à la mi-temps d'un match en ligne, chez l'hôte (ModNet.HostHalfBreak)
   static int GetHalfBreak() {
      LoadConfig();
      return m_nHalfBreak;
   }

   // Niveau de l'IA des coéquipiers en ligne (CoreLogic.GetDiffLevel) : fixe, il ne dépend pas du profil de l'hôte
   static int GetTeammateLevel() {
      LoadConfig();
      return m_nTeammateLevel;
   }

   // Le canal vers le serveur est ouvert (même si le serveur ne répond pas encore)
   static boolean IsOpen() {
      return m_cServer != null && m_nNet != _NET_OFF && m_nNet != _NET_FAILED;
   }

   static boolean IsMenu() {
      return m_bMenu;
   }

   static boolean InRoom() {
      return m_sRoom != null;
   }

   static boolean IsHost() {
      return m_sRoom != null && m_nRole == _ROLE_HOST;
   }

   static boolean IsGuest() {
      return m_sRoom != null && m_nRole == _ROLE_GUEST;
   }

   // Le match en cours est une partie classée (pas de « recommencer », quitter = forfait)
   static boolean IsRankedMatch() {
      return m_bRankedMatch && ModNet.IsOnline();
   }

   // ------------------------------------------------------------------ compte

   static String ProfileName() {
      PlayerSave cPlayer = CoreLogic.GetPlayer();
      return cPlayer != null && cPlayer.m_Name != null ? cPlayer.m_Name : "?";
   }

   // Nom montré aux autres joueurs : celui du profil
   static String MyName() {
      return m_sDebugName != null ? m_sDebugName : ProfileName();
   }

   // Jeton du compte en ligne du profil courant : lu dans online.dat, tiré au premier passage. Le serveur
   // reconnaît le joueur à ce jeton (pas de mot de passe) : qui copie online.dat joue sous ce compte.
   static void LoadToken() {
      String sProfile = ProfileName().replace(';', ',');
      if (sProfile.equals(m_sTokenOwner) && m_sToken.length() > 0) {
         return;
      }

      m_sTokenOwner = sProfile;
      m_sToken = "";
      java.io.File cFile = new java.io.File(_ACCOUNT_FILE);

      try {
         if (cFile.exists()) {
            java.io.BufferedReader in = new java.io.BufferedReader(new java.io.FileReader(cFile));

            for (String sLine = in.readLine(); sLine != null; sLine = in.readLine()) {
               int nSep = sLine.lastIndexOf(';');
               if (nSep > 0 && sLine.substring(0, nSep).equals(sProfile)) {
                  m_sToken = sLine.substring(nSep + 1).trim();
               }
            }

            in.close();
         }
      } catch (Exception e) {
         Log("ERREUR lecture de " + _ACCOUNT_FILE + " : " + e);
      }

      if (m_sToken.length() >= 16) {
         return;
      }

      m_sToken = NewToken(sProfile);

      try {
         java.io.FileWriter out = new java.io.FileWriter(_ACCOUNT_FILE, true);
         out.write(sProfile + ";" + m_sToken + "\r\n");
         out.close();
         Log("compte en ligne cree pour le profil " + sProfile);
      } catch (Exception e) {
         Log("ERREUR ecriture de " + _ACCOUNT_FILE + " : " + e + " (le compte sera perdu a la fermeture du jeu)");
      }
   }

   // 32 chiffres hexadécimaux tirés au hasard. Le JRE 1.4 du jeu met plusieurs secondes à préparer SecureRandom :
   // on mélange à la place tout ce qui varie d'un lancement et d'une machine à l'autre.
   static String NewToken(String sProfile) {
      StringBuffer sSeed = new StringBuffer(sProfile);
      sSeed.append(System.currentTimeMillis()).append(new java.util.Random().nextLong());
      sSeed.append(Runtime.getRuntime().freeMemory()).append(System.identityHashCode(new Object()));

      for (int i = 0; i < 8; i++) {
         // nombre de tours avant que l'horloge n'avance : dépend de la machine et de ce qu'elle fait
         long lStart = System.currentTimeMillis();
         int nSpins = 0;

         while (System.currentTimeMillis() == lStart && nSpins < 50000000) {
            nSpins++;
         }

         sSeed.append(nSpins).append(Math.random()).append(System.identityHashCode(new Object()));
      }

      StringBuffer sHex = new StringBuffer();

      try {
         byte[] tHash = java.security.MessageDigest.getInstance("SHA-1").digest(sSeed.toString().getBytes());

         for (int i = 0; i < 16; i++) {
            String s = Integer.toHexString(tHash[i] & 0xFF);
            sHex.append(s.length() == 1 ? "0" : "").append(s);
         }
      } catch (Throwable e) {
         java.util.Random cRandom = new java.util.Random(sSeed.toString().hashCode() ^ System.currentTimeMillis());
         sHex.setLength(0);

         for (int i = 0; i < 32; i++) {
            sHex.append(Integer.toHexString(cRandom.nextInt(16)));
         }
      }

      return sHex.toString();
   }

   // ------------------------------------------------------------------ ouverture, fermeture

   // MenuMain.OnOnline : le joueur entre dans l'écran « en ligne »
   static void Open() {
      m_bMenu = true;
      if (m_nNet == _NET_FAILED) {
         m_nNet = _NET_OFF; // nouvel essai
      }

      if (m_nNet != _NET_OFF) {
         return;
      }

      LoadConfig();
      ClearRoom();
      m_nWant = _WANT_NONE;
      m_nOut = _OUT_NONE;
      m_cServer = null;

      try {
         InetSocketAddress cAddr = new InetSocketAddress(m_sServerHost, m_nServerPort);
         if (cAddr.isUnresolved()) {
            Log("ECHEC : adresse du serveur introuvable (" + m_sServerHost + ")");
            m_nNet = _NET_FAILED;
            return;
         }

         if (!ModNet.OpenChannel(0)) {
            m_nNet = _NET_FAILED;
            return;
         }

         m_cServer = cAddr;
      } catch (Throwable e) {
         Log("ECHEC ouverture : " + e);
         m_nNet = _NET_FAILED;
         return;
      }

      m_nClientId = (int)((System.currentTimeMillis() ^ (long)(Math.random() * 2.147483647E9)) & 0x7FFFFFFFL);
      if (m_nClientId == 0) {
         m_nClientId = 1;
      }

      LoadToken();
      FindLocalIp();
      m_nNet = _NET_CONNECTING;
      m_bHelloNeeded = true;
      m_bLeaveNeeded = false;
      m_bMayResume = true;
      m_bQueued = false;
      m_bRankedMatch = false;
      m_nRepMatch = 0;
      m_nMyElo = 0;
      m_nMyGames = 0;
      m_nOnline = 0;
      m_bTopKnown = false;
      m_lNextTop = 0L;
      m_lNextSend = 0L;
      m_nUnanswered = 0;
      Log("serveur " + m_sServerHost + ":" + m_nServerPort + ", identifiant " + m_nClientId);
   }

   // Mon adresse sur mon réseau local : celle de la carte réseau par laquelle on joint le serveur. Une socket
   // jetable « connectée » au serveur la donne sans rien envoyer. InetAddress.getLocalHost() rendrait la
   // première carte venue (VPN, machine virtuelle).
   static void FindLocalIp() {
      m_tLocalIp = new byte[4];

      try {
         java.net.DatagramSocket cProbe = new java.net.DatagramSocket();
         cProbe.connect(m_cServer);
         byte[] t = cProbe.getLocalAddress().getAddress();
         cProbe.close();
         if (t != null && t.length == 4) {
            m_tLocalIp = t;
         }
      } catch (Throwable e) {
         Log("adresse locale inconnue : " + e);
      }
   }

   // Adresse et port sur 6 octets, dans un long (0 : aucune)
   private static long GetRawAddr(ByteBuffer b) {
      long l = 0L;

      for (int i = 0; i < 6; i++) {
         l = l << 8 | (long)(b.get() & 0xFF);
      }

      return l;
   }

   private static SocketAddress MakeAddr(long lRaw) {
      int nPort = (int)(lRaw & 0xFFFFL);
      if (lRaw >>> 16 == 0L || nPort == 0) {
         return null;
      }

      try {
         byte[] t = new byte[]{(byte)(lRaw >>> 40), (byte)(lRaw >>> 32), (byte)(lRaw >>> 24), (byte)(lRaw >>> 16)};
         return new InetSocketAddress(java.net.InetAddress.getByAddress(t), nPort);
      } catch (Throwable e) {
         return null;
      }
   }

   // La liaison directe est permise ici (online.scr, debugconf.scr) et le serveur a donné de quoi la tenter
   static boolean CanLink() {
      return m_bDirect && m_sRoom != null && m_nLinkKey != 0;
   }

   // MenuMain.Show : retour au menu principal, on quitte le salon et le serveur
   static void Close() {
      m_bMenu = false;
      if (m_nNet == _NET_OFF) {
         return;
      }

      if (IsOpen() && !ModNet.IsOnline()) {
         try {
            FlushReport();

            for (int i = 0; i < 2; i++) {
               Send(Begin(_PKT_LEAVE));
            }
         } catch (Throwable e) {
         }

         ModNet.CloseChannel();
      }

      Log("ferme");
      ModOnlineLeague.OnClose();
      m_nNet = _NET_OFF;
      m_cServer = null;
      m_nWant = _WANT_NONE;
      m_bQueued = false;
      m_bRankedMatch = false;
      m_nRepMatch = 0;
      ClearRoom();
   }

   static void ClearRoom() {
      m_sRoom = null;
      m_nRole = 0;
      m_bReady = false;
      m_bRanked = false;
      m_bLeagueRoom = false;
      m_nMatchId = 0;
      ClearPeer();
   }

   static void ClearPeer() {
      m_nLinkKey = 0;
      m_cPeerPublic = null;
      m_cPeerLocal = null;
      m_lPeerPublicRaw = 0L;
      m_lPeerLocalRaw = 0L;
      m_bPeerHere = false;
      m_sPeerName = "";
      m_nPeerElo = 0;
      m_bPeerState = false;
      m_bPeerReady = false;
      m_nPeerAnimal = -1;
      m_nPeerMatchId = 0;
      m_sPeerTactics = "";
   }

   // ------------------------------------------------------------------ demandes du joueur

   static void Create() {
      if (!IsOpen()) {
         return;
      }

      Want(_WANT_CREATE, "");
   }

   static void Join(String sCode) {
      if (!IsOpen()) {
         return;
      }

      Want(_WANT_JOIN, sCode);
   }

   // Partie rapide : le serveur me met dans un salon classé dès qu'un autre joueur attend aussi
   static void Queue() {
      if (!IsOpen()) {
         return;
      }

      Log("partie rapide : file d'attente");
      Want(_WANT_QUEUE, "");
   }

   // Quitte le salon, ou la file d'attente
   static void Leave() {
      if (m_sRoom != null) {
         Log("quitte le salon " + m_sRoom);
      }

      // L'issue d'un match classé qui vient de finir doit partir avant : sinon ce départ compterait comme un forfait
      try {
         FlushReport();
      } catch (Throwable e) {
      }

      Want(_WANT_NONE, "");
      m_bLeaveNeeded = true;
   }

   // Ligue en ligne : j'attends que le serveur ouvre le salon de mon match contre un ami (ModOnlineLeague redit
   // LG_PLAY). Sans cette demande, le jeu quitterait ce salon dès qu'il l'apprend.
   static void WantLeague() {
      if (IsOpen() && m_nWant != _WANT_LEAGUE) {
         Want(_WANT_LEAGUE, "");
      }
   }

   // Plus d'attente : on quitte le salon de la ligue, ou on cesse de l'attendre
   static void StopLeague() {
      if (m_sRoom != null) {
         Leave();
      } else if (m_nWant == _WANT_LEAGUE) {
         Want(_WANT_NONE, "");
      }
   }

   // Mon équipe dans le salon de la ligue : celle de la ligue, sans choix possible. Prêt tout de suite.
   static void SetLeagueTeam(int nAnimal, int nClothes) {
      if (!IsValidTeam(nAnimal, nClothes)) {
         return;
      }

      if (m_nMyAnimal != nAnimal || m_nMyClothes != nClothes || !m_bReady) {
         m_nMyAnimal = nAnimal;
         m_nMyClothes = nClothes;
         m_sMyTactics = ModTactics.ToLine(ModTactics.GetComposition(nAnimal));
         Log("ligue : equipe " + nAnimal + " tenue " + nClothes + ", pret");
      }

      SetReady(true);
   }

   private static void Want(int nWant, String sCode) {
      m_nWant = nWant;
      m_sWantCode = sCode;
      m_nWantReplies = 0;
      m_bMayResume = false;
      m_bQueued = false;
      m_nQueueRange = 0;
      m_nOut = _OUT_NONE;
      m_nSeqFloor = m_nSeq + 1;
      m_lNextSend = 0L;
      ClearRoom();
   }

   // Code de salon tapé par le joueur : 4 lettres, majuscules. Renvoie null s'il n'est pas valable.
   static String CleanCode(String s) {
      if (s == null) {
         return null;
      }

      s = s.trim().toUpperCase();
      if (s.length() != _CODE_LENGTH) {
         return null;
      }

      for (int i = 0; i < s.length(); i++) {
         if (s.charAt(i) < 'A' || s.charAt(i) > 'Z') {
            return null;
         }
      }

      return s;
   }

   // ------------------------------------------------------------------ mes choix

   static boolean IsValidTeam(int nAnimal, int nClothes) {
      return nAnimal >= 0 && nAnimal < EAnimals._ANIMALS_COUNT && nClothes >= 0 && nClothes < EAnimals.GetClothesCount(nAnimal);
   }

   // MenuOnline.Show : l'écran du salon revient (au début, après le choix de l'équipe, la tactique ou un match)
   static void OnScreenShown() {
      PlayerSave cPlayer = CoreLogic.GetPlayer();
      if (m_nMyAnimal < 0 && m_nAutoAnimal >= 0 && IsValidTeam(m_nAutoAnimal, m_nAutoClothes)) {
         m_nMyAnimal = m_nAutoAnimal;
         m_nMyClothes = m_nAutoClothes;
      }

      if (m_nMyAnimal < 0 || cPlayer != null && !cPlayer.IsTeamAvailable(m_nMyAnimal)) {
         m_nMyAnimal = Championships.m_nAvailableTeams[0];
         m_nMyClothes = 0;
      }

      if (m_nHalfIndex < 0 || m_nHalfIndex >= CoreLogic._HALF_TIMES.length) {
         m_nHalfIndex = cPlayer != null ? cPlayer.m_nMatchHalfTimeIndex : 1;
      }

      // L'écran Tactique travaille sur l'équipe de gauche : la mienne tant qu'on est dans les menus
      TeamData cData = ETeams.GetTeamData(m_nMyAnimal);
      cData.SetClothesId(m_nMyClothes);
      CoreLogic.m_cTeamDataLeft = cData;
      CoreLogic.m_cTeamDataRight = null;
      CoreLogic.m_bMultiplayer = false;
      CoreLogic.m_nNextLevel = -1;
      m_sMyTactics = ModTactics.ToLine(ModTactics.GetComposition(m_nMyAnimal));
      m_nMatchId = 0; // l'hôte est de retour dans le salon
      m_bRankedMatch = false;
      m_lNextSend = 0L;
      m_lNextTop = 0L; // le classement a pu bouger pendant le match
   }

   // MenuChooseTeamColors.OnOK
   static void SetMyTeam(int nAnimal, int nClothes) {
      if (IsValidTeam(nAnimal, nClothes)) {
         m_nMyAnimal = nAnimal;
         m_nMyClothes = nClothes;
         m_bReady = false;
      }
   }

   // MenuTactics.OnOK chez l'invité, à la mi-temps : sa nouvelle composition part chez l'hôte avec son état
   static boolean SetMyTactics(ModTactics.Composition c) {
      if (!IsOpen() || m_sRoom == null) {
         return false;
      }

      m_sMyTactics = ModTactics.ToLine(c);
      m_lNextSend = 0L;
      Log("composition envoyee a l'hote");
      return true;
   }

   static void SetReady(boolean bReady) {
      m_bReady = bReady && m_sRoom != null;
      m_lNextSend = 0L;
   }

   // Stade du match : celui choisi par l'hôte, par défaut celui de son équipe. En partie classée, toujours celui-là.
   static int GetLevel() {
      if (IsGuest()) {
         return m_nPeerLevel;
      }

      return m_nLevel >= 0 && !m_bRanked ? m_nLevel : ETeams.GetHomeField(m_nMyAnimal);
   }

   static boolean IsHomeLevel() {
      return !IsGuest() && (m_nLevel < 0 || m_bRanked);
   }

   // Durée d'une mi-temps en secondes, choisie par l'hôte. En partie classée : RankedHalf de online.scr.
   static int GetHalfSeconds() {
      if (IsGuest()) {
         return m_nPeerHalf;
      }

      if (m_bLeagueRoom) {
         return 60 * Math.max(1, ModOnlineLeague.m_nHalf); // celle choisie à la création de la ligue
      }

      return m_bRanked ? 60 * m_nRankedHalf : 60 * CoreLogic._HALF_TIMES[m_nHalfIndex];
   }

   static void NextLevel(int nDelta) {
      if (IsHost() && !m_bRanked) {
         // -1 (stade de l'hôte), puis les 12 stades
         m_nLevel = (m_nLevel + 1 + nDelta + _LEVELS + 1) % (_LEVELS + 1) - 1;
         m_lNextSend = 0L;
      }
   }

   static void NextHalf(int nDelta) {
      if (IsHost() && !m_bRanked) {
         int n = CoreLogic._HALF_TIMES.length;
         m_nHalfIndex = (m_nHalfIndex + nDelta + n) % n;
         m_lNextSend = 0L;
      }
   }

   // ------------------------------------------------------------------ lancement du match

   // En salon privé l'hôte lance quand l'invité est prêt. En partie classée les deux se mettent prêts, et le
   // match part tout seul (MenuOnline.Tick).
   static boolean CanStart() {
      return IsHost() && m_nNet == _NET_ONLINE && m_bPeerHere && m_bPeerState && m_bPeerReady && IsValidTeam(m_nPeerAnimal, m_nPeerClothes)
         && (!m_bRanked || m_bReady);
   }

   // L'hôte clique sur « jouer »
   static void HostStart() {
      if (!CanStart()) {
         return;
      }

      int nRightClothes = m_nPeerClothes;
      if (m_nPeerAnimal == m_nMyAnimal && nRightClothes == m_nMyClothes) {
         // même équipe et même tenue : l'invité prend la tenue suivante, pour qu'on distingue les deux camps
         nRightClothes = (nRightClothes + 1) % EAnimals.GetClothesCount(m_nPeerAnimal);
      }

      m_tMatchTeams[0] = m_nMyAnimal;
      m_tMatchTeams[1] = m_nMyClothes;
      m_tMatchTeams[2] = m_nPeerAnimal;
      m_tMatchTeams[3] = nRightClothes;
      m_nMatchLevel = GetLevel();
      m_nMatchHalf = GetHalfSeconds();
      int nId = (int)(System.currentTimeMillis() & 0x7FFFFFFFL);
      if (nId == 0 || nId == m_nLastMatchId) {
         nId++;
      }

      m_nMatchId = nId;
      m_cRemoteComp = ModTactics.FromLine(m_sPeerTactics);

      // L'invité doit l'apprendre avant que le chargement du stade ne fige ce jeu pendant plusieurs secondes
      try {
         for (int i = 0; i < 3; i++) {
            SendState();
         }
      } catch (Throwable e) {
         Log("ERREUR envoi du depart : " + e);
      }

      BeginMatch(true, m_tMatchTeams, m_nMatchLevel, m_nMatchHalf, m_nMatchId);
   }

   // MenuOnline.Tick, chez l'invité : l'hôte a lancé un match (ou en joue un quand on arrive dans le salon).
   // Renvoie vrai si le match démarre.
   static boolean GuestCheckStart() {
      if (!IsGuest() || !m_bPeerHere || !m_bPeerState || m_nPeerMatchId == 0 || m_nPeerMatchId == m_nLastMatchId) {
         return false;
      }

      if (!IsValidTeam(m_tPeerTeams[0], m_tPeerTeams[1]) || !IsValidTeam(m_tPeerTeams[2], m_tPeerTeams[3]) || m_nPeerLevel < 0 || m_nPeerLevel >= _LEVELS) {
         return false;
      }

      BeginMatch(false, m_tPeerTeams, m_nPeerLevel, m_nPeerHalf, m_nPeerMatchId);
      return true;
   }

   static void BeginMatch(boolean bHost, int[] tTeams, int nLevel, int nHalf, int nMatchId) {
      m_bReady = false;
      m_nLastMatchId = nMatchId;
      m_lAutoQuit = 0L;
      m_lDebugQuit = 0L;
      m_bRankedMatch = m_bRanked;
      m_nRepMatch = 0;
      m_tRepStats = null;
      if (m_nNet == _NET_ONLINE) {
         // Le serveur enregistre le match quand les deux jeux lui annoncent la même fin (ReportFrame) ; un match classé
         // change aussi l'Elo. Un salon privé est enregistré sans Elo, et le jeu n'en affiche pas l'issue.
         if (m_bRanked) {
            m_nLastRankedMatch = nMatchId;
            m_bResHost = bHost;
         }

         m_nRepMatch = nMatchId;
         m_nRepPhase = _MATCH_PLAYING;
         m_nRepLeft = 0;
         m_nRepRight = 0;
         m_bRepFinal = false;
         m_nRepTries = 0;
         m_lNextReport = 0L;
         m_bRepHost = bHost;
      }

      if (m_nAuto == 4) {
         m_nAutoMatches--;
      }

      CoreLogic.m_bChampionships = false;
      CoreLogic.m_bSurvival = false;
      CoreLogic.m_bLeague = false;
      CoreLogic.m_bDemo = false;
      CoreLogic.m_bMultiplayer = true;
      Match.m_cMatchData = null;
      // Des copies : les deux joueurs peuvent avoir pris la même équipe, avec deux tenues
      CoreLogic.m_cTeamDataLeft = new TeamData(ETeams.GetTeamData(tTeams[0]));
      CoreLogic.m_cTeamDataLeft.SetClothesId(tTeams[1]);
      CoreLogic.m_cTeamDataRight = new TeamData(ETeams.GetTeamData(tTeams[2]));
      CoreLogic.m_cTeamDataRight.SetClothesId(tTeams[3]);
      CoreLogic.m_nNextLevel = nLevel;
      if (m_bLeagueRoom) {
         ModOnlineLeague.OnFriendMatchBegin(bHost); // retour à l'écran de la ligue, but en or en playoffs
      }

      Log("match " + nMatchId + (bHost ? " (hote)" : " (invite)") + (m_bRanked ? " classe" : "") + (m_bLeagueRoom ? " (ligue)" : "") + " : equipe " + tTeams[0] + " tenue " + tTeams[1] + " contre equipe "
         + tTeams[2] + " tenue " + tTeams[3] + ", stade " + nLevel + ", mi-temps " + nHalf + " s");
      ModNet.StartMatch(bHost, m_cServer, nHalf);
      SoccerGame.game.GetCommentator().HideNow();
      SoccerGame.game.RunModule();
   }

   // ------------------------------------------------------------------ envoi

   static ByteBuffer Begin(int nType) {
      ByteBuffer b = m_cOut;
      b.clear();
      b.putInt(ModNet._MAGIC);
      b.put((byte)nType);
      b.putInt(m_nClientId);
      b.putInt(++m_nSeq);
      return b;
   }

   static void Send(ByteBuffer b) throws java.io.IOException {
      b.flip();
      ModNet.m_cChannel.send(b, m_cServer);
   }

   private static void SendHello() throws java.io.IOException {
      ByteBuffer b = Begin(_PKT_HELLO);
      b.putShort((short)_LOBBY_PROTO);
      b.putInt(ModNet._MOD_VERSION);
      ModNet.PutString(b, MyName());
      ModNet.PutString(b, m_sToken);
      // mon adresse locale : le serveur la donne à l'autre joueur si nous sortons par la même box
      b.put(m_tLocalIp);
      b.putShort((short)ModNet.m_cChannel.socket().getLocalPort());
      Send(b);
   }

   // ------------------------------------------------------------------ match classé : ce que j'annonce au serveur

   private static void SendReport() throws java.io.IOException {
      ByteBuffer b = Begin(_PKT_MATCH);
      b.putInt(m_nRepMatch);
      b.put((byte)m_nRepPhase);
      b.put((byte)m_nRepLeft);
      b.put((byte)m_nRepRight);
      // « je joue » : en liaison directe le serveur ne voit plus passer les paquets du match
      b.put((byte)(!m_bRepFinal && ModNet.IsSending() ? 1 : 0));
      Send(b);
      if (m_bRepFinal && m_tRepStats != null) {
         // le serveur ne garde que le premier reçu ; pas de réponse : celle de PKT_MATCH suffit
         b = Begin(_PKT_MATCH_STATS);
         b.put(m_tRepStats);
         Send(b);
      }
   }

   // [mod] statistiques du match qui vient de finir, pour le site du serveur. Seul le jeu de l'hôte les a (il simule).
   private static void TakeStats() {
      m_tRepStats = null;
      if (!m_bRepHost || !ModNet.MatchReady()) {
         return;
      }

      try {
         ByteBuffer b = ByteBuffer.allocate(m_cOut.capacity() - 13);
         ModStats.PutMatchStats(b, SoccerModule.m_cMatch, m_nRepMatch);
         b.flip();
         m_tRepStats = new byte[b.remaining()];
         b.get(m_tRepStats);
         Log("statistiques du match " + m_nRepMatch + " : " + m_tRepStats.length + " octets");
      } catch (Throwable e) {
         Log("ERREUR statistiques du match " + m_nRepMatch + " : " + e);
      }
   }

   // Avant de quitter le salon ou le serveur : la fin du match part tout de suite, deux fois
   private static void FlushReport() throws java.io.IOException {
      if (m_nRepMatch != 0 && m_bRepFinal && IsOpen() && m_nNet == _NET_ONLINE) {
         SendReport();
         SendReport();
      }
   }

   private static void ReadScore() {
      if (ModNet.MatchReady()) {
         m_nRepLeft = SoccerModule.m_cMatch.m_nScoreLeft;
         m_nRepRight = SoccerModule.m_cMatch.m_nScoreRight;
      }
   }

   // Chaque frame : suit le score et la fin du match classé, et les redit au serveur une fois par seconde
   // jusqu'à ce qu'il donne l'issue du match (ReadInfo)
   private static void ReportFrame(long lNow) throws java.io.IOException {
      if (m_nRepMatch == 0) {
         return;
      }

      if (!m_bRepFinal && ModNet.IsOnline() && !ModNet.m_bLeft && ModNet.MatchReady()) {
         int nLeft = m_nRepLeft;
         int nRight = m_nRepRight;
         ReadScore();
         if (SoccerGame.activeLevelModule.m_bMatchEnd) {
            m_nRepPhase = _MATCH_FINISHED;
            m_bRepFinal = true;
            Log("match en ligne " + m_nRepMatch + " fini : " + m_nRepLeft + "-" + m_nRepRight);
            TakeStats();
         }

         if (m_bRepFinal || nLeft != m_nRepLeft || nRight != m_nRepRight) {
            m_lNextReport = 0L;
         }
      }

      if (lNow < m_lNextReport) {
         return;
      }

      if (m_bRepFinal && ++m_nRepTries > 30) {
         Log("match en ligne " + m_nRepMatch + " : pas de reponse du serveur");
         m_nRepMatch = 0;
         return;
      }

      m_lNextReport = lNow + 1000L;
      SendReport();
   }

   // MenuInGame.Quit, avant ModNet.OnQuit : le joueur quitte le match. S'il n'est pas fini, c'est un forfait,
   // sauf si l'autre joueur avait déjà disparu (le serveur vérifie).
   static void OnMatchQuit(boolean bMatchEnd) {
      if (m_nRepMatch == 0 || m_bRepFinal || !ModNet.IsOnline()) {
         return;
      }

      ReadScore();
      m_nRepPhase = bMatchEnd ? _MATCH_FINISHED : (ModNet.PeerGone() ? _MATCH_PEER_LOST : _MATCH_ABANDON);
      m_bRepFinal = true;
      m_lNextReport = 0L;
      Log("match en ligne " + m_nRepMatch + " quitte : " + m_nRepLeft + "-" + m_nRepRight + ", phase " + m_nRepPhase);
      TakeStats();

      try {
         FlushReport();
      } catch (Throwable e) {
         Log("ERREUR envoi de la fin du match : " + e);
      }
   }

   // Issue du dernier match classé joué par ce jeu : _RESULT_NONE tant que le serveur n'a rien dit
   static int MatchResult() {
      return m_nLastRankedMatch != 0 && m_nResMatch == m_nLastRankedMatch ? m_nResOutcome : _RESULT_NONE;
   }

   // « +24 », « -8 »
   static String FormatDelta(int nDelta) {
      return nDelta >= 0 ? "+" + nDelta : "" + nDelta;
   }

   // Texte de l'issue du dernier match classé, pour le salon : « Victoire 2-1 : +24 Elo », ou "" s'il n'y en a pas
   static String ResultText() {
      int nResult = MatchResult();
      if (nResult == _RESULT_NONE) {
         return "";
      }

      if (nResult == _RESULT_VOID) {
         return Text.Get("&OnlineResVoid&");
      }

      String sWhat;
      if (m_nResHow == _HOW_PEER_FORFEIT) {
         sWhat = Text.Get("&OnlineResForfeitWin&");
      } else if (m_nResHow == _HOW_MY_FORFEIT) {
         sWhat = Text.Get("&OnlineResForfeitLoss&");
      } else {
         int nFor = m_bResHost ? m_nResLeft : m_nResRight;
         int nAgainst = m_bResHost ? m_nResRight : m_nResLeft;
         sWhat = Text.Get(nFor > nAgainst ? "&OnlineResWin&" : (nFor == nAgainst ? "&OnlineResDraw&" : "&OnlineResLoss&")) + " " + nFor + "-" + nAgainst;
      }

      return sWhat + " : " + FormatDelta(m_nResDelta) + " Elo";
   }

   // Couleur de ce texte : vert si l'Elo monte, rouge s'il descend, gris sinon
   static void SetResultColor(UIStatic cStatic) {
      if (MatchResult() == _RESULT_RATED && m_nResDelta > 0) {
         cStatic.SetTextColor(0.4F, 1.0F, 0.4F);
      } else if (MatchResult() == _RESULT_RATED && m_nResDelta < 0) {
         cStatic.SetTextColor(1.0F, 0.45F, 0.45F);
      } else {
         cStatic.SetTextColor(0.85F, 0.85F, 0.85F);
      }
   }

   // Ce que je montre à l'autre joueur. Le serveur le recopie sans le lire.
   private static void SendState() throws java.io.IOException {
      ByteBuffer b = Begin(_PKT_STATE);
      b.put((byte)((m_bReady ? 1 : 0) | (IsHost() ? 2 : 0)));
      b.put((byte)m_nMyAnimal);
      b.put((byte)m_nMyClothes);
      PutLong(b, m_sMyTactics);
      boolean bMatch = m_nMatchId != 0;
      b.put((byte)(bMatch ? m_nMatchLevel : GetLevel()));
      b.putShort((short)(bMatch ? m_nMatchHalf : GetHalfSeconds()));
      b.putInt(m_nMatchId);

      for (int i = 0; i < 4; i++) {
         b.put((byte)m_tMatchTeams[i]);
      }

      Send(b);
   }

   // Chaîne longue : deux octets de longueur, puis un octet par caractère
   static void PutLong(ByteBuffer b, String s) {
      if (s == null) {
         s = "";
      }

      int nLen = Math.min(s.length(), 600);
      b.putShort((short)nLen);

      for (int i = 0; i < nLen; i++) {
         b.put((byte)s.charAt(i));
      }
   }

   static String GetLong(ByteBuffer b) {
      int nLen = b.getShort() & 0xFFFF;
      if (nLen > b.remaining()) {
         throw new RuntimeException("chaine tronquee");
      }

      StringBuffer s = new StringBuffer(nLen);

      for (int i = 0; i < nLen; i++) {
         s.append((char)(b.get() & 0xFF));
      }

      return s.toString();
   }

   // Appelé à chaque frame par ModNet.OnFrame tant que le canal vers le serveur est ouvert
   static void OnFrame(long lNow) throws java.io.IOException {
      if (ModNet.IsOnline() || ModOnlineLeague.m_bMatch) {
         AutoMatchFrame(lNow);
      }

      if (m_nNet == _NET_ONLINE && !m_bHelloNeeded) {
         ReportFrame(lNow);
         ModOnlineLeague.Frame(lNow); // ligue en ligne : seulement devant ses écrans, ou pour finir une commande
         // classement : relu toutes les 5 s tant que le joueur est devant l'écran « en ligne », hors salon
         if (m_bMenu && m_sRoom == null && !ModNet.IsOnline() && lNow >= m_lNextTop) {
            m_lNextTop = lNow + 5000L;
            Send(Begin(_PKT_TOP));
         }
      }

      if (lNow < m_lNextSend) {
         return;
      }

      if (m_nNet != _NET_ONLINE || m_bHelloNeeded) {
         SendHello();
         m_lNextSend = lNow + (m_nNet == _NET_BAD_PROTO ? 3000L : 500L);
      } else if (m_nWant == _WANT_NONE) {
         if (m_bLeaveNeeded) {
            m_bLeaveNeeded = false;
            Send(Begin(_PKT_LEAVE));
            m_lNextSend = lNow + 300L;
         } else {
            SendHello(); // rien à demander : on reste connu du serveur
            m_lNextSend = lNow + 1000L;
         }
      } else if (m_sRoom == null && m_nWant == _WANT_LEAGUE) {
         SendHello(); // le salon viendra du serveur : on reste connu de lui
         m_lNextSend = lNow + 1000L;
      } else if (m_sRoom == null) {
         if (m_nWant == _WANT_CREATE) {
            Send(Begin(_PKT_CREATE));
         } else if (m_nWant == _WANT_QUEUE) {
            Send(Begin(_PKT_QUEUE)); // redit tant que j'attends : un joueur qui se tait sort de la file
         } else {
            ByteBuffer b = Begin(_PKT_JOIN);
            ModNet.PutString(b, m_sWantCode);
            Send(b);
         }

         m_lNextSend = lNow + 300L;
      } else {
         SendState();
         // pendant un match, il suffit de rester connu du serveur
         m_lNextSend = lNow + (ModNet.IsOnline() ? 1000L : 250L);
      }

      // Le chargement d'un stade fige le jeu plusieurs secondes : on ne compte le silence du serveur
      // que sur des paquets partis depuis sa dernière réponse
      if (m_nUnanswered == 0) {
         m_lFirstUnanswered = lNow;
      }

      m_nUnanswered++;
      if (m_nUnanswered >= 4 && lNow - m_lFirstUnanswered > _SILENCE_MS && (m_nNet == _NET_ONLINE || m_nNet == _NET_CONNECTING)) {
         Log("le serveur ne repond pas");
         m_nNet = _NET_UNREACHABLE;
      }
   }

   // ------------------------------------------------------------------ réception (ModNet.Receive)

   static void OnPacket(int nType, ByteBuffer b) {
      if (nType == _PKT_INFO) {
         ReadInfo(b);
      } else if (nType == _PKT_PEER) {
         ReadPeer(b);
      } else if (nType == _PKT_RANKS) {
         ReadRanks(b);
      } else if (nType == ModOnlineLeague._PKT_LEAGUE_INFO) {
         ModOnlineLeague.ReadInfo(b);
      }
   }

   private static void ReadRanks(ByteBuffer b) {
      int nMyRank = b.getShort();
      int nTotal = b.getShort();
      int nCount = Math.min(b.get() & 0xFF, _TOP_ROWS);

      for (int i = 0; i < nCount; i++) {
         m_tTopRank[i] = b.getShort();
         m_tTopName[i] = ModNet.GetString(b);

         for (int k = 0; k < 4; k++) {
            m_tTopStats[i][k] = b.getShort();
         }
      }

      // tout est lu : un paquet tronqué (exception plus haut) ne laisse pas un classement à moitié changé à l'écran
      m_nMyRank = nMyRank;
      m_nTopTotal = nTotal;
      m_nTopCount = nCount;
      m_bTopKnown = true;
   }

   private static void ReadInfo(ByteBuffer b) {
      int nSeq = b.getInt();
      int nStatus = b.get();
      m_nUnanswered = 0;
      if (nStatus == 1) {
         if (m_nNet != _NET_BAD_PROTO) {
            Log("le serveur parle une autre version");
         }

         m_nNet = _NET_BAD_PROTO;
         return;
      }

      if (nStatus == 2) {
         // serveur relancé, ou silence trop long de notre part : il faut se présenter de nouveau
         m_bHelloNeeded = true;
         m_lNextSend = 0L;
         return;
      }

      if (m_nNet != _NET_ONLINE) {
         Log("connecte au serveur");
      }

      m_nNet = _NET_ONLINE;
      m_bHelloNeeded = false;
      String sCode = ModNet.GetString(b);
      int nRole = b.get();
      int nMembers = b.get();
      String sPeerName = ModNet.GetString(b);
      int nJoinError = b.get();
      int nFlags = b.get();
      int nElo = b.getShort();
      int nGames = b.getShort();
      int nPeerElo = b.getShort();
      int nOnline = b.getShort();
      int nResMatch = b.getInt();
      int nResOutcome = b.get();
      int nResHow = b.get();
      int nResDelta = b.getShort();
      int nLinkKey = b.getInt();
      long lPeerPublic = GetRawAddr(b);
      long lPeerLocal = GetRawAddr(b);
      int nQueueRange = b.getShort();
      // Mon compte et l'issue de mon dernier match classé : vrais quelle que soit la demande en cours
      m_nMyElo = nElo;
      m_nMyGames = nGames;
      m_nOnline = nOnline;
      if (nResOutcome != _RESULT_NONE && nResMatch != 0 && nResMatch == m_nLastRankedMatch && nResMatch != m_nResMatch) {
         m_nResMatch = nResMatch;
         m_nResOutcome = nResOutcome;
         m_nResHow = nResHow;
         m_nResDelta = nResDelta;
         m_nResLeft = m_nRepLeft;
         m_nResRight = m_nRepRight;
         m_lNextTop = 0L;
         Log("match classe " + nResMatch + " : " + (nResOutcome == _RESULT_VOID ? "pas compte" : "compte (fin " + nResHow + "), Elo " + FormatDelta(nResDelta) + " -> " + nElo));
      }

      if (nResMatch != 0 && nResMatch == m_nRepMatch && nResOutcome != _RESULT_NONE) {
         m_nRepMatch = 0; // le serveur a tranché : plus rien à annoncer
      }

      if (nSeq < m_nSeqFloor) {
         return; // réponse à un paquet d'avant le dernier changement d'avis du joueur
      }

      m_bQueued = (nFlags & 1) != 0 && m_nWant == _WANT_QUEUE;
      m_nQueueRange = m_bQueued ? nQueueRange : 0;

      if (nJoinError != 0 && m_nWant == _WANT_JOIN && m_sRoom == null) {
         Log("entree refusee dans le salon " + m_sWantCode + " : " + nJoinError);
         m_nWant = _WANT_NONE;
         m_nOut = nJoinError >= _OUT_UNKNOWN_CODE && nJoinError <= _OUT_VERSION ? nJoinError : _OUT_UNKNOWN_CODE;
         return;
      }

      if (sCode.length() == 0) {
         if (m_sRoom != null) {
            Log("le salon " + m_sRoom + " est ferme");
            ClearRoom();
            m_nWant = _WANT_NONE;
            m_nOut = _OUT_CLOSED;
         } else if (m_nWant == _WANT_CREATE && ++m_nWantReplies > 20) {
            Log("le serveur ne cree pas de salon");
            m_nWant = _WANT_NONE;
            m_nOut = _OUT_NO_ROOM;
         }

         m_bMayResume = false;
         return;
      }

      if (m_nWant == _WANT_NONE) {
         if (!m_bMayResume) {
            m_bLeaveNeeded = true; // le serveur nous croit encore dans un salon
            return;
         }

         // Jeu relancé : le serveur a reconnu le compte et nous a rendu notre place dans le salon
         Log("retour dans le salon " + sCode);
         m_nWant = _WANT_JOIN;
         m_sWantCode = sCode;
      }

      m_bMayResume = false;
      if (m_sRoom == null) {
         Log("dans le salon " + sCode + (nRole == _ROLE_HOST ? " (hote)" : " (invite)") + ((nFlags & 2) != 0 ? ", partie classee" : "") + ((nFlags & 4) != 0 ? ", ligue" : ""));
         m_nOut = _OUT_NONE;
         m_lNextSend = 0L;
      }

      m_sRoom = sCode;
      m_nRole = nRole;
      m_bRanked = (nFlags & 2) != 0;
      m_bLeagueRoom = (nFlags & 4) != 0; // INFO_LEAGUE
      m_nPeerElo = nPeerElo;
      boolean bHere = nMembers >= 2;
      if (bHere != m_bPeerHere) {
         Log(bHere ? "l'autre joueur est la : " + sPeerName : "l'autre joueur est parti");
         if (!bHere) {
            ClearPeer();
         }
      }

      m_bPeerHere = bHere;
      m_sPeerName = bHere ? sPeerName : "";
      // Liaison directe : où joindre l'autre joueur. Son adresse change s'il relance son jeu.
      m_nLinkKey = bHere ? nLinkKey : 0;
      if (!bHere) {
         lPeerPublic = 0L;
         lPeerLocal = 0L;
      }

      if (lPeerPublic != m_lPeerPublicRaw || lPeerLocal != m_lPeerLocalRaw) {
         m_lPeerPublicRaw = lPeerPublic;
         m_lPeerLocalRaw = lPeerLocal;
         m_cPeerPublic = MakeAddr(lPeerPublic);
         m_cPeerLocal = MakeAddr(lPeerLocal);
         if (m_cPeerPublic != null) {
            Log("liaison directe possible : " + m_cPeerPublic + (m_cPeerLocal != null && lPeerLocal != lPeerPublic ? " ou " + m_cPeerLocal : ""));
         }
      }
   }

   private static void ReadPeer(ByteBuffer b) {
      if (m_sRoom == null) {
         return;
      }

      int nFlags = b.get();
      // Son rôle doit être l'inverse du mien : sinon c'est un paquet égaré d'un autre salon
      if (((nFlags & 2) != 0) == IsHost()) {
         return;
      }

      int nAnimal = b.get();
      int nClothes = b.get();
      String sTactics = GetLong(b);
      int nLevel = b.get();
      int nHalf = b.getShort();
      int nMatchId = b.getInt();

      for (int i = 0; i < 4; i++) {
         m_tPeerTeams[i] = b.get();
      }

      // Mi-temps : l'invité a revu sa composition (MenuTactics.OnOK). L'hôte l'applique à son équipe.
      if (IsHost() && ModNet.IsHost() && m_nMatchId != 0 && m_bPeerState && !sTactics.equals(m_sPeerTactics)) {
         m_cRemoteComp = ModTactics.FromLine(sTactics);
         ModNet.HostRemoteTactics();
      }

      m_nPeerAnimal = nAnimal;
      m_nPeerClothes = nClothes;
      m_sPeerTactics = sTactics;
      m_nPeerLevel = nLevel;
      m_nPeerHalf = nHalf;
      m_nPeerMatchId = nMatchId;
      m_bPeerReady = (nFlags & 1) != 0;
      m_bPeerState = true;
   }

   // ------------------------------------------------------------------ tests automatiques (NetAuto)

   // MenuOnline.Tick : fait à la place du joueur ce qu'il ferait à la souris
   static void AutoMenuFrame() {
      if (m_nAuto != 1 && m_nAuto != 2 && m_nAuto != 4 || m_nNet != _NET_ONLINE) {
         return;
      }

      if (m_nAuto == 4) {
         // partie rapide : file d'attente, puis prêt ; le match part quand les deux le sont
         if (m_nAutoMatches <= 0) {
            return;
         }

         if (m_sRoom == null && m_nWant == _WANT_NONE && m_nOut == _OUT_NONE) {
            Queue();
         } else if (m_sRoom != null && m_bRanked && !m_bReady) {
            SetReady(true);
         }
      } else if (m_nAuto == 1) {
         if (m_sRoom == null && m_nWant == _WANT_NONE && m_nOut == _OUT_NONE) {
            Create();
         } else if (CanStart() && m_nAutoMatches > 0) {
            m_nAutoMatches--;
            HostStart();
         }
      } else if (m_sRoom == null && m_nWant == _WANT_NONE && m_nOut == _OUT_NONE) {
         Join(m_sAutoCode);
      } else if (m_sRoom != null && !m_bReady) {
         SetReady(true);
      }
   }

   // Pendant un match lancé par NetAuto : quitte l'écran de fin au bout de 5 s, pour revenir au salon
   static void AutoMatchFrame(long lNow) {
      if (m_nAuto != 1 && m_nAuto != 2 && m_nAuto != 4 && m_nAuto != 5 && m_nAuto != 6) {
         return;
      }

      if (ModOnlineLeague.m_bMatch) {
         ModOnlineLeague.AutoMatchFrame(lNow); // ligue en ligne : match contre l'IA joué tout seul
      }

      SoccerModule cModule = SoccerGame.activeLevelModule;
      // NetQuit(n) : ouvre le menu du match n secondes après le coup d'envoi, puis quitte (test du forfait)
      if (m_nDebugQuit > 0 && cModule != null && !ModNet.m_bLeft && ModNet.MatchReady() && !cModule.m_bMatchEnd && cModule.m_cMenuInGameModule != null) {
         if (m_lDebugQuit == 0L) {
            m_lDebugQuit = lNow + 1000L * m_nDebugQuit;
         } else if (lNow >= m_lDebugQuit + 1500L) {
            m_lDebugQuit = 0L;
            Log("test : quitte le match en cours");
            cModule.m_cMenuInGameModule.m_cMenuInGame.Quit();
            return;
         } else if (lNow >= m_lDebugQuit && !cModule.m_bMenuVisible) {
            ModNet.OnEscape(cModule); // comme un joueur qui appuie sur échap : menu local, le match continue
         }
      }

      if (cModule == null || !cModule.m_bMenuVisible || !cModule.m_bMatchEnd || cModule.m_cMenuInGameModule == null) {
         m_lAutoQuit = 0L;
         return;
      }

      if (m_lAutoQuit == 0L) {
         m_lAutoQuit = lNow + 5000L;
      } else if (lNow >= m_lAutoQuit) {
         m_lAutoQuit = 0L;
         Log("test : fin du match, retour au salon");
         cModule.m_cMenuInGameModule.m_cMenuInGame.Quit();
      }
   }
}
