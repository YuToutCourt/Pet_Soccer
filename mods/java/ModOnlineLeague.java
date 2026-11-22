import java.nio.ByteBuffer;

// [mod] Ligue en ligne, côté jeu (notes/online-league.md) : plusieurs joueurs dans une ligue à 14 gardée par le
// serveur (server/leagues.py), les autres équipes jouées par l'IA.
//
// Comme le salon (ModLobby), par le même canal UDP : tant qu'un écran de la ligue est ouvert, le jeu redit au
// serveur « je regarde ma ligue » (LG_SYNC) une fois par seconde, ou la commande du joueur (créer, rejoindre...)
// jusqu'à ce que la réponse la confirme. Chaque réponse redit toute la ligue : membres, réglages, délai, et l'état
// de la ligue (la sauvegarde de ModLeague) quand il a changé. Le jeu le lit avec ModLeague.Load : MenuLeague et
// MenuLeagueBracket l'affichent comme une ligue solo dont le « joueur » est mon équipe.
class ModOnlineLeague {
   static final int _PKT_LEAGUE = 24;
   static final int _PKT_LEAGUE_INFO = 35;
   // commandes (LG_* de protocol.py)
   static final int _LG_SYNC = 0;
   static final int _LG_CREATE = 1;
   static final int _LG_JOIN = 2;
   static final int _LG_TEAM = 3;
   static final int _LG_START = 4;
   static final int _LG_LEAVE = 5;
   static final int _LG_RESULT = 6;
   static final int _LG_PLAY = 7;
   static final int _LG_SKIP = 8;
   // issue d'une commande (LGE_*)
   static final int _LGE_NONE = 0;
   static final int _LGE_UNKNOWN = 1;
   static final int _LGE_STARTED = 2;
   static final int _LGE_FULL = 3;
   static final int _LGE_TEAM_TAKEN = 4;
   static final int _LGE_NOT_OWNER = 5;
   static final int _LGE_NO_ACCOUNT = 6;
   static final int _LGE_REFUSED = 7;
   static final int _LGE_LIMIT = 8;
   static final int _LGE_VERSION = 9;
   static final int _LGE_REPLAY = 10;
   static final int _LGE_BAD = 11;
   // drapeaux de la ligue (LGF_*) et d'un membre (LGM_*)
   static final int _LGF_OWNER = 1;
   static final int _LGF_STARTED = 2;
   static final int _LGF_OVER = 4;
   static final int _LGM_ONLINE = 1;
   static final int _LGM_ME = 2;
   static final int _LGM_OWNER = 4;
   static final int _LGM_LEFT = 8;
   static final int _LGM_WAITING = 16;
   static final int _LGM_BUSY = 32;
   static final int _KEY_PLAYOFFS = 10000;
   static final int _MAX_MEMBERS = 14;
   // délai d'une journée proposé à la création, en minutes (0 : aucun)
   static final int[] _DELAYS = new int[]{360, 720, 1440, 2880, 4320, 0};

   // --- réglages de la prochaine ligue créée ---
   static int m_nDelayIndex = 2; // 24 h
   static int m_nHalfIndex = 1; // dans CoreLogic._HALF_TIMES : 4 min

   // --- écrans ---
   static boolean m_bScreen = false; // un écran de la ligue en ligne est ouvert : on suit la ligue
   static boolean m_bView = false; // MenuLeague et MenuLeagueBracket montrent la ligue en ligne (pas celle du profil)
   static boolean m_bPickTeam = false; // le choix de l'équipe a été ouvert depuis l'écran de la ligue
   static int m_nChanges = 0; // augmente à chaque réponse : les écrans se relisent quand il change

   // --- commande en cours, redite jusqu'à la réponse ---
   static int m_nCmd = -1;
   static String m_sCmdCode = "";
   static int m_nCmdKey = 0;
   static int m_nCmdGoalsHost = 0;
   static int m_nCmdGoalsGuest = 0;
   static int m_nCmdFirstSeq = 0; // numéro du premier envoi : une réponse à partir de lui confirme la commande
   static int m_nSeqFloor = 0; // les réponses plus anciennes ne comptent plus
   static long m_lNextSend = 0L;
   static int m_nLastCmd = -1; // dernière commande confirmée, et son issue
   static int m_nLastError = _LGE_NONE;
   static long m_lLastErrorAt = 0L; // l'issue n'est plus montrée 10 s après

   // --- ma ligue (dernière réponse du serveur) ---
   static boolean m_bHave = false;
   static String m_sCode = "";
   static int m_nFlags = 0;
   static int m_nVersion = 0; // version de m_cLeague (0 : rien), redite au serveur
   static int m_nQualifiers = 0;
   static int m_nDelay = 0;
   static int m_nHalf = 0;
   static int m_nDeadline = -1; // secondes avant la fin du délai, au moment de la réponse (-1 : aucun)
   static long m_lDeadlineAt = 0L;
   static int m_nMyTeam = -1;
   static int m_nMyKey = -1;
   static int m_nMembers = 0;
   static String[] m_tName = new String[_MAX_MEMBERS];
   static int[] m_tAnimal = new int[_MAX_MEMBERS];
   static int[] m_tClothes = new int[_MAX_MEMBERS];
   static int[] m_tMember = new int[_MAX_MEMBERS]; // drapeaux _LGM_*
   static ModLeague m_cLeague = null;
   static boolean m_bAnswered = false; // le serveur a répondu depuis l'ouverture des écrans

