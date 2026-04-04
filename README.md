# Taskzio

[![Android API](https://img.shields.io/badge/API-34%2B-brightgreen.svg)](https://android-arsenal.com/api?level=34)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Compose-Multiplatform-purple.svg)](https://developer.android.com/compose)

**Taskzio** is a high-performance productivity hub for Android, designed to bring your tasks, finances, and notes into a single, unified experience. No more jumping between five different apps to manage your day — Taskzio handles it all with a focus on speed, privacy, and a premium "Midnight" aesthetic.

---

## Key Modules

*   **Tasks**: Simple but powerful task management with priority levels and smooth swipe-to-delete interactions.
*   **Money**: Track your spending and income with intuitive category-based logging and visual donut charts.
*   **Savings**: Set financial goals and monitor your progress with real-time percentage tracking and daily reminders.
*   **Notes**: A clean, distraction-free space for your thoughts, supporting rich text and quick edits.
*   **Calendar**: A birds-eye view of your month, integrating your events and deadlines into a stable, scrollable grid.
*   **Cloud Sync**: Secure, encrypted backups to your personal Google Drive via the modern Google Credential Manager.

## The Tech Stack

Taskzio is built from the ground up using the latest Android industry standards:

- **UI**: 100% Jetpack Compose for a modern, reactive interface.
- **Database**: Room with **SQLCipher** for full database encryption at rest.
- **Dependency Injection**: Hilt for clean, testable architectural layers.
- **Async Pattern**: Kotlin Coroutines & Flow for a non-blocking, fluid UI.
- **Background Work**: WorkManager & AlarmManager for reliable reminders and sync.
- **Storage**: DataStore (Preferences) for lightweight settings and theme management.
- **Auth**: Google Identity Services (Credential Manager) for seamless cloud integration.

## Architecture

The project follows a **Clean MVVM** approach:
- **UI Layer**: Compose screens and state-aware ViewModels.
- **Domain Layer**: Repository pattern to decouple data sources from the UI.
- **Data Layer**: Room DAOs, DataStore, and external API integrations (Google Drive).

## Security First

Privacy isn't an afterthought here. Your data is encrypted locally using a 256-bit AES key managed by the Android Keystore. When you sync to the cloud, it's your personal Google Drive — no third-party servers, no tracking.

## Getting Started

1.  Clone the repository.
2.  Open in **Android Studio Hedgehog** (or newer).
3.  Ensure you have a `google-services.json` in the `app/` folder (or use the provided mock for testing).
4.  Build and run on an API 34+ device.

---

Designed and developed by **Ananmay Jha**.
