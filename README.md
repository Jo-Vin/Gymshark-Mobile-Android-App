# Gymshark Product Challenge

Android application for the Gymshark Mobile Engineering Challenge,
built with Kotlin and Jetpack Compose.

## Current status

Stages 1–5 connect catalogue retrieval, screen state and the Compose product grid.

- Single Android app module.
- Adaptive Compose grid displaying product images, titles, colours and labels.
- Robolectric smoke test.
- Catalogue ViewModel with loading, content, empty, error, retry and product selection.
- Shared Compose behaviour checks run with Robolectric and on a connected Android device.
- GitHub Actions verification is being configured.

Product details and HTML description rendering are implemented as the next increment.

## Requirements

The completed application will:

- Fetch and parse the supplied product JSON.
- Display products with images, titles, prices, colours and labels.
- Handle missing or failed images gracefully.
- Show further product information when a product is selected.
- Present HTML descriptions appropriately.

Prices are GBP minor units: the list displays the product-level price, while details
show the product price until a variant is selected and then show that variant's price.
Variants with no price are reported as unavailable. Details use the native Android
HTML parser for readable paragraphs, entities, Unicode and basic text spans after
removing script/style and known translator-wrapper markup. This is intentionally not
claimed to be comprehensive HTML sanitisation, and no WebView is used.

The implementation will use MVVM and focused unit tests, developed through
test-driven development.

## Setup

1. Open the repository in Android Studio.
2. Configure the Gradle JDK as JDK 17 or 21 (local verification uses JDK 21).
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
.\gradlew.bat :app:connectedDebugAndroidTest # Requires an available device or emulator
```

On macOS or Linux, use ./gradlew instead of .\gradlew.bat.

The debug APK is generated at:

app/build/outputs/apk/debug/app-debug.apk

The Robolectric smoke test checks application startup behaviour.
Visual appearance and interaction must also be verified on a device or emulator.

## Catalogue retrieval

`ProductRepository.fetchProducts()` returns the existing displayable `Product` models.
`RemoteProductRepository` takes an injected `okhttp3.Call.Factory`; a shared
`OkHttpClient` implements this interface, while tests supply fake calls. OkHttp
4.12.0 was already present through Coil, so it is now declared directly rather
than introducing another HTTP client. Coroutine dependencies are also declared
explicitly, with `kotlinx-coroutines-test` used for deterministic tests.

A GET retrieves the supplied Gymshark catalogue endpoint. OkHttp performs the
request and response-body read on its worker thread. The existing parser decodes
the JSON and the existing mapper validates and normalises it on an injected
dispatcher (production default: `Dispatchers.Default`). Cancellation cancels the
HTTP call, and responses are closed on success, failure and late arrival.
Network errors, unsuccessful HTTP statuses and malformed JSON remain exceptions
for the caller to handle. Valid empty catalogues return an empty list; the mapper
continues to exclude unusable records according to its existing policy.

Repository tests use the local saved JSON fixture and controlled callbacks, never
the live endpoint. Their GS-MOB-001 and GS-MOB-002 references provide partial
evidence for retrieval and decoding, not live CDN availability or UI behaviour.
`ProductListViewModel` now consumes the repository contract and exposes read-only
`StateFlow<ProductListUiState>`. It loads once on creation, supports retry after
network or decoding failure, and cancels work when cleared. Tests control responses
with a fake repository and a test main dispatcher, without network calls or delays.

`ProductApplication` owns the real repository and one shared OkHttp client.
`MainActivity` obtains a retained ViewModel through `ViewModelProvider`.
`ProductListRoute` observes state with `collectAsStateWithLifecycle`, while
`ProductListScreen` renders states and sends retry/selection callbacks. Cards use
stable product IDs and the existing `ProductImage` component; image loading or
failure does not prevent selection. Selecting a card highlights it and records
its ID. Selecting a card opens its detail view by stable product ID; the detail view
uses the selected variant price when a variant is chosen and otherwise shows the
product-level GBP minor-unit price. A missing variant price is shown as unavailable.
The description is rendered with Android's native HTML parser after removing script,
style and known translator-wrapper markup. This supports readable paragraphs,
entities, Unicode and basic bold/italic/underline spans; it is deliberately not
claimed to be comprehensive HTML sanitisation and does not use a WebView.
The detail presentation uses a fixed safe app bar, a full-width portrait image,
wrapped outlined size controls and a collapsed Description section. Screenshot-only
features such as reviews, wishlist, sharing, checkout, delivery claims and
recommendations remain intentionally unsupported.

`ProductListUiChecks` is shared between the Robolectric and instrumented runners.
It controls repository responses and image results locally, checking content,
loading, empty/error/retry, scrolling, selection and image accessibility. Existing
GS-MOB references provide partial component evidence, not full assessment
verification, real CDN reliability, TalkBack usability or visual correctness.
A manual check on the connected Samsung Android 16 phone confirmed that the real
catalogue and product photos load. Automated tests still use fake responses.
Local verification completed 58 unit tests, lint (warnings only) and debug assembly.
All eight instrumented checks passed on the unlocked phone. A subsequent run was
blocked by the locked phone and needs repeating with the screen unlocked.
No persistent catalogue cache is implemented.

## Assumptions and decisions

- Search, filters, checkout and authentication are outside the required scope.