   // --- match contre l'IA (phase 3) : de « jouer » au retour à l'écran de la ligue ---
   static boolean m_bMatch = false;
   static int m_nMatchKey = -1;
   static boolean m_bMatchHost = false; // mon équipe reçoit
   static boolean m_bMatchGolden = false; // playoffs : but en or à égalité
   static String m_sLastMatch = ""; // « Victoire 2-1 contre Gorillakov », montré au retour
   static int m_nLastMatchOutcome = 0; // 1 victoire, 0 nul, -1 défaite

   // --- match contre un ami (phase 4) : les deux cliquent « jouer », le serveur ouvre un salon classé entre eux ---
   static final long _WAIT_PLAY_MS = 300L; // LG_PLAY redit pendant l'attente (le serveur l'oublie après 3 s)
   static final long _ROOM_START_MS = 20000L; // salon ouvert sans que le match parte : on abandonne
   static boolean m_bWaitFriend = false; // « jouer » cliqué : j'attends l'autre joueur, puis le départ du match
   static int m_nWaitKey = -1;
   static long m_lRoomSince = 0L; // arrivée dans le salon de la ligue
   static boolean m_bFriendMatch = false; // le match en ligne en cours est celui de la ligue (retour à l'écran 26)
   static boolean m_bLastFriend = false; // mon dernier match était contre un ami : l'écran montre son issue (Elo en ligne)
   static String m_sFriendStop = ""; // pourquoi l'attente s'est arrêtée toute seule (clé de texte), "" sinon
   static long m_lFriendStopAt = 0L;
   static boolean m_bDebugGolden = false; // debugconf.scr : NetGolden(1), but en or à égalité dans tout match entre amis

   // --- score à envoyer, gardé dans online_league.dat (une ligne « profil;code;clé;buts reçoit;buts invitée »)
   //     jusqu'à la réponse du serveur : un jeu fermé ou une coupure ne le perdent pas ---
   static final String _PENDING_FILE = "online_league.dat";
   static boolean m_bPending = false;
   static String m_sPendCode = "";
   static int m_nPendKey = -1;
   static int m_nPendHost = 0;
   static int m_nPendGuest = 0;
   // issue de l'envoi du dernier score
   static final int _SENT_NONE = 0;
   static final int _SENT_OK = 1;
   static final int _SENT_LATE = 2; // délai dépassé : le serveur avait déjà simulé ce match
   static final int _SENT_REPLAY = 3; // nul en playoffs (match interrompu) : à rejouer
   static int m_nSent = _SENT_NONE;
   static long m_lSentAt = 0L;

   static void Log(String s) {
      GameObject.CrashLog("[mod] ligue en ligne : " + s + "\n");
   }

   static boolean IsStarted() {
      return m_bHave && (m_nFlags & _LGF_STARTED) != 0 && m_cLeague != null;
   }

   static boolean IsOwner() {
      return m_bHave && (m_nFlags & _LGF_OWNER) != 0;
   }

   static boolean IsOver() {
      return m_bHave && (m_nFlags & _LGF_OVER) != 0;
   }

   static boolean IsBusy() {
      return m_nCmd >= 0;
   }

   // La ligue que montrent MenuLeague et MenuLeagueBracket : celle du serveur, ou celle du profil (mode Ligue solo)
   static ModLeague GetViewLeague() {
      return m_bView ? (IsStarted() ? m_cLeague : null) : ModLeagueMode.GetLeague();
   }

   // Secondes avant la fin du délai de la journée, -1 s'il n'y en a pas
   static int DeadlineLeft() {
      if (!m_bHave || m_nDeadline < 0) {
         return -1;
      }

      return Math.max(0, m_nDeadline - (int)((System.currentTimeMillis() - m_lDeadlineAt) / 1000L));
   }

   // Index du membre qui joue cette équipe (index local de la ligue), -1 : l'IA
   static int MemberOfTeam(int nTeam) {
      if (m_cLeague == null || nTeam < 0 || nTeam >= m_cLeague.m_nTeams) {
         return -1;
      }

      for (int i = 0; i < m_nMembers; i++) {
         if (m_tAnimal[i] == m_cLeague.m_tTeam[nTeam] && (m_tMember[i] & _LGM_LEFT) == 0) {
            return i;
         }
      }

      return -1;
   }

   static void Clear() {
      m_bHave = false;
      m_sCode = "";
      m_nFlags = 0;
      m_nVersion = 0;
      m_nDeadline = -1;
      m_nMyTeam = -1;
      m_nMyKey = -1;
      m_nMembers = 0;
      m_cLeague = null;
   }

   // ModLobby.Close : on quitte les écrans en ligne (un score pas encore confirmé reste dans online_league.dat)
   static void OnClose() {
      m_bScreen = false;
      m_bView = false;
      m_bMatch = false;
      m_bFriendMatch = false;
      m_bWaitFriend = false;
      m_bAnswered = false;
      m_nCmd = -1;
      Clear();
   }

   // ------------------------------------------------------------------ match contre l'IA (phase 3)

