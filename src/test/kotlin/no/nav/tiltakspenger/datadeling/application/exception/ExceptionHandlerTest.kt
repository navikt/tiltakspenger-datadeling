package no.nav.tiltakspenger.datadeling.application.exception

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.Application
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import no.nav.tiltakspenger.datadeling.application.configureExceptions
import no.nav.tiltakspenger.datadeling.application.jacksonSerialization
import no.nav.tiltakspenger.libs.ktor.common.ErrorJson
import org.junit.jupiter.api.Test
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.exc.UnrecognizedPropertyException
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.KotlinFeature
import tools.jackson.module.kotlin.KotlinModule
import tools.jackson.module.kotlin.readValue
import java.time.LocalDate

class ExceptionHandlerTest {
    @Test
    fun `manglende paakrevd felt returnerer mangler_paakrevd_felt`() {
        testApplication {
            application { exceptionHandlerTestApplication() }

            val response = client.post("/exception-handler/missing-field") {
                contentType(ContentType.Application.Json)
                setBody("""{}""")
            }

            assertErrorResponse(
                response = response,
                expectedStatus = HttpStatusCode.BadRequest,
                expectedBody =
                """
                    {
                      "melding": "Mangler påkrevd felt 'ident'.",
                      "kode": "mangler_påkrevd_felt"
                    }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `ukjent felt returnerer ukjent_felt`() {
        val strictMapper = JsonMapper.builder()
            .addModule(
                KotlinModule.Builder()
                    .enable(KotlinFeature.KotlinPropertyNameAsImplicitName)
                    .build(),
            )
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build()

        val cause = runCatching {
            strictMapper.readValue<UnknownFieldRequest>(
                """
                {
                  "ident": "12345678910",
                  "ukjent": "verdi"
                }
                """.trimIndent(),
            )
        }.exceptionOrNull()

        val unrecognizedPropertyException = requireNotNull(cause) {
            "Forventet at streng ObjectMapper skulle kaste ved ukjent felt."
        }
        require(unrecognizedPropertyException is UnrecognizedPropertyException) {
            "Forventet UnrecognizedPropertyException, men fikk ${unrecognizedPropertyException::class.qualifiedName}."
        }

        mapClientInputError(IllegalStateException("wrapper", unrecognizedPropertyException)) shouldBe ErrorJson(
            melding = "Ugyldig forespørsel. Ukjent felt 'ukjent'.",
            kode = "ukjent_felt",
        )
    }

    @Test
    fun `ugyldig verdi returnerer ugyldig_verdi`() {
        testApplication {
            application { exceptionHandlerTestApplication() }

            val response = client.post("/exception-handler/invalid-format") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                      "dato": "ikke-en-dato"
                    }
                    """.trimIndent(),
                )
            }

            assertErrorResponse(
                response = response,
                expectedStatus = HttpStatusCode.BadRequest,
                expectedBody =
                """
                    {
                      "melding": "Ugyldig verdi i felt 'dato'. Kontroller datatype og format.",
                      "kode": "ugyldig_verdi"
                    }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `ugyldig verdi i nested liste bruker field path`() {
        testApplication {
            application { exceptionHandlerTestApplication() }

            val response = client.post("/exception-handler/nested-invalid-format") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                      "perioder": [
                        {
                          "fom": "ikke-en-dato"
                        }
                      ]
                    }
                    """.trimIndent(),
                )
            }

