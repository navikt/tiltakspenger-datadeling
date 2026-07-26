package no.nav.tiltakspenger.datadeling.behandling.infra.routes

import arrow.core.left
import arrow.core.right
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
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.behandling.KanIkkeMottaBehandling
import no.nav.tiltakspenger.datadeling.behandling.MottaNyBehandlingService
import no.nav.tiltakspenger.datadeling.behandling.MottattTiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.behandling.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.infra.jacksonSerialization
import no.nav.tiltakspenger.datadeling.infra.setupAuthentication
import no.nav.tiltakspenger.datadeling.testutils.TestApplicationContext
import no.nav.tiltakspenger.datadeling.testutils.leggTilSystembruker
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Route- og JSON-tester for `POST /behandling`, som tiltakspenger-saksbehandling-api kaller for hver behandling.
 * Dekker rollesjekk, de to feilutfallene fra servicen, og at hver status- og typeverdi i JSON-kontrakten mapper til riktig domeneverdi.
 */
class MottaNyBehandlingRouteTest {

    private val sakId = "sak_01ARZ3NDEKTSV4RRFFQ69G5FAV"

    private fun body(
        behandlingStatus: String = "KLAR_TIL_BEHANDLING",
        behandlingstype: String = "SOKNADSBEHANDLING",
        fraOgMed: String? = "2024-01-01",
        tilOgMed: String? = "2024-12-31",
    ) = """
        {
            "behandlingId": "behandlingId",
            "sakId": "$sakId",
            "fraOgMed": ${fraOgMed?.let { "\"$it\"" }},
            "tilOgMed": ${tilOgMed?.let { "\"$it\"" }},
            "behandlingStatus": "$behandlingStatus",
            "saksbehandler": "Z12345",
            "beslutter": "Z54321",
            "iverksattTidspunkt": "2024-02-01T12:00:00",
            "opprettetTidspunktSaksbehandlingApi": "2024-01-01T08:00:00",
            "behandlingstype": "$behandlingstype",
            "sistEndret": "2024-02-01T12:00:00"
        }
    """.trimIndent()

    @Test
    fun `motta behandling - gyldig request - lagrer og svarer 200`() {
        with(TestApplicationContext()) {
            val tac = this
            val service = mockk<MottaNyBehandlingService>()
            val mottatt = slot<MottattTiltakspengerBehandling>()
            every { service.motta(capture(mottatt)) } returns Unit.right()

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, service)
                postBehandling(token, body(), ForventetRespons(HttpStatusCode.OK, ForventetBody.Tom))
            }

