// [mod] Écran « ligue en ligne » (ModOnlineLeague), ouvert depuis l'écran « en ligne ». Trois états :
// - pas de ligue : « créer » (avec le délai d'une journée et la durée des mi-temps choisis ici) ou « rejoindre » celle
//   dont on tape le code, « équipe » pour choisir la sienne ;
// - ligue pas encore lancée : les joueurs et leurs équipes, « équipe », « quitter », « lancer » (créateur) ;
// - ligue lancée : journée et délai, mon prochain match, « classement » (MenuLeague / MenuLeagueBracket), « passer »
//   la journée (créateur), « quitter » (l'IA reprend mon équipe), « jouer » : contre l'IA, le match en solo ;
//   contre un ami, l'attente de son « jouer », puis le match en ligne dans le salon ouvert par le serveur.
// L'état vient du serveur : l'écran se relit 5 fois par seconde (Tick). Écran généré par
// mods/ui/make_online_league_scr.py, voir notes/online-league.md.
public class MenuOnlineLeague extends SoccerUserInterface {
   static final int _MSG_ROWS = 3;
   static final float _TICK = 0.2F;
   static final int DLG_LEAVE = 61;
   static final int DLG_SKIP = 62;
   static final int _STATE_NONE = 0;
   static final int _STATE_LOBBY = 1;
   static final int _STATE_STARTED = 2;
   static final float[] _GREY = new float[]{0.6F, 0.6F, 0.6F};
   static final float[] _LIGHT = new float[]{0.85F, 0.85F, 0.85F};
   static final float[] _HUMAN = new float[]{0.45F, 0.8F, 1.0F}; // les autres joueurs, aussi dans MenuLeague
   // tests sans souris (NetAuto 5 : crée la ligue et la lance ; 6 : la rejoint), voir mods/online-test.ps1 -League
   static boolean m_bAutoOpened = false;
   static boolean m_bAutoTable = false;
   static long m_lAutoTableAt = 0L;
   static int m_nAutoPlayed = 0;
   static final int _AUTO_MATCHES = 3;
   static long m_lAutoNext = 0L;
   UIStatic m_cStatus = null;
   UIEdit m_cCode = null;
   UIStatic m_cMyTeam = null;
   UIButton m_cAction1 = null;
   UIButton m_cAction2 = null;
   UIButton m_cAction3 = null;
   UIStatic m_cDelay = null;
   UIStatic m_cHalf = null;
   UIWindow[] m_tArrows = new UIWindow[4]; // < et > du délai et de la durée : cachés une fois la ligue créée
   UIStatic[] m_tName = new UIStatic[ModOnlineLeague._MAX_MEMBERS];
   UIStatic[] m_tTeam = new UIStatic[ModOnlineLeague._MAX_MEMBERS];
   UIStatic m_cRound = null;
   UIStatic[] m_tMsg = new UIStatic[_MSG_ROWS];
   UIButton m_cOk = null;
   UIButton m_cBack = null;
   java.util.Hashtable m_tShown = new java.util.Hashtable(); // élément -> dernier texte (et couleur) affiché
   int m_nWasState = -1;
   boolean m_bBadCode = false;

   MenuOnlineLeague() {
      super.m_sFiles = new String[]{"data\\menu\\scr\\OnlineLeague640.scr", "data\\menu\\scr\\OnlineLeague.scr", "data\\menu\\scr\\OnlineLeague1024.scr"};
   }

