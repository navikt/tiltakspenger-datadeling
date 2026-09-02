package no.nav.tiltakspenger.datadeling.behandling

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.testdata.BehandlingMother
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate

class HentBehandlingerForPeriodeServiceTest {

    @Test
    fun `hentBehandlingerForPeriode returnerer kun apne soknadsbehandlinger med periode`() {
        val fnr = Fnr.fromString("12845678910")
        val periode = Periode(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 31))
        val apenSoknadsbehandling = BehandlingMother.tiltakspengerBehandling(
            behandlingId = "apen-soknad",
            fnr = fnr,
            fom = periode.fraOgMed,
            tom = periode.tilOgMed,
            behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.KLAR_TIL_BEHANDLING,
            behandlingstype = TiltakspengerBehandling.Behandlingstype.SOKNADSBEHANDLING,
        )
        val apenUtenPeriode = BehandlingMother.tiltakspengerBehandling(
            behandlingId = "apen-uten-periode",
            fnr = fnr,
            fom = null,
            tom = null,
            behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.KLAR_TIL_BEHANDLING,
            behandlingstype = TiltakspengerBehandling.Behandlingstype.SOKNADSBEHANDLING,
        )
        val apenMeldekortbehandling = BehandlingMother.tiltakspengerBehandling(
            behandlingId = "apen-meldekort",
            fnr = fnr,
            behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.KLAR_TIL_BEHANDLING,
            behandlingstype = TiltakspengerBehandling.Behandlingstype.MELDEKORTBEHANDLING,
        )
        val lukketSoknadsbehandling = BehandlingMother.tiltakspengerBehandling(
            behandlingId = "lukket-soknad",
            fnr = fnr,
            behandlingStatus = TiltakspengerBehandling.Behandlingsstatus.VEDTATT,
            behandlingstype = TiltakspengerBehandling.Behandlingstype.SOKNADSBEHANDLING,
        )
        val behandlingRepo = TestBehandlingRepo(
            behandlingerForFnrOgPeriode = listOf(
                apenSoknadsbehandling,
                apenUtenPeriode,
                apenMeldekortbehandling,
                lukketSoknadsbehandling,
            ),
        )

        HentBehandlingerForPeriodeService(behandlingRepo).hentBehandlingerForPeriode(fnr, periode) shouldBe listOf(
            Behandling(
                behandlingId = "apen-soknad",
                periode = periode,
            ),
        )
    }
}
