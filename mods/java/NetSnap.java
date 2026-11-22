// [mod] en ligne : un état du match reçu de l'hôte (voir ModNet)
class NetSnap {
   static final int _MAX_OBJ = 15; // 7 + 7 joueurs + l'arbitre
   int nSeq = 0;
   float fTime = 0.0F; // horloge du module de l'hôte, en secondes
   int nMatchState = 0;
   int nHalfNo = 0;
   int nScoreLeft = 0;
   int nScoreRight = 0;
   float fClock = 0.0F;
   int nBallOwner = -1;
   int nCamTarget = 100;
   int nFocus0 = -1;
   int nMark0 = -1;
   int nFocus1 = -1;
   int nMark1 = -1;
   float[] tCam = new float[6]; // caméra de l'hôte : position, direction
   boolean bCamReplay = false;
   boolean bCamFree = false; // caméra de l'hôte en jeu normal (ni ralenti, ni fin de mi-temps, ni pause)
   float[] tBall = new float[6]; // position, vitesse
   int nCount0 = 0;
   int nCount1 = 0;
   float[] tObj = new float[_MAX_OBJ * 6]; // par objet : position, direction
   // suite de l'état (phase 2) : voir ModNet.HostPutExtras
   int nFlagsA = 0;
   int nFlagsB = 0;
   int nMenu = 0; // menu de l'hôte : 0 aucun, 1 pause, 2 mi-temps, 3 fin de match
   int nFade = 0; // fondu au noir, 0..255
   int nSpeed = 100; // vitesse du temps de l'hôte, en centièmes (ralentis)
   int nPlayer1 = 0;
   int nHalfTime = 0; // durée d'une mi-temps chez l'hôte, en secondes
   int nSpeedObj = -1; // joueur qui porte le son du super « vitesse », -1 : aucun
   int nFeintObj = -1;
   int nHalfLeft = 255; // pause de mi-temps : secondes avant la reprise, 255 hors de la pause
   float[] tExtra = new float[15]; // viseur, mur de l'équipe A, mur de l'équipe B, leurre, repère de destination
}
