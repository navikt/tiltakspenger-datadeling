package no.nav.tiltakspenger.datadeling.motta.app

import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode

interface VedtakRepo {
    fun lagre(vedtak: TiltakspengerVedtak)
    fun hentForFnrOgPeriode(
        fnr: Fnr,
        periode: Periode,
        kilde: String,
    ): List<TiltakspengerVedtak>
}
