# Hang: Android Impulse-Control Protection System

Hang prevents impulsive changes to selected applications by introducing a
deliberate friction mechanism: before a protected action (uninstall, disable,
force stop, permission changes, and more) can proceed, the user must manually
type a self-chosen passage exactly as shown.

The goal is not to permanently block the user from controlling their device,
but to create a meaningful pause between impulse and action.

## How it works

1. An **AccessibilityService** (`ProtectionAccessibilityService`) monitors
   Settings, package-installer, and system UI screens.
2. When a screen relates to a **protected app** and a **protected action**,
   the service backs out of the screen and launches a full-screen
   **interruption** (`VerificationActivity`).
3. The user must type the configured passage into a hardened text field
   (`SecureEditText`) that blocks paste, autofill, suggestions, drag-and-drop,
   and bulk text insertion. There is no shortcut button.
4. On an exact match (case sensitivity is configurable), the action is
   **temporarily unlocked for 60 seconds** so it can be completed, after which
   protection resumes automatically.

## Protected actions

Uninstall, disable, hide, force stop, device-admin removal, permission
changes, "Display over other apps", accessibility access, notification
access, battery optimization, and usage access.

## Setup

1. Build and install the app (`./gradlew :app:assembleDebug`).
2. Open the app and tap **Open Accessibility Settings**, then enable
   **Hang Impulse-Control Protection**.
3. Select the protected actions and apps, define your verification passage
   (minimum 20 characters), and choose strict (case-sensitive) or relaxed
   matching.
4. Save. Changing the passage later requires typing the current passage first.

## Project structure

| File | Purpose |
| --- | --- |
| `ProtectionAccessibilityService.kt` | Detects protected-action screens |
| `VerificationActivity.kt` | Full-screen typed-confirmation interruption |
| `SecureEditText.kt` | Anti-bypass input field |
| `ConfigRepository.kt` | DataStore-backed configuration |
| `UnlockManager.kt` | Temporary unlock window after verification |
| `MainActivity.kt` | Setup and secured passage-update workflow |

## Known limitations

- Detection is text/keyword based and may need tuning per OEM Settings app.
- Android does not allow apps to hard-block system actions; the service
  relies on backing out of screens, so extremely fast actions may slip
  through on some devices.
- ADB / safe mode can bypass protection by design; the system targets
  impulsive in-device actions, not determined adversaries.
