# Stille Modus Agenda

Android-app die je telefoon automatisch op stil zet tijdens afspraken in je agenda, en het geluid
daarna weer aanzet.

**[⬇ Download de app (APK)](https://github.com/TiesvdSar/Phone_Silece_Modus_Agenda_App/releases/latest/download/StilleModusAgenda.apk)**

Alle versies staan bij [Releases](https://github.com/TiesvdSar/Phone_Silece_Modus_Agenda_App/releases).

## Wat de app doet

- Zet je telefoon op stil zodra een afspraak begint en weer terug als die voorbij is.
- Tijdens stil zijn oproepen en meldingen stil. Je wekker gaat wel gewoon af.
- Geannuleerde en geweigerde afspraken en afspraken op "beschikbaar" tellen niet mee.
- Heb je zelf "Niet storen" aan, of zet je tijdens een afspraak het geluid aan, dan blijft dat zo.
- Agendawijzigingen worden vanzelf opgepikt, ook als de app niet openstaat.

Per agenda stel je in:

- Welke kleuren meetellen
- Trefwoorden: alleen afspraken met bepaalde woorden, of juist nooit
- Marge: hoeveel minuten voor de start en na afloop de telefoon stil is
- Of hele-dagafspraken meetellen

## Installeren

Je hebt Android 8.0 of nieuwer nodig.

1. Download de APK op je telefoon via de link hierboven.
2. Open het bestand en sta installeren toe als Android daarom vraagt.
3. Open de app en loop de stappen in **Even instellen** door.
4. Zet op het hoofdscherm **Automatisch stil tijdens afspraken** aan.

**Samsung:** staat Auto Blocker aan, dan wordt de installatie geblokkeerd. Zet het tijdelijk uit via
Instellingen → Beveiliging en privacy → Auto Blocker, installeer de app en zet het daarna weer aan.

**Batterij:** sommige merken stoppen apps op de achtergrond, waardoor je telefoon te laat stil kan
worden. Zie de [instructies per merk](docs/onboarding/batterij-optimalisatie.md).

## Privacy

Je agenda blijft op je telefoon. De app heeft geen internettoestemming en verstuurt niets. Lees de
[privacyverklaring](docs/privacy/privacyverklaring.md).

## Zelf bouwen

Open `android/` in Android Studio, of bouw vanuit die map:

```
./gradlew assembleDebug
```

Gradle draait op JDK 21 (`android/gradle/gradle-daemon-jvm.properties`), omdat lint in AGP 8.7 niet
werkt op nieuwere JDK's.

Voor een ondertekende release maak je `android/keystore.properties` aan (staat niet in git):

```
storeFile=path/to/release.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Daarna bouw je met `./gradlew assembleRelease` of `./gradlew bundleRelease`.

## Projectstructuur

```
android/                         Android-app (Kotlin), fase 1
  app/src/main/java/app/stillemodus/
    calendar/                    Leest agenda's en afspraken via CalendarContract
    scheduling/                  SilencePlanner (filters en stille periodes), exacte alarmen,
                                 BootReceiver (herstart, klokwijzigingen),
                                 CalendarChangeJobService (plant opnieuw bij agendawijzigingen)
    sound/                       Stil zetten via de interruption filter (alleen alarmen);
                                 ringer-wijzigingen worden op recente Android-versies genegeerd
                                 voor apps op de achtergrond
    settings/                    Regels per agenda en app-status (SharedPreferences)
    ui/onboarding/               Stappen voor de toestemmingen
    ui/settings/                 Instellingen per agenda
  app/src/main/res/              Layouts, teksten (nl/en), thema (licht/donker), icoon
  app/src/test/                  Unit tests (filters, planning, stilte-regels)
  app/src/androidTest/           Tests op een apparaat
ios/                             Fase 2, nog niet gestart (Shortcuts/Focus-aanpak)
docs/
  onboarding/                    Batterij-instructies per merk
  privacy/                       Privacyverklaring
```
