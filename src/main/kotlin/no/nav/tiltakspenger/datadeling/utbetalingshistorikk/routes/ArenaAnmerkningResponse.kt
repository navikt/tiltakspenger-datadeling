package no.nav.tiltakspenger.datadeling.utbetalingshistorikk.routes

import java.time.LocalDate

data class ArenaAnmerkningResponse(
    val kilde: String?,
    val registrert: LocalDate?,
    val beskrivelse: String?,
)
