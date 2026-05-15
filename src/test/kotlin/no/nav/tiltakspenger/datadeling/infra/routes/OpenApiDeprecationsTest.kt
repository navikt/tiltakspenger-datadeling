package no.nav.tiltakspenger.datadeling.infra.routes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml

/**
 * Vakthund mot at deprecated/fjernede OpenAPI-konstrukter sniker seg inn i
 * specen igjen. Specen er OpenAPI 3.1, som bygger på JSON Schema 2020-12 og
 * fjerner flere 3.0-spesifikke nøkler.
 *
 * Testen går rekursivt gjennom hele det bundlede dokumentet (paths + skjemaer
 * + components) og samler opp alle forekomster av nøkler vi har bestemt oss
 * for å ikke tillate. Rapporterer full JSON Pointer-lignende sti slik at det
 * er lett å finne igjen i kilden.
 */
internal class OpenApiDeprecationsTest {

    /**
     * Nøkler som ikke lenger er gyldige i OpenAPI 3.1. Kommentarene beskriver
     * hva den idiomatiske erstatningen er.
     */
    private val forbudteNøkler: Map<String, String> = mapOf(
        // Fjernet i OpenAPI 3.1 / JSON Schema 2020-12. Bruk union-type:
        //   type: [string, "null"]
        "nullable" to "bruk `type: [<type>, \"null\"]` (OpenAPI 3.1 / JSON Schema 2020-12)",
        // Begge disse ble flyttet ut av skjemaene; i 3.1 hører de hjemme på
        // operation-nivå som egne felt.
        "x-nullable" to "fjern – ikke gyldig i OpenAPI 3.1",
        // OpenAPI 3.0 brukte boolean. 3.1 bruker JSON Schema-tallverdi.
        "exclusiveMinimumBoolean" to "bruk `exclusiveMinimum: <tall>` (JSON Schema 2020-12)",
        "exclusiveMaximumBoolean" to "bruk `exclusiveMaximum: <tall>` (JSON Schema 2020-12)",
    )

    @Test
    fun `specen bruker ikke deprecated OpenAPI 3_0-nøkler`() {
        val spec = lesBundletSpec()
        val treff = finnForbudteNøkler(spec, sti = "#")

        assertTrue(
            treff.isEmpty(),
            buildString {
                appendLine("Fant deprecated OpenAPI-nøkler i bundled spec:")
                for (t in treff) {
                    appendLine("  ${t.sti}: '${t.nøkkel}' – ${forbudteNøkler.getValue(t.nøkkel)}")
                }
            },
        )
    }

    @Test
    fun `specen er på OpenAPI 3_1`() {
        val spec = lesBundletSpec()
        val versjon = spec["openapi"] as? String
        assertTrue(
            versjon != null && versjon.startsWith("3.1"),
            "Forventet OpenAPI 3.1.x, fikk: $versjon",
        )
    }

    @Test
    fun `hver operasjon har tags, operationId og summary`() {
        val spec = lesBundletSpec()

        @Suppress("UNCHECKED_CAST")
        val paths = spec["paths"] as Map<String, Map<String, Any?>>

        val mangler = mutableListOf<String>()
        for ((path, operasjonerPerMetode) in paths) {
            for ((metode, op) in operasjonerPerMetode) {
                if (metode !in httpMetoder) continue
                @Suppress("UNCHECKED_CAST")
                val operasjon = op as Map<String, Any?>
                if ((operasjon["tags"] as? List<*>).isNullOrEmpty()) {
                    mangler += "$metode $path mangler 'tags'"
                }
                if ((operasjon["operationId"] as? String).isNullOrBlank()) {
                    mangler += "$metode $path mangler 'operationId'"
                }
                if ((operasjon["summary"] as? String).isNullOrBlank()) {
                    mangler += "$metode $path mangler 'summary'"
                }
            }
        }

        assertEquals(
            emptyList<String>(),
            mangler,
            "Operasjoner mangler idiomatiske felt:\n  ${mangler.joinToString("\n  ")}",
        )
    }

    @Test
    fun `nullability-unioner beholdes i flow-stil i bundled spec`() {
        val rå = lesRåSpec()

        // I bundleren kollapser vi `type:\n- <t>\n- "null"` til flow-stil
        // `type: [<t>, "null"]` slik at konsumentene får en kompakt
        // nullability-markør. Her passer vi på at ingen block-form sniker
        // seg gjennom generatoren.
        val blockFormRegex = Regex("""(?m)^\s*type:\s*\n\s+- \w+\s*\n\s+- (?:"null"|'null')""")
        val treff = blockFormRegex.findAll(rå).map { it.value.trim() }.toList()

        assertEquals(
            emptyList<String>(),
            treff,
            "Fant nullability-unioner i block-stil i bundled spec – bundleren skal kollapse dem til `type: [<t>, \"null\"]`:\n  ${treff.joinToString("\n  ")}",
        )
    }

    // ---------- Hjelpere ------------------------------------------------------

    private fun lesRåSpec(): String {
        val url = this::class.java.classLoader.getResource("openapi/documentation.yaml")
            ?: error("Fant ikke openapi/documentation.yaml på classpath – kjør processResources først.")
        return url.readText()
    }

    private data class Treff(val sti: String, val nøkkel: String)

    private fun finnForbudteNøkler(node: Any?, sti: String): List<Treff> =
        when (node) {
            is Map<*, *> -> node.flatMap { (k, v) ->
                val nøkkel = k as? String
                val barnesti = if (nøkkel != null) "$sti/$nøkkel" else sti
                val treffHer = if (nøkkel in forbudteNøkler) listOf(Treff(barnesti, nøkkel!!)) else emptyList()
                treffHer + finnForbudteNøkler(v, barnesti)
            }

            is List<*> -> node.flatMapIndexed { i, v -> finnForbudteNøkler(v, "$sti[$i]") }

            else -> emptyList()
        }

    private fun lesBundletSpec(): Map<String, Any?> {
        val url = this::class.java.classLoader.getResource("openapi/documentation.yaml")
            ?: error("Fant ikke openapi/documentation.yaml på classpath – kjør processResources først.")
        @Suppress("UNCHECKED_CAST")
        return Yaml().load<Map<String, Any?>>(url.readText())
    }

    private val httpMetoder = setOf("get", "post", "put", "patch", "delete", "head", "options", "trace")
}
