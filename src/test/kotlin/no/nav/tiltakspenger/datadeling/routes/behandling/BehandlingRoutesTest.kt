package no.nav.tiltakspenger.datadeling.routes.behandling

import arrow.core.right
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
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.domene.Behandling
import no.nav.tiltakspenger.datadeling.jacksonSerialization
import no.nav.tiltakspenger.datadeling.routes.TestApplicationContext
import no.nav.tiltakspenger.datadeling.routes.behandlingRoutes
import no.nav.tiltakspenger.datadeling.service.BehandlingService
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.vedtak.routes.defaultRequest
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlingRoutesTest {
    @Test
    fun `test hent vedtak route`() {
        with(TestApplicationContext()) {
            val tac = this

            val behandlingService = mockk<BehandlingService>(relaxed = true)
            coEvery { behandlingService.hentBehandlingerForTp(any(), any(), any()) } returns listOf(
                Behandling(
                    behandlingId = "behandlingId",
                    periode = Periode(
                        LocalDate.of(2024, 1, 1),
                        LocalDate.of(2024, 12, 31),
                    ),
                ),
            ).right()
            testApplication {
                application {
                    jacksonSerialization()
                    routing {
                        behandlingRoutes(
                            behandlingService = behandlingService,
                            tokenService = tac.tokenService,
                        )
                    }
                }
                defaultRequest(
                    HttpMethod.Post,
                    url {
                        protocol = URLProtocol.HTTPS
                        path("behandlinger/perioder")
                    },
                    tac.jwtGenerator.createJwtForSystembruker(roles = listOf("les-vedtak")),
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
                            contentType() shouldBe ContentType.parse("application/json; charset=UTF-8")
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
}
