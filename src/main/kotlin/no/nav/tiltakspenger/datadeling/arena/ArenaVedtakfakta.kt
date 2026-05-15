package no.nav.tiltakspenger.datadeling.arena

import java.time.LocalDate

data class ArenaVedtakfakta(
    val dagsats: Int?,
    val gjelderFra: LocalDate?,
    val gjelderTil: LocalDate?,
    val antallUtbetalinger: Int?,
    val belopPerUtbetalinger: Int?,
    val alternativBetalingsmottaker: String?,
)
