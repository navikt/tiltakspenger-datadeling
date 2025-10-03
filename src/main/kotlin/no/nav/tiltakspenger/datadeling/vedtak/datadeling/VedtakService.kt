package no.nav.tiltakspenger.datadeling.vedtak.datadeling

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.client.arena.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.VedtakDTO
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.VedtakTidslinjeResponse
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.toVedtakDTO
import no.nav.tiltakspenger.datadeling.vedtak.datadeling.routes.toVedtakResponse
import no.nav.tiltakspenger.datadeling.vedtak.db.VedtakRepo
import no.nav.tiltakspenger.datadeling.vedtak.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.toTidslinje

class VedtakService(
    private val vedtakRepo: VedtakRepo,
    private val arenaClient: ArenaClient,
) {
    val logger = KotlinLogging.logger {}

    /**
     * Merk at denne er reservert Arena og de ønsker at vi kun sender perioder bruker har rett til tiltakspenger.
     * Ved en perfekt stans, ønsker de en tom liste.
     * Ref: https://nav-it.slack.com/archives/CC9GYTA2C/p1734512113726549
     */
    fun hentTpVedtak(
        fnr: Fnr,
        periode: Periode,
    ): List<TiltakspengerVedtak> {
        val toTidslinje = vedtakRepo.hentForFnrOgPeriode(fnr, periode, Kilde.TPSAK)
            .filter { it.rettighet != TiltakspengerVedtak.Rettighet.AVSLAG }
            .toTidslinje()
        return toTidslinje
            .filter { it.verdi.rettighet != TiltakspengerVedtak.Rettighet.STANS }
            .map { it.verdi.oppdaterPeriode(it.periode) }
            .verdier
    }

    fun hentTidslinjeOgAlleVedtak(
        fnr: Fnr,
        periode: Periode,
    ): VedtakTidslinjeResponse {
        val alleVedtak = vedtakRepo.hentForFnrOgPeriode(fnr, periode, Kilde.TPSAK)
        val tidslinje = alleVedtak
            .filter { it.rettighet != TiltakspengerVedtak.Rettighet.AVSLAG }
            .toTidslinje()
            .map { it.verdi.oppdaterPeriode(it.periode) }
            .verdier

        return VedtakTidslinjeResponse(
            tidslinje = tidslinje.toVedtakResponse(logger),
            alleVedtak = alleVedtak.toVedtakResponse(logger),
        )
    }

    suspend fun hentVedtaksperioder(
        fnr: Fnr,
        periode: Periode,
    ): List<VedtakDTO> {
        val vedtakFraTpsak = vedtakRepo.hentForFnrOgPeriode(fnr, periode, Kilde.TPSAK)
            .filter { it.rettighet != TiltakspengerVedtak.Rettighet.AVSLAG }
            .map { it.toVedtakDTO(logger) }
        val vedtakFraArena = arenaClient.hentVedtak(fnr, periode)
            .filter { it.rettighet != Rettighet.BARNETILLEGG }
            .map { it.toVedtakDTO() }

        return (vedtakFraArena + vedtakFraTpsak)
    }
}
