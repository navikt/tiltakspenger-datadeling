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
import no.nav.tiltakspenger.datadeling.behandling.datadeling.BehandlingService
import no.nav.tiltakspenger.datadeling.domene.Behandling
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.Systembrukerroller
import no.nav.tiltakspenger.datadeling.jacksonSerialization
import no.nav.tiltakspenger.datadeling.setupAuthentication
import no.nav.tiltakspenger.datadeling.testutils.TestApplicationContext
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequest
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import org.junit.jupiter.api.Test
import java.time.LocalDate

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
                roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_BEHANDLING)),
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
                    HttpMethod.Companion.Post,
                    url {
                        protocol = URLProtocol.Companion.HTTPS
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
                            status shouldBe HttpStatusCode.Companion.OK
                            contentType() shouldBe ContentType.Companion.parse("application/json; charset=UTF-8")
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
            } returns emptyList<Behandling>()
            val systembruker = Systembruker(
                roller = Systembrukerroller(listOf<Systembrukerrolle>(Systembrukerrolle.LES_BEHANDLING)),
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
                    HttpMethod.Companion.Post,
                    url {
                        protocol = URLProtocol.Companion.HTTPS
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
                            status shouldBe HttpStatusCode.Companion.OK
                            contentType() shouldBe ContentType.Companion.parse("application/json; charset=UTF-8")
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
                            behandlingRoutes(
                                behandlingService = behandlingService,
                            )
                        }
                    }
                }
                defaultRequest(
                    HttpMethod.Companion.Post,
                    url {
                        protocol = URLProtocol.Companion.HTTPS
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
                            status shouldBe HttpStatusCode.Companion.Forbidden
                            contentType() shouldBe ContentType.Companion.parse("application/json; charset=UTF-8")
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
}
