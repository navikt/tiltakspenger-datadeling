package no.nav.tiltakspenger.datadeling.vedtak.infra

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.arena.Rettighet
import no.nav.tiltakspenger.datadeling.vedtak.TiltakspengerVedtak.Rettighet.AVSLAG
import no.nav.tiltakspenger.datadeling.vedtak.VedtakRepo
import no.nav.tiltakspenger.datadeling.vedtak.infra.routes.VedtakDTO
import no.nav.tiltakspenger.datadeling.vedtak.infra.routes.toVedtakDTO
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periode.Periode

class HentVedtaksperioderService(
    private val vedtakRepo: VedtakRepo,
    private val arenaClient: ArenaClient,
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Henter alle vedtak som påvirker rett til tiltakspenger i perioden fra både TPSAK og Arena.
     * Avslag fra tp-sak ekskluderes.
     */
    suspend fun hentVedtaksperioder(
        fnr: Fnr,
        periode: Periode,
    ): List<VedtakDTO> {
        logger.debug { "Henter vedtaksperioder for fnr og periode" }
        val vedtakFraTpsak = vedtakRepo.hentForFnrOgPeriode(fnr, periode)
            .filter { it.vedtak.rettighet != AVSLAG }
            .map { it.vedtak.toVedtakDTO(logger) }
        val vedtakFraArena = arenaClient.hentVedtak(fnr, periode)
            .filter { it.rettighet != Rettighet.BARNETILLEGG }
            .map { it.toVedtakDTO() }

        return vedtakFraArena + vedtakFraTpsak
    }
}
