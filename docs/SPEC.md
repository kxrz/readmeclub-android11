# ReadmeClub pour Xteink S4 — Spécification de l'app Android

Oct 7, 2026 · @Florent Bertiaux

## Contexte et périmètre

La v1 est une app Android native, hors ligne d'abord, qui met trois contenus de readme.club sur la Xteink S4 : les news, les guides et les wallpapers. Tout passe par l'API du site. L'app doit être agréable à lire sur un écran e-ink de 4,3 pouces et ne rien coûter en maintenance éditoriale : ce qui est publié sur le site arrive dans l'app sans travail supplémentaire.

**Objectifs**

- Lire les articles et les guides confortablement sur l'appareil, connecté ou non.
- Trouver et installer un wallpaper au bon format en moins de 3 taps.
- Servir de vitrine readme.club auprès de Xteink (prototype S4) et des membres.

**Dans la v1**

- News : liste, lecture paginée, cache hors ligne des N derniers articles.
- Guides : liste filtrée par appareil, lecture paginée, téléchargement pour lecture hors ligne, progression.
- Wallpapers : grille filtrée sur la résolution de l'appareil, aperçu, enregistrement, définition comme fond.
- Réglages : taille de police, marges, fréquence de rafraîchissement complet, gestion du stockage.

**Hors v1**

- Compte membre, favoris synchronisés, commentaires.
- Firmware, hardware, ressources, fonts.
- Notifications push, Play Store.
- Support d'autres appareils que le S4 (l'architecture le permet, la v1 ne le teste pas).

## Appareil cible et contraintes e-ink

La cible unique de la v1 est la Xteink S4 : Android 11 (API 30), écran e-ink 4,3 pouces, batterie de 1 400 mAh, et un bouton capacitif sous l'écran au lieu des boutons physiques des X3/X4 ([Notebookcheck](https://www.notebookcheck.org/El-lector-electronico-compacto-Xteink-S4-pone-Android-en-la-parte-trasera-de-su-iPhone.1289443.0.html)). La résolution, la densité et la présence des Google Play Services restent à relever sur le prototype (voir Questions ouvertes).

| Contrainte | Conséquence pour l'app |
| --- | --- |
| Rafraîchissement lent, ghosting | Aucune animation ni transition ; pagination au lieu du défilement ; rafraîchissement complet forcé toutes les N pages (réglable, défaut 6) |
| Niveaux de gris (16 en général) | Noir sur blanc pur, aucune couleur porteuse de sens ; images converties en niveaux de gris tramés côté serveur |
| Petit écran 4,3" | Une colonne, cibles tactiles ≥ 48 dp, police de lecture 16–24 sp réglable |
| Bouton capacitif unique | Appui court = page suivante ; appui long = menu ; tap bords gauche/droit de l'écran = page précédente/suivante |
| Batterie 1 400 mAh | Pas de synchro en tâche de fond continue ; une synchro au lancement + une périodique (WorkManager, ≥ 6 h, Wi-Fi uniquement) |
| Android 11, connexion intermittente | minSdk 30 ; aucune dépendance aux Google Play Services ; tout fonctionne hors ligne une fois synchronisé |

## Fonctionnalités

Trois onglets (News, Guides, Wallpapers) et un écran Réglages. Chaque fonctionnalité a ses critères d'acceptation, repris dans le plan de livraison.

### News

- Liste des articles, du plus récent au plus ancien : titre, catégorie, auteur, date. Pas de vignette par défaut ; option « afficher les images » dans les réglages.
- Lecture paginée : le contenu est découpé en pages à la taille de l'écran. Indicateur « page 3/12 » en pied de page.
- Cache hors ligne des 30 derniers articles, texte + images converties. Au-delà, l'article est chargé à la demande.
- Lien « Ouvrir sur readme.club » en fin d'article (navigateur système).

### Guides

- Liste filtrée par appareil (défaut : S4 si des guides existent, sinon « Tous ») puis par marque. Badge de niveau (beginner, intermediate).
- Un guide = une suite de sections ; sommaire accessible par appui long.
- Bouton « Télécharger » par guide, et « Tout télécharger pour cet appareil » : le guide devient lisible hors ligne avec ses images.
- Progression mémorisée (section + page) ; reprise au dernier endroit lu.
- Mise à jour : si la version serveur d'un guide téléchargé change, badge « Mis à jour » et re-téléchargement à la prochaine synchro Wi-Fi.

