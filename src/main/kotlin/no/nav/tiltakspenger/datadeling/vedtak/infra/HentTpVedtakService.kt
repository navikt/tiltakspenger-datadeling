package no.nav.tiltakspenger.datadeling.vedtak.infra

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengeVedtakMedSak
import no.nav.tiltakspenger.datadeling.vedtak.VedtakRepo
import no.nav.tiltakspenger.datadeling.vedtak.hentInnvilgetTidslinje
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode

class HentTpVedtakService(
    private val vedtakRepo: VedtakRepo,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Merk at denne er reservert Arena og de ønsker at vi kun sender perioder bruker har rett til tiltakspenger.
     * Hvis ingen gjeldende vedtaksperioder gir rett lenger, ønsker de en tom liste.
     * Ref: https://nav-it.slack.com/archives/CC9GYTA2C/p1734512113726549
     */
    fun hentTpVedtak(
        fnr: Fnr,
        periode: Periode,
    ): List<TiltakspengeVedtakMedSak> {
        logger.debug { "Henter TP-vedtak for fnr og periode" }
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
