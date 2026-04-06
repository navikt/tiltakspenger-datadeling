package no.nav.tiltakspenger.datadeling.meldekort.datadeling.routes

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
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
import no.nav.tiltakspenger.datadeling.meldekort.datadeling.ArenaMeldekortService
import no.nav.tiltakspenger.datadeling.testutils.LogCapture
import no.nav.tiltakspenger.datadeling.testutils.TestApplicationContext
import no.nav.tiltakspenger.datadeling.testutils.configureTestApplication
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class ArenaMeldekortRoutesTest {
    @Test
    fun `hent arena meldekort - gyldig request - returnerer respons`() {
        with(TestApplicationContext()) {
            val arenaMeldekortService = mockk<ArenaMeldekortService>()
            coEvery { arenaMeldekortService.hentMeldekort(any(), any()) } returns listOf(
                ArenaMeldekortResponse(
                    meldekortId = "123",
                    mottatt = LocalDate.of(2026, 1, 15),
                    arbeidet = false,
                    kurs = false,
                    ferie = null,
                    syk = false,
                    annetFravaer = false,
                    registrert = LocalDateTime.of(2026, 1, 16, 10, 15),
                    sistEndret = LocalDateTime.of(2026, 1, 16, 10, 16),
                    type = "ELEKTRONISK",
                    status = "FERDIG",
                    statusDato = LocalDate.of(2026, 1, 16),
                    meldegruppe = "ARBS",
                    aar = 2026,
                    totaltArbeidetTimer = 0,
                    periode = ArenaMeldekortResponse.ArenaMeldekortPeriodeResponse(
                        aar = 2026,
                        periodekode = 1,
                        ukenrUke1 = 3,
                        ukenrUke2 = 4,
                        fraOgMed = LocalDate.of(2026, 1, 12),
                        tilOgMed = LocalDate.of(2026, 1, 25),
                    ),
                    dager = emptyList(),
                    fortsattArbeidsoker = true,
                ),
            )
            val token = getGyldigToken()

            testApplication {
                configureTestApplication(
                    arenaMeldekortService = arenaMeldekortService,
                    texasClient = texasClient,
                )
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("/arena/meldekort")
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
                                    "meldekortId": "123",
                                    "mottatt": "2026-01-15",
                                    "arbeidet": false,
                                    "kurs": false,
                                    "ferie": null,
                                    "syk": false,
                                    "annetFravaer": false,
                                    "registrert": "2026-01-16T10:15:00",
                                    "sistEndret": "2026-01-16T10:16:00",
                                    "type": "ELEKTRONISK",
                                    "status": "FERDIG",
                                    "statusDato": "2026-01-16",
                                    "meldegruppe": "ARBS",
                                    "aar": 2026,
                                    "totaltArbeidetTimer": 0,
                                    "periode": {
                                      "aar": 2026,
                                      "periodekode": 1,
                                      "ukenrUke1": 3,
                                      "ukenrUke2": 4,
                                      "fraOgMed": "2026-01-12",
                                      "tilOgMed": "2026-01-25"
                                    },
                                    "dager": [],
                                    "fortsattArbeidsoker": true
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
    fun `hent arena meldekort - mangler ident - returnerer 400 med nyttig feilmelding`() {
        with(TestApplicationContext()) {
            val arenaMeldekortService = mockk<ArenaMeldekortService>(relaxed = true)
            val token = getGyldigToken()

            testApplication {
                configureTestApplication(
                    arenaMeldekortService = arenaMeldekortService,
                    texasClient = texasClient,
                )
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("/arena/meldekort")
                    },
                    jwt = token,
                ) {
                    setBody("""{}""")
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
                                  "melding": "Mangler påkrevd felt 'ident'.",
                                  "kode": "mangler_påkrevd_felt"
                                }
                                """.trimIndent(),
                            )
                        }
                    }
            }
        }
    }

    @Test
    fun `hent arena meldekort - ugyldig json - returnerer 400 og logger uten sensitivt innhold`() {
        with(TestApplicationContext()) {
            val arenaMeldekortService = mockk<ArenaMeldekortService>(relaxed = true)
            val token = getGyldigToken()

            LogCapture.attach(ExceptionHandler::class.java).use { logCapture ->
                testApplication {
                    configureTestApplication(
                        arenaMeldekortService = arenaMeldekortService,
                        texasClient = texasClient,
                    )
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("/arena/meldekort")
                        },
                        jwt = token,
                    ) {
                        setBody(
                            """
                            {
                                "ident": "12345678910",
                                "debug": "ARENA-HEMMELIG",
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
                                bodyAsText().contains("ARENA-HEMMELIG") shouldBe false
                            }
                        }
                }

                val logs = logCapture.combined()
                logs.contains("12345678910") shouldBe false
                logs.contains("ARENA-HEMMELIG") shouldBe false
                logs.contains("/arena/meldekort") shouldBe true
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
