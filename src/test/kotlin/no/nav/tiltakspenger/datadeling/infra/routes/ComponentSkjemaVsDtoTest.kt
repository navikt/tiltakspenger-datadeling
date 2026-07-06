package no.nav.tiltakspenger.datadeling.infra.routes
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.behandling.Behandling
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import org.yaml.snakeyaml.Yaml
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure

/**
 * Sjekker at hvert skjema i openapi-specen matcher Kotlin-DTO-en som faktisk
 * serialiseres ut. Testen verifiserer:
 *  - property-navn
 *  - required-sett (non-nullable i Kotlin ↔ required i yaml)
 *  - streng type-/format-/enum-/$ref-sjekk
 *  - at alle alias-klasser for samme skjema har identisk property-sett
 *
 * Sannhetskilden for hver DTO er parameternavnene og -typene i primær-
 * konstruktøren. Det gir en stabil, intensjonell definisjon som matcher
 * hva utvikleren har designet klassen for å eksponere, og ignorerer arvede
 * getters (f.eks. `start`/`endInclusive` fra `ClosedRange`) som enten er
 * uønsket i API-et eller skal dokumenteres eksplisitt.
 *
 * Kjører som en [TestFactory] slik at hvert skjema rapporteres som et eget
 * testtilfelle – da ser man presist hvilke skjemaer som driver ut av takt.
 */
internal class ComponentSkjemaVsDtoTest {

    private val skjemaTilKlasser: Map<String, Set<KClass<*>>> = mapOf(
        "VedtakReqDTO" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.vedtak.infra.routes.VedtakReqDTO")),
        "BehandlingRequest" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.behandling.infra.routes.BehandlingRequestDTO")),
        "MappingError" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.vedtak.infra.routes.MappingError")),
        "BehandlingResponse" to setOf(Behandling::class),
        "TpsakBehandlingRespons" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.behandling.infra.routes.TpsakBehandlingResponseDTO")),
        "TpsakBehandlingSak" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.behandling.infra.routes.TpsakBehandlingResponseDTO\$SakDTO")),
        "TpsakBehandling" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.behandling.infra.routes.TpsakBehandlingResponseDTO\$BehandlingDTO")),
        "HentSakResponse" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.vedtak.infra.routes.HentSakResponseDTO")),
        "VedtakDetaljerResponse" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.vedtak.infra.routes.VedtakDetaljerResponse")),
        "Periode" to setOf(
            no.nav.tiltakspenger.libs.periode.Periode::class,
            dtoKlasse("no.nav.tiltakspenger.datadeling.vedtak.infra.routes.VedtakDTO\$PeriodeDTO"),
            dtoKlasse("no.nav.tiltakspenger.datadeling.vedtak.infra.routes.VedtakTidslinjeResponse\$VedtakResponse\$PeriodeDTO"),
        ),
        "BarnetilleggPeriode" to setOf(
            dtoKlasse("no.nav.tiltakspenger.datadeling.vedtak.infra.routes.VedtakDTO\$BarnetilleggDTO\$BarnetilleggPeriodeDTO"),
            dtoKlasse("no.nav.tiltakspenger.datadeling.vedtak.infra.routes.VedtakTidslinjeResponse\$VedtakResponse\$BarnetilleggDTO\$BarnetilleggPeriodeDTO"),
        ),
        "Barnetillegg" to setOf(
            dtoKlasse("no.nav.tiltakspenger.datadeling.vedtak.infra.routes.VedtakDTO\$BarnetilleggDTO"),
            dtoKlasse("no.nav.tiltakspenger.datadeling.vedtak.infra.routes.VedtakTidslinjeResponse\$VedtakResponse\$BarnetilleggDTO"),
        ),
        "VedtakDTO" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.vedtak.infra.routes.VedtakDTO")),
        "VedtakTidslinjeResponse" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.vedtak.infra.routes.VedtakTidslinjeResponse")),
        "VedtakTidslinjeSak" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.vedtak.infra.routes.VedtakTidslinjeSakDTO")),
        "VedtakResponse" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.vedtak.infra.routes.VedtakTidslinjeResponse\$VedtakResponse")),
        "MeldekortResponse" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.meldekort.infra.routes.MeldekortResponse")),
        "MeldekortKlartTilUtfylling" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.meldekort.infra.routes.MeldekortResponse\$MeldekortKlartTilUtfyllingDTO")),
        "GodkjentMeldekort" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.meldekort.infra.routes.MeldekortResponse\$GodkjentMeldekortDTO")),
        "MeldekortDag" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.meldekort.infra.routes.MeldekortResponse\$GodkjentMeldekortDTO\$MeldekortDag")),
        "ArenaMeldekort" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.meldekort.infra.routes.ArenaMeldekortResponse")),
        "ArenaMeldekortPeriode" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.meldekort.infra.routes.ArenaMeldekortResponse\$ArenaMeldekortPeriodeResponse")),
        "ArenaMeldekortDag" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.meldekort.infra.routes.ArenaMeldekortResponse\$ArenaMeldekortDagResponse")),
        "ArenaUtbetalingshistorikk" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.utbetalingshistorikk.infra.routes.ArenaUtbetalingshistorikkResponse")),
        "ArenaUtbetalingshistorikkDetaljer" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.utbetalingshistorikk.infra.routes.ArenaUtbetalingshistorikkDetaljerResponse")),
        "Vedtakfakta" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.utbetalingshistorikk.infra.routes.ArenaVedtakfaktaResponse")),
        "Anmerkning" to setOf(dtoKlasse("no.nav.tiltakspenger.datadeling.utbetalingshistorikk.infra.routes.ArenaAnmerkningResponse")),
    )

