package no.nav.tiltakspenger.datadeling.motta.app

import arrow.core.Either
import arrow.core.left
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak

class MottaNyttVedtakService(
    private val mottaNyttVedtakRepo: MottaNyttVedtakRepo,
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
        return mottaNyttVedtakRepo.lagre(vedtak).mapLeft {
            when (it) {
                KunneIkkeLagreVedtak.AlleredeLagret -> KanIkkeMottaVedtak.AlleredeLagret
            }
        }
    }
}

sealed interface KanIkkeMottaVedtak {
    data class HarIkkeTilgang(
        val kreverEnAvRollene: List<Systembrukerrolle>,
        val harRollene: List<Systembrukerrolle>,
    ) : KanIkkeMottaVedtak
    data object AlleredeLagret : KanIkkeMottaVedtak
}
