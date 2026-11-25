// [mod] Écran « en ligne » (ModLobby), voir notes/online.md. Deux états :
// - hors salon : état du serveur, « créer » un salon ou « rejoindre » celui dont on tape le code, « partie rapide »
//   (file d'attente d'une partie classée), « ligue » (MenuOnlineLeague), et le classement de l'Elo en ligne à droite ;
// - dans un salon : les deux joueurs et leur équipe, « équipe », « tactique », « prêt » (invité) ou « jouer » (hôte),
//   stade et durée choisis par l'hôte. En partie classée les deux joueurs se mettent prêts et le match part seul.
// L'état vient du réseau : l'écran se relit 5 fois par seconde (Tick). Écran généré par mods/ui/make_online_scr.py.
public class MenuOnline extends SoccerUserInterface {
   static final int _MSG_ROWS = 3;
   static final int _TOP_ROWS = ModLobby._TOP_ROWS;
   static final float _TICK = 0.2F;
   UIStatic m_cFrameMatch = null;
   UIStatic[][] m_tTop = new UIStatic[_TOP_ROWS][2]; // classement : [ligne][0 = rang et nom, 1 = Elo et bilan]
   UIStatic m_cTopMe = null;
   UIStatic m_cStatus = null;
   UIEdit m_cCode = null;
   UIButton m_cAction1 = null;
   UIButton m_cAction2 = null;
   UIButton m_cAction3 = null; // « ligue » hors salon, caché dans un salon
   boolean m_bAction3Shown = true;
   UIStatic m_cStadium = null;
   UIStatic m_cTime = null;
   UIStatic m_cVs = null;
   UIWindow m_cLogo1 = null;
   UIWindow m_cLogo2 = null;
   UIStatic m_cName1 = null;
   UIStatic m_cName2 = null;
   UIStatic m_cPlayer1 = null;
   UIStatic m_cPlayer2 = null;
   UIStatic m_cReady1 = null;
   UIStatic m_cReady2 = null;
   UIStatic[] m_tMsg = new UIStatic[_MSG_ROWS];
   UIButton m_cOk = null;
   UIButton m_cBack = null;
   java.util.Hashtable m_tShown = new java.util.Hashtable(); // élément -> dernier texte affiché
   int[] m_tLogoShown = new int[]{-2, -2};
   boolean m_bWasInRoom = false;
   boolean m_bBadCode = false; // le code tapé n'a pas 4 lettres

   MenuOnline() {
      super.m_sFiles = new String[]{"data\\menu\\scr\\online640.scr", "data\\menu\\scr\\online.scr", "data\\menu\\scr\\online1024.scr"};
   }

