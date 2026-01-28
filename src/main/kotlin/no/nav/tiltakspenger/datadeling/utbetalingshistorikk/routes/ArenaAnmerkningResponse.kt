package no.nav.tiltakspenger.datadeling.utbetalingshistorikk.routes

import java.time.LocalDateTime

data class ArenaAnmerkningResponse(
    val kilde: String?,
    val registrert: LocalDateTime?,
    val beskrivelse: String?,
)