   // (équipe qui reçoit, invitée) d'un match de la ligue, en index locaux
   static int[] Sides(int nKey) {
      ModLeague l = m_cLeague;
      if (l == null || nKey < 0) {
         return null;
      } else if (nKey < _KEY_PLAYOFFS) {
         return nKey < l.m_nFixtures ? new int[]{l.m_tFHost[nKey], l.m_tFGuest[nKey]} : null;
      } else {
         int m = nKey - _KEY_PLAYOFFS;
         return m < l.m_nMatches ? new int[]{l.GetHost(m), l.GetGuest(m)} : null;
      }
   }

   // L'adversaire de mon prochain match est un autre joueur (match entre amis : phase 4)
   static boolean IsNextVsHuman() {
      int[] t = Sides(m_nMyKey);
      return t != null && MemberOfTeam(t[0] == m_nMyTeam ? t[1] : t[0]) >= 0;
   }

   // Clé de mon prochain match s'il se joue maintenant contre l'IA, sinon -1
   static int PlayableKey() {
      if (!IsStarted() || IsOver() || m_nMyKey < 0 || m_nMyTeam < 0 || Sides(m_nMyKey) == null || IsNextVsHuman()) {
         return -1;
      }

      // son score attend encore la réponse du serveur
      return m_bPending && m_nPendKey == m_nMyKey && m_sPendCode.equals(m_sCode) ? -1 : m_nMyKey;
   }

   static int GetHalfSeconds() {
      return ModNet.m_nDebugHalf > 0 ? ModNet.m_nDebugHalf : 60 * Math.max(1, m_nHalf);
   }

   // Match.Update : en ligne, seul l'hôte décide (le client applique son état, but en or compris)
   static boolean IsGoldenGoal() {
      return (m_bMatch || m_bFriendMatch) && m_bMatchGolden && !CoreLogic.m_bDemo;
   }

   // « jouer » : mon équipe à gauche, l'IA à droite, au stade de l'équipe qui reçoit (comme ModLeagueMode.PrepareMatch).
   // LG_PLAY prévient le serveur : le délai de la journée épargne ce match pendant qu'il se joue.
   static boolean PrepareMatch() {
      int nKey = PlayableKey();
      int[] t = Sides(nKey);
      if (t == null) {
         return false;
      }

      ModLeague l = m_cLeague;
      int nMine = l.m_tTeam[m_nMyTeam];
      int nOther = l.m_tTeam[t[0] == m_nMyTeam ? t[1] : t[0]];
      int nClothes = 0;

      for (int i = 0; i < m_nMembers; i++) {
         if ((m_tMember[i] & _LGM_ME) != 0) {
            nClothes = m_tClothes[i];
         }
      }

      TeamData cMine = ETeams.GetTeamData(nMine);
      cMine.SetClothesId(nClothes >= 0 && nClothes < EAnimals.GetClothesCount(nMine) ? nClothes : 0);
      CoreLogic.m_cTeamDataLeft = cMine;
      CoreLogic.m_cTeamDataRight = ETeams.GetTeamData(nOther);
      CoreLogic.m_nNextLevel = ETeams.GetHomeField(l.m_tTeam[t[0]]);
      CoreLogic.m_bChampionships = false;
      CoreLogic.m_bSurvival = false;
      CoreLogic.m_bMultiplayer = false;
      CoreLogic.m_bDemo = false;
      CoreLogic.m_bLeague = true; // « recommencer » grisé, comme en ligue solo (ModLeagueMode.IsActive l'ignore : m_bMatch)
      Match.m_cMatchData = null;
      m_bMatch = true;
      m_nMatchKey = nKey;
      m_bMatchHost = t[0] == m_nMyTeam;
      m_bMatchGolden = nKey >= _KEY_PLAYOFFS;
      m_nCmdKey = nKey;
      Command(_LG_PLAY);
      Log("match " + nKey + " : equipe " + nMine + " contre " + nOther + " (IA)" + (m_bMatchGolden ? ", but en or" : ""));
      return true;
   }

   // MenuOnlineLeague.Show : retour à l'écran de la ligue (fin du match, ou avant-match abandonné)
   static void EndMatchMode() {
      if (m_bMatch) {
         m_bMatch = false;
         CoreLogic.m_bLeague = false;
      }

      m_bFriendMatch = false;
   }

   // ------------------------------------------------------------------ match contre un ami (phase 4)

   // Mon prochain match est contre un ami, qui n'a pas quitté la ligue
   static boolean CanPlayFriend() {
      if (!IsStarted() || IsOver() || m_nMyKey < 0 || m_nMyTeam < 0 || !IsNextVsHuman()) {
         return false;
      }

      // ce match vient d'être joué : l'état de la ligue qui le compte arrive (sauf match pas compté, à rejouer)
      int nResult = ModLobby.MatchResult();
      boolean bCounting = nResult == ModLobby._RESULT_RATED || nResult == ModLobby._RESULT_NONE && ModLobby.m_nRepMatch != 0;
      return !m_bLastFriend || m_nMatchKey != m_nMyKey || !bCounting;
   }

   // Index du membre contre qui je joue mon prochain match, -1 : l'IA (ou pas de match)
   static int Opponent() {
      int[] t = Sides(m_nMyKey);
      return t == null ? -1 : MemberOfTeam(t[0] == m_nMyTeam ? t[1] : t[0]);
   }

