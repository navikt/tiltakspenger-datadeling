package no.nav.tiltakspenger.datadeling.arena.infra.routes

import io.kotest.matchers.shouldBe
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.testdata.ArenaMother
import no.nav.tiltakspenger.datadeling.testutils.leggTilSystembruker
import no.nav.tiltakspenger.datadeling.testutils.withTestApplicationContextInMemory
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import org.junit.jupiter.api.Test

/**
 * Route- og JSON-tester for `GET /arena/utbetalingshistorikk/detaljer`.
 * Ingen kjent konsument per juli 2026, men endepunktet er eksponert og dekkes derfor på lik linje med de andre.
 */
class HentArenaUtbetalingshistorikkDetaljerRouteTest {

    @Test
    fun `hent detaljer - har vedtakfakta og anmerkninger - returnerer hele kontrakten`() {
        withTestApplicationContextInMemory { tac ->
            tac.arenaClient.utbetalingshistorikkDetaljer = ArenaMother.utbetalingshistorikkDetaljer()
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_MELDEKORT)

            getDetaljer(
                token,
                vedtakId = "7654321",
                meldekortId = "1234567",
                forventet = ForventetRespons(
                    status = HttpStatusCode.OK,
                    body = ForventetBody.Json(
                        // language=JSON
                        """
                        {
                          "vedtakfakta": {
                            "dagsats": 285,
                            "gjelderFra": "2024-01-01",
                            "gjelderTil": "2024-06-30",
                            "antallUtbetalinger": 13,
                            "belopPerUtbetalinger": 1995,
                            "alternativBetalingsmottaker": null
                          },
                          "anmerkninger": [
                            {
                              "kilde": "ARENA",
                              "registrert": "2024-01-20T09:00:00",
                              "beskrivelse": "Utbetaling gjennomført"
                            }
                          ]
                        }
                        """.trimIndent(),
                    ),
                    contentType = ContentType.parse("application/json"),
                ),
            )
        }
    }

    @Test
    fun `hent detaljer - uten vedtakfakta og anmerkninger - serialiseres som null og tom liste`() {
        withTestApplicationContextInMemory { tac ->
            tac.arenaClient.utbetalingshistorikkDetaljer =
                ArenaMother.utbetalingshistorikkDetaljer(vedtakfakta = null, anmerkninger = emptyList())
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_MELDEKORT)

            getDetaljer(
                token,
                vedtakId = "7654321",
                meldekortId = null,
                forventet = ForventetRespons(
                    status = HttpStatusCode.OK,
                    body = ForventetBody.Json("""{"vedtakfakta": null, "anmerkninger": []}"""),
                    contentType = ContentType.parse("application/json"),
                ),
            )
        }
    }

    @Test
    fun `hent detaljer - id-er som ikke er tall - sendes videre som null`() {
        withTestApplicationContextInMemory { tac ->
            tac.arenaClient.utbetalingshistorikkDetaljer = ArenaMother.utbetalingshistorikkDetaljer()
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_MELDEKORT)

            getDetaljer(
                token,
                vedtakId = "ikke-et-tall",
                meldekortId = "heller-ikke",
                forventet = ForventetRespons(status = HttpStatusCode.OK),
            )

            tac.arenaClient.sisteDetaljerForespørsel shouldBe
                ArenaClient.ArenaUtbetalingshistorikkDetaljerForespørsel(meldekortId = null, vedtakId = null)
        }
    }

    @Test
    fun `hent detaljer - arena feiler - svarer 500 server_feil`() {
        withTestApplicationContextInMemory { tac ->
            tac.arenaClient.utbetalingshistorikkDetaljer = ArenaMother.utbetalingshistorikkDetaljer()
            tac.arenaClient.feiler = true
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_MELDEKORT)

            getDetaljer(
                token,
                vedtakId = "7654321",
                meldekortId = "1234567",
                forventet = ForventetRespons(
                    status = HttpStatusCode.InternalServerError,
                    body = ForventetBody.Json(
                        """
                        {
                          "melding": "Noe gikk galt på serversiden",
                          "kode": "server_feil"
                        }
                        """.trimIndent(),
                    ),
                    contentType = ContentType.parse("application/json; charset=UTF-8"),
                ),
            )
        }
    }

    @Test
    fun `hent detaljer - mangler rolle - svarer 403`() {
        withTestApplicationContextInMemory { tac ->
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_VEDTAK)

            getDetaljer(
                token,
                vedtakId = "7654321",
                meldekortId = "1234567",
                forventet = ForventetRespons(
                    status = HttpStatusCode.Forbidden,
                    body = ForventetBody.Json(
                        """
                        {
                          "melding": "Mangler rollen LES_MELDEKORT. Har rollene: [LES_VEDTAK]",
                          "kode": "mangler_rolle"
                        }
                        """.trimIndent(),
                    ),
                    contentType = ContentType.parse("application/json; charset=UTF-8"),
                ),
            )
        }
    }

    private suspend fun ApplicationTestBuilder.getDetaljer(
        token: String,
        vedtakId: String?,
        meldekortId: String?,
        forventet: ForventetRespons,
    ): HttpResponse = defaultRequestWithAssertions(
        HttpMethod.Get,
        url {
            protocol = URLProtocol.HTTPS
            path("/arena/utbetalingshistorikk/detaljer")
            vedtakId?.let { parameters.append("vedtakId", it) }
            meldekortId?.let { parameters.append("meldekortId", it) }
        },
        jwt = token,
        forventet = forventet,
    )
}