            mottatt.captured shouldBe MottattTiltakspengerBehandling(
                behandlingId = "behandlingId",
                sakId = SakId.fromString(sakId),
                periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)),
                behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.KLAR_TIL_BEHANDLING,
                saksbehandler = "Z12345",
                beslutter = "Z54321",
                iverksattTidspunkt = LocalDateTime.parse("2024-02-01T12:00:00"),
                opprettetTidspunktSaksbehandlingApi = LocalDateTime.parse("2024-01-01T08:00:00"),
                mottattTidspunktDatadeling = nå(fixedClock),
                behandlingstype = TiltakspengerBehandling.Behandlingstype.SOKNADSBEHANDLING,
                sistEndret = LocalDateTime.parse("2024-02-01T12:00:00"),
            )
        }
    }

    @Test
    fun `motta behandling - uten fraOgMed og tilOgMed - lagres uten periode`() {
        with(TestApplicationContext()) {
            val tac = this
            val service = mockk<MottaNyBehandlingService>()
            val mottatt = slot<MottattTiltakspengerBehandling>()
            every { service.motta(capture(mottatt)) } returns Unit.right()

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, service)
                postBehandling(
                    token,
                    body(fraOgMed = null, tilOgMed = null),
                    ForventetRespons(HttpStatusCode.OK, ForventetBody.Tom),
                )
            }

            mottatt.captured.periode shouldBe null
        }
    }

    @ParameterizedTest(name = "behandlingStatus {0} mapper til {1}")
    @CsvSource(
        "UNDER_AUTOMATISK_BEHANDLING, UNDER_AUTOMATISK_BEHANDLING",
        "KLAR_TIL_BEHANDLING, KLAR_TIL_BEHANDLING",
        "UNDER_BEHANDLING, UNDER_BEHANDLING",
        "KLAR_TIL_BESLUTNING, KLAR_TIL_BESLUTNING",
        "UNDER_BESLUTNING, UNDER_BESLUTNING",
        "VEDTATT, VEDTATT",
        "AVBRUTT, AVBRUTT",
        "GODKJENT, GODKJENT",
        "AUTOMATISK_BEHANDLET, AUTOMATISK_BEHANDLET",
        "IKKE_RETT_TIL_TILTAKSPENGER, IKKE_RETT_TIL_TILTAKSPENGER",
    )
    fun `motta behandling - hver behandlingStatus mapper til riktig domeneverdi`(fraJson: String, domene: String) {
        with(TestApplicationContext()) {
            val tac = this
            val service = mockk<MottaNyBehandlingService>()
            val mottatt = slot<MottattTiltakspengerBehandling>()
            every { service.motta(capture(mottatt)) } returns Unit.right()

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, service)
                postBehandling(
                    token,
                    body(behandlingStatus = fraJson),
                    ForventetRespons(HttpStatusCode.OK, ForventetBody.Tom),
                )
            }

            mottatt.captured.behandlingStatus shouldBe TiltakspengerBehandling.Behandlingsstatus.valueOf(domene)
        }
    }

    @ParameterizedTest(name = "behandlingstype {0} mapper til {1}")
    @CsvSource(
        "SOKNADSBEHANDLING, SOKNADSBEHANDLING",
        "REVURDERING, REVURDERING",
        "MELDEKORTBEHANDLING, MELDEKORTBEHANDLING",
    )
    fun `motta behandling - hver behandlingstype mapper til riktig domeneverdi`(fraJson: String, domene: String) {
        with(TestApplicationContext()) {
            val tac = this
            val service = mockk<MottaNyBehandlingService>()
            val mottatt = slot<MottattTiltakspengerBehandling>()
            every { service.motta(capture(mottatt)) } returns Unit.right()

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, service)
                postBehandling(
                    token,
                    body(behandlingstype = fraJson),
                    ForventetRespons(HttpStatusCode.OK, ForventetBody.Tom),
                )
            }

            mottatt.captured.behandlingstype shouldBe TiltakspengerBehandling.Behandlingstype.valueOf(domene)
        }
    }

    @Test
    fun `motta behandling - mangler rolle - svarer 403 og lagrer ingenting`() {
        with(TestApplicationContext()) {
            val tac = this
            val service = mockk<MottaNyBehandlingService>()
            val token = leggTilSystembruker(Systembrukerrolle.LES_BEHANDLING)

            testApplication {
                konfigurer(tac, service)
                postBehandling(
                    token,
                    body(),
                    ForventetRespons(
                        status = HttpStatusCode.Forbidden,
                        body = ForventetBody.Json(
                            """
                            {
                              "melding": "Mangler rollen LAGRE_TILTAKSPENGER_HENDELSER. Har rollene: [LES_BEHANDLING]",
                              "kode": "mangler_rolle"
                            }
                            """.trimIndent(),
                        ),
                        contentType = ContentType.parse("application/json; charset=UTF-8"),
                    ),
                )
            }

            verify(exactly = 0) { service.motta(any()) }
        }
    }

    @Test
    fun `motta behandling - sak finnes ikke - svarer 400 sak_ikke_funnet`() {
        with(TestApplicationContext()) {
            val tac = this
            val service = mockk<MottaNyBehandlingService>()
            every { service.motta(any()) } returns KanIkkeMottaBehandling.SakIkkeFunnet(SakId.fromString(sakId)).left()

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, service)
                postBehandling(
                    token,
                    body(),
                    ForventetRespons(
                        status = HttpStatusCode.BadRequest,
                        body = ForventetBody.Json(
                            """
                            {
                              "melding": "Behandling med id behandlingId kunne ikke lagres siden sak $sakId ikke finnes",
                              "kode": "sak_ikke_funnet"
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
    fun `motta behandling - persisteringsfeil - svarer 500 ukjent_feil`() {
        with(TestApplicationContext()) {
            val tac = this
            val service = mockk<MottaNyBehandlingService>()
            every { service.motta(any()) } returns KanIkkeMottaBehandling.Persisteringsfeil.left()

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, service)
                postBehandling(
                    token,
                    body(),
                    ForventetRespons(
                        status = HttpStatusCode.InternalServerError,
                        body = ForventetBody.Json(
                            """
                            {
                              "melding": "Behandling med id behandlingId kunne ikke lagres siden en ukjent feil oppstod",
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
    fun `motta behandling - misformet json - svarer 400`() {
        with(TestApplicationContext()) {
            val tac = this
            val service = mockk<MottaNyBehandlingService>()
            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, service)
                postBehandling(token, """{"behandlingId":"bare-tull"}""", ForventetRespons(HttpStatusCode.BadRequest))
            }

            verify(exactly = 0) { service.motta(any()) }
        }
    }

    private fun ApplicationTestBuilder.konfigurer(
        tac: TestApplicationContext,
        service: MottaNyBehandlingService,
    ) {
        application {
            jacksonSerialization()
            setupAuthentication(tac.texasClient)
            routing {
                authenticate(IdentityProvider.AZUREAD.value) {
                    mottaNyBehandlingRoute(mottaNyBehandlingService = service, clock = fixedClock)
                }
            }
        }
    }

    private suspend fun ApplicationTestBuilder.postBehandling(
        token: String,
        body: String,
        forventet: ForventetRespons,
    ): HttpResponse = defaultRequestWithAssertions(
        HttpMethod.Post,
        url {
            protocol = URLProtocol.HTTPS
            path("behandling")
        },
        jwt = token,
        forventet = forventet,
    ) {
        setBody(body)
    }
}
