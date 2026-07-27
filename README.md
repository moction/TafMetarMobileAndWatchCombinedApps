# TAF/METAR — application mobile + Wear OS

Consultation des bulletins météo aéronautiques **METAR** et **TAF** sur téléphone Android et
montre Wear OS.

L'app **mobile** est seule responsable des appels réseau vers `aviationweather.gov` ; l'app
**Wear OS** ne fait aucun accès réseau et se contente de se synchroniser avec le téléphone via le
**Wearable Data Layer API** (`DataClient` / `MessageClient` / `NodeClient`).

## Structure

```
├── shared/   Modèles de données + contrat Data Layer (Kotlin pur, sans dépendance Android)
├── mobile/   App téléphone : réseau (Retrofit), WorkManager périodique, widget, push vers la montre
└── wear/     App montre : lecture d'un cache local, UI Wear Compose, tuile
```

Le module `shared` ne contient volontairement aucune dépendance Android, afin de compiler
identiquement des deux côtés.

## Fonctionnalités

**Téléphone**
- Liste des stations suivies (codes OACI), ajout et suppression — UI Jetpack Compose / Material 3
- Récupération des METAR et TAF depuis l'API publique NOAA (gratuite, sans clé)
- Rafraîchissement automatique toutes les 15 min via `WorkManager`, replanifié au redémarrage
  de l'appareil (`BootReceiver`)
- Widget d'écran d'accueil affichant le dernier METAR de la première station suivie

**Montre**
- Pager horizontal, une page par station favorite (`StationPagerScreen`)
- Détail METAR/TAF par station, avec bouton de rafraîchissement délégué au téléphone
- Tuile Wear OS (protolayout) affichant le METAR de la première station sans ouvrir l'app
- Fonctionne hors connexion : tout est lu depuis le cache local `DataStore`

## Flux de données

1. `RefreshWorker` (mobile) interroge `aviationweather.gov` toutes les 15 minutes pour les stations
   favorites (`FavoritesStore`, source de vérité côté téléphone).
2. Chaque METAR/TAF est poussé vers la montre par `WatchSyncManager` — un `DataItem` par station
   (`/metar/{ICAO}`, `/taf/{ICAO}`) — et le METAR est aussi conservé localement dans `ReportStore`
   pour alimenter le widget, qui ne peut pas déclencher d'appel réseau à chaque redessin.
3. Côté montre, `WatchDataListenerService` reçoit ces `DataItem` et les écrit dans `WatchCacheStore`.
   Au lancement de l'app, `DataItemSync.syncAll()` complète cette écoute passive par un pull actif
   de ce que le Data Layer local connaît déjà.
4. L'UI Wear observe ce cache via des `Flow` : affichage instantané, sans attente réseau.
5. « Rafraîchir » sur la montre envoie un `Message` (`/request-refresh`) au téléphone, que
   `PhoneMessageListenerService` traite en refaisant l'appel réseau puis en repoussant le résultat
   par le mécanisme de l'étape 2. Si aucun nœud n'est joignable, la montre l'indique plutôt que de
   rester en attente.
6. Toute modification des favoris est propagée à la montre (`DataItem` `/favorites`), et les
   `DataItem` des stations retirées sont supprimés — sans quoi ils persisteraient indéfiniment et
   seraient réimportés à chaque `syncAll`.

Seul le **texte brut** du bulletin est transporté : la montre l'affiche tel quel, les champs
décodés (vent, visibilité, température, QNH) n'étaient lus par personne. Seul l'horodatage est
conservé en plus, comme unique moyen programmatique de détecter une donnée périmée.

## Contrainte d'appairage importante

Les modules `:mobile` et `:wear` déclarent **le même `applicationId`** (`com.example.tafmetar`) et
doivent être signés avec **la même clé**. Le Data Layer route les `DataItem`/`Message` par le couple
(applicationId, signature) : si l'un des deux diffère, Play Services transporte les octets mais ne
trouve aucun destinataire et abandonne silencieusement — aucune erreur visible, simplement rien qui
n'arrive. Les `namespace` Gradle, eux, restent distincts (ils ne servent qu'aux classes générées).

## Prérequis et build

- Android Studio Koala ou plus récent, JDK 17+ (le daemon Gradle utilise un toolchain JDK 21)
- Gradle 9.0.0 (wrapper fourni), AGP 8.5.2, Kotlin 1.9.24
- `compileSdk`/`targetSdk` 34 ; `minSdk` 26 (mobile) et 30 (Wear OS 3+)

```bash
./gradlew :mobile:assembleDebug
./gradlew :wear:assembleDebug
```

Le fichier `local.properties` (chemin du SDK Android) n'est pas versionné : Android Studio le
regénère à l'ouverture du projet.

## Lancer les deux apps

1. Ouvrir le dossier du projet dans Android Studio et laisser la synchronisation Gradle se terminer.
2. Lancer la configuration `mobile` sur un téléphone ou un émulateur, y ajouter au moins une station.
3. Lancer `wear` sur une montre ou un émulateur **appairé** au même appareil.

Le Data Layer ne fonctionne qu'entre appareils réellement appairés (Bluetooth/Wi-Fi), y compris en
émulateur — utiliser l'assistant Wear OS d'Android Studio ou `adb -s <wear_emulator> forward`.

## Limites connues

- Les favoris ne se modifient **que depuis le téléphone** : la montre reçoit une copie en lecture
  seule. Il n'y a donc pas de conflit de synchronisation possible, mais pas d'ajout de station au
  poignet non plus.
- Le widget et la tuile n'affichent que la **première station** par ordre alphabétique, et
  uniquement le METAR (un TAF est trop long pour ces formats).
- `PhoneRequestSender.requestRefreshAll()` et le chemin `/request-refresh-all` sont implémentés des
  deux côtés mais aucune UI ne les déclenche encore.
- Le projet ne contient **aucun test** automatisé.
- Les DTO réseau ne déclarent que les champs réellement affichés ; la réponse de l'API en contient
  bien davantage (vent, visibilité, nuages, QNH) si un décodage plus riche devenait nécessaire.
