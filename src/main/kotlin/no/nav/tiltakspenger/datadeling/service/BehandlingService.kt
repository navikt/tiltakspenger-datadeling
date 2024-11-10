package no.nav.tiltakspenger.datadeling.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.datadeling.client.tp.TpClient
import no.nav.tiltakspenger.datadeling.domene.Behandling
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import java.time.LocalDate

class BehandlingService(
    private val tpClient: TpClient,
) {
    suspend fun hentBehandlinger(
        ident: String,
        // TODO post-mvp jah: Bytt til Periode
        fom: LocalDate,
        tom: LocalDate,
        systembruker: Systembruker,
    ): Either<KanIkkeHenteBehandlinger, List<Behandling>> {
        if (!systembruker.roller.kanLeseBehandlinger()) {
            return KanIkkeHenteBehandlinger.HarIkkeTilgang(
                kreverEnAvRollene = listOf(Systembrukerrolle.LES_BEHANDLING),
                harRollene = systembruker.roller.toList(),
            ).left()
        }
        return tpClient.hentBehandlinger(ident, fom, tom).right()
    }
}

sealed interface KanIkkeHenteBehandlinger {
    data class HarIkkeTilgang(
        val kreverEnAvRollene: List<Systembrukerrolle>,
        val harRollene: List<Systembrukerrolle>,
    ) : KanIkkeHenteBehandlinger
}
