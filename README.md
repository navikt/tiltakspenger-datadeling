tiltakspenger-datadeling
========================

tiltakspenger-datadeling er en backend tjeneste som skal svare på spørringer om data fra tiltakspenger. Tjenesten er en del av satsningen ["Flere i arbeid – P4"](https://memu.no/artikler/stor-satsing-skal-fornye-navs-utdaterte-it-losninger-og-digitale-verktoy/)

# Hvordan bruke tjenesten
Det er tre endepunkter som er tilgjengelig i tjenesten. Alle tjenestene kalles som en post med json body.
eksempel på body :
```json
{
  "ident": "12345678901",
  "fom": "2021-01-01",
  "tom": "2021-12-31"
}
```

Ident er obligatorisk. Fom og tom er valgfritt. Hvis fom og tom ikke er satt vil tjenesten returnere alle perioder/vedtak for brukeren. Man kan sette kun fom eller kun tom hva man vil ha alle perioder/vedtak fra en dato eller til en dato. 

Det finnes swagger for apiene [her](https://tiltakspenger-datadeling.intern.dev.nav.no/swagger).

### Endepunkter
- `/vedtak/perioder`
  - Returnerer en liste av perioder for en bruker som har fått tiltakspenger
- `/vedtak/detaljer`
  - Returnerer en liste av vedtak for en bruker som har fått tiltakspenger
- `/behandlinger/perioder`
  - Returnerer en liste av behandlinger som er starte å behandle i ny løsning for en bruker. Denne henter ikke behandlinger fra Arena.


eksempel på svar fra hent vedtak detaljer endepunktet:
```json
[
  {
    "fom": "2020-01-01",
    "tom": "2024-12-31",
    "antallDager": 10.0,
    "dagsatsTiltakspenger": 285,
    "dagsatsBarnetillegg": 0,
    "antallBarn": 0,
    "relaterteTiltak": 1234,
    "rettighet": "TILTAKSPENGER",
    "vedtakId": "123",
    "sakId": "123",
    "saksnummer": "202408271001",
    "kilde": "tp"
  }
]
```

---

eksempel på svar fra hent vedtak perioder-endepunktet
```json
[
  {
    "vedtakId": "id",
    "rettighet": "TILTAKSPENGER_OG_BARNETILLEGG",
    "periode": {
      "fraOgMed": "2023-07-01",
      "tilOgMed": "2023-11-01"
    },
    "kilde": "ARENA",
    "barnetillegg": {
      "perioder": [
        {
          "antallBarn": 1,
          "periode": {
            "fraOgMed": "2023-07-01",
            "tilOgMed": "2023-11-01"
          }
        }
      ]
    }
  }
]
```

---

eksempel på svar fra hent behandling perioder endepunktet
```json
[
  {
    "behandlingId": "beh_01HSTQVPVR7GB9TBC8V5HMSJ5Z",
    "fom": "2020-01-01",
    "tom": "2024-12-31"
  }
]
```


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
