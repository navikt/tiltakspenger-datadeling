package no.nav.tiltakspenger.datadeling.routes

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
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
        "TpsakBehandling",
        "Sak",
        "HentSakResponse",
        "VedtakDetaljerResponse",
        "Periode",
        "BarnetilleggPeriode",
        "Barnetillegg",
        "VedtakDTO",
        "VedtakTidslinjeResponse",
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
            assertTrue(
                spec.contains("$path:"),
                "Forventet path '$path' i bundled OpenAPI-spec",
            )
        }
        forventedeSkjemaer.forEach { skjema ->
            assertTrue(
                spec.contains("\n    $skjema:"),
                "Forventet skjema '$skjema' under components.schemas i bundled OpenAPI-spec",
            )
        }

        // Alle eksterne filreferanser skal være løst opp til interne refs.
        val eksterneRefs = Regex("""${'$'}ref:\s*["']([^"'#][^"']*)["']""").findAll(spec).toList()
        assertTrue(
            eksterneRefs.isEmpty(),
            "Forventet kun interne \$ref (#/...) etter bundling, men fant: " +
                eksterneRefs.joinToString { it.value },
        )
    }

    @Test
    fun `GET swagger documentation yaml returnerer den bundlede specen`() = testApplication {
        application {
            routing { swaggerRoute() }
        }

        val response = client.get("/swagger/documentation.yaml")

        assertEquals(HttpStatusCode.OK, response.status)
        val contentType = response.contentType()
        assertNotNull(contentType, "Mangler Content-Type på swagger-responsen")
        assertTrue(
            contentType!!.match(ContentType.parse("application/yaml")) ||
                contentType.match(ContentType.parse("text/yaml")),
            "Forventet yaml content-type, fikk: $contentType",
        )
        val body = response.bodyAsText()
        assertTrue(body.startsWith("openapi:"), "Forventet OpenAPI-dokument, fikk: ${body.take(80)}")
        forventedePaths.forEach { path ->
            assertTrue(body.contains("$path:"), "Forventet path '$path' i HTTP-respons")
        }
    }

    private fun lesSpecFraClasspath(): String {
        val url = this::class.java.classLoader.getResource("openapi/documentation.yaml")
        assertNotNull(
            url,
            "Fant ikke openapi/documentation.yaml på classpath – " +
                "kjør `./gradlew bundleOpenApi` eller `./gradlew processResources` først.",
        )
        return url!!.readText()
    }

    private fun io.ktor.client.statement.HttpResponse.contentType(): ContentType? =
        headers["Content-Type"]?.let { ContentType.parse(it) }
}