   // Ma tenue dans la ligue
   static int MyClothes() {
      for (int i = 0; i < m_nMembers; i++) {
         if ((m_tMember[i] & _LGM_ME) != 0) {
            return m_tClothes[i];
         }
      }

      return 0;
   }

   // « jouer » contre un ami : LG_PLAY redit jusqu'à ce qu'il clique aussi ; le serveur ouvre alors le salon
   static void StartWaiting() {
      if (!CanPlayFriend() || !ModLobby.IsOpen()) {
         return;
      }

      m_bWaitFriend = true;
      m_nWaitKey = m_nMyKey;
      m_lRoomSince = 0L;
      m_sFriendStop = "";
      m_bLastFriend = false;
      m_sLastMatch = "";
      m_lNextSend = 0L;
      ModLobby.WantLeague();
      int nOther = Opponent();
      Log("match " + m_nWaitKey + " contre " + (nOther >= 0 ? m_tName[nOther] : "?") + " : attente");
   }

   // Fin de l'attente : le joueur annule, ou elle ne peut plus aboutir (sStop : clé du texte à montrer, "" sinon)
   static void StopWaiting(String sStop) {
      if (!m_bWaitFriend) {
         return;
      }

      m_bWaitFriend = false;
      m_sFriendStop = sStop;
      m_lFriendStopAt = System.currentTimeMillis();
      ModLobby.StopLeague();
      Log("match " + m_nWaitKey + " : attente arretee" + (sStop.length() > 0 ? " (" + sStop + ")" : ""));
   }

   // MenuOnlineLeague.Tick : suit l'attente et le salon de la ligue. Renvoie vrai si le match démarre.
   static boolean FriendFrame() {
      long lNow = System.currentTimeMillis();
      if (!m_bWaitFriend) {
         // retour d'un match contre un ami : on quitte le salon une fois l'issue donnée par le serveur
         if (ModLobby.m_bLeagueRoom && !ModNet.IsOnline() && ModLobby.m_nRepMatch == 0) {
            Log("fin du match contre un ami : on quitte le salon");
            ModLobby.StopLeague();
         }

         return false;
      }

      if (m_nMyKey != m_nWaitKey || !CanPlayFriend()) {
         StopWaiting("LeagueOnWaitGone"); // la journée s'est fermée (délai, « passer »), ou l'ami a quitté la ligue
         return false;
      }

      if (!ModLobby.InRoom()) {
         if (ModLobby.m_nWant != ModLobby._WANT_LEAGUE) {
            // salon fermé avant le départ (l'autre est parti) : on recommence à attendre
            m_lRoomSince = 0L;
            ModLobby.WantLeague();
         }

         return false;
      }

      if (!ModLobby.m_bLeagueRoom) {
         return false;
      }

      if (m_lRoomSince == 0L) {
         m_lRoomSince = lNow;
         Log("salon " + ModLobby.m_sRoom + " ouvert" + (ModLobby.IsHost() ? " (je recois)" : ""));
      }

      if (!ModLobby.m_bPeerHere && lNow - m_lRoomSince > 3000L) {
         // l'ami est sorti du salon avant le départ : on le quitte aussi (sinon le serveur le croirait encore
         // ouvert pour ce match), et on attend de nouveau
         Log("l'ami n'est plus dans le salon");
         ModLobby.StopLeague();
         m_lRoomSince = 0L;
         return false;
      }

      ModLobby.SetLeagueTeam(m_cLeague.m_tTeam[m_nMyTeam], MyClothes());
      if (ModLobby.GuestCheckStart()) {
         return true;
      }

      if (ModLobby.CanStart()) {
         ModLobby.HostStart();
         return true;
      }

      if (lNow - m_lRoomSince > _ROOM_START_MS) {
         StopWaiting("LeagueOnWaitFailed");
      }

      return false;
   }

   // ModLobby.BeginMatch, dans le salon de la ligue : le match contre l'ami part
   static void OnFriendMatchBegin(boolean bHost) {
      m_bWaitFriend = false;
      m_bFriendMatch = true;
      m_bLastFriend = true;
      m_sLastMatch = "";
      m_nMatchKey = m_nWaitKey;
      m_bMatchGolden = m_nWaitKey >= _KEY_PLAYOFFS || m_bDebugGolden;
      Log("match " + m_nWaitKey + " contre un ami" + (bHost ? " (je recois)" : "") + (m_bMatchGolden ? ", but en or" : ""));
   }

   // Issue de mon dernier match contre un ami, pour l'écran : « Victoire 2-1 : +24 Elo », ou l'attente du serveur
   static String FriendResultText() {
      String s = ModLobby.ResultText();
      return s.length() > 0 ? s : T("LeagueOnResultWait");
   }

