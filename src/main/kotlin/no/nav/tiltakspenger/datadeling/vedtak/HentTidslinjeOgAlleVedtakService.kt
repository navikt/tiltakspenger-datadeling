package no.nav.tiltakspenger.datadeling.vedtak

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.arena.Rettighet
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode

class HentTidslinjeOgAlleVedtakService(
    private val vedtakRepo: VedtakRepo,
    private val arenaClient: ArenaClient,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun hentTidslinjeOgAlleVedtak(
        fnr: Fnr,
        periode: Periode,
    ): VedtakTidslinje {
        logger.debug { "Henter tidslinje og alle vedtak for fnr og periode" }
        val alleVedtakMedSak = vedtakRepo.hentForFnrOgPeriode(fnr, periode)
        val tpSak = alleVedtakMedSak.firstOrNull()?.sak
        val alleVedtak = alleVedtakMedSak.map { it.vedtak }
        val tidslinje = alleVedtak.hentTidslinje()
            // Vil kunne inneholde både innvilgelser (inkl. omgjøringer) og stans.
            .map { it.verdi.krympVirkningsperiode(it.periode) }
            .verdier

        val vedtakFraArena = arenaClient.hentVedtak(fnr, periode)
            .filter { it.rettighet != Rettighet.BARNETILLEGG }

        val arenaSak = if (tpSak == null) {
            vedtakFraArena.sortedByDescending { it.beslutningsdato }.firstOrNull()?.sak
        } else {
            null
        }

        return VedtakTidslinje(
            tidslinje = tidslinje.toVedtakTidslinjeVedtak(logger).sortedByDescending { it.vedtaksdato },
            alleVedtak = alleVedtak.toVedtakTidslinjeVedtak(logger).sortedByDescending { it.vedtaksdato },
            vedtakFraArena = vedtakFraArena.map { it.toDatadelingsvedtakUtenAvslag() }.sortedByDescending { it.periode.tilOgMed },
            sak = tpSak?.toVedtakSak() ?: arenaSak?.toVedtakSak(),
        )
    }
}
