package no.nav.tiltakspenger.datadeling.vedtak.infra.routes

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.shouldBe
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.tiltakspenger.datadeling.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.infra.jacksonSerialization
import no.nav.tiltakspenger.datadeling.infra.setupAuthentication
import no.nav.tiltakspenger.datadeling.testutils.TestApplicationContext
import no.nav.tiltakspenger.datadeling.testutils.leggTilSystembruker
import no.nav.tiltakspenger.datadeling.vedtak.Barnetillegg
import no.nav.tiltakspenger.datadeling.vedtak.BarnetilleggPeriode
import no.nav.tiltakspenger.datadeling.vedtak.KanIkkeMottaVedtak
import no.nav.tiltakspenger.datadeling.vedtak.MottaNyttVedtakService
import no.nav.tiltakspenger.datadeling.vedtak.MottattTiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.SakId
import no.nav.tiltakspenger.libs.common.fixedClock
import no.nav.tiltakspenger.libs.common.nå
import no.nav.tiltakspenger.libs.httpklient.infra.kall.HttpMethod
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetBody
import no.nav.tiltakspenger.libs.ktor.test.common.ForventetRespons
import no.nav.tiltakspenger.libs.ktor.test.common.TestRespons
import no.nav.tiltakspenger.libs.ktor.test.common.defaultRequestWithAssertions
import no.nav.tiltakspenger.libs.periode.Periode
import no.nav.tiltakspenger.libs.texas.IdentityProvider
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Route- og JSON-tester for `POST /vedtak`, som tiltakspenger-saksbehandling-api kaller for hvert rammevedtak.
 * Dekker rollesjekk, feilutfallene fra servicen, og at hver rettighet og hjemmel i JSON-kontrakten mapper til riktig domeneverdi.
 */
class MottaNyttVedtakRouteTest {

    private val sakId = "sak_01ARZ3NDEKTSV4RRFFQ69G5FAV"

    private fun body(
        rettighet: String = "TILTAKSPENGER",
        sakId: String = this.sakId,
        opprettet: String = "2024-01-15T10:30:00",
        barnetillegg: String = "null",
        valgteHjemler: String = "null",
        innvilgelsesperiode: String = """{"fraOgMed": "2024-01-01", "tilOgMed": "2024-06-30"}""",
    ) = """
        {
            "vedtakId": "vedtak_01ARZ3NDEKTSV4RRFFQ69G5FAV",
            "vedtaksperiode": {"fraOgMed": "2024-01-01", "tilOgMed": "2024-12-31"},
            "innvilgelsesperiode": $innvilgelsesperiode,
            "omgjørRammevedtakId": "vedtak_01ARZ3NDEKTSV4RRFFQ69G5FAW",
            "omgjortAvRammevedtakId": "vedtak_01ARZ3NDEKTSV4RRFFQ69G5FAX",
            "rettighet": "$rettighet",
            "sakId": "$sakId",
            "opprettet": "$opprettet",
            "barnetillegg": $barnetillegg,
            "valgteHjemlerHarIkkeRettighet": $valgteHjemler
        }
    """.trimIndent()

    @Test
    fun `motta vedtak - gyldig request - lagrer og svarer 200`() {
        with(TestApplicationContext()) {
            val tac = this
            val service = mockk<MottaNyttVedtakService>()
            val mottatt = slot<MottattTiltakspengerVedtak>()
            every { service.motta(capture(mottatt)) } returns Unit.right()

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, service)
                postVedtak(token, body(), ForventetRespons(200, ForventetBody.Tom))
            }

