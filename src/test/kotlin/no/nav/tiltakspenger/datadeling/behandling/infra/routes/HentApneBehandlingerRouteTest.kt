package no.nav.tiltakspenger.datadeling.behandling.infra.routes

import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.Systembruker
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.Systembrukerroller
import no.nav.tiltakspenger.datadeling.behandling.HentApneBehandlingerService
import no.nav.tiltakspenger.datadeling.behandling.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.infra.jacksonSerialization
import no.nav.tiltakspenger.datadeling.infra.setupAuthentication
import no.nav.tiltakspenger.datadeling.testdata.BehandlingMother
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testutils.TestApplicationContext
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Tester `POST /behandlinger/apne`.
 */
class HentApneBehandlingerRouteTest {
    @Test
    fun `hent åpne behandlinger - har åpne behandlinger - returnerer liste med behandlinger`() {
        with(TestApplicationContext()) {
            val tac = this
            withMigratedDb { testDataHelper ->
                val behandlingRepo = testDataHelper.behandlingRepo
                val sakRepo = testDataHelper.sakRepo
                val hentApneBehandlingerService = HentApneBehandlingerService(behandlingRepo)
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
                                hentApneBehandlingerRoute(
                                    hentApneBehandlingerService = hentApneBehandlingerService,
                                )
                            }
                        }
                    }
                    defaultRequestWithAssertions(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("behandlinger/apne")
                        },
                        jwt = token,
                        forventet = ForventetRespons(
                            status = HttpStatusCode.OK,
                            body = ForventetBody.Json(
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
                                        "sakId": "sak_01ARZ3NDEKTSV4RRFFQ69G5FAV",
                                        "saksnummer": "202401011001",
                                        "kilde": "TPSAK",
                                        "status": "Løpende",
                                        "opprettetDato": "2020-01-01T00:00:00"
                                      }
                                    }
                                """.trimIndent(),
                            ),
                            contentType = ContentType.parse("application/json"),
                        ),
                    ) {
                        setBody(
                            """
                        {
                            "ident": "${fnr.verdi}"
                        }
                            """.trimIndent(),
                        )
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
                val hentApneBehandlingerService = HentApneBehandlingerService(behandlingRepo)

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
                                hentApneBehandlingerRoute(
                                    hentApneBehandlingerService = hentApneBehandlingerService,
                                )
                            }
                        }
                    }
                    defaultRequestWithAssertions(
                        HttpMethod.Post,
                        url {
                            protocol = URLProtocol.HTTPS
                            path("behandlinger/apne")
                        },
                        jwt = token,
                        forventet = ForventetRespons(
                            status = HttpStatusCode.OK,
                            body = ForventetBody.Json(
                                // language=JSON
                                """
                                    {
                                      "behandlinger": [],
                                      "sak": null
                                    }
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
    fun `hent åpne behandlinger - har ikke tilgang - returnerer forbidden`() {
        with(TestApplicationContext()) {
            val tac = this

            val hentApneBehandlingerService = mockk<HentApneBehandlingerService>(relaxed = true)
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
                            hentApneBehandlingerRoute(
                                hentApneBehandlingerService = hentApneBehandlingerService,
                            )
                        }
                    }
                }
                defaultRequestWithAssertions(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("behandlinger/apne")
                    },
                    jwt = token,
                    forventet = ForventetRespons(
                        status = HttpStatusCode.Forbidden,
                        body = ForventetBody.Json(
                            // language=JSON
                            """
                                    {
                                      "melding":"Mangler rollen LES_BEHANDLING. Har rollene: [LES_VEDTAK]",
                                      "kode":"mangler_rolle"
                                    }
                            """.trimIndent(),
                        ),
                        contentType = ContentType.parse("application/json; charset=UTF-8"),
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
    fun `hent åpne behandlinger - ugyldig ident - returnerer bad request`() {
        with(TestApplicationContext()) {
            val tac = this
            val hentApneBehandlingerService = mockk<HentApneBehandlingerService>(relaxed = true)
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
                            hentApneBehandlingerRoute(hentApneBehandlingerService = hentApneBehandlingerService)
                        }
                    }
                }
                defaultRequestWithAssertions(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("behandlinger/apne")
                    },
                    jwt = token,
                    forventet = ForventetRespons(
                        status = HttpStatusCode.BadRequest,
                        body = ForventetBody.Json(
                            """
                        {
                          "feilmelding": "Ident ugyldig er ugyldig. Må bestå av 11 siffer"
                        }
                            """.trimIndent(),
                        ),
                    ),
                ) {
                    setBody(
                        """
                        {
                            "ident": "ugyldig"
                        }
                        """.trimIndent(),
                    )
                }
            }
        }
    }
}
