[![ci](https://github.com/logibooks/logibooks.scanner/actions/workflows/ci.yml/badge.svg)](https://github.com/logibooks/logibooks.scanner/actions/workflows/ci.yml)

# LogiScanner

LogiScanner is an Android barcode scanning app for Logibooks warehouse and logistics workflows. It lets an operator sign in, select an in-progress scan job, scan parcels or boxes, and watch the job state update while work is in progress.

The app supports industrial Android devices with an MT93 hardware scanner and standard Android phones paired with Bluetooth HID scanners such as the WD4 ring scanner.

## Features

- Email/password authentication against the Logibooks backend
- In-progress scan job list with live refresh
- Parcel and box barcode submission for the selected scan job
- Live scan job monitor with register, box, unassigned, and not-in-register views
- Auto-follow for the latest scan in the monitor
- MT93 hardware scanner input
- Bluetooth HID keyboard-wedge scanner input with scan-speed heuristics
- Visual scan-result feedback and Russian text-to-speech for backend scan messages
- Local auth state storage with DataStore Preferences
- English and Russian UI resources

## Requirements

- Android Studio with Android SDK support
- JDK 17 for local builds and CI
- Android 8.0 (API 26) or newer device
- Network access to a Logibooks backend
- MT93 scanner device or a Bluetooth HID scanner such as WD4

The app currently compiles with SDK 36 and targets SDK 36.

## Technology Stack

- Kotlin
- Jetpack Compose and Material 3
- Android ViewModel and Kotlin coroutines
- Retrofit, Moshi, and OkHttp
- Microsoft SignalR client for live scan job updates
- DataStore Preferences
- Gradle Kotlin DSL

## Project Structure

```text
app/
  src/main/java/consulting/sw/logiscanner/
    MainActivity.kt
    net/                         REST API models and service definitions
    repo/                        Login, scan job, scan, and monitor repositories
    scan/                        MT93 and HID scanner input handling
    store/                       DataStore-backed auth state
    ui/                          Compose UI and main view model
  src/main/res/
    values/                      English resources
    values-ru/                   Russian resources
docs/
  WD4-Setup.md                   Bluetooth HID ring scanner setup guide
```

## Building

Clone the repository and build with the Gradle wrapper:

```bash
git clone https://github.com/logibooks/logibooks.scanner.git
cd logibooks.scanner
./gradlew assembleDebug
```

On Windows PowerShell, use:

```powershell
.\gradlew.bat assembleDebug
```

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Release Builds

Release builds use the signing configuration in [app/build.gradle.kts](app/build.gradle.kts). Provide these environment variables before running `assembleRelease` or `build`:

- `SIGNING_STORE_FILE`: path to the keystore file
- `SIGNING_STORE_PASSWORD`: keystore password
- `SIGNING_KEY_ALIAS`: key alias
- `SIGNING_KEY_PASSWORD`: key password

Then run:

```bash
./gradlew assembleRelease
```

## Tests

Run the JVM unit tests with:

```bash
./gradlew test
```

Run the full Gradle build with:

```bash
./gradlew build
```

CI runs `./gradlew build` and `./gradlew test` on GitHub Actions.

## Configuration

### Server URLs

The backend base URL is compiled into `BuildConfig.SERVER_URL` by build type in [app/build.gradle.kts](app/build.gradle.kts):

- Debug: `http://192.168.11.140:8080/`
- Release: `https://logibooks.sw.consulting:8085/`

The URL must include a trailing slash.

### Application Version

The display version is managed in [gradle.properties](gradle.properties):

```properties
appVersionName=0.4.0
```

## Backend Integration

The app uses these REST endpoints relative to `SERVER_URL`:

- `POST api/Auth/login` for authentication
- `GET api/ScanJobs/ops` for scan job operation metadata
- `GET api/ScanJobs/in-progress` for available scan jobs
- `GET api/ScanJobs/{id}/monitor` for monitor snapshots
- `POST api/ScanJobs/scan` for scanned barcode submission

Authenticated REST requests send the JWT token as a Bearer token.

Live updates use the SignalR hub at:

```text
/hubs/scan-jobs
```

The app subscribes to scan job list changes and per-job monitor snapshots through that hub.

## Scanner Setup

### MT93

MT93-compatible devices use the built-in hardware scanner. No additional pairing is required.

### WD4 and Other Bluetooth HID Scanners

Configure the scanner as a Bluetooth HID keyboard, pair it with Android, and configure a scan terminator such as CR or LF when available.

See [docs/WD4-Setup.md](docs/WD4-Setup.md) for detailed WD4 setup and troubleshooting.

## Operator Flow

1. Sign in with a Logibooks account.
2. Select an in-progress scan job.
3. Start scanning.
4. Scan a parcel or box barcode with the hardware scanner or paired HID scanner.
5. Review the result color, spoken backend message, and live monitor update.
6. Return to the job list or log out when finished.

## Localization

The app includes English resources in `values/` and Russian resources in `values-ru/`. Android selects the language from the device locale and falls back to English.

## License

Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)  
All rights reserved.  
This file is a part of LogiScanner application.

## Support

For issues, questions, or contributions, contact the development team at www.sw.consulting.