    /** Primary-klasse brukt for navn/required/typer-sjekk (første i settet). */
    private val skjemaTilKlasse: Map<String, KClass<*>> =
        skjemaTilKlasser.mapValues { (_, klasser) -> klasser.first() }

    private fun dtoKlasse(binaryName: String): KClass<*> =
        try {
            Class.forName(binaryName).kotlin
        } catch (_: ClassNotFoundException) {
            error("Fant ikke DTO-klasse '$binaryName'. Oppdater ComponentSkjemaVsDtoTest eller DTO-klassen.")
        }

    /**
     * Skjemaer som bevisst er dokumentasjons-varianter uten egen DTO.
     * VedtakSakReqDTO brukes kun for /vedtak/sak, hvor den faktiske DTO-en
     * er VedtakReqDTO – men klienter trenger bare å sende `ident`, så vi
     * dokumenterer den som et eget skjema.
     */
    private val dokumentasjonsVarianter = setOf("VedtakSakReqDTO")

    @TestFactory
    fun `hvert component-skjema matcher sin DTO`(): List<org.junit.jupiter.api.DynamicNode> {
        val skjemaer = lesComponentSkjemaer()
        val dekket = skjemaTilKlasse.keys + dokumentasjonsVarianter
        val manglende = skjemaer.keys - dekket

        val tester = skjemaTilKlasse.map { (skjemaNavn, kClass) ->
            val dtoFelter = dtoFelter(kClass)

            dynamicContainer(
                skjemaNavn,
                listOf(
                    dynamicTest("property-rekkefølge") {
                        val spec = samleSkjema(skjemaer.getValue(skjemaNavn), skjemaer)
                        // Rekkefølgen i yaml-en skal matche primærkonstruktørens
                        // parameter-rekkefølge. Det gir forutsigbar dokumentasjon
                        // og speiler hvordan Jackson serialiserer ut feltene.
                        withClue("Property-rekkefølge mismatch for skjema $skjemaNavn vs ${kClass.simpleName}") {
                            dtoFelter.keys.toList() shouldBe spec.properties.keys.toList()
                        }
                    },
                    dynamicTest("required-rekkefølge") {
                        val spec = samleSkjema(skjemaer.getValue(skjemaNavn), skjemaer)
                        // Jackson serialiserer alltid alle felt, også null-felt,
                        // så alle properties skal stå i `required`. Nullability
                        // uttrykkes på typen (`type: [X, "null"]`) og ikke ved at
                        // feltet kan utelates. Rekkefølgen skal matche DTO-en.
                        withClue("Required-rekkefølge mismatch for $skjemaNavn: alle properties skal være required (Jackson utelater ikke null-felt) og rekkefølgen skal speile DTO-en.") {
                            dtoFelter.keys.toList() shouldBe spec.required
                        }
                    },
                    dynamicTest("property-typer") {
                        val spec = samleSkjema(skjemaer.getValue(skjemaNavn), skjemaer)
                        val feil = mutableListOf<String>()
                        for ((navn, specType) in spec.properties) {
                            val dtoType = dtoFelter[navn] ?: continue
                            feil += typeFeil(navn, specType, dtoType)
                        }
                        withClue("Type-mismatch i $skjemaNavn:\n  ${feil.joinToString("\n  ")}") {
                            feil.shouldBeEmpty()
                        }
                    },
                    dynamicTest("alle alias-klasser er strukturelt like") {
                        val klasser = skjemaTilKlasser.getValue(skjemaNavn)
                        if (klasser.size < 2) return@dynamicTest
                        val referansenavn = dtoFelter.keys.toSortedSet()
                        val referanseNonNull = dtoFelter
                            .filterValues { !it.isMarkedNullable }.keys.toSortedSet()
                        for (annen in klasser.drop(1)) {
                            val andreFelt = dtoFelter(annen)
                            withClue("Alias-klassen ${annen.qualifiedName} har andre felter enn ${kClass.qualifiedName}") {
                                referansenavn shouldBe andreFelt.keys.toSortedSet()
                            }
                            val andreNonNull = andreFelt
                                .filterValues { !it.isMarkedNullable }.keys.toSortedSet()
                            withClue("Alias-klassen ${annen.qualifiedName} har andre non-null-felter enn ${kClass.qualifiedName}") {
                                referanseNonNull shouldBe andreNonNull
                            }
                        }
                    },
                ),
            )
        }

        // Feil hvis spec har nye skjemaer som ikke er dekket her, slik at
        // mappingen holdes i synk ettersom specen utvides.
        val ekstraSjekk = dynamicTest("alle skjemaer i specen er enten mappet eller dokumentasjons-varianter") {
            withClue("Følgende skjemaer i specen mangler DTO-mapping i testen: $manglende") {
                manglende shouldBe emptySet()
            }
        }
        return tester + ekstraSjekk
    }

