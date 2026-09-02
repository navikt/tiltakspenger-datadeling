# AGENTS.md — tiltakspenger-datadeling

Dette repoet følger monorepo-konvensjonene i AGENTS.md og Kotlin/JVM-backendkonvensjonene i AGENTS-backend.md — begge i metarepoet `tiltakspenger` (ligger som `..` når repoet er klonet inn i monorepoet, eller som `../tiltakspenger` når dette repoet er klonet for seg selv).
Les disse først.

## Repo-spesifikke regler

### Grensetyper og DTO-eierskap

- Hvert endepunkt eier sine egne grensetyper så langt det er praktisk: DTO-er, request/response-klasser og `PostgresRepo`-er bør ligge nær endepunktet / feature-området som bruker dem.
- Når en DTO kun brukes av én route, bør den fortrinnsvis ligge som en `private data class` i samme route-fil.
  Dette gjelder spesielt interne request-DTO-er med lokal `toDomain`-mapping.
- Route-DTO-er som serialiseres/deserialiseres til JSON, navngis `*RequestDTO` / `*ResponseDTO` for top-level request/response; nøstede JSON-typer ender på `DTO`.
- DTO-er skal bruke primitive / JSON-nære typer.
  Bruk `List` der API-et serialiserer/deserialiserer et JSON-array.
  `LocalDate` / `LocalDateTime` er greit for dato-/tidsfelter; `String` er også fint.
- DTO-er som eier andre DTO-er, kan neste dem i body.
- DTO-er og DTO-mapping eies av `infra` (jf. AGENTS-backend.md i metarepoet `tiltakspenger`).
  Ikke legg `toDTO` / `tilDTO`-funksjoner i domenelaget; legg mapping nær DTO-en som eier response-/request-kontrakten — f.eks. som private mapper-funksjoner i samme fil.

### Lagdeling

> Lagdelingsreglene i AGENTS-backend.md i metarepoet `tiltakspenger` gjelder (domenelaget skal ikke importere `*.infra.*`).
> Punktene under er datadeling-spesifikke presiseringer.

- **Allerede på to-lags-målbildet:** Dette repoet følger to-lags-målbildet AGENTS-backend.md i metarepoet `tiltakspenger` beskriver — service-filene ligger rett i **domene-/feature-pakken** (f.eks. `vedtak/HentVedtakDetaljerService.kt`), ikke i et eget `service/`-lag.
  Behold dette; ikke gjeninnfør et separat `service/`-lag.
  Services returnerer domenetyper; mapping til DTO-er gjøres i `infra`-laget som eier route-en/responsen.
- En service skal ha **én** public/internal funksjon.
  Det samme gjelder route-filer: én public/internal route-funksjon per fil.
  Hjelpefunksjoner og lokal mapping skal være `private`.
- **Ett endepunkt = én route-fil + én service + ett navn.**
  Route-funksjon, fil og service navngis likt og etter endepunktet: `POST /vedtak/perioder` → `HentVedtakPerioderRoute.kt` med `hentVedtakPerioderRoute`, som kaller `HentVedtakPerioderService.hentVedtakPerioder`.
  Route-filen skriver full sti i `post(...)`/`get(...)`; ikke bruk `route(...)`-prefiks, slik at stien kan leses direkte i fila som betjener den.
- Følg command-query / CQRS-separasjon der det gir mening: write/innkommende modeller går gjennom en domene-kommando eid av domenelaget; route-`RequestDTO`-er eier mappingen fra JSON til kommandoen.

### Feature-moduler og autentisering

- Hver feature eier en `*Module.kt` i sin `infra/routes`-pakke (`vedtakModule`, `behandlingModule`, `meldekortModule`, `arenaModule`, `sakModule`).
  Modulen tar `ApplicationContext`, setter auth-provider og lister opp featurens routes — både lese- og motta-endepunkter.
  `KtorSetup` kaller kun modulene; den kjenner ikke enkeltroutene.
  Ikke lag wiring-filer som spenner over flere features.
- Autentisering, rollesjekk, 403-respons og logging av kallet ligger i `infra/auth/medSystembruker`.
  Route-en kaller `medSystembruker(Systembrukerrolle.X) { systembruker -> ... }` og inneholder ellers bare det som er unikt for endepunktet.
  Ikke gjenta `call.systembruker(...)`-oppsettet i route-filene.


### Testing

- Foretrekk ende-til-ende-tester som kjører fra route-en: send en JSON-streng til endepunktet og asserter på JSON-strengen som kommer tilbake.
  Ikke deserialiser responsen i route-tester når det er JSON-kontrakten som skal verifiseres.
- Route-tester skal som regel teste DTO-kontrakten indirekte.
  Direkte DTO-/schema-tester er fortsatt fine der de gir ekstra verdi (OpenAPI-synk, kompleks mapping).
  Private route-DTO-er kan refereres ved klassenavn i slike tester i stedet for å gjøres public utelukkende for testens skyld.
- Test mot en ekte database der det er mulig, særlig for full dekning av `PostgresRepo`.
  Når mange like tester dekker det samme i `PostgresRepo`, kan fakes brukes for varianter og edge cases.
- Fakes skal emulere tilsvarende `PostgresRepo`.
  Mer komplekse fakes bør ha egne tester.
- Når `PostgresRepo` testes direkte, bør tilsvarende fake shadow-testes med samme scenario og forventes å gi samme resultat.

### OpenAPI

- Hold OpenAPI-komponentskjemaer og DTO-er i synk.
  Oppdater `ComponentSkjemaVsDtoTest` når nye komponentskjemaer eller route-spesifikke DTO-er legges til.
- Kjør i det minste de relevante route-/schema-testene og `spotlessCheck` etter endringer i DTO-er, routes eller OpenAPI.

