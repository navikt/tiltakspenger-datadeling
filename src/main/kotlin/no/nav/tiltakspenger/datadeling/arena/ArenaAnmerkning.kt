package no.nav.tiltakspenger.datadeling.arena

import java.time.LocalDateTime

data class ArenaAnmerkning(
    val kilde: String?,
    val registrert: LocalDateTime?,
    val beskrivelse: String?,
)
