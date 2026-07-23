package no.nav.tiltakspenger.datadeling.testutils

import io.kotest.assertions.fail
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.json.lesTre
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import org.yaml.snakeyaml.Yaml
import tools.jackson.databind.JsonNode
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * Som [defaultRequestWithAssertions], men verifiserer i tillegg responsen mot openapi-kontrakten via [verifiserMotOpenApiKontrakt].
 * Brukes for alle statuser som er deklarert i kontrakten; for udeklarerte statuser (f.eks. dagens 500-svar) brukes [defaultRequestWithAssertions] direkte.
 */
suspend fun ApplicationTestBuilder.defaultRequestMedKontraktsverifisering(
    method: HttpMethod,
    uri: String,
    clock: Clock = fixedClock,
    jwt: String?,
    forventet: ForventetRespons?,
    setup: HttpRequestBuilder.() -> Unit = {},
): HttpResponse {
    return defaultRequestWithAssertions(
        method = method,
        uri = uri,
        clock = clock,
        jwt = jwt,
        forventet = forventet,
        setup = setup,
    ).verifiserMotOpenApiKontrakt()
}

/**
 * Verifiserer at responsen matcher openapi-kontrakten (bundlet `openapi/documentation.yaml`) for forespørselens sti, metode og status.
 * Stien, metoden og statuskoden må være deklarert i kontrakten.
 * Er application/json-innhold deklarert for statusen, valideres den faktiske respons-JSON-en strengt mot skjemaet: typer (inkludert null-unioner), required, enum, formater — og ingen udeklarerte felter.
 * Er statusen deklarert uten innhold (f.eks. 401/403 i dag), valideres ikke bodyen.
 * Validatoren dekker bevisst bare skjemakonstruksjonene spec-en faktisk bruker, og feiler på ukjente nøkkelord i stedet for å hoppe stille over dem.
 */
suspend fun HttpResponse.verifiserMotOpenApiKontrakt(): HttpResponse {
    verifiserMotOpenApiKontrakt(
        metode = request.method,
        sti = request.url.encodedPath,
        status = status,
        contentType = contentType(),
        body = bodyAsText(),
    )
    return this
}

internal fun verifiserMotOpenApiKontrakt(
    metode: HttpMethod,
    sti: String,
    status: HttpStatusCode,
    contentType: ContentType?,
    body: String,
) {
    val operasjon = OpenApiKontrakt.stier[sti]?.get(metode.value.lowercase())?.somMap()
        ?: fail("openapi-kontrakten deklarerer ikke ${metode.value} $sti")
    val responser = operasjon["responses"].somMap()
    val respons = responser[status.value.toString()]?.somMap()
        ?: fail("openapi-kontrakten deklarerer ikke status ${status.value} for ${metode.value} $sti (deklarert: ${responser.keys.sorted()})")
    val innhold = respons["content"]?.somMap() ?: return
    val skjema = innhold["application/json"]?.somMap()?.get("schema")?.somMap()
        ?: fail("openapi-kontrakten deklarerer innhold uten application/json-skjema for ${metode.value} $sti ${status.value} — utvid VerifiserMotOpenApiKontrakt")
    if (contentType?.match(ContentType.Application.Json) != true) {
        fail("openapi-kontrakten deklarerer application/json for ${metode.value} $sti ${status.value}, men responsen hadde Content-Type $contentType")
    }
    if (body.isBlank()) {
        fail("openapi-kontrakten deklarerer application/json for ${metode.value} $sti ${status.value}, men responsen hadde tom body")
    }
    val feil = mutableListOf<String>()
    valider(lesTre(body), skjema, "\$", feil)
    if (feil.isNotEmpty()) {
        fail(
            "Responsen bryter openapi-kontrakten for ${metode.value} $sti ${status.value}:\n" +
                feil.joinToString("\n") { "  - $it" } +
                "\nBody: $body",
        )
    }
}

private object OpenApiKontrakt {
    private val dokument: Map<String, Any?> by lazy {
        val resource = checkNotNull(this::class.java.classLoader.getResource("openapi/documentation.yaml")) {
            "Fant ikke openapi/documentation.yaml på classpath – kjør processResources først."
        }
        resource.openStream().use { Yaml().load(it) }
    }

    val stier: Map<String, Map<String, Any?>> by lazy {
        dokument["paths"].somMap().mapValues { (_, verdi) -> verdi.somMap() }
    }

    val komponentskjemaer: Map<String, Map<String, Any?>> by lazy {
        dokument["components"].somMap()["schemas"].somMap().mapValues { (_, verdi) -> verdi.somMap() }
    }
}

/** Skjemanøkkelord validatoren håndhever; nye nøkkelord i spec-en må implementeres her før de kan tas i bruk. */
private val validerteNokkelord = setOf("\$ref", "type", "format", "enum", "properties", "required", "items", "minItems", "additionalProperties", "anyOf")

/** Rene annotasjonsnøkkelord uten valideringssemantikk. */
private val ignorerteNokkelord = setOf("description", "deprecated", "example", "title")

