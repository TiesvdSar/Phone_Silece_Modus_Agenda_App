# Phone_Silece_Modus_Agenda_App
This is a right now adriod only app that check the agenda and if something is planned then the phone goes automaticly to silence modus.

## Project structure

```
android/                         Native Android app (Kotlin) — phase 1
  gradle/                        Version catalog (libs.versions.toml) / wrapper
  app/src/main/java/app/stillemodus/
    calendar/                    Reads calendars and events via CalendarContract
    scheduling/                  SilencePlanner (filters and silence windows), AlarmManager exact
                                 alarms, AlarmReceiver, BootReceiver (reboot, clock changes),
                                 CalendarChangeJobService (re-plans when the calendar changes)
    sound/                       Silences via the interruption filter (alarms only); ringer mode
                                 changes are ignored for background apps on recent Android
    settings/                    Per-calendar rules and app state (SharedPreferences, readable
                                 synchronously from receivers)
    ui/onboarding/               Permission steps: READ_CALENDAR, DND access, exact alarms,
                                 battery optimisation
    ui/settings/                 Per-calendar settings: colors, keywords, margin, all-day events
  app/src/main/res/              Resources (layouts, strings nl/en, theme light/dark, icon)
  app/src/test/                  Unit tests (filters, planning, silence rules)
  app/src/androidTest/           Instrumented tests on device
ios/                             Phase 2 — placeholder (Shortcuts/Focus approach, see kickoff)
docs/
  onboarding/                    Per-manufacturer battery optimisation instructions
  privacy/                       Privacy policy (needed for Play Store publication)
Stille Modus Agenda App/         Project notes (kickoff, MOC)
```

## Release build

Gradle runs on JDK 21 (`android/gradle/gradle-daemon-jvm.properties`); lint in AGP 8.7 fails on
newer JDKs. To sign a release, create `android/keystore.properties` (kept out of git):

```
storeFile=path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Then run `./gradlew assembleRelease` or `bundleRelease` from `android/`.
