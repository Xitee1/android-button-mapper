# Button Mapper

Small Android app that reads hardware buttons directly from `/dev/input/event*` via
**[Shizuku](https://shizuku.rikka.app/)** and maps gestures (single press, double press, press and hold)
to actions. Original goal: the Assistant button (`KEY_ASSISTANT` on `gpio-keys`) of the
Motorola Edge 60 Pro toggles the flashlight.

The UI is available in English and German and follows the system language.

## Setup

1. Install and start Shizuku.
2. Open the app, turn on "Mapper active" and grant the Shizuku permission.
3. "Add button" → press the button → "Continue".
4. On the button page, pick a gesture (single press, double press, press and hold) and add one or
   more actions; they run in order. Each action has its own settings page, e.g. auto-off for the
   flashlight or the pattern for the vibration feedback.
5. Optionally enable "Block normal button function" on the button page. Android then no longer
   reacts to the button itself. To do this the app grabs the button's whole input device
   (`EVIOCGRAB`) while the service is running and the button is active. If the device contains
   other buttons, the app lists them as a warning. Devices containing the power button are never
   grabbed.

Settings (gear icon) holds the detection timing and diagnostics, including all input devices with
their buttons and whether they are currently blocked.

## Screens

Home (status + configured buttons) → button page (general switches + one entry per gesture) →
gesture page (list of actions) → action page (settings of one action). Settings and "Add button" are separate pages. Navigation
uses Navigation Compose with type-safe routes (`ui/Routes.kt`).

## Structure

| Package | Purpose |
|---|---|
| `shizuku/EventReaderService` | Runs as a Shizuku user service (shell uid), reads evdev via `poll`, optionally grabs devices |
| `shizuku/EventReaderClient` | Binds the user service from the app |
| `gesture/GestureEngine` | Per-button state machine: single / double / hold |
| `model/Config` | Buttons (`ButtonConfig`) with one action per gesture, actions with their own settings (`ActionConfig`), timing; stored as JSON in DataStore |
| `action/ActionExecutor` | Runs actions |
| `service/MapperService` | Foreground service tying everything together; restarts after boot and app updates |
| `ui/` | Compose UI, one package per page; strings in `res/values` (English) and `res/values-de` (German) |

**Adding an action:** add a subclass to `ActionConfig` and a default instance to
`ActionConfig.defaults`, handle it in `ActionExecutor.execute`, add a label and summary in `ui/Labels.kt` plus
both `strings.xml` files and, if it has settings, a section in `ui/action/ActionSettings.kt`.

## Build

```bash
./gradlew :app:assembleDebug
```

Release signing: locally via `keystore.properties` (not in the repo); in GitHub Actions via the
secrets `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.
Every push to `main` builds a signed APK as a workflow artifact; a `v*` tag also creates a release.
