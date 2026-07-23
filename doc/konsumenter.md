# Konsumenter av tiltakspenger-datadeling

Denne oversikten dokumenterer hvem som konsumerer API-et, hvilke endepunkter de kaller, og hvordan klientene deres håndterer feilresponser.
Kilden til *hvem som har tilgang* er `accessPolicy.inbound` i [.nais/nais-prod.yml](../.nais/nais-prod.yml); resten er verifisert ved kildekodegjennomgang av konsumentenes klienter.
Sist verifisert: 2026-07-23.

Bakgrunnen for feilhåndteringskolonnen: når vi endrer innholdet i feilresponser (f.eks. teksten i `feilmelding`), må vi vite om noen konsument tolker innholdet for å ta avgjørelser.
Konklusjonen per 2026-07-23 er at **ingen av de granskbare konsumentene deserialiserer eller matcher på `feilmelding`** — alle brancher kun på statuskode.
Eneste ikke-granskbare konsument er Arena (internt Bitbucket).

## Endepunkt → konsument

| Endepunkt | Konsumenter |
|---|---|
| `POST /vedtak/perioder` | behandlingsflyt (Kelvin), modiapersonoversikt-api, tilleggsstonader-integrasjoner *(+ muligens arena)* |
| `POST /vedtak/detaljer` | tilleggsstonader-integrasjoner, veilarbportefolje *(+ muligens arena)* |
| `POST /vedtak/tidslinje` | NKS/Salesforce via saas-proxy *(+ muligens arena)* |
| `POST /vedtak/sak` | NKS/Salesforce via saas-proxy *(+ muligens arena)* |
| `POST /behandlinger/apne` | NKS/Salesforce via saas-proxy *(+ muligens arena)* |
| `POST /behandlinger/perioder` | ingen kjent *(muligens arena)* |
| `POST /meldekort/detaljer` | NKS/Salesforce via saas-proxy |
| `POST /arena/meldekort` | NKS/Salesforce via saas-proxy |
| `POST /arena/utbetalingshistorikk` | NKS/Salesforce via saas-proxy |
| `GET /arena/utbetalingshistorikk/detaljer` | ingen kjent (crm-nks har konfigurasjon for den, men stien er ikke i saas-proxy-whitelisten) |
| Motta-endepunktene (`POST /sak`, `/vedtak`, `/behandling`, `/meldekort`, `/meldeperioder`) | tiltakspenger-saksbehandling-api |

