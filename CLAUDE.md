# readmeclub-android11

App Android de readme.club pour la Xteink S4 (Android 11, e-ink 4,3"). Présentation : README.md ; particularités de l'appareil : docs/DEVICE.md.

## Règles non négociables
- minSdk 30. Aucune dépendance aux Google Play Services.
- Zéro animation, zéro couleur porteuse de sens, zéro défilement continu : tout est paginé.
- L'UI lit uniquement le cache local ; seul le package `sync` écrit depuis le réseau.
- Pas de framework d'injection : AppContainer dans Application.
- Toute nouvelle dépendance doit être justifiée dans la PR.
- Aucun lien vers l'extérieur : la S4 n'a pas de navigateur. Seuls les liens vers des articles et guides readme.club restent actifs, et ils s'ouvrent dans l'app.

## Commandes
- Build : ./gradlew assembleDebug
- Tests : ./gradlew testDebugUnitTest
- Installer sur le S4 : adb install -r app/build/outputs/apk/debug/app-debug.apk

## Vérification
- Le moteur `reader` a des tests unitaires (pagination de blocs connus, cas limites : image plus haute que l'écran, paragraphe sur 3 pages).
- Avant de dire "fini" : build vert (debug et release) et tests verts sur GitHub Actions, puis test sur l'appareil.

## Build
- L'environnement cloud n'a ni SDK Android ni accès à Google Maven : le build se vérifie sur GitHub Actions (`.github/workflows/android.yml`).
- Chaque push produit l'artefact `app-debug` (APK signé avec `app/debug.keystore`, partagé pour que les installs successives passent).
- Un tag `v*` produit l'APK release signé et le publie dans GitHub Releases : procédure complète dans docs/RELEASE.md.
- Les builds debug ont leur propre identifiant (`club.readme.android.debug`, « readme.club dev ») et cohabitent avec la release.
- Stack volontairement minimale : vues Android natives, Kotlin, zéro dépendance AndroidX ; réseau via `HttpURLConnection` + `org.json`.
