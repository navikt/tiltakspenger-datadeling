# AGENTS.md — tiltakspenger-datadeling

Dette repoet følger de generelle instruksene i [`../AGENTS.md`](../AGENTS.md). Les og følg dem først.

## Repo-spesifikke regler

- Hvert endepunkt skal eie sine egne boundary-typer så langt det er praktisk: DTO-er, request-/response-klasser og postgresrepoer skal ligge nær endepunktet/featureområdet som bruker dem.
- Når en DTO kun brukes av én route, bør den helst ligge som `private data class` i samme route-fil. Dette gjelder særlig interne mottaks-/request-DTO-er med lokal `toDomain`-mapping.
- Route-DTO-er som serialiseres/deserialiseres til JSON bør navngis med `*RequestDTO` eller `*ResponseDTO` for top-level request/response, og nested JSON-typer bør ende med `DTO`.
- DTO-er bør bruke primitive/JSON-nære typer. Bruk helst `List` der API-et skal serialisere/deserialisere et JSON-array. `LocalDate` og `LocalDateTime` er akseptert for dato/tid, men `String` er også greit for disse feltene.
- DTO-klasser som bruker og eier andre DTO-klasser kan gjerne ha disse som nested DTO-klasser i kroppen sin.
- Følg command-query/CQRS-skille der det er naturlig: mottaks-/skrivemodeller bør gå via en domenekommando eid av domenelaget, mens route-`RequestDTO`-er eier mappingen fra JSON til kommandoen.
- Bruk `Either.catch { ... }.mapLeft { ... }` fremfor lokal `try/catch` når exceptions skal gjøres om til domenefeil eller route-feil.
- Service-filer skal ligge i domenelaget, ikke i `infra`. Services skal returnere domenetyper; mapping til DTO-er skal gjøres i `infra`-laget som eier routen/responsen.
- En service bør som hovedregel ha én public/internal funksjon. Det samme gjelder route-filer: én public/internal route-funksjon per fil. Hjelpefunksjoner og lokal mapping bør være `private`.
- Alle DTO-er skal ligge i `infra`-pakker. Ikke legg DTO-er i domenelaget.
- Domenelaget skal ikke ha referanser til `infra`. Det betyr blant annet ingen imports fra `*.infra.*` i domene-pakker.
- DTO-mapping skal eies av `infra`. Ikke legg `toDTO`/`tilDTO`-funksjoner i domenelaget.
- DTO-mappinger bør ligge nær DTO-en som eier respons-/request-kontrakten, for eksempel som private mapper-funksjoner eller factory-funksjoner i samme fil/klasse som DTO-en.
- Hold OpenAPI component-skjemaer og DTO-er i synk. Oppdater `ComponentSkjemaVsDtoTest` når nye component-skjemaer eller route-spesifikke DTO-er legges til.
- Foretrekk end-to-end-tester som tester fra routen: send JSON-string til endepunktet og assert på JSON-stringen som kommer tilbake. Ikke deserialiser responsen i route-tester når JSON-kontrakten er det som skal verifiseres.
- Route-tester bør som hovedregel teste DTO-kontrakten indirekte. Direkte DTO-/skjematester er likevel greit der de gir ekstra verdi, for eksempel for OpenAPI-synk eller komplisert mapping. Private route-DTO-er kan refereres med klassenavn i slike tester i stedet for å gjøres public kun for testen.
- Test helst mot ekte database, særlig for å få full dekning av `PostgresRepo`. Hvis mange lignende tester dekker det samme i `PostgresRepo`, er det greit å bruke fakes for variasjoner og edge cases.
- Fakes bør emulere tilsvarende `PostgresRepo`. Mer kompliserte fakes bør ha egne tester.
- Når `PostgresRepo` testes direkte, bør tilsvarende fake skyggetestes med samme scenario og forventes å gi samme svar.
- Kjør minst relevante route-/skjema-tester og `spotlessCheck` etter endringer i DTO-er, routes eller OpenAPI.
