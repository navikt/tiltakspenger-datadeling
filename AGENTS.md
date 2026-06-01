# AGENTS.md — tiltakspenger-datadeling

Dette repoet følger monorepo-konvensjonene i [`../AGENTS.md`](../AGENTS.md) og Kotlin/JVM-backendkonvensjonene i [`../AGENTS-backend.md`](../AGENTS-backend.md). Les disse først.

## Repo-spesifikke regler

### Grensetyper og DTO-eierskap

- Hvert endepunkt eier sine egne grensetyper så langt det er praktisk: DTO-er, request/response-klasser og `PostgresRepo`-er bør ligge nær endepunktet / feature-området som bruker dem.
- Når en DTO kun brukes av én route, bør den fortrinnsvis ligge som en `private data class` i samme route-fil. Dette gjelder spesielt interne request-DTO-er med lokal `toDomain`-mapping.
- Route-DTO-er som serialiseres/deserialiseres til JSON, navngis `*RequestDTO` / `*ResponseDTO` for top-level request/response; nøstede JSON-typer ender på `DTO`.
- DTO-er skal bruke primitive / JSON-nære typer. Bruk `List` der API-et serialiserer/deserialiserer et JSON-array. `LocalDate` / `LocalDateTime` er greit for dato-/tidsfelter; `String` er også fint.
- DTO-er som eier andre DTO-er, kan neste dem i body.
- DTO-er og DTO-mapping eies av `infra` (jf. [`../AGENTS-backend.md`](../AGENTS-backend.md)). Ikke legg `toDTO` / `tilDTO`-funksjoner i domenelaget; legg mapping nær DTO-en som eier response-/request-kontrakten — f.eks. som private mapper-funksjoner i samme fil.

### Lagdeling

> Lagdelingsreglene i [`../AGENTS-backend.md`](../AGENTS-backend.md) gjelder (domenelaget skal ikke importere `*.infra.*`). Punktene under er datadeling-spesifikke presiseringer.

- **Allerede på to-lags-målbildet:** Dette repoet følger to-lags-målbildet [`../AGENTS-backend.md`](../AGENTS-backend.md) beskriver — service-filene ligger rett i **domene-/feature-pakken** (f.eks. `vedtak/HentTpVedtakService.kt`), ikke i et eget `service/`-lag. Behold dette; ikke gjeninnfør et separat `service/`-lag. Services returnerer domenetyper; mapping til DTO-er gjøres i `infra`-laget som eier route-en/responsen.
- En service skal som regel ha **én** public/internal funksjon. Det samme gjelder route-filer: én public/internal route-funksjon per fil. Hjelpefunksjoner og lokal mapping skal være `private`.
- Følg command-query / CQRS-separasjon der det gir mening: write/innkommende modeller går gjennom en domene-kommando eid av domenelaget; route-`RequestDTO`-er eier mappingen fra JSON til kommandoen.


### Testing

- Foretrekk ende-til-ende-tester som kjører fra route-en: send en JSON-streng til endepunktet og asserter på JSON-strengen som kommer tilbake. Ikke deserialiser responsen i route-tester når det er JSON-kontrakten som skal verifiseres.
- Route-tester skal som regel teste DTO-kontrakten indirekte. Direkte DTO-/schema-tester er fortsatt fine der de gir ekstra verdi (OpenAPI-synk, kompleks mapping). Private route-DTO-er kan refereres ved klassenavn i slike tester i stedet for å gjøres public utelukkende for testens skyld.
- Test mot en ekte database der det er mulig, særlig for full dekning av `PostgresRepo`. Når mange like tester dekker det samme i `PostgresRepo`, kan fakes brukes for varianter og edge cases.
- Fakes skal emulere tilsvarende `PostgresRepo`. Mer komplekse fakes bør ha egne tester.
- Når `PostgresRepo` testes direkte, bør tilsvarende fake shadow-testes med samme scenario og forventes å gi samme resultat.

### OpenAPI

- Hold OpenAPI-komponentskjemaer og DTO-er i synk. Oppdater `ComponentSkjemaVsDtoTest` når nye komponentskjemaer eller route-spesifikke DTO-er legges til.
- Kjør i det minste de relevante route-/schema-testene og `spotlessCheck` etter endringer i DTO-er, routes eller OpenAPI.