   public void OnInit() {
      super.OnInit();
      this.m_cStatus = (UIStatic)this.GetElem("ID_STATUS");
      this.m_cCode = (UIEdit)this.GetElem("ID_CODE");
      this.m_cCode.m_sForbiddenChars = "\\/~!@#$%^&*_+|{}[]<>?,'\"` .;:-=()0123456789";
      this.m_cMyTeam = (UIStatic)this.GetElem("ID_MYTEAM");
      this.m_cAction1 = (UIButton)this.GetElem("ID_ACTION1");
      this.m_cAction2 = (UIButton)this.GetElem("ID_ACTION2");
      this.m_cAction3 = (UIButton)this.GetElem("ID_ACTION3");
      this.m_cDelay = (UIStatic)this.GetElem("ID_DELAY_VALUE");
      this.m_cHalf = (UIStatic)this.GetElem("ID_HALF_VALUE");
      this.m_tArrows[0] = this.GetElem("ID_DELAY_PREV");
      this.m_tArrows[1] = this.GetElem("ID_DELAY_NEXT");
      this.m_tArrows[2] = this.GetElem("ID_HALF_PREV");
      this.m_tArrows[3] = this.GetElem("ID_HALF_NEXT");

      for (int i = 0; i < ModOnlineLeague._MAX_MEMBERS; i++) {
         this.m_tName[i] = (UIStatic)this.GetElem("ID_P" + i + "_NAME");
         this.m_tTeam[i] = (UIStatic)this.GetElem("ID_P" + i + "_TEAM");
      }

      this.m_cRound = (UIStatic)this.GetElem("ID_ROUND");

      for (int i = 0; i < _MSG_ROWS; i++) {
         this.m_tMsg[i] = (UIStatic)this.GetElem("ID_MSG" + i);
      }

      this.m_cOk = (UIButton)this.GetElem("ID_OK");
      this.m_cBack = (UIButton)this.GetElem("ID_CANCEL");
      this.AddToSlideIn(this.m_cOk);
      this.AddToSlideIn(this.m_cBack);
      this.m_tShown.clear();
      this.m_nWasState = -1;
   }

   public void Show() {
      ModOnlineLeague.EndMatchMode(); // retour d'un match contre l'IA (ou de son avant-match)
      ModLobby.OnScreenShown(); // mon équipe (valable pour ce profil) et l'équipe de gauche pour le choix d'équipe
      if (!ModOnlineLeague.m_bPending) {
         ModOnlineLeague.LoadPending(); // un score pas confirmé avant la fermeture du jeu repart
      }

      ModOnlineLeague.m_bScreen = true;
      ModOnlineLeague.m_bView = false;
      ModOnlineLeague.m_bPickTeam = false;
      ModOnlineLeague.m_lNextSend = 0L;
      this.m_bBadCode = false;
      this.m_tShown.clear();
      this.m_nWasState = -1;
      this.Refresh();
      super.m_sTitle = "";
      this.CenterCursor();
      super.Show();
      this.DisableCallMethod("Tick");
      this.CallMethodEvery("Tick", _TICK);
   }

   public void Hide() {
      ModOnlineLeague.StopWaiting(""); // l'attente d'un ami ne vit que sur cet écran
      this.DisableCallMethod("Tick");
      ModOnlineLeague.m_bScreen = false;
      super.Hide();
   }

   void Tick() {
      this.AutoFrame();
      if (ModOnlineLeague.FriendFrame()) {
         return; // le match contre l'ami se charge
      }

      this.Refresh();
   }

   // ---------------------------------------------------------------- affichage

   // SetText (et la couleur) seulement quand ils changent : l'écran est relu 5 fois par seconde
   void Set(UIStatic cWnd, String sText, float[] c) {
      if (cWnd == null) {
         return;
      }

      String sKey = (c != null ? c[0] + "," + c[1] + "," + c[2] + "|" : "") + sText;
      if (!sKey.equals(this.m_tShown.get(cWnd))) {
         this.m_tShown.put(cWnd, sKey);
         cWnd.SetText(sText);
         if (c != null) {
            cWnd.SetTextColor(c[0], c[1], c[2]);
         }
      }
   }

   void Set(UIStatic cWnd, String sText) {
      this.Set(cWnd, sText, null);
   }

   // Un bouton montré (avec ce texte, actif ou non) ou caché
   void SetButton(UIButton cButton, String sText, boolean bShown, boolean bEnabled) {
      if (cButton == null) {
         return;
      }

      String sKey = (bShown ? "1" : "0") + (bEnabled ? "1" : "0") + sText;
      if (!sKey.equals(this.m_tShown.get(cButton))) {
         this.m_tShown.put(cButton, sKey);
         cButton.SetText(sText);
         cButton.SetDisabled(!bEnabled);
         if (bShown) {
            cButton.Show();
         } else {
            cButton.Hide();
         }
      }
   }

   static String T(String sKey) {
      return Text.Get("&" + sKey + "&");
   }

   static String TeamName(int nAnimal) {
      return nAnimal >= 0 && nAnimal < EAnimals._ANIMALS_COUNT ? ETeams.GetTeamData(nAnimal).m_sNameAbr : "";
   }