   // AIMatch.AfterMatch : fin du match contre l'IA. Le score part au serveur et reste dans online_league.dat
   // tant qu'il n'a pas répondu. Quitter un match en cours ne compte pas : il reste à jouer.
   static void OnMatchEnd(AIMatch cMatch) {
      if (!m_bMatch || CoreLogic.m_bDemo || CoreLogic.m_bMultiplayer || m_nMatchKey < 0) {
         return;
      }

      int nHostTeam = cMatch.m_nHalfNo != 0 && !cMatch.m_bGoldGoal ? 1 : 0;
      boolean bLeft = cMatch.GetTeam(nHostTeam).m_bUserControled;
      int nFor = bLeft ? cMatch.m_nScoreLeft : cMatch.m_nScoreRight;
      int nAgainst = bLeft ? cMatch.m_nScoreRight : cMatch.m_nScoreLeft;
      int[] t = Sides(m_nMatchKey);
      int nOther = t != null ? m_cLeague.m_tTeam[t[0] == m_nMyTeam ? t[1] : t[0]] : -1;
      m_nLastMatchOutcome = nFor > nAgainst ? 1 : (nFor == nAgainst ? 0 : -1);
      m_sLastMatch = T(nFor > nAgainst ? "OnlineResWin" : (nFor == nAgainst ? "OnlineResDraw" : "OnlineResLoss")) + " " + nFor + "-" + nAgainst
         + (nOther >= 0 ? " " + T("LeagueAgainst") + " " + ETeams.GetTeamData(nOther).m_sNameAbr : "") + (cMatch.m_bGoldGoal ? " - " + T("SurvivalGolden") : "");
      m_bPending = true;
      m_sPendCode = m_sCode;
      m_nPendKey = m_nMatchKey;
      m_nPendHost = m_bMatchHost ? nFor : nAgainst;
      m_nPendGuest = m_bMatchHost ? nAgainst : nFor;
      SavePending();
      Log("fin du match " + m_nMatchKey + " : " + nFor + "-" + nAgainst + ", score " + m_nPendHost + "-" + m_nPendGuest + " a envoyer");
      SendPending();
   }

   static void SendPending() {
      m_nCmdKey = m_nPendKey;
      m_nCmdGoalsHost = m_nPendHost;
      m_nCmdGoalsGuest = m_nPendGuest;
      Command(_LG_RESULT);
   }

   // ------------------------------------------------------------------ online_league.dat

   static String ProfileKey() {
      return ModLobby.ProfileName().replace(';', ',');
   }

   // Lignes du fichier, sauf celle du profil courant
   private static java.util.Vector OtherLines() {
      java.util.Vector v = new java.util.Vector();
      java.io.File f = new java.io.File(_PENDING_FILE);
      if (!f.exists()) {
         return v;
      }

      try {
         java.io.BufferedReader in = new java.io.BufferedReader(new java.io.FileReader(f));
         String sMe = ProfileKey() + ";";

         for (String s = in.readLine(); s != null; s = in.readLine()) {
            if (s.length() > 0 && !s.startsWith(sMe)) {
               v.addElement(s);
            }
         }

         in.close();
      } catch (Exception e) {
         Log("ERREUR lecture de " + _PENDING_FILE + " : " + e);
      }

      return v;
   }

   private static void WriteLines(java.util.Vector v) {
      try {
         java.io.FileWriter out = new java.io.FileWriter(_PENDING_FILE);

         for (int i = 0; i < v.size(); i++) {
            out.write((String)v.elementAt(i) + "\r\n");
         }

         out.close();
      } catch (Exception e) {
         Log("ERREUR ecriture de " + _PENDING_FILE + " : " + e);
      }
   }

   static void SavePending() {
      java.util.Vector v = OtherLines();
      v.addElement(ProfileKey() + ";" + m_sPendCode + ";" + m_nPendKey + ";" + m_nPendHost + ";" + m_nPendGuest);
      WriteLines(v);
   }

   static void ClearPending() {
      if (m_bPending) {
         m_bPending = false;
         WriteLines(OtherLines());
      }
   }

   // MenuOnlineLeague.Show : un score pas encore confirmé (jeu fermé ou coupure avant la réponse) repart
   static void LoadPending() {
      m_bPending = false;
      java.io.File f = new java.io.File(_PENDING_FILE);
      if (!f.exists()) {
         return;
      }

      try {
         java.io.BufferedReader in = new java.io.BufferedReader(new java.io.FileReader(f));
         String sMe = ProfileKey() + ";";

         for (String s = in.readLine(); s != null; s = in.readLine()) {
            if (s.startsWith(sMe)) {
               java.util.StringTokenizer tok = new java.util.StringTokenizer(s.substring(sMe.length()), ";");
               m_sPendCode = tok.nextToken();
               m_nPendKey = Integer.parseInt(tok.nextToken().trim());
               m_nPendHost = Integer.parseInt(tok.nextToken().trim());
               m_nPendGuest = Integer.parseInt(tok.nextToken().trim());
               m_bPending = true;
            }
         }

         in.close();
      } catch (Exception e) {
         Log("ERREUR lecture de " + _PENDING_FILE + " : " + e);
      }

      if (m_bPending) {
         Log("score en attente : ligue " + m_sPendCode + ", match " + m_nPendKey + ", " + m_nPendHost + "-" + m_nPendGuest);
      }
   }

   // ------------------------------------------------------------------ tests sans souris (NetAuto 5 et 6)

   static long m_lBotNext = 0L;
   static long m_lBotMenuSince = 0L;

