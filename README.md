tiltakspenger-datadeling
========================

tiltakspenger-datadeling er en backend tjeneste som skal svare på spørringer om data fra tiltakspenger. Tjenesten er en del av satsningen ["Flere i arbeid – P4"](https://memu.no/artikler/stor-satsing-skal-fornye-navs-utdaterte-it-losninger-og-digitale-verktoy/)

# Hvordan bruke tjenesten
Det er to endepunkter som er tilgjengelig i tjenesten. Begge tjenestene kalles som en post med json body.
eksempel på body :
```json
{
  "ident": "12345678901", 
  "fom": "2021-01-01",
  "tom": "2021-12-31"
}
```


ident er obligatorisk. Fom og tom er valgfritt. Hvis fom og tom ikke er satt vil tjenesten returnere alle perioder/vedtak for brukeren.

### Endepunkter
- `/vedtak/perioder`
  - Returnerer en liste av perioder for en bruker som har fått tiltakspenger
- `/vedtak/detaljer`
  - Returnerer en liste av vedtak for en bruker som har fått tiltakspenger


eksempel på svar fra hent endepunktet:
```json
[
    {
    "fom":"2020-01-01",
    "tom":"2024-12-31",
    "antallDager":10.0,
    "dagsatsTiltakspenger":285,
    "dagsatsBarnetillegg":0,
    "antallBarn":0,
    "relaterteTiltak": 1234,
    "rettighet": "TILTAKSPENGER",
    "vedtakId": "123",
    "sakId": "123"
    }
    ]
```

[Json Schema](/doc/JsonSchemaDetaljer.json)

eksempel på svar fra hent perioder endepunktet
```json
[
    {
    "fom":"2020-01-01",
    "tom":"2024-12-31"
    }
    ]
```

[Json Schema](/doc/JsonSchemaPerioder.json)

# Komme i gang
## Forutsetninger
- [JDK](https://jdk.java.net/)
- [Kotlin](https://kotlinlang.org/)
- [Gradle](https://gradle.org/) brukes som byggeverktøy og er inkludert i oppsettet

For hvilke versjoner som brukes, [se byggefilen](build.gradle.kts)

## Bygging og denslags
For å bygge artifaktene:

```sh
./gradlew build
```

### Kjøre opp appen lokalt

Appen har alle miljøvariabler som trenger for lokal kjøring nedfelt i Configuration.kt, så det er ikke nødvendig å
sette egne miljøvariabler for å kjøre opp appen lokalt. Kjør som vanlig opp `main`-funksjonen i `Application.kt` for å kjøre
opp appen.

----

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #tpts-tech.
