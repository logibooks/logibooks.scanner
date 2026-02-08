[![ci](https://github.com/logibooks/logibooks.scanner/actions/workflows/ci.yml/badge.svg)](https://github.com/logibooks/logibooks.scanner/actions/workflows/ci.yml)

# LogiScanner

LogiScanner is an Android application designed for warehouse and logistics operations, providing barcode scanning capabilities for inventory management and parcel tracking. The app works with industrial Android scanning devices (MT93) and integrates with the Logibooks backend system.

## Features

- **User Authentication**: Secure login with email and password
- **Scan Job Management**: Select and manage different scanning jobs from the server
- **Real-time Barcode Scanning**: Hardware-based barcode scanning using MT93 devices
- **Visual & Audio Feedback**: Color-coded scan results with Russian text-to-speech feedback
- **Offline Support**: Local authentication data storage using DataStore
- **Multi-language Support**: UI strings available in English and Russian

## Requirements

- Android SDK 26 (Android 8.0 Oreo) or higher
- Target SDK: 36
- Compatible with MT93 barcode scanning devices
- Network connectivity to Logibooks backend server

## Technology Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose with Material 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Networking**: Retrofit + Moshi for REST API communication
- **Dependency Injection**: Manual dependency injection
- **Local Storage**: DataStore Preferences
- **Build System**: Gradle (Kotlin DSL)

## Project Structure

```
app/
├── src/main/
│   ├── java/consulting/sw/logiscanner/
│   │   ├── MainActivity.kt              # Main activity with Compose UI
│   │   ├── net/                         # Network layer
│   │   │   ├── ApiService.kt           # REST API definitions
│   │   │   ├── ApiModels.kt            # Data models
│   │   │   └── NetworkModule.kt        # Network configuration
│   │   ├── repo/                        # Repository layer
│   │   │   ├── LoginRepository.kt      # Authentication logic
│   │   │   ├── ScanJobRepository.kt    # Scan job management
│   │   │   └── ScanRepository.kt       # Barcode scanning operations
│   │   ├── scan/                        # Scanner integration
│   │   │   └── Mt93ScanReceiver.kt     # MT93 device receiver
│   │   ├── store/                       # Local data storage
│   │   │   └── AuthStore.kt            # Authentication data store
│   │   └── ui/                          # UI components
│   │       ├── MainViewModel.kt         # Main view model
│   │       └── theme/                   # Material theme
│   └── res/                             # Resources
│       ├── values/strings.xml           # English strings
│       └── values-ru/strings.xml        # Russian strings
```

## Building the Project

### Prerequisites

1. Install [Android Studio](https://developer.android.com/studio) (latest version recommended)
2. Install JDK 11 or higher
3. Clone the repository:
   ```bash
   git clone https://github.com/logibooks/logibooks.scanner.git
   cd logibooks.scanner
   ```

### Debug Build

To build a debug version of the app:

```bash
./gradlew assembleDebug
```

The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

### Release Build

To build a release version (requires signing configuration):

```bash
./gradlew assembleRelease
```

**Note**: Release builds require the following environment variables to be set:
- `SIGNING_STORE_FILE`: Path to the keystore file
- `SIGNING_STORE_PASSWORD`: Keystore password
- `SIGNING_KEY_ALIAS`: Key alias
- `SIGNING_KEY_PASSWORD`: Key password

### Running Tests

```bash
./gradlew test
```

## Configuration

### Server URLs

<<<<<<< HEAD
<<<<<<< HEAD
The app connects to different servers based on build type.
=======
The app connects to different servers based on build type:

- **Debug**: `http://192.168.11.140:8080/`
- **Release**: `https://logibooks.sw.consulting:8085/`

>>>>>>> c4c0885 (doc: add README documentation (#40))
=======
The app connects to different servers based on build type.
>>>>>>> 8d0a449 (chore: refactor README by removing unnecessary sections)
These URLs are configured in `app/build.gradle.kts` and can be modified if needed.

### Application Version

The app version is managed in `gradle.properties`:
```properties
appVersionName=0.2.1
```

## How It Works

1. **Login**: Users authenticate with their email and password
2. **Job Selection**: After login, users can view and select available scan jobs
3. **Scanning**: With a job selected, users can activate scanning mode
4. **Hardware Scan**: Press the hardware scan key on the MT93 device to scan barcodes
5. **Results**: The app displays scan results with:
   - **Green**: Item found successfully
   - **Yellow**: No items found
   - **Orange**: Issues detected
   - Audio feedback in Russian

## API Integration

The app communicates with the Logibooks backend API:

- `POST /logibooks/api/login` - User authentication
- `GET /logibooks/api/scanjobs` - Fetch available scan jobs
- `GET /logibooks/api/scanjobs/ops` - Fetch job type descriptions
- `POST /logibooks/api/scan` - Submit scanned barcode

All authenticated requests include a JWT Bearer token in the Authorization header.

## Localization

The application supports multiple languages:
- English (default)
- Russian (ru)

User interface adapts to the device locale, with fallback to English.

## Development

### Code Style

The project follows the official Kotlin coding conventions:
```properties
kotlin.code.style=official
```

### Git Workflow

1. Create a feature branch from `main`
2. Make your changes
3. Run tests and linting
4. Submit a pull request

## License

Copyright (C) 2026 Maxim [maxirmx] Samsonov (www.sw.consulting)
All rights reserved.
This file is a part of LogiScanner application.

## Support

For issues, questions, or contributions, please contact the development team at www.sw.consulting

---

**Version**: 0.2.1  
**Last Updated**: February 2026
