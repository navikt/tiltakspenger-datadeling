package no.nav.tiltakspenger.datadeling.motta.app

import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak

class MottaNyttVedtakService(
    private val mottaNyttVedtakRepo: MottaNyttVedtakRepo,
) {
    fun motta(vedtak: TiltakspengerVedtak) {
        mottaNyttVedtakRepo.lagre(vedtak)
    }
}