   // ModLobby.AutoMatchFrame : une passe toutes les 2 s (une touche ou un coup franc de mon équipe attend sinon la
   // passe), « continuer » à la mi-temps. La fin du match est quittée par ModLobby.AutoMatchFrame.
   static void AutoMatchFrame(long lNow) {
      SoccerModule cModule = SoccerGame.activeLevelModule;
      AIMatch cMatch = SoccerModule.m_cMatch;
      if (cModule == null || cMatch == null || cModule.m_bMatchEnd) {
         return;
      }

      if (cModule.m_bMenuVisible && cModule.m_cMenuInGameModule != null) {
         if (m_lBotMenuSince == 0L) {
            m_lBotMenuSince = lNow;
         } else if (lNow - m_lBotMenuSince > 3000L) {
            m_lBotMenuSince = 0L;
            Log("test : mi-temps, continuer");
            cModule.m_cMenuInGameModule.m_cMenuYesNo.Hide();
            cModule.HideMenu();
         }

         return;
      }

      m_lBotMenuSince = 0L;
      if (lNow < m_lBotNext) {
         return;
      }

      m_lBotNext = lNow + 2000L;

      for (int i = 0; i < 2; i++) {
         Team cTeam = cMatch.GetTeam(i);
         TeamController cController = cTeam != null && cTeam.m_bUserControled ? cTeam.GetTeamController() : null;
         if (cController != null && cController.m_Actions[0] instanceof InputDigital) {
            int nKey = ((InputDigital)cController.m_Actions[0]).m_iKeyValue;
            cController.Inject(nKey, true);
            cController.Inject(nKey, false);
         }
      }
   }

   // ------------------------------------------------------------------ commandes du joueur

   private static void Command(int nCmd) {
      m_nCmd = nCmd;
      m_nCmdFirstSeq = 0;
      m_nSeqFloor = ModLobby.m_nSeq + 1;
      m_lNextSend = 0L;
      m_nLastCmd = -1;
      m_nLastError = _LGE_NONE;
   }

   static void Create() {
      Command(_LG_CREATE);
      Log("creation d'une ligue, delai " + _DELAYS[m_nDelayIndex] + " min, mi-temps " + CoreLogic._HALF_TIMES[m_nHalfIndex] + " min");
   }

   static void Join(String sCode) {
      m_sCmdCode = sCode;
      Command(_LG_JOIN);
      Log("entree dans la ligue " + sCode);
   }

   static void SetTeam() {
      Command(_LG_TEAM);
   }

   static void Start() {
      Command(_LG_START);
      Log("lancement");
   }

   static void Leave() {
      Command(_LG_LEAVE);
      Log("quitte la ligue " + m_sCode);
   }

   static void Skip() {
      Command(_LG_SKIP);
      Log("journee passee");
   }

   // MenuChooseTeamColors.OnOK, choix ouvert depuis l'écran de la ligue
   static void OnTeamChosen(int nAnimal, int nClothes) {
      m_bPickTeam = false;
      ModLobby.SetMyTeam(nAnimal, nClothes);
      if (m_bHave && !IsStarted()) {
         SetTeam(); // avant le départ : le serveur prend la nouvelle équipe
      }
   }

   // ------------------------------------------------------------------ envoi (ModLobby.OnFrame, connecté au serveur)

   static void Frame(long lNow) throws java.io.IOException {
      if (!m_bScreen && m_nCmd < 0 || lNow < m_lNextSend) {
         return;
      }

      // un score attend : il repart dès qu'on sait qu'il est pour la ligue en cours
      if (m_nCmd < 0 && m_bPending && m_bHave && m_sPendCode.equals(m_sCode)) {
         SendPending();
      }

      boolean bWait = m_nCmd < 0 && m_bWaitFriend && !ModLobby.InRoom();
      int nCmd = m_nCmd >= 0 ? m_nCmd : (bWait ? _LG_PLAY : _LG_SYNC);
      ByteBuffer b = ModLobby.Begin(_PKT_LEAGUE);
      if (m_nCmd >= 0 && m_nCmdFirstSeq == 0) {
         m_nCmdFirstSeq = ModLobby.m_nSeq;
      }

      b.put((byte)nCmd);
      b.putInt(m_nVersion);
      switch (nCmd) {
         case _LG_CREATE:
            PutSettings(b);
            break;
         case _LG_JOIN:
            ModNet.PutString(b, m_sCmdCode);
            b.put((byte)ModLobby.m_nMyAnimal);
            b.put((byte)ModLobby.m_nMyClothes);
            break;
         case _LG_TEAM:
            b.put((byte)ModLobby.m_nMyAnimal);
            b.put((byte)ModLobby.m_nMyClothes);
            break;
         case _LG_RESULT:
            b.putInt(m_nCmdKey);
            b.put((byte)m_nCmdGoalsHost);
            b.put((byte)m_nCmdGoalsGuest);
            break;
         case _LG_PLAY:
            b.putInt(m_nCmd >= 0 ? m_nCmdKey : m_nWaitKey);
      }

      ModLobby.Send(b);
      m_lNextSend = lNow + (m_nCmd >= 0 ? 300L : (bWait ? _WAIT_PLAY_MS : 1000L));
   }

