package no.nav.tiltakspenger.datadeling.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.domene.PeriodisertKilde
import no.nav.tiltakspenger.datadeling.domene.Rettighet
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.motta.infra.db.VedtakRepo
import no.nav.tiltakspenger.datadeling.routes.vedtak.VedtakDTO
import no.nav.tiltakspenger.datadeling.routes.vedtak.toVedtakDTO
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.toTidslinje

class VedtakService(
    private val vedtakRepo: VedtakRepo,
    private val arenaClient: ArenaClient,
) {
    fun hentTpVedtak(
        fnr: Fnr,
        periode: Periode,
        systembruker: Systembruker,
    ): Either<KanIkkeHenteVedtak, Periodisering<TiltakspengerVedtak>> {
        if (!systembruker.roller.kanLeseVedtak()) {
            return KanIkkeHenteVedtak.HarIkkeTilgang(
                kreverEnAvRollene = listOf(Systembrukerrolle.LES_BEHANDLING),
                harRollene = systembruker.roller.toList(),
            ).left()
        }
        return vedtakRepo.hentForFnrOgPeriode(fnr, periode, Kilde.TPSAK).toTidslinje().right()
    }

    suspend fun hentPerioder(
        fnr: Fnr,
        periode: Periode,
        systembruker: Systembruker,
    ): Either<KanIkkeHenteVedtak, List<PeriodisertKilde>> {
        if (!systembruker.roller.kanLeseVedtak()) {
            return KanIkkeHenteVedtak.HarIkkeTilgang(
                kreverEnAvRollene = listOf(Systembrukerrolle.LES_VEDTAK),
                harRollene = systembruker.roller.toList(),
            ).left()
        }
        val vedtak = vedtakRepo.hentForFnrOgPeriode(fnr, periode, Kilde.TPSAK).map { vedtak ->
            PeriodisertKilde(
                kilde = vedtak.kilde,
                periode = vedtak.periode,
            )
        }
        val arena = arenaClient.hentPerioder(fnr, periode)

        return (arena + vedtak).right()
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
            .filter { it.rettighet != TiltakspengerVedtak.Rettighet.INGENTING }
            .map { it.toVedtakDTO() }
        val vedtakFraArena = arenaClient.hentVedtak(fnr, periode)
            .filter { it.rettighet != Rettighet.INGENTING && it.rettighet != Rettighet.BARNETILLEGG }
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
