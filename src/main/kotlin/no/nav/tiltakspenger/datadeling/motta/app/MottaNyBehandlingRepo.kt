package no.nav.tiltakspenger.datadeling.motta.app

import no.nav.tiltakspenger.datadeling.domene.TiltakspengerBehandling

interface MottaNyBehandlingRepo {
    fun lagre(behandling: TiltakspengerBehandling)
}

sealed interface KunneIkkeLagreBehandling {
    data object UkjentFeil : KunneIkkeLagreBehandling
}
