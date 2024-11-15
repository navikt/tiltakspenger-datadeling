package no.nav.tiltakspenger.datadeling.motta.infra.db

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.felles.BehandlingMother
import no.nav.tiltakspenger.datadeling.felles.withMigratedDb
import no.nav.tiltakspenger.libs.periodisering.Periode
import org.junit.jupiter.api.Test

class MottaNyBehandlingPostgresRepoTest {

    @Test
    fun `kan lagre og hente behandling`() {
        withMigratedDb { testDataHelper ->
            val repo = testDataHelper.mottaNyBehandlingPostgresRepo
            val behandling = BehandlingMother.tiltakspengerBehandling()
            repo.lagre(behandling)
            repo.hentForFnr(behandling.fnr) shouldBe behandling
            val enDagFørFraOgMed = behandling.periode.fraOgMed.minusDays(1)
            val enDagEtterTilOgMed = behandling.periode.tilOgMed.plusDays(1)

            // Feil kilde
            repo.hentForFnrOgPeriode(behandling.fnr, behandling.periode, "arena") shouldBe emptyList()
            // periode før behandling
            repo.hentForFnrOgPeriode(behandling.fnr, Periode(enDagFørFraOgMed, enDagFørFraOgMed), "tp") shouldBe emptyList()
            // periode første dag i behandling
            repo.hentForFnrOgPeriode(behandling.fnr, Periode(behandling.periode.fraOgMed, behandling.periode.fraOgMed), "tp") shouldBe listOf(behandling)
            // periode siste dag i behandling
            repo.hentForFnrOgPeriode(behandling.fnr, Periode(behandling.periode.tilOgMed, behandling.periode.tilOgMed), "tp") shouldBe listOf(behandling)
            // periode etter behandling
            repo.hentForFnrOgPeriode(behandling.fnr, Periode(enDagEtterTilOgMed, enDagEtterTilOgMed), "tp") shouldBe emptyList()
        }
    }
}
