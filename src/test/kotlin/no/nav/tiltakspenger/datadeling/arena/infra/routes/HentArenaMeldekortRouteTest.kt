package no.nav.tiltakspenger.datadeling.arena.infra.routes

import io.kotest.assertions.json.ArrayOrder
import io.kotest.assertions.json.FieldComparison
import io.kotest.assertions.json.shouldEqualJson
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
 * Route- og JSON-tester for `POST /arena/meldekort`, som NKS/Salesforce kaller via saas-proxy.
 * Kjører gjennom hele oppsettet (modul, route, service og responsmapping) med Arena-klienten byttet ut med en fake.
 */
class HentArenaMeldekortRouteTest {

    private val gyldigBody = """
        {
            "ident": "12345678910",
            "fom": "2024-01-01",
            "tom": "2024-01-31"
        }
    """.trimIndent()

    @Test
    fun `hent arena-meldekort - har meldekort - returnerer hele kontrakten`() {
        withTestApplicationContextInMemory { tac ->
            tac.arenaClient.meldekort = listOf(ArenaMother.meldekort())
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_MELDEKORT)

            postArenaMeldekort(
                token,
                gyldigBody,
                ForventetRespons(
                    status = 200,
                    body = ForventetBody.Json(
                        // language=JSON
                        """
                        [
                          {
                            "meldekortId": "1234567",
                            "mottatt": "2024-01-15",
                            "arbeidet": true,
                            "kurs": false,
                            "ferie": false,
                            "syk": false,
                            "annetFravaer": false,
                            "registrert": "2024-01-15T10:00:00",
                            "sistEndret": "2024-01-16T10:00:00",
                            "type": "ELEKTRONISK",
                            "status": "FERDIG",
                            "statusDato": "2024-01-16",
                            "meldegruppe": "INDIV",
                            "aar": 2024,
                            "totaltArbeidetTimer": 15,
                            "periode": {
                              "aar": 2024,
                              "periodekode": 202401,
                              "ukenrUke1": 1,
                              "ukenrUke2": 2,
                              "fraOgMed": "2024-01-01",
                              "tilOgMed": "2024-01-14"
                            },
                            "dager": [
                              {
                                "ukeNr": 1,
                                "dagNr": 1,
                                "arbeidsdag": true,
                                "ferie": false,
                                "kurs": false,
                                "syk": false,
                                "annetFravaer": false,
                                "registrertAv": "BRUKER",
                                "registrert": "2024-01-15T10:00:00",
                                "arbeidetTimer": 7
                              }
                            ],
                            "fortsattArbeidsoker": true
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
    fun `hent arena-meldekort - nullbare felter er null - serialiseres som null`() {
        withTestApplicationContextInMemory { tac ->
            tac.arenaClient.meldekort = listOf(
                ArenaMother.meldekort(
                    mottatt = null,
                    ferie = null,
                    dager = listOf(ArenaMother.meldekortdag(ferie = null)),
                ),
            )
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_MELDEKORT)

            val respons = postArenaMeldekort(token, gyldigBody, ForventetRespons(status = 200))

            respons.body.shouldEqualJson {
                // Feltene under er de eneste som skal endre seg fra hovedcaset, så resten sjekkes ikke her.
                arrayOrder = ArrayOrder.Strict
                fieldComparison = FieldComparison.Lenient
                // language=JSON
                """
                [
                  {
                    "mottatt": null,
                    "ferie": null,
                    "dager": [ { "ferie": null } ]
                  }
                ]
                """.trimIndent()
            }
        }
    }

    @Test
    fun `hent arena-meldekort - ingen meldekort - returnerer tom liste`() {
        withTestApplicationContextInMemory { tac ->
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_MELDEKORT)

            postArenaMeldekort(
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
    fun `hent arena-meldekort - arena feiler - svarer 500 server_feil`() {
        withTestApplicationContextInMemory { tac ->
            tac.arenaClient.feiler = true
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_MELDEKORT)

            postArenaMeldekort(
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
    fun `hent arena-meldekort - mangler rolle - svarer 403`() {
        withTestApplicationContextInMemory { tac ->
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_VEDTAK)

            postArenaMeldekort(
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
    fun `hent arena-meldekort - ugyldig ident - svarer 400`() {
        withTestApplicationContextInMemory { tac ->
            val token = tac.leggTilSystembruker(Systembrukerrolle.LES_MELDEKORT)

            postArenaMeldekort(
                token,
                """{"ident": "ugyldig", "fom": "2024-01-01", "tom": "2024-01-31"}""",
                ForventetRespons(
                    status = 400,
                    body = ForventetBody.Json("""{"feilmelding": "Ugyldig ident. Må bestå av 11 siffer."}"""),
                    contentType = "application/json",
                ),
            )
        }
    }

    private suspend fun ApplicationTestBuilder.postArenaMeldekort(
        token: String,
        body: String,
        forventet: ForventetRespons,
    ): TestRespons = defaultRequestWithAssertions(
        HttpMethod.POST,
        "/arena/meldekort",
        jwt = token,
        forventet = forventet,
        body = body,
    )
}
