import java.util.Random;

// Sert à server/test_league.py : joue une ligue solo avec mods/java/ModLeague.java et écrit sa sauvegarde
// (ModLeague.Save) après le départ puis après chaque match du joueur, une ligne d'entiers séparés par des virgules.
//   java LeagueCheck graine graineDesScores qualifiés revanche forme animalDuJoueur note0 note1 ...
// Score du joueur : deux tirages nextInt(5) d'un Random(graineDesScores) ; en playoffs, un nul donne un but de plus
// à l'équipe qui reçoit.
class LeagueCheck {
   public static void main(String[] args) {
      long nSeed = Long.parseLong(args[0]);
      Random cScores = new Random(Long.parseLong(args[1]));
      int nQualifiers = Integer.parseInt(args[2]);
      boolean bReset = Integer.parseInt(args[3]) != 0;
      float fForm = Float.parseFloat(args[4]);
      int nPlayer = Integer.parseInt(args[5]);
      int n = args.length - 6;
      int[] tAnimals = new int[n];
      float[] tRatings = new float[n];

      for (int i = 0; i < n; i++) {
         tAnimals[i] = i;
         tRatings[i] = Float.parseFloat(args[6 + i]);
      }

      ModLeague l = new ModLeague();
      l.m_cRandom = new Random(nSeed);
      l.m_nQualifiers = nQualifiers;
      l.m_bReset = bReset;
      l.m_fGoalsPerMatch = 0.2F * 2.0F * 4;
      l.Start(tAnimals, tRatings, fForm, nPlayer);
      Print(l);

      for (int k = 0; k < 200 && l.GetPlayerNext() != null; k++) {
         int gh = cScores.nextInt(5);
         int gg = cScores.nextInt(5);
         if (l.m_nPhase == ModLeague._PHASE_PLAYOFFS && gh == gg) {
            gh++;
         }

         l.Report(gh, gg);
         Print(l);
      }
   }

   static void Print(ModLeague l) {
      int[] t = l.Save();
      StringBuffer sb = new StringBuffer();

      for (int i = 0; i < t.length; i++) {
         if (i > 0) {
            sb.append(',');
         }

         sb.append(t[i]);
      }

      System.out.println(sb.toString());
   }
}
