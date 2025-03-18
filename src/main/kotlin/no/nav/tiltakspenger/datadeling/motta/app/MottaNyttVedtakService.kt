package no.nav.tiltakspenger.datadeling.motta.app

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.motta.infra.db.VedtakRepo

class MottaNyttVedtakService(
    private val vedtakRepo: VedtakRepo,
) {
    fun motta(
        vedtak: TiltakspengerVedtak,
        systembruker: Systembruker,
    ): Either<KanIkkeMottaVedtak, Unit> {
        if (!systembruker.roller.kanLagreTiltakspengerHendelser()) {
            return KanIkkeMottaVedtak.HarIkkeTilgang(
                kreverEnAvRollene = listOf(Systembrukerrolle.LAGRE_TILTAKSPENGER_HENDELSER),
                harRollene = systembruker.roller.toList(),
            ).left()
        }
        return Either.catch { vedtakRepo.lagre(vedtak) }.mapLeft {
            KanIkkeMottaVedtak.Persisteringsfeil
        }
    }
}

sealed interface KanIkkeMottaVedtak {
    data class HarIkkeTilgang(
        val kreverEnAvRollene: List<Systembrukerrolle>,
        val harRollene: List<Systembrukerrolle>,
    ) : KanIkkeMottaVedtak

    data object Persisteringsfeil : KanIkkeMottaVedtak
}
