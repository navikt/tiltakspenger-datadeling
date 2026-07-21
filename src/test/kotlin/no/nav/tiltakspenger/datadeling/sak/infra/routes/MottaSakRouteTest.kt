package no.nav.tiltakspenger.datadeling.sak.infra.routes

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
import io.ktor.http.contentType
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
import no.nav.tiltakspenger.datadeling.Systembruker
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.Systembrukerroller
import no.nav.tiltakspenger.datadeling.infra.jacksonSerialization
import no.nav.tiltakspenger.datadeling.infra.routes.mottaRoutes
import no.nav.tiltakspenger.datadeling.infra.setupAuthentication
import no.nav.tiltakspenger.datadeling.sak.MottaSakService
import no.nav.tiltakspenger.datadeling.sak.Sak
import no.nav.tiltakspenger.datadeling.sak.SakRepo
import no.nav.tiltakspenger.datadeling.testutils.TestApplicationContext
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.Saksnummer
import no.nav.tiltakspenger.libs.common.TikkendeKlokke
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.LocalDateTime

/**
 * Route- og JSON-tester for `POST /sak`.
 * Sjekker status, content-type og payload-form for de fire utfallene rollesjekk + lagring kan ende i.
 */
class MottaSakRouteTest {

    private val gyldigBody = """
        {
            "id": "sak_01ARZ3NDEKTSV4RRFFQ69G5FAV",
            "fnr": "12345678910",
            "saksnummer": "202401011001",
            "opprettet": "2024-01-15T10:30:00"
        }
    """.trimIndent()

    @Test
    fun `motta sak - gyldig request - lagrer og svarer 200`() {
        with(TestApplicationContext()) {
            val tac = this
            val sakRepo = mockk<SakRepo>()
            val lagret = slot<Sak>()
            every { sakRepo.lagre(capture(lagret)) } just Runs

            val token = leggTilSystembruker(listOf(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER))

            testApplication {
                konfigurerMottaSak(tac, sakRepo)
                postSak(token, gyldigBody, ForventetRespons(status = HttpStatusCode.OK, body = ForventetBody.Tom))
            }

            verify(exactly = 1) { sakRepo.lagre(any()) }
            lagret.captured shouldBe Sak(
                id = SakId.fromString("sak_01ARZ3NDEKTSV4RRFFQ69G5FAV"),
                fnr = Fnr.fromString("12345678910"),
                saksnummer = Saksnummer("202401011001"),
                opprettet = LocalDateTime.parse("2024-01-15T10:30:00"),
            )
        }
    }

    @Test
    fun `motta sak - mangler rolle - svarer 403 med strukturert feilmelding`() {
        with(TestApplicationContext()) {
            val tac = this
            val sakRepo = mockk<SakRepo>()
            val token = leggTilSystembruker(listOf(Systembrukerrolle.LES_VEDTAK))

            testApplication {
                konfigurerMottaSak(tac, sakRepo)
                postSak(
                    token,
                    gyldigBody,
                    ForventetRespons(
                        status = HttpStatusCode.Forbidden,
                        body = ForventetBody.Json(
                            """
                            {
                              "melding": "Mangler rollen LAGRE_TILTAKSPENGER_HENDELSER. Har rollene: [LES_VEDTAK]",
                              "kode": "mangler_rolle"
                            }
                            """.trimIndent(),
                        ),
                        contentType = ContentType.parse("application/json; charset=UTF-8"),
                    ),
                )
            }

            verify(exactly = 0) { sakRepo.lagre(any()) }
        }
    }

    @Test
    fun `motta sak - repo kaster - svarer 500 med strukturert feilmelding`() {
        with(TestApplicationContext()) {
            val tac = this
            val sakRepo = mockk<SakRepo>()
            every { sakRepo.lagre(any()) } throws RuntimeException("databasen er nede")

            val token = leggTilSystembruker(listOf(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER))

            testApplication {
                konfigurerMottaSak(tac, sakRepo)
                postSak(
                    token,
                    gyldigBody,
                    ForventetRespons(
                        status = HttpStatusCode.InternalServerError,
                        body = ForventetBody.Json(
                            """
                            {
                              "melding": "Sak kunne ikke lagres siden en ukjent feil oppstod",
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
    fun `motta sak - misformet json - svarer 400`() {
        with(TestApplicationContext()) {
            val tac = this
            val sakRepo = mockk<SakRepo>()
            val token = leggTilSystembruker(listOf(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER))

            testApplication {
                konfigurerMottaSak(tac, sakRepo)
                postSak(token, """{"id":"bare-tull"}""", ForventetRespons(status = HttpStatusCode.BadRequest))
            }

            verify(exactly = 0) { sakRepo.lagre(any()) }
        }
    }

    private fun TestApplicationContext.leggTilSystembruker(roller: List<Systembrukerrolle>): String {
        val rolleNavn = roller.map { rolle ->
            when (rolle) {
                Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER -> "lagre-tiltakspenger-hendelser"
                Systembrukerrolle.LES_VEDTAK -> "les-vedtak"
                Systembrukerrolle.LES_BEHANDLING -> "les-behandling"
                Systembrukerrolle.LES_MELDEKORT -> "les-meldekort"
            }
        }
        val systembruker = Systembruker(
            roller = Systembrukerroller(roller),
            klientnavn = "klientnavn",
            klientId = "id",
        )
        val token = jwtGenerator.createJwtForSystembruker(roles = rolleNavn)
        texasClient.leggTilSystembruker(token, systembruker)
        return token
    }

    private fun ApplicationTestBuilder.konfigurerMottaSak(
        tac: TestApplicationContext,
        sakRepo: SakRepo,
    ) {
        application {
            jacksonSerialization()
            setupAuthentication(tac.texasClient)
            routing {
                authenticate(IdentityProvider.AZUREAD.value) {
                    mottaRoutes(
                        mottaNyttVedtakService = mockk(relaxed = true),
                        mottaNyBehanlingService = mockk(relaxed = true),
                        clock = TikkendeKlokke(),
                        meldeperiodeRepo = mockk(relaxed = true),
                        godkjentMeldekortbehandlingRepo = mockk(relaxed = true),
                        mottaSakService = MottaSakService(sakRepo),
                    )
                }
            }
        }
    }

    private suspend fun ApplicationTestBuilder.postSak(
        token: String,
        body: String,
        forventet: ForventetRespons,
    ): HttpResponse = defaultRequestWithAssertions(
        HttpMethod.Post,
        url {
            protocol = URLProtocol.HTTPS
            path("sak")
        },
        jwt = token,
        forventet = forventet,
    ) {
        setBody(body)
    }
}
