# Publier une version de l'app

## Une seule fois : la clé de signature

Toutes les versions distribuées doivent être signées avec **la même clé**. Si elle est
perdue, plus aucune mise à jour ne pourra s'installer par-dessus l'app existante : chaque
utilisateur devra désinstaller (et perdre ses données) puis réinstaller.

1. Sur le Mac (`keytool` vient avec Java : `brew install openjdk` s'il manque) :

   ```sh
   keytool -genkeypair -v -keystore readmeclub-release.jks -alias readmeclub \
     -keyalg RSA -keysize 4096 -validity 36500
   ```

   Choisir un mot de passe solide (le même pour le keystore et la clé, c'est plus simple).

2. **Sauvegarder** `readmeclub-release.jks` et le mot de passe dans le gestionnaire de mots
   de passe, et une seconde copie ailleurs. Jamais dans un dépôt git.

3. Dans GitHub → `kxrz/readmeclub-android11` → Settings → Secrets and variables → Actions,
   créer quatre secrets :

   | Secret | Valeur |
   |---|---|
   | `RELEASE_KEYSTORE_BASE64` | sortie de `base64 -i readmeclub-release.jks` |
   | `RELEASE_KEYSTORE_PASSWORD` | le mot de passe du keystore |
   | `RELEASE_KEY_ALIAS` | `readmeclub` |
   | `RELEASE_KEY_PASSWORD` | le mot de passe de la clé |

## À chaque version

1. Dans `app/build.gradle.kts`, augmenter `versionCode` (+1) et `versionName`.
2. Commit, puis tag et push :

   ```sh
   git tag v1.0.0 && git push origin v1.0.0
   ```

3. La CI construit l'APK signé et minifié, et crée une GitHub Release avec
   `readmeclub.apk`, `readmeclub-1.0.0.apk` et `manifest.json`.

Rien d'autre à faire : l'APK est distribué par GitHub Releases, et l'écran About de
l'app lit `releases/latest/download/manifest.json` pour signaler une mise à jour. Un tag
avec suffixe (`v1.0.0-rc1`) est publié en *pre-release* : il n'est pas « latest », donc
jamais proposé comme mise à jour.

Lien de téléchargement permanent de la dernière version (dépôt public) :
`https://github.com/kxrz/readmeclub-android11/releases/latest/download/readmeclub.apk`

## Builds de dev et release sur la même liseuse

Les builds de dev (`app-debug`, CI à chaque push) s'installent sous un autre identifiant
(`club.readme.android.debug`) et s'appellent « readme.club dev » : ils cohabitent avec
l'app distribuée (`club.readme.android`) sans conflit de signature.
