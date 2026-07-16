# Used Phone Inspection Assistant

Used Phone Inspection Assistant is a private, offline-first native Android application for evaluating used phones, tablets, refurbished devices, repair-shop intake devices, and trade-ins. It combines Android-reported hardware readings with explicit human observations and stores the resulting inspection locally.

The package name is `com.shahriarhasan.usedphoneinspector`. Android Studio is optional: the checked-in Gradle Wrapper and GitHub Actions workflows are the authoritative build path, and normal editing works from VS Code without a local Android SDK.

## Product coverage

- Guided profiles for used phones and tablets, plus functional refurbished, repair-intake, and trade-in presets.
- Resumable inspection wizard with saved progress, notes, evidence, pass/warning/fail/skip/unsupported/permission-denied states, and coverage tracking.
- Device information, immersive display colours, multitouch trails and coverage, local speaker samples, short microphone record/playback, CameraX preview/capture/focus/zoom, torch, vibration, all-sensor inventory, interactive core sensors, charging, battery, Wi-Fi, Bluetooth, permitted telephony, physical checklist, and seller/customer intake.
- Transparent weighted score, separate completion coverage, incomplete-result warning below 60% coverage, and locked completion snapshot.
- Room-backed history with search, profile/grade/status filters, sorting, resume, edit, duplicate, report, and delete actions.
- Offline multipage PDF with free summary and Pro full-report modes, Bangla/English labels, evidence scaling, watermark rules, SAF save, Sharesheet, preview, and print.
- Non-consumable Google Play Billing product `lifetime_pro`; the displayed price always comes from Google Play.
- English and Bangla resources, light/dark/system themes, dynamic colour, compact bottom navigation, and large-screen navigation rail.

## Architecture

The project uses one application module and package-by-feature organization. Compose screens render immutable state and send events to Hilt ViewModels. Repositories own Room, DataStore, billing, reporting, evidence, and Android hardware APIs; composables do not retrieve restricted identifiers or call low-level hardware APIs directly.

```text
Compose UI -> ViewModel / StateFlow -> domain policy -> repository interface
                                                   -> Room / DataStore
                                                   -> Android hardware APIs
                                                   -> BillingClient / PdfDocument
```

Key decisions:

- UUID strings identify all local records.
- Room foreign keys and indexes protect relations; `MIGRATION_1_2` is explicit and destructive migration is disabled.
- Completion stores a JSON snapshot and report ID. Editing is an explicit reopen action and records modification time.
- `ScoreEngine` excludes skipped, unsupported, denied, and unfinished tests from possible score while keeping coverage separate.
- Missing optional hardware is unsupported, never failed.
- Battery health is only the enum/value Android reports; no synthetic battery-health percentage is calculated.
- Evidence stays in app-private storage, imported images use the picker, and exports require a direct user action.

## Technology stack

- Kotlin 2.3.10, Java 17, Gradle 8.13, Android Gradle Plugin 8.13.2
- `minSdk 26`, `compileSdk 36`, `targetSdk 36`
- Jetpack Compose with Material 3, Navigation Compose, and Material 3 Adaptive
- Coroutines, Flow/StateFlow, Room, Preferences DataStore, Hilt
- CameraX, `SensorManager`, `BatteryManager`, connectivity, Bluetooth, and permitted telephony APIs
- Android `PdfDocument`, FileProvider, Storage Access Framework, and print framework
- Google Play Billing 9.1.0
- Detekt, JUnit, Robolectric, Room tests, and Compose UI tests

All versions are pinned in `gradle/libs.versions.toml`; no alpha, beta, RC, snapshot, wildcard, or dynamic versions are used.

## Project structure

```text
app/src/main/java/com/shahriarhasan/usedphoneinspector/
  app/                    Application, activity, root state
  navigation/             Compose navigation and adaptive shell
  core/
    billing/ database/ datastore/ design/ hardware/
    model/ permissions/ reporting/ testing/ utilities/
  feature/
    onboarding/ home/ inspection/ displaytest/ multitouch/
    audiotest/ cameratest/ vibrationtest/ sensortest/
    chargingtest/ connectivity/ physicalinspection/
    deviceidentity/ seller/ review/ history/ settings/ upgrade/
```

## Privacy, offline behavior, and identifiers

There is no account, backend, analytics, advertising, crash-upload SDK, remote configuration, or tracking SDK. Inspections work offline. Purchase and restore require Google Play connectivity, while a cached Pro entitlement continues offline.

IMEI, serial number, Wi-Fi MAC, IMSI, ICCID, subscriber number, and other non-resettable identifiers are never retrieved automatically. IMEI and serial fields are manual, optional, local, and are not represented as blacklist or manufacturer verification. Read [PRIVACY_POLICY.md](PRIVACY_POLICY.md) and [SECURITY.md](SECURITY.md).