   static String TeamMin(int nAnimal) {
      return nAnimal >= 0 && nAnimal < EAnimals._ANIMALS_COUNT ? ETeams.GetTeamData(nAnimal).m_sNameMin : "";
   }

   // Texte raccourci (« Alexandr. ») s'il ne tient pas dans l'élément
   static String Fit(UIStatic s, String sText) {
      if (s == null) {
         return sText;
      }

      float fMax = s.GetSize().fX - 4.0F;

      while (sText.length() > 3 && s.GetTextExtent(sText).fX >= fMax) {
         sText = sText.substring(0, sText.length() - (sText.endsWith(".") ? 2 : 1)) + ".";
      }

      return sText;
   }

   // sLong s'il tient dans l'élément (640 x 480 : polices plus grosses par rapport à l'écran), sinon sShort
   static String Fits(UIStatic s, String sLong, String sShort) {
      return s == null || s.GetTextExtent(sLong).fX < s.GetSize().fX - 4.0F ? sLong : sShort;
   }

   int GetState() {
      if (!ModOnlineLeague.m_bHave) {
         return _STATE_NONE;
      }

      return ModOnlineLeague.IsStarted() ? _STATE_STARTED : _STATE_LOBBY;
   }

   void Refresh() {
      boolean bOnline = ModLobby.m_nNet == ModLobby._NET_ONLINE;
      boolean bBusy = ModOnlineLeague.IsBusy();
      int nState = this.GetState();
      ModLeague l = ModOnlineLeague.m_cLeague;
      // le champ du code : libre sans ligue, il montre le code de la ligue ensuite
      if (nState != this.m_nWasState) {
         if (this.m_nWasState >= 0) {
            this.m_tShown.clear(); // tout l'écran change de sens
         }

         this.m_nWasState = nState;
         this.m_cCode.SetCheck(false);
         this.m_cCode.SetDisabled(false);
         this.m_cCode.SetText(nState == _STATE_NONE ? "" : ModOnlineLeague.m_sCode);
         this.m_cCode.SetDisabled(nState != _STATE_NONE);
      }

      // état : serveur, puis où en est la ligue
      if (!bOnline) {
         this.Set(this.m_cStatus, MenuOnline.NetText(), ModLobby.m_nNet == ModLobby._NET_CONNECTING ? _LIGHT : MenuLeague._RED);
      } else if (nState == _STATE_NONE) {
         this.Set(this.m_cStatus, T("LeagueOnNone"), _LIGHT);
      } else if (nState == _STATE_LOBBY) {
         this.Set(this.m_cStatus, T("LeagueOnCode") + ModOnlineLeague.m_sCode, MenuLeague._GOLD);
      } else if (ModOnlineLeague.IsOver()) {
         this.Set(this.m_cStatus, T("LeagueOnOver"), MenuLeague._GOLD);
      } else if (l.m_nPhase == ModLeague._PHASE_LEAGUE) {
         this.Set(this.m_cStatus, T("LeagueOnRound") + (l.m_nRound + 1) + " / " + l.m_nRounds, MenuLeague._GOLD);
      } else {
         this.Set(this.m_cStatus, T("LeagueOnPlayoffs"), MenuLeague._GOLD);
      }

      // mon équipe, et ma place une fois la ligue lancée
      int nMine = ModLobby.m_nMyAnimal;
      if (nState != _STATE_NONE) {
         for (int i = 0; i < ModOnlineLeague.m_nMembers; i++) {
            if ((ModOnlineLeague.m_tMember[i] & ModOnlineLeague._LGM_ME) != 0) {
               nMine = ModOnlineLeague.m_tAnimal[i];
            }
         }
      }

      String sRank = nState == _STATE_STARTED && ModOnlineLeague.m_nMyTeam >= 0 ? " (" + (l.GetRank(ModOnlineLeague.m_nMyTeam) + 1) + ")" : "";
      this.Set(this.m_cMyTeam, Fits(this.m_cMyTeam, T("LeagueOnMyTeam") + TeamName(nMine) + sRank, T("LeagueOnMyTeam") + TeamMin(nMine) + sRank), MenuLeague._GOLD);
      this.RefreshMembers(nState);
      this.RefreshButtons(nState, bOnline, bBusy);
      this.RefreshMessages(nState, bOnline, bBusy, l);
   }

