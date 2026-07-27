package no.nav.tiltakspenger.datadeling.meldekort.infra.routes

import arrow.core.nonEmptyListOf
import io.kotest.matchers.shouldBe
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
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
import no.nav.tiltakspenger.datadeling.meldekort.GodkjentMeldekortbehandling
import no.nav.tiltakspenger.datadeling.meldekort.GodkjentMeldekortbehandlingRepo
import no.nav.tiltakspenger.datadeling.testutils.TestApplicationContext
import no.nav.tiltakspenger.datadeling.testutils.leggTilSystembruker
import no.nav.tiltakspenger.libs.common.MeldekortId
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.TestRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Route- og JSON-tester for `POST /meldekort`, som tiltakspenger-saksbehandling-api kaller for hver godkjente meldekortbehandling.
 *
 * Statusene og reduksjonene mappes med `when` over strenger og ikke over en enum, så en ny verdi fra saksbehandling-api gir en exception i stedet for en kompileringsfeil.
 * Derfor testes hver enkelt verdi, og at ukjente verdier faktisk avvises.
 */
class MottaGodkjentMeldekortbehandlingRouteTest {

    private val sakId = "sak_01ARZ3NDEKTSV4RRFFQ69G5FAV"
    private val meldekortbehandlingId = "meldekort_01ARZ3NDEKTSV4RRFFQ69G5FAV"

    private fun body(
        status: String = "DELTATT_UTEN_LONN_I_TILTAKET",
        reduksjon: String = "INGEN_REDUKSJON",
        korrigert: Boolean = false,
        totalDifferanse: String = "null",
    ) = """
        {
            "meldekortbehandlingId": "$meldekortbehandlingId",
            "sakId": "$sakId",
            "meldeperioder": [
                {
                    "kjedeId": "2024-01-01/2024-01-14",
                    "meldeperiodeId": "meldeperiode_01ARZ3NDEKTSV4RRFFQ69G5FAV",
                    "korrigert": $korrigert,
                    "meldekortdager": [
                        {"dato": "2024-01-01", "status": "$status", "reduksjon": "$reduksjon"}
                    ],
                    "totaltBelop": 4560,
                    "totalDifferanse": $totalDifferanse,
                    "fraOgMed": "2024-01-01",
                    "tilOgMed": "2024-01-14",
                    "mottattTidspunkt": "2024-01-15T10:00:00"
                }
            ],
            "vedtattTidspunkt": "2024-01-16T10:00:00",
            "behandletAutomatisk": false,
            "fraOgMed": "2024-01-01",
            "tilOgMed": "2024-01-14",
            "journalpostId": "jpid",
            "totaltBelop": 4560,
            "totalDifferanse": $totalDifferanse,
            "barnetillegg": true,
            "opprettet": "2024-01-15T10:00:00",
            "sistEndret": "2024-01-16T10:00:00"
        }
    """.trimIndent()

    @Test
    fun `motta meldekort - gyldig request - lagrer og svarer 200`() {
        with(TestApplicationContext()) {
            val tac = this
            val repo = mockk<GodkjentMeldekortbehandlingRepo>()
            val lagret = slot<GodkjentMeldekortbehandling>()
            every { repo.lagre(capture(lagret)) } just Runs

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, repo)
                postMeldekort(token, body(), ForventetRespons(200, ForventetBody.Tom))
            }