private fun valider(node: JsonNode, skjemaInn: Map<String, Any?>, sti: String, feil: MutableList<String>) {
    val skjema = følgRef(skjemaInn)

    val ukjente = skjema.keys - validerteNokkelord - ignorerteNokkelord
    if (ukjente.isNotEmpty()) {
        feil += "$sti: skjemaet bruker nøkkelord validatoren ikke støtter: $ukjente — utvid VerifiserMotOpenApiKontrakt"
        return
    }

    (skjema["anyOf"] as? List<*>)?.let { alternativer ->
        val alternativfeil = alternativer.map { alternativ ->
            mutableListOf<String>().also { valider(node, alternativ.somMap(), sti, it) }
        }
        if (alternativfeil.none { it.isEmpty() }) {
            feil += "$sti: verdien matcher ingen av anyOf-alternativene: ${alternativfeil.flatten()}"
        }
        return
    }

    typerI(skjema)?.let { typer ->
        if (typer.none { node.harJsonType(it) }) {
            feil += "$sti: forventet type $typer, men verdien $node er av type ${node.nodeType.toString().lowercase()}"
            return
        }
    }
    if (node.isNull) {
        // "null" i type-unionen matcher; øvrige nøkkelord gjelder ikke null-verdier.
        return
    }

    (skjema["enum"] as? List<*>)?.let { lovlige ->
        if (lovlige.none { it.toString() == node.asString() }) {
            feil += "$sti: verdien '${node.asString()}' er ikke blant enum-verdiene $lovlige"
        }
    }

    (skjema["format"] as? String)?.let { validerFormat(node, it, sti, feil) }

    if (node.isObject) validerObjekt(node, skjema, sti, feil)
    if (node.isArray) validerArray(node, skjema, sti, feil)
}

private fun følgRef(skjema: Map<String, Any?>): Map<String, Any?> {
    var aktuelt = skjema
    val besokt = mutableSetOf<String>()
    while (true) {
        val ref = aktuelt["\$ref"] as? String ?: return aktuelt
        check(aktuelt.size == 1) { "\$ref med søskennøkler støttes ikke: $aktuelt" }
        check(ref.startsWith("#/components/schemas/")) { "Kun interne \$ref-er støttes: $ref" }
        val navn = ref.removePrefix("#/components/schemas/")
        check(besokt.add(navn)) { "\$ref-sykel i openapi-kontrakten: $navn" }
        aktuelt = OpenApiKontrakt.komponentskjemaer[navn] ?: fail("Fant ikke komponentskjemaet '$navn' i openapi-kontrakten")
    }
}

private fun typerI(skjema: Map<String, Any?>): List<String>? = when (val type = skjema["type"]) {
    null -> null
    is String -> listOf(type)
    is List<*> -> type.map { it.toString() }
    else -> fail("Ugyldig type-deklarasjon i openapi-kontrakten: $type")
}

private fun JsonNode.harJsonType(type: String): Boolean = when (type) {
    "string" -> isString
    "integer" -> isIntegralNumber
    "number" -> isNumber
    "boolean" -> isBoolean
    "object" -> isObject
    "array" -> isArray
    "null" -> isNull
    else -> fail("Ukjent type '$type' i openapi-kontrakten — utvid VerifiserMotOpenApiKontrakt")
}

private fun validerFormat(node: JsonNode, format: String, sti: String, feil: MutableList<String>) {
    when (format) {
        "date" -> if (runCatching { LocalDate.parse(node.asString()) }.isFailure) {
            feil += "$sti: '${node.asString()}' er ikke en gyldig date (ISO-8601)"
        }

        "date-time" -> if (runCatching { OffsetDateTime.parse(node.asString()) }.isFailure) {
            feil += "$sti: '${node.asString()}' er ikke en gyldig date-time (RFC 3339)"
        }

        "int32" -> if (!node.canConvertToInt()) feil += "$sti: $node får ikke plass i int32"

        "int64" -> if (!node.canConvertToLong()) feil += "$sti: $node får ikke plass i int64"

        "double" -> if (!node.isNumber) feil += "$sti: $node er ikke et tall (double)"

        else -> feil += "$sti: ukjent format '$format' — utvid VerifiserMotOpenApiKontrakt"
    }
}

private fun validerObjekt(node: JsonNode, skjema: Map<String, Any?>, sti: String, feil: MutableList<String>) {
    val deklarerte = skjema["properties"]?.somMap() ?: emptyMap()
    val pakrevde = (skjema["required"] as? List<*>)?.map { it.toString() } ?: emptyList()
    val tilleggsskjema = skjema["additionalProperties"]

    pakrevde.filter { node.get(it) == null }.forEach {
        feil += "$sti: mangler påkrevd felt '$it'"
    }
    for (navn in node.propertyNames()) {
        val verdi = node.get(navn) ?: continue
        val feltskjema = deklarerte[navn]
        when {
            feltskjema != null -> valider(verdi, feltskjema.somMap(), "$sti.$navn", feil)
            tilleggsskjema != null -> valider(verdi, tilleggsskjema.somMap(), "$sti.$navn", feil)
            else -> feil += "$sti: feltet '$navn' er ikke deklarert i kontrakten"
        }
    }
}

private fun validerArray(node: JsonNode, skjema: Map<String, Any?>, sti: String, feil: MutableList<String>) {
    (skjema["minItems"] as? Int)?.let {
        if (node.size() < it) feil += "$sti: arrayet har ${node.size()} elementer, minItems er $it"
    }
    val elementskjema = skjema["items"]?.somMap() ?: return
    node.forEachIndexed { indeks, element -> valider(element, elementskjema, "$sti[$indeks]", feil) }
}

@Suppress("UNCHECKED_CAST")
private fun Any?.somMap(): Map<String, Any?> =
    this as? Map<String, Any?> ?: fail("Forventet map i openapi-kontrakten, fant: $this")
