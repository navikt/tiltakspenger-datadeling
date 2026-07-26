package no.nav.tiltakspenger.datadeling.vedtak

import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode

/**
 * Henter den innvilgede tidslinjen for en person i en periode, uten Arena-vedtak.
 * Serverer `POST /vedtak/detaljer`.
 */
class HentVedtakDetaljerService(
    private val vedtakRepo: VedtakRepo,
) {
    /**
     * Merk at denne er reservert Arena og de ønsker at vi kun sender perioder bruker har rett til tiltakspenger.
     * Hvis ingen gjeldende vedtaksperioder gir rett lenger, ønsker de en tom liste.
     * Ref: https://nav-it.slack.com/archives/CC9GYTA2C/p1734512113726549
     */
    fun hentVedtakDetaljer(
        fnr: Fnr,
        periode: Periode,
    ): List<TiltakspengeVedtakMedSak> {
        val alleVedtakMedSak = vedtakRepo.hentForFnrOgPeriode(fnr, periode)
        val sak = alleVedtakMedSak.firstOrNull()?.sak
        val alleVedtak = alleVedtakMedSak.map { it.vedtak }
        return alleVedtak.hentInnvilgetTidslinje()
            .map { it.verdi.krympVirkningsperiode(it.periode) }
            .verdier
            .map {
                TiltakspengeVedtakMedSak(
                    sak = sak!!,
                    vedtak = it,
                )
            }
    }
}
