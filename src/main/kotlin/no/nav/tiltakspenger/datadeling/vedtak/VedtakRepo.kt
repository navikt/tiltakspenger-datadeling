package no.nav.tiltakspenger.datadeling.vedtak

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.LocalDateTime

interface VedtakRepo {
    fun lagre(vedtak: TiltakspengerVedtak)
    fun hentForFnrOgPeriode(fnr: Fnr, periode: Periode): List<TiltakspengeVedtakMedSak>
    fun hentForFnr(fnr: Fnr): List<TiltakspengeVedtakMedSak>
    fun hentSakMedVedtakForFnr(fnr: Fnr): TiltakspengeSakMedVedtak?
    fun hentRammevedtakSomSkalDelesMedObo(limit: Int = 20): List<TiltakspengeVedtakMedSak>
    fun markerSendtTilObo(vedtakId: String, tidspunkt: LocalDateTime)
}
