package openapi

import com.diffplug.spotless.FormatterFunc
import java.io.Serializable

/**
 * Spotless-step som gjenoppretter flow-stil på nullability-unioner
 * (`type: [<type>, "null"]`) etter at Jackson sin YAML-serializer har
 * block-formatert dem til
 *
 *   type:
 *   - <type>
 *   - "null"
 *
 * OpenAPI 3.1 bruker slike to-element-unioner som markør for at en verdi
 * kan være `null`. Flow-stilen er mer idiomatisk og ekvivalent på JSON-
 * modell-nivå – utvikleren skal skrive den slik i kilden, og spotless
 * skal la den stå urørt.
 *
 * Klassen er definert i `buildSrc` (og ikke inline i `build.gradle.kts`)
 * fordi spotless sin input-fingerprinting krever at FormatterFunc-steg er
 * skikkelig serialiserbare. Kotlin-lambdaer og named classes i et
 * `.gradle.kts`-script har implisitte referanser til script-instansen og
 * serialiseres derfor ikke.
 */
class FlowStilNullableUnion : FormatterFunc, Serializable {

    override fun apply(input: String): String =
        nullableUnionBlockPattern.replace(input) { m ->
            """${m.groupValues[1]}type: [${m.groupValues[2]}, "null"]"""
        }

    companion object {
        private const val serialVersionUID: Long = 1L

        private val nullableUnionBlockPattern = Regex(
            """(?m)^(\s*)type:\s*\n\1- (\w+)\s*\n\1- (?:"null"|'null')\s*(?=\n|$)""",
        )

        /** Eksponert slik at `bundleOpenApi` kan bruke samme transformasjon. */
        fun transformer(input: String): String = FlowStilNullableUnion().apply(input)
    }
}