    // ---------- Hjelpere ------------------------------------------------------

    private data class SamletSkjema(
        val properties: Map<String, Map<*, *>>,
        val required: List<String>,
    )

    /**
     * Returnerer feltene vi anser som "serialisert" for en DTO: alle parametre
     * i primærkonstruktøren.
     */
    private fun dtoFelter(kClass: KClass<*>): Map<String, KType> {
        val ctor = kClass.primaryConstructor
            ?: error("${kClass.qualifiedName} har ingen primærkonstruktør – DTO-klasser må ha det for å kunne sammenlignes.")
        return ctor.parameters.associate { p ->
            val navn = p.name ?: error("Navnløs konstruktørparameter i ${kClass.qualifiedName}")
            navn to p.type
        }
    }

    /**
     * Slår sammen properties/required på tvers av `allOf` slik at
     * `HentSakResponseDTO` (som er Sak + et ekstra felt) kan sammenlignes med
     * den flate Kotlin-DTO-en.
     */
    @Suppress("UNCHECKED_CAST")
    private fun samleSkjema(
        skjema: Map<*, *>,
        alleSkjemaer: Map<String, Map<*, *>>,
    ): SamletSkjema {
        val props = linkedMapOf<String, Map<*, *>>()
        val required = mutableListOf<String>()
        val allOf = skjema["allOf"] as? List<Map<*, *>>
        if (allOf != null) {
            for (del in allOf) {
                val ref = del["\$ref"] as? String
                val underskjema = if (ref != null) {
                    val navn = ref.removePrefix("#/components/schemas/")
                    alleSkjemaer[navn] ?: error("Fant ikke $navn i components.schemas")
                } else {
                    del
                }
                val samlet = samleSkjema(underskjema, alleSkjemaer)
                props += samlet.properties
                required += samlet.required
            }
        } else {
            (skjema["properties"] as? Map<String, Map<*, *>>)?.let(props::putAll)
            (skjema["required"] as? List<String>)?.let(required::addAll)
        }
        return SamletSkjema(props, required)
    }

