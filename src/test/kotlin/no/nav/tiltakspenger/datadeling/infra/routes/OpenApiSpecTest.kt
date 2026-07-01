package no.nav.tiltakspenger.datadeling.infra.routes
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test

/**
 * Verifiserer at den bundlede OpenAPI-specen (generert av Gradle-tasken
 * `bundleOpenApi` fra kildene under src/main/openapi) havner på classpath
 * og serveres korrekt av Swagger-ruten.
 */
internal class OpenApiSpecTest {

    /**
     * Alle paths som skal være definert i specen. Hvis du legger til / fjerner
     * et endepunkt, oppdater listen her slik at bundling-testen fanger det opp.
     */
    private val forventedePaths = listOf(
        "/arena/meldekort",
        "/arena/utbetalingshistorikk",
        "/arena/utbetalingshistorikk/detaljer",
        "/behandlinger/perioder",
        "/behandlinger/apne",
        "/vedtak/detaljer",
        "/vedtak/perioder",
        "/vedtak/tidslinje",
        "/vedtak/sak",
        "/meldekort/detaljer",
    )

    /**
     * Alle skjemaer som skal være definert i components. Når vi splitter ut
     * skjemaene til egne filer, skal de fortsatt ende opp her etter bundling.
     */
    private val forventedeSkjemaer = listOf(
        "VedtakReqDTO",
        "VedtakSakReqDTO",
        "BehandlingRequest",
        "MappingError",
        "BehandlingResponse",
        "TpsakBehandlingRespons",
        "TpsakBehandlingSak",
        "TpsakBehandling",
        "HentSakResponse",
        "VedtakDetaljerResponse",
        "Periode",
        "BarnetilleggPeriode",
        "Barnetillegg",
        "VedtakDTO",
        "VedtakTidslinjeResponse",
        "VedtakTidslinjeSak",
        "VedtakResponse",
        "MeldekortResponse",
        "MeldekortKlartTilUtfylling",
        "GodkjentMeldekort",
        "MeldekortDag",
        "ArenaMeldekort",
        "ArenaMeldekortPeriode",
        "ArenaMeldekortDag",
        "ArenaUtbetalingshistorikk",
        "ArenaUtbetalingshistorikkDetaljer",
        "Vedtakfakta",
        "Anmerkning",
    )

    @Test
    fun `den bundlede OpenAPI-filen ligger på classpath og inneholder alle paths og skjemaer`() {
        val spec = lesSpecFraClasspath()

        forventedePaths.forEach { path ->
            withClue("Forventet path '$path' i bundled OpenAPI-spec") {
                spec.contains("$path:") shouldBe true
            }
        }
        forventedeSkjemaer.forEach { skjema ->
            withClue("Forventet skjema '$skjema' under components.schemas i bundled OpenAPI-spec") {
                spec.contains("\n    $skjema:") shouldBe true
            }
        }

        // Alle eksterne filreferanser skal være løst opp til interne refs.
        val eksterneRefs = Regex("""${'$'}ref:\s*["']([^"'#][^"']*)["']""").findAll(spec).toList()
        withClue("Forventet kun interne \$ref (#/...) etter bundling, men fant: " + eksterneRefs.joinToString { it.value }) {
            eksterneRefs.isEmpty() shouldBe true
        }
    }

    @Test
    fun `GET swagger documentation yaml returnerer den bundlede specen`() = testApplication {
        application {
            routing { swaggerRoute() }
        }

        val response = client.get("/swagger/documentation.yaml")

        response.status shouldBe HttpStatusCode.OK
        val contentType = response.contentType()
        withClue("Mangler Content-Type på swagger-responsen") {
            contentType shouldNotBe null
        }
        val resolvedContentType = requireNotNull(contentType)
        withClue("Forventet yaml content-type, fikk: $resolvedContentType") {
            (resolvedContentType.match(ContentType.parse("application/yaml")) || resolvedContentType.match(ContentType.parse("text/yaml"))) shouldBe true
        }
        val body = response.bodyAsText()
        withClue("Forventet OpenAPI-dokument, fikk: ${body.take(80)}") {
            body.startsWith("openapi:") shouldBe true
        }
        forventedePaths.forEach { path ->
            withClue("Forventet path '$path' i HTTP-respons") {
                body.contains("$path:") shouldBe true
            }
        }
    }

    private fun lesSpecFraClasspath(): String {
        val url = this::class.java.classLoader.getResource("openapi/documentation.yaml")
        withClue("Fant ikke openapi/documentation.yaml på classpath – kjør `./gradlew bundleOpenApi` eller `./gradlew processResources` først.") {
            url shouldNotBe null
        }
        return requireNotNull(url).readText()
    }

    private fun io.ktor.client.statement.HttpResponse.contentType(): ContentType? =
        headers["Content-Type"]?.let { ContentType.parse(it) }
}