   public void OnInit() {
      super.OnInit();
      this.m_cStatus = (UIStatic)this.GetElem("ID_STATUS");
      this.m_cCode = (UIEdit)this.GetElem("ID_CODE");
      this.m_cCode.m_sForbiddenChars = "\\/~!@#$%^&*_+|{}[]<>?,'\"` .;:-=()0123456789";
      this.m_cAction1 = (UIButton)this.GetElem("ID_ACTION1");
      this.m_cAction2 = (UIButton)this.GetElem("ID_ACTION2");
      this.m_cAction3 = (UIButton)this.GetElem("ID_ACTION3");
      this.m_bAction3Shown = true;
      this.m_cStadium = (UIStatic)this.GetElem("ID_STADIUM_VALUE");
      this.m_cTime = (UIStatic)this.GetElem("ID_TIME_VALUE");
      this.m_cVs = (UIStatic)this.GetElem("ID_VS");
      this.m_cLogo1 = this.GetElem("ID_LOGO_1");
      this.m_cLogo2 = this.GetElem("ID_LOGO_2");
      this.m_cName1 = (UIStatic)this.GetElem("ID_NAME_1");
      this.m_cName2 = (UIStatic)this.GetElem("ID_NAME_2");
      this.m_cPlayer1 = (UIStatic)this.GetElem("ID_PLAYER_1");
      this.m_cPlayer2 = (UIStatic)this.GetElem("ID_PLAYER_2");
      this.m_cReady1 = (UIStatic)this.GetElem("ID_READY_1");
      this.m_cReady2 = (UIStatic)this.GetElem("ID_READY_2");

      for (int i = 0; i < _MSG_ROWS; i++) {
         this.m_tMsg[i] = (UIStatic)this.GetElem("ID_MSG" + i);
      }

      this.m_cFrameMatch = (UIStatic)this.GetElem("ID_FRAME_MATCH");

      for (int i = 0; i < _TOP_ROWS; i++) {
         this.m_tTop[i][0] = (UIStatic)this.GetElem("ID_TOP" + i + "_NAME");
         this.m_tTop[i][1] = (UIStatic)this.GetElem("ID_TOP" + i + "_ELO");
      }

      this.m_cTopMe = (UIStatic)this.GetElem("ID_TOP_ME");
      this.m_cOk = (UIButton)this.GetElem("ID_OK");
      this.m_cBack = (UIButton)this.GetElem("ID_CANCEL");
      this.AddToSlideIn(this.m_cOk);
      this.AddToSlideIn(this.m_cBack);
      // l'écran vient d'être (re)chargé : rien n'est encore affiché
      this.m_tShown.clear();
      this.m_tLogoShown[0] = -2;
      this.m_tLogoShown[1] = -2;
      this.m_bWasInRoom = false;
   }

   public void Show() {
      ModLobby.OnScreenShown();
      this.m_bBadCode = false;
      this.Refresh();
      super.m_sTitle = "";
      this.CenterCursor();
      super.Show();
      this.DisableCallMethod("Tick");
      this.CallMethodEvery("Tick", _TICK);
   }

   public void Hide() {
      this.DisableCallMethod("Tick");
      super.Hide();
   }

   // L'état du salon change sans que le joueur ne clique : on le relit régulièrement
   void Tick() {
      // tests de la ligue en ligne sans souris (NetAuto 5 et 6) : droit à l'écran de la ligue
      if ((ModLobby.m_nAuto == 5 || ModLobby.m_nAuto == 6) && !MenuOnlineLeague.m_bAutoOpened && ModLobby.m_nNet == ModLobby._NET_ONLINE) {
         long lNow = System.currentTimeMillis();
         if (MenuOnlineLeague.m_lAutoNext == 0L) {
            MenuOnlineLeague.m_lAutoNext = lNow + 4000L; // le temps de voir cet écran
         }

         if (lNow < MenuOnlineLeague.m_lAutoNext) {
            this.Refresh();
            return;
         }

         MenuOnlineLeague.m_bAutoOpened = true;
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_ONLINE_LEAGUE);
         return;
      }

      // salon d'un match de la ligue en ligne : il ne se joue que depuis l'écran de la ligue, avec les équipes de la ligue
      if (ModLobby.m_bLeagueRoom && !ModNet.IsOnline()) {
         ModLobby.StopLeague();
      }

      ModLobby.AutoMenuFrame();
      if (ModLobby.GuestCheckStart()) {
         return; // le match se charge
      }

      if (ModLobby.m_bRanked && ModLobby.CanStart()) {
         ModLobby.HostStart(); // partie classée : les deux joueurs sont prêts
         return;
      }

