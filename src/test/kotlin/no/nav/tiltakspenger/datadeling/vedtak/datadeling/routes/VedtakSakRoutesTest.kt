package no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes

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
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.client.arena.domene.ArenaClient
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
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.dato.januar
import no.nav.tiltakspenger.libs.dato.mars
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.periode.til
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class VedtakSakRoutesTest {
    private val arenaClient = mockk<ArenaClient>()

    @BeforeEach
    fun setup() {
        clearMocks(arenaClient)
    }

    @Test
    fun `hent sak - har tom sak uten vedtak eller behandlinger i TPSAK - returnerer 404`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val sakRepo = testDataHelper.sakRepo
                val vedtakRepo = testDataHelper.vedtakRepo
                val fnr = Fnr.fromString("12345678910")
                val sak = SakMother.sak(
                    id = "sakId123",
                    saksnummer = "SAK123",
                    fnr = fnr,
                    opprettet = LocalDateTime.parse("2024-01-15T10:30:00"),
                )
                sakRepo.lagre(sak)
                coEvery { arenaClient.hentVedtak(any(), any()) } returns emptyList()
                val vedtakService = VedtakService(vedtakRepo, arenaClient, sakRepo, tac.clock)
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
                            path("$VEDTAK_PATH/sak")
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
                                status shouldBe HttpStatusCode.NotFound
                            }
                        }
                }
            }
        }
    }

    @Test
    fun `hent sak - har sak i TPSAK med iverksatt søknadsbehandling - inkluderer iverksattSoknadsbehandlingTidspunkt`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val sakRepo = testDataHelper.sakRepo
                val vedtakRepo = testDataHelper.vedtakRepo
                val fnr = Fnr.fromString("12345678910")
                val sak = SakMother.sak(
                    id = "sakId123",
                    saksnummer = "SAK123",
                    fnr = fnr,
                    opprettet = LocalDateTime.parse("2024-01-15T10:30:00"),
                )
                sakRepo.lagre(sak)
                // Førstegangs søknadsbehandling iverksatt 2024-02-01.
                vedtakRepo.lagre(
                    VedtakMother.tiltakspengerVedtak(
                        vedtakId = "soknadsvedtak",
                        sakId = sak.id,
                        rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                        virkningsperiode = 1.januar(2024) til 1.mars(2024),
                        opprettetTidspunkt = LocalDateTime.parse("2024-02-01T12:00:00"),
                    ),
                )
                // Senere omgjøring skal IKKE overstyre iverksatt søknadsbehandlingstidspunkt.
                vedtakRepo.lagre(
                    VedtakMother.tiltakspengerVedtak(
                        vedtakId = "omgjoring",
                        sakId = sak.id,
                        rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                        virkningsperiode = 1.januar(2024) til 1.mars(2024),
                        opprettetTidspunkt = LocalDateTime.parse("2024-03-01T12:00:00"),
                        omgjørRammevedtakId = "soknadsvedtak",
                    ),
                )
                coEvery { arenaClient.hentVedtak(any(), any()) } returns emptyList()
                val vedtakService = VedtakService(vedtakRepo, arenaClient, sakRepo, tac.clock)
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
                            path("$VEDTAK_PATH/sak")
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
                                contentType() shouldBe ContentType.parse("application/json")
                                bodyAsText().shouldEqualJson(
                                    """
                                     {
                                        "sakId": "sakId123",
                                        "saksnummer": "SAK123",
                                        "kilde": "TPSAK",
                                        "status": "Avsluttet",
                                        "opprettetDato": "2024-01-15T10:30:00",
                                        "iverksattSoknadsbehandlingTidspunkt": "2024-02-01T12:00:00"
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
    fun `hent sak - iverksatt søknadsbehandling med TILTAKSPENGER_OG_BARNETILLEGG - inkluderer iverksattSoknadsbehandlingTidspunkt`() {
        assertIverksattSoknadsbehandlingTidspunkt(
            rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER_OG_BARNETILLEGG,
            forventetTidspunkt = "\"2024-02-01T12:00:00\"",
        )
    }

    @Test
    fun `hent sak - kun STANS-vedtak - iverksattSoknadsbehandlingTidspunkt er null`() {
        assertIverksattSoknadsbehandlingTidspunkt(
            rettighet = TiltakspengerVedtak.Rettighet.STANS,
            forventetTidspunkt = "null",
        )
    }

    @Test
    fun `hent sak - kun AVSLAG-vedtak - iverksattSoknadsbehandlingTidspunkt er null`() {
        assertIverksattSoknadsbehandlingTidspunkt(
            rettighet = TiltakspengerVedtak.Rettighet.AVSLAG,
            forventetTidspunkt = "null",
        )
    }

    @Test
    fun `hent sak - kun OPPHØR-vedtak - iverksattSoknadsbehandlingTidspunkt er null`() {
        assertIverksattSoknadsbehandlingTidspunkt(
            rettighet = TiltakspengerVedtak.Rettighet.OPPHØR,
            forventetTidspunkt = "null",
        )
    }

    private fun assertIverksattSoknadsbehandlingTidspunkt(
        rettighet: TiltakspengerVedtak.Rettighet,
        forventetTidspunkt: String,
    ) {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val sakRepo = testDataHelper.sakRepo
                val vedtakRepo = testDataHelper.vedtakRepo
                val fnr = Fnr.fromString("12345678910")
                val sak = SakMother.sak(
                    id = "sakId123",
                    saksnummer = "SAK123",
                    fnr = fnr,
                    opprettet = LocalDateTime.parse("2024-01-15T10:30:00"),
                )
                sakRepo.lagre(sak)
                vedtakRepo.lagre(
                    VedtakMother.tiltakspengerVedtak(
                        vedtakId = "vedtak",
                        sakId = sak.id,
                        rettighet = rettighet,
                        virkningsperiode = 1.januar(2024) til 1.mars(2024),
                        opprettetTidspunkt = LocalDateTime.parse("2024-02-01T12:00:00"),
                    ),
                )
                coEvery { arenaClient.hentVedtak(any(), any()) } returns emptyList()
                val vedtakService = VedtakService(vedtakRepo, arenaClient, sakRepo, tac.clock)
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
                            path("$VEDTAK_PATH/sak")
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
                                contentType() shouldBe ContentType.parse("application/json")
                                bodyAsText().shouldEqualJson(
                                    """
                                     {
                                        "sakId": "sakId123",
                                        "saksnummer": "SAK123",
                                        "kilde": "TPSAK",
                                        "status": "Avsluttet",
                                        "opprettetDato": "2024-01-15T10:30:00",
                                        "iverksattSoknadsbehandlingTidspunkt": $forventetTidspunkt
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
    fun `hent sak - har ikke sak i TPSAK men har i Arena - returnerer sak fra Arena`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val sakRepo = testDataHelper.sakRepo
                val vedtakRepo = testDataHelper.vedtakRepo
                val fnr = Fnr.fromString("12345678910")
                val arenaVedtak = ArenaVedtak(
                    periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 3, 31)),
                    rettighet = Rettighet.TILTAKSPENGER,
                    vedtakId = "arenaVedtakId",
                    kilde = Kilde.ARENA,
                    fnr = fnr,
                    antallBarn = 0,
                    dagsatsTiltakspenger = 285,
                    dagsatsBarnetillegg = null,
                    beslutningsdato = LocalDate.of(2024, 1, 5),
                    sak = ArenaVedtak.Sak(
                        sakId = "arenaSakId",
                        saksnummer = "ARENA123",
                        opprettetDato = LocalDate.of(2024, 1, 1),
                        status = "Aktiv",
                    ),
                )
                coEvery { arenaClient.hentVedtak(any(), any()) } returns listOf(arenaVedtak)
                val vedtakService = VedtakService(vedtakRepo, arenaClient, sakRepo, tac.clock)
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
                            path("$VEDTAK_PATH/sak")
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
                                contentType() shouldBe ContentType.parse("application/json")
                                bodyAsText().shouldEqualJson(
                                    """
                                    {
                                        "sakId": "arenaSakId",
                                        "saksnummer": "ARENA123",
                                        "kilde": "ARENA",
                                        "status": "Aktiv",
                                        "opprettetDato": "2024-01-01T09:00:00",
                                        "iverksattSoknadsbehandlingTidspunkt": null
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
    fun `hent sak - har ingen sak - returnerer 404`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val sakRepo = testDataHelper.sakRepo
                val vedtakRepo = testDataHelper.vedtakRepo
                coEvery { arenaClient.hentVedtak(any(), any()) } returns emptyList()
                val vedtakService = VedtakService(vedtakRepo, arenaClient, sakRepo, tac.clock)
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
                            path("$VEDTAK_PATH/sak")
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
                                status shouldBe HttpStatusCode.NotFound
                            }
                        }
                }
            }
        }
    }

    @Test
    fun `hent sak - ugyldig fnr - returnerer 400`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val sakRepo = testDataHelper.sakRepo
                val vedtakRepo = testDataHelper.vedtakRepo
                coEvery { arenaClient.hentVedtak(any(), any()) } returns emptyList()
                val vedtakService = VedtakService(vedtakRepo, arenaClient, sakRepo, tac.clock)
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
                            path("$VEDTAK_PATH/sak")
                        },
                        jwt = token,
                    ) {
                        setBody(
                            """
                            {
                                "ident": "ugyldig"
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
                            }
                        }
                }
            }
        }
    }

    @Test
    fun `hent sak - mangler rolle - returnerer 403`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val sakRepo = testDataHelper.sakRepo
                val vedtakRepo = testDataHelper.vedtakRepo
                coEvery { arenaClient.hentVedtak(any(), any()) } returns emptyList()
                val vedtakService = VedtakService(vedtakRepo, arenaClient, sakRepo, tac.clock)
                val token = getTokenUtenRolle()
                testApplication {
                    configureTestApplication(
                        vedtakService = vedtakService,
                        texasClient = tac.texasClient,
                    )
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("$VEDTAK_PATH/sak")
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
                                status shouldBe HttpStatusCode.Forbidden
                            }
                        }
                }
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

    private fun TestApplicationContext.getTokenUtenRolle(): String {
        val systembruker = Systembruker(
            roller = Systembrukerroller(emptyList()),
            klientnavn = "klientnavn",
            klientId = "id",
        )
        val token = this.jwtGenerator.createJwtForSystembruker(roles = emptyList())
        texasClient.leggTilSystembruker(token, systembruker)
        return token
    }
}
