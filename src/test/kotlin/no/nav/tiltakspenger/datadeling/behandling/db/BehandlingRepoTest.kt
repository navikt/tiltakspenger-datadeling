package no.nav.tiltakspenger.datadeling.behandling.db

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.testdata.BehandlingMother
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.random
import no.nav.tiltakspenger.libs.periodisering.Periode
import org.junit.jupiter.api.Test
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
            val behandling = BehandlingMother.tiltakspengerBehandling(sakId = sak.id)
            behandlingRepo.lagre(behandling, fnr, sak.saksnummer)
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
            ).map { it.behandling } shouldBe listOf(behandling)
            // periode siste dag i behandling
            behandlingRepo.hentForFnrOgPeriode(
                fnr,
                Periode(behandling.periode.tilOgMed, behandling.periode.tilOgMed),
            ).map { it.behandling } shouldBe listOf(behandling)
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
                behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.VEDTATT,
            )
            behandlingRepo.lagre(avsluttetBehandling, fnr, sak.saksnummer)
            val apenRevurdering = BehandlingMother.tiltakspengerBehandling(
                sakId = sak.id,
                fom = null,
                tom = null,
                behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.UNDER_BEHANDLING,
                beslutter = null,
                iverksattTidspunkt = null,
                behandlingstype = TiltakspengerBehandling.Behandlingstype.REVURDERING,
            )
            behandlingRepo.lagre(apenRevurdering, fnr, sak.saksnummer)
            behandlingRepo.hentForFnr(fnr).size shouldBe 2

            val apneBehandlinger = behandlingRepo.hentApneBehandlinger(fnr)
            apneBehandlinger.size shouldBe 1
            apneBehandlinger.first().behandling.behandlingId shouldBe apenRevurdering.behandlingId
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
            )
            behandlingRepo.lagre(behandling, fnr, sak.saksnummer)
            behandlingRepo.hentForFnr(fnr).firstOrNull()?.behandling shouldBe behandling
            behandlingRepo.hentForFnrOgPeriode(fnr, Periode(LocalDate.of(1970, 1, 1), LocalDate.of(9999, 12, 31))) shouldBe emptyList()
        }
    }
}
