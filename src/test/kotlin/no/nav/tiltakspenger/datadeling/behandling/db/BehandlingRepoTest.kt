package no.nav.tiltakspenger.datadeling.behandling.db

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.testdata.BehandlingMother
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
            val repo = testDataHelper.behandlingRepo
            val behandling = BehandlingMother.tiltakspengerBehandling()
            repo.lagre(behandling)
            repo.hentForFnr(behandling.fnr).firstOrNull() shouldBe behandling
            val enDagFørFraOgMed = behandling.periode!!.fraOgMed.minusDays(1)
            val enDagEtterTilOgMed = behandling.periode.tilOgMed.plusDays(1)

            // periode før behandling
            repo.hentForFnrOgPeriode(
                behandling.fnr,
                Periode(enDagFørFraOgMed, enDagFørFraOgMed),
            ) shouldBe emptyList()
            // periode første dag i behandling
            repo.hentForFnrOgPeriode(
                behandling.fnr,
                Periode(behandling.periode.fraOgMed, behandling.periode.fraOgMed),
            ) shouldBe listOf(behandling)
            // periode siste dag i behandling
            repo.hentForFnrOgPeriode(
                behandling.fnr,
                Periode(behandling.periode.tilOgMed, behandling.periode.tilOgMed),
            ) shouldBe listOf(behandling)
            // periode etter behandling
            repo.hentForFnrOgPeriode(
                behandling.fnr,
                Periode(enDagEtterTilOgMed, enDagEtterTilOgMed),
            ) shouldBe emptyList()
        }
    }

    @Test
    fun `hentApneBehandlinger - har en åpen og en avsluttet behandling - returnerer åpen behandling`() {
        withMigratedDb { testDataHelper ->
            val repo = testDataHelper.behandlingRepo
            val fnr = Fnr.random()
            val avsluttetBehandling = BehandlingMother.tiltakspengerBehandling(
                fnr = fnr,
                behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.VEDTATT,
            )
            repo.lagre(avsluttetBehandling)
            val apenRevurdering = BehandlingMother.tiltakspengerBehandling(
                fnr = fnr,
                fom = null,
                tom = null,
                behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.UNDER_BEHANDLING,
                beslutter = null,
                iverksattTidspunkt = null,
                behandlingstype = TiltakspengerBehandling.Behandlingstype.REVURDERING,
            )
            repo.lagre(apenRevurdering)
            repo.hentForFnr(fnr).size shouldBe 2

            val apneBehandlinger = repo.hentApneBehandlinger(fnr)
            apneBehandlinger.size shouldBe 1
            apneBehandlinger.first().behandlingId shouldBe apenRevurdering.behandlingId
        }
    }

    @Test
    fun `støtter null i alle nullable felter`() {
        withMigratedDb { testDataHelper ->
            val repo = testDataHelper.behandlingRepo
            val behandling = BehandlingMother.tiltakspengerBehandling(
                fom = null,
                tom = null,
                saksbehandler = null,
                beslutter = null,
                iverksattTidspunkt = null,
            )
            repo.lagre(behandling)
            repo.hentForFnr(behandling.fnr).firstOrNull() shouldBe behandling
            repo.hentForFnrOgPeriode(behandling.fnr, Periode(LocalDate.of(1970, 1, 1), LocalDate.of(9999, 12, 31))) shouldBe emptyList()
        }
    }
}
