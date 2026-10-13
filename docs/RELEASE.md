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

1. In `app/build.gradle.kts`, bump `versionCode` (+1) and `versionName`, and in
   `CHANGELOG.md` replace "Unreleased" with today's date in that version's section.
2. Commit and push to `main`, then start the release, either way:
   - **From GitHub** (no computer needed): Actions → Android → *Run workflow*, branch `main`,
     version `1.0.0`. The job creates the tag `v1.0.0` on that commit.
   - **From a computer**: `git tag v1.0.0 && git push origin v1.0.0`.

   Both check that the version matches `versionName`. A manual run on a tag that already
   exists rebuilds that release's files.

3. CI builds the signed, minified APK and creates a GitHub Release with
   `readmeclub.apk`, `readmeclub-1.0.0.apk` and `manifest.json`, using the version's
   `CHANGELOG.md` section as release notes. A tag without a section fails the release.

The same files are also kept on the run (Actions › the run › Artifacts › `release-v1.0.0`):
if GitHub ever refuses the release step, attach them to the release by hand.

4. In `README.md`, update the release and APK size badges (static, so they always show).
5. Add `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt` (500 characters at
   most): app stores that read the repository (IzzyOnDroid, F-Droid) show it as the
   version's notes. The store listing itself (title, descriptions, icon, screenshots) lives
   next to it.

Nothing else to do. At launch the app reads `releases/latest/download/manifest.json`
(version, APK URL of that release, SHA-256). When a newer version exists, the tab reads
"Settings •" and About offers "Update to x.y.z": download, SHA-256 check, then Android's
confirmation screen. The first time, Android asks to allow readme.club to install apps.
Only release builds update themselves (the signing key must match).

A tag with a suffix (`v1.0.0-rc1`) is published as a *pre-release*: it is never "latest",
so it is never offered as an update.

Permanent download link for the latest version:
`https://github.com/kxrz/readmeclub-android11/releases/latest/download/readmeclub.apk`

## Google Play

One codebase, two builds of the same app (`club.readme.android`, same version, same key):

| | GitHub release (`assembleRelease`) | Google Play (`bundlePlay`) |
|---|---|---|
| Format | APK, on GitHub Releases and the website | App bundle (`.aab`), uploaded to the Play Console |
| Updates | The app updates itself from GitHub Releases | Google Play updates it; no update check, no install permission |

Every release run builds both. The signed bundle, `readmeclub-<version>-play.aab`, is only in
the run's artifact (Actions › the run › Artifacts › `release-v<version>`), not on the GitHub
release.

### Once: create the app in the Play Console

1. Play Console → *Create app*: name "readme.club", app, free.
2. **App signing: use our own key**, so the Play and GitHub builds share a signature and a
   user can move from one to the other without uninstalling. On the first release, in
   *App integrity* → *App signing*, pick *Use a different key* → *Export and upload a key
   from Java keystore*, download `pepk.jar` and the encryption key it gives, then:

   ```sh
   java -jar pepk.jar --keystore=readmeclub-release.jks --alias=readmeclub \
     --output=readmeclub-play-key.zip --include-cert --rsa-aes-encryption \
     --encryption-key-path=encryption_public_key.pem
   ```

   and upload `readmeclub-play-key.zip`. The bundles CI signs with this key are then
   accepted as uploads. Never let Google generate the key: Play installs would then refuse
   GitHub updates and the reverse.
3. *App content*: privacy policy URL (`https://www.readme.club/privacy`), ads (none), data safety (no tracking; when a member
   links the app: member number and email, sent to readme.club only, over HTTPS),
   content rating questionnaire, target audience (not children).
4. *Store listing*: title, descriptions, icon and screenshots are in
   `fastlane/metadata/android/en-US`. Play also needs a 1024 × 500 feature graphic.
5. **Closed test first.** A personal developer account must run a closed test with at least
   12 testers opted in for 14 days in a row before it can publish to production. Create the
   closed track, add the testers' Google accounts (or a Google Group), share the opt-in link.

### Every version

After the release run (see above), download the run's artifact and upload
`readmeclub-<version>-play.aab` to the Play Console track (closed test, then production), with
the version's `fastlane/.../changelogs/<versionCode>.txt` as release notes. Play refuses a
`versionCode` it has already seen: the +1 of every version covers it.

Play's target API floor rises every August (API 36 in 2026): bump `targetSdk` before then.

## Dev and release builds on the same reader

Dev builds (`app-debug`, built by CI on every push) install under their own id
(`club.readme.android.debug`) and are called "readme.club dev": they live next to the
distributed app (`club.readme.android`) without any signature clash.

## Stats

`scripts/stats.sh` prints, per release, APK downloads (installs and updates) and
`manifest.json` downloads (app launches, since every launch checks for updates). These are
volumes, not unique users; the app sends no identifier.
