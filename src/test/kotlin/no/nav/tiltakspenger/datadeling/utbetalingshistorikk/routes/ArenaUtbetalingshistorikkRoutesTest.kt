package no.nav.tiltakspenger.datadeling.utbetalingshistorikk.routes

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
import io.ktor.http.path
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.application.exception.ExceptionHandler
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.Systembrukerroller
import no.nav.tiltakspenger.datadeling.testutils.LogCapture
import no.nav.tiltakspenger.datadeling.testutils.TestApplicationContext
import no.nav.tiltakspenger.datadeling.testutils.configureTestApplication
import no.nav.tiltakspenger.datadeling.utbetalingshistorikk.ArenaUtbetalingshistorikkService
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ArenaUtbetalingshistorikkRoutesTest {
    @Test
    fun `hent arena utbetalingshistorikk - gyldig request - returnerer respons`() {
        with(TestApplicationContext()) {
            val arenaUtbetalingshistorikkService = mockk<ArenaUtbetalingshistorikkService>()
            coEvery { arenaUtbetalingshistorikkService.hentUtbetalingshistorikk(any(), any()) } returns listOf(
                ArenaUtbetalingshistorikkResponse(
                    meldekortId = 123L,
                    dato = LocalDate.of(2026, 2, 2),
                    transaksjonstype = "UTBETALING",
                    sats = 250.0,
                    status = "OK",
                    vedtakId = 456L,
                    belop = 5000.0,
                    fraOgMedDato = LocalDate.of(2026, 1, 1),
                    tilOgMedDato = LocalDate.of(2026, 1, 31),
                ),
            )
            val token = getGyldigToken()

            testApplication {
                configureTestApplication(
                    arenaUtbetalingshistorikkService = arenaUtbetalingshistorikkService,
                    texasClient = texasClient,
                )
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("/arena/utbetalingshistorikk")
                    },
                    jwt = token,
                ) {
                    setBody(
                        """
                        {
                            "ident": "12345678910",
                            "fom": "2026-01-01",
                            "tom": "2026-01-31"
                        }
                        """.trimIndent(),
                    )
                }
                    .apply {
                        withClue(
                            "Response details:\n" +
                                "Status: ${this.status}\n" +
                                "Content-Type: ${this.contentType()}\n" +
                                "Body: ${this.bodyAsText()}\n",
                        ) {
                            status shouldBe HttpStatusCode.OK
                            contentType() shouldBe ContentType.parse("application/json")
                            bodyAsText().shouldEqualJson(
                                """
                                [
                                  {
                                    "meldekortId": 123,
                                    "dato": "2026-02-02",
                                    "transaksjonstype": "UTBETALING",
                                    "sats": 250.0,
                                    "status": "OK",
                                    "vedtakId": 456,
                                    "belop": 5000.0,
                                    "fraOgMedDato": "2026-01-01",
                                    "tilOgMedDato": "2026-01-31"
                                  }
                                ]
                                """.trimIndent(),
                            )
                        }
                    }
            }
        }
    }

    @Test
    fun `hent arena utbetalingshistorikk detaljer - mangler query-parametere - returnerer 400`() {
        with(TestApplicationContext()) {
            val arenaUtbetalingshistorikkService = mockk<ArenaUtbetalingshistorikkService>(relaxed = true)
            val token = getGyldigToken()

            testApplication {
                configureTestApplication(
                    arenaUtbetalingshistorikkService = arenaUtbetalingshistorikkService,
                    texasClient = texasClient,
                )
                client.get("/arena/utbetalingshistorikk/detaljer") {
                    headers.append("Authorization", "Bearer $token")
                }
                    .apply {
                        withClue(
                            "Response details:\n" +
                                "Status: ${this.status}\n" +
                                "Content-Type: ${this.contentType()}\n" +
                                "Body: ${this.bodyAsText()}\n",
                        ) {
                            status shouldBe HttpStatusCode.BadRequest
                            contentType() shouldBe ContentType.parse("application/json")
                            bodyAsText().shouldEqualJson(
                                """
                                {
                                  "feilmelding": "Minst én av query-parameterne 'vedtakId' eller 'meldekortId' må oppgis."
                                }
                                """.trimIndent(),
                            )
                        }
                    }
            }
        }
    }

    @Test
    fun `hent arena utbetalingshistorikk detaljer - ugyldig vedtakId - returnerer 400 uten å lekke verdien`() {
        with(TestApplicationContext()) {
            val arenaUtbetalingshistorikkService = mockk<ArenaUtbetalingshistorikkService>(relaxed = true)
            val token = getGyldigToken()

            testApplication {
                configureTestApplication(
                    arenaUtbetalingshistorikkService = arenaUtbetalingshistorikkService,
                    texasClient = texasClient,
                )
                client.get("/arena/utbetalingshistorikk/detaljer?vedtakId=ikke-tall&meldekortId=123") {
                    headers.append("Authorization", "Bearer $token")
                }
                    .apply {
                        withClue(
                            "Response details:\n" +
                                "Status: ${this.status}\n" +
                                "Content-Type: ${this.contentType()}\n" +
                                "Body: ${this.bodyAsText()}\n",
                        ) {
                            status shouldBe HttpStatusCode.BadRequest
                            contentType() shouldBe ContentType.parse("application/json")
                            bodyAsText().shouldEqualJson(
                                """
                                {
                                  "feilmelding": "Ugyldig query-parameter 'vedtakId'. Må være et heltall."
                                }
                                """.trimIndent(),
                            )
                            bodyAsText().contains("ikke-tall") shouldBe false
                        }
                    }
            }
        }
    }

    @Test
    fun `hent arena utbetalingshistorikk - ugyldig json - returnerer 400 og logger uten sensitivt innhold`() {
        with(TestApplicationContext()) {
            val arenaUtbetalingshistorikkService = mockk<ArenaUtbetalingshistorikkService>(relaxed = true)
            val token = getGyldigToken()

            LogCapture.attach(ExceptionHandler::class.java).use { logCapture ->
                testApplication {
                    configureTestApplication(
                        arenaUtbetalingshistorikkService = arenaUtbetalingshistorikkService,
                        texasClient = texasClient,
                    )
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("/arena/utbetalingshistorikk")
                        },
                        jwt = token,
                    ) {
                        setBody(
                            """
                            {
                                "ident": "12345678910",
                                "arenaResponse": "SKJULT-DETALJ",
                            """.trimIndent(),
                        )
                    }
                        .apply {
                            withClue(
                                "Response details:\n" +
                                    "Status: ${this.status}\n" +
                                    "Content-Type: ${this.contentType()}\n" +
                                    "Body: ${this.bodyAsText()}\n",
                            ) {
                                status shouldBe HttpStatusCode.BadRequest
                                contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                                bodyAsText().shouldEqualJson(
                                    """
                                    {
                                      "melding": "Ugyldig JSON i forespørselen. Kontroller syntaksen.",
                                      "kode": "ugyldig_json"
                                    }
                                    """.trimIndent(),
                                )
                                bodyAsText().contains("12345678910") shouldBe false
                                bodyAsText().contains("SKJULT-DETALJ") shouldBe false
                            }
                        }
                }

                val logs = logCapture.combined()
                logs.contains("12345678910") shouldBe false
                logs.contains("SKJULT-DETALJ") shouldBe false
                logs.contains("/arena/utbetalingshistorikk") shouldBe true
            }
        }
    }

    @Test
    fun `hent arena utbetalingshistorikk detaljer - gyldig request - returnerer respons`() {
        with(TestApplicationContext()) {
            val arenaUtbetalingshistorikkService = mockk<ArenaUtbetalingshistorikkService>()
            coEvery {
                arenaUtbetalingshistorikkService.hentUtbetalingshistorikkDetaljer(
                    meldekortId = 123L,
                    vedtakId = 456L,
                )
            } returns ArenaUtbetalingshistorikkDetaljerResponse(
                vedtakfakta = ArenaVedtakfaktaResponse(
                    dagsats = 250,
                    gjelderFra = LocalDate.of(2026, 1, 1),
                    gjelderTil = LocalDate.of(2026, 1, 31),
                    antallUtbetalinger = 2,
                    belopPerUtbetalinger = 5000,
                    alternativBetalingsmottaker = null,
                ),
                anmerkninger = listOf(
                    ArenaAnmerkningResponse(
                        kilde = "ARENA",
                        registrert = LocalDateTime.of(2026, 2, 1, 9, 0),
                        beskrivelse = "Utbetalt",
                    ),
                ),
            )
            val token = getGyldigToken()

            testApplication {
                configureTestApplication(
                    arenaUtbetalingshistorikkService = arenaUtbetalingshistorikkService,
                    texasClient = texasClient,
                )
                client.get("/arena/utbetalingshistorikk/detaljer?vedtakId=456&meldekortId=123") {
                    headers.append("Authorization", "Bearer $token")
                }
                    .apply {
                        withClue(
                            "Response details:\n" +
                                "Status: ${this.status}\n" +
                                "Content-Type: ${this.contentType()}\n" +
                                "Body: ${this.bodyAsText()}\n",
                        ) {
                            status shouldBe HttpStatusCode.OK
                            contentType() shouldBe ContentType.parse("application/json")
                            bodyAsText().shouldEqualJson(
                                """
                                {
                                  "vedtakfakta": {
                                    "dagsats": 250,
                                    "gjelderFra": "2026-01-01",
                                    "gjelderTil": "2026-01-31",
                                    "antallUtbetalinger": 2,
                                    "belopPerUtbetalinger": 5000,
                                    "alternativBetalingsmottaker": null
                                  },
                                  "anmerkninger": [
                                    {
                                      "kilde": "ARENA",
                                      "registrert": "2026-02-01T09:00:00",
                                      "beskrivelse": "Utbetalt"
                                    }
                                  ]
                                }
                                """.trimIndent(),
                            )
                        }
                    }
            }
        }
    }

    private fun TestApplicationContext.getGyldigToken(): String {
        val systembruker = Systembruker(
            roller = Systembrukerroller(listOf(Systembrukerrolle.LES_MELDEKORT)),
            klientnavn = "klientnavn",
            klientId = "id",
        )
        val token = jwtGenerator.createJwtForSystembruker(roles = listOf("les-meldekort"))
        texasClient.leggTilSystembruker(token, systembruker)
        return token
    }
}
