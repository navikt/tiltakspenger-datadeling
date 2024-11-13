package no.nav.tiltakspenger.datadeling.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.client.tp.TpClient
import no.nav.tiltakspenger.datadeling.domene.PeriodisertKilde
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import java.time.LocalDate

class VedtakService(
    private val tpClient: TpClient,
    private val arenaClient: ArenaClient,
) {
    suspend fun hentVedtak(
        // TODO post-mvp jah: Bytt til Fnr+Periode
        ident: String,
        fom: LocalDate,
        tom: LocalDate,
        systembruker: Systembruker,
    ): Either<KanIkkeHenteVedtak, List<Vedtak>> {
        if (!systembruker.roller.kanLeseVedtak()) {
            return KanIkkeHenteVedtak.HarIkkeTilgang(
                kreverEnAvRollene = listOf(Systembrukerrolle.LES_BEHANDLING),
                harRollene = systembruker.roller.toList(),
            ).left()
        }
        val vedtak = tpClient.hentVedtak(ident, fom, tom)
        // TODO pre-mvp jah: Siden denne funksjonen brukes av Arena ønsker vi øke oppetiden ved å ikke gjøre dette kallet.
        // val arena = arenaClient.hentVedtak(ident, fom, tom)

        return (vedtak).right()
    }
    suspend fun hentPerioder(
        // TODO post-mvp jah: Bytt til Fnr+Periode
        ident: String,
        fom: LocalDate,
        tom: LocalDate,
        systembruker: Systembruker,
    ): Either<KanIkkeHenteVedtak, List<PeriodisertKilde>> {
        if (!systembruker.roller.kanLeseVedtak()) {
            return KanIkkeHenteVedtak.HarIkkeTilgang(
                kreverEnAvRollene = listOf(Systembrukerrolle.LES_VEDTAK),
                harRollene = systembruker.roller.toList(),
            ).left()
        }
        val vedtak = tpClient.hentVedtakPerioder(ident, fom, tom)
        val arena = arenaClient.hentPerioder(ident, fom, tom)

        return (arena + vedtak).right()
    }
}

sealed interface KanIkkeHenteVedtak {
    data class HarIkkeTilgang(
        val kreverEnAvRollene: List<Systembrukerrolle>,
        val harRollene: List<Systembrukerrolle>,
    ) : KanIkkeHenteVedtak
}
