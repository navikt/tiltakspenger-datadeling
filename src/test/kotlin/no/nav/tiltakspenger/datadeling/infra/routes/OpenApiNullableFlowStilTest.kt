package no.nav.tiltakspenger.datadeling.infra.routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.readText

/**
 * Håndhever at nullability-unioner i OpenAPI-kildefilene skrives i
 * flow-stil: `type: [<type>, "null"]`.
 *
 * OpenAPI 3.1 / JSON Schema 2020-12 bruker type-union som eneste måte å
 * uttrykke nullability på. Vi vil at alle utviklere skriver den samme,
 * kompakte formen:
 *
 *   type: [string, "null"]           # ✅ idiomatisk
 *
 * i stedet for block-formen:
 *
 *   type:                            # ❌ mindre lesbart for
 *   - string                         #   en to-element-union
 *   - "null"
 *
 * Spotless er konfigurert til å bevare flow-stilen (se [FlowStilNullableUnion]
 * i buildSrc), men en test er likevel siste skanse slik at ingen ved et uhell
 * commit-er block-form og må stole på at lint-en kjøres lokalt.
 */
internal class OpenApiNullableFlowStilTest {

    private val kildekatalog: Path = Path.of("src/main/openapi")

    private val blockStilNullableUnion = Regex(
        """(?m)^(\s*)type:\s*\n\1- \w+\s*\n\1- (?:"null"|'null')""",
    )

    @Test
    fun `nullability-unioner i kildefiler skrives i flow-stil`() {
        val treff = Files.walk(kildekatalog).use { stream ->
            stream
                .filter { Files.isRegularFile(it) && it.extension == "yaml" }
                .flatMap { fil ->
                    val innhold = fil.readText()
                    blockStilNullableUnion.findAll(innhold)
                        .map { match -> "$fil:\n  ${match.value.trim()}" }
                        .toList()
                        .stream()
                }
                .toList()
        }

        assertEquals(
            emptyList<String>(),
            treff,
            buildString {
                appendLine("Nullability-unioner skal skrives som `type: [<type>, \"null\"]`.")
                appendLine("Fant block-stil i:")
                for (t in treff) appendLine("  $t")
            },
        )
    }
}
