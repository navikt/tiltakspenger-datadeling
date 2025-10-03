package no.nav.tiltakspenger.datadeling.behandling.motta

import arrow.core.Either
import no.nav.tiltakspenger.datadeling.behandling.db.BehandlingRepo
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerBehandling

class MottaNyBehandlingService(
    private val mottaNyBehandlingRepo: BehandlingRepo,
) {
    fun motta(
        behandling: TiltakspengerBehandling,
    ): Either<KanIkkeMottaBehandling, Unit> {
        return Either.catch { mottaNyBehandlingRepo.lagre(behandling) }.mapLeft {
            KanIkkeMottaBehandling.Persisteringsfeil
        }
    }
}

sealed interface KanIkkeMottaBehandling {
    data object Persisteringsfeil : KanIkkeMottaBehandling
}
