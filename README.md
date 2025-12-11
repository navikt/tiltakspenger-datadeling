tiltakspenger-datadeling
========================

tiltakspenger-datadeling er en backend tjeneste som skal svare på spørringer om data fra tiltakspenger. Tjenesten er en del av satsningen ["Flere i arbeid – P4"](https://memu.no/artikler/stor-satsing-skal-fornye-navs-utdaterte-it-losninger-og-digitale-verktoy/)

# Hvordan bruke tjenesten
Det er seks endepunkter som er tilgjengelig i tjenesten. Alle tjenestene kalles som en post med json body.
eksempel på body :
```json
{
  "ident": "12345678901",
  "fom": "2021-01-01",
  "tom": "2021-12-31"
}
```

Ident er obligatorisk. Fom og tom er valgfritt. Hvis fom og tom ikke er satt vil tjenesten returnere alle perioder/vedtak/meldekort for brukeren. Man kan sette kun fom eller kun tom hva man vil ha alle perioder/vedtak/meldekort fra en dato eller til en dato. 

Endepunktet `/behandlinger/apne` kalles med en body uten periode siden åpne revurderinger vil mangle periode:
```json
{
  "ident": "12345678901"
}
```

Det finnes swagger for apiene [her](https://tiltakspenger-datadeling.intern.dev.nav.no/swagger).

### Endepunkter
- `/vedtak/tidslinje`
  - Returnerer en tidslinje av gjeldende vedtak fra ny løsning (TPSAK), i tillegg til en liste med alle vedtakene fra TPSAK (inkludert avslag), og saksinformasjon fra TPSAK. Inneholder også en liste med vedtak fra Arena.
- `/vedtak/perioder`
  - Returnerer en liste av perioder for en bruker som har fått tiltakspenger (både Arena og ny løsning)
- `/meldekort/detaljer`
  - Returnerer to lister: Meldekort som er klare til utfylling og godkjente meldekort. Henter kun meldekort fra ny løsning (TPSAK).
- `/behandlinger/perioder`
  - Hovedsakelig tiltenkt Arena: Returnerer en liste av behandlinger som er starte å behandle i ny løsning for en bruker. Henter kun vedtak fra ny løsning (TPSAK).
- `/behandlinger/apne`
  - Returnerer en liste av åpne behandlinger for en bruker, samt saksinformasjon. Kan være søknadsbehandlinger, revurderinger og meldekortbehandlinger. Henter kun behandlinger fra ny løsning (TPSAK).
- `/vedtak/detaljer`
  - Returnerer en tidslinje av gjeldende, innvilget vedtak (vi har filtrert bort de periodene som ikke (lenger) gir rett). Henter kun vedtak fra ny løsning (TPSAK).


eksempel på svar fra hent vedtak tidslinje-endepunktet
```json
{
  "tidslinje": [
    {
      "vedtakId": "vedtakId5",
      "rettighet": "TILTAKSPENGER_OG_BARNETILLEGG",
      "periode": {
        "fraOgMed": "2024-06-01",
        "tilOgMed": "2024-08-01"
      },
      "barnetillegg": {
        "perioder": [
          {
            "antallBarn": 2,
            "periode": {
              "fraOgMed": "2024-06-01",
              "tilOgMed": "2024-08-01"
            }
          }
        ]
      },
      "vedtaksdato": "2024-06-01",
      "valgteHjemlerHarIkkeRettighet": null,
      "sats": 285,
      "satsBarnetillegg": 53,
      "vedtaksperiode": {
        "fraOgMed": "2024-06-01",
        "tilOgMed": "2024-08-01"
      },
      "innvilgelsesperioder": [
        {
          "fraOgMed": "2024-06-01",
          "tilOgMed": "2024-08-01"
        }
      ],
      "omgjortAvRammevedtakId": null,
      "omgjorRammevedtakId": null
    },
    {
      "vedtakId": "vedtakId3",
      "rettighet": "STANS",
      "periode": {
        "fraOgMed": "2024-02-01",
        "tilOgMed": "2024-03-01"
      },
      "barnetillegg": null,
      "vedtaksdato": "2024-02-01",
      "valgteHjemlerHarIkkeRettighet": [
        "DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK"
      ],
      "sats": null,
      "satsBarnetillegg": null,
      "vedtaksperiode": {
        "fraOgMed": "2024-02-01",
        "tilOgMed": "2024-03-01"
      },
      "innvilgelsesperioder": [],
      "omgjortAvRammevedtakId": null,
      "omgjorRammevedtakId": null
    },
    {
      "vedtakId": "vedtakId2",
      "rettighet": "TILTAKSPENGER",
      "periode": {
        "fraOgMed": "2024-01-03",
        "tilOgMed": "2024-01-31"
      },
      "barnetillegg": null,
      "vedtaksdato": "2024-01-03",
      "valgteHjemlerHarIkkeRettighet": null,
      "sats": 285,
      "satsBarnetillegg": 0,
      "vedtaksperiode": {
        "fraOgMed": "2024-01-01",
        "tilOgMed": "2024-01-31"
      },
      "innvilgelsesperioder": [
        {
          "fraOgMed": "2024-01-03",
          "tilOgMed": "2024-01-31"
        }
      ],
      "omgjortAvRammevedtakId": null,
      "omgjorRammevedtakId": "vedtakId"
    }
  ],
  "alleVedtak": [
    {
      "vedtakId": "vedtakId5",
      "rettighet": "TILTAKSPENGER_OG_BARNETILLEGG",
      "periode": {
        "fraOgMed": "2024-06-01",
        "tilOgMed": "2024-08-01"
      },
      "barnetillegg": {
        "perioder": [
          {
            "antallBarn": 2,
            "periode": {
              "fraOgMed": "2024-06-01",
              "tilOgMed": "2024-08-01"
            }
          }
        ]
      },
      "vedtaksdato": "2024-06-01",
      "valgteHjemlerHarIkkeRettighet": null,
      "sats": 285,
      "satsBarnetillegg": 53,
      "vedtaksperiode": {
        "fraOgMed": "2024-06-01",
        "tilOgMed": "2024-08-01"
      },
      "innvilgelsesperioder": [
        {
          "fraOgMed": "2024-06-01",
          "tilOgMed": "2024-08-01"
        }
      ],
      "omgjortAvRammevedtakId": null,
      "omgjorRammevedtakId": null
    },
    {
      "vedtakId": "vedtakId4",
      "rettighet": "AVSLAG",
      "periode": {
        "fraOgMed": "2024-04-01",
        "tilOgMed": "2024-05-01"
      },
      "barnetillegg": null,
      "vedtaksdato": "2024-04-01",
      "valgteHjemlerHarIkkeRettighet": [
        "INSTITUSJONSOPPHOLD"
      ],
      "sats": null,
      "satsBarnetillegg": null,
      "vedtaksperiode": {
        "fraOgMed": "2024-04-01",
        "tilOgMed": "2024-05-01"
      },
      "innvilgelsesperioder": [],
      "omgjortAvRammevedtakId": null,
      "omgjorRammevedtakId": null
    },
    {
      "vedtakId": "vedtakId3",
      "rettighet": "STANS",
      "periode": {
        "fraOgMed": "2024-02-01",
        "tilOgMed": "2024-03-01"
      },
      "barnetillegg": null,
      "vedtaksdato": "2024-02-01",
      "valgteHjemlerHarIkkeRettighet": [
        "DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK"
      ],
      "sats": null,
      "satsBarnetillegg": null,
      "vedtaksperiode": {
        "fraOgMed": "2024-02-01",
        "tilOgMed": "2024-03-01"
      },
      "innvilgelsesperioder": [],
      "omgjortAvRammevedtakId": null,
      "omgjorRammevedtakId": null
    },
    {
      "vedtakId": "vedtakId2",
      "rettighet": "TILTAKSPENGER",
      "periode": {
        "fraOgMed": "2024-01-03",
        "tilOgMed": "2024-03-01"
      },
      "barnetillegg": null,
      "vedtaksdato": "2024-01-03",
      "valgteHjemlerHarIkkeRettighet": null,
      "sats": 285,
      "satsBarnetillegg": 0,
      "vedtaksperiode": {
        "fraOgMed": "2024-01-01",
        "tilOgMed": "2024-03-01"
      },
      "innvilgelsesperioder": [
        {
          "fraOgMed": "2024-01-03",
          "tilOgMed": "2024-03-01"
        }
      ],
      "omgjortAvRammevedtakId": null,
      "omgjorRammevedtakId": "vedtakId"
    },
    {
      "vedtakId": "vedtakId",
      "rettighet": "TILTAKSPENGER",
      "periode": {
        "fraOgMed": "2024-01-01",
        "tilOgMed": "2024-03-01"
      },
      "barnetillegg": null,
      "vedtaksdato": "2024-01-01",
      "valgteHjemlerHarIkkeRettighet": null,
      "sats": 285,
      "satsBarnetillegg": 0,
      "vedtaksperiode": {
        "fraOgMed": "2024-01-01",
        "tilOgMed": "2024-03-01"
      },
      "innvilgelsesperioder": [
        {
          "fraOgMed": "2024-01-01",
          "tilOgMed": "2024-03-01"
        }
      ],
      "omgjortAvRammevedtakId": "vedtakId2",
      "omgjorRammevedtakId": null
    }
  ],
  "vedtakFraArena": [
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
      },
      "sats": 285,
      "satsBarnetillegg": 53,
      "vedtaksperiode": {
        "fraOgMed": "2023-07-01",
        "tilOgMed": "2023-11-01"
      },
      "innvilgelsesperioder": [
        {
          "fraOgMed": "2023-07-01",
          "tilOgMed": "2023-11-01"
        }
      ],
      "omgjortAvRammevedtakId": null,
      "omgjorRammevedtakId": null,
      "vedtakstidspunkt": null
    }
  ],
  "sak": {
    "sakId": "sak_01K74A8HYH2VPA49SG047M3D9F",
    "saksnummer": "202509051005",
    "kilde": "TPSAK",
    "status": "Løpende",
    "opprettetDato": "2023-01-01T12:00:00+01"
  }
}
```

---

eksempel på svar fra hent vedtak perioder-endepunktet
```json
[
  {
    "vedtakId": "arenavedtakId",
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
    },
    "sats": 285,
    "satsBarnetillegg": 53,
    "vedtaksperiode": {
      "fraOgMed": "2023-07-01",
      "tilOgMed": "2023-11-01"
    },
    "innvilgelsesperioder": [
      {
        "fraOgMed": "2023-07-01",
        "tilOgMed": "2023-11-01"
      }
    ],
    "omgjortAvRammevedtakId": null,
    "omgjorRammevedtakId": null,
    "vedtakstidspunkt": null
  },
  {
    "vedtakId": "tpsakvedtakId1",
    "rettighet": "TILTAKSPENGER",
    "periode": {
      "fraOgMed": "2024-01-01",
      "tilOgMed": "2024-01-31"
    },
    "kilde": "TPSAK",
    "barnetillegg": null,
    "sats": 285,
    "satsBarnetillegg": 0,
    "vedtaksperiode": {
      "fraOgMed": "2024-01-01",
      "tilOgMed": "2024-01-31"
    },
    "innvilgelsesperioder": [
      {
        "fraOgMed": "2024-01-01",
        "tilOgMed": "2024-01-31"
      }
    ],
    "omgjortAvRammevedtakId": "tpsakvedtakId2",
    "omgjorRammevedtakId": null,
    "vedtakstidspunkt": "2024-01-01T12:00:00+01"
  },
  {
    "vedtakId": "tpsakvedtakId2",
    "rettighet": "TILTAKSPENGER",
    "periode": {
      "fraOgMed": "2024-01-05",
      "tilOgMed": "2024-01-31"
    },
    "kilde": "TPSAK",
    "barnetillegg": null,
    "sats": 285,
    "satsBarnetillegg": 0,
    "vedtaksperiode": {
      "fraOgMed": "2024-01-01",
      "tilOgMed": "2024-01-31"
    },
    "innvilgelsesperioder": [
      {
        "fraOgMed": "2024-01-05",
        "tilOgMed": "2024-01-31"
      }
    ],
    "omgjortAvRammevedtakId": null,
    "omgjorRammevedtakId": "tpsakvedtakId1",
    "vedtakstidspunkt": "2024-01-05T17:00:00+01"
  }
]
```

---

eksempel på svar fra hent meldekort-detaljer-endepunktet
```json
{
  "meldekortKlareTilUtfylling": [
    {
      "id": "meldeperiode_01K74A8HYHC9EHEV295ZQMDHMY",
      "kjedeId": "2025-09-22/2025-10-05",
      "opprettet": "2025-10-09T13:03:53.553315",
      "fraOgMed": "2025-09-22",
      "tilOgMed": "2025-10-05",
      "maksAntallDagerForPeriode": 10,
      "girRett": {
        "2025-09-22": true,
        "2025-09-23": true,
        "2025-09-24": true,
        "2025-09-25": true,
        "2025-09-26": true,
        "2025-09-27": false,
        "2025-09-28": false,
        "2025-09-29": true,
        "2025-09-30": true,
        "2025-10-01": true,
        "2025-10-02": true,
        "2025-10-03": true,
        "2025-10-04": false,
        "2025-10-05": false
      },
      "kanFyllesUtFraOgMed": "2025-10-03"
    }
  ],
  "godkjenteMeldekort": [
    {
      "meldekortbehandlingId": "meldekort_01KC649JCSAAMSMD98941ZJXNG",
      "kjedeId": "2025-09-08/2025-09-21",
      "mottattTidspunkt": "2025-10-09T12:03:53.339068",
      "vedtattTidspunkt": "2025-10-09T13:03:53.339079",
      "behandletAutomatisk": true,
      "fraOgMed": "2025-09-08",
      "tilOgMed": "2025-09-21",
      "meldekortdager": [
        {
          "dato": "2025-09-08",
          "status": "DELTATT_UTEN_LONN_I_TILTAKET",
          "reduksjon": "INGEN_REDUKSJON"
        },
        {
          "dato": "2025-09-09",
          "status": "DELTATT_UTEN_LONN_I_TILTAKET",
          "reduksjon": "INGEN_REDUKSJON"
        },
        {
          "dato": "2025-09-10",
          "status": "DELTATT_UTEN_LONN_I_TILTAKET",
          "reduksjon": "INGEN_REDUKSJON"
        },
        {
          "dato": "2025-09-11",
          "status": "DELTATT_UTEN_LONN_I_TILTAKET",
          "reduksjon": "INGEN_REDUKSJON"
        },
        {
          "dato": "2025-09-12",
          "status": "DELTATT_UTEN_LONN_I_TILTAKET",
          "reduksjon": "INGEN_REDUKSJON"
        },
        {
          "dato": "2025-09-13",
          "status": "IKKE_TILTAKSDAG",
          "reduksjon": "YTELSEN_FALLER_BORT"
        },
        {
          "dato": "2025-09-14",
          "status": "IKKE_TILTAKSDAG",
          "reduksjon": "YTELSEN_FALLER_BORT"
        },
        {
          "dato": "2025-09-15",
          "status": "DELTATT_UTEN_LONN_I_TILTAKET",
          "reduksjon": "INGEN_REDUKSJON"
        },
        {
          "dato": "2025-09-16",
          "status": "DELTATT_UTEN_LONN_I_TILTAKET",
          "reduksjon": "INGEN_REDUKSJON"
        },
        {
          "dato": "2025-09-17",
          "status": "DELTATT_UTEN_LONN_I_TILTAKET",
          "reduksjon": "INGEN_REDUKSJON"
        },
        {
          "dato": "2025-09-18",
          "status": "DELTATT_UTEN_LONN_I_TILTAKET",
          "reduksjon": "INGEN_REDUKSJON"
        },
        {
          "dato": "2025-09-19",
          "status": "DELTATT_UTEN_LONN_I_TILTAKET",
          "reduksjon": "INGEN_REDUKSJON"
        },
        {
          "dato": "2025-09-20",
          "status": "IKKE_TILTAKSDAG",
          "reduksjon": "YTELSEN_FALLER_BORT"
        },
        {
          "dato": "2025-09-21",
          "status": "IKKE_TILTAKSDAG",
          "reduksjon": "YTELSEN_FALLER_BORT"
        }
      ],
      "status": "SENDT_TIL_UTBETALING",
      "journalpostId": "jpid",
      "totaltBelop": 4560,
      "sats": 298,
      "satsBarnetillegg": 55,
      "korrigering": null,
      "opprettet": "2025-10-09T13:03:53.339084",
      "sistEndret": "2025-10-09T13:03:53.339086"
    }
  ]
}
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

---

eksempel på svar fra hent åpne behandlinger-endepunktet
```json
{
  "behandlinger": [
    {
      "behandlingId": "meldekort_01K4CXBX8XEAHBX6SPZGH4355B",
      "fom": "2025-11-03",
      "tom": "2025-11-17",
      "behandlingstatus": "UNDER_BEHANDLING",
      "behandlingstype": "MELDEKORTBEHANDLING",
      "saksbehandler": "testSaksbehandler",
      "beslutter": null,
      "iverksattTidspunkt": null,
      "opprettet": "2025-11-03T00:00:00",
      "sistEndret": "2025-11-04T00:00:00"
    }
  ],
  "sak": {
    "sakId": "sak_01K74A8HYH2VPA49SG047M3D9F",
    "saksnummer": "202509051005",
    "kilde": "TPSAK",
    "status": "Løpende",
    "opprettetDato": "2023-01-01T12:00:00+01"
  }
}
```

---

eksempel på svar fra hent vedtak detaljer endepunktet:
```json
[
  {
    "fom": "2020-01-01",
    "tom": "2024-12-31",
    "rettighet": "TILTAKSPENGER",
    "vedtakId": "123",
    "sakId": "321",
    "saksnummer": "202408271001",
    "kilde": "tp",
    "sats": 285,
    "satsBarnetillegg": 0
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

### Hvordan teste endepunktene i dev?
1. Hent access-token: https://docs.nais.io/auth/entra-id/how-to/generate/#generate-token-for-application-user-client-credentials
    * `curl -s -X POST "https://azure-token-generator.intern.dev.nav.no/api/public/m2m" -d "aud=dev-gcp:tpts:tiltakspenger-datadeling"`
2. https://tiltakspenger-datadeling.intern.dev.nav.no/swagger (eller bruk httpie/curl/curlie/wget eller lignende)

----

# Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på GitHub.

## For NAV-ansatte

Interne henvendelser kan sendes via Slack i kanalen #tpts-tech.