   void RefreshMembers(int nState) {
      for (int i = 0; i < ModOnlineLeague._MAX_MEMBERS; i++) {
         if (nState == _STATE_NONE || i >= ModOnlineLeague.m_nMembers) {
            this.Set(this.m_tName[i], "", MenuLeague._WHITE);
            this.Set(this.m_tTeam[i], "", MenuLeague._WHITE);
            continue;
         }

         int nFlags = ModOnlineLeague.m_tMember[i];
         float[] c = MenuLeague._WHITE;
         if ((nFlags & ModOnlineLeague._LGM_ME) != 0) {
            c = MenuLeague._GOLD;
         } else if ((nFlags & ModOnlineLeague._LGM_LEFT) != 0) {
            c = _GREY;
         } else if ((nFlags & ModOnlineLeague._LGM_ONLINE) == 0) {
            c = _LIGHT;
         } else {
            c = _HUMAN;
         }

         String sName = ModOnlineLeague.m_tName[i] + ((nFlags & ModOnlineLeague._LGM_OWNER) != 0 ? " *" : "");
         String sTeam = (nFlags & ModOnlineLeague._LGM_LEFT) != 0 ? T("LeagueOnLeft").trim() : TeamMin(ModOnlineLeague.m_tAnimal[i]);
         this.Set(this.m_tName[i], Fit(this.m_tName[i], sName), c);
         this.Set(this.m_tTeam[i], sTeam, c);
      }
   }

   void RefreshButtons(int nState, boolean bOnline, boolean bBusy) {
      boolean bCan = bOnline && !bBusy;
      boolean bOwner = ModOnlineLeague.IsOwner();
      if (nState == _STATE_NONE) {
         this.SetButton(this.m_cAction1, T("LeagueOnCreate"), true, bCan);
         this.SetButton(this.m_cAction2, T("LeagueOnJoin"), true, bCan);
         this.SetButton(this.m_cAction3, T("LeagueOnTeam"), true, !bBusy);
         this.SetButton(this.m_cOk, T("LeagueOnStart"), false, false);
         this.Set(this.m_cDelay, ModOnlineLeague.DelayText(ModOnlineLeague._DELAYS[ModOnlineLeague.m_nDelayIndex]));
         this.Set(this.m_cHalf, CoreLogic._HALF_TIMES[ModOnlineLeague.m_nHalfIndex] + T("OnlineMinutes"));
      } else {
         if (nState == _STATE_LOBBY) {
            this.SetButton(this.m_cAction1, T("LeagueOnTeam"), true, bCan);
            this.SetButton(this.m_cAction2, "", false, false);
            this.SetButton(this.m_cOk, T("LeagueOnStart"), bOwner, bCan);
         } else {
            this.SetButton(this.m_cAction1, T("LeagueOnTable"), true, true);
            this.SetButton(this.m_cAction2, T("LeagueOnSkip"), bOwner && !ModOnlineLeague.IsOver(), bCan);
            // « jouer » : mon match de la journée, contre l'IA ou contre un ami ; « annuler » pendant l'attente de l'ami
            boolean bNext = ModOnlineLeague.m_nMyKey >= 0 && !ModOnlineLeague.IsOver();
            if (ModOnlineLeague.m_bWaitFriend) {
               this.SetButton(this.m_cOk, T("LeagueOnCancel"), true, true);
            } else {
               boolean bPlay = ModOnlineLeague.PlayableKey() >= 0 || ModOnlineLeague.CanPlayFriend() && !ModLobby.InRoom();
               this.SetButton(this.m_cOk, T("LeagueOnPlay"), bNext, bCan && bPlay);
            }
         }

         this.SetButton(this.m_cAction3, T("LeagueOnLeave"), true, bCan);
         this.Set(this.m_cDelay, ModOnlineLeague.DelayText(ModOnlineLeague.m_nDelay));
         this.Set(this.m_cHalf, ModOnlineLeague.m_nHalf + T("OnlineMinutes"));
      }

      this.SetButton(this.m_cBack, T("MenuBack"), true, true);
      // les réglages ne se changent qu'avant la création
      Boolean bArrows = new Boolean(nState == _STATE_NONE);

      for (int i = 0; i < this.m_tArrows.length; i++) {
         UIWindow w = this.m_tArrows[i];
         if (w != null && !bArrows.equals(this.m_tShown.get(w))) {
            this.m_tShown.put(w, bArrows);
            if (bArrows.booleanValue()) {
               w.Show();
            } else {
               w.Hide();
            }
         }
      }
   }

