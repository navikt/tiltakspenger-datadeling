package no.nav.tiltakspenger.datadeling.behandling

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.testdata.BehandlingMother
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.libs.common.Fnr
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class HentApneBehandlingerServiceTest {

    @Test
    fun `hentApneBehandlinger sorterer synkende pa opprettet tidspunkt`() {
        val fnr = Fnr.fromString("12845678910")
        val sak = SakMother.sak(fnr = fnr)
        val eldst = BehandlingMother.tiltakspengerBehandling(
            behandlingId = "eldst",
            fnr = fnr,
            opprettetTidspunktSaksbehandlingApi = LocalDateTime.parse("2024-01-01T00:00:00"),
        )
        val nyest = BehandlingMother.tiltakspengerBehandling(
            behandlingId = "nyest",
            fnr = fnr,
            opprettetTidspunktSaksbehandlingApi = LocalDateTime.parse("2024-02-01T00:00:00"),
        )
        val behandlingRepo = TestBehandlingRepo(
            apneBehandlinger = listOf(
                TiltakspengeBehandlingMedSak(sak = sak, behandling = eldst),
                TiltakspengeBehandlingMedSak(sak = sak, behandling = nyest),
            ),
        )

        HentApneBehandlingerService(behandlingRepo).hentApneBehandlinger(fnr).map { it.behandling.behandlingId } shouldBe listOf(
            "nyest",
            "eldst",
        )
    }
}
