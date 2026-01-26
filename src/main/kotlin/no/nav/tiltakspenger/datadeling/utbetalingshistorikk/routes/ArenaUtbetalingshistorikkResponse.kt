package no.nav.tiltakspenger.datadeling.utbetalingshistorikk.routes

import java.time.LocalDate

data class ArenaUtbetalingshistorikkResponse(
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
