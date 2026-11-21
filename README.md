# Button Mapper

Kleine Android-App, die Hardware-Tasten per **Shizuku** direkt aus `/dev/input/event*` liest und
Gesten (einfach, doppelt, gedrückt halten) auf Aktionen legt. Ursprüngliches Ziel: die
Assistant-Taste (`KEY_ASSISTANT` auf `gpio-keys`) des Motorola Edge 60 Pro schaltet die Taschenlampe.

## Einrichtung

1. [Shizuku](https://shizuku.rikka.app/) installieren und starten.
2. App öffnen, „Mapper aktiv“ einschalten, Shizuku-Berechtigung erteilen.
3. „Zuordnung hinzufügen“ → Taste drücken → Geste und Aktion wählen.
4. Optional in der Zuordnung „Systemfunktion der Taste blockieren“ einschalten. Dann reagiert Android
   nicht mehr selbst auf die Taste. Technisch sperrt die App dafür das ganze Eingabegerät der Taste
   (EVIOCGRAB), solange der Dienst läuft und die Zuordnung aktiv ist. Enthält das Gerät weitere
   Tasten, zeigt die App sie als Warnung an. Geräte mit Power-Taste werden nie gesperrt.

Die Liste „Erkannte Geräte“ dient nur zur Info: Sie zeigt alle Eingabegeräte, ihre Tasten und ob sie
gerade gesperrt sind.

## Aufbau

| Paket | Aufgabe |
|---|---|
| `shizuku/EventReaderService` | Läuft als Shizuku-User-Service (Shell-UID), liest evdev per `poll`, optional mit Grab |
| `shizuku/EventReaderClient` | Bindet den User-Service aus der App |
| `gesture/GestureEngine` | Zustandsautomat je Taste: einfach / doppelt / halten |
| `model/Config` | Zuordnungen, Aktionen (`ActionConfig`), Zeiten; als JSON in DataStore |
| `action/ActionExecutor` | Führt Aktionen aus |
| `service/MapperService` | Vordergrunddienst, verbindet alles; startet nach Boot/Update neu |
| `ui/` | Compose-Oberfläche |

**Neue Aktion hinzufügen:** Unterklasse in `ActionConfig` anlegen, in `ActionConfig.all` eintragen und
im `when` von `ActionExecutor.execute` behandeln.

## Build

```bash
./gradlew :app:assembleDebug
```

Release-Signatur: lokal über `keystore.properties` (nicht im Repo), in GitHub Actions über die
Secrets `SIGNING_KEYSTORE_BASE64`, `SIGNING_STORE_PASSWORD`, `SIGNING_KEY_ALIAS`, `SIGNING_KEY_PASSWORD`.
Jeder Push auf `main` baut eine signierte APK als Artefakt, ein Tag `v*` erzeugt zusätzlich ein Release.
