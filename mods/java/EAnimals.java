public class EAnimals {
   static final int _ANIMAL_BEAR = 0;
   static final int _ANIMAL_BULL = 1;
   static final int _ANIMAL_CHICKEN = 2;
   static final int _ANIMAL_DINO = 3;
   static final int _ANIMAL_FROG = 4;
   static final int _ANIMAL_GIRAFFE = 5;
   static final int _ANIMAL_GORILLA = 6;
   static final int _ANIMAL_LION = 7;
   static final int _ANIMAL_PANTHER = 8;
   static final int _ANIMAL_SHARK = 9;
   static final int _ANIMAL_TURTLE = 10;
   static final int _ANIMAL_ROBO = 11;
   // [mod] deux equipes ajoutees, sur des modeles existants retextures (voir notes/new-teams.md)
   static final int _ANIMAL_PANDA = 12;
   static final int _ANIMAL_TIGER = 13;
   static final int _ANIMALS_COUNT = 14;
   // [mod] equipe d'origine dont la nouvelle equipe reprend les commentaires audio
   static final int[] m_nCommentAnimal = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 0, 8};
   static final String[] m_sMeshName = new String[]{
      "mis.3da",
      "byk.3da",
      "kurczak.3da",
      "dino.3da",
      "zaba.3da",
      "zyrafa.3da",
      "goryl.3da",
      "lew.3da",
      "pantera.3da",
      "rekin.3da",
      "zolw.3da",
      "robocik.3da",
      "mis.3da",
      "pantera.3da"
   };
   static final String m_sMeshNameMenuUnavail = "data/menu/Unavail.3da";
   static final String[][] m_sTextureNormal = new String[][]{
      {
            "data/textures/tshirts/mis_01.png",
            "data/textures/tshirts/mis_02.png",
            "data/textures/tshirts/mis_03.png",
            "data/textures/tshirts/mis_04.png",
            "data/textures/tshirts/mis_05.png"
      },
      {
            "data/textures/tshirts/byk_01.png",
            "data/textures/tshirts/byk_02.png",
            "data/textures/tshirts/byk_03.png",
            "data/textures/tshirts/byk_04.png",
            "data/textures/tshirts/byk_05.png"
      },
      {
            "data/textures/tshirts/kurczak_01.png",
            "data/textures/tshirts/kurczak_02.png",
            "data/textures/tshirts/kurczak_03.png",
            "data/textures/tshirts/kurczak_04.png",
            "data/textures/tshirts/kurczak_05.png"
      },
      {
            "data/textures/tshirts/dino_01.png",
            "data/textures/tshirts/dino_02.png",
            "data/textures/tshirts/dino_03.png",
            "data/textures/tshirts/dino_04.png",
            "data/textures/tshirts/dino_05.png"
      },
      {
            "data/textures/tshirts/zaba_01.png",
            "data/textures/tshirts/zaba_02.png",
            "data/textures/tshirts/zaba_03.png",
            "data/textures/tshirts/zaba_04.png",
            "data/textures/tshirts/zaba_05.png"
      },
      {
            "data/textures/tshirts/zyrafa_01.png",
            "data/textures/tshirts/zyrafa_02.png",
            "data/textures/tshirts/zyrafa_03.png",
            "data/textures/tshirts/zyrafa_04.png",
            "data/textures/tshirts/zyrafa_05.png"
      },
      {
            "data/textures/tshirts/goryl_01.png",
            "data/textures/tshirts/goryl_02.png",
            "data/textures/tshirts/goryl_03.png",
            "data/textures/tshirts/goryl_04.png",
            "data/textures/tshirts/goryl_05.png"
      },
      {
            "data/textures/tshirts/lew_01.png",
            "data/textures/tshirts/lew_02.png",
            "data/textures/tshirts/lew_03.png",
            "data/textures/tshirts/lew_04.png",
            "data/textures/tshirts/lew_05.png"
      },
      {
            "data/textures/tshirts/pantera_01.png",
            "data/textures/tshirts/pantera_02.png",
            "data/textures/tshirts/pantera_03.png",
            "data/textures/tshirts/pantera_04.png",
            "data/textures/tshirts/pantera_05.png"
      },
      {
            "data/textures/tshirts/rekin_01.png",
            "data/textures/tshirts/rekin_02.png",
            "data/textures/tshirts/rekin_03.png",
            "data/textures/tshirts/rekin_04.png",
            "data/textures/tshirts/rekin_05.png"
      },
      {
            "data/textures/tshirts/zolw_01.png",
            "data/textures/tshirts/zolw_02.png",
            "data/textures/tshirts/zolw_03.png",
            "data/textures/tshirts/zolw_04.png",
            "data/textures/tshirts/zolw_05.png"
      },
      {
            "data/textures/tshirts/robocik_01.png",
            "data/textures/tshirts/robocik_02.png",
            "data/textures/tshirts/robocik_03.png",
            "data/textures/tshirts/robocik_04.png",
            "data/textures/tshirts/robocik_05.png"
      },
      {
            "data/textures/tshirts/panda_01.png",
            "data/textures/tshirts/panda_02.png",
            "data/textures/tshirts/panda_03.png",
            "data/textures/tshirts/panda_04.png",
            "data/textures/tshirts/panda_05.png"
      },
      {
            "data/textures/tshirts/tygrys_01.png",
            "data/textures/tshirts/tygrys_02.png",
            "data/textures/tshirts/tygrys_03.png",
            "data/textures/tshirts/tygrys_04.png",
            "data/textures/tshirts/tygrys_05.png"
      }
   };
   static final String[][] m_sTextureNormalLogo = new String[][]{
      {
            "data/menu/TShirtsIcons/misie01.png",
            "data/menu/TShirtsIcons/misie02.png",
            "data/menu/TShirtsIcons/misie03.png",
            "data/menu/TShirtsIcons/misie04.png",
            "data/menu/TShirtsIcons/misie05.png"
      },
      {
            "data/menu/TShirtsIcons/byki01.png",
            "data/menu/TShirtsIcons/byki02.png",
            "data/menu/TShirtsIcons/byki03.png",
            "data/menu/TShirtsIcons/byki04.png",
            "data/menu/TShirtsIcons/byki05.png"
      },
      {
            "data/menu/TShirtsIcons/kurczaki01.png",
            "data/menu/TShirtsIcons/kurczaki02.png",
            "data/menu/TShirtsIcons/kurczaki03.png",
            "data/menu/TShirtsIcons/kurczaki04.png",
            "data/menu/TShirtsIcons/kurczaki05.png"
      },
      {
            "data/menu/TShirtsIcons/dino01.png",
            "data/menu/TShirtsIcons/dino02.png",
            "data/menu/TShirtsIcons/dino03.png",
            "data/menu/TShirtsIcons/dino04.png",
            "data/menu/TShirtsIcons/dino05.png"
      },
      {
            "data/menu/TShirtsIcons/zaby01.png",
            "data/menu/TShirtsIcons/zaby02.png",
            "data/menu/TShirtsIcons/zaby03.png",
            "data/menu/TShirtsIcons/zaby04.png",
            "data/menu/TShirtsIcons/zaby05.png"
      },
      {
            "data/menu/TShirtsIcons/zyrafy01.png",
            "data/menu/TShirtsIcons/zyrafy02.png",
            "data/menu/TShirtsIcons/zyrafy03.png",
            "data/menu/TShirtsIcons/zyrafy04.png",
            "data/menu/TShirtsIcons/zyrafy05.png"
      },
      {
            "data/menu/TShirtsIcons/goryle01.png",
            "data/menu/TShirtsIcons/goryle02.png",
            "data/menu/TShirtsIcons/goryle03.png",
            "data/menu/TShirtsIcons/goryle04.png",
            "data/menu/TShirtsIcons/goryle05.png"
      },
      {
            "data/menu/TShirtsIcons/lwy01.png",
            "data/menu/TShirtsIcons/lwy02.png",
            "data/menu/TShirtsIcons/lwy03.png",
            "data/menu/TShirtsIcons/lwy04.png",
            "data/menu/TShirtsIcons/lwy05.png"
      },
      {
            "data/menu/TShirtsIcons/pantery01.png",
            "data/menu/TShirtsIcons/pantery02.png",
            "data/menu/TShirtsIcons/pantery03.png",
            "data/menu/TShirtsIcons/pantery04.png",
            "data/menu/TShirtsIcons/pantery05.png"
      },
      {
            "data/menu/TShirtsIcons/rekiny01.png",
            "data/menu/TShirtsIcons/rekiny02.png",
            "data/menu/TShirtsIcons/rekiny03.png",
            "data/menu/TShirtsIcons/rekiny04.png",
            "data/menu/TShirtsIcons/rekiny05.png"
      },
      {
            "data/menu/TShirtsIcons/zolwie01.png",
            "data/menu/TShirtsIcons/zolwie02.png",
            "data/menu/TShirtsIcons/zolwie03.png",
            "data/menu/TShirtsIcons/zolwie04.png",
            "data/menu/TShirtsIcons/zolwie05.png"
      },
      {
            "data/menu/TShirtsIcons/roboty01.png",
            "data/menu/TShirtsIcons/roboty02.png",
            "data/menu/TShirtsIcons/roboty03.png",
            "data/menu/TShirtsIcons/roboty04.png",
            "data/menu/TShirtsIcons/roboty05.png"
      },
      {
            "data/menu/TShirtsIcons/pandy01.png",
            "data/menu/TShirtsIcons/pandy02.png",
            "data/menu/TShirtsIcons/pandy03.png",
            "data/menu/TShirtsIcons/pandy04.png",
            "data/menu/TShirtsIcons/pandy05.png"
      },
      {
            "data/menu/TShirtsIcons/tygrysy01.png",
            "data/menu/TShirtsIcons/tygrysy02.png",
            "data/menu/TShirtsIcons/tygrysy03.png",
            "data/menu/TShirtsIcons/tygrysy04.png",
            "data/menu/TShirtsIcons/tygrysy05.png"
      }
   };
   static final String[] m_sTexturePublic = new String[]{
      "data/textures/tshirts/mis_P.png",
      "data/textures/tshirts/byk_P.png",
      "data/textures/tshirts/kurczak_P.png",
      "data/textures/tshirts/dino_P.png",
      "data/textures/tshirts/zaba_P.png",
      "data/textures/tshirts/zyrafa_P.png",
      "data/textures/tshirts/goryl_P.png",
      "data/textures/tshirts/lew_P.png",
      "data/textures/tshirts/pantera_P.png",
      "data/textures/tshirts/rekin_P.png",
      "data/textures/tshirts/zolw_P.png",
      "data/textures/tshirts/robocik_P.png",
      "data/textures/tshirts/panda_P.png",
      "data/textures/tshirts/tygrys_P.png"
   };
   static final String[][] m_sTextureGoalKeeper = new String[][]{
      {
            "data/textures/tshirts/mis_B01.png",
            "data/textures/tshirts/mis_B02.png",
            "data/textures/tshirts/mis_B03.png",
            "data/textures/tshirts/mis_B04.png",
            "data/textures/tshirts/mis_B05.png"
      },
      {
            "data/textures/tshirts/byk_B01.png",
            "data/textures/tshirts/byk_B02.png",
            "data/textures/tshirts/byk_B03.png",
            "data/textures/tshirts/byk_B04.png",
            "data/textures/tshirts/byk_B05.png"
      },
      {
            "data/textures/tshirts/kurczak_B01.png",
            "data/textures/tshirts/kurczak_B02.png",
            "data/textures/tshirts/kurczak_B03.png",
            "data/textures/tshirts/kurczak_B04.png",
            "data/textures/tshirts/kurczak_B05.png"
      },
      {
            "data/textures/tshirts/dino_B01.png",
            "data/textures/tshirts/dino_B02.png",
            "data/textures/tshirts/dino_B03.png",
            "data/textures/tshirts/dino_B04.png",
            "data/textures/tshirts/dino_B05.png"
      },
      {
            "data/textures/tshirts/zaba_B01.png",
            "data/textures/tshirts/zaba_B02.png",
            "data/textures/tshirts/zaba_B03.png",
            "data/textures/tshirts/zaba_B04.png",
            "data/textures/tshirts/zaba_B05.png"
      },
      {
            "data/textures/tshirts/zyrafa_B01.png",
            "data/textures/tshirts/zyrafa_B02.png",
            "data/textures/tshirts/zyrafa_B03.png",
            "data/textures/tshirts/zyrafa_B04.png",
            "data/textures/tshirts/zyrafa_B05.png"
      },
      {
            "data/textures/tshirts/goryl_B01.png",
            "data/textures/tshirts/goryl_B02.png",
            "data/textures/tshirts/goryl_B03.png",
            "data/textures/tshirts/goryl_B04.png",
            "data/textures/tshirts/goryl_B05.png"
      },
      {
            "data/textures/tshirts/lew_B01.png",
            "data/textures/tshirts/lew_B02.png",
            "data/textures/tshirts/lew_B03.png",
            "data/textures/tshirts/lew_B04.png",
            "data/textures/tshirts/lew_B05.png"
      },
      {
            "data/textures/tshirts/pantera_B01.png",
            "data/textures/tshirts/pantera_B02.png",
            "data/textures/tshirts/pantera_B03.png",
            "data/textures/tshirts/pantera_B04.png",
            "data/textures/tshirts/pantera_B05.png"
      },
      {
            "data/textures/tshirts/rekin_B01.png",
            "data/textures/tshirts/rekin_B02.png",
            "data/textures/tshirts/rekin_B03.png",
            "data/textures/tshirts/rekin_B04.png",
            "data/textures/tshirts/rekin_B05.png"
      },
      {
            "data/textures/tshirts/zolw_B01.png",
            "data/textures/tshirts/zolw_B02.png",
            "data/textures/tshirts/zolw_B03.png",
            "data/textures/tshirts/zolw_B04.png",
            "data/textures/tshirts/zolw_B05.png"
      },
      {
            "data/textures/tshirts/robocik_B01.png",
            "data/textures/tshirts/robocik_B02.png",
            "data/textures/tshirts/robocik_B03.png",
            "data/textures/tshirts/robocik_B04.png",
            "data/textures/tshirts/robocik_B05.png"
      },
      {
            "data/textures/tshirts/panda_B01.png",
            "data/textures/tshirts/panda_B02.png",
            "data/textures/tshirts/panda_B03.png",
            "data/textures/tshirts/panda_B04.png",
            "data/textures/tshirts/panda_B05.png"
      },
      {
            "data/textures/tshirts/tygrys_B01.png",
            "data/textures/tshirts/tygrys_B02.png",
            "data/textures/tshirts/tygrys_B03.png",
            "data/textures/tshirts/tygrys_B04.png",
            "data/textures/tshirts/tygrys_B05.png"
      }
   };
   static final String[] m_sTextureReferee = new String[]{
      "data/textures/tshirts/mis_S.png",
      "data/textures/tshirts/byk_S.png",
      "data/textures/tshirts/kurczak_S.png",
      "data/textures/tshirts/dino_S.png",
      "data/textures/tshirts/zaba_S.png",
      "data/textures/tshirts/zyrafa_S.png",
      "data/textures/tshirts/goryl_S.png",
      "data/textures/tshirts/lew_S.png",
      "data/textures/tshirts/pantera_S.png",
      "data/textures/tshirts/rekin_S.png",
      "data/textures/tshirts/zolw_S.png",
      "data/textures/tshirts/robocik_S.png",
      "data/textures/tshirts/panda_S.png",
      "data/textures/tshirts/tygrys_S.png"
   };
   static final String[] m_sNames = new String[]{
      "Bear", "Bull", "Chicken", "Dino", "Frog", "Giraffe", "Gorilla", "Lion", "Panther", "Shark", "Turtle", "Robo", "Panda", "Tiger"
   };
   static final String[] m_sNamesGK = new String[]{
      "Bear_GK",
      "Bull_GK",
      "Chicken_GK",
      "Dino_GK",
      "Frog_GK",
      "Giraffe_GK",
      "Gorilla_GK",
      "Lion_GK",
      "Panther_GK",
      "Shark_GK",
      "Turtle_GK",
      "Robo_GK",
      "Panda_GK",
      "Tiger_GK"
   };
   static final String m_sWavPath = "data/sounds/commentary/";
   static final int _FIRST_FORM = 0;
   static final int _SEC_FORM = 1;
   static final String[] m_sTeamNamesWav = new String[]{
      "data/sounds/commentary/" + "001.wav",
      "data/sounds/commentary/" + "003.wav",
      "data/sounds/commentary/" + "002.wav",
      "data/sounds/commentary/" + "008.wav",
      "data/sounds/commentary/" + "006.wav",
      "data/sounds/commentary/" + "012.wav",
      "data/sounds/commentary/" + "009.wav",
      "data/sounds/commentary/" + "007.wav",
      "data/sounds/commentary/" + "005.wav",
      "data/sounds/commentary/" + "010.wav",
      "data/sounds/commentary/" + "004.wav",
      "data/sounds/commentary/" + "011.wav"
   };
   static final String[] m_sTeamCommentsWav = new String[]{
      "data/sounds/commentary/" + "112.wav",
      "data/sounds/commentary/" + "114.wav",
      "data/sounds/commentary/" + "113.wav",
      "data/sounds/commentary/" + "119.wav",
      "data/sounds/commentary/" + "117.wav",
      "data/sounds/commentary/" + "123.wav",
      "data/sounds/commentary/" + "120.wav",
      "data/sounds/commentary/" + "118.wav",
      "data/sounds/commentary/" + "116.wav",
      "data/sounds/commentary/" + "121.wav",
      "data/sounds/commentary/" + "115.wav",
      "data/sounds/commentary/" + "122.wav"
   };
   private static final String[][] m_sFBNamesWav = new String[][]{
      {
            "data/sounds/commentary/" + "013",
            "data/sounds/commentary/" + "014",
            "data/sounds/commentary/" + "015",
            "data/sounds/commentary/" + "016",
            "data/sounds/commentary/" + "017",
            "data/sounds/commentary/" + "018",
            "data/sounds/commentary/" + "019"
      },
      {
            "data/sounds/commentary/" + "027",
            "data/sounds/commentary/" + "028",
            "data/sounds/commentary/" + "029",
            "data/sounds/commentary/" + "030",
            "data/sounds/commentary/" + "031",
            "data/sounds/commentary/" + "032",
            "data/sounds/commentary/" + "033"
      },
      {
            "data/sounds/commentary/" + "020",
            "data/sounds/commentary/" + "021",
            "data/sounds/commentary/" + "022",
            "data/sounds/commentary/" + "023",
            "data/sounds/commentary/" + "024",
            "data/sounds/commentary/" + "025",
            "data/sounds/commentary/" + "026"
      },
      {
            "data/sounds/commentary/" + "062",
            "data/sounds/commentary/" + "063",
            "data/sounds/commentary/" + "064",
            "data/sounds/commentary/" + "065",
            "data/sounds/commentary/" + "066",
            "data/sounds/commentary/" + "067",
            "data/sounds/commentary/" + "068"
      },
      {
            "data/sounds/commentary/" + "048",
            "data/sounds/commentary/" + "049",
            "data/sounds/commentary/" + "050",
            "data/sounds/commentary/" + "051",
            "data/sounds/commentary/" + "052",
            "data/sounds/commentary/" + "053",
            "data/sounds/commentary/" + "054"
      },
      {
            "data/sounds/commentary/" + "090",
            "data/sounds/commentary/" + "091",
            "data/sounds/commentary/" + "092",
            "data/sounds/commentary/" + "093",
            "data/sounds/commentary/" + "094",
            "data/sounds/commentary/" + "095",
            "data/sounds/commentary/" + "096"
      },
      {
            "data/sounds/commentary/" + "069",
            "data/sounds/commentary/" + "070",
            "data/sounds/commentary/" + "071",
            "data/sounds/commentary/" + "072",
            "data/sounds/commentary/" + "073",
            "data/sounds/commentary/" + "074",
            "data/sounds/commentary/" + "075"
      },
      {
            "data/sounds/commentary/" + "055",
            "data/sounds/commentary/" + "056",
            "data/sounds/commentary/" + "057",
            "data/sounds/commentary/" + "058",
            "data/sounds/commentary/" + "059",
            "data/sounds/commentary/" + "060",
            "data/sounds/commentary/" + "061"
      },
      {
            "data/sounds/commentary/" + "041",
            "data/sounds/commentary/" + "042",
            "data/sounds/commentary/" + "043",
            "data/sounds/commentary/" + "044",
            "data/sounds/commentary/" + "045",
            "data/sounds/commentary/" + "046",
            "data/sounds/commentary/" + "047"
      },
      {
            "data/sounds/commentary/" + "076",
            "data/sounds/commentary/" + "077",
            "data/sounds/commentary/" + "078",
            "data/sounds/commentary/" + "079",
            "data/sounds/commentary/" + "080",
            "data/sounds/commentary/" + "081",
            "data/sounds/commentary/" + "082"
      },
      {
            "data/sounds/commentary/" + "034",
            "data/sounds/commentary/" + "035",
            "data/sounds/commentary/" + "036",
            "data/sounds/commentary/" + "037",
            "data/sounds/commentary/" + "038",
            "data/sounds/commentary/" + "039",
            "data/sounds/commentary/" + "040"
      },
      {
            "data/sounds/commentary/" + "083",
            "data/sounds/commentary/" + "084",
            "data/sounds/commentary/" + "085",
            "data/sounds/commentary/" + "086",
            "data/sounds/commentary/" + "087",
            "data/sounds/commentary/" + "088",
            "data/sounds/commentary/" + "089"
      }
   };
   static final String[] m_sTeamMissWav = new String[]{
      "data/sounds/commentary/" + "231.wav",
      "data/sounds/commentary/" + "227.wav",
      "data/sounds/commentary/" + "226.wav",
      "data/sounds/commentary/" + "235.wav",
      "data/sounds/commentary/" + "233.wav",
      "data/sounds/commentary/" + "229.wav",
      "data/sounds/commentary/" + "230.wav",
      "data/sounds/commentary/" + "234.wav",
      "data/sounds/commentary/" + "228.wav",
      "data/sounds/commentary/" + "236.wav",
      "data/sounds/commentary/" + "232.wav",
      "data/sounds/commentary/" + "237.wav"
   };
   static String m_sSpecNameGK = "Goalkeeper";
   static String m_sSpecName = "Footballer";
   static String m_sMeshPath = "data/meshes/animals/";

   static void SetAppearanceForMenu(MeshObject cObject, int nAnimalType, boolean bAvailable) {
      if (bAvailable) {
         String sNewMesh = GetMeshName(nAnimalType);
         if (sNewMesh.equalsIgnoreCase(cObject.MeshName)) {
            return;
         }

         cObject.LoadMesh(sNewMesh);
         int nMaterial = GetSkinMaterial(cObject);
         String sTexName = m_sTextureNormal[nAnimalType][0];
         cObject.SetSkinElement(sTexName, nMaterial);
         int nWhistleID = cObject.GetElementID("Gwizdek_0");
         if (nWhistleID == -1) {
            Tools.CrashLogError(cObject + " whistleId = -1");
         }

         cObject.HideElement(nWhistleID);
         int nCoinId = cObject.GetElementID("Moneta_0");
         if (nCoinId == -1) {
            Tools.CrashLogError(cObject + " nCoinId = -1");
         }

         cObject.HideElement(nCoinId);
      } else if (!cObject.MeshName.equalsIgnoreCase("data/menu/Unavail.3da")) {
         cObject.LoadMesh("data/menu/Unavail.3da");
      }
   }

   static int SelectReferee(int nLeftAnimalType, int nRightAnimalType) {
      float fRnd = GameObject.RandomFloat(130.0F);
      if (fRnd > 120.0F && nLeftAnimalType != 3 && nRightAnimalType != 3) {
         return 3;
      } else if (fRnd > 100.0F && nLeftAnimalType != 1 && nRightAnimalType != 1) {
         return 1;
      } else if (fRnd > 90.0F && nLeftAnimalType != 0 && nRightAnimalType != 0) {
         return 0;
      } else if (fRnd > 80.0F && nLeftAnimalType != 2 && nRightAnimalType != 2) {
         return 2;
      } else if (fRnd > 70.0F && nLeftAnimalType != 4 && nRightAnimalType != 4) {
         return 4;
      } else if (fRnd > 60.0F && nLeftAnimalType != 5 && nRightAnimalType != 5) {
         return 5;
      } else if (fRnd > 50.0F && nLeftAnimalType != 6 && nRightAnimalType != 6) {
         return 6;
      } else if (fRnd > 40.0F && nLeftAnimalType != 7 && nRightAnimalType != 7) {
         return 7;
      } else if (fRnd > 30.0F && nLeftAnimalType != 8 && nRightAnimalType != 8) {
         return 8;
      } else if (fRnd > 20.0F && nLeftAnimalType != 9 && nRightAnimalType != 9) {
         return 9;
      } else if (fRnd > 10.0F && nLeftAnimalType != 10 && nRightAnimalType != 10) {
         return 10;
      } else {
         return fRnd > 0.0F && nLeftAnimalType != 11 && nRightAnimalType != 11 ? 11 : 1;
      }
   }

   // [mod] index valide dans les tableaux de commentaires (12 equipes d'origine)
   static int GetCommentAnimal(int nAnimal) {
      return nAnimal >= 0 && nAnimal < m_nCommentAnimal.length ? m_nCommentAnimal[nAnimal] : nAnimal;
   }

   static String GetTeamCommentWav(int nAnimal) {
      return m_sTeamCommentsWav[GetCommentAnimal(nAnimal)];
   }

   static String GetAnimalScrFile(boolean bGoalkeeper) {
      return !bGoalkeeper ? "data\\Animals_scr\\AIFootballer.scr" : "data\\Animals_scr\\AIGoalkeeper.scr";
   }

   static String GetMeshName(int nAnimal) {
      return m_sMeshPath + m_sMeshName[nAnimal];
   }

   static String GetRefereeClassName(int nAnimalType) {
      String sClassName = null;
      switch (nAnimalType) {
         case 0:
            sClassName = "AIBearReferee";
            break;
         case 1:
            sClassName = "AIBullReferee";
            break;
         case 2:
            sClassName = "AIChickenReferee";
            break;
         case 3:
            sClassName = "AIDinoReferee";
            break;
         case 4:
            sClassName = "AIFrogReferee";
            break;
         case 5:
            sClassName = "AIGiraffeReferee";
            break;
         case 6:
            sClassName = "AIGorillaReferee";
            break;
         case 7:
            sClassName = "AILionReferee";
            break;
         case 8:
            sClassName = "AIPantherReferee";
            break;
         case 9:
            sClassName = "AISharkReferee";
            break;
         case 10:
            sClassName = "AITurtleReferee";
            break;
         case 11:
            sClassName = "AIRoboReferee";
      }

      return sClassName;
   }

   static String GetClothNormalLogo(int nAnimal, int nId) {
      return m_sTextureNormalLogo[nAnimal][nId];
   }

   static int GetSkinMaterial(MeshObject cAnimal) {
      int nMaterial = 0;
      String sSkin = cAnimal.GetSkinElement(0);
      if (sSkin.endsWith("gwizdek.png")) {
         nMaterial = 1;
      }

      return nMaterial;
   }

   static String GetAnimalName(int nAnimal, boolean bGoalKeeper) {
      return !bGoalKeeper ? m_sNames[nAnimal] : m_sNamesGK[nAnimal];
   }

   static void SetAppearance(MeshObject cObject, int nAnimalType, int nClothesId, int nFBType) {
      if (nFBType != -1) {
         cObject.LoadMesh(GetMeshName(nAnimalType));
      }

      boolean bHideWhistle = true;
      boolean bHideCoin = true;
      String sTexName = "red";
      int nMaterial = GetSkinMaterial(cObject);
      switch (nFBType) {
         case -1:
            sTexName = m_sTexturePublic[nAnimalType];
            bHideWhistle = true;
            bHideCoin = true;
            break;
         case 0:
            sTexName = m_sTextureNormal[nAnimalType][nClothesId];
            bHideWhistle = true;
            bHideCoin = true;
            break;
         case 1:
            sTexName = m_sTextureNormal[nAnimalType][nClothesId];
            bHideWhistle = true;
            bHideCoin = true;
            break;
         case 2:
            sTexName = m_sTextureGoalKeeper[nAnimalType][nClothesId];
            bHideWhistle = true;
            bHideCoin = true;
            break;
         case 3:
            sTexName = m_sTextureReferee[nAnimalType];
            bHideWhistle = false;
            bHideCoin = true;
      }

      cObject.SetSkinElement(sTexName, nMaterial);
      if (bHideWhistle) {
         int nWhistleID = cObject.GetElementID("Gwizdek_0");
         if (nWhistleID == -1) {
            Tools.CrashLogError(cObject + " whistleId = -1");
         }

         cObject.HideElement(nWhistleID);
      }

      if (bHideCoin) {
         int nCoinId = cObject.GetElementID("Moneta_0");
         if (nCoinId == -1) {
            Tools.CrashLogError(cObject + " nCoinId = -1");
         }

         cObject.HideElement(nCoinId);
      }
   }

   static void SetAppearance(MeshObject cObject, int nAnimalType, int nFBType) {
      SetAppearance(cObject, nAnimalType, 0, nFBType);
   }

   static int GetClothesCount(int nAnimal) {
      return m_sTextureNormal[nAnimal].length;
   }

   static String GetFBNameWav(int nAnimalType, int nNumber) {
      if (nAnimalType != -1 && nNumber != -1) {
         return m_sFBNamesWav[GetCommentAnimal(nAnimalType)][nNumber];
      }

      GameObject.CrashLog(" ERROR: commentator.java - nAnimalType " + nAnimalType + " nNumber " + nNumber);
      return null;
   }

   static String GetClothNormal(int nAnimal, int nId) {
      return m_sTextureNormal[nAnimal][nId];
   }

   static String GetTeamNameWav(int nAnimal) {
      return m_sTeamNamesWav[GetCommentAnimal(nAnimal)];
   }
}
