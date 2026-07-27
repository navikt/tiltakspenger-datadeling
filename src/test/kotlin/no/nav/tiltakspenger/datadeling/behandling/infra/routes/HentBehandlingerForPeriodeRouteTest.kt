package no.nav.tiltakspenger.datadeling.behandling.infra.routes

import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.Systembruker
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.Systembrukerroller
import no.nav.tiltakspenger.datadeling.behandling.Behandling
import no.nav.tiltakspenger.datadeling.behandling.HentBehandlingerForPeriodeService
import no.nav.tiltakspenger.datadeling.infra.jacksonSerialization
import no.nav.tiltakspenger.datadeling.infra.setupAuthentication
import no.nav.tiltakspenger.datadeling.testutils.TestApplicationContext
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import org.junit.jupiter.api.Test
import java.time.LocalDate

/**
 * Tester `POST /behandlinger/perioder`.
 */
class HentBehandlingerForPeriodeRouteTest {
    @Test
    fun `hent behandlinger - har behandlinger - returnerer liste med behandlinger`() {
        with(TestApplicationContext()) {
            val tac = this

            val hentBehandlingerForPeriodeService = mockk<HentBehandlingerForPeriodeService>(relaxed = true)
            coEvery { hentBehandlingerForPeriodeService.hentBehandlingerForPeriode(any(), any()) } returns listOf(
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
                            hentBehandlingerForPeriodeRoute(
                                hentBehandlingerForPeriodeService = hentBehandlingerForPeriodeService,
                            )
                        }
                    }
                }
                defaultRequestWithAssertions(
                    HttpMethod.POST,
                    "behandlinger/perioder",
                    jwt = token,
                    forventet = ForventetRespons(
                        status = 200,
                        body = ForventetBody.Json(
                            // language=JSON
                            """[
                            {
                              "behandlingId" : "behandlingId",
                              "fom":"2024-01-01",
                              "tom":"2024-12-31"
                            }
                            ]
                            """.trimIndent(),
                        ),
                        contentType = "application/json",
                    ),
                    body =
                    """
                        {
                            "ident": "12345678910",
                            "fom": "2024-01-01",
                            "tom": "2024-01-01"
                        }
                    """.trimIndent(),
                )
            }
        }
    }

    @Test
    fun `hent behandlinger - har ingen behandlinger - returnerer tom liste`() {
        with(TestApplicationContext()) {
            val tac = this

            val hentBehandlingerForPeriodeService = mockk<HentBehandlingerForPeriodeService>(relaxed = true)
            coEvery {
                hentBehandlingerForPeriodeService.hentBehandlingerForPeriode(
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
                            hentBehandlingerForPeriodeRoute(
                                hentBehandlingerForPeriodeService = hentBehandlingerForPeriodeService,
                            )
                        }
                    }
                }
                defaultRequestWithAssertions(
                    HttpMethod.POST,
                    "behandlinger/perioder",
                    jwt = token,
                    forventet = ForventetRespons(
                        status = 200,
                        body = ForventetBody.Json(
                            // language=JSON
                            """
                                    []
                            """.trimIndent(),
                        ),
                        contentType = "application/json",
                    ),
                    body =
                    """
                        {
                            "ident": "12345678910",
                            "fom": "2024-01-01",
                            "tom": "2024-01-01"
                        }
                    """.trimIndent(),
                )
            }
        }
    }

    @Test
    fun `hent behandlinger - har ikke tilgang - returnerer forbidden`() {
        with(TestApplicationContext()) {
            val tac = this

            val hentBehandlingerForPeriodeService = mockk<HentBehandlingerForPeriodeService>(relaxed = true)
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
                            hentBehandlingerForPeriodeRoute(
                                hentBehandlingerForPeriodeService = hentBehandlingerForPeriodeService,
                            )
                        }
                    }
                }
                defaultRequestWithAssertions(
                    HttpMethod.POST,
                    "behandlinger/perioder",
                    jwt = token,
                    forventet = ForventetRespons(
                        status = 403,
                        body = ForventetBody.Json(
                            // language=JSON
                            """
                                    {
                                      "melding":"Mangler rollen LES_BEHANDLING. Har rollene: [LES_VEDTAK]",
                                      "kode":"mangler_rolle"
                                    }
                            """.trimIndent(),
                        ),
                        contentType = "application/json; charset=UTF-8",
                    ),
                    body =
                    """
                        {
                            "ident": "12345678910",
                            "fom": "2024-01-01",
                            "tom": "2024-01-01"
                        }
                    """.trimIndent(),
                )
            }
        }
    }

    @Test
    fun `hent behandlinger - ugyldig ident - returnerer bad request`() {
        with(TestApplicationContext()) {
            val tac = this
            val hentBehandlingerForPeriodeService = mockk<HentBehandlingerForPeriodeService>(relaxed = true)
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
                            hentBehandlingerForPeriodeRoute(hentBehandlingerForPeriodeService = hentBehandlingerForPeriodeService)
                        }
                    }
                }
                defaultRequestWithAssertions(
                    HttpMethod.POST,
                    "behandlinger/perioder",
                    jwt = token,
                    forventet = ForventetRespons(
                        status = 400,
                        body = ForventetBody.Json(
                            """
                        {
                          "feilmelding": "Ugyldig ident. Må bestå av 11 siffer."
                        }
                            """.trimIndent(),
                        ),
                    ),
                    body =
                    """
                        {
                            "ident": "ugyldig",
                            "fom": "2024-01-01",
                            "tom": "2024-01-31"
                        }
                    """.trimIndent(),
                )
            }
        }
    }
}
