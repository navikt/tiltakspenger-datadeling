package no.nav.tiltakspenger.datadeling.behandling

import no.nav.tiltakspenger.libs.common.Fnr

/**
 * Henter alle åpne behandlinger for en person, uavhengig av periode og behandlingstype.
 * Serverer `POST /behandlinger/apne`.
 */
class HentApneBehandlingerService(
    private val behandlingRepo: BehandlingRepo,
) {
    /**
     * Nyeste behandling først.
     */
    fun hentApneBehandlinger(
        fnr: Fnr,
    ): List<TiltakspengeBehandlingMedSak> {
        return behandlingRepo.hentApneBehandlinger(fnr)
            .sortedByDescending { it.behandling.opprettetTidspunktSaksbehandlingApi }
    }
}