   void RefreshMessages(int nState, boolean bOnline, boolean bBusy, ModLeague l) {
      String sError = this.m_bBadCode ? T("LeagueOnErrCode") : ModOnlineLeague.ErrorText();
      String sLast = "";
      float[] cLast = MenuLeague._RED;
      boolean bSentRecent = System.currentTimeMillis() - ModOnlineLeague.m_lSentAt < 10000L;
      int nSent = ModOnlineLeague.m_nSent;
      if (!bOnline) {
         sLast = MenuOnline.NetText();
      } else if (ModOnlineLeague.m_bPending) {
         sLast = T("LeagueOnSendScore"); // le score du dernier match part au serveur
         cLast = _LIGHT;
      } else if (bBusy) {
         int nCmd = ModOnlineLeague.m_nCmd;
         sLast = T(nCmd == ModOnlineLeague._LG_CREATE ? "LeagueOnBusyCreate" : (nCmd == ModOnlineLeague._LG_JOIN ? "LeagueOnBusyJoin" : "LeagueOnBusy"));
         cLast = _LIGHT;
      } else if (bSentRecent && nSent != ModOnlineLeague._SENT_NONE) {
         sLast = T(nSent == ModOnlineLeague._SENT_OK ? "LeagueOnScoreOk" : (nSent == ModOnlineLeague._SENT_REPLAY ? "LeagueOnErrReplay" : "LeagueOnScoreLate"));
         cLast = nSent == ModOnlineLeague._SENT_OK ? MenuLeague._GREEN : MenuLeague._RED;
      } else {
         sLast = sError;
      }

      if (nState == _STATE_NONE) {
         this.Set(this.m_cRound, T("LeagueOnHelp3"), _LIGHT);
         this.Set(this.m_tMsg[0], T("LeagueOnHelp1"), _LIGHT);
         this.Set(this.m_tMsg[1], T("LeagueOnHelp2"), _LIGHT);
      } else if (nState == _STATE_LOBBY) {
         this.Set(this.m_cRound, T("LeagueOnHintTeam"), _LIGHT);
         this.Set(this.m_tMsg[0], T("LeagueOnCode") + ModOnlineLeague.m_sCode, MenuLeague._GOLD);
         this.Set(this.m_tMsg[1], T(ModOnlineLeague.IsOwner() ? "LeagueOnHintOwner" : "LeagueOnHintWait"), _LIGHT);
      } else {
         this.RefreshRound(l);
         this.RefreshMatch();
      }

      this.Set(this.m_tMsg[2], sLast, cLast);
   }

   // ligue lancée, deuxième ligne : l'attente de l'ami, l'issue de mon dernier match, ou ce que fera « jouer »
   void RefreshMatch() {
      int nOther = ModOnlineLeague.Opponent();
      String sOther = nOther >= 0 ? ModOnlineLeague.m_tName[nOther] : "";
      int nOtherFlags = nOther >= 0 ? ModOnlineLeague.m_tMember[nOther] : 0;
      int nOutcome = ModOnlineLeague.m_nLastMatchOutcome;
      boolean bStopRecent = System.currentTimeMillis() - ModOnlineLeague.m_lFriendStopAt < 10000L;
      String sText = "";
      float[] c = _LIGHT;
      if (ModOnlineLeague.m_bWaitFriend) {
         // attente de l'ami, puis salon ouvert par le serveur
         sText = ModLobby.m_bLeagueRoom ? T("LeagueOnRoomOpen") : T("LeagueOnWaitFriend") + sOther + "...";
         c = MenuLeague._GOLD;
      } else if (bStopRecent && ModOnlineLeague.m_sFriendStop.length() > 0) {
         sText = T(ModOnlineLeague.m_sFriendStop);
         c = MenuLeague._RED;
      } else if (ModOnlineLeague.CanPlayFriend() && (nOtherFlags & ModOnlineLeague._LGM_WAITING) != 0) {
         sText = sOther + T("LeagueOnFriendWaits"); // l'ami a cliqué « jouer » : à moi
         c = MenuLeague._GREEN;
      } else if (ModOnlineLeague.m_bLastFriend) {
         // dernier match contre un ami : son issue et l'Elo en ligne, donnés par le serveur
         sText = ModOnlineLeague.FriendResultText();
         int nResult = ModLobby.MatchResult();
         c = nResult == ModLobby._RESULT_RATED && ModLobby.m_nResDelta > 0 ? MenuLeague._GREEN
            : (nResult == ModLobby._RESULT_RATED && ModLobby.m_nResDelta < 0 ? MenuLeague._RED : _LIGHT);
      } else if (ModOnlineLeague.m_sLastMatch.length() > 0) {
         // dernier match joué contre l'IA
         sText = ModOnlineLeague.m_sLastMatch;
         c = nOutcome > 0 ? MenuLeague._GREEN : (nOutcome < 0 ? MenuLeague._RED : _LIGHT);
      } else if (ModOnlineLeague.CanPlayFriend()) {
         sText = (nOtherFlags & ModOnlineLeague._LGM_ONLINE) == 0 ? sOther + T("LeagueOnFriendOff") : T("LeagueOnVsFriend");
      }

      this.Set(this.m_tMsg[1], Fit(this.m_tMsg[1], sText), c);
   }

