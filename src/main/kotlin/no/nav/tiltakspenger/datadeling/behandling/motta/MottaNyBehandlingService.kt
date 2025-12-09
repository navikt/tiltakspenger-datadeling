package no.nav.tiltakspenger.datadeling.behandling.motta

import arrow.core.Either
import no.nav.tiltakspenger.datadeling.behandling.db.BehandlingRepo
import no.nav.tiltakspenger.datadeling.behandling.domene.TiltakspengerBehandling
import no.nav.tiltakspenger.libs.common.Fnr

class MottaNyBehandlingService(
    private val mottaNyBehandlingRepo: BehandlingRepo,
) {
    fun motta(
        behandling: TiltakspengerBehandling,
        fnr: Fnr,
        saksnummer: String,
    ): Either<KanIkkeMottaBehandling, Unit> {
        return Either.catch { mottaNyBehandlingRepo.lagre(behandling, fnr, saksnummer) }.mapLeft {
            KanIkkeMottaBehandling.Persisteringsfeil
        }
    }
}

sealed interface KanIkkeMottaBehandling {
    data object Persisteringsfeil : KanIkkeMottaBehandling
}
