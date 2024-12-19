package no.nav.tiltakspenger.datadeling.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.datadeling.domene.Behandling
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.motta.app.BehandlingRepo
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode

class BehandlingService(
    private val behandlingRepo: BehandlingRepo,
) {
    suspend fun hentBehandlingerForTp(
        fnr: Fnr,
        periode: Periode,
        systembruker: Systembruker,
    ): Either<KanIkkeHenteBehandlinger, List<Behandling>> {
        if (!systembruker.roller.kanLeseBehandlinger()) {
            return KanIkkeHenteBehandlinger.HarIkkeTilgang(
                kreverEnAvRollene = listOf(Systembrukerrolle.LES_BEHANDLING),
                harRollene = systembruker.roller.toList(),
            ).left()
        }
        // TODO post-mvp jah: Dersom vi får avbrutt, må vi filtrere bort disse.
        return behandlingRepo.hentForFnrOgPeriode(fnr, periode, "tp")
            .filter { it.behandlingStatus != TiltakspengerBehandling.Behandlingsstatus.VEDTATT }
            .map {
                Behandling(
                    behandlingId = it.behandlingId,
                    periode = it.periode,
                )
            }.right()
    }
}

sealed interface KanIkkeHenteBehandlinger {
    data class HarIkkeTilgang(
        val kreverEnAvRollene: List<Systembrukerrolle>,
        val harRollene: List<Systembrukerrolle>,
    ) : KanIkkeHenteBehandlinger
}
