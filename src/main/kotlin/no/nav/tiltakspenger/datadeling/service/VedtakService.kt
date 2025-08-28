package no.nav.tiltakspenger.datadeling.service

import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.motta.infra.db.VedtakRepo
import no.nav.tiltakspenger.datadeling.routes.vedtak.VedtakDTO
import no.nav.tiltakspenger.datadeling.routes.vedtak.toVedtakDTO
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.toTidslinje

class VedtakService(
    private val vedtakRepo: VedtakRepo,
    private val arenaClient: ArenaClient,
) {
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
            .toTidslinje()
        return toTidslinje
            .filter { it.verdi.rettighet != TiltakspengerVedtak.Rettighet.INGENTING }
            .map { it.verdi.oppdaterPeriode(it.periode) }
            .verdier
    }

    suspend fun hentVedtaksperioder(
        fnr: Fnr,
        periode: Periode,
    ): List<VedtakDTO> {
        val vedtakFraTpsak = vedtakRepo.hentForFnrOgPeriode(fnr, periode, Kilde.TPSAK)
            .map { it.toVedtakDTO() }
        val vedtakFraArena = arenaClient.hentVedtak(fnr, periode)
            .filter { it.rettighet != Rettighet.BARNETILLEGG }
            .map { it.toVedtakDTO() }

        return (vedtakFraArena + vedtakFraTpsak)
    }
}
