package no.nav.tiltakspenger.datadeling.arena.infra.routes

import java.time.LocalDateTime

data class ArenaAnmerkningResponse(
    val kilde: String?,
    val registrert: LocalDateTime?,
    val beskrivelse: String?,
)
