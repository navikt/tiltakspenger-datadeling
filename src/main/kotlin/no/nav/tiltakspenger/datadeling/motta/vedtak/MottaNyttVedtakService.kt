package no.nav.tiltakspenger.datadeling.motta.vedtak

import arrow.core.Either
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.motta.vedtak.db.VedtakRepo

class MottaNyttVedtakService(
    private val vedtakRepo: VedtakRepo,
) {
    fun motta(
        vedtak: TiltakspengerVedtak,
    ): Either<KanIkkeMottaVedtak, Unit> {
        return Either.catch { vedtakRepo.lagre(vedtak) }.mapLeft {
            KanIkkeMottaVedtak.Persisteringsfeil
        }
    }
}

sealed interface KanIkkeMottaVedtak {
    data object Persisteringsfeil : KanIkkeMottaVedtak
}
