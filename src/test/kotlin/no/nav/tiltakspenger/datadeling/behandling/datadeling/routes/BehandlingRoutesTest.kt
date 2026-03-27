package no.nav.tiltakspenger.datadeling.behandling.datadeling.routes

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
import no.nav.tiltakspenger.datadeling.application.exception.ExceptionHandler
import no.nav.tiltakspenger.datadeling.application.jacksonSerialization
import no.nav.tiltakspenger.datadeling.application.setupAuthentication
import no.nav.tiltakspenger.datadeling.behandling.datadeling.BehandlingService
import no.nav.tiltakspenger.datadeling.behandling.domene.Behandling
import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.Systembrukerroller
import no.nav.tiltakspenger.datadeling.testdata.BehandlingMother
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testutils.LogCapture
import no.nav.tiltakspenger.datadeling.testutils.TestApplicationContext
import no.nav.tiltakspenger.datadeling.testutils.configureTestApplication
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BehandlingRoutesTest {
    @Test
    fun `hent behandlinger - har behandlinger - returnerer liste med behandlinger`() {
        with(TestApplicationContext()) {
            val tac = this

            val behandlingService = mockk<BehandlingService>(relaxed = true)
            coEvery { behandlingService.hentBehandlingerForTp(any(), any()) } returns listOf(
                Behandling(
                    behandlingId = "behandlingId",
                    periode = Periode(
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 12, 31),
                    ),
                ),
            )
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf(Systembrukerrolle.LES_BEHANDLING)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-behandling"))
            texasClient.leggTilSystembruker(token, systembruker)

            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            behandlingRoutes(
                                behandlingService = behandlingService,
                            )
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("behandlinger/perioder")
                    },
                    token,
                ) {
                    setBody(
                        """
                        {
                            "ident": "12345678910",
                            "fom": "2024-01-01",
                            "tom": "2024-01-01"
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
                                // language=JSON
                                """[
                            {
                              "behandlingId" : "behandlingId",
                              "fom":"2024-01-01",
                              "tom":"2024-12-31"
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
    fun `hent behandlinger - har ingen behandlinger - returnerer tom liste`() {
        with(TestApplicationContext()) {
            val tac = this

            val behandlingService = mockk<BehandlingService>(relaxed = true)
            coEvery {
                behandlingService.hentBehandlingerForTp(
                    any(),
                    any(),
                )
            } returns emptyList()
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf(Systembrukerrolle.LES_BEHANDLING)),
                klientnavn = "klientnavn",
                klientId = "id",
            )
            val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-behandling"))
            texasClient.leggTilSystembruker(token, systembruker)

            testApplication {
                application {
                    jacksonSerialization()
                    setupAuthentication(texasClient)
                    routing {
                        authenticate(IdentityProvider.AZUREAD.value) {
                            behandlingRoutes(
                                behandlingService = behandlingService,
                            )
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("behandlinger/perioder")
                    },
                    token,
                ) {
                    setBody(
                        """
                        {
                            "ident": "12345678910",
                            "fom": "2024-01-01",
                            "tom": "2024-01-01"
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
                                // language=JSON
                                """
                                    []
                                """.trimIndent(),
                            )
                        }
                    }
            }
        }
    }

    @Test
    fun `hent behandlinger - har ikke tilgang - returnerer forbidden`() {
        with(TestApplicationContext()) {
            val tac = this

            val behandlingService = mockk<BehandlingService>(relaxed = true)
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
                            behandlingRoutes(
                                behandlingService = behandlingService,
                            )
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("behandlinger/perioder")
                    },
                    token,
                ) {
                    setBody(
                        """
                        {
                            "ident": "12345678910",
                            "fom": "2024-01-01",
                            "tom": "2024-01-01"
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
                            contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                            bodyAsText().shouldEqualJson(
                                // language=JSON
                                """
                                    {
                                      "melding":"Mangler rollen LES_BEHANDLING. Har rollene: [LES_VEDTAK]",
                                      "kode":"mangler_rolle"
                                    }
                                """.trimIndent(),
                            )
                        }
                    }
            }
        }
    }

    @Test
    fun `hent åpne behandlinger - har åpne behandlinger - returnerer liste med behandlinger`() {
        with(TestApplicationContext()) {
            val tac = this
            withMigratedDb { testDataHelper ->
                val behandlingRepo = testDataHelper.behandlingRepo
                val sakRepo = testDataHelper.sakRepo
                val behandlingService = BehandlingService(behandlingRepo)
                val fnr = Fnr.random()
                val sak = SakMother.sak(
                    fnr = fnr,
                    opprettet = LocalDateTime.parse("2020-01-01T00:00:00.000"),
                )
                sakRepo.lagre(sak)
                val avsluttetBehandling = BehandlingMother.tiltakspengerBehandling(
                    sakId = sak.id,
                    behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.VEDTATT,
                )
                behandlingRepo.lagre(avsluttetBehandling)
                val apenMeldekortbehandling = BehandlingMother.tiltakspengerBehandling(
                    behandlingId = "57048fe4-a58d-495b-8ace-6139f0c704ee",
                    sakId = sak.id,
                    fom = LocalDate.of(2025, 11, 3),
                    tom = LocalDate.of(2025, 11, 17),
                    behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.UNDER_BESLUTNING,
                    beslutter = null,
                    iverksattTidspunkt = null,
                    behandlingstype = TiltakspengerBehandling.Behandlingstype.MELDEKORTBEHANDLING,
                )
                behandlingRepo.lagre(apenMeldekortbehandling)
                val systembruker = Systembruker(
                    roller = Systembrukerroller(listOf(Systembrukerrolle.LES_BEHANDLING)),
                    klientnavn = "klientnavn",
                    klientId = "id",
                )
                val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-behandling"))
                texasClient.leggTilSystembruker(token, systembruker)

                testApplication {
                    application {
                        jacksonSerialization()
                        setupAuthentication(texasClient)
                        routing {
                            authenticate(IdentityProvider.AZUREAD.value) {
                                behandlingRoutes(
                                    behandlingService = behandlingService,
                                )
                            }
                        }
                    }
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("behandlinger/apne")
                        },
                        token,
                    ) {
                        setBody(
                            """
                        {
                            "ident": "${fnr.verdi}"
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
                                    // language=JSON
                                    """
                                    {
                                      "behandlinger": [
                                        {
                                          "behandlingId": "57048fe4-a58d-495b-8ace-6139f0c704ee",
                                          "fom": "2025-11-03",
                                          "tom": "2025-11-17",
                                          "behandlingstatus": "UNDER_BESLUTNING",
                                          "behandlingstype": "MELDEKORTBEHANDLING",
                                          "saksbehandler": "testSaksbehandler",
                                          "beslutter": null,
                                          "iverksattTidspunkt": null,
                                          "opprettet": "2021-01-01T00:00:00",
                                          "sistEndret": "2021-01-01T00:00:00"
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
    fun `hent åpne behandlinger - har ingen åpne behandlinger - returnerer tom respons`() {
        with(TestApplicationContext()) {
            val tac = this
            withMigratedDb { testDataHelper ->
                val behandlingRepo = testDataHelper.behandlingRepo
                val behandlingService = BehandlingService(behandlingRepo)

                val systembruker = Systembruker(
                    roller = Systembrukerroller(listOf(Systembrukerrolle.LES_BEHANDLING)),
                    klientnavn = "klientnavn",
                    klientId = "id",
                )
                val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-behandling"))
                texasClient.leggTilSystembruker(token, systembruker)

                testApplication {
                    application {
                        jacksonSerialization()
                        setupAuthentication(texasClient)
                        routing {
                            authenticate(IdentityProvider.AZUREAD.value) {
                                behandlingRoutes(
                                    behandlingService = behandlingService,
                                )
                            }
                        }
                    }
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("behandlinger/apne")
                        },
                        token,
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
                                    // language=JSON
                                    """
                                    {
                                      "behandlinger": [],
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
    fun `hent åpne behandlinger - har ikke tilgang - returnerer forbidden`() {
        with(TestApplicationContext()) {
            val tac = this

            val behandlingService = mockk<BehandlingService>(relaxed = true)
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
                            behandlingRoutes(
                                behandlingService = behandlingService,
                            )
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("behandlinger/apne")
                    },
                    token,
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
                            contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
                            bodyAsText().shouldEqualJson(
                                // language=JSON
                                """
                                    {
                                      "melding":"Mangler rollen LES_BEHANDLING. Har rollene: [LES_VEDTAK]",
                                      "kode":"mangler_rolle"
                                    }
                                """.trimIndent(),
                            )
                        }
                    }
            }
        }
    }

    @Test
    fun `hent behandlinger for periode - fom etter tom - returnerer 400 uten sensitiv request-data`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val behandlingService = BehandlingService(testDataHelper.behandlingRepo)
                val systembruker = Systembruker(
                    roller = Systembrukerroller(listOf(Systembrukerrolle.LES_BEHANDLING)),
                    klientnavn = "klientnavn",
                    klientId = "id",
                )
                val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-behandling"))
                texasClient.leggTilSystembruker(token, systembruker)

                testApplication {
                    configureTestApplication(
                        behandlingService = behandlingService,
                        texasClient = tac.texasClient,
                    )
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("behandlinger/perioder")
                        },
                        token,
                    ) {
                        setBody(
                            """
                            {
                                "ident": "12345678910",
                                "fom": "2024-12-31",
                                "tom": "2024-01-01"
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
                                contentType() shouldBe ContentType.parse("application/json")
                                bodyAsText().shouldEqualJson(
                                    """
                                    {
                                      "feilmelding": "Fra-dato kan ikke være etter til-dato."
                                    }
                                    """.trimIndent(),
                                )
                                bodyAsText().contains("12345678910") shouldBe false
                                bodyAsText().contains("2024-12-31") shouldBe false
                                bodyAsText().contains("2024-01-01") shouldBe false
                            }
                        }
                }
            }
        }
    }

    @Test
    fun `hent åpne behandlinger - mangler ident - returnerer 400 med nyttig feilmelding`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val behandlingService = BehandlingService(testDataHelper.behandlingRepo)
                val systembruker = Systembruker(
                    roller = Systembrukerroller(listOf(Systembrukerrolle.LES_BEHANDLING)),
                    klientnavn = "klientnavn",
                    klientId = "id",
                )
                val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-behandling"))
                texasClient.leggTilSystembruker(token, systembruker)

                testApplication {
                    configureTestApplication(
                        behandlingService = behandlingService,
                        texasClient = tac.texasClient,
                    )
                    defaultRequest(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("behandlinger/apne")
                        },
                        token,
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
    }

    @Test
    fun `hent åpne behandlinger - ugyldig json - returnerer 400 og logger uten sensitivt innhold`() {
        with(TestApplicationContext()) {
            withMigratedDb { testDataHelper ->
                val tac = this
                val behandlingService = BehandlingService(testDataHelper.behandlingRepo)
                val systembruker = Systembruker(
                    roller = Systembrukerroller(listOf(Systembrukerrolle.LES_BEHANDLING)),
                    klientnavn = "klientnavn",
                    klientId = "id",
                )
                val token = tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-behandling"))
                texasClient.leggTilSystembruker(token, systembruker)

                LogCapture.attach(ExceptionHandler::class.java).use { logCapture ->
                    testApplication {
                        configureTestApplication(
                            behandlingService = behandlingService,
                            texasClient = tac.texasClient,
                        )
                        defaultRequest(
                            HttpMethod.Post,
                            url {
                                protocol = URLProtocol.HTTPS
                                path("behandlinger/apne")
                            },
                            token,
                        ) {
                            setBody(
                                """
                                {
                                    "ident": "12345678910",
                                    "ekstra": "HEMMELIG-RESPONS-TEKST",
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
                                    bodyAsText().contains("HEMMELIG-RESPONS-TEKST") shouldBe false
                                }
                            }
                    }

                    val logs = logCapture.combined()
                    logs.contains("12345678910") shouldBe false
                    logs.contains("HEMMELIG-RESPONS-TEKST") shouldBe false
                    logs.contains("/behandlinger/apne") shouldBe true
                    logs.contains("Ugyldig JSON") shouldBe true
                }
            }
        }
    }
}
