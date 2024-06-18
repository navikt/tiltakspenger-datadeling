package no.nav.tiltakspenger.datadeling.client

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.datadeling.Configuration
import no.nav.tiltakspenger.datadeling.auth.TokenProvider
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClientImpl
import no.nav.tiltakspenger.datadeling.domene.Periode
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ArenaClientImplTest {
    private val mockTokenProvider = mockk<TokenProvider>(relaxed = true)
    private fun arenaClient(response: String?): ArenaClient {
        return ArenaClientImpl(
            config = Configuration.arenaClientConfig(),
            getToken = mockTokenProvider::getToken,
            httpClient = httpClientGeneric(mockEngine(response!!)),
        )
    }

    private fun mockEngine(response: String): MockEngine {
        return MockEngine {
            respond(
                content = response,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }
    }

    @Test
    fun `hent av vedtak fra arena`() {
        val ident = "01234567891"
        val fom = LocalDate.parse("2022-01-01")
        val tom = LocalDate.parse("2022-12-31")
        val responseJson = """
            [
              {
                "fraOgMed": "$fom",
                "tilOgMed": "$tom",
                "antallDager": 10.0,
                "dagsatsTiltakspenger": 285,
                "dagsatsBarnetillegg": 0,
                "antallBarn": 0,
                "relaterteTiltak": "tiltak",
                "rettighet": "TILTAKSPENGER"
              }
            ]
        """.trimIndent()
        val arenaClient = arenaClient(responseJson)

        runTest {
            val result = arenaClient.hentVedtak(ident, fom, tom)

            result shouldBe listOf(
                Vedtak(
                    fom = fom,
                    tom = tom,
                    antallDager = 10.0,
                    dagsatsTiltakspenger = 285,
                    dagsatsBarnetillegg = 0,
                    antallBarn = 0,
                ),
            )
        }
    }

    @Test
    fun `hent av perioder fra arena`() {
        val ident = "01234567891"
        val fom = LocalDate.parse("2022-01-01")
        val tom = LocalDate.parse("2022-12-31")
        val responseJson = """
            [
              {
                "fraOgMed": "$fom",
                "tilOgMed": "$tom",
                "antallDager": 10.0,
                "dagsatsTiltakspenger": 285,
                "dagsatsBarnetillegg": 0,
                "antallBarn": 0,
                "relaterteTiltak": "tiltak",
                "rettighet": "TILTAKSPENGER"
              }
            ]
        """.trimIndent()
        val arenaClient = arenaClient(responseJson)

        runTest {
            val result = arenaClient.hentPerioder(ident, fom, tom)

            result shouldBe listOf(
                Periode(fom, tom),
            )
        }
    }
}
