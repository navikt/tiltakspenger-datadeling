package no.nav.tiltakspenger.datadeling.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.motta.infra.db.VedtakRepo
import no.nav.tiltakspenger.datadeling.routes.vedtak.VedtakDTO
import no.nav.tiltakspenger.datadeling.routes.vedtak.toVedtakDTO
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.toTidslinjeMedHull

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
        systembruker: Systembruker,
    ): Either<KanIkkeHenteVedtak, List<TiltakspengerVedtak>> {
        if (!systembruker.roller.kanLeseVedtak()) {
            return KanIkkeHenteVedtak.HarIkkeTilgang(
                kreverEnAvRollene = listOf(Systembrukerrolle.LES_BEHANDLING),
                harRollene = systembruker.roller.toList(),
            ).left()
        }
        return vedtakRepo.hentForFnrOgPeriode(fnr, periode, Kilde.TPSAK)
            .toTidslinjeMedHull()
            .filter { it.verdi?.rettighet != TiltakspengerVedtak.Rettighet.INGENTING }
            .mapNotNull { it.verdi?.copy(periode = it.periode) }
            .right()
    }

    suspend fun hentVedtaksperioder(
        fnr: Fnr,
        periode: Periode,
        systembruker: Systembruker,
    ): Either<KanIkkeHenteVedtak, List<VedtakDTO>> {
        if (!systembruker.roller.kanLeseVedtak()) {
            return KanIkkeHenteVedtak.HarIkkeTilgang(
                kreverEnAvRollene = listOf(Systembrukerrolle.LES_VEDTAK),
                harRollene = systembruker.roller.toList(),
            ).left()
        }
        val vedtakFraTpsak = vedtakRepo.hentForFnrOgPeriode(fnr, periode, Kilde.TPSAK)
            .map { it.toVedtakDTO() }
        val vedtakFraArena = arenaClient.hentVedtak(fnr, periode)
            .filter { it.rettighet != Rettighet.BARNETILLEGG }
            .map { it.toVedtakDTO() }

        return (vedtakFraArena + vedtakFraTpsak).right()
    }
}

sealed interface KanIkkeHenteVedtak {
    data class HarIkkeTilgang(
        val kreverEnAvRollene: List<Systembrukerrolle>,
        val harRollene: List<Systembrukerrolle>,
    ) : KanIkkeHenteVedtak
}
