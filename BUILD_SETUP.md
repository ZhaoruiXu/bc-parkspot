# Build setup

## Prerequisites

- JDK 17 or newer
- Android SDK with platform 36 (extension level 1) and Build Tools 36

## Build and test

```bash
git clone https://github.com/ZhaoruiXu/bc-parkspot.git
cd bc-parkspot
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

With no Firebase configuration, the build uses placeholder values and default debug signing.
The app runs, but Google Sign-In and sync are unavailable.

## Firestore rule tests

These tests are skipped unless the Firebase emulators are running.
To include them:

```bash
firebase emulators:exec --only firestore,auth "./gradlew :app:testDebugUnitTest"
```

## Use your own Firebase project

1. Create a project in the [Firebase Console](https://console.firebase.google.com/).
2. Enable Google Sign-In and Cloud Firestore.
3. Register an Android app with the `applicationId` from `app/build.gradle.kts`, and add your signing certificate's SHA-1.
4. Save the downloaded `google-services.json` as `app/google-services.json`. It is gitignored.
5. Deploy `firestore.rules`.
