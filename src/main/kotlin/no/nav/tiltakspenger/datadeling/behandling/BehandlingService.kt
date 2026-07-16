package no.nav.tiltakspenger.datadeling.behandling

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode

class BehandlingService(
    private val behandlingRepo: BehandlingRepo,
) {
    /**
     * Tar kun med åpne søknadsbehandlinger med periode.
     * Se [TiltakspengerBehandling.erApenSoknadsbehandling] for kriterier.
     */
    fun hentBehandlingerForTp(
        fnr: Fnr,
        periode: Periode,
    ): List<Behandling> {
        return behandlingRepo.hentForFnrOgPeriode(fnr, periode)
            .filter { it.erApenSoknadsbehandling() }
            .mapNotNull { behandling ->
                behandling.periode?.let { periode ->
                    Behandling(
                        behandlingId = behandling.behandlingId,
                        periode = periode,
                    )
                }
            }
    }

    fun hentApneBehandlinger(
        fnr: Fnr,
    ): List<TiltakspengeBehandlingMedSak> {
        return behandlingRepo.hentApneBehandlinger(fnr)
            .sortedByDescending { it.behandling.opprettetTidspunktSaksbehandlingApi }
    }

    // TODO jah: Flytt dette domenepredikatet til TiltakspengerBehandling, ved siden av erApenBehandling().
    private fun TiltakspengerBehandling.erApenSoknadsbehandling() =
        this.erApenBehandling() && periode != null &&
            behandlingstype == TiltakspengerBehandling.Behandlingstype.SOKNADSBEHANDLING
}
