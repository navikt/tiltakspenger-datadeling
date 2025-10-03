package no.nav.tiltakspenger.datadeling.routes.vedtak

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
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.domene.Barnetillegg
import no.nav.tiltakspenger.datadeling.domene.BarnetilleggPeriode
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.Systembrukerroller
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.service.VedtakService
import no.nav.tiltakspenger.datadeling.testdata.VedtakMother
import no.nav.tiltakspenger.datadeling.testutils.TestApplicationContext
import no.nav.tiltakspenger.datadeling.testutils.configureTestApplication
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.satser.Satser.Companion.sats
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakRoutesHentTidslinjeOgVedtakTest {
    private val satser2024 = sats(1.januar(2024))
    private val arenaClient = mockk<ArenaClient>()

    @Test
    fun `hent tidslinje og vedtak - har to innvilgelser, stans og avslag - riktig respons`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val vedtakRepo = testDataHelper.vedtakRepo
                val tpVedtak = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId",
                    fnr = Fnr.fromString("12345678910"),
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 3, 1),
                    opprettetTidspunkt = LocalDate.of(2024, 1, 1).atStartOfDay(),
                )
                vedtakRepo.lagre(tpVedtak)
                val tpVedtakStanset = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId2",
                    fnr = Fnr.fromString("12345678910"),
                    rettighet = TiltakspengerVedtak.Rettighet.STANS,
                    fom = LocalDate.of(2024, 2, 1),
                    tom = LocalDate.of(2024, 3, 1),
                    valgteHjemlerHarIkkeRettighet = listOf(TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK),
                    opprettetTidspunkt = LocalDate.of(2024, 2, 1).atStartOfDay(),
                )
                vedtakRepo.lagre(tpVedtakStanset)
                val tpVedtakAvslag = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId3",
                    fnr = Fnr.fromString("12345678910"),
                    rettighet = TiltakspengerVedtak.Rettighet.AVSLAG,
                    fom = LocalDate.of(2024, 4, 1),
                    tom = LocalDate.of(2024, 5, 1),
                    valgteHjemlerHarIkkeRettighet = listOf(TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.INSTITUSJONSOPPHOLD),
                    opprettetTidspunkt = LocalDate.of(2024, 4, 1).atStartOfDay(),
                )
                vedtakRepo.lagre(tpVedtakAvslag)
                val tpVedtakMedBarnetillegg = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId4",
                    fnr = Fnr.fromString("12345678910"),
                    rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
                    fom = LocalDate.of(2024, 6, 1),
                    tom = LocalDate.of(2024, 8, 1),
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
                            path("$VEDTAK_PATH/tidslinje")
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
                                              "vedtakId": "vedtakId",
                                              "sakId": "sakId",
                                              "saksnummer": "saksnummer",
                                              "rettighet": "TILTAKSPENGER",
                                              "periode": {
                                                "fraOgMed": "2024-01-01",
                                                "tilOgMed": "2024-01-31"
                                              },
                                              "barnetillegg": null,
                                              "vedtaksdato": "2024-01-01",
                                              "valgteHjemlerHarIkkeRettighet": null,
                                              "sats": ${satser2024.sats},
                                              "satsBarnetillegg": 0
                                            },
                                            {
                                              "vedtakId": "vedtakId2",
                                              "sakId": "sakId",
                                              "saksnummer": "saksnummer",
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
                                              "satsBarnetillegg": null
                                            },
                                            {
                                              "vedtakId": "vedtakId4",
                                              "sakId": "sakId",
                                              "saksnummer": "saksnummer",
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
                                              "satsBarnetillegg": ${satser2024.satsBarnetillegg}
                                            }
                                          ],
                                          "alleVedtak": [
                                            {
                                              "vedtakId": "vedtakId",
                                              "sakId": "sakId",
                                              "saksnummer": "saksnummer",
                                              "rettighet": "TILTAKSPENGER",
                                              "periode": {
                                                "fraOgMed": "2024-01-01",
                                                "tilOgMed": "2024-03-01"
                                              },
                                              "barnetillegg": null,
                                              "vedtaksdato": "2024-01-01",
                                              "valgteHjemlerHarIkkeRettighet": null,
                                              "sats": ${satser2024.sats},
                                              "satsBarnetillegg": 0
                                            },
                                            {
                                              "vedtakId": "vedtakId2",
                                              "sakId": "sakId",
                                              "saksnummer": "saksnummer",
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
                                              "satsBarnetillegg": null
                                            },
                                            {
                                              "vedtakId": "vedtakId3",
                                              "sakId": "sakId",
                                              "saksnummer": "saksnummer",
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
                                              "satsBarnetillegg": null
                                            },
                                            {
                                              "vedtakId": "vedtakId4",
                                              "sakId": "sakId",
                                              "saksnummer": "saksnummer",
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
                                              "satsBarnetillegg": ${satser2024.satsBarnetillegg}
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
    }

    @Test
    fun `hent tidslinje og vedtak - har ingen vedtak - riktig respons`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val vedtakRepo = testDataHelper.vedtakRepo
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
                            path("$VEDTAK_PATH/tidslinje")
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
                                          "alleVedtak": []
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
                val vedtakRepo = testDataHelper.vedtakRepo
                val tpVedtak = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId",
                    fnr = Fnr.fromString("12345678910"),
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 3, 1),
                    rettighet = TiltakspengerVedtak.Rettighet.AVSLAG,
                    valgteHjemlerHarIkkeRettighet = listOf(TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.FREMMET_FOR_SENT),
                )
                vedtakRepo.lagre(tpVedtak)
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
                            path("$VEDTAK_PATH/tidslinje")
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
                                              "sakId": "sakId",
                                              "saksnummer": "saksnummer",
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
                                              "satsBarnetillegg": null
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
    }

    @Test
    fun `test at vi kan hente uten å oppgi dato`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val vedtakRepo = testDataHelper.vedtakRepo
                val tpVedtak = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId",
                    fnr = Fnr.fromString("12345678910"),
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 3, 1),
                )
                vedtakRepo.lagre(tpVedtak)
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
                            path("$VEDTAK_PATH/tidslinje")
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
                                              "sakId": "sakId",
                                              "saksnummer": "saksnummer",
                                              "rettighet": "TILTAKSPENGER",
                                              "periode": {
                                                "fraOgMed": "2024-01-01",
                                                "tilOgMed": "2024-03-01"
                                              },
                                              "barnetillegg": null,
                                              "vedtaksdato": "2021-01-01",
                                              "valgteHjemlerHarIkkeRettighet": null,
                                              "sats": ${satser2024.sats},
                                              "satsBarnetillegg": 0
                                            }
                                          ],
                                          "alleVedtak": [
                                            {
                                              "vedtakId": "vedtakId",
                                              "sakId": "sakId",
                                              "saksnummer": "saksnummer",
                                              "rettighet": "TILTAKSPENGER",
                                              "periode": {
                                                "fraOgMed": "2024-01-01",
                                                "tilOgMed": "2024-03-01"
                                              },
                                              "barnetillegg": null,
                                              "vedtaksdato": "2021-01-01",
                                              "valgteHjemlerHarIkkeRettighet": null,
                                              "sats": ${satser2024.sats},
                                              "satsBarnetillegg": 0
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
                        path("$VEDTAK_PATH/tidslinje")
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
                        path("$VEDTAK_PATH/tidslinje")
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
                        path("$VEDTAK_PATH/tidslinje")
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
                        path("$VEDTAK_PATH/tidslinje")
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
                assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }
    }

    private fun TestApplicationContext.getGyldigToken(): String {
        val systembruker = Systembruker(
            roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_VEDTAK)),
            klientnavn = "klientnavn",
            klientId = "id",
        )
        val token = this.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
        texasClient.leggTilSystembruker(token, systembruker)
        return token
    }
}
