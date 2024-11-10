package no.nav.tiltakspenger.datadeling.routes.vedtak

import arrow.core.right
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
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.datadeling.jacksonSerialization
import no.nav.tiltakspenger.datadeling.routes.TestApplicationContext
import no.nav.tiltakspenger.datadeling.routes.vedtakPath
import no.nav.tiltakspenger.datadeling.routes.vedtakRoutes
import no.nav.tiltakspenger.datadeling.service.VedtakService
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakRoutesHentTest {

    @Test
    fun `test hent vedtak route`() {
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<VedtakService>(relaxed = true)
            coEvery { vedtakService.hentVedtak(any(), any(), any(), any()) } returns listOf(
                Vedtak(
                    fom = LocalDate.of(2020, 1, 1),
                    tom = LocalDate.of(2024, 12, 31),
                    antallDager = 10.0,
                    dagsatsTiltakspenger = 285,
                    dagsatsBarnetillegg = 0,
                    antallBarn = 0,
                    tiltaksgjennomføringId = "",
                    rettighet = Rettighet.TILTAKSPENGER,
                    vedtakId = "",
                    sakId = "",
                    saksnummer = "12345",
                    kilde = "tp",
                    fnr = Fnr.random(),
                ),
            ).right()
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        vedtakRoutes(
                            vedtakService = vedtakService,
                            tokenService = tokenService,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$vedtakPath/detaljer")
                    },

                    jwt = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak")),
                ) {
                    setBody(
                        """
                        {
                            "ident": "12345678910",
                            "fom": "2021-01-01",
                            "tom": "2021-12-31"
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
                            contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                            bodyAsText().shouldEqualJson(
                                // language=JSON
                                """[
                            {
                              "fom":"2020-01-01",
                              "tom":"2024-12-31",
                              "antallDager":10.0,
                              "dagsatsTiltakspenger":285,
                              "dagsatsBarnetillegg":0,
                              "antallBarn":0,
                              "relaterteTiltak":"",
                              "rettighet":"TILTAKSPENGER",
                              "vedtakId": "",  
                              "sakId": "",  
                              "saksnummer":"12345",
                              "kilde":"tp"
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
    fun `test at vi kan hente uten å oppgi dato`() {
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<VedtakService>(relaxed = true)
            coEvery { vedtakService.hentVedtak(any(), any(), any(), any()) } returns listOf(
                Vedtak(
                    fom = LocalDate.of(2020, 1, 1),
                    tom = LocalDate.of(2024, 12, 31),
                    antallDager = 10.0,
                    dagsatsTiltakspenger = 285,
                    dagsatsBarnetillegg = 0,
                    antallBarn = 0,
                    tiltaksgjennomføringId = "",
                    rettighet = Rettighet.TILTAKSPENGER,
                    vedtakId = "",
                    sakId = "",
                    saksnummer = "12345",
                    kilde = "tp",
                    fnr = Fnr.random(),
                ),
            ).right()
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        vedtakRoutes(
                            vedtakService = vedtakService,
                            tokenService = tokenService,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$vedtakPath/detaljer")
                    },

                    jwt = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak")),
                ) {
                    setBody(
                        """
                        {
                            "ident": "12345678910"
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
                            contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                            bodyAsText().shouldEqualJson(
                                // language=JSON
                                """[
                            {
                              "fom":"2020-01-01",
                              "tom":"2024-12-31",
                              "antallDager":10.0,
                              "dagsatsTiltakspenger":285,
                              "dagsatsBarnetillegg":0,
                              "antallBarn":0,
                              "relaterteTiltak":"",
                              "rettighet":"TILTAKSPENGER",
                              "vedtakId":"",
                              "sakId":"",
                              "saksnummer":"12345",
                              "kilde":"tp"
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
    fun `test at uten ident gir feilmelding`() {
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<VedtakService>(relaxed = true)
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        vedtakRoutes(
                            vedtakService = vedtakService,
                            tokenService = tokenService,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$vedtakPath/detaljer")
                    },
                    jwt = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak")),
                ) {
                    setBody(
                        """
                        {
                            "ident": "",
                            "fom": "2021-01-01",
                            "tom": "2021-12-31"
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
                            status shouldBe HttpStatusCode.BadRequest
                            contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                            bodyAsText().shouldEqualJson(
                                // language=JSON
                                """
                            { "feilmelding" : "Mangler ident" }
                                """.trimIndent(),
                            )
                        }
                    }
            }
        }
    }

    @Test
    fun `test at fom som ikke kan parses som en gyldig dato gir feilmelding`() {
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<VedtakService>(relaxed = true)
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        vedtakRoutes(
                            vedtakService = vedtakService,
                            tokenService = tokenService,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$vedtakPath/detaljer")
                    },
                    jwt = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak")),
                ) {
                    setBody(
                        """
                        {
                            "ident": "01234567891",
                            "fom": "202X-01-01",
                            "tom": "2021-12-31"
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
                            status shouldBe HttpStatusCode.BadRequest
                            contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                            bodyAsText().shouldEqualJson(
                                // language=JSON
                                """
                            { "feilmelding" : "Ugyldig datoformat for fom-dato: 202X-01-01" }
                                """.trimIndent(),
                            )
                        }
                    }
            }
        }
    }

    @Test
    fun `test at tom som ikke kan parses som en gyldig dato gir feilmelding`() {
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<VedtakService>(relaxed = true)
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        vedtakRoutes(
                            vedtakService = vedtakService,
                            tokenService = tokenService,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$vedtakPath/detaljer")
                    },
                    jwt = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak")),
                ) {
                    setBody(
                        """
                        {
                            "ident": "01234567891",
                            "fom": "2020-01-01",
                            "tom": "202X-12-31"
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
                            status shouldBe HttpStatusCode.BadRequest
                            contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                            bodyAsText().shouldEqualJson(
                                // language=JSON
                                """
                            { "feilmelding" : "Ugyldig datoformat for tom-dato: 202X-12-31" }
                                """.trimIndent(),
                            )
                        }
                    }
            }
        }
    }

    @Test
    fun `test at fom og tom gir feilmelding når de ikke kommer i rikgit rekkefølge`() {
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<VedtakService>(relaxed = true)
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        vedtakRoutes(
                            vedtakService = vedtakService,
                            tokenService = tokenService,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$vedtakPath/detaljer")
                    },
                    jwt = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak")),
                ) {
                    setBody(
                        """
                        {
                            "ident": "01234567891",
                            "fom": "2021-01-01",
                            "tom": "2020-12-31"
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
                            status shouldBe HttpStatusCode.BadRequest
                            contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                            bodyAsText().shouldEqualJson(
                                // language=JSON
                                """
                            { "feilmelding" : "Fra-dato 2021-01-01 ikke være etter til-dato 2020-12-31" }
                                """.trimIndent(),
                            )
                        }
                    }
            }
        }
    }
}
