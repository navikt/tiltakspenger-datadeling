package no.nav.tiltakspenger.datadeling.infra.routes
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml

/**
 * Håndhever at det eksterne API-kontraktsgrensesnittet er ASCII-rent:
 *  - URL-er (paths) inneholder ikke æ/ø/å – ingen prosentkoding eller
 *    encoding-bugs i klienter.
 *  - JSON-feltnavn (properties i OpenAPI-skjemaene) bruker ikke norske
 *    tegn. Feltnavn er de mest støy-følsomme strengene i en JSON-payload
 *    fordi de snappes opp av deserialiserings-/kodegenereringsverktøy hos
 *    konsumenter.
 *
 * Andre steder i repoet (mapper, filnavn, beskrivelser, variabelnavn,
 * kommentarer) skriver vi gjerne norsk med æøå – denne testen berører
 * kun det som havner på nettet.
 */
internal class OpenApiAsciiTest {

    private val norskeTegn = Regex("[æøåÆØÅ]")

    @Test
    fun `URL-paths inneholder ikke æøå`() {
        val spec = lesBundletSpec()

        @Suppress("UNCHECKED_CAST")
        val paths = spec["paths"] as Map<String, Any?>

        val brudd = paths.keys.filter { norskeTegn.containsMatchIn(it) }

        withClue("URL-paths skal være ASCII – fant norske tegn i: $brudd") {
            brudd shouldBe emptyList()
        }
    }

    @Test
    fun `parameter-navn inneholder ikke æøå`() {
        val spec = lesBundletSpec()

        @Suppress("UNCHECKED_CAST")
        val paths = spec["paths"] as Map<String, Map<String, Any?>>

        val brudd = mutableListOf<String>()
        for ((path, operasjoner) in paths) {
            for ((metode, op) in operasjoner) {
                if (metode !in httpMetoder) continue
                @Suppress("UNCHECKED_CAST")
                val params = (op as Map<String, Any?>)["parameters"] as? List<Map<*, *>> ?: continue
                for (p in params) {
                    val navn = p["name"] as? String ?: continue
                    if (norskeTegn.containsMatchIn(navn)) {
                        brudd += "$metode $path?$navn"
                    }
                }
            }
        }

        withClue("Parameter-navn skal være ASCII – fant norske tegn i: $brudd") {
            brudd shouldBe emptyList()
        }
    }

    @Test
    fun `JSON-feltnavn inneholder ikke æøå`() {
        val spec = lesBundletSpec()
        val brudd = finnFeltnavnMedNorskeTegn(spec, sti = "#")

        withClue(
            buildString {
                appendLine("JSON-feltnavn skal være ASCII. Fant norske tegn i:")
                for (b in brudd) appendLine("  ${b.sti}: '${b.felt}'")
            },
        ) {
            brudd shouldBe emptyList()
        }
    }

    // ---------- Hjelpere ------------------------------------------------------

    private data class Brudd(val sti: String, val felt: String)

    /**
     * Går rekursivt gjennom dokumentet og rapporterer alle nøkler under en
     * `properties`-blokk (inkl. nested objekter og `additionalProperties`) som
     * inneholder æ/ø/å. Alle `properties`-noder i OpenAPI definerer JSON-
     * feltnavn, uansett om de ligger direkte i et skjema eller inne i allOf/
     * oneOf/anyOf.
     */
    private fun finnFeltnavnMedNorskeTegn(node: Any?, sti: String): List<Brudd> =
        when (node) {
            is Map<*, *> -> node.flatMap { (k, v) ->
                val nøkkel = k as? String
                val barnesti = if (nøkkel != null) "$sti/$nøkkel" else sti
                val treffHer = if (nøkkel == "properties" && v is Map<*, *>) {
                    v.keys.filterIsInstance<String>()
                        .filter { norskeTegn.containsMatchIn(it) }
                        .map { Brudd("$barnesti/$it", it) }
                } else {
                    emptyList()
                }
                treffHer + finnFeltnavnMedNorskeTegn(v, barnesti)
            }

            is List<*> -> node.flatMapIndexed { i, v -> finnFeltnavnMedNorskeTegn(v, "$sti[$i]") }

            else -> emptyList()
        }

    private fun lesBundletSpec(): Map<String, Any?> {
        val url = this::class.java.classLoader.getResource("openapi/documentation.yaml")
            ?: error("Fant ikke openapi/documentation.yaml på classpath – kjør processResources først.")
        @Suppress("UNCHECKED_CAST")
        return Yaml().load(url.readText())
    }

    private val httpMetoder = setOf("get", "post", "put", "patch", "delete", "head", "options", "trace")
}
