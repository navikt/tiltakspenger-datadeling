package no.nav.tiltakspenger.datadeling.routes.vedtak

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
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.datadeling.jacksonSerialization
import no.nav.tiltakspenger.datadeling.routes.defaultRequest
import no.nav.tiltakspenger.datadeling.routes.vedtakPath
import no.nav.tiltakspenger.datadeling.routes.vedtakRoutes
import no.nav.tiltakspenger.datadeling.service.VedtakService
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import java.time.LocalDate

class VedtakRoutesHentTest {

    private val vedtakService = mockk<VedtakService>(relaxed = true)

    @Test
    fun `test hent vedtak route`() {
        coEvery { vedtakService.hentVedtak(any(), any(), any()) } returns listOf(
            Vedtak(
                fom = LocalDate.of(2020, 1, 1),
                tom = LocalDate.of(2024, 12, 31),
                antallDager = 10.0,
                dagsatsTiltakspenger = 285,
                dagsatsBarnetillegg = 0,
                antallBarn = 0,
                relaterteTiltak = "",
                rettighet = Rettighet.TILTAKSPENGER,
                vedtakId = "",
                sakId = "",
            ),
        )
        testApplication {
            konfigurerTestApplikasjon()
            defaultRequest(
                HttpMethod.Post,
                url {
                    protocol = URLProtocol.HTTPS
                    path("$vedtakPath/detaljer")
                },
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
                    status shouldBe HttpStatusCode.OK
                    contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                    JSONAssert.assertEquals(
                        // language=JSON
                        """[
                            {
                              "fom":"2020-01-01",
                              "tom":"2024-12-31",
                              "antallDager":10.0,
                              "dagsatsTiltakspenger":285,
                              "dagsatsBarnetillegg":0,
                              "antallBarn":0
                            }
                            ]
                        """.trimIndent(),
                        bodyAsText(),
                        JSONCompareMode.LENIENT,
                    )
                }
        }
    }

    @Test
    fun `test at vi kan hente uten å oppgi dato`() {
        coEvery { vedtakService.hentVedtak(any(), any(), any()) } returns listOf(
            Vedtak(
                fom = LocalDate.of(2020, 1, 1),
                tom = LocalDate.of(2024, 12, 31),
                antallDager = 10.0,
                dagsatsTiltakspenger = 285,
                dagsatsBarnetillegg = 0,
                antallBarn = 0,
                relaterteTiltak = "",
                rettighet = Rettighet.TILTAKSPENGER,
                vedtakId = "",
                sakId = "",
            ),
        )
        testApplication {
            konfigurerTestApplikasjon()
            defaultRequest(
                HttpMethod.Post,
                url {
                    protocol = URLProtocol.HTTPS
                    path("$vedtakPath/detaljer")
                },
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
                    status shouldBe HttpStatusCode.OK
                    contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                    JSONAssert.assertEquals(
                        // language=JSON
                        """[
                            {
                              "fom":"2020-01-01",
                              "tom":"2024-12-31",
                              "antallDager":10.0,
                              "dagsatsTiltakspenger":285,
                              "dagsatsBarnetillegg":0,
                              "antallBarn":0
                            }
                            ]
                        """.trimIndent(),
                        bodyAsText(),
                        JSONCompareMode.LENIENT,
                    )
                }
        }
    }

    @Test
    fun `test at uten ident gir feilmelding`() {
        testApplication {
            konfigurerTestApplikasjon()
            defaultRequest(
                HttpMethod.Post,
                url {
                    protocol = URLProtocol.HTTPS
                    path("$vedtakPath/detaljer")
                },
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
                    status shouldBe HttpStatusCode.BadRequest
                    contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                    JSONAssert.assertEquals(
                        // language=JSON
                        """
                            { "feilmelding" : "Mangler ident" }
                        """.trimIndent(),
                        bodyAsText(),
                        JSONCompareMode.LENIENT,
                    )
                }
        }
    }

    @Test
    fun `test at fom som ikke kan parses som en gyldig dato gir feilmelding`() {
        testApplication {
            konfigurerTestApplikasjon()
            defaultRequest(
                HttpMethod.Post,
                url {
                    protocol = URLProtocol.HTTPS
                    path("$vedtakPath/detaljer")
                },
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
                    status shouldBe HttpStatusCode.BadRequest
                    contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                    JSONAssert.assertEquals(
                        // language=JSON
                        """
                            { "feilmelding" : "Ugyldig datoformat for fom-dato: 202X-01-01" }
                        """.trimIndent(),
                        bodyAsText(),
                        JSONCompareMode.LENIENT,
                    )
                }
        }
    }

    @Test
    fun `test at tom som ikke kan parses som en gyldig dato gir feilmelding`() {
        testApplication {
            konfigurerTestApplikasjon()
            defaultRequest(
                HttpMethod.Post,
                url {
                    protocol = URLProtocol.HTTPS
                    path("$vedtakPath/detaljer")
                },
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
                    status shouldBe HttpStatusCode.BadRequest
                    contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                    JSONAssert.assertEquals(
                        // language=JSON
                        """
                            { "feilmelding" : "Ugyldig datoformat for tom-dato: 202X-12-31" }
                        """.trimIndent(),
                        bodyAsText(),
                        JSONCompareMode.LENIENT,
                    )
                }
        }
    }

    @Test
    fun `test at fom og tom gir feilmelding når de ikke kommer i rikgit rekkefølge`() {
        testApplication {
            konfigurerTestApplikasjon()
            defaultRequest(
                HttpMethod.Post,
                url {
                    protocol = URLProtocol.HTTPS
                    path("$vedtakPath/detaljer")
                },
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
                    status shouldBe HttpStatusCode.BadRequest
                    contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                    JSONAssert.assertEquals(
                        // language=JSON
                        """
                            { "feilmelding" : "Fra-dato 2021-01-01 ikke være etter til-dato 2020-12-31" }
                        """.trimIndent(),
                        bodyAsText(),
                        JSONCompareMode.LENIENT,
                    )
                }
        }
    }

    private fun ApplicationTestBuilder.konfigurerTestApplikasjon() {
        application {
            jacksonSerialization()
            routing {
                vedtakRoutes(
                    vedtakService = vedtakService,
                )
            }
        }
    }
}