### Wallpapers

- Grille 2 colonnes de vignettes, filtrée par défaut sur l'orientation et la résolution de l'appareil ; filtre « Toutes résolutions » disponible.
- Écran détail : aperçu plein écran, titre, auteur, licence.
- « Enregistrer » : fichier dans Pictures/ReadmeClub via MediaStore (aucune permission requise sur Android 11).
- « Définir comme fond » : WallpaperManager (écran d'accueil et verrouillage). Si le S4 gère son écran de veille autrement, le bouton est masqué et l'enregistrement suffit.
- Pagination de la grille par pages (« Suivant »), pas par défilement infini.

### Réglages

- Police (2 choix : sérif / sans-sérif), taille (5 crans), marges (3 crans), interligne (3 crans).
- Rafraîchissement complet : toutes les 1, 3, 6 ou 10 pages.
- Synchro : Wi-Fi uniquement (oui/non), dernière synchro, bouton « Synchroniser maintenant ».
- Stockage : espace utilisé par News / Guides / Wallpapers, bouton « Vider » par catégorie.
- À propos : version, lien vers readme.club, vérification de mise à jour.

## Écrans et principes UI

Neuf écrans, une barre d'onglets fixe en bas, et un lecteur commun aux news et aux guides. L'identité visuelle reprend le [styleguide readme.club](https://www.readme.club/brand), ramené au noir et blanc.

| Écran | Contenu | Interactions |
| --- | --- | --- |
| Accueil / News | Liste paginée des articles (8 par page) | Tap = lire ; bouton = page suivante |
| Lecteur | Article ou section de guide, paginé | Bouton ou bord droit = suivant ; bord gauche = précédent ; appui long = menu (sommaire, taille, ouvrir sur le site) |
| Guides | Filtres appareil / marque + liste | Tap = détail du guide |
| Détail guide | Description, sommaire, état (en ligne / téléchargé / mis à jour) | Lire, Télécharger, Supprimer |
| Wallpapers | Grille 2 × 3 par page + filtre | Tap = détail |
| Détail wallpaper | Aperçu plein écran + métadonnées | Enregistrer, Définir comme fond |
| Réglages | Voir Fonctionnalités | — |
| Première ouverture | Choix de l'appareil (pré-rempli S4), synchro initiale | Continuer |
| Hors ligne | Bandeau discret « Hors ligne — contenu enregistré » | Réessayer |

**Règles UI à faire respecter partout**

- Thème unique clair : fond #FFFFFF, texte #000000, gris uniquement pour les séparateurs. Pas de mode sombre en v1.
- `android:windowAnimationStyle` à null, animations Compose désactivées, ripple remplacé par une inversion noir/blanc de 100 ms.
- Aucun spinner animé : texte « Chargement… » statique.
- Pagination partout : jamais de LazyColumn qui défile en continu ; les listes sont découpées en pages de hauteur écran.
- Hiérarchie par la taille et la graisse de la police, pas par la couleur.
- Rafraîchissement complet déclenché après N changements de page et à chaque ouverture d'écran plein (voir Architecture).

## Contrat d'API

L'app ne parle qu'à une couche dédiée, `https://www.readme.club/api/app/v1`, qui agrège les sources existantes et renvoie un contenu déjà prêt pour l'e-ink. D'après les URLs publiques, les articles et guides semblent venir de write.readme.club/api et les wallpapers du stockage api.readme.club ; cette couche isole l'app de ces choix. Pas d'authentification en v1 ; un en-tête `X-App-Version` est envoyé à chaque requête.

| Méthode et chemin | Rôle | Paramètres |
| --- | --- | --- |
| `GET /manifest` | Version minimale, dernière version et URL de l'APK, profils d'appareils | — |
| `GET /news` | Liste paginée des articles | `page`, `limit` (≤ 30), `since` (ISO 8601) |
| `GET /news/{slug}` | Article complet en blocs | — |
| `GET /guides` | Liste des guides | `device`, `brand`, `page`, `limit`, `since` |
| `GET /guides/{slug}` | Guide complet : sections en blocs + `version` | — |
| `GET /wallpapers` | Liste paginée | `width`, `height`, `orientation`, `page`, `limit` |
| `GET /wallpapers/{id}` | Détail + URL du fichier adapté à l'appareil | `width`, `height` |
| `GET /img` | Image convertie pour l'e-ink | `src`, `w`, `grey=1`, `dither=1` |

**Règles communes**

- Réponses JSON, `ETag` + `Cache-Control: public, max-age=300` ; l'app envoie `If-None-Match` et gère le 304.
- Pagination : `{ "items": [...], "page": 1, "pageCount": 4, "total": 31 }`.
- `since` renvoie aussi les suppressions : `"deleted": ["slug-a"]`, pour purger le cache local.
- Toutes les URLs d'images d'un contenu pointent déjà vers `/img` avec la largeur de l'appareil.

**Format du contenu : des blocs, pas du HTML.** Le serveur convertit le rich text du CMS en une liste de blocs typés. L'app peut ainsi mesurer et paginer elle-même, sans WebView. Types v1 : `heading` (niveau 2–4), `paragraph` (avec spans `bold`, `italic`, `code`, `link`), `list` (ordonnée ou non), `image` (url, alt, largeur, hauteur), `quote`, `code`, `callout` (info, warning), `divider`. Un type inconnu est ignoré par l'app.

```json
{
  "slug": "xteink-how-to-flash",
  "title": "How to Flash Xteink Firmware",
  "brand": "xteink",
  "devices": ["x3", "x4", "x4pro"],
  "level": "intermediate",
  "summary": "A practical, safety-first guide…",
  "updatedAt": "2026-10-01T09:00:00Z",
  "version": "a1b2c3",
  "url": "https://www.readme.club/guide/xteink-how-to-flash",
  "sections": [
    {
      "id": "backup",
      "title": "Back up first",
      "blocks": [
        { "type": "paragraph", "spans": [{ "text": "Before flashing, " }, { "text": "always", "bold": true }, { "text": " back up." }] },
        { "type": "image", "url": "https://www.readme.club/api/app/v1/img?src=…&w=480&grey=1&dither=1", "alt": "Flash tool", "width": 480, "height": 320 },
        { "type": "callout", "tone": "warning", "spans": [{ "text": "Do not unplug during the flash." }] }
      ]
    }
  ]
}
```

Un article de news a la même forme, avec `category`, `author`, `publishedAt` et un seul tableau `blocks` au lieu de `sections`. Un wallpaper renvoie `id`, `title`, `author`, `license`, `width`, `height`, `orientation`, `thumbUrl`, `fileUrl` et `fileSize` (octets).

**Conversion des images (côté serveur)** : redimensionnement à la largeur demandée, passage en niveaux de gris, tramage Floyd–Steinberg sur 16 niveaux, export PNG pour les wallpapers et JPEG qualité 80 pour les images d'articles. Résultat mis en cache par le CDN.

## Architecture Android

Un seul module Gradle, Kotlin + Jetpack Compose, un cache Room qui est la source de vérité de l'UI : l'écran lit toujours la base locale, la synchro la met à jour. Pas de framework d'injection : un `AppContainer` créé dans `Application` suffit à cette taille.

&#91;embedded content: architecture de l'app · 7 briques\]

L'UI ne lit que Room ; la synchro est le seul chemin vers l'API, qui isole l'app du CMS et du stockage.

| Brique | Choix | Raison |
| --- | --- | --- |
| Langage / UI | Kotlin 2.x, Jetpack Compose, Material 3 dépouillé | Bien maîtrisé par Claude Code, thème facile à verrouiller en noir et blanc |
| SDK | minSdk 30, targetSdk 30, compileSdk récent | Android 11 sur le S4 ; sideload, donc pas d'exigence Play Store sur le targetSdk |
| Réseau | OkHttp + Retrofit + kotlinx.serialization | Cache HTTP et ETag gérés par OkHttp |
| Base locale | Room | Articles, guides, sections, blocs (JSON), progression, wallpapers enregistrés |
| Images | Coil, cache disque 200 Mo | Images déjà converties côté serveur, donc aucun traitement sur l'appareil |
| Tâches de fond | WorkManager | Synchro périodique (≥ 6 h, Wi-Fi, batterie non faible) et téléchargements de guides |
| Réglages | DataStore Preferences | Police, marges, rafraîchissement, synchro |

**Organisation des packages** : `data/api`, `data/db`, `data/repo`, `sync`, `reader` (moteur de pagination + rendu des blocs), `ui/news`, `ui/guides`, `ui/wallpapers`, `ui/settings`, `eink` (rafraîchissement et entrées du bouton).

**Moteur de pagination (`reader`)** : il prend une liste de blocs, la taille utile de l'écran et les réglages typographiques, mesure chaque bloc avec `TextMeasurer` et remplit les pages. Un paragraphe trop long est coupé à la ligne ; une image trop haute est réduite pour tenir sur une page. Le résultat (liste de pages, chacune = liste de fragments de blocs) est mis en cache en mémoire par contenu + réglages. C'est la pièce la plus délicate : elle a ses propres tests unitaires.

**Rafraîchissement e-ink (`eink`)** : Android n'expose pas d'API standard. Ordre de tentative :

1. API ou broadcast constructeur, si Xteink en documente un (à demander).
2. Repli universel : afficher une frame entièrement noire puis blanche (\~120 ms au total) avant la page suivante, ce qui force un rafraîchissement complet sur la plupart des contrôleurs.

L'interface `EinkRefresher` cache le choix, pour pouvoir changer d'implémentation sans toucher l'UI.

**Entrées** : le bouton capacitif est intercepté via `onKeyEvent` ; son keycode exact est à relever sur le prototype avec un écran de diagnostic caché (5 taps sur la version dans À propos).

## Distribution et mises à jour

L'APK est distribué en sideload depuis GitHub Releases et une page readme.club/app ; pas de Play Store en v1.

- **Signature** : une clé de release unique, conservée hors du dépôt (secret CI). La perdre empêche toute mise à jour des installations existantes.
- **Build** : GitHub Actions construit l'APK signé à chaque tag `v*`, le publie dans Releases et met à jour `/manifest` (`latestVersion`, `apkUrl`, `minVersion`, `changelog`).
- **Mise à jour dans l'app** : au lancement, lecture de `/manifest`. Version plus récente : bandeau « Mise à jour disponible » qui télécharge l'APK et lance l'installateur système (permission `REQUEST_INSTALL_PACKAGES`). Version sous `minVersion` : écran bloquant avec le même bouton.
- **Installation** : la page readme.club/app explique l'activation des sources inconnues sur le S4, avec captures.
- **Mesure** : aucun SDK tiers. Le seul signal est le nombre de téléchargements de l'APK et les appels à `/manifest` côté serveur (comptés par version d'app, sans identifiant d'appareil).
- **Crash** : journal local des exceptions, exportable depuis l'écran de diagnostic pour un rapport manuel.

## Plan de livraison

Cinq sprints courts, chacun livrable et testable sur le S4. On commence par l'API et le lecteur, car tout le reste en dépend.

### Sprint 0 — Fondations

- [ ] Projet Android créé (minSdk 30), thème noir et blanc, animations coupées, barre d'onglets.
- [ ] Écran de diagnostic : résolution, densité, keycode du bouton, test de rafraîchissement noir/blanc.
- [ ] Endpoint `/manifest` en ligne ; l'app l'affiche dans À propos.
- [ ] APK signé produit par la CI et installé sur le S4.

### Sprint 1 — News

- [ ] Endpoints `/news` et `/news/{slug}` en ligne, conversion rich text → blocs, `/img` opérationnel.
- [ ] Liste paginée et lecteur paginé ; bouton capacitif et bords d'écran tournent les pages.
- [ ] Rafraîchissement complet toutes les N pages, N réglable.
- [ ] Mode avion : les 30 derniers articles restent lisibles avec leurs images.

### Sprint 2 — Guides

- [ ] Endpoints `/guides` et `/guides/{slug}` avec sections et `version`.
- [ ] Filtres appareil et marque ; détail du guide ; sommaire par appui long.
- [ ] Téléchargement d'un guide et de tous les guides d'un appareil ; lecture hors ligne vérifiée.
- [ ] Progression mémorisée ; un guide modifié sur le site affiche « Mis à jour » après synchro.

### Sprint 3 — Wallpapers

- [ ] Endpoints `/wallpapers` filtrés par résolution, fichiers convertis à la taille exacte du S4.
- [ ] Grille paginée, détail, Enregistrer (visible dans la galerie du S4).
- [ ] Définir comme fond testé sur le S4 ; bouton masqué si sans effet.

### Sprint 4 — Finitions et sortie

- [ ] Réglages complets, gestion du stockage, synchro périodique WorkManager.
- [ ] Mise à jour in-app via `/manifest` testée de bout en bout (v1.0.0 → v1.0.1).
- [ ] Page readme.club/app avec procédure d'installation.
- [ ] Une semaine d'usage réel sur le S4 sans crash ; batterie : synchro seule < 2 % par jour.

## Mode d'emploi pour Claude Code

Deux chantiers séparés : l'API dans le dépôt existant de readme.club, l'app dans un nouveau dépôt `readmeclub-android`. Exporter ce doc en Markdown dans `docs/SPEC.md` de chaque dépôt, et poser le `CLAUDE.md` ci-dessous à la racine du dépôt Android.

```markdown
# readmeclub-android

App Android de readme.club pour la Xteink S4 (Android 11, e-ink 4,3"). Spec complète : docs/SPEC.md.

## Règles non négociables
- minSdk 30. Aucune dépendance aux Google Play Services.
- Zéro animation, zéro couleur porteuse de sens, zéro défilement continu : tout est paginé.
- L'UI lit uniquement Room ; seul le package `sync` écrit depuis le réseau.
- Pas de framework d'injection : AppContainer dans Application.
- Toute nouvelle dépendance doit être justifiée dans la PR.

## Commandes
- Build : ./gradlew assembleDebug
- Tests : ./gradlew testDebugUnitTest
- Installer sur le S4 : adb install -r app/build/outputs/apk/debug/app-debug.apk

## Vérification
- Le moteur `reader` a des tests unitaires (pagination de blocs connus, cas limites : image plus haute que l'écran, paragraphe sur 3 pages).
- Chaque écran a une capture Compose de référence à la résolution du S4.
- Avant de dire "fini" : build vert, tests verts, critères du sprint cochés dans docs/SPEC.md.
```

**Ordre de travail conseillé**

1. Côté site, demander à Claude Code d'implémenter `/api/app/v1` (manifest, news, `/img`) avec des tests sur la conversion rich text → blocs, à partir de la section Contrat d'API.
2. Côté app, lancer le Sprint 0 en mode plan : Claude Code propose le squelette, tu valides, puis il code.
3. Un sprint par session, en collant ses critères d'acceptation dans la demande ; lui faire cocher les cases du `SPEC.md` en fin de session.
4. Après chaque sprint, installer sur le S4 et noter les écarts dans une issue avant de passer au suivant.

**Émulateur** : créer un AVD Android 11 à la résolution et à la densité du S4 pour le développement courant ; le test final se fait toujours sur l'appareil, car l'émulateur ne reproduit ni le ghosting ni la latence e-ink.

## Questions ouvertes

- [ ] Résolution, densité et orientation native de l'écran du S4 (à lire dans l'écran de diagnostic).
- [ ] Keycode du bouton capacitif, et gestion de l'appui long par le système.
- [ ] Xteink fournit-il une API ou un broadcast de rafraîchissement complet ? À demander dans le cadre du partenariat.
- [ ] Le S4 utilise-t-il le fond d'écran Android pour sa mise en veille, ou une image dédiée ?
- [ ] Le rich text des articles et des guides est-il au même format dans le CMS (un seul convertisseur en blocs) ?
- [ ] Faut-il des guides écrits pour le S4 avant la sortie, pour que le filtre par défaut ne soit pas vide ?
- [ ] Langue de l'interface : anglais seul (comme le site) ou anglais + français ?
- [ ] Nom de l'app et icône : « readme.club » ou « Readme Club for S4 » ?
