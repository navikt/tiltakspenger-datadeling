package no.nav.tiltakspenger.datadeling.routes.vedtak

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
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.Systembrukerroller
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.datadeling.felles.VedtakMother
import no.nav.tiltakspenger.datadeling.felles.withMigratedDb
import no.nav.tiltakspenger.datadeling.jacksonSerialization
import no.nav.tiltakspenger.datadeling.routes.TestApplicationContext
import no.nav.tiltakspenger.datadeling.service.VedtakService
import no.nav.tiltakspenger.datadeling.setupAuthentication
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.satser.Satser.Companion.sats
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import org.junit.jupiter.api.Test
import java.time.LocalDate

class VedtakRoutesHentPerioderTest {
    private val satser2024 = sats(1.januar(2024))

    @Test
    fun `hent vedtaksperioder - har vedtak fra arena og tpsak - riktig respons`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this

                val vedtakRepo = testDataHelper.vedtakRepo
                val arenaClient = mockk<ArenaClient>()

                val tpVedtak = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId",
                    fnr = Fnr.fromString("12345678910"),
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 3, 1),
                )
                vedtakRepo.lagre(tpVedtak)

                val arenaVedtak = Vedtak(
                    periode = Periode(tpVedtak.periode.fraOgMed.minusMonths(6), tpVedtak.periode.fraOgMed.minusMonths(2)),
                    rettighet = Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
                    vedtakId = "id",
                    sakId = tpVedtak.sakId,
                    saksnummer = tpVedtak.saksnummer,
                    kilde = Kilde.ARENA,
                    fnr = tpVedtak.fnr,
                    antallBarn = 1,
                    dagsatsTiltakspenger = 285,
                    dagsatsBarnetillegg = 53,
                )
                val vedtakService = VedtakService(vedtakRepo, arenaClient)
                coEvery { arenaClient.hentVedtak(any(), any()) } returns listOf(arenaVedtak)

                val systembruker = Systembruker(
                    roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_VEDTAK)),
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
                                    vedtakService = vedtakService,
                                )
                            }
                        }
                    }
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("$VEDTAK_PATH/perioder")
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
                                            "satsBarnetillegg": 53
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
                                            "satsBarnetillegg": 0
                                          }
                                        ]
                                    """.trimIndent(),
                                )
                            }
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

                val vedtakService = VedtakService(vedtakRepo, arenaClient)
                coEvery { arenaClient.hentVedtak(any(), any()) } returns emptyList()

                val systembruker = Systembruker(
                    roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_VEDTAK)),
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
                                    vedtakService = vedtakService,
                                )
                            }
                        }
                    }
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("$VEDTAK_PATH/perioder")
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
                                        []
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
                val arenaClient = mockk<ArenaClient>()

                val tpVedtak = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId",
                    fnr = Fnr.fromString("12345678910"),
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 3, 1),
                )
                vedtakRepo.lagre(tpVedtak)

                val vedtakService = VedtakService(vedtakRepo, arenaClient)
                coEvery { arenaClient.hentVedtak(any(), any()) } returns emptyList()

                val systembruker = Systembruker(
                    roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_VEDTAK)),
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
                                    vedtakService = vedtakService,
                                )
                            }
                        }
                    }
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("$VEDTAK_PATH/perioder")
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
                                            "satsBarnetillegg": 0
                                          }
                                        ]
                                    """.trimIndent(),
                                )
                            }
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

                val vedtakRepo = testDataHelper.vedtakRepo
                val arenaClient = mockk<ArenaClient>()

                val tpVedtak = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId",
                    fnr = Fnr.fromString("12345678910"),
                    fom = LocalDate.of(2024, 1, 1),
                    tom = LocalDate.of(2024, 3, 1),
                )
                vedtakRepo.lagre(tpVedtak)

                val tpVedtakStanset = VedtakMother.tiltakspengerVedtak(
                    vedtakId = "vedtakId2",
                    fnr = Fnr.fromString("12345678910"),
                    rettighet = TiltakspengerVedtak.Rettighet.INGENTING,
                    fom = LocalDate.of(2024, 2, 1),
                    tom = LocalDate.of(2024, 3, 1),
                )
                vedtakRepo.lagre(tpVedtakStanset)

                val vedtakService = VedtakService(vedtakRepo, arenaClient)
                coEvery { arenaClient.hentVedtak(any(), any()) } returns emptyList()

                val systembruker = Systembruker(
                    roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_VEDTAK)),
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
                                    vedtakService = vedtakService,
                                )
                            }
                        }
                    }
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("$VEDTAK_PATH/perioder")
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
                                            "satsBarnetillegg": 0
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
                                            "satsBarnetillegg": null
                                          }
                                        ]
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
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            val vedtakService = mockk<VedtakService>(relaxed = true)
            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                vedtakService = vedtakService,
                            )
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$VEDTAK_PATH/perioder")
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

            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            val vedtakService = mockk<VedtakService>(relaxed = true)
            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                vedtakService = vedtakService,
                            )
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$VEDTAK_PATH/perioder")
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

            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            val vedtakService = mockk<VedtakService>(relaxed = true)
            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                vedtakService = vedtakService,
                            )
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$VEDTAK_PATH/perioder")
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

            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_VEDTAK)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak"))
            texasClient.leggTilSystembruker(token, systembruker)

            val vedtakService = mockk<VedtakService>(relaxed = true)
            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            vedtakRoutes(
                                vedtakService = vedtakService,
                            )
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("$VEDTAK_PATH/perioder")
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
}
