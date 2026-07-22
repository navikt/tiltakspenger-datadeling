package no.nav.tiltakspenger.datadeling.vedtak

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.arena.Rettighet
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.httpklient.loggSuksess
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.Clock
import java.time.LocalDate

class HentTidslinjeOgAlleVedtakService(
    private val vedtakRepo: VedtakRepo,
    private val arenaClient: ArenaClient,
    private val clock: Clock,
    private val sikkerlogg: Sikkerlogg = Sikkerlogg,
) {
    private val logger = KotlinLogging.logger {}

    suspend fun hentTidslinjeOgAlleVedtak(
        fnr: Fnr,
        periode: Periode,
    ): Either<HttpKlientError, VedtakTidslinje> {
        logger.debug { "Henter tidslinje og alle vedtak for fnr og periode" }
        val alleVedtakMedSak = vedtakRepo.hentForFnrOgPeriode(fnr, periode)
        val tpSak = alleVedtakMedSak.firstOrNull()?.sak
        val alleVedtak = alleVedtakMedSak.map { it.vedtak }
        val tidslinje = alleVedtak.hentTidslinje()
            // Vil kunne inneholde både innvilgelser (inkl. omgjøringer) og stans.
            .map { it.verdi.krympVirkningsperiode(it.periode) }
            .verdier

        return arenaClient.hentVedtak(fnr, periode)
            .onLeft { it.loggFeil(logger, "henting av vedtak fra Arena", "Periode: $periode", sikkerlogg) }
            .onRight { it.loggSuksess(logger, "Hentet vedtak fra Arena.", sikkerlogg) }
            .map { respons ->
                val vedtakFraArena = respons.body.filter { it.rettighet != Rettighet.BARNETILLEGG }

                val arenaSak = if (tpSak == null) {
                    vedtakFraArena.sortedByDescending { it.beslutningsdato }.firstOrNull()?.sak
                } else {
                    null
                }

                val idag = LocalDate.now(clock)
                VedtakTidslinje(
                    tidslinje = tidslinje.toVedtakTidslinjeVedtak(logger, idag).sortedByDescending { it.vedtaksdato },
                    alleVedtak = alleVedtak.toVedtakTidslinjeVedtak(logger, idag).sortedByDescending { it.vedtaksdato },
                    vedtakFraArena = vedtakFraArena.map { it.toDatadelingsvedtakUtenAvslag() }.sortedByDescending { it.periode.tilOgMed },
                    sak = tpSak?.toVedtakSak() ?: arenaSak?.toVedtakSak(),
                )
            }
    }
}
