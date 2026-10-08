# Gymshark Product Challenge

Android application for the Gymshark Mobile Engineering Challenge,
built with Kotlin and Jetpack Compose.

## Current status

The initial application foundation is implemented.

- Single Android app module.
- Compose launch screen displaying "Product Challenge".
- Robolectric smoke test.
- Android lint passed locally.
- Unit-test, debug-build and device-launch results still need confirming.
- GitHub Actions verification is being configured.

Product-list and product-detail functionality are the next implementation steps.

## Requirements

The completed application will:

- Fetch and parse the supplied product JSON.
- Display products with images, titles, prices, colours and labels.
- Handle missing or failed images gracefully.
- Show further product information when a product is selected.
- Present HTML descriptions appropriately.

The implementation will use MVVM and focused unit tests, developed through
test-driven development.

## Setup

1. Open the repository in Android Studio.
2. Configure the Gradle JDK as JDK 17.
3. Install Android SDK Platform 36 and Build Tools 35.0.0.
4. Allow Gradle to sync.
5. Select an emulator or connected Android device and run the app.

The SDK location belongs in local.properties, which is excluded from Git.

Dependency and plugin versions are defined in gradle/libs.versions.toml.
The Gradle wrapper provides the project's Gradle version.

## SDK decisions

- Minimum SDK: 23, supporting Android 6.0 and later.
- Compile SDK: 36.
- Target SDK: 36.

Minimum SDK 23 was chosen for broad device compatibility within the assessment.
These choices can be revisited if the requirements change.

## Verification

From PowerShell in the repository root:

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

On macOS or Linux, use ./gradlew instead of .\gradlew.bat.

The debug APK is generated at:

app/build/outputs/apk/debug/app-debug.apk

The Robolectric smoke test checks application startup behaviour.
Visual appearance and interaction must also be verified on a device or emulator.

## Assumptions and decisions

- Currency, price units and product-versus-variant pricing are awaiting clarification.
- Pricing assumptions will be recorded before implementing price display.
- Search, filters, checkout and authentication are outside the required scope.