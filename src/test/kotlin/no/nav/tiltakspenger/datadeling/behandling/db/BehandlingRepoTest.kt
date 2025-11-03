package no.nav.tiltakspenger.datadeling.behandling.db

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.testdata.BehandlingMother
import no.nav.tiltakspenger.datadeling.testutils.withMigratedDb
import no.nav.tiltakspenger.libs.periodisering.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate

class BehandlingRepoTest {

    @Test
    fun `kan lagre og hente behandling`() {
        withMigratedDb { testDataHelper ->
            val repo = testDataHelper.behandlingRepo
            val behandling = BehandlingMother.tiltakspengerBehandling()
            repo.lagre(behandling)
            repo.hentForFnr(behandling.fnr) shouldBe behandling
            val enDagFørFraOgMed = behandling.periode!!.fraOgMed.minusDays(1)
            val enDagEtterTilOgMed = behandling.periode.tilOgMed.plusDays(1)

            // Feil kilde
            repo.hentForFnrOgPeriode(behandling.fnr, behandling.periode, Kilde.ARENA) shouldBe emptyList()
            // periode før behandling
            repo.hentForFnrOgPeriode(
                behandling.fnr,
                Periode(enDagFørFraOgMed, enDagFørFraOgMed),
                Kilde.TPSAK,
            ) shouldBe emptyList()
            // periode første dag i behandling
            repo.hentForFnrOgPeriode(
                behandling.fnr,
                Periode(behandling.periode.fraOgMed, behandling.periode.fraOgMed),
                Kilde.TPSAK,
            ) shouldBe listOf(behandling)
            // periode siste dag i behandling
            repo.hentForFnrOgPeriode(
                behandling.fnr,
                Periode(behandling.periode.tilOgMed, behandling.periode.tilOgMed),
                Kilde.TPSAK,
            ) shouldBe listOf(behandling)
            // periode etter behandling
            repo.hentForFnrOgPeriode(
                behandling.fnr,
                Periode(enDagEtterTilOgMed, enDagEtterTilOgMed),
                Kilde.TPSAK,
            ) shouldBe emptyList()
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
            repo.hentForFnr(behandling.fnr) shouldBe behandling
            repo.hentForFnrOgPeriode(behandling.fnr, Periode(LocalDate.of(1970, 1, 1), LocalDate.of(9999, 12, 31)), Kilde.TPSAK) shouldBe emptyList()
        }
    }
}