            assertErrorResponse(
                response = response,
                expectedStatus = HttpStatusCode.BadRequest,
                expectedBody =
                """
                    {
                      "melding": "Ugyldig verdi i felt 'perioder.[0].fom'. Kontroller datatype og format.",
                      "kode": "ugyldig_verdi"
                    }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `feil datatype eller struktur returnerer feil_datatype_eller_struktur`() {
        testApplication {
            application { exceptionHandlerTestApplication() }

            val response = client.post("/exception-handler/mismatched-input") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                      "detaljer": "skal-vare-objekt"
                    }
                    """.trimIndent(),
                )
            }

            assertErrorResponse(
                response = response,
                expectedStatus = HttpStatusCode.BadRequest,
                expectedBody =
                """
                    {
                      "melding": "Felt 'detaljer' har feil datatype eller struktur.",
                      "kode": "feil_datatype_eller_struktur"
                    }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `feil struktur paa request body bruker request-body som field path`() {
        testApplication {
            application { exceptionHandlerTestApplication() }

            val response = client.post("/exception-handler/root-mismatched-input") {
                contentType(ContentType.Application.Json)
                setBody("""[]""")
            }

            assertErrorResponse(
                response = response,
                expectedStatus = HttpStatusCode.BadRequest,
                expectedBody =
                """
                    {
                      "melding": "Felt 'request-body' har feil datatype eller struktur.",
                      "kode": "feil_datatype_eller_struktur"
                    }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `ugyldig json returnerer ugyldig_json`() {
        testApplication {
            application { exceptionHandlerTestApplication() }

            val response = client.post("/exception-handler/malformed-json") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                      "ident": "12345678910",
                    """.trimIndent(),
                )
            }

            assertErrorResponse(
                response = response,
                expectedStatus = HttpStatusCode.BadRequest,
                expectedBody =
                """
                    {
                      "melding": "Ugyldig JSON i forespørselen. Kontroller syntaksen.",
                      "kode": "ugyldig_json"
                    }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `bad request exception returnerer ugyldig_json`() {
        testApplication {
            application { exceptionHandlerTestApplication() }

            val response = client.get("/exception-handler/bad-request")

            assertErrorResponse(
                response = response,
                expectedStatus = HttpStatusCode.BadRequest,
                expectedBody =
                """
                    {
                      "melding": "Ugyldig JSON i forespørselen. Kontroller syntaksen.",
                      "kode": "ugyldig_json"
                    }
                """.trimIndent(),
            )
        }
    }

    @Test
    fun `ukjent feil returnerer server_feil`() {
        testApplication {
            application { exceptionHandlerTestApplication() }

            val response = client.post("/exception-handler/server-error") {
                contentType(ContentType.Application.Json)
                setBody(
                    """
                    {
                      "ident": "12345678910"
                    }
                    """.trimIndent(),
                )
            }

            assertErrorResponse(
                response = response,
                expectedStatus = HttpStatusCode.InternalServerError,
                expectedBody =
                """
                    {
                      "melding": "Noe gikk galt på serversiden",
                      "kode": "server_feil"
                    }
                """.trimIndent(),
            )
        }
    }

    private suspend fun assertErrorResponse(
        response: HttpResponse,
        expectedStatus: HttpStatusCode,
        expectedBody: String,
    ) {
        withClue(
            "Response details:\n" +
                "Status: ${response.status}\n" +
                "Content-Type: ${response.contentType()}\n" +
                "Body: ${response.bodyAsText()}\n",
        ) {
            response.status shouldBe expectedStatus
            response.contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
            response.bodyAsText().shouldEqualJson(expectedBody)
        }
    }
}

private fun Application.exceptionHandlerTestApplication() {
    jacksonSerialization()
    configureExceptions()
    routing {
        post("/exception-handler/missing-field") {
            call.receive<MissingFieldRequest>()
            call.respond(HttpStatusCode.OK)
        }
        post("/exception-handler/invalid-format") {
            call.receive<InvalidFormatRequest>()
            call.respond(HttpStatusCode.OK)
        }
        post("/exception-handler/nested-invalid-format") {
            call.receive<NestedInvalidFormatRequest>()
            call.respond(HttpStatusCode.OK)
        }
        post("/exception-handler/mismatched-input") {
            call.receive<MismatchedInputRequest>()
            call.respond(HttpStatusCode.OK)
        }
        post("/exception-handler/root-mismatched-input") {
            call.receive<MissingFieldRequest>()
            call.respond(HttpStatusCode.OK)
        }
        post("/exception-handler/malformed-json") {
            call.receive<MissingFieldRequest>()
            call.respond(HttpStatusCode.OK)
        }
        get("/exception-handler/bad-request") {
            throw BadRequestException("ugyldig forespørsel")
        }
        post("/exception-handler/server-error") {
            call.receive<MissingFieldRequest>()
            error("boom")
        }
    }
}

private fun mapClientInputError(cause: Throwable): ErrorJson? {
    val method = Class
        .forName("no.nav.tiltakspenger.datadeling.application.exception.ExceptionHandlerKt")
        .getDeclaredMethod("toClientInputError", Throwable::class.java)
    method.isAccessible = true
    return method.invoke(null, cause) as ErrorJson?
}

private data class MissingFieldRequest(
    val ident: String,
)

private data class UnknownFieldRequest(
    val ident: String,
)

private data class InvalidFormatRequest(
    val dato: LocalDate,
)

private data class NestedInvalidFormatRequest(
    val perioder: List<PeriodeRequest>,
)

private data class PeriodeRequest(
    val fom: LocalDate,
)

private data class MismatchedInputRequest(
    val detaljer: DetaljerRequest,
)

private data class DetaljerRequest(
    val ident: String,
)
