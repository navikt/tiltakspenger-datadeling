package no.nav.tiltakspenger.datadeling.client

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import no.nav.tiltakspenger.datadeling.Configuration
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClientImpl
import no.nav.tiltakspenger.datadeling.domene.ArenaVedtak
import no.nav.tiltakspenger.datadeling.domene.PeriodisertKilde
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.datadeling.felles.infra.http.klient.httpClientGeneric
import no.nav.tiltakspenger.datadeling.routes.token
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class ArenaClientImplTest {
    private fun arenaClient(response: String?): ArenaClient {
        return ArenaClientImpl(
            config = Configuration.arenaClientConfig(),
            getToken = { token },
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
        val fnr = Fnr.fromString(ident)
        val fom = LocalDate.parse("2022-01-01")
        val tom = LocalDate.parse("2022-12-31")
        val periode = Periode(fom, tom)
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
                "rettighet": "TILTAKSPENGER",
                "vedtakId": 36475317,
                "sakId": 13297369
              }
            ]
        """.trimIndent()
        val arenaClient = arenaClient(responseJson)

        runTest {
            val result = arenaClient.hentVedtak(fnr, periode)

            result shouldBe listOf(
                ArenaVedtak(
                    periode = periode,
                    rettighet = Vedtak.Rettighet.TILTAKSPENGER,
                    vedtakId = "36475317",
                    sakId = "13297369",
                    saksnummer = null,
                    fnr = fnr,
                ),
            )
        }
    }

    @Test
    fun `hent av perioder fra arena`() {
        val ident = "01234567891"
        val fnr = Fnr.fromString(ident)
        val fom = LocalDate.parse("2022-01-01")
        val tom = LocalDate.parse("2022-12-31")
        val periode = Periode(fom, tom)
        val responseJson = """
            [
              {
                "fraOgMed": "$fom",
                "tilOgMed": "$tom"
              }
            ]
        """.trimIndent()
        val arenaClient = arenaClient(responseJson)

        runTest {
            val result = arenaClient.hentPerioder(fnr, periode)

            result shouldBe listOf(
                PeriodisertKilde(periode, "arena"),
            )
        }
    }
}