   // ligue lancée : délai de la journée, puis mon prochain match (ou ce qui m'attend)
   void RefreshRound(ModLeague l) {
      if (ModOnlineLeague.IsOver()) {
         int nChampion = l.m_nChampion;
         int nMember = ModOnlineLeague.MemberOfTeam(nChampion);
         String sWho = nChampion >= 0 ? TeamName(l.m_tTeam[nChampion]) + (nMember >= 0 ? " (" + ModOnlineLeague.m_tName[nMember] + ")" : T("LeagueOnAI")) : "-";
         this.Set(this.m_cRound, T("LeagueOnChampion") + sWho, MenuLeague._GOLD);
         this.Set(this.m_tMsg[0], nChampion == ModOnlineLeague.m_nMyTeam ? T("LeagueChampionPlayer") : "", MenuLeague._GOLD);
         return;
      }

      int nLeft = ModOnlineLeague.DeadlineLeft();
      if (nLeft < 0) {
         this.Set(this.m_cRound, T("LeagueOnNoDeadline"), _LIGHT);
      } else if (nLeft > 0) {
         this.Set(this.m_cRound, T("LeagueOnDeadline") + ModOnlineLeague.TimeLeftText(nLeft), _LIGHT);
      } else {
         this.Set(this.m_cRound, T("LeagueOnDeadlineNow"), _LIGHT);
      }

      int nKey = ModOnlineLeague.m_nMyKey;
      if (nKey >= 0) {
         int nHost;
         int nGuest;
         if (nKey < ModOnlineLeague._KEY_PLAYOFFS) {
            nHost = l.m_tFHost[nKey];
            nGuest = l.m_tFGuest[nKey];
         } else {
            nHost = l.GetHost(nKey - ModOnlineLeague._KEY_PLAYOFFS);
            nGuest = l.GetGuest(nKey - ModOnlineLeague._KEY_PLAYOFFS);
         }

         int nOther = nHost == ModOnlineLeague.m_nMyTeam ? nGuest : nHost;
         int nMember = ModOnlineLeague.MemberOfTeam(nOther);
         String sWho = nMember >= 0 ? " (" + ModOnlineLeague.m_tName[nMember] + ")" : T("LeagueOnAI");
         int h = l.m_tTeam[nHost];
         int g = l.m_tTeam[nGuest];
         String sLong = T("LeagueOnNext") + TeamName(h) + " - " + TeamName(g) + sWho;
         this.Set(this.m_tMsg[0], Fits(this.m_tMsg[0], sLong, T("LeagueOnNext") + TeamMin(h) + " - " + TeamMin(g) + sWho), MenuLeague._WHITE);
      } else if (l.m_nPhase == ModLeague._PHASE_PLAYOFFS && !this.StillIn(l)) {
         this.Set(this.m_tMsg[0], T("LeagueOnOut"), MenuLeague._RED);
      } else {
         this.Set(this.m_tMsg[0], T("LeagueOnDone"), MenuLeague._GREEN);
      }
   }

