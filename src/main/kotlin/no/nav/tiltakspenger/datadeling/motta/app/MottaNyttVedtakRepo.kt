package no.nav.tiltakspenger.datadeling.motta.app

import arrow.core.Either
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak

interface MottaNyttVedtakRepo {
    fun lagre(vedtak: TiltakspengerVedtak): Either<KunneIkkeLagreVedtak, Unit>
}

sealed interface KunneIkkeLagreVedtak {
    data object AlleredeLagret : KunneIkkeLagreVedtak
}
