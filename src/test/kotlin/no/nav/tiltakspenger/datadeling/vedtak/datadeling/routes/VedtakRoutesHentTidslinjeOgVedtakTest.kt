package no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.header
import io.ktor.client.request.post
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
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.client.arena.domene.ArenaVedtak
import no.nav.tiltakspenger.datadeling.client.arena.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.Systembrukerroller
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.datadeling.testutils.TestApplicationContext
import no.nav.tiltakspenger.datadeling.testutils.configureTestApplication
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.VedtakService
import no.nav.tiltakspenger.datadeling.vedtak.domene.Barnetillegg
import no.nav.tiltakspenger.datadeling.vedtak.domene.BarnetilleggPeriode
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.dato.april
import no.nav.tiltakspenger.libs.dato.august
import no.nav.tiltakspenger.libs.dato.februar
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.juni
import no.nav.tiltakspenger.libs.dato.mai
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.til
import no.nav.tiltakspenger.libs.satser.Satser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class VedtakRoutesHentTidslinjeOgVedtakTest {
    private val satser2024 = Satser.sats(1.januar(2024))
    private val arenaClient = mockk<ArenaClient>()

    @BeforeEach
    fun setup() {
        clearMocks(arenaClient)
    }

    @Test
    fun `hent tidslinje og vedtak - har to innvilgelser, stans og avslag, et arenavedtak - riktig respons`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val sakRepo = testDataHelper.sakRepo
                val vedtakRepo = testDataHelper.vedtakRepo
                val fnr = Fnr.fromString("12345678910")
                val sak = SakMother.sak(
                    fnr = fnr,
                    opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                )
                sakRepo.lagre(sak)
                val tpVedtak = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId",
                    sakId = sak.id,
                    virkningsperiode = 1.januar(2024) til 1.mars(2024),
                    opprettetTidspunkt = LocalDate.of(2024, 1, 1).atStartOfDay(),
                    omgjortAvRammevedtakId = "vedtakId2",
                )
                vedtakRepo.lagre(tpVedtak)
                val tpVedtakOmgjøring = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId2",
                    sakId = sak.id,
                    virkningsperiode = 1.januar(2024) til 1.mars(2024),
                    innvilgelsesperiode = 3.januar(2024) til 1.mars(2024),
                    opprettetTidspunkt = LocalDate.of(2024, 1, 3).atStartOfDay(),
                    omgjørRammevedtakId = "vedtakId",
                )
                vedtakRepo.lagre(tpVedtakOmgjøring)
                val tpVedtakStanset = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId3",
                    sakId = sak.id,
                    rettighet = TiltakspengerVedtak.Rettighet.STANS,
                    virkningsperiode = 1.februar(2024) til 1.mars(2024),
                    valgteHjemlerHarIkkeRettighet = listOf(TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK),
                    opprettetTidspunkt = LocalDate.of(2024, 2, 1).atStartOfDay(),
                )
                vedtakRepo.lagre(tpVedtakStanset)
                val tpVedtakAvslag = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId4",
                    sakId = sak.id,
                    rettighet = TiltakspengerVedtak.Rettighet.AVSLAG,
                    virkningsperiode = 1.april(2024) til 1.mai(2024),
                    valgteHjemlerHarIkkeRettighet = listOf(TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.INSTITUSJONSOPPHOLD),
                    opprettetTidspunkt = LocalDate.of(2024, 4, 1).atStartOfDay(),
                )
                vedtakRepo.lagre(tpVedtakAvslag)
                val tpVedtakMedBarnetillegg = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId5",
                    sakId = sak.id,
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
                    virkningsperiode = 1.juni(2024) til 1.august(2024),
                    barnetillegg = Barnetillegg(
                        perioder = listOf(
                            BarnetilleggPeriode(
                                antallBarn = 2,
                                periode = Periode(
                                    fraOgMed = LocalDate.of(2024, 6, 1),
                                    tilOgMed = LocalDate.of(2024, 8, 1),
                                ),
                            ),
                        ),
                    ),
                    opprettetTidspunkt = LocalDate.of(2024, 6, 1).atStartOfDay(),
                )
                vedtakRepo.lagre(tpVedtakMedBarnetillegg)
                val arenaVedtak = ArenaVedtak(
                    periode = Periode(
                        tpVedtak.periode.fraOgMed.minusMonths(6),
                        tpVedtak.periode.fraOgMed.minusMonths(2),
                    ),
                    rettighet = Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
                    vedtakId = "id",
                    kilde = Kilde.ARENA,
                    fnr = fnr,
                    antallBarn = 1,
                    dagsatsTiltakspenger = 285,
                    dagsatsBarnetillegg = 53,
                    beslutningsdato = tpVedtak.periode.fraOgMed.minusMonths(5),
                    sak = ArenaVedtak.Sak(
                        sakId = tpVedtak.sakId,
                        saksnummer = sak.saksnummer,
                        opprettetDato = tpVedtak.periode.fraOgMed.minusMonths(4),
                        status = "Aktiv",
                    ),
                )
                coEvery { arenaClient.hentVedtak(any(), any()) } returns listOf(arenaVedtak)
                val vedtakService = VedtakService(vedtakRepo, arenaClient)
                val token = getGyldigToken()
                testApplication {
                    configureTestApplication(
                        vedtakService = vedtakService,
                        texasClient = tac.texasClient,
                    )
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("${VEDTAK_PATH}/tidslinje")
                        },
                        jwt = token,
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
                                    """
                                    {
                                      "tidslinje": [
                                        {
                                          "vedtakId": "vedtakId5",
                                          "rettighet": "TILTAKSPENGER_OG_BARNETILLEGG",
                                          "periode": {
                                            "fraOgMed": "2024-06-01",
                                            "tilOgMed": "2024-08-01"
                                          },
                                          "barnetillegg": {
                                            "perioder": [
                                              {
                                                "antallBarn": 2,
                                                "periode": {
                                                  "fraOgMed": "2024-06-01",
                                                  "tilOgMed": "2024-08-01"
                                                }
                                              }
                                            ]
                                          },
                                          "vedtaksdato": "2024-06-01",
                                          "valgteHjemlerHarIkkeRettighet": null,
                                          "sats": ${satser2024.sats},
                                          "satsBarnetillegg": ${satser2024.satsBarnetillegg},
                                          "vedtaksperiode": {
                                            "fraOgMed": "2024-06-01",
                                            "tilOgMed": "2024-08-01"
                                          },
                                          "innvilgelsesperioder": [
                                            {
                                              "fraOgMed": "2024-06-01",
                                              "tilOgMed": "2024-08-01"
                                            }
                                          ],
                                          "omgjortAvRammevedtakId": null,
                                          "omgjorRammevedtakId": null
                                        },
                                        {
                                          "vedtakId": "vedtakId3",
                                          "rettighet": "STANS",
                                          "periode": {
                                            "fraOgMed": "2024-02-01",
                                            "tilOgMed": "2024-03-01"
                                          },
                                          "barnetillegg": null,
                                          "vedtaksdato": "2024-02-01",
                                          "valgteHjemlerHarIkkeRettighet": [
                                            "DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK"
                                          ],
                                          "sats": null,
                                          "satsBarnetillegg": null,
                                          "vedtaksperiode": {
                                            "fraOgMed": "2024-02-01",
                                            "tilOgMed": "2024-03-01"
                                          },
                                          "innvilgelsesperioder": [],
                                          "omgjortAvRammevedtakId": null,
                                          "omgjorRammevedtakId": null
                                        },
                                        {
                                          "vedtakId": "vedtakId2",
                                          "rettighet": "TILTAKSPENGER",
                                          "periode": {
                                            "fraOgMed": "2024-01-03",
                                            "tilOgMed": "2024-01-31"
                                          },
                                          "barnetillegg": null,
                                          "vedtaksdato": "2024-01-03",
                                          "valgteHjemlerHarIkkeRettighet": null,
                                          "sats": ${satser2024.sats},
                                          "satsBarnetillegg": 0,
                                          "vedtaksperiode": {
                                            "fraOgMed": "2024-01-01",
                                            "tilOgMed": "2024-01-31"
                                          },
                                          "innvilgelsesperioder": [
                                            {
                                              "fraOgMed": "2024-01-03",
                                              "tilOgMed": "2024-01-31"
                                            }
                                          ],
                                          "omgjortAvRammevedtakId": null,
                                          "omgjorRammevedtakId": "vedtakId"
                                        }
                                      ],
                                      "alleVedtak": [
                                        {
                                          "vedtakId": "vedtakId5",
                                          "rettighet": "TILTAKSPENGER_OG_BARNETILLEGG",
                                          "periode": {
                                            "fraOgMed": "2024-06-01",
                                            "tilOgMed": "2024-08-01"
                                          },
                                          "barnetillegg": {
                                            "perioder": [
                                              {
                                                "antallBarn": 2,
                                                "periode": {
                                                  "fraOgMed": "2024-06-01",
                                                  "tilOgMed": "2024-08-01"
                                                }
                                              }
                                            ]
                                          },
                                          "vedtaksdato": "2024-06-01",
                                          "valgteHjemlerHarIkkeRettighet": null,
                                          "sats": ${satser2024.sats},
                                          "satsBarnetillegg": ${satser2024.satsBarnetillegg},
                                          "vedtaksperiode": {
                                            "fraOgMed": "2024-06-01",
                                            "tilOgMed": "2024-08-01"
                                          },
                                          "innvilgelsesperioder": [
                                            {
                                              "fraOgMed": "2024-06-01",
                                              "tilOgMed": "2024-08-01"
                                            }
                                          ],
                                          "omgjortAvRammevedtakId": null,
                                          "omgjorRammevedtakId": null
                                        },
                                        {
                                          "vedtakId": "vedtakId4",
                                          "rettighet": "AVSLAG",
                                          "periode": {
                                            "fraOgMed": "2024-04-01",
                                            "tilOgMed": "2024-05-01"
                                          },
                                          "barnetillegg": null,
                                          "vedtaksdato": "2024-04-01",
                                          "valgteHjemlerHarIkkeRettighet": [
                                            "INSTITUSJONSOPPHOLD"
                                          ],
                                          "sats": null,
                                          "satsBarnetillegg": null,
                                          "vedtaksperiode": {
                                            "fraOgMed": "2024-04-01",
                                            "tilOgMed": "2024-05-01"
                                          },
                                          "innvilgelsesperioder": [],
                                          "omgjortAvRammevedtakId": null,
                                          "omgjorRammevedtakId": null
                                        },
                                        {
                                          "vedtakId": "vedtakId3",
                                          "rettighet": "STANS",
                                          "periode": {
                                            "fraOgMed": "2024-02-01",
                                            "tilOgMed": "2024-03-01"
                                          },
                                          "barnetillegg": null,
                                          "vedtaksdato": "2024-02-01",
                                          "valgteHjemlerHarIkkeRettighet": [
                                            "DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK"
                                          ],
                                          "sats": null,
                                          "satsBarnetillegg": null,
                                          "vedtaksperiode": {
                                            "fraOgMed": "2024-02-01",
                                            "tilOgMed": "2024-03-01"
                                          },
                                          "innvilgelsesperioder": [],
                                          "omgjortAvRammevedtakId": null,
                                          "omgjorRammevedtakId": null
                                        },
                                        {
                                          "vedtakId": "vedtakId2",
                                          "rettighet": "TILTAKSPENGER",
                                          "periode": {
                                            "fraOgMed": "2024-01-03",
                                            "tilOgMed": "2024-03-01"
                                          },
                                          "barnetillegg": null,
                                          "vedtaksdato": "2024-01-03",
                                          "valgteHjemlerHarIkkeRettighet": null,
                                          "sats": ${satser2024.sats},
                                          "satsBarnetillegg": 0,
                                          "vedtaksperiode": {
                                            "fraOgMed": "2024-01-01",
                                            "tilOgMed": "2024-03-01"
                                          },
                                          "innvilgelsesperioder": [
                                            {
                                              "fraOgMed": "2024-01-03",
                                              "tilOgMed": "2024-03-01"
                                            }
                                          ],
                                          "omgjortAvRammevedtakId": null,
                                          "omgjorRammevedtakId": "vedtakId"
                                        },
                                        {
                                          "vedtakId": "vedtakId",
                                          "rettighet": "TILTAKSPENGER",
                                          "periode": {
                                            "fraOgMed": "2024-01-01",
                                            "tilOgMed": "2024-03-01"
                                          },
                                          "barnetillegg": null,
                                          "vedtaksdato": "2024-01-01",
                                          "valgteHjemlerHarIkkeRettighet": null,
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
                                          "omgjortAvRammevedtakId": "vedtakId2",
                                          "omgjorRammevedtakId": null
                                        }
                                      ],
                                      "vedtakFraArena": [
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
                                        }
                                      ],
                                      "sak": {
                                        "sakId": "sakId",
                                        "saksnummer": "saksnummer",
                                        "kilde": "TPSAK",
                                        "status": "Løpende",
                                        "opprettetDato": "2020-01-01T00:00:00"
                                      }
                                    }
                                    """.trimIndent(),
                                )
                            }
                        }
                }
            }
        }
    }

    @Test
    fun `hent tidslinje og vedtak - har ingen vedtak - riktig respons`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val vedtakRepo = testDataHelper.vedtakRepo
                coEvery { arenaClient.hentVedtak(any(), any()) } returns emptyList()
                val vedtakService = VedtakService(vedtakRepo, arenaClient)
                val token = getGyldigToken()
                testApplication {
                    configureTestApplication(
                        vedtakService = vedtakService,
                        texasClient = tac.texasClient,
                    )
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("${VEDTAK_PATH}/tidslinje")
                        },
                        jwt = token,
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
                                    """
                                        {
                                          "tidslinje": [],
                                          "alleVedtak": [],
                                          "vedtakFraArena": [],
                                          "sak": null
                                        }
                                    """.trimIndent(),
                                )
                            }
                        }
                }
            }
        }
    }

    @Test
    fun `hent tidslinje og vedtak - har kun avslag - returnerer tom tidslinje og avslaget`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val sakRepo = testDataHelper.sakRepo
                val vedtakRepo = testDataHelper.vedtakRepo
                val fnr = Fnr.fromString("12345678910")
                val sak = SakMother.sak(
                    fnr = fnr,
                    opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                )
                sakRepo.lagre(sak)
                val tpVedtak = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId",
                    sakId = sak.id,
                    virkningsperiode = 1.januar(2024) til 1.mars(2024),
                    rettighet = TiltakspengerVedtak.Rettighet.AVSLAG,
                    valgteHjemlerHarIkkeRettighet = listOf(TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.FREMMET_FOR_SENT),
                )
                vedtakRepo.lagre(tpVedtak)
                coEvery { arenaClient.hentVedtak(any(), any()) } returns emptyList()
                val vedtakService = VedtakService(vedtakRepo, arenaClient)
                val token = getGyldigToken()
                testApplication {
                    configureTestApplication(
                        vedtakService = vedtakService,
                        texasClient = tac.texasClient,
                    )
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("${VEDTAK_PATH}/tidslinje")
                        },
                        jwt = token,
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
                                    """
                                        {
                                          "tidslinje": [],
                                          "alleVedtak": [
                                            {
                                              "vedtakId": "vedtakId",
                                              "rettighet": "AVSLAG",
                                              "periode": {
                                                "fraOgMed": "2024-01-01",
                                                "tilOgMed": "2024-03-01"
                                              },
                                              "barnetillegg": null,
                                              "vedtaksdato": "2021-01-01",
                                              "valgteHjemlerHarIkkeRettighet": [
                                                "FREMMET_FOR_SENT"
                                              ],
                                              "sats": null,
                                              "satsBarnetillegg": null,
                                              "vedtaksperiode": {
                                                "fraOgMed": "2024-01-01",
                                                "tilOgMed": "2024-03-01"
                                              },
                                              "innvilgelsesperioder": [],
                                              "omgjortAvRammevedtakId": null,
                                              "omgjorRammevedtakId": null
                                            }
                                          ],
                                          "vedtakFraArena": [],
                                          "sak": {
                                            "sakId": "sakId",
                                            "saksnummer": "saksnummer",
                                            "kilde": "TPSAK",
                                            "status": "Løpende",
                                            "opprettetDato": "2020-01-01T00:00:00"
                                          }
                                        }
                                    """.trimIndent(),
                                )
                            }
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
                val fnr = Fnr.fromString("12345678910")
                val sak = SakMother.sak(
                    fnr = fnr,
                    opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                )
                sakRepo.lagre(sak)
                val tpVedtak = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId",
                    sakId = sak.id,
                    virkningsperiode = 1.januar(2024) til 1.mars(2024),
                )
                vedtakRepo.lagre(tpVedtak)
                coEvery { arenaClient.hentVedtak(any(), any()) } returns emptyList()
                val vedtakService = VedtakService(vedtakRepo, arenaClient)
                val token = getGyldigToken()
                testApplication {
                    configureTestApplication(
                        vedtakService = vedtakService,
                        texasClient = tac.texasClient,
                    )
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("${VEDTAK_PATH}/tidslinje")
                        },
                        jwt = token,
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
                                    """
                                        {
                                          "tidslinje": [
                                            {
                                              "vedtakId": "vedtakId",
                                              "rettighet": "TILTAKSPENGER",
                                              "periode": {
                                                "fraOgMed": "2024-01-01",
                                                "tilOgMed": "2024-03-01"
                                              },
                                              "barnetillegg": null,
                                              "vedtaksdato": "2021-01-01",
                                              "valgteHjemlerHarIkkeRettighet": null,
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
                                              "omgjorRammevedtakId": null
                                            }
                                          ],
                                          "alleVedtak": [
                                            {
                                              "vedtakId": "vedtakId",
                                              "rettighet": "TILTAKSPENGER",
                                              "periode": {
                                                "fraOgMed": "2024-01-01",
                                                "tilOgMed": "2024-03-01"
                                              },
                                              "barnetillegg": null,
                                              "vedtaksdato": "2021-01-01",
                                              "valgteHjemlerHarIkkeRettighet": null,
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
                                              "omgjorRammevedtakId": null
                                            }
                                          ],
                                          "vedtakFraArena": [],
                                          "sak": {
                                            "sakId": "sakId",
                                            "saksnummer": "saksnummer",
                                            "kilde": "TPSAK",
                                            "status": "Løpende",
                                            "opprettetDato": "2020-01-01T00:00:00"
                                          }
                                        }
                                    """.trimIndent(),
                                )
                            }
                        }
                }
            }
        }
    }

    @Test
    fun `test at uten gyldig ident gir feilmelding`() {
        with(TestApplicationContext()) {
            val tac = this
            val token = getGyldigToken()
            val vedtakService = mockk<VedtakService>(relaxed = true)
            testApplication {
                configureTestApplication(
                    vedtakService = vedtakService,
                    texasClient = tac.texasClient,
                )
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/tidslinje")
                    },
                    jwt = token,
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
                            { "feilmelding" : "Ident  er ugyldig. Må bestå av 11 siffer" }
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
            val token = getGyldigToken()
            val vedtakService = mockk<VedtakService>(relaxed = true)
            testApplication {
                configureTestApplication(
                    vedtakService = vedtakService,
                    texasClient = tac.texasClient,
                )
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/tidslinje")
                    },
                    jwt = token,
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
            val token = getGyldigToken()
            val vedtakService = mockk<VedtakService>(relaxed = true)
            testApplication {
                configureTestApplication(
                    vedtakService = vedtakService,
                    texasClient = tac.texasClient,
                )
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/tidslinje")
                    },
                    jwt = token,
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
    fun `test at fom og tom gir feilmelding når de ikke kommer i riktig rekkefølge`() {
        with(TestApplicationContext()) {
            val tac = this
            val token = getGyldigToken()
            val vedtakService = mockk<VedtakService>(relaxed = true)
            testApplication {
                configureTestApplication(
                    vedtakService = vedtakService,
                    texasClient = tac.texasClient,
                )
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("${VEDTAK_PATH}/tidslinje")
                    },
                    jwt = token,
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

    @Test
    fun `post med ugyldig token skal gi 401`() {
        with(TestApplicationContext()) {
            val tac = this
            testApplication {
                configureTestApplication(texasClient = tac.texasClient)
                val response = client.post("/vedtak/tidslinje") {
                    header("Authorization", "Bearer tulletoken")
                    header("Content-Type", "application/json")
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
                Assertions.assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }
    }

    private fun TestApplicationContext.getGyldigToken(): String {
        val systembruker = Systembruker(
            roller = Systembrukerroller(listOf(Systembrukerrolle.LES_VEDTAK)),
            klientnavn = "klientnavn",
            klientId = "id",
        )
        val token = this.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
        texasClient.leggTilSystembruker(token, systembruker)
        return token
    }
}
