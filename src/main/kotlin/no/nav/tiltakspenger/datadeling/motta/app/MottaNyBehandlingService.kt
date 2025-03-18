package no.nav.tiltakspenger.datadeling.motta.app

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.motta.infra.db.BehandlingRepo

class MottaNyBehandlingService(
    private val mottaNyBehandlingRepo: BehandlingRepo,
) {
    fun motta(
        behandling: TiltakspengerBehandling,
        systembruker: Systembruker,
    ): Either<KanIkkeMottaBehandling, Unit> {
        if (!systembruker.roller.kanLagreTiltakspengerHendelser()) {
            return KanIkkeMottaBehandling.HarIkkeTilgang(
                kreverEnAvRollene = listOf(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER),
                harRollene = systembruker.roller.toList(),
            ).left()
        }
        return Either.catch { mottaNyBehandlingRepo.lagre(behandling) }.mapLeft {
            KanIkkeMottaBehandling.Persisteringsfeil
        }
    }
}

sealed interface KanIkkeMottaBehandling {
    data class HarIkkeTilgang(
        val kreverEnAvRollene: List<Systembrukerrolle>,
        val harRollene: List<Systembrukerrolle>,
    ) : KanIkkeMottaBehandling

    data object Persisteringsfeil : KanIkkeMottaBehandling
}
