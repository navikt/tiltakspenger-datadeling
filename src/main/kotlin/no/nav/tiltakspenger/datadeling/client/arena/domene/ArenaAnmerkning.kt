package no.nav.tiltakspenger.datadeling.client.arena.domene

import java.time.LocalDateTime

data class ArenaAnmerkning(
    val kilde: String?,
    val registrert: LocalDateTime?,
    val beskrivelse: String?,
)