«Muligens arena» skyldes at appen `arena` har rollene `les-vedtak` og `les-behandling` og dermed *kan* treffe alle `/vedtak/*`- og `/behandlinger/*`-endepunktene, men kildekoden er ikke tilgjengelig for verifisering — se [arena](#arena-teamarenanais) under.

## behandlingsflyt (Kelvin) — namespace `aap`

Henter tiltakspengeperioder til samordningsvurdering i AAP-saksbehandlingen.
Leser kun `rettighet`, `periode.fraOgMed`, `periode.tilOgMed` og `kilde` fra responsen.

- Klient: [TiltakspengerGatewayImpl.kt](https://github.com/navikt/aap-behandlingsflyt/blob/HEAD/repository/src/main/kotlin/no/nav/aap/behandlingsflyt/integrasjon/samordning/TiltakspengerGatewayImpl.kt)
- Eneste kallsted: [TiltakspengerInformasjonskrav.kt](https://github.com/navikt/aap-behandlingsflyt/blob/HEAD/behandlingsflyt/src/main/kotlin/no/nav/aap/behandlingsflyt/faktagrunnlag/delvurdering/samordning/andrestatligeytelservurdering/TiltakspengerInformasjonskrav.kt)
- Feilhåndtering (i biblioteket aap-kelvin-komponenter): [DefaultResponseHandler.kt](https://github.com/navikt/aap-kelvin-komponenter/blob/HEAD/httpklient/src/main/kotlin/no/nav/aap/komponenter/httpklient/httpclient/error/DefaultResponseHandler.kt) leser feil-body som rå tekst, og [HandleStatus.kt](https://github.com/navikt/aap-kelvin-komponenter/blob/HEAD/httpklient/src/main/kotlin/no/nav/aap/komponenter/httpklient/httpclient/HandleStatus.kt) brancher kun på statuskode (400 → `BadRequestHttpResponsException` med bodyteksten i exception-meldingen).

Robust mot endringer i feilmeldingstekst: ja — body havner kun i logg/exception-melding.

## modiapersonoversikt-api — namespace `personoversikt`

Viser tiltakspengeytelser til veiledere i Modia personoversikt.

- Klient (generert): [tjenestespesifikasjoner/tiltakspenger/pom.xml](https://github.com/navikt/modiapersonoversikt-api/blob/HEAD/tjenestespesifikasjoner/tiltakspenger/pom.xml) — openapi-generator **rett fra vår `documentation.yaml` på main**; endringer i spec-strukturen treffer dem ved neste bygg.
- Tjenestelag: [TiltakspengerService.kt](https://github.com/navikt/modiapersonoversikt-api/blob/HEAD/web/src/main/java/no/nav/modiapersonoversikt/consumer/tiltakspenger/TiltakspengerService.kt), oppsett i [TiltakspengerConfig.kt](https://github.com/navikt/modiapersonoversikt-api/blob/HEAD/web/src/main/java/no/nav/modiapersonoversikt/consumer/tiltakspenger/TiltakspengerConfig.kt)
- Feilhåndtering: generert av [api.mustache](https://github.com/navikt/modiapersonoversikt-api/blob/HEAD/tjenestespesifikasjoner/openapi-templates/libraries/jvm-okhttp/api.mustache) — 4xx kaster `ClientException` med kun statuskode og HTTP-frase; feil-body lagres som rå streng og deserialiseres aldri.

Robust mot endringer i feilmeldingstekst: ja — men merk avhengigheten til spec-strukturen (skjemaene i `src/main/openapi/`).

## tilleggsstonader-integrasjoner — namespace `tilleggsstonader`

Henter tiltakspengeperioder og -detaljer for saksbehandling av tilleggsstønader.

- Klient: [TiltakspengerClient.kt](https://github.com/navikt/tilleggsstonader-integrasjoner/blob/HEAD/src/main/kotlin/no/nav/tilleggsstonader/integrasjoner/tiltakspenger/TiltakspengerClient.kt), brukes fra [YtelseService.kt](https://github.com/navikt/tilleggsstonader-integrasjoner/blob/HEAD/src/main/kotlin/no/nav/tilleggsstonader/integrasjoner/ytelse/YtelseService.kt)
- Feilhåndtering (i tilleggsstonader-libs): [RestTemplateExtensions.kt](https://github.com/navikt/tilleggsstonader-libs/blob/HEAD/http-client/main/no/nav/tilleggsstonader/libs/http/client/RestTemplateExtensions.kt) parser kun RFC 7807-bodyer (krever `"detail"`-nøkkel, som vår `{"feilmelding"}` ikke har); feil svelges i `YtelseService` og deres konsumenter får `ResultatKilde.FEILET` med tom liste.

Robust mot endringer i feilmeldingstekst: ja — feil-bodyen vår parses aldri og sendes ikke videre.

## veilarbportefolje — namespace `obo`

Indekserer tiltakspengeytelser i oversikten over brukere under arbeidsrettet oppfølging.
Kafka-topicet `obo.ytelser-v1` (som denne appen publiserer til) er kun *trigger*; dataene hentes via REST.

- Klient: [TiltakspengerClient.kt](https://github.com/navikt/veilarbportefolje/blob/HEAD/src/main/java/no/nav/pto/veilarbportefolje/tiltakspenger/TiltakspengerClient.kt), flyt i [TiltakspengerService.kt](https://github.com/navikt/veilarbportefolje/blob/HEAD/src/main/java/no/nav/pto/veilarbportefolje/tiltakspenger/TiltakspengerService.kt) og [YtelserKafkaService.kt](https://github.com/navikt/veilarbportefolje/blob/HEAD/src/main/java/no/nav/pto/veilarbportefolje/ytelserkafka/YtelserKafkaService.kt)
- Feilhåndtering (i common-java-modules): [RestUtils.java](https://github.com/navikt/common-java-modules/blob/HEAD/rest/src/main/java/no/nav/common/rest/client/RestUtils.java) `throwIfNotSuccessful` leser aldri feil-bodyen — exception-meldingen bygges av statuskode og URL.

Robust mot endringer i feilmeldingstekst: ja — feil-body leses ikke i det hele tatt.

## NKS/Salesforce via saas-proxy — namespace `teamcrm` og `platforce`

Viser ytelsesinformasjon til NKS-veiledere i Salesforce.
De to saas-proxy-instansene er samme kodebase og fronter samme Salesforce-org (migrering fra teamcrm til platforce pågår hos dem); det finnes bare én Salesforce-klient.

- Proxy: [Application.kt](https://github.com/navikt/saas-proxy/blob/HEAD/src/main/kotlin/no/nav/saas/proxy/Application.kt) er ren passthrough — feilresponser videresendes byte-for-byte uten tolkning.
  Tillatte stier står i [whitelist/prod.json](https://github.com/navikt/saas-proxy/blob/HEAD/src/main/resources/whitelist/prod.json) under nøkkelen `tpts.tiltakspenger-datadeling`.
- Apex-klient: [NKS_TiltakspengerService.cls](https://github.com/navikt/crm-nks-integration/blob/HEAD/force-app/ytelser/tiltakspenger/classes/NKS_TiltakspengerService.cls) brancher kun på statuskode og kaster fast feiltekst («Kunne ikke hente informasjon om vedtak fra tiltakspenger»); rå respons går kun til logg.
  Deserialisering i [NKS_TiltakspengerController.cls](https://github.com/navikt/crm-nks-integration/blob/HEAD/force-app/ytelser/tiltakspenger/classes/NKS_TiltakspengerController.cls) bruker `JSON.deserialize`, som ignorerer ukjente felter.
  Callout-laget er [ApiController.cls](https://github.com/navikt/crm-platform-base/blob/HEAD/force-app/ApiController/classes/ApiController.cls) i crm-platform-base.
  Frontenden ([ytTiltakspengerYtelseInfo](https://github.com/navikt/crm-nks-integration/blob/HEAD/force-app/ytelser/tiltakspenger/lwc/ytTiltakspengerYtelseInfo/ytTiltakspengerYtelseInfo.js)) logger feil til konsoll og nuller visningen.

Robust mot endringer i feilmeldingstekst: ja — kun statuskode-branching, fast feiltekst mot bruker.

## arena — namespace `teamarenanais`

Arena har inbound-tilgang med rollene `les-vedtak` og `les-behandling` (ikke `les-meldekort`).
Kildekoden ligger i internt Bitbucket og kan ikke granskes fra GitHub; hvilke endepunkter som faktisk kalles, og hvordan feil håndteres, er derfor **ukjent**.
Endringer i feilresponser på `/vedtak/*` og `/behandlinger/*` bør avklares med Team Arena.

## tiltakspenger-saksbehandling-api (eget team)

Har kun rollen `lagre-tiltakspenger-hendelser` og bruker bare motta-endepunktene til å pushe saker, vedtak, behandlinger og meldekort inn i datadeling.
Kaller ingen av leseendepunktene.

## Vedlikehold av denne oversikten

- Nye/fjernede konsumenter fanges i `accessPolicy.inbound` i [.nais/nais-prod.yml](../.nais/nais-prod.yml) — hold denne fila i synk ved endringer der.
- For Salesforce er det whitelisten i saas-proxy som avgjør hvilke stier som er i bruk, ikke access policy alene.
- Re-verifisering av klientkode: `gh search code --owner navikt "tiltakspenger-datadeling"` (og varianter som `"tpts.tiltakspenger-datadeling"` for azure-scope) finner klientene; sjekk feilhåndteringen deres for parsing av `feilmelding`.
- Lenkene over bruker `blob/HEAD/` og følger dermed default-branchen i konsumentrepoene, men stiene kan flytte seg — verifiser ved behov.
