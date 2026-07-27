package no.nav.tiltakspenger.datadeling.arena.infra.routes

import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
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
 * Route- og JSON-tester for `POST /arena/utbetalingshistorikk`, som NKS/Salesforce kaller via saas-proxy.
 */
class HentArenaUtbetalingshistorikkRouteTest {

    private val gyldigBody = """
        {
            "ident": "12345678910",
            "fom": "2024-01-01",
            "tom": "2024-01-31"
        }
    """.trimIndent()

    @Test
    fun `hent utbetalingshistorikk - har utbetalinger - returnerer hele kontrakten`() {
        withTestApplicationContextInMemory { tac ->
            tac.arenaClient.utbetalingshistorikk = listOf(ArenaMother.utbetalingshistorikk())
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_MELDEKORT)

            postUtbetalingshistorikk(
                token,
                gyldigBody,
                ForventetRespons(
                    status = 200,
                    body = ForventetBody.Json(
                        // language=JSON
                        """
                        [
                          {
                            "meldekortId": 1234567,
                            "dato": "2024-01-20",
                            "transaksjonstype": "UTBETALING",
                            "sats": 285.0,
                            "status": "UTBETALT",
                            "vedtakId": 7654321,
                            "belop": 1995.0,
                            "fraOgMedDato": "2024-01-01",
                            "tilOgMedDato": "2024-01-14"
                          }
                        ]
                        """.trimIndent(),
                    ),
                    contentType = "application/json",
                ),
            )
        }
    }

    @Test
    fun `hent utbetalingshistorikk - uten meldekortId og vedtakId - serialiseres som null`() {
        withTestApplicationContextInMemory { tac ->
            tac.arenaClient.utbetalingshistorikk = listOf(
                ArenaMother.utbetalingshistorikk(meldekortId = null, vedtakId = null),
            )
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_MELDEKORT)

            postUtbetalingshistorikk(
                token,
                gyldigBody,
                ForventetRespons(
                    status = 200,
                    body = ForventetBody.Json(
                        // language=JSON
                        """
                        [
                          {
                            "meldekortId": null,
                            "dato": "2024-01-20",
                            "transaksjonstype": "UTBETALING",
                            "sats": 285.0,
                            "status": "UTBETALT",
                            "vedtakId": null,
                            "belop": 1995.0,
                            "fraOgMedDato": "2024-01-01",
                            "tilOgMedDato": "2024-01-14"
                          }
                        ]
                        """.trimIndent(),
                    ),
                    contentType = "application/json",
                ),
            )
        }
    }

    @Test
    fun `hent utbetalingshistorikk - ingen utbetalinger - returnerer tom liste`() {
        withTestApplicationContextInMemory { tac ->
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_MELDEKORT)

            postUtbetalingshistorikk(
                token,
                gyldigBody,
                ForventetRespons(
                    status = 200,
                    body = ForventetBody.Json("[]"),
                    contentType = "application/json",
                ),
            )
        }
    }

    @Test
    fun `hent utbetalingshistorikk - arena feiler - svarer 500 server_feil`() {
        withTestApplicationContextInMemory { tac ->
            tac.arenaClient.feiler = true
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_MELDEKORT)

            postUtbetalingshistorikk(
                token,
                gyldigBody,
                ForventetRespons(
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
    fun `hent utbetalingshistorikk - mangler rolle - svarer 403`() {
        withTestApplicationContextInMemory { tac ->
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_VEDTAK)

            postUtbetalingshistorikk(
                token,
                gyldigBody,
                ForventetRespons(
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

    @Test
    fun `hent utbetalingshistorikk - ugyldig datoformat - svarer 400`() {
        withTestApplicationContextInMemory { tac ->
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_MELDEKORT)

            postUtbetalingshistorikk(
                token,
                """{"ident": "12345678910", "fom": "01.01.2024", "tom": "2024-01-31"}""",
                ForventetRespons(
                    status = 400,
                    body = ForventetBody.Json(
                        """{"feilmelding": "Ugyldig datoformat i felt 'fom'. Forventet format er yyyy-MM-dd."}""",
                    ),
                    contentType = "application/json",
                ),
            )
        }
    }

    private suspend fun ApplicationTestBuilder.postUtbetalingshistorikk(
        token: String,
        body: String,
        forventet: ForventetRespons,
    ): TestRespons = defaultRequestWithAssertions(
        HttpMethod.POST,
        "/arena/utbetalingshistorikk",
        jwt = token,
        forventet = forventet,
        body = body,
    )
}