      this.Refresh();
   }

   // ---------------------------------------------------------------- affichage

   // SetText seulement quand le texte change : l'écran est relu 5 fois par seconde
   void Set(UIStatic cWnd, String sText) {
      if (cWnd != null && !sText.equals(this.m_tShown.get(cWnd))) {
         this.m_tShown.put(cWnd, sText);
         cWnd.SetText(sText);
      }
   }

   void SetMsg(int i, String sText, float r, float g, float b) {
      if (this.m_tMsg[i] != null && !sText.equals(this.m_tShown.get(this.m_tMsg[i]))) {
         this.m_tMsg[i].SetTextColor(r, g, b);
      }

      this.Set(this.m_tMsg[i], sText);
   }

   void SetLogo(int nSlot, UIWindow cWnd, int nAnimal) {
      if (cWnd == null || this.m_tLogoShown[nSlot] == nAnimal) {
         return;
      }

      this.m_tLogoShown[nSlot] = nAnimal;
      if (nAnimal < 0) {
         cWnd.SetTextureAlpha(0.0F);
      } else {
         cWnd.SetTexture(ETeams.GetTeamData(nAnimal).m_cLogo);
         cWnd.SetTextureAlpha(1.0F);
      }
   }

   void SetDisabled(UIButton cButton, boolean bDisabled) {
      if (cButton != null) {
         cButton.SetDisabled(bDisabled);
      }
   }

   static String T(String sKey) {
      return Text.Get("&" + sKey + "&");
   }

   static String TeamName(int nAnimal) {
      return nAnimal >= 0 && nAnimal < EAnimals._ANIMALS_COUNT ? ETeams.GetTeamData(nAnimal).m_sName.toLowerCase() : "";
   }

   // « yu  1024 » : le nom d'un joueur et son Elo en ligne, s'il est connu
   static String WithElo(String sName, int nElo) {
      return nElo > 0 && sName.length() > 0 ? sName + "  " + nElo : sName;
   }

   static String NetText() {
      switch (ModLobby.m_nNet) {
         case ModLobby._NET_ONLINE:
            return T("OnlineNetOnline") + (ModLobby.m_nOnline > 1 ? ", " + ModLobby.m_nOnline + T("OnlineCount") : "");
         case ModLobby._NET_UNREACHABLE:
            return T("OnlineNetUnreachable");
         case ModLobby._NET_BAD_PROTO:
            return T("OnlineNetBadProto");
         case ModLobby._NET_FULL:
            return T("OnlineNetFull");
         case ModLobby._NET_FAILED:
         case ModLobby._NET_OFF:
            return T("OnlineNetFailed");
         default:
            return T("OnlineNetConnecting");
      }
   }

   void Refresh() {
      boolean bOnline = ModLobby.m_nNet == ModLobby._NET_ONLINE;
      boolean bRoom = ModLobby.InRoom();
      boolean bHost = ModLobby.IsHost();
      this.Set(this.m_cStatus, this.NetText());
      if (this.m_cStatus != null) {
         if (bOnline) {
            this.m_cStatus.SetTextColor(0.4F, 1.0F, 0.4F);
         } else if (ModLobby.m_nNet == ModLobby._NET_CONNECTING) {
            this.m_cStatus.SetTextColor(0.85F, 0.85F, 0.85F);
         } else {
            this.m_cStatus.SetTextColor(1.0F, 0.45F, 0.45F);
         }
      }

      // Le champ du code : libre hors salon, il montre le code du salon ensuite
      if (bRoom != this.m_bWasInRoom) {
         this.m_bWasInRoom = bRoom;
         this.m_cCode.SetCheck(false);
         this.m_cCode.SetDisabled(false);
         // une partie classée n'a pas de code à donner : on n'y entre que par la file d'attente
         this.m_cCode.SetText(bRoom && !ModLobby.m_bRanked ? ModLobby.m_sRoom : "");
         this.m_cCode.SetDisabled(bRoom);
      }

      this.Set(this.m_cFrameMatch, T(bRoom ? "OnlineFrameMatch" : "OnlineFrameTop"));
      if (this.m_cAction3 != null && this.m_bAction3Shown == bRoom) {
         this.m_bAction3Shown = !bRoom;
         if (bRoom) {
            this.m_cAction3.Hide();
         } else {
            this.m_cAction3.Show();
         }
      }

      this.RefreshTop(!bRoom);
      if (!bRoom) {
         this.RefreshLobby(bOnline);
      } else {
         this.RefreshRoom(bOnline, bHost);
      }
   }

   // Classement de l'Elo en ligne, à la place des deux équipes tant qu'on n'est pas dans un salon
   void RefreshTop(boolean bShow) {
      boolean bKnown = bShow && ModLobby.m_bTopKnown && ModLobby.m_nNet == ModLobby._NET_ONLINE;

      for (int i = 0; i < _TOP_ROWS; i++) {
         String sName = "";
         String sElo = "";
         if (bKnown && i < ModLobby.m_nTopCount) {
            int[] t = ModLobby.m_tTopStats[i];
            int nRank = ModElo.GetRankIndex(t[0]);
            boolean bMe = ModLobby.m_tTopRank[i] == ModLobby.m_nMyRank;
            sName = ModLobby.m_tTopRank[i] + ". " + ModLobby.m_tTopName[i] + (bMe ? T("OnlineYou") : "");
            sElo = t[0] + "   " + t[1] + T("EloWinsAbr") + " " + t[2] + T("EloDrawsAbr") + " " + t[3] + T("EloLossesAbr");
            if (this.m_tTop[i][0] != null && this.m_tTop[i][1] != null) {
               ModElo.SetRankColor(this.m_tTop[i][0], nRank);
               ModElo.SetRankColor(this.m_tTop[i][1], nRank);
            }
         } else if (bKnown && i == 0) {
            sName = T("OnlineTopEmpty");
            if (this.m_tTop[i][0] != null) {
               this.m_tTop[i][0].SetTextColor(0.85F, 0.85F, 0.85F);
            }
         }

         this.Set(this.m_tTop[i][0], sName);
         this.Set(this.m_tTop[i][1], sElo);
      }

      // ma ligne : « yu : Argent, 1024 Elo, rang 2 sur 12 »
      String sMe = "";
      if (bShow && ModLobby.m_nNet == ModLobby._NET_ONLINE && ModLobby.m_nMyElo > 0) {
         int nRank = ModElo.GetRankIndex(ModLobby.m_nMyElo);
         sMe = ModLobby.MyName() + " : " + ModElo.GetRankName(nRank) + ", " + ModLobby.m_nMyElo + " Elo, ";
         if (ModLobby.m_bTopKnown && ModLobby.m_nMyRank > 0) {
            sMe = sMe + T("OnlineRank") + ModLobby.m_nMyRank + T("OnlineOf") + ModLobby.m_nTopTotal;
         } else {
            sMe = sMe + T("OnlineUnranked");
         }

         if (this.m_cTopMe != null) {
            ModElo.SetRankColor(this.m_cTopMe, nRank);
         }
      }

      this.Set(this.m_cTopMe, sMe);
   }

   // hors salon
   void RefreshLobby(boolean bOnline) {
      boolean bBusy = ModLobby.m_nWant != ModLobby._WANT_NONE;
      boolean bQueue = ModLobby.m_nWant == ModLobby._WANT_QUEUE;
      this.Set(this.m_cAction1, T("OnlineCreate"));
      this.Set(this.m_cAction2, T("OnlineJoin"));
      this.SetDisabled(this.m_cAction1, !bOnline || bBusy);
      this.SetDisabled(this.m_cAction2, !bOnline || bBusy);
      this.Set(this.m_cAction3, T("OnlineLeague"));
      this.SetDisabled(this.m_cAction3, bBusy);
      this.Set(this.m_cOk, T("OnlineQuick"));
      this.SetDisabled(this.m_cOk, !bOnline || bBusy);
      this.Set(this.m_cBack, T(bQueue ? "OnlineCancel" : "MenuBack"));
      this.Set(this.m_cStadium, "-");
      this.Set(this.m_cTime, "-");
      this.SetLogo(0, this.m_cLogo1, -1);
      this.SetLogo(1, this.m_cLogo2, -1);
      this.Set(this.m_cVs, "");
      this.Set(this.m_cName1, "");
      this.Set(this.m_cName2, "");
      this.Set(this.m_cPlayer1, "");
      this.Set(this.m_cPlayer2, "");
      this.Set(this.m_cReady1, "");
      this.Set(this.m_cReady2, "");
      String sError = "";
      if (this.m_bBadCode) {
         sError = T("OnlineErrCode");
      } else if (ModLobby.m_nWant == ModLobby._WANT_CREATE) {
         sError = T("OnlineCreating");
      } else if (ModLobby.m_nWant == ModLobby._WANT_JOIN) {
         sError = T("OnlineJoining");
      } else if (bQueue) {
         sError = T("OnlineSearching");
         // l'écart d'Elo accepté par le serveur grandit avec l'attente : « Adversaire cherché : Elo 900 à 1100 »
         int nRange = ModLobby.m_nQueueRange / 10 * 10;
         if (ModLobby.m_bQueued && nRange > 0 && ModLobby.m_nMyElo > 0) {
            int nLow = ModLobby.m_nMyElo - nRange;
            sError = nLow > 0 ? T("OnlineSearchElo") + nLow + T("OnlineSearchTo") + (ModLobby.m_nMyElo + nRange) : T("OnlineSearchAny");
         }
      } else {
         switch (ModLobby.m_nOut) {
            case ModLobby._OUT_UNKNOWN_CODE:
               sError = T("OnlineErrUnknown");
               break;
            case ModLobby._OUT_FULL:
               sError = T("OnlineErrFull");
               break;
            case ModLobby._OUT_VERSION:
               sError = T("OnlineErrVersion");
               break;
            case ModLobby._OUT_CLOSED:
               sError = T("OnlineErrClosed");
               break;
            case ModLobby._OUT_NO_ROOM:
               sError = T("OnlineErrNoRoom");
         }
      }

      this.SetMsg(0, T("OnlineHelp1"), 0.85F, 0.85F, 0.85F);
      this.SetMsg(1, T("OnlineHelp2"), 0.85F, 0.85F, 0.85F);
      if (bBusy && !this.m_bBadCode) {
         this.SetMsg(2, sError, 0.85F, 0.85F, 0.85F);
      } else if (sError.length() == 0 && ModLobby.ResultText().length() > 0) {
         this.SetResultMsg(); // rien à signaler : l'issue du dernier match classé de cette session
      } else {
         this.SetMsg(2, sError, 1.0F, 0.45F, 0.45F);
      }
   }

   // Dernière ligne de message : « Victoire 2-1 : +24 Elo », en vert, rouge ou gris
   void SetResultMsg() {
      this.Set(this.m_tMsg[2], ModLobby.ResultText());
      if (this.m_tMsg[2] != null) {
         ModLobby.SetResultColor(this.m_tMsg[2]);
      }
   }

   // dans un salon : à gauche l'hôte, à droite l'invité
   void RefreshRoom(boolean bOnline, boolean bHost) {
      String sMe = ModLobby.MyName() + T("OnlineYou");
      boolean bPeer = ModLobby.m_bPeerHere;
      boolean bPeerTeam = bPeer && ModLobby.m_bPeerState && ModLobby.IsValidTeam(ModLobby.m_nPeerAnimal, ModLobby.m_nPeerClothes);
      int nHostAnimal = bHost ? ModLobby.m_nMyAnimal : (bPeerTeam ? ModLobby.m_nPeerAnimal : -1);
      int nGuestAnimal = bHost ? (bPeerTeam ? ModLobby.m_nPeerAnimal : -1) : ModLobby.m_nMyAnimal;
      boolean bRanked = ModLobby.m_bRanked;
      boolean bGuestReady = bHost ? bPeer && ModLobby.m_bPeerReady : ModLobby.m_bReady;
      boolean bGuestHere = !bHost || bPeer;
      // en partie classée l'hôte aussi se met prêt
      boolean bHostReady = bHost ? ModLobby.m_bReady : bPeer && ModLobby.m_bPeerReady;
      String sMeElo = WithElo(sMe, ModLobby.m_nMyElo);
      String sPeerElo = WithElo(ModLobby.m_sPeerName, ModLobby.m_nPeerElo);
      this.Set(this.m_cVs, T("SurvivalVs"));
      this.SetLogo(0, this.m_cLogo1, nHostAnimal);
      this.SetLogo(1, this.m_cLogo2, nGuestAnimal);
      this.Set(this.m_cName1, TeamName(nHostAnimal));
      this.Set(this.m_cName2, TeamName(nGuestAnimal));
      this.Set(this.m_cPlayer1, bHost ? sMeElo : sPeerElo);
      this.Set(this.m_cPlayer2, bHost ? sPeerElo : sMeElo);
      this.Set(this.m_cReady1, !bRanked ? T("OnlineHost") : (!bHost && !bPeer ? "" : T(bHostReady ? "OnlineReady" : "OnlineNotReady")));
      this.Set(this.m_cReady2, !bGuestHere ? "" : T(bGuestReady ? "OnlineReady" : "OnlineNotReady"));
      if (this.m_cReady1 != null) {
         if (bRanked && bHostReady) {
            this.m_cReady1.SetTextColor(0.4F, 1.0F, 0.4F);
         } else {
            this.m_cReady1.SetTextColor(bRanked ? 0.85F : 1.0F, bRanked ? 0.85F : 1.0F, bRanked ? 0.85F : 1.0F);
         }
      }

      if (this.m_cReady2 != null) {
         if (bGuestReady) {
            this.m_cReady2.SetTextColor(0.4F, 1.0F, 0.4F);
         } else {
            this.m_cReady2.SetTextColor(0.85F, 0.85F, 0.85F);
         }
      }

      // stade et durée : choisis par l'hôte, connus de l'invité dès le premier état reçu
      boolean bSettings = bHost || bPeer && ModLobby.m_bPeerState;
      int nLevel = ModLobby.GetLevel();
      String sStadium = bSettings && nLevel >= 0 && nLevel < ModLobby._LEVELS ? ETeams.GetTeamData(nLevel).m_sNameAbr : "-";
      this.Set(this.m_cStadium, ModLobby.IsHomeLevel() ? T("OnlineHome") : sStadium);
      this.Set(this.m_cTime, bSettings ? "2 x " + ModLobby.GetHalfSeconds() / 60 + T("OnlineMinutes") : "-");

      this.Set(this.m_cAction1, T("OnlineTeam"));
      this.Set(this.m_cAction2, T("OnlineTactics"));
      // prêt = choix arrêtés : on repasse à « pas prêt » pour changer d'équipe ou de tactique
      boolean bLocked = (!bHost || bRanked) && ModLobby.m_bReady;
      this.SetDisabled(this.m_cAction1, bLocked);
      this.SetDisabled(this.m_cAction2, bLocked);
      this.Set(this.m_cBack, T("OnlineLeave"));
      this.SetMsg(0, bRanked ? T("OnlineRankedRoom") : T("OnlineRoomCode") + ModLobby.m_sRoom, 1.0F, 0.82F, 0.25F);
      boolean bHostBusy = !bHost && bPeer && ModLobby.m_bPeerState && ModLobby.m_nPeerMatchId != 0;
      if (bRanked) {
         this.Set(this.m_cOk, T(ModLobby.m_bReady ? "OnlineNotReady" : "OnlineReady"));
         this.SetDisabled(this.m_cOk, !bOnline || bHostBusy);
         if (bHostBusy) {
            this.SetMsg(1, T("OnlineHostBusy"), 0.85F, 0.85F, 0.85F);
         } else if (bHostReady && bGuestReady) {
            this.SetMsg(1, T("OnlineRankedGo"), 0.4F, 1.0F, 0.4F);
         } else if (ModLobby.m_bReady) {
            this.SetMsg(1, T("OnlineRankedWait"), 0.4F, 1.0F, 0.4F);
         } else {
            this.SetMsg(1, T("OnlineGuestHint"), 0.85F, 0.85F, 0.85F);
         }
      } else if (bHost) {
         this.Set(this.m_cOk, T("OnlinePlay"));
         this.SetDisabled(this.m_cOk, !ModLobby.CanStart());
         if (!bPeer) {
            this.SetMsg(1, T("OnlineWaitPeer"), 0.85F, 0.85F, 0.85F);
         } else if (!ModLobby.CanStart()) {
            this.SetMsg(1, T("OnlineWaitReady"), 0.85F, 0.85F, 0.85F);
         } else {
            this.SetMsg(1, T("OnlineAllReady"), 0.4F, 1.0F, 0.4F);
         }
      } else {
         this.Set(this.m_cOk, T(ModLobby.m_bReady ? "OnlineNotReady" : "OnlineReady"));
         this.SetDisabled(this.m_cOk, !bOnline || bHostBusy);
         if (bHostBusy) {
            this.SetMsg(1, T("OnlineHostBusy"), 0.85F, 0.85F, 0.85F);
         } else if (ModLobby.m_bReady) {
            this.SetMsg(1, T("OnlineWaitHost"), 0.4F, 1.0F, 0.4F);
         } else {
            this.SetMsg(1, T("OnlineGuestHint"), 0.85F, 0.85F, 0.85F);
         }
      }

      if (bOnline) {
         this.SetResultMsg(); // l'issue du dernier match classé, s'il y en a un
      } else {
         this.SetMsg(2, this.NetText(), 1.0F, 0.45F, 0.45F);
      }
   }

   // ---------------------------------------------------------------- actions

   void OnJoin() {
      String sCode = ModLobby.CleanCode(this.m_cCode.m_sEditText);
      this.m_bBadCode = sCode == null;
      if (sCode != null) {
         ModLobby.Join(sCode);
      }

      this.Refresh();
   }

   void OnAction1() {
      if (!ModLobby.InRoom()) {
         this.m_bBadCode = false;
         ModLobby.Create();
         this.Refresh();
      } else {
         // choix de l'équipe et de la tenue, avec les écrans du match simple ; retour ici par MenuChooseTeamColors.OnOK
         ModLobby.SetReady(false);
         CoreLogic.m_nNextLevel = -1;
         MenuBackground.SetPlayerType(0);
         ((SoccerMainMenuModule)this.GetModule()).GetUI(2).Reset();
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(2);
      }
   }

   void OnAction2() {
      if (!ModLobby.InRoom()) {
         this.OnJoin();
      } else {
         ModLobby.SetReady(false);
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_TACTICS);
      }
   }

   void OnOK() {
      if (!ModLobby.InRoom()) {
         this.m_bBadCode = false;
         ModLobby.Queue(); // partie rapide
         this.Refresh();
      } else if (ModLobby.IsHost() && !ModLobby.m_bRanked) {
         ModLobby.HostStart();
      } else {
         ModLobby.SetReady(!ModLobby.m_bReady);
         this.Refresh();
      }
   }

   void OnBack() {
      if (ModLobby.InRoom() || ModLobby.m_nWant == ModLobby._WANT_QUEUE) {
         ModLobby.Leave(); // quitte le salon, ou la file d'attente
         this.Refresh();
      } else {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(0); // MenuMain.Show ferme la liaison au serveur
      }
   }

   void OnUICommand(UIWindow cSender, int nCommand) {
      super.OnUICommand(cSender, nCommand);
      if (cSender.m_sId == null) {
         return;
      }

      String sId = cSender.m_sId;
      // Entrée dans le champ du code = « rejoindre »
      if (nCommand == 3 && sId.equals("ID_CODE")) {
         if (!ModLobby.InRoom() && this.m_cCode.m_sEditText.length() > 0) {
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
      } else if (sId.equals("ID_ACTION3") && !ModLobby.InRoom() && ModLobby.m_nWant == ModLobby._WANT_NONE) {
         ((SoccerMainMenuModule)this.GetModule()).ShowUI(SoccerMainMenuModule._UI_ONLINE_LEAGUE);
      } else if (sId.startsWith("ID_STADIUM_") && nDelta != 0) {
         ModLobby.NextLevel(nDelta);
         this.Refresh();
      } else if (sId.startsWith("ID_TIME_") && nDelta != 0) {
         ModLobby.NextHalf(nDelta);
         this.Refresh();
      }
   }
}
