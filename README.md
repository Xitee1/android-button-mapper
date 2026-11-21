# Button Mapper

Small Android app that reads hardware buttons directly from `/dev/input/event*` via
**[Shizuku](https://shizuku.rikka.app/)** and maps gestures (single press, double press, press and hold)
to actions. Original goal: the Assistant button (`KEY_ASSISTANT` on `gpio-keys`) of the
Motorola Edge 60 Pro toggles the flashlight.

The UI is available in English and German and follows the system language.

## Setup

1. Install and start Shizuku.
2. Open the app, turn on "Mapper active" and grant the Shizuku permission.
3. "Add mapping" → press the button → choose gesture and action.
4. Optionally enable "Block normal button function" in the mapping. Android then no longer reacts
   to the button itself. To do this the app grabs the button's whole input device (`EVIOCGRAB`)
   while the service is running and the mapping is active. If the device contains other buttons,
   the app lists them as a warning. Devices containing the power button are never grabbed.

Optional: under "Flashlight", the flashlight can turn off automatically after a set time when a
mapping switched it on.

The "Detected devices" list is informational: it shows all input devices, their buttons and whether
they are currently blocked.

## Structure

| Package | Purpose |
|---|---|
| `shizuku/EventReaderService` | Runs as a Shizuku user service (shell uid), reads evdev via `poll`, optionally grabs devices |
| `shizuku/EventReaderClient` | Binds the user service from the app |
| `gesture/GestureEngine` | Per-button state machine: single / double / hold |
| `model/Config` | Mappings, actions (`ActionConfig`), timing, flashlight settings; stored as JSON in DataStore |
| `action/ActionExecutor` | Runs actions |
| `service/MapperService` | Foreground service tying everything together; restarts after boot and app updates |
| `ui/` | Compose UI; strings in `res/values` (English) and `res/values-de` (German) |

**Adding an action:** add a subclass to `ActionConfig`, list it in `ActionConfig.all`, handle it in
`ActionExecutor.execute` and add a label in `ui/Labels.kt` plus both `strings.xml` files.

## Build

```bash
./gradlew :app:assembleDebug
```

Release signing: locally via `keystore.properties` (not in the repo); in GitHub Actions via the
secrets `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.
Every push to `main` builds a signed APK as a workflow artifact; a `v*` tag also creates a release.
