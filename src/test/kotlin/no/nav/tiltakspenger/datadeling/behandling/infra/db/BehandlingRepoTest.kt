package no.nav.tiltakspenger.datadeling.behandling.infra.db
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.tiltakspenger.datadeling.behandling.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.testdata.BehandlingMother
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periode.Periode
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.postgresql.util.PSQLException
import java.time.LocalDate

class BehandlingRepoTest {

    @Test
    fun `kan lagre og hente søknadsbehandling`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val behandlingRepo = testDataHelper.behandlingRepo
            val fnr = Fnr.random()
            val sak = SakMother.sak(fnr = fnr)
            sakRepo.lagre(sak)
            val behandling = BehandlingMother.tiltakspengerBehandling(
                sakId = sak.id,
                fnr = sak.fnr,
                saksnummer = sak.saksnummer,
            )
            behandlingRepo.lagre(behandling)
            behandlingRepo.hentForFnr(fnr).firstOrNull()?.behandling shouldBe behandling
            val enDagFørFraOgMed = behandling.periode!!.fraOgMed.minusDays(1)
            val enDagEtterTilOgMed = behandling.periode.tilOgMed.plusDays(1)

            // periode før behandling
            behandlingRepo.hentForFnrOgPeriode(
                fnr,
                Periode(enDagFørFraOgMed, enDagFørFraOgMed),
            ) shouldBe emptyList()
            // periode første dag i behandling
            behandlingRepo.hentForFnrOgPeriode(
                fnr,
                Periode(behandling.periode.fraOgMed, behandling.periode.fraOgMed),
            ) shouldBe listOf(behandling)
            // periode siste dag i behandling
            behandlingRepo.hentForFnrOgPeriode(
                fnr,
                Periode(behandling.periode.tilOgMed, behandling.periode.tilOgMed),
            ) shouldBe listOf(behandling)
            // periode etter behandling
            behandlingRepo.hentForFnrOgPeriode(
                fnr,
                Periode(enDagEtterTilOgMed, enDagEtterTilOgMed),
            ) shouldBe emptyList()
        }
    }

    @Test
    fun `hentApneBehandlinger - har en åpen og en avsluttet behandling - returnerer åpen behandling`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val behandlingRepo = testDataHelper.behandlingRepo
            val fnr = Fnr.random()
            val sak = SakMother.sak(fnr = fnr)
            sakRepo.lagre(sak)
            val avsluttetBehandling = BehandlingMother.tiltakspengerBehandling(
                sakId = sak.id,
                fnr = sak.fnr,
                saksnummer = sak.saksnummer,
                behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.VEDTATT,
            )
            behandlingRepo.lagre(avsluttetBehandling)
            val apenRevurdering = BehandlingMother.tiltakspengerBehandling(
                sakId = sak.id,
                fnr = sak.fnr,
                saksnummer = sak.saksnummer,
                fom = null,
                tom = null,
                behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.UNDER_BEHANDLING,
                beslutter = null,
                iverksattTidspunkt = null,
                behandlingstype = TiltakspengerBehandling.Behandlingstype.REVURDERING,
            )
            behandlingRepo.lagre(apenRevurdering)
            behandlingRepo.hentForFnr(fnr).size shouldBe 2

            val apneBehandlinger = behandlingRepo.hentApneBehandlinger(fnr)
            apneBehandlinger.size shouldBe 1
            apneBehandlinger.first().behandling shouldBe apenRevurdering
        }
    }

    @Test
    fun `støtter null i alle nullable felter`() {
        withMigratedDb { testDataHelper ->
            val sakRepo = testDataHelper.sakRepo
            val behandlingRepo = testDataHelper.behandlingRepo
            val fnr = Fnr.random()
            val sak = SakMother.sak(fnr = fnr)
            sakRepo.lagre(sak)
            val behandling = BehandlingMother.tiltakspengerBehandling(
                fom = null,
                tom = null,
                saksbehandler = null,
                beslutter = null,
                iverksattTidspunkt = null,
                sakId = sak.id,
                fnr = sak.fnr,
                saksnummer = sak.saksnummer,
            )
            behandlingRepo.lagre(behandling)
            behandlingRepo.hentForFnr(fnr).firstOrNull()?.behandling shouldBe behandling
            behandlingRepo.hentForFnrOgPeriode(fnr, Periode(LocalDate.of(1970, 1, 1), LocalDate.of(9999, 12, 31))) shouldBe emptyList()
        }
    }

    @Test
    fun `kan ikke lagre behandling hvis kun en av fra og med og til og med er null`() {
        withMigratedDb { testDataHelper ->
            val sak = SakMother.sak()
            testDataHelper.sakRepo.lagre(sak)

            assertThrows<PSQLException> {
                testDataHelper.sessionFactory.withSession { session ->
                    session.run(
                        queryOf(
                            """
                                insert into behandling (
                                  behandling_id,
                                  sak_id,
                                  fra_og_med,
                                  til_og_med,
                                  behandling_status,
                                  saksbehandler,
                                  beslutter,
                                  iverksatt_tidspunkt,
                                  opprettet_tidspunkt_saksbehandling_api,
                                  mottatt_tidspunkt_datadeling,
                                  behandlingstype,
                                  sist_endret
                                ) values (
                                  :behandling_id,
                                  :sak_id,
                                  :fra_og_med,
                                  :til_og_med,
                                  :behandling_status,
                                  :saksbehandler,
                                  :beslutter,
                                  :iverksatt_tidspunkt,
                                  now(),
                                  now(),
                                  :behandlingstype,
                                  now()
                                )
                            """.trimIndent(),
                            mapOf(
                                "behandling_id" to "behandling-med-kun-fra-og-med",
                                "sak_id" to sak.id,
                                "fra_og_med" to LocalDate.of(2026, 1, 1),
                                "til_og_med" to null,
                                "behandling_status" to TiltakspengerBehandling.Behandlingsstatus.UNDER_BEHANDLING.name,
                                "saksbehandler" to null,
                                "beslutter" to null,
                                "iverksatt_tidspunkt" to null,
                                "behandlingstype" to TiltakspengerBehandling.Behandlingstype.SOKNADSBEHANDLING.name,
                            ),
                        ).asUpdate,
                    )
                }
            }
        }
    }
}
