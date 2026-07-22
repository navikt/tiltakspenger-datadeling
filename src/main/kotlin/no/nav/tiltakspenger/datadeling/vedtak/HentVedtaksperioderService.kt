package no.nav.tiltakspenger.datadeling.vedtak

import arrow.core.Either
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.arena.Rettighet
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak.Rettighet.AVSLAG
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.httpklient.HttpKlientError
import no.nav.tiltakspenger.libs.httpklient.loggFeil
import no.nav.tiltakspenger.libs.httpklient.loggSuksess
import no.nav.tiltakspenger.libs.logging.Sikkerlogg
import no.nav.tiltakspenger.libs.periode.Periode
import java.time.Clock
import java.time.LocalDate

class HentVedtaksperioderService(
    private val vedtakRepo: VedtakRepo,
    private val arenaClient: ArenaClient,
    private val clock: Clock,
    private val sikkerlogg: Sikkerlogg = Sikkerlogg,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Henter alle vedtak som påvirker rett til tiltakspenger i perioden fra både TPSAK og Arena.
     * Avslag fra tp-sak ekskluderes.
     */
    suspend fun hentVedtaksperioder(
        fnr: Fnr,
        periode: Periode,
    ): Either<HttpKlientError, List<DatadelingsvedtakUtenAvslag>> {
        logger.debug { "Henter vedtaksperioder for fnr og periode" }
        val idag = LocalDate.now(clock)
        val vedtakFraTpsak = vedtakRepo.hentForFnrOgPeriode(fnr, periode)
            .filter { it.vedtak.rettighet != AVSLAG }
            .map { it.vedtak.toDatadelingsvedtakUtenAvslag(logger, idag) }
        return arenaClient.hentVedtak(fnr, periode)
            .onLeft { it.loggFeil(logger, "henting av vedtak fra Arena", "Periode: $periode", sikkerlogg) }
            .onRight { it.loggSuksess(logger, "Hentet vedtak fra Arena.", sikkerlogg) }
            .map { respons ->
                val vedtakFraArena = respons.body
                    .filter { it.rettighet != Rettighet.BARNETILLEGG }
                    .map { it.toDatadelingsvedtakUtenAvslag() }
                vedtakFraArena + vedtakFraTpsak
            }
    }
}
