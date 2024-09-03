package no.nav.tiltakspenger.datadeling.routes

import com.nimbusds.jwt.SignedJWT
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.tiltakspenger.datadeling.configureTestApplication
import no.nav.tiltakspenger.datadeling.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.datadeling.service.VedtakService
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class VedtakRoutesTest {
    companion object {
        private val mockOAuth2Server = MockOAuth2Server().also {
            it.start(8080)
        }

        @AfterAll
        @JvmStatic
        fun teardown() = mockOAuth2Server.shutdown()
    }

    private fun token(
        issuerId: String = "azure",
        audience: String = "validAudience",
        expiry: Long = 3600L,
    ): SignedJWT = mockOAuth2Server
        .issueToken(
            issuerId = issuerId,
            audience = audience,
            expiry = expiry,
        )

    private val gyldigAzureToken: SignedJWT = token()

    private val utgåttAzureToken: SignedJWT = token(expiry = -60L)

    private val tokenMedFeilIssuer: SignedJWT = token(issuerId = "feilIssuer")

    private val tokenMedFeilAudience: SignedJWT = token(audience = "feilAudience")

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
        val vedtakServiceMock = mockk<VedtakService>().also { mock ->
            coEvery { mock.hentVedtak(any(), any(), any()) } returns listOf(
                Vedtak(
                    fom = LocalDate.of(2021, 1, 1),
                    tom = LocalDate.of(2021, 1, 31),
                    antallDager = 10.0,
                    dagsatsTiltakspenger = 1000,
                    dagsatsBarnetillegg = 200,
                    antallBarn = 2,
                    relaterteTiltak = "tiltak",
                    rettighet = Rettighet.TILTAKSPENGER,
                    vedtakId = "12345678910",
                    sakId = "9876543210",
                    saksnummer = "12345678910",
                    kilde = "tp",
                ),
            )
        }

        testApplication {
            configureTestApplication(vedtakService = vedtakServiceMock)
            val response = client.post("/vedtak/detaljer") {
                header("Authorization", "Bearer ${gyldigAzureToken.serialize()}")
                header("Content-Type", "application/json")
                setBody(vedtakRequestBody)
            }
            assertEquals(HttpStatusCode.OK, response.status)
        }
    }

    @Test
    fun `post med utgått token skal gi 401`() {
        testApplication {
            configureTestApplication()
            val response = client.post("/vedtak/detaljer") {
                header("Authorization", "Bearer ${utgåttAzureToken.serialize()}")
                header("Content-Type", "application/json")
                setBody(vedtakRequestBody)
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `post med feil issuer token skal gi 401`() {
        testApplication {
            configureTestApplication()
            val response = client.post("/vedtak/detaljer") {
                header("Authorization", "Bearer ${tokenMedFeilIssuer.serialize()}")
                header("Content-Type", "application/json")
                setBody(vedtakRequestBody)
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }

    @Test
    fun `post med feil audience token skal gi 401`() {
        testApplication {
            configureTestApplication()
            val response = client.post("/vedtak/detaljer") {
                header("Authorization", "Bearer ${tokenMedFeilAudience.serialize()}")
                header("Content-Type", "application/json")
                setBody(vedtakRequestBody)
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
    }
}
