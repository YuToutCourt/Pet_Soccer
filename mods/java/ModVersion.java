// [mod] Version du mod montrée au menu principal (MenuMain), écrite par mods/package.ps1 dans le
// mod.txt du paquet, et comparée par le lanceur (mods/installer/update.ps1) au tag de la dernière release GitHub.
// À augmenter à chaque nouveau paquet donné aux joueurs. _MOD_VERSION (ModNet) est autre chose : la version
// du jeu en ligne, qui ne change que quand les paquets réseau ou les règles du match changent.
public class ModVersion {
   static final String RELEASE = "1.2";

   static String GetText() {
      return "version " + RELEASE;
   }
}