    @Suppress("UNCHECKED_CAST")
    private fun lesComponentSkjemaer(): Map<String, Map<*, *>> {
        val url = this::class.java.classLoader.getResource("openapi/documentation.yaml")
            ?: error("Fant ikke openapi/documentation.yaml på classpath – kjør processResources først.")
        val rot = Yaml().load<Map<String, Any?>>(url.readText())
        val components = rot["components"] as Map<*, *>
        return components["schemas"] as Map<String, Map<*, *>>
    }

    /**
     * Streng, rekursiv type-sjekk mot yaml-skjemaet:
     *  - ukjente nøkler (som skrivefeil `typ:` eller `enumz:`) feiler
     *  - `type` må være en av JSON Schema-typene
     *  - `format` må være gyldig for valgt `type` og matche Kotlin-typen
     *    (date → LocalDate, int64 → Long, osv.)
     *  - `enum` sammenlignes 1:1 med Kotlin-enumets konstantsett
     *  - `$ref` må peke på et kjent skjema, og målklassen må matche DTO-en
     *  - `array.items` valideres rekursivt mot generic-argumentet
     *  - `additionalProperties` valideres mot verdi-typen i Map-en
     */
    @Suppress("UNCHECKED_CAST")
    private fun typeFeil(felt: String, specType: Map<*, *>, dtoType: KType): List<String> {
        val feil = mutableListOf<String>()
        val erasure = dtoType.jvmErasure

        // 1) Ukjente nøkler i skjemaet fanger skrivefeil som `typ: string`.
        val nøkler = specType.keys.mapNotNull { it as? String }.toSet()
        val ukjente = nøkler - tillatteSkjemaNøkler
        if (ukjente.isNotEmpty()) {
            feil += "'$felt': ukjente nøkler i skjema: $ukjente"
        }

        // 2) Nullability. Jackson serialiserer alltid alle felt, og nullability
        // uttrykkes derfor på typen – ikke ved at feltet kan utelates.
        //   - inline type:  `type: [X, "null"]`
        //   - $ref eller kompleks: `anyOf: [{$ref: ...}, {type: "null"}]`
        val dtoNullable = dtoType.isMarkedNullable
        val specNullable = tillaterNull(specType)
        if (dtoNullable && !specNullable) {
            feil += "'$felt': DTO er nullable men spec tillater ikke null " +
                "(bruk `type: [<type>, \"null\"]` eller `anyOf: [{\$ref: ...}, {type: \"null\"}]`)"
        } else if (!dtoNullable && specNullable) {
            feil += "'$felt': DTO er non-null men spec tillater null"
        }

        // 3) $ref
        val ref = specType["\$ref"] as? String ?: (specType["anyOf"] as? List<*>)
            ?.filterIsInstance<Map<*, *>>()
            ?.firstNotNullOfOrNull { it["\$ref"] as? String }
        if (ref != null) {
            val navn = ref.removePrefix("#/components/schemas/")
            val aksepterte = skjemaTilKlasser[navn]
            when {
                aksepterte == null ->
                    feil += "'$felt': \$ref peker til ukjent skjema '$navn'"

                erasure !in aksepterte ->
                    feil += "'$felt': \$ref peker til $navn (${aksepterte.joinToString { it.qualifiedName ?: it.simpleName.orEmpty() }}) men DTO er ${erasure.qualifiedName}"
            }
            return feil
        }

        // 4) type og format. OpenAPI 3.1 tillater `type` som liste for å
        // uttrykke nullability (f.eks. `type: [string, "null"]`) – vi plukker
        // ut den effektive ikke-null-typen for validering mot DTO-en.
        val type = effektivType(specType)
        val format = specType["format"] as? String
        if (type == null) {
            if (format != null) feil += "'$felt': 'format' uten 'type'"
            if ("enum" in specType) feil += "'$felt': 'enum' uten 'type'"
            if ("items" in specType) feil += "'$felt': 'items' uten 'type: array'"
            return feil
        }
        if (type !in gyldigeTyper) {
            feil += "'$felt': ugyldig type '$type'"
            return feil
        }
        val tillatteFormater = tillatteFormaterPerType[type]
        if (format != null && tillatteFormater != null && format !in tillatteFormater) {
            feil += "'$felt': ugyldig format '$format' for type $type"
        }

        // 5) Per-type-sjekk mot Kotlin-typen.
        when (type) {
            "string" -> feil += sjekkString(felt, specType, format, erasure)

            "integer" -> feil += sjekkInteger(felt, format, erasure)

            "number" -> feil += sjekkNumber(felt, format, erasure)

            "boolean" -> {
                if (erasure != Boolean::class) {
                    feil += "'$felt': type boolean men DTO er ${erasure.simpleName}"
                }
            }

            "array" -> {
                if (!Collection::class.java.isAssignableFrom(erasure.java)) {
                    feil += "'$felt': type array men DTO er ${erasure.simpleName}"
                } else {
                    val items = specType["items"] as? Map<*, *>
                        ?: run {
                            feil += "'$felt': array mangler 'items'"
                            return feil
                        }
                    val itemType = dtoType.arguments.firstOrNull()?.type
                    if (itemType != null) {
                        feil += typeFeil("$felt[items]", items, itemType)
                    }
                }
            }

            "object" -> {
                val addl = specType["additionalProperties"]
                val inlineProps = specType["properties"] as? Map<*, *>
                if (addl != null && !Map::class.java.isAssignableFrom(erasure.java)) {
                    feil += "'$felt': 'additionalProperties' forutsetter Map men DTO er ${erasure.simpleName}"
                } else if (addl is Map<*, *>) {
                    val valueType = dtoType.arguments.getOrNull(1)?.type
                    if (valueType != null) feil += typeFeil("$felt[value]", addl, valueType)
                } else if (inlineProps != null) {
                    // Dyp-valider inline nested object mot DTO-klassen: samme
                    // property-rekkefølge, samme required-liste, og hvert felt
                    // type-sjekkes rekursivt.
                    feil += inlineObjektFeil(felt, specType, erasure)
                }
            }
        }

        return feil
    }

