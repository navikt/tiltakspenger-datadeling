package no.nav.tiltakspenger.datadeling.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.domene.PeriodisertKilde
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.TiltakspengerVedtak
import no.nav.tiltakspenger.datadeling.motta.app.VedtakRepo
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.periodisering.Periode
import no.nav.tiltakspenger.libs.periodisering.Periodisering
import no.nav.tiltakspenger.libs.periodisering.toTidslinje

class VedtakService(
    private val vedtakRepo: VedtakRepo,
    private val arenaClient: ArenaClient,
) {
    suspend fun hentTpVedtak(
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
        return vedtakRepo.hentForFnrOgPeriode(fnr, periode, "tp").toTidslinje().right()
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
        val vedtak = vedtakRepo.hentForFnrOgPeriode(fnr, periode, "tp").map { vedtak ->
            PeriodisertKilde(
                kilde = "tp",
                periode = vedtak.periode,
            )
        }
        val arena = arenaClient.hentPerioder(fnr, periode)

        return (arena + vedtak).right()
    }
}

sealed interface KanIkkeHenteVedtak {
    data class HarIkkeTilgang(
        val kreverEnAvRollene: List<Systembrukerrolle>,
        val harRollene: List<Systembrukerrolle>,
    ) : KanIkkeHenteVedtak
}
