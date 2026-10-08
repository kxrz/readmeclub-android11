# readmeclub-android11

App Android de readme.club pour la Xteink S4 (Android 11, e-ink 4,3"). Spec complète : docs/SPEC.md.

## Règles non négociables
- minSdk 30. Aucune dépendance aux Google Play Services.
- Zéro animation, zéro couleur porteuse de sens, zéro défilement continu : tout est paginé.
- L'UI lit uniquement le cache local ; seul le package `sync` écrit depuis le réseau.
- Pas de framework d'injection : AppContainer dans Application.
- Toute nouvelle dépendance doit être justifiée dans la PR.

## Commandes
- Build : ./gradlew assembleDebug
- Tests : ./gradlew testDebugUnitTest
- Installer sur le S4 : adb install -r app/build/outputs/apk/debug/app-debug.apk

## Vérification
- Le moteur `reader` a des tests unitaires (pagination de blocs connus, cas limites : image plus haute que l'écran, paragraphe sur 3 pages).
- Avant de dire "fini" : build vert, tests verts, critères du sprint cochés dans docs/SPEC.md.
