package no.nav.tiltakspenger.datadeling.behandling

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode

/**
 * Henter de åpne søknadsbehandlingene som overlapper med en periode.
 * Serverer `POST /behandlinger/perioder`.
 */
class HentBehandlingerForPeriodeService(
    private val behandlingRepo: BehandlingRepo,
) {
    /**
     * Tar kun med åpne søknadsbehandlinger med periode.
     * Se [erApenSoknadsbehandling] for kriterier.
     */
    fun hentBehandlingerForPeriode(
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

    // TODO jah: Flytt dette domenepredikatet til TiltakspengerBehandling, ved siden av erApenBehandling().
    private fun TiltakspengerBehandling.erApenSoknadsbehandling() =
        this.erApenBehandling() && periode != null &&
            behandlingstype == TiltakspengerBehandling.Behandlingstype.SOKNADSBEHANDLING
}
