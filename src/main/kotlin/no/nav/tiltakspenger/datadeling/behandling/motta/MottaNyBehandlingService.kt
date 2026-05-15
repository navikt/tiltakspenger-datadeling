package no.nav.tiltakspenger.datadeling.behandling.motta

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import no.nav.tiltakspenger.datadeling.behandling.db.BehandlingRepo
import no.nav.tiltakspenger.datadeling.behandling.domene.MottattTiltakspengerBehandling
import no.nav.tiltakspenger.datadeling.sak.db.SakRepo

class MottaNyBehandlingService(
    private val mottaNyBehandlingRepo: BehandlingRepo,
    private val sakRepo: SakRepo,
) {
    fun motta(
        behandling: MottattTiltakspengerBehandling,
    ): Either<KanIkkeMottaBehandling, Unit> {
        return Either.catch { sakRepo.hentForId(behandling.sakId) }
            .mapLeft { KanIkkeMottaBehandling.Persisteringsfeil }
            .flatMap { sak ->
                if (sak == null) {
                    KanIkkeMottaBehandling.SakIkkeFunnet(behandling.sakId).left()
                } else {
                    Either.catch { mottaNyBehandlingRepo.lagre(behandling.medSak(sak)) }.mapLeft {
                        KanIkkeMottaBehandling.Persisteringsfeil
                    }
                }
            }
    }
}

sealed interface KanIkkeMottaBehandling {
    data class SakIkkeFunnet(val sakId: String) : KanIkkeMottaBehandling
    data object Persisteringsfeil : KanIkkeMottaBehandling
}
