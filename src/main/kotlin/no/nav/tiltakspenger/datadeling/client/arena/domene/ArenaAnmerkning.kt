package no.nav.tiltakspenger.datadeling.client.arena.domene

import java.time.LocalDate

data class ArenaAnmerkning(
    val kilde: String?,
    val registrert: LocalDate?,
    val beskrivelse: String?,
)
