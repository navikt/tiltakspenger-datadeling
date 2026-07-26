package no.nav.tiltakspenger.datadeling.meldekort.infra.routes

import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.path
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.ktor.server.util.url
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.infra.configureExceptions
import no.nav.tiltakspenger.datadeling.infra.jacksonSerialization
import no.nav.tiltakspenger.datadeling.infra.setupAuthentication
import no.nav.tiltakspenger.datadeling.meldekort.Meldeperiode
import no.nav.tiltakspenger.datadeling.meldekort.MeldeperiodeRepo
import no.nav.tiltakspenger.datadeling.testutils.TestApplicationContext
import no.nav.tiltakspenger.datadeling.testutils.leggTilSystembruker
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.meldekort.MeldeperiodeId
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Route- og JSON-tester for `POST /meldeperioder`, som tiltakspenger-saksbehandling-api kaller når meldeperiodene for en sak endrer seg.
 */
class MottaMeldeperioderRouteTest {

    private val sakId = "sak_01ARZ3NDEKTSV4RRFFQ69G5FAV"
    private val meldeperiodeId = "meldeperiode_01ARZ3NDEKTSV4RRFFQ69G5FAV"

    private fun body(meldeperioder: String = enMeldeperiode) = """
        {
            "sakId": "$sakId",
            "meldeperioder": $meldeperioder
        }
    """.trimIndent()

    private val enMeldeperiode = """
        [
            {
                "id": "$meldeperiodeId",
                "kjedeId": "2024-01-01/2024-01-14",
                "opprettet": "2024-01-01T08:00:00",
                "fraOgMed": "2024-01-01",
                "tilOgMed": "2024-01-14",
                "antallDagerForPeriode": 10,
                "girRett": {"2024-01-01": true, "2024-01-02": false}
            }
        ]
    """.trimIndent()

    @Test
    fun `motta meldeperioder - gyldig request - lagrer og svarer 200`() {
        with(TestApplicationContext()) {
            val tac = this
            val repo = mockk<MeldeperiodeRepo>()
            val lagret = slot<List<Meldeperiode>>()
            every { repo.lagre(capture(lagret)) } just Runs

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, repo)
                postMeldeperioder(token, body(), ForventetRespons(HttpStatusCode.OK, ForventetBody.Tom))
            }

            lagret.captured shouldBe listOf(
                Meldeperiode(
                    id = MeldeperiodeId.fromString(meldeperiodeId),
                    kjedeId = "2024-01-01/2024-01-14",
                    sakId = SakId.fromString(sakId),
                    opprettet = LocalDateTime.parse("2024-01-01T08:00:00"),
                    fraOgMed = LocalDate.of(2024, 1, 1),
                    tilOgMed = LocalDate.of(2024, 1, 14),
                    maksAntallDagerForPeriode = 10,
                    girRett = mapOf(
                        LocalDate.of(2024, 1, 1) to true,
                        LocalDate.of(2024, 1, 2) to false,
                    ),
                ),
            )
        }
    }

    @Test
    fun `motta meldeperioder - tom liste - svarer 200 uten a lagre`() {
        with(TestApplicationContext()) {
            val tac = this
            val repo = mockk<MeldeperiodeRepo>()
            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, repo)
                postMeldeperioder(token, body(meldeperioder = "[]"), ForventetRespons(HttpStatusCode.OK, ForventetBody.Tom))
            }

            verify(exactly = 0) { repo.lagre(any()) }
        }
    }

    @Test
    fun `motta meldeperioder - mangler rolle - svarer 403 og lagrer ingenting`() {
        with(TestApplicationContext()) {
            val tac = this
            val repo = mockk<MeldeperiodeRepo>()
            val token = leggTilSystembruker(Systembrukerrolle.LES_MELDEKORT)

            testApplication {
                konfigurer(tac, repo)
                postMeldeperioder(
                    token,
                    body(),
                    ForventetRespons(
                        status = HttpStatusCode.Forbidden,
                        body = ForventetBody.Json(
                            """
                            {
                              "melding": "Mangler rollen LAGRE_TILTAKSPENGER_HENDELSER. Har rollene: [LES_MELDEKORT]",
                              "kode": "mangler_rolle"
                            }
                            """.trimIndent(),
                        ),
                        contentType = ContentType.parse("application/json; charset=UTF-8"),
                    ),
                )
            }

            verify(exactly = 0) { repo.lagre(any()) }
        }
    }

    @Test
    fun `motta meldeperioder - repo kaster - svarer 500 ukjent_feil`() {
        with(TestApplicationContext()) {
            val tac = this
            val repo = mockk<MeldeperiodeRepo>()
            every { repo.lagre(any()) } throws RuntimeException("databasen er nede")

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, repo)
                postMeldeperioder(
                    token,
                    body(),
                    ForventetRespons(
                        status = HttpStatusCode.InternalServerError,
                        body = ForventetBody.Json(
                            """
                            {
                              "melding": "Meldeperioder kunne ikke lagres siden en ukjent feil oppstod",
                              "kode": "ukjent_feil"
                            }
                            """.trimIndent(),
                        ),
                        contentType = ContentType.parse("application/json; charset=UTF-8"),
                    ),
                )
            }
        }
    }

    @Test
    fun `motta meldeperioder - misformet json - svarer 400`() {
        with(TestApplicationContext()) {
            val tac = this
            val repo = mockk<MeldeperiodeRepo>()
            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, repo)
                postMeldeperioder(token, """{"sakId":"bare-tull"}""", ForventetRespons(HttpStatusCode.BadRequest))
            }

            verify(exactly = 0) { repo.lagre(any()) }
        }
    }

    private fun ApplicationTestBuilder.konfigurer(
        tac: TestApplicationContext,
        repo: MeldeperiodeRepo,
    ) {
        application {
            jacksonSerialization()
            setupAuthentication(tac.texasClient)
            configureExceptions()
            routing {
                authenticate(IdentityProvider.AZUREAD.value) {
                    mottaMeldeperioderRoute(meldeperiodeRepo = repo)
                }
            }
        }
    }

    private suspend fun ApplicationTestBuilder.postMeldeperioder(
        token: String,
        body: String,
        forventet: ForventetRespons,
    ): HttpResponse = defaultRequestWithAssertions(
        HttpMethod.Post,
        url {
            protocol = URLProtocol.HTTPS
            path("meldeperioder")
        },
        jwt = token,
        forventet = forventet,
    ) {
        setBody(body)
    }
}
