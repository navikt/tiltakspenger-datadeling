package no.nav.tiltakspenger.datadeling.motta.infra.db

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.felles.BehandlingMother
import no.nav.tiltakspenger.datadeling.felles.withMigratedDb
import org.junit.jupiter.api.Test

class MottaNyBehandlingPostgresRepoTest {

    @Test
    fun `kan lagre og hente behandling`() {
        withMigratedDb { testDataHelper ->
            val repo = testDataHelper.mottaNyBehandlingPostgresRepo
            val behandling = BehandlingMother.tiltakspengerBehandling()
            repo.lagre(behandling)
            repo.hentForFnr(behandling.fnr) shouldBe behandling
        }
    }
}
