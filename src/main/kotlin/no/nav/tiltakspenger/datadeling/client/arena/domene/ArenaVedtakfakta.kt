package no.nav.tiltakspenger.datadeling.client.arena.domene

import java.time.LocalDate

data class ArenaVedtakfakta(
    val dagsats: Int?,
    val gjelderFra: LocalDate?,
    val gjelderTil: LocalDate?,
    val antallUtbetalinger: Int?,
    val belopPerUtbetalinger: Int?,
    val alternativBetalingsmottaker: String?,
)
