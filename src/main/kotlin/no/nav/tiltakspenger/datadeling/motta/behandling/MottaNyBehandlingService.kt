package no.nav.tiltakspenger.datadeling.motta.behandling

import arrow.core.Either
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.motta.behandling.db.BehandlingRepo

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