    private fun sjekkString(felt: String, specType: Map<*, *>, format: String?, erasure: KClass<*>): List<String> {
        @Suppress("UNCHECKED_CAST")
        val enums = specType["enum"] as? List<String>
        return when {
            format == "date" ->
                if (erasure == LocalDate::class) {
                    emptyList()
                } else {
                    listOf("'$felt': format=date forutsetter LocalDate men DTO er ${erasure.simpleName}")
                }

            format == "date-time" ->
                if (erasure in dateTimeKlasser) {
                    emptyList()
                } else {
                    listOf("'$felt': format=date-time forutsetter LocalDateTime/OffsetDateTime men DTO er ${erasure.simpleName}")
                }

            enums != null -> {
                if (!erasure.java.isEnum) {
                    listOf("'$felt': 'enum' i spec men DTO er ${erasure.simpleName}, ikke enum")
                } else {
                    val dtoVerdier = erasure.java.enumConstants.map { (it as Enum<*>).name }.toSet()
                    val specVerdier = enums.toSet()
                    val manglerIDto = specVerdier - dtoVerdier
                    val manglerISpec = dtoVerdier - specVerdier
                    buildList {
                        if (manglerIDto.isNotEmpty()) {
                            add("'$felt': enum-verdier i spec mangler i DTO (${erasure.simpleName}): $manglerIDto")
                        }
                        if (manglerISpec.isNotEmpty()) {
                            add("'$felt': enum-verdier i DTO (${erasure.simpleName}) mangler i spec: $manglerISpec")
                        }
                    }
                }
            }

            else ->
                if (erasure == String::class || erasure.java.isEnum) {
                    emptyList()
                } else {
                    listOf("'$felt': type=string men DTO er ${erasure.simpleName}")
                }
        }
    }

