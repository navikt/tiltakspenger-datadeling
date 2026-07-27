package no.nav.tiltakspenger.datadeling.arena.infra.routes

import io.kotest.matchers.shouldBe
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.testdata.ArenaMother
import no.nav.tiltakspenger.datadeling.testutils.leggTilSystembruker
import no.nav.tiltakspenger.datadeling.testutils.withTestApplicationContextInMemory
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.TestRespons
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
                    status = 200,
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
                    contentType = "application/json",
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
                    status = 200,
                    body = ForventetBody.Json("""{"vedtakfakta": null, "anmerkninger": []}"""),
                    contentType = "application/json",
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
                forventet = ForventetRespons(status = 200),
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
                    status = 500,
                    body = ForventetBody.Json(
                        """
                        {
                          "melding": "Noe gikk galt på serversiden",
                          "kode": "server_feil"
                        }
                        """.trimIndent(),
                    ),
                    contentType = "application/json; charset=UTF-8",
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
                    status = 403,
                    body = ForventetBody.Json(
                        """
                        {
                          "melding": "Mangler rollen LES_MELDEKORT. Har rollene: [LES_VEDTAK]",
                          "kode": "mangler_rolle"
                        }
                        """.trimIndent(),
                    ),
                    contentType = "application/json; charset=UTF-8",
                ),
            )
        }
    }

    private suspend fun ApplicationTestBuilder.getDetaljer(
        token: String,
        vedtakId: String?,
        meldekortId: String?,
        forventet: ForventetRespons,
    ): TestRespons = defaultRequestWithAssertions(
        HttpMethod.GET,
        listOfNotNull(
            vedtakId?.let { "vedtakId=$it" },
            meldekortId?.let { "meldekortId=$it" },
        ).joinToString("&").let { query ->
            "/arena/utbetalingshistorikk/detaljer" + if (query.isEmpty()) "" else "?$query"
        },
        jwt = token,
        forventet = forventet,
    )
}
