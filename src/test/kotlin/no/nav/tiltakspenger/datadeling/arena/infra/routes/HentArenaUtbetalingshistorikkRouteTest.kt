package no.nav.tiltakspenger.datadeling.arena.infra.routes

import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.util.url
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.testdata.ArenaMother
import no.nav.tiltakspenger.datadeling.testutils.leggTilSystembruker
import no.nav.tiltakspenger.datadeling.testutils.withTestApplicationContextInMemory
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
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
                    status = HttpStatusCode.OK,
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
                    contentType = ContentType.parse("application/json"),
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
                    status = HttpStatusCode.OK,
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
                    contentType = ContentType.parse("application/json"),
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
                    status = HttpStatusCode.OK,
                    body = ForventetBody.Json("[]"),
                    contentType = ContentType.parse("application/json"),
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
    fun `hent utbetalingshistorikk - mangler rolle - svarer 403`() {
        withTestApplicationContextInMemory { tac ->
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_VEDTAK)

            postUtbetalingshistorikk(
                token,
                gyldigBody,
                ForventetRespons(
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

    @Test
    fun `hent utbetalingshistorikk - ugyldig datoformat - svarer 400`() {
        withTestApplicationContextInMemory { tac ->
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_MELDEKORT)

            postUtbetalingshistorikk(
                token,
                """{"ident": "12345678910", "fom": "01.01.2024", "tom": "2024-01-31"}""",
                ForventetRespons(
                    status = HttpStatusCode.BadRequest,
                    body = ForventetBody.Json(
                        """{"feilmelding": "Ugyldig datoformat i felt 'fom'. Forventet format er yyyy-MM-dd."}""",
                    ),
                    contentType = ContentType.parse("application/json"),
                ),
            )
        }
    }

    private suspend fun ApplicationTestBuilder.postUtbetalingshistorikk(
        token: String,
        body: String,
        forventet: ForventetRespons,
    ): HttpResponse = defaultRequestWithAssertions(
        HttpMethod.Post,
        url {
            protocol = URLProtocol.HTTPS
            path("/arena/utbetalingshistorikk")
        },
        jwt = token,
        forventet = forventet,
    ) {
        setBody(body)
    }
}