    private fun sjekkInteger(felt: String, format: String?, erasure: KClass<*>): List<String> {
        val forventede = when (format) {
            "int64" -> setOf(Long::class)
            "int32", null -> setOf(Int::class, Short::class, Byte::class)
            else -> emptySet()
        }
        return if (erasure !in forventede) {
            listOf("'$felt': type=integer${format?.let { "/$it" }.orEmpty()} men DTO er ${erasure.simpleName}")
        } else {
            emptyList()
        }
    }

    private fun sjekkNumber(felt: String, format: String?, erasure: KClass<*>): List<String> {
        val forventede = when (format) {
            "float" -> setOf(Float::class)
            "double", null -> setOf(Double::class, Float::class)
            else -> emptySet()
        }
        return if (erasure !in forventede) {
            listOf("'$felt': type=number${format?.let { "/$it" }.orEmpty()} men DTO er ${erasure.simpleName}")
        } else {
            emptyList()
        }
    }

    private val tillatteSkjemaNøkler = setOf(
        "type", "format", "enum", "items", "\$ref", "additionalProperties",
        "properties", "required", "description", "deprecated",
        "allOf", "oneOf", "anyOf",
        "minItems", "maxItems",
    )

    private val gyldigeTyper = setOf("string", "integer", "number", "boolean", "array", "object", "null")

    /**
     * OpenAPI 3.1 tillater `type` som enten en String eller en liste (union med
     * `null`). Returnerer den effektive typen utenom `null`, eller null hvis
     * feltet ikke har noen `type`.
     */
    private fun effektivType(specType: Map<*, *>): String? =
        when (val raw = specType["type"]) {
            is String -> raw
            is List<*> -> raw.filterIsInstance<String>().firstOrNull { it != "null" }
            else -> null
        }

    /**
     * Returnerer true hvis property-skjemaet tillater JSON `null`. To former:
     *   - inline:   `type: [X, "null"]`
     *   - wrapped:  `anyOf`/`oneOf` med en gren som er `{type: "null"}`
     */
    @Suppress("UNCHECKED_CAST")
    private fun tillaterNull(specType: Map<*, *>): Boolean {
        val type = specType["type"]
        if (type is List<*> && type.any { it == "null" }) return true
        if (type == "null") return true
        for (nøkkel in listOf("anyOf", "oneOf")) {
            val grener = specType[nøkkel] as? List<Map<*, *>> ?: continue
            if (grener.any { it["type"] == "null" }) return true
        }
        return false
    }

    /**
     * Dyp-valider et inline `type: object`-skjema mot den nestede Kotlin-
     * klassen DTO-en peker på. Sjekker property-rekkefølge, required-rekkefølge
     * og hvert felts type rekursivt.
     */
    @Suppress("UNCHECKED_CAST")
    private fun inlineObjektFeil(felt: String, specType: Map<*, *>, erasure: KClass<*>): List<String> {
        val feil = mutableListOf<String>()
        val inlineProps = specType["properties"] as? Map<String, Map<*, *>> ?: return feil
        val dtoFelter = runCatching { dtoFelter(erasure) }.getOrElse {
            return listOf("'$felt': inline object refererer til ${erasure.simpleName} som ikke har primærkonstruktør – promoter til eget skjema.")
        }
        val dtoNavn = dtoFelter.keys.toList()
        val specNavn = inlineProps.keys.toList()
        if (dtoNavn != specNavn) {
            feil += "'$felt': inline object property-rekkefølge mismatch. DTO=$dtoNavn, spec=$specNavn"
        }
        val specRequired = specType["required"] as? List<String> ?: emptyList()
        if (dtoNavn != specRequired) {
            feil += "'$felt': inline object required-rekkefølge mismatch. DTO=$dtoNavn, required=$specRequired"
        }
        for ((navn, propSpec) in inlineProps) {
            val propDtoType = dtoFelter[navn] ?: continue
            feil += typeFeil("$felt.$navn", propSpec, propDtoType)
        }
        return feil
    }

    private val tillatteFormaterPerType: Map<String, Set<String>> = mapOf(
        "string" to setOf("date", "date-time", "byte", "binary", "uuid"),
        "integer" to setOf("int32", "int64"),
        "number" to setOf("float", "double"),
    )

    private val dateTimeKlasser = setOf(LocalDateTime::class, OffsetDateTime::class)
}
