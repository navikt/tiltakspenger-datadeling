package no.nav.tiltakspenger.datadeling.client.arena.domene

import java.time.LocalDate

data class ArenaUtbetalingshistorikk(
    val meldekortId: Long?,
    val dato: LocalDate,
    val transaksjonstype: String,
    val sats: Double,
    val status: String,
    val vedtakId: Long?,
    val belop: Double,
    val fraOgMedDato: LocalDate,
    val tilOgMedDato: LocalDate,
)