   // en playoffs, mon équipe a encore un match à venir dans le tableau
   boolean StillIn(ModLeague l) {
      int nMe = ModOnlineLeague.m_nMyTeam;
      if (!l.IsPlayerQualified()) {
         return false;
      }

      int nLosses = 0;

      for (int m = 0; m < l.m_nMatches; m++) {
         if (l.m_tBActive[m] && l.m_tBPlayed[m] && l.GetLoser(m) == nMe) {
            nLosses++;
         }
      }

      return nLosses < (l.m_tSeed.length == 2 ? 1 : 2);
   }

   // ---------------------------------------------------------------- actions

   void PickTeam() {
      ModOnlineLeague.m_bPickTeam = true;
      CoreLogic.m_nNextLevel = -1;
      MenuBackground.SetPlayerType(0);
      ((SoccerMainMenuModule)this.GetModule()).GetUI(2).Reset();
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(2);
   }

   void OnJoin() {
      // champ vide : le serveur choisit la dernière ligue ouverte (pas de code à taper)
      String sCode = this.m_cCode.m_sEditText.trim().length() == 0 ? "" : ModLobby.CleanCode(this.m_cCode.m_sEditText);
      this.m_bBadCode = sCode == null;
      if (sCode != null) {
         ModOnlineLeague.Join(sCode);
      }

      this.Refresh();
   }

