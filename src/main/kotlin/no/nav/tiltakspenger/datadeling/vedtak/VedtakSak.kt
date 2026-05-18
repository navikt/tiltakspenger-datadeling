package no.nav.tiltakspenger.datadeling.vedtak

import no.nav.tiltakspenger.datadeling.arena.ArenaVedtak
import no.nav.tiltakspenger.datadeling.sak.Sak
import java.time.LocalDateTime

data class VedtakSak(
    val sakId: String,
    val saksnummer: String,
    val kilde: String = "TPSAK",
    val status: String = "Løpende",
    val opprettetDato: LocalDateTime,
)

fun Sak.toVedtakSak() = VedtakSak(
    sakId = id.toString(),
    saksnummer = saksnummer.verdi,
    opprettetDato = opprettet,
)

fun ArenaVedtak.Sak.toVedtakSak() = VedtakSak(
    sakId = sakId,
    saksnummer = saksnummer,
    opprettetDato = opprettetDato.atTime(9, 0),
    status = status,
    kilde = "ARENA",
)
