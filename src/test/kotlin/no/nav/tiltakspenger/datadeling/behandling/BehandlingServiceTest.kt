package no.nav.tiltakspenger.datadeling.behandling

import io.kotest.matchers.shouldBe
import no.nav.tiltakspenger.datadeling.testdata.BehandlingMother
import no.nav.tiltakspenger.datadeling.testdata.SakMother
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime

class BehandlingServiceTest {

    @Test
    fun `hentBehandlingerForTp returnerer kun apne soknadsbehandlinger med periode`() {
        val fnr = Fnr.fromString("12345678910")
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

        BehandlingService(behandlingRepo).hentBehandlingerForTp(fnr, periode) shouldBe listOf(
            Behandling(
                behandlingId = "apen-soknad",
                periode = periode,
            ),
        )
    }

    @Test
    fun `hentApneBehandlinger sorterer synkende pa opprettet tidspunkt`() {
        val fnr = Fnr.fromString("12345678910")
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

        BehandlingService(behandlingRepo).hentApneBehandlinger(fnr).map { it.behandling.behandlingId } shouldBe listOf(
            "nyest",
            "eldst",
        )
    }

    private class TestBehandlingRepo(
        private val behandlingerForFnrOgPeriode: List<TiltakspengerBehandling> = emptyList(),
        private val apneBehandlinger: List<TiltakspengeBehandlingMedSak> = emptyList(),
    ) : BehandlingRepo {
        override fun lagre(behandling: TiltakspengerBehandling) = Unit

        override fun hentForFnrOgPeriode(fnr: Fnr, periode: Periode): List<TiltakspengerBehandling> = behandlingerForFnrOgPeriode

        override fun hentApneBehandlinger(fnr: Fnr): List<TiltakspengeBehandlingMedSak> = apneBehandlinger

        override fun hentForFnr(fnr: Fnr): List<TiltakspengeBehandlingMedSak> = emptyList()
    }
}
