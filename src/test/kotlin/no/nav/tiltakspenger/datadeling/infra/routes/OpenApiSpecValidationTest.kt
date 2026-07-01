package no.nav.tiltakspenger.datadeling.infra.routes
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.swagger.v3.parser.OpenAPIV3Parser
import io.swagger.v3.parser.core.models.ParseOptions
import org.junit.jupiter.api.Test

/**
 * Validerer at den bundlede openapi-specen er et gyldig OpenAPI-dokument
 * iht. swagger-parser (samme biblioteket som driver mesteparten av
 * OpenAPI-verktøyflora på JVM). Fanger strukturelle feil som:
 *  - ugyldige referanser (`$ref` som ikke finnes)
 *  - ukjente/obligatoriske felter på root-/path-/skjema-nivå
 *  - feil bruk av discriminator/allOf/oneOf
 *  - inkonsistente skjema-definisjoner
 */
internal class OpenApiSpecValidationTest {

    @Test
    fun `bundlet openapi-spec er strukturelt gyldig`() {
        val url = this::class.java.classLoader.getResource("openapi/documentation.yaml")
            ?: error("Fant ikke openapi/documentation.yaml på classpath – kjør processResources først.")

        val options = ParseOptions().apply {
            isResolve = true
            isResolveFully = true
            isValidateExternalRefs = true
        }
        val result = OpenAPIV3Parser().readLocation(url.toString(), null, options)

        val meldinger = result.messages ?: emptyList()
        withClue("swagger-parser rapporterte valideringsfeil:\n  ${meldinger.joinToString("\n  ")}") {
            meldinger.isEmpty() shouldBe true
        }
        withClue("OpenAPI-dokumentet kunne ikke parses") {
            result.openAPI shouldNotBe null
        }
    }
}
