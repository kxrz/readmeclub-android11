# Publishing a version

## Once: the signing key

Every distributed version must be signed with **the same key**. If it is lost, no update
can ever install over the existing app: every user would have to uninstall (losing their
data) and reinstall.

1. On a computer with Java (`brew install openjdk@17` on a Mac, then call
   `$(brew --prefix openjdk@17)/bin/keytool`):

   ```sh
   keytool -genkeypair -v -keystore readmeclub-release.jks -alias readmeclub \
     -keyalg RSA -keysize 4096 -validity 36500
   ```

   Pick a strong password (with the default PKCS12 format, the keystore and the key share it).

2. **Back up** `readmeclub-release.jks` and its password in a password manager, plus a
   second copy elsewhere. Never in a git repository.

3. In GitHub → `kxrz/readmeclub-android11` → Settings → Secrets and variables → Actions,
   create four **repository** secrets:

   | Secret | Value |
   |---|---|
   | `RELEASE_KEYSTORE_BASE64` | output of `base64 -i readmeclub-release.jks` |
   | `RELEASE_KEYSTORE_PASSWORD` | the keystore password |
   | `RELEASE_KEY_ALIAS` | `readmeclub` |
   | `RELEASE_KEY_PASSWORD` | the key password (the same one) |

## Every version

1. In `app/build.gradle.kts`, bump `versionCode` (+1) and `versionName`.
2. Commit, then tag and push:

   ```sh
   git tag v1.0.0 && git push origin v1.0.0
   ```

3. CI builds the signed, minified APK and creates a GitHub Release with
   `readmeclub.apk`, `readmeclub-1.0.0.apk` and `manifest.json`.

Nothing else to do. At launch the app reads `releases/latest/download/manifest.json`
(version, APK URL of that release, SHA-256). When a newer version exists, the tab reads
"Settings •" and About offers "Update to x.y.z": download, SHA-256 check, then Android's
confirmation screen. The first time, Android asks to allow readme.club to install apps.
Only release builds update themselves (the signing key must match).

A tag with a suffix (`v1.0.0-rc1`) is published as a *pre-release*: it is never "latest",
so it is never offered as an update.

Permanent download link for the latest version:
`https://github.com/kxrz/readmeclub-android11/releases/latest/download/readmeclub.apk`

## Dev and release builds on the same reader

Dev builds (`app-debug`, built by CI on every push) install under their own id
(`club.readme.android.debug`) and are called "readme.club dev": they live next to the
distributed app (`club.readme.android`) without any signature clash.
