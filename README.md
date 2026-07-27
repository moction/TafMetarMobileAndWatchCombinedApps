# TafMetarApp — architecture bi-module (mobile + Wear OS)

Ce projet met en œuvre l'architecture validée : l'app **mobile** est seule responsable des appels
réseau vers `aviationweather.gov`, et l'app **Wear OS** ne fait plus que se synchroniser avec elle
via le **Wearable Data Layer API** (`DataClient` / `MessageClient` / `NodeClient`).

## Structure

```
TafMetarApp/
├── shared/   Modèles de données + constantes de chemins Data Layer (Kotlin pur, partagé)
├── mobile/   App téléphone : réseau (Retrofit), WorkManager périodique, push vers la montre
└── wear/     App montre : aucune dépendance réseau, lecture d'un cache local + Compose Wear
```

## Ce qui a changé par rapport à la version précédente (montre autonome)

| Avant | Maintenant |
|---|---|
| La montre appelle directement l'API météo | Le téléphone appelle l'API, la montre ne fait plus aucun appel réseau |
| Retrofit/OkHttp dans le module `wear` | Retrofit/OkHttp uniquement dans `mobile` |
| Permission `INTERNET` sur la montre | Permission `INTERNET` uniquement sur le téléphone |
| Rafraîchissement géré par la montre | `WorkManager` périodique (15 min) côté téléphone + refresh manuel via `MessageClient` |

## Flux de données

1. `RefreshWorker` (mobile) interroge `aviationweather.gov` toutes les 15 minutes pour les stations
   favorites (`FavoritesStore`).
2. Chaque METAR/TAF récupéré est poussé vers la montre via `WatchSyncManager.pushMetar/pushTaf`
   (un `DataItem` par station : `/metar/{ICAO}`, `/taf/{ICAO}`).
3. Côté montre, `WatchDataListenerService` reçoit ces `DataItem` et les écrit dans
   `WatchCacheStore` (DataStore local).
4. L'UI Compose Wear (`StationPagerScreen`) observe ce cache via des `Flow` : l'affichage est
   instantané, sans aller chercher quoi que ce soit sur le réseau.
5. Si l'utilisateur tape "Rafraîchir" sur la montre, `PhoneRequestSender` envoie un `Message`
   (`/request-refresh`) au téléphone, qui refait l'appel réseau et repousse le résultat par le
   même mécanisme que l'étape 2.

## Ouvrir le projet

1. Android Studio (Koala ou plus récent) → **Open** → sélectionner le dossier `TafMetarApp/`
2. Laisser la synchronisation Gradle se terminer (télécharge les dépendances Wear Compose,
   DataStore, Retrofit, Data Layer…)
3. Deux configurations de lancement apparaissent : `mobile` et `wear`. Lancer d'abord `mobile`
   sur un téléphone/émulateur, puis `wear` sur une montre/émulateur **appairée** au même compte
   (le Data Layer ne fonctionne qu'entre appareils réellement appairés Bluetooth/Wi-Fi, y compris
   en émulateur via `adb -s <wear_emulator> redirect...` ou l'assistant Wear OS d'Android Studio).

## Limites connues de cette première version à affiner

- `MetarTileService` a un rendu de mise en page volontairement minimal (pas de
  `LayoutElementBuilders` détaillé) — à enrichir avec `androidx.wear.tiles.material` pour un
  vrai rendu visuel.
- Le décodage du plafond de nuages (ceiling) n'est pas fait dans `WeatherRepository` faute de
  champ dans le DTO simplifié — `FlightCategory` n'utilise ici que la visibilité.
- Pas de gestion d'un éventuel conflit si l'utilisateur modifie ses favoris simultanément sur les
  deux appareils (dernier écrit gagne, ce qui est suffisant pour un usage mono-utilisateur).
- Aucune icône `mipmap` n'est fournie dans ce squelette : Android Studio proposera d'en générer
  via l'assistant "Image Asset" au premier build.
