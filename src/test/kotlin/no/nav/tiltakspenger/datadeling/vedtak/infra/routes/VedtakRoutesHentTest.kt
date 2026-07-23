package no.nav.tiltakspenger.datadeling.vedtak.infra.routes

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
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.Systembruker
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.Systembrukerroller
import no.nav.tiltakspenger.datadeling.infra.jacksonSerialization
import no.nav.tiltakspenger.datadeling.infra.setupAuthentication
import no.nav.tiltakspenger.datadeling.sak.Sak
import no.nav.tiltakspenger.datadeling.testutils.TestApplicationContext
import no.nav.tiltakspenger.datadeling.vedtak.Barnetillegg
import no.nav.tiltakspenger.datadeling.vedtak.BarnetilleggPeriode
import no.nav.tiltakspenger.datadeling.vedtak.HentTpVedtakService
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengeVedtakMedSak
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.dato.desember
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juli
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.satser.Satser
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class VedtakRoutesHentTest {
    private val satser2024 = Satser.sats(1.januar(2024))

    @Test
    fun `et vedtak med tiltakspenger`() {
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<HentTpVedtakService>(relaxed = true)
            val virkningsperiode = Periode(LocalDate.of(2020, 1, 1), LocalDate.of(2024, 12, 31))
            coEvery { vedtakService.hentTpVedtak(any(), any()) } returns listOf(
                TiltakspengeVedtakMedSak(
                    vedtak = TiltakspengerVedtak(
                        virkningsperiode = virkningsperiode,
                        innvilgelsesperiode = virkningsperiode,
                        omgjørRammevedtakId = null,
                        omgjortAvRammevedtakId = null,
                        rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                        vedtakId = "v1",
                        sakId = SakId.fromString("sak_01ARZ3NDEKTSV4RRFFQ69G5FAV"),
                        mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        barnetillegg = Barnetillegg(
                            perioder = listOf(
                                BarnetilleggPeriode(
                                    antallBarn = 1,
                                    periode = Periode(virkningsperiode.fraOgMed, virkningsperiode.tilOgMed),
                                ),
                            ),
                        ),
                        valgteHjemlerHarIkkeRettighet = null,
                        saksnummer = Saksnummer("202401011001"),
                        fnr = Fnr.fromString("12345678901"),
                    ),
                    sak = Sak(
                        id = SakId.fromString("sak_01ARZ3NDEKTSV4RRFFQ69G5FAV"),
                        saksnummer = Saksnummer("202401011001"),
                        fnr = Fnr.random(),
                        opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                    ),
                ),
            )
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                hentTpVedtakService = vedtakService,
                                hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                hentVedtaksperioderService = mockk(relaxed = true),
                                hentSakService = mockk(relaxed = true),
                                clock = fixedClock,
                            )
                        }
                    }
                }
                defaultRequestWithAssertions(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/detaljer")
                    },

                    jwt = token,
                    forventet = ForventetRespons(
                        status = HttpStatusCode.OK,
                        body = ForventetBody.Json(
                            // language=JSON
                            """[
                            {
                              "fom":"2020-01-01",
                              "tom":"2024-12-31",
                              "rettighet":"TILTAKSPENGER",
                              "vedtakId": "v1",  
                              "sakId": "sak_01ARZ3NDEKTSV4RRFFQ69G5FAV",  
                              "saksnummer":"202401011001",
                              "kilde":"tp",
                              "sats":${satser2024.sats},
                              "satsBarnetillegg":0
                            }
                            ]
                            """.trimIndent(),
                        ),
                        contentType = ContentType.parse("application/json"),
                    ),
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
            }
        }
    }

    @Test
    fun stansvedtak() {
        val fnr = Fnr.random()
        val saksnummer = Saksnummer("202401011001")
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<HentTpVedtakService>(relaxed = true)
            val virkningsperiode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30))
            coEvery { vedtakService.hentTpVedtak(any(), any()) } returns listOf(
                TiltakspengeVedtakMedSak(
                    vedtak = TiltakspengerVedtak(
                        virkningsperiode = virkningsperiode,
                        innvilgelsesperiode = virkningsperiode,
                        omgjørRammevedtakId = null,
                        omgjortAvRammevedtakId = null,
                        rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                        vedtakId = "v1",
                        sakId = SakId.fromString("sak_01ARZ3NDEKTSV4RRFFQ69G5FAV"),
                        mottattTidspunkt = LocalDateTime.parse("2024-01-01T00:00:00.000"),
                        opprettet = LocalDateTime.parse("2024-01-01T00:00:00.000"),
                        barnetillegg = null,
                        valgteHjemlerHarIkkeRettighet = null,
                        saksnummer = Saksnummer("202401011001"),
                        fnr = Fnr.fromString("12345678901"),
                    ),
                    sak = Sak(
                        id = SakId.fromString("sak_01ARZ3NDEKTSV4RRFFQ69G5FAV"),
                        saksnummer = saksnummer,
                        fnr = fnr,
                        opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                    ),
                ),
                TiltakspengeVedtakMedSak(
                    vedtak = TiltakspengerVedtak(
                        virkningsperiode = 1.juli(2024) til 31.desember(2024),
                        innvilgelsesperiode = 1.juli(2024) til 31.desember(2024),
                        omgjørRammevedtakId = null,
                        omgjortAvRammevedtakId = null,
                        rettighet = TiltakspengerVedtak.Rettighet.STANS,
                        vedtakId = "v2",
                        sakId = SakId.fromString("sak_01ARZ3NDEKTSV4RRFFQ69G5FAV"),
                        mottattTidspunkt = LocalDateTime.parse("2024-01-01T00:00:00.000"),
                        opprettet = LocalDateTime.parse("2024-01-01T00:00:00.000"),
                        barnetillegg = null,
                        valgteHjemlerHarIkkeRettighet = listOf(TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK),
                        saksnummer = Saksnummer("202401011001"),
                        fnr = Fnr.fromString("12345678901"),
                    ),
                    sak = Sak(
                        id = SakId.fromString("sak_01ARZ3NDEKTSV4RRFFQ69G5FAV"),
                        saksnummer = saksnummer,
                        fnr = fnr,
                        opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                    ),
                ),
            )
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                hentTpVedtakService = vedtakService,
                                hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                hentVedtaksperioderService = mockk(relaxed = true),
                                hentSakService = mockk(relaxed = true),
                                clock = fixedClock,
                            )
                        }
                    }
                }
                defaultRequestWithAssertions(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/detaljer")
                    },

                    jwt = token,
                    forventet = ForventetRespons(
                        status = HttpStatusCode.OK,
                        body = ForventetBody.Json(
                            // language=JSON
                            """[
                            {
                              "fom":"2024-01-01",
                              "tom":"2024-06-30",
                              "rettighet":"TILTAKSPENGER",
                              "vedtakId": "v1",  
                              "sakId": "sak_01ARZ3NDEKTSV4RRFFQ69G5FAV",  
                              "saksnummer":"202401011001",
                              "kilde":"tp",
                              "sats":${satser2024.sats},
                              "satsBarnetillegg":0
                            },
                            {
                              "fom":"2024-07-01",
                              "tom":"2024-12-31",
                              "rettighet":"INGENTING",
                              "vedtakId": "v2",  
                              "sakId": "sak_01ARZ3NDEKTSV4RRFFQ69G5FAV",  
                              "saksnummer":"202401011001",
                              "kilde":"tp",
                              "sats":null,
                              "satsBarnetillegg":null
                            }
                            ]
                            """.trimIndent(),
                        ),
                        contentType = ContentType.parse("application/json"),
                    ),
                ) {
                    setBody(
                        """
                        {
                            "ident": "12345678910",
                            "fom": "2024-01-01",
                            "tom": "2024-12-31"
                        }
                        """.trimIndent(),
                    )
                }
            }
        }
    }

    @Test
    fun `test at vi kan hente uten å oppgi dato`() {
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<HentTpVedtakService>(relaxed = true)
            coEvery { vedtakService.hentTpVedtak(any(), any()) } returns listOf(
                TiltakspengeVedtakMedSak(
                    vedtak = TiltakspengerVedtak(
                        virkningsperiode = 1.januar(2020) til 31.desember(2024),
                        innvilgelsesperiode = 1.januar(2020) til 31.desember(2024),
                        omgjørRammevedtakId = null,
                        omgjortAvRammevedtakId = null,
                        rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                        vedtakId = "v1",
                        sakId = SakId.fromString("sak_01ARZ3NDEKTSV4RRFFQ69G5FAV"),
                        mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        opprettet = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        barnetillegg = null,
                        valgteHjemlerHarIkkeRettighet = null,
                        saksnummer = Saksnummer("202401011001"),
                        fnr = Fnr.fromString("12345678901"),
                    ),
                    sak = Sak(
                        id = SakId.fromString("sak_01ARZ3NDEKTSV4RRFFQ69G5FAV"),
                        saksnummer = Saksnummer("202401011001"),
                        fnr = Fnr.random(),
                        opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                    ),
                ),
            )
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                hentTpVedtakService = vedtakService,
                                hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                hentVedtaksperioderService = mockk(relaxed = true),
                                hentSakService = mockk(relaxed = true),
                                clock = fixedClock,
                            )
                        }
                    }
                }
                defaultRequestWithAssertions(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/detaljer")
                    },

                    jwt = token,
                    forventet = ForventetRespons(
                        status = HttpStatusCode.OK,
                        body = ForventetBody.Json(
                            // language=JSON
                            """[
                            {
                              "fom":"2020-01-01",
                              "tom":"2024-12-31",
                              "rettighet":"TILTAKSPENGER",
                              "vedtakId":"v1",
                              "sakId":"sak_01ARZ3NDEKTSV4RRFFQ69G5FAV",
                              "saksnummer":"202401011001",
                              "kilde":"tp",
                              "sats":${satser2024.sats},
                              "satsBarnetillegg":0
                            }
                            ]
                            """.trimIndent(),
                        ),
                        contentType = ContentType.parse("application/json"),
                    ),
                ) {
                    setBody(
                        """
                        {
                            "ident": "12345678910"
                        }
                        """.trimIndent(),
                    )
                }
            }
        }
    }

    @Test
    fun `test at uten ident gir feilmelding`() {
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<HentTpVedtakService>(relaxed = true)
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                hentTpVedtakService = vedtakService,
                                hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                hentVedtaksperioderService = mockk(relaxed = true),
                                hentSakService = mockk(relaxed = true),
                                clock = fixedClock,
                            )
                        }
                    }
                }
                defaultRequestWithAssertions(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/detaljer")
                    },
                    jwt = token,
                    forventet = ForventetRespons(
                        status = HttpStatusCode.BadRequest,
                        body = ForventetBody.Json(
                            // language=JSON
                            """
                            { "feilmelding" : "Ugyldig ident. Må bestå av 11 siffer." }
                            """.trimIndent(),
                        ),
                        contentType = ContentType.parse("application/json"),
                    ),
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
            }
        }
    }

    @Test
    fun `test at fom som ikke kan parses som en gyldig dato gir feilmelding`() {
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<HentTpVedtakService>(relaxed = true)
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                hentTpVedtakService = vedtakService,
                                hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                hentVedtaksperioderService = mockk(relaxed = true),
                                hentSakService = mockk(relaxed = true),
                                clock = fixedClock,
                            )
                        }
                    }
                }
                defaultRequestWithAssertions(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/detaljer")
                    },
                    jwt = token,
                    forventet = ForventetRespons(
                        status = HttpStatusCode.BadRequest,
                        body = ForventetBody.Json(
                            // language=JSON
                            """
                            { "feilmelding" : "Ugyldig datoformat i felt 'fom'. Forventet format er yyyy-MM-dd." }
                            """.trimIndent(),
                        ),
                        contentType = ContentType.parse("application/json"),
                    ),
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
            }
        }
    }

    @Test
    fun `test at tom som ikke kan parses som en gyldig dato gir feilmelding`() {
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<HentTpVedtakService>(relaxed = true)
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                hentTpVedtakService = vedtakService,
                                hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                hentVedtaksperioderService = mockk(relaxed = true),
                                hentSakService = mockk(relaxed = true),
                                clock = fixedClock,
                            )
                        }
                    }
                }
                defaultRequestWithAssertions(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/detaljer")
                    },
                    jwt = token,
                    forventet = ForventetRespons(
                        status = HttpStatusCode.BadRequest,
                        body = ForventetBody.Json(
                            // language=JSON
                            """
                            { "feilmelding" : "Ugyldig datoformat i felt 'tom'. Forventet format er yyyy-MM-dd." }
                            """.trimIndent(),
                        ),
                        contentType = ContentType.parse("application/json"),
                    ),
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
            }
        }
    }

    @Test
    fun `test at fom og tom gir feilmelding når de ikke kommer i riktig rekkefølge`() {
        with(TestApplicationContext()) {
            val tac = this

            val vedtakService = mockk<HentTpVedtakService>(relaxed = true)
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                hentTpVedtakService = vedtakService,
                                hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                hentVedtaksperioderService = mockk(relaxed = true),
                                hentSakService = mockk(relaxed = true),
                                clock = fixedClock,
                            )
                        }
                    }
                }
                defaultRequestWithAssertions(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/detaljer")
                    },
                    jwt = token,
                    forventet = ForventetRespons(
                        status = HttpStatusCode.BadRequest,
                        body = ForventetBody.Json(
                            // language=JSON
                            """
                            { "feilmelding" : "Fra-dato kan ikke være etter til-dato." }
                            """.trimIndent(),
                        ),
                        contentType = ContentType.parse("application/json"),
                    ),
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
            }
        }
    }
}
