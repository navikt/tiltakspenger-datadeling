package no.nav.tiltakspenger.datadeling.vedtak.motta

import arrow.core.Either
import arrow.core.flatMap
import arrow.core.left
import no.nav.tiltakspenger.datadeling.sak.db.SakRepo
import no.nav.tiltakspenger.datadeling.vedtak.db.VedtakRepo
import no.nav.tiltakspenger.datadeling.vedtak.domene.MottattTiltakspengerVedtak

class MottaNyttVedtakService(
    private val vedtakRepo: VedtakRepo,
    private val sakRepo: SakRepo,
) {
    fun motta(vedtak: MottattTiltakspengerVedtak): Either<KanIkkeMottaVedtak, Unit> {
        return Either.catch { sakRepo.hentForId(vedtak.sakId) }
            .mapLeft { KanIkkeMottaVedtak.Persisteringsfeil }
            .flatMap { sak ->
                if (sak == null) {
                    KanIkkeMottaVedtak.SakIkkeFunnet(vedtak.sakId).left()
                } else {
                    Either.catch { vedtakRepo.lagre(vedtak.medSak(sak)) }.mapLeft {
                        KanIkkeMottaVedtak.Persisteringsfeil
                    }
                }
            }
    }
}

sealed interface KanIkkeMottaVedtak {
    data class SakIkkeFunnet(val sakId: String) : KanIkkeMottaVedtak
    data object Persisteringsfeil : KanIkkeMottaVedtak
}
