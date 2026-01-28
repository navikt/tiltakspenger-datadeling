package no.nav.tiltakspenger.datadeling.utbetalingshistorikk.routes

import java.time.LocalDate

data class ArenaVedtakfaktaResponse(
    val dagsats: Int?,
    val gjelderFra: LocalDate?,
    val gjelderTil: LocalDate?,
    val antallUtbetalinger: Int?,
    val belopPerUtbetalinger: Int?,
    val alternativBetalingsmottaker: String?,
)