   void ShowTable() {
      ModOnlineLeague.m_bView = true;
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_LEAGUE); // il passe au tableau en playoffs
   }

   void OnAction1() {
      this.m_bBadCode = false;
      int nState = this.GetState();
      if (nState == _STATE_NONE) {
         ModOnlineLeague.Create();
      } else if (nState == _STATE_LOBBY) {
         this.PickTeam();
      } else {
         this.ShowTable();
      }
   }

   void OnAction2() {
      int nState = this.GetState();
      if (nState == _STATE_NONE) {
         this.OnJoin();
      } else if (nState == _STATE_STARTED && ModOnlineLeague.IsOwner()) {
         this.YesNoDialog(DLG_SKIP, T("LeagueOnSkipTitle"), T("LeagueOnSkipAsk"), null, null);
      }
   }

   void OnAction3() {
      this.m_bBadCode = false;
      if (this.GetState() == _STATE_NONE) {
         this.PickTeam();
      } else {
         boolean bCloses = this.GetState() == _STATE_LOBBY && ModOnlineLeague.IsOwner();
         this.YesNoDialog(DLG_LEAVE, T("LeagueOnLeaveTitle"), T(bCloses ? "LeagueOnCloseAsk" : "LeagueOnLeaveAsk"), null, null);
      }
   }

   void OnOK() {
      int nState = this.GetState();
      if (nState == _STATE_LOBBY && ModOnlineLeague.IsOwner()) {
         ModOnlineLeague.Start();
         this.Refresh();
      } else if (nState == _STATE_STARTED && ModOnlineLeague.m_bWaitFriend) {
         ModOnlineLeague.StopWaiting(""); // « annuler »
         this.Refresh();
      } else if (nState == _STATE_STARTED && !ModOnlineLeague.IsBusy() && ModOnlineLeague.CanPlayFriend()) {
         ModOnlineLeague.StartWaiting(); // contre un ami : le match part quand il a cliqué aussi
         this.Refresh();
      } else if (nState == _STATE_STARTED && !ModOnlineLeague.IsBusy() && ModOnlineLeague.PrepareMatch()) {
         ModOnlineLeague.m_sLastMatch = "";
         ModOnlineLeague.m_bLastFriend = false;
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(9); // avant-match, comme en ligue solo
      }
   }

   void OnBack() {
      ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_ONLINE);
   }

   void OnYesFromDlg(int nID) {
      super.OnYesFromDlg(nID);
      if (nID == DLG_LEAVE) {
         ModOnlineLeague.Leave();
      } else if (nID == DLG_SKIP) {
         ModOnlineLeague.Skip();
      }

      this.Refresh();
   }

   void OnUICommand(UIWindow cSender, int nCommand) {
      super.OnUICommand(cSender, nCommand);
      if (cSender.m_sId == null) {
         return;
      }

      String sId = cSender.m_sId;
      // Entrée dans le champ du code = « rejoindre »
      if (nCommand == 3 && sId.equals("ID_CODE")) {
         if (this.GetState() == _STATE_NONE && this.m_cCode.m_sEditText.length() > 0) {
            this.OnJoin();
         }

         return;
      }

      if (nCommand != 0) {
         return;
      }

      int nDelta = sId.endsWith("_NEXT") ? 1 : (sId.endsWith("_PREV") ? -1 : 0);
      if (sId.equals("ID_OK")) {
         this.OnOK();
      } else if (sId.equals("ID_CANCEL")) {
         this.OnBack();
      } else if (sId.equals("ID_ACTION1")) {
         this.OnAction1();
      } else if (sId.equals("ID_ACTION2")) {
         this.OnAction2();
      } else if (sId.equals("ID_ACTION3")) {
         this.OnAction3();
      } else if (nDelta != 0 && this.GetState() == _STATE_NONE) {
         // réglages de la ligue à créer
         if (sId.startsWith("ID_DELAY_")) {
            int n = ModOnlineLeague._DELAYS.length;
            ModOnlineLeague.m_nDelayIndex = (ModOnlineLeague.m_nDelayIndex + nDelta + n) % n;
         } else if (sId.startsWith("ID_HALF_")) {
            int n = CoreLogic._HALF_TIMES.length;
            ModOnlineLeague.m_nHalfIndex = (ModOnlineLeague.m_nHalfIndex + nDelta + n) % n;
         }

         this.Refresh();
      }
   }

   // ---------------------------------------------------------------- tests sans souris (NetAuto 5 et 6)

   void AutoFrame() {
      int nAuto = ModLobby.m_nAuto;
      long lNow = System.currentTimeMillis();
      // (un match lancé met quelques secondes à charger : l'écran reçoit encore des Tick pendant ce temps)
      if (nAuto != 5 && nAuto != 6 || ModLobby.m_nNet != ModLobby._NET_ONLINE || ModOnlineLeague.IsBusy() || lNow < m_lAutoNext
         || ModOnlineLeague.m_bMatch || ModOnlineLeague.m_bFriendMatch || ModNet.IsOnline()) {
         return;
      }

      m_lAutoNext = lNow + 1000L;
      if (!ModOnlineLeague.m_bHave) {
         if (nAuto == 5) {
            ModOnlineLeague.Create();
         } else {
            ModOnlineLeague.Join(ModLobby.m_sAutoCode); // redit tant que la ligue n'existe pas encore
         }
      } else if (!ModOnlineLeague.IsStarted()) {
         // NetMatches(n) : le créateur lance la ligue quand n joueurs y sont
         if (nAuto == 5 && ModOnlineLeague.m_nMembers >= ModLobby.m_nAutoMatches) {
            ModOnlineLeague.Start();
         }
      } else if (m_nAutoPlayed < _AUTO_MATCHES && !ModOnlineLeague.IsOver()) {
         // trois matchs joués tout seuls : contre l'IA (ModOnlineLeague.AutoMatchFrame, sans l'avant-match), ou contre
         // l'autre jeu (« jouer » des deux côtés, puis NetBot dans le match en ligne). Entre deux, on attend que
         // l'autre jeu ait joué le sien (la journée se ferme alors).
         if (ModOnlineLeague.CanPlayFriend()) {
            if (!ModOnlineLeague.m_bWaitFriend && !ModLobby.InRoom() && !ModOnlineLeague.m_bPending) {
               m_nAutoPlayed++;
               m_lAutoTableAt = 0L;
               ModOnlineLeague.StartWaiting();
            }
         } else if (ModOnlineLeague.PlayableKey() >= 0 && !ModOnlineLeague.m_bPending && ModOnlineLeague.PrepareMatch()) {
            m_nAutoPlayed++;
            m_lAutoTableAt = 0L;
            ModOnlineLeague.m_sLastMatch = "";
            ModOnlineLeague.m_bLastFriend = false;
            SoccerGame.game.GetCommentator().HideNow();
            SoccerGame.game.RunModule();
         }
      } else if (m_lAutoTableAt == 0L) {
         m_lAutoTableAt = lNow + 8000L; // le temps de voir l'écran de la ligue lancée
      } else if (!m_bAutoTable && lNow >= m_lAutoTableAt) {
         m_bAutoTable = true;
         this.ShowTable();
      }
   }
}
