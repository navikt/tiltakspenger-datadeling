package no.nav.tiltakspenger.datadeling.service

import no.nav.tiltakspenger.datadeling.domene.Behandling
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.motta.behandling.db.BehandlingRepo
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode

class BehandlingService(
    private val behandlingRepo: BehandlingRepo,
) {
    fun hentBehandlingerForTp(
        fnr: Fnr,
        periode: Periode,
    ): List<Behandling> {
        return behandlingRepo.hentForFnrOgPeriode(fnr, periode, Kilde.TPSAK)
            .filter { it.behandlingStatus != TiltakspengerBehandling.Behandlingsstatus.VEDTATT && it.behandlingStatus != TiltakspengerBehandling.Behandlingsstatus.AVBRUTT }
            .map {
                Behandling(
                    behandlingId = it.behandlingId,
                    periode = it.periode,
                )
            }
    }
}