## Permissions

Permissions are requested only when the relevant test is opened:

| Permission | Purpose |
| --- | --- |
| Camera | Camera preview, test photos, and user-selected evidence |
| Record audio | Foreground-only short microphone sample |
| Vibrate | User-triggered haptic patterns |
| Bluetooth scan/connect | Explicit nearby scan and permitted adapter state on Android 12+ |
| Fine location (Android 11 and lower only) | Legacy Bluetooth/Wi-Fi scan requirement |
| Read phone state | Permitted SIM/network diagnostics only; never IMEI |

Denial records a permission-denied state and does not stop the inspection. No storage, SMS, contacts, call-log, accessibility, overlay, administrator, background-location, or root permission is requested.

## Free and Pro

All safety and hardware tests are available to every user.

Free supports three completed inspections, unlimited viewing, summary PDF, up to two report photos, and the default watermark. Pro unlocks unlimited completions, detailed PDF, safely compressed report photos, branding, repair/trade-in report formats, and JSON backup/restore. Create one non-consumable Play product with ID `lifetime_pro`; do not add a subscription.

## VS Code and command-line workflow

Install a Java 17 JDK. A local Android SDK is needed only for local compilation or device deployment; GitHub Actions installs the required SDK through the Android build tooling. From the repository root:

```bash
./gradlew tasks
./gradlew detekt
./gradlew lintDebug
./gradlew testDebugUnitTest
./gradlew assembleDebug
./gradlew bundleRelease
```

On Windows use `gradlew.bat` instead of `./gradlew`. Generated outputs are under `app/build/outputs/` and are ignored by Git.

## GitHub Actions

### Continuous integration

`.github/workflows/android-ci.yml` runs for pull requests, pushes to `main`, and manual dispatch. It validates the wrapper, configures Temurin Java 17 and Gradle caching, then runs:

```bash
./gradlew --no-daemon clean
./gradlew --no-daemon detekt
./gradlew --no-daemon lintDebug
./gradlew --no-daemon testDebugUnitTest
./gradlew --no-daemon assembleDebug
```

To download a debug APK, open a successful **Android CI** run in GitHub, scroll to **Artifacts**, and download `used-phone-inspector-debug-apk`.

### Signed releases

Configure these repository or environment secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Encode a keystore without line wrapping, store only the base64 value as a secret, and never commit the keystore. Push a tag to build APK/AAB and create a GitHub Release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Manual dispatch also accepts version name and version code. Temporary signing files are written under the runner temporary directory and deleted in an `always()` step.

### Google Play internal testing

1. In Play Console, create the app with package `com.shahriarhasan.usedphoneinspector` and enable Play App Signing.
2. Create the managed product `lifetime_pro` and activate it.
3. In Google Cloud/Play Console, create a service account, grant only the release permissions it needs, invite it under **Users and permissions**, and save its JSON as `PLAY_SERVICE_ACCOUNT_JSON`.
4. Configure all four signing secrets listed above.
5. Manually run **Publish to Google Play**, choose `internal`, and keep `release_status` as `draft` for the first upload.

The publishing workflow never defaults to production. Promote a tested release in Play Console or explicitly choose a different track during manual dispatch.

## Changing app identity

To change the application ID, update `namespace` and `applicationId` in `app/build.gradle.kts`, the Kotlin package directories/declarations, the Play workflow `packageName`, and any existing Play Console configuration. To change the visible name, edit `app_name` in both `values/strings.xml` and `values-bn/strings.xml`.

## Tests

Unit tests cover scoring, coverage, grades, exclusions, profile weights, completion persistence, entitlement limits, report filenames, import validation, Room operations, DataStore settings, and state transitions. Compose instrumentation tests cover core screen states and manual result controls. Hardware repositories have fakes so CI never needs a physical phone.

## Hardware and vendor limitations

- Android/OEMs may omit battery current, charge counter, charge-time estimate, cycle count, capacity, design capacity, manufacture date, camera physical IDs, signal details, SSID, or sensor values.
- CameraX can expose logical cameras without a stable consumer-facing ultrawide/telephoto label.
- Tablet and Wi-Fi-only hardware omits telephony/IMEI-related tests.
- Dead pixels, burn-in, speaker quality, microphone quality, repair history, water damage, ownership, IMEI validity, and future reliability require manual or external verification.
- Bluetooth/Wi-Fi scan behavior is permission-, vendor-, and throttle-dependent.
- A charging wattage value appears only when suitable voltage and current readings exist and is labelled as an estimate.

## License and contributions

Apache License 2.0. See [LICENSE](LICENSE) and [CONTRIBUTING.md](CONTRIBUTING.md).