   // Réglages de la ligue créée : ceux du league.scr de ce jeu, le délai et la durée choisis, mon équipe, puis les
   // notes des 14 équipes pour les matchs simulés (format _LEAGUE_SETTINGS de protocol.py)
   private static void PutSettings(ByteBuffer b) {
      ModLeagueMode.LoadConfig();
      b.put((byte)ModLeagueMode.m_nCfgQualifiers);
      b.put((byte)(ModLeagueMode.m_bCfgReset ? 1 : 0));
      b.put((byte)ModLeagueMode.m_nCfgPointsWin);
      b.put((byte)ModLeagueMode.m_nCfgPointsDraw);
      b.putShort((short)Math.round(ModLeagueMode.m_fCfgForm * 100.0F));
      b.putShort((short)Math.round(ModLeagueMode.m_fCfgGoalsPerMinute * 1000.0F));
      b.putShort((short)Math.round(ModLeagueMode.m_fCfgHomeBonus * 1000.0F));
      b.putShort((short)Math.round(ModLeagueMode.m_fCfgStrengthEffect * 1000.0F));
      b.putInt(_DELAYS[m_nDelayIndex]);
      b.put((byte)CoreLogic._HALF_TIMES[m_nHalfIndex]);
      b.put((byte)ModLobby.m_nMyAnimal);
      b.put((byte)ModLobby.m_nMyClothes);
      int n = EAnimals._ANIMALS_COUNT;
      b.put((byte)n);

      for (int i = 0; i < n; i++) {
         b.putShort((short)Math.round(ModLeagueMode.GetRating(i) * 100.0F));
      }
   }

   // ------------------------------------------------------------------ réception (ModLobby.OnPacket)

   static void ReadInfo(ByteBuffer b) {
      int nSeq = b.getInt();
      int nError = b.get() & 0xFF;
      boolean bHere = b.get() != 0;
      if (nSeq < m_nSeqFloor) {
         return; // réponse à un paquet d'avant la dernière commande
      }

      if (m_nCmd >= 0 && m_nCmdFirstSeq != 0 && nSeq >= m_nCmdFirstSeq) {
         if (nError != _LGE_NONE) {
            Log("commande " + m_nCmd + " : erreur " + nError);
         }

         m_nLastCmd = m_nCmd;
         m_nLastError = nError;
         m_lLastErrorAt = System.currentTimeMillis();
         m_nCmd = -1;
         m_lNextSend = 0L;
         if (m_nLastCmd == _LG_RESULT) {
            OnResultAnswer(nError);
         }
      } else if (m_nCmd < 0 && m_bWaitFriend && nError != _LGE_NONE) {
         // réponse à LG_PLAY pendant l'attente : versions du mod différentes, serveur plein, match plus à jouer
         Log("attente du match " + m_nWaitKey + " : erreur " + nError);
         m_nLastCmd = _LG_PLAY;
         m_nLastError = nError;
         m_lLastErrorAt = System.currentTimeMillis();
         StopWaiting("");
      }

      m_nChanges++;
      m_bAnswered = true;
      if (!bHere) {
         if (m_bHave) {
            Log("plus de ligue");
         }

         if (m_bPending) {
            Log("score en attente oublie : plus de ligue");
            ClearPending();
         }

         Clear();
         return;
      }

      String sCode = ModNet.GetString(b);
      int nFlags = b.get() & 0xFF;
      int nVersion = b.getInt();
      int nQualifiers = b.get() & 0xFF;
      b.get(); // revanche en grande finale
      b.get(); // points d'une victoire
      b.get(); // d'un nul
      b.getShort(); // forme
      b.getShort(); // buts par minute
      b.getShort(); // bonus à domicile
      b.getShort(); // effet de la note
      int nDelay = b.getInt();
      int nHalf = b.get() & 0xFF;
      int nDeadline = b.getInt();
      int nMyTeam = b.get();
      int nMyKey = b.getInt();
      int nMembers = Math.min(b.get() & 0xFF, _MAX_MEMBERS);
      String[] tName = new String[_MAX_MEMBERS];
      int[] tAnimal = new int[_MAX_MEMBERS];
      int[] tClothes = new int[_MAX_MEMBERS];
      int[] tMember = new int[_MAX_MEMBERS];

      for (int i = 0; i < nMembers; i++) {
         tName[i] = ModNet.GetString(b);
         tAnimal[i] = b.get() & 0xFF;
         tClothes[i] = b.get() & 0xFF;
         tMember[i] = b.get() & 0xFF;
      }

      int nState = b.getShort() & 0xFFFF;
      ModLeague cLeague = null;
      if (nState > 0) {
         int[] t = new int[nState];

         for (int i = 0; i < nState; i++) {
            t[i] = GetZigzag(b);
         }

         cLeague = new ModLeague();
         if (!cLeague.Load(t)) {
            Log("ERREUR etat de la ligue illisible (" + nState + " entiers)");
            cLeague = null;
         }
      }

      // tout est lu : un paquet tronqué (exception plus haut) ne laisse pas une ligue à moitié changée
      if (!m_bHave || !sCode.equals(m_sCode)) {
         Log("ligue " + sCode + ((nFlags & _LGF_OWNER) != 0 ? " (createur)" : "") + ", " + nMembers + " joueurs");
         m_cLeague = null;
         m_nVersion = 0;
      }

      m_bHave = true;
      m_sCode = sCode;
      m_nFlags = nFlags;
      m_nQualifiers = nQualifiers;
      m_nDelay = nDelay;
      m_nHalf = nHalf;
      m_nDeadline = nDeadline;
      m_lDeadlineAt = System.currentTimeMillis();
      m_nMyTeam = nMyTeam;
      m_nMyKey = nMyKey;
      m_nMembers = nMembers;
      m_tName = tName;
      m_tAnimal = tAnimal;
      m_tClothes = tClothes;
      m_tMember = tMember;
      if ((nFlags & _LGF_STARTED) == 0) {
         m_cLeague = null;
         m_nVersion = 0;
      } else if (cLeague != null) {
         if (m_cLeague == null) {
            Log("ligue lancee : equipe " + nMyTeam + ", " + cLeague.m_nRounds + " journees");
         }

         m_cLeague = cLeague;
         m_nVersion = nVersion;
      }

      MarkHumans();
      if (m_bPending && !m_sPendCode.equals(sCode)) {
         Log("score en attente oublie : il etait pour la ligue " + m_sPendCode);
         ClearPending();
      }
   }

