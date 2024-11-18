package no.nav.tiltakspenger.datadeling.routes.vedtak

import arrow.core.right
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.datadeling.routes.TestApplicationContext
import no.nav.tiltakspenger.datadeling.routes.configureTestApplication
import no.nav.tiltakspenger.datadeling.service.VedtakService
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime

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
                coEvery { mock.hentTpVedtak(any(), any(), any()) } returns listOf(
                    TiltakspengerVedtak(
                        periode = Periode(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 31)),
                        rettighet = Vedtak.Rettighet.TILTAKSPENGER,
                        vedtakId = "12345678910",
                        sakId = "9876543210",
                        saksnummer = "12345678910",
                        fnr = Fnr.random(),
                        antallDagerPerMeldeperiode = 10,
                        mottattTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
                        opprettetTidspunkt = LocalDateTime.parse("2021-01-01T00:00:00.000"),
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
