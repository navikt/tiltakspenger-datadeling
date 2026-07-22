package no.nav.tiltakspenger.datadeling.utbetalingshistorikk.infra.routes

import no.nav.tiltakspenger.datadeling.arena.ArenaUtbetalingshistorikk
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

internal fun List<ArenaUtbetalingshistorikk>.toArenaUtbetalingshistorikkResponse(): List<ArenaUtbetalingshistorikkResponse> = map {
    ArenaUtbetalingshistorikkResponse(
        meldekortId = it.meldekortId,
        dato = it.dato,
        transaksjonstype = it.transaksjonstype,
        sats = it.sats,
        status = it.status,
        vedtakId = it.vedtakId,
        belop = it.belop,
        fraOgMedDato = it.fraOgMedDato,
        tilOgMedDato = it.tilOgMedDato,
    )
}
