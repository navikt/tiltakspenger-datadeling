package no.nav.tiltakspenger.datadeling.vedtak.motta

import arrow.core.Either
import no.nav.tiltakspenger.datadeling.vedtak.db.VedtakRepo
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr

class MottaNyttVedtakService(
    private val vedtakRepo: VedtakRepo,
) {
    fun motta(
        vedtak: TiltakspengerVedtak,
        fnr: Fnr,
        saksnummer: String,
    ): Either<KanIkkeMottaVedtak, Unit> {
        return Either.catch { vedtakRepo.lagre(vedtak, fnr, saksnummer) }.mapLeft {
            KanIkkeMottaVedtak.Persisteringsfeil
        }
    }
}

sealed interface KanIkkeMottaVedtak {
    data object Persisteringsfeil : KanIkkeMottaVedtak
}
