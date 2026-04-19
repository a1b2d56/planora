# Planora

A native Android productivity hub that consolidates tasks, finances, and notes into a single, fast, offline-first application. Built entirely with Kotlin and Jetpack Compose.

## Core Capabilities

- **Notes & Handwriting**: Rich-text editing alongside a custom, low-latency, velocity-aware Canvas drawing engine for handwritten notes. Includes smart content-aware image exporting.
- **Financial Tracking**: Income, expenses, and savings goals tracked locally with dynamic Compose charts and category insights.
- **Task & Calendar Management**: Priority-layered task tracking with a custom scrollable month view.
- **Cloud Sync**: 100% serverless cloud backups directly to the user's personal Google Drive, via Google Credential Manager.
- **Security-First**: Full database encryption via SQLCipher ensures all financial and personal data remains protected at rest.

## Tech Stack

- **UI**: 100% Jetpack Compose (Dynamic Monet theming + true AMOLED Midnight mode)
- **Architecture**: Modular Monolith, Package-by-Feature, Clean MVVM
- **Local Storage**: Room + SQLCipher (Encrypted SQLite), Jetpack DataStore
- **Concurrency**: Coroutines & Flow
- **Dependency Injection**: Dagger Hilt
- **Background Tasks**: WorkManager
- **Authentication**: Google Identity Services

## Getting Started

1. Clone the repository.
2. Open the project in Android Studio (Hedgehog or newer).
3. Add your `google-services.json` to the `app/` directory (required for Authentication and Google Drive Sync).
4. Build and run. Target API level 34.

---
Designed and developed by **Ananmay Jha**.
