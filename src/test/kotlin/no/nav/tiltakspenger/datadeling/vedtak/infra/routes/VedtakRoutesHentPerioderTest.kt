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
import no.nav.tiltakspenger.datadeling.Kilde
import no.nav.tiltakspenger.datadeling.Systembruker
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.Systembrukerroller
import no.nav.tiltakspenger.datadeling.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.arena.ArenaVedtak
import no.nav.tiltakspenger.datadeling.arena.Rettighet
import no.nav.tiltakspenger.datadeling.infra.jacksonSerialization
import no.nav.tiltakspenger.datadeling.infra.setupAuthentication
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.datadeling.testutils.TestApplicationContext
import no.nav.tiltakspenger.datadeling.testutils.configureTestApplication
import no.nav.tiltakspenger.datadeling.testutils.defaultRequestMedKontraktsverifisering
import no.nav.tiltakspenger.datadeling.testutils.suksessRespons
import no.nav.tiltakspenger.datadeling.testutils.uventetStatusFeil
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.datadeling.vedtak.Barnetillegg
import no.nav.tiltakspenger.datadeling.vedtak.BarnetilleggPeriode
import no.nav.tiltakspenger.datadeling.vedtak.HentVedtaksperioderService
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import no.nav.tiltakspenger.libs.satser.Satser
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakRoutesHentPerioderTest {
    private val satser2024 = Satser.sats(1.januar(2024))

    @Test
    fun `hent vedtaksperioder - har vedtak fra arena og tpsak - riktig respons`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val sakRepo = testDataHelper.sakRepo
                val vedtakRepo = testDataHelper.vedtakRepo
                val arenaClient = mockk<ArenaClient>()

                val fnr = Fnr.fromString("12345678910")
                val sak = SakMother.sak(fnr = fnr)
                sakRepo.lagre(sak)
                val tpVedtak = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId",
                    sakId = sak.id,
                    virkningsperiode = 1.januar(2024) til 1.mars(2024),
                )
                vedtakRepo.lagre(tpVedtak)

                val arenaVedtak = ArenaVedtak(
                    periode = Periode(
                        tpVedtak.periode.fraOgMed.minusMonths(6),
                        tpVedtak.periode.fraOgMed.minusMonths(2),
                    ),
                    rettighet = Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
                    vedtakId = "id",
                    kilde = Kilde.ARENA,
                    fnr = sak.fnr,
                    antallBarn = 1,
                    dagsatsTiltakspenger = 285,
                    dagsatsBarnetillegg = 53,
                    beslutningsdato = tpVedtak.periode.fraOgMed.minusMonths(5),
                    sak = ArenaVedtak.Sak(
                        sakId = tpVedtak.sakId.toString(),
                        saksnummer = sak.saksnummer.verdi,
                        opprettetDato = tpVedtak.periode.fraOgMed.minusMonths(4),
                        status = "Aktiv",
                    ),
                )
                val vedtakService = HentVedtaksperioderService(vedtakRepo, arenaClient, fixedClock)
                coEvery { arenaClient.hentVedtak(any(), any()) } returns suksessRespons(listOf(arenaVedtak))

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
                                    hentTpVedtakService = mockk(relaxed = true),
                                    hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                    hentVedtaksperioderService = vedtakService,
                                    hentSakService = mockk(relaxed = true),
                                    clock = fixedClock,
                                )
                            }
                        }
                    }
                    defaultRequestMedKontraktsverifisering(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("${VEDTAK_PATH}/perioder")
                        },
                        jwt = token,
                        forventet = ForventetRespons(
                            status = HttpStatusCode.OK,
                            body = ForventetBody.Json(
                                """
                                        [
                                          {
                                            "vedtakId": "id",
                                            "rettighet": "TILTAKSPENGER_OG_BARNETILLEGG",
                                            "periode": {
                                              "fraOgMed": "2023-07-01",
                                              "tilOgMed": "2023-11-01"
                                            },
                                            "kilde": "ARENA",
                                            "barnetillegg": {
                                              "perioder": [
                                                {
                                                  "antallBarn": 1,
                                                  "periode": {
                                                    "fraOgMed": "2023-07-01",
                                                    "tilOgMed": "2023-11-01"
                                                  }
                                                }
                                              ]
                                            },
                                            "sats": 285,
                                            "satsBarnetillegg": 53,
                                             "vedtaksperiode": {
                                              "fraOgMed": "2023-07-01",
                                              "tilOgMed": "2023-11-01"
                                            },
                                            "innvilgelsesperioder": [
                                              {
                                                "fraOgMed": "2023-07-01",
                                                "tilOgMed": "2023-11-01"
                                              }
                                            ],
                                            "omgjortAvRammevedtakId": null,
                                            "omgjorRammevedtakId": null,
                                            "vedtakstidspunkt": "2023-08-01T09:00:00+02:00"
                                          },
                                          {
                                            "vedtakId": "vedtakId",
                                            "rettighet": "TILTAKSPENGER",
                                            "periode": {
                                              "fraOgMed": "2024-01-01",
                                              "tilOgMed": "2024-03-01"
                                            },
                                            "kilde": "TPSAK",
                                            "barnetillegg": null,
                                            "sats": ${satser2024.sats},
                                            "satsBarnetillegg": 0,
                                            "vedtaksperiode": {
                                              "fraOgMed": "2024-01-01",
                                              "tilOgMed": "2024-03-01"
                                            },
                                            "innvilgelsesperioder": [
                                              {
                                                "fraOgMed": "2024-01-01",
                                                "tilOgMed": "2024-03-01"
                                              }
                                            ],
                                            "omgjortAvRammevedtakId": null,
                                            "omgjorRammevedtakId": null,
                                            "vedtakstidspunkt": "2021-01-01T00:00:00+01:00"
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
                            "fom": "2023-01-01",
                            "tom": "2024-12-31"
                        }
                            """.trimIndent(),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `hent vedtaksperioder - dekker datadelingsvedtak uten avslag-varianter - riktig respons`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val sakRepo = testDataHelper.sakRepo
                val vedtakRepo = testDataHelper.vedtakRepo
                val arenaClient = mockk<ArenaClient>()

                val fnr = Fnr.fromString("12345678911")
                val sak = SakMother.sak(fnr = fnr)
                sakRepo.lagre(sak)
                val tpVedtakMedBarnetillegg = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "tp-med-barnetillegg",
                    sakId = sak.id,
                    saksnummer = sak.saksnummer,
                    fnr = fnr,
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
                    virkningsperiode = 1.januar(2024) til 31.januar(2024),
                    barnetillegg = Barnetillegg(
                        perioder = listOf(
                            BarnetilleggPeriode(
                                antallBarn = 2,
                                periode = 1.januar(2024) til 31.januar(2024),
                            ),
                        ),
                    ),
                    opprettetTidspunkt = LocalDate.of(2024, 1, 1).atStartOfDay(),
                )
                vedtakRepo.lagre(tpVedtakMedBarnetillegg)

                val arenaVedtakFraTpsakKilde = ArenaVedtak(
                    periode = 1.januar(2023) til 31.januar(2023),
                    rettighet = Rettighet.TILTAKSPENGER,
                    vedtakId = "arena-med-tpsak-kilde",
                    kilde = Kilde.TPSAK,
                    fnr = fnr,
                    antallBarn = 0,
                    dagsatsTiltakspenger = 285,
                    dagsatsBarnetillegg = 0,
                    beslutningsdato = null,
                    sak = ArenaVedtak.Sak(
                        sakId = "arena-sak-id",
                        saksnummer = "arena-saksnummer",
                        opprettetDato = 1.januar(2023),
                        status = "Aktiv",
                    ),
                )
                val arenaVedtakUtenRett = ArenaVedtak(
                    periode = 1.februar(2023) til 28.februar(2023),
                    rettighet = Rettighet.INGENTING,
                    vedtakId = "arena-uten-rett",
                    kilde = Kilde.ARENA,
                    fnr = fnr,
                    antallBarn = 0,
                    dagsatsTiltakspenger = null,
                    dagsatsBarnetillegg = null,
                    beslutningsdato = null,
                    sak = ArenaVedtak.Sak(
                        sakId = "arena-sak-id",
                        saksnummer = "arena-saksnummer",
                        opprettetDato = 1.januar(2023),
                        status = "Aktiv",
                    ),
                )
                val vedtakService = HentVedtaksperioderService(vedtakRepo, arenaClient, fixedClock)
                coEvery { arenaClient.hentVedtak(any(), any()) } returns suksessRespons(listOf(arenaVedtakFraTpsakKilde, arenaVedtakUtenRett))

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
                                    hentTpVedtakService = mockk(relaxed = true),
                                    hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                    hentVedtaksperioderService = vedtakService,
                                    hentSakService = mockk(relaxed = true),
                                    clock = fixedClock,
                                )
                            }
                        }
                    }
                    defaultRequestMedKontraktsverifisering(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("${VEDTAK_PATH}/perioder")
                        },
                        jwt = token,
                        forventet = ForventetRespons(
                            status = HttpStatusCode.OK,
                            body = ForventetBody.Json(
                                """
                                    [
                                      {
                                        "vedtakId": "arena-med-tpsak-kilde",
                                        "rettighet": "TILTAKSPENGER",
                                        "periode": {
                                          "fraOgMed": "2023-01-01",
                                          "tilOgMed": "2023-01-31"
                                        },
                                        "kilde": "TPSAK",
                                        "barnetillegg": null,
                                        "sats": 285,
                                        "satsBarnetillegg": 0,
                                        "vedtaksperiode": {
                                          "fraOgMed": "2023-01-01",
                                          "tilOgMed": "2023-01-31"
                                        },
                                        "innvilgelsesperioder": [
                                          {
                                            "fraOgMed": "2023-01-01",
                                            "tilOgMed": "2023-01-31"
                                          }
                                        ],
                                        "omgjortAvRammevedtakId": null,
                                        "omgjorRammevedtakId": null,
                                        "vedtakstidspunkt": null
                                      },
                                      {
                                        "vedtakId": "arena-uten-rett",
                                        "rettighet": "INGENTING",
                                        "periode": {
                                          "fraOgMed": "2023-02-01",
                                          "tilOgMed": "2023-02-28"
                                        },
                                        "kilde": "ARENA",
                                        "barnetillegg": null,
                                        "sats": null,
                                        "satsBarnetillegg": null,
                                        "vedtaksperiode": {
                                          "fraOgMed": "2023-02-01",
                                          "tilOgMed": "2023-02-28"
                                        },
                                        "innvilgelsesperioder": [],
                                        "omgjortAvRammevedtakId": null,
                                        "omgjorRammevedtakId": null,
                                        "vedtakstidspunkt": null
                                      },
                                      {
                                        "vedtakId": "tp-med-barnetillegg",
                                        "rettighet": "TILTAKSPENGER_OG_BARNETILLEGG",
                                        "periode": {
                                          "fraOgMed": "2024-01-01",
                                          "tilOgMed": "2024-01-31"
                                        },
                                        "kilde": "TPSAK",
                                        "barnetillegg": {
                                          "perioder": [
                                            {
                                              "antallBarn": 2,
                                              "periode": {
                                                "fraOgMed": "2024-01-01",
                                                "tilOgMed": "2024-01-31"
                                              }
                                            }
                                          ]
                                        },
                                        "sats": ${satser2024.sats},
                                        "satsBarnetillegg": ${satser2024.satsBarnetillegg},
                                        "vedtaksperiode": {
                                          "fraOgMed": "2024-01-01",
                                          "tilOgMed": "2024-01-31"
                                        },
                                        "innvilgelsesperioder": [
                                          {
                                            "fraOgMed": "2024-01-01",
                                            "tilOgMed": "2024-01-31"
                                          }
                                        ],
                                        "omgjortAvRammevedtakId": null,
                                        "omgjorRammevedtakId": null,
                                        "vedtakstidspunkt": "2024-01-01T00:00:00+01:00"
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
                                "ident": "12345678911",
                                "fom": "2023-01-01",
                                "tom": "2024-12-31"
                            }
                            """.trimIndent(),
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `hent vedtaksperioder - har ingen vedtak - riktig respons`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val vedtakRepo = testDataHelper.vedtakRepo
                val arenaClient = mockk<ArenaClient>()

                val vedtakService = HentVedtaksperioderService(vedtakRepo, arenaClient, fixedClock)
                coEvery { arenaClient.hentVedtak(any(), any()) } returns suksessRespons(emptyList())

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
                                    hentTpVedtakService = mockk(relaxed = true),
                                    hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                    hentVedtaksperioderService = vedtakService,
                                    hentSakService = mockk(relaxed = true),
                                    clock = fixedClock,
                                )
                            }
                        }
                    }
                    defaultRequestMedKontraktsverifisering(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("${VEDTAK_PATH}/perioder")
                        },
                        jwt = token,
                        forventet = ForventetRespons(
                            status = HttpStatusCode.OK,
                            body = ForventetBody.Json(
                                """
                                        []
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
    }

    @Test
    fun `hent vedtaksperioder - har avslag - returnerer tom liste`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val sakRepo = testDataHelper.sakRepo
                val vedtakRepo = testDataHelper.vedtakRepo
                val arenaClient = mockk<ArenaClient>()

                val fnr = Fnr.fromString("12345678910")
                val sak = SakMother.sak(fnr = fnr)
                sakRepo.lagre(sak)
                val tpVedtak = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId",
                    sakId = sak.id,
                    virkningsperiode = 1.januar(2024) til 1.mars(2024),
                    rettighet = TiltakspengerVedtak.Rettighet.AVSLAG,
                )
                vedtakRepo.lagre(tpVedtak)
                val vedtakService = HentVedtaksperioderService(vedtakRepo, arenaClient, fixedClock)
                coEvery { arenaClient.hentVedtak(any(), any()) } returns suksessRespons(emptyList())

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
                                    hentTpVedtakService = mockk(relaxed = true),
                                    hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                    hentVedtaksperioderService = vedtakService,
                                    hentSakService = mockk(relaxed = true),
                                    clock = fixedClock,
                                )
                            }
                        }
                    }
                    defaultRequestMedKontraktsverifisering(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("${VEDTAK_PATH}/perioder")
                        },
                        jwt = token,
                        forventet = ForventetRespons(
                            status = HttpStatusCode.OK,
                            body = ForventetBody.Json(
                                """
                                        []
                                """.trimIndent(),
                            ),
                            contentType = ContentType.parse("application/json"),
                        ),
                    ) {
                        setBody(
                            """
                        {
                            "ident": "12345678910",
                            "fom": "2023-01-01",
                            "tom": "2024-12-31"
                        }
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
            withMigratedDb { testDataHelper ->
                val tac = this
                val sakRepo = testDataHelper.sakRepo
                val vedtakRepo = testDataHelper.vedtakRepo
                val arenaClient = mockk<ArenaClient>()

                val fnr = Fnr.fromString("12345678910")
                val sak = SakMother.sak(fnr = fnr)
                sakRepo.lagre(sak)
                val tpVedtak = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId",
                    sakId = sak.id,
                    virkningsperiode = 1.januar(2024) til 1.mars(2024),
                )
                vedtakRepo.lagre(tpVedtak)

                val vedtakService = HentVedtaksperioderService(vedtakRepo, arenaClient, fixedClock)
                coEvery { arenaClient.hentVedtak(any(), any()) } returns suksessRespons(emptyList())

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
                                    hentTpVedtakService = mockk(relaxed = true),
                                    hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                    hentVedtaksperioderService = vedtakService,
                                    hentSakService = mockk(relaxed = true),
                                    clock = fixedClock,
                                )
                            }
                        }
                    }
                    defaultRequestMedKontraktsverifisering(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("${VEDTAK_PATH}/perioder")
                        },
                        jwt = token,
                        forventet = ForventetRespons(
                            status = HttpStatusCode.OK,
                            body = ForventetBody.Json(
                                """
                                        [
                                          {
                                            "vedtakId": "vedtakId",
                                            "rettighet": "TILTAKSPENGER",
                                            "periode": {
                                              "fraOgMed": "2024-01-01",
                                              "tilOgMed": "2024-03-01"
                                            },
                                            "kilde": "TPSAK",
                                            "barnetillegg": null,
                                            "sats": ${satser2024.sats},
                                            "satsBarnetillegg": 0,
                                            "vedtaksperiode": {
                                              "fraOgMed": "2024-01-01",
                                              "tilOgMed": "2024-03-01"
                                            },
                                            "innvilgelsesperioder": [
                                              {
                                                "fraOgMed": "2024-01-01",
                                                "tilOgMed": "2024-03-01"
                                              }
                                            ],
                                            "omgjortAvRammevedtakId": null,
                                            "omgjorRammevedtakId": null,
                                            "vedtakstidspunkt": "2021-01-01T00:00:00+01:00"
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
    }

    @Test
    fun `returnerer også perioder som er stanset eller avslått`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val sakRepo = testDataHelper.sakRepo
                val vedtakRepo = testDataHelper.vedtakRepo
                val arenaClient = mockk<ArenaClient>()

                val fnr = Fnr.fromString("12345678910")
                val sak = SakMother.sak(fnr = fnr)
                sakRepo.lagre(sak)
                val tpVedtak = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId",
                    sakId = sak.id,
                    virkningsperiode = 1.januar(2024) til 1.mars(2024),
                )
                vedtakRepo.lagre(tpVedtak)

                val tpVedtakStanset = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId2",
                    sakId = sak.id,
                    rettighet = TiltakspengerVedtak.Rettighet.STANS,
                    virkningsperiode = 1.februar(2024) til 1.mars(2024),
                )
                vedtakRepo.lagre(tpVedtakStanset)

                val vedtakService = HentVedtaksperioderService(vedtakRepo, arenaClient, fixedClock)
                coEvery { arenaClient.hentVedtak(any(), any()) } returns suksessRespons(emptyList())

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
                                    hentTpVedtakService = mockk(relaxed = true),
                                    hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                    hentVedtaksperioderService = vedtakService,
                                    hentSakService = mockk(relaxed = true),
                                    clock = fixedClock,
                                )
                            }
                        }
                    }
                    defaultRequestMedKontraktsverifisering(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("${VEDTAK_PATH}/perioder")
                        },
                        jwt = token,
                        forventet = ForventetRespons(
                            status = HttpStatusCode.OK,
                            body = ForventetBody.Json(
                                """
                                        [
                                          {
                                            "vedtakId": "vedtakId",
                                            "rettighet": "TILTAKSPENGER",
                                            "periode": {
                                              "fraOgMed": "2024-01-01",
                                              "tilOgMed": "2024-03-01"
                                            },
                                            "kilde": "TPSAK",
                                            "barnetillegg": null,
                                            "sats": ${satser2024.sats},
                                            "satsBarnetillegg": 0,
                                            "vedtaksperiode": {
                                              "fraOgMed": "2024-01-01",
                                              "tilOgMed": "2024-03-01"
                                            },
                                            "innvilgelsesperioder": [
                                              {
                                                "fraOgMed": "2024-01-01",
                                                "tilOgMed": "2024-03-01"
                                              }
                                            ],
                                            "omgjortAvRammevedtakId": null,
                                            "omgjorRammevedtakId": null,
                                            "vedtakstidspunkt": "2021-01-01T00:00:00+01:00"
                                          },
                                          {
                                            "vedtakId": "vedtakId2",
                                            "rettighet": "INGENTING",
                                            "periode": {
                                              "fraOgMed": "2024-02-01",
                                              "tilOgMed": "2024-03-01"
                                            },
                                            "kilde": "TPSAK",
                                            "barnetillegg": null,
                                            "sats": null,
                                            "satsBarnetillegg": null,
                                            "vedtaksperiode": {
                                              "fraOgMed": "2024-02-01",
                                              "tilOgMed": "2024-03-01"
                                            },
                                            "innvilgelsesperioder": [],
                                            "omgjortAvRammevedtakId": null,
                                            "omgjorRammevedtakId": null,
                                            "vedtakstidspunkt": "2021-01-01T00:00:00+01:00"
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
    }

    @Test
    fun `test at uten gyldig ident gir feilmelding`() {
        with(TestApplicationContext()) {
            val tac = this
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            val vedtakService = mockk<HentVedtaksperioderService>(relaxed = true)
            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                hentTpVedtakService = mockk(relaxed = true),
                                hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                hentVedtaksperioderService = vedtakService,
                                hentSakService = mockk(relaxed = true),
                                clock = fixedClock,
                            )
                        }
                    }
                }
                defaultRequestMedKontraktsverifisering(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/perioder")
                    },
                    jwt = token,
                    forventet = ForventetRespons(
                        status = HttpStatusCode.BadRequest,
                        body = ForventetBody.Json(
                            // language=JSON
                            """
                            { "feilmelding" : "Ident  er ugyldig. Må bestå av 11 siffer" }
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

            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            val vedtakService = mockk<HentVedtaksperioderService>(relaxed = true)
            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                hentTpVedtakService = mockk(relaxed = true),
                                hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                hentVedtaksperioderService = vedtakService,
                                hentSakService = mockk(relaxed = true),
                                clock = fixedClock,
                            )
                        }
                    }
                }
                defaultRequestMedKontraktsverifisering(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/perioder")
                    },
                    jwt = token,
                    forventet = ForventetRespons(
                        status = HttpStatusCode.BadRequest,
                        body = ForventetBody.Json(
                            // language=JSON
                            """
                            { "feilmelding" : "Ugyldig datoformat for fom-dato: 202X-01-01" }
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

            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            val vedtakService = mockk<HentVedtaksperioderService>(relaxed = true)
            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                hentTpVedtakService = mockk(relaxed = true),
                                hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                hentVedtaksperioderService = vedtakService,
                                hentSakService = mockk(relaxed = true),
                                clock = fixedClock,
                            )
                        }
                    }
                }
                defaultRequestMedKontraktsverifisering(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/perioder")
                    },
                    jwt = token,
                    forventet = ForventetRespons(
                        status = HttpStatusCode.BadRequest,
                        body = ForventetBody.Json(
                            // language=JSON
                            """
                            { "feilmelding" : "Ugyldig datoformat for tom-dato: 202X-12-31" }
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

            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            val vedtakService = mockk<HentVedtaksperioderService>(relaxed = true)
            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                hentTpVedtakService = mockk(relaxed = true),
                                hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                hentVedtaksperioderService = vedtakService,
                                hentSakService = mockk(relaxed = true),
                                clock = fixedClock,
                            )
                        }
                    }
                }
                defaultRequestMedKontraktsverifisering(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/perioder")
                    },
                    jwt = token,
                    forventet = ForventetRespons(
                        status = HttpStatusCode.BadRequest,
                        body = ForventetBody.Json(
                            // language=JSON
                            """
                            { "feilmelding" : "Fra-dato 2021-01-01 ikke være etter til-dato 2020-12-31" }
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

    @Test
    fun `hent vedtaksperioder - mangler rolle - returnerer 403`() {
        with(TestApplicationContext()) {
            val tac = this
            val systembruker = Systembruker(
                roller = Systembrukerroller(emptyList()),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = emptyList())
            texasClient.leggTilSystembruker(token, systembruker)
            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                hentTpVedtakService = mockk(relaxed = true),
                                hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                hentVedtaksperioderService = mockk(relaxed = true),
                                hentSakService = mockk(relaxed = true),
                                clock = fixedClock,
                            )
                        }
                    }
                }
                defaultRequestMedKontraktsverifisering(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/perioder")
                    },
                    jwt = token,
                    forventet = ForventetRespons(
                        status = HttpStatusCode.Forbidden,
                    ),
                ) {
                    setBody(
                        """
                        {
                            "ident": "01234567891",
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
    fun `hent vedtaksperioder - feil fra arena - returnerer 500 server_feil`() {
        with(TestApplicationContext()) {
            val tac = this
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            val arenaClient = mockk<ArenaClient>()
            coEvery { arenaClient.hentVedtak(any(), any()) } returns uventetStatusFeil()
            val vedtakService = HentVedtaksperioderService(mockk(relaxed = true), arenaClient, fixedClock)
            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                hentTpVedtakService = mockk(relaxed = true),
                                hentTidslinjeOgAlleVedtakService = mockk(relaxed = true),
                                hentVedtaksperioderService = vedtakService,
                                hentSakService = mockk(relaxed = true),
                                clock = fixedClock,
                            )
                        }
                    }
                }
                // 500 er ikke deklarert i openapi-kontrakten, så responsen kontraktsverifiseres ikke.
                defaultRequestWithAssertions(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/perioder")
                    },
                    jwt = token,
                    forventet = ForventetRespons(
                        status = HttpStatusCode.InternalServerError,
                        body = ForventetBody.Json(
                            // language=JSON
                            """
                            { "melding": "Noe gikk galt på serversiden", "kode": "server_feil" }
                            """.trimIndent(),
                        ),
                        contentType = ContentType.parse("application/json; charset=UTF-8"),
                    ),
                ) {
                    setBody(
                        """
                        {
                            "ident": "01234567891",
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
    fun `hent vedtaksperioder - uten token - 401 uten body`() {
        with(TestApplicationContext()) {
            testApplication {
                configureTestApplication(texasClient = texasClient)
                defaultRequestMedKontraktsverifisering(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/perioder")
                    },
                    jwt = null,
                    forventet = ForventetRespons(
                        status = HttpStatusCode.Unauthorized,
                        body = ForventetBody.Tom,
                    ),
                ) {
                    setBody("""{"ident": "12345678910"}""")
                }
            }
        }
    }

    @Test
    fun `hent vedtaksperioder - ukjent token - 401 uten body`() {
        with(TestApplicationContext()) {
            testApplication {
                configureTestApplication(texasClient = texasClient)
                defaultRequestMedKontraktsverifisering(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/perioder")
                    },
                    jwt = "token-texas-ikke-kjenner",
                    forventet = ForventetRespons(
                        status = HttpStatusCode.Unauthorized,
                        body = ForventetBody.Tom,
                    ),
                ) {
                    setBody("""{"ident": "12345678910"}""")
                }
            }
        }
    }

    @Test
    fun `hent vedtaksperioder - brukertoken som ikke er systembruker - 403 ikke_systembruker`() {
        with(TestApplicationContext()) {
            val token = jwtGenerator.createJwtForSaksbehandler()
            texasClient.leggTilBrukertoken(token)
            testApplication {
                configureTestApplication(texasClient = texasClient)
                defaultRequestMedKontraktsverifisering(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/perioder")
                    },
                    jwt = token,
                    forventet = ForventetRespons(
                        status = HttpStatusCode.Forbidden,
                        body = ForventetBody.Json(
                            // language=JSON
                            """
                            { "melding": "Brukeren er ikke en systembruker", "kode": "ikke_systembruker" }
                            """.trimIndent(),
                        ),
                        contentType = ContentType.parse("application/json; charset=UTF-8"),
                    ),
                ) {
                    setBody("""{"ident": "12345678910"}""")
                }
            }
        }
    }

    @Test
    fun `hent vedtaksperioder - ukjent felt i request ignoreres - 200`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val arenaClient = mockk<ArenaClient>()
                coEvery { arenaClient.hentVedtak(any(), any()) } returns suksessRespons(emptyList())
                val vedtakService = HentVedtaksperioderService(testDataHelper.vedtakRepo, arenaClient, fixedClock)
                val token = systembrukerTokenMedLesVedtak()
                testApplication {
                    configureTestApplication(
                        hentVedtaksperioderService = vedtakService,
                        texasClient = texasClient,
                    )
                    defaultRequestMedKontraktsverifisering(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("${VEDTAK_PATH}/perioder")
                        },
                        jwt = token,
                        forventet = ForventetRespons(
                            status = HttpStatusCode.OK,
                            body = ForventetBody.Json("[]"),
                            contentType = ContentType.parse("application/json"),
                        ),
                    ) {
                        setBody("""{"ident": "12345678910", "ukjentFelt": "ignoreres i dag"}""")
                    }
                }
            }
        }
    }

    @Test
    fun `hent vedtaksperioder - manglende ident-felt - 500 med dagens oppførsel`() {
        assertUgyldigRequestBodyGirServerFeil("""{"fom": "2024-01-01", "tom": "2024-12-31"}""")
    }

    @Test
    fun `hent vedtaksperioder - ident med feil datatype - 500 med dagens oppførsel`() {
        assertUgyldigRequestBodyGirServerFeil("""{"ident": {"objekt": true}}""")
    }

    @Test
    fun `hent vedtaksperioder - ugyldig JSON-syntaks - 500 med dagens oppførsel`() {
        assertUgyldigRequestBodyGirServerFeil("""{"ident": """)
    }

    @Test
    fun `hent vedtaksperioder - tom body - 500 med dagens oppførsel`() {
        assertUgyldigRequestBodyGirServerFeil("")
    }

    /**
     * Dagens oppførsel: deserialiseringsfeil treffer [no.nav.tiltakspenger.datadeling.infra.exception.ExceptionHandler] og gir 500.
     * Skal flippes til 400 med maskinlesbar kode i siste steg av feilmeldingsarbeidet.
     * 500 er ikke deklarert i openapi-kontrakten, så responsen kontraktsverifiseres ikke.
     */
    private fun assertUgyldigRequestBodyGirServerFeil(requestBody: String) {
        with(TestApplicationContext()) {
            val token = systembrukerTokenMedLesVedtak()
            testApplication {
                configureTestApplication(texasClient = texasClient)
                defaultRequestWithAssertions(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/perioder")
                    },
                    jwt = token,
                    forventet = ForventetRespons(
                        status = HttpStatusCode.InternalServerError,
                        body = ForventetBody.Json(
                            // language=JSON
                            """
                            { "melding": "Noe gikk galt på serversiden", "kode": "server_feil" }
                            """.trimIndent(),
                        ),
                        contentType = ContentType.parse("application/json; charset=UTF-8"),
                    ),
                ) {
                    setBody(requestBody)
                }
            }
        }
    }

    /** Registrerer en systembruker med LES_VEDTAK i Texas-faken og returnerer tokenet. */
    private fun TestApplicationContext.systembrukerTokenMedLesVedtak(): String {
        val systembruker = Systembruker(
            roller = Systembrukerroller(listOf(Systembrukerrolle.LES_VEDTAK)),
            klientnavn = "klientnavn",
            klientId = "id",
        )
        return jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak")).also {
            texasClient.leggTilSystembruker(it, systembruker)
        }
    }
}
