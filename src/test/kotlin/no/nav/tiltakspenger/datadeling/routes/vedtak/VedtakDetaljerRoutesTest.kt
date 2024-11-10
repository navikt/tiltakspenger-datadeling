package no.nav.tiltakspenger.datadeling.routes.vedtak

import arrow.core.right
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.datadeling.routes.TestApplicationContext
import no.nav.tiltakspenger.datadeling.routes.configureTestApplication
import no.nav.tiltakspenger.datadeling.service.VedtakService
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate

internal class VedtakDetaljerRoutesTest {
    private val vedtakRequestBody = """
        {
            "ident": "12345678910",
            "fom": "2021-01-01",
            "tom": "2021-01-31"
        }
    """.trimIndent()

    @Test
    fun `post med ugyldig token skal gi 401`() {
        testApplication {
            configureTestApplication()
            val response = client.post("/vedtak/detaljer") {
                header("Authorization", "Bearer tulletoken")
                header("Content-Type", "application/json")
                setBody(vedtakRequestBody)
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `post med gyldig token skal gi 200`() {
        with(TestApplicationContext()) {
            val tac = this
            val vedtakServiceMock = mockk<VedtakService>().also { mock ->
                coEvery { mock.hentVedtak(any(), any(), any(), any()) } returns listOf(
                    Vedtak(
                        fom = LocalDate.of(2021, 1, 1),
                        tom = LocalDate.of(2021, 1, 31),
                        antallDager = 10.0,
                        dagsatsTiltakspenger = 1000,
                        dagsatsBarnetillegg = 200,
                        antallBarn = 2,
                        tiltaksgjennomføringId = "tiltak",
                        rettighet = Rettighet.TILTAKSPENGER,
                        vedtakId = "12345678910",
                        sakId = "9876543210",
                        saksnummer = "12345678910",
                        kilde = "tp",
                        fnr = Fnr.random(),
                    ),
                ).right()
            }
            val token = tac.jwtGenerator.createJwtForSystembruker(
                roles = listOf("les-vedtak"),
            )
            testApplication {
                configureTestApplication(vedtakService = vedtakServiceMock, tokenService = tac.tokenService)
                val response = client.post("/vedtak/detaljer") {
                    header("Authorization", "Bearer $token")
                    header("Content-Type", "application/json")
                    setBody(vedtakRequestBody)
                }
                assertEquals(HttpStatusCode.OK, response.status)
            }
        }
    }

    @Test
    fun `post med utgått token skal gi 401`() {
        with(TestApplicationContext()) {
            val tac = this
            val token = tac.jwtGenerator.createJwtForSystembruker(
                roles = listOf("les-vedtak"),
                expiresAt = Instant.now().minusSeconds(60),
            )
            testApplication {
                configureTestApplication(tokenService = tac.tokenService)
                val response = client.post("/vedtak/detaljer") {
                    header("Authorization", "Bearer $token")
                    header("Content-Type", "application/json")
                    setBody(vedtakRequestBody)
                }
                assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }
    }

    @Test
    fun `post med feil issuer token skal gi 401`() {
        with(TestApplicationContext()) {
            val tac = this
            val token = jwtGenerator.createJwtForSystembruker(
                issuer = "feilIssuer",
                roles = listOf("les-vedtak"),
            )
            testApplication {
                configureTestApplication(tokenService = tac.tokenService)
                val response = client.post("/vedtak/detaljer") {
                    header("Authorization", "Bearer $token")
                    header("Content-Type", "application/json")
                    setBody(vedtakRequestBody)
                }
                assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }
    }

    @Test
    fun `post med feil audience token skal gi 401`() {
        with(TestApplicationContext()) {
            val tac = this
            val token = jwtGenerator.createJwtForSystembruker(
                audience = "feilAudience",
                roles = listOf("les-vedtak"),
            )
            testApplication {
                configureTestApplication(tokenService = tac.tokenService)
                val response = client.post("/vedtak/detaljer") {
                    header("Authorization", "Bearer $token")
                    header("Content-Type", "application/json")
                    setBody(vedtakRequestBody)
                }
                assertEquals(HttpStatusCode.Unauthorized, response.status)
            }
        }
    }
}