            lagret.captured shouldBe GodkjentMeldekortbehandling(
                meldekortbehandlingId = MeldekortId.fromString(meldekortbehandlingId),
                sakId = SakId.fromString(sakId),
                meldeperioder = nonEmptyListOf(
                    GodkjentMeldekortbehandling.Meldeperiode(
                        kjedeId = "2024-01-01/2024-01-14",
                        meldeperiodeId = "meldeperiode_01ARZ3NDEKTSV4RRFFQ69G5FAV",
                        korrigert = false,
                        meldekortdager = nonEmptyListOf(
                            GodkjentMeldekortbehandling.MeldekortDag(
                                dato = LocalDate.of(2024, 1, 1),
                                status = GodkjentMeldekortbehandling.MeldekortDag.MeldekortDagStatus.DELTATT_UTEN_LONN_I_TILTAKET,
                                reduksjon = GodkjentMeldekortbehandling.MeldekortDag.Reduksjon.INGEN_REDUKSJON,
                            ),
                        ),
                        totaltBelop = 4560,
                        totalDifferanse = null,
                        fraOgMed = LocalDate.of(2024, 1, 1),
                        tilOgMed = LocalDate.of(2024, 1, 14),
                        mottattTidspunkt = LocalDateTime.parse("2024-01-15T10:00:00"),
                    ),
                ),
                vedtattTidspunkt = LocalDateTime.parse("2024-01-16T10:00:00"),
                behandletAutomatisk = false,
                fraOgMed = LocalDate.of(2024, 1, 1),
                tilOgMed = LocalDate.of(2024, 1, 14),
                journalpostId = "jpid",
                totaltBelop = 4560,
                totalDifferanse = null,
                barnetillegg = true,
                opprettet = LocalDateTime.parse("2024-01-15T10:00:00"),
                sistEndret = LocalDateTime.parse("2024-01-16T10:00:00"),
            )
        }
    }

    @Test
    fun `motta meldekort - korrigering med differanse - mapper korrigeringsfeltene`() {
        with(TestApplicationContext()) {
            val tac = this
            val repo = mockk<GodkjentMeldekortbehandlingRepo>()
            val lagret = slot<GodkjentMeldekortbehandling>()
            every { repo.lagre(capture(lagret)) } just Runs

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, repo)
                postMeldekort(
                    token,
                    body(korrigert = true, totalDifferanse = "-500"),
                    ForventetRespons(200, ForventetBody.Tom),
                )
            }

            lagret.captured.totalDifferanse shouldBe -500
            lagret.captured.meldeperioder.single().korrigert shouldBe true
            lagret.captured.meldeperioder.single().totalDifferanse shouldBe -500
        }
    }

    @ParameterizedTest(name = "status {0} mapper til {1}")
    @CsvSource(
        "DELTATT_UTEN_LONN_I_TILTAKET, DELTATT_UTEN_LONN_I_TILTAKET",
        "DELTATT_MED_LONN_I_TILTAKET, DELTATT_MED_LONN_I_TILTAKET",
        "FRAVAER_SYK, FRAVAER_SYK",
        "FRAVAER_SYKT_BARN, FRAVAER_SYKT_BARN",
        "FRAVAER_GODKJENT_AV_NAV, FRAVAER_GODKJENT_AV_NAV",
        "FRAVAER_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU, FRAVAER_STERKE_VELFERDSGRUNNER_ELLER_JOBBINTERVJU",
        "FRAVAER_ANNET, FRAVAER_ANNET",
        "IKKE_BESVART, IKKE_BESVART",
        "IKKE_TILTAKSDAG, IKKE_TILTAKSDAG",
        "IKKE_RETT_TIL_TILTAKSPENGER, IKKE_RETT_TIL_TILTAKSPENGER",
    )
    fun `motta meldekort - hver meldekortdagstatus mapper til riktig domeneverdi`(fraJson: String, domene: String) {
        with(TestApplicationContext()) {
            val tac = this
            val repo = mockk<GodkjentMeldekortbehandlingRepo>()
            val lagret = slot<GodkjentMeldekortbehandling>()
            every { repo.lagre(capture(lagret)) } just Runs

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, repo)
                postMeldekort(token, body(status = fraJson), ForventetRespons(200, ForventetBody.Tom))
            }

            lagret.captured.meldeperioder.single().meldekortdager.single().status shouldBe
                GodkjentMeldekortbehandling.MeldekortDag.MeldekortDagStatus.valueOf(domene)
        }
    }

    @ParameterizedTest(name = "reduksjon {0} mapper til {1}")
    @CsvSource(
        "INGEN_REDUKSJON, INGEN_REDUKSJON",
        "UKJENT, UKJENT",
        "YTELSEN_FALLER_BORT, YTELSEN_FALLER_BORT",
    )
    fun `motta meldekort - hver reduksjon mapper til riktig domeneverdi`(fraJson: String, domene: String) {
        with(TestApplicationContext()) {
            val tac = this
            val repo = mockk<GodkjentMeldekortbehandlingRepo>()
            val lagret = slot<GodkjentMeldekortbehandling>()
            every { repo.lagre(capture(lagret)) } just Runs

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, repo)
                postMeldekort(token, body(reduksjon = fraJson), ForventetRespons(200, ForventetBody.Tom))
            }

            lagret.captured.meldeperioder.single().meldekortdager.single().reduksjon shouldBe
                GodkjentMeldekortbehandling.MeldekortDag.Reduksjon.valueOf(domene)
        }
    }

    @Test
    fun `motta meldekort - ukjent meldekortdagstatus - avvises med 500 og lagres ikke`() {
        with(TestApplicationContext()) {
            val tac = this
            val repo = mockk<GodkjentMeldekortbehandlingRepo>()
            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, repo)
                postMeldekort(
                    token,
                    body(status = "EN_HELT_NY_STATUS"),
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

            verify(exactly = 0) { repo.lagre(any()) }
        }
    }

    @Test
    fun `motta meldekort - ukjent reduksjon - avvises med 500 og lagres ikke`() {
        with(TestApplicationContext()) {
            val tac = this
            val repo = mockk<GodkjentMeldekortbehandlingRepo>()
            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, repo)
                postMeldekort(
                    token,
                    body(reduksjon = "EN_HELT_NY_REDUKSJON"),
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

            verify(exactly = 0) { repo.lagre(any()) }
        }
    }

    @Test
    fun `motta meldekort - mangler rolle - svarer 403 og lagrer ingenting`() {
        with(TestApplicationContext()) {
            val tac = this
            val repo = mockk<GodkjentMeldekortbehandlingRepo>()
            val token = leggTilSystembruker(Systembrukerrolle.LES_MELDEKORT)

            testApplication {
                konfigurer(tac, repo)
                postMeldekort(
                    token,
                    body(),
                    ForventetRespons(
                        status = 403,
                        body = ForventetBody.Json(
                            """
                            {
                              "melding": "Mangler rollen LAGRE_TILTAKSPENGER_HENDELSER. Har rollene: [LES_MELDEKORT]",
                              "kode": "mangler_rolle"
                            }
                            """.trimIndent(),
                        ),
                        contentType = "application/json; charset=UTF-8",
                    ),
                )
            }

            verify(exactly = 0) { repo.lagre(any()) }
        }
    }

    @Test
    fun `motta meldekort - repo kaster - svarer 500 ukjent_feil`() {
        with(TestApplicationContext()) {
            val tac = this
            val repo = mockk<GodkjentMeldekortbehandlingRepo>()
            every { repo.lagre(any()) } throws RuntimeException("databasen er nede")

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, repo)
                postMeldekort(
                    token,
                    body(),
                    ForventetRespons(
                        status = 500,
                        body = ForventetBody.Json(
                            """
                            {
                              "melding": "Meldekort kunne ikke lagres siden en ukjent feil oppstod",
                              "kode": "ukjent_feil"
                            }
                            """.trimIndent(),
                        ),
                        contentType = "application/json; charset=UTF-8",
                    ),
                )
            }
        }
    }

    @Test
    fun `motta meldekort - misformet json - svarer 400`() {
        with(TestApplicationContext()) {
            val tac = this
            val repo = mockk<GodkjentMeldekortbehandlingRepo>()
            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, repo)
                postMeldekort(token, """{"sakId":"bare-tull"}""", ForventetRespons(400))
            }

            verify(exactly = 0) { repo.lagre(any()) }
        }
    }

    private fun ApplicationTestBuilder.konfigurer(
        tac: TestApplicationContext,
        repo: GodkjentMeldekortbehandlingRepo,
    ) {
        application {
            jacksonSerialization()
            setupAuthentication(tac.texasClient)
            configureExceptions()
            routing {
                authenticate(IdentityProvider.AZUREAD.value) {
                    mottaGodkjentMeldekortbehandlingRoute(godkjentMeldekortbehandlingRepo = repo)
                }
            }
        }
    }

    private suspend fun ApplicationTestBuilder.postMeldekort(
        token: String,
        body: String,
        forventet: ForventetRespons,
    ): TestRespons = defaultRequestWithAssertions(
        HttpMethod.POST,
        "meldekort",
        jwt = token,
        forventet = forventet,
        body = body,
    )
}
