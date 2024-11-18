package no.nav.tiltakspenger.datadeling.service

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import no.nav.tiltakspenger.datadeling.client.arena.ArenaClient
import no.nav.tiltakspenger.datadeling.domene.Kilde
import no.nav.tiltakspenger.datadeling.domene.Systembruker
import no.nav.tiltakspenger.datadeling.domene.Systembrukerrolle
import no.nav.tiltakspenger.datadeling.domene.Vedtak
import no.nav.tiltakspenger.datadeling.motta.app.VedtakRepo
import no.nav.tiltakspenger.libs.common.Bruker
import no.nav.tiltakspenger.libs.common.Fnr
import no.nav.tiltakspenger.libs.common.Saksbehandler
import no.nav.tiltakspenger.libs.periodisering.Periode

/**
 * Henter vedtak for et gitt fnr og periode.
 */
class VedtakstidslinjeService(
    private val vedtakRepo: VedtakRepo,
    private val arenaClient: ArenaClient,
) {
    suspend fun hentTidslinje(
        fnr: Fnr,
        periode: Periode,
        kilder: Set<Kilde>,
        bruker: Bruker<*, *>,
    ): Either<KanIkkeHenteVedtak, List<Vedtak>> {
        bruker.validerRettigheter().onLeft { return it.left() }
        // TODO post-mvp jah: Dersom vi får revurderinger, må vi lage en tidslinje.
        val vedtakTp = if (kilder.contains(Kilde.TILTAKSPENGER)) {
            vedtakRepo.hentForFnrOgPeriode(
                fnr = fnr,
                periode = periode,
                // TODO post-mvp jah: Bruk enum
                kilde = "tp",
            )
        } else {
            emptyList()
        }
        val vedtakArena = if (kilder.contains(Kilde.ARENA)) arenaClient.hentVedtak(fnr, periode) else emptyList()
        return (vedtakArena + vedtakTp).right()
    }
}

private fun Bruker<*, *>.validerRettigheter(): Either<KanIkkeHenteVedtak.HarIkkeTilgang, Unit> {
    return when (this) {
        is Systembruker -> {
            if (!this.roller.kanLeseVedtak()) {
                KanIkkeHenteVedtak.HarIkkeTilgang(
                    kreverEnAvRollene = listOf(Systembrukerrolle.LES_VEDTAK),
                    harRollene = this.roller.toList(),
                ).left()
            } else {
                Unit.right()
            }
        }

        is Saksbehandler -> {
            if (!this.scopes.value.contains(Systembrukerrolle.LES_VEDTAK)) {
                KanIkkeHenteVedtak.HarIkkeTilgang(
                    kreverEnAvRollene = listOf(Systembrukerrolle.LES_VEDTAK),
                    harRollene = this.scopes.toList(),
                ).left()
            } else {
                Unit.right()
            }
        }

        else -> {
            throw IllegalStateException("Oops, har vi glemt å legge til en by brukertype? Ukjent brukertype $this")
        }
    }
}
