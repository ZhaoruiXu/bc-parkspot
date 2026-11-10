# BC ParkSpot

An Android app that shows trailhead parking across British Columbia parks, combining crowd-sourced reports with park pass rules.

**Status: in development.**
The app is not on Google Play, and no live backend ships with this repository.
Pass rules and lot details can go out of date, so always confirm them with the park authority before you travel.

## Features

- **Offline lot guide:** 18 lots across Sea-to-Sky, Metro Vancouver and the Fraser Valley, bundled with the app.
- **List or map:** browse lots as a list or as pins on a map, with the same region and pass filters.
  The map needs a connection; the list works offline.
- **Crowd reports:** availability comes only from hiker reports, never from sensors or cameras.
  A status shows only when at least 2 distinct users report the same status within the validity window.
  Otherwise the lot shows Unknown.
- **Freshness decay:** recent reports carry more weight, reports go stale after 90 minutes, and they expire after 3 hours.
- **Day-use pass rules:** date, weekday, holiday and time-of-day rules, including BC statutory holidays.
  A park with unpublished dates shows "Pass dates TBA" rather than a guess.
- **Saved-car locator:** pin your car, then see its distance and true-north bearing, offline.
- **Google Maps navigation:** one tap opens directions to a lot or back to your car.
- **Optional account sync:** Google Sign-In syncs saved lots and your car pin through Firestore, and you can delete your account and data from the app.

## Data

- Lot details come from public park authority pages, and each lot links to its source.
- Lots with a `verifiedAt` date were compared with the source on that date.
  Lots without one are unverified baseline data.
- Community reports stay in Firestore until their author deletes them or their account.
  The app stops using a report after 3 hours.
- Distances and drive times are estimates from the chosen starting town, not live traffic.
- Map tiles come from [OpenStreetMap](https://www.openstreetmap.org/copyright), © OpenStreetMap contributors, rendered with MapLibre.
  A release build would need its own tile provider, since the public OpenStreetMap tile servers are not meant for app traffic.
- The app icon and the banner image were generated with AI.

## Tech

- Kotlin, Jetpack Compose and Material 3
- MapLibre with OpenStreetMap tiles
- MVVM with StateFlow
- Room for offline storage
- Firebase Authentication and Cloud Firestore, with security rules in `firestore.rules`
- JUnit and Robolectric tests

## Build

```bash
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

A fresh clone builds and runs with no extra files.
Sign-in and sync stay off until you add your own Firebase project.
See `BUILD_SETUP.md`.

## Licence

MIT. See `LICENSE`.
