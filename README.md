# Product Challenge

Minimal Kotlin Android application using Jetpack Compose and a single app module.
The launch screen displays Product Challenge. No product features or service layers are included.

## Tooling and SDK choices

- Gradle 8.13, Android Gradle plugin 8.13.0, Kotlin/Compose compiler 2.2.10.
- Compose BOM 2025.08.00 and Activity Compose 1.10.1 are pinned stable dependencies.
- Use JDK 17 to run Gradle. Java/Kotlin bytecode targets Java 17.
- Minimum SDK 23 (Android 6.0) provides broad device coverage and is sufficient for
  this Compose foundation. Raise it only when assessment requirements justify it.
- Compile and target SDK 36 (Android 16) use the platform supported by AGP 8.13.
  This is provisional: no installed SDK was found during setup, so it cannot be
  described as the latest SDK already supported by this machine.
- Install Android SDK Platform 36 and Build Tools 35.0.0 using Android Studio's SDK
  Manager. Set the SDK path in untracked local.properties or ANDROID_HOME.
- No Android SDK, emulator or compatible Gradle runtime was found in the checked
  standard locations. PATH currently resolves Java 8; the IDE bundles Java 25.
  These do not provide a verified runtime for this Gradle 8.13 setup.

## Build and verification

From PowerShell with JAVA_HOME pointing to JDK 17:

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:lintDebug
.\gradlew.bat :app:assembleDebug
```

The Robolectric smoke test checks launcher resolution and activity content creation
on API 23 and 35. It does not establish that the screen rendered on a real device.
After a successful build, run MainActivity on an emulator/device to verify the launch
screen visually. The debug APK is app/build/outputs/apk/debug/app-debug.apk.

Setup verification: the wrapper reported Gradle 8.13 successfully. Unit tests, lint
and debug assembly each failed before project evaluation because the 32-bit Java 8
runtime could not reserve the configured 2 GB heap. A unit-test retry with
`-Dorg.gradle.jvmargs=-Xmx512m --no-daemon` reached configuration but failed with
`No Java compiler found`: PATH points to a JRE rather than a JDK. Gradle also emitted
a deprecation warning; its source has not been diagnosed with a compatible JDK.
No test, lint, APK build or device launch has been verified successfully.

## Project structure

Gradle automates dependency resolution, compilation, tests and packaging. The wrapper
pins its version; the version catalogue centralises plugin and library versions.
The app module produces the installable APK. The package/namespace and application ID
are com.jovinyap.productchallenge; the application ID identifies the installed app.
Minimum SDK controls install eligibility, compile SDK exposes platform APIs to the
compiler, and target SDK selects Android compatibility behaviour.
Compose describes the UI with Kotlin composable functions.

Keep future presentation state in ViewModels and business logic outside composables.
Add focused tests alongside new behaviour rather than speculative architecture.
Existing IntelliJ settings are preserved and excluded by the Android .gitignore.
