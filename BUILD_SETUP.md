# Build setup

This guide takes you from nothing to the app running on an emulator, then shows where things live so you can change them.
You need no API keys and no accounts.

## 1. Install the tools

Pick one path.

**Path A: Android Studio (easiest).**
Install a current [Android Studio](https://developer.android.com/studio) release.
It bundles a JDK and installs the Android SDK on first launch.
The project uses Android Gradle Plugin 9.1, so an older Android Studio may refuse to sync it; if it does, update Android Studio.

**Path B: command line only.**

1. Install a JDK from version 17 to 21, and check it with `java -version`.
2. Download the [Android command-line tools](https://developer.android.com/studio#command-line-tools-only) zip.
   The tools only work from a folder named `cmdline-tools/latest` inside your SDK folder, so unpack them there:

   ```bash
   export ANDROID_HOME="$HOME/Android/sdk"
   mkdir -p "$ANDROID_HOME/cmdline-tools"
   unzip ~/Downloads/commandlinetools-*.zip -d "$ANDROID_HOME/cmdline-tools"
   mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
   ```

3. Put the SDK tools on your `PATH`.
   Add these lines to your shell profile (`~/.zshrc` on a Mac, `~/.bashrc` on Linux), then open a new terminal, so every terminal you use later has them:

   ```bash
   export ANDROID_HOME="$HOME/Android/sdk"
   export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"
   ```

4. Install the SDK packages, and type `y` when asked to accept the licences:

   ```bash
   sdkmanager "platform-tools" "platforms;android-36.1" "build-tools;36.0.0" "emulator" "system-images;android-34;google_apis;arm64-v8a"
   ```

   On an Intel or AMD computer, use `x86_64` instead of `arm64-v8a`, here and when you create the emulator in section 3.
   Recent versions print a warning that `sdkmanager` is deprecated and hand over to Google's new `android` tool; the install still works.

## 2. Get the code

```bash
git clone https://github.com/ZhaoruiXu/bc-parkspot.git
cd bc-parkspot
```

## 3. Run the app

**Path A: Android Studio.**

1. Choose **File > Open** and select the `bc-parkspot` folder.
2. Wait for the Gradle sync to finish. The first sync downloads dependencies and takes a few minutes.
3. Open **Device Manager** and create a virtual device, for example a Pixel phone with an API 34 image.
4. Select the `app` run configuration and that device, then press **Run**.

**Path B: command line.**

1. Create and start an emulator:

   ```bash
   avdmanager create avd -n bcparkspot -k "system-images;android-34;google_apis;arm64-v8a" -d pixel_7
   emulator -avd bcparkspot
   ```

   On an Intel or AMD computer, use `x86_64` instead of `arm64-v8a`.
   You can use a real phone instead: turn on USB debugging and connect it.
2. In a second terminal, wait until the emulator has finished starting.
   This prints `1` when it is ready:

   ```bash
   adb wait-for-device shell 'while [ "$(getprop sys.boot_completed)" != 1 ]; do sleep 2; done; echo 1'
   ```

3. From the `bc-parkspot` folder, build and install the app:

   ```bash
   ./gradlew :app:installDebug
   ```

   The first build downloads Gradle and the dependencies, so it takes a few minutes.
4. Open **BC ParkSpot** from the emulator's app drawer, or start it with:

   ```bash
   adb shell am start -n ca.bcparkspot.app/.MainActivity
   ```

To only build the APK, run `./gradlew :app:assembleDebug`.
It is written to `app/build/outputs/apk/debug/app-debug.apk`.

## 4. Try the features

1. **Explore** shows the 18 lots as a list.
   Tap a region chip, or **No Pass Needed** or **Free Parking**, to filter it.
2. Tap **Departing from** at the top and pick a town.
   Distances and drive times on the cards change.
3. Tap **Map** to see the same lots as pins.
   Tap a pin for a summary card, then **Trailhead Details** for the full page.
4. Tap the location button on the map, below the zoom buttons.
   The app asks for location permission, then shows your position.
   On an emulator, set a location first, in the emulator's extended controls or with `adb emu geo fix -123.12 49.28` (longitude first).
5. On a lot's detail page, tap the bookmark icon at the top right.
   The lot appears under **Saved**, and it stays there after you close and reopen the app.
6. On the same page, look at the report status.
   With no recent reports, a lot that public sources or park rules say something about shows it, tagged **Public info**, and the directions to it show a **Today at this lot** card. Lines taken from a web page link to it; closures, gate hours and pass days come from the lot details in the app and carry no link.
   Which line appears depends on the date and time in BC: a gated lot at night shows its gate line, as does one whose gate closes within an hour, while pass days and the summer lines need their season, so outside it you would change the device date to see them.
7. Tap **Get Directions**.
   The **Route** tab shows the trip with the lot's report status and public info, and **Pin My Car Location Here (Offline)** saves where you parked.
8. Back on **Explore**, tap **Find my best bet**.
   The app picks the nearest lot by drive time, preferring confirmed-open lots, skips lots whose gate is usually or may be closed, or closes within an hour of arrival, and opens directions to it.
   With no route open, the **Route** tab offers the same choice through **Find my best bet**.
9. **Passes** explains which parks need a BC Parks day-use pass and when.
10. Tap the account icon at the top of **Explore** to open the account sheet.
    Under **Appearance**, pick **System**, **Light** or **Dark**; the map follows the theme.
11. Turn off the network on the device, with airplane mode or with `adb shell svc wifi disable` and `adb shell svc data disable`.
    The map shows "Map unavailable offline", and the list, filters, details and saved lots keep working.
    Turn it back on with `enable` in place of `disable`.

Google Sign-In, sync and shared community reports need a Firebase project.
Without one, saved lots, the car pin and your own reports stay on the phone, no reports are shared or received, and the rest of the app works normally.
See section 7.

## 5. Run the tests

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

The unit tests run on the JVM with Robolectric, so no emulator is needed.
They cover the report status rules, public info and its sources, the best-bet choice, pass rules, sync and merge rules, account flows, database migrations, theme colours and contrast, map pin mapping, app launch and navigation.
The 12 tests in `FirestoreRulesTest.kt` are skipped unless the Firebase emulators are running; see section 8.
The report is in `app/build/reports/tests/testDebugUnitTest/index.html`.

## 6. Find your way around the code

All app code is in `app/src/main/java/ca/bcparkspot/app/`.

| Folder or file | What it holds |
| --- | --- |
| `MainActivity.kt` | The four tabs and the navigation between screens |
| `AppGraph.kt` | Builds the app's long-lived objects: database, sync, accounts and Firebase |
| `model/ParkingModels.kt` | Data types: lots, pass rules, reports, routes |
| `domain/LotStatus.kt` | Pure classifier that turns reports into a lot status, with the rules in `ReportPolicies` |
| `domain/Prediction.kt` | Pure functions that turn sourced advice, gate hours, closures and pass days into the labelled public info line |
| `domain/FindNearest.kt` | The tier order "Find my best bet" uses to pick a lot |
| `data/repository/BCParksDataSource.kt` | The 18 lots, the starting towns, and the distance and drive-time estimates |
| `data/repository/FillPatterns.kt` | Busy-day tips per lot, each with its public source |
| `data/repository/LotFacts.kt` | Gathers one lot's advice, gate hours, closures and pass state at a given time |
| `data/repository/GateWindows.kt` | Each lot's usual gate hours, season by season, from its operator's published schedule |
| `data/repository/ParkingRepository.kt` | Combines bundled data, reports and public info for the screens |
| `data/local/` | The Room database: saved lots, the car pin and your own reports |
| `data/remote/` | Firestore reads and writes, and the field limits the rules enforce |
| `sync/` | Background sync between Room and Firestore, and the merge rules |
| `auth/` | Guest and Google accounts, sign-out and data deletion |
| `ui/viewmodel/ParkingViewModel.kt` | Screen state and user actions |
| `ui/screens/` | One file per screen |
| `ui/components/` | Reusable pieces: cards, chips, dialogs, the account sheet |
| `ui/map/` | The map view, its pins, its theme tinting and its named size constants |
| `ui/theme/` | Colours, type, spacing and the saved theme choice |

Tests mirror this layout under `app/src/test/java/ca/bcparkspot/app/`.

**Common changes:**

- **Add or update a lot:** edit its `ParkingLot` entry in `BCParksDataSource.kt`.
  Set `sourceUrl` to the official page you checked and `verifiedAt` to that date.
- **Change the report rules:** edit the constants in `ReportPolicies` in `domain/LotStatus.kt`, then update `LotStatusTest.kt`.
  The report window is repeated in `firestore.rules`, so change it there too.
- **Add or update a busy-day tip:** edit `FillPatterns.kt`, cite the source you used and claim only what it states, then update `FillPatternsTest.kt`.
  Gate hours go in `GateWindows.kt`.
- **Change the local database:** after editing an entity in `data/local/`, raise the database version in `AppDatabase.kt`, add a `Migration`, and add a case to `AppDatabaseMigrationTest.kt`.
  The build exports each version's schema to `app/schemas/`; commit those files.
- **Change map sizes:** use the named constants in `ui/map/ParkingMapConstants.kt` rather than raw numbers.
- **Change colours:** edit `ui/theme/Palette.kt`; `ColorLiteralScanTest.kt` fails if a colour is written anywhere else.

## 7. Use your own Firebase project (optional)

This turns on guest accounts, Google Sign-In, sync and shared community reports.

1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Enable **Authentication > Anonymous**, **Authentication > Google** and **Cloud Firestore**.
3. Register an Android app with the package name `ca.bcparkspot.app`, and add your debug signing certificate's SHA-1.
   Get it with `./gradlew :app:signingReport`.
4. Save the downloaded `google-services.json` as `app/google-services.json`.
   It is gitignored, so it is never committed.
5. Deploy the security rules with the [Firebase CLI](https://firebase.google.com/docs/cli):

   ```bash
   firebase login
   firebase use --add    # pick the project you created in step 1
   firebase deploy --only firestore:rules
   ```

6. Rebuild and run the app.

To keep testing out of your live data, add a second project for development the same way, register the package name `ca.bcparkspot.app.debug` there, and save its config as `app/src/debug/google-services.json`.
Debug builds then install as a separate app that talks to the development project, while release builds keep using `app/google-services.json`.

## 8. Firestore rule tests (optional)

These need the [Firebase CLI](https://firebase.google.com/docs/cli), which itself needs Java 21 for its emulators.

```bash
firebase emulators:exec --only firestore,auth --project demo-no-project "./gradlew :app:testDebugUnitTest --rerun"
```

`demo-no-project` is a demo project name, so the emulators never reach a real Firebase project.
`--rerun` makes Gradle run the tests again even if they already passed without the emulators.

## Troubleshooting

| What you see | What to do |
| --- | --- |
| `SDK location not found` | `ANDROID_HOME` is not set in this terminal. Add the section 1 lines to your shell profile and open a new terminal, or create `local.properties` in the project folder with `sdk.dir=/path/to/your/sdk`. |
| `Could not determine SDK root` from `sdkmanager` | The command-line tools are not in `cmdline-tools/latest`. Redo section 1, step 2. |
| A Gradle error that names your Java version | Your JDK is outside 17 to 21. Install JDK 17 or 21 and point `JAVA_HOME` at it. |
| `No connected devices!` | The emulator is not running or has not finished starting. Run the wait command in section 3. |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | Another copy of the app, signed differently, is installed. Run `adb uninstall ca.bcparkspot.app` and install again. |
| The map stays blank | The map needs internet access. Check the emulator has a connection; the list works without one. |