            mottatt.captured shouldBe MottattTiltakspengerVedtak(
                virkningsperiode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)),
                innvilgelsesperiode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 6, 30)),
                omgjørRammevedtakId = "vedtak_01ARZ3NDEKTSV4RRFFQ69G5FAW",
                omgjortAvRammevedtakId = "vedtak_01ARZ3NDEKTSV4RRFFQ69G5FAX",
                rettighet = TiltakspengerVedtak.Rettighet.TILTAKSPENGER,
                vedtakId = "vedtak_01ARZ3NDEKTSV4RRFFQ69G5FAV",
                sakId = SakId.fromString(sakId),
                mottattTidspunkt = nå(fixedClock),
                opprettet = LocalDateTime.parse("2024-01-15T10:30:00"),
                barnetillegg = null,
                valgteHjemlerHarIkkeRettighet = null,
            )
        }
    }

    @Test
    fun `motta vedtak - med barnetillegg og hjemler - mapper hele nyttelasten`() {
        with(TestApplicationContext()) {
            val tac = this
            val service = mockk<MottaNyttVedtakService>()
            val mottatt = slot<MottattTiltakspengerVedtak>()
            every { service.motta(capture(mottatt)) } returns Unit.right()

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, service)
                postVedtak(
                    token,
                    body(
                        rettighet = "TILTAKSPENGER_OG_BARNETILLEGG",
                        barnetillegg = """
                            {"perioder": [{"antallBarn": 2, "periode": {"fraOgMed": "2024-01-01", "tilOgMed": "2024-12-31"}}]}
                        """.trimIndent(),
                        valgteHjemler = """["ALDER", "INSTITUSJONSOPPHOLD"]""",
                    ),
                    ForventetRespons(200, ForventetBody.Tom),
                )
            }

            mottatt.captured.barnetillegg shouldBe Barnetillegg(
                perioder = listOf(
                    BarnetilleggPeriode(
                        antallBarn = 2,
                        periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31)),
                    ),
                ),
            )
            mottatt.captured.valgteHjemlerHarIkkeRettighet shouldBe listOf(
                TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.ALDER,
                TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.INSTITUSJONSOPPHOLD,
            )
        }
    }

    @Test
    fun `motta vedtak - uten innvilgelsesperiode - lagres uten`() {
        with(TestApplicationContext()) {
            val tac = this
            val service = mockk<MottaNyttVedtakService>()
            val mottatt = slot<MottattTiltakspengerVedtak>()
            every { service.motta(capture(mottatt)) } returns Unit.right()

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, service)
                postVedtak(
                    token,
                    body(innvilgelsesperiode = "null"),
                    ForventetRespons(200, ForventetBody.Tom),
                )
            }

            mottatt.captured.innvilgelsesperiode shouldBe null
        }
    }

    @ParameterizedTest(name = "rettighet {0} mapper til {1}")
    @CsvSource(
        "TILTAKSPENGER, TILTAKSPENGER",
        "TILTAKSPENGER_OG_BARNETILLEGG, TILTAKSPENGER_OG_BARNETILLEGG",
        "STANS, STANS",
        "AVSLAG, AVSLAG",
        "OPPHØR, OPPHØR",
    )
    fun `motta vedtak - hver rettighet mapper til riktig domeneverdi`(fraJson: String, domene: String) {
        with(TestApplicationContext()) {
            val tac = this
            val service = mockk<MottaNyttVedtakService>()
            val mottatt = slot<MottattTiltakspengerVedtak>()
            every { service.motta(capture(mottatt)) } returns Unit.right()

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, service)
                postVedtak(token, body(rettighet = fraJson), ForventetRespons(200, ForventetBody.Tom))
            }

            mottatt.captured.rettighet shouldBe TiltakspengerVedtak.Rettighet.valueOf(domene)
        }
    }

    @ParameterizedTest(name = "hjemmel {0} mapper til {1}")
    @CsvSource(
        "DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK, DELTAR_IKKE_PA_ARBEIDSMARKEDSTILTAK",
        "ALDER, ALDER",
        "LIVSOPPHOLDSYTELSER, LIVSOPPHOLDSYTELSER",
        "KVALIFISERINGSPROGRAMMET, KVALIFISERINGSPROGRAMMET",
        "INTRODUKSJONSPROGRAMMET, INTRODUKSJONSPROGRAMMET",
        "LONN_FRA_TILTAKSARRANGOR, LONN_FRA_TILTAKSARRANGOR",
        "LONN_FRA_ANDRE, LONN_FRA_ANDRE",
        "INSTITUSJONSOPPHOLD, INSTITUSJONSOPPHOLD",
        "FREMMET_FOR_SENT, FREMMET_FOR_SENT",
        "IKKE_LOVLIG_OPPHOLD, IKKE_LOVLIG_OPPHOLD",
    )
    fun `motta vedtak - hver valgt hjemmel mapper til riktig domeneverdi`(fraJson: String, domene: String) {
        with(TestApplicationContext()) {
            val tac = this
            val service = mockk<MottaNyttVedtakService>()
            val mottatt = slot<MottattTiltakspengerVedtak>()
            every { service.motta(capture(mottatt)) } returns Unit.right()

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, service)
                postVedtak(
                    token,
                    body(rettighet = "AVSLAG", valgteHjemler = """["$fraJson"]"""),
                    ForventetRespons(200, ForventetBody.Tom),
                )
            }

            mottatt.captured.valgteHjemlerHarIkkeRettighet shouldBe listOf(
                TiltakspengerVedtak.ValgtHjemmelHarIkkeRettighet.valueOf(domene),
            )
        }
    }

    @Test
    fun `motta vedtak - mangler rolle - svarer 403 og lagrer ingenting`() {
        with(TestApplicationContext()) {
            val tac = this
            val service = mockk<MottaNyttVedtakService>()
            val token = leggTilSystembruker(Systembrukerrolle.LES_VEDTAK)

            testApplication {
                konfigurer(tac, service)
                postVedtak(
                    token,
                    body(),
                    ForventetRespons(
                        status = 403,
                        body = ForventetBody.Json(
                            """
                            {
                              "melding": "Mangler rollen LAGRE_TILTAKSPENGER_HENDELSER. Har rollene: [LES_VEDTAK]",
                              "kode": "mangler_rolle"
                            }
                            """.trimIndent(),
                        ),
                        contentType = "application/json; charset=UTF-8",
                    ),
                )
            }

            verify(exactly = 0) { service.motta(any()) }
        }
    }

    @Test
    fun `motta vedtak - ugyldig sakId - svarer 400 uten a kalle servicen`() {
        with(TestApplicationContext()) {
            val tac = this
            val service = mockk<MottaNyttVedtakService>()
            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, service)
                postVedtak(token, body(sakId = "ikke-en-sakid"), ForventetRespons(400))
            }

            verify(exactly = 0) { service.motta(any()) }
        }
    }

    @Test
    fun `motta vedtak - sak finnes ikke - svarer 500 sak_ikke_funnet`() {
        with(TestApplicationContext()) {
            val tac = this
            val service = mockk<MottaNyttVedtakService>()
            every { service.motta(any()) } returns KanIkkeMottaVedtak.SakIkkeFunnet(SakId.fromString(sakId)).left()

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, service)
                postVedtak(
                    token,
                    body(),
                    ForventetRespons(
                        status = 500,
                        body = ForventetBody.Json(
                            """
                            {
                              "melding": "Vedtak med id vedtak_01ARZ3NDEKTSV4RRFFQ69G5FAV kunne ikke lagres siden sak $sakId ikke finnes",
                              "kode": "sak_ikke_funnet"
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
    fun `motta vedtak - persisteringsfeil - svarer 500 ukjent_feil`() {
        with(TestApplicationContext()) {
            val tac = this
            val service = mockk<MottaNyttVedtakService>()
            every { service.motta(any()) } returns KanIkkeMottaVedtak.Persisteringsfeil.left()

            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, service)
                postVedtak(
                    token,
                    body(),
                    ForventetRespons(
                        status = 500,
                        body = ForventetBody.Json(
                            """
                            {
                              "melding": "Vedtak med id vedtak_01ARZ3NDEKTSV4RRFFQ69G5FAV kunne ikke lagres siden en ukjent feil oppstod",
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
    fun `motta vedtak - misformet json - svarer 400`() {
        with(TestApplicationContext()) {
            val tac = this
            val service = mockk<MottaNyttVedtakService>()
            val token = leggTilSystembruker(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER)

            testApplication {
                konfigurer(tac, service)
                postVedtak(token, """{"vedtakId":"bare-tull"}""", ForventetRespons(400))
            }

            verify(exactly = 0) { service.motta(any()) }
        }
    }

    private fun ApplicationTestBuilder.konfigurer(
        tac: TestApplicationContext,
        service: MottaNyttVedtakService,
    ) {
        application {
            jacksonSerialization()
            setupAuthentication(tac.texasClient)
            routing {
                authenticate(IdentityProvider.AZUREAD.value) {
                    mottaNyttVedtakRoute(mottaNyttVedtakService = service, clock = fixedClock)
                }
            }
        }
    }

    private suspend fun ApplicationTestBuilder.postVedtak(
        token: String,
        body: String,
        forventet: ForventetRespons,
    ): TestRespons = defaultRequestWithAssertions(
        HttpMethod.POST,
        "vedtak",
        jwt = token,
        forventet = forventet,
        body = body,
    )
}
