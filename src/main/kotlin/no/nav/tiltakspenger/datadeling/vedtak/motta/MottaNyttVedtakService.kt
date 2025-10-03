package no.nav.tiltakspenger.datadeling.vedtak.motta

import arrow.core.Either
import no.nav.tiltakspenger.datadeling.vedtak.db.VedtakRepo
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak

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