   // Réponse du serveur au score d'un match contre l'IA : enregistré, trop tard (délai dépassé, le match a été
   // simulé), ou nul en playoffs à rejouer. Dans tous les cas le score n'est plus à envoyer.
   static void OnResultAnswer(int nError) {
      m_nSent = nError == _LGE_NONE ? _SENT_OK : (nError == _LGE_REPLAY ? _SENT_REPLAY : _SENT_LATE);
      m_lSentAt = System.currentTimeMillis();
      m_nLastError = _LGE_NONE; // l'écran dit ce qu'il en est (m_nSent)
      Log("score du match " + m_nPendKey + " : reponse " + nError);
      ClearPending();
   }

   // Les équipes des joueurs (pour les couleurs des écrans de la ligue)
   static void MarkHumans() {
      if (m_cLeague == null) {
         return;
      }

      boolean[] t = new boolean[m_cLeague.m_nTeams];

      for (int i = 0; i < m_nMembers; i++) {
         int nTeam = m_cLeague.FindTeam(m_tAnimal[i]);
         if (nTeam >= 0 && (m_tMember[i] & _LGM_LEFT) == 0) {
            t[nTeam] = true;
         }
      }

      m_cLeague.m_tHuman = t;
   }

   // Entier « zigzag » de 1 à 5 octets (protocol.put_zigzag)
   static int GetZigzag(ByteBuffer b) {
      int n = 0;
      int nShift = 0;

      while (true) {
         int k = b.get() & 0xFF;
         n |= (k & 0x7F) << nShift;
         nShift += 7;
         if ((k & 0x80) == 0) {
            return n >>> 1 ^ -(n & 1);
         }

         if (nShift > 28) {
            throw new RuntimeException("entier trop long");
         }
      }
   }

   // ------------------------------------------------------------------ textes

   // Issue de la dernière commande, "" si rien à dire (ou si elle date de plus de 10 s)
   static String ErrorText() {
      if (System.currentTimeMillis() - m_lLastErrorAt > 10000L) {
         return "";
      }

      switch (m_nLastError) {
         case _LGE_UNKNOWN:
            return T(m_nLastCmd == _LG_JOIN && m_sCmdCode.length() == 0 ? "LeagueOnErrNoOpen" : "LeagueOnErrUnknown");
         case _LGE_STARTED:
            return T("LeagueOnErrStarted");
         case _LGE_FULL:
            return T("LeagueOnErrFull");
         case _LGE_TEAM_TAKEN:
            return T(m_nLastCmd == _LG_JOIN ? "LeagueOnErrGiven" : "LeagueOnErrTaken");
         case _LGE_NOT_OWNER:
            return T("LeagueOnErrOwner");
         case _LGE_NO_ACCOUNT:
            return T("LeagueOnErrAccount");
         case _LGE_REFUSED:
            return T("LeagueOnErrRefused");
         case _LGE_LIMIT:
            return T(m_nLastCmd == _LG_PLAY ? "LeagueOnErrRooms" : "LeagueOnErrLimit"); // LG_PLAY : plus de salon libre
         case _LGE_VERSION:
            return T("LeagueOnErrVersion");
         case _LGE_REPLAY:
            return T("LeagueOnErrReplay");
         case _LGE_BAD:
            return T("LeagueOnErrBad");
         default:
            return "";
      }
   }

   static String T(String sKey) {
      return Text.Get("&" + sKey + "&");
   }

   // « 24 h », « 30 min », « aucun »
   static String DelayText(int nMinutes) {
      if (nMinutes <= 0) {
         return T("LeagueOnNoDelay");
      }

      return nMinutes % 60 == 0 ? nMinutes / 60 + T("LeagueOnHours") : nMinutes + T("OnlineMinutes");
   }

   // « 5 h 12 », « 12 min », « 40 s »
   static String TimeLeftText(int nSeconds) {
      if (nSeconds >= 3600) {
         int nMin = nSeconds / 60 % 60;
         return nSeconds / 3600 + " h " + (nMin < 10 ? "0" : "") + nMin;
      }

      return nSeconds >= 60 ? nSeconds / 60 + T("OnlineMinutes") : nSeconds + " s";
   }
}
