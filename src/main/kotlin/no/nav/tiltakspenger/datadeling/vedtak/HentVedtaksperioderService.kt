package no.nav.tiltakspenger.datadeling.vedtak

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.arena.Rettighet
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak.Rettighet.AVSLAG
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.Clock
import java.time.LocalDate

class HentVedtaksperioderService(
    private val vedtakRepo: VedtakRepo,
    private val arenaClient: ArenaClient,
    private val clock: Clock,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Henter alle vedtak som påvirker rett til tiltakspenger i perioden fra både TPSAK og Arena.
     * Avslag fra tp-sak ekskluderes.
     */
    suspend fun hentVedtaksperioder(
        fnr: Fnr,
        periode: Periode,
    ): List<DatadelingsvedtakUtenAvslag> {
        logger.debug { "Henter vedtaksperioder for fnr og periode" }
        val idag = LocalDate.now(clock)
        val vedtakFraTpsak = vedtakRepo.hentForFnrOgPeriode(fnr, periode)
            .filter { it.vedtak.rettighet != AVSLAG }
            .map { it.vedtak.toDatadelingsvedtakUtenAvslag(logger, idag) }
        val vedtakFraArena = arenaClient.hentVedtak(fnr, periode)
            .filter { it.rettighet != Rettighet.BARNETILLEGG }
            .map { it.toDatadelingsvedtakUtenAvslag() }

        return vedtakFraArena + vedtakFraTpsak
    }
}
