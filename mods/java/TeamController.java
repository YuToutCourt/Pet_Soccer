public class TeamController extends GameObject {
   int _KEY_STATES = 0;
   float _JOY_ANGLE = 0.0F;
   static final int _KEY_UP = 1;
   static final int _KEY_RIGHT = 2;
   static final int _KEY_DOWN = 4;
   static final int _KEY_LEFT = 8;
   boolean m_bAccelBrakeDigital = true;
   boolean m_bTurningDigital = true;
   boolean m_bJoyEnabled = false;
   int m_nDevice = -1;
   AITeam m_cTeam = null;
   InputAction[] m_Actions = new InputAction[InputAction._ACTIONS_NUM];
   boolean m_bRemote = false; // [mod] en ligne : manette pilotée par le réseau, sourde au clavier local
   int m_nKeySet = -1; // [mod] jeu de touches (0 = joueur 1, 1 = joueur 2), pour les touches directes de ModSwitch

   // [mod] en ligne : la manette devient un clavier virtuel dont les touches (codes ModNet._ACT_KEYS,
   // hors de la plage des vraies touches) arrivent par Inject()
   void SetRemote() {
      this.m_bRemote = true;
      this.m_nDevice = 3;
      this._KEY_STATES = 0;
      this.m_Actions[0] = new InputDigital(3, 0, ModNet._ACT_KEYS[ModNet._ACT_PASS], this);
      this.m_Actions[1] = new InputDigital(3, 1, ModNet._ACT_KEYS[ModNet._ACT_SHOT], this);
      this.m_Actions[2] = new InputDigital(3, 2, ModNet._ACT_KEYS[ModNet._ACT_FEINT], this);
      this.m_Actions[4] = new InputDigital(3, 4, ModNet._ACT_KEYS[ModNet._ACT_SUPER], this);
      this.m_Actions[3] = new InputDigital(3, 3, 255, this);
      this.m_Actions[5] = new InputDigital(3, 5, 255, this);
      this.SetUpKey(ModNet._ACT_KEYS[ModNet._ACT_UP]);
      this.SetRightKey(ModNet._ACT_KEYS[ModNet._ACT_RIGHT]);
      this.SetDownKey(ModNet._ACT_KEYS[ModNet._ACT_DOWN]);
      this.SetLeftKey(ModNet._ACT_KEYS[ModNet._ACT_LEFT]);
   }

   // [mod] même chemin que OnInputKey, pour une touche venue du réseau
   void Inject(int nKeyCode, boolean bKeyState) {
      for (int i = 0; i < InputAction._ACTIONS_NUM; i++) {
         if (this.m_Actions[i] != null) {
            this.m_Actions[i].Translate(3, nKeyCode, bKeyState);
         }
      }
   }

   void SetDownKey(int nDown) {
      if (3 < InputAction._ACTIONS_NUM) {
         if (this.m_Actions[3].m_iActionType == 3) {
            if (!this.IsJoy()) {
               InputDigital cAction = (InputDigital)this.m_Actions[3];
               cAction.m_nDownKey = nDown;
               if (5 < InputAction._ACTIONS_NUM) {
                  if (this.m_Actions[5].m_iActionType == 5) {
                     cAction = (InputDigital)this.m_Actions[5];
                     cAction.m_nDownKey = nDown;
                  }
               }
            }
         }
      }
   }

   Vector GetFieldDirFromKeys() {
      switch (this._KEY_STATES) {
         case 1:
            return GameField.GetControlDir(0);
         case 2:
            return GameField.GetControlDir(2);
         case 3:
            return GameField.GetControlDir(1);
         case 4:
            return GameField.GetControlDir(4);
         case 5:
         case 7:
         case 10:
         case 11:
         default:
            return null;
         case 6:
            return GameField.GetControlDir(3);
         case 8:
            return GameField.GetControlDir(6);
         case 9:
            return GameField.GetControlDir(7);
         case 12:
            return GameField.GetControlDir(5);
      }
   }

   boolean IsJoy() {
      return this.m_nDevice == 0 || this.m_nDevice == 1;
   }

   public void OnInputJoyButton(int nJoy, int nButton, boolean bState) {
      if (this.m_bRemote) { // [mod]
         return;
      }

      nButton += (nJoy + 1) * 1000;
      if (ModSwitch.OnKey(this, nButton, bState)) { // [mod] touches directes vers un joueur
         return;
      }

      for (int i = 0; i < InputAction._ACTIONS_NUM; i++) {
         if (this.m_Actions[i] != null) {
            this.m_Actions[i].Translate(3, nButton, bState);
         }
      }
   }

   void SetKeyCode(int nAction, int nKeyCode) {
      InputAction cAction = this.m_Actions[nAction];
      if (cAction instanceof InputDigital) {
         ((InputDigital)cAction).m_iKeyValue = nKeyCode;
      }
   }

   boolean IsKeyboard() {
      return this.m_nDevice == 3;
   }

   void Clear() {
      this._KEY_STATES = 0;
   }

   public void OnInputJoyAxis(int nJoy, int nAxis, float fDelta) {
      if (this.m_bRemote) { // [mod]
         return;
      }

      for (int i = 0; i < InputAction._ACTIONS_NUM; i++) {
         if (this.m_Actions[i] != null) {
            this.m_Actions[i].Translate(nJoy, nAxis, fDelta);
         }
      }
   }

   void OnCreate() {
      this.RegisterKeyRequest();
      this.RegisterJoyAxisRequest();
      this.RegisterJoyButtonRequest();
      SoccerGame.IncController();
   }

   void OnInputKey(int nKeyCode, boolean bKeyState, char nKeyAscii) {
      if (this.m_bRemote) { // [mod]
         return;
      }

      // [mod] touches directes vers un joueur (avant le réseau : le client les envoie lui-même)
      if (ModSwitch.OnKey(this, nKeyCode, bKeyState)) {
         return;
      }

      // [mod] en ligne : le client envoie ses touches à l'hôte au lieu de les jouer
      if (ModNet.OnLocalKey(this, nKeyCode, bKeyState)) {
         return;
      }

      for (int i = 0; i < InputAction._ACTIONS_NUM; i++) {
         if (this.m_Actions[i] != null) {
            this.m_Actions[i].Translate(3, nKeyCode, bKeyState);
         }
      }
   }

   void Init(AITeam cTeam, KbSettings cKbSettings) {
      this.m_cTeam = cTeam;
      this.m_nKeySet = cKbSettings == SoccerGame.GetKb0() ? 0 : (cKbSettings == SoccerGame.GetKb1() ? 1 : -1); // [mod]
      if (cKbSettings.m_Device == 0) {
         this.m_nDevice = 3;
      } else if (cKbSettings.m_JoyNumber == 0) {
         this.m_nDevice = 0;
      } else {
         this.m_nDevice = 1;
      }

      this.m_Actions[0] = new InputDigital(3, 0, 31, this);
      this.m_Actions[1] = new InputDigital(3, 1, 32, this);
      this.m_Actions[2] = new InputDigital(3, 2, 30, this);
      this.m_Actions[4] = new InputDigital(3, 4, 33, this);
      if (this.m_nDevice != 0 && this.m_nDevice != 1) {
         this.m_Actions[3] = new InputDigital(3, 3, 255, this);
         this.m_Actions[5] = new InputDigital(3, 5, 255, this);
      } else {
         this.m_Actions[3] = new InputAnalog(this.m_nDevice, 3, this);
         this.m_Actions[5] = new InputAnalog(this.m_nDevice, 5, this);
      }
   }

   void SetLeftKey(int nLeft) {
      if (3 < InputAction._ACTIONS_NUM) {
         if (this.m_Actions[3].m_iActionType == 3) {
            if (!this.IsJoy()) {
               InputDigital cAction = (InputDigital)this.m_Actions[3];
               cAction.m_nLeftKey = nLeft;
               if (5 < InputAction._ACTIONS_NUM) {
                  if (this.m_Actions[5].m_iActionType == 5) {
                     cAction = (InputDigital)this.m_Actions[5];
                     cAction.m_nLeftKey = nLeft;
                  }
               }
            }
         }
      }
   }

   void SetRightKey(int nRight) {
      if (3 < InputAction._ACTIONS_NUM) {
         if (this.m_Actions[3].m_iActionType == 3) {
            if (!this.IsJoy()) {
               InputDigital cAction = (InputDigital)this.m_Actions[3];
               cAction.m_nRightKey = nRight;
               if (5 < InputAction._ACTIONS_NUM) {
                  if (this.m_Actions[5].m_iActionType == 5) {
                     cAction = (InputDigital)this.m_Actions[5];
                     cAction.m_nRightKey = nRight;
                  }
               }
            }
         }
      }
   }

   void SetUpKey(int nUp) {
      if (3 < InputAction._ACTIONS_NUM) {
         if (this.m_Actions[3].m_iActionType == 3) {
            if (!this.IsJoy()) {
               InputDigital cAction = (InputDigital)this.m_Actions[3];
               cAction.m_nUpKey = nUp;
               if (5 < InputAction._ACTIONS_NUM) {
                  if (this.m_Actions[5].m_iActionType == 5) {
                     cAction = (InputDigital)this.m_Actions[5];
                     cAction.m_nUpKey = nUp;
                  }
               }
            }
         }
      }
   }

   public void OnDestroy() {
      this.m_cTeam = null;

      for (int i = 0; i < this.m_Actions.length; i++) {
         this.m_Actions[i].delete();
         this.m_Actions[i] = null;
      }
   }
}
