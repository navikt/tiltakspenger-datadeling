package no.nav.tiltakspenger.datadeling.motta.app

import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak

interface MottaNyttVedtakRepo {
    fun lagre(vedtak: TiltakspengerVedtak)
}
